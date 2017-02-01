/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.rs.cell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.WebDAVMethod.PROPFIND;
import org.apache.wink.webdav.model.Creationdate;
import org.apache.wink.webdav.model.Getcontentlength;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Getlastmodified;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Propfind;
import org.apache.wink.webdav.model.Resourcetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.EventUtils;
import io.personium.core.eventlog.ArchiveLogCollection;
import io.personium.core.eventlog.ArchiveLogFile;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * イベントログ用JAX-RS Resource.
 */
public class LogResource {

    /** archiveコレクション名. */
    public static final String ARCHIVE_COLLECTION = "archive";
    /** currentコレクション名. */
    public static final String CURRENT_COLLECTION = "current";

    private static final String DEFAULT_LOG = "default.log";

    Cell cell;
    AccessContext accessContext;
    DavRsCmp davRsCmp;

    static Logger log = LoggerFactory.getLogger(LogResource.class);

    /**
     * constructor.
     * @param cell Cell
     * @param accessContext AccessContext
     * @param davRsCmp DavRsCmp
     */
    public LogResource(final Cell cell, final AccessContext accessContext, final DavRsCmp davRsCmp) {
        this.accessContext = accessContext;
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * カレントのイベントログファイルの一覧を取得する.
     * @return JAX-RS Response Object
     */
    @Path(CURRENT_COLLECTION)
    @PROPFIND
    public final Response currentPropfind() {
        // 現状はカレントログの一覧取得は未実装のため、501を返却する
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * アーカイブのイベントログファイルの一覧を取得する.
     * @param requestBodyXml Request Body
     * @param uriInfo リクエストURL情報
     * @param contentLength contentlengthヘッダの内容
     * @param transferEncoding Transfer-Encodingヘッダの内容
     * @param depth Depthヘッダの内容
     * @return JAX-RS Response Object
     */
    @Path(ARCHIVE_COLLECTION)
    @PROPFIND
    public final Response archivePropfind(final Reader requestBodyXml,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth
            ) {

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.LOG_READ);

        // Depthヘッダの有効な値は 0, 1
        // infinityの場合はサポートしないので403で返す
        if ("infinity".equals(depth)) {
            throw PersoniumCoreException.Dav.PROPFIND_FINITE_DEPTH;
        } else if (depth == null) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params("null");
        } else if (!("0".equals(depth) || "1".equals(depth))) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        // リクエストボディをパースして pfオブジェクトを作成する
        // ボディが空の場合はallpropが設定されたのと同じ処理をする
        Propfind propfind = null;
        if (ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(requestBodyXml);
                propfind = Propfind.unmarshal(br);
            } catch (Exception e) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e);
            }
        }
        if (null != propfind && !propfind.isAllprop()) {
            throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
        }

        // archiveコレクションと配下のファイルの情報を収集する
        ArchiveLogCollection archiveLogCollection = new ArchiveLogCollection(this.cell, uriInfo);
        archiveLogCollection.createFileInformation();

        // レスポンス生成
        final Multistatus multiStatus = createMultiStatus(depth, archiveLogCollection);
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                Multistatus.marshal(multiStatus, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .entity(str).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    }

    private Multistatus createMultiStatus(final String depth, ArchiveLogCollection archiveLogCollection) {
        ObjectFactory of = new ObjectFactory();
        final Multistatus multiStatus = of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> responseList = multiStatus.getResponse();

        // Archiveコレクションの情報をレスポンスに追加する
        org.apache.wink.webdav.model.Response collectionResponse =
                this.createPropfindResponse(
                        archiveLogCollection.getCreated(),
                        archiveLogCollection.getUpdated(),
                        archiveLogCollection.getUrl(),
                        null);
        responseList.add(collectionResponse);

        // Depthが1の場合は、ログファイルの情報をレスポンスに追加する
        if ("1".equals(depth)) {
            for (ArchiveLogFile archiveFile : archiveLogCollection.getArchivefileList()) {
                org.apache.wink.webdav.model.Response fileResponse =
                        this.createPropfindResponse(
                                archiveFile.getCreated(),
                                archiveFile.getUpdated(),
                                archiveFile.getUrl(),
                                archiveFile.getSize());
                responseList.add(fileResponse);
            }
        }

        return multiStatus;
    }

    /**
     * PROPFINDのレスポンスを作成する.
     */
    org.apache.wink.webdav.model.Response createPropfindResponse(long created, long updated, String href, Long size) {
        // hrefを追加
        ObjectFactory of = new ObjectFactory();
        org.apache.wink.webdav.model.Response ret = of.createResponse();
        ret.getHref().add(href);

        // creationdateを追加
        Creationdate cd = of.createCreationdate();
        cd.setValue(new Date(created));
        ret.setPropertyOk(cd);

        // getlastmodifiedを追加
        Getlastmodified lm = of.createGetlastmodified();
        lm.setValue(new Date(updated));
        ret.setPropertyOk(lm);

        if (size != null) {
            // ログファイルの場合
            // getcontentlengthを追加
            Getcontentlength contentLength = of.createGetcontentlength();
            contentLength.setValue(String.valueOf(size));
            ret.setPropertyOk(contentLength);

            // getcontenttypeは"text/csv"固定で追加
            Getcontenttype contentType = of.createGetcontenttype();
            contentType.setValue(EventUtils.TEXT_CSV);
            ret.setPropertyOk(contentType);

            // 空のresourcetypeを追加
            Resourcetype colRt = of.createResourcetype();
            ret.setPropertyOk(colRt);
        } else {
            // ログコレクションの場合
            // resourcetypeはWebDavコレクション固定で追加
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            ret.setPropertyOk(colRt);
        }

        return ret;
    }

    /**
     * イベントログファイルを取得する.
     * @param ifNoneMatch If-None-Matchヘッダ
     * @param logCollection Collection名
     * @param fileName fileName
     * @return JAXRS Response
     */
    @Path("{logCollection}/{filename}")
    @GET
    public final Response getLogFile(@HeaderParam(HttpHeaders.IF_NONE_MATCH) final String ifNoneMatch,
            @PathParam("logCollection") final String logCollection,
            @PathParam("filename") final String fileName) {

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.LOG_READ);

        // イベントログのCollection名のチェック
        if (!isValidLogCollection(logCollection)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
        }

        // ファイル名がdefault.log以外の場合は404を返却
        if (!isValidLogFile(logCollection, fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
        }

        String cellId = davRsCmp.getCell().getId();
        String owner = davRsCmp.getCell().getOwner();

        // ログファイルのパスを取得
        StringBuilder logFileName = EventUtils.getEventLogDir(cellId, owner);
        logFileName.append(logCollection);
        logFileName.append(File.separator);
        logFileName.append(fileName);
        return getLog(logCollection, logFileName.toString());
    }

    private Response getLog(final String logCollection, String logFileName) {
        if (CURRENT_COLLECTION.equals(logCollection)) {
            File logFile = new File(logFileName);
            if (!logFile.isFile() || !logFile.canRead()) {
                // 何らかの理由でログが読み込めない場合でも、レスポンスボディが空で、SC_OKを返す。
                return getEmptyResponse();
            }
            try {
                final InputStream isInvariable = new FileInputStream(logFile);
                return createResponse(isInvariable);
            } catch (FileNotFoundException e) {
                throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
            }
        } else {
            ZipArchiveInputStream zipArchiveInputStream = null;
            BufferedInputStream bis = null;
            String archiveLogFileName = logFileName + ".zip";

            try {
                log.info("EventLog file path : " + archiveLogFileName);
                zipArchiveInputStream = new ZipArchiveInputStream(
                        new FileInputStream(archiveLogFileName));
                bis = new BufferedInputStream(zipArchiveInputStream);

                // ファイル内のentryを取り出す
                // 圧縮ログファイル内には1ファイルのみ格納されていることを前提としている
                ZipArchiveEntry nextZipEntry = zipArchiveInputStream.getNextZipEntry();
                if (nextZipEntry == null) {
                    IOUtils.closeQuietly(bis);
                    throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
                }
                return createResponse(bis);
            } catch (FileNotFoundException e1) {
                // 圧縮ファイルが存在しない場合は404エラーを返却
                throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
            } catch (IOException e) {
                log.info("Failed to read archive entry : " + e.getMessage());
                throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
            }
        }
    }

    private Response createResponse(final InputStream isInvariable) {
        // ステータスコードを追加
        ResponseBuilder res = Response.status(HttpStatus.SC_OK);
        res.header(HttpHeaders.CONTENT_TYPE, EventUtils.TEXT_CSV);
        res.entity(isInvariable);
        return res.build();
    }

    /**
     * イベントログが存在しない場合に返却する空レスポンスを取得する.
     * @return 空レスポンス
     */
    private Response getEmptyResponse() {
        // レスポンスの返却
        ResponseBuilder res = Response.status(HttpStatus.SC_OK);
        res.header(HttpHeaders.CONTENT_TYPE, EventUtils.TEXT_CSV);

        res.entity("");
        log.debug("main thread end.");
        return res.build();
    }

    /**
     * ログファイル削除.
     * @return レスポンス
     */
    @Path("{logCollection}/{filename}")
    @DELETE
    public final Response deleteLogFile() {
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * イベントログのCollection名チェック.
     * @param collectionName Collection名 ( "current" or "archive" )
     * @return true: 正しい、false: 誤り
     */
    protected boolean isValidLogCollection(String collectionName) {
        return CURRENT_COLLECTION.equals(collectionName)
                || ARCHIVE_COLLECTION.equals(collectionName);
    }

    /**
     * イベントログのファイル名チェック.
     * <ul>
     * <li>current: "default.log" 固定
     * <li>archive: "default.log." で始まるファイル名（実際にファイルがなければ404だが、ここではファイル名チェックのみ)
     * </ul>
     * @param collectionName Collection名 ( "current" or "archive" )
     * @param fileName ファイル名 ( "default.log" or "default.log.*" )
     * @return true: 正しい、false: 誤り
     */
    protected boolean isValidLogFile(String collectionName, String fileName) {
        if (CURRENT_COLLECTION.equals(collectionName)) {
            return DEFAULT_LOG.equals(fileName);
        } else { // コレクション名の例外は除外済み
            return fileName != null && fileName.startsWith(DEFAULT_LOG + ".");
        }
    }
}

/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.rs.box;

import java.io.InputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.wink.webdav.WebDAVMethod;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.bar.BarFileInstaller;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.model.progress.ProgressManager;
import io.personium.core.rs.cell.CellCtlResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS Resource for Box root URL.
 */
public class BoxResource {
    static Logger log = LoggerFactory.getLogger(BoxResource.class);
    String boxName;

    Cell cell;
    Box box;
    AccessContext accessContext;
    BoxRsCmp boxRsCmp;
    DavRsCmp cellRsCmp; // for box Install

    /**
     * Constructor.
     * @param cell CELL Object
     * @param boxName Box Name
     * @param cellRsCmp cellRsCmp
     * @param accessContext AccessContextオブジェクト
     * @param request HTTPリクエスト
     * @param jaxRsRequest JAX-RS用HTTPリクエスト
     */
    public BoxResource(final Cell cell, final String boxName, final AccessContext accessContext,
            final CellRsCmp cellRsCmp, final HttpServletRequest request, Request jaxRsRequest) {
        // 親はなし。パス名としてとりあえずboxNameをいれておく。
        this.cell = cell;
        this.boxName = boxName;
        // this.path= path;
        this.accessContext = accessContext;

        // Boxの存在確認
        // 本クラスではBoxが存在していることを前提としているため、Boxがない場合はエラーとする。
        // ただし、boxインストールではBoxがないことを前提としているため、以下の条件に合致する場合は処理を継続する。
        // －HTTPメソッドが MKCOL である。かつ、
        // －PathInfoが インストール先Box名 で終了している。
        // （CollectionへのMKCOLの場合があるため、boxインストールであることを確認する）
        this.box = this.cell.getBoxForName(boxName);
        // boxインストールではCellレベルで動作させる必要がある。
        this.cellRsCmp = cellRsCmp;
        if (this.box != null) {
            //BoxCmp is necessary only if this Box exists
            BoxCmp davCmp = ModelFactory.boxCmp(this.box);
            this.boxRsCmp = new BoxRsCmp(cellRsCmp, davCmp, this.accessContext, this.box);
        } else {
            //This box does not exist.
            String reqPathInfo = request.getPathInfo();
            if (!reqPathInfo.endsWith("/")) {
                reqPathInfo += "/";
            }
            String pathForBox = boxName;
            if (!pathForBox.endsWith("/")) {
                pathForBox += "/";
            }
            // Unless the HTTP method is MKCOL, respond with 404.
            if (!("MKCOL".equals(jaxRsRequest.getMethod()) && reqPathInfo.endsWith(pathForBox))) {
                throw PersoniumCoreException.Dav.BOX_NOT_FOUND.params(this.cell.getUrl() + boxName);
            }
        }

    }


    /**
     * 現在のリソースの一つ下位パスを担当するJax-RSリソースを返す.
     * @param nextPath 一つ下のパス名
     * @param request リクエスト
     * @return 下位パスを担当するJax-RSリソースオブジェクト
     */
    @Path("{nextPath}")
    public Object nextPath(@PathParam("nextPath") final String nextPath,
            @Context HttpServletRequest request) {
        return this.boxRsCmp.nextPath(nextPath, request);
    }

    /**
     * @return Box object
     */
    public Box getBox() {
        return this.box;
    }

    /**
     * @return AccessContext Object
     */
    public AccessContext getAccessContext() {
        return accessContext;
    }

    /**
     * GET リクエストの処理 .
     * @return JAX-RS Response
     */
    @GET
    public Response get() {

        // アクセス制御
        this.boxRsCmp.checkAccessContext(this.boxRsCmp.getAccessContext(), BoxPrivilege.READ);

        // キャッシュからboxインストールの非同期処理状況を取得する。
        // この際、nullが返ってきた場合は、boxインストールが実行されていないか、
        // 実行されたがキャッシュの有効期限が切れたとみなす。
        String key = "box-" + this.box.getId();
        Progress progress = ProgressManager.getProgress(key);
        if (progress == null) {
            JSONObject response = createNotRequestedResponse();
            return Response.ok().entity(response.toJSONString()).build();
        }

        String jsonString = progress.getValue();
        JSONObject jsonObj = null;
        try {
            jsonObj = (JSONObject) (new JSONParser()).parse(jsonString);
        } catch (ParseException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        }

        // キャッシュから取得できたが、boxインストールの処理状況ではない場合
        JSONObject barInfo = (JSONObject) jsonObj.get("barInfo");
        if (barInfo == null) {
            log.info("cache(" + key + "): process" + (String) jsonObj.get("process"));
            JSONObject response = createNotRequestedResponse();
            return Response.ok().entity(response.toJSONString()).build();
        }

        // boxインストールの処理状況に合わせてレスポンスを作成する。
        JSONObject response = createResponse(barInfo);
        return Response.ok().entity(response.toJSONString()).build();
    }

    /**
     * boxインストールが実行されていないか、実行されたがキャッシュの有効期限が切れた場合のレスポンスを作成する.
     * @return レスポンス用JSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private JSONObject createNotRequestedResponse() {
        JSONObject response = new JSONObject();
        response.put("status", ProgressInfo.STATUS.COMPLETED.value());
        response.put("schema", this.getBox().getSchema());

        SimpleDateFormat sdfIso8601ExtendedFormatUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdfIso8601ExtendedFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        String installedAt = sdfIso8601ExtendedFormatUtc.format(new Date(this.getBox().getPublished()));
        response.put("installed_at", installedAt);
        return response;
    }

    /**
     * boxインストールが実行されていないか、実行されたがキャッシュの有効期限が切れた場合のレスポンスを作成する.
     * @return レスポンス用JSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    private JSONObject createResponse(JSONObject values) {
        JSONObject response = new JSONObject();
        response.putAll(values);
        response.remove("cell_id");
        response.remove("box_id");
        response.put("schema", this.getBox().getSchema());
        ProgressInfo.STATUS status = ProgressInfo.STATUS.valueOf((String) values.get("status"));
        if (status == ProgressInfo.STATUS.COMPLETED) {
            response.remove("progress");
            String startedAt = (String) response.remove("started_at");
            response.put("installed_at", startedAt);
        }
        response.put("status", status.value());
        return response;
    }

    /**
     * DELETE method.
     * This endpoint is dedicated for recursive deletion.
     * @param recursiveHeader recursive header
     * @return JAX-RS response
     */
    @WriteAPI
    @DELETE
    public Response recursiveDelete(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE) final String recursiveHeader) {
        // If the X-Personium-Recursive header is not true, it is an error
        if (!Boolean.TRUE.toString().equalsIgnoreCase(recursiveHeader)) {
            throw PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
        }
        boolean recursive = Boolean.valueOf(recursiveHeader);

        // Check acl.
        boxRsCmp.checkAccessContext(boxRsCmp.getAccessContext(), CellPrivilege.BOX);

        return boxRsCmp.getDavCmp().delete(null, recursive).build();
    }

    /**
     * PROPFINDメソッドの処理.
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.boxRsCmp.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ_PROPERTIES);
        return this.boxRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_ACL);
    }

    /**
     * PROPPATCHメソッドの処理.
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WriteAPI
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // アクセス制御
        this.boxRsCmp.checkAccessContext(this.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.boxRsCmp.doProppatch(requestBodyXml);
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return this.boxRsCmp.options();
    }

    /**
     * ACLメソッドの処理. ACLの設定を行う.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        // アクセス制御
        this.boxRsCmp.checkAccessContext(this.boxRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.boxRsCmp.doAcl(reader);
    }

    /**
     * MKCOLメソッドの処理. boxインストールを行う.
     * @param uriInfo UriInfo
     * @param pCredHeader dcCredHeader
     * @param contentType Content-Typeヘッダの値
     * @param contentLength Content-Lengthヘッダの値
     * @param requestKey イベントログに出力するRequestKeyフィールドの値
     * @param inStream HttpリクエストのInputStream
     * @return JAX-RS Response
     */
    @WriteAPI
    @WebDAVMethod.MKCOL
    public Response mkcol(
            @Context final UriInfo uriInfo,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL) final String pCredHeader,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final String contentLength,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            final InputStream inStream) {

        EventBus eventBus = this.cell.getEventBus();
        String result = "";
        String schema = this.accessContext.getSchema();
        String subject = this.accessContext.getSubject();
        String object = String.format("%s:/%s", UriUtils.SCHEME_LOCALCELL, this.boxName);
        Response res = null;
        try {
            // ログファイル出力
            // X-Personium-RequestKeyの解析（指定なしの場合にデフォルト値を補充）
            requestKey = ResourceUtils.validateXPersoniumRequestKey(requestKey);
            // TODO findBugs対策↓
            log.debug(requestKey);

            if (Box.DEFAULT_BOX_NAME.equals(this.boxName)) {
                throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
            }

            // Boxを作成するためにCellCtlResource、ODataEntityResource(ODataProducer)が必要
            // この時点では "X-Personium-Credential" ヘッダーは不要なのでnullを指定する
            CellCtlResource cellctl = new CellCtlResource(this.accessContext, null, this.cellRsCmp);
            String keyName = "'" + this.boxName + "'";
            ODataEntityResource odataEntity = new ODataEntityResource(cellctl, Box.EDM_TYPE_NAME, keyName);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, contentType);
            headers.put(HttpHeaders.CONTENT_LENGTH, contentLength);

            BarFileInstaller installer =
                    new BarFileInstaller(this.cell, this.boxName, odataEntity, uriInfo);

            res = installer.barFileInstall(headers, inStream, requestKey);
            result = Integer.toString(res.getStatus());
        } catch (RuntimeException e) {
            // TODO 内部イベントの正式対応が必要
            if (e instanceof PersoniumCoreException) {
                result = Integer.toString(((PersoniumCoreException) e).getStatus());
            } else {
                result = Integer.toString(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            throw e;
        } finally {
            // post event to EventBus
            PersoniumEvent event = new PersoniumEvent(
                    schema, subject, PersoniumEventType.Category.BI, object, result, requestKey);
            eventBus.post(event);
        }
        return res;
    }

    /**
     * MOVEメソッドの処理.
     * @param headers ヘッダ情報
     * @return JAX-RS応答オブジェクト
     */
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {

        // Boxリソースに対するMOVEメソッドは使用禁止
        this.boxRsCmp.checkAccessContext(this.boxRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_BOX;
    }
}

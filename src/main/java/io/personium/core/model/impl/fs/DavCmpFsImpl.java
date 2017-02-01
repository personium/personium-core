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
package io.personium.core.model.impl.fs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.odata4j.producer.CountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.auth.token.Role;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.http.header.ByteRangeSpec;
import io.personium.core.http.header.RangeHeaderHandler;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavDestination;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.file.BinaryDataAccessor;
import io.personium.core.model.file.BinaryDataNotFoundException;
import io.personium.core.model.file.StreamingOutputForDavFile;
import io.personium.core.model.file.StreamingOutputForDavFileWithRange;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockKeyComposer;
import io.personium.core.model.lock.LockManager;
import io.personium.core.odata.PersoniumODataProducer;

/**
 * DavCmp implementation using FileSystem.
 */
public class DavCmpFsImpl implements DavCmp {
    String fsPath;
    File fsDir;

    Box box;
    Cell cell;
    ObjectFactory of;

    String name;
    Acl acl;
    DavMetadataFile metaFile;
    DavCmpFsImpl parent;
    List<String> ownerRepresentativeAccounts = new ArrayList<String>();
    boolean isPhantom = false;

    /**
     * Fixed File Name for storing file.
     */
    private static final String CONTENT_FILE_NAME = "content";
    private static final String TEMP_FILE_NAME = "tmp";

    /*
     * logger.
     */
    private static Logger log = LoggerFactory.getLogger(DavCmpFsImpl.class);

    DavCmpFsImpl() {
    }

    /**
     * constructor.
     * @param name
     *            name of the path component
     * @param parent
     *            parent DavCmp object
     * @param cell
     *            Cell
     * @param box
     *            Box
     */
    private DavCmpFsImpl(final String name, final DavCmpFsImpl parent) {
        this.name = name;
        this.of = new ObjectFactory();

        this.parent = parent;

        if (parent == null) {
            this.metaFile = DavMetadataFile.newInstance(this);
            return;
        }
        this.cell = parent.getCell();
        this.box = parent.getBox();
        this.fsPath = this.parent.fsPath + File.separator + this.name;
        this.fsDir = new File(this.fsPath);

        this.metaFile = DavMetadataFile.newInstance(this);
    }

    /**
     * create a DavCmp whose path most probably does not yet exist.
     * There still are possibilities that other thread creates the corresponding resource and
     * the path actually exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static DavCmpFsImpl createPhantom(final String name, final DavCmpFsImpl parent) {
        DavCmpFsImpl ret = new DavCmpFsImpl(name, parent);
        ret.isPhantom = true;
        return ret;
    }

    /**
     * create a DavCmp whose path most probably does exist.
     * There still are possibilities that other thread deletes the corresponding resource and
     * the path actually does not exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static DavCmpFsImpl create(final String name, final DavCmpFsImpl parent) {
        DavCmpFsImpl ret = new DavCmpFsImpl(name, parent);
        if (ret.exists()) {
            ret.load();
        } else {
            ret.isPhantom = true;
        }
        return ret;
    }


    void createNewMetadataFile() {
        this.metaFile = DavMetadataFile.prepareNewFile(this, this.getType());
        this.metaFile.save();
    }

    @Override
    public boolean isEmpty() {
        if (!this.exists()) {
            return true;
        }
        String type = this.getType();
        if (DavCmp.TYPE_COL_WEBDAV.equals(type)) {
            return !(this.getChildrenCount() > 0);
        } else if (DavCmp.TYPE_COL_BOX.equals(type)) {
            return !(this.getChildrenCount() > 0);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            // Collectionに紐づくEntityTypeの一覧を取得する
            // EntityTypeに紐づくリソース(AsssociationEndなど)はEntityTypeが必ず親となる関係であるため
            // EntityTypeのみ検索すれば、EntityTypeに紐づくリソースまでチェックする必要はない
            UserSchemaODataProducer producer = new UserSchemaODataProducer(this.cell, this);
            CountResponse cr = producer.getEntitiesCount(EntityType.EDM_TYPE_NAME, null);
            if (cr.getCount() > 0) {
                return false;
            }
            // Collectionに紐づくComplexTypeの一覧を取得する
            // ComplexTypeに紐づくリソース(ComplexTypeProperty)はComplexTypeが必ず親となる関係であるため
            // ComplexTypeのみ検索すれば、ComplexTypeに紐づくリソースまでチェックする必要はない
            cr = producer.getEntitiesCount(ComplexType.EDM_TYPE_NAME, null);
            return cr.getCount() < 1;
        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            DavCmp svcSourceCol = this.getChild(SERVICE_SRC_COLLECTION);
            if (!svcSourceCol.exists()) {
                // クリティカルなタイミングでServiceコレクションが削除された場合
                // ServiceSourceコレクションが存在しないため空とみなす
                return true;
            }
            return !(svcSourceCol.getChildrenCount() > 0);
        }
        PersoniumCoreLog.Misc.UNREACHABLE_CODE_ERROR.writeLog();
        throw PersoniumCoreException.Server.UNKNOWN_ERROR;
    }

    @Override
    public void makeEmpty() {
        // TODO Impl
    }

    /**
     * @return Acl
     */
    public Acl getAcl() {
        return this.acl;
    }

    /**
     * スキーマ認証のレベルを返す.
     * @return スキーマ認証レベル
     */
    public String getConfidentialLevel() {
        if (acl == null) {
            return null;
        }
        return this.acl.getRequireSchemaAuthz();
    }

    /**
     * ユニット昇格許可ユーザ設定を返す.
     * @return ユニット昇格許可ユーザ設定
     */
    public List<String> getOwnerRepresentativeAccounts() {
        return this.ownerRepresentativeAccounts;
    }

    /**
     * Boxをロックする.
     * @return 自ノードのロック
     */
    public Lock lock() {
        log.debug("lock:" + LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV, null, this.box.getId(), null));
        return LockManager.getLock(Lock.CATEGORY_DAV, null, this.box.getId(), null);
    }

    /**
     * @return ETag String with double quote signs.
     */
    @Override
    public String getEtag() {
        StringBuilder sb = new StringBuilder("\"");
        sb.append(this.metaFile.getVersion());
        sb.append("-");
        sb.append(this.metaFile.getUpdated());
        sb.append("\"");
        return sb.toString();
    }

    /**
     * checks if this cmp is Cell level.
     * @return true if Cell level
     */
    public boolean isCellLevel() {
        return false;
    }

    void createDir() {
        try {
            Files.createDirectories(this.fsDir.toPath());
        } catch (IOException e) {
            // Failed to create directory.
            throw new RuntimeException(e);
        }
    }

    /**
     * returns if this resource exists.<br />
     * before using this method, do not forget to load() and update the info.
     * @return true if this resource should exist
     */
    @Override
    public final boolean exists() {
        return (this.fsDir != null) && this.fsDir.exists() && this.metaFile.exists();
    }

    /**
     * load the info from FS for this Dav resouce.
     */
    public final void load() {
        this.metaFile.load();

        /*
         * Analyze JSON Object, and set metadata such as ACL.
         */
        this.name = fsDir.getName();
        this.acl = this.translateAcl(this.metaFile.getAcl());

        @SuppressWarnings("unchecked")
        Map<String, String> props = (Map<String, String>) this.metaFile.getProperties();
        if (props != null) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                int idx = key.indexOf("@");
                String elementName = key.substring(0, idx);
                String namespace = key.substring(idx + 1);
                QName keyQName = new QName(namespace, elementName);

                Element element = parseProp(val);
                String elementNameSpace = element.getNamespaceURI();
                // ownerRepresentativeAccountsの取り出し
                if (Key.PROP_KEY_OWNER_REPRESENTIVE_ACCOUNTS.equals(keyQName)) {
                    NodeList accountNodeList = element.getElementsByTagNameNS(elementNameSpace,
                            Key.PROP_KEY_OWNER_REPRESENTIVE_ACCOUNT.getLocalPart());
                    for (int i = 0; i < accountNodeList.getLength(); i++) {
                        this.ownerRepresentativeAccounts.add(accountNodeList.item(i).getTextContent().trim());
                    }
                }
            }
        }
    }


    /**
     * Davの管理データ情報を最新化する.<br />
     * 管理データが存在しない場合はエラーとする.
     */
    public final void loadAndCheckDavInconsistency() {
        load();
        if (this.metaFile == null) {
            // Boxから辿ってidで検索して、Davデータに不整合があった場合
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND;
        }
    }

    private Element parseProp(String value) {
        // valをDOMでElement化
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            ByteArrayInputStream is = new ByteArrayInputStream(value.getBytes(CharEncoding.UTF_8));
            doc = builder.parse(is);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(e1);
        }
        Element e = doc.getDocumentElement();
        return e;
    }


    /*
     * proppatch メソッドへの対応. 保存の方式 key = namespaceUri + "@" + localName Value =
     * inner XML String
     */
    @Override
    @SuppressWarnings("unchecked")
    public Multistatus proppatch(final Propertyupdate propUpdate, final String url) {
        long now = new Date().getTime();
        String reqUri = url;
        Multistatus ms = this.of.createMultistatus();
        Response res = this.of.createResponse();
        res.getHref().add(reqUri);

        // Lock
        Lock lock = this.lock();
        // 更新処理
        try {
            this.load(); // ロック後の最新情報取得

            if (!this.exists()) {
                // クリティカルなタイミング(初回ロード～ロック取得)で削除された場合は404エラーとする
                throw getNotFoundException().params(this.getUrl());
            }

            Map<String, Object> propsJson = (Map<String, Object>) this.metaFile.getProperties();
            List<Prop> propsToSet = propUpdate.getPropsToSet();

            for (Prop prop : propsToSet) {
                if (null == prop) {
                    throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
                }
                List<Element> lpe = prop.getAny();
                for (Element elem : lpe) {
                    res.setProperty(elem, HttpStatus.SC_OK);
                    String key = elem.getLocalName() + "@" + elem.getNamespaceURI();
                    String value = PersoniumCoreUtils.nodeToString(elem);
                    log.debug("key: " + key);
                    log.debug("val: " + value);
                    propsJson.put(key, value);
                }
            }

            List<Prop> propsToRemove = propUpdate.getPropsToRemove();
            for (Prop prop : propsToRemove) {
                if (null == prop) {
                    throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
                }
                List<Element> lpe = prop.getAny();
                for (Element elem : lpe) {

                    String key = elem.getLocalName() + "@" + elem.getNamespaceURI();
                    String v = (String) propsJson.get(key);
                    log.debug("Removing key: " + key);
                    if (v == null) {
                        res.setProperty(elem, HttpStatus.SC_NOT_FOUND);
                    } else {
                        propsJson.remove(key);
                        res.setProperty(elem, HttpStatus.SC_OK);
                    }
                }
            }
            // set the last updated date
            this.metaFile.setProperties((JSONObject) propsJson);
            this.metaFile.setUpdated(now);
            this.metaFile.save();
        } finally {
            lock.release();
        }
        ms.getResponse().add(res);
        return ms;
    }

    @Override
    public final ResponseBuilder acl(final Reader reader) {
        // リクエストが空でない場合、パースして適切な拡張を行う。
        Acl aclToSet = null;
        try {
            aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_CONTENT_ERROR.reason(e1);
        }
        if (!aclToSet.validateAcl(isCellLevel())) {
            throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR;
        }
        // ロック
        Lock lock = this.lock();
        try {
            // リソースのリロード
            this.load();
            if (!this.exists()) {
                throw getNotFoundException().params(this.getUrl());
            }

            // ACLのxml:baseの値を取得する
            String aclBase = aclToSet.getBase();

            // principalのhref の値を ロール名（Name） を ロールID（__id） に変換する。
            List<Ace> aceList = aclToSet.getAceList();
            if (aceList != null) {
                for (Ace ace : aceList) {
                    String pHref = ace.getPrincipalHref();
                    if (pHref != null) {
                        String id = this.cell.roleResourceUrlToId(pHref, aclBase);
                        ace.setPrincipalHref(id);
                    }
                }
            }

            JSONParser parser = new JSONParser();
            JSONObject aclJson = null;
            try {
                aclJson = (JSONObject) parser.parse(aclToSet.toJSON());
            } catch (ParseException e) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e);
            }
            // ESへxm:baseの値を登録しない TODO これでいいのか？
            aclJson.remove(KEY_ACL_BASE);
            this.metaFile.setAcl(aclJson);
            this.metaFile.save();
            // レスポンス
            return javax.ws.rs.core.Response.status(HttpStatus.SC_OK).header(HttpHeaders.ETAG, this.getEtag());
        } finally {
            lock.release();
        }
    }

    @Override
    public final ResponseBuilder putForCreate(final String contentType, final InputStream inputStream) {
        // Locking
        Lock lock = this.lock();
        try {
            // 新規作成時には、作成対象のDavNodeは存在しないため、親DavNodeをリロードして存在確認する。
            // 親DavNodeが存在しない場合：他のリクエストによって削除されたたため、404を返却
            // 親DavNodeが存在するが、作成対象のDavNodeが存在する場合：他のリクエストによって作成されたたｔめ、更新処理を実行
            this.parent.load();
            if (!this.parent.exists()) {
                throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.parent.getUrl());
            }

            // 作成対象のDavNodeが存在する場合は更新処理
            if (this.exists()) {
                return this.doPutForUpdate(contentType, inputStream, null);
            }
            // 作成対象のDavNodeが存在しない場合は新規作成処理
            return this.doPutForCreate(contentType, inputStream);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock1");
        }
    }

    @Override
    public final ResponseBuilder putForUpdate(final String contentType, final InputStream inputStream, String etag) {
        // ロック
        Lock lock = this.lock();
        try {
            // 更新には、更新対象のDavNodeが存在するため、更新対象のDavNodeをリロードして存在確認する。
            // 更新対象のDavNodeが存在しない場合：
            // ・更新対象の親DavNodeが存在しない場合：親ごと消えているため404を返却
            // ・更新対象の親DavNodeが存在する場合：他のリクエストによって削除されたたため、作成処理を実行
            // 更新対象のDavNodeが存在する場合：更新処理を実行
            this.load();
            if (this.metaFile == null) {
                this.parent.load();
                if (this.parent.metaFile == null) {
                    throw getNotFoundException().params(this.parent.getUrl());
                }
                return this.doPutForCreate(contentType, inputStream);
            }
            return this.doPutForUpdate(contentType, inputStream, etag);
        } finally {
            // ロックを開放する
            lock.release();
            log.debug("unlock2");
        }
    }

    /*
     * newly create the resource
     */
    final ResponseBuilder doPutForCreate(final String contentType, final InputStream inputStream) {
        // check the resource count
        checkChildResourceCount();

        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        try {
            // create new directory.
            Files.createDirectory(Paths.get(this.fsPath));
            // store the file content.
            File newFile = new File(this.getContentFilePath());
            Files.copy(bufferedInput, newFile.toPath());
            long writtenBytes = newFile.length();
            // create new metadata file.
            this.metaFile = DavMetadataFile.prepareNewFile(this, DavCmp.TYPE_DAV_FILE);
            this.metaFile.setContentType(contentType);
            this.metaFile.setContentLength(writtenBytes);
            this.metaFile.save();
        } catch (IOException ex) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        }
        this.isPhantom = false;
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_CREATED).header(HttpHeaders.ETAG, this.getEtag());
    }

    final ResponseBuilder doPutForUpdate(final String contentType, final InputStream inputStream, String etag) {
        // 現在時刻を取得
        long now = new Date().getTime();
        // 最新ノード情報をロード
        // TODO 全体として２回ロードしてしまうので、遅延ロードの仕組みを検討
        this.load();

        // クリティカルなタイミング(ロック～ロードまでの間)でWebDavの管理データが削除された場合の対応
        // WebDavの管理データがこの時点で存在しない場合は404エラーとする
        if (!this.exists()) {
            throw getNotFoundException().params(this.getUrl());
        }

        // 指定etagがあり、かつそれが*ではなく内部データから導出されるものと異なるときはエラー
        if (etag != null && !"*".equals(etag) && !this.getEtag().equals(etag)) {
            throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
        }

        try {
            // Update Content
            BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
            File tmpFile = new File(this.getTempContentFilePath());
            File contentFile = new File(this.getContentFilePath());
            Files.copy(bufferedInput, tmpFile.toPath());

            Files.delete(contentFile.toPath());
            Files.move(tmpFile.toPath(), contentFile.toPath());

            // Update Metadata
            this.metaFile.setUpdated(now);
            this.metaFile.setContentType(contentType);
            this.metaFile.setContentLength(contentFile.length());
            this.metaFile.save();
        } catch (IOException ex) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        }

        // response
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_NO_CONTENT).header(HttpHeaders.ETAG, this.getEtag());
    }

    @Override
    public final ResponseBuilder get(final String rangeHeaderField) {

        String contentType = this.getContentType();

        ResponseBuilder res = null;
        String fileFullPath = this.fsPath + File.separator + CONTENT_FILE_NAME;
        final long fileSize = this.getContentLength();

        // Rangeヘッダ解析処理
        final RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeaderField, fileSize);

        try {

            // Rangeヘッダ指定の時とで処理の切り分け
            if (!range.isValid()) {
                // ファイル全体返却
                StreamingOutput sout = new StreamingOutputForDavFile(fileFullPath);
                res = davFileResponse(sout, fileSize, contentType);
            } else {
                // Range対応部分レスポンス

                // Rangeヘッダの範囲チェック
                if (!range.isSatisfiable()) {
                    PersoniumCoreLog.Dav.REQUESTED_RANGE_NOT_SATISFIABLE.params(range.getRangeHeaderField()).writeLog();
                    throw PersoniumCoreException.Dav.REQUESTED_RANGE_NOT_SATISFIABLE;
                }

                if (range.getByteRangeSpecCount() > 1) {
                    // MultiPartレスポンスには未対応
                    throw PersoniumCoreException.Misc.NOT_IMPLEMENTED.params("Range-MultiPart");
                } else {
                    StreamingOutput sout = new StreamingOutputForDavFileWithRange(fileFullPath, fileSize, range);
                    res = davFileResponseForRange(sout, fileSize, contentType, range);
                }
            }
            return res.header(HttpHeaders.ETAG, this.getEtag()).header(PersoniumCoreUtils.HttpHeaders.ACCEPT_RANGES,
                    RangeHeaderHandler.BYTES_UNIT);

        } catch (BinaryDataNotFoundException nex) {
            this.load();
            if (!this.exists()) {
                throw getNotFoundException().params(this.getUrl());
            }
            throw PersoniumCoreException.Dav.DAV_UNAVAILABLE.reason(nex);
        }
    }

    /**
     * ファイルレスポンス処理.
     * @param sout
     *            StreamingOuputオブジェクト
     * @param fileSize
     *            ファイルサイズ
     * @param contentType
     *            コンテントタイプ
     * @return レスポンス
     */
    public ResponseBuilder davFileResponse(final StreamingOutput sout, long fileSize, String contentType) {
        return javax.ws.rs.core.Response.ok(sout).header(HttpHeaders.CONTENT_LENGTH, fileSize)
                .header(HttpHeaders.CONTENT_TYPE, contentType);
    }

    /**
     * ファイルレスポンス処理.
     * @param sout
     *            StreamingOuputオブジェクト
     * @param fileSize
     *            ファイルサイズ
     * @param contentType
     *            コンテントタイプ
     * @param range
     *            RangeHeaderHandler
     * @return レスポンス
     */
    private ResponseBuilder davFileResponseForRange(final StreamingOutput sout, long fileSize, String contentType,
            final RangeHeaderHandler range) {
        // MultiPartには対応しないため1個目のbyte-renge-setだけ処理する。
        int rangeIndex = 0;
        List<ByteRangeSpec> brss = range.getByteRangeSpecList();
        final ByteRangeSpec brs = brss.get(rangeIndex);

        // iPadのsafariにおいてChunkedのRangeレスポンスを処理できなかったので明にContent-Lengthを返却している。
        return javax.ws.rs.core.Response.status(HttpStatus.SC_PARTIAL_CONTENT).entity(sout)
                .header(PersoniumCoreUtils.HttpHeaders.CONTENT_RANGE, brs.makeContentRangeHeaderField())
                .header(HttpHeaders.CONTENT_LENGTH, brs.getContentLength())
                .header(HttpHeaders.CONTENT_TYPE, contentType);
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final DavCmp getChild(final String childName) {
        // if self is phantom then all children should be phantom.
        if (this.isPhantom) {
            return DavCmpFsImpl.createPhantom(childName, this);
        }
        // otherwise, child might / might not be phantom.
        return DavCmpFsImpl.create(childName, this);
    }

    @Override
    public String getType() {
        if (this.isPhantom) {
            return DavCmp.TYPE_NULL;
        }
        if (this.metaFile == null) {
            return DavCmp.TYPE_NULL;
        }
        return (String) this.metaFile.getNodeType();
    }


    @Override
    public final ResponseBuilder mkcol(final String type) {
        if (!this.isPhantom) {
            throw new RuntimeException("Bug do not call this .");
        }

        // ロック
        Lock lock = this.lock();
        try {
            // ここで改めて存在確認が必要。
            // TODO 何等かの手段で、再ロード
            this.parent.load();
            if (!this.parent.exists()) {
                // クリティカルなタイミングで先に親を削除されてしまい、
                // 親が存在しないので409エラーとする
                throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.parent.getUrl());
            }
            if (this.exists()) {
                // クリティカルなタイミングで先にコレクションを作られてしまい、
                // すでに存在するのでEXCEPTION
                throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
            }

            // コレクションの階層数のチェック
            DavCmpFsImpl current = this;
            int depth = 0;
            int maxDepth = PersoniumUnitConfig.getMaxCollectionDepth();
            while (null != current.parent) {
                current = current.parent;
                depth++;
            }
            if (depth > maxDepth) {
                // コレクション数の制限を超えたため、400エラーとする
                throw PersoniumCoreException.Dav.COLLECTION_DEPTH_ERROR;
            }

            // 親コレクション内のコレクション・ファイル数のチェック
            checkChildResourceCount();

            // Create New Directory
            Files.createDirectory(this.fsDir.toPath());
            // Create New Meta File
            this.metaFile = DavMetadataFile.prepareNewFile(this, type);
            this.metaFile.save();

            // TODO ディレクトリとメタデータつくるだけでいい？

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock");
        }
        this.isPhantom = false;

        // Response
        return javax.ws.rs.core.Response.status(HttpStatus.SC_CREATED).header(HttpHeaders.ETAG, this.getEtag());
    }

    /**
     * process MOVE operation.
     * @param etag
     *            ETag Value
     * @param overwrite
     *            whether or not overwrite the target resource
     * @param davDestination
     *            Destination information.
     * @return ResponseBuilder Response Object
     */
    @Override
    public ResponseBuilder move(String etag, String overwrite, DavDestination davDestination) {
        ResponseBuilder res = null;

        // ロック
        Lock lock = this.lock();
        try {
            // 移動元リソースの存在チェック
            this.load();
            if (!this.exists()) {
                // クリティカルなタイミング(初回ロード～ロック取得)で移動元を削除された場合。
                // 移動元が存在しないため404エラーとする
                throw getNotFoundException().params(this.getUrl());
            }
            // 指定etagがあり、かつそれが*ではなく内部データから導出されるものと異なるときはエラー
            if (etag != null && !"*".equals(etag) && !this.getEtag().equals(etag)) {
                throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
            }

            // 移動元のDavNodeをリロードしたことにより親DavNodeが別のリソースに切り替わっている可能性があるため、リロードする。
            // この際、親DavNodeが削除されている可能性もあるため、存在チェックを実施する。
            // this.parent.nodeId = this.metaFile.getParentId();
            // this.parent.load();
            // if (this.parent.metaFile == null) {
            // throw getNotFoundException().params(this.parent.getUrl());
            // }

            // 移動先のロード
            davDestination.loadDestinationHierarchy();
            // 移動先のバリデート
            davDestination.validateDestinationResource(overwrite, this);

            // MOVEメソッドでは移動元と移動先のBoxが同じであるため、移動先のアクセスコンテキストを取得しても、
            // 移動元のアクセスコンテキストを取得しても同じObjectが取得できる
            // このため、移動先のアクセスコンテキストを用いている
            AccessContext ac = davDestination.getDestinationRsCmp().getAccessContext();
            // 移動先に対するアクセス制御
            // 以下の理由により、ロック後に移動先に対するアクセス制御を行うこととした。
            // 1.アクセス制御ではESへのアクセスは発生しないため、ロック中に実施してもロック期間の長さに与える影響は少ない。
            // 2.ロック前に移動先のアクセス制御を行う場合、移動先の情報を取得する必要があり、ESへのリクエストが発生するため。
            davDestination.getDestinationRsCmp().getParent().checkAccessContext(ac, BoxPrivilege.WRITE);

            File destDir = ((DavCmpFsImpl) davDestination.getDestinationCmp()).fsDir;
            if (!davDestination.getDestinationCmp().exists()) {
                Files.move(this.fsDir.toPath(), destDir.toPath());
                res = javax.ws.rs.core.Response.status(HttpStatus.SC_CREATED);
            } else {
                FileUtils.deleteDirectory(destDir);
                Files.move(this.fsDir.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                res = javax.ws.rs.core.Response.status(HttpStatus.SC_NO_CONTENT);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock");
        }

        res.header(HttpHeaders.LOCATION, davDestination.getDestinationUri());
        res.header(HttpHeaders.ETAG, this.getEtag());
        return res;
    }

    private void checkChildResourceCount() {
        // 親コレクション内のコレクション・ファイル数のチェック
        int maxChildResource = PersoniumUnitConfig.getMaxChildResourceCount();
        if (this.parent.getChildrenCount() >= maxChildResource) {
            // コレクション内に作成可能なコレクション・ファイル数の制限を超えたため、400エラーとする
            throw PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
        }
    }

    @Override
    public final ResponseBuilder linkChild(final String childName, final String childNodeId, final Long asof) {
        return null;
    }

    @Override
    public final ResponseBuilder unlinkChild(final String childName, final Long asof) {
        return null;
    }

    /**
     * delete this resource.
     * @param ifMatch ifMatch header
     * @param recursive bool
     * @return JaxRS応答オブジェクトビルダ
     */
    @Override
    public final ResponseBuilder delete(final String ifMatch, boolean recursive) {
        // 指定etagがあり、かつそれが*ではなく内部データから導出されるものと異なるときはエラー
        if (ifMatch != null && !"*".equals(ifMatch) && !this.getEtag().equals(ifMatch)) {
            throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
        }
        // ロック
        Lock lock = this.lock();
        try {
            // リロード
            this.load();
            if (this.metaFile == null) {
                throw getNotFoundException().params(this.getUrl());
            }
            if (!recursive) {
                // WebDAVコレクションであって子孫リソースがあったら、エラーとする
                if (TYPE_COL_WEBDAV.equals(this.getType()) && this.getChildrenCount() > 0) {
                    throw PersoniumCoreException.Dav.HAS_CHILDREN;
                }
            } else {
                // TODO impl recursive
                throw PersoniumCoreException.Misc.NOT_IMPLEMENTED;
            }
            this.doDelete();
        } finally {
            // ★LOCK
            log.debug("unlock");
            lock.release();
        }
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_NO_CONTENT);
    }

    private void doDelete() {
        try {
            FileUtils.deleteDirectory(this.fsDir);
        } catch (IOException e) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(e);
        }
    }

    /**
     * バイナリデータのアクセサのインスタンスを生成して返す.
     * @return アクセサのインスタンス
     */
    protected BinaryDataAccessor getBinaryDataAccessor() {
        String owner = cell.getOwner();
        String unitUserName = null;
        if (owner == null) {
            unitUserName = AccessContext.TYPE_ANONYMOUS;
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }

        return new BinaryDataAccessor(PersoniumUnitConfig.getBlobStoreRoot(), unitUserName,
                PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());
    }

    @Override
    public final DavCmp getParent() {
        return this.parent;
    }

    @Override
    public final PersoniumODataProducer getODataProducer() {
        return ModelFactory.ODataCtl.userData(this.cell, this);
    }

    @Override
    public final PersoniumODataProducer getSchemaODataProducer(Cell cellObject) {
        return ModelFactory.ODataCtl.userSchema(cellObject, this);
    }

    @Override
    public final int getChildrenCount() {
        return this.getChildDir().length;
    }
    @Override
    public Map<String, DavCmp> getChildren() {
        Map<String, DavCmp> ret = new HashMap<>();
        File[] files = this.getChildDir();
        for (File f : files) {
            String childName = f.getName();
            ret.put(childName, this.getChild(childName));
        }
        return ret;
    }

    /*
     * retrieve child resource dir.
     */
    private File[] getChildDir() {
        File[] children = this.fsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File child) {
                if (child.isDirectory()) {
                    return true;
                }
                return false;
            }

        });
        return children;
    }

    private Acl translateAcl(JSONObject aclObj) {
        // principalのhref の値を ロールID（__id）からロールリソースURLに変換する。
        // base:xml値の設定
        String baseUrlStr = createBaseUrlStr();

        // TODO これはES検索が何度も走る重い処理であるため、必要になってはじめてやるべき
        // ここからは、一旦、はずすべきか。
        return this.roleIdToName(aclObj, baseUrlStr);
    }

    /**
     * ロールIDからロールリソースURLを取得.
     * jsonObjのロールIDをロールリソースURLに置換する
     * @param jsonObj
     *            ID置換後のJSON
     * @param baseUrlStr
     *            xml:base値
     */
    private Acl roleIdToName(Object jsonObj, String baseUrlStr) {
        Acl ret = Acl.fromJson(((JSONObject) jsonObj).toJSONString());
        List<Ace> aceList = ret.getAceList();
        if (aceList == null) {
            return ret;
        }
        // xml:base対応
        List<Ace> eraseList = new ArrayList<>();
        for (Ace ace : aceList) {
            String pHref = ace.getPrincipalHref();
            if (pHref != null) {
                // ロールIDに該当するロール名が無かった場合はロールが削除済みと判断し、無視する。
                String roloResourceUrl = this.cell.roleIdToRoleResourceUrl(pHref);
                log.debug("###" + pHref + ":" + roloResourceUrl);
                if (roloResourceUrl == null) {
                    eraseList.add(ace);
                    continue;
                }
                // base:xml値からロールリソースURLの編集
                roloResourceUrl = baseUrlToRoleResourceUrl(baseUrlStr, roloResourceUrl);
                ace.setPrincipalHref(roloResourceUrl);
            }
        }
        aceList.removeAll(eraseList);
        ret.setBase(baseUrlStr);
        return ret;
    }

    /**
     * PROPFINDのACL内のxml:base値を生成します.
     * @return
     */
    private String createBaseUrlStr() {
        String result = null;
        if (this.box != null) {
            // Boxレベル以下のACLの場合、BoxリソースのURL
            // セルURLは連結でスラッシュつけてるので、URLの最後がスラッシュだったら消す。
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.cell.getUrl().replaceFirst("/$", ""),
                    this.box.getName(), "");
        } else {
            // CellレベルのACLの場合、デフォルトBoxのリソースURL
            // セルURLは連結でスラッシュつけてるので、URLの最後がスラッシュだったら消す。
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.cell.getUrl().replaceFirst("/$", ""),
                    Box.DEFAULT_BOX_NAME, "");
        }
        return result;
    }

    /**
     * xml:baseに従ってRoleResorceUrlの整形.
     * @param baseUrlStr
     *            xml:baseの値
     * @param roloResourceUrl
     *            ロールリソースURL
     * @return
     */
    private String baseUrlToRoleResourceUrl(String baseUrlStr, String roloResourceUrlStr) {
        String result = null;
        Role baseUrl = null;
        Role roloResourceUrl = null;
        try {
            // base:xmlはロールリソースURLではないため、ダミーで「__」を追加
            baseUrl = new Role(new URL(baseUrlStr + "__"));
            roloResourceUrl = new Role(new URL(roloResourceUrlStr));
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND.reason(e);
        }
        if (baseUrl.getBoxName().equals(roloResourceUrl.getBoxName())) {
            // base:xmlのBOXとロールリソースURLのBOXが同じ場合
            result = roloResourceUrl.getName();
        } else {
            // base:xmlのBOXとロールリソースURLのBOXが異なる場合
            result = String.format(ACL_RELATIVE_PATH_FORMAT, roloResourceUrl.getBoxName(), roloResourceUrl.getName());
        }
        return result;
    }

    static final String KEY_SCHEMA = "Schema";
    static final String KEY_ACL_BASE = "@base";
    static final String ACL_RELATIVE_PATH_FORMAT = "../%s/%s";

    /**
     * @return cell id
     */
    public String getCellId() {
        return this.cell.getId();
    }

    /**
     * @return DavMetadataFile
     */
    public DavMetadataFile getDavMetadataFile() {
        return this.metaFile;
    }

    /**
     * @return FsPath
     */
    public String getFsPath() {
        return this.fsPath;
    }

    private String getContentFilePath() {
        return this.fsPath + File.separator + CONTENT_FILE_NAME;
    }

    private String getTempContentFilePath() {
        return this.fsPath + File.separator + TEMP_FILE_NAME;
    }

    /**
     * @return URL string of this Dav node.
     */
    public String getUrl() {
        // go to the top ancestor DavCmp (BoxCmp) recursively, and BoxCmp
        // overrides here and give root url.
        return this.parent.getUrl() + "/" + this.name;
    }

    /**
     * retruns NotFoundException for this resource. <br />
     * messages should vary among resource type Cell, box, file, etc..
     * Each *Cmp class should override this method and define the proper exception <br />
     * Additional info (reason etc.) for the message should be set after calling this method.
     * @return NotFoundException
     */
    public PersoniumCoreException getNotFoundException() {
        return PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
    }

    @Override
    public Cell getCell() {
        return this.cell;
    }

    @Override
    public Box getBox() {
        return this.box;
    }

    @Override
    public Long getUpdated() {
        return this.metaFile.getUpdated();
    }

    @Override
    public Long getPublished() {
        return this.metaFile.getPublished();
    }

    @Override
    public Long getContentLength() {
        return this.metaFile.getContentLength();
    }

    @Override
    public String getContentType() {
        return this.metaFile.getContentType();
    }

    @Override
    public String getId() {
        return this.metaFile.getNodeId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getProperties() {
        return this.metaFile.getProperties();
    }


}

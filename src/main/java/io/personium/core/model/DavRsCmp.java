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
package io.personium.core.model;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Creationdate;
import org.apache.wink.webdav.model.Getcontentlength;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Getlastmodified;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propfind;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.WebDAVModelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import io.personium.common.auth.token.Role;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.rs.box.DavCollectionResource;
import io.personium.core.rs.box.DavFileResource;
import io.personium.core.rs.box.PersoniumEngineSvcCollectionResource;
import io.personium.core.rs.box.NullResource;
import io.personium.core.rs.box.ODataSvcCollectionResource;
import io.personium.core.utils.ResourceUtils;

/**
 * JaxRS Resource オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class DavRsCmp {
    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(DavRsCmp.class);

    DavCmp davCmp;
    DavRsCmp parent;
    String pathName;
    ObjectFactory of;


    /**
     * constructor.
     * @param parent 親リソース
     * @param davCmp バックエンド実装に依存する処理を受け持つ部品
     */
    public DavRsCmp(final DavRsCmp parent, final DavCmp davCmp) {
        this.parent = parent;
        this.davCmp = davCmp;
        this.of = new ObjectFactory();

        if (this.davCmp != null) {
            this.pathName = this.davCmp.getName();
        }
    }

    /**
     * 現在のリソースの一つ下位パスを担当するJax-RSリソースを返す.
     * @param nextPath 一つ下のパス名
     * @param request リクエスト
     * @return 下位パスを担当するJax-RSリソースオブジェクト
     */
    public Object nextPath(final String nextPath, final HttpServletRequest request) {

        // nextPathを確認し、タイプをしらべて、new して返す
        if (this.davCmp == null) {
            return new NullResource(this, null, true);
        }
        DavCmp nextCmp = this.davCmp.getChild(nextPath);
        String type = nextCmp.getType();

        if (DavCmp.TYPE_NULL.equals(type)) {
            // 現在リソースを判断する
            if (DavCmp.TYPE_NULL.equals(this.davCmp.getType())) {
                // 現在リソースが存在しないパスの場合、次リソースから見て親リソースはNullResorce
                return new NullResource(this, nextCmp, true);
            } else {
                return new NullResource(this, nextCmp, false);
            }
        } else if (DavCmp.TYPE_COL_WEBDAV.equals(type)) {
            return new DavCollectionResource(this, nextCmp);
        } else if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            return new DavFileResource(this, nextCmp);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            return new ODataSvcCollectionResource(this, nextCmp);
        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            return new PersoniumEngineSvcCollectionResource(this, nextCmp);
        }

        return null;
    }

    /**
     * returns the URL string of this resource.
     * @return URL String
     */
    public String getUrl() {
        // 再帰的に最上位のBoxResourceまでいって、BoxResourceではここをオーバーライドしてルートURLを与えている。
        return this.parent.getUrl() + "/" + this.pathName;
    }

    /**
     * returns the Cell which this resource belongs to.
     * @return Cell Object
     */
    public Cell getCell() {
        // 再帰的に最上位のBoxResourceまでいって、そこからCellにたどりつくため、BoxResourceではここをオーバーライドしている。
        return this.parent.getCell();
    }

    /**
     * returns the Box which this resource belongs to.
     * @return Box Object
     */
    public Box getBox() {
        // 再帰的に最上位のBoxResourceまでいって、そこからCellにたどりつくため、BoxResourceではここをオーバーライドしている。
        return this.parent.getBox();
    }

    /**
     * このリソースのdavCmpを返します.
     * @return davCmp
     */
    public DavCmp getDavCmp() {
        return this.davCmp;
    }

    /**
     * このリソースのparentを返します.
     * @return DavRsCmp
     */
    public DavRsCmp getParent() {
        return this.parent;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.parent.getAccessContext();
    }

    /**
     * @param etag string
     * @return true if given string matches  the stored Etag
     */
    public boolean matchesETag(String etag) {
        if (etag == null) {
            return false;
            }
        String storedEtag = this.davCmp.getEtag();
        String weakEtag = "W/" +  storedEtag;
        return etag.equals(storedEtag) || etag.equals(weakEtag);
    }
    /**
     * Process a GET request.
     * @param ifNoneMatch ifNoneMatch header
     * @param rangeHeaderField range header
     * @return ResponseBuilder object
     */
    public final ResponseBuilder get(final String ifNoneMatch, final String rangeHeaderField) {
        // return "Not-Modified" if "If-None-Match" header matches.
        if (matchesETag(ifNoneMatch)) {
            return javax.ws.rs.core.Response.notModified().header(HttpHeaders.ETAG, this.davCmp.getEtag());
        }
        return this.davCmp.get(rangeHeaderField);
    }
    /**
     * PROPFINDの処理. バックエンド実装に依らない共通的な振る舞い.
     * @param requestBodyXml requestBody
     * @param depth Depthヘッ ダ
     * @param contentLength Content-Lengthヘッダ
     * @param transferEncoding Transfer-Encodingヘッダ
     * @param requiredForPropfind PROPFIND実行に必要なPrivilege
     * @param requiredForReadAcl ACL読み出しに必要なPrivilege
     * @return Jax-RS 応答オブジェクト
     */
    public final Response doPropfind(final Reader requestBodyXml, final String depth,
            final Long contentLength, final String transferEncoding, final Privilege requiredForPropfind,
            final Privilege requiredForReadAcl) {

        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), requiredForPropfind);

        // ユニットユーザもしくはACLのPrivilegeが設定せれている場合のみ、ACL設定の出力が可能
        boolean canAclRead = false;
        if (this.getAccessContext().isUnitUserToken()
                || this.hasPrivilege(this.getAccessContext(), requiredForReadAcl)) {
            canAclRead = true;
        }

        // リクエストをパースして pfオブジェクトを作成する
        Propfind propfind = null;
        if (ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(requestBodyXml);
                propfind = Propfind.unmarshal(br);
            } catch (Exception e1) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
            }
        } else {
            log.debug("Content-Length 0");
        }

        // Depthヘッダの有効な値は 0, 1
        // infinityの場合はサポートしないので403で返す
        if ("infinity".equals(depth)) {
            throw PersoniumCoreException.Dav.PROPFIND_FINITE_DEPTH;
        } else if (depth == null) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params("null");
        } else if (!("0".equals(depth) || "1".equals(depth))) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        String reqUri = this.getUrl();
        // 最後が/でおわるときは、それを取る
        if (reqUri.endsWith("/")) {
            reqUri = reqUri.substring(0, reqUri.length() - 1);
        }

        // リソース名がマルチバイトの場合、URLエスケープを行う
        int resourcePos = reqUri.lastIndexOf("/");
        if (resourcePos != -1) {
            String resourceName = reqUri.substring(resourcePos + 1);
            try {
                resourceName = URLEncoder.encode(resourceName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.debug("UnsupportedEncodingException");
            }
            String collectionUrl = reqUri.substring(0, resourcePos);
            reqUri = collectionUrl + "/" + resourceName;
        }

        String[] paths = reqUri.split("/");
        String nm = "";
        if (paths.length > 0) {
            nm = paths[paths.length - 1];
        }

        // 実際の処理
        final Multistatus ms = this.of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> resList = ms.getResponse();
        resList.add(createDavResponse(pathName, reqUri, this.davCmp, propfind, canAclRead));

        // if Depth is not 0, then process children.
        if (!"0".equals(depth)) {
            Map<String, DavCmp> childrenMap = this.davCmp.getChildren();
            for (String childName : childrenMap.keySet()) {
                DavCmp child = childrenMap.get(childName);
                resList.add(createDavResponse(childName, reqUri + "/" + childName, child, propfind, canAclRead));
            }
        }

        // 処理結果を出力
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException {
                Multistatus.marshal(ms, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .header(HttpHeaders.ETAG, this.davCmp.getEtag())
                .header("Content-Type", "application/xml")
                .entity(str).build();
    }

    /**
     * PROPPATCHの処理. 実サブクラスで必要に応じて呼び出すことを想定。 バックエンド実装に依らない共通的な振る舞い.
     * @param reqBodyXml requestBody
     * @return Jax-RS 応答オブジェクト
     */
    public final Response doProppatch(final Reader reqBodyXml) {

        // リクエストをパースして pu オブジェクトを作成する
        BufferedReader br = null;
        Propertyupdate pu = null;
        try {
            br = new BufferedReader(reqBodyXml);
            pu = Propertyupdate.unmarshal(br);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
        }

        // 実際の処理
        final Multistatus ms = this.davCmp.proppatch(pu, this.getUrl());

        // 処理結果を出力
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException {
                Multistatus.marshal(ms, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .header(HttpHeaders.ETAG, this.davCmp.getEtag())
                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                .entity(str).build();
    }

    /**
     * ACLメソッドの実処理. ACLの設定を行う. 実サブクラスで必要に応じて呼び出すことを想定。 バックエンド実装に依らない共通的な振る舞いをここに実装.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    public final Response doAcl(final Reader reader) {

        return this.davCmp.acl(reader).build();
    }

    /**
     * @return スキーマ認証レベル取得
     */
    public String getConfidentialLevel() {
        String confidentialStringTmp = null;
        if (this.davCmp == null) {
            confidentialStringTmp = this.parent.getConfidentialLevel();
        } else {
            confidentialStringTmp = this.davCmp.getConfidentialLevel();
        }

        if (confidentialStringTmp == null || "".equals(confidentialStringTmp)) {
            if (this.parent == null) {
                // BOXまで遡っても設定が存在しない場合はスキーマ認証は必要なしとみなす。
                return OAuth2Helper.SchemaLevel.NONE;
            }
            confidentialStringTmp = this.parent.getConfidentialLevel();
        }
        return confidentialStringTmp;
    }

    /**
     * 親のACL情報とマージし、アクセス可能か判断する.
     * @param ac アクセスコンテキスト
     * @param privilege ACLのプリビレッジ（readとかwrite）
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {

        // davCmpが無い（存在しないリソースが指定された）場合はそのリソースのACLチェック飛ばす
        if (this.davCmp != null
                && this.getAccessContext().requirePrivilege(this.davCmp.getAcl(), privilege, this.getCell().getUrl())) {
            return true;
        }

        // 親の設定をチェックする。
        if (this.parent != null && this.parent.hasPrivilege(ac, privilege)) {
            return true;
        }

        return false;
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);

        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MKCOL,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * アクセス制御を行う.
     * @param ac アクセスコンテキスト
     * @param privilege アクセス可能な権限
     */
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        // ユニットユーザトークンチェック
        if (ac.isUnitUserToken()) {
            return;
        }

        AcceptableAuthScheme allowedAuthScheme = getAcceptableAuthScheme();

        // スキーマ認証チェック
        ac.checkSchemaAccess(this.getConfidentialLevel(), this.getBox(), allowedAuthScheme);

        // Basic認証できるかチェック
        ac.updateBasicAuthenticationStateForResource(this.getBox());

        // アクセス権チェック
        if (!this.hasPrivilege(ac, privilege)) {
            // トークンの有効性チェック
            // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック

            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(allowedAuthScheme);
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), allowedAuthScheme);
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        AcceptableAuthScheme allowedAuthScheme = AcceptableAuthScheme.ALL;
        // スキーマ有のBox配下のリソースであるかチェックする
        String boxSchema = this.getBox().getSchema();
        // ボックスのスキーマが設定されている場合はBasicのWWW-Authenticateヘッダは付加しない
        if (boxSchema != null && boxSchema.length() > 0 && !Role.DEFAULT_BOX_NAME.equals(this.getBox().getName())) {
            allowedAuthScheme = AcceptableAuthScheme.BEARER;
        }
        return allowedAuthScheme;
    }

    /**
     * ユニット昇格権限設定チェック.
     * @param account チェックするアカウント
     * @return 権限の有無
     */
    public boolean checkOwnerRepresentativeAccounts(final String account) {
        List<String> ownerRepresentativeAccountsSetting = this.davCmp.getOwnerRepresentativeAccounts();
        if (ownerRepresentativeAccountsSetting == null || account == null) {
            return false;
        }

        for (String ownerRepresentativeAccount : ownerRepresentativeAccountsSetting) {
            if (account.equals(ownerRepresentativeAccount)) {
                return true;
            }
        }
        return false;
    }


    static final org.apache.wink.webdav.model.Response createDavResponse(final String pathName,
            final String href,
            final DavCmp dCmp,
            final Propfind propfind,
            final boolean isAclRead) {
        ObjectFactory of = new ObjectFactory();
        org.apache.wink.webdav.model.Response ret = of.createResponse();
        ret.getHref().add(href);

        // TODO v1.1 PROPFINDの内容によって返すものを変える
        if (propfind != null) {

            log.debug("isAllProp:" + propfind.isAllprop());
            log.debug("isPropName:" + propfind.isPropname());
        } else {
            log.debug("propfind is null");
        }

        /*
         * Displayname dn = of.createDisplayname(); dn.setValue(name); ret.setPropertyOk(dn);
         */

        Long updated = dCmp.getUpdated();
        if (updated != null) {
            Getlastmodified lm = of.createGetlastmodified();
            lm.setValue(new Date(updated));
            ret.setPropertyOk(lm);
        }
        Long published = dCmp.getPublished();
        if (published != null) {
            Creationdate cd = of.createCreationdate();
            cd.setValue(new Date(published));
            ret.setPropertyOk(cd);
        }
        String type = dCmp.getType();
        if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            // Dav リソースとしての処理
            Resourcetype rt1 = of.createResourcetype();
            ret.setPropertyOk(rt1);
            Getcontentlength gcl = new Getcontentlength();
            gcl.setValue(String.valueOf(dCmp.getContentLength()));
            ret.setPropertyOk(gcl);
            String contentType = dCmp.getContentType();
            Getcontenttype gct = new Getcontenttype();
            gct.setValue(contentType);
            ret.setPropertyOk(gct);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            // OData リソースとしての処理
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            List<Element> listElement = colRt.getAny();
            QName qname = new QName(PersoniumCoreUtils.XmlConst.NS_PERSONIUM, PersoniumCoreUtils.XmlConst.ODATA,
                    PersoniumCoreUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            listElement.add(element);
            ret.setPropertyOk(colRt);

        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            // Service リソースとしての処理
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            List<Element> listElement = colRt.getAny();
            QName qname = new QName(PersoniumCoreUtils.XmlConst.NS_PERSONIUM, PersoniumCoreUtils.XmlConst.SERVICE,
                    PersoniumCoreUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            listElement.add(element);
            ret.setPropertyOk(colRt);

        } else {
            // Col リソースとしての処理
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            ret.setPropertyOk(colRt);

        }

        // ACLの処理
        Acl acl = dCmp.getAcl();
        if (isAclRead && acl != null) {

            Document aclDoc = null;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                aclDoc = dbf.newDocumentBuilder().newDocument();
                ObjectIo.marshal(acl, aclDoc);
            } catch (Exception e) {
                throw new WebApplicationException(e);
            }
            if (aclDoc != null) {
                Element e = aclDoc.getDocumentElement();
                ret.setPropertyOk(e);
            }
        }

        Map<String, String> props = dCmp.getProperties();
        if (props != null) {
            List<String> nsList = new ArrayList<String>();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                int idx = key.indexOf("@");
                String ns = key.substring(idx + 1, key.length());

                int nsIdx = nsList.indexOf(ns);
                if (nsIdx == -1) {
                    nsList.add(ns);
                }

                Element e = parseProp(val);

                ret.setPropertyOk(e);
            }

        }
        return ret;
    }
    private static Element parseProp(String value) {
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
    /**
     * PROPFINDのACL内のxml:base値を生成します.
     * @return
     */
    private String createBaseUrlStr() {
        String result = null;
        if (!this.davCmp.isCellLevel()) {
            // Boxレベル以下のACLの場合、BoxリソースのURL
            // セルURLは連結でスラッシュつけてるので、URLの最後がスラッシュだったら消す。
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.davCmp.getCell().getUrl().replaceFirst("/$", ""),
                    this.davCmp.getBox().getName(), "");
        } else {
            // Cellレベル以下のACLの場合、デフォルトBoxのリソースURL
            // セルURLは連結でスラッシュつけてるので、URLの最後がスラッシュだったら消す。
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.davCmp.getCell().getUrl().replaceFirst("/$", ""),
                    Box.DEFAULT_BOX_NAME, "");
        }
        return result;
    }

}

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
package io.personium.core.rs.box;

import java.io.Reader;

import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.rs.odata.ODataResource;

/**
 * ODataSvcResourceを担当するJAX-RSリソース.
 */
public final class ODataSvcCollectionResource extends ODataResource {
    // DavCollectionResourceとしての機能を使うためこれをWRAPしておく。
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param davCmp DavCmp
     */
    public ODataSvcCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent.getAccessContext(), parent.getUrl() + "/" + davCmp.getName() + "/", davCmp.getODataProducer());
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * PROPFINDの処理.
     * @param requestBodyXml リクエストボディ
     * @param depth Depthヘッダ
     * @param contentLength Content-Length ヘッダ
     * @param transferEncoding Transfer-Encoding ヘッダ
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ_PROPERTIES);
        return this.davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_ACL);

    }

    /**
     * PROPPATCHの処理.
     * @param requestBodyXml リクエストボディ
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // アクセス制御
        this.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);

        return this.davRsCmp.doProppatch(requestBodyXml);
    }

    /**
     * ACLメソッドの処理. ACLの設定を行う.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    @ACL
    public Response acl(final Reader reader) {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davRsCmp.getDavCmp().acl(reader).build();
    }

    /**
     * DELETEメソッドを処理してこのリソースを削除します.
     * @return JAX-RS応答オブジェクト
     */
    @DELETE
    public Response delete() {
        // アクセス制御
        // ODataSvcCollectionResourceは必ず親(最上位はBox)を持つため、this.davRsCmp.getParent()の結果がnullになることはない
        this.davRsCmp.getParent().checkAccessContext(this.getAccessContext(), BoxPrivilege.WRITE);

        // ODataのスキーマ・データがすでにある場合、処理を失敗させる。
        if (!this.davRsCmp.getDavCmp().isEmpty()) {
            throw PersoniumCoreException.Dav.HAS_CHILDREN;
        }
        return this.davRsCmp.getDavCmp().delete(null, false).build();
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @Override
    @OPTIONS
    public Response optionsRoot() {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.DELETE,
                PersoniumCoreUtils.HttpMethod.MOVE,
                PersoniumCoreUtils.HttpMethod.PROPFIND,
                PersoniumCoreUtils.HttpMethod.PROPPATCH,
                PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * MOVEメソッドの処理.
     * @param headers ヘッダ情報
     * @return JAX-RS応答オブジェクト
     */
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {
        // 移動元に対するアクセス制御(親の権限をチェックする)
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }

    @Override
    public void checkAccessContext(AccessContext ac, Privilege privilege) {
        this.davRsCmp.checkAccessContext(ac, privilege);
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.davRsCmp.getAcceptableAuthScheme();
    }

    /**
     * アクセスコンテキストが$batchしてよい権限を持っているかを返す.
     * @param ac アクセスコンテキスト
     * @return true: アクセスコンテキストが$batchしてよい権限を持っている
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        Acl acl = this.davRsCmp.getDavCmp().getAcl();
        String url = this.davRsCmp.getCell().getUrl();
        if (ac.requirePrivilege(acl, BoxPrivilege.READ, url)) {
            return true;
        }
        if (ac.requirePrivilege(acl, BoxPrivilege.WRITE, url)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.davRsCmp.hasPrivilege(ac, privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
        ac.checkSchemaAccess(this.davRsCmp.getConfidentialLevel(), this.davRsCmp.getBox(),
                getAcceptableAuthScheme());
    }

    /**
     * basic認証できるかチェックする.
     * @param ac アクセスコンテキスト
     */
    public void setBasicAuthenticateEnableInBatchRequest(AccessContext ac) {
        ac.updateBasicAuthenticationStateForResource(this.davRsCmp.getBox());
    }

    /**
     * サービスメタデータリクエストに対応する.
     * @return JAX-RS 応答オブジェクト
     */
    @Path("{first: \\$}metadata")
    public ODataSvcSchemaResource metadata() {
        return new ODataSvcSchemaResource(this.davRsCmp, this);
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        return BoxPrivilege.READ;
    }

    @Override
    public Privilege getNecessaryWritePrivilege(String entitySetNameStr) {
        return BoxPrivilege.WRITE;
    }

    @Override
    public Privilege getNecessaryOptionsPrivilege() {
        return BoxPrivilege.READ;
    }

}

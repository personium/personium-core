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
package com.fujitsu.dc.core.rs.box;

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.wink.webdav.WebDAVMethod;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.annotations.ACL;
import com.fujitsu.dc.core.auth.BoxPrivilege;
import com.fujitsu.dc.core.model.DavCmp;
import com.fujitsu.dc.core.model.DavMoveResource;
import com.fujitsu.dc.core.model.DavRsCmp;

/**
 * プレーンなWebDAVコレクションに対応するJAX-RS Resource クラス.
 */
public final class DavCollectionResource {

    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent 親
     * @param davCmp 部品
     */
    public DavCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * GETメソッドを処理してこのリソースを取得します.
     * @return JAX-RS Response Object
     */
    @GET
    public Response get() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        StringBuilder sb = new StringBuilder();
        sb.append("URL : " + this.davRsCmp.getUrl() + "\n");
        return Response.status(HttpStatus.SC_OK).entity(sb.toString()).build();
    }

    /**
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.davRsCmp.doProppatch(requestBodyXml);
    }

    /**
     * DELETEメソッドを処理してこのリソースを削除します.
     * @param recursiveHeader recursive header
     * @return JAX-RS応答オブジェクト
     */
    @DELETE
    public Response delete(@HeaderParam(DcCoreUtils.HttpHeaders.X_DC_RECURSIVE) final String recursiveHeader) {
        boolean recursive = false;
        // X-Dc-Recursive Header
        if (recursiveHeader != null) {
            try {
                recursive = Boolean.valueOf(recursiveHeader);
            } catch (Exception e) {
                throw DcCoreException.Misc.PRECONDITION_FAILED.params(DcCoreUtils.HttpHeaders.X_DC_RECURSIVE);
            }
        }
        // アクセス制御(親の権限をチェックする)
        // DavCollectionResourceは必ず親(最上位はBox)を持つため、this.davRsCmp.getParent()の結果がnullになることはない
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        if (!this.davRsCmp.getDavCmp().isEmpty()) {
            return Response.status(HttpStatus.SC_CONFLICT).entity("delete children first").build();
        }
        return this.davRsCmp.getDavCmp().delete(null, recursive).build();
    }

    /**
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(DcCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {

        return this.davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_PROPERTIES, BoxPrivilege.READ_ACL);

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
        return this.davRsCmp.nextPath(nextPath, request);
    }

    /**
     * 405 (Method Not Allowed) - MKCOL can only be executed on a deleted/non-existent resource.
     * @return JAX-RS Response
     */
    @WebDAVMethod.MKCOL
    public Response mkcol() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw DcCoreException.Dav.METHOD_NOT_ALLOWED;
    }

    /**
     * ACLメソッドの処理. ACLの設定を行う.
     * @param reader 設定XML
     * @return JAX-RS Response
     */
    @ACL
    public Response acl(final Reader reader) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davRsCmp.doAcl(reader);
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        return DcCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MKCOL,
                com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MOVE,
                com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND,
                com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH,
                com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.ACL
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
        // DavCollectionResourceは必ず親(最上位はBox)を持つため、this.davRsCmp.getParent()の結果がnullになることはない
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }
}

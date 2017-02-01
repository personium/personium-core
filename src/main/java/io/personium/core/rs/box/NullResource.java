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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propstat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.REPORT;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavCommon;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.jaxb.Mkcol;
import io.personium.core.model.jaxb.Mkcol.RequestException;
import io.personium.core.model.jaxb.MkcolResponse;
import io.personium.core.model.jaxb.ObjectFactory;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.utils.ResourceUtils;

/**
 * Box以下の存在しないパスを担当するJAX-RSリソース.
 */
public class NullResource {
    static Logger log = LoggerFactory.getLogger(NullResource.class);

    DavRsCmp davRsCmp;
    boolean isParentNull;

    /**
     * constructor.
     * @param parent 親リソース
     * @param davCmp バックエンド実装に依存する処理を受け持つ部品
     * @param isParentNull 親がNullResourceかを判別する
     */
    public NullResource(final DavRsCmp parent, final DavCmp davCmp, final boolean isParentNull) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
        this.isParentNull = isParentNull;
    }

    /**
     * GETメソッドの処理. 404 Not Foundを返す.
     * @return 404 Not Foundを表すJax-RS Response
     */
    @GET
    public final Response get() {

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * このパスに新たなファイルを配置する.
     * @param contentType Content-Typeヘッダ
     * @param inputStream リクエストボディ
     * @return Jax-RS Responseオブジェクトト
     */
    @PUT
    public final Response put(
            @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            final InputStream inputStream) {

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        // 途中のパスが存在しないときは409エラー
        /*
         * A PUT that would result in the creation of a resource without an
         * appropriately scoped parent collection MUST fail with a 409 (Conflict).
         */

        if (!DavCommon.isValidResourceName(this.davRsCmp.getDavCmp().getName())) {
            throw PersoniumCoreException.Dav.RESOURCE_NAME_INVALID;
        }

        if (this.isParentNull) {
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.davRsCmp.getParent().getUrl());
        }

        return this.davRsCmp.getDavCmp().putForCreate(contentType, inputStream).build();
    }

    /**
     * このパスに新たなCollectionを作成する.
     * @param contentType Content-Type ヘッダ
     * @param contentLength Content-Length ヘッダ
     * @param transferEncoding Transfer-Encoding ヘッダ
     * @param inputStream リクエストボディ
     * @return JAX-RS Response
     */
    @WebDAVMethod.MKCOL
    public Response mkcol(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam("Content-Length") final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            final InputStream inputStream) {

        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        // 途中のパスが存在しないときは409エラー
        /*
         * 409 (Conflict) - A collection cannot be made at the Request-URI until one or more intermediate collections
         * have been created.
         */
        if (this.isParentNull) {
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.davRsCmp.getParent().getUrl());
        }

        if (!DavCommon.isValidResourceName(this.davRsCmp.getDavCmp().getName())) {
            throw PersoniumCoreException.Dav.RESOURCE_NAME_INVALID;
        }

        // リクエストが空なら素直にwebdavでコレクションを作成する
        if (!ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            return this.davRsCmp.getDavCmp().mkcol(DavCmp.TYPE_COL_WEBDAV).build();
        }

        // リクエストが空でない場合、パースして適切な拡張を行う。
        Mkcol mkcol = null;
        try {
            mkcol = ObjectIo.unmarshal(inputStream, Mkcol.class);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
        }
        ObjectFactory factory = new ObjectFactory();
        String colType;
        try {
            colType = mkcol.getWebdavColType();
            log.debug(colType);
            Response response = this.davRsCmp.getDavCmp().mkcol(colType).build();
            // ServiceCollectionの場合は、ServiceSource用のWebdavCollectionを生成する
            if (colType.equals(DavCmp.TYPE_COL_SVC) && response.getStatus() == HttpStatus.SC_CREATED) {
                this.davRsCmp.getDavCmp().loadAndCheckDavInconsistency();
                DavCmp srcCmp = this.davRsCmp.getDavCmp().getChild(DavCmp.SERVICE_SRC_COLLECTION);
                response = srcCmp.mkcol(DavCmp.TYPE_COL_WEBDAV).build();
            }
            return response;
            // return this.parent.mkcolChild(this.pathName, colType);
        } catch (RequestException e) {

            final MkcolResponse mr = factory.createMkcolResponse();
            Propstat stat = factory.createPropstat();
            stat.setStatus("HTTP/1.1 403 Forbidden");
            List<Prop> listProp = mkcol.getPropList();
            if (!listProp.isEmpty()) {
                stat.setProp(listProp.get(0));
            }
            org.apache.wink.webdav.model.Error error = factory.createError();
            error.setAny(factory.createValidResourceType());
            stat.setError(error);
            stat.setResponsedescription(e.getMessage());
            mr.addPropstat(stat);
            StreamingOutput str = new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    try {
                        ObjectIo.marshal(mr, os);
                    } catch (JAXBException e) {
                        throw new WebApplicationException(e);
                    }
                }
            };
            return Response.status(HttpStatus.SC_FORBIDDEN).entity(str).build();
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
        return this.davRsCmp.nextPath(nextPath, request);
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @DELETE
    public final Response delete() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @POST
    public final Response post() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @REPORT
    public final Response report() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @WebDAVMethod.PROPFIND
    public final Response propfind() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @WebDAVMethod.PROPPATCH
    public final Response proppatch() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @ACL
    public final Response acl() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUNDを返す.
     * @return Jax-RS 応答オブジェクト
     */
    @WebDAVMethod.MOVE
    public final Response move() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return this.davRsCmp.options();
    }
}

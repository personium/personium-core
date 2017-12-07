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
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.wink.webdav.WebDAVMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.impl.fs.DavCmpFsImpl;

/**
 * PersoniumEngineSvcCollectionResourceを担当するJAX-RSリソース.
 */
public final class PersoniumEngineSvcCollectionResource {
    private static Logger log = LoggerFactory.getLogger(PersoniumEngineSvcCollectionResource.class);

    DavCmp davCmp = null;
    DavCollectionResource dcr = null;
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param davCmp DavCmp
     */
    public PersoniumEngineSvcCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davCmp = davCmp;
        this.dcr = new DavCollectionResource(parent, davCmp);
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
     * DELETE method.
     * @param recursiveHeader recursive header
     * @return JAX-RS response
     */
    @WriteAPI
    @DELETE
    public Response delete(
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE) final String recursiveHeader) {
        // X-Personium-Recursive Header
        if (recursiveHeader != null
                && !"true".equalsIgnoreCase(recursiveHeader)
                && !"false".equalsIgnoreCase(recursiveHeader)) {
            throw PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE, recursiveHeader);
        }
        boolean recursive = Boolean.valueOf(recursiveHeader);
        // Check acl.(Parent acl check)
        // Since DavCollectionResource always has a parent, result of this.davRsCmp.getParent() will never be null.
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        if (!recursive && !this.davRsCmp.getDavCmp().isEmpty()) {
            throw PersoniumCoreException.Dav.HAS_CHILDREN;
        }
        return this.davCmp.delete(null, recursive).build();
    }

    /**
     * PROPPATCHの処理.
     * @param requestBodyXml リクエストボディ
     * @return JAX-RS Response
     */
    @WriteAPI
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.davRsCmp.doProppatch(requestBodyXml);
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
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davCmp.acl(reader).build();
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.DELETE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MOVE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * サービスソースを担当するJax-RSリソースを返す.
     * @return DavFileResource
     */
    @Path("__src")
    public PersoniumEngineSourceCollection src() {
        DavCmp nextCmp = this.davCmp.getChild(DavCmp.SERVICE_SRC_COLLECTION);
        if (nextCmp.exists()) {
            return new PersoniumEngineSourceCollection(this.davRsCmp, nextCmp);
        } else {
            // サービスソースコレクションが存在しないため404エラーとする
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(nextCmp.getUrl());
        }
    }

    /**
     * relay_GETメソッド.
     * @param path パス名
     * @param uriInfo URI
     * @param headers ヘッダ
     * @return JAX-RS Response
     */
    @Path("{path}")
    @GET
    public Response relayget(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.GET, uriInfo, path, headers, null);
    }

    /**
     * relay_POSTメソッド.
     * @param path パス名
     * @param uriInfo URI
     * @param headers ヘッダ
     * @param is リクエストボディ
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @POST
    public Response relaypost(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers,
            final InputStream is) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.POST, uriInfo, path, headers, is);
    }

    /**
     * relay_PUTメソッド.
     * @param path パス名
     * @param uriInfo URI
     * @param headers ヘッダ
     * @param is リクエストボディ
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @PUT
    public Response relayput(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers,
            final InputStream is) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.PUT, uriInfo, path, headers, is);
    }

    /**
     * relay_DELETEメソッド.
     * @param path パス名
     * @param uriInfo URI
     * @param headers ヘッダ
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @DELETE
    public Response relaydelete(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.DELETE, uriInfo, path, headers, null);
    }

    /**
     * relay共通処理のメソッド.
     * @param method メソッド
     * @param uriInfo URI
     * @param path パス名
     * @param headers ヘッダ
     * @param is リクエストボディ
     * @return JAX-RS Response
     */
    public Response relaycommon(
            String method,
            UriInfo uriInfo,
            String path,
            HttpHeaders headers,
            InputStream is) {

        String cellName = this.davRsCmp.getCell().getName();
        String boxName = this.davRsCmp.getBox().getName();
        String requestUrl = String.format("http://%s:%s/%s/%s/%s/service/%s", PersoniumUnitConfig.getEngineHost(),
                PersoniumUnitConfig.getEnginePort(), PersoniumUnitConfig.getEnginePath(), cellName, boxName, path);

        // baseUrlを取得
        String baseUrl = uriInfo.getBaseUri().toString();

        // リクエストヘッダを取得し、以下内容を追加
        HttpClient client = new DefaultHttpClient();
        HttpUriRequest req = null;
        if (method.equals(HttpMethod.POST)) {
            HttpPost post = new HttpPost(requestUrl);
            InputStreamEntity ise = new InputStreamEntity(is, -1);
            ise.setChunked(true);
            post.setEntity(ise);
            req = post;
        } else if (method.equals(HttpMethod.PUT)) {
            HttpPut put = new HttpPut(requestUrl);
            InputStreamEntity ise = new InputStreamEntity(is, -1);
            ise.setChunked(true);
            put.setEntity(ise);
            req = put;
        } else if (method.equals(HttpMethod.DELETE)) {
            HttpDelete delete = new HttpDelete(requestUrl);
            req = delete;
        } else {
            HttpGet get = new HttpGet(requestUrl);
            req = get;
        }

        req.addHeader("X-Baseurl", baseUrl);
        req.addHeader("X-Request-Uri", uriInfo.getRequestUri().toString());
        if (davCmp instanceof DavCmpFsImpl) {
            DavCmpFsImpl dcmp = (DavCmpFsImpl) davCmp;
            req.addHeader("X-Personium-Fs-Path", dcmp.getFsPath());
            req.addHeader("X-Personium-Fs-Routing-Id", dcmp.getCellId());
        }
        req.addHeader("X-Personium-Box-Schema", this.davRsCmp.getBox().getSchema());

        // リレイまでのヘッダを追加
        MultivaluedMap<String, String> multivalueHeaders = headers.getRequestHeaders();
        for (Iterator<Entry<String, List<String>>> it = multivalueHeaders.entrySet().iterator(); it.hasNext();) {
            Entry<String, List<String>> entry = it.next();
            String key = (String) entry.getKey();
            if (key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
            List<String> valueList = (List<String>) entry.getValue();
            for (Iterator<String> i = valueList.iterator(); i.hasNext();) {
                String value = (String) i.next();
                req.setHeader(key, value);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("【EngineRelay】 " + req.getMethod() + "  " + req.getURI());
            Header[] reqHeaders = req.getAllHeaders();
            for (int i = 0; i < reqHeaders.length; i++) {
                log.debug("RelayHeader[" + reqHeaders[i].getName() + "] : " + reqHeaders[i].getValue());
            }
        }

        // Engineにリクエストを投げる
        HttpResponse objResponse = null;
        try {
            objResponse = client.execute(req);
        } catch (ClientProtocolException e) {
            throw PersoniumCoreException.ServiceCollection.SC_INVALID_HTTP_RESPONSE_ERROR;
        } catch (Exception ioe) {
            throw PersoniumCoreException.ServiceCollection.SC_ENGINE_CONNECTION_ERROR.reason(ioe);
        }

        // ステータスコードを追加
        ResponseBuilder res = Response.status(objResponse.getStatusLine().getStatusCode());
        Header[] headersResEngine = objResponse.getAllHeaders();
        // レスポンスヘッダを追加
        for (int i = 0; i < headersResEngine.length; i++) {
            // Engineから返却されたTransfer-Encodingはリレーしない。
            // 後続のMWにてレスポンスの長さに応じてContent-LengthまたはTransfer-Encodingが付加されるので
            // 2重に付加されてしまうのを防ぐため、ここでは外しておく。
            if ("Transfer-Encoding".equalsIgnoreCase(headersResEngine[i].getName())) {
                continue;
            }
            // Engineから返却されたDateはリレーしない。
            // WebサーバのMWがJettyの場合は2重に付加されてしまうため。
            if (HttpHeaders.DATE.equalsIgnoreCase(headersResEngine[i].getName())) {
                continue;
            }
            res.header(headersResEngine[i].getName(), headersResEngine[i].getValue());
        }

        InputStream isResBody = null;

        // レスポンスボディを追加
        HttpEntity entity = objResponse.getEntity();
        if (entity != null) {
            try {
                isResBody = entity.getContent();
            } catch (IllegalStateException e) {
                throw PersoniumCoreException.ServiceCollection.SC_UNKNOWN_ERROR.reason(e);
            } catch (IOException e) {
                throw PersoniumCoreException.ServiceCollection.SC_ENGINE_CONNECTION_ERROR.reason(e);
            }
            final InputStream isInvariable = isResBody;
            // 処理結果を出力
            StreamingOutput strOutput = new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    int chr;
                    try {
                        while ((chr = isInvariable.read()) != -1) {
                            os.write(chr);
                        }
                    } finally {
                        isInvariable.close();
                    }
                }
            };
            res.entity(strOutput);
        }

        // レスポンス返却
        return res.build();
    }

    /**
     * MOVEメソッドの処理.
     * @param headers ヘッダ情報
     * @return JAX-RS応答オブジェクト
     */
    @WriteAPI
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {
        // 移動元に対するアクセス制御(親の権限をチェックする)
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }
}

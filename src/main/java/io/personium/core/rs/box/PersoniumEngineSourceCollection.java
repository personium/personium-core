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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;

/**
 * PersoniumEngineSourceCollectionResourceを担当するJAX-RSリソース.
 */
public class PersoniumEngineSourceCollection {

    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent 親リソース
     * @param davCmp バックエンド実装に依存する処理を受け持つ部品
     */
    PersoniumEngineSourceCollection(final DavRsCmp parent, final DavCmp davCmp) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
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

        DavCmp nextCmp = this.davRsCmp.getDavCmp().getChild(nextPath);
        String type = nextCmp.getType();
        if (DavCmp.TYPE_NULL.equals(type)) {
            return new PersoniumEngineSourceNullResource(this.davRsCmp, nextCmp);
        } else if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            return new PersoniumEngineSourceFileResource(this.davRsCmp, nextCmp);
        }

        // TODO Collectionタイプが不正な値の場合は5XX系で返却する
        return null;
    }

    /**
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transger-Encoding Header
     * @return JAX-RS Response
     */
    @WebDAVMethod.PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {

        return this.davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_PROPERTIES, BoxPrivilege.READ_ACL);
    }

    /**
     * MOVEの処理. <br />
     * __srcのMOVEは行えないため、一律400エラーとしている。
     */
    @WebDAVMethod.MOVE
    public void move() {
        // アクセス制御
        this.davRsCmp.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_MOVE;
    }

    /**
     * OPTIONSメソッドの処理.
     * @return JAX-RS応答オブジェクト
     */
    @OPTIONS
    public Response options() {
        // 移動元に対するアクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);
        return PersoniumCoreUtils.responseBuilderForOptions(
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND
                ).build();
    }
}

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

import java.io.InputStream;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;

/**
 * PersoniumEngineSourceNullResourceを担当するJAX-RSリソース.
 */
public class PersoniumEngineSourceNullResource extends NullResource {

    /**
     * constructor.
     * @param parent 親リソース
     * @param davCmp バックエンド実装に依存する処理を受け持つ部品
     */
    PersoniumEngineSourceNullResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent, davCmp, false);
    }

    /**
     * このパスに新たなCollectionを作成する.
     * @param contentType Content-Type ヘッダ
     * @param contentLength Content-Length ヘッダ
     * @param transferEncoding Transfer-Encoding ヘッダ
     * @param inputStream リクエストボディ
     * @return JAX-RS Response
     */
    @Override
    @WebDAVMethod.MKCOL
    public Response mkcol(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam("Content-Length") final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            final InputStream inputStream) {
        // アクセス制御
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
    }

}

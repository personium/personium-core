/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.MKCOL;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;

/**
 * JAX-RS resource responsible for PersoniumEngineSourceNullResource.
 */
public class PersoniumEngineSourceNullResource extends NullResource {

    /**
     * constructor.
     * @param parent parent resource
     * @param davCmp Parts responsible for processing dependent on backend implementation
     */
    PersoniumEngineSourceNullResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent, davCmp, false);
    }

    /**
     * Create a new Collection in this path.
     * @param contentType Content-Type header
     * @param contentLength Content-Length header
     * @param transferEncoding Transfer-Encoding header
     * @param inputStream request body
     * @return JAX-RS Response
     */
    @Override
    @MKCOL
    public Response mkcol(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam("Content-Length") final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            final InputStream inputStream) {
        //Access control
        this.davRsCmp.getParent().checkAccessContext(BoxPrivilege.BIND);
        throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
    }

}

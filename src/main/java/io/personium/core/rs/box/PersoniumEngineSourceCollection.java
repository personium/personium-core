/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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

import java.io.Reader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.REPORT;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS resource responsible for PersoniumEngineSourceCollectionResource.
 */
public class PersoniumEngineSourceCollection {

    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent parent resource
     * @param davCmp Parts responsible for processing dependent on backend implementation
     */
    PersoniumEngineSourceCollection(final DavRsCmp parent, final DavCmp davCmp) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * Returns a Jax-RS resource that is responsible for one lower-level path of the current resource.
     * @param nextPath path name one down
     * @param request request
     * @return Jax-RS resource object responsible for subordinate path
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

        //If the TODO Collection type is incorrect value, return it with 5XX type
        return null;
    }

    /**
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transger-Encoding Header
     * @return JAX-RS Response
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(BoxPrivilege.READ_PROPERTIES);
        return this.davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_ACL);
    }

    /**
     * REPORT Method.
     * @return JAX-RS response object
     */
    @REPORT
    public Response report() {
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * MOVE processing. <br />
     * Because __src MOVE can not be performed, it is set as uniform 400 errors.
     */
    @MOVE
    public void move() {
        //Access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.WRITE);
        throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_MOVE;
    }

    /**
     * Processing of OPTIONS method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        //Access control to move source
        this.davRsCmp.checkAccessContext(BoxPrivilege.READ);
        return ResourceUtils.responseBuilderForOptions(
                io.personium.common.utils.CommonUtils.HttpMethod.PROPFIND
                ).build();
    }
}

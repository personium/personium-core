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

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource class corresponding to plain WebDAV collection.
 */
public class DavCollectionResource {

    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @ param parent parent
     * @ param davCmp parts
     */
    public DavCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * Process the GET method to get this resource.
     * @return JAX-RS Response Object
     */
    @GET
    public Response get() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        StringBuilder sb = new StringBuilder();
        sb.append("URL : " + this.davRsCmp.getUrl() + "\n");
        return Response.status(HttpStatus.SC_OK).entity(sb.toString()).build();
    }

    /**
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WriteAPI
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.davRsCmp.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.davRsCmp.doProppatch(requestBodyXml);
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
                && !Boolean.TRUE.toString().equalsIgnoreCase(recursiveHeader)
                && !Boolean.FALSE.toString().equalsIgnoreCase(recursiveHeader)) {
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
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ_PROPERTIES);
        return this.davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                BoxPrivilege.READ_ACL);

    }

    /**
     * Returns a Jax-RS resource that is responsible for one lower-level path of the current resource.
     * @ param nextPath path name one down
     * @ param request request
     * @return Jax-RS resource object responsible for subordinate path
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
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
    }

    /**
     * Processing of ACL method Set ACL.
     * @ param reader configuration XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davRsCmp.doAcl(reader);
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MKCOL,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MOVE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * Processing of the MOVE method.
     * @ param headers header information
     * @return JAX-RS response object
     */
    @WriteAPI
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {
        //Access control to move source (check parent's authority)
        //Since DavCollectionResource always has a parent (the top is a Box), the result of this.davRsCmp.getParent () will never be null
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }
}

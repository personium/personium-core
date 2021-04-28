/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.MKCOL;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.PROPPATCH;
import io.personium.core.annotations.REPORT;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS Resource class corresponding to plain WebDAV collection.
 */
public class DavCollectionResource {

    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent parent
     * @param davCmp parts
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
        this.davRsCmp.checkAccessContext(BoxPrivilege.READ);

        StringBuilder sb = new StringBuilder();
        sb.append("URL : " + this.davRsCmp.getUrl() + "\n");
        return Response.status(HttpStatus.SC_OK).entity(sb.toString()).build();
    }

    /**
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WriteAPI
    @PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.WRITE_PROPERTIES);
        Response response = this.davRsCmp.doProppatch(requestBodyXml);

        // post event to EventBus
        String type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.PROPPATCH);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.davRsCmp)
                                              .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * DELETE method.
     * @param recursiveHeader recursive header
     * @return JAX-RS response
     */
    @WriteAPI
    @DELETE
    public Response delete(
            @HeaderParam(CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE) final String recursiveHeader) {
        // X-Personium-Recursive Header
        if (recursiveHeader != null
                && !Boolean.TRUE.toString().equalsIgnoreCase(recursiveHeader)
                && !Boolean.FALSE.toString().equalsIgnoreCase(recursiveHeader)) {
            throw PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE, recursiveHeader);
        }
        boolean recursive = Boolean.valueOf(recursiveHeader);
        // Check acl.(Parent acl check)
        // Since DavCollectionResource always has a parent, result of this.davRsCmp.getParent() will never be null.
        this.davRsCmp.getParent().checkAccessContext(BoxPrivilege.UNBIND);

        if (!recursive && !this.davRsCmp.getDavCmp().isEmpty()) {
            throw PersoniumCoreException.Dav.HAS_CHILDREN;
        }
        Response response = this.davRsCmp.getDavCmp().delete(null, recursive).build();

        // post event to EventBus
        String type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.DELETE);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.davRsCmp)
                                              .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS Response
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(BoxPrivilege.READ_PROPERTIES);
        Response response = this.davRsCmp.doPropfind(requestBodyXml,
                                                     depth,
                                                     contentLength,
                                                     transferEncoding,
                                                     BoxPrivilege.READ_ACL);

        // post event to EventBus
        String type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.PROPFIND);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.davRsCmp)
                                              .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(ev);

        return response;
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
     * Returns a Jax-RS resource that is responsible for one lower-level path of the current resource.
     * @param nextPath path name one down
     * @param request request
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
    @MKCOL
    public Response mkcol() {
        //Access control
        this.davRsCmp.getParent().checkAccessContext(BoxPrivilege.BIND);

        throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
    }

    /**
     * Processing of ACL method Set ACL.
     * @param reader configuration XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        //Access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.WRITE_ACL);
        Response response = this.davRsCmp.doAcl(reader);

        // post event to EventBus
        String type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.ACL);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.davRsCmp)
                                              .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
        this.davRsCmp.checkAccessContext(BoxPrivilege.READ);

        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.CommonUtils.HttpMethod.MKCOL,
                io.personium.common.utils.CommonUtils.HttpMethod.MOVE,
                io.personium.common.utils.CommonUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.CommonUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.CommonUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * Processing of the MOVE method.
     * @param headers header information
     * @return JAX-RS response object
     */
    @WriteAPI
    @MOVE
    public Response move(
            @Context HttpHeaders headers) {
        //Access control to move source (check parent's authority)
        //Since DavCollectionResource always has a parent (the top is a Box), the result of this.davRsCmp.getParent () will never be null
        this.davRsCmp.getParent().checkAccessContext(BoxPrivilege.UNBIND);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }
}

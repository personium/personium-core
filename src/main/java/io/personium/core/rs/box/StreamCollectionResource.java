/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.PROPPATCH;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS resource responsible for StreamCollectionResource.
 */
public class StreamCollectionResource {

    DavCmp davCmp = null;
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param davCmp DavCmp
     */
    public StreamCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davCmp = davCmp;
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * Processing of PROPFIND.
     * @param requestBodyXml request body
     * @param depth Depth header
     * @param contentLength Content-Length header
     * @param transferEncoding Transfer-Encoding header
     * @return JAX-RS Response
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ_PROPERTIES);
        return this.davRsCmp.doPropfind(requestBodyXml,
                                        depth,
                                        contentLength,
                                        transferEncoding,
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
        return this.davCmp.delete(null, recursive).build();
    }

    /**
     * Processing of PROPPATCH.
     * @param requestBodyXml request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.davRsCmp.doProppatch(requestBodyXml);
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
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davCmp.acl(reader).build();
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
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.DELETE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MOVE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * queue endpoint.
     * @return StreamQueueResource
     */
    @Path("queue")
    public StreamQueueResource queue() {
        return new StreamQueueResource(this.davRsCmp, "queue");
    }

    /**
     * topic endpoint.
     * @return StreamTopicResource
     */
    @Path("topic")
    public StreamTopicResource topic() {
        return new StreamTopicResource(this.davRsCmp, "topic");
    }

}
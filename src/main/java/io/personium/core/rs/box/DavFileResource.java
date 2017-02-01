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
import java.io.Reader;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.wink.webdav.WebDAVMethod;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.annotations.ACL;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;

/**
 * JAX-RS Resource class for a plain WebDAV file resource.
 */
public class DavFileResource {

    DavRsCmp davRsCmp;

    /**
     * Constructor.
     * @param parent Parent resource
     * @param davCmp DavCmp
     */
    public DavFileResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * process PUT Method and update the file.
     * @param contentType Content-Type Header
     * @param ifMatch If-Match Header
     * @param inputStream Request Body
     * @return JAX-RS response object
     */
    @PUT
    public Response put(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch,
            final InputStream inputStream) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        ResponseBuilder rb = this.davRsCmp.getDavCmp().putForUpdate(contentType, inputStream, ifMatch);
        return rb.build();
    }

    /**
     * process GET Method and retrieve the file content.
     * @param ifNoneMatch If-None-Match Header
     * @param rangeHeaderField Range header
     * @return JAX-RS response object
     */
    @GET
    public Response get(
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) final String ifNoneMatch,
            @HeaderParam("Range") final String rangeHeaderField
            ) {

        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        ResponseBuilder rb = this.davRsCmp.get(ifNoneMatch, rangeHeaderField);
        return rb.build();
    }

    /**
     * process DELETE Method and delete this resource.
     * @param ifMatch If-Match header
     * @return JAX-RS response object
     */
    @DELETE
    public Response delete(@HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        // Access Control
        // DavFileResourceは必ず親(最上位はBox)を持つため、this.davRsCmp.getParent()の結果がnullになることはない
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        ResponseBuilder rb = this.davRsCmp.getDavCmp().delete(ifMatch, false);
        return rb.build();
    }

    /**
     * process PROPPATCH Method.
     * @param requestBodyXml request body
     * @return JAX-RS response object
     */
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // Access Control
        this.davRsCmp.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        return this.davRsCmp.doProppatch(requestBodyXml);
    }

    /**
     * process PROPFIND Method.
     * @param requestBodyXml request body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS response object
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
     * process ACL Method and configure ACL.
     * @param reader request body
     * @return JAX-RS Response
     */
    @ACL
    public Response acl(final Reader reader) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davRsCmp.doAcl(reader);
    }

    /**
     * process MOVE Method.
     * @param headers Http headers
     * @return JAX-RS response object
     */
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {
        // Access Control against the move source
        // DavFileResourceは必ず親(最上位はBox)を持つため、this.davRsCmp.getParent()の結果がnullになることはない
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }

    /**
     * process OPTIONS Method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MOVE,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }
}

/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.PROPPATCH;
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
    @WriteAPI
    @PUT
    public Response put(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch,
            final InputStream inputStream) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_CONTENT);

        ResponseBuilder rb = this.davRsCmp.getDavCmp().putForUpdate(contentType, inputStream, ifMatch);
        Response res = rb.build();

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.UPDATE);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(res.getStatus());
        PersoniumEvent event = new PersoniumEvent.Builder()
                                                 .type(type)
                                                 .object(object)
                                                 .info(info)
                                                 .davRsCmp(this.davRsCmp)
                                                 .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(event);

        return res;
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
            @HeaderParam("Range") final String rangeHeaderField) {

        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        ResponseBuilder rb = this.davRsCmp.get(ifNoneMatch, rangeHeaderField);
        Response res = rb.build();

        // post event to EventBus
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(res.getStatus());
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.GET);
        PersoniumEvent event = new PersoniumEvent.Builder()
                                                 .type(type)
                                                 .object(object)
                                                 .info(info)
                                                 .davRsCmp(this.davRsCmp)
                                                 .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(event);

        return res;
    }

    /**
     * process DELETE Method and delete this resource.
     * @param ifMatch If-Match header
     * @return JAX-RS response object
     */
    @WriteAPI
    @DELETE
    public Response delete(@HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        // Access Control
        //The result of this.davRsCmp.getParent () is never null since DavFileResource always has a parent (the top is Box)
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.UNBIND);

        ResponseBuilder rb = this.davRsCmp.getDavCmp().delete(ifMatch, false);
        Response res = rb.build();

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.DELETE);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(res.getStatus());
        PersoniumEvent event = new PersoniumEvent.Builder()
                                                 .type(type)
                                                 .object(object)
                                                 .info(info)
                                                 .davRsCmp(this.davRsCmp)
                                                 .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(event);

        return res;
    }

    /**
     * process PROPPATCH Method.
     * @param requestBodyXml request body
     * @return JAX-RS response object
     */
    @WriteAPI
    @PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        Response response = this.davRsCmp.doProppatch(requestBodyXml);

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.PROPPATCH);
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
     * process PROPFIND Method.
     * @param requestBodyXml request body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS response object
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ_PROPERTIES);
        Response response = this.davRsCmp.doPropfind(requestBodyXml,
                                                     depth,
                                                     contentLength,
                                                     transferEncoding,
                                                     BoxPrivilege.READ_ACL);

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.PROPFIND);
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
     * process ACL Method and configure ACL.
     * @param reader request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        // Access Control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        Response response = this.davRsCmp.doAcl(reader);

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.ACL);
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
     * process MOVE Method.
     * @param headers Http headers
     * @return JAX-RS response object
     */
    @WriteAPI
    @MOVE
    public Response move(
            @Context HttpHeaders headers) {
        // Access Control against the move source
        //The result of this.davRsCmp.getParent () is never null since DavFileResource always has a parent (the top is Box)
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.UNBIND);

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
        return ResourceUtils.responseBuilderForOptions(
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

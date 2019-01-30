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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propstat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.personium.core.model.DavCommon;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.jaxb.Mkcol;
import io.personium.core.model.jaxb.Mkcol.RequestException;
import io.personium.core.model.jaxb.MkcolResponse;
import io.personium.core.model.jaxb.ObjectFactory;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * A JAX-RS resource that is responsible for nonexistent paths below Box.
 */
public class NullResource {
    static Logger log = LoggerFactory.getLogger(NullResource.class);

    DavRsCmp davRsCmp;
    boolean isParentNull;

    /**
     * constructor.
     * @param parent parent resource
     * @param davCmp Parts responsible for processing dependent on backend implementation
     * @param isParentNull Determine if the parent is a NullResource
     */
    public NullResource(final DavRsCmp parent, final DavCmp davCmp, final boolean isParentNull) {
        this.davRsCmp = new DavRsCmp(parent, davCmp);
        this.isParentNull = isParentNull;
    }

    /**
     * GET method processing 404 Not Found.
     * @return 404 Jax-RS Response representing Not Found
     */
    @GET
    public final Response get() {

        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * Place a new file in this path.
     * @param contentType Content-Type header
     * @param inputStream request body
     * @return Jax-RS Response object
     */
    @WriteAPI
    @PUT
    public final Response put(
            @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            final InputStream inputStream) {

        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        //If there is no intermediate path 409 error
        /*
         * A PUT that would result in the creation of a resource without an
         * appropriately scoped parent collection MUST fail with a 409 (Conflict).
         */

        if (!DavCommon.isValidResourceName(this.davRsCmp.getDavCmp().getName())) {
            throw PersoniumCoreException.Dav.RESOURCE_NAME_INVALID;
        }

        if (this.isParentNull) {
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.davRsCmp.getParent().getUrl());
        }

        Response response = this.davRsCmp.getDavCmp().putForCreate(contentType, inputStream).build();

        // post event to EventBus
        String type = PersoniumEventType.webdav(PersoniumEventType.Operation.CREATE);
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        String info = Integer.toString(response.getStatus());
        PersoniumEvent event = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .davRsCmp(this.davRsCmp)
                .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(event);

        return response;
    }

    private void postEvent(String colType, String info) {
        String type;
        if (DavCmp.TYPE_COL_WEBDAV.equals(colType)) {
            type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.MKCOL);
        } else if (DavCmp.TYPE_COL_ODATA.equals(colType)) {
            type = PersoniumEventType.odatacol(PersoniumEventType.Operation.MKCOL);
        } else if (DavCmp.TYPE_COL_SVC.equals(colType)) {
            type = PersoniumEventType.servicecol(PersoniumEventType.Operation.MKCOL);
        } else if (DavCmp.TYPE_COL_STREAM.equals(colType)) {
            type = PersoniumEventType.streamcol(PersoniumEventType.Operation.MKCOL);
        } else {
            type = PersoniumEventType.webdavcol(PersoniumEventType.Operation.MKCOL);
        }
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                                                                  this.davRsCmp.getUrl());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.davRsCmp)
                                              .build();
        EventBus eventBus = this.davRsCmp.getCell().getEventBus();
        eventBus.post(ev);
    }

    /**
     * Create a new Collection in this path.
     * @param contentType Content-Type header
     * @param contentLength Content-Length header
     * @param transferEncoding Transfer-Encoding header
     * @param inputStream request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @MKCOL
    public Response mkcol(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam("Content-Length") final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            final InputStream inputStream) {

        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        //If there is no intermediate path 409 error
        /*
         * 409 (Conflict) - A collection cannot be made at the Request-URI until one or more intermediate collections
         * have been created.
         */
        if (this.isParentNull) {
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.davRsCmp.getParent().getUrl());
        }

        if (!DavCommon.isValidResourceName(this.davRsCmp.getDavCmp().getName())) {
            throw PersoniumCoreException.Dav.RESOURCE_NAME_INVALID;
        }

        //If request is empty obediently create collection with webdav
        if (!ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            Response response = this.davRsCmp.getDavCmp().mkcol(DavCmp.TYPE_COL_WEBDAV).build();
            postEvent(DavCmp.TYPE_COL_WEBDAV, Integer.toString(response.getStatus()));
            return response;
        }

        //If the request is not empty, parse and do the appropriate extension.
        Mkcol mkcol = null;
        try {
            mkcol = ObjectIo.unmarshal(inputStream, Mkcol.class);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
        }
        ObjectFactory factory = new ObjectFactory();
        String colType;
        try {
            colType = mkcol.getWebdavColType();
            log.debug(colType);
            Response response = this.davRsCmp.getDavCmp().mkcol(colType).build();
            //For ServiceCollection, create a WebdavCollection for ServiceSource
            if (colType.equals(DavCmp.TYPE_COL_SVC) && response.getStatus() == HttpStatus.SC_CREATED) {
                this.davRsCmp.getDavCmp().loadAndCheckDavInconsistency();
                DavCmp srcCmp = this.davRsCmp.getDavCmp().getChild(DavCmp.SERVICE_SRC_COLLECTION);
                response = srcCmp.mkcol(DavCmp.TYPE_COL_WEBDAV).build();
            }
            postEvent(colType, Integer.toString(response.getStatus()));
            return response;
            // return this.parent.mkcolChild(this.pathName, colType);
        } catch (RequestException e) {

            final MkcolResponse mr = factory.createMkcolResponse();
            Propstat stat = factory.createPropstat();
            stat.setStatus("HTTP/1.1 403 Forbidden");
            List<Prop> listProp = mkcol.getPropList();
            if (!listProp.isEmpty()) {
                stat.setProp(listProp.get(0));
            }
            org.apache.wink.webdav.model.Error error = factory.createError();
            error.setAny(factory.createValidResourceType());
            stat.setError(error);
            stat.setResponsedescription(e.getMessage());
            mr.addPropstat(stat);
            StreamingOutput str = new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    try {
                        ObjectIo.marshal(mr, os);
                    } catch (JAXBException e) {
                        throw new WebApplicationException(e);
                    }
                }
            };
            return Response.status(HttpStatus.SC_FORBIDDEN).entity(str).build();
        }
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
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @DELETE
    public final Response delete() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @POST
    public final Response post() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @REPORT
    public final Response report() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @PROPFIND
    public final Response propfind() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.READ_PROPERTIES);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @PROPPATCH
    public final Response proppatch() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @ACL
    public final Response acl() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * 404 NOT FOUND is returned.
     * @return Jax-RS response object
     */
    @MOVE
    public final Response move() {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);

        throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(this.davRsCmp.getUrl());
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return this.davRsCmp.options();
    }
}

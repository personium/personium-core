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

import java.io.Reader;

import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.wink.webdav.WebDAVMethod;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.rs.odata.ODataResource;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 *JAX-RS resource in charge of ODataSvcResource.
 */
public final class ODataSvcCollectionResource extends ODataResource {
    //WRAP this to use the function as DavCollectionResource.
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param davCmp DavCmp
     */
    public ODataSvcCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        super(parent.getAccessContext(),
                UriUtils.convertSchemeFromHttpToLocalCell(parent.getCell().getUrl(),
                        parent.getUrl() + "/" + davCmp.getName() + "/"),
                davCmp.getODataProducer());
        this.davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     *Processing of PROPFIND.
     *@ param requestBodyXml request body
     *@ param depth Depth header
     *@ param contentLength Content-Length header
     *@ param transferEncoding Transfer-Encoding header
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
     *Processing of PROPPATCH.
     *@ param requestBodyXml request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @WebDAVMethod.PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.checkAccessContext(
                this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);

        return this.davRsCmp.doProppatch(requestBodyXml);
    }

    /**
     *Processing of ACL method Set ACL.
     *@ param reader configuration XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        //Access control
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.WRITE_ACL);
        return this.davRsCmp.getDavCmp().acl(reader).build();
    }

    /**
     * DELETE method.
     * @param recursiveHeader X-Personium-Recursive Header
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
        // Check acl.
        // Since ODataSvcCollectionResource always has a parent, result of this.davRsCmp.getParent() will never be null.
        this.davRsCmp.getParent().checkAccessContext(this.getAccessContext(), BoxPrivilege.WRITE);

        // If OData schema/data already exists, an error
        if (!recursive && !this.davRsCmp.getDavCmp().isEmpty()) {
            throw PersoniumCoreException.Dav.HAS_CHILDREN;
        }
        return this.davRsCmp.getDavCmp().delete(null, recursive).build();
    }

    /**
     *OPTIONS method.
     * @return JAX-RS Response
     */
    @Override
    @OPTIONS
    public Response optionsRoot() {
        //Access control
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.DELETE,
                PersoniumCoreUtils.HttpMethod.MOVE,
                PersoniumCoreUtils.HttpMethod.PROPFIND,
                PersoniumCoreUtils.HttpMethod.PROPPATCH,
                PersoniumCoreUtils.HttpMethod.ACL
                ).build();
    }

    /**
     *Processing of the MOVE method.
     *@ param headers header information
     *@return JAX-RS response object
     */
    @WriteAPI
    @WebDAVMethod.MOVE
    public Response move(
            @Context HttpHeaders headers) {
        //Access control to move source (check parent's authority)
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }

    @Override
    public void checkAccessContext(AccessContext ac, Privilege privilege) {
        this.davRsCmp.checkAccessContext(ac, privilege);
    }

    /**
     *Obtain Auth Scheme that can be used for authentication.
     *Autret Scheme that can be used for @return authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.davRsCmp.getAcceptableAuthScheme();
    }

    /**
     *Returns whether the access context has permission to $ batch.
     *@ param ac access context
     *@return true: The access context has permission to $ batch
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        Acl acl = this.davRsCmp.getDavCmp().getAcl();
        String url = this.davRsCmp.getCell().getUrl();
        if (ac.requirePrivilege(acl, BoxPrivilege.READ, url)) {
            return true;
        }
        if (ac.requirePrivilege(acl, BoxPrivilege.WRITE, url)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.davRsCmp.hasPrivilege(ac, privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
        ac.checkSchemaAccess(this.davRsCmp.getConfidentialLevel(), this.davRsCmp.getBox(),
                getAcceptableAuthScheme());
    }

    /**
     *basic Check if authentication can be done.
     *@ param ac access context
     */
    public void setBasicAuthenticateEnableInBatchRequest(AccessContext ac) {
        ac.updateBasicAuthenticationStateForResource(this.davRsCmp.getBox());
    }

    /**
     *Corresponds to the service metadata request.
     *@return JAX-RS response object
     */
    @Path("{first: \\$}metadata")
    public ODataSvcSchemaResource metadata() {
        return new ODataSvcSchemaResource(this.davRsCmp, this);
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        return BoxPrivilege.READ;
    }

    @Override
    public Privilege getNecessaryWritePrivilege(String entitySetNameStr) {
        return BoxPrivilege.WRITE;
    }

    @Override
    public Privilege getNecessaryOptionsPrivilege() {
        return BoxPrivilege.READ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(String entitySet, String object, String info, String op) {
        String type = PersoniumEventType.odata(entitySet, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postLinkEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.odataLink(src, target, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postNavPropEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.odataNavProp(src, target, op);
        postEventInternal(type, object, info);
    }

    private void postEventInternal(String type, String object, String info) {
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .davRsCmp(this.davRsCmp)
                .build();
        EventBus eventBus = this.getAccessContext().getCell().getEventBus();
        eventBus.post(ev);
    }

}

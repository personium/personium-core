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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.resources.OptionsQueryParser;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.MERGE;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.odata.PersoniumOptionsQueryParser;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * A JAX-RS resource that handles the Entity resource (id specified URL) of OData.
 */
public class ODataEntityResource extends AbstractODataResource {

    // public static final String PATH_CELL_ID = "cellid";

    private final String keyString;
    private final ODataResource odataResource;
    private final AccessContext accessContext;

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return accessContext;
    }

    /**
     * @return the odataResource
     */
    public ODataResource getOdataResource() {
        return odataResource;
    }

    private OEntityKey oEntityKey;

    /**
     * @return the odataResource
     */
    public OEntityKey getOEntityKey() {
        return this.oEntityKey;
    }

    /**
     * The OEntityId object of the OData resource this resource is responsible for.
     * @return OEntityId object
     */
    public OEntityId getOEntityId() {
        return OEntityIds.create(getEntitySetName(), this.oEntityKey);
    }

    /**
     * constructor.
     */
    public ODataEntityResource() {
        this.odataResource = null;
        this.accessContext = null;
        this.keyString = null;
        this.oEntityKey = null;
    }

    /**
     * constructor.
     * @param odataResource ODataResource which is the parent resource
     * @param entitySetName EntitySet Name
     * @param key key string
     */
    public ODataEntityResource(final ODataResource odataResource, final String entitySetName, final String key) {
        this.odataResource = odataResource;
        this.accessContext = this.odataResource.accessContext;
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);

        //Complex key correspondence
        //If null is specified, parsing will fail, so if the null value is set it will be replaced with a dummy key
        this.keyString = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(key);

        try {
            this.oEntityKey = OEntityKey.parse(this.keyString);
        } catch (IllegalArgumentException e) {
            throw PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.reason(e);
        }

        EdmDataServices metadata = getOdataProducer().getMetadata();
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(entitySetName);
        if (edmEntitySet == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
        }

        // normalize entitykey
        this.oEntityKey = AbstractODataResource.normalizeOEntityKey(this.oEntityKey, edmEntitySet);

        EdmEntityType edmEntityType = edmEntitySet.getType();
        validatePrimaryKey(oEntityKey, edmEntityType);
    }

    /**
     * Costructor for derrived class.
     * @param odataResource ODataResource object
     * @param entitySetName name of the entityset
     * @param keyString  key string processed already
     * @param oEntityKey normalized OEntityKey object
     */
    protected ODataEntityResource(final ODataResource odataResource,
            final String entitySetName, final String keyString, final OEntityKey oEntityKey) {
        this.odataResource = odataResource;
        this.accessContext = this.odataResource.accessContext;
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);
        this.keyString = keyString;
        this.oEntityKey = oEntityKey;
    }

    /**
     * Processing of GET method.
     * @param uriInfo UriInfo
     * @param accept Accept header
     * @param ifNoneMatch If-None-Match header
     * @param format $ format parameter
     * @param expand $ expand parameter
     * @param select $ select parameter
     * @return JAX-RSResponse
     */
    @GET
    public Response get(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) String accept,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) String ifNoneMatch,
            @QueryParam("$format") String format,
            @QueryParam("$expand") String expand,
            @QueryParam("$select") String select) {
        //Access control
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);

        //Determining the output format from the values ​​of $ format and Accept header
        MediaType contentType = decideOutputFormat(accept, format);
        String outputFormat = FORMAT_JSON;
        if (MediaType.APPLICATION_ATOM_XML_TYPE.equals(contentType)) {
            outputFormat = FORMAT_ATOM;
        }

        //Ask Producer to acquire Entity
        EntityResponse entityResp = getEntity(expand, select, resUriInfo);
        String respStr = renderEntityResponse(resUriInfo, entityResp, outputFormat, null);

        //Escape processing of control code
        respStr = escapeResponsebody(respStr);

        ResponseBuilder rb = Response.ok().type(contentType);
        rb.header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        //When ETag is formally implemented, it needs to be returned
        OEntity entity = entityResp.getEntity();
        String etag = null;
        //Basically enter this IF statement.
        if (entity instanceof OEntityWrapper) {
            OEntityWrapper oew = (OEntityWrapper) entity;

            //Determining accessibility for each entity
            this.odataResource.checkAccessContextPerEntity(this.accessContext, oew);

            etag = oew.getEtag();
            //Basically enter this IF statement.
            if (etag != null) {
                //When the If-None-Match header is specified
                if (ifNoneMatch != null && ifNoneMatch.equals(ODataResource.renderEtagHeader(etag))) {
                    return Response.notModified().build();
                }
                //Granting ETag header
                rb.header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag));
            }
        }
        Response res = rb.entity(respStr).build();

        // post event to EventBus
        String key = AbstractODataResource.replaceDummyKeyToNull(this.oEntityKey.toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(getEntitySetName())
                .append(key)
                .toString();
        String info = new StringBuilder(Integer.toString(res.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.GET);

        return res;
    }

    /**
     * Ask Producer to acquire Entity.
     * @param expand expand
     * @param select select
     * @param resUriInfo UriInfo
     * @return EntityResponse
     */
    EntityResponse getEntity(String expand, String select, UriInfo resUriInfo) {
        EntityQueryInfo queryInfo = null;

        if (resUriInfo != null) {
            queryInfo = queryInfo(expand, select, resUriInfo);
        }
        EntityResponse entityResp = getOdataProducer().getEntity(getEntitySetName(), this.oEntityKey, queryInfo);
        return entityResp;
    }

    EntityQueryInfo queryInfo(String expand, String select, UriInfo resUriInfo) {
        List<EntitySimpleProperty> selects = null;
        List<EntitySimpleProperty> expands = null;

        // $select
        if ("".equals(select)) {
            throw PersoniumCoreException.OData.SELECT_PARSE_ERROR;
        }
        if ("*".equals(select)) {
            select = null;
        }
        try {
            selects = PersoniumOptionsQueryParser.parseSelect(select);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.SELECT_PARSE_ERROR.reason(e);
        }

        try {
            expands = PersoniumOptionsQueryParser.parseExpand(expand);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.EXPAND_PARSE_ERROR.reason(e);
        }
        //Check upper limit of the number of properties specified for $ expand
        if (expands != null && expands.size() > PersoniumUnitConfig.getExpandPropertyMaxSizeForRetrieve()) {
            throw PersoniumCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED;
        }

        EntityQueryInfo queryInfo = new EntityQueryInfo(null,
                OptionsQueryParser.parseCustomOptions(resUriInfo),
                expands,
                selects);
        return queryInfo;
    }

    /**
     * Processing of PUT method.
     * @param reader request body
     * @param accept Accept header
     * @param ifMatch If-Match header
     * @return JAX-RSResponse
     */
    @WriteAPI
    @PUT
    public Response put(Reader reader,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {

        //Method execution feasibility check
        checkNotAllowedMethod();

        //Access control
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        String etag;

        //Ask Producer to update the request
        OEntityWrapper oew = updateEntity(reader, ifMatch);

        //If there are no exceptions, return a response.
        //Return ETag newly registered in oew
        etag = oew.getEtag();
        Response res = Response.noContent()
                .header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag))
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

        // post event to EventBus
        String key = AbstractODataResource.replaceDummyKeyToNull(this.oEntityKey.toKeyString());
        String object = this.odataResource.getRootUrl() + getEntitySetName() + key;
        // set new entitykey's string to Info
        String newKey = AbstractODataResource.replaceDummyKeyToNull(oew.getEntityKey().toKeyString());
        String info = Integer.toString(res.getStatus()) + "," + newKey;
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.UPDATE);

        return res;
    }

    /**
     * Ask Producer to update the request.
     * @param reader request body
     * @param ifMatch ifMatch
     * @return OEntityWrapper
     */
    OEntityWrapper updateEntity(Reader reader, final String ifMatch) {
        //Create an OEntityWrapper from the request.
        OEntity oe = this.createRequestEntity(reader, this.oEntityKey);
        OEntityWrapper oew = new OEntityWrapper(null, oe, null);

        //Process of attaching meta information if necessary
        this.odataResource.beforeUpdate(oew, this.oEntityKey);

        //Set the ETag entered in the If-Match header to OEntityWrapper for collision detection for MVCC.
        String etag = ODataResource.parseEtagHeader(ifMatch);
        oew.setEtag(etag);

        //Ask UPDATE processing to ODataProducer.
        //We will also check the existence of resources here.
        getOdataProducer().updateEntity(getEntitySetName(), this.oEntityKey, oew);
        return oew;
    }

    /**
     * Processing of MERGE method.
     * @param reader request body
     * @param accept Accept header
     * @param ifMatch If-Match header
     * @return JAX-RSResponse
     */
    @WriteAPI
    @MERGE
    public Response merge(Reader reader,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        ODataMergeResource oDataMergeResource = new ODataMergeResource(this.odataResource, this.getEntitySetName(),
                this.keyString, this.oEntityKey);
        return oDataMergeResource.merge(reader, accept, ifMatch);
    }

    /**
     * Processing of DELETE method.
     * @param accept Accept header
     * @param ifMatch If-Match header
     * @return JAX-RS Response
     */
    @WriteAPI
    @DELETE
    public Response delete(
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        //Access control
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        deleteEntity(ifMatch);
        Response res = Response.noContent()
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

        // post event to EventBus
        String key = AbstractODataResource.replaceDummyKeyToNull(this.oEntityKey.toKeyString());
        String object = this.odataResource.getRootUrl() + getEntitySetName() + key;
        String info = Integer.toString(res.getStatus());
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.DELETE);

        return res;
    }

    /**
     * Ask Producer to delete Entity.
     * @param ifMatch
     */
    void deleteEntity(final String ifMatch) {
        //Pre-deletion process
        this.odataResource.beforeDelete(getEntitySetName(), this.oEntityKey);
        String etag = ODataResource.parseEtagHeader(ifMatch);

        //Deletion processing
        PersoniumODataProducer op = this.getOdataProducer();
        op.deleteEntity(getEntitySetName(), this.oEntityKey, etag);

        //Delete processing
        this.odataResource.afterDelete(getEntitySetName(), this.oEntityKey);
    }

    /**
     * Processing the path $ links / {navProp}.
     * Delegate processing to ODataLinksResource.
     * @param targetNavProp Navigation Property
     * @return ODataLinksResource object
     */
    @Path("{first: \\$}links/{targetNavProp:.+?}")
    public ODataLinksResource links(@PathParam("targetNavProp") final String targetNavProp) {
        OEntityId oeId = OEntityIds.create(getEntitySetName(), this.oEntityKey);
        return new ODataLinksResource(this.odataResource, oeId, targetNavProp, null);
    }

    /**
     * Processing the path $ links / {navProp} ({targetKey}).
     * Delegate processing to ODataLinksResource.
     * @param targetNavProp target NavigationPropert
     * @param targetId ID of the target
     * @return ODataLinksResource object
     */
    @Path("{first: \\$}links/{targetNavProp:.+?}({targetId})")
    public ODataLinksResource link(@PathParam("targetNavProp") final String targetNavProp,
            @PathParam("targetId") final String targetId) {
        OEntityKey targetEntityKey = null;
        try {
            if (targetId != null && !targetId.isEmpty()) {
                //Complex key correspondence
                //If null is specified, parsing will fail, so if the null value is set it will be replaced with a dummy key
                String targetKey = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(targetId);
                targetEntityKey = OEntityKey.parse(targetKey);
            }
        } catch (IllegalArgumentException e) {
            throw PersoniumCoreException.OData.ENTITY_KEY_LINKS_PARSE_ERROR.reason(e);
        }
        OEntityId oeId = OEntityIds.create(getEntitySetName(), this.oEntityKey);
        return new ODataLinksResource(this.odataResource, oeId, targetNavProp, targetEntityKey);
    }

    /**
     * Skip the processing on the path {navProp:. +} to ODataPropertyResource.
     * @param navProp Navigation Property
     * @return ODataPropertyResource Object
     */
    @Path("{navProp: _.+}")
    public ODataPropertyResource getNavProperty(@PathParam("navProp") final String navProp) {
        return new ODataPropertyResource(this, navProp);
    }

    /**
     * It is 404 because NavigationProperty can not specify ID.
     * @param navProp Navigation Property
     * @param targetId ID of the target
     * @return ODataPropertyResource Object
     */
    @Path("{navProp: _.+}({targetId})")
    public ODataPropertyResource getNavProperty(@PathParam("navProp") final String navProp,
            @PathParam("targetId") final String targetId) {
        throw PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                CommonUtils.HttpMethod.MERGE,
                HttpMethod.DELETE
                ).build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        this.odataResource.validate(getEntitySetName(), props);
    }

    /**
     * Method execution feasibility check.
     */
    protected void checkNotAllowedMethod() {
        if (ReceivedMessage.EDM_TYPE_NAME.equals(getEntitySetName())
                || SentMessage.EDM_TYPE_NAME.equals(getEntitySetName())) {
            throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        }
    }
}

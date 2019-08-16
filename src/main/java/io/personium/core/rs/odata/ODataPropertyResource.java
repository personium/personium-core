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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumFormatWriterFactory;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * Resource handling the Navigation property.
 * Jax-RS Resource.
 */
public class ODataPropertyResource extends AbstractODataResource {

    private final ODataResource sourceOData;
    private final OEntityId sourceEntityId;
    private final String targetNavProp;
    private final EdmEntitySet targetEntitySet;
    private final AccessContext accessContext;
    private final ODataResource odataResource;

    /**
     * constructor.
     * @param entityResource parent resource
     * @param targetNavProp Navigation Property
     */
    public ODataPropertyResource(
            final ODataEntityResource entityResource,
            final String targetNavProp) {
        this.targetNavProp = targetNavProp;
        this.sourceOData = entityResource.getOdataResource();
        this.sourceEntityId = entityResource.getOEntityId();
        setOdataProducer(entityResource.getOdataProducer());
        this.accessContext = entityResource.getAccessContext();
        this.odataResource = entityResource.getOdataResource();
        //Confirm existence of Navigation property on schema
        EdmEntitySet eSet = getOdataProducer().getMetadata().findEdmEntitySet(this.sourceEntityId.getEntitySetName());
        EdmNavigationProperty enp = eSet.getType().findNavigationProperty(this.targetNavProp);
        if (enp == null) {
            throw PersoniumCoreException.OData.NOT_SUCH_NAVPROP;
        }
        //Prepare EntityKey, EdmEntitySet of Target
        EdmEntityType tgtType = enp.getToRole().getType();
        this.targetEntitySet = getOdataProducer().getMetadata().findEdmEntitySet(tgtType.getName());
    }

    /**
     * Processing to the POST method.
     * @param uriInfo UriInfo
     * @param accept Accept header
     * @param requestKey X-Personium-RequestKey Header
     * @param format $ format query
     * @param reader request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public final Response postEntity(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(CommonUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @DefaultValue(FORMAT_JSON) @QueryParam("$format") final String format,
            final Reader reader) {
        //Access control
        this.checkWriteAccessContext();

        OEntityWrapper oew = createEntityFromInputStream(reader);
        EntityResponse res = getOdataProducer().createNp(this.sourceEntityId, this.targetNavProp,
                oew, getEntitySetName());
        if (res == null || res.getEntity() == null) {
            return Response.status(HttpStatus.SC_PRECONDITION_FAILED).entity("conflict").build();
        }

        OEntity ent = res.getEntity();
        String etag = oew.getEtag();

        //When version is updated, update etag
        if (etag != null && ent instanceof OEntityWrapper) {
            ((OEntityWrapper) ent).setEtag(etag);
        }

        //Currently, ContentType is fixed to JSON
        MediaType outputFormat = this.decideOutputFormat(accept, format);
        //Render Entity Response
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);
        UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 2);
        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());

        String responseStr = renderEntityResponse(resUriInfo, res, format, contentTypes);

        //Escape processing of control code
        responseStr = escapeResponsebody(responseStr);
        ResponseBuilder rb = getPostResponseBuilder(ent, outputFormat, responseStr, resUriInfo, key);
        Response ret = rb.build();

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntityId.getEntityKey().toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(this.sourceEntityId.getEntitySetName())
                .append(srcKey)
                .append("/")
                .append(this.targetNavProp)
                .append(key)
                .toString();
        String info = new StringBuilder(Integer.toString(ret.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        String op = PersoniumEventType.Operation.CREATE;
        this.odataResource.postNavPropEvent(
                this.sourceEntityId.getEntitySetName(),
                object,
                info,
                this.targetNavProp.substring(1),
                op);

        return ret;
    }

    /**
     * Generate Entity data to be registered via NavigationProperty from the input stream.
     * @param reader input stream
     * @return OEntityWrapper object generated from the input stream
     */
    OEntityWrapper createEntityFromInputStream(final Reader reader) {
        EdmEntityType edmEntityType = getOdataProducer().getMetadata()
                .findEdmEntitySet(this.sourceEntityId.getEntitySetName()).getType();
        return createEntityFromInputStream(
                reader,
                edmEntityType,
                this.sourceEntityId.getEntityKey(),
                this.targetEntitySet.getName());
    }

    /**
     * Generate Entity data to be registered via NavigationProperty from the input stream.
     * @param reader input stream
     * @return OEntityWrapper object generated from the input stream
     */
    OEntityWrapper createEntityFromInputStream(
            final Reader reader,
            EdmEntityType sourceEdmEntityType,
            OEntityKey sourceEntityKey,
            String targetEntitySetName) {
        //Primary key validation
        validatePrimaryKey(sourceEntityKey, sourceEdmEntityType);

        //Create OEntity to register
        setEntitySetName(targetEntitySetName);
        OEntity newEnt = createRequestEntity(reader, null);

        //Wrapped in a trumpet. Since POST never receives ETags such as If-Match Etag is null.
        String uuid = PersoniumUUID.randomUUID();
        OEntityWrapper oew = new OEntityWrapper(uuid, newEnt, null);
        return oew;
    }

    /**
     * Register Entity via NavigationProperty.
     * @param oew OEntityWrapper object for registration
     * @return Entity response generated from registered content
     */
    EntityResponse createEntity(OEntityWrapper oew) {
        //Process of attaching meta information if necessary
        this.sourceOData.beforeCreate(oew);

        //Ask Producer to create an Entity. In addition to this, we also ask for existence confirmation.
        EntityResponse res = getOdataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * Search processing by GET method for NavProp.
     * @param uriInfo UriInfo
     * @param accept Accept header
     * @param requestKey X-Personium-RequestKey Header
     * @param callback ?? What is this? JSONP?
     * @param skipToken ?? What is this?
     * @param q full-text search parameter
     * @return JAX-RS Response
     */
    @GET
    @Produces({ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8, ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8,
            ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
    public final Response getNavProperty(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(CommonUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @QueryParam("$callback") final String callback,
            @QueryParam("$skiptoken") final String skipToken,
            @QueryParam("q") final String q) {
        //Access control
        this.checkReadAccessContext();

        //Parsing of query
        UriInfo uriInfo2 = UriUtils.createUriInfo(uriInfo, 2);
        QueryInfo queryInfo = ODataEntitiesResource.queryInfo(uriInfo);

        //Execute list acquisition via NavigationProperty
        BaseResponse response = getOdataProducer().getNavProperty(
                this.sourceEntityId.getEntitySetName(),
                this.sourceEntityId.getEntityKey(),
                this.targetNavProp,
                queryInfo);

        StringWriter sw = new StringWriter();
        //It ignores Accept with TODO restrictions and returns it with JSON, so it specifies JSON as fixed.
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
        //Since TODO restrictions ignore Query, it is fixed and null is specified.
        FormatWriter<EntitiesResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                acceptableMediaTypes, null, callback);

        fw.write(uriInfo2, sw, (EntitiesResponse) response);

        String entity = sw.toString();
        //Escape processing of control code
        entity = escapeResponsebody(entity);

        ODataVersion version = ODataVersion.V2;

        Response ret = Response.ok(entity, fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString).build();

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntityId.getEntityKey().toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(this.sourceEntityId.getEntitySetName())
                .append(srcKey)
                .append("/")
                .append(this.targetNavProp)
                .toString();
        String info = new StringBuilder(Integer.toString(ret.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        String op = PersoniumEventType.Operation.LIST;
        this.odataResource.postNavPropEvent(
                this.sourceEntityId.getEntitySetName(),
                object,
                info,
                this.targetNavProp.substring(1),
                op);

        return ret;
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryOptionsPrivilege());
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.POST
                ).build();
    }

    private void checkWriteAccessContext() {
        //Access control
        //The same process runs twice for TODO BOX level. Since it is useless, we need ingenuity such as passing Privilege as an array to checkAccessContext
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(this.sourceEntityId.getEntitySetName()));
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(targetNavProp.substring(1)));
    }

    private void checkReadAccessContext() {
        //Access control
        //The same process runs twice for TODO BOX level. Since it is useless, we need ingenuity such as passing Privilege as an array to checkAccessContext
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(this.sourceEntityId.getEntitySetName()));
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(targetNavProp.substring(1)));
    }
}

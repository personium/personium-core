/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.core4j.Enumerable;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatParserFactory;
import org.odata4j.format.FormatType;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.Settings;
import org.odata4j.format.SingleLink;
import org.odata4j.format.SingleLinks;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.impl.es.odata.EsODataProducer;
import io.personium.core.odata.PersoniumFormatWriterFactory;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS Resource handling $ links of OData.
 */
public final class ODataLinksResource {
    private final OEntityId sourceEntity;
    private final String targetNavProp;
    private final OEntityKey targetEntityKey;
    private final ODataResource odataResource;
    private final ODataProducer odataProducer;

    /**
     * log.
     */
    static Logger log = LoggerFactory.getLogger(ODataLinksResource.class);

    /**
     * constructor.
     * @param odataResource Parent ODataResource
     * @param sourceEntity source Entity
     * @param targetNavProp destination Navigation Property
     * @param targetEntityKey Link EntityKey
     */
    public ODataLinksResource(
            final ODataResource odataResource,
            final OEntityId sourceEntity,
            final String targetNavProp,
            final OEntityKey targetEntityKey) {
        this.odataResource = odataResource;
        this.odataProducer = this.odataResource.getODataProducer();
        this.sourceEntity = sourceEntity;
        this.targetNavProp = targetNavProp;
        this.targetEntityKey = targetEntityKey;
    }

    /**
     * Create a link by receiving the POST method.
     * The response at the time of success is 204. Since there is no description specifically, the Location header is not returned.
     * InsertLink Request
     * If an InsertLink Request is successful, the response MUST have a 204 status code,
     * as specified in [RFC2616], and contain an empty response body.
     * @param uriInfo UriInfo
     * @param reqBody request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public Response createLink(
            @Context UriInfo uriInfo,
            final Reader reqBody) {

        //Access control
        this.checkWriteAccessContext();

        //Link creation preprocessing
        this.odataResource.beforeLinkCreate(this.sourceEntity, this.targetNavProp);

        //Do not specify Nav Prop key in POST of $ links.
        if (this.targetEntityKey != null) {
            throw PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
        }
        log.debug("POSTING $LINK");
        OEntityId newTargetEntity =
                parseRequestUri(UriUtils.createUriInfo(uriInfo, NUM_LEVELS_FROM_SVC_ROOT), reqBody);

        //It checks whether the link destination object specified by URL is equal to the object specified for Body
        Pattern p = Pattern.compile("(.+)/([^/]+)$");
        Matcher m = p.matcher(newTargetEntity.getEntitySetName());
        String bodyNavProp = m.replaceAll("$2");
        String targetEntitySetName = null;
        //In the case of $ messages of inbound messages and accounts, set Account as the link destination
        if (ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT.equals(this.targetNavProp)) {
            targetEntitySetName = Account.EDM_TYPE_NAME;
            //In the case of $ link of account and received message, set ReceivedMessage to the link destination
        } else if (Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE.equals(this.targetNavProp)) {
            targetEntitySetName = ReceivedMessage.EDM_TYPE_NAME;
        } else {
            targetEntitySetName = this.targetNavProp.substring(1);
        }
        if (!targetEntitySetName.equals(bodyNavProp)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Common.P_FORMAT_PATTERN_URI);
        }

        this.odataProducer.createLink(sourceEntity, targetNavProp, newTargetEntity);

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(sourceEntity.getEntityKey().toKeyString());
        String targetKey = AbstractODataResource.replaceDummyKeyToNull(newTargetEntity.getEntityKey().toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(sourceEntity.getEntitySetName())
                .append(srcKey)
                .append("/$links/")
                .append(this.targetNavProp)
                .append(targetKey)
                .toString();
        String info = "204";
        String op = PersoniumEventType.Operation.CREATE;
        this.odataResource.postLinkEvent(sourceEntity.getEntitySetName(), object, info, targetEntitySetName, op);

        return noContent();
    }

    /**
     * Receive PUT method and update link.
     * @param uriInfo UriInfo
     * @param reqBody request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @PUT
    public Response updateLink(
            @Context UriInfo uriInfo,
            final Reader reqBody) {

        //Access control
        this.checkWriteAccessContext();

        if (this.targetEntityKey == null) {
            throw PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED;
        } else {
            throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
        }
    }

    /**
     * It checks whether the value specified in the request body is in the correct format, and returns the OEntityId of the $ link destination.
     * @param uriInfo request URL
     * @param reqBody request body
     * @param srcEntitySetName $ links Source EntitySet name
     * @param metadata metadata
     * @return $ links destination OEntityId
     */
    static OEntityId parseRequestUri(final UriInfo uriInfo,
            final Reader reqBody,
            String srcEntitySetName,
            EdmDataServices metadata) {
        Settings settings = new Settings(ODataVersion.V1, metadata, srcEntitySetName, null, null);
        FormatParser<SingleLink> parser = FormatParserFactory.getParser(SingleLink.class, FormatType.JSON, settings);
        SingleLink link = null;
        try {
            link = parser.parse(reqBody);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.JSON_PARSE_ERROR.reason(e);
        }
        if (link.getUri() == null) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        log.debug(uriInfo.getBaseUri().toASCIIString());

        //Complex key correspondence
        //If null is specified, parsing will fail, so if the null value is set it will be replaced with a dummy key
        String linkUrl = AbstractODataResource.replaceNullToDummyKey(link.getUri());
        log.debug(linkUrl);
        OEntityId oid = null;
        String serviceRootUri = uriInfo.getBaseUri().toASCIIString();
        try {
            oid = OEntityIds.parse(serviceRootUri, linkUrl);
        } catch (IllegalArgumentException e) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        // normalize entitykey
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(oid.getEntitySetName());
        OEntityKey entityKey = AbstractODataResource.normalizeOEntityKey(oid.getEntityKey(), edmEntitySet);
        oid = OEntityIds.create(oid.getEntitySetName(), entityKey);
        log.debug(oid.getEntityKey().toKeyString());

        //In the parse processing, we did not deal with backward-bracket checks, so we check correspondence between parentheses
        String entityId = linkUrl;
        if (entityId.toLowerCase().startsWith(serviceRootUri.toLowerCase())) {
            entityId = linkUrl.substring(serviceRootUri.length());
        }
        int indexOfParen = entityId.indexOf('(');
        String entitySetName = entityId.substring(indexOfParen);
        Pattern p = Pattern.compile("^\\(.+\\)$");
        Matcher m = p.matcher(entitySetName);
        if (!m.find()) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        return oid;
    }

    /**
     * It checks whether the value specified in the request body is in the correct format, and returns the OEntityId of the $ link destination.
     * @param uriInfo request URL
     * @param reqBody request body
     * @return $ links destination OEntityId
     */
    private OEntityId parseRequestUri(final UriInfo uriInfo, final Reader reqBody) {
        return parseRequestUri(uriInfo, reqBody, this.sourceEntity.getEntitySetName(),
                this.odataProducer.getMetadata());
    }

    private Response noContent() {
        return Response.noContent().header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
    }

    /**
     * Delete link by receiving DELETE method.
     * @return JAX-RS Response
     */
    @WriteAPI
    @DELETE
    public Response deleteLink() {

        //Access control
        this.checkWriteAccessContext();

        //Link deletion preprocessing
        this.odataResource.beforeLinkDelete(this.sourceEntity, this.targetNavProp);

        //Deletion of TODO $ links can be executed with the following two requests, but NavProp key unspecified is not implemented yet
        // 1. http://host/service.svc/Customers('ALFKI')/$links/Orders(1)
        // 2. http://host/service.svc/Orders(1)/$links/Customer.
        if (this.targetEntityKey == null) {
            throw PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED;
        }
        this.odataProducer.deleteLink(sourceEntity, targetNavProp, targetEntityKey);

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntity.getEntityKey().toKeyString());
        String targetKey = AbstractODataResource.replaceDummyKeyToNull(targetEntityKey.toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(sourceEntity.getEntitySetName())
                .append(srcKey)
                .append("/$links/")
                .append(this.targetNavProp)
                .append(targetKey)
                .toString();
        String info = "204";
        String op = PersoniumEventType.Operation.DELETE;
        this.odataResource.postLinkEvent(
                sourceEntity.getEntitySetName(),
                object,
                info,
                this.targetNavProp.substring(1),
                op);

        return noContent();
    }

    static final int NUM_LEVELS_FROM_SVC_ROOT = 3;

    /**
     * Receive the GET method and return the link list.
     * @param uriInfo UriInfo
     * @param format $format
     * @param callback ??
     * @return JAX-RS Response
     */
    @GET
    public Response getLinks(
            @Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @QueryParam("$callback") final String callback) {

        //Access control
        this.checkReadAccessContext();
        if (this.targetEntityKey != null) {
            return Response
                    .status(HttpStatus.SC_BAD_REQUEST)
                    .entity("targetId should not be specified in $links GET. your value = "
                            + this.targetEntityKey.toKeyString()).build();
        }
        log.debug("GETTING $LINK");

        //Link acquisition preprocessing
        this.odataResource.beforeLinkGet(this.sourceEntity, this.targetNavProp);

        EntityIdResponse response = getLinks(uriInfo);

        StringWriter sw = new StringWriter();
        // context.getRequest().getAcceptableMediaTypes()
        UriInfo uriInfo2 = UriUtils.createUriInfo(uriInfo, NUM_LEVELS_FROM_SVC_ROOT);
        String serviceRootUri = uriInfo2.getBaseUri().toASCIIString();
        String contentType;

        if (response.getMultiplicity() == EdmMultiplicity.MANY) {
            SingleLinks links = SingleLinks.create(serviceRootUri, response.getEntities());
            //The TODO response shall be JSON fixed.
            FormatWriter<SingleLinks> fw = PersoniumFormatWriterFactory.getFormatWriter(SingleLinks.class, null, "json",
                    callback);
            fw.write(uriInfo2, sw, links);
            contentType = fw.getContentType();
        } else {
            OEntityId entityId = Enumerable.create(response.getEntities()).firstOrNull();
            if (entityId == null) {
                throw new NotFoundException();
            }
            SingleLink link = SingleLinks.create(serviceRootUri, entityId);
            FormatWriter<SingleLink> fw = PersoniumFormatWriterFactory.getFormatWriter(SingleLink.class, null, "json",
                    callback);
            fw.write(uriInfo, sw, link);
            contentType = fw.getContentType();
        }

        String entity = sw.toString();

        Response res = Response.ok(entity, contentType)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntity.getEntityKey().toKeyString());
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(this.sourceEntity.getEntitySetName())
                .append(srcKey)
                .append("/$links/")
                .append(this.targetNavProp)
                .toString();
        String info = new StringBuilder(Integer.toString(res.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        String op = PersoniumEventType.Operation.LIST;
        this.odataResource.postLinkEvent(
                this.sourceEntity.getEntitySetName(),
                object,
                info,
                this.targetNavProp.substring(1),
                op);

        return res;
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {

        //Access control
        this.odataResource.checkAccessContext(this.odataResource.getNecessaryOptionsPrivilege());

        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.DELETE,
                HttpMethod.PUT,
                HttpMethod.POST
                ).build();
    }

    private EntityIdResponse getLinks(UriInfo uriInfo) {
        QueryInfo queryInfo = null;
        if (uriInfo != null) {
            queryInfo = queryInfo(uriInfo);
        }
        EntityIdResponse response = null;
        //We added parameters to the getLinks method, but since modifying the interface has a large influence range,
        //Cast a producer
        if (this.odataProducer instanceof EsODataProducer) {
            EsODataProducer producer = (EsODataProducer) this.odataProducer;
            response = producer.getLinks(sourceEntity, targetNavProp, queryInfo);
        }
        return response;
    }

    QueryInfo queryInfo(UriInfo uriInfo) {
        MultivaluedMap<String, String> mm = uriInfo.getQueryParameters(true);
        Integer top = QueryParser.parseTopQuery(mm.getFirst("$top"));
        Integer skip = QueryParser.parseSkipQuery(mm.getFirst("$skip"));

        return new QueryInfo(
                null,
                top,
                skip,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private void checkWriteAccessContext() {
        //Access control
        //The same process runs twice for TODO BOX level. Since it is useless, we need ingenuity such as passing Privilege as an array to checkAccessContext
        String entitySetNameFrom = sourceEntity.getEntitySetName();
        String entitySetNameTo = targetNavProp;
        if (entitySetNameFrom.equals(ReceivedMessage.EDM_TYPE_NAME)
                || entitySetNameTo.equals(Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE)) {
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryWritePrivilege(ReceivedMessage.EDM_TYPE_NAME));
        } else {
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryWritePrivilege(entitySetNameFrom));
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryWritePrivilege(entitySetNameTo.substring(1)));
        }
    }

    private void checkReadAccessContext() {
        //Access control
        //The same process runs twice for TODO BOX level. Since it is useless, we need ingenuity such as passing Privilege as an array to checkAccessContext
        String entitySetNameFrom = sourceEntity.getEntitySetName();
        String entitySetNameTo = targetNavProp;
        if (entitySetNameFrom.equals(ReceivedMessage.EDM_TYPE_NAME)
                || entitySetNameTo.equals(Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE)) {
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryReadPrivilege(ReceivedMessage.EDM_TYPE_NAME));
        } else {
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryReadPrivilege(entitySetNameFrom));
            this.odataResource.checkAccessContext(
                    this.odataResource.getNecessaryReadPrivilege(entitySetNameTo.substring(1)));
        }
    }
}

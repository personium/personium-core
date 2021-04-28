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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.odata.PersoniumFormatWriterFactory;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS resource handling OData's Entities resource (a URL with entitySet specified without id specification).
 */
public final class ODataEntitiesResource extends AbstractODataResource {

    private static final int Q_MAX_LENGTH = Common.MAX_Q_VALUE_LENGTH;
    ODataResource odataResource;

    /**
     * constructor.
     * @param odataResource parent Resource
     * @param entitySetName Entity set name
     */
    public ODataEntitiesResource(final ODataResource odataResource, final String entitySetName) {
        this.odataResource = odataResource;
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);
    }

    /**
     * @param uriInfo UriInfo
     * @param accept Accept header
     * @param format $ format parameter
     * @param callback callback
     * @param skipToken skip token
     * @param q full-text search parameter
     * @return JAX-RS Response
     */
    @GET
    public Response listEntities(
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @QueryParam("$format") String format,
            @QueryParam("$callback") final String callback,
            @QueryParam("$skiptoken") final String skipToken,
            @QueryParam("q") final String q) {

        //Access control
        this.odataResource.checkAccessContext(
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        //Ask Producer to get the request
        EntitiesResponse resp = getEntities(uriInfo, q);
        StringWriter sw = new StringWriter();

        //Determining the output format from the values ​​of $ format and Accept header
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        MediaType contentType = decideOutputFormat(accept, format);
        acceptableMediaTypes.add(contentType);

        FormatWriter<EntitiesResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                acceptableMediaTypes, null, callback);
        UriInfo uriInfo2 = UriUtils.createUriInfo(uriInfo, 1);

        fw.write(uriInfo2, sw, resp);
        String entity = null;
        entity = sw.toString();

        //Escape processing of control code
        entity = escapeResponsebody(entity);

        // TODO remove this hack, check whether we are Version 2.0 compatible anyway
        ODataVersion version = null;
        version = ODataVersion.V2;

        Response response = Response.ok(entity, fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString).build();

        // post event to EventBus
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(getEntitySetName())
                .toString();
        String info = new StringBuilder(Integer.toString(response.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.LIST);

        return response;
    }

    /**
     * Ask Producer to get the request.
     * @param queryInfo QueryInfo
     * @return response
     */
    EntitiesResponse getEntities(QueryInfo queryInfo) {
        EntitiesResponse resp = getOdataProducer().getEntities(getEntitySetName(), queryInfo);
        return resp;
    }

    /**
     * Ask Producer to get the request.
     * @param uriInfo UriInfo
     * @param fullTextSearchKeyword String Keyword to perform full text search
     * @return response
     */
    EntitiesResponse getEntities(UriInfo uriInfo, String fullTextSearchKeyword) {
        QueryInfo queryInfo = null;
        if (uriInfo != null) {
            queryInfo = queryInfo(uriInfo, fullTextSearchKeyword);
        }
        EntitiesResponse resp = getOdataProducer().getEntities(getEntitySetName(), queryInfo);
        return resp;
    }

    /**
     * @param uriInfo UriInfo
     * @param accept Accept header
     * @param format $ format parameter
     * @param reader request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public Response post(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @DefaultValue(FORMAT_JSON) @QueryParam("$format") final String format,
            final Reader reader) {

        //Method execution feasibility check
        checkNotAllowedMethod(uriInfo);

        //Access control
        this.odataResource.checkAccessContext(this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);

        //Ask Producer to create Entity
        EntityResponse res = this.createEntity(reader, this.odataResource);

        //Evaluation of creation result
        OEntity ent = res.getEntity();

        //Currently, ContentType is fixed to JSON
        MediaType outputFormat = this.decideOutputFormat(accept, format);
        //Render Entity Response
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);

        OEntityKey convertedKey = AbstractODataResource.convertToUrlEncodeKey(ent.getEntitySet(), ent.getEntityKey());
        String key = AbstractODataResource.replaceDummyKeyToNull(convertedKey.toKeyString());
        String responseStr = renderEntityResponse(resUriInfo, res, format, contentTypes);

        //Escape processing of control code
        responseStr = escapeResponsebody(responseStr);

        ResponseBuilder rb = getPostResponseBuilder(ent, outputFormat, responseStr, resUriInfo, key);
        Response response = rb.build();

        // post event to EventBus
        String object = new StringBuilder(this.odataResource.getRootUrl())
                .append(getEntitySetName())
                .append(key)
                .toString();
        String info = new StringBuilder(Integer.toString(response.getStatus()))
                .append(",")
                .append(uriInfo.getRequestUri())
                .toString();
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.CREATE);

        return response;
    }

    static QueryInfo queryInfo(UriInfo uriInfo) {
        return queryInfo(uriInfo, null);
    }

    static QueryInfo queryInfo(UriInfo uriInfo, String fullTextSearchKeyword) {
        MultivaluedMap<String, String> mm = uriInfo.getQueryParameters(true);

        Integer top = QueryParser.parseTopQuery(mm.getFirst("$top"));
        Integer skip = QueryParser.parseSkipQuery(mm.getFirst("$skip"));
        BoolCommonExpression filter = QueryParser.parseFilterQuery(mm.getFirst("$filter"));
        List<EntitySimpleProperty> select = QueryParser.parseSelectQuery(mm.getFirst("$select"));
        List<EntitySimpleProperty> expand = QueryParser.parseExpandQuery(mm.getFirst("$expand"));
        InlineCount inlineCount = QueryParser.parseInlinecountQuery(mm.getFirst("$inlinecount"));
        String skipToken = QueryParser.parseSkipTokenQuery(mm.getFirst("$skiptoken"));
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(mm.getFirst("$orderby"));

        //Validate of full-text search query q
        if (fullTextSearchKeyword != null && (fullTextSearchKeyword.getBytes().length < 1
                || fullTextSearchKeyword.getBytes().length > Q_MAX_LENGTH)) {
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("q", fullTextSearchKeyword);
        }

        //When $ expand is specified, the maximum value of $ top changes, so check it
        if (expand != null && top != null && top > PersoniumUnitConfig.getTopQueryMaxSizeWithExpand()) {
            //When returning the value as it is with Integer, a comma is attached, so return an error message with a character string
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", top.toString());
        }

        Map<String, String> options = new HashMap<String, String>();
        options.put("q", fullTextSearchKeyword);
        return new QueryInfo(
                inlineCount,
                top,
                skip,
                filter,
                orderBy,
                skipToken,
                options,
                expand,
                select);
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
        this.odataResource.checkAccessContext(this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.POST
                ).build();
    }

    /**
     * Check processing other than p: Format.
     * @param props property list
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        this.odataResource.validate(getEntitySetName(), props);
    }

    /**
     * Method execution feasibility check.
     * @param uriInfo Requested resource path
     */
    private void checkNotAllowedMethod(UriInfo uriInfo) {
        //Method permission check
        String[] uriPath = uriInfo.getPath().split("/");
        if (ReceivedMessage.EDM_TYPE_NAME.equals(uriPath[uriPath.length - 1])
                || SentMessage.EDM_TYPE_NAME.equals(uriPath[uriPath.length - 1])) {
            throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        }
    }
}

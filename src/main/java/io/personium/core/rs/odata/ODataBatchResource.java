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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.ODataBatchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumReadDeleteModeManager;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.Privilege;
import io.personium.core.exceptions.ODataErrorMessage;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumFormatWriterFactory;
import io.personium.core.rs.PersoniumCoreExceptionMapper;
import io.personium.core.utils.UriUtils;

/**
 * The ODataBatchResource class.
 */
public class ODataBatchResource extends AbstractODataResource {

    private static final String X_PERSONIUM_PRIORITY = "X-Personium-Priority";

    /**
     * Whether to sleep to give Lock to another process.
     */
    public enum BatchPriority {
        /** Do not give Lock to other processes.*/
        HIGH("high"),
        /** Transfer Lock to another process.*/
        LOW("low");

        private String priority;

        BatchPriority(String priority) {
            this.priority = priority;
        }

        /**
         * Generate an enumerated value from a character string (default value: LOW).
         * @param val String
         * @return enumeration value
         */
        public static BatchPriority fromString(String val) {
            for (BatchPriority e : BatchPriority.values()) {
                if (e.priority.equalsIgnoreCase(val)) {
                    return e;
                }
            }
            return LOW;
        }
    }

    Logger logger = LoggerFactory.getLogger(ODataBatchResource.class);

    ODataResource odataResource;
    LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();

    //Class that controls execution / skipping after occurrence of Too Many Concurrent during Batch request
    BatchRequestShutter shutter;

    Map<Privilege, BatchAccess> readAccess = new HashMap<Privilege, BatchAccess>();
    Map<Privilege, BatchAccess> writeAccess = new HashMap<Privilege, BatchAccess>();

    //Mapping data of EntityType name and EntityTypeID
    Map<String, String> entityTypeIds;

    /**
     * constructor.
     * @param odataResource ODataResource
     */
    public ODataBatchResource(final ODataResource odataResource) {
        this.odataResource = odataResource;
        this.shutter = new BatchRequestShutter();
    }

    /**
     * Process batch request.
     * @param uriInfo uriInfo
     * @param headers headers
     * @param request request
     * @param reader reader
     * @return response
     */
    @WriteAPI
    @POST
    public Response batchRequest(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context Request request,
            Reader reader) {

        long startTime = System.currentTimeMillis();
        //Timeout time (set by personium-unit-config.properties io.personium.core.odata.batch.timeoutInSec, in seconds)
        long batchTimeoutInSec = PersoniumUnitConfig.getOdataBatchRequestTimeoutInMillis();

        //In order to give Lock to another process, obtain the extension header value as to whether or not to sleep
        BatchPriority priority = BatchPriority.LOW;
        List<String> priorityHeaders = headers.getRequestHeader(X_PERSONIUM_PRIORITY);
        if (priorityHeaders != null) {
            priority = BatchPriority.fromString(priorityHeaders.get(0));
        }

        timer = new BatchElapsedTimer(startTime, batchTimeoutInSec, priority);

        checkAccessContext(this.odataResource.getAccessContext());

        //TODO Return an error if an incorrect content type is specified
        String boundary = headers.getMediaType().getParameters().get("boundary");

        //Parsing the request body
        BatchBodyParser parser = new BatchBodyParser();
        List<BatchBodyPart> bodyParts = parser.parse(boundary, reader, uriInfo.getRequestUri().toString());
        if (bodyParts == null || bodyParts.size() == 0) {
            //Parsing failed
            throw PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }
        if (bodyParts.size() > Integer.parseInt(PersoniumUnitConfig.getOdataBatchBulkRequestMaxSize())) {
            //Invalid number of requests specified by $ Batch
            throw PersoniumCoreException.OData.TOO_MANY_REQUESTS.params(bodyParts.size());
        }

        UserDataODataProducer producer = (UserDataODataProducer) this.odataResource.getODataProducer();
        entityTypeIds = producer.getEntityTypeIds();

        List<NavigationPropertyBulkContext> npBulkContexts = new ArrayList<NavigationPropertyBulkContext>();

        StringBuilder responseBody = new StringBuilder();

        //Execute request one by one
        for (BatchBodyPart bodyPart : bodyParts) {
            executePartRequest(responseBody, uriInfo, boundary, npBulkContexts, bodyPart);
        }

        //Bulk execution of POST
        checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);

        //Boundary termination string
        responseBody.append("--" + boundary + "--");

        //Response creation
        String contentType = ODataBatchProvider.MULTIPART_MIXED + "; boundary=" + boundary;
        return Response.status(HttpStatus.SC_ACCEPTED)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .entity(responseBody.toString())
                .build();
    }

    /**
     * Set timeout response in $ batch (Changeset).
     */
    private void setChangesetTimeoutResponse(StringBuilder builder, String boundary, BatchBodyPart bodyPart) {
        BatchResponse res = getTimeoutResponse();
        builder.append(getChangesetResponseBody(boundary, bodyPart, res));
    }

    /**
     * Set timeout response in $ batch.
     */
    private void setTimeoutResponse(StringBuilder builder, String boundary) {
        BatchResponse res = getTimeoutResponse();
        builder.append(getRetrieveResponseBody(boundary, res));
    }

    /**
     * Create timeout response.
     */
    private BatchResponse getTimeoutResponse() {
        BatchResponse res = new BatchResponse();
        res.setErrorResponse(PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT);
        return res;
    }

    private BatchElapsedTimer timer = null;
    private volatile boolean timedOut = false;

    /**
     * Determine whether the timeout time has elapsed <br />
     * Calling condition that timer object is instantiated at API call. <br />
     * If mode is YIELD, it sleeps to give Lock to another process before judging whether timeout time has elapsed.
     * @param mode Whether to sleep to give Lock to another process
     * @return timeout Whether time has elapsed
     */
    private boolean isTimedOut(BatchElapsedTimer.Lock mode) {
        if (!timedOut) {
            timedOut = timer.shouldBreak(mode);
            if (timedOut) {
                logger.info("Batch request timed out after " + timer.getElapseTimeToBreak() + " msec.");
            }
        }
        return timedOut;
    }

    private void executePartRequest(StringBuilder responseBody, UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> npBulkContexts,
            BatchBodyPart bodyPart) {
        //During ReadDeleteOnlyMode, except for GET and DELETE methods, we do not allow it, so set an error response
        if (!PersoniumReadDeleteModeManager.isAllowedMethod(bodyPart.getHttpMethod())) {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(PersoniumCoreException.Server.READ_DELETE_ONLY);
            responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            return;
        }
        //Set up update request response after Too Many Concurrent occurred during Batch request.
        if (!shutter.accept(bodyPart.getHttpMethod())) {
            setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
            return;
        }
        if (!isValidNavigationProperty(bodyPart)) {
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED);
            if (HttpMethod.GET.equals(bodyPart.getHttpMethod())) {
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            }
            return;
        }
        if (bodyPart.isLinksRequest()) {
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = new BatchResponse();
            if (bodyPart.getHttpMethod().equals(HttpMethod.POST)) {
                if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                    if (!shutter.isShuttered()) {
                        res = createLinks(uriInfo, bodyPart);
                        responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                    } else {
                        setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                    }
                } else {
                    setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
                }
            } else if (bodyPart.getHttpMethod().equals(HttpMethod.GET)) {
                res.setErrorResponse(PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED);
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                res.setErrorResponse(PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED);
                responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.POST)) {
            if (bodyPart.hasNavigationProperty()) {
                //User data registration via NP
                if (bulkRequests.size() != 0) {
                    if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                        execBulk(responseBody, uriInfo, boundary);
                    } else {
                        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
                            setChangesetTimeoutResponse(responseBody, boundary, request.getValue().getBodyPart());
                            BatchResponse res = new BatchResponse();
                            Exception exception = PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT;
                            res.setErrorResponse(exception);
                            responseBody.append(
                                    getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
                        }
                        bulkRequests.clear();
                    }
                }
                NavigationPropertyBulkContext navigationPropertyBulkContext =
                        new NavigationPropertyBulkContext(bodyPart);
                if (shutter.isShuttered()) {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                } else {
                    try {
                        navigationPropertyBulkContext = createNavigationPropertyBulkContext(bodyPart);
                        npBulkContexts.add(navigationPropertyBulkContext);
                    } catch (Exception e) {
                        navigationPropertyBulkContext.setException(e);
                        npBulkContexts.add(navigationPropertyBulkContext);
                    }
                }
            } else {
                //User data registration
                if (!npBulkContexts.isEmpty()) {
                    if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                        //Execute bulk execution of user data registration via NP
                        execBulkRequestForNavigationProperty(npBulkContexts);
                        createNavigationPropertyBulkResponse(
                                responseBody,
                                uriInfo,
                                boundary,
                                npBulkContexts);
                    } else {
                        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
                            npBulkContext.setException(PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                            npBulkContext.getBatchResponse().setErrorResponse(
                                    PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                            responseBody.append(getChangesetResponseBody(boundary, npBulkContext.getBodyPart(),
                                    npBulkContext.getBatchResponse()));
                        }
                    }
                    npBulkContexts.clear();
                }
                if (!shutter.isShuttered()) {
                    setBulkRequestsForEntity(bodyPart);
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.GET)) {
            //Bulk execution of POST
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = null;
            if (!isTimedOut(BatchElapsedTimer.Lock.HOLD)) {
                if (isListRequst(bodyPart)) {
                    res = list(uriInfo, bodyPart);
                } else {
                    res = retrieve(uriInfo, bodyPart);
                }
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                setTimeoutResponse(responseBody, boundary);
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.PUT)) {
            //Bulk execution of POST
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    BatchResponse res = update(bodyPart);
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            } else {
                setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.DELETE)) {
            //Bulk execution of POST
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    BatchResponse res = delete(bodyPart);
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            } else {
                setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
            }
        } else {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(PersoniumCoreException.Misc.METHOD_NOT_ALLOWED);
            responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
        }
    }

    private void setChangesetTooManyConcurrentResponse(StringBuilder responseBody,
            String boundary,
            BatchBodyPart bodyPart) {
        //Since the last POST request was TooManyConcurrent, an error response is created
        BatchResponse res = new BatchResponse();
        res.setErrorResponse(PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
        responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
    }

    /**
     * Bulk registration of user data via NP.
     * @param npBulkContexts NavigationProperty List of contexts
     */
    private void execBulkRequestForNavigationProperty(List<NavigationPropertyBulkContext> npBulkContexts) {
        //Create BulkRequest from context for bulk registration
        //Also check the existence of the EntityType on the NP side and check the ID conflict within the bulk data here
        LinkedHashMap<String, BulkRequest> npBulkRequests = new LinkedHashMap<String, BulkRequest>();
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            BatchBodyPart bodyPart = npBulkContext.getBodyPart();
            BulkRequest bulkRequest = new BulkRequest(bodyPart);
            String key = PersoniumUUID.randomUUID();

            if (npBulkContext.isError()) {
                bulkRequest.setError(npBulkContext.getException());
                npBulkRequests.put(key, bulkRequest);
                continue;
            }

            String targetEntitySetName = bodyPart.getTargetEntitySetName();
            bulkRequest = createBulkRequest(bodyPart, targetEntitySetName);
            //ID conflict check in data
            //TODO compound primary key correspondence, unique key check, NTKP compliant
            if (bulkRequest.getError() == null) {
                EntitySetDocHandler docHandler = bulkRequest.getDocHandler();
                key = docHandler.getEntityTypeId() + ":" + (String) docHandler.getStaticFields().get("__id");
                if (npBulkRequests.containsKey(key)) {
                    key = PersoniumUUID.randomUUID();
                    bulkRequest.setError(PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS);
                }
            }

            npBulkRequests.put(key, bulkRequest);
        }

        try {
            this.odataResource.getODataProducer().bulkCreateEntityViaNavigationProperty(npBulkContexts, npBulkRequests);
        } catch (PersoniumCoreException e) {
            //To keep processing after 503 occurred, set the status to shutter.
            shutter.updateStatus(e);
            if (!PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS.equals(e)) {
                throw e;
            } else {
                createTooManyConcurrentResponse(npBulkContexts);
            }
        }
        npBulkRequests.clear();
    }

    private void createNavigationPropertyBulkResponse(
            StringBuilder responseBody,
            UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> npBulkContexts) {
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            BatchBodyPart bodyPart = npBulkContext.getBodyPart();
            if (!npBulkContext.isError()) {
                try {
                    setNavigationPropertyResponse(uriInfo, bodyPart, npBulkContext);
                } catch (Exception e) {
                    npBulkContext.getBatchResponse().setErrorResponse(e);
                    npBulkContext.setException(e);
                }
            } else {
                npBulkContext.getBatchResponse().setErrorResponse(npBulkContext.getException());
            }
            responseBody.append(getChangesetResponseBody(boundary, bodyPart,
                    npBulkContext.getBatchResponse()));
        }
    }

    /**
     * Create response body of Changeset of each request.
     * @param boundaryStr Boundary string
     * @param bodyPart BatchBodyPart
     * @param res Response
     * @return request response body
     */
    private String getChangesetResponseBody(String boundaryStr,
            BatchBodyPart bodyPart,
            BatchResponse res) {
        StringBuilder resBody = new StringBuilder();

        //Response body creation
        //Boundary string
        if (bodyPart.isChangesetStart()) {
            //Add boundary string to response
            resBody.append("--" + boundaryStr + "\n");
            resBody.append(HttpHeaders.CONTENT_TYPE + ": ");
            resBody.append(ODataBatchProvider.MULTIPART_MIXED + "; boundary="
                    + bodyPart.getChangesetStr() + "\n\n");
        }

        //changeset String
        resBody.append("--" + bodyPart.getChangesetStr() + "\n");
        // ContentType
        resBody.append(HttpHeaders.CONTENT_TYPE + ": ");
        resBody.append("application/http\n");
        // Content-Transfer-Encoding
        resBody.append("Content-Transfer-Encoding: binary\n\n");

        //HTTP / 1.1 {response code} {description of response code}
        resBody.append("HTTP/1.1 ");
        resBody.append(res.getResponseCode() + " ");
        resBody.append(res.getResponseMessage() + "\n");
        //Response header
        for (String key : res.getHeaders().keySet()) {
            resBody.append(key + ": " + res.getHeaders().get(key) + "\n");
        }
        resBody.append("\n");

        //Response body
        if (res.getBody() != null) {
            resBody.append(res.getBody() + "\n\n");
        }

        //changeset Termination string
        if (bodyPart.isChangesetEnd()) {
            resBody.append("--" + bodyPart.getChangesetStr() + "--\n\n");
        }

        return resBody.toString();
    }

    /**
     * Create response body of GET, LIST of each request.
     * @param boundaryStr Boundary string
     * @param res Response
     * @return request response body
     */
    private String getRetrieveResponseBody(String boundaryStr,
            BatchResponse res) {
        StringBuilder resBody = new StringBuilder();

        //Response body creation
        //Boundary string
        //Add boundary string to response
        resBody.append("--" + boundaryStr + "\n");
        resBody.append(HttpHeaders.CONTENT_TYPE + ": application/http\n\n");

        //HTTP / 1.1 {response code} {description of response code}
        resBody.append("HTTP/1.1 ");
        resBody.append(res.getResponseCode() + " ");
        resBody.append(res.getResponseMessage() + "\n");
        //Response header
        for (String key : res.getHeaders().keySet()) {
            resBody.append(key + ": " + res.getHeaders().get(key) + "\n");
        }
        resBody.append("\n");

        //Response body
        if (res.getBody() != null) {
            resBody.append(res.getBody() + "\n\n");
        }

        return resBody.toString();
    }

    /**
     * Whether list acquisition or one acquisition is acquired is determined.
     * @param uri request URI
     * @return true: get list, false: get 1 case
     */
    private boolean isListRequst(BatchBodyPart bodyPart) {
        if (bodyPart.getEntityKeyWithParences() == null
                || bodyPart.hasNavigationProperty()) {
            return true;
        }
        return false;
    }

    /**
     * Set the registration data of the batch request to the bulk request.
     * @param bodyPart BatchBodyPart
     */
    private void setBulkRequestsForEntity(BatchBodyPart bodyPart) {
        String key = PersoniumUUID.randomUUID();

        BulkRequest bulkRequest = createBulkRequest(bodyPart, bodyPart.getEntitySetName());
        if (bulkRequest.getError() == null) {
            //ID conflict check in data
            //TODO compound primary key correspondence, unique key check, NTKP compliant
            EntitySetDocHandler docHandler = bulkRequest.getDocHandler();
            key = docHandler.getEntityTypeId() + ":" + (String) docHandler.getStaticFields().get("__id");
            if (bulkRequests.containsKey(key)) {
                key = PersoniumUUID.randomUUID();
                bulkRequest.setError(PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS);
            }
        }

        bulkRequests.put(key, bulkRequest);
    }

    private BulkRequest createBulkRequest(BatchBodyPart bodyPart, String entitySetName) {
        BulkRequest bulkRequest = new BulkRequest(bodyPart);
        try {
            //Access control
            checkWriteAccessContext(bodyPart);

            //If an entity set that does not exist is specified, a 404 exception is raised
            EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(entitySetName);
            if (eSet == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            //Generate request body
            ODataEntitiesResource resource = new ODataEntitiesResource(this.odataResource, entitySetName);
            OEntity oEntity = resource.getOEntityWrapper(new StringReader(bodyPart.getEntity()),
                    this.odataResource, this.odataResource.metadata);
            EntitySetDocHandler docHandler = resource.getOdataProducer().getEntitySetDocHandler(entitySetName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(entityTypeIds.get(entitySetName));

            this.odataResource.metadata = resource.getOdataProducer().getMetadata();

            //Pay out UUID if ID is not specified
            if (docHandler.getId() == null) {
                docHandler.setId(PersoniumUUID.randomUUID());
            }
            bulkRequest.setEntitySetName(entitySetName);
            bulkRequest.setDocHandler(docHandler);

        } catch (Exception e) {
            bulkRequest.setError(e);
        }
        return bulkRequest;
    }

    private void setNavigationPropertyResponse(UriInfo uriInfo,
            BatchBodyPart bodyPart,
            NavigationPropertyBulkContext npBulkContext) {
        OEntity ent = npBulkContext.getEntityResponse().getEntity();
        //Currently, ContentType is fixed to JSON
        String accept = bodyPart.getHttpHeaders().get(org.apache.http.HttpHeaders.ACCEPT);
        MediaType outputFormat = this.decideOutputFormat(accept, "json");
        //Render Entity Response
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);
        UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);
        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());
        String responseStr = renderEntityResponse(resUriInfo, npBulkContext.getEntityResponse(), "json",
                contentTypes);

        //Header setting
        npBulkContext.setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        String entityName = bodyPart.getTargetNavigationProperty().substring(1) + key;
        npBulkContext.setResponseHeader(HttpHeaders.LOCATION,
                resUriInfo.getBaseUri().toASCIIString() + entityName);
        npBulkContext.setResponseHeader(ODataConstants.Headers.DATA_SERVICE_VERSION,
                ODataVersion.V2.asString);
        //Give ETAG to response
        if (ent instanceof OEntityWrapper) {
            OEntityWrapper oew2 = (OEntityWrapper) ent;
            String etag = oew2.getEtag();
            if (etag != null) {
                npBulkContext.setResponseHeader(HttpHeaders.ETAG, "W/\"" + etag + "\"");
            }
        }
        npBulkContext.setResponseBody(responseStr);
        npBulkContext.setResponseCode(HttpStatus.SC_CREATED);
    }

    private NavigationPropertyBulkContext createNavigationPropertyBulkContext(BatchBodyPart bodyPart) {
        //Access control
        checkWriteAccessContext(bodyPart);

        //If an entity set that does not exist is specified, a 404 exception is raised
        EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(bodyPart.getEntitySetName());
        if (eSet == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
        }

        //Confirm existence of Navigation property on schema
        ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                bodyPart.getEntitySetName(), bodyPart.getEntityKey());
        OEntityId oEntityId = entityResource.getOEntityId();
        EdmNavigationProperty enp = eSet.getType().findNavigationProperty(bodyPart.getTargetNavigationProperty());
        if (enp == null) {
            throw PersoniumCoreException.OData.NOT_SUCH_NAVPROP;
        }

        //Request information creation
        StringReader requestReader = new StringReader(bodyPart.getEntity());
        OEntityWrapper oew = createEntityFromInputStream(
                requestReader,
                eSet.getType(),
                oEntityId.getEntityKey(),
                bodyPart.getTargetEntitySetName());

        String navigationPropertyName = bodyPart.getTargetNavigationProperty();
        return new NavigationPropertyBulkContext(bodyPart, oew, oEntityId, navigationPropertyName);
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
        EdmDataServices metadata = this.odataResource.getODataProducer().getMetadata();
        OEntity newEnt = createRequestEntity(reader, null, metadata, targetEntitySetName);

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
        this.odataResource.beforeCreate(oew);

        //Ask Producer to create an Entity. In addition to this, we also ask for existence confirmation.
        EntityResponse res = this.odataResource.getODataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * Relation type of link for registration via NP.
     */
    public enum NavigationPropertyLinkType {
        /** 1:1 / 0..1:1 / 1:0..1 / 0..1:0..1 . */
        oneToOne,
        /** n:1 / n:0..1 . */
        manyToOne,
        /** 1:n / 0..1:n . */
        oneToMany,
        /** n:n. */
        manyToMany
    }

    /**
     * Class for registration via NP.
     */
    public static class NavigationPropertyBulkContext {
        private OEntityWrapper oew;
        private EntityResponse entityResponse;
        private OEntityId srcEntityId;
        private String tgtNavProp;
        private BatchBodyPart bodyPart;

        private NavigationPropertyLinkType linkType;
        private EntitySetDocHandler sourceDocHandler;
        private EntitySetDocHandler targetDocHandler;
        private LinkDocHandler linkDocHandler;

        private BatchResponse res;
        private boolean isError;
        private Exception exception;

        /**
         * constructor.
         * @param bodyPart BatchBodyPart
         */
        public NavigationPropertyBulkContext(BatchBodyPart bodyPart) {
            this(bodyPart, null, null, null);
        }

        /**
         * constructor.
         * @param bodyPart Batch BodyPart to register
         * @param oew Register OEntity
         * @param srcEntityId Link OEntityId
         * @param tgtNavProp NavigationProperty name
         */
        public NavigationPropertyBulkContext(
                BatchBodyPart bodyPart,
                OEntityWrapper oew,
                OEntityId srcEntityId,
                String tgtNavProp) {
            this.oew = oew;
            this.entityResponse = null;
            this.srcEntityId = srcEntityId;
            this.tgtNavProp = tgtNavProp;

            this.bodyPart = bodyPart;
            this.res = new BatchResponse();
        }

        /**
         * Set the OEntity to register.
         * @param entity OEntity to set
         */
        public void setOEntityWrapper(OEntityWrapper entity) {
            this.oew = entity;
        }

        /**
         * Get the OEntity to register.
         * @return OEntity to register
         */
        public OEntityWrapper getOEntityWrapper() {
            return this.oew;
        }

        /**
         * Get the EntityResponse of the registered result.
         * @return EntityResponse
         */
        public EntityResponse getEntityResponse() {
            return this.entityResponse;
        }

        /**
         * Set EntityResponse of the registered result.
         * @param entityResponse EntityResponse of the registered result
         */
        public void setEntityResponse(EntityResponse entityResponse) {
            this.entityResponse = entityResponse;
        }

        /**
         * Get the link originator OEntityId.
         * @return Link OEntityId
         */
        public OEntityId getSrcEntityId() {
            return this.srcEntityId;
        }

        /**
         * Gets the NavigationProperty name.
         * @return NavigationProperty name
         */
        public String getTgtNavProp() {
            return this.tgtNavProp;
        }

        /**
         * Get the Batch BodyPart to register.
         * @return register BatchBodyPart
         */
        public BatchBodyPart getBodyPart() {
            return this.bodyPart;
        }

        /**
         * Acquire the response body for Batch.
         * @return Response body for Batch
         */
        public BatchResponse getBatchResponse() {
            return this.res;
        }

        /**
         * Set the response header for Batch.
         * @param key Batch Response header key
         * @param value The value of the header of BatchResponse
         */
        public void setResponseHeader(String key, String value) {
            this.res.setHeader(key, value);
        }

        /**
         * Set the response body for Batch.
         * @param body BatchResponse body
         */
        public void setResponseBody(String body) {
            this.res.setBody(body);
        }

        /**
         * Set the response code for Batch.
         * @param code Response code of BatchResponse
         */
        public void setResponseCode(int code) {
            this.res.setResponseCode(code);
        }

        /**
         * Get the link type of the link.
         * @return Link type of association
         */
        public NavigationPropertyLinkType getLinkType() {
            return linkType;
        }

        /**
         * Set association type of link.
         * @param linkType Link association type
         */
        public void setLinkType(NavigationPropertyLinkType linkType) {
            this.linkType = linkType;
        }

        /**
         * Set the context information of the link entity.
         * @return Context information of the link entity
         */
        public EntitySetDocHandler getSourceDocHandler() {
            return sourceDocHandler;
        }

        /**
         * Set the context information of the link entity.
         * @param sourceDocHandler Context information of the link entity
         */
        public void setSourceDocHandler(EntitySetDocHandler sourceDocHandler) {
            this.sourceDocHandler = sourceDocHandler;
        }

        /**
         * Acquire registration context information.
         * @return registration context information
         */
        public EntitySetDocHandler getTargetDocHandler() {
            return targetDocHandler;
        }

        /**
         * Set registration context information.
         * @param targetDocHandler Registration context information
         */
        public void setTargetDocHandler(EntitySetDocHandler targetDocHandler) {
            this.targetDocHandler = targetDocHandler;
        }

        /**
         * Get link information to register.
         * @return Link information to register
         */
        public LinkDocHandler getLinkDocHandler() {
            return linkDocHandler;
        }

        /**
         * Set link information to be registered.
         * @param linkDocHandler Link information to register
         */
        public void setLinkDocHandler(LinkDocHandler linkDocHandler) {
            this.linkDocHandler = linkDocHandler;
        }

        /**
         * Set exception information.
         * @param ex exception information
         */
        public void setException(Exception ex) {
            this.exception = ex;
            this.isError = true;
        }

        /**
         * Did an error occur during processing?
         * @return true: error occurred, false: no error
         */
        public boolean isError() {
            return this.isError;
        }

        /**
         * Acquire exception information on errors that occurred during processing.
         * @return exception information
         */
        public Exception getException() {
            return this.exception;
        }
    }

    /**
     * bulk Request checking and execution processing.
     * @param uriInfo uriInfo
     * @param boundary boundary
     * @param navigationPropertyBulkContexts List of registration request information via navigation properties
     */
    private void checkAndExecBulk(
            StringBuilder responseBody,
            UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> navigationPropertyBulkContexts) {
        if (!navigationPropertyBulkContexts.isEmpty()) {
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    execBulkRequestForNavigationProperty(navigationPropertyBulkContexts);

                    createNavigationPropertyBulkResponse(
                            responseBody,
                            uriInfo,
                            boundary,
                            navigationPropertyBulkContexts);
                } else {
                    //When a 503 error has occurred in the previous block
                    createTooManyConcurrentResponse(navigationPropertyBulkContexts);
                }
            } else {
                for (NavigationPropertyBulkContext npBulkContext : navigationPropertyBulkContexts) {
                    npBulkContext.setException(PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                    npBulkContext.getBatchResponse().setErrorResponse(
                            PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                    BatchBodyPart bodyPart = npBulkContext.getBodyPart();
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart,
                            npBulkContext.getBatchResponse()));
                }
            }
            navigationPropertyBulkContexts.clear();
        } else {
            if (bulkRequests.size() != 0) {
                if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                    if (!shutter.isShuttered()) {
                        execBulk(responseBody, uriInfo, boundary);
                    } else {
                        //When a 503 error has occurred in the previous block
                        createTooManyConcurrentResponse(responseBody, boundary);
                        bulkRequests.clear();
                    }
                } else {
                    for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
                        BatchResponse res = new BatchResponse();
                        Exception exception = PersoniumCoreException.Misc.SERVER_REQUEST_TIMEOUT;
                        res.setErrorResponse(exception);
                        //Response body creation
                        responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
                    }
                    bulkRequests.clear();
                }
            }
        }
    }

    /**
     * bulk Request execution processing.
     * @param responseBody For storing results
     * @param uriInfo uriInfo
     * @param boundary boundary
     */
    private void execBulk(StringBuilder responseBody, UriInfo uriInfo, String boundary) {
        EntityResponse entityRes = null;

        //Execute data registration
        String cellId = this.odataResource.accessContext.getCell().getId();
        List<EntityResponse> resultList = null;
        try {
            resultList = this.odataResource.getODataProducer().bulkCreateEntity(
                    this.odataResource.metadata, bulkRequests, cellId);
        } catch (PersoniumCoreException e) {
            //To keep processing after 503 occurred, set the status to shutter.
            shutter.updateStatus(e);
            if (shutter.isShuttered()) {
                createTooManyConcurrentResponse(responseBody, boundary);
                bulkRequests.clear();
                return;
            } else {
                throw e;
            }
        }

        //Generate a response
        int index = 0;
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            BatchResponse res = new BatchResponse();
            Exception exception = request.getValue().getError();
            if (exception != null) {
                res.setErrorResponse(exception);
            } else {
                //Response creation
                entityRes = resultList.get(index);
                OEntity oEntity = entityRes.getEntity();

                //Status code
                res.setResponseCode(HttpStatus.SC_CREATED);

                //Header information
                String key = oEntity.getEntityKey().toKeyString();
                res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                res.setHeader(HttpHeaders.LOCATION, request.getValue().getBodyPart().getUri() + key);
                res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
                String etag = ((OEntityWrapper) oEntity).getEtag();
                if (etag != null) {
                    res.setHeader(HttpHeaders.ETAG, "W/\"" + etag + "\"");
                }

                //Body information
                UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);
                String format = AbstractODataResource.FORMAT_JSON;
                List<MediaType> contentTypes = new ArrayList<MediaType>();
                contentTypes.add(MediaType.APPLICATION_JSON_TYPE);
                String responseStr = renderEntityResponse(resUriInfo, entityRes, format, contentTypes);
                res.setBody(responseStr);
                index++;
            }
            //Response body creation
            responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
        }
        bulkRequests.clear();
    }

    /**
     * Create a Too Many Concurrent error response for POST request.
     * @param responseBody
     * @param boundary
     */
    private void createTooManyConcurrentResponse(StringBuilder responseBody, String boundary) {
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
            //Response body creation
            responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
        }
    }

    /**
     * Create a Too Many Concurrent error response for POST request via NP.
     * @param responseBody
     * @param boundary
     */
    private void createTooManyConcurrentResponse(List<NavigationPropertyBulkContext> npBulkContexts) {
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            npBulkContext.setException(PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
        }
    }

    /**
     * Batch request list acquisition processing.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return response
     */
    private BatchResponse list(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        EntitiesResponse entitiesResp = null;
        try {
            //Access control
            checkReadAccessContext(bodyPart);
            //List acquisition via NavigationProperty is 501
            if (bodyPart.hasNavigationProperty()) {
                throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
            }
            ODataEntitiesResource entitiesResource = new ODataEntitiesResource(this.odataResource,
                    bodyPart.getEntitySetName());

            //Get Entity list
            String query = bodyPart.getRequestQuery();
            QueryInfo queryInfo = QueryParser.createQueryInfo(query);
            entitiesResp = entitiesResource.getEntities(queryInfo);

            //Response creation
            res.setResponseCode(HttpStatus.SC_OK);
            //TODO Current status, ContentType is JSON fixed
            res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            //Response body
            UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);
            StringWriter sw = new StringWriter();
            //It ignores Accept with TODO restrictions and returns it with JSON, so it specifies JSON as fixed.
            List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
            acceptableMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
            //Since TODO restrictions ignore Query, it is fixed and null is specified.
            FormatWriter<EntitiesResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                    acceptableMediaTypes, null, null);
            UriInfo uriInfo2 = UriUtils.createUriInfo(resUriInfo, 1);

            fw.write(uriInfo2, sw, entitiesResp);
            String entity = sw.toString();

            res.setBody(entity);

        } catch (Exception e) {
            res.setErrorResponse(e);
        }

        return res;
    }

    /**
     * Process of obtaining one case of batch request.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return response
     */
    private BatchResponse retrieve(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        EntityResponse entityResp = null;
        try {
            //Access control
            checkReadAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            //Entity get one case
            //TODO query supported
            entityResp = entityResource.getEntity(null, null, null);

            //Response creation
            res.setResponseCode(HttpStatus.SC_OK);
            //TODO Current status, ContentType is JSON fixed
            res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            //Response body
            UriInfo resUriInfo = UriUtils.createUriInfo(uriInfo, 1);
            //TODO Current status, ContentType is JSON fixed
            String format = AbstractODataResource.FORMAT_JSON;
            List<MediaType> contentTypes = new ArrayList<MediaType>();
            contentTypes.add(MediaType.APPLICATION_JSON_TYPE);
            String responseStr = entityResource.renderEntityResponse(resUriInfo, entityResp, format, null);
            res.setBody(responseStr);

        } catch (Exception e) {
            res.setErrorResponse(e);
        }

        return res;
    }

    /**
     * Update processing of batch request.
     * @param bodyPart BatchBodyPart
     */
    private BatchResponse update(BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            //Access control
            checkWriteAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            //Entity update
            Reader reader = new StringReader(bodyPart.getEntity());
            String ifMatch = bodyPart.getHttpHeaders().get(HttpHeaders.IF_MATCH);
            OEntityWrapper oew = entityResource.updateEntity(reader, ifMatch);

            //Response creation
            //If there are no exceptions, return a response.
            //Return ETag newly registered in oew
            String etag = oew.getEtag();
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            res.setHeader(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag));
        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * Batch request deletion processing.
     * @param bodyPart BatchBodyPart
     * @return BatchResponse
     */
    private BatchResponse delete(BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            //Access control
            checkWriteAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            //Delete Entity
            String ifMatch = bodyPart.getHttpHeaders().get(HttpHeaders.IF_MATCH);
            entityResource.deleteEntity(ifMatch);

            //Response creation
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * $ Link registration process of batch request.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return BatchResponse
     */
    private BatchResponse createLinks(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            //If an entity set that does not exist is specified, an immediate error
            EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(bodyPart.getEntitySetName());
            if (eSet == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            //Access control
            checkWriteAccessContext(bodyPart);

            //Do not specify Nav Prop key in POST of $ links.
            if (bodyPart.getTargetEntityKey().length() > 0) {
                throw PersoniumCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
            }

            OEntityKey oeKey = OEntityKey.parse(bodyPart.getEntityKeyWithParences());
            OEntityId sourceEntityId = OEntityIds.create(bodyPart.getEntitySetName(), oeKey);

            StringReader requestReader = new StringReader(bodyPart.getEntity());
            OEntityId targetEntityId = ODataLinksResource.parseRequestUri(UriUtils.createUriInfo(uriInfo, 1),
                    requestReader, bodyPart.getEntitySetName(), this.odataResource.metadata);

            this.odataResource.getODataProducer().createLink(sourceEntityId, bodyPart.getTargetEntitySetName(),
                    targetEntityId);
            //Response creation
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);

        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * $ batch Access control done on request.
     * @param ac access context
     */
    private void checkAccessContext(AccessContext ac) {
        //Schema authentication
        this.odataResource.checkSchemaAuth(this.odataResource.getAccessContext());

        //Unit user token check
        if (ac.isUnitUserToken()) {
            return;
        }

        //Check if basic authentication is possible
        this.odataResource.setBasicAuthenticateEnableInBatchRequest(ac);

        //If principal is other than ALL, authentication processing is performed
        //Note that access control is performed in each MIME part in the $ batch request
        if (!this.odataResource.hasPrivilegeForBatch(ac)) {
            //Check the validity of the token
            //Even if the token is INVALID, if the ACL setting and Privilege is set to all, it is necessary to permit access, so check at this timing
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(this.odataResource.getAcceptableAuthScheme());
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(),
                        this.odataResource.getAcceptableAuthScheme());
            }
            //If privilege not permitted as $ batch is specified, it is 403 because it reaches here
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * Access control to be performed on each MIME part in the $batch request.
     * @param ac AccessContext
     * @param privilege Required privilege
     */
    private void checkAccessContextForMimePart(Privilege privilege) {
        AccessContext ac = this.odataResource.getAccessContext();
        // Check UnitUser token.
        if (ac.isUnitUserToken(privilege)) {
            return;
        }

        if (!this.odataResource.hasPrivilege(privilege)) {
            //Authentication processing has already been executed for the $ batch request, so we only decide authorization here
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

    }

    /**
     * Write access control for $ batch.
     * @param bodyPart bodyPart
     */
    private void checkWriteAccessContext(BatchBodyPart bodyPart) {

        //Privilege management is required for every TODO EntitySet

        Privilege priv = this.odataResource.getNecessaryWritePrivilege(bodyPart.getEntitySetName());

        BatchAccess batchAccess = writeAccess.get(priv);
        if (batchAccess == null) {
            batchAccess = new BatchAccess();
            writeAccess.put(priv, batchAccess);
            try {
                this.checkAccessContextForMimePart(priv);
            } catch (PersoniumCoreException ex) {
                batchAccess.setAccessContext(ex);
            }
        }

        batchAccess.checkAccessContext();
    }

    /**
     * Read access control for $ batch.
     * @param bodyPart bodyPart
     */
    private void checkReadAccessContext(BatchBodyPart bodyPart) {

        //Privilege management is required for every TODO EntitySet

        Privilege priv = this.odataResource.getNecessaryReadPrivilege(bodyPart.getEntitySetName());

        BatchAccess batchAccess = readAccess.get(priv);
        if (batchAccess == null) {
            batchAccess = new BatchAccess();
            readAccess.put(priv, batchAccess);
            try {
                this.checkAccessContextForMimePart(priv);
            } catch (PersoniumCoreException ex) {
                batchAccess.setAccessContext(ex);
            }
        }

        batchAccess.checkAccessContext();
    }

    /**
     * Check whether the NavigationProperty specification value of the request path is in the correct format.
     * @param bodyPart bodyPart
     * Returns true if the @return specification is correct, false otherwise
     */
    private boolean isValidNavigationProperty(BatchBodyPart bodyPart) {
        if (bodyPart.hasNavigationProperty()
                && bodyPart.getTargetNavigationProperty().indexOf("(") >= 0) { //With key is NG
            return false;
        }
        return true;
    }

    /**
     * Batch Class for managing access information.
     */
    static class BatchAccess {
        private PersoniumCoreException exception = null;

        void checkAccessContext() {
            if (this.exception != null) {
                throw this.exception;
            }
        }

        void setAccessContext(PersoniumCoreException ex) {
            this.exception = ex;
        }

    }

    /**
     * Response information class of Batch request.
     */
    static class BatchResponse {

        private int responseCode;
        private Map<String, String> headers = new HashMap<String, String>();
        private String body = null;

        /**
         * Return the response code of BatchResponse.
         * @return Response code of BatchResponse
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * Set the response code of BatchResponse.
         * @param responseCode Response code of BatchResponse
         */
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        /**
         * Return the header of BatchResponse.
         * @return BatchResponse header
         */
        public Map<String, String> getHeaders() {
            return headers;
        }

        /**
         * Get description of response code.
         * @return Description of the response code (eg OK, No Content)
         */
        public String getResponseMessage() {
            String message = null;

            switch (this.responseCode) {
            case HttpStatus.SC_NO_CONTENT:
                message = "No Content";
                break;

            case HttpStatus.SC_CREATED:
                message = "Created";
                break;

            case HttpStatus.SC_OK:
                message = "OK";
                break;

            default:
                message = "";
                break;
            }

            return message;
        }

        /**
         * Set the header of BatchResponse.
         * @param key Batch Response header key
         * @param value The value of the header of BatchResponse
         */
        public void setHeader(String key, String value) {
            this.headers.put(key, value);
        }

        /**
         * Return the body of BatchResponse.
         * @return Body of BatchResponse
         */
        public String getBody() {
            return body;
        }

        /**
         * Set the body of BatchResponse.
         * @param body Body of the BatchResponse
         */
        public void setBody(String body) {
            this.body = body;
        }

        /**
         * Set error information.
         * @param res BatchResponse
         * @param e PersoniumCoreException
         */
        void setErrorResponse(Exception e) {
            //Log output
            PersoniumCoreExceptionMapper mapper = new PersoniumCoreExceptionMapper();
            mapper.toResponse(e);

            if (e instanceof PersoniumCoreException) {
                //Response creation
                setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                setResponseCode(((PersoniumCoreException) e).getStatus());
                setBody(createJsonBody((PersoniumCoreException) e));
            } else {
                //Response creation
                setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                setResponseCode(PersoniumCoreException.Server.UNKNOWN_ERROR.getStatus());
                setBody(createJsonBody(PersoniumCoreException.Server.UNKNOWN_ERROR));
            }
        }

        /**
         * Create an error message in Json format.
         * @param exception PersoniumCoreException
         * @return Json format error message for response body
         */
        private String createJsonBody(PersoniumCoreException exception) {
            String code = exception.getCode();
            String message = exception.getMessage();
            LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
            LinkedHashMap<String, Object> jsonMessage = new LinkedHashMap<String, Object>();
            json.put("code", code);
            jsonMessage.put("lang", ODataErrorMessage.DEFAULT_LANG_TAG);
            jsonMessage.put("value", message);
            json.put("message", jsonMessage);
            return JSONObject.toJSONString(json);
        }

    }
}

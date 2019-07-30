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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.PROPPATCH;
import io.personium.core.annotations.REPORT;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavMoveResource;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS resource responsible for PersoniumEngineSvcCollectionResource.
 */
public class PersoniumEngineSvcCollectionResource {
    private static Logger log = LoggerFactory.getLogger(PersoniumEngineSvcCollectionResource.class);

    DavCmp davCmp = null;
    DavCollectionResource dcr = null;
    DavRsCmp davRsCmp;
    PersoniumCoreLog relayLog = null;

    /**
     * constructor.
     * @param parent DavRsCmp
     * @param davCmp DavCmp
     */
    public PersoniumEngineSvcCollectionResource(final DavRsCmp parent, final DavCmp davCmp) {
        this.davCmp = davCmp;
        this.dcr = new DavCollectionResource(parent, davCmp);
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
        Response response = this.davRsCmp.doPropfind(requestBodyXml,
                                                     depth,
                                                     contentLength,
                                                     transferEncoding,
                                                     BoxPrivilege.READ_ACL);

        // post event to EventBus
        String type = PersoniumEventType.servicecol(PersoniumEventType.Operation.PROPFIND);
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
     * REPORT Method.
     * @return JAX-RS response object
     */
    @REPORT
    public Response report() {
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
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
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.UNBIND);

        if (!recursive && !this.davRsCmp.getDavCmp().isEmpty()) {
            throw PersoniumCoreException.Dav.HAS_CHILDREN;
        }
        Response response = this.davCmp.delete(null, recursive).build();

        // post event to EventBus
        String type = PersoniumEventType.servicecol(PersoniumEventType.Operation.DELETE);
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
     * Processing of PROPPATCH.
     * @param requestBodyXml request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_PROPERTIES);
        Response response = this.davRsCmp.doProppatch(requestBodyXml);

        // post event to EventBus
        String type = PersoniumEventType.servicecol(PersoniumEventType.Operation.PROPPATCH);
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
     * Processing of ACL method Set ACL.
     * @param reader configuration XML
     * @return JAX-RS Response
     */
    @WriteAPI
    @ACL
    public Response acl(final Reader reader) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.WRITE_ACL);
        Response response = this.davCmp.acl(reader).build();

        // post event to EventBus
        String type = PersoniumEventType.servicecol(PersoniumEventType.Operation.ACL);
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
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        //Access control
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
     * Returns the Jax-RS resource responsible for the service source.
     * @return DavFileResource
     */
    @Path("__src")
    public PersoniumEngineSourceCollection src() {
        DavCmp nextCmp = this.davCmp.getChild(DavCmp.SERVICE_SRC_COLLECTION);
        if (nextCmp.exists()) {
            return new PersoniumEngineSourceCollection(this.davRsCmp, nextCmp);
        } else {
            //Since the service source collection does not exist, it is regarded as a 404 error
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(nextCmp.getUrl());
        }
    }

    /**
     * relay_GET method.
     * @param path Path name
     * @param uriInfo URI
     * @param headers header
     * @return JAX-RS Response
     */
    @Path("{path}")
    @GET
    public Response relayget(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.GET, uriInfo, path, headers, null);
    }

    /**
     * relay_POST method.
     * @param path Path name
     * @param uriInfo URI
     * @param headers header
     * @param is Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @POST
    public Response relaypost(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers,
            final InputStream is) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.POST, uriInfo, path, headers, is);
    }

    /**
     * relay_PUT method.
     * @param path Path name
     * @param uriInfo URI
     * @param headers header
     * @param is Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @PUT
    public Response relayput(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers,
            final InputStream is) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.PUT, uriInfo, path, headers, is);
    }

    /**
     * relay_DELETE method.
     * @param path Path name
     * @param uriInfo URI
     * @param headers header
     * @return JAX-RS Response
     */
    @WriteAPI
    @Path("{path}")
    @DELETE
    public Response relaydelete(@PathParam("path") String path,
            @Context final UriInfo uriInfo,
            @Context HttpHeaders headers) {
        //Access control
        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.EXEC);
        return relaycommon(HttpMethod.DELETE, uriInfo, path, headers, null);
    }

    /**
     * Create event from request.
     * @param path service name
     * @return PersoniumEvent created event
     */
    private PersoniumEvent createEvent(String path) {
        String object = UriUtils.convertSchemeFromHttpToLocalCell(this.davRsCmp.getCell().getUrl(),
                this.davRsCmp.getUrl() + "/" + path);
        String type = PersoniumEventType.service(PersoniumEventType.Operation.EXEC);

        return new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .davRsCmp(this.davRsCmp)
                .build();
    }

    // create url to request to engine
    private URI createRequestUrl(String path) {
        String cellName = this.davRsCmp.getCell().getName();
        String boxName = this.davRsCmp.getBox().getName();
        String urlPath = new StringBuilder()
                .append(PersoniumUnitConfig.getEnginePath() + "/")
                .append(cellName + "/")
                .append(boxName + "/")
                .append("service/")
                .append(path)
                .toString();

        try {
            return new URIBuilder()
                    .setScheme("http")
                    .setHost(PersoniumUnitConfig.getEngineHost())
                    .setPort(PersoniumUnitConfig.getEnginePort())
                    .setPath(urlPath)
                    .build();
        } catch (URISyntaxException e) {
            throw PersoniumCoreException.ServiceCollection.SC_ENGINE_CONNECTION_ERROR.reason(e);
        }
    }

    // close httpclient
    private void closeHttpClient(HttpClient httpClient, HttpResponse httpResponse) {
        HttpClientUtils.closeQuietly(httpResponse);
        HttpClientUtils.closeQuietly(httpClient);
    }

    /**
     * relay Common processing method.
     * @param method method
     * @param uriInfo URI
     * @param path Path name
     * @param headers header
     * @param is Request body
     * @return JAX-RS Response
     */
    private Response relaycommon( // CHECKSTYLE IGNORE - Necessary processing
            String method,
            UriInfo uriInfo,
            String path,
            HttpHeaders headers,
            InputStream is) {

        // url to request to engine
        URI requestUrl = createRequestUrl(path);

        //Get baseUrl
        String baseUrl = davCmp.getCell().getUnitUrl();

        //Acquire request header, add content below
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_DEFAULT);
        HttpUriRequest req = null;
        if (method.equals(HttpMethod.POST)) {
            HttpPost post = new HttpPost(requestUrl);
            InputStreamEntity ise = new InputStreamEntity(is, -1);
            ise.setChunked(true);
            post.setEntity(ise);
            req = post;
        } else if (method.equals(HttpMethod.PUT)) {
            HttpPut put = new HttpPut(requestUrl);
            InputStreamEntity ise = new InputStreamEntity(is, -1);
            ise.setChunked(true);
            put.setEntity(ise);
            req = put;
        } else if (method.equals(HttpMethod.DELETE)) {
            HttpDelete delete = new HttpDelete(requestUrl);
            req = delete;
        } else {
            HttpGet get = new HttpGet(requestUrl);
            req = get;
        }

        req.addHeader("X-Baseurl", baseUrl);
        req.addHeader("X-Request-Uri", uriInfo.getRequestUri().toString());
        if (davCmp instanceof DavCmpFsImpl) {
            DavCmpFsImpl dcmp = (DavCmpFsImpl) davCmp;
            req.addHeader("X-Personium-Fs-Path", dcmp.getFsPath());
            req.addHeader("X-Personium-Fs-Routing-Id", dcmp.getCellId());
        }
        req.addHeader("X-Personium-Box-Schema", this.davRsCmp.getBox().getSchema());
        req.addHeader("X-Personium-Path-Based-Cell-Url-Enabled",
                String.valueOf(PersoniumUnitConfig.isPathBasedCellUrlEnabled()));

        //Add header to relay
        MultivaluedMap<String, String> multivalueHeaders = headers.getRequestHeaders();
        for (Iterator<Entry<String, List<String>>> it = multivalueHeaders.entrySet().iterator(); it.hasNext();) {
            Entry<String, List<String>> entry = it.next();
            String key = (String) entry.getKey();
            if (key.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                continue;
            }
            List<String> valueList = (List<String>) entry.getValue();
            for (Iterator<String> i = valueList.iterator(); i.hasNext();) {
                String value = (String) i.next();
                req.setHeader(key, value);
            }
        }

        // If RequestKey is not specified in the header, Take over the generated RequestKey.
        if (!req.containsHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY)) {
            String requestKey = this.getRequestKey(this.davRsCmp);
            if (requestKey != null) {
                req.addHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY, requestKey);
            }
        }

        // prepare event
        PersoniumEvent event = createEvent(path);
        EventBus eventBus = this.davRsCmp.getAccessContext().getCell().getEventBus();

        // write relay log
        setRelayLog(req);
        this.relayLog.writeStartLog();
        debugRelayHeader(req);

        //Throw a request to the Engine
        HttpResponse objResponse = null;
        try {
            objResponse = client.execute(req);
        } catch (ClientProtocolException e) {
            // post event to EventBus
            event.setInfo("500");
            eventBus.post(event);
            closeHttpClient(client, objResponse);
            throw PersoniumCoreException.ServiceCollection.SC_INVALID_HTTP_RESPONSE_ERROR;
        } catch (Exception ioe) {
            // post event to EventBus
            event.setInfo("500");
            eventBus.post(event);
            closeHttpClient(client, objResponse);
            throw PersoniumCoreException.ServiceCollection.SC_ENGINE_CONNECTION_ERROR.reason(ioe);
        }
        this.relayLog.writeEndLog();

        // post event to EventBus
        String info = Integer.toString(objResponse.getStatusLine().getStatusCode());
        event.setInfo(info);
        eventBus.post(event);

        //Add status code
        ResponseBuilder res = Response.status(objResponse.getStatusLine().getStatusCode());
        Header[] headersResEngine = objResponse.getAllHeaders();
        //Add response header
        for (int i = 0; i < headersResEngine.length; i++) {
            //Do not relay Transfer-Encoding returned from Engine.
            //Since Content-Length or Transfer-Encoding is appended according to the length
            //of the response in the subsequent MW.
            //In order to prevent it being doubly added, leave it out here.
            if ("Transfer-Encoding".equalsIgnoreCase(headersResEngine[i].getName())) {
                continue;
            }
            //Do not relay Date returned from Engine.
            //If MW of Web server is Jetty it will be doubly added.
            if (HttpHeaders.DATE.equalsIgnoreCase(headersResEngine[i].getName())) {
                continue;
            }
            res.header(headersResEngine[i].getName(), headersResEngine[i].getValue());
        }

        InputStream isResBody = null;

        //Add response body
        HttpEntity entity = objResponse.getEntity();
        if (entity != null) {
            try {
                isResBody = entity.getContent();
            } catch (IllegalStateException e) {
                closeHttpClient(client, objResponse);
                throw PersoniumCoreException.ServiceCollection.SC_UNKNOWN_ERROR.reason(e);
            } catch (IOException e) {
                closeHttpClient(client, objResponse);
                throw PersoniumCoreException.ServiceCollection.SC_ENGINE_CONNECTION_ERROR.reason(e);
            }
            final InputStream isInvariable = isResBody;
            final HttpClient httpClient = client;
            final HttpResponse httpResponse = objResponse;
            //Output processing result
            StreamingOutput strOutput = new StreamingOutput() {
                @Override
                public void write(final OutputStream os) throws IOException {
                    int chr;
                    try {
                        while ((chr = isInvariable.read()) != -1) {
                            os.write(chr);
                        }
                    } finally {
                        isInvariable.close();
                        HttpClientUtils.closeQuietly(httpResponse);
                        HttpClientUtils.closeQuietly(httpClient);
                    }
                }
            };
            res.entity(strOutput);
        }

        //Response return
        return res.build();
    }

    /**
     * get request key. (For when not specified in the header)
     * @param rsCmp DavRsCmp
     * @return request key
     */
    private String getRequestKey(DavRsCmp rsCmp) {
        if (rsCmp == null) {
            return null;
        }
        if (rsCmp instanceof CellRsCmp) {
            return ((CellRsCmp) rsCmp).getRequestKey();
        }
        if (rsCmp.getParent() == null) {
            return null;
        }
        return getRequestKey(rsCmp.getParent());
    }

    private void setRelayLog(HttpUriRequest req) {
        this.relayLog = PersoniumCoreLog.ServiceCollection.SC_ENGINE_RELAY.params(req.getMethod(), req.getURI());
    }

    private void debugRelayHeader(HttpUriRequest req) {
        if (log.isDebugEnabled()) {
            Header[] reqHeaders = req.getAllHeaders();
            for (int i = 0; i < reqHeaders.length; i++) {
                log.debug("RelayHeader[" + reqHeaders[i].getName() + "] : " + reqHeaders[i].getValue());
            }
        }
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
        this.davRsCmp.getParent().checkAccessContext(this.davRsCmp.getAccessContext(), BoxPrivilege.UNBIND);
        return new DavMoveResource(this.davRsCmp.getParent(), this.davRsCmp.getDavCmp(), headers).doMove();
    }
}

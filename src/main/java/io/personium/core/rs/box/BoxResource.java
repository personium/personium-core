/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.rs.box;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.ACL;
import io.personium.core.annotations.MKCOL;
import io.personium.core.annotations.MOVE;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.PROPPATCH;
import io.personium.core.annotations.REPORT;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.bar.BarFile;
import io.personium.core.bar.BarFileExporter;
import io.personium.core.bar.BarFileInstaller;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressManager;
import io.personium.core.rs.cell.CellCtlResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS Resource for Box root URL.
 */
public class BoxResource {
    static Logger log = LoggerFactory.getLogger(BoxResource.class);

    /** Media-Type:personium bar file. */
    private static final MediaType MEDIATYPE_PERSONIUM_BAR = MediaType.valueOf(BarFile.CONTENT_TYPE);

    String boxName;
    Cell cell;
    Box box;
    AccessContext accessContext;
    BoxRsCmp boxRsCmp;
    CellRsCmp cellRsCmp; // for box Install

    /**
     * Constructor.
     * @param cell CELL Object
     * @param boxName Box Name
     * @param cellRsCmp cellRsCmp
     * @param accessContext AccessContext object
     * @param jaxRsRequest HTTP request for JAX-RS
     */
    public BoxResource(final Cell cell, final String boxName, final AccessContext accessContext,
            final CellRsCmp cellRsCmp, Request jaxRsRequest) {
        //No parents. For now let's put boxName as the path name.
        this.cell = cell;
        this.boxName = boxName;
        // this.path= path;
        this.accessContext = accessContext;

        //Confirm existence of Box
        //Since this class assumes that Box exists, if there is no Box, it is an error.
        //However, since it is assumed that there is no Box in box installation, processing is continued if the following conditions are satisfied.
        //- The HTTP method is MKCOL. And,
        //- PathInfo is terminated with the installation destination Box name.
        //(MKCOL to the Collection may be the case, so confirm that it is a box installation)
        this.box = this.cell.getBoxForName(boxName);
        //In box installation it is necessary to operate at Cell level.
        this.cellRsCmp = cellRsCmp;
        if (this.box != null) {
            //BoxCmp is necessary only if this Box exists
            BoxCmp davCmp = ModelFactory.boxCmp(this.box);
            this.boxRsCmp = new BoxRsCmp(cellRsCmp, davCmp, this.accessContext, this.box);
        } else {
            //This box does not exist.
            String reqPathInfo = accessContext.getUriInfo().getPath();
            if (!reqPathInfo.endsWith("/")) {
                reqPathInfo += "/";
            }
            String pathForBox = boxName;
            if (!pathForBox.endsWith("/")) {
                pathForBox += "/";
            }
            // Unless the HTTP method is MKCOL, respond with 404.
            if (!("MKCOL".equals(jaxRsRequest.getMethod()) && reqPathInfo.endsWith(pathForBox))) {
                throw PersoniumCoreException.Dav.BOX_NOT_FOUND.params(this.cell.getUrl() + boxName);
            }
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
        return this.boxRsCmp.nextPath(nextPath, request);
    }

    /**
     * @return Box object
     */
    public Box getBox() {
        return this.box;
    }

    /**
     * @return AccessContext Object
     */
    public AccessContext getAccessContext() {
        return accessContext;
    }

    /**
     * Process GET method.
     * @param httpHeaders Headers
     * @return JAX-RS Response
     */
    @GET
    public Response get(@Context HttpHeaders httpHeaders) {
        if (httpHeaders.getAcceptableMediaTypes().contains(MEDIATYPE_PERSONIUM_BAR)) {
            return getBarFile();
        } else {
            return getMetadata();
        }
    }

    /**
     * Get bar file and response it.
     * @return JAX-RS Response
     */
    private Response getBarFile() {
        // Access control.
        boxRsCmp.checkAccessContext(BoxPrivilege.READ);
        boxRsCmp.checkAccessContext(BoxPrivilege.READ_ACL);

        BarFileExporter exporter = new BarFileExporter(boxRsCmp);
        // Execute export.
        return exporter.export();
    }

    /**
     * Get box metadata and response it.
     * @return JAX-RS Response
     */
    private Response getMetadata() {
        // Access control.
        this.boxRsCmp.checkAccessContext(BoxPrivilege.READ);

        //Get asynchronous processing status of box installation from cache.
        //In this case, if null is returned, box installation has not been executed,
        //It is assumed that the cache has expired although it was executed.
        String key = "box-" + this.box.getId();
        Progress progress = ProgressManager.getProgress(key);
        if (progress == null) {
            JSONObject response = createNotRequestedResponse();
            return Response.ok().entity(response.toJSONString()).build();
        }

        String jsonString = progress.getValue();
        JSONObject jsonObj = null;
        try {
            jsonObj = (JSONObject) (new JSONParser()).parse(jsonString);
        } catch (ParseException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        }

        // Could get cache but not box install cache.
        JSONObject barInfo = (JSONObject) jsonObj.get("barInfo");
        if (barInfo == null) {
            log.info("cache(" + key + "): process" + (String) jsonObj.get("process"));
            JSONObject response = createNotRequestedResponse();
            return Response.ok().entity(response.toJSONString()).build();
        }

        // Create response.
        JSONObject response = createResponse(barInfo);
        return Response.ok().entity(response.toJSONString()).build();
    }

    /**
     * box Creates a response if the installation is not running or if it was executed but the cache expired.
     * @return JSON object for response
     */
    @SuppressWarnings("unchecked")
    private JSONObject createNotRequestedResponse() {
        JSONObject response = new JSONObject();
        JSONObject boxMetadataJson = boxRsCmp.getBoxMetadataJson();
        JSONObject cellMetadataJson = cellRsCmp.getCellMetadataJson();
        JSONObject unitMetadataJson = accessContext.getUnitMetadataJson();
        response.putAll(boxMetadataJson);
        response.putAll(cellMetadataJson);
        response.putAll(unitMetadataJson);
        return response;
    }

    /**
     * box Creates a response if the installation is not running or if it was executed but the cache expired.
     * @return JSON object for response
     */
    @SuppressWarnings("unchecked")
    private JSONObject createResponse(JSONObject values) {
        JSONObject response = new JSONObject();
        JSONObject boxMetadataJson = boxRsCmp.getBoxMetadataJson(values);
        JSONObject cellMetadataJson = cellRsCmp.getCellMetadataJson();
        JSONObject unitMetadataJson = accessContext.getUnitMetadataJson();
        response.putAll(boxMetadataJson);
        response.putAll(cellMetadataJson);
        response.putAll(unitMetadataJson);
        return response;
    }

    /**
     * DELETE method.
     * This endpoint is dedicated for recursive deletion.
     * @param recursiveHeader recursive header
     * @return JAX-RS response
     */
    @WriteAPI
    @DELETE
    public Response recursiveDelete(
            @HeaderParam(CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE) final String recursiveHeader) {
        // If the X-Personium-Recursive header is not true, it is an error
        if (!Boolean.TRUE.toString().equalsIgnoreCase(recursiveHeader)) {
            throw PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
        }
        boolean recursive = Boolean.valueOf(recursiveHeader);

        // Check acl.
        boxRsCmp.checkAccessContext(CellPrivilege.BOX);

        Response response = boxRsCmp.getDavCmp().delete(null, recursive).build();

        // post event to EventBus
        String type = PersoniumEventType.box(PersoniumEventType.Operation.DELETE);
        String object = new StringBuilder(UriUtils.SCHEME_CELL_URI).append(this.boxName)
                                                                   .toString();
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.boxRsCmp)
                                              .build();
        EventBus eventBus = this.cell.getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * Processing of the PROPFIND method.
     * @param requestBodyXml Request Body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS Response
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Access Control
        this.boxRsCmp.checkAccessContext(BoxPrivilege.READ_PROPERTIES);
        Response response = this.boxRsCmp.doPropfind(requestBodyXml,
                                                     depth,
                                                     contentLength,
                                                     transferEncoding,
                                                     BoxPrivilege.READ_ACL);

        // post event to EventBus
        String type = PersoniumEventType.box(PersoniumEventType.Operation.PROPFIND);
        String object = new StringBuilder(UriUtils.SCHEME_CELL_URI).append(this.boxName)
                                                                   .toString();
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.boxRsCmp)
                                              .build();
        EventBus eventBus = this.cell.getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * Processing of the PROPPATCH method.
     * @param requestBodyXml Request Body
     * @return JAX-RS Response
     */
    @WriteAPI
    @PROPPATCH
    public Response proppatch(final Reader requestBodyXml) {
        //Access control
        this.boxRsCmp.checkAccessContext(BoxPrivilege.WRITE_PROPERTIES);
        Response response = this.boxRsCmp.doProppatch(requestBodyXml);

        // post event to EventBus
        String type = PersoniumEventType.box(PersoniumEventType.Operation.PROPPATCH);
        String object = new StringBuilder(UriUtils.SCHEME_CELL_URI).append(this.boxName)
                                                                   .toString();
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.boxRsCmp)
                                              .build();
        EventBus eventBus = this.cell.getEventBus();
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
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return this.boxRsCmp.options();
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
        this.boxRsCmp.checkAccessContext(BoxPrivilege.WRITE_ACL);
        Response response = this.boxRsCmp.doAcl(reader);

        // post event to EventBus
        String type = PersoniumEventType.box(PersoniumEventType.Operation.ACL);
        String object = new StringBuilder(UriUtils.SCHEME_CELL_URI).append(this.boxName)
                                                                   .toString();
        String info = Integer.toString(response.getStatus());
        PersoniumEvent ev = new PersoniumEvent.Builder()
                                              .type(type)
                                              .object(object)
                                              .info(info)
                                              .davRsCmp(this.boxRsCmp)
                                              .build();
        EventBus eventBus = this.cell.getEventBus();
        eventBus.post(ev);

        return response;
    }

    /**
     * Processing of MKCOL method.
     * @param pCredHeader dcCredHeader
     * @param contentType Value of Content-Type header
     * @param contentLength Value of the Content-Length header
     * @param inStream InputStream of Http request
     * @return JAX-RS Response
     */
    @WriteAPI
    @MKCOL
    public Response mkcol(
            @HeaderParam(CommonUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL) final String pCredHeader,
            @HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final String contentLength,
            final InputStream inStream) {

        EventBus eventBus = this.cell.getEventBus();
        String result = "";
        String object = new StringBuilder(UriUtils.SCHEME_CELL_URI)
                .append(this.boxName)
                .toString();
        String requestKey = this.cellRsCmp.getRequestKey();
        Response res = null;
        try {
            //Log file output
            //Analysis of X-Personium-RequestKey (supplementing default value when not specified)
            requestKey = ResourceUtils.validateXPersoniumRequestKey(requestKey);
            //TODO findBugs countermeasure ↓
            log.debug(requestKey);

            if (Box.MAIN_BOX_NAME.equals(this.boxName)) {
                throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
            }

            //CellCtlResource, ODataEntityResource (ODataProducer) required to create Box
            //At this point, the "X-Personium-Credential" header is unnecessary and therefore null is specified
            CellCtlResource cellctl = new CellCtlResource(this.accessContext, null, this.cellRsCmp);
            String keyName = "'" + this.boxName + "'";
            ODataEntityResource odataEntity = new ODataEntityResource(cellctl, Box.EDM_TYPE_NAME, keyName);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, contentType);
            headers.put(HttpHeaders.CONTENT_LENGTH, contentLength);

            BarFileInstaller installer =
                    new BarFileInstaller(this.cell, this.boxName, odataEntity);

            res = installer.barFileInstall(headers, inStream, requestKey);
            result = Integer.toString(res.getStatus());
        } catch (RuntimeException e) {
            //Formal response of TODO internal event is required
            if (e instanceof PersoniumCoreException) {
                result = Integer.toString(((PersoniumCoreException) e).getStatus());
            } else {
                result = Integer.toString(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
            throw e;
        } finally {
            // post event to EventBus
            String type = PersoniumEventType.boxinstall();
            PersoniumEvent event = new PersoniumEvent.Builder()
                    .type(type)
                    .object(object)
                    .info(result)
                    .davRsCmp(this.cellRsCmp)
                    .requestKey(requestKey)
                    .build();
            eventBus.post(event);
        }
        return res;
    }

    /**
     * Processing of the MOVE method.
     * @param headers header information
     * @return JAX-RS response object
     */
    @MOVE
    public Response move(
            @Context HttpHeaders headers) {

        //MOVE method for Box resource is disabled
        this.boxRsCmp.checkAccessContext(BoxPrivilege.WRITE);
        throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_BOX;
    }
}

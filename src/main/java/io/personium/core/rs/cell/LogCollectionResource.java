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
package io.personium.core.rs.cell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Creationdate;
import org.apache.wink.webdav.model.Getcontentlength;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Getlastmodified;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Propfind;
import org.apache.wink.webdav.model.Resourcetype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.eventlog.LogFile;
import io.personium.core.eventlog.EventUtils;
import io.personium.core.eventlog.LogCollection;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

public class LogCollectionResource {

    static Logger log = LoggerFactory.getLogger(LogCollectionResource.class);

    DavRsCmp davRsCmp;
    LogCollection logCollection;

    public LogCollectionResource(DavRsCmp davRsCmp, LogCollection logCollection) {
        this.davRsCmp = davRsCmp;
        this.logCollection = logCollection;
    }

    /**
     * Obtain a list of archive event log files.
     * @param requestBodyXml Request Body
     * @param uriInfo request URL information
     * @param contentLength contentlength Content of header
     * @param transferEncoding Contents of Transfer-Encoding header
     * @param depth Depth header content
     * @param collectionName Name of log collection
     * @return JAX-RS Response Object
     */
    @Path("")
    @PROPFIND
    public final Response propfindLogCollection(final Reader requestBodyXml,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @PathParam("collectionName") final String collectionName
            ) {

        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG_READ);

        //Valid values ​​of Depth header are 0, 1
        //Since it does not support when infinity, return it with 403
        if ("infinity".equals(depth)) {
            throw PersoniumCoreException.Dav.PROPFIND_FINITE_DEPTH;
        } else if (depth == null) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params("null");
        } else if (!("0".equals(depth) || "1".equals(depth))) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        //Parse the request body and create a pf object
        //If the body is empty, do the same processing as setting allprop
        Propfind propfind = null;
        if (ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(requestBodyXml);
                propfind = Propfind.unmarshal(br);
            } catch (Exception e) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e);
            }
        }
        if (null != propfind && !propfind.isAllprop()) {
            throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
        }

        if (logCollection == null) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(logCollection);
        }

        //Response generation
        final Multistatus multiStatus = createMultiStatus(depth);
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                Multistatus.marshal(multiStatus, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .entity(str).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    }

    /**
     * Propfind log file.
     * @param requestBodyXml Request Body
     * @param uriInfo request URL information
     * @param contentLength contentlength Content of header
     * @param transferEncoding Contents of Transfer-Encoding header
     * @param depth Depth header content
     * @param fileName Log file name
     * @return JAX-RS Response Object
     */
    @Path("{filename}")
    @PROPFIND
    public final Response propfindLogFile(final Reader requestBodyXml,
            @Context UriInfo uriInfo,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            @PathParam("filename") final String fileName) {

        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG_READ);

        //Parse the request body and create a pf object
        //If the body is empty, do the same processing as setting allprop
        Propfind propfind = null;
        if (ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(requestBodyXml);
                propfind = Propfind.unmarshal(br);
            } catch (Exception e) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e);
            }
        }
        if (null != propfind && !propfind.isAllprop()) {
            throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
        }

        //Valid values ​​of Depth header are 0, 1
        //Since it does not support when infinity, return it with 403
        if ("infinity".equals(depth)) {
            throw PersoniumCoreException.Dav.PROPFIND_FINITE_DEPTH;
        } else if (depth == null) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params("null");
        } else if (!("0".equals(depth) || "1".equals(depth))) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        //If the file name is other than default.log, return 404
        if (!this.logCollection.isValidLogFile(fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(fileName);
        }

        LogFile logFile = this.logCollection.getLogFile(fileName);

        if (logFile == null) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(uriInfo.getAbsolutePath());
        }

        //Response generation
        final Multistatus multiStatus = createMultiStatus(logFile);
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                Multistatus.marshal(multiStatus, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .entity(str).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    }

    /**
     * Get event log file.
     * @param ifNoneMatch If-None-Match header
     * @param fileName fileName
     * @return JAXRS Response
     */
    @Path("{filename}")
    @GET
    public final Response getLogFile(@HeaderParam(HttpHeaders.IF_NONE_MATCH) final String ifNoneMatch,
            @PathParam("filename") final String fileName) {

        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG_READ);

        //If the file name is other than valid name, return 404
        if (!this.logCollection.isValidLogFile(fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(fileName);
        }

        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                logCollection.writeLogData(fileName, output);
            }
        };

        return Response.status(HttpStatus.SC_OK).header(HttpHeaders.CONTENT_TYPE, EventUtils.TEXT_CSV)
                .entity(streamingOutput).build();
    }

    /**
     * Delete log file.
     * @param fileName fileName
     * @return response
     */
    @Path("{filename}")
    @WriteAPI
    @DELETE
    public final Response deleteLogFile(@PathParam("filename") final String fileName) {
        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG);

        //If the file name is other than default.log, return 404
        if (!logCollection.isValidLogFile(fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(fileName);
        }

        logCollection.deleteLogData(fileName);

        // respond 204
        return Response.noContent().build();
    }

    private Multistatus createMultiStatus(final LogFile logFile) {
        ObjectFactory of = new ObjectFactory();
        final Multistatus multiStatus = of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> responseList = multiStatus.getResponse();

        org.apache.wink.webdav.model.Response fileResponse =
                this.createPropfindResponse(
                        logFile.getCreated(),
                        logFile.getUpdated(),
                        logFile.getUrl(),
                        logFile.getSize());
        responseList.add(fileResponse);

        return multiStatus;
    }

    private Multistatus createMultiStatus(final String depth) {
        ObjectFactory of = new ObjectFactory();
        final Multistatus multiStatus = of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> responseList = multiStatus.getResponse();

        //Add information on the Archive collection to the response
        org.apache.wink.webdav.model.Response collectionResponse =
                this.createPropfindResponse(
                        logCollection.getCreated(),
                        logCollection.getUpdated(),
                        logCollection.getUrl(),
                        null);
        responseList.add(collectionResponse);

        //When Depth is 1, information of the log file is added to the response
        if ("1".equals(depth)) {
            for (LogFile logFile : logCollection.getFileList()) {
                org.apache.wink.webdav.model.Response fileResponse =
                        this.createPropfindResponse(
                                logFile.getCreated(),
                                logFile.getUpdated(),
                                logFile.getUrl(),
                                logFile.getSize());
                responseList.add(fileResponse);
            }
        }

        return multiStatus;
    }

    /**
     * Create a response of PROPFIND.
     */
    org.apache.wink.webdav.model.Response createPropfindResponse(long created, long updated, String href, Long size) {
        //Add href
        ObjectFactory of = new ObjectFactory();
        org.apache.wink.webdav.model.Response ret = of.createResponse();
        ret.getHref().add(href);

        //Add creationdate
        Creationdate cd = of.createCreationdate();
        cd.setValue(new Date(created));
        ret.setPropertyOk(cd);

        //Added getlastmodified
        Getlastmodified lm = of.createGetlastmodified();
        lm.setValue(new Date(updated));
        ret.setPropertyOk(lm);

        if (size != null) {
            //For log files
            //Add getcontentlength
            Getcontentlength contentLength = of.createGetcontentlength();
            contentLength.setValue(String.valueOf(size));
            ret.setPropertyOk(contentLength);

            //getcontenttype added with "text / csv" fixed
            Getcontenttype contentType = of.createGetcontenttype();
            contentType.setValue(EventUtils.TEXT_CSV);
            ret.setPropertyOk(contentType);

            //Add empty resourcetype
            Resourcetype colRt = of.createResourcetype();
            ret.setPropertyOk(colRt);
        } else {
            //For log collection
            //resourcetype added with fixed WebDav collection
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            ret.setPropertyOk(colRt);
        }

        return ret;
    }
}

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
package io.personium.core.rs.cell;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
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

import io.personium.common.file.FileDataAccessException;
import io.personium.common.file.FileDataAccessor;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.eventlog.ArchiveLogCollection;
import io.personium.core.eventlog.ArchiveLogFile;
import io.personium.core.eventlog.EventUtils;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource for event log.
 */
public class LogResource {

    /** archive Collection name.*/
    public static final String ARCHIVE_COLLECTION = "archive";
    /** current Collection name.*/
    public static final String CURRENT_COLLECTION = "current";

    private static final String DEFAULT_LOG = "default.log";

    Cell cell;
    AccessContext accessContext;
    DavRsCmp davRsCmp;

    static Logger log = LoggerFactory.getLogger(LogResource.class);

    /**
     * constructor.
     * @param cell Cell
     * @param accessContext AccessContext
     * @param davRsCmp DavRsCmp
     */
    public LogResource(final Cell cell, final AccessContext accessContext, final DavRsCmp davRsCmp) {
        this.accessContext = accessContext;
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * Get the list of the current event log files.
     * @return JAX-RS Response Object
     */
    @Path(CURRENT_COLLECTION)
    @PROPFIND
    public final Response currentPropfind() {
        //Since current list acquisition of current log is not implemented yet, return 501
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * Obtain a list of archive event log files.
     * @param requestBodyXml Request Body
     * @param uriInfo request URL information
     * @param contentLength contentlength Content of header
     * @param transferEncoding Contents of Transfer-Encoding header
     * @param depth Depth header content
     * @return JAX-RS Response Object
     */
    @Path(ARCHIVE_COLLECTION)
    @PROPFIND
    public final Response archivePropfind(final Reader requestBodyXml,
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth
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

        //Collect information on the archive collection and files under it
        ArchiveLogCollection archiveLogCollection = new ArchiveLogCollection(this.cell, uriInfo);
        archiveLogCollection.createFileInformation();

        //Response generation
        final Multistatus multiStatus = createMultiStatus(depth, archiveLogCollection);
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException, WebApplicationException {
                Multistatus.marshal(multiStatus, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .entity(str).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML).build();
    }

    private Multistatus createMultiStatus(final String depth, ArchiveLogCollection archiveLogCollection) {
        ObjectFactory of = new ObjectFactory();
        final Multistatus multiStatus = of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> responseList = multiStatus.getResponse();

        //Add information on the Archive collection to the response
        org.apache.wink.webdav.model.Response collectionResponse =
                this.createPropfindResponse(
                        archiveLogCollection.getCreated(),
                        archiveLogCollection.getUpdated(),
                        archiveLogCollection.getUrl(),
                        null);
        responseList.add(collectionResponse);

        //When Depth is 1, information of the log file is added to the response
        if ("1".equals(depth)) {
            for (ArchiveLogFile archiveFile : archiveLogCollection.getArchivefileList()) {
                org.apache.wink.webdav.model.Response fileResponse =
                        this.createPropfindResponse(
                                archiveFile.getCreated(),
                                archiveFile.getUpdated(),
                                archiveFile.getUrl(),
                                archiveFile.getSize());
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

    /**
     * Get event log file.
     * @param ifNoneMatch If-None-Match header
     * @param logCollection Collection name
     * @param fileName fileName
     * @return JAXRS Response
     */
    @Path("{logCollection}/{filename}")
    @GET
    public final Response getLogFile(@HeaderParam(HttpHeaders.IF_NONE_MATCH) final String ifNoneMatch,
            @PathParam("logCollection") final String logCollection,
            @PathParam("filename") final String fileName) {

        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG_READ);

        //Check the collection name of the event log
        if (!isValidLogCollection(logCollection)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(logCollection);
        }

        //If the file name is other than default.log, return 404
        if (!isValidLogFile(logCollection, fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(fileName);
        }

        String cellId = davRsCmp.getCell().getId();
        String owner = davRsCmp.getCell().getOwnerNormalized();

        //Get the path of the log file
        StringBuilder logFileName = EventUtils.getEventLogDir(cellId, owner);
        logFileName.append(logCollection);
        logFileName.append(File.separator);
        logFileName.append(fileName);
        return getLog(logCollection, logFileName.toString());
    }

    private Response getLog(final String logCollection, String logFileName) {
        if (CURRENT_COLLECTION.equals(logCollection)) {
            File logFile = new File(logFileName);
            if (!logFile.isFile() || !logFile.canRead()) {
                //Even if the log can not be read for some reason, the response body is empty and SC_OK is returned.
                return getEmptyResponse();
            }
            try {
                final InputStream isInvariable = new FileInputStream(logFile);
                return createResponse(isInvariable);
            } catch (FileNotFoundException e) {
                throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(logFile.getName());
            }
        } else {
            ZipArchiveInputStream zipArchiveInputStream = null;
            BufferedInputStream bis = null;
            String archiveLogFileName = logFileName + ".zip";

            try {
                log.info("EventLog file path : " + archiveLogFileName);
                zipArchiveInputStream = new ZipArchiveInputStream(
                        new FileInputStream(archiveLogFileName));
                bis = new BufferedInputStream(zipArchiveInputStream);

                //Retrieve the entry in the file
                //It is assumed that only one file is stored in the compression log file
                ZipArchiveEntry nextZipEntry = zipArchiveInputStream.getNextZipEntry();
                if (nextZipEntry == null) {
                    IOUtils.closeQuietly(bis);
                    throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
                }
                return createResponse(bis);
            } catch (FileNotFoundException e1) {
                //If compressed file does not exist, return 404 error
                String[] split = archiveLogFileName.split(File.separator);
                throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(split[split.length - 1]);
            } catch (IOException e) {
                log.info("Failed to read archive entry : " + e.getMessage());
                IOUtils.closeQuietly(bis);
                throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
            }
        }
    }

    private Response createResponse(final InputStream isInvariable) {
        //Add status code
        ResponseBuilder res = Response.status(HttpStatus.SC_OK);
        res.header(HttpHeaders.CONTENT_TYPE, EventUtils.TEXT_CSV);
        res.entity(isInvariable);
        return res.build();
    }

    /**
     * Acquire an empty response to be returned when there is no event log.
     * @return empty response
     */
    private Response getEmptyResponse() {
        //Returning the response
        ResponseBuilder res = Response.status(HttpStatus.SC_OK);
        res.header(HttpHeaders.CONTENT_TYPE, EventUtils.TEXT_CSV);

        res.entity("");
        log.debug("main thread end.");
        return res.build();
    }

    /**
     * Delete log file.
     * @param logCollection Collection name
     * @param fileName fileName
     * @return response
     */
    @Path("{logCollection}/{filename}")
    @WriteAPI
    @DELETE
    public final Response deleteLogFile(@PathParam("logCollection") final String logCollection,
            @PathParam("filename") final String fileName) {

        //Access control
        this.davRsCmp.checkAccessContext(CellPrivilege.LOG);

        //Check the collection name of the event log
        if (CURRENT_COLLECTION.equals(logCollection)) {
            throw PersoniumCoreException.Event.CURRENT_FILE_CANNOT_DELETE;
        } else if (!isValidLogCollection(logCollection)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(logCollection);
        }

        //If the file name is other than default.log, return 404
        if (!isValidLogFile(logCollection, fileName)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(fileName);
        }

        String cellId = davRsCmp.getCell().getId();
        String owner = davRsCmp.getCell().getOwnerNormalized();

        //Delete event log file
        StringBuilder logFilePath = EventUtils.getEventLogDir(cellId, owner);
        logFilePath.append(logCollection);
        logFilePath.append(File.separator);
        logFilePath.append(fileName);
        deleteLogArchive(logFilePath.toString());

        // respond 204
        return Response.noContent().build();
    }

    /**
     * Delete log archive.
     * @param logFilePath log file path
     */
    private void deleteLogArchive(String logFilePath) {
        String archiveLogFileName = logFilePath + ".zip";

        // File existence check.
        File logFile = new File(archiveLogFileName);
        if (!logFile.isFile()) {
            String[] split = archiveLogFileName.split(File.separator);
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(split[split.length - 1]);
        }

        // File delete.
        try {
            FileDataAccessor accessor = new FileDataAccessor("", null,
                    PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());
            accessor.deleteWithFullPath(archiveLogFileName);
        } catch (FileDataAccessException e) {
            log.info("Failed delete eventLog : " + e.getMessage());
            throw PersoniumCoreException.Event.FILE_DELETE_FAILED;
        }
    }

    /**
     * Collection name check of event log.
     * @param collectionName Collection name ("current" or "archive")
     * @return true: correct, false: error
     */
    protected boolean isValidLogCollection(String collectionName) {
        return CURRENT_COLLECTION.equals(collectionName)
                || ARCHIVE_COLLECTION.equals(collectionName);
    }

    /**
     * File name check of event log.
     * <ul>
     * <li> current: "default.log" fixed
     * <li> archive: File name starting with "default.log." (404 if there is no actual file, but here only the file name check)
     * </ul>
     * @param collectionName Collection name ("current" or "archive")
     * @param fileName File name ("default.log" or "default.log. *")
     * @return true: correct, false: error
     */
    protected boolean isValidLogFile(String collectionName, String fileName) {
        if (CURRENT_COLLECTION.equals(collectionName)) {
            return DEFAULT_LOG.equals(fileName);
        } else { //Collection name exceptions excluded
            return fileName != null && fileName.startsWith(DEFAULT_LOG + ".");
        }
    }
}

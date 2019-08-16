/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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

import java.io.InputStream;
import java.io.Reader;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.annotations.REPORT;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.CellSnapshotCellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling Cell Snapshot Api.
 *  logics for the url path /{cell name}/__snapshot/{file name}.
 */
public class CellSnapshotDavFileResource {

    /** Class that performs actual processing. */
    DavRsCmp davRsCmp;

    /**
     * Constructor.
     * @param parent Parent resource
     * @param davCmp DavCmp
     */
    public CellSnapshotDavFileResource(CellSnapshotCellRsCmp parent, DavCmp davCmp) {
        davRsCmp = new DavRsCmp(parent, davCmp);
    }

    /**
     * process GET Method and retrieve the file content.
     * @param ifNoneMatch If-None-Match Header
     * @return JAX-RS response object
     */
    @GET
    public Response get(
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) final String ifNoneMatch
            ) {
        // Check exist
        checkFileExists();
        // Access Control
        davRsCmp.getParent().checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.ROOT);
        ResponseBuilder rb = davRsCmp.get(ifNoneMatch, null);
        return rb.build();
    }

    /**
     * process PUT Method and update the file.
     * <p>
     * TODO Security considerations.
     * Implementing PUT makes it possible to perform Import with the snapshot file modified by the user.
     * Does malicious tampering with snapshot file cause problems?
     * It is necessary to carefully verify.
     *
     * @param contentType Content-Type Header
     * @param ifMatch If-Match Header
     * @param inputStream Request Body
     * @return JAX-RS response object
     */
    @WriteAPI
    @PUT
    public Response put(@HeaderParam(HttpHeaders.CONTENT_TYPE) final String contentType,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch,
            final InputStream inputStream) {
        // Access Control
        davRsCmp.getParent().checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.ROOT);

        ResponseBuilder rb = davRsCmp.getDavCmp().putForUpdate(contentType, inputStream, ifMatch);
        return rb.build();
    }

    /**
     * process DELETE Method and delete this resource.
     * @param ifMatch If-Match header
     * @return JAX-RS response object
     */
    @WriteAPI
    @DELETE
    public Response delete(@HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        // Check exist
        checkFileExists();
        // Access Control
        davRsCmp.getParent().checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.ROOT);
        ResponseBuilder rb = davRsCmp.getDavCmp().delete(ifMatch, false);
        return rb.build();
    }

    /**
     * process PROPFIND Method.
     * @param requestBodyXml request body
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @return JAX-RS response object
     */
    @PROPFIND
    public Response propfind(final Reader requestBodyXml,
            @HeaderParam(CommonUtils.HttpHeaders.DEPTH) final String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) final Long contentLength,
            @HeaderParam("Transfer-Encoding") final String transferEncoding) {
        // Check exist
        checkFileExists();
        // Access Control
        davRsCmp.getParent().checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.ROOT);
        return davRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding, CellPrivilege.ROOT);
    }

    /**
     * process REPORT Method.
     * @return JAX-RS response object
     */
    @REPORT
    public Response report() {
        throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
    }

    /**
     * process OPTIONS Method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        // Check exist
        checkFileExists();
        // Access Control
        davRsCmp.getParent().checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.ROOT);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.CommonUtils.HttpMethod.PROPFIND
                ).build();
    }

    /**
     * File exist check.
     */
    private void checkFileExists() {
        // TYPE_NULL (file does not exist) is checked.
        DavCmp davCmp = davRsCmp.getDavCmp();
        if (DavCmp.TYPE_NULL.equals(davCmp.getType())) {
            throw davCmp.getNotFoundException().params(davRsCmp.getParent().getUrl() + "/" + davCmp.getName());
        }
    }
}

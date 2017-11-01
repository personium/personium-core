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

import java.io.Reader;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressManager;
import io.personium.core.snapshot.SnapshotFileExportProgressInfo;
import io.personium.core.snapshot.SnapshotFileManager;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling Cell Exportl Api.
 *  logics for the url path /{cell name}/__export.
 */
public class CellExportResource {

//    /** Logger. */
//    private static Logger log = LoggerFactory.getLogger(CellExportResource.class);

    /** Key of JSON specified by Body : Name. */
    private static final String BODY_JSON_KEY_NAME = "Name";

    /** Export target cell information. */
    private CellRsCmp cellRsCmp;

    /**
     * Constructor.
     * @param cellRsCmp CellRsCmp
     */
    public CellExportResource(CellRsCmp cellRsCmp) {
        this.cellRsCmp = cellRsCmp;
    }

    /**
     * GET method.
     * @return JAX-RS Response
     */
    @GET
    public Response get() {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(cellRsCmp.getAccessContext(), CellPrivilege.ROOT);

        // Get processing status from cache.
        // If it returns null, it is regarded as ready state.
        String key = SnapshotFileExportProgressInfo.getKey(cellRsCmp.getCell().getId());
        Progress progress = ProgressManager.getProgress(key);
        if (progress == null) {
            JSONObject response = SnapshotFileExportProgressInfo.getReadyJson();
            return Response.ok().entity(response.toJSONString()).build();
        }

        String jsonString = progress.getValue();
        return Response.ok().entity(jsonString).build();
    }

    /**
     * POST method.
     * @param reader Request body
     * @return JAX-RS Response
     */
    @POST
    public Response post(final Reader reader) {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(cellRsCmp.getAccessContext(), CellPrivilege.ROOT);

        // Reading body.
        JSONObject body;
        body = ResourceUtils.parseBodyAsJSON(reader);
        String name = (String) body.get(BODY_JSON_KEY_NAME);
        // Validate body.
        if (name == null) {
            throw PersoniumCoreException.Common.REQUEST_BODY_REQUIRED_KEY_MISSING.params(BODY_JSON_KEY_NAME);
        }
        if (!ODataUtils.validateRegEx(name, Common.PATTERN_SNAPSHOT_NAME)) {
            throw PersoniumCoreException.Common.REQUEST_BODY_FIELD_FORMAT_ERROR.params(
                    BODY_JSON_KEY_NAME, Common.PATTERN_SNAPSHOT_NAME);
        }

        SnapshotFileManager snapshotFileManager = new SnapshotFileManager(cellRsCmp.getCell(), name);
        snapshotFileManager.exportSnapshot();

        ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
        res.header(HttpHeaders.LOCATION, cellRsCmp.getCell().getUrl() + "__export");
        return res.build();
    }

    /**
     * process OPTIONS Method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(cellRsCmp.getAccessContext(), CellPrivilege.ROOT);
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.POST
                ).build();
    }

}

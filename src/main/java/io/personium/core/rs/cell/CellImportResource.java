/**
 * Personium
 * Copyright 2017-2021 Personium Project Authors
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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.Charsets;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressManager;
import io.personium.core.snapshot.SnapshotFileImportProgressInfo;
import io.personium.core.snapshot.SnapshotFileManager;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling Cell Exportl Api.
 *  logics for the url path /{cell name}/__import.
 */
public class CellImportResource {

    /** Key of JSON specified by Body : Name. */
    private static final String BODY_JSON_KEY_NAME = "Name";

    /** Import target cell information. */
    private CellRsCmp cellRsCmp;

    /**
     * Constructor.
     * @param cellRsCmp CellRsCmp
     */
    public CellImportResource(CellRsCmp cellRsCmp) {
        this.cellRsCmp = cellRsCmp;
    }

    /**
     * GET method.
     * @return JAX-RS Response
     */
    @GET
    public Response get() {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(CellPrivilege.ROOT);

        String jsonString = "";
        if (Cell.STATUS_NORMAL.equals(cellRsCmp.getDavCmp().getCellStatus())) {
            // Get processing status from cache.
            // If it returns null, it is regarded as ready state.
            String key = SnapshotFileImportProgressInfo.getKey(cellRsCmp.getCell().getId());
            Progress progress = ProgressManager.getProgress(key);
            if (progress == null) {
                JSONObject response = SnapshotFileImportProgressInfo.getReadyJson();
                jsonString = response.toJSONString();
            } else {
                jsonString = progress.getValue();
            }
        } else {
            // Get status from error file.
            Path errorFilePath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                    cellRsCmp.getCell().getDataBundleName(), cellRsCmp.getCell().getId(), Cell.IMPORT_ERROR_FILE_NAME);
            try {
                jsonString = new String(Files.readAllBytes(errorFilePath), Charsets.UTF_8);
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read error json file").reason(e);
            }
        }
        return Response.ok().entity(jsonString).build();
    }

    /**
     * POST method.
     * @param reader Request body
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public Response post(final Reader reader) {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(CellPrivilege.ROOT);

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
        snapshotFileManager.importSnapshot();

        ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
        res.header(HttpHeaders.LOCATION, cellRsCmp.getCell().getUrl() + "__import");
        return res.build();
    }

    /**
     * process OPTIONS Method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        // Check the authority required for execution.
        cellRsCmp.checkAccessContext(CellPrivilege.ROOT);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.POST
                ).build();
    }

}

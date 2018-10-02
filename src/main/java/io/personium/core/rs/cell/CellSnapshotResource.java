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

import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.annotations.PROPFIND;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.CellSnapshotCellCmp;
import io.personium.core.model.CellSnapshotCellRsCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.utils.ResourceUtils;

/**
 * JAX-RS Resource handling Cell Snapshot Api.
 *  logics for the url path /{cell name}/__snapshot.
 */
public class CellSnapshotResource {

    /** Class that performs actual processing. */
    private CellSnapshotCellRsCmp cellSnapshotCellRsCmp;

    /**
     * Constructor.
     * @param cellRsCmp CellRsCmp
     */
    public CellSnapshotResource(CellRsCmp cellRsCmp) {
        CellSnapshotCellCmp cellSnapshotCellCmp = ModelFactory.cellSnapshotCellCmp(cellRsCmp.getCell());
        cellSnapshotCellRsCmp = new CellSnapshotCellRsCmp(cellSnapshotCellCmp, cellRsCmp.getCell(),
                cellRsCmp.getAccessContext());
    }

    /**
     * Returns the Jax-RS resource responsible for the lower path of the current resource.
     * @param nextPath One lower path name
     * @return A Jax-RS resource object responsible for the lower-level path
     */
    @Path("{nextPath}")
    public Object nextPath(@PathParam("nextPath") final String nextPath) {
        DavCmp nextCmp = cellSnapshotCellRsCmp.getDavCmp().getChild(nextPath);
        return new CellSnapshotDavFileResource(cellSnapshotCellRsCmp, nextCmp);
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
    public Response propfind(Reader requestBodyXml,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.DEPTH) String depth,
            @HeaderParam(HttpHeaders.CONTENT_LENGTH) Long contentLength,
            @HeaderParam("Transfer-Encoding") String transferEncoding) {
        // Access Control
        cellSnapshotCellRsCmp.checkAccessContext(cellSnapshotCellRsCmp.getAccessContext(), CellPrivilege.ROOT);
        return cellSnapshotCellRsCmp.doPropfind(requestBodyXml, depth, contentLength, transferEncoding,
                CellPrivilege.ROOT);
    }

    /**
     * process OPTIONS Method.
     * @return JAX-RS response object
     */
    @OPTIONS
    public Response options() {
        // Access Control
        cellSnapshotCellRsCmp.checkAccessContext(cellSnapshotCellRsCmp.getAccessContext(), CellPrivilege.ROOT);
        return ResourceUtils.responseBuilderForOptions(
                io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND
                ).build();
    }

}

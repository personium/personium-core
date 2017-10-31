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
package io.personium.core.model;

import io.personium.core.auth.AccessContext;

/**
 * Class called from JaxRS Resource object of Cell Snapshot.<br>
 * Perform processing excluding persistence of Dav file.
 */
public class CellSnapshotCellRsCmp extends CellRsCmp {

    /** snapshot api endpoint. */
    private static final String SNAPSHOT_ENDPOINT = "__snapshot";

    /**
     * Constructor.
     * @param davCmp DavCmp
     * @param cell Cell
     * @param accessContext AccessContext
     */
    public CellSnapshotCellRsCmp(final DavCmp davCmp, final Cell cell, final AccessContext accessContext) {
        super(davCmp, cell, accessContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl() {
        String cellUrl = super.getUrl();
        // Remove "/" when last is "/"
        if (cellUrl.endsWith("/")) {
            cellUrl = cellUrl.substring(0, cellUrl.length() - 1);
        }
        return cellUrl + "/" + SNAPSHOT_ENDPOINT;
    }
}

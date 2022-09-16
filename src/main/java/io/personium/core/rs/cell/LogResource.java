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

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.eventlog.ArchiveLogCollection;
import io.personium.core.eventlog.CurrentLogCollection;
import io.personium.core.eventlog.LogCollection;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;

/**
 * JAX-RS Resource for event log.
 */
public class LogResource {

    /** archive Collection name. */
    public static final String ARCHIVE_COLLECTION = "archive";
    /** current Collection name. */
    public static final String CURRENT_COLLECTION = "current";

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
     * Handler for current log.
     * @param uriInfo Requested URL information.
     * @return Resource object.
     */
    @Path(CURRENT_COLLECTION)
    public LogCollectionResource current(@Context UriInfo uriInfo) {
        // Generate URL of current collection
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.scheme(PersoniumUnitConfig.getUnitScheme());
        StringBuilder urlSb = new StringBuilder().append(uriBuilder.build().toASCIIString())
                .append(uriInfo.getMatchedURIs().get(0));

        LogCollection logCollection = new CurrentLogCollection(cell, urlSb.toString());
        return new LogCollectionResource(davRsCmp, logCollection);
    }

    /**
     * Handler for archive log.
     * @param uriInfo Requested URL information.
     * @return Resource object.
     */
    @Path(ARCHIVE_COLLECTION)
    public LogCollectionResource archive(@Context UriInfo uriInfo) {
        // Generate URL of current collection
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.scheme(PersoniumUnitConfig.getUnitScheme());
        StringBuilder urlSb = new StringBuilder().append(uriBuilder.build().toASCIIString())
                .append(uriInfo.getMatchedURIs().get(0));

        LogCollection logCollection = new ArchiveLogCollection(cell, urlSb.toString());
        return new LogCollectionResource(davRsCmp, logCollection);
    }
}

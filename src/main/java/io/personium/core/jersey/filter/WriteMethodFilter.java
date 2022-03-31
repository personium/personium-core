/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.core.jersey.filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.CellLockManager;

/**
 * Filter for requests with @WriteAPI annotation set.
 */
@Provider
@WriteAPI
public class WriteMethodFilter implements ContainerRequestFilter  {

    /**
     * {@inheritDoc}
     * <p>
     * When the cell is locking write, make the corresponding method inoperable.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get cell name from URI.
        String path = requestContext.getUriInfo().getPathSegments().get(0).getPath();
        if ("__ctl".equals(path)) {
            // For __ctl it is UnitLevelAPI.
            // Get {name} from [Cell('{name}') or Cell(Name='{name}')].
            path = requestContext.getUriInfo().getPathSegments().get(1).getPath();
            Pattern formatPattern = Pattern.compile("'(.+)'");
            Matcher formatMatcher = formatPattern.matcher(path);
            if (formatMatcher.find()) {
                path = formatMatcher.group(1);
            } else {
                return;
            }
        }
        String cellName = path;
        Cell cell = ModelFactory.cellFromName(cellName);
        if (cell != null) {
            CellLockManager.STATUS lockStatus = CellLockManager.getCellStatus(cell.getId());
            // If the lock status of Cell is "export", "import", do not allow access.
            if (CellLockManager.STATUS.EXPORT.equals(lockStatus) || CellLockManager.STATUS.IMPORT.equals(lockStatus)) {
                throw PersoniumCoreException.Common.LOCK_WRITING_TO_CELL.params(lockStatus.getMessage());
            }
        }
    }
}

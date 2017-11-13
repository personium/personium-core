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
package io.personium.core.jersey.filter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.CellLockManager;

/**
 * Filter for requests with @WriteAPI annotation set.
 */
public class WriteMethodFilter implements ResourceFilter, ContainerRequestFilter  {

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerResponseFilter getResponseFilter() {
        // do nothing.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // Get cell name from URI.
        String cellName = request.getPathSegments().get(0).getPath();
        Cell cell = ModelFactory.cell(cellName);
        if (cell != null) {
            CellLockManager.STATUS lockStatus = CellLockManager.getCellStatus(cell.getId());
            // If the lock status of Cell is "export", "import", do not allow access.
            if (CellLockManager.STATUS.EXPORT.equals(lockStatus) || CellLockManager.STATUS.IMPORT.equals(lockStatus)) {
                throw PersoniumCoreException.Common.LOCK_WRITING_TO_CELL.params(lockStatus.getMessage());
            }
        }
        return request;
    }
}

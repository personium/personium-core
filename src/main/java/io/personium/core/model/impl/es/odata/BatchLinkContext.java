/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package io.personium.core.model.impl.es.odata;

import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * Class for holding link information in $ batch of user OData.
 */
public class BatchLinkContext {

    private EntitySetDocHandler sourceDocHandler;
    private String targetEntityTypeName;
    private String targetEntityTypeId;
    private long existsCount;
    private long requestCount;

    BatchLinkContext(EntitySetDocHandler sourceDocHandler, String targetEntityType, String targetEntityTypeId) {
        this.sourceDocHandler = sourceDocHandler;
        this.targetEntityTypeName = targetEntityType;
        this.targetEntityTypeId = targetEntityTypeId;
    }

    /**
     * Get the source DocHandler.
     * @return Source side DocHandler
     */
    EntitySetDocHandler getSourceDocHandler() {
        return sourceDocHandler;
    }

    /**
     * Get the EntityType name of the target side.
     * @return EntityType name on target side
     */
    String getTargetEntityTypeName() {
        return targetEntityTypeName;
    }

    /**
     * Get the EntityTypeID of the target side.
     * @return EntityTypeID of target side
     */
    String getTargetEntityTypeId() {
        return targetEntityTypeId;
    }

    /**
     * The number of items to be registered (number of registered items in DB + number of analyzed items in request).
     * @return Number of items to register
     */
    long getRegistCount() {
        return this.existsCount + this.requestCount;
    }

    /**
     * Set the number of items registered in the DB.
     * @ param existsCount Number registered in DB
     */
    void setExistsCount(long existsCount) {
        this.existsCount = existsCount;
    }

    /**
     * Increment the number of analyzed items in the request.
     */
    void incrementRegistCount() {
        this.requestCount++;
    }

}

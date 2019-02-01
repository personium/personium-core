/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * --------------------------------------------------
 * This code is based on ODataProducer.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import java.util.LinkedHashMap;
import java.util.List;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;

import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;

/**
 * ETag · ODataProducer corresponding to change of primary key.
 */
public interface PersoniumODataProducer extends ODataProducer {
    /**
     * ETag · Entity update corresponding to primary key change.
     * @param entitySetName entitySetName
     * @param originalKey Key to be updated
     * @param oEntityWrapper data (including updated key)
     */
    void updateEntity(String entitySetName, OEntityKey originalKey, OEntityWrapper oEntityWrapper);

    /**
     * Account password change.
     * @param entitySetName entitySetName
     * @param originalKey Key to be updated
     * @param pCredHeader dcCredHeader
     */
    void updatePassword(EdmEntitySet entitySetName, OEntityKey originalKey, String pCredHeader);

    /**
     * ETag · Entity MERGE for primary key change.
     * @param entitySetName entitySetName
     * @param originalKey Key to be updated
     * @param oEntityWrapper data (including updated key)
     */
    void mergeEntity(String entitySetName, OEntityKey originalKey, OEntityWrapper oEntityWrapper);

    /**
     * Entity deletion corresponding to ETag.
     * @param entitySetName entitySetName
     * @param entityKey entityKey
     * @param etag etag
     */
    void deleteEntity(String entitySetName, OEntityKey entityKey, String etag);

    /**
     * EntitySetDocHandler is generated from EntitySet name and OEntity and acquired.
     * @param entitySetName EntitySet name
     * @param entity OEntity
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler getEntitySetDocHandler(String entitySetName, OEntity entity);

    /**
     * Update processing handler.
     * @param entitySetName Entity set name
     */
    void onChange(String entitySetName);

    /**
     * Perform bulk registration.
     * @param metadata schema information
     * @param bulkRequests List of EntitySetDocHandler to register
     * @param cellId Cell ID
     * @return EntitiesResponse
     */
    List<EntityResponse> bulkCreateEntity(EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests, String cellId);

    /**
     * Register the link after registering the entity via NP.
     * @param sourceEntity sourceEntity
     * @param targetNavProp targetNavProp
     * @param oew oew
     * @param entity targetEntity
     * @return etag
     */
    EntityResponse createNp(OEntityId sourceEntity, String targetNavProp,
            OEntity oew, String entity);

    /**
     * Register entities collectively via NavigationProperty.
     * @param npBulkContexts Context of bulk registration
     * @param npBulkRequests Request information for entity batch registration (for bulkCreateEntity)
     */
    void bulkCreateEntityViaNavigationProperty(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests);

    /**
     * Check the upper limit of the number of links when registering entities collectively via NavigationProperty.
     * @param npBulkContexts Context of bulk registration
     * @param npBulkRequests Request information for entity batch registration (for bulkCreateEntity)
     */
    void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests);



}

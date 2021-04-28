/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.model.impl.es.accessor;

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * Abstract class of access processing of ODataEntity.
 */
public abstract class AbstractEntitySetAccessor extends DataSourceAccessor implements EntitySetAccessor {

    /**
     * The SQLState of the SQLException that occurs when registering data.
     */
    protected static final String MYSQL_BAD_TABLE_ERROR = "42S02";

    /**
     * constructor.
     * @param index index
     * @param name Type name
     * @param routingId routingId
     */
    protected AbstractEntitySetAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * Register data of ODataEntity with UUID.
     * @param docHandler registration data
     * @return registration result
     */
    @Override
    public PersoniumIndexResponse create(final EntitySetDocHandler docHandler) {
        String id = PersoniumUUID.randomUUID();
        return create(id, docHandler);
    }

    /**
     * Perform data registration of ODataEntity.
     * @param id ID of registration data
     * @param docHandler registration data
     * @return registration result
     */
    public PersoniumIndexResponse create(String id, EntitySetDocHandler docHandler) {
        docHandler.setId(id);
        PersoniumIndexResponse response = create(id, docHandler.getSource());
        return response;
    }

    /**
     * Data of Cell is updated.
     * @param id ID of update data
     * @param docHandler registration data
     * @return Update result
     */
    @Override
    public PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler) {
        return this.update(id, docHandler, -1);
    }

    /**
     * Perform data update of ODataEntity with version specification.
     * @param id ID of update data
     * @param docHandler registration data
     * @param version version information
     * @return Update result
     */
    public PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler, long version) {
        PersoniumIndexResponse response = update(id, docHandler.getSource(), version);
        return response;
    }

    /**
     * Delete data of ODataEntity.
     * @param docHandler delete data
     * @return Deletion result
     */
    @Override
    public PersoniumDeleteResponse delete(final EntitySetDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * Delete data of ODataEntity.
     * @param docHandler delete data
     * @param version version information
     * @return Deletion result
     */
    @Override
    public PersoniumDeleteResponse delete(EntitySetDocHandler docHandler, long version) {
        String id = docHandler.getId();

        PersoniumDeleteResponse response = super.delete(id, version);
        return response;
    }
}

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

import java.util.Map;

import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * The interface class of the accessor for ODataEntitySet.
 */
public interface EntitySetAccessor {

    /**
     * Get a document.
     * @param id Document ID
     * @return response
     */
    PersoniumGetResponse get(String id);

    /**
     * Perform data registration with UUID.
     * @param docHandler registration data
     * @return registration result
     */
    PersoniumIndexResponse create(EntitySetDocHandler docHandler);

    /**
     * Register data with ID specified.
     * @param id Registration ID
     * @param docHandler registration data
     * @return registration result
     */
    PersoniumIndexResponse create(String id, EntitySetDocHandler docHandler);

    /**
     * Perform data update with version specification.
     * @param id ID of update data
     * @param docHandler registration data
     * @param version version information
     * @return Update result
     */
    PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler, long version);

    /**
     * Perform data update.
     * @param id ID of update data
     * @param docHandler registration data
     * @return Update result
     */
    PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler);

    /**
     * Perform data deletion.
     * @param docHandler delete data
     * @return Deletion result
     */
    PersoniumDeleteResponse delete(EntitySetDocHandler docHandler);

    /**
     * Get the number of documents.
     * @param query Query information
     * @return ES response
     */
    long count(Map<String, Object> query);

    /**
     * Search documents.
     * @param query Query information
     * @return ES response
     */
    PersoniumSearchResponse search(Map<String, Object> query);

    /**
     * Perform data deletion.
     * @param docHandler delete data
     * @param version version
     * @return Deletion result
     */
    PersoniumDeleteResponse delete(EntitySetDocHandler docHandler, long version);

    /**
     * Get Type.
     * @return response
     */
    String getType();

}

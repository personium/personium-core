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
import io.personium.core.model.impl.es.doc.LinkDocHandler;

/**
 * Class that implements ODataLink information access processing.
 */
public class ODataLinkAccessor extends DataSourceAccessor {

    /**
     * constructor.
     * @param index index
     * @param name Type name
     * @param routingId routingId
     */
    public ODataLinkAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * Register ODataLink data.
     * @param id ID of registration data
     * @param docHandler registration data
     * @return registration result
     */
    public PersoniumIndexResponse create(String id, LinkDocHandler docHandler) {
        docHandler.setId(id);
        PersoniumIndexResponse response = super.create(id, docHandler.createLinkDoc());
        return response;
    }

    /**
     * Delete a document.
     * @param docHandler delete data
     * @return response
     */
    public PersoniumDeleteResponse delete(final LinkDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * Delete ODataLink data.
     * @param docHandler delete data
     * @param version version information
     * @return Deletion result
     */
    public PersoniumDeleteResponse delete(final LinkDocHandler docHandler, long version) {
        String id = docHandler.getId();

        PersoniumDeleteResponse response = super.delete(id, version);
        return response;
    }

}

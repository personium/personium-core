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
package io.personium.core.model.impl.es.accessor;

import io.personium.common.es.EsIndex;

/**
 * A class that implements access processing of ODataEntity.
 */
public class ODataEntityAccessor extends AbstractEntitySetAccessor {

    /**
     * constructor.
     * @param index index
     * @param name Type name
     * @param routingId routingId
     */
    public ODataEntityAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

}

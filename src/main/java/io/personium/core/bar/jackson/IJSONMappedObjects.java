/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
package io.personium.core.bar.jackson;

import java.util.List;

import org.odata4j.core.OEntity;

/**
 * Interface that handles JSON of CellCtl objects in bar installation.
 */
public interface IJSONMappedObjects {

    /**
     * Get CellCtlObjects list size.
     * @return list size
     */
    int getObjectsSize();

//    /**
//     * Get CellCtlObject of the index specified by argument.
//     * @param index index
//     * @return CellCtlObject
//     */
//    JSONMappedObject getObject(int index);

    /**
     * Add CellCtlObject to the list.
     * @param entities OEntity response.
     */
    void addObjects(List<OEntity> entities);
}

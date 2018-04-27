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

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.OEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson correspondence class for handling extroles.json.
 */
public class JSONExtRoles implements IJSONMappedObjects {

    /** JsonProperty:ExtRoles. */
    @JsonProperty("ExtRoles")
    private List<JSONExtRole> extRoles;

    /**
     * Constructor.
     */
    public JSONExtRoles() {
        extRoles = new ArrayList<JSONExtRole>();
    }

    /**
     * Get value of ExtRoles.
     * @return value of ExtRoles
     */
    public List<JSONExtRole> getExtRoles() {
        return extRoles;
    }

    /**
     * Set value of ExtRoles.
     * @param extRoles ExtRoles
     */
    public void setExtRoles(List<JSONExtRole> extRoles) {
        this.extRoles = extRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getObjectsSize() {
        return extRoles.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObjects(List<OEntity> entities) {
        for (OEntity entity : entities) {
            JSONExtRole object = JSONExtRole.newInstance(entity);
            extRoles.add(object);
        }
    }
}

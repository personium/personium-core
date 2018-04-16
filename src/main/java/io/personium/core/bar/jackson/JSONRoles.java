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
 * Jackson correspondence class for handling roles.json.
 */
public class JSONRoles implements IJSONMappedObjects {

    /** JsonProperty:Roles. */
    @JsonProperty("Roles")
    private List<JSONRole> roles;

    /**
     * Constructor.
     */
    public JSONRoles() {
        roles = new ArrayList<JSONRole>();
    }

    /**
     * Get value of Roles.
     * @return value of Roles
     */
    public List<JSONRole> getRoles() {
        return roles;
    }

    /**
     * Set value of Roles.
     * @param roles roles
     */
    public void setRoles(List<JSONRole> roles) {
        this.roles = roles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObjects(List<OEntity> entities) {
        for (OEntity entity : entities) {
            JSONRole object = JSONRole.newInstance(entity);
            roles.add(object);
        }
    }
}

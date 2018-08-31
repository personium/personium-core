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
package io.personium.core.bar.jackson;

import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *Mapping definition class for reading JSON file for Role definition in bar file.
 */
public class JSONRole implements IJSONMappedObject {

    /** Name. */
    @JsonProperty("Name")
    private String name;

    /**
     * Create new instance.
     * @param entity Source entity
     * @return JSONRole instance.
     */
    public static JSONRole newInstance(OEntity entity) {
        JSONRole instance = new JSONRole();
        instance.setName(entity.getProperty("Name", String.class).getValue());
        return instance;
    }

    /**
     *Get Name property.
     * @return Name
     */
    public String getName() {
        return this.name;
    }

    /**
     *Set Name property.
     * @param name Name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("Name", this.name);
        return json;
    }
}

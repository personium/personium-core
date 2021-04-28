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
package io.personium.core.bar.jackson;

import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON file for $ links definition (70 _ $ links_ json) in bar file Mapping definition class for reading.
 */
public class JSONUserDataLink implements IJSONMappedObject {

    /**
     * FromType.
     */
    @JsonProperty("FromType")
    private String fromType;

    /**
     * FromId.
     */
    @JsonProperty("FromId")
    private Map<String, String> fromId;

    /**
     * ToType.
     */
    @JsonProperty("ToType")
    private String toType;

    /**
     * ToId.
     */
    @JsonProperty("ToId")
    private Map<String, String> toId;

    /**
     * Get FromType property.
     * @return FromType name
     */
    public String getFromType() {
        return this.fromType;
    }

    /**
     * Set the FromType property.
     * @param fromType FromType.
     */
    public void setFromType(String fromType) {
        this.fromType = fromType;
    }

    /**
     * Get FromId name property.
     * @return FromId
     */
    public Map<String, String> getFromId() {
        return this.fromId;
    }

    /**
     * Set the FromId name property.
     * @param fromIdValue FromId
     */
    public void setFromId(Map<String, String> fromIdValue) {
        this.fromId = fromIdValue;
    }

    /**
     * Get ToType property.
     * @return ToType name
     */
    public String getToType() {
        return this.toType;
    }

    /**
     * Set ToType property.
     * @param toType ToType.
     */
    public void setToType(String toType) {
        this.toType = toType;
    }

    /**
     * Get ToId name property.
     * @return ToId
     */
    public Map<String, String> getToId() {
        return this.toId;
    }

    /**
     * Set ToId name property.
     * @param toIdValue ToId
     */
    public void setToId(Map<String, String> toIdValue) {
        this.toId = toIdValue;
    }

    /**
     * Get ToType property in NavigatioinProperty format.
     * @return Names of ToType in NavigatioinProperty format
     */
    @JsonIgnore
    public String getNavPropToType() {
        return "_" + this.toType;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("FromType", this.fromType);

        JSONObject fromIdJson = new JSONObject();
        for (String name : this.fromId.keySet()) {
            fromIdJson.put(name, this.fromId.get(name));
        }
        json.put("FromId", fromIdJson);

        json.put("ToType", this.toType);

        JSONObject toIdJson = new JSONObject();
        for (String name : this.toId.keySet()) {
            toIdJson.put(name, this.toId.get(name));
        }
        json.put("ToId", toIdJson);

        return json;
    }
}

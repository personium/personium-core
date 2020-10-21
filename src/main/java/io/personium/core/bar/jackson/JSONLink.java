/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
public class JSONLink implements IJSONMappedObject {

    /** FromType. */
    @JsonProperty("FromType")
    private String fromType;
    /** FromName. */
    @JsonProperty("FromName")
    private Map<String, String> fromName;
    /** ToType. */
    @JsonProperty("ToType")
    private String toType;
    /** ToName. */
    @JsonProperty("ToName")
    private Map<String, String> toName;

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
     * Get FromName name property.
     * @return FromName
     */
    public Map<String, String> getFromName() {
        return this.fromName;
    }

    /**
     * Set FromName name property.
     * @param fromName FromName
     */
    public void setFromName(Map<String, String> fromName) {
        this.fromName = fromName;
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
     * Get ToName name property.
     * @return ToName
     */
    public Map<String, String> getToName() {
        return this.toName;
    }

    /**
     * Set ToName name property.
     * @param toName ToName
     */
    public void setToName(Map<String, String> toName) {
        this.toName = toName;
    }

    /**
     * Get ToType property in NavigatioinProperty format.
     * @return Names of ToType in NavigatioinProperty format
     */
    public String getNavPropToType() {
        return "_" + this.toType;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();

        json.put("FromType", this.fromType);

        JSONObject fromNameJson = new JSONObject();
        for (String name : this.fromName.keySet()) {
            fromNameJson.put(name, this.fromName.get(name));
        }
        json.put("FromName", fromNameJson);

        json.put("ToType", this.toType);

        JSONObject toNameJson = new JSONObject();
        for (String name : this.toName.keySet()) {
            toNameJson.put(name, this.toName.get(name));
        }
        json.put("ToName", toNameJson);

        return json;
    }
}

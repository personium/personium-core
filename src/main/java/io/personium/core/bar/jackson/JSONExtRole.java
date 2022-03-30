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
package io.personium.core.bar.jackson;

import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapping definition class for reading JSON file for ExtRole definition in bar file.
 */
public class JSONExtRole implements IJSONMappedObject {

    /** ExtRole. */
    @JsonProperty("ExtRole")
    private String extRole;
    /** _Relation.Name. */
    @JsonProperty("_Relation.Name")
    private String relation;

    /**
     * Create new instance.
     * @param entity Source entity
     * @return JSONExtRole instance.
     */
    public static JSONExtRole newInstance(OEntity entity) {
        JSONExtRole instance = new JSONExtRole();
        instance.setExtRole(entity.getProperty("ExtRole", String.class).getValue());
        instance.setRelationName(entity.getProperty("_Relation.Name", String.class).getValue());
        return instance;
    }

    /**
     * Get ExtRole property.
     * @return ExtRole name
     */
    public String getExtRole() {
        return this.extRole;
    }

    /**
     * Set ExtRole property.
     * @param url extRoleURL.
     */
    public void setExtRole(String url) {
        this.extRole = url;
    }

    /**
     * Get Relation name property.
     * @return Relation_Name
     */
    public String getRelationName() {
        return this.relation;
    }

    /**
     * Relation name property setting.
     * @param relationName RelationName
     */
    public void setRelationName(String relationName) {
        this.relation = relationName;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("ExtRole", this.extRole);
        json.put("_Relation.Name", this.relation);
        return json;
    }
}

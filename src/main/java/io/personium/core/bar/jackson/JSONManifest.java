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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.personium.core.utils.ODataUtils;

/**
 * Mapping class for reading 00_manifest.json.
 */
public class JSONManifest implements IJSONMappedObject {

    /** bar_version. */
    @JsonProperty("bar_version")
    private String barVersion;
    /** box_version. */
    @JsonProperty("box_version")
    private String boxVersion;
    /** DefaultPath. bar_version 1. */
    @JsonProperty("DefaultPath")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String oldDefaultPath;
    /** default_path. bar_version 2. */
    @JsonProperty("default_path")
    private String defaultPath;
    /** schema. */
    @JsonProperty("schema")
    private String schema;

    /**
     * Default constructor.
     * If this method does not exist, it will fail with deserialization of Json.
     */
    public JSONManifest() {
    }

    /**
     * Constructor.
     * @param barVersion bar_version
     * @param boxVersion box_version
     * @param defaultPath DefaultPath
     * @param schema schema
     */
    public JSONManifest(String barVersion, String boxVersion, String defaultPath, String schema) {
        this.barVersion = barVersion;
        this.boxVersion = boxVersion;
        this.defaultPath = defaultPath;
        this.schema = schema;
    }

    /**
     * Get bar_version property.
     * @return barVersion
     */
    public String getBarVersion() {
        return barVersion;
    }

    /**
     * Setting the bar_version property.
     * @param barVersion barVersion.
     */
    public void setBarVersion(String barVersion) {
        this.barVersion = barVersion;
    }

    /**
     * Get box_version property.
     * @return boxVersion
     */
    public String getBoxVersion() {
        return boxVersion;
    }

    /**
     * Set the box_version property.
     * @param boxVersion boxVersion.
     */
    public void setBoxVersion(String boxVersion) {
        this.boxVersion = boxVersion;
    }

    /**
     * Get DefaultPath property.
     * @return defaultPath
     */
    public String getOldDefaultPath() {
        return oldDefaultPath;
    }

    /**
     * Setting of the DefaultPath property.
     * @param oldDefaultPath oldDefaultPath.
     */
    public void setOldDefaultPath(String oldDefaultPath) {
        this.oldDefaultPath = oldDefaultPath;
    }

    /**
     * Get default_path property.
     * @return defaultPath
     */
    public String getDefaultPath() {
        return defaultPath;
    }

    /**
     * Setting the default_path property.
     * @param defaultPath defaultPath.
     */
    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    /**
     * Get the schema property.
     * @return schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Setting the schema property.
     * @param schema schema.
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Check the value of schema.
     * @return true: Validate OK, false: Validate NG
     */
    public boolean checkSchema() {
        //If the value of schema is null or not in URL format, return error.
        //Do not use isValidSchemaUri in anticipation of easing the requirement at the end of the box schema
        if (this.getSchema() == null
                || !(ODataUtils.isValidUri(this.getSchema()))) {
            return false;
        }
        return true;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("Name", this.oldDefaultPath);
        json.put("Schema", this.schema);
        return json;
    }
}

/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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

import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONObject;

/**
 * Mapping class for reading 50_rules.json.
 */
public class JSONRules implements JSONMappedObject {

    /**
     * External.
     */
    @JsonProperty("External")
    private boolean external;

    /**
     * Subject.
     */
    @JsonProperty("Subject")
    private String subject;

    /**
     * Type.
     */
    @JsonProperty("Type")
    private String type;

    /**
     * Object.
     */
    @JsonProperty("Object")
    private String object;

    /**
     * Info.
     */
    @JsonProperty("Info")
    private String info;

    /**
     * Action.
     */
    @JsonProperty("Action")
    private String action;

    /**
     * Service.
     */
    @JsonProperty("Service")
    private String service;

    /**
     * Get value of External.
     * @return external flag
     */
    public boolean getExternal() { return external; }

    /**
     * Get value of Subject.
     * @return value of Subject
     */
    public String getSubject() { return subject; }

    /**
     * Get value of Type.
     * @return value of Type
     */
    public String getType() { return type; }

    /**
     * Get value of Object.
     * @return value of Object
     */
    public String getObject() { return object; }

    /**
     * Get value of Info.
     * @return value of Info
     */
    public String getInfo() { return info; }

    /**
     * Get value of Action.
     * @return value of Action
     */
    public String getAction() { return action; }

    /**
     * Get value of Service.
     * @return value of Service
     */
    public String getService() { return service; }

    /**
     * Set value of External.
     * @param external external flag
     */
    public void setExternal(boolean external) { this.external = external; }

    /**
     * Set value of Subject.
     * @param subject subject string
     */
    public void setSubject(String subject) { this.subject = subject; }

    /**
     * Set value of Type.
     * @param type type string
     */
    public void setType(String type) { this.type = type; }

    /**
     * Set value of Object.
     * @param object object string
     */
    public void setObject(String object) { this.object = object; }

    /**
     * Set value of Info.
     * @param info info string
     */
    public void setInfo(String info) { this.info = info; }

    /**
     * Set value of Action.
     * @param action action string
     */
    public void setAction(String action) { this.action = action; }

    /**
     * Set value of Service.
     * @param service service url
     */
    public void setService(String service) { this.service = service; }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("External", this.external);
        json.put("Subject", this.subject);
        json.put("Type", this.type);
        json.put("Object", this.object);
        json.put("Info", this.info);
        json.put("Action", this.action);
        json.put("Service", this.service);
        return json;
    }
}

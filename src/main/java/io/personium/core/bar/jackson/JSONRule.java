/**
 * Personium
 * Copyright 2017-2021 Personium Project Authors
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mapping class for reading 50_rules.json.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // When the value is null, it is not output to Json.
public class JSONRule implements IJSONMappedObject {

    /** External. */
    @JsonProperty("EventExternal")
    private boolean external;
    /** Subject. */
    @JsonProperty("EventSubject")
    private String subject;
    /** Type. */
    @JsonProperty("EventType")
    private String type;
    /** Object. */
    @JsonProperty("EventObject")
    private String object;
    /** Info. */
    @JsonProperty("EventInfo")
    private String info;
    /** Action. */
    @JsonProperty("Action")
    private String action;
    /** Service. */
    @JsonProperty("TargetUrl")
    private String service;

    /**
     * Create new instance.
     * @param entity Source entity
     * @return JSONRule instance.
     */
    public static JSONRule newInstance(OEntity entity) {
        JSONRule instance = new JSONRule();
        instance.setExternal(entity.getProperty("EventExternal", Boolean.class).getValue());
        instance.setSubject(entity.getProperty("EventSubject", String.class).getValue());
        instance.setType(entity.getProperty("EventType", String.class).getValue());
        instance.setObject(entity.getProperty("EventObject", String.class).getValue());
        instance.setInfo(entity.getProperty("EventInfo", String.class).getValue());
        instance.setAction(entity.getProperty("Action", String.class).getValue());
        instance.setService(entity.getProperty("TargetUrl", String.class).getValue());
        return instance;
    }

    /**
     * Get value of External.
     * @return external flag
     */
    public boolean isExternal() {
        return external;
    }

    /**
     * Set value of External.
     * @param external external flag
     */
    public void setExternal(boolean external) {
        this.external = external;
    }

    /**
     * Get value of Subject.
     * @return value of Subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Set value of Subject.
     * @param subject subject string
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Get value of Type.
     * @return value of Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set value of Type.
     * @param type type string
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get value of Object.
     * @return value of Object
     */
    public String getObject() {
        return object;
    }

    /**
     * Set value of Object.
     * @param object object string
     */
    public void setObject(String object) {
        this.object = object;
    }

    /**
     * Get value of Info.
     * @return value of Info
     */
    public String getInfo() {
        return info;
    }

    /**
     * Set value of Info.
     * @param info info string
     */
    public void setInfo(String info) {
        this.info = info;
    }

    /**
     * Get value of Action.
     * @return value of Action
     */
    public String getAction() {
        return action;
    }

    /**
     * Set value of Action.
     * @param action action string
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Get value of Service.
     * @return value of Service
     */
    public String getService() {
        return service;
    }

    /**
     * Set value of Service.
     * @param service service url
     */
    public void setService(String service) {
        this.service = service;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("EventExternal", this.external);
        json.put("EventSubject", this.subject);
        json.put("EventType", this.type);
        json.put("EventObject", this.object);
        json.put("EventInfo", this.info);
        json.put("Action", this.action);
        json.put("TargetUrl", this.service);
        return json;
    }
}

/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.event;

import java.util.Date;
import java.util.List;

import io.personium.common.auth.token.Role;
import io.personium.core.model.DavRsCmp;

/**
 * Event.
 */
public class PersoniumEvent {
    /** value of internal event. */
    public static final Boolean INTERNAL_EVENT = Boolean.FALSE;
    /** value of external event. */
    public static final Boolean EXTERNAL_EVENT = Boolean.TRUE;

    /** max length of event string value. */
    public static final int MAX_EVENT_VALUE_LENGTH = 1024 * 50;

    Boolean external = null;
    String schema = null;
    String subject = null;
    String type = null;
    String object = null;
    String info = null;
    String requestKey = null;
    String eventId = null;
    String ruleChain = null;
    String via = null;
    String roles = null;
    String cellId = null;
    long time;

    /** Constructor. */
    public PersoniumEvent() {
        // default is internal event.
        this.external = INTERNAL_EVENT;
    }

    /**
     * Constructor.
     * @param type event type
     * @param object object of event
     * @param info information on event
     * @param requestKey key string for request
     */
    public PersoniumEvent(
            final String type,
            final String object,
            final String info,
            final String requestKey) {
        this.external = INTERNAL_EVENT;
        this.type = type;
        this.object = object;
        this.info = info;
        this.requestKey = requestKey;
    }

    /**
     * Constructor.
     * @param external flag for event kind
     * @param schema box schema uri
     * @param subject subject
     * @param type event type
     * @param object object of event
     * @param info information on event
     * @param requestKey key string for request
     * @param eventId event id
     * @param ruleChain string to check chain of rule
     * @param via list of cell url that event was relayed
     * @param roles list of role class url
     */
    public PersoniumEvent(Boolean external,
            final String schema,
            final String subject,
            final String type,
            final String object,
            final String info,
            final String requestKey,
            final String eventId,
            final String ruleChain,
            final String via,
            final String roles) {
        this.external = external;
        this.schema = schema;
        this.subject = subject;
        this.type = type;
        this.object = object;
        this.info = info;
        this.requestKey = requestKey;
        this.eventId = eventId;
        this.ruleChain = ruleChain;
        this.via = via;
        this.roles = roles;
    }

    /**
     * Constructor.
     * @param external flag for event kind
     * @param type event type
     * @param object object of event
     * @param info information on event
     * @param davRsCmp DavRsCmp object
     */
    public PersoniumEvent(Boolean external,
            final String type,
            final String object,
            final String info,
            final DavRsCmp davRsCmp) {
        this(external, type, object, info, davRsCmp, davRsCmp.getRequestKey());
    }

    /**
     * Constructor.
     * @param external flag for event kind
     * @param type event type
     * @param object object of event
     * @param info information on event
     * @param davRsCmp DavRsCmp object
     * @param requestKey key string for request
     */
    public PersoniumEvent(Boolean external,
            final String type,
            final String object,
            final String info,
            final DavRsCmp davRsCmp,
            final String requestKey) {
        this.external = external;
        this.type = type;
        this.object = object;
        this.info = info;
        this.schema = davRsCmp.getAccessContext().getSchema();
        this.subject = davRsCmp.getAccessContext().getSubject();
        this.requestKey = requestKey;
        this.eventId = davRsCmp.getEventId();
        this.ruleChain = davRsCmp.getRuleChain();
        this.via = davRsCmp.getVia();

        // roles
        List<Role> roleList = davRsCmp.getAccessContext().getRoleList();
        for (Role role : roleList) {
            if (this.roles == null) {
                this.roles = role.createUrl();
            } else {
                this.roles += "," + role.createUrl();
            }
        }
    }

    /**
     * Get value of Subject.
     * @return value of Subject
     */
    public final String getSubject() {
        return subject;
    }

    /**
     * Set value of Subject.
     * @param subject subject string
     */
    public final void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Get value of Type.
     * @return value of Type
     */
    public final String getType() {
        return type;
    }

    /**
     * Get value of Schema.
     * @return value of Schema
     */
    public final String getSchema() {
        return schema;
    }

    /**
     * Set value of Type.
     * @param type type string
     */
    public final void setType(String type) {
        this.type = type;
    }

    /**
     * Get value of Object.
     * @return object string
     */
    public final String getObject() {
        return object;
    }

    /**
     * Set value of Object.
     * @param object object string
     */
    public final void setObject(String object) {
        this.object = object;
    }

    /**
     * Get value of External.
     * @return external flag
     */
    public Boolean getExternal() {
        return external;
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
    public final void setInfo(String info) {
        this.info = info;
    }

    /**
     * Get value of RequestKey.
     * @return value of RequestKey
     */
    public final String getRequestKey() {
        return requestKey;
    }

    /**
     * Get value of EventId.
     * @return value of EventId
     */
    public final String getEventId() {
        return eventId;
    }

    /**
     * Get value of RuleChain.
     * @return value of RuleChain
     */
    public final String getRuleChain() {
        return ruleChain;
    }

    /**
     * Get value of Via.
     * @return value of Via
     */
    public final String getVia() {
        return via;
    }

    /**
     * Get value of roles.
     * @return value of roles
     */
    public final String getRoles() {
        return roles;
    }

    /**
     * Get value of CellId.
     * @return value of CellId
     */
    public final String getCellId() {
        return cellId;
    }

    /**
     * Set value of CellId.
     * @param cellId cellId string
     */
    void setCellId(String cellId) {
        this.cellId = cellId;
    }

    /**
     * Get value of time.
     * @return time
     */
    public long getTime() {
        return time;
    }

    /**
     * Set time to now.
     */
    void setTime() {
        this.time = new Date().getTime();
    }

    /**
     * Set time.
     * @param time timestamp
     */
    void setTime(long time) {
        this.time = time;
    }

    /**
     * Copy and override PersoniumEvent object.
     * @param typeValue event type
     * @param objectValue object of event
     * @param infoValue information of event
     * @param eventIdValue event id
     * @param ruleChainValue string to check chain of rule
     * @return created PersoniumEvent object
     */
    public PersoniumEvent copy(String typeValue, String objectValue, String infoValue,
            String eventIdValue, String ruleChainValue) {
        PersoniumEvent event = new PersoniumEvent(INTERNAL_EVENT, this.schema, this.subject,
                typeValue, objectValue, infoValue, this.requestKey, eventIdValue, ruleChainValue, null, null);
        event.setCellId(this.cellId);
        event.setTime();
        return event;
    }

    /**
     * Validate Type property.
     * @param typeValue the property
     * @return true if validation is success, false if validation is fail
     */
    public static boolean validateType(final String typeValue) {
        return validateStringValue(typeValue);
    }

    /**
     * Validate Object property.
     * @param objectValue the property
     * @return true if validation is success, false if validation is fail
     */
    public static boolean validateObject(final String objectValue) {
        return validateStringValue(objectValue);
    }
    /**
     * Validate Info property.
     * @param infoValue the property
     * @return true if validation is success, false if validation is fail
     */
    public static boolean validateInfo(final String infoValue) {
        return validateStringValue(infoValue);
    }

    /**
     * Validate the property.
     * @param value the property
     * @return true if validation is success, false if validation is fail
     */
    static boolean validateStringValue(final String value) {
        boolean retValue = true;
        if (value == null) {
            retValue = false;
        } else if (value.length() > MAX_EVENT_VALUE_LENGTH) {
            retValue = false;
        }
        return retValue;
    }

}

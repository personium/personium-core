/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
    String cellId = null;

    /** Constructor. */
    public PersoniumEvent() {
        // default is internal event.
        this.external = INTERNAL_EVENT;
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
     */
    public PersoniumEvent(Boolean external,
            final String schema,
            final String subject,
            final String type,
            final String object,
            final String info,
            final String requestKey) {
        this.external = external;
        this.schema = schema;
        this.subject = subject;
        this.type = type;
        this.object = object;
        this.info = info;
        this.requestKey = requestKey;
    }

    /**
     * Constructor.
     * @param schema box schema uri
     * @param subject subject
     * @param type event type
     * @param object object of event
     * @param info information on event
     * @param requestKey key string for request
     */
    public PersoniumEvent(
            final String schema,
            final String subject,
            final String type,
            final String object,
            final String info,
            final String requestKey) {
        this.external = INTERNAL_EVENT;
        this.schema = schema;
        this.subject = subject;
        this.type = type;
        this.object = object;
        this.info = info;
        this.requestKey = requestKey;
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
    public final void setCellId(String cellId) {
        this.cellId = cellId;
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

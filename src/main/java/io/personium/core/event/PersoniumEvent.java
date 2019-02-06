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

import java.net.MalformedURLException;
import java.net.URL;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.personium.common.auth.token.Role;
import io.personium.core.model.DavRsCmp;
import io.personium.core.utils.UriUtils;

/**
 * Event.
 */
public class PersoniumEvent implements Serializable {
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
    PersoniumEvent() {
    }

    /**
     * Get value of Subject.
     * @return value of Subject
     */
    public final Optional<String> getSubject() {
        return Optional.ofNullable(subject);
    }

    /**
     * Get value of Type.
     * @return value of Type
     */
    public final Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Get value of Schema.
     * @return value of Schema
     */
    public final Optional<String> getSchema() {
        return Optional.ofNullable(schema);
    }

    /**
     * Get value of Object.
     * @return object string
     */
    public final Optional<String> getObject() {
        return Optional.ofNullable(object);
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
    public final Optional<String> getInfo() {
        return Optional.ofNullable(info);
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
    public final Optional<String> getRequestKey() {
        return Optional.ofNullable(requestKey);
    }

    /**
     * Get value of EventId.
     * @return value of EventId
     */
    public final Optional<String> getEventId() {
        return Optional.ofNullable(eventId);
    }

    /**
     * Get value of RuleChain.
     * @return value of RuleChain
     */
    public final Optional<String> getRuleChain() {
        return Optional.ofNullable(ruleChain);
    }

    /**
     * Get value of Via.
     * @return value of Via
     */
    public final Optional<String> getVia() {
        return Optional.ofNullable(via);
    }

    /**
     * Get value of roles.
     * @return value of roles
     */
    public final Optional<String> getRoles() {
        return Optional.ofNullable(roles);
    }

    /**
     * Get role list.
     * @return role list
     */
    public final List<Role> getRoleList() {
        return getRoles().map(rolesStr ->
                              Stream.of(rolesStr.split(Pattern.quote(",")))
                                    .map(r -> {
                                         try {
                                             URL url = new URL(r);
                                             return new Role(url);
                                         } catch (MalformedURLException e) {
                                             return null;
                                         }
                                     })
                                    .filter(role -> role != null)
                                    .collect(Collectors.toList()))
                         .orElse(new ArrayList<Role>());
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
     * Get value of timeStamp.
     * @return time
     */
    public long getTime() {
        return time;
    }

    /**
     * Set timeStamp to now.
     */
    void setTime() {
        this.time = new Date().getTime();
    }

    /**
     * Set timeStamp.
     * @param time timestamp
     */
    void setTime(long time) {
        this.time = time;
    }

    /**
     * Clone PersonimEvent and return PersoniumEvent.Builder.
     * @return PersoniumEvent.Builder
     */
    public PersoniumEvent.Builder clone() {
        return new PersoniumEvent.Builder().clone(this);
    }

    /**
     * Reset subject to null.
     */
    public void resetSubject() {
        this.subject = null;
    }

    /**
     * Convert from personium-localcell to http scheme.
     * @param cellUrl cell url of cell that event belongs to
     */
    public void convertObject(String cellUrl) {
        this.object = UriUtils.convertSchemeFromLocalCellToHttp(cellUrl, this.object);
    }

    /**
     * Convert PersoniumEvent object to publish.
     */
    public void convertToPublish() {
        this.roles = null;
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

    /**
     * Builder class to build PersoniumEvent object.
     */
    public static class Builder {
        private Boolean external;
        private String schema;
        private String subject;
        private String type;
        private String object;
        private String info;
        private String requestKey;
        private String eventId;
        private String ruleChain;
        private String via;
        private String roles;
        private String cellId;
        private long time;
        private DavRsCmp davRsCmp;

        /**
         * Constructor.
         */
        public Builder() {
            this.external = INTERNAL_EVENT;
            this.cellId = ""; // must not be null
        }

        /**
         * Set to external event.
         * @return a reference to this object
         */
        public Builder external() {
            this.external = EXTERNAL_EVENT;
            return this;
        }

        /**
         * Set schema.
         * @param schema box schema url
         * @return a reference to this object
         */
        public Builder schema(String schema) { // CHECKSTYLE IGNORE
            this.schema = schema;
            return this;
        }

        /**
         * Set subject.
         * @param subject subject
         * @return a reference to this object
         */
        public Builder subject(String subject) { // CHECKSTYLE IGNORE
            this.subject = subject;
            return this;
        }

        /**
         * Set type.
         * @param type event type
         * @return a reference to this object
         */
        public Builder type(String type) { // CHECKSTYLE IGNORE
            this.type = type;
            return this;
        }

        /**
         * Set object.
         * @param object object of event
         * @return a reference to this object
         */
        public Builder object(String object) { // CHECKSTYLE IGNORE
            this.object = object;
            return this;
        }

        /**
         * Set info.
         * @param info information of event
         * @return a reference to this object
         */
        public Builder info(String info) { // CHECKSTYLE IGNORE
            this.info = info;
            return this;
        }

        /**
         * Set request key.
         * @param requestKey key string of event
         * @return a reference to this object
         */
        public Builder requestKey(String requestKey) { // CHECKSTYLE IGNORE
            this.requestKey = requestKey;
            return this;
        }

        /**
         * Set event id.
         * @param eventId id of event
         * @return a reference to this object
         */
        public Builder eventId(String eventId) { // CHECKSTYLE IGNORE
            this.eventId = eventId;
            return this;
        }

        /**
         * Set rule chain.
         * @param ruleChain string to check chain of rule
         * @return a reference to this object
         */
        public Builder ruleChain(String ruleChain) { // CHECKSTYLE IGNORE
            this.ruleChain = ruleChain;
            return this;
        }

        /**
         * Set via.
         * @param via list of cell url that event was relayed
         * @return a reference to this object
         */
        public Builder via(String via) { // CHECKSTYLE IGNORE
            this.via = via;
            return this;
        }

        /**
         * Set role list.
         * @param roles list of role class url
         * @return a reference to this object
         */
        public Builder roles(String roles) { // CHECKSTYLE IGNORE
            this.roles = roles;
            return this;
        }

        /**
         * Set cell id.
         * @param cellId id of cell that event belongs to
         * @return a reference to this object
         */
        Builder cellId(String cellId) { // CHECKSTYLE IGNORE
            this.cellId = cellId;
            return this;
        }

        /**
         * Set time that event has occurred.
         * @param time time stamp
         * @return a reference to this object
         */
        Builder time(long time) { // CHECKSTYLE IGNORE
            this.time = time;
            return this;
        }

        /**
         * Set time to now.
         * @return a reference to this object
         */
        Builder time() {
            this.time = new Date().getTime();
            return this;
        }

        /**
         * Set DavRsCmp.
         * @param davRsCmp DavRsCmp object
         * @return a reference to this object
         */
        public Builder davRsCmp(DavRsCmp davRsCmp) { // CHECKSTYLE IGNORE
            this.davRsCmp = davRsCmp;
            return this;
        }

        /**
         * Clone event.
         * @param event PersoniumEvent object
         * @return a reference to this object
         */
        Builder clone(PersoniumEvent event) {
            this.external = event.external;
            this.schema = event.schema;
            this.subject = event.subject;
            this.type = event.type;
            this.object = event.object;
            this.info = event.info;
            this.requestKey = event.requestKey;
            this.eventId = event.eventId;
            this.ruleChain = event.ruleChain;
            this.via = event.via;
            this.roles = event.roles;
            this.cellId = event.cellId;
            this.time = event.time;

            return this;
        }

        /**
         * Return a PersoniumEvent object.
         * @return built PersoniumEvent object
         */
        public PersoniumEvent build() {
            return new PersoniumEvent(this);
        }
    }

    /**
     * Constructor for Builder.
     * @param builder Builder
     */
    private PersoniumEvent(Builder builder) {
        this.external = builder.external;
        this.schema = builder.schema;
        this.subject = builder.subject;
        this.type = builder.type;
        this.object = builder.object;
        this.info = builder.info;
        this.requestKey = builder.requestKey;
        this.eventId = builder.eventId;
        this.ruleChain = builder.ruleChain;
        this.via = builder.via;
        this.roles = builder.roles;
        this.cellId = builder.cellId;
        this.time = builder.time;

        if (builder.davRsCmp != null) {
            if (this.requestKey == null) {
                this.requestKey = builder.davRsCmp.getRequestKey();
            }
            if (this.schema == null) {
                this.schema = builder.davRsCmp.getAccessContext().getSchema();
            }
            if (this.subject == null) {
                this.subject = builder.davRsCmp.getAccessContext().getSubject();
            }
            if (this.eventId == null) {
                this.eventId = builder.davRsCmp.getEventId();
            }
            if (this.ruleChain == null) {
                this.ruleChain = builder.davRsCmp.getRuleChain();
            }
            if (this.via == null) {
                this.via = builder.davRsCmp.getVia();
            }
            if (this.roles == null) {
                List<Role> roleList = builder.davRsCmp.getAccessContext().getRoleList();
                List<String> list = roleList.stream().map(role -> role.createUrl()).collect(Collectors.toList());
                this.roles = String.join(",", list);
            }
        }
    }

}

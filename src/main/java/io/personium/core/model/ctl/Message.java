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
package io.personium.core.model.ctl;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * Base class for Edm definition abount Message.
 */
public class Message {
    /**
     * Constructor.
     */
    protected Message() {
    }

    /** Type message. */
    public static final String TYPE_MESSAGE = "message";
    /** Type register relation. */
    public static final String TYPE_REQ_RELATION_BUILD = "req.relation.build";
    /** Type delete relation. */
    public static final String TYPE_REQ_RELATION_BREAK = "req.relation.break";
    /** Type register relation role. */
    public static final String TYPE_REQ_ROLE_GRANT = "req.role.grant";
    /** Type delete relation role. */
    public static final String TYPE_REQ_ROLE_REVOKE = "req.role.revoke";
    /** Type register rule. */
    public static final String TYPE_REQ_RULE_REGISTER = "req.rule.register";
    /** Type unregister rule. */
    public static final String TYPE_REQ_RULE_UNREGISTER = "req.rule.unregister";

    /** Status unread. */
    public static final String STATUS_UNREAD = "unread";
    /** Status read. */
    public static final String STATUS_READ = "read";
    /** Status none. */
    public static final String STATUS_NONE = "none";
    /** Status approved. */
    public static final String STATUS_APPROVED = "approved";
    /** Status rejected. */
    public static final String STATUS_REJECTED = "rejected";

    /** Command key string. */
    public static final String MESSAGE_COMMAND = "Command";

    /** Extended schema Format definition(Message RequestRelation). */
    public static final String P_FORMAT_PATTERN_MESSAGE_REQUEST_RELATION = "message-request-relation";

    /** Pattern InReplyTo. */
    public static final String PATTERN_IN_REPLY_TO = "^.{32}$";
    /**
     * Pattern message type.
     *    message
     *    req.relation.build / req.relation.break
     *    req.role.grant / req.role.revoke
     *    req.rule.register / req.rule.unregister
     */
    public static final String PATTERN_MESSAGE_TYPE = "^(message)|(req\\.relation\\.build)|(req\\.relation\\.break)|(req\\.role\\.grant)|(req\\.role\\.revoke)|(req\\.rule\\.register)|(req\\.rule\\.unregister)$"; // CHECKSTYLE IGNORE - To maintain readability
    /** Pattern message title. */
    public static final String PATTERN_MESSAGE_TITLE = "^.{0,256}$";
    /** Pattern message priority. */
    public static final String PATTERN_MESSAGE_PRIORITY = "^[1-5]$";

    /** Max length of message body. */
    public static final int MAX_MESSAGE_BODY_LENGTH = 1024 * 64;

    /**
     * Annotations for InReplyTo.
     */
    protected static final List<EdmAnnotation<?>> P_FORMAT_IN_REPLY_TO = new ArrayList<EdmAnnotation<?>>();
    /**
     * Annotations for MessageType.
     */
    private static final List<EdmAnnotation<?>> P_FORMAT_MESSAGE_TYPE = new ArrayList<EdmAnnotation<?>>();
    /**
     * Annotations for MessageTitle.
     */
    private static final List<EdmAnnotation<?>> P_FORMAT_MESSAGE_TITLE = new ArrayList<EdmAnnotation<?>>();
    /**
     * Annotations for MessagePriority.
     */
    private static final List<EdmAnnotation<?>> P_FORMAT_MESSAGE_PRIORITY = new ArrayList<EdmAnnotation<?>>();
    /**
     * Definition of p: Format for MessageRequestRelation item.
     */
    private static final List<EdmAnnotation<?>> P_FORMAT_MESSAGE_REQUEST_RELATION = new ArrayList<EdmAnnotation<?>>();

    static {
        P_FORMAT_IN_REPLY_TO.add(createFormatInReplyToAnnotation());
        P_FORMAT_MESSAGE_TYPE.add(createFormatMessageTypeAnnotation());
        P_FORMAT_MESSAGE_TITLE.add(createFormatMessageTitleAnnotation());
        P_FORMAT_MESSAGE_PRIORITY.add(createFormatMessagePriorityAnnotation());
        P_FORMAT_MESSAGE_REQUEST_RELATION.add(createFormatMessageRequestRelation());
    }

    /**
     * Get annotation for InReplyTo.
     * @return annotation for InReplyTo
     */
    private static EdmAnnotation<?> createFormatInReplyToAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_IN_REPLY_TO + "')");
    }

    /**
     * Get annotation for MessageType.
     * @return annotation for MessageType
     */
    private static EdmAnnotation<?> createFormatMessageTypeAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_MESSAGE_TYPE + "')");
    }

    /**
     * Get annotation for MessageTitle.
     * @return annotation for MessageTitle
     */
    private static EdmAnnotation<?> createFormatMessageTitleAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_MESSAGE_TITLE + "')");
    }

    /**
     * Get annotation for MessagePriority.
     * @return annotation for MessagePriority
     */
    private static EdmAnnotation<?> createFormatMessagePriorityAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_MESSAGE_PRIORITY + "')");
    }

    /**
     * Return p: Format Annotation for MessageRequestRelation item.
     * @return annotation for MessageRequestRelation
     */
    private static EdmAnnotation<?> createFormatMessageRequestRelation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_MESSAGE_REQUEST_RELATION);
    }

    /**
     * InReplyTo property.
     */
    public static final EdmProperty.Builder P_IN_REPLY_TO = EdmProperty.newBuilder("InReplyTo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_IN_REPLY_TO);
    /**
     * Type property.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue(TYPE_MESSAGE)
            .setAnnotations(P_FORMAT_MESSAGE_TYPE);
    /**
     * Title propert.
     */
    public static final EdmProperty.Builder P_TITLE = EdmProperty.newBuilder("Title")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue("")
            .setAnnotations(P_FORMAT_MESSAGE_TITLE);
    /**
     * Body property.
     */
    public static final EdmProperty.Builder P_BODY = EdmProperty.newBuilder("Body")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue("");
    /**
     * Priority property.
     */
    public static final EdmProperty.Builder P_PRIORITY = EdmProperty.newBuilder("Priority")
            .setType(EdmSimpleType.INT32)
            .setNullable(true)
            .setDefaultValue("3")
            .setAnnotations(P_FORMAT_MESSAGE_PRIORITY);
    /**
     * RequestRelation property.
     */
    public static final EdmProperty.Builder P_REQUEST_RELATION = EdmProperty.newBuilder("RequestRelation")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_MESSAGE_REQUEST_RELATION);
    /**
     * RequestRelationTarget property.
     */
    public static final EdmProperty.Builder P_REQUEST_RELATION_TARGET = EdmProperty.newBuilder("RequestRelationTarget")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_URI);
}
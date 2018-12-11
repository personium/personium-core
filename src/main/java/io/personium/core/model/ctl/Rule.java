/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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

import org.core4j.Enumerable;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * Edm definition of Rule.
 */
public class Rule {
    /**
     * Constructor.
     */
    private Rule() {
    }

    /**
     * Edm Entity Type Name.
     */
    public static final String EDM_TYPE_NAME = "Rule";

    /** action exec. */
    public static final String ACTION_EXEC = "exec";
    /** action relay. */
    public static final String ACTION_RELAY = "relay";
    /** action relay.data. */
    public static final String ACTION_RELAY_DATA = "relay.data";
    /** action relay.event. */
    public static final String ACTION_RELAY_EVENT = "relay.event";
    /** action log. */
    public static final String ACTION_LOG = "log";
    /** action log.info. */
    public static final String ACTION_LOG_INFO = "log.info";
    /** action log.warn. */
    public static final String ACTION_LOG_WARN = "log.warn";
    /** action log.error. */
    public static final String ACTION_LOG_ERROR = "log.error";

    /** Extended Schema Format rule-object. */
    public static final String P_FORMAT_PATTERN_RULE_OBJECT = "rule-object";
    /** Extended Schema Format rule-targeturl. */
    public static final String P_FORMAT_PATTERN_RULE_TARGETURL = "rule-targeturl";

    /** Pattern action. */
    private static final String PATTERN_ACTION = "^(exec)|(relay)|(relay\\.data)|(relay\\.event)|(log)|(log\\.info)|(log\\.warn)|(log\\.error)$"; // CHECKSTYLE IGNORE - To maintein readability

    /** Annotations for Object. */
    private static final List<EdmAnnotation<?>> P_FORMAT_OBJECT = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for Action. */
    private static final List<EdmAnnotation<?>> P_FORMAT_ACTION = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for TargetUrl. */
    private static final List<EdmAnnotation<?>> P_FORMAT_TARGETURL = new ArrayList<EdmAnnotation<?>>();

    static {
        P_FORMAT_OBJECT.add(createFormatObjectAnnotation());
        P_FORMAT_ACTION.add(createFormatActionAnnotation());
        P_FORMAT_TARGETURL.add(createFormatTargetUrlAnnotation());
    }

    /**
     * Get annotation for Object.
     * @return annotation for Object
     */
    private static EdmAnnotation<?> createFormatObjectAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_RULE_OBJECT);
    }

    /**
     * Get annotation for Action.
     * @return annotation for Action
     */
    private static EdmAnnotation<?> createFormatActionAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_ACTION + "')");
    }

    /**
     * Get annotation for TargetUrl.
     * @return annotation for TargetUrl
     */
    private static EdmAnnotation<?> createFormatTargetUrlAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_RULE_TARGETURL);
    }

    /**
     * Name property.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setDefaultValue(Common.UUID)
            .setAnnotations(Common.P_FORMAT_ID);
    /**
     * EventExternal property.
     */
    public static final EdmProperty.Builder P_EXTERNAL = EdmProperty.newBuilder("EventExternal")
            .setType(EdmSimpleType.BOOLEAN)
            .setNullable(true)
            .setDefaultValue("false");
    /**
     * EventSubjcet property.
     */
    public static final EdmProperty.Builder P_SUBJECT = EdmProperty.newBuilder("EventSubject")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_URI);
    /**
     * EventType property.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("EventType")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(Common.P_FORMAT_NAME_WITH_SIGN);
    /**
     * EventObject property.
     */
    public static final EdmProperty.Builder P_OBJECT = EdmProperty.newBuilder("EventObject")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_OBJECT);
    /**
     * EventInfo property.
     */
    public static final EdmProperty.Builder P_INFO = EdmProperty.newBuilder("EventInfo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * Action property.
     */
    public static final EdmProperty.Builder P_ACTION = EdmProperty.newBuilder("Action")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_ACTION);
    /**
     * TargetUrl property.
     */
    public static final EdmProperty.Builder P_TARGETURL = EdmProperty.newBuilder("TargetUrl")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_TARGETURL);

    /**
     * EntityType Builder.
     */
    static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(P_NAME, Common.P_BOX_NAME,
                            P_EXTERNAL, P_SUBJECT, P_TYPE, P_OBJECT, P_INFO, P_ACTION, P_TARGETURL,
                            Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(Common.P_NAME.getName(), Common.P_BOX_NAME.getName());

}

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
package io.personium.core.model.ctl;

import java.util.ArrayList;
import java.util.List;

import org.core4j.Enumerable;
import org.odata4j.edm.EdmAnnotation;
import org.odata4j.edm.EdmAnnotationAttribute;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * Edm definition of RequestObject.
 */
public class RequestObject {

    /** RequestType relation.add. */
    public static final String REQUEST_TYPE_RELATION_ADD = "relation.add";
    /** RequestType relation.remove. */
    public static final String REQUEST_TYPE_RELATION_REMOVE = "relation.remove";
    /** RequestType role.add. */
    public static final String REQUEST_TYPE_ROLE_ADD = "role.add";
    /** RequestType role.remove. */
    public static final String REQUEST_TYPE_ROLE_REMOVE = "role.remove";
    /** RequestType rule.add. */
    public static final String REQUEST_TYPE_RULE_ADD = "rule.add";
    /** RequestType rule.remove. */
    public static final String REQUEST_TYPE_RULE_REMOVE = "rule.remove";

    /** Extended schema format Name. */
    public static final String P_FORMAT_PATTERN_NAME = "request-name";
    /** Extended schema format TargetUrl. */
    public static final String P_FORMAT_PATTERN_TARGET_URL = "request-target-url";
    /** Extended schema format ClassUrl. */
    public static final String P_FORMAT_PATTERN_CLASS_URL = "request-class-url";

    /** Pattern RequestType. */
    private static final String PATTERN_REQUEST_TYPE = "^(relation\\.add)"
            + "|(relation\\.remove)"
            + "|(role\\.add)"
            + "|(role\\.remove)"
            + "|(rule\\.add)"
            + "|(rule\\.remove)$";

    /** Annotations for RequestType. */
    private static final List<EdmAnnotation<?>> P_FORMAT_REQUEST_TYPE = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for Name. */
    private static final List<EdmAnnotation<?>> P_FORMAT_NAME = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for TargetUrl. */
    private static final List<EdmAnnotation<?>> P_FORMAT_TARGET_URL = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for ClassUrl. */
    private static final List<EdmAnnotation<?>> P_FORMAT_CLASS_URL = new ArrayList<EdmAnnotation<?>>();

    // Initialization of format annotation.
    static {
        P_FORMAT_REQUEST_TYPE.add(createFormatRequestTypeAnnotation());
        P_FORMAT_NAME.add(createFormatNameAnnotation());
        P_FORMAT_TARGET_URL.add(createFormatTargetUrlAnnotation());
        P_FORMAT_CLASS_URL.add(createFormatClassUrlAnnotation());
    }

    /**
     * Constructor.
     */
    private RequestObject() {
    }

    // ------------------------------
    // Edm annotation settings.
    // ------------------------------
    /**
     * Create annotation for RequestType.
     * @return annotation for RequestType
     */
    private static EdmAnnotation<?> createFormatRequestTypeAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('" + PATTERN_REQUEST_TYPE + "')");
    }

    /**
     * Create annotation for Name.
     * @return annotation for Name
     */
    private static EdmAnnotation<?> createFormatNameAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_NAME);
    }

    /**
     * Create annotation for TargetUrl.
     * @return annotation for TargetUrl
     */
    private static EdmAnnotation<?> createFormatTargetUrlAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_TARGET_URL);
    }

    /**
     * Create annotation for ClassUrl.
     * @return annotation for ClassUrl
     */
    private static EdmAnnotation<?> createFormatClassUrlAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, P_FORMAT_PATTERN_CLASS_URL);
    }

    // ------------------------------
    // Edm property settings.
    // ------------------------------
    /** RequestType property. */
    public static final EdmProperty.Builder P_REQUEST_TYPE = EdmProperty.newBuilder("RequestType")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(P_FORMAT_REQUEST_TYPE);

    /** Name property. */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
//            .setDefaultValue(Common.UUID)
            .setAnnotations(P_FORMAT_NAME);

    /** TargetUrl property. */
    public static final EdmProperty.Builder P_TARGET_URL = EdmProperty.newBuilder("TargetUrl")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_TARGET_URL);

    /** ClassUrl property. */
    public static final EdmProperty.Builder P_CLASS_URL = EdmProperty.newBuilder("ClassUrl")
            .setType(EdmSimpleType.STRING)
            .setNullable(true)
            .setAnnotations(P_FORMAT_CLASS_URL);

    // ------------------------------
    // Edm complex type setting.
    // ------------------------------
    /** ComplexType Builder. */
    public static final EdmComplexType.Builder COMPLEX_TYPE_REQUEST_OBJECT = EdmComplexType.newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName("Message_Request_Object")
            .addProperties(Enumerable.create(P_REQUEST_TYPE, P_NAME, P_TARGET_URL, P_CLASS_URL,
                    Rule.P_SUBJECT, Rule.P_TYPE, Rule.P_OBJECT, Rule.P_INFO, Rule.P_ACTION).toList());
}

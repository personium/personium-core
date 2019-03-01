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
 * Edm definition of Account.
 */
public class Account {
    private Account() {
    }

    /** Pattern IP address range. */
    private static final String P_FORMAT_PATTERN_IP_ADDRESS_RANGE = "ip-address-range";
    /** Annotations for IP address range. */
    private static final List<EdmAnnotation<?>> P_FORMAT_IP_ADDRESS_RANGE = new ArrayList<EdmAnnotation<?>>();
    /** Annotations for status. */
    private static final List<EdmAnnotation<?>> P_FORMAT_STATUS = new ArrayList<EdmAnnotation<?>>();

    /** Account status active. */
    public static final String STATUS_ACTIVE = "active";
    /** Account status deactivated. */
    public static final String STATUS_DEACTIVATED = "deactivated";
    /** Account status passwordChangeRequired. */
    public static final String STATUS_PASSWORD_CHANGE_REQUIRED = "passwordChangeRequired";
    /** Account statuses. */
    public static final String[] STATUSES = {STATUS_ACTIVE, STATUS_DEACTIVATED, STATUS_PASSWORD_CHANGE_REQUIRED};

    /**
     * Type value basic.
     */
    public static final String TYPE_VALUE_BASIC = "basic";

    /**
     * Edm EntityType name.
     */
    public static final String EDM_TYPE_NAME = "Account";

    /**
     * NavigationProperty name with ReceivedMessage.
     */
    public static final String EDM_NPNAME_FOR_RECEIVED_MESSAGE = "_ReceivedMessageRead";

     // Initialization of format annotation.
     static {
         P_FORMAT_IP_ADDRESS_RANGE.add(createFormatIPAddressRangeAnnotation());
         P_FORMAT_STATUS.add(createFormatStatusAnnotation());
     }

     /**
      * Create annotation for IP address range.
      * @return annotation for IP address range
      */
     private static EdmAnnotation<?> createFormatIPAddressRangeAnnotation() {
         return new EdmAnnotationAttribute(
                 Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                 Common.P_FORMAT, P_FORMAT_PATTERN_IP_ADDRESS_RANGE);
     }

     /**
     * Create annotation for status.
     * @return EdmAnnotation
     */
    private static EdmAnnotation<?> createFormatStatusAnnotation() {
        return new EdmAnnotationAttribute(
                Common.P_NAMESPACE.getUri(), Common.P_NAMESPACE.getPrefix(),
                Common.P_FORMAT, Common.P_FORMAT_PATTERN_REGEX + "('^(" + String.join("|", STATUSES) + ")$')");
    }

    /**
     * Definition field of Name property.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name").setType(EdmSimpleType.STRING)
            .setNullable(false).setAnnotations(Common.P_FORMAT_NAME_WITH_SIGN);

    /**
     * Definition of Type property.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type").setType(EdmSimpleType.STRING)
            .setDefaultValue(TYPE_VALUE_BASIC).setAnnotations(Common.P_FORMAT_ACCOUNT_TYPE);

    /**
     * Definition of Cell property.
     */
    public static final EdmProperty.Builder P_CELL = EdmProperty.newBuilder("Cell").setType(EdmSimpleType.STRING)
            .setNullable(true).setDefaultValue("null");

    /**
     * Definition field of IP address range.
     */
    public static final EdmProperty.Builder P_IP_ADDRESS_RANGE = EdmProperty.newBuilder("IPAddressRange")
            .setType(EdmSimpleType.STRING).setNullable(true).setDefaultValue("null")
            .setAnnotations(P_FORMAT_IP_ADDRESS_RANGE);

    /**
     * Definition field of status.
     */
    public static final EdmProperty.Builder P_STATUS = EdmProperty.newBuilder("Status").setType(EdmSimpleType.STRING)
            .setNullable(true).setDefaultValue(STATUS_ACTIVE).setAnnotations(P_FORMAT_STATUS);

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL).setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(P_NAME, P_TYPE, P_CELL, P_IP_ADDRESS_RANGE,
                    P_STATUS, Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_NAME.getName());
}

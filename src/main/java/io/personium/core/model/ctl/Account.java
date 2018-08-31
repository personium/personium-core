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

import org.core4j.Enumerable;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

/**
 * Edm definition of Account.
 */
public class Account {
    private Account() {
    }

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

    /**
     * Definition field of Name property.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name").setType(EdmSimpleType.STRING)
            .setNullable(false).setAnnotations(Common.P_FORMAT_NAME_WITH_SIGN);

    /**
     * LastAuthenticated property definition body.
     */
    public static final EdmProperty.Builder P_LAST_AUTHENTICATED = EdmProperty.newBuilder("LastAuthenticated")
            .setType(EdmSimpleType.DATETIME).setNullable(true).setDefaultValue("null");

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
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL).setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(P_NAME, P_LAST_AUTHENTICATED, P_TYPE, P_CELL,
                    Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_NAME.getName());
}

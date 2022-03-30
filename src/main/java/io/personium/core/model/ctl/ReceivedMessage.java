/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
 * Edm definition of ReceivedMessage.
 */
public class ReceivedMessage extends Message {
    /**
     * Constructor.
     */
    protected ReceivedMessage() {
        super();
    }

    /**
     * Edm Entity Type Name of ReceivedMessage.
     */
    public static final String EDM_TYPE_NAME = "ReceivedMessage";

    /**
     * NavigationProperty Name with Account.
     */
    public static final String EDM_NPNAME_FOR_ACCOUNT = "_AccountRead";

    /**
     * __id property.
     */
    public static final EdmProperty.Builder P_ID = EdmProperty.newBuilder("__id")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(P_FORMAT_IN_REPLY_TO);
    /**
     * From property.
     */
    public static final EdmProperty.Builder P_FROM = EdmProperty.newBuilder("From")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_CELL_URL);
    /**
     * MulticastTo property.
     */
    public static final EdmProperty.Builder P_MULTICAST_TO = EdmProperty.newBuilder("MulticastTo")
            .setType(EdmSimpleType.STRING)
            .setNullable(true);
    /**
     * Status property.
     */
    public static final EdmProperty.Builder P_STATUS = EdmProperty.newBuilder("Status")
            .setType(EdmSimpleType.STRING)
            .setNullable(false);
    /**
     * EntityType Builder.
     */
    static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(P_ID, Common.P_BOX_NAME, P_IN_REPLY_TO, P_FROM, P_MULTICAST_TO, P_TYPE,
                            P_TITLE, P_BODY, P_PRIORITY, P_STATUS, P_REQUEST_OBJECTS,
                            Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(Common.P_ID.getName());
}

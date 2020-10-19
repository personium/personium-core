/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
 * Edm definition body for external access.
 */
public class ExtRole {
    private ExtRole() {
    }
    /**
     * Edm EntityType name.
     */
    public static final String EDM_TYPE_NAME = "ExtRole";
    /**
     * ExtRole property definition body.
     */
    public static final EdmProperty.Builder P_EXT_ROLE = EdmProperty.newBuilder("ExtRole")
            .setAnnotations(Common.P_FORMAT_URI)
            .setType(EdmSimpleType.STRING).setNullable(false);
    /**
     * _Relation.Name Property definition body.
     */
    public static final EdmProperty.Builder P_RELATION_NAME = EdmProperty.newBuilder("_Relation.Name")
            .setType(EdmSimpleType.STRING)
            .setAnnotations(Common.P_FORMAT_RELATION_NAME)
            .setNullable(false);
    /**
     * _Relation._ Box.Name Property definition body.
     */
    public static final EdmProperty.Builder P_RELATION_BOX_NAME = EdmProperty.newBuilder("_Relation._Box.Name")
            .setType(EdmSimpleType.STRING)
            .setAnnotations(Common.P_FORMAT_NAME)
            .setNullable(true);
    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_CELL_CTL)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(P_EXT_ROLE, P_RELATION_NAME, P_RELATION_BOX_NAME,
                            Common.P_PUBLISHED, Common.P_UPDATED)
                            .toList())
            .addKeys(P_EXT_ROLE.getName(), P_RELATION_NAME.getName(), P_RELATION_BOX_NAME.getName());

}

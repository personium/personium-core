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
 * Edm definition of EntityType (misleading but this is a name) which is one of EntityType handling the schema of user data.
 * Although confusing, it is EntityType for defining EntityType.
 */
public class EntityType {
    private EntityType() {
    }

    /**
     * Edm EntityType name.
     */
    public static final String EDM_TYPE_NAME = "EntityType";
    /**
     * Definition field of Name property.
     */
    public static final EdmProperty.Builder P_ENTITYTYPE_NAME = EdmProperty.newBuilder("Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_NAME);

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_ODATA_SVC_SCHEMA).setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(P_ENTITYTYPE_NAME, Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_ENTITYTYPE_NAME.getName());

}

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
 * An Edm definition body of Association which is one of EntityType handling the schema of user data.
 */
public class AssociationEnd {
    private AssociationEnd() {
    }

    /**
     * Edm EntityType name.
     */
    public static final String EDM_TYPE_NAME = "AssociationEnd";
    /**
     * Definition field of Name property.
     */
    public static final EdmProperty.Builder P_ASSOCIATION_NAME = EdmProperty.newBuilder("Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_NAME);

    /**
     * Definition of Multiplicity property.
     */
    public static final EdmProperty.Builder P_MULTIPLICITY = EdmProperty.newBuilder("Multiplicity")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_MULTIPLICITY);

    /**
     * _EntityType / Name Definition of property.
     */
    public static final EdmProperty.Builder P_ENTITYTYPE_NAME = EdmProperty.newBuilder("_EntityType.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_NAME);

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType
            .newBuilder()
            .setNamespace(Common.EDM_NS_ODATA_SVC_SCHEMA)
            .setName(EDM_TYPE_NAME)
            .addProperties(
                    Enumerable.create(P_ASSOCIATION_NAME, P_MULTIPLICITY, P_ENTITYTYPE_NAME,
                            Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_ASSOCIATION_NAME.getName(), P_ENTITYTYPE_NAME.getName());

}

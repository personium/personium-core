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
 * ユーザデータのスキーマを扱うEntityTypeの一つである、EntityType（紛らわしいがこれは名前）のEdm 定義体.
 * 紛らわしいが、EntityTypeを定義するためのEntityTypeということ。
 */
public class Property {
    private Property() {
    }

    /**
     * Edm EntityType名.
     */
    public static final String EDM_TYPE_NAME = "Property";

    /**
     * CollectionKind None.
     */
    public static final String COLLECTION_KIND_NONE = "None";
    /**
     * Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name").setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.DC_FORMAT_NAME);
    /**
     * _EntityType/Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_ENTITYTYPE_NAME = EdmProperty.newBuilder("_EntityType.Name")
            .setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.DC_FORMAT_NAME);
    /**
     * Typeプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type")
            .setType(EdmSimpleType.STRING)
            .setNullable(false);
    /**
     * Nullableプロパティの定義体.
     */
    public static final EdmProperty.Builder P_NULLABLE = EdmProperty.newBuilder("Nullable")
            .setDefaultValue("true")
            .setNullable(false)
            .setType(EdmSimpleType.BOOLEAN);
    /**
     * DefaultValueプロパティの定義体.
     */
    public static final EdmProperty.Builder P_DEFAULT_VALUE = EdmProperty.newBuilder("DefaultValue")
            .setNullable(true)
            .setType(EdmSimpleType.STRING);
    /**
     * CollectionKindプロパティの定義体.
     */
    public static final EdmProperty.Builder P_COLLECTION_KIND = EdmProperty.newBuilder("CollectionKind")
            .setNullable(true)
            .setType(EdmSimpleType.STRING)
            .setDefaultValue(COLLECTION_KIND_NONE);
    /**
     * IsKeyプロパティの定義体.
     */
    public static final EdmProperty.Builder P_IS_KEY = EdmProperty.newBuilder("IsKey")
            .setNullable(false)
            .setType(EdmSimpleType.BOOLEAN)
            .setDefaultValue("false");
    /**
     * UniqueKeyプロパティの定義体.
     */
    public static final EdmProperty.Builder P_UNIQUE_KEY = EdmProperty.newBuilder("UniqueKey")
            .setNullable(true)
            .setType(EdmSimpleType.STRING)
            .setAnnotations(Common.DC_FORMAT_NAME);
    /**
     * IsDeclaredプロパティの定義体.
     */
    public static final EdmProperty.Builder P_IS_DECLARED = EdmProperty.newBuilder("IsDeclared")
            .setNullable(true)
            .setType(EdmSimpleType.BOOLEAN)
            .setDefaultValue("true");

    /**
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_ODATA_SVC_SCHEMA)
            .setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(P_NAME,
                    P_ENTITYTYPE_NAME,
                    P_TYPE,
                    P_NULLABLE,
                    P_DEFAULT_VALUE,
                    P_COLLECTION_KIND,
                    P_IS_KEY,
                    P_UNIQUE_KEY,
                    P_IS_DECLARED,
                    Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_NAME.getName(), P_ENTITYTYPE_NAME.getName());
}

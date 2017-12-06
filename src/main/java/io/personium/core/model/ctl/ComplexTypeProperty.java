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
public class ComplexTypeProperty {
    private ComplexTypeProperty() {
    }

    /**
     * Edm EntityType名.
     */
    public static final String EDM_TYPE_NAME = "ComplexTypeProperty";
    /**
     * CollectionKind None.
     */
    public static final String COLLECTION_KIND_NONE = "None";
    /**
     * Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_NAME = EdmProperty.newBuilder("Name").setType(EdmSimpleType.STRING)
            .setNullable(false)
            .setAnnotations(Common.P_FORMAT_NAME);
    /**
     * _ComplexType/Nameプロパティの定義体.
     */
    public static final EdmProperty.Builder P_COMPLEXTYPE_NAME = EdmProperty.newBuilder("_ComplexType.Name")
            .setNullable(false)
            .setType(EdmSimpleType.STRING)
            .setAnnotations(Common.P_FORMAT_NAME);
    /**
     * Typeプロパティの定義体.
     */
    public static final EdmProperty.Builder P_TYPE = EdmProperty.newBuilder("Type")
            .setNullable(false)
            .setType(EdmSimpleType.STRING);
    /**
     * Nullableプロパティの定義体.
     */
    public static final EdmProperty.Builder P_NULLABLE = EdmProperty.newBuilder("Nullable")
            .setNullable(false)
            .setDefaultValue("true")
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
     * EntityType Builder.
     */
    public static final EdmEntityType.Builder EDM_TYPE_BUILDER = EdmEntityType.newBuilder()
            .setNamespace(Common.EDM_NS_ODATA_SVC_SCHEMA)
            .setName(EDM_TYPE_NAME)
            .addProperties(Enumerable.create(P_NAME,
                    P_COMPLEXTYPE_NAME,
                    P_TYPE,
                    P_NULLABLE,
                    P_DEFAULT_VALUE,
                    P_COLLECTION_KIND,
                    Common.P_PUBLISHED, Common.P_UPDATED).toList())
            .addKeys(P_NAME.getName(), P_COMPLEXTYPE_NAME.getName());
}

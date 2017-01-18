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
package com.fujitsu.dc.test.jersey.box.odatacol;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataComplexTypeのUtilsクラス.
 */
public final class UserDataComplexTypeUtils {

    /** EntityType名. */
    public static final String ENTITY_TYPE_NAME = "entityType";

    /** ComplexType名. */
    public static final String COMPLEX_TYPE_NAME = "complexType1st";

    /** entityTypeの文字列プロパティ名. */
    public static final String ET_STRING_PROP = "etStrProp";

    /** entityTypeのComplexTypeプロパティ名. */
    public static final String ET_CT1ST_PROP = "etComplexProp";

    /** complexType1stの文字列プロパティ名. */
    public static final String CT1ST_STRING_PROP = "ct1stStrProp";

    /**
     * コンストラクタ.
     */
    private UserDataComplexTypeUtils() {
    }

    /**
     * ComplexTypeスキーマを作成する.
     * @param entityTypeName エンティティタイプ名
     * @param complexTypeName コンプレックスタイプ名
     * @param propertyName プロパティ名
     * @param complexTypePropertyName コンプレックスタイププロパティ名
     * @param innnerComplexTypePropertyName インナーコンプレックスタイププロパティ名
     */
    public static void createComplexTypeSchema(String entityTypeName,
            String complexTypeName,
            String propertyName,
            String complexTypePropertyName,
            String innnerComplexTypePropertyName) {
        createComplexTypeSchema(entityTypeName,
                complexTypeName,
                propertyName,
                complexTypePropertyName,
                innnerComplexTypePropertyName,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
    }

    /**
     * ComplexTypeスキーマを作成する.
     * @param entityTypeName エンティティタイプ名
     * @param complexTypeName コンプレックスタイプ名
     * @param propertyName プロパティ名
     * @param complexTypePropertyName コンプレックスタイププロパティ名
     * @param innnerComplexTypePropertyName インナーコンプレックスタイププロパティ名
     * @param typeName スキーマタイプ名
     */
    public static void createComplexTypeSchema(String entityTypeName,
            String complexTypeName,
            String propertyName,
            String complexTypePropertyName,
            String innnerComplexTypePropertyName,
            String typeName) {
        // EntityType作成
        EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

        // ComplexType作成
        UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, complexTypeName);

        // Property作成
        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propertyName, entityTypeName,
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null, false, null);
        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, complexTypePropertyName, entityTypeName,
                complexTypeName, false, null, null, false, null);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, innnerComplexTypePropertyName, complexTypeName,
                typeName, false, null, null);
    }

    /**
     * ComplexTypeSchemaを削除する.
     */
    public static void deleteComplexTypeSchema() {
        deleteComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP,
                UserDataComplexTypeUtils.CT1ST_STRING_PROP);
    }

    /**
     * ComplexTypeSchemaを削除する.
     * @param entityTypeName エンティティタイプ名
     * @param complexTypeName コンプレックスタイプ名
     * @param stringPropetyName 文字列型プロパティ名
     * @param complexPropertyName コンプレックス型プロパティ名
     * @param complexStringPropertyName コンプレックスタイプ内の文字列型プロパティ名
     */
    public static void deleteComplexTypeSchema(String entityTypeName, String complexTypeName,
            String stringPropetyName, String complexPropertyName, String complexStringPropertyName) {
        String ctlocationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                complexTypeName);
        String propStrlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                stringPropetyName, entityTypeName);
        String propCtlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                complexPropertyName, entityTypeName);
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                complexStringPropertyName, complexTypeName);

        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(propStrlocationUrl);
        ODataCommon.deleteOdataResource(propCtlocationUrl);
        // 作成したComplexTypePropertyを削除
        ODataCommon.deleteOdataResource(ctplocationUrl);
        // 作成したComplexTypeを削除
        ODataCommon.deleteOdataResource(ctlocationUrl);
        // 作成したEntityTypeを削除
        EntityTypeUtils.delete(Setup.TEST_ODATA, AbstractCase.MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, entityTypeName, Setup.TEST_CELL1, -1);
    }

    /**
     * シンプル型の配列スキーマを作成する.
     */
    public static void createSimpleArraySchema() {
        UserDataComplexTypeUtils.createComplexTypeSchema(ENTITY_TYPE_NAME, COMPLEX_TYPE_NAME, ET_STRING_PROP,
                ET_CT1ST_PROP, CT1ST_STRING_PROP);

        // 配列Property作成
        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "etListPropStr", ENTITY_TYPE_NAME,
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, "List", false, null);

        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "etListPropInt", ENTITY_TYPE_NAME,
                EdmSimpleType.INT32.getFullyQualifiedTypeName(), false, null, "List", false, null);

        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "etListPropSingle", ENTITY_TYPE_NAME,
                EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), false, null, "List", false, null);

        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "etListPropBoolean", ENTITY_TYPE_NAME,
                EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), false, null, "List", false, null);
    }

    /**
     * シンプル型の配列スキーマを削除する.
     */
    public static void deleteSimpleArraySchema() {
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(UrlUtils.property(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "etListPropStr", ENTITY_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.property(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "etListPropInt", ENTITY_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.property(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "etListPropSingle", ENTITY_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.property(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "etListPropBoolean", ENTITY_TYPE_NAME));
        deleteComplexTypeSchema();
    }

    /**
     * Complex型の配列スキーマを作成する.
     */
    public static void createComplexArraySchema() {
        UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

        // ComplexTypeの配列Property作成
        UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ListComplexType");

        // Property作成
        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "listComplexType", UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                "ListComplexType", false, null, "List", false, null);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "lctStr", "ListComplexType",
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);
    }

    /**
     * Complex型の配列スキーマを削除する.
     */
    public static void deleteComplexArraySchema() {
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "lctStr", "ListComplexType"));
        ODataCommon.deleteOdataResource(UrlUtils.property(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "listComplexType",
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.complexType(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ListComplexType"));
        UserDataComplexTypeUtils.deleteComplexTypeSchema();
    }

    /**
     * Complex内のシンプル型の配列スキーマを削除する.
     */
    public static void createSimpleArraySchemaInComplex() {
        UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

        // 配列Property作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ctListPropStr", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, "List");

        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ctListPropInt", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                EdmSimpleType.INT32.getFullyQualifiedTypeName(), false, null, "List");

        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ctListPropSingle", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), false, null, "List");

        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ctListPropBoolean", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), false, null, "List");
    }

    /**
     * Complex内のシンプル型の配列スキーマを削除する.
     */
    public static void deleteSimpleArraySchemaInComplex() {
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ctListPropStr",
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ctListPropInt",
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ctListPropSingle",
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ctListPropBoolean",
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
        UserDataComplexTypeUtils.deleteComplexTypeSchema();
    }

    /**
     * Complex内のComplex型の配列スキーマを削除する.
     */
    public static void createComplexArraySchemaInComplex() {
        UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

        // ComplexTypeの配列Property作成
        UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "ListComplexType");

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "listComplexType", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                "ListComplexType", false, null, "List");

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "lctStr", "ListComplexType",
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);
    }

    /**
     * Complex内のComplex型の配列スキーマを削除する.
     */
    public static void deleteComplexArraySchemaInComplex() {
        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "lctStr", "ListComplexType"));
        ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "listComplexType",
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
        ODataCommon.deleteOdataResource(UrlUtils.complexType(
                Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ListComplexType"));
        UserDataComplexTypeUtils.deleteComplexTypeSchema();
    }

}

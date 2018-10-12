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
package io.personium.test.jersey.box.odatacol;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.box.odatacol.schema.complextype.ComplexTypeUtils;
import io.personium.test.jersey.box.odatacol.schema.complextypeproperty.ComplexTypePropertyUtils;
import io.personium.test.jersey.box.odatacol.schema.property.PropertyUtils;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * UserODataのDouble型静的プロパティNP経由操作のテストクラス.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataWithNPDeclaredDoublePropertyTest extends ODataCommon {

    /** テストセル. */
    public static final String CELL_NAME = "userdatawithnpdoubletest";
    /** テストボックス. */
    public static final String BOX_NAME = "box";
    /** テストコレクション. */
    public static final String COL_NAME = "odata";
    /** テストエンティティタイプ1. */
    public static final String ENTITYTYPE1 = "doubleTestEntity1";
    /** テストエンティティタイプ2. */
    public static final String ENTITYTYPE2 = "doubleTestEntity2";
    /** テストアソシエーションエンド1. */
    public static final String ASSOCIATIONEND1 = "associationEnd1";
    /** テストアソシエーションエンド2. */
    public static final String ASSOCIATIONEND2 = "associationEnd2";
    /** テストプロパティ(Simple型). */
    public static final String PROPERTY_SIMPLE = "propertySimple";
    /** テストプロパティ(Complex型). */
    public static final String PROPERTY_COMPLEX = "propertyComplex";
    /** テストコンプレックスタイプ. */
    public static final String COMPLEXTYPE = "complex";
    /** テストコンプレックスタイププロパティ. */
    public static final String COMPLEXTYPE_PROPERTY = "complexTypeProperty";

    /**
     * コンストラクタ.
     */
    public UserDataWithNPDeclaredDoublePropertyTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void ZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "0..1";
            String targetMultiplicity = "0..1";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * ZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void ZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "0..1";
            String targetMultiplicity = "1";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * ZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void ZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "0..1";
            String targetMultiplicity = "*";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "1";
            String targetMultiplicity = "0..1";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "1";
            String targetMultiplicity = "*";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * AST対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void AST対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "*";
            String targetMultiplicity = "0..1";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * AST対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void AST対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "*";
            String targetMultiplicity = "1";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    /**
     * AST対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     * @throws ParseException パース例外
     */
    @Test
    public final void AST対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() throws ParseException {
        try {
            String sourceMultiplicity = "*";
            String targetMultiplicity = "*";
            checkSchemaTypeDouble(sourceMultiplicity, targetMultiplicity);
        } finally {
            CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL_NAME);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkSchemaTypeDouble(String sourceMultiplicity, String targetMultiplicity) throws ParseException {
        // ベースとなるテストデータの作成
        createTestData(sourceMultiplicity, targetMultiplicity);

        // NP経由のUserData登録(スキーマ変更前)
        String targetUserDataBody1 = String.format("{\"__id\":\"%s\",\"%s\":%d,\"%s\":{\"%s\":%d}}",
                "id001", PROPERTY_SIMPLE, 12345, PROPERTY_COMPLEX, COMPLEXTYPE_PROPERTY, 56789);
        createUserDataViaNP("id1", targetUserDataBody1);

        // Propertyの型をInt32からDoubleに変更
        PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, CELL_NAME, BOX_NAME, COL_NAME, PROPERTY_SIMPLE,
                ENTITYTYPE2, PROPERTY_SIMPLE, ENTITYTYPE2,
                EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null);
        // ComplexTypePropertyの型をInt32からDoubleに変更
        ComplexTypePropertyUtils.update(CELL_NAME, BOX_NAME, COL_NAME, COMPLEXTYPE_PROPERTY, COMPLEXTYPE,
                COMPLEXTYPE_PROPERTY, COMPLEXTYPE, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(),
                true, null, "None");

        // NP経由のUserData登録(スキーマ変更後)
        String targetUserDataBody2 = String.format("{\"__id\":\"%s\",\"%s\":%f,\"%s\":{\"%s\":%f}}",
                "id002", PROPERTY_SIMPLE, 12345.12345, PROPERTY_COMPLEX, COMPLEXTYPE_PROPERTY, 56789.56789);
        createUserDataViaNP("id2", targetUserDataBody2);

        // NP経由のUserData一覧取得(スキーマ変更前に登録したUserData)
        TResponse response = UserDataUtils.listViaNP(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE1,
                "id1", ENTITYTYPE2, "", HttpStatus.SC_OK);
        String nameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + ENTITYTYPE2;
        JSONObject expectedJson = (JSONObject) new JSONParser().parse(targetUserDataBody1);
        Map<String, Map<String, Object>> idList = new HashMap<String, Map<String, Object>>();
        idList.put("id001", expectedJson);
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("id001", null);
        ODataCommon.checkResponseBodyList(
                response.bodyAsJson(), uri, nameSpace, idList, "__id", null, null);

        // NP経由のUserData一覧取得(スキーマ変更後に登録したUserData)
        response = UserDataUtils.listViaNP(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE1, "id2", ENTITYTYPE2,
                "", HttpStatus.SC_OK);
        nameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + ENTITYTYPE2;
        expectedJson = (JSONObject) new JSONParser().parse(targetUserDataBody2);
        idList = new HashMap<String, Map<String, Object>>();
        idList.put("id002", expectedJson);
        uri = new HashMap<String, String>();
        uri.put("id002", null);
        ODataCommon.checkResponseBodyList(
                response.bodyAsJson(), uri, nameSpace, idList, "__id", null, null);
    }

    private void createTestData(String sourceMultiplicity, String targetMultiplicity) throws ParseException {

        // 事前にデータを登録する
        CellUtils.create(CELL_NAME, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL_NAME, BOX_NAME, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(
                Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, COL_NAME);
        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN_NAME, BOX_NAME, COL_NAME, ENTITYTYPE1, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(
                CELL_NAME, MASTER_TOKEN_NAME, BOX_NAME, COL_NAME, ENTITYTYPE2, HttpStatus.SC_CREATED);
        AssociationEndUtils.create(
                MASTER_TOKEN_NAME, sourceMultiplicity, CELL_NAME, BOX_NAME, COL_NAME,
                HttpStatus.SC_CREATED, ASSOCIATIONEND1, ENTITYTYPE1);
        AssociationEndUtils.createViaNP(
                MASTER_TOKEN_NAME, CELL_NAME, BOX_NAME, COL_NAME,
                ASSOCIATIONEND1, ENTITYTYPE1, ASSOCIATIONEND2, targetMultiplicity, ENTITYTYPE2, HttpStatus.SC_CREATED);
        ComplexTypeUtils.create(CELL_NAME, BOX_NAME, COL_NAME, COMPLEXTYPE, HttpStatus.SC_CREATED);
        ComplexTypePropertyUtils.create(CELL_NAME, BOX_NAME, COL_NAME, COMPLEXTYPE_PROPERTY,
                COMPLEXTYPE, EdmSimpleType.INT32.getFullyQualifiedTypeName(), HttpStatus.SC_CREATED);
        PropertyUtils.create(BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE2,
                PROPERTY_SIMPLE, EdmSimpleType.INT32.getFullyQualifiedTypeName(),
                true, null, "None", false, null, HttpStatus.SC_CREATED);
        PropertyUtils.create(BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE2,
                PROPERTY_COMPLEX, COMPLEXTYPE, true, null, "None", false, null, HttpStatus.SC_CREATED);
    }

    private void createUserDataViaNP(String srcUserDataId, String targetUserDataBody)
            throws ParseException {
        UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                "{\"__id\":\"" + srcUserDataId + "\"}", CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE1);
        JSONParser parser = new JSONParser();
        UserDataUtils.createViaNP(MASTER_TOKEN_NAME, (JSONObject) parser.parse(targetUserDataBody), CELL_NAME,
                BOX_NAME,
                COL_NAME, ENTITYTYPE1, srcUserDataId, ENTITYTYPE2, HttpStatus.SC_CREATED);
    }
}

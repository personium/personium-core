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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DecimalFormat;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DaoException;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.box.odatacol.schema.property.PropertyUtils;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * UserODataのDouble型静的プロパティ登録のテストクラス.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataDeclardDoublePropertyTest extends AbstractUserDataTest {

    /** テストセル. */
    public static final String CELL_NAME = Setup.TEST_CELL1;
    /** テストボックス. */
    public static final String BOX_NAME = Setup.TEST_BOX1;
    /** テストコレクション. */
    public static final String COL_NAME = Setup.TEST_ODATA;
    /** テストエンティティタイプ. */
    public static final String ENTITYTYPE = "doubleTestEntity";

    /**
     * コンストラクタ.
     */
    public UserDataDeclardDoublePropertyTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * 数値形式のデータを登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 数値形式のデータを登録できること() {
        TResponse createResponse;
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        JSONObject createdEntity;
        JSONObject getEntity;
        JSONObject listEntity;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Int32型で登録・取得可能なこと
            String userDataId = "doubleTest1";
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("doubleProperty", Integer.MAX_VALUE);
            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            createdEntity = getResult(createResponse.bodyAsJson());
            assertEquals(userDataId, createdEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(createdEntity.get("doubleProperty")));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(listEntity.get("doubleProperty")));

            // Double型で登録・取得可能なこと
            userDataId = "doubleTest2";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":1234567890.12345d}", userDataId);
            createResponse = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(createResponse.getBody().contains(":1234567890.12345"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1234567890.12345"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1234567890.12345"));

            // Single型で登録・取得可能なこと
            userDataId = "doubleTest3";
            body.put("__id", userDataId);
            body.put("doubleProperty", 12345.54321d);
            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            createdEntity = getResult(createResponse.bodyAsJson());
            assertEquals(userDataId, createdEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(createdEntity.get("doubleProperty")));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(listEntity.get("doubleProperty")));

            // Long型で登録・取得可能なこと
            userDataId = "doubleTest4";
            body.put("__id", userDataId);
            body.put("doubleProperty", Long.MAX_VALUE);

            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            assertTrue(createResponse.getBody().contains(expectedString));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(expectedString));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(expectedString));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteUserOData("doubleTest2");
            deleteUserOData("doubleTest3");
            deleteUserOData("doubleTest4");
            deleteEntityType();
        }
    }

    /**
     * 小数値データ登録後に整数値データが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値データ登録後に整数値データが登録できること() {
        TResponse createResponse;
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        JSONObject createdEntity;
        JSONObject getEntity;
        JSONObject listEntity;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Double型で登録・取得可能なこと
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":1234567890.12345d}", userDataId);
            createResponse = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(createResponse.getBody().contains(":1234567890.12345"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1234567890.12345"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1234567890.12345"));

            // Int32型で登録・取得可能なこと
            JSONObject body = new JSONObject();
            userDataId = "doubleTest2";
            body.put("__id", userDataId);
            body.put("doubleProperty", Integer.MAX_VALUE);
            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            createdEntity = getResult(createResponse.bodyAsJson());
            assertEquals(userDataId, createdEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(createdEntity.get("doubleProperty")));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(listEntity.get("doubleProperty")));

        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteUserOData("doubleTest2");
            deleteEntityType();
        }
    }

    /**
     * Double型にNullが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型にNullが登録できること() {
        TResponse createResponse;
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        JSONObject createdEntity;
        JSONObject getEntity;
        JSONObject listEntity;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":1234567890.12345d}", userDataId);
            createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);

            // Double型にNullを登録・取得可能なこと
            JSONObject body = new JSONObject();
            userDataId = "doubleTest2";
            body.put("__id", userDataId);
            body.put("doubleProperty", null);
            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            createdEntity = getResult(createResponse.bodyAsJson());
            assertEquals(userDataId, createdEntity.get("__id"));
            assertEquals(null, createdEntity.get("doubleProperty"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(null, getEntity.get("doubleProperty"));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(null, listEntity.get("doubleProperty"));

        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteUserOData("doubleTest2");
            deleteEntityType();
        }
    }

    /**
     * Double型に文字列のデータが登録できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型に文字列のデータが登録できないこと() {
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":1234567890.12345d}", userDataId);
            createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);

            // Double型に文字列データが登録できないこと
            JSONObject body = new JSONObject();
            userDataId = "doubleTest2";
            body.put("__id", userDataId);
            body.put("doubleProperty", "test");
            createUserData(userDataId, body, HttpStatus.SC_BAD_REQUEST);

            getUserOData(userDataId, HttpStatus.SC_NOT_FOUND);
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteUserOData("doubleTest2");
            deleteEntityType();
        }
    }

    /**
     * Double型に文字列型の数値のデータが登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型に文字列型の数値のデータが登録できること() {
        TResponse createResponse;
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":1234567890.12345d}", userDataId);
            createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);

            // Double型に文字列データが登録できること
            JSONObject body = new JSONObject();
            userDataId = "doubleTest2";
            body.put("__id", userDataId);
            body.put("doubleProperty", "1234567890.12345d");
            createResponse = createUserData(userDataId, body, HttpStatus.SC_CREATED);
            assertTrue(createResponse.getBody().contains(":1234567890.12345"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1234567890.12345"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1234567890.12345"));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteUserOData("doubleTest2");
            deleteEntityType();
        }
    }

    /**
     * Double型に最小値より小さい値のデータが登録できないこと.
     */
    @Test
    public final void Double型に最小値より小さい値のデータが登録できないこと() {
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // Double型で登録できないこと
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"doubleProperty\":-1.791e308}", userDataId);
            createUserData(userDataId, doubleRequestBody, HttpStatus.SC_BAD_REQUEST);
            getUserData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId,
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);
        } finally {
            // テストデータ削除
            deleteEntityType();
        }
    }

    /**
     * Double型のデフォルト値がデータ登録時に反映されてデータが登録できること.
     */
    @Test
    public final void Double型のデフォルト値がデータ登録時に反映されてデータが登録できること() {
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        try {
            // EntityType登録
            EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
            String propName = "doubleProperty";

            // Property登録
            // リクエストパラメータ設定
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                    propName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, "12345.123456789", "None", false,
                    null,
                    HttpStatus.SC_CREATED);

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\"}", userDataId);
            TResponse res = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(res.getBody().contains(":12345.123456789"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":12345.123456789"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":12345.123456789"));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteEntityType();
        }
    }

    /**
     * Double型のデフォルト値が最大値の場合にデフォルト値を反映してデータが登録できること.
     */
    @Test
    public final void Double型のデフォルト値が最大値の場合にデフォルト値を反映してデータが登録できること() {
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        try {
            // EntityType登録
            EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
            String propName = "doubleProperty";

            // Property登録
            // リクエストパラメータ設定
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                    propName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, "1.79e308", "None", false, null,
                    HttpStatus.SC_CREATED);

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\"}", userDataId);
            TResponse res = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(res.getBody().contains(":1.79E308"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1.79E308"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1.79E308"));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteEntityType();
        }
    }

    /**
     * Double型配列のデータが登録できること.
     */
    @Test
    public final void Double型配列のデータが登録できること() {
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        try {
            // EntityType登録
            EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
            String propName = "doubleProperty";

            // Property登録
            // リクエストパラメータ設定
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                    propName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "List", false, null,
                    HttpStatus.SC_CREATED);

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\", \"%s\":[1.23456789, 12345.6789, 0]}", userDataId, propName);
            TResponse res = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(res.getBody().contains(":[1.23456789,12345.6789,0]"));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":[1.23456789,12345.6789,0]"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":[1.23456789,12345.6789,0]"));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteEntityType();
        }
    }

    /**
     * Double型配列のデータに最大制限値より大きい値が含まれている場合に登録できないこと.
     */
    @Test
    public final void Double型配列のデータに最大制限値より大きい値が含まれている場合に登録できないこと() {
        try {
            // EntityType登録
            EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
            String propName = "doubleProperty";

            // Property登録
            // リクエストパラメータ設定
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                    propName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "List", false, null,
                    HttpStatus.SC_CREATED);

            // Double型で登録
            String userDataId = "doubleTest1";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\", \"%s\":[1.23456789, 12345.6789, 1.791E308, 0]}", userDataId,
                            propName);
            createUserData(userDataId, doubleRequestBody, HttpStatus.SC_BAD_REQUEST);

            getUserOData(userDataId, HttpStatus.SC_NOT_FOUND);
        } finally {
            // テストデータ削除
            deleteEntityType();
        }
    }

    /**
     * 数値形式のデータを更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 数値形式のデータを更新できること() {
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        JSONObject getEntity;
        JSONObject listEntity;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            String userDataId = "doubleTest1";
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("doubleProperty", "123.456");
            createUserData(userDataId, body, HttpStatus.SC_CREATED);

            // Int32型の最大値で更新・取得可能なこと
            JSONObject updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", Integer.MAX_VALUE);
            updateUserData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, updateBody1);

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(listEntity.get("doubleProperty")));

            // Double型で更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", 1234567890.12345d);
            updateUserData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, updateBody1);

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1234567890.12345"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1234567890.12345"));

            // Single型に対応した値で更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", 12345.54321d);
            updateUserData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, updateBody1);

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(listEntity.get("doubleProperty")));

            // Long型の最大値で更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", Long.MAX_VALUE);
            updateUserData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, updateBody1);

            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(expectedString));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(expectedString));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteEntityType();
        }
    }

    /**
     * 数値形式のデータを部分更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 数値形式のデータを部分更新できること() {
        PersoniumResponse getResponse;
        PersoniumResponse listResponse;
        JSONObject getEntity;
        JSONObject listEntity;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            String userDataId = "doubleTest1";
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("doubleProperty", "123.456");
            createUserData(userDataId, body, HttpStatus.SC_CREATED);

            // Int32型の最大値で部分更新・取得可能なこと
            JSONObject updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", Integer.MAX_VALUE);
            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, updateBody1,
                    CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, "*");

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(Integer.MAX_VALUE), String.valueOf(listEntity.get("doubleProperty")));

            // Double型で部分更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", 1234567890.12345d);
            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, updateBody1,
                    CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, "*");

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":1234567890.12345"));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":1234567890.12345"));

            // Single型に対応した値で部分更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", 12345.54321d);
            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, updateBody1,
                    CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, "*");

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            getEntity = getResult(getResponse.bodyAsJson());
            assertEquals(userDataId, getEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(getEntity.get("doubleProperty")));

            listResponse = getUserODataList();
            listEntity = getResultsFromId(userDataId, listResponse.bodyAsJson());
            assertEquals(userDataId, listEntity.get("__id"));
            assertEquals(String.valueOf(12345.54321d), String.valueOf(listEntity.get("doubleProperty")));

            // Long型の最大値で部分更新・取得可能なこと
            updateBody1 = new JSONObject();
            updateBody1.put("doubleProperty", Long.MAX_VALUE);
            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, updateBody1,
                    CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId, "*");

            double expected = Double.parseDouble(String.valueOf(Long.MAX_VALUE));
            DecimalFormat format = new DecimalFormat("#.#");
            format.setMaximumIntegerDigits(15);
            format.setMaximumFractionDigits(14);
            String fomattedValue = format.format(expected);
            String expectedString;
            if (expected != Double.parseDouble(fomattedValue)) {
                expectedString = Double.toString(expected);
            } else {
                expectedString = fomattedValue;
            }

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(expectedString));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(expectedString));
        } catch (DaoException e) {
            fail(e.getMessage());
        } finally {
            // テストデータ削除
            deleteUserOData("doubleTest1");
            deleteEntityType();
        }
    }

    private void createEntityType() {
        // EntityType登録
        EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
        String propName = "doubleProperty";

        // Property登録
        // リクエストパラメータ設定
        PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                propName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                HttpStatus.SC_CREATED);

    }

    private TResponse createUserData(String userDataId, JSONObject body, int code) {
        return UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, code,
                body, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE);
    }

    private TResponse createUserData(String userDataId, String body, int code) {
        return UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, code,
                body, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE);
    }

    private PersoniumResponse getUserOData(String userDataId, int code) {
        PersoniumResponse response = ODataCommon.getOdataResource(
                UrlUtils.userdata(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId));
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    private PersoniumResponse getUserODataList() {
        PersoniumResponse response =
                ODataCommon.getOdataResource(UrlUtils.userData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE));
        return response;
    }

    private void deleteEntityType() {
        PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE,
                "doubleProperty", -1);
        EntityTypeUtils.delete(COL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, ENTITYTYPE, BOX_NAME, CELL_NAME, -1);
    }

    private void deleteUserOData(String userDataId) {
        ODataCommon.deleteOdataResource(UrlUtils.userdata(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId));
    }
}

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

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DaoException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserODataのDouble型動的プロパティ登録のテストクラス.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataDynamicDoublePropertyTest extends AbstractUserDataTest {

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
    public UserDataDynamicDoublePropertyTest() {
        super();
    }

    /**
     * 数値形式のデータを登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 数値形式のデータを登録できること() {
        TResponse createResponse;
        DcResponse getResponse;
        DcResponse listResponse;
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
        DcResponse getResponse;
        DcResponse listResponse;
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
        DcResponse getResponse;
        DcResponse listResponse;
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
     * 文字列型にDouble型のデータが文字列として登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 文字列型にDouble型のデータが文字列として登録できること() {
        TResponse createResponse;
        DcResponse getResponse;
        DcResponse listResponse;
        try {
            // テスト用エンティティタイプ作成
            createEntityType();

            // 文字列型で登録
            String userDataId = "doubleTest1";
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("stringProperty", "1234567890.12345d");
            createUserData(userDataId, body, HttpStatus.SC_CREATED);

            // Double型の値が文字列として登録・取得可能なこと
            userDataId = "doubleTest2";
            String doubleRequestBody =
                    String.format("{\"__id\":\"%s\",\"stringProperty\":1234567890.12345d}", userDataId);
            createResponse = createUserData(userDataId, doubleRequestBody, HttpStatus.SC_CREATED);
            assertTrue(createResponse.getBody().contains(":\"1234567890.12345\""));

            getResponse = getUserOData(userDataId, HttpStatus.SC_OK);
            assertTrue(getResponse.bodyAsString().contains(":\"1234567890.12345\""));

            listResponse = getUserODataList();
            assertTrue(listResponse.bodyAsString().contains(":\"1234567890.12345\""));
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
        DcResponse getResponse;
        DcResponse listResponse;
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

    private void createEntityType() {
        EntityTypeUtils.create(CELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                BOX_NAME, COL_NAME, ENTITYTYPE, HttpStatus.SC_CREATED);
    }

    private TResponse createUserData(String userDataId, JSONObject body, int code) {
        return UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, code,
                body, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE);
    }

    private TResponse createUserData(String userDataId, String body, int code) {
        return UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, code,
                body, CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE);
    }

    private DcResponse getUserOData(String userDataId, int code) {
        DcResponse response = ODataCommon.getOdataResource(
                UrlUtils.userdata(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId));
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    private DcResponse getUserODataList() {
        DcResponse response =
                ODataCommon.getOdataResource(UrlUtils.userData(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE));
        return response;
    }

    private void deleteEntityType() {
        EntityTypeUtils.delete(COL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, ENTITYTYPE, BOX_NAME, CELL_NAME, -1);
    }

    private void deleteUserOData(String userDataId) {
        ODataCommon.deleteOdataResource(UrlUtils.userdata(CELL_NAME, BOX_NAME, COL_NAME, ENTITYTYPE, userDataId));
    }
}

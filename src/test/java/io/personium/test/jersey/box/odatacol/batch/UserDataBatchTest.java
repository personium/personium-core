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
package io.personium.test.jersey.box.odatacol.batch;

import static io.personium.test.utils.BatchUtils.BOUNDARY;
import static io.personium.test.utils.BatchUtils.END_BOUNDARY;
import static io.personium.test.utils.BatchUtils.START_BOUNDARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.producer.resources.ODataBatchProvider;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.CompareJSON;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.box.odatacol.schema.property.PropertyUtils;
import io.personium.test.setup.Setup;
import io.personium.test.utils.BatchUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * UserData$batchのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * $batchの登録でID指定なしのデータを指定した場合に201返却されること.
     */
    @Test
    public final void $batchの登録でID指定なしのデータを指定した場合に201返却されること() {
        try {
            String body = START_BOUNDARY + BatchUtils.retrievePostBodyNoId("Supplier", HttpMethod.POST)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", ".*") + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            String listResponse = getUserDataList(cellName, boxName, colName, "Supplier").getBody()
                    .replaceAll("\n", "");
            Pattern pattern = Pattern.compile(".*__id\":\"([^\"]+).*");
            Matcher matcher = pattern.matcher(listResponse);
            matcher.find();
            String id = matcher.group(1);
            deleteUserData(cellName, boxName, colName, "Supplier", id, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);

        }

    }

    /**
     * $batchの登録で登録されたダイナミックプロパティにnullを指定して登録した場合に正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの登録で登録されたダイナミックプロパティにnullを指定して登録した場合に正常に登録できること() {
        String entityTypeName = "srcEntity";
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

            // $batchリクエストボディ作成
            JSONObject bodyFirst = new JSONObject();
            bodyFirst.put("__id", "first");
            bodyFirst.put("dynamicProperty", null);
            bodyFirst.put("First", "First");

            JSONObject bodySecond = new JSONObject();
            bodySecond.put("__id", "second");
            bodySecond.put("dynamicProperty", null);
            bodySecond.put("Second", "Second");

            String body = START_BOUNDARY + BatchUtils.retrievePostWithBody(entityTypeName, bodyFirst)
                    + START_BOUNDARY + BatchUtils.retrievePostWithBody(entityTypeName, bodySecond)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // ユーザデータの取得
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    "first", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            JSONObject resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertTrue(resBody.containsKey("First"));
            assertNotNull(resBody.get("First"));

            // ユーザデータの取得
            response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    "second", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // レスポンスボディーのチェック
            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertFalse(resBody.containsKey("First"));
            assertTrue(resBody.containsKey("Second"));
            assertNotNull(resBody.get("Second"));

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    "first", Setup.TEST_ODATA);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    "second", Setup.TEST_ODATA);
            Setup.entityTypeDelete(Setup.TEST_ODATA, entityTypeName, Setup.TEST_CELL1, Setup.TEST_BOX1);
        }
    }

    /**
     * $batchで登録直後に登録データの参照を行いレスポンスが返却されること.
     */
    @Test
    public final void $batchで登録直後に登録データの参照を行いレスポンスが返却されること() {
        try {
            String body = START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "refresh")
                    + START_BOUNDARY + BatchUtils.retrieveGetBody("Supplier('refresh')")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "refresh")
                    + START_BOUNDARY + BatchUtils.retrieveGetResBody("Supplier", "refresh") + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Supplier", "refresh", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchの更新でpublishedが更新されていないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの更新でpublishedが更新されていないこと() {
        // リクエストボディを設定
        String userDataId = "pubTest";
        JSONObject createBody = new JSONObject();
        createBody.put("__id", userDataId);
        try {
            // ユーザデータ登録から__publishedを取得する
            TResponse response = createUserData(createBody, HttpStatus.SC_CREATED);
            String expPublished = ODataCommon.getPublished(response);

            // $batchのリクエスト(ユーザデータを更新する)
            String body = START_BOUNDARY + BatchUtils.retrievePutBody("Category('pubTest')")
                    + END_BOUNDARY;
            TResponse bulkResponse = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // ユーザデータを取得し、レスポンスボディから__publishedを取得する
            response = getUserData(cellName, boxName, colName, "Category",
                    "pubTest", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            String published = ODataCommon.getPublished(response);

            // __publishedが更新されていないことを確認
            assertEquals(expPublished, published);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrievePutResBody() + END_BOUNDARY;
            checkBatchResponseBody(bulkResponse, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Category", "pubTest", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchで登録直後に取得、更新、削除を行いレスポンスが返却されること.
     */
    @Test
    public final void $batchで登録直後に取得更新削除を行いレスポンスが返却されること() {
        String body = START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrieveGetBody("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch2")
                + START_BOUNDARY + BatchUtils.retrievePutBody("Supplier('testBatch2')")
                + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch3")
                + START_BOUNDARY + BatchUtils.retrieveListBody("Supplier")
                + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch4")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch4')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch3')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch2')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')")
                + END_BOUNDARY;

        // リクエスト実行
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // 一覧取得で期待するIDのプレフィックスリスト
        List<String> listIds = new ArrayList<String>();
        listIds.add("testBatch");
        listIds.add("testBatch");
        listIds.add("testBatch");

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrieveGetResBody("Supplier", "testBatch1")
                + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch2")
                + START_BOUNDARY + BatchUtils.retrievePutResBody()
                + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch3")
                + START_BOUNDARY + BatchUtils.retrieveListSupplierResBody(listIds)
                + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch4")
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    /**
     * UserDataを$batch経由で正常に登録及び削除できること.
     */
    @Test
    public final void UserDataを$batch経由で正常に登録及び削除できること() {

        try {
            String body = START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch1") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch1")
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);

        } finally {
            String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプEdm_Doubleのデータを$batch経由で正常に登録及び削除できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプEdm_Doubleのデータを$batch経由で正常に登録及び削除できること() {

        String userOdataId = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataId);
            batchBody.put(propName, 1.23);
            String body = START_BOUNDARY + BatchUtils.retrievePostWithBody("Supplier", batchBody) + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataId)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);

            // ユーザOData一件取得
            res = UserDataUtils.get(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, colName, "Supplier",
                    userOdataId, HttpStatus.SC_OK);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userOdataId);
            additional.put(propName, 1.23);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            String nameSpace = getNameSpace("Supplier");
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, nameSpace, additional, null, etag);

            // ユーザOData一覧取得
            res = UserDataUtils.list(cellName, boxName, colName, "Supplier", "", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userOdataId, UrlUtils.userData(cellName, boxName, colName, "Supplier"
                    + "('" + userOdataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additionalMap = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additionalMap.put(userOdataId, additionalprop);
            additionalprop.put("__id", userOdataId);
            additionalprop.put(propName, 1.23);

            nameSpace = getNameSpace("Supplier");
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additionalMap, "__id", null, null);
        } finally {
            String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('" + userOdataId + "')")
                + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後にデータを$batch経由で正常に登録及び削除できることこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後にデータを$batch経由で正常に登録及び削除できること() {

        String userOdataIdInt = "batchInt";
        String userOdataIdDouble = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject body = new JSONObject();
            body.put("__id", userOdataIdInt);
            body.put(propName, 1);

            // ユーザデータの登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName,
                    colName, "Supplier");
            // プロパティの更新(Edm.Int32⇒Edm.double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propName, "Supplier",
                    propName, "Supplier", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false,
                    null);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataIdDouble);
            batchBody.put(propName, 1.23);
            String reqBody = START_BOUNDARY + BatchUtils.retrievePostWithBody("Supplier", batchBody) + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);

            // ユーザOData一覧取得
            res = UserDataUtils.list(cellName, boxName, colName, "Supplier", "", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userOdataIdInt, UrlUtils.userData(cellName, boxName, colName, "Supplier"
                    + "('" + userOdataIdInt + "')"));
            uri.put(userOdataIdDouble, UrlUtils.userData(cellName, boxName, colName, "Supplier"
                    + "('" + userOdataIdDouble + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additionalMap = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additionalMap.put(userOdataIdDouble, additionalprop);
            additionalprop.put("__id", userOdataIdInt);
            additionalprop.put(propName, 1);

            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additionalMap.put(userOdataIdDouble, additionalprop2);
            additionalprop2.put("__id", userOdataIdDouble);
            additionalprop2.put(propName, 1.23);

            String nameSpace = getNameSpace("Supplier");
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additionalMap, "__id", null, null);
        } finally {
            String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('" + userOdataIdInt + "')")
                + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('" + userOdataIdDouble + "')")
                + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * UserDataを$batch経由で正常に取得できること.
     */
    @Test
    public final void UserDataを$batch経由で正常に取得できること() {
        String body = START_BOUNDARY + BatchUtils.retrieveGetBody("SalesDetail('userdata005')") + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + BatchUtils.retrieveGetResBody("SalesDetail", "userdata005")
            + END_BOUNDARY;

        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * UserDataを$batch経由で正常に一覧取得できること.
     */
    @Test
    public final void UserDataを$batch経由で正常に一覧取得できること() {
        String entityTypeName = "testListEntity";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, PersoniumUnitConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String body = START_BOUNDARY + BatchUtils.retrievePostBody(entityTypeName, "testBatch1")
                    + START_BOUNDARY + BatchUtils.retrievePostBody(entityTypeName, "testBatch2")
                    + START_BOUNDARY + BatchUtils.retrieveListBody(entityTypeName)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add("testBatch");
            listRes1.add("testBatch");
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, "testBatch1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, "testBatch2")
                    + START_BOUNDARY + BatchUtils.retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, "testBatch1",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, "testBatch2",
                    PersoniumUnitConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);

        }
    }

    /**
     * $batchの取得で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeからデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの取得で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeからデータを取得できること() {

        try {
            // 事前準備
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");

            srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0002");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "SalesDetail");

            // $batch
            String body = START_BOUNDARY + BatchUtils.retrieveGetBody("Sales('id0001')")
                    + START_BOUNDARY + BatchUtils.retrieveGetBody("SalesDetail('id0001')")
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveGetResBody("Sales", "id0001")
                    + START_BOUNDARY + BatchUtils.retrieveGetResBody("SalesDetail", "id0001")
                    + END_BOUNDARY;

            checkBatchResponseBody(res, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0001",
                    PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchの削除で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeのデータを削除できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの削除で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeのデータを削除できること() {

        try {
            // 事前準備
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");

            srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0002");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "SalesDetail");

            // $batch
            String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("SalesDetail('id0001')")
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);

            // ユーザOData取得
            UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName, colName, "Sales",
                    "id0001", HttpStatus.SC_OK);
            UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName, colName, "SalesDetail",
                    "id0001", HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0001",
                    PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchの更新で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeのデータを更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの更新で異なるEntityTypeに対して同じ__idのデータが登録されている場合適切なEntityTypeのデータを更新できること() {

        try {
            // 事前準備
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");

            srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0002");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "SalesDetail");

            // 更新前ユーザOData取得
            TResponse originalSales = UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName,
                    colName, "Sales", "id0001", HttpStatus.SC_OK);
            TResponse originalSalesDetail = UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName,
                    colName, "SalesDetail", "id0001", HttpStatus.SC_OK);

            // $batch
            JSONObject batchBody = new JSONObject();
            batchBody.put("Name", "updated");
            String body = START_BOUNDARY + BatchUtils.retrievePutBody("SalesDetail('id0001')", batchBody)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrievePutResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);

            // ユーザOData取得(Salesが更新されていないこと)
            TResponse afterSales = UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName,
                    colName, "Sales", "id0001", HttpStatus.SC_OK);
            CompareJSON.Result compareRes = CompareJSON.compareJSON(
                    originalSales.bodyAsJson(), afterSales.bodyAsJson());
            assertNull(compareRes);

            // ユーザOData取得(SalesDetailが更新されていること)
            TResponse afterSalesDetail = UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName,
                    colName, "SalesDetail", "id0001", HttpStatus.SC_OK);
            compareRes = CompareJSON.compareJSON(
                    originalSalesDetail.bodyAsJson(), afterSalesDetail.bodyAsJson());
            assertNotNull(compareRes);
            assertEquals(1, compareRes.size());
            assertEquals("updated", compareRes.getMismatchValue("Name"));

        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0001",
                    PersoniumUnitConfig.getMasterToken(), -1);
        }
    }

    /**
     * UserDataを$batchに複数リクエスト指定で正常に処理ができること.
     */
    @Ignore
    @Test
    public final void UserDataを$batchに複数リクエスト指定で正常に処理ができること() {
        String body = START_BOUNDARY + BatchUtils.retrieveMultiRequestBody("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrieveGetBody("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch2")
                + START_BOUNDARY + BatchUtils.retrieveListBody("Supplier")
                + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier('testBatch2')/_Product", "id0001")
                + START_BOUNDARY + BatchUtils.retrieveGetBody("Product('id0001')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Product('id0001')")
                + START_BOUNDARY + BatchUtils.retrievePutBody("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrieveGetBody("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch2')")
                + START_BOUNDARY + BatchUtils.retrieveListBody("Supplier")
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        List<String> listRes1 = new ArrayList<String>();
        listRes1.add("testBatch");
        listRes1.add("testBatch");
        List<String> listRes2 = new ArrayList<String>();

        String id = "testBatch1";
        String uri = UrlUtils
                .userData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Supplier" + "\\('" + id + "'\\)");
        String expectedBody = START_BOUNDARY + BatchUtils.retrieveMultiRequestResBody(id, uri)
                + START_BOUNDARY + BatchUtils.retrieveGetResBody("Supplier", "testBatch1")
                + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch2")
                + START_BOUNDARY + BatchUtils.retrieveListSupplierResBody(listRes1)
                + START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                + START_BOUNDARY + BatchUtils.retrieveGetResBody("Product", "id0001")
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrievePutResBody()
                + START_BOUNDARY + BatchUtils.retrieveGetResBody("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + START_BOUNDARY + BatchUtils.retrieveListSupplierResBody(listRes2)
                + END_BOUNDARY;

        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * $batchの登録で不正フォーマットのデータを指定した場合に400が返却されること.
     */
    @Test
    public final void $batchの登録で不正フォーマットのデータを指定した場合に400が返却されること() {
        String body = START_BOUNDARY;
        String code = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getMessage();
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);

    }

    /**
     * $batchの登録で不正JSONフォーマットのデータを指定した場合に400が返却されること.
     */
    @Test
    public final void $batchの登録で不正JSONフォーマットのデータを指定した場合に400が返却されること() {
        String body = START_BOUNDARY + BatchUtils.retrievePostBodyJsonFormatError("Supplier", "testBatch")
                + END_BOUNDARY;
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    /**
     * $batchの登録でバウンダリのContentTypeを指定しない場合に400が返却されること.
     */
    @Test
    public final void $batchの登録でバウンダリのContentTypeを指定しない場合に400が返却されること() {
        String contentType = "";
        String body = START_BOUNDARY
                + BatchUtils.retrievePostBodyBoundaryHeaderError("Supplier", "testBatch", contentType)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(
                        HttpHeaders.CONTENT_TYPE).getMessage());
    }

    /**
     * $batchの登録でリクエストヘッダのContentTypeに誤ったバウンダリーを指定した場合に400が返却されること.
     */
    @Test
    public final void $batchの登録でリクエストヘッダのContentTypeに誤ったバウンダリーを指定した場合に400が返却されること() {
        String code = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getMessage();
        String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')") + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", "changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();

    }

    /**
     * $batchの登録でリクエストボディの末尾にバウンダリーがない場合に400が返却されること.
     */
    @Test
    public final void $batchの登録でリクエストボディの末尾にバウンダリーがない場合に400が返却されること() {
        String code = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_PARSE_ERROR.getMessage();
        String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')");
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", "changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchの登録でContentTypeにmultipartを指定しバウンダリを指定しない場合に400が返却されること.
     */
    @Test
    public final void $batchの登録でContentTypeにmultipartを指定しバウンダリを指定しない場合に400が返却されること() {
        String contentType = "Content-Type: multipart/mixed;\n";
        String body = START_BOUNDARY
                + BatchUtils.retrievePostBodyBoundaryHeaderError("Supplier", "testBatch", contentType)
                + END_BOUNDARY;

        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(
                        HttpHeaders.CONTENT_TYPE).getMessage());
    }

    /**
     * $batchの登録でバウンダリのContentTypeに許可しない文字列を指定した場合400が返却されること.
     */
    @Test
    public final void $batchの登録でバウンダリのContentTypeに許可しない文字列を指定した場合400が返却されること() {
        String contentType = "Content-Type: text/html;\n";
        String body = START_BOUNDARY
                + BatchUtils.retrievePostBodyBoundaryHeaderError("Supplier", "testBatch", contentType)
                + END_BOUNDARY;

        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(
                        HttpHeaders.CONTENT_TYPE).getMessage());
    }

    /**
     * $batchの登録でchangesetのContentTypeを指定しない場合に400が返却されること.
     */
    @Test
    public final void $batchの登録でchangesetのContentTypeを指定しない場合に400が返却されること() {
        String contentType = "";
        String body = START_BOUNDARY
                + BatchUtils.retrievePostBodyChangesetHeaderError("Supplier", "testBatch", contentType)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(
                        HttpHeaders.CONTENT_TYPE).getMessage());
    }

    /**
     * $batchの登録でchangesetのContentTypeに許可しない文字列を指定した場合400が返却されること.
     */
    @Test
    public final void $batchの登録でchangesetのContentTypeに許可しない文字列を指定した場合400が返却されること() {
        String contentType = "Content-Type: text/html;\n";
        String body = START_BOUNDARY
                + BatchUtils.retrievePostBodyChangesetHeaderError("Supplier", "testBatch", contentType)
                + END_BOUNDARY;

        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res,
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params(
                        HttpHeaders.CONTENT_TYPE).getMessage());
    }

    /**
     * $batchの登録でchangesetをネストで指定した場合400が返却されること.
     */
    @Test
    public final void $batchの登録でchangesetをネストで指定した場合400が返却されること() {
        String body = START_BOUNDARY
                + BatchUtils.retrieveNestChangesetBody("Supplier", "testBatch1")
                + END_BOUNDARY;

        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(res,
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR.getCode(),
                PersoniumCoreException.OData.BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR.getMessage());
    }

    /**
     * $batchの更新でリクエストボディに管理情報__publishedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの更新でリクエストボディに管理情報__publishedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "pubTest";
        JSONObject createBody = new JSONObject();
        createBody.put("__id", userDataId);
        try {
            createUserData(createBody, HttpStatus.SC_CREATED);

            String body = START_BOUNDARY
                    + BatchUtils.retrievePutBodyFieledInvalidError("Category('pubTest')", PUBLISHED)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Category", "pubTest", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchの更新でリクエストボディに管理情報__updatedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの更新でリクエストボディに管理情報__updatedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "pubTest";
        JSONObject createBody = new JSONObject();
        createBody.put("__id", userDataId);
        try {
            createUserData(createBody, HttpStatus.SC_CREATED);

            String body = START_BOUNDARY + BatchUtils.retrievePutBodyFieledInvalidError("Category('pubTest')", UPDATED)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Category", "pubTest", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchの更新でリクエストボディに管理情報__metadataを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの更新でリクエストボディに管理情報__metadataを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "pubTest";
        JSONObject createBody = new JSONObject();
        createBody.put("__id", userDataId);
        try {
            createUserData(createBody, HttpStatus.SC_CREATED);

            String body = START_BOUNDARY + BatchUtils.retrievePutBodyMetadataFieledInvalidError("Category('pubTest')")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Category", "pubTest", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchで整数データ値を含むデータを登録直後に文字列値で更新を行い400エラーが返却されること.
     */
    @Test
    public final void $batchで整数データ値を含むデータを登録直後に文字列値で更新を行い400エラーが返却されること() {
        String body = START_BOUNDARY + BatchUtils.retrievePostBodyIntData("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrievePutBodyIntData("Supplier('testBatch1')")
                + START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch1')")
                + END_BOUNDARY;

        // リクエスト実行
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // 一覧取得で期待するIDのプレフィックスリスト
        List<String> listIds = new ArrayList<String>();
        listIds.add("testBatch");
        listIds.add("testBatch");
        listIds.add("testBatch");

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "testBatch1")
                + START_BOUNDARY + BatchUtils.retrievePutResBody400()
                + START_BOUNDARY + BatchUtils.retrieveDeleteResBody()
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    /**
     * $batchの登録で存在しないデータを指定した場合に404が返却されること.
     */
    @Test
    public final void $batchの登録で存在しないデータを指定した場合に404が返却されること() {
        String body = START_BOUNDARY + BatchUtils.retrievePostBody("notExistsType", "testBatch")
                + END_BOUNDARY;
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    /**
     * $batchの登録で不正なMethodを指定した場合に400が返却されること.
     */
    @Test
    public final void $batchの登録で不正なMethodを指定した場合に400が返却されること() {
        String code = PersoniumCoreException.OData.BATCH_BODY_FORMAT_METHOD_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_FORMAT_METHOD_ERROR.params("POT").getMessage();

        String body = START_BOUNDARY + BatchUtils.retrievePostBodyNoId("Supplier", "POT")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);
    }

    /**
     * $batchの登録でESに存在するデータを指定した場合に409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchの登録でESに存在するデータを指定した場合に409が返却されること() {
        String userDataId = "conflict";
        JSONObject preRequestBody = new JSONObject();
        preRequestBody.put("__id", userDataId);
        try {
            createUserData(preRequestBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Supplier");

            String body = START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "conflict")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Supplier", userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchの登録でリクエストデータ内に存在するデータを指定した場合に409が返却されること.
     */
    @Test
    public final void $batchの登録でリクエストデータ内に存在するデータを指定した場合に409が返却されること() {
        try {
            String body = START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "conflict")
                    + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "conflict")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "conflict")
                    + START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Supplier", "conflict", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * $batchの登録で異なるEntityTypeに対して同じ__idを指定して作成できること.
     */
    @Test
    public final void $batchの登録で異なるEntityTypeに対して同じ__idを指定して作成できること() {
        try {
            String body = START_BOUNDARY + BatchUtils.retrievePostBody("Sales", "id0001")
                    + START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "id0001")
                    + END_BOUNDARY;

            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "id0001")
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザOData取得
            UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, PersoniumUnitConfig.getMasterToken(), boxName, colName, "Supplier", "id0001",
                    HttpStatus.SC_OK);

        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0001", PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * _2回目の$batchで1回目と同じデータを指定したときに全て409が返却されること.
     */
    @Test
    public final void _2回目の$batchで1回目と同じデータを指定したときに全て409が返却されること() {
        final int registerNumber = 20;
        try {
            // 1回目のデータ登録(全て登録)
            TResponse response = requestBatchPost(0, registerNumber);
            // レスポンスボディのチェック
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < registerNumber; i++) {
                String id = String.format("testBatch%02d", i);
                sbuf.append(START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", id));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
            checkRetrieveUserData(0, registerNumber);
            // 2回目のデータ登録(全て409エラー)
            response = requestBatchPost(0, registerNumber);
            // レスポンスボディのチェック
            sbuf = new StringBuilder();
            for (int i = 0; i < registerNumber; i++) {
                sbuf.append(START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
        } finally {
            for (int i = 0; i < registerNumber; i++) {
                String id = String.format("testBatch%02d", i);
                String url = UrlUtils.userdata(cellName, boxName, colName, "Supplier", id);
                ODataCommon.deleteOdataResource(url);
            }
        }
    }

    /**
     * _2回目の$batchで1回目と異なるデータを指定したときに全て201が返却されること.
     */
    @Test
    public final void _2回目の$batchで1回目と異なるデータを指定したときに全て201が返却されること() {
        final int registerNumber = 20;
        try {
            // 1回目のデータ登録(全て登録)
            TResponse response = requestBatchPost(0, registerNumber);
            // レスポンスボディのチェック
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < registerNumber; i++) {
                String id = String.format("testBatch%02d", i);
                sbuf.append(START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", id));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
            checkRetrieveUserData(0, registerNumber);
            // 2回目のデータ登録(全て409エラー)
            response = requestBatchPost(registerNumber, registerNumber);
            // レスポンスボディのチェック
            sbuf = new StringBuilder();
            for (int i = registerNumber; i < registerNumber * 2; i++) {
                String id = String.format("testBatch%02d", i);
                sbuf.append(START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", id));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
            checkRetrieveUserData(registerNumber, registerNumber);
            for (int i = 0; i < registerNumber * 2; i++) {
                String id = String.format("testBatch%02d", i);
                String url = UrlUtils.userdata(cellName, boxName, colName, "Supplier", id);
                PersoniumResponse res = ODataCommon.getOdataResource(url);
                assertEquals(res.getStatusCode(), HttpStatus.SC_OK);
            }
        } finally {
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < registerNumber * 2; i++) {
                String id = String.format("testBatch%02d", i);
                String url = UrlUtils.userdata(cellName, boxName, colName, "Supplier", id);
                ODataCommon.deleteOdataResource(url);
            }
            sbuf.append(END_BOUNDARY);
        }
    }

    /**
     * _2回目の$batchで1回目と一部異なるデータを指定したときに全て201が返却されること.
     */
    @Test
    public final void _2回目の$batchで1回目と一部異なるデータを指定したときに全て201が返却されること() {
        final int registerNumber = 20;
        final int offset = 10;
        try {
            // 1回目のデータ登録(全て登録)
            TResponse response = requestBatchPost(0, registerNumber);
            // レスポンスボディのチェック
            StringBuilder sbuf = new StringBuilder();
            for (int i = 0; i < registerNumber; i++) {
                String id = String.format("testBatch%02d", i);
                sbuf.append(START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", id));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
            checkRetrieveUserData(0, registerNumber);
            sbuf = null;
            // 2回目のデータ登録(全て409エラー)
            response = requestBatchPost(offset, registerNumber);
            // レスポンスボディのチェック
            sbuf = new StringBuilder();
            for (int i = offset; i < registerNumber; i++) {
                sbuf.append(START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT));
            }
            for (int i = registerNumber; i < registerNumber + offset; i++) {
                String id = String.format("testBatch%02d", i);
                sbuf.append(START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", id));
            }
            sbuf.append(END_BOUNDARY);
            checkBatchResponseBody(response, sbuf.toString());
        } finally {
            for (int i = 0; i < registerNumber + offset; i++) {
                String id = String.format("testBatch%02d", i);
                String url = UrlUtils.userdata(cellName, boxName, colName, "Supplier", id);
                ODataCommon.deleteOdataResource(url);
            }
        }
    }

    /**
     * UserDataを$batchに1001件リクエスト指定でエラーとなること.
     */
    @Test
    public final void UserDataを$batchに1001件リクエスト指定でエラーとなること() {

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= 500; i++) {
            sb.append(START_BOUNDARY + BatchUtils.retrievePostBody("Supplier", "testBatch" + i));
        }
        sb.append(START_BOUNDARY + BatchUtils.retrievePutBody("Supplier('testBatch1')"));

        for (int i = 1; i <= 500; i++) {

            sb.append(START_BOUNDARY + BatchUtils.retrieveDeleteBody("Supplier('testBatch" + i + "')"));
        }
        sb.append(END_BOUNDARY);

        String code = PersoniumCoreException.OData.TOO_MANY_REQUESTS.getCode();
        String err = PersoniumCoreException.OData.TOO_MANY_REQUESTS.params("1,001").getMessage();

        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", sb.toString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchで存在しないエンティティタイプのデータを削除した場合404レスポンスが返却されること.
     */
    @Test
    public final void $batchで存在しないエンティティタイプのデータを削除した場合404レスポンスが返却されること() {
        String body = START_BOUNDARY + BatchUtils.retrieveDeleteBody("notExists('testBatch1')")
                + END_BOUNDARY;

        // リクエスト実行
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + BatchUtils.retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);
    }

    /**
     * $batchでPOSTメソッドのパスにIDを指定した場合400レスポンスが返却されること.
     */
    @Test
    public final void $batchでPOSTメソッドのパスにIDを指定した場合400レスポンスが返却されること() {
        String path = "Sales('srcKey')";
        String code = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + path + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY
                + BatchUtils.retrievePostBody(path, "id0001")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchでPUTメソッドのパスにIDを指定しない場合400レスポンスが返却されること.
     */
    @Test
    public final void $batchでPUTメソッドのパスにIDを指定しない場合400レスポンスが返却されること() {
        String path = "Sales";
        String code = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("PUT " + path + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY
                + BatchUtils.retrievePutBody(path)
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchでDELETEメソッドのパスにIDを指定しない場合400レスポンスが返却されること.
     */
    @Test
    public final void $batchでDELETEメソッドのパスにIDを指定しない場合400レスポンスが返却されること() {
        String path = "Sales";
        String code = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("DELETE " + path + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY
                + BatchUtils.retrieveDeleteBody(path)
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchで空のバッチリクエストを送信した場合400レスポンスが返却されること.
     */
    @Test
    public final void $batchで空のバッチリクエストを送信した場合400レスポンスが返却されること() {
        String code = PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.getCode();
        String err = PersoniumCoreException.OData.BATCH_BODY_FORMAT_HEADER_ERROR.params("Content-Type")
                .getMessage();
        String body = START_BOUNDARY + END_BOUNDARY;

        // リクエスト実行
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);
    }

    /**
     * $batchでのデータ取得確認用メソッド.
     * @param offset 開始インデックス
     * @param count 登録データ件数
     * @return レスポンス
     */
    private void checkRetrieveUserData(final int offset, final int registerNumber) {
        // 登録データの取得チェック
        for (int i = 0; i < registerNumber; i++) {
            String id = String.format("testBatch%02d", i + offset);
            String url = UrlUtils.userdata(cellName, boxName, colName, "Supplier", id);
            PersoniumResponse res = ODataCommon.getOdataResource(url);
            assertEquals(res.getStatusCode(), HttpStatus.SC_OK);
        }
    }

    /**
     * $batchでのデータ登録用メソッド.
     * @param offset 開始インデックス
     * @param count 登録データ件数
     * @return レスポンス
     */
    private TResponse requestBatchPost(final int offset, final int count) {
        StringBuilder sbuf = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sbuf.append(START_BOUNDARY
                + BatchUtils.retrievePostBody("Supplier", String.format("testBatch%02d", i + offset)));
        }
        sbuf.append(END_BOUNDARY);

        // リクエスト実行
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", sbuf.toString())
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED);
        return response;
    }

    /**
     * レスポンスヘッダのチェック.
     * @param res TResponse
     */
    private void checkBatchResponseHeaders(TResponse res) {
        // DataServiceVersionのチェック
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");

        // ContentTypeのチェック
        res.checkHeader(HttpHeaders.CONTENT_TYPE, ODataBatchProvider.MULTIPART_MIXED + "; boundary=" + BOUNDARY);
    }

}

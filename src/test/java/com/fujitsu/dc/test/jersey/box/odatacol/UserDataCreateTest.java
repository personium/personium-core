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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataCreateTest extends AbstractUserDataTest {

    // 現在時刻を取得
    static final String DATE = ((Long) new Date().getTime()).toString();

    /** テストセル１の名前. */
    public static final String TEST_CELL = "testcell" + DATE;

    /** テストセル１>テストボックス１の名前. */
    public static final String TEST_BOX = "testbox";

    /** テストセル１>テストボックス1>Odataコレクションの名前. */
    public static final String TEST_COL = "testcol";

    /** テストセル１>テストボックス1>Odataコレクションの名前. */
    public static final String TEST_ENTITYTYPE = "testentity";

    /**
     * コンストラクタ.
     */
    public UserDataCreateTest() {
        super();
    }

    /**
     * UserDataを新規作成して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成して正常に登録できること() {
        // リクエストボディを設定
        String userDataId = "userdata001:";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("floatProperty", 123.123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);
        body.put("nullStringProperty", "null");
        body.put("intStringProperty", "123");
        body.put("floatStringProperty", "123.123");
        body.put("trueStringProperty", "true");
        body.put("falseStringProperty", "false");

        // リクエスト実行
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.userData(cellName, boxName, colName, entityTypeName + "('" + userDataId + "')");
            ODataCommon.checkCommonResponseHeader(response, location);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("dynamicProperty", "dynamicPropertyValue");
            additional.put("secondDynamicProperty", "secondDynamicPropertyValue");
            additional.put("nullProperty", null);
            additional.put("intProperty", 123);
            additional.put("floatProperty", 123.123);
            additional.put("trueProperty", true);
            additional.put("falseProperty", false);
            additional.put("nullStringProperty", "null");
            additional.put("intStringProperty", "123");
            additional.put("floatStringProperty", "123.123");
            additional.put("trueStringProperty", "true");
            additional.put("falseStringProperty", "false");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * Cell名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String cellPath = "diffCell" + DATE;

        // 前準備
        try {
            // 元となるデータを作成する
            // Cell作成
            CellUtils.create(TEST_CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(TEST_CELL, TEST_BOX, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE);

            // 元のデータとCell名のみ異なるデータを作成
            // Cell作成
            CellUtils.create(cellPath, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(cellPath, TEST_BOX, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellPath, TEST_BOX,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, TEST_ENTITYTYPE, cellPath);

            // ユーザデータの一覧取得(0件HITであることを確認)
            DcResponse listResponse = getUserDataWithDcClient(cellPath,
                    TEST_BOX,
                    TEST_COL,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが0件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 0);

            // リクエスト実行
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, cellPath, TEST_BOX, TEST_COL, TEST_ENTITYTYPE);

            // ユーザデータの一覧取得(1件HITであることを確認)
            listResponse = getUserDataWithDcClient(cellPath,
                    TEST_BOX,
                    TEST_COL,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが1件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 1);

        } finally {
            // データの削除
            deleteData(cellPath, TEST_BOX, TEST_COL, TEST_ENTITYTYPE, userDataId);
            deleteData(TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE, userDataId);
        }
    }

    /**
     * Box名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Box名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String boxPath = "diffBox";

        // 前準備
        try {
            // 元となるデータを作成する
            // Cell作成
            CellUtils.create(TEST_CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(TEST_CELL, TEST_BOX, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE);

            // 元のデータとBox名のみ異なるデータを作成
            // Box作成
            BoxUtils.create(TEST_CELL, boxPath, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, boxPath,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, boxPath, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータの一覧取得(0件HITであることを確認)
            DcResponse listResponse = getUserDataWithDcClient(TEST_CELL,
                    boxPath,
                    TEST_COL,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが0件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 0);

            // リクエスト実行
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, boxPath, TEST_COL, TEST_ENTITYTYPE);

            // ユーザデータの一覧取得(1件HITであることを確認)
            listResponse = getUserDataWithDcClient(TEST_CELL,
                    boxPath,
                    TEST_COL,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが1件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 1);

        } finally {
            // データの削除
            deleteData(TEST_CELL, boxPath, TEST_COL, TEST_ENTITYTYPE, userDataId);
            deleteData(TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE, userDataId);
        }
    }

    /**
     * Col名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Col名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String colPath = "diffCol";

        // 前準備
        try {
            // 元となるデータを作成する
            // Cell作成
            CellUtils.create(TEST_CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(TEST_CELL, TEST_BOX, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE);

            // 元のデータとCollection名のみ異なるデータを作成
            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX,
                    colPath);

            // EntityType作成
            Setup.entityTypePost(colPath, TEST_BOX, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータの一覧取得(0件HITであることを確認)
            DcResponse listResponse = getUserDataWithDcClient(TEST_CELL,
                    TEST_BOX,
                    colPath,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが0件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 0);

            // リクエスト実行
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, colPath, TEST_ENTITYTYPE);

            // ユーザデータの一覧取得(1件HITであることを確認)
            listResponse = getUserDataWithDcClient(TEST_CELL,
                    TEST_BOX,
                    colPath,
                    TEST_ENTITYTYPE,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが1件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 1);

        } finally {
            // データの削除
            deleteData(TEST_CELL, TEST_BOX, colPath, TEST_ENTITYTYPE, userDataId);
            deleteData(TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE, userDataId);
        }
    }

    /**
     * EntityType名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void EntityType名のみ異なるリクエストにUserDataを新規作成して正常に登録_取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String entityPath = "diffEntity";

        // 前準備
        try {
            // 元となるデータを作成する
            // Cell作成
            CellUtils.create(TEST_CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(TEST_CELL, TEST_BOX, MASTER_TOKEN_NAME);

            // col作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX,
                    TEST_COL);

            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, TEST_ENTITYTYPE, TEST_CELL);

            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE);

            // 元のデータとEntityType名のみ異なるデータを作成
            // EntityType作成
            Setup.entityTypePost(TEST_COL, TEST_BOX, entityPath, TEST_CELL);

            // ユーザデータの一覧取得(0件HITであることを確認)
            DcResponse listResponse = getUserDataWithDcClient(TEST_CELL,
                    TEST_BOX,
                    TEST_COL,
                    entityPath,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが0件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 0);

            // リクエスト実行
            // ユーザデータ作成
            createUserData(body, HttpStatus.SC_CREATED, TEST_CELL, TEST_BOX, TEST_COL, entityPath);

            // ユーザデータの一覧取得(1件HITであることを確認)
            listResponse = getUserDataWithDcClient(TEST_CELL,
                    TEST_BOX,
                    TEST_COL,
                    entityPath,
                    "?$inlinecount=allpages");

            // レスポンスボディーのチェック.__countが1件であることを確認する
            ODataCommon.checkResponseBodyCount(listResponse.bodyAsJson(), 1);

        } finally {
            // データの削除
            deleteData(TEST_CELL, TEST_BOX, TEST_COL, entityPath, userDataId);
            deleteData(TEST_CELL, TEST_BOX, TEST_COL, TEST_ENTITYTYPE, userDataId);
        }
    }

    /**
     * 登録済のダイナミックプロパティにnullを指定して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録済のダイナミックプロパティにnullを指定して正常に登録できること() {
        String entityTypeName = "srcEntity";

        // リクエストボディを設定
        String userDataFirstId = "first";
        String userDataSecondId = "second";
        JSONObject bodyFirst = new JSONObject();
        bodyFirst.put("__id", userDataFirstId);
        bodyFirst.put("dynamicProperty", null);
        bodyFirst.put("First", "test1");

        JSONObject bodySecond = new JSONObject();
        bodySecond.put("__id", userDataSecondId);
        bodySecond.put("dynamicProperty", null);
        bodySecond.put("Second", "test2");

        // リクエスト実行
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

            // ユーザODataを登録(1回目)
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_CREATED, bodyFirst, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    userDataFirstId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            JSONObject resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertTrue(resBody.containsKey("First"));
            assertNotNull(resBody.get("First"));

            // レスポンスボディーのチェック
            Map<String, Object> additionalFirst = new HashMap<String, Object>();
            additionalFirst.put("__id", userDataFirstId);
            additionalFirst.put("dynamicProperty", "dynamicPropertyValue");

            // ユーザODataを登録(2回目)
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_CREATED, bodySecond, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);

            // ユーザデータの取得
            response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    userDataSecondId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // レスポンスボディーのチェック
            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertFalse(resBody.containsKey("First"));
            assertTrue(resBody.containsKey("Second"));
            assertNotNull(resBody.get("Second"));

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    userDataFirstId, Setup.TEST_ODATA);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    userDataSecondId, Setup.TEST_ODATA);
            Setup.entityTypeDelete(Setup.TEST_ODATA, entityTypeName, Setup.TEST_CELL1, Setup.TEST_BOX1);
        }
    }

    /**
     * UserDataを新規作成のリクエストボディにサロゲートペア文字を指定して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディにサロゲートペア文字を指定して正常に登録できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("testword", "𠀋");

        // リクエスト実行
        try {
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse response = null;
            response = rest.post(UrlUtils.userData(cellName, boxName, colName,
                    entityTypeName), body.toJSONString(), requestheaders);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("testword", "𠀋");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);
        } catch (DcException e) {
            e.printStackTrace();

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__publishedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__publishedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(PUBLISHED, "/Date(0)/");

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__publishedの値に数値型を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__publishedの値に数値型を指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(PUBLISHED, 20120101);

        // リクエスト実行
        TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__publishedの値に文字列を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__publishedの値に文字列を指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(PUBLISHED, "20120101");

        // リクエスト実行
        TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__publishedの値にnullを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__publishedの値にnullを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(PUBLISHED, null);

        // リクエスト実行
        TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0007");
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__updatedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__updatedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(UPDATED, "/Date(999)/");

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__metadataを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__metadataを指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(METADATA, null);

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * UserDataを新規作成のリクエストボディに管理情報__metadataに数値型を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成のリクエストボディに管理情報__metadataに数値型を指定した場合400エラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put(METADATA, 12345);

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * UserDataにHash値を指定して新規作成を行い４００になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataにHash値を指定して新規作成を行い４００になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        JSONObject hash = new JSONObject();
        hash.put("dynamicProperty", "dynamicPropertyValue");
        body.put("hashProperty", hash);

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * UserDataのリクエストボディに最大数の要素を指定して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのリクエストボディに最大要素数を指定して正常に登録できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum; i++) {
            body.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.userData(cellName, boxName, colName, entityType + "('" + userDataId + "')");
            ODataCommon.checkCommonResponseHeader(response, location);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            for (int i = 0; i < maxPropNum; i++) {
                additional.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
            }

            String nameSpace = getNameSpace(entityType);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, nameSpace, additional, null, etag);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }

    }

    /**
     * UserDataのリクエストボディに最大数の要素プラス1を指定して４００になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの作成でリクエストボディに最大数の要素プラス１を指定して４００になること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum + 1; i++) {
            body.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        createUserData(body, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ユーザデータの__idにnullを指定した場合_正常に作成できること. #13493 β2FT __id:null で 500 Internal Server Error
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータの__idにnullを指定した場合_正常に作成できること() {
        String userDataId = "";
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", null);
        body.put("testword", "xxx");

        // リクエスト実行
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            userDataId = ((JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results")).get("__id")
                    .toString();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * 改行とtureとfalseとnullを含むボディリクエストでエラーにならないこと. #14011 β4FT 改行を含むJSONで、500 Internal Server Error #14013 β4FT 値にtrue
     * を指定するとInternal Server Error
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 改行とtureとfalseとnullを含むボディリクエストでエラーにならないこと() {
        String userDataId = "tureFalseNull";
        try {
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("ho\rge", "huga\nhuga");
            body.put("A", true);
            body.put("B", false);
            body.put("C", null);

            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * 文字列のスキーマに整数値をsetでエラーにならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 文字列のスキーマに整数値をsetでエラーにならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("dynamicProperty", 456);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 整数値のスキーマに文字列をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 整数値のスキーマに文字列をsetでエラーとなること() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("intProperty", "abc");

            response = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            checkErrorResponse(response.bodyAsJson(), "PR400-OD-0006");

        } finally {
            deleteUserData(userDataId);
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 小数値のスキーマに文字列をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値のスキーマに文字列をsetでエラーとなること() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("floatProperty", "abc");

            response = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            checkErrorResponse(response.bodyAsJson(), "PR400-OD-0006");

        } finally {
            deleteUserData(userDataId);
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * booleanのスキーマに整数値をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void booleanのスキーマに整数値をsetでエラーとなること() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("trueProperty", 12);

            createUserData(body, HttpStatus.SC_BAD_REQUEST);

        } finally {
            deleteUserData(userDataId);
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * null値のスキーマに小数値でエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void null値のスキーマに小数値でエラーとならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("nullProperty", 456.123);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 文字列値のスキーマにboolean値でエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 文字列値のスキーマにboolean値でエラーとならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("dynamicProperty", true);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * booleanのスキーマに小数値をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void booleanのスキーマに小数値をsetでエラーとなること() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("trueProperty", 12.34);

            createUserData(body, HttpStatus.SC_BAD_REQUEST);

        } finally {
            deleteUserData(userDataId);
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 整数値のスキーマにnull値でエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 整数値のスキーマにnull値でエラーとならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("intProperty", null);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 文字列値のスキーマに小数値でエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 文字列値のスキーマに小数値でエラーとならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("dynamicProperty", 456.321);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 文字列値のスキーマにnull値でエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 文字列値のスキーマにnull値でエラーとならないこと() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("dynamicProperty", null);

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * null値のスキーマに文字列値でエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void null値のスキーマに文字列値でエラーとなること() {
        String odataName = "setodata";
        String entityTypeName = "testSchemaEntity";
        // リクエストボディを設定
        String userDataId = "userdata001";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, odataName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullPropertS", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("__id", userDataId + "1");
            body.put("nullPropertyS", "vwxyz");

            createUserData(body, HttpStatus.SC_CREATED);

        } finally {
            deleteUserData(userDataId);
            deleteUserData(userDataId + "1");
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 長さ128文字のEntityTypeにユーザーデータを作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 長さ128文字のEntityTypeにユーザーデータを作成できること() {
        String entityTypeName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String userDataId = "128EntityType";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        try {
            // 128文字のエンティティタイプを作成
            createEntityType(entityTypeName, Setup.TEST_ODATA)
                    .statusCode(HttpStatus.SC_CREATED);

            // ユーザーデータを作成
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);
        } finally {
            // ユーザーデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // エンティティタイプを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    "application/json", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeを作成する.
     * @param name EntityTypeのName
     * @param odataName oadataコレクション名
     * @return レスポンス
     */
    private TResponse createEntityType(String name, String odataName) {
        return Http.request("box/entitySet-post.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("odataSvcPath", odataName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("Name", name)
                .returns()
                .debug();
    }

    private void deleteData(String cellPath, String boxPath, String colPath, String entityPath, String userDataId) {
        // ユーザデータ削除
        deleteUserData(cellPath, boxPath, colPath, entityPath,
                userDataId, MASTER_TOKEN_NAME, "*", HttpStatus.SC_NO_CONTENT);
        // EntityType削除
        Setup.entityTypeDelete(colPath, entityPath, cellPath, boxPath);

        if (entityPath.equals(TEST_ENTITYTYPE)) {
            // col削除
            DavResourceUtils.deleteCollection(cellPath, boxPath, colPath, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            if (colPath.equals(TEST_COL)) {
                // Box削除
                BoxUtils.delete(cellPath, MASTER_TOKEN_NAME, boxPath);
                if (boxPath.equals(TEST_BOX)) {
                    // Cell削除
                    CellUtils.delete(MASTER_TOKEN_NAME, cellPath);
                }
            }
        }
    }
}

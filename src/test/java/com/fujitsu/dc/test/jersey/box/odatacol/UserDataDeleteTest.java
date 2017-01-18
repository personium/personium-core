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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData削除のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataDeleteTest extends AbstractUserDataTest {

    String cellName = "testcell1";
    String boxName = "box1";
    String colName = "setodata";
    String entityTypeName = "Category";
    String userDataId = "userdata001";

    /**
     * コンストラクタ.
     */
    public UserDataDeleteTest() {
        super();
    }

    /**
     * UserDataを新規作成して正常に削除できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataを新規作成して正常に削除できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);

        // リクエスト実行
        createUserData(body, HttpStatus.SC_CREATED);

        // ユーザデータの削除(正常)
        deleteUserData(userDataId);
    }

    /**
     * UserData削除の存在しないセルを指定するテスト.
     */
    @Test
    public final void UserData削除の存在しないセルを指定するテスト() {

        // DELETEを実行
        deleteUserData("cellhoge", boxName, colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData削除の存在しないBoxを指定するテスト.
     */
    @Test
    public final void UserData削除の存在しないBoxを指定するテスト() {

        // DELETEを実行
        deleteUserData(cellName, "boxhoge", colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData削除の存在しないODataCollectionを指定するテスト.
     */
    @Test
    public final void UserData削除の存在しないODataCollectionを指定するテスト() {

        // DELETEを実行
        deleteUserData(cellName, boxName, "colhoge", entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData削除の存在しないEntitySetを指定するテスト.
     */
    @Test
    public final void UserData削除の存在しないEntitySetを指定するテスト() {

        // DELETEを実行
        deleteUserData(cellName, boxName, colName, "entityTypehoge",
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);

    }

    /**
     * UserData削除の存在しないEntityを指定するテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserData削除の存在しないEntityを指定するテスト() {

        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);

        try {
            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    "hoge" + userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataをIfMatchヘッダの指定なしで削除出来ること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをIfMatchヘッダの指定なしで削除出来ること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);

            // DELETEを実行
            // リクエスト実行
            Http.request("box/odatacol/delete-without-IfMatch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            deleteUserData(cellName, boxName, colName, entityTypeName, userDataId,
                    DcCoreConfig.getMasterToken(), "*", -1);
        }

    }

    /**
     * UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常に削除できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常に削除できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // ユーザデータ登録
        TResponse res = createUserData(body, HttpStatus.SC_CREATED);

        // Etag取得
        String etag = res.getHeader(HttpHeaders.ETAG);

        // DELETEを実行
        deleteUserData(cellName, boxName, colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, etag, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * UserDataのボディに返却されるEtagの値を指定して正常に削除できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのボディに返却されるEtagの値を指定して正常に削除できること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // ユーザデータ登録
        TResponse createRes = createUserData(body, HttpStatus.SC_CREATED);

        // ユーザデータ取得
        TResponse getRes = getUserData(cellName, boxName, colName, entityTypeName, userDataId,
                DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);

        // ボディからEtag取得
        JSONObject results = (JSONObject) ((JSONObject) createRes.bodyAsJson().get("d")).get("results");
        String createEtag = (String) ((JSONObject) results.get("__metadata")).get("etag");
        results = (JSONObject) ((JSONObject) getRes.bodyAsJson().get("d")).get("results");
        String getEtag = (String) ((JSONObject) results.get("__metadata")).get("etag");

        // etagが同一であることを確認
        assertEquals(createEtag, getEtag);

        // DELETEを実行
        deleteUserData(cellName, boxName, colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, createEtag, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * UserDataのIfMatchヘッダにEtagのVersionに不正な値を指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダにEtagのVersionに不正な値を指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "W/\"" + String.valueOf(version + 1) + "-" + String.valueOf(updated) + "\"",
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataのIfMatchヘッダにEtagのUpdatedに不正な値を指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダにEtagのUpdatedに不正な値を指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "W/\"" + String.valueOf(version) + "-" + String.valueOf(updated + 1) + "\"",
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataのIfMatchヘッダにEtagのStrongValidationを指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダにEtagのStrongValidationを指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "\"" + String.valueOf(version) + "-" + String.valueOf(updated) + "\"",
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataのIfMatchヘッダに数字のみを指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダに数字のみを指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long updated = ODataCommon.getEtagUpdated(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    String.valueOf(updated),
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataのIfMatchヘッダにEtagにWeakValidationで不正なフォーマットを指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダにEtagにWeakValidationで不正なフォーマットを指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "W/\"" + String.valueOf(version) + String.valueOf(updated) + "\"",
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserDataのIfMatchヘッダに文字列を含むEtagを指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダに文字列を含むEtagを指定して４１２エラーとなること() {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME,
                    "W/\"" + String.valueOf(version) + "-" + "test" + "\"",
                    HttpStatus.SC_PRECONDITION_FAILED);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserData削除のこデータの存在するデータを指定するテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserData削除のこデータの存在するデータを指定するテスト() {

        try {
            // 親データを作成
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);

            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);

            // DELETEを実行
            deleteUserData(cellName, boxName, colName, entityTypeName,
                    "userDatahaoge", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(userDataId);
        }

    }

    /**
     * UserData削除の無効な認証トークンを指定するテスト.
     */
    @Test
    public final void UserData削除の無効な認証トークンを指定するテスト() {
        // DELETEを実行
        deleteUserData(cellName, boxName, colName, entityTypeName,
                "userDatahaoge", "tokenhoge", HttpStatus.SC_UNAUTHORIZED);

    }

    /**
     * UserDataをシングルクォート無しで削除した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをシングルクォート無しで削除した場合400エラーとなること() {

        String userdataKey = "123456";

        // リクエスト実行
        try {
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userdataKey);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullProperty", null);
            body.put("intProperty", 123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);

            // リクエスト実行
            createUserData(body, HttpStatus.SC_CREATED);

            // ユーザデータ削除
            String requestURL =
                    UrlUtils.userdata(cellName, boxName,  colName, entityTypeName + "(" + userdataKey + ")", null);
            DcRequest req = DcRequest.delete(requestURL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            DcResponse res = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
        } finally {
            deleteUserData(userdataKey);
        }
    }

}

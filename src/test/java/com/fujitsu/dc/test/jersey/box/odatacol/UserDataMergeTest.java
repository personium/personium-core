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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

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
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataMergeTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataMergeTest() {
        super();
        colName = "setodata";
    }

    /**
     * UserDataのMERGEで存在するプロパティを指定してプロパティが更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEで存在するプロパティを指定してプロパティが更新されること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "updateDynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのMERGEで存在しないプロパティを指定してプロパティが追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEで存在しないプロパティを指定してプロパティが追加されること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("newProperty", "newPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "dynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");
        expectedDynamicFields.put("newProperty", "newPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのMERGEで存在しないkeyを指定し404が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEで存在しないkeyを指定し404が返却されること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("newProperty", "newPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // マージリクエスト実行
            res = mergeRequest("dummyKey", updateReqBody);
            res.statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのMERGEで__idを指定した場合__idが無視されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEで__idを指定した場合__idが無視されること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("__id", "newuserdata001");
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "updateDynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのMERGEで存在しないプロパティを指定してプロパティが追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをIfMatchヘッダの指定なしでMERGEして４１２エラーとなること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        try {
            createUserData(createReqBody, HttpStatus.SC_CREATED);

            Http.request("box/odatacol/merge.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateReqBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのIfMatchヘッダにEtagのVersionに不正な値を指定して４１２エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダにEtagのVersionに不正な値を指定して４１２エラーとなること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // マージリクエスト実行
            res = mergeRequest(userDataId, "W/\"" + String.valueOf(version + 1) + "-" + String.valueOf(updated) + "\"",
                    updateReqBody);
            res.statusCode(HttpStatus.SC_PRECONDITION_FAILED);

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
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // マージリクエスト実行
            res = mergeRequest(userDataId, "W/\"" + String.valueOf(version) + "-" + String.valueOf(updated + 1) + "\"",
                    updateReqBody);
            res.statusCode(HttpStatus.SC_PRECONDITION_FAILED);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常にMERGEできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常にMERGEできること() {
        String userDataId = "userdata001";

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("newProperty", "newPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "dynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");
        expectedDynamicFields.put("newProperty", "newPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, etag, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのリクエストボディに最大要素数のプロパティを登録し存在するプロパティを１つ指定してMERGEできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのリクエストボディに最大要素数のプロパティを登録し存在するプロパティを１つ指定してMERGEできること() {
        String userDataId = "userdata001";

        // 最大要素数
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        for (int i = 0; i < maxPropNum; i++) {
            createReqBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty" + 0, "updatePropertyValue" + 0);

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty" + 0, "updatePropertyValue" + 0);
        for (int i = 1; i < maxPropNum; i++) {
            expectedDynamicFields.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン情報取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataのリクエストボディに最大要素数の存在するプロパティを指定してMERGEできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのリクエストボディに最大要素数の存在するプロパティを指定してMERGEできること() {
        String userDataId = "userdata001";

        // 最大要素数
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        for (int i = 0; i < maxPropNum; i++) {
            createReqBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        for (int i = 0; i < maxPropNum; i++) {
            updateReqBody.put("dynamicProperty" + i, "updatePropertyValue" + i);
        }

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        for (int i = 0; i < maxPropNum; i++) {
            expectedDynamicFields.put("dynamicProperty" + i, "updatePropertyValue" + i);
        }

        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン情報取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, updateReqBody);

            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataのリクエストボディに最大要素数プラス１の存在するプロパティを指定して400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのリクエストボディに最大要素数プラス１の存在するプロパティを指定して400エラーとなること() {
        String userDataId = "userdata001";

        // 最大要素数
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        for (int i = 0; i < maxPropNum; i++) {
            createReqBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        for (int i = 0; i < maxPropNum + 1; i++) {
            updateReqBody.put("dynamicProperty" + i, "updatePropertyValue" + i);
        }

        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // マージリクエスト実行
            res = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getCode(),
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getMessage());

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataのMERGE後のプロパティ数が最大要素数の場合にMERGEできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGE後のプロパティ数が最大要素数の場合にMERGEできること() {
        String userDataId = "userdata001";

        // 最大要素数
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        // 初期作成用のリクエストボディ
        // 最大要素数 - 1のプロパティを指定
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        for (int i = 0; i < maxPropNum - 1; i++) {
            createReqBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty399", "newPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        for (int i = 0; i < maxPropNum - 1; i++) {
            expectedDynamicFields.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }
        expectedDynamicFields.put("dynamicProperty399", "newPropertyValue");

        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン情報取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, expectedDynamicFields, etag, published);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataのMERGE後のプロパティ数が最大要素数プラス１の場合に400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGE後のプロパティ数が最大要素数プラス１の場合に400エラーとなること() {
        String userDataId = "userdata001";

        // 最大要素数
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        // 初期作成用のリクエストボディ
        // 最大要素数 - 1のプロパティを指定
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        for (int i = 0; i < maxPropNum - 1; i++) {
            createReqBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("newProperty1", "newPropertyValue1");
        updateReqBody.put("newProperty2", "newPropertyValue2");

        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // マージリクエスト実行
            res = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType,
                    userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getCode(),
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getMessage());

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataのMERGEでstaticプロパティのみを指定した場合指定したプロパティのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEでstaticプロパティのみを指定した場合指定したプロパティのみ更新されること() {
        String userDataId = "userdata001";
        String propName = "declaredProperty";
        String locationUserData = null, locationProperty = null;

        // Nullable=flaseのPropertyを追加
        DcResponse resProperty = UserDataUtils.createProperty(cellName, boxName, Setup.TEST_ODATA, propName,
                "Category", "Edm.String", false, "", "None", false, null);
        locationProperty = resProperty.getFirstHeader(HttpHeaders.LOCATION);

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");
        createReqBody.put(propName, "declaredPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put(propName, "updateDeclaredPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "dynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");
        expectedDynamicFields.put(propName, "updateDeclaredPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);
            locationUserData = res.getLocationHeader();

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            if (locationUserData != null) {
                ODataCommon.deleteOdataResource(locationUserData);
            }
            if (locationProperty != null) {
                ODataCommon.deleteOdataResource(locationProperty);
            }
        }
    }

    /**
     * UserDataのMERGEでdynamicプロパティのみを指定した場合指定したプロパティのみ更新されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのMERGEでdynamicプロパティのみを指定した場合指定したプロパティのみ更新されること() {
        String userDataId = "userdata001";
        String propName = "declaredProperty";
        String locationUserData = null, locationProperty = null;

        // Nullable=flaseのPropertyを追加
        DcResponse resProperty = UserDataUtils.createProperty(cellName, boxName, Setup.TEST_ODATA, propName,
                "Category", "Edm.String", false, "", "None", false, null);
        locationProperty = resProperty.getFirstHeader(HttpHeaders.LOCATION);

        // 初期作成用のリクエストボディ
        JSONObject createReqBody = new JSONObject();
        createReqBody.put("__id", userDataId);
        createReqBody.put("dynamicProperty", "dynamicPropertyValue");
        createReqBody.put("secondDynamicProperty", "secondDynamicPropertyValue");
        createReqBody.put(propName, "declaredPropertyValue");

        // MERGE用のリクエストボディ
        JSONObject updateReqBody = new JSONObject();
        updateReqBody.put("dynamicProperty", "updateDynamicPropertyValue");

        // チェック項目の作成
        Map<String, Object> expectedDynamicFields = new HashMap<String, Object>();
        expectedDynamicFields.put("dynamicProperty", "updateDynamicPropertyValue");
        expectedDynamicFields.put("secondDynamicProperty", "secondDynamicPropertyValue");
        expectedDynamicFields.put(propName, "declaredPropertyValue");

        try {
            TResponse res = createUserData(createReqBody, HttpStatus.SC_CREATED);
            locationUserData = res.getLocationHeader();

            // __publishedの取得
            String published = ODataCommon.getPublished(res);
            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(res.getHeader(HttpHeaders.ETAG));

            // マージリクエスト実行
            res = mergeRequest(userDataId, updateReqBody);
            res.statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダからETAGを取得する
            String etag = res.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(res, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, expectedDynamicFields, etag, published);

        } finally {
            if (locationUserData != null) {
                ODataCommon.deleteOdataResource(locationUserData);
            }
            if (locationProperty != null) {
                ODataCommon.deleteOdataResource(locationProperty);
            }
        }
    }

    /**
     * 登録済のダイナミックプロパティにnullを指定して正常に部分更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録済のダイナミックプロパティにnullを指定して正常に部分更新できること() {
        String entityTypeName = "srcEntity";

        // 登録用リクエストボディ
        String userDataFirstId = "userdata";
        JSONObject bodyFirst = new JSONObject();
        bodyFirst.put("__id", userDataFirstId);
        bodyFirst.put("dynamicProperty1", null);
        bodyFirst.put("dynamicProperty2", null);
        bodyFirst.put("First", "test1");

        // 更新用リクエストボディ：dynamicProperty2は省略
        JSONObject bodySecond = new JSONObject();
        bodySecond.put("__id", userDataFirstId);
        bodySecond.put("dynamicProperty1", null);
        bodySecond.put("dynamicProperty3", null);
        bodySecond.put("Second", "test2");

        // リクエスト実行
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

            // ユーザODataを登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_CREATED, bodyFirst, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName);

            // ユーザデータの取得
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    userDataFirstId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            JSONObject resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            assertTrue(resBody.containsKey("dynamicProperty1"));
            assertNull(resBody.get("dynamicProperty1"));
            assertTrue(resBody.containsKey("dynamicProperty2"));
            assertNull(resBody.get("dynamicProperty2"));
            assertTrue(resBody.containsKey("First"));
            assertNotNull(resBody.get("First"));

            // レスポンスボディーのチェック
            Map<String, Object> additionalFirst = new HashMap<String, Object>();
            additionalFirst.put("__id", userDataFirstId);
            additionalFirst.put("dynamicProperty", "dynamicPropertyValue");

            // ユーザODataを更新
            UserDataUtils.merge(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NO_CONTENT, bodySecond, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, userDataFirstId, "*");

            // ユーザデータの取得
            response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    userDataFirstId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // レスポンスボディーのチェック
            assertTrue(resBody.containsKey("dynamicProperty1"));
            assertNull(resBody.get("dynamicProperty1"));
            assertTrue(resBody.containsKey("dynamicProperty2"));
            assertNull(resBody.get("dynamicProperty2"));
            assertTrue(resBody.containsKey("dynamicProperty3"));
            assertNull(resBody.get("dynamicProperty3"));
            assertTrue(resBody.containsKey("First"));
            assertNotNull(resBody.get("First"));
            assertTrue(resBody.containsKey("Second"));
            assertNotNull(resBody.get("Second"));

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    userDataFirstId, Setup.TEST_ODATA);
            Setup.entityTypeDelete(Setup.TEST_ODATA, entityTypeName, Setup.TEST_CELL1, Setup.TEST_BOX1);
        }
    }

    private TResponse mergeRequest(String userDataId, JSONObject updateReqBody) {
        return mergeRequest(userDataId, "*", updateReqBody);
    }

    private TResponse mergeRequest(String userDataId, String ifMatch, JSONObject updateReqBody) {
        return Http.request("box/odatacol/merge.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("id", userDataId)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", ifMatch)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", updateReqBody.toJSONString())
                .returns()
                .debug();
    }

    private TResponse mergeRequest(String cell,
            String box,
            String col,
            String entityType,
            String userDataId,
            JSONObject updateReqBody) {
        return Http.request("box/odatacol/merge.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("id", userDataId)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", "*")
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", updateReqBody.toJSONString())
                .returns()
                .debug();
    }

    /**
     * ユーザデータが更新されていることを確認する.
     * @param userDataId userDataId
     * @param additional additional
     * @param etag etag
     * @param published published
     */
    public final void checkUserData(String userDataId, Map<String, Object> additional, String etag, String published) {
        checkUserData(cellName, boxName, colName, entityTypeName, userDataId, additional, etag, published);
    }

    /**
     * ユーザデータが更新されていることを確認する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId userDataId
     * @param additional additional
     * @param etag etag
     * @param published published
     */
    public final void checkUserData(String cell, String box, String col, String entityType,
            String userDataId, Map<String, Object> additional, String etag, String published) {
        // ユーザデータの取得
        TResponse getResponse = getUserData(cell, box, col, entityType,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        String nameSpace = getNameSpace(entityType);
        ODataCommon.checkResponseBody(getResponse.bodyAsJson(), null, nameSpace, additional, null, etag, published);
    }
}

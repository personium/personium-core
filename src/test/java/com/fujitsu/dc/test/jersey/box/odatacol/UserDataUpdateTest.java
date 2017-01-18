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
import static org.junit.Assert.assertFalse;
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
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataUpdateTest extends AbstractUserDataWithNP {

    /**
     * コンストラクタ.
     */
    public UserDataUpdateTest() {
        super();
    }

    /**
     * UserDataをリクエストボディにID指定なしで正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをリクエストボディにID指定なしで正常に更新できること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(response);

            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(response.getHeader(HttpHeaders.ETAG));

            JSONObject updateBody = new JSONObject();
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            updateBody.put("nullProperty", null);
            updateBody.put("intProperty", 123);
            updateBody.put("floatProperty", 123.123);
            updateBody.put("trueProperty", true);
            updateBody.put("falseProperty", false);
            updateBody.put("nullStringProperty", "null");
            updateBody.put("intStringProperty", "123");
            updateBody.put("floatStringProperty", "123.123");
            updateBody.put("trueStringProperty", "true");
            updateBody.put("falseStringProperty", "false");
            response = Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, etag, published);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをリクエストボディにID指定ありで正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをリクエストボディにID指定ありで正常に更新できること() {
        colName = "setodata";
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);

            // __publishedの取得
            String published = ODataCommon.getPublished(response);

            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(response.getHeader(HttpHeaders.ETAG));

            JSONObject updateBody = new JSONObject();
            updateBody.put("__id", "update");
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            updateBody.put("nullProperty", null);
            updateBody.put("intProperty", 123);
            updateBody.put("floatProperty", 123.123);
            updateBody.put("trueProperty", true);
            updateBody.put("falseProperty", false);
            updateBody.put("nullStringProperty", "null");
            updateBody.put("intStringProperty", "123");
            updateBody.put("floatStringProperty", "123.123");
            updateBody.put("trueStringProperty", "true");
            updateBody.put("falseStringProperty", "false");
            response = Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, version + 1);

            // ユーザデータが更新されていることを確認
            checkUserData(userDataId, etag, published);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * リンク情報を0対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンク情報を0対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること() {
        colName = ODATA_COLLECTION;
        // リクエスト実行
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D);

            // 多重度0のユーザデータを更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("test", "test0");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", ENTITY_TYPE_A)
                    .with("id", "parent")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // 多重度0のユーザデータを取得する
            TResponse response = getUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent",
                    AbstractCase.MASTER_TOKEN_NAME, "", HttpStatus.SC_OK);

            // リンク情報が更新されていないことを確認
            String nameSpace = getNameSpace(ENTITY_TYPE_A);
            Map<String, String> np = new HashMap<String, String>();
            np.put("_" + ENTITY_TYPE_D, UrlUtils.userData(cellName,
                    boxName, colName, ENTITY_TYPE_A + "('parent')/_" + ENTITY_TYPE_D));
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, null, np);

            // 多重度*のユーザデータを更新する
            updateBody.put("test", "test1");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", ENTITY_TYPE_D)
                    .with("id", "userdataNP")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // 多重度0のユーザデータを取得する
            response = execNpList(ENTITY_TYPE_D, "userdataNP", ENTITY_TYPE_A);

            // リンク情報が更新されていないことを確認
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("parent", UrlUtils.userData(cellName, boxName, colName, ENTITY_TYPE_A + "('parent')"));
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", np, null);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D);
        }
    }

    /**
     * リンク情報を1対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンク情報を1対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること() {
        colName = ODATA_COLLECTION;
        // リクエスト実行
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // 多重度1のユーザデータを更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("test", "test0");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", ENTITY_TYPE_B)
                    .with("id", "parent")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // 多重度1のユーザデータを取得する
            TResponse response = getUserData(cellName, boxName, colName, ENTITY_TYPE_B, "parent",
                    AbstractCase.MASTER_TOKEN_NAME, "", HttpStatus.SC_OK);

            // リンク情報が更新されていないことを確認
            String nameSpace = getNameSpace(ENTITY_TYPE_B);
            Map<String, String> np = new HashMap<String, String>();
            np.put("_" + ENTITY_TYPE_D, UrlUtils.userData(cellName,
                    boxName, colName, ENTITY_TYPE_B + "('parent')/_" + ENTITY_TYPE_D));
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, null, np);

            // 多重度*のユーザデータを更新する
            updateBody.put("test", "test1");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", ENTITY_TYPE_D)
                    .with("id", "userdataNP")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // 多重度1のユーザデータを取得する
            response = execNpList(ENTITY_TYPE_D, "userdataNP", ENTITY_TYPE_B);

            // リンク情報が更新されていないことを確認
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("parent", UrlUtils.userData(cellName, boxName, colName, ENTITY_TYPE_B + "('parent')"));
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", np, null);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * リンク情報をN対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンク情報をN対Nで持つUserDataを更新してリンク情報が上書きされずに正常に更新できること() {
        colName = ODATA_COLLECTION;
        // リクエスト実行
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_C, ENTITY_TYPE_D);

            // 多重度*のユーザデータを更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("test", "test0");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", ENTITY_TYPE_C)
                    .with("id", "parent")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // 多重度*のユーザデータを取得する
            TResponse response = getUserData(cellName, boxName, colName, ENTITY_TYPE_C, "parent",
                    AbstractCase.MASTER_TOKEN_NAME, "", HttpStatus.SC_OK);

            // リンク情報が更新されていないことを確認
            String nameSpace = getNameSpace(ENTITY_TYPE_C);
            Map<String, String> np = new HashMap<String, String>();
            np.put("_" + ENTITY_TYPE_D, UrlUtils.userData(cellName,
                    boxName, colName, ENTITY_TYPE_C + "('parent')/_" + ENTITY_TYPE_D));
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, null, np);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_C, ENTITY_TYPE_D);
        }
    }

    /**
     * UserDataのリクエストボディに最大要素数を指定して正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのリクエストボディに最大要素数を指定して正常に更新できること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        String entityType = Setup.TEST_ENTITYTYPE_MDP;
        try {
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityType);

            // Etagのバージョン取得
            long version = ODataCommon.getEtagVersion(response.getHeader(HttpHeaders.ETAG));

            JSONObject updateBody = new JSONObject();
            for (int i = 0; i < maxPropNum; i++) {
                updateBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
            }

            response = Http.request("box/odatacol/update.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", entityType)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, version + 1);

            // レスポンスヘッダからETAGを取得する
            String etag = response.getHeader(HttpHeaders.ETAG);

            // ユーザデータが更新されていることを確認
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            for (int i = 0; i < maxPropNum; i++) {
                additional.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
            }

            // ユーザデータの取得
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityType);
            ODataCommon.checkResponseBody(getResponse.bodyAsJson(), null, nameSpace, additional, null, etag, null);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityType, userDataId, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * UserDataの更新でリクエストボディに最大数の要素プラス１を指定して４００になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの更新でリクエストボディに最大数の要素プラス１を指定して４００になること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");

        // リクエスト実行
        try {
            createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();
            for (int i = 0; i < maxPropNum + 1; i++) {
                updateBody.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
            }

            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの更新でリクエストボディに管理情報__publishedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの更新でリクエストボディに管理情報__publishedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            createUserData(body, HttpStatus.SC_CREATED);

            // __publishedを指定して更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("__id", userDataId);
            updateBody.put(PUBLISHED, "/Date(0)/");

            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの更新でリクエストボディに管理情報__updatedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの更新でリクエストボディに管理情報__updatedを指定した場合400エラーとなること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            createUserData(body, HttpStatus.SC_CREATED);

            // __updatedを指定して更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("__id", userDataId);
            updateBody.put(UPDATED, "/Date(0)/");

            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataの更新でリクエストボディに管理情報__metadataを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの更新でリクエストボディに管理情報__metadataを指定した場合400エラーとなること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            createUserData(body, HttpStatus.SC_CREATED);

            // __metadataを指定して更新する
            JSONObject updateBody = new JSONObject();
            updateBody.put("__id", userDataId);
            updateBody.put(METADATA, "test");

            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをIfMatchヘッダの指定なしで更新出来ること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをIfMatchヘッダの指定なしで更新出来ること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // リクエスト実行
        try {
            createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update-without-IfMatch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのIfMatchヘッダに現在登録されているEtagの値を指定して正常に更新できること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ登録
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = response.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            response = Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", etag)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, version + 1);

            // レスポンスヘッダからETAGを取得する
            etag = response.getHeader(HttpHeaders.ETAG);

            // ユーザデータが更新されていることを確認
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            checkUserData(userDataId, updateBody, etag, null);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataのボディに返却されるEtagの値を指定して正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataのボディに返却されるEtagの値を指定して正常に更新できること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ登録
            TResponse response = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            // ボディからEtag取得
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String etag = (String) ((JSONObject) results.get("__metadata")).get("etag");
            long version = ODataCommon.getEtagVersion(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            response = Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", etag)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response, version + 1);

            // レスポンスヘッダからETAGを取得する
            etag = response.getHeader(HttpHeaders.ETAG);

            // ユーザデータが更新されていることを確認
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            additional.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            checkUserData(userDataId, updateBody, etag, null);
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
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "W/\"" + String.valueOf(version + 1) + "-" + String.valueOf(updated) + "\"")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "W/\"" + String.valueOf(version) + "-" + String.valueOf(updated + 1) + "\"")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "\"" + String.valueOf(version) + "-" + String.valueOf(updated) + "\"")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long updated = ODataCommon.getEtagUpdated(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", String.valueOf(updated))
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);
            long updated = ODataCommon.getEtagUpdated(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "W/\"" + String.valueOf(version) + String.valueOf(updated) + "\"")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        colName = "setodata";
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        try {
            // ユーザデータ登録
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            JSONObject updateBody = new JSONObject();

            // Etag取得
            String etag = res.getHeader(HttpHeaders.ETAG);
            long version = ODataCommon.getEtagVersion(etag);

            // ユーザデータ更新
            updateBody.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "W/\"" + String.valueOf(version) + "-" + "test" + "\"")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", updateBody.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_PRECONDITION_FAILED)
                    .debug();

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
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("dynamicProperty", 456);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 整数値のスキーマに小数値を指定した場合は登録できない.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 整数値のスキーマに小数値を指定した場合は登録できない() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("intProperty", 111);
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            body.put("intProperty", 111.1);
            UserDataUtils.update(MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST, body, cellName, boxName,
                    colName, entityTypeName, userDataId, "*");
        } finally {
            deleteUserData(entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);
        }
    }

    /**
     * 小数値のスキーマに文字列をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値のスキーマに文字列をsetでエラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("floatProperty", "abcdefg");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * boolenのスキーマにnullをsetでエラーにならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void boolenのスキーマにnullをsetでエラーにならないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("trueProperty", null);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * nullのスキーマに整数をsetでエラーにならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void nullのスキーマに整数をsetでエラーにならないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("nullProperty", 123465);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 小数値のスキーマにboolean値をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値のスキーマにboolean値をsetでエラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("floatProperty", false);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 整数値のスキーマにboolean値をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 整数値のスキーマにboolean値をsetでエラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("intProperty", false);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 小数値のスキーマに整数値をsetでエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値のスキーマに整数値をsetでエラーとならないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("floatProperty", 9876);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 小数値のスキーマにnull値をsetでエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 小数値のスキーマにnull値をsetでエラーとならないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("floatProperty", null);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * boolean値のスキーマに文字列値をsetでエラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void boolean値のスキーマに文字列値をsetでエラーとなること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
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
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("floatProperty", "abcdefg");
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * null値のスキーマにboolean値をsetでエラーとならないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void null値のスキーマにboolean値をsetでエラーとならないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        String entityTypeName = "scmTest";

        try {
            // EntityTypeの登録
            createEntityType(entityTypeName, colName);
            // Schemaに数値をset

            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            body.put("secondDynamicProperty", "secondDynamicPropertyValue");
            body.put("nullPropertyB", null);
            body.put("intProperty", 123);
            body.put("floatProperty", 123.123);
            body.put("trueProperty", true);
            body.put("falseProperty", false);
            body.put("nullStringProperty", "null");
            body.put("intStringProperty", "123");
            body.put("floatStringProperty", "123.123");
            body.put("trueStringProperty", "true");
            body.put("falseStringProperty", "false");
            createUserData(body, entityTypeName, HttpStatus.SC_CREATED);

            // Schemaに文字列をセット
            // リクエストボディを設定
            body.put("nullPropertyB", false);
            Http.request("box/odatacol/update.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("id", userDataId)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("contentType", MediaType.APPLICATION_JSON)
                    .with("ifMatch", "*")
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body.toJSONString())
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteUserData(entityTypeName, userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * UserDataの更新時にidをシングルクォート無しで指定した場合エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataの更新時にidをシングルクォート無しで指定した場合エラーとなること() {
        // リクエストボディを設定
        colName = "setodata";
        String userDataId = "3830";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        // リクエスト実行
        try {
            // ユーザデータ登録
            createUserData(body, HttpStatus.SC_CREATED);

            // ユーザデータ更新
            String requestURL =
                    UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "Category(3830)", null);
            DcRequest req = DcRequest.put(requestURL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");
            req.addJsonBody("dynamicProperty", "dynamicPropertyValue");

            // リクエスト実行
            DcResponse res = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                    DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * 登録済のダイナミックプロパティにnullを指定して正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録済のダイナミックプロパティにnullを指定して正常に更新できること() {
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
            UserDataUtils.update(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NO_CONTENT, bodySecond, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, userDataFirstId, "*");

            // ユーザデータの取得
            response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName,
                    userDataFirstId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // レスポンスボディーのチェック
            assertTrue(resBody.containsKey("dynamicProperty1"));
            assertNull(resBody.get("dynamicProperty1"));
            assertFalse(resBody.containsKey("dynamicProperty2"));
            assertTrue(resBody.containsKey("dynamicProperty3"));
            assertNull(resBody.get("dynamicProperty3"));
            assertFalse(resBody.containsKey("First"));
            assertTrue(resBody.containsKey("Second"));
            assertNotNull(resBody.get("Second"));

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, entityTypeName,
                    userDataFirstId, Setup.TEST_ODATA);
            Setup.entityTypeDelete(Setup.TEST_ODATA, entityTypeName, Setup.TEST_CELL1, Setup.TEST_BOX1);
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

    /**
     * ユーザーデータを削除する.
     * @param name EntityTypeのName
     * @param userDataId 削除対象ID
     */
    protected void deleteUserData(String name, String userDataId) {
        // リクエスト実行
        Http.request("box/odatacol/delete.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", name)
                .with("id", userDataId)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ユーザーデータを作成する.
     * @param body リクエストボディ
     * @param entityType EntityTypeのName
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse createUserData(JSONObject body, String entityType, int sc) {
        TResponse response = Http.request("box/odatacol/create.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityType)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

    /**
     * ユーザデータが更新されていることを確認する.
     * @param userDataId userDataId
     * @param etag etag
     * @param published published
     */
    public final void checkUserData(String userDataId, String etag, String published) {

        // チェック項目の作成
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("__id", userDataId);
        additional.put("secondDynamicProperty", "updateSecondDynamicPropertyValue");
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

        checkUserData(userDataId, additional, etag, published);
    }

    /**
     * ユーザデータが更新されていることを確認する.
     * @param userDataId userDataId
     * @param additional additional
     * @param etag etag
     * @param published published
     */
    public final void checkUserData(String userDataId, Map<String, Object> additional, String etag, String published) {
        // ユーザデータの取得
        TResponse getResponse = getUserData(cellName, boxName, colName, entityTypeName,
                userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスボディーのチェック
        String nameSpace = getNameSpace(entityTypeName);
        ODataCommon.checkResponseBody(getResponse.bodyAsJson(), null, nameSpace, additional, null, etag, published);
    }
}

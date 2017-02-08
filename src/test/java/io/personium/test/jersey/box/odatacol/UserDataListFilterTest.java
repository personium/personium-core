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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * UserData一覧のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListFilterTest extends AbstractUserDataTest {

    static String userDataId201 = "userdata201";
    static String userDataId202 = "userdata202";

    /**
     * コンストラクタ.
     */
    public UserDataListFilterTest() {
        super();
    }

    /**
     * UserDataに完全一致検索クエリのキーを文字列で指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに完全一致検索クエリのキーを文字列で指定して対象のデータのみ取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String userDataId2 = "userdata002";
        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId2);
        body2.put("dynamicProperty", "dynamicPropertyValue2");

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse res1 = createUserData(body, HttpStatus.SC_CREATED);
            TResponse res2 = createUserData(body2, HttpStatus.SC_CREATED);

            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            Map<String, String> etag = new HashMap<String, String>();
            etag.put("userdata001", res1.getHeader(HttpHeaders.ETAG));
            etag.put("userdata002", res2.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue%27")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put("dynamicProperty", "dynamicPropertyValue");

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * Edm_String型の$filter検索の検証_制御コードのエスケープ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_String型の$filter検索の検証_制御コードのエスケープ() {
        // 制御コードのエスケープ
        String userDataId1 = "idctl1";
        String userDataId2 = "idctl2";
        try {
            // ユーザデータの作成
            JSONObject body1 = new JSONObject();
            body1.put("__id", userDataId1);
            body1.put("testField", "value_\\u0001_value"); // 0x01(Ctl-A)
            createUserData(body1, HttpStatus.SC_CREATED);
            // ユーザデータの作成
            JSONObject body2 = new JSONObject();
            body2.put("__id", userDataId2);
            body2.put("testField", "value_\\\\u0001_value");
            createUserData(body2, HttpStatus.SC_CREATED);

            // Edm.Stringの検索条件： 制御コードを含む文字列
            String query = "?\\$filter=testField+eq+%27value_%5Cu0001_value%27&\\$inlinecount=allpages";
            TResponse res = UserDataUtils.list(cellName, boxName, colName, entityTypeName, query,
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId1); // 制御コードを含むデータが検索されること
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, "UserData." + entityTypeName, additional, 1);
            // Edm.Stringの検索条件： エスケープ文字(\)を含む文字列
            query = "?\\$filter=testField+eq+%27value_%5C%5Cu0001_value%27&\\$inlinecount=allpages";
            res = UserDataUtils.list(cellName, boxName, colName, entityTypeName, query, AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("__id", userDataId2); // 制御コードを含まないデータが検索されること
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, "UserData." + entityTypeName, additional, 1);
            // Edm.Stringの検索条件： 制御コードとして扱えない文字を含む文字列
            query = "?\\$filter=testField+eq+%27value_%5Cu001_value%27&\\$inlinecount=allpages";
            res = UserDataUtils.list(cellName, boxName, colName, entityTypeName, query,
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST); // 不正なデータのため400エラーとなること
            ODataCommon.checkErrorResponseBody(res,
                    PersoniumCoreException.OData.OPERATOR_AND_OPERAND_UNABLE_TO_UNESCAPE.getCode(),
                    PersoniumCoreException.OData.OPERATOR_AND_OPERAND_UNABLE_TO_UNESCAPE.params("value_\\u001_value")
                            .getMessage());
        } finally {
            deleteUserData(userDataId1);
            deleteUserData(userDataId2);
            deleteDynamicProperty("testField");
        }
    }

    /**
     * UserDataに完全一致検索クエリのキーを整数値で指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーを整数値で指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=number+eq+5&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディを取得する
        JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        Map<String, String> etag = new HashMap<String, String>();
        // 一件取得をし、レスポンスヘッダからEtag情報を取得する
        for (Object result : results) {
            // __idを取得
            String resId = (String) ((JSONObject) result).get("__id");
            // 取得した__idから一件取得する
            TResponse responseUnit = getUserData(cellName, boxName, colName, sdEntityTypeName,
                    resId, PersoniumUnitConfig.getMasterToken(), "", HttpStatus.SC_OK);
            // レスポンスヘッダからEtag情報を取得する
            etag.put(resId, responseUnit.getHeader(HttpHeaders.ETAG));
        }
        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata005')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("sample", "sample5");
        additionalprop.put("test", "test5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, etag);
    }

    /**
     * UserDataに完全一致検索クエリのキーを小数値で指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーを小数値で指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=decimal+eq+5.1&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディを取得する
        JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        Map<String, String> etag = new HashMap<String, String>();
        // 一件取得をし、レスポンスヘッダからEtag情報を取得する
        for (Object result : results) {
            // __idを取得
            String resId = (String) ((JSONObject) result).get("__id");
            // 取得した__idから一件取得する
            TResponse responseUnit = getUserData(cellName, boxName, colName, sdEntityTypeName,
                    resId, PersoniumUnitConfig.getMasterToken(), "", HttpStatus.SC_OK);
            // レスポンスヘッダからEtag情報を取得する
            etag.put(resId, responseUnit.getHeader(HttpHeaders.ETAG));
        }

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata005')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("sample", "sample5");
        additionalprop.put("test", "test5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, etag);
    }

    /**
     * UserDataに完全一致検索クエリのキーをNULLで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーをNULLで指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=number+eq+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスボディを取得する
        JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        Map<String, String> etag = new HashMap<String, String>();
        // 一件取得をし、レスポンスヘッダからEtag情報を取得する
        for (Object result : results) {
            // __idを取得
            String resId = (String) ((JSONObject) result).get("__id");
            // 取得した__idから一件取得する
            TResponse responseUnit = getUserData(cellName, boxName, colName, sdEntityTypeName,
                    resId, PersoniumUnitConfig.getMasterToken(), "", HttpStatus.SC_OK);
            // レスポンスヘッダからEtag情報を取得する
            etag.put(resId, responseUnit.getHeader(HttpHeaders.ETAG));
        }

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata001_dynamicProperty2", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata001_dynamicProperty2')"));
        uri.put("userdata001_sample2", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata001_sample2')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata001_dynamicProperty2", additionalprop);
        additionalprop.put("__id", "userdata001_dynamicProperty2");
        additionalprop.put("test", "test1");
        additionalprop.put("truth", true);
        additionalprop.put("dynamicProperty", "dynamicPropertyValue2");
        additionalprop.put("decimal", 1.1);
        additionalprop.put("sample", "sample1");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 2, etag);
    }

    /**
     * UserDataに完全一致検索クエリのキーにIDを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーにIDを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=__id+eq+%27userdata006%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに完全一致検索クエリの値に真偽値で指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに完全一致検索クエリの値に真偽値で指定して対象のデータのみ取得できること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicPropertyBoolean", true);

        String userDataId2 = "userdata002";
        JSONObject body2 = new JSONObject();
        body2.put("__id", userDataId2);
        body2.put("dynamicPropertyBoolean", false);

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse res1 = createUserData(body, HttpStatus.SC_CREATED);
            TResponse res2 = createUserData(body2, HttpStatus.SC_CREATED);

            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            Map<String, String> etag = new HashMap<String, String>();
            etag.put("userdata001", res1.getHeader(HttpHeaders.ETAG));
            etag.put("userdata002", res2.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=dynamicPropertyBoolean+eq+true")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, entityTypeName
                    + "('" + userDataId + "')"));

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop);
            additionalprop.put("__id", userDataId);
            additionalprop.put("dynamicPropertyBoolean", true);

            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etag);
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
            deleteUserData(userDataId2);
            deleteDynamicProperty("dynamicPropertyBoolean");
        }
    }

    /**
     * UserDataに不一致検索クエリのキーにIDを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに不一致検索クエリのキーにIDを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        String query = "?$inlinecount=allpages";
        PersoniumResponse res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        int count = Integer.parseInt((String) ((JSONObject) json.get("d")).get("__count"));

        query = "?$filter=__id+ne+%27userdata006%27&$inlinecount=allpages";
        res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject jsonRes = res.bodyAsJson();
        // レスポンスボディーのサイズをチェック
        ODataCommon.checkResponseBodyCount(jsonRes, count - 1);
        // レスポンスボディのURIに、neで指定したUserODataが含まれないことをチェック
        String uri = UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')");
        ODataCommon.checkResponseUriNotExsists(jsonRes, uri);
    }

    /**
     * UserDataに不一致検索クエリのキーにnullを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに不一致検索クエリのキーにnullを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        String query = "?$inlinecount=allpages";
        PersoniumResponse res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        int count = Integer.parseInt((String) ((JSONObject) json.get("d")).get("__count"));

        query = "?$filter=number+ne+null&$inlinecount=allpages";
        res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject jsonRes = res.bodyAsJson();
        // レスポンスボディーのサイズをチェック
        ODataCommon.checkResponseBodyCount(jsonRes, count - 2);
        // レスポンスボディのURIに、neで指定したUserODataが含まれないことをチェック
        String uri = UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata001_dynamicProperty2')");
        ODataCommon.checkResponseUriNotExsists(jsonRes, uri);
        uri = UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_sample2')");
        ODataCommon.checkResponseUriNotExsists(jsonRes, uri);
    }

    /**
     * 不一致検索クエリとorderbyクエリが併用できること.
     */
    @Test
    public final void 不一致検索クエリとorderbyクエリが併用できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        String query = "?$inlinecount=allpages";
        PersoniumResponse res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        int count = Integer.parseInt((String) ((JSONObject) json.get("d")).get("__count"));

        query = "?$filter=__id+ne+%27userdata006%27&$orderby=__id%20desc&$inlinecount=allpages";
        res = UserDataUtils.listEntities(cellName, boxName, colName, sdEntityTypeName, query,
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        json = res.bodyAsJson();
        // レスポンスボディーのサイズをチェック
        ODataCommon.checkResponseBodyCount(json, count - 1);
        // レスポンスボディのURIに、neで指定したUserODataが含まれないことをチェック
        String uri = UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')");
        ODataCommon.checkResponseUriNotExsists(json, uri);
        // レスポンスボディーが降順に並んでいることのチェック
        ArrayList<String> urilist = new ArrayList<String>();
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata102')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata009')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata008')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata005')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata004')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata003')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata002')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_test2')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_sample2')"));
        urilist.add(UrlUtils
                .userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_dynamicProperty2')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001')"));
        urilist.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata000')"));
        ODataCommon.checkCommonResponseUri(json, urilist);

    }

    /**
     * UserDataに範囲検索クエリのキーにIDを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに範囲検索クエリのキーにIDを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=__id+gt+%27userdata006%27+and+__id+lt+%27userdata008%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);

        // ユーザデータの一覧取得
        response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=__id+ge+%27userdata006%27+and+__id+le+%27userdata008%27&\\$orderby=__id")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata008')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
    }

    /**
     * UserDataに前方一致検索クエリのキーにIDを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに前方一致検索クエリのキーにIDを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        String userDataId = "userdata0061";
        try {

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, colName, sdEntityTypeName);

            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            Map<String, String> etag = new HashMap<String, String>();
            etag.put("userdata061", response.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", sdEntityTypeName)
                    .with("query", "?\\$filter=startswith(__id,%27userdata006%27)&\\$orderby=__id")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata0061')"));

            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);

        } finally {
            deleteUserData(cellName, boxName, colName, sdEntityTypeName, userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * UserDataに部分一致検索クエリのキーにIDを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに部分一致検索クエリのキーにIDを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";

        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof(%27userdata006%27,__id)")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        ArrayList<String> uri = new ArrayList<String>();
        uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));

        ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);

    }

    /**
     * UserDataに完全一致検索クエリのキーにpublishedを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに完全一致検索クエリのキーにpublishedを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        String userDataId = "newData001";

        try {
            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, colName,
                    sdEntityTypeName);

            // 作成日を取得
            String date = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results"))
                    .get("__published");

            // ユーザデータの一覧取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", sdEntityTypeName)
                    .with("query", "?\\$filter=__published+eq+" + date.substring(6, 19))
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('newData001')"));

            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
        } finally {
            deleteUserData(cellName, boxName, colName, sdEntityTypeName, userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * UserDataに範囲検索クエリのキーにpublishedを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに範囲検索クエリのキーにpublishedを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        String userDataId = "newData001";

        try {
            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, colName, sdEntityTypeName);

            // 作成日を取得
            String dateStr = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results"))
                    .get("__published");
            long date = parseDateStringToLong(dateStr);

            // ユーザデータの一覧取得
            PersoniumResponse searchResponse = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                    "?$filter=__published+ge+" + date + "+and+__published+le+" + (date + 1000));

            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            // レスポンスボディーのチェック
            // URI
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('newData001')"));

            // ユーザデータの一覧取得
            searchResponse = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                    "?$filter=__published+gt+" + (date - 1000) + "+and+__published+lt+" + (date + 1000));

            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            ODataCommon.checkCommonResponseUri(searchResponse.bodyAsJson(), uri);
        } finally {
            deleteUserData(cellName, boxName, colName, sdEntityTypeName, userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * UserDataに完全一致検索クエリのキーにupdatedを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに完全一致検索クエリのキーにupdatedを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        String userDataId = "newData001";

        try {
            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED, cellName, boxName, colName,
                    sdEntityTypeName);

            // 作成日を取得
            String date = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results"))
                    .get("__updated");

            // ユーザデータの一覧取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", sdEntityTypeName)
                    .with("query", "?\\$filter=__updated+eq+" + date.substring(6, 19))
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            // URI
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('newData001')"));

            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
        } finally {
            deleteUserData(cellName, boxName, colName, sdEntityTypeName, userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteDynamicProperty("dynamicProperty");
        }

    }

    /**
     * UserDataに範囲検索クエリのキーにupdatedを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに範囲検索クエリのキーにupdatedを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        String userDataId = "newData001";

        try {
            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("dynamicProperty", "dynamicPropertyValue");
            TResponse response = createUserData(body, HttpStatus.SC_CREATED,
                    cellName, boxName, colName, sdEntityTypeName);

            // 更新日を取得
            String dateStr = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results"))
                    .get("__updated");
            long date = parseDateStringToLong(dateStr);

            // ユーザデータの一覧取得
            PersoniumResponse searchResponse = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                    "?$filter=__updated+ge+" + date + "+and+__updated+le+" + (date + 1000));

            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            // レスポンスボディーのチェック
            // URI
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('newData001')"));

            // ユーザデータの一覧取得
            searchResponse = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                    "?$filter=__updated+gt+" + (date - 1000) + "+and+__updated+lt+" + (date + 1000));

            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            ODataCommon.checkCommonResponseUri(searchResponse.bodyAsJson(), uri);
        } finally {
            deleteUserData(cellName, boxName, colName, sdEntityTypeName, userDataId, PersoniumUnitConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * UserDataに整数型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに整数型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=number+ge+5+and+number+lt+6&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata005')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("test", "test5");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);
        additionalprop.put("sample", "sample5");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, null);
    }

    /**
     * UserDataに小数型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに小数型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=decimal+gt+8.1+or+decimal+le+0.1&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata000", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata000')"));
        uri.put("userdata009", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata009')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata000", additionalprop);
        additionalprop.put("__id", "userdata000");
        additionalprop.put("test", "test0");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue0");
        additionalprop.put("number", 0);
        additionalprop.put("decimal", 0.1);
        additionalprop.put("sample", "sample0");
        additionalprop = new HashMap<String, Object>();
        additional.put("userdata009", additionalprop);
        additionalprop.put("__id", "userdata009");
        additionalprop.put("test", "test9");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue9");
        additionalprop.put("number", 9);
        additionalprop.put("decimal", 9.1);
        additionalprop.put("sample", "sample9");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 2, null);
    }

    /**
     * UserDataに文字列型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに文字列型のPropertyに範囲検索クエリを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+ge+'test5'+and+test+le+'test6'&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata005')"));
        uri.put("userdata006", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata006')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("test", "test5");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);
        additionalprop.put("sample", "sample5");
        additionalprop = new HashMap<String, Object>();
        additional.put("userdata006", additionalprop);
        additionalprop.put("__id", "userdata006");
        additionalprop.put("test", "test6");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue6");
        additionalprop.put("number", 6);
        additionalprop.put("decimal", 6.1);
        additionalprop.put("sample", "sample6");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 2, null);
    }

    /**
     * UserDataに異なるPropertyを範囲検索クエリに指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに異なるPropertyを範囲検索クエリに指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+gt+'test7'+and+number+lt+9&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata008", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('userdata008')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata008", additionalprop);
        additionalprop.put("__id", "userdata008");
        additionalprop.put("test", "test8");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue8");
        additionalprop.put("number", 8);
        additionalprop.put("decimal", 8.1);
        additionalprop.put("sample", "sample8");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", 1, null);
    }

    /**
     * UserDataに範囲検索クエリの右辺値にNullを指定した場合_400エラーとなること.
     */
    @Test
    public final void UserDataに範囲検索クエリの右辺値にNullを指定した場合_400エラーとなること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+ge+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+gt+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+le+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+lt+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに範囲検索クエリの右辺値にNullを指定した場合_400エラーとなること.
     */
    @Test
    public final void UserDataに範囲検索クエリの左辺値に存在しないPropertyを指定した場合_400エラーとなること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=tes+ge+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=tes+gt+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=tes+le+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=tes+lt+null&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに範囲検索クエリの右辺値を指定しない場合_400エラーとなること.
     */
    @Test
    public final void UserDataに範囲検索クエリの右辺値を指定しない場合_400エラーとなること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=test+ge&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataにand検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにand検索クエリを指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue1"
                        + "%27+and+sample+eq+%27sample1%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001')"));
        uri.put("userdata001_test2",
                UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_test2')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata001", additionalprop);
        additionalprop.put("__id", "userdata001");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test1");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata001_test2", additionalprop);
        additionalprop.put("__id", "userdata001_test2");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test2");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataにand検索クエリを複数指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにand検索クエリを複数指定して対象のデータのみ取得できること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue1"
                        + "%27+and+sample+eq+%27sample1%27+and+test+eq+%27test1%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        String userDataId = "userdata001";
        Map<String, String> uri = new HashMap<String, String>();
        uri.put(userDataId, UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName
                + "('" + userDataId + "')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put(userDataId, additionalprop);
        additionalprop.put("__id", userDataId);
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test1");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataにand検索クエリに日本語をクエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにand検索クエリに日本語を指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        PersoniumResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=japanese+eq+%27部分一致検索テスト%27"
                        + "+and+japanese+eq+%27部分一致検索漢字のテスト%27");

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null, "__id");
    }

    /**
     * UserDataにand検索クエリを左辺値のみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataにand検索クエリを左辺値のみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue1%27+and")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataにand検索クエリを右辺値のみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataにand検索クエリを右辺値のみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=and+dynamicProperty+eq+%27dynamicPropertyValue1%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataにor検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにor検索クエリを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue5%27"
                        + "+or+sample+eq+%27sample7%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata005')"));
        uri.put("userdata007", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("sample", "sample5");
        additionalprop.put("test", "test5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata007", additionalprop);
        additionalprop.put("__id", "userdata007");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue7");
        additionalprop.put("sample", "sample7");
        additionalprop.put("test", "test7");
        additionalprop.put("number", 7);
        additionalprop.put("decimal", 7.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataにor検索クエリを複数指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにor検索クエリを複数指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue5%27"
                        + "+or+sample+eq+%27sample7%27+or+test+eq+%27test9%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata005", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata005')"));
        uri.put("userdata007", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata007')"));
        uri.put("userdata009", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata009')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata005", additionalprop);
        additionalprop.put("__id", "userdata005");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue5");
        additionalprop.put("sample", "sample5");
        additionalprop.put("test", "test5");
        additionalprop.put("number", 5);
        additionalprop.put("decimal", 5.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata007", additionalprop);
        additionalprop.put("__id", "userdata007");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue7");
        additionalprop.put("sample", "sample7");
        additionalprop.put("test", "test7");
        additionalprop.put("number", 7);
        additionalprop.put("decimal", 7.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata009", additionalprop);
        additionalprop.put("__id", "userdata009");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue9");
        additionalprop.put("sample", "sample9");
        additionalprop.put("test", "test9");
        additionalprop.put("number", 9);
        additionalprop.put("decimal", 9.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataにor検索クエリに日本語をクエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataにor検索クエリに日本語を指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        PersoniumResponse response = getUserDataWithDcClient(cellName, boxName, colName, sdEntityTypeName,
                "?$filter=japanese+eq+%27部分一致検索テスト%27"
                        + "+or+japanese+eq+%27部分一致検索漢字のテスト%27");

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata100", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata100')"));
        uri.put("userdata101", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata101')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata100", additionalprop);
        additionalprop.put("__id", "userdata100");
        additionalprop.put("test", "atest");
        additionalprop.put("japanese", "部分一致検索テスト");
        additionalprop.put("english", "Search substringof Test");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata101", additionalprop);
        additionalprop.put("__id", "userdata101");
        additionalprop.put("test", "btest");
        additionalprop.put("japanese", "部分一致検索漢字のテスト");
        additionalprop.put("english", "Test Substringof Search value");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataにor検索クエリを左辺値のみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataにor検索クエリを左辺値のみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=dynamicProperty+eq+%27dynamicPropertyValue1%27+or")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataにor検索クエリを右辺値のみ指定してステータスコード400が返却されること.
     */
    @Test
    public final void UserDataにor検索クエリを右辺値のみ指定してステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$filter=or+dynamicProperty+eq+%27dynamicPropertyValue1%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに括弧ありで検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに括弧ありで検索クエリを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=sample+eq+%27sample6%27"
                        + "+and+(dynamicProperty+eq+%27dynamicPropertyValue1%27"
                        + "+or+test+eq+%27test2%27+or+test+eq+%27test6%27)")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata006", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata006", additionalprop);
        additionalprop.put("__id", "userdata006");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue6");
        additionalprop.put("sample", "sample6");
        additionalprop.put("test", "test6");
        additionalprop.put("number", 6);
        additionalprop.put("decimal", 6.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに括弧が複数ありで検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに括弧が複数ありで検索クエリを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=(sample+eq+%27sample1%27"
                        + "+and+dynamicProperty+eq+%27dynamicPropertyValue1%27)"
                        + "+and+(test+eq+%27test1%27+or+sample+eq+%27sample1%27)")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata001", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001')"));
        uri.put("userdata001_test2",
                UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_test2')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata001", additionalprop);
        additionalprop.put("__id", "userdata001");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test1");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata001_test2", additionalprop);
        additionalprop.put("__id", "userdata001_test2");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test2");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに括弧が2階層で検索クエリを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに括弧が2階層で検索クエリを指定して対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=sample+eq+%27sample6%27"
                        + "+and+(dynamicProperty+eq+%27dynamicPropertyValue1%27"
                        + "+or+(test+eq+%27test6%27+or+sample+eq+%27sample2%27))")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata006", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata006')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata006", additionalprop);
        additionalprop.put("__id", "userdata006");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue6");
        additionalprop.put("sample", "sample6");
        additionalprop.put("test", "test6");
        additionalprop.put("number", 6);
        additionalprop.put("decimal", 6.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * filterクエリに複数の括弧を指定して対象のデータのみ取得できること.
     */
    @Test
    public final void filterクエリに複数の括弧を指定して対象のデータのみ取得できること() {
        final String entity = "filterlist";
        String query = "?\\$filter=%28substringof%28%27string%27%2Cstring%29+and+%28boolean+eq+false"
                + "%29%29+or+%28int32+le+5000%29&\\$inlinecount=allpages&\\$orderby=__id+asc";
        TResponse res = UserDataUtils.list(Setup.TEST_CELL_FILTER, "box", "odata", entity,
                query, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        // レスポンスボディーが昇順に並んでいることのチェック
        JSONObject json = res.bodyAsJson();
        ArrayList<String> urilist = new ArrayList<String>();
        int[] ids = {0, 1, 2, 3, 4, 5, 7, 9 };
        for (int idx : ids) {
            urilist.add(UrlUtils.userData(Setup.TEST_CELL_FILTER, "box", "odata",
                    entity + String.format("('id_%04d')", idx)));
        }
        ODataCommon.checkCommonResponseUri(json, urilist);

    }

    /**
     * filterクエリに複数の括弧を指定してフィルタリング後に特定のプロパティのみが取得できること.
     */
    @Test
    public final void filterクエリに複数の括弧を指定してフィルタリング後に特定のプロパティのみが取得できること() {
        final String entity = "filterlist";
        String query = "?\\$top=3&\\$skip=4&\\$select=datetime&"
                + "\\$filter=%28substringof%28%27string%27%2Cstring%29+and+%28boolean+eq+false"
                + "%29%29+or+%28int32+le+5000%29&\\$inlinecount=allpages&\\$orderby=__id+asc";
        TResponse res = UserDataUtils.list(Setup.TEST_CELL_FILTER, "box", "odata", entity,
                query, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        // レスポンスボディーが昇順に並んでいることのチェック
        JSONObject jsonResp = res.bodyAsJson();
        ArrayList<String> urilist = new ArrayList<String>();
        int[] ids = {4, 5, 7 };
        for (int idx : ids) {
            urilist.add(UrlUtils.userData(Setup.TEST_CELL_FILTER, "box", "odata",
                    entity + String.format("('id_%04d')", idx)));
        }
        ODataCommon.checkCommonResponseUri(jsonResp, urilist);
        JSONArray array = (JSONArray) ((JSONObject) jsonResp.get("d")).get("results");
        for (Object element : array) {
            JSONObject json = (JSONObject) element;
            assertTrue(json.containsKey("datetime"));
            assertFalse(json.containsKey("int32"));

        }
    }

    /**
     * UserDataに括弧が片方のみで検索クエリを指定された場合_片括弧が無視されて対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに括弧が片方のみで検索クエリを指定された場合_片括弧が無視されて対象のデータのみ取得できること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=sample+eq+%27sample6%27"
                        + "+and+(dynamicProperty+eq+%27dynamicPropertyValue1%27"
                        + "+or+test+eq+%27test2%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();

        // レスポンスヘッダーのチェック
        ODataCommon.checkCommonResponseHeader(response);

        // レスポンスボディーのチェック
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdata002", UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata002')"));
        uri.put("userdata001_test2",
                UrlUtils.userData(cellName, boxName, colName, sdEntityTypeName + "('userdata001_test2')"));

        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdata002", additionalprop);
        additionalprop.put("__id", "userdata002");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue2");
        additionalprop.put("sample", "sample2");
        additionalprop.put("test", "test2");
        additionalprop.put("number", 2);
        additionalprop.put("decimal", 2.1);

        additionalprop = new HashMap<String, Object>();
        additional.put("userdata001_test2", additionalprop);
        additionalprop.put("__id", "userdata001_test2");
        additionalprop.put("dynamicProperty", "dynamicPropertyValue1");
        additionalprop.put("sample", "sample1");
        additionalprop.put("test", "test2");
        additionalprop.put("number", 1);
        additionalprop.put("decimal", 1.1);

        String nameSpace = getNameSpace(sdEntityTypeName);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
    }

    /**
     * UserDataに検索クエリの対象Keyに受付可能な記号を指定した場合対象データのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataに検索クエリの対象Keyに受付可能な記号を指定した場合対象データのみ取得できること() {
        try {
            // ユーザデータの作成
            // 作成したユーザデータのレスポンスヘッダからEtagを取得する
            // リクエストボディを設定
            JSONObject body = new JSONObject();
            body.put("__id", userDataId201);
            body.put("name", "pochi");
            body.put("na-me", "po-chi");
            body.put("na_me", "po_chi");
            createUserData(body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("__id", userDataId202);
            body2.put("name", "tama");
            body2.put("na-me", "ta-ma");
            body2.put("na_me", "ta_ma");
            createUserData(body2, HttpStatus.SC_CREATED);

            // ユーザデータの一覧取得(filterクエリに「-」指定)
            String sdEntityTypeName = "Category";
            TResponse response = userDataListWithFilter(sdEntityTypeName,
                    "?\\$filter=na-me+eq+%27po-chi%27", HttpStatus.SC_OK);
            checkResponse(response, 1);

            // ユーザデータの一覧取得(filterクエリに「_」指定)
            response = userDataListWithFilter(sdEntityTypeName, "?\\$filter=na_me+eq+%27ta_ma%27", HttpStatus.SC_OK);
            checkResponse(response, 1);
        } finally {
            deleteUserData(userDataId201);
            deleteUserData(userDataId202);
            deleteDynamicProperty("name");
            deleteDynamicProperty("na-me");
            deleteDynamicProperty("na_me");
        }
    }

    /**
     * UserDataに検索クエリの対象Keyに受付不可能な記号を指定した場合400エラーとなること.
     */
    @Test
    public final void UserDataに検索クエリの対象Keyに受付不可能な記号を指定した場合400エラーとなること() {
        String sdEntityTypeName = "Category";
        // ユーザデータの一覧取得(filterクエリに「.」指定)
        userDataListWithFilter(sdEntityTypeName,
                "?\\$filter=na.me+eq+%27pochi%27", HttpStatus.SC_BAD_REQUEST);

        // ユーザデータの一覧取得(filterクエリに「~」指定)
        userDataListWithFilter(sdEntityTypeName,
                "?\\$filter=na~me+eq+%27pochi%27", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * 完全一致検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 完全一致検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(4096);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=dynamicProperty+eq+%27" + value + "%27&$inlinecount=allpages");

            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 完全一致検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと. <br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void 完全一致検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(4097);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=dynamicProperty+eq+%27" + value + "%27&$inlinecount=allpages");

            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 範囲検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること.
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void 範囲検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること() {
        String userDataId = "userdata001";
        int queryLength = 4096;

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(queryLength);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得 gt
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=dynamicProperty+gt+%27" + UserDataUtils.createString(queryLength - 1) + "0"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);

            // ユーザデータの一覧取得 ge
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+ge+%27" + UserDataUtils.createString(queryLength - 1) + "0"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);

            // ユーザデータの一覧取得 lt
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+lt+%27" + UserDataUtils.createString(queryLength - 1) + "9"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);

            // ユーザデータの一覧取得 le
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+le+%27" + UserDataUtils.createString(queryLength - 1) + "9"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 範囲検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと.<br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void 範囲検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと() {
        String userDataId = "userdata001";
        int queryLength = 4097;

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(queryLength);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得 gt
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=dynamicProperty+gt+%27" + UserDataUtils.createString(queryLength - 1) + "0"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);

            // ユーザデータの一覧取得 ge
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+ge+%27" + UserDataUtils.createString(queryLength - 1) + "0"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);

            // ユーザデータの一覧取得 lt
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+lt+%27" + UserDataUtils.createString(queryLength - 1) + "9"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);

            // ユーザデータの一覧取得 le
            response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName, entityType,
                    "?$filter=dynamicProperty+le+%27" + UserDataUtils.createString(queryLength - 1) + "9"
                            + "%27&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 前方一致検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 前方一致検索クエリの値に4096文字の文字列を指定して対象のデータを取得できること() {
        String userDataId = "userdata001";
        int queryLength = 4096;

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(queryLength);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得 startswith
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=startswith(dynamicProperty,%27" + value + "%27)&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 1);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * 前方一致検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと.<br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void 前方一致検索クエリの値に4097文字の文字列を指定すると対象のデータを取得できないこと() {
        String userDataId = "userdata001";
        int queryLength = 4097;

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(queryLength);
        body.put("dynamicProperty", value);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityType = "Category";

        try {
            UserDataUtils.create(Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityType);

            // ユーザデータの一覧取得 startswith
            PersoniumResponse response = UserDataListFilterTest.getUserDataWithDcClient(cellName, boxName, colName,
                    entityType,
                    "?$filter=startswith(dynamicProperty,%27" + value + "%27)&$inlinecount=allpages");
            // ヒット数のチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), 0);
        } finally {
            UserDataUtils.delete(Setup.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType, userDataId);
            deleteDynamicProperty("dynamicProperty");
        }
    }

    /**
     * filterクエリを２つ指定した場合に先に指定した検索条件でデータが取得できること.
     */
    @Test
    public final void filterクエリを２つ指定した場合に先に指定した検索条件でデータが取得できること() {
        final String entity = "filterlist";
        String query = "?\\$filter=double+gt+5555555.555555&\\$filter=double+le+5555555.555555"
                + "&\\$inlinecount=allpages&\\$orderby=__id+asc";
        TResponse res = UserDataUtils.list(Setup.TEST_CELL_FILTER, "box", "odata", entity,
                query, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        // レスポンスボディーが昇順に並んでいることのチェック
        JSONObject jsonResp = res.bodyAsJson();
        ArrayList<String> urilist = new ArrayList<String>();
        int[] ids = {5, 6, 7, 8, 9 };
        for (int idx : ids) {
            urilist.add(UrlUtils.userData(Setup.TEST_CELL_FILTER, "box", "odata",
                    entity + String.format("('id_%04d')", idx)));
        }
        ODataCommon.checkCommonResponseUri(jsonResp, urilist);
    }

    /**
     * qクエリとfilterクエリを指定した場合にand条件で検索したデータが取得できること.
     */
    @Test
    public final void qクエリとfilterクエリを指定した場合にand条件で検索したデータが取得できること() {
        final String entity = "filterlist";
        String query = "?q=string+0001&\\$filter=double+gt+5555555.555555"
                + "&\\$inlinecount=allpages&\\$orderby=__id+asc";
        TResponse res = UserDataUtils.list(Setup.TEST_CELL_FILTER, "box", "odata", entity,
                query, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        // レスポンスボディーが昇順に並んでいることのチェック
        JSONObject jsonResp = res.bodyAsJson();
        ArrayList<String> urilist = new ArrayList<String>();
        int[] ids = {5, 6, 7, 8, 9 };
        for (int idx : ids) {
            urilist.add(UrlUtils.userData(Setup.TEST_CELL_FILTER, "box", "odata",
                    entity + String.format("('id_%04d')", idx)));
        }
        // TODO qクエリの結果として "string 0001" が or検索しているため、検索クエリを修正する必要あり。
        ODataCommon.checkCommonResponseUri(jsonResp, urilist);
    }


    private TResponse userDataListWithFilter(String sdEntityTypeName, String query, int code) {
        return Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .returns()
                .statusCode(code)
                .debug();
    }

    private void checkResponse(TResponse response, int expectListSize) {
        JSONObject json = response.bodyAsJson();

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        // 取得件数のチェック
        assertEquals(expectListSize, results.size());
    }

    /**
     * personium.ioの日付形式の文字列を、long型に変換する.<br />
     * 例) /Date(1385360971716)/ から 1385360971716 に変換。
     * @param dateStr personium.ioの日付形式の文字列
     * @return long型の日付
     */
    public static long parseDateStringToLong(String dateStr) {
        Pattern pattern = Pattern.compile("/Date\\(([0-9]+)\\)/");
        Matcher m = pattern.matcher(dateStr);
        m.matches();
        long date = Long.parseLong(m.group(1));
        return date;
    }

    /**
     * 動的Propertyを削除する.
     * 動的Propertyは様々なデータ型で作られるため、テスト後に必ず削除しておく必要がある。
     * @param propertyName
     */
    private void deleteDynamicProperty(String propertyName) {
        String resourceUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propertyName, entityTypeName);
        ODataCommon.deleteOdataResource(resourceUrl);
    }

}

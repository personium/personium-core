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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataのBoolean型への検索テスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListFilterBooleanTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataListFilterBooleanTest() {
        super();
    }

    /**
     * UserDataに完全一致検索クエリのキーを真偽値trueで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーを真偽値trueで指定して対象のデータのみ取得できること() {
        String entityTypeName = "boolFilterTest";
        try {
            // Boolean検索のテスト用データを作成する
            createTestData(entityTypeName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=bool+eq+true&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが1件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("1", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("boolTrue", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(true, (Boolean) ((JSONObject) results.get(0)).get("bool"));
        } finally {
            deleteTestData(entityTypeName);
        }
    }

    /**
     * UserDataに完全一致検索クエリのキーを真偽値falseで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーを真偽値falseで指定して対象のデータのみ取得できること() {
        String entityTypeName = "boolFilterTest";
        try {
            // Boolean検索のテスト用データを作成する
            createTestData(entityTypeName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=bool+eq+false&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが1件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("1", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("boolFalse", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(false, (Boolean) ((JSONObject) results.get(0)).get("bool"));
        } finally {
            deleteTestData(entityTypeName);
        }
    }

    /**
     * UserDataに完全一致検索クエリのキーにnullを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーにnullを指定して対象のデータのみ取得できること() {
        String entityTypeName = "boolFilterTest";
        try {
            // Boolean検索のテスト用データを作成する
            createTestData(entityTypeName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=bool+eq+null&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが1件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("1", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("boolNull", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(null, (Boolean) ((JSONObject) results.get(0)).get("bool"));
        } finally {
            deleteTestData(entityTypeName);
        }
    }

    /**
     * UserDataに完全一致検索クエリのキーに文字列trueを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーに文字列trueを指定した場合ステータスコード400が返却されること() {
        // 取得対象のデータに対する検索を実施する
        // ユーザデータの一覧取得
        String entityTypeName = "SalesDetail";
        String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                + "?$filter=truth+eq+%27true%27&$inlinecount=allpages";
        DcRequest req = DcRequest.get(searchRequestUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse searchResponse = request(req);
        assertEquals(HttpStatus.SC_BAD_REQUEST, searchResponse.getStatusCode());
    }

    /**
     * UserDataに完全一致検索クエリのキーに文字列falseを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーに文字列falseを指定した場合ステータスコード400が返却されること() {
        // 取得対象のデータに対する検索を実施する
        // ユーザデータの一覧取得
        String entityTypeName = "SalesDetail";
        String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                + "?$filter=truth+eq+%27false%27&$inlinecount=allpages";
        DcRequest req = DcRequest.get(searchRequestUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse searchResponse = request(req);
        assertEquals(HttpStatus.SC_BAD_REQUEST, searchResponse.getStatusCode());
    }

    /**
     * UserDataに完全一致検索クエリのキーに文字列nullを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーに文字列nullを指定した場合ステータスコード400が返却されること() {
        // 取得対象のデータに対する検索を実施する
        // ユーザデータの一覧取得
        String entityTypeName = "SalesDetail";
        String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                + "?$filter=truth+eq+%27null%27&$inlinecount=allpages";
        DcRequest req = DcRequest.get(searchRequestUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse searchResponse = request(req);
        assertEquals(HttpStatus.SC_BAD_REQUEST, searchResponse.getStatusCode());
    }

    /**
     * UserDataに完全一致検索クエリのキーに文字列hogeを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに完全一致検索クエリのキーに文字列hogeを指定した場合ステータスコード400が返却されること() {
        // 取得対象のデータに対する検索を実施する
        // ユーザデータの一覧取得
        String entityTypeName = "SalesDetail";
        String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                + "?$filter=truth+eq+%27hoge%27&$inlinecount=allpages";
        DcRequest req = DcRequest.get(searchRequestUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse searchResponse = request(req);
        assertEquals(HttpStatus.SC_BAD_REQUEST, searchResponse.getStatusCode());
    }

    /**
     * Boolean検索のテスト用データを作成する.
     * @param entityTypeName エンティティタイプ名
     */
    @SuppressWarnings("unchecked")
    protected void createTestData(String entityTypeName) {
        // Booleanの検索のテスト用エンティティタイプ作成
        EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                HttpStatus.SC_CREATED);

        // 真偽値が true / false / null のデータを作成
        JSONObject body = new JSONObject();
        body.put("__id", "boolTrue");
        body.put("bool", true);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "boolFalse");
        body.put("bool", false);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "boolNull");
        body.put("bool", null);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);
    }

    /**
     * Boolean検索のテスト用データを削除する.
     * @param entityTypeName エンティティタイプ名
     */
    protected void deleteTestData(String entityTypeName) {
        // UserODataの削除
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "boolTrue", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "boolFalse", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "boolNull", colName);

        // EntityTypeの削除
        EntityTypeUtils.delete(colName, DcCoreConfig.getMasterToken(),
                MediaType.APPLICATION_JSON, entityTypeName, boxName, cellName, -1);
    }

    /**
     * UserDataにboolean型のPropertyに範囲検索クエリを指定した場合_400エラーとなること.
     */
    @Test
    public final void UserDataにboolean型のPropertyに範囲検索クエリを指定した場合_400エラーとなること() {
        String sdEntityTypeName = "SalesDetail";
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=truth+ge+true&\\$inlinecount=allpages")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリに真偽値trueを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに真偽値trueを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28truth%2ctrue%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに前方一致検索クエリに真偽値falseを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに前方一致検索クエリに真偽値falseを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=startswith%28truth%2cfalse%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに真偽値trueを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに真偽値trueを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28true%2ctruth%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * UserDataに部分一致検索クエリに真偽値falseを指定した場合ステータスコード400が返却されること.
     */
    @Test
    public final void UserDataに部分一致検索クエリに真偽値falseを指定した場合ステータスコード400が返却されること() {
        // ユーザデータの一覧取得
        String sdEntityTypeName = "SalesDetail";

        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", sdEntityTypeName)
                .with("query", "?\\$filter=substringof%28false%2ctruth%29")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

}

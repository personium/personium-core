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
import static org.junit.Assert.assertNull;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataのDouble型への検索テスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListDeclaredDoubleTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataListDeclaredDoubleTest() {
        super();
    }

    /**
     * UserDataに$filterの完全一致検索クエリのキーをDoubleで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに$filterの完全一致検索クエリのキーをDoubleで指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+12345.12345&$inlinecount=allpages";
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
            assertEquals("double", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), (Double) ((JSONObject) results.get(0)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに整数値をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに整数値をで指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+1&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(1)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに小数値(1.0)をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに小数値をで指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+1.0&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(1)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+gt+1.0&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが3件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("3", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("double", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleMax", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleString", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Double.valueOf(1.2345), ((JSONObject) results.get(2)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの検索クエリleを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの検索クエリleを指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+le+0&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleMin", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(-1.79e308), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleZero", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(0), ((JSONObject) results.get(1)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリにnullを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリにnullを指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+null&$inlinecount=allpages&$orderby=__id";
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
            assertEquals("doubleNull", (String) ((JSONObject) results.get(0)).get("__id"));
            assertNull(((JSONObject) results.get(0)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最大値を指定して対象のデータのみ取得できること.
     */
    @Test
    @Ignore
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最大値を指定して対象のデータのみ取得できること() {
        // TODO #36625のチケットにて対応後、@ignoreを外すこと
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+1.79E308&$inlinecount=allpages&$orderby=__id";
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
            assertEquals("doubleMax", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), ((JSONObject) results.get(1)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最小値を指定して対象のデータのみ取得できること.
     */
    @Test
    @Ignore
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最小値を指定して対象のデータのみ取得できること() {
        // TODO #36625のチケットにて対応後、@ignoreを外すこと

        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+--1.79E308&$inlinecount=allpages&$orderby=__id";
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
            assertEquals("doubleMin", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), ((JSONObject) results.get(1)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの完全一致検索クエリで対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの完全一致検索クエリで対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Edm.Int32のユーザODataを登録
            JSONObject body = new JSONObject();
            body.put("__id", "doubleInt");
            body.put(propertyName, 1);
            UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, entityTypeName);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+eq+1.0&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが3件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("3", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(0)).get(propertyName));

            // Edm.Int32からEdm.Doubleへ変更したユーザDataが検索できていることを確認
            assertEquals("doubleInt", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(2)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Edm.Int32のユーザODataを登録
            JSONObject body = new JSONObject();
            body.put("__id", "doubleInt");
            body.put(propertyName, 2);
            UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, entityTypeName);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$filter=" + propertyName + "+gt+1.234&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが4件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("4", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("double", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), ((JSONObject) results.get(0)).get(propertyName));

            // Edm.Int32からEdm.Doubleへ変更したユーザDataが検索できていることを確認
            assertEquals("doubleInt", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(2), ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleMax", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), ((JSONObject) results.get(2)).get(propertyName));

            assertEquals("doubleString", (String) ((JSONObject) results.get(3)).get("__id"));
            assertEquals(Double.valueOf(1.2345), ((JSONObject) results.get(3)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm.Doubleのプロパティに対して$orderbyクエリ(asc)を指定して正しい順序でデータが取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$orderbyクエリ_ascを指定して正しい順序でデータが取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$orderby=" + propertyName + "+asc,__id&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが8件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("8", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            results = skipNullResults(results, propertyName);

            assertEquals("doubleMin", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), (Double) ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleZero", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(0), ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(2)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(3)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(3)).get(propertyName));

            assertEquals("doubleString", (String) ((JSONObject) results.get(4)).get("__id"));
            assertEquals(Double.valueOf(1.2345), (Double) ((JSONObject) results.get(4)).get(propertyName));

            assertEquals("double", (String) ((JSONObject) results.get(5)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), (Double) ((JSONObject) results.get(5)).get(propertyName));

            assertEquals("doubleMax", (String) ((JSONObject) results.get(6)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), (Double) ((JSONObject) results.get(6)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm.Doubleのプロパティに対して$orderbyクエリ(desc)を指定して正しい順序でデータが取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$orderbyクエリ_descを指定して正しい順序でデータが取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$orderby=" + propertyName + "+desc,__id&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが8件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("8", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            results = skipNullResults(results, propertyName);

            assertEquals("doubleMax", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), (Double) ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("double", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), (Double) ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleString", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Double.valueOf(1.2345), (Double) ((JSONObject) results.get(2)).get(propertyName));

            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(3)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(3)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(4)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(4)).get(propertyName));

            assertEquals("doubleZero", (String) ((JSONObject) results.get(5)).get("__id"));
            assertEquals(Long.valueOf(0), ((JSONObject) results.get(5)).get(propertyName));

            assertEquals("doubleMin", (String) ((JSONObject) results.get(6)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), (Double) ((JSONObject) results.get(6)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$orderbyクエリを指定して正しい順序で取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$orderbyクエリを指定して正しい順序で取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Edm.Int32のユーザODataを登録
            JSONObject body = new JSONObject();
            body.put("__id", "doubleInt");
            body.put(propertyName, 1);
            UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, entityTypeName);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?$orderby=" + propertyName + ",__id&$inlinecount=allpages";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが9件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("9", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            results = skipNullResults(results, propertyName);

            assertEquals("doubleMin", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), (Double) ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleZero", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(0), ((JSONObject) results.get(1)).get(propertyName));

            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(2)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(2)).get(propertyName));

            assertEquals("doubleInt", (String) ((JSONObject) results.get(3)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(3)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(4)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(4)).get(propertyName));

            assertEquals("doubleString", (String) ((JSONObject) results.get(5)).get("__id"));
            assertEquals(Double.valueOf(1.2345), (Double) ((JSONObject) results.get(5)).get(propertyName));

            assertEquals("double", (String) ((JSONObject) results.get(6)).get("__id"));
            assertEquals(Double.valueOf(12345.12345), (Double) ((JSONObject) results.get(6)).get(propertyName));

            assertEquals("doubleMax", (String) ((JSONObject) results.get(7)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), (Double) ((JSONObject) results.get(7)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対してqクエリのキーに最大値をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対してqクエリのキーに最大値をで指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?q=1.79E308&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleMax", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleMin", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), ((JSONObject) results.get(1)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対してqクエリのキーに最小値をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対してqクエリのキーに最小値をで指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?q=-1.79E308&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleMax", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Double.valueOf(1.79E308), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleMin", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Double.valueOf(-1.79E308), ((JSONObject) results.get(1)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対してqクエリ整数値を指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対してqクエリ整数値を指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Edm.Int32のユーザODataを登録
            JSONObject body = new JSONObject();
            body.put("__id", "doubleInt");
            body.put(propertyName, 1);
            UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, entityTypeName);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?q=1&$inlinecount=allpages&$orderby=__id";
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
            // Edm.Int32からEdm.Doubleへ変更したユーザDataが検索できていることを確認
            assertEquals("doubleInt", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(0)).get(propertyName));
        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対してqクエリ整数値を指定して対象のデータのみ取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対してqクエリ小数値を指定して対象のデータのみ取得できること() {
        String entityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, entityTypeName,
                    HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Edm.Int32のユーザODataを登録
            JSONObject body = new JSONObject();
            body.put("__id", "doubleInt");
            body.put(propertyName, 1);
            UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, entityTypeName);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    entityTypeName, propertyName,
                    entityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(entityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userData(cellName, boxName, colName, entityTypeName)
                    + "?q=1.0&$inlinecount=allpages&$orderby=__id";
            DcRequest req = DcRequest.get(searchRequestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse searchResponse = request(req);
            assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
            JSONObject responseBody = searchResponse.bodyAsJson();

            // ヒットしたデータが2件であることを確認する
            String count = (String) ((JSONObject) responseBody.get("d")).get("__count");
            assertEquals("2", count);

            // 期待したデータが取得できたことを確認する
            // Edm.Int32からEdm.Doubleへ変更したユーザDataが検索できないことを確認
            JSONArray results = (JSONArray) ((JSONObject) responseBody.get("d")).get("results");
            assertEquals("doubleDecimal", (String) ((JSONObject) results.get(0)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(0)).get(propertyName));

            assertEquals("doubleNatural", (String) ((JSONObject) results.get(1)).get("__id"));
            assertEquals(Long.valueOf(1), ((JSONObject) results.get(1)).get(propertyName));

        } finally {
            deleteTestData(entityTypeName, propertyName);
        }
    }

    /**
     * Double検索のテスト用データを作成する.
     * @param entityTypeName エンティティタイプ名
     * @param propertyName プロパティ名
     */
    @SuppressWarnings("unchecked")
    protected void createTestData(String entityTypeName, String propertyName) {
        JSONObject body = new JSONObject();
        body.put("__id", "doubleMax");
        body.put(propertyName, 1.79E308);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "double");
        body.put(propertyName, 12345.12345);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleString");
        body.put(propertyName, "1.2345");
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleNatural");
        body.put(propertyName, 1);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleDecimal");
        body.put(propertyName, 1.0);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleZero");
        body.put(propertyName, 0.0);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleMin");
        body.put(propertyName, -1.79e308);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);

        body.put("__id", "doubleNull");
        body.put(propertyName, null);
        UserDataUtils.create(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, entityTypeName);
    }

    /**
     * Double検索のテスト用データを削除する.
     * @param entityTypeName エンティティタイプ名
     * @param propertyName プロパティ名
     */
    protected void deleteTestData(String entityTypeName, String propertyName) {
        // UserODataの削除
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleInt", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "double", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleZero", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleNull", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleMax", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleMin", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleString", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleNatural", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, entityTypeName, "doubleDecimal", colName);

        PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, entityTypeName,
                propertyName, -1);
        // EntityTypeの削除
        EntityTypeUtils.delete(colName, DcCoreConfig.getMasterToken(),
                MediaType.APPLICATION_JSON, entityTypeName, boxName, cellName, -1);
    }

    @SuppressWarnings("unchecked")
    private JSONArray skipNullResults(JSONArray source, String propertyName) {
        JSONArray result = new JSONArray();
        for (Object item : source.toArray()) {
            if (((JSONObject) item).get(propertyName) == null) {
                continue;
            }
            result.add(item);
        }
        return result;
    }
}

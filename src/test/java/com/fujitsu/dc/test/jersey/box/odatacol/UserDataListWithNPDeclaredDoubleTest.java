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
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataのDouble型へのNP経由検索テスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListWithNPDeclaredDoubleTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataListWithNPDeclaredDoubleTest() {
        super();
    }

    /**
     * UserDataに$filterの完全一致検索クエリのキーをDoubleで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void UserDataに$filterの完全一致検索クエリのキーをDoubleで指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに整数値をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに整数値をで指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "doubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに小数値(1.0)をで指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリのキーに小数値をで指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの検索クエリleを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの検索クエリleを指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリにnullを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリにnullを指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最大値を指定して対象のデータのみ取得できること.
     */
    @Test
    @Ignore
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最大値を指定して対象のデータのみ取得できること() {
        // TODO #36625のチケットにて対応後、@ignoreを外すこと
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最小値を指定して対象のデータのみ取得できること.
     */
    @Test
    @Ignore
    public final void Edm_Doubleのプロパティに対して$filterの完全一致検索クエリに最小値を指定して対象のデータのみ取得できること() {
        // TODO #36625のチケットにて対応後、@ignoreを外すこと
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの完全一致検索クエリで対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの完全一致検索クエリで対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    targetEntityTypeName, propertyName,
                    targetEntityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること.
     */
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$filterの検索クエリgtを指定して対象のデータのみ取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    targetEntityTypeName, propertyName,
                    targetEntityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
                    + "?$filter=" + propertyName + "+gt+1.234&$inlinecount=allpages&$orderby=__id";
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm.Doubleのプロパティに対して$orderbyクエリ(asc)を指定して正しい順序でデータが取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$orderbyクエリ_ascを指定して正しい順序でデータが取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm.Doubleのプロパティに対して$orderbyクエリ(desc)を指定して正しい順序でデータが取得できること.
     */
    @Test
    public final void Edm_Doubleのプロパティに対して$orderbyクエリ_descを指定して正しい順序でデータが取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Edm_Int32からEdm_Doubleへ更新したプロパティに対して$orderbyクエリを指定して正しい順序で取得できること.
     */
    @Test
    public final void Edm_Int32からEdm_Doubleへ更新したプロパティに対して$orderbyクエリを指定して正しい順序で取得できること() {
        String srcEntityTypeName = "srcDoubleFilterTest";
        String targetEntityTypeName = "targetDoubleFilterTest";
        String propertyName = "doubleProp";
        String srcId = "srcId";
        try {
            // Doubleの検索のテスト用エンティティタイプ作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, srcEntityTypeName,
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, targetEntityTypeName,
                    HttpStatus.SC_CREATED);

            // AssociationEndの作成と紐づけ
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "assoc1", srcEntityTypeName);
            AssociationEndUtils.createViaNP(MASTER_TOKEN_NAME, cellName, boxName, colName,
                    "assoc1", srcEntityTypeName, "assoc2", "*", targetEntityTypeName, HttpStatus.SC_CREATED);

            // プロパティの登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                    propertyName, EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);

            // プロパティの更新
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, propertyName,
                    targetEntityTypeName, propertyName,
                    targetEntityTypeName, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None",
                    false, null);

            // Double検索のテスト用データを作成する
            createTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);

            // 取得対象のデータに対する検索を実施する
            // ユーザデータの一覧取得
            String searchRequestUrl = UrlUtils.userdataNP(cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName)
                    + "?$orderby=" + propertyName + ",__id&$inlinecount=allpages";
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
            deleteTestData(srcEntityTypeName, srcId, targetEntityTypeName, propertyName);
        }
    }

    /**
     * Double検索のテスト用データを作成する.
     * @param srcEntityTypeName ソース側のエンティティタイプ名
     * @param srcId ソース側のUserDataのID
     * @param targetEntityTypeName エンティティタイプ名
     * @param targetPropertyName プロパティ名
     */
    @SuppressWarnings("unchecked")
    protected void createTestData(String srcEntityTypeName, String srcId,
            String targetEntityTypeName, String targetPropertyName) {
        // src側のテスト用データ登録
        UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, "{\"__id\":\"" + srcId + "\"}",
                cellName, boxName, colName, srcEntityTypeName);

        // target側のテスト用データ登録
        JSONObject body = new JSONObject();
        body.put("__id", "doubleMax");
        body.put(targetPropertyName, 1.79E308);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "double");
        body.put(targetPropertyName, 12345.12345);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleString");
        body.put(targetPropertyName, "1.2345");
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleNatural");
        body.put(targetPropertyName, 1);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleDecimal");
        body.put(targetPropertyName, 1.0);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleZero");
        body.put(targetPropertyName, 0.0);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleMin");
        body.put(targetPropertyName, -1.79e308);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

        body.put("__id", "doubleNull");
        body.put(targetPropertyName, null);
        UserDataUtils.createViaNP(DcCoreConfig.getMasterToken(), body, cellName, boxName, colName,
                srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);
    }

    /**
     * Double検索のテスト用データを削除する.
     * @param srcEntityTypeName ソース側のエンティティタイプ名
     * @param srcId ソース側のUserDataのID
     * @param targetEntityTypeName エンティティタイプ名
     * @param propertyName プロパティ名
     */
    protected void deleteTestData(String srcEntityTypeName, String srcId,
            String targetEntityTypeName, String propertyName) {
        // ターゲット側のUserODataの削除
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleInt", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "double", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleZero", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleNull", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleMax", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleMin", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleString", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleNatural", colName);
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, targetEntityTypeName, "doubleDecimal", colName);

        // ソース側のUserODataの削除
        UserDataUtils.delete(DcCoreConfig.getMasterToken(), -1, srcEntityTypeName, srcId, colName);

        // AssociationEndの削除
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, srcEntityTypeName, boxName,
                "assoc1", -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, targetEntityTypeName, boxName,
                "assoc2", -1);

        // Propertyの削除
        PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, targetEntityTypeName,
                propertyName, -1);

        // EntityTypeの削除
        EntityTypeUtils.delete(colName, DcCoreConfig.getMasterToken(),
                MediaType.APPLICATION_JSON, srcEntityTypeName, boxName, cellName, -1);
        EntityTypeUtils.delete(colName, DcCoreConfig.getMasterToken(),
                MediaType.APPLICATION_JSON, targetEntityTypeName, boxName, cellName, -1);
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

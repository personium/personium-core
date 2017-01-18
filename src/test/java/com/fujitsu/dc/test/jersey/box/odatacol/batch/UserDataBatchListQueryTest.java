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
package com.fujitsu.dc.test.jersey.box.odatacol.batch;

import static com.fujitsu.dc.test.utils.BatchUtils.BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.END_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.START_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListResBodyWithCount;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListResBodyWithExpand;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostWithBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveQueryOperationResErrorBody;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.producer.resources.ODataBatchProvider;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData$batchのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchListQueryTest extends AbstractUserDataBatchTest {

    int topMaxNum = DcCoreConfig.getTopQueryMaxSize();
    int skipMaxNum = DcCoreConfig.getSkipQueryMaxSize();

    /**
     * コンストラクタ.
     */
    public UserDataBatchListQueryTest() {
        super();
    }

    /**
     * ユーザOData$batch登録でクエリにtopを指定して指定されてた件数取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにtopを指定して指定されてた件数取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchTop";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$top=1";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);

        }
    }

    /**
     * ユーザOData$batch登録でtopクエリに-1を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でtopクエリにマイナス1を指定して400エラーとなること() {
        String query = "?\\$top=-1";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でtopクエリに文字列を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でtopクエリに文字列を指定して400エラーとなること() {
        String query = "?\\$top=テスト";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でtopクエリに空文字を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でtopクエリに空文字を指定して400エラーとなること() {
        String query = "?\\$top=";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録で$batch内全体でtopクエリの指定件数の合計が指定最大値の場合に正常に取得できること.
     */
    @Test
    public final void ユーザOData$batch登録で$batch内全体でtopクエリの指定件数の合計が指定最大値の場合に正常に取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchTop";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$top=" + topMaxNum / 2;
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);

        }
    }

    /**
     * ユーザOData$batch登録で$batch内全体でtopクエリの指定件数の合計が指定最大値プラス1の場合に400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録で$batch内全体でtopクエリの指定件数の合計が指定最大値プラス1の場合に400エラーとなること() {
        String query = "?\\$top=";
        String body = START_BOUNDARY
                + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query + (topMaxNum / 2 + 1))
                + START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query + topMaxNum / 2)
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
    }

    /**
     * ユーザOData$batch登録でクエリにskipを指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにskipを指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchSkip";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$skip=1";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);

        }
    }

    /**
     * ユーザOData$batch登録でskipクエリに-1を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でskipクエリにマイナス1を指定して400エラーとなること() {
        String query = "?\\$skip=-1";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でskipクエリに文字列を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でskipクエリに文字列を指定して400エラーとなること() {
        String query = "?\\$skip=test";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でskipクエリに空文字を指定して400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でskipクエリに空文字を指定して400エラーとなること() {
        String query = "?\\$skip=";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でskipクエリに最大値を指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でskipクエリに最大値を指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchSkip";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$skip=" + skipMaxNum;
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add("testBatch");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrieveListResBody(null)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でskipクエリに最大値プラス1を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でskipクエリに最大値プラス1を指定した場合400エラーとなること() {
        String query = "?\\$skip=" + skipMaxNum + 1;
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにorderbyを指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにorderbyを指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchOrderby";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$orderby=Name";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix + "1");
            listRes1.add(entityIdPrefix + "2");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "11",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "21",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でクエリにorderbyをdesc指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにorderbyをdesc指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchOrderby";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$orderby=__id+desc";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix + "2");
            listRes1.add(entityIdPrefix + "1");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "11",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "21",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);

        }
    }

    /**
     * ユーザOData$batch登録でクエリにorderbyに空文字を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにorderbyに空文字を指定した場合400エラーとなること() {
        String query = "?\\$orderby=";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにinlinecountを指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにinlinecountを指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchInlinecount";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$inlinecount=allpages";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBodyWithCount(listRes1, 2)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でクエリにinlinecountに不正な文字を指定た場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにinlinecountに不正な文字を指定た場合400エラーとなること() {
        String query = "?\\$inlinecount=test";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにfilterを指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにfilterを指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchFilter";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$filter=__id+eq+%27" + entityIdPrefix + "111%27";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "111")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "211")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix + "1");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "111")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "211")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "111",
                    DcCoreConfig.getMasterToken(),
                    -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "211",
                    DcCoreConfig.getMasterToken(),
                    -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でfilterクエリに不正な値を指定した場合に400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でfilterクエリに不正な値を指定した場合に400エラーとなること() {
        String query = "?\\$filter=test+eq+test";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにexpandを指定して正常取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData$batch登録でクエリにexpandを指定して正常取得できること() {
        String entityTypeName = "Price";
        String navPropName = "Sales";
        String userDataId = "npdata";
        String userDataNpId = "npdataNp";

        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", userDataId + "1");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, -1, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName);
            body = new JSONObject();
            body.put("__id", userDataNpId + "1");
            UserDataUtils.createViaNP(AbstractCase.MASTER_TOKEN_NAME, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName, userDataId + "1", navPropName, -1);

            // $batchリクエスト
            String query = "?\\$expand=_" + navPropName;
            String bodyBatch = START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", bodyBatch)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(userDataId);
            List<String> listResNpName = new ArrayList<String>();
            listResNpName.add(navPropName);
            List<String> listResChildren = new ArrayList<String>();
            listResChildren.add(userDataNpId);
            String expectedBody = START_BOUNDARY
                    + retrieveListResBodyWithExpand(listRes1, listResNpName, listResChildren)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            UserDataUtils.deleteLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, userDataId
                    + "1",
                    navPropName, userDataNpId + "1", -1);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName, userDataId + "1");
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, userDataNpId + "1");
        }
    }

    /**
     * ユーザOData$batch登録でexpandに最大プロパティ数を指定した場合正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData$batch登録でexpandに最大プロパティ数を指定した場合正常に取得できること() {
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        String userDataId = "npdata";

        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", userDataId + "1");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, -1, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity);

            // $batchリクエスト
            String query = String.format("?\\$expand=_%s,_%s", targetEntity1, targetEntity2);
            String bodyBatch = START_BOUNDARY + retrieveListBodyWithQuery(fromEntity, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", bodyBatch)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userDataId);

            String expectedBody = START_BOUNDARY
                    + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity, userDataId + "1");
        }
    }

    /**
     * ユーザOData$batch登録でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData$batch登録でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること() {
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";
        String targetEntity3 = "Supplier";

        String userDataId = "npdata";

        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, -1, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity);

            // $batchリクエスト
            String query = String.format("?\\$expand=_%s,_%s,_%s", targetEntity1, targetEntity2, targetEntity3);
            String bodyBatch = START_BOUNDARY + retrieveListBodyWithQuery(fromEntity, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", bodyBatch)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity, userDataId);
        }
    }

    /**
     * ユーザOData$batch登録でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData$batch登録でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること() {
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        String userDataId = "npdata";
        int top = DcCoreConfig.getTopQueryMaxSizeWithExpand();

        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", userDataId + "1");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, -1, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity);

            // $batchリクエスト
            String query = String.format("?\\$expand=_%s,_%s&\\$top=%d", targetEntity1, targetEntity2, top);
            String bodyBatch = START_BOUNDARY + retrieveListBodyWithQuery(fromEntity, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", bodyBatch)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userDataId);

            String expectedBody = START_BOUNDARY
                    + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity, userDataId + "1");
        }
    }

    /**
     * ユーザOData$batch登録でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData$batch登録でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること() {
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        String userDataId = "npdata";
        int top = DcCoreConfig.getTopQueryMaxSizeWithExpand() + 1;

        try {
            // 事前にデータを登録する
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, -1, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity);

            // $batchリクエスト
            String query = String.format("?\\$expand=_%s,_%s&\\$top=%d", targetEntity1, targetEntity2, top);
            String bodyBatch = START_BOUNDARY + retrieveListBodyWithQuery(fromEntity, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", bodyBatch)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, fromEntity, userDataId);
        }
    }

    /**
     * ユーザOData$batch登録でexpandクエリに不正な値を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でexpandクエリに不正な値を指定した場合400エラーとなること() {
        String query = "?\\$expand=_test";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにselectを指定して正常取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにselectを指定して正常取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchSelect";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$select=Name";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でselectクエリに空文字を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でselectクエリに空文字を指定した場合400エラーとなること() {
        String query = "?\\$select=";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録でクエリにqを指定して対象のデータが取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリにqを指定して対象のデータが取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchQ";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\q=" + entityIdPrefix + "21";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix + "2");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "11",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "21",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録でqクエリの値が最大値プラス1を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザOData$batch登録でqクエリの値が最大値プラス1を指定した場合400エラーとなること() {
        String query = "?q=123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890123456";
        String body = START_BOUNDARY + retrieveListBodyWithQuery(Setup.TEST_ENTITYTYPE_M1, query)
                + END_BOUNDARY;
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスヘッダのチェック
        checkBatchResponseHeaders(res);

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                + END_BOUNDARY;
        checkBatchResponseBody(res, expectedBody);
    }

    /**
     * ユーザOData$batch登録で複数クエリを指定した場合に対象のデータが取得できること.
     */
    @Test
    public final void ユーザOData$batch登録で複数クエリを指定した場合に対象のデータが取得できること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchMulti";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$top=1&\\$skip=1&\\$inlinecount=allpages";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix + "2");
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "11")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "21")
                    + START_BOUNDARY + retrieveListResBodyWithCount(listRes1, 2)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "11",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "21",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * ユーザOData$batch登録で複数クエリを指定した場合に対象のデータが取得できること.
     */
    @Test
    public final void ユーザOData$batch登録でクエリを重複して指定した場合に先に指定されたクエリが優先されること() {
        String entityTypeName = "testListEntity";
        String entityIdPrefix = "testBatchMultiQuery";
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    entityTypeName,
                    HttpStatus.SC_CREATED);

            String query = "?\\$top=1&\\$top=10";
            String body = START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostBody(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListBodyWithQuery(entityTypeName, query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> listRes1 = new ArrayList<String>();
            listRes1.add(entityIdPrefix);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "1")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol(entityTypeName, entityIdPrefix + "2")
                    + START_BOUNDARY + retrieveListResBody(listRes1)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            // UserODataの削除
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "1",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, entityTypeName, entityIdPrefix + "2",
                    DcCoreConfig.getMasterToken(), -1);
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$filterクエリを指定して正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$filterクエリを指定して正常に取得できること() {

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
            batchBody.put("__id", userOdataIdDouble + 1);
            batchBody.put(propName, 1.23);

            String query = "?\\$filter=" + propName + "+eq+1.23";
            String reqBody = START_BOUNDARY + retrievePostWithBody("Supplier", batchBody)
                    + START_BOUNDARY + retrieveListBodyWithQuery("Supplier", query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userOdataIdDouble);
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble + 1)
                    + START_BOUNDARY + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            String body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdInt + "')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdDouble + 1 + "')") + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$selectクエリを指定して正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$selectクエリを指定して正常に取得できること() {

        String userOdataIdInt = "batchInt";
        String userOdataIdDouble = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject body = new JSONObject();
            body.put("__id", userOdataIdInt + 1);
            body.put(propName, 1);

            // ユーザデータの登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName,
                    colName, "Supplier");
            // プロパティの更新(Edm.Int32⇒Edm.double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propName, "Supplier",
                    propName,
                    "Supplier", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataIdDouble + 1);
            batchBody.put(propName, 1.23);

            String query = "?\\$select=" + propName + "&\\$orderby=__id";
            String reqBody = START_BOUNDARY + retrievePostWithBody("Supplier", batchBody)
                    + START_BOUNDARY + retrieveListBodyWithQuery("Supplier", query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userOdataIdDouble);
            idPrefix.add(userOdataIdInt);
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble + 1)
                    + START_BOUNDARY + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            String body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdInt + 1 + "')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdDouble + 1 + "')") + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$orderbyクエリを指定して正常に取得できること_asc.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$orderbyクエリを指定して正常に取得できること_asc() {

        String userOdataIdInt = "batchInt";
        String userOdataIdDouble = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject body = new JSONObject();
            body.put("__id", userOdataIdInt + 1);
            body.put(propName, 1);

            // ユーザデータの登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName,
                    colName, "Supplier");
            // プロパティの更新(Edm.Int32⇒Edm.double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propName, "Supplier",
                    propName,
                    "Supplier", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataIdDouble + 1);
            batchBody.put(propName, 1.23);

            String query = "?\\$orderby=" + propName + "+asc";
            String reqBody = START_BOUNDARY + retrievePostWithBody("Supplier", batchBody)
                    + START_BOUNDARY + retrieveListBodyWithQuery("Supplier", query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userOdataIdInt);
            idPrefix.add(userOdataIdDouble);
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble + 1)
                    + START_BOUNDARY + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            String body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdInt + 1 + "')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdDouble + 1 + "')") + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$orderbyクエリを指定して正常に取得できること_desc.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由で$orderbyクエリを指定して正常に取得できること_desc() {

        String userOdataIdInt = "batchInt";
        String userOdataIdDouble = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject body = new JSONObject();
            body.put("__id", userOdataIdInt + 1);
            body.put(propName, 1);

            // ユーザデータの登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName,
                    colName, "Supplier");
            // プロパティの更新(Edm.Int32⇒Edm.double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propName, "Supplier",
                    propName,
                    "Supplier", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataIdDouble + 1);
            batchBody.put(propName, 1.23);

            String query = "?\\$orderby=" + propName + "+desc";
            String reqBody = START_BOUNDARY + retrievePostWithBody("Supplier", batchBody)
                    + START_BOUNDARY + retrieveListBodyWithQuery("Supplier", query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userOdataIdDouble);
            idPrefix.add(userOdataIdInt);
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble + 1)
                    + START_BOUNDARY + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            String body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdInt + 1 + "')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdDouble + 1 + "')") + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
    }

    /**
     * プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由でqクエリを指定して正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void プロパティのタイプをEdm_Int32からEdm_Doubleへ更新後に$batch経由でqクエリを指定して正常に取得できること() {

        String userOdataIdInt = "batchInt";
        String userOdataIdDouble = "batchDouble";
        String propName = "doubleProp";
        try {
            // プロパティ登録
            PropertyUtils.create(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), true, null, "None", false, null,
                    HttpStatus.SC_CREATED);
            JSONObject body = new JSONObject();
            body.put("__id", userOdataIdInt + 1);
            body.put(propName, 1);

            // ユーザデータの登録
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName,
                    colName, "Supplier");
            // プロパティの更新(Edm.Int32⇒Edm.double)
            PropertyUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, propName, "Supplier",
                    propName,
                    "Supplier", EdmSimpleType.DOUBLE.getFullyQualifiedTypeName(), true, null, "None", false, null);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", userOdataIdDouble + 1);
            batchBody.put(propName, 1.23);

            String query = "?\\q=1.23";
            String reqBody = START_BOUNDARY + retrievePostWithBody("Supplier", batchBody)
                    + START_BOUNDARY + retrieveListBodyWithQuery("Supplier", query)
                    + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", reqBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            List<String> idPrefix = new ArrayList<String>();
            idPrefix.add(userOdataIdDouble);
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", userOdataIdDouble + 1)
                    + START_BOUNDARY + retrieveListResBody(idPrefix)
                    + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        } finally {
            String body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdInt + 1 + "')") + END_BOUNDARY;
            TResponse res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            body = START_BOUNDARY + retrieveDeleteBody("Supplier('" + userOdataIdDouble + 1 + "')") + END_BOUNDARY;
            res = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            PropertyUtils.delete(AbstractCase.BEARER_MASTER_TOKEN, cellName, boxName, colName, "Supplier", propName,
                    HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダのチェック
            checkBatchResponseHeaders(res);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveDeleteResBody() + END_BOUNDARY;
            checkBatchResponseBody(res, expectedBody);
        }
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

    static String retrieveListBodyWithQuery(String path, String query) {
        return "Content-Type: application/http\n"
                + "Content-Transfer-Encoding:binary\n\n"
                + "GET " + path + query + "\n"
                + "Host: host\n\n";
    }
}

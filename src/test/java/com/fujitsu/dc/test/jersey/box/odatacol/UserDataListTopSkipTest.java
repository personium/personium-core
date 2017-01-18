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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData一覧のテスト_$top_$skip.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListTopSkipTest extends AbstractUserDataTest {

    int topMaxNum = DcCoreConfig.getTopQueryMaxSize();
    int skipMaxNum = DcCoreConfig.getSkipQueryMaxSize();

    /**
     * コンストラクタ.
     */
    public UserDataListTopSkipTest() {
        super();
    }

    /**
     * $topに-1を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $topにマイナス1を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$top=-1")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", -1).getMessage());
    }

    /**
     * $topに0を指定してUserDataの一覧をした場合にデータが何も取得されないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $topに0を指定してUserDataの一覧をした場合にデータが何も取得されないこと() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$top=0")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * $topに最大値を指定してUserDataの一覧をした場合に200となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $topに最大値を指定してUserDataの一覧をした場合に200となること() {
        String top = Integer.toString(topMaxNum);
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$top=" + top)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * $topに最大値プラス1を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $topに最大値プラス1を指定してUserDataの一覧をした場合に400エラーとなること() {
        String top = Integer.toString(topMaxNum + 1);
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$top=" + top)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", top).getMessage());
    }

    /**
     * $topに文字列を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $topに文字列を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$top=%27test%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$top").getMessage());
    }

    /**
     * $topに空文字を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $topに空文字を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$top=")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$top").getMessage());
    }

    /**
     * $skipに-1を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $skipにマイナス1を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$skip=-1")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$skip", "-1").getMessage());
    }

    /**
     * $skipに0を指定してUserDataの一覧をした場合に200となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $skipに0を指定してUserDataの一覧をした場合に200となること() {
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$skip=0")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * $skipに最大値を指定してUserDataの一覧をした場合に200となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $skipに最大値を指定してUserDataの一覧をした場合に200となること() {
        String skip = Integer.toString(skipMaxNum);
        // リクエストボディを設定
        String userDataId = "userdata001";
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);

        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = createUserData(body, HttpStatus.SC_CREATED);

            Map<String, String> etagList = new HashMap<String, String>();
            etagList.put("userdata001", respons.getHeader(HttpHeaders.ETAG));

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$skip=" + skip)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // ユーザデータ削除
            deleteUserData(userDataId);
        }
    }

    /**
     * $skipに最大値プラス1を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $skipに最大値プラス1を指定してUserDataの一覧をした場合に400エラーとなること() {
        String skip = Integer.toString(skipMaxNum + 1);
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$skip=" + skip)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$skip", skip).getMessage());
    }

    /**
     * $skipに文字列を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $skipに文字列を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$skip=%27test%27")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$skip").getMessage());
    }

    /**
     * $skipに空文字を指定してUserDataの一覧をした場合に400エラーとなること.
     */
    @Test
    public final void $skipに空文字を指定してUserDataの一覧をした場合に400エラーとなること() {
        // ユーザデータの一覧取得
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("query", "?\\$skip=")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                DcCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$skip").getMessage());
    }
}

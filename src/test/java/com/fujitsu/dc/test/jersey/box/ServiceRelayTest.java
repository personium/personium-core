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
package com.fujitsu.dc.test.jersey.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * サービス実行のリレーテスト.
 */
@RunWith(DcRunner.class)
@Category({Integration.class, Regression.class })
public class ServiceRelayTest extends JerseyTest {
    /**
     * コンストラクタ.
     */
    public ServiceRelayTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /** リクエスト情報をそのままJSON文字列で返却するJSソース. */
    public static final String SOURCE
        = "function(request){"
        + "  var body = request.input.readAll('utf-8');"
        + "  var headers = request.headers;"
        + "  var response = {"
        + "    \"body\": body,"
        + "    \"header\": headers,"
        + "    \"method\" : request.method,"
        + "    \"query\" : request.queryString"
        + "  };"
        + "  return {"
        + "    status: 200,"
        + "    headers: {'Content-Type':'application/json'},"
        + "    body: [JSON.stringify(response)]"
        + " };"
        + "}";

    /** レスポンスヘッダーのチェック項目. */
    private static final String[] CHECK_HEADERS = {"x-baseurl",
            "x-request-uri",
            "x-dc-es-index",
            "x-dc-es-id",
            "x-dc-es-type",
            "host",
            "connection",
            "authorization",
            "user-agent"};

    /**
     * 事前準備.
     */
    @Before
    public final void before() {
        // PropPatch サービス設定の登録
        Http.request("box/proppatch-set-service.txt")
                .with("path", "service_relay")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("name", "relay")
                .with("src", "relay.js")
                .returns()
                .statusCode(HttpStatus.SC_MULTI_STATUS);

        // WebDAV サービスリソースの登録
        Http.request("box/dav-put.txt")
                .with("cellPath", "testcell1")
                .with("path", "service_relay/__src/relay.js")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", "box1")
                .with("contentType", "text/javascript")
                .with("source", SOURCE)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * 事後処理.
     */
    @After
    public final void after() {
        // WebDAV サービスリソースの削除
        Http.request("box/dav-delete.txt")
                .with("cellPath", "testcell1")
                .with("path", "service_relay/__src/relay.js")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", "box1")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Getリクエストのリレー.
     */
    @Test
    public final void Getリクエストのリレー() {
        String method = "GET";
        String body = "";
        String query = "sample=test";

        exexServiceWithCheck(method, body, query);
    }

    /**
     * Postリクエストのリレー.
     */
    @Test
    public final void Postリクエストのリレー() {
        String method = "POST";
        String body = "hello POST";
        String query = "";

        exexServiceWithCheck(method, body, query);
    }

    /**
     * Putリクエストのリレー.
     */
    @Test
    public final void Putリクエストのリレー() {
        String method = "PUT";
        String body = "hello PUT";
        String query = "";

        exexServiceWithCheck(method, body, query);
    }

    /**
     * Deleteリクエストのリレー.
     */
    @Test
    public final void Deleteリクエストのリレー() {
        String method = "DELETE";
        String body = "";
        String query = "";

        exexServiceWithCheck(method, body, query);
    }

    /**
     * サービス実行でexec権限のみリクエストが実行可能であること.
     */
    @Test
    public final void サービス実行でexec権限のみリクエストが実行可能であること() {
        String cell = "testcell1";
        String body = "";
        String query = "";
        // account1 アクセス権無し
        String noToken = ResourceUtils.getMyCellLocalToken(cell, "account1", "password1");
        // account4 読み書き
        String readwriteToken = ResourceUtils.getMyCellLocalToken(cell, "account4", "password4");
        // account5 実行権限
        String execToken = ResourceUtils.getMyCellLocalToken(cell, "account5", "password5");

        // GET
        execService(HttpMethod.GET, body, query, noToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.GET, body, query, readwriteToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.GET, body, query, execToken, HttpStatus.SC_OK);

        // DELETE
        execService(HttpMethod.DELETE, body, query, noToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.DELETE, body, query, readwriteToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.DELETE, body, query, execToken, HttpStatus.SC_OK);

        // POST
        body = "request body";
        execService(HttpMethod.POST, body, query, noToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.POST, body, query, readwriteToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.POST, body, query, execToken, HttpStatus.SC_OK);

        // PUT
        execService(HttpMethod.PUT, body, query, noToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.PUT, body, query, readwriteToken, HttpStatus.SC_FORBIDDEN);
        execService(HttpMethod.PUT, body, query, execToken, HttpStatus.SC_OK);
    }

    /**
     * レスポンスのチェック.
     */
    private void exexServiceWithCheck(String method, String body, String query) {
        // リクエストの実行
        String requestQuery = "";
        if (query.length() != 0) {
            requestQuery = "?" + query;
        }
        TResponse response = execService(method, body, requestQuery, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスのチェック
        JSONObject checkObj = response.bodyAsJson();
        assertEquals((String) checkObj.get("method"), method);
        assertEquals((String) checkObj.get("body"), body);
        assertEquals((String) checkObj.get("query"), query);
        JSONObject headerObj = (JSONObject) checkObj.get("header");
        for (String key : CHECK_HEADERS) {
            assertNotNull(headerObj.get(key));
        }
    }

    /**
     * サービスリクエストを実行する.
     * @param method リクエストメソッド
     * @param body リクエストボディ
     * @param requestQuery リクエストクエリ
     * @param token 認証トークン
     * @param code 期待するレスポンスコード
     * @return
     */
    private TResponse execService(String method, String body, String requestQuery, String token, int code) {
        TResponse response = Http.request("box/service-exec.txt")
                .with("path", "service_relay/relay")
                .with("token", token)
                .with("method", method)
                .with("body", body)
                .with("query", requestQuery)
                .returns()
                .debug()
                .statusCode(code);
        return response;
    }

}

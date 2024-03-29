/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.engine;

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

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;

/**
 * サービス実行のリレーテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Integration.class, Regression.class })
public abstract class ServiceRelayTestBase extends PersoniumTest {
    /**
     * コンストラクタ.
     */
    public ServiceRelayTestBase() {
        super(new PersoniumCoreApplication());
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
            "x-personium-fs-path",
            "x-personium-fs-routing-id",
            "host",
            "connection",
            "authorization",
            "user-agent",
            "x-personium-requestkey"};

    /**
     * Method called when test class configures service endpoint
     */
    abstract void configureService();

    /**
     * Method called when test class deconfigures service endpoint
     */
    abstract void deconfigureService();

    /**
     * Method called when test class decides the endpoint to execute
     * @return relative path in service collection to execute
     */
    abstract String getPathToExecute();

    /**
     * 事前準備.
     */
    @Before
    public final void before() {
        this.configureService();
    }

    /**
     * 事後処理.
     */
    @After
    public final void after() {
        this.deconfigureService();
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
     * Execute service with checking result status code.
     */
    private void exexServiceWithCheck(String method, String body, String query) {
        execServiceWithCheck(method, this.getPathToExecute(), body, query);
    }

    /**
     * Execute service with checking status code
     * @param method
     * @param path
     * @param body
     * @param query
     */
    private void execServiceWithCheck(String method, String path, String body, String query) {
        // リクエストの実行
        String requestQuery = "";
        if (query.length() != 0) {
            requestQuery = "?" + query;
        }
        TResponse response = execService(method, path, body, requestQuery, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        // レスポンスのチェック
        JSONObject checkObj = response.bodyAsJson();
        assertEquals((String) checkObj.get("method"), method);
        assertEquals((String) checkObj.get("body"), body);
        assertEquals((String) checkObj.get("query"), query);
        JSONObject headerObj = (JSONObject) checkObj.get("header");
        System.out.println(headerObj);
        for (String key : CHECK_HEADERS) {
            assertNotNull(headerObj.get(key));
        }
    }

    /**
     * Function for send service request
     * @param method Method of request
     * @param body Request body
     * @param requestQuery Request query
     * @param token authentication token
     * @param code expected status code of response
     * @return
     */
    private TResponse execService(String method, String body, String requestQuery, String token, int code) {
        return execService(method, this.getPathToExecute(), body, requestQuery, token, code);
    }

    /**
     * Function for send service request
     * @param method Method of request
     * @param path Request path
     * @param body Request body
     * @param requestQuery Request query
     * @param token authentication token
     * @param code expected status code of response
     * @return
     */
    protected TResponse execService(String method, String path, String body, String requestQuery, String token, int code) {
        TResponse response = Http.request("box/service-exec.txt")
                .with("path", path)
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

/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.test.jersey.box.odatacol;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * $expandクエリ指定のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataLinkQueryTest extends AbstractUserDataTest {

    int topMaxNum = PersoniumUnitConfig.getTopQueryMaxSize();
    int skipMaxNum = PersoniumUnitConfig.getSkipQueryMaxSize();

    private String toEntityTypeName = "toEntity";
    private String fromEntityTypeName = "fromEntity";
    private String toUserDataId = "toEntitySet";
    private String fromUserDataId = "fromEntitySet";
    private String toUserDataId2 = "toEntitySet2";
    private String fromUserDataId2 = "fromEntitySet2";

    /**
     * コンストラクタ.
     */
    public UserDataLinkQueryTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ユーザデータのlink一覧取得で$topに-1を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topにマイナス1を指定した場合400エラーとなること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$top=-1")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", "-1").getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$topに0を指定した場合0件取得されること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$topに0を指定した場合0件取得されること_AssociationEndがアスタ対アスタ() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product");
            // $link
            linkUserData("Sales", toUserDataId, "Product", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Product" + "('" + fromUserDataId + "')")
                    .with("trgPath", "Sales")
                    .with("query", "?\\$top=0")
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "Product", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$topに指定最大値を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$topに指定最大値を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ() {
        String top = Integer.toString(topMaxNum);
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product");
            // $link
            linkUserData("Sales", toUserDataId, "Product", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Product" + "('" + fromUserDataId + "')")
                    .with("trgPath", "Sales")
                    .with("query", "?\\$top=" + top)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "Product", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$topに0を指定した場合0件取得されること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$topに0を指定した場合0件取得されること_AssociationEndが1対アスタ() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // $link
            linkUserData("SalesDetail", toUserDataId, "Sales", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Sales" + "('" + fromUserDataId + "')")
                    .with("trgPath", "SalesDetail")
                    .with("query", "?\\$top=0")
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // $link
            deleteUserDataLinks("SalesDetail", toUserDataId, "Sales", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$topに指定最大値を指定した場合に正常に取得されること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$topに指定最大値を指定した場合に正常に取得されること_AssociationEndが1対アスタ() {
        String top = Integer.toString(topMaxNum);
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // $link
            linkUserData("SalesDetail", toUserDataId, "Sales", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Sales" + "('" + fromUserDataId + "')")
                    .with("trgPath", "SalesDetail")
                    .with("query", "?\\$top=" + top)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // $link
            deleteUserDataLinks("SalesDetail", toUserDataId, "Sales", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$topに指定最大値より1大きい値を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topに指定最大値より1大きい値を指定した場合に400エラーが返却されること() {
        String top = Integer.toString(topMaxNum + 1);
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$top=" + top)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", top).getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$topに文字列を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topに文字列を指定した場合に400エラーが返却されること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$top=%27test%27")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$top").getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$topに空文字を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topに空文字を指定した場合に400エラーが返却されること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$top=")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$top").getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$skipにマイナス1を指定した場合400エラーとなること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$skipにマイナス1を指定した場合400エラーとなること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$skip=-1")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$skip", "-1").getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$skipに0を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$skipに0を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product");
            // $link
            linkUserData("Sales", toUserDataId, "Product", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Product" + "('" + fromUserDataId + "')")
                    .with("trgPath", "Sales")
                    .with("query", "?\\$skip=0")
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "Product", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$skipに指定最大値を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$skipに指定最大値を指定した場合に正常に取得されること_AssociationEndがアスタ対アスタ() {
        String skip = Integer.toString(skipMaxNum);
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product");
            // $link
            linkUserData("Sales", toUserDataId, "Product", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Product" + "('" + fromUserDataId + "')")
                    .with("trgPath", "Sales")
                    .with("query", "?\\$skip=" + skip)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // $link
            deleteUserDataLinks("Sales", toUserDataId, "Product", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Product", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$skipに0を指定した場合に正常に取得されること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$skipに0を指定した場合に正常に取得されること_AssociationEndが1対アスタ() {
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // $link
            linkUserData("SalesDetail", toUserDataId, "Sales", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Sales" + "('" + fromUserDataId + "')")
                    .with("trgPath", "SalesDetail")
                    .with("query", "?\\$skip=0")
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            // $link
            deleteUserDataLinks("SalesDetail", toUserDataId, "Sales", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$skipに指定最大値を指定した場合に正常に取得されること_AssociationEndが1対アスタ.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのlink一覧取得で$skipに指定最大値を指定した場合に正常に取得されること_AssociationEndが1対アスタ() {
        String skip = Integer.toString(skipMaxNum);
        JSONObject body = new JSONObject();
        try {
            // toのユーザデータを作成
            body.put("__id", toUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail");
            // fromのユーザデータを作成
            body.put("__id", fromUserDataId);
            createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales");
            // $link
            linkUserData("SalesDetail", toUserDataId, "Sales", fromUserDataId);

            // $links一覧取得
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("boxPath", Setup.TEST_BOX1)
                    .with("colPath", Setup.TEST_ODATA)
                    .with("srcPath", "Sales" + "('" + fromUserDataId + "')")
                    .with("trgPath", "SalesDetail")
                    .with("query", "?\\$skip=" + skip)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
        } finally {
            // $link
            deleteUserDataLinks("SalesDetail", toUserDataId, "Sales", fromUserDataId);
            // fromのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "Sales", fromUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
            // toのユーザデータを削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "SalesDetail", toUserDataId, PersoniumUnitConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$skipに指定最大値より1大きい値を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$skipに指定最大値より1大きい値を指定した場合に400エラーが返却されること() {
        String skip = Integer.toString(skipMaxNum + 1);
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$skip=" + skip)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$skip", skip).getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$skipに文字列を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$skipに文字列を指定した場合に400エラーが返却されること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$skip=%27test%27")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$skip").getMessage());
    }

    /**
     * ユーザデータのlink一覧取得で$skipに空文字を指定した場合に400エラーが返却されること.
     */
    @Test
    public final void ユーザデータのlink一覧取得で$skipに空文字を指定した場合に400エラーが返却されること() {
        // $links一覧取得
        TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", "SalesDetail('userdata000')")
                .with("trgPath", "test")
                .with("query", "?\\$skip=")
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.getCode(),
                PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$skip").getMessage());
    }

    /**
     * データ作成処理.
     */
    @SuppressWarnings("unchecked")
    public final void createData() {
        navPropName = fromEntityTypeName;
        JSONObject body = new JSONObject();
        JSONObject linkBody = new JSONObject();

        // エンティティタイプを作成
        EntityTypeUtils.create(Setup.TEST_CELL1, PersoniumUnitConfig.getMasterToken(),
                Setup.TEST_ODATA, toEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(Setup.TEST_CELL1, PersoniumUnitConfig.getMasterToken(),
                Setup.TEST_ODATA, navPropName, HttpStatus.SC_CREATED);

        // AssociationEndを作成
        AssociationEndUtils.create(PersoniumUnitConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "AssociationEnd", toEntityTypeName);
        AssociationEndUtils.create(PersoniumUnitConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "LinkAssociationEnd", navPropName);

        // AssociationEndを関連付け
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, toEntityTypeName, navPropName, "AssociationEnd", "LinkAssociationEnd",
                HttpStatus.SC_NO_CONTENT);

        // ユーザデータを作成
        body.put("__id", toUserDataId);
        linkBody.put("__id", fromUserDataId);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        body.put("__id", toUserDataId2);
        linkBody.put("__id", fromUserDataId2);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId + "')")
                .with("trgPath", navPropName)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId2 + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId2 + "')")
                .with("trgPath", navPropName)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private void linkUserData(String toEntity, String toUserId,
           String fromEntity,
           String fromUserId) {

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + fromEntity + "('" + fromUserId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntity + "('" + toUserDataId + "')")
                .with("trgPath", fromEntity)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    private void deleteUserDataLinks(String srcEntityTypeName, String userDataId, String trgEntityTypeName,
            String navPropId) {
        // リクエスト実行
        Http.request("box/odatacol/delete-link.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", srcEntityTypeName)
                .with("id", userDataId)
                .with("navProp", "_" + trgEntityTypeName)
                .with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns();
    }
}

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
package io.personium.test.jersey.cell.ctl;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Relation;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.Http;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Relationの更新のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RelationUpdateTest extends ODataCommon {

    private static final String RELATION_TYPE = "CellCtl.Relation";
    private static String cellName = "testcell1";
    private static String testRelationName = "testrelation";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RelationUpdateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * RELATION更新のテスト 更新元_Box.Name=null, 更新先_Box.Name=存在するBox指定.
     */
    @Test
    public void RELATION更新_更新元BoxNameにnull_更新先BoxNameに存在するBox名を指定したとき204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRelationAndCheckResponse(false);

            // 更新
            Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "\"" + boxname + "\"")
                    .returns()
                    .debug();

            // 取得
            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName, boxname);
        }
    }

    /**
     * RELATION更新のテスト 更新元_Box.Name=存在するBox, 更新先_Box.Name=null指定.
     */
    @Test
    public void RELATION更新_更新元BoxNameに存在するBox_更新先BoxNameにnullを指定したとき204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRelation(boxname);

            // 更新
            Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "null")
                    .returns()
                    .debug();

            // 取得
            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * RELATION更新のテスト 更新元_Box.Name=存在するBox指定, 更新先_Box.Name=更新元と同名のBox指定.
     */
    @Test
    public void RELATION更新_複合キーのRelationに対して同名で更新すると204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRelation(boxname);

            // 更新
            Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "\"" + boxname + "\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName, boxname);
        }
    }

    /**
     * RELATION更新のテスト 更新元_Box.Name=null, 更新先_Box.Name=null.
     */
    @Test
    public void RELATION更新_単一キーのRelationに対して同名で更新すると204を返却すること() {
        try {
            // 登録
            createRelationAndCheckResponse(true);

            // 更新
            Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "null")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Relation.EDM_TYPE_NAME,
                    "Name='" + testRelationName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRelationName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * Relationの更新でURLに存在しないBox名を指定した場合404が返却されること.
     */
    @Test
    public void Relationの更新でURLに存在しないBox名を指定した場合404が返却されること() {
        String boxname = "dummy";

        try {
            // 登録
            createRelationAndCheckResponse(false);

            // 更新
            Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + boxname + "'")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "\"box1\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * Relationの更新でボディに存在しないBox名を指定した場合400が返却されること.
     */
    @Test
    public void Relationの更新でボディに存在しないBox名を指定した場合400が返却されること() {
        String boxname = "dummy";

        try {
            // 登録
            createRelationAndCheckResponse(false);

            // 更新
            TResponse res = Http.request("relation-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "null")
                    .with("updateName", testRelationName)
                    .with("updateBoxName", "\"" + boxname + "\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // メッセージ確認
            ODataCommon.checkErrorResponseBody(res,
                    PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(boxname).getMessage());

        } finally {
            CellCtlUtils.deleteRelation(cellName, testRelationName);
        }
    }

    /**
     * merge test.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void merge() {
        String boxname = "box1";
        String relationName = testRelationName;

        try {
            // Advance preparation
            createRelation(boxname);

            // Set and update all parameters(MERGE)
            JSONObject body = new JSONObject();
            body.put("Name", "testrelationmerge1");
            body.put("_Box.Name", null);
            Http.request("relation-update-merge.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", relationName)
                    .with("boxname", "'" + boxname + "'")
                    .with("body", body.toJSONString())
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            relationName = body.get("Name").toString();

            // Set name only
            body = new JSONObject();
            body.put("Name", "testrelationmerge2");
            Http.request("relation-update-merge.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", relationName)
                    .with("boxname", "null")
                    .with("body", body.toJSONString())
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            relationName = body.get("Name").toString();

            // Set _Box.Name only
            body = new JSONObject();
            body.put("_Box.Name", boxname);
            Http.request("relation-update-merge.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", relationName)
                    .with("boxname", "null")
                    .with("body", body.toJSONString())
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Set Name is null (Bad request)
            body = new JSONObject();
            body.put("Name", null);
            body.put("_Box.Name", null);
            Http.request("relation-update-merge.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", relationName)
                    .with("boxname", "'" + boxname + "'")
                    .with("body", body.toJSONString())
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // Get relation (Check response parameter)
            TResponse response = Http.request("relation-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", relationName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Relation.EDM_TYPE_NAME,
                    "Name='" + relationName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", relationName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName, boxname);
        }
    }

    /**
     * 指定されたボックス名にリンクされたRelationを作成する.
     * @param boxname
     */
    @SuppressWarnings("unchecked")
    private void createRelation(String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        body.put("_Box.Name", boxname);

        TResponse response = RelationUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                body, HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Relation",
                "Name='" + testRelationName + "',_Box.Name='" + boxname + "'");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRelationName);
        additional.put("_Box.Name", boxname);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

    }

    /**
     * ボックス名にリンクされていないRelationを作成する.
     * @param boxNameEmpty _Box.Nameを指定しない
     */
    @SuppressWarnings("unchecked")
    private void createRelationAndCheckResponse(boolean boxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        if (!boxNameEmpty) {
            body.put("_Box.Name", null);
        }

        TResponse response = RelationUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                body, HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Relation", "Name='"
                + testRelationName + "',_Box.Name=null");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRelationName);
        additional.put("_Box.Name", null);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, RELATION_TYPE, additional);

    }
}

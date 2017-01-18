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
package com.fujitsu.dc.test.jersey.cell.ctl;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ROLEの更新のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleUpdateTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRoleName = "testrole";
    private static final String ROLE_TYPE = "CellCtl.Role";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleUpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ROLE更新のテスト 更新元_Box.Name=null, 更新先_Box.Name=存在するBox指定.
     */
    @Test
    public void ROLE更新_更新元BoxNameにnull_更新先BoxNameに存在するBox名を指定したとき204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRoleAndCheckResponse(false);

            // 更新
            Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "null")
                    .with("rolename", testRoleName)
                    .with("boxname", "\"" + boxname + "\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName, boxname);
        }
    }

    /**
     * ROLE更新のテスト 更新元_Box.Name=存在するBox, 更新先_Box.Name=null指定.
     */
    @Test
    public void ROLE更新_更新元BoxNameに存在するBox_更新先BoxNameにnullを指定したとき204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRole(boxname);

            // 更新
            Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "'" + boxname + "'")
                    .with("rolename", testRoleName)
                    .with("boxname", "null")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * ROLE更新のテスト 更新元_Box.Name=存在するBox指定, 更新先_Box.Name=存在するBox指定.
     */
    @Test
    public void ROLE更新_複合キーのRoleに対して同名で更新すると204を返却すること() {
        String boxname = "box1";

        try {
            // 登録
            createRole(boxname);

            // 更新
            Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "'" + boxname + "'")
                    .with("rolename", testRoleName)
                    .with("boxname", "\"" + boxname + "\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName, boxname);
        }
    }

    /**
     * ROLE更新のテスト 更新元_Box.Name=null, 更新先_Box.Name=null.
     */
    @Test
    public void ROLE更新_単一キーのRoleに対して同名で更新すると204を返却すること() {
        try {
            // 登録
            createRoleAndCheckResponse(false);

            // 更新
            Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "null")
                    .with("rolename", testRoleName)
                    .with("boxname", "null")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // 取得
            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);
            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * Roleの更新でURLに存在しないBox名を指定した場合404が返却されること.
     */
    @Test
    public void Roleの更新でURLに存在しないBox名を指定した場合404が返却されること() {
        String boxname = "dummy";

        try {
            // 登録
            createRoleAndCheckResponse(false);

            // 更新
            Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "'" + boxname + "'")
                    .with("rolename", testRoleName)
                    .with("boxname", "\"box1\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * Roleの更新でボディに存在しないBox名を指定した場合400が返却されること.
     */
    @Test
    public void Roleの更新でボディに存在しないBox名を指定した場合400が返却されること() {
        String boxname = "dummy";

        try {
            // 登録
            createRoleAndCheckResponse(false);

            // 更新
            TResponse res = Http.request("role-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolenamekey", testRoleName)
                    .with("boxnamekey", "null")
                    .with("rolename", testRoleName)
                    .with("boxname", "\"" + boxname + "\"")
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // メッセージ確認
            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(boxname).getMessage());

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する.
     * @param boxname
     */
    @SuppressWarnings("unchecked")
    private void createRole(String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", boxname);
        TResponse response = Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Role",
                "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRoleName);
        additional.put("_Box.Name", boxname);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

    }

    /**
     * ボックス名にリンクされていないロール情報を作成する.
     * @param boxNameEmpty _Box.Nameを指定しない
     */
    @SuppressWarnings("unchecked")
    private void createRoleAndCheckResponse(boolean boxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        if (!boxNameEmpty) {
            body.put("_Box.Name", null);
        }

        TResponse response = Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Role",
                "Name='" + testRoleName + "',_Box.Name=null");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRoleName);
        additional.put("_Box.Name", null);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

    }

}

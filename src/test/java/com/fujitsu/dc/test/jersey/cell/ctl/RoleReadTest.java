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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ROLEの一件取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleReadTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRoleName = "testrole";
    private static final String ROLE_TYPE = "CellCtl.Role";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleReadTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ROLE一件取得のテスト.
     */
    @Test
    public void ROLE一件取得の正常系ボックス指定ありのテスト() {
        String boxname = "box1";
        try {
            CellCtlUtils.createRole(cellName, testRoleName, boxname);

            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "'" + boxname + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName, boxname);
        }
    }

    /**
     * ROLE一件取得の正常系ボックス指定なしのテスト.
     */
    @Test
    public void ROLE一件取得の正常系ボックス指定なしのテスト() {
        try {
            CellCtlUtils.createRole(cellName, testRoleName);

            TResponse response = Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "null")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * BoxNameを省略してRoleを一件取得した場合データが取得できること.
     */
    @Test
    public void BoxNameを省略してRoleを一件取得した場合データが取得できること() {
        TResponse resCreateRole = null;
        try {
            // _Box.Name指定なしでRole作成
            resCreateRole = CellCtlUtils.createRole(cellName, testRoleName);

            // _Box.Name指定なしでRole取得
            TResponse response = Http.request("role-retrieve-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1",
                    Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            // リンク情報からレスポンスボディ作成
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            if (resCreateRole.getLocationHeader() != null) {
                deleteOdataResource(resCreateRole.getLocationHeader());
            }
        }
    }

    /**
     * BoxNameを指定してRoleを登録し、BoxNameを省略してRoleを一件取得した場合データが取得できないこと.
     */
    @Test
    public void BoxNameを指定してRoleを登録しBoxNameを省略してRoleを一件取得した場合データが取得できないこと() {
        TResponse resCreateRole = null;
        String boxname = "box1";
        try {
            // _Box.Name指定ありでRole作成
            resCreateRole = CellCtlUtils.createRole(cellName, testRoleName, boxname);

            // _Box.Name指定なしでRole取得
            Http.request("role-retrieve-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            if (resCreateRole.getLocationHeader() != null) {
                deleteOdataResource(resCreateRole.getLocationHeader());
            }
        }
    }

    /**
     * BoxNameを指定せずRoleを登録し存在しないBox名を指定して一件取得した場合に404が返却されること.
     */
    @Test
    public void BoxNameを指定せずRoleを登録し存在しないBox名を指定して一件取得した場合に404が返却されること() {
        String dummyBoxName = "dummy";

        try {
            CellCtlUtils.createRole(cellName, testRoleName);

            Http.request("role-retrieve.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "'" + dummyBoxName + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }
}

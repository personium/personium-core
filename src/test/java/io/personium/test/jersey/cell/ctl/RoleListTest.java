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
package io.personium.test.jersey.cell.ctl;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.common.auth.token.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * ROLEの一覧取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleListTest extends ODataCommon {

    private static String cellName = "testrolecell";
    private static String testRoleName = "testrole";
    private static final String ROLE_TYPE = "CellCtl.Role";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleListTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ROLE一覧取得のテスト.
     */
    @Test
    public void ROLE一覧取得の正常系ボックス指定ありのテスト() {
        String boxname = "box1";
        try {
            createCellBox();
            CellCtlUtils.createRole(cellName, testRoleName, boxname);

            TResponse response = Http.request("role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName, Role.EDM_TYPE_NAME,
                    "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", boxname);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName, boxname);
            deleteCellBox();
        }
    }

    /**
     * ROLE一覧取得のテスト.
     */
    @Test
    public void ROLE一覧取得の正常系ボックス指定なしのテスト() {
        try {
            createCellBox();
            CellCtlUtils.createRole(cellName, testRoleName);

            TResponse response = Http.request("role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName, Role.EDM_TYPE_NAME, "Name='"
                    + testRoleName + "',_Box.Name=null");
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", testRoleName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
            deleteCellBox();
        }
    }

    private void createCellBox() {
        // Cell作成
        Http.request("cell-create.txt")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("cellPath", cellName)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);

        // Box作成
        Http.request("cell/box-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("boxPath", "box1")
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    private void deleteCellBox() {
        // Box削除
        Http.request("cell/box-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("boxPath", "box1")
                .returns();

        // Cell削除
        Http.request("cell-delete.txt")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("cellName", cellName)
                .returns();
    }

}

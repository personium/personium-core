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

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ROLEの削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleDeleteTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRoleName = "testrole";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleDeleteTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ROLE削除のテスト.
     */
    @Test
    public void ROLE削除の正常系ボックス指定ありのテスト() {
        String boxname = "box1";
        try {
            CellCtlUtils.createRole(cellName, testRoleName, boxname);
        } finally {
            deleteRole(boxname);
        }
    }

    /**
     * ROLE削除のテスト.
     */
    @Test
    public void ROLE削除の正常系ボックス指定なしのテスト() {
        try {
            CellCtlUtils.createRole(cellName, testRoleName);
        } finally {
            deleteRole();
        }
    }

    /**
     * BoxNameを省略してRoleを削除した場合データが削除できること.
     */
    @Test
    public void BoxNameを省略してRoleを削除した場合データが削除できること() {
        try {
            CellCtlUtils.createRole(cellName, testRoleName);
        } finally {
            TResponse res = Http.request("role-delete-without-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダーのチェック
            // DataServiceVersion
            res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        }
    }

    /**
     * BoxNameを指定してRoleを登録し、BoxNameを省略してRoleを削除した場合404が返却されること.
     */
    @Test
    public void BoxNameを指定してRoleを登録しBoxNameを省略してRoleを削除した場合404が返却されること() {
        String boxname = "box1";
        TResponse res = null;
        try {
            res = CellCtlUtils.createRole(cellName, testRoleName, boxname);
        } finally {
            Http.request("role-delete-without-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
    }

    /**
     * BoxNameを指定せずRoleを登録し、BoxNameに存在しないBox名を指定してRoleを削除した場合404が返却されること.
     */
    @Test
    public void BoxNameを指定せずRoleを登録しBoxNameに存在しないBox名を指定してRoleを削除した場合404が返却されること() {
        String dummyBoxName = "dummy";
        TResponse res = null;
        try {
            res = CellCtlUtils.createRole(cellName, testRoleName);
        } finally {
            Http.request("role-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("rolename", testRoleName)
                    .with("boxname", "'" + dummyBoxName + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を削除する.
     * @param boxname
     */
    private void deleteRole(String boxname) {
        TResponse res = Http.request("role-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rolename", testRoleName)
                .with("boxname", "'" + boxname + "'")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // レスポンスヘッダーのチェック
        // DataServiceVersion
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * ボックス名にリンクされていないロール情報を削除する.
     * @param boxname
     */
    private void deleteRole() {
        TResponse res = Http.request("role-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rolename", testRoleName)
                .with("boxname", "null")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // レスポンスヘッダーのチェック
        // DataServiceVersion
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }
}

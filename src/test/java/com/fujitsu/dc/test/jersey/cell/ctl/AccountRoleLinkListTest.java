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

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AccountとRoleのリンク一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AccountRoleLinkListTest extends AccountTest {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountRoleLinkListTest() {
        super();
    }

    /**
     * AccountとRoleのリンクを作成し一覧取得できること.
     */
    @Test
    public void AccountとRoleのリンクを作成し一覧取得できること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link
            createAccountRoleLink(testAccountName, roleUrl);

            // アカウント・ロールの一覧取得
            TResponse resList = Http.request("link-account-role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(roleUrl);
            // レスポンスボディのチェック
            checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteLinks(testAccountName, testRoleName);
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのリンクを作成し更新できないこと.
     */
    @Test
    public void AccountとRoleのリンクを作成し更新できないこと() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String testLinkPath = "__ctl/Account\\('" + testAccountName + "'\\)/\\$links/_Role";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link
            createAccountRoleLink(testAccountName, roleUrl);

            // アカウント・ロールの更新
            Http.request("link-update-with-body.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("linkPath", testLinkPath)
                    .with("body", "{\"Name\":\"testRole\"}")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteLinks(testAccountName, testRoleName);
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのリンクを複数作成し一覧取得できること.
     */
    @Test
    public void AccountとRoleのリンクを複数作成し一覧取得できること() {
        String testRoleName = "testRole";
        String testRoleName2 = "testRole2";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;
        String roleUrl2 = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);
            roleUrl2 = this.createRole(testRoleName2);

            // アカウント・ロールの$link
            createAccountRoleLink(testAccountName, roleUrl);
            createAccountRoleLink(testAccountName, roleUrl2);

            // アカウント・ロールの一覧取得
            TResponse resList = Http.request("link-account-role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(roleUrl);
            expectedUriList.add(roleUrl2);
            // レスポンスボディのチェック
            checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteLinks(testAccountName, testRoleName);
            deleteLinks(testAccountName, testRoleName2);
            deleteRole(roleUrl);
            deleteRole(roleUrl2);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとBoxと紐付いたRoleのリンクを複数作成し一覧取得できること.
     */
    @Test
    public void AccountとBoxと紐付いたRoleのリンクを複数作成し一覧取得できること() {
        String testRoleName = "testRole";
        String testRoleName2 = "testRole2";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        TResponse role = null;
        TResponse role2 = null;
        String roleUrl = null;
        String roleUrl2 = null;

        try {
            createBox2(Setup.TEST_BOX1, null);
            createBox2(Setup.TEST_BOX2, null);
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            role = createRole(testRoleName, Setup.TEST_BOX1);
            role2 = createRole(testRoleName2, Setup.TEST_BOX2);

            roleUrl = role.getHeader("Location");
            roleUrl2 = role2.getHeader("Location");

            // アカウント・ロールの$link
            createAccountRoleLink(testAccountName, roleUrl);
            createAccountRoleLink(testAccountName, roleUrl2);

            // アカウント・ロールの一覧取得
            TResponse resList = Http.request("link-account-role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(roleUrl);
            expectedUriList.add(roleUrl2);
            // レスポンスボディのチェック
            checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteLinks(testAccountName, testRoleName, Setup.TEST_BOX1);
            deleteLinks(testAccountName, testRoleName2, Setup.TEST_BOX2);
            deleteRole(roleUrl);
            deleteRole(roleUrl2);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのリンクを未作成で一覧取得できること.
     */
    @Test
    public void AccountとRoleのリンクを未作成で一覧取得できること() {
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);

            // アカウント・ロールの一覧取得
            TResponse resList = Http.request("link-account-role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            ArrayList<String> expectedUriList = new ArrayList<String>();
            // レスポンスボディのチェック
            checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            deleteAccount(accountUrl);
        }
    }

    private void createAccountRoleLink(String accountName, String roleUrl) {
        Http.request("link-account-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", accountName)
                .with("roleUrl", roleUrl)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .debug();
    }

    private void deleteAccount(String accountUrl) {
        DcRequest req = DcRequest.delete(accountUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        request(req);
    }

    private void deleteRole(String roleUrl) {
        DcRequest req = DcRequest.delete(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        request(req);
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する.
     * @param roleName
     * @param boxname
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    private TResponse createRole(String roleName, String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);
        body.put("_Box.Name", boxname);

        return Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .debug();
    }

    /**
     * リンク情報を作成する.
     * @param accountname 削除対象のAccountのName
     * @param roleUrl 削除対象のRoleのuri
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    protected TResponse createLinks(String accountname, String roleUrl, int code) {
        // リクエスト実行
        return Http.request("link-account-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", accountname)
                .with("roleUrl", roleUrl)
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * リンク情報を作成する(ボディのuriがnull).
     * @param accountname 削除対象のAccountのName
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    protected TResponse createLinksLinkNull(String accountname, int code) {
        // リクエスト実行
        return Http.request("link-account-roleWithBodyNull.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", accountname)
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * リンク情報を削除する.
     * @param accountname 削除対象のAccountのName
     * @param rolename 削除対象のRoleのName
     * @param boxName 削除対象のRoleのBox名
     * @return レスポンス
     */
    protected TResponse deleteLinks(String accountname, String rolename, String boxName) {
        // リクエスト実行
        return Http.request("cell/link-delete-account-role.txt")
                .with("cellPath", cellName)
                .with("accountKey", accountname)
                .with("roleKey", "Name='" + rolename + "',_Box.Name='" + boxName + "'")
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns();
    }

    /**
     * リンク情報を削除する.
     * @param accountname 削除対象のAccountのName
     * @param rolename 削除対象のRoleのName
     * @return レスポンス
     */
    protected TResponse deleteLinks(String accountname, String rolename) {
        // リクエスト実行
        return Http.request("cell/link-delete-account-role.txt")
                .with("cellPath", cellName)
                .with("accountKey", accountname)
                .with("roleKey", "Name='" + rolename + "',_Box.Name=null")
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns();
    }
}

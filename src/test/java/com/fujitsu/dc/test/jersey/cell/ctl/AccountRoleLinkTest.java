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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
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
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * AccountとRoleのリンクテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AccountRoleLinkTest extends AccountTest {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountRoleLinkTest() {
        super();
    }

    /**
     * AccountとRoleのリンクを作成し２０４が返却されること.
     */
    @Test
    public void AccountとRoleのリンクを作成し２０４が返却されること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", roleUrl)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT)
                    .debug();

        } finally {
            deleteLinks(testAccountName, testRoleName);
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }

    }

    /**
     * AccountとRoleのリンクの作成で存在しないRoleを指定して４００が返却されること.
     */
    @Test
    public void AccountとRoleのリンクの作成で存在しないRoleを指定して４００が返却されること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            String testRoleUri = roleUrl.substring(0, roleUrl.indexOf("("));
            testRoleUri += "(Name='dummyRole',_Box.Name=null)";

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", testRoleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }

    }

    /**
     * AccountとRoleのlink作成時uriの値に前丸カッコがない場合400になること.
     */
    @Test
    public void AccountとRoleのlink作成時uriの値に前丸カッコがない場合400になること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            String testRoleUri = roleUrl.substring(0, roleUrl.indexOf("("));
            testRoleUri += "Name='testRole',_Box.Name=null)";

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", testRoleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのlink作成時Roleが複合キーかつuriの値に後ろ丸カッコがない場合400になること.
     */
    @Test
    public void AccountとRoleのlink作成時Roleが複合キーかつuriの値に後ろ丸カッコがない場合400になること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            String testRoleUri = roleUrl.substring(0, roleUrl.indexOf(")"));

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", testRoleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのlink作成時Roleが単一キー指定かつuriの値に後ろ丸カッコがない場合400になること.
     */
    @Test
    public void AccountとRoleのlink作成時Roleが単一キー指定かつuriの値に後ろ丸カッコがない場合400になること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            String testRoleUri = roleUrl.substring(0, roleUrl.lastIndexOf(","));

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", testRoleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * AccountとRoleのリンクの作成で存在しないAccountを指定して４０４が返却されること.
     */
    @Test
    public void AccountとRoleのリンクの作成で存在しないAccountを指定して４０４が返却されること() {
        String testRoleName = "testRole";
        String testAccountName = "dummy_account";
        String roleUrl = null;

        try {
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link
            Http.request("link-account-role.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", testAccountName)
                    .with("roleUrl", roleUrl)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .debug();

        } finally {
            deleteRole(roleUrl);
        }

    }

    /**
     * AccountとRoleのリンクの削除で存在しないRoleを指定して４０４が返却されること.
     */
    @Test
    public void AccountとRoleのリンクの削除で存在しないRoleを指定して４０４が返却されること() {
        String testRoleName = "dummyRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);

            // アカウント・ロールの$link削除
            deleteLinks(testAccountName, testRoleName).statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteAccount(accountUrl);
        }

    }

    /**
     * AccountとRoleのリンクの削除で存在しないAccountを指定して４０４が返却されること.
     */
    @Test
    public void AccountとRoleのリンクの削除で存在しないAccountを指定して４０４が返却されること() {
        String testRoleName = "testRole";
        String testAccountName = "dummy_account";
        String roleUrl = null;

        try {
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link削除
            deleteLinks(testAccountName, testRoleName).statusCode(HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteRole(roleUrl);
        }

    }

    /**
     * リクエストボディのuriが空文字の場合に400になること.
     */
    @Test
    public void リクエストボディのuriが空文字の場合に400になること() {
        String testAccountName = "dummy_account";
        String roleUrl = "";

        // アカウント・ロールの$link.roleUrlが空なので400になる
        createLinks(testAccountName, roleUrl, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * AccountとRoleのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること.
     */
    @Test
    public final void AccountとRoleのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること() {
        String testRoleName = "testRole";
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accountUrl = null;
        String roleUrl = null;

        try {
            accountUrl = this.createAccount(testAccountName, testAccountPass);
            roleUrl = this.createRole(testRoleName);

            // アカウント・ロールの$link
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("entitySet", "Account")
                    .with("key", "'test_account'")
                    .with("navProp", "_Hoge")
                    .with("uri", roleUrl)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteRole(roleUrl);
            deleteAccount(accountUrl);
        }
    }

    /**
     * リクエストボディのuriがuri形式ではない場合に400になること.
     */
    @Test
    public void リクエストボディのuriがuri形式ではない場合に400になること() {
        String testAccountName = "dummy_account";
        String roleUrl = "noturi";

        // アカウント・ロールの$link.roleUrlがuri形式ではないので400になる
        createLinks(testAccountName, roleUrl, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * リクエストボディのuriがnullの場合に400になること.
     */
    @Test
    public void リクエストボディのuriがnullの場合に400になること() {
        String testAccountName = "dummy_account";

        // アカウント・ロールの$link.roleUrlがnullなので400になる
        createLinksLinkNull(testAccountName, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * リクエストボディのuriにパイプ文字(|)が含まれている場合に204になること.
     */
    @Test
    public void リクエストボディのuriにパイプ文字が含まれている場合に204になること() {
        String testRoleName = "testRole";
        String testAccountName = "test|account";
        String testAccountName4Uri = "test%7Caccount";
        String testAccountPass = "password";
        String accountUrl = null;

        try {
            // Account登録
            TResponse accountRes = AccountUtils.create(MASTER_TOKEN_NAME, Setup.TEST_CELL1, testAccountName,
                    testAccountPass, HttpStatus.SC_CREATED);
            accountUrl = accountRes.getLocationHeader();
            // Role登録
            RoleUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, testRoleName,
                    HttpStatus.SC_CREATED);

            // Role - Accountの$linksの登録を行う
            // ボディ：{"uri": "http://localhost:9998/testcell1/__ctl/Account('test|account')"}
            createRoleAccountLinks(Setup.TEST_CELL1, accountUrl, testRoleName, HttpStatus.SC_NO_CONTENT);
        } finally {
            AccountUtils.deleteLinksWithRole(Setup.TEST_CELL1, null, MASTER_TOKEN_NAME, testAccountName4Uri,
                    testRoleName, -1);
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, testAccountName4Uri, -1);
            RoleUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, null, testRoleName, -1);

        }
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
     * Account-Roleのリンク情報を作成する.
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
     * Role-Accountのリンク情報を作成する.
     * @param testCellName Cell名
     * @param accountUri 削除対象のAccountのUri
     * @param roleName 削除対象のRoleのuri
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    protected TResponse createRoleAccountLinks(String testCellName, String accountUri, String roleName, int code) {
        // リクエスト実行
        return Http.request("link-role-account.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", testCellName)
                .with("rolename", roleName)
                .with("accountUri", accountUri)
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

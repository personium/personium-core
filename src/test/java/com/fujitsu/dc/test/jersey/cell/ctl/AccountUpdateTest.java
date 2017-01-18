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
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import com.fujitsu.dc.core.model.lock.LockManager;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.cell.auth.AuthTestCommon;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Accountの作成のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class AccountUpdateTest extends ODataCommon {

    private final String cellName = "testcell1";
    private final String orgUserName = "account0";
    private final String orgPass = "password0";
    private String userName = orgUserName;

    /**
     * 前処理.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * 後処理.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountUpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * パスワード認証.
     * @param usr アカウント名
     * @param pwd パスワード
     * @param sc ステータスコード
     */
    private void auth(String usr, String pwd, int sc) {
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", usr)
                .with("password", pwd)
                .returns();
        res.statusCode(sc);
    }

    /**
     * アカウント更新.
     * @param newUsername アカウント名
     * @param newPassword パスワード
     * @param sc ステータスコード
     */
    private void updatePwd(String newUsername, String newPassword, int sc) {
        TResponse res = Http.request("account-update.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("newUsername", newUsername)
                .returns().debug();
        res.statusCode(sc);
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * アカウント更新（アカウント名のみ）.
     * @param newUsername アカウント名
     * @param sc ステータスコード
     */
    private void updatePwd(String newUsername, int sc) {
        TResponse res = Http.request("account-update-accountname.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("newUsername", newUsername)
                .returns().debug();
        res.statusCode(sc);
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * パスワードとアカウント名を更新して204が返却されること.
     */
    @Test
    public final void パスワードとアカウント名を更新して204が返却されること() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(newUserName, newPassword, HttpStatus.SC_NO_CONTENT);

        // 認証可能を確認
        this.auth(newUserName, newPassword, HttpStatus.SC_OK);

        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.userName = newUserName;
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * アカウント名のみを更新して204が返却されること.
     */
    @Test
    public final void アカウント名のみを更新して204が返却されること() {
        String newUserName = "account999";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(newUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);

        // 認証可能を確認
        this.auth(newUserName, this.orgPass, HttpStatus.SC_OK);

        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.userName = newUserName;
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * パスワードのみを更新して204が返却されること.
     */
    @Test
    public final void パスワードのみを更新して204が返却されること() {
        String newPassword = "new_password0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(this.orgUserName, newPassword, HttpStatus.SC_NO_CONTENT);

        // 認証可能を確認
        this.auth(this.orgUserName, newPassword, HttpStatus.SC_OK);

        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * XDcCredentialヘッダにパスワード指定なしでアカウント名のみを更新して204が返却されること.
     */
    @Test
    public final void XDcCredentialヘッダにパスワード指定なしでアカウント名のみを更新して204が返却されること() {
        String newUserName = "account999";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(newUserName, HttpStatus.SC_NO_CONTENT);

        // パスワードなしで認証可能にしてから以下を有効にする
        // // 認証可能を確認
        // this.auth(newUserName, "", HttpStatus.SC_OK);
        //
        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.userName = newUserName;
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * アカウントを更新時にLastAuthenticatedを省略した場合nullで更新されること.
     */
    @Test
    public final void アカウントを更新時にLastAuthenticatedを省略した場合nullで更新されること() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String getLastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("LastAuthenticated");
            assertNull(getLastAuthenticated);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * アカウントを更新時にLastAuthenticatedにnullを指定して更新されること.
     */
    @Test
    public final void アカウントを更新時にLastAuthenticatedにnullを指定して更新されること() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, null, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String getLastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("LastAuthenticated");
            assertNull(getLastAuthenticated);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * アカウントを更新時にLastAuthenticatedに時刻を指定して更新されること.
     */
    @Test
    public final void アカウントを更新時にLastAuthenticatedに時刻を指定して更新されること() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, "/Date(1414656074074)/", HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String getLastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("LastAuthenticated");
            assertEquals("/Date(1414656074074)/", getLastAuthenticated);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * アカウントを更新時にLastAuthenticatedに不正な書式を指定して400エラーとなること.
     */
    @Test
    public final void アカウントを更新時にLastAuthenticatedに不正な書式を指定して400エラーとなること() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, "/Date(1359340262406/", HttpStatus.SC_BAD_REQUEST);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String getLastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("LastAuthenticated");
            assertNull(getLastAuthenticated);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * アカウント更新のリクエストボディに管理情報__publishedを指定してレスポンスコード400が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void アカウント更新のリクエストボディに管理情報__publishedを指定してレスポンスコード400が返却されること() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", newUserName);
        updateBody.put(PUBLISHED, "/Date(0)/");

        // Account更新
        Http.request("account-update-without-body.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        // 更新失敗したAccountでは認証不可であることを確認
        this.auth(newUserName, newPassword, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // 元のユーザパスワードで認証可能であることを確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);
    }

    /**
     * アカウント更新のリクエストボディに管理情報__updatedを指定してレスポンスコード400が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void アカウント更新のリクエストボディに管理情報__updatedを指定してレスポンスコード400が返却されること() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", newUserName);
        updateBody.put(UPDATED, "/Date(0)/");

        // Account更新
        Http.request("account-update-without-body.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        // 更新失敗したAccountでは認証不可であることを確認
        this.auth(newUserName, newPassword, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // 元のユーザパスワードで認証可能であることを確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);
    }

    /**
     * アカウント更新のリクエストボディに管理情報__metadataを指定してレスポンスコード400が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void アカウント更新のリクエストボディに管理情報__metadataを指定してレスポンスコード400が返却されること() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", newUserName);
        updateBody.put(METADATA, "test");

        // Account更新
        Http.request("account-update-without-body.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        // 更新失敗したAccountでは認証不可であることを確認
        this.auth(newUserName, newPassword, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        // 元のユーザパスワードで認証可能であることを確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);
    }
}

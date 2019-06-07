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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import io.personium.core.model.ctl.Account;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.cell.auth.AuthTestCommon;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

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
        super(new PersoniumCoreApplication());
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
        String newPassword = "new_password0!";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(newUserName, newPassword, HttpStatus.SC_NO_CONTENT);

        // 認証可能を確認
        this.auth(newUserName, newPassword, HttpStatus.SC_OK);

        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.userName = newUserName;
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * パスワードのみを更新して204が返却されること.
     */
    @Test
    public final void パスワードのみを更新して204が返却されること() {
        String newPassword = "new_p@ssword0";
        // 認証可能を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);

        // Account更新
        this.updatePwd(this.orgUserName, newPassword, HttpStatus.SC_NO_CONTENT);

        // 認証可能を確認
        this.auth(this.orgUserName, newPassword, HttpStatus.SC_OK);

        // 元のユーザパスワードでは認証不可を確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

        // Accountを戻しておく
        this.userName = newUserName;
        this.updatePwd(this.orgUserName, this.orgPass, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * When IPAddressRange is omitted, it is updated with null.
     */
    @Test
    public final void update_IPAddressRange_not_set() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    "192.127.0.2", HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertNull(ipAddressRange);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When IPAddressRange is set null It is updated with null.
     */
    @Test
    public final void update_IPAddressRange_is_null() {
        String updateUserName = "account1999";
        String updatePass = "password19999";
        String updateIPAddressRange = null;

        try {
            // Account作成
            AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    "192.127.0.2", HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.updateWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updateUserName, orgPass, updateIPAddressRange, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(ipAddressRange, is(updateIPAddressRange));
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When IPAddressRange is set It is updated with that value.
     */
    @Test
    public final void update_IPAddressRange_is_set_value() {
        String updateUserName = "account1999";
        String updatePass = "password19999";
        String updateIPAddressRange = "192.0.1.0,192.0.2.0/24,192.0.3.0";

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.updateWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updateUserName, orgPass, updateIPAddressRange, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(ipAddressRange, is(updateIPAddressRange));
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When IPAddressRange is set illegal format It is result in 400 error.
     */
    @Test
    public final void update_IPAddressRange_is_set_illgal_format() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        String newIPAddressRange = "illegal_format";

        // Account更新
        AccountUtils.updateWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName, userName, newUserName,
                newPassword, newIPAddressRange, HttpStatus.SC_BAD_REQUEST);

        // Confirm that the IPAddressRange is not updated
        TResponse res = AccountUtils
                .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, this.orgUserName);
        String ipAddressRange = getResponseResultParam(res, "IPAddressRange");
        assertNull(ipAddressRange);
    }

    /**
     * When status is omitted, it is updated with "active".
     */
    @Test
    public final void update_status_not_set() {
        String updateUserName = "account1999";
        String updatePass = "password19999";

        try {
            // Account作成
            AccountUtils.createWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    Account.STATUS_DEACTIVATED, HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updateUserName,
                    orgPass, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String status = getResponseResultParam(res, "Status");
            assertEquals(Account.STATUS_ACTIVE, status);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When status is set null It is updated with "active".
     */
    @Test
    public final void update_status_is_null() {
        String updateUserName = "account1999";
        String updatePass = "password19999";
        String updateStatus = null;

        try {
            // Account作成
            AccountUtils.createWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    Account.STATUS_DEACTIVATED, HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.updateWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updateUserName, orgPass, updateStatus, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String status = getResponseResultParam(res, "Status");
            assertEquals(Account.STATUS_ACTIVE, status);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When status is set It is updated with that value.
     */
    @Test
    public final void update_status_is_set_value() {
        String updateUserName = "account1999";
        String updatePass = "password19999";
        String updateStatus = Account.STATUS_DEACTIVATED;

        try {
            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);
            // Account更新
            AccountUtils.updateWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updateUserName, orgPass, updateStatus, HttpStatus.SC_NO_CONTENT);
            // Account取得
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String status = getResponseResultParam(res, "Status");
            assertThat(status, is(updateStatus));
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * When status is set illegal format It is result in 400 error.
     */
    @Test
    public final void update_status_is_set_illgal_format() {
        String newUserName = "account999";
        String newPassword = "new_password0";
        String newStatus = "illegal_format";

        // Account update filled.
        AccountUtils.updateWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName, userName, newUserName,
                newPassword, newStatus, HttpStatus.SC_BAD_REQUEST);

        // Confirm that the status is not updated
        TResponse res = AccountUtils
                .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, this.orgUserName);
        String status = getResponseResultParam(res, "Status");
        assertThat(status, is(Account.STATUS_ACTIVE));
    }

    /**
     * merge test.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void merge() {
        String updateUserName = "test_merge";
        String updatePass = "testPassword";

        try {
            // Create test account.
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);

            // Set all properties and merge.
            JSONObject updateBody = new JSONObject();
            updateBody.put("Name", updateUserName);
            updateBody.put("Type", "oidc:google");
            updateBody.put("Status", Account.STATUS_DEACTIVATED);
            updateBody.put("IPAddressRange", "192.0.1.0");
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, updateBody, HttpStatus.SC_NO_CONTENT);
            // Get account. (Check properties)
            TResponse res = AccountUtils
                    .get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            String type = getResponseResultParam(res, "Type");
            String status = getResponseResultParam(res, "Status");
            String ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(type, is("oidc:google"));
            assertThat(status, is(Account.STATUS_DEACTIVATED));
            assertThat(ipAddressRange, is("192.0.1.0"));

            // Set only required properties and merge.
            JSONObject mergeBody = new JSONObject();
            mergeBody.put("Name", updateUserName);
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, mergeBody, HttpStatus.SC_NO_CONTENT);
            // Get account. (Check properties)
            res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            type = getResponseResultParam(res, "Type");
            status = getResponseResultParam(res, "Status");
            ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(type, is("oidc:google"));
            assertThat(status, is(Account.STATUS_DEACTIVATED));
            assertThat(ipAddressRange, is("192.0.1.0"));

            // Set only required properties and put.
            mergeBody.put("Name", updateUserName);
            AccountUtils.updateWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, mergeBody, HttpStatus.SC_NO_CONTENT);
            // Get account. (Check properties)
            res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            type = getResponseResultParam(res, "Type");
            status = getResponseResultParam(res, "Status");
            ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(type, is("basic"));
            assertThat(status, is(Account.STATUS_ACTIVE));
            assertNull(ipAddressRange);

            // Set null to any property and merge.
            mergeBody.put("Name", updateUserName);
            mergeBody.put("Type", null);
            mergeBody.put("Status", null);
            mergeBody.put("IPAddressRange", null);
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, mergeBody, HttpStatus.SC_NO_CONTENT);
            // Get account. (Check properties)
            res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            type = getResponseResultParam(res, "Type");
            status = getResponseResultParam(res, "Status");
            ipAddressRange = getResponseResultParam(res, "IPAddressRange");
            assertThat(type, is("basic"));
            assertThat(status, is(Account.STATUS_ACTIVE));
            assertNull(ipAddressRange);

            // Update status only (Name does not change)
            mergeBody = new JSONObject();
            mergeBody.put("Status", Account.STATUS_DEACTIVATED);
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, mergeBody, HttpStatus.SC_NO_CONTENT);
            // Get account. (Check properties)
            res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, updateUserName);
            status = getResponseResultParam(res, "Status");
            assertThat(type, is("basic"));
            assertThat(status, is(Account.STATUS_DEACTIVATED));
            assertNull(ipAddressRange);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * merge test.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void merge_invalid() {
        String updateUserName = "test_merge_invalid";
        String updatePass = "testPassword";

        try {
            // Create test account.
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName, updatePass,
                    HttpStatus.SC_CREATED);

            // Invalid Name.
            JSONObject updateBody = new JSONObject();
            updateBody.put("Name", null);
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, updateBody, HttpStatus.SC_BAD_REQUEST);

            // Invalid type.
            updateBody = new JSONObject();
            updateBody.put("Name", updateUserName);
            updateBody.put("Type", "invalid_Type");
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, updateBody, HttpStatus.SC_BAD_REQUEST);

            // Invalid status.
            updateBody = new JSONObject();
            updateBody.put("Name", updateUserName);
            updateBody.put("Status", "invalid_Status");
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, updateBody, HttpStatus.SC_BAD_REQUEST);

            // Invalid ip address range.
            updateBody = new JSONObject();
            updateBody.put("Name", updateUserName);
            updateBody.put("IPAddressRange", "invalid_IPAddressRange");
            AccountUtils.mergeWithBody(AbstractCase.MASTER_TOKEN_NAME, cellName, updateUserName,
                    updatePass, updateBody, HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, updateUserName, -1);
        }
    }

    /**
     * Get response result param.
     * @param res response
     * @param paramName parameter key name
     * @return parameter value
     */
    private String getResponseResultParam(TResponse res, String paramName) {
        return (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                .get("results"))
                .get(paramName);
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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

        // 元のユーザパスワードで認証可能であることを確認
        this.auth(this.orgUserName, this.orgPass, HttpStatus.SC_OK);
    }
}

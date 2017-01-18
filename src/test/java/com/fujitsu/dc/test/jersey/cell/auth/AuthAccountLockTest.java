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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.model.lock.LockManager;
import com.fujitsu.dc.core.rs.cell.TokenEndPointResource;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * 認証のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthAccountLockTest extends JerseyTest {

    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_APP_CELL1 = "schema1";

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
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(TokenEndPointResource.class);

    /**
     * コンストラクタ.
     */
    public AuthAccountLockTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * パスワード認証失敗後1秒以内に成功する認証をリクエストした場合400が返却されること(PR400-AN-0019).
     * com.fujitsu.dc.core.lock.accountlock.timeを1秒に設定すると失敗するためIgnore
     */
    @Test
    @Ignore
    public final void パスワード認証失敗後1秒以内に成功する認証をリクエストした場合400が返却されること() {
        // パスワード認証1回目_ 不正なパスワード指定(400エラー(PR400-AN-0017))
        TResponse passRes = requestAuthorization(TEST_CELL1, "account1",
                "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // 1秒以内にパスワード認証(400エラー(PR400-AN-0019))
        passRes = requestAuthorization(TEST_CELL1, "account1", "password1", HttpStatus.SC_BAD_REQUEST);
        body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0019]"));

        AuthTestCommon.waitForAccountLock();
    }

    /**
     * パスワード認証失敗後1秒以内に失敗する認証をリクエストした場合400が返却されること(PR400-AN-0019).
     * com.fujitsu.dc.core.lock.accountlock.timeを1秒に設定すると失敗するためIgnore
     */
    @Test
    @Ignore
    public final void パスワード認証失敗後1秒以内に失敗する認証をリクエストした場合400が返却されること() {
        // パスワード認証1回目_ 不正なパスワード指定(400エラー(PR400-AN-0017))
        TResponse passRes = requestAuthorization(TEST_CELL1, "account1",
                "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // 1秒以内にパスワード認証(400エラー(PR400-AN-0019))
        passRes = requestAuthorization(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0019]"));

        AuthTestCommon.waitForAccountLock();
    }

    /**
     * パスワード認証失敗後1秒後に成功する認証をリクエストした場合200が返却されること.
     */
    @Test
    public final void パスワード認証失敗後1秒後に成功する認証をリクエストした場合200が返却されること() {
        // パスワード認証1回目_ 不正なパスワード指定(400エラー(PR400-AN-0017))
        TResponse passRes = requestAuthorization(TEST_CELL1, "account1",
                "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.debug("");
        }
        // 1秒後にパスワード認証(認証成功)
        passRes = requestAuthorization(TEST_CELL1, "account1", "password1", HttpStatus.SC_OK);
        body = (String) passRes.bodyAsJson().get("access_token");
        assertNotNull(body);
    }

    /**
     * パスワード認証失敗後1秒後に失敗する認証をリクエストした場合400が返却されること(PR400-AN-0017).
     */
    @Test
    public final void パスワード認証失敗後1秒後に失敗する認証をリクエストした場合400が返却されること() {
        // パスワード認証1回目_ 不正なパスワード指定(400エラー(PR400-AN-0017))
        TResponse passRes = requestAuthorization(TEST_CELL1, "account1",
                "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.debug("");
        }

        // 1秒後にパスワード認証(400エラー(PR400-AN-0017))
        passRes = requestAuthorization(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));
        AuthTestCommon.waitForAccountLock();
    }

    private TResponse requestAuthorization(String cellName, String userName, String password, int code) {
        TResponse passRes =
                Http.request("authn/password-cl-c0.txt")
                        .with("remoteCell", cellName)
                        .with("username", userName)
                        .with("password", password)
                        .returns()
                        .statusCode(code);
        return passRes;
    }

}

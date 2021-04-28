/**
 * Personium
 * Copyright 2019-2021 Personium Project Authors
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
package io.personium.test.jersey.cell.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * account status test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthAccountStatusTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellstatus";
    /** test account name. */
    private static final String TEST_ACCOUNT_ACTIVE = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT_DEACTIVATED = "account2";
    /** test account name. */
    private static final String TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED = "account3";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD,
                Account.STATUS_ACTIVE, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_DEACTIVATED, TEST_PASSWORD,
                Account.STATUS_DEACTIVATED, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED,
                TEST_PASSWORD, Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_ACTIVE, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_DEACTIVATED, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
        LockManager.deleteAllLocks();
    }

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthAccountStatusTest.class);

    /**
     * constructor.
     */
    public AuthAccountStatusTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test whether to authenticate by status.
     */
    @Test
    public final void test_handlePassword() {
        // account active.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD, HttpStatus.SC_OK);

        // account suspended.
        TResponse passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT_DEACTIVATED, TEST_PASSWORD,
                HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // account password change required.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, "error", HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock();
        passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, TEST_PASSWORD,
                HttpStatus.SC_UNAUTHORIZED);
        JSONObject responseBody = passRes.bodyAsJson();
        String error = (String) responseBody.get("error");
        String errorDescription = (String) responseBody.get("error_description");
        String aToken = (String) responseBody.get("access_token");
        String url = (String) responseBody.get("url");
        String lastAuthenticated = (String) responseBody.get("last_authenticated");
        Long failedCount = (Long) responseBody.get("failed_count");
        assertThat(error, is(OAuth2Helper.Error.INVALID_GRANT));
        assertThat(errorDescription,
                is("[PR401-AN-0023] - The password should be changed."));
        assertTrue(aToken.startsWith(PasswordChangeAccessToken.PREFIX_ACCESS));
        assertTrue(url.endsWith("/__mypassword"));
        assertNull(lastAuthenticated);
        assertThat(failedCount, is(1L));
        passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, TEST_PASSWORD,
                HttpStatus.SC_UNAUTHORIZED);
        assertNull(lastAuthenticated);
        assertThat(failedCount, is(1L));
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthentication(String cellName, String userName, String password, int code) {
        TResponse passRes = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", userName)
                .with("password", password)
                .returns()
                .statusCode(code);
        return passRes;
    }
}

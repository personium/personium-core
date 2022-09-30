/**
 * Personium
 * Copyright 2019-2022 Personium Project Authors
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

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.UrlUtils;

/**
 * account status test for authorization.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzAccountStatusTest extends PersoniumTest {

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthzAccountStatusTest.class);

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthzaccountstatus";
    /** test box name. */
    private static final String TEST_BOX = "testboxauthzaccountstatus";
    /** test account name. */
    private static final String TEST_ACCOUNT_ACTIVE = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT_SUSPENDED = "account2";
    /** test account name. */
    private static final String TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED = "account3";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * constructor.
     */
    public AuthzAccountStatusTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.createWithSchema(TEST_CELL, TEST_BOX, AbstractCase.MASTER_TOKEN_NAME,
                UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD,
                Account.STATUS_ACTIVE, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD,
                Account.STATUS_DEACTIVATED, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED,
                TEST_PASSWORD, Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_ACTIVE, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_SUSPENDED, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, -1);
        BoxUtils.delete(TEST_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * Test whether to authenticate by status.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePassword() throws Exception {
        // account active.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // account suspended.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));

        // account password change required.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0006"));
    }

    /**
     * Test handle password change.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePasswordChange_normal() throws Exception {
        String testAccount = "test_handlePasswordChange";
        try {
            AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, testAccount,
                    TEST_PASSWORD, Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);

            // First authorization, Get password change access token.
            PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, testAccount, TEST_PASSWORD);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            Map<String, String> responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            String accessTokenStr = responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN);
            assertNotNull(accessTokenStr);
            assertTrue(Boolean.parseBoolean(responseMap.get(OAuth2Helper.Key.PASSWORD_CHANGE_REQUIRED)));

            // Password change success.
            String newPassword = "newpassword";
            res = requestAuthorizationPasswordChange(TEST_CELL, accessTokenStr, newPassword);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                    UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + "__/redirect.html"));
            responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertNotNull(responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN));

            // Authentication can be performed with the changed password.
            res = requestAuthorization4Authz(TEST_CELL, testAccount, newPassword);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertNotNull(responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN));

            // It can not be authenticated with the password before change
            res = requestAuthorization4Authz(TEST_CELL, testAccount, TEST_PASSWORD);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
        } finally {
            AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, testAccount, -1);
        }
    }

    /**
     * Test handle password change. invalid request etc.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePasswordChange_invalid() throws Exception {
        String testAccount = "test_handlePasswordChange_invalid";
        try {
            AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, testAccount,
                    TEST_PASSWORD, Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);

            // First authorization, Get password change access token.
            PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, testAccount, TEST_PASSWORD);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            Map<String, String> responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            String accessTokenStr = responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN);
            assertNotNull(accessTokenStr);
            assertTrue(Boolean.parseBoolean(responseMap.get(OAuth2Helper.Key.PASSWORD_CHANGE_REQUIRED)));

            // Password change failed, No pass.
            res = requestAuthorizationPasswordChange(TEST_CELL, accessTokenStr, "");
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0007"));
            assertNotNull(responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN));
            assertTrue(Boolean.parseBoolean(responseMap.get(OAuth2Helper.Key.PASSWORD_CHANGE_REQUIRED)));

            // Password change failed, Password invalid format.
            res = requestAuthorizationPasswordChange(TEST_CELL, accessTokenStr, "error");
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0008"));
            assertNotNull(responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN));
            assertTrue(Boolean.parseBoolean(responseMap.get(OAuth2Helper.Key.PASSWORD_CHANGE_REQUIRED)));

            // Password change failed, invalid token.
            res = requestAuthorizationPasswordChange(TEST_CELL, "dummy_token", "newpassword");
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0002"));
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ACCESS_TOKEN));
            assertFalse(Boolean.parseBoolean(responseMap.get(OAuth2Helper.Key.PASSWORD_CHANGE_REQUIRED)));

            // Authentication can be performed with the password before change. (account password change required)
            res = requestAuthorization4Authz(TEST_CELL, testAccount, TEST_PASSWORD);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0006"));
            assertNotNull(responseMap.get(OAuth2Helper.Key.ACCESS_TOKEN));
        } finally {
            AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, testAccount, -1);
        }
    }

    /**
     * request authorization.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @return http response
     */
    private PersoniumResponse requestAuthorization4Authz(String cellName, String userName, String password)
            throws PersoniumException {
        PersoniumResponse dcRes = CellUtils.implicitflowAuthenticate(cellName, Setup.TEST_CELL_SCHEMA1, userName,
                password, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, null);
        return dcRes;
    }

    /**
     * request authorization password change.
     * @param cellName cell name
     * @param apTokenStr password change access token string
     * @param password password
     * @return http response
     */
    private PersoniumResponse requestAuthorizationPasswordChange(String cellName, String apTokenStr, String password)
            throws PersoniumException {
        PersoniumResponse dcRes = CellUtils.implicitflowAuthenticatePasswordChange(cellName, Setup.TEST_CELL_SCHEMA1,
                apTokenStr, password, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, null);
        return dcRes;
    }
}

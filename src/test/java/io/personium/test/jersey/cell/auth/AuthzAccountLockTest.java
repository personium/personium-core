/**
 * Personium
 * Copyright 2019-2020 Personium Project Authors
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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
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
 * account lock test for authorization.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzAccountLockTest extends PersoniumTest {

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthzAccountLockTest.class);

    private static final int TEST_ACCOUNTLOCK_COUNT = 3;
    private static final int TEST_ACCOUNTLOCK_TIME = 5;

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthzaccountlock";
    /** test box name. */
    private static final String TEST_BOX = "testboxauthzaccountlock";
    /** test account name. */
    private static final String TEST_ACCOUNT1 = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT2 = "account2";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * constructor.
     */
    public AuthzAccountLockTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * before class.
     * @throws Exception Unexpected exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Rewrite "accountLockCount" as test parameters.
        Field field = LockManager.class.getDeclaredField("accountLockCount");
        field.setAccessible(true);
        field.set(LockManager.class, TEST_ACCOUNTLOCK_COUNT);
        // Rewrite "accountLockTime" as test parameters.
        field = LockManager.class.getDeclaredField("accountLockTime");
        field.setAccessible(true);
        field.set(LockManager.class, TEST_ACCOUNTLOCK_TIME);
    }

    /**
     * after class.
     * @throws Exception Unexpected exception
     */
    @AfterClass
    public static void afterClass() throws Exception {
        // Restore "accountLockCount".
        Field field = LockManager.class.getDeclaredField("accountLockCount");
        field.setAccessible(true);
        field.set(LockManager.class, PersoniumUnitConfig.getAccountLockCount());
        // Restore "accountLockTime".
        field = LockManager.class.getDeclaredField("accountLockTime");
        field.setAccessible(true);
        field.set(LockManager.class, PersoniumUnitConfig.getAccountLockTime());
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
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD,
                HttpStatus.SC_CREATED);
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT2, TEST_PASSWORD,
                HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT1, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT2, -1);
        BoxUtils.delete(TEST_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * Tests that are account locked when the failure is greater than or equal to "authn.account.lockCount".
     * Tests that lock is released after "authn.account.lockTime" elapsed.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void lock_and_unlock() throws Exception {
        // before account lock.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // authentication failed repeatedly, account is locked.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT; i++) {
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, "error");
        }
        AuthTestCommon.waitForIntervalLock();
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(UrlUtils.cellRoot(TEST_CELL) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        responseMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(responseMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));

        // other account not locked.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT2, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // wait account lock expiration time (s). lock is released .
        try {
            Thread.sleep(1000 * TEST_ACCOUNTLOCK_TIME);
        } catch (InterruptedException e) {
            log.debug("");
        }
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT2, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
    }

    /**
     * Test that the failure count is reset on successful authentication.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void reset_failed_count() throws Exception {
        // first authenticated.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT - 1; i++) {
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, "error");
        }
        AuthTestCommon.waitForIntervalLock();
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // seccond authenticated.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT - 1; i++) {
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, "error");
        }
        AuthTestCommon.waitForIntervalLock();
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
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
}

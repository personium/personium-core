/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

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
 * account lock test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthAccountLockTest extends PersoniumTest {

    private static final int TEST_ACCOUNTLOCK_COUNT = 3;
    private static final int TEST_ACCOUNTLOCK_TIME = 5;

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthaccountlock";
    /** test account name. */
    private static final String TEST_ACCOUNT1 = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT2 = "account2";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

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
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthAccountLockTest.class);

    /**
     * constructor.
     */
    public AuthAccountLockTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Tests that are account locked when the failure is greater than or equal to "authn.account.lockCount".
     * Tests that lock is released after "authn.account.lockTime" elapsed.
     */
    @Test
    public final void lock_and_unlock() {
        // before account lock.
        requestAuthorization(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD, HttpStatus.SC_OK);

        // authentication failed repeatedly, account is locked.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT; i++) {
            requestAuthorization(TEST_CELL, TEST_ACCOUNT1, "error", HttpStatus.SC_BAD_REQUEST);
        }
        AuthTestCommon.waitForIntervalLock();
        TResponse passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD, HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // other account not locked.
        requestAuthorization(TEST_CELL, TEST_ACCOUNT2, TEST_PASSWORD, HttpStatus.SC_OK);

        // wait account lock expiration time (s). accountlock is released .
        try {
            Thread.sleep(1000 * TEST_ACCOUNTLOCK_TIME);
        } catch (InterruptedException e) {
            log.debug("");
        }
        requestAuthorization(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD, HttpStatus.SC_OK);
    }

    /**
     * Test that the failure count is reset on successful authentication.
     */
    @Test
    public final void reset_failed_count() {
        // first authenticated.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT - 1; i++) {
            requestAuthorization(TEST_CELL, TEST_ACCOUNT1, "error", HttpStatus.SC_BAD_REQUEST);
        }
        AuthTestCommon.waitForIntervalLock();
        requestAuthorization(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD, HttpStatus.SC_OK);

        // seccond authenticated.
        for (int i = 0; i < TEST_ACCOUNTLOCK_COUNT - 1; i++) {
            requestAuthorization(TEST_CELL, TEST_ACCOUNT1, "error", HttpStatus.SC_BAD_REQUEST);
        }
        AuthTestCommon.waitForIntervalLock();
        requestAuthorization(TEST_CELL, TEST_ACCOUNT1, TEST_PASSWORD, HttpStatus.SC_OK);
    }

    /**
     * request authorization.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthorization(String cellName, String userName, String password, int code) {
        TResponse passRes = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", userName)
                .with("password", password)
                .returns()
                .statusCode(code);
        return passRes;
    }
}

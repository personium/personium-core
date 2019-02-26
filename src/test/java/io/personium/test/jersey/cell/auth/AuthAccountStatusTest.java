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

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * account lock test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthAccountStatusTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthaccountstatus";
    /** test account name. */
    private static final String TEST_ACCOUNT_ACTIVE = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT_SUSPENDED = "account2";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

//    /**
//     * before class.
//     * @throws Exception Unexpected exception
//     */
//    @BeforeClass
//    public static void beforeClass() throws Exception {
//    }
//
//    /**
//     * after class.
//     * @throws Exception Unexpected exception
//     */
//    @AfterClass
//    public static void afterClass() throws Exception {
//    }

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD,
                Account.STATUS_ACTIVE, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD,
                Account.STATUS_SUSPENDED, HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_ACTIVE, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_SUSPENDED, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
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
    public final void test() {
        // account active.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD, HttpStatus.SC_OK);

        // account suspended.
        TResponse passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD,
                HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));
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

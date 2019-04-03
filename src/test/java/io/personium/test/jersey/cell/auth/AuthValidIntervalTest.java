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
package io.personium.test.jersey.cell.auth;

import static org.junit.Assert.assertNotNull;
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
import io.personium.core.rs.cell.TokenEndPointResource;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * test authentication valid interval.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthValidIntervalTest extends PersoniumTest {

    private static final int TEST_ACCOUNT_VALID_AUTHN_INTERVAL = 5;

    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_APP_CELL1 = "schema1";

    /**
     * before class.
     * @throws Exception Unexpected exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Rewrite "accountValidAuthnInterval" as test parameters.
        Field field = LockManager.class.getDeclaredField("accountValidAuthnInterval");
        field.setAccessible(true);
        field.set(LockManager.class, TEST_ACCOUNT_VALID_AUTHN_INTERVAL);
    }

    /**
     * after class.
     * @throws Exception Unexpected exception
     */
    @AfterClass
    public static void afterClass() throws Exception {
        // Restore "accountValidAuthnInterval".
        Field field = LockManager.class.getDeclaredField("accountValidAuthnInterval");
        field.setAccessible(true);
        field.set(LockManager.class, PersoniumUnitConfig.getAccountValidAuthnInterval());
    }

    /**
     * Before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * After.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * logger.
     */
    static Logger log = LoggerFactory.getLogger(TokenEndPointResource.class);

    /**
     * constructor.
     */
    public AuthValidIntervalTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test when the certification interval is short.
     */
    @Test
    public final void test_interval_is_short() {
        // Authentication failed.
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // If the authentication interval is short, it will be 400(PR400-AN-0017).
        // Repeat several times. All returned as "400".
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            passRes = requestAuthentication(TEST_CELL1, "account1", "password1", HttpStatus.SC_BAD_REQUEST);
            body = (String) passRes.bodyAsJson().get("error_description");
            assertTrue(body.startsWith("[PR400-AN-0017]"));
        }
    }

    /**
     * Test in case of consecutive authentication failed.
     */
    @Test
    public final void test_interval_is_short_and_failed() {
        // Authentication failed.
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // Repeat authentication failure.
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
            body = (String) passRes.bodyAsJson().get("error_description");
            assertTrue(body.startsWith("[PR400-AN-0017]"));
        }
    }

    /**
     * It is a test where authentication is performed at intervals.
     */
    @Test
    public final void test_interval_normal() {
        // Authentication failed.
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // Make enough intervals.
        try {
            Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Authenticate.
        passRes = requestAuthentication(TEST_CELL1, "account1", "password1", HttpStatus.SC_OK);
        body = (String) passRes.bodyAsJson().get("access_token");
        assertNotNull(body);
    }

    /**
     * It is a test when authentication failures are repeated regularly.
     */
    @Test
    public final void test_interval_failed() {
        // Authentication failed.
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        for (int i = 0; i < 3; i++) {
            // Make enough intervals.
            try {
                Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Authentication failed.
            passRes = requestAuthentication(TEST_CELL1, "account1", "dummypassword1", HttpStatus.SC_BAD_REQUEST);
            body = (String) passRes.bodyAsJson().get("error_description");
            assertTrue(body.startsWith("[PR400-AN-0017]"));

            // Successive authentications from the last failure will fail.
            passRes = requestAuthentication(TEST_CELL1, "account1", "password1", HttpStatus.SC_BAD_REQUEST);
            body = (String) passRes.bodyAsJson().get("error_description");
            assertTrue(body.startsWith("[PR400-AN-0017]"));
        }
    }

    private TResponse requestAuthentication(String cellName, String userName, String password, int code) {
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

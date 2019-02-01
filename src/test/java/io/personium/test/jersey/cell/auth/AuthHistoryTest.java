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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.auth.OAuth2Helper;
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
 * test auth history.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthHistoryTest extends PersoniumTest {

    private static Logger log = LoggerFactory.getLogger(AuthHistoryTest.class);

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthhistory";
    /** test account name. */
    private static final String TEST_ACCOUNT = "account";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * constructor.
     */
    public AuthHistoryTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * before.
     */
    @Before
    public void before() {
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * first authenticated.
     */
    @Test
    public final void first_authenticated() {
        TResponse passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);

        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertNull(passRes.bodyAsJson().get(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.FAILED_COUNT));
        assertThat(passRes.bodyAsJson().get(OAuth2Helper.Key.FAILED_COUNT), is(0L));
    }

    /**
     * get "last_authenticated" and "failed_count".
     */
    @Test
    public final void get_last_authenticated() {
        // first get token. failed count = 3.
        requestAuthorization(TEST_CELL, TEST_ACCOUNT, "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        requestAuthorization(TEST_CELL, TEST_ACCOUNT, "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        requestAuthorization(TEST_CELL, TEST_ACCOUNT, "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.debug("");
        }
        Long beforeFirstAuthenticatedTime = new Date().getTime();
        TResponse passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);
        Long afterFirstAuthenticatedTime = new Date().getTime();

        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertNull(passRes.bodyAsJson().get(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertThat(passRes.bodyAsJson().get(OAuth2Helper.Key.FAILED_COUNT), is(3L));

        // second get token. failed count = 0.
        passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);
        Long afterSecondAuthenticatedTime = new Date().getTime();

        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.LAST_AUTHENTICATED));
        Long lastAuthenticated = (Long) passRes.bodyAsJson().get(OAuth2Helper.Key.LAST_AUTHENTICATED);
        assertTrue(beforeFirstAuthenticatedTime <= lastAuthenticated && lastAuthenticated <= afterFirstAuthenticatedTime);
        assertThat(passRes.bodyAsJson().get(OAuth2Helper.Key.FAILED_COUNT), is(0L));

        // third get token. failed count = 1.
        requestAuthorization(TEST_CELL, TEST_ACCOUNT, "dummypassword1", HttpStatus.SC_BAD_REQUEST);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.debug("");
        }
        passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);

        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.LAST_AUTHENTICATED));
        lastAuthenticated = (Long) passRes.bodyAsJson().get(OAuth2Helper.Key.LAST_AUTHENTICATED);
        assertTrue(afterFirstAuthenticatedTime <= lastAuthenticated && lastAuthenticated <= afterSecondAuthenticatedTime);
        assertTrue(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.FAILED_COUNT));
        assertThat(passRes.bodyAsJson().get(OAuth2Helper.Key.FAILED_COUNT), is(1L));
    }

    /**
     * token refresh.
     * has not been set "last_authenticated" and "failed_count".
     */
    @Test
    public final void token_refresh() {
        // first get token.
        Long beforeAuthenticatedTime = new Date().getTime();
        TResponse passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);
        Long afterFirstAuthenticatedTime = new Date().getTime();

        // failed get token after first get token.
        requestAuthorization(TEST_CELL, TEST_ACCOUNT, "dummypassword1", HttpStatus.SC_BAD_REQUEST);

        // token refresh.
        String refreshToken = (String) passRes.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
        passRes = Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL)
                .with("refresh_token", refreshToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        assertFalse(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertFalse(passRes.bodyAsJson().containsKey(OAuth2Helper.Key.FAILED_COUNT));

        // get token after refreshed. check last authenticated and failed count.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.debug("");
        }
        passRes = requestAuthorization(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_OK);
        Long lastAuthenticated = (Long) passRes.bodyAsJson().get(OAuth2Helper.Key.LAST_AUTHENTICATED);
        assertTrue(beforeAuthenticatedTime <= lastAuthenticated && lastAuthenticated <= afterFirstAuthenticatedTime);
        assertThat(passRes.bodyAsJson().get(OAuth2Helper.Key.FAILED_COUNT), is(1L));
    }

    /**
     * request authorization.（__token)
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

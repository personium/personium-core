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

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.UrlUtils;

/**
 * valid interval test for authorization.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthzValidIntervalTest extends PersoniumTest {

    private static final String REDIRECT_HTML = "__/redirect.html";
    static final String DEFAULT_STATE = "0000000111";

    private static final int TEST_ACCOUNT_VALID_AUTHN_INTERVAL = 5;

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
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * constructor.
     */
    public AuthzValidIntervalTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test when the certification interval is short.
     */
    @Test
    public final void test_interval_is_short() {
        String addbody = "&username=account2&password=dummypassword";

        // Authentication failed.
        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));

        // If the authentication interval is short.
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL / 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            addbody = "&username=account2&password=password2";
            res = requesttoAuthz(addbody);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                    UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
            assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
            queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
            assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
        }
    }

    /**
     * It is a test where authentication is performed at intervals.
     */
    @Test
    public final void test_interval_normal() {
        String addbody = "&username=account2&password=dummypassword";

        // Authentication failed.
        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));

        // Make enough intervals.
        try {
            Thread.sleep(1000 * TEST_ACCOUNT_VALID_AUTHN_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // authorization
        addbody = "&username=account2&password=password2";
        res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        try {
            ResidentLocalAccessToken aToken = ResidentLocalAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL1));
            assertNotNull("access token parse error.", aToken);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * request authorization.
     * @param addbody add body
     * @return response
     */
    private PersoniumResponse requesttoAuthz(String addbody) {
        return requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);
    }

    /**
     * request authorization.
     * @param addbody add body
     * @param requestCellName requestCellName
     * @param clientId client_id
     * @param redirecturi redirect_uri
     * @return response
     */
    private PersoniumResponse requesttoAuthz(String addbody, String requestCellName,
            String clientId, String redirecturi) {
        if (addbody == null) {
            addbody = "";
        }

        // Create a fixed request body
        if (redirecturi == null) {
            redirecturi = clientId + REDIRECT_HTML;
        }
        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;

        return requesttoAuthzWithBody(requestCellName, body);
    }

    /**
     * request authorization.
     * @param requestCellName requestCellName
     * @param body body
     * @return response
     */
    private PersoniumResponse requesttoAuthzWithBody(String requestCellName, String body) {
        return requesttoAuthzWithBody(requestCellName, body, null);
    }

    /**
     * request authorization.
     * @param requestCellName requestCellName
     * @param body body
     * @param requestheaders add request headers
     * @return response
     */
    private PersoniumResponse requesttoAuthzWithBody(String requestCellName,
            String body,
            HashMap<String, String> requestheaders) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // set request headers
        if (requestheaders == null) {
            requestheaders = new HashMap<String, String>();
        }
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            res = rest.post(UrlUtils.cellRoot(requestCellName) + "__authz", body, requestheaders);

        } catch (PersoniumException e) {
            e.printStackTrace();
        }

        return res;
    }

}

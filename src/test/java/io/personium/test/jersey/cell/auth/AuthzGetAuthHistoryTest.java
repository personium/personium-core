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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.auth.OAuth2Helper;
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
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;

/**
 * test auth history for authorization.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzGetAuthHistoryTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthzgetauthhistory";
    /** test box name. */
    private static final String TEST_BOX = "testboxauthzgetauthhistory";
    /** test account name. */
    private static final String TEST_ACCOUNT = "account";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * constructor.
     */
    public AuthzGetAuthHistoryTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * before.
     */
    @Before
    public void before() {
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.createWithSchema(TEST_CELL, TEST_BOX, AbstractCase.MASTER_TOKEN_NAME,
                UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT, -1);
        BoxUtils.delete(TEST_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * first authenticated.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void authz_first_authenticated() throws Exception {
        PersoniumResponse dcRes = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD);
        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(dcRes);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
        assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("0");
    }

    /**
     * get "last_authenticated" and "failed_count".
     * @throws Exception Unexpected exception
     */
    @Test
    public final void get_last_authenticated() throws Exception {
        // first get token. failed count = 3.
        requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
        requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
        requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
        AuthTestCommon.waitForIntervalLock();
        Long before1stAuthnTime = new Date().getTime();
        PersoniumResponse dcRes = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD);
        Long after1stAuthnTime = new Date().getTime();

        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(dcRes);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
        assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("3");

        // second get token. failed count = 0.
        dcRes = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD);
        Long after2ndAuthnTime = new Date().getTime();

        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = parseResponse(dcRes);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        Long lastAuthenticated = Long.valueOf(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertTrue(before1stAuthnTime <= lastAuthenticated && lastAuthenticated <= after1stAuthnTime);
        assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("0");

        // third get token. failed count = 1.
        requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
        AuthTestCommon.waitForIntervalLock();
        dcRes = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD);

        assertThat(dcRes.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = parseResponse(dcRes);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        lastAuthenticated = Long.valueOf(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED));
        assertTrue(after1stAuthnTime <= lastAuthenticated && lastAuthenticated <= after2ndAuthnTime);
        assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("1");
    }

    /**
    * test accountsnotrecordingauthhistory.
    * @throws Exception Unexpected exception
    */
    @Test
    public final void not_recording_auth_history() throws Exception {
        String accountNr2 = "accountNr2";

        try {
            AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, accountNr2, TEST_PASSWORD,
                    HttpStatus.SC_CREATED);
            CellUtils.proppatchSet(TEST_CELL,
                    "<p:accountsnotrecordingauthhistory>accountNr1,accountNr2</p:accountsnotrecordingauthhistory>",
                    Setup.MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // first get token. Authentication history is not recorded.
            requestAuthorization4Authz(TEST_CELL, accountNr2, "dummypassword");
            requestAuthorization4Authz(TEST_CELL, accountNr2, "dummypassword");
            requestAuthorization4Authz(TEST_CELL, accountNr2, "dummypassword");
            AuthTestCommon.waitForIntervalLock();
            PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, accountNr2, TEST_PASSWORD);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            Map<String, String> responseMap = parseResponse(res);
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
            assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("0");

            // second get token. Authentication history is not recorded.
            res = requestAuthorization4Authz(TEST_CELL, accountNr2, TEST_PASSWORD);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            responseMap = parseResponse(res);
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
            assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("0");

            // third get token. Authentication history is not recorded.
            requestAuthorization4Authz(TEST_CELL, accountNr2, "dummypassword");
            AuthTestCommon.waitForIntervalLock();
            res = requestAuthorization4Authz(TEST_CELL, accountNr2, TEST_PASSWORD);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            responseMap = parseResponse(res);
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
            assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("0");

            // The authentication history of other accounts is recorded.
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
            requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, "dummypassword");
            AuthTestCommon.waitForIntervalLock();
            res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
            responseMap = parseResponse(res);
            assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
            assertThat(responseMap.get(OAuth2Helper.Key.LAST_AUTHENTICATED)).contains("null");
            assertThat(responseMap.get(OAuth2Helper.Key.FAILED_COUNT)).contains("3");
        } finally {
            AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, accountNr2, -1);
            CellUtils.proppatchRemove(TEST_CELL, "<p:accountsnotrecordingauthhistory/>", Setup.MASTER_TOKEN_NAME, -1);
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
     * parse response.
     * @param res the personium response
     * @return parse response.
     */
    private Map<String, String> parseResponse(PersoniumResponse res) {
        String location = res.getFirstHeader(HttpHeaders.LOCATION);
        System.out.println(location);
        String[] locations = location.split("#");
        String[] responses = locations[1].split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String response : responses) {
            String[] value = response.split("=");
            map.put(value[0], value[1]);
        }

        return map;
    }
}

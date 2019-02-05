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
 * valid ip address range test for authz.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzValidIPAddressTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthzaddress";
    /** test box name. */
    private static final String TEST_BOX = "testboxauthzaddress";
    /** test account name. */
    private static final String TEST_ACCOUNT = "testaccount";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * before.
     */
    @Before
    public void before() {
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.createWithSchema(TEST_CELL, TEST_BOX, AbstractCase.MASTER_TOKEN_NAME,
                UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
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
     * constructor.
     */
    public AuthzValidIPAddressTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of single address.)
     * @throws Exception Unexpected exception
     */
    @Test
    public final void single_ip_address() throws Exception {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // failure if other IP address.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.2");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);

        // failure if IP address is unknown.
        AuthTestCommon.waitForIntervalLock();
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);

    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of multiple addresses.)
     * @throws Exception Unexpected exception
     */
    @Test
    public final void multiple_ip_addresses() throws Exception {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.2,192.127.0.3,192.127.1.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.3");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // failure if other IP address.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of single ip address range.)
     * @throws Exception Unexpected exception
     */
    @Test
    public final void single_ip_address_range() throws Exception {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.0/24", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.127");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // failure if other IP address.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.1.1");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of multiple IP address ranges.)
     * @throws Exception Unexpected exception
     */
    @Test
    public final void multiple_ip_address_ranges() throws Exception {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.1.0/24,192.127.2.0/24,192.127.3.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.2.127");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // failure if other IP address.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.127");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of not set)
     * @throws Exception Unexpected exception
     */
    @Test
    public final void not_set() throws Exception {
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_CREATED);

        // Authentication possible for all IP addresses.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
    }

    /**
     * request authorization.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @return http response
     */
    private PersoniumResponse requestAuthorization4Authz(String cellName, String userName, String password, String xForwardedFor)
            throws PersoniumException {
        HashMap<String, String> authorizationHeader = new HashMap<String, String>();
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            authorizationHeader.put("X-Forwarded-For", xForwardedFor);
        }

        PersoniumResponse res = CellUtils.implicitflowAuthenticate(cellName, Setup.TEST_CELL_SCHEMA1, userName,
                password, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, authorizationHeader);
        return res;
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

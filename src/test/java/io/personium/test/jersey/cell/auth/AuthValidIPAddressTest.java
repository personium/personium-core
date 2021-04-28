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

import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * valid ip address range test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthValidIPAddressTest extends PersoniumTest {

    /** test cell name. */
    private static final String TEST_CELL = "testcellipaddress";
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
    }

    /**
     * after.
     */
    @After
    public void after() {
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthValidIPAddressTest.class);

    /**
     * constructor.
     */
    public AuthValidIPAddressTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of single address.)
     */
    @Test
    public final void single_ip_address() {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1", HttpStatus.SC_OK);

        // failure if other IP address.
        TResponse passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.2",
                HttpStatus.SC_BAD_REQUEST);
        String body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

        // failure if IP address is unknown.
        AuthTestCommon.waitForIntervalLock();
        passRes = requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, null, HttpStatus.SC_BAD_REQUEST);
        body = (String) passRes.bodyAsJson().get("error_description");
        assertTrue(body.startsWith("[PR400-AN-0017]"));

    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of multiple addresses.)
     */
    @Test
    public final void multiple_ip_addresses() {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.2,192.127.0.3,192.127.1.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.2", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.3", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.1.1", HttpStatus.SC_OK);

        // failure if other IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of single ip address range.)
     */
    @Test
    public final void single_ip_address_range() {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.0.0/24", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        //   192.127.0.0/24：192.127.0.1～192.127.0.254
        //                      exclude Network Address(192.127.0.0) and Broadcast Address(192.127.0.255)
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.2", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.253", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.254", HttpStatus.SC_OK);

        // failure if other IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.1.1", HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock();
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.0", HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock();
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.255", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of multiple IP address ranges.)
     */
    @Test
    public final void multiple_ip_address_ranges() {
        AccountUtils.createWithIPAddressRange(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD,
                "192.127.1.0/24,192.127.2.0/24,192.127.3.1", HttpStatus.SC_CREATED);

        // Authentication possible if set IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.1.1", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.1.254", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.2.1", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.2.254", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.3.1", HttpStatus.SC_OK);

        // failure if other IP address.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1", HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForIntervalLock();
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.3.254", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test whether authentication is restricted with "IPAddressRange".
     * (In case of not set)
     */
    @Test
    public final void not_set() {
        AccountUtils.create(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, HttpStatus.SC_CREATED);

        // Authentication possible for all IP addresses.
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, "192.127.0.1", HttpStatus.SC_OK);
        requestAuthentication(TEST_CELL, TEST_ACCOUNT, TEST_PASSWORD, null, HttpStatus.SC_OK);
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param userName user name
     * @param pass password
     * @param xForwardedFor IP address(X-Forwarded-For)
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthentication(String cellName, String userName, String pass, String xForwardedFor,
            int code) {
        Http request;
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            request = Http.request("authn/password-cl-c0-xForwardedFor.txt")
                    .with("remoteCell", cellName)
                    .with("username", userName)
                    .with("password", pass)
                    .with("xForwardedFor", xForwardedFor);
        } else {
            request = Http.request("authn/password-cl-c0.txt")
                    .with("remoteCell", cellName)
                    .with("username", userName)
                    .with("password", pass);
        }
        TResponse passRes = request.returns().statusCode(code);
        return passRes;
    }
}

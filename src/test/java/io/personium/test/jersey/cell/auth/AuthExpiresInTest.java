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

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;

/**
 * account lock test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthExpiresInTest extends PersoniumTest {


    /** test cell name. */
    private static final String TEST_CELL1 = Setup.TEST_CELL1;
    /** test cell name. */
    private static final String TEST_CELL2 = Setup.TEST_CELL2;
    /** test app cell name. */
    static final String TEST_APP_CELL1 = "schema1";
    /** test account name. */
    private static final String TEST_ACCOUNT = "account4";
    /** test account password. */
    private static final String TEST_PASSWORD = "password4";

    /**
     * before class.
     * @throws Exception Unexpected exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

    }

    /**
     * after class.
     * @throws Exception Unexpected exception
     */
    @AfterClass
    public static void afterClass() throws Exception {

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

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthExpiresInTest.class);

    /**
     * constructor.
     */
    public AuthExpiresInTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test invalid parameters.
     */
    @Test
    public final void invalid_parameter() {
        // authentication.
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "0", "10", HttpStatus.SC_BAD_REQUEST);
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "3601", "10", HttpStatus.SC_BAD_REQUEST);
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "あ", "10", HttpStatus.SC_BAD_REQUEST);
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "0", HttpStatus.SC_BAD_REQUEST);
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "86400001", HttpStatus.SC_BAD_REQUEST);
        requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "あ", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test if we get the local token with handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void handlePassword_localToken() throws Exception {
        // Before access token expires in.
        TResponse res = requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "5", "3600",
                HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        String aToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        ResourceUtils.retrieve(aToken, "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);

        // After access token expires in.
        Thread.sleep(5000);
        ResourceUtils.retrieve(aToken, "", HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);

        // Before refresh token expires in
        res = requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "3600", "5",
                HttpStatus.SC_OK);
        json = res.bodyAsJson();
        String rToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        requestRefresh(TEST_CELL1, rToken, HttpStatus.SC_OK);

        // After refresh token expires in
        res = requestAuthentication(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "3600", "5",
                HttpStatus.SC_OK);
        json = res.bodyAsJson();
        rToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        Thread.sleep(5000);
        requestRefresh(TEST_CELL1, rToken, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test if we get the trans cell token with handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void handlePassword_transToken() throws Exception {
        TResponse res =
                Http.request("authn/password-tc-c0-expiresIn.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("expires_in", "5")
                        .with("refresh_token_expires_in", "3600")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // Before access token expires in.
        Http.request("authn/saml-tc-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // After refresh token expires in
        Thread.sleep(5000);
        Http.request("authn/saml-tc-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @param expiresIn accress token expires in time(s).
     * @param rTokenExpiresIn refresh token expires in time(s).
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthentication(String cellName, String userName, String password, String expiresIn,
            String rTokenExpiresIn, int code) {
        TResponse res = Http.request("authn/password-cl-c0-expiresIn.txt")
                .with("remoteCell", cellName)
                .with("username", userName)
                .with("password", password)
                .with("expires_in", expiresIn)
                .with("refresh_token_expires_in", rTokenExpiresIn)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * request token refresh.
     * @param cell cell
     * @param refreshToken refresh token.
     * @param code expected status code
     * @return http response
     */
    private static TResponse requestRefresh(String cell, String refreshToken, int code) {
        // アプリセルに対して認証
        TResponse res = Http.request("authn/refresh-cl.txt")
                .with("remoteCell", cell)
                .with("refresh_token", refreshToken)
                .returns()
                .statusCode(code);
        return res;
    }
}

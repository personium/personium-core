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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import io.personium.test.utils.AuthzUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TokenUtils;

/**
 * set token expires in test for authentication.
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
        // expiresIn.
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "0", "10", HttpStatus.SC_BAD_REQUEST);
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "3601", "10", HttpStatus.SC_BAD_REQUEST);
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "あ", "10", HttpStatus.SC_BAD_REQUEST);

        // rTokenExpiresIn.
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "0", HttpStatus.SC_BAD_REQUEST);
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "86400001", HttpStatus.SC_BAD_REQUEST);
        requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "1", "あ", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test if we get the local token with handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void handlePassword_localToken() throws Exception {
        // get local token.
        TResponse res = requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "2", "4",
                HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        String aToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        String rToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        Long expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        Long rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(2L));
        assertThat(rTokenExpiresIn, is(4L));

        // Before access token expires in.
        ResourceUtils.retrieve(aToken, "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        requestAuthn4Refresh(TEST_CELL1, rToken, null, null, null, HttpStatus.SC_OK);

        // After access token expires in.
        Thread.sleep(2000);
        ResourceUtils.retrieve(aToken, "", HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);
        requestAuthn4Refresh(TEST_CELL1, rToken, null, null, null, HttpStatus.SC_OK);

        // After refresh token expires in.
        Thread.sleep(2000);
        ResourceUtils.retrieve(aToken, "", HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);
        requestAuthn4Refresh(TEST_CELL1, rToken, null, null, null, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test if we get the trans cell token with handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void handlePassword_transCellToken() throws Exception {
        // get trans cell token.
        TResponse res = requestAuthn4Password(TEST_CELL1, "account1", "password1", UrlUtils.cellRoot(TEST_CELL2),
                null, "3", "6", HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        Long expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        Long rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));
    }

    /**
     * Test if receiveSaml2.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void receiveSaml2() throws Exception {
        TResponse res = requestAuthn4Password(TEST_CELL1, "account1", "password1", UrlUtils.cellRoot(TEST_CELL2),
                null, null, null, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        String baseToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // receiveSaml2 local
        res = requestAuthn4Saml(TEST_CELL2, baseToken, null, "3", "6", HttpStatus.SC_OK);
        json = res.bodyAsJson();
        Long expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        Long rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));

        // receiveSaml2 trans cell token.
        res = requestAuthn4Saml(TEST_CELL2, baseToken, UrlUtils.cellRoot(TEST_APP_CELL1), "3", "6", HttpStatus.SC_OK);
        json = res.bodyAsJson();
        expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));
    }

    /**
     * Test if receiveRefresh.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void receiveRefresh() throws Exception {
        TResponse res = requestAuthn4Password(TEST_CELL1, TEST_ACCOUNT, TEST_PASSWORD, "3600", "3600",
                HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        String rToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);

        // refresh local token.
        res = requestAuthn4Refresh(TEST_CELL1, rToken, null, "3", "6", HttpStatus.SC_OK);
        json = res.bodyAsJson();
        Long expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        Long rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));

        // refresh trans cell token.
        res = requestAuthn4Refresh(TEST_CELL1, rToken, UrlUtils.cellRoot(TEST_APP_CELL1), "3", "6", HttpStatus.SC_OK);
        json = res.bodyAsJson();
        expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));
    }

    /**
     * Test if receiveCord.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void receiveCord() throws Exception {
        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, "code",
                redirectUri, clientId, "state1", "account1", "password1", HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = parseQuery(locationHeader);
        String code = locationQuery.get("code");

        // Get app token.
        TResponse appTokenResponse = TokenUtils.getTokenPassword(
                Setup.TEST_CELL_SCHEMA1, "account0", "password0",
                UrlUtils.cellRoot(Setup.TEST_CELL1), HttpStatus.SC_OK);
        String appToken = (String) appTokenResponse.bodyAsJson().get("access_token");

        // receiveCord local.
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", OAuth2Helper.GrantType.AUTHORIZATION_CODE);
        params.put("code", code);
        params.put("client_id", clientId);
        params.put("client_secret", appToken);
        params.put("expires_in", "3");
        params.put("refresh_token_expires_in", "6");
        TResponse res = requestAuthentication(Setup.TEST_CELL1, params, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        Long expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        Long rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));

        // receiveCord trans cell.
        params.put("p_target", UrlUtils.cellRoot(TEST_APP_CELL1));
        res = requestAuthentication(Setup.TEST_CELL1, params, HttpStatus.SC_OK);
        json = res.bodyAsJson();
        expiresIn = (Long) json.get(OAuth2Helper.Key.EXPIRES_IN);
        rTokenExpiresIn = (Long) json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN);
        assertThat(expiresIn, is(3L));
        assertThat(rTokenExpiresIn, is(6L));
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param params params map
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthentication(String cellName, Map<String, String> params, int statusCode) {
        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            paramList.add(String.format("%s=%s", param.getKey(), param.getValue()));
        }
        TResponse res = TokenUtils.getToken(cellName, String.join("&", paramList), statusCode);
        return res;
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
    private TResponse requestAuthn4Password(String cellName, String userName, String password, String expiresIn,
            String rTokenExpiresIn, int code) {
        return requestAuthn4Password(cellName, userName, password, null, null, expiresIn, rTokenExpiresIn, code);
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @param owner owner
     * @param target target
     * @param expiresIn accress token expires in time(s).
     * @param rTokenExpiresIn refresh token expires in time(s).
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthn4Password(String cellName, String userName, String password, String target,
            String owner, String expiresIn, String rTokenExpiresIn, int code) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", OAuth2Helper.GrantType.PASSWORD);
        params.put("username", userName);
        params.put("password", password);
        if (target != null) {
            params.put("p_target", target);
        }
        if (owner != null) {
            params.put("p_owner", owner);
        }
        if (expiresIn != null) {
            params.put("expires_in", expiresIn);
        }
        if (rTokenExpiresIn != null) {
            params.put("refresh_token_expires_in", rTokenExpiresIn);
        }
        TResponse res = requestAuthentication(cellName, params, code);
        return res;
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param assertion assertion (trans cell token)
     * @param target target
     * @param expiresIn accress token expires in time(s).
     * @param rTokenExpiresIn refresh token expires in time(s).
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthn4Saml(String cellName, String assertion, String target,
            String expiresIn, String rTokenExpiresIn, int code) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", OAuth2Helper.GrantType.SAML2_BEARER);
        params.put("assertion", assertion);
        if (target != null) {
            params.put("p_target", target);
        }
        if (expiresIn != null) {
            params.put("expires_in", expiresIn);
        }
        if (rTokenExpiresIn != null) {
            params.put("refresh_token_expires_in", rTokenExpiresIn);
        }
        TResponse res = requestAuthentication(cellName, params, code);
        return res;
    }

    /**
     * request authentication.
     * @param cellName cell name
     * @param refreshToken refresh token.
     * @param target target
     * @param expiresIn accress token expires in time(s).
     * @param rTokenExpiresIn refresh token expires in time(s).
     * @param code expected status code
     * @return http response
     */
    private TResponse requestAuthn4Refresh(String cellName, String refreshToken, String target, String expiresIn,
            String rTokenExpiresIn, int code) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", OAuth2Helper.GrantType.REFRESH_TOKEN);
        params.put("refresh_token", refreshToken);
        if (target != null) {
            params.put("p_target", target);
        }
        if (expiresIn != null) {
            params.put("expires_in", expiresIn);
        }
        if (rTokenExpiresIn != null) {
            params.put("refresh_token_expires_in", rTokenExpiresIn);
        }
        TResponse res = requestAuthentication(cellName, params, code);
        return res;
    }

    /**
     * Return query part of the location header.
     * @param locationHeader
     * @return query
     */
    private Map<String, String> parseQuery(String locationHeader) {
        String[] splits = locationHeader.split("\\?");
        String[] querys = splits[1].split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String query : querys) {
            String[] keyvalue = query.split("=");
            map.put(keyvalue[0], keyvalue[1]);
        }
        return map;
    }
}

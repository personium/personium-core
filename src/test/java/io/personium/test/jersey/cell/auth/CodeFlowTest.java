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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.CharEncoding;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AuthzUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TokenUtils;

/**
 * Authz endpoint tests.
 * Code flow tests.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Integration.class})
public class CodeFlowTest extends PersoniumTest {

    /** username. */
    private static final String ACCOUNT_1 = "account1";
    /** password. */
    private static final String PASSWORD_1 = "password1";
    /** state. */
    private static final String STATE_1 = "state1";
    /** response_type. */
    private static final String TYPE_CODE = "code";

    /** Default value of authorizationhtmlurl. */
    private static String authorizationhtmlurlDefault;

    /**
     * Constructor.
     */
    public CodeFlowTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Befor class.
     * This test class is a premise that original authzhtml is not set.
     */
    @BeforeClass
    public static void beforClass() {
        authorizationhtmlurlDefault = PersoniumUnitConfig.get(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT);
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, "");
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT,
                authorizationhtmlurlDefault != null ? authorizationhtmlurlDefault : ""); // CHECKSTYLE IGNORE
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
     * Authz successful with ID/Password.
     */
    @Test
    public void normal_id_password() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = PASSWORD_1;
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        String locationUri = getRedirectUri(locationHeader);
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);

        assertThat(locationUri, is(redirectUri));
        assertNotNull(locationQuery.get("code"));
        assertThat(locationQuery.get("state"), is(state));
    }

    /**
     * Authz successful with pCookie.
     */
    @Test
    public void normal_pCookie() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = PASSWORD_1;

        TResponse tokenResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, username, password, HttpStatus.SC_OK);
        String setCookie = tokenResponse.getHeader("Set-Cookie");
        String pCookie = setCookie.split("=")[1];

        TResponse response = AuthzUtils.getPCookie(Setup.TEST_CELL1, TYPE_CODE, redirectUri, clientId,
                state, pCookie, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        String locationUri = getRedirectUri(locationHeader);
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);

        assertThat(locationUri, is(redirectUri));
        assertNotNull(locationQuery.get("code"));
        assertThat(locationQuery.get("state"), is(state));
    }

    /**
     * Token can be get from the acquired code.
     */
    @Test
    public void normal_token_get_from_code_from_password() {
        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, STATE_1, ACCOUNT_1, PASSWORD_1, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);
        // Get code.
        String code = locationQuery.get("code");

        // Get app token.
        TResponse appTokenResponse = TokenUtils.getTokenPassword(
                Setup.TEST_CELL_SCHEMA1, "account0", "password0",
                UrlUtils.cellRoot(Setup.TEST_CELL1), HttpStatus.SC_OK);
        String appToken = (String) appTokenResponse.bodyAsJson().get("access_token");

        // token endpoint.
        TResponse tokenResponse = TokenUtils.getTokenCode(Setup.TEST_CELL1, code, clientId, appToken, HttpStatus.SC_OK);
        String accessToken = (String) tokenResponse.bodyAsJson().get("access_token");
        assertNotNull(accessToken);
    }

    /**
     * Token can be get from the acquired code.
     */
    @Test
    public void normal_token_get_from_code_from_pCookie() {
        // token endpoint.
        TResponse pCookieResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, ACCOUNT_1, PASSWORD_1, HttpStatus.SC_OK);
        String setCookie = pCookieResponse.getHeader("Set-Cookie");
        // Get p_cookie.
        String pCookie = setCookie.split("=")[1];

        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.getPCookie(Setup.TEST_CELL1, TYPE_CODE, redirectUri, clientId,
                STATE_1, pCookie, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);
        // Get code.
        String code = locationQuery.get("code");

        // Get app token.
        TResponse appTokenResponse = TokenUtils.getTokenPassword(
                Setup.TEST_CELL_SCHEMA1, "account0", "password0",
                UrlUtils.cellRoot(Setup.TEST_CELL1), HttpStatus.SC_OK);
        String appToken = (String) appTokenResponse.bodyAsJson().get("access_token");

        // token endpoint.
        TResponse tokenResponse = TokenUtils.getTokenCode(Setup.TEST_CELL1, code, clientId, appToken, HttpStatus.SC_OK);
        String accessToken = (String) tokenResponse.bodyAsJson().get("access_token");
        assertNotNull(accessToken);
    }

    /**
     * Token and IDToken can be get from the acquired code.
     * Verify that IDToken is correct content.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void normal_token_get_from_code_from_password_oidc() throws Exception {
        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, STATE_1, OAuth2Helper.Scope.OPENID,
                ACCOUNT_1, PASSWORD_1, HttpStatus.SC_SEE_OTHER);
        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);
        // Get code.
        String code = locationQuery.get("code");

        // token endpoint.
        TResponse appTokenResponse = TokenUtils.getTokenPassword(
                Setup.TEST_CELL_SCHEMA1, "account0", "password0",
                UrlUtils.cellRoot(Setup.TEST_CELL1), HttpStatus.SC_OK);
        // Get app token.
        String appToken = (String) appTokenResponse.bodyAsJson().get("access_token");

        // token endpoint.
        TResponse tokenResponse = TokenUtils.getTokenCode(Setup.TEST_CELL1, code, clientId, appToken, HttpStatus.SC_OK);
        String accessToken = (String) tokenResponse.bodyAsJson().get("access_token");
        String idToken = (String) tokenResponse.bodyAsJson().get("id_token");

        // certs endpoint.
        TResponse certsResponse = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys = (JSONArray) certsResponse.bodyAsJson().get("keys");
        JSONObject jwk = (JSONObject) keys.get(0);
        String modulus = (String) jwk.get("n");
        String exponent = (String) jwk.get("e");

        assertNotNull(accessToken);
        // verify signature
        verifySignatureIdToken(idToken, modulus, exponent);
        // verify payload
        verifyPayloadIdToken(idToken, UrlUtils.cellRoot(Setup.TEST_CELL1), UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
    }

    /**
     * Token and IDToken can be get from the acquired code.
     * Verify that IDToken is correct content.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void normal_token_get_from_code_from_pCookie_oidc() throws Exception {
        // token endpoint.
        TResponse pCookieResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, ACCOUNT_1, PASSWORD_1, HttpStatus.SC_OK);
        String setCookie = pCookieResponse.getHeader("Set-Cookie");
        // Get p_cookie.
        String pCookie = setCookie.split("=")[1];

        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.getPCookie(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, STATE_1, OAuth2Helper.Scope.OPENID,
                pCookie, HttpStatus.SC_SEE_OTHER);
        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);
        // Get code.
        String code = locationQuery.get("code");

        // token endpoint.
        TResponse appTokenResponse = TokenUtils.getTokenPassword(
                Setup.TEST_CELL_SCHEMA1, "account0", "password0",
                UrlUtils.cellRoot(Setup.TEST_CELL1), HttpStatus.SC_OK);
        // Get app token.
        String appToken = (String) appTokenResponse.bodyAsJson().get("access_token");

        // token endpoint.
        TResponse tokenResponse = TokenUtils.getTokenCode(Setup.TEST_CELL1, code, clientId, appToken, HttpStatus.SC_OK);
        String accessToken = (String) tokenResponse.bodyAsJson().get("access_token");
        String idToken = (String) tokenResponse.bodyAsJson().get("id_token");

        // certs endpoint.
        TResponse certsResponse = AuthzUtils.certsGet(Setup.TEST_CELL1, HttpStatus.SC_OK);
        JSONArray keys = (JSONArray) certsResponse.bodyAsJson().get("keys");
        JSONObject jwk = (JSONObject) keys.get(0);
        String modulus = (String) jwk.get("n");
        String exponent = (String) jwk.get("e");

        assertNotNull(accessToken);
        // verify signature
        verifySignatureIdToken(idToken, modulus, exponent);
        // verify payload
        verifyPayloadIdToken(idToken, UrlUtils.cellRoot(Setup.TEST_CELL1), UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
    }

    /**
     * Specify username that does not exist.
     * @throws Exception exception
     */
    @Test
    public void error_user_not_found() throws Exception {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = "not_found_account";
        String password = PASSWORD_1;
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_SEE_OTHER);
        assertTrue(response.getHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(response.getHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(response.getHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
        String expectedMessage = PersoniumCoreMessageUtils.getMessage("PS-AU-0004");
        String errorDesp = queryMap.get(OAuth2Helper.Key.ERROR_DESCRIPTION);
        assertThat(URLDecoder.decode(errorDesp, CharEncoding.UTF_8), is(expectedMessage));
    }

    /**
     * Specify an incorrect password.
     * @throws Exception exception
     */
    @Test
    public void error_password_failed() throws Exception {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = "failed_password";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_SEE_OTHER);
        assertTrue(response.getHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(response.getHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(response.getHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
        String expectedMessage = PersoniumCoreMessageUtils.getMessage("PS-AU-0004");
        String errorDesp = queryMap.get(OAuth2Helper.Key.ERROR_DESCRIPTION);
        assertThat(URLDecoder.decode(errorDesp, CharEncoding.UTF_8), is(expectedMessage));
    }

    /**
     * Box with schema specified by client_id does not exist.
     */
    @Test
    public void error_box_not_found() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL2);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = PASSWORD_1;
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        String locationUri = getRedirectUri(locationHeader);
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);

        assertThat(locationUri, is(redirectUri));
        assertThat(locationQuery.get(OAuth2Helper.Key.ERROR), is(OAuth2Helper.Error.UNAUTHORIZED_CLIENT));
        assertThat(locationQuery.get(OAuth2Helper.Key.ERROR_DESCRIPTION).replaceAll("\\+", " "),
                is(PersoniumCoreMessageUtils.getMessage("PR401-AZ-0003")));
        assertThat(locationQuery.get(OAuth2Helper.Key.CODE), is("PR401-AZ-0003"));
    }

    /**
     * Box with schema specified by client_id does not exist with pCookie.
     */
    @Test
    public void error_box_not_found_pCookie() {
        String username = ACCOUNT_1;
        String password = PASSWORD_1;

        TResponse tokenResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, username, password, HttpStatus.SC_OK);
        String setCookie = tokenResponse.getHeader("Set-Cookie");
        String pCookie = setCookie.split("=")[1];

        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL2);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        TResponse response = AuthzUtils.getPCookie(Setup.TEST_CELL1, TYPE_CODE, redirectUri, clientId,
                state, pCookie, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        String locationUri = getRedirectUri(locationHeader);
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);

        assertThat(locationUri, is(redirectUri));
        assertThat(locationQuery.get("state"), is(state));
        assertThat(locationQuery.get(OAuth2Helper.Key.ERROR), is(OAuth2Helper.Error.UNAUTHORIZED_CLIENT));
        assertThat(locationQuery.get(OAuth2Helper.Key.ERROR_DESCRIPTION).replaceAll("\\+", " "),
                is(PersoniumCoreMessageUtils.getMessage("PR401-AZ-0003")));
        assertThat(locationQuery.get(OAuth2Helper.Key.CODE), is("PR401-AZ-0003"));
    }

    /**
     * client_id and redirect_uri are different cells.
     */
    @Test
    public void error_clientid_and_redirecturi_different_cells() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = PASSWORD_1;
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        String locationUri = getRedirectUri(locationHeader);
        Map<String, String> locationQuery = UrlUtils.parseQuery(locationHeader);

        String expectedUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error";
        assertThat(locationUri, is(expectedUri));
        assertThat(locationQuery.get("code"), is("PR400-AZ-0003"));
    }

    /**
     * Return redirect_uri part of the location header.
     * @param locationHeader Location header
     * @return redirect_uri
     */
    private String getRedirectUri(String locationHeader) {
        String[] splits = locationHeader.split("\\?");
        return splits[0];
    }

    /**
     * Verify signature of id_token.
     * @param idToken id_token
     * @param modulus RSA modulus
     * @param exponent RSA exponent
     * @throws Exception
     */
    private void verifySignatureIdToken(String idToken, String modulus, String exponent) throws Exception {
        String[] idTokenParts = partIdToken(idToken);

        byte[] data = (idTokenParts[0] + "." + idTokenParts[1]).getBytes(StandardCharsets.UTF_8);
        byte[] idTokenSignature = base64UrlDecodeToBytes(idTokenParts[2]);
        PublicKey publicKey = getPublicKey(modulus, exponent);

        assertTrue(verifyUsingPublicKey(data, idTokenSignature, publicKey));
    }

    /**
     * Create PublicKey object.
     * @param modulus RSA modulus
     * @param exponent RSA exponent
     * @return PublicKey object
     * @throws Exception
     */
    private PublicKey getPublicKey(String modulus, String exponent) throws Exception {
        byte[] nb = base64UrlDecodeToBytes(modulus);
        byte[] eb = base64UrlDecodeToBytes(exponent);
        BigInteger n = new BigInteger(1, nb);
        BigInteger e = new BigInteger(1, eb);

        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(n, e);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);
        return publicKey;
    }

    /**
     * Verify signature.
     * @param data signed data
     * @param signature signature
     * @param pubKey public key
     * @return true:correct false:error
     * @throws Exception
     */
    private boolean verifyUsingPublicKey(byte[] data, byte[] signature, PublicKey pubKey) throws Exception {
        Signature sig = Signature.getInstance(AlgorithmUtils.RS_SHA_256_JAVA);
        sig.initVerify(pubKey);
        sig.update(data);
        return sig.verify(signature);
    }

    /**
     * Verify payload of id_token.
     * @param idToken
     * @param iss
     * @param aud
     * @throws Exception
     */
    private void verifyPayloadIdToken(String idToken, String iss, String aud) throws Exception {
        String[] idTokenParts = partIdToken(idToken);
        String payload = new String(base64UrlDecodeToBytes(idTokenParts[1]), StandardCharsets.UTF_8);
        JSONParser parser = new JSONParser();
        JSONObject jwt = (JSONObject) parser.parse(payload);
        assertThat((String) jwt.get("iss"), is(iss));
        assertThat((String) jwt.get("aud"), is(aud));
    }

    /**
     * Part id_token.
     * @param idToken id_token
     * @return parts
     */
    private String[] partIdToken(String idToken) {
        return idToken.split("\\.");
    }

    /**
     * Base64 URL Decode to bytedata.
     * @param input input string
     * @return decoded bytedata.
     */
    private byte[] base64UrlDecodeToBytes(String input) {
        Base64 decoder = new Base64(-1, null, true);
        byte[] decodedBytes = decoder.decode(input);
        return decodedBytes;
    }
}

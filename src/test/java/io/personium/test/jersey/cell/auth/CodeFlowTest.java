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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
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
@Category({ Integration.class })
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
        Map<String, String> locationQuery = parseQuery(locationHeader);

        assertThat(locationUri, is(redirectUri));
        assertNotNull(locationQuery.get("code"));
        assertThat(locationQuery.get("state"), is(state));
    }

    /**
     * Token can be get from the acquired code.
     */
    @Test
    public void normal_token_get_from_code() {
        // authz endpoint.
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, STATE_1, ACCOUNT_1, PASSWORD_1, HttpStatus.SC_SEE_OTHER);

        String locationHeader = response.getLocationHeader();
        Map<String, String> locationQuery = parseQuery(locationHeader);
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
     * Specify username that does not exist.
     */
    @Test
    public void error_user_not_found() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = "not_found_account";
        String password = PASSWORD_1;
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_OK);

        String message = PersoniumCoreMessageUtils.getMessage("PS-AU-0004");
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        String expectedHtml = AuthzUtils.createDefaultHtml(
                clientId, redirectUri, message, state, null, TYPE_CODE, null, null, cellUrl);

        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
        assertThat(response.getBody(), is(expectedHtml));
    }

    /**
     * Specify an incorrect password.
     */
    @Test
    public void error_password_failed() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = STATE_1;
        String username = ACCOUNT_1;
        String password = "failed_password";
        TResponse response = AuthzUtils.postPassword(Setup.TEST_CELL1, TYPE_CODE,
                redirectUri, clientId, state, username, password, HttpStatus.SC_OK);

        String message = PersoniumCoreMessageUtils.getMessage("PS-AU-0004");
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        String expectedHtml = AuthzUtils.createDefaultHtml(
                clientId, redirectUri, message, state, null, TYPE_CODE, null, null, cellUrl);

        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
        assertThat(response.getBody(), is(expectedHtml));
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
        Map<String, String> locationQuery = parseQuery(locationHeader);

        String expectedUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error";
        assertThat(locationUri, is(expectedUri));
        assertThat(locationQuery.get("code"), is("PS-ER-0003"));
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
        Map<String, String> locationQuery = parseQuery(locationHeader);

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

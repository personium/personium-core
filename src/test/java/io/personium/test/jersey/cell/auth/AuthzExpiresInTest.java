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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AuthzUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TokenUtils;

/**
 * set token expires in test for authorization.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzExpiresInTest extends PersoniumTest {

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
    public AuthzExpiresInTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Testing handlePassword.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePassword() throws Exception {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("response_type=").append("token")
                .append("&client_id=").append(clientId)
                .append("&redirect_uri=").append(redirectUri)
                .append("&username=").append("account1")
                .append("&password=").append("password1")
                .append("&state=").append(ImplicitFlowTest.DEFAULT_STATE)
                .append("&expires_in=").append("5");

        // post authz handle password.
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        PersoniumResponse res = rest.post(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz", bodyBuilder.toString(), requestheaders);

        Map<String, String> responseMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(responseMap.get(OAuth2Helper.Key.EXPIRES_IN), is("5"));
    }

    /**
     * Testing handlePCookie for post.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_post_handlePCookie() throws Exception {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = "state1";
        String username = "account1";
        String password = "password1";

        TResponse tokenResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, username, password, HttpStatus.SC_OK);
        String setCookie = tokenResponse.getHeader("Set-Cookie");
        String pCookie = setCookie.split("=")[1];

        // post authz handle password.
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("response_type=").append("token")
                .append("&redirect_uri=").append(redirectUri)
                .append("&client_id=").append(clientId)
                .append("&state=").append(state)
                .append("&expires_in=").append("5");

        TResponse response = AuthzUtils.postPCookie(
                Setup.TEST_CELL1, bodyBuilder.toString(), pCookie, HttpStatus.SC_SEE_OTHER);

        Map<String, String> locationQuery = UrlUtils.parseFragment(response.getLocationHeader());
        assertThat(locationQuery.get(OAuth2Helper.Key.EXPIRES_IN), is("5"));
    }

    /**
     * Testing handlePCookie for get.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_get_handlePCookie() throws Exception {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirectUri = clientId + "__/redirect.html";
        String state = "state1";
        String username = "account1";
        String password = "password1";

        TResponse tokenResponse = TokenUtils.getTokenPasswordPCookie(
                Setup.TEST_CELL1, username, password, HttpStatus.SC_OK);
        String setCookie = tokenResponse.getHeader("Set-Cookie");
        String pCookie = setCookie.split("=")[1];

        // get authz handle password.
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("response_type=").append("token")
                .append("&redirect_uri=").append(redirectUri)
                .append("&client_id=").append(clientId)
                .append("&state=").append(state)
                .append("&expires_in=").append("5");

        TResponse response = AuthzUtils.getPCookie(
                Setup.TEST_CELL1, queryBuilder.toString(), pCookie, HttpStatus.SC_SEE_OTHER);

        Map<String, String> locationQuery = UrlUtils.parseFragment(response.getLocationHeader());
        assertThat(locationQuery.get(OAuth2Helper.Key.EXPIRES_IN), is("5"));
    }
}

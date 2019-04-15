/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AuthzUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.TResponse;

/**
 * Authorization endpoint tests. GET method.
 */
@Category({Integration.class})
public class AuthzGetTest extends AbstractCase {

    /** Authorization html file name. */
    private static final String AUTHORIZATION_HTML_NAME = "test.html";
    /** Authorization html2 file name. */
    private static final String AUTHORIZATION_HTML2_NAME = "test2.html";
    /** Authorization html file contents. */
    private static final String AUTHORIZATION_HTML_BODY = "<html><body>This is test html.</body></html>";
    /** Authorization html2 file contents. */
    private static final String AUTHORIZATION_HTML2_BODY = "<html><body>This is test2 html.</body></html>";

    /** Authorization html url. */
    private static final String AUTHORIZATION_HTML_URL = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
            + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
    /** Authorization html2 url. */
    private static final String AUTHORIZATION_HTML2_URL = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
            + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML2_NAME;

    /** Authorization html acl path. */
    private static final String AUTHORIZATION_HTML_ACL_PATH = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/"
            + AUTHORIZATION_HTML_NAME;
    /** Authorization html2 acl path. */
    private static final String AUTHORIZATION_HTML2_ACL_PATH = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/"
            + AUTHORIZATION_HTML2_NAME;

    /** Default value of authorizationhtmlurl. */
    private static String authorizationhtmlurlDefault;
    /** Default value of authorizationpasswordchangehtmlurl. */
    private static String passwordchangehtmlurlDefault;

    /**
     * Constructor.
     */
    public AuthzGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Befor class.
     */
    @BeforeClass
    public static void beforClass() {
        authorizationhtmlurlDefault = PersoniumUnitConfig.get(
                PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT);
        passwordchangehtmlurlDefault = PersoniumUnitConfig.get(
                PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT);
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT,
                authorizationhtmlurlDefault != null ? authorizationhtmlurlDefault : ""); // CHECKSTYLE IGNORE
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT,
                passwordchangehtmlurlDefault != null ? passwordchangehtmlurlDefault : ""); // CHECKSTYLE IGNORE
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

    // Forms Authentication Request (auth form)
    /**
     * Normal test for auth form.
     * Default authorizationhtmlurl not set.
     * Property authorizationhtmlurl not set.
     */
    @Test
    public void authform_default_not_set_and_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String state = "dummy_state";
        String scope = "token";
        String keepLogin = "false";
        String cancelFlg = "0";
        String expiresIn = "2400";

        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, "");

        // Exec.
        TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_OK);

        String cellUrl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/";
        String expected = AuthzUtils.createDefaultHtml(
                clientId, redirectUri, state, scope, responseType, null, null, cellUrl);
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
        assertThat(res.getBody(), is(expected));
    }

    /**
     * Normal test for auth form.
     * Default authorizationhtmlurl not set.
     * Property authorizationhtmlurl set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void authform_default_not_set_and_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, "");
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl>" + AUTHORIZATION_HTML_URL + "</p:authorizationhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, null, HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test for auth form.
     * Default authorizationhtmlurl set.
     * Property authorizationhtmlurl not set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void authform_default_set_and_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, AUTHORIZATION_HTML_URL);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, null, HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML_BODY));
        } finally {
            // Remove.
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test for auth form.
     * Default authorizationhtmlurl set.
     * Property authorizationhtmlurl set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void authform_default_set_and_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, AUTHORIZATION_HTML_URL);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML2_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML2_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML2_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl>" + AUTHORIZATION_HTML2_URL + "</p:authorizationhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, null, HttpStatus.SC_OK);
            // The page set in Property is displayed.
            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML2_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML2_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    // Forms Authentication Request (password change form)
    /**
     * Normal test for password change form.
     * Default authorizationpasswordchangehtmlurl not set.
     * Property authorizationpasswordchangehtmlurl not set.
     */
    @Test
    public void passwordchangeform_default_not_set_and_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String state = "dummy_state";
        String scope = "token";
        String keepLogin = "false";
        String cancelFlg = "0";
        String expiresIn = "2400";
        String apToken = "dummyPasswordChangeToken";

        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT, "");

        // Exec.
        TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, state, scope, keepLogin,
                cancelFlg, expiresIn, apToken, HttpStatus.SC_OK);

        String cellUrl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/";
        String expected = AuthzUtils.createDefaultPasswordChangeHtml(
                clientId, redirectUri, state, scope, responseType, null, null, cellUrl);
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
        assertThat(res.getBody(), is(expected));
    }

    /**
     * Normal test for password change form.
     * Default authorizationpasswordchangehtmlurl not set.
     * Property authorizationpasswordchangehtmlurl set.
     * authorizationpasswordchangehtmlurl:"http:"
     */
    @Test
    public void passwordchangeform_default_not_set_and_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String apToken = "dummyPasswordChangeToken";

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT, "");
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationpasswordchangehtmlurl>" + AUTHORIZATION_HTML_URL
                            + "</p:authorizationpasswordchangehtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, apToken, HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:authorizationpasswordchangehtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test for password change form.
     * Default authorizationpasswordchangehtmlurl set.
     * Property authorizationpasswordchangehtmlurl not set.
     * authorizationpasswordchangehtmlurl:"http:"
     */
    @Test
    public void passwordchangeform_default_set_and_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String apToken = "dummyPasswordChangeToken";

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT,
                    AUTHORIZATION_HTML_URL);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, apToken, HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML_BODY));
        } finally {
            // Remove.
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test for password change form.
     * Default authorizationpasswordchangehtmlurl set.
     * Property authorizationpasswordchangehtmlurl set.
     * authorizationpasswordchangehtmlurl:"http:"
     */
    @Test
    public void passwordchangeform_default_set_and_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String apToken = "dummyPasswordChangeToken";

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATION_PASSWORD_CHANGE_HTM_LURL_DEFAULT,
                    AUTHORIZATION_HTML_URL);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML2_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML2_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    AUTHORIZATION_HTML2_ACL_PATH, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationpasswordchangehtmlurl>" + AUTHORIZATION_HTML2_URL
                            + "</p:authorizationpasswordchangehtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null,
                    null, null, apToken, HttpStatus.SC_OK);
            // The page set in Property is displayed.
            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
            assertThat(res.getBody(), is(AUTHORIZATION_HTML2_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:authorizationpasswordchangehtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML2_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * invalid parameter test.
     */
    @Test
    public void invalid_parameter() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        String state = "dummy_state";
        String scope = "token";
        String keepLogin = "false";
        String cancelFlg = "false";
        String expiresIn = "2400";

        // clientId is invalid.
        TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, "invalid", state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error"));
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0002"));

        // redirectUri is invalid.
        res = AuthzUtils.get(Setup.TEST_CELL1, responseType, "invalid", clientId, state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error"));
        queryMap = UrlUtils.parseQuery(res.getHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0003"));

        // box non installed.
        String clientId2 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA2;
        String redirectUri2 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA2;
        res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri2, clientId2, state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error"));
        queryMap = UrlUtils.parseQuery(res.getHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0007"));

        // responseType is empty.
        res = AuthzUtils.get(Setup.TEST_CELL1, "", redirectUri, clientId, state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(redirectUri));
        Map<String, String> fragmentMap = UrlUtils.parseFragment(res.getHeader(HttpHeaders.LOCATION));
        assertThat(fragmentMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0004"));

        // responseType is invalid.
        res = AuthzUtils.get(Setup.TEST_CELL1, "invalid", redirectUri, clientId, state, scope, keepLogin,
                cancelFlg, expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(redirectUri));
        fragmentMap = UrlUtils.parseFragment(res.getHeader(HttpHeaders.LOCATION));
        assertThat(fragmentMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0001"));

        // expiresIn is invalid.
        res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, state, scope, keepLogin,
                cancelFlg, "invalid", null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(redirectUri));
        fragmentMap = UrlUtils.parseFragment(res.getHeader(HttpHeaders.LOCATION));
        assertThat(fragmentMap.get(OAuth2Helper.Key.CODE), is("PR400-AZ-0008"));

        // isCancel.
        res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, state, scope, keepLogin,
                "true", expiresIn, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(redirectUri));
        fragmentMap = UrlUtils.parseFragment(res.getHeader(HttpHeaders.LOCATION));
        assertThat(fragmentMap.get(OAuth2Helper.Key.CODE), is("PR401-AZ-0001"));
    }

    /**
     * cancel test.
     */
    @Test
    public void cancel() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        // CancelSC_UNAUTHORIZED
        TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, null, null, null, "true",
                null, null, HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getHeader(HttpHeaders.LOCATION).startsWith(redirectUri));
        Map<String, String> fragmentMap = UrlUtils.parseFragment(res.getHeader(HttpHeaders.LOCATION));
        assertThat(fragmentMap.get(OAuth2Helper.Key.CODE), is("PR401-AZ-0001"));
    }
}

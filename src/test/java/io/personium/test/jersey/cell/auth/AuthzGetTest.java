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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
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

    /** Default value of authorizationhtmlurl. */
    private static String authorizationhtmlurlDefault;

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
        authorizationhtmlurlDefault = PersoniumUnitConfig.get(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT);
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
     * Normal test.
     * Default authorizationhtmlurl not set.
     * Property authorizationhtmlurl not set.
     */
    @Test
    public void normal_default_not_set_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, "");

        // Exec.
        TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, HttpStatus.SC_OK);

        String cellUrl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/";
        String expected = AuthzUtils.createDefaultHtml(
                clientId, redirectUri, null, null, responseType, null, null, cellUrl);
        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
        assertThat(res.getBody(), is(expected));
    }

    /**
     * Normal test.
     * Default authorizationhtmlurl set.
     * Property authorizationhtmlurl not set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void normal_default_set_property_not_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        String authorizationhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, authorizationhtmlurl);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, HttpStatus.SC_OK);

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
     * Normal test.
     * Default authorizationhtmlurl not set.
     * Property authorizationhtmlurl set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void normal_default_not_set_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        String authorizationhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, "");
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl>" + authorizationhtmlurl + "</p:authorizationhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, HttpStatus.SC_OK);

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
     * Normal test.
     * Default authorizationhtmlurl set.
     * Property authorizationhtmlurl set.
     * authorizationhtmlurl:"http:"
     */
    @Test
    public void normal_default_set_property_set() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;

        String authorizationhtmlurl1 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        String aclPath1 = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        String authorizationhtmlurl2 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML2_NAME;
        String aclPath2 = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML2_NAME;

        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.AUTHORIZATIONHTMLURL_DEFAULT, authorizationhtmlurl1);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath1, OAuth2Helper.SchemaLevel.NONE);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, AUTHORIZATION_HTML2_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, AUTHORIZATION_HTML2_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath2, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:authorizationhtmlurl>" + authorizationhtmlurl2 + "</p:authorizationhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = AuthzUtils.get(Setup.TEST_CELL1, responseType, redirectUri, clientId, HttpStatus.SC_OK);

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

}

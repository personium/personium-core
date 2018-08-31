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
package io.personium.test.jersey.cell;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * GET Cell root url tests.
 */
@Category({Integration.class})
public class CellRootGetTest extends AbstractCase {

    /** Relay html file name. */
    private static final String RELAY_HTML_NAME = "test.html";
    /** Relay html2 file name. */
    private static final String RELAY_HTML2_NAME = "test2.html";
    /** Relay html file contents. */
    private static final String RELAY_HTML_BODY = "<html><body>This is test html.</body></html>";
    /** Relay html2 file contents. */
    private static final String RELAY_HTML2_BODY = "<html><body>This is test2 html.</body></html>";

    /** Default value of relayhtmlurl. */
    private static String relayhtmlurlDefault;

    /**
     * Constructor.
     */
    public CellRootGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Befor class.
     */
    @BeforeClass
    public static void beforClass() {
        relayhtmlurlDefault = PersoniumUnitConfig.get(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT);
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT,
                relayhtmlurlDefault != null ? relayhtmlurlDefault : ""); // CHECKSTYLE IGNORE
    }

    /**
     * Normal test.
     * Get xml.
     */
    @Test
    public void normal_get_xml() {
        TResponse res = Http.request("cell/cell-root-get.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("accept", MediaType.APPLICATION_XML)
                .returns().debug().statusCode(HttpStatus.SC_OK);

        String cellUrl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1;
        StringBuilder sb = new StringBuilder();
        sb.append("^<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?><cell xmlns=\"urn:x-personium:xmlns\"><uuid>")
        .append(".*")
        .append("</uuid><ctl>")
        .append(cellUrl)
        .append("/__ctl/</ctl></cell>$");
        Pattern pattern = Pattern.compile(sb.toString());

        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_XML));
        assertTrue(pattern.matcher(res.getBody()).matches());
    }

    /**
     * Normal test.
     * Get html.
     * Default relayhtml set.
     * Property relayhtml not set.
     * relayhtmlurl:"http:"
     */
    @Test
    public void normal_get_html_default_set_property_not_set() {
        String relayhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT, relayhtmlurl);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.TEXT_HTML));
            assertThat(res.getBody(), is(RELAY_HTML_BODY));
        } finally {
            // Remove.
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test.
     * Get html.
     * Default relayhtml not set.
     * Property relayhtml set.
     * relayhtmlurl:"http:"
     */
    @Test
    public void normal_get_html_default_not_set_property_set() {
        String relayhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT, "");
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.TEXT_HTML));
            assertThat(res.getBody(), is(RELAY_HTML_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test.
     * Get html.
     * Default relayhtml set.
     * Property relayhtml set.
     * relayhtmlurl:"http:"
     */
    @Test
    public void normal_get_html_default_set_property_set() {
        String relayhtmlurl1 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String aclPath1 = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String relayhtmlurl2 = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML2_NAME;
        String aclPath2 = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML2_NAME;
        try {
            // SetUp.
            PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT, relayhtmlurl1);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath1, OAuth2Helper.SchemaLevel.NONE);
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML2_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML2_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath2, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl2 + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.TEXT_HTML));
            assertThat(res.getBody(), is(RELAY_HTML2_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML2_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Error test.
     * Get html.
     * Default relayhtml not set.
     * Property relayhtml not set.
     */
    @Test
    public void error_get_html_default_not_set_property_not_set() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.Cell.RELAYHTMLURL_DEFAULT, "");
        // Exec.
        TResponse res = Http.request("cell/cell-root-get.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("accept", MediaType.TEXT_HTML)
                .returns().debug().statusCode(HttpStatus.SC_PRECONDITION_FAILED);

        PersoniumCoreException exception = PersoniumCoreException.UI.NOT_CONFIGURED_PROPERTY.params("relayhtmlurl");
        checkErrorResponse(res.bodyAsJson(), exception.getCode(), exception.getMessage());
    }

    /**
     * Normal test.
     * Get html.
     * relayhtmlurl:"personium-localunit:"
     */
    @Test
    public void normal_get_html_unit_url() {
        String relayhtmlurl = UriUtils.SCHEME_LOCALUNIT + ":/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        try {
            // SetUp.
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.TEXT_HTML));
            assertThat(res.getBody(), is(RELAY_HTML_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Normal test.
     * Get html.
     * relayhtmlurl:"personium-localcell:"
     */
    @Test
    public void normal_get_html_cell_url() {
        String relayhtmlurl = UriUtils.SCHEME_LOCALCELL + ":/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        try {
            // SetUp.
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MediaType.TEXT_HTML, MASTER_TOKEN_NAME, RELAY_HTML_BODY,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_OK);

            assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is(MediaType.TEXT_HTML));
            assertThat(res.getBody(), is(RELAY_HTML_BODY));
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, RELAY_HTML_NAME,
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Error test.
     * Get html.
     * relayhtmlurl:"file:"
     */
    @Test
    public void error_get_html_relayhtmlurl_not_url() {
        String relayhtmlurl = "file://" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + RELAY_HTML_NAME;
        try {
            // SetUp.
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_PRECONDITION_FAILED);

            PersoniumCoreException exception = PersoniumCoreException.UI.PROPERTY_NOT_URL.params("relayhtmlurl");
            checkErrorResponse(res.bodyAsJson(), exception.getCode(), exception.getMessage());
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Error test.
     * Get html.
     * Relay response is not html.
     */
    @Test
    public void error_get_html_relay_response_not_html() {
        String relayHtmlName = "test.xml";
        String relayHtmlBody = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><foo>bar</foo>";

        String relayhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + relayHtmlName;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + relayHtmlName;
        try {
            // SetUp.
            DavResourceUtils.createWebDAVFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, relayHtmlName,
                    MediaType.APPLICATION_XML, MASTER_TOKEN_NAME, relayHtmlBody,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setACLPrivilegeAllForAllUser(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    aclPath, OAuth2Helper.SchemaLevel.NONE);
            CellUtils.proppatchSet(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl>" + relayhtmlurl + "</p:relayhtmlurl>",
                    MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // Exec.
            TResponse res = Http.request("cell/cell-root-get.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("accept", MediaType.TEXT_HTML)
                    .returns().debug().statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            PersoniumCoreException exception = PersoniumCoreException.NetWork.UNEXPECTED_RESPONSE
                    .params(relayhtmlurl, MediaType.TEXT_HTML);
            checkErrorResponse(res.bodyAsJson(), exception.getCode(), exception.getMessage());
        } finally {
            // Remove.
            CellUtils.proppatchRemove(
                    Setup.TEST_CELL1,
                    "<p:relayhtmlurl/>",
                    MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteWebDavFile(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, relayHtmlName,
                    MASTER_TOKEN_NAME, -1);
        }
    }
}

package io.personium.test.jersey.cell.auth;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.junit.Test;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * Authorization endpoint tests. GET method.
 */
public class AuthzGetTest extends AbstractCase {

    /** Authorization html file name. */
    private static final String AUTHORIZATION_HTML_NAME = "test.html";
    /** Authorization html file contents. */
    private static final String AUTHORIZATION_HTML_BODY = "<html><body>This is test html.</body></html>";

    /**
     * Constructor.
     */
    public AuthzGetTest() {
        super("io.personium.core.rs");
    }

    // TODO When a response is returned by Transfer-Encoding,
    // there is a bug in which body in TResponse becomes abnormal value.
    // Comment out once until solve a bug in TResponse.
//    /**
//     * Normal test.
//     * Get system default html.
//     */
//    @Test
//    public void normal_get_default_html() {
//        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
//        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
//        String responseType = OAuth2Helper.ResponseType.TOKEN;
//        StringBuilder queryBuilder = new StringBuilder();
//        queryBuilder.append("response_type=").append(responseType)
//        .append("&client_id=").append(PersoniumCoreUtils.encodeUrlComp(clientId))
//        .append("&redirect_uri=").append(PersoniumCoreUtils.encodeUrlComp(redirectUri));
//
//        TResponse res = Http.request("cell/authz-get.txt")
//                .with("cellName", Setup.TEST_CELL1)
//                .with("query", queryBuilder.toString())
//                .returns().debug().statusCode(HttpStatus.SC_OK);
//
//        String cellUrl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/";
//        String message = PersoniumCoreMessageUtils.getMessage("PS-AU-0002");
//        String expected = createDefaultHtml(clientId, redirectUri, message, null, responseType, null, null, cellUrl);
//        assertThat(res.getHeader(HttpHeaders.CONTENT_TYPE), is("text/html;charset=UTF-8"));
//        assertThat(res.getBody(), is(expected));
//    }

    /**
     * Normal test.
     * Get authorization html.
     */
    @Test
    public void normal_get_authorizationhtmlurl() {
        String clientId = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String redirectUri = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL_SCHEMA1;
        String responseType = OAuth2Helper.ResponseType.TOKEN;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("response_type=").append(responseType)
        .append("&client_id=").append(PersoniumCoreUtils.encodeUrlComp(clientId))
        .append("&redirect_uri=").append(PersoniumCoreUtils.encodeUrlComp(redirectUri));

        String authorizationhtmlurl = UrlUtils.getBaseUrl() + "/" + Setup.TEST_CELL1 + "/"
                + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        String aclPath = Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/" + AUTHORIZATION_HTML_NAME;
        try {
            // SetUp.
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

            TResponse res = Http.request("cell/authz-get.txt")
                    .with("cellName", Setup.TEST_CELL1)
                    .with("query", queryBuilder.toString())
                    .returns().debug().statusCode(HttpStatus.SC_OK);

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

    // Create system default html.
    // TODO Should call AuthzEndPointResource.createForm() properly.
//    private String createDefaultHtml(String clientId, String redirectUriStr, String message, String state,
//            String responseType, String pTarget, String pOwner, String cellUrl) {
//        // If processing fails, return system default html.
//        List<Object> paramsList = new ArrayList<Object>();
//
//        if (!"".equals(clientId) && !clientId.endsWith("/")) {
//            clientId = clientId + "/";
//        }
//
//        paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
//        paramsList.add(clientId + Box.DEFAULT_BOX_NAME + "/profile.json");
//        paramsList.add(cellUrl + Box.DEFAULT_BOX_NAME + "/profile.json");
//        paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
//        paramsList.add(cellUrl + "__authz");
//        paramsList.add(message);
//        paramsList.add(state);
//        paramsList.add(responseType);
//        paramsList.add(pTarget != null ? pTarget : ""); // CHECKSTYLE IGNORE
//        paramsList.add(pOwner != null ? pOwner : ""); // CHECKSTYLE IGNORE
//        paramsList.add(clientId);
//        paramsList.add(redirectUriStr);
//        paramsList.add(AuthResourceUtils.getJavascript("ajax.js"));
//
//        Object[] params = paramsList.toArray();
//
//        String html = PersoniumCoreUtils.readStringResource("html/authform.html", CharEncoding.UTF_8);
//        html = MessageFormat.format(html, params);
//
//        return html;
//    }
}

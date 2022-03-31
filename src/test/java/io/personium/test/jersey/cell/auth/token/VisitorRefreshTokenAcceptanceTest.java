/**
 * Personium
 * Copyright 2020-2022 Personium Project Authors
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
package io.personium.test.jersey.cell.auth.token;

import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.PersoniumUrl;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;

/**
 * Tests about refreshing tokens using VisitorRefeshToken at the Token Endpoint.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class VisitorRefreshTokenAcceptanceTest extends PersoniumTest {
    private static Logger log = LoggerFactory.getLogger(VisitorRefreshTokenAcceptanceTest.class);
    static volatile String testCellUrl;
    static volatile String usr1CellUrl;
    static volatile String usr2CellUrl;
    static volatile String app1CellUrl;
    static volatile String app2CellUrl;
    static final String TEST_ROLE_NAME = "role1";

    @BeforeClass
    public static void beforeClass() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        String appCellLocalUnit1 = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String appCellLocalUnit2 = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA2 + ":/";
        String usrCellLocalUnit1 = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String usrCellLocalUnit2 = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL2 + ":/";
        app1CellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit1);
        app2CellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit2);
        usr1CellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit1);
        usr2CellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit2);
    }

    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.reload();
    }

    /**
     * Constructor.
     */
    public VisitorRefreshTokenAcceptanceTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Should_FailRefreshingToken_When_NewClientSpecifiedForTokenWithoutSchema.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void Should_FailRefreshingToken_When_NewClientSpecifiedForTokenWithoutSchema()
            throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {

        // Generate Refresh Token with schema app2CellUrl
        VisitorRefreshToken rt = new VisitorRefreshToken(
            "" + new Date().getTime(),
            new Date().getTime(),
            AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
            usr1CellUrl, // issuer
            usr2CellUrl + "#account1", // subject
            null,
            null,
            app2CellUrl,
            new String[] {"root"}
        );

        // Generate AppAuth Token from app1CellUrl
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(app1CellUrl, app1CellUrl + "#account1",
                usr1CellUrl, roleList,
                null, null);

        // Refresh Token
        HttpResponse res  = refreshToken(usr1CellUrl, rt.toTokenString(), null, app1CellUrl, appAuthToken.toTokenString());
        // Should be error
        assertEquals(401, res.getStatusLine().getStatusCode());
        log.info(parseJsonResponse(res).toString());
    }

    /**
     * Should_FailRefreshingToken_When_ClientIdNotMatchesSchemaInRefreshToken.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void Should_FailRefreshingToken_When_ClientIdNotMatchesSchemaInRefreshToken()
            throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {

        // Generate Refresh Token without schema
        VisitorRefreshToken rt = new VisitorRefreshToken(
            "" + new Date().getTime(),
            new Date().getTime(),
            AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
            usr1CellUrl, // issuer
            usr2CellUrl + "#account1", // subject
            null,
            null,
            app2CellUrl,
            new String[] {"root"}
        );

        // Generate AppAuth Token
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(
            app1CellUrl,
            app1CellUrl + "#account1",
            usr1CellUrl,
            roleList,
            null, null);

        // Refresh Token
        HttpResponse res  = refreshToken(usr1CellUrl, rt.toTokenString(), app2CellUrl, app1CellUrl, appAuthToken.toTokenString());

        // Should be error
        assertEquals(401, res.getStatusLine().getStatusCode());
        log.info(parseJsonResponse(res).toString());
    }

    /**
     * Should_FailRefrehingToken__When_RefreshTokenHasSchemaButNoAppAuth.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void Should_FailRefrehingToken__When_RefreshTokenHasSchemaButNoAppAuth()
            throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        // Generate Refresh Token
        VisitorRefreshToken rt = new VisitorRefreshToken(
            "" + new Date().getTime(),
            new Date().getTime(),
            AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
            usr1CellUrl, // issuer
            usr2CellUrl + "#account1", // subject
            null,
            null,
            app1CellUrl,
            new String[] {"root"}
        );

        // Refresh Token
        HttpResponse res = refreshToken(usr1CellUrl, rt.toTokenString(), null);
        assertEquals(401, res.getStatusLine().getStatusCode());
    }

    /**
     * Should_SuccessRefrehingToken__When_ClientIdMatchesSchemaInRefreshToken.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void Should_SuccessRefrehingToken__When_ClientIdMatchesSchemaInRefreshToken()
            throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        // Generate Refresh Token
        VisitorRefreshToken rt = new VisitorRefreshToken(
            "" + new Date().getTime(),
            new Date().getTime(),
            AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
            usr1CellUrl, // issuer
            usr2CellUrl + "#account1", // subject
            null,
            null,
            app1CellUrl,
            new String[] {"root"}
        );

        // Generate AppAuth Token
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(app1CellUrl, app1CellUrl + "#account1",
                usr1CellUrl, roleList,
                null, null);

        // Refresh Token
        HttpResponse res = refreshToken(usr1CellUrl, rt.toTokenString(), app1CellUrl, app1CellUrl, appAuthToken.toTokenString());
        assertEquals(200, res.getStatusLine().getStatusCode());
        JsonObject j  = parseJsonResponse(res);

        String at = j.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        log.info(at);

        TransCellAccessToken tcat = TransCellAccessToken.parse(at);
        String aud = tcat.getTarget();
        log.info(aud);

        assertFalse(aud.startsWith(SCHEME_LOCALUNIT));
        assertTrue(aud.startsWith("http"));
    }


    /**
     * Should_SuccessRefrehingToken__When_ClientIdNullAndRefreshTokenWithoutSchema.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void Should_SuccessRefrehingToken__When_ClientIdNullAndRefreshTokenWithoutSchema()
            throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        // Generate Refresh Token
        VisitorRefreshToken rt = new VisitorRefreshToken(
            "" + new Date().getTime(),
            new Date().getTime(),
            AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
            usr1CellUrl, // issuer
            usr2CellUrl + "#account1", // subject
            null,
            null,
            null,
            new String[] {"root"}
        );

        // Refresh Token
        HttpResponse res = refreshToken(usr1CellUrl, rt.toTokenString(), app1CellUrl);
        assertEquals(200, res.getStatusLine().getStatusCode());
        JsonObject j  = parseJsonResponse(res);

        String at = j.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        log.info(at);

        TransCellAccessToken tcat = TransCellAccessToken.parse(at);
        String aud = tcat.getTarget();
        log.info(aud);

        assertFalse(aud.startsWith(SCHEME_LOCALUNIT));
        assertTrue(aud.startsWith("http"));
    }
    
    /**
     * Should_SuccessRefrehingToken__When_ClientIdMatchesSchemaInRefreshToken.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void RefreshedAccessToken_ShouldHaveAppropriate_Roles() throws Exception {
        // Create a Cell for this test
        createTestCell();
        String testCellUrl = PersoniumUrl.create(SCHEME_LOCALUNIT + ":" + testCellName + ":/").toHttp();
        log.info("testCellUrl = " +  testCellUrl);
        // Create ExtCell pointing to TEST_CELL2
        createExtCellOnTestCell(usr2CellUrl);
        // Create A Role
        createRoleOnTestCell(TEST_ROLE_NAME);
        // Assign A Role
        linkRoleToExtCellOnTestCell(usr2CellUrl, TEST_ROLE_NAME);
        // Configure the role to have all privilege.
        grantAllPrivToRoleOnTestCell(TEST_ROLE_NAME);
        
        try {
            // Generate Refresh Token
            
            VisitorRefreshToken rt = new VisitorRefreshToken(
                "" + new Date().getTime(),
                new Date().getTime(),
                AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS,
                testCellUrl, // issuer
                usr2CellUrl + "#account1", // subject
                null,
                null,
                app1CellUrl,
                new String[] {"root"}
            );
    
            // Generate AppAuth Token
            List<Role> roleList = new ArrayList<Role>();
            TransCellAccessToken appAuthToken = new TransCellAccessToken(app1CellUrl, app1CellUrl + "#account1",
                    testCellUrl, roleList,
                    null, null);
    
            // Refresh Token
            HttpResponse res = refreshToken(testCellUrl, rt.toTokenString(), null, app1CellUrl, appAuthToken.toTokenString());
            assertEquals(200, res.getStatusLine().getStatusCode());
            JsonObject j  = parseJsonResponse(res);
    
            String at = j.getString(OAuth2Helper.Key.ACCESS_TOKEN);
            log.info(at);
    
            VisitorLocalAccessToken vlat = VisitorLocalAccessToken.parse(at, testCellUrl);
            log.info("num roles = " + vlat.getRoleList().size());
            assertEquals(1, vlat.getRoleList().size());
            Role r = vlat.getRoleList().get(0);
            assertEquals(TEST_ROLE_NAME, r.getName());
        } finally {
            // Delete the Cell for this test
            deleteTestCell();
        }
    }


    private static JsonObject parseJsonResponse(HttpResponse res) {
        try (InputStream is = res.getEntity().getContent()){
            return Json.createReader(is).readObject();
        } catch (UnsupportedOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpResponse refreshToken(String cellUrl, String refreshToken, String pTarget)
            throws ClientProtocolException, IOException {
        return refreshToken(cellUrl, refreshToken, pTarget, null, null);
    }

    private static HttpResponse refreshToken(String cellUrl, String refreshToken, String pTarget, String clientId, String clientSecret)
            throws ClientProtocolException, IOException {
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL);

        String tokenEndpoint = cellUrl + "__token";
        log.info("Testing against: " + tokenEndpoint);

        HttpPost post = new HttpPost(tokenEndpoint);

        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=refresh_token&refresh_token=");
        sb.append(refreshToken);
        if (pTarget != null) {
            sb.append("&p_target=");
            sb.append(pTarget);
        }
        if (clientId != null) {
            sb.append("&client_id=");
            sb.append(clientId);
            sb.append("&client_secret=");
            sb.append(clientSecret);
        }

        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

        HttpEntity reqEntity = new StringEntity(sb.toString());
        post.setEntity(reqEntity);
        return client.execute(post);
    }
}


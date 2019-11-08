/**
 * Personium
 * Copyright 2019 Personium Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;

/**
 * Tests about refreshing tokens using ResidentRefeshToken at the Token Endpoint.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ResidentRefreshTokenAcceptanceTest extends PersoniumTest {

    static final int MILLISECS_IN_AN_MINITE = 60 * 1000;
    private static Logger log = LoggerFactory.getLogger(ResidentRefreshTokenAcceptanceTest.class);


    /**
     * Constructor.
     */
    public ResidentRefreshTokenAcceptanceTest() {
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
        String appCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        // Generate Refresh Token without schema (schema null)
        ResidentRefreshToken clrt = new ResidentRefreshToken(usrCellUrl, "account1", null, null);

        // Generate AppAuth Token
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(appCellUrl, appCellUrl + "#account1",
                usrCellUrl, roleList,
                null, null);

        // Refresh Token
        HttpResponse res  = refreshToken(usrCellUrl, clrt.toTokenString(), null, appCellUrl, appAuthToken.toTokenString());
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
        String appCellLocalUnit1 = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String appCellLocalUnit2 = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA2 + ":/";
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl1 = UriUtils.resolveLocalUnit(appCellLocalUnit1);
        String appCellUrl2 = UriUtils.resolveLocalUnit(appCellLocalUnit2);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        // Generate Refresh Token without schema
        ResidentRefreshToken clrt = new ResidentRefreshToken(usrCellUrl, "account1", appCellUrl2, new String [] {"scope1"});

        // Generate AppAuth Token
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(appCellUrl1, appCellUrl1 + "#account1",
                usrCellUrl, roleList,
                null, null);

        // Refresh Token
        HttpResponse res  = refreshToken(usrCellUrl, clrt.toTokenString(), appCellUrl2, appCellUrl1, appAuthToken.toTokenString());

        // Should be error
        assertEquals(401, res.getStatusLine().getStatusCode());
        log.info(parseJsonResponse(res).toString());
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
        String appCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        // Generate Refresh Token
        ResidentRefreshToken clrt = new ResidentRefreshToken(usrCellUrl, "account1", appCellUrl, new String[] {"scope1"});

        // Generate AppAuth Token
        List<Role> roleList = new ArrayList<Role>();
        TransCellAccessToken appAuthToken = new TransCellAccessToken(appCellUrl, appCellUrl + "#account1",
                usrCellUrl, roleList,
                null, null);

        // Refresh Token
        HttpResponse res = refreshToken(usrCellUrl, clrt.toTokenString(), appCellUrl, appCellUrl, appAuthToken.toTokenString());
        assertEquals(200, res.getStatusLine().getStatusCode());
        JsonObject j  = parseJsonResponse(res);

        String at = j.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        log.info(at);

        TransCellAccessToken tcat = TransCellAccessToken.parse(at);
        String aud = tcat.getTarget();
        log.info(aud);

        assertFalse(aud.startsWith(UriUtils.SCHEME_LOCALUNIT));
        assertTrue(aud.startsWith("http"));
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
        String appCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        // Generate Refresh Token
        ResidentRefreshToken clrt = new ResidentRefreshToken(usrCellUrl, "account1", appCellUrl, new String[] {"scope1"});

        // Refresh Token
        HttpResponse res = refreshToken(usrCellUrl, clrt.toTokenString(), null);
        assertEquals(401, res.getStatusLine().getStatusCode());
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
        String appCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        // Generate Refresh Token
        ResidentRefreshToken clrt = new ResidentRefreshToken(usrCellUrl, "account1", null, new String[] {"scope1", "scope2"});

        // Refresh Token
        HttpResponse res = refreshToken(usrCellUrl, clrt.toTokenString(), appCellUrl);
        assertEquals(200, res.getStatusLine().getStatusCode());
        JsonObject j  = parseJsonResponse(res);

        String at = j.getString(OAuth2Helper.Key.ACCESS_TOKEN);
        log.info(at);

        TransCellAccessToken tcat = TransCellAccessToken.parse(at);
        String aud = tcat.getTarget();
        log.info(aud);

        assertFalse(aud.startsWith(UriUtils.SCHEME_LOCALUNIT));
        assertTrue(aud.startsWith("http"));
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
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_DEFAULT);

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


/**
 * Personium
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
package io.personium.test.jersey.cell.auth.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.io.IOException;
import java.io.InputStream;

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
import io.personium.common.auth.token.ResidentLocalAccessToken;
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
 * Tests about tokens issuance at the Token Endpoint.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class TokenIssuanceTest extends PersoniumTest {

    static final int MILLISECS_IN_AN_MINITE = 60 * 1000;
    private static Logger log = LoggerFactory.getLogger(TokenIssuanceTest.class);


    /**
     * Constructor.
     */
    public TokenIssuanceTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * When p_target is localunit scheme URL, then Trans-Cell Access Token issued should have http scheme audience.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void When_PTargetLocalunitSchemeURL_Then_TCATShouldHaveAudienceHttpSchemeURL () throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        String cellUrl = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String targetUrl = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL2 + ":/";
        cellUrl = UriUtils.resolveLocalUnit(cellUrl);
        String at = this.callROPC(cellUrl, "account1", "password1", targetUrl).getString("access_token");
        TransCellAccessToken tcat = TransCellAccessToken.parse(at);
        String aud = tcat.getTarget();
        log.info(aud);

        assertFalse(aud.startsWith(SCHEME_LOCALUNIT));
        assertTrue(aud.startsWith("http"));
    }

    /**
     * When request with empty body and no header, then token endpoint should respond with 400.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void When_RequestWithNoHeadersEmptyBody_Then_Return400BadRequest()
            throws IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        String cellUrl = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        cellUrl = UriUtils.resolveLocalUnit(cellUrl);
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_DEFAULT);

        String tokenEndpoint = cellUrl + "__token";
        log.info("Testing against: " + tokenEndpoint);

        HttpPost post = new HttpPost(tokenEndpoint);
        HttpResponse res = client.execute(post);
        assertEquals(400, res.getStatusLine().getStatusCode());

        try (InputStream is = res.getEntity().getContent()){
            JsonObject obj = Json.createReader(is).readObject();
            log.info("Response: " + obj.toString());
            assertTrue(obj.getString(OAuth2Helper.Key.ERROR_DESCRIPTION).startsWith("[PR400-AN-0016]"));
        }
    }


    /**
     * When client_id is localunit scheme URL, then app auth should still work.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public final void When_ClientIdLocalunitSchemeURL_Then_StillTheAppAuthShouldWork () throws ClientProtocolException, IOException, TokenParseException, TokenDsigException, TokenRootCrtException {
        String appCellLocalUnit = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
        String usrCellLocalUnit = SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
        String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

        String clientSecret = this.callROPC(appCellUrl, "account1", "password1", usrCellUrl).getString("access_token");
        String at = this.callROPC(usrCellUrl, "account1", "password1", null, appCellLocalUnit, clientSecret).getString("access_token");
        log.info("token:" + at);

        ResidentLocalAccessToken aat = ResidentLocalAccessToken.parse(at, usrCellUrl);

        String schema = aat.getSchema();
        log.info(schema);
        assertTrue(schema.startsWith(appCellUrl));
    }

    private JsonObject callROPC(String cellUrl, String username, String password, String pTarget)
            throws ClientProtocolException, IOException {
        return callROPC(cellUrl, username, password, pTarget, null, null);
    }

    private JsonObject callROPC(String cellUrl, String username, String password, String pTarget, String clientId, String clientSecret)
            throws ClientProtocolException, IOException {
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_DEFAULT);

        String tokenEndpoint = cellUrl + "__token";
        log.info("Testing against: " + tokenEndpoint);

        HttpPost post = new HttpPost(tokenEndpoint);

        StringBuilder sb = new StringBuilder();
        sb.append("grant_type=password&username=");
        sb.append(username);
        sb.append("&password=");
        sb.append(password);
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
        HttpResponse res = client.execute(post);

        try (InputStream is = res.getEntity().getContent()){
            return Json.createReader(is).readObject();
        }
    }



}


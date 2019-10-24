/**
 * Personium
 * Copyright 2019 Personium Project
 *  - Akio SHIMONO
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
package io.personium.test.jersey.scenario;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.io.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.utils.CommonUtils;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;

/**
 * Scenario Test simulating the flows on the HomeApp.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Integration.class})
public class HomeAppScenarioTest extends PersoniumTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String TEST_CELL2 = Setup.TEST_CELL2;
    static final String TEST_APP_CELL1 = Setup.TEST_CELL_SCHEMA1;

    static final String TEST_BOX = Setup.TEST_BOX1;
    static final String DAV_COLLECTION = "setdavcol/";
    static final String ODATA_COLLECTION = "setodata/";
    static final String DAV_RESOURCE = "dav.txt";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;


    private String appCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL_SCHEMA1 + ":/";
    private String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
    private String appCellUrl = UriUtils.resolveLocalUnit(appCellLocalUnit);
    private String usrCellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);

    private static Logger log = LoggerFactory.getLogger(HomeAppScenarioTest.class);

    HttpClientContext hcContext;

    /**
     * Constructor.
     */
    public HomeAppScenarioTest() {
        super(new PersoniumCoreApplication());
        this.hcContext = HttpClientContext.create();
        CookieStore cookieStore = new BasicCookieStore();
        this.hcContext.setCookieStore(cookieStore);
    }


    /**
     * App launch flow used before the introduction of OpenID Connect.
     *  - ROPC Login (Home App side)
     *  -   SetCookie from User Cell
     *  - App Launch (App side)
     *  -  Grant Code issued via OAuth 2.0 flow with the Cookie
     *  -  App Authentication against app cell
     *  -  access token issued from token endpoint of user cell.
     *  - BoxDiscovery
     *  - GET BoxMetadata
     */
    @Test
    public final void ROPCLogin_SetCookie_AppLaunch_FromCookieToGrantCode_BoxDiscovery_BoxMetadata()
            throws ClientProtocolException, IOException, TokenParseException {
        //ROPC at Test Cell1 By Homeapp
        //account2 (linked with role2) can read the box1 (schema = appCellUrl)
        JsonHttpResponse res = this.callROPC(this.usrCellUrl, "account2", "password2", null, null, null, true);

        //Start OAuth 2.0 process
        //   call authorization endpoint
        String state = new Date().getTime() + "";
        res = this.callAuthzEndpoint(this.usrCellUrl, this.appCellUrl,
                this.appCellUrl +"__/html/Engine/receive_redirect?cellUrl=", state);
        //   should be redirected
        assertEquals(303, res.statusCode);
        String location = res.getHeader("Location");
        log.info(location);

        //   parsing redirect url (location header)
        List<NameValuePair> queryList = URLEncodedUtils.parse(location, Charsets.UTF_8);
        Map<String, String> queryMap = queryList.stream().collect(
                Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        String grantCode = queryMap.get("code");
        log.info("GrantCode: " + grantCode);

        // simulate the redirect receiver
        //  account0 has no role
        //  account1 has confidential role
        res = this.callROPC(this.appCellUrl, "account0", "password0", this.usrCellUrl, null, null, false);
        String appAuthToken = res.jsonObject.getString("access_token");
        log.info("AppAuthToken: " + appAuthToken);

        // retrieve AccessToken using grant code
        res = this.callTokenEndpointWithGrantCode(this.usrCellUrl, grantCode, null, this.appCellUrl, appAuthToken, false);
        assertEquals(200, res.statusCode);
        String at = res.jsonObject.getString("access_token");
        log.info("AccessToken: " + at);

        //        ResidentLocalAccessToken vlat = ResidentLocalAccessToken.parse(at, this.usrCellUrl);
        //        log.info(" Scopes:"+String.join(" ", vlat.getScope()));
        //        log.info(" Roles:"+vlat.getRoles());
        //        log.info(" Subject:"+vlat.getSubject());
        //        log.info(" Issuer :"+vlat.getIssuer());

        // Box URL discovery
        res = this.getBoxUrl(this.usrCellUrl, at);
        log.info(res.jsonObject.toString());
        String boxUrl = res.jsonObject.getString("Url");
        assertEquals(200, res.statusCode);

        // Box Metadata Access
        res = this.getBoxMetadata(boxUrl, at);
        log.info(res.jsonObject.toString());
        assertEquals(200, res.statusCode);
    }

    private JsonHttpResponse getBoxMetadata(String boxUrl, String accessToken) {
        try(CloseableHttpClient client = this.createHttpClient()) {
            HttpGet get = new HttpGet(boxUrl);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return new JsonHttpResponse(client.execute(get));
         } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private JsonHttpResponse getBoxUrl(String cellUrl, String accessToken) {
        try(CloseableHttpClient client = this.createHttpClient()) {
            String url = cellUrl + "__box";
            HttpGet get = new HttpGet(url);
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return new JsonHttpResponse(client.execute(get));
         } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private JsonHttpResponse callROPC(String cellUrl, String username, String password, String pTarget,
            String clientId, String clientSecret, Boolean pCookie)
            throws ClientProtocolException, IOException {
        try(CloseableHttpClient client = this.createHttpClient()) {
            String tokenEndpoint = cellUrl + "__token";
            HttpPost post = new HttpPost(tokenEndpoint);
            // POST payload
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
                sb.append(CommonUtils.encodeUrlComp(clientId));
                sb.append("&client_secret=");
                sb.append(clientSecret);
            }
            if (pCookie) {
                sb.append("&p_cookie=true");
            }
            HttpEntity reqEntity = new StringEntity(sb.toString());
            post.setEntity(reqEntity);
            // Request Headers
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            // Make a request
            return new JsonHttpResponse(client.execute(post));
        }
    }
    private JsonHttpResponse callTokenEndpointWithGrantCode(String cellUrl, String grantCode, String pTarget,
            String clientId, String clientSecret, Boolean pCookie)
            throws ClientProtocolException, IOException {
        try(CloseableHttpClient client = this.createHttpClient()) {
            String tokenEndpoint = cellUrl + "__token";
            HttpPost post = new HttpPost(tokenEndpoint);
            // POST payload
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code&code=");
            sb.append(grantCode);
            if (pTarget != null) {
                sb.append("&p_target=");
                sb.append(pTarget);
            }
            if (clientId != null) {
                sb.append("&client_id=");
                sb.append(CommonUtils.encodeUrlComp(clientId));
                sb.append("&client_secret=");
                sb.append(clientSecret);
            }
            if (pCookie) {
                sb.append("&p_cookie=true");
            }
            HttpEntity reqEntity = new StringEntity(sb.toString());
            post.setEntity(reqEntity);
            // Request Headers
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            // Make a request
            return new JsonHttpResponse(client.execute(post));
        }
    }

    private JsonHttpResponse callAuthzEndpoint(String cellUrl, String clientId, String redirectUri, String state)
            throws ClientProtocolException, IOException {
        try(CloseableHttpClient client = this.createHttpClient()) {
            String authzEndpoint = cellUrl + "__authz";
            HttpPost post = new HttpPost(authzEndpoint);
            // POST payload
            StringBuilder sb = new StringBuilder();
            sb.append("response_type=code&");
            sb.append("client_id=");
            sb.append(CommonUtils.encodeUrlComp(clientId));
            sb.append("&redirect_uri=");
            sb.append(CommonUtils.encodeUrlComp(redirectUri));
            sb.append("&state=");
            sb.append(CommonUtils.encodeUrlComp(state));
            HttpEntity reqEntity = new StringEntity(sb.toString());
            post.setEntity(reqEntity);
            // Request Headers
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            // Make a request
            return new JsonHttpResponse(client.execute(post));
        }
    }
    // create an HttpClient that
    //   - shares cookie store
    //   - does not handle redirect automatically
    private CloseableHttpClient createHttpClient() {
        RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build();
        return HttpClients.custom().setDefaultRequestConfig(globalConfig).disableRedirectHandling()
                .setDefaultCookieStore(this.hcContext.getCookieStore()).build();
    }
    // inner class to handle compact-sized JSON Http response
    class JsonHttpResponse {
        JsonObject jsonObject;
        int statusCode;
        List<Header> headers;
        JsonHttpResponse(HttpResponse res){
            this.statusCode = res.getStatusLine().getStatusCode();
            headers = Arrays.asList(res.getAllHeaders());
            if (res.getEntity().getContentLength() == 0 ) {
                return;
            }
            try (InputStream is = res.getEntity().getContent()){
                this.jsonObject = Json.createReader(is).readObject();
            } catch (UnsupportedOperationException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        public String getHeader(String name) {
            Map<String, String> headerMap = this.headers.stream().collect(
                    Collectors.toMap(Header::getName, Header::getValue)
            );
            return headerMap.get(name);
        }
    }
}

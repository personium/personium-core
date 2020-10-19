/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.cookie.Cookie;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.rs.cell.AuthResourceUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.UrlUtils;

/**
 * ImplicitFlow認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
@SuppressWarnings("restriction")
public class ImplicitFlowTest extends PersoniumTest {

    private static final String REDIRECT_HTML = "__/redirect.html";
    static final String DEFAULT_STATE = "0000000111";

    // TODO : It will be used after implementing keeplogin.
    private static final String MAX_AGE = "maxAge";
    private static final String SESSION_ID = OAuth2Helper.Key.SESSION_ID;
    private List<Cookie> cookies = null;

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
     * Constructor.
     */
    public ImplicitFlowTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Execute ImplicitFlow authentication request for non-existent Cell and get 404 error.
     */
    @Test
    public final void not_existent_cell() {
        String reqCell = UrlUtils.cellRoot("dummyCell");

        PersoniumResponse res = requesttoAuthz(null, reqCell, Setup.TEST_CELL_SCHEMA1, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
    }

    /**
     * Authorization processing success.
     */
    @Test
    public final void normal() {

        String addbody = "&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        try {
            ResidentLocalAccessToken aToken = ResidentLocalAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL1));
            assertNotNull("access token parse error.", aToken);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
            assertFalse(response.containsKey(OAuth2Helper.Key.BOX_NOT_INSTALLED));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * If Box does not exist in the cell schema specified in client_id, return box_not_installed = true.
     */
    @Test
    public final void normal_box_not_found() {
        String clientId = UrlUtils.cellRoot("dummyCell");
        String addbody = "&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, clientId, null);
        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(clientId + REDIRECT_HTML));
        Map<String, String> fragmentMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        assertNotNull(fragmentMap.get(OAuth2Helper.Key.ACCESS_TOKEN));
        assertThat(fragmentMap.get(OAuth2Helper.Key.BOX_NOT_INSTALLED), is("true"));
    }

    /**
     * If the Box of Cell schema specified in client_id is another Cell, return box_not_installed = true.
     */
    @Test
    public final void normal_box_another_cell_schema() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String addbody = "&username=account2&password=password2";
        String cellName = "authztestcell";

        try {
            // Cell creation
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            AccountUtils.create(Setup.MASTER_TOKEN_NAME, cellName, "account2", "password2", HttpStatus.SC_CREATED);

            // Execute authz request
            PersoniumResponse res = requesttoAuthz(addbody, cellName, clientId, null);
            assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());
            assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(clientId + REDIRECT_HTML));
            Map<String, String> fragmentMap = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
            assertNotNull(fragmentMap.get(OAuth2Helper.Key.ACCESS_TOKEN));
            assertThat(fragmentMap.get(OAuth2Helper.Key.BOX_NOT_INSTALLED), is("true"));
        } finally {
            AccountUtils.delete(cellName, Setup.MASTER_TOKEN_NAME, "account2", -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName);
        }
    }

    /**
     * Authentication failure. (bad password)
     */
    @Test
    public final void authentication_failed_bad_password() {

        String addbody = "&username=account2&password=dummypassword";

        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
    }

    /**
     * Authentication failure. (username and password not set)
     */
    @Test
    public final void authentication_failed_not_set_username_and_password() {

        // not set username
        String addbody = "&username=&password=password2";
        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));

        // not set password
        addbody = "&username=account2&password=";
        res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));

        // not set username and password .
        addbody = "&username=&password=";
        res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));
    }

    /**
     * Authentication failure. (account not found)
     */
    @Test
    public final void authentication_failed_account_not_found() {

        String addbody = "&username=dummyaccount&password=dummypassword";
        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0004"));
    }

    /**
     * If you specify a string that is not in URL format in redirect_uri, you will be redirected to the error page.
     */
    @Test
    public final void errorpage_invalid_redirectUrl() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = REDIRECT_HTML;

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * If redirect_uri and client_id are different, redirect to the error page.
     */
    @Test
    public final void errorpage_redirectUri_and_clientId_are_different() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL2) + REDIRECT_HTML;

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * If you execute an authentication request to the Cell specified by client_id, it will be redirected to the error page.
     */
    @Test
    public final void errorpage_clientId_same_cell() {
        String addbody = "&username=account2&password=password2";
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL1);

        try {
            // Box作成
            BoxUtils.createWithSchema(Setup.TEST_CELL1, "authzBox", AbstractCase.MASTER_TOKEN_NAME, clientId);

            PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, clientId, null);
            assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        } finally {
            BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "authzBox");
        }
    }

    /**
     * If an invalid string is specified in response_type, 303 will be returned.
     */
    @Test
    public final void error_invalid_responseType() {
        String responseType = "test";

        String body = "response_type=" + responseType + "&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE + "&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // #error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=unsupported_response_type&error_description=Unsupported+response_type.&state=" + DEFAULT_STATE
                + "&code=PR400-AZ-0001",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * If an invalid string is specified in response_type, 303 will be returned.
     */
    @Test
    public final void error_invalid_expiresIn() {
        // If expires_in is not a number
        String expiresIn = "dummy";
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE + "&expires_in=" + expiresIn + "&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // #error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=invalid_request&error_description=Request+parameter+is+invalid+%5Bexpires_in%5D."
                + "&state=" + DEFAULT_STATE + "&code=PR400-AZ-0008",
                res.getFirstHeader(HttpHeaders.LOCATION));

        // If expires_in is out of range
        expiresIn = "-1";
        body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE + "&expires_in=" + expiresIn + "&username=account2&password=password2";

        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // #error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=invalid_request&error_description=Request+parameter+is+invalid+%5Bexpires_in%5D."
                + "&state=" + DEFAULT_STATE + "&code=PR400-AZ-0008",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * If canceled, 303 will be returned.
     */
    @Test
    public final void cancel() {
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE + "&cancel_flg=true&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // #error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=unauthorized_client&error_description=User+cancel.&state=" + DEFAULT_STATE
                + "&code=PR401-AZ-0001",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること.
     */
    // TODO : It corresponds after the implementation of keeplogin.
    @Ignore
    @Test
    public final void パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること() {

        String addbody = "&username=account2&password=password2&keeplogin=true";

        PersoniumResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
        try {
            ResidentLocalAccessToken aToken = ResidentLocalAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL1));
            assertNotNull("access token parse error.", aToken);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * __authzへのリクエストを投入する.
     * @param addbody 追加のボディ
     * @return レスポンス
     */
    private PersoniumResponse requesttoAuthz(String addbody) {
        return requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);
    }

    /**
     * __authzへのリクエストを投入する.
     * @param addbody 追加のボディ
     * @param requestCellName requestCellName
     * @param clientId client_id
     * @param redirecturi redirect_uri
     * @return レスポンス
     */
    private PersoniumResponse requesttoAuthz(String addbody, String requestCellName,
            String clientId, String redirecturi) {
        if (addbody == null) {
            addbody = "";
        }

        // 定型のリクエストbodyを作成しておく
        if (redirecturi == null) {
            redirecturi = clientId + REDIRECT_HTML;
        }
        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;

        return requesttoAuthzWithBody(requestCellName, body);
    }

    /**
     * __authzへのリクエストを投入する.
     * @param requestCellName requestCellName
     * @param body ボディ
     * @return レスポンス
     */
    private PersoniumResponse requesttoAuthzWithBody(String requestCellName, String body) {
        return requesttoAuthzWithBody(requestCellName, body, null);
    }

    /**
     * __authzへのリクエストを投入する.
     * @param requestCellName requestCellName
     * @param body ボディ
     * @param requestheaders 追加のリクエストヘッダ
     * @return レスポンス
     */
    private PersoniumResponse requesttoAuthzWithBody(String requestCellName,
            String body,
            HashMap<String, String> requestheaders) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        if (requestheaders == null) {
            requestheaders = new HashMap<String, String>();
        }
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            cookies = null;
            res = rest.post(UrlUtils.cellRoot(requestCellName) + "__authz", body, requestheaders);

            cookies = rest.getCookies();

        } catch (PersoniumException e) {
            e.printStackTrace();
        }

        return res;
    }

//    private Map<String, Object> getSessionMap() {
//        Map<String, Object> sessionMap = new HashMap<String, Object>();
//        String sessionId = null;
//        Date maxAge = null;
//        for (Cookie cookie : cookies) {
//            if (SESSION_ID.equals(cookie.getName())) {
//                sessionId = cookie.getValue();
//                maxAge = cookie.getExpiryDate();
//            }
//        }
//        sessionMap.put(SESSION_ID, sessionId);
//        sessionMap.put(MAX_AGE, maxAge);
//
//        return sessionMap;
//    }

    static void checkHtmlBody(PersoniumResponse res, String messageId, String dataCellName) {
        checkHtmlBody(res, messageId, dataCellName, "");
    }

    static void checkHtmlBody(PersoniumResponse res, String messageId, String dataCellName, String dcOwner) {
        DOMParser parser = new DOMParser();
        InputSource body = null;
        body = new InputSource(res.bodyAsStream());
        try {
            parser.parse(body);
        } catch (SAXException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Document document = parser.getDocument();
        NodeList nodeList = document.getElementsByTagName("script");
        assertEquals(AuthResourceUtils.getJavascript("ajax.js"), ((Element) nodeList.item(0)).getTextContent());

        nodeList = document.getElementsByTagName("title");
        assertEquals(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"), ((Element) nodeList.item(0)).getTextContent());

        nodeList = document.getElementsByTagName("body");
        String expectedAppUrl = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + "__/profile.json";
        String expectedDataUrl = UrlUtils.cellRoot(dataCellName) + "__/profile.json";
        assertEquals("requestFile('GET', '" + expectedAppUrl + "' , '" + expectedDataUrl + "' ,true )",
                ((Element) nodeList.item(0)).getAttribute("onload"));

        nodeList = document.getElementsByTagName("h1");
        assertEquals(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"), ((Element) nodeList.item(0)).getTextContent());

        nodeList = document.getElementsByTagName("form");
        String expectedFormUrl = UrlUtils.cellRoot(dataCellName) + "__authz";
        assertEquals(expectedFormUrl, ((Element) nodeList.item(0)).getAttribute("action"));

        nodeList = document.getElementsByTagName("div");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("id");
            if ("message".equals(id)) {
                assertEquals(PersoniumCoreMessageUtils.getMessage(messageId).replaceAll("<br />", ""),
                        element.getTextContent());
            }
        }

        nodeList = document.getElementsByTagName("input");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("id");
            if ("state".equals(id)) {
                assertEquals(DEFAULT_STATE, element.getAttribute("value"));
            } else if ("p_target".equals(id)) {
                assertEquals("", element.getAttribute("value"));

            } else if ("p_owner".equals(id)) {
                assertEquals(dcOwner, element.getAttribute("value"));
            } else if ("client_id".equals(id)) {
                assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), element.getAttribute("value"));
            } else if ("redirect_uri".equals(id)) {
                assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML,
                        element.getAttribute("value"));
            }
        }
    }
}

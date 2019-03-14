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

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;

/**
 * account status test for authorization.
 */
@SuppressWarnings("restriction")
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthzAccountStatusTest extends PersoniumTest {

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthzAccountStatusTest.class);

    /** test cell name. */
    private static final String TEST_CELL = "testcellauthzaccountstatus";
    /** test box name. */
    private static final String TEST_BOX = "testboxauthzaccountstatus";
    /** test account name. */
    private static final String TEST_ACCOUNT_ACTIVE = "account1";
    /** test account name. */
    private static final String TEST_ACCOUNT_SUSPENDED = "account2";
    /** test account name. */
    private static final String TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED = "account3";
    /** test account password. */
    private static final String TEST_PASSWORD = "password";

    /**
     * constructor.
     */
    public AuthzAccountStatusTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        CellUtils.create(TEST_CELL, Setup.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.createWithSchema(TEST_CELL, TEST_BOX, AbstractCase.MASTER_TOKEN_NAME,
                UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD,
                Account.STATUS_ACTIVE, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD,
                Account.STATUS_DEACTIVATED, HttpStatus.SC_CREATED);
        AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED,
                TEST_PASSWORD, Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_ACTIVE, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_SUSPENDED, -1);
        AccountUtils.delete(TEST_CELL, Setup.MASTER_TOKEN_NAME, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, -1);
        BoxUtils.delete(TEST_CELL, AbstractCase.MASTER_TOKEN_NAME, TEST_BOX, -1);
        CellUtils.delete(Setup.MASTER_TOKEN_NAME, TEST_CELL, -1);
    }

    /**
     * Test whether to authenticate by status.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePassword() throws Exception {
        // account active.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_ACTIVE, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));

        // account suspended.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_SUSPENDED, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);

        // account password change required.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0006", TEST_CELL);
    }

    /**
     * Test handle password change.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void test_handlePasswordChange() throws Exception {
        // First authorization, Get password change access token.
        PersoniumResponse res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED,
                TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        String apTokenStr = getPasswordChangeAccessTokenStr(res);
        assertNotNull(apTokenStr);

        // Password change failed, No pass.
        res = requestAuthorizationPasswordChange(TEST_CELL, apTokenStr, "");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0007", TEST_CELL);

        // Password change failed, Password invalid format.
        res = requestAuthorizationPasswordChange(TEST_CELL, apTokenStr, "error");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0008", TEST_CELL);

        // Password change failed, invalid token.
        res = requestAuthorizationPasswordChange(TEST_CELL, "dummy_token", "newpassword");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0002", TEST_CELL);

        // Password change success.
        String newPassword = "newpassword";
        res = requestAuthorizationPasswordChange(TEST_CELL, apTokenStr, newPassword);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        Map<String, String> responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        assertTrue(responseMap.containsKey(OAuth2Helper.Key.ACCESS_TOKEN));

        // Authentication can be performed with the changed password.
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, newPassword);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        responseMap = parseResponse(res);
        assertFalse(responseMap.containsKey(OAuth2Helper.Key.ERROR));
        assertTrue(responseMap.containsKey(OAuth2Helper.Key.ACCESS_TOKEN));

        // It can not be authenticated with the password before change
        res = requestAuthorization4Authz(TEST_CELL, TEST_ACCOUNT_PASSWORD_CHANGE_REQUIRED, TEST_PASSWORD);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        ImplicitFlowTest.checkHtmlBody(res, "PS-AU-0004", TEST_CELL);
    }

    /**
     * get password change access token string.
     * @param res PersoniumResponse
     * @return password change access token string
     */
    public String getPasswordChangeAccessTokenStr(PersoniumResponse res) {
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
        NodeList nodeList = document.getElementsByTagName("input");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("id");
            if ("ap_token".equals(id)) {
                return element.getAttribute("value");
            }
        }
        return null;
    }

    /**
     * request authorization.
     * @param cellName cell name
     * @param userName user name
     * @param password password
     * @return http response
     */
    private PersoniumResponse requestAuthorization4Authz(String cellName, String userName, String password)
            throws PersoniumException {
        PersoniumResponse dcRes = CellUtils.implicitflowAuthenticate(cellName, Setup.TEST_CELL_SCHEMA1, userName,
                password, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, null);
        return dcRes;
    }

    /**
     * request authorization password change.
     * @param cellName cell name
     * @param apTokenStr password change access token string
     * @param password password
     * @return http response
     */
    private PersoniumResponse requestAuthorizationPasswordChange(String cellName, String apTokenStr, String password)
            throws PersoniumException {
        PersoniumResponse dcRes = CellUtils.implicitflowAuthenticatePasswordChange(cellName, Setup.TEST_CELL_SCHEMA1,
                apTokenStr, password, "__/redirect.html", ImplicitFlowTest.DEFAULT_STATE, null);
        return dcRes;
    }

    /**
     * parse response.
     * @param res the personium response
     * @return parse response.
     */
    private Map<String, String> parseResponse(PersoniumResponse res) {
        String location = res.getFirstHeader(HttpHeaders.LOCATION);
        System.out.println(location);
        String[] locations = location.split("#");
        String[] responses = locations[1].split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String response : responses) {
            String[] value = response.split("=");
            map.put(value[0], value[1]);
        }

        return map;
    }
}

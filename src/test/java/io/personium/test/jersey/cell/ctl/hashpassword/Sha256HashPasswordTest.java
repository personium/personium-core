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
package io.personium.test.jersey.cell.ctl.hashpassword;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumUnitConfig.Security;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.hash.Sha256HashPasswordImpl;
import io.personium.core.model.lock.LockManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * test SHA-256 hash password.
 */
@Category({Unit.class, Integration.class, Regression.class})
public class Sha256HashPasswordTest extends ODataCommon {

    static String cellName = "testcell1";

    /** Default value of hashArgorithm. */
    private static String defaultHashArgorithm;

    /**
     * Befor class.
     */
    @BeforeClass
    public static void beforClass() {
        defaultHashArgorithm = PersoniumUnitConfig.getAuthPasswordHashAlgorithm();
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_HASH_ALGORITHM, Sha256HashPasswordImpl.HASH_ALGORITHM_NAME);
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(Security.AUTH_PASSWORD_HASH_ALGORITHM, defaultHashArgorithm);
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * constructor.
     */
    public Sha256HashPasswordTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * test create account.
     */
    @Test
    public final void create_account() {
        // chang password regex pattern.
        String defaultPasswordRegex = PersoniumUnitConfig.getAuthPasswordRegex();
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.AUTH_PASSWORD_REGEX,
                "(?=.*[A-Z])(?!^(?i)password(?-i)$)^.*[a-zA-Z0-9]$");

        String testAccountName = "account_hashpassword";

        ArrayList<String> normalPasswordList = new ArrayList<String>();
        normalPasswordList.add("A");
        normalPasswordList.add("ABC");
        normalPasswordList.add("ABCXYZabcxyz0123456789");
        normalPasswordList.add("12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "ABCDEF");

        try {
            for (String value : normalPasswordList) {
                createAccount(testAccountName, value, HttpStatus.SC_CREATED);
                AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
            }
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
            PersoniumUnitConfig.set(PersoniumUnitConfig.Security.AUTH_PASSWORD_REGEX, defaultPasswordRegex);
        }
    }

    /**
     * test update account password.
     */
    @Test
    public final void update_account_password() {
        String testAccountName = "account_hashpassword_update";

        String oldPassword = "old_p@ssword0";
        String newPassword = "new_p@ssword0";

        try {
            // create test account.
            createAccount(testAccountName, oldPassword, HttpStatus.SC_CREATED);
            this.auth(testAccountName, oldPassword, HttpStatus.SC_OK);

            // update password.
            this.updatePwd(testAccountName, newPassword, HttpStatus.SC_NO_CONTENT);
            this.auth(testAccountName, newPassword, HttpStatus.SC_OK);
            this.auth(testAccountName, oldPassword, HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * test change my password.
     */
    @Test
    public final void change_my_password() {
        String testAccountName = "account_hashpassword_update";

        String oldPassword = "old_p@ssword0";
        String newPassword = "new_p@ssword0";

        try {
            // create test account.
            createAccount(testAccountName, oldPassword, HttpStatus.SC_CREATED);
            JSONObject resBody = this.auth(testAccountName, oldPassword, HttpStatus.SC_OK);
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // request my password. (change my password)
            this.requestMyPassword(tokenStr, newPassword, HttpStatus.SC_NO_CONTENT);
            this.auth(testAccountName, newPassword, HttpStatus.SC_OK);
            this.auth(testAccountName, oldPassword, HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    private String createAccount(String testAccountName, String testAccountPass, int code) {
        String accLocHeader;
        TResponse res = Http.request("account-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", testAccountName)
                .with("password", testAccountPass)
                .returns()
                .debug();
        accLocHeader = res.getLocationHeader();
        res.statusCode(code);
        return accLocHeader;
    }

    /**
     * account update. (password only)
     * @param newUsername アカウント名
     * @param newPassword パスワード
     * @param sc ステータスコード
     */
    private void updatePwd(String username, String newPassword, int sc) {
        TResponse res = Http.request("account-update.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", username)
                .with("password", newPassword)
                .with("newUsername", username)
                .returns().debug();
        res.statusCode(sc);
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * request mypassword.
     * @param headerAuthorization access token.
     * @param headerCredential new password
     * @param sc status code
     * @return response
     */
    private PersoniumResponse requestMyPassword(String headerAuthorization, String headerCredential, int sc) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // set request header
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + headerAuthorization);
        requestheaders.put(CommonUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL, headerCredential);

        try {
            res = rest.put(UrlUtils.cellRoot(cellName) + "__mypassword", "", requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        assertEquals(sc, res.getStatusCode());
        return res;
    }

    /**
     * password authentication.
     * @param usr account name
     * @param pwd password
     * @param sc status code
     */
    private JSONObject auth(String usr, String pwd, int sc) {
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", usr)
                .with("password", pwd)
                .returns();
        res.statusCode(sc);
        JSONObject json = res.bodyAsJson();
        return json;
    }
}

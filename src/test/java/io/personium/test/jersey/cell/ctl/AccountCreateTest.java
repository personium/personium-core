/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.test.jersey.cell.ctl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.plugin.AuthPluginForAuthTest;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * Accountの作成のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class AccountCreateTest extends ODataCommon {

    static String cellName = "testcell1";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountCreateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Accountを作成し正常に登録できること.
     */
    @Test
    public final void Accountを作成し正常に登録できること() {
        String testAccountName = "test_account";
        String testAccountPass = "password";

        try {
            TResponse res = AccountUtils.create(MASTER_TOKEN_NAME, cellName, testAccountName,
                    testAccountPass, HttpStatus.SC_CREATED);
            String name = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results"))
                    .get("Name");
            assertEquals(testAccountName, name);

            res = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            String getName = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("Name");
            assertEquals(testAccountName, getName);

        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * Account新規登録時にNameに空文字を指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameに空文字を指定して400になること() {
        String testAccountName = "";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameにアンダーバー始まりの文字列を指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameにアンダーバー始まりの文字列を指定して400になること() {
        String testAccountName = "_test_account";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameにハイフン始まりの文字列を指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameにハイフン始まりの文字列を指定して400になること() {
        String testAccountName = "-test_account";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameにスラッシュを含む文字列を指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameにスラッシュを含む文字列を指定して400になること() {
        String testAccountName = "test/account";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameに__ctlを指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameに__ctlを指定して400になること() {
        String testAccountName = "__ctl";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameに129文字指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameに129文字指定して400になること() {
        String testAccountName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameに1文字指定して登録できること.
     */
    @Test
    public final void Account新規登録時にNameに1文字指定して登録できること() {
        String testAccountName = "1";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_CREATED);
        } finally {
            deleteAccount(accLocHeader);
        }
    }

    /**
     * Account新規登録時にNameに128文字指定して登録できること.
     */
    @Test
    public final void Account新規登録時にNameに128文字指定して登録できること() {
        String testAccountName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_CREATED);
        } finally {
            deleteAccount(accLocHeader);
        }
    }

    /**
     * Account新規登録時にNameに日本語を指定して400になること.
     */
    @Test
    public final void Account新規登録時にNameに日本語を指定して400になること() {
        String testAccountName = "日本語";
        String testAccountPass = "password";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時にNameに半角記号を指定して登録できること.
     */
    @Test
    public final void Account新規登録時にNameに半角記号を指定して登録できること() {
        // Before escaping is "abcde12345-_!$*=^`{|}~.@"
        String testAccountName = "abcde12345-_!\\$*=^`{|}~.@";
        String encodedtestAccountName = "abcde12345-_%21%24%2A%3D%5E%60%7B%7C%7D%7E.%40";

        String testAccountPass = "password";

        try {
            createAccount(testAccountName, testAccountPass, HttpStatus.SC_CREATED);
            AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, encodedtestAccountName);
            AccountUtils.update(MASTER_TOKEN_NAME, cellName,
                    encodedtestAccountName, testAccountName, "password2", HttpStatus.SC_NO_CONTENT);
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, encodedtestAccountName, -1);
        }
    }

    /**
     * Accounts can be registered with accountType specified in loaded plugin and without password.
     * (In test cases, AuthPluginForAuthTest is loaded by default.)
     */
    @Test
    public final void Accounts_can_be_registered_with_accountType_and_no_password() {
        String testAccountName = "personium.io@gmail.com";
        String testAccountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String accLocHeader = null;

        try {
            accLocHeader = createNoPassAccount(testAccountName, testAccountType, HttpStatus.SC_CREATED);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Accounts can be registered with accountType specified in loaded plugin and with password.
     * (In test cases, AuthPluginForAuthTest is loaded by default.)
     */
    @Test
    public final void Accounts_can_be_registered_with_accountType_and_password() {
        String testAccountName = "personium.io@gmail.com";
        String testAccountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String testAccountPass = "password1234";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, testAccountType, HttpStatus.SC_CREATED);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Accounts can be registered with accountType `basic` and one specified in loaded plugin and without password.
     * (In test cases, AuthPluginForAuthTest is loaded by default.)
     */
    @Test
    public final void Accouns_can_be_registered_with_basic_and_acountType_specified_in_plugin_without_password() {
        String testAccountName = "personium.io@gmail.com";
        String testAccountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String accLocHeader = null;

        try {
            accLocHeader = createNoPassAccount(testAccountName, testAccountType, HttpStatus.SC_CREATED);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Accounts can be registered with accountType `basic` and one specified in loaded plugin and with password.
     * (In test cases, AuthPluginForAuthTest is loaded by default.)
     */
    @Test
    public final void Accouns_can_be_registered_with_basic_and_acountType_specified_in_plugin_with_password() {
        String testAccountName = "personium.io@gmail.com";
        String testAccountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String testAccountPass = "password1234";
        String accLocHeader = null;

        try {
            accLocHeader = createAccount(testAccountName, testAccountPass, testAccountType, HttpStatus.SC_CREATED);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Account新規登録時に不正なType文字列を指定して400になること.
     */
    @Test
    public final void Account新規登録時に不正なType文字列を指定して400になること() {
        ArrayList<String> invalidTypeStrings = new ArrayList<String>();
        invalidTypeStrings.add("Type=");
        invalidTypeStrings.add("");
        invalidTypeStrings.add("!aa");
        invalidTypeStrings.add("basic  " + AuthPluginForAuthTest.ACCOUNT_TYPE);
        invalidTypeStrings.add("%E3%81%82");
        invalidTypeStrings.add("あ");
        invalidTypeStrings.add("       ");

        String testAccountName = "account_normalpassword";
        String testAccountPass = "password1234";
        String accLocHeader = null;

        try {
            for (String value : invalidTypeStrings) {
                accLocHeader = createAccount(testAccountName, testAccountPass, value, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * If you specify a normal password string at the time of new account registration, the account will be registered.
     * (Password regex pattern is default.)
     */
    @Test
    public final void password_normal() {
        String testAccountName = "account_password";

        ArrayList<String> normalPasswordList = new ArrayList<String>();
        normalPasswordList.add("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        normalPasswordList.add("abcdefghijklmnopqrstuvwxyz");
        normalPasswordList.add("123456");
        normalPasswordList.add("12345678901234567890123456789012");
        normalPasswordList.add("-_!\\$*=^`{|}~.@"); // Before escaping is "-_!$*=^`{|}~.@"

        try {
            for (String password : normalPasswordList) {
                createAccount(testAccountName, password, HttpStatus.SC_CREATED);
                AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
            }
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * If you specify an invalid password string at the time of new account registration, the response will be 400.
     * (Password regex pattern is default.)
     */
    @Test
    public final void password_invalid() {
        ArrayList<String> invalidPasswordList = new ArrayList<String>();
        invalidPasswordList.add("");
        invalidPasswordList.add("pass%word");
        invalidPasswordList.add("%E3%81%82");
        invalidPasswordList.add("あ");
        invalidPasswordList.add("123456789012345678901234567890123");

        String testAccountName = "account_badpassword";

        try {
            for (String invalidPassword : invalidPasswordList) {
                createAccount(testAccountName, invalidPassword, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * Test when change password regex pattern.
     */
    @Test
    public final void change_password_regex_pattern() {
        String defaultPasswordRegex = PersoniumUnitConfig.getAuthPasswordRegex();

        // chang password regex pattern.
        PersoniumUnitConfig.set(PersoniumUnitConfig.Security.AUTH_PASSWORD_REGEX,
                "(?=.*[A-Z])(?!^(?i)password(?-i)$)^.*[a-zA-Z0-9]$");

        String testAccountName = "account_testpasswordregex";

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

        ArrayList<String> invalidPasswordList = new ArrayList<String>();
        invalidPasswordList.add("");
        invalidPasswordList.add("TEST=");
        invalidPasswordList.add("abcxyz09");
        invalidPasswordList.add("PaSsWoRd");
        invalidPasswordList.add("12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "ABCDEFG");

        try {
            for (String value : normalPasswordList) {
                createAccount(testAccountName, value, HttpStatus.SC_CREATED);
                AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
            }
            for (String value : invalidPasswordList) {
                createAccount(testAccountName, value, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
            PersoniumUnitConfig.set(PersoniumUnitConfig.Security.AUTH_PASSWORD_REGEX, defaultPasswordRegex);
        }
    }

    /**
     * Test that designates a single IP address as "IPAddressRange" in account creation.
     */
    @Test
    public final void Account_create_set_IP_address() {
        String testAccountName = "account.create.test.IPAddress1";
        String testAccountPass = "password1234";
        String testAccountIPAddressRange = "127.93.0.234";
        String accLocHeader = null;

        try {
            TResponse response = AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    testAccountName, testAccountPass, testAccountIPAddressRange, HttpStatus.SC_CREATED);
            accLocHeader = response.getLocationHeader();

            String ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);

            response = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);


        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Test that designates a single IP address range as "IPAddressRange" in account creation.
     */
    @Test
    public final void Account_create_set_IP_address_range() {
        String testAccountName = "account.create.test.IPAddress2";
        String testAccountPass = "password1234";
        String testAccountIPAddressRange = "127.93.0.234/28";

        try {
            TResponse response = AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    testAccountName, testAccountPass, testAccountIPAddressRange, HttpStatus.SC_CREATED);

            String ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);

            response = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * Test that designates a mulutiple IP address range as "IPAddressRange" in account creation.
     */
    @Test
    public final void Account_create_set_IP_address_mulutiple() {
        String testAccountName = "account.create.test.IPAddress3";
        String testAccountPass = "password1234";
        String testAccountIPAddressRange = "0.0.0.0,127.93.0.234/1,127.93.0.235,127.93.0.236/32,255.255.255.255";

        try {
            TResponse response = AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    testAccountName, testAccountPass, testAccountIPAddressRange, HttpStatus.SC_CREATED);

            String ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);

            response = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            ipAddressRange = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("IPAddressRange");
            assertEquals(testAccountIPAddressRange, ipAddressRange);
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * Test that designates a illegal IP address as "IPAddressRange" in account creation.
     */
    @Test
    public final void Account_create_set_IP_address_illegal() {
        String testAccountName = "account.create.test.IPAddress4";
        String testAccountPass = "password1234";

        ArrayList<String> invalidIPAddressList = new ArrayList<String>();
        invalidIPAddressList.add("strings");
        invalidIPAddressList.add("1.2.3");
        invalidIPAddressList.add("1.2.3.4.5");
        invalidIPAddressList.add("-1.0.0.0");
        invalidIPAddressList.add("256.0.0.0");
        invalidIPAddressList.add("0.0.0.0/0");
        invalidIPAddressList.add("0.0.0.0/33");
        invalidIPAddressList.add("0.0.0.0/32/32");
        invalidIPAddressList.add("127.93.0.1,strings,127.93.0.3");

        try {
            for (String invalidIPAddress : invalidIPAddressList) {
                AccountUtils.createWithIPAddressRange(AbstractCase.MASTER_TOKEN_NAME, cellName,
                        testAccountName, testAccountPass, invalidIPAddress, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            AccountUtils.delete(cellName, MASTER_TOKEN_NAME, testAccountName, -1);
        }
    }

    /**
     * Test that default status in account creation.
     */
    @Test
    public final void Account_create_default_status() {
        String testAccountName = "account.create.test.status1";
        String testAccountPass = "password1234";
        String accLocHeader = null;

        try {
            TResponse response = AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    testAccountName, testAccountPass, HttpStatus.SC_CREATED);
            accLocHeader = response.getLocationHeader();

            String status = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("Status");
            assertEquals(Account.STATUS_ACTIVE, status);

            response = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            status = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("Status");
            assertEquals(Account.STATUS_ACTIVE, status);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Test that designates a status in account creation.
     */
    @Test
    public final void Account_create_set_status() {
        String testAccountName = "account.create.test.status2";
        String testAccountPass = "password1234";
        String testAccountStatus = Account.STATUS_DEACTIVATED;
        String accLocHeader = null;

        try {
            TResponse response = AccountUtils.createWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    testAccountName, testAccountPass, testAccountStatus, HttpStatus.SC_CREATED);
            accLocHeader = response.getLocationHeader();

            String getStatus = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("Status");
            assertEquals(testAccountStatus, getStatus);

            response = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            getStatus = (String) ((JSONObject) ((JSONObject) response.bodyAsJson().get("d"))
                    .get("results")).get("Status");
            assertEquals(testAccountStatus, getStatus);
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
        }
    }

    /**
     * Test that designates a illegal status in account creation.
     */
    @Test
    public final void Account_create_set_status_illegal() {
        String testAccountName = "account.create.test.status3";
        String testAccountPass = "password1234";

        ArrayList<String> invalidStatusList = new ArrayList<String>();
        invalidStatusList.add("illegal");

        try {
            for (String invalidStatus : invalidStatusList) {
                AccountUtils.createWithStatus(AbstractCase.MASTER_TOKEN_NAME, cellName,
                        testAccountName, testAccountPass, invalidStatus, HttpStatus.SC_BAD_REQUEST);
            }
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
     *  @Overload
     * Typeを指定してAccountを登録する場合
     */
    private String createAccount(String testAccountName, String testAccountPass, String testAccountType, int code) {
        String accLocHeader;
        TResponse res = Http.request("account-create-with-type.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", testAccountName)
                .with("password", testAccountPass)
                .with("accountType", testAccountType)
                .returns()
                .debug();
        accLocHeader = res.getLocationHeader();
        res.statusCode(code);
        return accLocHeader;
    }

    /**
     *  Typeを指定してAccountを登録、かつ、パスワードを登録しない場合.
     */
    private String createNoPassAccount(String testAccountName, String testAccountType, int code) {
        String accLocHeader;
        TResponse res = Http.request("account-create-Non-Credential-with-type.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", testAccountName)
                .with("accountType", testAccountType)
                .returns()
                .debug();
        accLocHeader = res.getLocationHeader();
        res.statusCode(code);
        return accLocHeader;
    }

    private void deleteAccount(String accountUrl) {
        PersoniumRequest req = PersoniumRequest.delete(accountUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        request(req);
    }
}

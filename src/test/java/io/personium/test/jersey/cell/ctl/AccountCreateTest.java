/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
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
            String lastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results"))
                    .get("LastAuthenticated");
            assertEquals(null, lastAuthenticated);

            res = AccountUtils.get(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, testAccountName);
            String getLastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                    .get("results"))
                    .get("LastAuthenticated");
            assertEquals(lastAuthenticated, getLastAuthenticated);

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
        // エスケープする前のNameは、abcde12345-_!$*=^`{|}~.@
        String testAccountName = "abcde12345\\-\\_\\!\\$\\*\\=\\^\\`\\{\\|\\}\\~.\\@";
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
     * Account新規登録時にPasswordなしでTypeに"oidc:google"を指定して登録できること.
     */
    @Test
    public final void Account新規登録時にPasswordなしでTypeにoidcコロンgoogleを指定して登録できること() {
        String testAccountName = "personium.io\\@gmail.com";
        String testAccountType = "oidc:google";
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
     * Account新規登録時にPasswordありでTypeに"oidc:google"を指定して登録できること.
     */
    @Test
    public final void Account新規登録時にPasswordありでTypeにoidcコロンgoogleを指定して登録できること() {
        String testAccountName = "personium.io\\@gmail.com";
        String testAccountType = "oidc:google";
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
     * Account新規登録時にパスワードなしでTypeに"basic oidc:google"を指定して登録できること.
     */
    @Test
    public final void Account新規登録時にパスワードなしでTypeにbasicスペースoidcコロンgoogleを指定して登録できること() {
        String testAccountName = "personium.io\\@gmail.com";
        String testAccountType = "basic oidc:google";
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
     * Account新規登録時にパスワードありでTypeに"basic oidc:google"を指定して登録できること.
     */
    @Test
    public final void Account新規登録時にパスワードありでTypeにbasicスペースoidcコロンgoogleを指定して登録できること() {
        String testAccountName = "personium.io\\@gmail.com";
        String testAccountType = "basic oidc:google";
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
        invalidTypeStrings.add("basic  oidc:google");
        invalidTypeStrings.add("%E3%81%82");
        invalidTypeStrings.add("あ");
        invalidTypeStrings.add("       ");

        String testAccountName = "account_badpassword";
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
     * Account新規登録時に不正なパスワード文字列を指定して400になること.
     */
    @Test
    public final void Account新規登録時に不正なパスワード文字列を指定して400になること() {
        ArrayList<String> invalidStrings = new ArrayList<String>();
        invalidStrings.add("password=");
        invalidStrings.add("");
        invalidStrings.add("!aa");
        invalidStrings.add("pass%word");
        invalidStrings.add("%E3%81%82");
        invalidStrings.add("あ");
        invalidStrings.add("123456789012345678901234567890123");

        String testAccountName = "account_badpassword";
        String accLocHeader = null;

        try {
            for (String value : invalidStrings) {
                accLocHeader = createAccount(testAccountName, value, HttpStatus.SC_BAD_REQUEST);
            }
        } finally {
            if (accLocHeader != null) {
                deleteAccount(accLocHeader);
            }
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

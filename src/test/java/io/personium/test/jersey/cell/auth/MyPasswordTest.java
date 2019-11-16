/**
 * Personium
 * Copyright 2014-2019 FUJITSU LIMITED
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Account;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;
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
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Test for Password change API.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class MyPasswordTest extends PersoniumTest {

    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String UNIT_USER_CELL = "unitusercell";

    private String cellUrl;


    /**
     * Constructor.
     */
    public MyPasswordTest() {
        super(new PersoniumCoreApplication());
    }
    @Before
    public void before() {
        String usrCellLocalUnit = UriUtils.SCHEME_LOCALUNIT + ":" + Setup.TEST_CELL1 + ":/";
        this.cellUrl = UriUtils.resolveLocalUnit(usrCellLocalUnit);
    }

    /**
     * When accessed with residential Access Token with sufficient scope, then return 204.
     * @throws TokenParseException
     */
    @Test
    public final void When_ResidentialAccessTokenWithSufficientScope_Then_Return_204() throws TokenParseException {
        String accountName = "PasswordTest";
        String accountPw = "password";

        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, accountName, accountPw, 201);
            // 認証
            HttpResponse httpRes = this.httpReqROPC(this.cellUrl, accountName, accountPw, null, null, null, null);
            JSONObject resBody = (JSONObject) (new JSONParser()).parse(new InputStreamReader(httpRes.getEntity().getContent(), Charsets.UTF_8));

            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse res = requesttoMypassword(tokenStr, "newPassword", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());

            // 確認
            // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
            // 1.変更前のパスワードのセルローカルトークンを使用して403となること
            //            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            //            assertEquals(403, res.getStatusCode());

            // 2.変更後のパスワードのセルローカルトークンでアカウントの取得を実行して200となること
            // 認証
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    accountName, "newPassword", -1);
            // セルローカルトークンを取得する
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, accountName, 204);
        }
    }

    /**
     * When Access Token_Has Insufficient Scope Then Return_403.
     * @throws TokenParseException
     */
    @Test
    public final void When_ResidentialAccessToken_HasInsufficientScope_Then_Return_403() throws TokenParseException {
        String accountName = "PasswordTest";
        String accountPw = "password";

        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, accountName, accountPw, 201);

            // 認証
            HttpResponse httpRes = this.httpReqROPC(this.cellUrl, accountName, accountPw, null, "messsage", null, null);
            JSONObject resBody = (JSONObject) (new JSONParser()).parse(new InputStreamReader(httpRes.getEntity().getContent(), Charsets.UTF_8));
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            // 確認

            PersoniumResponse res = requesttoMypassword(tokenStr, "newPassword", Setup.TEST_CELL1);
            assertEquals(403, res.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, accountName, 204);
        }
    }


    /**
     * Test that my password change token authentication can be change the password.
     * @throws TokenParseException token parse exception.
     */
    @Test
    public final void test_my_password_change_token() throws TokenParseException {
        String account = "TestPasswordChangeToken1";
        try {
            // Create test account.
            AccountUtils.createWithStatus(Setup.MASTER_TOKEN_NAME, Setup.TEST_CELL1, account, account,
                    Account.STATUS_PASSWORD_CHANGE_REQUIRED, HttpStatus.SC_CREATED);
            // Authenticate
//            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
//                    account, account, -1);
            HttpResponse httpRes = this.httpReqROPC(this.cellUrl, account, account, null, null, null, null);
            JSONObject resBody = (JSONObject) (new JSONParser()).parse(new InputStreamReader(httpRes.getEntity().getContent(), Charsets.UTF_8));
            System.out.println(resBody.toJSONString());

            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            String scope = (String) resBody.get(OAuth2Helper.Key.SCOPE);

            assertTrue(tokenStr.startsWith(PasswordChangeAccessToken.PREFIX_ACCESS));

            // Change my password.
            PersoniumResponse res = requesttoMypassword(tokenStr, "newPassword", Setup.TEST_CELL1);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Authenticate again.
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1, account, "newPassword", -1);
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            assertTrue(tokenStr.startsWith(ResidentLocalAccessToken.PREFIX_ACCESS));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, account, -1);
        }
    }

    /**
     * 記号を含むアカウントの自分セルローカルトークン認証でパスワード変更を実行し204が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 記号を含むアカウントの自分セルローカルトークン認証でパスワード変更を実行し204が返ること() throws TokenParseException {
        // エスケープする前のNameは、abcde12345-_!$*=^`{|}~.@
        String testAccountName = "abcde12345-_!\\$*=^`{|}~.@";
        String encodedtestAccountName = "abcde12345-_%21%24%2A%3D%5E%60%7B%7C%7D%7E.%40";

        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, testAccountName, "password", 201);
            // 認証
            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    testAccountName, "password", HttpStatus.SC_OK);
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse res = requesttoMypassword(tokenStr, "newPassword", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());

            // 確認
            // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
            // 1.変更前のパスワードのセルローカルトークンを使用して403となること
            // res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            // assertEquals(403, res.getStatusCode());

            // 2.変更後のパスワードのセルローカルトークンでアカウントの取得を実行して200となること
            // 認証
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    testAccountName, "newPassword", HttpStatus.SC_OK);
            // セルローカルトークンを取得する
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, encodedtestAccountName, 204);
        }
    }

    /**
     * 他人セルローカルトークン認証でパスワード変更を実行し403が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 他人セルローカルトークン認証でパスワード変更を実行し403が返ること() throws TokenParseException {
        // 他人セルローカルトークン取得
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("p_target", UrlUtils.cellRoot(Setup.TEST_CELL2))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        PersoniumResponse response = requesttoMypassword(transCellAccessToken, "newPassword", Setup.TEST_CELL1);
        assertEquals(403, response.getStatusCode());
    }

    /**
     * ユニットローカルユニットユーザトークン認証でパスワード変更を実行し403が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    @Ignore // UUT promotion setting API invalidation.
    public final void ユニットローカルユニットユーザトークン認証でパスワード変更を実行し403が返ること() throws TokenParseException {
        // 認証
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, MASTER_TOKEN,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // パスワード認証でのユニット昇格
        TResponse tokenResponse = Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = tokenResponse.bodyAsJson();
        String uluut = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // ユニットローカルユニットユーザトークンを取得する
        PersoniumResponse res = requesttoMypassword(uluut, "password3", Setup.TEST_CELL1);
        assertEquals(403, res.getStatusCode());
    }

    /**
     * ユニットユーザトークン認証でパスワード変更を実行し403が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void ユニットユーザトークン認証でパスワード変更を実行し403が返ること() throws TokenParseException {
        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    "Account", "AccountPass", HttpStatus.SC_CREATED);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", "Account")
                    .with("password", "AccountPass")
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse response = requesttoMypassword(unitUserToken, "newPassword", Setup.TEST_CELL1);
            assertEquals(403, response.getStatusCode());
        } finally {
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    "Account", -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }

    /**
     * マスタトークン認証でパスワード変更を実行し403が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void マスタトークン認証でパスワード変更を実行し403が返ること() throws TokenParseException {
        PersoniumResponse res = requesttoMypassword(MASTER_TOKEN, "password3", Setup.TEST_CELL1);
        assertEquals(403, res.getStatusCode());
    }

    /**
     * 不正なトークンでパスワード変更を実行し401が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 不正なトークンでパスワード変更を実行し401が返却されること() throws TokenParseException {
        PersoniumResponse res = requesttoMypassword("passwordhoge", "accountpass", Setup.TEST_CELL1);
        assertEquals(401, res.getStatusCode());
    }

    /**
     * If the new password is a normal password string, the response will be 204.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void new_password_normal() throws Exception {
        ArrayList<String> normalPasswordList = new ArrayList<String>();
        normalPasswordList.add("hoge@!");
        normalPasswordList.add("12345678901234567890123456789012");

        for (String password : normalPasswordList) {
            try {
                // create account and get cell local token.
                AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, "PasswordTest", "password", 201);
                JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                        "PasswordTest", "password", -1);
                String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

                // change password.
                PersoniumResponse res = requesttoMypassword(tokenStr, password, Setup.TEST_CELL1);
                assertEquals(204, res.getStatusCode());

                // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
                // 1. Cell local token of password before change can't be used.
                // res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
                // assertEquals(403, res.getStatusCode());

                // 2. The cell local token of the changed password can be used
                resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                        "PasswordTest", password, -1);
                tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
                res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
                assertEquals(204, res.getStatusCode());
            } finally {
                AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, "PasswordTest", 204);
            }
        }
    }

    /**
     * If the new password is an invalid password string, the response will be 400.
     * @throws Exception Unexpected exception
     */
    @Test
    public final void new_password_invalid() throws Exception {
        ArrayList<String> invalidPasswordList = new ArrayList<String>();
        invalidPasswordList.add("hogea");
        invalidPasswordList.add("123456789012345678901234567890123");

        for (String password : invalidPasswordList) {
            try {
                // create test account and get cell local token.
                AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, "PasswordTest", "password", 201);
                JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                        "PasswordTest", "password", -1);
                String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

                // Failed to change password.
                PersoniumResponse res = requesttoMypassword(tokenStr, password, Setup.TEST_CELL1);
                assertEquals(400, res.getStatusCode());
            } finally {
                AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, "PasswordTest", 204);
            }
        }
    }

    /**
     * 他セルの同名アカウントのパスワードでパスワード変更を実行し401が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 他セルの同名アカウントのパスワードでパスワード変更を実行し401が返却されること() throws TokenParseException {
        String cellName1 = "passcell1";
        String cellName2 = "passcell2";
        try {
            // Cell作成
            CellUtils.create(cellName1, MASTER_TOKEN, 201);
            CellUtils.create(cellName2, MASTER_TOKEN, 201);
            // Account作成
            AccountUtils.create(MASTER_TOKEN, cellName1, "PasswordTest", "password", 201);
            AccountUtils.create(MASTER_TOKEN, cellName2, "PasswordTest", "password", 201);
            // 認証
            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(cellName2,
                    "PasswordTest", "password", -1);
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse res = requesttoMypassword(tokenStr, "password1", cellName1);
            assertEquals(401, res.getStatusCode());
        } finally {
            AccountUtils.delete(cellName1, MASTER_TOKEN, "PasswordTest", -1);
            AccountUtils.delete(cellName2, MASTER_TOKEN, "PasswordTest", -1);
            CellUtils.delete(MASTER_TOKEN, cellName1);
            CellUtils.delete(MASTER_TOKEN, cellName2);
        }
    }

    /**
     * Credentialヘッダのキーが空でパスワード変更を実行し400が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void Credentialヘッダのキーが空でパスワード変更を実行し400が返却されること() throws TokenParseException {
        // 認証
        JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                "account10", "password10", -1);
        // セルローカルトークンを取得する
        String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

        PersoniumResponse res = requesttoMypassword(tokenStr, null, Setup.TEST_CELL1);
        assertEquals(400, res.getStatusCode());
    }

    /**
     * Credentialヘッダの指定なしでパスワード変更を実行し400が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void Credentialヘッダの指定なしでパスワード変更を実行し400が返却されること() throws TokenParseException {
        // 認証
        JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                "account10", "password10", -1);
        // セルローカルトークンを取得する
        String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + tokenStr);

        try {
            res = rest.put(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__mypassword", "",
                    requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        assertEquals(400, res.getStatusCode());
    }

    /**
     * __mypasswordへのリクエストする.
     * @param headerAuthorization トークン
     * @param headerCredential 変更後のパスワード
     * @param requestCellName cell名
     * @return レスポンス
     */
    private PersoniumResponse requesttoMypassword(String headerAuthorization, String headerCredential,
            String requestCellName) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        if (headerAuthorization == null) {
            headerAuthorization = "";
        }
        if (headerCredential == null) {
            headerCredential = "";
        }

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + headerAuthorization);
        requestheaders.put(CommonUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL, headerCredential);

        try {
            res = rest.put(UrlUtils.cellRoot(requestCellName) + "__mypassword", "",
                    requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        return res;
    }

    private HttpResponse httpReqROPC(String cellUrl, String username, String password, String pTarget, String scope,
            String clientId, String clientSecret) throws ClientProtocolException, IOException {
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_DEFAULT);

        String tokenEndpoint = cellUrl + "__token";
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
        if (scope != null) {
            sb.append("&scope=");
            sb.append(scope);
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

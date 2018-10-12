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
package io.personium.test.jersey.cell.auth;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.rs.PersoniumCoreApplication;
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
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;

/**
 * パスワード変更APIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MyPasswordTest extends PersoniumTest {

    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String UNIT_USER_CELL = "unitusercell";

    /**
     * コンストラクタ.
     */
    public MyPasswordTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * 自分セルローカルトークン認証でパスワード変更を実行し204が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 自分セルローカルトークン認証でパスワード変更を実行し204が返ること() throws TokenParseException {
        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, "PasswordTest", "password", 201);
            // 認証
            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "password", -1);
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "PasswordTest");
            PersoniumResponse res = requesttoMypassword(tokenStr, "newPassword", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
            AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(
                    Setup.TEST_CELL1, "PasswordTest", lastAuthenticatedTime);

            // 確認
            // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
            // 1.変更前のパスワードのセルローカルトークンを使用して403となること
//            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
//            assertEquals(403, res.getStatusCode());

            // 2.変更後のパスワードのセルローカルトークンでアカウントの取得を実行して200となること
            // 認証
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "newPassword", -1);
            // セルローカルトークンを取得する
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
        } finally {
             AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, "PasswordTest", 204);
        }
    }

    /**
     * 記号を含むアカウントの自分セルローカルトークン認証でパスワード変更を実行し204が返ること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 記号を含むアカウントの自分セルローカルトークン認証でパスワード変更を実行し204が返ること() throws TokenParseException {
        // エスケープする前のNameは、abcde12345-_!$*=^`{|}~.@
        String testAccountName = "abcde12345\\-\\_\\!\\$\\*\\=\\^\\`\\{\\|\\}\\~.\\@";
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
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
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
     * 新しいパスワードが5文字の時にパスワード変更を実行し400が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 新しいパスワードが5文字の時にパスワード変更を実行し400が返却されること() throws TokenParseException {
        // 認証
        JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                "account10", "password10", -1);
        // セルローカルトークンを取得する
        String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

        PersoniumResponse res = requesttoMypassword(tokenStr, "hogea", Setup.TEST_CELL1);
        assertEquals(400, res.getStatusCode());
    }

    /**
     * 新しいパスワードが6文字の時にパスワード変更を実行し204が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 新しいパスワードが6文字の時にパスワード変更を実行し204が返却されること() throws TokenParseException {
        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, "PasswordTest", "password", 201);
            // 認証
            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "password", -1);
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse res = requesttoMypassword(tokenStr, "hogeaa", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
            // 確認
            // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
            // 1.変更前のパスワードのセルローカルトークンを使用して403となること
            // res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            // assertEquals(403, res.getStatusCode());

            // 2.変更後のパスワードのセルローカルトークンでアカウントの取得を実行して200となること
            // 認証
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "hogeaa", -1);
            // セルローカルトークンを取得する
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, "PasswordTest", 204);
        }
    }

    /**
     * 新しいパスワードが32文字の時にパスワード変更を実行し204が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 新しいパスワードが32文字の時にパスワード変更を実行し204が返却されること() throws TokenParseException {
        try {
            // Account作成
            AccountUtils.create(MASTER_TOKEN, Setup.TEST_CELL1, "PasswordTest", "password", 201);
            // 認証
            JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "password", -1);
            // セルローカルトークンを取得する
            String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

            PersoniumResponse res = requesttoMypassword(tokenStr, "12345678901234567890123456789012", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
            // 変更前のパスワードのセルローカルトークンを使用して403となること
            // 確認
            // TODO 仕様の問題で変更前のトークンが有効となっているため確認未実施
            // 1.変更前のパスワードのセルローカルトークンを使用して403となること
            // res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            // assertEquals(403, res.getStatusCode());

            // 2.変更後のパスワードのセルローカルトークンでアカウントの取得を実行して200となること
            // 認証
            resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                    "PasswordTest", "12345678901234567890123456789012", -1);
            // セルローカルトークンを取得する
            tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);
            res = requesttoMypassword(tokenStr, "newPassword1", Setup.TEST_CELL1);
            assertEquals(204, res.getStatusCode());
        } finally {
            AccountUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN, "PasswordTest", 204);
        }
    }

    /**
     * 新しいパスワードが33文字の時にパスワード変更を実行し400が返却されること.
     * @throws TokenParseException 認証用トークンのパースエラー
     */
    @Test
    public final void 新しいパスワードが33文字の時にパスワード変更を実行し400が返却されること() throws TokenParseException {
        // 認証
        JSONObject resBody = ResourceUtils.getLocalTokenByPassAuth(Setup.TEST_CELL1,
                "account10", "password10", -1);
        // セルローカルトークンを取得する
        String tokenStr = (String) resBody.get(OAuth2Helper.Key.ACCESS_TOKEN);

        PersoniumResponse res = requesttoMypassword(tokenStr, "123456789012345678901234567890123", Setup.TEST_CELL1);
        assertEquals(400, res.getStatusCode());
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
            AccountUtils.delete(cellName1, MASTER_TOKEN, "PasswordTest", 204);
            AccountUtils.delete(cellName2, MASTER_TOKEN, "PasswordTest", 204);
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
        requestheaders.put(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL, headerCredential);

        try {
            res = rest.put(UrlUtils.cellRoot(requestCellName) + "__mypassword", "",
                    requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        return res;
    }
}

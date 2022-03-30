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
package io.personium.test.jersey.cell.auth;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Error;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.plugin.AuthPluginForAuthTest;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * 認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthErrorTest extends PersoniumTest {

    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_APP_CELL1 = "schema1";

    /**
     * コンストラクタ.
     */
    public AuthErrorTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Testing for returning 400 when unsupported grant_type is passed.
     */
    @Test
    public final void Returns400_WhenUnsupportedGrantTypesIsPassed() {
        String unsupportedGrantType = "unsupported_grant_type_test";

        TResponse res = Http.request("authn/auth.txt")
                .with("remoteCell", TEST_CELL1)
                .with("body", "grant_type=" + unsupportedGrantType)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(res);
        String code = PersoniumCoreAuthnException
            .UNSUPPORTED_GRANT_TYPE
            .getCode();
        String message = PersoniumCoreAuthnException
            .UNSUPPORTED_GRANT_TYPE
            .params(unsupportedGrantType)
            .getMessage();

        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(res, Error.UNSUPPORTED_GRANT_TYPE, errDesc);
    }

    /**
     * パスワード認証で不正なパスワードを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void パスワード認証で不正なパスワードを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること()
            throws InterruptedException {
        String accountName = "invalidPassAccount";
        try {
            // テスト用のアカウントを作成
            // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。このため、このテスト独自のAccountを作成する
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, accountName, "password1",
                    HttpStatus.SC_CREATED);
            Http.request("authn/password-cl-c0.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", accountName).with("password", "password2")
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * Typeがoidc系のみのアカウントに対しパスワード認証をしようとするとエラーが返ること.
     */
    @Test
    public final void Typeがoidc系のみのアカウントに対しパスワード認証をしようとするとエラーが返ること() {
        String accountName = "NonBasicTypeAccount";
        String type = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String pass = "dammypasswd";
        try {
            AccountUtils.createWithType(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, type, accountName,
                 pass, HttpStatus.SC_CREATED);

            Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", accountName)
                .with("password", pass)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * ロールが払い出されない関係のセルに対してトランスセルトークンを送信. ロールの検索処理で1件も無いと落ちてしまう問題の確認（異常系）.
     */
    @Test
    public final void ロールが払い出されない関係のセルに対してトランスセルトークンを送信() {
        // セルに対してパスワード認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_CELL1)
                .with("assertion", transCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * ExtCellが設定されていないセルにトークン認証をすると落ちる問題の修正(). ロールの検索処理でExtCellが1件も無いと落ちてしまう問題の確認（異常系）.
     */
    @Test
    public final void ExtCellが設定されていないセルにトークン認証をすると落ちる問題の修正() {
        // セルに対してパスワード認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_APP_CELL1)
                .with("assertion", transCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * Authorizationヘッダにリフレッシュトークンを指定すると500エラーとなる問題の修正確認().
     */
    @Test
    public final void Authorizationヘッダにリフレッシュトークンを指定すると500エラーとなる問題の修正確認() {
        // セルに対してパスワード認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        TResponse res2 =
                Http.request("authn/saml-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("assertion", transCellAccessToken)
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String transCellRefreshToken = (String) json2.get(OAuth2Helper.Key.REFRESH_TOKEN);

        // リフレッシュトークンでデータアクセス(認証エラー401になるはず)
        UserDataUtils.get(transCellRefreshToken, HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * パスワード認証APIに認証ヘッダを付与_400が返却されること.
     */
    @Test
    public final void パスワード認証APIに認証ヘッダを付与_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes = Http.request("authn/password-cl-with-bearerheader.txt")
                .with("remoteCell", TEST_CELL2)
                .with("Authorization_token", "bearerHeader")
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(passRes);
        String code = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getCode();
        String message = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(passRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * トークン認証APIに認証ヘッダを付与_400が返却されること.
     */
    @Test
    public final void トークン認証APIに認証ヘッダを付与_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = passRes.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        TResponse tokenRes =
                Http.request("authn/saml-cl-with-bearerheader.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("Authorization_token", "bearerHeader")
                        .with("assertion", transCellAccessToken)
                        .returns()
                        .debug()
                        .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getCode();
        String message = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * リフレッシュトークンAPIに認証ヘッダを付与_400が返却されること.
     */
    @Test
    public final void リフレッシュトークンAPIに認証ヘッダを付与_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-cl-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = passRes.bodyAsJson();
        String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        // リフレッシュトークン認証
        TResponse tokenRes = Http.request("authn/refresh-cl-with-bearerheader.txt")
                .with("remoteCell", TEST_CELL1)
                .with("Authorization_token", "invalidBearerHeader")
                .with("refresh_token", refreshToken)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getCode();
        String message = PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * パスワード認証APIのボディにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void パスワード認証APIのボディにclient_secretの指定がない場合_400が返却されること() {
        String clientId = UrlUtils.cellRoot(TEST_APP_CELL1);

        // セルに対してパスワード認証
        TResponse passRes = Http.request("authn/auth.txt")
                .with("remoteCell", TEST_CELL1)
                .with("body", "grant_type=password&username=account1&password=password1&client_id=" + clientId)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(passRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(passRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * パスワード認証APIのヘッダにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void パスワード認証APIのヘッダにclient_secretの指定がない場合_400が返却されること() {
        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1), "");

        // セルに対してパスワード認証
        TResponse passRes = Http.request("authn/auth-with-header.txt")
                .with("remoteCell", TEST_CELL1)
                .with("Authorization_header", schemaTransCellAccessTokenHeader)
                .with("body", "grant_type=password&username=account1&password=password1")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(passRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(passRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * トークン認証APIのボディにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void トークン認証APIのボディにclient_secretの指定がない場合_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = passRes.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        String clientId = UrlUtils.cellRoot(TEST_APP_CELL1);

        // セルに対してトークン認証
        TResponse tokenRes =
                Http.request("authn/auth.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("body", "grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer&assertion="
                                + transCellAccessToken + "&client_id=" + clientId)
                        .returns()
                        .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * トークン認証APIのヘッダにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void トークン認証APIのヘッダにclient_secretの指定がない場合_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL2)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = passRes.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1), "");

        // セルに対してトークン認証
        TResponse tokenRes =
                Http.request("authn/auth-with-header.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("Authorization_header", schemaTransCellAccessTokenHeader)
                        .with("body", "grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer&assertion="
                                + transCellAccessToken)
                        .returns()
                        .statusCode(HttpStatus.SC_BAD_REQUEST);

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * リフレッシュトークンAPIのボディにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void リフレッシュトークンAPIのボディにclient_secretの指定がない場合_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-cl-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        String clientId = UrlUtils.cellRoot(TEST_APP_CELL1);

        JSONObject json = passRes.bodyAsJson();
        String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        // リフレッシュトークン認証
        TResponse tokenRes = Http.request("authn/auth.txt")
                .with("remoteCell", TEST_CELL1)
                .with("body", "grant_type=refresh_token&refresh_token=" + refreshToken + "&client_id=" + clientId)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * リフレッシュトークンAPIのヘッダにclient_secretの指定がない場合_400が返却されること.
     */
    @Test
    public final void リフレッシュトークンAPIのヘッダにclient_secretの指定がない場合_400が返却されること() {
        // セルに対してパスワード認証
        TResponse passRes =
                Http.request("authn/password-cl-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = passRes.bodyAsJson();
        String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);

        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1), "");
        // リフレッシュトークン認証
        TResponse tokenRes = Http.request("authn/auth-with-header.txt")
                .with("remoteCell", TEST_CELL1)
                .with("Authorization_header", schemaTransCellAccessTokenHeader)
                .with("body", "grant_type=refresh_token&refresh_token=" + refreshToken)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_CLIENT, errDesc);
    }

    /**
     * リフレッシュトークンAPIのrefresh_tokenが空文字の場合_400が返却されること.
     */
    @Test
    public final void リフレッシュトークンAPIのrefresh_tokenが空文字の場合_400が返却されること() {
        // リフレッシュトークン認証
        TResponse tokenRes = Http.request("authn/auth.txt")
                .with("remoteCell", TEST_CELL1)
                .with("body", "grant_type=refresh_token&refresh_token=")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_GRANT, errDesc);
    }

    /**
     * リフレッシュトークンAPIのrefresh_tokenの指定がない場合_400が返却されること.
     */
    @Test
    public final void リフレッシュトークンAPIのrefresh_tokenの指定がない場合_400が返却されること() {
        // リフレッシュトークン認証
        TResponse tokenRes = Http.request("authn/auth.txt")
                .with("remoteCell", TEST_CELL1)
                .with("body", "grant_type=refresh_token")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();

        AuthTestCommon.checkAuthenticateHeaderNotExists(tokenRes);
        String code = PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.getCode();
        String message = PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.getMessage();
        String errDesc = String.format("[%s] - %s", code, message);

        checkErrorResponseBody(tokenRes, Error.INVALID_GRANT, errDesc);
    }

    /**
     * Json形式のエラーレスポンスのチェック(認証).
     * @param res レスポンス
     * @param expectedError 期待するエラータイプ
     * @param expectedErrorDescription 期待するメッセージ
     */
    private void checkErrorResponseBody(TResponse res, String expectedError, String expectedErrorDescription) {
        String error = (String) ((JSONObject) res.bodyAsJson()).get("error");
        String errorDescription = (String) ((JSONObject) res.bodyAsJson()).get("error_description");
        assertEquals(expectedError, error);
        assertEquals(expectedErrorDescription, errorDescription);
    }
}

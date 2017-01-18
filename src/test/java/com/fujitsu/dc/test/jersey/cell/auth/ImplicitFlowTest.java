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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.cookie.Cookie;
import org.json.simple.JSONObject;
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

import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token;
import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.TokenParseException;
import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import com.fujitsu.dc.common.auth.token.AccountAccessToken;
import com.fujitsu.dc.common.auth.token.CellLocalAccessToken;
import com.fujitsu.dc.common.auth.token.CellLocalRefreshToken;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.common.auth.token.UnitLocalUnitUserToken;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreMessageUtils;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.lock.LockManager;
import com.fujitsu.dc.core.rs.cell.AuthResourceUtils;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * ImplicitFlow認証のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
@SuppressWarnings("restriction")
public class ImplicitFlowTest extends JerseyTest {

    private static final String MAX_AGE = "maxAge";
    private static final String SESSION_ID = OAuth2Helper.Key.SESSION_ID;
    private static final String REDIRECT_HTML = "__/redirect.html";
    static final String DEFAULT_STATE = "0000000111";
    private List<Cookie> cookies = null;

    /**
     * 前処理.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * 後処理.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * コンストラクタ.
     */
    public ImplicitFlowTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * 存在しないCellに対してImplicitFlow認証リクエストを実行し404エラーとなること.
     */
    @Test
    public final void 存在しないCellに対してImplicitFlow認証リクエストを実行し404エラーとなること() {
        String reqCell = UrlUtils.cellRoot("dummyCell");

        DcResponse res = requesttoAuthz(null, reqCell, Setup.TEST_CELL_SCHEMA1, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
    }

    /**
     * 認証フォームへのPOSTで200が返ること.
     */
    @Test
    public final void 認証フォームへのPOSTで200が返ること() {

        DcResponse res = requesttoAuthz(null);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0002", Setup.TEST_CELL1);
    }

    /**
     * 認証フォームへのPOSTでclient_idで指定したCellに認証リクエストを実行し400エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでclient_idで指定したCellに認証リクエストを実行し400エラーとなること() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL1);

        try {
            // Box作成
            BoxUtils.createWithSchema(Setup.TEST_CELL1, "authzBox", AbstractCase.MASTER_TOKEN_NAME, clientId);

            DcResponse res = requesttoAuthz(null, Setup.TEST_CELL1, clientId, null);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        } finally {
            BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "authzBox");
        }
    }

    /**
     * 認証フォームへのPOSTでredirect_uriとclient_idが異なる場合400エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでredirect_uriとclient_idが異なる場合400エラーとなること() {
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL2) + REDIRECT_HTML;

        DcResponse res = requesttoAuthz(null, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが存在しない場合認可エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが存在しない場合認可エラーとなること() {
        String clientId = UrlUtils.cellRoot("dummyCell");

        DcResponse res = requesttoAuthz(null, Setup.TEST_CELL1, clientId, null);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PS-ER-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが別のCellに存在する場合認可エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが別のCellに存在する場合認可エラーとなること() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String cellName = "authztestcell";

        try {
            // Cell作成
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // __authzリクエストを実行
            DcResponse res = requesttoAuthz(null, cellName, clientId, null);
            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

            assertEquals(UrlUtils.cellRoot(cellName) + "__html/error?code=PS-ER-0003",
                    res.getFirstHeader(HttpHeaders.LOCATION));
        } finally {
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName);
        }
    }

    /**
     * パスワード認証で自分セルローカルトークンを取得できること.
     */
    @Test
    public final void パスワード認証で自分セルローカルトークンを取得できること() {

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        String addbody = "&username=account2&password=password2";

        DcResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // アカウントの最終ログイン時刻が更新されたことの確認
        AuthTestCommon.accountLastAuthenticatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // cookieの値と有効期限の確認
        checkSessionId(false, Setup.TEST_CELL1);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            AccountAccessToken aToken = AccountAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
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
     * パスワード認証で不正なパスワードを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     */
    @Test
    public final void パスワード認証で不正なパスワードを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること() {

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        String addbody = "&username=account2&password=dummypassword";

        DcResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);
        AuthTestCommon.waitForAccountLock();
    }

    /**
     * パスワード認証失敗後1秒以内に成功する認証をリクエストした場合200が返却されてエラーhtmlが返却されること.
     * com.fujitsu.dc.core.lock.accountlock.timeを1秒に設定すると失敗するためIgnore
     */
    @Test
    @Ignore
    public final void パスワード認証失敗後1秒以内に成功する認証をリクエストした場合200が返却されてエラーhtmlが返却されること() {
        String lockType = DcCoreConfig.getLockType();
        if (lockType.equals("memcached")) {
            String addbody = "&username=account2&password=dummypassword";

            // 認証前のアカウントの最終ログイン時刻を取得しておく
            Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

            // パスワード認証(失敗)
            DcResponse res = requesttoAuthz(addbody);

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);

            addbody = "&username=account2&password=password2";
            // 1秒以内にパスワード認証(401エラー(PR401-AN-0019))
            res = requesttoAuthz(addbody);

            assertEquals(HttpStatus.SC_OK, res.getStatusCode());
            // アカウントの最終ログイン時刻が更新されていないことの確認
            AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0006", Setup.TEST_CELL1);
            AuthTestCommon.waitForAccountLock();
        }
    }

    /**
     * パスワード認証失敗後1秒以内に失敗する認証をリクエストした場合200が返却されてエラーhtmlが返却されること.
     * com.fujitsu.dc.core.lock.accountlock.timeを1秒に設定すると失敗するためIgnore
     */
    @Test
    @Ignore
    public final void パスワード認証失敗後1秒以内に失敗する認証をリクエストした場合200が返却されてエラーhtmlが返却されること() {
        String lockType = DcCoreConfig.getLockType();
        if (lockType.equals("memcached")) {
            String addbody = "&username=account2&password=dummypassword";

            // パスワード認証(失敗)
            DcResponse res = requesttoAuthz(addbody);

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);

            addbody = "&username=account2&password=dummypassword";
            // 1秒以内にパスワード認証(401エラー(PR401-AN-0019))
            res = requesttoAuthz(addbody);

            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0006", Setup.TEST_CELL1);
            AuthTestCommon.waitForAccountLock();
        }
    }

    /**
     * パスワード認証失敗後1秒後に成功する認証をリクエストした場合302が返却されること.
     */
    @Test
    public final void パスワード認証失敗後1秒後に成功する認証をリクエストした場合302が返却されること() {
        String lockType = DcCoreConfig.getLockType();
        if (lockType.equals("memcached")) {
            String addbody = "&username=account2&password=dummypassword";

            // パスワード認証(失敗)
            DcResponse res = requesttoAuthz(addbody);

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);

            addbody = "&username=account2&password=password2";

            AuthTestCommon.waitForAccountLock();

            // 1秒後にパスワード認証(認証成功)
            res = requesttoAuthz(addbody);

            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

            Map<String, String> response = parseResponse(res);
            try {
                AccountAccessToken aToken = AccountAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
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
    }

    /**
     * パスワード認証失敗後1秒後に失敗する認証をリクエストした場合200が返却されてエラーhtmlが返却されること.
     */
    @Test
    public final void パスワード認証失敗後1秒後に失敗する認証をリクエストした場合200が返却されてエラーhtmlが返却されること() {
        String lockType = DcCoreConfig.getLockType();
        if (lockType.equals("memcached")) {
            String addbody = "&username=account2&password=dummypassword";

            // パスワード認証(失敗)
            DcResponse res = requesttoAuthz(addbody);

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);

            addbody = "&username=account2&password=dummypassword";

            AuthTestCommon.waitForAccountLock();

            // 1秒後にパスワード認証(401エラー(PS-AU-0004))
            res = requesttoAuthz(addbody);

            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);
            AuthTestCommon.waitForAccountLock();
        }
    }

    /**
     * パスワード認証でユーザ名またはパスワードに空文字を指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     */
    @Test
    public final void パスワード認証でユーザ名またはパスワードに空文字を指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること() {

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        // ユーザ名が空
        String addbody = "&username=&password=password2";

        DcResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0003", Setup.TEST_CELL1);

        // パスワードが空
        addbody = "&username=account2&password=";

        res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0003", Setup.TEST_CELL1);
        AuthTestCommon.waitForAccountLock();
    }

    /**
     * パスワード認証で未登録のユーザを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     */
    @Test
    public final void パスワード認証で未登録のユーザを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること() {

        String addbody = "&username=dummyaccount&password=dummypassword";

        DcResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0004", Setup.TEST_CELL1);
    }

    /**
     * パスワード認証でトランスセルトークンを取得できること.
     */
    @Ignore
    @Test
    public final void パスワード認証でトランスセルトークンを取得できること() {

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL2, "account4");

        String addbody = "&username=account4&password=password4&dc_target=" + UrlUtils.cellRoot(Setup.TEST_CELL1);

        DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されたことの確認
        AuthTestCommon.accountLastAuthenticatedCheck(Setup.TEST_CELL2, "account4", lastAuthenticatedTime);

        // cookieの値と有効期限の確認
        checkSessionId(false, Setup.TEST_CELL2);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            AbstractOAuth2Token tcToken = TransCellAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL1), UrlUtils.getHost());
            assertNotNull("access token parse error.", tcToken);
            assertTrue("access token parse error.", tcToken instanceof TransCellAccessToken);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } catch (TokenDsigException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } catch (TokenRootCrtException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * パスワード認証でULUUTを取得できること.
     */
    @Test
    public final void パスワード認証でULUUTを取得できること() {

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        String addbody = "&username=account2&password=password2&dc_owner=true";

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        DcResponse res = requesttoAuthz(addbody);

        // アカウントの最終ログイン時刻が更新されたことの確認
        AuthTestCommon.accountLastAuthenticatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * パスワード認証でdc_targetとdc_ownerを指定した場合ULUUTを取得できること.
     */
    @Test
    public final void パスワード認証でdc_targetとdc_ownerを指定した場合ULUUTを取得できること() {

        String addbody = "&username=account2&password=password2&dc_owner=true&dc_target="
                + UrlUtils.cellRoot(Setup.TEST_CELL1);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL2, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.getHost());
            assertEquals(Setup.OWNER_HMC, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * パスワード認証でredirect_uriにURL形式ではない文字列を指定した場合302が返ること.
     */
    @Test
    public final void パスワード認証でredirect_uriにURL形式ではない文字列を指定した場合302が返ること() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = REDIRECT_HTML;

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でredirect_uriとclient_idが異なる場合302が返ること.
     */
    @Test
    public final void パスワード認証でredirect_uriとclient_idが異なる場合302が返ること() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL2) + REDIRECT_HTML;

        DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でresponse_typeにtoken以外の文字列を指定した場合302が返ること.
     */
    @Test
    public final void パスワード認証でresponse_typeにtoken以外の文字列を指定した場合302が返ること() {
        String responseType = "code";

        String body = "response_type=" + responseType + "&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE + "&username=account2&password=password2";

        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // #error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=unsupported_response_type&error_description=unsupported_response_type&state=" + DEFAULT_STATE
                + "&code=PR400-AZ-0001",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること.
     */
    @Test
    public final void パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること() {

        String addbody = "&username=account2&password=password2&keeplogin=true";

        DcResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // cookieの値と有効期限の確認
        checkSessionId(true, Setup.TEST_CELL1);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            AccountAccessToken aToken = AccountAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
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
     * トークン認証で他人セルローカルトークンを取得できること.
     */
    @Test
    public final void トークン認証で他人セルローカルトークンを取得できること() {

        String transCellAccessToken = getTcToken();

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            CellLocalAccessToken token = CellLocalAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL2));
            assertNotNull("access token parse error.", token);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * トークン認証でトランスセルトークンを取得できること.
     */
    @Ignore
    @Test
    public final void トークン認証でトランスセルトークンを取得できること() {

        try {
            // トークン認証用のターゲットCell作成
            CellUtils.create("authzcell", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // ExtCell作成
            ExtCellUtils.create(AbstractCase.MASTER_TOKEN_NAME, "authzcell",
                    UrlUtils.cellRoot(Setup.TEST_CELL2),
                    HttpStatus.SC_CREATED);

            String transCellAccessToken = getTcToken();

            // トークン認証
            String addbody = "&assertion=" + transCellAccessToken + "&dc_target=" + UrlUtils.cellRoot("authzcell");
            DcResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2,
                    UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);

            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

            // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
            Map<String, String> response = parseResponse(res);
            try {
                AbstractOAuth2Token tcToken = TransCellAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                        UrlUtils.cellRoot(Setup.TEST_CELL2), UrlUtils.getHost());
                assertNotNull("access token parse error.", tcToken);
                assertTrue("access token parse error.", tcToken instanceof TransCellAccessToken);
                assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
                assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
                assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
            } catch (TokenParseException e) {
                fail(e.getMessage());
                e.printStackTrace();
            } catch (TokenDsigException e) {
                fail(e.getMessage());
                e.printStackTrace();
            } catch (TokenRootCrtException e) {
                fail(e.getMessage());
                e.printStackTrace();
            }
        } finally {
            ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, "authzcell",
                    UrlUtils.cellRoot(Setup.TEST_CELL2));
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, "authzcell");
        }
    }

    /**
     * トークン認証でresponse_typeの指定が無い場合302が返却されること.
     */
    @Test
    public final void トークン認証でresponse_typeの指定が無い場合302が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirecturi = clientId + REDIRECT_HTML;
        String body = "client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // {redirect_uri}#error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=invalid_request&error_description=invalid_request&state=" + DEFAULT_STATE
                + "&code=PR400-AZ-0004",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でclient_idの指定が無い場合302が返却されること.
     */
    @Test
    public final void トークン認証でclient_idの指定が無い場合302が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String redirecturi = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML;
        String body = "response_type=token"
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL2) + "__html/error?code=PR400-AZ-0002",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でredirect_uriの指定が無い場合302が返却されること.
     */
    @Test
    public final void トークン認証でredirect_uriの指定が無い場合302が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String body = "response_type=token&client_id=" + clientId
                + "&state=" + DEFAULT_STATE + addbody;
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL2) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でassertionに不正な文字列を指定した場合302が返却されること.
     */
    @Test
    public final void トークン認証でassertionに不正な文字列を指定した場合302が返却されること() {

        // トークン認証
        String addbody = "&assertion=dummytoken";
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirecturi = clientId + REDIRECT_HTML;
        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // {redirect_uri}#error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(redirecturi
                + "#error=access_denied&error_description=access_denied&state=" + DEFAULT_STATE
                + "&code=PR401-AZ-0002",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でassertionのターゲットが別のCellの場合302が返却されること.
     */
    @Test
    public final void トークン認証でassertionのターゲットが別のCellの場合302が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirecturi = clientId + REDIRECT_HTML;
        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // {redirect_uri}#error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(redirecturi
                + "#error=access_denied&error_description=access_denied&state=" + DEFAULT_STATE
                + "&code=PR401-AZ-0002",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * Cookie認証で自分セルローカルトークンを取得できること.
     */
    @Test
    public final void Cookie認証で自分セルローカルトークンを取得できること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account2&password=password2";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // 認証前のアカウントの最終ログイン時刻を取得しておく
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account2");
        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        checkSessionId(true, Setup.TEST_CELL1);
        // アカウントの最終ログイン時刻が更新されていないことの確認
        AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(Setup.TEST_CELL1, "account2", lastAuthenticatedTime);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            AccountAccessToken aToken = AccountAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
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
     * Cookie認証でトランスセルトークンを取得できること.
     */
    @Ignore
    @Test
    public final void Cookie認証でトランスセルトークンを取得できること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account2&password=password2";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE
                + "&dc_target=" + UrlUtils.cellRoot(Setup.TEST_CELL2);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        checkSessionId(true, Setup.TEST_CELL1);

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            AbstractOAuth2Token tcToken = TransCellAccessToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.cellRoot(Setup.TEST_CELL2), UrlUtils.getHost());
            assertNotNull("access token parse error.", tcToken);
            assertTrue("access token parse error.", tcToken instanceof TransCellAccessToken);
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } catch (TokenDsigException e) {
            fail(e.getMessage());
            e.printStackTrace();
        } catch (TokenRootCrtException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cookie認証でdc_ownerを指定した場合ULUUTを取得できること.
     */
    @Test
    public final void Cookie認証でdc_ownerを指定した場合ULUUTを取得できること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account2&password=password2";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE
                + "&dc_owner=true";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cookie認証でdc_targetとdc_ownerを指定した場合ULUUTを取得できること.
     */
    @Test
    public final void Cookie認証でdc_targetとdc_ownerを指定した場合ULUUTを取得できること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account2&password=password2";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE
                + "&dc_owner=true&dc_target=" + UrlUtils.cellRoot(Setup.TEST_CELL2);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = parseResponse(res);
        try {
            UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(response.get(OAuth2Helper.Key.ACCESS_TOKEN),
                    UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());
            assertEquals(OAuth2Helper.Scheme.BEARER, response.get(OAuth2Helper.Key.TOKEN_TYPE));
            assertEquals("3600", response.get(OAuth2Helper.Key.EXPIRES_IN));
            assertEquals(DEFAULT_STATE, response.get(OAuth2Helper.Key.STATE));
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cookie認証で昇格失敗した場合302が返ること.
     */
    @Test
    public final void Cookie認証で昇格失敗した場合302が返ること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account4&password=password4";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE
                + "&dc_owner=true";
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0005", Setup.TEST_CELL1, "true");
    }

    /**
     * Cookie認証でオーナー指定の無いセルに対しdc_ownerを指定した場合302が返ること.
     */
    @Test
    public final void Cookie認証でオーナー指定の無いセルに対しdc_ownerを指定した場合302が返ること() {

        String cellName = "authzcell";
        try {
            // Cell作成
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Account作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, "account1", "password1",
                    HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.createWithSchema(cellName, "box", AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1));

            // パスワード認証で自分セルリフレッシュトークン取得
            String addbody = "&username=account1&password=password1";
            DcResponse res = requesttoAuthz(addbody, cellName, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);
            assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
            // cookieの値と有効期限の確認
            String sessionId = checkSessionId(false, cellName);

            // アカウントにユニット昇格権限付与
            DavResourceUtils.setProppatch(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

            // Cookie認証
            String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                    + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                    + "&state=" + DEFAULT_STATE
                    + "&dc_owner=true";
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("session-id", sessionId);
            res = requesttoAuthzWithBody(cellName, body, headers);

            // cookieが設定されていないことの確認
            Map<String, Object> sessionMap = getSessionMap();
            assertNull(sessionMap.get(SESSION_ID));

            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // レスポンスヘッダのチェック
            assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

            // レスポンスボディのチェック
            checkHtmlBody(res, "PS-AU-0005", cellName, "true");
        } finally {
            // Box削除
            BoxUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, "box");

            // Account削除
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, "account1", -1);

            // Cell削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName);
        }
    }

    /**
     * Cookie認証でsession_idに不正なトークンを指定した場合302エラーとなること.
     */
    @Test
    public final void Cookie認証でsession_idに不正なトークンを指定した場合302エラーとなること() {

        // パスワード認証で自分セルリフレッシュトークン取得
        String addbody = "&username=account2&password=password2";
        DcResponse res = requesttoAuthz(addbody);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, res.getStatusCode());
        // cookieの値と有効期限の確認
        String sessionId = checkSessionId(false, Setup.TEST_CELL1);

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        res = requesttoAuthzWithBody(Setup.TEST_CELL2, body, headers);

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0005", Setup.TEST_CELL2);
    }

    /**
     * Cookie認証でsession_idにアクセストークンを指定した場合302エラーとなること.
     */
    @Test
    public final void Cookie認証でsession_idにアクセストークンを指定した場合302エラーとなること() {

        // アクセストークンの取得
        String sessionId = ResourceUtils.getMyCellLocalToken(Setup.TEST_CELL1, "account2", "password2");

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0005", Setup.TEST_CELL1);
    }

    /**
     * Cookie認証でsession_idに空文字を指定した場合302エラーとなること.
     */
    @Test
    public final void Cookie認証でsession_idに空文字を指定した場合302エラーとなること() {

        // アクセストークンの取得
        String sessionId = "";

        // Cookie認証
        String body = "response_type=token&client_id=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "&redirect_uri=" + UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "&state=" + DEFAULT_STATE;
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("session-id", sessionId);
        DcResponse res = requesttoAuthzWithBody(Setup.TEST_CELL1, body, headers);

        // cookieが設定されていないことの確認
        Map<String, Object> sessionMap = getSessionMap();
        assertNull(sessionMap.get(SESSION_ID));

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkHtmlBody(res, "PS-AU-0005", Setup.TEST_CELL1);
    }

    /**
     * __authzへのリクエストを投入する.
     * @param addbody 追加のボディ
     * @return レスポンス
     */
    private DcResponse requesttoAuthz(String addbody) {
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
    private DcResponse requesttoAuthz(String addbody, String requestCellName, String clientId, String redirecturi) {
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
    private DcResponse requesttoAuthzWithBody(String requestCellName, String body) {
        return requesttoAuthzWithBody(requestCellName, body, null);
    }

    /**
     * __authzへのリクエストを投入する.
     * @param requestCellName requestCellName
     * @param body ボディ
     * @param requestheaders 追加のリクエストヘッダ
     * @return レスポンス
     */
    private DcResponse requesttoAuthzWithBody(String requestCellName,
            String body,
            HashMap<String, String> requestheaders) {
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        if (requestheaders == null) {
            requestheaders = new HashMap<String, String>();
        }
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            cookies = null;
            res = rest.post(UrlUtils.cellRoot(requestCellName) + "__authz", body,
                    requestheaders);

            cookies = rest.getCookies();

        } catch (DcException e) {
            e.printStackTrace();
        }

        return res;
    }

    private Map<String, String> parseResponse(DcResponse res) {
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

    private String checkSessionId(boolean keeplogin, String cellName) {
        Map<String, Object> sessionMap = getSessionMap();

        try {
            CellLocalRefreshToken rToken = CellLocalRefreshToken.parse((String) sessionMap.get(SESSION_ID),
                    UrlUtils.cellRoot(cellName));
            assertNotNull("can't get session-id from response.", rToken);

            if (keeplogin) {
                assertNotNull("can't get ExpiryDate from response.", sessionMap.get(MAX_AGE));
            } else {
                assertNull("ExpiryDate is exists from response.", sessionMap.get(MAX_AGE));
            }
        } catch (TokenParseException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }

        return (String) sessionMap.get(SESSION_ID);
    }

    private Map<String, Object> getSessionMap() {
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        String sessionId = null;
        Date maxAge = null;
        for (Cookie cookie : cookies) {
            if (SESSION_ID.equals(cookie.getName())) {
                sessionId = cookie.getValue();
                maxAge = cookie.getExpiryDate();
            }
        }
        sessionMap.put(SESSION_ID, sessionId);
        sessionMap.put(MAX_AGE, maxAge);

        return sessionMap;
    }

    static void checkHtmlBody(DcResponse res, String messageId, String dataCellName) {
        checkHtmlBody(res, messageId, dataCellName, "");
    }

    static void checkHtmlBody(DcResponse res, String messageId, String dataCellName, String dcOwner) {
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
        assertEquals(DcCoreMessageUtils.getMessage("PS-AU-0001"), ((Element) nodeList.item(0)).getTextContent());

        nodeList = document.getElementsByTagName("body");
        String expectedAppUrl = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + "__/profile.json";
        String expectedDataUrl = UrlUtils.cellRoot(dataCellName) + "__/profile.json";
        assertEquals("requestFile('GET', '" + expectedAppUrl + "' , '" + expectedDataUrl + "' ,true )",
                ((Element) nodeList.item(0)).getAttribute("onload"));

        nodeList = document.getElementsByTagName("h1");
        assertEquals(DcCoreMessageUtils.getMessage("PS-AU-0001"), ((Element) nodeList.item(0)).getTextContent());

        nodeList = document.getElementsByTagName("form");
        String expectedFormUrl = UrlUtils.cellRoot(dataCellName) + "__authz";
        assertEquals(expectedFormUrl, ((Element) nodeList.item(0)).getAttribute("action"));

        nodeList = document.getElementsByTagName("div");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("id");
            if ("message".equals(id)) {
                assertEquals(DcCoreMessageUtils.getMessage(messageId).replaceAll("<br />", ""),
                        element.getTextContent());
            }
        }

        nodeList = document.getElementsByTagName("input");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            String id = element.getAttribute("id");
            if ("state".equals(id)) {
                assertEquals(DEFAULT_STATE, element.getAttribute("value"));
            } else if ("dc_target".equals(id)) {
                assertEquals("", element.getAttribute("value"));

            } else if ("dc_owner".equals(id)) {
                assertEquals(dcOwner, element.getAttribute("value"));
            } else if ("client_id".equals(id)) {
                assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), element.getAttribute("value"));
            } else if ("redirect_uri".equals(id)) {
                assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML,
                        element.getAttribute("value"));
            }
        }
    }

    private String getTcToken() {
        // セルに対してパスワード認証し、トランスセルトークンを取得
        TResponse resPassAuth =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", Setup.TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("dc_target", UrlUtils.cellRoot(Setup.TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = resPassAuth.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        return transCellAccessToken;
    }

}

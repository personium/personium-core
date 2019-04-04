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

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
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
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
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
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * ImplicitFlow認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
@SuppressWarnings("restriction")
public class ImplicitFlowTest extends PersoniumTest {

    private static final String MAX_AGE = "maxAge";
    private static final String SESSION_ID = OAuth2Helper.Key.SESSION_ID;
    private static final String REDIRECT_HTML = "__/redirect.html";
    static final String DEFAULT_STATE = "0000000111";
    private List<Cookie> cookies = null;

    /**
     * before class.
     * @throws Exception Unexpected exception
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
    }

    /**
     * after class.
     * @throws Exception Unexpected exception
     */
    @AfterClass
    public static void afterClass() throws Exception {
    }

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
        super(new PersoniumCoreApplication());
    }

    /**
     * 存在しないCellに対してImplicitFlow認証リクエストを実行し404エラーとなること.
     */
    @Test
    public final void 存在しないCellに対してImplicitFlow認証リクエストを実行し404エラーとなること() {
        String reqCell = UrlUtils.cellRoot("dummyCell");

        PersoniumResponse res = requesttoAuthz(null, reqCell, Setup.TEST_CELL_SCHEMA1, null);
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
    }

    /**
     * No query parameter. return 303.
     */
    @Test
    public final void No_query_parameter() {

        PersoniumResponse res = requesttoAuthz(null);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));
    }

    /**
     * 認証フォームへのPOSTでclient_idで指定したCellに認証リクエストを実行し認可エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでclient_idで指定したCellに認証リクエストを実行し認可エラーとなること() {
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL1);

        try {
            // Box作成
            BoxUtils.createWithSchema(Setup.TEST_CELL1, "authzBox", AbstractCase.MASTER_TOKEN_NAME, clientId);

            PersoniumResponse res = requesttoAuthz(null, Setup.TEST_CELL1, clientId, null);
            assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        } finally {
            BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "authzBox");
        }
    }

    /**
     * 認証フォームへのPOSTでredirect_uriとclient_idが異なる場合認可エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでredirect_uriとclient_idが異なる場合認可エラーとなること() {
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL2) + REDIRECT_HTML;

        PersoniumResponse res = requesttoAuthz(null, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());
    }

    /**
     * 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが存在しない場合認可エラーとなること.
     */
    @Test
    public final void 認証フォームへのPOSTでclient_idで指定したCellをスキーマに持つBoxが存在しない場合認可エラーとなること() {
        String clientId = UrlUtils.cellRoot("dummyCell");

        PersoniumResponse res = requesttoAuthz(null, Setup.TEST_CELL1, clientId, null);
        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0007",
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
            PersoniumResponse res = requesttoAuthz(null, cellName, clientId, null);
            assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

            assertEquals(UrlUtils.cellRoot(cellName) + "__html/error?code=PR400-AZ-0007",
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

        String addbody = "&username=account2&password=password2";

        PersoniumResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
     * パスワード認証でユーザ名またはパスワードに空文字を指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     */
    @Test
    public final void パスワード認証でユーザ名またはパスワードに空文字を指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること() {

        // ユーザ名が空
        String addbody = "&username=&password=password2";
        PersoniumResponse res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        Map<String, String> queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));

        // パスワードが空
        addbody = "&username=account2&password=";
        res = requesttoAuthz(addbody);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SC_SEE_OTHER);
        assertTrue(res.getFirstHeader(HttpHeaders.LOCATION).startsWith(
                UrlUtils.cellRoot(Setup.TEST_CELL1) + "__authz?"));
        assertTrue(UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION)).isEmpty());
        queryMap = UrlUtils.parseQuery(res.getFirstHeader(HttpHeaders.LOCATION));
        assertThat(queryMap.get(OAuth2Helper.Key.CODE), is("PS-AU-0003"));
    }

    /**
     * パスワード認証で未登録のユーザを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること.
     */
    @Test
    public final void パスワード認証で未登録のユーザを指定して自分セルトークンを取得し認証フォームにエラーメッセージが出力されること() {

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
     * パスワード認証でトランスセルトークンを取得できること.
     */
    @Ignore
    @Test
    public final void パスワード認証でトランスセルトークンを取得できること() {

        String addbody = "&username=account4&password=password4&p_target=" + UrlUtils.cellRoot(Setup.TEST_CELL1);

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                null);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
    @Ignore // UUT promotion setting API invalidation.
    public final void パスワード認証でULUUTを取得できること() {

        String addbody = "&username=account2&password=password2&p_owner=true";

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        PersoniumResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
     * パスワード認証でp_targetとp_ownerを指定した場合ULUUTを取得できること.
     */
    @Test
    @Ignore // UUT promotion setting API invalidation.
    public final void パスワード認証でp_targetとp_ownerを指定した場合ULUUTを取得できること() {

        String addbody = "&username=account2&password=password2&p_owner=true&p_target="
                + UrlUtils.cellRoot(Setup.TEST_CELL1);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL2, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                null);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
     * パスワード認証でredirect_uriにURL形式ではない文字列を指定した場合303が返ること.
     */
    @Test
    public final void パスワード認証でredirect_uriにURL形式ではない文字列を指定した場合303が返ること() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = REDIRECT_HTML;

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でredirect_uriとclient_idが異なる場合303が返ること.
     */
    @Test
    public final void パスワード認証でredirect_uriとclient_idが異なる場合303が返ること() {
        String addbody = "&username=account2&password=password2";
        String redirectUri = UrlUtils.cellRoot(Setup.TEST_CELL2) + REDIRECT_HTML;

        PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1),
                redirectUri);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * パスワード認証でresponse_typeにtoken以外の文字列を指定した場合303が返ること.
     */
    @Test
    public final void パスワード認証でresponse_typeにtoken以外の文字列を指定した場合303が返ること() {
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
     * パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること.
     */
    // TODO keeplogin実装後に対応
    @Ignore
    @Test
    public final void パスワード認証でkeeploginを指定した場合Cookieの有効期限が設定されること() {

        String addbody = "&username=account2&password=password2&keeplogin=true";

        PersoniumResponse res = requesttoAuthz(addbody);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
        Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
            String addbody = "&assertion=" + transCellAccessToken + "&p_target=" + UrlUtils.cellRoot("authzcell");
            PersoniumResponse res = requesttoAuthz(addbody, Setup.TEST_CELL2,
                    UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1), null);

            assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

            // {redirect_uri}#access_token={access_token}&token_type=Bearer&expires_in={expires_in}&state={state}
            Map<String, String> response = UrlUtils.parseFragment(res.getFirstHeader(HttpHeaders.LOCATION));
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
     * トークン認証でresponse_typeの指定が無い場合303が返却されること.
     */
    @Test
    public final void トークン認証でresponse_typeの指定が無い場合303が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String redirecturi = clientId + REDIRECT_HTML;
        String body = "client_id=" + clientId
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        // {redirect_uri}#error={error}&error_description={error_description}&state={state}&code={code}
        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML
                + "#error=invalid_request&error_description=Request+parameter+is+invalid+%5Bresponse_type%5D.&state="
                + DEFAULT_STATE + "&code=PR400-AZ-0004",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でclient_idの指定が無い場合303が返却されること.
     */
    @Test
    public final void トークン認証でclient_idの指定が無い場合303が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String redirecturi = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1) + REDIRECT_HTML;
        String body = "response_type=token"
                + "&redirect_uri=" + redirecturi
                + "&state=" + DEFAULT_STATE + addbody;
        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL2) + "__html/error?code=PR400-AZ-0002",
                res.getFirstHeader(HttpHeaders.LOCATION));
    }

    /**
     * トークン認証でredirect_uriの指定が無い場合303が返却されること.
     */
    @Test
    public final void トークン認証でredirect_uriの指定が無い場合303が返却されること() {

        String transCellAccessToken = getTcToken();

        // トークン認証
        String addbody = "&assertion=" + transCellAccessToken;
        String clientId = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1);
        String body = "response_type=token&client_id=" + clientId
                + "&state=" + DEFAULT_STATE + addbody;
        PersoniumResponse res = requesttoAuthzWithBody(Setup.TEST_CELL2, body);

        assertEquals(HttpStatus.SC_SEE_OTHER, res.getStatusCode());

        assertEquals(UrlUtils.cellRoot(Setup.TEST_CELL2) + "__html/error?code=PR400-AZ-0003",
                res.getFirstHeader(HttpHeaders.LOCATION));
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

    private String getTcToken() {
        // セルに対してパスワード認証し、トランスセルトークンを取得
        TResponse resPassAuth =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", Setup.TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(Setup.TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = resPassAuth.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        return transCellAccessToken;
    }

}

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token;
import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
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
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * 認証のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthCookieTest extends JerseyTest {

    private static final String TARGET_CELL = "targetcell";
    private static final String LOCAL_CELL = "localcell";
    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TESTCELL_NAME = "cell001";
    static final String USERNAME = "user0";
    static final String PASS = "password";
    static final String ROLENAMENONEBOX = "role0";
    static final int MILLISECS_IN_AN_MINITE = 60 * 1000;

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();

    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public AuthCookieTest() {
        super(new WebAppDescriptor.Builder(AuthCookieTest.INIT_PARAMS).build());
    }

    /**
     * dc_target指定なしでdc_cookieがtrueの場合にCookieが返却されること.
     */
    @Test
    public final void dc_target指定なしでdc_cookieがtrueの場合にCookieが返却されること() {
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(TEST_CELL1, "account1");
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1",
                "password1", "true", HttpStatus.SC_OK);
        AuthTestCommon.accountLastAuthenticatedCheck(TEST_CELL1, "account1", lastAuthenticatedTime);
        String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNotNull(body);
        String header = passRes.getHeader("Set-Cookie");
        assertNotNull(header);

        Map<String, String> cookie = resolveCookie(header);
        assertTrue(cookie.containsKey("dc_cookie"));
        assertTrue(cookie.containsKey("Version"));
        assertEquals("0", cookie.get("Version"));
        assertTrue(cookie.containsKey("Domain"));
        assertNotNull(cookie.get("Domain"));
        assertTrue(cookie.containsKey("Path"));
        assertTrue(cookie.get("Path").endsWith(TEST_CELL1 + "/"));
    }

    /**
     * dc_target指定なしでdc_cookieがfalseの場合にCookieが返却されないこと.
     */
    @Test
    public final void dc_target指定なしでdc_cookieがfalseの場合にCookieが返却されないこと() {
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1",
                "password1", "false", HttpStatus.SC_OK);
        String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNull(body);
        String header = passRes.getHeader("Set-Cookie");
        assertNull(header);
    }

    /**
     * dc_target指定ありでdc_cookieがtrueの場合にCookieが返却されないこと.
     */
    @Test
    public final void dc_target指定ありでdc_cookieがtrueの場合にCookieが返却されないこと() {
        try {
            // 本テスト用セルの作成
            CellUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // １．ボックスと結びつかないロールのトランスセル確認
            // アカウント追加(user0)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME,
                    USERNAME, PASS, HttpStatus.SC_CREATED);

            // ロール追加（BOXに結びつかない）
            RoleUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null,
                    ROLENAMENONEBOX, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX, HttpStatus.SC_NO_CONTENT);

            TResponse passRes = requestAuthorizationWithTarget(TESTCELL_NAME, USERNAME,
                    PASS, "true", UrlUtils.cellRoot(TEST_CELL1), HttpStatus.SC_OK);
            String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNull(body);
            String header = passRes.getHeader("Set-Cookie");
            assertNull(header);
        } finally {
            // １．ボックスと結びつかないロールのトランスセル確認
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRollDelete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX);
            // アカウント削除
            AccountUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, HttpStatus.SC_NO_CONTENT);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null, ROLENAMENONEBOX);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME, -1);
        }
    }

    /**
     * dc_target指定ありでdc_cookieがfalseの場合にCookieが返却されないこと.
     */
    @Test
    public final void dc_target指定ありでdc_cookieがfalseの場合にCookieが返却されないこと() {
        try {
            // 本テスト用セルの作成
            CellUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // １．ボックスと結びつかないロールのトランスセル確認
            // アカウント追加(user0)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME,
                    USERNAME, PASS, HttpStatus.SC_CREATED);

            // ロール追加（BOXに結びつかない）
            RoleUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null,
                    ROLENAMENONEBOX, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX, HttpStatus.SC_NO_CONTENT);

            TResponse passRes = requestAuthorizationWithTarget(TESTCELL_NAME, USERNAME,
                    PASS, "false", UrlUtils.cellRoot(TEST_CELL1), HttpStatus.SC_OK);
            String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNull(body);
            String header = passRes.getHeader("Set-Cookie");
            assertNull(header);
        } finally {
            // １．ボックスと結びつかないロールのトランスセル確認
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRollDelete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX);
            // アカウント削除
            AccountUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, HttpStatus.SC_NO_CONTENT);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null, ROLENAMENONEBOX);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME, -1);
        }
    }

    /**
     * dc_target指定なしでdc_cookieが指定なしの場合にCookieが返却されないこと.
     */
    @Test
    public final void dc_target指定なしでdc_cookieが指定なしの場合にCookieが返却されないこと() {
        TResponse passRes = requestAuthentication(TEST_CELL1, "account1",
                "password1", null, HttpStatus.SC_OK);
        String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNull(body);
        String header = passRes.getHeader("Set-Cookie");
        assertNull(header);
    }

    /**
     * dc_target指定ありでdc_cookieが指定なしの場合にCookieが返却されないこと.
     */
    @Test
    public final void dc_target指定ありでdc_cookieが指定なしの場合にCookieが返却されないこと() {
        try {
            // 本テスト用セルの作成
            CellUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // １．ボックスと結びつかないロールのトランスセル確認
            // アカウント追加(user0)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME,
                    USERNAME, PASS, HttpStatus.SC_CREATED);

            // ロール追加（BOXに結びつかない）
            RoleUtils.create(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null,
                    ROLENAMENONEBOX, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX, HttpStatus.SC_NO_CONTENT);

            TResponse passRes = requestAuthorizationWithTarget(TESTCELL_NAME, USERNAME,
                    PASS, null, UrlUtils.cellRoot(TEST_CELL1), HttpStatus.SC_OK);
            String body = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNull(body);
            String header = passRes.getHeader("Set-Cookie");
            assertNull(header);
        } finally {
            // １．ボックスと結びつかないロールのトランスセル確認
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRollDelete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, null, ROLENAMENONEBOX);
            // アカウント削除
            AccountUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME,
                    USERNAME, HttpStatus.SC_NO_CONTENT);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(TESTCELL_NAME, AbstractCase.MASTER_TOKEN_NAME, null, ROLENAMENONEBOX);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TESTCELL_NAME, -1);
        }
    }

    /**
     * パスワード認証でクッキー発行後＿クッキー認証で処理が受け付けられること.
     */
    @Test
    public final void パスワード認証でクッキー発行後＿クッキー認証で処理が受け付けられること() {

        // パスワード認証要求、クッキーを取得
        TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                "password2", "true", HttpStatus.SC_OK);
        String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNotNull(cookiePeer);

        // クッキーを受け取る
        String header = passRes.getHeader("Set-Cookie");
        assertNotNull(header);

        Map<String, String> cookie = resolveCookie(header);

        // クッキーを与えてオペレーションを実行できるか確認する。
        requestOperation(TEST_CELL1, cookiePeer,
                "dc_cookie=" + cookie.get("dc_cookie"), HttpStatus.SC_OK);
    }

    /**
     * パスワード認証でクッキー発行後＿クッキー内容を変更し処理が拒否されること.
     */
    @Test
    public final void パスワード認証でクッキー発行後＿クッキー内容を変更し処理が拒否されること() {

        // パスワード認証要求、クッキーを取得
        TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                "password2", "true", HttpStatus.SC_OK);
        String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNotNull(cookiePeer);

        // クッキーを受け取る
        String header = passRes.getHeader("Set-Cookie");
        assertNotNull(header);

        Map<String, String> cookie = resolveCookie(header);

        // クッキーを改変してオペレーションが失敗することを確認する。
        TResponse res = requestOperation(TEST_CELL1, cookiePeer,
                "dc_cookie=" + cookie.get("dc_cookie") + "invalid", HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);
    }

    /**
     * トークン認証でクッキー発行後_クッキー認証で処理が受け付けられること.
     * @throws Exception URLエンコードエラー
     */
    @Test
    public final void トークン認証でクッキー発行後_クッキー認証で処理が受け付けられること() throws Exception {
        try {
            // テスト用資源作成
            createTestResource();

            // トークン認証要求、クッキーを取得
            long issuedAt = new Date().getTime();
            String issuer = UrlUtils.cellRoot(TARGET_CELL);
            String subject = issuer + "#appuser";
            String target = UrlUtils.cellRoot(LOCAL_CELL);
            List<Role> roleList = new ArrayList<Role>();
            String schema = "";

            // 期限切れでないトークンを生成
            TransCellAccessToken validToken = new TransCellAccessToken(
                    issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                    issuer, subject, target, roleList, schema);
            // セルに対してトークン認証
            TResponse passRes = Http.request("authn/issue-cookie-with-saml.txt")
                    .with("remoteCell", LOCAL_CELL)
                    .with("assertion", validToken.toTokenString())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキーを与えてオペレーション(セル1のBox一覧取得)で認証エラーにならないことを確認する。
            requestOperation(LOCAL_CELL, cookiePeer,
                    "dc_cookie=" + cookie.get("dc_cookie"), HttpStatus.SC_OK);
        } finally {
            // セル1とセル2を削除
            Setup.cellBulkDeletion(TARGET_CELL);
            Setup.cellBulkDeletion(LOCAL_CELL);
        }

    }

    /**
     * トークン認証でクッキー発行後_クッキー内容を変更し処理が拒否されること.
     * @throws Exception URLエンコードエラー
     */
    @Test
    public final void トークン認証でクッキー発行後_クッキー内容を変更し処理が拒否されること() throws Exception {
        try {
            // テスト用資源作成
            createTestResource();

            // トークン認証要求、クッキーを取得
            long issuedAt = new Date().getTime();
            String issuer = UrlUtils.cellRoot(TARGET_CELL);
            String subject = issuer + "#appuser";
            String target = UrlUtils.cellRoot(LOCAL_CELL);
            List<Role> roleList = new ArrayList<Role>();
            String schema = "";

            // 期限切れでないトークンを生成
            TransCellAccessToken validToken = new TransCellAccessToken(
                    issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                    issuer, subject, target, roleList, schema);
            // セルに対してトークン認証
            TResponse passRes = Http.request("authn/issue-cookie-with-saml.txt")
                    .with("remoteCell", LOCAL_CELL)
                    .with("assertion", validToken.toTokenString())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキーを改変してオペレーションが失敗することを確認する。
            TResponse res = requestOperation(LOCAL_CELL, cookiePeer,
                    "dc_cookie=" + cookie.get("dc_cookie") + "invalid", HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, LOCAL_CELL);
        } finally {
            // セル1とセル2を削除
            Setup.cellBulkDeletion(TARGET_CELL);
            Setup.cellBulkDeletion(LOCAL_CELL);
        }

    }

    /**
     * リフレッシュトークン認証でクッキー発行後＿クッキー認証で処理が受け付けられること.
     */
    @Test
    public final void リフレッシュトークン認証でクッキー発行後_クッキー認証で処理が受け付けられること() {

        // パスワード認証要求、クッキーを取得
        TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                "password2", "true", HttpStatus.SC_OK);

        String token = (String) passRes.bodyAsJson().get("refresh_token");

        // リフレッシュトークンを取得する。
        passRes = requestRefreshToken(TEST_CELL1, token, "true", HttpStatus.SC_OK);

        // クッキーを受け取る
        String header = passRes.getHeader("Set-Cookie");
        assertNotNull(header);

        Map<String, String> cookie = resolveCookie(header);

        String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNotNull(cookiePeer);

        // クッキーを与えてオペレーションを実行できるか確認する。
        requestOperation(TEST_CELL1, cookiePeer,
                "dc_cookie=" + cookie.get("dc_cookie"), HttpStatus.SC_OK);
    }

    /**
     * リフレッシュトークン認証でクッキー発行後＿クッキー内容を変更し処理が拒否されること.
     */
    @Test
    public final void リフレッシュトークン認証でクッキー発行後_クッキー内容を変更し処理が拒否されること() {

        // パスワード認証要求、クッキーを取得
        TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                "password2", "true", HttpStatus.SC_OK);

        String token = (String) passRes.bodyAsJson().get("refresh_token");

        // リフレッシュトークンを取得する。
        passRes = requestRefreshToken(TEST_CELL1, token, "true", HttpStatus.SC_OK);

        // クッキーを受け取る
        String header = passRes.getHeader("Set-Cookie");
        assertNotNull(header);

        Map<String, String> cookie = resolveCookie(header);

        String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
        assertNotNull(cookiePeer);

        // クッキーを改変してオペレーションが失敗することを確認する。
        TResponse res = requestOperation(TEST_CELL1, cookiePeer,
                "dc_cookie=" + cookie.get("dc_cookie") + "invalid", HttpStatus.SC_UNAUTHORIZED);
        AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);
    }

    /**
     * 正しいクッキー情報を使用してすべてのユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること.
     */
    @Test
    public final void 正しいクッキー情報を使用してすべてのユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            String path = String.format("%s/%s/%s", TEST_CELL1, boxName, colName);
            DavResourceUtils.setACLPrivilegeAllForAllUser(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    path, "none");

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                    "password2", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie, HttpStatus.SC_OK);
        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 不正なクッキー情報を使用してすべてのユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること.
     */
    @Test
    public final void 不正なクッキー情報を使用してすべてのユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            String path = String.format("%s/%s/%s", TEST_CELL1, boxName, colName);
            DavResourceUtils.setACLPrivilegeAllForAllUser(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    path, "none");

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                    "password2", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            // dc_cookieが不正
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie") + "invalid";
            requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie, HttpStatus.SC_OK);

            // dc_cookieが空
            requestOperationWithoutCookie(TEST_CELL1, boxName, colName, cookiePeer, HttpStatus.SC_OK);

            // dc_cookie_peerが不正
            dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            String dcCookiePeer = cookiePeer + "invalid";
            requestOperation(TEST_CELL1, boxName, colName, dcCookiePeer, dcCookie, HttpStatus.SC_OK);
        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }

    }

    /**
     * 正しいクッキー情報を使用して該当ユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること.
     */
    @Test
    public final void 正しいクッキー情報を使用して該当ユーザが操作可能なコレクションに対してアクセスした場合に処理が受付けられること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            DavResourceUtils.setACLwithRoleBaseUrl(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, boxName, colName,
                    "none", UrlUtils.roleResource(TEST_CELL1, Box.DEFAULT_BOX_NAME, "role2"), "<D:read/>",
                    HttpStatus.SC_OK);

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                    "password2", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie, HttpStatus.SC_OK);
        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 不正なクッキー情報を使用して該当ユーザが操作可能なコレクションに対してアクセスした場合に処理が401エラーとなること.
     */
    @Test
    public final void 不正なクッキー情報を使用して該当ユーザが操作可能なコレクションに対してアクセスした場合に処理が401エラーとなること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            DavResourceUtils.setACLwithRoleBaseUrl(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, boxName, colName,
                    "none", UrlUtils.roleResource(TEST_CELL1, Box.DEFAULT_BOX_NAME, "role2"), "<D:read/>",
                    HttpStatus.SC_OK);

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account2",
                    "password2", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            // dc_cookieが不正
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie") + "invalid";
            TResponse res = requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);

            // dc_cookieが空
            res = requestOperationWithoutCookie(TEST_CELL1, boxName, colName, cookiePeer, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);

            // dc_cookie_peerが不正
            dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            String dcCookiePeer = cookiePeer + "invalid";
            res = requestOperation(TEST_CELL1, boxName, colName, dcCookiePeer, dcCookie, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);

        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 正しいクッキー情報を使用して該当ユーザが操作不可能なコレクションに対してアクセスした場合に処理が403エラーとなること.
     */
    @Test
    public final void 正しいクッキー情報を使用して該当ユーザが操作不可能なコレクションに対してアクセスした場合に処理が403エラーとなること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            DavResourceUtils.setACLwithRoleBaseUrl(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, boxName, colName,
                    "none", UrlUtils.roleResource(TEST_CELL1, Box.DEFAULT_BOX_NAME, "role1"), "<D:read-properties/>",
                    HttpStatus.SC_OK);

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account1",
                    "password1", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            TResponse res = requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie,
                    HttpStatus.SC_FORBIDDEN);
            AuthTestCommon.checkAuthenticateHeaderNotExists(res);
        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 不正なクッキー情報を使用して該当ユーザが操作不可能なコレクションに対してアクセスした場合に処理が401エラーとなること.
     */
    @Test
    public final void 不正なクッキー情報を使用して該当ユーザが操作不可能なコレクションに対してアクセスした場合に処理が401エラーとなること() {
        final String boxName = Setup.TEST_BOX1;
        final String colName = "cookieTestCollection";

        try {
            // コレクション作成
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                    boxName, colName);

            // ACL作成
            DavResourceUtils.setACLwithRoleBaseUrl(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, boxName, colName,
                    "none", UrlUtils.roleResource(TEST_CELL1, Box.DEFAULT_BOX_NAME, "role1"), "<D:read-properties/>",
                    HttpStatus.SC_OK);

            // パスワード認証要求、クッキーを取得
            TResponse passRes = requestAuthentication(TEST_CELL1, "account1",
                    "password1", "true", HttpStatus.SC_OK);
            String cookiePeer = (String) passRes.bodyAsJson().get("dc_cookie_peer");
            assertNotNull(cookiePeer);

            // クッキーを受け取る
            String header = passRes.getHeader("Set-Cookie");
            assertNotNull(header);

            Map<String, String> cookie = resolveCookie(header);

            // クッキー認証
            // dc_cookieが不正
            String dcCookie = "dc_cookie=" + cookie.get("dc_cookie") + "invalid";
            TResponse res = requestOperation(TEST_CELL1, boxName, colName, cookiePeer, dcCookie,
                    HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);

            // dc_cookieが空
            res = requestOperationWithoutCookie(TEST_CELL1, boxName, colName, cookiePeer, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);
            // dc_cookie_peerが不正
            dcCookie = "dc_cookie=" + cookie.get("dc_cookie");
            String dcCookiePeer = cookiePeer + "invalid";
            res = requestOperation(TEST_CELL1, boxName, colName, dcCookiePeer, dcCookie, HttpStatus.SC_UNAUTHORIZED);
            AuthTestCommon.checkAuthenticateHeader(res, OAuth2Helper.Scheme.BEARER, TEST_CELL1);
        } finally {
            // コレクション削除
            DavResourceUtils.deleteCollection(TEST_CELL1, boxName, colName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    private TResponse requestOperation(String cellName, String cookiePeer, String cookie, int expectedStatus) {
        return requestOperation(cellName, "box1", "setodata", cookiePeer, cookie, expectedStatus);
    }

    private TResponse requestOperation(String cellName,
            String boxName,
            String colName,
            String cookiePeer,
            String cookie,
            int expectedStatus) {
        return Http.request("authn/testOperationWithCookie.txt")
                .with("cellPath", cellName)
                .with("boxName", boxName)
                .with("colName", colName)
                .with("dc_cookie_peer", cookiePeer)
                .with("cookie", cookie)
                .returns()
                .debug()
                .statusCode(expectedStatus);
    }

    private TResponse requestOperationWithoutCookie(String cellName,
            String boxName,
            String colName,
            String cookiePeer,
            int expectedStatus) {
        return Http.request("authn/testOperationWithCookie.txt")
                .with("cellPath", cellName)
                .with("boxName", boxName)
                .with("colName", colName)
                .with("dc_cookie_peer", cookiePeer)
                .returns()
                .debug()
                .statusCode(expectedStatus);
    }

    private TResponse requestAuthentication(String cellName, String userName,
            String password, String dcCookie, int expectedStatus) {
        TResponse passRes = null;
        if (null == dcCookie) {
            passRes =
                    Http.request("authn/password-cl-c0.txt")
                            .with("remoteCell", cellName)
                            .with("username", userName)
                            .with("password", password)
                            .returns()
                            .statusCode(expectedStatus);
        } else {
            passRes =
                    Http.request("authn/issue-cookie-without-target.txt")
                            .with("remoteCell", cellName)
                            .with("username", userName)
                            .with("password", password)
                            .with("dc_cookie", dcCookie)
                            .returns()
                            .statusCode(expectedStatus);
        }
        return passRes;
    }

    private TResponse requestRefreshToken(String cellName,
            String token,
            String dcCookie,
            int expectedStatus) {
        TResponse passRes =
                Http.request("authn/issue-cookie-for-refreshToken.txt")
                        .with("remoteCell", cellName)
                        .with("refresh_token", token)
                        .returns()
                        .statusCode(expectedStatus);
        return passRes;
    }

    private TResponse requestAuthorizationWithTarget(String cellName, String userName,
            String password, String dcCookie, String dcTarget, int code) {
        TResponse passRes = null;
        if (null == dcCookie) {
            passRes =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", cellName)
                            .with("username", userName)
                            .with("password", password)
                            .with("dc_target", dcTarget)
                            .returns()
                            .statusCode(code);
        } else {
            passRes =
                    Http.request("authn/issue-cookie-with-target.txt")
                            .with("remoteCell", cellName)
                            .with("username", userName)
                            .with("password", password)
                            .with("dc_cookie", dcCookie)
                            .with("dc_target", dcTarget)
                            .returns()
                            .statusCode(code);
        }
        return passRes;
    }

    private Map<String, String> resolveCookie(String original) {
        Map<String, String> cookie = new HashMap<String, String>();
        for (String item : original.split(";")) {
            String[] keyValue = item.split("=");
            if (0 == keyValue.length) {
                continue;
            } else if (1 == keyValue.length) {
                cookie.put(keyValue[0], null);
            } else {
                cookie.put(keyValue[0], keyValue[1]);
            }
        }
        return cookie;
    }

    /**
     * トランスセルトークン作成のための資源を作成する.
     * @throws UnsupportedEncodingException
     */
    private void createTestResource() throws UnsupportedEncodingException {
        // セル1作成
        CellUtils.create(LOCAL_CELL, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET,
                HttpStatus.SC_CREATED);
        // セル2作成
        CellUtils.create(TARGET_CELL, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET,
                HttpStatus.SC_CREATED);

        // セル2にAccount作成
        AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TARGET_CELL, "appuser", "apppassword",
                HttpStatus.SC_CREATED);

        // セル1に対してセル2のExtCell作成
        String url = UrlUtils.cellRoot(TARGET_CELL);
        ExtCellUtils.create(AbstractCase.MASTER_TOKEN_NAME, LOCAL_CELL, url, HttpStatus.SC_CREATED);

        // セル1に対してRole作成
        RoleUtils.create(LOCAL_CELL, AbstractCase.MASTER_TOKEN_NAME, "appadmin", HttpStatus.SC_CREATED);

        // セル1のRoleとExtCellを$links
        TResponse tresponse = null;
        String roleUrl = UrlUtils.roleUrl(LOCAL_CELL, null, "appadmin");
        tresponse = Http.request("cell/link-extCell-role.txt")
                .with("cellPath", LOCAL_CELL)
                .with("cellName", URLEncoder.encode(url, "UTF-8"))
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleUrl", roleUrl)
                .returns();
        tresponse.statusCode(HttpStatus.SC_NO_CONTENT);

        // セル1に対してACLを設定
        String boxUrl = UrlUtils.roleResource(LOCAL_CELL, "__", "");
        Http.request("cell/acl-setting-single-request.txt")
                .with("url", LOCAL_CELL)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", boxUrl)
                .with("role", "appadmin")
                .with("privilege", "<D:box/>")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // Cookie認証動作検証用にODataコレクションを作成
        BoxUtils.create(LOCAL_CELL, "box1", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, LOCAL_CELL,
                "box1", "setodata");
        DavResourceUtils.setACLwithRoleBaseUrl(AbstractCase.MASTER_TOKEN_NAME, LOCAL_CELL, "box1", "setodata",
                "none", UrlUtils.roleResource(LOCAL_CELL, Box.DEFAULT_BOX_NAME, "appadmin"), "<D:read />",
                HttpStatus.SC_OK);
    }

}

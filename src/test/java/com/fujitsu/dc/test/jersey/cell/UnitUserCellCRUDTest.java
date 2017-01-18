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
package com.fujitsu.dc.test.jersey.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token.TokenParseException;
import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.common.auth.token.UnitLocalUnitUserToken;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.cell.auth.AuthTestCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * UnitUserでCellをCRUDするテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UnitUserCellCRUDTest extends JerseyTest {

    private static final String UNIT_USER_CELL = "UnitUserCell";
    private static final String UNIT_USER_ACCOUNT = "UnitUserName";
    private static final String UNIT_ADMIN_ROLE = "unitAdmin";
    private static final String UNIT_USER_ACCOUNT_PASS = "password";
    private static final String CREATE_CELL = "createCell";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public UnitUserCellCRUDTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {

    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
    }

    /**
     * マスタートークンでX_Dc_UnitUserヘッダを指定すると指定したユニットユーザトークンになることを確認.
     */
    @Test
    public final void マスタートークンでX_Dc_UnitUserヘッダを指定すると指定したユニットユーザトークンになることを確認() {
        // マスタートークンでX-Dc-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
        CellUtils.create(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_CREATED);

        // ユニットユーザトークンを使えば取得可能なことを確認
        CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_VET, HttpStatus.SC_OK);

        // オーナーが異なるユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと削除できないことを確認
        CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, Setup.OWNER_HMC, HttpStatus.SC_FORBIDDEN);

        // オーナーが一致するユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと削除できることを確認
        CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, Setup.OWNER_VET,
                HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ユニットユーザートークンでセル作成を行いオーナーが設定されることを確認.
     */
    @Test
    public final void ユニットユーザートークンでセル作成を行いオーナーが設定されることを確認() {
        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("dc_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ユニットユーザートークンを使ってセル作成をするとオーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            // ユニットユーザートークンを使ってセル更新ができることを確認
            CellUtils.update(CREATE_CELL, CREATE_CELL, unitUserToken,
                    HttpStatus.SC_NO_CONTENT);

            // オーナーが異なるユニットユーザートークン（マスタートークンのヘッダ指定での降格を利用）を使ってセル更新ができないことを確認
            CellUtils.update(CREATE_CELL, CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, "hoge"), HttpStatus.SC_FORBIDDEN);

            // マスタートークンを使ってセル更新ができることを確認
            CellUtils.update(CREATE_CELL, CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NO_CONTENT);

            // ユニットユーザトークンを使えば取得可能なことを確認
            CellUtils.get(CREATE_CELL, unitUserToken, HttpStatus.SC_OK);

            // マスタートークンのオーナーヘッダ指定を使えば取得可能なことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, UNIT_USER_ACCOUNT), HttpStatus.SC_OK);

            // オーナーが異なるユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと取得できないことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, "hoge"), HttpStatus.SC_FORBIDDEN);

            // ユニットユーザートークンを使ってセル削除ができることを確認
            CellUtils.delete(unitUserToken, CREATE_CELL, HttpStatus.SC_NO_CONTENT);

        } finally {
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }

    /**
     * ユニットアドミンロールをもつユニットユーザートークンでセル作成を行いオーナーが設定されないことを確認.
     */
    @Test
    public final void ユニットアドミンロールをもつユニットユーザートークンでセル作成を行いオーナーが設定されないことを確認() {

        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);

            // ロール追加（ユニットアドミンロール）
            RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, null,
                    UNIT_ADMIN_ROLE, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRole(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, null, UNIT_ADMIN_ROLE, HttpStatus.SC_NO_CONTENT);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("dc_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ユニットユーザートークンを使ってセル作成をするとオーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            // UnitUserTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                    UrlUtils.subjectUrl(UNIT_USER_CELL, UNIT_USER_ACCOUNT),
                    UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null);

            // ユニットユーザトークンでは取得できないことを確認
            CellUtils.get(CREATE_CELL, tcat.toTokenString(), HttpStatus.SC_FORBIDDEN);

            // セルのオーナーが見指定のため、マスタートークンのオーナーヘッダ指定を使うと取得不可なことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, UNIT_USER_ACCOUNT), HttpStatus.SC_FORBIDDEN);

            // オーナーが設定されていないのでマスタートークンのみアクセス可能
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        } finally {
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            ResourceUtils.linkAccountRollDelete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, null, UNIT_ADMIN_ROLE);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, null, UNIT_ADMIN_ROLE);
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
        }
    }

    /**
     * パスワード認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void パスワード認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認() throws TokenParseException {

        try {
            // アカウントにユニット昇格権限付与
            DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

            // パスワード認証でのユニット昇格
            Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(Setup.TEST_CELL1, "account1");
            TResponse res = Http.request("authnUnit/password-uluut.txt")
                    .with("remoteCell", Setup.TEST_CELL1)
                    .with("username", "account1")
                    .with("password", "password1")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            AuthTestCommon.accountLastAuthenticatedCheck(Setup.TEST_CELL1, "account1", lastAuthenticatedTime);

            JSONObject json = res.bodyAsJson();
            String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // トークンの中身の検証
            UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(uluutString, UrlUtils.getHost());
            assertEquals(Setup.OWNER_VET, uluut.getSubject());
            assertEquals(UrlUtils.getHost(), uluut.getIssuer());

            // ユニットローカルユニットユーザトークンを使ってセル作成をするとオーナーがユニットユーザー（testcell1のオーナー）になる。
            CellUtils.create(CREATE_CELL, uluutString, HttpStatus.SC_CREATED);

            // ユニットユーザトークンを使えば取得可能なことを確認
            CellUtils.get(CREATE_CELL, uluutString, HttpStatus.SC_OK);

            // マスタートークンのオーナーヘッダ指定を使えば取得可能なことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.OWNER_VET, HttpStatus.SC_OK);

            // オーナーが異なるユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと取得できないことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.OWNER_HMC, HttpStatus.SC_FORBIDDEN);
        } finally {
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
        }
    }

    /**
     * 昇格設定のないアカウントがパスワード認証でユニットローカルユニットユーザトークンへ昇格できないことの確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void 昇格設定のないアカウントがパスワード認証でユニットローカルユニットユーザトークンへ昇格できないことの確認() throws TokenParseException {
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // パスワード認証でのユニット昇格してみる（account1、account2は許可権限あるけどaccount3にはない）
        Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account3")
                .with("password", "password3")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ
    }

    /**
     * リフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void リフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String refresh = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // リフレッシュトークン認証でのユニット昇格
        TResponse res2 = Http.request("authnUnit/refresh-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("refresh_token", refresh)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String uluutString = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // トークンの中身の検証
        UnitLocalUnitUserToken uluut = UnitLocalUnitUserToken.parse(uluutString, UrlUtils.getHost());
        assertEquals(Setup.OWNER_VET, uluut.getSubject());
        assertEquals(UrlUtils.getHost(), uluut.getIssuer());
    }

    /**
     * 昇格設定のないアカウントがリフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格できないことの確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void 昇格設定のないアカウントがリフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格できないことの確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account3")
                .with("password", "password3")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String refresh = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // リフレッシュ認証でのユニット昇格してみる（account1、account2は許可権限あるけどaccount3にはない）
        Http.request("authnUnit/refresh-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("refresh_token", refresh)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * トランスセルトークンではセルの昇格ができないことの確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void トランスセルトークンではセルの昇格ができないことの確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL2)
                .with("username", "account1")
                .with("password", "password1")
                .with("dc_target", UrlUtils.cellRoot(Setup.TEST_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // トークン認証でのユニット昇格してみる(トークン認証での昇格はエラーになる)
        Http.request("authnUnit/saml-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("assertion", tokenStr)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * 他人セルリフレッシュトークンではセルの昇格ができないことの確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void 他人セルリフレッシュトークンではセルの昇格ができないことの確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL2)
                .with("username", "account1")
                .with("password", "password1")
                .with("dc_target", UrlUtils.cellRoot(Setup.TEST_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // 下のテストで使うリフレッシュトークンを取得するためにトークン認証
        TResponse res2 = Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("assertion", tokenStr)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String refresh = (String) json2.get(OAuth2Helper.Key.REFRESH_TOKEN);
        // リフレッシュトークン認証でのユニット昇格してみる(トークン認証での昇格はエラーになる)
        Http.request("authnUnit/refresh-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("refresh_token", refresh)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * オーナーが未設定のセルの昇格が認証エラーになることの確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void オーナーが未設定のセルの昇格が認証エラーになることの確認() throws TokenParseException {
        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    "account1", "password1", HttpStatus.SC_CREATED);

            // アカウントにユニット昇格権限付与
            DavResourceUtils.setProppatch(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

            // パスワード認証でのユニット昇格が認証エラーになることを確認
            Http.request("authnUnit/password-uluut.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", "account1")
                    .with("password", "password1")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
            AuthTestCommon.waitForAccountLock(); // アカウントロック回避用にスリープ

        } finally {
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    "account1", -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }

    /**
     * セルレベルPROPPATCHをユニットユーザトークンで実行可能なことを確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void セルレベルPROPPATCHをユニットユーザトークンで実行可能なことを確認() throws TokenParseException {
        // UnitUserTokenを自作
        TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_VET, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null);

        String unitUserToken = tcat.toTokenString();

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, unitUserToken,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * セルレベルPROPPATCHをオーナーの違うユニットユーザトークンでは実行不可なことを確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void セルレベルPROPPATCHをオーナーの違うユニットユーザトークンでは実行不可なことを確認() throws TokenParseException {
        // UnitUserTokenを自作
        TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_HMC, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null);

        String unitUserToken = tcat.toTokenString();

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, unitUserToken,
                "cell/proppatch-uluut.txt", HttpStatus.SC_FORBIDDEN);
    }

    /**
     * セルレベルPROPPATCHをユニットローカルユニットユーザトークンで実行可能なことを確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void セルレベルPROPPATCHをユニットローカルユニットユーザトークンで実行可能なことを確認() throws TokenParseException {
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // パスワード認証でのユニット昇格
        TResponse res = Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, uluutString,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * セルレベルPROPPATCHをオーナーが違うユニットローカルユニットユーザトークンで実行不可なことを確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void セルレベルPROPPATCHをオーナーが違うユニットローカルユニットユーザトークンで実行不可なことを確認() throws TokenParseException {
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // パスワード認証でのユニット昇格
        TResponse res = Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL2, uluutString,
                "cell/proppatch-uluut.txt", HttpStatus.SC_FORBIDDEN);
    }

    /**
     * ユニットローカルユニットユーザトークンでオーナーによる実行判断の確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void ユニットローカルユニットユーザトークンでオーナーによる実行判断の確認() throws TokenParseException {
        // アカウントにユニット昇格権限付与
        DavResourceUtils.setProppatch(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                "cell/proppatch-uluut.txt", HttpStatus.SC_MULTI_STATUS);

        // パスワード認証でのユニット昇格
        TResponse res = Http.request("authnUnit/password-uluut.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String uluutString = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルレベルPROPFIND 非オーナーのため、アクセス不可
        CellUtils.propfind(Setup.TEST_CELL2 + "/", uluutString, "0", HttpStatus.SC_FORBIDDEN);

        // セルレベルPROPFIND オーナーのため、アクセス可能
        CellUtils.propfind(Setup.TEST_CELL1 + "/", uluutString, "0", HttpStatus.SC_MULTI_STATUS);

        // ボックスレベルPROPFIND 非オーナーのため、アクセス不可
        CellUtils.propfind(Setup.TEST_CELL2 + "/" + Setup.TEST_BOX1 + "/", uluutString,
                "0", HttpStatus.SC_FORBIDDEN);

        // ボックスレベルPROPFIND オーナーのため、アクセス可能
        CellUtils.propfind(Setup.TEST_CELL1 + "/" + Setup.TEST_BOX1 + "/", uluutString,
                "0", HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * ユニットユーザトークンでオーナーによる実行判断の確認.
     */
    @Test
    public final void ユニットユーザトークンでオーナーによる実行判断の確認() {

        String boxName = "createCellBox";
        try {

            // UnitUserTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                    Setup.OWNER_VET, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null);

            String unitUserToken = tcat.toTokenString();

            // ユニットユーザートークンを使ってセル作成をするとオーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            // ボックスレベルPROPFIND用のボックス作成
            BoxUtils.create(CREATE_CELL, boxName, unitUserToken);

            // セルレベルPROPFIND 非オーナーのため、アクセス不可
            CellUtils.propfind(Setup.TEST_CELL2 + "/", unitUserToken, "0", HttpStatus.SC_FORBIDDEN);

            // セルレベルPROPFIND オーナーのため、アクセス可能
            CellUtils.propfind(CREATE_CELL + "/", unitUserToken, "0", HttpStatus.SC_MULTI_STATUS);

            // ボックスレベルPROPFIND 非オーナーのため、アクセス不可
            CellUtils.propfind(Setup.TEST_CELL2 + "/" + Setup.TEST_BOX1 + "/", unitUserToken,
                    "0", HttpStatus.SC_FORBIDDEN);

            // ボックスレベルPROPFIND オーナーのため、アクセス可能
            CellUtils.propfind(CREATE_CELL + "/" + boxName + "/", unitUserToken,
                    "0", HttpStatus.SC_MULTI_STATUS);
        } finally {
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, -1);
            // 本テスト用ボックスの削除
            BoxUtils.delete(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
        }
    }

    /**
     * セルの検索でオーナーが一致するものだけ検索できることの確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void セルの検索でオーナーが一致するものだけ検索できることの確認() throws TokenParseException {
        // VETをオーナーにもつUnitUserTokenを自作
        TransCellAccessToken tcatvet = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_VET, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null);

        // ユニットユーザトークンではオーナーが一致するセルのみ検索できることの確認（vetをオーナーに持つのはsetupで作っているtestcell1,schema1のみの想定）
        TResponse tcatget = CellUtils.list(tcatvet.toTokenString(), HttpStatus.SC_OK);
        JSONObject tcatgetJson = (JSONObject) tcatget.bodyAsJson().get("d");
        JSONArray tcatgetJson2 = (JSONArray) tcatgetJson.get("results");
        assertEquals(2, tcatgetJson2.size());

        // マスタートークンでxDcUnitUserヘッダ指定はヘッダで指定したオーナーのセルのみ検索できることを確認(hmcをオーナーに持つのはSetupで作っているtestcell2,schema2のみの想定)
        TResponse xdcmtget = CellUtils.list(AbstractCase.MASTER_TOKEN_NAME, Setup.OWNER_HMC, HttpStatus.SC_OK);
        JSONObject xdcmtgetJson = (JSONObject) xdcmtget.bodyAsJson().get("d");
        JSONArray xdcmtgetJson2 = (JSONArray) xdcmtgetJson.get("results");
        assertEquals(2, xdcmtgetJson2.size());

        // マスタートークンではオーナーにかかわらず全てのセルが取得できることの確認
        TResponse mtget = CellUtils.list(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        JSONObject mtgetJson = (JSONObject) mtget.bodyAsJson().get("d");
        JSONArray mtgetJson2 = (JSONArray) mtgetJson.get("results");
        assertTrue(mtgetJson2.size() > 2);
    }

    /**
     * アクセストークンではセル作成ができないことを確認.
     */
    @Test
    public final void アクセストークンではセル作成ができないことを確認() {
        // パスワード認証
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("dc_target", UrlUtils.cellRoot(Setup.TEST_CELL2))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String token = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アクセストークンではセルの作成ができないことを確認。
        CellUtils.create(CREATE_CELL, token, HttpStatus.SC_FORBIDDEN);
    }
}

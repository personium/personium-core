/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.test.jersey.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.cell.auth.AuthTestCommon;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Test of UnitUser.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UnitUserCellTest extends PersoniumTest {

    private static Logger log = LoggerFactory.getLogger(UnitUserCellTest.class);

    private static final String UNIT_USER_CELL = "unitusercell";
    private static final String UNIT_USER_ACCOUNT = "UnitUserName";
    private static final String UNIT_USER_ACCOUNT_PASS = "password";
    private static final String CREATE_CELL = "createcell";

    private static String unitAdminRole;
    private static String contentsReaderRole;
    private static String contentsAdminRole;

    /** unitUser.issuers in properties. */
    private static String issuersBackup = "";
    private static String urlModeBackup = "";

    /**
     * Constructor. テスト対象のパッケージをsuperに渡す必要がある
     */
    public UnitUserCellTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Befor class.
     * @throws Exception Unintended exception in test
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Override issuers in unitconfig.
        urlModeBackup = PersoniumUnitConfig.get(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED);
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED,
                "true");
        issuersBackup = PersoniumUnitConfig.get(PersoniumUnitConfig.UNIT_USER_ISSUERS);
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_USER_ISSUERS,
                SCHEME_LOCALUNIT + ":" + UNIT_USER_CELL + ":/");

        // Read role name from AccessContext
        Field admin = AccessContext.class.getDeclaredField("ROLE_UNIT_ADMIN");
        admin.setAccessible(true);
        unitAdminRole = (String) admin.get(null);
        Field contentsReader = AccessContext.class.getDeclaredField("ROLE_CELL_CONTENTS_READER");
        contentsReader.setAccessible(true);
        contentsReaderRole = (String) contentsReader.get(null);
        Field contentsAdmin = AccessContext.class.getDeclaredField("ROLE_CELL_CONTENTS_ADMIN");
        contentsAdmin.setAccessible(true);
        contentsAdminRole = (String) contentsAdmin.get(null);
    }

    /**
     * After class.
     */
    @AfterClass
    public static void afterClass() {
        // Restore issuers in unitconfig.
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_USER_ISSUERS,
                issuersBackup != null ? issuersBackup : ""); // CHECKSTYLE IGNORE
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, urlModeBackup);
    }

    /**
     * マスタートークンでX_PERSONIUM_UnitUserヘッダを指定すると指定したユニットユーザトークンになることを確認.
     */
    @Test
    public void マスタートークンでX_PERSONIUM_UnitUserヘッダを指定すると指定したユニットユーザトークンになることを確認() {
        // マスタートークンでX-Personium-UnitUserヘッダを指定すると指定した値のOwnerでセルが作成される。
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
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public void ユニットユーザートークンでセル作成を行いオーナーが設定されることを確認() throws TokenParseException, TokenDsigException, TokenRootCrtException {
        try {
            // 本テスト用 Unit User Cell の作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, -1);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS,  -1);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            //
            TransCellAccessToken tcToken = TransCellAccessToken.parse(unitUserToken);
            String subject = tcToken.getSubject();
            log.info("##TOKEN##");
            log.info("Subject: "+ subject);
            log.info("Issuer : "+ tcToken.getSubject());
            log.info("Target : "+ tcToken.getTarget());
            String localunitSubject = UriUtils.convertSchemeFromHttpToLocalUnit(subject);
            log.info("Owner Should be : "+ localunitSubject);

            // ユニットユーザートークンを使ってセル作成をする.
            //   オーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            Cell cell = ModelFactory.cellFromName(CREATE_CELL);
            String owner = cell.getOwnerRaw();
            log.info(" OWNER = " + owner);
            assertEquals(localunitSubject, owner);


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
     * ユニットユーザートークンでセル作成を行いオーナーとして各種処理が可能なことを確認.
     * @throws TokenRootCrtException
     * @throws TokenDsigException
     * @throws TokenParseException
     */
    @Test
    public void ユニットユーザートークンでセル作成を行いオーナーとして各種処理が可能なことを確認() throws TokenParseException, TokenDsigException, TokenRootCrtException {
        try {
            // 本テスト用 Unit User Cell の作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // AddAccount
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ユニットユーザートークンを使ってセル作成をする.
            //   オーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
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
    public void ユニットアドミンロールをもつユニットユーザートークンでセル作成を行いオーナーが設定されないことを確認() {

        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);

            // ロール追加（ユニットアドミンロール）
            RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, unitAdminRole,
                    null, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            LinksUtils.createLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null, Role.EDM_TYPE_NAME,
                    unitAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ユニットユーザートークンを使ってセル作成をするとオーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            // UnitUserTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                    UrlUtils.subjectUrl(UNIT_USER_CELL, UNIT_USER_ACCOUNT),
                    UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null, null);

            // ユニットユーザトークンでは取得できないことを確認
            CellUtils.get(CREATE_CELL, tcat.toTokenString(), HttpStatus.SC_FORBIDDEN);

            // セルのオーナーが見指定のため、マスタートークンのオーナーヘッダ指定を使うと取得不可なことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, UNIT_USER_ACCOUNT), HttpStatus.SC_FORBIDDEN);

            // オーナーが設定されていないのでマスタートークンのみアクセス可能
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
        } finally {
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            LinksUtils.deleteLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null,
                    Role.EDM_TYPE_NAME, unitAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, unitAdminRole, null);
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
    @Ignore // UUT promotion setting API invalidation.
    public void パスワード認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認() throws TokenParseException {

        try {
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
    @Ignore // UUT promotion setting API invalidation.
    public void 昇格設定のないアカウントがパスワード認証でユニットローカルユニットユーザトークンへ昇格できないことの確認() throws TokenParseException {
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
        AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ
    }

    /**
     * リフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認. setupのtestcell1がある前提.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    @Ignore // UUT promotion setting API invalidation.
    public void リフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格したトークンでセル作成を行いオーナーが設定されることを確認() throws TokenParseException {
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
    @Ignore // UUT promotion setting API invalidation.
    public void 昇格設定のないアカウントがリフレッシュトークン認証でユニットローカルユニットユーザトークンへ昇格できないことの確認() throws TokenParseException {
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
    @Ignore // UUT promotion setting API invalidation.
    public void トランスセルトークンではセルの昇格ができないことの確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL2)
                .with("username", "account1")
                .with("password", "password1")
                .with("p_target", UrlUtils.cellRoot(Setup.TEST_CELL1))
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
    @Ignore // UUT promotion setting API invalidation.
    public void 他人セルリフレッシュトークンではセルの昇格ができないことの確認() throws TokenParseException {
        // 認証（パスワード認証）
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL2)
                .with("username", "account1")
                .with("password", "password1")
                .with("p_target", UrlUtils.cellRoot(Setup.TEST_CELL1))
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
    @Ignore // UUT promotion setting API invalidation.
    public void オーナーが未設定のセルの昇格が認証エラーになることの確認() throws TokenParseException {
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
            AuthTestCommon.waitForIntervalLock(); // アカウントロック回避用にスリープ

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
    @Ignore // UUT promotion setting API invalidation.
    public void セルレベルPROPPATCHをユニットユーザトークンで実行可能なことを確認() throws TokenParseException {
        // UnitUserTokenを自作
        TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_VET, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null, null);

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
    @Ignore // UUT promotion setting API invalidation.
    public void セルレベルPROPPATCHをオーナーの違うユニットユーザトークンでは実行不可なことを確認() throws TokenParseException {
        // UnitUserTokenを自作
        TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_HMC, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null, null);

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
    @Ignore // UUT promotion setting API invalidation.
    public void セルレベルPROPPATCHをユニットローカルユニットユーザトークンで実行可能なことを確認() throws TokenParseException {
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
    @Ignore // UUT promotion setting API invalidation.
    public void セルレベルPROPPATCHをオーナーが違うユニットローカルユニットユーザトークンで実行不可なことを確認() throws TokenParseException {
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
    @Ignore // UUT promotion setting API invalidation.
    public void ユニットローカルユニットユーザトークンでオーナーによる実行判断の確認() throws TokenParseException {
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
    public void ユニットユーザトークンでオーナーによる実行判断の確認() {

        String boxName = "createCellBox";
        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // アカウント追加
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);
            // ロール追加（ContentsAdminRole）
            RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsAdminRole,
                    null, HttpStatus.SC_CREATED);
            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            LinksUtils.createLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null, Role.EDM_TYPE_NAME,
                    contentsAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // 認証（ユニットユーザートークン取得）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ユニットユーザートークンを使ってセル作成をするとオーナーがユニットユーザー（ここだとuserNameアカウントのURL）になるはず。
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);

            // ボックスレベルPROPFIND用のボックス作成
            BoxUtils.create(CREATE_CELL, boxName, unitUserToken);

            // UnitLevel GetCell other owner
            CellUtils.get(Setup.TEST_CELL2, unitUserToken, HttpStatus.SC_FORBIDDEN);

            // UnitLevel GetCell owner
            CellUtils.get(CREATE_CELL, unitUserToken, HttpStatus.SC_OK);

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
            // ロール結びつけ削除
            LinksUtils.deleteLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null,
                    Role.EDM_TYPE_NAME, contentsAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // ロール削除
            RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsAdminRole, null);
            // アカウント削除
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UNIT_USER_ACCOUNT, -1);

            // 本テスト用ボックスの削除
            BoxUtils.delete(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
        }
    }

    /**
     * セルの検索でオーナーが一致するものだけ検索できることの確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public void セルの検索でオーナーが一致するものだけ検索できることの確認() throws TokenParseException {
        // VETをオーナーにもつUnitUserTokenを自作
        TransCellAccessToken tcatvet = new TransCellAccessToken(UrlUtils.cellRoot(UNIT_USER_CELL),
                Setup.OWNER_VET, UrlUtils.getBaseUrl() + "/", new ArrayList<Role>(), null, null);

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
    public void アクセストークンではセル作成ができないことを確認() {
        // パスワード認証
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("p_target", UrlUtils.cellRoot(Setup.TEST_CELL2))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String token = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アクセストークンではセルの作成ができないことを確認。
        CellUtils.create(CREATE_CELL, token, HttpStatus.SC_FORBIDDEN);
    }

    /**
     * Normal test.
     * Confirm permissions of unituser.
     */
    @Test
    public void normal_permission_of_unituser() {
        String boxName = "createCellBox";
        String collectionName = "createCellCollection";
        try {
            // Create unit user cell.
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account.
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);
            // Get token.
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // UnitLevel Create cell.
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);
            // UnitLevel Get cell.
            CellUtils.get(CREATE_CELL, unitUserToken, HttpStatus.SC_OK);

            // CellLevel Create box.
            BoxUtils.create(CREATE_CELL, boxName, unitUserToken, HttpStatus.SC_FORBIDDEN);
            BoxUtils.create(CREATE_CELL, boxName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // CellLevel Get box.
            BoxUtils.get(CREATE_CELL, unitUserToken, boxName, HttpStatus.SC_FORBIDDEN);

            // BoxLevel Create collection.
            DavResourceUtils.createODataCollection(
                    unitUserToken, HttpStatus.SC_FORBIDDEN, CREATE_CELL, boxName, collectionName);
            DavResourceUtils.createODataCollection(
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, CREATE_CELL, boxName, collectionName);
            // BoxLevel Propfind collection.
            DavResourceUtils.propfind(
                    unitUserToken, CREATE_CELL, boxName + "/" + collectionName, "0", HttpStatus.SC_FORBIDDEN);
        } finally {
            // Delete collection.
            DavResourceUtils.deleteCollection(CREATE_CELL, boxName, collectionName, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete box.
            BoxUtils.delete(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
            // Delete cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);

            // Delete unituser account.
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_ACCOUNT, -1);
            // Delete unituser cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }

    /**
     * Normal test.
     * Confirm permissions of contents reader role.
     */
    @Test
    public void normal_permission_of_contents_reader_role() {
        String boxName = "createCellBox";
        String collectionName = "createCellCollection";
        try {
            // Create unit user cell.
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account.
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);
            // Create role.(ContentsReaderRole)
            RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsReaderRole,
                    null, HttpStatus.SC_CREATED);
            // Link account-role.
            LinksUtils.createLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null, Role.EDM_TYPE_NAME,
                    contentsReaderRole, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // Get token.
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // UnitLevel Create cell.
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);
            // UnitLevel Get cell.
            CellUtils.get(CREATE_CELL, unitUserToken, HttpStatus.SC_OK);

            // CellLevel Create box.
            BoxUtils.create(CREATE_CELL, boxName, unitUserToken, HttpStatus.SC_FORBIDDEN);
            BoxUtils.create(CREATE_CELL, boxName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // CellLevel Get box.
            BoxUtils.get(CREATE_CELL, unitUserToken, boxName, HttpStatus.SC_OK);

            // BoxLevel Create collection.
            DavResourceUtils.createODataCollection(
                    unitUserToken, HttpStatus.SC_FORBIDDEN, CREATE_CELL, boxName, collectionName);
            DavResourceUtils.createODataCollection(
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, CREATE_CELL, boxName, collectionName);
            // BoxLevel Propfind collection.
            DavResourceUtils.propfind(
                    unitUserToken, CREATE_CELL, boxName + "/" + collectionName, "0", HttpStatus.SC_MULTI_STATUS);
        } finally {
            // Delete collection.
            DavResourceUtils.deleteCollection(CREATE_CELL, boxName, collectionName, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete box.
            BoxUtils.delete(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
            // Delete cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);

            // Delete unituser link.
            LinksUtils.deleteLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null,
                    Role.EDM_TYPE_NAME, contentsReaderRole, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete unituser role.
            RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsReaderRole, null, -1);
            // Delete unituser account.
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_ACCOUNT, -1);
            // Delete unituser cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }

    /**
     * Normal test.
     * Confirm permissions of contents admin role.
     */
    @Test
    public void normal_permission_of_contents_admin_role() {
        String boxName = "createCellBox";
        String collectionName = "createCellCollection";
        try {
            // Create unit user cell.
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account.
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL,
                    UNIT_USER_ACCOUNT, UNIT_USER_ACCOUNT_PASS, HttpStatus.SC_CREATED);
            // Create role.(ContentsAdminRole)
            RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsAdminRole,
                    null, HttpStatus.SC_CREATED);
            // Link account-role.
            LinksUtils.createLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null, Role.EDM_TYPE_NAME,
                    contentsAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // Get token.
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", UNIT_USER_CELL)
                    .with("username", UNIT_USER_ACCOUNT)
                    .with("password", UNIT_USER_ACCOUNT_PASS)
                    .with("p_target", UrlUtils.unitRoot())
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            JSONObject json = res.bodyAsJson();
            String unitUserToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // UnitLevel Create cell.
            CellUtils.create(CREATE_CELL, unitUserToken, HttpStatus.SC_CREATED);
            // UnitLevel Get cell.
            CellUtils.get(CREATE_CELL, unitUserToken, HttpStatus.SC_OK);

            // CellLevel Create box.
            BoxUtils.create(CREATE_CELL, boxName, unitUserToken, HttpStatus.SC_CREATED);
            // CellLevel Get box.
            BoxUtils.get(CREATE_CELL, unitUserToken, boxName, HttpStatus.SC_OK);

            // BoxLevel Create collection.
            DavResourceUtils.createODataCollection(
                    unitUserToken, HttpStatus.SC_CREATED, CREATE_CELL, boxName, collectionName);
            // BoxLevel Propfind collection.
            DavResourceUtils.propfind(
                    unitUserToken, CREATE_CELL, boxName + "/" + collectionName, "0", HttpStatus.SC_MULTI_STATUS);
        } finally {
            // Delete collection.
            DavResourceUtils.deleteCollection(CREATE_CELL, boxName, collectionName, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete box.
            BoxUtils.delete(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, boxName, -1);
            // Delete cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);

            // Delete unituser link.
            LinksUtils.deleteLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null,
                    Role.EDM_TYPE_NAME, contentsAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete unituser role.
            RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, contentsAdminRole, null, -1);
            // Delete unituser account.
            AccountUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_ACCOUNT, -1);
            // Delete unituser cell.
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }
    }
}

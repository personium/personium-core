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
package com.fujitsu.dc.test.jersey.cell.auth.token;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.AbstractOAuth2Token;
import com.fujitsu.dc.common.auth.token.AccountAccessToken;
import com.fujitsu.dc.common.auth.token.CellLocalAccessToken;
import com.fujitsu.dc.common.auth.token.CellLocalRefreshToken;
import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.common.auth.token.TransCellRefreshToken;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * トークンのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class TokenTest extends JerseyTest {
    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_APP_CELL1 = "schema1";

    static final String DAV_COLLECTION = "setdavcol/";
    static final String DAV_RESOURCE = "dav.txt";

    static final int MILLISECS_IN_AN_MINITE = 60 * 1000;

    /**
     * コンストラクタ.
     */
    public TokenTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * トークン認証_トランスセルトークンの期限が切れていたら認証エラーになること.
     */
    @Test
    public final void トークン認証_トランスセルトークンの期限が切れていたら認証エラーになること() {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = issuer + "#account1";
        String target = UrlUtils.cellRoot(TEST_CELL2);
        List<Role> roleList = new ArrayList<Role>();
        String schema = "";

        // 期限切れでないトークンを生成
        TransCellAccessToken validToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // セルに対してトークン認証
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切れのトークンを生成
        TransCellAccessToken invalidToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // セルに対してトークン認証
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", invalidToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * リフレッシュトークン認証_パスワード認証時に払い出されたリフレッシュトークン期限切れの場合認証エラーになること.
     */
    @Test
    public final void リフレッシュトークン認証_パスワード認証時に払い出されたリフレッシュトークン期限切れの場合認証エラーになること() {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = issuer + "#account1";
        String schema = "";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        CellLocalRefreshToken validToken = new CellLocalRefreshToken(
                issuedAt - AbstractOAuth2Token.SECS_IN_AN_DAY * 1000 + MILLISECS_IN_AN_MINITE,
                issuer, subject, schema);

        // アプリセルに対して認証
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL1)
                .with("refresh_token", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切れのトークンを生成する（IT環境の通信時間を考慮して１分余裕を持たせる）
        CellLocalRefreshToken invalidToken = new CellLocalRefreshToken(
                issuedAt - AbstractOAuth2Token.SECS_IN_AN_DAY * 1000 - MILLISECS_IN_AN_MINITE, issuer, subject, schema);
        // アプリセルに対して認証
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL1)
                .with("refresh_token", invalidToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * リフレッシュトークン認証_トークン認証時に払い出されたリフレッシュトークンの期限切れの場合認証エラーになること.
     */
    @Test
    public final void リフレッシュトークン認証_トランスセル認証リフレッシュトークン期限切れのテスト() {
        String id = "";
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL2);
        String origIssuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = origIssuer + "#account1";
        List<Role> origRoleList = new ArrayList<Role>();
        String schema = "";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellRefreshToken validToken = new TransCellRefreshToken(
                id, issuedAt - AbstractOAuth2Token.SECS_IN_AN_DAY * 1000 + MILLISECS_IN_AN_MINITE,
                issuer, subject, origIssuer, origRoleList, schema);
        // Refresh
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL2)
                .with("refresh_token", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellRefreshToken invalidToken = new TransCellRefreshToken(
                id, issuedAt - AbstractOAuth2Token.SECS_IN_AN_DAY * 1000 - MILLISECS_IN_AN_MINITE,
                issuer, subject, origIssuer, origRoleList, schema);
        // Refresh
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL2)
                .with("refresh_token", invalidToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * スキーマ認証_スキーマトークンの期限切れの場合認証エラーになること.
     */
    @Test
    public final void スキーマ認証_スキーマトークンの期限切れの場合認証エラーになること() {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_APP_CELL1);
        String subject = issuer + "#account1";
        String target = UrlUtils.cellRoot(TEST_CELL1);
        List<Role> roleList = new ArrayList<Role>();
        String schema = "";

        String account = "account1";
        String pass = "password1";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellAccessToken validToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // セルに対してトークン認証
        Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", account)
                .with("password", pass)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切のトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellAccessToken invalidToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // セルに対してトークン認証
        Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", account)
                .with("password", pass)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", invalidToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * データアクセス_自分セルローカルトークンの期限切れの場合認証エラーになること.
     */
    @Test
    public final void データアクセス_自分セルローカルトークンの期限切れの場合認証エラーになること() {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = "account2";
        String schema = "";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        AccountAccessToken validToken = new AccountAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, schema);
        // データアクセス
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        AccountAccessToken invalidToken = new AccountAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, schema);
        // データアクセス
        ResourceUtils.retrieve(invalidToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);
    }

    /**
     * データアクセス_他人セルローカルトークンの期限切れの場合認証エラーになること.
     * @throws MalformedURLException URLパースエラー
     */
    @Test
    public final void データアクセス_他人セルローカルトークンの期限切れの場合認証エラーになること() throws MalformedURLException {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = issuer + "account2";
        List<Role> roleList = new ArrayList<Role>();
        Role role = new Role(new URL(UrlUtils.roleResource(TEST_CELL1, "__", "role2")));
        roleList.add(role);
        String schema = "";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        CellLocalAccessToken validToken = new CellLocalAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, roleList, schema);
        // データアクセス
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        CellLocalAccessToken invalidToken = new CellLocalAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, roleList, schema);
        // データアクセス
        ResourceUtils.retrieve(invalidToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);
    }

    /**
     * データアクセス_トランスセルトークンの期限切れの場合認証エラーになること.
     * @throws MalformedURLException URLパースエラー
     */
    @Test
    public final void データアクセス_トランスセルトークンの期限切れの場合認証エラーになること() throws MalformedURLException {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = issuer + "#account2";
        String target = UrlUtils.cellRoot(TEST_CELL2);
        List<Role> roleList = new ArrayList<Role>();
        Role role = new Role(new URL(UrlUtils.roleResource(TEST_CELL1, "__", "role2")));
        roleList.add(role);
        String schema = "";

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellAccessToken validToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // データアクセス
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_OK, TEST_CELL2, Setup.TEST_BOX1);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        TransCellAccessToken invalidToken = new TransCellAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, target, roleList, schema);
        // データアクセス
        ResourceUtils.retrieve(invalidToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_UNAUTHORIZED, TEST_CELL2, Setup.TEST_BOX1);
    }
}

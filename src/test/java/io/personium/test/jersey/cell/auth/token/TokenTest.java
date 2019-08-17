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
package io.personium.test.jersey.cell.auth.token;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;

/**
 * Access Token Acceptance test.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class TokenTest extends PersoniumTest {
    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_APP_CELL1 = "schema1";

    static final String DAV_COLLECTION = "setdavcol/";
    static final String DAV_RESOURCE = "dav.txt";

    static final int MILLISECS_IN_AN_MINITE = 60 * 1000;

    /**
     * Constructor.
     */
    public TokenTest() {
        super(new PersoniumCoreApplication());
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
        ResidentRefreshToken validToken = new ResidentRefreshToken(
                issuedAt - AbstractOAuth2Token.SECS_IN_A_DAY * 1000 + MILLISECS_IN_AN_MINITE,
                issuer, subject, schema, new String[] {"scope1", "scope2"});

        // アプリセルに対して認証
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL1)
                .with("refresh_token", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切れのトークンを生成する（IT環境の通信時間を考慮して１分余裕を持たせる）
        ResidentRefreshToken invalidToken = new ResidentRefreshToken(
                issuedAt - AbstractOAuth2Token.SECS_IN_A_DAY * 1000 - MILLISECS_IN_AN_MINITE, issuer, subject,
                schema,  new String[] {"scope1", "scope2", "scope3"});
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
        VisitorRefreshToken validToken = new VisitorRefreshToken(
                id, issuedAt - AbstractOAuth2Token.SECS_IN_A_DAY * 1000 + MILLISECS_IN_AN_MINITE,
                issuer, subject, origIssuer, origRoleList, schema);
        // Refresh
        Http.request("authn/refresh-cl.txt")
                .with("remoteCell", TEST_CELL2)
                .with("refresh_token", validToken.toTokenString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        VisitorRefreshToken invalidToken = new VisitorRefreshToken(
                id, issuedAt - AbstractOAuth2Token.SECS_IN_A_DAY * 1000 - MILLISECS_IN_AN_MINITE,
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
        String[] scope = new String[0];

        // 期限切れでないトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        ResidentLocalAccessToken validToken = new ResidentLocalAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, schema, scope);
        // データアクセス
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        ResidentLocalAccessToken invalidToken = new ResidentLocalAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR - MILLISECS_IN_AN_MINITE,
                issuer, subject, schema, scope);
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
        VisitorLocalAccessToken validToken = new VisitorLocalAccessToken(
                issuedAt - AbstractOAuth2Token.MILLISECS_IN_AN_HOUR + MILLISECS_IN_AN_MINITE,
                issuer, subject, roleList, schema);
        // データアクセス
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);

        // 期限切れのトークンを生成（IT環境の通信時間を考慮して１分余裕を持たせる）
        VisitorLocalAccessToken invalidToken = new VisitorLocalAccessToken(
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

    /**
     * Access by password change access token.
     */
    @Test
    public final void access_by_password_change_access_token() {
        long issuedAt = new Date().getTime();
        String issuer = UrlUtils.cellRoot(TEST_CELL1);
        String subject = "account2";
        String schema = "";

        // Create password change access token.
        PasswordChangeAccessToken validToken = new PasswordChangeAccessToken(
                issuedAt,
                issuer, subject, schema);

        // Password change access token can not access data.
        ResourceUtils.retrieve(validToken.toTokenString(),
                DAV_COLLECTION + DAV_RESOURCE, HttpStatus.SC_UNAUTHORIZED, TEST_CELL1, Setup.TEST_BOX1);
    }
}

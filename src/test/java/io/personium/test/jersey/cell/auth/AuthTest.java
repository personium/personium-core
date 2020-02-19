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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Relation;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.setup.Setup;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * 認証のテスト.
 * このテストクラスではトークンのパターン別（１６通り）にリソースのアクセス制御のテストします。
 * すべてのリソースのテストを全トークンで行うのはテストを投げる時間が無駄なので、パターン４(パスワード認証ー自分セルトークン取得)でのみ実施しています。
 * その他のトークンについてはボックスリソースのアクセステストのみ実施しています。
 * なお、スキーマ認証系のトークンに関しては別のテストクラスで実施済みのため、ここでのテストは省略しています。
 * ■アクセス制御テストを実施するトークン
 * ２．パスワード認証ートランセルトークン取得.
 * ４．パスワード認証ー自分セルトークン取得.
 * ６．トークン認証ートランセルトークン取得.
 * ８．トークン認証ー他人セルトークン取得.
 * １０．パスワード認証リフレッシュトークンートランセル.
 * １２．パスワード認証リフレッシュトークンー自セルトークン.
 * １４．トークン認証リフレッシュトークンートランセル.
 * １６．トークン認証リフレッシュトークンー他人セルトークン.
 * ■スキーマ認証系はアクセス制御処理はスキーマ無し系と共通のため省略。
 * １．スキーマ付きーパスワード認証ートランセルトークン取得.
 * ３．スキーマ付きーパスワード認証ー自分セルトークン取得.
 * ５．スキーマ付きートークン認証ートランセルトークン取得.
 * ７．スキーマ認証ートークン取得ー他人セルトークン.
 * ９．スキーマ付きパスワード認証リフレッシュトークンートランセル.
 * １１．スキーマ付きパスワード認証リフレッシュトークンー自セルトークン.
 * １３．スキーマ付きトークン認証リフレッシュトークンートランセル.
 * １５．スキーマ付きトークン認証リフレッシュトークンー他人セルトークン.
 * １７．スキーマ付き自セルリフレッシュートランスセルトークン.
 * １８．スキーマ付き自セルリフレッシュー自セルトークン.
 * １９．スキーマ付きトランスセルリフレッシュートランスセルトークン.
 * ２０．スキーマ付きトランスセルリフレッシュー自セルトークン.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthTest extends PersoniumTest {
    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(AuthTest.class);

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String TEST_CELL2 = Setup.TEST_CELL2;
    static final String TEST_APP_CELL1 = "schema1";
    static final String TEST_BOX = Setup.TEST_BOX1;
    static final String DAV_COLLECTION = "setdavcol/";
    static final String ODATA_COLLECTION = "setodata/";
    static final String DAV_RESOURCE = "dav.txt";
    static final String ACL_DEFAULT_SETTING_FILE = "box/acl-default.txt";
    static final String ACL_VARIABLE_SETTING_FILE = "box/acl-setting.txt";
    static final String ACL_AUTH_TEST_FILE = "box/acl-authtest.txt";
    static final String ALL_PROP_FILE = "box/propfind-col-allprop.txt";
    static final String DEL_COL_FILE = "box/delete-col.txt";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * 認証トークン配列番号.
     */
    static final int NO_PRIVILEGE = 0;
    static final int READ = 1;
    static final int WRITE = 2;
    static final int READ_WRITE = 3;
    static final int READ_ACL = 4;
    static final int WRITE_ACL = 5;
    static final int WRITE_PROP = 6;
    static final int READ_PROP = 7;

    /**
     * Constructor.
     */
    public AuthTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得Box() {
        // このテストの流れ
        // testcell1 => testcell1
        // パスワード認証 セルローカルでデータアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);

        AuthTestCommon.boxAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得davcol.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得davcol() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.davCollectionAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーユーザーOData.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得ーユーザーOData() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.odataSchemaAccess(token);
        AuthTestCommon.odataEntityAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーユーザーDavFileResource.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得ーユーザーDavFileResource() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.davFileAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーサービスリソース.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得ーサービスリソース() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.serviceCollectionAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーNullResource.
     */
    @Test
    public final void C04_パスワード認証ー自分セルトークン取得ーNullResource() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.nullResouceAccess(token);
    }

    /**
     * ２．パスワード認証ートランセルトークン取得.
     */
    @Test
    public final void C02_パスワード認証ートランセルトークン取得() {
        // このテストの流れ
        // testcell2 => testcell1
        // パスワード認証 TCトークンでデータアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuthForTransCell(TEST_CELL2, TEST_CELL1, token, refreshToken);

        AuthTestCommon.boxAccess(token);
    }

    /**
     * ３．スキーマ付きーパスワード認証ー自分セルトークン取得.
     * @throws UnsupportedEncodingException UnsupportedEncodingException
     */
    @Test
    public final void C03_スキーマ付きーパスワード認証ー自分セルトークン取得() throws UnsupportedEncodingException {
        // アプリセルに対して認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", "schema1")
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // Queryでスキーマ認証
        Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", transCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
                        transCellAccessToken);
        // Authorizationヘッダでスキーマ認証
        Http.request("authn/password-cl-ch.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("base64idpw", schemaTransCellAccessTokenHeader)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * １．スキーマ付きーパスワード認証ートランセルトークン取得.
     */
    @Test
    public final void C01_スキーマ付きーパスワード認証ートランセルトークン取得() {
        // アプリセルに対して認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", "schema1")
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String schemaTransCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // Queryでスキーマ認証
        Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", schemaTransCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
                        schemaTransCellAccessToken);
        // Authorizationヘッダでスキーマ認証
        Http.request("authn/password-cl-ch.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("base64idpw", schemaTransCellAccessTokenHeader)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * ６．トークン認証ートランセルトークン取得＿アクセス制御.
     */
    @Test
    public final void C06_トークン認証ートランセルトークン取得＿アクセス制御() {
        // このテストの流れ
        // testcell2 => testcell1 => testcell2 => testcell1 => testcell1
        // パスワード認証 TCトークン１ TCトークン２ TCトークン３ セルローカルでデータアクセス
        // 本来はTCトークン３でテストしたかったが、トークンの文字数が大きくなりすぎてヘッダのサイズ制限を超えてしまったのでセルローカルに持ち替えてテストを実施

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuthForTransCell(TEST_CELL2, TEST_CELL1, token, refreshToken);

        HashMap<Integer, String> token2 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken2 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForTransCell(TEST_CELL1, TEST_CELL2, token, token2, refreshToken2);

        HashMap<Integer, String> token3 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken3 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForTransCell(TEST_CELL2, TEST_CELL1, token2, token3, refreshToken3);

        HashMap<Integer, String> token4 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken4 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForCellLocal(TEST_CELL1, token3, token4, refreshToken4);

        AuthTestCommon.boxAccess(token4);
    }

    /**
     * ６．トークン認証ートランセルトークン取得＿トークン発行のテスト.
     */
    @Test
    public final void C06_トークン認証ートランセルトークン取得＿トークン発行のテスト() {
        // ROPC at "testcell1" targeting "testcell2"
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        Http.request("authn/saml-tc-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);

    }

    /**
     * トークン認証ートランセルトークン取得＿LocalUnitトークン発行のテスト.
     */
    @Test
    public final void トークン認証ートランセルトークン取得＿localunitスキーム宛のトークン発行できること() {
        // ROPC at "testcell1" targeting "testcell2"
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", "personium-localunit:/" + TEST_CELL2 + "/")
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // 発行されたトークンをTEST_CELL2で検証
        Http.request("authn/saml-tc-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("p_target", "personium-localunit:/" + TEST_APP_CELL1 + "/")
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * ExtCell with "personium-localunit" scheme Url should still work.
     *   Case1. When a role is directly assigned to it.
     */
    @Test
    public final void ExtCell_With_LocalunitSchemeUrl_ShouldWork_When_Role_isDirectlyAssigned() {
        //String httpCell1Url = UrlUtils.cellRoot(TEST_CELL1);
        String httpCell2Url = UrlUtils.cellRoot(TEST_CELL2);
        String localunitCell1Url = "personium-localunit:" + TEST_CELL1 + ":/";
        String transCellAccessToken = null;
        String testfile = "testfile.txt";
        String testrole = "transCellTestRole";
        String roleUrl = UrlUtils.roleUrl(TEST_CELL2, null, testrole);

        try {
            // --------------------
            // Preparing Test (Configuration with MASTER_TOKEN)
            // --------------------
            // 1. Create ExtCell pointing to Cell1 from Cell 2
            //  Temporarily create a ExtCell entry with "personium-localunit" scheme
            //  on cell 2 pointing to cell 1,
            //  which is semantically identical to one created by Setup#setup
            //  using http URL,
            ExtCellUtils.create(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, -1);

            // 2. Create a Role and attach it to the ExtCell (on Cell 2)
            //      create
            RoleUtils.create(TEST_CELL2, MASTER_TOKEN, testrole, -1);

            //      attach
            Http.request("cell/link-extCell-role.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("cellName", CommonUtils.encodeUrlComp(localunitCell1Url))
                    .with("token", MASTER_TOKEN)
                    .with("roleUrl", roleUrl)
                    .returns().statusCode(-1);

            // 3.PUT a Test File
            // use main box (box1 has ACL settings and not suitable for testing)
            Http.request("box/dav-put.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("source", "testFileBody")
                    .returns().statusCode(-1);
            // 4.ACL Configuration (grant read privilege)
            setAcl(TEST_CELL2, Box.MAIN_BOX_NAME, MASTER_TOKEN, testfile, "../__/" + testrole, "read");

            // --------------------
            // TEST execution
            // --------------------
            // T1. Issue Trans-Cell Access Token from TEST_CELL1 to TEST_CELL2.
            TResponse res1 =
                Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", "account1")
                    .with("password", "password1")
                    .with("p_target", httpCell2Url)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res1.bodyAsJson();
            transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // T2. Issue VisitorLocalAccessToken on TEST_CELL2 in exchange for the previous TCAT.
            TResponse res2 =
                Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            JSONObject jsonLocal = res2.bodyAsJson();
            String localCellAccessToken = (String) jsonLocal.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Now, verify that
            // localCellAccessToken has actually Role1
            VisitorLocalAccessToken vlat = VisitorLocalAccessToken.parse(localCellAccessToken, httpCell2Url);
            assertEquals(1, vlat.getRoleList().size());
            for (Role role : vlat.getRoleList()) {
                log.info("  role = " + role.toRoleClassURL());
            }

            // ここで取得できたlocalCellAccessTokenにRole1が割りあたっていることを検証する。

            // T3. Should be able to obtain textfile
            //                     w/ localCellAccessToken
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("token", localCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            //                     w/ transCellAccessToken
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("token", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

        } catch (Exception e) {
            log.info("-------Exception-----------");
            fail(e.getMessage());
        } finally {
            // 4. restore the ACL
            resetAcl(TEST_CELL2, Box.MAIN_BOX_NAME, MASTER_TOKEN, testfile);

            // 3. Delete the Text File
            TResponse resResource = Http.request("box/dav-delete.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .returns();
            resResource.statusCode(-1);

            // 2. Delete Role
            RoleUtils.delete(TEST_CELL2, MASTER_TOKEN, testrole, null);

            // 1. Delete ExtCell (restore)
            ExtCellUtils.delete(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, -1);
            //            ExtCellUtils.update(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, httpCell1Url, -1);
        }
    }

    /**
     * ExtCell with "personium-localunit" scheme Url should still work.
     *   Case2. A role is assigned to it via a relation.
     */
    @Test
    public final void ExtCell_With_LocalunitSchemeUrl_ShouldWork_When_Role_isAssignedViaRelation() {
        //String httpCell1Url = UrlUtils.cellRoot(TEST_CELL1);
        String httpCell2Url = UrlUtils.cellRoot(TEST_CELL2);
        String localunitCell1Url = "personium-localunit:" + TEST_CELL1 + ":/";
        String transCellAccessToken = null;
        String testfile = "testfile.txt";
        String testrole = "transCellTestRole";
        String testrelation = "testRelation";

        try {
            // --------------------
            // Preparing Test (Configuration with MASTER_TOKEN)
            // --------------------
            // 1. Create ExtCell on Cell2
            //  Temporarily create a ExtCell entry with
            // "personium-localunit" scheme on cell 2 pointing to cell 1,
            //  which is semantically identical to one created by Setup#setup
            //  using http URL,
            ExtCellUtils.create(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, -1);

            // 2. Configure attach a Relation and then a Role to the ExtCell
            RoleUtils.create(TEST_CELL2, MASTER_TOKEN, testrole, -1);

            JSONObject body = new JSONObject();
            body.put("Name", testrelation);
            body.put("_Box.Name", null);
            RelationUtils.create(TEST_CELL2, MASTER_TOKEN, body, -1);

            // Link ExtCell and Relation
            LinksUtils.createLinksExtCell(TEST_CELL2, CommonUtils.encodeUrlComp(localunitCell1Url),
                    Relation.EDM_TYPE_NAME, testrelation, null, MASTER_TOKEN, -1);
            // Link Relation to Role
            LinksUtils.createLinks(TEST_CELL2, Relation.EDM_TYPE_NAME, testrelation, null,
                    Role.EDM_TYPE_NAME, testrole, null, MASTER_TOKEN, -1);

            // 3.PUT a Test File
            // use main box (box1 has ACL settings and not suitable for testing)
            Http.request("box/dav-put.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("source", "testFileBody")
                    .returns().statusCode(-1);
            // 4.ACL Configuration (grant read privilege)
            setAcl(TEST_CELL2, Box.MAIN_BOX_NAME, MASTER_TOKEN, testfile, "../__/" + testrole, "read");

            // --------------------
            // TEST execution
            // --------------------
            // T1. Issue Trans-Cell Access Token from TEST_CELL1 to TEST_CELL2.
            TResponse res1 =
                Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", "account1")
                    .with("password", "password1")
                    .with("p_target", httpCell2Url)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res1.bodyAsJson();
            transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // T2. Issue VisitorLocalAccessToken on TEST_CELL2 in exchange for the previous TCAT.
            TResponse res2 =
                Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            JSONObject jsonLocal = res2.bodyAsJson();
            String localCellAccessToken = (String) jsonLocal.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Now, verify that
            // localCellAccessToken has actually Role1
            VisitorLocalAccessToken vlat = VisitorLocalAccessToken.parse(localCellAccessToken, httpCell2Url);
           ///assertEquals(1, vlat.getRoleList().size());
            for (Role role : vlat.getRoleList()) {
                log.info(" r=" + role.toRoleClassURL());
            }

            // T3. Should be able to obtain textfile
            //                     w/ localCellAccessToken
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("token", localCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            //                     w/ transCellAccessToken
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .with("token", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);
        } catch (Exception e) {
            log.info("-------Exception-----------");
            fail(e.getMessage());
        } finally {
            log.info("-------------------------------");
            // 4. restore the ACL
            resetAcl(TEST_CELL2, Box.MAIN_BOX_NAME, MASTER_TOKEN, testfile);

            // 3. Delete the Text File
            TResponse resResource = Http.request("box/dav-delete.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", Box.MAIN_BOX_NAME)
                    .with("path", testfile)
                    .returns();
            resResource.statusCode(-1);

            // Delete Relation, Role & ExtCell

            // Delete Relation on Cell 2
            RelationUtils.delete(TEST_CELL2, MASTER_TOKEN, testrelation, null, -1);

            // Delete Role
            RoleUtils.delete(TEST_CELL2, MASTER_TOKEN, testrole, null);

            // 1.Delete ExtCell (restore)
            ExtCellUtils.delete(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, -1);
        }
    }

    private TResponse setAcl(String cellName, String boxName, String token, String collection,
            String role, String... privileges)
            throws JAXBException {
        Acl acl = new Acl();
        for (String privilege : privileges) {
            acl.getAce().add(DavResourceUtils.createAce(false, role, privilege));
        }
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), cellName, boxName));
        return DavResourceUtils.setAcl(token, cellName, boxName, collection, acl, HttpStatus.SC_OK);
    }

    private TResponse resetAcl(String cellName, String boxName, String token, String collection) {
        Acl acl = new Acl();
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), cellName, boxName));
        try {
            return DavResourceUtils.setAcl(token, cellName, boxName, collection, acl, -1);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ８．トークン認証ー他人セルトークン取得＿アクセス制御.
     */
    @Test
    public final void C08_トークン認証ー他人セルトークン取得＿アクセス制御() {
        // このテストの流れ
        // testcell2 => testcell1 => testcell1
        // パスワード認証 TCトークン セルローカルでアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuthForTransCell(TEST_CELL2, TEST_CELL1, token, refreshToken);

        HashMap<Integer, String> token2 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken2 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForCellLocal(TEST_CELL1, token, token2, refreshToken2);

        AuthTestCommon.boxAccess(token2);
    }

    /**
     * ８．トークン認証ー他人セルトークン取得＿トークン発行のテスト.
     */
    @Test
    public final void C08_トークン認証ー他人セルトークン取得＿トークン発行のテスト() {
        // ROPC at "testcell1" targeting "testcell2"
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);

    }

    /**
     * ５．スキーマ付きートークン認証ートランセルトークン取得.
     */
    @Test
    public final void C05_スキーマ付きートークン認証ートランセルトークン取得() {
        // ROPC at "testcell1" targeting "testcell2"
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アプリセルに対して認証
        TResponse res2 =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_APP_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String schemaTransCellAccessToken = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // Queryでスキーマ認証
        Http.request("authn/saml-cl-cp.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", schemaTransCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        String schemaTransCellAccessTokenHeader =
                CommonUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
                        schemaTransCellAccessToken);
        // Authorizationヘッダでスキーマ認証
        Http.request("authn/saml-cl-ch.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("base64idpw", schemaTransCellAccessTokenHeader)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * ７．スキーマ認証ートークン取得ー他人セルトークン.
     */
    @Test
    public final void C07_スキーマ認証ートークン取得ー他人セルトークン() {
        // ROPC at "testcell1" targeting "testcell2"
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // アプリセルに対して認証
        TResponse res2 =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_APP_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String schemaTransCellAccessToken = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // Queryでスキーマ認証
        Http.request("authn/saml-tc-cp.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", schemaTransCellAccessToken)
                .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        String schemaTransCellAccessTokenHeader = CommonUtils.createBasicAuthzHeader(
                UrlUtils.cellRoot(TEST_APP_CELL1),
                schemaTransCellAccessToken);

        // Authorizationヘッダでスキーマ認証
        Http.request("authn/saml-tc-ch.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", transCellAccessToken)
                .with("base64idpw", schemaTransCellAccessTokenHeader)
                .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * １０．パスワード認証リフレッシュトークンートランセル＿アクセス制御.
     */
    @Test
    public final void C10_パスワード認証リフレッシュトークンートランセル＿アクセス制御() {
        // このテストの流れ
        // testcell2 => testcell2 => testcell1
        // パスワード認証 リフレッシュ TCトークンでデータアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL2, token, refreshToken);

        HashMap<Integer, String> afterToken = new HashMap<Integer, String>();
        AuthTestCommon.refreshAuthForTransCell(TEST_CELL2, TEST_CELL1, refreshToken, afterToken, refreshToken);

        AuthTestCommon.boxAccess(afterToken);
    }

    /**
     * １０．パスワード認証リフレッシュトークンートランセル＿トークン発行のテスト.
     */
    @Test
    public final void C10_パスワード認証リフレッシュトークンートランセル＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken = ResidentRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res = Http.request("authn/refresh-tc.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("refresh_token", refreshToken)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken2 = ResidentRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rCellLocalToken.getId().equals(rCellLocalToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rCellLocalToken.getIssuer(), rCellLocalToken2.getIssuer());
            assertEquals(rCellLocalToken.getSchema(), rCellLocalToken2.getSchema());
            assertEquals(rCellLocalToken.getSubject(), rCellLocalToken2.getSubject());
        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １２．パスワード認証リフレッシュトークンー自セルトークン＿アクセス制御.
     */
    @Test
    public final void C12_パスワード認証リフレッシュトークンー自セルトークン＿アクセス制御() {
        // このテストの流れ
        // testcell1 => testcell1 => testcell1
        // パスワード認証 リフレッシュ セルローカルでデータアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);

        HashMap<Integer, String> token2 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken2 = new HashMap<Integer, String>();
        AuthTestCommon.refreshAuthForCellLocal(TEST_CELL1, refreshToken, token2, refreshToken2);

        AuthTestCommon.boxAccess(token2);
    }

    /**
     * １２．パスワード認証リフレッシュトークンー自セルトークン＿トークン発行のテスト.
     */
    @Test
    public final void C12_パスワード認証リフレッシュトークンー自セルトークン＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken = ResidentRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res = Http.request("authn/refresh-cl.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("refresh_token", refreshToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken2 = ResidentRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rCellLocalToken.getId().equals(rCellLocalToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rCellLocalToken.getIssuer(), rCellLocalToken2.getIssuer());
            assertEquals(rCellLocalToken.getSchema(), rCellLocalToken2.getSchema());
            assertEquals(rCellLocalToken.getSubject(), rCellLocalToken2.getSubject());

        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * ９．スキーマ付きパスワード認証リフレッシュトークンートランセル.
     */
    @Test
    public final void C09_スキーマ付きパスワード認証リフレッシュトークンートランセル() {
        try {
            // App Auth at "schema1"
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ROPC at "testcell1" with app auth (body)
            TResponse res2 = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", "account1")
                    .with("password", "password1")
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", transCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken = ResidentRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // refresh without app auth  should fail
            TResponse res3 =
                    Http.request("authn/refresh-tc-cp.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("refresh_token", refreshToken)
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", transCellAccessToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken2 = ResidentRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rCellLocalToken.getId().equals(rCellLocalToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rCellLocalToken.getIssuer(), rCellLocalToken2.getIssuer());
            assertEquals(rCellLocalToken.getSchema(), rCellLocalToken2.getSchema());
            assertEquals(rCellLocalToken.getSubject(), rCellLocalToken2.getSubject());
        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １１．スキーマ付きパスワード認証リフレッシュトークンー自セルトークン.
     */
    @Test
    public final void C11_スキーマ付きパスワード認証リフレッシュトークンー自セルトークン() {
        try {
            // アプリセルに対して認証
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Queryでスキーマ認証
            TResponse res2 = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", "account1")
                    .with("password", "password1")
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", transCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken = ResidentRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res3 =
                    Http.request("authn/refresh-cl-cp.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("refresh_token", refreshToken)
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", transCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            ResidentRefreshToken rCellLocalToken2 = ResidentRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rCellLocalToken.getId().equals(rCellLocalToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rCellLocalToken.getIssuer(), rCellLocalToken2.getIssuer());
            assertEquals(rCellLocalToken.getSchema(), rCellLocalToken2.getSchema());
            assertEquals(rCellLocalToken.getSubject(), rCellLocalToken2.getSubject());
        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １４．トークン認証リフレッシュトークンートランセル＿アクセス制御.
     */
    @Test
    public final void C14_トークン認証リフレッシュトークンートランセル＿アクセス制御() {
        // このテストの流れ
        // testcell2 => testcell1 => testcell2 => testcell1 => testcell1 => testcell1
        // パスワード認証 TCトークン１ TCトークン２ TCトークン３ リフレッシュ セルローカルでデータアクセス
        // 本来はTCトークン３でテストしたかったが、トークンの文字数が大きくなりすぎてヘッダのサイズ制限を超えてしまったのでセルローカルに持ち替えてテストを実施

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuthForTransCell(TEST_CELL2, TEST_CELL1, token, refreshToken);

        HashMap<Integer, String> token2 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken2 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForTransCell(TEST_CELL1, TEST_CELL2, token, token2, refreshToken2);

        HashMap<Integer, String> token3 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken3 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForTransCell(TEST_CELL2, TEST_CELL1, token2, token3, refreshToken3);

        HashMap<Integer, String> token4 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken4 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForCellLocal(TEST_CELL1, token3, token4, refreshToken4);

        HashMap<Integer, String> token5 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken5 = new HashMap<Integer, String>();
        AuthTestCommon.refreshAuthForCellLocal(TEST_CELL1, refreshToken4, token5, refreshToken5);

        AuthTestCommon.boxAccess(token5);
    }

    /**
     * １４．トークン認証リフレッシュトークンートランセル＿トークン発行のテスト.
     */
    @Test
    public final void C14_トークン認証リフレッシュトークンートランセル＿トークン発行のテスト() {
        try {
            // ROPC at "testcell1" targeting "testcell2"
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // セルに対してトークン認証
            TResponse res2 =
                    Http.request("authn/saml-tc-c0.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("assertion", transCellAccessToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json2 = res2.bodyAsJson();
            String refreshToken = (String) json2.get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken = VisitorRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // Refresh
            TResponse res3 =
                    Http.request("authn/refresh-tc.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            JSONObject json3 = res3.bodyAsJson();
            String refreshToken2 = (String) json3.get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken2 = VisitorRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken.getRoleList().get(0).toRoleInstanceURL(), rToken2.getRoleList().get(0).toRoleInstanceURL());
            assertEquals(rToken.getSchema(), rToken2.getSchema());
            assertEquals(rToken.getSubject(), rToken2.getSubject());
        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １６．トークン認証リフレッシュトークンー他人セルトークン＿アクセス制御.
     */
    @Test
    public final void C16_トークン認証リフレッシュトークンー他人セルトークン＿アクセス制御() {
        // このテストの流れ
        // testcell2 => testcell1 => testcell1 => testcell1
        // パスワード認証 TCトークン リフレッシュ セルローカルでデータアクセス

        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuthForTransCell(TEST_CELL2, TEST_CELL1, token, refreshToken);

        HashMap<Integer, String> token2 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken2 = new HashMap<Integer, String>();
        AuthTestCommon.samlAuthForCellLocal(TEST_CELL1, token, token2, refreshToken2);

        HashMap<Integer, String> token3 = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken3 = new HashMap<Integer, String>();
        AuthTestCommon.refreshAuthForCellLocal(TEST_CELL1, refreshToken2, token3, refreshToken3);

        AuthTestCommon.boxAccess(token3);
    }

    /**
     * １６．トークン認証リフレッシュトークンー他人セルトークン＿トークン発行のテスト.
     */
    @Test
    public final void C16_トークン認証リフレッシュトークンー他人セルトークン＿トークン発行のテスト() {
        try {
            // ROPC at "testcell1" targeting "testcell2"
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // セルに対してトークン認証
            TResponse res2 =
                    Http.request("authn/saml-tc-c0.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("assertion", transCellAccessToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json2 = res2.bodyAsJson();
            String refreshToken = (String) json2.get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken = VisitorRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // Refresh
            TResponse res3 =
                    Http.request("authn/refresh-cl.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            JSONObject json3 = res3.bodyAsJson();
            String refreshToken2 = (String) json3.get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken2 = VisitorRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken.getRoleList().get(0).toRoleInstanceURL(), rToken2.getRoleList().get(0).toRoleInstanceURL());
            assertEquals(rToken.getSchema(), rToken2.getSchema());
            assertEquals(rToken.getSubject(), rToken2.getSubject());
        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １３．スキーマ付きトークン認証リフレッシュトークンートランセル.
     */
    @Test
    public final void C13_スキーマ付きトークン認証リフレッシュトークンートランセル() {
        try {
            // ROPC at "testcell1" targeting "testcell2"
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // App Auth at "schema1"
            TResponse res2 =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json2 = res2.bodyAsJson();
            String schemaTransCellAccessToken = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // receive assertion at "testcell2" with app auth
            TResponse res3 = Http.request("authn/saml-cl-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken1 = VisitorRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res4 =
                    Http.request("authn/refresh-tc-cp.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", schemaTransCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken2 = VisitorRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken1.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken1.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken1.getRoleList().get(0).toRoleInstanceURL(), rToken2.getRoleList().get(0).toRoleInstanceURL());
            assertEquals(rToken1.getSchema(), rToken2.getSchema());
            assertEquals(rToken1.getSubject(), rToken2.getSubject());

        } catch (TokenParseException e) {
            fail();
        }
    }

    /**
     * １５．スキーマ付きトークン認証リフレッシュトークンー他人セルトークン.
     */
    @Test
    public final void C15_スキーマ付きトークン認証リフレッシュトークンー他人セルトークン() {
        try {
            // ROPC at "testcell1" targeting at "testcell2"
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // App Auth at "schema1"
            TResponse res2 =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json2 = res2.bodyAsJson();
            String schemaTransCellAccessToken = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // receive assertion at "testcell2"
            TResponse res3 = Http.request("authn/saml-cl-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken1 = VisitorRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // Refresh
            TResponse res4 =
                    Http.request("authn/refresh-cl-cp.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", schemaTransCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            VisitorRefreshToken rToken2 = VisitorRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken1.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken1.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken1.getRoleList().get(0).toRoleInstanceURL(), rToken2.getRoleList().get(0).toRoleInstanceURL());
            assertEquals(rToken1.getSchema(), rToken2.getSchema());
            assertEquals(rToken1.getSubject(), rToken2.getSubject());

        } catch (TokenParseException e) {
            fail();
        }

    }

    /**
     */
    @Test
    public void C17_スキーマ付き自セルリフレッシュートランスセルトークン() {
        try {
            // App Auth Token for TEST_CELL1
            TResponse res2 =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            String schemaTransCellAccessToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ROPC at TEST_CELL1 without app auth
            TResponse res =
                    Http.request("authn/password-cl-cp.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", schemaTransCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            String cellAccessToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            String refreshToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);

            // One second stop to use the refresh token
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }


            // ------------------------------
            // refresh at TEST_CELL1 adding app auth (body)
            // ------------------------------
            TResponse res3 = Http.request("authn/refresh-tc-cp.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("refresh_token", refreshToken)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String transCellAccessToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken = (TransCellAccessToken) TransCellAccessToken.parse(transCellAccessToken,
                    UrlUtils.cellRoot(TEST_CELL1), UrlUtils.getHost());

            // Token check
            assertTrue(!cellAccessToken.equals(transCellAccessToken));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL1)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertThat(aToken.getTarget(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

            // ------------------------------
            // refresh at TEST_CELL1 adding app auth  (header)
            // ------------------------------
            String schemaTransCellAccessTokenHeader = CommonUtils.createBasicAuthzHeader(
                    UrlUtils.cellRoot(TEST_APP_CELL1), schemaTransCellAccessToken);

            res3 = Http.request("authn/refresh-tc-ch.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("refresh_token", refreshToken)
                    .with("base64idpw", schemaTransCellAccessTokenHeader)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            transCellAccessToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            aToken = (TransCellAccessToken) TransCellAccessToken.parse(transCellAccessToken,
                    UrlUtils.cellRoot(TEST_CELL1), UrlUtils.getHost());

            // Token check
            assertTrue(!cellAccessToken.equals(transCellAccessToken));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL1)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertThat(aToken.getTarget(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

        } catch (TokenParseException | TokenDsigException | TokenRootCrtException e) {
            fail();
        }
    }

    /**
     * １８．スキーマ付き自セルリフレッシュー自セルトークン.
     */
    @Test
    public void C18_スキーマ付き自セルリフレッシュー自セルトークン() {
        // ROPC "testcell1" without app auth
        TResponse res =
                Http.request("authn/password-cl-c0.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        String cellAccessToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
        String refreshToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);

        // pause to use the refresh token
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail();
        }

        // App Auth at "schema1" cell
        TResponse res2 =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", TEST_APP_CELL1)
                        .with("username", "account1")
                        .with("password", "password1")
                        .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        String schemaTransCellAccessToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

        // ------------------------------
        // Refresh Should fail when added app (body) auth at refresh time
        // ------------------------------
        TResponse res3 = Http.request("authn/refresh-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("refresh_token", refreshToken)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", schemaTransCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

/*            String cellAccessToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            AccountAccessToken aToken = (AccountAccessToken) AccountAccessToken.parse(
                    cellAccessToken2, UrlUtils.cellRoot(TEST_CELL1));

            // Token check
//            assertTrue(!cellAccessToken.equals(cellAccessToken2));
//            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL1)));
//            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
//            assertNull(aToken.getTarget());
//            assertThat(aToken.getSubject(), is("account1"));
 *
 */

        // ------------------------------
        // Refresh should fail when added app auth (header) at refresh time.
        // ------------------------------
        String schemaTransCellAccessTokenHeader = CommonUtils.createBasicAuthzHeader(
                UrlUtils.cellRoot(TEST_APP_CELL1), schemaTransCellAccessToken);

        res3 = Http.request("authn/refresh-cl-ch.txt")
                .with("remoteCell", TEST_CELL1)
                .with("refresh_token", refreshToken)
                .with("base64idpw", schemaTransCellAccessTokenHeader)
                .returns()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        /*
        cellAccessToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
        aToken = (AccountAccessToken) AccountAccessToken.parse(
                cellAccessToken2, UrlUtils.cellRoot(TEST_CELL1));

        // Token check
        assertTrue(!cellAccessToken.equals(cellAccessToken2));
        assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL1)));
        assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
        assertNull(aToken.getTarget());
        assertThat(aToken.getSubject(), is("account1"));
        */

    }

    /**
     * １９．スキーマ付きトランスセルリフレッシュートランスセルトークン.
     */
    @Test
    public void C19_スキーマ付きトランスセルリフレッシュートランスセルトークン() {
        try {
            // ROPC at "testcell1" targeting at "testcell2" without app auth
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            String transCellAccessToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // App Auth at "schema1" for "testcell2"
            TResponse res3 =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String schemaTransCellAccessToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Authenticate to user cell (get TransCellRefreshToken)
            TResponse res2 =
                    Http.request("authn/saml-cl-cp.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("assertion", transCellAccessToken)
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", schemaTransCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String cellAccessToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            String refreshToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);

            // One second stop to use the refresh token
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }


            // ------------------------------
            // Refresh at "testcell2" adding app auth  (body)
            // ------------------------------
            TResponse res4 = Http.request("authn/refresh-tc-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("refresh_token", refreshToken)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String transCellAccessToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken = (TransCellAccessToken) TransCellAccessToken.parse(transCellAccessToken2,
                    UrlUtils.cellRoot(TEST_CELL1), UrlUtils.getHost());

            // Token check
            assertTrue(!cellAccessToken.equals(transCellAccessToken2));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertThat(aToken.getTarget(), is(UrlUtils.cellRoot(TEST_CELL1)));
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

            // ------------------------------
            // Refresh at "testcell2" adding app auth  (header)
            // ------------------------------
            String schemaTransCellAccessTokenHeader = CommonUtils.createBasicAuthzHeader(
                    UrlUtils.cellRoot(TEST_APP_CELL1), schemaTransCellAccessToken);

            res4 = Http.request("authn/refresh-tc-ch.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("refresh_token", refreshToken)
                    .with("base64idpw", schemaTransCellAccessTokenHeader)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            transCellAccessToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            aToken = (TransCellAccessToken) TransCellAccessToken.parse(transCellAccessToken2,
                    UrlUtils.cellRoot(TEST_CELL1), UrlUtils.getHost());

            // Token check
            assertTrue(!cellAccessToken.equals(transCellAccessToken2));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertThat(aToken.getTarget(), is(UrlUtils.cellRoot(TEST_CELL1)));
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

        } catch (TokenParseException | TokenDsigException | TokenRootCrtException e) {
            fail();
        }
    }

    /**
     * ２０．スキーマ付きトランスセルリフレッシュー自セルトークン.
     */
    @Test
    public void C20_スキーマ付きトランスセルリフレッシュー自セルトークン() {
        try {

            // ROPC at "testcell1" targeting at "testcell2" without app auth
            TResponse res =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            String transCellAccessToken = (String) res.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);


            // App Auth at "schema1" for "testcell2"
            TResponse res3 =
                    Http.request("authn/password-tc-c0.txt")
                            .with("remoteCell", TEST_APP_CELL1)
                            .with("username", "account1")
                            .with("password", "password1")
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            String schemaTransCellAccessToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);


            // receive TCAT at "testcell2"
            TResponse res2 =
                    Http.request("authn/saml-cl-cp.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("assertion", transCellAccessToken)
                            .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .with("client_secret", schemaTransCellAccessToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String cellAccessToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            String refreshToken = (String) res2.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);

            // One second stop to use the refresh token
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }


            // ------------------------------
            // Refresh at "testcell2" adding app auth  (body)
            // ------------------------------
            TResponse res4 = Http.request("authn/refresh-cl-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("refresh_token", refreshToken)
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String cellAccessToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            VisitorLocalAccessToken aToken = (VisitorLocalAccessToken) VisitorLocalAccessToken.parse(
                    cellAccessToken2, UrlUtils.cellRoot(TEST_CELL2));

            // Token check
            assertTrue(!cellAccessToken.equals(cellAccessToken2));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertNull(aToken.getTarget());
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

            // ------------------------------
            // Refresh at "testcell2" adding app auth  (header)
            // ------------------------------
            String schemaTransCellAccessTokenHeader = CommonUtils.createBasicAuthzHeader(
                    UrlUtils.cellRoot(TEST_APP_CELL1), schemaTransCellAccessToken);

            res4 = Http.request("authn/refresh-cl-ch.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("refresh_token", refreshToken)
                    .with("base64idpw", schemaTransCellAccessTokenHeader)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            cellAccessToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            aToken = (VisitorLocalAccessToken) VisitorLocalAccessToken.parse(
                    cellAccessToken2, UrlUtils.cellRoot(TEST_CELL2));

            // Token check
            assertTrue(!cellAccessToken.equals(cellAccessToken2));
            assertThat(aToken.getIssuer(), is(UrlUtils.cellRoot(TEST_CELL2)));
            assertThat(aToken.getSchema(), is(UrlUtils.cellRoot(TEST_APP_CELL1) + "#c"));
            assertNull(aToken.getTarget());
            assertThat(aToken.getSubject(), is(UrlUtils.cellRoot(TEST_CELL1) + "#account1"));

        } catch (TokenParseException e) {
            fail();
        }
    }

}

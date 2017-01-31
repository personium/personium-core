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

import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.CellLocalRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.Relation;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DcRunner;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

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
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthTest extends JerseyTest {

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
     * コンストラクタ.
     */
    public AuthTest() {
        super("io.personium.core.rs");
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得.
     */
    @Test
    public final void パスワード認証ー自分セルトークン取得Box() {
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
    public final void パスワード認証ー自分セルトークン取得davcol() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.davCollectionAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーユーザーOData.
     */
    @Test
    public final void パスワード認証ー自分セルトークン取得ーユーザーOData() {
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
    public final void パスワード認証ー自分セルトークン取得ーユーザーDavFileResource() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.davFileAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーサービスリソース.
     */
    @Test
    public final void パスワード認証ー自分セルトークン取得ーサービスリソース() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.serviceCollectionAccess(token);
    }

    /**
     * ４．パスワード認証ー自分セルトークン取得ーNullResource.
     */
    @Test
    public final void パスワード認証ー自分セルトークン取得ーNullResource() {
        HashMap<Integer, String> token = new HashMap<Integer, String>();
        HashMap<Integer, String> refreshToken = new HashMap<Integer, String>();
        AuthTestCommon.accountAuth(TEST_CELL1, token, refreshToken);
        AuthTestCommon.nullResouceAccess(token);
    }

    /**
     * ２．パスワード認証ートランセルトークン取得.
     */
    @Test
    public final void パスワード認証ートランセルトークン取得() {
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
    public final void スキーマ付きーパスワード認証ー自分セルトークン取得() throws UnsupportedEncodingException {
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
        Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(TEST_CELL1, "account1");
        Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", "account1")
                .with("password", "password1")
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", transCellAccessToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        AuthTestCommon.accountLastAuthenticatedCheck(TEST_CELL1, "account1", lastAuthenticatedTime);

        String schemaTransCellAccessTokenHeader =
                PersoniumCoreUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
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
    public final void スキーマ付きーパスワード認証ートランセルトークン取得() {
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
                PersoniumCoreUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
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
    public final void トークン認証ートランセルトークン取得＿アクセス制御() {
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
    public final void トークン認証ートランセルトークン取得＿トークン発行のテスト() {
        // セルに対してパスワード認証
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
        // TEST_CELL1のパスワード認証にてTEST_CELL2宛トークンを発行
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
     * 外部セルのurlがlocalunitの場合でもトークン発行できること.
     */
    @Test
    public final void ターゲットhttp外部セルのurlがlocalunitの場合でもトークン発行できること_外部セルにロールが直接わりあてられている場合() {
        String httpCell1Url = UrlUtils.cellRoot(TEST_CELL1);
        String httpCell2Url = UrlUtils.cellRoot(TEST_CELL2);
        String localunitCell1Url = "personium-localunit:/" + TEST_CELL1 + "/";
        String transCellAccessToken = null;
        String testfile = "testfile.txt";
        String testrole = "transCellTestRole";
        String roleUrl = UrlUtils.roleUrl(TEST_CELL2, null, testrole);
        // main box を使用（box1にはACL設定がありテストには不適切であるため）
        String testBox = "__";

        // dcTargetの値がhttpの場合
        try {
            // テスト準備  （MASTER_TOKENで実施）
            // 1.ExtCell更新
            // Setupでセル２に外部セルとして登録されているセル１のhttpのURLをpersonium-localunitに一時的に更新。
            ExtCellUtils.update(MASTER_TOKEN, TEST_CELL2, httpCell1Url, localunitCell1Url, HttpStatus.SC_NO_CONTENT);

            // Role作成
            RoleUtils.create(TEST_CELL2, MASTER_TOKEN, testrole, HttpStatus.SC_CREATED);

            // 2.セル2の設定として、この外部セルにロール１を割当。
            // Setupで作成されたrole1を紐づけ。
            Http.request("cell/link-extCell-role.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("cellName", PersoniumCoreUtils.encodeUrlComp(localunitCell1Url))
                    .with("token", MASTER_TOKEN)
                    .with("roleUrl", roleUrl)
                    .returns().statusCode(HttpStatus.SC_NO_CONTENT);

            // 3.リソースを配置
            Http.request("box/dav-put.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("source", "testFileBody")
                    .returns().statusCode(HttpStatus.SC_CREATED);
            // 4.ACL設定 (リソース権限を割り当てる)
            setAcl(TEST_CELL2, testBox, MASTER_TOKEN, testfile, "../__/" + testrole, "read");

            // ここからがテストの実施
            // TEST_CELL1のパスワード認証にてTEST_CELL2宛トークンを発行
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

            // 発行されたトークンをTEST_CELL2で検証
            TResponse res2 =
                Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            JSONObject jsonLocal = res2.bodyAsJson();
            String localCellAccessToken = (String) jsonLocal.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ここで取得できたlocalCellAccessTokenにRole1が割りあたっていることを検証する。

            // テキストファイルが取得ができること(localCellAccessToken)
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("token", localCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            // テキストファイルが取得ができること(transCellAccessToken)
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("token", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // 4.ACLを元に戻す
            resetAcl(TEST_CELL2, testBox, MASTER_TOKEN, testfile);

            // 3.テキストを削除
            TResponse resResource = Http.request("box/dav-delete.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", testBox)
                    .with("path", testfile)
                    .returns();
            resResource.statusCode(HttpStatus.SC_NO_CONTENT);

            // 2.割り当てを解除
            TResponse tresponse = null;
            tresponse = Http.request("cell/link-delete.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("sourceEntity", "Role")
                    .with("sourceKey", "'" + testrole + "'")
                    .with("navPropName", "_ExtCell")
                    .with("navPropKey", "'" + PersoniumCoreUtils.encodeUrlComp(localunitCell1Url) + "'")
                    .with("token", "Bearer " + MASTER_TOKEN)
                    .with("ifMatch", "*")
                    .returns();
            tresponse.statusCode(HttpStatus.SC_NO_CONTENT);

            // ロール削除
            RoleUtils.delete(TEST_CELL2, MASTER_TOKEN, null, testrole);

            // 1.ExtCell更新（元に戻す）
            ExtCellUtils.update(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, httpCell1Url, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * 外部セルのurlがlocalunitの場合でもトークン発行できること.
     */
    @Test
    public final void 外部セルのurlがlocalunitの場合でもトークン発行できること_外部セルにリレーションが割り当てられさらにリレーションにロールが割り当てられている場合() {
        String httpCell1Url = UrlUtils.cellRoot(TEST_CELL1);
        String httpCell2Url = UrlUtils.cellRoot(TEST_CELL2);
        String localunitCell1Url = "personium-localunit:/" + TEST_CELL1 + "/";
        String transCellAccessToken = null;
        String testfile = "testfile.txt";
        String testrole = "transCellTestRole";
        String testrelation = "testRelation";
        String roleUrl = UrlUtils.roleUrl(TEST_CELL2, null, testrole);
        // main box を使用（box1にはACL設定がありテストには不適切であるため）
        String testBox = "__";

        // dcTargetの値がhttpの場合
        try {
            // テスト準備  （MASTER_TOKENで実施）
            // 1.ExtCell更新
            // Setupでセル２に外部セルとして登録されているセル１のhttpのURLをpersonium-localunitに一時的に更新。
            ExtCellUtils.update(MASTER_TOKEN, TEST_CELL2, httpCell1Url, localunitCell1Url, HttpStatus.SC_NO_CONTENT);

            // Role作成
            RoleUtils.create(TEST_CELL2, MASTER_TOKEN, testrole, HttpStatus.SC_CREATED);

            // Relation作成
            // Cell1にRelationを作成する
            JSONObject body = new JSONObject();
            body.put("Name", testrelation);
            body.put("_Box.Name", null);
            RelationUtils.create(TEST_CELL2, MASTER_TOKEN, body, HttpStatus.SC_CREATED);

            // Cell1のExtCellとRelationを結びつけ
            ResourceUtils.linksWithBody(TEST_CELL2, Relation.EDM_TYPE_NAME, testrelation, "null",
                    ExtCell.EDM_TYPE_NAME, UrlUtils.extCellResource(TEST_CELL2, localunitCell1Url),
                    MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
            // Cell1のRelationとRoleを結びつけ
            ResourceUtils.linksWithBody(TEST_CELL2, Relation.EDM_TYPE_NAME, testrelation, "null",
                    Role.EDM_TYPE_NAME, roleUrl, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);

            // 3.リソースを配置
            Http.request("box/dav-put.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("source", "testFileBody")
                    .returns().statusCode(HttpStatus.SC_CREATED);
            // 4.ACL設定 (リソース権限を割り当てる)
            setAcl(TEST_CELL2, testBox, MASTER_TOKEN, testfile, "../__/" + testrole, "read");

            // ここからがテストの実施
            // TEST_CELL1のパスワード認証にてTEST_CELL2宛トークンを発行
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

            // 発行されたトークンをTEST_CELL2で検証
            TResponse res2 =
                Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            JSONObject jsonLocal = res2.bodyAsJson();
            String localCellAccessToken = (String) jsonLocal.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ここで取得できたlocalCellAccessTokenにRole1が割りあたっていることを検証する。

            // テキストファイルが取得ができること(localCellAccessToken)
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("token", localCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

            // テキストファイルが取得ができること(transCellAccessToken)
            Http.request("box/dav-get.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("box", testBox)
                    .with("path", testfile)
                    .with("token", transCellAccessToken)
                    .returns().statusCode(HttpStatus.SC_OK);

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            System.out.println("■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■");
            // 4.ACLを元に戻す
            resetAcl(TEST_CELL2, testBox, MASTER_TOKEN, testfile);

            // 3.テキストを削除
            TResponse resResource = Http.request("box/dav-delete.txt")
                    .with("cellPath", TEST_CELL2)
                    .with("token", MASTER_TOKEN)
                    .with("box", testBox)
                    .with("path", testfile)
                    .returns();
            resResource.statusCode(HttpStatus.SC_NO_CONTENT);

            // Cell1のRelationとRoleの削除
            ResourceUtils.linksDelete(TEST_CELL2, Relation.EDM_TYPE_NAME, testrelation, "null",
                    Role.EDM_TYPE_NAME, "_Box.Name=null,Name='" + testrole + "'", MASTER_TOKEN);

            // Cell1のExtCellとRelationの削除
            ResourceUtils.linksDelete(TEST_CELL2, Relation.EDM_TYPE_NAME, testrelation,
                    "null", ExtCell.EDM_TYPE_NAME,
                    "'" + PersoniumCoreUtils.encodeUrlComp(localunitCell1Url) + "'", MASTER_TOKEN);

            // Cell1のRelationを削除
            RelationUtils.delete(TEST_CELL2, MASTER_TOKEN, testrelation, null, HttpStatus.SC_NO_CONTENT);

            // ロール削除
            RoleUtils.delete(TEST_CELL2, MASTER_TOKEN, null, testrole);

            // 1.ExtCell更新（元に戻す）
            ExtCellUtils.update(MASTER_TOKEN, TEST_CELL2, localunitCell1Url, httpCell1Url, HttpStatus.SC_NO_CONTENT);
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
            return DavResourceUtils.setAcl(token, cellName, boxName, collection, acl, HttpStatus.SC_OK);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ８．トークン認証ー他人セルトークン取得＿アクセス制御.
     */
    @Test
    public final void トークン認証ー他人セルトークン取得＿アクセス制御() {
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
    public final void トークン認証ー他人セルトークン取得＿トークン発行のテスト() {
        // セルに対してパスワード認証
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
    public final void スキーマ付きートークン認証ートランセルトークン取得() {
        // セルに対してパスワード認証
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
                PersoniumCoreUtils.createBasicAuthzHeader(UrlUtils.cellRoot(TEST_APP_CELL1),
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
    public final void スキーマ認証ートークン取得ー他人セルトークン() {
        // セルに対してパスワード認証
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

        String schemaTransCellAccessTokenHeader = PersoniumCoreUtils.createBasicAuthzHeader(
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
    public final void パスワード認証リフレッシュトークンートランセル＿アクセス制御() {
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
    public final void パスワード認証リフレッシュトークンートランセル＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
            CellLocalRefreshToken rCellLocalToken = CellLocalRefreshToken.parse(refreshToken,
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
            CellLocalRefreshToken rCellLocalToken2 = CellLocalRefreshToken.parse(refreshToken2,
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
    public final void パスワード認証リフレッシュトークンー自セルトークン＿アクセス制御() {
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
    public final void パスワード認証リフレッシュトークンー自セルトークン＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            String refreshToken = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
            CellLocalRefreshToken rCellLocalToken = CellLocalRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            Long lastAuthenticatedTime = AuthTestCommon.getAccountLastAuthenticated(TEST_CELL1, "account1");
            TResponse res = Http.request("authn/refresh-cl.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("refresh_token", refreshToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            AuthTestCommon.accountLastAuthenticatedNotUpdatedCheck(TEST_CELL1, "account1", lastAuthenticatedTime);
            String refreshToken2 = (String) res.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            CellLocalRefreshToken rCellLocalToken2 = CellLocalRefreshToken.parse(refreshToken2,
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
    public final void スキーマ付きパスワード認証リフレッシュトークンートランセル() {
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
            CellLocalRefreshToken rCellLocalToken = CellLocalRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res3 =
                    Http.request("authn/refresh-tc.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("refresh_token", refreshToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            CellLocalRefreshToken rCellLocalToken2 = CellLocalRefreshToken.parse(refreshToken2,
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
    public final void スキーマ付きパスワード認証リフレッシュトークンー自セルトークン() {
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
            CellLocalRefreshToken rCellLocalToken = CellLocalRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL1));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res3 =
                    Http.request("authn/refresh-cl.txt")
                            .with("remoteCell", TEST_CELL1)
                            .with("refresh_token", refreshToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            CellLocalRefreshToken rCellLocalToken2 = CellLocalRefreshToken.parse(refreshToken2,
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
    public final void トークン認証リフレッシュトークンートランセル＿アクセス制御() {
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
    public final void トークン認証リフレッシュトークンートランセル＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
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
            TransCellRefreshToken rToken = TransCellRefreshToken.parse(refreshToken,
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
            TransCellRefreshToken rToken2 = TransCellRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken.getRoles().get(0).createUrl(), rToken2.getRoles().get(0).createUrl());
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
    public final void トークン認証リフレッシュトークンー他人セルトークン＿アクセス制御() {
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
    public final void トークン認証リフレッシュトークンー他人セルトークン＿トークン発行のテスト() {
        try {
            // セルに対してパスワード認証
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
            TransCellRefreshToken rToken = TransCellRefreshToken.parse(refreshToken,
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
            TransCellRefreshToken rToken2 = TransCellRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken.getRoles().get(0).createUrl(), rToken2.getRoles().get(0).createUrl());
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
    public final void スキーマ付きトークン認証リフレッシュトークンートランセル() {
        try {
            // セルに対してパスワード認証
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
            TResponse res3 = Http.request("authn/saml-cl-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            TransCellRefreshToken rToken1 = TransCellRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // アプリセルに対して認証
            TResponse res4 =
                    Http.request("authn/refresh-tc.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .with("p_target", UrlUtils.cellRoot(TEST_APP_CELL1))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            TransCellRefreshToken rToken2 = TransCellRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken1.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken1.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken1.getRoles().get(0).createUrl(), rToken2.getRoles().get(0).createUrl());
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
    public final void スキーマ付きトークン認証リフレッシュトークンー他人セルトークン() {
        try {
            // セルに対してパスワード認証
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
            TResponse res3 = Http.request("authn/saml-cl-cp.txt")
                    .with("remoteCell", TEST_CELL2)
                    .with("assertion", transCellAccessToken)
                    .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                    .with("client_secret", schemaTransCellAccessToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            String refreshToken = (String) res3.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            TransCellRefreshToken rToken1 = TransCellRefreshToken.parse(refreshToken,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンの作成時にミリ秒を秒に丸めてトークン文字列化しているため1秒停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                fail();
            }

            // Refresh
            TResponse res4 =
                    Http.request("authn/refresh-cl.txt")
                            .with("remoteCell", TEST_CELL2)
                            .with("refresh_token", refreshToken)
                            .returns()
                            .statusCode(HttpStatus.SC_OK);
            String refreshToken2 = (String) res4.bodyAsJson().get(OAuth2Helper.Key.REFRESH_TOKEN);
            TransCellRefreshToken rToken2 = TransCellRefreshToken.parse(refreshToken2,
                    UrlUtils.cellRoot(TEST_CELL2));

            // リフレッシュトークンが更新されている事をチェック
            assertTrue(!refreshToken.equals(refreshToken2));
            // トークンのIDが変更されている事（トークン作成時間を含んでいるため、異なるハズ）
            assertTrue(!rToken1.getId().equals(rToken2.getId()));
            // トークンの内容が更新されていないこと
            assertEquals(rToken1.getIssuer(), rToken2.getIssuer());
            assertEquals(rToken1.getRoles().get(0).createUrl(), rToken2.getRoles().get(0).createUrl());
            assertEquals(rToken1.getSchema(), rToken2.getSchema());
            assertEquals(rToken1.getSubject(), rToken2.getSubject());

        } catch (TokenParseException e) {
            fail();
        }

    }
}

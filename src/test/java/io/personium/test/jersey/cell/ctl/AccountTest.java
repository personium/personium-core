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
package io.personium.test.jersey.cell.ctl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Accountの作成のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class AccountTest extends ODataCommon {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(AccountTest.class);

    static String cellName = "cellname";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        cellName = cellName + Long.toString(Calendar.getInstance().getTimeInMillis());
        log.debug(AccountTest.class.toString() + " BeforeClass:cellName=" + cellName);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        Http.request("cell-create.txt")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("cellPath", cellName)
                .returns();
        super.setResponse(null);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
    }

    /**
     * 以下の流れを確認する. Account追加=>参照=>更新. Role追加=>参照=>更新. Account・Role紐付け追加=>参照=>更新. 紐付け削除=>Role削除=>Account削除.
     * @throws TokenParseException TokenParseException
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void crud() throws TokenParseException, TokenDsigException, TokenRootCrtException {
        String testAccountName = "test_account";
        String testAccountPass = "password";
        String accUrl = this.createAccount(testAccountName, testAccountPass);
        log.debug(accUrl);

        String testRoleName = "testRole";
        String roleUrl = this.createRole(testRoleName);
        log.debug(roleUrl);

        // アカウント・ロールの紐付けC
        Http.request("link-account-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", testAccountName)
                .with("roleUrl", roleUrl)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // DcRequest req = DcRequest.post(UrlUtils.accountLinks(cellName, testAccountName));
        // req.header(HttpHeaders.AUTHORIZATION, MASTER_TOKEN).addJsonBody("uri", roleUrl);
        // DcResponse res = request(req);
        // assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

        // アカウント・ロールの紐付けのR(全件取得)
        // TODO 一件取得が無いのはバグ。
        log.debug("koooooooo");

        PersoniumRequest req = PersoniumRequest.get(UrlUtils.accountLinks(cellName, testAccountName))
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).header(HttpHeaders.ACCEPT, "application/json");
        PersoniumResponse res = request(req);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        JSONObject linkJson = res.bodyAsJson();
        JSONArray linksResults = getResultsFromEntitysResponse(linkJson);
        JSONObject linkJson2 = (JSONObject) linksResults.get(0);
        assertEquals(roleUrl, linkJson2.get("uri"));

        // アカウント・ロールの紐付けのU
        // TODO UPDATEは未実装のためいずれ。。。

        // BOX作成
        String boxName = "testbox";
        String boxSchema = "https://example.com/hogecell/";
        createBox2(boxName, boxSchema);

        // パスワード認証。セルトークン取得。
        // grant_type=password&username={username}&password={password}
        req = PersoniumRequest.post(UrlUtils.auth(cellName));
        String authBody =
                String.format("grant_type=password&username=%s&password=%s", testAccountName, testAccountPass);
        req.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded").addStringBody(authBody);
        res = request(req);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        JSONObject token = res.bodyAsJson();
        assertNotNull(token);

        // パスワード認証。トランスセルトークン取得
        req = PersoniumRequest.post(UrlUtils.auth(cellName));
        authBody = String.format("grant_type=password&username=%s&password=%s&p_target=%s",
                testAccountName, testAccountPass, "https://example.com/testcell");
        req.header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded").addStringBody(authBody);
        res = request(req);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        token = res.bodyAsJson();
        assertNotNull(token);
        String accessToken = null;
        accessToken = (String) token.get("access_token");
        assertNotNull(accessToken);
        TransCellAccessToken tcat = TransCellAccessToken.parse(accessToken);
        String url = UrlUtils.cellRoot(cellName);
        assertEquals(testRoleName, tcat.getRoleList().get(0).getName());
        assertEquals(url + "__role/__/" + testRoleName, tcat.getRoleList().get(0).schemeCreateUrl(url));
        assertEquals(url, tcat.getIssuer());
        assertEquals(url + "#" + testAccountName, tcat.getSubject());

        // アカウント・ロールの紐付けのD
        String roleId = roleUrl.substring(roleUrl.indexOf("'") + 1);
        roleId = roleId.substring(0, roleId.indexOf("'"));
        req = PersoniumRequest.delete(UrlUtils.accountLink(cellName, testAccountName, roleId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        res = request(req);
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

        // RoleのD
        req = PersoniumRequest.delete(roleUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        res = request(req);
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

        // AccountのD
        req = PersoniumRequest.delete(accUrl)
                .header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        res = request(req);
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Normal test.
     * Delete account linked with role.
     */
    @Test
    public void normal_delete_account_linked_with_role() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String accountName = "account";
        String pass = "password";
        String roleName = "role";
        String boxName = null;
        try {
            // 準備。アカウント、ロール作ってリンクさせる。
            AccountUtils.create(token, cellName, accountName, pass, HttpStatus.SC_CREATED);
            RoleUtils.create(cellName, token, roleName, boxName, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, boxName, token, HttpStatus.SC_NO_CONTENT);

            AccountUtils.delete(cellName, token, accountName, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinks(cellName, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, boxName, token, -1);
            RoleUtils.delete(accountName, token, roleName, boxName, -1);
            AccountUtils.delete(cellName, token, accountName, -1);
        }

    }

    /**
     * アカウントのテスト.
     * @param name アカウント名
     * @param pass パスワード
     * @return 作成されたAccountのURL
     */
    String createAccount(final String name, final String pass) {
        // AccountのC
        TResponse res = Http.request("account-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", name)
                .with("password", pass)
                .returns();

        String accLocHeader = res.getLocationHeader();
        String etag = res.getHeader(HttpHeaders.ETAG);

        // AccountのR
        res = Http.request("account-retrieve.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", name)
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .contentType("application/json");

        JSONObject accountJson = res.bodyAsJson();
        accountJson = getResultsFromEntityResponse(accountJson);
        assertEquals(name, accountJson.get("Name"));
        assertNotNull(accountJson.get("__published"));
        assertNotNull(accountJson.get("__updated"));
        assertEquals(etag, ((JSONObject) accountJson.get("__metadata")).get("etag"));

        // AccountのU
        // TODO UPDATEは未実装のためいずれ。。。

        return accLocHeader;
    }

    /**
     * ロールを作成するテスト.
     * @param testRoleName
     * @return 作成されたRoleのId
     */
    @SuppressWarnings("unchecked")
    String createRole(final String testRoleName) {
        // RoleのC
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", null);

        TResponse res = Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED)
                .inspect(new TResponse.Inspector() {
                    public void inspect(TResponse resp) {
                    }
                });

        String roleLocHeader = res.getLocationHeader();

        assertNotNull(roleLocHeader);

        // RoleのR
        res = Http.request("role-retrieve.txt")
                .with("cellPath", cellName)
                .with("rolename", testRoleName)
                .with("boxname", "null")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .contentType("application/json");

        JSONObject roleJson = res.bodyAsJson();
        roleJson = getResultsFromEntityResponse(roleJson);
        assertEquals(testRoleName, roleJson.get("Name"));
        assertNull(roleJson.get("_Box.Name"));
        assertNotNull(roleJson.get("__published"));
        assertNotNull(roleJson.get("__updated"));
        // assertEquals(etag, ((JSONObject) roleJson.get("__metadata")).get("etag"));

        // RoleのU
        // TODO UPDATEは未実装のためいずれ。。。
        return roleLocHeader;
    }

    /**
     * BOX作成テスト.
     * @param boxName ボックス名
     * @param boxSchema ボックススキーマ
     * @return boxURL
     */
    protected String createBox2(final String boxName, final String boxSchema) {
        // BOXのC
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(cellName, Box.EDM_TYPE_NAME));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", boxName);
        if (boxSchema != null) {
            req.addJsonBody("Schema", boxSchema);
        }
        PersoniumResponse res = request(req);
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        String url = res.getFirstHeader(HttpHeaders.LOCATION);
        assertNotNull(url);
        return url;
    }

    /**
     * ファイルのすべてを読み込む.
     * @param inputStream 読み込むストリーム
     * @return バイト配列
     */
    public byte[] readAll(InputStream inputStream) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int len = inputStream.read(buffer);
                if (len < 0) {
                    break;
                }
                bout.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bout.toByteArray();
    }

    /**
     * @param body
     * @return results配下のJSONオブジェクト
     */
    private JSONObject getResultsFromEntityResponse(JSONObject body) {
        assertNotNull(body);
        JSONObject d = (JSONObject) body.get("d");
        assertNotNull(d);
        JSONObject results = (JSONObject) d.get("results");
        assertNotNull(results);
        return results;
    }

    /**
     * @param body
     * @return results配下のJSONオブジェクト
     */
    private JSONArray getResultsFromEntitysResponse(JSONObject body) {
        assertNotNull(body);
        JSONObject d = (JSONObject) body.get("d");
        assertNotNull(d);
        JSONArray results = (JSONArray) d.get("results");
        assertNotNull(results);
        return results;
    }
}

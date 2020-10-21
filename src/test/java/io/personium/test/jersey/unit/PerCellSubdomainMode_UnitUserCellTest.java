/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.io.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Test of UnitUser under the condition of Per Cell Subdomain Mode.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PerCellSubdomainMode_UnitUserCellTest extends PersoniumTest {

    private static Logger log = LoggerFactory.getLogger(PerCellSubdomainMode_UnitUserCellTest.class);

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

    private static String unitUserCellUrl;
    private static String unitUserToken = null;

    /**
     * Constructor. テスト対象のパッケージをsuperに渡す必要がある
     */
    public PerCellSubdomainMode_UnitUserCellTest() {
        super(new PersoniumCoreApplication());
    }


    private static void createUnitUserAccount() {
        String accountODataUrl = unitUserCellUrl + "__ctl/Account";
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost post = new HttpPost(accountODataUrl);
            post.addHeader(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            post.addHeader(io.personium.common.utils.CommonUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL,
                    UNIT_USER_ACCOUNT_PASS);
            String jsonStr = Json.createObjectBuilder().add("Name", UNIT_USER_ACCOUNT).build().toString();
            HttpEntity entity = new StringEntity(jsonStr);
            post.setEntity(entity);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static void deleteUnitUserAccount() {
        // Delete Unit User Account
        String accountODataUrl = unitUserCellUrl + "__ctl/Account";
        HttpDelete del = new HttpDelete(accountODataUrl + "(%27" + UNIT_USER_ACCOUNT + "%27)");
        del.addHeader(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)){
            client.execute(del);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String getUnitUserToken() {
        String tokenEndpoint = unitUserCellUrl + "__token";
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost post = new HttpPost(tokenEndpoint);
            post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
            post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=password&username=");
            sb.append(UNIT_USER_ACCOUNT);
            sb.append("&password=");
            sb.append(UNIT_USER_ACCOUNT_PASS);
            sb.append("&p_target=");
            sb.append(UrlUtils.unitRoot());
            HttpEntity entity = new StringEntity(sb.toString());
            post.setEntity(entity);
            try (CloseableHttpResponse res = client.execute(post)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                JsonObject jo = Json.createReader(new InputStreamReader(res.getEntity().getContent(), Charsets.UTF_8)).readObject();
                return jo.getString(OAuth2Helper.Key.ACCESS_TOKEN);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static void deleteUnitAdminRole() {
        //RoleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, unitAdminRole, null);
        String roleODataUrl = unitUserCellUrl + "__ctl/Role(%27" + unitAdminRole + "%27)";
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpDelete del = new HttpDelete(roleODataUrl);
            del.addHeader(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            client.execute(del);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void linkUuAccountAndUnitAdminRole() {
        //            LinksUtils.createLinks(UNIT_USER_CELL, Account.EDM_TYPE_NAME, UNIT_USER_ACCOUNT, null, Role.EDM_TYPE_NAME,
        //                    unitAdminRole, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
        String roleODataUrl = unitUserCellUrl + "__ctl/Role(Name=%27" + unitAdminRole + "%27)";
        String accountRoleLinkUrl = unitUserCellUrl + "__ctl/Account(%27" + UNIT_USER_ACCOUNT + "%27)/$links/_Role";
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost post = new HttpPost(accountRoleLinkUrl);
            post.addHeader(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            String jsonStr = Json.createObjectBuilder()
                    .add("uri", roleODataUrl)
                    .build().toString();
            HttpEntity entity = new StringEntity(jsonStr);
            post.setEntity(entity);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void createUnitAdminRole() {
//          RoleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, unitAdminRole,
//          null, HttpStatus.SC_CREATED);
        String roleODataUrl = unitUserCellUrl + "__ctl/Role";
        try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
            HttpPost post = new HttpPost(roleODataUrl);
            post.addHeader(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            String jsonStr = Json.createObjectBuilder().add("Name", unitAdminRole).build().toString();
            HttpEntity entity = new StringEntity(jsonStr);
            post.setEntity(entity);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Before class.
     * @throws Exception Unintended exception in test
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Override issuers in unitconfig.
        urlModeBackup = PersoniumUnitConfig.get(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED);
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED,
                "false");
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


        unitUserCellUrl = UriUtils.resolveLocalUnit("personium-localunit:" + UNIT_USER_CELL + ":/");

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
        CellUtils.create(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, unitUserCellUrl, HttpStatus.SC_CREATED);

        // ユニットユーザトークンを使えば取得可能なことを確認
        CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME, unitUserCellUrl, HttpStatus.SC_OK);

        // オーナーが異なるユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと削除できないことを確認
        CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, Setup.OWNER_HMC, HttpStatus.SC_FORBIDDEN);

        // オーナーが一致するユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと削除できることを確認
        CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, unitUserCellUrl,
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
            // Create  Account
            createUnitUserAccount();
            // retrieve a Unit User Token
            unitUserToken = getUnitUserToken();

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
            // Delete Unit User Account
            deleteUnitUserAccount();


            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
            // Delete Unit User Cell for the tests
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
            // Creating Unit User Cell for this test case.
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Add Account
            // Create  Account
            createUnitUserAccount();
            // retrieve a Unit User Token
            unitUserToken = getUnitUserToken();
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
                    unitUserCellUrl + "#" + UNIT_USER_ACCOUNT, HttpStatus.SC_OK);

            // オーナーが異なるユニットユーザトークン（マスタートークンのヘッダ指定での降格を利用）を使うと取得できないことを確認
            CellUtils.get(CREATE_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    UrlUtils.subjectUrl(UNIT_USER_CELL, "hoge"), HttpStatus.SC_FORBIDDEN);

            // ユニットユーザートークンを使ってセル削除ができることを確認
            CellUtils.delete(unitUserToken, CREATE_CELL, HttpStatus.SC_NO_CONTENT);
        } finally {
            // アカウント削除
            deleteUnitUserAccount();

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
            createUnitUserAccount();

            // Add unitAdminRole
            createUnitAdminRole();

            // linkUnit AdminRole with account
            linkUuAccountAndUnitAdminRole();


            // 認証（ユニットユーザートークン取得）
            unitUserToken = getUnitUserToken();

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
            // ロール削除（BOXに結びつかない）
            deleteUnitAdminRole();

            // アカウント削除
            deleteUnitUserAccount();
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, CREATE_CELL, -1);
        }
    }




    /**
     * セルレベルPROPPATCHをユニットユーザトークンで実行可能なことを確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public void セルレベルPROPPATCHをユニットユーザトークンで実行可能なことを確認() throws TokenParseException {
        try {
            // 本テスト用セルの作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // アカウント追加
            createUnitUserAccount();
            // 認証（ユニットユーザートークン取得）
            unitUserToken = getUnitUserToken();

            // アカウントにユニット昇格権限付
            try (CloseableHttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_ALWAYS_LOCAL)) {
//                BasicHttpEntityEnclosingRequest proppatch = new BasicHttpEntityEnclosingRequest("PROPPATCH", unitUserCellUrl);
                String ppStr = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                        "<D:propertyupdate xmlns:D=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\">\n" +
                        "  <D:set>\n" +
                        "    <D:prop>\n" +
                        "      <p:ownerRepresentativeAccounts><p:account>account1</p:account><p:account>account2</p:account></p:ownerRepresentativeAccounts>\n" +
                        "    </D:prop>\n" +
                        "  </D:set>\n" +
                        "</D:propertyupdate>\n";
                HttpEntity entity = new StringEntity(ppStr);
                RequestBuilder proppatch = RequestBuilder.create("PROPPATCH")
                        .setUri(unitUserCellUrl)
                        .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + unitUserToken)
                        .addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_XML.getMimeType())
                        .addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_XML.getMimeType())
                        .setEntity(entity);
                try (CloseableHttpResponse res = client.execute(null, proppatch.build())) {
                    assertEquals(207, res.getStatusLine().getStatusCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {

            // アカウント削除
            deleteUnitUserAccount();
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL, -1);
        }


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


}

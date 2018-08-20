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
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.CellLocalRefreshToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * スキーマ認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class SchemaAuthTest extends PersoniumTest {

    static final String TEST_CELL1 = "testcell1";
    static final String TEST_CELL2 = "testcell2";
    static final String TEST_BOX1 = "box1";
    static final String TEST_APP_CELL1 = "schema1";
    static final String DAV_COLLECTION = "setdavcol/";
    static final String ODATA_COLLECTION = "setodata";
    static final String DAV_RESOURCE = "dav.txt";
    static final String ACL_SETTING_FILE = "box/acl.txt";
    static final String ACL_AUTH_TEST_SETTING_FILE = "box/acl-setscheme.txt";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String DEFAULT_PRIVILEGE = "<D:read/></D:privilege><D:privilege><D:write/>";

    /**
     * コンストラクタ.
     */
    public SchemaAuthTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * スキーマ無しパスワード認証でセルローカルとリフレッシュトークン.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void スキーマ無しパスワード認証でセルローカルとリフレッシュトークン() throws TokenParseException {
        // 認証
        JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account2", "password2", -1);

        String issuer = UrlUtils.cellRoot(TEST_CELL1);

        // トークンチェック
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        AccountAccessToken aToken = AccountAccessToken.parse(tokenStr, issuer);
        assertNotNull(aToken.getSchema());
        String rTokenStr = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        CellLocalRefreshToken rToken = CellLocalRefreshToken.parse(rTokenStr, issuer);
        assertNotNull(rToken.getSchema());

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.cheacResourcesWithNoneSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr, TEST_CELL1);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL1);
        }
    }

    /**
     * リソースに対して、スキーマ無しのトークンでアクセス制御を確認.
     */
    private void cheacResourcesWithNoneSchema(String path, String file, String token, String cellPath) {

        // スキーマ設定無し→アクセス可能
        this.cheackResourceSchema(path, file, token, "", HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
        // スキーマ設定NONE→アクセス可能
        this.cheackResourceSchema(path, file, token,
                OAuth2Helper.SchemaLevel.NONE, HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
        // スキーマ設定PUBLIC→アクセス不可
        this.cheackResourceSchema(path, file, token,
                OAuth2Helper.SchemaLevel.PUBLIC, HttpStatus.SC_FORBIDDEN, Setup.TEST_BOX1, cellPath);
        // スキーマ設定CONFIDENTIAL→アクセス不可
        this.cheackResourceSchema(path, file, token,
                OAuth2Helper.SchemaLevel.CONFIDENTIAL, HttpStatus.SC_FORBIDDEN, Setup.TEST_BOX1, cellPath);
    }

    /**
     * リソースに対して、スキーマ付トークンでアクセス制御を確認.
     */
    private void checkResourcesWithSchema(String path, String file, String token, String boxName, String cellPath) {

        // スキーマ設定無し→アクセス可能
        this.cheackResourceSchema(path, file, token, "", HttpStatus.SC_OK, boxName, cellPath);
        // スキーマ設定NONE→アクセス可能
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.NONE, HttpStatus.SC_OK,
                boxName, cellPath);
        // スキーマ設定PUBLIC→アクセス可能
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.PUBLIC, HttpStatus.SC_OK,
                boxName, cellPath);
        // スキーマ設定CONFIDENTIAL→アクセス不可
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.CONFIDENTIAL, HttpStatus.SC_FORBIDDEN,
                boxName, cellPath);
    }

    /**
     * リソースに対していconfidentialRoleスキーマ付トークンでアクセス制御を確認.
     */
    private void checkResourcesWithWithConfidentialSchema(String path, String file, String token, String cellPath) {

        // すべてのスキーマ設定でアクセス可能
        this.cheackResourceSchema(path, file, token, "", HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.NONE,
                HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.PUBLIC,
                HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
        this.cheackResourceSchema(path, file, token, OAuth2Helper.SchemaLevel.CONFIDENTIAL,
                HttpStatus.SC_OK, Setup.TEST_BOX1, cellPath);
    }

    /**
     * リソースアクセスのスキーマ認証制御の確認.
     */
    private void cheackResourceSchema(String path, String file, String token,
            String level, int status, String boxName, String cellPath) {
        // ACLでスキーマレベル設定
        this.setAclSchema(boxName, path, level, cellPath);
        // リソースアクセス
        ResourceUtils.accessResource(path + file, token, status, boxName, cellPath);
    }

    /**
     * ACLによるスキーマ設定.
     * @param box ボックス名
     * @param path コレクション以下のパス
     * @param level スキーマレベル
     * @param cellPath セル
     */
    private void setAclSchema(String box, String path, String level, String cellPath) {
        String settingFile = ACL_AUTH_TEST_SETTING_FILE;
        if (level.isEmpty()) {
            settingFile = "box/acl-setscheme-none-schema-level.txt";
        }
        DavResourceUtils.setACL(cellPath, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, path,
                settingFile, box, level);
    }

    /**
     * ACLによるスキーマ設定（ACL設定を指定する場合）.
     * @param box ボックス名
     * @param path コレクション以下のパス
     * @param roleBaseUrl ACLのBaseUrl
     * @param level スキーマレベル
     * @param cellPath セル
     * @param role ACLのPricipal
     * @param privilege ACLのprivilege
     */
    private void setAclSchema(String box, String path, String roleBaseUrl, String level, String cellPath,
            String role, String privilege) {
        Http.request("box/acl-setting.txt")
                .with("cellPath", cellPath)
                .with("path", path)
                .with("box", box)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", roleBaseUrl)
                .with("colname", "")
                .with("level", level)
                .with("role", role)
                .with("privilege", privilege)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * スキーマ無しパスワード認証でトランスセルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void スキーマ無しパスワード認証でトランスセルトークンのチェック() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {
        // 認証
        JSONObject json = getTransTokenByPassAuth("account2", "password2");

        // トークンチェック
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        TransCellAccessToken aToken = TransCellAccessToken.parse(tokenStr);
        assertNotNull(aToken.getSchema());
    }

    /**
     * スキーマ付パスワード認証でセルローカルとリフレッシュトークン.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void スキーマ付パスワード認証でセルローカルとリフレッシュトークン() throws TokenParseException {
        String tokenStr = checkCellLocalWithSchema("account0", "password0",
                TEST_APP_CELL1, UrlUtils.cellRoot(TEST_APP_CELL1));

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.checkResourcesWithSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr, Setup.TEST_BOX1, TEST_CELL1);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, DAV_COLLECTION,
                    ACL_AUTH_TEST_SETTING_FILE, Setup.TEST_BOX1, OAuth2Helper.SchemaLevel.NONE);
        }
    }

    /**
     * スキーマ付パスワード認証でトランスセルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void スキーマ付パスワード認証でトランスセルトークンのチェック() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {
        checkTransTokenWithSchema("account0", "password0", UrlUtils.cellRoot(TEST_APP_CELL1));
    }

    /**
     * confidentialRoleスキーマ付パスワード認証でセルローカルとリフレッシュトークン.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void confidentialRoleスキーマ付パスワード認証でセルローカルとリフレッシュトークン() throws TokenParseException {
        String tokenStr = checkCellLocalWithSchema("account1", "password1",
                TEST_APP_CELL1, UrlUtils.cellRoot(TEST_APP_CELL1) + OAuth2Helper.Key.CONFIDENTIAL_MARKER);

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.checkResourcesWithWithConfidentialSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr, TEST_CELL1);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL1);
        }

    }

    /**
     * confidentialRoleスキーマ付パスワード認証でトランスセルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void confidentialRoleスキーマ付パスワード認証でトランスセルトークンのチェック() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {
        checkTransTokenWithSchema("account1", "password1",
                UrlUtils.cellRoot(TEST_APP_CELL1) + OAuth2Helper.Key.CONFIDENTIAL_MARKER);
    }

    /**
     * スキーマ無しトークン認証でセルローカルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void スキーマ無しトークン認証でセルローカルトークンのチェック() throws TokenParseException {
        // 認証
        JSONObject json = getTransTokenByPassAuth("account2", "password2");

        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        TResponse res2 = Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", tokenStr)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        String issuer = UrlUtils.cellRoot(TEST_CELL2);

        JSONObject json2 = res2.bodyAsJson();
        String tokenStr2 = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);
        CellLocalAccessToken aToken = CellLocalAccessToken.parse(tokenStr2, issuer);
        assertNotNull(aToken.getSchema());

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.cheacResourcesWithNoneSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr2, TEST_CELL2);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL2);
        }
    }

    /**
     * スキーマ付トークン認証でセルローカルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void スキーマ付トークン認証でセルローカルトークンのチェック() throws TokenParseException {
        String tokenStr = cheackTokenAuth("account0", "password0",
                UrlUtils.cellRoot(TEST_APP_CELL1));

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.checkResourcesWithSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr, Setup.TEST_BOX1, TEST_CELL2);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL2);
        }
    }

    /**
     * confidentialRoleスキーマ付トークン認証でセルローカルトークンのチェック.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void confidentialRoleスキーマ付トークン認証でセルローカルトークンのチェック() throws TokenParseException {
        String tokenStr = cheackTokenAuth("account1", "password1",
                UrlUtils.cellRoot(TEST_APP_CELL1)
                        + OAuth2Helper.Key.CONFIDENTIAL_MARKER);

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            this.checkResourcesWithWithConfidentialSchema(DAV_COLLECTION, DAV_RESOURCE, tokenStr, TEST_CELL2);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL2);
        }
    }

    /**
     * スキーマ認証時に無効なトークンを検出した場合401が返ることの確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void スキーマ認証時に無効なトークンを検出した場合401が返ることの確認() throws TokenParseException {
        // テキトーなトークン
        String token = "hogeracho";
        try {
            // スキーマ認証レベル設定
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.PUBLIC, TEST_CELL1);

            // リソースアクセス。401になるはず。
            ResourceUtils.accessResource(DAV_COLLECTION + DAV_RESOURCE, token, HttpStatus.SC_UNAUTHORIZED,
                    Setup.TEST_BOX1, TEST_CELL1);

        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, DAV_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL1);
        }
    }

    /**
     * Boxレベル$batchでのスキーマ認証制御の確認.
     * @throws TokenParseException TokenParseException
     */
    @Test
    public final void Boxレベル$batchでのスキーマ認証制御の確認() throws TokenParseException {

        // スキーマ無しの認証トークン取得
        String token = ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account0", "password0");
        // スキーマ認証トークン取得
        String schemaToken = this.checkCellLocalWithSchema("account0", "password0",
                TEST_APP_CELL1, UrlUtils.cellRoot(TEST_APP_CELL1));
        try {
            // スキーマ認証レベル設定
            this.setAclSchema(TEST_BOX1, ODATA_COLLECTION, OAuth2Helper.SchemaLevel.PUBLIC, TEST_CELL1);

            // スキーマ認証チェックエラーになるはず
            UserDataUtils.batch(TEST_CELL1, TEST_BOX1, ODATA_COLLECTION, AuthBatchTest.BOUNDARY,
                    AuthBatchTest.TEST_BODY, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_ACCEPTED);

            // スキーマ認証チェックエラーになるはず
            UserDataUtils.batch(TEST_CELL1, TEST_BOX1, ODATA_COLLECTION, AuthBatchTest.BOUNDARY,
                    AuthBatchTest.TEST_BODY, token, HttpStatus.SC_FORBIDDEN);

            // リクエストが受け付けられるはず
            UserDataUtils.batch(TEST_CELL1, TEST_BOX1, ODATA_COLLECTION, AuthBatchTest.BOUNDARY,
                    AuthBatchTest.TEST_BODY, schemaToken, HttpStatus.SC_ACCEPTED);
        } finally {
            // ACLとスキーマレベル設定を元に戻す
            this.setAclSchema(Setup.TEST_BOX1, ODATA_COLLECTION, OAuth2Helper.SchemaLevel.NONE, TEST_CELL1);
        }
    }

    /**
     * スキーマレベル設定の継承ー自分の設定が優先されること.
     */
    @Test
    public final void スキーマレベル設定の継承ー自分の設定が優先されること() {
        try {
            // ACL設定
            this.setACL(TEST_BOX1, "", ACL_SETTING_FILE);

            // スキーマレベル設定
            this.setAclSchema(TEST_BOX1, "", OAuth2Helper.SchemaLevel.PUBLIC, TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION, "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION + DAV_RESOURCE, OAuth2Helper.SchemaLevel.NONE, TEST_CELL1);

            // データセルに自分セルローカルトークン認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account4", "password4", -1);
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ボックスにアクセス
            // publicが設定されているのでアクセス不可。
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_FORBIDDEN, Setup.TEST_BOX1, TEST_CELL1);

            // コレクションにアクセス
            // 親にpublicが設定されているのでアクセス不可。
            ResourceUtils.accessResource(DAV_COLLECTION, tokenStr, HttpStatus.SC_FORBIDDEN, Setup.TEST_BOX1,
                    TEST_CELL1);

            // データアクセス
            // アクセスするファイルのスキーマ設定レベルがNONEなのでスキーマ無しでアクセスできる。
            ResourceUtils.accessResource(DAV_COLLECTION + DAV_RESOURCE, tokenStr, HttpStatus.SC_OK,
                    Setup.TEST_BOX1, TEST_CELL1);

        } finally {
            // スキーマレベル設定を戻す
            this.setAclSchema(TEST_BOX1, "", "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION, "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION + DAV_RESOURCE, "", TEST_CELL1);

            // ACL設定を元に戻す
            this.setACL(TEST_BOX1, "", ACL_AUTH_TEST_SETTING_FILE);
        }
    }

    /**
     * スキーマレベル設定の継承ー自分に設定が無い場合親の設定が有効になること.
     */
    @Test
    public final void スキーマレベル設定の継承ー自分に設定が無い場合親の設定が有効になること() {
        try {
            // ACL設定
            this.setACL(TEST_BOX1, "", ACL_SETTING_FILE);

            this.setAclSchema(TEST_BOX1, "", "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION, "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION + DAV_RESOURCE, "", TEST_CELL1);

            // データセルに自分セルローカルトークン認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account4", "password4", -1);
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // データアクセス（設定変更前）
            // スキーマ設定レベルが親含め設定されていないのでアクセス可能。
            ResourceUtils.accessResource(DAV_COLLECTION + DAV_RESOURCE, tokenStr, HttpStatus.SC_OK,
                    Setup.TEST_BOX1, TEST_CELL1);

            // アクセスするリソースの親にスキーマレベル設定
            this.setAclSchema(TEST_BOX1, "", OAuth2Helper.SchemaLevel.PUBLIC, TEST_CELL1);

            // データアクセス（設定変更後）
            // 親にスキーマ設定レベルが設定されたのでアクセスできなくなる。
            ResourceUtils.accessResource(DAV_COLLECTION + DAV_RESOURCE, tokenStr, HttpStatus.SC_FORBIDDEN,
                    Setup.TEST_BOX1, TEST_CELL1);

        } finally {
            // スキーマレベル設定を戻す
            this.setAclSchema(TEST_BOX1, "", "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION, "", TEST_CELL1);
            this.setAclSchema(TEST_BOX1, DAV_COLLECTION + DAV_RESOURCE, "", TEST_CELL1);

            // ACL設定を元に戻す
            this.setACL(TEST_BOX1, "", ACL_AUTH_TEST_SETTING_FILE);
        }
    }

    /**
     * スキーマレベル設定の継承ーデフォルトはスキーマ認証不要であることの確認.
     */
    @Test
    public final void スキーマレベル設定の継承ーデフォルトはスキーマ認証不要であることの確認() {
        try {
            // ACL設定
            this.setACL(TEST_BOX1, "", ACL_SETTING_FILE);

            // データセルに自分セルローカルトークン認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account4", "password4", -1);
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // データアクセス
            // 親にスキーマ設定レベルが設定されたのでアクセスできなくなる。
            ResourceUtils.accessResource(DAV_COLLECTION + DAV_RESOURCE, tokenStr, HttpStatus.SC_OK,
                    Setup.TEST_BOX1, TEST_CELL1);

        } finally {

            // ACL設定を元に戻す
            this.setACL(TEST_BOX1, "", ACL_AUTH_TEST_SETTING_FILE);
        }
    }

    /**
     * スキーマ認証スキーマ値チェックの確認.
     * @throws TokenParseException トークンパースエラー
     */
    @SuppressWarnings("deprecation")
    @Test
    public final void スキーマ認証スキーマ値チェックの確認() throws TokenParseException {
        String userCell = "cell20161221";
        String schemaCell = "cell20161221schema";
        String user = "user";
        String pass = "password";
        String boxWithHttpSchemaUrl = "testschemabox1";
        String boxWithNonSchemaCellSchemaUrl = "testschemabox2";
        String boxWithLocalUnitSchemaUrl = "testschemabox3";
        String role = "role";
        String colName = "col";
        String aTokenStr = null;

        try {
            // セルの作成
            CellUtils.create(userCell, MASTER_TOKEN, HttpStatus.SC_CREATED);
            CellUtils.create(schemaCell, MASTER_TOKEN, HttpStatus.SC_CREATED);

            // Accountの作成
            AccountUtils.create(MASTER_TOKEN, userCell, user, pass, HttpStatus.SC_CREATED);
            AccountUtils.create(MASTER_TOKEN, schemaCell, user, pass, HttpStatus.SC_CREATED);

            // Boxの作成
            BoxUtils.createWithSchema(userCell, boxWithHttpSchemaUrl, MASTER_TOKEN, UrlUtils.cellRoot(schemaCell));
            BoxUtils.createWithSchema(userCell, boxWithNonSchemaCellSchemaUrl, MASTER_TOKEN,
                    UrlUtils.cellRoot(userCell));
            BoxUtils.createWithSchema(userCell, boxWithLocalUnitSchemaUrl,
                    MASTER_TOKEN, "personium-localunit:/" + schemaCell + "/");

            // Roleの作成
//            RoleUtils.create(userCell, MASTER_TOKEN, boxWithHttpSchemaUrl, role, HttpStatus.SC_CREATED);
//            RoleUtils.create(userCell, MASTER_TOKEN, boxWithLocalUnitSchemaUrl, role, HttpStatus.SC_CREATED);

            // RoleとAccountの$links
//            ResourceUtils.linkAccountRole(userCell, MASTER_TOKEN, user, boxWithHttpSchemaUrl,
//                    role, HttpStatus.SC_NO_CONTENT);
//            ResourceUtils.linkAccountRole(userCell, MASTER_TOKEN, user, boxWithLocalUnitSchemaUrl,
//                    role, HttpStatus.SC_NO_CONTENT);

            // BoxにConfidentialレベルの設定
//            this.setAclSchema(boxWithHttpSchemaUrl, "", UrlUtils.roleResource(userCell, boxWithHttpSchemaUrl, ""),
//                    OAuth2Helper.SchemaLevel.PUBLIC, userCell, role, DEFAULT_PRIVILEGE);
//            this.setAclSchema(boxWithNonSchemaCellSchemaUrl, "",
//                    UrlUtils.roleResource(userCell, boxWithHttpSchemaUrl, ""),
//                    OAuth2Helper.SchemaLevel.PUBLIC, userCell, role, DEFAULT_PRIVILEGE);
//            this.setAclSchema(boxWithLocalUnitSchemaUrl, "",
//                    UrlUtils.roleResource(userCell, boxWithLocalUnitSchemaUrl, ""),
//                    OAuth2Helper.SchemaLevel.PUBLIC, userCell, role, DEFAULT_PRIVILEGE);

            // ACLの設定（今回テストではACL設定は無関係のため、ALLで設定）
            DavResourceUtils.setACL(userCell, MASTER_TOKEN, HttpStatus.SC_OK,
                    userCell + "/" + boxWithHttpSchemaUrl,
                    "box/acl-setting-all.txt", role, "<D:read/></D:privilege><D:privilege><D:write/>",
                    OAuth2Helper.SchemaLevel.PUBLIC);
            DavResourceUtils.setACL(userCell, MASTER_TOKEN, HttpStatus.SC_OK,
                    userCell + "/" + boxWithNonSchemaCellSchemaUrl,
                    "box/acl-setting-all.txt", role, "<D:read/></D:privilege><D:privilege><D:write/>",
                    OAuth2Helper.SchemaLevel.PUBLIC);
            DavResourceUtils.setACL(userCell, MASTER_TOKEN, HttpStatus.SC_OK,
                    userCell + "/" + boxWithLocalUnitSchemaUrl,
                    "box/acl-setting-all.txt", role, "<D:read/></D:privilege><D:privilege><D:write/>",
                    OAuth2Helper.SchemaLevel.PUBLIC);

            // スキーマ認証用トランスセルトークンの取得
            JSONObject appAuthJson = getTransTokenByAppAuth(schemaCell, user, pass, UrlUtils.cellRoot(userCell));
            String appToken = (String) appAuthJson.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Queryでスキーマ認証
            TResponse res = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", userCell)
                    .with("username", user)
                    .with("password", pass)
                    .with("client_id", UrlUtils.cellRoot(schemaCell))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();

            String rTokenStr = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
            aTokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // コレクションの作成（boxWithHttpSchemaUrlはスキーマと、トークンのスキーマが一致するため作成可能）
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", userCell,
                    boxWithHttpSchemaUrl + "/" + colName,
                    aTokenStr, HttpStatus.SC_CREATED);

            // コレクションの作成（boxWithNonSchemaCellSchemaUrlはスキーマと、トークンのスキーマが一致しないため作成不可）
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", userCell,
                    boxWithNonSchemaCellSchemaUrl + "/" + colName,
                    aTokenStr, HttpStatus.SC_FORBIDDEN);

            // コレクションの作成（boxWithLocalUnitSchemaUrlはスキーマと、トークンのスキーマが一致するため作成可能）
            DavResourceUtils.createWebDavCollection("box/mkcol.txt", userCell,
                    boxWithLocalUnitSchemaUrl + "/" + colName,
                    aTokenStr, HttpStatus.SC_CREATED);

            // リフレッシュトークン認証
            TResponse refreshRes = ResourceUtils.refreshTokenAuthCl(userCell, rTokenStr);
            aTokenStr = (String) refreshRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
        } finally {
            // コレクションの削除（testbox03はスキーマと、トークンのスキーマが一致するため削除可能）
            DavResourceUtils.deleteCollection(userCell, boxWithHttpSchemaUrl, colName, MASTER_TOKEN,
                    HttpStatus.SC_NO_CONTENT);
            // コレクションの削除（testbox05はスキーマと、トークンのスキーマが一致するため削除可能）
            DavResourceUtils.deleteCollection(userCell, boxWithLocalUnitSchemaUrl, colName, MASTER_TOKEN,
                    HttpStatus.SC_NO_CONTENT);

            // RoleとAccountの$linksの削除
//            ResourceUtils.linkAccountRollDelete(userCell, MASTER_TOKEN, user, boxWithHttpSchemaUrl, role);

            // Roleの削除
//            RoleUtils.delete(userCell, MASTER_TOKEN, boxWithHttpSchemaUrl, role);

            // Boxの削除
            BoxUtils.delete(userCell, MASTER_TOKEN, boxWithHttpSchemaUrl);
            BoxUtils.delete(userCell, MASTER_TOKEN, boxWithNonSchemaCellSchemaUrl);
            BoxUtils.delete(userCell, MASTER_TOKEN, boxWithLocalUnitSchemaUrl);

            // Accountの削除
            AccountUtils.delete(schemaCell, MASTER_TOKEN, user, HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(userCell, MASTER_TOKEN, user, HttpStatus.SC_NO_CONTENT);

            // セルの削除
            CellUtils.delete(MASTER_TOKEN, schemaCell, -1);
            CellUtils.delete(MASTER_TOKEN, userCell, -1);
        }
    }


    /**
     * デフォルトボックスに対するスキーマ認証の確認.
     * @throws TokenParseException トークンパースエラー
     */
    @Test
    public final void デフォルトボックスに対するスキーマ認証の確認() throws TokenParseException {
        String tokenStr = checkCellLocalWithSchema("account0", "password0",
                TEST_CELL1, UrlUtils.cellRoot(TEST_CELL1));

        // WebDavのスキーマアクセス制御確認
        // 自分セルローカルトークン
        try {
            // テスト用のファイルをPUT
            DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                    "hoge", Box.DEFAULT_BOX_NAME, DAV_RESOURCE, -1);
            // ACL設定
            DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, DAV_RESOURCE,
                    "box/acl-all-none-schema-level.txt", Box.DEFAULT_BOX_NAME, "");

            this.checkResourcesWithSchema("", DAV_RESOURCE, tokenStr, Box.DEFAULT_BOX_NAME, TEST_CELL1);
        } finally {
            // テスト用のファイルを削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1,
                    AbstractCase.MASTER_TOKEN_NAME, DAV_RESOURCE, -1, Box.DEFAULT_BOX_NAME);
        }
    }

    /**
     * パスワード認証-トランスセルトークン取得.
     * @param account アカウント名
     * @param pass パスワード
     * @return トークン
     */
    private JSONObject getTransTokenByPassAuth(String account, String pass) {
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", account)
                .with("password", pass)
                .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                .returns()
                .statusCode(HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();

        return json;
    }

    /**
     * アプリセルパスワード認証-トランスセルトークン取得.
     * @param cellName ターゲットCell名
     * @param account アカウント名
     * @param pass パスワード
     * @return トークン
     */
    private JSONObject getTransTokenByAppAuth(String cellName, String account, String pass, String targetUrl) {
        // アプリセルに対して認証
        TResponse res =
                Http.request("authn/password-tc-c0.txt")
                        .with("remoteCell", cellName)
                        .with("username", account)
                        .with("password", pass)
                        .with("p_target", targetUrl)
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();

        return json;
    }

    /**
     * スキーマ付パスワード認証-セルローカルトークン取得.
     * @param account アカウント名
     * @param pass パスワード
     * @param clientIdCell clientIdCell
     * @return トークン
     */
    private JSONObject getLocalTokenByPassAuthWithSchema(String account, String pass, String token,
            String clientIdCell) {
        // Queryでスキーマ認証
        TResponse res = Http.request("authn/password-cl-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", account)
                .with("password", pass)
                .with("client_id", UrlUtils.cellRoot(clientIdCell))
                .with("client_secret", token)
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();

        return json;
    }

    /**
     * スキーマ付パスワード認証-セルローカルトークン取得.
     * @param account アカウント名
     * @param pass パスワード
     * @return トークン
     */
    private JSONObject getTransTokenByPassAuthWithSchema(String account, String pass, String token) {
        // アプリセルに対して認証
        TResponse res = Http.request("authn/password-tc-cp.txt")
                .with("remoteCell", TEST_CELL1)
                .with("username", account)
                .with("password", pass)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", token)
                .with("p_target", UrlUtils.cellRoot(TEST_CELL2))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();

        return json;
    }

    /**
     * ACL設定.
     * @param path 対象のコレクションのパス
     * @return レスポンス
     */
    private TResponse setACL(String box, String path, String setFile) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(setFile)
                .with("cellPath", TEST_CELL1)
                .with("colname", path)
                .with("box", box)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                .with("level", "none")
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return tresponseWebDav;
    }

    private String checkCellLocalWithSchema(String account, String pass, String schemaCell, String schema)
            throws TokenParseException {
        // スキーマ認証
        JSONObject appAuthJson = this.getTransTokenByAppAuth(schemaCell, account, pass,
                UrlUtils.cellRoot(TEST_CELL1));
        String appToken = (String) appAuthJson.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // 認証
        JSONObject json = this.getLocalTokenByPassAuthWithSchema("account2", "password2", appToken, schemaCell);

        String issuer = UrlUtils.cellRoot(TEST_CELL1);

        // トークンチェック
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        AccountAccessToken aToken = AccountAccessToken.parse(tokenStr, issuer);
        assertEquals(schema, aToken.getSchema());
        String rTokenStr = (String) json.get(OAuth2Helper.Key.REFRESH_TOKEN);
        CellLocalRefreshToken rToken = CellLocalRefreshToken.parse(rTokenStr, issuer);
        assertEquals(schema, rToken.getSchema());

        return tokenStr;
    }

    /**
     * スキーマ付セルローカルトークン認証処理.
     * @param account アカウント名
     * @param pass パスワード
     * @return トークン
     * @throws TokenParseException トークンパースエラー
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    private void checkTransTokenWithSchema(String account, String pass, String schema) throws TokenParseException,
            TokenDsigException, TokenRootCrtException {
        // スキーマ認証
        JSONObject appAuthJson = getTransTokenByAppAuth(TEST_APP_CELL1, account, pass,
                UrlUtils.cellRoot(TEST_CELL1));
        String appToken = (String) appAuthJson.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // 認証
        JSONObject json = getTransTokenByPassAuthWithSchema("account2", "password2", appToken);

        // トークンチェック
        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        TransCellAccessToken aToken = TransCellAccessToken.parse(tokenStr);
        assertEquals(schema, aToken.getSchema());
    }

    /**
     * スキーマ付トークン認証処理.
     * @param account アカウント名
     * @param pass パスワード
     * @param schema スキーマ名
     * @return 他人セルローカルトークン
     * @throws TokenParseException トークンパースエラー
     */
    private String cheackTokenAuth(String account, String pass, String schema) throws TokenParseException {
        // 認証
        JSONObject json = getTransTokenByPassAuth("account2", "password2");

        String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // スキーマ認証
        JSONObject appAuthJson = getTransTokenByAppAuth(TEST_APP_CELL1, account, pass,
                UrlUtils.cellRoot(TEST_CELL2));
        String appToken = (String) appAuthJson.get(OAuth2Helper.Key.ACCESS_TOKEN);

        // セルに対してトークン認証
        // Queryでスキーマ認証
        TResponse res3 = Http.request("authn/saml-cl-cp.txt")
                .with("remoteCell", TEST_CELL2)
                .with("assertion", tokenStr)
                .with("client_id", UrlUtils.cellRoot(TEST_APP_CELL1))
                .with("client_secret", appToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        String issuer = UrlUtils.cellRoot(TEST_CELL2);

        JSONObject json2 = res3.bodyAsJson();
        String tokenStr2 = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);
        CellLocalAccessToken aToken = CellLocalAccessToken.parse(tokenStr2, issuer);
        assertEquals(schema, aToken.getSchema());

        return tokenStr2;
    }
}

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
package io.personium.test.jersey.cell.auth;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.cell.ctl.CellCtlUtils;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.UrlUtils;

/**
 * 認証（Account名更新）のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class, Integration.class, Regression.class })
public class AuthAccountUpdateTest extends PersoniumTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;

    /**
     * コンストラクタ.
     */
    public AuthAccountUpdateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名を変更した場合401エラーとなること_Cellレベル.
     */
    @Test
    public final void 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名を変更した場合401エラーとなること_Cellレベル() {
        String cellName = "testcell001";
        String accountName = "testUser001";
        String accountNameUpdated = "testUser001_updated";
        String pass = "password";
        String roleName = "testRole001";

        try {
            // Cell作成
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName,
                    accountName, pass, HttpStatus.SC_CREATED);

            // Roleの作成
            CellCtlUtils.createRole(cellName, roleName);

            // RoleとAccountの結びつけ
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // CellにACLの設定
            Http.request("cell/acl-setting-single-request.txt")
                    .with("url", cellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, ""))
                    .with("role", roleName)
                    .with("privilege", "<p:auth/></D:privilege><D:privilege><p:auth-read/>")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // パスワード認証
            String token = ResourceUtils.getMyCellLocalToken(cellName, accountName, pass);

            // Account名更新
            Http.request("account-update-accountname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("username", accountName)
                    .with("newUsername", accountNameUpdated)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Account更新前に取得したトークンを使用してBox作成
            BoxUtils.create(cellName, "testBox001", token, HttpStatus.SC_UNAUTHORIZED);

        } finally {
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(cellName, Account.EDM_TYPE_NAME, accountNameUpdated, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Roleの削除
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Accountの削除
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, accountNameUpdated, -1);
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
            // Cell削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名を変更した場合401エラーとなること.
     */
    @Test
    public final void 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名を変更した場合401エラーとなること() {
        String accountName = "testUser001";
        String accountNameUpdated = "testUser001_updated";
        String pass = "password";
        String roleName = "testRole001";

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    accountName, pass, HttpStatus.SC_CREATED);

            // Roleの作成
            CellCtlUtils.createRole(TEST_CELL1, roleName);

            // RoleとAccountの結びつけ
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // Box1にACLの設定
            Http.request("box/acl-setting.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("colname", "")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "none")
                    .with("role", roleName)
                    .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // パスワード認証
            String token = ResourceUtils.getMyCellLocalToken(TEST_CELL1, accountName, pass);

            // Account名更新
            Http.request("account-update-accountname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", TEST_CELL1)
                    .with("username", accountName)
                    .with("newUsername", accountNameUpdated)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Account更新前に取得したトークンを使用してCollection作成
            DavResourceUtils.createWebDavCollection(token, HttpStatus.SC_UNAUTHORIZED, "collection");

        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    "", "box/acl-authtest.txt", Setup.TEST_BOX1, "");
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, accountNameUpdated, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountNameUpdated, -1);
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * 自分セルローカルトークン_RoleとリンクされていないAccountを使用してトークン認証後Account名を変更した場合401エラーとなること.
     */
    @Test
    public final void 自分セルローカルトークン_RoleとリンクされていないAccountを使用してトークン認証後Account名を変更した場合401エラーとなること() {
        String accountName = "testUser001";
        String accountNameUpdated = "testUser001_updated";
        String pass = "password";
        String roleName = "testRole001";

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    accountName, pass, HttpStatus.SC_CREATED);

            // Roleの作成
            CellCtlUtils.createRole(TEST_CELL1, roleName);

            // Box1にACLの設定
            Http.request("box/acl-setting.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("colname", "")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "none")
                    .with("role", roleName)
                    .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // パスワード認証
            String token = ResourceUtils.getMyCellLocalToken(TEST_CELL1, accountName, pass);

            // Account名更新
            Http.request("account-update-accountname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", TEST_CELL1)
                    .with("username", accountName)
                    .with("newUsername", accountNameUpdated)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Account更新前に取得したトークンを使用してCollection作成
            DavResourceUtils.createWebDavCollection(token, HttpStatus.SC_UNAUTHORIZED, "collection");

        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    "", "box/acl-authtest.txt", Setup.TEST_BOX1, "");
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountNameUpdated, -1);
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * 自分セルローカルトークン_トークン認証後Account名を変更しACL設定されていないリソースに対する操作で401エラーとなること.
     */
    @Test
    public final void 自分セルローカルトークン_トークン認証後Account名を変更しACL設定されていないリソースに対する操作で401エラーとなること() {
        String accountName = "testUser001";
        String accountNameUpdated = "testUser001_updated";
        String pass = "password";

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    accountName, pass, HttpStatus.SC_CREATED);

            // パスワード認証
            String token = ResourceUtils.getMyCellLocalToken(TEST_CELL1, accountName, pass);

            // Account名更新
            Http.request("account-update-accountname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", TEST_CELL1)
                    .with("username", accountName)
                    .with("newUsername", accountNameUpdated)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Account更新前に取得したトークンを使用してCollection作成
            DavResourceUtils.createWebDavCollection(token, HttpStatus.SC_UNAUTHORIZED, "collection");

        } finally {
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountNameUpdated, -1);
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名とパスワードを変更した場合401エラーとなること.
     */
    @Test
    public final void 自分セルローカルトークン_RoleとリンクされたAccountを使用してトークン認証後Account名とパスワードを変更した場合401エラーとなること() {
        String accountName = "testUser001";
        String accountNameUpdated = "testUser001_updated";
        String pass = "password";
        String passUpdated = "passwordUpdated";
        String roleName = "testRole001";

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    accountName, pass, HttpStatus.SC_CREATED);

            // Roleの作成
            CellCtlUtils.createRole(TEST_CELL1, roleName);

            // RoleとAccountの結びつけ
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // Box1にACLの設定
            Http.request("box/acl-setting.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("colname", "")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "none")
                    .with("role", roleName)
                    .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // パスワード認証
            String token = ResourceUtils.getMyCellLocalToken(TEST_CELL1, accountName, pass);

            // パスワードを空に更新
            Http.request("account-update.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", TEST_CELL1)
                    .with("username", accountName)
                    .with("newUsername", accountNameUpdated)
                    .with("password", passUpdated)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // Account更新前に取得したトークンを使用してCollection作成
            DavResourceUtils.createWebDavCollection(token, HttpStatus.SC_UNAUTHORIZED, "collection");

        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK,
                    "", "box/acl-authtest.txt", Setup.TEST_BOX1, "");
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, accountName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, accountNameUpdated, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountNameUpdated, -1);
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, -1);
        }
    }

}

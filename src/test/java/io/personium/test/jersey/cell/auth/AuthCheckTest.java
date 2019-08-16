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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Relation;
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
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TestMethodUtils;

/**
 * 認証のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthCheckTest extends PersoniumTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String ACL_AUTH_TEST_SETTING_FILE = "box/acl-authtest.txt";
    /**
     * デフォルトボックス名.
     */
    static final String DEFAULT_BOX_NAME = "__";

    static final String CELL_NAME1 = "cell11";
    static final String CELL_NAME2 = "cell21";
    static final String APP_CELL_NAME = "app";
    static final String BOX_NAME1 = "box2";
    static final String BOX_NAME2 = "box3";
    static final String BOX_SCHEMA_NAME = UrlUtils.cellRoot(APP_CELL_NAME);
    static final String USER_NAME1 = "user1";
    static final String USER_NAME2 = "user2";
    static final String USER_NAME3 = "user3";
    static final String USER_NAME4 = "user4";
    static final String PASSWORD = "password";
    static final String ROLE_NAME = "doctor";
    static final String ROLE_NAME1 = "extRole1";
    static final String ROLE_NAME2 = "extRole2";
    static final String ROLE_NAME3 = "role3";
    static final String ROLE_NAME4 = "extRole4";
    static final String RELATION_NAME = "relation1";
    static final String EXTCELL_URL = UrlUtils.extCellResource(CELL_NAME1, UrlUtils.cellRoot(CELL_NAME2));
    static final String ROLE_URI = UrlUtils.roleUrl(CELL_NAME1, null, ROLE_NAME);
    static final String RELATION_BOX_NAME = null;
    static final String EXTROLE_NAME1 = UrlUtils.roleResource(APP_CELL_NAME, Box.MAIN_BOX_NAME, ROLE_NAME1);
    static final String EXTROLE_NAME2 = UrlUtils.roleResource(CELL_NAME2, Box.MAIN_BOX_NAME, ROLE_NAME2);
    static final String EXTROLE_NAME4 = UrlUtils.roleResource(APP_CELL_NAME, Box.MAIN_BOX_NAME, ROLE_NAME4);

    /**
     * コンストラクタ.
     */
    public AuthCheckTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * １．ボックスと結びつかないロールのトランスセル確認.
     * @throws TokenParseException TokenParseException
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void ボックスと結びつかないロールのトランスセル確認() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {

        String testCellName = "cell001";
        String userName = "user0";
        String pass = "password";
        String roleNameNoneBox = "role0";

        try {
            // 本テスト用セルの作成
            CellUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // １．ボックスと結びつかないロールのトランスセル確認
            // アカウント追加(user0)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName,
                    userName, pass, HttpStatus.SC_CREATED);

            // ロール追加（BOXに結びつかない）
            RoleUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameNoneBox,
                    null, HttpStatus.SC_CREATED);

            // ロール結びつけ（BOXに結びつかないロールとアカウント結びつけ）
            LinksUtils.createLinks(testCellName, Account.EDM_TYPE_NAME, userName, null, Role.EDM_TYPE_NAME,
                    roleNameNoneBox, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // 認証（トランセルトークン）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", testCellName)
                    .with("username", userName)
                    .with("password", pass)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken = TransCellAccessToken.parse(transCellAccessToken);

            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(testCellName, Box.MAIN_BOX_NAME, roleNameNoneBox),
                    aToken.getRoles().get(0).createUrl());
        } finally {

            // １．ボックスと結びつかないロールのトランスセル確認
            // ロール結びつけ削除（BOXに結びつかないロールとアカウント結びつけ）
            LinksUtils.deleteLinks(testCellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleNameNoneBox, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // アカウント削除
            AccountUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME,
                    userName, HttpStatus.SC_NO_CONTENT);
            // ロール削除（BOXに結びつかない）
            RoleUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameNoneBox, null);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName, -1);

        }
    }

    /**
     * ２．スキーマありのボックスと結びつくロールのトランスセル確認.
     * @throws TokenParseException TokenParseException
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void スキーマありのボックスと結びつくロールのトランスセル確認() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {

        String testCellName = "authchecktest-cell";
        String boxNameWithScheme = "box2";
        String userName = "user2";
        String pass = "password";
        String roleNameWithBox2 = "role2";
        String schemeCellName = "app1";
        String scheme = UrlUtils.getBaseUrl() + "/" + schemeCellName + "/";

        try {
            // 本テスト用セルの作成
            CellUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ２．スキーマありのボックスと結びつくロールのトランスセル確認
            // Box作成(スキーマあり:Box2)
            BoxUtils.createWithSchema(testCellName, boxNameWithScheme,
                    AbstractCase.MASTER_TOKEN_NAME, scheme);

            // アカウント追加(user2)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName,
                    userName, pass, HttpStatus.SC_CREATED);

            // ロール追加（スキーマありBOXに結びつつく）
            RoleUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox2,
                    boxNameWithScheme, HttpStatus.SC_CREATED);

            // ロール結びつけ（スキーマありBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.createLinks(testCellName, Account.EDM_TYPE_NAME, userName, null, Role.EDM_TYPE_NAME,
                    roleNameWithBox2, boxNameWithScheme, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // 認証（トランセルトークン）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", testCellName)
                    .with("username", userName)
                    .with("password", pass)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken = TransCellAccessToken.parse(transCellAccessToken);

            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(schemeCellName, DEFAULT_BOX_NAME, roleNameWithBox2),
                    aToken.getRoles().get(0).createUrl());
        } finally {

            // ２．スキーマありのボックスと結びつくロールのトランスセル確認
            // ロール結びつけ削除（スキーマありBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.deleteLinks(testCellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleNameWithBox2, boxNameWithScheme, AbstractCase.MASTER_TOKEN_NAME, -1);
            // アカウント削除
            AccountUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME,
                    userName, HttpStatus.SC_NO_CONTENT);
            // ロール削除（スキーマありBOXに結びつつく）
            RoleUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox2, boxNameWithScheme);
            // Box削除(スキーマあり)
            BoxUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, boxNameWithScheme);
            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName, -1);
        }
    }

    /**
     * ３．スキーマなしのボックスと結びつくロールのトランスセルの確認.
     * @throws TokenParseException TokenParseException
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void スキーマなしのボックスと結びつくロールのトランスセルの確認() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {

        String testCellName = "authchecktest-cell";
        String boxNameNoneScheme = "box1";
        String userName = "user1";
        String pass = "password";
        String roleNameWithBox1 = "role1";

        try {
            // 本テスト用セルの作成
            CellUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ３．スキーマなしのボックスと結びつくロールのトランスセルの確認
            // Box作成(スキーマ無し:Box1)
            BoxUtils.create(testCellName, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME);

            // アカウント追加(user1)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName,
                    userName, pass, HttpStatus.SC_CREATED);

            // ロール追加（スキーマなしBOXに結びつつく）
            RoleUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox1,
                    boxNameNoneScheme, HttpStatus.SC_CREATED);

            // ロール結びつけ（スキーマなしBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.createLinks(testCellName, Account.EDM_TYPE_NAME, userName, null, Role.EDM_TYPE_NAME,
                    roleNameWithBox1, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // 認証（トランセルトークン）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", testCellName)
                    .with("username", userName)
                    .with("password", pass)
                    .with("p_target", UrlUtils.cellRoot(TEST_CELL1))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken = TransCellAccessToken.parse(transCellAccessToken);

            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(testCellName, DEFAULT_BOX_NAME, roleNameWithBox1),
                    aToken.getRoles().get(0).createUrl());
        } finally {

            // ３．スキーマなしのボックスと結びつくロールのトランスセルの確認
            // ロール結びつけ削除（スキーマなしBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.deleteLinks(testCellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleNameWithBox1, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME, -1);

            // アカウント削除
            AccountUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME,
                    userName, HttpStatus.SC_NO_CONTENT);

            // ロール削除（スキーマなしBOXに結びつつく）
            RoleUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox1, boxNameNoneScheme);

            // Box削除(スキーマ無し)
            BoxUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, boxNameNoneScheme);

            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName, -1);

        }
    }

    /**
     * ExtCellとRelationによる外部Cellユーザ評価の確認. #12868トランスセル評価その２.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtCellとRelationによる外部Cellユーザ評価の確認() {

        String testCellName1 = "cell01";
        String testCellName2 = "cell02";
        String testCellName3 = "cell03";
        String userName2 = "user2";
        String userName3 = "user3";
        String pass = "password";
        String roleName = "role1";
        String relationName = "relation1";
        String masterToken = AbstractCase.MASTER_TOKEN_NAME;

        try {
            // 本テスト用セルの作成（Cell1）
            CellUtils.create(testCellName1, masterToken, HttpStatus.SC_CREATED);
            // 本テスト用セルの作成（Cell2）
            CellUtils.create(testCellName2, masterToken, HttpStatus.SC_CREATED);
            // 本テスト用セルの作成（Cell3）
            CellUtils.create(testCellName3, masterToken, HttpStatus.SC_CREATED);
            // Cell2のユーザを作成する
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName2, userName2,
                    pass, HttpStatus.SC_CREATED);
            // Cell3のユーザを作成する
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName3, userName3,
                    pass, HttpStatus.SC_CREATED);
            // Cell1のロールを作成する
            RoleUtils.create(testCellName1, masterToken, roleName, null, HttpStatus.SC_CREATED);
            // Cell1のExtCellを作成する
            ExtCellUtils.create(masterToken, testCellName1, UrlUtils.cellRoot(testCellName2));
            // Cell1にRelationを作成する
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", null);
            RelationUtils.create(testCellName1, AbstractCase.MASTER_TOKEN_NAME, body, HttpStatus.SC_CREATED);
            // Cell1のExtCellとRelationを結びつけ
            LinksUtils.createLinksExtCell(testCellName1,
                    PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(testCellName2)),
                    Relation.EDM_TYPE_NAME, relationName, null, masterToken, HttpStatus.SC_NO_CONTENT);

            // Cell1のRelationとRoleを結びつけ
            LinksUtils.createLinks(testCellName1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, masterToken, HttpStatus.SC_NO_CONTENT);

            // テスト１（user2でのアクセス時にTCAT内にrole1が入っていること）
            List<Role> tokenRoles1 = this.checkTransCellAccessToken(testCellName1,
                    testCellName2, userName2, pass, roleName);
            // テスト環境がロール１つのため、１以外はテスト失敗
            assertEquals(1, tokenRoles1.size());
            String token1RoleUrl = tokenRoles1.get(0).createUrl();

            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(testCellName1, Box.MAIN_BOX_NAME, roleName), token1RoleUrl);

            // テスト２（user3でのアクセス時にTCAT内にrole2が入っていないこと）
            List<Role> tokenRoles2 = this.checkTransCellAccessToken(testCellName1,
                    testCellName3, userName3, pass, roleName);
            // ロールが払い出されないことを確認
            assertEquals(0, tokenRoles2.size());
        } finally {
            // Cell1のRelationとRoleの削除
            LinksUtils.deleteLinks(testCellName1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, masterToken, -1);

            // Cell1のExtCellとRelationの削除
            LinksUtils.deleteLinksExtCell(testCellName1,
                    PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(testCellName2)),
                    Relation.EDM_TYPE_NAME, relationName, null, masterToken, -1);
            // Cell1のRelationを削除
            RelationUtils.delete(testCellName1, masterToken, relationName, null, HttpStatus.SC_NO_CONTENT);
            // Cell1のExtCellを削除
            ExtCellUtils.delete(masterToken, testCellName1, UrlUtils.cellRoot(testCellName2),
                    HttpStatus.SC_NO_CONTENT);
            // Cell3のアカウント削除
            AccountUtils.delete(testCellName3, masterToken, userName3, HttpStatus.SC_NO_CONTENT);
            // Cell2のアカウント削除
            AccountUtils.delete(testCellName2, masterToken, userName2, HttpStatus.SC_NO_CONTENT);
            // Cell1のロール削除
            RoleUtils.delete(testCellName1, masterToken, roleName, null);
            // 本テスト用セルの削除(Cell1)
            CellUtils.delete(masterToken, testCellName1, -1);
            // 本テスト用セルの削除(Cell2)
            CellUtils.delete(masterToken, testCellName2, -1);
            // 本テスト用セルの削除(Cell3)
            CellUtils.delete(masterToken, testCellName3, -1);

        }
    }

    /**
     * ExtCellとRelationとExtRoleによる外部Cellユーザ評価の確認. #12870トランスセル評価その３.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtCellとRelationとExtRoleによる外部Cellユーザ評価の確認() {
        String masterToken = AbstractCase.MASTER_TOKEN_NAME;
        try {
            // テスト用セルの作成
            CellUtils.create(CELL_NAME1, masterToken, HttpStatus.SC_CREATED);
            CellUtils.create(CELL_NAME2, masterToken, HttpStatus.SC_CREATED);
            // テスト用ボックスの作成
            BoxUtils.createWithSchema(CELL_NAME2, BOX_NAME1, masterToken, BOX_SCHEMA_NAME);
            BoxUtils.create(CELL_NAME2, BOX_NAME2, masterToken);
            // Cell2のユーザを作成する
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, CELL_NAME2,
                    USER_NAME1, PASSWORD, HttpStatus.SC_CREATED);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, CELL_NAME2,
                    USER_NAME2, PASSWORD, HttpStatus.SC_CREATED);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, CELL_NAME2,
                    USER_NAME3, PASSWORD, HttpStatus.SC_CREATED);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, CELL_NAME2,
                    USER_NAME4, PASSWORD, HttpStatus.SC_CREATED);
            // Cell2のユーザ1用のロール作成及び結びつけ
            RoleUtils.create(CELL_NAME2, masterToken, ROLE_NAME1, BOX_NAME1, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME1, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME1, BOX_NAME1, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell2のユーザ2用のロール作成及び結びつけ
            RoleUtils.create(CELL_NAME2, masterToken, ROLE_NAME2, null, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME2, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME2, null, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell2のユーザ3用のロール作成及び結びつけ
            RoleUtils.create(CELL_NAME2, masterToken, ROLE_NAME3, null, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME3, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME3, null, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell2のユーザ4用のロール作成及び結びつけ
            RoleUtils.create(CELL_NAME2, masterToken, ROLE_NAME4, BOX_NAME2, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME4, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME4, BOX_NAME2, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell1のロールを作成する
            RoleUtils.create(CELL_NAME1, masterToken, ROLE_NAME, null, HttpStatus.SC_CREATED);
            // Cell1のExtCellを作成する
            ExtCellUtils.create(masterToken, CELL_NAME1, UrlUtils.cellRoot(CELL_NAME2));
            // Cell1にRelationを作成する
            JSONObject body = new JSONObject();
            body.put("Name", RELATION_NAME);
            body.put("_Box.Name", null);
            RelationUtils.create(CELL_NAME1, masterToken, body, HttpStatus.SC_CREATED);
            // Cell1のExtRole1を作成する
            JSONObject extRoleBody = new JSONObject();
            extRoleBody.put("ExtRole", EXTROLE_NAME1);
            extRoleBody.put("_Relation.Name", RELATION_NAME);
            extRoleBody.put("_Relation._Box.Name", null);
            ExtRoleUtils.create(masterToken, CELL_NAME1, extRoleBody, HttpStatus.SC_CREATED);
            // Cell1のExtRole2を作成する
            JSONObject extRoleBody2 = new JSONObject();
            extRoleBody2.put("ExtRole", EXTROLE_NAME2);
            extRoleBody2.put("_Relation.Name", RELATION_NAME);
            extRoleBody2.put("_Relation._Box.Name", null);
            ExtRoleUtils.create(masterToken, CELL_NAME1, extRoleBody2, HttpStatus.SC_CREATED);
            // Cell1のExtRole4を作成する
            JSONObject extRoleBody4 = new JSONObject();
            extRoleBody4.put("ExtRole", EXTROLE_NAME4);
            extRoleBody4.put("_Relation.Name", RELATION_NAME);
            extRoleBody4.put("_Relation._Box.Name", null);
            ExtRoleUtils.create(masterToken, CELL_NAME1, extRoleBody4, HttpStatus.SC_CREATED);
            // Cell1のExtCellとRelationを結びつけ
            LinksUtils.createLinksExtCell(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(CELL_NAME2)),
                    Relation.EDM_TYPE_NAME, RELATION_NAME, null, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell1のExtRoleとRoleを結びつけ
            LinksUtils.createLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME1),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null,
                    masterToken, HttpStatus.SC_NO_CONTENT);
            LinksUtils.createLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME2),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null,
                    masterToken, HttpStatus.SC_NO_CONTENT);
            LinksUtils.createLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME4),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null,
                    masterToken, HttpStatus.SC_NO_CONTENT);

            // テスト
            // テスト１（user1でのアクセス時にTCAT内にdoctorが入っていること）
            List<Role> tokenRoles1 = this.checkTransCellAccessToken(CELL_NAME1,
                    CELL_NAME2, USER_NAME1, PASSWORD, ROLE_NAME1);
            // テスト環境がロール１つのため、１以外はテスト失敗
            assertEquals(1, tokenRoles1.size());
            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(CELL_NAME1, Box.MAIN_BOX_NAME, ROLE_NAME),
                    tokenRoles1.get(0).createUrl());

            // テスト２（user2でのアクセス時にTCAT内にdoctorが入っていること）
            List<Role> tokenRoles2 = this.checkTransCellAccessToken(CELL_NAME1,
                    CELL_NAME2, USER_NAME2, PASSWORD, ROLE_NAME2);
            // テスト環境がロール１つのため、１以外はテスト失敗
            assertEquals(1, tokenRoles2.size());
            // 取得トークン内のロール確認
            assertEquals(UrlUtils.roleResource(CELL_NAME1, Box.MAIN_BOX_NAME, ROLE_NAME),
                    tokenRoles2.get(0).createUrl());

            // テスト３（user3でのアクセス時にTCAT内にdoctorが入っていないこと）
            List<Role> tokenRoles3 = this.checkTransCellAccessToken(CELL_NAME1,
                    CELL_NAME2, USER_NAME3, PASSWORD, ROLE_NAME3);
            // 関係のあるロールが払い出されないので、ロールリストのサイズは0である。
            assertEquals(0, tokenRoles3.size());

            // テスト４（user4でのアクセス時にTCAT内にdoctorが入っていないこと）
            List<Role> tokenRoles4 = this.checkTransCellAccessToken(CELL_NAME1,
                    CELL_NAME2, USER_NAME4, PASSWORD, ROLE_NAME4);
            // 関係のあるロールが払い出されないので、ロールリストのサイズは0である。
            assertEquals(0, tokenRoles4.size());
        } finally {
            // Cell1のExtRoleとRoleを結びつけを削除
            LinksUtils.deleteLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME1),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null, masterToken, -1);
            LinksUtils.deleteLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME2),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null, masterToken, -1);
            LinksUtils.deleteLinksExtRole(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(EXTROLE_NAME4),
                    RELATION_NAME, null, Role.EDM_TYPE_NAME, ROLE_NAME, null, masterToken, -1);
            // Cell1のExtCellとRelationの削除
            LinksUtils.deleteLinksExtCell(CELL_NAME1, PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(CELL_NAME2)),
                    Relation.EDM_TYPE_NAME, RELATION_NAME, null, masterToken, -1);
            // Cell1のExtRoleを削除する
            ExtRoleUtils.delete(CELL_NAME1, EXTROLE_NAME1,
                    RELATION_NAME, RELATION_BOX_NAME, masterToken, HttpStatus.SC_NO_CONTENT);
            ExtRoleUtils.delete(CELL_NAME1, EXTROLE_NAME2,
                    RELATION_NAME, RELATION_BOX_NAME, masterToken, HttpStatus.SC_NO_CONTENT);
            ExtRoleUtils.delete(CELL_NAME1, EXTROLE_NAME4,
                    RELATION_NAME, RELATION_BOX_NAME, masterToken, HttpStatus.SC_NO_CONTENT);
            // Cell1のRelationを削除
            RelationUtils.delete(CELL_NAME1, masterToken, RELATION_NAME, null, HttpStatus.SC_NO_CONTENT);
            // Cell1のExtCellを削除
            ExtCellUtils.delete(masterToken, CELL_NAME1, UrlUtils.cellRoot(CELL_NAME2),
                    HttpStatus.SC_NO_CONTENT);
            // Cell1のロール削除
            RoleUtils.delete(CELL_NAME1, masterToken, ROLE_NAME, null);
            // Cell2のユーザのロール結びつけ削除
            LinksUtils.deleteLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME4, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME4, BOX_NAME2, masterToken, -1);
            RoleUtils.delete(CELL_NAME2, masterToken, ROLE_NAME4, BOX_NAME2);
            LinksUtils.deleteLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME3, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME3, null, masterToken, -1);
            RoleUtils.delete(CELL_NAME2, masterToken, ROLE_NAME3, null);
            LinksUtils.deleteLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME2, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME2, null, masterToken, -1);
            RoleUtils.delete(CELL_NAME2, masterToken, ROLE_NAME2, null);
            LinksUtils.deleteLinks(CELL_NAME2, Account.EDM_TYPE_NAME, USER_NAME1, null,
                    Role.EDM_TYPE_NAME, ROLE_NAME1, BOX_NAME1, masterToken, -1);
            RoleUtils.delete(CELL_NAME2, masterToken, ROLE_NAME1, BOX_NAME1);
            // Cell2のアカウント削除
            AccountUtils.delete(CELL_NAME2, masterToken, USER_NAME4, HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(CELL_NAME2, masterToken, USER_NAME3, HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(CELL_NAME2, masterToken, USER_NAME2, HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(CELL_NAME2, masterToken, USER_NAME1, HttpStatus.SC_NO_CONTENT);
            // テスト用BOXの削除
            BoxUtils.delete(CELL_NAME2, masterToken, BOX_NAME1);
            BoxUtils.delete(CELL_NAME2, masterToken, BOX_NAME2);
            // テスト用セルの削除
            CellUtils.delete(masterToken, CELL_NAME1, -1);
            CellUtils.delete(masterToken, CELL_NAME2, -1);
        }
    }

    /**
     * Boxに紐付くRoleでのACLアクセス制御の確認.
     */
    @Test
    public final void Boxに紐付くRoleでのACLアクセス制御の確認() {
        String boxName = "box1";
        String roleName = "testRole02";
        String userName = "testUser02";
        String pass = "password";
        String masterToken = AbstractCase.MASTER_TOKEN_NAME;

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    userName, pass, HttpStatus.SC_CREATED);
            // Boxに紐付くロールの作成
            RoleUtils.create(TEST_CELL1, masterToken, roleName, boxName, HttpStatus.SC_CREATED);
            // RoleとAccountの結びつけ
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, boxName, masterToken, HttpStatus.SC_NO_CONTENT);
            // ACLの設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, masterToken, HttpStatus.SC_OK, boxName,
                    "", "box/acl-setting.txt", roleName, boxName, "<D:read/>", "");
            // 認証
            String token1 = ResourceUtils.getMyCellLocalToken(TEST_CELL1, userName, pass);
            // Boxアクセス制御のテスト testcell1/box2
            // GET
            ResourceUtils.retrieve(token1, "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, masterToken, HttpStatus.SC_OK,
                    "", ACL_AUTH_TEST_SETTING_FILE, Setup.TEST_BOX1, "");
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, boxName, masterToken, -1);
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, masterToken, roleName, boxName);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, masterToken, userName, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * PROPFINDのRole名の大文字小文字チェックの確認.
     */
    @Test
    public final void PROPFINDのRole名の大文字小文字チェックの確認() {
        String boxName = "box1";
        String roleName = "testRole02";
        String userName = "testUser02";
        String pass = "password";
        String masterToken = AbstractCase.MASTER_TOKEN_NAME;

        try {
            // Accountの作成 (小文字)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, userName.toLowerCase(),
                    pass, HttpStatus.SC_CREATED);
            // Accountの作成 (大文字)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, userName.toUpperCase(),
                    pass, HttpStatus.SC_CREATED);
            // Boxに紐付くロールの作成 (小文字)
            RoleUtils.create(TEST_CELL1, masterToken, roleName.toLowerCase(), boxName, HttpStatus.SC_CREATED);
            // Boxに紐付くロールの作成 (大文字)
            RoleUtils.create(TEST_CELL1, masterToken, roleName.toUpperCase(), boxName, HttpStatus.SC_CREATED);
            // RoleとAccountの結びつけ(小文字)
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName.toLowerCase(), null,
                    Role.EDM_TYPE_NAME, roleName.toLowerCase(), boxName, masterToken, HttpStatus.SC_NO_CONTENT);
            // RoleとAccountの結びつけ(大文字)
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName.toUpperCase(), null,
                    Role.EDM_TYPE_NAME, roleName.toUpperCase(), boxName, masterToken, HttpStatus.SC_NO_CONTENT);
            // ACLの設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK, boxName, "",
                    "box/acl-2role-setting.txt", roleName.toLowerCase(), roleName.toUpperCase(), boxName, "<D:read/>",
                    "<D:write/>", "");
            // PROPFINDの確認
            // PROPFINDでACLの確認
            TResponse tresponse = CellUtils.propfind(TEST_CELL1 + "/" + boxName, masterToken, "0",
                    HttpStatus.SC_MULTI_STATUS);
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            List<String> roleList = new ArrayList<String>();
            List<String> roleList2 = new ArrayList<String>();
            roleList.add("read");
            roleList2.add("read");
            roleList2.add("write");
            map.put(roleName.toLowerCase(), roleList);
            map2.put(roleName.toUpperCase(), roleList2);
            list.add(map);
            list.add(map2);
            Element root = tresponse.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, boxName);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, boxName, ""), null);

            // 認証
            String token1 = ResourceUtils.getMyCellLocalToken(TEST_CELL1, userName.toLowerCase(), pass);
            // 小文字のユーザには、write権限がないのでエラーになる事を確認
            DavResourceUtils.createWebDavCollection("box/mkcol-normal.txt", TEST_CELL1, "test", token1,
                    HttpStatus.SC_FORBIDDEN);
        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, masterToken, HttpStatus.SC_OK, "", ACL_AUTH_TEST_SETTING_FILE,
                    Setup.TEST_BOX1, "");
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName.toLowerCase(), null,
                    Role.EDM_TYPE_NAME, roleName.toLowerCase(), boxName, masterToken, -1);
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, userName.toUpperCase(), null,
                    Role.EDM_TYPE_NAME, roleName.toUpperCase(), boxName, masterToken, -1);
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, masterToken, roleName.toLowerCase(), boxName);
            RoleUtils.delete(TEST_CELL1, masterToken, roleName.toUpperCase(), boxName);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, masterToken, userName.toLowerCase(), HttpStatus.SC_NO_CONTENT);
            AccountUtils.delete(TEST_CELL1, masterToken, userName.toUpperCase(), HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ボックスと結びつくロールのトランスセルトークン内のロール確認. #13692
     * @throws TokenParseException TokenParseException
     * @throws TokenRootCrtException TokenRootCrtException
     * @throws TokenDsigException TokenDsigException
     */
    @Test
    public final void ボックスと結びつくロールのトランスセルトークン内のロール確認() throws TokenParseException,
            TokenDsigException, TokenRootCrtException {

        String testCellName = "cell401";
        String testCellName2 = "cell402";
        String boxNameNoneScheme = "box1";
        String userName = "user1";
        String pass = "password";
        String roleNameWithBox1 = "doctor";

        try {
            // 本テスト用セルの作成
            CellUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ボックスと結びつくロールのトランスセルトークン内のロール確認
            // Box作成(Box1)
            BoxUtils.create(testCellName, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME);

            // アカウント追加(user1)
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName,
                    userName, pass, HttpStatus.SC_CREATED);

            // ロール追加（スキーマなしBOXに結びつつく）
            RoleUtils.create(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox1,
                    boxNameNoneScheme, HttpStatus.SC_CREATED);

            // ロール結びつけ（スキーマなしBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.createLinks(testCellName, Account.EDM_TYPE_NAME, userName, null, Role.EDM_TYPE_NAME,
                    roleNameWithBox1, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // テストセル2
            CellUtils.create(testCellName2, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // ボックス作成2
            BoxUtils.create(testCellName2, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME);

            // extCell
            ExtCellUtils.create(AbstractCase.MASTER_TOKEN_NAME, testCellName2,
                    UrlUtils.cellRoot(testCellName));

            // ロール
            RoleUtils.create(testCellName2, AbstractCase.MASTER_TOKEN_NAME,
                    roleNameWithBox1, boxNameNoneScheme, HttpStatus.SC_CREATED);

            // extCellとロールの結びつけ
            LinksUtils.createLinksExtCell(testCellName2,
                    PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(testCellName)),
                    Role.EDM_TYPE_NAME, roleNameWithBox1, boxNameNoneScheme,
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // 認証（パスワード認証-トランセルトークン）
            TResponse res = Http.request("authn/password-tc-c0.txt")
                    .with("remoteCell", testCellName)
                    .with("username", userName)
                    .with("password", pass)
                    .with("p_target", UrlUtils.cellRoot(testCellName2))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            JSONObject json = res.bodyAsJson();
            String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // セルに対してトークン認証
            TResponse res2 =
                    Http.request("authn/saml-tc-c0.txt")
                            .with("remoteCell", testCellName2)
                            .with("assertion", transCellAccessToken)
                            .with("p_target", UrlUtils.cellRoot("schema1"))
                            .returns()
                            .statusCode(HttpStatus.SC_OK);

            JSONObject json2 = res2.bodyAsJson();
            String transCellAccessToken2 = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);
            TransCellAccessToken aToken2 = TransCellAccessToken.parse(transCellAccessToken2);
            assertEquals(UrlUtils.roleResource(testCellName2, DEFAULT_BOX_NAME, roleNameWithBox1),
                    aToken2.getRoles().get(0).createUrl());
        } finally {
            // ロール結びつけ削除（スキーマなしBOXに結びつくロールとアカウント結びつけ）
            LinksUtils.deleteLinks(testCellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleNameWithBox1, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME, -1);

            // ロールとextCellの結びつけ削除
            LinksUtils.deleteLinksExtCell(testCellName2,
                    PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(testCellName)),
                    Role.EDM_TYPE_NAME, roleNameWithBox1, boxNameNoneScheme, AbstractCase.MASTER_TOKEN_NAME, -1);

            // ExtCell削除
            ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName2,
                    UrlUtils.cellRoot(testCellName), -1);

            // アカウント削除
            AccountUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, userName, -1);

            // ロール削除
            RoleUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, roleNameWithBox1,
                    boxNameNoneScheme, -1);
            RoleUtils.delete(testCellName2, AbstractCase.MASTER_TOKEN_NAME,
                    roleNameWithBox1, boxNameNoneScheme, -1);

            // Box削除
            BoxUtils.delete(testCellName, AbstractCase.MASTER_TOKEN_NAME, boxNameNoneScheme, -1);
            BoxUtils.delete(testCellName2, AbstractCase.MASTER_TOKEN_NAME, boxNameNoneScheme, -1);

            // 本テスト用セルの削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, testCellName2, -1);

        }
    }

    /**
     * XDcCredentialが未指定のアカウントでのパスワード認証が出来ないことを確認.
     * @throws TokenParseException TokenParseException
     */
    @Test
    public final void XDcCredentialが未指定のアカウントでのパスワード認証が出来ないことを確認() throws TokenParseException {

        String userName = "blankUser";
        try {

            // アカウント追加(X-Personium-Credentialヘッダなし)
            AccountUtils.createNonCredential(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    userName, HttpStatus.SC_CREATED);

            // パスワード認証（password空文字）
            ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, userName, "", HttpStatus.SC_BAD_REQUEST);
            AuthTestCommon.waitForIntervalLock(); // アカウントロック回避

            // パスワード認証（password指定無し）
            Http.request("authn/password-cl-c0-no-password.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("username", userName)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            // アカウント削除
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, userName,
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * ACLに設定されたロール削除後のアクセス制御の確認.
     */
    @Test
    public final void ACLに設定されたロール削除後のアクセス制御の確認() {
        String roleNotDelete = "testRole001";
        String notDelRoleUser = "testUser001";
        String roleDelete = "testRole002";
        String pass = "password";
        String masterToken = AbstractCase.MASTER_TOKEN_NAME;

        try {
            // Accountの作成
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    notDelRoleUser, pass, HttpStatus.SC_CREATED);
            // Boxに紐付くロールの作成
            RoleUtils.create(TEST_CELL1, masterToken, roleNotDelete, Setup.TEST_BOX1, HttpStatus.SC_CREATED);
            RoleUtils.create(TEST_CELL1, masterToken, roleDelete, Setup.TEST_BOX1, HttpStatus.SC_CREATED);
            // RoleとAccountの結びつけ
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, notDelRoleUser, null,
                    Role.EDM_TYPE_NAME, roleNotDelete, Setup.TEST_BOX1, masterToken, HttpStatus.SC_NO_CONTENT);
            // Box1にACLの設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK,
                    Setup.TEST_BOX1, "", "box/acl-2role-setting.txt", roleNotDelete, roleDelete, Setup.TEST_BOX1,
                    "<D:read/>", "<D:write/>", "");

            // testRole002の削除
            RoleUtils.delete(TEST_CELL1, masterToken, roleDelete, Setup.TEST_BOX1, HttpStatus.SC_NO_CONTENT);

            // PROPFIND
            TResponse tresponse = DavResourceUtils.propfind("box/propfind-box-allprop.txt", masterToken,
                    HttpStatus.SC_MULTI_STATUS, Setup.TEST_BOX1);

            // testRole002が存在しない事を確認する=aceタグが１つのみ
            NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "ace");
            assertTrue(tresponse.getBody(), list.getLength() == 1);

            // testUser001が存在する事を確認する
            assertTrue(tresponse.getBody(), list.item(0).getTextContent().indexOf(roleNotDelete) > -1);

            // 認証
            String token1 = ResourceUtils.getMyCellLocalToken(TEST_CELL1, notDelRoleUser, pass);

            // GET 削除されたロールで「role not found」とならない事を確認
            ResourceUtils.retrieve(token1, "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        } finally {
            // ACLの初期化
            DavResourceUtils.setACL(TEST_CELL1, masterToken, HttpStatus.SC_OK,
                    "", ACL_AUTH_TEST_SETTING_FILE, Setup.TEST_BOX1, "");
            // RoleとAccountの結びつけの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, notDelRoleUser, null,
                    Role.EDM_TYPE_NAME, roleNotDelete, Setup.TEST_BOX1, masterToken, -1);
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, masterToken, roleNotDelete, Setup.TEST_BOX1, -1);
            RoleUtils.delete(TEST_CELL1, masterToken, roleDelete, Setup.TEST_BOX1, -1);
            // Accountの削除
            AccountUtils.delete(TEST_CELL1, masterToken, notDelRoleUser, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * CellACLを設定後にロール削除してCellレベル操作で403が返却されること.
     */
    @Test
    public final void CellACLを設定後にロール削除してCellレベル操作で403が返却されること() {
        String cellName = "aclroletest";
        String userName = "user";
        String pass = "password";
        String roleName = "testRole";

        try {
            // Create cell
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, userName, pass, HttpStatus.SC_CREATED);
            // Create role
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_CREATED);
            // Set acl
            setCellACL(cellName, roleName, AbstractCase.MASTER_TOKEN_NAME);
            // Link account-role
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // Get token
            String token = ResourceUtils.getMyCellLocalToken(cellName, userName, pass);
            // Delete role
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_NO_CONTENT);

            // Get account
            AccountUtils.get(token, HttpStatus.SC_FORBIDDEN, cellName, userName);
        } finally {
            // Delete link
            LinksUtils.deleteLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Delete account
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, userName, -1);
            // Delete cell
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * CellACLを設定後にロールをリネームしてCellレベル操作で200が返却されること.
     */
    @Test
    public final void CellACLを設定後にロールをリネームしてCellレベル操作で200が返却されること() {
        String cellName = "aclroletest";
        String userName = "user";
        String pass = "password";
        String roleName = "testRole";

        try {
            // Create cell
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, userName, pass, HttpStatus.SC_CREATED);
            // Create role
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_CREATED);
            // Set acl
            setCellACL(cellName, roleName, AbstractCase.MASTER_TOKEN_NAME);
            // Link account-role
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // Get token
            String token = ResourceUtils.getMyCellLocalToken(cellName, userName, pass);
            // Update role
            RoleUtils.update(AbstractCase.MASTER_TOKEN_NAME, cellName, roleName, roleName + "updated",
                    null, HttpStatus.SC_NO_CONTENT);

            // Get account
            AccountUtils.get(token, HttpStatus.SC_OK, cellName, userName);
        } finally {
            // Delete link
            LinksUtils.deleteLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName + "updated", null, -1);
            // Delete account
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, userName, -1);
            // Delete cell
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * CellACLを設定後にロール削除_再作成してCellレベル操作で403が返却されること.
     */
    @Test
    public final void CellACLを設定後にロール削除_再作成してCellレベル操作で403が返却されること() {
        String cellName = "aclroletest";
        String userName = "user";
        String pass = "password";
        String roleName = "testRole";

        try {
            // Create cell
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, userName, pass, HttpStatus.SC_CREATED);
            // Create role
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_CREATED);
            // Set acl
            setCellACL(cellName, roleName, AbstractCase.MASTER_TOKEN_NAME);
            // Link account-role
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);
            // Get token
            String token = ResourceUtils.getMyCellLocalToken(cellName, userName, pass);
            // Delete role
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_NO_CONTENT);
            // Create role
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, HttpStatus.SC_CREATED);

            // Get account
            AccountUtils.get(token, HttpStatus.SC_FORBIDDEN, cellName, userName);
        } finally {
            // Delete link
            LinksUtils.deleteLinks(cellName, Account.EDM_TYPE_NAME, userName, null,
                    Role.EDM_TYPE_NAME, roleName, null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, roleName, null, -1);
            // Delete account
            AccountUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, userName, -1);
            // Delete cell
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    private void setCellACL(String cellName, String roleName, String accessToken1) {
        Http.request("cell/acl-setting-single.txt")
                .with("url", cellName)
                .with("token", accessToken1)
                .with("role1", roleName)
                .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, roleName))
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    private List<Role> checkTransCellAccessToken(final String tokenAuthCellName, final String accountAuthCellName,
            final String userName, final String pass, final String roleName) {
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", accountAuthCellName)
                .with("username", userName)
                .with("password", pass)
                .with("p_target", UrlUtils.cellRoot(tokenAuthCellName))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        JSONObject json = res.bodyAsJson();
        String transCellAccessToken = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        // Cell1へのトークン認証
        TResponse res2 =
                Http.request("authn/saml-cl-c0.txt")
                        .with("remoteCell", tokenAuthCellName)
                        .with("assertion", transCellAccessToken)
                        .returns()
                        .statusCode(HttpStatus.SC_OK);

        JSONObject json2 = res2.bodyAsJson();
        String localToken2 = (String) json2.get(OAuth2Helper.Key.ACCESS_TOKEN);
        VisitorLocalAccessToken aToken = null;
        try {
            aToken = VisitorLocalAccessToken.parse(localToken2, UrlUtils.cellRoot(tokenAuthCellName));
        } catch (TokenParseException e) {
            fail();
        }
        return aToken.getRoles();
    }
}

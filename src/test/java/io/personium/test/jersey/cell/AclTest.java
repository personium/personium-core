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
package io.personium.test.jersey.cell;

import static org.junit.Assert.assertTrue;
import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALUNIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.bar.BarInstallTestUtils;
import io.personium.test.jersey.cell.ctl.BoxCrudTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.ReceivedMessageUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.SentMessageUtils;
import io.personium.test.utils.SignUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TestMethodUtils;
import io.personium.test.utils.UrlUtils;

/**
 * Cell level ACL testing.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class AclTest extends AbstractCase {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String TEST_ROLE1 = "role4";
    static final String TEST_ROLE2 = "role5";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static Logger log = LoggerFactory.getLogger(AclTest.class);


    /**
     * Constructor.
     */
    public AclTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * CellレベルACL設定の確認テスト.
     */
    @Test
    public final void CellレベルACL設定の確認() {

        try {

            // role4・role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-request.txt").with("url", TEST_CELL1).with("token", TOKEN)
                    .with("role1", TEST_ROLE1).with("role2", TEST_ROLE2)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt").with("url", TEST_CELL1)
                    .with("depth", "0").with("token", TOKEN).returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            rolList.add("read");
            map.put(TEST_ROLE1, rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("root");
            map2.put(TEST_ROLE2, rolList2);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

        } finally {
            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1).with("token", TOKEN).with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2).with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).with("level", "").returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }
    /**
     * Base URL of ACL is stored using localunit scheme whenever possible.
     * @throws ParseException
     */
    @Test
    public final void baseUrlStoredUsingLocalUnitSchemeWheneverPossible() throws ParseException {

        try {
            // Configure acl includng role4, role5 onto testcell1
            Http.request("cell/acl-setting-request.txt").with("url", TEST_CELL1).with("token", TOKEN)
                    .with("role1", TEST_ROLE1).with("role2", TEST_ROLE2)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            Cell cell = ModelFactory.cellFromName(TEST_CELL1);
            CellCmp cc = ModelFactory.cellCmp(cell);
            Acl acl = cc.getAcl();
            log.info(acl.toJSON());
            JSONObject j = (JSONObject) new JSONParser().parse(acl.toJSON());
            String base = (String)j.get("@xml.base");
            assertTrue(base.startsWith(SCHEME_LOCALUNIT));

        } finally {
            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1).with("token", TOKEN).with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2).with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).with("level", "").returns()
                    .statusCode(-1);
        }


    }

    /**
     * CellとBoxのACLを設定しBoxにアクセスできることを確認テスト.
     */
    @Test
    public final void CellとBoxにBoxレベルACLを設定しBoxにアクセスできることを確認() {
        String testBox1 = "testAclBox";

        try {
            // CellのACL設定 role4を含むACLをtestcell1
            Http.request("cell/acl-setting-cell-box.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            // 設定の確認 PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map1 = new HashMap<String, List<String>>();
            List<String> rolList1 = new ArrayList<String>();
            rolList1.add("auth");
            rolList1.add("auth-read");
            rolList1.add("read");
            map1.put(TEST_ROLE1, rolList1);
            list.add(map1);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("root");
            map2.put(TEST_ROLE2, rolList2);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                     UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);

            // ACLをbox1に設定すればできるのは従来からの機能
            // Principal:all Privilege:read
            // Principal:role1 Privilege:write
            setAclAllandRole(TEST_CELL1, TOKEN, HttpStatus.SC_OK, TEST_CELL1 + "/" + testBox1,
                "box/acl-setting-all-role.txt", "role1", "<D:read/>", "<D:write/>", "none");

            // PROPFINDでACLの確認
            CellUtils.propfind(TEST_CELL1 + "/" + testBox1, TOKEN, "0", HttpStatus.SC_MULTI_STATUS);

            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // account1でbox1を操作
            // リソースを取得 Box1に対してGET
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_OK, testBox1, TEST_CELL1);

            // コレクションの作成 Box1に対してPUT
            DavResourceUtils.createWebDavFile(TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", testBox1,
                    "text.txt", HttpStatus.SC_CREATED);

        } finally {
            // コレクションの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, TOKEN, "text.txt", -1, testBox1);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);

            // Box ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", TOKEN)
                    .with("level", "none")
                    .returns()
                    .statusCode(-1);

            // Cell ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", testBox1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .returns()
                    .statusCode(-1);
        }
    }


    /**
     * CellACLとしてreadwriteを設定しBoxにアクセスできること.
     */
    @Test
    public final void CellACLとしてreadwriteを設定しBoxにアクセスできること() {
        String testBox1 = "testNoAclBox";
        String testRole1 = "role1";

        try {
            // role4,role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-cell-none-box.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", testRole1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map1 = new HashMap<String, List<String>>();
            List<String> rolList1 = new ArrayList<String>();
            rolList1.add("read");
            rolList1.add("write");
            map1.put(testRole1, rolList1);
            list.add(map1);

            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                     UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);

            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // account1でbox1を操作
            // リソースを取得 Box1に対してGET
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_OK, testBox1, TEST_CELL1);

            // リソースの作成 Box1に対してPUT
            DavResourceUtils.createWebDavFile(TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", testBox1,
                    "text.txt", HttpStatus.SC_CREATED);

        } finally {
            // リソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, TOKEN, "text.txt", -1, testBox1);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);

            // Cell ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", testBox1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellACLとしてrootを設定しBoxにアクセスできること.
     */
    @Test
    public final void CellACLとしてrootを設定しBoxにアクセスできること() {
        String testBox1 = "testNoAclBox";
        String testRole1 = "role1";

        try {
            // role4,role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", testRole1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();

            List<String> rolList1 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList1.add("root");
            map2.put(testRole1, rolList1);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                     UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);

            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // account1でbox1を操作
            // リソースを取得 Box1に対してGET
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_OK, testBox1, TEST_CELL1);

            // リソースの作成 Box1に対してPUT
            DavResourceUtils.createWebDavFile(TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", testBox1,
                    "text.txt", HttpStatus.SC_CREATED);

        } finally {
            // リソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, TOKEN, "text.txt", -1, testBox1);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);

            // Cell ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", testBox1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellACLとしてauthを設定しBoxにアクセスできないレスポンス403が返却されること.
     */
    @Test
    public final void CellACLとしてauthを設定しBoxにアクセスできないレスポンス403が返却されること() {
        String testBox1 = "testNoAclBox";
        String testRole1 = "role1";

        try {
            // role4,role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-cell-none-auth.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", testRole1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();

            List<String> rolList1 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList1.add("auth");
            map2.put(testRole1, rolList1);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                     UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);

            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // account1でbox1を操作
            // リソースを取得 Box1に対してGET
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_FORBIDDEN, testBox1, TEST_CELL1);

            // リソースの作成 Box1に対してPUT
            DavResourceUtils.createWebDavFile(TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", testBox1,
                    "text.txt", HttpStatus.SC_FORBIDDEN);

        } finally {
            // リソースの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, TOKEN, "text.txt", -1, testBox1);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);

            // Cell ACLの設定を元に戻す
            Http.request("cell/acl-default.txt").with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", testBox1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * ACL設定.
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス
     * @param settingFile ACLリクエストファイル
     * @param role ACLに設定するPrincipal（Role）
     * @param privilege1 ACLに設定する権限1
     * @param privilege2 ACLに設定する権限2
     * @param level スキーマ認証level
     * @return レスポンス
     */
    private static TResponse setAclAllandRole(String cell, String token, int code, String path,
            String settingFile, String role, String privilege1, String privilege2, String level) {
        TResponse tresponseWebDav = null;
        // ACLの設定
        tresponseWebDav = Http.request(settingFile)
                .with("cellPath", cell)
                .with("colname", path)
                .with("token", token)
                .with("role", role)
                .with("privilege1", privilege1)
                .with("privilege2", privilege2)
                .with("roleBaseUrl", UrlUtils.roleResource(cell, null, ""))
                .with("level", level)
                .returns()
                .statusCode(code);
        return tresponseWebDav;
    }


    /**
     * 既にBoxに紐づかない同名のRoleが存在する状態でBoxに紐づかないRoleでのCellレベルACL設定の確認テスト.
     */
    @Test
    public final void 既にBoxに紐づかない同名のRoleが存在する状態でBoxに紐づかないRoleでのCellレベルACL設定の確認テスト() {
        String testBox = "testBox_27481Cell";
        String testRole = "testRole_27481Cell";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付くRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testRole, testBox, HttpStatus.SC_CREATED);
            // Boxに紐づかないRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testRole, null, HttpStatus.SC_CREATED);

            // ACLをtestcell1に設定
            Http.request("cell/acl-setting-single.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", testRole)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            map.put(testRole, rolList);
            list.add(map);

            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

        } finally {
            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            // Roleの削除(Boxに紐づく)
            RoleUtils.delete(TEST_CELL1, TOKEN, testRole, testBox);
            // Roleの削除(Boxに紐づかない)
            RoleUtils.delete(TEST_CELL1, TOKEN, testRole, null);
            // Boxの削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * CellレベルACL設定後のPROPPATCHの確認.
     */
    @Test
    public final void CellレベルACL設定後のPROPPATCHの確認() {

        try {

            // role4・role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-request.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            rolList.add("read");
            map.put(TEST_ROLE1, rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("root");
            map2.put(TEST_ROLE2, rolList2);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

            // PROPPATCH設定実行
            DavResourceUtils.setProppatch(TEST_CELL1, TOKEN, HttpStatus.SC_MULTI_STATUS, "author1", "hoge1");

            // PROPFINDでtestcell1のACLを取得
            tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            list = new ArrayList<Map<String, List<String>>>();
            map = new HashMap<String, List<String>>();
            rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            rolList.add("read");
            map.put(TEST_ROLE1, rolList);
            list.add(map);

            rolList2 = new ArrayList<String>();
            map2 = new HashMap<String, List<String>>();
            rolList2.add("root");
            map2.put(TEST_ROLE2, rolList2);
            list.add(map2);
            resorce = UrlUtils.cellRoot(TEST_CELL1);
            root = tresponse.bodyAsXml().getDocumentElement();

            sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);
        } finally {
            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellレベルACL設定hrefにフルパス設定の確認.
     */
    @Test
    public final void CellレベルACL設定hrefにフルパス設定の確認() {
        try {
            // role4・role5を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-request.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", UrlUtils.roleResource(TEST_CELL1, null, TEST_ROLE1))
                    .with("role2", UrlUtils.roleResource(TEST_CELL1, null, TEST_ROLE2))
                    .with("roleBaseUrl", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                    .with("url", TEST_CELL1)
                    .with("depth", "0")
                    .with("token", TOKEN)
                    .returns();
            tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            rolList.add("read");
            map.put(TEST_ROLE1, rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("root");
            map2.put(TEST_ROLE2, rolList2);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

        } finally {
            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellレベルACL設定hrefとbaseが異なるボックス設定の確認.
     */
    @Test
    public final void CellレベルACL設定hrefとbaseが異なるボックス設定の確認() {

        String box2 = "box2";
        try {
            // box2に紐付くロール作成
            RoleUtils.create(TEST_CELL1, TOKEN, "role02", box2, HttpStatus.SC_CREATED);
            RoleUtils.create(TEST_CELL1, TOKEN, "role03", box2, HttpStatus.SC_CREATED);

            // role2・role3を含むACLをtestcell1に設定
            Http.request("cell/acl-setting-base.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", UrlUtils.aclRelativePath(box2, "role02"))
                    .with("role2", UrlUtils.aclRelativePath(box2, "role03"))
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "box1"))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = CellUtils.propfind(TEST_CELL1, TOKEN, "0",
                    HttpStatus.SC_MULTI_STATUS);

            // PROPFOINDレスポンスボディの確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("auth");
            rolList.add("auth-read");
            map.put(UrlUtils.aclRelativePath("box2", "role02"), rolList);
            list.add(map);

            List<String> rolList2 = new ArrayList<String>();
            Map<String, List<String>> map2 = new HashMap<String, List<String>>();
            rolList2.add("auth");
            map2.put(UrlUtils.aclRelativePath("box2", "role03"), rolList2);
            list.add(map2);
            String resorce = UrlUtils.cellRoot(TEST_CELL1);
            Element root = tresponse.bodyAsXml().getDocumentElement();

            StringBuffer sb = new StringBuffer(resorce);

            // 1番最後の文字を削除する。
            sb.deleteCharAt(resorce.length() - 1);

            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, Box.MAIN_BOX_NAME, ""), null);

        } finally {
            // ロールの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, "role02", box2);
            RoleUtils.delete(TEST_CELL1, TOKEN, "role03", box2);

            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellレベルACL設定済みのロール削除時の確認.
     */
    @Test
    public final void CellレベルACL設定済みのロール削除時の確認() {

        String box2 = "box2";
        String roleNotDelete = "role001";
        String roleDelete = "role002";
        try {
            // box2に紐付くロール作成
            RoleUtils.create(TEST_CELL1, TOKEN, roleNotDelete, box2, HttpStatus.SC_CREATED);
            RoleUtils.create(TEST_CELL1, TOKEN, roleDelete, box2, HttpStatus.SC_CREATED);

            // ACLをtestcell1に設定
            Http.request("cell/acl-setting-base.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", UrlUtils.aclRelativePath(box2, roleNotDelete))
                    .with("role2", UrlUtils.aclRelativePath(box2, roleDelete))
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "box1"))
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // roleを削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleDelete, box2, HttpStatus.SC_NO_CONTENT);

            // PROPFINDでtestcell1のACLを取得
            TResponse tresponse = CellUtils.propfind(TEST_CELL1, TOKEN, "0",
                    HttpStatus.SC_MULTI_STATUS);

            // role002が存在しない事を確認する=aceタグが１つのみ
            NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "ace");
            assertTrue(tresponse.getBody(), list.getLength() == 1);

            // role001が存在する事を確認する
            assertTrue(tresponse.getBody(), list.item(0).getTextContent().indexOf(roleNotDelete) > -1);

        } finally {
            // ロールの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleNotDelete, box2, -1);
            RoleUtils.delete(TEST_CELL1, TOKEN, roleDelete, box2, -1);

            // ACLの設定を元に戻す
            Http.request("cell/acl-default.txt")
                    .with("url", TEST_CELL1)
                    .with("token", TOKEN)
                    .with("role1", TEST_ROLE1)
                    .with("role2", TEST_ROLE2)
                    .with("box", Setup.TEST_BOX1)
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * CellレベルACL設定アクセス制御確認.
     */
    @Test
    public final void CellレベルACL設定アクセス制御確認() {
        List<String> account = new ArrayList<String>();
        // テスト用トークンの作成
        account = accountAuth();
        // Cellアクセス制御のテスト testcell1

        // Account
        accountAclTest(account);

        // Role
        roleAclTest(account);

        // Relation
        RelationAclTest(account);

        // ExtRole
        extRoleAclTest(account);

        // ReceivedMessage
        receivedMessageTest(account);

        // SentMessage
        sentMessageTest(account);

        // ApprovedMessage
        approvedMessageTest(account);

        // Event
        eventAclTest(account);

        // log
        logAclTest(account);

        // log PROPFIND
        logListAclTest(account);

        // PROPFIND
        propfindAclTest(account);

        // PROPPACTHはユニットユーザのみ実行可能とするため、ここではテスト不要

        // extCell
        extCellAclTest(account);

        // box
        boxAclTest(account);

        // bar-install
        barInstallAclTest(account);

        // OPTIONS Privilege social-readが必要
        ResourceUtils.requestUtil("OPTIONS", account.get(0), "/" + TEST_CELL1, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("OPTIONS", account.get(1), "/" + TEST_CELL1, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("OPTIONS", account.get(4), "/" + TEST_CELL1, HttpStatus.SC_OK);
        ResourceUtils.requestUtil("OPTIONS", account.get(10), "/" + TEST_CELL1, HttpStatus.SC_OK);
        ResourceUtils.requestUtil("OPTIONS", account.get(14), "/" + TEST_CELL1, HttpStatus.SC_OK);

        // ACL Privilege aclが必要
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(TEST_CELL1, account.get(0), HttpStatus.SC_FORBIDDEN, "", aclTestFile, Setup.TEST_BOX1,
                "");
        DavResourceUtils.setACL(TEST_CELL1, account.get(3), HttpStatus.SC_FORBIDDEN, "", aclTestFile, Setup.TEST_BOX1,
                "");
        DavResourceUtils.setACL(TEST_CELL1, account.get(6), HttpStatus.SC_OK, "", aclTestFile, Setup.TEST_BOX1,
                "");
        DavResourceUtils.setACL(TEST_CELL1, account.get(10), HttpStatus.SC_OK, "", aclTestFile, Setup.TEST_BOX1,
                "");
        DavResourceUtils.setACL(TEST_CELL1, account.get(16), HttpStatus.SC_FORBIDDEN, "", aclTestFile, Setup.TEST_BOX1,
                "");

        // sign
        signTest(account);
    }

    /**
     * CellレベルACL設定アクセス制御$link確認.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void CellレベルACL設定アクセス制御$link確認() {
        String extCellUrl = CommonUtils.encodeUrlComp(UrlUtils.cellRoot(Setup.TEST_CELL2));
        String relationName = "testRelation";
        try {
            List<String> account = new ArrayList<String>();
            // account
            account = accountAuth();
            String roleName = "role8";

            // $link accountとroleの$link→AUTH権限が必要
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account10", null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(0), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account10", null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(2), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account10", null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(1), HttpStatus.SC_NO_CONTENT);
            // 削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account10", null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(10), HttpStatus.SC_NO_CONTENT);

            // $link Relationとroleの$link→SOCIALとAUTH権限が必要
            String roleUri = UrlUtils.roleUrl(TEST_CELL1, null, roleName);
            // Relationの作成
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", null);
            RelationUtils.create(TEST_CELL1, TOKEN, body, HttpStatus.SC_CREATED);

            LinksUtils.createLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(0), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(1), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(4), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, account.get(9), HttpStatus.SC_NO_CONTENT);
            // 削除
            LinksUtils.deleteLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);

            // $link RelationとextCellの$link→SOCIAL権限が必要
            // extCellの作成
            ExtCellUtils.create(TOKEN, TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2));

            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Relation.EDM_TYPE_NAME, relationName, null,
                    account.get(0), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Relation.EDM_TYPE_NAME, relationName, null,
                    account.get(1), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Relation.EDM_TYPE_NAME, relationName, null,
                    account.get(4), HttpStatus.SC_NO_CONTENT);
            // 削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1,
                    CommonUtils.encodeUrlComp(UrlUtils.cellRoot(Setup.TEST_CELL2)),
                    Relation.EDM_TYPE_NAME, relationName, null, account.get(10), HttpStatus.SC_NO_CONTENT);

            // $link extCellとrole→SOCIALとAUTHの権限が必要
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Role.EDM_TYPE_NAME, "role1", null,
                    account.get(0), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Role.EDM_TYPE_NAME, "role1", null,
                    account.get(1), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Role.EDM_TYPE_NAME, "role1", null,
                    account.get(4), HttpStatus.SC_FORBIDDEN);
            LinksUtils.createLinksExtCell(TEST_CELL1, extCellUrl, Role.EDM_TYPE_NAME, "role1", null,
                    account.get(9), HttpStatus.SC_NO_CONTENT);

            // 削除
            LinksUtils.deleteLinksExtCell(Setup.TEST_CELL1,
                    CommonUtils.encodeUrlComp(UrlUtils.cellRoot(Setup.TEST_CELL2)),
                    Role.EDM_TYPE_NAME, "role1", null, account.get(10), HttpStatus.SC_NO_CONTENT);
        } finally {
            // Relationの削除
            RelationUtils.delete(TEST_CELL1, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * 存在しないCellをxml:baseに指定してCellレベルACL設定をした場合400エラーが返却されること.
     */
    @Test
    public final void 存在しないCellをxml_baseに指定してCellレベルACL設定をした場合400エラーが返却されること() {
        Http.request("cell/acl-setting-single-request.txt")
                .with("url", Setup.TEST_CELL1)
                .with("token", TOKEN)
                .with("roleBaseUrl", UrlUtils.roleResource("notExistsCell", "__", ""))
                .with("role", TEST_ROLE1)
                .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * 存在しないCellに対してACL設定をした場合404エラーが返却されること.
     */
    @Test
    public final void 存在しないCellに対してACL設定をした場合404エラーが返却されること() {
        TResponse res = Http.request("cell/acl-setting-all.txt")
                .with("url", Setup.TEST_CELL1 + "notexist")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, "role"))
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
        PersoniumCoreException expectedException = PersoniumCoreException.Dav.CELL_NOT_FOUND;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 存在しないBoxに対してCellレベルACL設定をした場合400エラーが返却されること.
     */
    @Test
    public final void 存在しないBoxに対してCellレベルACL設定をした場合400エラーが返却されること() {
        Http.request("cell/acl-setting-single-request.txt")
                .with("url", Setup.TEST_CELL1)
                .with("token", TOKEN)
                .with("roleBaseUrl", UrlUtils.roleResource(Setup.TEST_CELL1, "noneExistBox", ""))
                .with("role", TEST_ROLE1)
                .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Roleに紐付いていないBox名を指定した場合400エラーが返却されること.
     */
    @Test
    public final void Roleに紐付いていないBox名を指定した場合400エラーが返却されること() {
        String boxName = "noneLinkedBox";
        try {
            // Roleと紐ついていないBoxを登録
            PersoniumRequest req = PersoniumRequest.post(UrlUtils.cellCtl(Setup.TEST_CELL1, "Box"));
            String[] key = {"Name" };
            String[] value = {boxName };
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN).addJsonBody(key, value);
            request(req);

            Http.request("cell/acl-setting-single-request.txt")
                    .with("url", Setup.TEST_CELL1)
                    .with("token", TOKEN)
                    .with("roleBaseUrl", UrlUtils.roleResource(Setup.TEST_CELL1, boxName, ""))
                    .with("role", TEST_ROLE1)
                    .with("privilege", "<D:read/></D:privilege><D:privilege><D:write/>")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // Boxの削除
            BoxCrudTest.deleteBoxRequest(boxName).returns();
        }
    }

    /**
     * principalに不正なタグを設定した場合400エラーが返却されること.
     */
    @Test
    public final void principalに不正なタグを設定した場合400エラーが返却されること() {
        String body = "<D:acl xmlns:D='DAV:' xml:base='"
                + UrlUtils.roleResource(TEST_CELL1, null, Setup.TEST_BOX1) + "'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:test/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        TResponse res = Http.request("cell/acl-setting-none-body.txt")
                .with("url", Setup.TEST_CELL1)
                .with("token", TOKEN)
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                "Principal is neither href nor all");
        res.checkErrorResponse(expected.getCode(), expected.getMessage());
    }

    /**
     * 認証で使用されたAccountに対象のRoleが存在しないかつ権限が設定されていないRoleが紐ついている場合にリクエストを実行した場合_403が返却されること.
     * テスト観点はチケット#34823「アクセス権限のチェックにて、対象のRoleが存在しない場合500エラーが発生」を参照
     */
    @Test
    public final void 認証で使用されたAccountに対象のRoleが存在しないかつ権限が設定されていないRoleが紐ついている場合にリクエストを実行した場合_403が返却されること() {
        String cellName = "cellacltest";
        String account = "accountAclTest";
        String role1 = "roleAclTest1";
        String role2 = "roleAclTest2";
        try {
            // 前準備として、CellにACLを設定されているRoleを全て削除して、内部的にACLの設定が「<acl><ace/></acl>」のようなaceが空の状態を設定する
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, -1);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, account, "password", -1);
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, role1, -1);
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, account, null,
                    Role.EDM_TYPE_NAME, role1, null, AbstractCase.MASTER_TOKEN_NAME, -1);

            // CellにACL設定
            Http.request("cell/acl-setting-single.txt").with("url", cellName).with("token", TOKEN)
                    .with("role1", role1)
                    .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, role1))
                    .returns()
                    .statusCode(-1);

            // 削除するRoleと紐付いているアカウントの認証トークン取得
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(cellName, account, "password", -1);
            String accessToken = json.get("access_token").toString();

            // ACL設定がされたRoleの削除
            AccountUtils.deleteLinksWithRole(cellName, null, AbstractCase.MASTER_TOKEN_NAME, account, role1, -1);
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, role1, null);

            // アクセスするアカウントはRoleと結びついていないとaceのチェック前で権限エラーとなるため、ACL設定がされていないRoleの作成
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, role2, -1);
            LinksUtils.createLinks(cellName, Account.EDM_TYPE_NAME, account, null,
                    Role.EDM_TYPE_NAME, role2, null, AbstractCase.MASTER_TOKEN_NAME, -1);

            // ここから実際のテスト
            // Cellレベルに認証トークンを使用してリクエストを実行(403が返却されること)
            RoleUtils.list(accessToken, cellName, HttpStatus.SC_FORBIDDEN);
        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, cellName);
        }

    }

    private List<String> accountAuth() {
        List<String> result = new ArrayList<String>();

        // 0 account1 アクセス権無し
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account1", "password1"));
        // 1 account10 AUTH
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account10", "password10"));
        // 2 account11 MESSAGE
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account11", "password11"));
        // 3 account12 EVENT
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account12", "password12"));
        // 4 account13 SOCIAL
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account13", "password13"));
        // 5 account14 BOX
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account14", "password14"));
        // 6 account15 ACL
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account15", "password15"));
        // 7 account17 PROPFINDのみ
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account17", "password17"));
        // 8 account18 PROPFIND＋acl
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account18", "password18"));
        // 9 account19 AUTH＋SOCIAL
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account19", "password19"));
        // 10 account20 ROOT
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account20", "password20"));
        // 11 account21 auth-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account21", "password21"));
        // 12 account22 message-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account22", "password22"));
        // 13 account23 event-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account23", "password23"));
        // 14 account24 social-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account24", "password24"));
        // 15 account25 box-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account25", "password25"));
        // 16 account26 acl-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account26", "password26"));
        // 17 account27 bar-install
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account27", "password27"));
        // 18 account28 message/social
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account28", "password28"));
        // 19 account29 log
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account29", "password29"));
        // 20 account30 log-read
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account30", "password30"));
        // 21 account31 sign
        result.add(ResourceUtils.getMyCellLocalToken(TEST_CELL1, "account31", "password31"));

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(TEST_CELL1, TOKEN, HttpStatus.SC_OK, "", aclTestFile, Setup.TEST_BOX1,
                "");

        return result;
    }

    private void accountAclTest(List<String> account) {
        String testUserName = "testhoge";
        String testUserName2 = "testhoge2";
        String testUserName3 = "testhoge3";
        String testPassWord = "passhoge";
        // accountの作成 authが必要
        AccountUtils.create(account.get(0), TEST_CELL1, testUserName, testPassWord,
                HttpStatus.SC_FORBIDDEN);
        AccountUtils.create(account.get(1), TEST_CELL1, testUserName, testPassWord,
                HttpStatus.SC_CREATED);
        AccountUtils.create(account.get(2), TEST_CELL1, testUserName2, testPassWord,
                HttpStatus.SC_FORBIDDEN);
        AccountUtils.create(account.get(9), TEST_CELL1, testUserName2, testPassWord,
                HttpStatus.SC_CREATED);
        AccountUtils.create(account.get(10), TEST_CELL1, testUserName3, testPassWord,
                HttpStatus.SC_CREATED);
        AccountUtils.create(account.get(11), TEST_CELL1, testUserName2, testPassWord,
                HttpStatus.SC_FORBIDDEN);
        // accountの取得 auth-readが必要
        AccountUtils.get(account.get(0), HttpStatus.SC_FORBIDDEN, TEST_CELL1, testUserName);
        AccountUtils.get(account.get(1), HttpStatus.SC_OK, TEST_CELL1, testUserName);
        AccountUtils.get(account.get(2), HttpStatus.SC_FORBIDDEN, TEST_CELL1, testUserName);
        AccountUtils.get(account.get(9), HttpStatus.SC_OK, TEST_CELL1, testUserName);
        AccountUtils.get(account.get(10), HttpStatus.SC_OK, TEST_CELL1, testUserName);
        AccountUtils.get(account.get(11), HttpStatus.SC_OK, TEST_CELL1, testUserName);
        // accountの更新 authが必要
        AccountUtils.update(account.get(0), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_FORBIDDEN);
        AccountUtils.update(account.get(1), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_NO_CONTENT);
        AccountUtils.update(account.get(2), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_FORBIDDEN);
        AccountUtils.update(account.get(9), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_NO_CONTENT);
        AccountUtils.update(account.get(10), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_NO_CONTENT);
        AccountUtils.update(account.get(11), TEST_CELL1,
                testUserName, testUserName, testPassWord, HttpStatus.SC_FORBIDDEN);
        // accountの削除 authが必要
        AccountUtils.delete(TEST_CELL1, account.get(0), testUserName, HttpStatus.SC_FORBIDDEN);
        AccountUtils.delete(TEST_CELL1, account.get(1), testUserName, HttpStatus.SC_NO_CONTENT);
        AccountUtils.delete(TEST_CELL1, account.get(2), testUserName2, HttpStatus.SC_FORBIDDEN);
        AccountUtils.delete(TEST_CELL1, account.get(9), testUserName2, HttpStatus.SC_NO_CONTENT);
        AccountUtils.delete(TEST_CELL1, account.get(10), testUserName3, HttpStatus.SC_NO_CONTENT);
        AccountUtils.delete(TEST_CELL1, account.get(11), testUserName2, HttpStatus.SC_FORBIDDEN);
    }

    private void roleAclTest(List<String> account) {
        String testRoleName = "testRole1";
        String testRoleName2 = "testRole2";
        String testRoleName3 = "testRole3";
        // Roleの作成 POST authが必要
        RoleUtils.create(TEST_CELL1, account.get(0), testRoleName, null, HttpStatus.SC_FORBIDDEN);
        RoleUtils.create(TEST_CELL1, account.get(1), testRoleName, null, HttpStatus.SC_CREATED);
        RoleUtils.create(TEST_CELL1, account.get(3), testRoleName, null, HttpStatus.SC_FORBIDDEN);
        RoleUtils.create(TEST_CELL1, account.get(9), testRoleName2, null, HttpStatus.SC_CREATED);
        RoleUtils.create(TEST_CELL1, account.get(10), testRoleName3, null, HttpStatus.SC_CREATED);
        RoleUtils.create(TEST_CELL1, account.get(11), testRoleName, null, HttpStatus.SC_FORBIDDEN);

        // Roleの取得 GET auth-readが必要
        RoleUtils.list(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN);
        RoleUtils.list(account.get(1), TEST_CELL1, HttpStatus.SC_OK);
        RoleUtils.list(account.get(3), TEST_CELL1, HttpStatus.SC_FORBIDDEN);
        RoleUtils.list(account.get(9), TEST_CELL1, HttpStatus.SC_OK);
        RoleUtils.list(account.get(10), TEST_CELL1, HttpStatus.SC_OK);
        RoleUtils.list(account.get(11), TEST_CELL1, HttpStatus.SC_OK);

        // Roleの更新 PUT authが必要
        RoleUtils.update(account.get(0), TEST_CELL1, testRoleName, testRoleName, null,
                HttpStatus.SC_FORBIDDEN);
        RoleUtils.update(account.get(1), TEST_CELL1, testRoleName, testRoleName, null,
                HttpStatus.SC_NO_CONTENT);
        RoleUtils.update(account.get(3), TEST_CELL1, testRoleName, testRoleName, null,
                HttpStatus.SC_FORBIDDEN);
        RoleUtils.update(account.get(9), TEST_CELL1, testRoleName2, testRoleName2, null,
                HttpStatus.SC_NO_CONTENT);
        RoleUtils.update(account.get(10), TEST_CELL1, testRoleName3, testRoleName3, null,
                HttpStatus.SC_NO_CONTENT);
        RoleUtils.update(account.get(11), TEST_CELL1, testRoleName, testRoleName, null,
                HttpStatus.SC_FORBIDDEN);

        // Roleの削除 DELETE authが必要
        RoleUtils.delete(TEST_CELL1, account.get(0), testRoleName, null, HttpStatus.SC_FORBIDDEN);
        RoleUtils.delete(TEST_CELL1, account.get(1), testRoleName, null, HttpStatus.SC_NO_CONTENT);
        RoleUtils.delete(TEST_CELL1, account.get(3), testRoleName, null, HttpStatus.SC_FORBIDDEN);
        RoleUtils.delete(TEST_CELL1, account.get(9), testRoleName2, null, HttpStatus.SC_NO_CONTENT);
        RoleUtils.delete(TEST_CELL1, account.get(10), testRoleName3, null, HttpStatus.SC_NO_CONTENT);
        RoleUtils.delete(TEST_CELL1, account.get(11), testRoleName, null, HttpStatus.SC_FORBIDDEN);

    }

    @SuppressWarnings("unchecked")
    private void RelationAclTest(List<String> account) {
        String relationName = "RelationAclTest";
        String relationName2 = "RelationAclTest2";
        String relationName3 = "RelationAclTest3";
        String relationName4 = "RelationAclTest4";

        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", null);
        JSONObject body2 = new JSONObject();
        body2.put("Name", relationName2);
        body2.put("_Box.Name", null);
        JSONObject body3 = new JSONObject();
        body3.put("Name", relationName3);
        body3.put("_Box.Name", null);
        try {
            // relation作成 POST socialが必要
            RelationUtils.create(TEST_CELL1, account.get(0), body, HttpStatus.SC_FORBIDDEN);
            RelationUtils.create(TEST_CELL1, account.get(1), body, HttpStatus.SC_FORBIDDEN);
            RelationUtils.create(TEST_CELL1, account.get(4), body, HttpStatus.SC_CREATED);
            RelationUtils.create(TEST_CELL1, account.get(9), body2, HttpStatus.SC_CREATED);
            RelationUtils.create(TEST_CELL1, account.get(10), body3, HttpStatus.SC_CREATED);
            RelationUtils.create(TEST_CELL1, account.get(14), body, HttpStatus.SC_FORBIDDEN);
            // relation取得 GET social-readが必要
            RelationUtils.get(TEST_CELL1, account.get(0), relationName3, null, HttpStatus.SC_FORBIDDEN);
            RelationUtils.get(TEST_CELL1, account.get(1), relationName3, null, HttpStatus.SC_FORBIDDEN);
            RelationUtils.get(TEST_CELL1, account.get(4), relationName3, null, HttpStatus.SC_OK);
            RelationUtils.get(TEST_CELL1, account.get(9), relationName3, null, HttpStatus.SC_OK);
            RelationUtils.get(TEST_CELL1, account.get(10), relationName3, null, HttpStatus.SC_OK);
            RelationUtils.get(TEST_CELL1, account.get(14), relationName3, null, HttpStatus.SC_OK);
            // relation更新 PUT socialが必要
            RelationUtils.update(TEST_CELL1, account.get(0), relationName, null, relationName4, "null",
                    HttpStatus.SC_FORBIDDEN);
            RelationUtils.update(TEST_CELL1, account.get(1), relationName, null, relationName4, "null",
                    HttpStatus.SC_FORBIDDEN);
            RelationUtils.update(TEST_CELL1, account.get(4), relationName, null, relationName4, "null",
                    HttpStatus.SC_NO_CONTENT);
            RelationUtils.update(TEST_CELL1, account.get(9), relationName4, null, relationName, "null",
                    HttpStatus.SC_NO_CONTENT);
            RelationUtils.update(TEST_CELL1, account.get(10), relationName, null, relationName4, "null",
                    HttpStatus.SC_NO_CONTENT);
            RelationUtils.update(TEST_CELL1, account.get(14), relationName, null, relationName4, "null",
                    HttpStatus.SC_FORBIDDEN);
        } finally {
            // relation削除 DELETE /${cellPath}/__ctl/Relation
            RelationUtils.delete(TEST_CELL1, account.get(0), relationName, null, HttpStatus.SC_FORBIDDEN);
            RelationUtils.delete(TEST_CELL1, account.get(1), relationName, null, HttpStatus.SC_FORBIDDEN);
            RelationUtils.delete(TEST_CELL1, account.get(4), relationName4, null, HttpStatus.SC_NO_CONTENT);
            RelationUtils.delete(TEST_CELL1, account.get(9), relationName2, null, HttpStatus.SC_NO_CONTENT);
            RelationUtils.delete(TEST_CELL1, account.get(10), relationName3, null, HttpStatus.SC_NO_CONTENT);
            RelationUtils.delete(TEST_CELL1, account.get(14), relationName3, null, HttpStatus.SC_FORBIDDEN);
        }
    }

    @SuppressWarnings("unchecked")
    private void extRoleAclTest(List<String> account) {
        // Relationの作成
        String relationName = "relationtest";
        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", null);

        String extCellNameBase = UrlUtils.roleResource(TEST_CELL1, "__", "testhoge");
        String extRoleName2 = UrlUtils.roleResource(TEST_CELL1, "__", "testhoge2");
        String extRoleName3 = UrlUtils.roleResource(TEST_CELL1, "__", "testhoge3");
        String extCellNameReName = UrlUtils.roleResource(TEST_CELL1, "__", "testrename");
        JSONObject extRoleBody = new JSONObject();
        extRoleBody.put("ExtRole", extCellNameBase);
        extRoleBody.put("_Relation.Name", relationName);
        extRoleBody.put("_Relation._Box.Name", null);
        JSONObject extRoleBody2 = new JSONObject();
        extRoleBody2.put("ExtRole", extRoleName2);
        extRoleBody2.put("_Relation.Name", relationName);
        extRoleBody2.put("_Relation._Box.Name", null);
        JSONObject extRoleBody3 = new JSONObject();
        extRoleBody3.put("ExtRole", extRoleName3);
        extRoleBody3.put("_Relation.Name", relationName);
        extRoleBody3.put("_Relation._Box.Name", null);

        try {
            RelationUtils.create(TEST_CELL1, account.get(4), body, HttpStatus.SC_CREATED);

            // extRoleの作成 POST /${cellPath}/__ctl/ExtRole authが必要
            ExtRoleUtils.create(account.get(0), TEST_CELL1, extRoleBody, HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.create(account.get(1), TEST_CELL1, extRoleBody, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(account.get(2), TEST_CELL1, extRoleBody, HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.create(account.get(9), TEST_CELL1, extRoleBody2, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(account.get(10), TEST_CELL1, extRoleBody3, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(account.get(11), TEST_CELL1, extRoleBody3, HttpStatus.SC_FORBIDDEN);
            // extRoleの取得 auth-readが必要
            ExtRoleUtils.get(account.get(0), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.get(account.get(1), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_OK);
            ExtRoleUtils.get(account.get(2), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.get(account.get(9), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_OK);
            ExtRoleUtils.get(account.get(10), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_OK);
            ExtRoleUtils.get(account.get(11), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    HttpStatus.SC_OK);

            // extRoleの更新 authが必要
            ExtRoleUtils.update(account.get(0), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_FORBIDDEN);

            ExtRoleUtils.update(account.get(1), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);
            // もとに戻す
            ExtRoleUtils.update(TOKEN, TEST_CELL1, extCellNameReName, "'" + relationName + "'", "null",
                    extCellNameBase, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);

            ExtRoleUtils.update(account.get(2), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_FORBIDDEN);

            ExtRoleUtils.update(account.get(9), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null",
                    extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);
            // もとに戻す
            ExtRoleUtils.update(TOKEN, TEST_CELL1, extCellNameReName, "'" + relationName + "'", "null",
                    extCellNameBase, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);

            ExtRoleUtils.update(account.get(10), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null", extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);

            ExtRoleUtils.update(account.get(11), TEST_CELL1, extCellNameBase, "'" + relationName + "'",
                    "null", extCellNameReName, "\"" + relationName + "\"", "null", HttpStatus.SC_FORBIDDEN);
            // もとに戻す
            ExtRoleUtils.update(TOKEN, TEST_CELL1, extCellNameReName, "'" + relationName + "'", "null",
                    extCellNameBase, "\"" + relationName + "\"", "null", HttpStatus.SC_NO_CONTENT);

            // extRoleの削除 authが必要
            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(0), HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(1), HttpStatus.SC_NO_CONTENT);
            // 再作成
            ExtRoleUtils.create(TOKEN, TEST_CELL1, extRoleBody, HttpStatus.SC_CREATED);

            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(2), HttpStatus.SC_FORBIDDEN);
            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(9), HttpStatus.SC_NO_CONTENT);
            // 再作成
            ExtRoleUtils.create(TOKEN, TEST_CELL1, extRoleBody, HttpStatus.SC_CREATED);

            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(10), HttpStatus.SC_NO_CONTENT);
            // 再作成
            ExtRoleUtils.create(TOKEN, TEST_CELL1, extRoleBody, HttpStatus.SC_CREATED);
            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName,
                    null, account.get(11), HttpStatus.SC_FORBIDDEN);
        } finally {
            // extRoleの削除
            ExtRoleUtils.delete(TEST_CELL1, extCellNameBase, relationName, null, TOKEN, -1);
            ExtRoleUtils.delete(TEST_CELL1, extRoleName2, relationName, null, TOKEN, -1);
            ExtRoleUtils.delete(TEST_CELL1, extRoleName3, relationName, null, TOKEN, -1);
            // Relationの削除
            RelationUtils.delete(TEST_CELL1, TOKEN, relationName, null, -1);
        }
    }

    private void receivedMessageTest(List<String> account) {
        // メッセージ受信はMessageTest.javaでテストを実施しているため、ここではテストしない

        // メッセージ1件取得
        // アクセス権無し
        ReceivedMessageUtils.get(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");
        // MESSAGE
        ReceivedMessageUtils.get(account.get(2), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
        // ROOT
        ReceivedMessageUtils.get(account.get(10), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
        // message-read
        ReceivedMessageUtils.get(account.get(12), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");

        // メッセージ一覧取得
        // アクセス権無し
        ReceivedMessageUtils.list(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN);
        // MESSAGE
        ReceivedMessageUtils.list(account.get(2), TEST_CELL1, HttpStatus.SC_OK);
        // ROOT
        ReceivedMessageUtils.list(account.get(10), TEST_CELL1, HttpStatus.SC_OK);
        // message-read
        ReceivedMessageUtils.list(account.get(12), TEST_CELL1, HttpStatus.SC_OK);

        // メッセージ削除
        // アクセス権無し
        ReceivedMessageUtils.delete(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");
        // MESSAGE
        ReceivedMessageUtils.delete(account.get(2), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
        // ROOT
        ReceivedMessageUtils.delete(account.get(10), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
        // message-read
        ReceivedMessageUtils.delete(account.get(12), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");
    }

    private void sentMessageTest(List<String> account) {
        String message01 = "{\"To\":\"" + UrlUtils.cellRoot(Setup.TEST_CELL2)
                + "\",\"Title\":\"test mail\",\"Body\":\"test body01\"}";

        TResponse response1 = null;
        TResponse response2 = null;

        try {
            // メッセージ送信
            // アクセス権無し
            SentMessageUtils.sent(account.get(0), TEST_CELL1, message01, HttpStatus.SC_FORBIDDEN);
            // MESSAGE
            response1 = SentMessageUtils.sent(account.get(2), TEST_CELL1, message01, HttpStatus.SC_CREATED);
            // ROOT
            response2 = SentMessageUtils.sent(account.get(10), TEST_CELL1, message01, HttpStatus.SC_CREATED);
            // message-read
            SentMessageUtils.sent(account.get(12), TEST_CELL1, message01, HttpStatus.SC_FORBIDDEN);

            // メッセージ1件取得
            // アクセス権無し
            SentMessageUtils.get(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");
            // MESSAGE
            SentMessageUtils.get(account.get(2), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
            // ROOT
            SentMessageUtils.get(account.get(10), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
            // message-read
            SentMessageUtils.get(account.get(12), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");

            // メッセージ一覧取得
            // アクセス権無し
            SentMessageUtils.list(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN);
            // MESSAGE
            SentMessageUtils.list(account.get(2), TEST_CELL1, HttpStatus.SC_OK);
            // ROOT
            SentMessageUtils.list(account.get(10), TEST_CELL1, HttpStatus.SC_OK);
            // message-read
            SentMessageUtils.list(account.get(12), TEST_CELL1, HttpStatus.SC_OK);

            // メッセージ削除
            // アクセス権無し
            SentMessageUtils.delete(account.get(0), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");
            // MESSAGE
            SentMessageUtils.delete(account.get(2), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
            // ROOT
            SentMessageUtils.delete(account.get(10), TEST_CELL1, HttpStatus.SC_NOT_FOUND, "acltest");
            // message-read
            SentMessageUtils.delete(account.get(12), TEST_CELL1, HttpStatus.SC_FORBIDDEN, "acltest");

        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            if (response2 != null) {
                ODataCommon.deleteOdataResource(response2.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL2, UrlUtils.cellRoot(TEST_CELL1), "message", "test mail", "test body01");
        }
    }

    /**
     * メッセージ承認に関するアクセス制御テスト. TODO 関係登録依頼、関係削除依頼の実装後、messageに関するテストを有効にすること
     * @param account アカウント情報
     */
    private void approvedMessageTest(List<String> account) {
        TResponse rcvRes1 = null, rcvRes2 = null, rcvRes3 = null;
        TResponse apvRes1 = null, apvRes2 = null, apvRes3 = null;
        TResponse apvRes4 = null, apvRes5 = null, apvRes6 = null, apvRes7 = null;

        try {
            // 受信メッセージ(Type:message)
            String body = getReceivedMessageBody("message", null, "12345678901234567890123456789012");
            rcvRes1 = ReceivedMessageUtils.receive(null, TEST_CELL1, body, HttpStatus.SC_CREATED);
            JSONObject results = (JSONObject) ((JSONObject) rcvRes1.bodyAsJson().get("d")).get("results");
            String uuid = (String) results.get("__id");
            // NG: アクセス権無し
            ReceivedMessageUtils.read(account.get(0), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social
            ReceivedMessageUtils.read(account.get(4), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: message-read
            ReceivedMessageUtils.read(account.get(12), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social-read
            ReceivedMessageUtils.read(account.get(14), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // OK: ROOT
            apvRes1 = ReceivedMessageUtils.read(account.get(10), TEST_CELL1, uuid, HttpStatus.SC_NO_CONTENT);
            // OK: message
            apvRes2 = ReceivedMessageUtils.read(account.get(2), TEST_CELL1, uuid, HttpStatus.SC_NO_CONTENT);
            // OK: message+social
            apvRes3 = ReceivedMessageUtils.read(account.get(18), TEST_CELL1, uuid, HttpStatus.SC_NO_CONTENT);

            // 受信メッセージ(req.relation.build)
            body = getReceivedMessageBody("request", "relation.add", "12345678901234567890123456789013");
            rcvRes2 = ReceivedMessageUtils.receive(null, TEST_CELL1, body, HttpStatus.SC_CREATED);
            results = (JSONObject) ((JSONObject) rcvRes2.bodyAsJson().get("d")).get("results");
            uuid = (String) results.get("__id");
            // Create Relation
            RelationUtils.create(TEST_CELL1, "user", null, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // NG: アクセス権無し
            ReceivedMessageUtils.approve(account.get(0), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: message
            ReceivedMessageUtils.approve(account.get(2), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social
            ReceivedMessageUtils.approve(account.get(4), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: message-read
            ReceivedMessageUtils.approve(account.get(12), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social-read
            ReceivedMessageUtils.approve(account.get(14), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);

            // OK: ROOT
            apvRes4 = ReceivedMessageUtils.approve(account.get(10), TEST_CELL1, uuid, HttpStatus.SC_NO_CONTENT);
            // Relation-ExtCell $links削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(UrlUtils.cellRoot("targetcell")),
                    Relation.EDM_TYPE_NAME, "user", null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // ExtCell削除
            ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, UrlUtils.cellRoot("targetcell"));
            if (rcvRes2 != null) {
                ODataCommon.deleteOdataResource(rcvRes2.getLocationHeader());
                rcvRes2 = ReceivedMessageUtils.receive(null, TEST_CELL1, body, HttpStatus.SC_CREATED);
                results = (JSONObject) ((JSONObject) rcvRes2.bodyAsJson().get("d")).get("results");
                uuid = (String) results.get("__id");
            }

            // OK: message+social
            apvRes5 = ReceivedMessageUtils.approve(account.get(18), TEST_CELL1, uuid, HttpStatus.SC_NO_CONTENT);
            // Relation-ExtCell $links削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(UrlUtils.cellRoot("targetcell")),
                    Relation.EDM_TYPE_NAME, "user", null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Relation削除
            RelationUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "user", null, -1);
            // ExtCell削除
            ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    UrlUtils.cellRoot("targetcell"));

            // 受信メッセージ(req.relation.break)
            body = getReceivedMessageBody("request", "relation.remove", "12345678901234567890123456789014");
            rcvRes3 = ReceivedMessageUtils.receive(null, TEST_CELL1, body, HttpStatus.SC_CREATED);
            results = (JSONObject) ((JSONObject) rcvRes3.bodyAsJson().get("d")).get("results");
            uuid = (String) results.get("__id");
            // NG: アクセス権無し
            ReceivedMessageUtils.reject(account.get(0), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: message
            // ResourceUtils.postRejectedRelation(account.get(2), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social
            ReceivedMessageUtils.reject(account.get(4), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: message-read
            ReceivedMessageUtils.reject(account.get(12), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // NG: social-read
            ReceivedMessageUtils.reject(account.get(14), TEST_CELL1, uuid, HttpStatus.SC_FORBIDDEN);
            // OK: ROOT
            apvRes6 = ReceivedMessageUtils.reject(account.get(10), TEST_CELL1, uuid,
                    HttpStatus.SC_NO_CONTENT);
            if (rcvRes3 != null) {
                ODataCommon.deleteOdataResource(rcvRes3.getLocationHeader());
                rcvRes3 = ReceivedMessageUtils.receive(null, TEST_CELL1, body, HttpStatus.SC_CREATED);
                results = (JSONObject) ((JSONObject) rcvRes3.bodyAsJson().get("d")).get("results");
                uuid = (String) results.get("__id");
            }
            // OK: message+social
            apvRes7 = ReceivedMessageUtils.reject(account.get(18), TEST_CELL1, uuid,
                    HttpStatus.SC_NO_CONTENT);

        } finally {
            // 作成したメッセージの削除
            if (rcvRes1 != null) {
                ODataCommon.deleteOdataResource(rcvRes1.getLocationHeader());
            }
            if (rcvRes2 != null) {
                ODataCommon.deleteOdataResource(rcvRes2.getLocationHeader());
            }
            if (rcvRes3 != null) {
                ODataCommon.deleteOdataResource(rcvRes3.getLocationHeader());
            }
            if (apvRes1 != null) {
                ODataCommon.deleteOdataResource(apvRes1.getLocationHeader());
            }
            if (apvRes2 != null) {
                ODataCommon.deleteOdataResource(apvRes2.getLocationHeader());
            }
            if (apvRes3 != null) {
                ODataCommon.deleteOdataResource(apvRes3.getLocationHeader());
            }
            if (apvRes4 != null) {
                ODataCommon.deleteOdataResource(apvRes4.getLocationHeader());
            }
            if (apvRes5 != null) {
                ODataCommon.deleteOdataResource(apvRes5.getLocationHeader());
            }
            if (apvRes6 != null) {
                ODataCommon.deleteOdataResource(apvRes6.getLocationHeader());
            }
            if (apvRes7 != null) {
                ODataCommon.deleteOdataResource(apvRes7.getLocationHeader());
            }
            // Relation-ExtCell $links削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(UrlUtils.cellRoot("targetcell")),
                    Relation.EDM_TYPE_NAME, "user", null, AbstractCase.MASTER_TOKEN_NAME, -1);
            // Relation削除
            RelationUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "user", null, -1);
            // ExtCell削除
            ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1,
                    UrlUtils.cellRoot("targetcell"));
        }
    }

    /**
     * メッセージ受信用Bodyの取得.
     * @param type メッセージ種別
     * @param requestType リクエストタイプ
     * @param id 受信メッセージのID
     * @return メッセージボディ
     */
    @SuppressWarnings("unchecked")
    protected String getReceivedMessageBody(String type, String requestType, String id) {
        JSONObject body = new JSONObject();
        body.put("__id", id);
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", type);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);

        if ("message".equals(type)) {
            body.put("Status", "unread");
        } else if ("request".equals(type)) {
            body.put("Status", "none");

            JSONObject requestObject = new JSONObject();
            requestObject.put("RequestType", requestType);
            requestObject.put("Name", "user");
            requestObject.put("TargetUrl", UrlUtils.cellRoot("targetcell"));
            JSONArray requestObjects = new JSONArray();
            requestObjects.add(requestObject);
            body.put("RequestObjects", requestObjects);
        } else {
            body.put("Status", "unread");
        }

        return body.toJSONString();
    }

    private void eventAclTest(List<String> account) {
        // PROPPACTH Event
        CellUtils.proppatchEvent(TEST_CELL1, account.get(0), HttpStatus.SC_NOT_IMPLEMENTED, "hoge", "huga");
        CellUtils.proppatchEvent(TEST_CELL1, account.get(1), HttpStatus.SC_NOT_IMPLEMENTED, "hoge", "huga");
        CellUtils.proppatchEvent(TEST_CELL1, account.get(3), HttpStatus.SC_NOT_IMPLEMENTED, "hoge", "huga");
        CellUtils.proppatchEvent(TEST_CELL1, account.get(10), HttpStatus.SC_NOT_IMPLEMENTED, "hoge", "huga");
        CellUtils.proppatchEvent(TEST_CELL1, account.get(13), HttpStatus.SC_NOT_IMPLEMENTED, "hoge", "huga");

        // POST Event
        String jsonBody = "{\"Type\": \"TYPE\", \"Object\": \"OBJECT\", \"Info\": \"INFO\"}";
        CellUtils.event(account.get(0), HttpStatus.SC_FORBIDDEN, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(1), HttpStatus.SC_FORBIDDEN, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(3), HttpStatus.SC_OK, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(10), HttpStatus.SC_OK, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(13), HttpStatus.SC_FORBIDDEN, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(19), HttpStatus.SC_FORBIDDEN, TEST_CELL1, jsonBody);
        CellUtils.event(account.get(20), HttpStatus.SC_FORBIDDEN, TEST_CELL1, jsonBody);

    }

    private void propfindAclTest(List<String> account) {
        // PROPFIND ACL付き
        CellUtils.propfind(TEST_CELL1, account.get(0), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(TEST_CELL1, account.get(1), "0", HttpStatus.SC_FORBIDDEN);
        TResponse tresponse = CellUtils.propfind(TEST_CELL1, account.get(7), "0",
                HttpStatus.SC_MULTI_STATUS);
        // ACLタグが含まれていない事
        NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "acl");
        assertTrue(tresponse.getBody(), list.getLength() == 0);

        TResponse tresponse2 = CellUtils.propfind(TEST_CELL1, account.get(8), "0",
                HttpStatus.SC_MULTI_STATUS);
        // ACLタグがついてること
        NodeList list2 = tresponse2.bodyAsXml().getElementsByTagNameNS("DAV:", "acl");
        assertTrue(tresponse2.getBody(), list2.getLength() > 0);

        TResponse tresponse3 = CellUtils.propfind(TEST_CELL1, account.get(10), "0",
                HttpStatus.SC_MULTI_STATUS);
        // ACLタグがついてること
        NodeList list3 = tresponse3.bodyAsXml().getElementsByTagNameNS("DAV:", "acl");
        assertTrue(tresponse3.getBody(), list3.getLength() > 0);
    }

    private void extCellAclTest(List<String> account) {

        String testCell1 = "extcellacltest1";
        String testCell2 = "extcellacltest2";
        String testCell3 = "extcellacltest3";

        String extCellUrl = UrlUtils.cellRoot(testCell1);
        String extCellUrl2 = UrlUtils.cellRoot(testCell2);
        String extCellUrl3 = UrlUtils.cellRoot(testCell3);
        try {
            // テスト用セルの作成
            CellUtils.create(testCell1, TOKEN, HttpStatus.SC_CREATED);
            CellUtils.create(testCell2, TOKEN, HttpStatus.SC_CREATED);
            CellUtils.create(testCell3, TOKEN, HttpStatus.SC_CREATED);

            // extCell作成 POST socialが必要
            ExtCellUtils.create(account.get(0), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.create(account.get(1), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.create(account.get(4), TEST_CELL1, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.create(account.get(10), TEST_CELL1, extCellUrl2, HttpStatus.SC_CREATED);
            ExtCellUtils.create(account.get(14), TEST_CELL1, extCellUrl2, HttpStatus.SC_FORBIDDEN);
            // extCell取得 social-readが必要
            ExtCellUtils.get(account.get(0), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.get(account.get(1), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.get(account.get(4), TEST_CELL1, extCellUrl, HttpStatus.SC_OK);
            ExtCellUtils.get(account.get(10), TEST_CELL1, extCellUrl, HttpStatus.SC_OK);
            ExtCellUtils.get(account.get(14), TEST_CELL1, extCellUrl, HttpStatus.SC_OK);
            // extCell更新 socialが必要
            ExtCellUtils.update(account.get(0), TEST_CELL1, extCellUrl, extCellUrl3,
                    HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.update(account.get(1), TEST_CELL1, extCellUrl, extCellUrl3,
                    HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.update(account.get(4), TEST_CELL1, extCellUrl, extCellUrl3,
                    HttpStatus.SC_NO_CONTENT);
            ExtCellUtils.update(account.get(10), TEST_CELL1, extCellUrl3, extCellUrl,
                    HttpStatus.SC_NO_CONTENT);
            ExtCellUtils.update(account.get(14), TEST_CELL1, extCellUrl, extCellUrl3,
                    HttpStatus.SC_FORBIDDEN);
            // extCell削除 DELETE socialが必要
            ExtCellUtils.delete(account.get(0), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.delete(account.get(1), TEST_CELL1, extCellUrl, HttpStatus.SC_FORBIDDEN);
            ExtCellUtils.delete(account.get(4), TEST_CELL1, extCellUrl, HttpStatus.SC_NO_CONTENT);
            ExtCellUtils.delete(account.get(10), TEST_CELL1, extCellUrl2, HttpStatus.SC_NO_CONTENT);
            ExtCellUtils.delete(account.get(14), TEST_CELL1, extCellUrl2, HttpStatus.SC_FORBIDDEN);
        } finally {
            // テスト用セルの削除
            ExtCellUtils.delete(TOKEN, TEST_CELL1, extCellUrl, -1);
            ExtCellUtils.delete(TOKEN, TEST_CELL1, extCellUrl2, -1);
            CellUtils.delete(TOKEN, testCell1, -1);
            CellUtils.delete(TOKEN, testCell2, -1);
            CellUtils.delete(TOKEN, testCell3, -1);
        }
    }

    private void boxAclTest(List<String> account) {
        String boxName = "testhoge";
        String boxName2 = "testhoge2";
        String boxName3 = "testhoge3";
        // box作成 POST Privilege boxが必要
        BoxUtils.create(TEST_CELL1, boxName, account.get(0), HttpStatus.SC_FORBIDDEN);
        BoxUtils.create(TEST_CELL1, boxName, account.get(1), HttpStatus.SC_FORBIDDEN);
        BoxUtils.create(TEST_CELL1, boxName, account.get(5), HttpStatus.SC_CREATED);
        BoxUtils.create(TEST_CELL1, boxName2, account.get(10), HttpStatus.SC_CREATED);
        BoxUtils.create(TEST_CELL1, boxName, account.get(15), HttpStatus.SC_FORBIDDEN);
        // box取得 GET Privilege box-readが必要
        BoxUtils.get(TEST_CELL1, account.get(0), boxName, HttpStatus.SC_FORBIDDEN);
        BoxUtils.get(TEST_CELL1, account.get(1), boxName, HttpStatus.SC_FORBIDDEN);
        BoxUtils.get(TEST_CELL1, account.get(5), boxName, HttpStatus.SC_OK);
        BoxUtils.get(TEST_CELL1, account.get(10), boxName2, HttpStatus.SC_OK);
        BoxUtils.get(TEST_CELL1, account.get(15), boxName, HttpStatus.SC_OK);
        // box更新 PUT Privilege boxが必要
        BoxUtils.update(TEST_CELL1, account.get(0), boxName, "*", boxName3, UrlUtils.cellRoot(TEST_CELL1),
                HttpStatus.SC_FORBIDDEN);
        BoxUtils.update(TEST_CELL1, account.get(1), boxName, "*", boxName3, UrlUtils.cellRoot(TEST_CELL1),
                HttpStatus.SC_FORBIDDEN);
        BoxUtils.update(TEST_CELL1, account.get(5), boxName, "*", boxName3, UrlUtils.cellRoot(TEST_CELL1),
                HttpStatus.SC_NO_CONTENT);
        BoxUtils.update(TEST_CELL1, account.get(10), boxName3, "*", boxName, UrlUtils.cellRoot(TEST_CELL1),
                HttpStatus.SC_NO_CONTENT);
        BoxUtils.update(TEST_CELL1, account.get(15), boxName, "*", boxName3, UrlUtils.cellRoot(TEST_CELL1),
                HttpStatus.SC_FORBIDDEN);
        // box削除 DELETE Privilege boxが必要
        BoxUtils.delete(TEST_CELL1, account.get(0), boxName, HttpStatus.SC_FORBIDDEN);
        BoxUtils.delete(TEST_CELL1, account.get(1), boxName, HttpStatus.SC_FORBIDDEN);
        BoxUtils.delete(TEST_CELL1, account.get(5), boxName, HttpStatus.SC_NO_CONTENT);
        BoxUtils.delete(TEST_CELL1, account.get(10), boxName2, HttpStatus.SC_NO_CONTENT);
        BoxUtils.delete(TEST_CELL1, account.get(15), boxName, HttpStatus.SC_FORBIDDEN);
    }

    private void barInstallAclTest(List<String> account) {
        String targetBoxName1 = "boxImportAcl1";
        String targetBoxName2 = "boxImportAcl2";
        String targetBoxName3 = "boxImportAcl3";
        String targetBoxName4 = "boxImportAcl4";

        TResponse res = null;

        // barファイルインストール
        try {
            // アクセス権無し: 権限エラー
            res = ResourceUtils.barInstall(account.get(0), TEST_CELL1, targetBoxName1,
                    HttpStatus.SC_FORBIDDEN);
        } finally {
            deleteBox(targetBoxName1, res.getHeader(HttpHeaders.LOCATION));
        }

        try {
            // BOX: 成功
            res = ResourceUtils.barInstall(account.get(5), TEST_CELL1, targetBoxName1, HttpStatus.SC_ACCEPTED);
        } finally {
            deleteBox(targetBoxName1, res.getHeader(HttpHeaders.LOCATION));
        }

        try {
            // ROOT: 成功
            res = ResourceUtils.barInstall(account.get(10), TEST_CELL1, targetBoxName2, HttpStatus.SC_ACCEPTED);
        } finally {
            deleteBox(targetBoxName2, res.getHeader(HttpHeaders.LOCATION));
        }

        try {
            // bar-install: 成功
            res = ResourceUtils.barInstall(account.get(17), TEST_CELL1, targetBoxName3, HttpStatus.SC_ACCEPTED);
        } finally {
            deleteBox(targetBoxName3, res.getHeader(HttpHeaders.LOCATION));
        }

        try {
            // box-read: 失敗
            res = ResourceUtils.barInstall(account.get(15), TEST_CELL1, targetBoxName4, HttpStatus.SC_FORBIDDEN);
        } finally {
            deleteBox(targetBoxName4, res.getHeader(HttpHeaders.LOCATION));
        }
    }

    private void logAclTest(List<String> account) {
        String logFile = "/" + TEST_CELL1 + "/__log/current/default.log";

        // log GET log-read
        ResourceUtils.requestUtil("GET", account.get(0), logFile, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("GET", account.get(1), logFile, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("GET", account.get(3), logFile, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("GET", account.get(10), logFile, HttpStatus.SC_OK);
        ResourceUtils.requestUtil("GET", account.get(13), logFile, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("GET", account.get(19), logFile, HttpStatus.SC_OK);
        ResourceUtils.requestUtil("GET", account.get(20), logFile, HttpStatus.SC_OK);
    }

    private void logListAclTest(List<String> account) {
        // log GET log-read
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(0), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(1), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(3), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(10), HttpStatus.SC_MULTI_STATUS);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(13), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(19), HttpStatus.SC_MULTI_STATUS);
        ResourceUtils.logCollectionPropfind(TEST_CELL1, "archive", "0", account.get(20), HttpStatus.SC_MULTI_STATUS);
    }

    private void signTest(List<String> account) {
        // sign POST sign
        // UNIT_MASTER_TOKEN
        SignUtils.post(TOKEN, "example", HttpStatus.SC_OK, TEST_CELL1).contentType("application/jose");
        // sign privilege
        SignUtils.post(account.get(21), "example", HttpStatus.SC_OK, TEST_CELL1).contentType("application/jose");
        // other
        SignUtils.post(account.get(0), "example", HttpStatus.SC_FORBIDDEN, TEST_CELL1).contentType("application/json");
    }

    /**
     * CellレベルACL設定アクセス制御$link確認.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void CellレベルACL設定アクセス制御NavigationProperty確認() {
        String relationName = "testRelation";
        List<String> account = new ArrayList<String>();
        String extCellUrl = UrlUtils.cellRoot("cellhoge");
        try {
            // account
            account = accountAuth();
            String roleName = "roleHuga";
            String post = "POST";
            // accountとroleの$link→AUTH権限が必要
            JSONObject roleBody = new JSONObject();
            roleBody.put("Name", roleName);
            roleBody.put("_Box.Name", null);

            CellUtils.createNp(post, TEST_CELL1, Account.EDM_TYPE_NAME, "account11", "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(0), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Account.EDM_TYPE_NAME, "account11", "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(2), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Account.EDM_TYPE_NAME, "account11", "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(1), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account11", null,
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);
            // 作成したRole削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);

            CellUtils.createNp(post, TEST_CELL1, Account.EDM_TYPE_NAME, "account11", "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(10), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinks(TEST_CELL1, Account.EDM_TYPE_NAME, "account11", null,
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);
            // 作成したRole削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);

            // Relationとroleの$link→SOCIALとAUTH権限が必要
            // Relationの作成
            JSONObject relationBody = new JSONObject();
            relationBody.put("Name", relationName);
            relationBody.put("_Box.Name", null);
            RelationUtils.create(TEST_CELL1, TOKEN, relationBody, HttpStatus.SC_CREATED);

            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(0), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(1), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(4), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(9), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);
            // 作成したRole削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);

            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(10), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinks(TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);
            // 作成したRole削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);

            // RelationとextCellの$link→SOCIAL権限が必要
            JSONObject extCellBody = new JSONObject();
            extCellBody.put("Url", extCellUrl);

            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + ExtCell.EDM_TYPE_NAME,
                    extCellBody, account.get(0), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + ExtCell.EDM_TYPE_NAME,
                    extCellBody, account.get(1), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + ExtCell.EDM_TYPE_NAME,
                    extCellBody, account.get(4), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(extCellUrl),
                    Relation.EDM_TYPE_NAME, relationName, null, account.get(10), HttpStatus.SC_NO_CONTENT);
            // 作成したExtCell削除
            ExtCellUtils.delete(TOKEN, TEST_CELL1, extCellUrl,
                    HttpStatus.SC_NO_CONTENT);
            CellUtils.createNp(post, TEST_CELL1, Relation.EDM_TYPE_NAME, relationName,
                    "_" + ExtCell.EDM_TYPE_NAME,
                    extCellBody, account.get(10), HttpStatus.SC_CREATED);

            // extCellとrole→SOCIALとAUTHの権限が必要
            CellUtils.createNp(post, TEST_CELL1, ExtCell.EDM_TYPE_NAME,
                    CommonUtils.encodeUrlComp(extCellUrl),
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(0), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, ExtCell.EDM_TYPE_NAME,
                    CommonUtils.encodeUrlComp(extCellUrl),
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(1), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, ExtCell.EDM_TYPE_NAME,
                    CommonUtils.encodeUrlComp(extCellUrl),
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(4), HttpStatus.SC_FORBIDDEN);
            CellUtils.createNp(post, TEST_CELL1, ExtCell.EDM_TYPE_NAME,
                    CommonUtils.encodeUrlComp(extCellUrl),
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(9), HttpStatus.SC_CREATED);
            // 作成した$linkの削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(extCellUrl),
                    Role.EDM_TYPE_NAME, roleName, null, TOKEN, HttpStatus.SC_NO_CONTENT);
            // Role削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);
            CellUtils.createNp(post, TEST_CELL1, ExtCell.EDM_TYPE_NAME,
                    CommonUtils.encodeUrlComp(extCellUrl),
                    "_" + Role.EDM_TYPE_NAME,
                    roleBody, account.get(10), HttpStatus.SC_CREATED);
            // Role削除
            RoleUtils.delete(TEST_CELL1, TOKEN, roleName, null);

        } finally {
            // 作成した$linkの削除
            LinksUtils.deleteLinksExtCell(TEST_CELL1, CommonUtils.encodeUrlComp(extCellUrl),
                    Relation.EDM_TYPE_NAME, relationName, null, TOKEN, -1);
            // ExtCell 削除
            ExtCellUtils.delete(TOKEN, TEST_CELL1, extCellUrl,
                    HttpStatus.SC_NO_CONTENT);
            // Realationの削除
            RelationUtils.delete(TEST_CELL1, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);
        }
    }




    private void deleteBox(String boxName, String location) {

        if (location == null) {
            return;
        }

        BarInstallTestUtils.waitBoxInstallCompleted(location);

        String reqCell = Setup.TEST_CELL1;
        Http.request("cell/box-delete.txt")
                .with("cellPath", reqCell)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("boxPath", boxName)
                .returns()
                .debug();
    }
}

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
package com.fujitsu.dc.test.jersey.box.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.TestMethodUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * BOXレベルACLのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AclTest extends JerseyTest {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    static final String TEST_CELL1 = "testcell1";
    static final String TEST_ROLE1 = "role4";
    static final String TEST_ROLE2 = "role5";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String BOX_NAME = "box1";
    static final String DEPTH = "0";
    static final String ACL_ALL_TEST = "box/acl-setting-all.txt";
    static final String ACL_SETTING_TEST = "box/acl-setting.txt";
    static final String ACL_NULL_TEST = "box/acl-null.txt";

    /**
     * コンストラクタ.
     */
    public AclTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * BoxレベルACL設定Principalのallの確認テスト.
     */
    @Test
    public final void ACL設定Principalのallの確認テスト() {

        try {
            // Principal:all
            // Privilege:readのACLをbox1に設定
            DavResourceUtils.setACL(null, TOKEN, HttpStatus.SC_OK, TEST_CELL1 + "/" + BOX_NAME, ACL_ALL_TEST,
                    null, "<D:read/>", "");

            // PROPFINDでACLの確認
            TResponse tresponse = CellUtils.propfind(TEST_CELL1 + "/" + BOX_NAME,
                    TOKEN, DEPTH, HttpStatus.SC_MULTI_STATUS);
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("all");
            rolList.add("read");
            list.add(map);
            Element root = tresponse.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, BOX_NAME);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, BOX_NAME, ""), null);

            // account1でbox1を操作
            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Box1に対してGET（可能）
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_OK, Setup.TEST_BOX1, TEST_CELL1);
            // トークン空でもbox1に対してGET（可能）
            ResourceUtils.accessResource("", "", HttpStatus.SC_OK, Setup.TEST_BOX1, TEST_CELL1);
            // AuthorizationHedderが無しでもbox1に対してGET（可能）
            ResourceUtils.accessResourceNoAuth("", HttpStatus.SC_OK, TEST_CELL1);

            // Box1に対してPUT（不可：権限エラー）
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_FORBIDDEN);
            // トークン空でもbox1に対してPUT（不可：認証エラー）
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, "", "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_UNAUTHORIZED);
            // AuthorizationHedderが無しでもbox1に対してPUT（不可：認証エラー）
            DavResourceUtils.createWebDavFileNoAuthHeader(Setup.TEST_CELL1, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_UNAUTHORIZED);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    "text.txt", -1, Setup.TEST_BOX1);

            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * BoxレベルACL設定Principalのallとroleの同時設定確認テスト.
     */
    @Test
    public final void BoxレベルACL設定Principalのallとroleの同時設定確認テスト() {
        try {
            // Principal:all Privilege:read
            // Principal:role1 Privilege:write
            // のACLをbox1に設定
            setAclAllandRole(TEST_CELL1, TOKEN, HttpStatus.SC_OK, TEST_CELL1 + "/" + BOX_NAME,
                    "box/acl-setting-all-role.txt", "role1", "<D:read/>", "<D:write/>", "");

            // PROPFINDでACLの確認
            CellUtils.propfind(TEST_CELL1 + "/" + BOX_NAME,
                    TOKEN, DEPTH, HttpStatus.SC_MULTI_STATUS);

            // account1でbox1を操作
            // 認証
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(TEST_CELL1, "account1", "password1", -1);
            // トークン取得
            String tokenStr = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);

            // Box1に対してGET（可能）
            ResourceUtils.accessResource("", tokenStr, HttpStatus.SC_OK, Setup.TEST_BOX1, TEST_CELL1);
            // トークン空でもbox1に対してGET（可能）
            ResourceUtils.accessResource("", "", HttpStatus.SC_OK, Setup.TEST_BOX1, TEST_CELL1);
            // AuthorizationHedderが無しでもbox1に対してGET（可能）
            ResourceUtils.accessResourceNoAuth("", HttpStatus.SC_OK, TEST_CELL1);

            // Box1に対してPUT（可能）
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, tokenStr, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_CREATED);
            // トークン空でもbox1に対してPUT（不可：認証エラー）
            DavResourceUtils.createWebDavFile(Setup.TEST_CELL1, "", "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_UNAUTHORIZED);
            // AuthorizationHedderが無しでもbox1に対してPUT（不可：認証エラー）
            DavResourceUtils.createWebDavFileNoAuthHeader(Setup.TEST_CELL1, "box/dav-put.txt", "hoge", Setup.TEST_BOX1,
                    "text.txt", HttpStatus.SC_UNAUTHORIZED);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", Setup.TEST_CELL1, TOKEN,
                    "text.txt", -1, Setup.TEST_BOX1);

            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * BoxレベルACL設定_空のACLを送るとACLの設定が削除できることの確認.
     */
    @Test
    public final void BoxレベルACL設定_空のACLを送るとACLの設定が削除できることの確認() {
        try {

            // ・まずは設定ができることを確認
            // Principal:all
            // Privilege:readのACLをbox1に設定
            DavResourceUtils.setACL(null, TOKEN, HttpStatus.SC_OK, TEST_CELL1 + "/" + BOX_NAME, ACL_ALL_TEST,
                    null, "<D:read/>", "");

            // PROPFINDでACLの確認
            TResponse tresponse = CellUtils.propfind(TEST_CELL1 + "/" + BOX_NAME,
                    TOKEN, DEPTH, HttpStatus.SC_MULTI_STATUS);

            // ACEタグがついてることを正規表現で確認
            NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "ace");
            assertTrue(tresponse.getBody(), list.getLength() > 0);

            // ・空のACLを設定してACLが消えることを確認
            DavResourceUtils.setACL(TEST_CELL1, TOKEN, HttpStatus.SC_OK, "", ACL_NULL_TEST,
                    null, null, "");

            // PROPFINDでACLの確認
            TResponse tresponse2 = CellUtils.propfind(TEST_CELL1 + "/" + BOX_NAME,
                    TOKEN, DEPTH, HttpStatus.SC_MULTI_STATUS);
            // ACEタグが消えていること
            NodeList list2 = tresponse2.bodyAsXml().getElementsByTagNameNS("DAV:", "ace");
            assertTrue(tresponse2.getBody(), list2.getLength() == 0);
        } finally {
            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * Boxに紐付くRoleでのACL設定テスト.
     */
    @Test
    public final void Boxに紐付くRoleでのACL設定テスト() {
        String testBox = "testBox01";
        String testRole = "testRole01";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付くRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox, testRole, HttpStatus.SC_CREATED);

            // 上記Boxに上記RoleでACL設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_OK, testBox, "",
                    ACL_SETTING_TEST, testRole, testBox, "<D:read/>", "");

            // PROPFIND
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop.txt",
                    TOKEN, HttpStatus.SC_MULTI_STATUS, testBox);

            // PROPFINDレスポンス確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            list.add(map);
            map.put(testRole, rolList);
            Element root = res.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, testBox);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, testBox, ""), null);

        } finally {
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox, testRole);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * 既にBoxに紐づかない同名のRoleが存在する状態でBoxに紐づかないRoleでのACL設定テスト.
     */
    @Test
    public final void 既にBoxに紐づかない同名のRoleが存在する状態でBoxに紐づかないRoleでのACL設定テスト() {
        String testBox = "testBox_27481";
        String testRole = "testRole_27481";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付くRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox, testRole, HttpStatus.SC_CREATED);
            // Boxに紐づかないRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, null, testRole, HttpStatus.SC_CREATED);

            // 上記BoxにBoxに紐付かないRoleでACL設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_OK, testBox, "", ACL_SETTING_TEST,
                    testRole, null, "<D:read/>", "");

            // PROPFIND
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop.txt",
                    TOKEN, HttpStatus.SC_MULTI_STATUS, testBox);

            // PROPFINDレスポンス確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            list.add(map);
            map.put("../__/" + testRole, rolList);
            Element root = res.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, testBox);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, testBox, ""), null);

        } finally {
            // Roleの削除(Boxに紐づく)
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox, testRole);
            // Roleの削除(Boxに紐づかない)
            RoleUtils.delete(TEST_CELL1, TOKEN, null, testRole);
            // Boxの削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * ACL設定対象のセルURLが異なる場合のテスト.
     */
    @Test
    public final void ACL設定対象のセルURLが異なる場合のテスト() {
        String testBox = "box1";
        String testRole = UrlUtils.roleResource("hogeCell", testBox, "role1");

        // Box1に上記RoleでACL設定
        DavResourceUtils.setACLwithRoleBaseUrl(TEST_CELL1, TOKEN, HttpStatus.SC_BAD_REQUEST, testBox, "",
                "box/acl-setting-baseurl.txt", testRole, "<D:read/>", "");

    }

    /**
     * baseUrlの相対パス利用確認.
     */
    @Test
    public final void baseUrlの相対パス利用確認() {
        String testBox1 = "testBox01";
        String testBox2 = "testBox02";
        String testRole02 = "testRole02";
        String testRole = "../testBox02/testRole02";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);
            BoxUtils.create(TEST_CELL1, testBox2, TOKEN);

            // Roleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox2, testRole02, HttpStatus.SC_CREATED);

            // 上記Boxに上記RoleでACL設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_OK, testBox1, "",
                    ACL_SETTING_TEST, testRole, testBox1, "<D:read/>", "");

            // PROPFIND
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop.txt",
                    TOKEN, HttpStatus.SC_MULTI_STATUS, testBox1);

            // PROPFINDレスポンス確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            list.add(map);
            map.put(UrlUtils.aclRelativePath("testBox02", testRole02), rolList);
            Element root = res.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, testBox1);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, testBox1, ""), null);

        } finally {

            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox2, testRole02);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox2);
        }
    }

    /**
     * BOXレベルACLでhrefにロールリソースURLをフルで設定した場合.
     */
    @Test
    public final void BOXレベルACLでhrefにロールリソースURLをフルで設定した場合() {
        String testBox1 = "testBox01";
        String testRole = "testRole02";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox1, TOKEN);

            // Roleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox1, testRole, HttpStatus.SC_CREATED);

            // 上記Boxに上記RoleでACL設定
            DavResourceUtils.setACLwithRoleBaseUrl(TEST_CELL1, TOKEN, HttpStatus.SC_OK, testBox1, "",
                    "box/acl-setting-baseurl.txt", UrlUtils.roleResource(TEST_CELL1, testBox1, testRole),
                    "<D:read/>", "");

            // PROPFIND
            TResponse res = DavResourceUtils.propfind("box/propfind-box-allprop.txt",
                    TOKEN, HttpStatus.SC_MULTI_STATUS, testBox1);

            // PROPFINDレスポンス確認
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("read");
            list.add(map);
            map.put(testRole, rolList);
            Element root = res.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, testBox1);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, testBox1, ""), null);

        } finally {

            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox1, testRole);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox1);
        }
    }

    /**
     * BOXレベルACLでrequireSchemaAuthを設定取得できることの確認.
     */
    @Test
    public final void BOXレベルACLでrequireSchemaAuthを設定取得できることの確認() {
        try {

            String requireSchamaAuthz = "public";
            // Principal:all
            // Privilege:readのACLをbox1に設定
            DavResourceUtils.setACL(null, TOKEN, HttpStatus.SC_OK, TEST_CELL1 + "/" + BOX_NAME, ACL_ALL_TEST,
                    null, "<D:read/>", requireSchamaAuthz);

            // PROPFINDでACLの確認
            TResponse tresponse = CellUtils.propfind(TEST_CELL1 + "/" + BOX_NAME,
                    TOKEN, DEPTH, HttpStatus.SC_MULTI_STATUS);
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("all");
            rolList.add("read");
            list.add(map);
            Element root = tresponse.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(TEST_CELL1, BOX_NAME);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 1,
                    UrlUtils.roleResource(TEST_CELL1, BOX_NAME, ""), requireSchamaAuthz);

        } finally {
            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * BoxレベルACL設定済みのロール削除時の確認.
     */
    @Test
    public final void BoxレベルACL設定済みのロール削除時の確認() {

        String box2 = "box2";
        String roleNotDelete = "role001";
        String roleDelete = "role002";
        try {
            // box2に紐付くロール作成
            RoleUtils.create(TEST_CELL1, TOKEN, box2, roleNotDelete, HttpStatus.SC_CREATED);
            RoleUtils.create(TEST_CELL1, TOKEN, box2, roleDelete, HttpStatus.SC_CREATED);

            // ACLをtestcell1/box2に設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_OK, box2, "",
                    "box/acl-2role-setting.txt", roleNotDelete, roleDelete, box2, "<D:read/>",
                    "<D:write/>", "");

            // roleを削除
            RoleUtils.delete(TEST_CELL1, TOKEN, box2, roleDelete, HttpStatus.SC_NO_CONTENT);

            // PROPFINDでtestcell1/box2のACLを取得
            TResponse tresponse = DavResourceUtils.propfind("box/propfind-box-allprop.txt", TOKEN,
                    HttpStatus.SC_MULTI_STATUS, box2);

            // role002が存在しない事を確認する=aceタグが１つのみ
            NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "ace");
            assertTrue(tresponse.getBody(), list.getLength() == 1);

            // role001が存在する事を確認する
            assertTrue(tresponse.getBody(), list.item(0).getTextContent().indexOf(roleNotDelete) > -1);

        } finally {
            // ロールの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, box2, roleNotDelete, -1);
            RoleUtils.delete(TEST_CELL1, TOKEN, box2, roleDelete, -1);

            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * BoxレベルACL設定済みの全ロール削除時の確認.
     */
    @Test
    public final void BoxレベルACL設定済みの全ロール削除時の確認() {

        String box2 = "box2";
        String roleDelete = "role002";
        try {
            // box2に紐付くロール作成
            RoleUtils.create(TEST_CELL1, TOKEN, null, roleDelete, HttpStatus.SC_CREATED);

            // ACLをtestcell1/box2に設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_OK, box2, "",
                    ACL_SETTING_TEST, roleDelete, null, "<D:read/>", "");

            // roleを削除
            RoleUtils.delete(TEST_CELL1, TOKEN, null, roleDelete, HttpStatus.SC_NO_CONTENT);

            // PROPFINDでtestcell1/box2のACLを取得
            TResponse tresponse = DavResourceUtils.propfind("box/propfind-box-allprop.txt", TOKEN,
                    HttpStatus.SC_MULTI_STATUS, box2);

            // role002が存在しない事を確認する=principalタグがない
            NodeList list = tresponse.bodyAsXml().getElementsByTagNameNS("DAV:", "principal");
            assertTrue(tresponse.getBody(), list.getLength() == 0);

        } finally {
            // ロールの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, null, roleDelete, -1);

            // ACLの設定を元に戻す
            Http.request("box/acl-authtest.txt")
                    .with("cellPath", TEST_CELL1)
                    .with("colname", "")
                    .with("roleBaseUrl", UrlUtils.roleResource(TEST_CELL1, null, ""))
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("level", "")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * 存在しないBoxを指定した場合404エラーが返却されること.
     */
    @Test
    public final void 存在しないBoxを指定した場合404エラーが返却されること() {
        // 存在しないBoxを指定でACL設定
        TResponse res = DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_NOT_FOUND, "noneExistBox", "",
                ACL_SETTING_TEST, "role", "noneExistBox", "<D:read/>", "");
        String boxUrl = UrlUtils.boxRoot(TEST_CELL1, "noneExistBox");
        DcCoreException expectedException = DcCoreException.Dav.BOX_NOT_FOUND.params(boxUrl);
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * roleBaseUrlに存在しないBoxを指定した場合400エラーが返却されること.
     */
    @Test
    public final void roleBaseUrlに存在しないBoxを指定した場合400エラーが返却されること() {
        String testBox = "testBox01";
        String testRole = "testRole01";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付くRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox, testRole, HttpStatus.SC_CREATED);

            // 存在しないBoxを指定でACL設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_BAD_REQUEST, testBox, "",
                    ACL_SETTING_TEST, testRole, "noneExistBox", "<D:read/>", "");

        } finally {
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox, testRole);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * 存在しないCellをxml:baseに指定してBoxレベルACL設定をした場合400エラーが返却されること.
     */
    @Test
    public final void 存在しないCellをxml_baseに指定してBoxレベルACL設定をした場合400エラーが返却されること() {
        String testBox = "testBox01";
        String testRole = "testRole01";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付くRoleの作成
            RoleUtils.create(TEST_CELL1, TOKEN, testBox, testRole, HttpStatus.SC_CREATED);

            // 存在しないCellをxml:baseに指定してACL設定
            DavResourceUtils.setACLwithRoleBaseUrl(TEST_CELL1, TOKEN, HttpStatus.SC_BAD_REQUEST, testBox, "",
                    testRole, ACL_SETTING_TEST, UrlUtils.roleResource("notExistsCell", "__", testRole),
                    "<D:read/>", "");

        } finally {
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, testBox, testRole);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
        }
    }

    /**
     * Roleに紐付いていないBox名を指定した場合400エラーが返却されること.
     */
    @Test
    public final void Roleに紐付いていないBox名を指定した場合400エラーが返却されること() {
        String testBox = "testBox01";
        String testRole = "testRole01";
        try {
            // Boxの作成
            BoxUtils.create(TEST_CELL1, testBox, TOKEN);

            // Boxに紐付かないRoleの作成
            createRole(TEST_CELL1, TOKEN, testRole, HttpStatus.SC_CREATED);

            // 上記Boxに上記RoleでACL設定
            DavResourceUtils.setACLwithBox(TEST_CELL1, TOKEN, HttpStatus.SC_BAD_REQUEST, testBox, "",
                    ACL_SETTING_TEST, testRole, testBox, "<D:read/>", "");
        } finally {
            // Roleの削除
            RoleUtils.delete(TEST_CELL1, TOKEN, null, testRole);

            // Box1の削除
            BoxUtils.delete(TEST_CELL1, TOKEN, testBox);
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
        TResponse res = Http.request("box/acl-setting-none-body.txt")
                .with("cell", TEST_CELL1)
                .with("box", Setup.TEST_BOX1)
                .with("colname", Setup.TEST_ODATA)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        res.checkErrorResponse(DcCoreException.Dav.XML_VALIDATE_ERROR.getCode(),
                DcCoreException.Dav.XML_VALIDATE_ERROR.getMessage());
    }

    /**
     * 認証で使用されたAccountに対象のRoleが存在しないかつ権限が設定されていないRoleが紐ついている場合にリクエストを実行した場合_403が返却されること.
     * テスト観点はチケット#34823「アクセス権限のチェックにて、対象のRoleが存在しない場合500エラーが発生」を参照
     */
    @Test
    public final void 認証で使用されたAccountに対象のRoleが存在しないかつ権限が設定されていないRoleが紐ついている場合にリクエストを実行した場合_403が返却されること() {
        String cellName = "cellAclTest";
        String boxName = "boxAclTest";
        String colName = "colAclTest";
        String account = "accountAclTest";
        String role1 = "roleAclTest1";
        String role2 = "roleAclTest2";
        try {
            // 前準備として、Box、CollectionにACLを設定されているRoleを全て削除して、内部的にACLの設定が「<acl><ace/></acl>」のようなaceが空の状態を設定する
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, -1);
            BoxUtils.create(cellName, boxName, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName);
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, cellName, account, "password", -1);
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, role1, -1);
            AccountUtils.createLinkWithRole(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, account, role1, -1);

            // BoxにACL設定
            Http.request("box/acl-setting-single.txt").with("cell", cellName)
                    .with("box", boxName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("role1", role1)
                    .with("roleBaseUrl", UrlUtils.roleResource(cellName, boxName, role1))
                    .returns()
                    .statusCode(-1);

            // CollectionにACL設定
            Http.request("box/acl-setting-single.txt").with("cell", cellName)
                    .with("box", boxName + "/" + colName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("role1", role1)
                    .with("roleBaseUrl", UrlUtils.roleResource(cellName, boxName, role1))
                    .returns()
                    .statusCode(-1);

            // 削除するRoleと紐付いているアカウントの認証トークン取得
            JSONObject json = ResourceUtils.getLocalTokenByPassAuth(cellName, account, "password", -1);
            String accessToken = json.get("access_token").toString();

            // ACL設定がされたRoleの削除
            AccountUtils.deleteLinksWithRole(cellName, boxName, AbstractCase.MASTER_TOKEN_NAME, account, role1, -1);
            RoleUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, role1);

            // アクセスするアカウントはRoleと結びついていないとaceのチェック前で権限エラーとなるため、ACL設定がされていないRoleの作成
            RoleUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, boxName, role2, -1);
            AccountUtils.createLinkWithRole(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, account, role2, -1);

            // ここから実際のテスト
            // Boxレベルに認証トークンを使用してリクエストを実行(403が返却されること)
            DavResourceUtils.createODataCollection(accessToken, HttpStatus.SC_FORBIDDEN, cellName, boxName, "dummycol");
            // Collectionレベルに認証トークンを使用してリクエストを実行(403が返却されること)
            EntityTypeUtils.create(cellName, accessToken, boxName, colName, "dummyEntityType", HttpStatus.SC_FORBIDDEN);
        } finally {
            CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, cellName);
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
     * Roleを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param roleName ロール名
     * @param code レスポンスコード
     */
    @SuppressWarnings("unchecked")
    public static void createRole(final String cellName, final String token,
            final String roleName, final int code) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);

        TResponse res = Http.request("role-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns();

        assertEquals(code, res.getStatusCode());

    }

}

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
package com.fujitsu.dc.test.jersey.cell.ctl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * BoxとRoleの$linksのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxRoleLinkTest extends JerseyTest {

    private static final String CELL_NAME = "testcell1";
    private static final String ENTITY_SET_BOX = "Box";
    private static final String ENTITY_SET_ROLE = "Role";
    private static final String KEY = "'box1'";
    private static final String NAV_PROP_BOX = "_Box";
    private static final String NAV_PROP_ROLE = "_Role";
    private static String roleUri;
    private static String roleKey;
    private static String roleChangedKey;
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BoxRoleLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * 各テストを実行する前の処理.
     */
    @Before
    public final void before() {
        if (roleUri == null) {
            TResponse response = Http.request("role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME).with("cellPath", CELL_NAME)
                    .returns().statusCode(HttpStatus.SC_OK);
            JSONObject d = (JSONObject) response.bodyAsJson().get("d");
            JSONArray results = (JSONArray) d.get("results");
            String name = (String) ((JSONObject) results.get(0)).get("Name");
            String boxName = (String) ((JSONObject) results.get(0)).get("_Box.Name");
            if (boxName == null) {
                roleKey = "Name='" + name + "',_Box.Name=null";
            } else {
                roleKey = "Name='" + name + "',_Box.Name='" + boxName + "'";
            }
            roleChangedKey = "_Box.Name=" + KEY + ",Name='" + name + "'";
            roleUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_ROLE, roleKey);
        }
    }

    /**
     * BoxとRoleのlinkを登録しresponseが204であること.
     */
    @Test
    public final void BoxとRoleのlinkを登録しresponseが204であること() {
        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", KEY)
                    .with("navProp", NAV_PROP_ROLE)
                    .with("uri", roleUri).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            deleteLink();
        }
    }

    /**
     * RoleとBoxのlinkを登録しresponseが204であること.
     */
    @Test
    public final void RoleとBoxのlinkを登録しresponseが204であること() {
        String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, KEY);
        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_ROLE)
                    .with("key", roleKey)
                    .with("navProp", NAV_PROP_BOX).with("uri", boxUri).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            deleteLink();
        }
    }

    /**
     * RoleとBoxのlink登録でRoleが既に他のBoxと紐付いている場合409となること.
     */
    @Test
    public final void RoleとBoxのlink登録でRoleが既に他のBoxと紐付いている場合409となること() {
        String cellName = Setup.TEST_CELL1;
        String baseBoxName = "RoleBoxLinkTestBaseBox";
        String boxName = "RoleBoxLinkTestBox";
        String roleName = "RoleBoxLinkTestRole";
        try {
            // Role作成
            RoleUtils.create(cellName, TOKEN, roleName, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(cellName, baseBoxName, TOKEN);
            BoxUtils.create(cellName, boxName, TOKEN);

            // Role-Box $links登録
            RoleUtils.createLink(cellName, TOKEN, RoleUtils.keyString(roleName), "Box", "'" + baseBoxName + "'",
                    HttpStatus.SC_NO_CONTENT);
            RoleUtils.createLink(cellName, TOKEN, RoleUtils.keyString(roleName, baseBoxName),
                    "Box", "'" + boxName + "'",
                    HttpStatus.SC_CONFLICT);
        } finally {
            // Role削除
            RoleUtils.delete(cellName, TOKEN, null, roleName, -1);
            RoleUtils.delete(cellName, TOKEN, baseBoxName, roleName, -1);

            // Box削除
            BoxUtils.delete(cellName, TOKEN, baseBoxName, -1);
            BoxUtils.delete(cellName, TOKEN, boxName, -1);

        }
    }

    /**
     * RoleとBoxのlink登録で既に同じリンクが存在する場合409となること.
     */
    @Test
    public final void RoleとBoxのlink登録で既に同じリンクが存在する場合409となること() {
        String cellName = Setup.TEST_CELL1;
        String boxName = "RoleBoxLinkTestBox";
        String roleName = "RoleBoxLinkTestRole";
        try {
            // Role作成
            RoleUtils.create(cellName, TOKEN, roleName, HttpStatus.SC_CREATED);

            // Box作成
            BoxUtils.create(cellName, boxName, TOKEN);

            // Role-Box $links登録
            RoleUtils.createLink(cellName, TOKEN, RoleUtils.keyString(roleName), "Box", "'" + boxName + "'",
                    HttpStatus.SC_NO_CONTENT);
            RoleUtils.createLink(cellName, TOKEN, RoleUtils.keyString(roleName, boxName),
                    "Box", "'" + boxName + "'",
                    HttpStatus.SC_CONFLICT);
        } finally {
            // Role削除
            RoleUtils.delete(cellName, TOKEN, boxName, roleName, -1);

            // Box削除
            BoxUtils.delete(cellName, TOKEN, boxName, -1);

        }
    }

    /**
     * 存在しないBoxを指定してRoleのlinkを登録した場合responseが404であること.
     */
    @Test
    public final void 存在しないBoxを指定してRoleのlinkを登録した場合responseが404であること() {
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'boxx'")
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", roleUri).returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 存在しないRoleを指定してBoxのlinkを登録した場合responseが400であること.
     */
    @Test
    public final void 存在しないRoleを指定してBoxのlinkを登録した場合responseが400であること() {
        String noRoleUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_ROLE, "Name='keyx',_Box.Name="
                + KEY);
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", noRoleUri).returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * 存在しないEntitySetにRoleのlinkを登録しresponseが404であること.
     */
    @Test
    public final void 存在しないEntitySetにRoleのlinkを登録しresponseが404であること() {
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", "test")
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", roleUri).returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Boxに存在しないNavigationPropetiesを指定してlinkを登録しresponseが400であること.
     */
    @Test
    public final void Boxに存在しないNavigationPropetiesを指定してlinkを登録しresponseが400であること() {
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", "_test")
                .with("uri", roleUri).returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * すでにlinksが登録されているBoxに再度Roleを登録した場合responseが409であること.
     */
    @Test
    public final void すでにlinksが登録されているBoxに再度Roleを登録した場合responseが409であること() {
        try {
            createLink();

            // 再度同一のリクエストを実行して409になることを確認
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", KEY)
                    .with("navProp", NAV_PROP_ROLE)
                    .with("uri", UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_ROLE, roleChangedKey))
                    .returns()
                    .statusCode(HttpStatus.SC_CONFLICT);
        } finally {
            deleteLink();
        }
    }

    /**
     * BoxとRoleのlink作成時uriの値に前丸カッコがない場合400になること.
     */
    @Test
    public final void BoxとRoleのlink作成時uriの値に前丸カッコがない場合400になること() {
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1)
                + "__ctl/RoleName='confidentialClient',_Box.Name=null)";
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", targetUri)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * BoxとRoleのlink作成時uriの値に後ろ丸カッコがない場合400になること.
     */
    @Test
    public final void BoxとRoleのlink作成時uriの値に後ろ丸カッコがない場合400になること() {
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1)
                + "__ctl/Role(Name='confidentialClient',_Box.Name=null";
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", targetUri)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * BoxとRoleのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること.
     */
    @Test
    public final void BoxとRoleのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること() {
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL_SCHEMA1)
                + "__ctl/Role(Name='confidentialClient',_Box.Name=null)";
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_SCHEMA1)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", "_Relation")
                .with("uri", targetUri)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * BoxとIdなしRoleのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void BoxとIdなしRoleのlinkを更新するとresponseが400であること() {
        String linkPath = "__ctl/" + ENTITY_SET_BOX + "\\('" + KEY + "'\\)/\\$links/" + NAV_PROP_ROLE;

        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", KEY)
                    .with("navProp", NAV_PROP_ROLE)
                    .with("uri", roleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // IdなしRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteLink();
        }
    }

    /**
     * BoxとIdありRoleのlinkを更新するとresponseが501であること.
     */
    @Test
    public final void BoxとIdありRoleのlinkを更新するとresponseが501であること() {
        String linkPath = "__ctl/" + ENTITY_SET_BOX + "\\(" + KEY + "\\)/\\$links/"
                + NAV_PROP_ROLE + "\\(" + roleKey + "\\)";

        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", KEY)
                    .with("navProp", NAV_PROP_ROLE)
                    .with("uri", roleUri)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // IdありRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } finally {
            deleteLink();
        }
    }

    /**
     * BoxとRoleのlinkを削除しresponseが204であること.
     */
    @Test
    public final void BoxとRoleのlinkを削除しresponseが204であること() {
        createLink();

        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", roleChangedKey)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * RoleとBoxのlinkを削除しresponseが204であること.
     */
    @Test
    public final void RoleとBoxのlinkを削除しresponseが204であること() {
        createLink();

        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_ROLE)
                .with("key", roleChangedKey)
                .with("navProp", NAV_PROP_BOX)
                .with("navKey", KEY)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 存在しないBoxとRoleのlinkを削除しresponseが404であること.
     */
    @Test
    public final void 存在しないBoxとRoleのlinkを削除しresponseが404であること() {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'boxx'")
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", roleKey)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * BoxとLinkを登録していないRoleのlinkを削除しresponseが404であること.
     */
    @Test
    public final void BoxとLinkを登録していないRoleのlinkを削除しresponseが404であること() {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'box2'")
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", roleKey)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Boxと存在しないRoleのlinkを削除しresponseが404であること.
     */
    @Test
    public final void Boxと存在しないRoleのlinkを削除しresponseが404であること() {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", "'rolex'")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * BoxとRoleのlink削除をRoleのNavPropKeyを指定しないで実行してresponseが400であること.
     */
    @Test
    public final void BoxとRoleのlink削除をRoleのNavPropKeyを指定しないで実行してresponseが400であること() {
        Http.request("links-request-no-navkey.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * BoxとRoleのlinkを取得しresponseが200であること.
     */
    @Test
    public final void BoxとRoleのlinkを取得しresponseが200であること() {
        String roleName = "role_BoxRoleLinkTest";
        String boxName = "box_BoxRoleLinkTest";

        TResponse resBox = null;
        TResponse resRole = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRole登録
            resRole = CellCtlUtils.createRole(CELL_NAME, roleName, boxName);

            // Boxに紐付くRoleのlink一覧取得
            TResponse res = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", "Name='" + boxName + "'")
                    .with("navProp", NAV_PROP_ROLE)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(resRole.getLocationHeader());
            ODataCommon.checkLinResponseBody(res.bodyAsJson(), uri);

        } finally {
            // Role削除
            CellCtlUtils.deleteOdataResource(resRole.getLocationHeader());

            // Box削除
            CellCtlUtils.deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * RoleとBoxのlinkを取得しresponseが200であること.
     */
    @Test
    public final void RoleとBoxのlinkを取得しresponseが200であること() {
        try {
            createLink();

            TResponse response = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_ROLE)
                    .with("key", roleChangedKey)
                    .with("navProp", NAV_PROP_BOX)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, KEY);
            checkResposeJson(response, boxUri);
        } finally {
            deleteLink();
        }
    }

    /**
     * Linksを登録していない状態でBoxとRoleのlinkを取得しresponseが200であること.
     */
    @Test
    public final void Linksを登録していない状態でBoxとRoleのlinkを取得しresponseが200であること() {
        Http.request("links-request-no-navkey.txt")
                .with("method", "GET")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * 存在しないBoxとRoleのlinkを取得しresponseが404であること.
     */
    @Test
    public final void 存在しないBoxとRoleのlinkを取得しresponseが404であること() {
        Http.request("links-request-no-navkey.txt")
                .with("method", "GET")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'boxx'")
                .with("navProp", NAV_PROP_ROLE)
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 存在しないNavPropを指定してlinkを取得しresponseが400であること.
     */
    @Test
    public final void 存在しないNavPropを指定してlinkを取得しresponseが400であること() {
        Http.request("links-request-no-navkey.txt")
                .with("method", "GET")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", "_test")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * RoleとIdなしBoxのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void RoleとIdなしBoxのlinkを更新するとresponseが400であること() {
        String linkPath = "__ctl/" + ENTITY_SET_ROLE + "\\('" + roleKey + "'\\)/\\$links/" + NAV_PROP_BOX;
        String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, KEY);

        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_ROLE)
                    .with("key", roleKey)
                    .with("navProp", NAV_PROP_BOX)
                    .with("uri", boxUri)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // IdなしRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            deleteLink();
        }
    }

    /**
     * RoleとIdありBoxのlinkを更新するとresponseが501であること.
     */
    @Test
    public final void RoleとIdありBoxのlinkを更新するとresponseが501であること() {
        String linkPath = "__ctl/" + ENTITY_SET_ROLE + "\\(" + roleKey + "\\)/\\$links/"
                + NAV_PROP_BOX + "\\(" + KEY + "\\)";
        String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, KEY);

        try {
            Http.request("links-request-with-body.txt")
                    .with("method", "POST")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_ROLE)
                    .with("key", roleKey)
                    .with("navProp", NAV_PROP_BOX)
                    .with("uri", boxUri)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // IdありRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } finally {
            deleteLink();
        }
    }

    /**
     * RoleとLinkされているBoxを削除するとresponseが409であること.
     */
    @Test
    public final void RoleとLinkされているBoxを削除するとresponseが409であること() {

        String boxName = "roleLinkBox";
        String roleName = "boxLinkRole";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRole作成
            RoleUtils.create(CELL_NAME, TOKEN, boxName, roleName, HttpStatus.SC_CREATED);

            // Boxの削除(結びつくロールがあるため、409)
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_CONFLICT);

            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, HttpStatus.SC_NO_CONTENT);

            // Boxの削除(結びつくロールが無くなったため、204)
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * RelationとLinkされているBoxを削除するとresponseが409であること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RelationとLinkされているBoxを削除するとresponseが409であること() {

        String boxName = "relationLinkBox";
        String relationName = "boxLinkrelation";
        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", boxName);
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRelation作成
            RelationUtils.create(CELL_NAME, TOKEN, body, HttpStatus.SC_CREATED);

            // Boxの削除(結びつくRelationがあるため、409)
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_CONFLICT);

            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, boxName, HttpStatus.SC_NO_CONTENT);

            // Boxの削除(結びつくRelationが無くなったため、204)
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, boxName, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * BoxとRoleのLink削除時に単一キーの同名Roleが存在すると409になること.
     */
    @Test
    public final void BoxとRoleのLink削除時に単一キーの同名Roleが存在すると409になること() {
        final String boxName = "roleLinkBox";
        final String roleName = "boxLinkRole";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRole作成
            RoleUtils.create(CELL_NAME, TOKEN, boxName, roleName, HttpStatus.SC_CREATED);

            // 上のBoxと結びつかないRole作成
            RoleUtils.create(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_CREATED);

            String roleKeyName = "_Box.Name='" + boxName + "',Name='" + roleName + "'";

            // BoxとRoleのLink削除（単一キーのRoleが存在するため削除できない(409))
            deleteBoxRoleLink(boxName, roleKeyName, HttpStatus.SC_CONFLICT);
            // RoleとBoxのLink削除（逆向きでも同様に409）
            deleteRoleBoxLink(roleKeyName, boxName, HttpStatus.SC_CONFLICT);

            // ロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_NO_CONTENT);

            // BoxとRoleのLink削除（単一キーのRoleが存在しないので削除できる)
            deleteBoxRoleLink(boxName, roleKeyName, HttpStatus.SC_NO_CONTENT);

            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_NO_CONTENT);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, -1);
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * BoxとRoleのLink追加時に複合キーの同名Roleが存在すると409になること.
     */
    @Test
    public final void BoxとRoleのLink追加時に複合キーの同名Roleが存在すると409になること() {
        final String boxName = "roleLinkBox";
        final String roleName = "boxLinkRole";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRole作成
            RoleUtils.create(CELL_NAME, TOKEN, boxName, roleName, HttpStatus.SC_CREATED);

            // 上のBoxと結びつかないRole作成
            RoleUtils.create(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_CREATED);

            String roleKeyName = "_Box.Name='" + boxName + "',Name='" + roleName + "'";

            // BoxとRoleのLink作成（複合キーのRoleが存在するため作成できない(409))
            createBoxRoleLink(boxName, roleName, HttpStatus.SC_CONFLICT);
            // RoleとBoxのLink作成（逆向きでも同様に409）
            createRoleBoxLink(roleName, boxName, HttpStatus.SC_CONFLICT);

            // ロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_NO_CONTENT);

            // BoxとRoleのLink削除
            deleteBoxRoleLink(boxName, roleKeyName, HttpStatus.SC_NO_CONTENT);

            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_NO_CONTENT);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, -1);
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * Boxに結びつかないRoleをexpandで展開できること.
     */
    @Test
    public final void Boxに結びつかないRoleをexpandで展開できること() {
        final String roleName = "boxLinkRole";
        try {
            // Role作成
            RoleUtils.create(CELL_NAME, TOKEN, null, roleName, HttpStatus.SC_CREATED);

            // Boxに紐付くRoleのlink一覧取得
            RoleUtils.list(TOKEN, CELL_NAME, "\\$expand=_Box&\\$filter=startswith(Name,'boxLinkRole')",
                    HttpStatus.SC_OK);
        } finally {
            // ロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, -1);
        }
    }

    /**
     * BoxとRoleをlinkしてexpandで展開できること.
     */
    @Test
    public final void BoxとRoleをlinkしてexpandで展開できること() {
        final String boxName = "roleLinkBox";
        final String roleName = "boxLinkRole";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // Collectionの作成
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, boxName, "test");

            // 上のBoxと結びつくRole作成
            RoleUtils.create(CELL_NAME, TOKEN, boxName, roleName, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRole作成
            RoleUtils.create(CELL_NAME, TOKEN, boxName, "boxLinkRole2", HttpStatus.SC_CREATED);

            // Roleに紐付くBoxのlink一覧取得
            RoleUtils.list(TOKEN, CELL_NAME, "\\$expand=_Box&\\$filter=startswith(Name,'boxLinkRole')",
                    HttpStatus.SC_OK);
        } finally {
            // 結びつくロールの削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, -1);
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, "boxLinkRole2", -1);

            // Collectionの削除
            DavResourceUtils.deleteCollection(CELL_NAME, boxName, "test", TOKEN, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    private void deleteBoxRoleLink(final String boxName, final String linkRoleKey, final int status) {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'" + boxName + "'")
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", linkRoleKey)
                .returns()
                .debug()
                .statusCode(status);
    }

    private void deleteRoleBoxLink(final String roleName, final String linkBoxKey, final int status) {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_ROLE)
                .with("key", roleName)
                .with("navProp", NAV_PROP_BOX)
                .with("navKey", "'" + linkBoxKey + "'")
                .returns()
                .debug()
                .statusCode(status);
    }

    private void createBoxRoleLink(final String boxName, final String roleName, final int expectedStatus) {
        String url = UrlUtils.cellCtl(CELL_NAME, ENTITY_SET_ROLE, roleName);
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", "'" + boxName + "'")
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", url)
                .returns()
                .debug()
                .statusCode(expectedStatus);
    }

    private void createRoleBoxLink(final String roleName, final String boxName, final int expectedStatus) {
        String url = UrlUtils.cellCtl(CELL_NAME, ENTITY_SET_BOX, boxName);
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_ROLE)
                .with("key", "'" + roleName + "'")
                .with("navProp", NAV_PROP_BOX)
                .with("uri", url)
                .returns()
                .debug()
                .statusCode(expectedStatus);
    }

    /**
     * Linkを登録する.
     */
    private void createLink() {
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("uri", roleUri).returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Linkを削除する.
     */
    private void deleteLink() {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", CELL_NAME)
                .with("entitySet", ENTITY_SET_BOX)
                .with("key", KEY)
                .with("navProp", NAV_PROP_ROLE)
                .with("navKey", roleChangedKey)
                .returns()
                .statusCode(-1);
    }

    // /**
    // * レスポンスボディのXMLをチェックする.
    // * @param TResponse レスポンス
    // */
    // private void checkResposeXml(TResponse response, String baseUri) {
    // String xml = response.getBody().replaceAll(
    // " xmlns=\"http://schemas.microsoft.com/ado/2007/08/dataservices\"", "");
    // Link linksUri = JAXB.unmarshal(new StringReader(xml), Link.class);
    // List<String> uris = linksUri.getUri();
    // for (String uri : uris) {
    // assertEquals(baseUri, uri);
    // }
    //
    // }

    /**
     * レスポンスボディのJSONをチェックする.
     * @param TResponse レスポンス
     */
    private void checkResposeJson(TResponse response, String baseUri) {
        JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        for (Object result : results) {
            assertEquals(baseUri, (String) ((JSONObject) result).get("uri"));
        }
    }
}

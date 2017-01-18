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

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
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
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * BoxとRelationの$linksのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxRelationLinkTest extends ODataCommon {

    private static final String CELL_NAME = "testcell1";
    private static final String ENTITY_SET_BOX = "Box";
    private static final String ENTITY_SET_RELATION = "Relation";
    private static final String NAV_PROP_BOX = "_Box";
    private static final String NAV_PROP_RELATION = "_Relation";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BoxRelationLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * BoxとRelationのlinkを一覧取得できること.
     */
    @Test
    public final void BoxとRelationのlinkを一覧取得できること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";

        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Boxに紐付くRelationのlink一覧取得
            TResponse res = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", "Name='" + boxName + "'")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダのチェック
            checkCommonResponseHeader(res);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(resRelation.getLocationHeader());
            checkLinResponseBody(res.bodyAsJson(), uri);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * Boxと紐付くRelationが存在しない場合Relationのlinkを一覧取得し200が返却されること.
     */
    @Test
    public final void Boxと紐付くRelationが存在しない場合Relationのlinkを一覧取得し200が返却されること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";

        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付かないRelation登録
            resRelation = createRelation(relationName, null);

            // Boxに紐付くRelationのlink一覧取得
            TResponse res = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_BOX)
                    .with("key", "Name='" + boxName + "'")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダのチェック
            checkCommonResponseHeader(res);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            checkLinResponseBody(res.bodyAsJson(), uri);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * RelationとBoxのlinkを一覧取得できること.
     */
    @Test
    public final void RelationとBoxのlinkを一覧取得できること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box1";

        TResponse resRelation = null;
        try {
            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Relationに紐付くBoxのlink一覧取得
            TResponse res = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_RELATION)
                    .with("key", "Name='" + relationName + "',_Box.Name='" + boxName + "'")
                    .with("navProp", NAV_PROP_BOX)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダのチェック
            checkCommonResponseHeader(res);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.cellCtl(CELL_NAME, ENTITY_SET_BOX, boxName));
            checkLinResponseBody(res.bodyAsJson(), uri);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());
        }
    }

    /**
     * BoxとIdなしRelationのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void BoxとIdなしRelationのlinkを更新するとresponseが400であること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";
        String linkPath = "__ctl/" + ENTITY_SET_BOX + "\\('" + boxName + "'\\)/\\$links/" + NAV_PROP_RELATION;
        String relationUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_RELATION, relationName);
        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Boxに紐付くRelationのlink一覧取得
            Http.request("link-update-with-body.txt")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("body", "\\{\\\"uri\\\":\\\"" + relationUri + "\\\"")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * BoxとIdありRelationのlinkを更新するとresponseが501であること.
     */
    @Test
    public final void BoxとIdありRelationのlinkを更新するとresponseが501であること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";
        String linkPath = "__ctl/" + ENTITY_SET_BOX + "\\('" + boxName + "'\\)/\\$links/"
            + NAV_PROP_RELATION + "\\('" + relationName + "'\\)";
        String relationUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_RELATION, relationName);
        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Boxに紐付くRelationのlink一覧取得
            Http.request("link-update-with-body.txt")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("body", "\\{\\\"uri\\\":\\\"" + relationUri + "\\\"")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * Relationと紐付くBoxが存在しない場合Boxのlinkを一覧取得し200が返却されること.
     */
    @Test
    public final void Relationと紐付くBoxが存在しない場合Boxのlinkを一覧取得し200が返却されること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = null;

        TResponse resRelation = null;
        try {
            // Boxと紐付かないRelation登録
            resRelation = createRelation(relationName, boxName);

            // Relationに紐付くBoxのlink一覧取得
            TResponse res = Http.request("links-request-no-navkey.txt")
                    .with("method", "GET")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("entitySet", ENTITY_SET_RELATION)
                    .with("key", "Name='" + relationName + "'")
                    .with("navProp", NAV_PROP_BOX)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_OK);

            // レスポンスヘッダのチェック
            checkCommonResponseHeader(res);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            checkLinResponseBody(res.bodyAsJson(), uri);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());
        }
    }

    /**
     * RelationとIdなしBoxのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void RelationとIdなしBoxのlinkを更新するとresponseが400であること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";
        String linkPath = "__ctl/" + ENTITY_SET_RELATION + "\\('" + relationName + "'\\)/\\$links/" + NAV_PROP_BOX;
        String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, boxName);

        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Boxに紐付くRelationのlink一覧取得
            Http.request("link-update-with-body.txt")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("body", "\\{\\\"uri\\\":\\\"" + boxUri + "\\\"")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * RelationとIdありBoxのlinkを更新するとresponseが501であること.
     */
    @Test
    public final void RelationとIdありBoxのlinkを更新するとresponseが501であること() {
        String relationName = "relation_BoxRelationLinkTest";
        String boxName = "box_BoxRelationLinkTest";
        String linkPath = "__ctl/" + ENTITY_SET_RELATION + "\\('" + relationName + "'\\)/\\$links/"
            + NAV_PROP_BOX + "\\('" + boxName + "'\\)";
        String boxUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_BOX, boxName);

        TResponse resBox = null;
        TResponse resRelation = null;
        try {
            // Box作成
            resBox = BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Boxと紐付いたRelation登録
            resRelation = createRelation(relationName, boxName);

            // Boxに紐付くRelationのlink一覧取得
            Http.request("link-update-with-body.txt")
                    .with("token", TOKEN)
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("body", "\\{\\\"uri\\\":\\\"" + boxUri + "\\\"")
                    .with("navProp", NAV_PROP_RELATION)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);

        } finally {
            // Relation削除
            deleteOdataResource(resRelation.getLocationHeader());

            // Box削除
            deleteOdataResource(resBox.getLocationHeader());
        }
    }

    /**
     * BoxとRelationのLink削除時に単一キーの同名Relationが存在すると409になること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void BoxとRelationのLink削除時に単一キーの同名Relationが存在すると409になること() {
        final String boxName = "relationLinkBox";
        final String relationName = "boxLinkRelation";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRelation作成
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", boxName);
            RelationUtils.create(CELL_NAME, TOKEN, body, HttpStatus.SC_CREATED);

            // 上のBoxと結びつかないRelation作成
            body.remove("_Box.Name");
            RelationUtils.create(CELL_NAME, TOKEN, body, HttpStatus.SC_CREATED);

            String relationKeyName = "_Box.Name='" + boxName + "',Name='" + relationName + "'";

            // BoxとRelationのLink削除（単一キーのRelationが存在するため削除できない(409))
            deleteBoxRelationLink(boxName, relationKeyName, HttpStatus.SC_CONFLICT);
            // RelationとBoxのLink削除（逆向きでも同様に409）
            deleteRelationBoxLink(relationKeyName, boxName, HttpStatus.SC_CONFLICT);

            // Relationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);

            // BoxとRelationのLink削除（単一キーのRelationが存在しないので削除できる)
            deleteBoxRelationLink(boxName, relationKeyName, HttpStatus.SC_NO_CONTENT);

            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, boxName, -1);
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * BoxとRelationのLink作成時に複合キーの同名Relationが存在すると409になること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void BoxとRelationのLink作成時に複合キーの同名Relationが存在すると409になること() {
        final String boxName = "relationLinkBox";
        final String relationName = "boxLinkRelation";
        try {
            // Boxの作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN, HttpStatus.SC_CREATED);

            // 上のBoxと結びつくRelation作成
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", boxName);
            RelationUtils.create(CELL_NAME, TOKEN, body, HttpStatus.SC_CREATED);

            // 上のBoxと結びつかないRelation作成
            body.remove("_Box.Name");
            RelationUtils.create(CELL_NAME, TOKEN, body, HttpStatus.SC_CREATED);

            String relationKeyName = "_Box.Name='" + boxName + "',Name='" + relationName + "'";

            // BoxとRelationのLink作成（複合キーの同一Relationが存在するため作成できない(409))
            createBoxRelationLink(boxName, relationName, HttpStatus.SC_CONFLICT);
            // RelationとBoxのLink作成（逆向きでも同様に409）
            createRelationBoxLink(relationName, boxName, HttpStatus.SC_CONFLICT);

            // Relationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);

            // BoxとRelationのLink削除（単一キーのRelationが存在しないので削除できる)
            deleteBoxRelationLink(boxName, relationKeyName, HttpStatus.SC_NO_CONTENT);

            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, HttpStatus.SC_NO_CONTENT);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくRelationの削除
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, boxName, -1);
            RelationUtils.delete(CELL_NAME, TOKEN, relationName, null, -1);

            // Boxの削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }


    /**
     * BoxとRelationのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること.
     */
    @Test
    public final void BoxとRelationのlink作成時URLのNP名とボディのエンティティ名が異なる場合400になること() {
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1)
                    + "__ctl/Relation(Name='" + Setup.CELL_RELATION + "',_Box.Name=null)";

        // Box-Relationの$link
         Http.request("links-request-with-body.txt")
            .with("method", "POST")
            .with("token", AbstractCase.MASTER_TOKEN_NAME)
            .with("cellPath", Setup.TEST_CELL1)
            .with("entitySet", "Box")
            .with("key", "'" + Setup.TEST_BOX1 + "'")
            .with("navProp", "_Role")
            .with("uri", targetUri)
            .returns()
            .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    private void deleteBoxRelationLink(final String boxName, final String linkRoleKey, final int status) {
        Http.request("links-request.txt")
        .with("method", "DELETE")
        .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", CELL_NAME)
        .with("entitySet", ENTITY_SET_BOX)
        .with("key", "'" + boxName + "'")
        .with("navProp", NAV_PROP_RELATION)
        .with("navKey", linkRoleKey)
        .returns()
        .debug()
        .statusCode(status);
    }

    private void deleteRelationBoxLink(final String roleName, final String linkBoxKey, final int status) {
        Http.request("links-request.txt")
        .with("method", "DELETE")
        .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", CELL_NAME)
        .with("entitySet", ENTITY_SET_RELATION)
        .with("key", roleName)
        .with("navProp", NAV_PROP_BOX)
        .with("navKey", "'" + linkBoxKey + "'")
        .returns()
        .debug()
        .statusCode(status);
    }


    private void createBoxRelationLink(final String boxName, final String relationName, final int expectedStatus) {
        String url  = UrlUtils.cellCtl(CELL_NAME, ENTITY_SET_RELATION, relationName);
        Http.request("links-request-with-body.txt")
        .with("method", "POST")
        .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", CELL_NAME)
        .with("entitySet", ENTITY_SET_BOX)
        .with("key", "'" + boxName + "'")
        .with("navProp", NAV_PROP_RELATION)
        .with("uri", url)
        .returns()
        .debug()
        .statusCode(expectedStatus);
    }

    private void createRelationBoxLink(final String relationName, final String boxName, final int expectedStatus) {
        String url  = UrlUtils.cellCtl(CELL_NAME, ENTITY_SET_BOX, boxName);
        Http.request("links-request-with-body.txt")
        .with("method", "POST")
        .with("token", AbstractCase.MASTER_TOKEN_NAME)
        .with("cellPath", CELL_NAME)
        .with("entitySet", ENTITY_SET_RELATION)
        .with("key", "'" + relationName + "'")
        .with("navProp", NAV_PROP_BOX)
        .with("uri", url)
        .returns()
        .debug()
        .statusCode(expectedStatus);
    }


    /**
     * Relation作成.
     * @param relationName relationName
     * @param boxName boxName(指定しない場合はnull)
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    private TResponse createRelation(String relationName, String boxName) {
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("Name", relationName);
        if (boxName != null) {
            bodyJson.put("_Box.Name", boxName);
        }
        return RelationUtils.create(CELL_NAME, TOKEN, bodyJson, HttpStatus.SC_CREATED);
    }

}

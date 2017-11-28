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

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import io.personium.core.model.Box;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;

/**
 * Relationの削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RelationDeleteTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRelationName = "testrelation";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RelationDeleteTest() {
        super("io.personium.core.rs");
    }

    /**
     * Relation削除のテスト.
     */
    @Test
    public void Relation削除の正常系ボックス指定ありのテスト() {
        String boxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, testRelationName, boxName);
        } finally {
            TResponse res = RelationUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    testRelationName, boxName, HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダーのチェック
            // DataServiceVersion
            res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        }
    }

    /**
     * Relation削除のテスト.
     */
    @Test
    public void Relation削除の正常系ボックス指定なしのテスト() {
        try {
            CellCtlUtils.createRelation(cellName, testRelationName);
        } finally {
            TResponse res = RelationUtils.delete(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    testRelationName, null, HttpStatus.SC_NO_CONTENT);
            // レスポンスヘッダーのチェック
            // DataServiceVersion
            res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        }
    }

    /**
     * BoxNameを省略してRelationを削除した場合データが削除できること.
     */
    @Test
    public void BoxNameを省略してRelationを削除した場合データが削除できること() {
        try {
            CellCtlUtils.createRelation(cellName, testRelationName);
        } finally {
            TResponse res = Http.request("relation-delete-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // レスポンスヘッダーのチェック
            // DataServiceVersion
            res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        }
    }

    /**
     * BoxNameを指定してRelationを登録し、BoxNameを省略してRelationを削除した場合404が返却されること.
     */
    @Test
    public void BoxNameを指定してRelationを登録しBoxNameを省略してRelationを削除した場合404が返却されること() {
        String boxname = "box1";
        TResponse res = null;
        try {
            res = CellCtlUtils.createRelation(cellName, testRelationName, boxname);
        } finally {
            Http.request("relation-delete-without-boxname.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
    }

    /**
     * ExtRoleとLinkされているRelationを削除するとresponseが409であること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtRoleとLinkされているRelationを削除するとresponseが409であること() {

        String boxName = "box1";
        String relationName = "boxLinkrelation";
        String extRoleName = UrlUtils.roleResource(cellName, "__", "relationLinkextRole");

        JSONObject relationBody = new JSONObject();
        relationBody.put("Name", relationName);
        relationBody.put("_Box.Name", boxName);

        JSONObject extRoleBody = new JSONObject();
        extRoleBody.put("ExtRole", extRoleName);
        extRoleBody.put("_Relation.Name", relationName);
        extRoleBody.put("_Relation._Box.Name", boxName);
        try {
            // Relationの作成
            RelationUtils.create(cellName, MASTER_TOKEN_NAME, relationBody, HttpStatus.SC_CREATED);

            // 上のRelationと結びつくExtRole作成
            ExtRoleUtils.create(MASTER_TOKEN_NAME, cellName, extRoleBody, HttpStatus.SC_CREATED);

            // Relationの削除(結びつくextRoleがあるため、409)
            RelationUtils.delete(cellName, MASTER_TOKEN_NAME, relationName, boxName, HttpStatus.SC_CONFLICT);

            // 結びつくextRoleの削除
            ExtRoleUtils.delete(MASTER_TOKEN_NAME, cellName, extRoleName, "'" + relationName + "'",
                    "'" + boxName + "'", HttpStatus.SC_NO_CONTENT);

            // Relationの削除(結びつくextRoleが存在しないため、204)
            RelationUtils.delete(cellName, MASTER_TOKEN_NAME, relationName, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            // 結びつくextRoleの削除
            ExtRoleUtils.delete(MASTER_TOKEN_NAME, cellName, extRoleName, "'" + relationName + "'",
                    "'" + boxName + "'", -1);
            // Relationの削除
            RelationUtils.delete(cellName, MASTER_TOKEN_NAME, relationName, boxName, -1);
        }
    }

    /**
     * Normal test.
     * Delete relation linked with role.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_delete_relation_linked_with_role() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String boxName = null;
        String roleName = "role";
        String relationName = "relationhoge";

        try {
            // 準備。Relation、ロール作ってリンクさせる。
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", null);
            RelationUtils.create(cellName, token, body, HttpStatus.SC_CREATED);
            RoleUtils.create(cellName, token, roleName, boxName, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(cellName, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, token, HttpStatus.SC_NO_CONTENT);

            RelationUtils.delete(cellName, token, relationName, null, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinks(cellName, Relation.EDM_TYPE_NAME, relationName, null,
                    Role.EDM_TYPE_NAME, roleName, null, token, -1);
            RoleUtils.delete(cellName, token, roleName, boxName, -1);
            RelationUtils.delete(cellName, token, relationName, null, -1);
        }
    }

    /**
     * Error test.
     * Delete box linked with relation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void error_delete_box_linked_with_relation() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String boxName = "boxhuga";
        String relationName = "relationhuge";

        try {
            // Boxの作成
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);

            // 準備。Relation、ロール作ってリンクさせる。
            JSONObject body = new JSONObject();
            body.put("Name", relationName);
            body.put("_Box.Name", null);
            RelationUtils.create(cellName, token, body, HttpStatus.SC_CREATED);
            LinksUtils.createLinks(cellName, Relation.EDM_TYPE_NAME, relationName, null,
                    Box.EDM_TYPE_NAME, boxName, null, token, HttpStatus.SC_NO_CONTENT);

            // 削除できないことの確認
            BoxUtils.delete(cellName, token, boxName, HttpStatus.SC_CONFLICT);

            // リンクを解除し、削除できるようになることの確認
            LinksUtils.deleteLinks(cellName, Relation.EDM_TYPE_NAME, relationName, boxName,
                    Box.EDM_TYPE_NAME, boxName, null, token, HttpStatus.SC_NO_CONTENT);
            BoxUtils.delete(cellName, token, boxName, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinks(cellName, Relation.EDM_TYPE_NAME, relationName, boxName,
                    Box.EDM_TYPE_NAME, boxName, null, token, -1);
            BoxUtils.delete(cellName, token, boxName, -1);
            RelationUtils.delete(cellName, token, relationName, null, -1);
        }
    }

    /**
     * BoxNameを指定せずRelationを登録し、BoxNameに存在しないBox名を指定してRelationを削除した場合404が返却されること.
     */
    @Test
    public void BoxNameを指定せずRelationを登録しBoxNameに存在しないBox名を指定してRelationを削除した場合404が返却されること() {
        String dummyBoxName = "dummy";
        TResponse res = null;
        try {
            res = CellCtlUtils.createRelation(cellName, testRelationName);
        } finally {
            Http.request("relation-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("relationname", testRelationName)
                    .with("boxname", "'" + dummyBoxName + "'")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);

            if (res.getLocationHeader() != null) {
                deleteOdataResource(res.getLocationHeader());
            }
        }
    }
}

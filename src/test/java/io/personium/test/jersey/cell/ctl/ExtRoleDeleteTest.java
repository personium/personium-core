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
package io.personium.test.jersey.cell.ctl;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * ExtRoleの削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleDeleteTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testExtRoleName = UrlUtils.roleResource(cellName, "__", "testrole");
    private final String token = PersoniumUnitConfig.getMasterToken();

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleDeleteTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ExtRole削除のテスト ExtRole, _Relation.Name, _Relation._Box.Name指定あり.
     */
    @Test
    public void ExtRole削除でボックスありのリレーションを指定して正常に削除できること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(cellName, testExtRoleName, relationName, relationBoxName);
        } finally {
            deleteExtRole("'" + relationName + "'", "'" + relationBoxName + "'");
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRole削除のテスト _Relation.Name, _Relation._Box.Nameにnull指定.
     */
    @Test
    public void ExtRole削除でボックスなしのリレーションを指定して正常に削除できること() {
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            CellCtlUtils.createExtRole(cellName, testExtRoleName, relationName, null);
        } finally {
            deleteExtRole("'" + relationName + "'", "null");
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRole削除でリレーションを指定しない場合404エラーが返却されること.
     */
    @Test
    public void ExtRole削除でリレーションを指定しない場合404エラーが返却されること() {
        deleteExtRole();
    }

    /**
     * Urlが数字の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが数字の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.DELETE, cellName, "123", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlが真偽値の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが真偽値の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.DELETE, cellName, "false", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * 指定されたボックス名にリンクされたExtRole情報を削除する.
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    private void deleteExtRole(
            String relationName,
            String relationBoxName) {
        TResponse res = Http.request("cell/extRole/extRole-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("extRoleName", CommonUtils.encodeUrlComp(testExtRoleName))
                .with("relationName", relationName)
                .with("relationBoxName", relationBoxName)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        // レスポンスヘッダーのチェック
        // DataServiceVersion
        res.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
    }

    /**
     * 指定されたボックス名にリンクされたExtRole情報を削除する.
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    private void deleteExtRole() {
        Http.request("cell/extRole/extRole-delete-norelation.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("extRoleName", CommonUtils.encodeUrlComp(testExtRoleName))
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Normal test.
     * Delete extrole linked with role.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_delete_extrole_linked_with_role() {
        String boxName = null;
        String roleName = "role";
        String extRole = "extRole";
        String extRoleName = UrlUtils.roleResource("extCell", null, extRole);

        try {
            // 準備。ExtRole、ロール作ってリンクさせる。
            JSONObject extRoleBody = new JSONObject();
            extRoleBody.put("ExtRole", extRoleName);
            extRoleBody.put("_Relation.Name", "relation");
            extRoleBody.put("_Relation._Box.Name", null);
            JSONObject relationBody = new JSONObject();
            relationBody.put("Name", "relation");
            relationBody.put("_Box.Name", null);
            RelationUtils.create(cellName, token, relationBody, -1);
            ExtRoleUtils.create(token, cellName, extRoleBody, HttpStatus.SC_CREATED);
            RoleUtils.create(cellName, token, roleName, boxName, HttpStatus.SC_CREATED);
            LinksUtils.createLinksExtRole(cellName, CommonUtils.encodeUrlComp(extRoleName),
                    "relation", null, Role.EDM_TYPE_NAME, roleName, null, token, HttpStatus.SC_NO_CONTENT);

            ExtRoleUtils.delete(cellName, extRoleName, "relation", null, token, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinksExtRole(cellName, CommonUtils.encodeUrlComp(extRoleName),
                    "relation", null, Role.EDM_TYPE_NAME, roleName, null, token, -1);
            RoleUtils.delete(cellName, token, roleName, boxName, -1);
            ExtRoleUtils.delete(cellName, extRoleName, "relation", null, token, -1);
            RelationUtils.delete(cellName, token, "relation", null, -1);
        }
    }
}

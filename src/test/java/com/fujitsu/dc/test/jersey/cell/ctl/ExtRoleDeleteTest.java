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

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.ExtRoleUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ExtRoleの削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleDeleteTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testExtRoleName = UrlUtils.roleResource(cellName, "__", "testrole");
    private final String token = DcCoreConfig.getMasterToken();

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleDeleteTest() {
        super("com.fujitsu.dc.core.rs");
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
                .with("extRoleName", DcCoreUtils.encodeUrlComp(testExtRoleName))
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
                .with("extRoleName", DcCoreUtils.encodeUrlComp(testExtRoleName))
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * RoleとLinkされているExtRoleを削除するとresponseが409であること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RoleとLinkされているExtRoleを削除するとresponseが409であること() {
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
            RoleUtils.create(cellName, token, boxName, roleName, HttpStatus.SC_CREATED);
            ResourceUtils.linksExtRoleToRole(cellName, DcCoreUtils.encodeUrlComp(extRoleName),
                    "'relation'", "null", UrlUtils.roleUrl(cellName, null, roleName), token);

            // 削除できないことの確認
            ExtRoleUtils.delete(token, cellName, extRoleName, "'relation'", "null", HttpStatus.SC_CONFLICT);

            // リンクを解除し、削除できるようになることの確認
            ResourceUtils.linksDeleteExtRoleToRole(cellName, DcCoreUtils.encodeUrlComp(extRoleName),
                    "relation", "null", roleName, token);
            ExtRoleUtils.delete(token, cellName, extRoleName, "'relation'", "null", HttpStatus.SC_NO_CONTENT);
        } finally {
            ResourceUtils.linksDeleteExtRoleToRole(cellName, DcCoreUtils.encodeUrlComp(extRoleName),
                    "relation", "null", roleName, token);
            RoleUtils.delete(cellName, token, boxName, roleName, -1);
            ExtRoleUtils.delete(token, cellName, extRoleName, "'relation'", "null", -1);
            RelationUtils.delete(cellName, token, "relation", null, -1);
        }
    }
}

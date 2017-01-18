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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * RoleのNP経由登録／一覧取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleViaNPTest extends ODataCommon {

    private static final String CELL_NAME = Setup.TEST_CELL1;
    private static final String ROLE_TYPE = "CellCtl.Role";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleViaNPTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * RelationからNP経由でRoleの登録_一覧取得ができること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public void RelationからNP経由でRoleの登録_一覧取得ができること() throws ParseException {
        String roleName = "RoleViaNPTestRole";
        String relationName = "RoleViaNPTestRelation";

        try {
            // Relation作成
            CellCtlUtils.createRelation(CELL_NAME, relationName);

            // Relation-RoleNP経由登録
            TResponse res = RoleUtils.createViaNP(CELL_NAME, MASTER_TOKEN_NAME, "Relation",
                    RelationUtils.keyString(relationName),
                    roleName, HttpStatus.SC_CREATED);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, "CellCtl.Role",
                    (JSONObject) (new JSONParser()).parse("{\"Name\":\"" + roleName + "\"}"));

            // Relation-RoleNP経由一覧取得
            res = RoleUtils.listViaNP(CELL_NAME, MASTER_TOKEN_NAME, "Relation", RelationUtils.keyString(relationName));

            // レスポンスヘッダーのチェック
            String location = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, Role.EDM_TYPE_NAME,
                    "Name='" + roleName + "',_Box.Name=null'");
            ODataCommon.checkCommonResponseHeader(res);

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", roleName);
            additional.put("_Box.Name", null);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), location, ROLE_TYPE, additional);

        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Relation", RelationUtils.keyString(relationName), -1);

            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);

            // Relation削除
            RelationUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, relationName, null, -1);
        }
    }

    /**
     * RelationからNP経由でRoleの登録で同じ名前のRoleが既に存在する場合409となること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public void RelationからNP経由でRoleの登録で同じ名前のRoleが既に存在する場合409となること() throws ParseException {
        String roleName = "RoleViaNPTestRole";
        String relationName = "RoleViaNPTestRelation";

        try {
            // Role作成
            CellCtlUtils.createRole(CELL_NAME, roleName);
            // Relation作成
            CellCtlUtils.createRelation(CELL_NAME, relationName);

            // Relation-RoleNP経由登録
            RoleUtils.createViaNP(CELL_NAME, MASTER_TOKEN_NAME, "Relation",
                    RelationUtils.keyString(relationName),
                    roleName, HttpStatus.SC_CONFLICT);
        } finally {
            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);

            // Relation削除
            RelationUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, relationName, null, -1);
        }
    }
}

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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.ctl.ExtRole;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtRoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ExtCell取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleReadTest extends ODataCommon {

    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    private static String cellName = "testcell1";
    private static String testExtRoleName = UrlUtils.roleResource(cellName, "__", "testextrole");
    private static final String EXT_ROLE_TYPE = "CellCtl.ExtRole";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleReadTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ExtRole取得の正常系のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtRole取得の正常系のテスト() {
        String relationName = "testrelation02";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);
            TResponse response = ExtRoleUtils.get(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'", HttpStatus.SC_OK);

            String location = UrlUtils.cellCtlWithoutSingleQuote(cellName,
                    ExtRole.EDM_TYPE_NAME,
                    "ExtRole='" + testExtRoleName + "',_Relation.Name='" + relationName + "',_Relation._Box.Name='"
                            + relationBoxName + "'");

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("ExtRole", testExtRoleName);
            additional.put("_Relation.Name", relationName);
            additional.put("_Relation._Box.Name", relationBoxName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, EXT_ROLE_TYPE, additional);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRoleが存在しない場合404エラーを返却すること.
     */
    @Test
    public final void ExtRoleが存在しない場合404エラーを返却すること() {
        ExtRoleUtils.get(token, cellName, testExtRoleName + "A",
                "'testrelation'", "'box1'", HttpStatus.SC_NOT_FOUND);
    }

}

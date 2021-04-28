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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

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
        super(new PersoniumCoreApplication());
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

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("ExtRole", testExtRoleName);
            additional.put("_Relation.Name", relationName);
            additional.put("_Relation._Box.Name", relationBoxName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    EXT_ROLE_TYPE, additional);
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

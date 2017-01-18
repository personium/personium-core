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

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.ExtRoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ExtCell一覧取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleListTest extends ODataCommon {

    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    private static String extRoleTestCell = "testextrolecell1";
    private static String testExtRoleName = UrlUtils.roleResource(extRoleTestCell, "__", "testextrole");
    private static final String EXT_ROLE_TYPE = "CellCtl.ExtRole";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ExtRole一覧取得の正常系のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtRole一覧取得の正常系のテスト() {
        String relationName = "testrelation02";
        String relationBoxName = "box1";
        try {
            // Cell作成
            CellUtils.create(extRoleTestCell, token, HttpStatus.SC_CREATED);
            // Box作成
            BoxUtils.create(extRoleTestCell, relationBoxName, token);
            // Relation作成
            CellCtlUtils.createRelation(extRoleTestCell, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);
            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", testExtRoleName + "1");
            body2.put("_Relation.Name", relationName);
            body2.put("_Relation._Box.Name", relationBoxName);
            // ExtRole作成
            ExtRoleUtils.create(token, extRoleTestCell, body, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(token, extRoleTestCell, body2, HttpStatus.SC_CREATED);
            // ExtRole一覧取得
            TResponse response = ExtRoleUtils.list(token, extRoleTestCell, HttpStatus.SC_OK);

            // レスポンスボディーのチェック(URI)
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(testExtRoleName,
                    UrlUtils.extRoleUrl(extRoleTestCell, relationBoxName, relationName, testExtRoleName));
            uri.put(testExtRoleName + "1",
                    UrlUtils.extRoleUrl(extRoleTestCell, relationBoxName, relationName, testExtRoleName + "1"));

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additionalprop.put("ExtRole", testExtRoleName);
            additionalprop.put("_Relation.Name", relationName);
            additionalprop.put("_Relation._Box.Name", relationBoxName);
            additionalprop2.put("ExtRole", testExtRoleName + "1");
            additionalprop2.put("_Relation.Name", relationName);
            additionalprop2.put("_Relation._Box.Name", relationBoxName);
            additional.put(testExtRoleName, additionalprop);
            additional.put(testExtRoleName + "1", additionalprop2);

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri,
                    EXT_ROLE_TYPE, additional, "ExtRole");
        } finally {
            CellCtlUtils.deleteExtRole(extRoleTestCell, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteExtRole(extRoleTestCell, testExtRoleName + "1", relationName, relationBoxName);
            CellCtlUtils.deleteRelation(extRoleTestCell, relationName, relationBoxName);
            BoxUtils.delete(extRoleTestCell, token, relationBoxName);
            CellUtils.delete(token, extRoleTestCell);
        }
    }
}

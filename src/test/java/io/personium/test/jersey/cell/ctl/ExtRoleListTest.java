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

import java.net.URLEncoder;
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
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * ExtCell一覧取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleListTest extends ODataCommon {

    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    private static final String EXT_ROLE_TEST_CELL = "testextrolecell1";
    private static final String EXT_ROLE_URL = UrlUtils.roleResource(EXT_ROLE_TEST_CELL, "__", "testextrole");
    private static final String EXT_ROLE_TYPE = "CellCtl.ExtRole";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleListTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ExtRole一覧取得の正常系のテスト.
     * @throws Exception exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtRole一覧取得の正常系のテスト() throws Exception {
        String relationName1 = "testrelation01";
        String relationBoxName1 = "box1";
        String relationName2 = "testrelation02";
        String relationBoxName2 = null;
        try {
            // Cell作成
            CellUtils.create(EXT_ROLE_TEST_CELL, token, HttpStatus.SC_CREATED);
            // Box作成
            BoxUtils.create(EXT_ROLE_TEST_CELL, relationBoxName1, token);
            // Relation作成
            CellCtlUtils.createRelation(EXT_ROLE_TEST_CELL, relationName1, relationBoxName1);
            CellCtlUtils.createRelation(EXT_ROLE_TEST_CELL, relationName2, relationBoxName2);
            JSONObject body = new JSONObject();
            body.put("ExtRole", EXT_ROLE_URL);
            body.put("_Relation.Name", relationName1);
            body.put("_Relation._Box.Name", relationBoxName1);
            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", EXT_ROLE_URL + "1");
            body2.put("_Relation.Name", relationName2);
            body2.put("_Relation._Box.Name", relationBoxName2);
            // ExtRole作成
            ExtRoleUtils.create(token, EXT_ROLE_TEST_CELL, body, HttpStatus.SC_CREATED);
            ExtRoleUtils.create(token, EXT_ROLE_TEST_CELL, body2, HttpStatus.SC_CREATED);
            // ExtRole一覧取得
            TResponse response = ExtRoleUtils.list(token, EXT_ROLE_TEST_CELL, HttpStatus.SC_OK);

            // レスポンスボディーのチェック(URI)
            Map<String, String> uri = new HashMap<String, String>();
            String encodedExtRoleUrl = URLEncoder.encode(EXT_ROLE_URL, "utf-8");
            uri.put(EXT_ROLE_URL,
                    UrlUtils.extRoleUrl(EXT_ROLE_TEST_CELL, relationBoxName1, relationName1, encodedExtRoleUrl));
            uri.put(EXT_ROLE_URL + "1",
                    UrlUtils.extRoleUrl(EXT_ROLE_TEST_CELL, relationBoxName2, relationName2, encodedExtRoleUrl + "1"));

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additionalprop.put("ExtRole", EXT_ROLE_URL);
            additionalprop.put("_Relation.Name", relationName1);
            additionalprop.put("_Relation._Box.Name", relationBoxName1);
            additionalprop2.put("ExtRole", EXT_ROLE_URL + "1");
            additionalprop2.put("_Relation.Name", relationName2);
            additionalprop2.put("_Relation._Box.Name", relationBoxName2);
            additional.put(EXT_ROLE_URL, additionalprop);
            additional.put(EXT_ROLE_URL + "1", additionalprop2);

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri,
                    EXT_ROLE_TYPE, additional, "ExtRole");
        } finally {
            ExtRoleUtils.delete(EXT_ROLE_TEST_CELL, EXT_ROLE_URL, relationName1, relationBoxName1,
                    AbstractCase.MASTER_TOKEN_NAME, -1);
            ExtRoleUtils.delete(EXT_ROLE_TEST_CELL, EXT_ROLE_URL + "1", relationName2, relationBoxName2,
                    AbstractCase.MASTER_TOKEN_NAME, -1);
            RelationUtils.delete(EXT_ROLE_TEST_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    relationName1, relationBoxName1, -1);
            RelationUtils.delete(EXT_ROLE_TEST_CELL, AbstractCase.MASTER_TOKEN_NAME,
                    relationName2, relationBoxName2, -1);
            BoxUtils.delete(EXT_ROLE_TEST_CELL, token, relationBoxName1);
            CellUtils.delete(token, EXT_ROLE_TEST_CELL);
        }
    }
}

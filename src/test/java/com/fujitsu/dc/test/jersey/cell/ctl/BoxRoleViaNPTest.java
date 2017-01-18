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

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * BoxとRoleのNP経由登録／一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BoxRoleViaNPTest extends JerseyTest {

    private static final String CELL_NAME = Setup.TEST_CELL1;
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BoxRoleViaNPTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * RoleからNP経由でBoxの登録_一覧取得ができること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RoleからNP経由でBoxの登録_一覧取得ができること() throws ParseException {
        String roleName = "boxRoleNpTestRole";
        String boxName = "boxRoleNpTestBox";
        String requestBody = "{\"Name\":\"" + boxName + "\"}";

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, TOKEN, roleName, HttpStatus.SC_CREATED);

            // Role-Box NP経由登録
            TResponse res = BoxUtils.createViaNP(TOKEN, CELL_NAME, "Role", RoleUtils.keyString(roleName), requestBody);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, "CellCtl.Box",
                    (JSONObject) (new JSONParser()).parse(requestBody));

            // Role-Box NP経由一覧取得
            res = BoxUtils.listViaNP(TOKEN, CELL_NAME, "Role", RoleUtils.keyString(roleName, boxName));
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, "CellCtl.Box",
                    (JSONObject) (new JSONParser()).parse(requestBody));

        } finally {
            // Role削除
            RoleUtils.delete(CELL_NAME, TOKEN, boxName, roleName, -1);

            // Box削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }

    /**
     * RoleからNP経由でBoxの登録でRoleが既に他のBoxと紐付いている場合409となること.
     */
    @Test
    public final void RoleからNP経由でBoxの登録でRoleが既に他のBoxと紐付いている場合409となること() {
        String roleName = "boxRoleNpTestRole";
        String boxName = "boxRoleNpTestBox";
        String baseBoxName = "boxRoleNpTestBaseBox";
        String requestBody = "{\"Name\":\"" + boxName + "\"}";

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, TOKEN, roleName, HttpStatus.SC_CREATED);

            // Role-Box NP経由登録
            BoxUtils.createViaNP(TOKEN, CELL_NAME, "Role", RoleUtils.keyString(roleName),
                    "{\"Name\":\"" + baseBoxName + "\"}");

            // Role-Box NP経由登録
            BoxUtils.createViaNP(TOKEN, CELL_NAME, "Role", RoleUtils.keyString(roleName, baseBoxName),
                    requestBody, HttpStatus.SC_CONFLICT);

        } finally {
            // Role削除
            RoleUtils.delete(CELL_NAME, TOKEN, baseBoxName, roleName, -1);

            // Box削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
            BoxUtils.delete(CELL_NAME, TOKEN, baseBoxName, -1);
        }
    }

    /**
     * RoleからNP経由でBoxの登録で同じ名前のBoxが既に存在する場合409となること.
     */
    @Test
    public final void RoleからNP経由でBoxの登録で同じ名前のBoxが既に存在する場合409となること() {
        String roleName = "boxRoleNpTestRole";
        String boxName = "boxRoleNpTestBox";
        String requestBody = "{\"Name\":\"" + boxName + "\"}";

        try {
            // Box作成
            BoxUtils.create(CELL_NAME, boxName, TOKEN);

            // Role作成
            RoleUtils.create(CELL_NAME, TOKEN, roleName, HttpStatus.SC_CREATED);

            // Role-Box NP経由登録
            BoxUtils.createViaNP(TOKEN, CELL_NAME, "Role", RoleUtils.keyString(roleName), requestBody,
                    HttpStatus.SC_CONFLICT);

        } finally {
            // Role削除
            RoleUtils.delete(CELL_NAME, TOKEN, null, roleName, -1);

            // Box削除
            BoxUtils.delete(CELL_NAME, TOKEN, boxName, -1);
        }
    }
}

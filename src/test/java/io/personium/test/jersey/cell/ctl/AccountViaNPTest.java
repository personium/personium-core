/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;

/**
 * AccountのNP経由登録／一覧取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class AccountViaNPTest extends ODataCommon {

    private static final String CELL_NAME = Setup.TEST_CELL1;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountViaNPTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * RoleからNP経由でAccountの登録ができること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public void RoleからNP経由でAccountの登録ができること() throws ParseException {
        String roleName = "AccountViaNPTestRole";
        String accountName = "AccountViaNPTestAccount";

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, MASTER_TOKEN_NAME, roleName, HttpStatus.SC_CREATED);

            // Role-AccountNP経由登録
            String body = "{\"Name\":\"" + accountName + "\"}";
            TResponse res = AccountUtils.createViaNPNonCredential(CELL_NAME, MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
            ODataCommon.checkResponseBody(res.bodyAsJson(),
                    null,
                    "CellCtl.Account",
                    (JSONObject) (new JSONParser()).parse("{\"Name\":\"" + accountName
                            + "\"}"));
        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Account", "'" + accountName + "'", -1);

            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, roleName, null, -1);

            // Account削除
            AccountUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, accountName, -1);
        }
    }
}

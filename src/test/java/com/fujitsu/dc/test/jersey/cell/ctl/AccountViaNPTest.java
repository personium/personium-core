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

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.utils.ODataUtils;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.UserDataListFilterTest;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

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
        super("com.fujitsu.dc.core.rs");
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
                            + "\", \"LastAuthenticated\":null}"));
        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Account", "'" + accountName + "'", -1);

            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);

            // Account削除
            AccountUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * RoleからNP経由でLastAuthenticatedに時刻を指定したAccountの登録ができること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public void RoleからNP経由でLastAuthenticatedに時刻を指定したAccountの登録ができること() throws ParseException {
        String roleName = "AccountViaNPTestRole";
        String accountName = "AccountViaNPTestAccount";
        long datelong = 1414656074074L;

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, MASTER_TOKEN_NAME, roleName, HttpStatus.SC_CREATED);

            // Role-AccountNP経由登録
            String body = "{\"Name\":\"" + accountName + "\", \"LastAuthenticated\":\"/Date(" + datelong + ")/\"}";
            TResponse res = AccountUtils.createViaNPNonCredential(CELL_NAME, MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
            ODataCommon.checkResponseBody(res.bodyAsJson(),
                    null,
                    "CellCtl.Account",
                    (JSONObject) (new JSONParser()).parse(body));
        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Account", "'" + accountName + "'", -1);

            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);

            // Account削除
            AccountUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * RoleからNP経由でLastAuthenticatedにSYSUTCDATETIMEを指定したAccountの登録ができること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public void RoleからNP経由でLastAuthenticatedにSYSUTCDATETIMEを指定したAccountの登録ができること() throws ParseException {
        String roleName = "AccountViaNPTestRole";
        String accountName = "AccountViaNPTestAccount";

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, MASTER_TOKEN_NAME, roleName, HttpStatus.SC_CREATED);

            // Role-AccountNP経由登録
            String body = "{\"Name\":\"" + accountName + "\", \"LastAuthenticated\":\"" + Common.SYSUTCDATETIME + "\"}";
            TResponse res = AccountUtils.createViaNPNonCredential(CELL_NAME, MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_CREATED);
            String lastAuthenticated = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results"))
                    .get("LastAuthenticated");

            assertTrue("LastAuthenticatedがpersonium.ioの日付書式としてバリデートできなかった 。 lastAuthenticated:" + lastAuthenticated,
                    ODataUtils.validateDateTime(lastAuthenticated));
        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Account", "'" + accountName + "'", -1);

            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);

            // Account削除
            AccountUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * RoleからNP経由でLastAuthenticatedに予約語以外の文字列を指定したAccountの登録ができないこと.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public void RoleからNP経由でLastAuthenticatedに予約語以外の文字列を指定したAccountの登録ができないこと() throws ParseException {
        String roleName = "AccountViaNPTestRole";
        String accountName = "AccountViaNPTestAccount";

        try {
            // Role作成
            RoleUtils.create(CELL_NAME, MASTER_TOKEN_NAME, roleName, HttpStatus.SC_CREATED);

            // Role-AccountNP経由登録
            String body = "{\"Name\":\"" + accountName + "\", \"LastAuthenticated\":\"SYSUTCDATE()\"}";
            TResponse res = AccountUtils.createViaNPNonCredential(CELL_NAME, MASTER_TOKEN_NAME, "Role", roleName, body,
                    HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("LastAuthenticated").getMessage());
        } finally {
            // $links削除
            RoleUtils.deleteLink(CELL_NAME, MASTER_TOKEN_NAME, RoleUtils.keyString(roleName),
                    "Account", "'" + accountName + "'", -1);
            // Role削除
            RoleUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, null, roleName, -1);
            // Account削除
            AccountUtils.delete(CELL_NAME, MASTER_TOKEN_NAME, accountName, -1);
        }
    }

    /**
     * 最終ログイン時刻でNP経由での範囲検索ができることの確認.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public void 最終ログイン時刻でNP経由での範囲検索ができることの確認() throws ParseException {
        // 範囲検索クエリ用の時刻を取り出す
        TResponse res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, Setup.TEST_CELL1,
                "LastAuthenticatedListUser005");
        String lastAuthenticatedString1 = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                .get("results"))
                .get("LastAuthenticated");
        long lastAuthenticatedlong1 = UserDataListFilterTest.parseDateStringToLong(lastAuthenticatedString1);
        res = AccountUtils.get(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, Setup.TEST_CELL1,
                "LastAuthenticatedListUser015");
        String lastAuthenticatedString2 = (String) ((JSONObject) ((JSONObject) res.bodyAsJson().get("d"))
                .get("results"))
                .get("LastAuthenticated");
        long lastAuthenticatedlong2 = UserDataListFilterTest.parseDateStringToLong(lastAuthenticatedString2);

        // Accountの一覧検索
        // ・Nameが"LastAuthenticatedListUser"で始まる、かつ、
        // ・LastAuthenticatedがLastAuthenticatedListUser006～LastAuthenticatedListUser015の値の間のAccountを
        // ・LastAuthenticatedで昇順並び替え
        String query = "?\\$filter=startswith(Name,%27LastAuthenticatedListUser%27)+"
                + "and+LastAuthenticated+gt+" + lastAuthenticatedlong1 + "+"
                + "and+LastAuthenticated+le+" + lastAuthenticatedlong2
                + "&\\$orderby=LastAuthenticated&\\$inlinecount=allpages";
        res = AccountUtils.listViaNP(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "Role",
                "'accountListViaNPRoleName'", query);

        // レスポンスボディーチェック用expectの作成
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        for (int i = 6; i <= 15; i++) {
            String accountName = String.format("LastAuthenticatedListUser%3d", i);
            uri.put(accountName, UrlUtils.accountLinks(Setup.TEST_CELL1, accountName));
        }
        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        for (int i = 6; i <= 15; i++) {
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            String accountName = String.format("LastAuthenticatedListUser%3d", i);
            additional.put(accountName, additionalprop);
            additionalprop.put("Name", accountName);
        }

        ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, "CellCtl.Account", additional, "__id", 10, null);
    }
}

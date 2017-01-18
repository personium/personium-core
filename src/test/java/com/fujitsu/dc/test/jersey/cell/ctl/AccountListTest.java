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
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.UserDataListFilterTest;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Accountの一覧取得のIT.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AccountListTest extends JerseyTest {
    // セル再帰的削除を使うための設定
    // 現在の仕様では、filterを設定しないと参照ロックが残ってしまい、セルが削除できないため
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountListTest() {
        super(new WebAppDescriptor.Builder(AccountListTest.INIT_PARAMS).build());
    }

    /**
     * 最終ログイン時刻で範囲検索ができることの確認.
     */
    @Test
    public final void 最終ログイン時刻で範囲検索ができることの確認() {
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
        String query = "\\$filter=startswith(Name,%27LastAuthenticatedListUser%27)+"
                + "and+LastAuthenticated+gt+" + lastAuthenticatedlong1 + "+"
                + "and+LastAuthenticated+le+" + lastAuthenticatedlong2
                + "&\\$orderby=LastAuthenticated&\\$inlinecount=allpages";
        res = AccountUtils.list(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, query, HttpStatus.SC_OK);

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

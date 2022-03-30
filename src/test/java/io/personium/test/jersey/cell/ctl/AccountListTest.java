/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Accountの一覧取得のIT.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AccountListTest extends PersoniumTest {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public AccountListTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test to get account list.
     */
    @Test
    public final void get_account_list() {
        // Accountの一覧検索
        // ・Nameが"AccountListUser"で始まる
        // ・Nameで昇順並び替え
        String query = "\\$filter=startswith(Name,%27AccountListUser%27)&\\$orderby=Name&\\$inlinecount=allpages";
        TResponse res = AccountUtils.list(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, query, HttpStatus.SC_OK);

        // レスポンスボディーチェック用expectの作成
        // URI
        Map<String, String> uri = new HashMap<String, String>();
        for (int i = 1; i < 25; i++) {
            String accountName = String.format("AccountListUser%03d", i);
            uri.put(accountName, UrlUtils.accountLinks(Setup.TEST_CELL1, accountName));
        }
        // プロパティ
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        for (int i = 1; i < 25; i++) {
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            String accountName = String.format("AccountListUser%03d", i);
            additional.put(accountName, additionalprop);
            additionalprop.put("Name", accountName);
            if (i <= 10) {
                additionalprop.put("IPAddressRange", "192.1." + i + ".1");
            } else if (i <= 20) {
                additionalprop.put("IPAddressRange", "192.1." + i + ".0/24,192.2.2.254");
            } else {
                additionalprop.put("IPAddressRange", null);
            }
        }

        ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, "CellCtl.Account", additional, "__id", 25, null);
    }
}

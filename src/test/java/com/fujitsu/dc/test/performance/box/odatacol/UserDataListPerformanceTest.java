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
package com.fujitsu.dc.test.performance.box.odatacol;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Performance;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.AbstractUserDataTest;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserData一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Performance.class })
public class UserDataListPerformanceTest extends AbstractUserDataTest {

    private static final int USERDATA_COUNT = 1000;
    private static final String USERDATA_ID_PREFIX = "userdata";

    /**
     * コンストラクタ.
     */
    public UserDataListPerformanceTest() {
        super();
    }

    /**
     * ユーザデータの一覧を作成.
     * @param index インデックス番号
     * @return TResponse レスポンス情報
     */
    @SuppressWarnings("unchecked")
    public TResponse createData(int index) {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", USERDATA_ID_PREFIX + String.valueOf(index));
        body.put("dynamicProperty", "dynamicPropertyValue");
        body.put("secondDynamicProperty", "secondDynamicPropertyValue");
        body.put("nullProperty", null);
        body.put("intProperty", 123);
        body.put("floatProperty", 123.123);
        body.put("trueProperty", true);
        body.put("falseProperty", false);
        body.put("nullStringProperty", "null");
        body.put("intStringProperty", "123");
        body.put("floatStringProperty", "123.123");
        body.put("trueStringProperty", "true");
        body.put("falseStringProperty", "false");

        // ユーザデータ作成
        return createUserData(body, HttpStatus.SC_CREATED);
    }

    /**
     * ユーザデータの一覧を削除.
     * @param index インデックス番号
     */
    public void deleteData(int index) {
        deleteUserData(USERDATA_ID_PREFIX + String.valueOf(index));
    }

    /**
     * UserDataの一覧を正常に取得できること.
     */
    @Test
    public final void UserDataの一覧を正常に取得できること() {
        // リクエスト実行
        try {
            // ユーザデータ作成
            TResponse respons = null;
            Map<String, String> etagList = new HashMap<String, String>();
            for (int i = 0; i < USERDATA_COUNT; i++) {
                respons = createData(i);
                etagList.put(USERDATA_ID_PREFIX + String.valueOf(i), respons.getHeader(HttpHeaders.ETAG));
            }

            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", "Category")
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // パフォーマンスチェックが目的のため取得結果はチェックしない
        } finally {
            // ユーザデータ削除
            for (int i = 0; i < USERDATA_COUNT; i++) {
                deleteData(i);
            }
        }
    }
}

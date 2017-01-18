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
package com.fujitsu.dc.test.jersey.box.odatacol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DaoException;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataの特殊文字のレスポンスチェックのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataSpecificCharTest extends AbstractUserDataTest {

    /**
     * テスト用Fixture.
     */
    static class Fixture {
        String testComment;
        /**
         * 入力値.
         */
        String inputValue;
        /**
         * 期待する返却値.
         */
        String expectedReturnValue;

        /**
         * コンストラクタ.
         * @param testComment テスト内容
         * @param inputValue 入力値
         * @param expectedReturnValue 期待する返却値
         */
        Fixture(String testComment,
                String inputValue,
                String expectedReturnValue) {
            this.testComment = testComment;
            this.inputValue = inputValue;
            this.expectedReturnValue = expectedReturnValue;
        }
    }

    /**
     * 制御コードのエスケープテストパターンを作成.
     * @return テストパターン
     */
    public static Fixture[] getFixture() {
        Fixture[] datas = {
                new Fixture("\\u0008が\\bにエスケープされること", "\u0008", "\\b"),
                new Fixture("\\u0009が\\tにエスケープされること", "\u0009", "\\t"),
                new Fixture("\\u000Aが\\nにエスケープされること", "\n", "\\n"),
                new Fixture("\\u000Cが\\fにエスケープされること", "\u000C", "\\f"),
                new Fixture("\\u000Dが\\rにエスケープされること", "\r", "\\r"),
                new Fixture("\\u002Fが\\/にエスケープされること", "\u002F", "\\/"),
                new Fixture("\\u005Cが\\\\にエスケープされること", "\\", "\\\\")
        };
        return datas;
    }

    String cellName = "testcell1";
    String boxName = "box1";
    String colName = "setodata";
    String entityTypeName = "Category";
    String userDataId = "userdata001";

    /**
     * 特殊文字を含むUserDataの操作時にエスケープされて返却されること.
     * @throws DaoException ボディのパースに失敗
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void 特殊文字を含むUserDataの操作時にエスケープされて返却されること() throws DaoException {
        for (Fixture f : getFixture()) {

            // リクエスト実行
            try {
                // ユーザデータの作成
                JSONObject body = new JSONObject();
                body.put("__id", userDataId);
                body.put("testField", "value_\\" + f.inputValue + "_value"); // 0x0a(Ctl-A)
                TResponse res = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                        cellName, boxName, colName, entityTypeName);

                // レスポンスボディーのチェック
                String resBody = res.getBody();
                assertTrue(f.testComment, resBody.contains("value_" + f.expectedReturnValue + "_value"));
                assertFalse(f.testComment, resBody.contains("value_" + f.inputValue + "_value"));

                // ユーザデータの一件取得
                String url = String.format("%s/%s/%s/%s/%s('%s')",
                        UrlUtils.getBaseUrl(), cellName, boxName, colName, entityTypeName, userDataId);
                DcRequest request = DcRequest.get(url);
                request.header("Accept", "application/json");
                request.header("Authorization", "Bearer " + AbstractCase.MASTER_TOKEN_NAME);
                DcResponse dres = ODataCommon.request(request);
                assertEquals(f.testComment, dres.getStatusCode(), HttpStatus.SC_OK);

                // レスポンスボディーのチェック
                resBody = dres.bodyAsString();
                assertTrue(f.testComment, resBody.contains("value_" + f.expectedReturnValue + "_value"));
                assertFalse(f.testComment, resBody.contains("value_" + f.inputValue + "_value"));

                // ユーザデータの一覧取得
                url = String.format("%s/%s/%s/%s/%s",
                        UrlUtils.getBaseUrl(), cellName, boxName, colName, entityTypeName);
                DcRequest listRequest = DcRequest.get(url);
                listRequest.header("Accept", "application/json");
                listRequest.header("Authorization", "Bearer " + AbstractCase.MASTER_TOKEN_NAME);
                dres = ODataCommon.request(listRequest);
                assertEquals(f.testComment, dres.getStatusCode(), HttpStatus.SC_OK);

                // レスポンスボディーのチェック
                resBody = dres.bodyAsString();
                assertTrue(f.testComment, resBody.contains("value_" + f.expectedReturnValue + "_value"));
                assertFalse(f.testComment, resBody.contains("value_" + f.inputValue + "_value"));

            } finally {
                UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityTypeName,
                        userDataId);
            }
        }
    }
}

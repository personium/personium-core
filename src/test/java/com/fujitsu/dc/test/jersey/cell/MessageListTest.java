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
package com.fujitsu.dc.test.jersey.cell;

import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ReceivedMessageUtils;
import com.fujitsu.dc.test.utils.SentMessageUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * MessageAPIの検索テスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageListTest extends ODataCommon {

    private static final int MAX_INDEXING_SIZE = 4096;

    static final String TEST_CELL1 = Setup.TEST_CELL1;

    static final String MESSAGE = "message";
    static final String REQ_RELATION_BUILD = "req.relation.build";
    static final String REQ_RELATION_BREAK = "req.relation.break ";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Messageの完全一致検索_Indexingできる最大サイズのBodyを指定した場合検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの完全一致検索_Indexingできる最大サイズのBodyを指定した場合検索できること() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE);
        String query = "?\\$filter=Body+eq+%27" + messageBody + "%27";

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

    /**
     * Messageの完全一致検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと.<br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの完全一致検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE + 1);
        String query = "?\\$filter=Body+eq+%27" + messageBody + "%27";

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

    /**
     * Messageの範囲検索_Indexingできる最大サイズのBodyを指定した場合検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの範囲検索_Indexingできる最大サイズのBodyを指定した場合検索できること() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE);

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // gt
            String query = "?\\$filter=Body+gt+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE - 1) + "0" + "%27";
            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // ge
            query = "?\\$filter=Body+ge+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE) + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // lt
            query = "?\\$filter=Body+lt+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE - 1) + "9" + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // le
            query = "?\\$filter=Body+le+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE) + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

    /**
     * Messageの範囲検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと.<br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの範囲検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE + 1);

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // gt
            String query = "?\\$filter=Body+gt+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE) + "0" + "%27";
            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // ge
            query = "?\\$filter=Body+ge+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE + 1) + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // lt
            query = "?\\$filter=Body+lt+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE) + "9" + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // le
            query = "?\\$filter=Body+le+%27" + UserDataUtils.createString(MAX_INDEXING_SIZE + 1) + "%27";
            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

    /**
     * Messageの前方検索_Indexingできる最大サイズのBodyを指定した場合検索できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの前方一致検索_Indexingできる最大サイズのBodyを指定した場合検索できること() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE);
        String query = "?\\$filter=startswith(Body,%27" + messageBody + "%27)";

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

    /**
     * Messageの前方一致検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと.<br />
     * 0.19の既存IndexはMapping更新しない方針のため、ignore_above: 4096の設定が適用されない.<br />
     * （4096文字より大きい文字列はIndexingしないという修正が適用されない。<br />
     * このため、本テストがエラーとなるためIgnoreする。
     */
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageの前方一致検索_Indexingできる最大サイズプラス1のBodyを指定した場合検索でHitしないこと() {
        String messageBody = UserDataUtils.createString(MAX_INDEXING_SIZE + 1);
        String query = "?\\$filter=startswith(Body,%27" + messageBody + "%27)";

        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", messageBody);
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // 送信メッセージの一覧取得
            TResponse listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // 受信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, query, HttpStatus.SC_OK);
            // 取得件数のチェック
            results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    messageBody);
        }
    }

}

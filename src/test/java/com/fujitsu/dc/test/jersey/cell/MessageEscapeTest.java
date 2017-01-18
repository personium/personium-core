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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
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

/**
 * MessageAPIのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageEscapeTest extends ODataCommon {

    static final String TEST_CELL1 = Setup.TEST_CELL1;

    static final String MESSAGE = "message";
    static final String REQ_RELATION_BUILD = "req.relation.build";
    static final String REQ_RELATION_BREAK = "req.relation.break ";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageEscapeTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Titleに制御コードが含まれるメッセージを送信した場合エスケープされて取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Titleに制御コードが含まれるメッセージを送信した場合エスケープされて取得できること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "ti\\u0000tle");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse responseSentMessage = null;
        try {
            // メッセージ送信
            responseSentMessage = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            String responseStr = responseSentMessage.getBody();
            assertTrue(responseStr.contains("ti\\u0000tle"));
            assertFalse(responseStr.contains("ti\u0000tle"));

            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) responseSentMessage.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");

            // 送信メッセージ一件取得
            TResponse response = SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK, id);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("ti\\u0000tle"));
            assertFalse(responseStr.contains("ti\u0000tle"));

            // 送信メッセージ一覧取得
            response = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("ti\\u0000tle"));
            assertFalse(responseStr.contains("ti\u0000tle"));

            // 受信メッセージ一件取得
            response = ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCell, HttpStatus.SC_OK, id);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("ti\\u0000tle"));
            assertFalse(responseStr.contains("ti\u0000tle"));

            // 受信メッセージ一覧取得
            response = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, HttpStatus.SC_OK);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("ti\\u0000tle"));
            assertFalse(responseStr.contains("ti\u0000tle"));

        } finally {
            if (responseSentMessage != null) {
                deleteOdataResource(responseSentMessage.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE,
                    "ti\u0000tle",
                    "body");
        }
    }

    /**
     * Bodyに制御コードが含まれるメッセージを送信した場合エスケープされて取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Bodyに制御コードが含まれるメッセージを送信した場合エスケープされて取得できること() {
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
        body.put("Body", "bo\\u0000dy");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse responseSentMessage = null;
        try {
            // メッセージ送信
            responseSentMessage = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            String responseStr = responseSentMessage.getBody();
            assertTrue(responseStr.contains("bo\\u0000dy"));
            assertFalse(responseStr.contains("bo\u0000dy"));

            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) responseSentMessage.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");

            // 送信メッセージ一件取得
            TResponse response = SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK, id);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("bo\\u0000dy"));
            assertFalse(responseStr.contains("bo\u0000dy"));

            // 送信メッセージ一覧取得
            response = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("bo\\u0000dy"));
            assertFalse(responseStr.contains("bo\u0000dy"));

            // 受信メッセージ一件取得
            response = ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCell, HttpStatus.SC_OK, id);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("bo\\u0000dy"));
            assertFalse(responseStr.contains("bo\u0000dy"));

            // 受信メッセージ一覧取得
            response = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, targetCell, HttpStatus.SC_OK);
            // レスポンスボディのチェック
            responseStr = response.getBody();
            assertTrue(responseStr.contains("bo\\u0000dy"));
            assertFalse(responseStr.contains("bo\u0000dy"));

        } finally {
            if (responseSentMessage != null) {
                deleteOdataResource(responseSentMessage.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    "bo\u0000dy");
        }
    }

    /**
     * Toに制御コードが含まれるメッセージを送信した場合400となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Toに制御コードが含まれるメッセージを送信した場合400となること() {
        // 送信先CellUrl
        String targetCell = "ce\\u0000ll";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                            .params(UrlUtils.cellRoot(targetCell)).getMessage());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title",
                    "body");
        }
    }

    /**
     * RequestRelationに制御コードが含まれるメッセージを送信した場合400となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RequestRelationに制御コードが含まれるメッセージを送信した場合400となること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", REQ_RELATION_BUILD);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot("re\\u0000lation"));
        body.put("RequestRelationTarget", UrlUtils.cellRoot(targetCell));

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                            .params("RequestRelation").getMessage());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), REQ_RELATION_BUILD,
                    "title",
                    "body");
        }
    }

    /**
     * RequestRelationTargetに制御コードが含まれるメッセージを送信した場合400となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RequestRelationTargetに制御コードが含まれるメッセージを送信した場合400となること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", REQ_RELATION_BUILD);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCell));
        body.put("RequestRelationTarget", UrlUtils.cellRoot("re\\u0000lation"));

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);

            // レスポンスボディのチェック
            ODataCommon.checkErrorResponseBody(response,
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                            .params("RequestRelationTarget").getMessage());

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            SentMessageUtils.deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), REQ_RELATION_BUILD,
                    "title",
                    "body");
        }
    }
}

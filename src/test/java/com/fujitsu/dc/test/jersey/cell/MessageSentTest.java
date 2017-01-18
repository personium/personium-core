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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.ReceivedMessagePort;
import com.fujitsu.dc.core.model.ctl.SentMessage;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ReceivedMessageUtils;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.SentMessageUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * MessageAPIのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageSentTest extends ODataCommon {

    private static final String SENT_MESSAGE_TYPE = "CellCtl.SentMessage";

    static final String TEST_CELL1 = Setup.TEST_CELL1;

    static final String MESSAGE = "message";
    static final String REQ_RELATION_BUILD = "req.relation.build";
    static final String REQ_RELATION_BREAK = "req.relation.break ";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageSentTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Message送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Message送信できること() {
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
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCell));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCell));
            expected.put("ToRelation", null);
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", null);
            expected.put("RequestRelationTarget", null);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");

            // 受信メッセージの一件取得
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCell, HttpStatus.SC_OK, id);

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信で存在しないCellを指定した場合レスポンスボディにエラー情報が設定されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Message送信で存在しないCellを指定した場合レスポンスボディにエラー情報が設定されること() {
        // 送信先CellUrl
        String targetCell = "dummyCell";

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
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCell));
            // TODO 本来は"code":"404","Reason":"Cell not found."を期待
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_FORBIDDEN));
            expectedResult.put("Reason", "Unit user access required.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCell));
            expected.put("ToRelation", null);
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", null);
            expected.put("RequestRelationTarget", null);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信でToに複数のURLを指定した場合に送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Message送信でToに複数のURLを指定した場合に送信できること() {
        // 送信先CellUrl
        String targetCell1 = Setup.TEST_CELL_SCHEMA1;
        String targetCell2 = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell1) + "," + UrlUtils.cellRoot(targetCell2));
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
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");

            // 受信メッセージの取得
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCell1, HttpStatus.SC_OK, id);
            // 受信メッセージの取得
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCell2, HttpStatus.SC_OK, id);

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell1, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
            deleteReceivedMessage(targetCell2, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信でToで指定したCellが存在しない場合.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Message送信でToで指定したCellが存在しない場合() {
        // 送信先CellUrl
        String targetCell = "dummyCell";

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
                    body.toJSONString(), HttpStatus.SC_CREATED);

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * ToもToRelationも指定がなくてMessage送信が失敗すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ToもToRelationも指定がなくてMessage送信が失敗すること() {

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
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
            // エラーメッセージチェック
            String code = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode();
            String detail = SentMessage.P_TO.getName() + "," + SentMessage.P_TO_RELATION.getName();
            String message = DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail).getMessage();
            checkErrorResponse(response.bodyAsJson(), code, message);

        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * 送信メッセージの一覧取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 送信メッセージの一覧取得ができること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ1作成
        JSONObject body1 = new JSONObject();
        body1.put("BoxBound", false);
        body1.put("InReplyTo", null);
        body1.put("To", UrlUtils.cellRoot(targetCell));
        body1.put("ToRelation", null);
        body1.put("Type", MESSAGE);
        body1.put("Title", "title1");
        body1.put("Body", "Hello");
        body1.put("Priority", 3);
        body1.put("RequestRelation", null);
        body1.put("RequestRelationTarget", null);

        // リクエストボディ1作成
        JSONObject body2 = new JSONObject();
        body2.put("BoxBound", false);
        body2.put("InReplyTo", null);
        body2.put("To", UrlUtils.cellRoot(targetCell));
        body2.put("ToRelation", null);
        body2.put("Type", MESSAGE);
        body2.put("Title", "title2");
        body2.put("Body", "Good Bye");
        body2.put("Priority", 2);
        body2.put("RequestRelation", null);
        body2.put("RequestRelationTarget", null);

        TResponse response1 = null;
        TResponse response2 = null;
        TResponse listresponse = null;
        try {
            // メッセージ1送信
            response1 = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body1.toJSONString(), HttpStatus.SC_CREATED);
            // メッセージ2送信
            response2 = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body2.toJSONString(), HttpStatus.SC_CREATED);

            // 送信メッセージの一覧取得
            listresponse = SentMessageUtils.list(MASTER_TOKEN_NAME, TEST_CELL1,
                    HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            // TODO レスポンスボディのチェック
        } finally {
            if (response1 != null) {
                deleteOdataResource(response1.getLocationHeader());
            }
            if (response2 != null) {
                deleteOdataResource(response2.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title1", "Hello");
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title2", "Good Bye");
        }
    }

    /**
     * 送信メッセージの一件取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 送信メッセージの一件取得ができること() {
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
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);
            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // 送信メッセージの一件取得
            SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK, id);
            // TODO レスポンスボディのチェック
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * 送信メッセージの削除ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 送信メッセージの削除ができること() {
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
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        String id = null;
        try {
            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            id = (String) results.get("__id");
        } finally {
            if (response != null) {
                // メッセージの削除
                SentMessageUtils.delete(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_NO_CONTENT, id);
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }

    }

    /**
     * toRelationを使用してMessage送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void toRelationを使用してMessage送信できること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", "cellrelation");
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
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCell));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", null);
            expected.put("ToRelation", "cellrelation");
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", null);
            expected.put("RequestRelationTarget", null);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // __idの取得
            JSONObject result = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) result.get("__id");

            // 送信メッセージを一件取得してボディをチェック
            TResponse getSMResponse = SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK,
                    id);
            ODataCommon.checkResponseBody(getSMResponse.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // 受信メッセージを取得してボディをチェック
            TResponse listRMResponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, Setup.TEST_CELL2,
                    HttpStatus.SC_OK);
            JSONArray results = (JSONArray) ((JSONObject) listRMResponse.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());

            expected = new JSONObject();
            expected.put("Body", "body");
            expected.put("_Box.Name", null);
            expected.put("RequestRelation", null);
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Priority", 3);
            expected.put("Status", "unread");
            expected.put("RequestRelationTarget", null);
            expected.put("InReplyTo", null);
            expected.put("MulticastTo", null);
            expected.put("From", UrlUtils.cellRoot(Setup.TEST_CELL1));

            ODataCommon.checkResults((JSONObject) results.get(0), null, Common.EDM_NS_CELL_CTL + "."
                    + ReceivedMessagePort.EDM_TYPE_NAME, expected);
        } finally {
            // 送信メッセージの削除
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * ToとtoRelationに同じCellの値を使用して1件だけMessage送信されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ToとtoRelationに同じCellの値を使用して1件だけMessage送信されること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", "cellrelation");
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
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCell));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCell));
            expected.put("ToRelation", "cellrelation");
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", null);
            expected.put("RequestRelationTarget", null);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // __idの取得
            JSONObject result = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) result.get("__id");

            // 送信メッセージを一件取得してボディをチェック
            TResponse getSMResponse = SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK,
                    id);
            ODataCommon.checkResponseBody(getSMResponse.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);
        } finally {
            // 送信メッセージの削除
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * ToとtoRelationに異なるCellの値を使用して複数件Message送信されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ToとtoRelationに異なるCellの値を使用して複数件Message送信されること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell) + "," + UrlUtils.cellRoot("testcell999"));
        body.put("ToRelation", "cellrelation");
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
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // レスポンスボディのチェック
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCell));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONObject expectedResultNotFound = new JSONObject();
            expectedResultNotFound.put("To", UrlUtils.cellRoot("testcell999"));
            expectedResultNotFound.put("Code", Integer.toString(HttpStatus.SC_FORBIDDEN));
            expectedResultNotFound.put("Reason", "Unit user access required.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            expectedResults.add(expectedResultNotFound);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCell) + "," + UrlUtils.cellRoot("testcell999"));
            expected.put("ToRelation", "cellrelation");
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", null);
            expected.put("RequestRelationTarget", null);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // __idの取得
            JSONObject result = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) result.get("__id");

            // 送信メッセージを一件取得してボディをチェック
            TResponse getSMResponse = SentMessageUtils.get(MASTER_TOKEN_NAME, TEST_CELL1, HttpStatus.SC_OK,
                    id);
            ODataCommon.checkResponseBody(getSMResponse.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);
        } finally {
            // 送信メッセージの削除
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), MESSAGE, "title", "body");
        }
    }

    /**
     * 存在しないToRelationを指定した場合Message送信が失敗すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 存在しないToRelationを指定した場合Message送信が失敗すること() {

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", "norelation");
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
            // エラーメッセージチェック
            String code = DcCoreException.SentMessage.TO_RELATION_NOT_FOUND_ERROR.getCode();
            String message = DcCoreException.SentMessage.TO_RELATION_NOT_FOUND_ERROR.params("norelation").getMessage();
            checkErrorResponse(response.bodyAsJson(), code, message);

        } finally {
            if (response.getStatusCode() == HttpStatus.SC_CREATED) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * ExtCellに紐付いていないToRelationを指定した場合Message送信が失敗すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtCellに紐付いていないToRelationを指定した場合Message送信が失敗すること() {

        // Relation作成のリクエストボディ
        JSONObject relation = new JSONObject();
        String relationName = "noExtCellRelation";
        relation.put("Name", relationName);

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", relationName);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        TResponse response = null;
        try {
            // ExtCellに紐付いていないRelationを作成
            RelationUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, relation,
                    HttpStatus.SC_CREATED);

            // メッセージ送信
            response = SentMessageUtils.sent(MASTER_TOKEN_NAME, TEST_CELL1,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);
            // エラーメッセージチェック
            String code = DcCoreException.SentMessage.RELATED_EXTCELL_NOT_FOUND_ERROR.getCode();
            String message = DcCoreException.SentMessage.RELATED_EXTCELL_NOT_FOUND_ERROR.params("noExtCellRelation")
                    .getMessage();
            checkErrorResponse(response.bodyAsJson(), code, message);

        } finally {
            if (response.getStatusCode() == HttpStatus.SC_CREATED) {
                deleteOdataResource(response.getLocationHeader());
            }
            RelationUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, relationName, null, -1);
        }
    }

    /**
     * Boxに紐づいた送信Messageを作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Boxに紐づいた送信Messageを作成できること() {
        String targetCell = Setup.TEST_CELL2;
        String id = "MessageSendViaNPTest11111111111111111111111111";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", id);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", null);
        body.put("RequestRelationTarget", null);

        try {
            // メッセージ送信
            SentMessageUtils.sendViaNP(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    body.toString(), HttpStatus.SC_CREATED);
        } finally {
                SentMessageUtils.delete(Setup.MASTER_TOKEN_NAME, Setup.TEST_CELL1, -1, id);
        }
    }


    /**
     * 自動生成された受信メッセージの削除.
     * @param targetCell targetCell
     * @param fromCellUrl fromCellUrl
     * @param type type
     * @param title title
     * @param body body
     */
    public static void deleteReceivedMessage(String targetCell,
            String fromCellUrl,
            String type,
            String title,
            String body) {
        SentMessageUtils.deleteReceivedMessage(targetCell, fromCellUrl, type, title, body);
    }

}

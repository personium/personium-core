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
package io.personium.test.jersey.cell;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.SentMessage;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.ReceivedMessageUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.SentMessageUtils;
import io.personium.test.utils.TResponse;

/**
 * MessageAPIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageSentTest extends ODataCommon {

    private static final String SENT_MESSAGE_TYPE = "CellCtl.SentMessage";

    static final String TEST_CELL1 = Setup.TEST_CELL1;

    static final String MESSAGE = "message";
    static final String REQ_RELATION_BUILD = "req.relation.build";
    static final String REQ_RELATION_BREAK = "req.relation.break";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageSentTest() {
        super("io.personium.core.rs");
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
            String code = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode();
            String detail = SentMessage.P_TO.getName() + "," + SentMessage.P_TO_RELATION.getName();
            String message = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail).getMessage();
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
     * Normal test.
     * Send BoxBound message of type Message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_send_boxbound_message_of_type_message() {
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Create role
            RoleUtils.create(srcCellName, MASTER_TOKEN_NAME, "testRole001", HttpStatus.SC_CREATED);
            // Set acl to role
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", srcCellName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("role1", "testRole001")
                    .with("roleBaseUrl", UrlUtils.roleResource(srcCellName, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);
            // Set links account and role
            ResourceUtils.linkAccountRole(srcCellName, MASTER_TOKEN_NAME, "account4", null,
                    "testRole001", HttpStatus.SC_NO_CONTENT);

            // App auth
            TResponse authnRes = CellUtils.tokenAuthenticationWithTarget(appCellName, "account0",
                    "password0", srcCellName);
            String appToken = (String) authnRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            // authz
            TResponse authzRes = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", srcCellName)
                    .with("username", "account4")
                    .with("password", "password4")
                    .with("client_id", UrlUtils.cellRoot(appCellName))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String token = (String) authzRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ---------------
            // Execution
            // ---------------
            // Send message
            response = SentMessageUtils.sent(token, srcCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
            // Set expected response body
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCellName));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
            expected.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));
            expected.put("Result", expectedResults);
            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // Get message id
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // Verify that the sent message is saved
            SentMessageUtils.get(MASTER_TOKEN_NAME, srcCellName, HttpStatus.SC_OK, id);
            // Verify that the received message is saved
            TResponse receivedResponse = ReceivedMessageUtils.get(
                    MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
            // Verify that boxname is stored in the received message
            results = (JSONObject) ((JSONObject) receivedResponse.bodyAsJson().get("d")).get("results");
            assertThat((String) results.get("_Box.Name"), is(Setup.TEST_BOX1));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), MESSAGE, "title", "body");
            // Delete ExtCell and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    ExtCell.EDM_TYPE_NAME, "'" + PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(srcCellName)) + "'",
                    MASTER_TOKEN_NAME);
            // Delete Box and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    Box.EDM_TYPE_NAME, "Name='" + Setup.TEST_BOX1 + "'", MASTER_TOKEN_NAME);
            // Delete relation
            RelationUtils.delete(targetCellName, MASTER_TOKEN_NAME, targetRelationName, null, -1);
            // Delete Role and Account $links
            ResourceUtils.linkAccountRollDelete(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001");
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, null, "testRole001", -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RelationBuild.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_send_boxbound_message_of_type_relation_build() {
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", REQ_RELATION_BUILD);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Create role
            RoleUtils.create(srcCellName, MASTER_TOKEN_NAME, "testRole001", HttpStatus.SC_CREATED);
            // Set acl to role
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", srcCellName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("role1", "testRole001")
                    .with("roleBaseUrl", UrlUtils.roleResource(srcCellName, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);
            // Set links account and role
            ResourceUtils.linkAccountRole(srcCellName, MASTER_TOKEN_NAME, "account4", null,
                    "testRole001", HttpStatus.SC_NO_CONTENT);

            // App auth
            TResponse authnRes = CellUtils.tokenAuthenticationWithTarget(appCellName, "account0",
                    "password0", srcCellName);
            String appToken = (String) authnRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            // authz
            TResponse authzRes = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", srcCellName)
                    .with("username", "account4")
                    .with("password", "password4")
                    .with("client_id", UrlUtils.cellRoot(appCellName))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String token = (String) authzRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ---------------
            // Execution
            // ---------------
            // Send message
            response = SentMessageUtils.sent(token, srcCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
            // Set expected response body
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCellName));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", REQ_RELATION_BUILD);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
            expected.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));
            expected.put("Result", expectedResults);
            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // Get message id
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // Verify that the sent message is saved
            SentMessageUtils.get(MASTER_TOKEN_NAME, srcCellName, HttpStatus.SC_OK, id);
            // Verify that the received message is saved
            TResponse receivedResponse = ReceivedMessageUtils.get(
                    MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
            // Verify that boxname is stored in the received message
            results = (JSONObject) ((JSONObject) receivedResponse.bodyAsJson().get("d")).get("results");
            assertThat((String) results.get("_Box.Name"), is(Setup.TEST_BOX1));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), REQ_RELATION_BUILD, "title", "body");
            // Delete ExtCell and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    ExtCell.EDM_TYPE_NAME, "'" + PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(srcCellName)) + "'",
                    MASTER_TOKEN_NAME);
            // Delete Box and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    Box.EDM_TYPE_NAME, "Name='" + Setup.TEST_BOX1 + "'", MASTER_TOKEN_NAME);
            // Delete relation
            RelationUtils.delete(targetCellName, MASTER_TOKEN_NAME, targetRelationName, null, -1);
            // Delete Role and Account $links
            ResourceUtils.linkAccountRollDelete(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001");
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, null, "testRole001", -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RelationBreak.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_send_boxbound_message_of_type_relation_break() {
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", REQ_RELATION_BREAK);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Create role
            RoleUtils.create(srcCellName, MASTER_TOKEN_NAME, "testRole001", HttpStatus.SC_CREATED);
            // Set acl to role
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", srcCellName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("role1", "testRole001")
                    .with("roleBaseUrl", UrlUtils.roleResource(srcCellName, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);
            // Set links account and role
            ResourceUtils.linkAccountRole(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001",
                    HttpStatus.SC_NO_CONTENT);

            // App auth
            TResponse authnRes = CellUtils.tokenAuthenticationWithTarget(appCellName, "account0", "password0",
                    srcCellName);
            String appToken = (String) authnRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            // authz
            TResponse authzRes = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", srcCellName)
                    .with("username", "account4")
                    .with("password", "password4")
                    .with("client_id", UrlUtils.cellRoot(appCellName))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String token = (String) authzRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ---------------
            // Execution
            // ---------------
            // Send message
            response = SentMessageUtils.sent(token, srcCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
            // Set expected response body
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCellName));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_CREATED));
            expectedResult.put("Reason", "Created.");
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", REQ_RELATION_BREAK);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
            expected.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));
            expected.put("Result", expectedResults);
            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // Get message id
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // Verify that the sent message is saved
            SentMessageUtils.get(MASTER_TOKEN_NAME, srcCellName, HttpStatus.SC_OK, id);
            // Verify that the received message is saved
            TResponse receivedResponse = ReceivedMessageUtils.get(
                    MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
            // Verify that boxname is stored in the received message
            results = (JSONObject) ((JSONObject) receivedResponse.bodyAsJson().get("d")).get("results");
            assertThat((String) results.get("_Box.Name"), is(Setup.TEST_BOX1));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), REQ_RELATION_BREAK, "title", "body");
            // Delete ExtCell and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    ExtCell.EDM_TYPE_NAME, "'" + PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(srcCellName)) + "'",
                    MASTER_TOKEN_NAME);
            // Delete Box and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    Box.EDM_TYPE_NAME, "Name='" + Setup.TEST_BOX1 + "'", MASTER_TOKEN_NAME);
            // Delete relation
            RelationUtils.delete(targetCellName, MASTER_TOKEN_NAME, targetRelationName, null, -1);
            // Delete Role and Account $links
            ResourceUtils.linkAccountRollDelete(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001");
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, null, "testRole001", -1);
        }
    }

    /**
     * Error test.
     * Send BoxBound message of type Message.
     * Box corresponding to the schema does not exist on sender.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void error_send_boxbound_message_box_not_exists_on_sender() {
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = "testSchema001";

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Create cell
            CellUtils.create(appCellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account
            AccountUtils.create(MASTER_TOKEN_NAME, appCellName, "account0", "password0", HttpStatus.SC_CREATED);

            // Create role
            RoleUtils.create(srcCellName, MASTER_TOKEN_NAME, "testRole001", HttpStatus.SC_CREATED);
            // Set acl to role
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", srcCellName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("role1", "testRole001")
                    .with("roleBaseUrl", UrlUtils.roleResource(srcCellName, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);
            // Set links account and role
            ResourceUtils.linkAccountRole(srcCellName, MASTER_TOKEN_NAME, "account4", null,
                    "testRole001", HttpStatus.SC_NO_CONTENT);

            // App auth
            TResponse authnRes = CellUtils.tokenAuthenticationWithTarget(appCellName, "account0",
                    "password0", srcCellName);
            String appToken = (String) authnRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            // authz
            TResponse authzRes = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", srcCellName)
                    .with("username", "account4")
                    .with("password", "password4")
                    .with("client_id", UrlUtils.cellRoot(appCellName))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String token = (String) authzRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ---------------
            // Execution
            // ---------------
            // Send message
            response = SentMessageUtils.sent(token, srcCellName,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);

            // ---------------
            // Verification
            // ---------------
            // Check response body
            PersoniumCoreException exception = PersoniumCoreException.SentMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS
                    .params(UrlUtils.cellRoot(appCellName));
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), MESSAGE, "title", "body");
            // Delete Account
            AccountUtils.delete(appCellName, MASTER_TOKEN_NAME, "account0", -1);
            // Delete Cell
            CellUtils.delete(MASTER_TOKEN_NAME, appCellName, -1);
            // Delete ExtCell and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    ExtCell.EDM_TYPE_NAME, "'" + PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(srcCellName)) + "'",
                    MASTER_TOKEN_NAME);
            // Delete Box and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    Box.EDM_TYPE_NAME, "Name='" + Setup.TEST_BOX1 + "'", MASTER_TOKEN_NAME);
            // Delete relation
            RelationUtils.delete(targetCellName, MASTER_TOKEN_NAME, targetRelationName, null, -1);
            // Delete Role and Account $links
            ResourceUtils.linkAccountRollDelete(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001");
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, null, "testRole001", -1);
        }
    }

    /**
     * Error test.
     * Send BoxBound message of type Message.
     * Box corresponding to the schema does not exist on receiver.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void error_send_boxbound_message_box_not_exists_on_receiver() {
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = "testSchema001";

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);
        body.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Create cell
            CellUtils.create(appCellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            // Create account
            AccountUtils.create(MASTER_TOKEN_NAME, appCellName, "account0", "password0", HttpStatus.SC_CREATED);

            // Create box
            BoxUtils.createWithSchema(srcCellName, "testBox001", MASTER_TOKEN_NAME, UrlUtils.cellRoot(appCellName));

            // Create role
            RoleUtils.create(srcCellName, MASTER_TOKEN_NAME, "testRole001", HttpStatus.SC_CREATED);
            // Set acl to role
            Http.request("cell/acl-setting-cell-none-root.txt")
                    .with("url", srcCellName)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("role1", "testRole001")
                    .with("roleBaseUrl", UrlUtils.roleResource(srcCellName, null, "")).returns()
                    .statusCode(HttpStatus.SC_OK);
            // Set links account and role
            ResourceUtils.linkAccountRole(srcCellName, MASTER_TOKEN_NAME, "account4", null,
                    "testRole001", HttpStatus.SC_NO_CONTENT);

            // App auth
            TResponse authnRes = CellUtils.tokenAuthenticationWithTarget(appCellName, "account0",
                    "password0", srcCellName);
            String appToken = (String) authnRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);
            // authz
            TResponse authzRes = Http.request("authn/password-cl-cp.txt")
                    .with("remoteCell", srcCellName)
                    .with("username", "account4")
                    .with("password", "password4")
                    .with("client_id", UrlUtils.cellRoot(appCellName))
                    .with("client_secret", appToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            String token = (String) authzRes.bodyAsJson().get(OAuth2Helper.Key.ACCESS_TOKEN);

            // ---------------
            // Execution
            // ---------------
            // Send message
            response = SentMessageUtils.sent(token, srcCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
            // Set expected response body
            JSONObject expectedResult = new JSONObject();
            expectedResult.put("To", UrlUtils.cellRoot(targetCellName));
            expectedResult.put("Code", Integer.toString(HttpStatus.SC_BAD_REQUEST));
            PersoniumCoreException exception = PersoniumCoreException.ReceiveMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS
                    .params(UrlUtils.cellRoot(appCellName));
            expectedResult.put("Reason", exception.getMessage());
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", "testBox001");
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestRelation", UrlUtils.cellRoot(targetCellName) + "__relation/__/" + targetRelationName);
            expected.put("RequestRelationTarget", UrlUtils.cellRoot(srcCellName));
            expected.put("Result", expectedResults);
            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);

            // Get message id
            JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // Verify that the sent message is saved
            SentMessageUtils.get(MASTER_TOKEN_NAME, srcCellName, HttpStatus.SC_OK, id);
            // Verify that the received message is not saved
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_NOT_FOUND, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), MESSAGE, "title", "body");
            // Delete Box
            BoxUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testBox001", -1);
            // Delete Account
            AccountUtils.delete(appCellName, MASTER_TOKEN_NAME, "account0", -1);
            // Delete Cell
            CellUtils.delete(MASTER_TOKEN_NAME, appCellName, -1);
            // Delete ExtCell and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    ExtCell.EDM_TYPE_NAME, "'" + PersoniumCoreUtils.encodeUrlComp(UrlUtils.cellRoot(srcCellName)) + "'",
                    MASTER_TOKEN_NAME);
            // Delete Box and Relation $links
            ResourceUtils.linksDelete(targetCellName, Relation.EDM_TYPE_NAME, targetRelationName, Setup.TEST_BOX1,
                    Box.EDM_TYPE_NAME, "Name='" + Setup.TEST_BOX1 + "'", MASTER_TOKEN_NAME);
            // Delete relation
            RelationUtils.delete(targetCellName, MASTER_TOKEN_NAME, targetRelationName, null, -1);
            // Delete Role and Account $links
            ResourceUtils.linkAccountRollDelete(srcCellName, MASTER_TOKEN_NAME, "account4", null, "testRole001");
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, null, "testRole001", -1);
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
            String code = PersoniumCoreException.SentMessage.TO_RELATION_NOT_FOUND_ERROR.getCode();
            String message = PersoniumCoreException.SentMessage.TO_RELATION_NOT_FOUND_ERROR.params(
                    "norelation").getMessage();
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
            String code = PersoniumCoreException.SentMessage.RELATED_EXTCELL_NOT_FOUND_ERROR.getCode();
            String message = PersoniumCoreException.SentMessage.RELATED_EXTCELL_NOT_FOUND_ERROR.params(
                    "noExtCellRelation").getMessage();
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

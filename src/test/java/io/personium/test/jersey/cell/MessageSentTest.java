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

import static io.personium.core.model.ctl.Message.TYPE_MESSAGE;
import static io.personium.core.model.ctl.Message.TYPE_REQUEST;
import static io.personium.core.model.ctl.RequestObject.REQUEST_TYPE_RELATION_ADD;
import static io.personium.core.model.ctl.RequestObject.REQUEST_TYPE_RELATION_REMOVE;
import static io.personium.core.model.ctl.RequestObject.REQUEST_TYPE_ROLE_ADD;
import static io.personium.core.model.ctl.RequestObject.REQUEST_TYPE_ROLE_REMOVE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.rs.PersoniumCoreApplication;
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
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.ReceivedMessageUtils;
import io.personium.test.utils.RelationUtils;
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

    private static final String TEST_CELL1 = Setup.TEST_CELL1;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageSentTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Message送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Message送信できること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信で存在しないCellを指定した場合レスポンスボディにエラー情報が設定されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Message送信で存在しないCellを指定した場合レスポンスボディにエラー情報が設定されること() {
        // 送信先CellUrl
        String targetCell = "dummyCell";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("Result", expectedResults);

            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    SENT_MESSAGE_TYPE, expected);
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信でToに複数のURLを指定した場合に送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Message送信でToに複数のURLを指定した場合に送信できること() {
        // 送信先CellUrl
        String targetCell1 = Setup.TEST_CELL_SCHEMA1;
        String targetCell2 = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell1) + "," + UrlUtils.cellRoot(targetCell2));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            deleteReceivedMessage(targetCell1, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
            deleteReceivedMessage(targetCell2, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * Message送信でToで指定したCellが存在しない場合.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Message送信でToで指定したCellが存在しない場合() {
        // 送信先CellUrl
        String targetCell = "dummyCell";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * ToもToRelationも指定がなくてMessage送信が失敗すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ToもToRelationも指定がなくてMessage送信が失敗すること() {

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
    public void 送信メッセージの一覧取得ができること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ1作成
        JSONObject body1 = new JSONObject();
        body1.put("BoxBound", false);
        body1.put("InReplyTo", null);
        body1.put("To", UrlUtils.cellRoot(targetCell));
        body1.put("ToRelation", null);
        body1.put("Type", TYPE_MESSAGE);
        body1.put("Title", "title1");
        body1.put("Body", "Hello");
        body1.put("Priority", 3);

        // リクエストボディ1作成
        JSONObject body2 = new JSONObject();
        body2.put("BoxBound", false);
        body2.put("InReplyTo", null);
        body2.put("To", UrlUtils.cellRoot(targetCell));
        body2.put("ToRelation", null);
        body2.put("Type", TYPE_MESSAGE);
        body2.put("Title", "title2");
        body2.put("Body", "Good Bye");
        body2.put("Priority", 2);

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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title1", "Hello");
            // 自動生成された受信メッセージの削除
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title2", "Good Bye");
        }
    }

    /**
     * 送信メッセージの一件取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 送信メッセージの一件取得ができること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * 送信メッセージの削除ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 送信メッセージの削除ができること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }

    }

    /**
     * toRelationを使用してMessage送信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void toRelationを使用してMessage送信できること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", "cellrelation");
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            expected.put("Type", TYPE_MESSAGE);
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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * ToとtoRelationに同じCellの値を使用して1件だけMessage送信されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ToとtoRelationに同じCellの値を使用して1件だけMessage送信されること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", "cellrelation");
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * ToとtoRelationに異なるCellの値を使用して複数件Message送信されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ToとtoRelationに異なるCellの値を使用して複数件Message送信されること() {
        // 送信先CellUrl
        String targetCell = Setup.TEST_CELL2;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell) + "," + UrlUtils.cellRoot("testcell999"));
        body.put("ToRelation", "cellrelation");
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            deleteReceivedMessage(targetCell, UrlUtils.cellRoot(Setup.TEST_CELL1), TYPE_MESSAGE, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBuild.
     * RequestRelation is RelationClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_build_relationClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBuild.
     * RequestRelation is unit local RelationClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_build_unit_local_relationClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.unitLocalRelationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.unitLocalRelationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBuild.
     * RequestRelation is RelationName.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_build_relationName() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("Name", targetRelationName);
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", targetRelationName);
            expectedRequestObject.put("ClassUrl", null);
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Error test.
     * Send message of type RelationBuild.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_message_of_type_relation_build_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationUrl(appCellName, "box1", targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            PersoniumCoreException exception = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                    .params("RequestObjects.ClassUrl");
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBreak.
     * RequestRelation is RelationClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_break_relationClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBreak.
     * RequestRelation is unit local RelationClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_break_unit_local_relationClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.unitLocalRelationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.unitLocalRelationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RelationBreak.
     * RequestRelation is RelationName.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_relation_break_relationName() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("Name", targetRelationName);
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", targetRelationName);
            expectedRequestObject.put("ClassUrl", null);
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Error test.
     * Send message of type RelationBreak.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_message_of_type_relation_break_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationUrl(targetCellName, "box1", targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            PersoniumCoreException exception = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                    .params("RequestObjects.ClassUrl");
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleGrant.
     * RequestRelation is RoleClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_grant_roleClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleGrant.
     * RequestRelation is unit local RoleClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_grant_unit_local_roleClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.unitLocalRoleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.unitLocalRoleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleGrant.
     * RequestRelation is RoleName.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_grant_roleName() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("Name", targetRoleName);
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", targetRoleName);
            expectedRequestObject.put("ClassUrl", null);
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Error test.
     * Send message of type RoleGrant.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_message_of_type_role_grant_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleUrl(appCellName, "box1", targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            PersoniumCoreException exception = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                    .params("RequestObjects.ClassUrl");
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleRevoke.
     * RequestRelation is RoleClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_revoke_roleClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleRevoke.
     * RequestRelation is unit local RoleClassURL.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_revoke_unit_local_roleClassURL() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.unitLocalRoleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.unitLocalRoleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message of type RoleRevoke.
     * RequestRelation is RoleName.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_of_type_role_revoke_roleName() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("Name", targetRoleName);
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", targetRoleName);
            expectedRequestObject.put("ClassUrl", null);
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Error test.
     * Send message of type RoleRevoke.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_message_of_type_role_revoke_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleUrl(targetCellName, "box1", targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            PersoniumCoreException exception = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                    .params("RequestObjects.ClassUrl");
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send message multiple RequestObject.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_message_multiple_request_object() {
        String messageType = TYPE_REQUEST;
        String requestType1 = REQUEST_TYPE_RELATION_ADD;
        String requestType2 = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRelationName = "testRelation001";
        String targetRoleName = "testRole001";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject1 = new JSONObject();
        requestObject1.put("RequestType", requestType1);
        requestObject1.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
        requestObject1.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONObject requestObject2 = new JSONObject();
        requestObject2.put("RequestType", requestType2);
        requestObject2.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
        requestObject2.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject1);
        requestObjects.add(requestObject2);
        body.put("RequestObjects", requestObjects);

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            String token = MASTER_TOKEN_NAME;

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
            JSONObject expectedRequestObject1 = new JSONObject();
            expectedRequestObject1.put("RequestType", requestType1);
            expectedRequestObject1.put("Name", null);
            expectedRequestObject1.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
            expectedRequestObject1.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject1.put("EventSubject", null);
            expectedRequestObject1.put("EventType", null);
            expectedRequestObject1.put("EventObject", null);
            expectedRequestObject1.put("EventInfo", null);
            expectedRequestObject1.put("Action", null);
            JSONObject expectedRequestObject2 = new JSONObject();
            expectedRequestObject2.put("RequestType", requestType2);
            expectedRequestObject2.put("Name", null);
            expectedRequestObject2.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
            expectedRequestObject2.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject2.put("EventSubject", null);
            expectedRequestObject2.put("EventType", null);
            expectedRequestObject2.put("EventObject", null);
            expectedRequestObject2.put("EventInfo", null);
            expectedRequestObject2.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject1);
            expectedRequestObjects.add(expectedRequestObject2);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", null);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, id);
        } finally {
            // Delete sent message
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
            // Delete received message
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type Message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_boxbound_message_of_type_message() {
        String messageType = TYPE_MESSAGE;
        String targetCellName = Setup.TEST_CELL2;
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RelationBuild.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_boxbound_message_of_type_relation_build() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
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
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RelationBreak.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_boxbound_message_of_type_relation_break() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_REMOVE;
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
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.relationClassUrl(appCellName, targetRelationName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RoleGrant.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_boxbound_message_of_type_role_grant() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole002";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Normal test.
     * Send BoxBound message of type RoleRevoke.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_send_boxbound_message_of_type_role_revoke() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_REMOVE;
        String targetCellName = Setup.TEST_CELL2;
        String targetRoleName = "testRole002";
        String srcCellName = TEST_CELL1;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", messageType);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

        JSONObject requestObject = new JSONObject();
        requestObject.put("RequestType", requestType);
        requestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
        requestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
        JSONArray requestObjects = new JSONArray();
        requestObjects.add(requestObject);
        body.put("RequestObjects", requestObjects);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            JSONObject expectedRequestObject = new JSONObject();
            expectedRequestObject.put("RequestType", requestType);
            expectedRequestObject.put("Name", null);
            expectedRequestObject.put("ClassUrl", UrlUtils.roleClassUrl(appCellName, targetRoleName));
            expectedRequestObject.put("TargetUrl", UrlUtils.cellRoot(srcCellName));
            expectedRequestObject.put("EventSubject", null);
            expectedRequestObject.put("EventType", null);
            expectedRequestObject.put("EventObject", null);
            expectedRequestObject.put("EventInfo", null);
            expectedRequestObject.put("Action", null);
            JSONArray expectedRequestObjects = new JSONArray();
            expectedRequestObjects.add(expectedRequestObject);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", messageType);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
            expected.put("RequestObjects", expectedRequestObjects);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), messageType, "title", "body");
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Error test.
     * Send BoxBound message of type Message.
     * Box corresponding to the schema does not exist on sender.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_boxbound_message_box_not_exists_on_sender() {
        String targetCellName = Setup.TEST_CELL2;
        String srcCellName = TEST_CELL1;
        String appCellName = "testschema001";

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), TYPE_MESSAGE, "title", "body");
            // Delete Account
            AccountUtils.delete(appCellName, MASTER_TOKEN_NAME, "account0", -1);
            // Delete Cell
            CellUtils.delete(MASTER_TOKEN_NAME, appCellName, -1);
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * Error test.
     * Send BoxBound message of type Message.
     * Box corresponding to the schema does not exist on receiver.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_send_boxbound_message_box_not_exists_on_receiver() {
        String targetCellName = Setup.TEST_CELL2;
        String srcCellName = TEST_CELL1;
        String appCellName = "testschema001";

        // Set request body
        JSONObject body = new JSONObject();
        body.put("BoxBound", true);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCellName));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
            LinksUtils.createLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

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
            PersoniumCoreException exception = PersoniumCoreException.ReceivedMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS
                    .params(UrlUtils.cellRoot(appCellName));
            expectedResult.put("Reason", exception.getMessage());
            JSONArray expectedResults = new JSONArray();
            expectedResults.add(expectedResult);
            JSONObject expected = new JSONObject();
            expected.put("_Box.Name", "testBox001");
            expected.put("InReplyTo", null);
            expected.put("To", UrlUtils.cellRoot(targetCellName));
            expected.put("ToRelation", null);
            expected.put("Type", TYPE_MESSAGE);
            expected.put("Title", "title");
            expected.put("Body", "body");
            expected.put("Priority", 3);
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
            deleteReceivedMessage(targetCellName, UrlUtils.cellRoot(srcCellName), TYPE_MESSAGE, "title", "body");
            // Delete Box
            BoxUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testBox001", -1);
            // Delete Account
            AccountUtils.delete(appCellName, MASTER_TOKEN_NAME, "account0", -1);
            // Delete Cell
            CellUtils.delete(MASTER_TOKEN_NAME, appCellName, -1);
            // Delete Role and Account $links
            LinksUtils.deleteLinks(srcCellName, Account.EDM_TYPE_NAME, "account4", null,
                    Role.EDM_TYPE_NAME, "testRole001", null, MASTER_TOKEN_NAME, -1);
            // Delete role
            RoleUtils.delete(srcCellName, MASTER_TOKEN_NAME, "testRole001", null, -1);
        }
    }

    /**
     * 存在しないToRelationを指定した場合Message送信が失敗すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 存在しないToRelationを指定した場合Message送信が失敗すること() {

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", "norelation");
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
    public void ExtCellに紐付いていないToRelationを指定した場合Message送信が失敗すること() {

        // Relation作成のリクエストボディ
        JSONObject relation = new JSONObject();
        String relationName = "noExtCellRelation";
        relation.put("Name", relationName);

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("BoxBound", false);
        body.put("InReplyTo", null);
        body.put("ToRelation", relationName);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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
    public void Boxに紐づいた送信Messageを作成できること() {
        String targetCell = Setup.TEST_CELL2;
        String id = "MessageSendViaNPTest11111111111111111111111111";

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", id);
        body.put("InReplyTo", null);
        body.put("To", UrlUtils.cellRoot(targetCell));
        body.put("ToRelation", null);
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "title");
        body.put("Body", "body");
        body.put("Priority", 3);

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

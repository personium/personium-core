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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.ExtCell;
import com.fujitsu.dc.core.model.ctl.Relation;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ReceivedMessageUtils;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * メッセージ承認APIのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageApproveTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public MessageApproveTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * タイプがMessageのデータのメッセージ承認できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void タイプがMessageのデータのメッセージ承認できること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            DcRestAdapter rest;

            HashMap<String, String> requestheaders;
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            rest = new DcRestAdapter();
            res = null;

            // リクエストヘッダをセット
            requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"read\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            // 更新結果を取得
            checkMessageStatus(messageId, "read");
            // もう一度未読みする
            res = rest.post(requestUrl, "{\"Command\":\"unread\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            // 更新結果を取得
            checkMessageStatus(messageId, "unread");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * タイプがMessageの場合不正なIDでのメッセージ承認が404エラーになること.
     */
    @Test
    public final void タイプがMessageの場合不正なIDでのメッセージ承認が404エラーになること() {

        try {
            String messageId = "notfoundid123456789";

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            String requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"read\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.NO_SUCH_ENTITY.getCode());
        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * タイプがMessageのデータのメッセージ承認でボディが不正な場合400エラーになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void タイプがMessageのデータのメッセージ承認でボディが不正な場合400エラーになること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            DcRestAdapter rest;

            HashMap<String, String> requestheaders;
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            rest = new DcRestAdapter();
            res = null;

            // リクエストヘッダをセット
            requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // nullでメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":null }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // 空文字でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // 不正な文字列でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"ABCDE\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * タイプがMessageのデータのメッセージ承認でボディにapprovedやrejectedを指定した場合400エラーになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void タイプがMessageのデータのメッセージ承認でボディにapprovedやrejectedを指定した場合400エラーになること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            DcRestAdapter rest;

            HashMap<String, String> requestheaders;
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            rest = new DcRestAdapter();
            res = null;

            // リクエストヘッダをセット
            requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // approvedでメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // rejectedでメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録で既に存在するRelationを使用したメッセージ承認ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録で既に存在するRelationを使用したメッセージ承認ができること() {
        String relationName = "messageTestRelation";

        // Relationのリクエストボディ
        JSONObject relationBody = new JSONObject();
        relationBody.put("Name", relationName);

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // Relation登録
            RelationUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationBody, HttpStatus.SC_CREATED);
            // ExtCell登録
            ExtCellUtils.create(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"),
                    HttpStatus.SC_CREATED);

            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Relationが存在することの確認
            RelationUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, HttpStatus.SC_OK);

            // ExtCellが存在することの確認
            ExtCellUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"),
                    HttpStatus.SC_OK);

            // Relation-ExtCellの$links一覧取得
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.extCellResource(Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell")));
            checkRelationExtCellLinks(relationName, expectedUriList);

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "approved");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation-ExtCell $links削除
            ResourceUtils.linkExtCellRelationDelete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot("targetCell"), relationName, -1);
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"));
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録で存在しないRelationを使用したメッセージ承認ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録で存在しないRelationを使用したメッセージ承認ができること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            // __idの取得
            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Relationが存在することの確認
            RelationUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, HttpStatus.SC_OK);

            // ExtCellが存在することの確認
            ExtCellUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"),
                    HttpStatus.SC_OK);

            // Relation-ExtCellの$links一覧取得
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.extCellResource(Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell")));
            checkRelationExtCellLinks(relationName, expectedUriList);

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "approved");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation-ExtCell $links削除
            ResourceUtils.linkExtCellRelationDelete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot("targetCell"), relationName, -1);
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"));
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録で関係登録済みのRelationを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録で関係登録済みのRelationを指定した場合400エラーとなること() {
        String relationName = "messageTestRelation";

        // Relationのリクエストボディ
        JSONObject relationBody = new JSONObject();
        relationBody.put("Name", relationName);

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // Relation登録
            RelationUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationBody, HttpStatus.SC_CREATED);
            // ExtCell登録
            ExtCellUtils.create(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"),
                    HttpStatus.SC_CREATED);
            // Relation-ExtCell $links
            ResourceUtils.linksWithBody(Setup.TEST_CELL1, Relation.EDM_TYPE_NAME, relationName, null,
                    ExtCell.EDM_TYPE_NAME,
                    UrlUtils.cellCtl(Setup.TEST_CELL1, ExtCell.EDM_TYPE_NAME, UrlUtils.cellRoot("targetCell")),
                    MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT);

            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.ReceiveMessage.REQUEST_RELATION_EXISTS_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation-ExtCell $links削除
            ResourceUtils.linkExtCellRelationDelete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot("targetCell"), relationName, -1);
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"));
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録でrejectedを指定した場合Relationが作成されないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録でrejectedを指定した場合Relationが作成されないこと() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Relationが存在しないことの確認
            RelationUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, HttpStatus.SC_NOT_FOUND);

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "rejected");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録でCommandに不正な値を指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録でCommandに不正な値を指定した場合400エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // nullでメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":null }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // 空文字でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // 不正な文字列でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"ABCDE\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録でCommandにreadやunreadを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録でCommandにreadやunreadを指定した場合400エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // 空文字でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"read\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

            // 不正な文字列でメッセージ承認
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"unread\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録でStatusがapprovedのメッセージを承認して400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録でStatusがapprovedのメッセージを承認して400エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 再度メッセージ承認する
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation-ExtCell $links削除
            ResourceUtils.linkExtCellRelationDelete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot("targetCell"), relationName, -1);
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"));
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録でStatusがrejectedのメッセージを拒否して400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録でStatusがrejectedのメッセージを拒否して400エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ拒否する
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 再度メッセージ拒否する
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録で存在しないメッセージIDを指定した場合404エラーとなること.
     */
    @Test
    public final void 関係登録で存在しないメッセージIDを指定した場合404エラーとなること() {

        try {
            // 存在しない__idを設定
            String messageId = "dummyMessageId";

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // approved
            String requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            DcResponse res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.NO_SUCH_ENTITY.getCode());

            // rejected
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.OData.NO_SUCH_ENTITY.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * 不正なRequestRelationが指定されたメッセージを関係登録承認した場合409エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 不正なRequestRelationが指定されたメッセージを関係登録承認した場合409エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/('" + relationName + "')");
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(), DcCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係登録で不正なRelationTarget名を指定した場合409エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係登録で不正なRelationTarget名を指定した場合409エラーとなること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("('targetCell')"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR.getCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 関係削除のメッセージを承認できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係削除のメッセージを承認できること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.build");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;
        String breakLocationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            // __idの取得
            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // 関係削除用リクエストボディに変更する
            body.put("Type", "req.relation.break");
            body.put("__id", "12345678901234567890123456789013");

            // 関係削除メッセージ受信
            requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = createReceivedMessage(requestUrl, body);
            breakLocationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            // 関係削除メッセージを承認する
            messageId = getMessageId(res);
            rest = new DcRestAdapter();

            // リクエストヘッダをセット
            requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }", requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Relationが存在することの確認
            RelationUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, HttpStatus.SC_OK);

            // ExtCellが存在することの確認
            ExtCellUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"),
                    HttpStatus.SC_OK);

            // Relation-ExtCellの$links一覧取得
            TResponse linkResponse = getRelationExtCellLinks(relationName);
            JSONArray results = (JSONArray) ((JSONObject) linkResponse.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "approved");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // Relation-ExtCell $links削除
            ResourceUtils.linkExtCellRelationDelete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot("targetCell"), relationName, -1);
            // Relation削除
            RelationUtils.delete(Setup.TEST_CELL1, MASTER_TOKEN_NAME, relationName, null, -1);
            // ExtCell削除
            ExtCellUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1, UrlUtils.cellRoot("targetCell"));
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
            if (breakLocationHeader != null) {
                deleteOdataResource(breakLocationHeader);
            }
        }
    }

    /**
     * 存在しないRequestRelationの関係削除のメッセージを承認した場合に４０９が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 存在しないRequestRelationの関係削除のメッセージを承認した場合に４０９が返却されること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.break");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String breakLocationHeader = null;

        try {
            // 関係削除メッセージ受信
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            breakLocationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            // 関係削除メッセージを承認する
            String messageId = getMessageId(res);
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"approved\" }", requestheaders);
            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "none");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // 受信メッセージ削除
            if (breakLocationHeader != null) {
                deleteOdataResource(breakLocationHeader);
            }
        }
    }

    /**
     * 関係削除のメッセージを拒否できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 関係削除のメッセージを拒否できること() {
        String relationName = "messageTestRelation";

        // 受信メッセージのリクエストボディ
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "req.relation.break");
        body.put("Schema", true);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");
        body.put("RequestRelation", UrlUtils.cellRoot(Setup.TEST_CELL1) + "__relation/__/" + relationName);
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            // メッセージ受信を登録
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = createReceivedMessage(requestUrl, body);
            locationHeader = res.getFirstHeader(HttpHeaders.LOCATION);

            String messageId = getMessageId(res);

            // メッセージ承認にする
            DcRestAdapter rest = new DcRestAdapter();

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            requestUrl = UrlUtils.approvedMessage(Setup.TEST_CELL1, messageId);
            res = rest.post(requestUrl, "{\"Command\":\"rejected\" }",
                    requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // Statusが変更されていることを確認
            checkMessageStatus(messageId, "rejected");

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // 受信メッセージ削除
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    private DcResponse createReceivedMessage(String requestUrl, JSONObject body) throws DcException {
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        res = rest.post(requestUrl, body.toJSONString(),
                requestheaders);
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        return res;
    }

    private String getCellIssueToken(String targetCellUrl) {
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
        TransCellAccessToken token = new TransCellAccessToken(cellUrl, cellUrl,
                targetCellUrl, new ArrayList<Role>(), "");
        return token.toTokenString();
    }

    private void checkMessageStatus(String messageId, String expectedStatus) {
        TResponse tRes = ReceivedMessageUtils.get(MASTER_TOKEN_NAME,
                Setup.TEST_CELL1, HttpStatus.SC_OK, messageId);
        JSONObject results = (JSONObject) ((JSONObject) tRes.bodyAsJson().get("d")).get("results");
        String status = (String) results.get("Status");
        assertEquals(expectedStatus, status);
    }

    private String getMessageId(DcResponse res) {
        JSONObject results = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
        String messageId = (String) results.get("__id");
        return messageId;
    }

    private TResponse getRelationExtCellLinks(String relationName) {
        return Http.request("links-request-no-navkey.txt")
                .with("method", "GET")
                .with("token", MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("entitySet", "Relation")
                .with("key", "'" + relationName + "'")
                .with("navProp", "_ExtCell")
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
    }

    private void checkRelationExtCellLinks(String relationName, ArrayList<String> expectedUriList) {
        TResponse resList = getRelationExtCellLinks(relationName);

        // レスポンスボディのチェック
        checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
    }

}

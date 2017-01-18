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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.core.DcCoreAuthzException;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Account;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.ReceivedMessage;
import com.fujitsu.dc.core.model.ctl.ReceivedMessagePort;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DaoException;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.ReceivedMessageUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.SentMessageUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * MessageAPIのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageReceivedTest extends ODataCommon {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageReceivedTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Messageを受信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Messageを受信できること() {

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
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

            try {
                String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put("Body", "Body");
                expected.put("_Box.Name", null);
                expected.put("RequestRelation", null);
                expected.put("Type", "message");
                expected.put("Title", "Title");
                expected.put("Priority", 3);
                expected.put("Status", "unread");
                expected.put("RequestRelationTarget", null);
                expected.put("InReplyTo", null);
                expected.put("MulticastTo", null);
                expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");
                locationHeader = res.getFirstHeader("Location");
                JSONObject json = res.bodyAsJson();
                checkResponseBody(json, locationHeader, Common.EDM_NS_CELL_CTL + "."
                        + ReceivedMessagePort.EDM_TYPE_NAME, expected);
                System.out.println(json.toJSONString());
            } catch (DcException e) {
                e.printStackTrace();
            }

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * idなしのMessageを受信できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void idなしのMessageを受信できないこと() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(
                            ReceivedMessage.P_ID.getName()).getMessage());
        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * マスタートークンでMessage受信が４０３になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void マスタートークンでMessage受信が４０３になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(),
                    DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage());
        } catch (DcException e) {
            e.printStackTrace();
        }

    }

    /**
     * Authorizationヘッダ無しでMessage受信が４０１になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Authorizationヘッダ無しでMessage受信が４０１になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreAuthzException.AUTHORIZATION_REQUIRED.getCode(),
                    DcCoreAuthzException.AUTHORIZATION_REQUIRED.getMessage());
        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * Authorizationヘッダに不正文字でMessage受信が４０１になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Authorizationヘッダに不正文字でMessage受信が４０１になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer TEST");

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreAuthzException.TOKEN_PARSE_ERROR.getCode(),
                    DcCoreAuthzException.TOKEN_PARSE_ERROR.getMessage());
        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * SubjectがAccountのトークンでMessage受信が４０３になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void SubjectがAccountのトークンでMessage受信が４０３になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        TransCellAccessToken token = new TransCellAccessToken(cellUrl, cellUrl + "#account",
                targetCellUrl, new ArrayList<Role>(), "");

        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token.toTokenString());

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(),
                    DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage());

        } catch (DcException e) {
            e.printStackTrace();
        }

    }

    /**
     * メッセージタイプがmessageでStatusがnoneを指定して４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void メッセージタイプがmessageでStatusがnoneを指定して４００が返却されること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "none");

        DcRestAdapter rest = new DcRestAdapter();

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            DcResponse res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.P_STATUS.getName()).getMessage());
        } catch (DcException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schema指定ありでMessageを受信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Schema指定ありでMessageを受信できること() {

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
        body.put("RequestRelation", UrlUtils.cellRoot("appCell") + "__relation/__/+:xxx");
        body.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));

        String locationHeader = null;

        try {
            DcRestAdapter rest = new DcRestAdapter();
            DcResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

            try {
                String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put("Body", "Body");
                expected.put("_Box.Name", null);
                expected.put("Type", "req.relation.build");
                expected.put("Title", "Title");
                expected.put("Priority", 3);
                expected.put("Status", "none");
                expected.put("RequestRelation", UrlUtils.cellRoot("appCell") + "__relation/__/+:xxx");
                expected.put("RequestRelationTarget", UrlUtils.cellRoot("targetCell"));
                expected.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
                expected.put("MulticastTo", null);
                expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");
                locationHeader = res.getFirstHeader("Location");
                JSONObject json = res.bodyAsJson();
                checkResponseBody(json, locationHeader, Common.EDM_NS_CELL_CTL + "."
                        + ReceivedMessagePort.EDM_TYPE_NAME, expected);
                System.out.println(json.toJSONString());

            } catch (DcException e) {
                e.printStackTrace();
            }

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 受信メッセージの一覧取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージの一覧取得ができること() {

        DcRestAdapter rest1 = new DcRestAdapter();
        DcRestAdapter rest2 = new DcRestAdapter();
        DcResponse res1 = null;
        DcResponse res2 = null;

        // リクエストボディ1作成
        JSONObject body1 = new JSONObject();
        body1.put("__id", "12345678901234567890123456789012");
        body1.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body1.put("Type", "message");
        body1.put("Title", "Title1");
        body1.put("Body", "Hello");
        body1.put("Priority", 3);
        body1.put("Status", "unread");

        // リクエストボディ2作成
        JSONObject body2 = new JSONObject();
        body2.put("__id", "12345678901234567890123456789013");
        body2.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body2.put("Type", "message");
        body2.put("Title", "Title2");
        body2.put("Body", "Good Bye");
        body2.put("Priority", 2);
        body2.put("Status", "unread");

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        TResponse listresponse = null;
        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            // メッセージ1受信
            res1 = rest1.post(requestUrl, body1.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res1.getStatusCode());
            // メッセージ2受信
            res2 = rest2.post(requestUrl, body2.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res2.getStatusCode());
            // 送信メッセージの一覧取得
            listresponse = ReceivedMessageUtils.list(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    HttpStatus.SC_OK);
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) listresponse.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            // TODO レスポンスボディのチェック
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (res1 != null) {
                deleteOdataResource(res1.getFirstHeader("Location"));
            }
            if (res2 != null) {
                deleteOdataResource(res2.getFirstHeader("Location"));
            }

        }
    }

    /**
     * 受信メッセージの一件取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージの一件取得ができること() {

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title1");
        body.put("Body", "Hello");
        body.put("Priority", 3);
        body.put("Status", "unread");

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            // メッセージ受信
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            String id = (String) results.get("__id");
            // 送信メッセージの一件取得
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, Setup.TEST_CELL1, HttpStatus.SC_OK, id);
            // TODO レスポンスボディのチェック
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (res != null) {
                deleteOdataResource(res.getFirstHeader("Location"));
            }
        }
    }

    /**
     * 受信メッセージの削除ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージの削除ができること() {

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        String id = null;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title1");
        body.put("Body", "Hello");
        body.put("Priority", 3);
        body.put("Status", "unread");

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            // メッセージ受信
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
            // __idの取得
            JSONObject results = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            id = (String) results.get("__id");
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (res != null) {
                // メッセージの削除
                ReceivedMessageUtils.delete(MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                        HttpStatus.SC_NO_CONTENT, id);
            }
        }
    }

    /**
     * 受信メッセージとアカウントがリンクできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージとアカウントがリンクできること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            DcResponse messageResponse = createReceivedMessage();
            locationHeader = messageResponse.getFirstHeader(HttpHeaders.LOCATION);
            messageId = getId(messageResponse);

            // $links作成
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject body = new JSONObject();
            body.put("uri", UrlUtils.cellCtl(Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName));
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // $links取得
            rest = new DcRestAdapter();
            res = null;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new DcRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (DcException e) {
                    e.printStackTrace();
                }
                assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            }
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 受信メッセージのlink作成時URLのNP名が_Accountの場合に400となること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージのlink作成時URLのNP名が_Accountの場合に400となること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            DcResponse messageResponse = createReceivedMessage();
            locationHeader = messageResponse.getFirstHeader(HttpHeaders.LOCATION);
            messageId = getId(messageResponse);

            // $links作成
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    "_Account");
            JSONObject body = new JSONObject();
            body.put("uri", UrlUtils.cellCtl(Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName));
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * アカウントと受信メッセージがリンクできること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void アカウントと受信メッセージがリンクできること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            DcResponse messageResponse = createReceivedMessage();
            locationHeader = messageResponse.getFirstHeader(HttpHeaders.LOCATION);
            messageId = getId(messageResponse);

            // $links作成
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            JSONObject body = new JSONObject();
            body.put("uri", UrlUtils.cellCtl(Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId));
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // $links取得
            rest = new DcRestAdapter();
            res = null;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName,
                        Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE, messageId);
                try {
                    rest = new DcRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (DcException e) {
                    e.printStackTrace();
                }
                assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            }
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ReadMessageに対するアクセス権があるアカウントで受信メッセージとアカウントのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void ReadMessageに対するアクセス権があるアカウントで受信メッセージとアカウントのリンクが作成できる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account11";
        String receiveAccountPassword = "password11";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME, messageSentCell,
                    message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // 受信メッセージとアカウントのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell, Account.EDM_TYPE_NAME,
                    receiveAccountName));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links取得
            try {
                DcRestAdapter restAdapterForGetLink = new DcRestAdapter();
                DcResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(
                        requestUrl, requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    DcRestAdapter restAdapterForDeleteLink = new DcRestAdapter();
                    DcResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl, requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (DcException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * 権限ALLのアカウントで受信メッセージとアカウントのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void 権限ALLのアカウントで受信メッセージとアカウントのリンクが作成できる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account20";
        String receiveAccountPassword = "password20";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME,
                    messageSentCell, message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // 受信メッセージとアカウントのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell, Account.EDM_TYPE_NAME,
                    receiveAccountName));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links取得
            try {
                DcRestAdapter restAdapterForGetLink = new DcRestAdapter();
                DcResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    DcRestAdapter restAdapterForDeleteLink = new DcRestAdapter();
                    DcResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl, requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (DcException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権がないアカウントで受信メッセージとアカウントのリンク作成がエラー(403)になる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void ReadMessageに対するアクセス権がないアカウントで受信メッセージとアカウントのリンク作成がエラー403になる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account1";
        String receiveAccountPassword = "password1";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME, messageSentCell,
                    message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // 受信メッセージとアカウントのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell, Account.EDM_TYPE_NAME,
                    receiveAccountName));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_FORBIDDEN, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権があるアカウントでアカウントと受信メッセージのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void ReadMessageに対するアクセス権があるアカウントでアカウントと受信メッセージのリンクが作成できる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account11";
        String receiveAccountPassword = "password11";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME,
                    messageSentCell, message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // アカウントと受信メッセージのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell,
                    Account.EDM_TYPE_NAME, receiveAccountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME,
                    messageId));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links取得
            try {
                DcRestAdapter restAdapterForGetLink = new DcRestAdapter();
                DcResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    DcRestAdapter restAdapterForDeleteLink = new DcRestAdapter();
                    DcResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl, requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (DcException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * 権限ALLのアカウントでアカウントと受信メッセージのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void 権限ALLのアカウントでアカウントと受信メッセージのリンクが作成できる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account20";
        String receiveAccountPassword = "password20";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME,
                    messageSentCell, message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // アカウントと受信メッセージのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell,
                    Account.EDM_TYPE_NAME, receiveAccountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell,
                    ReceivedMessage.EDM_TYPE_NAME, messageId));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links取得
            try {
                DcRestAdapter restAdapterForGetLink = new DcRestAdapter();
                DcResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    DcRestAdapter restAdapterForDeleteLink = new DcRestAdapter();
                    DcResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl, requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (DcException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権がないアカウントでアカウントと受信メッセージのリンク作成がエラー(403)になる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void ReadMessageに対するアクセス権がないアカウントでアカウントと受信メッセージのリンク作成がエラー403になる() {
        TResponse response1 = null;
        String messageSentCell = Setup.TEST_CELL2;
        String messageReceivedCell = Setup.TEST_CELL1;
        String receiveAccountName = "account1";
        String receiveAccountPassword = "password1";

        // CellレベルACL設定
        String aclTestFile = "cell/acl-authtest.txt";
        DavResourceUtils.setACL(messageReceivedCell, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_OK, "", aclTestFile, "", "");

        // メッセージ受信アカウントのトークンを取得
        String accessToken = ResourceUtils.getMyCellLocalToken(messageReceivedCell,
                receiveAccountName, receiveAccountPassword);

        try {
            JSONObject message = new JSONObject();
            message.put("To", UrlUtils.cellRoot(messageReceivedCell));
            message.put("Title", "test mail");
            message.put("Body", "test body01");

            // メッセージ送信
            response1 = SentMessageUtils.sent(AbstractCase.MASTER_TOKEN_NAME,
                    messageSentCell, message.toJSONString(), HttpStatus.SC_CREATED);

            // 受信メッセージ一覧取得
            TResponse listResponse = ReceivedMessageUtils.list(AbstractCase.MASTER_TOKEN_NAME,
                    messageReceivedCell, HttpStatus.SC_OK);
            JSONObject body = listResponse.bodyAsJson();
            JSONArray results = (JSONArray) ((JSONObject) body.get("d")).get("results");
            String messageId = (String) ((JSONObject) results.get(results.size() - 1)).get("__id");

            HashMap<String, String> requestheaders = new HashMap<String, String>();
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            // アカウントと受信メッセージのリンク
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    messageReceivedCell,
                    Account.EDM_TYPE_NAME, receiveAccountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            JSONObject bodyForCreateLink = new JSONObject();
            bodyForCreateLink.put("uri", UrlUtils.cellCtl(messageReceivedCell,
                    ReceivedMessage.EDM_TYPE_NAME, messageId));

            try {
                DcRestAdapter restAdapterForCreateLink = new DcRestAdapter();
                DcResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_FORBIDDEN, linkCreateResponse.getStatusCode());
            } catch (DcException ex) {
                fail();
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), "message", "test mail", "test body01");
        }
    }

    /**
     * NavigationProperty経由で受信メッセージとアカウント情報が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void NavigationProperty経由で受信メッセージとアカウント情報が取得できること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            DcResponse messageResponse = createReceivedMessage();
            locationHeader = messageResponse.getFirstHeader(HttpHeaders.LOCATION);
            messageId = getId(messageResponse);

            // $links作成
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject body = new JSONObject();
            body.put("uri", UrlUtils.cellCtl(Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName));
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // ReceivedMessage基準 NavigationProperty経由
            rest = new DcRestAdapter();
            requestUrl = UrlUtils.cellCtlNagvigationProperty(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // Account基準 NavigationProperty経由
            rest = new DcRestAdapter();
            requestUrl = UrlUtils.cellCtlNagvigationProperty(
                    Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new DcRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (DcException e) {
                    e.printStackTrace();
                }
                assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            }
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * 受信メッセージとアカウント情報の一覧取得で$expandを使用してデータが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 受信メッセージとアカウント情報の一覧取得で$expandを使用してデータが取得できること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            DcResponse messageResponse = createReceivedMessage();
            locationHeader = messageResponse.getFirstHeader(HttpHeaders.LOCATION);
            messageId = getId(messageResponse);

            // $links作成
            String requestUrl = UrlUtils.cellCtlLinksMulti(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            JSONObject body = new JSONObject();
            body.put("uri", UrlUtils.cellCtl(Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName));
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // ReceivedMessage取得
            rest = new DcRestAdapter();
            requestUrl = UrlUtils.cellCtl(Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME)
                    + "?$format=json&$expand=" + ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            try {
                System.out.println(res.bodyAsString());
            } catch (DaoException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // Account取得
            rest = new DcRestAdapter();
            requestUrl = UrlUtils.cellCtl(Setup.TEST_CELL1, Account.EDM_TYPE_NAME)
                    + "?$format=json&$expand=" + Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            try {
                System.out.println(res.bodyAsString());
            } catch (DaoException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (DcException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new DcRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (DcException e) {
                    e.printStackTrace();
                }
                assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
            }
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * Boxに紐づいた受信Messageを作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Boxに紐づいた受信Messageを作成できること() {

        String id = "12345678901234567890123456789012";
        JSONObject body = new JSONObject();
        body.put("__id", id);
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        try {
            ReceivedMessageUtils.receiveViaNP(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    body.toString(), HttpStatus.SC_CREATED);
        } finally {
            ReceivedMessageUtils.delete(Setup.MASTER_TOKEN_NAME, Setup.TEST_CELL1, -1, id);
        }
    }

    private String getId(DcResponse response) {
        JSONObject result = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        return (String) result.get("__id");
    }

    @SuppressWarnings("unchecked")
    private DcResponse createReceivedMessage() {
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } catch (DcException e) {
            e.printStackTrace();
        }
        return res;
    }

    private String getCellIssueToken(String targetCellUrl) {
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
        TransCellAccessToken token = new TransCellAccessToken(cellUrl, cellUrl,
                targetCellUrl, new ArrayList<Role>(), "");
        return token.toTokenString();
    }
}

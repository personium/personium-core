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

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.ctl.ReceivedMessage;
import com.fujitsu.dc.core.model.ctl.SentMessage;
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
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * MessageAPIのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageMethodNotAllowTest extends ODataCommon {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageMethodNotAllowTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * KeyなしRecivedMessageのPOSTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしRecivedMessageのPOSTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyなしRecivedMessageのPUTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしRecivedMessageのPUTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1);
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyなしRecivedMessageのMERGEメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしRecivedMessageのMERGEメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            // マージリクエスト実行
            String requestUrl = ReceivedMessage.EDM_TYPE_NAME;
            TResponse res = mergeRequest(requestUrl, body);
            res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * KeyありRecivedMessageのPOSTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありRecivedMessageのPOSTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyありRecivedMessageのPUTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありRecivedMessageのPUTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyありRecivedMessageのMERGEメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありRecivedMessageのMERGEメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            // マージリクエスト実行
            String requestUrl = ReceivedMessage.EDM_TYPE_NAME + "('MessageId')";
            TResponse res = mergeRequest(requestUrl, body);
            res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * KeyなしSentMessageのPOSTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしSentMessageのPOSTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyなしSentMessageのPUTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしSentMessageのPUTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1);
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyなしSentMessageのMERGEメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyなしSentMessageのMERGEメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            // マージリクエスト実行
            String requestUrl = SentMessage.EDM_TYPE_NAME;
            TResponse res = mergeRequest(requestUrl, body);
            res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * KeyありSentMessageのPOSTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありSentMessageのPOSTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyありSentMessageのPUTメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありSentMessageのPUTメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
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
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
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
     * KeyありSentMessageのMERGEメソッドが405エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void KeyありSentMessageのMERGEメソッドが405エラーとなること() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", "message");
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        String locationHeader = null;

        try {
            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());

            // マージリクエスト実行
            String requestUrl = SentMessage.EDM_TYPE_NAME + "('MessageId')";
            TResponse res = mergeRequest(requestUrl, body);
            res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    private TResponse mergeRequest(String path, JSONObject updateReqBody) {
        return mergeRequest(path, "*", updateReqBody);
    }
    private TResponse mergeRequest(String path, String ifMatch, JSONObject updateReqBody) {
        return Http.request("cell/merge.txt")
                .with("cell", Setup.TEST_CELL1)
                .with("path", path)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", ifMatch)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", updateReqBody.toJSONString())
                .returns()
                .debug();
    }
}

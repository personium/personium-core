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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * MessageAPIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageMethodNotAllowTest extends ODataCommon {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageMethodNotAllowTest() {
        super(new PersoniumCoreApplication());
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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1);
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.receivedMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1);
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1);
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.post(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            PersoniumRestAdapter rest = new PersoniumRestAdapter();
            PersoniumResponse res = null;

            // リクエストヘッダをセット
            HashMap<String, String> requestheaders = new HashMap<String, String>();
            // TODO Authorizationヘッダ
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

            try {
                String requestUrl = UrlUtils.sentMessageCtl(Setup.TEST_CELL1, "MessageId");
                res = rest.put(requestUrl, body.toJSONString(),
                        requestheaders);
                assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
            } catch (PersoniumException e) {
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
            requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

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
                .with("token", PersoniumUnitConfig.getMasterToken())
                .with("body", updateReqBody.toJSONString())
                .returns()
                .debug();
    }
}

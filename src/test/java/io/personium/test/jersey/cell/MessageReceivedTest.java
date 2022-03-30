/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
import static io.personium.core.model.ctl.RequestObject.REQUEST_TYPE_ROLE_ADD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DaoException;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.setup.Setup;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.ReceivedMessageUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.SentMessageUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * MessageAPIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MessageReceivedTest extends ODataCommon {

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public MessageReceivedTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Messageを受信できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Messageを受信できること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
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
                expected.put("Type", TYPE_MESSAGE);
                expected.put("Title", "Title");
                expected.put("Priority", 3);
                expected.put("Status", "unread");
                expected.put("InReplyTo", null);
                expected.put("MulticastTo", null);
                expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");
                locationHeader = res.getFirstHeader("Location");
                JSONObject json = res.bodyAsJson();
                checkResponseBody(json, locationHeader, Common.EDM_NS_CELL_CTL + "."
                        + ReceivedMessagePort.EDM_TYPE_NAME, expected);
                System.out.println(json.toJSONString());
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
     * idなしのMessageを受信できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void idなしのMessageを受信できないこと() {

        JSONObject body = new JSONObject();
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

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
                    PersoniumCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    PersoniumCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(
                            ReceivedMessage.P_ID.getName()).getMessage());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
    }

    /**
     * マスタートークンでMessage受信が４０３になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void マスタートークンでMessage受信が４０３になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + PersoniumUnitConfig.getMasterToken());

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(),
                    PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }

    }

    /**
     * Authorizationヘッダ無しでMessage受信が４０１になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Authorizationヘッダ無しでMessage受信が４０１になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getCode(),
                    PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getMessage());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
    }

    /**
     * Authorizationヘッダに不正文字でMessage受信が４０１になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Authorizationヘッダに不正文字でMessage受信が４０１になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

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
                    PersoniumCoreAuthzException.TOKEN_PARSE_ERROR.getCode(),
                    PersoniumCoreAuthzException.TOKEN_PARSE_ERROR.getMessage());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
    }

    /**
     * SubjectがAccountのトークンでMessage受信が４０３になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void SubjectがAccountのトークンでMessage受信が４０３になること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        TransCellAccessToken token = new TransCellAccessToken(cellUrl, cellUrl + "#account",
                targetCellUrl, new ArrayList<Role>(), "", null);

        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + token.toTokenString());

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_FORBIDDEN, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(),
                    PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage());

        } catch (PersoniumException e) {
            e.printStackTrace();
        }

    }

    /**
     * メッセージタイプがmessageでStatusがnoneを指定して４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void メッセージタイプがmessageでStatusがnoneを指定して４００が返却されること() {

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "none");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            PersoniumResponse res = rest.post(requestUrl, body.toJSONString(),
                    requestheaders);
            assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
            checkErrorResponse(res.bodyAsJson(),
                    PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.P_STATUS.getName()).getMessage());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
    }

    /**
     * Normal test.
     * Received message of type relation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_received_message_of_type_relation() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL1;
        String targetRelationName = "testRelation001";
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", null);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");

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
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
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
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("Body", "Body");
            expected.put("_Box.Name", null);
            expected.put("Type", messageType);
            expected.put("Title", "Title");
            expected.put("Priority", 3);
            expected.put("Status", "none");
            expected.put("RequestObjects", expectedRequestObjects);
            expected.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
            expected.put("MulticastTo", null);
            expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");

            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    "CellCtl.ReceivedMessage", expected);
            // Verify that the received message is saved
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, (String) body.get("__id"));
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * Error test.
     * Received message of type relation.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_received_message_of_type_relation_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_RELATION_ADD;
        String targetCellName = Setup.TEST_CELL1;
        String targetRelationName = "testRelation001";
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", null);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");

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
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
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
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * Normal test.
     * Received message of type role.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_received_message_of_type_role() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL1;
        String targetRoleName = "testRole001";
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", null);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");

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
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
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
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("Body", "Body");
            expected.put("_Box.Name", null);
            expected.put("Type", messageType);
            expected.put("Title", "Title");
            expected.put("Priority", 3);
            expected.put("Status", "none");
            expected.put("RequestObjects", expectedRequestObjects);
            expected.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
            expected.put("MulticastTo", null);
            expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");

            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    "CellCtl.ReceivedMessage", expected);
            // Verify that the received message is saved
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, (String) body.get("__id"));
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * Error test.
     * Received message of type role.
     * RequestRelation is invalid format.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_received_message_of_type_role_requestRelation_invalid_format() {
        String messageType = TYPE_REQUEST;
        String requestType = REQUEST_TYPE_ROLE_ADD;
        String targetCellName = Setup.TEST_CELL1;
        String targetRoleName = "testRole001";
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", null);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");

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
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
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
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * Normal test.
     * Received schema message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void normal_received_schema_message() {
        String messageType = TYPE_MESSAGE;
        String targetCellName = Setup.TEST_CELL1;
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = Setup.TEST_CELL_SCHEMA1;

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", UrlUtils.cellRoot(appCellName));
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "unread");

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
                    body.toJSONString(), HttpStatus.SC_CREATED);

            // ---------------
            // Verification
            // ---------------
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("Body", "Body");
            expected.put("_Box.Name", Setup.TEST_BOX1);
            expected.put("Type", messageType);
            expected.put("Title", "Title");
            expected.put("Priority", 3);
            expected.put("Status", "unread");
            expected.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
            expected.put("MulticastTo", null);
            expected.put("From", UrlUtils.getBaseUrl() + "/testcell2/");

            // Check response body
            ODataCommon.checkResponseBody(response.bodyAsJson(), response.getLocationHeader(),
                    "CellCtl.ReceivedMessage", expected);
            // Verify that the received message is saved
            ReceivedMessageUtils.get(MASTER_TOKEN_NAME, targetCellName, HttpStatus.SC_OK, (String) body.get("__id"));
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * Error test.
     * Received schema message of type Message.
     * Box corresponding to the schema does not exist.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void error_received_schema_message_box_not_exists() {
        String messageType = TYPE_MESSAGE;
        String targetCellName = Setup.TEST_CELL1;
        String srcCellName = Setup.TEST_CELL2;
        String appCellName = "testSchema001";

        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(srcCellName));
        body.put("Type", messageType);
        body.put("Schema", UrlUtils.cellRoot(appCellName));
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("InReplyTo", "d3330643f57a42fd854558fb0a96a96a");
        body.put("Priority", 3);
        body.put("Status", "none");

        TResponse response = null;
        try {
            // ---------------
            // Preparation
            // ---------------
            // Authorizationヘッダ
            String targetCellUrl = UrlUtils.cellRoot(targetCellName);

            // ---------------
            // Execution
            // ---------------
            response = ReceivedMessageUtils.receive(getCellIssueToken(targetCellUrl), targetCellName,
                    body.toJSONString(), HttpStatus.SC_BAD_REQUEST);

            // ---------------
            // Verification
            // ---------------
            // Check response body
            PersoniumCoreException exception = PersoniumCoreException.ReceivedMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS
                    .params(UrlUtils.cellRoot(appCellName));
            String message = (String) ((JSONObject) response.bodyAsJson().get("message")).get("value");
            assertThat(message, is(exception.getMessage()));
        } finally {
            if (response != null) {
                deleteOdataResource(response.getLocationHeader());
            }
        }
    }

    /**
     * 受信メッセージの一覧取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void 受信メッセージの一覧取得ができること() {

        PersoniumRestAdapter rest1 = new PersoniumRestAdapter();
        PersoniumRestAdapter rest2 = new PersoniumRestAdapter();
        PersoniumResponse res1 = null;
        PersoniumResponse res2 = null;

        // リクエストボディ1作成
        JSONObject body1 = new JSONObject();
        body1.put("__id", "12345678901234567890123456789012");
        body1.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body1.put("Type", TYPE_MESSAGE);
        body1.put("Title", "Title1");
        body1.put("Body", "Hello");
        body1.put("Priority", 3);
        body1.put("Status", "unread");

        // リクエストボディ2作成
        JSONObject body2 = new JSONObject();
        body2.put("__id", "12345678901234567890123456789013");
        body2.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body2.put("Type", TYPE_MESSAGE);
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
        } catch (PersoniumException e) {
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
    public void 受信メッセージの一件取得ができること() {

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
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
        } catch (PersoniumException e) {
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
    public void 受信メッセージの削除ができること() {

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        String id = null;

        // リクエストボディ作成
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
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
        } catch (PersoniumException e) {
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
    public void 受信メッセージとアカウントがリンクできること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            PersoniumResponse messageResponse = createReceivedMessage();
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
            rest = new PersoniumRestAdapter();
            res = null;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (PersoniumException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new PersoniumRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (PersoniumException e) {
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
    public void 受信メッセージのlink作成時URLのNP名が_Accountの場合に400となること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            PersoniumResponse messageResponse = createReceivedMessage();
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
        } catch (PersoniumException e) {
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
    public void アカウントと受信メッセージがリンクできること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            PersoniumResponse messageResponse = createReceivedMessage();
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
            rest = new PersoniumRestAdapter();
            res = null;
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (PersoniumException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName,
                        Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE, messageId);
                try {
                    rest = new PersoniumRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (PersoniumException e) {
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
    public void ReadMessageに対するアクセス権があるアカウントで受信メッセージとアカウントのリンクが作成できる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links取得
            try {
                PersoniumRestAdapter restAdapterForGetLink = new PersoniumRestAdapter();
                PersoniumResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(
                        requestUrl, requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    PersoniumRestAdapter restAdapterForDeleteLink = new PersoniumRestAdapter();
                    PersoniumResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl,
                            requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (PersoniumException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * 権限ALLのアカウントで受信メッセージとアカウントのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void 権限ALLのアカウントで受信メッセージとアカウントのリンクが作成できる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links取得
            try {
                PersoniumRestAdapter restAdapterForGetLink = new PersoniumRestAdapter();
                PersoniumResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    PersoniumRestAdapter restAdapterForDeleteLink = new PersoniumRestAdapter();
                    PersoniumResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl,
                            requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (PersoniumException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権がないアカウントで受信メッセージとアカウントのリンク作成がエラー(403)になる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void ReadMessageに対するアクセス権がないアカウントで受信メッセージとアカウントのリンク作成がエラー403になる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_FORBIDDEN, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権があるアカウントでアカウントと受信メッセージのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void ReadMessageに対するアクセス権があるアカウントでアカウントと受信メッセージのリンクが作成できる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links取得
            try {
                PersoniumRestAdapter restAdapterForGetLink = new PersoniumRestAdapter();
                PersoniumResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    PersoniumRestAdapter restAdapterForDeleteLink = new PersoniumRestAdapter();
                    PersoniumResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl,
                            requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (PersoniumException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * 権限ALLのアカウントでアカウントと受信メッセージのリンクが作成できる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void 権限ALLのアカウントでアカウントと受信メッセージのリンクが作成できる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_NO_CONTENT, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links取得
            try {
                PersoniumRestAdapter restAdapterForGetLink = new PersoniumRestAdapter();
                PersoniumResponse linkRetreiveResponse = restAdapterForGetLink.getAcceptEncodingGzip(requestUrl,
                        requestheaders);
                assertEquals(HttpStatus.SC_OK, linkRetreiveResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }

            // $links削除
            if (messageId != null) {
                String linkDeleteRequestUrl = UrlUtils.cellCtlLinks(
                        messageReceivedCell, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, receiveAccountName);
                try {
                    PersoniumRestAdapter restAdapterForDeleteLink = new PersoniumRestAdapter();
                    PersoniumResponse linkDeleteResponse = restAdapterForDeleteLink.del(linkDeleteRequestUrl,
                            requestheaders);
                    assertEquals(HttpStatus.SC_NO_CONTENT, linkDeleteResponse.getStatusCode());
                } catch (PersoniumException e) {
                    fail();
                }
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * ReadMessageに対するアクセス権がないアカウントでアカウントと受信メッセージのリンク作成がエラー(403)になる.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void ReadMessageに対するアクセス権がないアカウントでアカウントと受信メッセージのリンク作成がエラー403になる() {
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
                PersoniumRestAdapter restAdapterForCreateLink = new PersoniumRestAdapter();
                PersoniumResponse linkCreateResponse = restAdapterForCreateLink.post(requestUrl,
                        bodyForCreateLink.toJSONString(), requestheaders);
                assertEquals(HttpStatus.SC_FORBIDDEN, linkCreateResponse.getStatusCode());
            } catch (PersoniumException ex) {
                fail();
            }
        } finally {
            // 作成したッセージの削除
            if (response1 != null) {
                ODataCommon.deleteOdataResource(response1.getLocationHeader());
            }
            MessageSentTest.deleteReceivedMessage(
                    Setup.TEST_CELL1, UrlUtils.cellRoot(Setup.TEST_CELL2), TYPE_MESSAGE, "test mail", "test body01");
        }
    }

    /**
     * NavigationProperty経由で受信メッセージとアカウント情報が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void NavigationProperty経由で受信メッセージとアカウント情報が取得できること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            PersoniumResponse messageResponse = createReceivedMessage();
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
            rest = new PersoniumRestAdapter();
            requestUrl = UrlUtils.cellCtlNagvigationProperty(
                    Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                    ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT);
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

            // Account基準 NavigationProperty経由
            rest = new PersoniumRestAdapter();
            requestUrl = UrlUtils.cellCtlNagvigationProperty(
                    Setup.TEST_CELL1, Account.EDM_TYPE_NAME, accountName,
                    Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE);
            res = rest.getAcceptEncodingGzip(requestUrl, requestheaders);
            assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        } catch (PersoniumException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new PersoniumRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (PersoniumException e) {
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
    public void 受信メッセージとアカウント情報の一覧取得で$expandを使用してデータが取得できること() {
        String locationHeader = null;
        String messageId = null;
        String accountName = "account1";
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

        try {
            // 受信メッセージの作成
            PersoniumResponse messageResponse = createReceivedMessage();
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
            rest = new PersoniumRestAdapter();
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
            rest = new PersoniumRestAdapter();
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

        } catch (PersoniumException e) {
            e.printStackTrace();
        } finally {
            // $links削除
            if (messageId != null) {
                String requestUrl = UrlUtils.cellCtlLinks(
                        Setup.TEST_CELL1, ReceivedMessage.EDM_TYPE_NAME, messageId,
                        ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT, accountName);
                try {
                    rest = new PersoniumRestAdapter();
                    res = null;
                    res = rest.del(requestUrl, requestheaders);
                } catch (PersoniumException e) {
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
    public void Boxに紐づいた受信Messageを作成できること() {

        String id = "12345678901234567890123456789012";
        JSONObject body = new JSONObject();
        body.put("__id", id);
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
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

    private String getId(PersoniumResponse response) {
        JSONObject result = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        return (String) result.get("__id");
    }

    @SuppressWarnings("unchecked")
    private PersoniumResponse createReceivedMessage() {
        JSONObject body = new JSONObject();
        body.put("__id", "12345678901234567890123456789012");
        body.put("From", UrlUtils.cellRoot(Setup.TEST_CELL2));
        body.put("Type", TYPE_MESSAGE);
        body.put("Title", "Title");
        body.put("Body", "Body");
        body.put("Priority", 3);
        body.put("Status", "unread");

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        // Authorizationヘッダ
        String targetCellUrl = UrlUtils.cellRoot(Setup.TEST_CELL1);
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + getCellIssueToken(targetCellUrl));

        try {
            String requestUrl = UrlUtils.receivedMessage(Setup.TEST_CELL1);
            res = rest.post(requestUrl, body.toJSONString(), requestheaders);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        return res;
    }

    private String getCellIssueToken(String targetCellUrl) {
        String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
        TransCellAccessToken token = new TransCellAccessToken(cellUrl, cellUrl,
                targetCellUrl, new ArrayList<Role>(), "", null);
        return token.toTokenString();
    }
}

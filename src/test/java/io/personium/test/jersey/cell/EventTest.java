/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DaoException;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * Tests for Event API.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class EventTest extends ODataCommon {
    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public EventTest() {
        super("io.personium.core.rs");
    }

    /**
     * イベント受付に対するPOSTで200が返却されること.
     */
    @Test
    public final void イベント受付に対するPOSTで200が返却されること() {
        JSONObject body = createEventBody();

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * イベント受付に対する許可しないメソッドで405が返却されること.
     */
    @Test
    public final void イベント受付に対する許可しないメソッドで405が返却されること() {

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", "")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.DELETE)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", "")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.PUT)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", "")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * イベント受付に対するPROPFINDで501が返却されること.
     */
    @Test
    public final void イベント受付に対するPROPFINDで501が返却されること() {

        Http.request("cell/cell-event.txt")
                .with("METHOD", io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", "")
                .returns()
                .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    /**
     * イベント受付に対するPROPPATCHで501が返却されること.
     */
    @Test
    public final void イベント受付に対するPROPPATCHで501が返却されること() {

        Http.request("cell/cell-event.txt")
                .with("METHOD", io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", "")
                .returns()
                .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    /**
     * イベント受付時の requestKeyに空文字を指定したPOSTで200が返却されること.
     */
    @Test
    public final void イベント受付時のrequestKeyに空文字を指定したPOSTで200が返却されること() {
        JSONObject body = createEventBody();

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "")
                .with("json", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * イベント受付時の requestKeyに正しいキー文字列を指定したPOSTで200が返却されること.
     */
    @Test
    public final void イベント受付時のrequestKeyに正しいキー文字列を指定したPOSTで200が返却されること() {
        JSONObject body = createEventBody();

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "abcdefghij12345_-xyz")
                .with("json", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * イベント受付時のrequestKeyヘッダを指定せずPOSTしても200が返却されること.
     */
    @Test
    public final void イベント受付時のrequestKeyヘッダを指定せずPOSTしても200が返却されること() {
        JSONObject body = createEventBody();

        Http.request("cell/cell-event-without-requestkey.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("json", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * イベント受付時のrequestKeyヘッダに不正文字を含めた値を指定しPOSTした場合400が返却されること.
     */
    @Test
    public final void イベント受付時のrequestKeyヘッダに不正文字を含めた値を指定しPOSTした場合400が返却されること() {

        TResponse response =
                Http.request("cell/cell-event.txt")
                        .with("METHOD", HttpMethod.POST)
                        .with("token", AbstractCase.MASTER_TOKEN_NAME)
                        .with("cellPath", Setup.TEST_CELL1)
                        .with("requestKey", "abc#123")
                        .with("json", "")
                        .returns();
        response.checkErrorResponse("PR400-EV-0002",
                PersoniumCoreException.Event.X_PERSONIUM_REQUESTKEY_INVALID.getMessage());
        response.statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Test Event API.
     * Normal test.
     * @throws DaoException DaoException
     * @throws InterruptedException InterruptedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void event_Normal_escape_processing() throws DaoException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("Type", "\\\"POST,");
        body.put("Object", "Object\\\"\\\"Data");
        body.put("Info", "resultData\\\"");

        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("requestKey", "testRequestKey")
                .with("json", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // wait a moment
        Thread.sleep(1000);

        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.get(UrlUtils.log(Setup.TEST_CELL1) + "/current/default.log");
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエスト実行
        PersoniumResponse response = request(req);
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        String responseBody;
        responseBody = response.bodyAsString();
        assertTrue(0 < responseBody.length());
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile(".*\"testRequestKey\",\"true\",null,null,"
                        + "\"\"\"POST,\",\"Object\"\"\"\"Data\",\"resultData\"\"\"$");
        Matcher matcher = pattern.matcher(responseBody);
        assertTrue(matcher.find());
    }



    @SuppressWarnings("unchecked")
    private JSONObject createEventBody() {
        JSONObject body = new JSONObject();
        body.put("Type", "TypeData");
        body.put("Object", "ObjectData");
        body.put("Info", "InfoData");
        return body;
    }
}

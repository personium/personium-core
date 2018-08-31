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
import static org.junit.Assert.fail;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.DaoException;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;

/**
 * Test for Error Page.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ErrorPageTest extends PersoniumTest {

    /**
     * constructor.
     */
    public ErrorPageTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * エラーページへのGETで指定したコードに対応するメッセージが返却されること.
     */
    @Test
    public final void エラーページへのGETで指定したコードに対応するメッセージが返却されること() {

        String code = PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode();
        PersoniumResponse res = requesttoErrorPage(code);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkResponseBody(res, code);

    }

    /**
     * 定義されていないコードを指定してエラーページを取得しundefinedとなること.
     */
    @Test
    public final void personiumで定義されていないコードを指定してエラーページを取得しundefinedとなること() {

        String code = "dummyCode";
        PersoniumResponse res = requesttoErrorPage(code);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkResponseBody(res, null);

    }

    /**
     * コードの値を指定せずにエラーページを取得しundefinedとなること.
     */
    @Test
    public final void コードの値を指定せずにエラーページを取得しundefinedとなること() {

        String code = "";
        PersoniumResponse res = requesttoErrorPage(code);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkResponseBody(res, null);
    }

    /**
     * コードを指定せずにエラーページを取得しundefinedとなること.
     */
    @Test
    public final void コードを指定せずにエラーページを取得しundefinedとなること() {

        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            res = rest.getAcceptEncodingGzip(
                    UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error", requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        // レスポンスヘッダのチェック
        assertEquals(MediaType.TEXT_HTML + ";charset=UTF-8", res.getFirstHeader(HttpHeaders.CONTENT_TYPE));

        // レスポンスボディのチェック
        checkResponseBody(res, null);

    }

    /**
     * エラーページへのPOSTで405となること.
     */
    @Test
    public final void エラーページへのPOSTで405となること() {

        String code = PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode();
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            res = rest.post(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=" + code, "", requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
    }

    /**
     * エラーページへのPUTで405となること.
     */
    @Test
    public final void エラーページへのPUTで405となること() {

        String code = PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode();
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            res = rest.put(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=" + code, "", requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
    }

    /**
     * エラーページへのDELETEで405となること.
     */
    @Test
    public final void エラーページへのDELETEで405となること() {

        String code = PersoniumCoreException.OData.JSON_PARSE_ERROR.getCode();
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            res = rest.del(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code=" + code, requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
    }

    /**
     * エラーページにリクエストを投入する.
     * @return レスポンス
     */
    private PersoniumResponse requesttoErrorPage(String code) {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        PersoniumResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();

        try {
            res = rest.getAcceptEncodingGzip(UrlUtils.cellRoot(Setup.TEST_CELL1) + "__html/error?code="
                    + code, requestheaders);
        } catch (PersoniumException e) {
            e.printStackTrace();
        }

        return res;
    }

    /**
     * レスポンスボディのチェックを行う.
     * @param res レスポンス情報
     * @param expectedCode 期待するエラーコード
     */
    public static void checkResponseBody(PersoniumResponse res, String expectedCode) {
        String body = null;
        String expectedMessage = null;
        String expectedTitle = PersoniumCoreMessageUtils.getMessage("PS-ER-0001");
        if (expectedCode == null) {
            expectedMessage = PersoniumCoreMessageUtils.getMessage("PS-ER-0002");
        } else {
            expectedMessage = PersoniumCoreMessageUtils.getMessage(expectedCode);
        }
        try {
            body = res.bodyAsString();
            System.out.println(body);
            assertEquals(
                    "<html><head><title>" + expectedTitle + "</title></head><body><h1>" + expectedTitle + "</h1><p>"
                            + expectedMessage + "</p></body></html>",
                    body.replaceFirst("<!-- .*-->", ""));
        } catch (DaoException e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
}

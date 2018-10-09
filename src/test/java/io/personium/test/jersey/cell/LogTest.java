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

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
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
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * Log APIのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class LogTest extends ODataCommon {

    private static final String ARCHIVE_COLLECTION = "archive";
    private static final String DEFAULT_LOG = "default.log";
    private static final String CURRENT_COLLECTION = "current";

    private static final long WAIT_TIME_FOR_EVENT = 3000; // msec

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public LogTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ログファイルに対するGETで200が返却されること.
     * @throws InterruptedException InterruptedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ログファイルに対するGETで200が返却されること()
            throws InterruptedException {
        JSONObject body = new JSONObject();
        body.put("Type", "POST");
        body.put("Object", "ObjectData");
        body.put("Info", "resultData");

        CellUtils.event(MASTER_TOKEN_NAME, HttpStatus.SC_OK, Setup.TEST_CELL1, body.toJSONString());

        // wait for log output
        Thread.sleep(WAIT_TIME_FOR_EVENT);

        TResponse response = Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns();
        response.debug();
        String responseBody = response.getBody();
        assertTrue(0 < responseBody.length());
        response.statusCode(HttpStatus.SC_OK);
    }

    /**
     * 日本語を含むログファイルに対するGETで200が返却されること.
     */
    // IT環境で実施するとエラーとなるため、Ignoreとする（チケット発行済み）
    @Ignore
    @SuppressWarnings("unchecked")
    @Test
    public final void 日本語を含むログファイルに対するGETで200が返却されること() {
        String cellName = "JPNEvntCell";

        JSONObject body = new JSONObject();
        body.put("Type", "POST");
        body.put("Object", "ObjectData");
        body.put("Info", "テスト用ログ");

        try {
            // 日本語ログ用セル作成
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // イベント登録
            CellUtils.event(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, body.toJSONString());

            // リクエストパラメータ設定
            PersoniumRequest req = PersoniumRequest.get(UrlUtils.log(cellName) + "/current/default.log");
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

            // リクエスト実行
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

            String responseBody;
            responseBody = response.bodyAsString();
            assertTrue(0 < responseBody.length());
            String[] cols = responseBody.split(",");
            String result = cols[cols.length - 1];
            assertEquals("\"テスト用ログ\"", result.trim());

        } catch (DaoException e) {
            e.printStackTrace();
        } finally {
            // 日本語ログ用セル削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, -1);
        }

    }

    /**
     * デフォルトログが存在しないときにファイルに対するGETで200が返却されること.
     */
    @Test
    public final void デフォルトログが存在しないときにファイルに対するGETで200が返却されること() {
        try {
            // Cell作成
            CellUtils.create("testcellforlognotfound", MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            TResponse response = Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", "TestCellForLogNotFound")
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns();
            response.debug();
            response.statusCode(HttpStatus.SC_OK);
            String responseBody = response.getBody();
            // 空のレスポンスボディが返されることを確認
            assertEquals(0, responseBody.length());
        } finally {
            // Cell削除
            CellUtils.delete(MASTER_TOKEN_NAME, "testcellforlognotfound");
        }

    }

    /**
     * デフォルトログ以外のファイルに対するGETで404が返却されること.
     */
    @Test
    public final void デフォルトログ以外のファイルに対するGETで404が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", "InvalidFileName.log")
                .with("ifNoneMatch", "*")
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 不正なコレクションを指定したGETで404が返却されること.
     */
    @Test
    public final void 不正なコレクションを指定したGETで404が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", "InvalidCollencion")
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * archiveコレクションを指定したGETで404が返却されること.
     */
    @Test
    public final void archiveコレクションを指定したGETで404が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", "archive")
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * ログファイルに対する許可しないメソッドで405が返却されること.
     */
    @Test
    public final void ログファイルに対する許可しないメソッドで405が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.PUT)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-get.txt")
                .with("METHOD", io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-get.txt")
                .with("METHOD", io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * ログファイルに対するDELETEで501が返却されること.
     */
    @Test
    public final void ログファイルに対するDELETEで501が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.DELETE)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    /**
     * archiveコレクションのログファイルに対するDELETEで501が返却されること.
     */
    @Test
    public final void archiveコレクションのログファイルに対するDELETEで501が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.DELETE)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", ARCHIVE_COLLECTION)
                .with("fileName", "default.log.1")
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    /**
     * 存在しないログファイルに対するGETで404が返却されること.
     */
    @Test
    public final void 存在しないログファイルに対するGETで404が返却されること() {

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", CURRENT_COLLECTION)
                .with("fileName", "dummy.log")
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);

        Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("collection", "dummy")
                .with("fileName", DEFAULT_LOG)
                .with("ifNoneMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Cellを削除しcurrentのログが削除されること.
     * @throws InterruptedException InterruptedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellを削除しcurrentのログが削除されること() throws InterruptedException {
        String cellName = "logtest_cell";

        JSONObject body = new JSONObject();
        body.put("Type", "POST");
        body.put("Object", "ObjectData");
        body.put("Info", "resultData");

        try {
            // Cell作成
            CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // イベント受付
            CellUtils.event(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, body.toJSONString());

            // wait for log output
            Thread.sleep(WAIT_TIME_FOR_EVENT);

            // ログ取得できること
            Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // Cell削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, HttpStatus.SC_NO_CONTENT);

            // ログ取得できないこと
            Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        } finally {
            // Cell削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * Owner情報がURL形式ではない場合にログに対する操作がひととおり行えること.
     * @throws InterruptedException InterruptedException
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Owner情報がURL形式ではない場合にログに対する操作がひととおり行えること()
            throws InterruptedException {
        String cellName = "logtest_ownercell";
        String ownerName = "dd4b407a-5b39-4c97-9fd5-5e2be9de06cf";

        JSONObject body = new JSONObject();
        body.put("Type", "POST");
        body.put("Object", "ObjectData");
        body.put("Info", "resultData");

        try {
            // Cell作成
            CellUtils.create(cellName, MASTER_TOKEN_NAME, ownerName, HttpStatus.SC_CREATED);

            // イベント受付
            CellUtils.event(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, body.toJSONString());

            // wait for log output
            Thread.sleep(WAIT_TIME_FOR_EVENT);

            // ログ取得できること
            Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // イベント受付(2回目)
            CellUtils.event(MASTER_TOKEN_NAME, HttpStatus.SC_OK, cellName, body.toJSONString());

            // wait for log output
            Thread.sleep(WAIT_TIME_FOR_EVENT);

            // ログ取得できること
            Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            // Cell削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, HttpStatus.SC_NO_CONTENT);

            // ログ取得できないこと
            Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
        } finally {
            // Cell削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, -1);
        }
    }

}

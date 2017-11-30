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
package io.personium.test.jersey.bar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.WebDAVMethod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.test.framework.JerseyTest;

import io.personium.core.model.progress.ProgressInfo;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * barファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BarInstallEventLogTest extends JerseyTest {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(BarInstallEventLogTest.class);

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String REQUEST_NOTYPE_FILE = "bar-install-without-type.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";
    private static final String BAR_FILE_MINIMUM = "/V1_1_2_bar_minimum.bar";
    private static final String DEFAULT_LOG = "default.log";
    private static final String CURRENT_COLLECTION = "current";
    private static final String UNIT_USER_CELL = "UnitUserCell";

    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BarInstallEventLogTest() {
        super("io.personium.core.rs");
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト完了時に実行される処理.
     */
    @AfterClass
    public static void afterClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
        cleanup();
    }

    private static void cleanup() {
        String reqCell = UNIT_USER_CELL;

        try {
            // コレクションの削除
            Http.request("box/delete-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col1/col11")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col3")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col2")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            Http.request("cell/box-delete.txt")
                    .with("cellPath", reqCell)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("boxPath", INSTALL_TARGET)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
    }

    /**
     * barファイルインストール後イベントログを取得して正常終了すること.
     */
    @Test
    public final void barファイルインストール後イベントログを取得して正常終了すること() {
        try {
            // CELL作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Barインストール実施
            String reqCell = UNIT_USER_CELL;
            String reqPath = INSTALL_TARGET;

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // イベント取得
            TResponse response = Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", UNIT_USER_CELL)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns();
            response.debug();

            // レスポンスの解析
            List<String[]> lines = BarInstallTestUtils.getListedBody(response.getBody());
            int count = 0;
            for (String[] line : lines) {
                if (line[6].equals(WebDAVMethod.MKCOL.toString())) {
                    assertEquals("202", line[8].trim());
                    break;
                }
                count++;
            }
            assertTrue(count < lines.size());
            lines.remove(count);

            int index = 0;
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-1000",
                    UrlUtils.getBaseUrl() + "/UnitUserCell/installBox", "Bar installation started.", index++);
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-1001",
                    "bar/00_meta/00_manifest.json", "Installation started.", index++);
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-1003",
                    "bar/00_meta/00_manifest.json", "Installation completed.", index++);
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-1001",
                    "bar/00_meta/90_rootprops.xml", "Installation started.", index++);
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-1003",
                    "bar/00_meta/90_rootprops.xml", "Installation completed.", index++);
            checkResponseLog(lines, "[INFO ]", "server", "PL-BI-0000",
                    UrlUtils.getBaseUrl() + "/UnitUserCell/installBox", "Bar installation completed.", index++);
            response.statusCode(HttpStatus.SC_OK);
        } finally {
            cleanup();
            // CELL削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL);
        }
    }

    /**
     * barファイルインストール受付時に400エラーとなったイベントログが取得できること.
     */
    @Test
    public final void barファイルインストール受付時に400エラーとなったイベントログが取得できること() {
        try {
            // CELL作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Barインストール実施
            String reqCell = UNIT_USER_CELL;
            String reqPath = INSTALL_TARGET;

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NOTYPE_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            // イベント取得
            TResponse response = Http.request("cell/log-get.txt")
                    .with("METHOD", HttpMethod.GET)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", UNIT_USER_CELL)
                    .with("collection", CURRENT_COLLECTION)
                    .with("fileName", DEFAULT_LOG)
                    .with("ifNoneMatch", "*")
                    .returns();
            response.debug();

            // レスポンスの解析
            List<String[]> lines = BarInstallTestUtils.getListedBody(response.getBody());
            int count = 0;
            for (String[] line : lines) {
                if (line[6].equals(WebDAVMethod.MKCOL.toString())) {
                    assertEquals("400", line[8].trim());
                    break;
                }
                count++;
            }
            assertTrue(count < lines.size());
        } finally {
            // CELL削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL);
        }
    }

    private void checkResponseLog(List<String[]> lines, String logLevel,
            String name, String action, String message, String result, int index) {
        String[] line = lines.get(index);
        assertEquals(logLevel, line[1]);
        assertEquals(name, line[3]);
        assertEquals(action, line[6]);
        assertEquals(message, line[7]);
        assertEquals(result, line[8].trim());
    }
}

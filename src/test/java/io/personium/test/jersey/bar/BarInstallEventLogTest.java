/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.RuleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * barファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BarInstallEventLogTest extends PersoniumTest {
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
    private static final String UNIT_USER_CELL = "unitusercell";

    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BarInstallEventLogTest() {
        super(new PersoniumCoreApplication());
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
     * @throws InterruptedException InterruptedException
     */
    @Test
    public final void barファイルインストール後イベントログを取得して正常終了すること() throws InterruptedException {
        try {
            // CELL作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Create Rule.
            JSONObject rule = new JSONObject();
            rule.put("Name", "boxinstall");
            rule.put("Action", "log");
            rule.put("EventType", "boxinstall");
            RuleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, rule, HttpStatus.SC_CREATED);
            rule.put("Name", "pl-bi");
            rule.put("EventType", "PL-BI-");
            RuleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, rule, HttpStatus.SC_CREATED);
            // wait for rule register
            Thread.sleep(3000);

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

            // Wait for log output
            Thread.sleep(3000);

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
                if (line[6].equals("boxinstall")) {
                    assertEquals("202", line[8].trim());
                    break;
                }
                count++;
            }
            assertTrue(count < lines.size());
            lines.remove(count);

            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-1000",
                    UrlUtils.getBaseUrl() + "/unitusercell/installBox", "Bar installation started.");
            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-1001",
                    "bar/00_meta/00_manifest.json", "Installation started.");
            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-1003",
                    "bar/00_meta/00_manifest.json", "Installation completed.");
            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-1001",
                    "bar/00_meta/90_rootprops.xml", "Installation started.");
            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-1003",
                    "bar/00_meta/90_rootprops.xml", "Installation completed.");
            checkResponseLog(lines, "[INFO ]", "false", "PL-BI-0000",
                    UrlUtils.getBaseUrl() + "/unitusercell/installBox", "Bar installation completed.");
            response.statusCode(HttpStatus.SC_OK);
        } finally {
            cleanup();
            // Delete Rule.
            RuleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, "pl-bi", null);
            RuleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, "boxinstall", null);
            // CELL削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL);
            // wait for event processing
            Thread.sleep(3000);
        }
    }

    /**
     * barファイルインストール受付時に400エラーとなったイベントログが取得できること.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public final void barファイルインストール受付時に400エラーとなったイベントログが取得できること()
            throws InterruptedException {
        try {
            // CELL作成
            CellUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            // Create Rule.
            JSONObject rule = new JSONObject();
            rule.put("Name", "box");
            rule.put("Action", "log");
            RuleUtils.create(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, rule, HttpStatus.SC_CREATED);
            Thread.sleep(3000);

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

            // wait for log output
            Thread.sleep(3000);

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
                if (line[6].equals("boxinstall")) {
                    assertEquals("400", line[8].trim());
                    break;
                }
                count++;
            }
            assertTrue(count < lines.size());
        } finally {
            // Delete Rule.
            RuleUtils.delete(UNIT_USER_CELL, AbstractCase.MASTER_TOKEN_NAME, "box", null);
            // CELL削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, UNIT_USER_CELL);
            // wait for event processing
            Thread.sleep(3000);
        }
    }

    private void checkResponseLog(List<String[]> lines, String logLevel,
            String external, String type, String object, String info) {
        for (String[] line : lines) {
            if (line[6].equals(type) && line[7].equals(object)) {
                assertEquals(logLevel, line[1]);
                assertEquals(external, line[3]);
                assertEquals(type, line[6]);
                assertEquals(object, line[7]);
                assertEquals(info, line[8].trim());
                return;
            }
        }
        assertTrue(false);
    }
}

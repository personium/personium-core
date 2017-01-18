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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcException;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Log API: 過去ログ取得用テスト.
 * <p>
 * 本クラスのテストでは、過去ログに対するテストを行うため、事前に過去ログを作成しておく必要がある。 <br />
 * EventLogのローテートは、50MBのサイズローテートを行っている。<br />
 * 毎回のテスト時に過去ログを作成すると時間がかかるため、Setup#reset()とは別の手段で初期設定する。
 * @see com.fujitsu.dc.test.setup.Setup#resetEventLog()
 *      </p>
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class EventArchiveLogGetTest extends ODataCommon {

    private static final int LOG_COLUMNS = 9;
    private static final String ARCHIVE_COLLECTION = "archive";
    private static final String DEFAULT_LOG_FORMAT = "default.log.%d";
    private static final String TEXT_CSV = "text/csv";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public EventArchiveLogGetTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ローテートされた過去ログを取得して200が返却されること.
     * @throws IOException レスポンスボディの読み込みに失敗した場合
     * @throws DcException DcException
     */
    @Test
    public final void ローテートされた過去ログを取得して200が返却されること() throws IOException, DcException {
        final String cell = Setup.TEST_CELL_EVENTLOG;

        // 過去ログのファイル名を取得するため、いったんPROPFINDを発行する
        TResponse tresponse = ResourceUtils.logCollectionPropfind(cell, ARCHIVE_COLLECTION, "1",
                AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

        Element root = tresponse.bodyAsXml().getDocumentElement();
        NodeList responseNodeList = root.getElementsByTagName("href");
        assertNotNull(responseNodeList);

        List<String> hrefList = new ArrayList<String>();
        for (int i = 1; i < responseNodeList.getLength(); i++) {
            hrefList.add(responseNodeList.item(i).getFirstChild().getNodeValue());
        }
        assertFalse("ログが1件も取得できなかった", hrefList.size() == 0);

        for (String href : hrefList) {
            String[] splitedHref = href.split("log\\.");
            String archiveLogName = String.format(DEFAULT_LOG_FORMAT, Long.valueOf(splitedHref[1]));
            DcResponse response = CellUtils.getLog(cell, ARCHIVE_COLLECTION, archiveLogName);
            assertEquals(TEXT_CSV, response.getFirstHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());

            checkResponseBody(response);
        }
    }

    /**
     * 存在しない過去ログを取得して404が返却されること.
     */
    @Test
    public final void 存在しない過去ログを取得して404が返却されること() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL_EVENTLOG;

        // 現在では作成されうることのないUnixタイムスタンプ(1)を設定している
        String archiveLogName = String.format(DEFAULT_LOG_FORMAT, 1);
        CellUtils.getLog(token, HttpStatus.SC_NOT_FOUND, cell, ARCHIVE_COLLECTION, archiveLogName);

        // その他
        CellUtils.getLog(token, HttpStatus.SC_NOT_FOUND, cell, ARCHIVE_COLLECTION, "default.log");
        CellUtils.getLog(token, HttpStatus.SC_NOT_FOUND, cell, ARCHIVE_COLLECTION, "test");
    }

    /**
     * archiveされた過去ログを取得で不正なメソッドを指定した場合405が返却されること.
     */
    @Test
    public final void archiveされた過去ログを取得で不正なメソッドを指定した場合405が返却されること() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL_EVENTLOG;

        String archiveLogName = String.format(DEFAULT_LOG_FORMAT, 1);
        TResponse response = Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", token)
                .with("cellPath", cell)
                .with("collection", ARCHIVE_COLLECTION)
                .with("fileName", archiveLogName)
                .with("ifNoneMatch", "*")
                .returns();
        response.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * 過去ログ取得時のレスポンスボディをチェックする.
     * <ul>
     * <li>１行ごとにレスポンスを読み込んで区切り子(",")で9カラム存在すること
     * <li>最終行(空行)は除外
     * </ul>
     * @param response レスポンス情報
     * @throws IOException レスポンスボディの読み込みに失敗した場合
     */
    private void checkResponseBody(DcResponse response) throws IOException {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try {
            is = response.bodyAsStream();
            isr = new InputStreamReader(is, "UTF-8");
            reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                assertEquals(LOG_COLUMNS, line.split(",").length);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(isr);
            IOUtils.closeQuietly(is);
        }
    }
}

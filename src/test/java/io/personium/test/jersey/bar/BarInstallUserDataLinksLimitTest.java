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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumUnitConfig.OData;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;

/**
 * ユーザデータの$links制限値チェック向けのbarファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class })
public class BarInstallUserDataLinksLimitTest extends PersoniumTest {

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";
    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";
    private static final String MASTER_TOKEN_NAME = Setup.MASTER_TOKEN_NAME;

    private static int linkNtoNMaxSize = 40;

    private static final int DEFAULT_LINKS_NTON_MAX_SIZE = PersoniumUnitConfig.getLinksNtoNMaxSize();

    /**
     * コンストラクタ.
     */
    public BarInstallUserDataLinksLimitTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
        PersoniumUnitConfig.set(OData.NN_LINKS_MAX_NUM,
                String.valueOf(linkNtoNMaxSize));
    }

    /**
     * すべてのテスト完了時に実行される処理.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(OData.NN_LINKS_MAX_NUM,
                String.valueOf(DEFAULT_LINKS_NTON_MAX_SIZE));
    }

    /**
     * barファイルインストールでAST対ASTのユーザデータLinkの登録が上限値を超えた場合異常終了すること.
     */
    @Test
    public final void barファイルインストールでAST対ASTのユーザデータLinkの登録が上限値を超えた場合異常終了すること() {
        final String reqCell = "boxinstalllinkslimitcell";
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";
        final String srcEntityName = "entity1";
        final String targetEntityName = "entity2";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + "/V1_1_2_bar_userdata_create_link_limit_nn.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            // 登録用のCellを作成
            CellUtils.create(reqCell, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

            // $links登録数のチェック
            // 1. $linksが上限値分作成されていること
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", reqCell)
                    .with("box", reqPath)
                    .with("collection", odataColName + "/" + srcEntityName + "('barInstallTest')")
                    .with("entityType", "_" + targetEntityName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(),
                    linkNtoNMaxSize);

            // 2. 上限値を超えた分のUserDataはlinkされていないこと
            int userDataIndex = linkNtoNMaxSize + 1;
            for (; userDataIndex < linkNtoNMaxSize + 4; userDataIndex++) {
                response = UserDataUtils.listLink(reqCell, reqPath, odataColName, targetEntityName, "barInstallTest"
                        + Integer.toString(userDataIndex), srcEntityName);
                results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
                assertEquals(0, results.size());
            }
        } finally {
            // 登録用Cellを再帰的削除
            Setup.cellBulkDeletion(reqCell);
        }
    }

    /**
     * barファイルインストールでONE対ASTのユーザデータLinkの登録が上限値を超えた場合登録できること.
     */
    @Test
    public final void barファイルインストールでONE対ASTのユーザデータLinkの登録が上限値を超えた場合登録できること() {
        // Linkの関係がAST対ASTの場合のみ、Linkの数の上限値を設けるようにしている。
        // 本テストでは、Linkの関係がONE対ASTの場合に上限値が有効にならないことを観点にしている。

        final String reqCell = "boxinstalllinkslimitcell";
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "col1";
        final String srcEntityName = "entity1";
        final String targetEntityName = "entity2";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + "/V1_1_2_bar_userdata_create_link_limit_1n.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            // 登録用のCellを作成
            CellUtils.create(reqCell, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // $links登録数のチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", reqCell)
                    .with("box", reqPath)
                    .with("collection", odataColName + "/" + srcEntityName + "('barInstallTest')")
                    .with("entityType", "_" + targetEntityName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", MASTER_TOKEN_NAME)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());
            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(),
                    linkNtoNMaxSize + 3);
        } finally {
            // 登録用Cellを再帰的削除
            Setup.cellBulkDeletion(reqCell);
        }
    }
}

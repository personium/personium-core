/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Collection, WebDAVファイル向けのbarファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BarInstallCollectionTest extends PersoniumTest {

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";

    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BarInstallCollectionTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
        BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, -1);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        BoxUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, -1);
    }

    /**
     * ContentTypeの指定が有効になること.
     */
    @Test
    public final void ContentTypeの指定が有効になること() {
        final String barFilePath = "/V1_1_2_bar_webdav_contentType.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // 登録したファイルの確認
            TResponse davResponse = DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    "box/dav-get.txt",
                    INSTALL_TARGET, "webdavcol1/testdavfile.txt", HttpStatus.SC_OK);
            assertEquals(MediaType.TEXT_PLAIN, davResponse.getHeader(HttpHeaders.CONTENT_TYPE));

        } finally {
            // ファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, box,
                    "webdavcol1/testdavfile.txt");

            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ContentTypeの指定が不正の場合異常終了すること.
     */
    @Test
    public final void ContentTypeの指定が不正の場合異常終了すること() {
        final String barFilePath = "/V1_1_2_bar_webdav_contentType_invalid.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 複数階層のCollectionにWebDAVファイルが登録できること.
     */
    @Test
    public final void 複数階層のCollectionにWebDAVファイルが登録できること() {
        final String barFilePath = "/V1_1_2_bar_webdav_hierarchy.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // 登録したファイルの確認
            TResponse davResponse = DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    "box/dav-get.txt",
                    INSTALL_TARGET, "webdavcol1/webdavcol1/testdavfile.txt", HttpStatus.SC_OK);
            assertEquals(MediaType.TEXT_PLAIN, davResponse.getHeader(HttpHeaders.CONTENT_TYPE));

        } finally {
            // ファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, box,
                    "webdavcol1/webdavcol1/testdavfile.txt");

            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1/webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);

            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 同階層に複数のDavコレクションが存在する場合に正しい階層で登録できること.
     */
    @Test
    public final void 同階層に複数のDavコレクションが存在する場合に正しい階層で登録できること() {
        final String barFilePath = "/V1_1_2_bar_webdav_samehierarchy.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // Collectionが登録されたことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol2", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol2/davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol1/davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol2", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol1/davcol2", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol1/davcol1/davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol2/davcol2", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol2/davcol1/davcol1", HttpStatus.SC_OK);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol1/davcol1/davcol1",
                    AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol1/davcol2", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol2/davcol1/davcol1",
                    AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol2/davcol2", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol1/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol2/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol2", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol2", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * rootpropsのコレクションの定義順に誤りがある場合異常終了すること.
     */
    @Test
    public final void rootpropsのコレクションの定義順に誤りがある場合異常終了すること() {
        final String barFilePath = "/V1_1_2_bar_webdav_badhierarchy.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

            // Collectionが登録されていないことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1", HttpStatus.SC_NOT_FOUND);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/davcol1", HttpStatus.SC_NOT_FOUND);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "davcol1/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Odataコレクション配下にコレクションが作成できないこと.
     */
    @Test
    public final void Odataコレクション配下にコレクションが作成できないこと() {
        final String barFilePath = "/V1_1_2_bar_webdav_odatacolhierarchy.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

            // Collectionが登録されていないことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "odatacol1", HttpStatus.SC_NOT_FOUND);
            // Collectionが登録されていないことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "odatacol1/davcol1", HttpStatus.SC_NOT_FOUND);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "odatacol1/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "odatacol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * サービスコレクション配下にコレクションが作成できないこと.
     */
    @Test
    public final void サービスコレクション配下にコレクションが作成できないこと() {
        final String barFilePath = "/V1_1_2_bar_webdav_svccolhierarchy.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

            // Collectionが登録されていないことの確認
            BarInstallTestUtils.propfind(cell, box, "svccol1", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NOT_FOUND);
            // Collectionが登録されていないことの確認
            BarInstallTestUtils.propfind(cell, box, "svccol1/davcol1", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NOT_FOUND);

        } finally {
            // コレクションの削除
            // DavResourceUtils.deleteCol(cell, box, "svccol1/davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "svccol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Davコレクションの配下にOdataコレクションを作成できること.
     */
    @Test
    public final void Davコレクションの配下にOdataコレクションを作成できること() {
        final String barFilePath = "/V1_1_2_bar_webdav_davodatacol.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // コレクションが登録されたことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/odatacol1", HttpStatus.SC_OK);
            // エンティティタイプが登録されたことの確認
            EntityTypeUtils.get(cell, AbstractCase.MASTER_TOKEN_NAME, box, "davcol1/odatacol1", "entity1",
                    HttpStatus.SC_OK);
        } finally {
            // エンティティタイプの削除
            Setup.entityTypeDelete("davcol1/odatacol1", "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "davcol1/odatacol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Davコレクションの配下にOdataコレクションを作成できること.
     */
    @Test
    public final void Davコレクションの配下にOdataコレクションにを作成できること() {
        final String barFilePath = "/V1_1_2_bar_webdav_davodatacol.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // コレクションが登録されたことの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/odatacol1", HttpStatus.SC_OK);
            // エンティティタイプが登録されたことの確認
            EntityTypeUtils.get(cell, AbstractCase.MASTER_TOKEN_NAME, box, "davcol1/odatacol1", "entity1",
                    HttpStatus.SC_OK);
        } finally {
            // エンティティタイプの削除
            Setup.entityTypeDelete("davcol1/odatacol1", "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "davcol1/odatacol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * Davコレクションの配下にServiceコレクションを作成できること.
     */
    @Test
    public final void Davコレクションの配下にServiceコレクションを作成できること() {
        final String barFilePath = "/V1_1_2_bar_webdav_davsvccol.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // コレクションが登録されたことの確認
            BarInstallTestUtils.propfind(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_MULTI_STATUS);
            BarInstallTestUtils.propfind(cell, box, "davcol1/svccol1", AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_MULTI_STATUS);
        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "davcol1/svccol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * rootpropsに定義が無いWebDAVファイルを登録した場合異常終了すること.
     */
    @Test
    public final void rootpropsに定義が無いWebDAVファイルを登録した場合異常終了すること() {
        final String barFilePath = "/V1_1_2_bar_webdav_rootprops_notcontain.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // ファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, box,
                    "webdavcol1/testdavfile.txt");

            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * rootpropsに定義が有りWebDAVファイルが存在しない場合ファイルが登録されないこと.
     */
    @Test
    public final void rootpropsに定義が有りWebDAVファイルが存在しない場合ファイルが登録されないこと() {
        final String barFilePath = "/V1_1_2_bar_webdav_file_not_exist.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // 登録したファイルの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "webdavcol1/testdavfile.txt", HttpStatus.SC_NOT_FOUND);

        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * サービスソースが登録されること.
     */
    @Test
    public final void サービスソースが登録されること() {
        final String barFilePath = "/V1_1_2_bar_service_source.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // 登録したファイルの確認
            res = DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "svccol1/__src/test.js", HttpStatus.SC_OK);

            assertTrue(res.getBody().startsWith("// テストです。"));

        } finally {
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, box,
                    "svccol1/__src/test.js");
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "svccol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 存在しないコレクションに対してサービスソースを登録した場合ソースが登録されないこと.
     */
    @Test
    public final void 存在しないコレクションに対してサービスソースを登録した場合ソースが登録されないこと() {
        final String barFilePath = "/V1_1_2_bar_service_source_notexist_col.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);

            // ファイルの確認
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "dummysvccol1/__src/test.js", HttpStatus.SC_NOT_FOUND);

        } finally {
            // コレクションの削除
            DavResourceUtils.deleteCollection(cell, box, "svccol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * rootpropsのスキーマなしBox用ACL_URLのBox名がデフォルトボックスではない場合に異常終了すること.
     */
    @Test
    @Ignore
    public final void rootpropsのBox用ACL_URLのBox名がデフォルトボックスではない場合に異常終了すること() {
        final String barFilePath = "/V1_1_2_bar_90rootprops_acl_box_url_invalid.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスメッセージの確認
            List<String[]> lines = BarInstallTestUtils.getListedBody(res.getBody());
            int index = 0;
            assertEquals("PL-BI-1000", lines.get(index++)[0]);
            for (int i = 0; i < 2; i++) {
                assertEquals("PL-BI-1001", lines.get(index++)[0]);
                assertEquals("PL-BI-1003", lines.get(index++)[0]);
            }
            assertEquals("PL-BI-1001", lines.get(index++)[0]);
            assertEquals("PL-BI-1004", lines.get(index++)[0]);
            assertEquals("PL-BI-0001", lines.get(index++)[0]);
        } finally {
            String resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "admin");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "user");
            ODataCommon.deleteOdataResource(resourceUrl);
        }
    }

    /**
     * rootpropsのスキーマなしCollection用ACL_URLのBox名がデフォルトボックスではない場合に異常終了すること.
     */
    @Test
    @Ignore
    public final void rootpropsのCollection用ACL_URLのBox名がデフォルトボックスではない場合に異常終了すること() {
        final String barFilePath = "/V1_1_2_bar_90rootprops_acl_col_invalid.bar";
        String cell = Setup.TEST_CELL1;
        String box = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスメッセージの確認
            List<String[]> lines = BarInstallTestUtils.getListedBody(res.getBody());
            int index = 0;
            assertEquals("PL-BI-1000", lines.get(index++)[0]);
            for (int i = 0; i < 2; i++) {
                assertEquals("PL-BI-1001", lines.get(index++)[0]);
                assertEquals("PL-BI-1003", lines.get(index++)[0]);
            }
            assertEquals("PL-BI-1001", lines.get(index++)[0]);
            assertEquals("PL-BI-1004", lines.get(index++)[0]);
            assertEquals("PL-BI-0001", lines.get(index++)[0]);
        } finally {
            String resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "admin");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "user");
            ODataCommon.deleteOdataResource(resourceUrl);
        }
    }
}

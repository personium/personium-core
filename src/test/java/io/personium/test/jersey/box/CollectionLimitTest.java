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
package io.personium.test.jersey.box;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.bar.BarInstallTestUtils;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * MKCOLのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class })
public class CollectionLimitTest extends PersoniumTest {

    /**
     * コンストラクタ.
     */
    public CollectionLimitTest() {
        super(new PersoniumCoreApplication());
    }

    static final String ACL_AUTH_TEST_SETTING_FILE = "box/acl-authtest.txt";
    static final String ACL_AUTH_PROPPATCH_TEST_SETTING_FILE = "box/acl-authtestProppatch.txt";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String ACL_SETTING_TEST = "box/acl-setting.txt";

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";
    private static final String DEFAULT_SCHEMA_URL = "https://fqdn/testcell1/";

    /**
     * 階層チェックのテスト_WebDavコレクションの追加.
     */
    @Test
    public final void 階層チェックのテスト() {
        // propertyで指定した階層数を超えた場合、エラーとなることを確認する。
        String cellName = "collectionLimitCell";
        String boxName = "box1";
        String colNamePrefix = "col";
        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);
            // テスト用Boxの作成
            BoxUtils.create(cellName, boxName, TOKEN, HttpStatus.SC_CREATED);

            // コレクションの階層数の最大値を取得
            int maxCollectionDepth = PersoniumUnitConfig.getMaxCollectionDepth();

            // 最大値階層分のWebDavコレクションを作成
            String path = "";
            int i;
            for (i = 1; i <= maxCollectionDepth; i++) {
                if (!path.isEmpty()) {
                    path += "/";
                }
                path += colNamePrefix + String.valueOf(i);
                // Davコレクションの作成
                Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", path).with("token", TOKEN)
                        .returns().statusCode(HttpStatus.SC_CREATED);
            }

            // 最大値を超える分のWebDavコレクションを作成
            path += "/" + colNamePrefix + String.valueOf(i);
            // Davコレクションの作成(400エラーになること)
            Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", path).with("token", TOKEN)
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * 階層チェックのテスト_WebDavファイルの追加.
     */
    @Test
    public final void 階層チェックのテスト_WebDavファイルの追加() {
        String cellName = "collectionLimitCell";
        String boxName = "box1";
        String colNamePrefix = "col";
        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);
            // テスト用Boxの作成
            BoxUtils.create(cellName, boxName, TOKEN, HttpStatus.SC_CREATED);

            // コレクションの階層数の最大値を取得
            int maxCollectionDepth = PersoniumUnitConfig.getMaxCollectionDepth();

            // 最大値階層分のWebDavコレクションを作成
            String path = "";
            int i;
            for (i = 1; i <= maxCollectionDepth; i++) {
                if (!path.isEmpty()) {
                    path += "/";
                }
                path += colNamePrefix + String.valueOf(i);
                // Davコレクションの作成
                Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", path)
                        .with("token", TOKEN).returns().statusCode(HttpStatus.SC_CREATED);
            }

            // WebDavファイルを作成
            path += "hoge.txt";
            // Davファイルの作成
            DavResourceUtils.createWebDavFile(cellName, TOKEN, "box/dav-put.txt", "hoge", boxName, path,
                    HttpStatus.SC_CREATED);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }


    /**
     * コレクション配下のコレクション最大数チェックのテスト.
     */
    @Test
    public final void コレクション配下のコレクション最大数チェックのテスト() {
        // propertyで指定した要素数を超えた場合、エラーとなることを確認する。
        String cellName = "collectionLimitCell";
        String boxName = "box1";
        String colNamePrefix = "col";
        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);
            // テスト用Boxの作成
            BoxUtils.create(cellName, boxName, TOKEN, HttpStatus.SC_CREATED);

            // コレクションの子要素数の最大値を取得
            int maxChildCount = PersoniumUnitConfig.getMaxChildResourceCount();

            // 基底コレクションの作成
            String basePath = "col1";
            Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", basePath)
            .with("token", TOKEN).returns().statusCode(HttpStatus.SC_CREATED);

            // 子コレクションの作成
            String path = "";
            int i;
            for (i = 1; i <= maxChildCount; i++) {
                path = basePath + "/" + colNamePrefix + String.valueOf(i);
                // Davコレクションの作成
                Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", path)
                        .with("token", TOKEN).returns().statusCode(HttpStatus.SC_CREATED);
            }

            // 最大値を超える分の子コレクションを作成
            path = basePath + "/" + colNamePrefix + String.valueOf(i);
            // Davコレクションの作成
            Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", path).with("token", TOKEN)
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * コレクション配下のファイル最大数チェックのテスト.
     */
    @Test
    public final void コレクション配下のファイル最大数チェックのテスト() {
        // propertyで指定した要素数を超えた場合、エラーとなることを確認する。
        String cellName = "collectionLimitCell";
        String boxName = "box1";
        String davFileNamePrefix = "davFile";
        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);
            // テスト用Boxの作成
            BoxUtils.create(cellName, boxName, TOKEN, HttpStatus.SC_CREATED);

            // コレクションの子要素数の最大値を取得
            int maxChildCount = PersoniumUnitConfig.getMaxChildResourceCount();

            // 基底コレクションの作成
            String basePath = "col1";
            Http.request("box/mkcol-normal.txt").with("cellPath", cellName).with("path", basePath).with("token", TOKEN)
                    .returns().statusCode(HttpStatus.SC_CREATED);

            // 最大数分の子ファイルを作成
            String path = "";
            int i;
            for (i = 1; i <= maxChildCount; i++) {
                path = basePath + "/" + davFileNamePrefix + String.valueOf(i);
                // Davファイルの作成
                DavResourceUtils.createWebDavFile(cellName, TOKEN, "box/dav-put.txt", "hoge", boxName, path,
                        HttpStatus.SC_CREATED);
            }

            // 最大値を超える分の子コレクションを作成
            path = basePath + "/" + davFileNamePrefix + String.valueOf(i);
            // Davファイルの作成
            DavResourceUtils.createWebDavFile(cellName, TOKEN, "box/dav-put.txt", "hoge", boxName, path,
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * Boxインストールで作成する場合の階層チェックのテスト.
     */
    @Test
    public final void Boxインストールで作成する場合の階層チェックのテスト() {
        // 本テストでは子要素の最大数：10 コレクションの最大階層数：5の設定となっていることを前提とし、
        // 階層数:6のBarファイルを登録して、エラーとなることを確認する。
        final String barFilePath = "/V1_1_2_bar_webdav_hierarchy_error.bar";
        String cellName = "collectionLimitCell";
        String box = INSTALL_TARGET;

        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cellName, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cellName) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * Boxインストールで作成する場合の階層チェックのテスト_WebDavファイルの追加.
     */
    @Test
    public final void Boxインストールで作成する場合の階層チェックのテスト_WebDavファイルの追加() {
        // 本テストでは子要素の最大数：10 コレクションの最大階層数：5の設定となっていることを前提とし、
        // 階層数:5のコレクション配下にWebDavファイルが存在するBarファイルを登録して、正常終了することを確認する。
        final String barFilePath = "/V1_1_2_bar_webdav_into_max_hierarchy.bar";
        String cellName = "collectionLimitCell";
        String box = INSTALL_TARGET;

        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cellName, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cellName) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * Boxインストールで作成する場合のコレクション配下のコレクション最大数チェックのテスト.
     */
    @Test
    public final void Boxインストールで作成する場合のコレクション配下のコレクション最大数チェックのテスト() {
        // 本テストでは子要素の最大数：20 コレクションの最大階層数：5の設定となっていることを前提とし、
        // WebDavコレクションの子要素数21(全てWebDavコレクション)のBarファイルを登録して、エラーとなることを確認する。
        final String barFilePath = "/V1_1_2_bar_webdav_collection_count_error.bar";
        String cellName = "collectionLimitCell";
        String box = INSTALL_TARGET;

        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cellName, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cellName) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * Boxインストールで作成する場合のコレクション配下のファイル最大数チェックのテスト.
     */
    @Test
    public final void Boxインストールで作成する場合のコレクション配下のファイル最大数チェックのテスト() {
        // 本テストでは子要素の最大数：20 コレクションの最大階層数：5の設定となっていることを前提とし、
        // WebDavコレクションの子要素数21(全てWebDavファイル)のBarファイルを登録して、エラーとなることを確認する。
        final String barFilePath = "/V1_1_2_bar_webdav_file_count_error.bar";
        String cellName = "collectionLimitCell";
        String box = INSTALL_TARGET;

        try {
            // テスト用Cellの作成
            CellUtils.create(cellName, TOKEN, HttpStatus.SC_CREATED);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + barFilePath);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cellName, box, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cellName) + box;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, DEFAULT_SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }


}

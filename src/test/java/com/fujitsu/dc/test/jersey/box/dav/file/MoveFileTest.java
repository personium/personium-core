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
package com.fujitsu.dc.test.jersey.box.dav.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * MOVEのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveFileTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public MoveFileTest() {
        super("com.fujitsu.dc.core.rs");
    }

    private void checkPropfindResponse(final String expectedUrl, TResponse res, boolean isExists) {
        Document propfind = res.bodyAsXml();
        NodeList list = propfind.getElementsByTagName("href");
        int index = 0;
        boolean isMatch = false;
        for (index = 0; index < list.getLength(); index++) {
            org.w3c.dom.Node node = list.item(index);
            NodeList children = node.getChildNodes();

            assertEquals(1, children.getLength());
            Text href = (Text) children.item(0);
            if (expectedUrl.equals(href.getNodeValue())) {
                isMatch = true;
            }
        }
        assertEquals(isExists, isMatch);
    }

    /**
     * MOVEで移動元が__srcの場合に400エラーとなること.
     */
    @Test
    public final void MOVEで移動元が__srcの場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        final String svcColName = "svcColforMOVE";
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);

            // Fileの移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src", destination, HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_MOVE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * MOVEで移動元がユーザスキーマで上位リソースであるコレクションが存在しない場合に404エラーとなること.
     */
    @Test
    public final void MOVEで移動元がユーザスキーマで上位リソースであるコレクションが存在しない場合に404エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // Fileの移動
            String url = UrlUtils.entityType(CELL_NAME, BOX_NAME, "dummyCollection", "entity");
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEで移動元がユーザスキーマで上位リソースであるコレクションが存在する場合に405エラーとなること.
     */
    @Test
    public final void MOVEで移動元がユーザスキーマで上位リソースであるコレクションが存在する場合に405エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // Fileの移動
            String url = UrlUtils.entityType(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "entity");
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEで移動元がユーザODataで上位リソースであるEntityTypeが存在しない場合に404エラーとなることと.
     */
    @Test
    public final void MOVEで移動元がユーザODataで上位リソースであるEntityTypeが存在しない場合に404エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // Fileの移動
            String url = UrlUtils.userdata(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "entity", "key");
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEで移動元がユーザODataで上位リソースであるEntityTypeが存在する場合に405エラーとなること.
     */
    @Test
    public final void MOVEで移動元がユーザODataで上位リソースであるEntityTypeが存在する場合に405エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // Fileの移動
            String url = UrlUtils.userdata(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, "SalesDetail", "userdata100");
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_METHOD_NOT_ALLOWED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEで移動元のファイルが存在しない場合に404エラーとなること.
     */
    @Test
    public final void MOVEで移動元のファイルが存在しない場合に404エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_NOT_FOUND;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEで移動元がBoxの場合に400エラーとなること.
     */
    @Test
    public final void MOVEで移動元がBoxの場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 移動
            String url = UrlUtils.boxRoot(CELL_NAME, BOX_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_BOX;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * MOVEでリクエストボディを指定した場合に無視されて移動できること.
     */
    @Test
    public final void MOVEでリクエストボディを指定した場合に無視されて移動できること() {
        final String colName = "davColforMOVE";
        final String destPath = "destResource";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            String sourcePath = BOX_NAME + "/" + colName + "/" + FILE_NAME;
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    sourcePath, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");
            req.addJsonBody(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destPath, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destPath);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダの値の末尾にスラッシュを指定した場合に無視されて移動できること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダの値の末尾にスラッシュを指定した場合に無視されて移動できること() {
        final String colName = "davColforMOVE";
        final String destPath = "destResource";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, colName);
            String sourcePath = BOX_NAME + "/" + colName + "/" + FILE_NAME;
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    sourcePath, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination + "/");
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destPath, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destPath);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = String.format("%s://%s/%s/%s/%s",
                DcCoreConfig.getUnitScheme(), "fqdn", CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダにBaseURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダにBaseURLを指定した場合に400エラーとなること() {
        final String destination = UrlUtils.getBaseUrl();
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに移動元とCell名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに移動元とCell名が異なる文字列を指定した場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box("another_cell", BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, "another_box", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先がServieコレクション配下の場合400エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先がServieコレクション配下の場合400エラーとなること() {
        final String colName = "svcColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME + "/" + colName, FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先がServieコレクションの__src配下の場合移動できること.
     */
    @Test
    public final void FileのMOVEで移動先がServieコレクションの__src配下の場合移動できること() {
        final String colName = "svcColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src", FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME + "/" + colName + "/__src", FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先の1階層目の親リソースが存在しない場合409エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先の1階層目の親リソースが存在しない場合409エラーとなること() {
        String invalidColName = "invalidCol";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, invalidColName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CONFLICT);
            DcCoreException expectedException = DcCoreException.Dav.HAS_NOT_PARENT.params(invalidColName);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先の2階層目の親リソースが存在しない場合409エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先の2階層目の親リソースが存在しない場合409エラーとなること() {
        String colName = "col";
        String invalidColName = "invalidCol";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, invalidColName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CONFLICT);
            DcCoreException expectedException = DcCoreException.Dav.HAS_NOT_PARENT.params(invalidColName);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに存在しないCellのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに存在しないCellのURLを指定した場合に400エラーとなること() {
        final String destination = UrlUtils.cellRoot("dummyTestCellForMove");
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること() {
        final String destination = UrlUtils.boxRoot(CELL_NAME, "dummyTestBoxForMove");
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在しないWebDavコレクションの場合に移動できること. <br />
     * ここでは以下のテストケースを想定してるが、移動先リソースがない場合は、移動先リソースをファイルとして扱うため、同一の結果となる。<br />
     * そのため、これらのテストケースをまとめてテストを実施する。
     * <ul>
     * <li>FileのMOVEで移動先が存在しないファイル配下のファイルの場合400エラーとなること</li>
     * <li>FileのMOVEで移動先が存在しないServieコレクションの場合400エラーとなること</li>
     * <li>FileのMOVEで移動先が存在しないServieソースコレクションの場合400エラーとなること（ありえないテストケース）</li>
     * <li>FileのMOVEで移動先が存在しないODataコレクションの場合400エラーとなること</li>
     * <li>FileのMOVEで移動先が存在しないODataコレクション配下のファイルの場合400エラーとなること（ありえないテストケース）</li>
     * </ul>
     */
    @Test
    public final void FileのMOVEで移動先が存在しないリソースの場合に移動できること() {
        final String colName = "davColforMOVE";
        final String destPath = "destResource";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            String sourcePath = BOX_NAME + "/" + colName + "/" + FILE_NAME;
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    sourcePath, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destPath, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destPath);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに存在するCellのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに存在するCellのURLを指定した場合に400エラーとなること() {
        final String destination = UrlUtils.cellRoot(CELL_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに存在するBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに存在するBoxのURLを指定した場合に400エラーとなること() {
        final String destination = UrlUtils.boxRoot(CELL_NAME, BOX_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在するWebDavコレクションに対して上書きモードで移動する場合400エラーとなること.
     * TODO コレクションの移動を実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void FileのMOVEで移動先が存在するWebDavコレクションに対して上書きモードで移動する場合400エラーとなること() {
        final String colName = "davColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在するWebDavコレクションに対して上書き禁止モードで移動する場合412エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先が存在するWebDavコレクションに対して上書き禁止モードで移動する場合412エラーとなること() {
        final String destFileName = "file2.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destFileName, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEで移動先が存在するファイル配下のファイルの場合400エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先が存在するファイル配下のファイルの場合400エラーとなること() {
        final String parentFileName = "parent_file";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, parentFileName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + parentFileName, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_FILE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, parentFileName);
        }
    }

    /**
     * FileのMOVEで移動先が存在するServieコレクションの場合400エラーとなること.
     * TODO コレクションの移動を実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void FileのMOVEで移動先が存在するServieコレクションの場合400エラーとなること() {
        final String colName = "svcColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在するServieソースコレクションの場合400エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先が存在するServieソースコレクションの場合400エラーとなること() {
        final String colName = "svcColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src");
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在するODataコレクションの場合400エラーとなること.
     * TODO コレクションの移動を実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void FileのMOVEで移動先が存在するODataコレクションの場合400エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで移動先が存在するODataコレクション配下のファイルの場合400エラーとなること.
     */
    @Test
    public final void FileのMOVEで移動先が存在するODataコレクション配下のファイルの場合400エラーとなること() {
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA) + "/" + FILE_NAME;
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEで同一Box直下の存在しないファイルに上書き禁止モードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一Box直下の存在しないファイルに上書き禁止モードで移動できること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEで同一Box直下の存在しないファイルに上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一Box直下の存在しないファイルに上書きモードで移動できること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEで同一Box直下の存在するファイルに上書き禁止モードで412エラーとなること.
     */
    @Test
    public final void FileのMOVEで同一Box直下の存在するファイルに上書き禁止モードで412エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destFileName, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            TResponse res = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, destination, HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

            // 移動元ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEで同一Box直下の存在するファイルに上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一Box直下の存在するファイルに上書きモードで移動できること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destFileName, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);
            // 移動元ファイルが取得できないこと
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME,
                    HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEで同一WebDAVコレクション直下の存在しないファイルに上書き禁止モードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一WebDAVコレクション直下の存在しないファイルに上書き禁止モードで移動できること() {
        final String colName = "davColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + colName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで同一WebDAVコレクション直下の存在しないファイルに上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一WebDAVコレクション直下の存在しないファイルに上書きモードで移動できること() {
        final String colName = "davColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + colName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで同一WebDAVコレクション直下の存在するファイルに上書き禁止モードで412エラーとなること.
     */
    @Test
    public final void FileのMOVEで同一WebDAVコレクション直下の存在するファイルに上書き禁止モードで412エラーとなること() {
        final String colName = "davColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + colName + "/" + destFileName,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元ファイルを取得できること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + FILE_NAME, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで同一WebDAVコレクション直下の存在するファイルに上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで同一WebDAVコレクション直下の存在するファイルに上書きモードで移動できること() {
        final String colName = "davColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + colName + "/" + destFileName,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルが取得できないこと
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    colName + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + colName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで別のWebDAVコレクション直下の存在しないファイルが上書き禁止モードで移動できること.
     */
    @Test
    public final void FileのMOVEで別のWebDAVコレクション直下の存在しないファイルが上書き禁止モードで移動できること() {
        final String srcColName1 = "srcDavColforMOVE1";
        final String srcColName2 = "srcDavColforMOVE2";
        final String dstColName1 = "dstDavColforMOVE1";
        final String dstColName2 = "dstDavColforMOVE2";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    srcColName1 + "/" + srcColName2);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, dstColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    dstColName1 + "/" + dstColName2);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName1 + "/" + srcColName2 + "/" + FILE_NAME,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動（上書き禁止モード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1), res, true);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1 + "/" + srcColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2, FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1 + "/" + dstColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
            checkPropfindResponse(href, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1 + "/" + dstColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで別のWebDAVコレクション直下の存在しないファイルが上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで別のWebDAVコレクション直下の存在しないファイルが上書きモードで移動できること() {
        final String srcColName1 = "srcDavColforMOVE1";
        final String srcColName2 = "srcDavColforMOVE2";
        final String dstColName1 = "dstDavColforMOVE1";
        final String dstColName2 = "dstDavColforMOVE2";
        final String destFileName = "destFile.txt";
        final String destination2 = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    srcColName1 + "/" + srcColName2);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, dstColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    dstColName1 + "/" + dstColName2);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName1 + "/" + srcColName2 + "/" + FILE_NAME,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動（上書きモード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination2);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルを取得できない
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1), res, true);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1 + "/" + srcColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2, FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1 + "/" + dstColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
            checkPropfindResponse(href, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1 + "/" + dstColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで別のWebDAVコレクション直下の存在するファイルが上書き禁止モードで412エラーとなること.
     */
    @Test
    public final void FileのMOVEで別のWebDAVコレクション直下の存在するファイルが上書き禁止モードで412エラーとなること() {
        final String srcColName1 = "srcDavColforMOVE1";
        final String srcColName2 = "srcDavColforMOVE2";
        final String dst1ColName1 = "dst1DavColforMOVE1";
        final String dst1ColName2 = "dst1DavColforMOVE2";
        final String destFileName = "destFile.txt";
        final String destination1 = UrlUtils.box(CELL_NAME, BOX_NAME, dst1ColName1, dst1ColName2, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    srcColName1 + "/" + srcColName2);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, dst1ColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    dst1ColName1 + "/" + dst1ColName2);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName1 + "/" + srcColName2 + "/" + FILE_NAME,
                    FILE_BODY + "1", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + dst1ColName1 + "/" + dst1ColName2 + "/" + destFileName,
                    FILE_BODY + "2", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動（上書き禁止モード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination1);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元ファイルを取得できること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "1");
            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dst1ColName1 + "/" + dst1ColName2 + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "2");

            // PROPFIND（移動元）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1), res, true);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dst1ColName1), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1 + "/" + srcColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2, FILE_NAME);
            checkPropfindResponse(href, res, true);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dst1ColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dst1ColName1, dst1ColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dst1ColName1 + "/" + dst1ColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dst1ColName1, dst1ColName2, destFileName);
            checkPropfindResponse(href, res, true);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dst1ColName1 + "/" + dst1ColName2 + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dst1ColName1 + "/" + dst1ColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dst1ColName1, TOKEN, -1);
        }
    }

    /**
     * FileのMOVEで別のWebDAVコレクション直下の存在するファイルが上書きモードで移動できること.
     */
    @Test
    public final void FileのMOVEで別のWebDAVコレクション直下の存在するファイルが上書きモードで移動できること() {
        final String srcColName1 = "srcDavColforMOVE1";
        final String srcColName2 = "srcDavColforMOVE2";
        final String dstColName1 = "dstDavColforMOVE1";
        final String dstColName2 = "dstDavColforMOVE2";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    srcColName1 + "/" + srcColName2);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, dstColName1);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    dstColName1 + "/" + dstColName2);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName1 + "/" + srcColName2 + "/" + FILE_NAME,
                    FILE_BODY + "1", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + dstColName1 + "/" + dstColName2 + "/" + destFileName,
                    FILE_BODY + "2", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動（上書きモード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元ファイルが取得できないこと
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "1");
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFIND（移動元）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1), res, true);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName1 + "/" + srcColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName1, srcColName2, FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2), res, true);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstColName1 + "/" + dstColName2,
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstColName1, dstColName2, destFileName);
            checkPropfindResponse(href, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName1 + "/" + srcColName2 + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstColName1 + "/" + dstColName2 + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1 + "/" + srcColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName1, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1 + "/" + dstColName2, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstColName1, TOKEN, -1);
        }
    }

    /**
     * Etag情報からバージョン情報を取得.
     * @param etag Etag
     * @return バージョン
     */
    public static long getEtagVersion(String etag) {
        // version取得
        Pattern pattern = Pattern.compile("^\"([0-9]+)-([0-9]+)\"$");
        Matcher m = pattern.matcher(etag);
        return Long.parseLong(m.replaceAll("$1"));
    }

}

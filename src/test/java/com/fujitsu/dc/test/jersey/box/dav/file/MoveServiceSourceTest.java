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
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * ServiceSourceのMOVEのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveServiceSourceTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public MoveServiceSourceTest() {
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
     * ServiceSourceのMOVEで移動先が存在しないリソースの場合に移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで移動先が存在しないリソースの場合に移動できること() {
        final String colName = "serviceColforMOVE";
        final String destPath = "destResource";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destPath);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            String sourcePath = BOX_NAME + "/" + colName + "/__src/" + FILE_NAME;
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    sourcePath, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            String etag = response.getFirstHeader(HttpHeaders.ETAG);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME,
                    HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destPath, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destPath);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで移動元のファイルが存在しない場合に404エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで移動元のファイルが存在しない場合に404エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NOT_FOUND);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_NOT_FOUND;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src", FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = String.format("%s://%s/%s/%s/%s",
                DcCoreConfig.getUnitScheme(), "fqdn", CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, "another_box", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで移動先の1階層目の親リソースが存在しない場合409エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで移動先の1階層目の親リソースが存在しない場合409エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String invalidColName = "invalidCol";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, invalidColName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destination = UrlUtils.boxRoot(CELL_NAME, "dummyTestBoxForMove");
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダに存在するBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダに存在するBoxのURLを指定した場合に400エラーとなること() {
        final String colName = "serviceColforMOVE";
        final String destination = UrlUtils.boxRoot(CELL_NAME, BOX_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, colName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + colName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, colName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, colName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, colName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで移動先が存在するWebDavコレクションに対して上書きモードで移動する場合400エラーとなること.
     * TODO コレクションの移動を実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ServiceSourceのMOVEで移動先が存在するWebDavコレクションに対して上書きモードで移動する場合400エラーとなること() {
        final String svcColName = "serviceColforMOVE";
        final String davColName = "davColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, davColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, davColName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, davColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで移動先が存在するWebDavコレクションに対して上書き禁止モードで移動する場合412エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで移動先が存在するWebDavコレクションに対して上書き禁止モードで移動する場合412エラーとなること() {
        final String svcColName = "serviceColforMOVE";
        final String davColName = "davColforMOVE";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, davColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, davColName);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, davColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで移動先が存在するファイル配下のファイルの場合400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで移動先が存在するファイル配下のファイルの場合400エラーとなること() {
        final String svcColName = "serviceColforMOVE";
        final String parentFileName = "parent_file";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, parentFileName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + parentFileName, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
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
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, parentFileName);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在しないファイルに上書き禁止モードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在しないファイルに上書き禁止モードで移動できること() {
        final String svcColName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", FILE_NAME);
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
                    svcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    svcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在しないファイルに上書きモードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在しないファイルに上書きモードで移動できること() {
        final String svcColName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", FILE_NAME);
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
                    svcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    svcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在するファイルに上書き禁止モードで412エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在するファイルに上書き禁止モードで412エラーとなること() {
        final String svcColName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/" + destFileName,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", FILE_NAME);
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
                    svcColName + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    svcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在するファイルに上書きモードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで同一ServiceSourceコレクション直下の存在するファイルに上書きモードで移動できること() {
        final String svcColName = "serviceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/" + destFileName,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", FILE_NAME);
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
                    svcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    svcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在しないファイルに上書き禁止モードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在しないファイルに上書き禁止モードで移動できること() {
        final String srcSvcColName = "sourceServiceColforMOVE";
        final String dstSvcColName = "destServiceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcSvcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, dstSvcColName);

            // Fileの移動（上書き禁止モード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
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
                    srcSvcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
            checkPropfindResponse(href, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstSvcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在しないファイルが上書きモードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在しないファイルが上書きモードで移動できること() {
        final String srcSvcColName = "sourceServiceColforMOVE";
        final String dstSvcColName = "destServiceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcSvcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, dstSvcColName);

            // Fileの移動（上書きモード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
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
                    srcSvcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
            checkPropfindResponse(href, res, true);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstSvcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在するファイルが上書き禁止モードで412エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在するファイルが上書き禁止モードで412エラーとなること() {
        final String srcSvcColName = "sourceServiceColforMOVE";
        final String dstSvcColName = "destServiceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcSvcColName + "/__src/" + FILE_NAME, FILE_BODY + "1", MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, dstSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + dstSvcColName + "/__src/" + destFileName, FILE_BODY + "2", MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動（上書き禁止モード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
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
                    srcSvcColName + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "1");

            // 移動先ファイルを取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "2");

            // PROPFIND（移動元）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
            checkPropfindResponse(href, res, true);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
            checkPropfindResponse(href, res, true);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    srcSvcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstSvcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在するファイルが上書きモードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEで別のServiceSourceコレクション直下の存在するファイルが上書きモードで移動できること() {
        final String srcSvcColName = "sourceServiceColforMOVE";
        final String dstSvcColName = "destServiceColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcSvcColName + "/__src/" + FILE_NAME, FILE_BODY + "1", MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, dstSvcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + dstSvcColName + "/__src/" + destFileName, FILE_BODY + "2", MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動（上書きモード）
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
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
                    srcSvcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY + "1");
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFIND（移動元）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + srcSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            String href = UrlUtils.box(CELL_NAME, BOX_NAME, srcSvcColName, "__src", FILE_NAME);
            checkPropfindResponse(href, res, false);

            // PROPFIND（移動先）ができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + dstSvcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            href = UrlUtils.box(CELL_NAME, BOX_NAME, dstSvcColName, "__src", destFileName);
            checkPropfindResponse(href, res, true);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    dstSvcColName + "/__src/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, dstSvcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでWebDavコレクション直下の存在するファイルに上書きモードで移動できること.
     */
    @Test
    public final void ServiceSourceのMOVEでWebDavコレクション直下の存在するファイルに上書きモードで移動できること() {
        final String svcColName = "serviceColforMOVE";
        final String davColName = "davColforMOVE";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, davColName, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, davColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + davColName + "/" + destFileName,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcColName, "__src", FILE_NAME);
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
                    svcColName + "/__src/" + FILE_NAME, HttpStatus.SC_NOT_FOUND);

            // 移動先ファイルが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    davColName + "/" + destFileName, HttpStatus.SC_OK);
            assertThat(res.getBody()).isEqualTo(FILE_BODY);
            assertThat(res.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);

            // PROPFINDができること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + svcColName + "/__src/",
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, false);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + davColName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            checkPropfindResponse(destination, res, true);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME,
                    davColName + "/" + destFileName);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, davColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcColName, TOKEN, -1);
        }
    }
}

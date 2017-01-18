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
package com.fujitsu.dc.test.jersey.box.dav.col;

import static org.fest.assertions.Assertions.assertThat;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

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
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * MOVEのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public MoveCollectionTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * WebDAVコレクションのMOVEで同一Box直下の存在しないリソースに上書き禁止モードで移動できること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで同一Box直下の存在しないリソースに上書き禁止モードで移動できること() {
        final String srcColName = "srcCol";
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 移動
            String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName, HttpStatus.SC_NOT_FOUND);

            // 移動したコレクションが取得できること
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    destColName, HttpStatus.SC_OK);
            String expectedBody = "URL : " + destUrl;
            assertThat(res.getBody()).contains(expectedBody);

            // 移動したファイルが取得できること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    destColName + "/" + FILE_NAME, HttpStatus.SC_OK);

            // 移動したコレクション配下にファイルを追加
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destColName + "/" + FILE_NAME + "2", FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 元の場所に移動
            req = DcRequest.move(destUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, srcUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(destUrl, res);
            assertContainsHrefUrl(srcUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    destColName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, srcColName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, srcColName + "/" + FILE_NAME + "2");
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/" + FILE_NAME + "2");
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダの末尾にスラッシュを指定した場合無視されて移動できること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダの末尾にスラッシュを指定した場合無視されて移動できること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String destColUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName) + "/";
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);

            // 移動
            String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destColUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcColName, HttpStatus.SC_NOT_FOUND);

            // 内容確認
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destColName, HttpStatus.SC_OK);
            String expectedBody = "URL : " + destColUrl;
            assertThat(res.getBody()).contains(expectedBody);

            // 移動したコレクションにファイルを作成
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destColName + "/" + FILE_NAME,
                    FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = srcCol;
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = String.format("%s://%s/%s/%s/%s",
                DcCoreConfig.getUnitScheme(), "fqdn", CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, "another_box", destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destUrl = UrlUtils.boxRoot(CELL_NAME, "dummyTestBoxForMove");
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String notExistColName = "notExistCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, notExistColName, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);

            // Fileの移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName, destUrl, HttpStatus.SC_CONFLICT);
            DcCoreException expectedException = DcCoreException.Dav.HAS_NOT_PARENT.params(notExistColName);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで移動先にServiceコレクション配下を指定した場合に400が返却されること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで移動先にServiceコレクション配下を指定した場合に400が返却されること() {
        final String srcColName = "davColforMOVE";
        final String destSvcColName = "destServiceCol";
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destSvcColName, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destSvcColName);

            // 移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName, destUrl,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
                    expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで移動先にServiceコレクションの__src配下を指定した場合に400が返却されること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで移動先にServiceコレクションの__src配下を指定した場合に400が返却されること() {
        final String srcColName = "davColforMOVE";
        final String destSvcColName = "destServiceCol";
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destSvcColName, "__src", destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destSvcColName);

            // 移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName, destUrl,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException =
                    DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
                    expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destSvcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);

            // 移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName, destUrl,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, "__src");
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, "__src", FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException =
                    DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
                    expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * WebDAVコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に204となること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に204となること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダにファイル配下のURIを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダにファイル配下のURIを指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destFileName = "destFile";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_FILE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダにOdataコレクション配下のURIを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダにOdataコレクション配下のURIを指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, Setup.TEST_ODATA, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダにCellのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダにCellのURLを指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destUrl = UrlUtils.cellRoot(CELL_NAME);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダに別BoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダに別BoxのURLを指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destBox = "destBox";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, destBox, destCol);

        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            BoxUtils.create(CELL_NAME, destBox, TOKEN, HttpStatus.SC_CREATED);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            BoxUtils.delete(CELL_NAME, TOKEN, destBox, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEでDestinationヘッダに別コレクション配下のURLを指定した場合に移動できること.
     */
    @Test
    public final void WebDAVコレクションのMOVEでDestinationヘッダに別コレクション配下のURLを指定した場合に移動できること() {
        final String srcCol = "srcCol";
        final String destParentCol = "srcParentCol";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destParentCol, destCol);

        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destParentCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(url, res);
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destParentCol,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(destUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destParentCol + "/" + destCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destParentCol, TOKEN, -1);
        }
    }

    /**
     * WebDAVコレクションのMOVEで移動後にACLが取得できること.
     */
    @Test
    public final void WebDAVコレクションのMOVEで移動後にACLが取得できること() {
        final String srcCol = "srcCol";
        final String destParentCol = "srcParentCol";
        final String destCol = "destCol";
        final String srcPath = String.format("%s/%s/%s", CELL_NAME, BOX_NAME, srcCol);
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destParentCol, destCol);

        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destParentCol);
            // 移動前のコレクションにACLを設定
            DavResourceUtils.setACLPrincipalAll(CELL_NAME, TOKEN, HttpStatus.SC_OK, srcPath, "<D:read />", "");
            TResponse resBefore = DavResourceUtils.propfind(TOKEN,
                    CELL_NAME, BOX_NAME + "/" + srcCol, "1", HttpStatus.SC_MULTI_STATUS);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse resAfter = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destParentCol,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(url, resAfter);
            assertContainsHrefUrl(destUrl, resAfter);
            resAfter = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destParentCol + "/" + destCol,
                    "1", HttpStatus.SC_MULTI_STATUS);

            // 移動前と移動後のACL設定が変更されていないこと
            DavResourceUtils.assertEqualsNodeInResXml(resAfter, "acl",
                    resBefore.bodyAsXml().getElementsByTagName("acl").item(0));
            resAfter = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destParentCol + "/" + destCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destParentCol, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで同一Box直下の存在しないリソースに上書き禁止モードで移動できること.
     */
    @Test
    public final void ODataコレクションのMOVEで同一Box直下の存在しないリソースに上書き禁止モードで移動できること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        final String entityTypeName = "entity";
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            EntityTypeUtils.create(CELL_NAME, TOKEN, BOX_NAME, srcColName, entityTypeName, HttpStatus.SC_CREATED);

            // 移動
            String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);

            // 移動したコレクション配下のEntityTypeを取得
            EntityTypeUtils.get(CELL_NAME, TOKEN, BOX_NAME, destColName, entityTypeName, HttpStatus.SC_OK);

            // 移動したコレクション配下のファイルのサービス登録が解除されていないこと
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destColName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertIsODataCol(res);
        } finally {
            EntityTypeUtils.delete(srcColName, TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                    -1);
            EntityTypeUtils.delete(destColName, TOKEN, MediaType.APPLICATION_JSON, entityTypeName, BOX_NAME, CELL_NAME,
                    -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = srcCol;
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = String.format("%s://%s/%s/%s/%s",
                DcCoreConfig.getUnitScheme(), "fqdn", CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, "another_box", destCol);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destUrl = UrlUtils.boxRoot(CELL_NAME, "dummyTestBoxForMove");
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String notExistColName = "notExistCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, notExistColName, destColName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);

            // Fileの移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName, destUrl, HttpStatus.SC_CONFLICT);
            DcCoreException expectedException = DcCoreException.Dav.HAS_NOT_PARENT.params(notExistColName);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);

            // 移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName, destUrl,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ODataコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ODataコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ODataコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, "__src");
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            // TODO Serviceコレクション配下への移動の制限追加時にエラーコードのチェックをする
            // DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            // ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
            // expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void ODataコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * ODataコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に400エラーとなること. <br/>
     */
    @Test
    public final void ODataコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcColName);
            EntityTypeUtils.create(CELL_NAME, TOKEN, BOX_NAME, srcColName, "entity", HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName, HttpStatus.SC_NOT_FOUND);

            // 移動したコレクションが取得できること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName,
                    "0", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(destUrl, res);

            // 移動したコレクション配下のEntityTypeが取得できること
            EntityTypeUtils.get(CELL_NAME, TOKEN, BOX_NAME, destFileName, "entity", HttpStatus.SC_OK);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
            EntityTypeUtils.delete(srcColName, TOKEN, MediaType.APPLICATION_JSON, "entity", BOX_NAME, CELL_NAME, -1);
            EntityTypeUtils.delete(destFileName, TOKEN, MediaType.APPLICATION_JSON, "entity", BOX_NAME, CELL_NAME, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFileName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで同一Box直下の存在しないリソースに上書きモードで移動できること.
     */
    @Test
    public final void ServiceコレクションのMOVEで同一Box直下の存在しないリソースに上書きモードで移動できること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);
            DavResourceUtils.setServiceProppatch(TOKEN, HttpStatus.SC_MULTI_STATUS, CELL_NAME, BOX_NAME, srcColName,
                    FILE_NAME, "test");

            // 移動
            String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName, HttpStatus.SC_NOT_FOUND);

            // 移動したコレクション配下のファイルを取得
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destColName + "/__src/" + FILE_NAME,
                    HttpStatus.SC_OK);

            // 移動したコレクション配下のファイルのサービス登録が解除されていないこと
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destColName,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertIsServiceCol(res);
            assertContainsHrefUrl(destUrl + "/__src", res);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, srcColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEでDestinationヘッダの値がリクエストURLと同じ場合に403エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = srcCol;
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_EQUALS_SOURCE_URL.params(destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEでDestinationヘッダに移動元とホスト名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = String.format("%s://%s/%s/%s/%s",
                DcCoreConfig.getUnitScheme(), "fqdn", CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEでDestinationヘッダに移動元とBox名が異なる文字列を指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, "another_box", destCol);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEでDestinationヘッダに存在しないBoxのURLを指定した場合に400エラーとなること() {
        final String srcCol = "srcCol";
        final String destUrl = UrlUtils.boxRoot(CELL_NAME, "dummyTestBoxForMove");
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで移動先の親リソースが存在しない場合に409エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destColName = "destCol";
        final String notExistColName = "notExistCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, notExistColName, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);

            // コレクションの移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + srcColName, destUrl, HttpStatus.SC_CONFLICT);
            DcCoreException expectedException = DcCoreException.Dav.HAS_NOT_PARENT.params(notExistColName);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するBoxを指定した場合に400エラーとなること() {
        final String srcColName = "davColforMOVE";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);

            // 移動
            TResponse response = DavResourceUtils.moveWebDav(TOKEN, CELL_NAME, BOX_NAME + "/" + srcColName, destUrl,
                    HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するWebDAVコレクションに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するWebDAVコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するODataコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createODataCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること. <br/>
     * TODO コレクションの上書きを実装した場合は、正常終了(204)となる。
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するServiceコレクションに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するServiceコレクションの__srcに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, "__src");
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するServiceソースファイルに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destColName = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destColName, FILE_NAME);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, destColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + destColName + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            // TODO Serviceコレクション配下への移動の制限追加時にエラーコードのチェックをする
            // DcCoreException expectedException = DcCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            // ODataCommon.checkErrorResponseBody(response, expectedException.getCode(),
            // expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destColName + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destColName, TOKEN, -1);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するWebDAVファイルに上書き禁止モードで移動した場合に412エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "F");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_PRECONDITION_FAILED);
            DcCoreException expectedException = DcCoreException.Dav.DESTINATION_ALREADY_EXISTS;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColName, TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * ServiceコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に400エラーとなること.
     */
    @Test
    public final void ServiceコレクションのMOVEで存在するWebDAVファイルに上書きモードで移動した場合に400エラーとなること() {
        final String srcColName = "srcCol";
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColName);
        final String destFileName = "destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, srcColName);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName, FILE_BODY,
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // 移動
            DcRequest req = DcRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

            // 存在確認
            TResponse res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME,
                    "1", HttpStatus.SC_MULTI_STATUS);
            assertNotContainsHrefUrl(srcUrl, res);
            assertContainsHrefUrl(destUrl, res);
            res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME,
                    srcColName, HttpStatus.SC_NOT_FOUND);

            // 移動したコレクションが取得できること
            res = DavResourceUtils.propfind(TOKEN, CELL_NAME, BOX_NAME + "/" + destFileName,
                    "0", HttpStatus.SC_MULTI_STATUS);
            assertContainsHrefUrl(destUrl, res);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFileName, TOKEN, -1);
        }
    }

    /**
     * PROPFINDの結果にODataコレクションが含まれるかどうかをチェックする.
     * @param res PROPFINDレスポンス
     */
    private void assertIsODataCol(TResponse res) {
        DavResourceUtils.assertIsODataCol(res);
    }

    /**
     * PROPFINDの結果にServiceコレクションが含まれるかどうかをチェックする.
     * @param res PROPFINDレスポンス
     */
    private void assertIsServiceCol(TResponse res) {
        DavResourceUtils.assertIsServiceCol(res);
    }

    /**
     * PROPFINDの結果にhref タグで指定されたURLが含まれていることをチェックする.
     * @param expectedUrl 期待するURL
     * @param res PROPFINDレスポンス
     */
    private void assertContainsHrefUrl(
            final String expectedUrl,
            TResponse res) {
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
    }

    /**
     * PROPFINDの結果にhref タグで指定されたURLが含まれていないことをチェックする.
     * @param expectedUrl 期待するURL
     * @param res PROPFINDレスポンス
     */
    private void assertNotContainsHrefUrl(
            final String expectedUrl,
            TResponse res) {
        DavResourceUtils.assertNotContainsHrefUrl(expectedUrl, res);
    }

    // /**
    // * PROPFINDの結果にACEが含まれるかどうかをチェックする. <br />
    // * @param res PROPFINDレスポンス
    // */
    // public static void assertContainAce(TResponse res) {
    // NodeList list = res.bodyAsXml().getElementsByTagNameNS("acl", "ace");
    // assertThat(list.getLength()).isGreaterThan(0);
    // }

}

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
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

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

/**
 * コレクションに対するMOVEメソッドのヘッダー指定に関する妥当性検証を実装するクラス. <br />
 * 本クラスでは、ヘッダーの指定内容に関する検証を実施するため、エラーレスポンスに対しては以下のチェックを実施する。
 * <ul>
 * <li>ステータスコード</li>
 * <li>エラー時：エラーコード</li>
 * <li>エラー時：エラーメッセージ</li>
 * </ul>
 * @see com.fujitsu.dc.test.jersey.box.dav.file.MoveFileTest
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionHeaderValidateTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";

    /**
     * コンストラクタ.
     */
    public MoveCollectionHeaderValidateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * コレクションのMOVEでDestinationヘッダを省略した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDestinationヘッダを省略した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
                    HttpHeaders.DESTINATION);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDestinationヘッダに空文字を指定した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDestinationヘッダに空文字を指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destUrl = "";
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
            DcCoreException expectedException = DcCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
                    HttpHeaders.DESTINATION, destUrl);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDestinationヘッダにURI形式でない文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDestinationヘッダにURI形式でない文字列を指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destUrl = "http/?#/dest#?#://destCol";
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
     * コレクションのMOVEでDestinationヘッダにスキームがFTPとなるURIを指定した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDestinationヘッダにスキームがFTPとなるURIを指定した場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol).replaceAll("http[s]{0,1}", "ftp");
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
     * コレクションのMOVEでOverwriteヘッダを省略した場合に移動先が存在しないリソースへの移動ができること.
     */
    @Test
    public final void コレクションのMOVEでOverwriteヘッダを省略した場合に移動先が存在しないリソースへの移動ができること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
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

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでOverwriteヘッダが空文字の場合に400エラーになること.
     */
    @Test
    public final void コレクションのMOVEでOverwriteヘッダが空文字の場合に400エラーになること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.OVERWRITE, "");
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "Y");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.OVERWRITE, "Y");
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDepthヘッダを省略した場合に移動ができること.
     */
    @Test
    public final void コレクションのMOVEでDepthヘッダを省略した場合に移動ができること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            DcResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDepthヘッダに空文字を指定した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDepthヘッダに空文字を指定した場合に400エラーとなること() {
        final String depth = "";
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDepthヘッダにinfinityを指定した場合に正常に移動できること.
     */
    @Test
    public final void コレクションのMOVEでDepthヘッダにinfinityを指定した場合に正常に移動できること() {
        final String depth = "infinity";
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDepthヘッダにINFInITYを指定した場合に正常に移動できること.
     */
    @Test
    public final void コレクションのMOVEでDepthヘッダにINFInITYを指定した場合に正常に移動できること() {
        final String depth = "INFInITY";
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void コレクションのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること() {
        final String depth = "1";
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでIf_Matchヘッダを省略した場合に移動できること.
     */
    @Test
    public final void コレクションのMOVEでIf_Matchヘッダを省略した場合に移動できること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.DEPTH, "infinity");
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでIf_Matchヘッダに空文字を指定した場合に400エラーになること.
     */
    @Test
    public final void コレクションのMOVEでIf_Matchヘッダに空文字を指定した場合に400エラーになること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, "");
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.ETAG_NOT_MATCH;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでIf_Matchヘッダにアスタリスクを指定した場合に移動できること.
     */
    @Test
    public final void コレクションのMOVEでIf_Matchヘッダにアスタリスクを指定した場合に移動できること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.IF_MATCH, "*");
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること.
     */
    @Test
    public final void コレクションのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            String etag = res.getHeader(HttpHeaders.ETAG);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag + "dummy");
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.ETAG_NOT_MATCH;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            // 移動先のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

    /**
     * コレクションのMOVEでIf_Matchヘッダに存在するドキュメントバージョンを指定すると移動できること.
     */
    @Test
    public final void コレクションのMOVEでIf_Matchヘッダに存在するドキュメントバージョンを指定すると移動できること() {
        final String srcCol = "davColforMOVE";
        final String destCol = "destCol";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destCol);
        try {
            // 事前準備
            DavResourceUtils.createWebDavCollection(TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, srcCol);
            TResponse res = DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_OK);
            String etag = res.getHeader(HttpHeaders.ETAG);

            // 移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, srcCol);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag);
            DcResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            // 移動元のコレクションが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, srcCol, HttpStatus.SC_NOT_FOUND);
            // 移動先のコレクションが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destCol, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcCol, TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destCol, TOKEN, -1);
        }
    }

}

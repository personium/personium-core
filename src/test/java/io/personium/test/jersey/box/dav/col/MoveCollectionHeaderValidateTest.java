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
package io.personium.test.jersey.box.dav.col;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * コレクションに対するMOVEメソッドのヘッダー指定に関する妥当性検証を実装するクラス. <br />
 * 本クラスでは、ヘッダーの指定内容に関する検証を実施するため、エラーレスポンスに対しては以下のチェックを実施する。
 * <ul>
 * <li>ステータスコード</li>
 * <li>エラー時：エラーコード</li>
 * <li>エラー時：エラーメッセージ</li>
 * </ul>
 * @see io.personium.test.jersey.box.dav.file.MoveFileTest
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionHeaderValidateTest extends PersoniumTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";

    /**
     * コンストラクタ.
     */
    public MoveCollectionHeaderValidateTest() {
        super(new PersoniumCoreApplication());
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException =
                    PersoniumCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException =
                    PersoniumCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "");
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "Y");
            PersoniumResponse response = AbstractCase.request(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            PersoniumResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, depth);
            PersoniumResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.DEPTH, "infinity");
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, "");
            PersoniumResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.ETAG_NOT_MATCH;
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.IF_MATCH, "*");
            PersoniumResponse response = AbstractCase.request(req);

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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag + "dummy");
            PersoniumResponse response = AbstractCase.request(req);

            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.ETAG_NOT_MATCH;
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
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.DEPTH, "infinity");
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag);
            PersoniumResponse response = AbstractCase.request(req);

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

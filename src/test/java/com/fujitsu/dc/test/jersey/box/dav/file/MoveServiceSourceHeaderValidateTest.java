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
 * ServiceSourceに対するMOVEメソッドのヘッダー指定に関する妥当性検証を実装するクラス. <br />
 * 本クラスでは、ヘッダーの指定内容に関する検証を実施するため、エラーレスポンスに対しては以下のチェックを実施する。
 * <ul>
 * <li>ステータスコード</li>
 * <li>エラー時：エラーコード</li>
 * <li>エラー時：エラーメッセージ</li>
 * </ul>
 * なお、現状では、サービスソースとWebDAVファイルで実装が同じため、一部のエラー系のチェックのみ実施している.
 * @see com.fujitsu.dc.test.jersey.box.dav.file.MoveFileTest
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveServiceSourceHeaderValidateTest extends JerseyTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public MoveServiceSourceHeaderValidateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ServiceSourceのMOVEでDestinationヘッダを省略した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDestinationヘッダを省略した場合に400エラーとなること() {
        final String svcCol = "serviceCol";
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcCol);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcCol + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcCol, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
                    HttpHeaders.DESTINATION);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること() {
        final String svcCol = "serviceCol";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcCol);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcCol + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcCol, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "Y");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            DcCoreException expectedException = DcCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.OVERWRITE, "Y");
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils
                    .getWebDav(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void ServiceSourceのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること() {
        final String svcCol = "serviceCol";
        final String depth = "1";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcCol);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcCol + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcCol, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils
                    .getWebDav(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcCol, TOKEN, -1);
        }
    }

    /**
     * ServiceSourceのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること.
     */
    @Test
    public final void ServiceSourceのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること() {
        final String svcCol = "serviceCol";
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, svcCol);
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + svcCol + "/__src/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 正しいEtag値の取得
            TResponse res = DavResourceUtils.getWebDav(
                    CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            String etag = res.getHeader(HttpHeaders.ETAG);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, svcCol, "__src", FILE_NAME);
            DcRequest req = DcRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag + "dummy");

            // リクエスト実行
            DcResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            DcCoreException expectedException = DcCoreException.Dav.ETAG_NOT_MATCH;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils
                    .getWebDav(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, svcCol + "/__src/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, svcCol, TOKEN, -1);
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

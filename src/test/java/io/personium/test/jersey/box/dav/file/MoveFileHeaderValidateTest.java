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
package io.personium.test.jersey.box.dav.file;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

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
 * WebDAVファイルに対するMOVEメソッドのヘッダー指定に関する妥当性検証を実装するクラス. <br />
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
public class MoveFileHeaderValidateTest extends PersoniumTest {
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";

    /**
     * コンストラクタ.
     */
    public MoveFileHeaderValidateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * FileのMOVEでDestinationヘッダを省略した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダを省略した場合に400エラーとなること() {
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException =
                    PersoniumCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
                    HttpHeaders.DESTINATION);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダに空文字を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダに空文字を指定した場合に400エラーとなること() {
        final String destination = "";
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException =
                    PersoniumCoreException.Dav.REQUIRED_REQUEST_HEADER_NOT_EXIST.params(
                            HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダにURI形式でない文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダにURI形式でない文字列を指定した場合に400エラーとなること() {
        final String destination = "http/?#/dest#?#://destFile.txt";
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでDestinationヘッダにスキームがFTPとなるURIを指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDestinationヘッダにスキームがFTPとなるURIを指定した場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName).replaceAll("http[s]{0,1}", "ftp");
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.DESTINATION, destination);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
        }
    }

    /**
     * FileのMOVEでOverwriteヘッダを省略した場合に移動先が存在しないリソースへの移動ができること.
     */
    @Test
    public final void FileのMOVEでOverwriteヘッダを省略した場合に移動先が存在しないリソースへの移動ができること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでOverwriteヘッダが空文字の場合に400エラーになること.
     */
    @Test
    public final void  FileのMOVEでOverwriteヘッダが空文字の場合に400エラーになること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.OVERWRITE, "");
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでOverwriteヘッダにTを指定し移動先にリソースが存在しない場合に移動できること.
     */
    @Test
    public final void FileのMOVEでOverwriteヘッダにTを指定し移動先にリソースが存在しない場合に移動できること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでOverwriteヘッダにFを指定し移動先にリソースが存在しない場合に移動できること.
     */
    @Test
    public final void FileのMOVEでOverwriteヘッダにFを指定し移動先にリソースが存在しない場合に移動できること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "F");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでOverwriteヘッダに許可しない文字列を指定し移動先にリソースが存在しない場合に400エラーとなること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "Y");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    HttpHeaders.OVERWRITE, "Y");
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでDepthヘッダを省略した場合に移動ができること.
     */
    @Test
    public final void FileのMOVEでDepthヘッダを省略した場合に移動ができること() {
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_CREATED);

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }


    /**
     * FileのMOVEでDepthヘッダに空文字を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDepthヘッダに空文字を指定した場合に400エラーとなること() {
        final String depth = "";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでDepthヘッダにinfinityを指定した場合に正常に移動できること.
     */
    @Test
    public final void FileのMOVEでDepthヘッダにinfinityを指定した場合に正常に移動できること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでDepthヘッダにINFInITYを指定した場合に正常に移動できること.
     */
    @Test
    public final void FileのMOVEでDepthヘッダにINFInITYを指定した場合に正常に移動できること() {
        final String depth = "INFInITY";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること.
     */
    @Test
    public final void FileのMOVEでDepthヘッダにinfinity以外の文字列を指定した場合に400エラーとなること() {
        final String depth = "1";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでIf_Matchヘッダを省略した場合に移動できること.
     */
    @Test
    public final void FileのMOVEでIf_Matchヘッダを省略した場合に移動できること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでIf_Matchヘッダに空文字を指定した場合に400エラーになること.
     */
    @Test
    public final void FileのMOVEでIf_Matchヘッダに空文字を指定した場合に400エラーになること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, "");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.ETAG_NOT_MATCH;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでIf_Matchヘッダにアスタリスクを指定した場合に移動できること.
     */
    @Test
    public final void FileのMOVEでIf_Matchヘッダにアスタリスクを指定した場合に移動できること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }

    /**
     * FileのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること.
     */
    @Test
    public final void FileのMOVEでIf_Matchヘッダに存在しないドキュメントバージョンを指定した場合に412エラーになること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            TResponse res = DavResourceUtils.getWebDav(
                    CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            String etag = res.getHeader(HttpHeaders.ETAG);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag + "dummy");

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_PRECONDITION_FAILED, response.getStatusCode());
            PersoniumCoreException expectedException = PersoniumCoreException.Dav.ETAG_NOT_MATCH;
            ODataCommon.checkErrorResponseBody(response, expectedException.getCode(), expectedException.getMessage());

            // 移動元のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            // 移動先のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_NOT_FOUND);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
        }
    }


    /**
     * FileのMOVEでIf_Matchヘッダに存在するドキュメントバージョンを指定すると移動できること.
     */
    @Test
    public final void FileのMOVEでIf_Matchヘッダに存在するドキュメントバージョンを指定すると移動できること() {
        final String depth = "infinity";
        final String destFileName = "destFile.txt";
        final String destination = UrlUtils.box(CELL_NAME, BOX_NAME, destFileName);
        try {
            // 事前準備
            DavResourceUtils.createWebDavFile(TOKEN, CELL_NAME,
                    BOX_NAME + "/" + FILE_NAME, FILE_BODY, MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            TResponse res = DavResourceUtils.getWebDav(
                    CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_OK);
            String etag = res.getHeader(HttpHeaders.ETAG);

            // Fileの移動
            String url = UrlUtils.box(CELL_NAME, BOX_NAME, FILE_NAME);
            PersoniumRequest req = PersoniumRequest.move(url);
            req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.DESTINATION, destination);
            req.header(HttpHeaders.DEPTH, depth);
            req.header(HttpHeaders.OVERWRITE, "T");
            req.header(HttpHeaders.IF_MATCH, etag);

            // リクエスト実行
            PersoniumResponse response = AbstractCase.request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 移動元のファイルが存在しないこと
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME, HttpStatus.SC_NOT_FOUND);
            // 移動先のファイルが存在すること
            DavResourceUtils.getWebDav(CELL_NAME, TOKEN, BOX_NAME, destFileName, HttpStatus.SC_OK);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, FILE_NAME);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, TOKEN, BOX_NAME, destFileName);
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

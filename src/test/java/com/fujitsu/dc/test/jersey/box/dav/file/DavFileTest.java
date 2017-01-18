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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreConfig.BinaryData;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * DAV File related tests.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class DavFileTest extends JerseyTest {
    private static final String CELL_NAME = "testcell1";
    private static final String TEST_BOX1 = "box1";
    private static final String FILE_NAME = "file1.txt";
    private static final String FILE_BODY = "testFileBody";
    private static final String FILE_BODY2 = "testFileBody2";

    /**
     * constructor.
     */
    public DavFileTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * IfNoneMatch指定なしでFileをGetした場合に200が返却されること.
     */
    @Test
    public final void IfNoneMatch指定なしでFileをGetした場合に200が返却されること() {
        try {
            // ファイル新規作成
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Etag取得
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);

            // ファイル取得
            TResponse getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, null)
                    .returns();
            getResp.statusCode(HttpStatus.SC_OK);
            assertEquals(FILE_BODY, getResp.getBody());

        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }

    }

    /**
     * IfNoneMatchでに正しい形式の値を指定してFileをGetした場合に指定が有効になること.
     */
    @Test
    public final void returns_304_on_GET_with_matching_ETag_in_IfNoneMatch_header() {
        try {
            // create new DAV file for this test
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // Retrieve Etag header
            String etag1 = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag1);

            // Should return 304 on GET request with
            // matching ETag specified in If-None-Match.
            TResponse getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, etag1)
                    .returns();
            getResp.statusCode(HttpStatus.SC_NOT_MODIFIED);
            // Body Should be empty.
            assertEquals("", getResp.getBody());
            // Same ETag should be returned again
            assertEquals(etag1, resp.getHeader(HttpHeaders.ETAG));

            // Weak ETag format should also work.
            String wEtag = "W/" + etag1;
            getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, wEtag)
                    .returns();
            getResp.statusCode(HttpStatus.SC_NOT_MODIFIED);
            // Body Should be empty.
            assertEquals("", getResp.getBody());
            // Same ETag should be returned again
            assertEquals(etag1, resp.getHeader(HttpHeaders.ETAG));

            // Update the DAV File
            resp = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);

            // retrieve new Etag
            String etag2 = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag2);

            // Should return 200 on GET request with
            // non-matching ETag specified in If-None-Match.
            getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, etag1)
                    .returns();
            getResp.statusCode(HttpStatus.SC_OK);
            assertEquals(FILE_BODY, getResp.getBody());
            assertEquals(etag2, resp.getHeader(HttpHeaders.ETAG));

        } finally {
            // delete the DAV file for this test
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }

    }

    /**
     * IfNoneMatchにアスタを指定してFileをGetした場合に200が返却されること.
     */
    @Test
    public final void returns_200_on_GET_with_asterisk_in_IfNoneMatch_header() {
        try {
            // create new DAV file for this test
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // retrieve Etag header
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);

            // ファイル取得
            TResponse getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, "*")
                    .returns();
            getResp.statusCode(HttpStatus.SC_OK);
            assertEquals(FILE_BODY, getResp.getBody());
            assertEquals(etag, resp.getHeader(HttpHeaders.ETAG));

        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }

    }

    /**
     * It should return 200 on GET with invalid value in IfNoneMatch header.
     */
    @Test
    public final void returns_200_on_GET_with_invalid_value_in_IfNoneMatch_header() {
        try {
            // create new DAV file for this test
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // retrieve Etag header
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);

            // If-None-Matchヘッダに空文字を指定した場合に、ヘッダが無視されてファイルを取得できること
            TResponse getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, "")
                    .returns();
            getResp.statusCode(HttpStatus.SC_OK);
            assertEquals(FILE_BODY, getResp.getBody());
            assertEquals(etag, resp.getHeader(HttpHeaders.ETAG));

            // If-None-MatchヘッダにEtag形式でない文字列を指定した場合に、ヘッダが無視されてファイルを取得できること
            getResp = this.getFileRequest(FILE_NAME, TEST_BOX1, "abc!abc!abc!abc!abc!abc!")
                    .returns();
            getResp.statusCode(HttpStatus.SC_OK);
            assertEquals(FILE_BODY, getResp.getBody());
            assertEquals(etag, resp.getHeader(HttpHeaders.ETAG));

        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }

    }

    /**
     * FileをPUTで新規配置しそれが取得できる.
     */
    @Test
    public final void FileをPUTで新規配置するテスト() {
        try {
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);

            this.getFileRequest(FILE_NAME, TEST_BOX1, null)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            this.getFileRequest(FILE_NAME, TEST_BOX1, etag)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_MODIFIED);

            long expectedSize = FILE_BODY.getBytes().length;
            String fileSize = null;
            // PROPFINDでDavファイルのContentLengthが正しく取得できるかチェック
            TResponse res = Http.request("box/propfind-box-allprop.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("path", Setup.TEST_BOX1)
                    .with("depth", "1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root = res.bodyAsXml().getDocumentElement();
            NodeList resElems = root.getElementsByTagName("response");
            assertTrue(0 < resElems.getLength());
            for (int i = 0; i < resElems.getLength(); i++) {
                Element elem = (Element) resElems.item(i);
                String davUrl = elem.getElementsByTagName("href").item(0).getTextContent();
                if (!davUrl.endsWith("/" + FILE_NAME)) {
                    continue;
                }
                fileSize = elem.getElementsByTagName("getcontentlength").item(0).getTextContent();
            }
            assertNotNull(fileSize);
            assertEquals(expectedSize, Long.parseLong(fileSize));
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * FileをEtagなしPUTで更新する.
     */
    @Test
    public final void FileをEtagなしPUTで更新する() {
        try {
            TResponse resp = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);
            TResponse resp2 = this.putFileRequest(FILE_NAME, FILE_BODY2, null, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            String etag2 = resp2.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag2);
            assertFalse(etag.equals(etag2));
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * FileをEtagなしPUTで更新し更新後データを正常に取得できる.
     */
    @Test
    public final void FileをEtagなしPUTで更新し更新後もデータを正常に取得できる() {
        try {
            // PUT
            TResponse resp = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);
            // GET
            TResponse getRes = this.getFileRequest(FILE_NAME, TEST_BOX1, null)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            int cl1 = Integer.parseInt(getRes.getHeader(HttpHeaders.CONTENT_LENGTH));
            assertEquals(FILE_BODY.length(), cl1);
            // PUT 
            TResponse resp2 = this.putFileRequest(FILE_NAME, FILE_BODY2, null, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            String etag2 = resp2.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag2);
            // GET 
            getRes = this.getFileRequest(FILE_NAME, TEST_BOX1, null)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
            int cl2 = Integer.parseInt(getRes.getHeader(HttpHeaders.CONTENT_LENGTH));
            assertEquals(FILE_BODY2.length(), cl2);
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    
    /**
     * FileをETAGつきPUTで更新してDELETEする.
     */
    @Test
    public final void FileをETAGつきPUTで更新してDELETEする() {
        try {
            TResponse resp = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);
            TResponse resp2 = this.putFileRequest(FILE_NAME, FILE_BODY2, etag, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            String etag2 = resp2.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag2);
            assertFalse(etag.equals(etag2));
            this.deleteFileRequest(FILE_NAME, etag2, Setup.TEST_BOX1)
                    .returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND);
            // 既に消えているはずなので404を期待
        }
    }

    /**
     * FileをPUTでの格納フォルダ重複エラーを確認する.
     * テストの実施に時間が掛かり過ぎる（約3分）ため、本テストは無効化する。
     * なお、既存ディレクトリの存在チェックテストは、以下ユニットテストで確認する。
     * BinaryDataAccessorTest#既存ディレクトリにファイルの登録が可能な事を確認する()
     */
    @Test
    @Ignore
    public final void FileをPUTでの格納フォルダ重複エラーを確認する() {
        String filaName = "test";
        String txt = ".txt";
        int makeFileNum = 1500;
        try {
            // ファイルを1500作成し、"File System Inconsistency Detected."を発生しない事を確認
            for (int i = 0; i < makeFileNum; i++) {
                final Http theReq = this.putFileRequest(filaName + i + txt, FILE_BODY, null, Setup.TEST_BOX1);
                theReq.returns().statusCode(HttpStatus.SC_CREATED);
            }
            // test1499.txtが作成されていることを確認する
            final Http theReq = this.getFileRequest(filaName + "1499" + txt, Setup.TEST_BOX1, null);
            theReq.returns().statusCode(HttpStatus.SC_OK);

        } finally {
            for (int i = 0; i < makeFileNum; i++) {
                this.deleteFileRequest(filaName + i + txt, null, Setup.TEST_BOX1).returns();
            }
        }
    }

    /**
     * FileをfsyncでPUTする.
     */
    @Test
    public final void FileをfsyncでPUTする() {
        // fsyncを有効にする
        boolean fsyncEnabled = DcCoreConfig.getFsyncEnabled();
        DcCoreConfig.set(BinaryData.FSYNC_ENABLED, "true");
        try {
            final Http theReq = this.putFileRequest(FILE_NAME, FILE_BODY, null, Setup.TEST_BOX1);
            TResponse resp = theReq.returns()
                    .statusCode(HttpStatus.SC_CREATED);
            String etag = resp.getHeader(HttpHeaders.ETAG);
            assertNotNull(etag);

            this.getFileRequest(FILE_NAME, TEST_BOX1, null)
                    .returns()
                    .statusCode(HttpStatus.SC_OK);

            this.getFileRequest(FILE_NAME, TEST_BOX1, etag)
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_MODIFIED);

            long expectedSize = FILE_BODY.getBytes().length;
            String fileSize = null;
            // PROPFINDでDavファイルのContentLengthが正しく取得できるかチェック
            TResponse res = Http.request("box/propfind-box-allprop.txt")
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("path", Setup.TEST_BOX1)
                    .with("depth", "1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_MULTI_STATUS);
            Element root = res.bodyAsXml().getDocumentElement();
            NodeList resElems = root.getElementsByTagName("response");
            assertTrue(0 < resElems.getLength());
            for (int i = 0; i < resElems.getLength(); i++) {
                Element elem = (Element) resElems.item(i);
                String davUrl = elem.getElementsByTagName("href").item(0).getTextContent();
                if (!davUrl.endsWith("/" + FILE_NAME)) {
                    continue;
                }
                fileSize = elem.getElementsByTagName("getcontentlength").item(0).getTextContent();
            }
            assertNotNull(fileSize);
            assertEquals(expectedSize, Long.parseLong(fileSize));
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            DcCoreConfig.set(BinaryData.FSYNC_ENABLED, String.valueOf(fsyncEnabled));
        }
    }

    /**
     * Rangeヘッダ指定でファイルの部分取得.
     */
    @Test
    public final void Rangeヘッダ指定でファイルの部分取得() {
        try {
            String body = "abcdefghijklmn";

            // ファイル新規作成
            final Http theReq = this.putFileRequest(FILE_NAME, body, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_CREATED);

            // ファイル取得
            int first = 2;
            int last = 10;
            String rangeHeader = String.format("bytes=%s-%s", first, last);
            TResponse getResp = this.getFileRequestAtRange(FILE_NAME, TEST_BOX1, rangeHeader)
                    .returns();

            getResp.statusCode(HttpStatus.SC_PARTIAL_CONTENT);

            assertEquals(String.format("bytes %s-%s/%s", first, last, body.length()),
                    getResp.getHeader(DcCoreUtils.HttpHeaders.CONTENT_RANGE));

            assertEquals(body.substring(first, last + 1), getResp.getBody());
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * 無効なRangeヘッダ指定でファイル全体取得になること.
     */
    @Test
    public final void 無効なRangeヘッダ指定でファイル全体取得になること() {
        try {
            String body = "abcdefghijklmn";

            // ファイル新規作成
            final Http theReq = this.putFileRequest(FILE_NAME, body, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_CREATED);

            // ファイル取得
            String rangeHeader = "bytes=a-b,1-2";
            TResponse getResp = this.getFileRequestAtRange(FILE_NAME, TEST_BOX1, rangeHeader)
                    .returns();

            getResp.statusCode(HttpStatus.SC_OK);

            assertEquals(body, getResp.getBody());
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * Rangeヘッダ指定範囲誤りで416レスポンスが返却されること.
     */
    @Test
    public final void Rangeヘッダ指定範囲誤りで416レスポンスが返却されること() {
        try {
            String body = "abcdefghijklmn";

            // ファイル新規作成
            final Http theReq = this.putFileRequest(FILE_NAME, body, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_CREATED);

            // ファイル取得
            int first = body.length() + 100;
            int last = first + 10;
            String rangeHeader = String.format("bytes=%s-%s", first, last);
            TResponse getResp = this.getFileRequestAtRange(FILE_NAME, TEST_BOX1, rangeHeader)
                    .returns();

            getResp.statusCode(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * Rangeヘッダで複数範囲指定すると501レスポンスが返却されること.
     * MultiPartレスポンスを制限事項とするため501を返却する.
     */
    @Test
    public final void Rangeヘッダで複数範囲指定すると501レスポンスが返却されること() {
        try {
            String body = "abcdefghijklmn";

            // ファイル新規作成
            final Http theReq = this.putFileRequest(FILE_NAME, body, null, Setup.TEST_BOX1);
            theReq.returns().statusCode(HttpStatus.SC_CREATED);

            // ファイル取得
            String rangeHeader = "bytes=1-2,3-4";
            TResponse getResp = this.getFileRequestAtRange(FILE_NAME, TEST_BOX1, rangeHeader)
                    .returns();

            getResp.statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } finally {
            this.deleteFileRequest(FILE_NAME, null, Setup.TEST_BOX1).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * File取得リクエストを生成.
     * @param boxName box名
     * @param col ロール名
     * @return リクエストオブジェクト
     */
    Http getFileRequest(String fileName, String boxName, String etag) {
        if (etag == null) {
            return Http.request("box/dav-get.txt")
                    .with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", boxName)
                    .with("path", fileName);
        } else {
            return Http.request("box/dav-get-ifnonematch.txt")
                    .with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("etag", etag)
                    .with("box", boxName)
                    .with("path", fileName);
        }
    }

    /**
     * Rangeヘッダ指定File取得リクエストを生成.
     * @param fileName リソースファイル名
     * @param boxName ボックス名
     * @param rangeHeader Rangeヘッダ指定
     * @return リクエストオブジェクト
     */
    Http getFileRequestAtRange(String fileName, String boxName, String rangeHeader) {
        return Http.request("box/dav-get-range.txt")
                .with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", boxName)
                .with("path", fileName)
                .with("range-field", rangeHeader);
    }

    /**
     * File作成リクエストを生成.
     * @param boxName box名
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http putFileRequest(String fileName, String fileBody, String etag, String boxName) {
        if (etag == null) {
            return Http.request("box/dav-put.txt")
                    .with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("path", fileName)
                    .with("box", boxName)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("source", fileBody);
        } else {
            return Http.request("box/dav-put-ifmatch.txt")
                    .with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("path", fileName)
                    .with("box", boxName)
                    .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                    .with("etag", etag)
                    .with("source", fileBody);
        }
    }

    /**
     * File削除リクエストを生成.
     * @param boxName box名
     * @param col ロール名
     * @return リクエストオブジェクト
     */
    Http deleteFileRequest(String fileName, String etag, String boxName) {
        if (etag == null) {
            return Http.request("box/dav-delete.txt")
                    .with("cellPath", CELL_NAME)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", boxName)
                    .with("path", fileName);
        } else {
            return Http.request("box/dav-delete-ifmatch.txt")
                    .with("cellPath", CELL_NAME)
                    .with("etag", etag)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("box", boxName)
                    .with("path", fileName);
        }
    }

}

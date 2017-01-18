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
package com.fujitsu.dc.test.jersey.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Log APIのテスト.
 * 本クラスのテストでは、過去ログに対するテストを行うため、事前に過去ログを作成しておく必要がある。 <br />
 * EventLogのローテートは、50MBのサイズローテートを行っている。<br />
 * 毎回のテスト時に過去ログを作成すると時間がかかるため、Setup#reset()とは別の手段で初期設定する。
 * @see com.fujitsu.dc.test.setup.Setup#resetEventLog()
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class LogListTest extends ODataCommon {

    private static final String ARCHIVE_COLLECTION = "archive";
    private static final String CURRENT_COLLECTION = "current";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public LogListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ログファイル一覧取得に対するPROPFINDで501が返却されること.
     */
    @Test
    public final void ログファイル一覧取得に対するPROPFINDで501が返却されること() {

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
    }

    /**
     * ログファイル一覧取得に対する許可しないメソッドで405が返却されること.
     */
    @Test
    public final void ログファイル一覧取得に対する許可しないメソッドで405が返却されること() {

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", HttpMethod.PUT)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", HttpMethod.DELETE)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", CURRENT_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * ログファイル一覧取得で存在しないコレクションに対するPROPFINDで404が返却されること.
     */
    @Test
    public final void ログファイル一覧取得で存在しないコレクションに対するPROPFINDで404が返却されること() {

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", "dummy")
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    /**
     * アーカイブログファイル一覧取得_アーカイブディレクトリが存在しない場合にコレクション情報のみ取得できること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_アーカイブディレクトリが存在しない場合にコレクション情報のみ取得できること() {
        String cellName = "archiveDirectoryNotCreatedCell";
        try {
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            TResponse response = ResourceUtils.logCollectionPropfind(cellName, ARCHIVE_COLLECTION, "0",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // レスポンス情報のチェック
            checkLogListResponse(response, cellName, 0);
        } finally {
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * アーカイブログファイル一覧取得_アーカイブディレクトリが存在しない場合にDepthヘッダに1を指定してコレクション情報のみ取得できること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_アーカイブディレクトリが存在しない場合にDepthヘッダに1を指定してコレクション情報のみ取得できること() {
        String cellName = "archiveDirectoryNotCreatedCell";
        try {
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            TResponse response = ResourceUtils.logCollectionPropfind(cellName, ARCHIVE_COLLECTION, "1",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_MULTI_STATUS);

            // レスポンス情報のチェック
            checkLogListResponse(response, cellName, 0);
        } finally {
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }

    }

    /**
     * アーカイブログファイル一覧取得_ボディなしかつContentLengthありのPROPFINDで207が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディなしかつContentLengthありのPROPFINDで207が返却されること() {

        Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * アーカイブログファイル一覧取得_ボディなしかつContentLengthなしのPROPFINDで207が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディなしかつContentLengthなしのPROPFINDで207が返却されること() {

        Http.request("cell/log-propfind-with-nobody-non-content-length.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "0")
                .returns()
                .statusCode(HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * アーカイブログファイル一覧取得にDepth0を指定した場合に1階層分だけ返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得にDepth0を指定した場合に1階層分だけ返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        TResponse tresponse = Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "0")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_MULTI_STATUS);

        // BodyXMLからの要素取得
        checkLogListResponse(tresponse, Setup.TEST_CELL_EVENTLOG, 0);
    }

    /**
     * アーカイブログファイル一覧取得_ボディにallpropを指定したPROPFINDで207が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディにallpropを指定したPROPFINDで207が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        TResponse tresponse = Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "1")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_MULTI_STATUS);

        checkLogListResponse(tresponse, Setup.TEST_CELL_EVENTLOG, 1);

    }

    /**
     * アーカイブログファイル一覧取得_ボディにallpropが指定されないPROPFINDで400が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディにallpropが指定されないPROPFINDで400が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "1")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * アーカイブログファイル一覧取得_ボディに不正なプロパティが指定さた場合400が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディに不正なプロパティが指定さた場合400が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:hoge/></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "1")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * アーカイブログファイル一覧取得_ボディ不正な形式のXMLを指定したPROPFINDで400が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_ボディ不正な形式のXMLを指定したPROPFINDで400が返却されること() {
        // 最初の要素のとじカッコがないXMLをボディに指定する
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "0")
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * アーカイブログファイル一覧取得_Authorizationが不正な時に401が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_Authorizationが不正な時に401が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", "Invalid-Token")
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "0")
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * アーカイブログファイル一覧取得_Depthヘッダの指定がない場合に400が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_Depthヘッダの指定がない場合に400が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        Http.request("cell/log-propfind-with-body-no-depth.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * アーカイブログファイル一覧取得_Depthヘッダにinfinityを指定した場合に403が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_Depthヘッダにinfinityを指定した場合に403が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "infinity")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    /**
     * アーカイブログファイル一覧取得_Depthヘッダに0または1以外の数値を指定した場合に400が返却されること.
     */
    @Test
    public final void アーカイブログファイル一覧取得_Depthヘッダに0または1以外の数値を指定した場合に400が返却されること() {

        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

        Http.request("cell/log-propfind-with-body.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL_EVENTLOG)
                .with("collection", ARCHIVE_COLLECTION)
                .with("depth", "100")
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ログの一覧取得のレスポンス情報をチェックする.
     * @param tresponse レスポンス情報
     * @param cellName セル名
     * @param depth Depth 1:ファイル情報を含む 0:コレクション情報のみ
     */
    private void checkLogListResponse(TResponse tresponse, String cellName, int depth) {
        // コレクションの情報
        String status = getNodeValue(tresponse.bodyAsXml(), 0, "status");
        assertEquals("HTTP/1.1 200 OK", status);

        String href = getNodeValue(tresponse.bodyAsXml(), 0, "href");
        String expectedHref = UrlUtils.getBaseUrl() + String.format("/%s/__log/archive", cellName);
        assertEquals(expectedHref, href);

        assertNotNull(getNodeValue(tresponse.bodyAsXml(), 0, "creationdate"));
        assertNotNull(getNodeValue(tresponse.bodyAsXml(), 0, "getlastmodified"));
        assertNotNull(getNodeValue(tresponse.bodyAsXml(), 0, "resourcetype"));
        assertTrue(hasNode(tresponse.bodyAsXml(), 0, "collection"));

        // ログファイルの情報
        if (depth == 1) {
            for (int i = 1; i < 3; i++) {
                status = getNodeValue(tresponse.bodyAsXml(), i, "status");
                assertEquals("HTTP/1.1 200 OK", status);

                href = getNodeValue(tresponse.bodyAsXml(), i, "href");

                // Date型かの判定
                String[] splitedHref = href.split("log\\.");
                Date date = new Date(Long.valueOf(splitedHref[1]));
                assertNotNull(date);
                // Archiveされたタイムスタンプが「2014-06-19 11:41:58」以降であるか判断
                // 旧フォーマット(1など小さい値)で出力されていないかをチェックするため
                assertTrue(date.getTime() >= 1403145718419L);
                // ログのパス形式が意図したとおりになっているかの確認
                expectedHref = UrlUtils.getBaseUrl()
                        + String.format("/%s/__log/archive/default.log", cellName);
                assertTrue(href.startsWith(expectedHref));

                assertNotNull(getNodeValue(tresponse.bodyAsXml(), i, "creationdate"));
                assertNotNull(getNodeValue(tresponse.bodyAsXml(), i, "getlastmodified"));
                assertNotNull(getNodeValue(tresponse.bodyAsXml(), i - 1, "getcontentlength"));
                assertNotNull(getNodeValue(tresponse.bodyAsXml(), i - 1, "getcontenttype"));
                assertTrue(hasNode(tresponse.bodyAsXml(), i, "resourcetype"));
            }
        }
    }

    /**
     * XMLから指定したタグ名の要素を返す.
     * @param doc ドキュメント
     * @param index インデックス
     * @param tagName タグ名
     * @return result 要素の値
     */
    private String getNodeValue(Document doc, int index, String tagName) {
        Element root = doc.getDocumentElement();
        NodeList responseNodeList = root.getElementsByTagName(tagName);
        if (null == responseNodeList) {
            return null;
        }

        return responseNodeList.item(index).getFirstChild().getNodeValue();
    }

    /**
     * XMLに指定したタグがあるかどうかを返却.
     * @param doc ドキュメント
     * @param index インデックス
     * @param tagName タグ名
     * @return result 要素の値
     */
    private boolean hasNode(Document doc, int index, String tagName) {
        boolean ret = false;
        Element root = doc.getDocumentElement();
        NodeList responseNodeList = root.getElementsByTagName(tagName);

        if (null != responseNodeList && null != responseNodeList.item(index)) {
            ret = true;
        }

        return ret;
    }

}

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

import java.util.Calendar;
import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;

/**
 * JerseyTestFrameworkを利用したユニットテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ReadTest extends AbstractCase {
    /** レスポンスヘッダ. */
    private static final String[] RESPONSE_HEADERS = {HTTP.CONTENT_TYPE, ODataConstants.Headers.DATA_SERVICE_VERSION};
    /** レスポンスヘッダの値. */
    private static final String[] RESPONSE_HEADER_VALUES = {MediaType.APPLICATION_JSON, ODataVersion.V2.asString};
    /** レスポンスヘッダのHashMap. */
    private HashMap<String, String> responseHeaderMap;
    /** テストに使用するCellのId. */
    private String cellId;
    /** Cellを削除したかどうかのフラグ. */
    private boolean deleteCell = false;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ReadTest() {
        super("com.fujitsu.dc.core.rs");
        this.responseHeaderMap = new HashMap<String, String>();
        for (int i = 0; i < RESPONSE_HEADERS.length; i++) {
            this.responseHeaderMap.put(RESPONSE_HEADERS[i], RESPONSE_HEADER_VALUES[i]);
        }
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * テスト完了時にelasticsearchのノードをクローズする.
     */
    @AfterClass
    public static void afterClass() {
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @Before
    public final void initCell() {
        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        String cellName = "cellName";
        cellName = cellName + Long.toString(Calendar.getInstance().getTimeInMillis());

        // Cellを作成
        DcResponse res;
        res = createCell(cellName);

        // Cell作成のレスポンスチェック
        // 201になることを確認
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        // LOCATIONヘッダを取得
        Header[] resHeaders = res.getResponseHeaders(HttpHeaders.LOCATION);
        // レスポンスヘッダにLOCATIONが含まれているかチェック
        assertNotNull(resHeaders);
        // LOCATIONヘッダが複数存在する場合もNGとする
        assertEquals(1, resHeaders.length);
        // 作成したCellのIDを抽出
        this.cellId = resHeaders[0].getValue().split("'")[1];
    }

    /**
     * testの後に実行する.
     */
    @After
    public final void afterCell() {
        if (!this.deleteCell) {
            this.settleCell();
        }
    }

    /**
     * 登録したCellをすぐに取得するテスト.
     */
    @Test
    public final void Cell登録直後にCellが参照できること() {
        // Cellを作成
        DcResponse res;
        res = createCell("testSoonGetCell");
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());
        // Cell作成のレスポンスチェック
        // LOCATIONヘッダを取得
        Header[] resHeaders = res.getResponseHeaders(HttpHeaders.LOCATION);
        // 作成したCellのIDを抽出
        String createdcellId = resHeaders[0].getValue().split("'")[1];
        // Cellを取得
        res = this.restGet(getUrl(createdcellId));
        // Cellを取得できることを確認
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        // 作成したCellを削除
        res = restDelete(getUrl(createdcellId));
        // 削除された事を確認するため、取得を行い、404になる事を確認
        res = this.restGet(getUrl(createdcellId));
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
    }

    /**
     * test用に作成したCellを削除する.
     */
    private void settleCell() {
        // test用に作成したCellを削除
        this.setHeaders(null);
        DcResponse res = restDelete(getUrl(this.cellId));
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

        // 削除された事を確認するため、取得を行い、404になる事を確認
        res = this.restGet(getUrl(this.cellId));
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        this.deleteCell = true;
    }

    /**
     * Cellの取得の正常系のテスト. $format → なし Accept → なし
     */
    @Test
    public final void Cellの取得の正常系のテスト() {
        // $format なし
        // Acceptヘッダ なし
        String url = getUrl(this.cellId);
        DcResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * Cell名にクオート無しの数値型式名を指定した場合400エラーとなること.
     */
    @Test
    public final void Cell名にクオート無しの数値型式名を指定した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String cellName = "123456";
        String url = getUrlWithOutQuote(cellName, null);
        DcResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        checkErrorResponse(res.bodyAsJson(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * Cellの取得で認証ヘッダに空文字を指定した場合に認証エラーが返却されること.
     */
    @Test
    public final void Cellの取得で認証ヘッダに空文字を指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0003");
    }

    /**
     * Cellの取得で認証トークンにBearerとBasic以外の形式でトークン値空文字を指定した場合に認証エラーが返却されること.
     */
    @Test
    public final void Cellの取得で認証トークンにBearerとBasic以外の形式でトークン値空文字を指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "Token token=\"" + AbstractCase.MASTER_TOKEN_NAME + "\"");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0003");
    }

    /**
     * Cellの取得で認証トークンにBasic形式でトークン値空文字を指定した場合に認証エラーが返却されること.
     */
    @Test
    public final void Cellの取得で認証トークンにBasic形式でトークン値空文字を指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "Basic ");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    /**
     * Cellの取得で認証トークンにBasic形式でマスタートークンを指定した場合に認証エラーが返却されること.
     * TODO V1.1 Basic認証に対応後有効化する
     */
    @Test
    @Ignore
    public final void Cellの取得で認証トークンにBasic形式でマスタートークンを指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "Basic " + AbstractCase.MASTER_TOKEN_NAME);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0004");
    }

    /**
     * Cellの取得で認証トークンにBasic形式で不正な値を指定した場合に認証エラーが返却されること.
     * TODO V1.1 Basic認証に対応後有効化する
     */
    @Test
    @Ignore
    public final void Cellの取得で認証トークンにBasic形式で不正な値を指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        // コロンを含んだ文字列ををBase64化して、ヘッダーに指定
        headers.put(HttpHeaders.AUTHORIZATION, "Basic YzNzgUZpbm5vdg==");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0004");
    }

    /**
     * Cellの取得で認証トークンにBearer形式でトークン値空文字を指定した場合に認証エラーが返却されること.
     */
    @Test
    public final void Cellの取得で認証トークンにBearer形式でトークン値空文字を指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer ");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0006");
    }

    /**
     * Cellの取得でマスタートークン以外のトークンを指定した場合に認証エラーが返却されること.
     */
    @Test
    public final void Cellの取得でマスタートークン以外のトークンを指定した場合に認証エラーが返却されること() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer test");
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // ステータスコード:401
        // コンテンツタイプ:application/json
        // Etagヘッダが存在しない
        // ボディのエラーコードチェック
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR401-AU-0006");
    }

    /**
     * Cellの取得で不正なIDを指定した場合に、BadRequestが返却される事. $format → なし Accept → なし
     */
    @Test
    public final void Cellの取得で不正なIDを指定した場合にBadRequestが返却されること() {
        DcResponse res = this.restGet(getUrl("'a'"));

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0004");
    }

    /**
     * Cellの取得でIDを空文字指定した場合にjsonフォーマットでNotFoundが返却されること. $format → なし Accept → なし
     */
    @Test
    public final void Cellの取得でIDを空文字指定した場合にjsonフォーマットでNotFoundが返却されること() {
        DcResponse res = this.restGet(getUrl(""));

        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → json Accept → なし
     */
    @Test
    public final void ＄formatがjsonでCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format json
        // Acceptヘッダ なし
        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_JSON);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * Cell取得した場合にatomフォーマットでレスポンスが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomでCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format atom
        // Acceptヘッダ なし
        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_ATOM);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Cell取得した場合にatomフォーマットでレスポンスが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomかつacceptがatomでCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format atom
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_ATOM);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Cell取得した場合にatomフォーマットでレスポンスが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomかつacceptがjsonでCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format atom
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_ATOM);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Cell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → json Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがjsonかつacceptがatomでCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format json
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_JSON);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * Cell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → json Accept → application/json
     */
    @Test
    public final void ＄formatがjsonかつacceptがjsonでCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format json
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url + "?" + QUERY_FORMAT_JSON);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * Cell取得した場合にatomフォーマットでレスポンスが返却されること. $format → なし Accept → application/atom+xml
     */
    @Test
    public final void acceptがatomでCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format なし
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Cell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → なし Accept → application/json
     */
    @Test
    public final void acceptがjsonでCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format なし
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * IfNoneMatchヘッダに一致する値を指定した場合レスポンスが304で返却されること.
     * $format → なし Accept → なし
     */
    @Test
    public final void IfNoneMatchヘッダに一致する値を指定した場合レスポンスが304で返却されること() {
        String url = getUrl(this.cellId);
        // 一度リクエストを実行してEtagを取得する
        DcResponse res = this.restGet(getUrl(this.cellId));
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());

        this.checkHeaders(res);
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        this.checkCellResponse(res.bodyAsJson(), url);

        // If-None-MatchヘッダにEtagの値を指定して再度リクエストを実行する
        String eTag = res.getFirstHeader(HttpHeaders.ETAG);
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.IF_NONE_MATCH, eTag);
        this.setHeaders(headers);

        res = this.restGet(url);
        assertEquals(HttpStatus.SC_NOT_MODIFIED, res.getStatusCode());
    }

    /**
     * IfNoneMatchヘッダに一致しない値を指定した場合にレスポンスが200で返却されること.
     * $format → なし Accept → なし
     */
    @Test
    public final void IfNoneMatchヘッダに一致しない値を指定した場合にレスポンスが200で返却されること() {
        String url = getUrl(this.cellId);
        // $format なし
        // Acceptヘッダ なし
        DcResponse res = this.restGet(url);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        this.checkCellResponse(res.bodyAsJson(), url);

        String eTag = res.getFirstHeader(HttpHeaders.ETAG);
        // ETagを改変する
        String eTag2 = eTag + "aa";

        // IfNoneMatchヘッダに一致しない値を指定した場合にレスポンスが200で返却されることを確認する
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.IF_NONE_MATCH, eTag2);
        this.setHeaders(headers);

        res = this.restGet(url);

        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        this.checkCellResponse(res.bodyAsJson(), url);

    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → なし Accept → なし
     */
    @Test
    public final void Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        DcResponse res = this.restGet(getUrl(this.cellId));
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → json Accept → なし
     */
    @Test
    public final void ＄formatがjsonでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format json
        // Acceptヘッダ なし
        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_JSON));
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → atom Accept → なし
     */
    @Test
    public final void ＄formatがatomでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format atom
        // Acceptヘッダ なし
        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_ATOM));
        // TODO $formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomかつacceptがatomでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format atom
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_ATOM));

        // TODO $formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → atom Accept → application/json
     */
    @Test
    public final void ＄formatがatomかつacceptがjsonでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format atom
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_ATOM));

        // TODO $formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");

    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → json Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがjsonかつacceptがatomでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format json
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_JSON));

        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");

    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → json Accept → application/json
     */
    @Test
    public final void ＄formatがjsonかつacceptがjsonでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format json
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId, QUERY_FORMAT_JSON));

        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → なし Accept → application/atom+xml
     */
    @Test
    public final void acceptがatomでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format なし
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        // TODO $formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");
    }

    /**
     * Cellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること. $format → なし Accept → application/json
     */
    @Test
    public final void acceptがjsonでCellの取得で存在しないIDを指定した場合にjsonフォーマットでNotFoundが返却されること() {
        this.settleCell();
        // $format なし
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restGet(getUrl(this.cellId));

        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR404-OD-0002");

    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → なし Accept → なし
     */
    @Test
    public final void Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        DcResponse res = restPost(getUrl(this.cellId), "");

        // レスポンスのチェック
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → json Accept → なし
     */
    @Test
    public final void ＄formatがjsonでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format json
        // Acceptヘッダ なし
        DcResponse res = restPost(getUrl(this.cellId), QUERY_FORMAT_JSON);

        // レスポンスのチェック
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → atom Accept → なし
     */
    @Test
    public final void ＄formatがatomでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format atom
        // Acceptヘッダ なし
        DcResponse res = this.restPost(getUrl(this.cellId), QUERY_FORMAT_ATOM);

        // レスポンスのチェック
        // TODO formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomかつacceptがatomでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format atom
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), QUERY_FORMAT_ATOM);

        // レスポンスのチェック
        // TODO formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → atom Accept → application/json
     */
    @Test
    public final void ＄formatがatomかつacceptがjsonでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format atom
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), QUERY_FORMAT_ATOM);

        // レスポンスのチェック
        // TODO formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → json Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがjsonかつacceptがatomでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format json
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), QUERY_FORMAT_JSON);

        // レスポンスのチェック
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → json Accept → application/json
     */
    @Test
    public final void ＄formatがjsonかつacceptがjsonでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format json
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), QUERY_FORMAT_JSON);

        // レスポンスのチェック
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → なし Accept → application/atom+xml
     */
    @Test
    public final void acceptがatomでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format なし
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), "");

        // レスポンスのチェック
        // TODO formatのxml対応が完了したら確認内容を修正する
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Cellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること. $format → なし Accept → application/json
     */
    @Test
    public final void acceptがjsonでCellの取得で許可されていないメソッドを指定した場合にjsonフォーマットでMethodNotAllowedが返却されること() {
        // $format なし
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = this.restPost(getUrl(this.cellId), "");

        // レスポンスのチェック
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * Acceptにimage/jpegを指定してCell取得した場合にUnsupportedMediaTypeとなること.
     */
    @Test
    public final void acceptがjpegでCell取得した場合にUnsupportedMediaTypeとなること() {
        String url = getUrl(this.cellId);
        // $format なし
        // Acceptヘッダ image/jpeg
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "image/jpeg");
        this.setHeaders(headers);

        DcResponse res = restGet(url);
        assertEquals(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, res.getStatusCode());
    }

    /**
     * Acceptにimage/jpegを指定してCell取得した場合にatomフォーマットでレスポンスが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomでacceptがjpegでCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format atom
        // Acceptヘッダ image/jpeg
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "image/jpeg");
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + QUERY_FORMAT_ATOM);
        // TODO Acceptヘッダーのチェック処理が完了したら、NOT_ACCEPTABLEのチェックに変更する
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Acceptにimage/jpegを指定してCell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → json Accept → image/jpeg
     */
    @Test
    public final void ＄formatがjsonでacceptがjpegでCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format json
        // Acceptヘッダ image/jpeg
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "image/jpeg");
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + QUERY_FORMAT_JSON);
        // TODO Acceptヘッダーのチェック処理が完了したら、NOT_ACCEPTABLEのチェックに変更する
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * Acceptに空文字を指定してCell取得した場合にXMLでレスポンスが返却されること. $format → なし Accept → 空文字
     */
    @Test
    public final void acceptが空文字でCell取得した場合にXMLでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format なし
        // Acceptヘッダ 空文字
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "");
        this.setHeaders(headers);

        DcResponse res = restGet(url);
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        res.bodyAsXml();
    }

    /**
     * Acceptに空文字を指定してCell取得した場合にatomフォーマットでレスポンスが返却されること. $format → atom Accept → application/atom+xml
     */
    @Test
    public final void ＄formatがatomでacceptが空文字でCell取得した場合にatomフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format atom
        // Acceptヘッダ 空文字
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "");
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + QUERY_FORMAT_ATOM);
        // TODO Acceptヘッダーのチェック処理が完了したら、NOT_ACCEPTABLEのチェックに変更する
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.responseHeaderMap.put(HTTP.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        res.bodyAsXml();
    }

    /**
     * Acceptに空文字を指定してCell取得した場合にjsonフォーマットでレスポンスが返却されること. $format → json Accept → 空文字
     */
    @Test
    public final void ＄formatがjsonでacceptが空文字でCell取得した場合にjsonフォーマットでレスポンスが返却されること() {
        String url = getUrl(this.cellId);
        // $format json
        // Acceptヘッダ 空文字
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, "");
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + QUERY_FORMAT_JSON);
        // TODO Acceptヘッダーのチェック処理が完了したら、NOT_ACCEPTABLEのチェックに変更する
        assertEquals(HttpStatus.SC_OK, res.getStatusCode());
        this.checkHeaders(res);
        // Etagのチェック
        assertEquals(1, res.getResponseHeaders(HttpHeaders.ETAG).length);
        // レスポンスボディのパース
        this.checkCellResponse(res.bodyAsJson(), url);
    }

    /**
     * レスポンスフォーマットに不正値を指定した場合にBadRequestとなること.
     */
    @Test
    public final void レスポンスフォーマットに不正値を指定した場合にBadRequestとなること() {
        String url = getUrl(this.cellId);
        // $format csv
        // Acceptヘッダ なし
        DcResponse res = restGet(url + "?" + "$format=csv");

        // TODO $formatのチェック処理が完了したら、BAD_REQUESTのチェックに変更する
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * レスポンスフォーマットに不正値かつAcceptヘッダにATOMを指定した場合にBadRequestとなること.
     */
    @Test
    public final void レスポンスフォーマットに不正値かつAcceptヘッダにATOMを指定した場合にBadRequestとなること() {
        String url = getUrl(this.cellId);
        // $format csv
        // Acceptヘッダ application/atom+xml
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + "$format=csv");
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    /**
     * レスポンスフォーマットに不正値かつAcceptヘッダにJSONを指定した場合にBadRequestとなること.
     */
    @Test
    public final void レスポンスフォーマットに不正値かつAcceptヘッダにJSONを指定した場合にBadRequestとなること() {
        String url = getUrl(this.cellId);
        // $format csv
        // Acceptヘッダ application/json
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        this.setHeaders(headers);

        DcResponse res = restGet(url + "?" + "$format=csv");
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
    }

    private void checkHeaders(final DcResponse res) {
        for (int i = 0; i < RESPONSE_HEADERS.length; i++) {
            Header[] headers = res.getResponseHeaders(RESPONSE_HEADERS[i]);
            assertEquals(headers.length, 1);
            String value = this.responseHeaderMap.get(RESPONSE_HEADERS[i]);
            assertEquals(value, headers[0].getValue());
        }
    }
}

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
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.NodeList;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Cellの更新のIT.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class UpdateTest extends AbstractCase {

    private String cellName;
    private String cellNameToDelete;
    private DcResponse res;
    private static String eTag = "";
    private static String published = "";

    /**
     * ETagの値.
     */
    public static final String ETAG_ASTA = "*";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public UpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * テスト全体の実行前で必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * 各テストの実行前で必ず１度実行される処理.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        // Cellを作成
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        this.cellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        this.cellNameToDelete = cellName;

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        DcResponse beforeres = createCell(headers, requestBody);

        // 更新したCellのIDを保持する
        // Header[] resHeadersLocate = beforeres.getResponseHeaders(HttpHeaders.LOCATION);
        // cellName = resHeadersLocate[0].getValue().split("'")[1];
        // ETagが正式実装された場合は、レスポンスのEtagを使用するため保持する必要がある
        Header[] resHeadersEtag = beforeres.getResponseHeaders(HttpHeaders.ETAG);
        if (resHeadersEtag != null && resHeadersEtag.length == 1) {
            eTag = resHeadersEtag[0].getValue();
        }
        // eTag = ETAG_ASTA;

        // __publishedを取得
        published = ODataCommon.getPublished(beforeres);
    }

    /**
     * 各テストの実行後で必ず１度実行される処理.
     */
    @After
    public void after() {
        deleteCell();
    }

    /**
     * Cell更新の正常リクエストで204が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell更新の正常リクエストで204が返却されること() {
        // リクエストヘッダを設定する
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, eTag);

        // リクエストボディを設定する
        JSONObject requestBody = new JSONObject();
        String updateCellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        requestBody.put("Name", updateCellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);
        cellNameToDelete = updateCellName;

        // __publishedを取得する
        DcResponse getResp = restGet(getUrl(updateCellName));
        String resPublished = ODataCommon.getPublished(getResp);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        assertEquals(published, resPublished);
    }

    /**
     * ACL設定済みのCell更新の正常リクエストで204が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ACL設定済みのCell更新の正常リクエストで204が返却されること() {
        // ACL(all/all)をCellに設定
        Http.request("cell/acl-setting-all.txt")
                .with("url", cellName)
                .with("token", Setup.BEARER_MASTER_TOKEN)
                .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, ""))
                .returns()
                .statusCode(HttpStatus.SC_OK);

        // PROPFINDでCellのACLを取得
        TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                .with("url", cellName)
                .with("depth", "0")
                .with("token", Setup.MASTER_TOKEN_NAME)
                .returns();
        tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

        // principalにallが設定されていることの確認
        NodeList nodeListPrincipal = tresponse.bodyAsXml().getElementsByTagName("principal").item(0).getChildNodes();
        boolean existsPrincipalAll = false;
        for (int i = 0; i < nodeListPrincipal.getLength(); i++) {
            String nodename = nodeListPrincipal.item(i).getNodeName();
            if ("all".equals(nodename)) {
                existsPrincipalAll = true;
            }
        }
        assertTrue(existsPrincipalAll);

        // privilegeにallが設定されていることの確認
        NodeList nodeListPrivilege = tresponse.bodyAsXml().getElementsByTagName("privilege").item(0).getChildNodes();
        boolean existsPrivilegeAll = false;
        for (int i = 0; i < nodeListPrivilege.getLength(); i++) {
            String nodename = nodeListPrivilege.item(i).getNodeName();
            if ("all".equals(nodename)) {
                existsPrivilegeAll = true;
            }
        }
        assertTrue(existsPrivilegeAll);

        // 更新用のリクエストヘッダを設定する
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // 更新用リクエストボディを設定する
        JSONObject requestBody = new JSONObject();
        String updateCellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        requestBody.put("Name", updateCellName);

        // 更新リクエストを実行する
        res = updateCell(headers, requestBody);
        cellNameToDelete = updateCellName;

        // __publishedを取得する
        DcResponse getResp = restGet(getUrl(updateCellName));
        String resPublished = ODataCommon.getPublished(getResp);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        assertEquals(published, resPublished);

        // PROPFINDでCellのACLを取得
        TResponse tresponsePut = Http.request("cell/propfind-cell-allprop.txt").with("url", updateCellName)
                .with("depth", "0")
                .with("token", Setup.MASTER_TOKEN_NAME).returns();
        tresponse.statusCode(HttpStatus.SC_MULTI_STATUS);

        // principalにallが設定されていることの確認
        assertNotNull(tresponsePut.bodyAsXml().getElementsByTagName("principal").item(0));
        nodeListPrincipal = tresponsePut.bodyAsXml().getElementsByTagName("principal").item(0).getChildNodes();
        existsPrincipalAll = false;
        for (int i = 0; i < nodeListPrincipal.getLength(); i++) {
            String nodename = nodeListPrincipal.item(i).getNodeName();
            if ("all".equals(nodename)) {
                existsPrincipalAll = true;
            }
        }
        assertTrue(existsPrincipalAll);

        // privilegeにallが設定されていることの確認
        assertNotNull(tresponsePut.bodyAsXml().getElementsByTagName("privilege").item(0));
        nodeListPrivilege = tresponsePut.bodyAsXml().getElementsByTagName("privilege").item(0).getChildNodes();
        existsPrivilegeAll = false;
        for (int i = 0; i < nodeListPrivilege.getLength(); i++) {
            String nodename = nodeListPrivilege.item(i).getNodeName();
            if ("all".equals(nodename)) {
                existsPrivilegeAll = true;
            }
        }
        assertTrue(existsPrivilegeAll);

    }

    /**
     * Cell名にクオート無しの数値型式名を指定した場合400エラーとなること.
     */
    @Test
    public final void Cell名にクオート無しの数値型式名を指定した場合400エラーとなること() {
        // $format なし
        // Acceptヘッダ なし
        String cellNameAsInteger = "123456";
        String url = getUrlWithOutQuote(cellNameAsInteger, null);
        DcResponse response = this.restPut(url, "");

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getCode(),
                DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.getMessage());
    }

    /**
     * Cellの更新のNameが無い アクセプトヘッダ無しのパターンのテスト.
     */
    @Test
    public final void Cellの更新のNameが無いアクセプトヘッダ無しのパターンのテスト() {
        cellNameNull(null);
    }

    /**
     * Cellの更新のNameが無い アクセプトヘッダJSONのパターンのテスト.
     */
    @Test
    public final void Cellの更新のNameが無いアクセプトヘッダJSONのパターンのテスト() {
        cellNameNull(MediaType.APPLICATION_JSON);
    }

    /**
     * Cellの更新のNameが無い アクセプトヘッダATOMのパターンのテスト.
     */
    @Test
    public final void Cellの更新のNameが無いアクセプトヘッダATOMのパターンのテスト() {
        cellNameNull(MediaType.APPLICATION_ATOM_XML);
    }

    /**
     * Cellの更新のNameが無いパターンのテスト.
     * @param accept アクセプトヘッダの値
     */
    private void cellNameNull(String accept) {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        if (accept != null) {
            headers.put(HttpHeaders.ACCEPT, accept);
        } else {
            accept = MediaType.APPLICATION_ATOM_XML;
        }

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();

        res = updateCell(headers, requestBody);

        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());

        // ボディのチェック
        checkErrorResponse(this.res.bodyAsJson(), "PR400-OD-0009");
    }

    /**
     * Cellの更新のNameが空のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが空のパターンのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", "");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cellの更新のNameが1文字のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが1文字のパターンのテスト() {

        HashMap<String, String> headers = new HashMap<String, String>();
        try {
            // Cellを更新
            // リクエストヘッダをセット
            headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            headers.put(HttpHeaders.IF_MATCH, "*");

            // リクエストボディを生成
            JSONObject requestBody = new JSONObject();
            requestBody.put("Name", "0");

            res = updateCell(headers, requestBody);

            // Cell更新のレスポンスチェック
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        } finally {
            // セル名を元に戻す（削除はafterで実行する）
            JSONObject requestBody = new JSONObject();
            requestBody.put("Name", cellName);
            this.updateCellName(headers, requestBody, "0");
        }

    }

    /**
     * Cellの更新のNameが128文字のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが128文字のパターンのテスト() {
        String id = "01234567890123456789012345678901234567890123456789"
                + "012345678901234567890123456789012345678901234567890123456789012345678901234567";
        HashMap<String, String> headers = new HashMap<String, String>();
        try {
            // Cellを更新
            // リクエストヘッダをセット
            headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            headers.put(HttpHeaders.IF_MATCH, "*");

            // リクエストボディを生成
            JSONObject requestBody = new JSONObject();
            requestBody.put("Name", id);

            res = updateCell(headers, requestBody);

            // Cell更新のレスポンスチェック
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        } finally {
            // セル名を元に戻す（削除はafterで実行する）
            JSONObject requestBody = new JSONObject();
            requestBody.put("Name", cellName);
            this.updateCellName(headers, requestBody, id);
        }
    }

    /**
     * Cellの更新のNameが129文字のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが129文字のパターンのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        String name = "01234567890123456789012345678901234567890123456789"
                + "0123456789012345678901234567890123456789012345678901234567890123456789012345678";
        requestBody.put("Name", name);

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cellの更新のNameが半角英数と"-","_"のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが半角英数と使用可能な記号のパターンのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        this.cellNameToDelete = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_";
        requestBody.put("Name", this.cellNameToDelete);

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新のNameが半角英数と"-","_"以外のパターンのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが半角英数と使用可能な記号以外のパターンのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", "あ");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cellの更新のNameが__の場合に４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが__の場合に４００が返却されること() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", "__");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cellの更新のNameが__ctlの場合に４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のNameが__ctlの場合に４００が返却されること() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", "__ctl");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cell更新のリクエストボディに__publishedを指定した場合に４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell更新のリクエストボディに__publishedを指定した場合に４００が返却されること() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        String updateCellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        requestBody.put("Name", updateCellName);
        requestBody.put(PUBLISHED, "/Date(0)/");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    /**
     * Cell更新のリクエストボディに__updatedを指定した場合に４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell更新のリクエストボディに__updatedを指定した場合に４００が返却されること() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        String updateCellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        requestBody.put("Name", updateCellName);
        requestBody.put(UPDATED, "/Date(0)/");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    /**
     * Cell更新のリクエストボディに__metadataを指定した場合に４００が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cell更新のリクエストボディに__metadataを指定した場合に４００が返却されること() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        String updateCellName = "cellname" + Long.toString(Calendar.getInstance().getTimeInMillis());
        requestBody.put("Name", updateCellName);
        requestBody.put(METADATA, "test");

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
    }

    /**
     * Cellの更新のJSONフォーマットエラー アクセプトヘッダ無しのテスト.
     */
    @Test
    public final void Cellの更新のJSONフォーマットエラーでアクセプトヘッダ無しのテスト() {
        cellJsonFormatErr(null);
    }

    /**
     * Cellの更新のJSONフォーマットエラー アクセプトヘッダJSONのテスト.
     */
    @Test
    public final void Cellの更新のJSONフォーマットエラーでアクセプトヘッダJSONのテスト() {
        cellJsonFormatErr(MediaType.APPLICATION_JSON);
    }

    /**
     * Cellの更新のJSONフォーマットエラー アクセプトヘッダATOMのテスト.
     */
    @Test
    public final void Cellの更新のJSONフォーマットエラーでアクセプトヘッダATOMのテスト() {
        cellJsonFormatErr(MediaType.APPLICATION_ATOM_XML);
    }

    /**
     * Cellの更新のJSONフォーマットエラー.
     * @param accept アクセプトヘッダの値
     */
    private void cellJsonFormatErr(String accept) {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        if (accept != null) {
            headers.put(HttpHeaders.ACCEPT, accept);
        } else {
            accept = MediaType.APPLICATION_ATOM_XML;
        }

        // リクエストボディを生成
        DcRestAdapter rest = new DcRestAdapter();

        String data = "\"test\"";

        try {
            // リクエスト
            res = rest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Cell更新のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0001");

    }

    /**
     * Cellの更新のXMLフォーマットエラー アクセプトヘッダ無しのテスト.
     */
    @Test
    public final void Cellの更新のXMLフォーマットエラーでアクセプトヘッダ無しのテスト() {
        cellXmlFormatErr(null);
    }

    /**
     * Cellの更新のXMLフォーマットエラー アクセプトヘッダJSONのテスト.
     */
    @Test
    public final void Cellの更新のXMLフォーマットエラーでアクセプトヘッダJSONのテスト() {
        cellXmlFormatErr(MediaType.APPLICATION_JSON);
    }

    /**
     * Cellの更新のXMLフォーマットエラー アクセプトヘッダATOMのテスト.
     */
    @Test
    public final void Cellの更新のXMLフォーマットエラーでアクセプトヘッダATOMのテスト() {
        cellXmlFormatErr(MediaType.APPLICATION_ATOM_XML);
    }

    /**
     * Cellの更新のXMLフォーマットエラー.
     * @param accept アクセプトヘッダの値
     */
    private void cellXmlFormatErr(String accept) {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);

        if (accept != null) {
            headers.put(HttpHeaders.ACCEPT, accept);
        } else {
            accept = MediaType.APPLICATION_ATOM_XML;
        }

        // リクエストボディを生成
        DcRestAdapter rest = new DcRestAdapter();

        String data = "\"test\"";

        try {
            // リクエスト
            res = rest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Cell更新のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, res.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        this.checkErrorResponse(res.bodyAsJson(), "PR400-OD-0001");
    }

    /**
     * Cellの更新の認証ヘッダ無し アクセプトヘッダ無しのテスト.
     */
    @Test
    public final void Cellの更新の認証ヘッダ無しアクセプトヘッダ無しのテスト() {
        cellAuthHeaderNone(null);
    }

    /**
     * Cellの更新の認証ヘッダ無し アクセプトヘッダJSONのテスト.
     */
    @Test
    public final void Cellの更新の認証ヘッダ無しアクセプトヘッダJSONのテスト() {
        cellAuthHeaderNone(MediaType.APPLICATION_JSON);
    }

    /**
     * Cellの更新の認証ヘッダ無し アクセプトヘッダATOMのテスト.
     */
    @Test
    public final void Cellの更新の認証ヘッダ無しアクセプトヘッダATOMのテスト() {
        cellAuthHeaderNone(MediaType.APPLICATION_ATOM_XML);
    }

    /**
     * Cellの更新の認証ヘッダ無しのテスト.
     * @param accept アクセプトヘッダの値
     */
    @SuppressWarnings("unchecked")
    private void cellAuthHeaderNone(String accept) {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        if (accept != null) {
            headers.put(HttpHeaders.ACCEPT, accept);
        } else {
            accept = MediaType.APPLICATION_ATOM_XML;
        }

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        this.res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        // 401になることを確認
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = res.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        assertEquals(resContentTypeHeaders[0].getValue(), MediaType.APPLICATION_JSON);

        // ボディのチェック
        checkErrorResponse(this.res.bodyAsJson(), "PR401-AU-0001");
    }

    /**
     * Cellの更新の不正な認証ヘッダのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の認証ヘッダ不正なテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "test");
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        // 401になることを確認
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = res.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        assertEquals(MediaType.APPLICATION_JSON, resContentTypeHeaders[0].getValue());

        // ボディのチェック
        checkErrorResponse(this.res.bodyAsJson(), "PR401-AU-0003");

    }

    /**
     * Cellの更新の認証ヘッダが空文字でのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の認証ヘッダが空文字でのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, "");
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        // 401になることを確認
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = res.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        assertEquals(MediaType.APPLICATION_JSON, resContentTypeHeaders[0].getValue());

        // ボディのチェック
        checkErrorResponse(this.res.bodyAsJson(), "PR401-AU-0003");

    }

    /**
     * Cellの更新のpostメソッド指定のテストAcceptヘッダが無し.
     */
    @Test
    public final void Cellの更新のpostメソッド指定のテストAcceptヘッダが無し() {
        cellPost(null);
    }

    /**
     * Cellの更新のpostメソッド指定AcceptヘッダがJSONのテスト.
     */
    @Test
    public final void Cellの更新のpostメソッド指定AcceptヘッダがJSONのテスト() {
        cellPost(MediaType.APPLICATION_JSON);
    }

    /**
     * Cellの更新のpostメソッド指定AcceptヘッダがATOMのテスト.
     */
    @Test
    public final void Cellの更新のpostメソッド指定AcceptヘッダがATOMのテスト() {
        cellPost(MediaType.APPLICATION_ATOM_XML);
    }

    /**
     * Cellの更新のPOSTメソッドのテスト.
     * @param accept アクセプトヘッダの値
     */
    @SuppressWarnings("unchecked")
    private void cellPost(String accept) {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        if (accept != null) {
            headers.put(HttpHeaders.ACCEPT, accept);
        } else {
            accept = MediaType.APPLICATION_ATOM_XML;
        }

        DcRestAdapter rest = new DcRestAdapter();

        try {
            // リクエストボディを生成
            JSONObject requestBody = new JSONObject();
            requestBody.put("Name", cellName);

            // リクエスト
            res = rest.post(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellName), requestBody.toJSONString(), headers);

            // Cell更新のレスポンスチェック
            // 405になることを確認
            assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());

            // ContentTypeのチェック
            Header[] resContentTypeHeaders = res.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
            assertEquals(1, resContentTypeHeaders.length);
            // 制限にてJSON固定
            assertEquals(MediaType.APPLICATION_JSON, resContentTypeHeaders[0].getValue());

            // ボディのチェック
            checkErrorResponse(this.res.bodyAsJson(), "PR405-MC-0001");
        } catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Cellの更新の不正なContentTypeのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の不正なContentTypeのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, "image/jpeg");
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        // TODO Acceptヘッダのチェック処理実装後に修正する必要がある
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

    }

    /**
     * Cellの更新のconflictのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のconflictのテスト() {
        // Cellを新規で作成する
        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, "image/jpeg");

        // DBサーバーを共有した際、同時にテストを行うと、同じCell名では409となってしまうため、一意にするため、Cell名に時間をセット
        String conflictName = "conflictName" + Long.toString(Calendar.getInstance().getTimeInMillis());

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", conflictName);

        // Cellを作成
        DcResponse beforeres = createCell(headers, requestBody);
        // 201になることを確認
        assertEquals(HttpStatus.SC_CREATED, beforeres.getStatusCode());

        // 新規作成したCellと同一のNameを指定して更新を実行する
        DcResponse resConflict;
        headers.put(HttpHeaders.IF_MATCH, "*");
        resConflict = updateCell(headers, requestBody);
        this.cellNameToDelete = conflictName;

        // 競合チェックが実装されたら409のチェックを行う必要がある
        assertEquals(HttpStatus.SC_CONFLICT, resConflict.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON,
                resConflict.getResponseHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue());
        assertEquals(0, resConflict.getResponseHeaders(HttpHeaders.ETAG).length);
        this.checkErrorResponse(resConflict.bodyAsJson(), "PR409-OD-0003");

    }

    /**
     * Cellの更新の$formatがjsonのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがjsonのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);
        res = updateCellQuery(headers, requestBody, QUERY_FORMAT_JSON);

        // Cell更新のレスポンスチェック
        // TODO $formatのチェックが実装されたら変更する必要がある
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$formatがatomのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがatomのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);
        res = updateCellQuery(headers, requestBody, QUERY_FORMAT_ATOM);

        // Cell更新のレスポンスチェック
        // TODO $formatのチェックが実装されたら変更する必要がある
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

    }

    /**
     * Cellの更新の$formatがjson, atom以外のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがjsonとatom以外のテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCellQuery(headers, requestBody, "$format=test");

        // Cell更新のレスポンスチェック
        // TODO $formatのチェックが実装されたら変更する必要がある
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$format指定なし、Acceptがjsonのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$format指定なしでAcceptがjsonのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$format指定なし、Acceptがatomのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$format指定なしでAcceptがatomのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$formatがjson, Acceptがjsonのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがjsonでAcceptがjsonのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$formatがjson, Acceptがatomのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがjsonでAcceptがatomのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$formatがatom, Acceptがjsonのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがatomでAcceptがjsonのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の$formatがatom, Acceptがatomのテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の$formatがatomでAcceptがatomのテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_ATOM_XML);
        headers.put(HttpHeaders.IF_MATCH, "*");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新の存在しないID指定.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新の存在しないID指定() {
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "*");

        DcRestAdapter rest = new DcRestAdapter();

        // リクエスト先のURL文字列を生成
        String cellNameHoge = "hoge" + Long.toString(Calendar.getInstance().getTimeInMillis());
        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);
        String data = requestBody.toJSONString();

        try {
            // リクエスト
            res = rest.put(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellNameHoge), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Cell更新のレスポンスチェック
        // TODO Cell更新時に指定された主キーが存在しなかった場合はリソース作成するべき？
        assertEquals(HttpStatus.SC_NOT_FOUND, res.getStatusCode());
    }

    /**
     * Cellの更新のIf-Matchが有効値のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のIfMatchが有効値のテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, eTag);

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        // リクエストをもう一度実行する
        res = updateCell(headers, requestBody);
        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_PRECONDITION_FAILED, res.getStatusCode());

    }

    /**
     * Cellの更新のIf-Matchが未指定のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のIfMatchが未指定のテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCell(headers, requestBody);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
    }

    /**
     * Cellの更新のIf-Matchが不正な値のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Cellの更新のIfMatchが不正な値のテスト() {

        // Cellを更新
        // リクエストヘッダをセット
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.IF_MATCH, "hoge");

        // リクエストボディを生成
        JSONObject requestBody = new JSONObject();
        requestBody.put("Name", cellName);

        res = updateCell(headers, requestBody);

        // リクエストを実行する
        res = updateCell(headers, requestBody);

        // Cell更新のレスポンスチェック
        assertEquals(HttpStatus.SC_PRECONDITION_FAILED, res.getStatusCode());
    }

    /**
     * セル更新のリクエスト実行.
     * @param headers リクエストヘッダ
     * @param requestBody リクエストボディ
     * @return Cell更新時のレスポンスオブジェクト
     */
    private static DcResponse createCell(final HashMap<String, String> headers, final JSONObject requestBody) {
        return createCellQuery(headers, requestBody, null);
    }

    /**
     * セル更新のリクエスト実行.
     * @param headers リクエストヘッダ
     * @param requestBody リクエストボディ
     * @param query クエリ文字列
     * @return Cell更新時のレスポンスオブジェクト
     */
    private static DcResponse createCellQuery(final HashMap<String, String> headers,
            final JSONObject requestBody,
            final String query) {
        DcResponse ret = null;
        DcRestAdapter rest = new DcRestAdapter();

        String data = requestBody.toJSONString();

        // リクエスト先のURL文字列を生成
        StringBuilder url = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME));
        if (query != null) {
            url.append("?" + query);
        }

        try {
            // リクエスト
            ret = rest.post(url.toString(), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return ret;
    }

    /**
     * セル更新のリクエスト実行.
     * @param headers リクエストヘッダ
     * @param requestBody リクエストボディ
     * @return Cell更新時のレスポンスオブジェクト
     */
    private DcResponse updateCell(final HashMap<String, String> headers, final JSONObject requestBody) {
        return updateCellQuery(headers, requestBody, null);
    }

    /**
     * セル更新のリクエスト実行.
     * @param headers リクエストヘッダ
     * @param requestBody リクエストボディ
     * @param query クエリ文字列
     * @return Cell更新時のレスポンスオブジェクト
     */
    private DcResponse updateCellQuery(final HashMap<String, String> headers,
            final JSONObject requestBody,
            final String query) {
        DcResponse ret = null;
        DcRestAdapter rest = new DcRestAdapter();

        String data = requestBody.toJSONString();

        // リクエスト先のURL文字列を生成
        StringBuilder url = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellName));
        if (query != null) {
            url.append("?" + query);
        }

        try {
            // リクエスト
            ret = rest.put(url.toString(), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return ret;
    }

    /**
     * セル名指定のセル更新のリクエスト実行.
     * @param headers リクエストヘッダ
     * @param requestBody リクエストボディ
     * @param cellNameStr 更新前セル名
     * @return Cell更新時のレスポンスオブジェクト
     */
    private DcResponse updateCellName(final HashMap<String, String> headers,
            final JSONObject requestBody,
            final String cellNameStr) {
        DcResponse ret = null;
        DcRestAdapter rest = new DcRestAdapter();

        String data = requestBody.toJSONString();

        // リクエスト先のURL文字列を生成
        StringBuilder url = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellNameStr));

        try {
            // リクエスト
            ret = rest.put(url.toString(), data, headers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return ret;
    }

    /**
     * IDを指定してセルを削除.
     * @param id セルID
     */
    private void deleteCell(String id) {
        this.deleteCell(id, true);
    }

    /**
     * IDを指定してセルを削除.
     * @param id セルID
     * @param checkStatusCode ステータスコードをチェック
     */
    private void deleteCell(String id, Boolean checkStatusCode) {

        // Cellを削除
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse delresponse = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        requestheaders.put(HttpHeaders.IF_MATCH, "*");

        try {
            // リクエスト
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            delresponse = rest.del(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, id), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        if (checkStatusCode) {
            assertEquals(HttpStatus.SC_NO_CONTENT, delresponse.getStatusCode());
        }
    }

    /**
     * セル削除.
     */
    private void deleteCell() {
        deleteCell(cellNameToDelete);
    }
}

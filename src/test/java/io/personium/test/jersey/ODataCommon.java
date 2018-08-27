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
package io.personium.test.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.odata4j.core.ODataConstants;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Cell;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.TResponse;

/**
 * ODataリソース関連の共通テスト処理.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Ignore
public class ODataCommon extends AbstractCase {

    /**
     * __countが返却されない時の値.
     */
    public static final int COUNT_NONE = -1;

    /** DcResponseオブジェクト. */
    private PersoniumResponse res = null;

    /**
     * Constructor.
     * @param application jax-rs application
     */
    public ODataCommon(Application application) {
        super(application);
    }

    /**
     * DcResponseヘッダのsetter.
     * @param value 値
     */
    public void setResponse(PersoniumResponse value) {
        res = value;
    }

    /**
     * 指定されたurlに従いリソースを取得する.
     * @param url URL
     * @return レスポンス
     */
    public static PersoniumResponse getOdataResource(String url) {
        PersoniumResponse dcRes = null;
        if (url != null) {
            PersoniumRequest req = PersoniumRequest.get(url)
                    .header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            dcRes = request(req);
        }
        return dcRes;

    }

    /**
     * 指定されたurlに従いリソースを削除する.
     * @param url URL
     * @return レスポンス
     */
    public static PersoniumResponse deleteOdataResource(String url) {
        PersoniumResponse dcRes = null;
        if (url != null) {
            PersoniumRequest req = PersoniumRequest.delete(url)
                    .header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN)
                    .header(HttpHeaders.IF_MATCH, "*");
            dcRes = request(req);
        }
        return dcRes;

    }

    /**
     * テストで作成したセルを削除する.
     */
    public void cellDelete() {
        cellDelete(this.res);
    }

    /**
     * テストで作成したセルを削除する.
     * @param response DcResponseオブジェクト
     */
    public void cellDelete(PersoniumResponse response) {
        if (response.getStatusCode() == HttpStatus.SC_CREATED) {
            // 作成したCellのIDを抽出
            String cellId = response.getResponseHeaders(HttpHeaders.LOCATION)[0].getValue().split("'")[1];

            // CellのURL文字列を生成
            // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
            StringBuilder cellUrl = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellId));

            // Cellを削除
            PersoniumResponse resDel = restDelete(cellUrl.toString());
            assertEquals(HttpStatus.SC_NO_CONTENT, resDel.getStatusCode());
        }
    }

    /**
     * テストで作成したセルを削除する.
     * @param cellId 作成したセルID
     * @param checkStatusCode ステータスコードのチェック
     */
    public void cellDelete(String cellId, Boolean checkStatusCode) {
        // CellのURL文字列を生成
        // 本来は、LOCATIONヘッダにURLが格納されているが、jerseyTestFrameworkに向け直すため、再構築する
        StringBuilder cellUrl = new StringBuilder(UrlUtils.unitCtl(Cell.EDM_TYPE_NAME, cellId));

        // Cellを削除
        PersoniumResponse resDel = restDelete(cellUrl.toString());

        if (checkStatusCode) {
            assertEquals(HttpStatus.SC_NO_CONTENT, resDel.getStatusCode());
        }
    }

    /**
     * セルを削除する.
     * @param cellId セルID
     */
    public void cellDelete(String cellId) {
        this.cellDelete(cellId, true);
    }

    /**
     * テストで作成したセルの一覧を削除する.
     * @param cellIdList 作成したセルのIDリスト
     */
    public void cellDeleteList(ArrayList<String> cellIdList) {
        for (int i = 0; i < cellIdList.size(); i++) {
            cellDelete(cellIdList.get(i));
        }
    }

    /**
     * 正常系のパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param name 作成するCellの名前
     */
    public void cellNormal(PersoniumRequest req, String name) {
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name);
        this.res = request(req);
        checkSuccessResponse(res, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * 正常系のパターンのテスト(return DcResponse).
     * @param req DcRequestオブジェクト
     * @param name 作成するCellの名前
     * @return res this.resオブジェクト
     */
    public PersoniumResponse cellNormalResponse(PersoniumRequest req, String name) {
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name);
        this.res = request(req);
        checkSuccessResponse(res, MediaType.APPLICATION_JSON_TYPE);
        return this.res;
    }

    /**
     * Cellの一覧取得の正常系のテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellListNormal(PersoniumRequest req) {
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        this.res = request(req);
        checkCellListResponse(res, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Cellの一覧取得の正常系のテスト(XML).
     * @param req DcRequestオブジェクト
     */
    public void cellListNormalXml(PersoniumRequest req) {
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        this.res = request(req);
        String resContentType = res.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        assertEquals(MediaType.APPLICATION_ATOM_XML, resContentType.split(";")[0]);
    }

    /**
     * メソッドが不正なパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param name 作成するCellの名前
     */
    public void cellErrorInvalidMethod(PersoniumRequest req, String name) {

        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 405になることを確認
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR405-MC-0001");
    }

    /**
     * メソッドが不正なパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorInvalidMethod(PersoniumRequest req) {

        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 405になることを確認
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, res.getStatusCode());

    }

    /**
     * セル名重複のテスト.
     * @param req DcRequestオブジェクト
     * @param name 作成するCellの名前
     */
    public void cellConflict(PersoniumRequest req, String name) {

        // 1回目
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 201になることを確認
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

        // 2回目の要求
        PersoniumResponse resConflict = request(req);

        // Cell作成のレスポンスチェック
        // 409になることを確認
        try {
            assertEquals(HttpStatus.SC_CONFLICT, resConflict.getStatusCode());

            // レスポンスボディのチェック
            checkErrorResponse(resConflict.bodyAsJson(), PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS.getCode());
        } finally {
            cellDelete(resConflict);
        }
    }

    /**
     * 2つのCellを登録し、2回目のステータスコードを確認する。Cell名が重複でなければ201、重複であれば409であることを確認.
     * @param req DcRequestオブジェクト
     * @param method HTTPメソッド
     * @param url URL
     * @param name1 １つ目に作成するCellの名前
     * @param name2 ２つ目に作成するCellの名前
     */
    public void cellCreateResCheck(PersoniumRequest req, String method, String url, String name1, String name2) {

        // 1回目
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name1);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 201になることを確認
        assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

        // 2回目の要求
        PersoniumRequest req2 = null;
        if (method.equals(HttpMethod.POST)) {
            req2 = PersoniumRequest.post(url);
        }
        req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name2);
        PersoniumResponse resConflict = request(req2);

        // Cell作成のレスポンスチェック
        try {
            if (name1.equals(name2)) {
                // 409であることを確認
                assertEquals(HttpStatus.SC_CONFLICT, resConflict.getStatusCode());
                // レスポンスボディのチェック
                checkErrorResponse(resConflict.bodyAsJson(),
                        PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS.getCode());
            } else {
                // 201であることを確認
                assertEquals(HttpStatus.SC_CREATED, resConflict.getStatusCode());
            }
        } finally {
            cellDelete(resConflict);
        }
    }

    /**
     * リクエストボディが未指定のパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorEmptyBody(PersoniumRequest req) {
        // Cellを作成
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        this.res = request(req);
        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0001");
    }

    /**
     * Jsonがおかしいパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param name Cell名
     */
    public void cellErrorInvalidJson(PersoniumRequest req, String name) {
        // Cellを作成
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addStringBody(name);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0001");
    }

    /**
     * Xmlがおかしいパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param name Cell名
     */
    public void cellErrorInvalidXml(PersoniumRequest req, String name) {
        // Cellを作成
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addStringBody(name);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0001");
    }

    /**
     * リクエストボディに不正なフィールド名を指定した場合のテスト.
     * @param req DcRequestオブジェクト
     * @param name Cell名
     */
    public void cellErrorInvalidField(PersoniumRequest req, String name) {
        // Cellを作成
        String[] key = {"Name", "testKey" };
        String[] value = {name, "testValue" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

        // レスポンスボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0007");
    }

    /**
     * Cell名が0バイトのパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorName0(PersoniumRequest req) {
        cellErrorInvalidName(req, "");

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cell名が129バイトのパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorName129(PersoniumRequest req) {
        String name = "01234567890123456789012345678901234567890123456789"
                + "0123456789012345678901234567890123456789012345678901234567890123456789012345678";
        cellErrorInvalidName(req, name);

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cell名がa～zと0～9と‐と_以外のパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorNameCharacter(PersoniumRequest req) {
        String name = "テスト";
        cellErrorInvalidName(req, name);

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cell名が「__」のパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorNameUnderbaer(PersoniumRequest req) {
        String name = "__";
        cellErrorInvalidName(req, name);

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * Cell名が「__ctl」のパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorNameUnderbaerCtl(PersoniumRequest req) {
        String name = "__ctl";
        cellErrorInvalidName(req, name);

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR400-OD-0006");
    }

    /**
     * リクエストボディに管理情報である場合のパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param keyName 管理キー名
     */
    public void cellErrorBodyDateCtl(PersoniumRequest req, String keyName) {
        // Cellを作成
        String[] key = {"Name", keyName };
        String[] value = {"testCell", "/Date(0)/" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

    }

    /**
     * リクエストボディに管理情報である場合のパターンのテスト.
     * @param req DcRequestオブジェクト
     * @param keyName 管理キー名
     */
    public void cellErrorBodyMetadataCtl(PersoniumRequest req, String keyName) {
        // Cellを作成
        String[] key = {"Name", keyName };
        String[] value = {"testCell", "test" };
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody(key, value);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

    }

    /**
     * Cellの作成のNameが1文字のパターンのテスト.
     * @param req DcRequestオブジェクト.
     */
    public void cellName1(PersoniumRequest req) {
        String id = "0";
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", id);
        this.res = request(req);
        checkSuccessResponse(res, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Cellの作成のNameが128文字のパターンのテスト.
     * @param req DcRequestオブジェクト.
     */
    public void cellName128(PersoniumRequest req) {
        String id = "01234567890123456789012345678901234567890123456789"
                + "012345678901234567890123456789012345678901234567890123456789012345678901234567";
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", id);
        this.res = request(req);
        checkSuccessResponse(res, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * 認証エラーの処理.Authorizationヘッダがない.
     * @param req DcRequestオブジェクト.
     */
    private void cellErrorAccess(PersoniumRequest req) {
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 401になることを確認
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR401-AU-0001");
    }

    /**
     * 認証エラーの処理.Authorizationヘッダが異常.
     * @param req DcRequestオブジェクト.
     */
    private void cellErrorAuth(PersoniumRequest req) {
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 401になることを確認
        assertEquals(HttpStatus.SC_UNAUTHORIZED, res.getStatusCode());

        // ボディのチェック
        checkErrorResponse(res.bodyAsJson(), "PR401-AU-0003");
    }

    /**
     * 認証ヘッダが無いパターンのテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorAuthNone(PersoniumRequest req) {
        cellErrorAccess(req);
    }

    /**
     * 不正なトークンでの認証エラーテスト.
     * @param req DcRequestオブジェクト
     */
    public void cellErrorAuthInvalid(PersoniumRequest req) {
        req.header(HttpHeaders.AUTHORIZATION, "test");
        cellErrorAuth(req);
    }

    /**
     * 正常系のレスポンスチェック.
     * @param response response
     * @param contentType ContentType
     */
    private void checkSuccessResponse(PersoniumResponse response, MediaType contentType) {
        // Cell作成のレスポンスチェック
        // 201になることを確認
        assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        // LOCATIONヘッダを取得
        // レスポンスヘッダにLOCATIONが含まれているかチェック
        // LOCATIONヘッダが複数存在する場合もNGとする
        Header[] resHeaders = response.getResponseHeaders(HttpHeaders.LOCATION);
        assertEquals(1, resHeaders.length);

        // DataServiceVersionのチェック
        Header[] resDsvHeaders = response.getResponseHeaders(ODataConstants.Headers.DATA_SERVICE_VERSION);
        assertEquals(1, resDsvHeaders.length);
        assertEquals("2.0", resDsvHeaders[0].getValue());

        // Etagがあることのチェック
        Header[] resEtagHeaders = response.getResponseHeaders(HttpHeaders.ETAG);
        assertEquals(1, resEtagHeaders.length);

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            // ContentTypeのチェック
            Header[] resContentTypeHeaders = response.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
            assertEquals(1, resContentTypeHeaders.length);
            assertEquals(MediaType.APPLICATION_JSON, resContentTypeHeaders[0].getValue());

            // レスポンスボディのJsonもチェックが必要
            checkCellJson(response.bodyAsJson(), resHeaders[0].getValue());
        } else if (contentType == MediaType.APPLICATION_ATOM_XML_TYPE) {
            // ContentTypeのチェック
            Header[] resContentTypeHeaders = response.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
            assertEquals(1, resContentTypeHeaders.length);
            assertEquals(MediaType.APPLICATION_ATOM_XML, resContentTypeHeaders[0].getValue());

            // レスポンスボディのチェック
            checkCellXML(response.bodyAsXml());
        }
    }

    /**
     * 正常系の共通レスポンスヘッダーチェック.
     * @param response レスポンス
     * @param version 期待するEtagに含まれるバージョン
     */
    public static void checkCommonResponseHeader(TResponse response, long version) {
        checkCommonResponseHeader(response);

        // Etagのチェック
        checkEtag(response, version);
    }

    /**
     * 正常系の共通レスポンスヘッダーチェック.
     * @param response レスポンス
     */
    public static void checkCommonResponseHeader(TResponse response) {
        // DataServiceVersionのチェック
        response.checkHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");

        // ContentTypeのチェック
        // response.checkHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    /**
     * 正常系の共通レスポンスヘッダーチェック.
     * @param response レスポンス
     * @param location ヘッダーの期待値
     */
    public static void checkCommonResponseHeader(TResponse response, String location) {
        // LOCATIONヘッダを取得
        response.checkHeader(HttpHeaders.LOCATION, location);

        checkCommonResponseHeader(response);
    }

    /**
     * 正常系の共通レスポンスヘッダーチェック.
     * @param json レスポンス
     * @param uri ヘッダーの期待値
     */
    public static void checkCommonResponseUri(final JSONObject json, final ArrayList<String> uri) {
        String value;
        int count = 0;
        JSONObject jsonResult;

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");

        assertEquals(uri.size(), results.size());
        for (Object result : results) {
            jsonResult = (JSONObject) result;
            JSONObject resMetadata = (JSONObject) jsonResult.get("__metadata");
            value = (String) resMetadata.get("uri");
            assertEquals(uri.get(count++), value);
        }
    }

    /**
     * レスポンスボディの中に特定のuriが含まれないことをチェックする.
     * @param json レスポンス
     * @param uri レスポンスに含まれてほしくないuri
     */
    public static void checkResponseUriNotExsists(final JSONObject json, final String uri) {
        String value;
        JSONObject jsonResult;

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");

        for (Object result : results) {
            jsonResult = (JSONObject) result;
            JSONObject resMetadata = (JSONObject) jsonResult.get("__metadata");
            value = (String) resMetadata.get("uri");
            assertFalse(uri.equals(value));
        }
    }

    /**
     * Etagヘッダに含まれるバージョン情報のチェック.
     * @param response レスポンス
     * @param version 期待するEtagヘッダに含まれるバージョン情報
     */
    private static void checkEtag(TResponse response, long expectedVersion) {
        // Etag取得
        String etag = response.getHeader(HttpHeaders.ETAG);
        long version = getEtagVersion(etag);

        assertEquals(expectedVersion, version);
    }

    /**
     * Json形式のエラーレスポンスのチェック.
     * @param res レスポンス
     * @param expectedCode 期待するエラーコード
     * @param expectedMessage 期待するメッセージ
     */
    public static final void checkErrorResponseBody(TResponse res, String expectedCode, String expectedMessage) {
        String code = (String) ((JSONObject) res.bodyAsJson()).get("code");
        String message = (String) ((JSONObject) ((JSONObject) res.bodyAsJson()).get("message")).get("value");
        assertEquals(expectedCode, code);
        assertEquals(expectedMessage, message);
    }

    /**
     * Json形式のエラーレスポンスのチェック.
     * @param res レスポンス
     * @param expectedCode 期待するエラーコード
     * @param expectedMessage 期待するメッセージ
     */
    public static final void checkErrorResponseBody(PersoniumResponse res, String expectedCode,
            String expectedMessage) {
        JSONObject body = (JSONObject) res.bodyAsJson();
        String code = (String) body.get("code");
        String message = (String) ((JSONObject) body.get("message")).get("value");
        assertEquals(expectedCode, code);
        assertEquals(expectedMessage, message);
    }

    /**
     * Json形式のフォーマットチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     */
    public static final void checkResponseBody(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional) {
        checkResponseBody(json, locationHeader, type, additional, null, null);
    }

    /**
     * Json形式のフォーマットチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param np NP追加のチェック要素
     */
    public static final void checkResponseBody(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional, Map<String, String> np) {
        checkResponseBody(json, locationHeader, type, additional, np, null);
    }

    /**
     * Json形式のフォーマットチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param np NPのチェック要素
     * @param etag etagのチェック要素
     */
    public static final void checkResponseBody(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional, Map<String, String> np, String etag) {
        checkResponseBody(json, locationHeader, type, additional, np, etag, null);
    }

    /**
     * Json形式のフォーマットチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param np NPのチェック要素
     * @param etag etagのチェック要素
     * @param published publishedのチェック要素
     */
    public static final void checkResponseBody(final JSONObject json,
            final String locationHeader,
            final String type,
            final Map<String, Object> additional,
            Map<String, String> np,
            String etag,
            String published) {
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        checkResults(results, locationHeader, type, additional, np, etag, published);
    }

    /**
     * __countのチェック処理.
     * @param json レスポンスボディ
     * @param count チェック件数
     */
    public static final void checkResponseBodyCount(final JSONObject json, int count) {
        // __countのチェック処理
        if (count == COUNT_NONE) {
            // __countが返却されない場合
            assertEquals(null, ((JSONObject) json.get("d")).get("__count"));
        } else {
            // __countが返却される場合
            int rescount = Integer.parseInt((String) ((JSONObject) json.get("d")).get("__count"));
            assertEquals(count, rescount);
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param uri 期待するURIの値のリスト
     * @param type データのタイプ
     * @param additional 追加のチェック要素リスト
     * @param key エンティティキー
     */
    public static final void checkResponseBodyList(final JSONObject json, final Map<String, String> uri,
            final String type, final Map<String, Map<String, Object>> additional, String key) {
        checkResponseBodyList(json, uri, type, additional, key, null, null);
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param uri 期待するURIの値のリスト
     * @param type データのタイプ
     * @param additional 追加のチェック要素リスト
     * @param key エンティティキー
     * @param count 期待するカウントの値
     * @param etag etag
     */
    public static final void checkResponseBodyList(final JSONObject json, final Map<String, String> uri,
            final String type, final Map<String, Map<String, Object>> additional, String key,
            int count, Map<String, String> etag) {
        // __countのチェック
        checkResponseBodyCount(json, count);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            if (etag != null) {
                // Etagのチェック
                checkResponseEtagList(result, etag, key);
            }
            String reskey = (String) ((JSONObject) result).get(key);
            checkResults((JSONObject) result, uri.get(reskey), type, additional.get(reskey));
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param uri 期待するURIの値のリスト
     * @param type データのタイプ
     * @param additional 追加のチェック要素リスト
     * @param key エンティティキー
     * @param np NP追加のチェック要素リスト
     * @param etag etag
     */
    public static final void checkResponseBodyList(final JSONObject json, final Map<String, String> uri,
            final String type, final Map<String, Map<String, Object>> additional, String key, Map<String,
            String> np, Map<String, String> etag) {
        // __countのチェック
        checkResponseBodyCount(json, COUNT_NONE);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");

        // 取得件数のチェック
        if (uri == null) {
            assertEquals(0, results.size());
        } else {
            assertEquals(uri.size(), results.size());
        }

        // resultsのチェック
        for (Object result : results) {
            if (etag != null) {
                // Etagのチェック
                checkResponseEtagList(result, etag, key);
            }

            String reskey = (String) ((JSONObject) result).get(key);
            checkResults((JSONObject) result, uri.get(reskey), type, additional.get(reskey), np, null);
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param uri 期待するURIの値のリスト
     * @param type データのタイプ
     * @param additional 追加のチェック要素リスト
     * @param key エンティティキー
     * @param count 期待するカウントの値
     * @param np NP追加のチェック要素
     * @param etag etag
     */
    public static final void checkResponseBodyList(final JSONObject json,
            final Map<String, String> uri,
            final String type,
            final Map<String, Map<String, Object>> additional,
            String key,
            int count,
            final Map<String, Map<String, String>> np,
            Map<String, String> etag) {
        // __countのチェック
        checkResponseBodyCount(json, count);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            String reskey = (String) ((JSONObject) result).get(key);
            checkResults((JSONObject) result, uri.get(reskey), type, additional.get(reskey), np.get(reskey), null);
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     */
    public static final void checkResponseBodyList(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional) {
        // __countのチェック
        checkResponseBodyCount(json, COUNT_NONE);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            checkResults((JSONObject) result, locationHeader, type, additional);
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param count 期待するカウントの値
     */
    public static final void checkResponseBodyList(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional, int count) {
        // __countのチェック
        checkResponseBodyCount(json, count);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            checkResults((JSONObject) result, locationHeader, type, additional);
        }
    }

    /**
     * Json形式のフォーマットチェック(一覧用).
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param etag etag
     */
    public static final void checkResponseBodyList(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional, final Map<String, String> etag) {
        // __countのチェック
        checkResponseBodyCount(json, COUNT_NONE);

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");
        for (Object result : results) {
            checkResults((JSONObject) result, locationHeader, type, additional);
        }
    }

    /**
     * Etagチェック(一覧用).
     * @param result result
     * @param etag etag
     * @param key key
     */
    private static void checkResponseEtagList(Object result, Map<String, String> etag, String key) {
        String expectEtag;
        String resId = (String) ((JSONObject) result).get(key);
        JSONObject resMetadata = (JSONObject) ((JSONObject) result).get("__metadata");
        String resEtag = (String) resMetadata.get("etag");
        expectEtag = etag.get(resId);
        if (expectEtag != null) {
            assertEquals(expectEtag, resEtag);
        } else {
            fail();
        }
    }

    /**
     * Json形式のresultsチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     */
    public static final void checkResults(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional) {
        checkResults(json, locationHeader, type, additional, null, null);
    }

    /**
     * Json形式のresultsチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param np NPのチェック要素
     * @param etag etagのチェック要素
     */
    public static final void checkResults(final JSONObject json, final String locationHeader,
            final String type, final Map<String, Object> additional, Map<String, String> np, String etag) {
        checkResults(json, locationHeader, type, additional, np, etag, null);
    }

    /**
     * Json形式のresultsチェック.
     * @param json JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     * @param type データのタイプ
     * @param additional 追加のチェック要素
     * @param np NPのチェック要素
     * @param etag etagのチェック要素
     * @param published publishedのチェック要素
     */
    @SuppressWarnings("unchecked")
    public static final void checkResults(final JSONObject json,
            final String locationHeader,
            final String type,
            final Map<String, Object> additional,
            Map<String, String> np,
            String etag,
            String published) {
        String value;

        // d:__published要素の値が日付形式かどうかチェック
        value = (String) json.get("__published");
        assertNotNull(value);
        assertTrue(validDate(value));

        // d:__published要素の値が期待値と一致するかどうかチェック
        if (published != null) {
            assertEquals(published, value);
        }

        // d:__updated要素の値が日付形式かどうかチェック
        value = (String) json.get("__updated");
        assertNotNull(value);
        assertTrue(validDate(value));

        // __metadata要素
        JSONObject metadata = (JSONObject) json.get("__metadata");
        // uri要素
        value = (String) metadata.get("uri");
        assertNotNull(value);
        if (locationHeader != null) {
            // LOCATIONヘッダと等しいかチェック
            assertEquals(locationHeader, value);
        }
        // etag要素
        value = (String) metadata.get("etag");
        if (etag != null) {
            assertEquals(etag, value);
        }

        // type要素
        value = (String) metadata.get("type");
        assertNotNull(value);
        assertEquals(type, value);

        if (additional != null) {
            // 追加要素をチェック
            for (Map.Entry<String, Object> e : additional.entrySet()) {
                Object jsonValue = json.get(e.getKey());
                if (jsonValue instanceof Integer) {
                    jsonValue = ((Integer) jsonValue).longValue();
                }
                if (jsonValue instanceof JSONArray) {
                    JSONArray array = ((JSONArray) jsonValue);
                    for (int i = 0; i < array.size(); i++) {
                        if (array.get(i) instanceof Integer) {
                            array.set(i, ((Integer) array.get(i)).longValue());
                        }
                    }
                }
                Object expected = e.getValue();
                if (expected instanceof Integer) {
                    expected = ((Integer) expected).longValue();
                }
                if (expected instanceof JSONArray) {
                    JSONArray array = ((JSONArray) expected);
                    for (int i = 0; i < array.size(); i++) {
                        if (array.get(i) instanceof Integer) {
                            array.set(i, ((Integer) array.get(i)).longValue());
                        }
                    }
                }
                assertEquals(expected, jsonValue);
            }
        }

        if (np != null) {
            // NPの要素をチェック
            for (Map.Entry<String, String> e : np.entrySet()) {
                JSONObject jsonValue = (JSONObject) json.get(e.getKey());
                JSONObject defferedValue = (JSONObject) jsonValue.get("__deferred");
                String uriValue = (String) defferedValue.get("uri");
                assertEquals(e.getValue(), uriValue);
            }
        }
    }

    /**
     * $link一覧のレスポンスボディチェック.
     * @param json レスポンス
     * @param uri uriの期待値
     */
    public static void checkLinResponseBody(final JSONObject json, final ArrayList<String> uri) {
        String value;
        JSONObject jsonResult;

        // results
        JSONArray results = (JSONArray) ((JSONObject) json.get("d")).get("results");

        assertEquals(uri.size(), results.size());
        for (Object result : results) {
            jsonResult = (JSONObject) result;
            value = (String) jsonResult.get("uri");
            assertTrue("expected uri doesn't contain [" + value + "]", uri.contains(value));
        }
    }

    /**
     * Etag情報からバージョン情報を取得.
     * @param etag Etag
     * @return バージョン
     */
    public static long getEtagVersion(String etag) {
        // version取得
        Pattern pattern = Pattern.compile("^W/\"([0-9]+)-([0-9]+)\"$");
        Matcher m = pattern.matcher(etag);
        return Long.parseLong(m.replaceAll("$1"));
    }

    /**
     * Etag情報からUpdated情報を取得.
     * @param etag Etag
     * @return Updatedの値
     */
    public static long getEtagUpdated(String etag) {
        // version取得
        Pattern pattern = Pattern.compile("^W/\"([0-9]+)-([0-9]+)\"$");
        Matcher m = pattern.matcher(etag);
        return Long.parseLong(m.replaceAll("$2"));
    }

    /**
     * レスポンスからpublished情報を取得.
     * @param response response
     * @return publishedの値
     */
    public static String getPublished(TResponse response) {
        JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        return (String) results.get("__published");
    }

    /**
     * レスポンスからpublished情報を取得.
     * @param response response
     * @return publishedの値
     */
    public static String getPublished(PersoniumResponse response) {
        JSONObject results = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");
        return (String) results.get("__published");
    }

    /**
     * レスポンスからEtag情報を取得.
     * @param response response
     * @return Etagの値
     */
    public static String getEtag(PersoniumResponse response) {
        return response.getResponseHeaders(HttpHeaders.ETAG)[0].getValue();
    }

    /**
     * Cell名が不正のパターンの処理.
     * @param req DcRequestオブジェクト
     * @param name Cell名
     * @return レスポンスオブジェクト
     */
    private void cellErrorInvalidName(PersoniumRequest req, String name) {
        // Cellを作成
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", name);
        this.res = request(req);

        // Cell作成のレスポンスチェック
        // 400になることを確認
        assertEquals(HttpStatus.SC_BAD_REQUEST, res.getStatusCode());

    }

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     */
    private void checkCellXML(final Document doc) {
        checkCellResponse(doc);
    }

    /**
     * CellのJson形式のフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     * @param locationHeader LOCATIONヘッダ
     */
    private void checkCellJson(final JSONObject doc, final String locationHeader) {
        checkCellResponse(doc, locationHeader);
    }

    /**
     * CellのXML形式のフォーマットチェック.
     * @param doc Documentオブジェクト
     */
    private void checkCellResponse(final Document doc) {
        Node elm;

        // id要素
        elm = getElementByTagName(doc, "id");
        assertNotNull(elm);
        // id値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // title要素
        elm = getElementByTagName(doc, "title");
        assertNotNull(elm);
        // type属性が"text"かチェック
        assertEquals("text", getAttributeValue(elm, "type"));

        // updated要素
        elm = getElementByTagName(doc, "updated");
        assertNotNull(elm);
        // updated要素の値が日付形式かどうかチェック
        assertTrue(validISO8601a(elm.getTextContent()));

        // Name要素の存在チェック
        elm = getElementByTagName(doc, "Name");
        assertNotNull(elm);

        // link要素
        elm = getElementByTagName(doc, "link");
        assertNotNull(elm);
        assertEquals("edit", getAttributeValue(elm, "rel"));
        assertEquals("Cell", getAttributeValue(elm, "title"));
        assertTrue(getAttributeValue(elm, "href").length() > 0);

        // category要素
        elm = getElementByTagName(doc, "category");
        assertNotNull(elm);
        assertEquals(TYPE_CELL, getAttributeValue(elm, "term"));
        assertEquals("http://schemas.microsoft.com/ado/2007/08/dataservices/scheme", getAttributeValue(elm, "scheme"));

        // content要素
        elm = getElementByTagName(doc, "content");
        assertNotNull(elm);
        assertEquals("application/xml", getAttributeValue(elm, "type"));

        // d:__id要素
        elm = getElementByTagNameNS(doc, "__id", MS_DS);
        assertNotNull(elm);
        // id値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // d:Name要素
        elm = getElementByTagNameNS(doc, "Name", MS_DS);
        assertNotNull(elm);
        // name値が空かどうかチェック
        assertTrue(elm.getTextContent().length() > 0);

        // d:__published要素
        elm = getElementByTagNameNS(doc, "__published", MS_DS);
        assertNotNull(elm);
        // __published要素の値が日付形式かどうかチェック
        assertTrue(validISO8601b(elm.getTextContent()));
        assertEquals("Edm.DateTime", getAttributeValueNS(elm, "type", MS_MD));

        // d:__updated要素
        elm = getElementByTagNameNS(doc, "__updated", MS_DS);
        assertNotNull(elm);
        // __updated要素の値が日付形式かどうかチェック
        assertTrue(validISO8601b(elm.getTextContent()));
        assertEquals("Edm.DateTime", getAttributeValueNS(elm, "type", MS_MD));
    }

    /**
     * DOMから、指定した要素名のタグを取得する.
     * @param doc Documentオブジェクト
     * @param name 取得する要素名
     * @return 取得したNodeオブジェクト（存在しない場合はnull)
     */
    private Node getElementByTagName(final Document doc, final String name) {
        NodeList nl = doc.getElementsByTagName(name);
        if (nl.getLength() > 0) {
            return nl.item(0);
        } else {
            return null;
        }
    }

    /**
     * 名前空間を指定してDOMから要素を取得する.
     * @param doc Documentオブジェクト
     * @param name 取得する要素名
     * @param ns 名前空間名
     * @return 取得したNodeオブジェクト
     */
    private Node getElementByTagNameNS(final Document doc, final String name, final String ns) {
        NodeList nl = doc.getElementsByTagNameNS(ns, name);
        if (nl.getLength() > 0) {
            return nl.item(0);
        } else {
            return null;
        }
    }

    /**
     * Node内の指定した属性値を取得する.
     * @param node 対象となる要素(Nodeオブジェクト)
     * @param name 取得する属性名
     * @return 取得した属性値
     */
    private String getAttributeValue(final Node node, final String name) {
        NamedNodeMap nnm = node.getAttributes();
        Node attr = nnm.getNamedItem(name);
        if (attr != null) {
            return attr.getNodeValue();
        } else {
            return "";
        }
    }

    /**
     * 名前空間を指定してNode内の指定した属性値を取得する.
     * @param node 対象となる要素(Nodeオブジェクト)
     * @param name 取得する属性名
     * @param ns 名前空間名
     * @return 取得した属性値
     */
    private String getAttributeValueNS(final Node node, final String name, final String ns) {
        NamedNodeMap nnm = node.getAttributes();
        Node attr = nnm.getNamedItemNS(ns, name);
        if (attr != null) {
            return attr.getNodeValue();
        } else {
            return "";
        }
    }

    /**
     * 指定した文字列形式の日付が、日付形式なのかチェックする.
     * @param src チェック対象の日付文字列
     * @return true:日付形式、false：日付形式ではない
     */
    private Boolean validISO8601a(final String src) {
        // FastDateFormat fastDateFormat =
        // org.apache.commons.lang.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss'Z'"};
        try {
            org.apache.commons.lang.time.DateUtils.parseDate(src, patterns);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     * 指定した文字列形式の日付が、日付形式なのかチェックする.
     * @param src チェック対象の日付文字列
     * @return true:日付形式、false：日付形式ではない
     */
    private Boolean validISO8601b(final String src) {
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSS"};
        try {
            org.apache.commons.lang.time.DateUtils.parseDate(src, patterns);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     * CellのJson形式のリストのフォーマットチェック.
     * @param response DcResponseオブジェクト
     * @param contentType レスポンスのContentType
     */
    private void checkCellListResponse(PersoniumResponse response, MediaType contentType) {

        // Cell作成のレスポンスチェック
        // 200になることを確認
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());

        // DataServiceVersionのチェック
        Header[] resDsvHeaders = response.getResponseHeaders(ODataConstants.Headers.DATA_SERVICE_VERSION);
        assertEquals(1, resDsvHeaders.length);
        assertEquals("2.0", resDsvHeaders[0].getValue());

        // ContentTypeのチェック
        Header[] resContentTypeHeaders = response.getResponseHeaders(HttpHeaders.CONTENT_TYPE);
        assertEquals(1, resContentTypeHeaders.length);
        String value = resContentTypeHeaders[0].getValue();
        String[] values = value.split(";");
        assertEquals(contentType.toString(), values[0]);

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            // レスポンスボディのJsonもチェックが必要
            checkCellListResponse(response.bodyAsJson());

        } else if (contentType == MediaType.APPLICATION_ATOM_XML_TYPE) {
            // TODO レスポンスボディのチェック
            fail("Not Implemented.");
            // checkCellXML(response.bodyAsXml());
        }
    }

    /**
     * CellのJson形式のリストのフォーマットチェック.
     * @param doc JSONObjectオブジェクト
     */
    private void checkCellListResponse(JSONObject doc) {
        JSONArray arResults = (JSONArray) ((JSONObject) doc.get("d")).get("results");
        for (Object obj : arResults) {
            JSONObject results = (JSONObject) obj;
            validateCellResponse(results, null);
        }
    }
}

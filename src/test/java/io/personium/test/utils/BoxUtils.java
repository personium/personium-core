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
package io.personium.test.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import io.personium.test.jersey.DcException;
import io.personium.test.jersey.DcResponse;
import io.personium.test.jersey.DcRestAdapter;
import io.personium.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class BoxUtils {
    private BoxUtils() {
    }

    /**
     * BOX取得ユーティリティ.
     * @param cellName cellName
     * @param token token
     * @param boxName boxName
     * @param code レスポンスコード
     */
    public static void get(final String cellName, final String token, final String boxName,
            final int code) {
        // Box削除
        Http.request("cell/box-get.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .returns()
                .statusCode(code);
    }

    /**
     * BOX取得ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @return レスポンス
     * @throws DcException リクエスト失敗
     */
    public static DcResponse getWithAuthSchema(final String cellName,
            final String boxName,
            final String authorization) throws DcException {
        DcRestAdapter adaper = new DcRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);

        return adaper.get(UrlUtils.cellCtl(cellName, "Box", boxName), header);
    }

    /**
     * BOX作成ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param token token
     * @return レスポンス
     */
    public static TResponse create(final String cellName, final String boxName, final String token) {
        // Box作成
        return Http.request("cell/box-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * BOX作成ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param token token
     * @param code レスポンスコード
     */
    public static void create(final String cellName, final String boxName, final String token, int code) {
        // Box作成
        Http.request("cell/box-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .returns()
                .statusCode(code);
    }

    /**
     * BOX作成ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @return レスポンス
     * @throws DcException リクエスト失敗
     */
    public static DcResponse createWithAuthSchema(final String cellName,
            final String boxName,
            final String authorization) throws DcException {
        DcRestAdapter adaper = new DcRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);

        String body = String.format("{\"Name\":\"%s\"}", boxName);

        return adaper.post(UrlUtils.cellCtl(cellName, "Box"), body, header);
    }

    /**
     * スキーマ付きBOX作成ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param token token
     * @param schema schema
     */
    public static void createWithSchema(final String cellName, final String boxName,
            final String token, final String schema) {
        // Box作成
        Http.request("cell/box-create-with-scheme.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("schema", schema)
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * BOX NP経由登録ユーティリティ.
     * @param token トークン
     * @param cellName Cell名
     * @param sourceEntityType ソース側EntityType名
     * @param sourceEntityKeyString ソース側キー(例："Name='test'")
     * @param body リクエストボディ
     * @return レスポンス
     */
    public static TResponse createViaNP(
            final String token,
            final String cellName,
            final String sourceEntityType,
            final String sourceEntityKeyString,
            final String body) {
        // Box作成
        return createViaNP(token, cellName, sourceEntityType, sourceEntityKeyString, body, HttpStatus.SC_CREATED);
    }

    /**
     * BOX NP経由登録ユーティリティ.
     * @param token トークン
     * @param cellName Cell名
     * @param sourceEntityType ソース側EntityType名
     * @param sourceEntityKeyString ソース側キー(例："Name='test'")
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse createViaNP(
            final String token,
            final String cellName,
            final String sourceEntityType,
            final String sourceEntityKeyString,
            final String body,
            final int code) {
        // Box作成
        return Http.request("cell/createNPWithoutQuote.txt")
                .with("method", HttpMethod.POST)
                .with("token", "Bearer " + token)
                .with("cell", cellName)
                .with("entityType", sourceEntityType)
                .with("id", sourceEntityKeyString)
                .with("navPropName", "_Box")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("body", body)
                .returns()
                .statusCode(code);
    }

    /**
     * BOX一覧取得ユーティリティ.
     * @param cellName cellName
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @return レスポンス
     * @throws DcException リクエスト失敗
     */
    public static DcResponse listWithAuthSchema(final String cellName,
            final String authorization) throws DcException {
        DcRestAdapter adaper = new DcRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);

        return adaper.get(UrlUtils.cellCtl(cellName, "Box"), header);
    }

    /**
     * BOX NP経由一覧取得ユーティリティ.
     * @param token トークン
     * @param cellName Cell名
     * @param sourceEntityType ソース側EntityType名
     * @param sourceEntityKeyString ソース側キー(例："Name='test'")
     * @return レスポンス
     */
    public static TResponse listViaNP(
            final String token,
            final String cellName,
            final String sourceEntityType,
            final String sourceEntityKeyString) {
        // Box作成
        return Http.request("cell/listViaNP.txt")
                .with("token", "Bearer " + token)
                .with("cell", cellName)
                .with("entityType", sourceEntityType)
                .with("id", sourceEntityKeyString)
                .with("navPropName", "_Box")
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * BOX更新ユーティリティ.
     * @param cellName cellName
     * @param token token
     * @param boxName boxName
     * @param etag etag
     * @param newName 新ボックス名
     * @param schema schema
     * @param code レスポンスコード
     */
    public static void update(final String cellName, final String token, final String boxName,
            final String etag, final String newName, final String schema, final int code) {
        Http.request("cell/box-update.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("token", token)
                .with("etag", etag)
                .with("newBoxPath", newName)
                .with("schema", schema)
                .returns()
                .statusCode(code);
    }

    /**
     * BOX更新ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param newName 新ボックス名
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @return レスポンス
     * @throws DcException リクエスト失敗
     */
    public static DcResponse updateWithAuthSchema(final String cellName,
            final String boxName,
            final String newName,
            final String authorization) throws DcException {
        DcRestAdapter adaper = new DcRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);

        String body = String.format("{\"Name\":\"%s\"}", newName);

        return adaper.put(UrlUtils.cellCtl(cellName, "Box", boxName), body, header);
    }

    /**
     * BOX削除ユーティリティ.
     * @param cellName cellName
     * @param token token
     * @param boxName boxName
     */
    public static void delete(final String cellName, final String token, final String boxName) {
        // Box削除
        Http.request("cell/box-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * BOX削除ユーティリティ.
     * @param cellName cellName
     * @param token token
     * @param boxName boxName
     * @param code レスポンスコード
     */
    public static void delete(final String cellName, final String token, final String boxName,
            final int code) {
        // Box削除
        TResponse res = Http.request("cell/box-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .returns()
                .statusCode(code);
        if (code != -1) {
            assertEquals(code, res.getStatusCode());
        }
    }

    /**
     * BOX削除ユーティリティ.
     * @param cellName cellName
     * @param boxName boxName
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @return レスポンス
     * @throws DcException リクエスト失敗
     */
    public static DcResponse deleteWithAuthSchema(final String cellName,
            final String boxName,
            final String authorization) throws DcException {
        DcRestAdapter adaper = new DcRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);

        return adaper.del(UrlUtils.cellCtl(cellName, "Box", boxName), header);
    }

    /**
     * eventのPOSTを行うユーティリティ.
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param cellName セル名
     * @param boxName ボックス名
     * @param jsonBody リクエストボディ
     */
    public static void event(String token, int code, String cellName, String boxName, String jsonBody) {
        Http.request("cell/event-post.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("boxName", boxName)
                .with("json", jsonBody)
                .returns()
                .statusCode(code);
    }

    /**
     * boxレベルのpropfind(allprop指定有)を行うユーティリティ.
     * @param cellName セル名
     * @param path Box名
     * @param depth Depthヘッダの値
     * @param token トークン(Bearerなし)
     * @param code 期待するレスポンスコード
     * @return tresponse レスポンス
     */
    public static TResponse propfind(String cellName, String path, String depth, String token, int code) {
        TResponse tresponse = Http.request("box/propfind-box-allprop.txt")
                .with("cellPath", cellName)
                .with("path", path)
                .with("depth", depth)
                .with("token", token)
                .returns();
        tresponse.statusCode(code);

        return tresponse;
    }

    /**
     * boxレベルのpropfind(リクエストボディなし)を行うユーティリティ.
     * @param cellName セル名
     * @param path Box名
     * @param depth Depthヘッダの値
     * @param token トークン(Bearerなし)
     * @param withContentLength ContentLengthヘッダを付加するかどうか
     * @param code 期待するレスポンスコード
     * @return tresponse レスポンス
     */
    public static TResponse propfind(String cellName, String path, String depth,
            String token, boolean withContentLength, int code) {
        TResponse tresponse = null;
        if (withContentLength) {
            tresponse = Http.request("box/propfind-box-body-0.txt")
                    .with("cellPath", cellName)
                    .with("path", path)
                    .with("depth", depth)
                    .with("token", token)
                    .returns();
            tresponse.statusCode(code);
        } else {
            tresponse = Http.request("box/propfind-box-body-0-non-content-length.txt")
                    .with("cellPath", cellName)
                    .with("path", path)
                    .with("depth", depth)
                    .with("token", token)
                    .returns();
            tresponse.statusCode(code);
        }

        return tresponse;
    }
}

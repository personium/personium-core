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

import java.io.File;
import java.util.HashMap;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.common.utils.CommonUtils;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumException;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumRestAdapter;
import io.personium.test.jersey.bar.BarInstallTestUtils;
import io.personium.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class CellUtils {
    private static final long WAIT_TIME_BULK_DELETE = 1000L;

    private CellUtils() {
    }

    /**
     * Cellを1件取得するユーティリティ.
     * @param cellName セル名
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse getWithAnyAuthSchema(final String cellName, final String tokenWithAuthSchema, int code) {
        return Http.request("cell-retrieve.txt")
                .with("token", tokenWithAuthSchema)
                .with("cellPath", cellName)
                .returns()
                .statusCode(code);
    }

    /**
     * Cellを1件取得するユーティリティ(OAuth2認証用).
     * @param cellName セル名
     * @param token トークン(Bearer 以降の文字列)
     * @param code 返却値
     */
    public static void get(final String cellName, final String token, final int code) {
        // Cell作成
        TResponse res = Http.request("cell-retrieve.txt")
                .with("token", "Bearer " + token)
                .with("cellPath", cellName)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * オーナー指定でCellを1件取得するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param owner X-Personium-Unit-Userに指定する値
     * @param code 返却値
     */
    public static void get(final String cellName, final String token, final String owner, final int code) {
        // Cell1件取得
        TResponse res = Http.request("cell-retrieveWithOwner.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("owner", owner)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * Cellを一覧取得するユーティリティ.
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse listWithAnyAuthSchema(final String tokenWithAuthSchema, final int code) {
        // Cell一覧取得
        TResponse res = Http.request("cell-get.txt")
                .with("token", tokenWithAuthSchema)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * Cellを一覧取得するユーティリティ(OAuth2認証用).
     * @param token トークン(Bearer 以降の文字列)
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse list(final String token, final int code) {
        // Cell一覧取得
        TResponse res = Http.request("cell-get.txt")
                .with("token", "Bearer " + token)
                .returns()
                .statusCode(code);
        res.getStatusCode();
        return res;
    }

    /**
     * オーナー指定でCellを一覧取得するユーティリティ.
     * @param token トークン
     * @param owner X-Personium-Unit-Userに指定する値
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse list(final String token, final String owner, final int code) {
        // Cell一覧取得
        TResponse res = Http.request("cell-getWithOwner.txt")
                .with("token", token)
                .with("owner", owner)
                .returns()
                .statusCode(code);
        res.getStatusCode();
        return res;
    }

    /**
     * Cellを作成するユーティリティ.
     * @param cellName セル名
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse createWithAnyAuthSchema(final String cellName, final String tokenWithAuthSchema, int code) {
        // Cell作成
        return Http.request("cell-create.txt")
                .with("token", tokenWithAuthSchema)
                .with("cellPath", cellName)
                .returns()
                .statusCode(code);
    }

    /**
     * Cellを作成するユーティリティ(OAuth2認証用).
     * @param cellName セル名
     * @param token トークン(Bearer 以降の文字列)
     * @param code 返却値
     */
    public static void create(final String cellName, final String token, int code) {
        // Cell作成
        TResponse res = Http.request("cell-create.txt")
                .with("token", "Bearer " + token)
                .with("cellPath", cellName)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * オーナー指定でCellを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param owner X-Personium-Unit-Userに指定する値
     * @param code 返却値
     */
    public static void create(final String cellName, final String token, final String owner, final int code) {
        // Cell作成
        TResponse res = Http.request("cell-createWithOwner.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("owner", owner)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * セルレベルNP作成ユーティリティ.
     * @param method メソッド名
     * @param cell セル名
     * @param entityType エンティティ名
     * @param id エンティティID
     * @param navPropName navPropName
     * @param body リクエストボディ
     * @param token 認証トークン
     * @param sc レスポンスコード
     * @return レスポンス
     */
    public static TResponse createNp(String method, String cell, String entityType,
            String id, String navPropName, JSONObject body, String token, int sc) {
        TResponse res = Http.request("cell/createNP.txt")
                .with("method", method)
                .with("cell", cell)
                .with("entityType", entityType)
                .with("id", id)
                .with("navPropName", navPropName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("body", body.toString())
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * Cellを更新するユーティリティ.
     * @param cellPath 更新前のセル名
     * @param cellName 更新後のセル名
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param code 返却値
     * @return レスポンス
     */
    public static TResponse updateWithAnyAuthSchema(final String cellPath,
            final String cellName,
            final String tokenWithAuthSchema,
            int code) {
        return Http.request("cell-update.txt")
                .with("token", tokenWithAuthSchema)
                .with("cellPath", cellPath)
                .with("cellName", cellName)
                .returns()
                .statusCode(code);
    }

    /**
     * Cellを更新するユーティリティ(OAuth2認証用).
     * @param cellPath 更新前のセル名
     * @param cellName 更新後のセル名
     * @param token トークン(Bearer 以降の文字列)
     * @param code 返却値
     */
    public static void update(final String cellPath, final String cellName, final String token, int code) {
        // Cell作成
        TResponse res = Http.request("cell-update.txt")
                .with("token", "Bearer " + token)
                .with("cellPath", cellPath)
                .with("cellName", cellName)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * オーナー指定でCellを更新するユーティリティ.
     * @param cellPath 更新前のセル名
     * @param cellName 更新後のセル名
     * @param token トークン
     * @param owner X-Personium-Unit-Userに指定する値
     * @param code 返却値
     */
    public static void update(final String cellPath, final String cellName, final String token,
            final String owner, int code) {
        // Cell更新
        TResponse res = Http.request("cell-updateWithOwner.txt")
                .with("token", token)
                .with("cellPath", cellPath)
                .with("cellName", cellName)
                .with("owner", owner)
                .returns()
                .statusCode(code);
        res.getStatusCode();
    }

    /**
     * Cellを削除するユーティリティー.
     * @param token 利用トークン
     * @param cellName 対象セル名
     */
    public static void delete(final String token, final String cellName) {
        delete(token, cellName, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Cellを削除するユーティリティー(OAuth2認証用).
     * @param token 利用トークン(Bearer 以降の文字列)
     * @param cellName 対象セル名
     * @param code チェックするステータスコード
     */
    public static void delete(final String token, final String cellName, int code) {
        // Cell削除
        Http.request("cell-delete.txt")
                .with("token", "Bearer " + token)
                .with("cellName", cellName)
                .returns()
                .statusCode(code);
    }

    /**
     * Cellを削除するユーティリティー.
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param cellName 対象セル名
     * @param code チェックするステータスコード
     * @return レスポンス
     */
    public static TResponse deleteWithAnyAuthSchema(final String tokenWithAuthSchema, final String cellName, int code) {
        // Cell削除
        return Http.request("cell-delete.txt")
                .with("token", tokenWithAuthSchema)
                .with("cellName", cellName)
                .returns()
                .statusCode(code);
    }

    /**
     * Cellを削除するユーティリティー.
     * @param token 利用トークン
     * @param cellName 対象セル名
     * @param owner オーナー
     * @param code チェックするステータスコード
     */
    public static void delete(final String token, final String cellName, String owner, int code) {
        // Cell削除
        TResponse res = Http.request("cell-deleteWithOwner.txt")
                .with("token", token)
                .with("cellName", cellName)
                .with("owner", owner)
                .returns();
        if (code != -1) {
            assertEquals(code, res.getStatusCode());
        }
    }

    /**
     * Cell再帰的削除ユーティリティー.
     * @param tokenWithAuthSchema トークン(auth-schemaを含む文字列)
     * @param cellName 対象セル名
     * @return レスポンス
     */
    public static PersoniumResponse bulkDeletion(final String tokenWithAuthSchema, final String cellName) {
        // セル再帰的削除APIを実行する
        PersoniumRequest request = PersoniumRequest.delete(UrlUtils.cellRoot(cellName));
        request.header(HttpHeaders.AUTHORIZATION, tokenWithAuthSchema)
                .header("X-Personium-Recursive", "true");
        PersoniumResponse response = AbstractCase.request(request);
        // Sleep 1 second for asynchronous processing.
        try {
            Thread.sleep(WAIT_TIME_BULK_DELETE);
        } catch (InterruptedException e) {
            System.out.println("");
        }
        return response;
    }

    /**
     * PROPFINDを行うユーティリティー.
     * @param url URL
     * @param token 認証トークン
     * @param depth Depthヘッダー
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse propfind(String url, String token, String depth, int code) {
        TResponse tresponse = Http.request("cell/propfind-cell-allprop.txt")
                .with("url", url)
                .with("token", token)
                .with("depth", depth)
                .returns();
        tresponse.statusCode(code);

        return tresponse;
    }

    /**
     * PROPFINDを行うユーティリティー.
     * @param cellName Cell名
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @param depth Depthヘッダー
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse propfindWithAnyAuthSchema(String cellName, String authorization, String depth, int code) {
        TResponse tresponse = Http.request("cell/propfind-cell-allprop-anyAuthSchema.txt")
                .with("url", cellName)
                .with("authorization", authorization)
                .with("depth", depth)
                .returns();
        tresponse.statusCode(code);

        return tresponse;
    }

    /**
     * PROPFINDを行うユーティリティー(リクエストファイル名指定).
     * @param url URL
     * @param filename リクエストに使用するファイル名
     * @param token 認証トークン
     * @param depth Depthヘッダー
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse propfind(String url, String filename, String token, String depth, int code) {
        TResponse tresponse = Http.request(filename)
                .with("url", url)
                .with("token", token)
                .with("depth", depth)
                .returns();
        tresponse.statusCode(code);

        return tresponse;
    }

    /**
     * Set property.
     * @param cellName cell name
     * @param property property string. ex:<p:foo>bar</p:foo>
     * @param token token
     * @param code expected response code
     * @return response
     */
    public static TResponse proppatchSet(String cellName, String property, String token, int code) {
        return Http.request("cell/proppatch-set.txt")
                .with("cellName", cellName)
                .with("property", property)
                .with("token", token)
                .returns().debug().statusCode(code);
    }

    /**
     * Remove property.
     * @param cellName cell name
     * @param property property string. ex:<p:foo/>
     * @param token token
     * @param code expected response code
     * @return response
     */
    public static TResponse proppatchRemove(String cellName, String property, String token, int code) {
        return Http.request("cell/proppatch-remove.txt")
                .with("cellName", cellName)
                .with("property", property)
                .with("token", token)
                .returns().debug().statusCode(code);
    }

    /**
     * eventのPROPPATCHを行うユーティリティ.
     * @param cell cell
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param values 設定値
     * @return レスポンス
     */
    public static TResponse proppatchEvent(String cell, String token, int code, String... values) {
        // PROPPATCH設定実行
        TResponse tresponse = Http.request("cell/event-proppacth.txt")
                .with("cellPath", cell)
                .with("token", token)
                .with("author1", values[0])
                .with("hoge", values[1])
                .returns();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * eventのPROPPATCHを行うユーティリティ.
     * @param cell cell
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code レスポンスコード
     * @param values 設定値
     * @return レスポンス
     */
    public static TResponse proppatchWithAnyAuthSchema(String cell, String authorization, int code, String... values) {
        // PROPPATCH設定実行
        return Http.request("cell/proppacth-anyAuthSchema.txt")
                .with("cellPath", cell)
                .with("authorization", authorization)
                .with("author1", values[0])
                .returns()
                .statusCode(code);
    }

    /**
     * eventのPOSTを行うユーティリティ.
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param cellName セル名
     * @param jsonBody リクエストボディ
     */
    public static void event(String token, int code, String cellName, String jsonBody) {
        Http.request("cell/cell-event.txt")
                .with("METHOD", HttpMethod.POST)
                .with("token", token)
                .with("cellPath", cellName)
                .with("requestKey", "")
                .with("json", jsonBody)
                .returns()
                .statusCode(code);
    }

    /**
     * Post event.
     * @param authorization Authorization Header(including auth-shema)
     * @param code response code
     * @param cellName cell name
     * @param type event type
     * @param object event object
     * @param info event info
     * @return response
     */
    @SuppressWarnings("unchecked")
    public static TResponse eventWithAnyAuthSchema(String authorization,
            int code,
            String cellName,
            String type,
            String object,
            String info) {
        JSONObject body = new JSONObject();
        body.put("Type", type);
        body.put("Object", object);
        body.put("Info", info);
        return Http.request("cell/cell-event-anyAuthSchema.txt")
                .with("METHOD", HttpMethod.POST)
                .with("authorization", authorization)
                .with("cellPath", cellName)
                .with("requestKey", "")
                .with("json", body.toJSONString())
                .returns()
                .statusCode(code);
    }

    /**
     * eventのGETを行うユーティリティ(TResponse).
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param cellName セル名
     * @param collection ログの種別（"current" or "archive")
     * @param fileName ログのパス("default.log" or "default.log.{no}")
     * @return レスポンス
     */
    public static TResponse getLog(String token, int code, String cellName, String collection, String fileName) {
        TResponse response = Http.request("cell/log-get.txt")
                .with("METHOD", HttpMethod.GET)
                .with("token", token)
                .with("cellPath", cellName)
                .with("collection", collection)
                .with("fileName", fileName)
                .with("ifNoneMatch", "*")
                .returns();
        response.statusCode(code);
        return response;
    }

    /**
     * eventのGETを行うユーティリティ(DcResponse).
     * @param cellName セル名
     * @param collection ログの種別（"current" or "archive")
     * @param fileName ログのパス("default.log" or "default.log.{no}")
     * @return レスポンス DcResponse
     * @throws PersoniumException DcException
     */
    public static PersoniumResponse getLog(String cellName, String collection, String fileName)
            throws PersoniumException {
        PersoniumRestAdapter adaper = new PersoniumRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        return adaper.get(UrlUtils.cellRoot(cellName) + "__log/" + collection + "/" + fileName, header);
    }

    /**
     * Utility to delete event.
     * @param cellName cell name
     * @param collection Log type（"current" or "archive")
     * @param fileName log path("default.log" or "default.log.{no}")
     * @return PersoniumResponse
     * @throws PersoniumException Exception
     */
    public static PersoniumResponse deleteLog(String cellName, String collection, String fileName)
            throws PersoniumException {
        PersoniumRestAdapter adaper = new PersoniumRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        return adaper.del(UrlUtils.cellRoot(cellName) + "__log/" + collection + "/" + fileName, header);
    }

    /**
     * eventのGETを行うユーティリティ.
     * @param cellName セル名
     * @param fileName ログのパス("default.log" or "default.log.{no}")
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @return レスポンス DcResponse
     * @throws PersoniumException DcException
     */
    public static PersoniumResponse getCurrentLogWithAnyAuth(String cellName,
            String fileName,
            String authorization) throws PersoniumException {
        PersoniumRestAdapter adaper = new PersoniumRestAdapter();
        HashMap<String, String> header = new HashMap<String, String>();
        header.put(HttpHeaders.AUTHORIZATION, authorization);
        return adaper.get(UrlUtils.cellRoot(cellName) + "__log/current/" + fileName, header);
    }

    /**
     * PROPFINDを行うユーティリティー.
     * @param cellName Cell名
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @param depth Depthヘッダー
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse propfindArchiveLogDir(String cellName, String authorization, String depth, int code) {
        TResponse tresponse = Http.request("cell/propfind-cell-allprop-anyAuthSchema.txt")
                .with("url", cellName + "/__log/archive")
                .with("authorization", authorization)
                .with("depth", depth)
                .returns();
        tresponse.statusCode(code);

        return tresponse;
    }

    /**
     * パスワード変更を行うユーティリティ(__mypassword).
     * @param cellName Cell名
     * @param newPassword 新しいパスワード
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @return レスポンス
     * @throws PersoniumException リクエスト失敗
     */
    public static PersoniumResponse changePassword(String cellName, String newPassword, String authorization)
            throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, authorization);
        requestheaders.put(CommonUtils.HttpHeaders.X_PERSONIUM_CREDENTIAL, newPassword);

        return rest.put(UrlUtils.cellRoot(cellName) + "__mypassword", "", requestheaders);
    }

    /**
     * スキーマ認証をBasic認証にて行うユーティリティ.
     * @param cellName スキーマ認証を行うCell名
     * @param account スキーマ認証を行うCellに登録されたアカウント
     * @param password スキーマ認を行うCellに登録されたパスワード
     * @param schemaCell スキーマ認証元Cell名
     * @param schemaAccount スキーマ認証元Cellに登録されたアカウント
     * @param schemaPassword スキーマ認証元Cellに登録されたパスワード
     * @return レスポンス
     * @throws PersoniumException リクエスト失敗
     */
    public static PersoniumResponse schemaAuthenticateWithBasic(
            String cellName,
            String account,
            String password,
            String schemaCell,
            String schemaAccount,
            String schemaPassword) throws PersoniumException {
        // スキーマ認証元Cellでトークン認証
        TResponse res = tokenAuthenticationWithTarget(schemaCell, schemaAccount, schemaPassword, cellName);
        String schemaAuthenticatedToken = (String) res.bodyAsJson().get("access_token");

        // スキーマ認証(Basic認証)
        PersoniumRestAdapter rest = new PersoniumRestAdapter();
        // リクエストヘッダをセット
        String schemaCellUrl = UrlUtils.cellRoot(schemaCell);
        String authorization =
                CommonUtils.createBasicAuthzHeader(schemaCellUrl, schemaAuthenticatedToken);

        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        requestheaders.put(HttpHeaders.AUTHORIZATION, authorization);

        String body = String.format("grant_type=password&username=%s&password=%s", account, password);
        return rest.post(UrlUtils.cellRoot(cellName) + "__token", body, requestheaders);
    }

    /**
     * ターゲット指定でパスワード認証を行うユーティリティ（トランスセルトークン取得）.
     * @param cellName Cell名
     * @param account アカウント名
     * @param pass パスワード
     * @param targetCell 払い出されるトークンを使う先（セル名）
     * @return トークン
     */
    public static TResponse tokenAuthenticationWithTarget(String cellName,
            String account,
            String pass,
            String targetCell) {
        String targetUrl = UrlUtils.cellRoot(targetCell);
        return Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", cellName)
                .with("username", account)
                .with("password", pass)
                .with("p_target", targetUrl)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

    /**
     * ImplicitFlow認証を行うユーティリティ.
     * @param cellName Cell名
     * @param schemaCell スキーマ認証元Cell名
     * @param schemaAccount スキーマ認証元Cellに登録されたアカウント
     * @param schemaPassword スキーマ認証元Cellに登録されたアカウント
     * @param redirectPath リダイレクト先のパス
     * @param state リクエストとコールバックの間で状態を維持するために使用するランダムな値
     * @param addHeader 追加のヘッダ情報
     * @return レスポンス
     * @throws PersoniumException リクエスト失敗
     */
    public static PersoniumResponse implicitflowAuthenticate(String cellName,
            String schemaCell,
            String schemaAccount,
            String schemaPassword,
            String redirectPath,
            String state,
            HashMap<String, String> addHeader) throws PersoniumException {
        String clientId = UrlUtils.cellRoot(schemaCell);
        if (null == redirectPath) {
            redirectPath = "__/redirect.html";
        }

        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + clientId + redirectPath
                + "&username=" + schemaAccount
                + "&password=" + schemaPassword
                + "&state=" + state;

        PersoniumRestAdapter rest = new PersoniumRestAdapter();

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        if (addHeader != null) {
            requestheaders.putAll(addHeader);
        }
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        return rest.post(UrlUtils.cellRoot(cellName) + "__authz", body, requestheaders);
    }

    /**
     * ImplicitFlow password change util.
     * @param cellName cell name
     * @param schemaCell schema cell name
     * @param apTokenStr password change access token string
     * @param schemaPassword password
     * @param redirectPath redirect path
     * @param state status
     * @param addHeader header
     * @return response
     * @throws PersoniumException failed
     */
    public static PersoniumResponse implicitflowAuthenticatePasswordChange(String cellName,
            String schemaCell,
            String apTokenStr,
            String schemaPassword,
            String redirectPath,
            String state,
            HashMap<String, String> addHeader) throws PersoniumException {
        String clientId = UrlUtils.cellRoot(schemaCell);
        if (null == redirectPath) {
            redirectPath = "__/redirect.html";
        }

        String body = "response_type=token&client_id=" + clientId
                + "&redirect_uri=" + clientId + redirectPath
                + "&access_token=" + apTokenStr
                + "&password_change_required=true"
                + "&password=" + schemaPassword
                + "&state=" + state;

        PersoniumRestAdapter rest = new PersoniumRestAdapter();

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        if (addHeader != null) {
            requestheaders.putAll(addHeader);
        }
        requestheaders.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        return rest.post(UrlUtils.cellRoot(cellName) + "__authz", body, requestheaders);
    }

    /**
     * Box URL取得ユーティリティ.
     * @param cellName Cell名
     * @param schemaCell schemaCell名
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @return レスポンス
     * @throws PersoniumException リクエスト失敗
     */
    public static PersoniumResponse getBoxUrl(String cellName,
            String schemaCell,
            String authorization) throws PersoniumException {
        PersoniumRestAdapter rest = new PersoniumRestAdapter();

        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, authorization);

        String query = null;
        if (schemaCell != null) {
            query = UrlUtils.cellRoot(schemaCell);
        }
        return rest.get(UrlUtils.boxUrl(cellName, query), requestheaders);
    }

    /**
     * Boxインストールを実行するユーティリティ.
     * @param cellName Cell名
     * @param boxName インストール先Box名
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @return レスポンス
     */
    public static TResponse boxInstall(String cellName,
            String boxName,
            String authorization) {
        File barFile = new File("requestData/barInstall/V1_1_2_bar_minimum.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);

        Http client = Http.request("bar-install.txt")
                .with("cellPath", cellName)
                .with("path", boxName)
                .with("token", authorization)
                .with("contType", "application/zip")
                .with("contLength", String.valueOf(body.length));

        return client.setBodyBinary(body)
                .returns()
                .debug();
    }

    /**
     * ACLを設定するユーティリティ(principal: all, privilege: all).
     * @param cellName Cell名
     * @param authorization auth-schemaを含むAuthorizationヘッダの値
     * @return レスポンス
     */
    public static TResponse setAclPriviriegeAllPrincipalAll(String cellName,
            String authorization) {
        return Http.request("cell/acl-setting-all.txt")
                .with("url", cellName)
                .with("token", authorization)
                .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, ""))
                .returns()
                .debug();
    }

    /**
     * Set ACL. Single privilege.
     * @param cellName target cell
     * @param token token
     * @param boxName box name
     * @param roleName target role
     * @param privilege privilege
     * @param code expected response code
     * @return api response
     */
    public static TResponse setAclSingle(String cellName, String token, String roleName, String privilege, int code) {
        return Http.request("cell/acl-setting-single-request.txt")
                .with("url", cellName)
                .with("token", token)
                .with("role", roleName)
                .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, ""))
                .with("privilege", privilege)
                .returns().debug().statusCode(code);
    }

    /**
     * ACLを設定するユーティリティ(デフォルト).
     * @param cellName Cell名
     * @param token トークン
     * @return レスポンス
     */
    public static TResponse setAclDefault(String cellName,
            String token) {
        return Http.request("cell/acl-default.txt")
                .with("url", cellName)
                .with("token", token)
                .with("roleBaseUrl", UrlUtils.roleResource(cellName, null, ""))
                .returns()
                .debug();
    }
}

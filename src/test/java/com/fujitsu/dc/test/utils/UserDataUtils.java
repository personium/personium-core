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
package com.fujitsu.dc.test.utils;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty.ComplexTypePropertyUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Odata関連のHttpリクエストドキュメントを利用するユーティリティ.
 */
public class UserDataUtils {
    private static final int NUMBER_FOR_CREATESTRING = 10;

    private UserDataUtils() {
    }

    /**
     * ComplexTypeの作成.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param odataSvcPath Odataコレクション名
     * @param complexTypeName コンプレックスタイプ名
     * @return レスポンス
     */
    public static DcResponse createComplexType(final String cellName,
            final String boxName,
            final String odataSvcPath,
            final String complexTypeName) {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(UrlUtils.complexType(cellName, boxName, odataSvcPath, null));
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.addJsonBody("Name", complexTypeName);

        // リクエスト実行
        return AbstractCase.request(req);
    }

    /**
     * Propertyの作成.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param odataSvcPath Odataコレクション名
     * @param propName プロパティ名
     * @param entityTypeName エンティティタイプ名
     * @param type タイプ
     * @param nullable Null許可
     * @param defaultValue デフォルト値
     * @param collectionKind 配列指定
     * @param isKey 主キー設定
     * @param uniqueKey ユニークキー
     * @return レスポンス
     */
    public static DcResponse createProperty(final String cellName,
            final String boxName,
            final String odataSvcPath,
            final String propName,
            final String entityTypeName,
            final String type,
            final boolean nullable,
            final String defaultValue,
            final String collectionKind,
            final boolean isKey,
            final String uniqueKey) {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(UrlUtils.property(cellName, boxName, odataSvcPath, null, null));
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, type);
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, nullable);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, collectionKind);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, isKey);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);

        // リクエスト実行
        return AbstractCase.request(req);
    }

    /**
     * ComplexTypePropertyの作成.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param odataSvcPath Odataコレクション名
     * @param propName プロパティ名
     * @param complexTypeName コンプレックスタイプ名
     * @param type タイプ
     * @param nullable Null許可
     * @param defaultValue デフォルト値
     * @param collectionKind 配列指定
     * @return レスポンス
     */
    public static DcResponse createComplexTypeProperty(final String cellName,
            final String boxName,
            final String odataSvcPath,
            final String propName,
            final String complexTypeName,
            final String type,
            final boolean nullable,
            final String defaultValue,
            final String collectionKind) {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(UrlUtils.complexTypeProperty(cellName, boxName, odataSvcPath, null, null));
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, propName);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, type);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, nullable);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, collectionKind);

        // リクエスト実行
        return AbstractCase.request(req);
    }

    /**
     * ユーザーデータに取得を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse get(String token, int code) {
        TResponse res = Http.request("box/odatacol/get.txt").with("cell", "testcell1").with("box", "box1")
                .with("collection", "setodata").with("entityType", "Price").with("id", "auth_test")
                .with("accept", MediaType.APPLICATION_JSON).with("token", token).with("query", "").returns()
                .statusCode(code).debug();
        return res;
    }

    /**
     * ユーザーデータに取得を実行し、レスポンスコードをチェックする.
     * @param cellName Cell名
     * @param token トークン
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param entTypeName EntityType名
     * @param id ユーザID
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse get(final String cellName,
            final String token,
            final String boxName,
            final String colName,
            final String entTypeName,
            String id,
            int code) {
        return getWithQuery(cellName, token, boxName, colName, entTypeName, "", id, code);
    }

    /**
     * ユーザーデータに取得を実行し、レスポンスコードをチェックする.
     * @param cellName Cell名
     * @param token トークン
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param entTypeName EntityType名
     * @param query クエリ
     * @param id ユーザID
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse getWithQuery(final String cellName,
            final String token,
            final String boxName,
            final String colName,
            final String entTypeName,
            final String query,
            String id,
            int code) {
        TResponse res = Http.request("box/odatacol/get.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entTypeName)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("query", query)
                .returns()
                .statusCode(code).debug();
        return res;
    }

    /**
     * ユーザーデータに取得を実行し、レスポンスコードをチェックする.
     * @param cellName Cell名
     * @param token トークン(認証スキーマを含む)
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param entTypeName EntityType名
     * @param query クエリ
     * @param id ユーザID
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse getWithQueryAnyAuthSchema(final String cellName,
            final String token,
            final String boxName,
            final String colName,
            final String entTypeName,
            final String query,
            String id,
            int code) {
        TResponse res = Http.request("box/odatacol/get-anyAuthSchema.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entTypeName)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("query", query)
                .returns()
                .statusCode(code).debug();
        return res;
    }

    /**
     * ユーザーデータの一覧取得を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(String token, int code) {
        return list(token, "", code);
    }

    /**
     * ユーザーデータの一覧取得を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param query クエリ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(String token, String query, int code) {
        return list("testcell1", "box1", "setodata", "Category", query, token, code);
    }

    /**
     * ユーザーデータを一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param query クエリ
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(String cell,
            String box,
            String col,
            String entityType,
            String query,
            String token,
            int code) {
        TResponse response = Http.request("box/odatacol/list.txt").with("cell", cell).with("box", box)
                .with("collection", col).with("entityType", entityType).with("query", query)
                .with("accept", MediaType.APPLICATION_JSON).with("token", token).returns().statusCode(code).debug();
        return response;
    }

    /**
     * ユーザーデータを一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param query クエリ
     * @param token トークン(認証スキーマを含む)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listAnyAuthSchema(String cell,
            String box,
            String col,
            String entityType,
            String query,
            String token,
            int code) {
        TResponse response = Http.request("box/odatacol/list-anyAuthSchema.txt").with("cell", cell).with("box", box)
                .with("collection", col).with("entityType", entityType).with("query", query)
                .with("accept", MediaType.APPLICATION_JSON).with("token", token).returns().statusCode(code).debug();
        return response;
    }

    /**
     * ユーザーデータを一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param query クエリ
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static DcResponse listEntities(String cell,
            String box,
            String col,
            String entityType,
            String query,
            String token,
            int code) {
        String reqUrl = UrlUtils.userData(cell, box, col, entityType) + query;
        DcRequest req = DcRequest.get(reqUrl);
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        DcResponse res = AbstractCase.request(req);
        if (-1 != code) {
            assertEquals(code, res.getStatusCode());
        }
        return res;
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse create(String token, int code) {
        return create(token, code, "testcell1", "box1");
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cell セル
     * @param box ボックス
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse create(String token, int code, String cell, String box) {
        JSONObject body = new JSONObject();
        body.put("__id", "auth_test");
        TResponse res = Http.request("box/odatacol/create.txt").with("cell", cell).with("box", box)
                .with("collection", "setodata").with("entityType", "Price").with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON).with("token", "Bearer " + token)
                .with("body", body.toJSONString())
                .returns().statusCode(code).debug();
        return res;
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @return レスポンス
     */
    public static TResponse create(String token, int code, JSONObject body, String cell,
            String box, String collection, String entityType) {
        return create(token, code, body.toJSONString(), cell, box, collection, entityType);
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @return レスポンス
     */
    public static TResponse create(String token, int code, String body, String cell,
            String box, String collection, String entityType) {
        TResponse res = Http.request("box/odatacol/create.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + token)
                .with("body", body)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする(Basic認証).
     * @param accountName Basic認証で使用するAccount名
     * @param password Basic認証で使用するパスワード
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @return レスポンス
     */
    public static TResponse createWithBasic(String accountName,
            String password,
            int code,
            String body,
            String cell,
            String box,
            String collection,
            String entityType) {
        String credentials = Base64.encodeBase64String((accountName + ":" + password).getBytes());

        TResponse res = Http.request("box/odatacol/create.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Basic " + credentials)
                .with("body", body)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックした後に登録データを削除する.
     * @param token トークン
     * @param code 期待するレスポンスコード
     */
    public static void createWithDelete(String token, int code) {
        TResponse res = create(token, code);
        if (res.getStatusCode() == HttpStatus.SC_CREATED) {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, "Price",
                    "auth_test", "setodata");
        }
    }

    /**
     * ユーザーデータに更新を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse update(String token, int code) {
        JSONObject body = new JSONObject();
        body.put("__id", "auth_test");
        body.put("prop", "prop");
        TResponse res = Http.request("box/odatacol/update.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("entityType", "Price")
                .with("id", "auth_test")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", "*")
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーODataの更新を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザODataのUUID
     * @param ifMatch If-Matchヘッダー値
     * @return レスポンス
     */
    public static TResponse update(String token, int code, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String ifMatch) {
        TResponse res = Http.request("box/odatacol/update.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", ifMatch)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーODataの更新を実行し、レスポンスコードをチェックする.
     * @param token トークン(認証スキーマ付き)
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザODataのUUID
     * @param ifMatch If-Matchヘッダー値
     * @return レスポンス
     */
    public static TResponse updateAnyAuthSchema(String token, int code, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String ifMatch) {
        TResponse res = Http.request("box/odatacol/update-anyAuthSchema.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", ifMatch)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータに削除を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param entityType エンティティタイプ
     * @param id ユーザーデータID
     * @param odataName odataコレクション名
     * @return レスポンス
     */
    public static TResponse delete(String token, int code, String entityType, String id, String odataName) {
        return delete(token,
                code,
                Setup.TEST_CELL1,
                Setup.TEST_BOX1,
                odataName,
                entityType,
                id);
    }

    /**
     * ユーザーデータに削除を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName odataコレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザーデータID
     * @return レスポンス
     */
    public static TResponse delete(String token,
            int code,
            String cellName,
            String boxName,
            String colName,
            String entityType,
            String id) {
        TResponse res = Http.request("box/odatacol/delete.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityType)
                .with("id", id)
                .with("token", token)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータに削除を実行し、レスポンスコードをチェックする.
     * @param token トークン(認証スキーマを含む)
     * @param code 期待するレスポンスコード
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName odataコレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザーデータID
     * @return レスポンス
     */
    public static TResponse deleteAnyAuthSchema(String token,
            int code,
            String cellName,
            String boxName,
            String colName,
            String entityType,
            String id) {
        TResponse res = Http.request("box/odatacol/delete-anyAuthSchema.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityType)
                .with("id", id)
                .with("token", token)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータにMERGEを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse merge(String token, int code) {
        JSONObject body = new JSONObject();
        body.put("__id", "auth_test");
        body.put("prop", "prop");
        TResponse res = Http.request("box/odatacol/merge.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("entityType", "Price")
                .with("id", "auth_test")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", "*")
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーODataの部分更新を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザODataのUUID
     * @param ifMatch If-Matchヘッダー値
     * @return レスポンス
     */
    public static TResponse merge(String token, int code, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String ifMatch) {
        TResponse res = Http.request("box/odatacol/merge.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", ifMatch)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーODataの部分更新を実行し、レスポンスコードをチェックする.
     * @param token トークン(認証スキーマ付き)
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザODataのUUID
     * @param ifMatch If-Matchヘッダー値
     * @return レスポンス
     */
    public static TResponse mergeAnyAuthSchema(String token, int code, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String ifMatch) {
        TResponse res = Http.request("box/odatacol/merge-anyAuthSchema.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", ifMatch)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * $batchリクエストの送信.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param boundary バウンダリー
     * @param body $batchリクエストのボディ
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse batch(String cellName, String boxName, String colName,
            String boundary, String body, String token, int code) {
        // UserDataを$batchに複数リクエスト指定
        TResponse res = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", boundary)
                .with("token", token)
                .with("body", body)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータにOPTIONSを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path リクエストパス
     * @return レスポンス
     */
    public static TResponse options(String token, int code, String path) {
        TResponse res = Http.request("crossdomain/xhr2-preflight.txt")
                .with("path", path)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータをNP経由で作成し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNP(String token, int code) {
        JSONObject body = new JSONObject();
        body.put("__id", "npdata");
        TResponse res = Http.request("box/odatacol/createNP.txt")
                .with("cell", "testcell1")
                .with("box", "box1")
                .with("collection", "setodata")
                .with("entityType", "Price")
                .with("id", "auth_test")
                .with("navPropName", "_Sales")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();

        return res;
    }

    /**
     * ユーザーデータをNP経由で作成し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザデータの__id
     * @param navPropName NavigationProperty名("_"なし)
     * @return レスポンス
     */
    public static TResponse createViaNP(String token, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String navPropName, int code) {
        TResponse res = Http.request("box/odatacol/createNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("navPropName", "_" + navPropName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザーデータをNP経由で作成し、レスポンスコードをチェックする.
     * @param token トークン(認証スキーマ付き)
     * @param code 期待するレスポンスコード
     * @param body リクエストボディ
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityType エンティティタイプ
     * @param id ユーザデータの__id
     * @param navPropName NavigationProperty名("_"なし)
     * @return レスポンス
     */
    public static TResponse createViaNPAnyAuthSchema(String token, JSONObject body, String cell,
            String box, String collection, String entityType, String id, String navPropName, int code) {
        TResponse res = Http.request("box/odatacol/createNP-anyAuthSchema.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entityType)
                .with("id", id)
                .with("navPropName", "_" + navPropName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ユーザODataの$links登録を行う.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param srcEntityTypeName ソース側EntityType名
     * @param srcId ソース側のユーザOData ID
     * @param navPropName ターゲット側EntityType名
     * @param targetId ターゲット側のユーザOData ID
     * @param code 期待するコード
     * @return レスポンス
     */
    public static TResponse createLink(
            String token,
            String cellName,
            String boxName,
            String colName,
            String srcEntityTypeName,
            String srcId,
            String navPropName,
            String targetId,
            int code) {
        String targetUri = UrlUtils.cellRoot(cellName) + boxName + "/"
                + colName + "/" + navPropName + "('" + targetId + "')";
        return Http.request("link-userdata-userdata.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("colPath", colName)
                .with("srcPath", srcEntityTypeName + "('" + srcId + "')")
                .with("trgPath", navPropName)
                .with("token", token)
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ユーザODataの$links登録を行う.
     * @param token トークン(認証スキーマを含む)
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param srcEntityTypeName ソース側EntityType名
     * @param srcId ソース側のユーザOData ID
     * @param navPropName ターゲット側EntityType名
     * @param targetId ターゲット側のユーザOData ID
     * @param code 期待するコード
     * @return レスポンス
     */
    public static TResponse createLinkAnyAuthSchema(
            String token,
            String cellName,
            String boxName,
            String colName,
            String srcEntityTypeName,
            String srcId,
            String navPropName,
            String targetId,
            int code) {
        String targetUri = UrlUtils.cellRoot(cellName) + boxName + "/"
                + colName + "/" + navPropName + "('" + targetId + "')";
        return Http.request("link-userdata-userdata-anyAuthSchema.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("colPath", colName)
                .with("srcPath", srcEntityTypeName + "('" + srcId + "')")
                .with("trgPath", navPropName)
                .with("token", token)
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ユーザーデータの登録を実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     */
    public static void createViaNPWithDelete(String token, int code) {
        TResponse res = createViaNP(token, code);
        if (res.getStatusCode() == HttpStatus.SC_CREATED) {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_NO_CONTENT, "Sales", "npdata", "setodata");
        }
    }

    /**
     * ユーザデータのリンクの一覧を取得する.
     * @param token 認証トークン(認証スキーマを含む)
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param sourceEntity ソース側エンティティタイプ名
     * @param sourceId ソース側ユーザデータの__id
     * @param targetEntity NavigationProperty名("_"なし)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listLinkAnyAuthSchema(
            String token,
            String cellName,
            String boxName,
            String colName,
            String sourceEntity,
            String sourceId,
            String targetEntity,
            int code) {
        return Http.request("box/odatacol/list-link-anyAuthSchema.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("colPath", colName)
                .with("srcPath", sourceEntity + "('" + sourceId + "')")
                .with("trgPath", targetEntity)
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * ユーザデータのリンクの一覧を取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param sourceEntity ソース側エンティティタイプ名
     * @param sourceId ソース側ユーザデータの__id
     * @param targetEntity NavigationProperty名("_"なし)
     * @return レスポンス
     */
    public static TResponse listLink(
            String cellName,
            String boxName,
            String colName,
            String sourceEntity,
            String sourceId,
            String targetEntity) {
        return Http.request("box/odatacol/list-link.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("colPath", colName)
                .with("srcPath", sourceEntity + "('" + sourceId + "')")
                .with("trgPath", targetEntity)
                .with("token", DcCoreConfig.getMasterToken())
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
    }

    /**
     * ユーザーデータのlinkを削除し、レスポンスコードをチェックする.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcEntity ソース側エンティティタイプ
     * @param userDataId ソース側ユーザデータの__id
     * @param navPropEntity NavigationProperty名("_"なし)
     * @param navPropId ターゲット側ユーザデータの__id
     * @param code 期待するレスポンスコード
     */
    public static void deleteLinks(String cell, String box, String collection,
            String srcEntity, String userDataId, String navPropEntity, String navPropId, int code) {
        // リクエスト実行
        Http.request("box/odatacol/delete-link.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", srcEntity)
                .with("id", userDataId)
                .with("navProp", "_" + navPropEntity)
                .with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
    }

    /**
     * ユーザーデータのlinkを削除し、レスポンスコードをチェックする.
     * @param token 認証トークン(認証スキーマを含む)
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcEntity ソース側エンティティタイプ
     * @param userDataId ソース側ユーザデータの__id
     * @param navPropEntity NavigationProperty名("_"なし)
     * @param navPropId ターゲット側ユーザデータの__id
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse deleteLinksAnyAuthSchema(String token, String cell, String box, String collection,
            String srcEntity, String userDataId, String navPropEntity, String navPropId, int code) {
        // リクエスト実行
        return Http.request("box/odatacol/delete-link-anyAuthSchema.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", srcEntity)
                .with("id", userDataId)
                .with("navProp", "_" + navPropEntity)
                .with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
    }

    /**
     * ユーザODataNP経由一覧取得.
     * @param cell Cell名
     * @param box Box名
     * @param collection コレクション名
     * @param srcEntity srcEntity
     * @param srcId srcId
     * @param navPropEntity navPropEntity
     * @param query クエリパラメータ(指定しない場合は空文字を設定)
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaNP(String cell, String box, String collection,
            String srcEntity, String srcId, String navPropEntity, String query, int code) {
        // ユーザデータの一覧取得
        return Http.request("box/odatacol/list.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection + "/" + srcEntity + "('" + srcId + "')")
                .with("entityType", "_" + navPropEntity)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * ユーザODataNP経由一覧取得.
     * @param token 認証トークン（認証スキーマ付き）
     * @param cell Cell名
     * @param box Box名
     * @param collection コレクション名
     * @param srcEntity srcEntity
     * @param srcId srcId
     * @param navPropEntity navPropEntity
     * @param query クエリパラメータ(指定しない場合は空文字を設定)
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaNPAnyAuthSchema(String token, String cell, String box, String collection,
            String srcEntity, String srcId, String navPropEntity, String query, int code) {
        // ユーザデータの一覧取得
        return Http.request("box/odatacol/list-anyAuthSchema.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection + "/" + srcEntity + "('" + srcId + "')")
                .with("entityType", "_" + navPropEntity)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * 引数で指定した長さの文字列を作成する。
     * @param length 生成する文字列の長さ
     * @return 生成した文字列
     */
    public static String createString(int length) {
        String str = "";

        for (int i = 0; i < length; i++) {
            str += (i % NUMBER_FOR_CREATESTRING);
        }
        return str;
    }

}

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

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;

import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.test.setup.Setup;

/**
 * Odata関連のHttpリクエストドキュメントを利用するユーティリティ.
 */
public class EntityTypeUtils {

    /** 名前空間. */
    public static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + EntityType.EDM_TYPE_NAME;

    private EntityTypeUtils() {
    }

    /**
     * entityTypeの取得.
     * @param cellName Cell名
     * @param token トークン
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param entTypeName EntityType名
     * @param sc 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse get(final String cellName,
            final String token,
            final String boxName,
            final String colName,
            final String entTypeName,
            int sc) {
        TResponse res = Http.request("box/entitySet-get.txt").with("cellPath", cellName).with("boxPath", boxName)
                .with("odataSvcPath", colName).with("token", token).with("accept", "application/json")
                .with("Name", entTypeName).returns().statusCode(sc);
        return res;
    }

    /**
     * entityTypeの一覧取得.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            final String token,
            final String cellName,
            final String boxName,
            final String colName,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", "\\$metadata/EntityType")
                .with("query", "")
                .with("token", token)
                .with("accept", "application/json")
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * entityTypeの一覧取得.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param query 検索クエリ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            final String token,
            final String cellName,
            final String boxName,
            final String colName,
            final String query,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", "\\$metadata/EntityType")
                .with("query", query)
                .with("token", token)
                .with("accept", "application/json")
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * EntityTypeのPOST.
     * @param cellName セル名
     * @param token トークン
     * @param odataSvcPath Odataserviceコレクション名
     * @param name EntityType名
     * @param code 期待するレスポンスコード
     * @return レスポンスコート
     */
    public static TResponse create(final String cellName, final String token,
            final String odataSvcPath, final String name, final int code) {
        TResponse tresponse = Http.request("box/entitySet-post.txt")
                .with("cellPath", cellName)
                .with("boxPath", Setup.TEST_BOX1)
                .with("odataSvcPath", odataSvcPath)
                .with("token", "Bearer " + token)
                .with("accept", "application/xml")
                .with("Name", name)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * EntityTypeのPOST.
     * @param cellName セル名
     * @param token トークン
     * @param boxName box名
     * @param odataSvcPath Odataserviceコレクション名
     * @param name EntityType名
     * @param code 期待するレスポンスコード
     * @return レスポンスコート
     */
    public static TResponse create(final String cellName, final String token,
            final String boxName, final String odataSvcPath, final String name, final int code) {
        TResponse tresponse = Http.request("box/entitySet-post.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("token", "Bearer " + token)
                .with("accept", "application/xml")
                .with("Name", name)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * EntityTypeのPOST(Basic認証).
     * @param cellName セル名
     * @param accountName Basic認証で使用するAccount名
     * @param password Basic認証で使用するパスワード
     * @param boxName box名
     * @param odataSvcPath Odataserviceコレクション名
     * @param name EntityType名
     * @param code 期待するレスポンスコード
     * @return レスポンスコート
     */
    public static TResponse createWithBasic(final String cellName,
            final String accountName,
            final String password,
            final String boxName,
            final String odataSvcPath,
            final String name,
            final int code) {
        String credentials = Base64.encodeBase64String((accountName + ":" + password).getBytes());

        TResponse tresponse = Http.request("box/entitySet-post.txt")
                .with("cellPath", cellName)
                .with("boxPath", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("token", "Basic " + credentials)
                .with("accept", "application/xml")
                .with("Name", name)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * EntityTypeの更新を行う.
     * @param token トークン
     * @param cellName Cell名
     * @param boxNmae Box名
     * @param colName Collection名
     * @param entityTypeName 更新前EntityType名
     * @param body リクエストボディ
     * @param code 期待するステータスコード
     * @return レスポンス
     */
    public static TResponse update(
            final String token,
            final String cellName,
            final String boxNmae,
            final String colName,
            final String entityTypeName,
            final String body,
            final int code) {
        return Http.request("box/odatacol/update.txt")
                .with("token", token)
                .with("cell", cellName)
                .with("box", boxNmae)
                .with("collection", colName + "/\\$metadata")
                .with("entityType", "EntityType")
                .with("id", entityTypeName)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", "*")
                .with("body", body)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * EntityTypeの部分更新を行う.
     * @param token トークン
     * @param cellName Cell名
     * @param boxNmae Box名
     * @param colName Collection名
     * @param entityTypeName 更新前EntityType名
     * @param body リクエストボディ
     * @param code 期待するステータスコード
     * @return レスポンス
     */
    public static TResponse merge(
            final String token,
            final String cellName,
            final String boxNmae,
            final String colName,
            final String entityTypeName,
            final String body,
            final int code) {
        return Http.request("box/odatacol/merge.txt")
                .with("token", token)
                .with("cell", cellName)
                .with("box", boxNmae)
                .with("collection", colName + "/\\$metadata")
                .with("entityType", "EntityType")
                .with("id", entityTypeName)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", "*")
                .with("body", body)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * entityTypeの削除.
     * @param odataName ODataコレクション名
     * @param token トークン
     * @param accept acceptヘッダー
     * @param entSetName entityType名
     * @param cellPath セル名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse delete(final String odataName, final String token,
            final String accept, final String entSetName, final String cellPath, final int code) {
        TResponse tresponse = Http.request("box/entitySet-delete.txt")
                .with("cellPath", cellPath)
                .with("boxPath", Setup.TEST_BOX1)
                .with("odataSvcPath", odataName)
                .with("token", token)
                .with("accept", accept)
                .with("Name", entSetName)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * entityTypeの削除.
     * @param odataName ODataコレクション名
     * @param token トークン
     * @param accept acceptヘッダー
     * @param entSetName entityType名
     * @param boxName box名
     * @param cellPath セル名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse delete(final String odataName, final String token,
            final String accept, final String entSetName, final String boxName, final String cellPath, final int code) {
        TResponse tresponse = Http.request("box/entitySet-delete.txt")
                .with("cellPath", cellPath)
                .with("boxPath", boxName)
                .with("odataSvcPath", odataName)
                .with("token", token)
                .with("accept", accept)
                .with("Name", entSetName)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * EntityTypeからのNP経由でAssociationEndの一覧を取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entitySetName AssociationEnd名
     * @param id entityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaEntityTypeNP(
            String token, String cell, String box, String collection, String entitySetName, String id, int code) {
        TResponse res = Http.request("box/odatacol/schema/listViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entitySetName)
                .with("id", id)
                .with("navPropName", "_EntityType")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }
}

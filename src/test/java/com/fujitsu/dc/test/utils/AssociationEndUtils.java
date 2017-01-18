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

import org.apache.http.HttpStatus;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Odata関連のHttpリクエストドキュメントを利用するユーティリティ.
 */
public class AssociationEndUtils {
    private AssociationEndUtils() {
    }

    /**
     * AssociationEndを作成する.
     * @param token トークン
     * @param multiplicity 多重度
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param code 期待するレスポンスコード
     * @param associationEndName associationEnd名
     * @param entityTypeName entityType名
     * @return レスポンス
     */
    public static TResponse create(String token, String multiplicity, String cell,
            String box, String collection, int code,
            String associationEndName, String entityTypeName) {
        TResponse res = Http.request("box/odatacol/schema/assocend/create.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("name", associationEndName)
                .with("multiplicity", multiplicity)
                .with("entityTypeName", entityTypeName)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * AssociationEndの一件取得.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param associationEndName AssociationEnd名
     * @param entityTypeName EntityType名
     * @param sc 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse get(
            final String token,
            final String cellName,
            final String boxName,
            final String colName,
            final String associationEndName,
            final String entityTypeName,
            int sc) {
        return Http.request("box/odatacol/get-without-singlequote.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/\\$metadata")
                .with("entityType", "AssociationEnd")
                .with("id", getAssociationEndKey(associationEndName, entityTypeName))
                .with("query", "")
                .returns()
                .debug()
                .statusCode(sc);
    }

    /**
     * AssociationEndのキーを作成する.
     * @param associationEndName AssociationEnd名
     * @param entityTypeName EntityType名
     * @return キー文字列
     */
    public static String getAssociationEndKey(final String associationEndName, final String entityTypeName) {
        if (entityTypeName == null) {
            return String.format("Name='%s',_EntityType.Name=null", associationEndName);
        }
        return String.format("Name='%s',_EntityType.Name='%s'", associationEndName, entityTypeName);
    }

    /**
     * AssociationEndの一覧取得.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param sc 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            final String token,
            final String cellName,
            final String boxName,
            final String colName,
            int sc) {
        return Http.request("box/odatacol/list.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/\\$metadata")
                .with("entityType", "AssociationEnd")
                .with("query", "")
                .returns()
                .debug()
                .statusCode(sc);
    }

    /**
     * AssociationEndをAssociationEndからのNP経由で登録する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param sourceAssociationEndName ソース側のAssociationのName
     * @param sourceEntityTypeName ソース側のAssociationのEntityType名
     * @param associationEndName associationEnd名
     * @param multiplicity 多重度
     * @param entityTypeName entityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse createViaNP(
            String token, String cell, String box, String collection,
            String sourceAssociationEndName, String sourceEntityTypeName,
            String associationEndName, String multiplicity, String entityTypeName,
            int code) {
        TResponse res = Http.request("box/odatacol/schema/assocend/createViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("sourceAssociationEndName", sourceAssociationEndName)
                .with("sourceEntityTypeName", sourceEntityTypeName)
                .with("name", associationEndName)
                .with("multiplicity", multiplicity)
                .with("entityTypeName", entityTypeName)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * AssociationEndをEntityTypeからのNP経由で登録する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param associationEndName associationEnd名
     * @param multiplicity 多重度
     * @param entityTypeName entityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse createViaEntityTypeNP(
            String token, String cell, String box, String collection,
            String associationEndName, String multiplicity, String entityTypeName,
            int code) {
        TResponse res = Http.request("box/odatacol/schema/assocend/createViaEntityTypeNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("name", associationEndName)
                .with("multiplicity", multiplicity)
                .with("entityTypeName", entityTypeName)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * AssociationEndの更新.
     * @param token トークン
     * @param cell セル名
     * @param odataSvcPath Odataコレクション名
     * @param entityTypeName エンティティタイプ名
     * @param boxName ボックス名
     * @param name AssociationEnd名
     * @param reName 変更後AssociationEnd名
     * @param multiplicity 多重度
     * @param entityTypeReName エンティティタイプ名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse update(String token, final String cell,
            final String odataSvcPath, final String entityTypeName, final String boxName, final String name,
            final String reName, final String multiplicity, final String entityTypeReName, final int code) {
        TResponse tresponse = Http.request("box/associationEnd-put.txt")
                .with("cell", cell)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("entityTypeName", entityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .with("reName", reName)
                .with("multiplicity", multiplicity)
                .with("entityTypeReName", entityTypeReName)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndの更新(body指定).
     * @param token トークン
     * @param cell セル名
     * @param odataSvcPath Odataコレクション名
     * @param entityTypeName エンティティタイプ名
     * @param boxName ボックス名
     * @param name AssociationEnd名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse update(String token, final String cell,
            final String odataSvcPath, final String entityTypeName, final String boxName, final String name,
            final String body, final int code) {
        TResponse tresponse = Http.request("box/associationEnd-putWithBody.txt")
                .with("cell", cell)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("entityTypeName", entityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .with("body", body)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndの削除.
     * @param token トークン
     * @param cellName セル名
     * @param odataSvcPath Odataコレクション名
     * @param entityTypeName エンティティタイプ名
     * @param boxName ボックス名
     * @param name AssociationEnd名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse delete(String token,
            final String cellName,
            final String odataSvcPath,
            final String entityTypeName,
            final String boxName,
            final String name,
            final int code) {
        TResponse tresponse = Http.request("box/associationEnd-delete.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("entityTypeName", entityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndの$links作成.
     * @param token トークン
     * @param cellName セル名
     * @param boxName ボックス名
     * @param odataSvcPath Odataコレクション名
     * @param entityTypeName entityTypeName名
     * @param linkEntityTypeName linkEntityTypeName名
     * @param name AssociationEnd名
     * @param linkName LinkAssociationEnd名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse createLink(String token,
            String cellName,
            String boxName,
            String odataSvcPath,
            String entityTypeName,
            String linkEntityTypeName,
            String name,
            String linkName, int code) {

        return Http.request("box/associationEnd-createLink.txt")
                .with("baseUrl", UrlUtils.cellRoot(cellName))
                .with("cell", cellName)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("entityTypeName", entityTypeName)
                .with("linkEntityTypeName", linkEntityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .with("linkName", linkName)
                .returns()
                .statusCode(code);
    }

    /**
     * AssociationEndの$links削除.
     * @param cellName セル名
     * @param odataSvcPath Odataコレクション名
     * @param boxName ボックス名
     * @param key AssociationEnd名
     * @param navKey Navigationproperty側AssociationEnd名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse deleteLink(final String cellName, final String odataSvcPath,
            final String boxName, final String key, final String navKey, final int code) {
        TResponse tresponse = Http.request("box/associationEnd-deleteLink.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("key", key)
                .with("navKey", navKey)
                .returns()
                .debug()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndの$links削除.
     * @param token トークン
     * @param cellName セル名
     * @param boxName ボックス名
     * @param odataSvcPath Odataコレクション名
     * @param key AssociationEnd名
     * @param navKey Navigationproperty側AssociationEnd名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse deleteLinkWithToken(
            final String token,
            final String cellName,
            final String boxName,
            final String odataSvcPath,
            final String key,
            final String navKey,
            final int code) {
        TResponse tresponse = Http.request("box/associationEnd-deleteLink.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("odataSvcPath", odataSvcPath)
                .with("token", token)
                .with("key", key)
                .with("navKey", navKey)
                .returns()
                .debug()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AssociationEndを$links経由で一覧取得する.
     * @param token アクセストークン
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityTypeName EntityType名
     * @param name AssociationEnd名
     * @param code チェック用ステータスコード
     * @return レスポンス
     */
    public static TResponse getAssociationEndLinkList(String token, String cell, String box, String col,
            String entityTypeName, String name, int code) {
        TResponse response = Http.request("box/associationEnd-listLink.txt")
                .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                .with("cell", cell)
                .with("box", box)
                .with("odataSvcPath", col)
                .with("entityTypeName", entityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * AssociationEndを$links経由で一覧取得する.
     * @param token アクセストークン
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityTypeName EntityType名
     * @param name AssociationEnd名
     * @param code チェック用ステータスコード
     * @return レスポンス
     */
    public static TResponse getAssociationEndNpLinkList(String token, String cell, String box, String col,
            String entityTypeName, String name, int code) {
        TResponse response = Http.request("box/associationEnd-listNpLink.txt")
                .with("baseUrl", UrlUtils.cellRoot(Setup.TEST_CELL1))
                .with("cell", cell)
                .with("box", box)
                .with("odataSvcPath", col)
                .with("entityTypeName", entityTypeName)
                .with("token", token)
                .with("accept", "application/json")
                .with("name", name)
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
        return response;
    }

    /**
     * EntityTypeからのNP経由でAssociationEndの一覧を取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entitySetName EntityType名
     * @param id entityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaAssociationEndNP(
            String token, String cell, String box, String collection, String entitySetName, String id, int code) {
        TResponse res = Http.request("box/odatacol/schema/listViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("entityType", entitySetName)
                .with("id", id)
                .with("navPropName", "_AssociationEnd")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

}

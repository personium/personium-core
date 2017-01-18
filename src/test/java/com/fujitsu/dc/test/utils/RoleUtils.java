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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class RoleUtils {
    private RoleUtils() {
    }

    /**
     * Roleの複合キーの文字列を作成する.
     * @param roleName Role名
     * @return 複合キー
     */
    public static String keyString(String roleName) {
        return "Name='" + roleName + "'";
    }

    /**
     * Roleの複合キーの文字列を作成する.
     * @param roleName Role名
     * @param boxName 紐付くBox名
     * @return 複合キー
     */
    public static String keyString(String roleName, String boxName) {
        return "Name='" + roleName + "',_Box.Name='" + boxName + "'";
    }

    /**
     * ロール取得ユーティリティ.
     * @param cellName セル名
     * @param token 認証トークン
     * @param roleName ロール名
     * @param boxName ボックス名
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse get(String cellName, String token, String roleName,
            String boxName, int sc) {
        TResponse res = Http.request("role-retrieve.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rolename", roleName)
                .with("boxname", "'" + boxName + "'")
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * ロールリスト取得ユーティリティ.
     * @param token 認証トークン
     * @param cellName セル名
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse list(String token, String cellName, int sc) {
        TResponse res = Http.request("role-list.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * ロールリスト取得ユーティリティ.
     * @param token 認証トークン
     * @param cellName セル名
     * @param query クエリ
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse list(String token, String cellName, String query, int sc) {
        TResponse res = Http.request("role-list-with-query.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("query", query)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * ロールの$links一覧を取得するユーティリティ.
     * @param cellName セル名
     * @param sourceEntityType ソース側エンティティタイプ名
     * @param sourceEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listLinkWithAuthSchema(
            final String cellName,
            final String authorization,
            final String sourceEntityType,
            final String sourceEntityKeyString,
            final int code) {

        String key = sourceEntityKeyString;
        if (key != null && !key.contains("'")) {
            key = "'" + sourceEntityKeyString + "'";
        }
        return Http.request("links-request-anyAuthSchema.txt")
                .with("method", HttpMethod.GET)
                .with("authorization", authorization)
                .with("cellPath", cellName)
                .with("entitySet", sourceEntityType)
                .with("key", key)
                .with("navProp", "_Role")
                .returns()
                .statusCode(code);
    }

    /**
     * ロールNP経由一覧取得ユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param sourceEntityType ソース側エンティティタイプ名
     * @param sourceEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @return レスポンス
     */
    public static TResponse listViaNP(
            final String cellName,
            final String token,
            final String sourceEntityType,
            final String sourceEntityKeyString) {
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
     * ロールをNP経由で一覧取得するユーティリティ.
     * @param cellName セル名
     * @param sourceEntityType ソース側エンティティタイプ名
     * @param sourceEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaNPWithAuthSchema(
            final String cellName,
            final String authorization,
            final String sourceEntityType,
            final String sourceEntityKeyString,
            final int code) {

        String key = sourceEntityKeyString;
        if (key != null && !key.contains("'")) {
            key = "'" + sourceEntityKeyString + "'";
        }
        return Http.request("cell/listViaNP.txt")
                .with("token", authorization)
                .with("cell", cellName)
                .with("entityType", sourceEntityType)
                .with("id", key)
                .with("navPropName", "_Role")
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(code);
    }

    /**
     * Roleを作成するユーティリティ(Box指定なし).
     * @param cellName セル名
     * @param token トークン
     * @param roleName ロール名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse create(final String cellName, final String token,
            final String roleName, final int code) {
        return create(cellName, token, null, roleName, code);
    }

    /**
     * Roleを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param roleName ロール名
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse create(final String cellName, final String token,
            final String boxName, final String roleName, final int code) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);
        if (boxName != null) {
            body.put("_Box.Name", boxName);
        }

        return Http.request("role-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
    }

    /**
     * NP経由でRoleを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param srcEntityName ソース側エンティティタイプ名
     * @param srcEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param roleName ロール名
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNP(
            final String cellName,
            final String token,
            final String srcEntityName,
            final String srcEntityKeyString,
            final String roleName,
            final int code) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);

        return Http.request("cell/createNPWithoutQuote.txt")
                .with("method", HttpMethod.POST)
                .with("token", "Bearer " + token)
                .with("cell", cellName)
                .with("entityType", srcEntityName)
                .with("id", srcEntityKeyString)
                .with("navPropName", "_Role")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code);
    }

    /**
     * NP経由でRoleを作成するユーティリティ.
     * @param cellName セル名
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param srcEntityName ソース側エンティティタイプ名
     * @param srcEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param roleName ロール名
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNPWithAuthSchema(
            final String cellName,
            final String authorization,
            final String srcEntityName,
            final String srcEntityKeyString,
            final String roleName,
            final int code) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);

        String key = srcEntityKeyString;
        if (srcEntityKeyString != null && !srcEntityKeyString.contains("'")) {
            key = "'" + srcEntityKeyString + "'";
        }

        return Http.request("cell/createNPWithoutQuote.txt")
                .with("method", HttpMethod.POST)
                .with("token", authorization)
                .with("cell", cellName)
                .with("entityType", srcEntityName)
                .with("id", key)
                .with("navPropName", "_Role")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code);
    }

    /**
     * NP経由でRoleを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param roleKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param targetEntityName ターゲット側エンティティタイプ名
     * @param targetEntityKeyString ターゲット側エンティティキー文字列（例："Name='xxx'"）
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse createLink(
            final String cellName,
            final String token,
            final String roleKeyString,
            final String targetEntityName,
            final String targetEntityKeyString,
            final int code) {

        return Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("token", token)
                .with("cellPath", cellName)
                .with("entitySet", "Role")
                .with("key", roleKeyString)
                .with("navProp", "_" + targetEntityName)
                .with("uri", UrlUtils.cellCtlWithoutSingleQuote(cellName, targetEntityName, targetEntityKeyString))
                .returns()
                .statusCode(code);
    }

    /**
     * NP経由でRoleを作成するユーティリティ.
     * @param cellName セル名
     * @param roleKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param targetEntityName ターゲット側エンティティタイプ名
     * @param targetEntityKeyString ターゲット側エンティティキー文字列（例："Name='xxx'"）
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse createLinkWithAuthSchema(
            final String cellName,
            final String roleKeyString,
            final String targetEntityName,
            final String targetEntityKeyString,
            final String authorization,
            final int code) {

        String srcKey = roleKeyString;
        if (roleKeyString != null && !roleKeyString.contains("'")) {
            srcKey = "'" + srcKey + "'";
        }

        String targetKey = targetEntityKeyString;
        if (targetEntityKeyString != null && !targetEntityKeyString.contains("'")) {
            targetKey = "'" + targetEntityKeyString + "'";
        }

        return Http.request("links-request-with-body-anyAuthSchema.txt")
                .with("method", "POST")
                .with("token", authorization)
                .with("cellPath", cellName)
                .with("entitySet", "Role")
                .with("key", srcKey)
                .with("navProp", "_" + targetEntityName)
                .with("uri", UrlUtils.cellCtlWithoutSingleQuote(cellName, targetEntityName, targetKey))
                .returns()
                .statusCode(code);
    }

    /**
     * ロール更新ユーティリティ.
     * @param token 認証トークン
     * @param cellName セル名
     * @param roleName ロール名
     * @param newRoleName 新ロール名
     * @param boxName ボックス名
     * @param sc ステータスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse update(String token, String cellName, String roleName,
            String newRoleName, String boxName, int sc) {
        JSONObject body = new JSONObject();
        body.put("Name", newRoleName);
        String boxNameStr = null;
        if (boxName != null) {
            body.put("_Box.Name", boxName);
            boxNameStr = "'" + boxName + "'";
        } else {
            boxNameStr = "null";
        }
        TResponse res = Http.request("cell/role-update.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("rolename", roleName)
                .with("boxname", boxNameStr)
                .with("body", body.toString())
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * ロールを削除するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param roleName ロール名
     */
    public static void delete(final String cellName, final String token,
            final String boxName, final String roleName) {
        String keyBoxName = null;
        if (boxName == null) {
            keyBoxName = "null";
        } else {
            keyBoxName = "'" + boxName + "'";
        }
        Http.request("role-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("rolename", roleName)
                .with("boxname", keyBoxName)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ロールを削除するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param roleName ロール名
     * @param code レスポンスコード
     */
    public static void delete(final String cellName, final String token,
            final String boxName, final String roleName, final int code) {
        String keyBoxName = null;
        if (boxName == null) {
            keyBoxName = "null";
        } else {
            keyBoxName = "'" + boxName + "'";
        }

        Http.request("role-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("rolename", roleName)
                .with("boxname", keyBoxName)
                .returns()
                .statusCode(code);
    }

    /**
     * ロールリンク削除.
     * @param cellName セル名
     * @param token トークン
     * @param roleKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param targetEntityName ターゲット側エンティティタイプ名
     * @param targetEntityKey ターゲット側エンティティキー文字列（例："Name='xxx'"）
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse deleteLink(
            final String cellName,
            final String token,
            final String roleKeyString,
            final String targetEntityName,
            final String targetEntityKey,
            final int code) {
        return Http.request("cell/link-delete.txt")
                .with("token", "Bearer " + token)
                .with("cellPath", cellName)
                .with("sourceEntity", "Role")
                .with("sourceKey", roleKeyString)
                .with("navPropName", "_" + targetEntityName)
                .with("navPropKey", targetEntityKey)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
    }

    /**
     * ロールリンク削除.
     * @param cellName セル名
     * @param roleKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param targetEntityName ターゲット側エンティティタイプ名
     * @param targetEntityKey ターゲット側エンティティキー文字列（例："Name='xxx'"）
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse deleteLinkWithAuthSchema(
            final String cellName,
            final String roleKeyString,
            final String targetEntityName,
            final String targetEntityKey,
            final String authorization,
            final int code) {

        String srcKey = roleKeyString;
        if (srcKey != null && !srcKey.contains("'")) {
            srcKey = "'" + srcKey + "'";
        }

        String targetKey = targetEntityKey;
        if (targetKey != null && !targetKey.contains("'")) {
            targetKey = "'" + targetKey + "'";
        }

        return Http.request("cell/link-delete.txt")
                .with("token", authorization)
                .with("cellPath", cellName)
                .with("sourceEntity", "Role")
                .with("sourceKey", srcKey)
                .with("navPropName", "_" + targetEntityName)
                .with("navPropKey", targetKey)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
    }
}

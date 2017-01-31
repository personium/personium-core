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

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;

import io.personium.test.jersey.AbstractCase;
import io.personium.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class AccountUtils {
    private AccountUtils() {
    }

    /**
     * AccountのGETを行うユーティリティ.
     * @param token 認証トークン
     * @param code レスポンスコード
     * @param cellName セル
     * @param userName ユーザ名
     * @return レスポンス
     */
    public static TResponse get(String token, int code, String cellName, String userName) {
        TResponse response = Http.request("account-retrieve.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .returns()
                .statusCode(code)
                .contentType("application/json");
        return response;
    }

    /**
     * アカウントを作成するユーティリティ.
     * @param token トークン
     * @param cellName セル名
     * @param userName ユーザ名
     * @param pass パスワード
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse create(final String token, final String cellName,
            final String userName, final String pass, int code) {
        // AccountのC
        TResponse tresponse = Http.request("account-create.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", pass)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * アカウントを作成するユーティリティ.
     * @param token トークン
     * @param cellName セル名
     * @param userName ユーザ名
     * @param pass パスワード
     * @param lastAuthenticated 最終ログイン時刻(Edm.DateTime)
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse create(final String token, final String cellName,
            final String userName, final String pass, final String lastAuthenticated, int code) {
        // AccountのC
        TResponse tresponse = Http.request("account-create-lastauthenticated.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", pass)
                .with("lastauthenticated", lastAuthenticated)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * X-Dc-Credentialヘッダー有でTypeを指定してアカウントを作成するユーティリティ.
     * @param token トークン
     * @param typeName Type値
     * @param cellName セル名
     * @param userName ユーザ名
     * @param pass パスワード
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse createWithType(final String token, final String cellName, final String typeName,
            final String userName, final String pass, int code) {
        // AccountのC
        TResponse tresponse = Http.request("account-create-with-type.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", pass)
                .with("accountType", typeName)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * X-Dc-Credentialヘッダー無しでアカウントを作成するユーティリティ.
     * @param token トークン
     * @param cellName セル名
     * @param userName ユーザ名
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse createNonCredential(final String token, final String cellName,
            final String userName, int code) {
        // AccountのC
        TResponse tresponse = Http.request("account-create-Non-Credential.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * X-Dc-Credentialヘッダー無しでTypeを指定してアカウントを作成するユーティリティ.
     * @param token トークン
     * @param cellName セル名
     * @param userName ユーザ名
     * @param type Type値
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse createNonCredentialWithType(final String token, final String cellName,
            final String userName, final String type, int code) {
        // AccountのC
        TResponse tresponse = Http.request("account-create-Non-Credential-with-type.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("accountType", type)
                .returns()
                .statusCode(code);
        return tresponse;
    }

    /**
     * AccountとRoleの$links登録.
     * @param token トークン
     * @param cellName セル名
     * @param boxName ボックス名
     * @param userName ユーザ名
     * @param roleName ロール名
     * @param code ステータスコード
     * @return レスポンス
     */
    public static TResponse createLinkWithRole(final String token, final String cellName, final String boxName,
            final String userName, final String roleName, int code) {
        // アカウント・ロールの$link
        return Http.request("link-account-role.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("roleUrl", UrlUtils.roleUrl(cellName, boxName, roleName))
                .returns()
                .statusCode(code)
                .debug();
    }

    /**
     * NP経由でAccountを作成するユーティリティ.
     * @param cellName Cell名
     * @param token 認証トークン(Bearerなし)
     * @param srcEntityName NP経由元のエンティティ名
     * @param srcEntityKeyString NP経由元のID
     * @param body リクエストボディ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse createViaNPNonCredential(
            final String cellName,
            final String token,
            final String srcEntityName,
            final String srcEntityKeyString,
            final String body,
            final int code) {
        return Http.request("cell/createNP.txt")
                .with("method", HttpMethod.POST)
                .with("token", token)
                .with("cell", cellName)
                .with("entityType", srcEntityName)
                .with("id", srcEntityKeyString)
                .with("navPropName", "_Account")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("body", body)
                .returns()
                .statusCode(code);
    }

    /**
     * アカウント更新.
     * @param token 認証トークン
     * @param cellName セル名
     * @param userName 旧ユーザ名
     * @param newUsername アカウント名
     * @param newPassword パスワード
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse update(String token, String cellName, String userName, String newUsername,
            String newPassword, int sc) {
        TResponse res = Http.request("account-update.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("newUsername", newUsername)
                .returns().debug();
        res.statusCode(sc);
        return res;
    }

    /**
     * アカウント更新(LastAuthenticated指定あり).
     * @param token 認証トークン
     * @param cellName セル名
     * @param userName 旧ユーザ名
     * @param newUsername アカウント名
     * @param newPassword パスワード
     * @param newLastAuthenticated 最終ログイン時刻
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse update(String token, String cellName, String userName, String newUsername,
            String newPassword, String newLastAuthenticated, int sc) {
        if (null != newLastAuthenticated) {
            // nullでない場合は文字列であるため、ダブルクォーテーションで囲う
            newLastAuthenticated = "\"" + newLastAuthenticated + "\"";
        } else {
            newLastAuthenticated = "null";
        }
        TResponse res = Http.request("account-update-lastauthenticated.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("newUsername", newUsername)
                .with("newLastAuthenticated", newLastAuthenticated)
                .returns().debug();
        res.statusCode(sc);
        return res;
    }

    /**
     * アカウントを削除するユーティリティー.
     * @param cellName セル名
     * @param token トークン
     * @param userName ユーザ名
     * @param code レスポンスコード
     */
    public static void delete(final String cellName, final String token, final String userName, final int code) {
        // アカウント削除
        TResponse res = Http.request("account-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .returns();
        if (code != -1) {
            assertEquals(code, res.getStatusCode());
        }
    }

    /**
     * AccountとRoleの$links削除.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param token トークン
     * @param userName ユーザ名
     * @param roleName ロール名
     * @param code レスポンスコード
     */
    public static void deleteLinksWithRole(final String cellName,
            final String boxName,
            final String token,
            final String userName,
            final String roleName,
            final int code) {
        String roleKey;
        if (boxName == null) {
            roleKey = "Name='" + roleName + "',_Box.Name=null";
        } else {
            roleKey = "Name='" + roleName + "',_Box.Name='" + boxName + "'";
        }
        // Account-Role1登録$links削除
        Http.request("cell/link-delete-account-role.txt")
                .with("cellPath", cellName)
                .with("accountKey", userName)
                .with("roleKey", roleKey)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", token)
                .with("ifMatch", "*")
                .returns()
                .statusCode(code);
    }

    /**
     * アカウントリスト取得ユーティリティ(クエリ有).
     * @param token 認証トークン
     * @param cellName セル名
     * @param query クエリ
     * @param sc ステータスコード
     * @return レスポンス
     */
    public static TResponse list(String token, String cellName, String query, int sc) {
        TResponse res = Http.request("account-list-with-query.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("query", query)
                .returns()
                .statusCode(sc);
        return res;
    }

    /**
     * アカウントNP経由一覧取得ユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param sourceEntityType ソース側エンティティタイプ名
     * @param sourceEntityKeyString ソース側エンティティキー文字列（例："Name='xxx'"）
     * @param query クエリ("?$"から指定すること)
     * @return レスポンス
     */
    public static TResponse listViaNP(
            final String cellName,
            final String token,
            final String sourceEntityType,
            final String sourceEntityKeyString,
            final String query) {
        String navPropName = "_Account";
        if (null != query) {
            navPropName += query;
        }
        return Http.request("cell/listViaNP.txt")
                .with("token", "Bearer " + token)
                .with("cell", cellName)
                .with("entityType", sourceEntityType)
                .with("id", sourceEntityKeyString)
                .with("navPropName", navPropName)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(HttpStatus.SC_OK);
    }

}

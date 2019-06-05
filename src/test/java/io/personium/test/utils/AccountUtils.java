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
import org.json.simple.JSONObject;

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
     * Utility to create account with IPAddressRenge.
     * @param token token
     * @param cellName cell name
     * @param userName user name
     * @param pass password
     * @param ipAddressRange IP address range
     * @param code Expected response code
     * @return responese
     */
    public static TResponse createWithIPAddressRange(final String token, final String cellName, final String userName,
            final String pass, String ipAddressRange, int code) {
        TResponse tresponse = Http.request("account-create-with-IPAddressRange.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", pass)
                .with("IPAddressRange", ipAddressRange)
                .returns()
                .debug();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * Utility to create account with status.
     * @param token token
     * @param cellName cell name
     * @param userName user name
     * @param pass password
     * @param status status
     * @param code Expected response code
     * @return responese
     */
    public static TResponse createWithStatus(final String token, final String cellName, final String userName,
            final String pass, String status, int code) {
        TResponse tresponse = Http.request("account-create-with-status.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", pass)
                .with("status", status)
                .returns()
                .debug();
        tresponse.statusCode(code);
        return tresponse;
    }

    /**
     * X-Personium-Credentialヘッダー有でTypeを指定してアカウントを作成するユーティリティ.
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
     * X-Personium-Credentialヘッダー無しでアカウントを作成するユーティリティ.
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
     * X-Personium-Credentialヘッダー無しでTypeを指定してアカウントを作成するユーティリティ.
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
     * アカウント更新 IPアドレス範囲付き.
     * @param token 認証トークン
     * @param cellName セル名
     * @param userName 旧ユーザ名
     * @param newUsername アカウント名
     * @param newPassword パスワード
     * @param newIPAddressRange IPアドレス範囲
     * @param sc ステータスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse updateWithIPAddressRange(String token, String cellName, String userName, String newUsername,
            String newPassword, String newIPAddressRange, int sc) {
        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", newUsername);
        updateBody.put("IPAddressRange", newIPAddressRange);
        TResponse res = Http.request("account-update-without-body.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns().debug();
        res.statusCode(sc);
        return res;
    }

    /**
     * Account update with status.
     * @param token token
     * @param cellName cell name
     * @param userName current user name
     * @param newUsername new user name
     * @param newPassword new password
     * @param newStatus new status
     * @param sc expected status code
     * @return response
     */
    @SuppressWarnings("unchecked")
    public static TResponse updateWithStatus(String token, String cellName, String userName, String newUsername,
            String newPassword, String newStatus, int sc) {
        JSONObject updateBody = new JSONObject();
        updateBody.put("Name", newUsername);
        updateBody.put("Status", newStatus);
        TResponse res = Http.request("account-update-without-body.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns().debug();
        res.statusCode(sc);
        return res;
    }

    /**
     * Account update with body.
     * @param token token
     * @param cellName cell name
     * @param userName current user name
     * @param newPassword new password
     * @param updateBody update body
     * @param sc expected status code
     * @return response
     */
    public static TResponse updateWithBody(String token, String cellName, String userName,
            String newPassword, JSONObject updateBody, int sc) {
        TResponse res = Http.request("account-update-without-body.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
                .returns().debug();
        res.statusCode(sc);
        return res;
    }

    /**
     * Account update merge.
     * @param token token
     * @param cellName cell name
     * @param userName current user name
     * @param newPassword new password
     * @param updateBody update body
     * @param sc expected status code
     * @return response
     */
    public static TResponse mergeWithBody(String token, String cellName, String userName,
            String newPassword, JSONObject updateBody, int sc) {
        TResponse res = Http.request("account-update-merge-without-body.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", userName)
                .with("password", newPassword)
                .with("body", updateBody.toJSONString())
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

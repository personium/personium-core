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
import org.json.simple.JSONObject;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class RelationUtils {

    private RelationUtils() {
    }

    /**
     * Relationの複合キーの文字列を作成する.
     * @param relationName Relation名
     * @return 複合キー
     */
    public static String keyString(String relationName) {
        return "Name='" + relationName + "'";
    }

    /**
     * Relationの複合キーの文字列を作成する.
     * @param relationName Relation名
     * @param boxName 紐付くBox名
     * @return 複合キー
     */
    public static String keyString(String relationName, String boxName) {
        return "Name='" + relationName + "',_Box.Name='" + boxName + "'";
    }

    /**
     * Relationを取得するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param relationName リレーション名
     * @param boxName ボックス名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse get(final String cellName,
            final String token, final String relationName, final String boxName, final int code) {
        String boxNameStr = "";
        if (boxName == null) {
            boxNameStr = "null";
        } else {
            boxNameStr = "'" + boxName + "'";
        }
        TResponse response = Http.request("relation-retrieve.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("relationname", relationName)
                .with("boxname", boxNameStr)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * Relationを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param body リクエストBody
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse create(final String cellName,
            final String token, final JSONObject body, int code) {
        TResponse response = Http.request("relation-create.txt")
                .with("token", "Bearer " + token)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * Relationを作成するユーティリティ(Basic認証).
     * @param cellName セル名
     * @param accountName Basic認証で使用するAccount名
     * @param password Basic認証で使用するパスワード
     * @param body リクエストBody
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse createWithBasic(final String cellName,
            final String accountName,
            final String password,
            final JSONObject body,
            int code) {
        String credentials = Base64.encodeBase64String((accountName + ":" + password).getBytes());

        TResponse response = Http.request("relation-create.txt")
                .with("token", "Basic " + credentials)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * NP経由でRelationを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param relationName ロール名
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNP(final String cellName, final String token,
            final String boxName, final String relationName, final int code) {
        JSONObject body = new JSONObject();
        body.put("Name", relationName);

        TResponse res = Http.request("createNP.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cellName)
                .with("entityType", "Box")
                .with("id", boxName)
                .with("navPropName", "_Relation")
                .with("body", body.toString())
                .returns();

        assertEquals(code, res.getStatusCode());

        return res;
    }

    /**
     * Relationを更新するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param relationName リレーション名
     * @param boxName ボックス名
     * @param updateName 新リレーション名
     * @param updateBoxName 新ボックス名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse update(final String cellName,
            final String token, final String relationName, final String boxName,
            final String updateName, final String updateBoxName, final int code) {
        String boxNameStr = "";
        if (boxName == null) {
            boxNameStr = "null";
        } else {
            boxNameStr = "'" + boxName + "'";
        }
        TResponse response = Http.request("relation-update.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("relationname", relationName)
                .with("boxname", boxNameStr)
                .with("updateName", updateName)
                .with("updateBoxName", updateBoxName)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * Relationを削除するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param relationName リレーション名
     * @param boxName ボックス名
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse delete(final String cellName,
            final String token, final String relationName, final String boxName, final int code) {
        String boxNameStr = "";
        if (boxName == null) {
            boxNameStr = "null";
        } else {
            boxNameStr = "'" + boxName + "'";
        }
        TResponse response = Http.request("relation-delete.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("relationname", relationName)
                .with("boxname", boxNameStr)
                .returns()
                .statusCode(code);
        return response;
    }
}

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

import org.json.simple.JSONObject;

import com.fujitsu.dc.common.utils.DcCoreUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class ExtRoleUtils {
    private ExtRoleUtils() {
    }

    /**
     * ExtRoleの取得.
     * @param token トークン
     * @param cellName セル名
     * @param extRoleName 外部ロール名
     * @param relationName 結びつく関係名
     * @param relationBoxName 結びつくボックス名
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse get(final String token, final String cellName, final String extRoleName,
            final String relationName, final String relationBoxName, final int code) {
        TResponse response = Http.request("cell/extRole/extRole-get.txt")
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(extRoleName))
                .with("relationName", relationName)
                .with("relationBoxName", relationBoxName)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * ExtRoleの一覧取得.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse list(final String token, final String cellName, final int code) {
        TResponse response = Http.request("cell/extRole/extRole-list.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * ExtRoleの作成.
     * @param token トークン
     * @param cellName セル名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse create(final String token, final String cellName,
            final JSONObject body, final int code) {
        TResponse res = Http.request("cell/extRole/extRole-create.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * NP経由でExtRoleを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param relationName リレーション名
     * @param relationBoxName リレーションに紐づくBox名
     * @param extRoleUrl ExtRoleのURL
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNP(final String cellName, final String token,
            final String relationName, final String relationBoxName,
            final String extRoleUrl, final int code) {
        JSONObject body = new JSONObject();
        body.put("ExtRole", extRoleUrl);
        body.put("_Relation.Name", relationName);
        body.put("_Relation._Box.Name", relationBoxName);

        TResponse res = Http.request("createNP.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cellName)
                .with("entityType", "Relation")
                .with("id", relationName)
                .with("navPropName", "_ExtRole")
                .with("body", body.toString())
                .returns();

        assertEquals(code, res.getStatusCode());

        return res;
    }


    /**
     * ExtRoleの更新.
     * @param token トークン
     * @param cellName セル名
     * @param extRoleName 外部ロール名
     * @param relationName 結びつく関係名
     * @param relationBoxName 結びつくボックス名
     * @param newextRoleName 新外部ロール名
     * @param newRelation 新結びつく関係名
     * @param newRelationBox 新結びつくボックス名
     * @param code レスポンスコード
     */
    public static void update(final String token, final String cellName, final String extRoleName,
            final String relationName, final String relationBoxName, final String newextRoleName,
            final String newRelation, final String newRelationBox, final int code) {
        Http.request("cell/extRole/extRole-update.txt")
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(extRoleName))
                .with("relationName", relationName)
                .with("relationBoxName", relationBoxName)
                .with("newextRoleName", newextRoleName)
                .with("newRelation", newRelation)
                .with("newRelationBox", newRelationBox)
                .with("token", token).returns()
                .statusCode(code);
    }

    /**
     * ExtRoleの更新(任意のBodyを付加).
     * @param token トークン
     * @param cellName セル名
     * @param extRoleName 外部ロール名
     * @param relationName 結びつく関係名
     * @param relationBoxName 結びつくボックス名
     * @param body リクエストボディ
     * @param code レスポンスコード
     */
    public static void update(final String token, final String cellName, final String extRoleName,
            final String relationName, final String relationBoxName, final JSONObject body, final int code) {
        Http.request("cell/extRole/extRole-update-nobody.txt")
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(extRoleName))
                .with("relationName", relationName)
                .with("relationBoxName", relationBoxName)
                .with("token", token)
                .with("body", body.toString())
                .returns()
                .statusCode(code);
    }

    /**
     * ExtRoleの削除.
     * @param token トークン
     * @param cellName セル名
     * @param extRoleName 外部ロール名
     * @param relationName 結びつく関係名
     * @param relationBoxName 結びつくボックス名
     * @param code レスポンスコード
     */
    public static void delete(final String token, final String cellName, final String extRoleName,
            final String relationName, final String relationBoxName, final int code) {
        Http.request("cell/extRole/extRole-delete.txt")
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(extRoleName))
                .with("relationName", relationName)
                .with("relationBoxName", relationBoxName)
                .with("token", token)
                .returns()
                .statusCode(code);
    }
}

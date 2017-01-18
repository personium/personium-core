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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;

import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class ReceivedMessageUtils {
    private ReceivedMessageUtils() {
    }

    /**
     * Messageの受信.
     * @param pToken トークン
     * @param cellName セル名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse receive(
            final String pToken, final String cellName, final String body, final int code) {
        String token = pToken;
        if (token == null) {
            String targetCellUrl = UrlUtils.cellRoot(cellName);
            String cellUrl = UrlUtils.cellRoot(Setup.TEST_CELL2);
            List<com.fujitsu.dc.common.auth.token.Role> list = new ArrayList<com.fujitsu.dc.common.auth.token.Role>();
            TransCellAccessToken ttk = new TransCellAccessToken(cellUrl, cellUrl, targetCellUrl, list, "");
            token = ttk.toTokenString();
        }
        TResponse response = Http.request("received-message.txt")
                .with("cellPath", cellName)
                .with("body", body)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * NP経由でMessageを受信する.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse receiveViaNP(final String cellName, final String token,
            final String boxName, final String body, final int code) {
        TResponse res = Http.request("createNP.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cellName)
                .with("entityType", "Box")
                .with("id", boxName)
                .with("navPropName", "_ReceivedMessage")
                .with("body", body)
                .returns();

        assertEquals(code, res.getStatusCode());

        return res;
    }

    /**
     * ReceivedMessageの1件取得.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @param messageId メッセージId
     * @return response レスポンス情報
     */
    public static TResponse get(final String token,
            final String cellName, final int code, final String messageId) {
        TResponse response = Http.request("received-message-get.txt")
                .with("cellPath", cellName)
                .with("messageId", messageId)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * ReceivedMessageの一覧取得.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse list(final String token, final String cellName, final int code) {
        return list(token, cellName, "", code);
    }

    /**
     * ReceivedMessageの一覧取得.
     * @param token トークン
     * @param cellName セル名
     * @param query クエリ文字列（"?"を含む）
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse list(final String token, final String cellName, String query, final int code) {
        TResponse response = Http.request("received-message-list.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("query", query)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * ReceivedMessageの削除.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @param messageId メッセージId
     * @return response レスポンス情報
     */
    public static TResponse delete(final String token,
            final String cellName, final int code, final String messageId) {
        TResponse response = Http.request("received-message-delete.txt")
                .with("cellPath", cellName)
                .with("messageId", messageId)
                .with("token", token)
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * Message承認.
     * @param token トークン
     * @param cellName セル名
     * @param uuid メッセージのuuid
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    @SuppressWarnings("unchecked")
    public static TResponse approve(final String token,
            final String cellName, final String uuid, final int code) {
        JSONObject body = new JSONObject();
        body.put("Command", "approved");
        TResponse response = Http.request("approved-message.txt")
                .with("method", "POST")
                .with("cellPath", cellName)
                .with("uuid", uuid)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * Message承認拒否.
     * @param token トークン
     * @param cellName セル名
     * @param uuid メッセージのuuid
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    @SuppressWarnings("unchecked")
    public static TResponse reject(final String token,
            final String cellName, final String uuid, final int code) {
        JSONObject body = new JSONObject();
        body.put("Command", "rejected");
        TResponse response = Http.request("approved-message.txt")
                .with("method", "POST")
                .with("cellPath", cellName)
                .with("uuid", uuid)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * Message承認.
     * @param token トークン
     * @param cellName セル名
     * @param uuid メッセージのuuid
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    @SuppressWarnings("unchecked")
    public static TResponse read(final String token,
            final String cellName, final String uuid, final int code) {
        JSONObject body = new JSONObject();
        body.put("Command", "read");
        TResponse response = Http.request("approved-message.txt")
                .with("method", "POST")
                .with("cellPath", cellName)
                .with("uuid", uuid)
                .with("token", token)
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return response;
    }
}

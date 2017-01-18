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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class SentMessageUtils {
    private SentMessageUtils() {
    }

    /**
     * Messageの送信.
     * @param token トークン
     * @param cellName セル名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse sent(final String token, final String cellName, final String body, final int code) {
        TResponse response = Http.request("sent-message.txt")
                .with("cellPath", cellName)
                .with("body", body)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * Messageの送信.
     * @param authorization トークン
     * @param cellName セル名
     * @param request リクエストボディ
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse sentWithAnyAuthSchema(final String authorization,
            final String cellName,
            final Request request,
            final int code) {
        TResponse response = Http.request("sent-message-anyAuthSchema.txt")
                .with("cellPath", cellName)
                .with("body", request.toJsonString())
                .with("authorization", authorization)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * NP経由でMessageを送信する.
     * @param cellName セル名
     * @param token トークン
     * @param boxName ボックス名
     * @param body リクエストボディ
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse sendViaNP(final String cellName, final String token,
            final String boxName, final String body, final int code) {
        TResponse res = Http.request("createNP.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cellName)
                .with("entityType", "Box")
                .with("id", boxName)
                .with("navPropName", "_SentMessage")
                .with("body", body)
                .returns();

        assertEquals(code, res.getStatusCode());

        return res;
    }

    /**
     * SentMessageの1件取得.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @param messageId メッセージId
     * @return response レスポンス情報
     */
    public static TResponse get(final String token,
            final String cellName, final int code, final String messageId) {
        TResponse response = Http.request("sent-message-get.txt")
                .with("cellPath", cellName)
                .with("messageId", messageId)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * SentMessageの一覧取得.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse list(final String token, final String cellName, final int code) {
        return list(token, cellName, "", code);
    }

    /**
     * SentMessageの一覧取得.
     * @param token トークン
     * @param cellName セル名
     * @param query クエリ（"?"から指定する）
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse list(final String token, final String cellName, String query, final int code) {
        TResponse response = Http.request("sent-message-list.txt")
                .with("cellPath", cellName)
                .with("query", query)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * SentMessageの削除.
     * @param token トークン
     * @param cellName セル名
     * @param code レスポンスコード
     * @param messageId メッセージId
     * @return response レスポンス情報
     */
    public static TResponse delete(final String token,
            final String cellName, final int code, final String messageId) {
        TResponse response = Http.request("sent-message-delete.txt")
                .with("cellPath", cellName)
                .with("messageId", messageId)
                .with("token", token)
                .returns()
                .statusCode(code);
        return response;
    }

    /**
     * 送信メッセージの__idを取得する.
     * @param res メッセージAPIのレスポンス
     * @return 送信メッセージの__id
     */
    public static String getMessageId(TResponse res) {
        // __idの取得
        JSONObject results = (JSONObject) ((JSONObject) res.bodyAsJson().get("d")).get("results");
        return (String) results.get("__id");
    }

    /**
     * 自動生成された受信メッセージの削除.
     * @param targetCell targetCell
     * @param fromCellUrl fromCellUrl
     * @param type type
     * @param title title
     * @param body body
     */
    public static void deleteReceivedMessage(String targetCell,
            String fromCellUrl,
            String type,
            String title,
            String body) {
        TResponse resReceivedList = null;
        try {
            resReceivedList = Http
                    .request("received-message-list.txt")
                    .with("cellPath", targetCell)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("query", "?\\$filter=From+eq+%27" + URLEncoder.encode(fromCellUrl, "UTF-8") + "%27+"
                            + "and+Type+eq+%27" + URLEncoder.encode(type, "UTF-8") + "%27+"
                            + "and+Title+eq+%27" + URLEncoder.encode(title, "UTF-8") + "%27")
                    .returns()
                    .debug();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject jsonBody = (JSONObject) resReceivedList.bodyAsJson().get("d");
        if (jsonBody != null) {
            JSONArray resultsList = (JSONArray) jsonBody.get("results");
            for (int i = 0; i < resultsList.size(); i++) {
                JSONObject results = (JSONObject) resultsList.get(i);
                String id = (String) results.get("__id");
                ReceivedMessageUtils.delete(AbstractCase.MASTER_TOKEN_NAME, targetCell, -1, id);
            }
        }
    }

    /**
     * Message送信リクエスト用のボディを整形するクラス.
     */
    @SuppressWarnings("unchecked")
    public static class Request {

        private JSONObject body = new JSONObject();

        /**
         * Message送信用のコンストラクタ.
         * @param targetCell 送信先Cell名
         */
        public Request(String targetCell) {
            // リクエストボディ作成
            body.put("To", UrlUtils.cellRoot(targetCell));
        }

        /**
         * 送信Messageのタイトルを設定する.
         * @param title タイトル
         * @return リクエスト情報
         */
        public Request title(String title) {
            body.put("Title", title);
            return this;
        }

        /**
         * 送信MessageのBodyを設定する.
         * @param messageBody ボディ
         * @return リクエスト情報
         */
        public Request body(String messageBody) {
            body.put("Body", messageBody);
            return this;
        }

        /**
         * @return リクエストボディ
         */
        public String toJsonString() {
            return body.toJSONString();
        }

    }
}

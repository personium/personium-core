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

import org.json.simple.JSONObject;

import com.fujitsu.dc.common.utils.DcCoreUtils;

/**
 * ExtCell用ユーティリティ.
 */
public class ExtCellUtils {

    private ExtCellUtils() {
    }

    /**
     * ExtCellの取得.
     * @param token トークン
     * @param cellName セル名
     * @param url URL
     * @param code レスポンスコード
     */
    public static void get(final String token, final String cellName,
            final String url, final int code) {
        Http.request("cell/extCell-get.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("accept", "application/xml")
                .with("url", DcCoreUtils.encodeUrlComp(url))
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ExtCellの作成.
     * @param token トークン
     * @param cellName セル名
     * @param url URL
     */
    public static void create(final String token, final String cellName, final String url) {
        Http.request("cell/extCell-create.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("accept", "application/xml")
                .with("url", url)
                .returns();
    }

    /**
     * ExtCellの作成.
     * @param token トークン
     * @param cellName セル名
     * @param url URL
     * @param code レスポンスコード
     */
    public static void create(final String token, final String cellName, final String url, final int code) {
        Http.request("cell/extCell-create.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("accept", "application/xml")
                .with("url", url)
                .returns()
                .statusCode(code);
    }

    /**
     * NP経由でExtCellを作成するユーティリティ.
     * @param cellName セル名
     * @param token トークン
     * @param extCellUrl ExtCellのUrl
     * @param srcEntitySet ソース側エンティティセット名
     * @param srcEntitySetKey ソース側エンティティのキー
     * @param code レスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaNP(
            final String token,
            final String cellName,
            final String extCellUrl,
            final String srcEntitySet,
            final String srcEntitySetKey,
            final int code) {
        JSONObject body = new JSONObject();
        body.put("Url", extCellUrl);

        return Http.request("createNP.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cellName)
                .with("entityType", srcEntitySet)
                .with("id", srcEntitySetKey)
                .with("navPropName", "_ExtCell")
                .with("body", body.toString())
                .returns()
                .statusCode(code);
    }

    /**
     * ExtCellの更新.
     * @param token トークン
     * @param cellName セル名
     * @param url URL
     * @param newUrl URL
     * @param code レスポンスコード
     */
    public static void update(final String token, final String cellName,
            final String url, final String newUrl, final int code) {
        Http.request("cell/extCell-update.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("accept", "application/xml")
                .with("url", DcCoreUtils.encodeUrlComp(url))
                .with("newUrl", newUrl)
                .returns()
                .statusCode(code);
    }

    /**
     * ExtCellの削除.
     * @param token トークン
     * @param cellName セル名
     * @param url ExtCellのurl
     */
    public static void delete(final String token, final String cellName, final String url) {
        Http.request("cell/extCell-delete.txt").with("cellPath", cellName).with("token", token)
                .with("accept", "application/xml").with("url", DcCoreUtils.encodeUrlComp(url)).returns();
    }

    /**
     * ExtCellの削除.
     * @param token トークン
     * @param cellName セル名
     * @param url URL
     * @param code レスポンスコード
     */
    public static void delete(final String token, final String cellName,
            final String url, final int code) {
        Http.request("cell/extCell-delete.txt")
                .with("cellPath", cellName)
                .with("token", token)
                .with("accept", "application/xml")
                .with("url", DcCoreUtils.encodeUrlComp(url))
                .returns()
                .statusCode(code);
    }

    /**
     * ExtCellのアクセスを行う.
     * @param method メソッド名
     * @param cellName セル名
     * @param url URL
     * @param token トークン
     * @param body リクエストボディ
     * @param code レスポンスコード
     */
    public static void extCellAccess(String method, String cellName, String url, String token, String body, int code) {
        Http.request("cell/extCell-multi.txt").with("method", method).with("cellPath", cellName).with("token", token)
                .with("url", DcCoreUtils.encodeUrlComp(url)).with("body", body).returns().statusCode(code);
    }
}

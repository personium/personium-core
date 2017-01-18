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

/**
 * OData Schema 取得($metadata 取得)を利用するユーティリティ.
 */
public class ODataSchemaUtils {

    private ODataSchemaUtils() {
    }

    /**
     * サービスドキュメント($metadata)を取得する.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param code 期待するステータスコード
     * @return レスポンス
     */
    public static TResponse getServiceDocument(
            String token,
            String cellName,
            String boxName,
            String colName,
            int code) {
        return Http.request("box/$metadata-get.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("col", colName)
                .with("accept", "application/atomsvc+xml")
                .with("token", token)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ODataスキーマ($metadata/$metadata)を取得する.
     * @param token トークン
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName ODataコレクション名
     * @param code 期待するステータスコード
     * @return レスポンス
     */
    public static TResponse getODataSchema(
            String token,
            String cellName,
            String boxName,
            String colName,
            int code) {
        return Http.request("box/$metadata-get.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("col", colName + "/\\$metadata")
                .with("accept", "application/atomsvc+xml")
                .with("token", token)
                .returns()
                .debug()
                .statusCode(code);
    }
}

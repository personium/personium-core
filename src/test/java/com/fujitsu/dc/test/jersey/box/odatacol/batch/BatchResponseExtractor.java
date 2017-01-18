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
package com.fujitsu.dc.test.jersey.box.odatacol.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fujitsu.dc.test.utils.TResponse;

/**
 * バッチリクエストのレスポンスから個々のレスポンスを集めるクラス.
 * 注) ただし指定可能なバウンダリは１つのみ
 */
public class BatchResponseExtractor {

    /**
     * コンストラクタ.
     */
    private BatchResponseExtractor() {
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertToItem(String item, Class<T> clazz) throws ParseException {
        if (clazz == JSONObject.class || clazz == Map.class) {
            // JSON部分のみを取り出す。
            int jsonStart = item.indexOf("{");
            String json = "{}";
            if (-1 < jsonStart) {
                json = item.substring(jsonStart);
                JSONParser parser = new JSONParser();
                return (T) parser.parse(json);
            }
        }
        return (T) item;
    }

    /**
     * @param <T> 結果リストの要素型
     * @param response $batchのレスポンス
     * @param startBoundary 開始バウンダリ
     * @param endBoundary 終了バウンダリ
     * @param clazz 結果リストに返したいオブジェクトの型
     * @return 結果のリスト
     * @throws ParseException JSONの解析失敗
     */
    public static <T> List<T> retrieveResponseBodies(TResponse response,
            String startBoundary,
            String endBoundary,
            Class<T> clazz) throws ParseException {
        return retrieveResponseBodies(response.getBody(), startBoundary, endBoundary, clazz);
    }

    /**
     * @param <T> 結果リストの要素型
     * @param response $batchのレスポンス
     * @param startBoundary 開始バウンダリ
     * @param endBoundary 終了バウンダリ
     * @param clazz 結果リストに返したいオブジェクトの型
     * @return 結果のリスト
     * @throws ParseException JSONの解析失敗
     */
    public static <T> List<T> retrieveResponseBodies(String response,
            String startBoundary,
            String endBoundary,
            Class<T> clazz) throws ParseException {
        List<T> result = new ArrayList<T>();

        // パターンマッチングもいいいが、面倒なので文字列検索する
        String target = response;

        // 初回の startBoundaryを探す。
        int position = target.indexOf(startBoundary);
        if (-1 == position) {
            return result;
        }

        target = target.substring(position + startBoundary.length());

        while (0 <= (position = target.indexOf(startBoundary))) {
            String item = target.substring(0, position);
            result.add(BatchResponseExtractor.convertToItem(item, clazz));
            target = target.substring(position + startBoundary.length());
        }
        position = target.indexOf(endBoundary);
        if (-1 < position) {
            String item = target.substring(0, position);
            result.add(BatchResponseExtractor.convertToItem(item, clazz));
        }
        return result;
    }

    /**
     * テスト用.
     * @param args 引数
     * @throws Exception 処理例外
     */
    public static void main(String[] args) throws Exception {
        final String testStartBoundary = "-- abcde\n";
        final String testEndBoundary = "-- abcde";

        String response = "{\"a\":1}" + testStartBoundary + "{\"b\":2, \"x\": { \"aaa\" : \"foo\"}}"
                + testStartBoundary + "{\"c\":3}" + testEndBoundary;
        List<JSONObject> result = BatchResponseExtractor.retrieveResponseBodies(
                response, testStartBoundary, testEndBoundary, JSONObject.class);
        for (JSONObject res : result) {
            System.out.println("=====================");
            System.out.println(res.toJSONString());
            System.out.println("=====================");
        }
    }
}

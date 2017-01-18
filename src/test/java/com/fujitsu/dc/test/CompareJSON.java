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
package com.fujitsu.dc.test;

import java.util.Arrays;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * JSON比較クラス.
 */
public class CompareJSON {

    /**
     * デフォルトコンストラクタの隠蔽.
     */
    private CompareJSON() {
        // Nothing todo
    }

    /**
     * 比較結果格納クラス.
     */
    public static class Result extends HashMap<Object, Object> {

        /**
         * .
         */
        private static final long serialVersionUID = 4728015078227545856L;

        /**
         * .
         * @param key キー
         * @param actualValue 実際の値
         * @param message メッセージ
         * @return 旧オブジェクト
         */
        public Object put(Object key, Object actualValue, String message) {
            return super.put(key, new Object[] {message, actualValue });
        }

        /**
         * 差分があった場合の差分情報メッセージを返す。
         * @param key キー
         * @return 差分があった場合の差分情報メッセージ
         */
        @Override
        public Object get(Object key) {
            return ((Object[]) super.get(key))[0];
        }

        /**
         * JSON内の acutalValueを返す。
         * @param key キー
         * @return 実際の値
         */
        public Object getMismatchValue(Object key) {
            return ((Object[]) super.get(key))[1];
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Object value : this.values()) {
                builder.append(((Object[]) value)[0]).append("\n");
            }
            return builder.toString();
        }
    }

    /**
     * ２つのJSONを比較し、差分を報告する.
     * @param source 正しい/オリジナルの JSON
     * @param toBeCompared 比較対象JSON
     * @return 比較結果. 差分がない場合は nullを返す。
     */
    public static Result compareJSON(JSONObject source, JSONObject toBeCompared) {
        Result result = new Result();

        // 比較不要な要素の削除
        if (source.containsKey("d")) {
            source = (JSONObject) source.get("d");
        }
        if (source.containsKey("results")) {
            source = (JSONObject) source.get("results");
        }

        if (toBeCompared.containsKey("d")) {
            toBeCompared = (JSONObject) toBeCompared.get("d");
        }
        if (toBeCompared.containsKey("results")) {
            toBeCompared = (JSONObject) toBeCompared.get("results");
        }

        source.remove("__published");
        source.remove("__updated");
        source.remove("__metadata");

        toBeCompared.remove("__published");
        toBeCompared.remove("__updated");
        toBeCompared.remove("__metadata");

        // JSONの比較
        compareJSON(result, "", source, toBeCompared);
        if (0 < result.size()) {
            return result;
        } else {
            return null;
        }
    }

    /**
     * ２つのJSONを比較し、差分を報告する.
     * @param source 正しい/オリジナルの JSON
     * @param toBeCompared 比較対象JSON
     * @return 比較結果. 差分がない場合は nullを返す。
     * @throws ParseException 入力が正しい JSONではない場合.
     */
    public static Result compareJSON(String source, String toBeCompared) throws ParseException {
        JSONParser parser = new JSONParser();
        return compareJSON((JSONObject) parser.parse(source), (JSONObject) parser.parse(toBeCompared));
    }

    private static String toKey(Object parentKey, Object key) {
        if (0 < ((String) parentKey).length()) {
            return parentKey + "." + key;
        } else {
            return (String) key;
        }
    }

    /**
     * ２つのJSONを比較し、差分を報告する.
     * @param result 結果オブジェクト
     * @param parentKey jsonの上位キー
     * @param source 正しい/オリジナルの JSON
     * @param toBeCompared 比較対象JSON
     * @return 比較結果
     */
    private static Result compareJSON(Result result, Object parentKey, JSONObject source, JSONObject toBeCompared) {
        for (Object key : source.keySet()) {
            Object originalValue = source.get(key);
            Object targetValue = null;
            if (toBeCompared.containsKey(key)) {
                targetValue = toBeCompared.get(key);
            } else {
                result.put(toKey(parentKey, key), null,
                        String.format("key[%s] does not exist in target JSON.", toKey(parentKey, key)));
                continue;
            }

            if (originalValue instanceof JSONArray) {
                Object targetVal = toBeCompared.get(key);
                if (targetVal instanceof JSONArray) {
                    compareJSONArray(result, toKey(parentKey, key), (JSONArray) originalValue, (JSONArray) targetValue);
                } else {
                    result.put(toKey(parentKey, key), String.format(
                            "Value of target of key[%s] has different type (e.g. JSONArray) as source JSON.",
                            targetVal,
                            toKey(parentKey, key)));
                }
            } else if (originalValue instanceof JSONObject) {
                if (targetValue instanceof JSONObject) {
                    compareJSON(result, toKey(parentKey, key), (JSONObject) originalValue, (JSONObject) targetValue);
                } else {
                    result.put(toKey(parentKey, key),
                            targetValue,
                            String.format(
                                    "Value of target of key[%s] has different type (e.g. JSONArray) as source JSON.",
                                    toKey(parentKey, key)));
                }
            } else {
                compareValue(result, toKey(parentKey, key), originalValue, targetValue);
            }
        }

        for (Object key : toBeCompared.keySet()) {
            if (!source.containsKey(key)) {
                result.put(toKey(parentKey, key),
                        toBeCompared.get(key),
                        String.format("Excess key[%s] exists in target JSON.", toKey(parentKey, key)));
            }
        }
        return result;
    }

    private static Result compareValue(Result result, Object key, Object source, Object toBeCompared) {
        if (source == toBeCompared) {
            // nullチェック
            return result;
        }
        if (!source.equals(toBeCompared)) {
            result.put(key,
                    toBeCompared,
                    String.format(
                            "Value of target of key[%s] "
                                    + "does not have the same value as source JSON.  orignal: [%s], target[%s]",
                            key, source, toBeCompared));
        }
        return result;
    }

    private static Result compareJSONArray(Result result, Object key, JSONArray source, JSONArray toBeCompared) {
        if (source == toBeCompared) {
            // nullチェック
            return result;
        }
        if (null == toBeCompared) {
            result.put(key,
                    toBeCompared,
                    String.format("Size of JSONArray [%s] is different. expected[%d], target[null]", key,
                            source.size()));
        }
        Object[] expectedArray = source.toArray();
        Object[] targetArray = toBeCompared.toArray();
        Arrays.sort(expectedArray);
        Arrays.sort(targetArray);
        if (expectedArray.length != targetArray.length) {
            result.put(key,
                    toBeCompared,
                    String.format("Size of JSONArray [%s] is different. expected[%d], target[%d]", key,
                            source.size(),
                            toBeCompared.size()));
            return result;
        }
        for (int i = 0; i < expectedArray.length; i++) {
            Object expected2 = expectedArray[i];
            Object target2 = targetArray[i];
            if (expected2 instanceof JSONObject && target2 instanceof JSONObject) {
                compareJSON(result, key, (JSONObject) expected2, (JSONObject) target2);
            } else if (!expected2.equals(target2)) {
                result.put(key,
                        toBeCompared,
                        String.format("Value of target of key[%s] does "
                                + "not have the same value as source JSON.  orignal: [%s], target[%s]",
                                key, source, toBeCompared));
            }
        }
        return result;
    }

}

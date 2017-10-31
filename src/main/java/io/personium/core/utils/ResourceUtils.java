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
package io.personium.core.utils;

import java.io.IOException;
import java.io.Reader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.personium.core.PersoniumCoreException;

/**
 * リソース系ユーティリティ関数を集めたクラス.
 */
public class ResourceUtils {

    /**
     * constructor.
     */
    private ResourceUtils() {
    }

    /**
     * Readerからリクエストボディを取得してJSONObjectにする.
     * @param reader リクエストボディ
     * @return JSONObject
     */
    public static JSONObject parseBodyAsJSON(Reader reader) {
        StringBuilder sb = new StringBuilder();
        int character;
        JSONObject body;
        try {
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Common.REQUEST_BODY_LOAD_FAILED.reason(e);
        }
        String bodyString = sb.toString();
        try {
            body = (JSONObject) (new JSONParser()).parse(bodyString);
        } catch (ParseException e) {
            throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(bodyString);
        }
        return body;
    }

    /**
     * リクエストボディが存在するかどうかをヘッダ情報からチェックする. <br />
     * ※本メソッドではリクエストボディは読み取らず、ヘッダ情報からリクエストボディの有無を判定する。
     * @param contentLength Content-Lengthヘッダの値
     * @param transferEncoding Transfer-Encodingヘッダの値
     * @return true:リクエストボディが存在する false:リクエストボディが存在しない
     */
    public static boolean hasApparentlyRequestBody(Long contentLength, String transferEncoding) {
        // 以下を満たせばリクエストボディが存在すると判定する
        return ((null != contentLength && contentLength > 0L) //NOPMD -To maintain readability
                || null != transferEncoding);
    }

}

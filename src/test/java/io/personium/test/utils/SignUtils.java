/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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

import java.nio.charset.StandardCharsets;

/**
 * Utility Class for using Sign API
 */
public class SignUtils {
    private SignUtils() {
    }

    /**
     * Generate sign for content text
     * @param cellName target cellName
     * @param token token to be used
     * @param text text to be signed
     * @param code expected status code
     * @return TResponse
     */
    public static TResponse post(String cellName, String token, String text, int code) {
        return post(cellName, token, "application/jose", text, code);
    }

    /**
     * Generate sign for content text
     * @param cellName target cellName
     * @param token token to be used
     * @param body byte array body to be signed
     * @param code expected status code
     * @return TResponse
     */
    public static TResponse post(String cellName, String token, byte[] body, int code) {
        return post(cellName, token, "application/jose", body, code);
    }

    /**
     * Generate sign for content text
     * @param cellName target cellName
     * @param token token to be used
     * @param accept accept header
     * @param text text to be signed
     * @param code expected status code
     * @return TResponse
     */
    public static TResponse post(String cellName, String token, String accept, String text, int code) {
        return post(cellName, token, accept, text.getBytes(StandardCharsets.UTF_8), code);
    }


    /**
     * Generate sign for content text
     * @param cellName target cellName
     * @param token token to be used
     * @param accept accept header
     * @param body byte array body to be signed
     * @param code expected status code
     * @return TResponse
     */
    public static TResponse post(String cellName, String token, String accept, byte[] body, int code) {
        TResponse response = Http.request("cell/sign-post.txt").with("token", token).with("accept", accept)
                .with("cellPath", cellName).setBodyBinary(body).returns().statusCode(code);
        return response;
    }
}
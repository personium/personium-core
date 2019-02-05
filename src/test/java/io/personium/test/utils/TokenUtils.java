/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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

import io.personium.core.auth.OAuth2Helper;

/**
 * Util for calling token endpoint.
 */
public class TokenUtils {

    /**
     * Constructor.
     */
    private TokenUtils() {
    }

    public static TResponse getTokenPassword(String cellName, String username, String password, int statusCode) {
        return getTokenPassword(cellName, username, password, null, statusCode);
    }

    public static TResponse getTokenPassword(String cellName, String username, String password, String pTarget, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.PASSWORD)
                   .append("&username=").append(username)
                   .append("&password=").append(password);
        if (pTarget != null) {
            bodyBuilder.append("&p_target=").append(pTarget);
        }
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    public static TResponse getTokenRefresh(String cellName, String refreshToken, int statusCode) {
        return getTokenRefresh(cellName, refreshToken, null, statusCode);
    }

    public static TResponse getTokenRefresh(String cellName, String refreshToken, String pTarget, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.REFRESH_TOKEN)
                   .append("&refresh_token=").append(refreshToken);
        if (pTarget != null) {
            bodyBuilder.append("&p_target=").append(pTarget);
        }
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    public static TResponse getTokenCode(String cellName, String code, String clientId, String clientSecret, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.AUTHORIZATION_CODE)
                   .append("&code=").append(code)
                   .append("&client_id=").append(clientId)
                   .append("&client_secret=").append(clientSecret);
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    public static TResponse getToken(String cellName, String body, int statusCode) {
        return Http.request("authn/auth.txt")
                .with("remoteCell", cellName)
                .with("body", body)
                .returns().statusCode(statusCode).debug();
    }
}

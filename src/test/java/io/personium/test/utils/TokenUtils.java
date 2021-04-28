/**
 * Personium
 * Copyright 2019-2021 Personium Project Authors
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

    /**
     * Exec token API for password authentication.
     * @param cellName Target cell name
     * @param username username
     * @param password password
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenPassword(String cellName, String username, String password, int statusCode) {
        return getTokenPassword(cellName, username, password, null, statusCode);
    }

    /**
     * Exec token API for password authentication.
     * @param cellName Target cell name
     * @param username username
     * @param password password
     * @param pTarget p_target
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenPassword(String cellName, String username, String password,
            String pTarget, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.PASSWORD)
                   .append("&username=").append(username)
                   .append("&password=").append(password);
        if (pTarget != null) {
            bodyBuilder.append("&p_target=").append(pTarget);
        }
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec token API for password authentication.
     * p_cookie=true.
     * @param cellName Target cell name
     * @param username username
     * @param password password
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenPasswordPCookie(String cellName, String username, String password, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.PASSWORD)
                   .append("&username=").append(username)
                   .append("&password=").append(password)
                   .append("&p_cookie=true");
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec token API for password authentication.
     * p_cookie=true.
     * @param cellName Target cell name
     * @param username username
     * @param password password
     * @param expiresIn expires in
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenPasswordPCookie(String cellName, String username, String password, String expiresIn, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.PASSWORD)
                   .append("&username=").append(username)
                   .append("&password=").append(password)
                   .append("&expires_in=").append(expiresIn)
                   .append("&p_cookie=true");
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec token API for token refresh.
     * @param cellName Target cell name
     * @param refreshToken refreshtoken
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenRefresh(String cellName, String refreshToken, int statusCode) {
        return getTokenRefresh(cellName, refreshToken, null, statusCode);
    }

    /**
     * Exec token API for token refresh.
     * @param cellName Target cell name
     * @param refreshToken refreshtoken
     * @param pTarget p_target
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenRefresh(String cellName, String refreshToken, String pTarget, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.REFRESH_TOKEN)
                   .append("&refresh_token=").append(refreshToken);
        if (pTarget != null) {
            bodyBuilder.append("&p_target=").append(pTarget);
        }
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec token API for code authentication.
     * @param cellName Target cell name
     * @param code code
     * @param clientId client_id
     * @param clientSecret client_secret
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getTokenCode(String cellName, String code,
            String clientId, String clientSecret, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("grant_type=").append(OAuth2Helper.GrantType.AUTHORIZATION_CODE)
                   .append("&code=").append(code)
                   .append("&client_id=").append(clientId)
                   .append("&client_secret=").append(clientSecret);
        return getToken(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec token API.
     * @param cellName Target cell name
     * @param body body string
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getToken(String cellName, String body, int statusCode) {
        return Http.request("authn/auth.txt")
                .with("remoteCell", cellName)
                .with("body", body)
                .returns().statusCode(statusCode).debug();
    }
}

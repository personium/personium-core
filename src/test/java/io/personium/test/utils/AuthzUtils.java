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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.CharEncoding;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.model.Box;
import io.personium.core.rs.cell.AuthResourceUtils;
import io.personium.test.setup.Setup;

/**
 * Util for calling authz endpoint.
 */
public class AuthzUtils {

    /**
     * Constructor.
     */
    private AuthzUtils() {
    }

    /**
     * Exec certs endpoint GET API.
     * @param cellName Target cell name
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse certsGet(String cellName, int statusCode) {
        return Http.request("cell/certs-get.txt")
                .with("cell", Setup.TEST_CELL1)
                .returns().debug().statusCode(statusCode);
    }

    /**
     * Exec GET API.
     * This method that specifies only required params.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse get(String cellName, String responseType, String redirectUri,
            String clientId, int statusCode) {
        return get(cellName, responseType, redirectUri, clientId, null, statusCode);
    }

    /**
     * Exec GET API.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse get(String cellName, String responseType, String redirectUri,
            String clientId, String state, int statusCode) {
        return get(cellName, responseType, redirectUri, clientId, state, null, statusCode);
    }

    /**
     * Exec GET API.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param scope scope
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse get(String cellName, String responseType, String redirectUri,
            String clientId, String state, String scope, int statusCode) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("response_type=").append(responseType)
                    .append("&redirect_uri=").append(redirectUri)
                    .append("&client_id=").append(clientId);
        if (state != null) {
            queryBuilder.append("&state=").append(state);
        }
        if (scope != null) {
            queryBuilder.append("&scope=").append(scope);
        }
        return get(cellName, queryBuilder.toString(), statusCode);
    }

    /**
     * Exec GET API.
     * @param cellName Target cell name
     * @param query Query string
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse get(String cellName, String query, int statusCode) {
        return Http.request("authz/authz-get.txt")
                .with("cellName", cellName)
                .with("query", query)
                .returns().statusCode(statusCode).debug();
    }

    /**
     * Exec GET API.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param pCookie p_cookie
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getPCookie(String cellName, String responseType, String redirectUri,
            String clientId, String state, String pCookie, int statusCode) {
        return getPCookie(cellName, responseType, redirectUri, clientId, state, null, pCookie, statusCode);
    }

    /**
     * Exec GET API.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param scope scope
     * @param pCookie p_cookie
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getPCookie(String cellName, String responseType, String redirectUri,
            String clientId, String state, String scope, String pCookie, int statusCode) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("response_type=").append(responseType)
                    .append("&redirect_uri=").append(redirectUri)
                    .append("&client_id=").append(clientId);
        if (state != null) {
            queryBuilder.append("&state=").append(state);
        }
        if (scope != null) {
            queryBuilder.append("&scope=").append(scope);
        }
        return getPCookie(cellName, queryBuilder.toString(), pCookie, statusCode);
    }

    /**
     * Exec GET API for p_cookie authentication.
     * @param cellName Target cell name
     * @param query Query string
     * @param pCookie p_cookie
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse getPCookie(String cellName, String query, String pCookie, int statusCode) {
        return Http.request("authz/authz-get-pcookie.txt")
                .with("cellName", cellName)
                .with("pCookie", pCookie)
                .with("query", query)
                .returns().statusCode(statusCode).debug();
    }

    /**
     * Exec POST API for password authentication.
     * This method that specifies only required params.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param username username
     * @param password password
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse postPassword(String cellName, String responseType, String redirectUri,
            String clientId, String username, String password, int statusCode) {
        return postPassword(cellName, responseType, redirectUri, clientId, null, username, password, statusCode);
    }

    /**
     * Exec POST API for password authentication.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param username username
     * @param password password
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse postPassword(String cellName, String responseType, String redirectUri,
            String clientId, String state, String username, String password, int statusCode) {
        return postPassword(cellName, responseType, redirectUri, clientId, state, null, username, password, statusCode);
    }

    /**
     * Exec POST API for password authentication.
     * This method that can also specify optional params.
     * Parameters that specify null are ignored.
     * @param cellName Target cell name
     * @param responseType response_type
     * @param redirectUri redirect_uri
     * @param clientId client_id
     * @param state state
     * @param scope scope
     * @param username username
     * @param password password
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse postPassword(String cellName, String responseType, String redirectUri,
            String clientId, String state, String scope, String username, String password, int statusCode) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("response_type=").append(responseType)
                   .append("&redirect_uri=").append(redirectUri)
                   .append("&client_id=").append(clientId)
                   .append("&username=").append(username)
                   .append("&password=").append(password);
        if (state != null) {
            bodyBuilder.append("&state=").append(state);
        }
        if (scope != null) {
            bodyBuilder.append("&scope=").append(scope);
        }
        return post(cellName, bodyBuilder.toString(), statusCode);
    }

    /**
     * Exec POST API.
     * @param cellName Target cell name
     * @param body Body string
     * @param statusCode Expected response code
     * @return API response
     */
    public static TResponse post(String cellName, String body, int statusCode) {
        return Http.request("authz/authz-post.txt")
                .with("cellName", cellName)
                .with("body", body)
                .returns().statusCode(statusCode).debug();
    }

    // Create system default html.
    // TODO Should call AuthzEndPointResource.createForm() properly.
    public static String createDefaultHtml(String clientId, String redirectUriStr, String message, String state,
            String scope, String responseType, String pTarget, String pOwner, String cellUrl) {
        // If processing fails, return system default html.
        List<Object> paramsList = new ArrayList<Object>();

        if (!"".equals(clientId) && !clientId.endsWith("/")) {
            clientId = clientId + "/";
        }

        paramsList.add(AuthResourceUtils.getJavascript("ajax.js"));
        paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
        paramsList.add(clientId + Box.DEFAULT_BOX_NAME + "/profile.json");
        paramsList.add(cellUrl + Box.DEFAULT_BOX_NAME + "/profile.json");
        paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
        paramsList.add(cellUrl + "__authz");
        paramsList.add(message);
        paramsList.add(state != null ? state : ""); // CHECKSTYLE IGNORE
        paramsList.add(responseType);
        paramsList.add(clientId);
        paramsList.add(redirectUriStr);
        paramsList.add(scope != null ? scope : ""); // CHECKSTYLE IGNORE

        Object[] params = paramsList.toArray();

        String html = PersoniumCoreUtils.readStringResource("html/authform.html", CharEncoding.UTF_8);
        html = MessageFormat.format(html, params);

        return html;
    }
}

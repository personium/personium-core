/**
 * Personium
 * Copyright 2014-2019 Personium Project
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
package io.personium.core.auth;

import javax.xml.namespace.QName;

import io.personium.common.utils.CommonUtils;

/**
 * A utility around OAuth 2.0.
 * RFC6749 The OAuth 2.0 Authorization Framework
 *   https://tools.ietf.org/html/rfc6749
 * RFC6750 The OAuth 2.0 The OAuth 2.0 Authorization Framework: Bearer Token Usage
 *   https://tools.ietf.org/html/rfc6750
 * RFC7522 SAML 2.0 Profile for OAuth 2.0 Client Authentication and Authorization Grants
 *   https://tools.ietf.org/html/rfc7522
 */
public final class OAuth2Helper {
    private OAuth2Helper() {
    }

    /**
     * URN representing SAML Assertion.
     */
    public static final String URN_SAML_ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";
    /**
     * URN representing SAML Format.
     */
    public static final String SAML_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";

    /**
     * Literal on Authorization Scheme handled by OAuth 2.
     */
    public static class Scheme {
        /**
         * Bearer.
         */
        public static final String BEARER = "Bearer";
        /**
         * Basic.
         */
        public static final String BASIC = "Basic";
        /**
         * Credentials prefix for Bearer format.
         */
        public static final String BEARER_CREDENTIALS_PREFIX = "Bearer ";
    }

    /**
     * Literal on OAuth 2 error.
     */
    public static class Error {
        /**
         * The request is missing a required parameter, includes an
         * unsupported parameter value (other than grant type),
         * repeats a parameter, includes multiple credentials,
         * utilizes more than one mechanism for authenticating the
         * client, or is otherwise malformed.
         */
        public static final String INVALID_REQUEST = "invalid_request";
        /**
         * Client authentication failed (e.g. unknown client, no
         * client authentication included, or unsupported
         * authentication method). The authorization server MAY
         * return an HTTP 401 (Unauthorized) status code to indicate
         * which HTTP authentication schemes are supported. If the
         * client attempted to authenticate via the "Authorization"
         * request header field, the authorization server MUST
         * respond with an HTTP 401 (Unauthorized) status code, and
         * include the "WWW-Authenticate" response header field
         * matching the authentication scheme used by the client.
         */
        public static final String INVALID_CLIENT = "invalid_client";
        /**
         * The provided authorization grant (e.g. authorization code, resource owner credentials) or refresh token is
         * invalid, expired, revoked, does not match the redirection
         * URI used in the authorization request, or was issued to
         * another client.
         */
        public static final String INVALID_GRANT = "invalid_grant";
        /**
         * The authenticated client is not authorized to use this authorization grant type.
         */
        public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
        /**
         * The resource owner or authorization server denied the request.
         */
        public static final String ACCESS_DENIED = "access_denied";
        /**
         * The authorization grant type is not supported by the authorization server.
         */
        public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
        /**
         * The authorization response_type is not supported by the authorization server.
         */
        public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
        /**
         * The requested scope is invalid, unknown, malformed, or
         * exceeds the scope granted by the resource owner.
         */
        public static final String INVALID_SCOPE = "invalid_scope";
        /**
         * server_error.
         */
        public static final String SERVER_ERROR = "server_error";
        /**
         * temporarily_unavailable.
         */
        public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    }

    /**
     * Literal on Grant Type of OAuth 2.
     */
    public static class GrantType {
        /**
         * password.
         */
        public static final String PASSWORD = "password";
        /**
         * client_credentials.
         */
        public static final String CLIENT_CREDENTIALS = "client_credentials";
        /**
         * authorization_code.
         */
        public static final String AUTHORIZATION_CODE = "authorization_code";
        /**
         * saml2 bearer.
         */
        public static final String SAML2_BEARER = "urn:ietf:params:oauth:grant-type:saml2-bearer";
        /**
         * refresh_token.
         */
        public static final String REFRESH_TOKEN = "refresh_token";
        /**
         * Grant Type URN for oidc google.
         */
        public static final String URN_OIDC_GOOGLE = "urn:x-personium:oidc:google";

    }

    /**
     * Literal on Response Type of OAuth 2.
     */
    public static class ResponseType {
        /**
         * token.
         */
        public static final String TOKEN = "token";
        /** code. */
        public static final String CODE = "code";
        /** id_token. It is used with the openid connect of the oauth2 extension. */
        public static final String ID_TOKEN = "id_token";
    }

    /**
     * Literal on Scope.
     */
    public static class Scope {
        /** openid. It is used with the openid connect of the oauth2 extension. */
        public static final String OPENID = "openid";

    }

    /**
     * Literal on various key parameters of OAuth 2.
     */
    public static class Key {
        /**
         * grant_type.
         */
        public static final String GRANT_TYPE = "grant_type";
        /**
         * code.
         */
        public static final String CODE = "code";
        /**
         * redirect_uri.
         */
        public static final String REDIRECT_URI = "redirect_uri";
        /**
         * response_type.
         */
        public static final String RESPONSE_TYPE = "response_type";
        /**
         * client_id.
         */
        public static final String CLIENT_ID = "client_id";
        /**
         * client_secret.
         */
        public static final String CLIENT_SECRET = "client_secret";
        /**
         * "client_assertion" parameter key defined in RFC7521.
         * https://tools.ietf.org/html/rfc7521#section-4.2
         */
        public static final String CLIENT_ASSERTION = "client_assertion";
        /**
         * "client_assertion_type" parameter key defined in RFC7521.
         * https://tools.ietf.org/html/rfc7521#section-4.2
         */
        public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";


        /**
         * state.
         */
        public static final String STATE = "state";
        /**
         * keeplogin.
         */
        public static final String KEEPLOGIN = "keeplogin";
        /**
         * Cancel flag.
         */
        public static final String CANCEL_FLG = "cancel_flg";
        /**
         * username.
         */
        public static final String USERNAME = "username";
        /**
         * password.
         */
        public static final String PASSWORD = "password";
        /**
         * assertion.
         */
        public static final String ASSERTION = "assertion";
        /**
         * refresh_token.
         */
        public static final String REFRESH_TOKEN = "refresh_token";
        /**
         * id_token.
         */
        public static final String ID_TOKEN = "id_token";

        /**
         * session_id.
         */
        public static final String SESSION_ID = "session-id";
        /**
         * error.
         */
        public static final String ERROR = "error";
        /**
         * error_description.
         */
        public static final String ERROR_DESCRIPTION = "error_description";
        /**
         * error_uri.
         */
        public static final String ERROR_URI = "error_uri";
        /**
         * access_token.
         */
        public static final String ACCESS_TOKEN = "access_token";
        /**
         * token_type.
         */
        public static final String TOKEN_TYPE = "token_type";
        /**
         * expires_in.
         */
        public static final String EXPIRES_IN = "expires_in";
        /**
         * password change required.
         */
        public static final String PASSWORD_CHANGE_REQUIRED = "password_change_required";
        /**
         * box not installed.
         */
        public static final String BOX_NOT_INSTALLED = "box_not_installed";
        /**
         * scope.
         */
        public static final String SCOPE = "scope";
        /**
         * p_target.
         */
        public static final String TARGET = "p_target";
        /**
         * p_owner.
         */
        public static final String OWNER = "p_owner";
        /**
         * p_cookie.
         */
        public static final String P_COOKIE = "p_cookie";
        /**
         * p_owner value.
         */
        public static final String TRUE_STR = "true";
        
        /**
         * "code_challenge" defined in PKCE (RFC 7636).
         * https://tools.ietf.org/html/rfc7636
         */
        public static final String CODE_CHALLENGE = "code_challenge";
        /**
         * "code_challenge_method" defined in PKCE (RFC 7636).
         * https://tools.ietf.org/html/rfc7636
         */
        public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
        /**
         * "code_verifier" defined in PKCE (RFC 7636).
         * https://tools.ietf.org/html/rfc7636
         */
        public static final String CODE_VERIFIER = "code_verifier";
        /**
         * "S256" .
         * https://tools.ietf.org/html/rfc7636
         */
        public static final String S256 = "S256";

        /**
         * refresh_token_expires_in.
         */
        public static final String REFRESH_TOKEN_EXPIRES_IN = "refresh_token_expires_in";
        /**
         * last_authenticated.
         */
        public static final String LAST_AUTHENTICATED = "last_authenticated";
        /**
         * failed_count.
         */
        public static final String FAILED_COUNT = "failed_count";
        /**
         * #c.
         */
        public static final String CONFIDENTIAL_MARKER = "#c";
        /**
         * Confidential Role name.
         */
        public static final String CONFIDENTIAL_ROLE_NAME = "confidentialClient";
        /**
         * ConfidentialRole resource URL format.
         */
        public static final String CONFIDENTIAL_ROLE_URL_FORMAT = "%s__role/%s/" + CONFIDENTIAL_ROLE_NAME;
        /**
         * ownerRepresentativeAccounts.
         */
        public static final QName PROP_KEY_OWNER_REPRESENTIVE_ACCOUNTS =
                new QName(CommonUtils.XmlConst.NS_PERSONIUM, "ownerRepresentativeAccounts");
        /**
         * ownerRepresentativeAccount.
         */
        public static final QName PROP_KEY_OWNER_REPRESENTIVE_ACCOUNT =
                new QName(CommonUtils.XmlConst.NS_PERSONIUM, "account");
    }

    /**
     * Schema Authz Level.
     */
    public static class SchemaLevel {
        /**
         * none.
         */
        public static final String NONE = "none";
        /**
         * public.
         */
        public static final String PUBLIC = "public";
        /**
         * confidential.
         */
        public static final String CONFIDENTIAL = "confidential";

        /**
         * Check whether it matches permitted value.
         * Returns true if argument is null or empty.
         * @param value Target value
         * @return true:match false:not match
         */
        public static boolean isPermittedValue(String value) {
            if (value == null
                    || NONE.equals(value)
                    || PUBLIC.equals(value)
                    || CONFIDENTIAL.equals(value)) {
                return true;
            }
            return false;
        }
    }

    /**
     * The type of AuthScheme allowed according to the resource.
     */
    public enum AcceptableAuthScheme {
        /** Allow only Basic.*/
        BASIC,
        /** Only allow Bearer.*/
        BEARER,
        /** Allow all AuthScheme.*/
        ALL
    }
}

/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.core.auth.OAuth2Helper.Error;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.auth.OAuth2Helper.Scheme;
import io.personium.core.utils.EscapeControlCode;
import io.personium.plugin.base.PluginMessageUtils.Severity;
import io.personium.plugin.base.auth.AuthPluginException;

/**
 * Log message creation class.
 */
@SuppressWarnings("serial")
public final class PersoniumCoreAuthnException extends PersoniumCoreException {

    /**
     * Grant-Type value is abnormal.
     */
    public static final PersoniumCoreAuthnException UNSUPPORTED_GRANT_TYPE =
            create("PR400-AN-0001", Error.UNSUPPORTED_GRANT_TYPE);
    /**
     * Abnormal value of p_target.
     */
    public static final PersoniumCoreAuthnException INVALID_TARGET =
            create("PR400-AN-0002", Error.INVALID_REQUEST);
    /**
     * Client Secret Parsing error.
     */
    public static final PersoniumCoreAuthnException CLIENT_ASSERTION_PARSE_ERROR =
            create("PR400-AN-0003", Error.INVALID_CLIENT);
    /**
     * Client Secret expiration date check.
     */
    public static final PersoniumCoreAuthnException CLIENT_SECRET_EXPIRED =
            create("PR400-AN-0004", Error.INVALID_CLIENT);
    /**
     * Client Secret Signature validation error.
     */
    public static final PersoniumCoreAuthnException CLIENT_SECRET_DSIG_INVALID =
            create("PR400-AN-0005", Error.INVALID_CLIENT);
    /**
     * Issuer of Client Secret does not match client id.
     */
    public static final PersoniumCoreAuthnException CLIENT_SECRET_ISSUER_MISMATCH =
            create("PR400-AN-0006", Error.INVALID_CLIENT);
    /**
     * Client Secret's target is not yourself.
     */
    public static final PersoniumCoreAuthnException CLIENT_SECRET_TARGET_WRONG =
            create("PR400-AN-0007", Error.INVALID_CLIENT);

    /**
     * For transcell token authentication, unit user promotion is not possible.
     */
    public static final PersoniumCoreAuthnException TC_ACCESS_REPRESENTING_OWNER =
            create("PR400-AN-0008", Error.INVALID_GRANT);
    /**
     * Token parsing error.
     */
    public static final PersoniumCoreAuthnException TOKEN_PARSE_ERROR =
            create("PR400-AN-0009", Error.INVALID_GRANT);
    /**
     * Expired.
     */
    public static final PersoniumCoreAuthnException TOKEN_EXPIRED =
            create("PR400-AN-0010", Error.INVALID_GRANT);
    /**
     * Error in signature verification.
     */
    public static final PersoniumCoreAuthnException TOKEN_DSIG_INVALID =
            create("PR400-AN-0011", Error.INVALID_GRANT);
    /**
     * The target of the token is not yourself.
     * {0}: target URL of token
     */
    public static final PersoniumCoreAuthnException TOKEN_TARGET_WRONG =
            create("PR400-AN-0012", Error.INVALID_GRANT);
    /**
     * It is not a refresh token.
     */
    public static final PersoniumCoreAuthnException NOT_REFRESH_TOKEN =
            create("PR400-AN-0013", Error.INVALID_GRANT);
    /**
     * I can not be promoted because I do not have permission.
     */
    public static final PersoniumCoreAuthnException NOT_ALLOWED_REPRESENT_OWNER =
            create("PR400-AN-0014", Error.INVALID_GRANT);
    /**
     * A cell without an owner can not be promoted.
     */
    public static final PersoniumCoreAuthnException NO_CELL_OWNER =
            create("PR400-AN-0015", Error.INVALID_GRANT);
    /**
     * There is no required parameter.
     * {0}: Parameter key name
     */
    public static final PersoniumCoreAuthnException REQUIRED_PARAM_MISSING =
            create("PR400-AN-0016", Error.INVALID_REQUEST);
    /**
     * Authentication error.
     */
    public static final PersoniumCoreAuthnException AUTHN_FAILED =
            create("PR400-AN-0017", Error.INVALID_GRANT);
    /**
     * Invalid specification of authentication header.
     */
    public static final PersoniumCoreAuthnException AUTH_HEADER_IS_INVALID =
            create("PR400-AN-0018", Error.INVALID_CLIENT);
    /**
     * Invalid Grant Code.
     */
    public static final PersoniumCoreAuthnException INVALID_GRANT_CODE =
            create("PR400-AN-0019", Error.INVALID_GRANT);
    /**
     * Authenticated Client does not match the refresh token.
     */
    public static final PersoniumCoreAuthnException CLIENT_MISMATCH =
            create("PR401-AN-0020", Error.INVALID_CLIENT);
    /**
     * Client auth required to refresh the token.
     */
    public static final PersoniumCoreAuthnException CLIENT_AUTH_REQUIRED =
            create("PR401-AN-0021", Error.INVALID_CLIENT);
    /**
     * Invalid assertion type parameter.
     */
    public static final PersoniumCoreAuthnException INVALID_CLIENT_ASSERTION_TYPE =
            create("PR400-AN-0022", Error.INVALID_CLIENT);
    /**
     * Password change required.
     */
    public static final PersoniumCoreAuthnException PASSWORD_CHANGE_REQUIRED =
            create("PR401-AN-0023", Error.INVALID_GRANT);
    
    /**
     * NetWork related error.
     */
    public static final PersoniumCoreAuthnException NETWORK_ERROR =
            create("PR500-NW-0000", Error.SERVER_ERROR);
    /**
     * HTTP request failed.
     */
    public static final PersoniumCoreAuthnException HTTP_REQUEST_FAILED =
            create("PR500-NW-0001", Error.SERVER_ERROR);
    /**
     * The connection destination returns an unexpected response.
     */
    public static final PersoniumCoreAuthnException UNEXPECTED_RESPONSE =
            create("PR500-NW-0002", Error.SERVER_ERROR);
    /**
     * The connection destination returns an unexpected value.
     */
    public static final PersoniumCoreAuthnException UNEXPECTED_VALUE =
            create("PR500-NW-0003", Error.SERVER_ERROR);

    /**
     * Force load inner class.
     */
    public static void loadConfig() {
    }

    String error;
    String realm;
    private Map<String, Object> errorJsonOptionParams = new HashMap<>();

    /**
     * constructor.
     * @param status HTTP response status
     * @param severity error level
     * @param code error code
     * @param message error message
     * @param error Error code of OAuth authentication error
     * @param realm To return the WWWW-Authenticate header, set the realm value here
     * @param cause Causing Throwable
     */
    PersoniumCoreAuthnException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final String error,
            final String realm, final Throwable cause) {
        super(code, severity, message, status, cause);
        this.error = error;
        this.realm = realm;
    }
    PersoniumCoreAuthnException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final String error,
            final String realm) {
        this(code, severity, message, status, error, realm, null);
    }

    /**
     * Set realm and create object.
     * @param realm2set realm
     * @return CoreAuthnException
     */
    public PersoniumCoreAuthnException realm(String realm2set) {
        //Make a clone
        return new PersoniumCoreAuthnException(this.code, this.severity, this.message, this.status,
                this.error, realm2set);
    }

    /**
     * add option param.
     * @param key key
     * @param value value
     */
    public void addErrorJsonParam(String key, Object value) {
        this.errorJsonOptionParams.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response createResponse() {
        JSONObject errorJson = new JSONObject();

        errorJson.put(Key.ERROR, this.error);

        String errDesc = String.format("[%s] - %s", this.code, this.message);
        errorJson.put(Key.ERROR_DESCRIPTION, errDesc);

        if (!this.errorJsonOptionParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : this.errorJsonOptionParams.entrySet()) {
                errorJson.put(entry.getKey(), entry.getValue());
            }
        }

        int statusCode = parseCode(this.code);
        ResponseBuilder rb = Response.status(statusCode)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(errorJson.toJSONString());

        //If the realm value is set, the WWW-Authenticate header is returned.
        //On the __token endpoint, since Auth Scheme returns the Basic value to the same header at the time of authentication failure (401 return), it is assumed to be a fixed value here.
        if (this.realm != null && statusCode == HttpStatus.SC_UNAUTHORIZED) {
            rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
        }
        return rb.build();
    }

    /**
     * Cause Create and return an exception added.
     * @param t cause exception
     * @return PersoniumCoreException
     */
    public PersoniumCoreException reason(final Throwable t) {
        //Make a clone
        PersoniumCoreException ret = new PersoniumCoreAuthnException(
                this.code, this.severity, this.message, this.status, this.error, this.realm, t);
        //Set stack trace
        ret.setStackTrace(t.getStackTrace());
        return ret;
    }

    /**
     * Factory method.
     * @param code Personium message code
     * @param error OAuth 2 error code
     * @return PersoniumCoreException
     */
    public static PersoniumCoreAuthnException create(String code, String error) {
        int statusCode = PersoniumCoreException.parseCode(code);

        //Acquire log level
        Severity severity = PersoniumCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            //If the log level is not set, it is automatically judged from the response code.
            severity = decideSeverity(statusCode);
        }

        //Obtaining log messages
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumCoreAuthnException(code, severity, message, statusCode, error, null);
    }

    /**
     * Factory method.
     * @param authPluginException AuthPluginException
     * @return PersoniumCoreAuthnException
     */
    public static PersoniumCoreAuthnException create(AuthPluginException authPluginException) {
        int statusCode = authPluginException.getStatusCode();
        Severity severity = decideSeverity(statusCode);
        StringBuilder builder = new StringBuilder();
        builder.append("PR").append(statusCode).append("-PA-0001");
        String errorCode = builder.toString();
        String message = PersoniumCoreMessageUtils.getMessage(errorCode);
        message = MessageFormat.format(message, authPluginException.getMessage());
        String oAuthError = authPluginException.getOAuthError();
        return new PersoniumCoreAuthnException(errorCode, severity, message, statusCode, oAuthError, null);
    }

    /**
     * It creates and returns a message with parameter substitution, and the expression such as $ 1 $ 2 on the error message is a keyword for parameter substitution.
     * @param params Additional message
     * @return PersoniumCoreMessage
     */
    @Override
    public PersoniumCoreAuthnException params(final Object... params) {
        //Replacement message creation
        String ms = MessageFormat.format(this.message, params);

        //Escape processing of control code
        ms = EscapeControlCode.escape(ms);

        //Create a message replacement clone
        PersoniumCoreAuthnException ret = new PersoniumCoreAuthnException(
                 this.code, this.severity, ms, this.status, this.error, this.realm);
        return ret;
    }
}

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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;

import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.OAuth2Helper.Scheme;
import io.personium.core.exceptions.ODataErrorMessage;
import io.personium.plugin.base.PluginMessageUtils.Severity;

/**
 * Log output class when authentication error (PR401 - AU - xxxx) occurs.
 */
@SuppressWarnings("serial")
public final class PersoniumCoreAuthzException extends PersoniumCoreException {

    /**
     * Authentication header is required.
     */
    public static final PersoniumCoreAuthzException AUTHORIZATION_REQUIRED = create("PR401-AU-0001");
    /**
     * The token expired.
     */
    public static final PersoniumCoreAuthzException EXPIRED_ACCESS_TOKEN = create("PR401-AU-0002");
    /**
     * AuthenticationScheme is invalid.
     */
    public static final PersoniumCoreAuthzException INVALID_AUTHN_SCHEME = create("PR401-AU-0003");
    /**
     * The format of basic authentication header is invalid.
     */
    public static final PersoniumCoreAuthzException BASIC_AUTH_FORMAT_ERROR = create("PR401-AU-0004");

    /**
     * Token parsing error.
     */
    public static final PersoniumCoreAuthzException TOKEN_PARSE_ERROR = create("PR401-AU-0006");
    /**
     * Access with refresh token.
     */
    public static final PersoniumCoreAuthzException ACCESS_WITH_REFRESH_TOKEN = create("PR401-AU-0007");
    /**
     * Token signature validation error.
     */
    public static final PersoniumCoreAuthzException TOKEN_DISG_ERROR = create("PR401-AU-0008");
    /**
     * Cookie authentication error.
     */
    public static final PersoniumCoreAuthzException COOKIE_AUTHENTICATION_FAILED = create("PR401-AU-0009");

    /**
     * Basic authentication error (Account locked).
     */
    public static final PersoniumCoreAuthzException BASIC_AUTHENTICATION_FAILED_IN_ACCOUNT_LOCK =
            create("PR401-AU-0010");

    /**
     * Basic authentication error.
     */
    public static final PersoniumCoreAuthzException BASIC_AUTHENTICATION_FAILED = create("PR401-AU-0011");

    /**
     * Access with password change token.
     */
    public static final PersoniumCoreAuthzException ACCESS_WITH_PASSWORD_CHANGE_ACCESS_TOKEN = create("PR401-AU-0012");

    /**
     * Force load inner class.
     */
    public static void loadConfig() {
    }

    String realm;
    AcceptableAuthScheme authScheme = AcceptableAuthScheme.ALL; //Make the setting to allow Basic / Bearer as default

    /**
     * constructor.
     * @param status HTTP response status
     * @param severity error level
     * @param code error code
     * @param message error message
     * @param error Error code of OAuth authentication error
     * @param realm To return the WWWW-Authenticate header, set the realm value here
     * @param authScheme AuthScheme type to allow authentication
     */
    PersoniumCoreAuthzException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final String realm,
            final AcceptableAuthScheme authScheme) {
        super(code, severity, message, status);
        this.realm = realm;
        this.authScheme = authScheme;
    }

    /**
     * Set realm and create object.
     * @param realm2set realm
     * @return CoreAuthnException
     */
    public PersoniumCoreAuthzException realm(String realm2set) {
        //Make a clone
        return new PersoniumCoreAuthzException(this.code, this.severity, this.message, this.status, realm2set,
                AcceptableAuthScheme.ALL);
    }

    /**
     * Set realm and create object.
     * @param realm2set realm
     * @param acceptableAuthScheme AuthScheme type to allow authentication
     * @return CoreAuthnException
     */
    public PersoniumCoreAuthzException realm(String realm2set, AcceptableAuthScheme acceptableAuthScheme) {
        //Make a clone
        return new PersoniumCoreAuthzException(this.code, this.severity,
                this.message, this.status, realm2set, acceptableAuthScheme);
    }

    @Override
    public Response createResponse() {
        ResponseBuilder rb = Response.status(HttpStatus.SC_UNAUTHORIZED)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(new ODataErrorMessage(code, message));

        //If the realm value is set, the WWW-Authenticate header is returned.
        if (null != this.realm) {
            switch (this.authScheme) {
            case BEARER:
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BEARER + " realm=\"" + this.realm + "\"");
                break;
            case BASIC:
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
                break;
            default: //As default, return both Bearer / Basic.
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BEARER + " realm=\"" + this.realm + "\"");
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
                break;
            }
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
        PersoniumCoreException ret = new PersoniumCoreAuthzException(
                this.code, this.severity, this.message, this.status, this.realm, this.authScheme);
        //Set stack trace
        ret.setStackTrace(t.getStackTrace());
        return ret;
    }

    /**
     * Factory method.
     * @param code Personium message code
     * @return PersoniumCoreException
     */
    public static PersoniumCoreAuthzException create(String code) {
        int statusCode = PersoniumCoreException.parseCode(code);

        //Acquire log level
        Severity severity = PersoniumCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            //If the log level is not set, it is automatically judged from the response code.
            severity = decideSeverity(statusCode);
        }

        //Obtaining log messages
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumCoreAuthzException(code, severity, message, statusCode, null, AcceptableAuthScheme.ALL);
    }
}

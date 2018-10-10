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
package io.personium.core.rs.cell;

import static io.personium.common.auth.token.AbstractOAuth2Token.MILLISECS_IN_AN_HOUR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.CellLocalRefreshToken;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.IRefreshToken;
import io.personium.common.auth.token.LocalToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.FacadeResource;
import io.personium.core.utils.ResourceUtils;

/**
 * ImplicitFlow JAX-RS resource responsible for authentication processing.
 */
public class AuthzEndPointResource {

    private static final int COOKIE_MAX_AGE = 86400;

    private static final String PROFILE_JSON_NAME = "/profile.json";

    /**
     * log.
     */
    static Logger log = LoggerFactory.getLogger(AuthzEndPointResource.class);

    private final Cell cell;
    private final CellRsCmp cellRsCmp;

    /**
     * Login form _ Javascript source file.
     */
    private final String jsFileName = "ajax.js";

    /**
     * Login form _ Initial display message.
     */
    private final String passFormMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0002");

    /**
     * Login form _ User ID/Password not yet entered.
     */
    private final String noIdPassMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0003");

    /**
     * Message when cookie authentication failed.
     */
    private final String missCookieMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0005");

    /**
     * The UUID of the Account used for password authentication. It is used to update the last login time after password authentication.
     */
    private String accountId;

    /**
     * constructor.
     * @param cell Cell
     * @param cellRsCmp cellRsCmp
     */
    public AuthzEndPointResource(final Cell cell, final CellRsCmp cellRsCmp) {
        this.cell = cell;
        this.cellRsCmp = cellRsCmp;
    }

    /**
     * Authentication endpoint. <H2> Issuance of token </ h2>
     * <ul>
     * <li> If URL is written in p_target, issue transCellToken as CELL of TARGET as its CELL. </ li>
     * </ul>
     * @param authzHeader Authorization header
     * @param pTarget query parameter
     * @param pOwner query parameter
     * @param assertion query parameter
     * @param clientId query parameter
     * @param responseType query parameter
     * @param redirectUri query parameter
     * @param host Host header
     * @param pCookie p_cookie
     * @param cookieRefreshToken cookie
     * @param keepLogin query parameter
     * @param state query parameter
     * @param isCancel Cancel flag
     * @param uriInfo context
     * @return JAX-RS Response Object
     */
    @GET
    public final Response authGet(@HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader,
            @QueryParam(Key.TARGET) final String pTarget,
            @QueryParam(Key.OWNER) final String pOwner,
            @QueryParam(Key.ASSERTION) final String assertion,
            @QueryParam(Key.CLIENT_ID) final String clientId,
            @QueryParam(Key.RESPONSE_TYPE) final String responseType,
            @QueryParam(Key.REDIRECT_URI) final String redirectUri,
            @HeaderParam(HttpHeaders.HOST) final String host,
            @CookieParam(FacadeResource.P_COOKIE_KEY) final String pCookie,
            @CookieParam(Key.SESSION_ID) final String cookieRefreshToken,
            @QueryParam(Key.KEEPLOGIN) final String keepLogin,
            @QueryParam(Key.STATE) final String state,
            @QueryParam(Key.CANCEL_FLG) final String isCancel,
            @Context final UriInfo uriInfo) {

        return auth(pOwner, null, null, pTarget, assertion, clientId, responseType, redirectUri, host,
                pCookie, cookieRefreshToken, keepLogin, state, isCancel, uriInfo);

    }

    /**
     * Authentication endpoint. <H2> Issuance of token </ h2>
     * <ul>
     * <li> If URL is written in p_target, issue transCellToken as CELL of TARGET as its CELL. </ li>
     * </ul>
     * @param authzHeader Authorization header
     * @param host Host header
     * @param pCookie p_cookie
     * @param cookieRefreshToken cookie
     * @param formParams Body parameters
     * @param uriInfo context
     * @return JAX-RS Response Object
     */
    @POST
    public final Response authPost(@HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader,  // CHECKSTYLE IGNORE
            @HeaderParam(HttpHeaders.HOST) final String host,
            @CookieParam(FacadeResource.P_COOKIE_KEY) final String pCookie,
            @CookieParam(Key.SESSION_ID) final String cookieRefreshToken,
            MultivaluedMap<String, String> formParams,
            @Context final UriInfo uriInfo) {
        // Using @FormParam will cause a closed error on the library side in case of an incorrect body.
        // Since we can not catch Exception, retrieve the value after receiving it with MultivaluedMap.
        String pOwner = formParams.getFirst(Key.OWNER);
        String username = formParams.getFirst(Key.USERNAME);
        String password = formParams.getFirst(Key.PASSWORD);
        String pTarget = formParams.getFirst(Key.TARGET);
        String assertion = formParams.getFirst(Key.ASSERTION);
        String clientId = formParams.getFirst(Key.CLIENT_ID);
        String responseType = formParams.getFirst(Key.RESPONSE_TYPE);
        String redirectUri = formParams.getFirst(Key.REDIRECT_URI);
        String keepLogin = formParams.getFirst(Key.KEEPLOGIN);
        String state = formParams.getFirst(Key.STATE);
        String isCancel = formParams.getFirst(Key.CANCEL_FLG);

        return auth(pOwner, username, password, pTarget, assertion, clientId, responseType, redirectUri, host,
                pCookie, cookieRefreshToken, keepLogin, state, isCancel, uriInfo);
    }

    private Response auth(final String pOwner,
            final String username,
            final String password,
            final String pTarget,
            final String assertion,
            final String clientId,
            final String responseType,
            final String redirectUri,
            final String host,
            final String pCookie,
            final String cookieRefreshToken,
            final String keepLogin,
            final String state,
            final String isCancel,
            final UriInfo uriInfo) {

        String normalizedRedirectUri = redirectUri;
        String normalizedClientId = clientId;
        if (redirectUri == null || "".equals(redirectUri)) {
            return this.returnErrorRedirect(cell.getUrl() + "__html/error", "PR400-AZ-0003");
        } else {
            //Presence / absence of trailing "/"
            if (!redirectUri.endsWith("/")) {
                normalizedRedirectUri = redirectUri + "/";
            }
        }
        if (clientId == null || "".equals(clientId)) {
            return this.returnErrorRedirect(cell.getUrl() + "__html/error", "PR400-AZ-0002");
        } else {
            if (!clientId.endsWith("/")) {
                normalizedClientId = clientId + "/";
            }
        }

        //Authorization processing
        //Check if there is a Box with the cell URL specified by clientId in the schema
        //
        if (!checkAuthorization(normalizedClientId)) {
            log.debug(PersoniumCoreMessageUtils.getMessage("PS-ER-0003"));
            return this.returnErrorRedirect(cell.getUrl() + "__html/error", "PS-ER-0003");
        }

        //clientId and redirectUri parameter check
        try {
            this.checkImplicitParam(normalizedClientId, normalizedRedirectUri, uriInfo.getBaseUri());
        } catch (PersoniumCoreException e) {
            log.debug(e.getMessage());
            if ((username == null && password == null) //NOPMD -To maintain readability
                    && (assertion == null || "".equals(assertion))
                    && cookieRefreshToken == null) {
                //If user ID, password, assertion, cookie are not specified, send form
                throw e;
            } else {
                return this.returnErrorRedirect(cell.getUrl() + "__html/error", e.getCode());
            }
        }

        if ("1".equals(isCancel)) {
            //Redirect to redirect_uri
            return this.returnErrorRedirect(redirectUri, OAuth2Helper.Error.UNAUTHORIZED_CLIENT,
                    PersoniumCoreMessageUtils.getMessage("PR401-AZ-0001"), state, "PR401-AZ-0001");
        }

        String schema = clientId;

        //Check value of response_Type
        if (responseType == null) {
            //Redirect to redirect_uri
            return this.returnErrorRedirect(redirectUri, OAuth2Helper.Error.INVALID_REQUEST,
                    OAuth2Helper.Error.INVALID_REQUEST, state, "PR400-AZ-0004");
        } else if (OAuth2Helper.ResponseType.TOKEN.equals(responseType)) {
            return this.handleImplicitFlow(redirectUri, clientId, host, username, password, cookieRefreshToken,
                    pTarget, keepLogin, assertion, schema, state, pOwner);
        } else if (OAuth2Helper.ResponseType.CODE.equals(responseType)) {
            return handleCodeFlow(redirectUri, clientId, host, username, password, pCookie,
                    pTarget, state, pOwner, uriInfo);
        } else {
            return this.returnErrorRedirect(redirectUri, OAuth2Helper.Error.UNSUPPORTED_RESPONSE_TYPE,
                    OAuth2Helper.Error.UNSUPPORTED_RESPONSE_TYPE, state, "PR400-AZ-0001");
        }
    }

    //TODO CodeFlow association is temporary implementation
    private Response handleCodeFlow(
            final String redirectUriStr,
            final String clientId,
            final String host,
            final String username,
            final String password,
            final String pCookie,
//            final String cookieRefreshToken,
            final String pTarget,
//            final String keepLogin,
//            final String assertion,
//            final String schema,
            final String state,
            final String pOwner,
            UriInfo uriInfo) {
        //If p_target is not a URL, it creates a vulnerability of header injection. (Such as a line feed code is included)
        try {
            this.checkPTarget(pTarget);
        } catch (PersoniumCoreAuthnException e) {
            return this.returnErrorRedirectCodeGrant(redirectUriStr, OAuth2Helper.Error.INVALID_REQUEST,
                    e.getMessage(), state, "code");
        }

        //Password authentication / Transcel token authentication / Cookie authentication separation
        if (username != null || password != null) {
            //TODO Return error because it is not yet implemented
            return this.returnErrorRedirectCodeGrant(redirectUriStr, OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE,
                    OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE, state, "PR400-AZ-0007");
         //Do you need TODO?
//        } else if (cookieRefreshToken != null) {
//// if cookie is specified
//// For cookie authentication, keepLogin always works as true
//            return handleCookieRefreshToken(redirectUriStr, clientId, host,
//                    cookieRefreshToken, OAuth2Helper.Key.TRUE_STR, state, pOwner);
        } else if (pCookie != null) {
            return handlePCookie(redirectUriStr, clientId, host,
                    pCookie, OAuth2Helper.Key.TRUE_STR, state, pOwner, uriInfo);
        } else {
            //TODO Return error because it is not yet implemented
//// If user ID, password, assertion, cookie are not specified, send form
//            ResponseBuilder rb = Response.ok().type(MediaType.TEXT_HTML);
//                    return rb.entity(createForm(clientId, redirectUriStr, passFormMsg, state,
//            OAuth2Helper.ResponseType.CODE, pTarget, pOwner))
//                    .header("Content-Type", "text/html; charset=UTF-8").build();
            return this.returnErrorRedirectCodeGrant(redirectUriStr, OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE,
                    OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE, state, "PR400-AZ-0007");
        }
    }

    private Response handlePCookie(final String redirectUriStr,
            final String clientId,
            final String host,
            final String pCookie,
            final String keepLogin,
            final String state,
            final String pOwner,
            UriInfo uriInfo) {
        //Cookie authentication
        //Get decrypted value of cookie value
        CellLocalRefreshToken rToken;
        CellLocalAccessToken aToken;
        try {
            String decodedCookieValue = LocalToken.decode(pCookie,
                    UnitLocalUnitUserToken.getIvBytes(
                            AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost())));
            int separatorIndex = decodedCookieValue.indexOf("\t");
            //Obtain authorizationHeader equivalent token from information in cookie
            String authToken = decodedCookieValue.substring(separatorIndex + 1);

            AbstractOAuth2Token token = AbstractOAuth2Token.parse(authToken, cell.getUrl(), host);

            if (!(token instanceof IAccessToken)) {
                return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
            }

            //Checking the validity of tokens
            if (token.isExpired()) {
                return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
            }

            long issuedAt = new Date().getTime();

            rToken = new CellLocalRefreshToken(issuedAt, token.getIssuer(), token.getSubject(), clientId);
            //Regenerate AccessToken from received Token
            List<Role> roleList = cell.getRoleListForAccount(token.getSubject());
            aToken = new CellLocalAccessToken(issuedAt, token.getIssuer(), token.getSubject(), roleList, clientId);
        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        }
        //Cookie authentication successful
        //Respond with 303 and return Location header
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toCodeString(),
                    rToken.toTokenString(), keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        }
    }

    private Response returnSuccessRedirect(String redirectUriStr, String code,
            String refreshToken, String keepLogin, String state) throws MalformedURLException {
        //Respond with 302 and return the Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUriStr + getConnectionCode(redirectUriStr)
                + "code" + "=" + code
                + "&" + OAuth2Helper.Key.STATE + "=" + state);
        //Returning the response

        //Return a cookie that is valid only in the cell to be authenticated
        URL cellUrl = new URL(cell.getUrl());
        NewCookie cookies = null;
        Cookie cookie = new Cookie(OAuth2Helper.Key.SESSION_ID, refreshToken, cellUrl.getPath(), null);
        if (code != null) {
            //Create a cookie that can be used only with the same SSL as the expiration date of the refresh token
            //Only when the execution environment is https, set the secure flag
            if (OAuth2Helper.Key.TRUE_STR.equals(keepLogin)) {
                //Set cookie expiration time to 24 hours
                cookies = new NewCookie(cookie, "", COOKIE_MAX_AGE, PersoniumUnitConfig.isHttps());
            } else {
                //Do not set cookie expiration date
                cookies = new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps());
            }
        } else {
            cookies = new NewCookie(cookie, "", 0, PersoniumUnitConfig.isHttps());
        }
        return rb.entity("").cookie(cookies).build();
    }

    private Response returnErrorRedirectCodeGrant(String redirectUri, String error,
            String errorDesp, String state, String code) {
        //Respond with 303 and return Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        //URL encode the fragment information to be added to the Location header
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(redirectUri)
            .append(getConnectionCode(redirectUri))
            .append(OAuth2Helper.Key.ERROR)
            .append("=");
        try {
            sbuf.append(URLEncoder.encode(error, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
            sbuf.append(URLEncoder.encode(errorDesp, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
            sbuf.append(URLEncoder.encode(state, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
            sbuf.append(URLEncoder.encode(code, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            //Since the encoding type is fixed and set to utf-8, it is impossible to come here
            log.warn("Failed to URLencode, fragmentInfo of Location header.");
        }
        rb.header(HttpHeaders.LOCATION, sbuf.toString());
        //Returning the response
        return rb.entity("").build();
    }

    private Response returnErrorMessageCodeGrant(String clientId, String redirectUriStr, String massage,
            String state, String pTarget, String pOwner) {
        ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
        return rb.entity(this.createForm(clientId, redirectUriStr, massage, state,
                OAuth2Helper.ResponseType.CODE, pTarget, pOwner)).build();
    }

    private void checkPTarget(final String pTarget) {
        String target = pTarget;
        if (target != null && !"".equals(pTarget)) {
            try {
                new URL(target);
                if (!target.endsWith("/")) {
                    target = target + "/";
                }
                if (target.contains("\n") || target.contains("\r")) {
                    //Error when p_target is not a URL
                    throw PersoniumCoreAuthnException.INVALID_TARGET;
                }
            } catch (MalformedURLException e) {
                //Error when p_target is not a URL
                throw PersoniumCoreAuthnException.INVALID_TARGET;
            }
        }
    }

    /**
     * ImplicitFlow Password authentication form.
     * @param clientId clientId
     * @param redirectUriStr redirectUriStr
     * @param message String to be output to message display area
     * @param state state
     * @param dcTraget dcTraget
     * @param pOwner pOwner
     * @return HTML
     */
    private String createForm(String clientId, String redirectUriStr, String message, String state,
            String responseType, String pTarget, String pOwner) {

        try {
            HttpResponse response = cellRsCmp.requestGetAuthorizationHtml();
            StringBuilder builder = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), CharEncoding.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }
            }
            return builder.toString();
        } catch (PersoniumCoreException | IOException e) {
            // If processing fails, return system default html.
            List<Object> paramsList = new ArrayList<Object>();

            //Presence / absence of trailing "/"
            if (!"".equals(clientId) && !clientId.endsWith("/")) {
                clientId = clientId + "/";
            }

            //title
            paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
            //Ansel's profile.json
            paramsList.add(clientId + Box.DEFAULT_BOX_NAME + PROFILE_JSON_NAME);
            //Data cell profile.json
            paramsList.add(cell.getUrl() + Box.DEFAULT_BOX_NAME + PROFILE_JSON_NAME);
            //title
            paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
            //Callee
            paramsList.add(cell.getUrl() + "__authz");
            //Message display area
            paramsList.add(message);
            //hidden item
            paramsList.add(state);
            paramsList.add(responseType);
            paramsList.add(pTarget != null ? pTarget : ""); // CHECKSTYLE IGNORE
            paramsList.add(pOwner != null ? pOwner : ""); // CHECKSTYLE IGNORE
            paramsList.add(clientId);
            paramsList.add(redirectUriStr);
            paramsList.add(AuthResourceUtils.getJavascript(jsFileName));

            Object[] params = paramsList.toArray();

            String html = PersoniumCoreUtils.readStringResource("html/authform.html", CharEncoding.UTF_8);
            html = MessageFormat.format(html, params);

            return html;
        }
    }

    /**
     * Password authentication processing at ImplicitFlow.
     * @param pTarget
     * @param redirectUriStr
     * @param clientId
     * @param username
     * @param password
     * @param keepLogin
     * @param state
     * @param pOwner
     * @param host
     * @return
     */
    private Response handleImplicitFlowPassWord(final String pTarget,
            final String redirectUriStr,
            final String clientId,
            final String username,
            final String password,
            final String keepLogin,
            final String state,
            final String pOwner,
            final String host) {

        //If both user ID and password are unspecified, return login error
        boolean passCheck = true;
        if (username == null || password == null || "".equals(username) || "".equals(password)) {
            ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
            return rb.entity(this.createForm(clientId, redirectUriStr, noIdPassMsg, state,
                    OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
        }

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            String resCode = "PS-AU-0004";
            String missIdPassMsg = PersoniumCoreMessageUtils.getMessage(resCode);
            log.info("MessageCode : " + resCode);
            log.info("responseMessage : " + missIdPassMsg);
            ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
            return rb.entity(this.createForm(clientId, redirectUriStr, missIdPassMsg, state,
                    OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
        }
        //In order to update the last login time, keep UUID in class variable
        accountId = (String) oew.getUuid();

        //Check lock
        Boolean isLock = true;
        try {
            isLock = AuthResourceUtils.isLockedAccount(accountId);
            if (isLock) {
                //Update lock time of memcached
                AuthResourceUtils.registAccountLock(accountId);
                String resCode = "PS-AU-0006";
                String accountLockMsg = PersoniumCoreMessageUtils.getMessage(resCode);
                log.info("MessageCode : " + resCode);
                log.info("responseMessage : " + accountLockMsg);
                ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
                return rb.entity(this.createForm(clientId, redirectUriStr, accountLockMsg, state,
                        OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
            }

            //Check user ID and password
            passCheck = cell.authenticateAccount(oew, password);
            if (!passCheck) {
                //Make lock on memcached
                AuthResourceUtils.registAccountLock(accountId);
                String resCode = "PS-AU-0004";
                String missIdPassMsg = PersoniumCoreMessageUtils.getMessage(resCode);
                log.info("MessageCode : " + resCode);
                log.info("responseMessage : " + missIdPassMsg);
                ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
                return rb.entity(this.createForm(clientId, redirectUriStr, missIdPassMsg, state,
                        OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
            }
        } catch (PersoniumCoreException e) {
            return this.returnErrorRedirect(redirectUriStr, e.getMessage(),
                    e.getMessage(), state, e.getCode());
        }

        long issuedAt = new Date().getTime();
        String schema = clientId;

        AbstractOAuth2Token localToken = null;

        if (Key.TRUE_STR.equals(pOwner)) {
            //Check unit escalation privilege setting
            if (!this.cellRsCmp.checkOwnerRepresentativeAccounts(username)) {
                return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, pOwner);
            }
            //Do not promote cells for which the owner of the cell is not set.
            if (cell.getOwner() == null) {
                return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, pOwner);
            }

            //uluut issuance processing
            localToken = new UnitLocalUnitUserToken(
                    issuedAt, UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                    cell.getOwner(), host);

        }

        //Generate Refresh Token
        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt,
                CellLocalRefreshToken.REFRESH_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getUrl(), username, schema);
        //Respond with 303 and return Location header
        try {
            if (localToken != null) {
                //Returning ULUUT
                UnitLocalUnitUserToken aToken = (UnitLocalUnitUserToken) localToken;
                return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                        null, null, state);
            } else {
                //Create a response.
                if (pTarget == null || "".equals(pTarget)) {
                    //Returning cell local token
                    AccountAccessToken aToken = new AccountAccessToken(issuedAt,
                            AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR, cell.getUrl(),
                            username, schema);
                    return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                            rToken.toTokenString(), keepLogin, state);
                } else {
                    //Returning transcell token
                    List<Role> roleList = cell.getRoleListForAccount(username);
                    TransCellAccessToken tcToken = new TransCellAccessToken(cell.getUrl(),
                            cell.getUrl() + "#" + username, pTarget, roleList, schema);
                    return returnSuccessRedirect(redirectUriStr, tcToken.toTokenString(), tcToken.expiresIn(),
                            rToken.toTokenString(), keepLogin, state);
                }
            }
        } catch (MalformedURLException e) {
            return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, pOwner);
        }
    }

    /**
     * Transcel token authentication processing at ImplicitFlow.
     * @param redirectUriStr
     * @param clientId
     * @param cookieRefreshToken
     * @param pTarget
     * @param keepLogin
     * @return
     */
    private Response handleImplicitFlowTcToken(final String redirectUriStr,
            final String clientId,
            final String assertion,
            final String pTarget,
            final String keepLogin,
            final String schema,
            final String state) {

        //First to parse
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(assertion);
        } catch (TokenParseException e) {
            //When parsing fails
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        } catch (TokenDsigException e) {
            //Error in signature verification
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        }

        //Verification of Token
        //1. Expiration check
        if (tcToken.isExpired()) {
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        }

        //If the target of the token is not yourself, an error response
        try {
            if (!(AuthResourceUtils.checkTargetUrl(this.cell, tcToken))) {
                return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                        OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
            }
        } catch (MalformedURLException e) {
            log.debug(e.getMessage());
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        }

        //Authentication is successful -------------------------------

        long issuedAt = new Date().getTime();

        //Ask CELL to decide the role of you from the role of TC issuer.
        List<Role> rolesHere = cell.getRoleListHere(tcToken);

        String schemaVerified = schema;

        //Authentication token issue processing
        //The target can be freely decided.
        IAccessToken aToken = null;
        if (pTarget == null || "".equals(pTarget)) {
            aToken = new CellLocalAccessToken(issuedAt, cell.getUrl(), tcToken.getSubject(), rolesHere, schemaVerified);
        } else {
            aToken = new TransCellAccessToken(UUID.randomUUID().toString(), issuedAt, cell.getUrl(),
                    tcToken.getSubject(), pTarget, rolesHere, schemaVerified);
        }
        //Successful authentication with transcell token
        //Respond with 303 and return Location header
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                    null, keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, "");
        }
    }

    /**
     * Cookie authentication processing at ImplicitFlow.
     * @param redirectUriStr
     * @param clientId
     * @param host
     * @param cookieRefreshToken
     * @param pTarget
     * @param keepLogin
     * @return
     */
    private Response handleImplicitFlowcookie(final String redirectUriStr,
            final String clientId,
            final String host,
            final String cookieRefreshToken,
            final String pTarget,
            final String keepLogin,
            final String state,
            final String pOwner) {
        IRefreshToken rToken = null;
        IAccessToken aToken = null;
        try {
            AbstractOAuth2Token token = AbstractOAuth2Token.parse(cookieRefreshToken, cell.getUrl(), host);
            if (!(token instanceof IRefreshToken)) {
                return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
            }

            //Refresh token expiration check
            if (token.isRefreshExpired()) {
                return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
            }

            long issuedAt = new Date().getTime();

            if (Key.TRUE_STR.equals(pOwner)) {
                //You can be promoted only for your own cell refresh.
                if (token.getClass() != CellLocalRefreshToken.class) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
                //Check unit escalation privilege setting
                if (!this.cellRsCmp.checkOwnerRepresentativeAccounts(token.getSubject())) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
                //Do not promote cells for which the owner of the cell is not set.
                if (cell.getOwner() == null) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }

                //uluut issuance processing
                UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                        issuedAt, UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                        cell.getOwner(), host);
                //Cookie authentication successful
                //Respond with 303 and return Location header
                try {
                    return returnSuccessRedirect(redirectUriStr, uluut.toTokenString(), uluut.expiresIn(),
                            null, keepLogin, state);
                } catch (MalformedURLException e) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
            } else {
                //Regenerate AccessToken and RefreshToken from received Refresh Token
                rToken = (IRefreshToken) token;
                rToken = rToken.refreshRefreshToken(issuedAt);

                if (rToken instanceof CellLocalRefreshToken) {
                    String subject = rToken.getSubject();
                    List<Role> roleList = cell.getRoleListForAccount(subject);
                    aToken = rToken.refreshAccessToken(issuedAt, pTarget, cell.getUrl(), roleList);
                } else {
                    //Ask CELL to determine the role of you from the role of the token issuer.
                    List<Role> rolesHere = cell.getRoleListHere((IExtRoleContainingToken) rToken);
                    aToken = rToken.refreshAccessToken(issuedAt, pTarget, cell.getUrl(), rolesHere);
                }
            }

        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        }
        //Cookie authentication successful
        //Respond with 303 and return Location header
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                    rToken.toTokenString(), keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        }
    }

    /**
     * Authentication processing handling by ImplicitFlow.
     * @param redirectUriStr
     * @param clientId
     * @param host
     * @param username
     * @param password
     * @param cookieRefreshToken
     * @param pTarget
     * @param keepLogin
     * @param assertion
     * @param state
     * @param pOwner TODO
     * @return
     */
    private Response handleImplicitFlow(
            final String redirectUriStr,
            final String clientId,
            final String host,
            final String username,
            final String password,
            final String cookieRefreshToken,
            final String pTarget,
            final String keepLogin,
            final String assertion,
            final String schema,
            final String state,
            final String pOwner) {

        //If p_target is not a URL, it creates a vulnerability of header injection. (Such as a line feed code is included)
        try {
            this.checkPTarget(pTarget);
        } catch (PersoniumCoreAuthnException e) {
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.INVALID_REQUEST,
                    e.getMessage(), state, "code");
        }
        //TODO box existence check -> In some cases: Return token, if not: Create box (authorization check -> Box import execution)
        //However, it returns an error until Box import is implemented

        //Password authentication / Transcel token authentication / Cookie authentication separation
        if (username != null || password != null) {
            //When there is a setting in either user ID or password
            Response response = this.handleImplicitFlowPassWord(pTarget, redirectUriStr, clientId,
                    username, password, keepLogin, state, pOwner, host);

            if (PersoniumUnitConfig.getAccountLastAuthenticatedEnable()
                    && isSuccessAuthorization(response)) {
                //Obtain schema information of Account
                PersoniumODataProducer producer = ModelFactory.ODataCtl.cellCtl(cell);
                EdmEntitySet esetAccount = producer.getMetadata().getEdmEntitySet(Account.EDM_TYPE_NAME);
                OEntityKey originalKey = OEntityKey.parse("('" + username + "')");
                //Ask Producer to change the last login time (Get / release lock within this method)
                producer.updateLastAuthenticated(esetAccount, originalKey, accountId);
            }
            return response;
        } else if (assertion != null && !"".equals(assertion)) {
            //When assertion is specified
            return this.handleImplicitFlowTcToken(redirectUriStr, clientId, assertion, pTarget, keepLogin, schema,
                    state);
        } else if (cookieRefreshToken != null) {
            //When cookie is specified
            //For cookie authentication, keepLogin always works as true
            return this.handleImplicitFlowcookie(redirectUriStr, clientId, host,
                    cookieRefreshToken, pTarget, OAuth2Helper.Key.TRUE_STR, state, pOwner);
        } else {
            //If user ID, password, assertion, cookie are not specified, send form
            ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
            return rb.entity(this.createForm(clientId, redirectUriStr, passFormMsg, state,
                    OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
        }
    }

    /**
     * It is determined whether password authentication in ImplicitFlow was successful.
     * @param response Authentication response
     * @return true: Authentication success false: Authentication failure
     */
    protected boolean isSuccessAuthorization(Response response) {
        //When the response code is other than 303, it is regarded as an error that the screen transition does not occur
        if (Status.SEE_OTHER.getStatusCode() != response.getStatus()) {
            return false;
        }

        //It checks whether there is error information in the fragment of the URL specified in the Location header
        String locationStr = (String) response.getMetadata().getFirst(HttpHeaders.LOCATION);
        try {
            URI uri = new URI(locationStr);
            String fragment = uri.getFragment();
            //When there is no fragment, it is regarded as an I / F error of the API
            if (null == fragment) {
                return false;
            }
            if (fragment.indexOf(OAuth2Helper.Key.ERROR + "=") >= 0
                    && fragment.indexOf(OAuth2Helper.Key.ERROR_DESCRIPTION + "=") >= 0
                    && fragment.indexOf(OAuth2Helper.Key.STATE + "=") >= 0
                    && fragment.indexOf(OAuth2Helper.Key.CODE + "=") >= 0) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }

    /**
     * After authenticating with ImplicitFlow, execute Redirect.
     * @param redirectUriStr
     * @param localTokenStr
     * @param localTokenExpiresIn
     * @param refreshTokenStr
     * @param keepLogin
     * @param state
     * @return
     * @throws MalformedURLException
     */
    private Response returnSuccessRedirect(String redirectUriStr, String localTokenStr,
            int localTokenExpiresIn, String refreshTokenStr,
            String keepLogin, String state) throws MalformedURLException {
        //Respond with 303 and return Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUriStr + "#"
                + OAuth2Helper.Key.ACCESS_TOKEN + "=" + localTokenStr + "&"
                + OAuth2Helper.Key.TOKEN_TYPE + "="
                + OAuth2Helper.Scheme.BEARER
                + "&" + OAuth2Helper.Key.EXPIRES_IN + "=" + localTokenExpiresIn
                + "&" + OAuth2Helper.Key.STATE + "=" + state);
        //Returning the response

        //Return a cookie that is valid only in the cell to be authenticated
        URL cellUrl = new URL(cell.getUrl());
        NewCookie cookies = null;
        Cookie cookie = new Cookie(OAuth2Helper.Key.SESSION_ID, refreshTokenStr, cellUrl.getPath(), null);
        if (refreshTokenStr != null) {
            //Create a cookie that can be used only with the same SSL as the expiration date of the refresh token
            //Only when the execution environment is https, set the secure flag
            if (OAuth2Helper.Key.TRUE_STR.equals(keepLogin)) {
                //Set cookie expiration time to 24 hours
                cookies = new NewCookie(cookie, "", COOKIE_MAX_AGE, PersoniumUnitConfig.isHttps());
            } else {
                //Do not set cookie expiration date
                cookies = new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps());
            }
        } else {
            cookies = new NewCookie(cookie, "", 0, PersoniumUnitConfig.isHttps());
        }
        return rb.entity("").cookie(cookies).build();
    }

    /**
     * Of the errors during authentication with ImplicitFlow, execute Redirect to redirect_uri set by the user in the following situation. Invalid / unspecified response_type 2
     * @param state
     * @return
     * @throws MalformedURLException
     */
    private Response returnErrorRedirect(String redirectUri, String code) {
        //Respond with 303 and return Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUri + getConnectionCode(redirectUri)
                + OAuth2Helper.Key.CODE + "=" + code);
        //Returning the response
        return rb.entity("").build();
    }

    /**
     * Of the errors during authentication with ImplicitFlow, execute Redirect to redirect_uri set by the user in the following situation. Invalid / unspecified response_type 2
     * @param state
     * @return
     * @throws MalformedURLException
     */
    private Response returnErrorRedirect(String redirectUri, String error,
            String errorDesp, String state, String code) {
        //Respond with 302 and return the Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        //URL encode the fragment information to be added to the Location header
        StringBuilder sbuf = new StringBuilder(redirectUri + "#" + OAuth2Helper.Key.ERROR + "=");
        try {
            sbuf.append(URLEncoder.encode(error, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
            sbuf.append(URLEncoder.encode(errorDesp, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
            sbuf.append(URLEncoder.encode(state, "utf-8"));
            sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
            sbuf.append(URLEncoder.encode(code, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            //Since the encoding type is fixed and set to utf-8, it is impossible to come here
            log.warn("Failed to URLencode, fragmentInfo of Location header.");
        }
        rb.header(HttpHeaders.LOCATION, sbuf.toString());
        //Returning the response
        return rb.entity("").build();
    }

    /**
     * Error handling during authentication with ImplicitFlow_cookie.
     * @param clientId
     * @param redirectUriStr
     * @param state TODO
     * @param massagae
     * @return
     */
    private Response returnErrorMessage(String clientId, String redirectUriStr, String massage,
            String state, String pTarget, String pOwner) {
        ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
        return rb.entity(this.createForm(clientId, redirectUriStr, massage, state,
                OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
    }

    /**
     * Authorization processing It is checked whether there is a Box whose schema is the cell URL specified by clientId.
     * @param clientId App Store URL
     * @return true: authorization success false: authorization failure
     */
    private boolean checkAuthorization(final String clientId) {
        EntitySetAccessor boxAcceccor = EsModel.box(this.cell);

        // {filter={and={filters=[{term={c=$CELL_ID}, {term={s.Schema.untouched=$CLIENT_ID}]}}}
        // {filter={and={filters=[
        //     {term={c=$CELL_ID}},
        //     {or={filters=[
        //         {term={s.Schema.untouched=$CLIENT_ID}},
        //         {term={s.Schema.untouched=$NORMALIZED_CLIENT_ID}}
        //     ]}}
        // ]}}}
        Map<String, Object> query1 = new HashMap<String, Object>();
        Map<String, Object> term1 = new HashMap<String, Object>();
        Map<String, Object> query2 = new HashMap<String, Object>();
        Map<String, Object> term2 = new HashMap<String, Object>();
//        Map<String, Object> query3 = new HashMap<String, Object>();
//        Map<String, Object> term3 = new HashMap<String, Object>();
        List<Map<String, Object>> filtersList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> queriesList = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();

        query1.put("c", this.cell.getId());
        term1.put("term", query1);

        String boxSchemaKey = OEntityDocHandler.KEY_STATIC_FIELDS + "." + Box.P_SCHEMA.getName() + ".untouched";
        query2.put(boxSchemaKey, clientId);
        term2.put("term", query2);

        // TODO Issue-223
//        String normalizedCelientId = UriUtils.convertCellBaseToDomainBase(clientId);
//        query3.put(boxSchemaKey, normalizedCelientId);
//        term3.put("term", query3);

        queriesList.add(term1);
        filtersList.add(term2);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queriesList));

        filters.put("filters", filtersList);
        and.put("and", filters);

        filter.put("filter", and);
        filter.put("query", query);

        long count = boxAcceccor.count(filter);

        if (count <= 0) {
            return false;
        }

        return true;
    }

    /**
     * Parameter check at ImplicitFlow authentication time.
     * @param clientId
     * @param redirectUri
     * @param baseUri
     */
    private void checkImplicitParam(String clientId, String redirectUri, URI baseUri) {
        if (redirectUri == null || clientId == null) {
            //TODO Error if one is null. Message change required
            throw PersoniumCoreAuthnException.INVALID_TARGET;
        }

        URL objClientId = null;
        URL objRedirectUri = null;
        try {
            objClientId = new URL(clientId);
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }
        try {
            objRedirectUri = new URL(redirectUri);
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }

        if (redirectUri.contains("\n") || redirectUri.contains("\r")) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }
        if (clientId.contains("\n") || clientId.contains("\r")) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }

        //Get baseurl's path
        String bPath = baseUri.getPath();

        //Deletes the path of baseUri from the path of client_id and redirect_uri
        String cPath = objClientId.getPath().substring(bPath.length());
        String rPath = objRedirectUri.getPath().substring(bPath.length());

        //split the path of client_id and redirect_uri with /
        String[] cPaths = StringUtils.split(cPath, "/");
        String[] rPaths = StringUtils.split(rPath, "/");

        //Compare client_id and redirect_uri, and if the cells are different, an authentication error
        //Comparison of cell URLs
        if (!objClientId.getAuthority().equals(objRedirectUri.getAuthority())
                || rPaths.length == 0
                || !cPaths[0].equals(rPaths[0])) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }

        //Compare the client_id with the name of the requested cell, and an error if the cells are the same
        if (cPaths.length == 0
                || cPaths[0].equals(this.cell.getName())) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }

    }

    /**
     *
     * @param redirectUriStr
     * @return
     */
    private String getConnectionCode(String redirectUriStr) {
        if (StringUtils.contains(redirectUriStr, "?")) {
            return "&";
        } else {
            return "?";
        }
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST, HttpMethod.GET).build();
    }
}

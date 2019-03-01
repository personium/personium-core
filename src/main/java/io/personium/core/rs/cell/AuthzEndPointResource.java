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

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.IdToken;
import io.personium.common.auth.token.LocalToken;
import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.fs.CellKeysFile;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.FacadeResource;
import io.personium.core.utils.ResourceUtils;

//TODO Refactor the entire class.
/**
 * ImplicitFlow JAX-RS resource responsible for authentication processing.
 */
public class AuthzEndPointResource {
    // core issue #223
    // "issuer" in the token may be interpreted by other units.
    // For that reason, "path based cell url" is set for "issuer" regardless of unit property setting.

    private static final String SEPARATOR_QUERY = "?";
    private static final String SEPARATOR_FRAGMENT = "#";
    // TODO add p_cookie
//    private static final int COOKIE_MAX_AGE = 86400;

    private static final String PROFILE_JSON_NAME = "/profile.json";

    /** log. */
    static Logger log = LoggerFactory.getLogger(AuthzEndPointResource.class);

    private final Cell cell;
    private final CellRsCmp cellRsCmp;
    private String ipaddress;
    private UriInfo requestURIInfo;

    /** Login form _ Javascript source file. */
    private static final String AJAX_FILE_NAME = "ajax.js";
    /** Login form _ Initial display message. */
    private static final String MSG_PASS_FORM = PersoniumCoreMessageUtils.getMessage("PS-AU-0002");
    /** Login form _ User ID/Password not yet entered. */
    private static final String MSG_NO_ID_PASS = PersoniumCoreMessageUtils.getMessage("PS-AU-0003");
    /** Login form User ID or password is incorrect. */
    private static final String MSG_INCORRECT_ID_PASS = PersoniumCoreMessageUtils.getMessage("PS-AU-0004");
    /** Message when cookie authentication failed. */
    private static final String MSG_MISS_COOKIE = PersoniumCoreMessageUtils.getMessage("PS-AU-0005");

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
     * Authorization endpoint. <H2> Issuance of token </ h2>
     * <ul>
     * <li> If URL is written in p_target, issue transCellToken as CELL of TARGET as its CELL. </ li>
     * </ul>
     * @param responseType query parameter
     * @param clientId query parameter
     * @param redirectUri query parameter
     * @param pCookie p_cookie
     * @param state query parameter
     * @param scope query parameter
     * @param keepLogin query parameter
     * @param isCancel Cancel flag
     * @param expiresInStr accress token expires in time(s).
     * @param uriInfo context
     * @param xForwardedFor X-Forwarded-For Header
     * @return JAX-RS Response Object
     */
    @GET
    public final Response authGet(
            @QueryParam(Key.RESPONSE_TYPE) final String responseType,
            @QueryParam(Key.CLIENT_ID) final String clientId,
            @QueryParam(Key.REDIRECT_URI) final String redirectUri,
            @CookieParam(FacadeResource.P_COOKIE_KEY) final String pCookie,
            @QueryParam(Key.STATE) final String state,
            @QueryParam(Key.SCOPE) final String scope,
            @QueryParam(Key.KEEPLOGIN) final String keepLogin,
            @QueryParam(Key.CANCEL_FLG) final String isCancel,
            @QueryParam(Key.EXPIRES_IN) final String expiresInStr,
            @Context final UriInfo uriInfo,
            @HeaderParam("X-Forwarded-For") final String xForwardedFor) {

        return auth(responseType, clientId, redirectUri, null, null,
                pCookie, state, scope, keepLogin, isCancel, expiresInStr, uriInfo, xForwardedFor);
    }

    /**
     * Authorization endpoint. <H2> Issuance of token </ h2>
     * <ul>
     * <li> If URL is written in p_target, issue transCellToken as CELL of TARGET as its CELL. </ li>
     * </ul>
     * @param pCookie p_cookie
     * @param formParams Body parameters
     * @param uriInfo context
     * @param xForwardedFor X-Forwarded-For Header
     * @return JAX-RS Response Object
     */
    @POST
    public final Response authPost(
            @CookieParam(FacadeResource.P_COOKIE_KEY) final String pCookie,
            MultivaluedMap<String, String> formParams,
            @Context final UriInfo uriInfo,
            @HeaderParam("X-Forwarded-For") final String xForwardedFor) {
        // Using @FormParam will cause a closed error on the library side in case of an incorrect body.
        // Since we can not catch Exception, retrieve the value after receiving it with MultivaluedMap.
        String responseType = formParams.getFirst(Key.RESPONSE_TYPE);
        String clientId = formParams.getFirst(Key.CLIENT_ID);
        String redirectUri = formParams.getFirst(Key.REDIRECT_URI);
        String username = formParams.getFirst(Key.USERNAME);
        String password = formParams.getFirst(Key.PASSWORD);
        String state = formParams.getFirst(Key.STATE);
        String scope = formParams.getFirst(Key.SCOPE);
        String keepLogin = formParams.getFirst(Key.KEEPLOGIN);
        String isCancel = formParams.getFirst(Key.CANCEL_FLG);
        String expiresInStr = formParams.getFirst(Key.EXPIRES_IN);

        return auth(responseType, clientId, redirectUri, username, password,
                pCookie, state, scope, keepLogin, isCancel, expiresInStr, uriInfo, xForwardedFor);
    }

    /**
     * Get url of "issuer" to be set to token.
     * @return url of "issuer"
     */
    private String getIssuerUrl() {
        return cell.getUrl();
    }

    /**
     * Authorization process.
     * @param responseType response_type
     * @param clientId client_id
     * @param redirectUri redirect_uri
     * @param username username
     * @param password password
     * @param pCookie p_cookie
     * @param state state
     * @param scope scope
     * @param keepLogin keep_login
     * @param isCancel is_cancel
     * @param expiresInStr accress token expires in time(s).
     * @param uriInfo uri_info
     * @return JAX-RS Response
     */
    private Response auth(
            final String responseType,
            final String clientId,
            final String redirectUri,
            final String username,
            final String password,
            final String pCookie,
            final String state,
            final String scope,
            final String keepLogin,
            final String isCancel,
            final String expiresInStr,
            final UriInfo uriInfo,
            final String xForwardedFor) {

        this.requestURIInfo = uriInfo;
        this.ipaddress = xForwardedFor;

         //clientId and redirectUri parameter check
        try {
            this.validateClientIdAndRedirectUri(clientId, redirectUri);
        } catch (PersoniumCoreException e) {
            log.debug(e.getMessage());
            return returnErrorPageRedirect(e.getCode());
        }

        //Check value of response_Type
        if (StringUtils.isEmpty(responseType)) {
            //Redirect to redirect_uri
            return this.returnErrorRedirect(responseType, redirectUri, OAuth2Helper.Error.INVALID_REQUEST,
                    OAuth2Helper.Error.INVALID_REQUEST, state, "PR400-AZ-0004");
        }
        //Check value of expires_in
        long expiresIn = AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS;
        if (expiresInStr != null && !expiresInStr.isEmpty()) {
            try {
                expiresIn = Integer.parseInt(expiresInStr) * AbstractOAuth2Token.MILLISECS_IN_A_SEC;
                if (expiresIn <= 0 || expiresIn > AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS) {
                    return this.returnErrorRedirect(responseType, redirectUri, OAuth2Helper.Error.INVALID_REQUEST,
                            OAuth2Helper.Error.INVALID_REQUEST, state, "PR400-AZ-0008");
                }
            } catch (NumberFormatException e) {
                return this.returnErrorRedirect(responseType, redirectUri, OAuth2Helper.Error.INVALID_REQUEST,
                        OAuth2Helper.Error.INVALID_REQUEST, state, "PR400-AZ-0008");
            }
        }

        // response_type = token || response_type = code || (response_type = id_token && scope = openid)
        if (!OAuth2Helper.ResponseType.TOKEN.equals(responseType)
                && !OAuth2Helper.ResponseType.CODE.equals(responseType)
                && (!OAuth2Helper.ResponseType.ID_TOKEN.equals(responseType)
                        || OAuth2Helper.ResponseType.ID_TOKEN.equals(responseType)
                        && !OAuth2Helper.Scope.OPENID.equals(scope))) {
            return this.returnErrorRedirect(responseType, redirectUri, OAuth2Helper.Error.UNSUPPORTED_RESPONSE_TYPE,
                    OAuth2Helper.Error.UNSUPPORTED_RESPONSE_TYPE, state, "PR400-AZ-0001");
        }

        if ("1".equals(isCancel)) {
            //Redirect to redirect_uri
            return this.returnErrorRedirect(responseType, redirectUri, OAuth2Helper.Error.UNAUTHORIZED_CLIENT,
                    PersoniumCoreMessageUtils.getMessage("PR401-AZ-0001"), state, "PR401-AZ-0001");
        }

        //Password authentication / Transcel token authentication / Cookie authentication separation
        if (username != null || password != null) {
            //When there is a setting in either user ID or password
            Response response = handlePassword(responseType, clientId, redirectUri,
                    username, password, state, scope, keepLogin, expiresIn);
            return response;
        } else if (pCookie != null) {
            return handlePCookie(responseType, clientId, redirectUri,
                    pCookie, state, scope, keepLogin, expiresIn, uriInfo);
        } else {
            //If user ID, password, cookie are not specified, send form
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_PASS_FORM, state, scope);
        }
    }

    /**
     * Authorization username / password.
     * @param responseType response_type
     * @param clientId client_id
     * @param redirectUri redirect_uri
     * @param username username
     * @param password password
     * @param state state
     * @param scope scope
     * @param keepLogin keep_login
     * @param expiresIn accress token expires in time(ms).
     * @return JAX-RS Response
     */
    private Response handlePassword(String responseType, String clientId, String redirectUri,
            String username, String password, String state, String scope, String keepLogin, long expiresIn) {
        //If both user ID and password are unspecified, return login error
        boolean passCheck = true;
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_NO_ID_PASS, state, scope);
        }

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            log.info("responseMessage : " + MSG_INCORRECT_ID_PASS);
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);
        }
        //In order to update the last login time, keep UUID in class variable
        String accountId = (String) oew.getUuid();

        Boolean isLock = true;
        boolean passwordChangeRequired = false;
        try {
            //Check valid authentication interval
            isLock = AuthResourceUtils.isLockedInterval(accountId);
            if (isLock) {
                //Update lock time of memcached
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                PersoniumCoreLog.Authn.FAILED_BEFORE_AUTHENTICATION_INTERVAL.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(cellRsCmp.getDavCmp().getFsPath(), accountId);
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);
            }

            //Check account lock
            isLock = AuthResourceUtils.isLockedAccount(accountId);
            if (isLock) {
                //Update lock time of memcached
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                PersoniumCoreLog.Authn.FAILED_ACCOUNT_IS_LOCKED.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(cellRsCmp.getDavCmp().getFsPath(), accountId);
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);
            }

            // Check valid IP address
            if (!AuthUtils.isValidIPAddress(oew, this.ipaddress)) {
                //Update lock time of memcached
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                PersoniumCoreLog.Authn.FAILED_OUTSIDE_IP_ADDRESS_RANGE.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(cellRsCmp.getDavCmp().getFsPath(), accountId);
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);

            }

            //Check user ID and password
            passCheck = cell.authenticateAccount(oew, password);
            if (!passCheck) {
                //Make lock on memcached
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                PersoniumCoreLog.Authn.FAILED_INCORRECT_PASSWORD.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(cellRsCmp.getDavCmp().getFsPath(), accountId);
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);
            }

            //Check account status.
            boolean accountActive = AuthUtils.isActive(oew);
            passwordChangeRequired = AuthUtils.isPasswordChangeReuired(oew);
            if (!accountActive && !passwordChangeRequired) {
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                PersoniumCoreLog.Authn.FAILED_ACCOUNT_IS_DEACTIVATED.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(cellRsCmp.getDavCmp().getFsPath(), accountId);
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_INCORRECT_ID_PASS, state, scope);
            }
        } catch (PersoniumCoreException e) {
            return this.returnErrorRedirect(responseType, redirectUri, e.getMessage(),
                    e.getMessage(), state, e.getCode());
        }

        long issuedAt = new Date().getTime();
        String schema = clientId;
        Map<String, String> paramMap = new HashMap<>();

        if (passwordChangeRequired) {
            // Issue password change.
            PasswordChangeAccessToken aToken = new PasswordChangeAccessToken(issuedAt, expiresIn,
                    getIssuerUrl(), username, schema);
            paramMap.put(OAuth2Helper.Key.ACCESS_TOKEN, aToken.toTokenString());
            paramMap.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
            paramMap.put(OAuth2Helper.Key.EXPIRES_IN, String.valueOf(aToken.expiresIn()));
        } else {
            if (OAuth2Helper.ResponseType.TOKEN.equals(responseType)
                    || OAuth2Helper.ResponseType.CODE.equals(responseType)) {
                // TODO add p_cookie
//                //Generate Refresh Token
//                CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt,
//                        CellLocalRefreshToken.REFRESH_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
//                        getIssuerUrl(), username, schema);
                //Respond with 303 and return Location header
                //Returning cell local token
                if (OAuth2Helper.ResponseType.TOKEN.equals(responseType)) {
                    AccountAccessToken aToken = new AccountAccessToken(issuedAt, expiresIn,
                            getIssuerUrl(), username, schema);
                    paramMap.put(OAuth2Helper.Key.ACCESS_TOKEN, aToken.toTokenString());
                    paramMap.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
                    paramMap.put(OAuth2Helper.Key.EXPIRES_IN, String.valueOf(aToken.expiresIn()));
                } else if (OAuth2Helper.ResponseType.CODE.equals(responseType)) {
                    List<Role> roleList = cell.getRoleListForAccount(username);
                    CellLocalAccessToken aToken = new CellLocalAccessToken(issuedAt,
                            CellLocalAccessToken.CODE_EXPIRES, getIssuerUrl(), username, roleList, schema, scope);
                    paramMap.put(OAuth2Helper.Key.CODE, aToken.toCodeString());
                }
            } else {
                CellCmp cellCmp = (CellCmp) cellRsCmp.getDavCmp();
                CellKeysFile cellKeysFile = cellCmp.getCellKeys().getCellKeysFile();
                long issuedAtSec = issuedAt / AbstractOAuth2Token.MILLISECS_IN_A_SEC;
                long expiryTime = issuedAtSec + AbstractOAuth2Token.SECS_IN_A_HOUR;
                IdToken idToken = new IdToken(
                        cellKeysFile.getKeyId(), AlgorithmUtils.RS_SHA_256_ALGO, getIssuerUrl(),
                        username, schema, expiryTime, issuedAtSec, cellKeysFile.getPrivateKey());
                paramMap.put(OAuth2Helper.Key.ID_TOKEN, idToken.toTokenString());
            }

            // get last auth history.
            AuthHistoryLastFile last = AuthResourceUtils.getAuthHistoryLast(
                    cellRsCmp.getDavCmp().getFsPath(), accountId);
            String lastAuthenticated = last.getLastAuthenticated() != null ? last.getLastAuthenticated().toString() : null; //CHECKSTYLE IGNORE
            String failedCount = last.getFailedCount() != null ? last.getFailedCount().toString() : null; //CHECKSTYLE IGNORE
            paramMap.put(OAuth2Helper.Key.LAST_AUTHENTICATED, lastAuthenticated);
            paramMap.put(OAuth2Helper.Key.FAILED_COUNT, failedCount);
        }

        if (StringUtils.isNotEmpty(state)) {
            paramMap.put(OAuth2Helper.Key.STATE, state);
        }

        // update auth history.
        AuthResourceUtils.updateAuthHistoryLastFileWithSuccess(cellRsCmp.getDavCmp().getFsPath(), accountId, issuedAt);
        // release account lock.
        AuthResourceUtils.releaseAccountLock(accountId);

        return returnSuccessRedirect(responseType, redirectUri, paramMap, keepLogin);
    }

    /**
     * Authorization p_cookie.
     * @param responseType response_type
     * @param clientId client_id
     * @param redirectUri redirect_uri
     * @param pCookie p_cookie
     * @param state state
     * @param scope scope
     * @param keepLogin keep_login
     * @param expiresIn accress token expires in time(ms).
     * @param uriInfo UriInfo
     * @return JAX-RS Response
     */
    private Response handlePCookie(String responseType, String clientId, String redirectUri,
            String pCookie, String state, String scope, String keepLogin, long expiresIn, UriInfo uriInfo) {
        //Cookie authentication
        //Get decrypted value of cookie value
        AbstractOAuth2Token token;
        try {
            String decodedCookieValue = LocalToken.decode(pCookie,
                    UnitLocalUnitUserToken.getIvBytes(
                            AccessContext.getCookieCryptKey(uriInfo.getBaseUri().getHost())));
            int separatorIndex = decodedCookieValue.indexOf("\t");
            //Obtain authorizationHeader equivalent token from information in cookie
            String authToken = decodedCookieValue.substring(separatorIndex + 1);

            token = AbstractOAuth2Token.parse(authToken, getIssuerUrl(), cell.getUnitUrl());

            if (!(token instanceof IAccessToken)) {
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_PASS_FORM, state, scope);
            }

            //Checking the validity of tokens
            if (token.isExpired()) {
                return returnHtmlForm(responseType, clientId, redirectUri, MSG_MISS_COOKIE, state, scope);
            }

        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_PASS_FORM, state, scope);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_PASS_FORM, state, scope);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return returnHtmlForm(responseType, clientId, redirectUri, MSG_PASS_FORM, state, scope);
        }
        long issuedAt = new Date().getTime();
        Map<String, String> paramMap = new HashMap<>();

        if (OAuth2Helper.ResponseType.TOKEN.equals(responseType)
                || OAuth2Helper.ResponseType.CODE.equals(responseType)) {
            // TODO add p_cookie
//            CellLocalRefreshToken rToken = new CellLocalRefreshToken(
//                    issuedAt, token.getIssuer(), token.getSubject(), clientId);
            //Regenerate AccessToken from received Token
            String username = token.getSubject();

            if (OAuth2Helper.ResponseType.TOKEN.equals(responseType)) {
                AccountAccessToken aToken = new AccountAccessToken(issuedAt, expiresIn,
                        getIssuerUrl(), username, clientId);
                paramMap.put(OAuth2Helper.Key.ACCESS_TOKEN, aToken.toTokenString());
                paramMap.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
                paramMap.put(OAuth2Helper.Key.EXPIRES_IN, String.valueOf(aToken.expiresIn()));
            } else if (OAuth2Helper.ResponseType.CODE.equals(responseType)) {
                List<Role> roleList = cell.getRoleListForAccount(token.getSubject());
                CellLocalAccessToken aToken = new CellLocalAccessToken(issuedAt,
                        CellLocalAccessToken.CODE_EXPIRES, getIssuerUrl(), username, roleList, clientId, scope);
                paramMap.put(OAuth2Helper.Key.CODE, aToken.toCodeString());
            }
        } else {
            CellCmp cellCmp = (CellCmp) cellRsCmp.getDavCmp();
            CellKeysFile cellKeysFile = cellCmp.getCellKeys().getCellKeysFile();
            String subject = token.getSubject();
            long issuedAtSec = issuedAt / AbstractOAuth2Token.MILLISECS_IN_A_SEC;
            long expiryTime = issuedAtSec + AbstractOAuth2Token.SECS_IN_A_HOUR;
            IdToken idToken = new IdToken(
                    cellKeysFile.getKeyId(), AlgorithmUtils.RS_SHA_256_ALGO, getIssuerUrl(),
                    subject, clientId, expiryTime, issuedAtSec, cellKeysFile.getPrivateKey());
            paramMap.put(OAuth2Helper.Key.ID_TOKEN, idToken.toTokenString());
        }
        if (StringUtils.isNotEmpty(state)) {
            paramMap.put(OAuth2Helper.Key.STATE, state);
        }
        //Cookie authentication successful
        //Respond with 303 and return Location header
        return returnSuccessRedirect(responseType, redirectUri, paramMap, keepLogin);
    }

    private Response returnSuccessRedirect(String responseType, String redirectUri,
            Map<String, String> paramMap, String keepLogin) { //NOPMD add p_cookie
        //Respond with 302 and return the Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(redirectUri).append(getConnectionCode(responseType, redirectUri));
        for (String key : paramMap.keySet()) {
            sbuf.append(key).append("=").append(paramMap.get(key)).append("&");
        }
        // Delete '&'
        sbuf.deleteCharAt(sbuf.length() - 1);
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        // TODO add p_cookie
//        //Return a cookie that is valid only in the cell to be authenticated
//        URL cellUrl = new URL(cell.getUrl());
//        NewCookie cookies = null;
//        Cookie cookie = new Cookie(OAuth2Helper.Key.SESSION_ID, refreshToken, cellUrl.getPath(), null);
//        if (code != null) {
//            //Create a cookie that can be used only with the same SSL as the expiration date of the refresh token
//            //Only when the execution environment is https, set the secure flag
//            if (OAuth2Helper.Key.TRUE_STR.equals(keepLogin)) {
//                //Set cookie expiration time to 24 hours
//                cookies = new NewCookie(cookie, "", COOKIE_MAX_AGE, PersoniumUnitConfig.isHttps());
//            } else {
//                //Do not set cookie expiration date
//                cookies = new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps());
//            }
//        } else {
//            cookies = new NewCookie(cookie, "", 0, PersoniumUnitConfig.isHttps());
//        }
        return rb.entity("").build();
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
     * Redirect to error page.
     * @param state
     * @return
     * @throws MalformedURLException
     */
    // RFC6749.
    // If the request fails due to a missing, invalid, or mismatching redirection URI,
    // or if the client identifier is missing or invalid,
    // the authorization server SHOULD inform the resource owner of the error and MUST NOT automatically redirect
    // the user-agent to the invalid redirection URI.
    private Response returnErrorPageRedirect(String code) {
        String redirectUri = cell.getUrl() + "__html/error";
        //Respond with 303 and return Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUri + SEPARATOR_QUERY
                + OAuth2Helper.Key.CODE + "=" + code);
        //Returning the response
        return rb.entity("").build();
    }

    /**
     * Of the errors during authentication with ImplicitFlow, execute Redirect to redirect_uri set by the user in the following situation. Invalid / unspecified response_type 2
     * @param responseType
     * @param redirectUri
     * @param error
     * @param errorDesp
     * @param state
     * @param code
     * @return
     */
    private Response returnErrorRedirect(String responseType, String redirectUri, String error,
            String errorDesp, String state, String code) {
        //Respond with 303 and return Location header
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        //URL encode the fragment information to be added to the Location header
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(redirectUri)
            .append(getConnectionCode(responseType, redirectUri))
            .append(OAuth2Helper.Key.ERROR)
            .append("=");
        try {
            sbuf.append(URLEncoder.encode(error, CharEncoding.UTF_8))
                .append("&").append(OAuth2Helper.Key.ERROR_DESCRIPTION)
                .append("=").append(URLEncoder.encode(errorDesp, CharEncoding.UTF_8));
            if (StringUtils.isNotEmpty(state)) {
                sbuf.append("&").append(OAuth2Helper.Key.STATE)
                    .append("=").append(URLEncoder.encode(state, CharEncoding.UTF_8));
            }
            sbuf.append("&").append(OAuth2Helper.Key.CODE)
                .append("=").append(URLEncoder.encode(code, CharEncoding.UTF_8));
        } catch (UnsupportedEncodingException e) {
            //Since the encoding type is fixed and set to utf-8, it is impossible to come here
            log.warn("Failed to URLencode, fragmentInfo of Location header.");
        }
        rb.header(HttpHeaders.LOCATION, sbuf.toString());
        //Returning the response
        return rb.entity("").build();
    }

    /**
     * Return authorization form html.
     * @param responseType
     * @param clientId
     * @param redirectUri
     * @param state
     * @param scope
     * @param massagae
     * @return
     */
    private Response returnHtmlForm(String responseType, String clientId, String redirectUri,
            String massage, String state, String scope) {
        ResponseBuilder rb = Response.ok().type(MediaType.TEXT_HTML_TYPE.withCharset(CharEncoding.UTF_8));
        return rb.entity(this.createForm(responseType, clientId, redirectUri, massage, state, scope)).build();
    }

    /**
     * Password authentication form.
     * @param responseType responseType
     * @param clientId clientId
     * @param redirectUri redirectUri
     * @param message String to be output to message display area
     * @param state state
     * @param scope scope
     * @param oAuthResponseType responseType
     * @param dcTraget dcTraget
     * @param pOwner pOwner
     * @return HTML
     */
    private String createForm(String responseType, String clientId, String redirectUri,
            String message, String state, String scope) {

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

            paramsList.add(AuthResourceUtils.getJavascript(AJAX_FILE_NAME));
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
            paramsList.add(state != null ? state : ""); // CHECKSTYLE IGNORE
            paramsList.add(responseType);
            paramsList.add(clientId);
            paramsList.add(redirectUri);
            paramsList.add(scope != null ? scope : ""); // CHECKSTYLE IGNORE

            Object[] params = paramsList.toArray();

            String html = PersoniumCoreUtils.readStringResource("html/authform.html", CharEncoding.UTF_8);
            html = MessageFormat.format(html, params);

            return html;
        }
    }

    /**
     * Check client_id and redirect_uri.
     * @param clientId
     * @param redirectUri
     */
    // RFC6749.
    // If the request fails due to a missing, invalid, or mismatching redirection URI,
    // or if the client identifier is missing or invalid,
    // the authorization server SHOULD inform the resource owner of the error and MUST NOT automatically redirect
    // the user-agent to the invalid redirection URI.
    private void validateClientIdAndRedirectUri(String clientId, String redirectUri) {
        if (StringUtils.isEmpty(clientId)) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }
        if (StringUtils.isEmpty(redirectUri)) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }

        String normalizedRedirectUri = redirectUri;
        String normalizedClientId = clientId;
        //Presence / absence of trailing "/"
        if (!redirectUri.endsWith("/")) {
            normalizedRedirectUri = redirectUri + "/";
        }
        if (!clientId.endsWith("/")) {
            normalizedClientId = clientId + "/";
        }

        URL objClientId = null;
        URL objRedirectUri = null;
        try {
            objClientId = new URL(normalizedClientId);
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }
        try {
            objRedirectUri = new URL(normalizedRedirectUri);
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }

        if (normalizedRedirectUri.contains("\n") || normalizedRedirectUri.contains("\r")) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }
        if (normalizedClientId.contains("\n") || normalizedClientId.contains("\r")) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }

        //Compare client_id and redirect_uri, and if the cells are different, an authentication error
        //Comparison of cell URLs
        if (!objClientId.getAuthority().equals(objRedirectUri.getAuthority())
                || !normalizedRedirectUri.startsWith(normalizedClientId)) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }
        //Compare the client_id with the name of the requested cell, and an error if the cells are the same
        if (normalizedClientId.equals(cell.getUrl())) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_CLIENTID_INVALID;
        }
        // Check box installed.
        if (!checkBoxInstall(normalizedClientId)) {
            PersoniumCoreException exception = PersoniumCoreException.Auth.BOX_NOT_INSTALLED;
            log.debug(exception.getMessage());
            throw exception;
        }
    }

    /**
     * Authorization processing It is checked whether there is a Box whose schema is the cell URL specified by clientId.
     * @param clientId App Store URL
     * @return true: authorization success false: authorization failure
     */
    private boolean checkBoxInstall(final String clientId) {
        EntitySetAccessor boxAcceccor = EsModel.box(this.cell);

        // {filter={and={filters=[{term={c=$CELL_ID}, {term={s.Schema.untouched=$CLIENT_ID}]}}}
        Map<String, Object> query1 = new HashMap<String, Object>();
        Map<String, Object> term1 = new HashMap<String, Object>();
        Map<String, Object> query2 = new HashMap<String, Object>();
        Map<String, Object> term2 = new HashMap<String, Object>();
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

    private String getConnectionCode(String responseType, String redirectUri) {
        String separator = getSeparator(responseType);
        if (StringUtils.contains(redirectUri, separator)) {
            return "&";
        } else {
            return separator;
        }
    }

    private String getSeparator(String responseType) {
        if (OAuth2Helper.ResponseType.CODE.equals(responseType)) {
            return SEPARATOR_QUERY;
        } else {
            return SEPARATOR_FRAGMENT;
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

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

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
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
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.plugin.PluginInfo;
import io.personium.core.plugin.PluginManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;
import io.personium.plugin.base.Plugin;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginException;
import io.personium.plugin.base.auth.AuthenticatedIdentity;

/**
 * JAX-RS Resource class for authentication.
 */
public class TokenEndPointResource {
    // core issue #223
    // "issuer" in the token may be interpreted by other units.
    // For that reason, "path based cell url" is set for "issuer" regardless of unit property setting.

    static Logger log = LoggerFactory.getLogger(TokenEndPointResource.class);

    private final Cell cell;
    private final DavRsCmp davRsCmp;
    private boolean issueCookie = false;
    private UriInfo requestURIInfo;
    //The UUID of the Account used for password authentication. It is used to update the last login time after password authentication.
    private String accountId;
    private String ipaddress;

    /**
     * constructor.
     * @param cell  Cell
     * @param davRsCmp davRsCmp
     */
    public TokenEndPointResource(final Cell cell, final DavRsCmp davRsCmp) {
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }

    /**
     * OAuth2.0 Token Endpoint. <h2>Issue some kinds of tokens.</h2>
     * <ul>
     * <li> If URL is written in p_target, issue transCellToken as CELL of TARGET as its CELL. </ li>
     * <li> Issue CellLocal if scope does not exist. </ li>
     * </ul>
     * @param uriInfo  URI information
     * @param authzHeader Authorization Header
     * @param formParams Body parameters
     * @param xForwardedFor X-Forwarded-For Header
     * @return JAX-RS Response Object
     */
    @POST
    public final Response token(@Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader,
            MultivaluedMap<String, String> formParams,
            @HeaderParam("X-Forwarded-For") final String xForwardedFor) {
        // Using @FormParam will cause a closed error on the library side in case of an incorrect body.
        // Since we can not catch Exception, retrieve the value after receiving it with MultivaluedMap.
        String grantType = formParams.getFirst(Key.GRANT_TYPE);
        String username = formParams.getFirst(Key.USERNAME);
        String password = formParams.getFirst(Key.PASSWORD);
        String pTarget = formParams.getFirst(Key.TARGET);
        String pOwner = formParams.getFirst(Key.OWNER);
        String assertion = formParams.getFirst(Key.ASSERTION);
        String refreshToken = formParams.getFirst(Key.REFRESH_TOKEN);
        String code = formParams.getFirst(Key.CODE);
        String clientId = formParams.getFirst(Key.CLIENT_ID);
        String clientSecret = formParams.getFirst(Key.CLIENT_SECRET);
        String pCookie = formParams.getFirst("p_cookie");

        // Accept unit local scheme url.
        String target = UriUtils.convertSchemeFromLocalUnitToHttp(
                cell.getUnitUrl(), pTarget);
        //If p_target is not a URL, it creates a vulnerability of header injection. (Such as a line feed code is included)
        target = this.checkPTarget(target);

        if (null != pTarget) {
            issueCookie = false;
        } else {
            issueCookie = Boolean.parseBoolean(pCookie);
            requestURIInfo = uriInfo;
        }

        this.ipaddress = xForwardedFor;

        String schema = null;
        //First, check if you want to authenticate Client
        //If neither Scope nor authzHeader nor clientId exists, it is assumed that Client authentication is not performed.
        if (clientId != null || authzHeader != null) {
            schema = this.clientAuth(clientId, clientSecret, authzHeader, cell.getUrl());
        }

        if (OAuth2Helper.GrantType.PASSWORD.equals(grantType)) {
            //Regular password authentication
            Response response = this.handlePassword(target, pOwner,
                    schema, username, password);
            return response;
        } else if (OAuth2Helper.GrantType.SAML2_BEARER.equals(grantType)) {
            return this.receiveSaml2(target, pOwner, schema, assertion);
        } else if (OAuth2Helper.GrantType.REFRESH_TOKEN.equals(grantType)) {
            return this.receiveRefresh(target, pOwner, schema, refreshToken);
        } else if (OAuth2Helper.GrantType.AUTHORIZATION_CODE.equals(grantType)) {
            return receiveCode(target, pOwner, schema, code);
        } else {
            // Call Auth Plugins
            return this.callAuthPlugins(grantType, formParams, target, pOwner,
                    schema);
        }
    }

    /**
     * Get url of "issuer" to be set to token.
     * @return url of "issuer"
     */
    private String getIssuerUrl() {
        return cell.getPathBaseUrl();
    }

    //TODO temporary implementation
    private Response receiveCode(final String target, String owner, String schema,
            final String code) {
        if (code == null) {
            //If code is not set, it is regarded as a parse error
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        }
        if (schema == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.CLIENT_ID);
        }
        if (Key.TRUE_STR.equals(owner)) {
            throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER
                    .realm(this.cell.getUrl());
        }
        if (!code.startsWith(CellLocalAccessToken.PREFIX_CODE)) {
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        }

        CellLocalAccessToken token;
        try {
            token = (CellLocalAccessToken) AbstractOAuth2Token.parse(code, getIssuerUrl(), cell.getUnitUrl());
        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        // Check if expired.
        if (token.isRefreshExpired()) {
            throw PersoniumCoreAuthnException.TOKEN_EXPIRED.realm(this.cell.getUrl());
        }

        if (!StringUtils.equals(schema, token.getSchema())) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        long issuedAt = new Date().getTime();

        //Regenerate AccessToken and RefreshToken from the received Token
        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt, getIssuerUrl(), token.getSubject(), schema);
        IAccessToken aToken = null;
        if (target == null) {
            aToken = new CellLocalAccessToken(issuedAt, getIssuerUrl(), token.getSubject(), token.getRoles(), schema);
        } else {
            List<Role> roleList = cell.getRoleListForAccount(token.getSubject());
            aToken = new TransCellAccessToken(issuedAt, getIssuerUrl(),
                    cell.getPathBaseUrl() + "#" + token.getSubject(), target, roleList, schema);
        }

        if (aToken instanceof TransCellAccessToken) {
            log.debug("reissuing TransCell Token");
            // aToken.addRole("admin");
            // return this.responseAuthSuccess(tcToken);
        }
        return this.responseAuthSuccess(aToken, rToken);
    }

    /**
     * call Auth Plugins.
     * @param grantType
     * @param params
     * @param target
     * @param owner
     * @param schema
     * @param username
     * @return Response
     */
    private Response callAuthPlugins(String grantType, MultivaluedMap<String, String> params,
            String target, String owner, String schema) {
        // Plugin manager.
        PluginManager pm = PersoniumCoreApplication.getPluginManager();
        // Search target plugin.
        PluginInfo pi = pm.getPluginsByGrantType(grantType);
        if (pi == null) {
            // When there is no plugin.
            throw PersoniumCoreAuthnException.UNSUPPORTED_GRANT_TYPE
                    .realm(this.cell.getUrl());
        }

        AuthenticatedIdentity ai = null;
        // Invoke the plug-in function.
        Map<String, List<String>> body = new HashMap<String, List<String>>();
        if (params != null) {
            for (String key : params.keySet()) {
                body.put(key, params.get(key));
            }
        }
        Object plugin = (Plugin) pi.getObj();
        try {
            ai = ((AuthPlugin) plugin).authenticate(body);
        } catch (AuthPluginException ape) {
            throw PersoniumCoreAuthnException.create(ape);
        } catch (Exception e) {
            // Unexpected exception throwed from "Plugin", create default PersoniumCoreAuthException
            // and set reason from catched Exception.
            throw PersoniumCoreException.Plugin.UNEXPECTED_ERROR.reason(e);
        }

        if (ai == null) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }
        String accountName = ai.getAccountName();
        String accountType = ai.getAccountType();
        if (accountName == null || accountType == null) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }

        // If the Account shown in IdToken does not exist in cell.
        OEntityWrapper idTokenUserOew = cell.getAccount(accountName);
        if (idTokenUserOew == null) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.OIDC.NO_SUCH_ACCOUNT.params(accountName).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }

        // Confirm if OidC is included in Type when there is Account.
        if (!AuthUtils.getAccountType(idTokenUserOew).contains(accountType)) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.OIDC.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(accountType,
                    accountName).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }

        // When processing is normally completed, issue a token.
        return this.issueToken(target, owner, schema, accountName);
    }

    /**
     * checkPTarget.
     */
    private String checkPTarget(final String pTarget) {
        String target = pTarget;
        if (target != null) {
            try {
                new URL(target);
                if (!target.endsWith("/")) {
                    target = target + "/";
                }
                if (target.contains("\n") || target.contains("\r")) {
                    //Error when p_target is not a URL
                    throw PersoniumCoreAuthnException.INVALID_TARGET
                            .realm(this.cell.getUrl());
                }
            } catch (MalformedURLException e) {
                //Error when p_target is not a URL
                throw PersoniumCoreAuthnException.INVALID_TARGET
                        .realm(this.cell.getUrl());
            }
        }
        return target;
    }

    /**
     * Client authentication processing.
     * @param clientId Schema
     * @param clientSecret token
     * @param authzHeader Value of Authorization Header
     * @param cellUrl Cell URL
     * @return null: Client authentication failed.
     */
    public static String clientAuth(final String clientId, final String clientSecret,
            final String authzHeader, final String cellUrl) {
        String targetClientId = clientId;
        String targetClientSecret = clientSecret;
        if (targetClientSecret == null) {
            targetClientSecret = "";
        }

        //Parsing authzHeader
        if (authzHeader != null) {
            String[] idpw = PersoniumCoreUtils
                    .parseBasicAuthzHeader(authzHeader);
            if (idpw != null) {
                //Specify authzHeader first
                targetClientId = idpw[0];
                targetClientSecret = idpw[1];
            } else {
                throw PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID
                        .realm(cellUrl);
            }
        }

        //Check pw
        //· Since PW is a SAML token, it is parsed.
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(targetClientSecret);
        } catch (TokenParseException e) {
            //Perth failure
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.CLIENT_SECRET_PARSE_ERROR.realm(
                    cellUrl).reason(e);
        } catch (TokenDsigException e) {
            //Signature validation error
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID
                    .realm(cellUrl);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(
                    e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        //· Expiration date check
        if (tcToken.isExpired()) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_EXPIRED.realm(cellUrl);
        }

        // Confirm that Issuer is equal to ID
        // issuer is always pathbase.
        String fqdnBaseIssuer;
        try {
            fqdnBaseIssuer = UriUtils.convertPathBaseToFqdnBase(tcToken.getIssuer());
        } catch (URISyntaxException e) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_ISSUER_MISMATCH.realm(cellUrl);
        }
        if (!targetClientId.equals(tcToken.getIssuer())
                && !targetClientId.equals(fqdnBaseIssuer)) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_ISSUER_MISMATCH.realm(cellUrl);
        }

        // If the target of the token is not yourself, an error response
        if (!tcToken.getTarget().equals(cellUrl)) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_TARGET_WRONG.realm(cellUrl);
        }

        //Give # c if the role is a confidential value
        String confidentialRoleUrl = String.format(
                OAuth2Helper.Key.CONFIDENTIAL_ROLE_URL_FORMAT,
                tcToken.getIssuer(), Box.DEFAULT_BOX_NAME);
        for (Role role : tcToken.getRoles()) {
            if (confidentialRoleUrl.equals(role.createUrl())) {
                //Successful authentication.
                return targetClientId + OAuth2Helper.Key.CONFIDENTIAL_MARKER;
            }
        }
        //Successful authentication.
        return targetClientId;
    }

    private Response receiveSaml2(final String target, final String owner,
            final String schema, final String assertion) {
        if (Key.TRUE_STR.equals(owner)) {
            //Do not promote unit user in token authentication
            throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER
                    .realm(this.cell.getUrl());
        }

        //Assertion null check
        if (assertion == null) {
            //If assertion is not set, it is regarded as a parse error
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        }

        //First to parse
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(assertion);
        } catch (TokenParseException e) {
            //When parsing fails
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        } catch (TokenDsigException e) {
            //Error in signature verification
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        //Verification of Token
        //1. Expiration check
        if (tcToken.isExpired()) {
            throw PersoniumCoreAuthnException.TOKEN_EXPIRED.realm(this.cell.getUrl());
        }

        //If the target of the token is not yourself, an error response
        try {
            if (!(AuthResourceUtils.checkTargetUrl(this.cell, tcToken))) {
                throw PersoniumCoreAuthnException.TOKEN_TARGET_WRONG.realm(
                        this.cell.getUrl()).params(tcToken.getTarget());
            }
        } catch (MalformedURLException e) {
            throw PersoniumCoreAuthnException.TOKEN_TARGET_WRONG.realm(
                    this.cell.getUrl()).params(tcToken.getTarget());
        }

        //Authentication is successful -------------------------------

        //Create a refresh token based on the authentication information
        long issuedAt = new Date().getTime();
        TransCellRefreshToken rToken = new TransCellRefreshToken(
                tcToken.getId(), //Save ID of received SAML
                issuedAt, getIssuerUrl(), tcToken.getSubject(),
                tcToken.getIssuer(), //Save receipt of SAML's
                tcToken.getRoles(), //Save receipt of SAML's
                schema);

        //Ask CELL to decide the role of you from the role of TC issuer.
        List<Role> rolesHere = cell.getRoleListHere(tcToken);

        //Can I use the specified one for TODO schema?
        //TODO schema authentication is necessary.
        String schemaVerified = schema;

        //Authentication token issue processing
        //The target can be freely decided.
        IAccessToken aToken = null;
        if (target == null) {
            aToken = new CellLocalAccessToken(issuedAt, getIssuerUrl(),
                    tcToken.getSubject(), rolesHere, schemaVerified);
        } else {
            aToken = new TransCellAccessToken(UUID.randomUUID().toString(),
                    issuedAt, getIssuerUrl(), tcToken.getSubject(), target,
                    rolesHere, schemaVerified);
        }
        return this.responseAuthSuccess(aToken, rToken);
    }

    /**
     * Authentication with Refresh token.
     * @param target
     * @param owner
     * @param schema
     * @param refreshToken
     * @return
     */
    private Response receiveRefresh(final String target, String owner, String schema,
            final String refreshToken) {
        if (refreshToken == null) {
            //If refreshToken is not set, it is regarded as a parse error
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        }

        AbstractOAuth2Token token;
        try {
            token = AbstractOAuth2Token.parse(refreshToken, getIssuerUrl(), cell.getUnitUrl());
        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        if (!(token instanceof IRefreshToken)) {
            throw PersoniumCoreAuthnException.NOT_REFRESH_TOKEN.realm(this.cell.getUrl());
        }

        // Check if expired.
        if (token.isRefreshExpired()) {
            throw PersoniumCoreAuthnException.TOKEN_EXPIRED.realm(this.cell.getUrl());
        }

        long issuedAt = new Date().getTime();

        if (Key.TRUE_STR.equals(owner)) {
            //You can be promoted only for your own cell refresh.
            if (token.getClass() != CellLocalRefreshToken.class) {
                throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER.realm(this.cell.getUrl());
            }
            //Check unit escalation privilege setting
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(token.getSubject())) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER.realm(this.cell.getUrl());
            }
            //Do not promote cells for which the owner of the cell is not set.
            if (cell.getOwner() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell.getUrl());
            }

            //uluut issuance processing
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt,
                    UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                    cell.getOwner(), cell.getUnitUrl());

            return this.responseAuthSuccess(uluut, null);
        } else {
            //Regenerate AccessToken and RefreshToken from received Refresh Token
            IRefreshToken rToken = (IRefreshToken) token;
            rToken = rToken.refreshRefreshToken(issuedAt);

            IAccessToken aToken = null;
            if (rToken instanceof CellLocalRefreshToken) {
                String subject = rToken.getSubject();
                List<Role> roleList = cell.getRoleListForAccount(subject);
                aToken = rToken.refreshAccessToken(issuedAt, target, cell.getPathBaseUrl(), roleList, schema);
            } else {
                //Ask CELL to determine the role of you from the role of the token issuer.
                List<Role> rolesHere = cell.getRoleListHere((IExtRoleContainingToken) rToken);
                aToken = rToken.refreshAccessToken(issuedAt, target, getIssuerUrl(), rolesHere, schema);
            }

            if (aToken instanceof TransCellAccessToken) {
                log.debug("reissuing TransCell Token");
                // aToken.addRole("admin");
                // return this.responseAuthSuccess(tcToken);
            }
            return this.responseAuthSuccess(aToken, rToken);
        }
    }

    @SuppressWarnings("unchecked")
    private Response responseAuthSuccess(final IAccessToken accessToken, final IRefreshToken refreshToken) {
        JSONObject resp = new JSONObject();
        resp.put(OAuth2Helper.Key.ACCESS_TOKEN, accessToken.toTokenString());
        resp.put(OAuth2Helper.Key.EXPIRES_IN, accessToken.expiresIn());
        if (refreshToken != null) {
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN, refreshToken.toTokenString());
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN, refreshToken.refreshExpiresIn());
        }
        resp.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
        ResponseBuilder rb = Response.ok().type(MediaType.APPLICATION_JSON_TYPE);
        if (accessToken.getTarget() != null) {
            resp.put(OAuth2Helper.Key.TARGET, accessToken.getTarget());
            rb.header(HttpHeaders.LOCATION, accessToken.getTarget() + "__token");
        }

        if (issueCookie) {
            String tokenString = accessToken.toTokenString();
            //Set random UUID as p_cookie_peer
            String pCookiePeer = UUID.randomUUID().toString();
            String cookieValue = pCookiePeer + "\t" + tokenString;
            //The p_cookie value to return to the header is encrypted
            String encodedCookieValue = LocalToken.encode(cookieValue,
                    UnitLocalUnitUserToken.getIvBytes(AccessContext
                            .getCookieCryptKey(requestURIInfo.getBaseUri().getHost())));
            //Specify cookie version (0)
            int version = 0;
            String path = getCookiePath();

            //Create a cookie and return it to the response header
            Cookie cookie = new Cookie("p_cookie", encodedCookieValue, path,
                    requestURIInfo.getBaseUri().getHost(), version);
            rb.cookie(new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps()));
            //Return "p_cookie_peer" of the response body
            resp.put("p_cookie_peer", pCookiePeer);
        }

        if (accountId != null && !accountId.isEmpty()) {
            // get last auth history.
            AuthHistoryLastFile last = AuthResourceUtils.getAuthHistoryLast(
                    davRsCmp.getDavCmp().getFsPath(), accountId);
            resp.put(OAuth2Helper.Key.LAST_AUTHENTICATED, last.getLastAuthenticated());
            resp.put(OAuth2Helper.Key.FAILED_COUNT, last.getFailedCount());
            // update auth history.
            AuthResourceUtils.addSuccessAuthHistory(davRsCmp.getDavCmp().getFsPath(), accountId);
            // release account lock.
            AuthResourceUtils.releaseAccountLock(accountId);
        }

        return rb.entity(resp.toJSONString()).build();
    }

    /**
     * Create a path to set as a cookie.
     * @return Path to set for cookie
     */
    private String getCookiePath() {
        String cellUrl = cell.getUrl();
        try {
            URL url = new URL(cellUrl);
            return url.getPath();
        } catch (MalformedURLException e) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }
    }

    private Response handlePassword(final String target, final String owner,
            final String schema, final String username,
            final String password) {

        //Password check processing
        if (username == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.USERNAME);
        } else if (password == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.PASSWORD);
        }

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            PersoniumCoreLog.Auth.AUTHN_FAILED_NO_SUCH_ACCOUNT.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        //Confirmation of Type value
        if (!AuthUtils.isAccountTypeBasic(oew)) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.Auth.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(
                    Account.TYPE_VALUE_BASIC, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        //In order to update the last login time, keep UUID in class variable
        accountId = (String) oew.getUuid();

        //Check valid authentication interval
        Boolean isLock = AuthResourceUtils.isLockedInterval(accountId);
        if (isLock) {
            //Update lock time of memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCountForAccountLock(accountId);
            AuthResourceUtils.addFailedAuthHistory(davRsCmp.getDavCmp().getFsPath(), accountId);
            PersoniumCoreLog.Auth.AUTHN_FAILED_BEFORE_AUTHENTICATION_INTERVAL.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        //Check account lock
        isLock = AuthResourceUtils.isLockedAccount(accountId);
        if (isLock) {
            //Update lock time of memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCountForAccountLock(accountId);
            AuthResourceUtils.addFailedAuthHistory(davRsCmp.getDavCmp().getFsPath(), accountId);
            PersoniumCoreLog.Auth.AUTHN_FAILED_ACCOUNT_IS_LOCKED.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        boolean authSuccess = cell.authenticateAccount(oew, password);

        if (!authSuccess) {
            //Make lock on memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCountForAccountLock(accountId);
            AuthResourceUtils.addFailedAuthHistory(davRsCmp.getDavCmp().getFsPath(), accountId);
            PersoniumCoreLog.Auth.AUTHN_FAILED_INCORRECT_PASSWORD.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        return issueToken(target, owner, schema, username);
    }

    private Response issueToken(final String target, final String owner,
            final String schema, final String username) {
        long issuedAt = new Date().getTime();

        if (Key.TRUE_STR.equals(owner)) {
            //Check unit escalation privilege setting
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(username)) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER
                        .realm(this.cell.getUrl());
            }
            //Do not promote cells for which the owner of the cell is not set.
            if (cell.getOwner() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell.getUrl());
            }

            //uluut issuance processing
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt,
                    UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR
                            * MILLISECS_IN_AN_HOUR, cell.getOwner(), cell.getUnitUrl());
            return this.responseAuthSuccess(uluut, null);
        }

        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt,
                CellLocalRefreshToken.REFRESH_TOKEN_EXPIRES_HOUR
                        * MILLISECS_IN_AN_HOUR, getIssuerUrl(), username, schema);

        //Create a response.
        if (target == null) {
            AccountAccessToken localToken = new AccountAccessToken(issuedAt,
                    AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR
                            * MILLISECS_IN_AN_HOUR, getIssuerUrl(), username, schema);
            return this.responseAuthSuccess(localToken, rToken);
        } else {
            //Check that TODO SCHEMA is URL
            //Check that TODO TARGET is URL

            List<Role> roleList = cell.getRoleListForAccount(username);

            TransCellAccessToken tcToken = new TransCellAccessToken(
                    getIssuerUrl(), cell.getPathBaseUrl() + "#" + username, target,
                    roleList, schema);
            return this.responseAuthSuccess(tcToken, rToken);
        }
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST).build();
    }
}

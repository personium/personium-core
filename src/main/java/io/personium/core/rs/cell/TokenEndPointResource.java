/**
 * Personium
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
package io.personium.core.rs.cell;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.GrantCode;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.IRefreshToken;
import io.personium.common.auth.token.IdToken;
import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.common.auth.token.ResidentRefreshToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.auth.ScopeArbitrator;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.impl.fs.CellKeysFile;
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
 * JAX-RS Resource class for Token Endpoint.
 */
public class TokenEndPointResource {
    static Logger log = LoggerFactory.getLogger(TokenEndPointResource.class);
    public static final String PATH = "__token";

    private final Cell cell;
    private final CellRsCmp davRsCmp;
    private boolean issueCookie = false;
    private UriInfo requestURIInfo;
    //The UUID of the Account used for password authentication. It is used to update the last login time after password authentication.
    private String accountId;
    private String ipaddress;
    private boolean isRecordingAuthHistory = false;

    /**
     * constructor.
     * @param cell  Cell
     * @param davRsCmp davRsCmp
     */
    public TokenEndPointResource(final Cell cell, final CellRsCmp davRsCmp) {
        this.cell = cell;
        this.davRsCmp = davRsCmp;
    }
    public String getUrl() {
        return this.cell.getUrl() + PATH;
    }

    /**
     * OAuth2.0 Token Endpoint.
     * Issues differnt kinds of tokens depending on the parameters.
     * <ul>
     * <li> If p_target parameter exists, it issues Trans-Cell access token targeting at the specified URL. </ li>
     * <li> If p_target parameter is not specified, it issues Cell-local access token. </ li>
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
        if (formParams == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.params(OAuth2Helper.Key.GRANT_TYPE);
        }
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
        String clientAssertion = formParams.getFirst(Key.CLIENT_ASSERTION);
        String clientAssertionType = formParams.getFirst(Key.CLIENT_ASSERTION_TYPE);
        String expiresInStr = formParams.getFirst(Key.EXPIRES_IN);
        String rTokenExpiresInStr = formParams.getFirst(Key.REFRESH_TOKEN_EXPIRES_IN);
        String pCookie = formParams.getFirst(Key.P_COOKIE);
        String scopeStr = formParams.getFirst(Key.SCOPE);

        String[] scope = AbstractOAuth2Token.Scope.parse(scopeStr);

        // relsolve personium-localunit scheme url.
        String target = UriUtils.convertSchemeFromLocalUnitToHttp(pTarget);

        //Check the given target to prevent security attacks such as Header Injection.
        //eg. If p_target is not a URL and include line feed code, it creates a vulnerability of header injection.
        if (target != null) {
            this.checkURL(target);
            target = this.addTrainlingSlash(target);
            // TODO should do more normalization.
        }

        // Do not issue cookie if p_target exists, regardless of the p_cookie parameter.
        if (null != pTarget) {
            issueCookie = false;
        } else {
            issueCookie = Boolean.parseBoolean(pCookie);
        }

        this.requestURIInfo = uriInfo;
        this.ipaddress = xForwardedFor;

        String schema = null;
        // Authenticate client first if necessary.
        // If neither authzHeader, clientAssertion nor clientId exists,
        // client authentication is not performed.
        if (clientId != null || authzHeader != null || clientAssertion != null || clientAssertionType != null) {
            schema = clientAuth(clientId, clientSecret, clientAssertionType, clientAssertion,
                    authzHeader, cell.getUrl());
        }

        // Check value of expires_in
        long expiresIn = AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS;
        if (expiresInStr != null && !expiresInStr.isEmpty()) {
            try {
                expiresIn = Integer.parseInt(expiresInStr) * AbstractOAuth2Token.MILLISECS_IN_A_SEC;
                if (expiresIn <= 0 || expiresIn > AbstractOAuth2Token.ACCESS_TOKEN_EXPIRES_MILLISECS) {
                    throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                            this.cell.getUrl()).params(Key.EXPIRES_IN);
                }
            } catch (NumberFormatException e) {
                throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                        this.cell.getUrl()).params(Key.EXPIRES_IN);
            }
        }
        // Check value of refresh_token_expires_in
        long rTokenExpiresIn = AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS;
        if (rTokenExpiresInStr != null && !rTokenExpiresInStr.isEmpty()) {
            try {
                rTokenExpiresIn = Integer.parseInt(rTokenExpiresInStr) * AbstractOAuth2Token.MILLISECS_IN_A_SEC;
                if (rTokenExpiresIn <= 0 || rTokenExpiresIn > AbstractOAuth2Token.REFRESH_TOKEN_EXPIRES_MILLISECS) {
                    throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                            this.cell.getUrl()).params(Key.REFRESH_TOKEN_EXPIRES_IN);
                }
            } catch (NumberFormatException e) {
                throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                        this.cell.getUrl()).params(Key.REFRESH_TOKEN_EXPIRES_IN);
            }
        }

        if (OAuth2Helper.GrantType.PASSWORD.equals(grantType)) {
            //Regular password authentication
            Response response = this.handlePassword(target, pOwner,
                    schema, username, password, expiresIn, rTokenExpiresIn, scope);
            return response;
        } else if (OAuth2Helper.GrantType.SAML2_BEARER.equals(grantType)) {
            return this.receiveSaml2(target, pOwner, schema, assertion, expiresIn, rTokenExpiresIn);
        } else if (OAuth2Helper.GrantType.REFRESH_TOKEN.equals(grantType)) {
            return this.receiveRefresh(target, pOwner, schema, refreshToken, expiresIn, rTokenExpiresIn);
        } else if (OAuth2Helper.GrantType.AUTHORIZATION_CODE.equals(grantType)) {
            return receiveCode(target, pOwner, schema, code, expiresIn, rTokenExpiresIn);
        } else {
            // Call Auth Plugins
            return this.callAuthPlugins(grantType, formParams, target, pOwner,
                    schema, expiresIn, rTokenExpiresIn, scope);
        }
    }

    /**
     * Get url of "issuer" to be set to token.
     * @return url of "issuer"
     */
    private String getIssuerUrl() {
        return cell.getUrl();
    }

    /**
     * call Auth Plugins.
     * @param grantType
     * @param params
     * @param target
     * @param owner
     * @param schema
     * @param expiresIn accress token expires in time(ms).
     * @param rTokenExpiresIn refresh token expires in time(ms).
     * @return Response
     */
    private Response callAuthPlugins(String grantType, MultivaluedMap<String, String> params,
            String target, String owner, String schema, long expiresIn, long rTokenExpiresIn, String[] requestScopes) {
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
        //OEntityWrapper idTokenUserOew = cell.getAccount(accountName);
        Account account = cell.getAccount(accountName);
        if (account == null) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.OIDC.NO_SUCH_ACCOUNT.params(accountName).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }

        // Confirm if OidC is included in Type when there is Account.
        if (!account.typeList.contains(accountType)) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.OIDC.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(accountType,
                    accountName).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED;
        }

        String[] scopes = this.cell.getScopeArbitrator(schema, grantType).request(requestScopes).getResults();

        // Check account is active.
        if (!account.isActive()) {
            if (Account.STATUS_PASSWORD_CHANGE_REQUIRED.equals(account.status)) {
                // Issue password change.
                issuePasswordChange(schema, accountName, rTokenExpiresIn, scopes);
            } else {
                PersoniumCoreLog.OIDC.ACCOUNT_IS_DEACTIVATED.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, accountName).writeLog();
                throw PersoniumCoreAuthnException.AUTHN_FAILED;
            }
        }

        // When processing is normally completed, issue a token.
        return this.issueToken(target, owner, schema, accountName, expiresIn, rTokenExpiresIn, scopes);
    }

    /**
     * check p_target parameter for security.
     */
    private void checkURL(final String url) {
        try {
            new URL(url);
            if (url.contains("\n") || url.contains("\r")) {
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

    private String addTrainlingSlash(final String url) {
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    public static String clientAuth(
            final String clientId, final String clientSecret,
            final String clientAssertionType, final String clientAssertion,
            final String authzHeader, final String cellUrl) {
        // When clientAssertionType is spesified,
        if (clientAssertionType != null || clientAssertion != null) {
            // Then clientAssertionType should be valid value.
            if (!OAuth2Helper.GrantType.SAML2_BEARER.equals(clientAssertionType)) {
                throw PersoniumCoreAuthnException.INVALID_CLIENT_ASSERTION_TYPE.params(OAuth2Helper.GrantType.SAML2_BEARER);
            }
            // Just ignore clientSecret, authzHeader
            //
            return clientAuth(clientId, clientAssertion,
                    null, cellUrl);
        } else {
            // When clientAssertionType is NOT spesified,

            // clientId or authz header should be specified.
            if (clientId == null && authzHeader == null) {
                throw PersoniumCoreAuthnException.CLIENT_SECRET_ISSUER_MISMATCH.realm(cellUrl);
            }
            // Then use clientId, clientSecret or authHeader
            return clientAuth(clientId, clientSecret,
                    authzHeader, cellUrl);

        }
    }


    /**
     * Client authentication processing.
     * @param clientId Schema URL. if null is specified then skip check.
     * @param clientSecret token
     * @param authzHeader Value of Authorization Header
     * @param cellUrl Cell URL
     * @return null: Client authentication failed.
     */
    public static String clientAuth(
            final String clientId, final String clientSecret,
            final String authzHeader, final String cellUrl) {
        String targetClientId = clientId;
        String targetClientSecret = clientSecret;

        if (targetClientSecret == null) {
            targetClientSecret = "";
        }

        //Parsing authzHeader
        if (authzHeader != null) {
            String[] idpw = CommonUtils
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

        // relsolve personium-localunit scheme url.
        targetClientId = UriUtils.resolveLocalUnit(targetClientId);

        //Check pw
        //· Since PW is a SAML token, it is parsed.
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(targetClientSecret);
        } catch (TokenParseException e) {
            //Perth failure
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.CLIENT_ASSERTION_PARSE_ERROR.realm(
                    cellUrl).reason(e);
        } catch (TokenDsigException e) {
            //Signature validation error
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID
                    .realm(cellUrl).reason(e);
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
        // if clientId is null, then just skip this check
        if (clientId != null && !targetClientId.equals(tcToken.getIssuer())) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_ISSUER_MISMATCH.realm(cellUrl);
        }

        // If the target of the token is not yourself, an error response
        if (!tcToken.getTarget().equals(cellUrl)) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_TARGET_WRONG.realm(cellUrl);
        }

        //Give # c if the role is a confidential value
        String confidentialRoleUrl = String.format(
                OAuth2Helper.Key.CONFIDENTIAL_ROLE_URL_FORMAT,
                tcToken.getIssuer(), Box.MAIN_BOX_NAME);
        for (Role role : tcToken.getRoleList()) {
            if (confidentialRoleUrl.equals(role.toRoleInstanceURL())) {
                //Successful authentication.
                return targetClientId + OAuth2Helper.Key.CONFIDENTIAL_MARKER;
            }
        }
        //Successful authentication.
        return targetClientId;
    }

    /**
     * authorization_code process.
     * @param target p_target
     * @param owner p_owner
     * @param schema client_id
     * @param code code
     * @param expiresIn accress token expires in time(ms).
     * @param rTokenExpiresIn refresh token expires in time(ms).
     * @return API response
     */
    private Response receiveCode(final String target, String owner, String schema,
            final String code, long expiresIn, long rTokenExpiresIn) {
        if (code == null) {
            //If code is not set, it is regarded as a parse error
            throw PersoniumCoreAuthnException.INVALID_GRANT_CODE.reason(new IllegalArgumentException("grant code not provided"));
        }
        if (schema == null) {
            throw PersoniumCoreAuthnException.CLIENT_AUTH_REQUIRED.realm(
                    this.cell.getUrl());
        }
        if (Key.TRUE_STR.equals(owner)) {
            throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER
                    .realm(this.cell.getUrl());
        }
        if (!code.startsWith(GrantCode.PREFIX_CODE)) {
            throw PersoniumCoreAuthnException.INVALID_GRANT_CODE
                .reason(new IllegalArgumentException("Invalid Prefix"));
        }

        GrantCode grantCode;
        try {
            grantCode = (GrantCode) AbstractOAuth2Token.parse(code, getIssuerUrl(), cell.getUnitUrl());
        } catch (TokenParseException e) {
            // failed in parse
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.INVALID_GRANT_CODE.reason(e);
        } catch (TokenDsigException e) {
            //certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        // Check if expired.
        if (grantCode.isExpired()) {
            throw PersoniumCoreAuthnException.TOKEN_EXPIRED.realm(this.cell.getUrl());
        }

        String gcSchema = grantCode.getSchema();
        if (!StringUtils.equals(gcSchema, schema.replaceAll(OAuth2Helper.Key.CONFIDENTIAL_MARKER, ""))) {
            throw PersoniumCoreAuthnException.CLIENT_MISMATCH.params(gcSchema, schema).realm(this.cell.getUrl());
        }

        long issuedAt = new Date().getTime();

        //Regenerate AccessToken and RefreshToken from the received Token
        ResidentRefreshToken rToken = new ResidentRefreshToken(issuedAt, rTokenExpiresIn, getIssuerUrl(),
                grantCode.getSubject(), schema, grantCode.getScope());
        IAccessToken aToken = null;
        if (target == null) {
            aToken = new ResidentLocalAccessToken(issuedAt, expiresIn, getIssuerUrl(),
                    grantCode.getSubject(), schema, grantCode.getScope());
        } else {
            List<Role> roleList = cell.getRoleListForAccount(grantCode.getSubject());
            aToken = new TransCellAccessToken(issuedAt, expiresIn, getIssuerUrl(),
                    getIssuerUrl() + "#" + grantCode.getSubject(), target, roleList, schema, grantCode.getScope());
        }

        // If scope is openid it returns id_token.
        IdToken idToken = null;
        Set<String> reqScopes = new HashSet<>(Arrays.asList(grantCode.getScope()));
        if (reqScopes.contains(OAuth2Helper.Scope.OPENID)) {
            CellCmp cellCmp = (CellCmp) davRsCmp.getDavCmp();
            CellKeysFile cellKeysFile = cellCmp.getCellKeys().getCellKeysFile();
            String subject = grantCode.getSubject();
            long issuedAtSec = issuedAt / AbstractOAuth2Token.MILLISECS_IN_A_SEC;
            long expiryTime = issuedAtSec + AbstractOAuth2Token.SECS_IN_AN_HOUR;
            idToken = new IdToken(
                    cellKeysFile.getKeyId(), AlgorithmUtils.RS_SHA_256_ALGO, getIssuerUrl(),
                    subject, schema, expiryTime, issuedAtSec, cellKeysFile.getPrivateKey());
        }

        return this.responseAuthSuccess(aToken, rToken, idToken, issuedAt);
    }

    private Response receiveSaml2(final String target, final String owner,
            final String schema, final String assertion, long expiresIn, long rTokenExpiresIn) {
        if (Key.TRUE_STR.equals(owner)) {
            //Do not promote unit user in token authentication
            throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER
                    .realm(this.cell.getUrl());
        }

        //Assertion null check
        if (assertion == null) {
            //If assertion is not set, it is regarded as a parse error
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl())
                .reason(new IllegalArgumentException("assertion not provided"));
        }

        //First to parse
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(assertion);
        } catch (TokenParseException e) {
            //When parsing fails
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            //Error in signature verification
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl()).reason(e);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR.reason(e);
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

        // Scope arbitration
        String[] scopes = this.cell.getScopeArbitrator(schema, OAuth2Helper.GrantType.SAML2_BEARER).request(tcToken.getScope()).getResults();

        // Create a refresh token based on the authentication information
        long issuedAt = new Date().getTime();
        VisitorRefreshToken rToken = new VisitorRefreshToken(
                tcToken.getId(), //Save ID of received SAML
                issuedAt, rTokenExpiresIn, getIssuerUrl(), tcToken.getSubject(),
                tcToken.getIssuer(), //Save receipt of SAML's
                tcToken.getRoleList(), //Save receipt of SAML's
                schema, scopes);

        //Ask CELL to decide the role of you from the role of TC issuer.
        List<Role> rolesHere = cell.getRoleListHere(tcToken);

        //Can I use the specified one for TODO schema?
        //TODO schema authentication is necessary.
        String schemaVerified = schema;

        //Authentication token issue processing
        //The target can be freely decided.
        IAccessToken aToken = null;

        // TODO


        if (target == null) {
            aToken = new VisitorLocalAccessToken(issuedAt, expiresIn, getIssuerUrl(),
                    tcToken.getSubject(), rolesHere, schemaVerified, scopes);
        } else {
            aToken = new TransCellAccessToken(issuedAt, expiresIn, getIssuerUrl(),
                    tcToken.getSubject(), target, rolesHere, schemaVerified, scopes);
        }
        return this.responseAuthSuccess(aToken, rToken, issuedAt);
    }

    /**
     * Authentication with Refresh token.
     * @param target
     * @param owner
     * @param schema
     * @param refreshToken
     * @param expiresIn accress token expires in time(ms).
     * @param rTokenExpiresIn refresh token expires in time(ms).
     * @return
     */
    private Response receiveRefresh(final String target, String owner, String schema,
            final String refreshToken, long expiresIn, long rTokenExpiresIn) {
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
        String tSchema = token.getSchema();

        if (!(Objects.equals(schema, tSchema) || schema == null && StringUtils.isEmpty(tSchema))) {
            if (schema == null) {
                throw PersoniumCoreAuthnException.CLIENT_AUTH_REQUIRED;
            }
            throw PersoniumCoreAuthnException.CLIENT_MISMATCH.params(tSchema, schema);
        }

        long issuedAt = new Date().getTime();

        if (Key.TRUE_STR.equals(owner)) {
            //You can be promoted only for your own cell refresh.
            if (token.getClass() != ResidentRefreshToken.class) {
                throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER.realm(this.cell.getUrl());
            }
            //Check unit escalation privilege setting
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(token.getSubject())) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER.realm(this.cell.getUrl());
            }
            //Do not promote cells for which the owner of the cell is not set.
            if (cell.getOwnerNormalized() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell.getUrl());
            }

            //uluut issuance processing
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt, expiresIn,
                    cell.getOwnerNormalized(), cell.getUnitUrl());

            return this.responseAuthSuccess(uluut, null, issuedAt);
        }

        //Regenerate AccessToken and RefreshToken from received Refresh Token
        IRefreshToken rToken = (IRefreshToken) token;
        rToken = rToken.refreshRefreshToken(issuedAt, rTokenExpiresIn);

        IAccessToken aToken = null;
        if (rToken instanceof ResidentRefreshToken) {
            String subject = rToken.getSubject();
            List<Role> roleList = cell.getRoleListForAccount(subject);
            aToken = rToken.refreshAccessToken(issuedAt, expiresIn, target, getIssuerUrl(), roleList);
        } else {
            //Ask CELL to determine the role of you from the role of the token issuer.
            List<Role> rolesHere = cell.getRoleListHere((IExtRoleContainingToken) rToken);
            aToken = rToken.refreshAccessToken(issuedAt, expiresIn, target,
                    getIssuerUrl(), rolesHere);
        }

        if (aToken instanceof TransCellAccessToken) {
            log.debug("reissuing TransCell Token");
            // aToken.addRole("admin");
            // return this.responseAuthSuccess(tcToken);
        }
        return this.responseAuthSuccess(aToken, rToken, issuedAt);
    }

    private Response responseAuthSuccess(final IAccessToken accessToken, final IRefreshToken refreshToken,
            long issuedAt) {
        return responseAuthSuccess(accessToken, refreshToken, null, issuedAt);
    }

    @SuppressWarnings("unchecked")
    private Response responseAuthSuccess(IAccessToken accessToken, IRefreshToken refreshToken, IdToken idToken,
            long issuedAt) {
        JSONObject resp = new JSONObject();
        resp.put(OAuth2Helper.Key.ACCESS_TOKEN, accessToken.toTokenString());
        resp.put(OAuth2Helper.Key.EXPIRES_IN, accessToken.expiresIn());
        if (accessToken.getScope() != null && accessToken.getScope().length > 0) {
            resp.put(OAuth2Helper.Key.SCOPE, AbstractOAuth2Token.Scope.toConcatValue(accessToken.getScope()));
        }
        if (refreshToken != null) {
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN, refreshToken.toTokenString());
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN, refreshToken.refreshExpiresIn());
        }
        if (idToken != null) {
            resp.put(OAuth2Helper.Key.ID_TOKEN, idToken.toTokenString());
        }
        resp.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
        ResponseBuilder rb = Response.ok().type(MediaType.APPLICATION_JSON_TYPE);
        if (accessToken.getTarget() != null) {
            resp.put(OAuth2Helper.Key.TARGET, accessToken.getTarget());
            rb.header(HttpHeaders.LOCATION, accessToken.getTarget() + "__token");
        }

        if (issueCookie) {
            //Set random UUID as p_cookie_peer
            String pCookiePeer = UUID.randomUUID().toString();
            //The p_cookie value to return to the header is encrypted
            String encodedCookieValue = accessToken.getCookieString(pCookiePeer,
                    AccessContext.getCookieCryptKey(this.cell.getId()));
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
            if (isRecordingAuthHistory) {
                AuthResourceUtils.updateAuthHistoryLastFileWithSuccess(
                        davRsCmp.getDavCmp().getFsPath(), accountId, issuedAt);
            }
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
            final String password, long expiresIn, long rTokenExpiresIn, String[] scope) {

        //Password check processing
        if (username == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.USERNAME);
        } else if (password == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.PASSWORD);
        }

        // In order to cope with the time exploiting attack,
        // even if the account is not found, processing is done uselessly.
        Account account = cell.getAccount(username);
        if (account != null) {
            accountId = account.id;
        }
        Boolean isLockedInterval = AuthResourceUtils.isLockedInterval(accountId);
        Boolean isLockedAccount = AuthResourceUtils.isLockedAccount(accountId);
        boolean passCheck = AuthUtils.isMatchePassword(account, password);

        if (account == null) {
            PersoniumCoreLog.Authn.FAILED_NO_SUCH_ACCOUNT.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }
        Boolean validIPAddress = account.acceptsIpAddress(this.ipaddress);
        boolean accountActive = account.isActive();
        boolean passwordChangeRequired = account.isPasswordChangeRequired();

        //Confirmation of Type value
        if (!account.isTypeBasic()) {
            //In order not to be abused in checking the existence of the account, an error response only for failure
            PersoniumCoreLog.Auth.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(
                    Account.TYPE_VALUE_BASIC, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        // Check if the target account records authentication history.
        isRecordingAuthHistory = ((CellRsCmp) davRsCmp).isRecordingAuthHistory(accountId, username);

        //Check valid authentication interval
        if (isLockedInterval) {
            //Update lock time of memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCount(accountId);
            if (isRecordingAuthHistory) {
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(davRsCmp.getDavCmp().getFsPath(), accountId);
            }
            PersoniumCoreLog.Authn.FAILED_BEFORE_AUTHENTICATION_INTERVAL.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        //Check account lock
        if (isLockedAccount) {
            //Update lock time of memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCount(accountId);
            if (isRecordingAuthHistory) {
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(davRsCmp.getDavCmp().getFsPath(), accountId);
            }
            PersoniumCoreLog.Authn.FAILED_ACCOUNT_IS_LOCKED.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        // Check valid IP address
        if (!validIPAddress) {
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCount(accountId);
            if (isRecordingAuthHistory) {
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(davRsCmp.getDavCmp().getFsPath(), accountId);
            }
            PersoniumCoreLog.Authn.FAILED_OUTSIDE_IP_ADDRESS_RANGE.params(
                    requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        if (!passCheck) {
            //Make lock on memcached
            AuthResourceUtils.registIntervalLock(accountId);
            AuthResourceUtils.countupFailedCount(accountId);
            if (isRecordingAuthHistory) {
                AuthResourceUtils.updateAuthHistoryLastFileWithFailed(davRsCmp.getDavCmp().getFsPath(), accountId);
            }
            PersoniumCoreLog.Authn.FAILED_INCORRECT_PASSWORD.params(
                    this.getUrl(), this.ipaddress, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
        }

        //Check account status.
        if (!accountActive) {
            if (passwordChangeRequired) {
                // Issue password change.
                issuePasswordChange(schema, username, rTokenExpiresIn, scope);
            } else {
                AuthResourceUtils.registIntervalLock(accountId);
                AuthResourceUtils.countupFailedCount(accountId);
                if (isRecordingAuthHistory) {
                    AuthResourceUtils.updateAuthHistoryLastFileWithFailed(davRsCmp.getDavCmp().getFsPath(), accountId);
                }
                PersoniumCoreLog.Authn.FAILED_ACCOUNT_IS_DEACTIVATED.params(
                        requestURIInfo.getRequestUri().toString(), this.ipaddress, username).writeLog();
                throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell.getUrl());
            }
        }
        ScopeArbitrator sa = this.cell.getScopeArbitrator(schema, OAuth2Helper.GrantType.PASSWORD);
        String[] scopes = sa.request(scope).getResults();

        return issueToken(target, owner, schema, username, expiresIn, rTokenExpiresIn, scopes);
    }

    /**
     * Issue password change.
     * Throws PersoniumCoreAuthnException for error handling.
     * @param schema schema
     * @param username user name
     * @param expiresIn expires in
     */
    private void issuePasswordChange(final String schema, final String username, long expiresIn, String[] scope) {
        // create account password change access token.
        long issuedAt = new Date().getTime();
        PasswordChangeAccessToken aToken = new PasswordChangeAccessToken(
                issuedAt, expiresIn, getIssuerUrl(), username, schema, scope);

        // get auth history. (non update auth history)
        AuthHistoryLastFile last = AuthResourceUtils.getAuthHistoryLast(
                davRsCmp.getDavCmp().getFsPath(), accountId);

        // throws password change required.
        PersoniumCoreAuthnException ex = PersoniumCoreAuthnException.PASSWORD_CHANGE_REQUIRED.realm(this.cell.getUrl());
        ex.addErrorJsonParam(OAuth2Helper.Key.ACCESS_TOKEN, aToken.toTokenString());
        ex.addErrorJsonParam("url", this.cell.getUrl() + "__mypassword");
        ex.addErrorJsonParam(OAuth2Helper.Key.LAST_AUTHENTICATED, last.getLastAuthenticated());
        ex.addErrorJsonParam(OAuth2Helper.Key.FAILED_COUNT, last.getFailedCount());
        throw ex;
    }

    private Response issueToken(final String target, final String owner,
            final String schema, final String username, long expiresIn, long rTokenExpiresIn, String[] scopes) {
        long issuedAt = new Date().getTime();

        if (Key.TRUE_STR.equals(owner)) {
            //Check unit escalation privilege setting
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(username)) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER
                        .realm(this.cell.getUrl());
            }
            //Do not promote cells for which the owner of the cell is not set.
            if (cell.getOwnerNormalized() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell.getUrl());
            }

            //uluut issuance processing
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt, expiresIn,
                    cell.getOwnerNormalized(), cell.getUnitUrl());
            return this.responseAuthSuccess(uluut, null, issuedAt);
        }

        ResidentRefreshToken rToken = new ResidentRefreshToken(issuedAt, rTokenExpiresIn,
                getIssuerUrl(), username, schema, scopes);

        //Create a response.
        if (target == null) {
            ResidentLocalAccessToken localToken = new ResidentLocalAccessToken(issuedAt, expiresIn,
                    getIssuerUrl(), username, schema, scopes);
            return this.responseAuthSuccess(localToken, rToken, issuedAt);
        } else {
            //Check that TODO SCHEMA is URL
            //Check that TODO TARGET is URL

            List<Role> roleList = cell.getRoleListForAccount(username);

            TransCellAccessToken tcToken = new TransCellAccessToken(issuedAt, expiresIn,
                    getIssuerUrl(), getIssuerUrl() + "#" + username, target, roleList, schema, scopes);
            return this.responseAuthSuccess(tcToken, rToken, issuedAt);
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

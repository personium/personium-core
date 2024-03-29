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
package io.personium.core.auth;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractLocalAccessToken;
import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.PasswordChangeAccessToken;
import io.personium.common.auth.token.ResidentLocalAccessToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.auth.token.VisitorLocalAccessToken;
import io.personium.common.auth.token.VisitorRefreshToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.cell.AuthResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * Access context information.
 * Based on the information extracted from the Authorization header, Access context information such as who the role is and what kind of application is accessed is generated and held in this object.
 * Generate permission by matching with ACL. cell, id, pw → AC AC → Token (issuer, subj, roles) Token → AC AC + ACL → permissions
 * In addition, this class holds the check result of authentication and authorization, and the coping method differs depending on the place to handle these information, so do not throw exceptions easily.
 */
public class AccessContext {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(AccessContext.class);

    /** Anonymous access : No Authorization header. */
    public static final String TYPE_ANONYMOUS = "anon";
    /** Access with invalid access token : Authorization header was present, but it was not authenticated. */
    public static final String TYPE_INVALID = "invalid";
    /** Access with master token : Authorization header content is unit master token. */
    public static final String TYPE_UNIT_MASTER = "unit-master";
    /** Access with basic authentication. */
    public static final String TYPE_BASIC = "basic";
    /** Access with Resident Local  Access Token. */
    public static final String TYPE_RESIDENT = "account";
    /** Access with password change access token. */
    public static final String TYPE_PASSWORD_CHANGE = "password-change";
    /** Access with visitor local access token. */
    public static final String TYPE_VISITOR = "local";
    /** Access with Trans Cell Access Token. */
    public static final String TYPE_TRANS = "trans";
    /** Access with Unit User Access token. */
    public static final String TYPE_UNIT_USER = "unit-user";
    /** Access with "Unit User Access token" assigned "UnitAdmin authority". */
    public static final String TYPE_UNIT_ADMIN = "unit-admin";
    /** Access by Unit Local Unit User Token. */
    public static final String TYPE_UNIT_LOCAL = "unit-local";

    /**
     * Unit Admin Role.
     * If this role is assigned to unit user, it can accept "X-Personium-Unit-User" header.
     */
    private static final String ROLE_UNIT_ADMIN = "UnitAdmin";
    /**
     * Cell Contents Reader Role.
     * If this role is assigned to unit user, contents of Cell can be read.
     */
    private static final String ROLE_CELL_CONTENTS_READER = "CellContentsReader";
    /**
     * Cell Contents Admin Role.
     * If this role is assigned to unit user, contents of Cell can be read/write.
     */
    private static final String ROLE_CELL_CONTENTS_ADMIN = "CellContentsAdmin";

    /**
     * Cause of invalid token.
     */
    private enum InvalidReason {
        /** Expired.*/
        expired,
        /** Authentication Scheme is invalid.*/
        authenticationScheme,
        /** The format of basic authentication header is invalid.*/
        basicAuthFormat,
        /** Basic authentication was attempted against resources that can not be authenticated.*/
        basicNotAllowed,
        /** Basic authentication error.*/
        basicAuthError,
        /** Authentication error (Account locked).*/
        basicAuthErrorInAccountLock,
        /** Cookie authentication error.*/
        cookieAuthError,
        /** Token parsing error.*/
        tokenParseError,
        /** Token signature error.*/
        tokenDsigError,
        /** Access with refresh token.*/
        refreshToken,
    }

    /** Access cell info. */
    private Cell cell;
    /** Access token type. */
    private String accessType;
    /** accessing user subject. */
    private String subject;
    /** access token issuer. */
    private String issuer;
    /** accessing app schema. */
    private String schema;
    /** scopes granted to the app. */
    private Set<String> scopes = new HashSet<>();
    /** CellPrivilege granted for App  as scope. */
    private Set<CellPrivilege> scopePrivileges = new HashSet<>();
    /** Roles granted for App as scope. */
    private Set<Role> scopeRoles = new HashSet<>();

    /** confidentialLevel. */
    private String confidentialLevel;
    /** Roles associated with access account. */
    private List<Role> roles = new ArrayList<Role>();
    /** base uri. */
    private String baseUri;
    /** uri info. */
    private UriInfo uriInfo;
    /** Cause of invalid token. */
    private InvalidReason invalidReason;
    /** Role associated with unit user. */
    private String unitUserRole;

    private AccessContext(String type, Cell cell, String baseUri, UriInfo uriInfo) {
        this(type, cell, baseUri, uriInfo, null);
    }

    private AccessContext(String type, Cell cell, String baseUri, UriInfo uriInfo, InvalidReason invalidReason) {
        this.accessType = type;
        this.cell = cell;
        this.baseUri = baseUri;
        this.uriInfo = uriInfo;
        this.invalidReason = invalidReason;

    }

    /**
     * Factory method, which creates and returns an object based on the value of the accessing Cell and Authorization header.
     * @param authzHeaderValue Authorization header value
     * @param requestURIInfo URI information of the request
     * @param pCookiePeer The value of p_cookie_peer specified in the request parameter
     * @param pCookieAuthValue Value specified for p_cookie in cookie
     * @param cell Accessing Cell
     * @param baseUri accessing baseUri
     * @param headerHost The value of Host in the request header
     * @param xPersoniumUnitUser X-Personium-UnitUser header
     * @return Generated AccessContext object
     */
    public static AccessContext create(String authzHeaderValue,
            UriInfo requestURIInfo, String pCookiePeer, String pCookieAuthValue,
            Cell cell, String baseUri, String headerHost, String xPersoniumUnitUser) {
        if (authzHeaderValue == null) {
            if (pCookiePeer == null || 0 == pCookiePeer.length()) {
                return new AccessContext(TYPE_ANONYMOUS, cell, baseUri, requestURIInfo);
            }
            String nonPortHost = headerHost.split(":")[0];

            // Cookie related processing requires no port number.
            String authToken = null;
            try {
                authToken = AbstractLocalAccessToken.parseCookie(pCookieAuthValue, pCookiePeer,
                        AccessContext.getCookieCryptKey(nonPortHost), true);
                return create(OAuth2Helper.Scheme.BEARER + " " + authToken,
                        requestURIInfo, null, null, cell, baseUri, headerHost, xPersoniumUnitUser);
            } catch (TokenParseException e) {
                return new AccessContext(
                        TYPE_INVALID, cell, baseUri, requestURIInfo, InvalidReason.cookieAuthError);
            }
        }

        // First branch depending on the authentication method
        if (authzHeaderValue.startsWith(OAuth2Helper.Scheme.BASIC)) {
            //Basic authentication
            return createBasicAuthz(authzHeaderValue, cell, baseUri, requestURIInfo);

        } else if (authzHeaderValue.startsWith(OAuth2Helper.Scheme.BEARER)) {
            //OAuth 2.0 authentication
            return createBearerAuthz(authzHeaderValue, cell, headerHost, baseUri,
                    requestURIInfo, headerHost, xPersoniumUnitUser);
        }
        return new AccessContext(TYPE_INVALID, cell, baseUri, requestURIInfo, InvalidReason.authenticationScheme);
    }

    /**
    * return an access context for WebSocket connection.
    * @param accessToken Authorization-token
    * @param cell Accessing cell
    * @param baseUri Accessing baseUri
    * @param host Accessing host
    * @return Created AccessContext Object
    */
   public static AccessContext createForWebSocket(
           String accessToken, Cell cell, String baseUri, String host) {
       String bearerAccessToken = OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + accessToken;
       return createBearerAuthz(bearerAccessToken, cell, host, baseUri, null, host, null);
   }

    /**
     * Get cell.
     * @return Access cell info
     */
    public Cell getCell() {
        return cell;
    }

    /**
     * Get accessType.
     * @return Access token type
     */
    public String getType() {
        return accessType;
    }

    /**
     * Get subject.
     * @return subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Get issuer.
     * @return issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Get schema.
     * This value is entered only when the application is authenticated.
     * @return schema
     */
    public String getSchema() {
        return schema;
    }
    /**
     * Get scopes.
     * @return scopes
     */
    public String[] getScope() {
        return this.scopes.toArray(new String[0]);
    }

    /**
     * Get confidentialLevel.
     * @return confidentialLevel
     */
    public String getConfidentialLevel() {
        return confidentialLevel;
    }

    /**
     * Get roles.
     * @return Roles associated with access account
     */
    public List<Role> getRoleList() {
        return roles;
    }

    /**
     * Get baseUri.
     * @return base uri
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Get uri info.
     * @return uri info
     */
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    /**
     * Get unitUserRole.
     * @return Role associated with unit user.
     */
    public String getUnitUserRole() {
        return unitUserRole;
    }

    /**
     * Merge with the parent's ACL information and judge whether access is possible.
     * @param acl ACL set in the resource
     * @param resourcePrivilege Privilege required to access the resource
     * @return boolean
     */
    public boolean hasSubjectPrivilegeForAcl(Acl acl, Privilege resourcePrivilege) {
        //No access if ACL is not set
        if (acl == null || acl.getAceList() == null) {
            return false;
        }
        //No access if Privilege is undefined
        if (resourcePrivilege == null) {
            return false;
        }

        //Acquire ROLE information from ACL and obtain authority
        if (acl.getAceList() == null) {
            return false;
        }
        for (Ace ace : acl.getAceList()) {
            //When an empty ace is set, it continues because there is no need for checking
            if (ace.getGrantedPrivilegeList().size() == 0 && ace.getPrincipalHref() == null) {
                continue;
            }
            //Accessible when Principal is all
            if (ace.getPrincipalAll() != null) {
                if (requireAcePrivilege(ace.getGrantedPrivilegeList(), resourcePrivilege)) {
                    return true;
                }
                continue;
            }
            //If a Role associated with Account does not exist, it is not accessible
            if (this.roles == null) {
                return false;
            }

            for (Role role : this.roles) {
                //Relative path roll URL correspondence
                String principalHref = getPrincipalHrefUrl(acl.getBase(), ace.getPrincipalHref());
                if (principalHref == null) {
                    return false;
                }

                //Detect setting corresponding to role
                if (role.toRoleInstanceURL().equals(principalHref)) {
                    //Confirm whether Root is set
                    if (ace.getGrantedPrivilegeList().contains(CellPrivilege.ROOT.getName())) {
                        return true;
                    }
                    //Detect setting corresponding to role
                    if (requireAcePrivilege(ace.getGrantedPrivilegeList(), resourcePrivilege)) {
                       return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Perform access control (only master token, unit user token, unit local unit user token accessible).
     * @return Whether access is possible
     */
    public boolean isUnitUserToken() {
        String type = getType();
        if (TYPE_UNIT_MASTER.equals(type) || TYPE_UNIT_ADMIN.equals(type)) {
            return true;
        } else if ((TYPE_UNIT_USER.equals(type) || TYPE_UNIT_LOCAL.equals(type))
                && getSubject().equals(getCell().getOwnerNormalized())) {
            //↑ Unit user, Unit For local unit users, this is valid only when the unit owner name included in the token and the cell owner to be processed match.
            return true;
        }
        return false;
    }


    /**
     * Perform access control (only master token, unit user token, unit local unit user token accessible).
     * @param resourcePrivilege Required authority
     * @return Whether access is possible
     */
    public boolean isUnitUserToken(Privilege resourcePrivilege) {
        String type = getType();
        if (TYPE_UNIT_MASTER.equals(type)) {
            return true;
        } else if (TYPE_UNIT_ADMIN.equals(type)
                || ((TYPE_UNIT_USER.equals(type) || TYPE_UNIT_LOCAL.equals(type)) //NOPMD - To maintain readability
                        && getSubject().equals(getCell().getOwnerNormalized()))) {
            // In the case of a UnitUser or UnitLocal, it is effective only when the unit owner name included
            // in the processing target cell owner and the token matches.

            if (ROLE_CELL_CONTENTS_ADMIN.equals(getUnitUserRole()) //NOPMD - To maintain readability
                    || ROLE_CELL_CONTENTS_READER.equals(getUnitUserRole())
                    && Privilege.ACCESS_TYPE_READ.equals(resourcePrivilege.getAccessType())) {
                // In the case of CellContentsReader, only when the necessary authority is READ is permitted.

                return true;
            }
        }
        return false;
    }

    /**
     * Get unit info (url, path_based_cellurl_enabled).
     * @return unit info
     */
    @SuppressWarnings("unchecked")
    public JSONObject getUnitMetadataJson() {
        JSONObject responseJson = new JSONObject();
        JSONObject unitMetadataJson = new JSONObject();
        unitMetadataJson.put("url", PersoniumUnitConfig.getBaseUrl());
        unitMetadataJson.put("path_based_cellurl_enabled", PersoniumUnitConfig.isPathBasedCellUrlEnabled());
        responseJson.put("unit", unitMetadataJson);
        return responseJson;
    }

    /**
     * Check that the subject in the TCAT is identical to the issuer.
     * @param acceptableAuthScheme
     *  Whether it is a call from a resource that does not allow basic authentication
     */
    public void checkCellIssueToken(AcceptableAuthScheme acceptableAuthScheme) {
        if (TYPE_TRANS.equals(this.getType())
                && this.getSubject().equals(this.getIssuer())) {
            //Valid only when the token ISSUER (issuer) and Subject (token owner) match.
            return;

        } else if (TYPE_INVALID.equals(this.getType())) {
            this.throwInvalidTokenException(acceptableAuthScheme);

        } else if (TYPE_ANONYMOUS.equals(this.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), acceptableAuthScheme);

        } else {
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * Make sure the token is your local cell token or your password change token.
     * @param acceptableAuthScheme Whether it is a call from a resource that does not allow basic authentication
     */
    public void checkResidentLocalOrPasswordChangeToken(AcceptableAuthScheme acceptableAuthScheme) {
        //Returning 401 if there is no illegal token or token designation
        //Returning 403 for a token other than your own cell local token
        if (TYPE_INVALID.equals(this.getType())) {
            this.throwInvalidTokenException(acceptableAuthScheme);
        } else if (TYPE_ANONYMOUS.equals(this.getType())
                || TYPE_BASIC.equals(this.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), acceptableAuthScheme);
        } else if (!TYPE_RESIDENT.equals(this.getType()) && !TYPE_PASSWORD_CHANGE.equals(this.getType())) {
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

        // Check that the subject is resident and the app scope include auth priv.
        if (TYPE_RESIDENT.equals(this.getType()) && !this.hasScopeCellPrivilege(CellPrivilege.AUTH)) {
            throw PersoniumCoreException.Auth.INSUFFICIENT_SCOPE.params(CellPrivilege.AUTH.getName());
        }
    }

    /**
     * Schema setting is checked to judge whether access is possible.
     * @param settingConfidentialLevel Schema level setting
     * @param box box
     * @param acceptableAuthScheme Whether it is a call from a resource that does not allow basic authentication
     */
    public void checkSchemaAccess(String settingConfidentialLevel, Box box, AcceptableAuthScheme acceptableAuthScheme) {
        // If accessed with a master, unit user token, or unit local unit user token,
        // Then pass through schema authentication.
        if (this.isUnitUserToken()) {
            return;
        }

        String tokenConfidentialLevel = this.getConfidentialLevel();

        //If the schema authentication level is not set (empty) or NONE, schema authentication check is unnecessary.
        if ("".equals(settingConfidentialLevel) || OAuth2Helper.SchemaLevel.NONE.equals(settingConfidentialLevel)) {
            return;
        }

        //Check the validity of the token
        //If the token is INVALID but the schema level setting has not been set, it is necessary to permit access, so check at this timing
        if (TYPE_INVALID.equals(this.getType())) {
            this.throwInvalidTokenException(acceptableAuthScheme);
        } else if (TYPE_ANONYMOUS.equals(this.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), acceptableAuthScheme);
        }

        //Schema check in token (only for access below Box level and other than Master Token)
        checkSchemaMatches(box);

        if (OAuth2Helper.SchemaLevel.PUBLIC.equals(settingConfidentialLevel)) {
            //If the setting is PUBLIC, it is OK if the schema of the token (ac) is PUBLIC and CONFIDENTIAL
            if (OAuth2Helper.SchemaLevel.PUBLIC.equals(tokenConfidentialLevel)
                    || OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(tokenConfidentialLevel)) {
                return;
            }
        } else if (OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(settingConfidentialLevel)
                && OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(tokenConfidentialLevel)) {
            //When the setting is CONFIDENTIAL, it is OK if the schema of the token (ac) is CONFIDENTIAL
            return;
        }
        throw PersoniumCoreException.Auth.INSUFFICIENT_SCHEMA_AUTHZ_LEVEL;
    }

    /**
     * Check if tokenSchema matches boxSchema.
     * @param box target box
     */
    public void checkSchemaMatches(Box box) {
        if (box != null) {
            String boxSchema = UriUtils.convertSchemeFromLocalUnitToHttp(box.getSchema());
            String tokenSchema = getSchema();

            // Do not check if box schema is not set.
            if (boxSchema != null) {
                if (tokenSchema == null) {
                    throw PersoniumCoreException.Auth.SCHEMA_AUTH_REQUIRED;
                } else if (!tokenSchema.replaceAll(OAuth2Helper.Key.CONFIDENTIAL_MARKER, "").equals(boxSchema)) {
                    // If token schema is Confidential, delete #c and compare.
                    throw PersoniumCoreException.Auth.SCHEMA_MISMATCH;
                }
            }
        }
    }

    /**
     * If basic authentication can not be done, it is checked whether basic authentication can be performed or not,
     *  and the state of Basic authentication disabled is set in context. <br />
     * In this method, only checking is performed, and whether or not it is actually an authentication error
     * is left to the access right check process of the structure.
     * @param box Box object (specify null for Cell level)
     */
    public void updateBasicAuthenticationStateForResource(Box box) {
        //No check unless it is basic authentication
        if (!TYPE_BASIC.equals(this.getType())) {
            return;
        }

        //Check if it is a resource that can be authenticated as Basic
        if (box == null) {
            invalidateBasicAuthentication();
            return;
        }

        //The main box has a schema but basic authentication is possible
        if (Box.MAIN_BOX_NAME.equals(box.getName())) {
            return;
        }

        //Check if it is a resource under Box with schema
        String boxSchema = box.getSchema();
        //If the schema of the box is set, Basic authentication is not accepted
        if (boxSchema != null && boxSchema.length() > 0) {
            invalidateBasicAuthentication();
            return;
        }
    }

    /**
     * Throw invalid token exceptions.
     * @param allowedAuthScheme Whether it is a call from a resource that does not allow basic authentication
     */
    public void throwInvalidTokenException(AcceptableAuthScheme allowedAuthScheme) {
        String realm = getRealm();

        switch (this.invalidReason) {
        case expired:
            throw PersoniumCoreAuthzException.EXPIRED_ACCESS_TOKEN.realm(realm, allowedAuthScheme);
        case authenticationScheme:
            throw PersoniumCoreAuthzException.INVALID_AUTHN_SCHEME.realm(realm, allowedAuthScheme);
        case basicAuthFormat:
            throw PersoniumCoreAuthzException.BASIC_AUTH_FORMAT_ERROR.realm(realm, allowedAuthScheme);
        case basicNotAllowed:
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), allowedAuthScheme);
        case basicAuthError:
            throw PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED.realm(realm, allowedAuthScheme);
        case basicAuthErrorInAccountLock:
            throw PersoniumCoreAuthzException.BASIC_AUTHENTICATION_FAILED_IN_ACCOUNT_LOCK.realm(realm,
                    allowedAuthScheme);
        case cookieAuthError:
            throw PersoniumCoreAuthzException.COOKIE_AUTHENTICATION_FAILED.realm(realm, allowedAuthScheme);
        case tokenParseError:
            throw PersoniumCoreAuthzException.TOKEN_PARSE_ERROR.realm(realm, allowedAuthScheme);
        case refreshToken:
            throw PersoniumCoreAuthzException.ACCESS_WITH_REFRESH_TOKEN.realm(realm, allowedAuthScheme);
        case tokenDsigError:
            throw PersoniumCoreAuthzException.TOKEN_DISG_ERROR.realm(realm, allowedAuthScheme);
        default:
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
    }

    /**
     * Generate the key for token encryption / decryption at the time of cookie authentication.
     * @param host request host
     * @return Key used for encryption / decryption
     */
    public static String getCookieCryptKey(String host) {
        //Because PCS is stateless access, it is difficult to change the key for each user,
        //Generate a key based on the host name of URI.
        //Process the host name.
        return host.replaceAll("[aiueo]", "#");
    }

    /**
     * Realm information (URL of Cell) is generated.
     * @return realm information
     */
    public String getRealm() {
        return getRealm(this.baseUri, this.cell);
    }

    /**
     * Factory method. It creates and returns an object by basic authentication based on the value of Cell and Authorization header being accessed.
     * @param authzHeaderValue Authorization header value
     * @param cell Accessing Cell
     * @param baseUri accessing baseUri
     * @param uriInfo uri info
     * @return Generated AccessContext object
     */
    private static AccessContext createBasicAuthz(String authzHeaderValue, Cell cell, String baseUri, UriInfo uriInfo) {

        //Basic authentication is not possible for access to Unit control
        if (cell == null) {
            return new AccessContext(TYPE_INVALID, null, baseUri, uriInfo, InvalidReason.basicAuthError);
        }

        String[] idpw = CommonUtils.parseBasicAuthzHeader(authzHeaderValue);
        if (idpw == null) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.basicAuthFormat);
        }

        String username = idpw[0];
        String password = idpw[1];

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.basicAuthError);
        }

        //Check valid authentication interval
        String accountId = oew.getUuid();
        Boolean isLock = AuthResourceUtils.isLockedAccount(accountId);
        if (isLock) {
            //Update lock time of memcached
            AuthResourceUtils.registIntervalLock(accountId);
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.basicAuthErrorInAccountLock);
        }

        boolean authnSuccess = cell.authenticateAccount(oew, password);
        if (!authnSuccess) {
            //Make lock on memcached
            AuthResourceUtils.registIntervalLock(accountId);
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.basicAuthError);
        }
        //If authentication succeeds
        AccessContext ret = new AccessContext(TYPE_BASIC, cell, baseUri, uriInfo);
        ret.subject = username;
        //Acquire role information
        ret.roles = cell.getRoleListForAccount(username);
        // TODO Make configurable
        ret.addScope("root");

        return ret;
    }
    public void addScope(String scopeStr) {
        this.scopes.add(scopeStr);
        if (scopeStr.startsWith("https://") || scopeStr.startsWith("http://")) {
            try {
                this.scopeRoles.add(Role.createFromRoleInstanceUrl(scopeStr));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            CellPrivilege prv = CellPrivilege.get(CellPrivilege.class, scopeStr);
            if (prv != null) {
                this.scopePrivileges.add(prv);
            }
        }

    }

    /**
     * Factory method, which creates and returns an object by Bearer authentication based on the value of Cell and Authorization header being accessed.
     * @param authzHeaderValue Authorization header value
     * @param cell Accessing Cell
     * @param baseUri accessing baseUri
     * @param uriInfo uri info
     * @param xPersoniumUnitUser X-Personium-UnitUser header
     * @return Generated AccessContext object
     */
    private static AccessContext createBearerAuthz(String authzHeaderValue, Cell cell,
            String requestURIHost, String baseUri, UriInfo uriInfo, String host, String xPersoniumUnitUser) {
        // Bearer
        //If the value of the authentication token does not start with [Bearer], it is determined to be an invalid token
        if (!authzHeaderValue.startsWith(OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX)) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.tokenParseError);
        }
        String accessToken = authzHeaderValue.substring(OAuth2Helper.Scheme.BEARER.length() + 1);
        //Detection of master token
        //In the master token specification, if there is no X-Personium-UnitUser header, it is treated as a master token
        if (PersoniumUnitConfig.getMasterToken().equals(accessToken) && xPersoniumUnitUser == null) {
            AccessContext ret = new AccessContext(TYPE_UNIT_MASTER, cell, baseUri, uriInfo);
            return ret;
        } else if (PersoniumUnitConfig.getMasterToken().equals(accessToken) && xPersoniumUnitUser != null) {
            //Demote from master to unit user token with X-Personium-UnitUser header specification
            AccessContext ret = new AccessContext(TYPE_UNIT_USER, cell, baseUri, uriInfo);
            ret.subject = xPersoniumUnitUser;
            return ret;
        }
        //Since, Cell level.
        AbstractOAuth2Token tk = null;
        try {
            String issuer = null;
            String unitHost = host;
            if (cell != null) {
                issuer = cell.getUrl();
                unitHost = cell.getUnitUrl();
            }
            tk = AbstractOAuth2Token.parse(accessToken, issuer, unitHost);
        } catch (TokenParseException e) {
            //Because I failed in Perth
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.tokenParseError);
        } catch (TokenDsigException e) {
            //Because certificate validation failed
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.tokenDsigError);
        } catch (TokenRootCrtException e) {
            //Error setting root CA certificate
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }
        log.debug(tk.getClass().getCanonicalName());
        //If it is not an AccessToken, ie a refresh token.
        if (!(tk instanceof IAccessToken) || tk instanceof VisitorRefreshToken) {
            //Access by refresh token is not permitted.
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.refreshToken);
        }

        //Checking the validity of tokens
        if (tk.isExpired()) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, uriInfo, InvalidReason.expired);
        }

        AccessContext ret = new AccessContext(null, cell, baseUri, uriInfo);
        if (tk instanceof ResidentLocalAccessToken) {
            ret.accessType = TYPE_RESIDENT;
            //Retrieve role information.
            String acct = tk.getSubject();
            ret.roles = cell.getRoleListForAccount(acct);
            if (ret.roles == null) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(baseUri, cell),
                        AcceptableAuthScheme.BEARER);
            }
            //In AccessContext, Subject is normalized to URL.
            ret.subject = cell.getUrl() + "#" + tk.getSubject();
            ret.issuer = tk.getIssuer();
        } else if (tk instanceof PasswordChangeAccessToken) {
            ret.accessType = TYPE_PASSWORD_CHANGE;
            ret.subject = cell.getUrl() + "#" + tk.getSubject();
            ret.issuer = tk.getIssuer();
        } else if (tk instanceof VisitorLocalAccessToken) {
            VisitorLocalAccessToken clat = (VisitorLocalAccessToken) tk;
            ret.accessType = TYPE_VISITOR;
            //Acquire roll information and pack it.
            ret.roles = clat.getRoleList();
            ret.subject = tk.getSubject();
            ret.issuer = tk.getIssuer();
        } else if (tk instanceof UnitLocalUnitUserToken) {
            ret.accessType = TYPE_UNIT_LOCAL;
            ret.subject = tk.getSubject();
            ret.issuer = tk.getIssuer();
            //Unit local unit user token does not concern schema authentication, so return here
            return ret;
        } else {
            TransCellAccessToken tca = (TransCellAccessToken) tk;
            ret = createAccessContext(cell, requestURIHost, baseUri, uriInfo, xPersoniumUnitUser, tca);
            if (TYPE_UNIT_USER.equals(ret.accessType)) {
                return ret;
            }
        }
        ret.schema = tk.getSchema();
        if (ret.schema == null || "".equals(ret.schema)) {
            ret.confidentialLevel = OAuth2Helper.SchemaLevel.NONE;
        } else if (ret.schema.endsWith(OAuth2Helper.Key.CONFIDENTIAL_MARKER)) {
            ret.confidentialLevel = OAuth2Helper.SchemaLevel.CONFIDENTIAL;
        } else {
            ret.confidentialLevel = OAuth2Helper.SchemaLevel.PUBLIC;
        }
        if (tk.getScope() != null) {
            ret.scopes.addAll(Arrays.asList(tk.getScope()));
            for (String scope : ret.scopes) {
                if (OAuth2Helper.Scope.OPENID.contentEquals(scope)) {
                    continue;
                }
                if (scope.startsWith("https://") || scope.startsWith("http://")) {
                    try {
                        ret.scopeRoles.add(Role.createFromRoleInstanceUrl(scope));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    CellPrivilege prv = CellPrivilege.get(CellPrivilege.class, scope);
                    if (prv != null) {
                        ret.scopePrivileges.add(prv);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * It checks whether necessary privilege is set to Privilege of ACL.
     * @param acePrivileges List of Privilege settings configured on the ACE
     * @param resourcePrivilege Required authority
     * @return Checkability
     */
    private boolean requireAcePrivilege(List<String> acePrivileges, Privilege resourcePrivilege) {
        for (String aclPrivilege : acePrivileges) {
            Privilege priv = Privilege.get(resourcePrivilege.getClass(), aclPrivilege);
            if (priv != null
                    && priv.includes(resourcePrivilege)) {
                //Notes
                //Privilege.get ($ {setting the ACL}) includes ($ {necessary value to access the resource})) {
                return true;
            }
        }
        return false;
    }

    /**
     * Relative path resolution of configuration role URL.
     * The value of the xml: base attribute of the @param base ACL
     * @param principal Href principal-Href of ACL
     * @return
     */
    private String getPrincipalHrefUrl(String base, String principalHref) {
        String result = null;
        if (base != null && !"".equals(base)) {
            //Relative path resolution
            try {
                URI url = new URI(base);
                result = url.resolve(principalHref).toString();
            } catch (URISyntaxException e) {
                return null;
            }
        } else {
            //If xml: base is not set, treat it as full path setting to href
            result = principalHref;
        }
        return result;
    }

    /**
     * Set basic authentication invalid state in the context.
     */
    private void invalidateBasicAuthentication() {
        this.accessType = TYPE_INVALID;
        this.invalidReason = InvalidReason.basicNotAllowed;
        this.subject = null;
        this.roles = new ArrayList<Role>();
    }

    /**
     * Generate realm information (Cell's URL) (internal use).
     * @return realm information
     */
    private static String getRealm(String baseUri, Cell cellobj) {
        //In case of unit control resource cell needs to be judged because it becomes null
        String realm = baseUri;
        if (null != cellobj) {
            realm = cellobj.getUrl();
        }
        return realm;
    }

    /**
     * Creates and returns AccessContext object by TransCellAccesToken.
     * @param cell Accessing Cell
     * @param requestURIHost
     * @param baseUri accessing baseUri
     * @param uriInfo uri info
     * @param xPersoniumUnitUser X-Personium-UnitUser header
     * @param tca based token
     * @return Generated AccessContext object
     */
    private static AccessContext createAccessContext(Cell cell, String requestURIHost,
            String baseUri, UriInfo uriInfo, String xPersoniumUnitUser, TransCellAccessToken tca) {
        AccessContext ret = new AccessContext(null, cell, baseUri, uriInfo);

        //In the case of TCAT, check the possibility of being a unit user token
        //TCAT is unit user token Condition 1: Target is your own unit.
        //TCAT is unit user token Condition 2: Issuer is UnitUserCell which exists in the setting.

        String escapedBaseUri = baseUri;
        if (requestURIHost.contains(".")) {
            String cellName = requestURIHost.split("\\.")[0];
            escapedBaseUri = baseUri.replaceFirst(cellName + "\\.", "");
        }

        String issuer = tca.getIssuer();
        if ((tca.getTarget().equals(baseUri) || tca.getTarget().equals(escapedBaseUri))
                && (PersoniumUnitConfig.checkUnitUserIssuers(issuer)
                        || PersoniumUnitConfig.checkUnitUserIssuers(issuer))) {
            //Processing unit user tokens
            ret.accessType = TYPE_UNIT_USER;
            ret.subject = tca.getSubject();
            ret.issuer = tca.getIssuer();

            //Take role information and if you have unit admin roll, promote to unit admin.
            List<Role> roles = tca.getRoleList();
            Role unitAdminRole = new Role(ROLE_UNIT_ADMIN, Box.MAIN_BOX_NAME, null, tca.getIssuer());
            String unitAdminRoleUrl = unitAdminRole.toRoleInstanceURL();
            Role cellContentsReaderRole = new Role(ROLE_CELL_CONTENTS_READER, Box.MAIN_BOX_NAME,
                    null, tca.getIssuer());
            String cellContentsReaderUrl = cellContentsReaderRole.toRoleInstanceURL();
            Role cellContentsAdminRole = new Role(ROLE_CELL_CONTENTS_ADMIN, Box.MAIN_BOX_NAME,
                    null, tca.getIssuer());
            String cellContentsAdminUrl = cellContentsAdminRole.toRoleInstanceURL();

            String unitUserRole = null;
            for (Role role : roles) {
                String roleUrl = role.toRoleInstanceURL();
                if (unitAdminRoleUrl.equals(roleUrl)) {
                    if (xPersoniumUnitUser == null) {
                        // If there is no X-Personium-UnitUser header, UnitAdmin
                        ret = new AccessContext(TYPE_UNIT_ADMIN, cell, baseUri, uriInfo);
                    } else {
                        // If there is an X-Personium-UnitUser header, UnitUser
                        ret.subject = xPersoniumUnitUser;
                    }
                } else if (cellContentsReaderUrl.equals(roleUrl) && unitUserRole == null) {
                    // If roles are not set, set the CellContentsReader role.
                    // To preferentially set the CellContentsAdmin role.
                    unitUserRole = ROLE_CELL_CONTENTS_READER;
                } else if (cellContentsAdminUrl.equals(roleUrl)) {
                    // Set the CellContentsAdmin role.
                    unitUserRole = ROLE_CELL_CONTENTS_ADMIN;
                }
            }
            ret.unitUserRole = unitUserRole;

            //Unit user token does not concern schema authentication, so return here
            return ret;
        } else if (cell == null) {
            //Because only the master token and the unit user token allow tokens with Cell empty at unit level, treat them as invalid tokens.
            throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
        } else {
            //TCAT processing
            ret.accessType = TYPE_TRANS;
            ret.subject = tca.getSubject();
            ret.issuer = tca.getIssuer();

            //Obtaining the Role corresponding to the token
            ret.roles = cell.getRoleListHere(tca);
            return ret;
        }
    }

    /**
     * Check if this access context has the cell level privilege.
     * @param cellPriv .
     * @return .
     */
    public boolean hasScopeCellPrivilege(CellPrivilege cellPriv) {
        for (CellPrivilege scopePriv : this.scopePrivileges) {
            if (scopePriv.includes(cellPriv)) {
                return true;
            }
        }
        // TODO scope role check
        return false;
    }
}

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
package io.personium.core.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.AbstractOAuth2Token;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenDsigException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenParseException;
import io.personium.common.auth.token.AbstractOAuth2Token.TokenRootCrtException;
import io.personium.common.auth.token.AccountAccessToken;
import io.personium.common.auth.token.CellLocalAccessToken;
import io.personium.common.auth.token.IAccessToken;
import io.personium.common.auth.token.LocalToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.PersoniumCoreUtils;
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
 * Authorization ヘッダから取り出した情報に基づいて 誰がどいう役割で、どういうアプリからアクセスしているのかといったAccess文脈情報を 生成し、これをこのオブジェクトに保持する。
 * ACLとの突合でpermissionを生成する。 cell, id, pw → AC AC → Token(issuer, subj, roles) Token → AC AC + ACL → permissions
 * なお、本クラスでは認証・認可のチェック結果を保持しており、これらの情報を扱う場所によって対処方法が異なるため、安易に例外はスローしないこと。
 */
public class AccessContext {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(AccessContext.class);

    /** Anonymous access : No Authorization header. */
    public static final String TYPE_ANONYMOUS = "anon";
    /** Access with invalid permissions : Authorization header was present, but it was not authenticated. */
    public static final String TYPE_INVALID = "invalid";
    /** Access with master token : Authorization header content is master token. */
    public static final String TYPE_UNIT_MASTER = "unit-master";
    /** Access by basic authentication. */
    public static final String TYPE_BASIC = "basic";
    /** Access by cell local access token. */
    public static final String TYPE_LOCAL = "local";
    /** Access by TransCell Access Token. */
    public static final String TYPE_TRANS = "trans";
    /** Access by Unit User Access token. */
    public static final String TYPE_UNIT_USER = "unit-user";
    /** Access by "Unit User Access token" assigned "UnitAdmin authority". */
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
        /** 有効期限切れ. */
        expired,
        /** Authentication Schemeが無効. */
        authenticationScheme,
        /** ベーシック認証ヘッダのフォーマットが無効. */
        basicAuthFormat,
        /** ベーシック認証できないリソースに対してベーシック認証しようとした. */
        basicNotAllowed,
        /** ベーシック認証エラー. */
        basicAuthError,
        /** 認証エラー(Accountロック中). */
        basicAuthErrorInAccountLock,
        /** Cookie認証エラー. */
        cookieAuthError,
        /** トークンパースエラー. */
        tokenParseError,
        /** トークン署名エラー. */
        tokenDsigError,
        /** リフレッシュトークンでのアクセス. */
        refreshToken,
    }

    /** Access cell info. */
    private Cell cell;
    /** Access token type. */
    private String accessType;
    /** subject. */
    private String subject;
    /** issuer. */
    private String issuer;
    /** schema. */
    private String schema;
    /** confidentialLevel. */
    private String confidentialLevel;
    /** Roles associated with access account. */
    private List<Role> roles = new ArrayList<Role>();
    /** base uri. */
    private String baseUri;
    /** Cause of invalid token. */
    private InvalidReason invalidReason;
    /** Role associated with unit user. */
    private String unitUserRole;

    private AccessContext(String type, Cell cell, String baseUri) {
        this(type, cell, baseUri, null);
    }

    private AccessContext(String type, Cell cell, String baseUri, InvalidReason invalidReason) {
        this.accessType = type;
        this.cell = cell;
        this.baseUri = baseUri;
        this.invalidReason = invalidReason;
    }

    /**
     * ファクトリメソッド. アクセスしているCellとAuthorizationヘッダの値を元にオブジェクトを生成して返します.
     * @param authzHeaderValue Authorizationヘッダの値
     * @param requestURIInfo リクエストのURI情報
     * @param pCookiePeer リクエストパラメタに指定された p_cookie_peerの値
     * @param pCookieAuthValue クッキー内 p_cookieに指定されている値
     * @param cell アクセスしているCell
     * @param baseUri アクセスしているbaseUri
     * @param host リクエストヘッダのHostの値
     * @param xPersoniumUnitUser X-Personium-UnitUserヘッダ
     * @return 生成されたAccessContextオブジェクト
     */
    public static AccessContext create(String authzHeaderValue,
            UriInfo requestURIInfo, String pCookiePeer, String pCookieAuthValue,
            Cell cell, String baseUri, String host, String xPersoniumUnitUser) {
        if (authzHeaderValue == null) {
            if (pCookiePeer == null || 0 == pCookiePeer.length()) {
                return new AccessContext(TYPE_ANONYMOUS, cell, baseUri);
            }
            // クッキー認証の場合
            // クッキー内の値を復号化した値を取得
            try {
                if (null == pCookieAuthValue) {
                    return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.cookieAuthError);
                }
                String decodedCookieValue = LocalToken.decode(pCookieAuthValue,
                        UnitLocalUnitUserToken.getIvBytes(
                                AccessContext.getCookieCryptKey(requestURIInfo.getBaseUri())));
                int separatorIndex = decodedCookieValue.indexOf("\t");
                String peer = decodedCookieValue.substring(0, separatorIndex);
                // クッキー内の情報から authorizationHeader相当のトークンを取得
                String authToken = decodedCookieValue.substring(separatorIndex + 1);
                if (pCookiePeer.equals(peer)) {
                    // 再帰呼び出しで適切な AccessContextを生成する。
                    return create(OAuth2Helper.Scheme.BEARER + " " + authToken,
                            requestURIInfo, null, null, cell, baseUri, host, xPersoniumUnitUser);
                } else {
                    return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.cookieAuthError);
                }
            } catch (TokenParseException e) {
                return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.cookieAuthError);
            }
        }

        // TODO V1.1 ここはキャッシュできる部分。ここでキャッシュから取得すればいい。

        // まずは認証方式によって分岐

        if (authzHeaderValue.startsWith(OAuth2Helper.Scheme.BASIC)) {
            // Basic認証
            return createBasicAuthz(authzHeaderValue, cell, baseUri);

        } else if (authzHeaderValue.startsWith(OAuth2Helper.Scheme.BEARER)) {
            // OAuth2.0認証
            return createBearerAuthz(authzHeaderValue, cell, baseUri, host, xPersoniumUnitUser);
        }
        return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.authenticationScheme);
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
       return createBearerAuthz(bearerAccessToken, cell, baseUri, host, null);
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
     * Get unitUserRole.
     * @return Role associated with unit user.
     */
    public String getUnitUserRole() {
        return unitUserRole;
    }

    /**
     * 親のACL情報とマージし、アクセス可能か判断する.
     * @param acl リソースに設定されているALC
     * @param resourcePrivilege リソースにアクセスするために必要なPrivilege
     * @param cellUrl セルURL
     * @return boolean
     */
    public boolean requirePrivilege(Acl acl, Privilege resourcePrivilege, String cellUrl) {
        // ACLが未設定だったらアクセス不可
        if (acl == null || acl.getAceList() == null) {
            return false;
        }
        // Privilegeが未定義だったらアクセス不可
        if (resourcePrivilege == null) {
            return false;
        }

        // ACLからROLE情報を取得し、権限を取得
        if (acl.getAceList() == null) {
            return false;
        }
        for (Ace ace : acl.getAceList()) {
            // 空のaceが設定されている場合はチェックの必要がないためがcontinueする
            if (ace.getGrantedPrivilegeList().size() == 0 && ace.getPrincipalHref() == null) {
                continue;
            }
            // Principalがallの場合、アクセス可
            if (ace.getPrincipalAll() != null) {
                if (requireAcePrivilege(ace.getGrantedPrivilegeList(), resourcePrivilege)) {
                    return true;
                }
                continue;
            }
            // Accountに紐付いたRoleが存在しない場合は、アクセス不可
            if (this.roles == null) {
                return false;
            }

            for (Role role : this.roles) {
                // 相対パスロールURL対応
                String principalHref = getPrincipalHrefUrl(acl.getBase(), ace.getPrincipalHref());
                if (principalHref == null) {
                    return false;
                }

                // ロールに対応している設定を検出
                if (role.localCreateUrl(cellUrl).equals(principalHref)) {
                    // Rootが設定されているかどうかの確認
                    if (ace.getGrantedPrivilegeList().contains(CellPrivilege.ROOT.getName())) {
                        return true;
                    }
                    // ロールに対応している設定を検出
                    if (requireAcePrivilege(ace.getGrantedPrivilegeList(), resourcePrivilege)) {
                       return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * アクセス制御を行う(マスタートークン、ユニットユーザトークン、ユニットローカルユニットユーザトークンのみアクセス可能とする).
     * @return アクセス可能かどうか
     */
    public boolean isUnitUserToken() {
        String type = getType();
        if (TYPE_UNIT_MASTER.equals(type)
                || TYPE_UNIT_ADMIN.equals(type)) {
            return true;
        } else if ((TYPE_UNIT_USER.equals(type) || TYPE_UNIT_LOCAL.equals(type))
                && getSubject().equals(getCell().getOwner())) {
            // ↑ユニットユーザ、ユニットローカルユニットユーザの場合は処理対象のセルオーナーとトークンに含まれるユニットユーザ名が一致した場合のみ有効。
            return true;
        }
        return false;
    }

    /**
     * アクセス制御を行う(マスタートークン、ユニットユーザトークン、ユニットローカルユニットユーザトークンのみアクセス可能とする).
     * @param resourcePrivilege 必要な権限
     * @return アクセス可能かどうか
     */
    public boolean isUnitUserToken(Privilege resourcePrivilege) {
        String type = getType();
        if (TYPE_UNIT_MASTER.equals(type)) {
            return true;
        } else if (TYPE_UNIT_ADMIN.equals(type)
                || ((TYPE_UNIT_USER.equals(type) || TYPE_UNIT_LOCAL.equals(type)) //NOPMD - To maintain readability
                        && getSubject().equals(getCell().getOwner()))) {
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
     * アクセス制御を行う(SubjectがCELLのトークンのみアクセス可能とする).
     * @param acceptableAuthScheme Basic認証を許可しないリソースからの呼び出しであるかどうか
     */
    public void checkCellIssueToken(AcceptableAuthScheme acceptableAuthScheme) {
        if (TYPE_TRANS.equals(this.getType())
                && this.getSubject().equals(this.getIssuer())) {
            // トークンのISSUER（発行者）とSubject（トークンの持ち主）が一致した場合のみ有効。
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
     * トークンが自分セルローカルトークンか確認する.
     * @param cellname cell
     * @param acceptableAuthScheme Basic認証を許可しないリソースからの呼び出しであるかどうか
     */
    public void checkMyLocalToken(Cell cellname, AcceptableAuthScheme acceptableAuthScheme) {
        // 不正なトークンorトークン指定がない場合401を返却
        // 自分セルローカルトークン以外のトークンの場合403を返却
        if (TYPE_INVALID.equals(this.getType())) {
            this.throwInvalidTokenException(acceptableAuthScheme);
        } else if (TYPE_ANONYMOUS.equals(this.getType())
                || TYPE_BASIC.equals(this.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), acceptableAuthScheme);
        } else if (!(this.getType() == TYPE_LOCAL
        && this.getCell().getName().equals(cellname.getName()))) {
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * スキーマ設定をチェックしアクセス可能か判断する.
     * @param settingConfidentialLevel スキーマレベル設定
     * @param box box
     * @param acceptableAuthScheme Basic認証を許可しないリソースからの呼び出しであるかどうか
     */
    public void checkSchemaAccess(String settingConfidentialLevel, Box box, AcceptableAuthScheme acceptableAuthScheme) {
        // マスタートークンかユニットユーザ、ユニットローカルユニットユーザの場合はスキーマ認証をスルー。
        if (this.isUnitUserToken()) {
            return;
        }

        String tokenConfidentialLevel = this.getConfidentialLevel();

        // スキーマ認証レベルが未設定（空）かNONEの場合はスキーマ認証チェック不要。
        if ("".equals(settingConfidentialLevel) || OAuth2Helper.SchemaLevel.NONE.equals(settingConfidentialLevel)) {
            return;
        }

        // トークンの有効性チェック
        // トークンがINVALIDでもスキーマレベル設定が未設定だとアクセスを許可する必要があるのでこのタイミングでチェック
        if (TYPE_INVALID.equals(this.getType())) {
            this.throwInvalidTokenException(acceptableAuthScheme);
        } else if (TYPE_ANONYMOUS.equals(this.getType())) {
            throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(), acceptableAuthScheme);
        }

        // トークン内のスキーマチェック(Boxレベル以下かつマスタートークン以外のアクセスの場合のみ)
        checkSchemaMatches(box);

        if (OAuth2Helper.SchemaLevel.PUBLIC.equals(settingConfidentialLevel)) {
            // 設定がPUBLICの場合はトークン（ac）のスキーマがPUBLICとCONFIDENTIALならOK
            if (OAuth2Helper.SchemaLevel.PUBLIC.equals(tokenConfidentialLevel)
                    || OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(tokenConfidentialLevel)) {
                return;
            }
        } else if (OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(settingConfidentialLevel)
                && OAuth2Helper.SchemaLevel.CONFIDENTIAL.equals(tokenConfidentialLevel)) {
            // 設定がCONFIDENTIALの場合はトークン（ac）のスキーマがCONFIDENTIALならOK
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
            String boxSchema = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), box.getSchema());
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
     * Basic認証できるかどうかチェックし、Basic認証できない場合は、コンテキストにBasic認証不可の状態を設定する.<br />
     * 本メソッドではチェックのみを行い、実際に認証エラーとするかどうかは構造のアクセス権限チェック処理に任せる。
     * @param box Boxオブジェクト(Cellレベルの場合はnullを指定)
     */
    public void updateBasicAuthenticationStateForResource(Box box) {
        // Basic認証でなければチェック不要
        if (!TYPE_BASIC.equals(this.getType())) {
            return;
        }

        // Basic認証できるリソースであるかチェックする
        if (box == null) {
            invalidateBasicAuthentication();
            return;
        }

        // メインボックスはスキーマを持っているがBasic認証可能
        if (Role.DEFAULT_BOX_NAME.equals(box.getName())) {
            return;
        }

        // スキーマ有のBox配下のリソースであるかチェックする
        String boxSchema = box.getSchema();
        // ボックスのスキーマが設定されている場合はBasic認証を受け付けない
        if (boxSchema != null && boxSchema.length() > 0) {
            invalidateBasicAuthentication();
            return;
        }
    }

    /**
     * 無効なトークンの例外を投げ分ける.
     * @param allowedAuthScheme Basic認証を許可しないリソースからの呼び出しであるかどうか
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
     * クッキー認証の際の、トークン暗号化・複合化のキーを生成する.
     * @param uri リクエストURI
     * @return 暗号化・複合化に用いるためのキー
     */
    public static String getCookieCryptKey(URI uri) {
        // PCSではステートレスアクセスなので、ユーザ毎にキーを変更することは難しいため、
        // URIのホスト名を基にキーを生成する。
        // ホスト名を加工する。
        return uri.getHost().replaceAll("[aiueo]", "#");
    }

    /**
     * realm情報(CellのURL)を生成する.
     * @return realm情報
     */
    public String getRealm() {
        return getRealm(this.baseUri, this.cell);
    }

    /**
     * ファクトリメソッド. アクセスしているCellとAuthorizationヘッダの値を元にBasic認証にてオブジェクトを生成して返します.
     * @param authzHeaderValue Authorizationヘッダの値
     * @param cell アクセスしているCell
     * @param baseUri アクセスしているbaseUri
     * @return 生成されたAccessContextオブジェクト
     */
    private static AccessContext createBasicAuthz(String authzHeaderValue, Cell cell, String baseUri) {

        // Unitコントロールへのアクセスの場合は、Basic認証不可
        if (cell == null) {
            return new AccessContext(TYPE_INVALID, null, baseUri, InvalidReason.basicAuthError);
        }

        String[] idpw = PersoniumCoreUtils.parseBasicAuthzHeader(authzHeaderValue);
        if (idpw == null) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.basicAuthFormat);
        }

        String username = idpw[0];
        String password = idpw[1];

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.basicAuthFormat);
        }

        // Accountのロックチェック
        String accountId = oew.getUuid();
        Boolean isLock = AuthResourceUtils.isLockedAccount(accountId);
        if (isLock) {
            // memcachedのロック時間を更新
            AuthResourceUtils.registAccountLock(accountId);
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.basicAuthErrorInAccountLock);
        }

        boolean authnSuccess = cell.authenticateAccount(oew, password);
        if (!authnSuccess) {
            // memcachedにロックを作成
            AuthResourceUtils.registAccountLock(accountId);
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.basicAuthError);
        }
        // 認証して成功なら
        AccessContext ret = new AccessContext(TYPE_BASIC, cell, baseUri);
        ret.subject = username;
        // ロール情報を取得
        ret.roles = cell.getRoleListForAccount(username);
        return ret;
    }

    /**
     * ファクトリメソッド. アクセスしているCellとAuthorizationヘッダの値を元にBearer認証にてオブジェクトを生成して返します.
     * @param authzHeaderValue Authorizationヘッダの値
     * @param cell アクセスしているCell
     * @param baseUri アクセスしているbaseUri
     * @param xPersoniumUnitUser X-Personium-UnitUserヘッダ
     * @return 生成されたAccessContextオブジェクト
     */
    private static AccessContext createBearerAuthz(String authzHeaderValue, Cell cell,
            String baseUri, String host, String xPersoniumUnitUser) {
        // Bearer
        // 認証トークンの値が[Bearer ]で開始していなければ不正なトークンと判断する
        if (!authzHeaderValue.startsWith(OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX)) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.tokenParseError);
        }
        String accessToken = authzHeaderValue.substring(OAuth2Helper.Scheme.BEARER.length() + 1);
        // マスタートークンの検出
        // マスタートークン指定で、X-Personium-UnitUserヘッダがなかった場合はマスタートークン扱い
        if (PersoniumUnitConfig.getMasterToken().equals(accessToken) && xPersoniumUnitUser == null) {
            AccessContext ret = new AccessContext(TYPE_UNIT_MASTER, cell, baseUri);
            return ret;
        } else if (PersoniumUnitConfig.getMasterToken().equals(accessToken) && xPersoniumUnitUser != null) {
            // X-Personium-UnitUserヘッダ指定だとマスターからユニットユーザトークンへの降格
            AccessContext ret = new AccessContext(TYPE_UNIT_USER, cell, baseUri);
            ret.subject = xPersoniumUnitUser;
            return ret;
        }
        // 以降、Cellレベル。
        AbstractOAuth2Token tk = null;
        try {
            String issuer = null;
            if (cell != null) {
                issuer = cell.getUrl();
            }
            tk = AbstractOAuth2Token.parse(accessToken, issuer, host);
        } catch (TokenParseException e) {
            // パースに失敗したので
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.tokenParseError);
        } catch (TokenDsigException e) {
            // 証明書検証に失敗したので
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.tokenDsigError);
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }
        log.debug(tk.getClass().getCanonicalName());
        // AccessTokenではない場合、すなわちリフレッシュトークン。
        if (!(tk instanceof IAccessToken) || tk instanceof TransCellRefreshToken) {
            // リフレッシュトークンでのアクセスは認めない。
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.refreshToken);
        }

        // トークンの有効期限チェック
        if (tk.isExpired()) {
            return new AccessContext(TYPE_INVALID, cell, baseUri, InvalidReason.expired);
        }

        AccessContext ret = new AccessContext(null, cell, baseUri);
        if (tk instanceof AccountAccessToken) {
            ret.accessType = TYPE_LOCAL;
            // ロール情報をとってくる。
            String acct = tk.getSubject();
            ret.roles = cell.getRoleListForAccount(acct);
            if (ret.roles == null) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(getRealm(baseUri, cell),
                        AcceptableAuthScheme.BEARER);
            }
            // AccessContextではSubjectはURLに正規化。
            ret.subject = cell.getUrl() + "#" + tk.getSubject();
            ret.issuer = tk.getIssuer();
        } else if (tk instanceof CellLocalAccessToken) {
            CellLocalAccessToken clat = (CellLocalAccessToken) tk;
            ret.accessType = TYPE_LOCAL;
            // ロール情報を取得して詰める。
            ret.roles = clat.getRoles();
            ret.subject = tk.getSubject();
            ret.issuer = tk.getIssuer();
        } else if (tk instanceof UnitLocalUnitUserToken) {
            ret.accessType = TYPE_UNIT_LOCAL;
            ret.subject = tk.getSubject();
            ret.issuer = tk.getIssuer();
            // ユニットローカルユニットユーザトークンはスキーマ認証関係無いのでここで復帰
            return ret;
        } else {
            TransCellAccessToken tca = (TransCellAccessToken) tk;

            // TCATの場合はユニットユーザトークンである可能性をチェック
            // TCATがユニットユーザトークンである条件１：Targetが自分のユニットであること。
            // TCATがユニットユーザトークンである条件２：Issuerが設定に存在するUnitUserCellであること。
            if (tca.getTarget().equals(baseUri) && PersoniumUnitConfig.checkUnitUserIssuers(tca.getIssuer(), baseUri)) {
                // ユニットユーザトークンの処理
                ret.accessType = TYPE_UNIT_USER;
                ret.subject = tca.getSubject();
                ret.issuer = tca.getIssuer();

                // ロール情報をとってきて、ユニットアドミンロールがついていた場合、ユニットアドミンに昇格させる。
                List<Role> roles = tca.getRoles();
                Role unitAdminRole = new Role(ROLE_UNIT_ADMIN, Box.DEFAULT_BOX_NAME, null, tca.getIssuer());
                String unitAdminRoleUrl = unitAdminRole.createUrl();
                Role cellContentsReaderRole = new Role(ROLE_CELL_CONTENTS_READER, Box.DEFAULT_BOX_NAME,
                        null, tca.getIssuer());
                String cellContentsReaderUrl = cellContentsReaderRole.createUrl();
                Role cellContentsAdminRole = new Role(ROLE_CELL_CONTENTS_ADMIN, Box.DEFAULT_BOX_NAME,
                        null, tca.getIssuer());
                String cellContentsAdminUrl = cellContentsAdminRole.createUrl();

                for (Role role : roles) {
                    String roleUrl = role.createUrl();
                    if (unitAdminRoleUrl.equals(roleUrl)) {
                        if (xPersoniumUnitUser == null) {
                            // If there is no X-Personium-UnitUser header, UnitAdmin
                            ret = new AccessContext(TYPE_UNIT_ADMIN, cell, baseUri);
                        } else {
                            // If there is an X-Personium-UnitUser header, UnitUser
                            ret.subject = xPersoniumUnitUser;
                        }
                    } else if (cellContentsReaderUrl.equals(roleUrl) && ret.unitUserRole == null) {
                        // If roles are not set, set the CellContentsReader role.
                        // To preferentially set the CellContentsAdmin role.
                        ret.unitUserRole = ROLE_CELL_CONTENTS_READER;
                    } else if (cellContentsAdminUrl.equals(roleUrl)) {
                        // Set the CellContentsAdmin role.
                        ret.unitUserRole = ROLE_CELL_CONTENTS_ADMIN;
                    }
                }

                // ユニットユーザトークンはスキーマ認証関係無いのでここで復帰
                return ret;
            } else if (cell == null) {
                // ユニットレベルでCellが空のトークンを許すのはマスタートークンと、ユニットユーザトークンだけなので無効なトークン扱いにする。
                throw PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;
            } else {
                // TCATの処理
                ret.accessType = TYPE_TRANS;
                ret.subject = tca.getSubject();
                ret.issuer = tca.getIssuer();

                // トークンに対応するRoleの取得
                ret.roles = cell.getRoleListHere((TransCellAccessToken) tk);
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

        // TODO Cache Cell Level
        return ret;
    }

    /**
     * 必要な権限がACLのPrivilegeに設定されているかチェックする.
     * @param acePrivileges ACEに設定されたPrivilege設定のリスト
     * @param resourcePrivilege 必要な権限
     * @return チェック可否
     */
    private boolean requireAcePrivilege(List<String> acePrivileges, Privilege resourcePrivilege) {
        for (String aclPrivilege : acePrivileges) {
            Privilege priv = Privilege.get(resourcePrivilege.getClass(), aclPrivilege);
            if (priv != null
                    && priv.includes(resourcePrivilege)) {
                // メモ
                // Privilege.get(${ACLの設定}).includes(${リソースにアクセスするために必要な値})) {
                return true;
            }
        }
        return false;
    }

    /**
     * 設定ロールURLの相対パス解決.
     * @param base ACLのxml:base属性の値
     * @param principalHref ACLのprincipal-Href
     * @return
     */
    private String getPrincipalHrefUrl(String base, String principalHref) {
        String result = null;
        if (base != null && !"".equals(base)) {
            // 相対パスの解決
            try {
                URI url = new URI(base);
                result = url.resolve(principalHref).toString();
            } catch (URISyntaxException e) {
                return null;
            }
        } else {
            // xml:baseが未設定の場合、hrefにフルパス設定されていると扱う
            result = principalHref;
        }
        return result;
    }

    /**
     * コンテキストにBasic認証不可の状態を設定する.
     */
    private void invalidateBasicAuthentication() {
        this.accessType = TYPE_INVALID;
        this.invalidReason = InvalidReason.basicNotAllowed;
        this.subject = null;
        this.roles = new ArrayList<Role>();
    }

    /**
     * realm情報(CellのURL)を生成する(内部用).
     * @return realm情報
     */
    private static String getRealm(String baseUri, Cell cellobj) {
        // ユニットコントロールリソースの場合はcellがnullになるため判定が必要
        String realm = baseUri;
        if (null != cellobj) {
            realm = cellobj.getUrl();
        }
        return realm;
    }

}

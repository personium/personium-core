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
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.FormParam;
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
import io.personium.common.auth.token.TransCellRefreshToken;
import io.personium.common.auth.token.UnitLocalUnitUserToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreAuthnException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.AuthUtils;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.plugin.PluginInfo;
import io.personium.core.plugin.PluginManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.utils.UriUtils;
import io.personium.plugin.base.Plugin;
import io.personium.plugin.base.PluginException;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthenticatedIdentity;

/**
 * JAX-RS Resource class for authentication.
 */
public class TokenEndPointResource {

    static Logger log = LoggerFactory.getLogger(TokenEndPointResource.class);

    private final Cell cell;
    private final DavRsCmp davRsCmp;
    private boolean issueCookie = false;
    private UriInfo requestURIInfo;
    // パスワード認証時に使用するAccountのUUID。パスワード認証後に最終ログイン時刻の更新に使用する。
    private String accountId;

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
     * <li>p_targetにURLが書いてあれば、そのCELLをTARGETのCELLとしてtransCellTokenを発行する。</li>
     * <li>scopeがなければCellLocalを発行する。</li>
     * </ul>
     * @param uriInfo  URI information
     * @param authzHeader Authorization Header
     * @param grantType  One of query parameters
     * @param username One of query parameters
     * @param password One of query parameters
     * @param pTarget One of query parameters
     * @param pOwner One of query parameters
     * @param assertion One of query parameters
     * @param refreshToken One of query parameters
     * @param code One of query parameters
     * @param clientId One of query parameters
     * @param clientSecret One of query parameters
     * @param pCookie One of query parameters
     * @param host Host header
     * @param formParams Body parameters
     * @return JAX-RS Response Object
     */
    @POST
    public final Response token(@Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authzHeader,
            @FormParam(Key.GRANT_TYPE) final String grantType,
            @FormParam(Key.USERNAME) final String username,
            @FormParam(Key.PASSWORD) final String password,
            @FormParam(Key.TARGET) final String pTarget,
            @FormParam(Key.OWNER) final String pOwner,
            @FormParam(Key.ASSERTION) final String assertion,
            @FormParam(Key.REFRESH_TOKEN) final String refreshToken,
            @FormParam(Key.CODE) final String code,
            @FormParam(Key.CLIENT_ID) final String clientId,
            @FormParam(Key.CLIENT_SECRET) final String clientSecret,
            @FormParam("p_cookie") final String pCookie,
            @HeaderParam(HttpHeaders.HOST) final String host,
            MultivaluedMap<String, String> formParams) {

        // Accept unit local scheme url.
        String target = UriUtils.convertSchemeFromLocalUnitToHttp(
                cell.getUnitUrl(), pTarget);
        // p_target がURLでない場合はヘッダInjectionの脆弱性を産んでしまう。(改行コードが入っているなど)
        target = this.checkPTarget(target);

        if (null != pTarget) {
            issueCookie = false;
        } else {
            issueCookie = Boolean.parseBoolean(pCookie);
            requestURIInfo = uriInfo;
        }

        String schema = null;
        // まずはClient認証したいのかを確認
        // ScopeもauthzHeaderもclientIdもない場合はClient認証しないとみなす。
        if (clientId != null || authzHeader != null) {
            schema = this.clientAuth(clientId, clientSecret, authzHeader);
        }

        if (OAuth2Helper.GrantType.PASSWORD.equals(grantType)) {
            // 通常のパスワード認証
            Response response = this.handlePassword(target, pOwner, host,
                    schema, username, password);

            // パスワード認証が成功した場合はアカウントの最終ログイン時刻を更新する
            // パスワード認証が成功した場合のみ、ここを通る(handlePassword内でエラーが発生すると、例外がthrowされる)
            if (PersoniumUnitConfig.getAccountLastAuthenticatedEnable()) {
                // Accountのスキーマ情報を取得する
                PersoniumODataProducer producer = ModelFactory.ODataCtl
                        .cellCtl(cell);
                EdmEntitySet esetAccount = producer.getMetadata()
                        .getEdmEntitySet(Account.EDM_TYPE_NAME);
                OEntityKey originalKey = OEntityKey.parse("('" + username
                        + "')");
                // 最終ログイン時刻の変更をProducerに依頼(このメソッド内でロックを取得・解放)
                producer.updateLastAuthenticated(esetAccount, originalKey,
                        accountId);
            }
            return response;
        } else if (OAuth2Helper.GrantType.SAML2_BEARER.equals(grantType)) {
            return this.receiveSaml2(target, pOwner, schema, assertion);
        } else if (OAuth2Helper.GrantType.REFRESH_TOKEN.equals(grantType)) {
            return this.receiveRefresh(target, pOwner, schema, host, refreshToken);
        } else if (OAuth2Helper.GrantType.AUTHORIZATION_CODE.equals(grantType)) {
            return receiveCode(target, pOwner, schema, host, code);
        } else {
            // Call Auth Plugins
            return this.callAuthPlugins(grantType, formParams, target, pOwner,
                    schema, host);
        }
    }

    // TODO 仮実装
    private Response receiveCode(final String target, String owner, String schema,
            final String host, final String code) {
        if (code == null) {
            // codeが未設定の場合、パースエラーとみなす
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
            token = (CellLocalAccessToken) AbstractOAuth2Token.parse(code, cell.getUrl(), host);
        } catch (TokenParseException e) {
            // パースに失敗したので
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            // 証明書検証に失敗したので
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
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

        // 受け取ったTokenから AccessTokenとRefreshTokenを再生成
        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt, cell.getUrl(), token.getSubject(), schema);
        IAccessToken aToken = null;
        if (target == null) {
            aToken = new CellLocalAccessToken(issuedAt, cell.getUrl(), token.getSubject(), token.getRoles(), schema);
        } else {
            List<Role> roleList = cell.getRoleListForAccount(token.getSubject());
            aToken = new TransCellAccessToken(issuedAt, cell.getUrl(), cell.getUrl() + "#" + token.getSubject(),
                    target, roleList, schema);
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
     * @param host
     * @return Response
     */
    private Response callAuthPlugins(String grantType, MultivaluedMap<String, String> params,
            String target, String owner, String schema, String host) {
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
        } catch (PluginException pe) {
            throw PersoniumCoreException.create(pe);
        } catch (Exception e) {
            // Unexpected exception throwed from "Plugin", create default PersoniumCoreAuthException
            // and set reason from catched Exception.
            throw PersoniumCoreException.Plugin.UNEXPECTED_ERROR.reason(e);
        }

        if (ai == null) {
            throw PersoniumCoreException.Plugin.PLUGIN_AUTHN_FAILED;
        }
        String accountName = ai.getAccountName();
        String accountType = ai.getAccountType();
        if (accountName == null || accountType == null) {
            throw PersoniumCoreAuthnException.Plugin.PLUGIN_AUTHN_FAILED;
        }

        // If the Account shown in IdToken does not exist in cell.
        OEntityWrapper idTokenUserOew = cell.getAccount(accountName);
        if (idTokenUserOew == null) {
            // アカウントの存在確認に悪用されないように、失敗の旨のみのエラー応答
            PersoniumCoreLog.OIDC.NO_SUCH_ACCOUNT.params(accountName).writeLog();
            throw PersoniumCoreAuthnException.Plugin.PLUGIN_AUTHN_FAILED;
        }

        // Confirm if OidC is included in Type when there is Account.
        if (!AuthUtils.getAccountType(idTokenUserOew).contains(accountType)) {
            // アカウントの存在確認に悪用されないように、失敗の旨のみのエラー応答
            PersoniumCoreLog.OIDC.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(accountType,
                    accountName).writeLog();
            throw PersoniumCoreAuthnException.Plugin.PLUGIN_AUTHN_FAILED;
        }

        // When processing is normally completed, issue a token.
        return this.issueToken(target, owner, host, schema, accountName);
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
                    // p_targetがURLでない場合はエラー
                    throw PersoniumCoreAuthnException.INVALID_TARGET
                            .realm(this.cell.getUrl());
                }
            } catch (MalformedURLException e) {
                // p_targetがURLでない場合はエラー
                throw PersoniumCoreAuthnException.INVALID_TARGET
                        .realm(this.cell.getUrl());
            }
        }
        return target;
    }

    /**
     * クライアント認証処理.
     * @param clientId
     * @param clientSecret
     * @param authzHeader
     * @return null： Client認証に失敗した場合。
     */
    private String clientAuth(final String clientId, final String clientSecret,
            final String authzHeader) {
        String id = clientId;
        String pw = clientSecret;
        if (pw == null) {
            pw = "";
        }

        // authzHeaderのパース
        if (authzHeader != null) {
            String[] idpw = PersoniumCoreUtils
                    .parseBasicAuthzHeader(authzHeader);
            if (idpw != null) {
                // authzHeaderの指定を優先
                id = idpw[0];
                pw = idpw[1];
            } else {
                throw PersoniumCoreAuthnException.AUTH_HEADER_IS_INVALID
                        .realm(cell.getUrl());
            }
        }

        // pwのチェック
        // ・PWはSAMLトークンなので、これをパースする。
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(pw);
        } catch (TokenParseException e) {
            // パースの失敗
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.CLIENT_SECRET_PARSE_ERROR.realm(
                    cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            // 署名検証エラー
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID
                    .realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(
                    e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        // ・有効期限チェック
        if (tcToken.isExpired()) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_EXPIRED.realm(cell
                    .getUrl());
        }

        // ・IssuerがIDと等しいことを確認
        if (!id.equals(tcToken.getIssuer())) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_ISSUER_MISMATCH
                    .realm(cell.getUrl());
        }

        // トークンのターゲットが自分でない場合はエラー応答
        if (!tcToken.getTarget().equals(cell.getUrl())) {
            throw PersoniumCoreAuthnException.CLIENT_SECRET_TARGET_WRONG
                    .realm(cell.getUrl());
        }

        // ロールが特殊(confidential)な値だったら#cを付与
        String confidentialRoleUrl = String.format(
                OAuth2Helper.Key.CONFIDENTIAL_ROLE_URL_FORMAT,
                tcToken.getIssuer(), Box.DEFAULT_BOX_NAME);
        for (Role role : tcToken.getRoles()) {
            if (confidentialRoleUrl.equals(role.createUrl())) {
                // 認証成功。
                return id + OAuth2Helper.Key.CONFIDENTIAL_MARKER;
            }
        }
        // 認証成功。
        return id;
    }

    private Response receiveSaml2(final String target, final String owner,
            final String schema, final String assertion) {
        if (Key.TRUE_STR.equals(owner)) {
            // トークン認証でのユニットユーザ昇格はさせない
            throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER
                    .realm(this.cell.getUrl());
        }

        // assertionのnullチェック
        if (assertion == null) {
            // assertionが未設定の場合、パースエラーとみなす
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell
                    .getUrl());
        }

        // まずはパースする
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(assertion);
        } catch (TokenParseException e) {
            // パース失敗時
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell
                    .getUrl());
        } catch (TokenDsigException e) {
            // 署名検証でエラー
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage())
                    .writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID
                    .realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(
                    e.getMessage()).writeLog();
            throw PersoniumCoreException.Auth.ROOT_CA_CRT_SETTING_ERROR;
        }

        // Tokenの検証
        // 1．有効期限チェック
        if (tcToken.isExpired()) {
            throw PersoniumCoreAuthnException.TOKEN_EXPIRED.realm(this.cell
                    .getUrl());
        }

        // トークンのターゲットが自分でない場合はエラー応答
        try {
            if (!(AuthResourceUtils.checkTargetUrl(this.cell, tcToken))) {
                throw PersoniumCoreAuthnException.TOKEN_TARGET_WRONG.realm(
                        this.cell.getUrl()).params(tcToken.getTarget());
            }
        } catch (MalformedURLException e) {
            throw PersoniumCoreAuthnException.TOKEN_TARGET_WRONG.realm(
                    this.cell.getUrl()).params(tcToken.getTarget());
        }

        // 認証は成功 -------------------------------

        // 認証できた情報をもとにリフレッシュトークンを作成
        long issuedAt = new Date().getTime();
        TransCellRefreshToken rToken = new TransCellRefreshToken(
                tcToken.getId(), // IDは受領SAMLのものを保存
                issuedAt, cell.getUrl(), tcToken.getSubject(),
                tcToken.getIssuer(), // 受領SAMLのものを保存
                tcToken.getRoles(), // 受領SAMLのものを保存
                schema);

        // CELLに依頼してTC発行元のロールから自分のところのロールを決定する。
        List<Role> rolesHere = cell.getRoleListHere(tcToken);

        // TODO schema は指定のものをつかっていい？
        // TODO schema認証は必要。
        String schemaVerified = schema;

        // 認証トークン発行処理
        // ターゲットは自由に決めてよい。
        IAccessToken aToken = null;
        if (target == null) {
            aToken = new CellLocalAccessToken(issuedAt, cell.getUrl(),
                    tcToken.getSubject(), rolesHere, schemaVerified);
        } else {
            aToken = new TransCellAccessToken(UUID.randomUUID().toString(),
                    issuedAt, cell.getUrl(), tcToken.getSubject(), target,
                    rolesHere, schemaVerified);
        }
        return this.responseAuthSuccess(aToken, rToken);
    }

    /**
     * Authentication with Refresh token.
     * @param target
     * @param owner
     * @param schema
     * @param host
     * @param refreshToken
     * @return
     */
    private Response receiveRefresh(final String target, String owner, String schema,
            final String host, final String refreshToken) {
        if (refreshToken == null) {
            // refreshTokenが未設定の場合、パースエラーとみなす
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl());
        }

        AbstractOAuth2Token token;
        try {
            token = AbstractOAuth2Token.parse(refreshToken, cell.getUrl(), host);
        } catch (TokenParseException e) {
            // パースに失敗したので
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_PARSE_ERROR.realm(this.cell.getUrl()).reason(e);
        } catch (TokenDsigException e) {
            // 証明書検証に失敗したので
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            throw PersoniumCoreAuthnException.TOKEN_DSIG_INVALID.realm(this.cell.getUrl());
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
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
            // 自分セルリフレッシュの場合のみ昇格できる。
            if (token.getClass() != CellLocalRefreshToken.class) {
                throw PersoniumCoreAuthnException.TC_ACCESS_REPRESENTING_OWNER.realm(this.cell.getUrl());
            }
            // ユニット昇格権限設定のチェック
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(token.getSubject())) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER.realm(this.cell.getUrl());
            }
            // セルのオーナーが未設定のセルに対しては昇格させない。
            if (cell.getOwner() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell.getUrl());
            }

            // uluut発行処理
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt,
                    UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                    cell.getOwner(), host);

            return this.responseAuthSuccess(uluut, null);
        } else {
            // 受け取ったRefresh Tokenから AccessTokenとRefreshTokenを再生成
            IRefreshToken rToken = (IRefreshToken) token;
            rToken = rToken.refreshRefreshToken(issuedAt);

            IAccessToken aToken = null;
            if (rToken instanceof CellLocalRefreshToken) {
                String subject = rToken.getSubject();
                List<Role> roleList = cell.getRoleListForAccount(subject);
                aToken = rToken.refreshAccessToken(issuedAt, target, cell.getUrl(), roleList, schema);
            } else {
                // CELLに依頼してトークン発行元のロールから自分のところのロールを決定する。
                List<Role> rolesHere = cell.getRoleListHere((IExtRoleContainingToken) rToken);
                aToken = rToken.refreshAccessToken(issuedAt, target, cell.getUrl(), rolesHere, schema);
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
    private Response responseAuthSuccess(final IAccessToken accessToken,
            final IRefreshToken refreshToken) {
        JSONObject resp = new JSONObject();
        resp.put(OAuth2Helper.Key.ACCESS_TOKEN, accessToken.toTokenString());
        resp.put(OAuth2Helper.Key.EXPIRES_IN, accessToken.expiresIn());
        if (refreshToken != null) {
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN,
                    refreshToken.toTokenString());
            resp.put(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN,
                    refreshToken.refreshExpiresIn());
        }
        resp.put(OAuth2Helper.Key.TOKEN_TYPE, OAuth2Helper.Scheme.BEARER);
        ResponseBuilder rb = Response.ok()
                .type(MediaType.APPLICATION_JSON_TYPE);
        if (accessToken.getTarget() != null) {
            resp.put(OAuth2Helper.Key.TARGET, accessToken.getTarget());
            rb.header(HttpHeaders.LOCATION, accessToken.getTarget() + "__token");
        }

        if (issueCookie) {
            String tokenString = accessToken.toTokenString();
            // p_cookie_peerとして、ランダムなUUIDを設定する
            String pCookiePeer = UUID.randomUUID().toString();
            String cookieValue = pCookiePeer + "\t" + tokenString;
            // ヘッダに返却するp_cookie値は、暗号化する
            String encodedCookieValue = LocalToken.encode(cookieValue,
                    UnitLocalUnitUserToken.getIvBytes(AccessContext
                            .getCookieCryptKey(requestURIInfo.getBaseUri())));
            // Cookieのバージョン(0)を指定
            int version = 0;
            String path = getCookiePath();

            // Cookieを作成し、レスポンスヘッダに返却する
            Cookie cookie = new Cookie("p_cookie", encodedCookieValue, path,
                    requestURIInfo.getBaseUri().getHost(), version);
            rb.cookie(new NewCookie(cookie, "", -1, PersoniumUnitConfig
                    .isHttps()));
            // レスポンスボディの"p_cookie_peer"を返却する
            resp.put("p_cookie_peer", pCookiePeer);
        }
        return rb.entity(resp.toJSONString()).build();
    }

    /**
     * Cookieに設定するパスを作成する.
     * @return Cookieに設定するパス
     */
    private String getCookiePath() {
        String cellUrl = cell.getUrl();
        try {
            URL url = new URL(cellUrl);
            return url.getPath();
        } catch (MalformedURLException e) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell
                    .getUrl());
        }
    }

    private Response handlePassword(final String target, final String owner,
            final String host, final String schema, final String username,
            final String password) {

        // パスワードのCheck処理
        if (username == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.USERNAME);
        } else if (password == null) {
            throw PersoniumCoreAuthnException.REQUIRED_PARAM_MISSING.realm(
                    this.cell.getUrl()).params(Key.PASSWORD);
        }

        OEntityWrapper oew = cell.getAccount(username);
        if (oew == null) {
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell
                    .getUrl());
        }

        // Typeの値確認
        if (!AuthUtils.isAccountTypeBasic(oew)) {
            // アカウントの存在確認に悪用されないように、失敗の旨のみのエラー応答
            PersoniumCoreLog.Auth.UNSUPPORTED_ACCOUNT_GRANT_TYPE.params(
                    Account.TYPE_VALUE_BASIC, username).writeLog();
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell
                    .getUrl());
        }

        // 最終ログイン時刻を更新するために、UUIDをクラス変数にひかえておく
        accountId = (String) oew.getUuid();

        // ロックのチェック
        Boolean isLock = AuthResourceUtils.isLockedAccount(accountId);
        if (isLock) {
            // memcachedのロック時間を更新
            AuthResourceUtils.registAccountLock(accountId);
            throw PersoniumCoreAuthnException.ACCOUNT_LOCK_ERROR
                    .realm(this.cell.getUrl());
        }

        boolean authSuccess = cell.authenticateAccount(oew, password);

        if (!authSuccess) {
            // memcachedにロックを作成
            AuthResourceUtils.registAccountLock(accountId);
            throw PersoniumCoreAuthnException.AUTHN_FAILED.realm(this.cell
                    .getUrl());
        }

        return issueToken(target, owner, host, schema, username);
    }

    private Response issueToken(final String target, final String owner,
            final String host, final String schema, final String username) {
        long issuedAt = new Date().getTime();

        if (Key.TRUE_STR.equals(owner)) {
            // ユニット昇格権限設定のチェック
            if (!this.davRsCmp.checkOwnerRepresentativeAccounts(username)) {
                throw PersoniumCoreAuthnException.NOT_ALLOWED_REPRESENT_OWNER
                        .realm(this.cell.getUrl());
            }
            // セルのオーナーが未設定のセルに対しては昇格させない。
            if (cell.getOwner() == null) {
                throw PersoniumCoreAuthnException.NO_CELL_OWNER.realm(this.cell
                        .getUrl());
            }

            // uluut発行処理
            UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(issuedAt,
                    UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR
                            * MILLISECS_IN_AN_HOUR, cell.getOwner(), host);
            return this.responseAuthSuccess(uluut, null);
        }

        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt,
                CellLocalRefreshToken.REFRESH_TOKEN_EXPIRES_HOUR
                        * MILLISECS_IN_AN_HOUR, cell.getUrl(), username, schema);

        // レスポンスをつくる。
        if (target == null) {
            AccountAccessToken localToken = new AccountAccessToken(issuedAt,
                    AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR
                            * MILLISECS_IN_AN_HOUR, cell.getUrl(), username,
                    schema);
            return this.responseAuthSuccess(localToken, rToken);
        } else {
            // TODO SCHEMAがURLであることのチェック
            // TODO TARGETがURLであることのチェック

            List<Role> roleList = cell.getRoleListForAccount(username);

            TransCellAccessToken tcToken = new TransCellAccessToken(
                    cell.getUrl(), cell.getUrl() + "#" + username, target,
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
        return PersoniumCoreUtils.responseBuilderForOptions(HttpMethod.POST)
                .build();
    }
}

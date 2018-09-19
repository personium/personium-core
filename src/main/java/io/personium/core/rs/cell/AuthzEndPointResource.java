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
 * ImplicitFlow認証処理を司るJAX-RSリソース.
 */
public class AuthzEndPointResource {

    private static final int COOKIE_MAX_AGE = 86400;

    private static final String PROFILE_JSON_NAME = "/profile.json";

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(AuthzEndPointResource.class);

    private final Cell cell;
    private final CellRsCmp cellRsCmp;

    /**
     * ログインフォーム_Javascriptソースファイル.
     */
    private final String jsFileName = "ajax.js";

    /**
     * ログインフォーム_初期表示メッセージ.
     */
    private final String passFormMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0002");

    /**
     * ログインフォーム_ユーザID・パスワード未入力のメッセージ.
     */
    private final String noIdPassMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0003");

    /**
     * Cookie認証失敗時のメッセージ.
     */
    private final String missCookieMsg = PersoniumCoreMessageUtils.getMessage("PS-AU-0005");

    /**
     * パスワード認証時に使用するAccountのUUID。パスワード認証後に最終ログイン時刻の更新に使用する.
     */
    private String accountId;

    /**
     * コンストラクタ.
     * @param cell Cell
     * @param cellRsCmp cellRsCmp
     */
    public AuthzEndPointResource(final Cell cell, final CellRsCmp cellRsCmp) {
        this.cell = cell;
        this.cellRsCmp = cellRsCmp;
    }

    /**
     * 認証のエンドポイント. <h2>トークンの発行しわけ</h2>
     * <ul>
     * <li>p_targetにURLが書いてあれば、そのCELLをTARGETのCELLとしてtransCellTokenを発行する。</li>
     * </ul>
     * @param authzHeader Authorization ヘッダ
     * @param pTarget クエリパラメタ
     * @param pOwner クエリパラメタ
     * @param assertion クエリパラメタ
     * @param clientId クエリパラメタ
     * @param responseType クエリパラメタ
     * @param redirectUri クエリパラメタ
     * @param host Hostヘッダ
     * @param pCookie p_cookie
     * @param cookieRefreshToken クッキー
     * @param keepLogin クエリパラメタ
     * @param state クエリパラメタ
     * @param isCancel Cancelフラグ
     * @param uriInfo コンテキスト
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
     * 認証のエンドポイント. <h2>トークンの発行しわけ</h2>
     * <ul>
     * <li>p_targetにURLが書いてあれば、そのCELLをTARGETのCELLとしてtransCellTokenを発行する。</li>
     * </ul>
     * @param authzHeader Authorization ヘッダ
     * @param host Hostヘッダ
     * @param pCookie p_cookie
     * @param cookieRefreshToken クッキー
     * @param formParams Body parameters
     * @param uriInfo コンテキスト
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
            // 末尾"/"の有無対応
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

        // 認可処理
        // clientIdで指定されたセルURLをスキーマに持つBoxが存在するかチェック
        //
        if (!checkAuthorization(normalizedClientId)) {
            log.debug(PersoniumCoreMessageUtils.getMessage("PS-ER-0003"));
            return this.returnErrorRedirect(cell.getUrl() + "__html/error", "PS-ER-0003");
        }

        // clientIdとredirectUriパラメタチェック
        try {
            this.checkImplicitParam(normalizedClientId, normalizedRedirectUri, uriInfo.getBaseUri());
        } catch (PersoniumCoreException e) {
            log.debug(e.getMessage());
            if ((username == null && password == null) //NOPMD -To maintain readability
                    && (assertion == null || "".equals(assertion))
                    && cookieRefreshToken == null) {
                // ユーザID・パスワード・assertion・cookieが未指定の場合、フォーム送信
                throw e;
            } else {
                return this.returnErrorRedirect(cell.getUrl() + "__html/error", e.getCode());
            }
        }

        if ("1".equals(isCancel)) {
            // redirect_uriへリダイレクト
            return this.returnErrorRedirect(redirectUri, OAuth2Helper.Error.UNAUTHORIZED_CLIENT,
                    PersoniumCoreMessageUtils.getMessage("PR401-AZ-0001"), state, "PR401-AZ-0001");
        }

        String schema = clientId;

        // response_Typeの値チェック
        if (responseType == null) {
            // redirect_uriへリダイレクト
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

    // TODO CodeFlow関連は仮実装
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
        // p_target がURLでない場合はヘッダInjectionの脆弱性を産んでしまう。(改行コードが入っているなど)
        try {
            this.checkPTarget(pTarget);
        } catch (PersoniumCoreAuthnException e) {
            return this.returnErrorRedirectCodeGrant(redirectUriStr, OAuth2Helper.Error.INVALID_REQUEST,
                    e.getMessage(), state, "code");
        }

        // パスワード認証・トランスセルトークン認証・cookie認証の切り分け
        if (username != null || password != null) {
            // TODO まだ未実装のためエラーを返す
            return this.returnErrorRedirectCodeGrant(redirectUriStr, OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE,
                    OAuth2Helper.Error.UNSUPPORTED_GRANT_TYPE, state, "PR400-AZ-0007");
         // TODO 必要？
//        } else if (cookieRefreshToken != null) {
//            // cookieの指定がある場合
//            // cookie認証の場合、keepLoginは常にtrueとして動作する
//            return handleCookieRefreshToken(redirectUriStr, clientId, host,
//                    cookieRefreshToken, OAuth2Helper.Key.TRUE_STR, state, pOwner);
        } else if (pCookie != null) {
            return handlePCookie(redirectUriStr, clientId, host,
                    pCookie, OAuth2Helper.Key.TRUE_STR, state, pOwner, uriInfo);
        } else {
            // TODO まだ未実装のためエラーを返す
//            // ユーザID・パスワード・assertion・cookieが未指定の場合、フォーム送信
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
        // クッキー認証の場合
        // クッキー内の値を復号化した値を取得
        CellLocalRefreshToken rToken;
        CellLocalAccessToken aToken;
        try {
            String decodedCookieValue = LocalToken.decode(pCookie,
                    UnitLocalUnitUserToken.getIvBytes(
                            AccessContext.getCookieCryptKey(uriInfo.getBaseUri())));
            int separatorIndex = decodedCookieValue.indexOf("\t");
            // クッキー内の情報から authorizationHeader相当のトークンを取得
            String authToken = decodedCookieValue.substring(separatorIndex + 1);

            AbstractOAuth2Token token = AbstractOAuth2Token.parse(authToken, cell.getUrl(), host);

            if (!(token instanceof IAccessToken)) {
                return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
            }

            // トークンの有効期限チェック
            if (token.isExpired()) {
                return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
            }

            long issuedAt = new Date().getTime();

            rToken = new CellLocalRefreshToken(issuedAt, token.getIssuer(), token.getSubject(), clientId);
            // 受け取ったTokenから AccessTokenを再生成
            List<Role> roleList = cell.getRoleListForAccount(token.getSubject());
            aToken = new CellLocalAccessToken(issuedAt, token.getIssuer(), token.getSubject(), roleList, clientId);
        } catch (TokenParseException e) {
            // パースに失敗したので
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        } catch (TokenDsigException e) {
            // 証明書検証に失敗したので
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        }
        // Cookieでの認証成功
        // 303でレスポンスし、Locationヘッダを返却
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toCodeString(),
                    rToken.toTokenString(), keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessageCodeGrant(clientId, redirectUriStr, missCookieMsg, state, null, pOwner);
        }
    }

    private Response returnSuccessRedirect(String redirectUriStr, String code,
            String refreshToken, String keepLogin, String state) throws MalformedURLException {
        // 302でレスポンスし、Locationヘッダを返却
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUriStr + getConnectionCode(redirectUriStr)
                + "code" + "=" + code
                + "&" + OAuth2Helper.Key.STATE + "=" + state);
        // レスポンスの返却

        // 認証を行うセルでのみ有効なcookieを返却する
        URL cellUrl = new URL(cell.getUrl());
        NewCookie cookies = null;
        Cookie cookie = new Cookie(OAuth2Helper.Key.SESSION_ID, refreshToken, cellUrl.getPath(), null);
        if (code != null) {
            // リフレッシュトークンの有効期限と同じSSLのみで使用出来るCookieを作成
            // 実行環境がhttpsの場合のみ、secureフラグを立てる
            if (OAuth2Helper.Key.TRUE_STR.equals(keepLogin)) {
                // Cookieの有効期限を24時間に設定
                cookies = new NewCookie(cookie, "", COOKIE_MAX_AGE, PersoniumUnitConfig.isHttps());
            } else {
                // Cookieの有効期限を設定しない
                cookies = new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps());
            }
        } else {
            cookies = new NewCookie(cookie, "", 0, PersoniumUnitConfig.isHttps());
        }
        return rb.entity("").cookie(cookies).build();
    }

    private Response returnErrorRedirectCodeGrant(String redirectUri, String error,
            String errorDesp, String state, String code) {
        // 303でレスポンスし、Locationヘッダを返却
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        // Locationヘッダに付加するフラグメント情報をURLエンコードする
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
            // エンコード種別は固定でutf-8にしているので、ここに来ることはありえない
            log.warn("Failed to URLencode, fragmentInfo of Location header.");
        }
        rb.header(HttpHeaders.LOCATION, sbuf.toString());
        // レスポンスの返却
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
                    // p_targetがURLでない場合はエラー
                    throw PersoniumCoreAuthnException.INVALID_TARGET;
                }
            } catch (MalformedURLException e) {
                // p_targetがURLでない場合はエラー
                throw PersoniumCoreAuthnException.INVALID_TARGET;
            }
        }
    }

    /**
     * ImplicitFlowパスワード認証フォーム.
     * @param clientId clientId
     * @param redirectUriStr redirectUriStr
     * @param message メッセージ表示領域に出力する文字列
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

            // 末尾"/"の有無対応
            if (!"".equals(clientId) && !clientId.endsWith("/")) {
                clientId = clientId + "/";
            }

            // タイトル
            paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
            // アプリセルのprofile.json
            paramsList.add(clientId + Box.DEFAULT_BOX_NAME + PROFILE_JSON_NAME);
            // データセルのprofile.json
            paramsList.add(cell.getUrl() + Box.DEFAULT_BOX_NAME + PROFILE_JSON_NAME);
            // タイトル
            paramsList.add(PersoniumCoreMessageUtils.getMessage("PS-AU-0001"));
            // 呼び出し先
            paramsList.add(cell.getUrl() + "__authz");
            // メッセージ表示領域
            paramsList.add(message);
            // hidden項目
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
     * ImplicitFlow時のパスワード認証処理.
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

        // ユーザIDとパスワードが一方でも未指定の場合、ログインエラーを返却する
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
        // 最終ログイン時刻を更新するために、UUIDをクラス変数にひかえておく
        accountId = (String) oew.getUuid();

        // ロックのチェック
        Boolean isLock = true;
        try {
            isLock = AuthResourceUtils.isLockedAccount(accountId);
            if (isLock) {
                // memcachedのロック時間を更新
                AuthResourceUtils.registAccountLock(accountId);
                String resCode = "PS-AU-0006";
                String accountLockMsg = PersoniumCoreMessageUtils.getMessage(resCode);
                log.info("MessageCode : " + resCode);
                log.info("responseMessage : " + accountLockMsg);
                ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
                return rb.entity(this.createForm(clientId, redirectUriStr, accountLockMsg, state,
                        OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
            }

            // ユーザIDとパスワードのチェック
            passCheck = cell.authenticateAccount(oew, password);
            if (!passCheck) {
                // memcachedにロックを作成
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
            // ユニット昇格権限設定のチェック
            if (!this.cellRsCmp.checkOwnerRepresentativeAccounts(username)) {
                return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, pOwner);
            }
            // セルのオーナーが未設定のセルに対しては昇格させない。
            if (cell.getOwner() == null) {
                return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, pOwner);
            }

            // uluut発行処理
            localToken = new UnitLocalUnitUserToken(
                    issuedAt, UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                    cell.getOwner(), host);

        }

        // リフレッシュトークンの生成
        CellLocalRefreshToken rToken = new CellLocalRefreshToken(issuedAt,
                CellLocalRefreshToken.REFRESH_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                cell.getUrl(), username, schema);
        // 303でレスポンスし、Locationヘッダを返却
        try {
            if (localToken != null) {
                // ULUUTの返却
                UnitLocalUnitUserToken aToken = (UnitLocalUnitUserToken) localToken;
                return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                        null, null, state);
            } else {
                // レスポンスをつくる。
                if (pTarget == null || "".equals(pTarget)) {
                    // セルローカルトークンの返却
                    AccountAccessToken aToken = new AccountAccessToken(issuedAt,
                            AccountAccessToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR, cell.getUrl(),
                            username, schema);
                    return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                            rToken.toTokenString(), keepLogin, state);
                } else {
                    // トランスセルトークンの返却
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
     * ImplicitFlow時のトランスセルトークン認証処理.
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

        // まずはパースする
        TransCellAccessToken tcToken = null;
        try {
            tcToken = TransCellAccessToken.parse(assertion);
        } catch (TokenParseException e) {
            // パース失敗時
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        } catch (TokenDsigException e) {
            // 署名検証でエラー
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        }

        // Tokenの検証
        // 1．有効期限チェック
        if (tcToken.isExpired()) {
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.ACCESS_DENIED,
                    OAuth2Helper.Error.ACCESS_DENIED, state, "PR401-AZ-0002");
        }

        // トークンのターゲットが自分でない場合はエラー応答
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

        // 認証は成功 -------------------------------

        long issuedAt = new Date().getTime();

        // CELLに依頼してTC発行元のロールから自分のところのロールを決定する。
        List<Role> rolesHere = cell.getRoleListHere(tcToken);

        String schemaVerified = schema;

        // 認証トークン発行処理
        // ターゲットは自由に決めてよい。
        IAccessToken aToken = null;
        if (pTarget == null || "".equals(pTarget)) {
            aToken = new CellLocalAccessToken(issuedAt, cell.getUrl(), tcToken.getSubject(), rolesHere, schemaVerified);
        } else {
            aToken = new TransCellAccessToken(UUID.randomUUID().toString(), issuedAt, cell.getUrl(),
                    tcToken.getSubject(), pTarget, rolesHere, schemaVerified);
        }
        // トランスセルトークンでの認証成功
        // 303でレスポンスし、Locationヘッダを返却
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                    null, keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessage(clientId, redirectUriStr, passFormMsg, state, pTarget, "");
        }
    }

    /**
     * ImplicitFlow時のcookie認証処理.
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

            // リフレッシュトークンの有効期限チェック
            if (token.isRefreshExpired()) {
                return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
            }

            long issuedAt = new Date().getTime();

            if (Key.TRUE_STR.equals(pOwner)) {
                // 自分セルリフレッシュの場合のみ昇格できる。
                if (token.getClass() != CellLocalRefreshToken.class) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
                // ユニット昇格権限設定のチェック
                if (!this.cellRsCmp.checkOwnerRepresentativeAccounts(token.getSubject())) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
                // セルのオーナーが未設定のセルに対しては昇格させない。
                if (cell.getOwner() == null) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }

                // uluut発行処理
                UnitLocalUnitUserToken uluut = new UnitLocalUnitUserToken(
                        issuedAt, UnitLocalUnitUserToken.ACCESS_TOKEN_EXPIRES_HOUR * MILLISECS_IN_AN_HOUR,
                        cell.getOwner(), host);
                // Cookieでの認証成功
                // 303でレスポンスし、Locationヘッダを返却
                try {
                    return returnSuccessRedirect(redirectUriStr, uluut.toTokenString(), uluut.expiresIn(),
                            null, keepLogin, state);
                } catch (MalformedURLException e) {
                    return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
                }
            } else {
                // 受け取ったRefresh Tokenから AccessTokenとRefreshTokenを再生成
                rToken = (IRefreshToken) token;
                rToken = rToken.refreshRefreshToken(issuedAt);

                if (rToken instanceof CellLocalRefreshToken) {
                    String subject = rToken.getSubject();
                    List<Role> roleList = cell.getRoleListForAccount(subject);
                    aToken = rToken.refreshAccessToken(issuedAt, pTarget, cell.getUrl(), roleList);
                } else {
                    // CELLに依頼してトークン発行元のロールから自分のところのロールを決定する。
                    List<Role> rolesHere = cell.getRoleListHere((IExtRoleContainingToken) rToken);
                    aToken = rToken.refreshAccessToken(issuedAt, pTarget, cell.getUrl(), rolesHere);
                }
            }

        } catch (TokenParseException e) {
            // パースに失敗したので
            PersoniumCoreLog.Auth.TOKEN_PARSE_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        } catch (TokenDsigException e) {
            // 証明書検証に失敗したので
            PersoniumCoreLog.Auth.TOKEN_DISG_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        } catch (TokenRootCrtException e) {
            // ルートCA証明書の設定エラー
            PersoniumCoreLog.Auth.ROOT_CA_CRT_SETTING_ERROR.params(e.getMessage()).writeLog();
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        }
        // Cookieでの認証成功
        // 303でレスポンスし、Locationヘッダを返却
        try {
            return returnSuccessRedirect(redirectUriStr, aToken.toTokenString(), aToken.expiresIn(),
                    rToken.toTokenString(), keepLogin, state);
        } catch (MalformedURLException e) {
            return returnErrorMessage(clientId, redirectUriStr, missCookieMsg, state, pTarget, pOwner);
        }
    }

    /**
     * ImplicitFlowによる認証処理ハンドリング.
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

        // p_target がURLでない場合はヘッダInjectionの脆弱性を産んでしまう。(改行コードが入っているなど)
        try {
            this.checkPTarget(pTarget);
        } catch (PersoniumCoreAuthnException e) {
            return this.returnErrorRedirect(redirectUriStr, OAuth2Helper.Error.INVALID_REQUEST,
                    e.getMessage(), state, "code");
        }
        // TODO ボックスの存在チェック⇒ある場合：トークン返却、ない場合：ボックス作成（権限チェック⇒Boxインポート実行）
        // ただし、Boxインポートが実装されるまではエラーを返す

        // パスワード認証・トランスセルトークン認証・cookie認証の切り分け
        if (username != null || password != null) {
            // ユーザID・パスワードのどちらかに設定がある場合
            Response response = this.handleImplicitFlowPassWord(pTarget, redirectUriStr, clientId,
                    username, password, keepLogin, state, pOwner, host);

            if (PersoniumUnitConfig.getAccountLastAuthenticatedEnable()
                    && isSuccessAuthorization(response)) {
                // Accountのスキーマ情報を取得する
                PersoniumODataProducer producer = ModelFactory.ODataCtl.cellCtl(cell);
                EdmEntitySet esetAccount = producer.getMetadata().getEdmEntitySet(Account.EDM_TYPE_NAME);
                OEntityKey originalKey = OEntityKey.parse("('" + username + "')");
                // 最終ログイン時刻の変更をProducerに依頼(このメソッド内でロックを取得・解放)
                producer.updateLastAuthenticated(esetAccount, originalKey, accountId);
            }
            return response;
        } else if (assertion != null && !"".equals(assertion)) {
            // assertionの指定がある場合
            return this.handleImplicitFlowTcToken(redirectUriStr, clientId, assertion, pTarget, keepLogin, schema,
                    state);
        } else if (cookieRefreshToken != null) {
            // cookieの指定がある場合
            // cookie認証の場合、keepLoginは常にtrueとして動作する
            return this.handleImplicitFlowcookie(redirectUriStr, clientId, host,
                    cookieRefreshToken, pTarget, OAuth2Helper.Key.TRUE_STR, state, pOwner);
        } else {
            // ユーザID・パスワード・assertion・cookieが未指定の場合、フォーム送信
            ResponseBuilder rb = Response.ok().type("text/html; charset=UTF-8");
            return rb.entity(this.createForm(clientId, redirectUriStr, passFormMsg, state,
                    OAuth2Helper.ResponseType.TOKEN, pTarget, pOwner)).build();
        }
    }

    /**
     * ImplicitFlowでのパスワード認証が成功したかどうかを判定する.
     * @param response 認証レスポンス
     * @return true: 認証成功 false:認証失敗
     */
    protected boolean isSuccessAuthorization(Response response) {
        // レスポンスコードが303以外の場合は、画面遷移しないエラーとみなす
        if (Status.SEE_OTHER.getStatusCode() != response.getStatus()) {
            return false;
        }

        // Locationヘッダに指定されたURLのフラグメントにエラー情報があるかをチェックする
        String locationStr = (String) response.getMetadata().getFirst(HttpHeaders.LOCATION);
        try {
            URI uri = new URI(locationStr);
            String fragment = uri.getFragment();
            // フラグメントがない場合はAPIのI/Fエラーとみなす
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
     * ImplicitFlowでの認証後、Redirectを実行する.
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
        // 303でレスポンスし、Locationヘッダを返却
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUriStr + "#"
                + OAuth2Helper.Key.ACCESS_TOKEN + "=" + localTokenStr + "&"
                + OAuth2Helper.Key.TOKEN_TYPE + "="
                + OAuth2Helper.Scheme.BEARER
                + "&" + OAuth2Helper.Key.EXPIRES_IN + "=" + localTokenExpiresIn
                + "&" + OAuth2Helper.Key.STATE + "=" + state);
        // レスポンスの返却

        // 認証を行うセルでのみ有効なcookieを返却する
        URL cellUrl = new URL(cell.getUrl());
        NewCookie cookies = null;
        Cookie cookie = new Cookie(OAuth2Helper.Key.SESSION_ID, refreshTokenStr, cellUrl.getPath(), null);
        if (refreshTokenStr != null) {
            // リフレッシュトークンの有効期限と同じSSLのみで使用出来るCookieを作成
            // 実行環境がhttpsの場合のみ、secureフラグを立てる
            if (OAuth2Helper.Key.TRUE_STR.equals(keepLogin)) {
                // Cookieの有効期限を24時間に設定
                cookies = new NewCookie(cookie, "", COOKIE_MAX_AGE, PersoniumUnitConfig.isHttps());
            } else {
                // Cookieの有効期限を設定しない
                cookies = new NewCookie(cookie, "", -1, PersoniumUnitConfig.isHttps());
            }
        } else {
            cookies = new NewCookie(cookie, "", 0, PersoniumUnitConfig.isHttps());
        }
        return rb.entity("").cookie(cookies).build();
    }

    /**
     * ImplicitFlowでの認証時のエラーのうち以下の状況で、ユーザが設定したredirect_uriへのRedirectを実行する. １．response_typeが不正・未指定 ２
     * @param state
     * @return
     * @throws MalformedURLException
     */
    private Response returnErrorRedirect(String redirectUri, String code) {
        // 303でレスポンスし、Locationヘッダを返却
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, redirectUri + getConnectionCode(redirectUri)
                + OAuth2Helper.Key.CODE + "=" + code);
        // レスポンスの返却
        return rb.entity("").build();
    }

    /**
     * ImplicitFlowでの認証時のエラーのうち以下の状況で、ユーザが設定したredirect_uriへのRedirectを実行する. １．response_typeが不正・未指定 ２
     * @param state
     * @return
     * @throws MalformedURLException
     */
    private Response returnErrorRedirect(String redirectUri, String error,
            String errorDesp, String state, String code) {
        // 302でレスポンスし、Locationヘッダを返却
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        // Locationヘッダに付加するフラグメント情報をURLエンコードする
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
            // エンコード種別は固定でutf-8にしているので、ここに来ることはありえない
            log.warn("Failed to URLencode, fragmentInfo of Location header.");
        }
        rb.header(HttpHeaders.LOCATION, sbuf.toString());
        // レスポンスの返却
        return rb.entity("").build();
    }

    /**
     * ImplicitFlow_cookieでの認証時のエラー処理.
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
     * 認可処理 clientIdで指定されたセルURLをスキーマに持つBoxが存在するかチェック.
     * @param clientId アプリセルURL
     * @return true：認可成功 false:認可失敗
     */
    private boolean checkAuthorization(final String clientId) {
        EntitySetAccessor boxAcceccor = EsModel.box(this.cell);

        // {filter={and={filters=[{term={c=【CellID】}}, {term={s.Schema.untouched=【clientID】}}]}}}
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

        // TODO Issue-223 一時対処
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
     * ImplicitFlow認証時のパラメータチェック.
     * @param clientId
     * @param redirectUri
     * @param baseUri
     */
    private void checkImplicitParam(String clientId, String redirectUri, URI baseUri) {
        if (redirectUri == null || clientId == null) {
            // TODO 一方がnullの場合はエラー。メッセージ変更の必要あり
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

        // baseurlのパスを取得する
        String bPath = baseUri.getPath();

        // client_idとredirect_uriのパスからbaseUriのパスを削除する
        String cPath = objClientId.getPath().substring(bPath.length());
        String rPath = objRedirectUri.getPath().substring(bPath.length());

        // client_idとredirect_uriのパスを/で分割
        String[] cPaths = StringUtils.split(cPath, "/");
        String[] rPaths = StringUtils.split(rPath, "/");

        // client_idとredirect_uriを比較し、セルが異なる場合は認証エラー
        // セルのURLまでの比較
        if (!objClientId.getAuthority().equals(objRedirectUri.getAuthority())
                || !cPaths[0].equals(rPaths[0])) {
            throw PersoniumCoreException.Auth.REQUEST_PARAM_REDIRECT_INVALID;
        }

        // client_idとリクエストされたCellの名前を比較し、セルが同じ場合はエラー
        if (cPaths[0].equals(this.cell.getName())) {
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
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        return ResourceUtils.responseBuilderForOptions(HttpMethod.POST, HttpMethod.GET).build();
    }
}

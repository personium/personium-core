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
package io.personium.core;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import io.personium.core.DcCoreMessageUtils.Severity;
import io.personium.core.auth.OAuth2Helper.Error;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.auth.OAuth2Helper.Scheme;

/**
 * ログメッセージ作成クラス.
 */
/**
 * @author naoki
 */
@SuppressWarnings("serial")
public final class DcCoreAuthnException extends DcCoreException {

    /**
     * Grant-Typeの値が異常.
     */
    public static final DcCoreAuthnException UNSUPPORTED_GRANT_TYPE =
            create("PR400-AN-0001", Error.UNSUPPORTED_GRANT_TYPE);
    /**
     * p_targetの値異常.
     */
    public static final DcCoreAuthnException INVALID_TARGET = create("PR400-AN-0002", Error.INVALID_REQUEST);
    /**
     * Client Sercret パースエラー.
     */
    public static final DcCoreAuthnException CLIENT_SERCRET_PARSE_ERROR = create("PR400-AN-0003", Error.INVALID_CLIENT);
    /**
     * Client Sercret 有効期限チェック.
     */
    public static final DcCoreAuthnException CLIENT_SERCRET_EXPIRED = create("PR400-AN-0004", Error.INVALID_CLIENT);
    /**
     * Client Sercret 署名検証をエラー.
     */
    public static final DcCoreAuthnException CLIENT_SERCRET_DSIG_INVALID =
            create("PR400-AN-0005", Error.INVALID_CLIENT);
    /**
     * Client Sercret のIssuerがIDと等しくない.
     */
    public static final DcCoreAuthnException CLIENT_SERCRET_ISSUER_MISMATCH =
            create("PR400-AN-0006", Error.INVALID_CLIENT);
    /**
     * Client Sercret のターゲットが自分でない.
     */
    public static final DcCoreAuthnException CLIENT_SERCRET_TARGET_WRONG =
            create("PR400-AN-0007", Error.INVALID_CLIENT);

    /**
     * トランスセルトークン認証ではユニットユーザ昇格はできない.
     */
    public static final DcCoreAuthnException TC_ACCESS_REPRESENTING_OWNER =
            create("PR400-AN-0008", Error.INVALID_GRANT);
    /**
     * トークンパースエラー.
     */
    public static final DcCoreAuthnException TOKEN_PARSE_ERROR = create("PR400-AN-0009", Error.INVALID_GRANT);
    /**
     * 有効期限切れ.
     */
    public static final DcCoreAuthnException TOKEN_EXPIRED = create("PR400-AN-0010", Error.INVALID_GRANT);
    /**
     * 署名検証をエラー.
     */
    public static final DcCoreAuthnException TOKEN_DSIG_INVALID = create("PR400-AN-0011", Error.INVALID_GRANT);
    /**
     * トークン のターゲットが自分でない.
     * {0}:トークンのターゲットURL
     */
    public static final DcCoreAuthnException TOKEN_TARGET_WRONG = create("PR400-AN-0012", Error.INVALID_GRANT);
    /**
     * リフレッシュトークンでない.
     */
    public static final DcCoreAuthnException NOT_REFRESH_TOKEN = create("PR400-AN-0013", Error.INVALID_GRANT);
    /**
     * 権限がないから昇格できない.
     */
    public static final DcCoreAuthnException NOT_ALLOWED_REPRESENT_OWNER = create("PR400-AN-0014", Error.INVALID_GRANT);
    /**
     * オーナーがいないセルは昇格できない.
     */
    public static final DcCoreAuthnException NO_CELL_OWNER = create("PR400-AN-0015", Error.INVALID_GRANT);
    /**
     * 必須パラメータが無い.
     * {0}:パラメータキー名
     */
    public static final DcCoreAuthnException REQUIRED_PARAM_MISSING = create("PR400-AN-0016", Error.INVALID_REQUEST);
    /**
     * 認証エラー.
     */
    public static final DcCoreAuthnException AUTHN_FAILED = create("PR400-AN-0017", Error.INVALID_GRANT);
    /**
     * 認証ヘッダの指定誤り.
     */
    public static final DcCoreAuthnException AUTH_HEADER_IS_INVALID = create("PR400-AN-0018", Error.INVALID_CLIENT);
    /**
     * Accountロック中.
     */
    public static final DcCoreAuthnException ACCOUNT_LOCK_ERROR = create("PR400-AN-0019", Error.INVALID_GRANT);
    /**
     * IDTokenの検証の中で、受け取ったIdTokenのAudienceが信頼するClientIDのリストに無かった.
     */
    public static final DcCoreAuthnException OIDC_WRONG_AUDIENCE = create("PR400-AN-0030", Error.INVALID_GRANT);
    /**
     * OIDCの認証エラー.
     */
    public static final DcCoreAuthnException OIDC_AUTHN_FAILED = create("PR400-AN-0031", Error.INVALID_GRANT);
    /**
     * 無効なIDToken.
     */
    public static final DcCoreAuthnException OIDC_INVALID_ID_TOKEN = create("PR400-AN-0032", Error.INVALID_GRANT);
    /**
     * IDTokenの有効期限切れ.
     */
    public static final DcCoreAuthnException OIDC_EXPIRED_ID_TOKEN = create("PR400-AN-0033", Error.INVALID_GRANT);

    /**
     * インナークラスを強制的にロードする.
     */
    public static void loadConfig() {
    }

    String error;
    String realm;

    /**
     * コンストラクタ.
     * @param status HTTPレスポンスステータス
     * @param severityエラーレベル
     * @param code エラーコード
     * @param message エラーメッセージ
     * @param error OAuth認証エラーのエラーコード
     * @param realm WWWW-Authenticateヘッダを返す場合はここにrealm値を設定する
     */
    DcCoreAuthnException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final String error,
            final String realm) {
        super(code, severity, message, status);
        this.error = error;
        this.realm = realm;
    }

    /**
     * realmを設定してオブジェクト生成.
     * @param realm2set realm
     * @return CoreAuthnException
     */
    public DcCoreAuthnException realm(String realm2set) {
        // クローンを作成
        return new DcCoreAuthnException(this.code, this.severity, this.message, this.status, this.error, realm2set);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Response createResponse() {
        JSONObject errorJson = new JSONObject();

        errorJson.put(Key.ERROR, this.error);

        String errDesc = String.format("[%s] - %s", this.code, this.message);
        errorJson.put(Key.ERROR_DESCRIPTION, errDesc);

        int statusCode = parseCode(this.code);
        ResponseBuilder rb = Response.status(statusCode)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(errorJson.toJSONString());

        // レルム値が設定されていれば、WWW-Authenticateヘッダーを返却する。
        // __authエンドポイントでは、認証失敗時(401返却時)には、同ヘッダーに Auth SchemeがBasicの値を返却するため、ここでは固定値とする。
        if (this.realm != null && statusCode == HttpStatus.SC_UNAUTHORIZED) {
            rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
        }
        return rb.build();
    }

    /**
     * 原因例外を追加したものを作成して返します.
     * @param t 原因例外
     * @return DcCoreException
     */
    public DcCoreException reason(final Throwable t) {
        // クローンを作成して
        DcCoreException ret = new DcCoreAuthnException(
                this.code, this.severity, this.message, this.status, this.error, this.realm);
        // スタックトレースをセット
        ret.setStackTrace(t.getStackTrace());
        return ret;
    }

    /**
     * ファクトリーメソッド.
     * @param code DCメッセージコード
     * @param error OAuth2エラーコード
     * @return DcCoreException
     */
    public static DcCoreAuthnException create(String code, String error) {
        int statusCode = DcCoreException.parseCode(code);

        // ログレベルの取得
        Severity severity = DcCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            // ログレベルが設定されていなかったらレスポンスコードから自動的に判定する。
            severity = decideSeverity(statusCode);
        }

        // ログメッセージの取得
        String message = DcCoreMessageUtils.getMessage(code);

        return new DcCoreAuthnException(code, severity, message, statusCode, error, null);
    }
}

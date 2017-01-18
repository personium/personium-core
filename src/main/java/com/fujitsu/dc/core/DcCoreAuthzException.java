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
package com.fujitsu.dc.core;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;

import com.fujitsu.dc.core.DcCoreMessageUtils.Severity;
import com.fujitsu.dc.core.auth.OAuth2Helper.AcceptableAuthScheme;
import com.fujitsu.dc.core.auth.OAuth2Helper.Scheme;
import com.fujitsu.dc.core.exceptions.ODataErrorMessage;

/**
 * 認証エラー(PR401-AU-xxxx)が発生した場合のログ出力クラス.
 */
@SuppressWarnings("serial")
public final class DcCoreAuthzException extends DcCoreException {

    /**
     * 認証ヘッダが必要.
     */
    public static final DcCoreAuthzException AUTHORIZATION_REQUIRED = create("PR401-AU-0001");
    /**
     * トークン有効期限切れ.
     */
    public static final DcCoreAuthzException EXPIRED_ACCESS_TOKEN = create("PR401-AU-0002");
    /**
     * AuthenticationSchemeが無効.
     */
    public static final DcCoreAuthzException INVALID_AUTHN_SCHEME = create("PR401-AU-0003");
    /**
     * ベーシック認証ヘッダのフォーマットが無効.
     */
    public static final DcCoreAuthzException BASIC_AUTH_FORMAT_ERROR = create("PR401-AU-0004");

    /**
     * トークンパースエラー.
     */
    public static final DcCoreAuthzException TOKEN_PARSE_ERROR = create("PR401-AU-0006");
    /**
     * リフレッシュトークンでのアクセス.
     */
    public static final DcCoreAuthzException ACCESS_WITH_REFRESH_TOKEN = create("PR401-AU-0007");
    /**
     * トークン署名検証エラー.
     */
    public static final DcCoreAuthzException TOKEN_DISG_ERROR = create("PR401-AU-0008");
    /**
     * クッキー認証エラー.
     */
    public static final DcCoreAuthzException COOKIE_AUTHENTICATION_FAILED = create("PR401-AU-0009");

    /**
     * ベーシック認証エラー(Accountロック中).
     */
    public static final DcCoreAuthzException BASIC_AUTHENTICATION_FAILED_IN_ACCOUNT_LOCK =
            create("PR401-AU-0010");

    /**
     * ベーシック認証エラー.
     */
    public static final DcCoreAuthzException BASIC_AUTHENTICATION_FAILED = create("PR401-AU-0011");

    /**
     * インナークラスを強制的にロードする.
     */
    public static void loadConfig() {
    }

    String realm;
    AcceptableAuthScheme authScheme = AcceptableAuthScheme.ALL; // デフォルトとしてBasic/Bearerを許可する設定にしておく

    /**
     * コンストラクタ.
     * @param status HTTPレスポンスステータス
     * @param severityエラーレベル
     * @param code エラーコード
     * @param message エラーメッセージ
     * @param error OAuth認証エラーのエラーコード
     * @param realm WWWW-Authenticateヘッダを返す場合はここにrealm値を設定する
     * @param authScheme 認証を許可するAuthSchemeの種別
     */
    DcCoreAuthzException(final String code,
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
     * realmを設定してオブジェクト生成.
     * @param realm2set realm
     * @return CoreAuthnException
     */
    public DcCoreAuthzException realm(String realm2set) {
        // クローンを作成
        return new DcCoreAuthzException(this.code, this.severity, this.message, this.status, realm2set,
                AcceptableAuthScheme.ALL);
    }

    /**
     * realmを設定してオブジェクト生成.
     * @param realm2set realm
     * @param acceptableAuthScheme 認証を許可するAuthSchemeの種別
     * @return CoreAuthnException
     */
    public DcCoreAuthzException realm(String realm2set, AcceptableAuthScheme acceptableAuthScheme) {
        // クローンを作成
        return new DcCoreAuthzException(this.code, this.severity,
                this.message, this.status, realm2set, acceptableAuthScheme);
    }

    @Override
    public Response createResponse() {
        ResponseBuilder rb = Response.status(HttpStatus.SC_UNAUTHORIZED)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(new ODataErrorMessage(code, message));

        // レルム値が設定されていれば、WWW-Authenticateヘッダーを返却する。
        if (null != this.realm) {
            switch (this.authScheme) {
            case BEARER:
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BEARER + " realm=\"" + this.realm + "\"");
                break;
            case BASIC:
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
                break;
            default: // デフォルトとして、Bearer/Basicの両方を返却する。
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BEARER + " realm=\"" + this.realm + "\"");
                rb = rb.header(HttpHeaders.WWW_AUTHENTICATE, Scheme.BASIC + " realm=\"" + this.realm + "\"");
                break;
            }
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
        DcCoreException ret = new DcCoreAuthzException(
                this.code, this.severity, this.message, this.status, this.realm, this.authScheme);
        // スタックトレースをセット
        ret.setStackTrace(t.getStackTrace());
        return ret;
    }

    /**
     * ファクトリーメソッド.
     * @param code DCメッセージコード
     * @return DcCoreException
     */
    public static DcCoreAuthzException create(String code) {
        int statusCode = DcCoreException.parseCode(code);

        // ログレベルの取得
        Severity severity = DcCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            // ログレベルが設定されていなかったらレスポンスコードから自動的に判定する。
            severity = decideSeverity(statusCode);
        }

        // ログメッセージの取得
        String message = DcCoreMessageUtils.getMessage(code);

        return new DcCoreAuthzException(code, severity, message, statusCode, null, AcceptableAuthScheme.ALL);
    }
}

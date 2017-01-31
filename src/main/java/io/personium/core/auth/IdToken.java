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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.DcCoreAuthnException;
import io.personium.core.DcCoreException;

/**
 * IdToken.
 */
public class IdToken {

    static Logger log = LoggerFactory.getLogger(IdToken.class);

    private String header;
    private String payload;
    private String signature;

    /* header */
    private String kid;

    /* payload */
    private String email;
    private String issuer;
    private String audience;
    private Long exp;

    private static final String GOOGLE_DISCOV_DOC_URL = "https://accounts.google.com/.well-known/openid-configuration";
    private static final String ALG = "SHA256withRSA";

    private static final String KID = "kid";
    private static final String KTY = "kty";

    private static final String ISS = "iss";
    private static final String EML = "email";
    private static final String AUD = "aud";
    private static final String EXP = "exp";

    private static final String N = "n";
    private static final String E = "e";

    private static final int SPLIT_TOKEN_NUM = 3;

    private static final int VERIFY_WAIT = 60;
    private static final int VERIFY_SECOND = 1000;

    /**
     * IdToken.
     */
    public IdToken() {
    }

    /**
     * IdToken.
     * @param json JSON
     */
    public IdToken(JSONObject json) {
        this.setEmail((String) json.get("email"));
        this.setIssuer((String) json.get("issuer"));
        this.setAudience((String) json.get("audience"));
        this.setExp((Long) json.get("exp"));
    }

    /**
     * 最終検証結果を返す.
     * @param null
     * @retrun boolean
     * @throws DcCoreAuthnException dcae
     */
    public void verify() throws DcCoreAuthnException {
        // expireしていないかチェック(60秒くらいは過ぎても良い)
        boolean expired = (exp + VERIFY_WAIT) * VERIFY_SECOND < System.currentTimeMillis();
        if (expired) {
            throw DcCoreAuthnException.OIDC_EXPIRED_ID_TOKEN.params(exp);
        }
        // 署名検証
        verifySignature();
    }

    /**
     * 署名検証.
     *
     * @param null
     * @return boolean
     */
    private void verifySignature() {
        RSAPublicKey rsaPubKey = this.getKey();
        try {
            Signature sig = Signature.getInstance(ALG);
            sig.initVerify(rsaPubKey);
            sig.update((this.getHeader() + "." + this.getPayload()).getBytes());
            boolean verified = sig.verify(PersoniumCoreUtils.decodeBase64Url(this.getSignature()));
            if (!verified) {
                // 署名検証結果、署名が不正であると認定
                throw DcCoreAuthnException.OIDC_AUTHN_FAILED;
            }
        } catch (NoSuchAlgorithmException e) {
            // 環境がおかしい以外でここには来ない
            throw new RuntimeException(ALG + " not supported.", e);
        } catch (InvalidKeyException e) {
            // バグ以外でここには来ない
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            // IdTokenのSignatureがおかしい
            // the passed-in signature is improperly encoded or of the wrong
            // type,
            // if this signature algorithm is unable to process the input data
            // provided, etc.
            throw DcCoreAuthnException.OIDC_INVALID_ID_TOKEN.params("ID Token sig value is invalid.");
        }
    }

    /**
     * 公開鍵情報から、IDTokenのkidにマッチする方で公開鍵を生成.
     *
     * @return RSAPublicKey 公開鍵
     */
    private RSAPublicKey getKey() {
        JSONArray jsonAry = getKeys();
        for (int i = 0; i < jsonAry.size(); i++) {
            JSONObject k = (JSONObject) jsonAry.get(i);
            String compKid = (String) k.get(KID);
            if (compKid.equals(this.getKid())) {
                BigInteger n = new BigInteger(1, PersoniumCoreUtils.decodeBase64Url((String) k.get(N)));
                BigInteger e = new BigInteger(1, PersoniumCoreUtils.decodeBase64Url((String) k.get(E)));
                RSAPublicKeySpec rsaPubKey = new RSAPublicKeySpec(n, e);
                try {
                    KeyFactory kf = KeyFactory.getInstance((String) k.get(KTY));
                    return (RSAPublicKey) kf.generatePublic(rsaPubKey);
                } catch (NoSuchAlgorithmException e1) {
                    // ktyの値がRSA以外はサポートしない
                    throw DcCoreException.NetWork.UNEXPECTED_VALUE.params(KTY, "RSA").reason(e1);
                } catch (InvalidKeySpecException e1) {
                    // バグ以外でここには来ない
                    throw new RuntimeException(e1);
                }
            }
        }
        // 該当するkidを持つ鍵情報が取れなかった場合
        throw DcCoreAuthnException.OIDC_INVALID_ID_TOKEN.params("ID Token header value is invalid.");
    }

    /**
     * IdToken の検証のためのパース処理.
     *
     * @param idTokenStr IDトークン
     *
     * @return IdToken idToken
     */
    public static IdToken parse(String idTokenStr) {
        IdToken ret = new IdToken();
        String[] splitIdToken = idTokenStr.split("\\.");
        if (splitIdToken.length != SPLIT_TOKEN_NUM) {
            throw DcCoreAuthnException.OIDC_INVALID_ID_TOKEN.params("2 periods required.");
        }
        ret.header = splitIdToken[0];
        ret.payload = splitIdToken[1];
        ret.signature = splitIdToken[2];

        try {
            String headerDecoded = new String(PersoniumCoreUtils.decodeBase64Url(ret.header), StandardCharsets.UTF_8);
            String payloadDecoded = new String(PersoniumCoreUtils.decodeBase64Url(ret.payload), StandardCharsets.UTF_8);

            JSONObject header = (JSONObject) new JSONParser().parse(headerDecoded);
            JSONObject payload = (JSONObject) new JSONParser().parse(payloadDecoded);
            ret.kid = (String) header.get(KID);
            ret.issuer = (String) payload.get(ISS);
            ret.email = (String) payload.get(EML);
            ret.audience = (String) payload.get(AUD);
            ret.exp = (Long) payload.get(EXP);
        } catch (ParseException e) {
            // BASE64はOk.JSONのパースに失敗.
            throw DcCoreAuthnException.OIDC_INVALID_ID_TOKEN
                .params("Header and payload should be Base64 encoded JSON.");
    } catch (Exception e) {
            // BASE64が失敗.
            throw DcCoreAuthnException.OIDC_INVALID_ID_TOKEN.params("Header and payload should be Base64 encoded.");
        }
        return ret;
    }

    private static String getJwksUri(String endpoint) {
        return (String) getHttpJSON(endpoint).get("jwks_uri");
    }

    private static JSONArray getKeys() {
            return (JSONArray) getHttpJSON(getJwksUri(GOOGLE_DISCOV_DOC_URL)).get("keys");
    }

    /**
     * Cacheを聞かせるため、ClientをStaticとする. たかだか限定されたURLのbodyを保存するのみであり、
     * 最大キャッシュサイズはCacheConfigクラスで定義された16kbyte程度である. そのため、Staticで持つこととした.
     */
    private static HttpClient httpClient = new CachingHttpClient();

    /**
     * HTTPでJSONオブジェクトを取得する処理. Cacheが利用可能であればその値を用いる.
     *
     * @param url URL
     * @return JSONObject
     */
    public static JSONObject getHttpJSON(String url) {
        HttpGet get = new HttpGet(url);
        int status = 0;
        try {
            HttpResponse res = httpClient.execute(get);
            InputStream is = res.getEntity().getContent();
            status = res.getStatusLine().getStatusCode();
            String body = PersoniumCoreUtils.readInputStreamAsString(is);
            JSONObject jsonObj = (JSONObject) new JSONParser().parse(body);
            return jsonObj;
        } catch (ClientProtocolException e) {
            // HTTPのプロトコル違反
            throw DcCoreException.NetWork.UNEXPECTED_RESPONSE.params(url, "proper HTTP response", status).reason(e);
        } catch (IOException e) {
            // サーバーに接続できない場合に発生
            throw DcCoreException.NetWork.HTTP_REQUEST_FAILED.params(HttpGet.METHOD_NAME, url).reason(e);
        } catch (ParseException e) {
            // JSONでないものを返してきた
            throw DcCoreException.NetWork.UNEXPECTED_RESPONSE.params(url, "JSON", status).reason(e);
        }
    }

    /**
     * getHeader.
     * @return header
     */
    public String getHeader() {
        return header;
    }

    /**
     * setHeader.
     * @param header HEADER
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * getPayload.
     * @return payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * setPayload.
     * @param payload PAYLOAD
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

    /**
     * getSignature.
     * @return signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * setSignature.
     * @param signature SIGATURE
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * getKid.
     * @return kid
     */
    public String getKid() {
        return kid;
    }

    /**
     * setKid.
     * @param kid KID
     */
    public void setKid(String kid) {
        this.kid = kid;
    }

    /**
     * getEmail.
     * @return email
     */
    public String getEmail() {
        return email;
    }

    /**
     * setEmail.
     * @param email E-MAIL
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * getIssuer.
     * @return issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * setIssuer.
     * @param issuer ISSUER
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * getAudience.
     * @return audience
     */
    public String getAudience() {
        return audience;
    }

    /**
     * setAudience.
     * @param audience audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    /**
     * getExp.
     * @return exp
     */
    public Long getExp() {
        return exp;
    }

    /**
     * setExp.
     * @param exp exp
     */
    public void setExp(Long exp) {
        this.exp = exp;
    }
}

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
package io.personium.test.jersey;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import io.personium.test.DcCoreTestConfig;

/**
 * RESTアクセスのためのクラス.
 */
public class PersoniumRestAdapter {
    /** ログオブジェクト. */
    private Log log;

    /** Content-Type に指定する文字列定義. */
    public static final String CONTENT_TYPE_JSON = "application/json";
    /** Content-Type に指定する文字列定義. */
    public static final String CONTENT_TYPE_XML = "application/xml";
    /** Content-Type に指定する文字列定義. */
    public static final String CONTENT_FORMURLENCODE = "application/x-www-form-urlencoded";
    /** ポストデータのエンコード種別. */
    private static final String ENCODE = "UTF-8";
    /** デフォルトタイムアウト値. */
    private static final int TIMEOUT = 85000;
    /** HTTPClient. */
    private HttpClient httpClient;

    /**
     * コンストラクタ.
     */
    public PersoniumRestAdapter() {
        httpClient = HttpClientFactory.create("insecure", TIMEOUT);
        HttpClientParams.setRedirecting(httpClient.getParams(), false);
        log = LogFactory.getLog(PersoniumRestAdapter.class);
    }

    /**
     * HttpClientを置き換える(ユニットテスト用).
     * @param value HttpClientオブジェクト
     */
    public final void setHttpClient(final HttpClient value) {
        this.httpClient = value;
    }

    /**
     * レスポンスボディを受け取るGETメソッド.
     * @param url リクエスト対象URL
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse get(final String url, final HashMap<String, String> headers)
            throws DcException {
        HttpUriRequest req = new HttpGet(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, "");
        DcResponse res = this.request(req);
        return res;
    }

    /**
     * レスポンスボディを受け取るGETメソッド.
     * @param url リクエスト対象URL
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse getAcceptEncodingGzip(final String url, final HashMap<String, String> headers)
            throws DcException {
        HttpUriRequest req = new HttpGet(url);
        req.addHeader("Accept-Encoding", "gzip");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, "");
        DcResponse res = this.request(req);
        return res;
    }

    /**
     * レスポンスボディを受ける PUTメソッド.
     * @param url リクエスト対象URL
     * @param data 書き込むデータ
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse put(final String url,
            final String data,
            final HashMap<String, String> headers) throws DcException {
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        HttpUriRequest req = makePutRequest(url, data, contentType);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, data);
        DcResponse res = request(req);
        return res;
    }

    /**
     * Stream登録を行うPUTメソッド.
     * @param url PUT対象のURL
     * @param headers リクエストヘッダのハッシュマップ
     * @param is PUTするデータストリーム
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse put(final String url, final HashMap<String, String> headers, final InputStream is)
            throws DcException {
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        HttpUriRequest req = makePutRequestByStream(url, contentType, is);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, "body is InputStream...");
        DcResponse res = request(req);
        return res;
    }

    /**
     * リクエストボディを受け取る POSTメソッド.
     * @param url リクエスト対象URL
     * @param data 書き込むデータ
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse post(final String url, final String data, final HashMap<String, String> headers)
            throws DcException {
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        HttpUriRequest req = makePostRequest(url, data, contentType);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, data);
        DcResponse res = request(req);
        return res;
    }

    /**
     * DELETEメソッド.
     * @param url リクエスト対象URL
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public final DcResponse del(final String url, final HashMap<String, String> headers) throws DcException {
        HttpDelete req = new HttpDelete(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        debugHttpRequest(req, "");
        return this.request(req);
    }

    /**
     * GET/POST/PUT/DELETE 以外のメソッド.
     * @param method Httpメソッド
     * @param url リクエスト対象URL
     * @param body リクエストボディ
     * @param headers リクエストヘッダのハッシュマップ
     * @return DcResponse型
     * @throws DcException DAO例外
     */
    public DcResponse request(final String method, String url, String body,
            HashMap<String, String> headers) throws DcException {
        HttpEntityEnclosingRequestBase req = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return method;
            }
        };
        req.setURI(URI.create(url));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.setHeader(entry.getKey(), entry.getValue());
        }
        req.addHeader("X-Personium-Version", DcCoreTestConfig.getCoreVersion());

        if (body != null) {
            HttpEntity httpEntity = null;
            try {
                String bodyStr = toUniversalCharacterNames(body);
                httpEntity = new StringEntity(bodyStr);
            } catch (UnsupportedEncodingException e) {
                throw DcException.create("error while request body encoding : " + e.getMessage(), 0);
            }
            req.setEntity(httpEntity);
        }
        debugHttpRequest(req, body);
        return this.request(req);
    }

    /**
     * GETメソッドのためのリクエストオブジェクト生成.
     * @param url リクエスト対象のURL
     * @param accept メディアタイプ
     * @return リクエストオブジェクト
     */
    protected final HttpUriRequest makeGetRequest(final String url, final String accept) {
        HttpUriRequest request = new HttpGet(url);
        makeCommonHeaders(request, null, accept, null);
        request.addHeader("Accept-Encoding", "gzip");
        return request;
    }

    /**
     * PUTメソッドのためのリクエストオブジェクトを生成.
     * @param url リクエスト対象のURL
     * @param data PUTするデータ
     * @param contentType メディアタイプ
     * @return HttpPutインスタンス
     * @throws DcException DAO例外
     */
    protected final HttpPut makePutRequest(final String url, final String data, final String contentType)
            throws DcException {
        HttpPut request = new HttpPut(url);
        HttpEntity body = null;
        try {
            if (PersoniumRestAdapter.CONTENT_TYPE_JSON.equals(contentType)) {
                String bodyStr = toUniversalCharacterNames(data);
                body = new StringEntity(bodyStr);
            } else {
                body = new StringEntity(data, PersoniumRestAdapter.ENCODE);
            }
        } catch (UnsupportedEncodingException e) {
            throw DcException.create("error while request body encoding : " + e.getMessage(), 0);
        }
        request.setEntity(body);
        return request;
    }

    /**
     * PUTするリクエストオブジェクトを生成.
     * @param url リクエスト対象のURL
     * @param contentType メディアタイプ
     * @param is PUTするデータストリーム
     * @return HttpPutクラスインスタンス
     * @throws DcException DAO例外
     */
    protected final HttpPut makePutRequestByStream(final String url, final String contentType, final InputStream is)
            throws DcException {
        HttpPut request = new HttpPut(url);
        InputStreamEntity body;
        body = new InputStreamEntity(is, -1);
        Boolean chunked = true;
        if (chunked != null) {
            body.setChunked(chunked);
        }
        request.setEntity(body);
        return request;
    }

    /**
     * POSTメソッドのためのリクエストオブジェクトを生成.
     * @param url POST対象のURL
     * @param data POSTするデータ
     * @param contentType メディアタイプ
     * @return POSTされたデータ
     * @throws DcException DAO例外
     */
    protected final HttpPost makePostRequest(final String url, final String data, final String contentType)
            throws DcException {
        HttpPost request = new HttpPost(url);
        HttpEntity body = null;
        try {
            String bodyStr = toUniversalCharacterNames(data);
            body = new StringEntity(bodyStr);
        } catch (UnsupportedEncodingException e) {
            throw DcException.create("error while request body encoding : " + e.getMessage(), 0);
        }
        request.setEntity(body);
        makeCommonHeaders(request, contentType, contentType, null);
        return request;
    }

    /**
     * 共通のヘッダをセット.
     * @param req リクエストオブジェクト
     * @param contentType メディア種別
     * @param accept Acceptヘッダ
     * @param etag Etag値
     */
    protected final void makeCommonHeaders(final HttpUriRequest req,
            final String contentType,
            final String accept,
            final String etag) {
        /*
         * String token = accessor.getAccessToken(); if (!token.isEmpty()) { req.setHeader("Authorization",
         * "Token token=\"" + token + "\""); } DaoConfig config = accessor.getDaoConfig(); String version =
         * config.getDcVersion(); if (!"".equals(version)) { req.setHeader("X-Tritium-Version", version); }
         */
        if (contentType != null) {
            req.addHeader("Content-Type", contentType);
        }
        if (accept != null) {
            req.addHeader("Accept", accept);
        }
        if (etag != null) {
            req.addHeader("If-Match", etag);
        }
    }

    /** ステータスコード 300. */
    // private static final int STATUS300 = 300;
    /** ステータスコード 499. */
    // private static final int STATUS499 = 499;

    /**
     * Reponseボディを受ける場合のHTTPリクエストを行う.
     * @param httpReq HTTPリクエスト
     * @return DCレスポンスオブジェクト
     * @throws DcException DAO例外
     */
    private DcResponse request(final HttpUriRequest httpReq) throws DcException {
        try {
            HttpResponse objResponse = httpClient.execute(httpReq);
            DcResponse dcRes = new DcResponse(objResponse);
            /*
             * int statusCode = objResponse.getStatusLine().getStatusCode(); if (statusCode >= STATUS300) {
             * debugHttpResponse(objResponse); throw DcException.create(dcRes.bodyAsString(), statusCode); } if
             * (statusCode > STATUS499) { debugHttpResponse(objResponse); throw DcException.create(dcRes.bodyAsString(),
             * statusCode); }
             */
            debugHttpResponse(objResponse);
            return dcRes;
        } catch (Exception ioe) {
            throw DcException.create("io exception : " + ioe.getMessage(), 0);
        }
    }

    /**
     * Cookieを取得する.
     * @return cookieリスト
     */
    public List<Cookie> getCookies() {
        return ((DefaultHttpClient) httpClient).getCookieStore().getCookies();
    }

    /** 日本語UTFのためのマスク値. */
    private static final int CHAR_MASK = 0x7f;
    /** 日本語UTFのためのマスク値. */
    private static final int CHAR_JPUTF_MASK = 0x10000;

    /**
     * 日本語文字列エンコード.
     * @param inStr エンコード対象の文字列
     * @return エンコード後の文字列
     */
    private String toUniversalCharacterNames(final String inStr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inStr.length(); i++) {
            int c = inStr.charAt(i);
            if (c > CHAR_MASK) {
                sb.append("\\u");
                sb.append(Integer.toHexString((CHAR_JPUTF_MASK + c)).substring(1));
            } else {
                sb.append((char) c);
            }
        }
        return sb.substring(0, sb.length());
    }

    /**
     * デバッグ用.
     * @param res デバッグ出力するResponseオブジェクト
     */
    private void debugHttpResponse(final HttpResponse res) {
        if (log.isDebugEnabled()) {
            log.debug("【Response】 ResponseCode: " + res.getStatusLine().getStatusCode());
            Header[] headers = res.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                log.debug("ResponseHeader[" + headers[i].getName() + "] : " + headers[i].getValue());
            }
        }
    }

    /**
     * デバッグ用.
     * @param req デバッグ出力するRequestオブジェクト
     * @param body デバッグ出力するリクエストボディ
     */
    private void debugHttpRequest(final HttpUriRequest req, final String body) {
        log.debug(req.getURI());
        if (log.isDebugEnabled()) {
            log.debug("【Request】 " + req.getMethod() + "  " + req.getURI());
            Header[] headers = req.getAllHeaders();
            for (int i = 0; i < headers.length; i++) {
                log.debug("RequestHeader[" + headers[i].getName() + "] : " + headers[i].getValue());
            }
            log.debug("RequestBody:  " + body);
        }
    }
}

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
package com.fujitsu.dc.test.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.test.DcCoreTestConfig;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.sun.jersey.core.header.OutBoundHeaders;

/**
 * Httpリクエストを送るテスト用ユーティリティ.
 * <h2>使い方の例</h2>
 * <pre>
 * HttpTest.request("{リクエストファイル}")
 *  .with("キーワード", "値")
 *  .with("キーワード", "値")
 *         .returns()
 *         .statusCode(201);
 * </pre>
 * リクエストファイルは以下のようにHTTPの電文をそのまま記述する。
 * <pre>
 * GET / HTTP/1.1
 * Host: ?
 * Connection: close
 * Content-Length: 0
 * </pre>
 */
public class Http {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(Http.class);

    String method;
    String path;
    InputStream is;
    Map<String, String> params;
    byte[] paraBody;
    OutBoundHeaders headers = new OutBoundHeaders();
    URL url;
    Socket socket;
    InputStream sIn;
    OutputStream sOut;
    BufferedReader sReader;
    BufferedOutputStream sWriter;
    private Http() {
        this.params = new HashMap<String, String>();
    }
    /**
     * リクエストを実行しレスポンスを得ます.
     * @return TResponse
     */
    public TResponse returns() {
        BufferedReader br = null;
        try {
            // ファイルの読み込み
            InputStreamReader isr = new InputStreamReader(is, CharEncoding.UTF_8);
            br = new BufferedReader(isr);
            String firstLine = br.readLine();
            firstLine = this.processParams(firstLine);
            String[] l1 = firstLine.split(" ");
            this.method = l1[0];
            this.path = l1[1];
            String protoVersion = l1[2];

            // ソケットOpen
            this.url = new URL(baseUrl + this.path);
            try {
                this.socket = createSocket(this.url);
            } catch (KeyManagementException e) {
                throw new RuntimeException(e);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
            this.sIn = this.socket.getInputStream();
            this.sOut = this.socket.getOutputStream();
            this.sReader = new BufferedReader(new InputStreamReader(this.sIn, CharEncoding.UTF_8));
            this.sWriter = new BufferedOutputStream(this.sOut);

            // 第１行送信
            StringBuilder sb = new StringBuilder();
            sb.append(this.method);
            sb.append(" ");
            sb.append(this.url.getPath());
            if (this.url.getQuery() != null) {
                sb.append("?");
                sb.append(this.url.getQuery());
            }
            sb.append(" ");
            sb.append(protoVersion);
            this.sWriter.write(sb.toString().getBytes(CharEncoding.UTF_8));
            this.sWriter.write(CRLF.getBytes(CharEncoding.UTF_8));
            log.debug("Req Start -------");
            log.debug(sb.toString());

            // Header
            String line = null;
            String lastLine = null;
            int contentLengthLineNum = -1;
            List<String> lines = new ArrayList<String>();
            int i = 0;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    break;
                }
                line = this.processParams(line);
                if (line.toLowerCase().startsWith(HttpHeaders.CONTENT_LENGTH.toLowerCase())) {
                    // Content-Lengthが出てきた行を覚えておく。（複数あったときは後勝ち）
                    contentLengthLineNum = i;
                } else if (line.toLowerCase().startsWith(HttpHeaders.HOST.toLowerCase())) {
                    line = line.replaceAll("\\?", this.url.getAuthority());
                }
                lines.add(line + CRLF);
                lastLine = line;
                i++;
            }

            // Version情報のヘッダを追加
            lines.add("X-Dc-Version: " + DcCoreTestConfig.getCoreVersion() + CRLF);
            String body = null;
            // 前処理で空行でBreakしたときはBodyがあることの証。
            if (line != null) {
                log.debug("Req Body-------");
                i = 1;
                StringWriter sw = new StringWriter();
                int chr;
                while ((chr =  br.read()) != -1) {
                    sw.write((char) chr);
                }
                body = sw.toString();
                body = this.processParams(body);
                // Content-Lengthヘッダの値を設定
                if (contentLengthLineNum != -1) {
                    String contentLength = lines.get(contentLengthLineNum)
                            .replaceAll("\\?", String.valueOf(body.getBytes().length));
                    lines.set(contentLengthLineNum, contentLength);
                }
            } else {
                if (this.paraBody != null) {
                    log.debug("Req Body-------");
                    // バイナリBodyのサイズを取得
                    String contentLength = lines.get(contentLengthLineNum)
                            .replaceAll("\\?", String.valueOf(this.paraBody.length));
                    lines.set(contentLengthLineNum, contentLength);
                } else {
                    // 最終行が空行でなければ空行を送る.
                    if (lastLine.length() > 0) {
                        log.debug("one more CRLF");
                        this.sWriter.write(CRLF.getBytes(CharEncoding.UTF_8));
                    }
                }
            }
            // Headerの送信
            for (String l : lines) {
                this.sWriter.write(l.getBytes(CharEncoding.UTF_8));
                if (log.isDebugEnabled()) {
                    l.replaceAll(CRLF, "");
                    log.debug(l);
                }
            }
            // 改行コード
            this.sWriter.write(CRLF.getBytes(CharEncoding.UTF_8));
            // Bodyの送信
            if (body != null) {
                this.sWriter.write(body.getBytes(CharEncoding.UTF_8));
                log.debug(body);
            }
            // バイナリBodyの送信
            if (this.paraBody != null) {
                this.sWriter.write(this.paraBody);
                this.sWriter.write(CRLF.getBytes(CharEncoding.UTF_8));
            }
            this.sWriter.flush();
            // レスポンスオブジェクト生成
            TResponse ret = new TResponse(this.sReader);
            return ret;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
           throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (this.sWriter != null) {
                    this.sWriter.close();
                }
                if (this.sReader != null) {
                    this.sReader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    // 置換パラメタの処理
    String processParams(String in) {
        String ret = in;
        for (String k : this.params.keySet()) {
            ret = ret.replaceAll("\\$\\{" + k + "\\}", this.params.get(k));
        }
        return ret;
    }


    static final String CRLF = "\r\n";

    /**
     * システムプロパティから接続先のURLを取得する。 指定がない場合はデフォルトのURLを使用する。
     */
    private static String baseUrl = System.getProperty(UrlUtils.PROP_TARGET_URL, UrlUtils.DEFAULT_TARGET_URL);

    /**
     * @param baseUrltoSet baseUrl
     */
    public static void setBaseUrl(String baseUrltoSet) {
        baseUrl = baseUrltoSet;
    }

    /**
     * @return BaseURL
     */
    public static String getBaseUrl() {
        return baseUrl;
    }

    /**
     * リクエストファイルを定義してHttpTestオブジェクトを生成.
     * @param resPath リクエストファイルのりソースパス
     * @return 応答
     */
    public static Http request(String resPath) {
        Http ret = new Http();
        ret.is = ClassLoader.getSystemResourceAsStream("request/" + resPath);
        return ret;
    }
    /**
     * 置換キーワードを追加設定します.
     * @param key key
     * @param value value
     * @return HttpTest
     */
    public Http with(final String key, final String value) {
        this.params.put(key, value);
        return this;
    }
    /**
     * 置換キーワードを追加設定します.
     * @param value value
     * @return HttpTest
     */
    public Http setBodyBinary(final byte[] value) {
        this.paraBody = value;
        return this;
    }
    static final int PORT_HTTP = 80;
    static final int PORT_HTTPS = 443;

    static Socket createSocket(URL url)
            throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, KeyManagementException {
        String host = url.getHost();
        int port = url.getPort();
        String proto = url.getProtocol();
        if (port < 0) {
            if ("https".equals(proto)) {
                port = PORT_HTTPS;
            }
            if ("http".equals(proto)) {
                port = PORT_HTTP;
            }
        }
        log.debug("sock: " + host + ":" + port);
        log.debug("proto: " + proto);
        // HTTPSのときは、証明書チェックなしのいい加減なSSLSocketを作って返す。
        if ("https".equals(proto)) {
            KeyManager[] km = null;
            TrustManager[] tm = {
                new javax.net.ssl.X509TrustManager() {
                    public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                            throws java.security.cert.CertificateException {
                        log.debug("Insecure SSLSocket Impl for Testing: NOP at X509TrustManager#checkClientTrusted");
                    }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                            throws java.security.cert.CertificateException {
                        log.debug("Insecure SSLSocket Impl for Testing: NOP at X509TrustManager#checkServerTrusted");
                    }
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(km, tm, new SecureRandom());
            SocketFactory sf = sslContext.getSocketFactory();
            return (SSLSocket) sf.createSocket(host, port);
        }
        // HTTPSでないときは普通のソケット
        return new Socket(host, port);
    }
}

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
package com.fujitsu.dc.core.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * HttpClientの実装を切り替えてNewする.
 */
public class HttpClientFactory extends DefaultHttpClient {
    /** HTTP通信のタイプ. */
    public static final String TYPE_DEFAULT = "default";
    /** HTTP通信のタイプ. */
    public static final String TYPE_INSECURE = "insecure";

    /** PORT SSL. */
    private static final int PORTHTTPS = 443;
    /** PORT HTTP. */
    private static final int PORTHTTP = 80;
    /** 接続タイムアウト値. */
    private static final int TIMEOUT = 60000; // 20000;

    /**
     * HTTPClientオブジェクトを作成.
     * @param type 通信タイプ
     * @return 作成したHttpClientクラスインスタンス
     */
    public static HttpClient create(final String type) {
        if (TYPE_DEFAULT.equalsIgnoreCase(type)) {
            return new DefaultHttpClient();
        }

        SSLSocketFactory sf = null;
        try {
            if (TYPE_INSECURE.equalsIgnoreCase(type)) {
                sf = createInsecureSSLSocketFactory();
            }
        } catch (Exception e) {
            return null;
        }

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("https", PORTHTTPS, sf));
        schemeRegistry.register(new Scheme("http", PORTHTTP, PlainSocketFactory.getSocketFactory()));
        HttpParams params = new BasicHttpParams();
        ClientConnectionManager cm = new SingleClientConnManager(schemeRegistry);
        // ClientConnectionManager cm = new
        // ThreadSafeClientConnManager(schemeRegistry);
        HttpClient hc = new DefaultHttpClient(cm, params);

        HttpParams params2 = hc.getParams();
        int timeout = TIMEOUT;
        HttpConnectionParams.setConnectionTimeout(params2, timeout); // 接続のタイムアウト
        HttpConnectionParams.setSoTimeout(params2, timeout); // データ取得のタイムアウト
        return hc;
    }

    /**
     * SSLSocketを生成.
     * @return 生成したSSLSocket
     */
    private static SSLSocketFactory createInsecureSSLSocketFactory() {
        // CHECKSTYLE:OFF
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e1) {
            throw new RuntimeException(e1);
        }

        try {
            sslContext.init(null, new TrustManager[] {new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    // System.out.println("getAcceptedIssuers =============");
                    X509Certificate[] ret = new X509Certificate[0];
                    return ret;
                }

                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                    // System.out.println("checkClientTrusted =============");
                }

                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                    // System.out.println("checkServerTrusted =============");
                }
            } }, new SecureRandom());
        } catch (KeyManagementException e1) {
            throw new RuntimeException(e1);
        }
        // CHECKSTYLE:ON

        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, (X509HostnameVerifier) hostnameVerifier);
        // socketFactory.setHostnameVerifier((X509HostnameVerifier)
        // hostnameVerifier);

        return socketFactory;
    }
}

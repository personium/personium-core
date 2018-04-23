/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * HttpClientの実装を切り替えてNewする.
 */
public class HttpClientFactory {
    /** HTTP通信のタイプ. */
    public static final String TYPE_DEFAULT = "default";
    /** HTTP通信のタイプ. */
    public static final String TYPE_INSECURE = "insecure";

    /** 接続タイムアウト値. */
    private static final int TIMEOUT = 75000; // 20000;

    /** Constructor. */
    private HttpClientFactory() {
    }

    /**
     * HTTPClientオブジェクトを作成.
     * @param type 通信タイプ
     * @param connectionTimeout タイムアウト値(ミリ秒)。0の場合はデフォルト値を利用する。
     * @return 作成したHttpClientクラスインスタンス
     */
    public static HttpClient create(final String type, final int connectionTimeout) {
        int timeout = TIMEOUT;
        if (connectionTimeout != 0) {
            timeout = connectionTimeout;
        }
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .setRedirectsEnabled(false)
                .build();

        if (TYPE_DEFAULT.equalsIgnoreCase(type)) {
            return HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .build();
        } else if (!TYPE_INSECURE.equalsIgnoreCase(type)) {
            return null;
        }

        SSLConnectionSocketFactory sf = null;
        try {
            sf = createInsecureSSLConnectionSocketFactory();
        } catch (Exception e) {
            return null;
        }

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sf)
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .build();
        HttpClientConnectionManager cm = new BasicHttpClientConnectionManager(registry);

        HttpClient hc = HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .setConnectionManager(cm)
                .build();

        return hc;
    }

    /**
     * SSLConnectionSocketFactoryを生成.
     * @return 生成したSSLConnectionSocketFactory
     */
    private static SSLConnectionSocketFactory createInsecureSSLConnectionSocketFactory()
            throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException {
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE)
                .build();

        return new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE);
    }
}

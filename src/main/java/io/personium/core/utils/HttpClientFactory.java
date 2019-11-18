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
package io.personium.core.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.ssl.SSLContextBuilder;

/**
 * Switch implementation by changing HttpClient.
 */
public class HttpClientFactory {
    /** Type of HTTP communication.*/
    public static final String TYPE_DEFAULT = "default";
    /** Type of HTTP communication.*/
    public static final String TYPE_INSECURE = "insecure";
    /** Type name of HTTP client that always connect to .*/
    public static final String TYPE_ALWAYS_LOCAL = "alwayslocal";

    /** Connection timeout value.*/
    private static final int TIMEOUT = 60000; // 20000;

    /** Addr local. */
    public static final String IP_ADDR_LOCAL = "127.0.0.1";

    /** Constructor. */
    private HttpClientFactory() {
    }

    /**
     * Create an HTTPClient object.
     * @param type communication type
     * @return HttpClient class instance created
     */
    public static CloseableHttpClient create(final String type) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setRedirectsEnabled(false)
                .build();

        if (TYPE_DEFAULT.equalsIgnoreCase(type)) {
            return HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .useSystemProperties()
                    .build();
        } else if (TYPE_INSECURE.equalsIgnoreCase(type)) {
            return createInsecure(config);
        } else if (TYPE_ALWAYS_LOCAL.equalsIgnoreCase(type)) {
            return createAlwaysLocal(config);
        }
        return null;

    }
    private static CloseableHttpClient createAlwaysLocal(RequestConfig config) {
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
        /* Custom DNS resolver */
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                // Always 127.0.0.1
                return new InetAddress[] { InetAddress.getByName(IP_ADDR_LOCAL) };
            }
        };
        HttpClientConnectionManager cm = new BasicHttpClientConnectionManager(registry,
                null, /* Default ConnectionFactory */
                null, /* Default SchemePortResolver */
                dnsResolver  /* Our DnsResolver */
                );


        return HttpClientBuilder.create()
                                .setDefaultRequestConfig(config)
                                .setConnectionManager(cm)
                                .useSystemProperties()
                                .build();


    }
    private static CloseableHttpClient createInsecure(RequestConfig config) {
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

        return HttpClientBuilder.create()
                                .setDefaultRequestConfig(config)
                                .setConnectionManager(cm)
                                .useSystemProperties()
                                .build();

    }

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

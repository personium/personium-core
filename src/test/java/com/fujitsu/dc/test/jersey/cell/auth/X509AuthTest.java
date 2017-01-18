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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.auth.token.Role;
import com.fujitsu.dc.common.auth.token.TransCellAccessToken;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.Http;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * トランスセルアクセストークンでのX509証明書検証テスト. IT/STのように、サーバとクライアントが分かれている場合は本テストは実施出来ないため、POMの設定で無効化
 */
@RunWith(DcRunner.class)
public class X509AuthTest extends JerseyTest {

    private static String folderPath = "x509/";

    /**
     * コンストラクタ.
     */
    public X509AuthTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ルートCA証明局に署名されていない証明書でのトランスセルアクセストークン認証の確認.
     * @throws IOException IOException
     * @throws CertificateException CertificateException
     * @throws InvalidKeySpecException InvalidKeySpecException
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    @Test
    public final void ルートCA証明局に署名されていない証明書でのトランスセルアクセストークン認証の確認() throws
            NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, IOException {

        String folder = folderPath + "ca_different/";

        try {
            // 使用する証明書を設定
            String privateKeyFileName = ClassLoader.getSystemResource(folder + "pio.key").getPath();
            String certificateFileName = ClassLoader.getSystemResource(folder + "pio.crt").getPath();
            String[] rootCertificateFileNames = new String[1];
            rootCertificateFileNames[0] = ClassLoader.getSystemResource(folder + "cacert.crt").getPath();
            try {
                TransCellAccessToken.configureX509(privateKeyFileName, certificateFileName, rootCertificateFileNames);
            } catch (Exception e) {
                fail(e.getStackTrace().toString());
            }

            // TransCellAccessTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken(UrlUtils.cellRoot(Setup.TEST_CELL1),
                    "https://example/test/#admin", UrlUtils.cellRoot(Setup.TEST_CELL1), new ArrayList<Role>(), null);

            String token = tcat.toTokenString();
            // テスト用トークンを作成したら、サーバ側の証明書をデフォルトに再設定
            try {
                TransCellAccessToken.configureX509(null, null, null);
            } catch (Exception e) {
                fail(e.getStackTrace().toString());
            }
            // testcell1にトークン認証して400
            Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", Setup.TEST_CELL1)
                    .with("assertion", token)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // トランスセルアクセストークンでのデータアクセス
            BoxUtils.get(Setup.TEST_CELL1, token, Setup.TEST_BOX1, HttpStatus.SC_UNAUTHORIZED);
        } finally {
            TransCellAccessToken.configureX509(DcCoreConfig.getX509PrivateKey(), DcCoreConfig.getX509Certificate(),
                    DcCoreConfig.getX509RootCertificate());
        }
    }

    /**
     * トークンのissureのFQDNと証明書のCNが異なる場合のトランスセルアクセストークン認証の確認.
     */
    @Test
    public final void トークンのissureのFQDNと証明書のCNが異なる場合のトランスセルアクセストークン認証の確認() {

        // TransCellAccessTokenを自作
        TransCellAccessToken tcat = new TransCellAccessToken("https://example/test/",
                "https://example/test/#admin", UrlUtils.cellRoot(Setup.TEST_CELL1), new ArrayList<Role>(), null);
        String token = tcat.toTokenString();

        // testcell1にトークン認証して400
        Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", Setup.TEST_CELL1)
                .with("assertion", token)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        // トランスセルアクセストークンでのデータアクセス
        BoxUtils.get(Setup.TEST_CELL1, token, Setup.TEST_BOX1, HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * トークンの証明書の有効期限が切れている場合のトランスセルアクセストークン認証の確認.
     * @throws IOException IOException
     * @throws CertificateException CertificateException
     * @throws InvalidKeySpecException InvalidKeySpecException
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    @Test
    public final void トークンの証明書の有効期限が切れている場合のトランスセルアクセストークン認証の確認() throws
            NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, IOException {

        String folder = folderPath + "server_expiration/";
        try {
            // 使用する証明書を設定
            String privateKeyFileName = ClassLoader.getSystemResource(folder + "pio.key").getPath();
            String certificateFileName = ClassLoader.getSystemResource(folder + "pio2.crt").getPath();
            String[] rootCertificateFileNames = new String[1];
            rootCertificateFileNames[0] = ClassLoader.getSystemResource(folder + "cacert.crt").getPath();
            try {
                TransCellAccessToken.configureX509(privateKeyFileName, certificateFileName, rootCertificateFileNames);
            } catch (Exception e) {
                fail(e.getStackTrace().toString());
            }

            // TransCellAccessTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken("https://localhost/test/",
                    "https://example/test/#admin", UrlUtils.cellRoot(Setup.TEST_CELL1), new ArrayList<Role>(), null);

            String token = tcat.toTokenString();
            // テスト用トークンを作成したら、サーバ側の証明書をデフォルトに再設定
            try {
                TransCellAccessToken.configureX509(null, null, null);
            } catch (Exception e) {
                fail(e.getStackTrace().toString());
            }
            // testcell1にトークン認証して400
            Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", Setup.TEST_CELL1)
                    .with("assertion", token)
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            // トランスセルアクセストークンでのデータアクセス
            BoxUtils.get(Setup.TEST_CELL1, token, Setup.TEST_BOX1, HttpStatus.SC_UNAUTHORIZED);
        } finally {
            TransCellAccessToken.configureX509(DcCoreConfig.getX509PrivateKey(), DcCoreConfig.getX509Certificate(),
                    DcCoreConfig.getX509RootCertificate());
        }

    }

    /**
     * ルートCA証明書が重複設定されている場合のトランスセルアクセストークン認証の確認.
     * @throws IOException IOException
     * @throws CertificateException CertificateException
     * @throws InvalidKeySpecException InvalidKeySpecException
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    @Test
    public final void ルートCA証明書が重複設定されている場合のトランスセルアクセストークン認証の確認() throws
            NoSuchAlgorithmException, InvalidKeySpecException, CertificateException, IOException {

        String folder = folderPath + "server_expiration/";

        try {
            // 使用する証明書を設定
            String[] rootCertificateFileNames = new String[2];
            rootCertificateFileNames[0] = ClassLoader.getSystemResource(folder + "cacert.crt").getPath();
            rootCertificateFileNames[1] = ClassLoader.getSystemResource(folder + "cacert.crt").getPath();
            try {
                TransCellAccessToken.configureX509(null, null, rootCertificateFileNames);
            } catch (Exception e) {
                fail(e.getStackTrace().toString());
            }

            // TransCellAccessTokenを自作
            TransCellAccessToken tcat = new TransCellAccessToken("https://localhost/test/",
                    "https://example/test/#admin", UrlUtils.cellRoot(Setup.TEST_CELL1), new ArrayList<Role>(), null);

            String token = tcat.toTokenString();

            // testcell1にトークン認証して500
            Http.request("authn/saml-cl-c0.txt")
                    .with("remoteCell", Setup.TEST_CELL1)
                    .with("assertion", token)
                    .returns()
                    .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            // トランスセルアクセストークンでのデータアクセス
            BoxUtils.get(Setup.TEST_CELL1, token, Setup.TEST_BOX1, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } finally {
            TransCellAccessToken.configureX509(DcCoreConfig.getX509PrivateKey(), DcCoreConfig.getX509Certificate(),
                    DcCoreConfig.getX509RootCertificate());
        }

    }
}

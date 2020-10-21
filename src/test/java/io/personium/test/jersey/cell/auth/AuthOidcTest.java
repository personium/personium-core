/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.jersey.cell.auth;


import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;

/**
 * OpenID Connect認証のテスト.
 * 正常系のテストには、有効なIDTokenをリアルタイムで取得する必要があるため、
 * V1.3.24での実装では、ダミーのIDTokenを用いた異常系のみに限定する.
 * 検証可能なIDTokenを用いた異常系のテストも同様に未実装.
 * <TODO>: 正常系テスト
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class AuthOidcTest extends PersoniumTest {

    static final String OIDC_PROVIDER_GOOGLE = "google";
    static final String TEST_CELL1 = "testcell1";
    static final String JWT_DUMMY =
         "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIi"
       + "wiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";

    static final String ID_TOKEN_DUMMY =
        "eyJhbGciOiJSUzI1NiIsImtpZCI6ImFiYzZhY2ZhNjFhZGIyMDhjYTk1MjRhNzdlMGNiMTM0OTFkZWM4NDMifQ."
      + "eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiYXRfaGFzaCI6InBKYUd5YThjamhqRzNjcnpvVVR0NFEiLCJ"
      + "hdWQiOiI3NTkyNjQ5MjE2MzItNHZqYzc3czc4aDZzajA5cHJmYWIyMmc4OHRtMnBwMDEuYXBwcy5nb29nbGV1c2"
      + "VyY29udGVudC5jb20iLCJzdWIiOiIxMDc2Mjk5NDI0NjkwMDQ5OTM4MTEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1Z"
      + "SwiYXpwIjoiNzU5MjY0OTIxNjMyLTR2amM3N3M3OGg2c2owOXByZmFiMjJnODh0bTJwcDAxLmFwcHMuZ29vZ2xl"
      + "dXNlcmNvbnRlbnQuY29tIiwiZW1haWwiOiJwZXJzb25pdW0uaW9AZ21haWwuY29tIiwiaWF0IjoxNDYxMzEzNTg"
      + "4LCJleHAiOjE0NjEzMTcxODgsIm5hbWUiOiJwZXJzb25pdW0gaW8iLCJnaXZlbl9uYW1lIjoicGVyc29uaXVtIi"
      + "wiZmFtaWx5X25hbWUiOiJpbyIsImxvY2FsZSI6ImphIn0.NRFBcCk6-rdzwftnKefXOzOWJTtBiZScOz4QArh1B"
      + "ghI9H3f_cONu57SRzPatxbpF7J5-DKWFo9K-T4XNUMD5VgBU3wpvar2ZqZTLOH7yZV_ngxGchSLuITPJRR59yrY"
      + "gmbbjC0Ke5J-shUjb_x39zhAtVMePJRFIpcozzf7Mdz9-HV_nkFEPJkrFdcnPBhxP97GaKUaMjRv41OPoQxfBLW"
      + "2V9HodvgM2RxAYN1zfmWNyGSl84x13NmxEHC8Q9CC6jCsG9p8R-fRF2f_LtD3yIUPuvyNgRErmip7aZiVo40Gle"
      + "txATXXihKG8nE2onz9zCpMXd2gYJ4vt7b_7vNktg";

    static final String INVALID_TOKEN =
        "eyJhbGciOiJSUzI1NiIsImtpZCI6ImFiYzZhY2ZhNjFhZGIymDhjYTk1mjRhNzdlmGNimTm0OTFkZWm4NDmifQ."
      + "eyJpc3miOiJhY2NvdW50cy5Nb29NbGUuY29tIiwiYXRfaGFzaCI6INBKYUd5YThjamhqRzNjcNpvVVR0NFEiLCJ"
      + "hdWQiOiI3NTkyNjQ5mjE2mzItNHZqYzc3czc4aDZzajA5cHJmYWIymmc4OHRtmNBwmDEuYXBwcy5Nb29NbGV1c2"
      + "VyY29udGVudC5jb20iLCJzdWIiOiIxmDc2mjk5NDI0NjkwmDQ5OTm4mTEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1Z"
      + "SwiYXpwIjoiNzU5mjY0OTIxNjmyLTR2amm3N3m3OGg2c2owOXByZmFimjJNODh0bTJwcDAxLmFwcHmuZ29vZ2xl"
      + "dXNlcmNvbNRlbNQuY29tIiwiZW1haWwiOiJwZXJzb25pdW0uaW9AZ21haWwuY29tIiwiaWF0IjoxNDYxmzEzNTg"
      + "4LCJleHAiOjE0NjEzmTcxODgsIm5hbWUiOiJwZXJzb25pdW0gaW8iLCJNaXZlbl9uYW1lIjoicGVyc29uaXVtIi"
      + "wiZmFtaWx5X25hbWUiOiJpbyIsImxvY2FsZSI6ImphIN0.NRFBcCk6-rdzwftNKefXOzOWJTtBiZScOz4QArh1B"
      + "ghI9H3f_cONu57SRzPatxbpF7J5-DKWFo9K-T4XNUmD5VgBU3wpvar2ZqZTLOH7yZV_NgxGchSLuITPJRR59yrY"
      + "gmbbjC0Ke5J-shUjb_x39zhAtVmePJRFIpcozzf7mdz9-HV_NkFEPJkrFdcNPBhxP97GaKUamjRv41OPoQxfBLW"
      + "2V9Hodvgm2RxAYN1zfmWNyGSl84x13NmxEHC8Q9CC6jCsG9p8R-fRF2f_LtD3yIUPuvyNgRErmip7aZiVo40Gle"
      + "txATXXihKG8NE2oNz9zCpmXd2gYJ4vt7b_7vNktg";
/**
     * コンストラクタ.
     */
    public AuthOidcTest() {
        super(new PersoniumCoreApplication());
    }
    /**
     * 不正なIDTokenを指定し400エラーが返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void 不正なIDTokenを指定し400エラーが返却されること()
            throws InterruptedException {
        String accountName = "invalidIdTokenAccount";
        try {
            // テスト用のアカウントを作成
            // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。このため、このテスト独自のAccountを作成する
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME,
                    TEST_CELL1, accountName, "oidc:google", HttpStatus.SC_CREATED);

            Http.request("authn/oidc-auth.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("id_provider", OIDC_PROVIDER_GOOGLE)
                    .with("id_token", "hogefugatest")
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * JWT形式の不正なIDTokenを指定し400エラーが返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void  JWT形式の不正なIDTokenを指定し400エラーが返却されること()
            throws InterruptedException {
        String accountName = "invalidJWTIdTokenAccount";
        try {
            // テスト用のアカウントを作成
            // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。このため、このテスト独自のAccountを作成する
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME,
                    TEST_CELL1, accountName, "oidc:google", HttpStatus.SC_CREATED);

            Http.request("authn/oidc-auth.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("id_provider", OIDC_PROVIDER_GOOGLE)
                    .with("id_token", JWT_DUMMY)
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, HttpStatus.SC_NO_CONTENT);
        }
    }
    /**
     * 有効期限切れのIDTokenを指定し400エラーが返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void 有効期限切れのIDTokenを指定し400エラーが返却されること()
            throws InterruptedException {
        String accountName = "expiredIdTokenAccount";
        try {
            // テスト用のアカウントを作成
            // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。このため、このテスト独自のAccountを作成する
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME,
                    TEST_CELL1, accountName, "oidc:google", HttpStatus.SC_CREATED);

            Http.request("authn/oidc-auth.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("id_provider", OIDC_PROVIDER_GOOGLE)
                    .with("id_token", ID_TOKEN_DUMMY)
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * デコードできないJWT形式の不正なIDTokenを指定し400エラーが返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void デコードできないJWT形式の不正なIDTokenを指定し400エラーが返却されること()
            throws InterruptedException {
        String accountName = "invalidIdTokenAccount";
        try {
            // テスト用のアカウントを作成
            // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。このため、このテスト独自のAccountを作成する
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME,
                    TEST_CELL1, accountName, "oidc:google", HttpStatus.SC_CREATED);

            Http.request("authn/oidc-auth.txt")
                    .with("remoteCell", TEST_CELL1)
                    .with("id_provider", OIDC_PROVIDER_GOOGLE)
                    .with("id_token", INVALID_TOKEN)
                    .returns().statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, accountName, HttpStatus.SC_NO_CONTENT);
        }
    }
}

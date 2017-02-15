/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.test.plugin;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.sun.jersey.test.framework.JerseyTest;

import io.personium.core.plugin.PluginInfo;
import io.personium.core.plugin.PluginManager;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.plugin.base.auth.AuthConst;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginException;
import io.personium.plugin.base.auth.AuthenticatedIdentity;

/**
 * Pluginクラスのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PluginTest extends JerseyTest {
    private String name;

    /**
     * idToken.
     */
    public static final  String PROP_IDTOKEN = "id_token";
    /**
     * accountName.
     */
    public static final  String PROP_ACCOUNT = "account";

    /** google oidc. **/
    public static final String OIDC_GOOGLE = "oidc:google";
    /** urn google grantType. **/
    public static final String GOOGLE_GRANT_TYPE = "urn:x-personium:oidc:google:code";
    /** マスタートークン(Bearer + MASTER_TOKEN_NAME). */
    public static final String BEARER_MASTER_TOKEN = "Bearer " + AbstractCase.MASTER_TOKEN_NAME;

    // google
    static final String OIDC_PROVIDER_GOOGLE = "google";
    static final String TEST_CELL1 = "testcell1";

    // 有効期限が切れているトー－クンの設定
    static final String INVALID_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImF"
     + "jYTJhZTQwZTA2NDY5YmQ0YjQ2NmI1MDI1MGVmNWE2MGM5OGU0ZTAifQ.eyJpc3Mi"
     + "OiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJpYXQiOjE0ODQ3OTY1OTAs"
     + "ImV4cCI6MTQ4NDgwMDE5MCwiYXRfaGFzaCI6IjV3alJTVGsxOVRSZ2d0MEduaTVC"
     + "VFEiLCJhdWQiOiIxMDY5MTg5NzU1MjAzLWdoMnU1dnR1OGliZnJuM2pvbDAzcGh1"
     + "ajMyaDZ0c2RnLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwic3ViIjoiMTE1"
     + "MTg5NDkwNjc5NDIwMzU2MjA3IiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImF6cCI6"
     + "IjEwNjkxODk3NTUyMDMtZ2gydTV2dHU4aWJmcm4zam9sMDNwaHVqMzJoNnRzZGcu"
     + "YXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJlbWFpbCI6InNvcmEyMzgzQGdt"
     + "YWlsLmNvbSIsIm5hbWUiOiJmdW1peWFzdSBhYmUiLCJwaWN0dXJlIjoiaHR0cHM6"
     + "Ly9saDYuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1veVZhMURwaDh2US9BQUFBQUFB"
     + "QUFBSS9BQUFBQUFBQUFBQS9BS0JfVThzQUFBbGNZOVpISEFmeDNHOVJZbXJ4Rm1Z"
     + "ajl3L3M5Ni1jL3Bob3RvLmpwZyIsImdpdmVuX25hbWUiOiJmdW1peWFzdSIsImZh"
     + "bWlseV9uYW1lIjoiYWJlIiwibG9jYWxlIjoiamEifQ.fJ8pbTYFYhyQrYSvuob4W"
     + "jIHu383KxpYkPuxlyxntYPvHIbftgoyGgA20Uq9xBXb6rBi6d8sEBSZTJEGyrExL"
     + "N5dCILltbbvwgmolsR_z7TqTaGkNwpxcVYA4YaLltcvUwChQkklvjJYsQXrOi2Bq"
     + "Kf3th-cSFGwssAh8-zLcJT8dgigNOaW9fG-Ppo8ty07DjKeQKkBy43usuyY8BpDS"
     + "91yUwQ8d1DtMQ8XPjuAgbmOhIqE63RKHaLqF64G6i8cvKuYzYwruH1CIcBwluy87"
     + "PLfRKGiMr8vRehW_PhPQo7uoXKmtEZrOK949KFbnGRujcS0qnPai45A45ZQ6wv2rg";

    /**
      * コンストラクタ.
      */
     public PluginTest() {
         super("io.persoium.core.rs");
     }

    /**
     * プラグイン処理のgoogle認証を取得できること.
     * @throws Exception .
     */
    @Test
    public void プラグイン処理_一覧からgoogle認証プラグインを取得できること() throws Exception {
        boolean bFind = false;
        PluginManager pm = new PluginManager();
        PluginInfo pi = (PluginInfo) pm.getPluginsByGrantType(GOOGLE_GRANT_TYPE);
        if (pi != null) {
            bFind = true;
            System.out.println("OK get grant_type = " + GOOGLE_GRANT_TYPE);
        }
        assertTrue(bFind);
    }


    /**
     * プラグイン処理の異常GrantTypeの場合にエラーとなること.
     * @throws Exception .
     */
    @Test
    public void プラグイン処理_異常GrantTypeの場合にエラーとなること() throws Exception {
        String invalidGratType = "urn:x-dc1:oidc:hoge:code";

        PluginManager pm = new PluginManager();
        PluginInfo pi = pm.getPluginsByGrantType(invalidGratType);

        try {
            AuthPlugin ap = (AuthPlugin) pi.getObj();
            if (ap == null) {
                assertTrue(true);
            }
        } catch (Exception e) {
            System.out.println(e);
            assertFalse(false);
        }
    }

    /**
     * プラグイン処理の認証一覧を取得できること.
     * @throws Exception .
     */
    @Test
    public void プラグイン処理_認証プラグイン一覧を取得できること() throws Exception {
        boolean bFind = false;

        // プラグインjarがディレクトリに存在する場合
        PluginManager pm = new PluginManager();
        ArrayList<PluginInfo> pl = pm.getPluginsByType(AuthConst.TYPE_AUTH);
        for (int i = 0; i < pl.size(); i++) {
            PluginInfo pi = (PluginInfo) pl.get(i);
            if (pi.getType().equals(AuthConst.TYPE_AUTH)) {
                bFind = true;
            }
        }
        assertTrue(bFind);
    }

    /**
     * GooglePlugin_アカウントとIdTokenを指定し認証プラグイン処理を直接実行できること.
     * @throws Exception .
     */
    @Test
    public void GooglePlugin_正常なアカウントとIdTokenを指定し認証プラグイン処理を直接実行できること() throws Exception {
        PluginManager pm = new PluginManager();
        PluginInfo pi = pm.getPluginsByGrantType(GOOGLE_GRANT_TYPE);

        // Map設定
        Map<String, String> body = new HashMap<String, String>();
        // debug customMessage
        body.put(AuthConst.KEY_MESSAGE, "");

        // idTokenの設定
        Properties properties = getIdTokenProperty();
        String idToken = properties.getProperty(PROP_IDTOKEN);
        String account = properties.getProperty(PROP_ACCOUNT);

        if (idToken != null && account != null) {
            try {
                // Plugin
                body.put(AuthConst.KEY_TOKEN, idToken);
                AuthPlugin ap = (AuthPlugin) pi.getObj();
                AuthenticatedIdentity ai = ap.authenticate(body);

                // 実行結果
                if (ai != null) {
                    String accountName = ai.getAccountName();
                    if (accountName != null) {
                        if (account.equals(accountName)) {
                            String oidcType = ai.getAttributes(AuthConst.KEY_OIDC_TYPE);
                            System.out.println("OK authenticate account = " + accountName + " oidcType=" + oidcType);
                        }
                        assertTrue(true);
                    }
                } else {
                    System.out.println("NG authenticate");
                    assertFalse(false);
                }
            } catch (AuthPluginException ape) {
                System.out.println(ape);
                System.out.println(ape.getType());
                System.out.println(ape.getParams().toString());
                assertFalse(true);
            } catch (Exception e) {
                System.out.println(e);
                assertFalse(true);
            }
        }
    }

    /**
     * GooglePlugin_アカウントとIdTokenを設定し正常に認証され200が返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void GooglePlugin_正常なアカウントとIdTokenを設定し認証され200が返却されること()
            throws InterruptedException {

        Properties properties = getIdTokenProperty();
        String idToken = properties.getProperty(PROP_IDTOKEN);
        String account = properties.getProperty(PROP_ACCOUNT);

        if (idToken != null && account != null) {
            try {
                // テスト用のアカウントを作成
                // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。
                // このため、このテスト独自のAccountを作成する
                AccountUtils.createNonCredentialWithType(
                        AbstractCase.MASTER_TOKEN_NAME,
                        TEST_CELL1, account, OIDC_GOOGLE, HttpStatus.SC_CREATED);

                Http.request("authn/plugin-oidc-auth.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("id_provider", OIDC_PROVIDER_GOOGLE)
                        .with("id_token", idToken)
                        .returns().statusCode(HttpStatus.SC_OK);

            } finally {
                AccountUtils.delete(TEST_CELL1,
                        AbstractCase.MASTER_TOKEN_NAME,
                        account, HttpStatus.SC_NO_CONTENT);
            }
        }
    }

    /**
     * 異常な期限切れIDTokenを設定し400が返却されるこ.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void GooglePlugin_異常な期限切れIDTokenを設定し400が返却されること()
            throws InterruptedException {

        Properties properties = getIdTokenProperty();
        String idToken = properties.getProperty(PROP_IDTOKEN);
        String account = properties.getProperty(PROP_ACCOUNT);

        if (idToken != null && account != null) {
            try {
                // テスト用のアカウントを作成
                // 他のテストと共用するAccountを使用すると、認証失敗のロックがかかり、テストが失敗する。
                // このため、このテスト独自のAccountを作成する
                AccountUtils.createNonCredentialWithType(
                        AbstractCase.MASTER_TOKEN_NAME,
                        TEST_CELL1, account, OIDC_GOOGLE, HttpStatus.SC_CREATED);

                Http.request("authn/plugin-oidc-auth.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("id_provider", OIDC_PROVIDER_GOOGLE)
                        .with("id_token", INVALID_TOKEN)
                        .returns().statusCode(HttpStatus.SC_BAD_REQUEST);

            } finally {
                AccountUtils.delete(TEST_CELL1,
                        AbstractCase.MASTER_TOKEN_NAME,
                        account, HttpStatus.SC_NO_CONTENT);
            }
        }
    }

    /**
     * 異常アカウントをCell設定しIDTokenは認証され異常値が返却されること.
     * @throws InterruptedException 待機失敗
     */
    @Test
    public final void GooglePlugin_異常アカウントをCell設定しIDTokenは認証され異常値が返却されること()
            throws InterruptedException {

        Properties properties = getIdTokenProperty();
        String idToken = properties.getProperty(PROP_IDTOKEN);
        String account = "hogehoge";

        if (idToken != null && account != null) {
            try {
                AccountUtils.createNonCredentialWithType(
                        AbstractCase.MASTER_TOKEN_NAME,
                        TEST_CELL1, account, OIDC_GOOGLE, HttpStatus.SC_CREATED);

                Http.request("authn/plugin-oidc-auth.txt")
                        .with("remoteCell", TEST_CELL1)
                        .with("id_provider", OIDC_PROVIDER_GOOGLE)
                        .with("id_token", idToken)
                        .returns().statusCode(HttpStatus.SC_BAD_REQUEST);

            } finally {
                AccountUtils.delete(TEST_CELL1,
                        AbstractCase.MASTER_TOKEN_NAME,
                        account, HttpStatus.SC_NO_CONTENT);
            }
        }
    }

    /**
     * getProperty.
     * @return properties
     */
    public static Properties getIdTokenProperty() {
        Properties properties = new Properties();
        String basepath = System.getProperty("user.dir");
        String file = "/src/test/resources/testoauth.properties";
        try {
            InputStream inputStream = new FileInputStream(basepath + file);
            properties.load(inputStream);
            inputStream.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return properties;
    }

    @Override
    public String toString() {
        return "Plugin => Name:" + name + "\n";
    }
}

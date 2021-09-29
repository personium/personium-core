/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.test.jersey.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.plugin.PluginInfo;
import io.personium.core.plugin.PluginManager;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.plugin.base.PluginException;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthenticatedIdentity;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.Http;

/**
 * Test for AuthPlugin class. This test tests only check if a plugin works correctly. As PluginsLoader loads AuthPlugin
 * specified in properties as default, this test is executed while the dummy AuthPlugin (AuthPluginForTesting) is
 * loaded.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class})
public class PluginTest extends PersoniumTest {
    private String name;

    static final String TEST_CELL1 = "testcell1";

    /**
     * constructor.
     */
    public PluginTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Test if AuthPlugin loaded by PluginManager has authenticate function and it returns AuthenticatedIdentity when it
     * is called.
     * @throws Exception .
     */
    @Test
    public void AuthPlugin_loaded_by_PluginManager_has_authenticate_function_callable() throws Exception {
        String account = AuthPluginForAuthTest.ACCOUNT_NAME;
        String authParam = AuthPluginForAuthTest.CORRECTVALUE_STRING;

        try {
            PluginManager pm = new PluginManager();
            if (pm.size() > 0) {
                PluginInfo pi = pm.getPluginsByGrantType(AuthPluginForAuthTest.GRANT_TYPE);

                // Authenticate paramaters
                Map<String, List<String>> body = new HashMap<String, List<String>>();

                body.put(AuthPluginForAuthTest.KEY_PARAM, Arrays.asList(authParam));
                AuthPlugin ap = (AuthPlugin) pi.getObj();
                AuthenticatedIdentity ai = ap.authenticate(body);

                // Check if AuthenticatedIdentity is correct
                if (ai != null) {
                    String accountName = ai.getAccountName();
                    if (accountName != null) {
                        if (account.equals(accountName)) {
                            String accountType = ai.getAccountType();
                            System.out.println(
                                    "OK authenticate account = " + accountName + " accountType=" + accountType);
                        }
                        assertEquals(account, accountName);
                    }
                } else {
                    fail("No AuthenticatedIdentity is returned");
                }
            }
        } catch (PluginException pe) {
            fail(pe.getMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Return 200 when __token endpoint receives correct parameter.
     * @throws InterruptedException Failed to wait
     */
    @Test
    public final void Token_endpoint_with_plugin_returns_200_when_correct_param_is_requested()
            throws InterruptedException {

        String account = AuthPluginForAuthTest.ACCOUNT_NAME;
        String accountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String authParam = AuthPluginForAuthTest.CORRECTVALUE_STRING;

        try {
            // Creating account for testing.
            // To avoid being locked by authentication failure, this test prepares
            // another account only for this test.
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, account, accountType,
                    HttpStatus.SC_CREATED);

            Http.request("authn/plugin-auth.txt").with("remoteCell", TEST_CELL1)
                    .with("grant_type", AuthPluginForAuthTest.GRANT_TYPE)
                    .with("key_param", AuthPluginForAuthTest.KEY_PARAM).with("param", authParam).returns()
                    .statusCode(HttpStatus.SC_OK);

        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, account, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * Return 400 when __token endpoint receives wrong parameter.
     * @throws InterruptedException Failed to wait
     */
    @Test
    public final void Token_endpoint_with_plugin_returns_400_when_wrong_param_is_requested()
            throws InterruptedException {

        String account = AuthPluginForAuthTest.ACCOUNT_NAME;
        String accountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String authParam = AuthPluginForAuthTest.WRONGVALUE_STRING;

        try {
            // Creating account for testing.
            // To avoid being locked by authentication failure, this test prepares
            // another account only for this test.
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, account, accountType,
                    HttpStatus.SC_CREATED);

            Http.request("authn/plugin-auth.txt").with("remoteCell", TEST_CELL1)
                    .with("grant_type", AuthPluginForAuthTest.GRANT_TYPE)
                    .with("key_param", AuthPluginForAuthTest.KEY_PARAM).with("param", authParam).returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, account, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * Return 400 when the cell does not contain an account specified in AuthenticatedIdentity.
     * @throws InterruptedException Failed to wait
     */
    @Test
    public final void Token_endpoint_with_plugin_returns_400_when_no_account_matched() throws InterruptedException {

        String account = "hogehoge";
        String accountType = AuthPluginForAuthTest.ACCOUNT_TYPE;
        String authParam = AuthPluginForAuthTest.WRONGVALUE_STRING;

        try {
            AccountUtils.createNonCredentialWithType(AbstractCase.MASTER_TOKEN_NAME, TEST_CELL1, account, accountType,
                    HttpStatus.SC_CREATED);

            Http.request("authn/plugin-auth.txt").with("remoteCell", TEST_CELL1)
                    .with("grant_type", AuthPluginForAuthTest.GRANT_TYPE)
                    .with("key_param", AuthPluginForAuthTest.KEY_PARAM).with("param", authParam).returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

        } finally {
            AccountUtils.delete(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, account, HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Plugin => Name:" + name + "\n";
    }
}

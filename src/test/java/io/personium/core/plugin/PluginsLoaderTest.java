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
package io.personium.core.plugin;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.personium.core.PersoniumUnitConfig;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.utils.PluginUtils;
import io.personium.test.categories.Unit;

/**
 * Test for PluginLoader class.
 */
@Category({ Unit.class })
public class PluginsLoaderTest {

    /**
     * Check if PluginLoader can load specified AuthPlugin (single).
     * @throws Exception .
     */
    @Test
    public void PluginsLoader_loads_single_AuthPlugin() throws Exception {
        PluginsLoader pl = new PluginsLoader(AuthPluginForTesting.class.getName());
        ArrayList<PluginInfo> arrPI = pl.loadPlugins(PersoniumUnitConfig.getPluginPath());

        boolean bFind = false;
        for (PluginInfo pluginInfo : arrPI) {
            if (pluginInfo != null) {
                AuthPlugin ap = (AuthPlugin) pluginInfo.getObj();
                String gtype = ap.getGrantType();
                if (AuthPluginForTesting.DEFAULT_GRANT_TYPE.equals(gtype)) {
                    bFind = true;
                }
            }
        }
        assertTrue(AuthPluginForTesting.class.getName() + " is not found", bFind);
    }

    /**
     * Check if PluginLoader can load specified AuthPlugin (multiple).
     * @throws Exception .
     */
    @Test
    public void PluginsLoader_loads_multiple_AuthPlugin() throws Exception {
        PluginsLoader pl = new PluginsLoader(AuthPluginLoaderForTesting.class.getName());
        ArrayList<PluginInfo> arrPI = pl.loadPlugins(PersoniumUnitConfig.getPluginPath());

        Set<String> foundGrantType = new HashSet<String>();
        for (PluginInfo pluginInfo : arrPI) {
            if (pluginInfo != null) {
                AuthPlugin ap = (AuthPlugin) pluginInfo.getObj();
                String gtype = ap.getGrantType();
                foundGrantType.add(gtype);
            }
        }
        assertTrue("Found grantTypes " + foundGrantType + " is not matched with "
                + "AuthPluginsLoaderForTesting.defaultGrantTypes " + AuthPluginLoaderForTesting.DEFAULT_GRANT_TYPES,
                foundGrantType.containsAll(AuthPluginLoaderForTesting.DEFAULT_GRANT_TYPES));
    }

    /**
     * Check if PluginsLoader can load default plugin.
     * @throws Exception .
     */
    @Test
    @SuppressWarnings("unchecked")
    public void PluginManager_loads_default_plugin() throws Exception {
        MockedStatic<PluginUtils> mocked = Mockito.mockStatic(PluginUtils.class);

        // For testing, http requests return dummy
        // as default plugin uses Http Requests during its initialization.
        JSONObject dummyConfig = new JSONObject();
        dummyConfig.put("jwks_uri", "https://localhost/jwks");
        mocked.when(() -> {
            PluginUtils.getHttpJSON(Mockito.anyString());
        }).thenReturn(dummyConfig);

        String keyConfigurationFile = "io.personium.configurationFile";

        String prevConfig = System.getProperty(keyConfigurationFile);
        System.setProperty(keyConfigurationFile, "personium-unit-config-oidcpluginloadertest.properties");

        try {
            PluginManager pm = new PluginManager();
            boolean bHit = (pm.size() > 0);

            Set<String> grantTypes = new HashSet<String>(Arrays.asList("grantType001", "grantType002", "grantType004"));

            // all plugins for specified grantTypes are loaded
            for (String grantType : grantTypes) {
                PluginInfo pi = (PluginInfo) pm.getPluginsByGrantType(grantType);
                if (pi != null) {
                    System.out.println("OK get grant_type = " + grantType);
                } else {
                    bHit = false;
                }
            }
            assertTrue(bHit);
        } finally {
            if (prevConfig == null) {
                System.clearProperty(keyConfigurationFile);
            } else {
                System.setProperty(keyConfigurationFile, prevConfig);
            }
        }
    }

}

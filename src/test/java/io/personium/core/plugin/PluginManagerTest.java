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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumUnitConfig;
import io.personium.plugin.base.auth.AuthConst;
import io.personium.test.categories.Unit;

/**
 * Test class of PluginManager.
 */
@Category({ Unit.class })
public class PluginManagerTest {

    private static String previousConfig = null;

    /**
     * Preparation before executing this test class.
     */
    @BeforeClass
    public static void prepare() {
        previousConfig = PersoniumUnitConfig.getPluginDefaultLoadClassname();
        PersoniumUnitConfig.set(PersoniumUnitConfig.PLUGIN_DEFAULT_LOAD_CLASSNAME,
                AuthPluginLoaderForTesting.class.getName());
    }

    /**
     * Clean up after executing this test class.
     */
    @AfterClass
    public static void cleanup() {
        if (previousConfig == null) {
            PersoniumUnitConfig.getProperties().remove(PersoniumUnitConfig.PLUGIN_DEFAULT_LOAD_CLASSNAME);
        } else {
            PersoniumUnitConfig.set(PersoniumUnitConfig.PLUGIN_DEFAULT_LOAD_CLASSNAME, previousConfig);
        }
    }

    /**
     * Test that PluginManager can find correct plugin by grantType.
     *
     * @throws Exception .
     */
    @Test
    public void PluginManger_can_find_AuthPlugin_by_grantType() throws Exception {
        String testGrantType = AuthPluginLoaderForTesting.DEFAULT_GRANT_TYPES.iterator().next();
        PluginManager pm = new PluginManager();
        assertTrue("PluginManager failed to load plugins", (pm.size() > 0));

        PluginInfo pi = (PluginInfo) pm.getPluginsByGrantType(testGrantType);
        assertNotNull("getPluginsByGrantType [" + testGrantType + "] returns null", pi);
    }

    /**
     * Test that PluginManager returns null if invalid grantType is presented.
     *
     * @throws Exception .
     */
    @Test
    public void PluginManager_returns_null_if_invalid_grantType() throws Exception {
        String invalidGrantType = "urn:x-dc1:oidc:hoge:code";

        PluginManager pm = new PluginManager();
        assertTrue("PluginManager failed to load plugins", (pm.size() > 0));

        PluginInfo pi = pm.getPluginsByGrantType(invalidGratType);
        assertNull("PluginManager returns not null", pi);
    }

    /**
     * PluginManager_can_returns_list_of_AuthPlugin.
     *
     * @throws Exception .
     */
    @Test
    public void PluginManager_can_returns_list_of_AuthPlugin() throws Exception {
        boolean bFind = true;

        PluginManager pm = new PluginManager();
        List<PluginInfo> pl = pm.getPluginsByType(AuthConst.PLUGIN_TYPE);

        assertTrue("PluginManager returns 0 plugin.", (pl.size() > 0));
        for (int i = 0; i < pl.size(); i++) {
            PluginInfo pi = (PluginInfo) pl.get(i);
            if (!pi.getType().equals(AuthConst.PLUGIN_TYPE)) {
                bFind = false;
            }
        }
        assertTrue(bFind);
    }

}

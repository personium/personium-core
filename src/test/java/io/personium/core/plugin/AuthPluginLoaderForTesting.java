/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginLoader;

/**
 * Dummy AuthPluginLoader class for testing.
 */
public class AuthPluginLoaderForTesting implements AuthPluginLoader {

    /** GrantTypes of authPlugin created. */
    public static final Set<String> DEFAULT_GRANT_TYPES = new HashSet<String>(
            Arrays.asList("grantType001", "grantType002"));

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<AuthPlugin> loadInstances() {
        ArrayList<AuthPlugin> results = new ArrayList<AuthPlugin>();
        for (String grantType : DEFAULT_GRANT_TYPES) {
            results.add(new AuthPluginForTesting(grantType, "accountType_for_" + grantType));
        }
        return results;
    }

}

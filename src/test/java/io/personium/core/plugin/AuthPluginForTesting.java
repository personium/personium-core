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

import java.util.List;
import java.util.Map;

import io.personium.plugin.base.auth.AuthConst;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginException;
import io.personium.plugin.base.auth.AuthenticatedIdentity;

/**
 * Dummy AuthPlugin class for testing.
 */
public class AuthPluginForTesting implements AuthPlugin {

    /** default grantType. */
    public static final String DEFAULT_GRANT_TYPE = "urn:x-personium:test:defaultgrant";

    /** default accountType. */
    public static final String DEFAULT_ACCOUNT_TYPE = "accountTypeForTesting";

    /** specified grantType. */
    private String grantType = DEFAULT_GRANT_TYPE;

    /** specified accountType. */
    private String accountType = DEFAULT_ACCOUNT_TYPE;

    /**
     * default constructor.
     */
    public AuthPluginForTesting() {

    }

    /**
     * constructor for setting custom values.
     *
     * @param grantType   grantType which this instance returns
     * @param accountType accountType which this instance returns
     */
    public AuthPluginForTesting(String grantType, String accountType) {
        if (grantType != null) {
            this.grantType = grantType;
        }
        if (accountType != null) {
            this.accountType = accountType;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return AuthConst.PLUGIN_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGrantType() {
        return grantType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccountType() {
        return accountType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticatedIdentity authenticate(Map<String, List<String>> body) throws AuthPluginException {
        return null;
    }

}

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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import io.personium.plugin.auth.oidc.OidcPluginException;
import io.personium.plugin.base.auth.AuthConst;
import io.personium.plugin.base.auth.AuthPlugin;
import io.personium.plugin.base.auth.AuthPluginException;
import io.personium.plugin.base.auth.AuthenticatedIdentity;

/**
 * Dummy class for testing. This class authenticate user with dummy parameters.
 */
public class AuthPluginForAuthTest implements AuthPlugin {

    /** Grant type which this class handles. */
    public static final String GRANT_TYPE = "urn:x-personium:authtest";

    /** Accout type which this class returns after authentication. */
    public static final String ACCOUNT_TYPE = "oidc:plugintest";

    /** Key of parameter for authentication. */
    public static final String KEY_PARAM = "test_param";

    /** Parameter for authentication (correct pattern). */
    public static final String CORRECTVALUE_STRING = "test_param_value";

    /** Parameter for authentication (wrong pattern). */
    public static final String WRONGVALUE_STRING = "test_param_value_wrong";

    /** Account name which this class returns after authentication. */
    public static final String ACCOUNT_NAME = "test_user_auth_plugin_for_auth_test";

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
        return GRANT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccountType() {
        return ACCOUNT_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticatedIdentity authenticate(Map<String, List<String>> body) throws AuthPluginException {
        // TODO: refactoring not to use OidcPluginException

        if (body == null) {
            throw OidcPluginException.REQUIRED_PARAM_MISSING.create("Body");
        }

        List<String> params = body.get(KEY_PARAM);

        if (params == null) {
            // no param
            throw OidcPluginException.REQUIRED_PARAM_MISSING.create(KEY_PARAM);
        }

        String param = params.get(0);

        if (StringUtils.isEmpty(param)) {
            throw OidcPluginException.REQUIRED_PARAM_MISSING.create(KEY_PARAM);
        }

        // verify
        if (CORRECTVALUE_STRING.equals(param)) {
            AuthenticatedIdentity ai = new AuthenticatedIdentity();
            ai.setAccountName(ACCOUNT_TYPE);
            ai.setAccountType(ACCOUNT_NAME);
            return ai;
        } else if (WRONGVALUE_STRING.equals(param)) {
            throw OidcPluginException.AUTHN_FAILED.create();
        } else {
            throw OidcPluginException.INVALID_ID_TOKEN.create();
        }
    }
}

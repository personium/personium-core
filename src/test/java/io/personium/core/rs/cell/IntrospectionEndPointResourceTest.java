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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for IntrospectionEndPointResource.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessContext.class, PersoniumUnitConfig.class,
    PersoniumCoreAuthzException.class, TokenEndPointResource.class })
@Category({ Unit.class })
public class IntrospectionEndPointResourceTest {

    private AccessContext accessContext;
    private IntrospectionEndPointResource introspectResource;

    private void initInstrospectResource() {
        Cell cell = mock(Cell.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        accessContext = mock(AccessContext.class);
        when(cellRsCmp.getAccessContext()).thenReturn(accessContext);
        introspectResource = spy(new IntrospectionEndPointResource(cell, cellRsCmp));
    }

    /**
     * introspect method returns AUTHORIZATION_REQUIRED when accessContext is ANONYMOUS.
     */
    @Test
    public void introspect_returns_AUTHORIZATION_REQUIRED_When_accessContext_is_ANONYMOUS() {
        initInstrospectResource();
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_ANONYMOUS);

        try {
            introspectResource.introspect(null, null, null, null);
            fail();
        } catch (PersoniumCoreAuthzException e) {
            assertEquals(PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.getCode(), e.getCode());
        }
    }

    /**
     * introspect method returns NECESSARY_PRIVILEGE_LACKING when token is not UnitUserToken and schema is null.
     */
    @Test
    public void introspect_returns_NECESSARY_PRIVILEGE_LACKING_When_token_is_not_UnitUserToken_and_schema_is_null() {
        initInstrospectResource();
        // getType returns not TYPE_ANONYMOUS.
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_ANONYMOUS + "_");
        when(accessContext.isUnitUserToken()).thenReturn(false);
        when(accessContext.getSchema()).thenReturn(null);

        try {
            introspectResource.introspect(null, null, null, null);
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(), e.getCode());
        }
    }

    /**
     * introspect method returns OK when token is UnitUserToken.
     */
    @Test
    public void introspect_returns_OK_When_token_is_UnitUserToken() {
        initInstrospectResource();
        // getType returns not TYPE_ANONYMOUS.
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_ANONYMOUS + "_");
        when(accessContext.isUnitUserToken()).thenReturn(true);
        when(accessContext.getSchema()).thenReturn(null);

        var res = introspectResource.introspect(null, null, null, null);
        assertEquals(HttpStatus.SC_OK, res.getStatus());
    }

    /**
     * introspect method returns OK when basic authentication is passed.
     */
    @Test
    public void introspect_returns_OK_when_accessContext_is_INVALID_and_BASIC_auth_is_passed() {
        initInstrospectResource();
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_INVALID);

        String introspectUser = "introspectUser";
        String introspectPass = "introspectPass";

        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        when(PersoniumUnitConfig.getIntrospectUsername()).thenReturn(introspectUser);
        when(PersoniumUnitConfig.getIntrospectPassword()).thenReturn(introspectPass);

        // Auth with introspect username/password
        String authzHeader = "Basic " + CommonUtils.encodeBase64Url(
            (introspectUser + ":" + introspectPass).getBytes()
        );

        var res = introspectResource.introspect(null, authzHeader, null, null);
        assertEquals(HttpStatus.SC_OK, res.getStatus());
    }

    /**
     * introspect method returns NECESSARY_PRIVILEGE_LACKING when basic authentication is failed.
     */
    @Test
    public void introspect_returns_NECESSARY_PRIVILEGE_LACKING_when_BASIC_auth_is_failed() {
        initInstrospectResource();
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_INVALID);

        String introspectUser = "introspectUser";
        String introspectPass = "introspectPass";

        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        when(PersoniumUnitConfig.getIntrospectUsername()).thenReturn(introspectUser);
        when(PersoniumUnitConfig.getIntrospectPassword()).thenReturn(introspectPass);

        // username and password is not separated with colon symbol.
        String authzHeader = "Basic " + CommonUtils.encodeBase64Url(
            (introspectUser + "_" + introspectPass).getBytes()
        );

        try {
            introspectResource.introspect(null, authzHeader, null, null);
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(), e.getCode());
        }
    }

    /**
     * introspect method returns OK when basic client authentication is passed.
     */
    @Test
    public void introspect_returns_OK_when_BASIC_clientAuth_is_passed() {
        initInstrospectResource();
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_INVALID);

        String introspectUser = "introspectUser";
        String introspectPass = "introspectPass";

        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        when(PersoniumUnitConfig.getIntrospectUsername()).thenReturn(introspectUser);
        when(PersoniumUnitConfig.getIntrospectPassword()).thenReturn(introspectPass);

        PowerMockito.mockStatic(TokenEndPointResource.class);
        when(TokenEndPointResource.clientAuth(any(), any(), any(), any())).thenReturn(introspectUser);

        // Auth with introspect clientId/transcellToken
        String authzHeader = "Basic " + CommonUtils.encodeBase64Url(
            ("clientId:transcellToken").getBytes()
        );

        var res = introspectResource.introspect(null, authzHeader, null, null);
        assertEquals(HttpStatus.SC_OK, res.getStatus());
    }

    /**
     * introspect method returns OK when basic client authentication is failed.
     */
    @Test
    public void introspect_returns_NECESSARY_PRIVILEGE_LACKING_BASIC_clientAuth_is_failed() {
        initInstrospectResource();
        when(accessContext.getType()).thenReturn(AccessContext.TYPE_INVALID);

        String introspectUser = "introspectUser";
        String introspectPass = "introspectPass";

        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        when(PersoniumUnitConfig.getIntrospectUsername()).thenReturn(introspectUser);
        when(PersoniumUnitConfig.getIntrospectPassword()).thenReturn(introspectPass);

        // Make clientAuth fail
        PowerMockito.mockStatic(TokenEndPointResource.class);
        when(TokenEndPointResource.clientAuth(any(), any(), any(), any())).thenReturn(null);

        // Auth with introspect clientId/transcellToken
        String authzHeader = "Basic " + CommonUtils.encodeBase64Url(
            ("clientId:transcellToken").getBytes()
        );

        try {
            introspectResource.introspect(null, authzHeader, null, null);
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals(PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode(), e.getCode());
        }
    }
}

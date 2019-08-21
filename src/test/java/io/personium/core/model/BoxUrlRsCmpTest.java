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
package io.personium.core.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for BoxUrlRsCmp.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BoxUrlRsCmp.class })
@Category({ Unit.class })
public class BoxUrlRsCmpTest {

    /** Test class. */
    private BoxUrlRsCmp boxUrlRsCmp;

    /**
     * Test checkAccessContext().
     * normal.
     * Token is UnitUserToken.
     */
    @Test
    public void checkAccessContext_Normal_unit_user_token() {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();
        doReturn(true).when(ac).isUnitUserToken(privilege);

        // Expected result
        // None.

        // Run method
        boxUrlRsCmp.checkAccessContext(privilege);
    }

    /**
     * Test checkAccessContext().
     * normal.
     * TokenSchema match BoxSchema.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Normal_match_box_schema() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();
        doReturn(false).when(ac).isUnitUserToken(privilege);
        doReturn("testSchema").when(ac).getSchema();
        PowerMockito.doReturn(true).when(boxUrlRsCmp, "isMatchesBoxSchema", ac);

        // Expected result
        // None.

        // Run method
        boxUrlRsCmp.checkAccessContext(privilege);
    }

    /**
     * Test checkAccessContext().
     * normal.
     * Token with access authority.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Normal_has_privilege() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();

        doReturn(false).when(ac).isUnitUserToken(privilege);
        doReturn("testSchema").when(ac).getSchema();
        PowerMockito.doReturn(false).when(boxUrlRsCmp, "isMatchesBoxSchema", ac);

        doReturn("none").when(boxUrlRsCmp).getConfidentialLevel();
        doReturn(null).when(boxUrlRsCmp).getBox();
        doNothing().when(ac).checkSchemaAccess("none", null, AcceptableAuthScheme.BEARER);

        doNothing().when(ac).updateBasicAuthenticationStateForResource(null);

        doReturn(true).when(boxUrlRsCmp).hasSubjectPrivilege(privilege);

        // Expected result
        // None.

        // Run method
        boxUrlRsCmp.checkAccessContext(privilege);
    }

    /**
     * Test checkAccessContext().
     * error.
     * Token without access authority.
     * Access type invalid.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_not_has_privilege_type_invalid() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();

        doReturn(false).when(ac).isUnitUserToken(privilege);
        doReturn("testSchema").when(ac).getSchema();
        PowerMockito.doReturn(false).when(boxUrlRsCmp, "isMatchesBoxSchema", ac);

        doReturn("none").when(boxUrlRsCmp).getConfidentialLevel();
        doReturn(null).when(boxUrlRsCmp).getBox();
        doNothing().when(ac).checkSchemaAccess("none", null, AcceptableAuthScheme.BEARER);

        doNothing().when(ac).updateBasicAuthenticationStateForResource(null);

        doReturn(false).when(boxUrlRsCmp).hasSubjectPrivilege(privilege);

        doReturn(AccessContext.TYPE_INVALID).when(ac).getType();
        doThrow(PersoniumCoreException.Server.UNKNOWN_ERROR).when(ac).throwInvalidTokenException(
                AcceptableAuthScheme.BEARER);

        // Run method
        try {
            boxUrlRsCmp.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            assertThat(e.getCode(), is(PersoniumCoreException.Server.UNKNOWN_ERROR.getCode()));
            assertThat(e.getMessage(), is(PersoniumCoreException.Server.UNKNOWN_ERROR.getMessage()));
        }
    }

    /**
     * Test checkAccessContext().
     * error.
     * Token without access authority.
     * Access type anon.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_not_has_privilege_type_anon() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();

        doReturn(false).when(ac).isUnitUserToken(privilege);
        doReturn("testSchema").when(ac).getSchema();
        PowerMockito.doReturn(false).when(boxUrlRsCmp, "isMatchesBoxSchema", ac);

        doReturn("none").when(boxUrlRsCmp).getConfidentialLevel();
        doReturn(null).when(boxUrlRsCmp).getBox();
        doNothing().when(ac).checkSchemaAccess("none", null, AcceptableAuthScheme.BEARER);

        doNothing().when(ac).updateBasicAuthenticationStateForResource(null);

        doReturn(false).when(boxUrlRsCmp).hasSubjectPrivilege(privilege);

        doReturn(AccessContext.TYPE_ANONYMOUS).when(ac).getType();
        doReturn("https://personium/testcell").when(ac).getRealm();

        // Run method
        try {
            boxUrlRsCmp.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(
                    "https://personium/testcell", AcceptableAuthScheme.BEARER);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContext().
     * error.
     * Token without access authority.
     * Access type other.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_not_has_privilege_type_other() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, ac), null, ac, null));
        doReturn(AcceptableAuthScheme.BEARER).when(boxUrlRsCmp).getAcceptableAuthScheme();

        doReturn(false).when(ac).isUnitUserToken(privilege);
        doReturn("testSchema").when(ac).getSchema();
        PowerMockito.doReturn(false).when(boxUrlRsCmp, "isMatchesBoxSchema", ac);

        doReturn("none").when(boxUrlRsCmp).getConfidentialLevel();
        doReturn(null).when(boxUrlRsCmp).getBox();
        doNothing().when(ac).checkSchemaAccess("none", null, AcceptableAuthScheme.BEARER);

        doNothing().when(ac).updateBasicAuthenticationStateForResource(null);

        doReturn(false).when(boxUrlRsCmp).hasSubjectPrivilege(privilege);

        doReturn(AccessContext.TYPE_LOCAL).when(ac).getType();

        // Run method
        try {
            boxUrlRsCmp.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            assertThat(e.getCode(), is(PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode()));
            assertThat(e.getMessage(), is(PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage()));
        }
    }

    /**
     * Test isMatchesBoxSchema().
     * normal.
     * Return true.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void isMatchesBoxSchema_Normal_true() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, null), null, null, null));

        doReturn(null).when(boxUrlRsCmp).getBox();
        doNothing().when(ac).checkSchemaMatches(null);

        // Expected result
        boolean expected = true;

        // Load methods for private
        Method method = BoxUrlRsCmp.class.getDeclaredMethod("isMatchesBoxSchema", AccessContext.class);
        method.setAccessible(true);

        // Run method
        boolean actual = (boolean) method.invoke(boxUrlRsCmp, ac);

        // Confirm result
        assertThat(actual, is(expected));
    }

    /**
     * Test isMatchesBoxSchema().
     * normal.
     * Return false.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void isMatchesBoxSchema_Normal_false() throws Exception {
        // Test method args
        AccessContext ac = mock(AccessContext.class);

        // Mock settings
        boxUrlRsCmp = PowerMockito.spy(new BoxUrlRsCmp(new CellRsCmp(null, null, null), null, null, null));

        doReturn(null).when(boxUrlRsCmp).getBox();
        doThrow(PersoniumCoreException.Auth.SCHEMA_MISMATCH).when(ac).checkSchemaMatches(null);

        // Expected result
        boolean expected = false;

        // Load methods for private
        Method method = BoxUrlRsCmp.class.getDeclaredMethod("isMatchesBoxSchema", AccessContext.class);
        method.setAccessible(true);

        // Run method
        boolean actual = (boolean) method.invoke(boxUrlRsCmp, ac);

        // Confirm result
        assertThat(actual, is(expected));
    }
}

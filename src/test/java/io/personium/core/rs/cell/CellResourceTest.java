/**
 * Personium
 * Copyright 2018-2020 Personium Project Authors
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.UnitUserLockManager;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for CellResource.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessContext.class, ModelFactory.class, PersoniumUnitConfig.class, UnitUserLockManager.class })
@Category({ Unit.class })
public class CellResourceTest {

    /** Test class. */
    private CellResource cellResource;

    /**
     * Mock CellResource.
     * @param cell cell
     * @param cellCmp cellCmp
     * @param cellRsCmp cellRsCmp
     * @param accessContext accessContext
     * @throws Exception Unintended exception in test
     */
    private void initCellResource(
            Cell cell, CellCmp cellCmp, CellRsCmp cellRsCmp, AccessContext accessContext) throws Exception {
        // Mock settings
        doReturn(cell).when(accessContext).getCell();
        PowerMockito.mockStatic(ModelFactory.class);
        PowerMockito.doReturn(cellCmp).when(ModelFactory.class, "cellCmp", anyObject());
        doReturn(true).when(cellCmp).exists();
        PowerMockito.whenNew(CellRsCmp.class).withAnyArguments().thenReturn(cellRsCmp);
        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        PowerMockito.doReturn("u0").when(PersoniumUnitConfig.class, "getEsUnitPrefix");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockType");
        PowerMockito.doReturn("0").when(PersoniumUnitConfig.class, "getLockRetryInterval");
        PowerMockito.doReturn("0").when(PersoniumUnitConfig.class, "getLockRetryTimes");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockMemcachedHost");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockMemcachedPort");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountValidAuthnInterval");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountLockCount");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountLockTime");
        PowerMockito.mockStatic(UnitUserLockManager.class);
        PowerMockito.doReturn(false).when(UnitUserLockManager.class, "hasLockObject", anyString());
        doReturn(Cell.STATUS_NORMAL).when(cellCmp).getCellStatus();
        cellResource = spy(new CellResource(accessContext, null, null, null, null, null));
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * normal.
     * Type is UnitMaster.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Normal_type_unitmaster() throws Exception {
        // Test method args
        String cellOwner = "owner";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_UNIT_MASTER).when(accessContext).getType();

        // Expected result
        // None.

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellResource, cellOwner);
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * normal.
     * Type is UnitUser.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Normal_type_unituser() throws Exception {
        // Test method args
        String cellOwner = "owner";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_UNIT_USER).when(accessContext).getType();
        doReturn("owner").when(accessContext).getSubject();

        // Expected result
        // None.

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        // Run method
        method.invoke(cellResource, cellOwner);
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * error.
     * Type is UnitUser.
     * Owner is null.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Error_type_unituser_owner_null() throws Exception {
        // Test method args
        String cellOwner = null;

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_UNIT_USER).when(accessContext).getType();
        doReturn("owner").when(accessContext).getSubject();

        // Expected result
        PersoniumCoreException expected = PersoniumCoreException.Auth.NOT_YOURS;

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellResource, cellOwner);
            fail("Not throws exception.");
        } catch (InvocationTargetException e) {
            // Confirm result
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * error.
     * Type is invalid.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Error_type_invalid() throws Exception {
        // Test method args
        String cellOwner = "owner";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_INVALID).when(accessContext).getType();
        doReturn(null).when(cellRsCmp).getAcceptableAuthScheme();
        doThrow(PersoniumCoreAuthzException.EXPIRED_ACCESS_TOKEN).when(
                accessContext).throwInvalidTokenException(anyObject());

        // Expected result
        PersoniumCoreAuthzException expected = PersoniumCoreAuthzException.EXPIRED_ACCESS_TOKEN;

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellResource, cellOwner);
            fail("Not throws exception.");
        } catch (InvocationTargetException e) {
            // Confirm result
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreAuthzException.class)));
            PersoniumCoreAuthzException exception = (PersoniumCoreAuthzException) e.getCause();
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * error.
     * Type is anonymous.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Error_type_anonymous() throws Exception {
        // Test method args
        String cellOwner = "owner";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_ANONYMOUS).when(accessContext).getType();
        doReturn("realm").when(accessContext).getRealm();
        doReturn(null).when(cellRsCmp).getAcceptableAuthScheme();

        // Expected result
        PersoniumCoreAuthzException expected = PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED;

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellResource, cellOwner);
            fail("Not throws exception.");
        } catch (InvocationTargetException e) {
            // Confirm result
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreAuthzException.class)));
            PersoniumCoreAuthzException exception = (PersoniumCoreAuthzException) e.getCause();
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContextForCellBulkDeletion().
     * error.
     * Type is local.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextForCellBulkDeletion_Error_type_local() throws Exception {
        // Test method args
        String cellOwner = "owner";

        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_VISITOR).when(accessContext).getType();

        // Expected result
        PersoniumCoreException expected = PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;

        // Load methods for private
        Method method = CellResource.class.getDeclaredMethod("checkAccessContextForCellBulkDeletion", String.class);
        method.setAccessible(true);
        try {
            // Run method
            method.invoke(cellResource, cellOwner);
            fail("Not throws exception.");
        } catch (InvocationTargetException e) {
            // Confirm result
            assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
            assertThat(exception.getCode(), is(expected.getCode()));
            assertThat(exception.getMessage(), is(expected.getMessage()));
        }
    }
}

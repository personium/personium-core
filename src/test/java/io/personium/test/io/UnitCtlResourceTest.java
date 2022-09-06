/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.test.io;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.Privilege;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.unit.UnitCtlResource;
import io.personium.core.utils.UriUtils;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for UnitCtlResource.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ OEntityWrapper.class, AccessContext.class, UriUtils.class })
@Category({ Unit.class })
public class UnitCtlResourceTest {

    /** Test class. */
    private UnitCtlResource unitCtlResource;

    /**
     * Test beforeCreate().
     * normal.
     * Type is UnitUser and subject not null.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void beforeCreate_Normal_type_unituser_subject_not_null() throws Exception {
        // Test method args
        OEntityWrapper oEntityWrapper = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        unitCtlResource = spy(new UnitCtlResource(accessContext));

        doReturn(accessContext).when(unitCtlResource).getAccessContext();
        doReturn(AccessContext.TYPE_UNIT_USER).when(accessContext).getType();

        doReturn("http://personiumunit/admincell/#admin").when(accessContext).getSubject();
        doReturn("http://personiumunit/").when(accessContext).getBaseUri();
        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("personium-localunit:/admincell/#admin").when(UriUtils.class,
                "convertSchemeFromHttpToLocalUnit", "http://personiumunit/admincell/#admin");

        doNothing().when(oEntityWrapper).put("Owner", "personium-localunit:/admincell/#admin");

        // Expected result
        // None.

        // Run method
        unitCtlResource.beforeCreate(oEntityWrapper);

        // Confirm result
        verify(oEntityWrapper, times(1)).put("Owner", "personium-localunit:/admincell/#admin");
    }

    /**
     * Test beforeCreate().
     * normal.
     * Type is UnitUser and subject is null.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void beforeCreate_Normal_type_unituser_subject_null() throws Exception {
        // Test method args
        OEntityWrapper oEntityWrapper = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        unitCtlResource = spy(new UnitCtlResource(accessContext));

        doReturn(accessContext).when(unitCtlResource).getAccessContext();
        doReturn(AccessContext.TYPE_UNIT_USER).when(accessContext).getType();

        doReturn(null).when(accessContext).getSubject();

        // Expected result
        // None.

        // Run method
        unitCtlResource.beforeCreate(oEntityWrapper);

        // Confirm result
        verify(oEntityWrapper, times(0)).put(anyString(), anyString());
    }

    /**
     * Test beforeCreate().
     * normal.
     * Type is UnitMaster.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void beforeCreate_Normal_type_unitmaster() throws Exception {
        // Test method args
        OEntityWrapper oEntityWrapper = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        unitCtlResource = spy(new UnitCtlResource(accessContext));

        doReturn(accessContext).when(unitCtlResource).getAccessContext();
        doReturn(AccessContext.TYPE_UNIT_MASTER).when(accessContext).getType();

        // Expected result
        // None.

        // Run method
        unitCtlResource.beforeCreate(oEntityWrapper);

        // Confirm result
        // None.
    }

    /**
     * Test checkAccessContext().
     * normal.
     * Type is UnitMaster.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Normal_type_unitmaster() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        doReturn(AccessContext.TYPE_UNIT_MASTER).when(ac).getType();

        // Expected result
        // None.

        // Run method
        unitCtlResource.checkAccessContext(privilege);
    }

    /**
     * Test checkAccessContext().
     * error.
     * Type is Invalid.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_type_invalid() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        doReturn(AccessContext.TYPE_INVALID).when(ac).getType();
        doReturn(null).when(unitCtlResource).getAcceptableAuthScheme();
        doThrow(PersoniumCoreAuthzException.EXPIRED_ACCESS_TOKEN).when(ac).throwInvalidTokenException(any());

        // Expected result
        PersoniumCoreAuthzException expected = PersoniumCoreAuthzException.EXPIRED_ACCESS_TOKEN;

        try {
            // Run method
            unitCtlResource.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreAuthzException e) {
            // Confirm result
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContext().
     * error.
     * Type is Anonymous.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_type_anonymous() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        doReturn(AccessContext.TYPE_ANONYMOUS).when(ac).getType();
        doReturn(null).when(unitCtlResource).getAcceptableAuthScheme();
        doReturn("realm").when(ac).getRealm();

        // Expected result
        PersoniumCoreAuthzException expected = PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED;

        try {
            // Run method
            unitCtlResource.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreAuthzException e) {
            // Confirm result
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContext().
     * error.
     * Type is Local.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContext_Error_type_local() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        Privilege privilege = null;

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        doReturn(AccessContext.TYPE_VISITOR).when(ac).getType();

        // Expected result
        PersoniumCoreException expected = PersoniumCoreException.Auth.UNITUSER_ACCESS_REQUIRED;

        try {
            // Run method
            unitCtlResource.checkAccessContext(privilege);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test checkAccessContextPerEntity().
     * normal.
     * Type is UnitMaster.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextPerEntity_Normal_type_unitmaster() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        OEntityWrapper oew = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        Map<String, Object> meta = new HashMap<>();
        meta.put("Owner", "personium-localunit:/admincell/#admin");
        doReturn(meta).when(oew).getMetadata();

        doReturn("http://personiumunit/").when(ac).getBaseUri();
        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personiumunit/admincell/#admin").when(UriUtils.class,
                "convertSchemeFromLocalUnitToHttp", "personium-localunit:/admincell/#admin");

        doReturn(AccessContext.TYPE_UNIT_MASTER).when(ac).getType();

        // Expected result
        // None.

        // Run method
        unitCtlResource.checkAccessContextPerEntity(ac, oew);

        // Confirm result
        // None.
    }

    /**
     * Test checkAccessContextPerEntity().
     * normal.
     * Type is UnitUser and owner equeal subject.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextPerEntity_Normal_type_unituser_owner_equal_subject() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        OEntityWrapper oew = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        Map<String, Object> meta = new HashMap<>();
        meta.put("Owner", "personium-localunit:/admincell/#admin");
        doReturn(meta).when(oew).getMetadata();

        doReturn("http://personiumunit/").when(ac).getBaseUri();
        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personiumunit/admincell/#admin").when(UriUtils.class,
                "convertSchemeFromLocalUnitToHttp", "personium-localunit:/admincell/#admin");

        doReturn(AccessContext.TYPE_UNIT_USER).when(ac).getType();

        doReturn("http://personiumunit/admincell/#admin").when(ac).getSubject();

        // Expected result
        // None.

        // Run method
        unitCtlResource.checkAccessContextPerEntity(ac, oew);

        // Confirm result
        // None.
    }

    /**
     * Test checkAccessContextPerEntity().
     * error.
     * Type is UnitUser and owner not equeal subject.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void checkAccessContextPerEntity_Error_type_unituser_owner_not_equal_subject() throws Exception {
        // Test method args
        AccessContext ac = PowerMockito.mock(AccessContext.class);
        OEntityWrapper oew = PowerMockito.mock(OEntityWrapper.class);

        // Mock settings
        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI("");
        doReturn(uri).when(uriInfo).getBaseUri();
        unitCtlResource = spy(new UnitCtlResource(ac));

        Map<String, Object> meta = new HashMap<>();
        meta.put("Owner", "personium-localunit:/admincell/#admin");
        doReturn(meta).when(oew).getMetadata();

        doReturn("http://personiumunit/").when(ac).getBaseUri();
        PowerMockito.mockStatic(UriUtils.class);
        PowerMockito.doReturn("http://personiumunit/admincell/#admin").when(UriUtils.class,
                "convertSchemeFromLocalUnitToHttp", "personium-localunit:/admincell/#admin");

        doReturn(AccessContext.TYPE_UNIT_USER).when(ac).getType();

        doReturn("http://personiumunit/admincell/#admin2").when(ac).getSubject();

        // Expected result
        // None.

        // Run method
        try {
            unitCtlResource.checkAccessContextPerEntity(ac, oew);
            fail("Not throws exception.");
        } catch (Exception e) {
            // Confirm result
            assertThat(e, is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException exception = (PersoniumCoreException) e;
            assertThat(exception.getCode(), is(PersoniumCoreException.Auth.NOT_YOURS.getCode()));
        }
    }
}

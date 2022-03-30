/**
 * Personium
 * Copyright 2014-2022 Personium Project
 *  - Akio Shimono
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
package io.personium.core.rs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ModelFactory;
import io.personium.core.rs.unit.UnitResource;

/**
 * Unit test class for FacadeResource.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelFactory.class)
public class FacadeResourceTest {
    static Logger log = LoggerFactory.getLogger(FacadeResourceTest.class);
    static volatile String shelterFqdn;

    /**
     * Class-level setup method.
     */
    @BeforeClass
    public static void beforeClass() {
        shelterFqdn = CommonUtils.getFQDN();
        CommonUtils.setFQDN("unit.example");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "8801");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");
    }

    /**
     * Class-level tear-down method.
     */
    @AfterClass
    public static void afterClass() {
        CommonUtils.setFQDN(shelterFqdn);
        PersoniumUnitConfig.reload();
    }

    /**
     * Test for facade() method.
     */
    @Test
    public void facade_PathBased_ValidUrlAccess_ShouldReturn_UnitResource() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");

        URI[] accessUrlList = new URI[] {
                URI.create("https://unit.example:8801/"),
                URI.create("https://unit.example:8801/cell/_ctl"),
                URI.create("https://unit.example:8801/cell/box/file.txt")
        };
        this.assertFacadeReturnsUnitResource(accessUrlList);
    }

    /**
     * Test for facade() method.
     */
    @Test
    public void facade_SubdomainBased_ValidUrlAccess_ShouldReturn_UnitResource() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        URI[] accessUrlList = new URI[] {
                URI.create("https://unit.example:8801/"),
                URI.create("https://unit.example:8801/_ctl"),
                URI.create("https://unit.example:8801/box/file.txt")
        };
        this.assertFacadeReturnsUnitResource(accessUrlList);
    }
    private void assertFacadeReturnsUnitResource(URI[] accessUrlList) {
        for (URI accessUrl : accessUrlList) {
            UriInfo uriInfo = Mockito.mock(UriInfo.class);
            HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

            Mockito.doReturn(null).when(httpServletRequest).getAttribute(anyString());
            Mockito.doReturn(accessUrl).when(uriInfo).getBaseUri();
            PowerMockito.mockStatic(ModelFactory.class);
            PowerMockito.when(ModelFactory.cellFromName(anyString())).thenReturn(null);

            /* Target class of unit test. */
            FacadeResource facadeResource = new FacadeResource();
            Object o = facadeResource.facade(null, null, null, accessUrl.getHost(), null,
                    null, null, null, null, uriInfo, httpServletRequest);
            assertTrue(o instanceof UnitResource);
        }
    }

    /**
     * Test for facade() method.
     */
    @Test
    public void facade_PathBased_InvalidUrlAccess_ShouldThrow_Exception() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");

        URI[] accessUrlList = new URI[] {
                URI.create("https://cell.unit.example:8801/"),
                URI.create("https://127.0.0.1:8801/"),
                URI.create("https://different.example:8801/"),
                URI.create("https://unit.example/_ctl"),
                URI.create("https://unit.example:8800/box/file.txt")
        };
        this.assertFacadeThrowsPersoniumCoreException(accessUrlList,
                PersoniumCoreException.Common.INVALID_URL_AUTHORITY);
    }
    /**
     * Test for facade() method.
     */
    @Test
    public void facade_SubdomainBased_InvalidUrlAccess_ShouldThrow_Exception() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        URI[] accessUrlList2 = new URI[] {
                URI.create("https://cell.unit.example:8801/"),
                URI.create("https://cell.unit.example:8801/_ctl")
        };
        this.assertFacadeThrowsPersoniumCoreException(accessUrlList2,  PersoniumCoreException.Dav.CELL_NOT_FOUND);

        URI[] accessUrlList = new URI[] {
                URI.create("https://cell.unit.example:8800/"),
                URI.create("https://sub.cell.unit.example:8801/"),
                URI.create("https://127.0.0.1:8801/"),
                URI.create("https://different.example:8801/"),
                URI.create("https://unit.example/_ctl"),
                URI.create("https://unit.example:8800/box/file.txt")
        };
        this.assertFacadeThrowsPersoniumCoreException(accessUrlList,
                PersoniumCoreException.Common.INVALID_URL_AUTHORITY);
    }

    private void assertFacadeThrowsPersoniumCoreException(
            URI[] accessUrlList, PersoniumCoreException expectedException) {

        for (URI accessUrl : accessUrlList) {
            UriInfo uriInfo = Mockito.mock(UriInfo.class);
            HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

            Mockito.doReturn(null).when(httpServletRequest).getAttribute(anyString());
            Mockito.doReturn(accessUrl).when(uriInfo).getBaseUri();

            PowerMockito.mockStatic(ModelFactory.class);
            PowerMockito.when(ModelFactory.cellFromName(anyString())).thenReturn(null);

            /* Target class of unit test. */
            FacadeResource facadeResource = new FacadeResource();
            try {
                facadeResource.facade(null, null, null, accessUrl.getHost(), null,
                    null, null, null, null, uriInfo, httpServletRequest);
                fail("Should throw Exception for URL = " + accessUrl.toString());
            } catch (PersoniumCoreException pce) {
                assertEquals(expectedException.getCode(), pce.getCode());
            }
        }
    }
}


/**
 * Personium
 * Copyright 2018-2019 Personium Project Authors
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
package io.personium.core;

import static io.personium.core.PersoniumUnitConfig.UNIT_PORT;
import static io.personium.core.PersoniumUnitConfig.UNIT_SCHEME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.CommonUtils;
import io.personium.test.categories.Unit;

/**
 * Unit Test for PersoniumUnitConfig.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CommonUtils.class)
@Category({ Unit.class })
public class PersoniumUnitConfigTest {
    private static final String TEST_KEY = "io.personium.test.key";

    @BeforeClass
    public static void beforeClass() {
    }

    @Before
    public void before() {
        clear();
    }

    @After
    public void after() {
    }

    @AfterClass
    public static void afterClass() {
        clear();
        PersoniumUnitConfig.reload();
    }

    private static void clear() {
        System.clearProperty(TEST_KEY);
        System.clearProperty(PersoniumUnitConfig.KEY_CONFIG_FILE);
    }

    /**
     * Test getBaseUrl().
     * normal.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getBaseUrl_Noraml() throws Exception {
        String testFqdn = "host.domain";
        PowerMockito.mockStatic(CommonUtils.class);
        PowerMockito.when(CommonUtils.getFQDN()).thenReturn(testFqdn);
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "9998");
        assertEquals("https://host.domain:9998/", PersoniumUnitConfig.getBaseUrl());

        PersoniumUnitConfig.set(UNIT_SCHEME, "http");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
        assertEquals("http://host.domain/", PersoniumUnitConfig.getBaseUrl());

        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "443");
        assertEquals("https://host.domain:443/", PersoniumUnitConfig.getBaseUrl());

        PowerMockito.when(CommonUtils.getFQDN()).thenReturn("localhost");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
        assertEquals("https://localhost/", PersoniumUnitConfig.getBaseUrl());

        PowerMockito.when(CommonUtils.getFQDN()).thenReturn("192.168.1.10");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
        assertEquals("https://192.168.1.10/", PersoniumUnitConfig.getBaseUrl());
    }

    /**
     * getConfigFileInputStream should, when existing file path is specified, then return its InputStream.
     */
    @Test
    public void getConfigFileInputStream_Should_When_ExistingFileSpecified_Then_Return_ItsInputStream() {
        // This file exists in test/resources/
        String configFilePath = ClassLoader.getSystemResource("personium-unit-config.properties.unit").getPath();

        System.setProperty(PersoniumUnitConfig.KEY_CONFIG_FILE, configFilePath);
        PersoniumUnitConfig.reload();
        assertEquals("confValue", PersoniumUnitConfig.get(TEST_KEY));
    }

    /**
     * getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified.
     */
    @Test
    public void getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified()  {
        System.setProperty(PersoniumUnitConfig.KEY_CONFIG_FILE, "some-non-exisiting/path/unit.properties");
        PersoniumUnitConfig.reload();
        assertNotEquals(PersoniumUnitConfig.Status.READ_FROM_SPECIFIED_FILE, PersoniumUnitConfig.getStatus());
    }

    /**
     * reload_ShouldInclude_SystemProperies.
     */
    @Test
    public void reload_ShouldInclude_SystemProperies() {
        String testVal = "SystemPropertyValue";
        System.setProperty(TEST_KEY, testVal);
        PersoniumUnitConfig.reload();
        assertEquals(testVal, PersoniumUnitConfig.get(TEST_KEY));
    }
}

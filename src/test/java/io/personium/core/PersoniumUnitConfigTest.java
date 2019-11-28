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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Properties;

import org.junit.AfterClass;
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
 * Test for PersoniumUnitConfig.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CommonUtils.class)
@Category({ Unit.class })
public class PersoniumUnitConfigTest {
    public static String scheme;
    public static int port;

    @BeforeClass
    public static void beforeClass() {
        scheme = PersoniumUnitConfig.getUnitScheme();
        port = PersoniumUnitConfig.getUnitPort();
    }
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.set(UNIT_SCHEME, scheme);
        PersoniumUnitConfig.set(UNIT_PORT, String.valueOf(port));
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
        PersoniumUnitConfig pUnitConfig = new PersoniumUnitConfig();
        Properties properties = new Properties();
        // This file exists in test/resources/
        String configFilePath = ClassLoader.getSystemResource("personium-unit-config.properties.unit").getPath();
        try {
            properties.load(pUnitConfig.getConfigFileInputStream(configFilePath));
        } catch (IOException e) {
            fail("properties load failure");
        }
        assertEquals("unitTest", properties.getProperty("io.personium.core.testkey"));
    }

    /**
     * getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified.
     */
    @Test
    public void getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified()  {
        System.setProperty(PersoniumUnitConfig.KEY_CONFIG_FILE, "some-non-exisiting/path/unit.properties");
        PersoniumUnitConfig pUnitConfig = new PersoniumUnitConfig();
        Properties properties = pUnitConfig.getPersoniumConfigProperties();
        if (pUnitConfig.status == PersoniumUnitConfig.STATUS_DEFAULT) {
            assertTrue(properties.isEmpty());
        }
    }
    @Test
    public void reload_ShouldInclude_SystemProperies() {
        String testKey = "io.personium.test.key";
        String testVal = "testValue";
        System.setProperty(testKey, testVal);
        PersoniumUnitConfig.reload();
        assertEquals(testVal, PersoniumUnitConfig.get(testKey));
    }
}

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
package io.personium.test.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.test.categories.Unit;

/**
 * Test for PersoniumUnitConfig.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CommonUtils.class)
@Category({ Unit.class })
public class PersoniumUnitConfigIoTest extends PersoniumUnitConfig {

    @BeforeClass
    public static void beforeClass() {
    }
    @AfterClass
    public static void afterClass() {
        PersoniumUnitConfig.reload();
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
        assertEquals("unitTest", PersoniumUnitConfig.get("io.personium.core.testkey"));
    }

    /**
     * getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified.
     */
    @Test
    public void getPersoniumConfigProperties_ShouldReturnEmptyProperty_IfNoValidConfigFileSpecified()  {
        System.setProperty(PersoniumUnitConfig.KEY_CONFIG_FILE, "some-non-exisiting/path/unit.properties");
        PersoniumUnitConfig.reload();
        assertNotEquals(PersoniumUnitConfig.STATUS_READ_FROM_SPECIFIED_FILE, PersoniumUnitConfig.getStatus());
    }
}

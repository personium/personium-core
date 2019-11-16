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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import static io.personium.core.PersoniumUnitConfig.UNIT_PORT;
import static io.personium.core.PersoniumUnitConfig.UNIT_SCHEME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.powermock.api.mockito.PowerMockito;

import io.personium.common.utils.CommonUtils;
import io.personium.test.categories.Unit;

/**
 * Test for PersoniumUnitConfig.
 */

@Category({ Unit.class })
public class PersoniumUnitConfigTest {
    public volatile static String fqdn;
    public static String scheme;
    public static int port;
    @BeforeClass
    public static void beforeClass() {
        fqdn = CommonUtils.getFQDN();
        scheme = PersoniumUnitConfig.getUnitScheme();
        port = PersoniumUnitConfig.getUnitPort();
    }
    @AfterClass
    public static void afterClass() {
        CommonUtils.setFQDN(fqdn);
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
//        PowerMockito.spy(CommonUtils.class);
        CommonUtils.setFQDN(testFqdn);
//        PowerMockito.spy(PersoniumUnitConfig.class);

//        PowerMockito.doReturn("host.domain").when(CommonUtils.class, "getFQDN");
//        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
//        PowerMockito.doReturn(9998).when(PersoniumUnitConfig.class, "getUnitPort");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "9998");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://host.domain:9998/"));

//        PowerMockito.doReturn("host.domain").when(CommonUtils.class, "getFQDN");
        PersoniumUnitConfig.set(UNIT_SCHEME, "http");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
//        PowerMockito.doReturn("http").when(PersoniumUnitConfig.class, "getUnitScheme");
//        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("http://host.domain/"));

//        PowerMockito.doReturn("host.domain").when(CommonUtils.class, "getFQDN");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "443");
//        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
//        PowerMockito.doReturn(443).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://host.domain:443/"));

        CommonUtils.setFQDN("localhost");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
//        PowerMockito.doReturn("localhost").when(CommonUtils.class, "getFQDN");
//        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
//        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertEquals("https://localhost/", PersoniumUnitConfig.getBaseUrl());

        CommonUtils.setFQDN("192.168.1.10");
        PersoniumUnitConfig.set(UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(UNIT_PORT, "-1");
//        PowerMockito.doReturn("192.168.1.10").when(CommonUtils.class, "getFQDN");
//        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
//        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://192.168.1.10/"));
    }

    /**
     *  Subclass for mocking purpose.
     */
    public class UnitTestPersoniumUnitConfig extends PersoniumUnitConfig {
        /**
         * Constructor.
         */
        public UnitTestPersoniumUnitConfig() {
            super();
        }

        /**
         * 設定ファイルを読み込む.
         * @param configFilePath 設定ファイルパス
         * @return 設定ファイルIS
         */
        public InputStream unitGetConfigFileInputStream(String configFilePath) {
            return this.getConfigFileInputStream(configFilePath);
        }
    }

    /**
     * 存在するプロパティファイルのパスを指定した場合_指定したパスのプロパティファイルを読み込むこと.
     */
    @Test
    public void 存在するプロパティファイルのパスを指定した場合_指定したパスのプロパティファイルを読み込むこと() {
        UnitTestPersoniumUnitConfig pUnitConfig = new UnitTestPersoniumUnitConfig();
        Properties properties = new Properties();
        String configFilePath = ClassLoader.getSystemResource("personium-unit-config.properties.unit").getPath();
        try {
            properties.load(pUnitConfig.unitGetConfigFileInputStream(configFilePath));
        } catch (IOException e) {
            fail("properties load failuer");
        }
        assertEquals("unitTest", properties.getProperty("io.personium.core.testkey"));
    }

    /**
     * 存在しないプロパティファイルのパスを指定した場合_クラスパス上のプロパティを読み込むこと.
     */
    @Test
    public void 存在しないプロパティファイルのパスを指定した場合_クラスパス上のプロパティを読み込むこと() {
        UnitTestPersoniumUnitConfig pUnitConfig = new UnitTestPersoniumUnitConfig();
        Properties properties = new Properties();
        try {
            properties.load(pUnitConfig.unitGetConfigFileInputStream("personium-unit-config.properties.unitx"));
        } catch (IOException e) {
            fail("properties load failuer");
        }
        assertNotNull(properties.getProperty("io.personium.core.masterToken"));
    }
}

/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package io.personium.test.unit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.DcCoreConfig;
import io.personium.test.categories.Unit;

/**
 * AccessContext ユニットテストクラス.
 */
@Category({ Unit.class })
public class DcCoreConfigTest {

    /**
     * ユニットテスト用クラス.
     */
    public class UnitDcCoreConfig extends DcCoreConfig {
        /**
         * コンストラクタ.
         */
        public UnitDcCoreConfig() {
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
        UnitDcCoreConfig dcCoreConfig = new UnitDcCoreConfig();
        Properties properties = new Properties();
        String configFilePath = ClassLoader.getSystemResource("personium-unit-config.properties.unit").getPath();
        try {
            properties.load(dcCoreConfig.unitGetConfigFileInputStream(configFilePath));
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
        UnitDcCoreConfig dcCoreConfig = new UnitDcCoreConfig();
        Properties properties = new Properties();
        try {
            properties.load(dcCoreConfig.unitGetConfigFileInputStream("personium-unit-config.properties.unitx"));
        } catch (IOException e) {
            fail("properties load failuer");
        }
        assertNotNull(properties.getProperty("io.personium.core.masterToken"));
    }
}

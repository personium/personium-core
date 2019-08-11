/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.test.categories.Unit;

/**
 * Test for PersoniumUnitConfig.
 */

@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PersoniumCoreUtils.class, PersoniumUnitConfig.class })
public class PersoniumUnitConfigTest {

    /**
     * Test getBaseUrl().
     * normal.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void getBaseUrl_Noraml() throws Exception {
        PowerMockito.spy(PersoniumCoreUtils.class);
        PowerMockito.spy(PersoniumUnitConfig.class);

        PowerMockito.doReturn("host.domain").when(PersoniumCoreUtils.class, "getFQDN");
        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
        PowerMockito.doReturn(9998).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://host.domain:9998/"));

        PowerMockito.doReturn("host.domain").when(PersoniumCoreUtils.class, "getFQDN");
        PowerMockito.doReturn("http").when(PersoniumUnitConfig.class, "getUnitScheme");
        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("http://host.domain/"));

        PowerMockito.doReturn("host.domain").when(PersoniumCoreUtils.class, "getFQDN");
        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
        PowerMockito.doReturn(443).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://host.domain:443/"));

        PowerMockito.doReturn("localhost").when(PersoniumCoreUtils.class, "getFQDN");
        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://localhost/"));

        PowerMockito.doReturn("192.168.1.10").when(PersoniumCoreUtils.class, "getFQDN");
        PowerMockito.doReturn("https").when(PersoniumUnitConfig.class, "getUnitScheme");
        PowerMockito.doReturn(-1).when(PersoniumUnitConfig.class, "getUnitPort");
        assertThat(PersoniumUnitConfig.getBaseUrl(), is("https://192.168.1.10/"));

    }
}

/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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
package io.personium.core.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for UriUtils.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PersoniumUnitConfig.class, UriUtils.class })
public class UriUtilsTest {

    /**
     * Test convertSchemeFromLocalUnitToHttp().
     * normal.
     * path base
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromLocalUnitToHttp_Normal_pathBase() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(true)
                    .when(PersoniumUnitConfig.class, "isPathBasedCellUrlEnabled");
        PowerMockito.doReturn("https://host.domain/")
        .when(PersoniumUnitConfig.class, "getBaseUrl");


        // Single Colon
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/"),
            is("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/"),
            is("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/#account"),
            is("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box"),
            is("https://host.domain/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
            is("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            is("https://host.domain/cell/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            is("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
            is("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:/box"),
            is("https://host.domain/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:/box/col/ent?$inlinecount=allpages"),
            is("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));
    }

    /**
     * Test convertSchemeFromLocalUnitToHttp().
     * normal.
     * fqdn base
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromLocalUnitToHttp_Normal_fqdnBase() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(false)
             .when(PersoniumUnitConfig.class, "isPathBasedCellUrlEnabled");
        PowerMockito.doReturn("https://host.domain/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
        /*
        PowerMockito.spy(UriUtils.class);
        PowerMockito.doReturn("http://cell.host.domain/")
                    .when(UriUtils.class, "convertPathBaseToFqdnBase", "http://host.domain/cell/");
        PowerMockito.doReturn("https://cell.host.domain/")
                    .when(UriUtils.class, "convertPathBaseToFqdnBase", "https://host.domain/cell/");
                    */

        // Single Colon
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            is("https://cell.host.domain/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            is("https://cell.host.domain/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/#account"),
            is("https://cell.host.domain/#account"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box"),
            is("https://cell.host.domain/box"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
            is("https://cell.host.domain/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
        	is("https://cell.host.domain/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
        	is("https://cell.host.domain/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
        		is("https://cell.host.domain/#account"));

    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url starts with uniturl.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_starts_with_uniturl() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(false)
             .when(PersoniumUnitConfig.class, "isPathBasedCellUrlEnabled");
        PowerMockito.doReturn("https://unit.example/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
        assertThat(
            UriUtils.convertSchemeFromHttpToLocalUnit("https://unit.example/cell/"),
            is("personium-localunit:/cell/"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url is fqdn base.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_is_fqdn_base() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn("http://unit.example/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
        String actual = UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/");
        assertThat(actual, is("personium-localunit:cell:/"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url not starts with uniturl.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_not_starts_with_uniturl() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn("http://unit.example/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
/*
        PowerMockito.spy(UriUtils.class);
        PowerMockito.doReturn("http://otherdomain/otherhost/cell/")
                    .when(UriUtils.class, "convertFqdnBaseToPathBase", "http://otherhost.otherdomain/cell/");
                    */
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://otherhost.otherdomain/cell/"),
                is("http://otherhost.otherdomain/cell/"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url is null.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_is_null() throws Exception {
    	try {
            UriUtils.convertSchemeFromHttpToLocalUnit(null);
    	} catch(PersoniumCoreException e) {
            assertEquals(e.getCode(), "PR500-CM-0003");

        }
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url is invalid.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_is_invalid() throws Exception {
        assertThat(UriUtils.convertSchemeFromHttpToLocalUnit("hoge"), is("hoge"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url starts with personium-localbox.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_starts_with_localbox() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localbox:/col", "box");
        assertThat(actual, is("personium-localcell:/box/col"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * boxName is null.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_boxName_is_null() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localbox:/col", null);
        assertThat(actual, is("personium-localbox:/col"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url not starts with personium-localbox.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_not_starts_with_localbox() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localunit:/cell", "box");
        assertThat(actual, is("personium-localunit:/cell"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url starts with personium-localcell.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_starts_with_localcell() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localcell:/cell/box", "box");
        assertThat(actual, is("personium-localcell:/cell/box"));
    }

    /**
     * Test convertFqdnBaseToPathBase().
     * normal.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertFqdnBaseToPathBase_Noraml() throws Exception {
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/"),
                   is("https://host.domain/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col"),
                   is("https://host.domain/cell/box/col"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col/"),
                   is("https://host.domain/cell/box/col/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/#account"),
                   is("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col/ent?$inlinecount=allpages"),
                   is("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host.domain/cell/"),
                   is("https://domain/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host/cell/"),
                   is("https://host/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("hoge"),
                   is("hoge"));
    }

    /**
     * Test convertFqdnBaseToPathBase().
     * error.
     * @throws Exception exception occurred in some errors
     */
    @Test(expected = NullPointerException.class)
    public void convertFqdnBaseToPathBase_Error_null() throws Exception {
        UriUtils.convertFqdnBaseToPathBase(null);
    }

    /**
     * Test convertPathBaseToFqdnBase().
     * normal.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertPathBaseToFqdnBase_Noraml() throws Exception {
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/"),
                   is("https://cell.host.domain/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/box/col"),
                   is("https://cell.host.domain/box/col"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/box/col/"),
                   is("https://cell.host.domain/box/col/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/#account"),
                   is("https://cell.host.domain/#account"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://cell.host.domain/box"),
                   is("https://box.cell.host.domain"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host/cell/"),
                   is("https://cell.host/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("hoge"),
                   is("hoge"));
    }

    /**
     * Test convertPathBaseToFqdnBase().
     * error.
     * @throws Exception exception occurred in some errors
     */
    @Test(expected = NullPointerException.class)
    public void convertPathBaseToFqdnBase_Error_null() throws Exception {
        UriUtils.convertPathBaseToFqdnBase(null);
    }

}

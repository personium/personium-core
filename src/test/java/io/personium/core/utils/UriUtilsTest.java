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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.cell.TokenEndPointResourceTest;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for UriUtils.
 */
@Category({ Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ PersoniumUnitConfig.class, UriUtils.class })
public class UriUtilsTest {
    static Logger log = LoggerFactory.getLogger(TokenEndPointResourceTest.class);

    @AfterClass
    public static void tearDown() {
        PersoniumUnitConfig.reload();
    }


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
            equalTo("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/"),
                equalTo("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/#account"),
                equalTo("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box"),
                equalTo("https://host.domain/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
                equalTo("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://host.domain/cell/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://host.domain/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
                equalTo("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:/box"),
                equalTo("https://host.domain/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:/box/col/ent?$inlinecount=allpages"),
                equalTo("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));
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

        // Single Colon
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            equalTo("https://cell.host.domain/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            equalTo("https://cell.host.domain/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/#account"),
            equalTo("https://cell.host.domain/#account"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box"),
            equalTo("https://cell.host.domain/box"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
            equalTo("https://cell.host.domain/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
                equalTo("https://cell.host.domain/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
                equalTo("https://cell.host.domain/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
                equalTo("https://cell.host.domain/#account"));

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
            equalTo("personium-localunit:/cell/"));
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("https://unit.example/cell/#acct"),
                equalTo("personium-localunit:/cell/#acct"));
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
        PowerMockito.doReturn(false)
            .when(PersoniumUnitConfig.class, "isPathBasedCellUrlEnabled");
        PowerMockito.doReturn("http://unit.example/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
        assertThat(
            UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/"),
            equalTo("personium-localunit:cell:/"));
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/#account"),
                equalTo("personium-localunit:cell:/#account"));
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/ab/?query=23"),
                equalTo("personium-localunit:cell:/ab/?query=23"));
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/ab/?query=23#frag"),
                equalTo("personium-localunit:cell:/ab/?query=23#frag"));
        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.unit.example/ab/#frag?query"),
                equalTo("personium-localunit:cell:/ab/#frag?query"));
    }
    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url is path base.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_is_path_base() throws Exception {
        PowerMockito.spy(PersoniumUnitConfig.class);
        PowerMockito.doReturn(true)
            .when(PersoniumUnitConfig.class, "isPathBasedCellUrlEnabled");
        PowerMockito.doReturn("http://unit.example/")
            .when(PersoniumUnitConfig.class, "getBaseUrl");
        assertThat(
            UriUtils.convertSchemeFromHttpToLocalUnit("http://unit.example/cell/"),
            equalTo("personium-localunit:cell:/"));
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

        assertThat(
                UriUtils.convertSchemeFromHttpToLocalUnit("http://otherhost.otherdomain/cell/"),
                equalTo("http://otherhost.otherdomain/cell/"));
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
        assertThat(UriUtils.convertSchemeFromHttpToLocalUnit("foo"), equalTo("foo"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url starts with personium-localbox.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_starts_with_localbox() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localbox:/col", "box");
        assertThat(actual, equalTo("personium-localcell:/box/col"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * boxName is null.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_boxName_is_null() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localbox:/col", null);
        assertThat(actual, equalTo("personium-localbox:/col"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url not starts with personium-localbox.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_not_starts_with_localbox() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localunit:/cell", "box");
        assertThat(actual, equalTo("personium-localunit:/cell"));
    }

    /**
     * Test convertSchemeFromLocalBoxToLocalCell().
     * normal.
     * url starts with personium-localcell.
     */
    @Test
    public void convertSchemeFromLocalBoxToLocalCell_Noraml_url_starts_with_localcell() {
        String actual = UriUtils.convertSchemeFromLocalBoxToLocalCell("personium-localcell:/cell/box", "box");
        assertThat(actual, equalTo("personium-localcell:/cell/box"));
    }

    /**
     * Test convertFqdnBaseToPathBase().
     * normal.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertFqdnBaseToPathBase_Noraml() throws Exception {
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/"),
                equalTo("https://host.domain/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col"),
                equalTo("https://host.domain/cell/box/col"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col/"),
                equalTo("https://host.domain/cell/box/col/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/#account"),
                equalTo("https://host.domain/cell/#account"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.host.domain/box/col/ent?$inlinecount=allpages"),
                equalTo("https://host.domain/cell/box/col/ent?$inlinecount=allpages"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host.domain/cell/"),
                equalTo("https://domain/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host/cell/"),
                equalTo("https://host/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("hoge"),
                equalTo("hoge"));
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
                equalTo("https://cell.host.domain/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/box/col"),
                equalTo("https://cell.host.domain/box/col"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/box/col/"),
                equalTo("https://cell.host.domain/box/col/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/cell/#account"),
                equalTo("https://cell.host.domain/#account"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://cell.host.domain/box"),
                equalTo("https://box.cell.host.domain"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host/cell/"), equalTo("https://cell.host/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/"), equalTo("https://host.domain/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host.domain/__ctl/"), equalTo("https://host.domain/__ctl/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("hoge"),
                equalTo("hoge"));
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

    @Test
    public void getUrlVariations() {
        // Subdomain based
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "222");
        CommonUtils.setFQDN("unit.example");;

        String urlSingleColon = "personium-localunit:/cell1/";
        String urlDoubleColon = "personium-localunit:cell1:/";
        String urlHttp = "http://cell1.unit.example:222/";
        //
        List<String> result =UriUtils.getUrlVariations(urlDoubleColon);
        for (String r: result) {
            log.info(r);
        }
        // TODO change to 3
        assertEquals(2, result.size());
        assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        // TODO add this
        //assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }

        result =UriUtils.getUrlVariations(urlSingleColon);
        // TODO change to 3
        assertEquals(2, result.size());
        // TODO add this
        // assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }
        result =UriUtils.getUrlVariations(urlHttp);
        // TODO change to 3
        assertEquals(2, result.size());
        // TODO add this
        //assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        // TODO add this
        //assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }
        // Path based
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
    }
}

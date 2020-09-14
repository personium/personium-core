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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for UriUtils.
 */
@Category({ Unit.class })
public class UriUtilsTest {
    static Logger log = LoggerFactory.getLogger(UriUtilsTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonUtils.setFQDN("unit.example");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        PersoniumUnitConfig.reload();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
            PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
    }

    /**
     * Test convertSchemeFromLocalUnitToHttp().
     * normal.
     * path base
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromLocalUnitToHttp_Normal_pathBase() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        // Single Colon
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/"),
            equalTo("https://unit.example/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/"),
            equalTo("https://unit.example/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/#account"),
            equalTo("https://unit.example/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box"),
            equalTo("https://unit.example/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
            equalTo("https://unit.example/cell/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://unit.example/cell/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://unit.example/cell/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
            equalTo("https://unit.example/cell/#account"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:/box"),
            equalTo("https://unit.example/cell/box"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp(
            "personium-localunit:cell:/box/col/ent?$inlinecount=allpages"),
            equalTo("https://unit.example/cell/box/col/ent?$inlinecount=allpages"));
    }

    /**
     * Test convertSchemeFromLocalUnitToHttp().
     * normal.
     * fqdn base
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromLocalUnitToHttp_Normal_fqdnBase() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        // Single Colon
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            equalTo("https://cell.unit.example/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/"),
            equalTo("https://cell.unit.example/"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/#account"),
            equalTo("https://cell.unit.example/#account"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box"),
            equalTo("https://cell.unit.example/box"));
        assertThat(
            UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:/cell/box/col/ent?$inlinecount=allpages"),
            equalTo("https://cell.unit.example/box/col/ent?$inlinecount=allpages"));

        // Double Colons
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://cell.unit.example/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:"),
            equalTo("https://cell.unit.example/"));
        assertThat(UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:cell:#account"),
            equalTo("https://cell.unit.example/#account"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url starts with uniturl.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_starts_with_uniturl() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        assertThat(
            UriUtils.convertSchemeFromHttpToLocalUnit("https://unit.example/cell/"),
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        assertThat(
            UriUtils.convertSchemeFromHttpToLocalUnit("http://cell.otherhost.otherdomain/"),
            equalTo("http://cell.otherhost.otherdomain/"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url not starts with uniturl.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_not_starts_with_uniturl_and_url_is_path_base() throws Exception {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        assertNull(UriUtils.convertSchemeFromHttpToLocalUnit(null));
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
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
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.unit.example/"),
            equalTo("https://unit.example/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.unit.example/box/col"),
            equalTo("https://unit.example/cell/box/col"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.unit.example/box/col/"),
            equalTo("https://unit.example/cell/box/col/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.unit.example/#account"),
            equalTo("https://unit.example/cell/#account"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://cell.unit.example/box/col/ent?$inlinecount=allpages"),
            equalTo("https://unit.example/cell/box/col/ent?$inlinecount=allpages"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host.domain/cell/"),
            equalTo("https://domain/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("https://host/cell/"),
            equalTo("https://host/host/cell/"));
        assertThat(UriUtils.convertFqdnBaseToPathBase("foo"),
            equalTo("foo"));
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
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/cell/"),
            equalTo("https://cell.unit.example/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/cell/box/col"),
            equalTo("https://cell.unit.example/box/col"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/cell/box/col/"),
            equalTo("https://cell.unit.example/box/col/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/cell/#account"),
            equalTo("https://cell.unit.example/#account"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://cell.unit.example/box"),
            equalTo("https://box.cell.unit.example"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://host/cell/"), equalTo("https://cell.host/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/"), equalTo("https://unit.example/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("https://unit.example/__ctl/"),
            equalTo("https://unit.example/__ctl/"));
        assertThat(UriUtils.convertPathBaseToFqdnBase("hoge"), equalTo("hoge"));
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

        String urlSingleColon = "personium-localunit:/cell1/";
        String urlDoubleColon = "personium-localunit:cell1:/";
        String urlHttp = "https://cell1.unit.example/";

        List<String> result = UriUtils.getUrlVariations(urlDoubleColon);
        for (String r: result) {
            log.info(r);
        }
        assertEquals(3, result.size());
        assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }

        result = UriUtils.getUrlVariations(urlSingleColon);
        assertEquals(3, result.size());
        assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }
        result = UriUtils.getUrlVariations(urlHttp);
        assertEquals(3, result.size());
        assertTrue(result.contains(urlDoubleColon));
        assertTrue(result.contains(urlHttp));
        assertTrue(result.contains(urlSingleColon));
        for (String v : result) {
            log.info(v);
        }
        // Path based
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
    }
}

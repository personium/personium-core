/**
 * Personium
 * Copyright 2020 Personium Project Authors
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
package io.personium.core.model;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.IExtRoleContainingToken;
import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;

public class CellTest {
    static Cell testCell;
    static final String CELL_NAME = "testcell";
    static final String CELL_ID = "12345";
    static final String CELL_OWNER = "somebody";
    static Logger log = LoggerFactory.getLogger(CellTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        CommonUtils.setFQDN("testunit.example");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");

        testCell = new Cell() {
            @Override
            public String getDataBundleNameWithOutPrefix() {
                return null;
            }

            @Override
            public String getDataBundleName() {
                return null;
            }

            @Override
            public void makeEmpty() {

            }
            @Override
            public void delete(boolean recursive, String unitUserName) {

            }

            @Override
            public List<Role> getRoleListForAccount(String username) {
                return null;
            }

            @Override
            public List<Role> getRoleListHere(IExtRoleContainingToken token) {
                return null;
            }
            @Override
            public String roleIdToRoleResourceUrl(String roleId) {
                return null;
            }
            @Override
            public String roleResourceUrlToId(String roleUrl, String baseUrl) {
                return null;
            }

        };
        testCell.id = CELL_ID;
        testCell.name = CELL_NAME;
        testCell.owner = CELL_OWNER;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // reload FQDN
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
        // reload unit config
        PersoniumUnitConfig.reload();
    }

    @Test
    public void getUrl() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        String result = testCell.getUrl();
        log.info(result);
        assertEquals("https://testunit.example/testcell/", result);

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        result = testCell.getUrl();
        log.info(result);
        assertEquals("https://testcell.testunit.example/", result);
    }

    @Test
    public void getUnitUrl_Returns_UnitUrl_RegardlessOfCellUrlModes() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        String result = testCell.getUnitUrl();
        assertEquals("https://testunit.example/", result);
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        String result2 = testCell.getUnitUrl();
        assertEquals(result, result2);
    }

    @Test
    public void getOwnerNormalized() {
        // When just a normal string
        testCell.owner = "abcde";
        // Should be same as raw
        assertEquals("abcde", testCell.getOwnerNormalized());

        // When http based url
        testCell.owner = "https://example.com/";
        // Should be same as raw
        assertEquals("https://example.com/", testCell.getOwnerNormalized());

        // When personium-localunit schem url
        testCell.owner = "personium-localunit:cellname:/";
        String result = testCell.getOwnerNormalized();
        log.info(result);
        // Should be resolved to http based url
        assertEquals("https://cellname.testunit.example/", result);

        // When personium-localunit schem url with single colon (old format)
        testCell.owner = "personium-localunit:/cellname/";
        String result2 = testCell.getOwnerNormalized();
        log.info(result2);
        // Still Should be resolved to same http based url
        assertEquals("https://cellname.testunit.example/", result2);

        // set null again;
        testCell.owner = CELL_OWNER;
    }
}

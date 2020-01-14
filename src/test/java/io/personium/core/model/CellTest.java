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
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.UriUtils;

public class CellTest {
    static Cell testCell;
    static final String CELL_NAME = "testcell";
    static final String CELL_ID = "12345";
    static final String CELL_OWNER = "somebody";
    static Logger log = LoggerFactory.getLogger(CellTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());

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
        testCell.owner = CELL_NAME;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        PersoniumUnitConfig.reload();
    }

    @Test
    public void getUrl() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        String result = testCell.getUrl();
        log.info(result);
        assertEquals("http://localhost:9998/testcell/", result);

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        result = testCell.getUrl();
        log.info(result);
        assertEquals("http://testcell.localhost:9998/", result);
    }

    @Test
    public void getUnitUrl_Returns_UnitUrl_RegardlessOfCellUrlModes() {
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        String result = testCell.getUnitUrl();
        assertEquals("http://localhost:9998/", result);
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
        String result2 = testCell.getUnitUrl();
        assertEquals(result, result2);
    }

    @Test
    public void getOwnerNormalized() {
        // When just a normal string
        testCell.owner = "abcde";
        // Should be same as raw
        assertEquals(testCell.getOwnerRaw(), testCell.getOwnerNormalized());

        // When http based url
        testCell.owner = "https://example.com/";
        // Should be same as raw
        assertEquals(testCell.getOwnerRaw(), testCell.getOwnerNormalized());

        // When personium-localunit schem url
        testCell.owner = "personium-localunit:cellname:/";
        String result = testCell.getOwnerNormalized();
        log.info(result);
        // Should be resolved to http based url
        assertEquals(UriUtils.convertSchemeFromLocalUnitToHttp(testCell.getOwnerRaw()), result);

        // When personium-localunit schem url with single colon (old format)
        testCell.owner = "personium-localunit:/cellname/";
        String result2 = testCell.getOwnerNormalized();
        log.info(result2);
        // Still Should be resolved to same http based url
        assertEquals(result, result2);

        // set null again;
        testCell.owner = null;
    }
}

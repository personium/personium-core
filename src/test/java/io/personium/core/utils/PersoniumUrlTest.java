package io.personium.core.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.PersoniumUrl.ResourceType;
import io.personium.core.utils.PersoniumUrl.SchemeType;

public class PersoniumUrlTest {
    static Logger log = LoggerFactory.getLogger(PersoniumUrlTest.class);


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonUtils.setFQDN("unit.example");;
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "false");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        PersoniumUnitConfig.reload();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
    }

    /**
     * Give various url with supported url schemes, and checki the constructed object's schemeType.
     */
    @Test
    public void constructor_schemeType() {
        assertThat(new PersoniumUrl("personium-localcell:/box/path/").schemeType,
                equalTo(SchemeType.LOCAL_CELL));
        assertThat(new PersoniumUrl("personium-localbox:/path/").schemeType,
                equalTo(SchemeType.LOCAL_BOX));
        assertThat(new PersoniumUrl("personium-localunit:cell1:/").schemeType,
                equalTo(SchemeType.LOCAL_UNIT_DOUBLE_COLON));
        assertThat(new PersoniumUrl("personium-localunit:/cell1/").schemeType,
                equalTo(SchemeType.LOCAL_UNIT_SINGLE_COLON));
        assertThat(new PersoniumUrl("http://cell1.unit.example/").schemeType,
                equalTo(SchemeType.HTTP));
        assertThat(new PersoniumUrl("https://cell1.unit.example/").schemeType,
                equalTo(SchemeType.HTTP));
        assertThat(new PersoniumUrl("wss://cell1.unit.example/").schemeType,
                equalTo(SchemeType.WS));
        assertThat(new PersoniumUrl("ws://cell1.unit.example/").schemeType,
                equalTo(SchemeType.WS));
        assertThat(new PersoniumUrl("mailto:cell1@unit.example").schemeType,
                equalTo(SchemeType.INVALID));
    }
    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_UnitLevel() {
        // unit root
        assertThat(new PersoniumUrl("https://unit.example/").resourceType,
                equalTo(ResourceType.UNIT_ROOT));
        assertThat(new PersoniumUrl("https://unit.example").resourceType,
                equalTo(ResourceType.UNIT_ROOT));
        assertThat(new PersoniumUrl("personium-localunit:/").resourceType,
                equalTo(ResourceType.UNIT_ROOT));
        assertThat(new PersoniumUrl("personium-localunit:").resourceType,
                equalTo(ResourceType.UNIT_ROOT));

        // unit level
        assertThat(new PersoniumUrl("https://unit.example/__ctl").resourceType,
                equalTo(ResourceType.UNIT_LEVEL));
        assertThat(new PersoniumUrl("https://unit.example/__ctl/").resourceType,
                equalTo(ResourceType.UNIT_LEVEL));
        assertThat(new PersoniumUrl("https://unit.example/__ctl/Cell").resourceType,
                equalTo(ResourceType.UNIT_LEVEL));
        assertThat(new PersoniumUrl("personium-localunit:/__ctl/Cell").resourceType,
                equalTo(ResourceType.UNIT_LEVEL));
        assertThat(new PersoniumUrl("personium-localunit:/__ctl").resourceType,
                equalTo(ResourceType.UNIT_LEVEL));
    }

    String exampleCellRoot() {
        return "https://cell1.unit.example";
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_CellLevel() {
        // cell root
        assertThat(new PersoniumUrl("personium-localunit:cell1:/").resourceType,
                equalTo(ResourceType.CELL_ROOT));
        assertThat(new PersoniumUrl("personium-localunit:cell1:").resourceType,
                equalTo(ResourceType.CELL_ROOT));
        assertThat(new PersoniumUrl("personium-localcell:/").resourceType,
                equalTo(ResourceType.CELL_ROOT));
        assertThat(new PersoniumUrl("personium-localcell:").resourceType,
                equalTo(ResourceType.CELL_ROOT));


        // cell level
        assertThat(new PersoniumUrl("personium-localcell:/__ctl/").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl("personium-localcell:/__ctl").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl("personium-localcell:/__message").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl("personium-localcell:/__ctl/Account").resourceType,
                equalTo(ResourceType.CELL_LEVEL));

        assertThat(new PersoniumUrl(exampleCellRoot() + "/").resourceType,
                equalTo(ResourceType.CELL_ROOT));
        assertThat(new PersoniumUrl(exampleCellRoot()).resourceType,
                equalTo(ResourceType.CELL_ROOT));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/__ctl/").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/__ctl").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/__ctl/Account('me')").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/__mypassword").resourceType,
                equalTo(ResourceType.CELL_LEVEL));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/__message").resourceType,
                equalTo(ResourceType.CELL_LEVEL));

    }
    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_BoxLevel() {
        // box root
        assertThat(new PersoniumUrl("personium-localbox:/").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl("personium-localbox:").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl("personium-localcell:/bx1/").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl("personium-localcell:/bx1").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl("personium-localcell:/__").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/__").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/__/").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/bx1").resourceType,
                equalTo(ResourceType.BOX_ROOT));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/bx1/").resourceType,
                equalTo(ResourceType.BOX_ROOT));

        // box level
        assertThat(new PersoniumUrl("personium-localbox:/path").resourceType,
                equalTo(ResourceType.BOX_LEVEL));
        assertThat(new PersoniumUrl("personium-localcell:/bx1/path").resourceType,
                equalTo(ResourceType.BOX_LEVEL));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/bx1/path").resourceType,
                equalTo(ResourceType.BOX_LEVEL));
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_ExternalUnit() {
        // host mismatch
        assertThat(new PersoniumUrl("https://cell1.extunit.example/").resourceType,
                equalTo(ResourceType.EXTERNAL_UNIT));
        assertThat(new PersoniumUrl("https://extunit.example/").resourceType,
                equalTo(ResourceType.EXTERNAL_UNIT));
        // port mismatch
        assertThat(new PersoniumUrl("https://cell1.unit.example:8080/").resourceType,
                equalTo(ResourceType.EXTERNAL_UNIT));
        // scheme mismatch
        assertThat(new PersoniumUrl("http://cell1.unit.example/").resourceType,
                equalTo(ResourceType.EXTERNAL_UNIT));
    }
    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_abnormalUrls() {
        try {
            new PersoniumUrl("https:///cell1.extunit.example/");
            fail();
       } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            new PersoniumUrl(null);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try {
            new PersoniumUrl("");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        assertThat(new PersoniumUrl("https://localhost/cell2").resourceType,
                equalTo(ResourceType.EXTERNAL_UNIT));
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_SubSubDomain() {
        try {
            PersoniumUrl pu = new PersoniumUrl("https://cell2.cell1.unit.example/");
            log.info(pu.toString());
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void constructor_cellName() {
        assertNull(new PersoniumUrl("https://unit.example/").cellName);
        assertThat(new PersoniumUrl(exampleCellRoot() + "/").cellName, equalTo("cell1"));
    }
    @Test
    public void constructor_unitDomain() {
        assertNull(new PersoniumUrl("personium-localcell:/box/path/a").unitDomain);
        assertThat(new PersoniumUrl("https://unit.example/").unitDomain, equalTo("unit.example"));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/").unitDomain, equalTo("unit.example"));
    }
    @Test
    public void getLocalHostSignleColonUrl() {
        assertThat(new PersoniumUrl(this.exampleCellRoot()).getLocalUnitSingleColonUrl(),
                equalTo("personium-localunit:/cell1/"));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/").getLocalUnitSingleColonUrl(),
                equalTo("personium-localunit:/cell1/"));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/somePath").getLocalUnitSingleColonUrl(),
                equalTo("personium-localunit:/cell1/somePath"));
    }
    @Test
    public void getLocalHostDoubleColonUrl() {
        assertThat(new PersoniumUrl(this.exampleCellRoot()).toLocalunit(),
                equalTo("personium-localunit:cell1:/"));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/").toLocalunit(),
                equalTo("personium-localunit:cell1:/"));
        assertThat(new PersoniumUrl(this.exampleCellRoot() + "/somePath").toLocalunit(),
                equalTo("personium-localunit:cell1:/somePath"));

        try {
            new PersoniumUrl("https://unit.example/").toLocalunit();
        } catch (Exception e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void getHttpUrl() {
        PersoniumUrl pu;
        // external url should be treated as is;
        pu = new PersoniumUrl("mailto:cell1@unit.example");
        assertThat(pu.toHttp(), equalTo("mailto:cell1@unit.example"));
        pu = new PersoniumUrl("https://cellx.extunit.example/");
        assertThat(pu.toHttp(), equalTo("https://cellx.extunit.example/"));

        // localunit double colon
        pu = new PersoniumUrl("personium-localunit:cell1:");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localunit:cell1:/");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localunit:cell1:/bx/");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx/"));

        pu = new PersoniumUrl("personium-localunit:cell1:/bx");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx/"));

        pu = new PersoniumUrl("personium-localunit:cell1:/__ctl");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/__ctl"));

        // localunit single colon
        pu = new PersoniumUrl("personium-localunit:/cell1");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localunit:/cell1/");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localunit:/cell1/bx");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx/"));

        pu = new PersoniumUrl("personium-localunit:/cell1/__ctl");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/__ctl"));

        pu = new PersoniumUrl("personium-localunit:/__ctl");
        pu.unitDomain = "unit.example";
        assertThat(pu.toHttp(), equalTo("https://unit.example/__ctl"));

        // localcell
        pu = new PersoniumUrl("personium-localcell:");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localcell:/");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/"));

        pu = new PersoniumUrl("personium-localcell:/__ctl");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/__ctl"));

        pu = new PersoniumUrl("personium-localcell:/bx1");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx1/"));

        // localbox
        pu = new PersoniumUrl("personium-localbox:");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        pu.boxName = "bx1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx1/"));

        pu = new PersoniumUrl("personium-localbox:/");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        pu.boxName = "bx1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx1/"));

        pu = new PersoniumUrl("personium-localbox:/some/path.txt");
        pu.unitDomain = "unit.example";
        pu.cellName = "cell1";
        pu.boxName = "bx1";
        assertThat(pu.toHttp(), equalTo(this.exampleCellRoot() + "/bx1/some/path.txt"));

        String[] neverChange = new String[] {
                "https://unit.example/",
                "https://unit.example/__ctl",
                "https://unit.example/__ctl/Cell?q=Name",
                "https://cell1.unit.example/",
                "https://cell1.unit.example/box/",
                "https://cell1.unit.example/__message",
                "https://cell1.unit.example/__ctl",
                "https://cell1.unit.example/__ctl/$metadata",
                "https://cell1.unit.example/__/",
                "https://cell1.unit.example/__/odata",
                "https://cell1.unit.example/__/odata/$metadata",
                "https://cell1.unit.example/box/odata/ent('foo')/_Np",
                "https://cell1.unit.example/bx/eng/svc?query=foo#frag=134",
                "https://external.server.example/",
                "https://external.server.example"
        };
        for (String target: neverChange) {
            assertEquals(target, PersoniumUrl.create(target).toHttp());
        }
        
        String[] willBeAddedSlash = new String[] {
                "https://unit.example",
                "https://cell1.unit.example",
                "https://cell1.unit.example/box",
                "https://cell1.unit.example/__"
        };
        for (String target: willBeAddedSlash) {
            assertThat(PersoniumUrl.create(target).toHttp(), equalTo(target + "/"));
        }

    }

    @Test
    public void addTrailingSlashIfMissing() {
        String urlWoSlash = "https://unit.example";
        String urlWithSlash = "https://unit.example/";
        String result = PersoniumUrl.addTrailingSlashIfMissing(urlWoSlash);
        assertEquals(urlWithSlash, result);
        result = PersoniumUrl.addTrailingSlashIfMissing(urlWithSlash);
        assertEquals(urlWithSlash, result);
    }
}

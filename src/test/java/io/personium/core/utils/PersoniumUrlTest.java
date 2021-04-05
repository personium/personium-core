package io.personium.core.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Before;
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

    @Before
    public void before() {
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
    public void constructor_resourceType_UnitRoot() {
        // unit root
        String[] unitRootNormalized = new String[] {
            "https://unit.example/",
            "https://unit.example/?abc=d",
            "personium-localunit:/?abc=d",
            "personium-localunit:/"
        };
        for (String url : unitRootNormalized) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.UNIT_ROOT));
            assertTrue(new PersoniumUrl(url).isNormalized);
        }
        String[] unitRootNotNormal = new String[] {
            "https://unit.example",
            "https://unit.example/#abc=d",
            "https://unit.example/aa/../",
            "https://unit.example/./aa/../bb/.././",
            "https://unit.example?abc=d",
            "https://unit.example#abc=d",
            "personium-localunit:#abc=d",
            "personium-localunit:?abc=d",
            "personium-localunit:/#abc=d",
            "personium-localunit:/./aa/../bb/.././",
            "personium-localunit:"
        };
        for (String url : unitRootNotNormal) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.UNIT_ROOT));
            assertFalse(new PersoniumUrl(url).isNormalized);
        }
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_UnitLevel() {
        // unit level
        String[] unitLevel = new String[] {
            "https://unit.example/__ctl",
            "https://unit.example/__ctl?abc=d",
            "https://unit.example/__ctl#abc=d",
            "https://unit.example/__ctl/",
            "https://unit.example/__ctl/?abc=d",
            "https://unit.example/__ctl/#abc=d",
            "https://unit.example/__ctl/$metadata",
            "https://unit.example/__ctl/Cell",
            "https://unit.example/__ctl/Cell?$orderby=Name",
            "https://unit.example/__ctl/Cell('foo')",
            "personium-localunit:/__ctl/Cell",
            "personium-localunit:/__ctl/Cell?$orderby=Name",
            "personium-localunit:/__ctl/Cell('foo')",
            "personium-localunit:/__ctl/",
            "personium-localunit:/__ctl?abc=d",
            "personium-localunit:/__ctl#abc=d",
            "personium-localunit:/__ctl"
        };
        for (String url : unitLevel) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.UNIT_LEVEL));
        }
    }

    String exampleCellRoot() {
        return "https://cell1.unit.example";
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_CellRoot() {
        // cell root
        String[] cellRootNormalized = new String[] {
            "personium-localunit:cell1:/",
            "personium-localunit:/cell1/",
            "personium-localcell:/",
            "personium-localcell:/?abc=d",
            exampleCellRoot() + "/?abc=d",
            exampleCellRoot() + "/"
        };
        for (String url : cellRootNormalized) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.CELL_ROOT));
            assertTrue(new PersoniumUrl(url).isNormalized);
        }
        String[] cellRootNotNormal = new String[] {
            "personium-localcell:",
            "personium-localcell:/#abc=d",
            "personium-localunit:cell1:",
            "personium-localunit:/cell1",
            exampleCellRoot() + "/#abc=d",
            exampleCellRoot() + "#abc=d",
            exampleCellRoot() + "?abc=d",
            exampleCellRoot()
        };
        for (String url : cellRootNotNormal) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.CELL_ROOT));
            assertFalse(new PersoniumUrl(url).isNormalized);
        }
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_CellLevel() {
        // cell level
        String[] cellLevel = new String[] {
            "personium-localunit:cell1:__ctl",
            "personium-localunit:cell1:/__ctl",
            "personium-localunit:cell1:/__ctl/",
            "personium-localunit:cell1:/__ctl/Account",
            "personium-localunit:cell1:/__ctl/Account('me')",
            "personium-localcell:/__message",
            "personium-localcell:/__mypassword",
            "personium-localcell:/__ctl",
            "personium-localcell:/__ctl/",
            "personium-localcell:/__ctl/",
            "personium-localcell:/__ctl/Account?$orderby=Name",
            "personium-localcell:/__ctl/Account#abc=d",
            exampleCellRoot() + "/__ctl/",
            exampleCellRoot() + "/__ctl/?abc=d",
            exampleCellRoot() + "/__ctl/#abc=d",
            exampleCellRoot() + "/__ctl/Account?q=%27",
            exampleCellRoot() + "/__ctl/Account?$orderby=Name",
            exampleCellRoot() + "/__ctl?abc=d",
            exampleCellRoot() + "/__ctl#abc=d",
            exampleCellRoot() + "/__ctl"
        };
        for (String url : cellLevel) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.CELL_LEVEL));
        }
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_BoxRoot() {
        // box root
        String[] boxRoot = new String[] {
            "personium-localunit:cell1:/bx",
            "personium-localunit:cell1:/bx/",
            "personium-localcell:/bx",
            "personium-localcell:/bx/",
            "personium-localcell:/__",
            "personium-localcell:/__/",
            "personium-localcell:/__/?abc=d",
            "personium-localcell:/__/#abc=d",
            "personium-localbox:/",
            "personium-localbox:/?abc=d",
            "personium-localbox:/#abc=d",
            "personium-localbox:",
            "personium-localbox:?abc=d",
            "personium-localbox:#abc=d",
            exampleCellRoot() + "/bx",
            exampleCellRoot() + "/bx/",
            exampleCellRoot() + "/bx/?abc=d",
            exampleCellRoot() + "/bx/#abc=d",
            exampleCellRoot() + "/bx?abc=d",
            exampleCellRoot() + "/bx#abc=d",
            exampleCellRoot() + "/__/",
            exampleCellRoot() + "/__"
        };
        for (String url : boxRoot) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.BOX_ROOT));
        }
    }

    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    public void constructor_resourceType_BoxLevel() {
        // box level
        String[] boxLevel = new String[] {
            "personium-localunit:cell1:/bx/col/file.txt",
            "personium-localunit:cell1:/bx/col/file.txt",
            "personium-localcell:/bx/col/file.txt",
            "personium-localcell:/bx/col/file.txt",
            "personium-localcell:/__/col/file.txt",
            "personium-localcell:/__/col/",
            "personium-localbox:/col/file.txt",
            "personium-localbox:/file.txt",
            exampleCellRoot() + "/bx/col/file.txt",
            exampleCellRoot() + "/bx/col/file.txt",
            exampleCellRoot() + "/bx/col?abc=d",
            exampleCellRoot() + "/bx/col#abc=d",
            exampleCellRoot() + "/bx/col/file.txt?abc=d",
            exampleCellRoot() + "/bx/col/file.txt#abc=d",
            exampleCellRoot() + "/__/col/file.txt",
            exampleCellRoot() + "/__/file.txt"
        };
        for (String url : boxLevel) {
            assertThat(new PersoniumUrl(url).resourceType, equalTo(ResourceType.BOX_LEVEL));
        }
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
    @Test(expected = IllegalArgumentException.class)
    public void constructor_SubSubDomain() {
        PersoniumUrl pu = new PersoniumUrl("https://cell2.cell1.unit.example/");
        log.info(pu.toString());
    }

    @Test
    public void normalizePath() {
        assertThat(PersoniumUrl.normalizePath("/aa/../bb"), equalTo("/bb"));
        assertThat(PersoniumUrl.normalizePath("/aa/./bb/./cc"), equalTo("/aa/bb/cc"));
        assertThat(PersoniumUrl.normalizePath("/a/../bb?x=y"), equalTo("/bb?x=y"));
        assertThat(PersoniumUrl.normalizePath("/a?"), equalTo("/a?"));
    }

    @Test
    public void constructor_cellName() {
        assertNull(new PersoniumUrl("https://unit.example/").cellName);
        assertThat(new PersoniumUrl(exampleCellRoot()).cellName, equalTo("cell1"));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/").cellName, equalTo("cell1"));
    }
    @Test
    public void constructor_unitDomain() {
        assertNull(new PersoniumUrl("personium-localcell:/box/path/a").unitDomain);
        assertThat(new PersoniumUrl("https://unit.example/").unitDomain, equalTo("unit.example"));
        assertThat(new PersoniumUrl(exampleCellRoot() + "/").unitDomain, equalTo("unit.example"));
        assertThat(new PersoniumUrl(exampleCellRoot()).unitDomain, equalTo("unit.example"));
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

        String[] urlsThatShouldBe_addedTrailingSlash = new String[] {
                "https://unit.example",
                this.exampleCellRoot(),
                this.exampleCellRoot() + "/box",
                this.exampleCellRoot() + "/__"
        };
        for (String target : urlsThatShouldBe_addedTrailingSlash) {
            assertThat(PersoniumUrl.create(target).toHttp(), equalTo(target + "/"));
        }

        String[] urlThatShouldBeUnchanged = new String[] {
            "https://unit.example/",
            "https://unit.example/__ctl",
            "https://unit.example/__ctl/Cell?q=Name",
            this.exampleCellRoot() + "/",
            this.exampleCellRoot() + "/box/",
            this.exampleCellRoot() + "/__message",
            this.exampleCellRoot() + "/__ctl",
            this.exampleCellRoot() + "/__ctl/$metadata",
            this.exampleCellRoot() + "/__/",
            this.exampleCellRoot() + "/__/odata",
            this.exampleCellRoot() + "/__/odata/$metadata",
            this.exampleCellRoot() + "/box/odata/ent('foo')/_Np",
            this.exampleCellRoot() + "/bx/eng/svc?query=foo#frag=134",
            "https://external.server.example/",
            "https://external.server.example"
        };
        for (String target : urlThatShouldBeUnchanged) {
            assertEquals(target, PersoniumUrl.create(target).toHttp());
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

    @Test
    public void isOnSameUnit_SameUnit() {
        PersoniumUrl url1 = PersoniumUrl.create("https://unit.example");
        PersoniumUrl url2 = PersoniumUrl.create("https://unit.example");
        assertTrue(url1.isOnSameUnit(url2));
    }

    @Test
    public void isOnSameUnit_DifferentUnit() {
        PersoniumUrl url1 = PersoniumUrl.create("https://unit.example");
        PersoniumUrl url2 = PersoniumUrl.create("https://different.example");
        assertFalse(url1.isOnSameUnit(url2));
    }

    @Test
    public void isOnSameCell_SameCell() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example");
        PersoniumUrl url2 = PersoniumUrl.create("https://testcell.unit.example");
        assertTrue(url1.isOnSameCell(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell");
        url2 = PersoniumUrl.create("https://unit.example/testcell");
        assertTrue(url1.isOnSameCell(url2));
    }

    @Test
    public void isOnSameCell_DifferentCell() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example");
        PersoniumUrl url2 = PersoniumUrl.create("https://different.unit.example");
        assertFalse(url1.isOnSameCell(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell");
        url2 = PersoniumUrl.create("https://unit.example/different");
        assertFalse(url1.isOnSameCell(url2));
    }

    @Test
    public void isOnSameCell_DifferentUnit() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example");
        PersoniumUrl url2 = PersoniumUrl.create("https://testcell.different.example");
        assertFalse(url1.isOnSameCell(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell");
        url2 = PersoniumUrl.create("https://different.example/testcell");
        assertFalse(url1.isOnSameCell(url2));
    }

    @Test
    public void isOnSameBox_SameBox() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example/box");
        PersoniumUrl url2 = PersoniumUrl.create("https://testcell.unit.example/box");
        assertTrue(url1.isOnSameBox(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell/box");
        url2 = PersoniumUrl.create("https://unit.example/testcell/box");
        assertTrue(url1.isOnSameBox(url2));
    }

    @Test
    public void isOnSameBox_DifferentBox() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example/box");
        PersoniumUrl url2 = PersoniumUrl.create("https://testcell.unit.example/different");
        assertFalse(url1.isOnSameBox(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell/box");
        url2 = PersoniumUrl.create("https://unit.example/testcell/different");
        assertFalse(url1.isOnSameBox(url2));
    }

    @Test
    public void isOnSameBox_DifferentCell() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example/box");
        PersoniumUrl url2 = PersoniumUrl.create("https://different.unit.example/box");
        assertFalse(url1.isOnSameBox(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell/box");
        url2 = PersoniumUrl.create("https://unit.example/different/box");
        assertFalse(url1.isOnSameBox(url2));
    }

    @Test
    public void isOnSameBox_DifferentUnit() {
        PersoniumUrl url1 = PersoniumUrl.create("https://testcell.unit.example/box");
        PersoniumUrl url2 = PersoniumUrl.create("https://testcell.different.example/box");
        assertFalse(url1.isOnSameBox(url2));

        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
        url1 = PersoniumUrl.create("https://unit.example/testcell/box");
        url2 = PersoniumUrl.create("https://different.example/testcell/box");
        assertFalse(url1.isOnSameBox(url2));
    }

}

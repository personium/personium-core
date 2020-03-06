package io.personium.core.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.PersoniumUrl.ResourceType;

public class PersoniumUrlTest_PathBasedTest extends PersoniumUrlTest{


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonUtils.setFQDN("unit.example");;
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_PORT, "");
        PersoniumUnitConfig.set(PersoniumUnitConfig.UNIT_SCHEME, "https");
        PersoniumUnitConfig.set(PersoniumUnitConfig.PATH_BASED_CELL_URL_ENABLED, "true");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        PersoniumUnitConfig.reload();
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
    }

    @Override
    String exampleCellRoot() {
        return "https://unit.example/cell1";
    }
    /**
     * Give various url with supported url schemes, and check the constructed object's resourceType.
     */
    @Test
    @Override
    public void constructor_SubSubDomain() {
        assertThat(new PersoniumUrl("https://cell2.cell1.unit.example/").resourceType,
            equalTo(ResourceType.EXTERNAL_UNIT));
    }

}

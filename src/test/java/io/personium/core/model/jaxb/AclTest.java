package io.personium.core.model.jaxb;

import static org.junit.Assert.assertEquals;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.UriUtils;

public class AclTest {
    private static Logger log = LoggerFactory.getLogger(AclTest.class);

    private static String unitUrl;


    @BeforeClass
    public static void beforeClass() throws Exception {
        // Configure PersoniumUnitConfig's BaseUrl
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
        unitUrl = PersoniumUnitConfig.getBaseUrl();
    }

    @Test
    public void testGetSetBase_localUnitURL_shouldBeStoredUsing_localUnitScheme() throws Exception {
        Acl acl = new Acl();
        String unitUrl = PersoniumUnitConfig.getBaseUrl();
        String mbUrl = unitUrl + "foo/__/";
        log.info("Configured Unit Url: " + unitUrl);
        // ---------------
        acl.setBase(mbUrl);
        // ---------------
        // URL Should be innternally
        JSONObject j = (JSONObject) new JSONParser().parse(acl.toJSON());
        String baseVal = (String) j.get("@xml.base");
        log.info(j.toJSONString());
        log.info("base: " + baseVal);
        // relativized using localunit scheme
        assertEquals(UriUtils.convertSchemeFromHttpToLocalUnit(mbUrl), baseVal);
        // ---------------
        String retrievedUrl = acl.getBase();
        // ---------------
        assertEquals(mbUrl, retrievedUrl);
    }

}

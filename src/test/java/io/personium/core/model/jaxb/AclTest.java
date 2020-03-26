package io.personium.core.model.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.UriUtils;

public class AclTest {
    private static Logger log = LoggerFactory.getLogger(AclTest.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Configure PersoniumUnitConfig's BaseUrl
        TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
    }

    @Test
    public void setBase_localUnitURL_shouldBeStoredUsing_localUnitScheme() throws Exception {
        Acl acl = new Acl();
        String unitUrl = PersoniumUnitConfig.getBaseUrl();
        String mbUrl = UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:foo:/__/");
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

    /**
     * toJSON_requiredSchemaAuthz_ShouldBe_MappedToKeyWithNamespacePrefix
     * @throws Exception
     */
    @Test
    public void toJSON_requiredSchemaAuthz_ShouldBeMappedTo_KeyWithNamespacePrefix() throws Exception {
        // prepare Acl object
        Acl acl = new Acl();
        String mbUrl = UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:foo:/__/");
        acl.setBase(mbUrl);

        // setRequireSchemaAuthz()
        String requiredSchemaAuthz = "public";
        acl.setRequireSchemaAuthz(requiredSchemaAuthz);

        // call toJSON
        JSONObject j = (JSONObject) new JSONParser().parse(acl.toJSON());
 
        // requireSchemaAuthz should be mapped to the following key.
        String prsa = (String) j.get("@p.requireSchemaAuthz");
        log.info(j.toJSONString());
        log.info("prsa: " + prsa);
        // should be equal to the previously set value.
        assertEquals(requiredSchemaAuthz, prsa);
    }
    
    @Test
    public void fromJSON_requiredSchemaAuthz_ShouldBeMappedTo_KeyEitherWithOrWithoutNamespacePrefix() throws Exception {
        // prepare Json expression with @p.requireSchemaAuthz key (with namespace prefix  p)
        //   new format starting from 1.7.21
        String jsonStrWithPrefix = "{\"@p.requireSchemaAuthz\":\"public\",\"@xml.base\":\"personium-localunit:foo:\\/__\\/\",\"D.ace\":[]}";
        // call  fromJson() 
        Acl acl = Acl.fromJson(jsonStrWithPrefix);
        assertEquals("public", acl.getRequireSchemaAuthz());
 
        // prepare Json expression with @requireSchemaAuthz key (without namespace prefix  p)
        //   old format upto 1.7.20
        String jsonStrWithoutPrefix = "{\"@requireSchemaAuthz\":\"public\",\"@xml.base\":\"personium-localunit:foo:\\/__\\/\",\"D.ace\":[]}";
        Acl acl2 = Acl.fromJson(jsonStrWithoutPrefix);
        assertEquals("public", acl2.getRequireSchemaAuthz());
    }
    
    /**
     * test toJSON method and check that requiredSchemaAuthz should be mapped to key with a name space prefix.
     * @throws Exception
     */
    @Test
    public void toJSON_ace_ShouldBe_MappedTo_properKeys() throws Exception {
        // prepare Acl object
        Acl acl = new Acl();
        String mbUrl = UriUtils.convertSchemeFromLocalUnitToHttp("personium-localunit:foo:/__/");
        acl.setBase(mbUrl);

        // setRequireSchemaAuthz()
        String requiredSchemaAuthz = "public";
        acl.setRequireSchemaAuthz(requiredSchemaAuthz);
        
        Ace ace0 = new Ace();
        ace0.setPrincipalHref("role1");
        ace0.addGrantedPrivilege("read");
        ace0.addGrantedPrivilege("write-properties");
        acl.getAceList().add(ace0);

        Ace ace1 = new Ace();
        ace1.principal = new Principal();
        ace1.principal.all = "";
        ace1.addGrantedPrivilege("root");
        acl.getAceList().add(ace1);

        StringWriter sw = new StringWriter();
        ObjectIo.marshal(acl, sw);
        log.info(sw.toString());
        
        // call toJSON
        JSONObject j = (JSONObject) new JSONParser().parse(acl.toJSON());
        log.info(j.toJSONString());

        // requireSchemaAuthz should be mapped to the following key.
        JSONArray aces = (JSONArray)j.get("D.ace");
        assertEquals(2, aces.size());
        
        // check 1st ace
        JSONObject a0 = (JSONObject)aces.get(0);
        //    principal should be href = role1
        JSONObject a0principal = (JSONObject)a0.get("D.principal");
        assertEquals("role1", (String)a0principal.get("D.href"));
        assertNull(a0principal.get("D.all"));
        
        //    privilege .
        JSONArray a0priv = (JSONArray) ((JSONObject)a0.get("D.grant")).get("D.privilege");
        assertEquals(2, a0priv.size());
        JSONObject a0priv0 = (JSONObject) a0priv.get(0);
        //    D.read should be populated .
        assertNotNull((JSONObject)a0priv0.get("D.read"));
        assertNull((JSONObject)a0priv0.get("D.write"));
        
        JSONObject a0priv1 = (JSONObject) a0priv.get(1);
        //    D.write-properties should be populated .
        assertNotNull((JSONObject)a0priv1.get("D.write-properties"));
        assertNull((JSONObject)a0priv1.get("D.write"));
        
        // check 2nd ace
        JSONObject a1 = (JSONObject)aces.get(1);
        //    principal should be D.all
        JSONObject a1principal = (JSONObject)a1.get("D.principal");
        assertNotNull(a1principal.get("D.all"));
        assertNull(a1principal.get("D.href"));

        //    privilege .
        JSONArray a1priv = (JSONArray) ((JSONObject)a1.get("D.grant")).get("D.privilege");
        assertEquals(1, a1priv.size());
        JSONObject a1priv0 = (JSONObject) a1priv.get(0);
        //    p.root should be populated .
        assertNotNull((JSONObject)a1priv0.get("p.root"));
        assertNull((JSONObject)a1priv0.get("D.root"));
    }

    /**
     * Test ObjectIo#unmarshal() and check if proper object structure will be formed when valid xml is given.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ObjectIo_unmarshal_When_ValidXml_Given_Then_ProperObjectStructure_Formed() throws IOException, JAXBException {
        // prepare ACL XML
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/' "
                + "xmlns:p='urn:x-personium:xmlns' p:requireSchemaAuthz='public'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        // call ObjectIo#unmarshal()
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);
        aclToSet = ObjectIo.unmarshal(reader, Acl.class);

        // .validateAcl() should not throw any exception since the given xml is valid
        aclToSet.validateAcl(true);

        // requiredSchemaAuthz should be properly populated
        assertEquals("public", aclToSet.getRequireSchemaAuthz());

        // size of ace = 1
        assertEquals(1, aclToSet.aces.size());

        // The ace principal should be "all"
        Ace ace = aclToSet.aces.get(0);
        assertEquals("", ace.getPrincipalAll());
        assertNull(ace.getPrincipalHref());
        
        // The ace granted privilege should be just "all"
        assertEquals(1, ace.getGrantedPrivilegeList().size());
        String priv = ace.getGrantedPrivilegeList().get(0);
        assertEquals("all", priv);
    }

    /**
     * Test validateAcl().
     * Error case.
     * requireSchemaAuthz not match.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void validateAcl_ShouldThrowException_WhenGiven_Invalid_RequireSchemaAuthz() throws Exception {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/' "
                + "xmlns:p='urn:x-personium:xmlns' p:requireSchemaAuthz='test'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Reader reader = new StringReader(aclString);
        Acl aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Value [test] for requireSchemaAuthz is invalid");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでaceが空の場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceが空の場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Can not read grant");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでaceの項目にprincipalのみ存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目にprincipalのみ存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Can not read grant");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでaceの項目にgrantのみ存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目にgrantのみ存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Can not read principal");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでaceの項目に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:test>"
                + "<D:acltest/>"
                + "</D:test>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        aclToSet.validateAcl(true);
    }

    /**
     * ACLのバリデートでprincipalの項目が空の場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipalの項目が空の場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Principal is neither href nor all");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでprincipalの項目に必須項目がない場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipalの項目に必須項目がない場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:test>"
                + "</D:test>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Principal is neither href nor all");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでprincipalの項目にhrefだけ存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipalの項目にhrefだけ存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:href>"
                + "role"
                + "</D:href>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        aclToSet.validateAcl(true);
    }

    /**
     * ACLのバリデートでprincipalの項目に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipalの項目に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:test>"
                + "role"
                + "</D:test>"
                + "<D:all/>"
                + "<D:href>"
                + "role"
                + "</D:href>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        aclToSet.validateAcl(true);
    }

    /**
     * ACLのバリデートでprincipal_hrefの項目が空の場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipal_hrefの項目が空の場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:href>"
                + "</D:href>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "href in principal is empty");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでprincipal_hrefの項目内に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprincipal_hrefの項目内に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:href>"
                + "<D:test>"
                + "role"
                + "</D:test>"
                + "</D:href>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "href in principal is empty");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでprivilegeの項目が空の場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprivilegeの項目が空の場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Privilege is empty");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * CellレベルのACLのバリデートでprivilegeの項目に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void CellレベルのACLのバリデートでprivilegeの項目に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:test/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "[test] that can not be set in privilege");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * CellレベルのACLのバリデートでprivilegeの項目に許可するPrivilegeを指定した場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void CellレベルのACLのバリデートでprivilegeの項目に許可するPrivilegeを指定した場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:auth-read/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        aclToSet.validateAcl(true);
    }

    /**
     * Cellレベル以外のACLのバリデートでprivilegeの項目に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void Cellレベル以外のACLのバリデートでprivilegeの項目に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:privilege>"
                + "<D:test/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(false);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "[test] that can not be set in privilege");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでgrantの項目が空の場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでgrantの項目が空の場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        try {
            aclToSet.validateAcl(true);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            PersoniumCoreException expected = PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(
                    "Can not read privilege");
            assertEquals(expected.getCode(), e.getCode());
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    /**
     * ACLのバリデートでgrantの項目に不正なタグが存在する場合.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでgrantの項目に不正なタグが存在する場合() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "<D:principal>"
                + "<D:all/>"
                + "</D:principal>"
                + "<D:grant>"
                + "<D:test>"
                + "</D:test>"
                + "<D:privilege>"
                + "<D:all/>"
                + "</D:privilege>"
                + "</D:grant>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        aclToSet.validateAcl(true);
    }
}

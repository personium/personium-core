package io.personium.core.model.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Unit Test class for ObjectIo class.
 * @author shimono.akio
 */
public class ObjectIoTest {
    private static Logger log = LoggerFactory.getLogger(ObjectIoTest.class);

    static String ACL_XML = "<acl xmlns=\"DAV:\" xmlns:p=\"urn:x-personium:xmlns\" "
            + "xml:base=\"https://cell.unit.example/__role/__/\" "
            + "p:requireSchemaAuthz=\"public\">"
            + "<ace>"
            + "<principal><all/></principal>"
            + "<grant><privilege><read/></privilege></grant>"
            + "</ace>"
            + "<ace>"
            + "<principal><href>role1</href></principal>"
            + "<grant><privilege><read/></privilege></grant>"
            + "</ace>"
            + "</acl>";
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void unmarshal_Acl() throws IOException, JAXBException {
        //log.info(ACL_XML);

        // Correct ACL XML given
        StringReader sr = new StringReader(ACL_XML);

        // Target method call
        Acl acl = ObjectIo.unmarshal(sr, Acl.class);

        // unmarshaled ACL object should be valid
        //  = no exception thrown
        acl.validateAcl(false);

        // Check the contents
        assertEquals("public", acl.requireSchemaAuthz);
        assertEquals(2, acl.getAceList().size());
        Ace ace1 = acl.getAceList().get(0);
        Ace ace2 = acl.getAceList().get(1);
        assertNotNull(ace1.getPrincipalAll());
        assertNull(ace1.getPrincipalHref());
        assertEquals(1, ace1.getGrantedPrivilegeList().size());

        assertNull(ace2.getPrincipalAll());
        assertEquals("role1", ace2.getPrincipalHref());
        assertEquals(1, ace2.getGrantedPrivilegeList().size());
    }

    public Acl prepareAcl() {
        Acl acl = new Acl();
        acl.setRequireSchemaAuthz("public");
        acl.setBase("https://cell.unit.example/__role/__/");
        Ace[] aces = new Ace[] {
                new Ace(),
                new Ace()
        };
        aces[0].principal = new Principal();
        aces[0].principal.all = "all";
        aces[0].addGrantedPrivilege("read");
        aces[1].principal = new Principal();
        aces[1].principal.href = "foo";
        aces[1].addGrantedPrivilege("read");
        aces[1].addGrantedPrivilege("root");
        for (Ace ace : aces) {
            acl.getAceList().add(ace);
        }
        return acl;
    }
    @Test
    public void marshal_Acl() throws Exception {
        Acl acl = this.prepareAcl();
        String jsonStr = acl.toJSON();

        log.info(jsonStr.replaceAll(",", ",\n"));
        //        log.info("-----------");

        Acl acl2 = Acl.fromJson(jsonStr);
        StringWriter sw = new StringWriter();

        // Target method call
        ObjectIo.marshal(acl2, sw);
        String xmlStr = sw.toString();
        log.info(xmlStr.replaceAll("><", ">\n<"));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document result = db.parse(new InputSource(new StringReader(xmlStr)));
        XPath xpath = XPathFactory.newInstance().newXPath();

        // ACL base Url 
        String base = xpath.evaluate("//acl[position()=1]/@base", result);
        log.info("//acl[position()=1]/@base = " + base);
        assertEquals(acl.getBase(), base);
        
        // ACL requireSchemaAuthz
        String requiredSchemaAuthz = xpath.evaluate("//acl[position()=1]/@requireSchemaAuthz", result);
        log.info("//acl[position()=1]/@requireSchemaAuthz = " + requiredSchemaAuthz);
        assertEquals(acl.getRequireSchemaAuthz(), requiredSchemaAuthz);
        
        // Number of ACE's 
        String roleHref = xpath.evaluate("count(//ace)", result);
        log.info("count(//ace) = " + roleHref);
        assertEquals("" +acl.getAceList().size(), roleHref);
    }
}

package io.personium.core.model.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void marshal_Acl() throws IOException, JAXBException {
        Acl acl = this.prepareAcl();
        String jsonStr = acl.toJSON();

        //        log.info(jsonStr.replaceAll(",", ",\n"));
        //        log.info("-----------");

        Acl acl2 = Acl.fromJson(jsonStr);
        StringWriter sw = new StringWriter();

        // Target method call
        ObjectIo.marshal(acl2, sw);
        String xmlStr = sw.toString();

        StringReader sr = new StringReader(xmlStr);

        // unmarshall again
        Acl acl3 = ObjectIo.unmarshal(sr, Acl.class);

        //log.info(xmlStr.replaceAll(">", ">\n"));

        // check the acl contents are kept the same
        assertEquals(acl.getRequireSchemaAuthz(), acl3.getRequireSchemaAuthz());
        assertEquals(acl.getBase(), acl3.getBase());
        assertEquals(acl.getAceList().size(), acl3.getAceList().size());

    }

}

package io.personium.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.StreamingOutput;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.Role;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;

public class TestUtils {
    static Logger log = LoggerFactory.getLogger(TestUtils.class);
    public static final Date DATE_PUBLISHED = DateTime.parse("2014-12-10T00:00:00.000+0900").toDate();
    public static final Date DATE_UPDATED = DateTime.parse("2019-11-09T15:26:00.000+0900").toDate();

    public static Box mockBox(Cell cell, String boxName, String boxSchemaUrl) {
        return new Box(cell, boxName, boxSchemaUrl, UUID.randomUUID().toString() , TestUtils.DATE_PUBLISHED.getTime());
    }
    public static Acl mockAcl(Box box, Map<Role, List<String>> aclSettings) {
        Acl acl =  new Acl();
        acl.setBase(box.getCell().getUrl() + "__role/" + box.getName() + "/");

        for (Role role : aclSettings.keySet()) {
            List<String> privilegeList = aclSettings.get(role);
            Ace ace = new Ace();
            ace.setPrincipalHref(role.getName());
            for (String priv : privilegeList) {
                ace.addGrantedPrivilege(priv);
            }
            acl.getAceList().add(ace);
        }
        return acl;
    }
    public static byte[] responseToBytes(StreamingOutput so) {
        byte [] byteArray = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            so.write(baos);
            byteArray = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArray;
    }


}

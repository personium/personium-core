/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package com.fujitsu.dc.test.unit.core.model.jaxb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.jaxb.Acl;
import com.fujitsu.dc.core.model.jaxb.ObjectIo;
import com.fujitsu.dc.test.categories.Unit;

/**
 * ユニットテストクラス.
 */
@Category({Unit.class })
public class AclTest {

    /**
     * ACLのバリデートで全ての設定が正しく設定されている場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートで全ての設定が正しく設定されている場合にtrueが返却されること() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
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
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        assertTrue(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでaceが空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceが空の場合にfalseが返却されること() throws IOException, JAXBException {
        String aclString = "<D:acl xmlns:D='DAV:' xml:base='https://fqdn/aclTest/__role/__/'>"
                + "<D:ace>"
                + "</D:ace>"
                + "</D:acl>";
        Acl aclToSet = null;
        Reader reader = new StringReader(aclString);

        aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでaceの項目にprincipalのみ存在する場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目にprincipalのみ存在する場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでaceの項目にgrantのみ存在する場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目にgrantのみ存在する場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでaceの項目に不正なタグが存在する場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでaceの項目に不正なタグが存在する場合にtrueが返却されること() throws IOException, JAXBException {
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
        assertTrue(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipalの項目が空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipalの項目が空の場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipalの項目が空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipalの項目に必須項目がない場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipalの項目にhrefだけ存在する場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipalの項目にhrefだけ存在する場合にtrueが返却されること() throws IOException, JAXBException {
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
        assertTrue(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipalの項目に不正なタグが存在する場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipalの項目に不正なタグが存在する場合にtrueが返却されること() throws IOException, JAXBException {
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
        assertTrue(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipal_hrefの項目が空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipal_hrefの項目が空の場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでproncipal_hrefの項目内に不正なタグが存在する場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでproncipal_hrefの項目内に不正なタグが存在する場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでprivilegeの項目が空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでprivilegeの項目が空の場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * CellレベルのACLのバリデートでprivilegeの項目に不正なタグが存在する場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void CellレベルのACLのバリデートでprivilegeの項目に不正なタグが存在する場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * CellレベルのACLのバリデートでprivilegeの項目に許可するPrivilegeを指定した場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void CellレベルのACLのバリデートでprivilegeの項目に許可するPrivilegeを指定した場合にtrueが返却されること() throws IOException, JAXBException {
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
        assertTrue(aclToSet.validateAcl(true));
    }

    /**
     * Cellレベル以外のACLのバリデートでprivilegeの項目に不正なタグが存在する場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void Cellレベル以外のACLのバリデートでprivilegeの項目に不正なタグが存在する場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(false));
    }

    /**
     * ACLのバリデートでgrantの項目が空の場合にfalseが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでgrantの項目が空の場合にfalseが返却されること() throws IOException, JAXBException {
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
        assertFalse(aclToSet.validateAcl(true));
    }

    /**
     * ACLのバリデートでgrantの項目に不正なタグが存在する場合にtrueが返却されること.
     * @throws IOException IOException
     * @throws JAXBException JAXBException
     */
    @Test
    public void ACLのバリデートでgrantの項目に不正なタグが存在する場合にtrueが返却されること() throws IOException, JAXBException {
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
        assertTrue(aclToSet.validateAcl(true));
    }

}

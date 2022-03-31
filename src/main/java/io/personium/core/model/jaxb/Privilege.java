/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.model.jaxb;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import io.personium.common.utils.CommonUtils.XmlConst;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.CellPrivilege;

/**
 * D: JAXB object corresponding to the privilege tag.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "DAV:", name = "privilege")
public final class Privilege {

    /** CellPrivilege map with QName key */
    public static Map<QName, io.personium.core.auth.Privilege> cellPrivileges;

    /** BoxPrivilege map with QName key */
    public static Map<QName, io.personium.core.auth.Privilege> boxPrivileges;

    static {
        // initialize maps
        cellPrivileges = new HashMap<QName, io.personium.core.auth.Privilege>();
        CellPrivilege.getPrivilegeMap().forEach((name, priv) -> {
            cellPrivileges.put(new QName(XmlConst.NS_PERSONIUM, name), priv);
        });

        boxPrivileges = new HashMap<QName, io.personium.core.auth.Privilege>();
        BoxPrivilege.getPrivilegeMap().forEach((name, priv) -> {
            if (name.equals("exec") || name.equals("stream-send") || name.equals("stream-receive")) {
                boxPrivileges.put(new QName(XmlConst.NS_PERSONIUM, name), priv);
            } else {
                boxPrivileges.put(new QName(XmlConst.NS_DAV, name), priv);
            }
        });
    }

    @XmlAnyElement
    Element body;

    @Override
    public String toString() {
        if (body == null) {
            return "";
        }
        return body.getLocalName();
    }
}

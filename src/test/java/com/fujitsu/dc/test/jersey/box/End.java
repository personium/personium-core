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
package com.fujitsu.dc.test.jersey.box;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * End.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class End {
    /**
     * コンストラクタ.
     */
    public End() {
    }

    End(String role, String type, String multiplicity) {
        this.role = role;
        this.type = type;
        this.multiplicity = multiplicity;
    }

    @XmlAttribute(name = "Role")
    String role;

    @XmlAttribute(name = "Type")
    String type;

    @XmlAttribute(name = "Multiplicity")
    String multiplicity;

    @Override
    public boolean equals(Object obj) {
        End end = (End) obj;
        if (!role.equals(end.role)) {
            Edmx.printResult(this, "Role", end.role, role);
            return false;
        }
        if (!type.equals(end.type)) {
            Edmx.printResult(this, "Type", end.type, type);
            return false;
        }
        if (!multiplicity.equals(end.multiplicity)) {
            Edmx.printResult(this, "Multiplicity", end.multiplicity, multiplicity);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "End => Role:" + role + ",Type:" + type + ",Multiplicity:" + multiplicity + "\n";
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

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
 * EndAssociationSet.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class EndAssociationSet {
    /**
     * コンストラクタ.
     */
    public EndAssociationSet() {
    }

    EndAssociationSet(String role, String entitySet) {
        this.role = role;
        this.entitySet = entitySet;
    }

    @XmlAttribute(name = "Role")
    String role;

    @XmlAttribute(name = "EntitySet")
    String entitySet;

    @Override
    public boolean equals(Object obj) {
        EndAssociationSet endAssocSet = (EndAssociationSet) obj;
        if (!role.equals(endAssocSet.role)) {
            Edmx.printResult(this, "Role", endAssocSet.role, role);
            return false;
        }
        if (!entitySet.equals(endAssocSet.entitySet)) {
            Edmx.printResult(this, "EntitySet", endAssocSet.entitySet, entitySet);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EndAssociationSet => Role:").append(role)
                .append(",EntitySet:").append(entitySet).append("\n");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

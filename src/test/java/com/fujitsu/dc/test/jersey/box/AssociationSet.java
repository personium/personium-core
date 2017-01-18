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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * AssociationSet.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class AssociationSet {
    /**
     * コンストラクタ.
     */
    public AssociationSet() {
    }

    AssociationSet(String name, String association, List<EndAssociationSet> endAssociationSets) {
        this.name = name;
        this.association = association;
        this.endAssociationSets = endAssociationSets;
    }

    @XmlElement(name = "End", type = EndAssociationSet.class)
    List<EndAssociationSet> endAssociationSets;

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(name = "Association")
    String association;

    @Override
    public boolean equals(Object obj) {
        AssociationSet assocSet = (AssociationSet) obj;
        if (!name.equals(assocSet.name)) {
            Edmx.printResult(this, "Name", assocSet.name, name);
            return false;
        }
        if (!association.equals(assocSet.association)) {
            Edmx.printResult(this, "Association", assocSet.association, association);
            return false;
        }
        if (endAssociationSets.size() != assocSet.endAssociationSets.size()) {
            Edmx.printResult(this, "End", assocSet.endAssociationSets.toString(), endAssociationSets.toString());
            return false;
        }
        if (!endAssociationSets.equals(assocSet.endAssociationSets)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AssociationSet => Name:").append(name)
                .append(",Association:").append(association).append("\n");

        for (EndAssociationSet endAssociationSet : endAssociationSets) {
            builder.append(endAssociationSet.toString());
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

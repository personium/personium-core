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
 * NavigationProperty.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class NavigationProperty {
    /**
     * コンストラクタ.
     */
    public NavigationProperty() {
    }

    NavigationProperty(String name, String relationship, String fromRole, String toRole) {
        this.name = name;
        this.relationship = relationship;
        this.fromRole = fromRole;
        this.toRole = toRole;
    }

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(name = "Relationship")
    String relationship;

    @XmlAttribute(name = "FromRole")
    String fromRole;

    @XmlAttribute(name = "ToRole")
    String toRole;

    @Override
    public boolean equals(Object obj) {
        NavigationProperty nProperty = (NavigationProperty) obj;
        if (!name.equals(nProperty.name)) {
            Edmx.printResult(this, "Name", nProperty.name, name);
            return false;
        }
        if (!relationship.equals(nProperty.relationship)) {
            Edmx.printResult(this, "Relationship", nProperty.relationship, relationship);
            return false;
        }
        if (!fromRole.equals(nProperty.fromRole)) {
            Edmx.printResult(this, "FromRole", nProperty.fromRole, fromRole);
            return false;
        }
        if (!toRole.equals(nProperty.toRole)) {
            Edmx.printResult(this, "ToRole", nProperty.toRole, toRole);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\nNavigationProperty => Name:").append(name).append(",Relationship:").append(relationship);
        builder.append(",FromRole:").append(fromRole).append(",ToRole:").append(toRole);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

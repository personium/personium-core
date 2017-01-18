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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

/**
 * EntityTypeを扱うオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class EntityType {
    /**
     * コンストラクタ.
     */
    public EntityType() {
    }

    EntityType(String name,
            String opentype,
            Key key,
            List<Property> properties,
            List<NavigationProperty> navProperties) {
        this.name = name;
        if ("true".equals(opentype)) {
            // trueの時だけ設定しておく
            this.opentype = opentype;
        }
        this.key = key;
        this.properties = properties;
        this.navProperties = navProperties;
    }

    EntityType(String name, Key key, List<Property> properties, List<NavigationProperty> navProperties) {
        this.name = name;
        this.key = key;
        this.properties = properties;
        this.navProperties = navProperties;
    }

    EntityType(String name, String opentype, Key key, List<Property> properties) {
        this.name = name;
        if ("true".equals(opentype)) {
            // trueの時だけ設定しておく
            this.opentype = opentype;
        }
        this.key = key;
        this.properties = properties;
    }

    EntityType(String name, Key key, List<Property> properties) {
        this.name = name;
        this.key = key;
        this.properties = properties;
    }

    @XmlElement(name = "Key", type = Key.class)
    Key key;

    @XmlElements({@XmlElement(name = "Property", type = Property.class) })
    List<Property> properties;

    @XmlElements({@XmlElement(name = "NavigationProperty", type = NavigationProperty.class) })
    List<NavigationProperty> navProperties;

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(name = "OpenType")
    String opentype;

    @Override
    public boolean equals(Object obj) {
        EntityType eType = (EntityType) obj;
        if (!name.equals(eType.name)) {
            Edmx.printResult(this, "Name", (eType.name), name);
            return false;
        }

        if ((opentype == null && eType.opentype != null)
                || (opentype != null && !opentype.equals(eType.opentype))) {
            Edmx.printResult(this, "OpenType", eType.opentype, opentype);
            return false;
        }

        if (!key.equals(eType.key)) {
            Edmx.printResult(this, "Key", eType.key.toString(), key.toString());
            return false;
        }

        if (eType.properties.size() != properties.size()) {
            Edmx.printResult(this, "Property", eType.properties.toString(), properties.toString());
            return false;
        }
        if (!properties.equals(eType.properties)) {
            return false;
        }

        if (eType.navProperties == null) {
            if (navProperties != null) {
                Edmx.printResult(this, "NavigationProperty", "null", navProperties.toString());
                return false;
            }
        } else {
            if (navProperties == null) {
                return false;
            }
            if (eType.navProperties.size() != navProperties.size()) {
                Edmx.printResult(this, "NavigationProperty", eType.navProperties.toString(), navProperties.toString());
                return false;
            }
            if (!navProperties.equals(eType.navProperties)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntityType => Name:").append(name).append(",OpenType:").append(opentype).append("\n");
        builder.append(key.toString());

        for (Property property : properties) {
            builder.append(property.toString());
        }
        if (navProperties != null) {
            for (NavigationProperty navProperty : navProperties) {
                builder.append(navProperty.toString());
            }
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

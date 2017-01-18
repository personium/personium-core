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
 * EntitySet.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class EntitySet {
    /**
     * コンストラクタ.
     */
    public EntitySet() {
    }

    EntitySet(String name, String entityType) {
        this.name = name;
        this.entityType = entityType;
    }

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(name = "EntityType")
    String entityType;

    @Override
    public boolean equals(Object obj) {
        EntitySet eSet = (EntitySet) obj;
        if (!name.equals(eSet.name)) {
            return false;
        }
        if (!entityType.equals(eSet.entityType)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EntitySet => Name:").append(name)
                .append(",EntityType:").append(entityType).append("\n");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

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
 * PropertyRef.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class PropertyRef {
    /**
     * コンストラクタ.
     */
    public PropertyRef() {
    }

    PropertyRef(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "Name")
    String name;

    @Override
    public boolean equals(Object obj) {
        if (!name.equals(((PropertyRef) obj).name)) {
            Edmx.printResult(this, "Name", (((PropertyRef) obj).name), name);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PropertyRef => Name:").append(name).append("\n");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

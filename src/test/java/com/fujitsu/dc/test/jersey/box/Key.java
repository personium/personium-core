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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Key.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class Key {
    /**
     * コンストラクタ.
     */
    public Key() {
    }

    Key(List<PropertyRef> propertyRefs) {
        this.propertyRefs = propertyRefs;
    }

    @XmlElement(name = "PropertyRef", type = PropertyRef.class)
    List<PropertyRef> propertyRefs;

    @Override
    public boolean equals(Object obj) {
        Key key = (Key) obj;
        if (propertyRefs.size() != key.propertyRefs.size()) {
            Edmx.printResult(this, "propertyRefs", (key.propertyRefs.toString()), propertyRefs.toString());
        }
        if (!propertyRefs.equals(key.propertyRefs)) {
            Edmx.printResult(propertyRefs);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Key => \n");
        for (PropertyRef propertyRef : propertyRefs) {
            builder.append(propertyRef.toString());
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Association.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class Association {
    /**
     * コンストラクタ.
     */
    public Association() {
    }

    Association(String name, List<End> ends) {
        this.name = name;
        this.ends = ends;
        for (End end : ends) {
            endMap.put(end.role, end);
        }
    }

    @XmlElement(name = "End", type = End.class)
    List<End> ends;

    @XmlAttribute(name = "Name")
    String name;

    private Map<String, End> endMap = new HashMap<String, End>();

    @Override
    public boolean equals(Object obj) {
        Association assoc = (Association) obj;
        if (!name.equals(assoc.name)) {
            Edmx.printResult(this, "Name", assoc.name, name);
            return false;
        }

        if (ends.size() != assoc.ends.size()) {
            Edmx.printResult(this, "End", assoc.ends.toString(), ends.toString());
            return false;
        }
        for (End end : assoc.ends) {
            if (!end.equals(endMap.get(end.role))) {
                Edmx.printResult(this, "End", end.toString(), endMap.get(end.role).toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Association => Name:").append(name).append("\n");
        for (End end : ends) {
            builder.append(end.toString());
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

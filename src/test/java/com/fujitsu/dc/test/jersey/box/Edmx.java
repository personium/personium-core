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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Edmx.
 */
@XmlRootElement(name = "Edmx")
@XmlAccessorType(XmlAccessType.FIELD)
public class Edmx {
    /**
     * コンストラクタ.
     */
    public Edmx() {
    }

    Edmx(String version, DataSerevices dataServices) {
        this.version = version;
        this.dataServices = dataServices;
    }

    @XmlElement(namespace = "http://schemas.microsoft.com/ado/2007/06/edmx",
            name = "DataServices",
            type = DataSerevices.class)
    DataSerevices dataServices;

    @XmlAttribute(name = "Version")
    String version;

    @Override
    public boolean equals(Object obj) {
        Edmx edmx = (Edmx) obj;
        if (!version.equals(edmx.version)) {
            Edmx.printResult(this, "Version", edmx.version, version);
            return false;
        }
        if (!dataServices.equals(edmx.dataServices)) {
            return false;
        }
        // printResult(this, "%%%%%%%%%%", this.toString(), edmx.toString());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Edmx => Version:").append(version).append("\n");
        builder.append(dataServices.toString());
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * 結果表示.
     * @param obj obj
     */
    public static final void printResult(Object obj) {
        System.out.println("********* Invalid " + obj.getClass().getName());
    }

    /**
     * 結果表示.
     * @param obj obj
     * @param target target
     * @param result result
     * @param expected expected
     */
    public static final void printResult(Object obj, String target, String result, String expected) {
        System.out.println(obj.getClass().getName()
                + "\n##### INVALID " + target + "\n!! result => " + result + "\n== expected => " + expected);
    }
}

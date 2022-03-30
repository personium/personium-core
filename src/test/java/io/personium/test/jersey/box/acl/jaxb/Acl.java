/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.jersey.box.acl.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}ace" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "ace"
})
@XmlRootElement(name = "acl")
public class Acl {

    @XmlAttribute(name = "xml:base")
    String xmlbase;

    /**
     * get xml:base attribute value.
     * @return xml:baseattribute value
     */
    public String getXmlbase() {
        return xmlbase;
    }

    /**
     * set xml:base attribute value.
     * @param xmlbase xml:baseattribute value
     */
    public void setXmlbase(String xmlbase) {
        this.xmlbase = xmlbase;
    }

    List<Ace> ace;

    /**
     * Gets the value of the ace property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ace property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAce().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Ace }
     *
     * @return JAXB Object
     *
     */
    public List<Ace> getAce() {
        if (ace == null) {
            ace = new ArrayList<Ace>();
        }
        return this.ace;
    }

}

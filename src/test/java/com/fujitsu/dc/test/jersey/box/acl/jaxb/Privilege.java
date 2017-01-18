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
package com.fujitsu.dc.test.jersey.box.acl.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice minOccurs="0">
 *         &lt;element name="read" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="write" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="alter-schema" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="read-properties" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="write-properties" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="read-acl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="write-acl" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="bind" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="unbind" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="exec" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "read",
        "write",
        "alterSchema",
        "readProperties",
        "writeProperties",
        "readAcl",
        "writeAcl",
        "bind",
        "unbind",
        "exec",
        "all"
})
@XmlRootElement(name = "privilege")
public class Privilege {

    String read;
    String write;
    @XmlElement(name = "alter-schema")
    String alterSchema;
    @XmlElement(name = "read-properties")
    String readProperties;
    @XmlElement(name = "write-properties")
    String writeProperties;
    @XmlElement(name = "read-acl")
    String readAcl;
    @XmlElement(name = "write-acl")
    String writeAcl;
    String bind;
    String unbind;
    String exec;
    String all;

    /**
     * Gets the value of the read property.
     * @return
     *         possible object is {@link String }
     */
    public String getRead() {
        return read;
    }

    /**
     * Sets the value of the read property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setRead(String value) {
        this.read = value;
    }

    /**
     * Gets the value of the write property.
     * @return
     *         possible object is {@link String }
     */
    public String getWrite() {
        return write;
    }

    /**
     * Sets the value of the write property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setWrite(String value) {
        this.write = value;
    }

    /**
     * Gets the value of the alterSchema property.
     * @return
     *         possible object is {@link String }
     */
    public String getAlterSchema() {
        return alterSchema;
    }

    /**
     * Sets the value of the alterSchema property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setAlterSchema(String value) {
        this.alterSchema = value;
    }

    /**
     * Gets the value of the readProperties property.
     * @return
     *         possible object is {@link String }
     */
    public String getReadProperties() {
        return readProperties;
    }

    /**
     * Sets the value of the readProperties property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setReadProperties(String value) {
        this.readProperties = value;
    }

    /**
     * Gets the value of the writeProperties property.
     * @return
     *         possible object is {@link String }
     */
    public String getWriteProperties() {
        return writeProperties;
    }

    /**
     * Sets the value of the writeProperties property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setWriteProperties(String value) {
        this.writeProperties = value;
    }

    /**
     * Gets the value of the readAcl property.
     * @return
     *         possible object is {@link String }
     */
    public String getReadAcl() {
        return readAcl;
    }

    /**
     * Sets the value of the readAcl property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setReadAcl(String value) {
        this.readAcl = value;
    }

    /**
     * Gets the value of the writeAcl property.
     * @return
     *         possible object is {@link String }
     */
    public String getWriteAcl() {
        return writeAcl;
    }

    /**
     * Sets the value of the writeAcl property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setWriteAcl(String value) {
        this.writeAcl = value;
    }

    /**
     * Gets the value of the bind property.
     * @return
     *         possible object is {@link String }
     */
    public String getBind() {
        return bind;
    }

    /**
     * Sets the value of the bind property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setBind(String value) {
        this.bind = value;
    }

    /**
     * Gets the value of the unbind property.
     * @return
     *         possible object is {@link String }
     */
    public String getUnbind() {
        return unbind;
    }

    /**
     * Sets the value of the unbind property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setUnbind(String value) {
        this.unbind = value;
    }

    /**
     * Gets the value of the exec property.
     * @return
     *         possible object is {@link String }
     */
    public String getExec() {
        return exec;
    }

    /**
     * Sets the value of the exec property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setExec(String value) {
        this.exec = value;
    }

    /**
     * Gets the value of the all property.
     * @return
     *         possible object is {@link String }
     */
    public String getAll() {
        return all;
    }

    /**
     * Sets the value of the all property.
     * @param value
     *        allowed object is {@link String }
     */
    public void setAll(String value) {
        this.all = value;
    }
}

/**
 * Personium
 * Modifications copyright 2018 Personium Project Authors
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
 * --------------------------------------------------
 * This code is based on Getcontenttype.java of wink-webdav, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *******************************************************************************/
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.1-b02-fcs
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2008.12.04 at 02:20:17 PM IST
//

package org.apache.wink.webdav.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The <code>getcontenttype</code> Property per the WebDAV specification [RFC
 * 4918]
 *
 * <pre>
 *    Name:       getcontenttype
 *    Namespace:  DAV:
 *    Purpose:    Contains the Content-Type header returned by a GET
 *    without accept headers.
 *    Description: This getcontenttype property MUST be defined on any DAV
 *    compliant resource that returns the Content-Type header in response
 *    to a GET.
 *    Value:      media-type   ; defined in section 3.7 of [RFC2068]
 *
 *    &lt;!ELEMENT getcontenttype (#PCDATA) &gt;
 *
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"content"})
@XmlRootElement(namespace = "DAV:", name = "getcontenttype")
public class Getcontenttype {

    @XmlMixed
    protected List<String> content;

    public Getcontenttype() {
    }

    public Getcontenttype(String value) {
        setValue(value);
    }

    private List<String> getContent() {
        if (content == null) {
            content = new ArrayList<String>();
        }
        return this.content;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        List<String> content = getContent();
        content.clear();
        if (value != null) {
            content.add(value);
        }
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        List<String> content = getContent();
        if (content.size() == 0) {
            return null;
        }
        return content.get(0);
    }

}

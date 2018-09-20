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
/**
 * package to store JAXB objects.
 */
// Package level annotation
//Set prefix mapping for the following namespace with this annotation
@javax.xml.bind.annotation.XmlSchema (
        xmlns = {
                //When "DAV:" is specified without prefix, JAXB adds a mechanical prefix like "ns1" when marshall is executed, so a blank character is specified
                @javax.xml.bind.annotation.XmlNs(prefix = "", namespaceURI = "DAV:"),
                //"urn: x-personium: xmlns" is fixed with "p"
                @javax.xml.bind.annotation.XmlNs(prefix = "p", namespaceURI = "urn:x-personium:xmlns")
        }
)
package io.personium.core.model.jaxb;


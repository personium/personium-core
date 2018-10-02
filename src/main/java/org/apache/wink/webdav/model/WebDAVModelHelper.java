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

package org.apache.wink.webdav.model;

import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.wink.common.RestException;
import org.apache.wink.common.internal.i18n.Messages;
import org.apache.wink.webdav.WebDAVConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WebDAVModelHelper {

    private static final JAXBContext            context;
    private static final DocumentBuilderFactory documentBuilderFactory;
    private static final Document               document;
    private static final String                 XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; //$NON-NLS-1$

    static {
        try {
            context = JAXBContext.newInstance(Propertyupdate.class.getPackage().getName());
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            document = documentBuilderFactory.newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new RestException(Messages.getMessage("webDAVFailSetupPropertyHelper"), e); //$NON-NLS-1$
        }
    }

    public static Marshaller createMarshaller() {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new RestException(Messages.getMessage("webDAVFailCreateMarshaller"), e); //$NON-NLS-1$
        }
    }

    public static Unmarshaller createUnmarshaller() {
        try {
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller;
        } catch (JAXBException e) {
            throw new RestException(Messages.getMessage("webDAVFailCreateUnmarshaller"), e); //$NON-NLS-1$
        }
    }

    public static void marshal(Marshaller m, Object element, Writer writer, String elementName) {
        try {
            m.marshal(element, writer);
        } catch (JAXBException e) {
            throw new RestException(Messages
                .getMessage("webDAVUnableToMarshalElement", elementName), e); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(Unmarshaller u,
                                  Reader reader,
                                  Class<T> elementClass,
                                  String elementName) {
        try {
            Object object = u.unmarshal(reader);
            if (object instanceof JAXBElement) {
                object = ((JAXBElement<?>)object).getValue();
            }
            if (!(elementClass.equals(object.getClass()))) {
                throw new RestException(Messages
                    .getMessage("webDAVIncompatibleTypeInRequest", elementName, object.getClass() //$NON-NLS-1$
                        .getName(), elementClass.getName()));
            }
            return (T)object;
        } catch (JAXBException e) {
            throw new RestException(Messages.getMessage("webDAVUnableToParseElement", elementName), //$NON-NLS-1$
                                    e);
        }
    }

    public static Set<QName> extractPropertyNames(Prop prop, Set<QName> set) {
        if (prop != null) {
            if (prop.getCreationdate() != null) {
                set.add(WebDAVConstants.PROPERTY_CREATIONDATE);
            }
            if (prop.getDisplayname() != null) {
                set.add(WebDAVConstants.PROPERTY_DISPLAYNAME);
            }
            if (prop.getGetcontentlanguage() != null) {
                set.add(WebDAVConstants.PROPERTY_GETCONTENTLANGUAGE);
            }
            if (prop.getGetcontentlength() != null) {
                set.add(WebDAVConstants.PROPERTY_GETCONTENTLENGTH);
            }
            if (prop.getGetcontenttype() != null) {
                set.add(WebDAVConstants.PROPERTY_GETCONTENTTYPE);
            }
            if (prop.getGetetag() != null) {
                set.add(WebDAVConstants.PROPERTY_GETETAG);
            }
            if (prop.getGetlastmodified() != null) {
                set.add(WebDAVConstants.PROPERTY_GETLASTMODIFIED);
            }
            if (prop.getLockdiscovery() != null) {
                set.add(WebDAVConstants.PROPERTY_LOCKDISCOVERY);
            }
            if (prop.getResourcetype() != null) {
                set.add(WebDAVConstants.PROPERTY_RESOURCETYPE);
            }
            if (prop.getSupportedlock() != null) {
                set.add(WebDAVConstants.PROPERTY_SUPPORTEDLOCK);
            }
            for (Element element : prop.getAny()) {
                set.add(new QName(element.getNamespaceURI(), element.getLocalName()));
            }
        }
        return set;
    }

    public static Element createElement(QName qname) {
        return createElement(qname, (String)null);
    }

    public static Element createElement(QName qname, String content) {
        String name = getFullName(qname);
        return createElement(qname.getNamespaceURI(), name, content);
    }

    public static Element createElement(QName qname, Element content) {
        String name = getFullName(qname);
        return createElement(qname.getNamespaceURI(), name, content);
    }

    public static String getFullName(QName qname) {
        String name = qname.getLocalPart();
        String prefix = qname.getPrefix();
        if (prefix != null && prefix.length() > 0) {
            name = qname.getPrefix() + ":" + name; //$NON-NLS-1$
        }
        return name;
    }

    public static Element createElement(String namespaceURI, String name) {
        return createElement(namespaceURI, name, (String)null);
    }

    public static Element createElement(String namespaceURI, String name, String content) {
        Element element = document.createElementNS(namespaceURI, name);
        if (content != null) {
            element.setTextContent(content);
        }
        return element;
    }

    public static Element createElement(String namespaceURI, String qualifiedName, Element child) {
        Element element = document.createElementNS(namespaceURI, qualifiedName);
        if (child != null) {
            element.appendChild(child);
        }
        return element;
    }

    /* package */static JAXBElement<Prop> createProp(Prop prop, QName name) {
        return new JAXBElement<Prop>(name, Prop.class, prop);
    }

    public static String convertDateToXMLDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(XML_DATE_FORMAT);
        dateFormat.setLenient(false);
        return dateFormat.format(date);
    }

    public static Date convertXMLDateToDate(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(XML_DATE_FORMAT);
        dateFormat.setLenient(false);
        return dateFormat.parse(date);
    }

}

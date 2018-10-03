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

package org.apache.wink.webdav;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * WebDAV properties.
 */
public final class WebDAVConstants {

    private WebDAVConstants() {
    } // no instances

    public static final String     NAMESPACE_DAV               = "DAV:"; //$NON-NLS-1$

    public static final QName      PROPERTY_CREATIONDATE       =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "creationdate"); //$NON-NLS-1$
    public static final QName      PROPERTY_DISPLAYNAME        =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "displayname"); //$NON-NLS-1$
    public static final QName      PROPERTY_GETCONTENTLANGUAGE =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "getcontentlanguage"); //$NON-NLS-1$
    public static final QName      PROPERTY_GETCONTENTLENGTH   =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "getcontentlength"); //$NON-NLS-1$
    public static final QName      PROPERTY_GETCONTENTTYPE     =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "getcontenttype"); //$NON-NLS-1$
    public static final QName      PROPERTY_GETETAG            =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "getetag"); //$NON-NLS-1$
    public static final QName      PROPERTY_GETLASTMODIFIED    =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "getlastmodified"); //$NON-NLS-1$
    public static final QName      PROPERTY_LOCKDISCOVERY      =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "lockdiscovery"); //$NON-NLS-1$
    public static final QName      PROPERTY_RESOURCETYPE       =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "resourcetype"); //$NON-NLS-1$
    public static final QName      PROPERTY_SUPPORTEDLOCK      =
                                                                   new QName(NAMESPACE_DAV,
                                                                             "supportedlock"); //$NON-NLS-1$

    public static final Set<QName> PROPERTIES_SET              = new HashSet<QName>();

    static {
        PROPERTIES_SET.add(PROPERTY_CREATIONDATE);
        PROPERTIES_SET.add(PROPERTY_DISPLAYNAME);
        PROPERTIES_SET.add(PROPERTY_GETCONTENTLANGUAGE);
        PROPERTIES_SET.add(PROPERTY_GETCONTENTLENGTH);
        PROPERTIES_SET.add(PROPERTY_GETCONTENTTYPE);
        PROPERTIES_SET.add(PROPERTY_GETETAG);
        PROPERTIES_SET.add(PROPERTY_GETLASTMODIFIED);
        PROPERTIES_SET.add(PROPERTY_LOCKDISCOVERY);
        PROPERTIES_SET.add(PROPERTY_RESOURCETYPE);
        PROPERTIES_SET.add(PROPERTY_SUPPORTEDLOCK);
    }

}

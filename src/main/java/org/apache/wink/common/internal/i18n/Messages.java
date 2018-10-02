/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wink.common.internal.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;

public class Messages {

    private static final String        resourceName = "resource"; //$NON-NLS-1$
    private static final MessageBundle messageBundle;

    static {
        messageBundle =
            new MessageBundle(Messages.class.getPackage().getName(), resourceName, Locale
                .getDefault(), Messages.class.getClassLoader());
    }

    /**
     * Get a message from resource.properties from the package of the given
     * object.
     * 
     * @param key The resource key
     * @return The formatted message
     */
    public static String getMessage(String key) throws MissingResourceException {
        return messageBundle.getMessage(key);
    }

    /**
     * 
     * Get a message from resource.properties from the package of the given
     * object.
     * 
     * @param key The resource key
     * @param args Any parameter arguments for the message
     * @return The formatted message
     */
    public static String getMessage(String key, Object... args) {
        String msg = getMessage(key);
        return MessageFormat.format(msg, args);
    }

}

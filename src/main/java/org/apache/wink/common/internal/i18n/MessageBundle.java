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

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Loads and holds a ResourceBundle according to the specified parameters.
 */
public class MessageBundle {

    private final ResourceBundle resourceBundle;
    private final String         className;

    public MessageBundle(String packageName,
                         String resourceName,
                         Locale locale,
                         ClassLoader classLoader) throws MissingResourceException {
        className = packageName + '.' + resourceName;
        resourceBundle = ResourceBundle.getBundle(className, locale, classLoader);
    }

    /**
     * Gets a string message from the resource bundle for the given key
     * 
     * @param key The resource key
     * @return The message, or key in the case of MissingResourceException
     */
    public String getMessage(String key) {
        try {
            String msg = resourceBundle.getString(key);

            if (msg == null) {
                throw new MissingResourceException("Cannot find resource key \"" + key //$NON-NLS-1$
                        + "\" in base name " //$NON-NLS-1$
                        + className, className, key);
            }
            return msg;
        } catch (MissingResourceException e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
        return key;
    }
}

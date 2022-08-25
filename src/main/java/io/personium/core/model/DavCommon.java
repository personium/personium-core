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
package io.personium.core.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Define constants commonly used for Dav related.
 */
public class DavCommon {

    private DavCommon() {
    }

    /** Resource minimum length.*/
    private static final int MIN_RESOURCE_LENGTH = 1;
    /** Maximum resource length.*/
    private static final int MAX_RESOURCE_LENGTH = 256;

    /** Default value of Depth header.*/
    public static final String DEPTH_INFINITY = "infinity";

    /** Overwrite Value for overwriting header overrides.*/
    public static final String OVERWRITE_TRUE = "T";
    /** Overwrite Value when header overwrite is not allowed.*/
    public static final String OVERWRITE_FALSE = "F";

    public static Pattern PATTERN_CTRL_CHARS = Pattern.compile("\\p{C}");
    public static Pattern PATTERN_INNVALID_RESOURCE_CHARS = Pattern.compile("[\\\\/:*?\"<>| ]");
    /**
     * Invalid name check.
     * @param name Name of the resource to be checked
     * @return true: normal, false: invalid
     */
    public static final boolean isValidResourceName(String name) {
        if (name.length() < MIN_RESOURCE_LENGTH
                || name.length() > MAX_RESOURCE_LENGTH) {
            return false;
        }
        // prohibit use of \ / : * ? " < > |
        Matcher m = PATTERN_INNVALID_RESOURCE_CHARS.matcher(name);
        if (m.find()) {
            return false;
        }
        // prohibi use of control chars
        m = PATTERN_CTRL_CHARS.matcher(name);
        if (m.find()) {
            return false;
        }
        return true;
    }

}

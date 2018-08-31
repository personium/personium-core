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
package io.personium.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *Class for escaping control code.
 */
public class EscapeControlCode {

    /**
     * constructor.
     */
    private EscapeControlCode() {
    }

    /**
     *Escape control code.
     *@ param input String to be escaped
     *@return String escaping control code
     */
    public static String escape(String input) {
        String unicodePattern = "[\u0000-\u001F\u007F]";
        Pattern pattern = Pattern.compile(unicodePattern);
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String unicode = convertToUnicode(matcher.group());
            matcher.appendReplacement(sb, "\\" + unicode);
        }
        matcher.appendTail(sb);
        input = sb.toString();
        return input;
    }

    /**
     *And determines whether or not a control code is included.
     *@ param input judgment character string
     *@return true: Contains the control code false: contains no control code
     */
    public static boolean isContainsControlChar(String input) {
        String unicodePattern = "[\u0000-\u001F\u007F]";
        Pattern pattern = Pattern.compile(unicodePattern);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    /**
     *Converts a character string to a Unicode character.
     *@ param convertString String to be converted
     *@return Unicode character
     */
    private static String convertToUnicode(String convertString) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < convertString.length(); i++) {
            sb.append(String.format("\\u%04X", Character.codePointAt(convertString, i)));
        }
        String unicode = sb.toString();
        return unicode;
    }

}

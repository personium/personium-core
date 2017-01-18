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
package com.fujitsu.dc.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 制御コードのエスケープを行うクラス.
 */
public class EscapeControlCode {

    /**
     * constructor.
     */
    private EscapeControlCode() {
    }

    /**
     * 制御コードをエスケープする.
     * @param input エスケープ対象の文字列
     * @return 制御コードをエスケープした文字列
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
     * 制御コードが含まれているかを判定する.
     * @param input 判定する文字列
     * @return true:制御コードが含まれている false:制御コードが含まれていない
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
     * 文字列をユニコード文字に変換する.
     * @param convertString 変換対象の文字列
     * @return ユニコード文字
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

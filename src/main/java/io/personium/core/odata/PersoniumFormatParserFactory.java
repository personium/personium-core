/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * This code is based on FormatParserFactory.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import javax.ws.rs.core.MediaType;

import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatType;
import org.odata4j.format.Settings;

/**
 * ODataのFormatParserFactory.
 */
public class PersoniumFormatParserFactory {

    private PersoniumFormatParserFactory() {
    }

    /**
     * FormatParsers.
     */
    private interface FormatParsers {
        FormatParser<Entry> getEntryFormatParser(Settings settings);
    }

    /**
     * FormatParserを取得する.
     * @param <T> クラス
     * @param targetType パース対象タイプ
     * @param type 変換フォーマット
     * @param settings セッティング
     * @return FormatParser
     */
    @SuppressWarnings("unchecked")
    public static <T> FormatParser<T> getParser(Class<T> targetType,
            FormatType type, Settings settings) {
        FormatParsers formatParsers = new JsonParsers();
        return (FormatParser<T>) formatParsers.getEntryFormatParser(settings);
    }

    /**
     * FormatParserを取得する.
     * @param <T> クラス
     * @param targetType パース対象タイプ
     * @param contentType 変換フォーマット
     * @param settings セッティング
     * @return FormatParser
     */
    public static <T> FormatParser<T> getParser(Class<T> targetType, MediaType contentType, Settings settings) {
        FormatType type = FormatType.JSON;
        return getParser(targetType, type, settings);
    }

    /**
     * JSONパーサー群.
     */
    public static class JsonParsers implements FormatParsers {

        @Override
        public FormatParser<Entry> getEntryFormatParser(Settings settings) {
            return new PersoniumJsonEntryFormatParser(settings);
        }
    }

}

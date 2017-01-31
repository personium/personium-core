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
 * This code is based on JsonEntryFormatParser.java of odata4j-core, and some modifications
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

import java.io.Reader;

import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.Settings;

import io.personium.core.odata.DcJsonStreamReaderFactory.JsonStreamReader;

/**
 * JsonEntryFormatParser.
 */
public class DcJsonEntryFormatParser extends DcJsonFormatParser implements FormatParser<Entry> {

    /**
     * コンストラクタ.
     * @param settings セッティング
     */
    public DcJsonEntryFormatParser(Settings settings) {
        super(settings);
    }

    /**
     * Entryのパース.
     * @param reader バース対象文字
     * @return Entry
     */
    @Override
    public Entry parse(Reader reader) {
        JsonStreamReader jsr = DcJsonStreamReaderFactory.createJsonStreamReader(reader);
        try {
            ensureNext(jsr);

            // skip the StartObject event
            ensureStartObject(jsr.nextEvent());

            // parse the entry
            return parseEntry(getMetadata().getEdmEntitySet(getEntitySetName()), jsr);

            // no interest in the closing events
        } finally {
            jsr.close();
        }
    }

}

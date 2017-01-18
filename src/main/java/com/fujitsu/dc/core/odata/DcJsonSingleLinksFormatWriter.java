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
 * This code is based on JsonSingleLinksFormatWriter.java of odata4j-core, and some modifications
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
package com.fujitsu.dc.core.odata;

import javax.ws.rs.core.UriInfo;

import org.odata4j.format.SingleLink;
import org.odata4j.format.SingleLinks;
import org.odata4j.format.json.JsonWriter;

/**
 * JsonEntryFormatWriterのラッパークラス.
 */
public class DcJsonSingleLinksFormatWriter extends DcJsonFormatWriter<SingleLinks> {

    /**
     * コンストラクタ.
     * @param jsonpCallback コールバック
     */
    public DcJsonSingleLinksFormatWriter(String jsonpCallback) {
        super(jsonpCallback);
    }

    @Override
    protected void writeContent(UriInfo uriInfo, JsonWriter jw, SingleLinks links) {
        jw.startObject();
        jw.writeName("results");
        jw.startArray();
        boolean isFirst = true;

        for (SingleLink link : links) {
            if (isFirst) {
                isFirst = false;
            } else {
                jw.writeSeparator();
            }
            DcJsonSingleLinkFormatWriter.writeUri(jw, link);
        }
        jw.endArray();
        jw.endObject();
    }
}

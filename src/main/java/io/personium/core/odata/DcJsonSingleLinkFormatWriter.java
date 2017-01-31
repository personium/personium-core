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
 * This code is based on JsonSingleLinkFormatWriter.java of odata4j-core, and some modifications
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

import javax.ws.rs.core.UriInfo;

import org.odata4j.format.SingleLink;
import org.odata4j.format.json.JsonWriter;

import io.personium.core.rs.odata.AbstractODataResource;

/**
 * JsonEntryFormatWriterのラッパークラス.
 */
public class DcJsonSingleLinkFormatWriter extends DcJsonFormatWriter<SingleLink> {

    /**
     * コンストラクタ.
     * @param jsonpCallback コールバック
     */
    public DcJsonSingleLinkFormatWriter(String jsonpCallback) {
        super(jsonpCallback);
    }

    @Override
    protected void writeContent(UriInfo uriInfo, JsonWriter jw, SingleLink link) {
        writeUri(jw, link);
    }

    static void writeUri(JsonWriter jw, SingleLink link) {
        jw.startObject();
        jw.writeName("uri");
        jw.writeString(getUri(link));
        jw.endObject();
    }

    private static String getUri(SingleLink link) {
        return AbstractODataResource.replaceDummyKeyToNull(link.getUri());
    }
}

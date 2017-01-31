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
 * This code is based on JsonFeedFormatWriter.java of odata4j-core, and some modifications
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

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.OEntity;
import org.odata4j.format.json.JsonWriter;
import org.odata4j.producer.EntitiesResponse;

/**
 * JsonFeedFormatWriterのラッパークラス.
 */
public class DcJsonFeedFormatWriter extends DcJsonFormatWriter<EntitiesResponse> {

    /**
     * コンストラクタ.
     * @param jsonpCallback コールバック
     */
    public DcJsonFeedFormatWriter(String jsonpCallback) {
        super(jsonpCallback);
    }

    @Override
    public void writeContent(UriInfo uriInfo, JsonWriter jw, EntitiesResponse target) {

        jw.startObject();
        jw.writeName("results");

        jw.startArray();
        boolean isFirst = true;
        for (OEntity oe : target.getEntities()) {

            if (isFirst) {
                isFirst = false;
            } else {
                jw.writeSeparator();
            }

            writeOEntity(uriInfo, jw, oe, target.getEntitySet(), true);
        }

        jw.endArray();

        if (target.getInlineCount() != null) {
            jw.writeSeparator();
            jw.writeName("__count");
            jw.writeString(target.getInlineCount().toString());
        }

        if (target.getSkipToken() != null) {

            // $skip only applies to the first page of results.
            // if $top was given, we have to reduce it by the number of entities
            // we are returning now.
            String tops = uriInfo.getQueryParameters().getFirst("$top");
            int top = -1;
            if (null != tops) {
                // query param value already validated
                top = Integer.parseInt(tops);
                top -= target.getEntities().size();
            }
            UriBuilder uri = uriInfo.getRequestUriBuilder();
            if (top > 0) {
                uri.replaceQueryParam("$top", top);
            } else {
                uri.replaceQueryParam("$top");
            }
            String nextHref = uri
                    .replaceQueryParam("$skiptoken", target.getSkipToken())
                    .replaceQueryParam("$skip").build().toString();

            jw.writeSeparator();
            jw.writeName("__next");
            jw.writeString(nextHref);
        }
        jw.endObject();
    }
}

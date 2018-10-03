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
 * This code is based on JsonFeedFormatParser.java of odata4j-core, and some modifications
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

import java.util.List;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.format.Entry;
import org.odata4j.format.Settings;

/**
 * PersoniumJsonFeedFormatParser.
 */
public class PersoniumJsonFeedFormatParser extends PersoniumJsonFormatParser {

    /**
     * JsonEntry.
     */
    static class JsonEntry implements Entry {

        private EdmEntityType entityType;

        List<OProperty<?>> properties;
        List<OLink> links;
        OEntity oentity;

        JsonEntry(EdmEntitySet eset) {
            this.entityType = eset.getType();
        }

        public EdmEntityType getEntityType() {
            return this.entityType;
        }

        @Override
        public String getUri() {
            return null;
        }

        @Override
        public OEntity getEntity() {
            return oentity;
        }

        public OEntityKey getEntityKey() {
            return null;
        }

        @Override
        public String getETag() {
            return null;
        }
    }

    /**
     * constructor.
     * @param settings setting information
     */
    public PersoniumJsonFeedFormatParser(Settings settings) {
        super(settings);
    }

}

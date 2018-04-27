/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
package io.personium.core.bar.jackson;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.OEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson correspondence class for handling links.json.
 */
public class JSONLinks implements IJSONMappedObjects {

    /** JsonProperty:Links. */
    @JsonProperty("Links")
    private List<JSONLink> links;

    /**
     * Constructor.
     */
    public JSONLinks() {
        links = new ArrayList<JSONLink>();
    }

    /**
     * Get value of Links.
     * @return value of Links
     */
    public List<JSONLink> getLinks() {
        return links;
    }

    /**
     * Set value of Links.
     * @param links links
     */
    public void setLinks(List<JSONLink> links) {
        this.links = links;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getObjectsSize() {
        return links.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObjects(List<OEntity> entities) {
        // TODO
    }
}

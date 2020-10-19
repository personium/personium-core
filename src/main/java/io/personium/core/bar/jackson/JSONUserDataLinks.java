/**
 * Personium
 * Copyright 2018-2020 Personium Project Authors
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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson correspondence class for handling odatarelations.json.
 */
public class JSONUserDataLinks {

    /** JsonProperty:Links. */
    @JsonProperty("Links")
    private List<JSONUserDataLink> links;

    /**
     * Constructor.
     */
    public JSONUserDataLinks() {
        links = new ArrayList<JSONUserDataLink>();
    }

    /**
     * Get value of Links.
     * @return value of Links
     */
    public List<JSONUserDataLink> getLinks() {
        return links;
    }

    /**
     * Set value of Links.
     * @param links links
     */
    public void setLinks(List<JSONUserDataLink> links) {
        this.links = links;
    }
}

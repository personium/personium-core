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

import org.odata4j.core.OEntity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson correspondence class for handling rules.json.
 */
public class JSONRules implements IJSONMappedObjects {

    /** JsonProperty:Rules. */
    @JsonProperty("Rules")
    private List<JSONRule> rules;

    /**
     * Constructor.
     */
    public JSONRules() {
        rules = new ArrayList<JSONRule>();
    }

    /**
     * Get value of Rules.
     * @return value of Rules
     */
    public List<JSONRule> getRules() {
        return rules;
    }

    /**
     * Set value of Rules.
     * @param rules rules
     */
    public void setRules(List<JSONRule> rules) {
        this.rules = rules;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return rules.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObjects(List<OEntity> entities) {
        for (OEntity entity : entities) {
            JSONRule object = JSONRule.newInstance(entity);
            rules.add(object);
        }
    }
}

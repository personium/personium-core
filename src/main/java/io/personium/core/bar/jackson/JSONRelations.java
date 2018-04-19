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
 * Jackson correspondence class for handling relations.json.
 */
public class JSONRelations implements IJSONMappedObjects {

    /** JsonProperty:Relations. */
    @JsonProperty("Relations")
    private List<JSONRelation> relations;

    /**
     * Constructor.
     */
    public JSONRelations() {
        relations = new ArrayList<JSONRelation>();
    }

    /**
     * Get value of Relations.
     * @return value of Relations
     */
    public List<JSONRelation> getRelations() {
        return relations;
    }

    /**
     * Set value of Relations.
     * @param relations relations
     */
    public void setRelations(List<JSONRelation> relations) {
        this.relations = relations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addObjects(List<OEntity> entities) {
        for (OEntity entity : entities) {
            JSONRelation object = JSONRelation.newInstance(entity);
            relations.add(object);
        }
    }
}

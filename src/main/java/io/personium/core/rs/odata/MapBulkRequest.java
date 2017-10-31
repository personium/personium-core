/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.rs.odata;

import java.util.Map;

import io.personium.common.es.EsBulkRequest;

/**
 * Request to post BulkRequest to Elasticsearch.
 * Specify the contents of request with Map.
 * Currently used only with CellImport.
 */
public class MapBulkRequest implements EsBulkRequest {

    BulkRequestType requestType;
    String type;
    String id;
    Map<String, Object> source;

    /**
     * Constructor.
     */
    public MapBulkRequest() {
    }

    /**
     * Constructor.
     * @param requestType bulk request type
     * @param type es type
     * @param id id
     * @param source _source
     */
    public MapBulkRequest(BulkRequestType requestType, String type, String id, Map<String, Object> source) {
        this.requestType = requestType;
        this.type = type;
        this.id = id;
        this.source = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BulkRequestType getRequestType() {
        return requestType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getSource() {
        return source;
    }

}

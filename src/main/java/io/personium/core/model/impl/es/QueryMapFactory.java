/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.model.impl.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * The Factory class that generates a Map for use in searching.
 */
public final class QueryMapFactory {

    /**
     * constructor.
     */
    private QueryMapFactory() {
    }

    /**
     * Register the specified value in "query" and return it.
     * @param value Value to be registered in query
     * @return query
     */
    public static Map<String, Object> query(Map<String, Object> value) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("query", value);
        return query;
    }

    /**
     * term Generate and return an instance of Map storing query information.
     * @param key search key
     * @param value Search keyword
     * @return Map containing input values
     */
    public static Map<String, Object> termQuery(String key, Object value) {
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> term = new HashMap<String, Object>();

        term.put(key, value);
        query.put("term", term);
        return query;
    }

    /**
     * sort Creates and returns an instance of Map storing query information.
     * @param key Sort key
     * @param order Specify ascending / descending order
     * @return Map containing input values
     */
    public static Map<String, Object> sortQuery(String key, String order) {
        Map<String, Object> sortMap = new HashMap<String, Object>();
        Map<String, Object> sortOption = new HashMap<String, Object>();

        sortOption.put("order", order);
        sortOption.put("ignore_unmapped", true);
        sortMap.put(key, sortOption);
        return sortMap;
    }

    /**
     * @param filters filters
     * @return and filter
     */
    public static Map<String, Object> andFilter(List<Map<String, Object>> filters) {
        Map<String, Object> filter = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();
        filter.put("and", and);
        and.put("filters", filters);
        return filter;
    }

    /**
     * bool.must Create and return a Map instance that stores query information.
     * @param queries must Query list specified by query
     * @return Map containing input values
     */
    public static Map<String, Object> mustQuery(List<Map<String, Object>> queries) {
        Map<String, Object> bool = new HashMap<String, Object>();
        Map<String, Object> must = new HashMap<String, Object>();
        must.put("must", queries);
        bool.put("bool", must);
        return bool;
    }

    /**
     * bool.should Generate a Map instance storing query information and return it.
     * @param queries should Query list specified by query
     * @return Map containing input values
     */
    public static Map<String, Object> shouldQuery(List<Map<String, Object>> queries) {
        Map<String, Object> bool = new HashMap<String, Object>();
        Map<String, Object> should = new HashMap<String, Object>();
        should.put("should", queries);
        bool.put("bool", should);
        return bool;
    }

    /**
     * missing Create and return an instance of Map that stores query information.
     * @param key search key
     * @return Map containing input values
     */
    public static Map<String, Object> missingFilter(String key) {
        Map<String, Object> field = new HashMap<String, Object>();
        Map<String, Object> missing = new HashMap<String, Object>();
        field.put("field", key);
        missing.put("missing", field);
        return missing;
    }

    /**
     * Generate and return an instance of Map storing term filter information.
     * @param key search key
     * @param value Search keyword
     * @param isCache Whether to cache
     * @return Map containing input values
     */
    public static Map<String, Object> termFilter(String key, String value, boolean isCache) {
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> term = new HashMap<String, Object>();

        term.put(key, value);
        term.put("_cache", isCache);
        query.put("term", term);
        return query;
    }

    /**
     * Creates and returns an instance of Map that stores filtered query information.
     * @param query filtered-query If omitted omit match_all
     * @param filter filtered-filter
     * @return Map containing input values
     */
    public static Map<String, Object> filteredQuery(Map<String, Object> query, Map<String, Object> filter) {
        Map<String, Object> filtered = new HashMap<String, Object>();

        if (query == null) {
            Map<String, Object> matchAll = new HashMap<String, Object>();
            matchAll.put("match_all", new HashMap<String, Object>());
            filtered.put("query", matchAll);
        } else {
            filtered.put("query", query);
        }
        filtered.put("filter", filter);

        Map<String, Object> filteredQuery = new HashMap<String, Object>();
        filteredQuery.put("filtered", filtered);

        return filteredQuery;
    }

    /**
     * Create an implicit filter based on Cell / Box / Node / EntityType.
     * @param cellId UUID of Cell
     * @param boxId UUID of Box
     * @param nodeId UUID of DavNode
     * @param entityTypeId UUID of EntityType
     * @param entitySetName Entity set name
     * @return Implicit filter based on Cell / Box / Node / EntityType
     */
    public static List<Map<String, Object>> getImplicitFilters(String cellId, String boxId, String nodeId,
            String entityTypeId, String entitySetName) {
        List<Map<String, Object>> implicitFilters = new ArrayList<Map<String, Object>>();
        if (cellId != null) {
            Map<String, Object> cellAnd = new HashMap<String, Object>();
            Map<String, Object> cellTerm = new HashMap<String, Object>();
            cellAnd.put("term", cellTerm);
            cellTerm.put(OEntityDocHandler.KEY_CELL_ID, cellId);
            implicitFilters.add(0, cellAnd);
            if (boxId != null) {
                Map<String, Object> boxAnd = new HashMap<String, Object>();
                Map<String, Object> boxTerm = new HashMap<String, Object>();
                boxAnd.put("term", boxTerm);
                boxTerm.put(OEntityDocHandler.KEY_BOX_ID, boxId);
                implicitFilters.add(0, boxAnd);
                if (nodeId != null) {
                    Map<String, Object> nodeAnd = new HashMap<String, Object>();
                    Map<String, Object> nodeTerm = new HashMap<String, Object>();
                    nodeAnd.put("term", nodeTerm);
                    nodeTerm.put(OEntityDocHandler.KEY_NODE_ID, nodeId);
                    implicitFilters.add(0, nodeAnd);
                    if (entitySetName != null && entityTypeId != null) {
                        Map<String, Object> entityTypeAnd = new HashMap<String, Object>();
                        Map<String, Object> entityTypeTerm = new HashMap<String, Object>();
                        entityTypeAnd.put("term", entityTypeTerm);
                        entityTypeTerm.put(OEntityDocHandler.KEY_ENTITY_ID, entityTypeId);
                        implicitFilters.add(0, entityTypeAnd);
                    }
                }
            }
        }
        return implicitFilters;
    }

    /**
     * Generate a list of term queries for multiple types of Elasticsearch.
     * @param searchTargetTypes List of types of Elasticsearch
     * @return List of created term queries
     */
    public static List<Map<String, Object>> multiTypeTerms(String[] searchTargetTypes) {
        List<Map<String, Object>> multiTypeTerms = new ArrayList<Map<String, Object>>();
        for (String type : searchTargetTypes) {
            multiTypeTerms.add(termQuery("_type", type));
        }
        return multiTypeTerms;
    }
}

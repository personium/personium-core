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
package io.personium.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.odata.NavigationTargetKeyProperty;
import io.personium.core.rs.odata.AbstractODataResource;

/**
 * ES implementation of the NavigationTargetKeyProperty class.
 */
public class EsNavigationTargetKeyProperty implements NavigationTargetKeyProperty {

    /** EntityType. */
    private String type;

    /** cellId. */
    private String cellId;

    /** boxId. */
    private String boxId;

    /** nodeId. */
    private String nodeId;

    /** Search field of static field.*/
    private Map<String, String> statics = new HashMap<String, String>();

    /** link field search item.*/
    private Map<String, String> links = new HashMap<String, String>();

    /** Destination NTKP.*/
    private EsNavigationTargetKeyProperty shiftNtkp;

    /** Property information.*/
    private Set<OProperty<?>> properties = new HashSet<OProperty<?>>();

    /**
     * properties setter.
     * @param properties properties
     */
    public void setProperties(Set<OProperty<?>> properties) {
        this.properties = properties;
    }

    /** ODataProducer information.*/
    private EsODataProducer odataProducer;

    /**
     * constructor.
     * @param cellId CellId
     * @param boxId boxId
     * @param nodeId nodeId
     * @param type type
     * @param odataProducer odataProducer
     */
    public EsNavigationTargetKeyProperty(String cellId,
            String boxId,
            String nodeId,
            String type,
            EsODataProducer odataProducer) {
        this.cellId = cellId;
        this.boxId = boxId;
        this.nodeId = nodeId;
        this.type = type;
        this.odataProducer = odataProducer;
    }

    /**
     * Search NavigationTargetKeyProperty.
     * @return HashMap of type and ID of link information
     */
    private Map<String, String> search() {
        Map<String, Object> filter = getSearchQuery();

        //Perform search
        EntitySetAccessor esType = odataProducer.getAccessorForEntitySet(type);
        PersoniumSearchHits sHits = esType.search(filter).hits();

        Map<String, String> link = new HashMap<String, String>();
        if (sHits.getCount() == 0) {
            //I will raise an exception if it does not exist
            String value = null;
            for (Map.Entry<String, String> ent : statics.entrySet()) {
                value = ent.getValue();
            }
            if (AbstractODataResource.isDummy(value)) {
                link.put(type, AbstractODataResource.DUMMY_KEY);
            } else {
                throw new NTKPNotFoundException(value);
            }
        } else {
            //Return search result in the form of {"type": "id information"}
            link.put(type, sHits.getAt(0).getId());
        }
        return link;
    }

    /**
     * Acquire a search query.
     * @return search query
     */
    private Map<String, Object> getSearchQuery() {
        //Assemble search query of Static field
        List<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        if (!statics.isEmpty()) {
            for (Map.Entry<String, String> ent : statics.entrySet()) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + "." + ent.getKey()
                        + ".untouched", ent.getValue()));
            }
        }

        //Assemble the search query of Link field
        if (!links.isEmpty()) {
            for (Map.Entry<String, String> ent : links.entrySet()) {
                String linkKey = OEntityDocHandler.KEY_LINK + "." + ent.getKey();
                if (AbstractODataResource.isDummy(ent.getValue())) {
                    //In the case of a dummy key, construct a null search query of the link
                    terms.add(QueryMapFactory.missingFilter(linkKey));
                } else {
                    terms.add(QueryMapFactory.termQuery(linkKey, ent.getValue()));
                }
            }
        }

        List<Map<String, Object>> implicitConditions = new ArrayList<Map<String, Object>>();
        //Specify NodeID as search condition
        if (nodeId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_NODE_ID, nodeId));
        }
        //Specify BoxId as search condition
        if (boxId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_BOX_ID, boxId));
        }
        //Specify CellID as search condition
        if (cellId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        }

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(implicitConditions));

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("version", true);
        if (!terms.isEmpty()) {
            filter.put("filter", QueryMapFactory.andFilter(terms));
        }
        filter.put("query", query);
        return filter;
    }

    /**
     * Analyze property information.
     */
    private void analyzeProperties() {
        for (OProperty<?> property : properties) {
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(property.getName());
            if (ntkp == null) {
                //Search conditions for static fields
                statics.put(property.getName(), (String) property.getValue());
            } else {
                //Search conditions for links field
                if (shiftNtkp == null) {
                    shiftNtkp = new EsNavigationTargetKeyProperty(cellId, boxId, nodeId, ntkp.get("entityType"),
                            odataProducer);
                }
                //Assemble and add OProperty
                OProperty<?> newProperty = OProperties.string(ntkp.get("propName"), (String) property.getValue());
                shiftNtkp.properties.add(newProperty);
            }
        }
        if (shiftNtkp != null) {
            links = shiftNtkp.recursiveSearch();
        }
    }

    /**
     * Recursively search the transition of NTKP and return HashMap of Type and ID of link information.
     * @return HashMap of type and ID of link information
     */
    private Map<String, String> recursiveSearch() {
        analyzeProperties();
        return search();
    }

    /**
     * From the information of NavigationTargetKeyProperty, acquire Entry of Type and ID of link information.
     * @return Type and ID Entry of link information
     */
    @Override
    public Map.Entry<String, String> getLinkEntry() {
        analyzeProperties();
        if (links.isEmpty()) {
            return null;
        }
        //No link information when dummy key is set
        Map.Entry<String, String> entry = links.entrySet().iterator().next();
        if (AbstractODataResource.isDummy(entry.getValue())) {
            return null;
        }
        return entry;
    }

    /**
     * Analyze NavigationTargetKeyProperty and get a query to search for NTKP.
     * @return NTKP search query
     */
    @Override
    public Map<String, Object> getNtkpSearchQuery() {
        analyzeProperties();
        return getSearchQuery();
    }

    /**
     * NTKPNotFoundException.
     */
    @SuppressWarnings("serial")
    public static class NTKPNotFoundException extends RuntimeException {
        /**
         * constructor.
         * @param msg msg.
         */
        public NTKPNotFoundException(String msg) {
            super(msg);
        }
    }
}

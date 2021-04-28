/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.model.impl.es.doc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.odata4j.producer.QueryInfo;

import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.odata.EsQueryHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;

/**
 * We deal with N: N links with ES. Links are stretched between two types. EsLinkHandler elh = new EsLinkHandler (type 1, type 2); Specify both keys to create a linked document.
 * Designate both keys and create a link document key. Specify the key of Type on one side to obtain the list of the other Type.
 */
public class LinkDocHandler implements EsDocHandler {
    private String id;
    private String cellId;
    private String boxId;
    private String nodeId;
    private String ent1Type;
    private String ent1Key;
    private String ent2Type;
    private String ent2Key;
    private Long published;
    private Long updated;

    private static final int DEFAULT_TOP_VALUE = PersoniumUnitConfig.getTopQueryDefaultSize();

    /**
     * constructor.
     */
    public LinkDocHandler() {
        this.id = null;
        this.cellId = null;
        this.boxId = null;
        this.nodeId = null;
        this.ent1Type = null;
        this.ent1Key = null;
        this.ent2Type = null;
        this.ent2Key = null;
        this.published = null;
        this.updated = null;
    }

    /**
     * constructor.
     * @param srcHandler OEntityDocHandler
     * @param tgtHandler OEntityDocHandler
     */
    public LinkDocHandler(final EntitySetDocHandler srcHandler, final EntitySetDocHandler tgtHandler) {
        this.cellId = srcHandler.getCellId();
        this.boxId = srcHandler.getBoxId();
        this.nodeId = srcHandler.getNodeId();
        String srcType = srcHandler.getType();
        String srcId = srcHandler.getId();
        String tgtType = tgtHandler.getType();
        String tgtId = tgtHandler.getId();

        //Create unique key when saving ES
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put(srcType, srcId);
        tm.put(tgtType, tgtId);

        this.ent1Type = tm.firstKey();
        this.ent2Type = tm.lastKey();
        this.ent1Key = tm.get(ent1Type);
        this.ent2Key = tm.get(ent2Type);
        this.id = this.createLinkId();
        long dateTime = new Date().getTime();
        this.published = dateTime;
        this.updated = dateTime;
    }

    /**
     * Constructor that generates LinkDocHandler from search result.
     * @param searchHit Search result data
     */
    public LinkDocHandler(final PersoniumSearchHit searchHit) {
        this.id = searchHit.getId();

        Map<String, Object> source = searchHit.getSource();
        this.cellId = source.get(KEY_CELL_ID).toString();
        if (source.containsKey(KEY_BOX_ID) && source.get(KEY_BOX_ID) != null) {
            this.boxId = source.get(KEY_BOX_ID).toString();
        }
        if (source.containsKey(KEY_NODE_ID) && source.get(KEY_NODE_ID) != null) {
            this.nodeId = source.get(KEY_NODE_ID).toString();
        }

        //Create unique key when saving ES
        String srcType = source.get(KEY_ENT1_TYPE).toString();
        String srcId = source.get(KEY_ENT1_ID).toString();
        String tgtType = source.get(KEY_ENT2_TYPE).toString();
        String tgtId = source.get(KEY_ENT2_ID).toString();
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put(srcType, srcId);
        tm.put(tgtType, tgtId);

        this.ent1Type = tm.firstKey();
        this.ent2Type = tm.lastKey();
        this.ent1Key = tm.get(ent1Type);
        this.ent2Key = tm.get(ent2Type);
        this.published = Long.parseLong(source.get(KEY_PUBLISHED).toString());
        this.updated = Long.parseLong(source.get(KEY_UPDATED).toString());
    }

    /**
     * @return the ent1Type
     */
    public String getEnt1Type() {
        return ent1Type;
    }

    /**
     * @param ent1Type the ent1Type to set
     */
    public void setEnt1Type(String ent1Type) {
        this.ent1Type = ent1Type;
    }

    /**
     * @return the ent1Key
     */
    public String getEnt1Key() {
        return ent1Key;
    }

    /**
     * @param ent1Key the ent1Key to set
     */
    public void setEnt1Key(String ent1Key) {
        this.ent1Key = ent1Key;
    }

    /**
     * @return the ent2Type
     */
    public String getEnt2Type() {
        return ent2Type;
    }

    /**
     * @param ent2Type the ent2Type to set
     */
    public void setEnt2Type(String ent2Type) {
        this.ent2Type = ent2Type;
    }

    /**
     * @return the ent2Key
     */
    public String getEnt2Key() {
        return ent2Key;
    }

    /**
     * @param ent2Key the ent2Key to set
     */
    public void setEnt2Key(String ent2Key) {
        this.ent2Key = ent2Key;
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * @return cell id
     */
    public String getCellId() {
        return cellId;
    }

    /**
     * @param cellid cell Id
     */
    public void setCellId(String cellid) {
        this.cellId = cellid;
    }

    /**
     * @return box id
     */
    public String getBoxId() {
        return boxId;
    }

    /**
     * @param boxid box id
     */
    public void setBoxId(String boxid) {
        this.boxId = boxid;
    }

    /**
     * @return node id
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeid node id
     */
    public void setNodeId(String nodeid) {
        this.nodeId = nodeid;
    }

    @Override
    public Long getVersion() {
        return null;
    }

    @Override
    public Map<String, Object> getSource() {
        return this.createLinkDoc();
    }

    /**
     * @return Json
     */
    public Map<String, Object> createLinkDoc() {
        Map<String, Object> ret = new HashMap<String, Object>();
        if (this.cellId != null) {
            ret.put(KEY_CELL_ID, this.cellId);
        }
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_ENT1_TYPE, this.ent1Type);
        ret.put(KEY_ENT1_ID, this.ent1Key);
        ret.put(KEY_ENT2_TYPE, this.ent2Type);
        ret.put(KEY_ENT2_ID, this.ent2Key);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        return ret;
    }

    /**
     * @return ID
     */
    public String createLinkId() {
        return this.ent1Key + "-" + this.ent2Key;
    }

    /**
     * Get the document count of N: N links.
     * @param accessor link accessor
     * @param srcHandler OEntityDocHandler
     * @param targetSetName targetSetName
     * @param targetEntityTypeId targetEntityTypeId
     * @return Document count
     */
    public static long getNtoNCount(
            final ODataLinkAccessor accessor,
            final EntitySetDocHandler srcHandler,
            final String targetSetName,
            final String targetEntityTypeId) {

        NtoNQueryParameter parameter = new NtoNQueryParameter(srcHandler, targetSetName,
                targetEntityTypeId);

        return accessor.count(parameter.getSource(0, 0));
    }

    /**
     * Get a list of N: N links.
     * @param accessor link accessor
     * @param srcHandler OEntityDocHandler
     * @param targetSetName targetSetName
     * @param targetEntityTypeId targetEntityTypeId
     * @param queryInfo queryInfo
     * @return ESQuery
     */
    public static List<String> query(final ODataLinkAccessor accessor,
            final EntitySetDocHandler srcHandler,
            final String targetSetName,
            final String targetEntityTypeId,
            final QueryInfo queryInfo) {

        NtoNQueryParameter parameter = new NtoNQueryParameter(srcHandler, targetSetName,
                targetEntityTypeId);

        //Search list of ID
        Integer size = DEFAULT_TOP_VALUE;
        Integer from = 0;
        if (queryInfo != null) {
            if (queryInfo.top != null) {
                size = queryInfo.top;
            }
            if (queryInfo.skip != null) {
                from = queryInfo.skip;
            }
        }

        List<String> ret = new ArrayList<String>();
        PersoniumSearchResponse sr = accessor.search(parameter.getSource(size, from));
        if (sr == null) {
            return ret;
        }

        for (PersoniumSearchHit hit : sr.getHits().getHits()) {
            Map<String, Object> hs = hit.getSource();
            ret.add((String) hs.get(parameter.getTargetKey()));
        }
        return ret;
    }

    /**
     * @return the published
     */
    public Long getPublished() {
        return published;
    }

    /**
     * @param published the published to set
     */
    public void setPublished(Long published) {
        this.published = published;
    }

    /**
     * @return the updated
     */
    public Long getUpdated() {
        return updated;
    }

    /**
     * @param updated the updated to set
     */
    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * JSON key that stores update date and time in OData Link storage on ES.
     */
    public static final String KEY_UPDATED = "u";
    /**
     * JSON key that stores created date in OData Link storage on ES.
     */
    public static final String KEY_PUBLISHED = "p";
    /**
     * JSON key that stores the internal ID of Cell in OData Link storage on ES.
     */
    public static final String KEY_CELL_ID = "c";
    /**
     * JSON key that stores internal ID of Box in OData Link storage on ES.
     */
    public static final String KEY_BOX_ID = "b";
    /**
     * JSON key that stores the nodeid of the collection in OData Link storage on ES.
     */
    public static final String KEY_NODE_ID = "n";

    /**
     * JSON key that stores the type name of the smaller side in string comparison in OData Link storage on ES.
     */
    public static final String KEY_ENT1_TYPE = "t1";
    /**
     * JSON key that stores the entity ID of the smaller type in string comparison in OData Link storage on ES.
     */
    public static final String KEY_ENT1_ID = "k1";
    /**
     * JSON key that stores the type name of the larger side in string comparison in OData Link storage on ES.
     */
    public static final String KEY_ENT2_TYPE = "t2";
    /**
     * JSON key that stores the entity ID of the type of the larger side in string comparison in OData Link storage on ES.
     */
    public static final String KEY_ENT2_ID = "k2";

    /**
     * Return the ID of the data associated with the specified entity type.
     * @param baseEntityType entity type
     * @return ID Return null if it does not exist
     */
    public String getLinkedEntitytIdFromBaseEntityType(String baseEntityType) {
        if (this.ent1Type.equals(baseEntityType)) {
            return this.ent2Key;
        } else if (this.ent2Type.equals(baseEntityType)) {
            return this.ent1Key;
        } else {
            return null;
        }
    }

    /**
     * Return ID of the specified entity type.
     * @param entityType entity type
     * @return ID Return null if it does not exist
     */
    public String getEntitytIdFromEntityType(String entityType) {
        if (this.ent1Type.equals(entityType)) {
            return this.ent1Key;
        } else if (this.ent2Type.equals(entityType)) {
            return this.ent2Key;
        } else {
            return null;
        }
    }

    /**
     * A class that generates query information for obtaining a list of N: N links.
     */
    public static class NtoNQueryParameter {
        private String t1;
        private String t2;
        private String k1;
        private String k2;

        /**
         * constructor.
         * @param srcHandler OEntityDocHandler
         * @param targetSetName EntitySet name on the target side
         * @param targetEntityTypeId EntityTypeID of target side
         */
        public NtoNQueryParameter(
                final EntitySetDocHandler srcHandler,
                final String targetSetName,
                final String targetEntityTypeId) {
            String srcSetName = srcHandler.getType();
            String srcId = srcHandler.getId();

            TreeMap<String, String> tm = new TreeMap<String, String>();
            if (srcSetName.equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
                tm.put(srcHandler.getEntityTypeId(), srcId);
                tm.put(targetEntityTypeId, "");
            } else {
                tm.put(srcSetName, srcId);
                tm.put(targetSetName, "");
            }
            this.t1 = tm.firstKey();
            this.t2 = tm.lastKey();
            this.k1 = tm.get(t1);
            this.k2 = tm.get(t2);
        }

        /**
         * Generate query information for obtaining a list of N: N links (no sorting).
         * @param size Number of items to be acquired
         * @param from fetch count
         * @return ESQuery
         */
        public Map<String, Object> getSource(Integer size, Integer from) {
            Map<String, Object> source = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();

            if (this.k1.length() == 0) {
                filters.add(QueryMapFactory.termQuery(KEY_ENT2_ID, this.k2));
            } else {
                filters.add(QueryMapFactory.termQuery(KEY_ENT1_ID, this.k1));
            }

            List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
            queries.add(QueryMapFactory.termQuery(KEY_ENT1_TYPE, this.t1));
            queries.add(QueryMapFactory.termQuery(KEY_ENT2_TYPE, this.t2));

            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

            and.put("filters", filters);
            filter.put("and", and);

            source.put("query", query);
            source.put("filter", filter);
            source.put("size", size);
            source.put("from", from);
            return source;
        }

        /**
         * Expand Generates query information for acquiring a list of N: N links at the time of acquiring target data.
         * @param size Number of items to be acquired
         * @return ESQuery
         */
        public Map<String, Object> getSourceForExpand(Integer size) {
            Map<String, Object> source = new HashMap<String, Object>();

            List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
            if (this.k1.length() == 0) {
                queries.add(QueryMapFactory.termQuery(KEY_ENT2_ID, this.k2));
            } else {
                queries.add(QueryMapFactory.termQuery(KEY_ENT1_ID, this.k1));
            }
            queries.add(QueryMapFactory.termQuery(KEY_ENT1_TYPE, this.t1));
            queries.add(QueryMapFactory.termQuery(KEY_ENT2_TYPE, this.t2));

            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

            source.put("query", query);
            source.put("sort", QueryMapFactory.sortQuery(getTargetKey(), EsQueryHandler.SORT_ASC));
            source.put("size", size);
            return source;
        }

        /**
         * Get the key name (k1, k2) of the target side.
         */
        String getTargetKey() {
            if (this.k1.length() == 0) {
                return KEY_ENT1_ID;
            } else {
                return KEY_ENT2_ID;
            }
        }
    }

}

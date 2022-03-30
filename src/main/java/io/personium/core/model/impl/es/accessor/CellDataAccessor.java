/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.core.model.impl.es.accessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * Accessor that accesses Cell entire data without limiting Type.
 */
public class CellDataAccessor extends DataSourceAccessor {

    /** BoxKey of 1:N links. */
    private static final String BOX_LINK_KEY = OEntityDocHandler.KEY_LINK + "." + Box.EDM_TYPE_NAME;
    /** RelationKey of 1:N links. */
    private static final String RELATION_LINK_KEY = OEntityDocHandler.KEY_LINK + "." + Relation.EDM_TYPE_NAME;

    /** CellID. */
    private String cellId;

    /**
     * Constructor.
     * @param index es index
     * @param cellId target cell id.
     */
    public CellDataAccessor(EsIndex index, String cellId) {
        super(index);
        this.cellId = cellId;
    }

    /**
     * Bulk delete of cell.
     * Only the contents are deleted, and the cell itself is not deleted.
     */
    public void bulkDeleteCell() {
        // Specify cell ID and delete all cell related entities in bulk.
        Map<String, Object> filter = new HashMap<String, Object>();
        filter = QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId);
        Map<String, Object> filtered = new HashMap<String, Object>();
        filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        try {
            deleteByQuery(cellId, query);
            log.info("KVS Deletion Success.");
        } catch (EsClientException e) {
            // If the deletion fails, output a log and continue processing.
            log.warn(String.format("Delete CellResource From KVS Failed. CellId:[%s]", cellId), e);
        }
    }

    /**
     * Bulk delete of Box.
     * Only the contents are deleted, and the cell itself is not deleted.
     * @param boxId Target box id
     */
    public void bulkDeleteBox(String boxId) {
        // Specifying filter
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_BOX_ID, boxId));
        Map<String, Object> filter = QueryMapFactory.andFilter(filters);
        Map<String, Object> filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        deleteByQuery(cellId, query);
    }

    /**
     * Delete data linked to Box.
     * Objects that may be deleted: Role, Relation, SentMessage, ReceivedMessage, ExtRole.
     * @param boxId Target box id
     */
    public void deleteBoxLinkData(String boxId) {
        // Delete extRole linked to box.
        deleteExtRoleLinkedToBox(boxId);

        // Since box can set only 1:N Links, it suffices to search l.Box.
        // Specifying filter
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        filters.add(QueryMapFactory.termQuery(BOX_LINK_KEY, boxId));
        Map<String, Object> filter = QueryMapFactory.andFilter(filters);
        Map<String, Object> filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        deleteByQuery(cellId, query);
    }

    /**
     * Bulk delete of ODataServiceCollection.
     * Only the contents are deleted, and the collection itself(DavFile) is not deleted.
     * @param boxId Target box id
     * @param nodeId Target collection id
     */
    public void bulkDeleteODataCollection(String boxId, String nodeId) {
        // Specifying filter
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_BOX_ID, boxId));
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_NODE_ID, nodeId));
        Map<String, Object> filter = QueryMapFactory.andFilter(filters);
        Map<String, Object> filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        deleteByQuery(cellId, query);
    }

    /**
     * Delete extRole linked to box.
     * @param boxId Target box id
     */
    private void deleteExtRoleLinkedToBox(String boxId) {
        // ExtRole needs to search from Relation linked to Box.
        // (Box <- link -> Relation <- link -> ExtRole)
        PersoniumSearchResponse response = searchRelationLinkedToBox(boxId);
        if (response.getHits().getCount() == 0L) {
            return;
        }

        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(QueryMapFactory.termQuery("_type", ExtRole.EDM_TYPE_NAME));
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));

        List<Map<String, Object>> shouldQueries = new ArrayList<>();
        for (PersoniumSearchHit hit : response.getHits().getHits()) {
            shouldQueries.add(QueryMapFactory.termQuery(RELATION_LINK_KEY, hit.getId()));
        }
        filters.add(QueryMapFactory.shouldQuery(shouldQueries));

        Map<String, Object> filter = QueryMapFactory.andFilter(filters);
        Map<String, Object> filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        deleteByQuery(cellId, query);
    }

    /**
     * Search relation linked to box.
     * @param boxId Target box id
     * @return Search results
     */
    private PersoniumSearchResponse searchRelationLinkedToBox(String boxId) {
        // Since box can set only 1:N Links, it suffices to search l.Box.
        // Specifying filter
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(QueryMapFactory.termQuery("_type", Relation.EDM_TYPE_NAME));
        filters.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        filters.add(QueryMapFactory.termQuery(BOX_LINK_KEY, boxId));
        Map<String, Object> filter = QueryMapFactory.andFilter(filters);
        Map<String, Object> filtered = QueryMapFactory.filteredQuery(null, filter);
        // Generate query
        Map<String, Object> query = QueryMapFactory.query(filtered);

        return searchForIndex(cellId, query);
    }
}

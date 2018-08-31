/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * A class that implements Cell access processing.
 */
public class CellAccessor extends AbstractEntitySetAccessor {

    /**
     * constructor.
     * @ param index index
     * @ param name Type name
     * @param routingId routingId
     */
    public CellAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * Return the number of DavFiles under the cell.
     * TODO It will be deleted in response to core issue #71.
     * @ param cellId Cell ID to delete
     * @ param unitUserName unit user name
     * @return Number of DavFiles under cell
     */
    public long getDavFileTotalCount(String cellId, String unitUserName) {
        //CellAccessor obtains unit user's accessor for access to ad index
        DataSourceAccessor accessor = EsModel.dsa(unitUserName);

        //To obtain only Count, specify the size as 0
        Map<String, Object> countQuery = getDavFileFilterQuery(cellId);
        countQuery.put("size", 0);

        PersoniumSearchResponse response = accessor.searchForIndex(cellId, countQuery);
        return response.getHits().getAllPages();
    }

    /**
     * Return the DavFileID list under the cell.
     * TODO It will be deleted in response to core issue #71.
     * @ param cellId Cell ID to delete
     * @ param unitUserName unit user name
     * @ param size Number of acquisitions
     * @ param from acquisition start position
     * @return Number of DavFiles under cell
     */
    public List<String> getDavFileIdList(String cellId, String unitUserName, int size, int from) {
        //CellAccessor obtains unit user's accessor for access to ad index
        DataSourceAccessor accessor = EsModel.dsa(unitUserName);

        Map<String, Object> searchQuery = getDavFileFilterQuery(cellId);
        searchQuery.put("size", size);
        searchQuery.put("from", from);

        PersoniumSearchResponse response = accessor.searchForIndex(cellId, searchQuery);
        List<String> davFileIdList = new ArrayList<String>();
        for (PersoniumSearchHit hit : response.getHits().getHits()) {
            davFileIdList.add(hit.getId());
        }
        return davFileIdList;
    }

    // TODO It will be deleted in response to core issue #71.
    private Map<String, Object> getDavFileFilterQuery(String cellId) {
        Map<String, Object> cellQuery = new HashMap<String, Object>();
        cellQuery.put(OEntityDocHandler.KEY_CELL_ID, cellId);
        Map<String, Object> cellTermQuery = new HashMap<String, Object>();
        cellTermQuery.put("term", cellQuery);

        Map<String, Object> davTypeQuery = new HashMap<String, Object>();
        davTypeQuery.put(OEntityDocHandler.KEY_ENTITY_ID, DavCmp.TYPE_DAV_FILE);
        Map<String, Object> davTypeTermQuery = new HashMap<String, Object>();
        davTypeTermQuery.put("term", davTypeQuery);

        List<Map<String, Object>> andQueryList = new ArrayList<Map<String, Object>>();
        andQueryList.add(cellTermQuery);
        andQueryList.add(davTypeTermQuery);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(andQueryList));

        Map<String, Object> countQuery = QueryMapFactory.query(query);
        return countQuery;
    }
}

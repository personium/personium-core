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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsBulkRequest;
import io.personium.common.es.EsIndex;
import io.personium.common.es.EsType;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.PersoniumActionResponse;
import io.personium.common.es.response.PersoniumBulkResponse;
import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.response.PersoniumMultiSearchResponse;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 *Base class that implements basic processing of the data store layer.
 */
public class DataSourceAccessor {
    private EsIndex index;
    private EsType type;
    private String routingid;

    /** Object for logging.*/
    static Logger log = LoggerFactory.getLogger(DataSourceAccessor.class);

    /**
     *constructor.
     *@ param index index
     */
    public DataSourceAccessor(EsIndex index) {
        this.index = index;
    }

    /**
     *constructor.
     *@ param index index
     *@ param name Type name
     * @param routingId routingID
     */
    protected DataSourceAccessor(EsIndex index, String name, String routingId) {
        this.index = index;
        int times = Integer.valueOf(PersoniumUnitConfig.getESRetryTimes());
        int interval = Integer.valueOf(PersoniumUnitConfig.getESRetryInterval());
        this.type = EsModel.type(index.getName(), name, routingId, times, interval);
        this.routingid = routingId;
    }

    /**
     *Getter of Index.
     * @return EsIndex
     */
    public EsIndex getIndex() {
        return this.index;
    }

    /**
     *Get Type.
     *@return response
     */
    public String getType() {
        return this.type.getType();
    }

    /**
     *Get the routing ID to use when searching for ES.
     *@return routing ID
     */
    protected String getRoutingId() {
        return this.routingid;
    }

    /**
     *Acquisition of one document.
     *@ param id Document ID
     *@return response
     */
    public PersoniumGetResponse get(final String id) {
        try {
            return this.type.get(id);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Create a new document.
     *@ param data document
     *@return ES response
     */
    @SuppressWarnings("rawtypes")
    public PersoniumIndexResponse create(final Map data) {
        String id = PersoniumUUID.randomUUID();
        return this.create(id, data);
    }

    /**
     *Create a new document.
     * @param id ID
     *@ param data document
     *@return ES response
     */
    @SuppressWarnings({"rawtypes" })
    public PersoniumActionResponse createForDavNodeFile(final String id, final Map data) {
        PersoniumIndexResponse res = create(id, data);
        return res;
    }

    /**
     *Create a new document.
     * @param id ID
     *@ param data document
     *@return ES response
     */
    @SuppressWarnings({"rawtypes" })
    public PersoniumIndexResponse create(final String id, final Map data) {
        try {
            return this.type.create(id, data);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw PersoniumCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            PersoniumCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                return this.type.create(id, data);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw PersoniumCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Create a new document (for Cell creation).
     * @param id ID
     *@ param data document
     *@ param docHandler document handler
     *@return ES response
     */
    @SuppressWarnings({"rawtypes" })
    public PersoniumIndexResponse create(final String id, final Map data, final EntitySetDocHandler docHandler) {
        try {
            return this.type.create(id, data);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw PersoniumCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            PersoniumCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                return this.type.create(id, data);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw PersoniumCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Document update.
     * @param id ID
     *@ param data document
     *@ param version version number
     *@return ES response
     */
    @SuppressWarnings({"rawtypes" })
    public PersoniumIndexResponse update(final String id, final Map data, final long version) {
        try {
            return this.type.update(id, data, version);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw PersoniumCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            PersoniumCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                return this.type.update(id, data, version);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw PersoniumCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Document update.
     * @param id ID
     *@ param data document
     *@return ES response
     */
    @SuppressWarnings("rawtypes")
    public PersoniumIndexResponse update(final String id, final Map data) {
        return this.update(id, data, -1);
    }

    /**
     *Get the number of documents.
     *@ param query Query information
     *@return ES response
     */
    public long count(final Map<String, Object> query) {
        Map<String, Object> requestQuery = null;
        if (query != null) {
            requestQuery = new HashMap<String, Object>(query);
        } else {
            requestQuery = new HashMap<String, Object>();
        }
        requestQuery.put("size", 0);
        try {
            PersoniumSearchResponse hit = this.type.search(requestQuery);
            return hit.getHits().getAllPages();
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Search documents.
     *@ param query Query information
     *@return ES response
     */
    public PersoniumSearchResponse search(final Map<String, Object> query) {
        Map<String, Object> requestQuery = null;
        if (query != null) {
            requestQuery = new HashMap<String, Object>(query);
        } else {
            requestQuery = new HashMap<String, Object>();
        }

        if (!requestQuery.containsKey("size")) {
            requestQuery.put("size", this.count(query));
        }
        try {
            return this.type.search(requestQuery);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Multi-search documents.
     *When using this method, call number by specifying the number of acquisitions (size) in the query
     *@ param queryList List of query information
     *@return ES response
     */
    public PersoniumMultiSearchResponse multiSearch(final List<Map<String, Object>> queryList) {
        try {
            return this.type.multiSearch(queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }

    }

    /**
     *Search documents against the index of ES.
     *@ param query Query information
     *@return ES response
     */
    public PersoniumSearchResponse indexSearch(final Map<String, Object> query) {
        Map<String, Object> requestQuery = null;
        if (query == null) {
            requestQuery = new HashMap<String, Object>();
        } else {
            requestQuery = new HashMap<String, Object>(query);
        }

        if (!requestQuery.containsKey("size")) {
            requestQuery.put("size", this.count(query));
        }
        try {
            return this.index.search(null, requestQuery);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * Delete a document.
     * @param docId Document id to delete
     * @param version The version of the document to delete
     *@return response
     */
    public PersoniumDeleteResponse delete(final String docId, final long version) {
        try {
            return this.type.delete(docId, version);
        } catch (EsClientException.EsIndexMissingException e) {
            PersoniumCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            return null;
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Register the data in bulk. <br />
     *Update and delete are not supported yet.
     *@ param esBulkRequest Bulk registration document list for ES
     * @param routingId routingId
     *@return bulk response
     */
    public PersoniumBulkResponse bulkCreate(List<EsBulkRequest> esBulkRequest,
            String routingId) {

        PersoniumBulkResponse response = null;
        try {
            response = this.index.bulkRequest(routingId, esBulkRequest, false);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
        return response;
    }

    /**
     *Register / update data in bulk. <br />
     *Deletion is not supported yet.
     *@ param esBulkRequest Bulk registration document list for ES
     * @param routingId routingId
     *@return bulk response
     */
    public PersoniumBulkResponse bulkUpdate(List<EsBulkRequest> esBulkRequest, String routingId) {

        PersoniumBulkResponse response = null;
        try {
            response = this.index.bulkRequest(routingId, esBulkRequest, false);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
        return response;
    }

    /**
     * Use queries to delete data.
     * @param routingId routingId
     * @param deleteQuery query
     */
    protected void deleteByQuery(String routingId, Map<String, Object> deleteQuery) {
        this.index.deleteByQuery(routingId, deleteQuery);
    }

    /**
     * Count documents.
     * @param routingId routingId
     * @param query query
     * @return Number of documents
     */
    public long countForIndex(String routingId, Map<String, Object> query) {
        Map<String, Object> requestQuery = null;
        if (query != null) {
            requestQuery = new HashMap<String, Object>(query);
        } else {
            requestQuery = new HashMap<String, Object>();
        }
        requestQuery.put("size", 0);
        try {
            PersoniumSearchResponse hit = this.index.search(routingId, requestQuery);
            return hit.getHits().getAllPages();
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Execute search request for index.
     * @param routingId routingId
     *@ param query search query
     *@return Search results
     */
    public PersoniumSearchResponse searchForIndex(String routingId, Map<String, Object> query) {
        try {
            if (!query.containsKey("size")) {
                try {
                    //When size is not specified, size is set so as to acquire all cases
                    query.put("size", 0);
                    PersoniumSearchResponse hit = this.index.search(routingId, query);
                    query.put("size", hit.getHits().getAllPages());
                } catch (EsClientException.EsNoResponseException e) {
                    throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
                }
            }
            return this.index.search(routingId, query);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     *Execute multi search request for index.
     * @param routingId routingId
     *@ param queryList Search query list
     *@return Search results
     */
    public PersoniumMultiSearchResponse multiSearchForIndex(String routingId, List<Map<String, Object>> queryList) {
        try {
            return this.index.multiSearch(routingId, queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

}

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
 * データストア層の基本処理を実装した基底クラス.
 */
public class DataSourceAccessor {
    private EsIndex index;
    private EsType type;
    private String routingid;

    /** ログ用オブジェクト. */
    static Logger log = LoggerFactory.getLogger(DataSourceAccessor.class);

    /**
     * コンストラクタ.
     * @param index インデックス
     */
    public DataSourceAccessor(EsIndex index) {
        this.index = index;
    }

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
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
     * Indexのゲッター.
     * @return EsIndex
     */
    public EsIndex getIndex() {
        return this.index;
    }

    /**
     * Typeを取得する.
     * @return 応答
     */
    public String getType() {
        return this.type.getType();
    }

    /**
     * ESへの検索時に使用するルーティングIDを取得する.
     * @return ルーティングID
     */
    protected String getRoutingId() {
        return this.routingid;
    }

    /**
     * ドキュメントの１件取得.
     * @param id ドキュメントのID
     * @return 応答
     */
    public PersoniumGetResponse get(final String id) {
        try {
            return this.type.get(id);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * ドキュメント新規作成.
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings("rawtypes")
    public PersoniumIndexResponse create(final Map data) {
        String id = PersoniumUUID.randomUUID();
        return this.create(id, data);
    }

    /**
     * ドキュメント新規作成.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings({"rawtypes" })
    public PersoniumActionResponse createForDavNodeFile(final String id, final Map data) {
        PersoniumIndexResponse res = create(id, data);
        return res;
    }

    /**
     * ドキュメント新規作成.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
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
     * ドキュメント新規作成(Cell作成用).
     * @param id ID
     * @param data ドキュメント
     * @param docHandler ドキュメントハンドラ
     * @return ES応答
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
     * ドキュメント更新.
     * @param id ID
     * @param data ドキュメント
     * @param version version番号
     * @return ES応答
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
     * ドキュメント更新.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings("rawtypes")
    public PersoniumIndexResponse update(final String id, final Map data) {
        return this.update(id, data, -1);
    }

    /**
     * ドキュメントの件数を取得.
     * @param query クエリ情報
     * @return ES応答
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
     * ドキュメントを検索.
     * @param query クエリ情報
     * @return ES応答
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
     * ドキュメントをマルチ検索.
     * 本メソッド時使用時には取得件数(size)をクエリに指定して呼び出すこと
     * @param queryList クエリ情報の一覧
     * @return ES応答
     */
    public PersoniumMultiSearchResponse multiSearch(final List<Map<String, Object>> queryList) {
        try {
            return this.type.multiSearch(queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }

    }

    /**
     * ESのインデックスに対してドキュメントを検索する.
     * @param query クエリ情報
     * @return ES応答
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
     * @return 応答
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
     * バルクでデータを登録する.<br />
     * 更新、削除は未サポート.
     * @param esBulkRequest ES用バルク登録ドキュメントリスト
     * @param routingId routingId
     * @return バルクレスポンス
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
     * バルクでデータを登録/更新する.<br />
     * 削除は未サポート.
     * @param esBulkRequest ES用バルク登録ドキュメントリスト
     * @param routingId routingId
     * @return バルクレスポンス
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
     * インデックスに対して検索リクエストを実行する.
     * @param routingId routingId
     * @param query 検索クエリ
     * @return 検索結果
     */
    public PersoniumSearchResponse searchForIndex(String routingId, Map<String, Object> query) {
        try {
            if (!query.containsKey("size")) {
                try {
                    // サイズの指定がない場合は、全件取得するようsizeを設定
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
     * インデックスに対してマルチ検索リクエストを実行する.
     * @param routingId routingId
     * @param queryList 検索クエリ一覧
     * @return 検索結果
     */
    public PersoniumMultiSearchResponse multiSearchForIndex(String routingId, List<Map<String, Object>> queryList) {
        try {
            return this.index.multiSearch(routingId, queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw PersoniumCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

}

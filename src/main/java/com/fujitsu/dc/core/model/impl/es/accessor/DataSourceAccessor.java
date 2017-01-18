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
package com.fujitsu.dc.core.model.impl.es.accessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogWriter;
import com.fujitsu.dc.common.es.EsBulkRequest;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.EsType;
import com.fujitsu.dc.common.es.query.DcQueryBuilder;
import com.fujitsu.dc.common.es.response.DcActionResponse;
import com.fujitsu.dc.common.es.response.DcBulkItemResponse;
import com.fujitsu.dc.common.es.response.DcBulkResponse;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcGetResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcMultiSearchResponse;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.common.es.response.EsClientException;
import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.core.model.impl.es.ads.AdsConnectionException;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.ads.JdbcAds;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.lock.Lock;
import com.fujitsu.dc.core.model.lock.LockKeyComposer;

/**
 * データストア層の基本処理を実装した基底クラス.
 */
public class DataSourceAccessor {
    private EsIndex index;
    private EsType type;
    private JdbcAds ads;
    private String routingid;

    /** ログ用オブジェクト. */
    static Logger log = LoggerFactory.getLogger(DataSourceAccessor.class);

    /**
     * コンストラクタ.
     * @param index インデックス
     */
    public DataSourceAccessor(EsIndex index) {
        this.index = index;
        try {
            if (DcCoreConfig.getEsAdsType().equals(DcCoreConfig.ES.ADS.TYPE_JDBC)) {
                ads = new JdbcAds();
            } else {
                ads = null;
            }
        } catch (AdsConnectionException ex) {
            // 初回接続エラー時は接続エラーのログを出力する.
            DcCoreLog.Server.ADS_CONNECTION_ERROR.params(ex.getMessage()).reason(ex).writeLog();
            throw DcCoreException.Server.ADS_CONNECTION_ERROR;
        }
    }

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingID
     */
    protected DataSourceAccessor(EsIndex index, String name, String routingId) {
        this.index = index;
        int times = Integer.valueOf(DcCoreConfig.getESRetryTimes());
        int interval = Integer.valueOf(DcCoreConfig.getESRetryInterval());
        this.type = EsModel.type(index.getName(), name, routingId, times, interval);
        this.routingid = routingId;
        try {
            if (DcCoreConfig.getEsAdsType().equals(DcCoreConfig.ES.ADS.TYPE_JDBC)) {
                ads = new JdbcAds();
            } else {
                ads = null;
            }
        } catch (AdsConnectionException ex) {
            // 初回接続エラー時は接続エラーのログを出力する.
            DcCoreLog.Server.ADS_CONNECTION_ERROR.params(ex.getMessage()).reason(ex).writeLog();
            throw DcCoreException.Server.ADS_CONNECTION_ERROR;
        }
    }

    /**
     * Adsのゲッター.
     * @return JdbcAds
     */
    protected JdbcAds getAds() {
        return this.ads;
    }

    /**
     * Adsのセッター.
     * @param ads JdbcAds
     */
    protected void setAds(JdbcAds ads) {
        this.ads = ads;
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
    public DcGetResponse get(final String id) {
        try {
            return this.type.get(id);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * ドキュメント新規作成.
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings("rawtypes")
    public DcIndexResponse create(final Map data) {
        String id = DcUUID.randomUUID();
        return this.create(id, data);
    }

    /**
     * ドキュメント新規作成.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings({"rawtypes" })
    public DcActionResponse createForDavNodeFile(final String id, final Map data) {
        DcIndexResponse res = create(id, data);
        return res;
    }

    /**
     * ドキュメント新規作成.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings({"rawtypes" })
    public DcIndexResponse create(final String id, final Map data) {
        try {
            return this.type.create(id, data);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw DcCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            DcCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                createAdsIndex(null);
                return this.type.create(id, data);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw DcCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
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
    public DcIndexResponse create(final String id, final Map data, final EntitySetDocHandler docHandler) {
        try {
            return this.type.create(id, data);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw DcCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            DcCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                createAdsIndex(docHandler.getUnitUserName());
                return this.type.create(id, data);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw DcCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
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
    public DcIndexResponse update(final String id, final Map data, final long version) {
        try {
            return this.type.update(id, data, version);
        } catch (EsClientException.EsSchemaMismatchException e) {
            throw DcCoreException.OData.SCHEMA_MISMATCH;
        } catch (EsClientException.EsIndexMissingException e) {
            DcCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            try {
                this.index.create();
                createAdsIndex(null);
                return this.type.update(id, data, version);
            } catch (EsClientException.EsNoResponseException esRetry) {
                throw DcCoreException.Server.ES_RETRY_OVER.params(esRetry.getMessage());
            }
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * ドキュメント更新.
     * @param id ID
     * @param data ドキュメント
     * @return ES応答
     */
    @SuppressWarnings("rawtypes")
    public DcIndexResponse update(final String id, final Map data) {
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
            DcSearchResponse hit = this.type.search(requestQuery);
            return hit.getHits().getAllPages();
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * ドキュメントを検索.
     * @param query クエリ情報
     * @return ES応答
     */
    public DcSearchResponse search(final Map<String, Object> query) {
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
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * ドキュメントをマルチ検索.
     * 本メソッド時使用時には取得件数(size)をクエリに指定して呼び出すこと
     * @param queryList クエリ情報の一覧
     * @return ES応答
     */
    public DcMultiSearchResponse multiSearch(final List<Map<String, Object>> queryList) {
        try {
            return this.type.multiSearch(queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }

    }

    /**
     * ESのインデックスに対してドキュメントを検索する.
     * @param query クエリ情報
     * @return ES応答
     */
    public DcSearchResponse indexSearch(final Map<String, Object> query) {
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
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * Delete a document.
     * @param docId Document id to delete
     * @param version The version of the document to delete
     * @return 応答
     */
    public DcDeleteResponse delete(final String docId, final long version) {
        try {
            return this.type.delete(docId, version);
        } catch (EsClientException.EsIndexMissingException e) {
            DcCoreLog.Server.ES_INDEX_NOT_EXIST.params(this.index.getName()).writeLog();
            return null;
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * バルクでデータを登録する.<br />
     * 更新、削除は未サポート.
     * @param esBulkRequest ES用バルク登録ドキュメントリスト
     * @param adsBulkRequest ADS用バルク登録ドキュメントリスト
     * @param routingId routingId
     * @return バルクレスポンス
     */
    public DcBulkResponse bulkCreate(List<EsBulkRequest> esBulkRequest,
            List<EntitySetDocHandler> adsBulkRequest,
            String routingId) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(this.index.getName());

        DcBulkResponse response = null;
        try {
            response = this.index.bulkRequest(routingId, esBulkRequest, false);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
        if (this.ads != null) {
            try {
                this.ads.bulkEntity(this.index.getName(), adsBulkRequest);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_BULK_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                for (EntitySetDocHandler docHandler : adsBulkRequest) {
                    // Adsの登録に失敗した場合は、専用のログに書込む
                    String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                            docHandler.getCellId(), null, docHandler.getNodeId());
                    AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                            this.getIndex().getName(), docHandler.getType(), lockKey,
                            docHandler.getCellId(), docHandler.getId(),
                            AdsWriteFailureLogInfo.OperationKind.CREATE, 1, docHandler.getUpdated());
                    recordAdsWriteFailureLog(loginfo);
                }
            }
        }
        return response;
    }

    /**
     * バルクでデータを登録/更新する.<br />
     * 削除は未サポート.
     * @param esBulkRequest ES用バルク登録ドキュメントリスト
     * @param adsBulkEntityRequest ADS用バルク更新ドキュメントリスト(Entity)
     * @param adsBulkLinkRequest ADS用バルク登録ドキュメントリスト(Link)
     * @param routingId routingId
     * @return バルクレスポンス
     */
    public DcBulkResponse bulkUpdateLink(List<EsBulkRequest> esBulkRequest,
            List<EntitySetDocHandler> adsBulkEntityRequest,
            List<LinkDocHandler> adsBulkLinkRequest,
            String routingId) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(this.index.getName());

        DcBulkResponse response = null;
        try {
            response = this.index.bulkRequest(routingId, esBulkRequest, false);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
        if (this.ads != null) {
            try {
                // Entityテーブル更新
                if (adsBulkEntityRequest.size() > 0) {
                    this.ads.bulkUpdateEntity(this.index.getName(), adsBulkEntityRequest);
                }
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_BULK_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                // ESでのバージョン情報を取得するためにesBulkRequestをループさせている
                DcBulkItemResponse[] responseItems = response.items();
                int responseIndex = 0;
                int adsBulkEntityRequestIndex = 0;
                for (EsBulkRequest request : esBulkRequest) {
                    if (request.getType().equals(EsModel.TYPE_CTL_LINK)) {
                        responseIndex++;
                        continue;
                    }
                    DcBulkItemResponse itemResponse = responseItems[responseIndex++];
                    EntitySetDocHandler docHandler = adsBulkEntityRequest.get(adsBulkEntityRequestIndex++);
                    String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                            docHandler.getCellId(), null, docHandler.getNodeId());
                    AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                            this.getIndex().getName(), docHandler.getType(), lockKey,
                            docHandler.getCellId(), docHandler.getId(),
                            AdsWriteFailureLogInfo.OperationKind.UPDATE, itemResponse.version(),
                            docHandler.getUpdated());
                    recordAdsWriteFailureLog(loginfo);
                }
            }
            try {
                // Linkテーブル追加
                if (adsBulkLinkRequest.size() > 0) {
                    this.ads.bulkCreateLink(this.index.getName(), adsBulkLinkRequest);
                }
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_BULK_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                for (LinkDocHandler docHandler : adsBulkLinkRequest) {
                    // Adsの登録に失敗した場合は、専用のログに書込む
                    String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                            docHandler.getCellId(), null, docHandler.getNodeId());
                    AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                            this.getIndex().getName(), EsModel.TYPE_CTL_LINK, lockKey,
                            docHandler.getCellId(), docHandler.getId(),
                            AdsWriteFailureLogInfo.OperationKind.CREATE, 1, docHandler.getUpdated());
                    recordAdsWriteFailureLog(loginfo);
                }
            }
        }
        return response;
    }

    /**
     * 指定されたクエリを使用してデータの削除を行う.
     * @param routingId routingId
     * @param deleteQuery 削除対象を指定するクエリ
     */
    protected void deleteByQuery(String routingId, DcQueryBuilder deleteQuery) {
        this.index.deleteByQuery(routingId, deleteQuery);
    }

    /**
     * 指定されたIDのセルのリソースを削除する.
     * @param cellId 削除対象のセルID
     * @param unitUserName ユニットユーザ名
     * @throws AdsException 削除に失敗
     */
    protected void cellBulkDeletionAds(String cellId, String unitUserName) throws AdsException {
        this.ads.deleteCellResourceFromEntity(unitUserName, cellId);
        this.ads.deleteCellResourceFromDavNode(unitUserName, cellId);
        this.ads.deleteCellResourceFromLink(unitUserName, cellId);
    }

    /**
     * インデックスに対して検索リクエストを実行する.
     * @param routingId routingId
     * @param query 検索クエリ
     * @return 検索結果
     */
    public DcSearchResponse searchForIndex(String routingId, Map<String, Object> query) {
        try {
            if (!query.containsKey("size")) {
                try {
                    // サイズの指定がない場合は、全件取得するようsizeを設定
                    query.put("size", 0);
                    DcSearchResponse hit = this.index.search(routingId, query);
                    query.put("size", hit.getHits().getAllPages());
                } catch (EsClientException.EsNoResponseException e) {
                    throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
                }
            }
            return this.index.search(routingId, query);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * インデックスに対してマルチ検索リクエストを実行する.
     * @param routingId routingId
     * @param queryList 検索クエリ一覧
     * @return 検索結果
     */
    public DcMultiSearchResponse multiSearchForIndex(String routingId, List<Map<String, Object>> queryList) {
        try {
            return this.index.multiSearch(routingId, queryList);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
    }

    /**
     * 引数で渡されたUnitUser名でADS上にUnitUserを作成する.
     * @param unitUserName UnitUser名。nullの場合はEsIndexの値を使用する。
     */
    protected void createAdsIndex(String unitUserName) {
        if (ads == null) {
            return;
        }
        String indexName = this.index.getName();
        if (unitUserName != null) {
            indexName = unitUserName;
        }
        try {
            ads.createIndex(indexName);
        } catch (AdsException adsEx) {
            // TODO エラー処理が必要？参照モードにする必要があるのでは？要検討。
            DcCoreLog.Server.FAILED_TO_CREATE_ADS.params(indexName).reason(adsEx).writeLog();
        }
    }

    /**
     * データの登録/更新/削除実行前の処理.
     * @param unitId unitId
     */
    protected void prepareDataUpdate(final String unitId) {
        checkAdsConnection();
    }

    /**
     * Adsの接続確認を行う.
     */
    protected void checkAdsConnection() {
        try {
            if (ads != null) {
                ads.checkConnection();
            }
        } catch (AdsException e) {
            // 接続に失敗した場合はエラーレスポンスを返却する
            DcCoreLog.Server.ADS_CONNECTION_ERROR.params(e.getMessage()).reason(e).writeLog();
            throw DcCoreException.Server.ADS_CONNECTION_ERROR.reason(e);
        }
    }

    /**
     * Ads書込みエラー時にファイルにリペア用のエラー情報を書込む.
     * @param loginfo リペア用のエラー情報
     */
    protected void recordAdsWriteFailureLog(AdsWriteFailureLogInfo loginfo) {
        AdsWriteFailureLogWriter adsWriteFailureLogWriter = AdsWriteFailureLogWriter.getInstance(
                DcCoreConfig.getAdsWriteFailureLogDir(),
                DcCoreConfig.getCoreVersion(),
                DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        try {
            adsWriteFailureLogWriter.writeActiveFile(loginfo);
        } catch (AdsWriteFailureLogException e2) {
            DcCoreLog.Server.WRITE_ADS_FAILURE_LOG_ERROR.reason(e2).writeLog();
            DcCoreLog.Server.WRITE_ADS_FAILURE_LOG_INFO.params(loginfo.toString());
        }
    }

}

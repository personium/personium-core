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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.personium.common.ads.AdsWriteFailureLogInfo;
import io.personium.common.es.EsIndex;
import io.personium.common.es.query.PersoniumQueryBuilder;
import io.personium.common.es.query.PersoniumQueryBuilders;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.ads.AdsException;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockKeyComposer;

/**
 * Cellのアクセス処理を実装したクラス.
 */
public class CellAccessor extends AbstractEntitySetAccessor {

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingId
     */
    public CellAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * セル配下のDavFile数を返却する.
     * @param cellId 削除対象のセルID
     * @param unitUserName ユニットユーザ名
     * @return セル配下のDavFile数
     */
    public long getDavFileTotalCount(String cellId, String unitUserName) {
        // CellAccessorはadインデックスに対するアクセスのため、ユニットユーザ側のアクセッサを取得
        DataSourceAccessor accessor = EsModel.dsa(unitUserName);

        // Countのみを取得するためサイズを0で指定
        Map<String, Object> countQuery = getDavFileFilterQuery(cellId);
        countQuery.put("size", 0);

        PersoniumSearchResponse response = accessor.searchForIndex(cellId, countQuery);
        return response.getHits().getAllPages();
    }

    /**
     * セル配下のDavFileID一覧を返却する.
     * @param cellId 削除対象のセルID
     * @param unitUserName ユニットユーザ名
     * @param size 取得件数
     * @param from 取得開始位置
     * @return セル配下のDavFile数
     */
    public List<String> getDavFileIdList(String cellId, String unitUserName, int size, int from) {
        // CellAccessorはadインデックスに対するアクセスのため、ユニットユーザ側のアクセッサを取得
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

    private Map<String, Object> getDavFileFilterQuery(String cellId) {
        Map<String, Object> cellQuery = new HashMap<String, Object>();
        cellQuery.put("c", cellId);
        Map<String, Object> cellTermQuery = new HashMap<String, Object>();
        cellTermQuery.put("term", cellQuery);

        Map<String, Object> davTypeQuery = new HashMap<String, Object>();
        davTypeQuery.put("t", DavCmp.TYPE_DAV_FILE);
        Map<String, Object> davTypeTermQuery = new HashMap<String, Object>();
        davTypeTermQuery.put("term", davTypeQuery);

        List<Map<String, Object>> andQueryList = new ArrayList<Map<String, Object>>();
        andQueryList.add(cellTermQuery);
        andQueryList.add(davTypeTermQuery);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(andQueryList));

        Map<String, Object> countQuery = new HashMap<String, Object>();
        countQuery.put("query", query);
        return countQuery;
    }
    /**
     * セル配下のエンティティを一括削除する.
     * @param cellId 削除対象のセルID
     * @param unitUserName ユニットユーザ名
     */
    public void cellBulkDeletion(String cellId, String unitUserName) {
        // AdsのCell配下のエンティティはバッチにて削除するので
        // Cell削除管理テーブルに削除対象のDB名とセルIDを追加する
        insertCellDeleteRecord(unitUserName, cellId);
        DataSourceAccessor accessor = EsModel.dsa(unitUserName);

        // セルIDを指定してelasticsearchからセル関連エンティティを一括削除する
        PersoniumQueryBuilder matchQuery = PersoniumQueryBuilders.matchQuery("c", cellId);
        try {
            accessor.deleteByQuery(cellId, matchQuery);
            log.info("KVS Deletion Success.");
        } catch (EsClientException e) {
            // 削除に失敗した場合はログを出力して処理を続行する
            log.warn(String.format("Delete CellResource From KVS Failed. CellId:[%s], CellUnitUserName:[%s]",
                    cellId, unitUserName), e);
        }
    }
    private void insertCellDeleteRecord(String unitUserName, String cellId) {
        CellDeleteAccessor accessor = new CellDeleteAccessor();
        if (!accessor.isValid()) {
            log.warn(String.format("Insert CELL_DELETE Record To Ads Failed. db_name:[%s], cell_id:[%s]",
                    unitUserName, cellId));
            return;
        }
        accessor.createManagementDatabase();
        accessor.insertCellDeleteRecord(unitUserName, cellId);
    }

    /**
     * マスターデータを登録する.
     * @param docHandler 登録データ
     */
    @Override
    protected void createAds(EntitySetDocHandler docHandler) {
        // 登録に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            String unitUserName = docHandler.getUnitUserName();
            try {
                getAds().createCell(unitUserName, docHandler);
            } catch (AdsException e) {
                // Indexが存在しない場合はインデックスを作成する。
                if (e.getCause() instanceof SQLException
                        && MYSQL_BAD_TABLE_ERROR.equals(((SQLException) e.getCause()).getSQLState())) {
                    PersoniumCoreLog.Server.ES_INDEX_NOT_EXIST.params(unitUserName).writeLog();
                    createAdsIndex(unitUserName);
                    try {
                        getAds().createCell(unitUserName, docHandler);
                    } catch (AdsException e1) {
                        PersoniumCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(
                                e1.getMessage()).reason(e1).writeLog();

                        // Adsの登録に失敗した場合は、専用のログに書込む
                        String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                                docHandler.getCellId(), null, null);
                        AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                                docHandler.getUnitUserName(), docHandler.getType(), lockKey,
                                docHandler.getCellId(), docHandler.getId(),
                                AdsWriteFailureLogInfo.OperationKind.CREATE, 1, docHandler.getUpdated());
                        recordAdsWriteFailureLog(loginfo);
                    }
                } else {
                    PersoniumCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                    // Adsの登録に失敗した場合は、専用のログに書込む
                    String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                            docHandler.getCellId(), null, null);
                    AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                            docHandler.getUnitUserName(), docHandler.getType(), lockKey,
                            docHandler.getCellId(), docHandler.getId(),
                            AdsWriteFailureLogInfo.OperationKind.CREATE, 1, docHandler.getUpdated());
                    recordAdsWriteFailureLog(loginfo);
                }
            }
        }
    }

    /**
     * マスターデータを更新する.
     * @param docHandler 登録データ
     * @param version Elasticsearchに登録されたドキュメントのバージョン
     */
    @Override
    protected void updateAds(EntitySetDocHandler docHandler, long version) {
        // 更新に成功した場合、マスタデータを更新する
        if (getAds() != null) {
            try {
                getAds().updateCell(docHandler.getUnitUserName(), docHandler);
            } catch (AdsException e) {
                PersoniumCoreLog.Server.DATA_STORE_ENTITY_UPDATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        docHandler.getUnitUserName(), docHandler.getType(), lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.UPDATE, version, docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }

    /**
     * マスタデータを削除する.
     * @param docHandler 削除データ
     * @param version 削除したデータのバージョン
     */
    @Override
    protected void deleteAds(EntitySetDocHandler docHandler, long version) {
        String id = docHandler.getId();
        String unitUserName = docHandler.getUnitUserName();

        // 削除に成功した場合、マスタデータを削除する
        if (getAds() != null) {
            try {
                getAds().deleteCell(unitUserName, id);
            } catch (AdsException e) {
                PersoniumCoreLog.Server.DATA_STORE_ENTITY_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        docHandler.getUnitUserName(), docHandler.getType(), lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.DELETE, version, docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }
}

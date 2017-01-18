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

import java.sql.SQLException;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.lock.Lock;
import com.fujitsu.dc.core.model.lock.LockKeyComposer;

/**
 * ODataEntityのアクセス処理を実装したクラス.
 */
public class ODataEntityAccessor extends AbstractEntitySetAccessor {

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingId
     */
    public ODataEntityAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * マスターデータを登録する.
     * @param docHandler 登録データ
     */
    protected void createAds(EntitySetDocHandler docHandler) {
        // 登録に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            String indexName = getIndex().getName();
            try {
                getAds().createEntity(indexName, docHandler);
            } catch (AdsException e) {
                // Indexが存在しない場合はインデックスを作成する。
                if (e.getCause() instanceof SQLException
                        && MYSQL_BAD_TABLE_ERROR.equals(((SQLException) e.getCause()).getSQLState())) {
                    DcCoreLog.Server.ES_INDEX_NOT_EXIST.params(indexName).writeLog();
                    createAdsIndex(indexName);
                    try {
                        getAds().createEntity(indexName, docHandler);
                    } catch (AdsException e1) {
                        DcCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(e1.getMessage()).reason(e1).writeLog();

                        // Adsの登録に失敗した場合は、専用のログに書込む
                        String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                                docHandler.getCellId(), null, docHandler.getNodeId());
                        AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                                this.getIndex().getName(), docHandler.getType(), lockKey,
                                docHandler.getCellId(), docHandler.getId(),
                                AdsWriteFailureLogInfo.OperationKind.CREATE, 1, docHandler.getUpdated());
                        recordAdsWriteFailureLog(loginfo);
                    }
                } else {
                    DcCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

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
    }

    /**
     * マスターデータを更新する.
     * @param docHandler 登録データ
     * @param version Elasticsearchに登録されたドキュメントのバージョン
     */
    protected void updateAds(EntitySetDocHandler docHandler, long version) {
        // 更新に成功した場合、マスタデータを更新する
        if (getAds() != null) {
            try {
                getAds().updateEntity(getIndex().getName(), docHandler);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_UPDATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, docHandler.getNodeId());
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), docHandler.getType(), lockKey,
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

        // 削除に成功した場合、マスタデータを削除する
        if (getAds() != null) {
            try {
                getAds().deleteEntity(getIndex().getName(), id);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, docHandler.getNodeId());
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), docHandler.getType(), lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.DELETE, version, docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }
}

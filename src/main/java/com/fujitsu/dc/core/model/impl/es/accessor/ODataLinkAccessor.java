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

import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.lock.Lock;
import com.fujitsu.dc.core.model.lock.LockKeyComposer;

/**
 * ODataLink情報のアクセス処理を実装したクラス.
 */
public class ODataLinkAccessor extends DataSourceAccessor {

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingId
     */
    public ODataLinkAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * UUIDでODataLinkのデータ登録を行う.
     * @param docHandler 登録データ
     * @return 登録結果
     */
    public DcIndexResponse create(final LinkDocHandler docHandler) {
        String id = DcUUID.randomUUID();
        return this.create(id, docHandler);
    }

    /**
     * ODataLinkのデータ登録を行う.
     * @param id 登録データのID
     * @param docHandler 登録データ
     * @return 登録結果
     */
    public DcIndexResponse create(String id, LinkDocHandler docHandler) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        docHandler.setId(id);
        DcIndexResponse response = super.create(id, docHandler.createLinkDoc());
        createAds(docHandler);
        return response;
    }

    /**
     * マスターデータを登録する.
     * @param docHandler 登録データ
     */
    protected void createAds(LinkDocHandler docHandler) {
        // 登録に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().createLink(getIndex().getName(), docHandler);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

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

    /**
     * ODataLinkのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @return 更新結果
     */
    public DcIndexResponse update(String id, LinkDocHandler docHandler) {
        return this.update(id, docHandler, -1);
    }

    /**
     * バージョン指定ありでODataLinkのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @param version バージョン情報
     * @return 更新結果
     */
    public DcIndexResponse update(String id, LinkDocHandler docHandler, long version) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcIndexResponse response = super.update(id, docHandler.createLinkDoc(), version);
        updateAds(docHandler, response.getVersion());
        return response;
    }

    /**
     * マスターデータを更新する.
     * @param docHandler 更新データ
     * @param version Elasticsearchに登録されたドキュメントのバージョン
     */
    protected void updateAds(LinkDocHandler docHandler, long version) {
        // 更新に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().updateLink(getIndex().getName(), docHandler);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_UPDATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, docHandler.getNodeId());
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), EsModel.TYPE_CTL_LINK, lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.UPDATE, version, docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }

    /**
     * Delete a document.
     * @param docHandler 削除データ
     * @return 応答
     */
    public DcDeleteResponse delete(final LinkDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * ODataLinkのデータ削除を行う.
     * @param docHandler 削除データ
     * @param version バージョン情報
     * @return 削除結果
     */
    public DcDeleteResponse delete(final LinkDocHandler docHandler, long version) {
        String id = docHandler.getId();

        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcDeleteResponse response = super.delete(id, version);
        deleteAds(docHandler, response.getVersion());
        return response;
    }

    /**
     * マスターデータを削除する.
     * @param docHandler 削除データ
     * @param version 削除したデータのバージョン
     */
    protected void deleteAds(LinkDocHandler docHandler, long version) {
        String id = docHandler.getId();

        // 削除に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().deleteLink(getIndex().getName(), id);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                        docHandler.getCellId(), null, docHandler.getNodeId());
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), EsModel.TYPE_CTL_LINK, lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.DELETE, version, docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }
}

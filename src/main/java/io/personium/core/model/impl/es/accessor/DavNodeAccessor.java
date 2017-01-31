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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.ads.AdsWriteFailureLogInfo;
import io.personium.common.es.EsIndex;
import io.personium.common.es.response.DcActionResponse;
import io.personium.common.es.response.DcDeleteResponse;
import io.personium.common.es.response.DcIndexResponse;
import io.personium.common.es.util.DcUUID;
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.DcCoreLog;
import io.personium.core.model.file.BinaryDataAccessException;
import io.personium.core.model.file.BinaryDataAccessor;
import io.personium.core.model.impl.es.DavNode;
import io.personium.core.model.impl.es.ads.AdsException;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockKeyComposer;

/**
 * DavNode情報のアクセス処理を実装したクラス.
 */
public class DavNodeAccessor extends DataSourceAccessor {

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(DavNodeAccessor.class);

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingID
     */
    public DavNodeAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * UUIDでDavNodeのデータ登録を行う.
     * @param davNode Davコンポーネント
     * @return 登録結果
     */
    public DcIndexResponse create(final DavNode davNode) {
        String id = DcUUID.randomUUID();
        return this.create(id, davNode);
    }

    /**
     * DavNodeのデータ登録を行う.
     * @param id 登録データのID
     * @param davNode Davコンポーネント
     * @return 登録結果
     */
    public DcIndexResponse create(String id, DavNode davNode) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        davNode.setId(id);
        DcIndexResponse response = super.create(id, davNode.getSource());
        createAds(davNode);
        return response;
    }

    /**
     * DavNodeファイルのデータ登録を行う.
     * @param id 登録データのID
     * @param davNode Davコンポーネント
     * @return 登録結果
     */
    public DcActionResponse createForFile(String id, DavNode davNode) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(getIndex().getName());
        davNode.setId(id);
        DcActionResponse response = null;
        try {
            // ElasticSearch更新
            response = createForDavNodeFile(id, davNode.getSource());

            // 一時ファイルコピー
            String unitUserName = getIndex().getName().replace(DcCoreConfig.getEsUnitPrefix() + "_", "");
            BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                    unitUserName, DcCoreConfig.getFsyncEnabled());
            binaryDataAccessor.copyFile(id);

            // MySQL更新
            createAds(davNode);
        } catch (BinaryDataAccessException ex) {
            // 一時ファイルコピー失敗
            throw DcCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        } finally {
            // 一時ファイル削除
            deleteTmpFile(id);
        }

        return response;
    }

    private void deleteTmpFile(String id) {
        String unitUserName = getIndex().getName().replace(DcCoreConfig.getEsUnitPrefix() + "_", "");
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                unitUserName, DcCoreConfig.getFsyncEnabled());
        try {
            // 一時ファイル物理削除
            binaryDataAccessor.deletePhysicalFile(id + ".tmp");
        } catch (BinaryDataAccessException e1) {
            log.info(e1.getMessage());
        }
    }

    /**
     * マスターデータを登録する.
     * @param davNode 登録データ
     */
    protected void createAds(DavNode davNode) {
        // 登録に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().createDavNode(getIndex().getName(), davNode);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV, null,
                        davNode.getBoxId(), null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), "dav", lockKey, davNode.getCellId(), davNode.getId(),
                        AdsWriteFailureLogInfo.OperationKind.CREATE, 1, davNode.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }

    /**
     * DavNodeのデータ削除を行う.
     * @param davNode 削除データ
     * @return 削除結果
     */
    public DcDeleteResponse delete(DavNode davNode) {
        return this.delete(davNode, -1);
    }

    /**
     * DavNodeのデータ削除を行う.
     * @param version バージョン情報
     * @param davNode 削除データ
     * @return 削除結果
     */
    public DcDeleteResponse delete(DavNode davNode, long version) {
        String id = davNode.getId();

        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcDeleteResponse response = super.delete(id, version);
        deleteAds(davNode, response.getVersion());
        return response;
    }

    /**
     * マスターデータを削除する.
     * @param davNode 削除データ
     * @param version 削除したデータのバージョン
     */
    protected void deleteAds(DavNode davNode, long version) {
        String id = davNode.getId();

        // 削除に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().deleteDavNode(getIndex().getName(), id);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV, null,
                        davNode.getBoxId(), null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), "dav", lockKey, davNode.getCellId(), davNode.getId(),
                        AdsWriteFailureLogInfo.OperationKind.DELETE, version, davNode.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }

    /**
     * DavNodeのデータ更新を行う.
     * @param id 更新データのID
     * @param davNode Davコンポーネント
     * @return 更新結果
     */
    public DcIndexResponse update(String id, DavNode davNode) {
        return this.update(id, davNode, -1);
    }

    /**
     * バージョン指定ありでDavNodeのデータ更新を行う.
     * @param id 更新データのID
     * @param davNode Davコンポーネント
     * @param version バージョン情報
     * @return 更新結果
     */
    public DcIndexResponse update(String id, DavNode davNode, long version) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcIndexResponse response = super.update(id, davNode.getSource(), version);
        updateAds(davNode, response.getVersion());
        return response;
    }

    /**
     * バージョン指定ありでDavNodeファイルのデータ更新を行う.
     * @param id 更新データのID
     * @param davNode Davコンポーネント
     * @param version バージョン情報
     * @return 更新結果
     */
    public DcIndexResponse updateForFile(String id, DavNode davNode, long version) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcIndexResponse response = null;
        try {
            // ElasticSearch更新
            response = super.update(id, davNode.getSource(), version);

            // 一時ファイルコピー
            String unitUserName = getIndex().getName().replace(DcCoreConfig.getEsUnitPrefix() + "_", "");
            BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                    unitUserName, DcCoreConfig.getFsyncEnabled());
            binaryDataAccessor.copyFile(id);

            // MySQL更新
            updateAds(davNode, response.getVersion());
        } catch (BinaryDataAccessException ex) {
            // 一時ファイルコピー失敗
            throw DcCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        } finally {
            // 一時ファイル削除
            deleteTmpFile(id);
        }

        return response;
    }

    /**
     * マスターデータを更新する.
     * @param davNode 更新データ
     * @param version Elasticsearchに登録されたドキュメントのバージョン
     */
    protected void updateAds(DavNode davNode, long version) {
        // 更新に成功した場合、マスタデータを書き込む
        if (getAds() != null) {
            try {
                getAds().updateDavNode(getIndex().getName(), davNode);
            } catch (AdsException e) {
                DcCoreLog.Server.DATA_STORE_ENTITY_UPDATE_FAIL.params(e.getMessage()).reason(e).writeLog();

                // Adsの登録に失敗した場合は、専用のログに書込む
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV, null,
                        davNode.getBoxId(), null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), "dav", lockKey, davNode.getCellId(), davNode.getId(),
                        AdsWriteFailureLogInfo.OperationKind.UPDATE, version, davNode.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }
}

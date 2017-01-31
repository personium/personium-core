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
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.personium.common.ads.AdsWriteFailureLogInfo;
import io.personium.common.es.EsBulkRequest;
import io.personium.common.es.EsBulkRequest.BULK_REQUEST_TYPE;
import io.personium.common.es.EsIndex;
import io.personium.common.es.response.DcBulkItemResponse;
import io.personium.common.es.response.DcBulkResponse;
import io.personium.common.es.response.EsClientException;
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
 * DavNode情報をMOVEする場合のアクセス処理を実装したクラス.
 */
public class DavMoveAccessor extends DavNodeAccessor {

    DavNode srcParentNodeForRollback;
    DavNode dstParentNodeForRollback;
    DavNode srcNodeForRollback;
    DavNode dstNoeForRollback;

    // ES用バルク登録ドキュメントリスト
    List<EsBulkRequest> esBulkRequest = new ArrayList<EsBulkRequest>();
    // ADS用バルク更新ドキュメントリスト
    List<DavNode> adsBulkRequest = new ArrayList<DavNode>();

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingID
     */
    public DavMoveAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * 移動元の親DavNodeをロールバック用に格納する. <br />
     * 値が設定されていない場合はロールバック対象とはしない。
     * @param davNode DavNode
     */
    public void setSourceParentNodeForRollback(DavNode davNode) {
        this.srcParentNodeForRollback = cloneDavNode(davNode);
    }

    /**
     * 移動先の親DavNodeをロールバック用に格納する. <br />
     * 値が設定されていない場合はロールバック対象とはしない。
     * @param davNode DavNode
     */
    public void setDestinationParentNodeForRollback(DavNode davNode) {
        this.dstParentNodeForRollback = cloneDavNode(davNode);
    }

    /**
     * 移動対象のDavNodeをロールバック用に格納する. <br />
     * 値が設定されていない場合はロールバック対象とはしない。
     * @param davNode DavNode
     */
    public void setSourceNodeForRollback(DavNode davNode) {
        this.srcNodeForRollback = cloneDavNode(davNode);
    }

    /**
     * 移動先のDavNodeをロールバック用に格納する. <br />
     * 値が設定されていない場合はロールバック対象とはしない。
     * @param davNode DavNode
     */
    public void setDestinationNodeForRollback(DavNode davNode) {
        this.dstNoeForRollback = cloneDavNode(davNode);
    }

    /**
     * DavNodeの複製を作成する.
     * @param davNode
     * @return 複製したDavNode
     */
    private DavNode cloneDavNode(DavNode davNode) {
        if (null == davNode) {
            return null;
        }
        String jsonString = davNode.getSource().toJSONString();
        return DavNode.createFromJsonString(davNode.getId(), jsonString);
    }

    /**
     * DavNodeの移動用リクエストをAccessorに設定する.
     * @param srcName 移動対象DavNodeの名前
     * @param dstName 移動対象DavNodeの名前
     * @param srcNode 移動対象DavNode
     * @param dstNode 移動先のDavNodeの階層情報
     * @param srcParentNode 移動対象DavNodeの親DavNode
     * @param dstParentNode 移動先DavNodeの親DavNode
     */
    public void setMoveRequest(
            String srcName,
            String dstName,
            DavNode srcNode,
            DavNode dstNode,
            DavNode srcParentNode,
            DavNode dstParentNode) {

        long now = new Date().getTime();

        // MOVEのパターンによって更新内容を作成し分ける
        if (srcNode.getParentId().equals(dstParentNode.getId())) {
            // 移動せず変名のみの場合
            // 親ノードのchildren情報から、移動対象のリソース情報を削除する
            Map<String, String> srcChildren = srcParentNode.getChildren();
            srcChildren.remove(srcName);

            // 親ノードのchildren情報に、移動対象のリソース情報を追加する
            srcChildren.put(dstName, srcNode.getId());
            srcParentNode.setChildren(srcChildren);
            srcParentNode.setUpdated(now);

            adsBulkRequest.add(srcParentNode);
            esBulkRequest.add(srcParentNode);
        } else {
            // 異なるコレクションに移動する場合
            // 移動前の親ノードのchildren情報から、移動対象のリソース情報を削除する
            Map<String, String> srcChildren = srcParentNode.getChildren();
            srcChildren.remove(srcName);
            srcParentNode.setChildren(srcChildren);
            srcParentNode.setUpdated(now);

            adsBulkRequest.add(srcParentNode);
            esBulkRequest.add(srcParentNode);

            setDestinationParentNodeForRollback(dstParentNode);
            // 移動先の親ノードのchildren情報に、移動対象のリソース情報を追加する
            Map<String, String> dstChildren = dstParentNode.getChildren();
            dstChildren.put(dstName, srcNode.getId());
            dstParentNode.setChildren(dstChildren);
            dstParentNode.setUpdated(now);

            adsBulkRequest.add(dstParentNode);
            esBulkRequest.add(dstParentNode);

            setSourceNodeForRollback(srcNode);
            // 移動対象のリソースの親リソース情報を移動先のものに変更する
            srcNode.setParentId(dstParentNode.getId());
            srcNode.setUpdated(now);

            adsBulkRequest.add(srcNode);
            esBulkRequest.add(srcNode);
        }
        // 移動先のDavNodeが存在する場合は、そのDavNodeを削除する（ファイル実体は含まない）。
        if (null != dstNode) {
            dstNode.setRequestType(EsBulkRequest.BULK_REQUEST_TYPE.DELETE);
            adsBulkRequest.add(dstNode);
            esBulkRequest.add(dstNode);
            setDestinationNodeForRollback(dstNode);
        }
    }

    /**
     * Move用の更新/削除リクエストをバルクで行う.<br/>
     * 全リクエストの情報をログに出力しているため、大量のリクエストを行う際には要注意.<br/>
     * ES用のバルク登録ドキュメント数とADS用のバルク登録ドキュメント数は、同じにすること
     * @return バルクレスポンス
     */
    public DcBulkResponse move() {
        DcBulkResponse response = esMove(this.esBulkRequest);
        // ESへのバルクリクエストでエラーが発生した場合はロールバックするためバイナリファイルの削除とADSの更新は行わない
        if (response.hasFailures()) {
            rollback();
            throw DcCoreException.Server.DATA_STORE_UPDATE_ERROR_ROLLBACKED;
        }

        adsMove(response);

        deleteDavFile(this.dstNoeForRollback);

        return response;
    }

    private DcBulkResponse adsMove(DcBulkResponse response) {
        // ADSへの書込みは、連続した登録/更新のリクエストをバルクで実行する
        // 削除リクエストはリクエストを単体で実行する
        // 例）登録→更新→削除→削除→登録→更新→登録→削除 の順で実行した場合
        // 1. 登録→更新 をバルクで実行
        // 2. 削除を実行
        // 3. 削除を実行
        // 4. 登録→更新→登録 をバルクで実行
        // 5. 削除を実行
        // TODO 削除リクエストを複数実行する際は、バルク化する
        if (getAds() != null) {
            List<DavNode> adsUpdateBulkRequest = new ArrayList<DavNode>();
            int i;
            for (i = 0; i < esBulkRequest.size(); i++) {
                EsBulkRequest esReq = esBulkRequest.get(i);
                if (BULK_REQUEST_TYPE.DELETE != esReq.getRequestType()) {
                    adsUpdateBulkRequest.add(adsBulkRequest.get(i));
                } else {
                    // Davテーブル一括更新
                    bulkUpdateAds(adsUpdateBulkRequest, response, i);
                    adsUpdateBulkRequest.clear();

                    // Davテーブル削除
                    deleteAds(adsBulkRequest.get(i), -1);
                }
            }
            // Davテーブル一括更新
            bulkUpdateAds(adsUpdateBulkRequest, response, i);
        }
        return response;
    }

    /**
     * moveメソッドでのElasticsearchへのデータの更新を行う. <br />
     * ロールバックでもこのメソッドを使用するため、リクエスト情報はパラメータとして受け取る。
     * @param esRequest ES用バルク登録ドキュメントリスト
     * @return バルクレスポンス
     */
    public DcBulkResponse esMove(List<EsBulkRequest> esRequest) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(getIndex().getName());

        DcBulkResponse response = null;
        try {
            response = getIndex().bulkRequest(getRoutingId(), esRequest, true);
        } catch (EsClientException.EsNoResponseException e) {
            throw DcCoreException.Server.ES_RETRY_OVER.params(e.getMessage());
        }
        return response;
    }

    /**
     * マスターデータを一括更新する.
     * @param metaFile 更新データ
     * @param response Elasticsearchのバルク更新レスポンス（Ads書込み失敗ログ出力用）
     * @param responseIndex Elasticsearchのバルク更新レスポンスのインデックス
     */
    private void bulkUpdateAds(List<DavNode> adsRequest, DcBulkResponse response, int responseIndex) {
        try {
            // Davテーブルを一括更新
            if (adsRequest.size() > 0) {
                getAds().bulkUpdateDav(getIndex().getName(), adsRequest);
            }
        } catch (AdsException e) {
            DcCoreLog.Server.DATA_STORE_ENTITY_BULK_CREATE_FAIL.params(e.getMessage()).reason(e).writeLog();

            // Adsの登録に失敗した場合は、専用のログに書込む
            // ESでのバージョン情報を取得するためにesBulkRequestをループさせている
            DcBulkItemResponse[] responseItems = response.items();

            for (DavNode docHandler : adsRequest) {
                // Adsの登録に失敗した場合は、専用のログに書込む
                DcBulkItemResponse itemResponse = responseItems[responseIndex++];
                String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV,
                        null, docHandler.getBoxId(), null);
                AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                        this.getIndex().getName(), "dav", lockKey,
                        docHandler.getCellId(), docHandler.getId(),
                        AdsWriteFailureLogInfo.OperationKind.UPDATE, itemResponse.version(),
                        docHandler.getUpdated());
                recordAdsWriteFailureLog(loginfo);
            }
        }
    }

    /**
     * ESへのバルクリクエスト失敗時に、元の状態に戻すためのリクエストを実行する.
     */
    private void rollback() {
        log.info("Failed to update data store, then rollback start");
        // バルクリクエスト作成
        List<DavNode> adsRequest = new ArrayList<DavNode>();
        List<EsBulkRequest> esRequest = new ArrayList<EsBulkRequest>();

        adsRequest.add(this.srcParentNodeForRollback);
        esRequest.add(this.srcParentNodeForRollback);

        if (null != this.dstParentNodeForRollback) {
            adsRequest.add(this.dstParentNodeForRollback);
            esRequest.add(this.dstParentNodeForRollback);
        }

        if (null != this.srcNodeForRollback) {
            adsRequest.add(this.srcNodeForRollback);
            esRequest.add(this.srcNodeForRollback);
        }

        if (null != this.dstNoeForRollback) {
            adsRequest.add(this.dstNoeForRollback);
            esRequest.add(this.dstNoeForRollback);
        }

        // ロールバックではElasticsearchのデータのみを戻す
        DcBulkResponse bulkResponse = esMove(esRequest);
        if (bulkResponse.hasFailures()) {
            // ロールバック失敗時はロールバックしようとしたデータの内容を出力する
            log.info("rollback was abnormally end.");
            outputRollbackRequest("srcParent ", srcParentNodeForRollback);
            outputRollbackRequest("dstParent", dstParentNodeForRollback);
            outputRollbackRequest("source", srcNodeForRollback);
            outputRollbackRequest("destination", dstNoeForRollback);
            // ロールバックに失敗
            throw DcCoreException.Server.DATA_STORE_UPDATE_ROLLBACK_ERROR;
        }
        // ロールバックに成功
        log.info("rollback was successfully end.");
    }

    /**
     * ロールバック失敗時にデータ補正用のDavNodeデータを出力する.
     * @param prefix データ項目名
     * @param davNode DavNode
     */
    private void outputRollbackRequest(String prefix, DavNode davNode) {
        if (null != davNode) {
            log.info(String.format("%-11s: %s", prefix, davNode.getSource().toJSONString()));
        }
    }

    /**
     * DavNodeのIDをもとにファイルを削除する.
     * @param davCmp Davコンポーネント
     */
    private void deleteDavFile(DavNode davNode) {
        if (null != davNode) {
            BinaryDataAccessor accessor = getBinaryDataAccessor();
            try {
                accessor.delete(davNode.getId());
            } catch (BinaryDataAccessException e) {
                DcCoreLog.Dav.FILE_DELETE_FAIL.params(davNode.getId()).writeLog();
            }
        }
    }

    /**
     * バイナリデータのアクセサのインスタンスを生成して返す.
     * @return アクセサのインスタンス
     */
    protected BinaryDataAccessor getBinaryDataAccessor() {
        String unitUserName = getIndex().getName().replace(DcCoreConfig.getEsUnitPrefix() + "_", "");
        return new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(), unitUserName,
                DcCoreConfig.getPhysicalDeleteMode(), DcCoreConfig.getFsyncEnabled());
    }
}

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
package io.personium.core.model.impl.es;

import java.util.Map;

import io.personium.common.es.EsClient;
import io.personium.common.es.EsClient.Event;
import io.personium.common.es.EsIndex;
import io.personium.common.es.EsRequestLogInfo;
import io.personium.common.es.EsType;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.DavNodeAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataEntityAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;

/**
 * 本アプリでElasticSearchを扱うモデル.
 */
public class EsModel {

    private static EsClient esClient;

    static {
        // ESへの接続後にログを出力するハンドラを設定
        EsClient.setEventHandler(Event.connected, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.CONNECTED.params(params).writeLog();
            }
        });
        // ESへの登録以外のリクエスト後にログを出力するハンドラを設定
        EsClient.setEventHandler(Event.afterRequest, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.AFTER_REQUEST.params(params).writeLog();
            }
        });
        // ESへのインデックス作成前にログを出力するハンドラを設定
        EsClient.setEventHandler(Event.creatingIndex, new EsClient.EventHandler() {
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                PersoniumCoreLog.Es.CREATING_INDEX.params(params).writeLog();
            }
        });
        // ESへの登録リクエスト後にログを出力するハンドラを設定
        EsClient.setEventHandler(Event.afterCreate, new EsClient.EventHandler() {
            @SuppressWarnings("unchecked")
            @Override
            public void handleEvent(EsRequestLogInfo logInfo, Object... params) {
                if (logInfo == null) {
                    return; // 出力情報がないためログは出力せずに終了する
                } else if (UserDataODataProducer.USER_ODATA_NAMESPACE.equals(logInfo.getType())) {
                    String uuid = "";
                    Map<String, Object> body = logInfo.getData();
                    if (body != null && body.containsKey("s")) {
                        Map<String, Object> staticFields = (Map<String, Object>) body.get("s");
                        if (staticFields != null && staticFields.containsKey("__id")) {
                            uuid = (String) staticFields.get("__id");
                        }
                    }
                    PersoniumCoreLog.Es.AFTER_CREATE.params(logInfo.getIndex(),
                            logInfo.getType(), logInfo.getId(), logInfo.getOpType(), uuid).writeLog();
                    PersoniumCoreLog.Es.AFTER_CREATE_BODY.params(logInfo.getDataAsString()).writeLog();

                } else {
                    PersoniumCoreLog.Es.AFTER_CREATE.params(logInfo.getIndex(), logInfo.getType(),
                            logInfo.getId(), logInfo.getOpType(), logInfo.getDataAsString()).writeLog();
                }
            }
        });

        esClient = new EsClient(PersoniumUnitConfig.getEsClusterName(), PersoniumUnitConfig.getEsHosts());
    }

    private EsModel() {
    }

    /**
     * ESクライアントオブジェクトを返す.
     * @return クライアントオブジェクト
     */
    public static EsClient client() {
        return esClient;
    }

    /**
     * 管理用のIndex操作オブジェクトを返します.
     * @return Indexオブジェクト
     */
    public static EsIndex idxAdmin() {
        return esClient.idxAdmin(PersoniumUnitConfig.getEsUnitPrefix(),
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * UnitUser用のIndex操作オブジェクトを返します.
     * @param userUri UnitUser名（URL)
     * @return Indexオブジェクト
     */
    public static EsIndex idxUser(String userUri) {
        return esClient.idxUser(PersoniumUnitConfig.getEsUnitPrefix(),
                userUri,
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * ESのインデックス名から、UnitUser用のIndex操作オブジェクトを返します.
     * @param indexName ESのインデックス名
     * @return Indexオブジェクト
     */
    public static EsIndex idxUserWithUnitPrefix(String indexName) {
        return esClient.idxUser(indexName,
                Integer.valueOf(PersoniumUnitConfig.getESRetryTimes()),
                Integer.valueOf(PersoniumUnitConfig.getESRetryInterval()));
    }

    /**
     * 指定された名前のIndex操作オブジェクトを返します.
     * @param indexName index名
     * @param typeName indexの種類
     * @param routingId indexの種類
     * @param times indexの種類
     * @param interval indexの種類
     * @return EsTypeオブジェクト
     */
    public static EsType type(String indexName, String typeName, String routingId, int times, int interval) {
        return esClient.type(indexName, typeName, routingId, times, interval);
    }

    /**
     * Cell用のType操作オブジェクトを返します.
     * @return Typeオブジェクト
     */
    public static EntitySetAccessor cell() {
        return new CellAccessor(idxAdmin(), Cell.EDM_TYPE_NAME, EsIndex.CELL_ROUTING_KEY_NAME);
    }

    /**
     * Box用のType操作オブジェクトを返します.
     * @param cell Cell
     * @return Typeオブジェクト
     */
    public static EntitySetAccessor box(final Cell cell) {
        return cell(cell, Box.EDM_TYPE_NAME);
    }

    /**
     * 指定タイプ名のUnit制御Type操作オブジェクトを返します.
     * @param type タイプ名
     * @param cellId cellId
     * @return Typeオブジェクト
     */
    public static EntitySetAccessor unitCtl(final String type, final String cellId) {
        if ("Cell".equals(type)) {
            return EsModel.cell();
        } else {
            return new ODataEntityAccessor(idxAdmin(), type, cellId);
        }
    }

    /**
     * 指定Cellの指定タイプ名のCell制御Type操作オブジェクトを返します.
     * @param cell Cell
     * @param type タイプ名
     * @return Typeオブジェクト
     */
    public static EntitySetAccessor cellCtl(final Cell cell, final String type) {
        return cell(cell, type);
    }

    static EntitySetAccessor cell(final Cell cell, final String type) {
        String userUri = cell.getOwner();
        return new ODataEntityAccessor(idxUser(userUri), type, cell.getId());
    }

    /**
     * Unit制御オブジェクト間リンク情報Typeの操作オブジェクトを返します.
     * @param cellId cellId
     * @return Typeオブジェクト
     */
    public static ODataLinkAccessor unitCtlLink(String cellId) {
        return new ODataLinkAccessor(idxAdmin(), TYPE_CTL_LINK, cellId);
    }

    /**
     * 指定CellのCell制御オブジェクト間リンク情報Typeの操作オブジェクトを返します.
     * @param cell Cell
     * @return Typeオブジェクト
     */
    public static ODataLinkAccessor cellCtlLink(final Cell cell) {
        String userUri = cell.getOwner();
        return new ODataLinkAccessor(idxUser(userUri), TYPE_CTL_LINK, cell.getId());
    }

    /**
     * Link 情報を保存する Type名.
     */
    public static final String TYPE_CTL_LINK = "link";

    /**
     * 指定Cell, BoxのDavノード情報Typeの操作オブジェクトを返します.
     * @param cell Cell
     * @return Typeオブジェクト
     */
    public static DavNodeAccessor col(final Cell cell) {
        return new DavNodeAccessor(idxUser(cell.getOwner()), "dav", cell.getId());
    }

    /**
     * Cell用のBulkDataAccessorを返します.
     * @return Typeオブジェクト
     */
    public static DataSourceAccessor batch() {
        return new DataSourceAccessor(idxAdmin());
    }

    /**
     * 指定CellのBulkDataAccessorを返します.
     * @param cell Cell
     * @return BulkDataAccessor
     */
    public static DataSourceAccessor batch(final Cell cell) {
        return new DataSourceAccessor(idxUser(cell.getOwner()));
    }

    /**
     * 指定UnitUser名のDataSourceAccessorを返します.
     * @param unitUserName ユニットユーザー名
     * @return DataSourceAccessor
     */
    public static DataSourceAccessor dsa(final String unitUserName) {
        return new DataSourceAccessor(idxUser(unitUserName));
    }

    /**
     * 指定ESインデックス名のDataSourceAccessorを返します.
     * @param indexName ESのインデックス名
     * @return DataSourceAccessor
     */
    public static DataSourceAccessor getDataSourceAccessorFromIndexName(final String indexName) {
        return new DataSourceAccessor(idxUserWithUnitPrefix(indexName));
    }
}

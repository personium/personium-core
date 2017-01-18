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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogWriter;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.model.impl.es.ads.Ads;
import com.fujitsu.dc.core.model.impl.es.ads.AdsConnectionException;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.ads.JdbcAds;

/**
 * Cell削除管理テーブルのアクセス処理を実装したクラス.
 */
public class CellDeleteAccessor {

    static Logger log = LoggerFactory.getLogger(CellDeleteAccessor.class);

    Ads ads;

    /**
     * コンストラクタ.
     */
    public CellDeleteAccessor() {
        try {
            if (DcCoreConfig.getEsAdsType().equals(DcCoreConfig.ES.ADS.TYPE_JDBC)) {
                ads = new JdbcAds();
            } else {
                ads = null;
            }
        } catch (AdsConnectionException e) {
            // 接続エラー時は接続エラーのログを出力する.
            DcCoreLog.Server.ADS_CONNECTION_ERROR.params(e.getMessage()).reason(e).writeLog();
            throw DcCoreException.Server.ADS_CONNECTION_ERROR;
        }
    }

    /**
     * 使用可能な状態かを確認する.
     * @return true:使用可能 false:使用不可
     */
    public boolean isValid() {
        return ads != null;
    }

    /**
     * 管理用DBの作成する.
     */
    public void createManagementDatabase() {
        if (!isValid()) {
            return;
        }
        try {
            ads.createManagementDatabase();
        } catch (AdsException e) {
            // 管理用DBの作成に失敗した場合はログを出力して処理を続行する
            log.warn("Create pcs_management Database to Ads Failed.", e);
        }
    }

    /**
     * 削除対象のDB名とセルIDを追加する.
     * @param dbName DB名
     * @param cellId セルID
     */
    public void insertCellDeleteRecord(String dbName, String cellId) {
        if (!isValid()) {
            return;
        }
        try {
            ads.insertCellDeleteRecord(dbName, cellId);
            log.info("Ads Deletion Success.");
        } catch (AdsException e) {
            // 削除対象のDB名とセルIDの追加に失敗した場合はログを出力して処理を続行する
            log.info(String.format("Insert CELL_DELETE Record To Ads Failed. db_name:[%s], cell_id:[%s]",
                    dbName, cellId), e);

            // Adsの登録に失敗した場合は、専用のログに書込む
            // Cell再帰削除のときは、DB名、Cell IDのみログに書込む
            // ※Cell再帰削除時にAdsに登録する情報としては、他にTable名があるが、ここでは意識しない
            AdsWriteFailureLogWriter adsWriteFailureLogWriter = AdsWriteFailureLogWriter.getInstance(
                    DcCoreConfig.getAdsWriteFailureLogDir(),
                    DcCoreConfig.getCoreVersion(),
                    DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
            AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                    dbName, null, null, null, cellId,
                    AdsWriteFailureLogInfo.OperationKind.PCS_MANAGEMENT_INSERT, 0, 0);
            try {
                adsWriteFailureLogWriter.writeActiveFile(loginfo);
            } catch (AdsWriteFailureLogException e2) {
                DcCoreLog.Server.WRITE_ADS_FAILURE_LOG_ERROR.reason(e2).writeLog();
                DcCoreLog.Server.WRITE_ADS_FAILURE_LOG_INFO.params(loginfo.toString());
            }

        }
    }

}

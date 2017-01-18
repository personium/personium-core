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
package com.fujitsu.dc.core.model.impl.es.repair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AbstractAdsWriteFailureLog;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogFilter;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogWriter;
import com.fujitsu.dc.common.ads.RollingAdsWriteFailureLog;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.response.DcSearchHits;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.common.es.response.EsClientException;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.lock.Lock;
import com.fujitsu.dc.core.model.lock.LockManager;

/**
 * ADSへのデータ補正処理を実装したクラス.
 * <p>
 * 本クラスでは、ADS書き込み失敗ログを読み込み、Elasticsearch上のデータを基準としてADSへのデータ補正を実施する。 データ補正処理は singletonとし、dc1-coreの
 * ScheduleExecutorServiceにて一定間隔で実行する。 なお、本処理の内容は、PCSのAPIとは異なるため、別ファイルとしてログを出力する。
 * </p>
 */
public class RepairAds {

    static Logger logger = LoggerFactory.getLogger(RepairAds.class);

    private static RepairAds singleton;
    static {
        singleton = new RepairAds();
    }
    private String pcsVersion;
    private String adsLogBaseDirPath;
    private File adsLogBaseDir;
    private boolean physicalDelete;
    private int logCountPerIteration;
    // 処理中のADS書き込み失敗ログのファイル名に付加されているタイムスタンプ
    // TODO AdsWriteFailureLogWriterクラスから取得するように見直す。
    private long createTime;

    /**
     * デフォルトコンストラクタ（使用不可）.
     */
    private RepairAds() {
        super();
    }

    /**
     * インスタンスの取得.
     * @return singletonインスタンス
     */
    public static RepairAds getInstance() {
        if (null == singleton) {
            singleton = new RepairAds();
        }
        return singleton;
    }

    /**
     * ADSへのデータ補正を実施するメイン処理.
     * TODO OutOfMemoryErrorが発生した場合、ここで対処しておく必要があるか？
     * （singletonのオブジェクトをクリアするなどの対処）
     * TODO 全般的にエラーが連続して発生する場合のログ出力抑止を検討すること。
     */
    public void repairAds() {

        try {
            logger.info("Ads repair process started.");

            // プロパティ情報の読み込み
            readProperties();

            if (!AdsAccessor.initializedAds()) {
                // MySQLへの初回接続に失敗したため、リペアツールを異常終了させる
                throw new RepairAdsException("Failed to connect MySQL master.");
            }

            // ADS書き込み失敗ログファイルの一覧取得とファイルのソート（created timestamp）
            Map<Long, File> logFilesMap = collectAdsWriteFailureLogFiles();
            if (logFilesMap.isEmpty()) {
                logger.info("AdsWriteFailureLog does not exist. Terminate the repair process.");
                return;
            }

            // リペア処理本体
            // - ADS書き込み失敗ログファイルの読み込み（一定量での読み込み）
            // - リペア有無判定とリペア方法選別
            // - ADSへのリペア（Memcachedへのロックを含む）
            for (Map.Entry<Long, File> map : logFilesMap.entrySet()) {
                File logFile = map.getValue();
                this.createTime = map.getKey();
                logger.info("Starting repair. Repairlog file: " + logFile.toString());
                readAdsWriteFialureLog(logFile);
                logger.info("Finished repair. Repairlog file: " + logFile.toString());
            }

            if (isRepairCompleted()) {
                // INFOログに出力されるとログ情報が膨大なため調査困難と監視が検知できない場合が発生する。このためリペア完了メッセージはERRORレベルで対応する
                logger.error("All ads repair process is completed on " + pcsVersion + ".");
            } else {
                logger.info("Ads repair process is completed. Remained target files will be processed later.");
            }
        } catch (Throwable e) {
            // TODO 同じエラーは抑止しておきたい。
            // - 最後にまとめて失敗したログファイルを出す？
            // - 前回からのエラーも検出したいので、static変数でログ出力可否を判定する？
            logger.error("An error is detected in ads repair process.", e);
        } finally {
            rotateRetryAndErrorLog();
        }
    }

    /**
     * リペア処理中に出力されたリトライ用/エラー用のADS書き込み失敗ログをローテートする.
     */
    private void rotateRetryAndErrorLog() {
        AdsWriteFailureLogWriter errorlog = AdsWriteFailureLogWriter.getInstanceforError(
                adsLogBaseDir.getAbsolutePath(), pcsVersion, DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        AdsWriteFailureLogWriter retrylog = AdsWriteFailureLogWriter.getInstanceforRetry(
                adsLogBaseDir.getAbsolutePath(), pcsVersion, DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        // エラー用ADS書き込み失敗ログをローテートする
        try {
            if (errorlog.isExistsAdsWriteFailureLogs()) {
                errorlog.rotateActiveFile();
            }
        } catch (AdsWriteFailureLogException e1) {
            logger.warn("Failed to rotate errorAdsWriteFailureLog.", e1);
        }
        // リトライ用ADS書き込み失敗ログをローテートする
        try {
            if (retrylog.isExistsAdsWriteFailureLogs()) {
                retrylog.rotateActiveFile();
            }
        } catch (AdsWriteFailureLogException e1) {
            logger.warn("Failed to rotate retryAdsWriteFailureLog.", e1);
        }
    }

    /**
     * プロパティ情報を読み込む.
     */
    private void readProperties() {
        pcsVersion = DcCoreConfig.getCoreVersion();
        adsLogBaseDirPath = DcCoreConfig.getAdsWriteFailureLogDir();
        adsLogBaseDir = new File(adsLogBaseDirPath);
        physicalDelete = DcCoreConfig.getAdsWriteFailureLogPhysicalDelete();
        logCountPerIteration = DcCoreConfig.getAdsWriteFailureLogCountPerIteration();
    }

    /**
     * ファイル作成時刻でソートしたADS書き込み失敗ログのファイル一覧を取得する.
     * @return ADS書き込み失敗ログのファイル一覧
     * @throws RepairAdsException 実行環境に問題が発生した場合
     */
    private Map<Long, File> collectAdsWriteFailureLogFiles() throws RepairAdsException {
        // TreeMapを使い、ファイル名のsuffixについた作成時刻をキーにして自動的にソートされるようにしておく。
        Map<Long, File> logFilesMap = new TreeMap<Long, File>();

        if (!adsLogBaseDir.isDirectory()) {
            String message = String.format("Configuration is wrong, invalid adsWriteFailureLog directory [%s]",
                    adsLogBaseDir.getAbsolutePath());
            throw new RepairAdsException(message);
        }

        File[] logFiles = adsLogBaseDir.listFiles(new AdsWriteFailureLogFilter(pcsVersion));
        for (File logFile : logFiles) {
            // この時点でファイル名フォーマットは正しいのでsuffixからファイルの作成時刻を抽出しMapに格納する。
            String fileNamePrefix = String.format("adsWriteFailure_%s.log.", pcsVersion);
            String createdTimeStr = logFile.getName().replace(fileNamePrefix, "");
            // リトライ用ADS失敗ログの場合はファイル名の最後に「.retry」が付加されているので、削除してファイルの作成時刻だけにする
            createdTimeStr = createdTimeStr.replace(AbstractAdsWriteFailureLog.RETRY_LOGNAME_SUFFIX, "");
            long createdTime = Long.parseLong(createdTimeStr);
            logFilesMap.put(createdTime, logFile);
        }
        return logFilesMap;
    }

    /**
     * エラー用ADS書き込み失敗ログのファイルが存在するかどうかを返却する.
     * @return true baseDir配下にエラー用ADS書き込み失敗ファイルが存在する
     *         false baseDir配下にエラー用ADS書き込み失敗ファイルが存在しない
     * @throws RepairAdsException 実行環境に問題が発生した場合
     */
    private boolean existsErrorLog() throws RepairAdsException {
        if (!adsLogBaseDir.isDirectory()) {
            String message = String.format("Configuration is wrong, invalid adsWriteFailureLog directory [%s]",
                    adsLogBaseDir.getAbsolutePath());
            throw new RepairAdsException(message);
        }

        File[] logFiles = adsLogBaseDir.listFiles();
        for (File logFile : logFiles) {
            if (logFile.getName().endsWith(AbstractAdsWriteFailureLog.ERROR_LOGNAME_SUFFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * アクティブログが存在するかを返却する.
     * @return true baseDir配下アクティブのADS書き込み失敗ファイルが存在する
     *         false baseDir配下にアクティブのADS書き込み失敗ファイルが存在しない
     * @throws RepairAdsException 実行環境に問題が発生した場合
     */
    private boolean existsActiveLog() throws RepairAdsException {
        if (!adsLogBaseDir.isDirectory()) {
            String message = String.format("Configuration is wrong, invalid adsWriteFailureLog directory [%s]",
                    adsLogBaseDir.getAbsolutePath());
            throw new RepairAdsException(message);
        }

        File[] logFiles = adsLogBaseDir.listFiles();
        for (File logFile : logFiles) {
            String regex = "adsWriteFailure_" + pcsVersion + "_\\d{13}.log";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(logFile.getName());
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * ADS書き込み失敗ログを１ファイルずつ読み込み、ADSへデータ補正する.
     * @param logFile ローテートされたADS書き込み失敗ログ
     */
    private void readAdsWriteFialureLog(File logFile) throws RepairAdsException {
        RollingAdsWriteFailureLog adsLog = new RollingAdsWriteFailureLog(
                logFile, adsLogBaseDirPath, pcsVersion, physicalDelete);
        try {
            while (true) {
                // ADS書き込み失敗ログを読み込み、ログ出力情報を作成する。
                List<String> logRecords = adsLog.readAdsFailureLog(logCountPerIteration);
                if (null == logRecords || logRecords.size() == 0) {
                    break;
                }
                List<AdsWriteFailureLogInfo> rowsData = new ArrayList<AdsWriteFailureLogInfo>(logRecords.size());

                for (int i = 0; i < logRecords.size(); i++) {
                    try {
                        // 各行のフォーマットチェック
                        AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(logRecords.get(i));
                        // セル再帰的削除のリペアは未サポートのため退避ログに出力しスキップ
                        if (AdsWriteFailureLogInfo.OperationKind.PCS_MANAGEMENT_INSERT
                        == AdsWriteFailureLogInfo.OperationKind.fromValue(logInfo.getOperation())) {
                            writeAdsErrorLog(logInfo.toString());
                            logger.warn("CellBulkDeletion is not supported by Master Repair.");
                            continue;
                        }
                        rowsData.add(logInfo);
                    } catch (AdsWriteFailureLogException e) {
                        // ADS書き込み失敗ログの内容のフォーマットが不正
                        // 該当行はエラーとし、次の行を読み込む
                        writeAdsErrorLog(logRecords.get(i));
                        logger.error("Failed to parse adsWriteFailureLog.", e);
                    }
                }
                Map<String, List<AdsWriteFailureLogInfo>> mapOfOdataLock =
                        new HashMap<String, List<AdsWriteFailureLogInfo>>();
                // データ書き込み時のOData空間キーごとにADS書き込み失敗ログから読み込んだログ出力情報を集約する。
                // 同じデータに対するログが複数存在する場合は、後勝ちとする。
                for (AdsWriteFailureLogInfo logInfo : rowsData) {
                    String key = logInfo.getIndexName() + "_" + logInfo.getLockKey();
                    updateAdsWriteFailureLogMap(mapOfOdataLock, logInfo, key);
                }
                for (Entry<String, List<AdsWriteFailureLogInfo>> sourceLogInfo : mapOfOdataLock.entrySet()) {
                    executeRepairAds(sourceLogInfo.getValue());
                }
            }
        } catch (AdsWriteFailureLogException e) {
            // ここでエラーとなった場合は、呼び出し元に戻り、次のADS書き込み失敗ログの処理に移行する。
            String message = String.format("Failed to read rotated adsWriteFailureLog. [%s]",
                    logFile.getAbsoluteFile());
            logger.error(message, e);
            // エラー発生のため、ADS出力失敗ログは削除せず、クローズのみとしておく
            adsLog.closeRotatedFile();
            return;
        }
        try {
            // 正常終了のため、ADS出力失敗ログのクローズおよび削除を行う
            adsLog.deleteRotatedLog();
        } catch (AdsWriteFailureLogException e) {
            logger.error("Faild to delete adsWriteFailureLog.", e);
        }
    }

    /**
     * OData空間ごとに管理しているリペア情報を新たに読み込んだ情報で更新する.
     * @param map OData空間ごとに管理しているリペア情報のマップ
     * @param logInfo ログから読み込んだADS書き込み失敗情報
     * @param key mapへアクセスするためのキー
     */
    private void updateAdsWriteFailureLogMap(Map<String, List<AdsWriteFailureLogInfo>> map,
            AdsWriteFailureLogInfo logInfo, String key) {
        List<AdsWriteFailureLogInfo> list = map.get(key);
        if (null == list) {
            list = new ArrayList<AdsWriteFailureLogInfo>();
            list.add(logInfo);
            map.put(key, list);
        } else {
            // OData空間が存在する場合は、リストの中身をUUIDで検索し、検索結果をもとに追加 or 更新する。
            String uuid = logInfo.getUuid();
            int index = 0;
            for (; index < list.size(); index++) {
                AdsWriteFailureLogInfo item = list.get(index);
                if (uuid.equals(item.getUuid())) {
                    break;
                }
            }
            if (index == list.size()) {
                list.add(logInfo);
            } else {
                list.remove(index);
                list.add(logInfo);
            }
        }
    }

    private void executeRepairAds(List<AdsWriteFailureLogInfo> logInfos) throws RepairAdsException {
        // 1件ごとに処理を行う。
        // TODO 性能的な問題が発生した場合は、ｎ件ごとに処理を行うように修正すること。
        for (AdsWriteFailureLogInfo logInfo : logInfos) {
            logger.info("Starting repair. Repair record: " + logInfo.toString());
            String indexName = logInfo.getIndexName();
            String routingId = logInfo.getRoutingId();
            if ("".equals(routingId)) {
                routingId = EsIndex.CELL_ROUTING_KEY_NAME;
            }

            Lock lock = null;
            String lockKey = logInfo.getLockKey();
            try {
                // lockKeyが空(Cell再帰的削除の場合)であれば、ロックしない
                if (null != lockKey && !lockKey.isEmpty()) {
                    try {
                        lock = lock(lockKey);
                    } catch (DcCoreException e) {
                        if (e.getCode().equals(DcCoreException.Server.GET_LOCK_STATE_ERROR.getCode())
                                || e.getCode().equals(DcCoreException.Server.DATA_STORE_UNKNOWN_ERROR.getCode())) {
                            // 全体を異常終了させる
                            throw e;
                        } else {
                            // lockが他のプロセスに取得されており、取得できなかったため、異常終了にはせず、次の行に移行する
                            writeAdsRetryLog(logInfo.toString());
                            logger.info("Other process has a lockObject.");
                            continue;
                        }
                    }
                    if (null == lock) {
                        // 該当行を不正ログファイルに退避する
                        writeAdsErrorLog(logInfo.toString());
                        String message = String.format("Faild to get lock. lockKey= [%s]", lockKey);
                        logger.error(message);
                        continue;
                    }
                }

                List<String> idList = new ArrayList<String>();
                idList.add(logInfo.getUuid());
                try {
                    // TypeがCellである場合は、Elasticsearchのインデックス名を「{UnitPrefix}_ad」に変更する
                    if (Cell.EDM_TYPE_NAME.equals(logInfo.getType())) {
                        indexName = DcCoreConfig.getEsUnitPrefix() + "_" + EsIndex.CATEGORY_AD;
                    }
                    // ES/ADSへの検索は、一括検索ができるようなAPIを使用しているが、今のところは1件ずつ処理することを前提としている。
                    DcSearchResponse esResponse = EsAccessor.search(indexName, routingId, idList, logInfo.getType());
                    List<JSONObject> adsResponse = AdsAccessor.getIdListOnAds(logInfo);
                    repairToAds(logInfo, esResponse, adsResponse);
                } catch (EsClientException e) {
                    String message = String.format("Failed to get response from Elasticsearch. [%s]",
                            logInfo.toString());
                    // Elasticsearchへの検索に失敗したため、ツール全体を異常終了させる
                    throw new RepairAdsException(message, e);
                } catch (AdsException e) {
                    String message = String.format("Failed to get response from Ads. [%s]", logInfo.toString());
                    // MySQLへの検索に失敗したため、ツール全体を異常終了させる
                    throw new RepairAdsException(message, e);
                }
            } finally {
                if (null != lock) {
                    logger.debug("unlock");
                    lock.release();
                }
            }
        }

    }

    /**
     * ロックオブジェクトを取得する.
     * @param lockKey ロックオブジェクトのキー(カテゴリ-UUID)
     * @return ロックオブジェクト or null(ロックのカテゴリが不正である場合)
     */
    private Lock lock(String lockKey) {
        Lock lock = null;
        if (lockKey.startsWith(Lock.CATEGORY_DAV + "-")) {
            // Boxレベルのロックを取得
            String boxId = lockKey.substring((Lock.CATEGORY_DAV + "-").length());
            lock = LockManager.getLock(Lock.CATEGORY_DAV, null, boxId, null);
        } else if (lockKey.startsWith(Lock.CATEGORY_ODATA + "-")) {
            // ODataレベルのロックを取得
            String nodeId = lockKey.substring((Lock.CATEGORY_ODATA + "-").length());
            lock = LockManager.getLock(Lock.CATEGORY_ODATA, null, null, nodeId);
        } else {
            // ロックのカテゴリが不正なため、ロックを取得しない
            String message = String.format("lockKey is wrong. [%s]", lockKey);
            logger.info(message);
        }
        return lock;
    }

    /**
     * リペア対象のデータをADSにリクエストする.
     * @param logInfo ログから読み込んだADS書き込み失敗情報
     * @param esResponse Elasticsearchにリペア対象のデータを検索した結果
     * @param adsResponse ADSにリペア対象のデータを検索した結果
     * @throws RepairAdsException DcRepairAdsException
     */
    public void repairToAds(
            AdsWriteFailureLogInfo logInfo,
            DcSearchResponse esResponse,
            List<JSONObject> adsResponse) throws RepairAdsException {

        // 検索結果として、２件以上がヒットした場合は異常事態とみなし、エラーとする。
        // （現状は、１件ずつ処理しているため、このチェックがある。ｎ件ずつ処理する場合は、ｎ件ヒットで確認する）
        // TODO 件数の数値は、フィールドに追い出す。
        DcSearchHits hits = esResponse.getHits();
        if (hits.getAllPages() > 1) {
            String message =
                    String.format("Unexpected number of data returned from Elasticsearch. [%d]", hits.getAllPages());
            // 2件以上帰ってきた場合は、ツール全体を異常終了させる
            throw new RepairAdsException(message);
        } else if (adsResponse.size() > 1) {
            String message = String.format("Unexpected number of data returned from ads. [id=%s, hits=%d]",
                    logInfo.getUuid(), adsResponse.size());
            // 2件以上帰ってきた場合は、ツール全体を異常終了させる
            throw new RepairAdsException(message);
        }

        try {
            // リペアの判断
            String repairId = logInfo.getUuid();
            String indexName = logInfo.getIndexName();
            String type = logInfo.getType();

            if (hits.getCount() == 1 && adsResponse.size() == 0) {
                // Elasticsearchに存在し、MySQLにデータが存在しない場合
                // MySQLにデータを登録する(Create)
                AdsAccessor.createAds(indexName, type, esResponse);
                logger.info("New recored is inserted into ads. : " + hits.getAt(0).getSource());
            } else if (hits.getCount() == 1 && adsResponse.size() == 1
                    && logInfo.getEsVersion() == hits.getAt(0).getVersion()) {
                // Elasticsearchにデータが存在し、データのバージョンがJournalログのバージョンと同じである場合
                // MySQLにデータを更新する(Update)
                AdsAccessor.updateAds(indexName, type, esResponse);
                logger.info("Ads record is updated : " + hits.getAt(0).getSource());
            } else if (hits.getCount() == 0 && adsResponse.size() == 1) {
                // ES上にデータが存在せず、MySQLにデータが存在する場合
                // MySQLにデータを削除する(Delete)
                AdsAccessor.deleteAds(indexName, type, repairId);
                logger.info("Ads record is deleted from ads. : " + repairId);
            } else {
                // ここに来た場合は、MySQLのデータ更新は無視されたことになる。
                logger.info("No operation performed for repair log recordID : " + repairId);
            }
        } catch (AdsException e) {
            if (e.getCause() instanceof DcCoreException) {
                // 該当行を不正ログファイルに退避する
                // MySQLへのリペア用データの作成に失敗した場合(Elasticsearchから取得したデータのパースに失敗)
                writeAdsErrorLog(logInfo.toString());
                logger.error("Failed to repair record to MySQL. Invalid data is detected in Elasticsearch.", e);
            } else {
                writeAdsRetryLog(logInfo.toString());
                logger.error("Failed to repair record to MySQL.", e);
            }
        }
    }

    /**
     * リペア完了を判定する.
     * リペア完了の判定条件は以下のとおり
     * ADS書き込み失敗ログと出力中のADS書き込み失敗ログが存在しない場合
     * @return リペアが完了していればtrue、完了していない場合はfalse
     * @throws RepairAdsException ベースディレクトリがディレクトリではない場合
     */
    private boolean isRepairCompleted() throws RepairAdsException {
        AdsWriteFailureLogWriter writer = AdsWriteFailureLogWriter.getInstance(
                this.adsLogBaseDir.getAbsolutePath(),
                this.pcsVersion,
                DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        Map<Long, File> logFilesMap = collectAdsWriteFailureLogFiles();
        if (logFilesMap.isEmpty() && !writer.isExistsAdsWriteFailureLogs()
                && !existsErrorLog() && !existsActiveLog()) {
            return true;
        }
        return false;
    }

    /**
     * リトライログにログを出力する.
     * @param logInfo ログ情報
     */
    private void writeAdsRetryLog(String logInfo) {
        AdsWriteFailureLogWriter retryLog = AdsWriteFailureLogWriter.getInstanceforRetry(
                adsLogBaseDir.getPath(), pcsVersion,
                DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        try {
            retryLog.openActiveFile(createTime);
            retryLog.writeActiveFile(logInfo);
        } catch (AdsWriteFailureLogException e) {
            logger.error("Faild to write retry information.", e);
        }
    }

    /**
     * 退避ログにログを出力する.
     * @param logInfo ログ情報
     */
    private void writeAdsErrorLog(String logInfo) {
        AdsWriteFailureLogWriter errorLog = AdsWriteFailureLogWriter.getInstanceforError(
                adsLogBaseDir.getPath(), pcsVersion,
                DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());
        try {
            errorLog.openActiveFile(createTime);
            errorLog.writeActiveFile(logInfo);
        } catch (AdsWriteFailureLogException e) {
            logger.error("Failed to write error information.", e);
        }
    }
}

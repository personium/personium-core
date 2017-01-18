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
package com.fujitsu.dc.core.webcontainer.listener;

import java.io.File;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogWriter;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.repair.RepairAds;

/**
 * Ads書き込み失敗内容をリペアするServiceの定期実行を起動するユーティリティクラス.
 */
public class RepairServiceLauncher {

    private static Logger logger = LoggerFactory.getLogger(RepairServiceLauncher.class);

    /**
     * Adsのリペア処理を行うクラス.
     */
    public static class RepairAdsService implements Runnable {

        /**
         * RepairAdsを起動するか否かを判定するファイルのパス.
         */
        String invocationFlagFile = DcCoreConfig.getRepairAdsInvocationFilePath();

        @Override
        public void run() {
            try {
                AdsWriteFailureLogWriter adsWriteFailureLogWriter = AdsWriteFailureLogWriter.getInstance(
                        DcCoreConfig.getAdsWriteFailureLogDir(),
                        DcCoreConfig.getCoreVersion(),
                        DcCoreConfig.getAdsWriteFailureLogPhysicalDelete());

                // 現在出力中のジャーナルログファイルのローテート。
                adsWriteFailureLogWriter.rotateActiveFile();
            } catch (AdsWriteFailureLogException e) {
                logger.warn("Faield to rotate ads failure log file.");
                return;
            }

            // Adsリペア処理実行の有無チェック.
            logger.debug("Checking invocation flag for RepairAds.");
            if (shouldInvoke()) {
                logger.debug("Invocation flag for RepairAds exists. Invoking RepairAds.");
                // Adsリペア処理の実行.
                try {
                    RepairAds.getInstance().repairAds();
                } catch (Throwable t) {
                    // 例外を飛ばすとそれ以降のスケジュールが無効になるため、外部には飛ばさない。
                    // warnレベルでログを出力したいが、定期的にアラートメールが飛ぶのを防ぐため、infoレベルで出力.
                    logger.info("Ads repair process reported an error.", t);
                }
            } else {
                logger.debug("No invocation flag for RepairAds. Invocation cancelled.");
            }
        }

        /**
         * 特定ファイルの存在有無で、リペア処理の実行必要性の有無を判断する.
         * @return true: 実行が指示されている false: 実行しないように指示されている
         */
        public boolean shouldInvoke() {
            File flagFile = new File(invocationFlagFile);
            return flagFile.exists() && flagFile.isFile();
        }
    }

    ScheduledThreadPoolExecutor executor;

    /**
     * コンストラクタ. Webコンテナ起動時に呼ばれる。
     */
    public RepairServiceLauncher() {
        // 同時起動はしないため、Threadは１つ.
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        // リペアツールの実行をスケジュールする.
        RepairAdsService service = new RepairAdsService();
        executor.scheduleWithFixedDelay(service,
                DcCoreConfig.getAdsRepairInitialDelayInSec(),
                DcCoreConfig.getAdsRepairIntervalInSec(),
                TimeUnit.SECONDS);
        logger.info(String.format("RepairAds scheduled with delay interval %d sec.",
                DcCoreConfig.getAdsRepairIntervalInSec()));
    }

    /**
     * Webコンテナ終了時に呼ばれるメソッド.
     */
    public void shutdown() {
        if (null != executor && !executor.isTerminated()) {
            logger.info("Shutting down RepairAds scheduler.");
            executor.shutdown();
            try {
                long awaitShutdownInSec = DcCoreConfig.getAdsRepairAwaitShutdownInSec();
                logger.info(String.format("Waiting RepairAds termination up to %d sec.", awaitShutdownInSec));
                if (executor.awaitTermination(awaitShutdownInSec, TimeUnit.SECONDS)) {
                    logger.info("Completed shutting down RepairAds scheduler.");
                } else {
                    logger.warn("Shutting down timed out. RepairAds scheduler have not been terminated.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }
}

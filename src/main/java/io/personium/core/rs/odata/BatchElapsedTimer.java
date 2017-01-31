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
package io.personium.core.rs.odata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.rs.odata.ODataBatchResource.BatchPriority;

/**
 * Batchのタイムアウト制御用クラス.
 */
public class BatchElapsedTimer {
    private static Logger log = LoggerFactory.getLogger(BatchElapsedTimer.class);

    private long breakTimeInMillis = 0;
    private long elapseTimeToBreak = 0;
    private long lastSleepTimeStamp;
    private BatchPriority priority = BatchPriority.LOW;

    private long sleep = DcCoreConfig.getOdataBatchSleepInMillis();
    private long sleepInterval = DcCoreConfig.getOdataBatchSleepIntervalInMillis();

    /**
     * Lockを他プロセスに譲るためにスリープするか否かを指定するための列挙型.
     */
    public enum Lock {
        /** Lockを他プロセスに譲るためにスリープする. */
        YIELD,
        /** スリープせずにLockの取得を試みる. */
        HOLD
    }

    /**
     * コンストラクタ.
     * @param startTimeInMillis 処理開始時間.
     * @param elapseTimeToBreakInMillis タイムアウトまでの経過時間.
     * @param priority Lockを他プロセスに譲るためにスリープするか否か
     */
    public BatchElapsedTimer(long startTimeInMillis, long elapseTimeToBreakInMillis, BatchPriority priority) {
        breakTimeInMillis = startTimeInMillis + elapseTimeToBreakInMillis;
        elapseTimeToBreak = elapseTimeToBreakInMillis;
        lastSleepTimeStamp = startTimeInMillis;
        this.priority = priority;
    }

    /**
     * 呼び出し時に、timeoutしているか否かを返す。
     * @param mode Lockを他プロセスに譲るためにスリープするか否か
     * @return true: timeout時間が経過した。false: timeoutしていない。
     */
    public boolean shouldBreak(Lock mode) {
        long current = System.currentTimeMillis();
        if (BatchPriority.LOW == priority && Lock.YIELD.equals(mode)
                && lastSleepTimeStamp + sleepInterval < current) {
            // 前回スリープしてから指定時間経過している場合は、Lockを他プロセスに譲るためにスリープする
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                log.warn("Batch request interrupted.", e);
                throw DcCoreException.Server.UNKNOWN_ERROR;
            }
            current = System.currentTimeMillis();
            lastSleepTimeStamp = current;
        }

        // timeout時間が経過したかを判定
        return breakTimeInMillis < current;
    }

    /**
     * タイムアウト設定値を取得する.
     * @return タイムアウト設定値
     */
    public long getElapseTimeToBreak() {
        return elapseTimeToBreak;
    }

}

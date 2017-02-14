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
package io.personium.core.model.lock;




/**
 * セルLockを管理するユーティリティ.
 */
public abstract class CellLockManager extends LockManager {

    /**
     * セルの処理状態：通常.
     */
    public static final long CELL_STATUS_NORMAL = 0L;
    /**
     * セルの処理状態：一括削除中.
     */
    public static final long CELL_STATUS_BULK_DELETION = 1L;

    abstract Lock getLock(String fullKey);
    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * 参照カウントオブジェクトのプレフィックス.
     */
    public static final String REFERENCE_COUNT_PREFIX = "CellAccessCount_";

    /**
     * セルステータスオブジェクトのプレフィックス.
     */
    public static final String CELL_STATUS_PREFIX = "CellStatus_";

    /**
     * 指定したIDのセルの処理ステータスを返す.
     * @param cellId 処理ステータスを取得する対象のセルID
     * @return 指定したセルの処理ステータス
     */
    public static long getCellStatus(String cellId) {
        String key =  CELL_STATUS_PREFIX + cellId;
        long status = singleton.doGetCellStatus(key);
        if (status < 0) {
            // 存在しない場合は何もしていないので「0:通常」を返す
            status = CELL_STATUS_NORMAL;
        }
        return status;
    }

    /**
     * セルの処理ステータスを「一括削除中」に設定する.
     * @param cellId 対象のセルID
     * @return 設定に成功した場合true、エラーとなった場合はfalse
     */
    public static Boolean setBulkDeletionStatus(String cellId) {
        return setCellStatus(cellId, CELL_STATUS_BULK_DELETION);
    }

    /**
     * セルの処理ステータスを「一括削除中」に設定する.
     * @param cellId 対象のセルID
     * @return 設定に成功した場合true、エラーとなった場合はfalse
     */
    public static Boolean resetBulkDeletionStatus(String cellId) {
        return setCellStatus(cellId, CELL_STATUS_NORMAL);
    }

    /**
     * 指定した初期値でセルの処理ステータスを設定する.
     * ステータスに「0:通常」を指定した場合は設定済みのステータスオブジェクトを削除する.
     * @param cellId 対象のセルID
     * @param customStatus 処理ステータス (0:通常 1:一括削除処理中)
     * @return 設定後の処理ステータス
     */
    private static Boolean setCellStatus(String cellId, long status) {
        String key =  CELL_STATUS_PREFIX + cellId;
        Boolean success = true;
        if (status == CELL_STATUS_NORMAL) {
            // 通常状態に戻す場合はデータ自体を削除する
            singleton.doDeleteCellStatus(key);
        } else {
            success = singleton.doSetCellStatus(key, status);
        }
        return success;
    }

    /**
     * 指定したIDのセルに対する参照カウントを返す.
     * @param cellId 参照カウントを取得する対象のセルID
     * @return 指定したセルの参照カウント
     */
    public static long getReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doGetReferenceCount(key);
        return count;
    }

    /**
     * 指定したセルの参照カウントをインクリメントする.
     * @param cellId 対象のセルID
     * @return インクリメント後の参照カウントの値
     */
    public static long incrementReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doIncrementReferenceCount(key);
        return count;
    }

    /**
     * 指定したセルの参照カウントをデクリメントする.
     * @param cellId 対象のセルID
     * @return デクリメント後の参照カウントの値
     */
    public static long decrementReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doDecrementReferenceCount(key);
        if (count < 0) {
            count = 0;
        }
        return count;
    }
}

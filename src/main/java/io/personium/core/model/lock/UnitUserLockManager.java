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

import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * UnitUserごとのLockを管理するユーティリティ.
 */
public abstract class UnitUserLockManager extends LockManager {

    abstract Lock getLock(String fullKey);

    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * UnitUserごとのデータアクセスをロックするLockカテゴリ.
     */
    public static final String CATEGORY_UNITUSER_LOCK = "UnitUserLock";

    /**
     * UnitUserLockの状態確認.
     * @param unitUserName ユニットユーザ名
     * @return TRUE：Lock／FALSE：Unlock
     */
    public static boolean hasLockObject(final String unitUserName) {
        try {
            String key = CATEGORY_UNITUSER_LOCK + "-" + unitUserName;
            // 対象UnitUserのLock確認
            String lockPublic = singleton.doGetUnituserLock(key);
            return lockPublic != null;
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }

    /**
     * UnitUserLockの書き込み.
     * @param unitUserName ロック対象のユニットユーザ名
     */
    public static void registLockObjct(final String unitUserName) {
        // memcached にキーが存在するか調べる
        // なければmemcached に書きに行く
        // あったら、リトライする。
        int timesRetry = 0;
        while (timesRetry <= lockRetryTimes) {
            String key = LockKeyComposer.fullKeyFromCategoryAndKey(CATEGORY_UNITUSER_LOCK, unitUserName);
            String lock = null;
            try {
                lock = singleton.doGetUnituserLock(key);
            } catch (MemcachedClientException e) {
                MemcachedClient.reportError();
                throw PersoniumCoreException.Server.GET_LOCK_STATE_ERROR;
            }
            if (lock == null) {
                lock = "service mentenance.";
                Boolean success = singleton.doPutUnituserLock(key, lock, 0);
                if (success) {
                    return;
                }
            }
            try {
                Thread.sleep(lockRetryInterval);
            } catch (InterruptedException e) {
                throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
            }
            timesRetry++;
        }
        throw PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;
    }

    /**
     * UnitUserLockのリリース.
     * @param unitUserName ユニットユーザ名
     */
    public static void releaseLockObject(final String unitUserName) {
        String fullKey = LockKeyComposer.fullKeyFromCategoryAndKey(CATEGORY_UNITUSER_LOCK, unitUserName);
        singleton.doReleaseLock(fullKey);
    }
}

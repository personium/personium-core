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
package com.fujitsu.dc.core.model.lock;

import java.util.Date;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Lockを管理するユーティリティ.
 */
public abstract class LockManager {

    static volatile LockManager singleton;
    static volatile String lockType = DcCoreConfig.getLockType();
    static volatile long lockRetryInterval = Long.valueOf(DcCoreConfig.getLockRetryInterval());
    static volatile int lockRetryTimes = Integer.valueOf(DcCoreConfig.getLockRetryTimes());
    static volatile String lockMemcachedHost = DcCoreConfig.getLockMemcachedHost();
    static volatile String lockMemcachedPort = DcCoreConfig.getLockMemcachedPort();
    static volatile int accountLockLifeTime = Integer.valueOf(DcCoreConfig.getAccountLockLifetime());

    /**
     * Memcached タイプ.
     */
    public static final String TYPE_MEMCACHED = "memcached";
    /**
     * InProcess タイプ.
     */
    public static final String TYPE_IN_PROCESS = "inProcess";

    abstract Lock doGetLock(String fullKey);

    abstract Boolean doPutLock(String fullKey, Lock lock);

    abstract void doReleaseLock(String fullKey);

    abstract void doDeleteAllLocks();

    abstract String doGetReferenceOnlyLock(String fullKey);

    abstract Boolean doPutReferenceOnlyLock(String fullKey, String value);

    abstract String doGetAccountLock(String fullKey);

    abstract Boolean doPutAccountLock(String fullKey, String value, int expired);

    abstract String doGetUnituserLock(String fullKey);

    abstract Boolean doPutUnituserLock(String fullKey, String value, int expired);

    abstract long doGetReferenceCount(String fullKey);

    abstract long doIncrementReferenceCount(String fullKey);

    abstract long doDecrementReferenceCount(String fullKey);

    abstract long doGetCellStatus(String fullKey);

    abstract Boolean doSetCellStatus(String fullKey, long status);

    abstract void doDeleteCellStatus(String fullKey);

    abstract String doGetReadDeleteOnlyMode(String fullKey);

    static {
        if (TYPE_MEMCACHED.equals(lockType)) {
            singleton = new MemcachedLockManager();
        } else if (TYPE_IN_PROCESS.equals(lockType)) {
            singleton = new InProcessLockManager();
        }
    }

    /**
     * ロックを取得します.
     * @param category ロックのカテゴリ
     * @param cellId CellのID
     * @param boxId BoxのID
     * @param nodeId NodeのID
     * @return Lock
     */
    public static Lock getLock(String category, String cellId, String boxId, String nodeId) {
        Long createdAt = (new Date()).getTime();
        // memcached にキーが存在するか調べる
        // なければmemcached に書きに行く
        // あったら、リトライする。
        int timesRetry = 0;
        while (timesRetry <= lockRetryTimes) {
            String fullKey = LockKeyComposer.fullKeyFromCategoryAndKey(category, cellId, boxId, nodeId);
            Lock lock = null;
            try {
                lock = singleton.doGetLock(fullKey);
            } catch (MemcachedClientException e) {
                MemcachedClient.reportError();
                throw DcCoreException.Server.GET_LOCK_STATE_ERROR;
            }
            if (lock == null) {
                lock = new Lock(fullKey, createdAt);
                Boolean success = singleton.doPutLock(fullKey, lock);
                if (success) {
                    return lock;
                }
            }
            try {
                Thread.sleep(lockRetryInterval);
            } catch (InterruptedException e) {
                throw DcCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
            }
            timesRetry++;
        }
        throw DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;
    }

    /*
     * ロックのリリース処理
     */
    static void releaseLock(String fullKey) {
        singleton.doReleaseLock(fullKey);
    }


    /**
     * ロックをすべて消します.
     */
    public static void deleteAllLocks() {
        singleton.doDeleteAllLocks();
    }

    /**
     * @return the lockType
     */
    public static final String getLockType() {
        return lockType;
    }

    /**
     * @param lockType the lockType to set
     */
    public static final void setLockType(String lockType) {
        LockManager.lockType = lockType;
    }

    /**
     * @return the lockRetryInterval
     */
    public static final long getLockRetryInterval() {
        return lockRetryInterval;
    }

    /**
     * @param lockRetryInterval the lockRetryInterval to set
     */
    public static final void setLockRetryInterval(long lockRetryInterval) {
        LockManager.lockRetryInterval = lockRetryInterval;
    }

    /**
     * @return the lockRetryTimes
     */
    public static final int getLockRetryTimes() {
        return lockRetryTimes;
    }

    /**
     * @param lockRetryTimes the lockRetryTimes to set
     */
    public static final void setLockRetryTimes(int lockRetryTimes) {
        LockManager.lockRetryTimes = lockRetryTimes;
    }

    /**
     * @return the lockMemcachedHost
     */
    public static final String getLockMemcachedHost() {
        return lockMemcachedHost;
    }

    /**
     * @param lockMemcachedHost the lockMemcachedHost to set
     */
    public static final void setLockMemcachedHost(String lockMemcachedHost) {
        LockManager.lockMemcachedHost = lockMemcachedHost;
    }

    /**
     * @return the lockMemcachedPort
     */
    public static final String getLockMemcachedPort() {
        return lockMemcachedPort;
    }

    /**
     * @param lockMemcachedPort the lockMemcachedPort to set
     */
    public static final void setLockMemcachedPort(String lockMemcachedPort) {
        LockManager.lockMemcachedPort = lockMemcachedPort;
    }

}

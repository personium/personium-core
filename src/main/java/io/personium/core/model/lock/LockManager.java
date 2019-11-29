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

import java.util.Date;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Utility to manage Lock.
 */
public abstract class LockManager {

    static volatile LockManager singleton;
    static volatile String lockType = PersoniumUnitConfig.getLockType();
    static volatile long lockRetryInterval = Long.valueOf(PersoniumUnitConfig.getLockRetryInterval());
    static volatile int lockRetryTimes = Integer.valueOf(PersoniumUnitConfig.getLockRetryTimes());
    static volatile String lockMemcachedHost = PersoniumUnitConfig.getLockMemcachedHost();
    static volatile String lockMemcachedPort = PersoniumUnitConfig.getLockMemcachedPort();
    static volatile int accountValidAuthnInterval = PersoniumUnitConfig.getAccountValidAuthnInterval();
    static volatile int accountLockCount = PersoniumUnitConfig.getAccountLockCount();
    static volatile int accountLockTime = PersoniumUnitConfig.getAccountLockTime();

    /**
     * Memcached type.
     */
    public static final String TYPE_MEMCACHED = "memcached";
    /**
     * InProcess type.
     */
    public static final String TYPE_IN_PROCESS = "inProcess";

    abstract Lock doGetLock(String fullKey);

    abstract Boolean doPutLock(String fullKey, Lock lock);

    abstract void doReleaseLock(String fullKey);

    abstract void doDeleteAllLocks();

    abstract String doGetReferenceOnlyLock(String fullKey);

    abstract Boolean doPutReferenceOnlyLock(String fullKey, String value);

    abstract long doGetAccountLock(String fullKey);

    abstract Boolean doPutAccountLock(String fullKey, long value, int expired);

    abstract long doIncrementAccountLock(String fullKey, int expired);

    abstract void doReleaseAccountLock(String fullKey);

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
     * Get the lock.
     * @param category Category of lock
     * @param cellId Cell ID
     * @param boxId Box ID
     * @param nodeId ID of Node
     * @return Lock
     */
    public static Lock getLock(String category, String cellId, String boxId, String nodeId) {
        Long createdAt = new Date().getTime();
        //Check if memcached has key
        //If not, I will write to memcached
        //If you do, try retrying.
        int timesRetry = 0;
        while (timesRetry <= lockRetryTimes) {
            String fullKey = LockKeyComposer.fullKeyFromCategoryAndKey(category, cellId, boxId, nodeId);
            Lock lock = null;
            try {
                lock = singleton.doGetLock(fullKey);
            } catch (MemcachedClientException e) {
                MemcachedClient.reportError();
                throw PersoniumCoreException.Server.GET_LOCK_STATE_ERROR;
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
                throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
            }
            timesRetry++;
        }
        throw PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;
    }

    /*
     * Lock release processing
     */
    static void releaseLock(String fullKey) {
        singleton.doReleaseLock(fullKey);
    }


    /**
     * Erase all locks.
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
        if (TYPE_MEMCACHED.equals(lockType)) {
            singleton = new MemcachedLockManager();
        } else if (TYPE_IN_PROCESS.equals(lockType)) {
            singleton = new InProcessLockManager();
        }
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

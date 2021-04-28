/**
 * Personium
 * Copyright 2019-2021 Personium Project Authors
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
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Utility to manage Account Lock.
 */
public abstract class AccountLockManager extends LockManager {

    abstract Lock getLock(String fullKey);

    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * Lock category that puts data access for each UnitUser temporarily into reference mode.
     */
    public static final String CATEGORY_ACCOUNT_LOCK = "AccountLock_";

    /**
     * Countup failed count to memcached.
     * Lock time follows properties.
     * @param accountId Account ID that failed authentication
     */
    public static void countupFailedCount(final String accountId) {
        if (accountLockCount == 0 || accountLockTime == 0) {
            return;
        }

        // increment failed count to memcached.
        String key = CATEGORY_ACCOUNT_LOCK + accountId;
        try {
            singleton.doIncrementAccountLock(key, accountLockTime);
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }

    /**
     * Check the account locked.
     * @param accountId account ID
     * @return TRUE: Lock / FALSE: Unlock
     */
    public static boolean isLockedAccount(final String accountId) {
        if (accountLockCount == 0 || accountLockTime == 0) {
            return false;
        }

        long failedCount = getFailedCount(accountId);
        if (failedCount >= accountLockCount) {
            return true;
        }
        return false;
    }

    /**
     * release account lock.
     * @param accountId account ID
     */
    public static void releaseAccountLock(final String accountId) {
        if (accountLockCount == 0 || accountLockTime == 0) {
            return;
        }

        String key = CATEGORY_ACCOUNT_LOCK + accountId;
        try {
            singleton.doReleaseAccountLock(key);
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }

    /**
     * get failed count from memcached.
     * @param accountId account ID
     * @return failed count
     */
    public static long getFailedCount(final String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return 0;
        }
        if (accountLockCount == 0 || accountLockTime == 0) {
            return 0;
        }
        String key = CATEGORY_ACCOUNT_LOCK + accountId;
        try {
            long failedCount = singleton.doGetAccountLock(key);
            return failedCount;
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }
}

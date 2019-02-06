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
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Utility to manage Account valid authentication interval.
 */
public abstract class AccountValidAuthnIntervalLockManager extends LockManager {

    abstract Lock getLock(String fullKey);
    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * Lock category that puts data access for each UnitUser temporarily into reference mode.
     */
    public static final String CATEGORY_ACCOUNT_VALID_AUTHENTICATION_INTERVAL = "AccountValidAuthnInterval_";

    /**
     * Write Account valid authentication interval to memcached.
     * Lock time follows properties.
     * @param accountId Account ID that failed authentication
     */
    public static void registLockObject(final String accountId) {
        String key = CATEGORY_ACCOUNT_VALID_AUTHENTICATION_INTERVAL + accountId;
        if (!singleton.doPutAccountLock(key, 1, accountValidAuthnInterval)) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }

    /**
     * Check the status of Account valid authentication interval.
     * @param accountId account ID
     * @return TRUE: Lock / FALSE: Unlock
     */
    public static boolean hasLockObject(final String accountId) {
        try {
            String key = CATEGORY_ACCOUNT_VALID_AUTHENTICATION_INTERVAL + accountId;
            //Confirm Lock of target account
            Integer lockPublic = singleton.doGetAccountLock(key);
            return lockPublic != null;
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }
}

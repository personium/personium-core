/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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

import java.util.HashMap;
import java.util.Map;

/**
 * InProcess's LockManager.
 */
class InProcessLockManager extends LockManager {
    Map<String, Object> inProcessLock = new HashMap<String, Object>();
    Map<String, AccountLock> inProcessAccountLock = new HashMap<String, AccountLock>();

    @Override
    synchronized Lock doGetLock(String fullKey) {
        return (Lock) inProcessLock.get(fullKey);
    }

    @Override
    synchronized Boolean doPutLock(String fullKey, Lock lock) {
        if (inProcessLock.get(fullKey) == null) {
            inProcessLock.put(fullKey, lock);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    synchronized void doReleaseLock(String fullKey) {
        inProcessLock.remove(fullKey);
    }

    @Override
    synchronized void doDeleteAllLocks() {
        inProcessLock.clear();
        inProcessAccountLock.clear();
    }

    @Override
    synchronized String doGetReferenceOnlyLock(String fullKey) {
        return (String) inProcessLock.get(fullKey);
    }

    @Override
    synchronized Boolean doPutReferenceOnlyLock(String fullKey, String value) {
        if (inProcessLock.get(fullKey) == null) {
            inProcessLock.put(fullKey, value);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    synchronized long doGetAccountLock(String fullKey) {
        Long value = -1L;
        if (inProcessAccountLock.containsKey(fullKey)) {
            AccountLock lock = inProcessAccountLock.get(fullKey);
            value = lock.value();
        }
        return value;
    }

    @Override
    synchronized Boolean doPutAccountLock(String fullKey, long value, int expired) {
        inProcessAccountLock.put(fullKey, new AccountLock(value, expired));
        return Boolean.TRUE;
    }

    @Override
    synchronized long doIncrementAccountLock(String fullKey, int expired) {
        Long value = 1L;
        if (inProcessAccountLock.containsKey(fullKey)) {
            AccountLock lock = inProcessAccountLock.get(fullKey);
            value = lock.value();
            value++;
        }
        inProcessAccountLock.put(fullKey, new AccountLock(value, expired));
        return value;
    }

    @Override
    synchronized void doReleaseAccountLock(String fullKey) {
        inProcessAccountLock.remove(fullKey);
    }

    @Override
    synchronized String doGetUnituserLock(String fullKey) {
        return (String) inProcessLock.get(fullKey);
    }

    @Override
    synchronized Boolean doPutUnituserLock(String fullKey, String value, int expired) {
        if (inProcessLock.get(fullKey) == null) {
            inProcessLock.put(fullKey, value);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    synchronized long doGetReferenceCount(String fullKey) {
        Long value = -1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
        }
        return value;
    }

    @Override
    synchronized long doIncrementReferenceCount(String fullKey) {
        Long value = 1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
            value++;
        }
        inProcessLock.put(fullKey, value);
        return value;
    }

    @Override
    synchronized long doDecrementReferenceCount(String fullKey) {
        Long value = 0L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
            value--;
            inProcessLock.put(fullKey, value);
            if (value == 0) {
                inProcessLock.remove(fullKey);
            }
        }
        return value;
    }

    @Override
    synchronized long doGetCellStatus(String fullKey) {
        Long value = -1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
        }
        return value;
    }

    @Override
    synchronized Boolean doSetCellStatus(String fullKey, long status) {
        inProcessLock.put(fullKey, status);
        return true;
    }

    @Override
    synchronized void doDeleteCellStatus(String fullKey) {
        inProcessLock.remove(fullKey);
    }

    @Override
    synchronized String doGetReadDeleteOnlyMode(String fullKey) {
        String value = null;
        if (inProcessLock.containsKey(fullKey)) {
            value = (String) inProcessLock.get(fullKey);
        }
        return value;
    }

    /**
     * A class that holds information for AccountLock in InProcess.
     */
    static class AccountLock {

        private static final int TIME_MILLIS = 1000;
        private long value;
        private int expiredInSeconds;
        private long createdAt;

        /**
         * constructor.
         * @param value value
         * @param expired Lock retention period (seconds)
         */
        AccountLock(long value, int expired) {
            this.value = value;
            this.expiredInSeconds = expired;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * Return specified AccountLock <br />
         * If expired is exceeded, return null.
         * @return The value corresponding to the specified key (or null if it does not exist, if it exceeds expired)
         */
        public long value() {
            long now = System.currentTimeMillis();

            //If expired is exceeded, return null
            if (now > this.createdAt + expiredInSeconds * TIME_MILLIS) {
                return -1;

            }
            return this.value;
        }
    }
}

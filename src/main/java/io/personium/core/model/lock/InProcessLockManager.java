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

import java.util.HashMap;
import java.util.Map;

/**
 * InProcessのLockManager.
 */
class InProcessLockManager extends LockManager {
    Map<String, Object> inProcessLock = new HashMap<String, Object>();
    Map<String, AccountLock> inProcessAccountLock = new HashMap<String, AccountLock>();

    @Override
    Lock doGetLock(String fullKey) {
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
    void doReleaseLock(String fullKey) {
        inProcessLock.remove(fullKey);
    }

    @Override
    void doDeleteAllLocks() {
        inProcessLock.clear();
    }

    @Override
    String doGetReferenceOnlyLock(String fullKey) {
        return (String) inProcessLock.get(fullKey);
    }

    @Override
    Boolean doPutReferenceOnlyLock(String fullKey, String value) {
        if (inProcessLock.get(fullKey) == null) {
            inProcessLock.put(fullKey, value);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    String doGetAccountLock(String fullKey) {
        AccountLock lock = inProcessAccountLock.get(fullKey);
        if (lock == null) {
            return null;
        }
        return lock.value();
    }

    @Override
    Boolean doPutAccountLock(String fullKey, String value, int expired) {
        inProcessAccountLock.put(fullKey, new AccountLock(value, expired));
        return Boolean.TRUE;
    }

    @Override
    String doGetUnituserLock(String fullKey) {
        return (String) inProcessLock.get(fullKey);
    }

    @Override
    Boolean doPutUnituserLock(String fullKey, String value, int expired) {
        if (inProcessLock.get(fullKey) == null) {
            inProcessLock.put(fullKey, value);
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    @Override
    long doGetReferenceCount(String fullKey) {
        Long value = -1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
        }
        return value;
    }

    @Override
    long doIncrementReferenceCount(String fullKey) {
        Long value = 1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
            value++;
        }
        inProcessLock.put(fullKey, value);
        return value;
    }

    @Override
    long doDecrementReferenceCount(String fullKey) {
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
    long doGetCellStatus(String fullKey) {
        Long value = -1L;
        if (inProcessLock.containsKey(fullKey)) {
            value = (Long) inProcessLock.get(fullKey);
        }
        return value;
    }

    @Override
    Boolean doSetCellStatus(String fullKey, long status) {
        inProcessLock.put(fullKey, status);
        return true;
    }

    @Override
    void doDeleteCellStatus(String fullKey) {
        inProcessLock.remove(fullKey);
    }

    @Override
    String doGetReadDeleteOnlyMode(String fullKey) {
        String value = null;
        if (inProcessLock.containsKey(fullKey)) {
            value = (String) inProcessLock.get(fullKey);
        }
        return value;
    }

    /**
     * InProcessでのAccountLock用の情報を保持するクラス.
     */
    static class AccountLock {

        private static final int TIME_MILLIS = 1000;
        private String value;
        private int expiredInSeconds;
        private long createdAt;

        /**
         * constructor.
         * @param value 値
         * @param expired ロックの保持期間(秒)
         */
        AccountLock(String value, int expired) {
            this.value = value;
            this.expiredInSeconds = expired;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * 指定されたAccountLockを返却する. <br />
         * expiredを超えている場合は、nullを返却する.
         * @return 指定されたキーに対応する値（存在しない場合、expiredを超えている場合はnull）
         */
        public String value() {
            long now = System.currentTimeMillis();

            // expiredを超えている場合は、nullを返却
            if (now > this.createdAt + expiredInSeconds * TIME_MILLIS) {
                return null;

            }
            return this.value;
        }
    }
}

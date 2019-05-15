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

import io.personium.core.utils.MemcachedClient;

/**
 */
class MemcachedLockManager extends LockManager {
    /**
     * Lock for mutateWithDefault. Exclusive control of count increase / decrease for the same key.
     */
    public static final String MUTATE_WITH_DEFAULT_LOCK = "mutateWithDefault_";

    @Override
    Lock doGetLock(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, Lock.class);
    }

    @Override
    Boolean doPutLock(String fullKey, Lock lock) {
        return MemcachedClient.getLockClient().add(fullKey, lock);
    }

    @Override
    void doReleaseLock(String fullKey) {
        MemcachedClient.getLockClient().delete(fullKey);
    }

    @Override
    void doDeleteAllLocks() {
        MemcachedClient.getLockClient().clear();
    }

    @Override
    String doGetReferenceOnlyLock(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, String.class);
    }

    @Override
    Boolean doPutReferenceOnlyLock(String fullKey, String value) {
        return MemcachedClient.getLockClient().add(fullKey, value);
    }

    @Override
    long doGetAccountLock(String fullKey) {
        return MemcachedClient.getLockClient().getLongValue(fullKey);
    }

    @Override
    Boolean doPutAccountLock(String fullKey, long value, int expired) {
        return MemcachedClient.getLockClient().createLongValue(fullKey, value, expired);
    }

    @Override
    long doIncrementAccountLock(String fullKey, int expired) {
        Lock lock = getLock(MUTATE_WITH_DEFAULT_LOCK, null, null, fullKey);
        long value = MemcachedClient.getLockClient().incrementLongValue(fullKey, expired);
        lock.release();
        return value;
    }

    @Override
    void doReleaseAccountLock(String fullKey) {
        MemcachedClient.getLockClient().delete(fullKey);
    }

    @Override
    String doGetUnituserLock(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, String.class);
    }

    @Override
    Boolean doPutUnituserLock(String fullKey, String value, int expired) {
        return MemcachedClient.getLockClient().put(fullKey, expired, value);
    }

    @Override
    long doGetReferenceCount(String fullKey) {
        return MemcachedClient.getLockClient().getLongValue(fullKey);
    }

    @Override
    long doIncrementReferenceCount(String fullKey) {
        Lock lock = getLock(MUTATE_WITH_DEFAULT_LOCK, null, null, fullKey);
        long value = MemcachedClient.getLockClient().incrementLongValue(fullKey);
        lock.release();
        return value;
    }

    @Override
    long doDecrementReferenceCount(String fullKey) {
        Lock lock = getLock(MUTATE_WITH_DEFAULT_LOCK, null, null, fullKey);
        long value = MemcachedClient.getLockClient().decrementLongValue(fullKey);
        lock.release();
        return value;
    }

    @Override
    long doGetCellStatus(String fullKey) {
        Long value = MemcachedClient.getLockClient().get(fullKey, Long.class);
        if (value == null) {
            return -1L;
        }
        return value.longValue();
    }

    @Override
    Boolean doSetCellStatus(String fullKey, long status) {
        MemcachedClient.getLockClient().delete(fullKey);
        return MemcachedClient.getLockClient().add(fullKey, status);
    }

    @Override
    void doDeleteCellStatus(String fullKey) {
        MemcachedClient.getLockClient().delete(fullKey);
    }

    @Override
    String doGetReadDeleteOnlyMode(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, String.class);
    }
}

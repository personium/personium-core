/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import io.personium.core.utils.MemcachedClient;

/**
 */
class MemcachedLockManager extends LockManager {

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
    synchronized long doGetAccountLock(String fullKey) {
        return MemcachedClient.getLockClient().getLongValue(fullKey);
    }

    @Override
    synchronized Boolean doPutAccountLock(String fullKey, long value, int expired) {
        return MemcachedClient.getLockClient().createLongValue(fullKey, value, expired);
    }

    @Override
    synchronized long doIncrementAccountLock(String fullKey, int expired) {
        return MemcachedClient.getLockClient().incrementLongValue(fullKey, expired);
    }

    @Override
    synchronized void doReleaseAccountLock(String fullKey) {
        MemcachedClient.getLockClient().delete(fullKey);
    }

    @Override
    String doGetUnituserLock(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, String.class);
    }

    @Override
    Boolean doPutUnituserLock(String fullKey, String value, int expired) {
        return MemcachedClient.getLockClient().add(fullKey, expired, value);
    }

    @Override
    synchronized long doGetReferenceCount(String fullKey) {
        return MemcachedClient.getLockClient().getLongValue(fullKey);
    }

    @Override
    synchronized long doIncrementReferenceCount(String fullKey) {
        return MemcachedClient.getLockClient().incrementLongValue(fullKey);
    }

    @Override
    synchronized long doDecrementReferenceCount(String fullKey) {
        return MemcachedClient.getLockClient().decrementLongValue(fullKey);
    }

    @Override
    synchronized long doGetCellStatus(String fullKey) {
        return MemcachedClient.getLockClient().getLongValue(fullKey);
    }

    @Override
    synchronized Boolean doSetCellStatus(String fullKey, long status) {
        MemcachedClient.getLockClient().deleteLongValue(fullKey);
        return MemcachedClient.getLockClient().createLongValue(fullKey, status);
    }

    @Override
    synchronized void doDeleteCellStatus(String fullKey) {
        MemcachedClient.getLockClient().deleteLongValue(fullKey);
    }

    @Override
    String doGetReadDeleteOnlyMode(String fullKey) {
        return MemcachedClient.getLockClient().get(fullKey, String.class);
    }
}

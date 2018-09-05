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
package io.personium.core.model.progress;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Class managing asynchronous processing status
 * The storage location of the processing status is the same as that of the LockManager class.
 * @see io.personium.core.model.lock.LockManager
 */
public abstract class ProgressManager {
    static volatile ProgressManager singleton;
    static volatile String storeType = PersoniumUnitConfig.getLockType();
    static volatile long retryInterval = Long.valueOf(PersoniumUnitConfig.getLockRetryInterval());
    static volatile int retryTimes = Integer.valueOf(PersoniumUnitConfig.getLockRetryTimes());
    static volatile String memcachedHost = PersoniumUnitConfig.getLockMemcachedHost();
    static volatile String memcachedPort = PersoniumUnitConfig.getLockMemcachedPort();
    static volatile int lifeTime = Integer.valueOf(PersoniumUnitConfig.getBarInstallProgressLifeTimeExpireInSec());

    /**
     * Memcached type.
     */
    public static final String TYPE_MEMCACHED = "memcached";
    /**
     * InProcess type.
     */
    public static final String TYPE_IN_PROCESS = "inProcess";

    abstract Progress doGetProgress(String fullKey);
    abstract Boolean doPutProgress(String fullKey, Progress progress);
    abstract void doDeleteProgress(String fullKey);
    abstract void doDeleteAllProgress();

    static {
        if (ProgressManager.TYPE_MEMCACHED.equals(storeType)) {
            singleton = new MemcachedProgressManager();
        } else if (ProgressManager.TYPE_IN_PROCESS.equals(storeType)) {
            singleton = new InProcessProgressManager();
        }
    }


    /**
     * Get asynchronous processing status.
     * @ param fullKey Asynchronous processing status key
     * @return Progress
     */
    public static Progress getProgress(String fullKey) {
        try {
            Progress progress = singleton.doGetProgress(fullKey);
            return progress;
        } catch (MemcachedClientException e) {
            MemcachedClient.reportError();
            throw PersoniumCoreException.Server.GET_LOCK_STATE_ERROR;
        }
    }

    /**
     * Asynchronous processing status grading process.
     * @ param key
     * @ param progress Asynchronous processing status object
     */
    public static void putProgress(String key, Progress progress) {
        singleton.doPutProgress(key, progress);
    }

    /**
     * Asynchronous processing status grading process.
     * @ param key
     */
    public static void deleteProgress(String key) {
        singleton.doDeleteProgress(key);
    }

    /**
     * Erase all asynchronous processing status.
     */
    public static void deleteAllProgress() {
        singleton.doDeleteAllProgress();
    }
    /**
     * @return the storeType
     */
    public static final String getStoreType() {
        return storeType;
    }
    /**
     * @param storeType the storeType to set
     */
    public static void setStoreType(String storeType) {
        ProgressManager.storeType = storeType;
    }
    /**
     * @return the retryInterval
     */
    public static final long getRetryInterval() {
        return retryInterval;
    }
    /**
     * @param retryInterval the retryInterval to set
     */
    public static final void setRetryInterval(long retryInterval) {
        ProgressManager.retryInterval = retryInterval;
    }
    /**
     * @return the retryTimes
     */
    public static final int getkRetryTimes() {
        return retryTimes;
    }
    /**
     * @param retryTimes the retryTimes to set
     */
    public static void setRetryTimes(int retryTimes) {
        ProgressManager.retryTimes = retryTimes;
    }
    /**
     * @return the lockMemcachedHost
     */
    public static final String getMemcachedHost() {
        return memcachedHost;
    }
    /**
     * @param memcachedHost the memcachedHost to set
     */
    public static final void setMemcachedHost(String memcachedHost) {
        ProgressManager.memcachedHost = memcachedHost;
    }
    /**
     * @return the memcachedPort
     */
    public static final String getMemcachedPort() {
        return memcachedPort;
    }
    /**
     * @param memcachedPort the memcachedPort to set
     */
    public static final void setMemcachedPort(String memcachedPort) {
        ProgressManager.memcachedPort = memcachedPort;
    }
}

/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;

/**
 * Client that administers Memcached access with this application.
 * It wraps an open source MemcachedClient, and the impact of future library changes is within this class.
 * Because establishing connection of Memcached client takes time, keep Client in class variable of this class,
 * Establish connection at server startup and maintain connection as it is.
 */
public class MemcachedClient implements CacheClient {
    static volatile boolean isReportError = false;
    net.spy.memcached.MemcachedClient spyClient = null;

    private MemcachedClient(String host, String port, long opTimeout) {
        try {
            ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder(new DefaultConnectionFactory());
            //Set memcached timeout time
            cfb.setOpTimeout(opTimeout);

            List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();
            addrs.add(new InetSocketAddress(host, Integer.valueOf(port)));

            this.spyClient = new net.spy.memcached.MemcachedClient(cfb.build(), addrs);
        } catch (NumberFormatException e) {
            PersoniumCoreLog.Server.MEMCACHED_PORT_FORMAT_ERROR.params(e.getMessage()).reason(e).writeLog();
        } catch (IOException e) {
            PersoniumCoreLog.Server.MEMCACHED_CONNECTO_FAIL.params(host, port, e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    private MemcachedClient() {
    }

    /**
     * Get the cache of the specified key.
     * @param <T> Type to get
     * @param key Cache key
     * @param clazz ClassCastExcetpion occurred when there is a type or type problem to get
     * @return When cached object / null cache does not exist
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            T ret = (T) this.spyClient.get(key);
            if (isReportError) {
                isReportError = false;
            }
            return ret;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Cache objects only for a certain expiration date with the specified key.
     * @param key Key of the cache
     * @param expiresIn lifetime
     * @param object Object to cache
     * @return Returns True on successful processing / False on failure.
     */
    public Boolean add(String key, int expiresIn, Object object) {
        try {
            return this.spyClient.add(key, expiresIn, object).get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
        return Boolean.FALSE;
    }

    /**
     * Cache the object with the specified key.
     * @param key Key of the cache
     * @param object Object to cache
     * @return Returns True on successful processing / False on failure.
     */
    public Boolean add(String key, Object object) {
        return this.add(key, 0, object);
    }

    /**
     * Cache objects only for a certain expiration date with the specified key.
     * @param key Key of the cache
     * @param expiresIn lifetime
     * @param object Object to cache
     * @return Returns True on successful processing / False on failure.
     */
    @Override
    public Boolean put(String key, int expiresIn, Object object) {
        try {
            if (!this.spyClient.replace(key, expiresIn, object).get()) {
                if (!this.spyClient.add(key, expiresIn, object).get()) { //NOPMD - To maintain readability
                    // Coping with core issue #35.
                    // Correspondence to failure where processing fails when accessing at the same time.
                    return this.spyClient.replace(key, expiresIn, object).get();
                }
            }
            return true;
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
        return false;
    }

    /**
     * Clear all caches.
     */
    public void clear() {
        try {
            this.spyClient.flush().get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_CLEAR_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_CLEAR_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Deletion of specified key cache.
     * @param key Cache key
     */
    @Override
    public void delete(String key) {
        try {
            this.spyClient.delete(key).get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Create a new object with the specified key.
     * @param key Cache key
     * @param initValue Initial value
     * @return Returns true if creation succeeded or already exists, false if it fails
     */
    public Boolean createLongValue(String key, long initValue) {
        return createLongValue(key, initValue, 0);
    }

    /**
     * Create a new object with the specified key.
     * @param key Cache key
     * @param initValue Initial value
     * @param expiresIn lifetime
     * @return Returns true if creation succeeded or already exists, false if it fails
     */
    public Boolean createLongValue(String key, long initValue, int expiresIn) {
        try {
            long count = this.spyClient.incr(key, 0, initValue, expiresIn);
            if (expiresIn > 0) {
                this.spyClient.touch(key, expiresIn);
            }
            return count == initValue;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Returns the value of the specified key.
     * @param key Cache key
     * @return Specified key value
     */
    public long getLongValue(String key) {
        try {
            //Acquire the current set value by incrementing with increment 0
            return this.spyClient.incr(key, 0);
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Increment the value of the specified key.
     * @param key Cache key
     * @return Value after increment
     */
    public long incrementLongValue(String key) {
        return incrementLongValue(key, 0);
    }

    /**
     * Increment the value of the specified key.
     * @param key Cache key
     * @param expiresIn lifetime
     * @return Value after increment
     */
    public long incrementLongValue(String key, int expiresIn) {
        try {
            long count = this.spyClient.incr(key, 1, 1, expiresIn);
            if (expiresIn > 0) {
                this.spyClient.touch(key, expiresIn);
            }
            return count;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Decrement the value of the specified key.
     * @param key Cache key
     * @return Value after decrementing
     */
    public long decrementLongValue(String key) {
        try {
            long count = this.spyClient.decr(key, 1);
            if (count == 0) {
                delete(key);
            }
            return count;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * Delete the value of the specified key.
     * @param key Cache key
     */
    public void deleteLongValue(String key) {
        delete(key);
    }

    static Logger log = LoggerFactory.getLogger(MemcachedClient.class);

    static {
        if ("memcached".equals(PersoniumUnitConfig.getCacheType())) {
            cacheClient = new MemcachedClient(PersoniumUnitConfig.getCacheMemcachedHost(),
                    PersoniumUnitConfig.getCacheMemcachedPort(),
                    PersoniumUnitConfig.getCacheMemcachedOpTimeout());
        }
        if ("memcached".equals(PersoniumUnitConfig.getLockType())) {
            lockClient = new MemcachedClient(PersoniumUnitConfig.getLockMemcachedHost(),
                    PersoniumUnitConfig.getLockMemcachedPort(),
                    PersoniumUnitConfig.getLockMemcachedOpTimeout());
        }
    }
    /**
     * Client to use for caching.
     */
    static MemcachedClient cacheClient;
    /**
     * Client to use for lock.
     */
    static MemcachedClient lockClient;

    /**
     * @return Client used for caching.
     */
    public static final MemcachedClient getCacheClient() {
        return cacheClient;
    }

    /**
     * @return Client to use for locking.
     */
    public static final MemcachedClient getLockClient() {
        return lockClient;
    }

    /**
     * Determine the value of isReportError and output log.
     */
    public static final void reportError() {
        if (isReportError) {
            log.info("Failed to connect memcached.");
        } else {
            log.error("Failed to connect memcached.");
            isReportError = true;
        }
    }

    /**
     * Exception class for Memcached client.
     */
    @SuppressWarnings("serial")
    public static class MemcachedClientException extends RuntimeException {
        /**
         * constructor.
         * @param cause root exception
         */
        public MemcachedClientException(Throwable cause) {
            super(cause);
        }
    };

}

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
package io.personium.core.model.impl.es.cache;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.CacheClient;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * A class that handles caching of user data schema.
 */
public class UserDataSchemaCache {
    static Logger log = LoggerFactory.getLogger(UserDataSchemaCache.class);
    static MemcachedClient mcdClient = MemcachedClient.getCacheClient();

    static CacheClient getMcdClient() {
        return mcdClient;
    }

    private UserDataSchemaCache() {
    }

    /**
     * Retrieves the schema information from the cache and returns the Map storing the schema information.
     * @param nodeId node ID
     * @return A Map object containing schema information. It is null if it does not exist in the cache
     */
    public static Map<String, Object> get(String nodeId) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = getMcdClient().get(cacheKey(nodeId), Map.class);
            return cache;
        } catch (MemcachedClientException e) {
            //If cache access fails, return null to get data from DB
            log.info("Failed to get UserDataSchemaCache.");
            return null;
        }
    }

    /**
     * Cache the schema information.
     * @param nodeId node ID
     * @param schema Map object containing schema information
     */
    public static void cache(String nodeId, Map<String, Object> schema) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return;
        }
        getMcdClient().put(cacheKey(nodeId), PersoniumUnitConfig.getCacheMemcachedExpiresIn(), schema);
    }

    /**
     * Delete the cache information of the specified schema.
     * @param nodeId node ID
     */
    public static void clear(String nodeId) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return;
        }
        getMcdClient().delete(cacheKey(nodeId));
    }

    /**
     * Invalidate the cache information of the specified schema.
     * @param nodeId node ID
     */
    public static void disable(String nodeId) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return;
        }

        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put("disabledTime", System.currentTimeMillis());
        getMcdClient().put(cacheKey(nodeId), PersoniumUnitConfig.getCacheMemcachedExpiresIn(), schema);
    }

    /**
     * Returns whether cache information is invalidated.
     * @param cache Cache information
     * @return true if disabled, false if enabled
     */
    public static boolean isDisabled(Map<String, Object> cache) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return true;
        }

        if (cache != null && cache.containsKey("disabledTime")) {
            return true;
        }

        return false;
    }

    /**
     * It returns whether there was a change in the cache information.
     * @param nodeId node ID
     * @param cache Original cache information
     * @return true if there was a change, false if there is no change
     */
    @SuppressWarnings("unchecked")
    public static boolean isChanged(String nodeId, Map<String, Object> cache) {
        if (!PersoniumUnitConfig.isSchemaCacheEnabled()) {
            return false;
        }

        Map<String, Object> latestCache = null;
        try {
            latestCache = getMcdClient().get(cacheKey(nodeId), Map.class);
        } catch (MemcachedClientException e) {
            //If cache access fails, return true to get data from the DB
            log.info("Failed to get latest UserDataSchemaCache.");
        }
        if (latestCache == null) {
            return true;
        }

        long disabledTime = 0;
        if (cache.containsKey("disabledTime")) {
            disabledTime = (Long) cache.get("disabledTime");
        }

        long latestDisabledTime = 0;
        if (latestCache.containsKey("disabledTime")) {
            latestDisabledTime = (Long) latestCache.get("disabledTime");
        }

        if (disabledTime != latestDisabledTime) {
            return true;
        }

        return false;
    }

    static String cacheKey(String nodeId) {
        return "userodata:" + nodeId;
    }
}

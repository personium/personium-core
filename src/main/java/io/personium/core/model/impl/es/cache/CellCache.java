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
package io.personium.core.model.impl.es.cache;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.CacheClient;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Class dealing with Cell's cache.
 */
public class CellCache {
    static Logger log = LoggerFactory.getLogger(CellCache.class);

    static MemcachedClient mcdClient = MemcachedClient.getCacheClient();

    private CellCache() {
    }

    static CacheClient getMcdClient() {
        return mcdClient;
    }

    /**
     * Get the Cell information from the cache and return the Map storing the Cell information.
     * @param cellName Cell name
     * @return Map object that stores Cell information. It is null if it does not exist in the cache
     */
    public static Map<String, Object> get(String cellName) {
        if (!PersoniumUnitConfig.isCellCacheEnabled()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = getMcdClient().get(cacheKey(cellName), Map.class);
            return cache;
        } catch (MemcachedClientException e) {
            //If cache access fails, return null to get data from DB
            log.info("Failed to get CellCache.");
            return null;
        }
    }

    /**
     * Cache the Cell.
     * @param cellName Cell name
     * @param cell Map object that stores cell information
     */
    public static void cache(String cellName, Map<String, Object> cell) {
        if (!PersoniumUnitConfig.isCellCacheEnabled()) {
            return;
        }
        getMcdClient().put(cacheKey(cellName), PersoniumUnitConfig.getCacheMemcachedExpiresIn(), cell);
    }

    /**
     * Delete the cache information of the specified Cell name.
     * @param cellName Cell name
     */
    public static void clear(String cellName) {
        if (!PersoniumUnitConfig.isCellCacheEnabled()) {
            return;
        }
        getMcdClient().delete(cacheKey(cellName));
    }

    static String cacheKey(String cellName) {
        return "cell:" + cellName;
    }
}

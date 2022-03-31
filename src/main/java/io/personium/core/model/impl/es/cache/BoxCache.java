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
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.utils.CacheClient;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Class handling Box caching.
 */
public class BoxCache {
    static Logger log = LoggerFactory.getLogger(BoxCache.class);
    private static MemcachedClient mcdClient = MemcachedClient.getCacheClient();

    static CacheClient getMcdClient() {
        return mcdClient;
    }

    private BoxCache() {
    }

    /**
     * Get Box information from cache and return Box object.
     * @param boxName Box name
     * @param cell Cell object
     * @return Box object. It is null if it does not exist in the cache
     */
    public static Box get(String boxName, Cell cell) {
        if (!PersoniumUnitConfig.isBoxCacheEnabled()) {
            return null;
        }

        @SuppressWarnings("rawtypes")
        HashMap obj = null;
        try {
            obj = getMcdClient().get(cacheKey(boxName, cell), HashMap.class);
        } catch (MemcachedClientException e) {
            //If cache access fails, return null to get data from DB
            log.info("Failed to get BoxCache.");
        }
        if (obj == null) {
            return null;
        }

        return new Box(cell,
                (String) obj.get("name"),
                (String) obj.get("schema"),
                (String) obj.get("id"),
                (Long) obj.get("published"));
    }

    /**
     * Cache Box.
     * @param box Box object
     */
    public static void cache(Box box) {
        if (!PersoniumUnitConfig.isBoxCacheEnabled()) {
            return;
        }

        Map<String, Object> obj = new HashMap<String, Object>();
        obj.put("id", box.getId());
        obj.put("name", box.getName());
        obj.put("schema", box.getSchema());
        obj.put("published", box.getPublished());

        getMcdClient().put(cacheKey(box.getName(), box.getCell()),
                PersoniumUnitConfig.getCacheMemcachedExpiresIn(), obj);
    }

    /**
     * Delete the cache information of the specified Box name.
     * @param boxName Box name
     * @param cell Cell object
     */
    public static void clear(String boxName, Cell cell) {
        if (!PersoniumUnitConfig.isBoxCacheEnabled()) {
            return;
        }
        getMcdClient().delete(cacheKey(boxName, cell));
    }

    static String cacheKey(String boxName, Cell cell) {
        return "box:" + cell.getId() + ":" + boxName;
    }
}

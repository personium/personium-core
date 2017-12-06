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
package io.personium.core.model.impl.es.cache;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.utils.CacheClient;
import io.personium.core.utils.MemcachedClient;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Cellのキャッシュを扱うクラス.
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
     * Cell情報をキャッシュから取得し、Cell情報を格納したMapを返す.
     * @param cellName Cell名
     * @return Cell情報を格納したMapオブジェクト。キャッシュに存在しない場合はnull
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
            // キャッシュのアクセスに失敗した場合は、DBからデータを取得させるためnullを返却
            log.info("Failed to get CellCache.");
            return null;
        }
    }

    /**
     * Cellをキャッシュする.
     * @param cellName Cell名
     * @param cell Cell情報を格納したMapオブジェクト
     */
    public static void cache(String cellName, Map<String, Object> cell) {
        if (!PersoniumUnitConfig.isCellCacheEnabled()) {
            return;
        }
        getMcdClient().put(cacheKey(cellName), PersoniumUnitConfig.getCacheMemcachedExpiresIn(), cell);
    }

    /**
     * 指定したCell名のキャッシュ情報を削除する.
     * @param cellName Cell名
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

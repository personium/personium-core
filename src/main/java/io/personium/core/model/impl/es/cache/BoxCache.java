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
 * Boxのキャッシュを扱うクラス.
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
     * Box情報をキャッシュから取得し、Boxオブジェクトを返す.
     * @param boxName Box名
     * @param cell Cellオブジェクト
     * @return Boxオブジェクト。キャッシュに存在しない場合はnull
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
            // キャッシュのアクセスに失敗した場合は、DBからデータを取得させるためnullを返却
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
     * Boxをキャッシュする.
     * @param box Boxオブジェクト
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

        getMcdClient().put(cacheKey(box.getName(), box.getCell()), PersoniumUnitConfig.getCacheMemcachedExpiresIn(), obj);
    }

    /**
     * 指定したBox名のキャッシュ情報を削除する.
     * @param boxName Box名
     * @param cell Cellオブジェクト
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

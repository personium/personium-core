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
package com.fujitsu.dc.core.model.impl.es.cache;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.utils.CacheClient;
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.core.utils.MemcachedClient.MemcachedClientException;

/**
 * ユーザデータスキーマのキャッシュを扱うクラス.
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
     * スキーマ情報をキャッシュから取得し、スキーマ情報を格納したMapを返す.
     * @param nodeId ノードID
     * @return スキーマ情報を格納したMapオブジェクト。キャッシュに存在しない場合はnull
     */
    public static Map<String, Object> get(String nodeId) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> cache = getMcdClient().get(cacheKey(nodeId), Map.class);
            return cache;
        } catch (MemcachedClientException e) {
            // キャッシュのアクセスに失敗した場合は、DBからデータを取得させるためnullを返却
            log.info("Failed to get UserDataSchemaCache.");
            return null;
        }
    }

    /**
     * スキーマ情報をキャッシュする.
     * @param nodeId ノードID
     * @param schema スキーマ情報を格納したMapオブジェクト
     */
    public static void cache(String nodeId, Map<String, Object> schema) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return;
        }
        getMcdClient().put(cacheKey(nodeId), DcCoreConfig.getCacheMemcachedExpiresIn(), schema);
    }

    /**
     * 指定したスキーマのキャッシュ情報を削除する.
     * @param nodeId ノードID
     */
    public static void clear(String nodeId) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return;
        }
        getMcdClient().delete(cacheKey(nodeId));
    }

    /**
     * 指定したスキーマのキャッシュ情報を無効化する.
     * @param nodeId ノードID
     */
    public static void disable(String nodeId) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return;
        }

        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put("disabledTime", System.currentTimeMillis());
        getMcdClient().put(cacheKey(nodeId), DcCoreConfig.getCacheMemcachedExpiresIn(), schema);
    }

    /**
     * キャッシュ情報が無効化されているかを返す.
     * @param cache キャッシュ情報
     * @return 無効の場合はtrue,有効の場合はfalse
     */
    public static boolean isDisabled(Map<String, Object> cache) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return true;
        }

        if (cache != null && cache.containsKey("disabledTime")) {
            return true;
        }

        return false;
    }

    /**
     * キャッシュ情報に変更があったかを返す.
     * @param nodeId ノードID
     * @param cache 元のキャッシュ情報
     * @return 変更があった場合はtrue,変更がない場合はfalse
     */
    @SuppressWarnings("unchecked")
    public static boolean isChanged(String nodeId, Map<String, Object> cache) {
        if (!DcCoreConfig.isSchemaCacheEnabled()) {
            return false;
        }

        Map<String, Object> latestCache = null;
        try {
            latestCache = getMcdClient().get(cacheKey(nodeId), Map.class);
        } catch (MemcachedClientException e) {
            // キャッシュのアクセスに失敗した場合は、DBからデータを取得させるためtrueを返却
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

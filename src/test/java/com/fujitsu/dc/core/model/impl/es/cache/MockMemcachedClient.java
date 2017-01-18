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

import com.fujitsu.dc.core.utils.CacheClient;

/**
 * テスト用のキャッシュクラス.
 */
public class MockMemcachedClient implements CacheClient {

    Map<String, Object> cache = new HashMap<String, Object>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key, Class<T> clazz) {
        return (T) cache.get(key);
    }

    @Override
    public Boolean put(String key, int expiresIn, Object object) {
        cache.put(key, object);
        return true;
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }
}

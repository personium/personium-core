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
package io.personium.core.utils;

/**
 * Interface class for Cache operations.
 */
public interface CacheClient {

    /**
     * Get the cache of the specified key.
     * @ param <T> Type to get
     * @ param key Cache key
     * @ param clazz ClassCastExcetpion occurred when there is a type or type problem to get
     * @return When cached object / null cache does not exist
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * Cache objects only for a certain expiration date with the specified key.
     * @ param key Key of the cache
     * @ param expiresIn lifetime
     * @ param object Object to cache
     * @return Returns True on successful processing / False on failure.
     */
    Boolean put(String key, int expiresIn, Object object);

    /**
     * Deletion of specified key cache.
     * @ param key Cache key
     */
    void delete(String key);

}

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

import io.personium.core.utils.MemcachedClient;

/**
 * Class for accessing memcached asynchronous processing status.
 */
class MemcachedProgressManager extends ProgressManager {

    @Override
    Progress doGetProgress(String fullKey) {
        return MemcachedClient.getCacheClient().get(fullKey, Progress.class);
    }

    @Override
    Boolean doPutProgress(String fullKey, Progress progress) {
        return MemcachedClient.getCacheClient().put(fullKey, lifeTime, progress);
    }

    @Override
    void doDeleteProgress(String fullKey) {
        MemcachedClient.getCacheClient().delete(fullKey);
    }

    @Override
    void doDeleteAllProgress() {
        MemcachedClient.getCacheClient().clear();
    }
}

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
package io.personium.core.model.lock;

import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * PCSの動作モードを参照するクラス.
 */
public abstract class ReadDeleteModeLockManager extends LockManager {

    /**
     * PCSの動作モード(ReadDeleteOnlyModeのmemcached上の格納キー).
     */
    private static final String LOCK_KEY = "PcsReadDeleteMode";

    /**
     * PCSの動作モードの状態確認.
     * @return TRUE：ReadDeleteOnlyモード状態／FALSE：通常状態
     */
    public static boolean isReadDeleteOnlyMode() {
        try {
            String response = singleton.doGetReadDeleteOnlyMode(LOCK_KEY);
            return response != null;
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }
}

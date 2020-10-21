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
package io.personium.core.model.lock;

import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Class referring to PCS operation mode.
 */
public abstract class ReadDeleteModeLockManager extends LockManager {

    /**
     * PCS operation mode (storage key on memcached of ReadDeleteOnlyMode).
     */
    private static final String LOCK_KEY = "PcsReadDeleteMode";

    /**
     * Confirmation of PCS operation mode status.
     * @return TRUE: ReadDeleteOnly mode state / FALSE: normal state
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

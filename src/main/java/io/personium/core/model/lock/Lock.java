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

import java.io.Serializable;

/**
 * Lock object.
 */
public class Lock implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Lock category to use when dealing with OData.
     */
    public static final String CATEGORY_ODATA = "odata";

    /**
     * Lock category to use when dealing with Dav.
     */
    public static final String CATEGORY_DAV = "dav";

    /**
     * Lock category used when handling Cell.
     */
    public static final String CATEGORY_CELL = "Cell";

    /**
     * Lock category that puts data access for each UnitUser temporarily into reference mode.
     */
    public static final String CATEGORY_REFERENCE_ONLY = "referenceOnly";

    /**
     * Lock category to use when dealing with auth history.
     */
    public static final String CATEGORY_AUTH_HISTORY = "authHistory";

    String fullKey;
    Long createdAt;

    /**
     * Constructor (not disclosed).
     * @param key Key of lock
     */
    Lock(String fullKey, Long createdAt) {
        this.fullKey = fullKey;
        this.createdAt = createdAt;
    }

    /**
     * Release the lock.
     */
    public void release() {
        LockManager.releaseLock(this.fullKey);
    }
}

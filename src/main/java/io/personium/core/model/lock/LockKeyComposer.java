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
package io.personium.core.model.lock;

import io.personium.core.PersoniumUnitConfig;

/**
 * Class that generates Lock key.
 */
public class LockKeyComposer {

    static final String KEY_SEPARATOR = "-";

    private LockKeyComposer() {
    }

    /**
     * Generate a key for locking.
     * @param category category name of lock
     * @param key Information that contains the UUID to set as the lock key
     * @return lock key
     */
    public static String fullKeyFromCategoryAndKey(String category, String key) {
        StringBuilder sb = new StringBuilder(category);
        sb.append(KEY_SEPARATOR);
        sb.append(key);
        return sb.toString();
    }

    /**
     * Generate a key for locking.
     * @param category category name of lock
     * @param cellId Cell ID
     * @param boxId Box ID
     * @param nodeId ID of Node
     * @return lock key
     */
    public static String fullKeyFromCategoryAndKey(String category, String cellId, String boxId, String nodeId) {
        return fullKeyFromCategoryAndKey(category, createLockScopeKey(cellId, boxId, nodeId));
    }

    private static String createLockScopeKey(String cellId, String boxId, String nodeId) {
        if (nodeId != null) {
            return nodeId;
        } else if (boxId != null) {
            return boxId;
        } else if (cellId != null) {
            return cellId;
        } else {
            //If CellID is Null, unitPrefix
            return PersoniumUnitConfig.getEsUnitPrefix();
        }
    }

}

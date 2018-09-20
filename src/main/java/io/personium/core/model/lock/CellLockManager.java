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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to manage cell lock.
 */
public abstract class CellLockManager extends LockManager {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(CellLockManager.class);

    /** Map of Cell status with status ID key. */
    private static Map<Long, STATUS> statusMap = new HashMap<>();

    /**
     * Status enum.
     */
    public enum STATUS {
        /** Normal. */
        NORMAL(0L, "normal"),
        /** Bulk deletion. */
        BULK_DELETION(1L, "cell bulk deletion"),
        /** Export. */
        EXPORT(2L, "cell export"),
        /** Import. */
        IMPORT(3L, "cell import");

        /** ID. */
        private long id;
        /** Message. */
        private String message;

        /**
         * Constructor.
         * @param id id
         * @param message message
         */
        STATUS(long id, String message) {
            this.id = id;
            this.message = message;
            statusMap.put(id, this);
        }

        /**
         * Get id.
         * @return id
         */
        public long getId() {
            return id;
        }

        /**
         * Get message.
         * @return message
         */
        public String getMessage() {
            return message;
        }
    }

    abstract Lock getLock(String fullKey);
    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * Prefix of reference count object.
     */
    public static final String REFERENCE_COUNT_PREFIX = "CellAccessCount_";

    /**
     * Cell status object prefix.
     */
    public static final String CELL_STATUS_PREFIX = "CellStatus_";

    /**
     * Returns processing status of cell with the specified ID.
     * @param cellId Target cell id
     * @return Processing status of cell
     */
    public static STATUS getCellStatus(String cellId) {
        String key =  CELL_STATUS_PREFIX + cellId;
        STATUS status = statusMap.get(singleton.doGetCellStatus(key));
        if (status == null) {
            status = STATUS.NORMAL;
        }
        return status;
    }

    /**
     * Set the processing status of the cell with the specified value.
     * When "Normal" is specified as the status, data is deleted from the cache.
     * @param cellId Target cell id
     * @param status processing status
     * @return Processing result
     */
    public static Boolean setCellStatus(String cellId, STATUS status) {
        String key =  CELL_STATUS_PREFIX + cellId;
        Boolean success = true;
        if (STATUS.NORMAL.equals(status)) {
            //When returning to the normal state, delete the data itself
            singleton.doDeleteCellStatus(key);
        } else {
            success = singleton.doSetCellStatus(key, status.getId());
        }
        log.info(String.format("Changed cell lock status. CellID:%s, LockStatus:%s", cellId, status.getMessage()));
        return success;
    }

    /**
     * Returns the reference count for the cell with the specified ID.
     * @param cellId Cell ID for which reference count is to be acquired
     * @return Reference count for the specified cell
     */
    public static long getReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doGetReferenceCount(key);
        return count;
    }

    /**
     * Increment the reference count of the specified cell.
     * @param cellId Target cell ID
     * @return Value of reference count after increment
     */
    public static long incrementReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doIncrementReferenceCount(key);
        return count;
    }

    /**
     * Decrement the reference count of the specified cell.
     * @param cellId Target cell ID
     * @return Value of reference count after decrementing
     */
    public static long decrementReferenceCount(String cellId) {
        String key =  REFERENCE_COUNT_PREFIX + cellId;
        long count = singleton.doDecrementReferenceCount(key);
        if (count < 0) {
            count = 0;
        }
        return count;
    }
}

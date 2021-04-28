/**
 * Personium
 * Copyright 2017-2021 Personium Project Authors
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
package io.personium.core.snapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONObject;

import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressManager;

/**
 * Manage processing status of cell export.
 */
public class SnapshotFileExportProgressInfo {

    /**
     * Status enum.
     */
    enum STATUS {
        /** Ready. */
        READY("ready"),
        /** Processing. */
        PROCESSING("exportation in progress");

        /** Message. */
        private String message;

        /**
         * Constructor.
         * @param message message
         */
        STATUS(String message) {
            this.message = message;
        }

        /**
         * Get message.
         * @return message
         */
        public String value() {
            return message;
        }
    }

    /** Progress max percentage. */
    private static final int PERCENTAGE = 100;
    /** Threshold to update percentage display. */
    private static final int THRESHOLD = 5;
    /** Cache key prefix. */
    // Each process that uses cache defines the Key individually.
    // TODO It is better to have a mechanism that can check Key(prefix) in a list.
    private static final String CACHE_KEY_CATEGORY = "cell-export-";

    /** All processing count. */
    private long entryCount;
    /** Current number of processes. */
    private long progressCount = 0L;
    /** Percent displayed. */
    private int lastPercent = 0;

    /** Target cell id. */
    private String cellId;
    /** Processing start time. */
    private String startTime;
    /** Processing state. */
    private STATUS status;
    /** Snapshot file name(no extension). */
    private String snapshotName;

    /**
     * Get processing cache key.
     * @param cellId Target cell id
     * @return Cache key
     */
    public static String getKey(String cellId) {
        return CACHE_KEY_CATEGORY + cellId;
    }

    /**
     * Creates and returns json in "ready" state.
     * @return json
     */
    @SuppressWarnings("unchecked")
    public static JSONObject getReadyJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", STATUS.READY.value());
        return jsonObject;
    }

    /**
     * Constructor.
     * @param cellId Target cell id.
     * @param snapshotName snapshot file name(no extension)
     * @param entryCount All processing count.
     */
    public SnapshotFileExportProgressInfo(String cellId, String snapshotName, long entryCount) {
        this.cellId = cellId;
        this.snapshotName = snapshotName;
        this.entryCount = entryCount;
        status = STATUS.PROCESSING;
        SimpleDateFormat sdfIso8601ExtendedFormatUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdfIso8601ExtendedFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        startTime = sdfIso8601ExtendedFormatUtc.format(new Date());
    }

    /**
     * Update the progress rate.
     * @param delta The number of cases processed(increment)
     */
    public void addDelta(long delta) {
        progressCount += delta;
    }

    /**
     * Set status.
     * @param status status
     */
    public void setStatus(STATUS status) {
        this.status = status;
    }

    /**
     * Write progress into the cache.
     */
    public void writeToCache() {
        writeToCache(false);
    }

    /**
     * Write progress into the cache.
     * @param forceOutput true : Even if the percentage does not exceed the threshold value, it writes.
     */
    @SuppressWarnings("unchecked")
    public void writeToCache(boolean forceOutput) {
        int progressInPercent = (int) (((double) progressCount  * PERCENTAGE) / entryCount);
        if (!isOutput(progressInPercent) && !forceOutput) {
            return;
        }
        lastPercent = (progressInPercent / THRESHOLD) * THRESHOLD;

        // Create json to write.
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", status.value());
        if (STATUS.PROCESSING.equals(status)) {
            jsonObject.put("started_at", startTime);
            jsonObject.put("progress", progressInPercent + "%");
            jsonObject.put("exportation_name", snapshotName);
        }

        String key = getKey(cellId);
        Progress progress = new Progress(key, jsonObject.toJSONString());
        ProgressManager.putProgress(key, progress);
    }

    /**
     * Delete progress from the cache.
     */
    public void deleteFromCache() {
        String key = getKey(cellId);
        ProgressManager.deleteProgress(key);
    }

    /**
     * Determine if the percentage exceeds the threshold.
     * @param progressInPercent Current Percentage
     * @return true : percentage exceeds the threshold
     */
    private boolean isOutput(int progressInPercent) {
        return progressInPercent - lastPercent > THRESHOLD;
    }

}

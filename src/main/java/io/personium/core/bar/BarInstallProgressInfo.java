/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.bar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONObject;

import io.personium.core.model.progress.ProgressInfo;

/**
 * bar Asynchronous processing status object for installation.
 */
public class BarInstallProgressInfo implements ProgressInfo {

    private static final int PERCENTAGE = 100;
    private static final int THRESHHOLD = 10;
    private static final String PROCESS_NAME = "barInstall";

    private long entryCount;
    private String cellId;
    private String boxId;
    private String startTime;
    private String endTime;
    private STATUS status;
    private JSONObject message;
    private JSONObject jsonObject;

    private long progressCount = 0L;
    private int progressInPercent = 0;
    private int lastPercent = 0;

    /**
     * constructor.
     * @param cellId uuid of Cell
     * @param boxId uuid of Box
     * @param entryCount bar Number of entries (files) in the file
     */
    public BarInstallProgressInfo(String cellId, String boxId, long entryCount) {
        this.cellId = cellId;
        this.boxId = boxId;
        this.entryCount = entryCount;
        this.status = STATUS.PROCESSING;
        SimpleDateFormat sdfIso8601ExtendedFormatUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdfIso8601ExtendedFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.startTime = sdfIso8601ExtendedFormatUtc.format(new Date());
    }

    /**
     * Update the progress rate.
     * @param delta Number of processed files (increment)
     */
    public void addDelta(long delta) {
        this.progressCount += delta;
        this.progressInPercent = (int) (((double) progressCount  * PERCENTAGE) / entryCount);
    }

    /**
     * Whether or not the progress rate can be output as an internal event is judged.
     * The judgment criteria are as follows.
     * <ul>
     * <li> Whether the progress rate exceeds 10% band </ li>
     * </ul>.
     * @return Return true if output is possible, false otherwise.
     */
    public boolean isOutputEventBus() {
       //TODO Updated in increments of 10% may not be updated for a long time, so it will be updated so that it will be updated even after a certain period of time
       if (this.progressInPercent - this.lastPercent > THRESHHOLD) {
           this.lastPercent = (this.progressInPercent / THRESHHOLD) * THRESHHOLD;
           return true;
       }
        return false;
    }

    /**
     * @return the entryCount
     */
    public long getEntryCount() {
        return entryCount;
    }

    /**
     * @param entryCount the entryCount to set
     */
    public void setEntryCount(int entryCount) {
        this.entryCount = entryCount;
    }

    /**
     * @return the process
     */
    public String getProcessName() {
        return PROCESS_NAME;
    }

    /**
     * @return the cellId
     */
    public String getCellId() {
        return cellId;
    }

    /**
     * @return the boxId
     */
    public String getBoxId() {
        return boxId;
    }

    /**
     * @return the startTime
     */
    public String getStartTime() {
        return startTime;
    }
    /**
     * @return the endTime
     */
    public String getEndTime() {
        return endTime;
    }
    /**
     * the endTime to set.
     */
    public void setEndTime() {
        SimpleDateFormat sdfIso8601ExtendedFormatUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdfIso8601ExtendedFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.endTime = sdfIso8601ExtendedFormatUtc.format(new Date());
    }
    /**
     * @return status
     */
    public STATUS getStatus() {
        return status;
    }
    /**
     * @param status STATUS
     */
    public void setStatus(STATUS status) {
        this.status = status;
    }
    /**
     * @return the progress
     */
    public String getProgress() {
        return progressInPercent + "%";
    }
    /**
     * @return the message
     */
    public JSONObject getMessage() {
        return message;
    }
    /**
     * @param message String
     */
    public void setMessage(JSONObject message) {
        this.message = message;
    }

    /**
     * Acquires the contents of stored data in JSON format.
     * @return JSON object.
     */
    @SuppressWarnings("unchecked")
    public JSONObject getJsonObject() {
        JSONObject barInfoJson = null;
        if (this.jsonObject == null) {
            this.jsonObject = new JSONObject();
        }
        this.jsonObject.put("process", getProcessName());
        this.jsonObject.put("barInfo", new JSONObject());
        barInfoJson = (JSONObject) jsonObject.get("barInfo");
        barInfoJson.put("cell_id", getCellId());
        barInfoJson.put("box_id", getBoxId());
        barInfoJson.put("started_at", getStartTime());
        barInfoJson.put("status", getStatus().toString());
        barInfoJson.put("progress", getProgress());
        if (this.status == STATUS.FAILED) {
            barInfoJson.put("message", getMessage());
        }
        return this.jsonObject;
    }

    /**
     * Get the contents of stored data as JSON character string.
     * @return JSON string
     */
    public String toString() {
        return getJsonObject().toJSONString();
    }
}


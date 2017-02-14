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
package io.personium.core.bar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.JSONObject;

import io.personium.core.model.progress.ProgressInfo;

/**
 * barインストール用非同期処理状況オブジェクト.
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
     * コンストラクタ.
     * @param cellId Cellのuuid
     * @param boxId Boxのuuid
     * @param entryCount barファイル内のエントリ（ファイル）数
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
     * 進捗率を更新する.
     * @param delta 処理済みのファイル数（増分）
     */
    public void addDelta(long delta) {
        this.progressCount += delta;
        this.progressInPercent = (int) (((double) progressCount  * PERCENTAGE) / entryCount);
    }

    /**
     * 進捗率を内部イベントとして出力可能かどうかを判定する.
     * 判定基準は以下のとおり.
     * <ul>
     * <li>進捗率が10%帯を超えているかどうか</li>
     * </ul>.
     * @return 出力可能な場合はtrueを、それ以外はfalseを返す。
     */
    public boolean isOutputEventBus() {
       // TODO 10%単位での更新だと長時間更新されない場合があるため、一定時間経過でも更新するように修正
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
     * @return the customStatus
     */
    public STATUS getStatus() {
        return status;
    }
    /**
     * @param customStatus the customStatus to set
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
     * @return the customMessage
     */
    public JSONObject getMessage() {
        return message;
    }
    /**
     * @param customMessage the customMessage to set
     */
    public void setMessage(JSONObject message) {
        this.message = message;
    }

    /**
     * 保存されているデータの内容をJSON形式で取得する.
     * @return JSONオブジェクト.
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
        barInfoJson.put("customStatus", getStatus().toString());
        barInfoJson.put("progress", getProgress());
        if (this.status == STATUS.FAILED) {
            barInfoJson.put("customMessage", getMessage());
        }
        return this.jsonObject;
    }

    /**
     * 保存されているデータの内容をJSON文字列で取得する.
     * @return JSON文字列
     */
    public String toString() {
        return getJsonObject().toJSONString();
    }
}


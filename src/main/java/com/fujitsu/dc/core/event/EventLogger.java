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
package com.fujitsu.dc.core.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.model.Cell;

/**
 * Eventのログ機構.
 */
public class EventLogger {
    int logLevel = 0;
    Cell cell;
    /**
     * コンストラクタ.
     * @param cell Cell
     * @param logLevel log level
     */
    public EventLogger(final Cell cell, final int logLevel) {
        this.cell = cell;
        this.logLevel = logLevel;
    }

    static Logger log = LoggerFactory.getLogger(EventLogger.class);
    /**
     * イベントの情報をログに出力する.
     * @param ev ログに出力するイベント
     */
    public void log(final DcEvent ev) {
        int evLv = ev.getLevel();
        if (evLv >=  this.logLevel) {
            // Do Logging
            if (evLv == DcEvent.Level.ERROR) {
                log.error(cell.getId() + ev.toLogMessage());
            } else if (evLv == DcEvent.Level.WARN) {
                log.warn(cell.getId() + ev.toLogMessage());
            } else if (evLv == DcEvent.Level.INFO) {
                log.info(cell.getId() + ev.toLogMessage());
            }
        }
    }
    /**
     * @return the logLevel
     */
    public int getLogLevel() {
        return logLevel;
    }
    /**
     * @param logLevel the logLevel to set
     */
    public void setLogLevel(final int logLevel) {
        this.logLevel = logLevel;
    }

}

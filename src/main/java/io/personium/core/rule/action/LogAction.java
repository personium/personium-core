/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.rule.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.model.Cell;
import io.personium.core.event.PersoniumEvent;

/**
 * Action for log action.
 */
public class LogAction extends Action {
    /**
     * Definition of log level on LogAction.
     */
    enum LEVEL {
        INFO,
        WARN,
        ERROR
    };

    private static final int IDX_1ST_START = 0;
    private static final int IDX_1ST_END = 2;
    private static final int IDX_2ND_START = 2;
    private static final int IDX_2ND_END = 4;
    private Logger logger;
    private LEVEL level;

    /**
     * Constructor.
     */
    private LogAction() {
        // get Logger per instance
        this.logger = LoggerFactory.getLogger("io.personium.core.rule.action");
    }

    /**
     * Constructor.
     * @param cell target cell object
     * @param level log level
     */
    public LogAction(final Cell cell, LEVEL level) {
        this();
        String unitUserName = getUnitUserName(cell.getOwner());
        String prefix1 = cell.getId().substring(IDX_1ST_START, IDX_1ST_END);
        String prefix2 = cell.getId().substring(IDX_2ND_START, IDX_2ND_END);
        String path = String.format("%s/%s/%s/%s", unitUserName, prefix1, prefix2, cell.getId());

        // set eventlog_path to MDC
        MDC.put("eventlog_path", path);

        this.level = level;
    }

    // clear eventlog_path from MDC
    private void clearEventLogPath() {
        MDC.remove("eventlog_path");
    }

    /**
     * get UnitUserName.
     * @param owner owner of cell
     * @return UnitUserName
     */
    private String getUnitUserName(final String owner) {
        String unitUserName = null;
        if (owner == null) {
            unitUserName = "anon";
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }
        return unitUserName;
    }

    // output log in accordance with log level
    private void outputLog(PersoniumEvent event) {
        if (level == LEVEL.INFO) {
            logger.info(createLogContent(event));
        } else if (level == LEVEL.WARN) {
            logger.warn(createLogContent(event));
        } else if (level == LEVEL.ERROR) {
            logger.error(createLogContent(event));
        }
    }

    /**
     * Output event as log.
     * @param event target event object
     * @return return value is always null
     */
    @Override
    public PersoniumEvent execute(PersoniumEvent event) {
        outputLog(event);
        clearEventLogPath();
        return null;
    }

    /**
     * Output events as log.
     * @param events array of target event object
     * @return return value is always null
     */
    @Override
    public PersoniumEvent execute(PersoniumEvent[] events) {
        for (PersoniumEvent event : events) {
            outputLog(event);
        }
        clearEventLogPath();
        return null;
    }

    // create log from event
    private String createLogContent(PersoniumEvent event) {
        return String.format("%s,%s,%s,%s,%s,%s,%s",
                makeCsvItem(event.getRequestKey()),
                makeCsvItem(event.getExternal().toString()),
                makeCsvItem(event.getSchema()),
                makeCsvItem(event.getSubject()),
                makeCsvItem(event.getType()),
                makeCsvItem(event.getObject()),
                makeCsvItem(event.getInfo()));
    }

    // convert to CSV format
    private String makeCsvItem(String item) {
        if (null == item) {
            return item;
        }
        String replacedItem = item.replaceAll("\"", "\"\"");
        return String.format("\"%s\"", replacedItem);
    }

}
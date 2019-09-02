/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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

import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.model.Cell;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.utils.ResourceUtils;

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
        this.logger = LoggerFactory.getLogger("io.personium.core.rule.action.LogAction");
    }

    /**
     * Constructor.
     * @param cell target cell object
     * @param level log level
     */
    public LogAction(final Cell cell, LEVEL level) {
        this();
        String unitUserName = getUnitUserName(Optional.ofNullable(cell.getOwnerNormalized()));
        String prefix1 = cell.getId().substring(IDX_1ST_START, IDX_1ST_END);
        String prefix2 = cell.getId().substring(IDX_2ND_START, IDX_2ND_END);
        String path = new StringBuilder(unitUserName)
                .append("/")
                .append(prefix1)
                .append("/")
                .append(prefix2)
                .append("/")
                .append(cell.getId())
                .toString();

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
    private String getUnitUserName(final Optional<String> owner) {
        String unitUserName  = owner.map(o -> IndexNameEncoder.encodeEsIndexName(o))
                                    .orElse("anon");
        return unitUserName;
    }

    // output log in accordance with log level
    private void outputLog(PersoniumEvent event, String requestKey) {
        if (level == LEVEL.INFO) {
            logger.info(createLogContent(event, "[INFO ]", requestKey));
        } else if (level == LEVEL.WARN) {
            logger.warn(createLogContent(event, "[WARN ]", requestKey));
        } else if (level == LEVEL.ERROR) {
            logger.error(createLogContent(event, "[ERROR]", requestKey));
        }
    }

    /**
     * Output event as log.
     * @param event target event object
     * @return return value is always null
     */
    @Override
    public PersoniumEvent execute(PersoniumEvent event) {
        String requestKey = event.getRequestKey()
                                 .orElse(ResourceUtils.validateXPersoniumRequestKey(null));
        outputLog(event, requestKey);
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
        Optional<String> commonRequestKey = Optional.empty();

        for (PersoniumEvent event: events) {
            String requestKey = event.getRequestKey()
                                     .orElse(commonRequestKey.orElse(ResourceUtils.validateXPersoniumRequestKey(null)));
            if (!commonRequestKey.isPresent()) {
                commonRequestKey = Optional.ofNullable(requestKey);
            }
            outputLog(event, requestKey);
        }
        clearEventLogPath();
        return null;
    }

    // create log from event
    private String createLogContent(PersoniumEvent event, String levelString, String requestKey) {
        return new StringBuilder(new Date(event.getTime()).toInstant().toString())
                .append(",")
                .append(levelString)
                .append(",")
                .append(makeCsvItem(requestKey))
                .append(",")
                .append(makeCsvItem(event.getExternal().toString()))
                .append(",")
                .append(makeCsvItem(event.getSchema()))
                .append(",")
                .append(makeCsvItem(event.getSubject()))
                .append(",")
                .append(makeCsvItem(event.getType()))
                .append(",")
                .append(makeCsvItem(event.getObject()))
                .append(",")
                .append(makeCsvItem(event.getInfo()))
                .toString();
    }

    private String makeCsvItem(Optional<String> item) {
        return item.map(i -> makeCsvItem(i)).orElse(null);
    }

    // convert to CSV format
    private String makeCsvItem(String item) {
        String replacedItem = item.replaceAll("\"", "\"\"");
        return new StringBuilder("\"")
                .append(replacedItem)
                .append("\"")
                .toString();
    }

}

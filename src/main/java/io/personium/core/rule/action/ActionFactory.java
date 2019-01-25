/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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

import io.personium.core.model.Cell;
import io.personium.core.model.ctl.Rule;

import io.personium.core.rule.ActionInfo;

/**
 * Factory class for Action.
 */
public class ActionFactory {
    private ActionFactory() {
    }

    /**
     * Create Action in accordance with the action.
     * @param cell target cell object
     * @param ai target ActionInfo object
     * @return created Action
     */
    public static Action createAction(Cell cell, ActionInfo ai) {
        String action = ai.getAction();
        if (Rule.ACTION_LOG.equals(action) || Rule.ACTION_LOG_INFO.equals(action)) {
            return new LogAction(cell, LogAction.LEVEL.INFO);
        } else if (Rule.ACTION_LOG_WARN.equals(action)) {
            return new LogAction(cell, LogAction.LEVEL.WARN);
        } else if (Rule.ACTION_LOG_ERROR.equals(action)) {
            return new LogAction(cell, LogAction.LEVEL.ERROR);
        } else if (Rule.ACTION_EXEC.equals(action)) {
            return new ExecAction(cell, ai);
        } else if (Rule.ACTION_RELAY.equals(action)) {
            return new RelayAction(cell, ai);
        } else if (Rule.ACTION_RELAY_EVENT.equals(action)) {
            return new RelayEventAction(cell, ai);
        } else if (Rule.ACTION_RELAY_DATA.equals(action)) {
            return new RelayDataAction(cell, ai);
        }
        return null;
    }
}

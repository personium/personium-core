/**
 * Personium
 * Copyright 2017-2020 Personium Project Authors
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
package io.personium.core.rule;

import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.Cell;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rule.action.Action;
import io.personium.core.rule.action.ActionFactory;

/**
 * Runnable class for Action.
 */
class ActionRunner implements Runnable {
    private Cell cell;
    private ActionInfo actionInfo;
    private PersoniumEvent event;

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai information of action for target event
     * @param event target event object
     */
    ActionRunner(Cell cell, ActionInfo ai, PersoniumEvent event) {
        this.cell = cell;
        this.actionInfo = ai;
        this.event = event;

        inc();
    }

    // increment cell reference count
    private void inc() {
        CellLockManager.incrementReferenceCount(cell.getId());
    }

    // decrement cell reference count
    private void dec() {
        CellLockManager.decrementReferenceCount(cell.getId());
    }

    @Override
    public void run() {
        Action action = ActionFactory.createAction(cell, actionInfo);
        if (action != null) {
            // execute action
            PersoniumEvent evt = action.execute(event);
            // if evt exists, output event and evt
            if (evt != null) {
                PersoniumEvent[] events = {
                        event, evt
                };
                ActionInfo ai = new ActionInfo(Rule.ACTION_LOG_INFO);
                Action log = ActionFactory.createAction(cell, ai);
                log.execute(events);
            }
        }

        dec();
    }
}

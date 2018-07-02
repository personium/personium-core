/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import org.json.simple.JSONObject;

import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Action for relay.event action.
 */
public class RelayEventAction extends RelayAction {
    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public RelayEventAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
    }

    @Override
    protected String getRequestUrl() {
        return service + "__event";
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    protected JSONObject createEvent(PersoniumEvent event) {
        String type = event.getType();
        if (type == null) {
            type = "";
        }
        if (!type.startsWith("relay.")) {
            if (event.getExternal()) {
                type = "relay.ext." + type;
            } else {
                type = "relay." + type;
            }
        }

        JSONObject json = new JSONObject();
        json.put("Type", type);
        json.put("Object", event.getObject());
        json.put("Info", event.getInfo());

        return json;
    }

    @Override
    protected String getVia(PersoniumEvent event) {
        // set X-Personium-Via header
        String via = event.getVia();
        if (via == null) {
            via = cell.getUrl();
        } else {
            via = via + "," + cell.getUrl();
        }
        return via;
    }

    @Override
    protected String getTargetCellUrl() {
        return service;
    }
}


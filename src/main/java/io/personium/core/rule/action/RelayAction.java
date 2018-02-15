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

import org.apache.http.HttpMessage;
import org.json.simple.JSONObject;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Action for relay action.
 */
public class RelayAction extends EngineAction {
    private static String boxName = "__";

    /** System script name for relay. */
    private String svcName;

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public RelayAction(Cell cell, ActionInfo ai) {
        super(cell, ai);
        this.svcName = "relay";
    }

    /**
     * Set svcName.
     * @param svcName system script name
     */
    protected void setSvcName(String svcName) {
        this.svcName = svcName;
    }

    @Override
    protected String getRequestUrl() {
        if (cell == null) {
            return null;
        }
        String cellName = cell.getName();
        String requestUrl = String.format("http://%s:%s/%s/%s/%s/system/%s", PersoniumUnitConfig.getEngineHost(),
                PersoniumUnitConfig.getEnginePort(), PersoniumUnitConfig.getEnginePath(), cellName, boxName, svcName);
        return requestUrl;
    }

    @Override
    protected void setHeaders(HttpMessage req, PersoniumEvent event) {
        if (cell == null || req == null) {
            return;
        }
        req.addHeader("X-Baseurl", cell.getUnitUrl());
        req.addHeader("X-Request-Uri", cell.getUrl() + boxName + "/" + svcName);
        if (event.getSchema() != null) {
            req.addHeader("X-Personium-Box-Schema", event.getSchema());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addEvents(JSONObject json) {
        if (json == null) {
             return;
        }
        json.put("TargetUrl", service);
    }
}


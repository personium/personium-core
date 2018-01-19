/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
import io.personium.core.model.Cell;

/**
 * Action for relay action.
 */
public class RelayAction extends EngineAction {
    private static String boxName = "__";
    private static String svcName = "proxy";

    /**
     * Constructor.
     * @param cell target cell object
     * @param extservice the url that HTTP POST will be sent
     */
    public RelayAction(Cell cell, String extservice) {
        super(cell, extservice, "relay");
    }

    @Override
    protected String getRequestUrl() {
        if (cell == null) {
            return null;
        }
        String cellName = cell.getName();
        String requestUrl = String.format("http://%s:%s/%s/%s/%s/system/%s", PersoniumUnitConfig.getEngineHost(),
            PersoniumUnitConfig.getEnginePort(), PersoniumUnitConfig.getEnginePath(), cellName, boxName, "proxy");
        return requestUrl;
    }

    @Override
    protected void setHeaders(HttpMessage req) {
        if (cell == null || req == null) {
            return;
        }
        req.addHeader("X-Baseurl", cell.getUnitUrl());
        req.addHeader("X-Request-Uri", cell.getUrl() + boxName + "/" + svcName);
        req.addHeader("X-Personium-Box-Schema", null);
    }

    @Override
    protected void addEvents(JSONObject json) {
        if (json == null) {
             return;
        }
        json.put("service", service);
    }
}


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

import java.net.URL;

import org.apache.http.HttpMessage;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.impl.fs.DavCmpFsImpl;

/**
 * Action for exec action.
 */
public class ExecAction extends EngineAction {
    static Logger logger = LoggerFactory.getLogger(ExecAction.class);

    // path is expected as followe: /cell/box/col/service
    //   parts[0]: ""
    //   parts[1]: "cell"
    //   parts[2]: "box"
    //   parts[3]: "col"
    //   parts[4]: "service"
    static final int PATH_SPLIT_NUMBER = 5;
    static final int INDEX_CELLNAME = 1;
    static final int INDEX_BOXNAME = 2;
    static final int INDEX_COLNAME = 3;
    static final int INDEX_SVCNAME = 4;

    private String cellName;
    private String boxName;
    private String colName;
    private String svcName;

    /**
     * Constructor.
     * @param cell target cell object
     * @param service the url that HTTP POST will be sent
     */
    public ExecAction(Cell cell, String service) {
        super(cell, service, "exec");
    }

    @Override
    protected String getRequestUrl() {
        URL url;
        try {
            url = new URL(service);
        } catch (Exception e) {
            logger.error("invalid service url: " + service);
            return null;
        }

        String path = url.getPath();
        String[] parts = path.split("/");
        if (parts.length != PATH_SPLIT_NUMBER) {
            logger.error("incorrect service url: " + service);
            return null;
        }
        this.cellName = parts[INDEX_CELLNAME];
        this.boxName = parts[INDEX_BOXNAME];
        this.colName = parts[INDEX_COLNAME];
        this.svcName = parts[INDEX_SVCNAME];
        String requestUrl = String.format("http://%s:%s/%s/%s/%s/service/%s",
                PersoniumUnitConfig.getEngineHost(),
                PersoniumUnitConfig.getEnginePort(),
                PersoniumUnitConfig.getEnginePath(),
                this.cellName, this.boxName, this.svcName);

        return requestUrl;
    }

    @Override
    protected void addEvents(JSONObject json) {
        // There is no event to be added.
    }


    @Override
    protected void setHeaders(HttpMessage req) {
        if (cell == null || req == null) {
            return;
        }
        // set headers for Engine
        //   X-Baseurl, X-Request-Uri, X-Personium-Fs-Path, X-Personium-Fs-Routing-Id, X-Personium-Box-Schema
        req.addHeader("X-Baseurl", cell.getUnitUrl());
        req.addHeader("X-Request-Uri", service);
        try {
            if (cell.getName().equals(this.cellName)) {
                // box
                Box box = cell.getBoxForName(this.boxName);
                BoxCmp davCmp = ModelFactory.boxCmp(box);

                // collection
                DavCmp nextCmp = davCmp.getChild(this.colName);
                String type = nextCmp.getType();
                if (DavCmp.TYPE_COL_SVC.equals(type)) {
                    if (nextCmp instanceof DavCmpFsImpl) {
                        DavCmpFsImpl dcmp = (DavCmpFsImpl) nextCmp;
                        req.addHeader("X-Personium-Fs-Path", dcmp.getFsPath());
                        req.addHeader("X-Personium-Fs-Routing-Id", dcmp.getCellId());
                    }
                }
                req.addHeader("X-Personium-Box-Schema", box.getSchema());
            }
        } catch (Exception e) {
            logger.error("error: " + e.getMessage(), e);
            // ignore error, continue
        }
    }
}


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
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;

/**
 * Abstract class of Action about Engine.
 */
public abstract class EngineAction extends Action {
    static Logger logger = LoggerFactory.getLogger(EngineAction.class);

    Cell cell;
    String service;
    String action;

    /**
     * Constructor.
     * @param cell target cell object
     * @param service the url that HTTP POST will be sent
     * @param action 'relay' or 'exec'
     */
    public EngineAction(Cell cell, String service, String action) {
        this.cell = cell;
        this.service = service;
        this.action = action;
    }

    @Override
    public PersoniumEvent execute(PersoniumEvent event) {
        String requestUrl = getRequestUrl();
        if (requestUrl == null) {
            return null;
        }

        HttpClient client = new DefaultHttpClient();
        HttpPost req = new HttpPost(requestUrl);

        // create payload as JSON
        JSONObject json = new JSONObject();
        json.put("evt_external", event.getExternal());
        json.put("evt_requestkey", event.getRequestKey());
        if (event.getSchema() != null) {
            json.put("evt_schema", event.getSchema());
        }
        if (event.getSubject() != null) {
            json.put("evt_subject", event.getSubject());
        }
        json.put("evt_type", event.getType());
        json.put("evt_object", event.getObject());

        // add specific events to payload in derrived class
        addEvents(json);

        req.setEntity(new StringEntity(json.toString(), ContentType.create("application/json")));

        // set specific headers in derrived class
        setHeaders(req);

        HttpResponse objResponse = null;
        try {
            objResponse = client.execute(req);
            logger.info(EntityUtils.toString(objResponse.getEntity()));
        } catch (ClientProtocolException e) {
            logger.error("Invalid Http response: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Connection Error: " + e.getMessage(), e);
            return null;
        }

        String result;
        if (objResponse != null) {
            result = Integer.toString(objResponse.getStatusLine().getStatusCode());
        } else {
            result = "404";
        }

        PersoniumEvent evt = new PersoniumEvent(null, null, action, service, result, event.getRequestKey());
        evt.setCellId(cell.getId());

        return evt;
    }

    @Override
    public PersoniumEvent execute(PersoniumEvent[] events) {
        // not supported
        return null;
    }

    /**
     * Create request url in derrived class.
     * @return created request url, null if error is occurred
     */
    protected abstract String getRequestUrl();

    /**
     * Add specific events in derrived class.
     * @param json JSONObject to add events
     */
    protected abstract void addEvents(JSONObject json);

    /**
     * Set specific HTTP headers in derrived class.
     * @param req Request to set headers
     */
    protected abstract void setHeaders(HttpMessage req);
}


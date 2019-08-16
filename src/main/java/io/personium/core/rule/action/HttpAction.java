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

import java.util.Optional;

import org.apache.http.HttpMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.CommonUtils;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Cell;
import io.personium.core.rule.ActionInfo;

/**
 * Abstract class of Action with Http request.
 */
public abstract class HttpAction extends Action {
    static Logger logger = LoggerFactory.getLogger(HttpAction.class);

    Cell cell;
    String service;
    String action;
    String eventId;
    String chain;

    /**
     * Constructor.
     * @param cell target cell object
     * @param ai ActionInfo object
     */
    public HttpAction(Cell cell, ActionInfo ai) {
        this.cell = cell;
        this.service = ai.getService();
        this.action = ai.getAction();
        this.eventId = ai.getEventId();
        this.chain = ai.getRuleChain();
    }

    /**
     * Set common headers.
     * @param req HttpMessage object
     * @param event PersoniumEvent object
     */
    protected void setCommonHeaders(HttpMessage req, PersoniumEvent event) {
        // set common headers
        //  X-Personium-RequestKey, X-Personium-EventId, X-Personium-RuleChain, X-Personium-Via
        event.getRequestKey().ifPresent(requestKey ->
                                  req.addHeader(CommonUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY,
                                                requestKey));
        req.addHeader(CommonUtils.HttpHeaders.X_PERSONIUM_EVENTID, eventId);
        req.addHeader(CommonUtils.HttpHeaders.X_PERSONIUM_RULECHAIN, chain);
        getVia(event).ifPresent(via -> req.addHeader(CommonUtils.HttpHeaders.X_PERSONIUM_VIA, via));
    }

    @Override
    public PersoniumEvent execute(PersoniumEvent[] events) {
        // not supported
        return null;
    }

    /**
     * Get Via header string.
     * @param event PersoniumEvent object
     * @return via header string
     */
    protected Optional<String> getVia(PersoniumEvent event) {
        return event.getVia();
    }

}

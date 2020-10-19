/**
 * Personium
 * Copyright 2018-2020 Personium Project Authors
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
package io.personium.core.ws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.EventFactory;
import io.personium.core.event.EventSubscriber;
import io.personium.core.event.PersoniumEvent;

/**
 * Runnable class for receiving broadcasted event.
 */
class EventSubscribeRunner implements Runnable {
    static Logger log = LoggerFactory.getLogger(EventSubscribeRunner.class);

    @Override
    public void run() {
        try {
            EventSubscriber subscriber = EventFactory.createEventSubscriber();

            while (!Thread.interrupted()) {
                try {
                    List<PersoniumEvent> list = subscriber.receive();
                    if (list == null) {
                        break;
                    }
                    for (PersoniumEvent event : list) {
                        if (event != null) {
                            log.debug("Received MapMessage with '" + event.getType() + "'.");
                            log.debug("    External: " + event.getExternal());
                            log.debug("    Schema: " + event.getSchema());
                            log.debug("    Subject: " + event.getSubject());
                            log.debug("    Type: " + event.getType());
                            log.debug("    Object: " + event.getObject());
                            log.debug("    Info: " + event.getInfo());
                            log.debug("    RequestKey: " + event.getRequestKey());
                            log.debug("    EventId: " + event.getEventId());
                            log.debug("    RuleChain: " + event.getRuleChain());
                            log.debug("    Via: " + event.getVia());
                            log.debug("    CellId: " + event.getCellId());
                            log.debug("    Time: " + event.getTime());

                            // WebSocket send
                            WebSocketService.sendEvent(event);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception occurreed: " + e.getMessage(), e);
                }
            }
            subscriber.unsubscribe();
        } catch (Exception e) {
            log.error("Exception occurred: " + e.getMessage(), e);
        }
    }

}


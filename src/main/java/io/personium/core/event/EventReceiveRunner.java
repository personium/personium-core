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
package io.personium.core.event;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.rule.RuleManager;

/**
 * Runnable class for receiving PersoniumEvent.
 * The received event is processed on RuleManager.
 */
class EventReceiveRunner implements Runnable {
    private static Logger log = LoggerFactory.getLogger(EventReceiveRunner.class);

    @Override
    public void run() {
        try {
            EventReceiver receiver = EventFactory.createEventReceiver();

            // Get RuleManager's instance.
            RuleManager rman = RuleManager.getInstance();

            while (!Thread.interrupted()) {
                try {
                    List<PersoniumEvent> list = receiver.receive();
                    if (list == null) {
                        // if list is null, can't continue.
                        break;
                    }
                    for (PersoniumEvent event : list) {
                        if (event != null) {
                            log.debug("Received Message with '" + event.getType() + " '.");
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
                            log.debug("    Roles: " + event.getRoles());
                            log.debug("    CellId: " + event.getCellId());
                            log.debug("    Time: " + event.getTime());

                            // If the event matches with a rule, execute action of the rule.
                            rman.judge(event);

                            // publish event
                            EventFactory.getEventPublisher().send(event);
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception occurred: " + e.getMessage(), e);
                }
            }
            receiver.unsubscribe();
        } catch (Exception e) {
            log.error("EventReceiver failed: " + e.getMessage(), e);
        }
    }

}


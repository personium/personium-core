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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.EventFactory;
import io.personium.core.event.EventSubscriber;

/**
 * Runnable class for receiving events about rule.
 */
class RuleEventSubscribeRunner implements Runnable {
    static Logger log = LoggerFactory.getLogger(RuleEventSubscribeRunner.class);

    @Override
    public void run() {
        try {
            EventSubscriber subscriber =
                    EventFactory.createEventSubscriber(PersoniumUnitConfig.getEventBusRuleTopicName());

            // Get RuleManager instance.
            RuleManager rman = RuleManager.getInstance();

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
                            log.debug("    Type: " + event.getType());
                            log.debug("    Object: " + event.getObject());
                            log.debug("    Info: " + event.getInfo());
                            log.debug("    CellId: " + event.getCellId());
                            log.debug("    Time: " + event.getTime());

                            // Register or unregister rule in accordance with event about rule.
                            rman.handleRuleEvent(event);
                        } else {
                            log.info("Received Message null");
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception occurred: " + e.getMessage(), e);
                }
            }
            subscriber.unsubscribe();
        } catch (Exception e) {
            log.error("Exception occurred: " + e.getMessage(), e);
        }
    }

}


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
package io.personium.core.rule;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.EventSender;

/**
 * Runnable class for receiving PersoniumEvent.
 * The received event is processed on RuleManager.
 */
class EventReceiver implements Runnable {
    private static Logger log = LoggerFactory.getLogger(EventReceiver.class);

    @Override
    public void run() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(PersoniumUnitConfig.getEventBusBrokerUrl());
            Connection connection = factory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(PersoniumUnitConfig.getEventBusQueueName());
            MessageConsumer receiver = session.createConsumer(queue);
            connection.start();
            log.debug("Listen JMS messages ...");

            // Get RuleManager's instance.
            RuleManager rman = RuleManager.getInstance();

            while (!Thread.interrupted()) {
                try {
                    Message m = receiver.receive();
                    PersoniumEvent event = EventSender.convertToEvent(m);
                    if (event != null) {
                        log.debug(String.format("Received MapMessage with '%s'.", event.getType()));
                        log.debug("    External: " + event.getExternal());
                        log.debug("    Schema: " + event.getSchema());
                        log.debug("    Subject: " + event.getSubject());
                        log.debug("    Type: " + event.getType());
                        log.debug("    Object: " + event.getObject());
                        log.debug("    Info: " + event.getInfo());
                        log.debug("    RequestKey: " + event.getRequestKey());
                        log.debug("    CellId: " + event.getCellId());

                        // If the event matches with a rule, execute action of the rule.
                        rman.judge(event);
                    } else {
                        log.info("Received Message is null");
                    }
                } catch (JMSException e) {
                    Exception exp = e.getLinkedException();
                    if (exp instanceof InterruptedException) {
                        log.debug("Interrupted");
                        break;
                    } else {
                        throw e;
                    }
                } catch (Exception e) {
                    log.info("Exception occurred: " + e.getMessage(), e);
                }
            }
            session.close();
            connection.close();
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }
}


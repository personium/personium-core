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
package io.personium.core.ws;

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
import io.personium.core.event.EventPublisher;

/**
 * Runnable class for receiving broadcasted event.
 */
public class EventSubscriber implements Runnable {
    static Logger log = LoggerFactory.getLogger(EventSubscriber.class);

    @Override
    public void run() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(PersoniumUnitConfig.getEventBusBrokerUrl());
            Connection connection = factory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination topic = session.createTopic(PersoniumUnitConfig.getEventBusTopicName());
            MessageConsumer subscriber = session.createConsumer(topic);
            connection.start();
            log.debug("Listen Topic messages ...");

            while (!Thread.interrupted()) {
                try {
                    Message m = subscriber.receive();
                    PersoniumEvent event = EventPublisher.convertToEvent(m);
                    if (event != null) {
                        log.debug(String.format("Received MapMessage with '%s'.", event.getType()));
                        log.debug("    External: " + event.getExternal());
                        log.debug("    Schema: " + event.getSchema());
                        log.debug("    Subject: " + event.getSubject());
                        log.debug("    Type: " + event.getType());
                        log.debug("    Object: " + event.getObject());
                        log.debug("    Info: " + event.getInfo());
                        log.debug("    RequestKey: " + event.getRequestKey());
                        log.debug("    EventId: " + event.getEventId());
                        log.debug("    RuleChain: " + event.getRuleChain());
                        log.debug("    CellId: " + event.getCellId());
                        log.debug("    DateTime: " + event.getDateTime());

                        // WebSocket send
                        WebSocketService.sendEvent(event);

                    } else {
                        log.info("Received Message null");
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
                    log.info("Exception occurreed: " + e.getMessage(), e);
                }
            }
            session.close();
            connection.close();
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }
}


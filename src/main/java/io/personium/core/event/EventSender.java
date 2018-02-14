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

import javax.jms.JMSException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.MapMessage;
import javax.jms.Message;

import org.apache.activemq.ActiveMQConnectionFactory;

import io.personium.core.PersoniumUnitConfig;

/**
 * Send event to queue.
 */
public class EventSender {
    /** Constructor. */
    private EventSender() {
    }

    /**
     * Send event.
     * @param event event to send
     */
    public static void send(PersoniumEvent event) {
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(PersoniumUnitConfig.getEventBusBrokerUrl());
            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(PersoniumUnitConfig.getEventBusQueueName());
            MessageProducer sender = session.createProducer(queue);

            MapMessage msg = session.createMapMessage();
            msg.setString("RequestKey", event.getRequestKey());
            msg.setString("EventId", event.getEventId());
            msg.setString("RuleChain", event.getRuleChain());
            msg.setString("Via", event.getVia());
            msg.setString("Roles", event.getRoles());
            msg.setBoolean("External", event.getExternal());
            msg.setString("Schema", event.getSchema());
            msg.setString("Subject", event.getSubject());
            msg.setString("Type", event.getType());
            msg.setString("Object", event.getObject());
            msg.setString("Info", event.getInfo());
            msg.setString("cellId", event.getCellId());
            msg.setString("dateTime", event.getDateTime());

            sender.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Convert message to PersoniumEvent.
     * @param msg message to be converted
     * @return event converted from message
     */
    public static PersoniumEvent convertToEvent(Message msg) {
        PersoniumEvent event = null;

        if (msg instanceof MapMessage) {
            MapMessage mm = (MapMessage) msg;
            String requestKey = null;
            String eventId = null;
            String ruleChain = null;
            String via = null;
            String roles = null;
            Boolean external = null;
            String schema = null;
            String subject = null;
            String type = null;
            String object = null;
            String info = null;
            String cellId = null;
            String dateTime = null;

            try {
                if (mm.itemExists("RequestKey")) {
                    requestKey = mm.getString("RequestKey");
                }
                if (mm.itemExists("EventId")) {
                    eventId = mm.getString("EventId");
                }
                if (mm.itemExists("RuleChain")) {
                    ruleChain = mm.getString("RuleChain");
                }
                if (mm.itemExists("Via")) {
                    via = mm.getString("Via");
                }
                if (mm.itemExists("Roles")) {
                    roles = mm.getString("Roles");
                }
                if (mm.itemExists("External")) {
                    external = mm.getBoolean("External");
                }
                if (mm.itemExists("Schema")) {
                    schema = mm.getString("Schema");
                }
                if (mm.itemExists("Subject")) {
                    subject = mm.getString("Subject");
                }
                if (mm.itemExists("Type")) {
                    type = mm.getString("Type");
                }
                if (mm.itemExists("Object")) {
                    object = mm.getString("Object");
                }
                if (mm.itemExists("Info")) {
                    info = mm.getString("Info");
                }
                if (mm.itemExists("cellId")) {
                    cellId = mm.getString("cellId");
                }
                if (mm.itemExists("dateTime")) {
                    dateTime = mm.getString("dateTime");
                }
            } catch (JMSException e) {
                return null;
            }
            event = new PersoniumEvent(external, schema, subject, type, object, info,
                    requestKey, eventId, ruleChain, via, roles);
            event.setCellId(cellId);
            event.setDateTime(dateTime);
        }

        return event;
    }
}

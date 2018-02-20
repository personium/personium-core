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
    /** Key name of RequestKey. */
    static final String KEY_REQUESTKEY = "RequestKey";
    /** Key name of EventId. */
    static final String KEY_EVENTID = "EventId";
    /** Key name of RuleChain. */
    static final String KEY_RULECHAIN = "RuleChain";
    /** Key name of Via. */
    static final String KEY_VIA = "Via";
    /** Key name of External. */
    static final String KEY_EXTERNAL = "External";
    /** Key name of Schema. */
    static final String KEY_SCHEMA = "Schema";
    /** Key name of Subject. */
    static final String KEY_SUBJECT = "Subject";
    /** Key name of Type. */
    static final String KEY_TYPE = "Type";
    /** Key name of Object. */
    static final String KEY_OBJECT = "Object";
    /** Key name of Info. */
    static final String KEY_INFO = "Info";
    /** Key name of cellId. */
    static final String KEY_CELLID = "cellId";
    /** Key name of Time. */
    static final String KEY_TIME = "Time";
    /** Key name of Roles. */
    static final String KEY_ROLES = "Roles";

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
            convertToMessage(event, msg);

            // only queue
            msg.setString(KEY_ROLES, event.getRoles());

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
     * Convert PersoniumEvent to MapMessage.
     * @param event event to be converted
     * @param msg converted from event
     * @throws JMSException
     */
    static void convertToMessage(PersoniumEvent event, MapMessage msg) throws JMSException {
        msg.setString(KEY_REQUESTKEY, event.getRequestKey());
        msg.setString(KEY_EVENTID, event.getEventId());
        msg.setString(KEY_RULECHAIN, event.getRuleChain());
        msg.setString(KEY_VIA, event.getVia());
        msg.setBoolean(KEY_EXTERNAL, event.getExternal());
        msg.setString(KEY_SCHEMA, event.getSchema());
        msg.setString(KEY_SUBJECT, event.getSubject());
        msg.setString(KEY_TYPE, event.getType());
        msg.setString(KEY_OBJECT, event.getObject());
        msg.setString(KEY_INFO, event.getInfo());
        msg.setString(KEY_CELLID, event.getCellId());
        msg.setLong(KEY_TIME, event.getTime());
    }

    /**
     * Convert MapMessage to PersoniumEvent.
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
            long time = 0;

            try {
                if (mm.itemExists(KEY_REQUESTKEY)) {
                    requestKey = mm.getString(KEY_REQUESTKEY);
                }
                if (mm.itemExists(KEY_EVENTID)) {
                    eventId = mm.getString(KEY_EVENTID);
                }
                if (mm.itemExists(KEY_RULECHAIN)) {
                    ruleChain = mm.getString(KEY_RULECHAIN);
                }
                if (mm.itemExists(KEY_VIA)) {
                    via = mm.getString(KEY_VIA);
                }
                if (mm.itemExists(KEY_ROLES)) {
                    roles = mm.getString(KEY_ROLES);
                }
                if (mm.itemExists(KEY_EXTERNAL)) {
                    external = mm.getBoolean(KEY_EXTERNAL);
                }
                if (mm.itemExists(KEY_SCHEMA)) {
                    schema = mm.getString(KEY_SCHEMA);
                }
                if (mm.itemExists(KEY_SUBJECT)) {
                    subject = mm.getString(KEY_SUBJECT);
                }
                if (mm.itemExists(KEY_TYPE)) {
                    type = mm.getString(KEY_TYPE);
                }
                if (mm.itemExists(KEY_OBJECT)) {
                    object = mm.getString(KEY_OBJECT);
                }
                if (mm.itemExists(KEY_INFO)) {
                    info = mm.getString(KEY_INFO);
                }
                if (mm.itemExists(KEY_CELLID)) {
                    cellId = mm.getString(KEY_CELLID);
                }
                if (mm.itemExists(KEY_TIME)) {
                    time = mm.getLong(KEY_TIME);
                }
            } catch (JMSException e) {
                return null;
            }
            event = new PersoniumEvent(external, schema, subject, type, object, info,
                    requestKey, eventId, ruleChain, via, roles);
            event.setCellId(cellId);
            event.setTime(time);
        }

        return event;
    }
}

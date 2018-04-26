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
package io.personium.core.event.impl.activemq;

import javax.jms.JMSException;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventSender;
import io.personium.core.event.PersoniumEvent;

/**
 * Send event to queue.
 */
public class ActiveMQEventSender implements EventSender {

    private ConnectionFactory factory;
    private Connection connection;
    private String queueName;

    /** Constructor. */
    public ActiveMQEventSender() {
    }

    /**
     * Open connection.
     * @param queue queue name to send event
     */
    @Override
    public void open(final String queue) {
        try {
            factory =
                    new ActiveMQConnectionFactory(PersoniumUnitConfig.getEventBusActiveMQBrokerUrl());
            connection = factory.createConnection();
            connection.start();
        } catch (Exception e) {
            connection = null;
        }
        this.queueName = queue;
    }

    /**
     * Send event.
     * @param event event to send
     */
    @Override
    public void send(final PersoniumEvent event) {
        Session session = null;
        MessageProducer sender = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(queueName);
            sender = session.createProducer(queue);

            ObjectMessage msg = session.createObjectMessage();
            msg.setObject(event);

            sender.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (sender != null) {
                    sender.close();
                }
                if (session != null) {
                    session.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close connection.
     */
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}

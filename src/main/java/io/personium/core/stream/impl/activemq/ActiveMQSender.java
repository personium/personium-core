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
package io.personium.core.stream.impl.activemq;

import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventPublisher;
import io.personium.core.event.EventSender;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.stream.DataPublisher;
import io.personium.core.stream.DataSender;

/**
 * Send data to mq.
 */
public class ActiveMQSender implements DataPublisher, DataSender, EventPublisher, EventSender {

    static final String PROP_CELL = "__PERSONIUM_CELL";
    static final String PROP_MESSAGE_ID = "__PERSONIUM_MESSAGE_ID";

    private ConnectionFactory factory;
    private Connection connection;
    private String name;

    private String broker;
    private String username;
    private String password;

    /**
     * Constructor.
     * @param broker broker url
     */
    public ActiveMQSender(final String broker) {
        this.broker = broker;
    }

    /**
     * Constructor.
     * @param broker broker url
     * @param username username
     * @param password password
     */
    public ActiveMQSender(final String broker, final String username, final String password) {
        this.broker = broker;
        this.username = username;
        this.password = password;
    }

    /**
     * Open connection.
     * @param name queue or topic name
     */
    @Override
    public void open(final String name) { // CHECKSTYLE IGNORE
        try {
            factory = new ActiveMQConnectionFactory(broker);
            if (username != null) {
                connection = factory.createConnection(username, password);
            } else {
                connection = factory.createConnection();
            }
            connection.start();
        } catch (Exception e) {
            connection = null;
            e.printStackTrace();
        }
        this.name = name;
    }

    /**
     * Send data.
     * @param cellUrl cell url
     * @param data data to send
     */
    @Override
    public void send(final String cellUrl, final String data) {
        Session session = null;
        MessageProducer producer = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = session.createQueue(this.name);
            producer = session.createProducer(dest);

            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.setTimeToLive(TimeUnit.SECONDS.toMillis(PersoniumUnitConfig.getStreamExpiresIn()));

            TextMessage msg = session.createTextMessage();
            msg.setText(data);

            msg.setStringProperty(PROP_CELL, cellUrl);
            msg.setStringProperty(PROP_MESSAGE_ID, PersoniumUUID.randomUUID());

            producer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
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
     * Publish data.
     * @param cellUrl cell url
     * @param data data to send
     */
    @Override
    public void publish(final String cellUrl, final String data) {
        Session session = null;
        MessageProducer producer = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = session.createTopic(this.name);
            producer = session.createProducer(dest);

            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.setTimeToLive(TimeUnit.SECONDS.toMillis(PersoniumUnitConfig.getStreamExpiresIn()));

            TextMessage msg = session.createTextMessage();
            msg.setText(data);

            msg.setStringProperty(PROP_CELL, cellUrl);
            msg.setStringProperty(PROP_MESSAGE_ID, PersoniumUUID.randomUUID());

            producer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
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
     * Send event.
     * @param event event to send
     */
    @Override
    public void send(final PersoniumEvent event) {
        Session session = null;
        MessageProducer producer = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = session.createQueue(this.name);
            producer = session.createProducer(dest);

            ObjectMessage msg = session.createObjectMessage();
            msg.setObject(event);

            producer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
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
     * Publish event.
     * @param event event to publish
     */
    @Override
    public void publish(final PersoniumEvent event) {
        Session session = null;
        MessageProducer producer = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = session.createTopic(this.name);
            producer = session.createProducer(dest);

            ObjectMessage msg = session.createObjectMessage();
            msg.setObject(event);

            producer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            try {
                if (producer != null) {
                    producer.close();
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
    @Override
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

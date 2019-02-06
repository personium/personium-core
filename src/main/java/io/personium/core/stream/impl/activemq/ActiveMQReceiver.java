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
package io.personium.core.stream.impl.activemq;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.EventSubscriber;
import io.personium.core.event.EventReceiver;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.stream.DataReceiver;
import io.personium.core.stream.DataSubscriber;
import io.personium.core.stream.IDataListener;

/**
 * Receiver for ActiveMQ.
 */
public class ActiveMQReceiver implements MessageListener,
                                         DataReceiver, DataSubscriber, EventReceiver, EventSubscriber {
    private static Logger log = LoggerFactory.getLogger(ActiveMQReceiver.class);

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    private String broker;
    private String username;
    private String password;

    private IDataListener listener;

    private static final long POLL_TIMEOUT = 1000L;

    /**
     * Constructor.
     * @param broker broker url
     */
    public ActiveMQReceiver(final String broker) {
        this.broker = broker;
    }

    /**
     * Constructor.
     * @param broker broker url
     * @param username username
     * @param password password
     */
    public ActiveMQReceiver(final String broker, final String username, final String password) {
        this.broker = broker;
        this.username = username;
        this.password = password;
    }

    @Override
    public void open(final String queueName) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(broker);
            factory.setTrustedPackages(
                    Arrays.asList("java.lang", "io.personium.core.event"));
            if (username != null) {
                connection = factory.createConnection(username, password);
            } else {
                connection = factory.createConnection();
            }
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(queueName);
            consumer = session.createConsumer(queue);
            connection.start();
            log.debug("Listen JMS messages ...");
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void subscribe(final String topicName) {
        try {
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(broker);
            factory.setTrustedPackages(
                    Arrays.asList("java.lang", "io.personium.core.event"));
            if (username != null) {
                connection = factory.createConnection(username, password);
            } else {
                connection = factory.createConnection();
            }
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination topic = session.createTopic(topicName);
            consumer = session.createConsumer(topic);
            connection.start();
            log.debug("Listen JMS messages ...");
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void setListener(IDataListener listener) {
        if (listener != null) {
            try {
                this.listener = listener;
                consumer.setMessageListener(this);
            } catch (JMSException e) {
                this.listener = null;
            }
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                String cell = tm.getStringProperty(ActiveMQSender.PROP_CELL);
                String msgId = tm.getStringProperty(ActiveMQSender.PROP_MESSAGE_ID);
                String body = tm.getText();
                if (cell == null || msgId == null) {
                    return;
                }
                if (this.listener != null) {
                    this.listener.onMessage(cell, body);
                }
            }
        } catch (JMSException e) {
            log.error("JMSException occurred: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PersoniumEvent> receive() {
        List<PersoniumEvent> list = new ArrayList<>();

        try {
            Message m = consumer.receive(POLL_TIMEOUT);
            if (m instanceof ObjectMessage) {
                ObjectMessage om = (ObjectMessage) m;
                PersoniumEvent event = (PersoniumEvent) om.getObject();
                if (event != null) {
                    list.add(event);
                }
            }
        } catch (JMSException e) {
            Exception exp = e.getLinkedException();
            if (exp instanceof InterruptedException) {
                log.debug("Interrupted");
            } else {
                log.error("JMSException occurred: " + e.getMessage(), e);
            }
            return null;
        } catch (Exception e) {
            log.error("Exception occurred: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public List<String> receiveData() {
        List<String> list = new ArrayList<>();

        try {
            Message m = consumer.receive(POLL_TIMEOUT);
            if (m instanceof TextMessage) {
                TextMessage tm = (TextMessage) m;
                String cell = tm.getStringProperty(ActiveMQSender.PROP_CELL);
                String msgId = tm.getStringProperty(ActiveMQSender.PROP_MESSAGE_ID);
                String body = tm.getText();
                if (cell == null || msgId == null) {
                    return list;
                }
                list.add(body);
            }
        } catch (JMSException e) {
            log.debug("JMSException occurred: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void close() {
        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            Exception exp = e.getLinkedException();
            if (exp instanceof InterruptedIOException) {
                log.debug("Interrupted");
            } else {
                log.error("Exception occurred: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void unsubscribe() {
        close();
    }

}


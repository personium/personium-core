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

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventReceiver;
import io.personium.core.event.PersoniumEvent;

/**
 * EventReceiver for ActiveMQ.
 */
public class ActiveMQEventReceiver implements EventReceiver {
    private static Logger log = LoggerFactory.getLogger(ActiveMQEventReceiver.class);

    private Connection connection;
    private Session session;
    private MessageConsumer receiver;

    /**
     * Constructor.
     */
    public ActiveMQEventReceiver() {
    }

    @Override
    public void subscribe(final String queueName) {
        try {
            ActiveMQConnectionFactory factory =
                    new ActiveMQConnectionFactory(PersoniumUnitConfig.getEventBusActiveMQBrokerUrl());
            factory.setTrustedPackages(
                    Arrays.asList("java.lang", "io.personium.core.event"));
            connection = factory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination queue = session.createQueue(queueName);
            receiver = session.createConsumer(queue);
            connection.start();
            log.debug("Listen JMS messages ...");
        } catch (Exception e) {
            log.error("JMS failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PersoniumEvent> receive() {
        List<PersoniumEvent> list = new ArrayList<>();

        try {
            Message m = receiver.receive();
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
    public void unsubscribe() {
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

}


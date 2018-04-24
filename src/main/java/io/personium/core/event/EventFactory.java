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
package io.personium.core.event;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.impl.activemq.ActiveMQEventReceiver;
import io.personium.core.event.impl.activemq.ActiveMQEventPublisher;
import io.personium.core.event.impl.activemq.ActiveMQEventSender;
import io.personium.core.event.impl.activemq.ActiveMQEventSubscriber;
import io.personium.core.event.impl.kafka.KafkaEventReceiver;
import io.personium.core.event.impl.kafka.KafkaEventSender;
import io.personium.core.event.impl.kafka.KafkaEventSubscriber;

/**
 * Factory class for event.
 */
public class EventFactory {
    private static final String ACTIVEMQ = "activemq";
    private static final String KAFKA = "kafka";

    private static String mq = PersoniumUnitConfig.getEventBusMQ();
    private static String queueName = PersoniumUnitConfig.getEventBusQueueName();
    private static String topicName = PersoniumUnitConfig.getEventBusTopicName();

    private static EventSender eventSender;
    private static EventPublisher eventPublisher;

    /** Constructor. */
    private EventFactory() {
    }

    /**
     * Create EventReceiver.
     * @return created EventReceiver
     */
    static EventReceiver createEventReceiver() {
        EventReceiver eventReceiver;

        if (ACTIVEMQ.equals(mq)) {
            eventReceiver = new ActiveMQEventReceiver();
        } else if (KAFKA.equals(mq)) {
            eventReceiver = new KafkaEventReceiver();
        } else {
            eventReceiver = new ActiveMQEventReceiver();
        }
        eventReceiver.subscribe(queueName);

        return eventReceiver;
    }

    /**
     * Create EventSubscriber.
     * @return created EventSubscriber
     */
    public static EventSubscriber createEventSubscriber() {
        EventSubscriber eventSubscriber;

        if (ACTIVEMQ.equals(mq)) {
            eventSubscriber = new ActiveMQEventSubscriber();
        } else if (KAFKA.equals(mq)) {
            eventSubscriber = new KafkaEventSubscriber();
        } else {
            eventSubscriber = new ActiveMQEventSubscriber();
        }
        eventSubscriber.subscribe(topicName);

        return eventSubscriber;
    }

    /**
     * Create EventSubscriber with topic name.
     * @param topic topic name
     * @return created EventSubscriber
     */
    public static EventSubscriber createEventSubscriber(final String topic) {
        EventSubscriber eventSubscriber;

        if (ACTIVEMQ.equals(mq)) {
            eventSubscriber = new ActiveMQEventSubscriber();
        } else if (KAFKA.equals(mq)) {
            eventSubscriber = new KafkaEventSubscriber();
        } else {
            eventSubscriber = new ActiveMQEventSubscriber();
        }
        eventSubscriber.subscribe(topic);

        return eventSubscriber;
    }

    /**
     * Create EventSender.
     */
    static void createEventSender() {
        if (ACTIVEMQ.equals(mq)) {
            eventSender = new ActiveMQEventSender();
        } else if (KAFKA.equals(mq)) {
            eventSender = new KafkaEventSender();
        } else {
            eventSender = new ActiveMQEventSender();
        }
        eventSender.open(queueName);
    }

    /**
     * Get EventSender.
     * @return EventSender
     */
    static EventSender getEventSender() {
        return eventSender;
    }

    /**
     * Close EventSender.
     */
    static void closeEventSender() {
        eventSender.close();
    }

    /**
     * Create EventPublisher.
     * @return created EventPublisher
     */
    static void createEventPublisher() {
        if (ACTIVEMQ.equals(mq)) {
            eventPublisher = new ActiveMQEventPublisher();
        } else if (KAFKA.equals(mq)) {
            eventPublisher = new KafkaEventSender();
        } else {
            eventPublisher = new ActiveMQEventPublisher();
        }
        eventPublisher.open(topicName);
    }

    /**
     * Get EventPublisher.
     * @return EventPublisher
     */
    static EventPublisher getEventPublisher() {
        return eventPublisher;
    }

    /**
     * Close EventPublisher.
     */
    static void closeEventPublisher() {
        eventPublisher.close();
    }

    /**
     * Create EventPublisher.
     * @param topic topic name
     * @return created EventPublisher
     */
    public static EventPublisher createEventPublisher(final String topic) {
        EventPublisher publisher;

        if (ACTIVEMQ.equals(mq)) {
            publisher = new ActiveMQEventPublisher();
        } else if (KAFKA.equals(mq)) {
            publisher = new KafkaEventSender();
        } else {
            publisher = new ActiveMQEventPublisher();
        }
        publisher.open(topic);

        return publisher;
    }

}

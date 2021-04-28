/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.core.stream;

import java.util.Optional;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.stream.impl.activemq.ActiveMQReceiver;
import io.personium.core.stream.impl.activemq.ActiveMQSender;
import io.personium.core.stream.impl.kafka.KafkaDataSender;
import io.personium.core.stream.impl.kafka.KafkaDataReceiver;
import io.personium.core.stream.impl.kafka.SpringKafkaDataSubscriber;

/**
 * Factory class for stream.
 */
public class StreamFactory {
    private static final String ACTIVEMQ = "activemq";
    private static final String KAFKA = "kafka";

    private static String mq = PersoniumUnitConfig.getStreamMQ();
    private static String broker = PersoniumUnitConfig.getStreamBroker();
    private static String username = PersoniumUnitConfig.getStreamUsername();
    private static String password = PersoniumUnitConfig.getStreamPassword();

    /** Constructor. */
    private StreamFactory() {
    }

    /**
     * Create DataSender.
     * @return created DataSender
     */
    public static Optional<DataSender> createDataSender() {
        if (mq == null || broker == null) {
            return Optional.empty();
        }

        DataSender sender = null;

        if (ACTIVEMQ.equals(mq)) {
            sender = new ActiveMQSender(broker, username, password);
        } else if (KAFKA.equals(mq)) {
            sender = new KafkaDataSender(broker, username, password);
        }

        return Optional.ofNullable(sender);
    }

    /**
     * Create DataPublisher.
     * @return created DataPublisher
     */
    public static Optional<DataPublisher> createDataPublisher() {
        if (mq == null || broker == null) {
            return Optional.empty();
        }

        DataPublisher publisher = null;

        if (ACTIVEMQ.equals(mq)) {
            publisher = new ActiveMQSender(broker, username, password);
        } else if (KAFKA.equals(mq)) {
            publisher = new KafkaDataSender(broker, username, password);
        }

        return Optional.ofNullable(publisher);
    }

    /**
     * Create DataReceiver.
     * @return created DataReceiver
     */
    public static Optional<DataReceiver> createDataReceiver() {
        if (mq == null || broker == null) {
            return Optional.empty();
        }

        DataReceiver receiver = null;

        if (ACTIVEMQ.equals(mq)) {
            receiver = new ActiveMQReceiver(broker, username, password);
        } else if (KAFKA.equals(mq)) {
            receiver = new KafkaDataReceiver(broker, username, password);
        }

        return Optional.ofNullable(receiver);
    }

    /**
     * Create DataSubscriber.
     * @return created DataSubscriber
     */
    public static Optional<DataSubscriber> createDataSubscriber() {
        if (mq == null || broker == null) {
            return Optional.empty();
        }

        DataSubscriber subscriber = null;

        if (ACTIVEMQ.equals(mq)) {
            subscriber = new ActiveMQReceiver(broker, username, password);
        } else if (KAFKA.equals(mq)) {
            subscriber = new SpringKafkaDataSubscriber(broker, username, password);
        }

        return Optional.ofNullable(subscriber);
    }

}

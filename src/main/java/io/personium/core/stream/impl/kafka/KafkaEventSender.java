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
package io.personium.core.stream.impl.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import io.personium.core.event.EventPublisher;
import io.personium.core.event.EventSender;
import io.personium.core.event.PersoniumEvent;

/**
 * Send event to queue.
 */
public class KafkaEventSender implements EventSender, EventPublisher {

    private Producer<String, PersoniumEvent> producer;
    private String broker;
    private String topicName;

    /**
     * Constructor.
     * @param broker brokers
     */
    public KafkaEventSender(final String broker) {
        this.broker = broker;
    }

    /**
     * Open connection.
     * @param topic topic name to send event
     */
    @Override
    public void open(final String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("acks", "all");
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", PersoniumEventSerializer.class);

        producer = new KafkaProducer<>(props);

        this.topicName = topic;
    }

    /**
     * Send event.
     * @param event event to send
     */
    @Override
    public void send(final PersoniumEvent event) {
        producer.send(new ProducerRecord<>(topicName, event));
    }

    @Override
    public void publish(final PersoniumEvent event) {
        send(event);
    }

    /**
     * Close connection.
     */
    @Override
    public void close() {
        producer.close();
    }

}

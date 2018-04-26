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
package io.personium.core.event.impl.kafka;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventSubscriber;
import io.personium.core.event.PersoniumEvent;

/**
 * EventSubcriber for Kafka.
 */
public class KafkaEventSubscriber implements EventSubscriber {
    private static Logger log = LoggerFactory.getLogger(KafkaEventSubscriber.class);

    private KafkaConsumer<String, PersoniumEvent> consumer;

    private static final long POLL_TIMEOUT = 1000L;

    /**
     * Constructor.
     */
    public KafkaEventSubscriber() {
    }

    /**
     * Subscribe.
     * @param topic topic name
     */
    @Override
    public void subscribe(final String topic) {
        String servers = PersoniumUnitConfig.getEventBusKafkaServers();

        Properties props = new Properties();
        props.put("bootstrap.servers", servers);
        props.put("group.id", UUID.randomUUID().toString());
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.intervals.ms", "1000");
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", PersoniumEventDeserializer.class);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));
    }

    /**
     * Receive.
     * @return list of PersoniumEvent received
     */
    @Override
    public List<PersoniumEvent> receive() {
        List<PersoniumEvent> list = new ArrayList<>();

        try {
            ConsumerRecords<String, PersoniumEvent> records = consumer.poll(POLL_TIMEOUT);
            for (ConsumerRecord<String, PersoniumEvent> record : records) {
                PersoniumEvent event = record.value();
                list.add(event);
            }
        } catch (InterruptException e) {
            log.debug("Interrupted");
            return null;
        } catch (KafkaException e) {
            log.error("KafkaException occurred: " + e.getMessage(), e);
            return null;
        }

        return list;
    }

    /**
     * Unsubscribe.
     */
    @Override
    public void unsubscribe() {
        try {
            consumer.close();
        } catch (InterruptException e) {
            log.debug("Interrupted");
        }
    }

}

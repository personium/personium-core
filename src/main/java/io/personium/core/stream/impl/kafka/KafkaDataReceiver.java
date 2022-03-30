/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.stream.DataReceiver;

/**
 * DataReceiver for Kafka.
 */
public class KafkaDataReceiver implements DataReceiver {
    private static Logger log = LoggerFactory.getLogger(KafkaDataReceiver.class);

    private KafkaConsumer<String, String> consumer;
    private String broker;
    private String config;

    private static final long POLL_TIMEOUT = 100L;

    /**
     * Constructor.
     * @param broker brokers
     * @param username username
     * @param password password
     */
    public KafkaDataReceiver(final String broker, final String username, final String password) {
        this.broker = broker;
        if (username != null) {
            this.config = "org.apache.kafka.common.security.plain.PlainLoginModule required"
                          + " username=\"" + username + "\""
                          + " password=\"" + password + "\";";
        }
    }

    /**
     * Open connection.
     * @param topic topic name
     */
    @Override
    public void open(final String topic) {
        Properties props = new Properties();
        props.put("bootstrap.servers", broker);
        props.put("group.id", "data_receiver");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.intervals.ms", "1000");
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", StringDeserializer.class);

        if (config != null) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config", config);
        }

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(topic));
    }

    /**
     * Receive data.
     * @return list of received data
     */
    @Override
    public List<String> receiveData() {
        List<String> list = new ArrayList<>();

        try {
            ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
            for (ConsumerRecord<String, String> record : records) {
                Headers headers = record.headers();
                Header header = headers.lastHeader(KafkaDataSender.HEADER_CELL);
                if (header != null) {
                    list.add(record.value());
                }
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
     * Close.
     */
    @Override
    public void close() {
        try {
            consumer.close();
        } catch (InterruptException e) {
            log.debug("Interrupted");
        }
    }

}

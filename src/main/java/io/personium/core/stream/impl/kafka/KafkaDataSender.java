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

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringSerializer;

import io.personium.core.stream.DataPublisher;
import io.personium.core.stream.DataSender;

/**
 * Send data to topic.
 */
public class KafkaDataSender implements DataPublisher, DataSender {

    static final String HEADER_CELL = "__PERSONIUM_CELL";

    private Producer<String, String> producer;
    private String topicName;

    private String broker;
    private String config;

    /**
     * Constructor.
     * @param broker brokers
     * @param username username
     * @param password password
     */
    public KafkaDataSender(final String broker, final String username, final String password) {
        this.broker = broker;
        if (username != null) {
            this.config = "org.apache.kafka.common.security.plain.PlainLoginModule required"
                          + " username=\"" + username + "\""
                          + " password=\"" + password + "\";";
        }
    }

    /**
     * Open connection.
     * @param topic topic name to send event
     */
    @Override
    public void open(final String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.broker);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        if (config != null) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, config);
        }

        producer = new KafkaProducer<>(props);

        this.topicName = topic;
    }

    /**
     * Send data.
     * @param cellUrl cell url
     * @param data data to send
     */
    @Override
    public void send(final String cellUrl, final String data) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topicName, data);
            Headers headers = record.headers();
            headers.add(HEADER_CELL, cellUrl.getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Publish data.
     * @param cellUrl cell url
     * @param data data to send
     */
    @Override
    public void publish(final String cellUrl, final String data) {
        send(cellUrl, data);
    }

    /**
     * Close connection.
     */
    @Override
    public void close() {
        producer.close();
    }

}

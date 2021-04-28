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
package io.personium.core.stream.impl.kafka;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;

import io.personium.core.stream.DataSubscriber;
import io.personium.core.stream.IDataListener;

/**
 * DataSubscriber for Kafka.
 */
public class SpringKafkaDataSubscriber implements DataSubscriber, MessageListener<String, String> {

    KafkaMessageListenerContainer<String, String> container;
    private String broker;
    private String config;

    private IDataListener listener;

    /**
     * Constructor.
     * @param broker brokers
     * @param username username
     * @param password password
     */
    public SpringKafkaDataSubscriber(final String broker, final String username, final String password) {
        this.broker = broker;
        if (username != null) {
            this.config = "org.apache.kafka.common.security.plain.PlainLoginModule required"
                          + " username=\"" + username + "\""
                          + " password=\"" + password + "\";";
        }
    }

    /**
     * Subscribe.
     * @param topic topic name
     */
    @Override
    public void subscribe(final String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        if (config != null) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            props.put(SaslConfigs.SASL_JAAS_CONFIG, config);
        }

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(props);
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener(this);

        container = new KafkaMessageListenerContainer<>(cf, containerProps);
        container.start();
    }

    @Override
    public void setListener(IDataListener listener) {
        this.listener = listener;
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> message) {
        if (this.listener != null) {
            Headers headers = message.headers();
            Header header = headers.lastHeader(KafkaDataSender.HEADER_CELL);
            String cellUrl = null;
            if (header != null) {
                cellUrl = new String(header.value(), StandardCharsets.UTF_8);
            }
            this.listener.onMessage(cellUrl, message.value());
        }
    }

    @Override
    public void unsubscribe() {
        container.stop();
    }

}

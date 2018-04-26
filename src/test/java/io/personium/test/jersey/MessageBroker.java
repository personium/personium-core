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
package io.personium.test.jersey;

import org.apache.activemq.broker.BrokerService;
import org.springframework.kafka.test.rule.KafkaEmbedded;

import io.personium.core.PersoniumUnitConfig;

/**
 * Message Broker setup class.
 */
public class MessageBroker {
    // ActiveMQ
    BrokerService broker;

    // Kafka
    KafkaEmbedded embeddedKafka;

    /**
     * Constructor.
     */
    public MessageBroker() {
    }

    /**
     * Start Message Broker.
     * @throws Exception exception
     */
    public void start() throws Exception {
        if ("kafka".equals(PersoniumUnitConfig.getEventBusMQ())) {
            startKafka();
        } else {
            startActiveMQ();
        }
    }

    /**
     * Stop Message Broker.
     * @throws Exception exception
     */
    public void stop() throws Exception {
        if ("kafka".equals(PersoniumUnitConfig.getEventBusMQ())) {
            stopKafka();
        } else {
            stopActiveMQ();
        }
    }

    private void startActiveMQ() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.start();
    }

    private void stopActiveMQ() throws Exception {
        broker.stop();
    }

    private void startKafka() throws Exception {
        embeddedKafka = new KafkaEmbedded(1, true, 1, PersoniumUnitConfig.getEventBusQueueName());
        embeddedKafka.before();
        PersoniumUnitConfig.set(
                "io.personium.core.eventbus.kafka.bootstrap.servers",
                embeddedKafka.getBrokersAsString());
    }

    private void stopKafka() {
        embeddedKafka.after();
    }

}

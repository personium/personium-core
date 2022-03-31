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
package io.personium.core.stream;

/**
 * Send data to topic.
 */
public interface DataPublisher {

    /**
     * Open connection.
     * @param topic topic name
     */
    void open(final String topic);

    /**
     * Send data to topic.
     * @param cellUrl cell url
     * @param data data to send
     */
    void publish(final String cellUrl, final String data);

    /**
     * Close connection.
     */
    void close();

}

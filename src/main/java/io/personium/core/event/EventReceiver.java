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
package io.personium.core.event;

import java.util.List;

/**
 * EventReceiver interface.
 */
public interface EventReceiver {

    /**
     * Open.
     * @param queueName queue name
     */
    void open(String queueName);

    /**
     * Receive.
     * @return list of event
     */
    List<PersoniumEvent> receive();

    /**
     * Close.
     */
    void close();

}

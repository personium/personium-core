/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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
package io.personium.core.rule.action;

import io.personium.core.event.PersoniumEvent;

/**
 * Action abstract class.
 */
public abstract class Action {
    /**
     * Execute action for one event.
     * @param event event to be executed
     * @return event created from result of executing
     */
    public abstract PersoniumEvent execute(PersoniumEvent event);

    /**
     * Execute action for some events.
     * @param events array of events to be executed
     * @return event created from result of executing
     */
    public abstract PersoniumEvent execute(PersoniumEvent[] events);
}


/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
 * - FUJITSU LIMITED
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.rule.RuleManager;

/**
 * Bus for sendig event.
 */
public final class EventBus {
    Cell cell;

    /**
     * Constructor.
     * @param cell Cell
     */
    public EventBus(Cell cell) {
        this.cell = cell;
    }

    /**
     * Post event.
     * @param ev event
     */
    public void post(final PersoniumEvent ev) {
        // set cell id
        ev.setCellId(this.cell.getId());

        // set time
        ev.setTime();

        // send event
        EventFactory.getEventSender().send(ev);
    }

    private static ExecutorService pool;

    /**
     * Start EventBus.
     */
    public static void start() {
        EventFactory.createEventPublisher();

        // RuleManager
        RuleManager.getInstance();

        // create thread pool.
        int threadNumber = PersoniumUnitConfig.getEventProcThreadNum();
        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("event-receiver-%d");
        pool = Executors.newFixedThreadPool(threadNumber, builder.build());
        // Execute receiver for processing rule.
        for (int i = 0; i < threadNumber; i++) {
            pool.execute(new EventReceiveRunner());
        }

        EventFactory.createEventSender();
    }

    /**
     * Stop EventBus.
     */
    public static void stop() {
        EventFactory.closeEventSender();

        // shutdown thread pool.
        try {
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        // shutdown RuleManager.
        RuleManager rman = RuleManager.getInstance();
        if (rman != null) {
            rman.shutdown();
        }

        EventFactory.closeEventPublisher();
    }

}

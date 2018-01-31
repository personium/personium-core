/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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
package io.personium.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import io.personium.common.utils.PersoniumThread;
import io.personium.core.rule.RuleManager;
import io.personium.core.ws.EventSubscriber;

/**
 * Listener Class for start/shutdown processing.
 */
@WebListener
public class PersoniumCoreListener implements ServletContextListener {
    /** Maximum timeout seconds. */
    private static final long TIMEOUT_SECONDS = 50;

    private ExecutorService pool;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Create ThreadPool.
        pool = Executors.newFixedThreadPool(1);
        // Execute receiver for all event.
        pool.execute(new EventSubscriber());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Shutdown ThreadPool.
        try {
            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
        // Finalize RuleManager.
        RuleManager rman = RuleManager.getInstance();
        if (rman != null) {
            rman.finalize();
        }
        // Shutdown PersoniumThread.
        PersoniumThread.shutdown(TIMEOUT_SECONDS);
    }
}

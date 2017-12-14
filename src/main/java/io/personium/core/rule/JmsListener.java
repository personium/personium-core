/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.rule;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.annotation.WebListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

/**
 * Class for executing threads abount EventBus.
 */
@WebListener
public class JmsListener implements ServletContextListener {
    private static final int EVENTRECEIVER_NUM = 2;
    private static final int RULEEVENTSUBSCRIBER_NUM = 1;
    private static final int THREAD_NUMBER = EVENTRECEIVER_NUM + RULEEVENTSUBSCRIBER_NUM;

    private ExecutorService pool;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Initialize RuleManager.
        // Load rules from DB.
        RuleManager rman = RuleManager.getInstance();
        rman.load();

        // Create ThreadPoll.
        pool = Executors.newFixedThreadPool(THREAD_NUMBER);
        // Execute receiver for processing rule.
        for (int i = 0; i < EVENTRECEIVER_NUM; i++) {
            pool.execute(new EventReceiver());
        }
        // Execute receiver for managing rule.
        pool.execute(new RuleEventSubscriber());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        pool.shutdown();
    }
}


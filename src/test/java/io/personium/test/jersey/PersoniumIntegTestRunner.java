/**
 * Personium
 * Copyright 2014-2019 FUJITSU LIMITED
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

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.event.EventBus;
import io.personium.core.rs.PersoniumCoreApplication;

/**
 * Test Runner class for integration tests.
 */
public class PersoniumIntegTestRunner extends BlockJUnit4ClassRunner {
    /**
     * logger.
     */
    private static Logger log = LoggerFactory.getLogger(PersoniumIntegTestRunner.class);

    /**
     * Constructor.
     * @param klass klass
     * @throws InitializationError InitializationError
     */
    public PersoniumIntegTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    MessageBroker broker;

    private void start() throws Exception {
        broker = new MessageBroker();
        broker.start();
        PersoniumCoreApplication.start();
        EventBus.start();
    }

    private void stop() throws Exception {
        EventBus.stop();
        PersoniumCoreApplication.stop();
        broker.stop();
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            // start
            start();

            // run
            super.run(notifier);

            // stop
            stop();
        } catch (Exception e) {
            log.info("exeption occurred: ", e);
        }
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        log.info("######## " + method.getName() + " ########");
        super.runChild(method, notifier);
    }

}

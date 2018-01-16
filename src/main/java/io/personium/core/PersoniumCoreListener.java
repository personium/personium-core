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
package io.personium.core;

import javax.servlet.annotation.WebListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;

import io.personium.common.utils.PersoniumThread;
import io.personium.core.rule.RuleManager;

/**
 * Listener Class for start/shutdown processing.
 */
@WebListener
public class PersoniumCoreListener implements ServletContextListener {
    /** Maximum timeout seconds. */
    private static final long TIMEOUT_SECONDS = 50;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Currently nothing to do.
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Finalize RuleManager.
        RuleManager rman = RuleManager.getInstance();
        if (rman != null) {
            rman.finalize();
        }
        // Shutdown PersoniumThread.
        PersoniumThread.shutdown(TIMEOUT_SECONDS);
    }
}

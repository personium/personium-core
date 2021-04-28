/**
 * Personium
 * Copyright 2017-2021 Personium Project Authors
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

import io.personium.core.event.EventBus;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.core.ws.WebSocketService;

/**
 * Listener Class for start/shutdown processing.
 */
@WebListener
public class PersoniumCoreListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Start Application.
        PersoniumCoreApplication.start();

        // Start EventBus.
        EventBus.start();

        // Start WebSocketService.
        WebSocketService.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stop WebSocket service.
        WebSocketService.stop();

        // Stop EventBus.
        EventBus.stop();

        // Stop Application.
        PersoniumCoreApplication.stop();
    }
}

/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package com.fujitsu.dc.core.webcontainer.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


/**
 * Webコンテナ起動/終了時に呼び出されるListenerクラス.
 */
public class WebContainerListenerForAdsRepair implements ServletContextListener {

    RepairServiceLauncher launcher = null;

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        launcher = new RepairServiceLauncher();
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        if (null != launcher) {
            launcher.shutdown();
        }
    }
}

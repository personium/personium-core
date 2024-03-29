/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.rs;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import io.personium.common.auth.token.AbstractLocalToken;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.file.DataCryptor;
import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.jersey.filter.PersoniumCoreContainerFilter;
import io.personium.core.jersey.filter.WriteMethodFilter;
import io.personium.core.plugin.PluginManager;

/**
 * Personium-core / _cell _ / * JAX-RS Application responsible for URL below.
 */
public class PersoniumCoreApplication extends Application {
    private static PluginManager pm;

    /** Maximum timeout seconds. */
    private static final long TIMEOUT_SECONDS = 50;

    /**
     * Start Application.
     */
    public static void start() {
        try {
            loadConfig();
            TransCellAccessToken.configureX509(PersoniumUnitConfig.getX509PrivateKey(),
                    PersoniumUnitConfig.getX509Certificate(), PersoniumUnitConfig.getX509RootCertificate());
            PersoniumThread.start(PersoniumUnitConfig.getThreadPoolNumForCellIO(),
                    PersoniumUnitConfig.getThreadPoolNumForBoxIO(),
                    PersoniumUnitConfig.getThreadPoolNumForMisc());

            // Set SystemProperties for lib-es-adapter.
            // To guarantee switching between the Es2 library and the Es5 library.
            String numberOfShards = String.valueOf(PersoniumUnitConfig.getESIndexNumberOfShards());
            String numberOfReplicas = String.valueOf(PersoniumUnitConfig.getESIndexNumberOfReplicas());
            String maxResultWindow = String.valueOf(PersoniumUnitConfig.getESIndexMaxResultWindow());
            System.setProperty("io.personium.es.index.numberOfShards", numberOfShards);
            System.setProperty("io.personium.es.index.numberOfReplicas", numberOfReplicas);
            System.setProperty("io.personium.es.index.maxResultWindow", maxResultWindow);
            Integer mergeSchedulerMaxThreadCount = PersoniumUnitConfig.getESIndexMergeSchedulerMaxThreadCount();
            if (mergeSchedulerMaxThreadCount != null) {
                System.setProperty("io.personium.es.index.merge.scheduler.maxThreadCount",
                        String.valueOf(mergeSchedulerMaxThreadCount));
            }

            loadPlugins();
        } catch (Exception e) {
            PersoniumCoreLog.Server.FAILED_TO_START_SERVER.reason(e).writeLog();
            throw new RuntimeException(e);
        }
    }
    public static void loadConfig() {
        AbstractLocalToken.setKeyString(PersoniumUnitConfig.getTokenSecretKey());
        DataCryptor.setKeyString(PersoniumUnitConfig.getTokenSecretKey());
    }

    public static void loadPlugins() {
        pm = new PluginManager();
    }

    /**
     * Stop Application.
     */
    public static void stop() {
        PersoniumThread.stop(TIMEOUT_SECONDS);
    }

    /*
     * (non-Javadoc)
     * @see javax.ws.rs.core.Application#getClasses()
     */
    @Override
    public final Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PersoniumCoreExceptionMapper.class);
        classes.add(PersoniumCoreContainerFilter.class);
        classes.add(WriteMethodFilter.class);
        classes.add(FacadeResource.class);
        return classes;
    }

    /**
     * get PluginsManager.
     * @return PluginsManager
     */
    public static PluginManager getPluginManager() {
        return pm;
    }
}

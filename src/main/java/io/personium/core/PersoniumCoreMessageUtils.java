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
package io.personium.core;

import io.personium.plugin.base.PluginMessageUtils.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * Log message creation class.
 */
public abstract class PersoniumCoreMessageUtils {
    /**
     * Holds the log level setting.
     */
    private static final Properties LOG_LEVEL_PROP;
    /**
     * Holds the setting of the log message.
     */
    private static final Properties LOG_MSG_PROP;

    /**
     * Key of log level setting.
     * Define up to the dot since you attach a message code to the back
     * Example) io.personium.core.loglevel.PR400 - OD - 0001
     */
    public static final String LOG_LEVEL = PersoniumUnitConfig.KEY_ROOT + "loglevel.";

    /**
     * Key of log message setting.
     * Define up to the dot since you attach a message code to the back
     */
    public static final String LOG_MESSAGE = PersoniumUnitConfig.KEY_ROOT + "msg.";


    static {
        LOG_LEVEL_PROP = doLoad("personium-log-level.properties");
        LOG_MSG_PROP = doLoad("personium-messages.properties");
    }

    private static Properties doLoad(String file) {
        Properties prop = new Properties();
        prop.clear();
        InputStream is = PersoniumUnitConfig.class.getClassLoader().getResourceAsStream(file);
        try {
            prop.load(is);
        } catch (IOException e) {
            throw new RuntimeException("failed to load config!", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("failed to close config stream", e);
            }
        }
        return prop;
    }

    /**
     * constructor.
     */
    private PersoniumCoreMessageUtils() {
    }

    /**
     * Acquire log level from configuration file.
     * @param code Message code
     * @return log level
     */
    public static Severity getSeverity(String code) {
        String logLevel = LOG_LEVEL_PROP.getProperty(LOG_LEVEL + code);
        Severity severity = null;
        if (Severity.DEBUG.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.DEBUG;
        } else if (Severity.INFO.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.INFO;
        } else if (Severity.WARN.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.WARN;
        } else if (Severity.ERROR.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.ERROR;
        }
        return severity;
    }

    /**
     * Retrieve messages from the configuration file.
     * @param code Message code
     * @return message
     */
    public static String getMessage(String code) {
        String msg = LOG_MSG_PROP.getProperty(LOG_MESSAGE + code);
        if (msg == null) {
            //Exception if log is not defined
            throw new RuntimeException("message undefined for code=[" + code + "].");
        }
        return msg;
    }
}

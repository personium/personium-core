/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import java.text.MessageFormat;

/**
 * Log message with measurement creation class.
 */
public final class ElapsedTimeLog extends PersoniumCoreLog {

    /**
     * WebDAV related.
     */
    public static class Dav {
        /**
         * Write file.
         * {0}: File size
         */
        public static final ElapsedTimeLog FILE_OPERATION_END = create("PL-DV-0006");
    }

    /**
     * Service collection.
     */
    public static class ServiceCollection {
        /**
         * Personium-Engine reley ends.
         */
        public static final ElapsedTimeLog SC_ENGINE_RELAY_END = create("PL-SC-0002");
    }

    private long startTime = 0L;

    ElapsedTimeLog(PersoniumCoreLog coreLog) {
        super(coreLog.code, coreLog.severity, coreLog.message);
    }

    /**
     * Force load inner class.
     * Add an inner class of error classification here if it is added.
     */
    public static void loadConfig() {
        new Dav();
        new ServiceCollection();
    }

    /**
     * It creates and returns a message with a parameter substitution, and the expression of {1} {2} etc. on the error message is a keyword for parameter substitution.
     * @param params Additional message
     * @return PersoniumMeasurmentLog
     */
    public ElapsedTimeLog params(final Object... params) {
        return new ElapsedTimeLog(super.params(params));
    }

    /**
     * It set a message with a parameter substitution, and the expression of {1} {2} etc. on the error message is a keyword for parameter substitution.
     * @param params Additional message
     */
    public void setParams(final Object... params) {
        String messageFormat = PersoniumCoreMessageUtils.getMessage(code);
        this.message = MessageFormat.format(messageFormat, params);
    }

    /**
     * Factory method.
     * @param code log code
     * @return PersoniumMeasurmentLog
     */
    public static ElapsedTimeLog create(String code) {
        return new ElapsedTimeLog(PersoniumCoreLog.create(code));
    }

    /**
     * Set start time.
     */
    public void setStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Log output with time measurement..
     * When outputting the log, display the class name, method name, number of lines, and measurement time of the log output source.
     * Output example)
     * 2019-07-31 15:18:00.558 [main] [INFO ] PersoniumCoreLog [PL-SC-0002] - [EngineRelay] End. (1000ms) - [io.personium.core.PersoniumMeasurementLogTest#testMethod:67]
     */
    public void writeLog() {
        if (this.startTime != 0L) {
            long elapsedTime = System.currentTimeMillis() - this.startTime;
            this.message = this.message.replaceFirst("%time", String.format("%d", elapsedTime));
        }
        StackTraceElement[] ste = new Throwable().getStackTrace();
        doWriteLog("[%s] - %s - [%s#%s:%s]",
                this.code, this.message, ste[1].getClassName(), ste[1].getMethodName(), ste[1].getLineNumber());
    }

}

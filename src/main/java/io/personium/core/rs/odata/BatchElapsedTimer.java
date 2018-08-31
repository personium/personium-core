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
package io.personium.core.rs.odata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.core.rs.odata.ODataBatchResource.BatchPriority;

/**
 *Class for timeout control of Batch.
 */
public class BatchElapsedTimer {
    private static Logger log = LoggerFactory.getLogger(BatchElapsedTimer.class);

    private long breakTimeInMillis = 0;
    private long elapseTimeToBreak = 0;
    private long lastSleepTimeStamp;
    private BatchPriority priority = BatchPriority.LOW;

    private long sleep = PersoniumUnitConfig.getOdataBatchSleepInMillis();
    private long sleepInterval = PersoniumUnitConfig.getOdataBatchSleepIntervalInMillis();

    /**
     *An enumeration type for specifying whether to sleep or not to give Lock to another process.
     */
    public enum Lock {
        /** Sleep to give Lock to another process.*/
        YIELD,
        /** Attempt to acquire Lock without sleeping.*/
        HOLD
    }

    /**
     *constructor.
     *@ param startTimeInMillis Process start time.
     *@ param elapseTimeToBreakInMillis Elapsed time to timeout.
     *@ param priority Whether to sleep to give Lock to another process
     */
    public BatchElapsedTimer(long startTimeInMillis, long elapseTimeToBreakInMillis, BatchPriority priority) {
        breakTimeInMillis = startTimeInMillis + elapseTimeToBreakInMillis;
        elapseTimeToBreak = elapseTimeToBreakInMillis;
        lastSleepTimeStamp = startTimeInMillis;
        this.priority = priority;
    }

    /**
     *At the time of calling, it returns whether it is timeout or not.
     *@ param mode Whether to sleep to give Lock to another process
     *@return true: timeout time has passed. false: not timeout.
     */
    public boolean shouldBreak(Lock mode) {
        long current = System.currentTimeMillis();
        if (BatchPriority.LOW == priority && Lock.YIELD.equals(mode)
                && lastSleepTimeStamp + sleepInterval < current) {
            //If the specified time has elapsed since sleeping last time, sleep to Lock to give it to another process
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                log.warn("Batch request interrupted.", e);
                throw PersoniumCoreException.Server.UNKNOWN_ERROR;
            }
            current = System.currentTimeMillis();
            lastSleepTimeStamp = current;
        }

        //Determine whether the timeout time has elapsed
        return breakTimeInMillis < current;
    }

    /**
     *Get the timeout setting value.
     *@return timeout setting value
     */
    public long getElapseTimeToBreak() {
        return elapseTimeToBreak;
    }

}

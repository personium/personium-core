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
package io.personium.core.model.progress;

import org.json.simple.JSONObject;

/**
 * Interface definition for asynchronous processing situations.
 */
public interface ProgressInfo {

    /**
     * Processing status (status).
     */
    enum STATUS {
        /** processing.*/
        PROCESSING("installation in progress"),
        /** Processing completed (normal termination).*/
        COMPLETED("ready"),
        /** Processing complete (abnormal termination).*/
        FAILED("installation failed"),
        /** Cancellation complete.*/
        CANCELLED("installation cancelled"); //unused

        private String message;

        /**
         * constructor.
         * @param message Message
         */
        STATUS(String message) {
            this.message = message;
        }

        /**
         * A message corresponding to each Enum value is obtained.
         * @return message
         */
        public String value() {
            return message;
        }
    }

    /**
     * Acquires the contents of stored data in JSON format.
     * @return JSON object.
     */
    JSONObject getJsonObject();
}


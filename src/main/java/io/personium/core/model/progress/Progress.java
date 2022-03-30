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

import java.io.Serializable;

/**
 * Asynchronous processing status object.
 */
public class Progress implements Serializable {
    private static final long serialVersionUID = 1L;

    String key;     //Key
    String value;   //JSON string
    Long createdAt; //Creation time

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * constructor.
     * @param key Asynchronous processing status key
     * @param value Asynchronous processing status value (JSON string)
     */
    public Progress(String key, String value) {
        this.key = key;
        this.value = value;
        this.createdAt = Long.valueOf(System.currentTimeMillis());
    }

    /**
     * Delete asynchronous processing status.
     */
    public void delete() {
        ProgressManager.deleteProgress(this.key);
    }
}

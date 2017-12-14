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
package io.personium.core.rule;

/**
 * Class of Action information.
 */
public class ActionInfo {
    private String action;
    private String service;

    /**
     * Constructor.
     * @param action action to be performed
     * @param service url that HTTP POST will be sent if not null
     */
    public ActionInfo(String action, String service) {
        this.action = action;
        this.service = service;
    }

    /**
     * Get value of Action.
     * @return value of Action
     */
    public String getAction() {
        return action;
    }

    /**
     * Get value of Service.
     * @return value of Service
     */
    public String getService() {
        return service;
    }
}

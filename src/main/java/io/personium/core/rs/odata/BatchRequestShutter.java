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
package io.personium.core.rs.odata;

import javax.ws.rs.HttpMethod;

import io.personium.core.PersoniumCoreException;

/**
 * Class that controls execution / skipping after Too Many Concurrent occurs during Batch request.
 */
public class BatchRequestShutter {

    private boolean shuttered = false;

    /**
     * Too Many Conflict True if occurred, false otherwise.
     * @return Too Many Conflict true, false otherwise
     */
    public boolean isShuttered() {
        return shuttered;
    }

    /**
     * Update status of whether Too Many Concurrent occurred during Batch request.
     * @param e Exception raised
     */
    public void updateStatus(Exception e) {
        if (PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS.equals(e)) {
            shuttered = true;
        }
    }

    /**
     * Determine if you can execute individual requests in Batch <br />
     * If Too Many Concurrent has already occurred in the Batch request and it is an update method it is not possible to execute it.
     * @param httpMethod method name
     * @return true: executable, false: not executable
     */
    public boolean accept(String httpMethod) {
        if (!isShuttered()) {
            return true;
        }
        return HttpMethod.GET.equals(httpMethod);
    }

}

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
package io.personium.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.PathSegment;

import io.personium.core.model.lock.ReadDeleteModeLockManager;

/**
 * Class referring to PCS operation mode.
 */
public class PersoniumReadDeleteModeManager {
    private PersoniumReadDeleteModeManager() {
    }

    /**
     * Authorization method in ReadDeleteOnly mode.
     */
    static final Set<String> ACCEPT_METHODS = new HashSet<String>(
            Arrays.asList(
                    HttpMethod.GET,
                    HttpMethod.DELETE,
                    HttpMethod.OPTIONS,
                    HttpMethod.HEAD,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                    "REPORT"
                    )
            );

    /**
     * Check whether it is an executable method in ReadDeleteOnly mode.
     * @param method Request method
     * @return boolean true: authorization method false: unauthorized method
     */
    public static boolean isAllowedMethod(String method) {
        //If it is not in the ReadDeleteOnly mode, processing is permitted
        if (!ReadDeleteModeLockManager.isReadDeleteOnlyMode()) {
            return true;
        }

        if (!ACCEPT_METHODS.contains(method)) {
            return false;
        }

        return true;
    }

    /**
     * When the operation mode of the PCS is the ReadDeleteOnly mode, only the reference system request is permitted.
     * If it is not permitted, raise an exception and process it with ExceptionMapper.
     * @param method Request method
     * @param pathSegment path segment
     */
    public static void checkReadDeleteOnlyMode(String method, List<PathSegment> pathSegment) {
        //If it is not in the ReadDeleteOnly mode, processing is permitted
        if (!ReadDeleteModeLockManager.isReadDeleteOnlyMode()) {
            return;
        }

        //Authentication processing is a POST method but does not write, so it is allowed as an exception
        if (isAuthPath(pathSegment)) {
            return;
        }

        //Since $ batch is a POST method but requests for reference and deletion can also be executed
        //Write batch processing as error within $ batch
        if (isBatchPath(pathSegment)) {
            return;
        }

        //In the ReadDeleteOnly mode, if it is an enabled method, processing is permitted
        if (ACCEPT_METHODS.contains(method)) {
            return;
        }

        throw PersoniumCoreException.Server.READ_DELETE_ONLY;
    }

    private static boolean isBatchPath(List<PathSegment> pathSegment) {
        String lastPath = pathSegment.get(pathSegment.size() - 1).getPath();
        if ("$batch".equals(lastPath)) {
            return true;
        }
        return false;
    }

    private static boolean isAuthPath(List<PathSegment> pathSegment) {
        //Since the authentication path is / cell name / __ token or / cell name / __ authz, set the size to 2
        if (pathSegment.size() == 2) {
            String lastPath = pathSegment.get(1).getPath();
            if ("__token".equals(lastPath) || "__authz".equals(lastPath)) {
                return true;
            }
        }
        return false;
    }
}

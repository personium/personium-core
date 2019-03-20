/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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
package io.personium.core.auth.hash;

import io.personium.core.odata.OEntityWrapper;

/**
 * A utility hash password interface.
 */
public interface HashPassword {

    /**
     * get algorithm name.
     * @return algorithm name
     */
    String algorithmName();

    /**
     * Hash string of password string.
     * @param passwd raw password string
     * @return hashed password string
     */
    String hashPassword(String passwd);

    /**
     * get hash attrbutes.
     * @return hash attrbutes.
     */
    String hashAttrbutes();

    /**
     * matches password.
     * @param oew account
     * @param rawPasswd raw password string
     * @return true if matches password.
     */
    boolean matches(OEntityWrapper oew, String rawPasswd);
}

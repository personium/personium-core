/**
 * Personium
 * Copyright 2019-2021 Personium Project Authors
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
package io.personium.core.model;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Class dealing with cell specific key information.
 */
public interface CellKeyPair {

    /**
     * Get Key ID.
     * @return Key ID
     */
    public String getKeyId();

    /**
     * Get Public key.
     * @return Public key
     */
    public byte[] getPublicKeyBytes();

    /**
     * Get Public key.
     * @return Public key
     */
    public PublicKey getPublicKey();

    /**
     * Get Private key.
     * @return Private key
     */
    public byte[] getPrivateKeyBytes();

    /**
     * Get Private key.
     * @return Private key
     */
    public PrivateKey getPrivateKey();
}

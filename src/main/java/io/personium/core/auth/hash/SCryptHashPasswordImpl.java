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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;

/**
 * A utility SCrypt hash password.
 */
public class SCryptHashPasswordImpl implements HashPassword {
    /** Logger. */
    static Logger log = LoggerFactory.getLogger(SCryptHashPasswordImpl.class);

    /** hash algorithm name. */
    public static final String HASH_ALGORITHM_NAME = "scrypt";
    /** hash attribute keyLength. */
    public static final String HASH_ATTRIBUTE_KEYLENGTH = "keyLength";
    /** dummy Hashed credential. */
    private static String dummyCred = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAlgorithmName() {
        return HASH_ALGORITHM_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createHashPassword(String passwd) {
        if (passwd == null) {
            return null;
        }

        int cpuCost = PersoniumUnitConfig.getSCryptCpuCost();
        int memoryCost = PersoniumUnitConfig.getSCryptMemoryCost();
        int parallelization = PersoniumUnitConfig.getSCryptParallelization();
        int keyLength = PersoniumUnitConfig.getSCryptKeyLength();
        int saltLength = PersoniumUnitConfig.getSCryptSaltLength();
        PasswordEncoder passwordEncoder = new SCryptPasswordEncoder(
                cpuCost, memoryCost, parallelization, keyLength, saltLength);

        // password hash.
        String str2hash = passwordEncoder.encode(passwd);
        return str2hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public String createHashAttrbutes() {
        // Set attributes.
        JSONObject json = new JSONObject();
        json.put(HASH_ATTRIBUTE_KEYLENGTH, PersoniumUnitConfig.getSCryptKeyLength());
        return json.toJSONString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(Account account, String rawPasswd) {

        // Parameters other than keyLength are required to generate Instanse but are not actually used.
        // (Obtained from hash at verification)
        int cpuCost = PersoniumUnitConfig.getSCryptCpuCost();
        int memoryCost = PersoniumUnitConfig.getSCryptMemoryCost();
        int parallelization = PersoniumUnitConfig.getSCryptParallelization();
        int saltLength = PersoniumUnitConfig.getSCryptSaltLength();

        // get hashed credential.
        String cred = null;
        if (account != null) {
            cred = account.credential;
        }

        // get key length.
        Integer keyLength = getKeyLenghtFromOEntityWrapper(account);
        if (keyLength == null) {
            keyLength = PersoniumUnitConfig.getSCryptKeyLength();
        }

        // matchs
        PasswordEncoder passwordEncoder = new SCryptPasswordEncoder(
                cpuCost, memoryCost, parallelization, keyLength, saltLength);
        if (cred != null && !cred.isEmpty()) {
            return passwordEncoder.matches(rawPasswd, cred);
        } else {
            // In order to cope with the todo time exploiting attack, even if an ID is not found, processing is done uselessly.
            if (dummyCred == null) {
                dummyCred = passwordEncoder.encode("dummyCred");
            }
            passwordEncoder.matches(rawPasswd, dummyCred);
            return false;
        }
    }

    /**
     * get keyLength from OEntityWrapper.
     * @param oew OEntityWrapper
     * @return keyLength
     */
    private Integer getKeyLenghtFromOEntityWrapper(Account account) {
        if (account == null || account.hashAttributes == null) {
            return null;
        }

        Integer keyLength = null;
        try {
            JSONParser parser = new JSONParser();
            JSONObject attributes;
            attributes = (JSONObject) parser.parse(account.hashAttributes);
            if (attributes.containsKey(HASH_ATTRIBUTE_KEYLENGTH)) {
                keyLength = new Integer(attributes.get(HASH_ATTRIBUTE_KEYLENGTH).toString());
            }
        } catch (Exception e) {
            throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(account.hashAttributes);
        }
        return keyLength;
    }
}

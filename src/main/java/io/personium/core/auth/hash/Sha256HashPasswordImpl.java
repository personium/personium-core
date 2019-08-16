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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang.CharEncoding;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;
import io.personium.core.odata.OEntityWrapper;

/**
 * A utility SHA-256 hash password.
 */
public class Sha256HashPasswordImpl implements HashPassword {

    /** hash algorithm name. */
    public static final String HASH_ALGORITHM_NAME = "sha-256";

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

        // DC0 Ruby Code
        // Digest::SHA256.hexdigest(pw + "Password hash salt value")
        String str2hash = passwd + PersoniumUnitConfig.getAuthPasswordSalt();
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM_NAME);
            byte[] digestBytes = md.digest(str2hash.getBytes(CharEncoding.UTF_8));
            //Although its data efficiency is better, this implementation is made for compatibility with DC 0.
            return CommonUtils.byteArray2HexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createHashAttrbutes() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(OEntityWrapper oew, String rawPasswd) {
        // In order to cope with the todo time exploiting attack, even if an ID is not found, processing is done uselessly.
        String cred = null;
        if (oew != null) {
            cred = (String) oew.get(Account.HASHED_CREDENTIAL);
        }
        String hCred = this.createHashPassword(rawPasswd);
        if (hCred.equals(cred)) {
            return true;
        }

        return false;
    }
}

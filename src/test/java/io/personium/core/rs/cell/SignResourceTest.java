/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.fusesource.hawtbuf.BufferInputStream;
import org.jose4j.jws.JsonWebSignature;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellKeyPair;
import io.personium.core.model.CellKeys;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.lock.UnitUserLockManager;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for SignResource.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AccessContext.class, ModelFactory.class, PersoniumUnitConfig.class, UnitUserLockManager.class })
@Category({ Unit.class })
public class SignResourceTest {

    /** Test class. */
    private SignResource signResource;

    // keyId for mock
    String keyId;
    // public key for mock
    PublicKey publicKey;
    // private key for mock
    PrivateKey privateKey;

    /**
     * Initializing mock for testing
     * @throws Exception
     */
    @Before
    public void initialize() throws Exception {

        // Initialize key pair
        String KEY_ALGORITHM = "RSA";
        int KEY_SIZE = 2048;
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        KeyFactory factoty = KeyFactory.getInstance(KEY_ALGORITHM);

        RSAPublicKeySpec publicKeySpec = factoty.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
        RSAPrivateKeySpec privateKeySpec = factoty.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

        publicKey = factoty.generatePublic(publicKeySpec);
        privateKey = factoty.generatePrivate(privateKeySpec);
        keyId = UUID.randomUUID().toString();

        // Initialize mocks
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        CellKeys cellKeys = mock(CellKeys.class);
        CellKeyPair cellKeyPair = mock(CellKeyPair.class);

        doReturn(publicKey).when(cellKeyPair).getPublicKey();
        doReturn(privateKey).when(cellKeyPair).getPrivateKey();
        doReturn(keyId).when(cellKeyPair).getKeyId();
        doReturn(cellKeyPair).when(cellKeys).getCellKeyPairs();
        doReturn(cellKeys).when(cellCmp).getCellKeys();

        this.signResource = new SignResource(cellRsCmp, cellCmp);
    }

    /**
     * Internal helper function to create stream with specified length
     * @param size size of unit
     * @param times repetition count
     * @throws Exception
     */
    private void signAndVerify(int size, int times) throws Exception {
        byte[] dummyToSign = new byte[size];
        new Random().nextBytes(dummyToSign);

        try (InputStream inputStream = createInputStream(dummyToSign, times)) {
            String vcString = signResource.signJWS(inputStream);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(vcString);
            jws.setKey(publicKey);

            boolean signatureVerified = jws.verifySignature();

            assertEquals("KeyID is not matched", this.keyId, jws.getKeyIdHeaderValue());
            try (InputStream orig = createInputStream(dummyToSign, times);
                    InputStream result = new BufferInputStream(jws.getPayloadBytes())) {
                assertTrue("Payload is not matched", IOUtils.contentEquals(orig, result));
            }
            assertTrue("Verifing JWS is failed", signatureVerified);
        }
    }

    /**
     * Create InputStream
     * @param data data to be repeated
     * @param times repeated count
     * @return InputStream which data is repeated specified count.
     */
    private InputStream createInputStream(byte[] data, int times) {
        return new InputStream() {
            private long pos = 0;
            private final long total = (long) data.length * times;

            public int read() {
                return pos < total ? data[(int) (pos++ % data.length)] : -1;
            }
        };
    }

    /**
     * Testing that signJWS function can generate valid jws with random input
     */
    @Test
    public void signJWS_with_random_bufferinputstream_return_valid_jws() {
        try {
            signAndVerify(1024, 32);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Testing that signJWS function can generate valid jws with zero-size input
     */
    @Test
    public void signJWS_with_zero_length_bufferinputstream_return_valid_jws() {
        try {
            signAndVerify(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * Testing that signJWS throws IllegalArgumentException when null is input
     */
    @Test(expected = IllegalArgumentException.class)
    public void signJWS_with_null_inputstream_throws_illegalargumentexception() {
        signResource.signJWS((InputStream) null);
    }

    /**
     * Testing that signJWS can generate valid jws
     * @throws Exception
     */
    @Test
    @Ignore
    public void signJWS_with_too_large_inputstream_throws_outofmemory() throws Exception {
        signAndVerify(1024, 1024 * 1024 * 1024); // 1TiB
    }
}

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.InputStream;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Random;

import org.fusesource.hawtbuf.BufferInputStream;
import org.jose4j.jws.JsonWebSignature;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.CellKeyPair;
import io.personium.core.model.CellKeys;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.impl.fs.CellKeyPairFsImpl;
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

    /**
     * Mock CellResource.
     * @param cell cell
     * @param cellCmp cellCmp
     * @param cellRsCmp cellRsCmp
     * @param accessContext accessContext
     * @throws Exception Unintended exception in test
     */
    private void initCellResource(
            Cell cell, CellCmp cellCmp, CellRsCmp cellRsCmp, AccessContext accessContext) throws Exception {
        // Mock settings
        doReturn(cell).when(accessContext).getCell();
        PowerMockito.mockStatic(ModelFactory.class);
        PowerMockito.doReturn(cellCmp).when(ModelFactory.class, "cellCmp", any());
        doReturn(true).when(cellCmp).exists();
        PowerMockito.whenNew(CellRsCmp.class).withAnyArguments().thenReturn(cellRsCmp);
        PowerMockito.mockStatic(PersoniumUnitConfig.class);
        PowerMockito.doReturn("u0").when(PersoniumUnitConfig.class, "getEsUnitPrefix");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockType");
        PowerMockito.doReturn("0").when(PersoniumUnitConfig.class, "getLockRetryInterval");
        PowerMockito.doReturn("0").when(PersoniumUnitConfig.class, "getLockRetryTimes");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockMemcachedHost");
        PowerMockito.doReturn(null).when(PersoniumUnitConfig.class, "getLockMemcachedPort");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountValidAuthnInterval");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountLockCount");
        PowerMockito.doReturn(0).when(PersoniumUnitConfig.class, "getAccountLockTime");
        PowerMockito.mockStatic(UnitUserLockManager.class);
        PowerMockito.doReturn(false).when(UnitUserLockManager.class, "hasLockObject", anyString());
        doReturn(Cell.STATUS_NORMAL).when(cellCmp).getCellStatus();
        signResource = spy(new SignResource(cellRsCmp, cellCmp));
    }

    /**
     * Testing that sign
     * @throws Exception
     */
    @Test
    public void Test_that_signResource_generates_valid_jws_from_InputStream() throws Exception {
        // Mock settings
        AccessContext accessContext = PowerMockito.mock(AccessContext.class);
        Cell cell = mock(Cell.class);
        CellCmp cellCmp = mock(CellCmp.class);
        CellRsCmp cellRsCmp = mock(CellRsCmp.class);
        initCellResource(cell, cellCmp, cellRsCmp, accessContext);

        doNothing().when(accessContext).updateBasicAuthenticationStateForResource(null);
        doReturn(AccessContext.TYPE_UNIT_MASTER).when(accessContext).getType();

        cellCmp.getCellKeys();
        CellKeys cellKeys = mock(CellKeys.class);
        CellKeyPair tmpKeyPair = CellKeyPairFsImpl.newInstance(Paths.get("tmp"));

        doReturn(tmpKeyPair).when(cellKeys).getCellKeyPairs();
        doReturn(cellKeys).when(cellCmp).getCellKeys();

        byte[] dummyToSign = new byte[1024 * 32];
        new Random().nextBytes(dummyToSign);

        try (InputStream inputStream = new BufferInputStream(dummyToSign)) {
            String vcString = signResource.signJWS(inputStream);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(vcString);

            PublicKey publicKey = tmpKeyPair.getPublicKey();
            jws.setKey(publicKey);

            boolean signatureVerified = jws.verifySignature();

            assertEquals("KeyID is not matched", tmpKeyPair.getKeyId(), jws.getKeyIdHeaderValue());
            assertArrayEquals("Payload is not matched", dummyToSign, jws.getPayloadBytes());
            assertTrue("Verifing JWS is failed", signatureVerified);
        }
    }
}

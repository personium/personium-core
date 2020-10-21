/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.test.io;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumUnitConfig.BinaryData;
import io.personium.core.bar.BarFileInstaller;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.test.categories.Unit;

/**
 * BarFileのバリデートのユニットテストクラス.
 */
@Category({Unit.class })
public class BarFileTemporaryFileTest {

    private static final String RESOURCE_PATH = "requestData/barInstallUnit";


    /**
     * fsync ON.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testStoreTemporaryBarFile() throws Exception {
        boolean fsyncEnabled = PersoniumUnitConfig.getFsyncEnabled();
        PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, "true");
        try {
            CellEsImpl cell = new CellEsImpl();
            cell.setId("hogeCell");
            BarFileInstaller bfi = Mockito.spy(new BarFileInstaller(cell, "hogeBox", null));
            Method method = BarFileInstaller.class.getDeclaredMethod(
                    "storeTemporaryBarFile", new Class<?>[] {InputStream.class});
            method.setAccessible(true);
            //any file
            method.invoke(bfi, new FileInputStream("pom.xml"));
            Mockito.verify(bfi, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.anyObject());
        } finally {
            PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, String.valueOf(fsyncEnabled));
        }
    }

    /**
     * fsync OFF.
     * FileDescriptor#sync() should never be called.
     * @throws Exception .
     */
    @Test
    public void testStoreTemporaryBarFile2() throws Exception {
        boolean fsyncEnabled = PersoniumUnitConfig.getFsyncEnabled();
        PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, "false");
        try {
            CellEsImpl cell = new CellEsImpl();
            cell.setId("hogeCell");
            BarFileInstaller bfi = Mockito.spy(new BarFileInstaller(cell, "hogeBox", null));
            Method method = BarFileInstaller.class.getDeclaredMethod(
                    "storeTemporaryBarFile", new Class<?>[] {InputStream.class});
            method.setAccessible(true);
            //any file
            method.invoke(bfi, new FileInputStream("pom.xml"));
            Mockito.verify(bfi, Mockito.never()).sync((FileDescriptor) Mockito.anyObject());
        } finally {
            PersoniumUnitConfig.set(BinaryData.FSYNC_ENABLED, String.valueOf(fsyncEnabled));
        }
    }
}

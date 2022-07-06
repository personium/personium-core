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
package io.personium.core.model.file;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import io.personium.common.file.FileDataAccessor;
import io.personium.test.categories.Unit;

/**
 * BinaryDataAccessorユニットテストクラス.
 */
@Category({Unit.class })
public class BinaryDataAccessorTest {

    Path tmpFilePath;

    /**
     * Prepare for test.
     */
    @Before
    public void prepareForTest() {
        tmpFilePath = null;
        try {
            tmpFilePath = Files.createTempFile("dummy", ".txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Destroy resources used in test.
     */
    @After
    public void destroyResources() {
        try {
            if (tmpFilePath != null) {
                Files.delete(tmpFilePath);
            }
        } catch (IOException e) {
                throw new RuntimeException(e);
        }
    }

    /**
     * fsync ON  & FileInputStream.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testCloseOutputStream() throws Exception {
        FileDataAccessor bda = Mockito.spy(new FileDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = FileDataAccessor.class.getDeclaredMethod(
                "closeOutputStream", new Class<?>[] {OutputStream.class});
        closeOSMethod.setAccessible(true);
        closeOSMethod.invoke(bda, new FileOutputStream(tmpFilePath.toFile()));
        Mockito.verify(bda, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.any());
    }

    /**
     * fsync OFF  & FileInputStream.
     * FileDescriptor#sync() should never be called.
     * @throws Exception .
     */
    @Test
    public void testCloseOutputStream2() throws Exception {
        FileDataAccessor bda = Mockito.spy(new FileDataAccessor("/dummy", "dummy", true, false));
        Method closeOSMethod = FileDataAccessor.class.getDeclaredMethod(
                "closeOutputStream", new Class<?>[] {OutputStream.class});
        closeOSMethod.setAccessible(true);
        closeOSMethod.invoke(bda, new FileOutputStream(tmpFilePath.toFile()));
        Mockito.verify(bda, Mockito.never()).sync((FileDescriptor) Mockito.any());
    }

    /**
     * fsync ON  & FilterOutputStream.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testCloseOutputStream3() throws Exception {
        FileDataAccessor bda = Mockito.spy(new FileDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = FileDataAccessor.class.getDeclaredMethod(
                "closeOutputStream", new Class<?>[] {OutputStream.class});
        closeOSMethod.setAccessible(true);
        closeOSMethod.invoke(bda, new BufferedOutputStream(new FileOutputStream(tmpFilePath.toFile())));
        Mockito.verify(bda, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.any());
    }

    /**
     * fsyn ON  & Neither FileOutputStream nor FilterOutputStream.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testCloseOutputStream4() throws Exception {
        FileDataAccessor bda = Mockito.spy(new FileDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = FileDataAccessor.class.getDeclaredMethod(
                "closeOutputStream", new Class<?>[] {OutputStream.class});
        closeOSMethod.setAccessible(true);
        closeOSMethod.invoke(bda, new ByteArrayOutputStream());
        Mockito.verify(bda, Mockito.never()).sync((FileDescriptor) Mockito.any());
    }
}

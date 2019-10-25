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
package io.personium.core.model.file;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import io.personium.common.file.FileDataAccessor;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;

/**
 * BinaryDataAccessorユニットテストクラス.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class })
public class BinaryDataAccessorTest {

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
        closeOSMethod.invoke(bda, new FileOutputStream("hoge"));
        Mockito.verify(bda, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.anyObject());
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
        closeOSMethod.invoke(bda, new FileOutputStream("hoge"));
        Mockito.verify(bda, Mockito.never()).sync((FileDescriptor) Mockito.anyObject());
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
        closeOSMethod.invoke(bda, new BufferedOutputStream(new FileOutputStream("hoge")));
        Mockito.verify(bda, Mockito.atLeast(1)).sync((FileDescriptor) Mockito.anyObject());
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
        Mockito.verify(bda, Mockito.never()).sync((FileDescriptor) Mockito.anyObject());
    }
}

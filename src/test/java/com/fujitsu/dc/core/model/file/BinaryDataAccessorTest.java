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
package com.fujitsu.dc.core.model.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;

/**
 * BinaryDataAccessorユニットテストクラス.
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class BinaryDataAccessorTest {

    /**
     * 既存ディレクトリにファイルの登録が可能な事を確認する.
     * DcCoreConfigを利用するため、coreにテストを記述する
     */
    @Test
    public void 既存ディレクトリにファイルの登録が可能な事を確認する() {
        String filename1 = "PcsInnov1";
        String filename2 = "PcsInnov2";
        String filename3 = "Pcs3nnov3";
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                DcCoreConfig.getFsyncEnabled());
        try {
            long dataSize = "testhoge".getBytes().length;
            // ディレクトリを作成
            InputStream is = new ByteArrayInputStream("testhoge".getBytes());
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            try {
                long fileSize = binaryDataAccessor.create(bufferedInput, filename1);
                assertEquals(dataSize, fileSize);
            } catch (BinaryDataAccessException e) {
                fail(e.getMessage());
            }
            // 既存ディレクトリにファイル登録
            dataSize = "testhuga".getBytes().length;
            InputStream is2 = new ByteArrayInputStream("testhuga".getBytes());
            BufferedInputStream bufferedInput2 = new BufferedInputStream(is2);
            try {
                long fileSize = binaryDataAccessor.create(bufferedInput2, filename2);
                assertEquals(dataSize, fileSize);
            } catch (BinaryDataAccessException e) {
                fail(e.getMessage());
            }
            // 親ディレクトリにファイル登録
            InputStream is3 = new ByteArrayInputStream("testhugahoge".getBytes());
            BufferedInputStream bufferedInput3 = new BufferedInputStream(is3);
            dataSize = "testhugahoge".getBytes().length;
            try {
                long fileSize = binaryDataAccessor.create(bufferedInput3, filename3);
                assertEquals(dataSize, fileSize);
            } catch (BinaryDataAccessException e) {
                fail(e.getMessage());
            }
        } finally {
            // ファイル削除の確認
            try {
                binaryDataAccessor.delete(filename3 + ".tmp");
                binaryDataAccessor.delete(filename2 + ".tmp");
                binaryDataAccessor.delete(filename1 + ".tmp");
            } catch (BinaryDataAccessException e) {
                fail(e.getMessage());
            }
        }

    }

    /**
     * ファイルを物理削除した場合ファイルが削除されていること.
     * DcCoreConfigを利用するため、coreにテストを記述する
     * @throws BinaryDataAccessException バイナリアクセスに失敗
     */
    @Test
    public void ファイルを物理削除した場合ファイルが削除されていること() throws BinaryDataAccessException {
        String filename1 = "PcsInnovPhysicalDelete1";
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                DcCoreConfig.getFsyncEnabled());
        try {
            // テンポラリファイルファイル作成
            long dataSize = "testhoge".getBytes().length;
            InputStream is = new ByteArrayInputStream("testhoge".getBytes());
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            long fileSize = binaryDataAccessor.create(bufferedInput, filename1);
            assertEquals(dataSize, fileSize);

            // 「XXX.tmp」→「XXX」
            binaryDataAccessor.copyFile(filename1);

            // 物理削除
            binaryDataAccessor.deletePhysicalFile(filename1);

            // 削除確認
            boolean isExists = binaryDataAccessor.existsForFilename(filename1);
            assertFalse(isExists);
        } finally {
            // ファイル削除
            binaryDataAccessor.deletePhysicalFile(filename1);
        }
    }

    /**
     * ファイルの物理削除で存在しないファイルを指定した場合例外がスローされないこと.
     * DcCoreConfigを利用するため、coreにテストを記述する
     * @throws BinaryDataAccessException バイナリアクセスに失敗
     */
    @Test
    public void ファイルの物理削除で存在しないファイルを指定した場合例外がスローされないこと() throws BinaryDataAccessException {
        String filename1 = "PcsInnovPhysicalDelete1";
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                DcCoreConfig.getFsyncEnabled());
        // 物理削除
        binaryDataAccessor.deletePhysicalFile(filename1);

        // 削除確認
        boolean isExists = binaryDataAccessor.existsForFilename(filename1);
        assertFalse(isExists);
    }

    /**
     * 物理削除設定を指定していない場合ファイルが論理削除されていること.
     * DcCoreConfigを利用するため、coreにテストを記述する
     * @throws BinaryDataAccessException バイナリアクセスに失敗
     */
    @Test
    public void 物理削除設定を指定していない場合ファイルが論理削除されていること() throws BinaryDataAccessException {
        String filename1 = "PcsInnovPhysicalDelete1";
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
                DcCoreConfig.getFsyncEnabled());
        try {
            // テンポラリファイルファイル作成
            long dataSize = "testhoge".getBytes().length;
            InputStream is = new ByteArrayInputStream("testhoge".getBytes());
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            long fileSize = binaryDataAccessor.create(bufferedInput, filename1);
            assertEquals(dataSize, fileSize);

            // 「XXX.tmp」→「XXX」
            binaryDataAccessor.copyFile(filename1);

            // ファイル削除
            binaryDataAccessor.delete(filename1);

            // 削除確認
            boolean isExists = binaryDataAccessor.existsForFilename(filename1);
            assertFalse(isExists);
            // 論理削除ファイルが存在することの確認
            isExists = binaryDataAccessor.existsForFilename(filename1 + ".deleted");
            assertTrue(isExists);

        } finally {
            // ファイル削除
            binaryDataAccessor.deletePhysicalFile(filename1);
            binaryDataAccessor.deletePhysicalFile(filename1 + ".deleted");
        }
    }

    /**
     * 物理削除設定の場合ファイルの実体が削除されていること.
     * DcCoreConfigを利用するため、coreにテストを記述する
     * @throws BinaryDataAccessException バイナリアクセスに失敗
     */
    @Test
    public void 物理削除設定の場合ファイルの実体が削除されていること() throws BinaryDataAccessException {
        String filename1 = "PcsInnovPhysicalDelete1";
        boolean isPhysicalDeleteMode = true;
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(), null,
                isPhysicalDeleteMode, DcCoreConfig.getFsyncEnabled());
        try {
            // テンポラリファイルファイル作成
            long dataSize = "testhoge".getBytes().length;
            InputStream is = new ByteArrayInputStream("testhoge".getBytes());
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            long fileSize = binaryDataAccessor.create(bufferedInput, filename1);
            assertEquals(dataSize, fileSize);

            // 「XXX.tmp」→「XXX」
            binaryDataAccessor.copyFile(filename1);

            // ファイル削除
            binaryDataAccessor.delete(filename1);

            // 削除確認
            boolean isExists = binaryDataAccessor.existsForFilename(filename1);
            assertFalse(isExists);
            // 論理削除ファイルが存在しないことの確認
            isExists = binaryDataAccessor.existsForFilename(filename1 + ".deleted");
            assertFalse(isExists);

        } finally {
            // ファイル削除
            binaryDataAccessor.deletePhysicalFile(filename1);
            binaryDataAccessor.deletePhysicalFile(filename1 + ".deleted");
        }
    }

    /**
     * 論理削除設定の場合ファイルが論理削除されていること.
     * DcCoreConfigを利用するため、coreにテストを記述する
     * @throws BinaryDataAccessException バイナリアクセスに失敗
     */
    @Test
    public void 論理削除設定の場合ファイルが論理削除されていること() throws BinaryDataAccessException {
        String filename1 = "PcsInnovPhysicalDelete1";
        boolean isPhysicalDeleteMode = false;
        BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(), null,
                isPhysicalDeleteMode, DcCoreConfig.getFsyncEnabled());
        try {
            // テンポラリファイルファイル作成
            long dataSize = "testhoge".getBytes().length;
            InputStream is = new ByteArrayInputStream("testhoge".getBytes());
            BufferedInputStream bufferedInput = new BufferedInputStream(is);
            long fileSize = binaryDataAccessor.create(bufferedInput, filename1);
            assertEquals(dataSize, fileSize);

            // 「XXX.tmp」→「XXX」
            binaryDataAccessor.copyFile(filename1);

            // ファイル削除
            binaryDataAccessor.delete(filename1);

            // 削除確認
            boolean isExists = binaryDataAccessor.existsForFilename(filename1);
            assertFalse(isExists);
            // 論理削除ファイルが存在することの確認
            isExists = binaryDataAccessor.existsForFilename(filename1 + ".deleted");
            assertTrue(isExists);

        } finally {
            // ファイル削除
            binaryDataAccessor.deletePhysicalFile(filename1);
            binaryDataAccessor.deletePhysicalFile(filename1 + ".deleted");
        }
    }

    /**
     * fsync ON  & FileInputStream.
     * FileDescriptor#sync() should be called.
     * @throws Exception .
     */
    @Test
    public void testCloseOutputStream() throws Exception {
        BinaryDataAccessor bda = Mockito.spy(new BinaryDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = BinaryDataAccessor.class.getDeclaredMethod(
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
        BinaryDataAccessor bda = Mockito.spy(new BinaryDataAccessor("/dummy", "dummy", true, false));
        Method closeOSMethod = BinaryDataAccessor.class.getDeclaredMethod(
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
        BinaryDataAccessor bda = Mockito.spy(new BinaryDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = BinaryDataAccessor.class.getDeclaredMethod(
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
        BinaryDataAccessor bda = Mockito.spy(new BinaryDataAccessor("/dummy", "dummy", true, true));
        Method closeOSMethod = BinaryDataAccessor.class.getDeclaredMethod(
                "closeOutputStream", new Class<?>[] {OutputStream.class});
        closeOSMethod.setAccessible(true);
        closeOSMethod.invoke(bda, new ByteArrayOutputStream());
        Mockito.verify(bda, Mockito.never()).sync((FileDescriptor) Mockito.anyObject());
    }
}

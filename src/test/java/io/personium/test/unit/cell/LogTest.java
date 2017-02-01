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
package io.personium.test.unit.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.rs.cell.LogResource;
import io.personium.test.categories.Unit;

/**
 * LogResourceユニットテストクラス.
 */
@Category({Unit.class })
public class LogTest {

    /**
     * テスト用LogResourceクラス.
     */
    private class TestLogResource extends LogResource {
        TestLogResource() {
            super(null, null, null);
        }

        /**
         * イベントログのCollection名チェック.
         * @param collectionName Collection名
         * @return true: 正しい、false: 誤り
         */
        @Override
        public boolean isValidLogCollection(String collectionName) {
            return super.isValidLogCollection(collectionName);
        }

        /**
         * ファイルの存在チェック.
         * @param fileName ファイル名
         * @return true: 存在する、false: 存在しない
         */
        @Override
        protected boolean isValidLogFile(String collection, String fileName) {
            return super.isValidLogFile(collection, fileName);
        }
    }

    /**
     * ログコレクション名がcurrentの場合trueが返却されること.
     */
    @Test
    public void ログコレクション名がcurrentの場合trueが返却されること() {
        String collectionName = "current";

        TestLogResource logResource = new TestLogResource();

        boolean res = logResource.isValidLogCollection(collectionName);
        assertTrue(res);
    }

    /**
     * ログコレクション名がarchiveの場合trueが返却されること.
     */
    @Test
    public void ログコレクション名がarchiveの場合trueが返却されること() {
        String collectionName = "archive";

        TestLogResource logResource = new TestLogResource();

        boolean res = logResource.isValidLogCollection(collectionName);
        assertTrue(res);
    }

    /**
     * ログコレクション名が不正な文字列の場合falseが返却されること.
     */
    @Test
    public void ログコレクション名が不正な文字列の場合falseが返却されること() {
        TestLogResource logResource = new TestLogResource();

        String collectionName = "test";
        boolean res = logResource.isValidLogCollection(collectionName);
        assertFalse(res);

        collectionName = "";
        res = logResource.isValidLogCollection(collectionName);
        assertFalse(res);

        collectionName = null;
        res = logResource.isValidLogCollection(collectionName);
        assertFalse(res);
    }

    /**
     * ログファイル名がdefault.logの場合tureが返却されること.
     */
    @Test
    public void ログファイル名がdefault_logの場合tureが返却されること() {
        String fileName = "default.log";

        TestLogResource logResource = new TestLogResource();

        boolean res = logResource.isValidLogFile("current", fileName);
        assertTrue(res);
    }

    /**
     * ログファイル名が不正な文字列の場合falseが返却されること.
     */
    @Test
    public void ログファイル名が不正な文字列の場合falseが返却されること() {
        TestLogResource logResource = new TestLogResource();

        String fileName = "error.log";
        boolean res = logResource.isValidLogFile("current", fileName);
        assertFalse(res);

        fileName = "";
        res = logResource.isValidLogFile("current", fileName);
        assertFalse(res);

        fileName = null;
        res = logResource.isValidLogFile("current", fileName);
        assertFalse(res);
    }

    /**
     * 引数に空文字列lを渡した場合_レスポンスボディに空文字列が入ったSC_OKレスポンスが返る.
     */
    @Test
    public void 引数に空文字列lを渡した場合_レスポンスボディに空文字列が入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            String filename = "";
            Object result = method.invoke(logResource, new Object[] {"current", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(HttpStatus.SC_OK, ((Response) result).getStatus());
            assertTrue(((Response) result).getEntity() instanceof String);
            assertEquals(0, ((String) ((Response) result).getEntity()).length());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 引数に存在しないファイルパスを渡した場合_レスポンスボディに空文字列が入ったSC_OKレスポンスが返る.
     */
    @Test
    public void 引数に存在しないファイルパスを渡した場合_レスポンスボディに空文字列が入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            String filename = "/non-existing-file-path";
            Object result = method.invoke(logResource, new Object[] {"current", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(HttpStatus.SC_OK, ((Response) result).getStatus());
            assertTrue(((Response) result).getEntity() instanceof String);
            assertEquals(0, ((String) ((Response) result).getEntity()).length());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 引数に存在するファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る.
     * ※ レスポンスに返されるログの内容は、本Unitテストでは検査不可なため、別途検証する。
     */
    @Test
    public void 引数に存在するファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            File file = File.createTempFile("TestFile", "log");
            file.deleteOnExit();

            final String logContent = "a,b,c\n" + "x,y,z\n";
            FileWriter writer = new FileWriter(file);
            try {
                writer.write(logContent);
            } finally {
                writer.close();
            }

            String filename = file.getAbsolutePath();
            Object result = method.invoke(logResource, new Object[] {"current", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(((Response) result).getStatus(), HttpStatus.SC_OK);

            assertTrue(((Response) result).getEntity() instanceof InputStream);

            InputStream in = (InputStream) ((Response) result).getEntity();
            String out = new String(IOUtils.toByteArray(in), "UTF-8");
            assertEquals(logContent, out);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 引数に存在する圧縮ファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る.
     * ※ レスポンスに返されるログの内容は、本Unitテストでは検査不可なため、別途検証する。
     */
    @Test
    public void 引数に存在する圧縮ファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            File file = File.createTempFile("TestFile", "log");
            file.deleteOnExit();

            final String logContent = "a,b,c\n" + "x,y,z\n";
            FileWriter writer = new FileWriter(file);
            try {
                writer.write(logContent);
            } finally {
                writer.close();
            }

            String filename = file.getAbsolutePath();
            createZip(filename + ".zip", new File[] {file });
            // zipの中身のファイルを削除する
            file.delete();

            // ログ取得実行
            Object result = method.invoke(logResource, new Object[] {"archive", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(((Response) result).getStatus(), HttpStatus.SC_OK);

            assertTrue(((Response) result).getEntity() instanceof InputStream);

            InputStream in = (InputStream) ((Response) result).getEntity();
            String out = new String(IOUtils.toByteArray(in), "UTF-8");
            assertEquals(logContent, out);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 引数に存在する0byte圧縮ファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る.
     * ※ レスポンスに返されるログの内容は、本Unitテストでは検査不可なため、別途検証する。
     */
    @Test
    public void 引数に存在する0byte圧縮ファイルパスを渡した場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            File file = File.createTempFile("TestFile", "log");
            file.deleteOnExit();

            final String logContent = "";
            FileWriter writer = new FileWriter(file);
            try {
                writer.write(logContent);
            } finally {
                writer.close();
            }

            String filename = file.getAbsolutePath();
            createZip(filename + ".zip", new File[] {file });
            // zipの中身のファイルを削除する
            file.delete();

            // ログ取得実行
            Object result = method.invoke(logResource, new Object[] {"archive", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(((Response) result).getStatus(), HttpStatus.SC_OK);

            assertTrue(((Response) result).getEntity() instanceof InputStream);

            InputStream in = (InputStream) ((Response) result).getEntity();
            String out = new String(IOUtils.toByteArray(in), "UTF-8");
            assertEquals(logContent, out);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    /**
     * 引数に存在する圧縮ファイルパスを渡した場合_レスポンスボディにStreamingOutputオブジェクトが入ったSC_OKレスポンスが返る.
     * ※ レスポンスに返されるログの内容は、本Unitテストでは検査不可なため、別途検証する。
     */
    @Test
    public void コレクション名がarchiveで引数に圧縮されていないファイルパスを渡した場合_500エラーが返却されること() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            File file = File.createTempFile("TestFile", ".log.zip");
            file.deleteOnExit();

            final String logContent = "a,b,c\n" + "x,y,z\n";
            FileWriter writer = new FileWriter(file);
            try {
                writer.write(logContent);
            } finally {
                writer.close();
            }

            String filename = file.getAbsolutePath();
            String paramFileName = filename.substring(0, filename.length() - ".zip".length());

            // ログ取得実行
            method.invoke(logResource, new Object[] {"archive", paramFileName });
            fail();
        } catch (Exception e) {
            Throwable t = e.getCause();
            assertEquals(PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN.getMessage(), t.getMessage());
        }
    }

    /**
     * 存在するファイルパスだが中身が空の場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る.
     * ※ レスポンスに返されるログの内容は、本Unitテストでは検査不可なため、別途検証する。
     */
    @Test
    public void 存在するファイルパスだが中身が空の場合_レスポンスボディにInputStreamオブジェクトが入ったSC_OKレスポンスが返る() {
        TestLogResource logResource = new TestLogResource();

        try {
            Method method = LogResource.class.getDeclaredMethod("getLog", new Class[] {String.class, String.class });
            method.setAccessible(true);

            File file = File.createTempFile("TestFile", "log");
            file.deleteOnExit();

            String filename = file.getAbsolutePath();
            Object result = method.invoke(logResource, new Object[] {"current", filename });

            assertNotNull(result);
            assertTrue(result instanceof Response);
            assertEquals(((Response) result).getStatus(), HttpStatus.SC_OK);
            assertTrue(((Response) result).getEntity() instanceof InputStream);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private void createZip(String fileName, File[] files) throws IOException {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName))));
            createZip(zos, files);
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    private void createZip(ZipOutputStream zos, File[] files) throws IOException {
        byte[] buf = new byte[1024];
        InputStream is = null;
        try {
            for (File file : files) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);

                is = new BufferedInputStream(new FileInputStream(file));
                int len = 0;
                while ((len = is.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}

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
package io.personium.core.model.impl.fs;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static javax.ws.rs.core.HttpHeaders.ETAG;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.personium.common.es.EsClient;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.http.header.RangeHeaderHandler;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavDestination;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.file.DataCryptor;
import io.personium.core.model.file.StreamingOutputForDavFile;
import io.personium.core.model.file.StreamingOutputForDavFileWithRange;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.lock.Lock;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for DavCmpFsImpl.
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.crypto.*" })
@PrepareForTest({ DavCmpFsImpl.class, AccessContext.class, PersoniumUnitConfig.class, DavMetadataFile.class,
    EsClient.class, EsModel.class })
@Category({ Unit.class })
public class DavCmpFsImplTest {

    /** Class name. */
    private static final String CLASS_NAME = "DavCmpFsImplTest";
    /** Test dir path. */
    private static final String TEST_DIR_PATH = "/personium_nfs/personium-core/unitTest/" + CLASS_NAME + "/";
    /** Content file name for update. */
    private static final String CONTENT_FILE = "content";
    /** Temp content file name for update. */
    private static final String TEMP_CONTENT_FILE = "tmp";
    /** Source file name for move. */
    private static final String SOURCE_FILE = "source";
    /** Dest file name for move. */
    private static final String DEST_FILE = "target";
    /** AES key string. */
    private static final String AES_KEY = "abcdef0123456789";
    /** Cell ID(AES IV:dIlleCtseTmuinos). */
    private static final String CELL_ID = "PersoniumTestCellId";

    /** Test class. */
    private DavCmpFsImpl davCmpFsImpl;
    /** Test dir. */
    private static File testDir;

    /**
     * BeforeClass.
     */
    @BeforeClass
    public static void beforeClass() {
        testDir = new File(TEST_DIR_PATH);
        testDir.mkdirs();
        testDir.setWritable(true);
    }

    /**
     * AfterClass.
     */
    @AfterClass
    public static void afterClass() {
        testDir.delete();
    }

    /**
     * Test doPutForCreate().
     * normal.
     * DavEncryptEnabled is false.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForCreate_Normal_encrypt_false() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        File contentFile = new File(contentPath);
        InputStream inputStream = null;
        FileInputStream contentStream = null;
        try {
            // --------------------
            // Test method args
            // --------------------
            String contentType = "text/plain";
            inputStream = getSystemResourceAsStream("davFile/file01.txt");

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            PowerMockito.doNothing().when(davCmpFsImpl, "checkChildResourceCount");

            PowerMockito.mockStatic(PersoniumUnitConfig.class);
            PowerMockito.doReturn(false).when(PersoniumUnitConfig.class, "isDavEncryptEnabled");

            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            PowerMockito.doReturn(contentPath).when(davCmpFsImpl, "getContentFilePath");

            DavMetadataFile davMetaDataFile = mock(DavMetadataFile.class);
            PowerMockito.mockStatic(DavMetadataFile.class);
            PowerMockito.doReturn(davMetaDataFile).when(DavMetadataFile.class,
                    "prepareNewFile", davCmpFsImpl, DavCmp.TYPE_DAV_FILE);

            doNothing().when(davMetaDataFile).setContentType(anyString());
            doNothing().when(davMetaDataFile).setContentLength(anyLong());
            doNothing().when(davMetaDataFile).setEncryptionType(anyString());
            doNothing().when(davMetaDataFile).save();

            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            boolean contentFileExists = true;
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/file01.txt"));
            ResponseBuilder expected = Response.ok().status(SC_CREATED).header(ETAG, "\"1-1487652733383\"");

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForCreate", String.class, InputStream.class);
            method.setAccessible(true);
            // Run method
            ResponseBuilder actual = (ResponseBuilder) method.invoke(davCmpFsImpl, contentType, inputStream);

            // --------------------
            // Confirm result
            // --------------------
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> contentLengthCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> encryptionTypeCaptor = ArgumentCaptor.forClass(String.class);
            verify(davMetaDataFile, times(1)).setContentType(contentTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).setContentLength(contentLengthCaptor.capture());
            verify(davMetaDataFile, times(1)).setEncryptionType(encryptionTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).save();
            assertThat(contentTypeCaptor.getValue(), is(contentType));
            assertThat(contentLengthCaptor.getValue(), is(15L));
            assertThat(encryptionTypeCaptor.getValue(), is(DataCryptor.ENCRYPTION_TYPE_NONE));

            assertThat(contentFile.exists(), is(contentFileExists));
            contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (contentStream != null) {
                contentStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test doPutForCreate().
     * normal.
     * DavEncryptEnabled is true.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForCreate_Normal_encrypt_true() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        File contentFile = new File(contentPath);
        InputStream inputStream = null;
        FileInputStream contentStream = null;
        try {
            // --------------------
            // Test method args
            // --------------------
            String contentType = "text/plain";
            inputStream = getSystemResourceAsStream("davFile/decrypt01.txt");

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            PowerMockito.doNothing().when(davCmpFsImpl, "checkChildResourceCount");

            PowerMockito.mockStatic(PersoniumUnitConfig.class);
            PowerMockito.doReturn(true).when(PersoniumUnitConfig.class, "isDavEncryptEnabled");

            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            DataCryptor.setKeyString(AES_KEY);

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            PowerMockito.doReturn(contentPath).when(davCmpFsImpl, "getContentFilePath");

            DavMetadataFile davMetaDataFile = mock(DavMetadataFile.class);
            PowerMockito.mockStatic(DavMetadataFile.class);
            PowerMockito.doReturn(davMetaDataFile).when(DavMetadataFile.class,
                    "prepareNewFile", davCmpFsImpl, DavCmp.TYPE_DAV_FILE);

            doNothing().when(davMetaDataFile).setContentType(anyString());
            doNothing().when(davMetaDataFile).setContentLength(anyLong());
            doNothing().when(davMetaDataFile).setEncryptionType(anyString());
            doNothing().when(davMetaDataFile).save();

            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            boolean contentFileExists = true;
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/encrypt01.txt"));
            ResponseBuilder expected = Response.ok().status(SC_CREATED).header(ETAG, "\"1-1487652733383\"");

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForCreate", String.class, InputStream.class);
            method.setAccessible(true);
            // Run method
            ResponseBuilder actual = (ResponseBuilder) method.invoke(davCmpFsImpl, contentType, inputStream);

            // --------------------
            // Confirm result
            // --------------------
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> contentLengthCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> encryptionTypeCaptor = ArgumentCaptor.forClass(String.class);
            verify(davMetaDataFile, times(1)).setContentType(contentTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).setContentLength(contentLengthCaptor.capture());
            verify(davMetaDataFile, times(1)).setEncryptionType(encryptionTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).save();
            assertThat(contentTypeCaptor.getValue(), is(contentType));
            assertThat(contentLengthCaptor.getValue(), is(94L));
            assertThat(encryptionTypeCaptor.getValue(), is(DataCryptor.ENCRYPTION_TYPE_AES));

            assertThat(contentFile.exists(), is(contentFileExists));
            contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (contentStream != null) {
                contentStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test doPutForUpdate().
     * normal.
     * DavEncryptEnabled is false.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForUpdate_Normal_encrypt_false() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        String tempContentPath = TEST_DIR_PATH + TEMP_CONTENT_FILE;
        InputStream inputStream = null;
        FileInputStream contentStream = null;
        File contentFile = new File(contentPath);
        File tempContentFile = new File(tempContentPath);
        try {
            contentFile.createNewFile();
            // --------------------
            // Test method args
            // --------------------
            String contentType = "text/plain";
            inputStream = getSystemResourceAsStream("davFile/file01.txt");
            String etag = "\"1-1487652733383\"";

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            DavMetadataFile davMetaDataFile = mock(DavMetadataFile.class);
            Whitebox.setInternalState(davCmpFsImpl, "metaFile", davMetaDataFile);

            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();
            PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());

            PowerMockito.mockStatic(PersoniumUnitConfig.class);
            PowerMockito.doReturn(false).when(PersoniumUnitConfig.class, "isDavEncryptEnabled");

            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();

            PowerMockito.doReturn(tempContentPath).when(davCmpFsImpl, "getTempContentFilePath");
            PowerMockito.doReturn(contentPath).when(davCmpFsImpl, "getContentFilePath");

            doNothing().when(davMetaDataFile).setUpdated(anyLong());
            doNothing().when(davMetaDataFile).setContentType(anyString());
            doNothing().when(davMetaDataFile).setContentLength(anyLong());
            doNothing().when(davMetaDataFile).setEncryptionType(anyString());
            doNothing().when(davMetaDataFile).save();

            // --------------------
            // Expected result
            // --------------------
            boolean contentFileExists = true;
            boolean tempFileExists = false;
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/file01.txt"));
            ResponseBuilder expected = Response.ok().status(SC_NO_CONTENT).header(ETAG, "\"1-1487652733383\"");

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForUpdate",
                    String.class, InputStream.class, String.class);
            method.setAccessible(true);
            // Run method
            ResponseBuilder actual = (ResponseBuilder) method.invoke(davCmpFsImpl, contentType, inputStream, etag);

            // --------------------
            // Confirm result
            // --------------------
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> contentLengthCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> encryptionTypeCaptor = ArgumentCaptor.forClass(String.class);
            verify(davMetaDataFile, times(1)).setUpdated(anyLong());
            verify(davMetaDataFile, times(1)).setContentType(contentTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).setContentLength(contentLengthCaptor.capture());
            verify(davMetaDataFile, times(1)).setEncryptionType(encryptionTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).save();
            assertThat(contentTypeCaptor.getValue(), is(contentType));
            assertThat(contentLengthCaptor.getValue(), is(15L));
            assertThat(encryptionTypeCaptor.getValue(), is(DataCryptor.ENCRYPTION_TYPE_NONE));

            assertThat(contentFile.exists(), is(contentFileExists));
            assertThat(tempContentFile.exists(), is(tempFileExists));
            contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (contentStream != null) {
                contentStream.close();
            }
            contentFile.delete();
            tempContentFile.delete();
        }
    }

    /**
     * Test doPutForUpdate().
     * normal.
     * DavEncryptEnabled is true.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForUpdate_Normal_encrypt_true() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        String tempContentPath = TEST_DIR_PATH + TEMP_CONTENT_FILE;
        InputStream inputStream = null;
        FileInputStream contentStream = null;
        File contentFile = new File(contentPath);
        File tempContentFile = new File(tempContentPath);
        try {
            contentFile.createNewFile();
            // --------------------
            // Test method args
            // --------------------
            String contentType = "text/plain";
            inputStream = getSystemResourceAsStream("davFile/decrypt01.txt");
            String etag = "\"1-1487652733383\"";

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            DavMetadataFile davMetaDataFile = mock(DavMetadataFile.class);
            Whitebox.setInternalState(davCmpFsImpl, "metaFile", davMetaDataFile);

            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();
            PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());

            PowerMockito.mockStatic(PersoniumUnitConfig.class);
            PowerMockito.doReturn(true).when(PersoniumUnitConfig.class, "isDavEncryptEnabled");

            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            DataCryptor.setKeyString(AES_KEY);

            PowerMockito.doReturn(tempContentPath).when(davCmpFsImpl, "getTempContentFilePath");
            PowerMockito.doReturn(contentPath).when(davCmpFsImpl, "getContentFilePath");

            doNothing().when(davMetaDataFile).setUpdated(anyLong());
            doNothing().when(davMetaDataFile).setContentType(anyString());
            doNothing().when(davMetaDataFile).setContentLength(anyLong());
            doNothing().when(davMetaDataFile).setEncryptionType(anyString());
            doNothing().when(davMetaDataFile).save();

            // --------------------
            // Expected result
            // --------------------
            boolean contentFileExists = true;
            boolean tempFileExists = false;
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/encrypt01.txt"));
            ResponseBuilder expected = Response.ok().status(SC_NO_CONTENT).header(ETAG, "\"1-1487652733383\"");

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForUpdate",
                    String.class, InputStream.class, String.class);
            method.setAccessible(true);
            // Run method
            ResponseBuilder actual = (ResponseBuilder) method.invoke(davCmpFsImpl, contentType, inputStream, etag);

            // --------------------
            // Confirm result
            // --------------------
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> contentLengthCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<String> encryptionTypeCaptor = ArgumentCaptor.forClass(String.class);
            verify(davMetaDataFile, times(1)).setUpdated(anyLong());
            verify(davMetaDataFile, times(1)).setContentType(contentTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).setContentLength(contentLengthCaptor.capture());
            verify(davMetaDataFile, times(1)).setEncryptionType(encryptionTypeCaptor.capture());
            verify(davMetaDataFile, times(1)).save();
            assertThat(contentTypeCaptor.getValue(), is(contentType));
            assertThat(contentLengthCaptor.getValue(), is(94L));
            assertThat(encryptionTypeCaptor.getValue(), is(DataCryptor.ENCRYPTION_TYPE_AES));

            assertThat(contentFile.exists(), is(contentFileExists));
            assertThat(tempContentFile.exists(), is(tempFileExists));
            contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (contentStream != null) {
                contentStream.close();
            }
            contentFile.delete();
            tempContentFile.delete();
        }
    }

    /**
     * Test doPutForUpdate().
     * Error case.
     * ETag not match.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForUpdate_Error_Not_match_ETag() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        String tempContentPath = TEST_DIR_PATH + TEMP_CONTENT_FILE;
        File contentFile = new File(contentPath);
        File tempContentFile = new File(tempContentPath);
        try {
            contentFile.createNewFile();
            // Test method args
            String contentType = "application/json";
            InputStream inputStream = getSystemResourceAsStream("request/unit/cell-create.txt");
            String etag = "\"1-1487652733383\"";

            // Expected result
            boolean contentFileExists = true;
            boolean tempFileExists = false;
            FileInputStream sourceStream = new FileInputStream(contentFile);
            String sourceFileMD5 = md5Hex(sourceStream);
            sourceStream.close();

            // Mock settings
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            PowerMockito.doReturn(false).when(davCmpFsImpl, "matchesETag", anyString());

            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForUpdate",
                    String.class, InputStream.class, String.class);
            method.setAccessible(true);

            // Run method
            try {
                method.invoke(davCmpFsImpl, contentType, inputStream, etag);
                fail("Not throws exception.");
            } catch (InvocationTargetException e) {
                // Confirm result
                assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
                PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
                assertThat(exception.getCode(), is(PersoniumCoreException.Dav.ETAG_NOT_MATCH.getCode()));
            }
            assertThat(contentFile.exists(), is(contentFileExists));
            assertThat(tempContentFile.exists(), is(tempFileExists));
            FileInputStream contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));

            inputStream.close();
            contentStream.close();
        } finally {
            contentFile.delete();
            tempContentFile.delete();
        }
    }

    /**
     * Test get().
     * normal.
     * DavEncryptEnabled is false.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void get_Normal_encrypt_false() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        InputStream inputStream = null;
        File contentFile = new File(contentPath);
        try {
            inputStream = getSystemResourceAsStream("davFile/file01.txt");
            Files.copy(inputStream, contentFile.toPath());
            // --------------------
            // Test method args
            // --------------------
            String rangeHeaderField = null;

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            doReturn("text/plain").when(davCmpFsImpl).getContentType();
            doReturn(98L).when(davCmpFsImpl).getContentLength();
            doReturn(DataCryptor.ENCRYPTION_TYPE_NONE).when(davCmpFsImpl).getEncryptionType();
            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/file01.txt"));
            ResponseBuilder expected = Response.ok().header(HttpHeaders.CONTENT_LENGTH, 98L)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .header(ETAG, "\"1-1487652733383\"")
                    .header(PersoniumCoreUtils.HttpHeaders.ACCEPT_RANGES, RangeHeaderHandler.BYTES_UNIT);

            // --------------------
            // Run method
            // --------------------
            ResponseBuilder actual = davCmpFsImpl.get(rangeHeaderField);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            StreamingOutputForDavFile entity = (StreamingOutputForDavFile) actual.build().getEntity();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            entity.write(output);
            assertThat(md5Hex(output.toByteArray()), is(sourceFileMD5));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test get().
     * normal.
     * DavEncryptEnabled is true.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void get_Normal_encrypt_true() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        InputStream inputStream = null;
        File contentFile = new File(contentPath);
        try {
            inputStream = getSystemResourceAsStream("davFile/encrypt01.txt");
            Files.copy(inputStream, contentFile.toPath());
            // --------------------
            // Test method args
            // --------------------
            String rangeHeaderField = null;

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            doReturn("text/plain").when(davCmpFsImpl).getContentType();
            doReturn(98L).when(davCmpFsImpl).getContentLength();
            doReturn(DataCryptor.ENCRYPTION_TYPE_AES).when(davCmpFsImpl).getEncryptionType();
            DataCryptor.setKeyString(AES_KEY);
            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/decrypt01.txt"));
            ResponseBuilder expected = Response.ok().header(HttpHeaders.CONTENT_LENGTH, 98L)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .header(ETAG, "\"1-1487652733383\"")
                    .header(PersoniumCoreUtils.HttpHeaders.ACCEPT_RANGES, RangeHeaderHandler.BYTES_UNIT);

            // --------------------
            // Run method
            // --------------------
            ResponseBuilder actual = davCmpFsImpl.get(rangeHeaderField);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            StreamingOutputForDavFile entity = (StreamingOutputForDavFile) actual.build().getEntity();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            entity.write(output);
            assertThat(md5Hex(output.toByteArray()), is(sourceFileMD5));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test get().
     * normal.
     * Range specification.
     * DavEncryptEnabled is false.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void get_Normal_range_encrypt_false() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        InputStream inputStream = null;
        File contentFile = new File(contentPath);
        try {
            inputStream = getSystemResourceAsStream("davFile/decrypt01.txt");
            Files.copy(inputStream, contentFile.toPath());
            // --------------------
            // Test method args
            // --------------------
            String rangeHeaderField = "bytes=10-40";

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            doReturn("text/plain").when(davCmpFsImpl).getContentType();
            doReturn(98L).when(davCmpFsImpl).getContentLength();
            doReturn(DataCryptor.ENCRYPTION_TYPE_NONE).when(davCmpFsImpl).getEncryptionType();
            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/range01.txt"));
            ResponseBuilder expected = Response.status(HttpStatus.SC_PARTIAL_CONTENT)
                    .header(PersoniumCoreUtils.HttpHeaders.CONTENT_RANGE, "bytes 10-40/98")
                    .header(HttpHeaders.CONTENT_LENGTH, 31L)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .header(ETAG, "\"1-1487652733383\"")
                    .header(PersoniumCoreUtils.HttpHeaders.ACCEPT_RANGES, RangeHeaderHandler.BYTES_UNIT);

            // --------------------
            // Run method
            // --------------------
            ResponseBuilder actual = davCmpFsImpl.get(rangeHeaderField);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            StreamingOutputForDavFileWithRange entity = (StreamingOutputForDavFileWithRange) actual.build().getEntity();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            entity.write(output);
            assertThat(md5Hex(output.toByteArray()), is(sourceFileMD5));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test get().
     * normal.
     * Range specification.
     * DavEncryptEnabled is true.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void get_Normal_range_encrypt_true() throws Exception {
        String contentPath = TEST_DIR_PATH + CONTENT_FILE;
        InputStream inputStream = null;
        File contentFile = new File(contentPath);
        try {
            inputStream = getSystemResourceAsStream("davFile/encrypt01.txt");
            Files.copy(inputStream, contentFile.toPath());
            // --------------------
            // Test method args
            // --------------------
            String rangeHeaderField = "bytes=10-40";

            // --------------------
            // Mock settings
            // --------------------
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

            Whitebox.setInternalState(davCmpFsImpl, "fsPath", TEST_DIR_PATH);

            doReturn("text/plain").when(davCmpFsImpl).getContentType();
            doReturn(98L).when(davCmpFsImpl).getContentLength();
            doReturn(DataCryptor.ENCRYPTION_TYPE_AES).when(davCmpFsImpl).getEncryptionType();
            DataCryptor.setKeyString(AES_KEY);
            doReturn(CELL_ID).when(davCmpFsImpl).getCellId();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            // --------------------
            // Expected result
            // --------------------
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("davFile/range01.txt"));
            ResponseBuilder expected = Response.status(HttpStatus.SC_PARTIAL_CONTENT)
                    .header(PersoniumCoreUtils.HttpHeaders.CONTENT_RANGE, "bytes 10-40/98")
                    .header(HttpHeaders.CONTENT_LENGTH, 31L)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .header(ETAG, "\"1-1487652733383\"")
                    .header(PersoniumCoreUtils.HttpHeaders.ACCEPT_RANGES, RangeHeaderHandler.BYTES_UNIT);

            // --------------------
            // Run method
            // --------------------
            ResponseBuilder actual = davCmpFsImpl.get(rangeHeaderField);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            StreamingOutputForDavFile entity = (StreamingOutputForDavFile) actual.build().getEntity();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            entity.write(output);
            assertThat(md5Hex(output.toByteArray()), is(sourceFileMD5));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            contentFile.delete();
        }
    }

    /**
     * Test move().
     * Dest DavNode not exists.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void move_Normal_Dest_DavNode_not_exists() throws Exception {
        String sourcePath = TEST_DIR_PATH + SOURCE_FILE;
        String destPath = TEST_DIR_PATH + DEST_FILE;
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            sourceFile.createNewFile();
            // Test method args
            String etag = "\"1-1487652733383\"";
            String overwrite = "overwrite";
            DavDestination davDestination = mock(DavDestination.class);

            // Expected result
            boolean sourceFileExists = false;
            boolean destFileExists = true;
            FileInputStream sourceStream = new FileInputStream(sourceFile);
            String sourceFileMD5 = md5Hex(sourceStream);
            sourceStream.close();
            ResponseBuilder expected = Response.status(HttpStatus.SC_CREATED);
            expected.header(HttpHeaders.LOCATION, destPath);
            expected.header(HttpHeaders.ETAG, "\"1-1487652733383\"");

            // Mock settings
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            Lock lock = mock(Lock.class);
            doNothing().when(lock).release();
            doReturn(lock).when(davCmpFsImpl).lock();
            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());
            File fsDir = mock(File.class);
            doReturn(sourceFile.toPath()).when(fsDir).toPath();
            Whitebox.setInternalState(davCmpFsImpl, "fsDir", fsDir);
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

            doNothing().when(davDestination).loadDestinationHierarchy();
            doNothing().when(davDestination).validateDestinationResource(anyString(), any(DavCmp.class));
            DavRsCmp davRsCmp = mock(DavRsCmp.class);
            AccessContext accessContext = PowerMockito.mock(AccessContext.class);
            doReturn(accessContext).when(davRsCmp).getAccessContext();
            doReturn(davRsCmp).when(davRsCmp).getParent();
            doNothing().when(davRsCmp).checkAccessContext(any(AccessContext.class), any(BoxPrivilege.class));
            doReturn(davRsCmp).when(davDestination).getDestinationRsCmp();
            DavCmpFsImpl destDavCmp = PowerMockito.mock(DavCmpFsImpl.class);
            File destDir = mock(File.class);
            doReturn(destFile.toPath()).when(destDir).toPath();
            Whitebox.setInternalState(destDavCmp, "fsDir", destDir);
            doReturn(false).when(destDavCmp).exists();
            doReturn(destDavCmp).when(davDestination).getDestinationCmp();
            doReturn(destPath).when(davDestination).getDestinationUri();

            // Run method
            ResponseBuilder actual = davCmpFsImpl.move(etag, overwrite, davDestination);

            // Confirm result
            assertThat(sourceFile.exists(), is(sourceFileExists));
            assertThat(destFile.exists(), is(destFileExists));
            FileInputStream destStream = new FileInputStream(destFile);
            assertThat(md5Hex(destStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            destStream.close();
        } finally {
            sourceFile.delete();
            destFile.delete();
        }
    }

    /**
     * Test move().
     * Error case.
     * ETag not match.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void move_Error_Not_match_ETag() throws Exception {
        String sourcePath = TEST_DIR_PATH + SOURCE_FILE;
        String destPath = TEST_DIR_PATH + DEST_FILE;
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            sourceFile.createNewFile();
            // Test method args
            String etag = "\"1-1487652733383\"";
            String overwrite = "overwrite";
            DavDestination davDestination = mock(DavDestination.class);

            // Expected result
            boolean sourceFileExists = true;
            boolean destFileExists = false;
            FileInputStream sourceStream = new FileInputStream(sourceFile);
            String sourceFileMD5 = md5Hex(sourceStream);
            sourceStream.close();

            // Mock settings
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            Lock lock = mock(Lock.class);
            doNothing().when(lock).release();
            doReturn(lock).when(davCmpFsImpl).lock();
            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            PowerMockito.doReturn(false).when(davCmpFsImpl, "matchesETag", anyString());

            try {
                // Run method
                davCmpFsImpl.move(etag, overwrite, davDestination);
                fail("Not throws exception.");
            } catch (PersoniumCoreException e) {
                // Confirm result
                assertThat(e.getCode(), is(PersoniumCoreException.Dav.ETAG_NOT_MATCH.getCode()));
            }
            assertThat(sourceFile.exists(), is(sourceFileExists));
            assertThat(destFile.exists(), is(destFileExists));
            FileInputStream stream = new FileInputStream(sourceFile);
            assertThat(md5Hex(stream), is(sourceFileMD5));

            stream.close();
        } finally {
            sourceFile.delete();
            destFile.delete();
        }
    }

    /**
     * Test delete().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void delete_Normal_Recursive_false() throws Exception {
        // Test method args
        String ifMatch = "\"1-1487652733383\"";
        boolean recursive = false;

        // Expected result
        ResponseBuilder expected = Response.ok().status(HttpStatus.SC_NO_CONTENT);

        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
        PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());
        Lock lock = mock(Lock.class);
        doNothing().when(lock).release();
        doReturn(lock).when(davCmpFsImpl).lock();
        doNothing().when(davCmpFsImpl).load();
        DavMetadataFile davMetaDataFile = DavMetadataFile.newInstance(new File(""));
        Whitebox.setInternalState(davCmpFsImpl, "metaFile", davMetaDataFile);
        doReturn("testType").when(davCmpFsImpl).getType();
        doReturn(1).when(davCmpFsImpl).getChildrenCount();
        doNothing().when(davCmpFsImpl).doDelete();

        // Run method
        ResponseBuilder actual = davCmpFsImpl.delete(ifMatch, recursive);

        // Confirm result
        assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
    }

    /**
     * Test delete().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void delete_Normal_Recursive_true() throws Exception {
        // Test method args
        String ifMatch = "\"1-1487652733383\"";
        boolean recursive = true;

        // Expected result
        ResponseBuilder expected = Response.ok().status(HttpStatus.SC_NO_CONTENT);

        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
        PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());
        Lock lock = mock(Lock.class);
        doNothing().when(lock).release();
        doReturn(lock).when(davCmpFsImpl).lock();
        doNothing().when(davCmpFsImpl).load();
        DavMetadataFile davMetaDataFile = DavMetadataFile.newInstance(new File(""));
        Whitebox.setInternalState(davCmpFsImpl, "metaFile", davMetaDataFile);
        PowerMockito.doNothing().when(davCmpFsImpl, "makeEmpty");

        // Run method
        ResponseBuilder actual = davCmpFsImpl.delete(ifMatch, recursive);

        // Confirm result
        assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
    }

    /**
     * Test delete().
     * Error case.
     * ETag not match.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void delete_Error_Not_match_ETag() throws Exception {
        // Test method args
        String ifMatch = "\"1-1487652733383\"";
        boolean recursive = false;

        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
        PowerMockito.doReturn(false).when(davCmpFsImpl, "matchesETag", anyString());

        try {
            // Run method
            davCmpFsImpl.delete(ifMatch, recursive);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            assertThat(e.getCode(), is(PersoniumCoreException.Dav.ETAG_NOT_MATCH.getCode()));
        }
    }

    /**
     * Test makeEmpty().
     * normal.
     * Type is OData collection.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void makeEmpty_Normal_Type_ODataCollection() throws Exception {
        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

        doReturn(DavCmpFsImpl.TYPE_COL_ODATA).when(davCmpFsImpl).getType();

        doReturn("cellId").when(davCmpFsImpl).getCellId();
        Box box = mock(Box.class);
        doReturn(box).when(davCmpFsImpl).getBox();
        doReturn("boxId").when(box).getId();
        doReturn("nodeId").when(davCmpFsImpl).getId();

        Lock lock = mock(Lock.class);
        doNothing().when(lock).release();
        doReturn(lock).when(davCmpFsImpl).lockOData("cellId", "boxId", "nodeId");

        PowerMockito.whenNew(EsClient.class).withAnyArguments().thenReturn(null);
        PowerMockito.mockStatic(EsModel.class);
        PowerMockito.doReturn(null).when(EsModel.class, "type", "", "", "", 0, 0);

        CellDataAccessor cellDataAccessor = mock(CellDataAccessor.class);
        PowerMockito.doReturn(cellDataAccessor).when(EsModel.class, "cellData", "bundleName", "cellId");

        Cell cell = mock(Cell.class);
        doReturn("bundleName").when(cell).getDataBundleNameWithOutPrefix();
        davCmpFsImpl.cell = cell;
        doNothing().when(cellDataAccessor).bulkDeleteODataCollection("boxId", "nodeId");
        doNothing().when(davCmpFsImpl).doDelete();

        // Run method
        davCmpFsImpl.makeEmpty();
    }

    /**
     * Test makeEmpty().
     * normal.
     * Type is WebDAV collection.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void makeEmpty_Normal_Type_WebDAVCollection() throws Exception {
        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

        doReturn(DavCmpFsImpl.TYPE_COL_WEBDAV).when(davCmpFsImpl).getType();

        Map<String, DavCmp> children = new HashMap<>();
        doReturn(children).when(davCmpFsImpl).getChildren();
        DavCmp child01 = mock(DavCmp.class);
        DavCmp child02 = mock(DavCmp.class);
        doNothing().when(child01).makeEmpty();
        doNothing().when(child02).makeEmpty();
        children.put("child01", child01);
        children.put("child02", child02);

        doNothing().when(davCmpFsImpl).doDelete();

        // Run method
        davCmpFsImpl.makeEmpty();

        // Confirm result
        verify(child01, times(1)).makeEmpty();
        verify(child02, times(1)).makeEmpty();
    }

    /**
     * Test makeEmpty().
     * normal.
     * Type is EngineSvc collection.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void makeEmpty_Normal_Type_EngineSvcCollection() throws Exception {
        // Mock settings
        davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));

        doReturn(DavCmpFsImpl.TYPE_COL_SVC).when(davCmpFsImpl).getType();

        doNothing().when(davCmpFsImpl).doDelete();

        // Run method
        davCmpFsImpl.makeEmpty();

        // Confirm result
        verify(davCmpFsImpl, times(1)).doDelete();
    }

    /**
     * Test matchesETag().
     * argETag equal ETag.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void matchesETag_Normal_argETag_equal_ETag() throws Exception {
        // Test method args
        String etag = "\"1-1487652733383\"";

        // Expected result
        boolean expected = true;

        // Mock settings
        davCmpFsImpl = spy(DavCmpFsImpl.class);
        doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

        // Load methods for private
        Method method = DavCmpFsImpl.class.getDeclaredMethod("matchesETag", String.class);
        method.setAccessible(true);
        // Run method
        boolean actual = (boolean) method.invoke(davCmpFsImpl, etag);
        // Confirm result
        assertThat(actual, is(expected));
    }

    /**
     * Test matchesETag().
     * argETag not equal ETag.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void matchesETag_Normal_argETag_not_equal_ETag() throws Exception {
        // Test method args
        String etag = "\"1-1487652733383\"";

        // Expected result
        boolean expected = false;

        // Mock settings
        davCmpFsImpl = spy(DavCmpFsImpl.class);
        doReturn("\"2-1487652733383\"").when(davCmpFsImpl).getEtag();

        // Load methods for private
        Method method = DavCmpFsImpl.class.getDeclaredMethod("matchesETag", String.class);
        method.setAccessible(true);
        // Run method
        boolean actual = (boolean) method.invoke(davCmpFsImpl, etag);
        // Confirm result
        assertThat(actual, is(expected));
    }

    /**
     * Test matchesETag().
     * Weak argETag equal ETag.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void matchesETag_Normal_weak_argETag_equal_ETag() throws Exception {
        // Test method args
        String etag = "W/\"1-1487652733383\"";

        // Expected result
        boolean expected = true;

        // Mock settings
        davCmpFsImpl = spy(DavCmpFsImpl.class);
        doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();

        // Load methods for private
        Method method = DavCmpFsImpl.class.getDeclaredMethod("matchesETag", String.class);
        method.setAccessible(true);
        // Run method
        boolean actual = (boolean) method.invoke(davCmpFsImpl, etag);
        // Confirm result
        assertThat(actual, is(expected));
    }

    /**
     * Test matchesETag().
     * argETag is null.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void matchesETag_Normal_argETag_is_null() throws Exception {
        // Test method args
        String etag = null;

        // Expected result
        boolean expected = false;

        // Mock settings
        davCmpFsImpl = spy(DavCmpFsImpl.class);

        // Load methods for private
        Method method = DavCmpFsImpl.class.getDeclaredMethod("matchesETag", String.class);
        method.setAccessible(true);
        // Run method
        boolean actual = (boolean) method.invoke(davCmpFsImpl, etag);
        // Confirm result
        assertThat(actual, is(expected));
    }
}


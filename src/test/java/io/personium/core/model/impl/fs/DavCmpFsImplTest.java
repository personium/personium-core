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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavDestination;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.lock.Lock;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for DavCmpFsImpl.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DavCmpFsImpl.class, AccessContext.class})
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
     * Test doPutForUpdate().
     * normal.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doPutForUpdate_Normal() throws Exception {
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
            String sourceFileMD5 = md5Hex(getSystemResourceAsStream("request/unit/cell-create.txt"));
            ResponseBuilder expected = Response.ok().status(SC_NO_CONTENT).header(ETAG, "\"1-1487652733383\"");

            // Mock settings
            davCmpFsImpl = PowerMockito.spy(DavCmpFsImpl.create("", null));
            DavMetadataFile davMetaDataFile = mock(DavMetadataFile.class);
            Whitebox.setInternalState(davCmpFsImpl, "metaFile", davMetaDataFile);
            doNothing().when(davCmpFsImpl).load();
            doReturn(true).when(davCmpFsImpl).exists();
            doReturn("\"1-1487652733383\"").when(davCmpFsImpl).getEtag();
            PowerMockito.doReturn(true).when(davCmpFsImpl, "matchesETag", anyString());
            PowerMockito.doReturn(tempContentPath).when(davCmpFsImpl, "getTempContentFilePath");
            PowerMockito.doReturn(contentPath).when(davCmpFsImpl, "getContentFilePath");

            doNothing().when(davMetaDataFile).setUpdated(anyLong());
            doNothing().when(davMetaDataFile).setContentType(anyString());
            doNothing().when(davMetaDataFile).setContentLength(anyLong());
            doNothing().when(davMetaDataFile).save();

            // Load methods for private
            Method method = DavCmpFsImpl.class.getDeclaredMethod("doPutForUpdate",
                    String.class, InputStream.class, String.class);
            method.setAccessible(true);

            // Run method
            ResponseBuilder actual = (ResponseBuilder) method.invoke(davCmpFsImpl, contentType, inputStream, etag);

            // Confirm result
            assertThat(contentFile.exists(), is(contentFileExists));
            assertThat(tempContentFile.exists(), is(tempFileExists));
            FileInputStream contentStream = new FileInputStream(contentFile);
            assertThat(md5Hex(contentStream), is(sourceFileMD5));
            assertThat(actual.build().getStatus(), is(expected.build().getStatus()));
            assertThat(actual.build().getMetadata().toString(), is(expected.build().getMetadata().toString()));

            inputStream.close();
            contentStream.close();
        } finally {
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
    public void delete_Normal() throws Exception {
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
        PowerMockito.doNothing().when(davCmpFsImpl, "doDelete");

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
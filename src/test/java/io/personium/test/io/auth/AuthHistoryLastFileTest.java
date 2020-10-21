/**
 * Personium
 * Copyright 2019-2020 Personium Project Authors
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
package io.personium.test.io.auth;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.expectation.PowerMockitoStubber;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for AuthHistoryLastFile.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AuthHistoryLastFile.class })
@Category({ Unit.class })
public class AuthHistoryLastFileTest {

    /** Class name. */
    private static final String CLASS_NAME = "AuthHistoryLastFileTest";

    /** Test account id. */
    private static final String TEST_ACCOUNT_ID = "account_last_1";

    /** Test dir path. */
    private static final String TEST_DIR_PATH = "/personium_nfs/personium-core/unitTest/" + CLASS_NAME + "/";
    /** Test auth history account path. */
    private static final String TEST_AUTH_HISTORY_PATH = AuthHistoryLastFile.AUTH_HISTORY_DIRECTORY + "/"
            + TEST_ACCOUNT_ID + "/";

    /** UnitTest path. */
    private static String unitTestPath;
    /** Test dir. */
    private static File testDir;
    /** Test auth history path. */
    private static String testAuthHistoryPath;
    /** Test auth history dir. */
    private static File testAuthHistoryDir;

    /** Test class. */
    private AuthHistoryLastFile authHistoryLastFile;

    /**
     * BeforeClass.
     */
    @BeforeClass
    public static void beforeClass() {
        unitTestPath = PersoniumUnitConfig.get("io.personium.core.test.unitTest.root");
        if (unitTestPath != null) {
            unitTestPath += "/" + CLASS_NAME + "/";
        } else {
            unitTestPath = TEST_DIR_PATH;
        }

        // create test dir.
        testDir = new File(unitTestPath);
        testDir.mkdirs();
        testDir.setWritable(true);

        testAuthHistoryPath = unitTestPath + TEST_AUTH_HISTORY_PATH;
        testAuthHistoryDir = new File(testAuthHistoryPath);
    }

    /**
     * AfterClass.
     * @throws IOException Unexpected error
     */
    @AfterClass
    public static void afterClass() throws IOException {
        FileUtils.deleteDirectory(testDir);
    }

    /**
     * After.
     * @throws IOException Unexpected error
     */
    @After
    public void after() throws IOException {
        if (testDir.listFiles().length > 0) {
            FileUtils.cleanDirectory(testDir);
        }
    }

    /**
     * Test load().
     * Normal.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void load_Normal() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        authHistoryLastFile = PowerMockito.spy(AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID));
        PowerMockito.doNothing().when(authHistoryLastFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        authHistoryLastFile.load();

        // --------------------
        // Confirm result
        // --------------------
        PowerMockito.verifyPrivate(authHistoryLastFile, times(1)).invoke("doLoad");
    }

    /**
     * Test load().
     * Normal.
     * Retry OK.
     * @throws Exception Unintended exception in test
     */
    @Test
    @Ignore
    public void load_Normal_retry_ok() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        authHistoryLastFile = PowerMockito.spy(AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID));

        // The first and second fail, the third success.
        PersoniumCoreException exception = PersoniumCoreException.Common.FILE_IO_ERROR.reason(new IOException());
        ((PowerMockitoStubber) PowerMockito
                .doThrow(exception)
                .doThrow(exception)
                .doNothing()).when(authHistoryLastFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        authHistoryLastFile.load();

        // --------------------
        // Confirm result
        // --------------------
        PowerMockito.verifyPrivate(authHistoryLastFile, times(3)).invoke("doLoad");
    }

    /**
     * Test load().
     * Error.
     * Retry NG.
     * @throws Exception Unintended exception in test
     */
    @Test
    @Ignore
    public void load_Error_retry_ng() throws Exception {
        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        authHistoryLastFile = PowerMockito.spy(AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID));

        // All fail.
        PersoniumCoreException exception = PersoniumCoreException.Common.FILE_IO_ERROR.reason(new IOException());
        ((PowerMockitoStubber) PowerMockito
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)).when(authHistoryLastFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        try {
            authHistoryLastFile.load();
            fail("Not exception.");
        } catch (Exception e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e, is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException pe = (PersoniumCoreException) e;
            assertThat(pe.getCode(), is(PersoniumCoreException.Common.FILE_IO_ERROR.getCode()));
            assertThat(pe.getCause(), is(instanceOf(IOException.class)));
        }

        PowerMockito.verifyPrivate(authHistoryLastFile, times(6)).invoke("doLoad");
    }

    /**
     * Test doLoad().
     * Normal.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings("unchecked")
    @Test
    public void doLoad_Normal() throws Exception {
        String path = testAuthHistoryPath + AuthHistoryLastFile.AUTH_HISTORY_LAST_FILE_NAME;
        File file = new File(path);

        try {
            testAuthHistoryDir.mkdirs();
            testAuthHistoryDir.setWritable(true);
            Files.copy(getSystemResourceAsStream("pauthhistory/authHistoryLastTest/auth_history_last.json"),
                    file.toPath());
            authHistoryLastFile = AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID);

            // --------------------
            // Test method args
            // --------------------
            // None.

            // --------------------
            // Mock settings
            // --------------------
            // None.

            // --------------------
            // Expected result
            // --------------------
            JSONObject expected = new JSONObject();
            expected.put("last_authenticated", 1548132805L);
            expected.put("failed_count", 2L);

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = AuthHistoryLastFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);
            method.invoke(authHistoryLastFile);

            // --------------------
            // Confirm result
            // --------------------
            JSONObject actual = authHistoryLastFile.getJSON();
            assertThat(actual.get("last_authenticated"), is(expected.get("last_authenticated")));
            assertThat(actual.get("failed_count"), is(expected.get("failed_count")));
        } finally {
            file.delete();
        }
    }

    /**
     * Test doLoad().
     * Error.
     * IOException occurred.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doLoad_Error_IOException() throws Exception {
        String path = testAuthHistoryPath + AuthHistoryLastFile.AUTH_HISTORY_LAST_FILE_NAME;
        File file = new File(path);

        try {
            authHistoryLastFile = AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID);

            // --------------------
            // Test method args
            // --------------------
            // None.

            // --------------------
            // Mock settings
            // --------------------
            // None.

            // --------------------
            // Expected result
            // --------------------
            // None.

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = AuthHistoryLastFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);

            try {
                // Run method
                method.invoke(authHistoryLastFile);
                fail("Not exception.");
            } catch (InvocationTargetException e) {
                // --------------------
                // Confirm result
                // --------------------
                assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
                PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
                assertThat(exception.getCode(), is(PersoniumCoreException.Common.FILE_IO_ERROR.getCode()));
                assertThat(exception.getCause(), is(instanceOf(IOException.class)));
            }
        } finally {
            file.delete();
        }
    }

    /**
     * Test doLoad().
     * Error.
     * ParseException occurred.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void doLoad_Error_ParseException() throws Exception {
        String path = testAuthHistoryPath + AuthHistoryLastFile.AUTH_HISTORY_LAST_FILE_NAME;
        File file = new File(path);
        String data = "}{1234567890!#$%&()";

        try (InputStream stream = new ByteArrayInputStream(data.getBytes())) {
            testAuthHistoryDir.mkdirs();
            testAuthHistoryDir.setWritable(true);
            Files.copy(stream, file.toPath());
            authHistoryLastFile = AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID);

            // --------------------
            // Test method args
            // --------------------
            // None.

            // --------------------
            // Mock settings
            // --------------------
            // None.

            // --------------------
            // Expected result
            // --------------------
            // None.

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = AuthHistoryLastFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);

            try {
                // Run method
                method.invoke(authHistoryLastFile);
                fail("Not exception.");
            } catch (InvocationTargetException e) {
                // --------------------
                // Confirm result
                // --------------------
                assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
                PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
                assertThat(exception.getCode(), is(PersoniumCoreException.Common.FILE_IO_ERROR.getCode()));
                assertThat(exception.getCause(), is(instanceOf(ParseException.class)));
            }
        } finally {
            file.delete();
        }
    }

    /**
     * Test save().
     * Normal. create file.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void save_Normal_create() throws Exception {
        String path = testAuthHistoryPath + AuthHistoryLastFile.AUTH_HISTORY_LAST_FILE_NAME;
        File file = new File(path);

        try {
            authHistoryLastFile = AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID);
            authHistoryLastFile.setLastAuthenticated(null);
            authHistoryLastFile.setFailedCount(1L);

            // --------------------
            // Test method args
            // --------------------
            // None.

            // --------------------
            // Mock settings
            // --------------------
            // None.

            // --------------------
            // Expected result
            // --------------------
            // None.

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = AuthHistoryLastFile.class.getDeclaredMethod("save");
            method.setAccessible(true);
            method.invoke(authHistoryLastFile);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(file.exists(), is(true));
            try (Reader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8)) {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                assertThat(json.get("last_authenticated"), is(authHistoryLastFile.getLastAuthenticated()));
                assertThat(json.get("failed_count"), is(authHistoryLastFile.getFailedCount()));
            } catch (Exception e) {
                fail();
            }
        } finally {
            file.delete();
        }
    }

    /**
     * Test save().
     * Normal. update file.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void save_Normal_update() throws Exception {
        String path = testAuthHistoryPath + AuthHistoryLastFile.AUTH_HISTORY_LAST_FILE_NAME;
        File file = new File(path);

        try {
            testAuthHistoryDir.mkdirs();
            testAuthHistoryDir.setWritable(true);
            Files.copy(getSystemResourceAsStream("pauthhistory/authHistoryLastTest/auth_history_last.json"),
                    file.toPath());

            authHistoryLastFile = AuthHistoryLastFile.newInstance(unitTestPath, TEST_ACCOUNT_ID);
            authHistoryLastFile.setLastAuthenticated(12345678L);
            authHistoryLastFile.setFailedCount(123L);

            // --------------------
            // Test method args
            // --------------------
            // None.

            // --------------------
            // Mock settings
            // --------------------
            // None.

            // --------------------
            // Expected result
            // --------------------
            // None.

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = AuthHistoryLastFile.class.getDeclaredMethod("save");
            method.setAccessible(true);
            method.invoke(authHistoryLastFile);

            // --------------------
            // Confirm result
            // --------------------
            assertThat(file.exists(), is(true));
            try (Reader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8)) {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                assertThat(json.get("last_authenticated"), is(authHistoryLastFile.getLastAuthenticated()));
                assertThat(json.get("failed_count"), is(authHistoryLastFile.getFailedCount()));
            } catch (Exception e) {
                fail();
            }
        } finally {
            file.delete();
        }
    }
}

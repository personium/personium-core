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
package io.personium.test.io.model.impl.fs;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
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
import io.personium.core.model.impl.fs.DavMetadataFile;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for DavMetadataFile.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DavMetadataFile.class })
@Category({ Unit.class })
public class DavMetadataFileTest {

    /** Class name. */
    private static final String CLASS_NAME = "DavMetadataFileTest";
    /** Test dir path. */
    private static final String TEST_DIR_PATH = "/personium_nfs/personium-core/unitTest/" + CLASS_NAME + "/";

    /** Test class. */
    private DavMetadataFile davMetadataFile;

    /** Test dir. */
    private static File testDir;

    /** UnitTest path. */
    private static String unitTestPath;

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
        testDir = new File(unitTestPath);
        testDir.mkdirs();
        testDir.setWritable(true);
    }

    /**
     * AfterClass.
     */
    @AfterClass
    public static void afterClass() {
        if (testDir != null && testDir.exists()) {
            testDir.delete();
        }
    }

    /**
     * Test load().
     * Normal.
     * @throws Exception Unintended exception in test
     */
    @Test
    public void load_Normal() throws Exception {
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        davMetadataFile = PowerMockito.spy(DavMetadataFile.newInstance(metaFile));
        PowerMockito.doNothing().when(davMetadataFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        davMetadataFile.load();

        // --------------------
        // Confirm result
        // --------------------
        PowerMockito.verifyPrivate(davMetadataFile, times(1)).invoke("doLoad");
    }

    /**
     * Test load().
     * Normal.
     * Retry OK.
     * @throws Exception Unintended exception in test
     */
    @Ignore
    @Test
    public void load_Normal_retry_ok() throws Exception {
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        davMetadataFile = PowerMockito.spy(DavMetadataFile.newInstance(metaFile));

        // The first and second fail, the third success.
        PersoniumCoreException exception = PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(new IOException());
        ((PowerMockitoStubber) PowerMockito
                .doThrow(exception)
                .doThrow(exception)
                .doNothing()).when(davMetadataFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        davMetadataFile.load();

        // --------------------
        // Confirm result
        // --------------------
        PowerMockito.verifyPrivate(davMetadataFile, times(3)).invoke("doLoad");
    }

    /**
     * Test load().
     * Error.
     * Retry NG.
     * @throws Exception Unintended exception in test
     */
    @Ignore
    @Test
    public void load_Error_retry_ng() throws Exception {
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        davMetadataFile = PowerMockito.spy(DavMetadataFile.newInstance(metaFile));

        // All fail.
        PersoniumCoreException exception = PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(new IOException());
        ((PowerMockitoStubber) PowerMockito
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)
                .doThrow(exception)).when(davMetadataFile, "doLoad");

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        try {
            davMetadataFile.load();
            fail("Not exception.");
        } catch (Exception e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e, is(instanceOf(PersoniumCoreException.class)));
            PersoniumCoreException pe = (PersoniumCoreException) e;
            assertThat(pe.getCode(), is(PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.getCode()));
            assertThat(pe.getCause(), is(instanceOf(IOException.class)));
        }

        PowerMockito.verifyPrivate(davMetadataFile, times(6)).invoke("doLoad");
    }

    /**
     * Test load().
     * Error.
     * Sleep error.
     * @throws Exception Unintended exception in test
     */
    @Test
    @Ignore
    // TODO Thread class mocking can not be done successfully pending.
    public void load_Error_retry_sleep_error() throws Exception {
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        // --------------------
        // Test method args
        // --------------------
        // None.

        // --------------------
        // Mock settings
        // --------------------
        davMetadataFile = PowerMockito.spy(DavMetadataFile.newInstance(metaFile));

        PersoniumCoreException exception = PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(new IOException());

        PowerMockito.doThrow(exception).when(davMetadataFile, "doLoad");

        // Thread class mocking can not be done successfully pending.
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class, "sleep", anyLong());

        // --------------------
        // Expected result
        // --------------------
        // None.

        // --------------------
        // Run method
        // --------------------
        try {
            davMetadataFile.load();
            fail("Not exception.");
        } catch (Exception e) {
            // --------------------
            // Confirm result
            // --------------------
            assertThat(e, is(instanceOf(RuntimeException.class)));
            assertThat(e.getCause(), is(instanceOf(InterruptedException.class)));
        }

        PowerMockito.verifyPrivate(davMetadataFile, times(1)).invoke("doLoad");
    }

    /**
     * Test doLoad().
     * Normal.
     * @throws Exception Unintended exception in test
     */
    @SuppressWarnings("unchecked")
    @Test
    public void doLoad_Normal() throws Exception {
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        try {
            Files.copy(getSystemResourceAsStream("davFile/pmeta01"), metaFile.toPath());
            davMetadataFile = DavMetadataFile.newInstance(metaFile);

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
            expected.put("i", "Drz50UUjQE-V2SkXnqrj0Q");
            expected.put("t", "dav.file");

            JSONObject acl = new JSONObject();
            JSONArray aceArray = new JSONArray();
            JSONObject ace01 = new JSONObject();
            JSONObject principal = new JSONObject();
            JSONObject grant = new JSONObject();
            JSONObject privilege = new JSONObject();
            privilege.put("D.read", null);
            grant.put("D.privilege", privilege);
            principal.put("D.href", "fwo4fjQpS5ikPsa4uT2_CA");
            ace01.put("D.principal", principal);
            ace01.put("D.grant", grant);
            aceArray.add(ace01);
            acl.put("@requireSchemaAuthz", "");
            acl.put("D.ace", aceArray);

            expected.put("a", acl);
            expected.put("d", new JSONObject());
            expected.put("p", 1496130499857L);
            expected.put("u", 1496130599857L);
            expected.put("ct", "text/plan");
            expected.put("cl", 4L);
            expected.put("v", 1L);

            // --------------------
            // Run method
            // --------------------
            // Load methods for private
            Method method = DavMetadataFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);
            method.invoke(davMetadataFile);

            // --------------------
            // Confirm result
            // --------------------
            JSONObject actual = davMetadataFile.getJSON();

            JSONObject actualAcl = (JSONObject) actual.get("a");
            JSONArray actualAceArray = (JSONArray) actualAcl.get("D.ace");
            JSONObject actualAce01 = (JSONObject) actualAceArray.get(0);
            JSONObject actualPrincipal = (JSONObject) actualAce01.get("D.principal");
            JSONObject actualGrant = (JSONObject) actualAce01.get("D.grant");
            JSONObject actualPrivilege = (JSONObject) actualGrant.get("D.privilege");

            assertThat(actual.get("i"), is(expected.get("i")));
            assertThat(actual.get("t"), is(expected.get("t")));

            assertThat(actualPrivilege, is(privilege));
            assertThat(actualPrincipal, is(principal));
            assertThat(actualAcl.get("@requireSchemaAuthz"), is(acl.get("@requireSchemaAuthz")));

            assertThat(actual.get("d"), is(expected.get("d")));
            assertThat(actual.get("p"), is(expected.get("p")));
            assertThat(actual.get("u"), is(expected.get("u")));
            assertThat(actual.get("ct"), is(expected.get("ct")));
            assertThat(actual.get("cl"), is(expected.get("cl")));
            assertThat(actual.get("v"), is(expected.get("v")));
        } finally {
            metaFile.delete();
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
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);

        try {
            davMetadataFile = DavMetadataFile.newInstance(metaFile);

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
            Method method = DavMetadataFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);

            try {
                // Run method
                method.invoke(davMetadataFile);
                fail("Not exception.");
            } catch (InvocationTargetException e) {
                // --------------------
                // Confirm result
                // --------------------
                assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
                PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
                assertThat(exception.getCode(), is(PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.getCode()));
                assertThat(exception.getCause(), is(instanceOf(IOException.class)));
            }
        } finally {
            metaFile.delete();
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
        String metaPath = unitTestPath + "/.pmeta";
        File metaFile = new File(metaPath);
        String pmetaStr = "}{1234567890!#$%&()";

        try (InputStream stream = new ByteArrayInputStream(pmetaStr.getBytes())) {
            Files.copy(stream, metaFile.toPath());
            davMetadataFile = DavMetadataFile.newInstance(metaFile);

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
            Method method = DavMetadataFile.class.getDeclaredMethod("doLoad");
            method.setAccessible(true);

            try {
                // Run method
                method.invoke(davMetadataFile);
                fail("Not exception.");
            } catch (InvocationTargetException e) {
                // --------------------
                // Confirm result
                // --------------------
                assertThat(e.getCause(), is(instanceOf(PersoniumCoreException.class)));
                PersoniumCoreException exception = (PersoniumCoreException) e.getCause();
                assertThat(exception.getCode(), is(PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.getCode()));
                assertThat(exception.getCause(), is(instanceOf(ParseException.class)));
            }
        } finally {
            metaFile.delete();
        }
    }
}

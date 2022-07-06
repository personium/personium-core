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
package io.personium.core.eventlog;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.personium.core.model.Cell;

public class CurrentLogCollectionTest {

    private class TestCurrentLogCollection extends CurrentLogCollection {
        Path realDir;

        TestCurrentLogCollection(Cell cell, String collectionUrl, Path realDir) {
            super(cell, collectionUrl);
            this.realDir = realDir;
        }

        @Override
        protected String getLogFilePath(String filename) {
            return realDir.resolve(filename).toString();
        }
    }

    TestCurrentLogCollection logCollection;
    Path tempFolder = null;

    /**
     * Prepare for test.
     * Create temporal directory.
     */
    @Before
    public void prepareLogCollection() {
        Cell cell = mock(Cell.class);
        when(cell.getPublished()).thenReturn(0L);
        when(cell.getId()).thenReturn("dummyid");
        when(cell.getOwnerNormalized()).thenReturn("dummyowner");

        try {
            tempFolder = Files.createTempDirectory("logcollectiontest");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.logCollection = new TestCurrentLogCollection(cell, "", tempFolder);
    }

    /**
     * Cleanup after test.
     * Delete temporal directory.
     */
    @After
    public void destroyResources() {
        if (tempFolder != null) {
            try {
                FileUtils.deleteDirectory(tempFolder.toFile());
                tempFolder = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Test that isValidLogFile returns true if filename is default.log.
     */
    @Test
    public void isValidLog_returns_true_if_filename_is_default_log() {
        String fileName = "default.log";
        boolean res = logCollection.isValidLogFile(fileName);
        assertTrue(res);
    }

    /**
     * Test that isValidLogFile returns false if filename is illegal.
     */
    @Test
    public void isValidLog_returns_false_if_filename_is_illegal() {
        String fileName = "error.log";
        boolean res = logCollection.isValidLogFile(fileName);
        assertFalse(res);

        fileName = "";
        res = logCollection.isValidLogFile(fileName);
        assertFalse(res);

        fileName = null;
        res = logCollection.isValidLogFile(fileName);
        assertFalse(res);
    }

    /**
     * Test that writeLogData writes empty string when empty filename is presented.
     */
    @Test
    public void writeLogData_writes_empty_string_when_empty_filename_is_presented() {
        String filename = "";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(filename, baos);
            assertEquals(baos.size(), 0);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test that writeLogData writes empty string when file does not exist.
     */
    @Test
    public void writeLogData_writes_empty_string_when_file_does_not_exist() {
        String filename = "non-existing-file.log";

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(filename, baos);
            assertEquals(baos.size(), 0);
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test that writeLogData writes body when file exists.
     */
    @Test
    public void writeLogData_writes_body_when_file_exists() {
        String filename = "TestFile.log";
        final String logContent = "a,b,c\nx,y,z\n";
        try (FileWriter writer = new FileWriter(tempFolder.resolve(filename).toFile())) {
            writer.write(logContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(filename, baos);
            assertArrayEquals(logContent.getBytes("UTF-8"), baos.toByteArray());
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test that writeLogData writes empty string when file exists but contains nothing.
     */
    @Test
    public void writeLogData_writes_empty_string_when_file_exists_but_contains_nothing() {
        String filename = "TestFile.log";
        try (FileWriter writer = new FileWriter(tempFolder.resolve(filename).toFile())) {
            writer.write("");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(filename, baos);
            assertEquals(baos.size(), 0);
        } catch (IOException e) {
            fail();
        }
    }
}

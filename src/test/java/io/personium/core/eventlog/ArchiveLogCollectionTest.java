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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Cell;

public class ArchiveLogCollectionTest {

    private class TestArchiveLogCollection extends ArchiveLogCollection {
        Path realDir;

        TestArchiveLogCollection(Cell cell, String collectionUrl, Path realDir) {
            super(cell, collectionUrl);
            this.realDir = realDir;
        }

        @Override
        protected String getLogArchiveFilename(String filename) {
            return realDir.resolve(filename).toString();
        }
    }

    TestArchiveLogCollection logCollection;
    Path tempFolder = null;

    /**
     * Prepare for test. Create temporal directory.
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

        this.logCollection = new TestArchiveLogCollection(cell, "", tempFolder);
    }

    /**
     * Cleanup after test. Delete temporal directory.
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
     * Test that writeLogData writes body when zip file exists.
     */
    @Test
    public void writeLogData_writes_body_when_zipfile_exists() {
        String filename = "TestFile.log";
        File file = tempFolder.resolve(filename).toFile();
        String zipFilename = file.getAbsolutePath() + ".zip";
        final String logContent = "a,b,c\nx,y,z\n";
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(logContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            createZip(zipFilename, new File[] { file });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(zipFilename, baos);
            assertArrayEquals(logContent.getBytes("UTF-8"), baos.toByteArray());
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test that writeLogData writes empty string when zip file contains empty file.
     */
    @Test
    public void writeLogData_writes_empty_string_when_zipfile_contains_empty_file() {
        String filename = "TestFile.log";
        File file = tempFolder.resolve(filename).toFile();
        String zipFilename = file.getAbsolutePath() + ".zip";
        final String logContent = "";
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(logContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            createZip(zipFilename, new File[] { file });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(zipFilename, baos);
            assertArrayEquals(logContent.getBytes("UTF-8"), baos.toByteArray());
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * Test that writeLogData throws exception when specified file is not zip file.
     */
    @Test
    public void writeLogData_throws_exception_when_specified_file_isnt_archive() {
        String filename = "TestFile.log";
        final String logContent = "a,b,c\nx,y,z\n";
        try (FileWriter writer = new FileWriter(tempFolder.resolve(filename).toFile())) {
            writer.write(logContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(1024)) {
            logCollection.writeLogData(filename, baos);
            fail();
        } catch (IOException e) {
            fail();
        } catch (PersoniumCoreException e) {
            assertEquals(e, PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN);
        }
    }

    /**
     * Test that deleteLogData deletes specified log file.
     */
    @Test
    public void deleteLogData_deletes_log_file() {
        String filename = "TestFile.log";
        File file = tempFolder.resolve(filename).toFile();
        String zipFilename = file.getAbsolutePath() + ".zip";
        final String logContent = "";
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(logContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            createZip(zipFilename, new File[] { file });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logCollection.deleteLogData(zipFilename);

        File zipFile = new File(zipFilename);
        assertFalse(zipFile.exists());
    }

    private void createZip(String fileName, File[] files) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(fileName));
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZipOutputStream zos = new ZipOutputStream(bos);) {
            createZip(zos, files);
        }
    }

    private void createZip(ZipOutputStream zos, File[] files) throws IOException {
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file);
            ) {
                ZipEntry entry = new ZipEntry(file.getName());
                zos.putNextEntry(entry);
                IOUtils.copy(fis, zos);
                zos.closeEntry();
            }
        }
    }
}

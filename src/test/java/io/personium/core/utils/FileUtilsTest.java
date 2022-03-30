/**
 * Personium
 * Copyright 2020-2022 Personium Project Authors
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
package io.personium.core.utils;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import io.personium.test.categories.Unit;

@Category({ Unit.class })
public class FileUtilsTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        tempFolder.newFile();
        final File dir = tempFolder.newFolder();
        Files.createFile(Paths.get(dir.getPath(), "test"));
    }

    @Test
    public void deleteDirectory_GivenPath_Success() throws IOException {
        FileUtils.deleteDirectory(tempFolder.getRoot().toPath());
        assertFalse(tempFolder.getRoot().exists());
    }

    @Test
    public void deleteDirectory_GivenFile_Success() throws IOException {
        FileUtils.deleteDirectory(tempFolder.getRoot());
        assertFalse(tempFolder.getRoot().exists());
    }

    @Test
    public void deleteDirectory_GivenNonExistentFile_Success() throws IOException {
        final File dir = new File("NonExistent");
        assertFalse(dir.exists());
        FileUtils.deleteDirectory(dir);
        assertFalse(dir.exists());
    }
}

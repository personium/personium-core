/**
 * Personium
 * Copyright 2020-2021 Personium Project Authors
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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A class that collects file-related utility functions.
 */
public class FileUtils {

    /**
     * constructor.
     */
    private FileUtils() {
    }

    /**
     * Delete a directory recursively with Files#delete method.
     *
     * @param path directory path to delete
     * @throws IOException in case deletion fails
     */
    public static void deleteDirectory(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Delete a directory recursively with Files#delete method.
     *
     * @param dir directory to delete
     * @throws IOException in case deletion fails
     */
    public static void deleteDirectory(final File dir) throws IOException {
        FileUtils.deleteDirectory(dir.toPath());
    }
}

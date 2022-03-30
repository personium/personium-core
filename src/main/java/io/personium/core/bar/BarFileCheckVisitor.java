/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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
package io.personium.core.bar;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;

/**
 * FileVisitor for check bar file contents.
 */
public class BarFileCheckVisitor implements FileVisitor<Path> {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(BarFileCheckVisitor.class);

    /** MByte to Byte. */
    private static final long MB = 1024 * 1024;

    /** Number of files in bar. */
    private long entryCount = 0L;

    /**
     * Get entry count.
     * @return entry count
     */
    public long getEntryCount() {
        return entryCount;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        entryCount++;
        long maxSize = PersoniumUnitConfig.getBarEntryMaxSize();

        long fileSize = attrs.size();

        if (fileSize > maxSize * MB) {
            String fileName = file.toFile().getName();
            String message = "Bar file entry size too large invalid file [%s: %sB]";
            log.info(String.format(message, fileName, String.valueOf(fileSize)));
            throw PersoniumCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE.params(
                    fileName, String.valueOf(fileSize));
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.error("visitFileFailed. file:" + file.toString());
        throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(file.toString());
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}

/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.snapshot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.file.DataCryptor;
import io.personium.core.model.impl.fs.DavMetadataFile;

/**
 * FileVisitor for copying WebDAV files recursively from zip file to file system.
 * If file encryption is on, encrypt the file and copy it.
 */
public class SnapshotFileImportVisitor implements FileVisitor<Path> {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(SnapshotFileImportVisitor.class);

    /** Target cell id. */
    private String cellId;
    /** WebDAV root directory. */
    private Path webdavRootDir;
    /** WebDAV root directory in zip. */
    private Path webdavRootDirInZip;
    /** Import progress info. */
    private SnapshotFileImportProgressInfo progressInfo;

    /**
     * Constructor.
     * @param cellId Target cell id
     * @param webdavRootDir WebDAV root directory
     * @param webdavRootDirInZip WebDAV root directory in zip
     * @param progressInfo Progress info
     */
    public SnapshotFileImportVisitor(String cellId, Path webdavRootDir, Path webdavRootDirInZip,
            SnapshotFileImportProgressInfo progressInfo) {
        this.cellId = cellId;
        this.webdavRootDir = webdavRootDir;
        this.webdavRootDirInZip = webdavRootDirInZip;
        this.progressInfo = progressInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // Create directory
        Path relativePath = replaceUnderscoreToMainboxId(webdavRootDirInZip.relativize(dir));
        Path path = webdavRootDir.resolve(relativePath.toString());
        Files.createDirectories(path);
        return FileVisitResult.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relativePath = replaceUnderscoreToMainboxId(webdavRootDirInZip.relativize(file));
        Path path = webdavRootDir.resolve(relativePath.toString());

        if (DavMetadataFile.DAV_META_FILE_NAME.equals(file.getFileName().toString())) {
            // Metadata file
            // It reads the setting file and decides whether to encrypt it or not.
            Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING);
            if (PersoniumUnitConfig.isDavEncryptEnabled()) {
                // In the case of ZipPath, toFile() can not be used, so copy it first and rewrite it.
                DavMetadataFile metadata = DavMetadataFile.newInstance(path.toFile());
                metadata.load();
                if (DataCryptor.ENCRYPTION_TYPE_NONE.equals(metadata.getEncryptionType())) {
                    metadata.setEncryptionType(DataCryptor.ENCRYPTION_TYPE_AES);
                    metadata.save();
                }
            }
        } else {
            // Content file
            // It reads the setting file and decides whether to encrypt it or not.
            DataCryptor cryptor = new DataCryptor(cellId);
            try (InputStream in = cryptor.encode(Files.newInputStream(file),
                    PersoniumUnitConfig.isDavEncryptEnabled())) {
                Files.copy(in, path);
            }
        }
        progressInfo.addDelta(1L);
        progressInfo.writeToCache();
        return FileVisitResult.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.error("visitFileFailed. file:" + file.toString());
        throw exc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * Replace "__" in Path with MainBoxID(same as CellID) and return it.
     * @param path target path
     * @return Replaced path
     */
    private Path replaceUnderscoreToMainboxId(Path path) {
        Path resultPath = path;
        if (path.startsWith(SnapshotFile.MAIN_BOX_DIR_NAME)) {
            String replace = path.toString().replaceFirst(SnapshotFile.MAIN_BOX_DIR_NAME, cellId);
            resultPath = Paths.get(replace);
        }
        return resultPath;
    }
}

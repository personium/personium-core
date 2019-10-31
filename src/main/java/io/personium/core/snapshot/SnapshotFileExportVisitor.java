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

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.file.DataCryptor;
import io.personium.core.auth.AuthHistoryLastFile;
import io.personium.core.model.impl.fs.CellKeys;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.model.impl.fs.DavMetadataFile;

/**
 * FileVisitor for copying WebDAV files recursively to zip files.
 * If the file is encrypted, decrypt it and copy it.
 */
public class SnapshotFileExportVisitor implements FileVisitor<Path> {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(SnapshotFileExportVisitor.class);

    /** Target cell id. */
    private String cellId;
    /** WebDAV root directory. */
    private Path webdavRootDir;
    /** WebDAV root directory in zip. */
    private Path webdavRootDirInZip;
    /** Export progress info. */
    private SnapshotFileExportProgressInfo progressInfo;

    /**
     * Constructor.
     * @param cellId Target cell id
     * @param webdavRootDir WebDAV root directory
     * @param webdavRootDirInZip WebDAV root directory in zip
     * @param progressInfo Progress info
     */
    public SnapshotFileExportVisitor(String cellId, Path webdavRootDir, Path webdavRootDirInZip,
            SnapshotFileExportProgressInfo progressInfo) {
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
        Path relativezeDir = webdavRootDir.relativize(dir);
        // Skip export pkeys and pauthhistory.
        if (relativezeDir.startsWith(CellKeys.KEYS_DIR_NAME)
                || relativezeDir.startsWith(AuthHistoryLastFile.AUTH_HISTORY_DIRECTORY)) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        // Create directory in zip
        Path relativePath = replaceMainboxIdToUnderscore(relativezeDir);
        Path pathInZip = webdavRootDirInZip.resolve(relativePath.toString());
        Files.createDirectories(pathInZip);
        return FileVisitResult.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relativePath = replaceMainboxIdToUnderscore(webdavRootDir.relativize(file));
        Path pathInZip = webdavRootDirInZip.resolve(relativePath.toString());

        if (DavMetadataFile.DAV_META_FILE_NAME.equals(file.getFileName().toString())) {
            // Metadata file
            // Load Metadata to determine whether it is encrypted or not
            DavMetadataFile metadata = DavMetadataFile.newInstance(file.toFile());
            metadata.load();
            String encryptionType = metadata.getEncryptionType();
            if (encryptionType != null
                    && !encryptionType.isEmpty()
                    && !DataCryptor.ENCRYPTION_TYPE_NONE.equals(encryptionType)) {
                metadata.setEncryptionType(DataCryptor.ENCRYPTION_TYPE_NONE);
                try (BufferedWriter metadataWriter = Files.newBufferedWriter(pathInZip, Charsets.UTF_8)) {
                    metadataWriter.write(metadata.toJSONString());
                }
            } else {
                Files.copy(file, pathInZip);
            }
        } else if (DavCmpFsImpl.CONTENT_FILE_NAME.equals(file.getFileName().toString())) {
            // Content file
            // Load Metadata to determine whether it is encrypted or not
            Path metadataPath = file.getParent().resolve(DavMetadataFile.DAV_META_FILE_NAME);
            DavMetadataFile metadata = DavMetadataFile.newInstance(metadataPath.toFile());
            metadata.load();
            DataCryptor cryptor = new DataCryptor(cellId);
            try (InputStream in = cryptor.decode(new FileInputStream(file.toFile()), metadata.getEncryptionType())) {
                Files.copy(in, pathInZip);
            }
        } else {
            // Metafile other than DavMetadata.
            // Because encryption is not done, only copy files.
            Files.copy(file, pathInZip);
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
     * Replace MainBoxID(same as CellID) in Path with "__" and return it.
     * @param path target path
     * @return Replaced path
     */
    private Path replaceMainboxIdToUnderscore(Path path) {
        Path resultPath = path;
        if (path.startsWith(cellId)) {
            String replace = path.toString().replaceFirst(cellId, SnapshotFile.MAIN_BOX_DIR_NAME);
            resultPath = Paths.get(replace);
        }
        return resultPath;
    }
}

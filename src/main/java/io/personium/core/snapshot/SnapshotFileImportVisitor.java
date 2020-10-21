/**
 * Personium
 * Copyright 2017-2020 Personium Project Authors
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.file.DataCryptor;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.DavCmp;
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
    /** OData id map. */
    private Map<String, String> odataIdMap;
    /** WebDAV root directory. */
    private Path webdavRootDir;
    /** WebDAV root directory in zip. */
    private Path webdavRootDirInZip;
    /** Import progress info. */
    private SnapshotFileImportProgressInfo progressInfo;

    /**
     * Constructor.
     * @param cellId Target cell id
     * @param odataIdMap odata id map
     * @param webdavRootDir WebDAV root directory
     * @param webdavRootDirInZip WebDAV root directory in zip
     * @param progressInfo Progress info
     */
    public SnapshotFileImportVisitor(String cellId, Map<String, String> odataIdMap,
            Path webdavRootDir, Path webdavRootDirInZip, SnapshotFileImportProgressInfo progressInfo) {
        this.cellId = cellId;
        this.odataIdMap = odataIdMap;
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
        Path relativePath = replaceBoxDirName(webdavRootDirInZip.relativize(dir));
        Path path = webdavRootDir.resolve(relativePath.toString());
        Files.createDirectories(path);
        return FileVisitResult.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path relativePath = replaceBoxDirName(webdavRootDirInZip.relativize(file));
        Path path = webdavRootDir.resolve(relativePath.toString());

        if (DavMetadataFile.DAV_META_FILE_NAME.equals(file.getFileName().toString())) {
            // Metadata file
            // It reads the setting file and decides whether to encrypt it or not.
            Path tempPath = webdavRootDir.resolve(relativePath.toString() + ".temp");
            Files.copy(file, tempPath);
            // In order to perform atomic file operation, it moves after copying.
            Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(path.toFile());
            }

            DavMetadataFile metadata = DavMetadataFile.newInstance(path.toFile());
            metadata.load();
            // Replace acl.
            String aclString = replaceIdString(metadata.getAcl().toJSONString());
            try {
                JSONObject aclJson = (JSONObject) new JSONParser().parse(aclString);
                metadata.setAcl(aclJson);
            } catch (ParseException e) {
                throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(aclString);
            }
            // Recreate node id.
            if (DavCmp.TYPE_COL_ODATA.equals(metadata.getNodeType())) {
                String newId = PersoniumUUID.randomUUID();
                odataIdMap.put(metadata.getNodeId(), newId);
                metadata.setNodeId(newId);
            }
            // Set encryption type.
            if (PersoniumUnitConfig.isDavEncryptEnabled()
                    && DataCryptor.ENCRYPTION_TYPE_NONE.equals(metadata.getEncryptionType())) {
                // In the case of ZipPath, toFile() can not be used, so copy it first and rewrite it.
                metadata.setEncryptionType(DataCryptor.ENCRYPTION_TYPE_AES);
            }
            metadata.save();
        } else {
            // Content file
            // It reads the setting file and decides whether to encrypt it or not.
            DataCryptor cryptor = new DataCryptor(cellId);
            try (InputStream in = cryptor.encode(Files.newInputStream(file),
                    PersoniumUnitConfig.isDavEncryptEnabled())) {
                Files.copy(in, path);
            }
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(path.toFile());
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
     * Replace _id.
     * @param source target string
     * @return replaced string
     */
    private String replaceIdString(String source) {
        String replacedString = source;
        for (String oldId : odataIdMap.keySet()) {
            String newId = odataIdMap.get(oldId);
            replacedString = replacedString.replaceAll(oldId, newId);
        }
        return replacedString;
    }

    /**
     * Replace box dir name and return it.
     * @param path target path
     * @return Replaced path
     */
    private Path replaceBoxDirName(Path path) {
        Path resultPath = path;
        resultPath = replaceUnderscoreToMainboxId(resultPath);
        resultPath = replaceOldIdToNewId(resultPath);
        return resultPath;
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

    /**
     * Replace box dir name. Old _id to new _id.
     * @param path target path
     * @return Replaced path
     */
    private Path replaceOldIdToNewId(Path path) {
        Path resultPath = path;
        for (String oldId : odataIdMap.keySet()) {
            if (path.startsWith(oldId)) {
                String newId = odataIdMap.get(oldId);
                String replace = path.toString().replaceFirst(oldId, newId);
                resultPath = Paths.get(replace);
                break;
            }

        }
        return resultPath;
    }

    /**
     * sync file.
     *
     * @param file target file
     * @throws IOException failed sync file
     */
    private void sync(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.getFD().sync();
        } catch (IOException e) {
            throw e;
        }
    }
}

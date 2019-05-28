/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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
package io.personium.core.model.impl.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;

/**
 * Class that manages cell specific key information.
 */
public class CellKeys {
    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(CellKeys.class);

    /** Keys file storage directory name. */
    public static final String KEYS_DIR_NAME = ".pkeys";

    /**
     * Cell specific key information.
     * It is a map because there is a possibility of having two or more information in the future.
     * Currently only one can be created.
     */
    private Map<String, CellKeysFile> keysFileMap;
    /** Keys file storage directory path. */
    private Path keysDirPath;

    /**
     * Constructor.
     * @param keysDirPath Keys file storage directory path
     */
    public CellKeys(Path keysDirPath) {
        keysFileMap = new HashMap<>();
        this.keysDirPath = keysDirPath;
    }

    /**
     * Read the keys file.
     * If it does not exist, it creates and returns it.
     * @return CellKeys
     */
    public CellKeys load() {
        log.debug("load started.");
        if (exists()) {
            doLoad();
        } else {
            // Automatically generated if keys file does not exist.
            doCreate();
        }
        log.debug("load ended.");
        return this;
    }

    /**
     * Get CellKeysFile.
     * Currently return only one.
     * In the future we will return map with another method.
     * @return CellKeysFile
     */
    public CellKeysFile getCellKeysFile() {
        CellKeysFile cellKeysFile = null;
        for (String keyId : keysFileMap.keySet()) {
            cellKeysFile = keysFileMap.get(keyId);
        }
        return cellKeysFile;
    }

    /**
     * Check the existence of the keys file.
     * @return true:exists false:not exists
     */
    private boolean exists() {
        // Existence check of keysDir.
        if (!Files.exists(keysDirPath)) {
            return false;
        }
        // Existence check under keysDir.
        try (Stream<Path> paths = Files.list(keysDirPath)) {
            return paths.findAny().isPresent();
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read KeysDir").reason(e);
        }
    }

    /**
     * Load keys file.
     */
    private void doLoad() {
        for (String keyId : keysDirPath.toFile().list()) {
            CellKeysFile cellKeysFile = CellKeysFile.load(keysDirPath, keyId);
            keysFileMap.put(keyId, cellKeysFile);
        }
    }

    /**
     * Create keys file.
     */
    private void doCreate() {
        CellKeysFile cellKeysFile = CellKeysFile.newInstance(keysDirPath);
        // Perform presence check again just before file writing.
        // To reduce the risk of writing at the same timing.
        if (exists()) {
            doLoad();
        }
        // Write to the file.
        cellKeysFile.save();
        keysFileMap.put(cellKeysFile.getKeyId(), cellKeysFile);
    }
}

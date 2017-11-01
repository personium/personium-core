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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.impl.fs.DavCmpFsImpl;

/**
 * Manage the snapshot file.
 * <p>
 * Planned to write the processing of the "__snapshot" API here,
 * but in order to use the existing source of WebDAV, I described it under the model package.
 * XXX The subsequent processing is dedicated to elasticsearch and filesystem
 * as a result of considering processing speed and creation period.
 */
public class SnapshotFileManager {

//    /** Logger. */
//    private static Logger log = LoggerFactory.getLogger(SnapshotFileManager.class);

    /** Extension of the snapshot file to be created. */
    private static final String SNAPSHOT_FILE_EXTENSION = ".zip";

    /** Target cell object. */
    private Cell targetCell;
    /** Snapshot file name. */
    private String snapshotFileName;

    /**
     * Constructor.
     * @param targetCell Target cell object
     * @param snapshotFileName Snapshot file name(no extension)
     */
    public SnapshotFileManager(Cell targetCell, String snapshotFileName) {
        this.targetCell = targetCell;
        this.snapshotFileName = snapshotFileName + SNAPSHOT_FILE_EXTENSION;
    }

    /**
     * Execute export.
     */
    public void exportSnapshot() {
        Path snapshotDirPath = Paths.get(PersoniumUnitConfig.getCellExportRoot(),
                targetCell.getId(), snapshotFileName);
        // File duplication check
        if (Files.exists(snapshotDirPath)) {
            throw PersoniumCoreException.Dav.FILE_ALREADY_EXISTS.params(snapshotFileName);
        }
        // Create snapshot directory.
        // A directory named {Name}.zip is created
        try {
            Files.createDirectories(snapshotDirPath);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create snapshot directory").reason(e);
        }

        Path snapshotFilePath = snapshotDirPath.resolve(DavCmpFsImpl.CONTENT_FILE_NAME);

        SnapshotFileExportRunner runner = new SnapshotFileExportRunner(targetCell, snapshotFilePath);
        PersoniumThread.execute(runner);
    }

    /**
     * Execute import.
     */
    public void importSnapshot() {
        Path snapshotDirPath = Paths.get(PersoniumUnitConfig.getCellExportRoot(), targetCell.getId(), snapshotFileName);
        // File exists check.
        if (!Files.exists(snapshotDirPath)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(snapshotFileName);
        }

        Path snapshotFilePath = snapshotDirPath.resolve(DavCmpFsImpl.CONTENT_FILE_NAME);

        SnapshotFileImportRunner runner = new SnapshotFileImportRunner(targetCell, snapshotFilePath);
        PersoniumThread.execute(runner);
    }
}

/**
 * Personium
 * Copyright 2017-2022 Personium Project Authors
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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.model.lock.CellLockManager;

/**
 * Manage the snapshot file.
 * <p>
 * Planned to write the processing of the "__snapshot" API here,
 * but in order to use the existing source of WebDAV, I described it under the model package.
 * XXX The subsequent processing is dedicated to elasticsearch and filesystem
 * as a result of considering processing speed and creation period.
 */
public class SnapshotFileManager {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(SnapshotFileManager.class);

    /**
     * Snapshot api version.
     * <p>
     * In the future, increment such as when changing the configuration of snapshot file.
     * Refer to the value with the import API and switch the processing.
     */
    public static final long SNAPSHOT_API_VERSION = 2L;
    /** Manifest json key : export api version. */
    public static final String MANIFEST_JSON_KEY_EXPORT_VERSION = "export_version";
    /** Manifest json key : export unit url. */
    public static final String MANIFEST_JSON_KEY_UNIT_URL = "unit_url";
    /** Manifest json key : snapshot create date. */
    public static final String MANIFEST_JSON_KEY_CREATE_DATE = "create_date";

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
        Path snapshotDirPath = Paths.get(PersoniumUnitConfig.getCellSnapshotRoot(),
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

        waitCellAccessible(targetCell.getId());
        try {
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.EXPORT);
            SnapshotFileExportRunner runner = new SnapshotFileExportRunner(targetCell, snapshotFilePath);
            PersoniumThread.CELL_IO.execute(runner);
        } catch (Throwable e) {
            // If an exception occurs before the execution of the thread, return the lock status to its original state.
            // If it is normal, lock is released in the thread.
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.NORMAL);
            throw e;
        }
    }

    /**
     * Execute import.
     */
    public void importSnapshot() {
        Path snapshotDirPath = Paths.get(PersoniumUnitConfig.getCellSnapshotRoot(),
                targetCell.getId(), snapshotFileName);
        // File exists check.
        if (!Files.exists(snapshotDirPath)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(snapshotFileName);
        }

        Path snapshotFilePath = snapshotDirPath.resolve(DavCmpFsImpl.CONTENT_FILE_NAME);

        validateSnapshot(snapshotFilePath);

        waitCellAccessible(targetCell.getId());
        try {
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.IMPORT);
            SnapshotFileImportRunner runner = new SnapshotFileImportRunner(targetCell, snapshotFilePath);
            PersoniumThread.CELL_IO.execute(runner);
        } catch (Throwable e) {
            // If an exception occurs before the execution of the thread, return the lock status to its original state.
            // If it is normal, lock is released in the thread.
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.NORMAL);
            throw e;
        }
    }

    /**
     * Wait for other access to the specified cell to be completed.
     * Exception is thrown if maximum wait time set by UnitConfig elapses.
     * @param cellId target cell id
     * @throws maximum wait time elapses
     */
    private void waitCellAccessible(String cellId) throws PersoniumCoreException {
        int maxLoopCount = PersoniumUnitConfig.getCellLockRetryTimes();
        long interval = PersoniumUnitConfig.getCellLockRetryInterval();

        for (int loopCount = 0; loopCount < maxLoopCount; loopCount++) {
            long count = CellLockManager.getReferenceCount(cellId);
            // Since it includes this request, it is larger than 1 if there are other requests.
            if (count <= 1) {
                return;
            }
            try {
                log.info(String.format("Wait for other access to cell. ReferenceCount:%d", count));
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
            }
        }
        throw PersoniumCoreException.Misc.CONFLICT_CELLACCESS;
    }

    /**
     * Validate snapshoat file structure.
     * @param snapshotFilePath snapshot file
     */
    private void validateSnapshot(Path snapshotFilePath) {
        try (SnapshotFile snapshotFile = SnapshotFile.newInstance(snapshotFilePath)) {
            // Check version.
            String manifestJsonStr = snapshotFile.readManifestJson();
            JSONObject manifestJson;
            try {
                manifestJson = (JSONObject) new JSONParser().parse(manifestJsonStr);
            } catch (ParseException e) {
                throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(manifestJsonStr);
            }
            Long exportVersion = (Long) manifestJson.get(MANIFEST_JSON_KEY_EXPORT_VERSION);
            if (exportVersion == null || !exportVersion.equals(SNAPSHOT_API_VERSION)) {
                throw PersoniumCoreException.Misc.SNAPSHOT_VERSION_INVALID.params(SNAPSHOT_API_VERSION, exportVersion);
            }
            // Check structure.
            snapshotFile.checkStructure();
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read snapshot file").reason(e);
        } catch (UnsupportedOperationException e) {
            throw PersoniumCoreException.Misc.SNAPSHOT_IS_NOT_ZIP;
        }
    }
}

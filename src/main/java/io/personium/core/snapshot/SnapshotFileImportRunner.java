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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsBulkRequest;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.rs.odata.MapBulkRequest;
import io.personium.core.utils.UriUtils;

/**
 * Runner that performs cell import processing.
 */
public class SnapshotFileImportRunner implements Runnable {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(SnapshotFileImportRunner.class);

    /** Limit when bulk request. */
    private static final int BULK_REQUEST_LIMIT = 1000;

    /** Target cell object. */
    private Cell targetCell;
    /** Snapshot file path. */
    private Path snapshotFilePath;
    /** Progress info. */
    private SnapshotFileImportProgressInfo progressInfo;
    /**
     * OData _id mapping.
     * key: old _id.
     * value: new _id.
     */
    private Map<String, String> odataIdMap;

    /**
     * Constructor.
     * @param targetCell Target cell
     * @param snapshotFilePath Snapshot file path
     */
    public SnapshotFileImportRunner(Cell targetCell, Path snapshotFilePath) {
        this.targetCell = targetCell;
        this.snapshotFilePath = snapshotFilePath;
        String snapshotName = FilenameUtils.getBaseName(this.snapshotFilePath.getParent().getFileName().toString());
        long entryCount = countEntry();
        progressInfo = new SnapshotFileImportProgressInfo(this.targetCell.getId(), snapshotName, entryCount);
        odataIdMap = new HashMap<String, String>();
        log.info(String.format("Setup cell import. CellName:%s, EntryCount:%d, SnapshotName:%s",
                this.targetCell.getName(), entryCount, snapshotName));
    }

    /**
     * Import snapshot file.
     */
    @Override
    public void run() {
        try {
            log.info(String.format("Start import. CellName:%s", targetCell.getName()));
            try (SnapshotFile snapshotFile = SnapshotFile.newInstance(snapshotFilePath)) {
                // start import.
                progressInfo.writeToCache(true);
                // Delete cell data.
                deleteCellData();
                // Import snapshot.
                makeCellData(snapshotFile);
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read snapshot file").reason(e);
            }
            // Write 100%. It clears immediately, but it writes once.
            progressInfo.writeToCache(true);
            // Change cell status.
            changeCellStatus(Cell.STATUS_NORMAL);
            // Delete error file.
            deleteErrorFile();
            // Post event to EventBus.
            String object = UriUtils.SCHEME_LOCALCELL + ":/__import";
            String info = "";
            String type = PersoniumEventType.cell(PersoniumEventType.Operation.IMPORT);
            PersoniumEvent event = new PersoniumEvent.Builder()
                    .type(type)
                    .object(object)
                    .info(info)
                    .build();
            EventBus eventBus = targetCell.getEventBus();
            eventBus.post(event);
        } catch (Throwable e) {
            // When processing fails, output error file.
            progressInfo.setStatus(SnapshotFileImportProgressInfo.STATUS.IMPORT_FAILED);
            // Create error file.
            makeErrorFile(e);
            log.info(String.format("Made error file."));
            // Change cell status.
            changeCellStatus(Cell.STATUS_IMPORT_ERROR);
        } finally {
            // Delete progress info.
            progressInfo.deleteFromCache();
            // Unlock the cell.
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.NORMAL);
            log.info(String.format("End import. CellName:%s", targetCell.getName()));
        }
    }

    /**
     * Returns the number of entry.
     * @return number of entry
     */
    private long countEntry() {
        try (SnapshotFile snapshotFile = SnapshotFile.newInstance(snapshotFilePath)) {
            return snapshotFile.countODataPJson() + snapshotFile.countWebDAVFile();
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read snapshot file").reason(e);
        }
    }

    /**
     * Delete cell data.
     */
    private void deleteCellData() {
        deleteWebDAV();
        deleteOData();
    }

    /**
     * Delete cell webdav data.
     */
    private void deleteWebDAV() {
        Path webdavRootPath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId());

        // Since want to keep .pmeta file under the cell, delete only the directory.
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        File[] dirList = webdavRootPath.toFile().listFiles(filter);

        if (dirList == null || dirList.length == 0) {
            return;
        }
        try {
            for (File dir : dirList) {
                FileUtils.deleteDirectory(dir);
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("delete WebDAV files").reason(e);
        }
    }

    /**
     * Delete cell odata data.
     */
    private void deleteOData() {
        CellDataAccessor accessor = EsModel.cellData(targetCell.getDataBundleNameWithOutPrefix(), targetCell.getId());
        accessor.bulkDeleteCell();
    }

    /**
     * Make the contents of the cell.
     * @param snapshotFile snapshot file
     */
    private void makeCellData(SnapshotFile snapshotFile) {
        modifyCellInfo(snapshotFile);
        log.info(String.format("Modified cell info."));
        addCellLevelODataToCell(snapshotFile);
        log.info(String.format("Added cell level odata."));
        addWebDAVToCell(snapshotFile);
        log.info(String.format("Added webdav file."));
        addBoxLevelODataToCell(snapshotFile);
        log.info(String.format("Added box level odata."));
    }

    /**
     * Extract cell data from snapshot file and modify the cell.
     * @param snapshotFile snapshot file
     */
    @SuppressWarnings("unchecked")
    private void modifyCellInfo(SnapshotFile snapshotFile) {
        // The content of the current Cell is not updated.
        // It only overwrites the update date with the current time.

        String cellJsonStr = snapshotFile.readCellJson();
        JSONObject cellJson;
        try {
            cellJson = (JSONObject) new JSONParser().parse(cellJsonStr);
        } catch (ParseException e) {
            throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(cellJsonStr);
        }

        // When owner attribute is rewritten, since it is treated as data of another cell,
        // it is overwritten with information of target cell.
        Map<String, Object> map = jsonToMap((JSONObject) cellJson.get("_source"));
        Map<String, Object> s = (Map<String, Object>) map.get(OEntityDocHandler.KEY_STATIC_FIELDS);
        s.put("Name", targetCell.getName());
        Map<String, Object> h = (Map<String, Object>) map.get(OEntityDocHandler.KEY_HIDDEN_FIELDS);
        String owner = UriUtils.convertSchemeFromHttpToLocalUnit(targetCell.getUnitUrl(), targetCell.getOwner());
        h.put("Owner", owner);
        map.put(OEntityDocHandler.KEY_UPDATED, System.currentTimeMillis());

        CellAccessor cellAccessor = (CellAccessor) EsModel.cell();
        cellAccessor.update(targetCell.getId(), map);
    }

    /**
     * Extract cell level odata from snapshot file and add it to cell.
     * @param snapshotFile snapshot file
     */
    private void addCellLevelODataToCell(SnapshotFile snapshotFile) {
        for (String key : SnapshotFile.ODATA_PJSON_CELL_LEVEL_MAP.keySet()) {
            addODataToCell(key, snapshotFile);
        }
    }

    /**
     * Extract box level odata from snapshot file and add it to cell.
     * @param snapshotFile snapshot file
     */
    private void addBoxLevelODataToCell(SnapshotFile snapshotFile) {
        for (String key : SnapshotFile.ODATA_PJSON_BOX_LEVEL_MAP.keySet()) {
            addODataToCell(key, snapshotFile);
        }
    }

    /**
     * Extract odata from snapshot file and add it to cell.
     * @param typeName Es type name
     * @param snapshotFile snapshot file
     */
    private void addODataToCell(String typeName, SnapshotFile snapshotFile) {
        try (BufferedReader bufferedReader = snapshotFile.getODataPJsonReader(typeName)) {
            String line = null;
            DataSourceAccessor accessor = EsModel.batch(targetCell);
            List<EsBulkRequest> bulkRequestList = new ArrayList<>();
            while ((line = bufferedReader.readLine()) != null) {
                String replacedLine = replaceIdString(line);
                JSONObject dataJson;
                try {
                    dataJson = (JSONObject) new JSONParser().parse(replacedLine);
                } catch (ParseException e) {
                    throw PersoniumCoreException.Common.JSON_PARSE_ERROR.params(replacedLine);
                }

                // When c attribute is rewritten, since it is treated as data of another cell,
                // it is overwritten with information of target cell.
                Map<String, Object> map = jsonToMap((JSONObject) dataJson.get("_source"));
                map.put("c", targetCell.getId());

                String type = (String) dataJson.get("_type");
                String oldId = (String) dataJson.get("_id");
                String newId = PersoniumUUID.randomUUID();
                odataIdMap.put(oldId, newId);
                bulkRequestList.add(new MapBulkRequest(EsBulkRequest.BulkRequestType.INDEX, type, newId, map));

                if (BULK_REQUEST_LIMIT <= bulkRequestList.size()) {
                    accessor.bulkCreate(bulkRequestList, targetCell.getId());
                    progressInfo.addDelta(bulkRequestList.size());
                    progressInfo.writeToCache();
                    bulkRequestList.clear();
                }
            }
            if (!bulkRequestList.isEmpty()) {
                accessor.bulkCreate(bulkRequestList, targetCell.getId());
                progressInfo.addDelta(bulkRequestList.size());
                progressInfo.writeToCache();
            }
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read data pjson from snapshot file").reason(e);
        }
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
     * Extract webdav file from snapshot file and add it to cell.
     * Encrypt the file according to the setting of the unitconfig property.
     * @param snapshotFile snapshot file
     */
    private void addWebDAVToCell(SnapshotFile snapshotFile) {
        Path webdavRootPathInZip = snapshotFile.getWebDAVDirPath();
        Path webdavRootPath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId());
        // Use FileVisitor to process files recursively
        FileVisitor<Path> visitor = new SnapshotFileImportVisitor(targetCell.getId(),
                odataIdMap, webdavRootPath, webdavRootPathInZip.toAbsolutePath(), progressInfo);
        try {
            Files.walkFileTree(webdavRootPathInZip.toAbsolutePath(), visitor);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("copy webdav data from snapshot file").reason(e);
        }
    }

    /**
     * Change cell status to error.
     */
    private void changeCellStatus(String status) {
        CellCmp cellCmp = ModelFactory.cellCmp(targetCell);
        if (!status.equals(cellCmp.getCellStatus())) {
            cellCmp.setCellStatusAndSave(status);
        }
    }

    /**
     * Make error file.
     * @param e error
     */
    private void makeErrorFile(Throwable e) {
        progressInfo.setErrorMessage(e);
        Path errorFilePath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId(), Cell.IMPORT_ERROR_FILE_NAME);
        progressInfo.writeToFile(errorFilePath);
    }

    /**
     * Delete error file.
     */
    private void deleteErrorFile() {
        Path errorFilePath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId(), Cell.IMPORT_ERROR_FILE_NAME);
        try {
            Files.deleteIfExists(errorFilePath);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("delete error file").reason(e);
        }
    }

    /**
     * Convert json object to map.
     * @param json source json
     * @return map
     */
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> retMap = new HashMap<String, Object>();
        if (json != null) {
            retMap = toMap(json);
        }
        return retMap;
    }

    /**
     * Convert json object to map.
     * @param object source json
     * @return map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<String, Object>();

        Set<String> keys = object.keySet();
        for (String key : keys) {
            Object value = object.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    /**
     * Convert json array to list.
     * @param array source json array
     * @return list
     */
    private List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for (Object value : array) {
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}

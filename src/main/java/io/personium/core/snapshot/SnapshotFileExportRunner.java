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

import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALCELL;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.file.DataCryptor;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.EsQueryHandler;
import io.personium.core.model.impl.fs.DavCmpFsImpl;
import io.personium.core.model.impl.fs.DavMetadataFile;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.utils.FileUtils;

/**
 * Runner that performs cell export processing.
 */
public class SnapshotFileExportRunner implements Runnable {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(SnapshotFileExportRunner.class);

    /** Limit when retrieving OData. */
    private static final int SEARCH_LIMIT = 1000;

    /** Extension of the error file. */
    private static final String ERROR_FILE_EXTENSION = ".error";

    /** Target cell object. */
    private Cell targetCell;
    /** Snapshot file path. */
    private Path snapshotFilePath;
    /** Progress info. */
    private SnapshotFileExportProgressInfo progressInfo;

    /**
     * Constructor.
     * @param targetCell Target cell
     * @param snapshotFilePath Snapshot file path
     */
    public SnapshotFileExportRunner(Cell targetCell, Path snapshotFilePath) {
        this.targetCell = targetCell;
        this.snapshotFilePath = snapshotFilePath;
        String snapshotName = FilenameUtils.getBaseName(this.snapshotFilePath.getParent().getFileName().toString());
        Path webdavRootPath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId());
        long entryCount = countODataEntry() + countWebDAVFile(webdavRootPath.toFile());
        progressInfo = new SnapshotFileExportProgressInfo(this.targetCell.getId(), snapshotName, entryCount);
        log.info(String.format("Setup cell export. CellName:%s, EntryCount:%d, SnapshotName:%s",
                this.targetCell.getName(), entryCount, snapshotName));
    }

    /**
     * Export snapshot file.
     */
    @Override
    public void run() {
        try {
            log.info(String.format("Start export. CellName:%s", targetCell.getName()));
            // start export.
            progressInfo.writeToCache(true);
            try (SnapshotFile snapshotFile = SnapshotFile.newInstance(snapshotFilePath)) {
                // Make the contents of the zip file.
                makeSnapshotFile(snapshotFile);
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create snapshot file").reason(e);
            } catch (UnsupportedOperationException e) {
                throw PersoniumCoreException.Misc.SNAPSHOT_IS_NOT_ZIP;
            }

            // Sync snapshot file.
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                try (FileOutputStream fos = new FileOutputStream(snapshotFilePath.toFile(), true)) {
                    fos.getFD().sync();
                } catch (IOException e) {
                    throw PersoniumCoreException.Common.FILE_IO_ERROR.params("sync failed").reason(e);
                }
            }

            // Write 100%. It clears immediately, but it writes once.
            progressInfo.writeToCache(true);
            // Create new metadata file
            createMetaFile();
            // Post event to EventBus.
            String object = SCHEME_LOCALCELL + ":/__export";
            String info = "";
            String type = PersoniumEventType.cell(PersoniumEventType.Operation.EXPORT);
            PersoniumEvent event = new PersoniumEvent.Builder()
                    .type(type)
                    .object(object)
                    .info(info)
                    .build();
            EventBus eventBus = targetCell.getEventBus();
            eventBus.post(event);
        } catch (Throwable e) {
            // When processing fails, delete snapshot file and output error file.
            // Delete snapshot file.
            deleteSnapshotFile();
            // Create error file.
            makeErrorFile(e);
            log.info(String.format("Made error file."));
        } finally {
            // Delete progress info.
            progressInfo.deleteFromCache();
            // Unlock the cell.
            CellLockManager.setCellStatus(targetCell.getId(), CellLockManager.STATUS.NORMAL);
            log.info(String.format("End export. CellName:%s", targetCell.getName()));
        }
    }

    /**
     * Returns the number of odata entry.
     * @return number of odata entry
     */
    private long countODataEntry() {
        // Specifying filter
        Map<String, Object> filter = new HashMap<String, Object>();
        filter = QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, targetCell.getId());
        Map<String, Object> filtered = new HashMap<String, Object>();
        filtered = QueryMapFactory.filteredQuery(null, filter);

        // Generate query
        long queryFrom = 0L;
        Map<String, Object> query = QueryMapFactory.query(filtered);
        query.put("from", queryFrom);

        // Get index accessor of Es
        String indexName = targetCell.getDataBundleName();
        DataSourceAccessor dataSourceAccessor = EsModel.getDataSourceAccessorFromIndexName(indexName);

        return dataSourceAccessor.countForIndex(targetCell.getId(), query);
    }

    /**
     * Returns the number of files under the directory.
     * This method arg is directory only.
     * @return number of files
     */
    private long countWebDAVFile(File dir) {
        long count = 0L;

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                count += countWebDAVFile(file);
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Make the contents of the zip file.
     * @param snapshotFile snapshot file
     */
    private void makeSnapshotFile(SnapshotFile snapshotFile) {
        addManifestToZip(snapshotFile);
        log.info(String.format("Added manifest json."));
        addCellToZip(snapshotFile);
        log.info(String.format("Added cell json."));
        addODataToZip(snapshotFile);
        log.info(String.format("Added odata pjson."));
        addWebDAVToZip(snapshotFile);
        log.info(String.format("Added webdav file."));
    }

    /**
     * Generate manifest json file and add it to zip file.
     * @param snapshotFile snapshot file
     */
    @SuppressWarnings("unchecked")
    private void addManifestToZip(SnapshotFile snapshotFile) {
        JSONObject manifestJson = new JSONObject();
        manifestJson.put(SnapshotFileManager.MANIFEST_JSON_KEY_EXPORT_VERSION,
                SnapshotFileManager.SNAPSHOT_API_VERSION);
        manifestJson.put(SnapshotFileManager.MANIFEST_JSON_KEY_UNIT_URL, targetCell.getUnitUrl());
        manifestJson.put(SnapshotFileManager.MANIFEST_JSON_KEY_CREATE_DATE, System.currentTimeMillis());
        snapshotFile.writeManifestJson(manifestJson.toJSONString());
    }

    /**
     * Extract cell data from OData and add it to zip file.
     * @param snapshotFile snapshot file
     */
    @SuppressWarnings("unchecked")
    private void addCellToZip(SnapshotFile snapshotFile) {
        // Get cell data by specifying CellID
        EntitySetAccessor accessor = EsModel.cell();
        PersoniumGetResponse response = accessor.get(targetCell.getId());

        JSONObject resultJson = new JSONObject();
        resultJson.put("_index", response.getIndex());
        resultJson.put("_type", response.getType());
        resultJson.put("_id", response.getId());
        resultJson.put("_source", response.getSource());

        snapshotFile.writeCellJson(resultJson.toJSONString());
    }

    /**
     * Extract data other than cells from OData and add it to the zip file.
     * @param snapshotFile snapshot file
     */
    private void addODataToZip(SnapshotFile snapshotFile) {
        for (String key : SnapshotFile.ODATA_PJSON_CELL_LEVEL_MAP.keySet()) {
            addODataToZip(key, snapshotFile);
        }
        for (String key : SnapshotFile.ODATA_PJSON_BOX_LEVEL_MAP.keySet()) {
            addODataToZip(key, snapshotFile);
        }
    }

    /**
     * Extract data other than cells from OData and add it to the zip file.
     * @param typeName Es type name
     * @param snapshotFile snapshot file
     */
    @SuppressWarnings("unchecked")
    private void addODataToZip(String typeName, SnapshotFile snapshotFile) {
        // Specifying filter
        Map<String, Object> filter = new HashMap<String, Object>();
        filter = QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, targetCell.getId());
        Map<String, Object> filtered = new HashMap<String, Object>();
        filtered = QueryMapFactory.filteredQuery(null, filter);

        // Specifying sort
        List<Map<String, Object>> sortList = new ArrayList<Map<String, Object>>();
        sortList.add(QueryMapFactory.sortQuery("_type", EsQueryHandler.SORT_ASC));
        sortList.add(QueryMapFactory.sortQuery("_uid", EsQueryHandler.SORT_ASC));

        // Generate query
        long queryFrom = 0L;
        Map<String, Object> query = QueryMapFactory.query(filtered);
        query.put("sort", sortList);
        query.put("from", queryFrom);
        query.put("size", SEARCH_LIMIT);

        // Get index accessor of Es
        EntitySetAccessor entitySetAccessor = EsModel.cellCtl(targetCell, typeName);

        // At least create an empty file.
        snapshotFile.createODataPJson(typeName);
        while (true) {
            // Search Es
            PersoniumSearchResponse response = entitySetAccessor.search(query);
            if (response.getHits().getCount() == 0) {
                break;
            }

            JSONObject resultJson = new JSONObject();
            StringBuilder builder = new StringBuilder();
            for (PersoniumSearchHit hit : response.getHits().getHits()) {
                resultJson.put("_index", hit.getIndex());
                resultJson.put("_type", hit.getType());
                resultJson.put("_id", hit.getId());
                resultJson.put("_source", hit.getSource());

                builder.append(resultJson.toJSONString());
                builder.append(System.lineSeparator());

                resultJson.clear();
            }
            snapshotFile.writeODataPJson(typeName, builder.toString());

            progressInfo.addDelta(response.getHits().getCount());
            progressInfo.writeToCache();

            // If the search result is smaller than LIMIT, the processing is terminated
            if (SEARCH_LIMIT > response.getHits().getCount()) {
                break;
            }
            // If the search result is LIMIT, search again from the following
            queryFrom += SEARCH_LIMIT;
            query.put("from", queryFrom);
        }
    }

    /**
     * Extract data from WebDAV and add it to the zip file.
     * Encrypted data is decrypted.
     * @param snapshotFile snapshot file
     */
    private void addWebDAVToZip(SnapshotFile snapshotFile) {
        Path webdavRootPathInZip = snapshotFile.getWebDAVDirPath();
        Path webdavRootPath = Paths.get(PersoniumUnitConfig.getBlobStoreRoot(),
                targetCell.getDataBundleName(), targetCell.getId());
        // Use FileVisitor to process files recursively
        FileVisitor<Path> visitor = new SnapshotFileExportVisitor(targetCell.getId(),
                webdavRootPath, webdavRootPathInZip, progressInfo);
        try {
            Files.walkFileTree(webdavRootPath, visitor);
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("add webdav data to snapshot file").reason(e);
        }
    }

    /**
     * Create new metadata file.
     */
    private void createMetaFile() {
        Path metadataFilePath = snapshotFilePath.getParent().resolve(DavMetadataFile.DAV_META_FILE_NAME);
        DavMetadataFile metadataFile = DavMetadataFile.prepareNewFile(metadataFilePath.toFile(), DavCmp.TYPE_DAV_FILE);
        metadataFile.setContentType("application/zip");
        metadataFile.setContentLength(snapshotFilePath.toFile().length());
        metadataFile.setEncryptionType(DataCryptor.ENCRYPTION_TYPE_NONE);
        metadataFile.save();
    }

    /**
     * Delete snapshot file.
     */
    private void deleteSnapshotFile() {
        try {
            FileUtils.deleteDirectory(snapshotFilePath.getParent());
        } catch (IOException e) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("delete snapshot file").reason(e);
        }
    }

    /**
     * Make error file.
     * @param e error
     */
    @SuppressWarnings("unchecked")
    private void makeErrorFile(Throwable e) {
        String snapshotFileName = snapshotFilePath.getParent().getFileName().toString();
        String errorFileName = FilenameUtils.getBaseName(snapshotFileName) + ERROR_FILE_EXTENSION;
        Path errorDirPath = Paths.get(PersoniumUnitConfig.getCellSnapshotRoot(), targetCell.getId(), errorFileName);
        try {
            Files.createDirectory(errorDirPath);
        } catch (IOException e1) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create error file").reason(e);
        }
        Path errorFilePath = errorDirPath.resolve(DavCmpFsImpl.CONTENT_FILE_NAME);

        String code;
        if (e instanceof PersoniumCoreException) {
            code = ((PersoniumCoreException) e).getCode();
        } else {
            code = PersoniumCoreException.Server.UNKNOWN_ERROR.getCode();
        }
        JSONObject messageJson = new JSONObject();
        JSONObject messageDetailJson = new JSONObject();
        messageJson.put("code", code);
        messageJson.put("message", messageDetailJson);
        messageDetailJson.put("lang", "en");
        messageDetailJson.put("value", e.getMessage());

        try {
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                Files.write(errorFilePath, messageJson.toJSONString().getBytes(Charsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } else {
                Files.write(errorFilePath, messageJson.toJSONString().getBytes(Charsets.UTF_8));
            }
        } catch (IOException e1) {
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create error file").reason(e1);
        }

        Path metadataFilePath = errorFilePath.getParent().resolve(DavMetadataFile.DAV_META_FILE_NAME);
        DavMetadataFile metadataFile = DavMetadataFile.prepareNewFile(metadataFilePath.toFile(), DavCmp.TYPE_DAV_FILE);
        metadataFile.setContentType("application/json");
        metadataFile.setContentLength(errorFilePath.toFile().length());
        metadataFile.setEncryptionType(DataCryptor.ENCRYPTION_TYPE_NONE);
        metadataFile.save();
    }
}

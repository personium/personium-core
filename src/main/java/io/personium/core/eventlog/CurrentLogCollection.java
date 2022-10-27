/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.eventlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Cell;

/**
 * Class for collection that stores current log files.
 */
public class CurrentLogCollection implements LogCollection {

    static Logger log = LoggerFactory.getLogger(CurrentLogCollection.class);

    static final String CURRENT_COLLECTION = "current";
    static final String DEFAULT_LOG = "default.log";

    private long created;
    private long updated;
    private String url;
    private String directoryPath;

    /**
     * constructor.
     * @param cell Cell object to which the collection belongs
     * @param collectionUrl collection URL
     */
    public CurrentLogCollection(Cell cell, String collectionUrl) {
        this.created = cell.getPublished();
        this.updated = cell.getPublished();
        this.url = collectionUrl;

        StringBuilder dirName = EventUtils.getEventLogDir(cell.getId(), cell.getOwnerNormalized())
                .append(File.separator)
                .append(CURRENT_COLLECTION);
        this.directoryPath = dirName.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreated() {
        return created;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUpdated() {
        return updated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl() {
        return url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends LogFile> getFileList() {
        LogFile logFile = getLogFile(DEFAULT_LOG);
        if (logFile == null) {
            return Collections.emptyList();
        }

        if (this.updated < logFile.getUpdated()) {
            this.updated = logFile.getUpdated();
        }
        return Collections.singletonList(logFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogFile getLogFile(String filename) {
        //If the file name is other than default.log, return 404
        if (!isValidLogFile(filename)) {
            return null;
        }
        File logFile = new File(getLogFilePath(filename));
        if (!logFile.isFile() || !logFile.canRead()) {
            return null;
        }
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
            long fileCreated = attr.creationTime().toMillis();
            long fileUpdated = logFile.lastModified();
            long fileSize = attr.size();
            String fileUrl = this.url + "/" + filename;
            return new LogFile(fileCreated, fileUpdated, fileSize, fileUrl);
        } catch (IOException e) {
            throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
        }
    }

    protected String getLogFilePath(String filename) {
        return new StringBuilder(directoryPath).append(File.separator).append(filename).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidLogFile(String fileName) {
        return DEFAULT_LOG.equals(fileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLogData(String filename, OutputStream output) {
        String filePath = getLogFilePath(filename);
        File logFile = new File(filePath);
        if (!logFile.isFile() || !logFile.canRead()) {
            // Even if the log can not be read for some reason, write nothing.
            return;
        }

        try (FileInputStream fis = new FileInputStream(logFile)) {
            IOUtils.copy(fis, output);
        } catch (FileNotFoundException e) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(filename);
        } catch (IOException e) {
            log.info("Failed to read archive entry : " + e.getMessage());
            throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteLogData(String filename) {
        throw PersoniumCoreException.Event.CURRENT_FILE_CANNOT_DELETE;
    }
}

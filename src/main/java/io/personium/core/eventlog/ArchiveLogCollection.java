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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.file.FileDataAccessException;
import io.personium.common.file.FileDataAccessor;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;

/**
 * Class for collection that stores archived log files. Archived log collection contains log files compressed as zip
 * file.
 */
public class ArchiveLogCollection implements LogCollection {

    static Logger log = LoggerFactory.getLogger(ArchiveLogCollection.class);

    static final String DEFAULT_LOG = "default.log";

    private long created;
    private long updated;
    private String url;
    private String directoryPath;
    private List<LogFile> archivefileList = new ArrayList<LogFile>();

    /**
     * constructor.
     * @param cell Cell object to which the collection belongs
     * @param collectionUrl collection URL
     */
    public ArchiveLogCollection(Cell cell, String collectionUrl) {
        // The creation date and update date of the archive collection is the creation date of the cell
        // However, update date is set the latest date of the file in collection by "createFileInformation"
        this.created = cell.getPublished();
        this.updated = cell.getPublished();
        this.url = collectionUrl;

        StringBuilder archiveDirName = EventUtils.getEventLogDir(cell.getId(), cell.getOwnerNormalized())
                .append("archive");
        this.directoryPath = archiveDirName.toString();
    }

    /**
     * Get file information under collection.
     */
    public void createFileInformation() {
        File archiveDir = new File(this.directoryPath);
        // If it is not rotated, there is no archive directory, so do not acquire file information
        if (!archiveDir.exists()) {
            return;
        }
        File[] fileList = archiveDir.listFiles();
        for (File file : fileList) {
            LogFile archiveFile = getLogFileStat(file);
            if (archiveFile == null) {
                continue;
            }

            // The update date of the archive log collection shall be the latest update date of the archive file
            if (this.updated < archiveFile.getUpdated()) {
                this.updated = archiveFile.getUpdated();
            }

            this.archivefileList.add(archiveFile);
            log.info(String.format("filename:%s created:%d updated:%d size:%d", file.getName(),
                    archiveFile.getCreated(), archiveFile.getUpdated(), archiveFile.getSize()));
        }
    }

    private LogFile getLogFileStat(File file) {
        if (file == null || !file.isFile() || !file.canRead()) {
            return null;
        }
        // Get update date of file
        long fileUpdated = file.lastModified();

        BasicFileAttributes attr = null;
        long fileCreated = 0L;
        long size = 0L;

        // Obrain size after decompressing the file
        try (ZipFile zipFile = new ZipFile(file)) {
            // Get creation date of file
            attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            fileCreated = attr.creationTime().toMillis();

            Enumeration<? extends ZipEntry> emu = zipFile.entries();
            while (emu.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) emu.nextElement();
                if (null == entry) {
                    log.info("Zip file entry is null.");
                    throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
                }
                size += entry.getSize();
            }
        } catch (ZipException e) {
            log.info("ZipException", e);
            throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
        } catch (IOException e) {
            log.info("IOException", e);
            throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
        }

        // the file name without extension (. Zip)
        String fileName = file.getName();
        String fileNameWithoutZip = fileName.substring(0, fileName.length() - ".zip".length());
        String fileUrl = this.url + "/" + fileNameWithoutZip;
        LogFile archiveFile = new LogFile(fileCreated, fileUpdated, size, fileUrl);
        return archiveFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogFile getLogFile(String filename) {
        File zippedLogFile = new File(getLogArchiveFilename(filename));
        return getLogFileStat(zippedLogFile);
    }

    protected String getLogArchiveFilename(String filename) {
        StringBuilder logFileName = new StringBuilder(this.directoryPath).append(File.separator).append(filename)
                .append(".zip");

        return logFileName.toString();
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
        createFileInformation();
        return archivefileList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidLogFile(String filename) {
        // Collection name exceptions excluded
        return filename != null && filename.startsWith(DEFAULT_LOG + ".");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLogData(String filename, OutputStream output) {
        String archiveLogFileName = this.getLogArchiveFilename(filename);

        try (FileInputStream fis = new FileInputStream(archiveLogFileName);
                ZipArchiveInputStream zais = new ZipArchiveInputStream(fis)) {
            // Retrieve the entry in the file
            // It is assumed that only one file is stored in the compression log file
            ZipArchiveEntry zae = zais.getNextZipEntry();
            if (zae == null) {
                throw PersoniumCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
            }
            IOUtils.copy(zais, output);
        } catch (FileNotFoundException e) {
            // If compressed file does not exist, return 404 error
            String[] split = archiveLogFileName.split(File.separator);
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(split[split.length - 1]);
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
        String archiveLogFileName = this.getLogArchiveFilename(filename);

        // File existence check.
        File logFile = new File(archiveLogFileName);
        if (!logFile.isFile()) {
            String[] split = archiveLogFileName.split(File.separator);
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(split[split.length - 1]);
        }

        // File delete.
        try {
            FileDataAccessor accessor = new FileDataAccessor("", null,
                    PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());
            accessor.deleteWithFullPath(archiveLogFileName);
        } catch (FileDataAccessException e) {
            log.info("Failed delete eventLog : " + e.getMessage());
            throw PersoniumCoreException.Event.FILE_DELETE_FAILED;
        }
    }
}

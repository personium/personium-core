/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.core.event.EventUtils;
import io.personium.core.model.Cell;

/**
 * Class for collection that stores archived log files.
 */
public class ArchiveLogCollection {

    static Logger log = LoggerFactory.getLogger(ArchiveLogCollection.class);

    private long created;
    private long updated;
    private String url;
    private String directoryPath;
    private List<ArchiveLogFile> archivefileList = new ArrayList<ArchiveLogFile>();;

    /**
     * constructor.
     * @param cell Cell object to which the collection belongs
     * @param uriInfo collection URL information
     */
    public ArchiveLogCollection(Cell cell, UriInfo uriInfo) {
        //The creation date and update date of the archive collection is the creation date of the cell
        //However, when the archive log file is created for the update date, the latest date of the file is set with "createFileInformation"
        this.created = cell.getPublished();
        this.updated = cell.getPublished();

        //Generate URL of archive collection
        StringBuilder urlSb = new StringBuilder();
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.scheme(PersoniumUnitConfig.getUnitScheme());
        urlSb.append(uriBuilder.build().toASCIIString());
        urlSb.append(uriInfo.getPath());
        this.url = urlSb.toString();

        StringBuilder archiveDirName = EventUtils.getEventLogDir(cell.getId(), cell.getOwner()).append("archive");
        this.directoryPath = archiveDirName.toString();
    }

    /**
     * Get file information under collection.
     */
    public void createFileInformation() {
        File archiveDir = new File(this.directoryPath);
        //If it is not rotated, there is no archive directory, so do not acquire file information
        if (!archiveDir.exists()) {
            return;
        }
        File[] fileList = archiveDir.listFiles();
        for (File file : fileList) {
            //Get update date of file
            long fileUpdated = file.lastModified();

            //The update date of the archive log collection shall be the latest update date of the archive file
            if (this.updated < fileUpdated) {
                this.updated = fileUpdated;
            }

            BasicFileAttributes attr = null;
            ZipFile zipFile = null;
            long fileCreated = 0L;
            long size = 0L;
            try {
                //Get creation date of file
                attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                fileCreated = attr.creationTime().toMillis();

                //Currently, since the past log acquisition API can only acquire the state after decompression, obtain the size after decompressing the file
                zipFile = new ZipFile(file);
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
            } finally {
                IOUtils.closeQuietly(zipFile);
            }

            //Here as well, since the past log acquisition API can only acquire the state after decompression, it acquires the file name without extension (. Zip)
            String fileName = file.getName();
            String fileNameWithoutZip = fileName.substring(0, fileName.length() - ".zip".length());
            String fileUrl = this.url + "/" + fileNameWithoutZip;
            ArchiveLogFile archiveFile = new ArchiveLogFile(fileCreated, fileUpdated, size, fileUrl);

            this.archivefileList.add(archiveFile);
            log.info(String.format("filename:%s created:%d updated:%d size:%d", file.getName(), fileCreated,
                    fileUpdated, size));
        }
    }

    /**
     * Return creation date and time.
     * @return Created date and time
     */
    public long getCreated() {
        return created;
    }

    /**
     * Refresh date and time is returned.
     * @return Update date and time
     */
    public long getUpdated() {
        return updated;
    }

    /**
     * Return URL.
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Return the path to the directory.
     * @return URL
     */
    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Return files under the collection.
     * @return Archivefile list
     */
    public List<ArchiveLogFile> getArchivefileList() {
        return archivefileList;
    }
}

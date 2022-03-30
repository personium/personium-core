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
import java.util.ArrayList;
import java.util.List;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.file.FileDataAccessException;
import io.personium.common.file.FileDataAccessor;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.cell.LogResource;

/**
 * Utility class for events.
 */
public class EventUtils {

    /** Mime-Type in CSV format.*/
    public static final String TEXT_CSV = "text/csv";

    private static final int SUBDIR_NAME_LEN = 2;

    private EventUtils() {
    }

    /**
     * Get the directory path of the log file to be output on the event bus.
     * @param cellId uuid of Cell
     * @param owner owner information
     * @return Directory of the log file (return null if it does not exist)
     */
    public static StringBuilder getEventLogDir(String cellId, String owner) {

        String unitUserName = "anon";
        if (owner != null) {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }

        StringBuilder logDir = new StringBuilder();
        logDir.append(PersoniumUnitConfig.getEventLogCurrentDir());
        logDir.append(File.separator);
        logDir.append(unitUserName);
        logDir.append(File.separator);
        logDir.append(getSubDirectoryName(cellId));
        logDir.append(cellId);
        logDir.append(File.separator);

        return logDir;
    }

    private static String getSubDirectoryName(String filename) {
        StringBuilder sb = new StringBuilder("");
        sb.append(splitDirectoryName(filename, 0));
        sb.append(File.separator);
        sb.append(splitDirectoryName(filename, SUBDIR_NAME_LEN));
        sb.append(File.separator);
        return sb.toString();
    }

    private static String splitDirectoryName(String filename, int index) {
        return filename.substring(index, index + SUBDIR_NAME_LEN);
    }

    /**
     * Delete all event logs.
     * @param cellId ID of the cell
     * @param owner Cell owner (URL format)
     * @throws BinaryDataAccessException Failed to delete event log file
     */
    public static void deleteEventLog(String cellId, String owner) throws FileDataAccessException {
        //Retrieve log list
        List<String> logFiles = getLogFileList(cellId, owner);
        FileDataAccessor accessor = new FileDataAccessor("", null,
                PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());

        //File logical deletion
        for (String logFile : logFiles) {
            accessor.deleteWithFullPath(logFile);
        }

        //Delete metadata of TODO archive

    }

    private static List<String> getLogFileList(String cellId, String owner) {
        String logDir = EventUtils.getEventLogDir(cellId, owner).toString();
        List<String> logFiles = new ArrayList<String>();

        //Acquire current log list
        StringBuilder sb = new StringBuilder(logDir);
        sb.append(LogResource.CURRENT_COLLECTION);
        sb.append(File.separator);
        File currentLogDir = new File(sb.toString());
        if (currentLogDir.exists()) {
            String[] currentLogFiles = currentLogDir.list();
            for (String currentLogFile : currentLogFiles) {
                logFiles.add(sb.toString() + currentLogFile);
            }
        }

        //Acquire archive log list
        sb = new StringBuilder(logDir);
        sb.append(LogResource.ARCHIVE_COLLECTION);
        sb.append(File.separator);
        File archiveLogDir = new File(sb.toString());
        if (archiveLogDir.exists()) {
            String[] archiveLogFiles = archiveLogDir.list();
            for (String archiveLogFile : archiveLogFiles) {
                logFiles.add(sb.toString() + archiveLogFile);
            }
        }

        return logFiles;
    }

}

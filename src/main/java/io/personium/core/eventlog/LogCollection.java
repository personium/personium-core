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

import java.io.OutputStream;
import java.util.List;

/**
 * Interface for log collection.
 */
public interface LogCollection {

    /**
     * Return creation date and time.
     * @return Created date and time
     */
    long getCreated();

    /**
     * Return updated date and time.
     * @return Updated date and time
     */
    long getUpdated();

    /**
     * Return URL.
     * @return URL
     */
    String getUrl();

    /**
     * Return files under the collection.
     * @return Archivefile list
     */
    List<? extends LogFile> getFileList();

    /**
     * Return information about specified log.
     * @param filename log filename
     * @return LogFile object
     */
    LogFile getLogFile(String filename);

    /**
     * File name check of event log.
     * <ul>
     * <li> current: "default.log" fixed
     * <li> archive: File name starting with "default.log." (404 if there is no actual file, but here only the file name check)
     * </ul>
     * @param filename File name ("default.log" or "default.log. *")
     * @return true: correct, false: error
     */
    boolean isValidLogFile(String filename);

    /**
     * Write log data to specified OutputStream.
     * @param filename Log file name
     * @param output OutputStream to be written log data
     */
    void writeLogData(String filename, OutputStream output);

    /**
     * Delete log data.
     * @param filename Log file name
     */
    void deleteLogData(String filename);
}

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


/**
 *Class for archive log files.
 */
public class ArchiveLogFile {

    private long created;
    private long updated;
    private long size;
    private String url;

    /**
     * constructor.
     *@ param created Created date and time
     *@ param updated Modified date and time
     *@ param size size
     * @param url URL
     */
    public ArchiveLogFile(long created, long updated, long size, String url) {
        this.created = created;
        this.updated = updated;
        this.url = url;
        this.size = size;
    }

    /**
     *Return creation date and time.
     *@return Created date and time
     */
    public long getCreated() {
        return created;
    }

    /**
     *Refresh date and time is returned.
     *@return Update date and time
     */
    public long getUpdated() {
        return updated;
    }

    /**
     *Return URL.
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     *Return file size.
     *@return file size
     */
    public long getSize() {
        return size;
    }

}

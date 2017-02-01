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
 * Archiveされたログファイル用のクラス.
 */
public class ArchiveLogFile {

    private long created;
    private long updated;
    private long size;
    private String url;

    /**
     * constructor.
     * @param created 作成日時
     * @param updated 更新日時
     * @param size サイズ
     * @param url URL
     */
    public ArchiveLogFile(long created, long updated, long size, String url) {
        this.created = created;
        this.updated = updated;
        this.url = url;
        this.size = size;
    }

    /**
     * 作成日時を返却.
     * @return 作成日時
     */
    public long getCreated() {
        return created;
    }

    /**
     * 更新日時を返却.
     * @return 更新日時
     */
    public long getUpdated() {
        return updated;
    }

    /**
     * URLを返却.
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * ファイルサイズを返却.
     * @return ファイルサイズ
     */
    public long getSize() {
        return size;
    }

}

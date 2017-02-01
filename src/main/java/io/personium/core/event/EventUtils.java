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
package io.personium.core.event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.file.BinaryDataAccessException;
import io.personium.core.model.file.BinaryDataAccessor;
import io.personium.core.rs.cell.LogResource;

/**
 * イベント用ユーティリティクラス.
 */
public class EventUtils {

    /** CSV形式のMime-Type. */
    public static final String TEXT_CSV = "text/csv";

    private static final int SUBDIR_NAME_LEN = 2;

    private EventUtils() {
    }

    /**
     * イベントバスで出力するログファイルのディレクトリパスを取得する.
     * @param cellId Cellのuuid
     * @param owner オーナー情報
     * @return ログファイルのディレクトリ（存在しない場合はnullを返却）
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
     * すべてのイベントログを削除する.
     * @param cellId セルのID
     * @param owner セルのオーナー（URL形式）
     * @throws BinaryDataAccessException イベントログファイルの削除に失敗
     */
    public static void deleteEventLog(String cellId, String owner) throws BinaryDataAccessException {
        // ログの一覧を取得
        List<String> logFiles = getLogFileList(cellId, owner);
        BinaryDataAccessor accessor = new BinaryDataAccessor("", null,
                PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());

        // ファイル論理削除
        for (String logFile : logFiles) {
            accessor.deleteWithFullPath(logFile);
        }

        // TODO archiveのメタデータ削除

    }

    private static List<String> getLogFileList(String cellId, String owner) {
        String logDir = EventUtils.getEventLogDir(cellId, owner).toString();
        List<String> logFiles = new ArrayList<String>();

        // currentのログ一覧取得
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

        // archiveのログ一覧取得
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

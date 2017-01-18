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
package com.fujitsu.dc.core.eventlog;

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

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.event.EventUtils;
import com.fujitsu.dc.core.model.Cell;

/**
 * Archiveされたログファイルを格納するコレクション用のクラス.
 */
public class ArchiveLogCollection {

    static Logger log = LoggerFactory.getLogger(ArchiveLogCollection.class);

    private long created;
    private long updated;
    private String url;
    private String directoryPath;
    private List<ArchiveLogFile> archivefileList = new ArrayList<ArchiveLogFile>();;

    /**
     * コンストラクタ.
     * @param cell コレクションが属するCellオブジェクト
     * @param uriInfo コレクションのURL情報
     */
    public ArchiveLogCollection(Cell cell, UriInfo uriInfo) {
        // archiveコレクションの作成日と更新日はセルの作成日とする
        // ただし更新日についてはアーカイブログファイルが作成されている場合、そのファイルの最新日が「createFileInformation」で設定される
        this.created = cell.getPublished();
        this.updated = cell.getPublished();

        // archiveコレクションのURLを生成
        StringBuilder urlSb = new StringBuilder();
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.scheme(DcCoreConfig.getUnitScheme());
        urlSb.append(uriBuilder.build().toASCIIString());
        urlSb.append(uriInfo.getPath());
        this.url = urlSb.toString();

        StringBuilder archiveDirName = EventUtils.getEventLogDir(cell.getId(), cell.getOwner()).append("archive");
        this.directoryPath = archiveDirName.toString();
    }

    /**
     * コレクション配下のファイル情報を取得する.
     */
    public void createFileInformation() {
        File archiveDir = new File(this.directoryPath);
        // ローテートされていない場合はアーカイブディレクトリが存在しないため、ファイル情報は取得しない
        if (!archiveDir.exists()) {
            return;
        }
        File[] fileList = archiveDir.listFiles();
        for (File file : fileList) {
            // ファイルの更新日を取得
            long fileUpdated = file.lastModified();

            // アーカイブログコレクションの更新日はアーカイブファイルの最新の更新日とする
            if (this.updated < fileUpdated) {
                this.updated = fileUpdated;
            }

            BasicFileAttributes attr = null;
            ZipFile zipFile = null;
            long fileCreated = 0L;
            long size = 0L;
            try {
                // ファイルの作成日を取得
                attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                fileCreated = attr.creationTime().toMillis();

                // 現状、過去ログ取得APIでは解凍後の状態しか取得できないためファイルの解凍後のサイズを取得する
                zipFile = new ZipFile(file);
                Enumeration<? extends ZipEntry> emu = zipFile.entries();
                while (emu.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) emu.nextElement();
                    if (null == entry) {
                        log.info("Zip file entry is null.");
                        throw DcCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
                    }
                    size += entry.getSize();
                }
            } catch (ZipException e) {
                log.info("ZipException", e);
                throw DcCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
            } catch (IOException e) {
                log.info("IOException", e);
                throw DcCoreException.Event.ARCHIVE_FILE_CANNOT_OPEN;
            } finally {
                IOUtils.closeQuietly(zipFile);
            }

            // こちらも現状、過去ログ取得APIでは解凍後の状態しか取得できないため拡張子(.zip)を外したファイル名を取得する
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
     * ディレクトリへのパスを返却.
     * @return URL
     */
    public String getDirectoryPath() {
        return directoryPath;
    }

    /**
     * コレクション配下のファイルを返却.
     * @return Archivefileのリスト
     */
    public List<ArchiveLogFile> getArchivefileList() {
        return archivefileList;
    }
}

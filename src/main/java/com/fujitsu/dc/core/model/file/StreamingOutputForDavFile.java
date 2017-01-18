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
package com.fujitsu.dc.core.model.file;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.DcCoreConfig;

/**
 * Davファイルの内容を Responseに返却する際に利用する StreamingOutputクラス. 内部的には、読み込み専用にハードリンクを作成し、出力完了後に削除する。
 */
public class StreamingOutputForDavFile implements StreamingOutput {

    private static Logger logger = LoggerFactory.getLogger(StreamingOutputForDavFile.class);

    /**
     * Davファイルの読み書き時、ハードリンク作成/ファイル名改変時の最大リトライ回数.
     */
    private static int maxRetryCount = DcCoreConfig.getDavFileOperationRetryCount();

    /**
     * Davファイルの読み書き時、ハードリンク作成/ファイル名改変時のリトライ間隔(msec).
     */
    private static long retryInterval = DcCoreConfig.getDavFileOperationRetryInterval();

    /**
     * 読み込み用のハードリンクのパス.
     */
    Path hardLinkPath = null;

    /**
     * 読み込み用のハードリンクからの入力ストリーム.
     */
    InputStream hardLinkInput = null;

    /**
     * コンストラクタ.
     * @param fileFullPath 読み込むファイルのフルパス
     * @throws BinaryDataNotFoundException ファイルが存在しない場合.
     */
    public StreamingOutputForDavFile(String fileFullPath) throws BinaryDataNotFoundException {
        if (!Files.exists(Paths.get(fileFullPath))) {
            throw new BinaryDataNotFoundException(fileFullPath);
        }

        // 読み込み専用のハードリンクを作成するため、ユニーク名を生成。
        String hardLinkName = UniqueNameComposer.compose(fileFullPath);

        for (int i = 0; i < maxRetryCount; i++) {
            try {
                synchronized (fileFullPath) {
                    // ハードリンクを作成.
                    hardLinkPath = Files.createLink(Paths.get(hardLinkName), Paths.get(fileFullPath));
                }
                // ハードリンクからの入力ストリームを取得
                hardLinkInput = new BufferedInputStream(new FileInputStream(hardLinkPath.toFile()));
                // 成功したら終了
                return;
            } catch (IOException e) {
                // 指定回数まではリトライする。
                logger.debug(String.format("Creating hard link %s failed. Will try again.", hardLinkName));
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e1) {
                    logger.debug("Thread interrupted.");
                }
            }
        }

        throw new BinaryDataNotFoundException("Unable to create hard link for DAV file: " + hardLinkName);
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        if (null == hardLinkInput) {
            throw new WebApplicationException(new BinaryDataNotFoundException(hardLinkPath.toString()));
        }
        try {
            IOUtils.copy(hardLinkInput, output);
        } finally {
            IOUtils.closeQuietly(hardLinkInput);
            // 後始末。自分用の読み込みハードリンクを削除する。
            Files.delete(hardLinkPath);
        }
    }

}

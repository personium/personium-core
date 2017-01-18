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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.http.header.ByteRangeSpec;
import com.fujitsu.dc.core.http.header.RangeHeaderHandler;

/**
 * Davファイルの内容を Responseに返却する際に利用する StreamingOutputクラス. 内部的には、読み込み専用にハードリンクを作成し、出力完了後に削除する。
 */
public class StreamingOutputForDavFileWithRange extends StreamingOutputForDavFile {

    private RangeHeaderHandler range = null;
    private long fileSize = 0;

    /**
     * コンストラクタ.
     * @param fileFullPath 読み込むファイルのフルパス
     * @param fileSize 読み込むファイルのサイズ
     * @param range RangeHeader
     * @throws BinaryDataNotFoundException ファイルが存在しない場合.
     */
    public StreamingOutputForDavFileWithRange(final String fileFullPath,
            final long fileSize,
            final RangeHeaderHandler range) throws BinaryDataNotFoundException {
        super(fileFullPath);
        this.range = range;
        this.fileSize = fileSize;
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        try {
            // MultiPartには対応しないため1個目のbyte-renge-setだけ処理する。
            int rangeIndex = 0;
            List<ByteRangeSpec> brss = range.getByteRangeSpecList();
            final ByteRangeSpec brs = brss.get(rangeIndex);

            int chr;
            long first = brs.getFirstBytePos();
            long last = brs.getLastBytePos();
            // Rangeの先頭まで読み飛ばし
            if (hardLinkInput.skip(first) != first) {
                DcCoreLog.Dav.FILE_TOO_SHORT
                        .params("skip failed", fileSize, range.getRangeHeaderField()).writeLog();
                throw DcCoreException.Dav.FS_INCONSISTENCY_FOUND;
            }
            // Rangeの終端まで返却
            for (long pos = first; pos < last + 1; pos++) {
                chr = hardLinkInput.read();
                if (chr == -1) {
                    DcCoreLog.Dav.FILE_TOO_SHORT
                            .params("too short.size", fileSize, range.getRangeHeaderField()).writeLog();
                    throw DcCoreException.Dav.FS_INCONSISTENCY_FOUND;
                }
                output.write((char) chr);
            }
        } finally {
            IOUtils.closeQuietly(hardLinkInput);
            Files.delete(hardLinkPath);
        }
    }

}

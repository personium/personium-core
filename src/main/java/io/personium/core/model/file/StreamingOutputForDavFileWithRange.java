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
package io.personium.core.model.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.http.header.ByteRangeSpec;
import io.personium.core.http.header.RangeHeaderHandler;

/**
 *StreamingOutput class to use when returning the contents of the Dav file to Response Internally create a hard link for reading only and delete it after completion of output.
 */
public class StreamingOutputForDavFileWithRange extends StreamingOutputForDavFile {

    private RangeHeaderHandler range = null;
    private long fileSize = 0;

    /**
     *constructor.
     *@ param fileFullPath Full path of the file to read
     *@ param fileSize Size of the file to read
     * @param range RangeHeader
     * @param cellId Cell ID
     * @param encryptionType encryption type
     *@throws BinaryDataNotFoundException if the file does not exist.
     */
    public StreamingOutputForDavFileWithRange(final String fileFullPath,
            final long fileSize,
            final RangeHeaderHandler range,
            String cellId,
            String encryptionType) throws BinaryDataNotFoundException {
        super(fileFullPath, cellId, encryptionType);
        this.range = range;
        this.fileSize = fileSize;
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        try {
            //Because it does not correspond to MultiPart, it processes only the first byte-renge-set.
            int rangeIndex = 0;
            List<ByteRangeSpec> brss = range.getByteRangeSpecList();
            final ByteRangeSpec brs = brss.get(rangeIndex);

            int chr;
            long first = brs.getFirstBytePos();
            long last = brs.getLastBytePos();
            //Skip to the beginning of Range
            if (hardLinkInput.skip(first) != first) {
                PersoniumCoreLog.Dav.FILE_TOO_SHORT
                        .params("skip failed", fileSize, range.getRangeHeaderField()).writeLog();
                throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND;
            }
            //Return to the end of Range
            for (long pos = first; pos < last + 1; pos++) {
                chr = hardLinkInput.read();
                if (chr == -1) {
                    PersoniumCoreLog.Dav.FILE_TOO_SHORT
                            .params("too short.size", fileSize, range.getRangeHeaderField()).writeLog();
                    throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND;
                }
                output.write((char) chr);
            }
        } finally {
            IOUtils.closeQuietly(hardLinkInput);
            Files.delete(hardLinkPath);
        }
    }

}

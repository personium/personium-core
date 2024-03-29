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
package io.personium.core.model.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.io.IOUtils;

import io.personium.common.file.FileDataNotFoundException;
import io.personium.core.ElapsedTimeLog;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.http.header.ByteRangeSpec;
import io.personium.core.http.header.RangeHeaderHandler;

/**
 * StreamingOutput class to use when returning the contents of the Dav file to Response Internally create a hard link for reading only and delete it after completion of output.
 */
public class StreamingOutputForDavFileWithRange extends StreamingOutputForDavFile {

    private RangeHeaderHandler range = null;
    private long fileSize = 0;

    private static final int KILO_BYTES = 1000;

    /**
     * constructor.
     * @param fileFullPath Full path of the file to read
     * @param fileSize Size of the file to read
     * @param range RangeHeader
     * @param cellId Cell ID
     * @param encryptionType encryption type
     * @throws FileDataNotFoundException if the file does not exist.
     */
    public StreamingOutputForDavFileWithRange(final String fileFullPath,
            final long fileSize,
            final RangeHeaderHandler range,
            String cellId,
            String encryptionType) throws FileDataNotFoundException {
        super(fileFullPath, cellId, encryptionType);
        this.range = range;
        this.fileSize = fileSize;
    }

    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        // write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params("-").writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();

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

            // write end log
            endLog.setParams(last / KILO_BYTES);
            endLog.writeLog();
        } finally {
            IOUtils.closeQuietly(hardLinkInput);
            Files.delete(hardLinkPath);
        }

    }

}

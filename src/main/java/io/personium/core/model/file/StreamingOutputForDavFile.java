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

import io.personium.core.PersoniumCoreLog;
import io.personium.core.ElapsedTimeLog;
import io.personium.core.PersoniumUnitConfig;

/**
 * StreamingOutput class to use when returning the contents of the Dav file to Response Internally create a hard link for reading only and delete it after completion of output.
 */
public class StreamingOutputForDavFile implements StreamingOutput {

    private static Logger logger = LoggerFactory.getLogger(StreamingOutputForDavFile.class);
    private static final int KILO_BYTES = 1000;

    /**
     * Maximum number of retries at the time of reading / writing Dav file, hard link creation / file name modification.
     */
    private static int maxRetryCount = PersoniumUnitConfig.getDavFileOperationRetryCount();

    /**
     * Retry interval (msec) at the time of reading / writing Dav file, hard link creation / file name modification.
     */
    private static long retryInterval = PersoniumUnitConfig.getDavFileOperationRetryInterval();

    /**
     * The hard link path for loading.
     */
    Path hardLinkPath = null;

    /**
     * Input stream from the hard link for reading.
     */
    InputStream hardLinkInput = null;

    /**
     * Constructor.
     * @param fileFullPath Full path of the file to be read
     * @param cellId Cell ID
     * @param encryptionType encryption type
     * @throws BinaryDataNotFoundException Error when file does not exist.
     */
    public StreamingOutputForDavFile(String fileFullPath, String cellId, String encryptionType)
            throws BinaryDataNotFoundException {
        if (!Files.exists(Paths.get(fileFullPath))) {
            throw new BinaryDataNotFoundException(fileFullPath);
        }

        //Generate a unique name to create a read-only hard link.
        String hardLinkName = UniqueNameComposer.compose(fileFullPath);

        for (int i = 0; i < maxRetryCount; i++) {
            try {
                synchronized (fileFullPath) {
                    //Create a hard link.
                    hardLinkPath = Files.createLink(Paths.get(hardLinkName), Paths.get(fileFullPath));
                }
                //Get input stream from hard link
                InputStream inputStream;
                // Perform decryption.
                DataCryptor cryptor = new DataCryptor(cellId);
                inputStream = cryptor.decode(new FileInputStream(hardLinkPath.toFile()), encryptionType);
                hardLinkInput = new BufferedInputStream(inputStream);
                //End if successful
                return;
            } catch (IOException e) {
                //Retry until the specified number of times.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(OutputStream output) throws IOException, WebApplicationException {
        if (null == hardLinkInput) {
            throw new WebApplicationException(new BinaryDataNotFoundException(hardLinkPath.toString()));
        }
        // write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params("-").writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();

        int writtenBytes = 0;
        try {
            writtenBytes = IOUtils.copy(hardLinkInput, output);
        } finally {
            IOUtils.closeQuietly(hardLinkInput);
            //Cleanup. Delete the reading hard link for yourself.
            Files.delete(hardLinkPath);
        }
        // write end log
        endLog.setParams(writtenBytes / KILO_BYTES);
        endLog.writeLog();
    }

}

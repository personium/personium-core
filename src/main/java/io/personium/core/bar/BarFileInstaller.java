/**
 * Personium
 * Copyright 2014-2019 FUJITSU LIMITED
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
package io.personium.core.bar;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SyncFailedException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.utils.PersoniumThread;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.bar.jackson.JSONManifest;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.odata.PersoniumOptionsQueryParser;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.rs.odata.ODataResource;

/**
 * bar The class that performs the installation process.
 */
public class BarFileInstaller {
    /**
     * Object for logging.
     */
    static Logger log = LoggerFactory.getLogger(BarFileInstaller.class);

    static final long MB = 1024 * 1024;
    static final int BUF_SIZE = 1024; // for output response.

    private final Cell cell;
    private String boxName;
    private ODataEntityResource oDataEntityResource;

    private String barTempDir = PersoniumUnitConfig.getBarInstallTempDir();

    private JSONObject manifestJson;

    /**
     * constructor.
     * @param cell
     * Cell object
     * @param boxName
     * Box name
     * @param oDataEntityResource oDataEntityResource
     */
    public BarFileInstaller(
            final Cell cell,
            final String boxName,
            final ODataEntityResource oDataEntityResource) {
        this.cell = cell;
        this.boxName = boxName;
        this.oDataEntityResource = oDataEntityResource;
    }

    /**
     * bar Method to perform file installation.
     * @param headers
     * MAP storing Http header
     * @param inStream
     * InputStream for Http request body
     * @param requestKey The value of the RequestKey field to be output to the event log
     * @return response
     */
    public Response barFileInstall(Map<String, String> headers,
            InputStream inStream, String requestKey) {

        //Advance check
        checkPreConditions(headers);

        //Store bar file
        File file = storeTemporaryBarFile(inStream);
        IOUtils.closeQuietly(inStream);

        // bar_version : 2
        try {
            if (execVer2Process(file, requestKey)) {
                //Returning the response
                ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
                res.header(HttpHeaders.LOCATION, this.cell.getUrl() + boxName);
                return res.build();
            }
        } catch (Exception e) {
            removeBarFile(file);
            throw e;
        }

        BarFileReadRunner runner = null;
        try {
            //Bar file validation
            long entryCount = checkBarFileContents(file);

            //Duplicate check of Box and schema URL
            checkDuplicateBoxAndSchema((String) this.manifestJson.get("Schema"));

            //Create Box
            //Errors so far are 400 series errors, Box is not created, so it does not write to Box metadata (cache) and ends.
            runner = new BarFileReadRunner(file, this.cell, this.boxName,
                    this.oDataEntityResource, this.oDataEntityResource.getOdataProducer(),
                    Box.EDM_TYPE_NAME, requestKey);
            runner.createBox(this.manifestJson);

            //bar Set the number of entries in the file (create ProgressInfo at this point)
            runner.setEntryCount(entryCount);
            runner.writeInitProgressCache();

        } catch (PersoniumCoreException e) {
            if (null != runner) {
                runner.writeErrorProgressCache();
            }
            removeBarFile(file);
            throw e;
        } catch (Exception e) {
            if (null != runner) {
                runner.writeErrorProgressCache();
            }
            removeBarFile(file);
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }

        //Asynchronous execution
        PersoniumThread.BOX_IO.execute(runner);

        //Returning the response
        ResponseBuilder res = Response.status(HttpStatus.SC_ACCEPTED);
        res.header(HttpHeaders.LOCATION, this.cell.getUrl() + boxName);
        return res.build();
    }

    /**
     * Exec bar_version 2 process.
     * If bar file structure is different from version 2, do not process anything.
     * @param file bar file
     * @param requestKey personium event requestkey
     * @return false:bar file structure is different from version 2
     */
    private boolean execVer2Process(File file, String requestKey) {
        long entryCount;
        String schema;
        try (BarFile barFile = BarFile.newInstance(file.toPath())) {
            if (!barFile.exists(BarFile.MANIFEST_JSON)) {
                return false;
            }
            ObjectMapper mapper = new ObjectMapper();
            JSONManifest manifest = mapper.readValue(barFile.getReader(BarFile.MANIFEST_JSON), JSONManifest.class);
            if (StringUtils.isEmpty(manifest.getBarVersion())) {
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params("bar_version");
            }
            int barVersion;
            try {
                barVersion = Integer.parseInt(manifest.getBarVersion());
            } catch (NumberFormatException e) {
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params("bar_version");
            }
            if (barVersion != 2) {
                throw PersoniumCoreException.BarInstall.BAR_FILE_STRUCTURE_AND_VERSION_MISMATCH;
            }
            checkBarFileSize(file);
            barFile.checkStructure();
            //Duplicate check of Box and schema URL
            checkDuplicateBoxAndSchema(manifest.getSchema());
            // Use FileVisitor to check process recursively.
            BarFileCheckVisitor visitor = new BarFileCheckVisitor();
            try {
                Path path = barFile.getRootDirPath();
                Files.walkFileTree(path, visitor);
            } catch (IOException e) {
                removeBarFile(file);
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(e.getMessage());
            }

            entryCount = visitor.getEntryCount();
            schema = manifest.getSchema();
        } catch (IOException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        }
        BarFileInstallRunner runner = new BarFileInstallRunner(file.toPath(), entryCount,
                boxName, schema, oDataEntityResource, requestKey);
        PersoniumThread.BOX_IO.execute(runner);
        return true;
    }

    private void removeBarFile(File barFile) {
        if (barFile.exists() && !barFile.delete()) {
            log.warn("Failed to remove bar file. [" + barFile.getAbsolutePath() + "].");
        }
    }

    /**
     * bar Method to pre-check at the time of acceptance of installation.
     * @param headers HTTP header
     */
    private void checkPreConditions(Map<String, String> headers) {
        //[403] Access control
        ODataResource odataResource = this.oDataEntityResource.getOdataResource();
        odataResource.checkAccessContext(CellPrivilege.BOX_BAR_INSTALL);

        //[400] Request header format check
        checkHeaders(headers);
    }

    /**
     * Http header check.
     * @param headers
     * MAP storing Http header
     */
    private void checkHeaders(Map<String, String> headers) {
        //Content-Type: fixed application / zip
        String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
        if (!"application/zip".equals(contentType)) {
            throw PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR
                    .params(HttpHeaders.CONTENT_TYPE);
        }
    }

    /**
     * Get the maximum file size (MB) of the bar file set in the system property.
     * @return io.personium.core.bar.file.maxSize
     */
    protected long getMaxBarFileSize() {
        long maxBarFileSize;
        try {
            maxBarFileSize = Long.parseLong(PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_FILE_MAX_SIZE));
        } catch (NumberFormatException ne) {
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return maxBarFileSize;
    }

    /**
     * Get the maximum file size (MB) in the BAR file from the property file.
     * @return io.personium.core.bar.entry.maxSize
     */
    protected long getMaxBarEntryFileSize() {
        long maxBarFileSize;
        try {
            maxBarFileSize = Long.parseLong(PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_ENTRY_MAX_SIZE));
        } catch (NumberFormatException ne) {
            log.info("NumberFormatException" + PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_ENTRY_MAX_SIZE));
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return maxBarFileSize;
    }

    /**
     * Synchronization of file descriptors.
     * @param fd file descriptor
     * @throws SyncFailedException Synchronization failed
     */
    public void sync(FileDescriptor fd) throws SyncFailedException {
        fd.sync();
    }

    /**
     * Http Reads the bar file from the request body and stores it in the temporary area.
     * @param inStream InputStream object for Http request body
     * @return The File object of the bar file stored in the temporary area
     */
    private File storeTemporaryBarFile(InputStream inStream) {

        //If there is no directory to store the bar file, it creates it.
        String unitUserName = BarFileUtils.getUnitUserName(this.cell.getOwnerNormalized());
        File barFileDir = new File(new File(barTempDir, unitUserName), "bar");
        if (!barFileDir.exists() && !barFileDir.mkdirs()) {
            String message = "unable create directory: " + barFileDir.getAbsolutePath();
            throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(message);
        }

        //Store bar file on NFS.
        String prefix = this.cell.getId() + "_" + this.boxName;
        File barFile = null;
        OutputStream outStream = null;
        try {
            barFile = File.createTempFile(prefix, ".bar", barFileDir);
            barFile.deleteOnExit(); //Delete setting to be deleted when abnormal termination of VM
            outStream = new FileOutputStream(barFile);
            IOUtils.copyLarge(inStream, outStream);
        } catch (IOException e) {
            String message = "unable save bar file: %s";
            if (barFile == null) {
                message = String.format(message, barFileDir + prefix + "XXX.bar");
            } else {
                message = String.format(message, barFile.getAbsolutePath());
            }
            throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(message);
        } finally {
            if (null != outStream && PersoniumUnitConfig.getFsyncEnabled()) {
                try {
                    sync(((FileOutputStream) outStream).getFD());
                } catch (Exception e) {
                    IOUtils.closeQuietly(outStream);
                    throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(e.getMessage());
                }
            }
            IOUtils.closeQuietly(outStream);
        }
        return barFile;
    }

    /**
     * bar A method that reads a file and validates it.
     * <ul>
     * <li> Count the number of entries (files only) in the bar file. </ li>
     * <li> Check the upper limit of the file size of each entry in the bar file. </ li>
     * <li> Check the order of each entry in TODO bar file. </ li>
     * </ul>.
     * @param barFile The File object of the bar file saved in the temporary area
     * @returns bar Number of entries (files) in the file
     */
    private long checkBarFileContents(File barFile) {

        //bar File size check
        checkBarFileSize(barFile);

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(barFile, "UTF-8");
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            ZipArchiveEntry zae = null;
            long entryCount = 0;
            String entryName = null;
            try {
                long maxBarEntryFileSize = getMaxBarEntryFileSize();
                //Setup data for mandatory file check
                Map<String, String> requiredBarFiles = setupBarFileOrder();
                while (entries.hasMoreElements()) {
                    zae = entries.nextElement();
                    entryName = zae.getName();
                    log.info("read: " + entryName);
                    if (!zae.isDirectory()) {
                        //Count the number of files in the bar file as the parameter for calculating the installation progress ratio
                        entryCount++;

                        //Check file size of entry in bar file
                        checkBarFileEntrySize(zae, entryName, maxBarEntryFileSize);

                        //Read only the manifest file for generating Box.
                        if (zae.getName().endsWith("/" + BarFileReadRunner.MANIFEST_JSON)) {
                            checkAndReadManifest(entryName, zae, zipFile);
                        }
                    }
                    //Required file check of bar file (check the storage order at installation)
                    if (!checkBarFileStructures(zae, requiredBarFiles)) {
                        throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryName);
                    }
                }
                if (!requiredBarFiles.isEmpty()) {
                    StringBuilder entryNames = new StringBuilder();
                    Object[] requiredFileNames = requiredBarFiles.keySet().toArray();
                    for (int i = 0; i < requiredFileNames.length; i++) {
                        if (i > 0) {
                            entryNames.append(" " + requiredFileNames[i]);
                        } else {
                            entryNames.append(requiredFileNames[i]);
                        }
                    }
                    throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryNames.toString());
                }
                return entryCount;
            } catch (PersoniumCoreException e) {
                throw e;
            } catch (Exception e) {
                log.info(e.getMessage(), e.fillInStackTrace());
                throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(entryName);
            }
        } catch (FileNotFoundException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params("barFile");
        } catch (ZipException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        } catch (IOException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.params(e.getMessage());
        } catch (PersoniumCoreException e) {
            throw e;
        } catch (RuntimeException e) {
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        } finally {
            ZipFile.closeQuietly(zipFile);
        }
    }

    private void checkAndReadManifest(String entryName, ZipArchiveEntry zae, ZipFile zipFile) throws IOException {
        InputStream inStream = zipFile.getInputStream(zae);
        try {
            JSONManifest manifest =
                    BarFileUtils.readJsonEntry(inStream, entryName, JSONManifest.class);
            if (!manifest.checkSchema()) {
                throw PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.params(entryName);
            }
            this.manifestJson = manifest.getJson();
        } finally {
            IOUtils.closeQuietly(inStream);
        }
    }

    /**
     * Check file size of entry in bar file.
     * @param zae bar file entry
     * @param entryName entry name
     * @param maxBarEntryFileSize File size of entry
     */
    protected void checkBarFileEntrySize(ZipArchiveEntry zae, String entryName,
            long maxBarEntryFileSize) {
        //[400] bar File size of file entry exceeds upper limit
        if (zae.getSize() > (long) (maxBarEntryFileSize * MB)) {
            String message = "Bar file entry size too large invalid file [%s: %sB]";
            log.info(String.format(message, entryName, String.valueOf(zae.getSize())));
            throw PersoniumCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE
                    .params(entryName, String.valueOf(zae.getSize()));
        }
    }

    /**
     * bar File size check.
     * @param barFile bar file
     */
    protected void checkBarFileSize(File barFile) {
        //[400] bar file file size exceeds the upper limit
        long maxBarFileSize = getMaxBarFileSize();
        if (barFile.length() > (long) (maxBarFileSize * MB)) {
            String message = "Bar file size too large invalid file [%sB]";
            log.info(String.format(message, String.valueOf(barFile.length())));
            throw PersoniumCoreException.BarInstall.BAR_FILE_SIZE_TOO_LARGE
                    .params(String.valueOf(barFile.length()));
        }
    }

    /**
     * Required file for bar file.
     */
    private Map<String, String> setupBarFileOrder() {
        Map<String, String> requiredBarFiles = new LinkedHashMap<String, String>();
        requiredBarFiles.put("bar/", BarFileReadRunner.ROOT_DIR);
        requiredBarFiles.put("bar/00_meta/", BarFileReadRunner.META_DIR);
        requiredBarFiles.put("bar/00_meta/00_manifest.json", BarFileReadRunner.MANIFEST_JSON);
        requiredBarFiles.put("bar/00_meta/90_rootprops.xml", BarFileReadRunner.ROOTPROPS_XML);
        return requiredBarFiles;
    }

    /**
     * Check the structure of the bar file.
     */
    private boolean checkBarFileStructures(ZipArchiveEntry zae, Map<String, String> requiredBarFiles)
            throws UnsupportedEncodingException, ParseException {

        String entryName = zae.getName(); // ex. "bar/00_meta/00_manifest.json"
        if (requiredBarFiles.containsKey(entryName)) {
            requiredBarFiles.remove(entryName);
        }
        return true;
    }

    /**
     * Check whether the installation destination Box has already been registered and whether the schema URL defined in the manifest has already been registered.
     */
    private void checkDuplicateBoxAndSchema(String schema) {
        PersoniumODataProducer producer = oDataEntityResource.getOdataProducer();

        //[400] A Box already having the same scheme URL exists
        //Search for a Box with the same schema URL, and if it hits more than one hit it is an error.
        BoolCommonExpression filter = PersoniumOptionsQueryParser.parseFilter("Schema eq '" + schema + "'");
        QueryInfo query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, query).getCount() > 0) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.params(schema);
        }

        //[405] Box of the same name already exists
        //Search is performed by using only the Box name, and if a search is hit regardless of the presence or absence of the schema, it is an error.
        filter = PersoniumOptionsQueryParser.parseFilter("Name eq '" + this.boxName + "'");
        query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
        if (producer.getEntitiesCount(Box.EDM_TYPE_NAME, query).getCount() > 0) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.params(this.boxName);
        }

        log.info("Install target Box is not found, able to install.");
    }

    /**
     * Acquisition of ODataEntityResource.
     * @return ODataEntityResource
     */
    public ODataEntityResource getODataEntityResource() {
        return oDataEntityResource;
    }
}

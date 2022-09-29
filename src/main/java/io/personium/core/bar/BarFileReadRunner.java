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
package io.personium.core.bar;

import static io.personium.core.utils.PersoniumUrl.SCHEME_LOCALCELL;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propstat;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.Response;
import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmStructuralType;
import org.odata4j.producer.EntityResponse;
import org.odata4j.stax2.XMLEventReader2;
import org.odata4j.stax2.XMLFactoryProvider2;
import org.odata4j.stax2.XMLInputFactory2;
import org.odata4j.stax2.staximpl.StaxXMLFactoryProvider2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.CommonUtils;
import io.personium.common.utils.CommonUtils.HttpMethod;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.bar.jackson.IJSONMappedObject;
import io.personium.core.bar.jackson.JSONExtRole;
import io.personium.core.bar.jackson.JSONLink;
import io.personium.core.bar.jackson.JSONManifest;
import io.personium.core.bar.jackson.JSONRelation;
import io.personium.core.bar.jackson.JSONRole;
import io.personium.core.bar.jackson.JSONRule;
import io.personium.core.bar.jackson.JSONUserDataLink;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavCommon;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.model.progress.ProgressManager;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumEdmxFormatParser;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataEntitiesResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.rs.odata.ODataResource;

/**
 * Class for reading bar files from the Http request body.
 */
public class BarFileReadRunner implements Runnable {

    private static final String CODE_BAR_INSTALL_FAILED = "PL-BI-0001";

    private static final String CODE_BAR_INSTALL_COMPLETED = "PL-BI-0000";

    private static final String CODE_BAR_INSTALL_STARTED = "PL-BI-1001";

    /**
     * Object for logging.
     */
    static Logger log = LoggerFactory.getLogger(BarFileReadRunner.class);

    static final long MB = 1024 * 1024;
    private static final int TYPE_WEBDAV_COLLECTION = 0;
    private static final int TYPE_ODATA_COLLECTION = 1;
    private static final int TYPE_SERVICE_COLLECTION = 2;
    private static final int TYPE_DAV_FILE = 3;
    private static final int TYPE_SVC_FILE = 4;
    private static final int TYPE_MISMATCH = -1;

    private Map<String, Boolean> barFileOrder;
    private File barFile;
    private ZipArchiveInputStream zipArchiveInputStream;
    private final String boxName;
    private final ODataEntityResource odataEntityResource;
    private final PersoniumODataProducer odataProducer;
    private final String entitySetName;
    private final String requestKey;

    static final String ROOT_DIR = "bar/";
    static final String META_DIR = "bar/00_meta/";
    static final String CONTENTS_DIR_NAME = "90_contents";
    static final String CONTENTS_DIR = ROOT_DIR + CONTENTS_DIR_NAME + "/";
    static final String MANIFEST_JSON = "00_manifest.json";
    static final String RELATION_JSON = "10_relations.json";
    static final String ROLE_JSON = "20_roles.json";
    static final String EXTROLE_JSON = "30_extroles.json";
    static final String RULE_JSON = "50_rules.json";
    static final String LINKS_JSON = "70_$links.json";
    static final String ROOTPROPS_XML = "90_rootprops.xml";
    static final String METADATA_XML = "00_$metadata.xml";
    static final String USERDATA_LINKS_JSON = "10_odatarelations.json";
    static final String USERDATA_DIR_NAME = "90_data";

    private static final String DCBOX_NO_SLUSH = "dcbox:";
    private static final String DCBOX = "dcbox:/";

    private Cell cell;
    private Box box;
    private BoxCmp boxCmp;
    private Map<String, DavCmp> davCmpMap;
    private Map<String, String> davFileContentTypeMap = new HashMap<String, String>();
    private Map<String, Element> davFileAclMap = new HashMap<String, Element>();
    private Map<String, List<Element>> davFilePropsMap = new HashMap<String, List<Element>>();
    private long linksOutputStreamSize = Long.parseLong(PersoniumUnitConfig
            .get(PersoniumUnitConfig.BAR.BAR_USERDATA_LINKS_OUTPUT_STREAM_SIZE));
    private long bulkSize = Long.parseLong(PersoniumUnitConfig
            .get(PersoniumUnitConfig.BAR.BAR_USERDATA_BULK_SIZE));
    private EventBus eventBus;
    private PersoniumEvent.Builder eventBuilder;
    private BarInstallProgressInfo progressInfo;

    /**
     * constructor.
     * @param barFile bar file object
     * @param cell Install target Cell
     * @param boxName Install target Box Name
     * @param odataEntityResource JAX-RS resource
     * @param producer ODataProducer
     * @param entitySetName entitySetName(=box name)
     * @param requestKey The value of the RequestKey field to be output to the event log
     */
    public BarFileReadRunner(
            File barFile,
            Cell cell,
            String boxName,
            ODataEntityResource odataEntityResource,
            PersoniumODataProducer producer,
            String entitySetName,
            String requestKey) {
        this.barFile = barFile;
        this.boxName = boxName;
        this.odataEntityResource = odataEntityResource;
        this.odataProducer = producer;
        this.entitySetName = entitySetName;
        this.cell = cell;
        this.box = null;
        this.boxCmp = null;
        this.davCmpMap = new HashMap<String, DavCmp>();
        this.requestKey = requestKey;
        setupBarFileOrder();
    }

    /**
     * bar file reading process.
     */
    public void run() {
        boolean isSuccess = true;

        String path = "/" + this.cell.getName() + "/" + boxName + "/";
        log.debug("install target: " + path);

        try {
            List<String> doneKeys = new ArrayList<String>();

            try {
                this.zipArchiveInputStream = new ZipArchiveInputStream(new FileInputStream(barFile));
            } catch (IOException e) {
                throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(e.getMessage());
            }
            //Check for existence of root directory ("bar /")
            if (!isRootDir()) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", ROOT_DIR, message);
                isSuccess = false;
                return;
            }

            //Existence check of 00_meta
            if (!isMetadataDir()) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", META_DIR, message);
                isSuccess = false;
                return;
            }

            //Loading 00_meta
            ZipArchiveEntry zae = null;
            try {
                long maxBarEntryFileSize = getMaxBarEntryFileSize();
                Set<String> keyList = barFileOrder.keySet();

                while ((zae = this.zipArchiveInputStream.getNextZipEntry()) != null) {
                    String entryName = zae.getName();
                    log.debug("Entry Name: " + entryName);
                    log.debug("Entry Size: " + zae.getSize());
                    log.debug("Entry Compressed Size: " + zae.getCompressedSize());
                    if (!zae.isDirectory()) {
                        this.progressInfo.addDelta(1L);
                    }

                    //Analysis & data registration of entry in bar file
                    isSuccess = createMetadata(zae, entryName, maxBarEntryFileSize, keyList, doneKeys);
                    if (!isSuccess) {
                        break;
                    }
                    //When 90_contents is detected, the presence or absence of collection definition is checked
                    if (isContentsDir(zae)) {
                        if (davCmpMap.isEmpty()) {
                            writeOutputStream(true, "PL-BI-1004", zae.getName());
                            isSuccess = false;
                        } else {
                            writeOutputStream(false, "PL-BI-1003", zae.getName());
                        }
                        doneKeys.add(zae.getName());
                        break;
                    }
                }
            } catch (IOException ex) {
                isSuccess = false;
                log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            }

            //Reading 90_contents (user data)
            if (isSuccess && isContentsDir(zae)) {
                isSuccess = createContents();
            }

            //It is checked whether all necessary data has been processed
            //(Skip if error has already been detected)
            if (isSuccess) {
                Set<String> filenameList = barFileOrder.keySet();
                for (String filename : filenameList) {
                    Boolean isNecessary = barFileOrder.get(filename);
                    if (isNecessary && !doneKeys.contains(filename)) {
                        String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                        writeOutputStream(true, "PL-BI-1004", filename, message);
                        isSuccess = false;
                    }
                }
            }
        } catch (Throwable ex) {
            isSuccess = false;
            String message = getErrorMessage(ex);
            log.info("Exception: " + message, ex.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1005", "", message);
        } finally {
            if (isSuccess) {
                writeOutputStream(false, CODE_BAR_INSTALL_COMPLETED, SCHEME_LOCALCELL + ":/" + boxName, "");
                this.progressInfo.setStatus(ProgressInfo.STATUS.COMPLETED);
            } else {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(false, CODE_BAR_INSTALL_FAILED, SCHEME_LOCALCELL + ":/" + boxName, message);
                this.progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            }
            this.progressInfo.setEndTime();
            writeToProgressCache(true);
            IOUtils.closeQuietly(this.zipArchiveInputStream);
            try {
                Files.deleteIfExists(this.barFile.toPath());
            } catch (IOException e) {
                log.warn("Failed to remove bar file. [" + this.barFile.getAbsolutePath() + "].", e);
            }
        }
    }

    /**
     * bar Make settings for internal event output of installation processing status.
     */
    private void setEventBus() {
        // The schema of the TODO Box and the subject's log are implemented at the time of formal correspondence of internal events

        String type = HttpMethod.MKCOL;
        String object = SCHEME_LOCALCELL + ":/" + boxName;
        String result = "";
        this.eventBuilder = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(result)
                .requestKey(this.requestKey);
        this.eventBus = this.cell.getEventBus();
    }

    /**
     * Get messages from exception objects.
     * @param ex exception object
     * @return message
     */
    private String getErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        //If there is no message Return exception class name
        if (message == null) {
            message = "throwed " + ex.getClass().getCanonicalName();
        }
        return message;
    }

    /**
     * Returns whether the entry obtained from the Zip archive is a "bar /" directory or not.
     * true if it is @return "bar /"
     */
    private boolean isRootDir() {
        return isMatchEntryName(ROOT_DIR);
    }

    /**
     * Returns whether the entry obtained from the Zip archive is the "bar / 00_meta" directory or not.
     * true if it is @return "bar / 00_meta"
     */
    private boolean isMetadataDir() {
        return isMatchEntryName(META_DIR);
    }

    /**
     * Returns whether the entry obtained from the Zip archive is the "bar / 90_contents" directory or not.
     * @param zae ZipArchiveEntry object
     * true if it is @return "bar / 90_contents"
     */
    private boolean isContentsDir(ZipArchiveEntry zae) {
        boolean ret = false;
        if (zae == null) {
            ret = isMatchEntryName(CONTENTS_DIR);
        } else {
            ret = zae.getName().equals(CONTENTS_DIR);
        }
        return ret;
    }

    /**
     * Returns whether the entry name obtained from the Zip archive matches the specified character string.
     * @param name String to be compared
     * @return true if matching
     */
    private boolean isMatchEntryName(String name) {
        boolean ret = false;
        try {
            ZipArchiveEntry zae = this.zipArchiveInputStream.getNextZipEntry();
            if (zae != null) {
                ret = zae.getName().equals(name);
            }
        } catch (IOException ex) {
            log.info("bar file entry was not read.");
            ret = false;
        }
        return ret;
    }

    /**
     * Get the maximum file size (MB) in the BAR file from the property file.
     * @return io.personium.core.bar.entry.maxSize
     */
    private long getMaxBarEntryFileSize() {
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
     * Read and register one metadata in the bar file.
     * @param zae ZipArchiveEntry
     * @param entryName bar File entry name
     * @param maxSize Maximum file size of entry (MB)
     * @param keyList definition file list
     * @param doneKeys run & executed definition file
     * @return boolean Process success
     */
    protected boolean createMetadata(
            ZipArchiveEntry zae,
            String entryName,
            long maxSize,
            Set<String> keyList,
            List<String> doneKeys) {
        if (!isValidFileStructure(zae, entryName, maxSize, doneKeys)) {
            return false;
        }

        if (getFileExtension(entryName).equals(".xml")) {
            //For XML files
            String boxUrl = this.box.getCell().getUrl() + this.box.getName();
            if (!registXmlEntry(entryName, this.zipArchiveInputStream, boxUrl)) {
                doneKeys.add(entryName);
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", entryName);
            doneKeys.add(entryName);
            return true;
        }

        if (getFileExtension(entryName).equals(".json")) {
            //For JSON files
            if (!registJsonEntry(entryName, this.zipArchiveInputStream)) {
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", entryName);
            doneKeys.add(entryName);
            return true;
        }

        if (entryName.endsWith("/")) {
            return true;
        }
        return false;
    }

    /**
     * Read one content data (bar/90_contents) in the bar file and register it.
     * @return boolean Processing result true:success false:failure
     */
    protected boolean createContents() {
        boolean isSuccess = true;
        // Create a map for each collection type.
        Map<String, DavCmp> odataCols = getCollections(DavCmp.TYPE_COL_ODATA);
        Map<String, DavCmp> webdavCols = getCollections(DavCmp.TYPE_COL_WEBDAV);
        // Since it may be referred to as parent, Box must be registered.
        webdavCols.putAll(getCollections(DavCmp.TYPE_COL_BOX));
        Map<String, DavCmp> serviceCols = getCollections(DavCmp.TYPE_COL_SVC);

        DavCmp davCmp = null;
        List<String> doneKeys = new ArrayList<String>();
        try {
            ZipArchiveEntry zae = null;
            String currentPath = null;
            int userDataCount = 0;
            List<IJSONMappedObject> userDataLinks = new ArrayList<IJSONMappedObject>();
            LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();
            Map<String, String> fileNameMap = new HashMap<String, String>();
            PersoniumODataProducer producer = null;

            while ((zae = this.zipArchiveInputStream.getNextZipEntry()) != null) {
                String entryName = zae.getName();
                log.debug("Entry Name: " + entryName);
                log.debug("Entry Size: " + zae.getSize());
                log.debug("Entry Compressed Size: " + zae.getCompressedSize());
                if (!zae.isDirectory()) {
                    this.progressInfo.addDelta(1L);
                }
                writeOutputStream(false, CODE_BAR_INSTALL_STARTED, entryName);

                //When processing changes from ODataCollection to Dav / ServiceCollection / another ODataCollection resource
                //If it is necessary to register the user data or link, the process is executed
                if (currentPath != null && !entryName.startsWith(currentPath)) {
                    if (!execBulkRequest(davCmp.getCell().getId(), bulkRequests, fileNameMap, producer)) {
                        return false;
                    }
                    if (!createUserdataLinks(producer, userDataLinks)) {
                        return false;
                    }
                    userDataLinks = new ArrayList<IJSONMappedObject>();
                    currentPath = null;
                }
                int entryType = getEntryType(entryName, odataCols, webdavCols, serviceCols, this.davFileContentTypeMap);
                switch (entryType) {
                case TYPE_ODATA_COLLECTION:
                    //Register OData Collection
                    if (!odataCols.isEmpty()) {
                        if (!isValidODataContents(entryName, odataCols, doneKeys)) {
                            return false;
                        }
                        Pattern formatPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/");
                        Matcher formatMatcher = formatPattern.matcher(entryName);
                        if (formatMatcher.matches()) {
                            currentPath = entryName;
                        }
                        Pattern userodataDirPattern = Pattern.compile(CONTENTS_DIR + ".+/90_data/.+");
                        Matcher userodataDirMatcher = userodataDirPattern.matcher(entryName);

                        if (getFileExtension(entryName).equals(".xml")) {
                            //Analysis of 00_ $ metadata.xml · User schema registration
                            davCmp = getCollection(entryName, odataCols);
                            //Update Producer if OData's collection switches
                            producer = davCmp.getODataProducer();
                            if (!registUserSchema(entryName, this.zipArchiveInputStream, davCmp)) {
                                doneKeys.add(entryName);
                                return false;
                            }
                            writeOutputStream(false, "PL-BI-1003", entryName);
                            doneKeys.add(entryName);
                            continue;
                        } else if (entryName.endsWith(USERDATA_LINKS_JSON)) {
                            userDataLinks = registJsonLinksUserdata(entryName, this.zipArchiveInputStream);
                            if (userDataLinks == null) {
                                doneKeys.add(entryName);
                                return false;
                            }
                            writeOutputStream(false, "PL-BI-1003", entryName);
                            doneKeys.add(entryName);
                            continue;
                        } else if (userodataDirMatcher.matches() && getFileExtension(entryName).equals(".json")) {
                            userDataCount++;
                            if (!setBulkRequests(entryName, producer, bulkRequests, fileNameMap)) {
                                return false;
                            }
                            doneKeys.add(entryName);

                            if ((userDataCount % bulkSize) == 0
                                    && !execBulkRequest(davCmp.getCell().getId(),
                                            bulkRequests, fileNameMap, producer)) {
                                return false;
                            }
                            continue;
                        } else if (!entryName.endsWith("/")) {
                            //If there are files other than xml and json files, return error
                            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                            log.info(message + " [" + entryName + "]");
                            writeOutputStream(true, "PL-BI-1004", entryName, message);
                            return false;
                        }
                    }
                    break;

                case TYPE_DAV_FILE:
                    //Create a WebDAV collection
                    //Register entries under bar / 90_contents / {davcol_name} one by one
                    if (!registWebDavFile(entryName, this.zipArchiveInputStream, webdavCols)) {
                        return false;
                    }
                    break;

                case TYPE_SVC_FILE:
                    //Creating a Service collection
                    if (!installSvcCollection(webdavCols, entryName)) {
                        return false;
                    }
                    break;

                case TYPE_MISMATCH:
                    //Entries not under the OData collection and not defined in rootprops
                    String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2006");
                    log.info(message + " [" + entryName + "]");
                    writeOutputStream(true, "PL-BI-1004", entryName, message);
                    return false;

                default:
                    break;
                }
                writeOutputStream(false, "PL-BI-1003", entryName);
                doneKeys.add(entryName);
            }
            //When processing on resources of ODataCollection is finished, if registration of user data and link registration is necessary, it is executed
            if (currentPath != null) {
                if (!execBulkRequest(davCmp.getCell().getId(), bulkRequests, fileNameMap, producer)) {
                    return false;
                }
                if (!createUserdataLinks(producer, userDataLinks)) {
                    return false;
                }
                userDataLinks = null;
            }
        } catch (IOException ex) {
            isSuccess = false;
            log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, CODE_BAR_INSTALL_FAILED, "", message);
        }
        //Confirm mandatory data (bar / 90_contents / {odatacol_name} / 00 _ $ metadata.xml)
        isSuccess = checkNecessaryFile(isSuccess, odataCols, doneKeys);
        return isSuccess;
    }

    private boolean checkNecessaryFile(boolean isSuccess, Map<String, DavCmp> odataCols, List<String> doneKeys) {
        Set<String> colList = odataCols.keySet();
        for (String colName : colList) {
            String filename = colName + METADATA_XML;
            if (!doneKeys.contains(filename)) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", filename, message);
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private boolean installSvcCollection(Map<String, DavCmp> webdavCols, String entryName) {
        //Register entries under the bar / 90_contents / {svccol_name} one by one as WebDAV / service
        //Convert {serviceCollection} / {scriptName} to {serviceCollection} / __ src / {scriptName}
        int lastSlashIndex = entryName.lastIndexOf("/");
        StringBuilder serviceSrcName = new StringBuilder();
        serviceSrcName.append(entryName.substring(0, lastSlashIndex));
        serviceSrcName.append("/__src");
        serviceSrcName.append(entryName.substring(lastSlashIndex));

        if (!registWebDavFile(serviceSrcName.toString(), this.zipArchiveInputStream, webdavCols)) {
            return false;
        }
        return true;
    }

    private boolean setBulkRequests(String entryName,
            PersoniumODataProducer producer,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            Map<String, String> fileNameMap) {
        BulkRequest bulkRequest = new BulkRequest();
        String key = PersoniumUUID.randomUUID();
        try {
            //Get entityType name
            String entityTypeName = getEntityTypeName(entryName);
            if (producer.getMetadata().findEdmEntitySet(entityTypeName) == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            //Get JSON of user data in StringReader format from ZipArchiveImputStream
            StringReader stringReader = getStringReaderFromZais();

            //Generate request body
            ODataResource odataResource = odataEntityResource.getOdataResource();
            ODataEntitiesResource resource = new ODataEntitiesResource(odataResource, entityTypeName);
            OEntity oEntity = resource.getOEntityWrapper(stringReader, odataResource, producer.getMetadata());

            UserDataODataProducer userDataProducer = (UserDataODataProducer) producer;
            EntitySetDocHandler docHandler = producer.getEntitySetDocHandler(entityTypeName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(userDataProducer.getEntityTypeId(oEntity.getEntitySetName()));

            odataEntityResource.setOdataProducer(userDataProducer);

            //ID conflict check in data
            //TODO compound primary key correspondence, unique key check, NTKP compliant
            key = oEntity.getEntitySetName() + ":" + (String) docHandler.getStaticFields().get("__id");

            if (bulkRequests.containsKey(key)) {
                throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
            }

            //Pay out UUID if ID is not specified
            if (docHandler.getId() == null) {
                docHandler.setId(PersoniumUUID.randomUUID());
            }
            bulkRequest.setEntitySetName(entityTypeName);
            bulkRequest.setDocHandler(docHandler);
        } catch (Exception e) {
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info(entryName + " : " + e.getMessage());
            bulkRequest.setError(e);
            return false;
        }
        bulkRequests.put(key, bulkRequest);
        fileNameMap.put(key, entryName);
        return true;
    }

    private StringReader getStringReaderFromZais() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(this.zipArchiveInputStream, "UTF-8"));
        StringBuffer buf = new StringBuffer();
        String str = null;
        while ((str = bufferedReader.readLine()) != null) {
            buf.append(str);
        }
        StringReader stringReader = new StringReader(buf.toString());
        return stringReader;
    }

    private String getEntityTypeName(String entryName) {
        String[] hierarchy = entryName.split("/");
        int size = hierarchy.length;
        String entityTypeName = hierarchy[size - 2];
        return entityTypeName;
    }

    private boolean execBulkRequest(String cellId, LinkedHashMap<String, BulkRequest> bulkRequests,
            Map<String, String> fileNameMap,
            PersoniumODataProducer producer) {
        //Execute bulk registration in bulk
        producer.bulkCreateEntity(producer.getMetadata(), bulkRequests, cellId);

        //Check response
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            //If an error has occurred, return the error response
            if (request.getValue().getError() != null) {
                if (request.getValue().getError() instanceof PersoniumCoreException) {
                    PersoniumCoreException e = (PersoniumCoreException) request.getValue().getError();
                    writeOutputStream(true, "PL-BI-1004", fileNameMap.get(request.getKey()), e.getMessage());
                    log.info("PersoniumCoreException: " + e.getMessage());
                } else {
                    Exception e = request.getValue().getError();
                    String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2003");
                    writeOutputStream(true, "PL-BI-1004", fileNameMap.get(request.getKey()), message);
                    log.info("Regist Entity Error: " + e.toString());
                    log.info("Regist Entity Error: " + e.getClass().getName());
                    log.info("Regist Entity Error: " + e);
                }
                return false;
            }
            writeOutputStream(false, "PL-BI-1003", fileNameMap.get(request.getKey()));
        }

        bulkRequests.clear();
        fileNameMap.clear();
        return true;
    }

    /**
     * Get the type of entry under 90_contents of the bar file.
     * @param entryName bar File entry name
     * @param odataCols List of OData collections
     * @param webdavCols List of WebDAV collections
     * @param serviceCols List of service collections
     * @param davFiles List of WebDAV files
     * @return entry type
     */
    protected int getEntryType(String entryName,
            Map<String, DavCmp> odataCols,
            Map<String, DavCmp> webdavCols,
            Map<String, DavCmp> serviceCols,
            Map<String, String> davFiles) {

        if (odataCols.containsKey(entryName)) {
            return TYPE_ODATA_COLLECTION;
        } else if (webdavCols.containsKey(entryName)) {
            return TYPE_WEBDAV_COLLECTION;
        } else if (serviceCols.containsKey(entryName)) {
            return TYPE_SERVICE_COLLECTION;
        } else if (davFiles.containsKey(entryName)) {
            return TYPE_DAV_FILE;
        }

        for (Entry<String, DavCmp> entry : odataCols.entrySet()) {
            String odataColPath = entry.getKey();
            if (entryName.startsWith(odataColPath)) {
                return TYPE_ODATA_COLLECTION;
            }
        }

        for (Entry<String, DavCmp> entry : serviceCols.entrySet()) {
            String serviceColPath = entry.getKey();
            if (entryName.startsWith(serviceColPath)) {
                return TYPE_SVC_FILE;
            }
        }

        return TYPE_MISMATCH;
    }

    /**
     * Register the WebDAV file.
     * @param entryName bar File entry name
     * @param inputStream data
     * @param webdavCols WebDAV collection list
     * @return true: registration successful, false: registration failure
     */
    protected boolean registWebDavFile(String entryName, InputStream inputStream,
            Map<String, DavCmp> webdavCols) {

        //Retrieve the file path / collection name of the registration destination
        String filePath = entryName.replaceAll(CONTENTS_DIR, "");
        String colPath = entryName.substring(0, entryName.lastIndexOf("/") + 1);

        //Create DavCmp
        DavCmp parentCmp = webdavCols.get(colPath);

        //Check number of collection files in parent collection
        int maxChildResource = PersoniumUnitConfig.getMaxChildResourceCount();
        if (parentCmp.getChildrenCount() >= maxChildResource) {
            //The number of collection files that can be created in the collection exceeds the limit, so it is an error
            String message = PersoniumCoreMessageUtils.getMessage("PR400-DV-0007");
            log.info(message);
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        //Create a new node

        //Add pointer to parent node
        String fileName = "";
        fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

        //Implementation-dependent elimination
        DavCmp fileCmp = parentCmp.getChild(fileName);

        //Check Content-Type
        String contentType = null;
        try {
            contentType = this.davFileContentTypeMap.get(entryName);
            RuntimeDelegate.getInstance().createHeaderDelegate(MediaType.class).fromString(contentType);
        } catch (Exception e) {
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2005");
            log.info(message + ": " + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        //File Registration
        try {
            fileCmp.putForCreate(contentType, new CloseShieldInputStream(inputStream));
        } catch (Exception e) {
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2004");
            log.info(message + ": " + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        //ACL registration
        Element aclElement = davFileAclMap.get(entryName);
        if (aclElement != null) {
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(CommonUtils.nodeToString(aclElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            fileCmp.acl(aclXml);
        }

        //PROPPATCH registration
        registProppatch(fileCmp, davFilePropsMap.get(entryName), fileCmp.getUrl());

        return true;
    }

    /**
     * Returns the file name extension.
     * @param filename file name
     */
    private String getFileExtension(String filename) {
        String extension = "";
        int idx = filename.lastIndexOf(".");
        if (idx >= 0) {
            extension = filename.substring(idx);
        }
        return extension;
    }

    /**
     * Analyze 90_rootprops_xml and perform registration processing such as Collectoin / ACL / WebDAV.
     * @param rootPropsName Path name in bar file of 90_rootprops_xml
     * @param inputStream Input stream
     * @param boxUrl URL of box
     * @return true if successful
     */
    protected boolean registXmlEntry(String rootPropsName, InputStream inputStream, String boxUrl) {
        //If you pass InputStream to the XML parser (StAX, SAX, DOM) as is, the file list acquisition processing
        //Because it will be interrupted, store it as a provisional countermeasure and then parse it
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                buf.append(str);
            }

            Multistatus multiStatus = Multistatus.unmarshal(new ByteArrayInputStream(buf.toString().getBytes()));

            //Validate the definition contents of 90_rootprops.xml.
            //By checking in advance, make sure that garbage data is not created.
            if (!validateCollectionDefinitions(multiStatus, rootPropsName)) {
                return false;
            }
            for (Response response : multiStatus.getResponse()) {
                int collectionType = TYPE_WEBDAV_COLLECTION;
                boolean hasCollection = false;
                boolean isBox = false;

                List<String> hrefs = response.getHref();
                String href = hrefs.get(0);
                if (href.equals("dcbox:")) {
                    href = DCBOX;
                }
                if (href.equals(DCBOX)) {
                    isBox = true;
                }
                String collectionUrl = null;
                collectionUrl = href.replaceFirst(DCBOX, boxUrl + "/");

                List<Element> propElements = new ArrayList<Element>();
                Element aclElement = null;
                String contentType = null;
                for (Propstat propstat : response.getPropstat()) {
                    Prop prop = propstat.getProp();
                    Resourcetype resourceType = prop.getResourcetype();
                    if (resourceType != null) {
                        if (resourceType.getCollection() != null) {
                            hasCollection = true;
                        }
                        List<Element> elements = resourceType.getAny();
                        for (Element element : elements) {
                            String nodeName = element.getNodeName();
                            if (nodeName.equals("p:odata")) {
                                collectionType = TYPE_ODATA_COLLECTION;
                            } else if (nodeName.equals("p:service")) {
                                collectionType = TYPE_SERVICE_COLLECTION;
                            }
                        }
                    }
                    //prop subordinate confirmation
                    Getcontenttype getContentType = prop.getGetcontenttype();
                    if (getContentType != null) {
                        contentType = getContentType.getValue();
                    }

                    List<Element> pElements = prop.getAny();
                    for (Element element : pElements) {
                        String nodeName = element.getNodeName();
                        if (nodeName.equals("creationdate")
                                || nodeName.equals("getlastmodified")
                                || nodeName.equals("resourcetype")) {
                            continue;
                        }
                        if (nodeName.equals("acl")) {
                            aclElement = BarFileUtils.convertToRoleInstanceUrl(element,
                                    box.getCell().getUrl(), box.getName());
                            continue;
                        }
                        propElements.add(element);
                    }
                }

                String entryName = CONTENTS_DIR + href.replaceFirst(DCBOX, "");
                if (isBox) {
                    // For Box, collection and ACL registration.
                    davCmpMap.put(entryName, boxCmp);
                    registBoxAclAndProppatch(this.box, aclElement, propElements, collectionUrl);
                } else if (hasCollection) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    //For collections, collection, ACL, PROPPATH registration
                    log.info(entryName);
                    createCollection(collectionUrl, entryName, this.cell, this.box, collectionType, aclElement,
                            propElements);
                } else {
                    //WebDAV file
                    this.davFileContentTypeMap.put(entryName, contentType);
                    this.davFileAclMap.put(entryName, aclElement);
                    this.davFilePropsMap.put(entryName, propElements);
                }
            }
        } catch (PersoniumCoreException e) {
            log.info("PersoniumCoreException: " + e.getMessage());
            writeOutputStream(true, "PL-BI-1004", rootPropsName, e.getMessage());
            return false;
        } catch (Exception ex) {
            String message = getErrorMessage(ex);
            log.info("XMLParseException: " + message, ex.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
            return false;
        }
        return true;
    }

    /**
     * Verify that there is no inconsistency in the hierarchical structure of path defined in 90_rootprops.xml.
     * @param multiStatus 90 JOXB object read from 90 _rootprops.xml
     * @param rootPropsName Name of the entry currently being processed (for log output)
     * @return Returns true if there is no conflict, false if there is contradiction.
     */
    protected boolean validateCollectionDefinitions(Multistatus multiStatus, String rootPropsName) {

        //Read the XML definition and get the path definition and type of href element (OData collection / WebDAV collection / service collection, WebDAV file, service source).
        Map<String, Integer> pathMap = new LinkedHashMap<String, Integer>();
        for (Response response : multiStatus.getResponse()) {
            List<String> hrefs = response.getHref();
            if (hrefs.size() != 1) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2008");
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            String href = hrefs.get(0);
            //If there is no href attribute value, it is regarded as a definition error.
            if (href == null || href.length() == 0) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2009");
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            //If it does not start with "dcbox: /" as the href attribute value, it is regarded as a definition error.
            if (!href.startsWith(DCBOX_NO_SLUSH)) {
                String message = MessageFormat.format(
                        PersoniumCoreMessageUtils.getMessage("PL-BI-2010"), DCBOX_NO_SLUSH, href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            //Select the type of the defined path. Abnormal termination (log output is unnecessary) when an incorrect path type is specified.
            int collectionType = getCollectionType(rootPropsName, response);
            switch (collectionType) {
            case TYPE_WEBDAV_COLLECTION:
            case TYPE_ODATA_COLLECTION:
            case TYPE_SERVICE_COLLECTION:
                if (href.endsWith("/")) {
                    href = href.substring(0, href.length() - 1);
                }
                break;
            case TYPE_MISMATCH:
                return false;
            default:
                break;
            }
            //If the path definitions are duplicated, the same data is registered, so it is defined as a definition error.
            //In order to ignore the condition of "/" designation at the end of the path, check at this timing.
            if (pathMap.containsKey(href)) {
                String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2011"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            pathMap.put(href, Integer.valueOf(collectionType));
        }
        //Verify the validity of the Collection path based on the read path definition.
        //· Common: Definition of Box route is mandatory
        //· Common: There is no inconsistency in the path hierarchy structure
        //· For OData collection: Path definition under collection does not exist
        //· For Service collection: Path definition "__src" exists under collection
        Set<String> keySet = pathMap.keySet();
        for (Entry<String, Integer> entry : pathMap.entrySet()) {
            String href = entry.getKey();
            int currentCollectionType = entry.getValue();
            int upperPathposition = href.lastIndexOf("/");
            if (upperPathposition < 0) { //Skip the path of "dcbox:" because it is not checked
                continue;
            }
            //If an upper layer to be checked is not defined as path information, it is defined as a definition error.
            //Even if the Box root path is not defined, definition error also occurs.
            String upper = href.substring(0, upperPathposition);
            if (!keySet.contains(upper)) {
                String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2012"), upper);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            int upperCollectionType = pathMap.get(upper);
            String resourceName = href.substring(upperPathposition + 1, href.length());
            if (upperCollectionType == TYPE_ODATA_COLLECTION) {
                //OData collection: If a collection / file is defined under the collection, it is a definition error.
                String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2013"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            } else if (upperCollectionType == TYPE_SERVICE_COLLECTION) {
                //Service collection: If a collection / file is defined under the collection, it is a definition error.
                //However, only "__ src" is excluded as an exception.
                if (!("__src".equals(resourceName) && currentCollectionType == TYPE_WEBDAV_COLLECTION)) {
                    String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2014"), href);
                    writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                    return false;
                }
            } else if (upperCollectionType == TYPE_DAV_FILE) {
                //If a collection / file is defined under the WebDAV file / Service source, it is a definition error.
                String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2015"), href);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
            //If the current collection is a Service collection, if "__src" is not defined in the immediately following path, it is a definition error.
            if (currentCollectionType == TYPE_SERVICE_COLLECTION) {
                String srcPath = href + "/__src";
                if (!keySet.contains(srcPath) || pathMap.get(srcPath) != TYPE_WEBDAV_COLLECTION) {
                    String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2016"), href);
                    writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                    return false;
                }
            }

            //Confirm that it is correct as a resource name (collection / file name format is common).
            if (!DavCommon.isValidResourceName(resourceName)) {
                String message = MessageFormat.format(PersoniumCoreMessageUtils.getMessage("PL-BI-2017"), resourceName);
                writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                return false;
            }
        }
        return true;
    }

    /**
     * Get the collection type of the path defined in each response tag in 90_rootprops.xml.
     * @param rootPropsName Name of the entry currently being processed (for log output)
     * @param response JAXB object for response tag to be processed
     * @return Returns the value of the collection type according to the definition content.
     * WebDAV file, Service source is returned as WebDAV file.
     * If the type of unauthorized collection is defined, return it as undefined.
     */
    private int getCollectionType(String rootPropsName, Response response) {
        //Get the type of the collection defined by following the <propstat> element
        //If the DOM node path of -prop / resourcetype / collecton exists, it is regarded as a collection definition
        //At this time, if there is no DOM node path of "p: odata" or "p: service", it is regarded as a WebDAV collection definition
        //- If it does not apply to the above, it is regarded as a WebDAv file or service source
        for (Propstat propstat : response.getPropstat()) {
            Prop prop = propstat.getProp();
            Resourcetype resourceType = prop.getResourcetype();
            if (resourceType != null && resourceType.getCollection() != null) {
                List<Element> elements = resourceType.getAny();
                for (Element element : elements) {
                    String nodeName = element.getNodeName();
                    if (nodeName.equals("p:odata")) {
                        return TYPE_ODATA_COLLECTION;
                    } else if (nodeName.equals("p:service")) {
                        return TYPE_SERVICE_COLLECTION;
                    } else {
                        String message = MessageFormat.format(
                                PersoniumCoreMessageUtils.getMessage("PL-BI-2018"), nodeName);
                        writeOutputStream(true, "PL-BI-1004", rootPropsName, message);
                        return TYPE_MISMATCH;
                    }
                }
            } else {
                return TYPE_DAV_FILE;
            }
        }
        return TYPE_WEBDAV_COLLECTION;
    }

    /**
     * Process one JSON data.
     * @param entryName Target file name
     * @param inputStream Input stream
     * @return true if successful
     */
    private boolean registJsonEntry(String entryName, InputStream inputStream) {
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        try {
            jp = f.createParser(inputStream);
            JsonToken token = jp.nextToken(); //JSON root element ("{")
            Pattern formatPattern = Pattern.compile(".*/+(.*)");
            Matcher formatMatcher = formatPattern.matcher(entryName);
            String jsonName = formatMatcher.replaceAll("$1");

            if (token == JsonToken.START_OBJECT) {
                if (jsonName.equals(RELATION_JSON) || jsonName.equals(ROLE_JSON)
                        || jsonName.equals(EXTROLE_JSON) || jsonName.equals(LINKS_JSON)
                        || jsonName.equals(RULE_JSON)) {
                    registJsonEntityData(jp, mapper, jsonName);
                } else if (jsonName.equals(MANIFEST_JSON)) {
                    manifestJsonValidate(jp, mapper); //Box created at the beginning of installation
                }
                log.debug(jsonName);
            } else {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        } catch (PersoniumCoreException e) {
            //JSON file validation error
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("PersoniumCoreException" + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (JsonParseException e) {
            //Error parsing JSON file
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (JsonMappingException e) {
            //Data definition error of JSON file
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            return false;
        } catch (Exception e) {
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("Exception: " + e.getMessage(), e.fillInStackTrace());
            return false;
        }
        return true;
    }

    /**
     * Reads data of 10_odatarelations.json and generates Link information of user data.
     * @param entryName Target file name
     * @param inputStream Input stream
     * @return true if successful
     */
    protected List<IJSONMappedObject> registJsonLinksUserdata(String entryName, InputStream inputStream) {
        List<IJSONMappedObject> userDataLinks = new ArrayList<IJSONMappedObject>();
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        try {
            jp = f.createParser(inputStream);
            JsonToken token = jp.nextToken(); //JSON root element ("{")

            if (token == JsonToken.START_OBJECT) {
                token = jp.nextToken();

                //Check $ links
                checkMatchFieldName(jp, USERDATA_LINKS_JSON);

                token = jp.nextToken();
                //If it is not an array, an error
                if (token != JsonToken.START_ARRAY) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
                }
                token = jp.nextToken();

                while (jp.hasCurrentToken()) {
                    if (token == JsonToken.END_ARRAY) {
                        break;
                    } else if (token != JsonToken.START_OBJECT) {
                        throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
                    }
                    userDataLinks.add(barFileJsonValidate(jp, mapper, USERDATA_LINKS_JSON));

                    token = jp.nextToken();
                }
            } else {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(USERDATA_LINKS_JSON);
            }
        } catch (JsonParseException e) {
            //Error parsing JSON file
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (JsonMappingException e) {
            //Data definition error of JSON file
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (PersoniumCoreException e) {
            //JSON file validation error
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("PersoniumCoreException" + e.getMessage(), e.fillInStackTrace());
            return null;
        } catch (IOException e) {
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2000");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            log.info("IOException: " + e.getMessage(), e.fillInStackTrace());
            return null;
        }
        return userDataLinks;
    }

    /**
     * Check that the file structure in the metadata directory is correct.
     * @param zae
     * @param entryName
     * @param maxSize
     * @param doneKeys
     * @return Return true if correct
     */
    private boolean isValidFileStructure(ZipArchiveEntry zae,
            String entryName,
            long maxSize,
            List<String> doneKeys) {
        writeOutputStream(false, CODE_BAR_INSTALL_STARTED, entryName);

        //Check if it is an invalid file
        if (!barFileOrder.containsKey(entryName)) {
            log.info("[" + entryName + "] invalid file");
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        //Check if the order is correct
        Pattern formatPattern = Pattern.compile(".*/+([0-9][0-9])_.*");
        Matcher formatMatcher = formatPattern.matcher(entryName);
        String entryIndex = formatMatcher.replaceAll("$1");
        if (doneKeys.isEmpty()) {
            //Required to be "00" for the first entry
            if (!entryIndex.equals("00")) {
                log.info("bar/00_meta/00_manifest.json is not exsist");
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        } else {
            String lastEntryName = doneKeys.get(doneKeys.size() - 1);
            formatMatcher = formatPattern.matcher(lastEntryName);
            String lastEntryIndex = formatMatcher.replaceAll("$1");

            //Compare with the prefix of the previously processed entry
            if (entryIndex.compareTo(lastEntryIndex) < 0) {
                log.info("[" + entryName + "] invalid file");
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        //[400] bar File / bar file entry size exceeds the upper limit
        if (zae.getSize() > (long) (maxSize * MB)) {
            log.info("Bar file entry size too large invalid file [" + entryName + "]");
            String message = PersoniumCoreException.BarInstall.BAR_FILE_ENTRY_SIZE_TOO_LARGE
                    .params(zae.getName(), String.valueOf(zae.getSize())).getMessage();
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        return true;
    }

    /**
     * bar / 90_contents / {OdataCol_name} is checked to see if it is a correct definition.
     * @param entryName entry name (collection name)
     * @param colMap Map object for collection
     * @param doneKeys List of entries for processed OData collection
     * @return judgment processing result
     */
    protected boolean isValidODataContents(String entryName, Map<String, DavCmp> colMap, List<String> doneKeys) {

        String odataColPath = "";
        for (Map.Entry<String, DavCmp> entry : colMap.entrySet()) {
            if (entryName.startsWith(entry.getKey())) {
                odataColPath = entry.getKey();
                break;
            }
        }

        //Entry name directly under the OData collection
        String odataPath = entryName.replaceAll(odataColPath, "");

        //bar / 90_contents / {OData_collection} Order check immediately below
        if (USERDATA_LINKS_JSON.equals(odataPath)) {

            //Check whether 00_ $ metadata.xml has been processed
            String meatadataPath = odataColPath + METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
            //Check whether 90_data / has been processed
            String userDataPath = odataColPath + USERDATA_DIR_NAME + "/";
            if (doneKeys.contains(userDataPath)) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }
        if (odataPath.startsWith(USERDATA_DIR_NAME + "/")) {
            //Check whether 00_ $ metadata.xml has been processed
            String meatadataPath = odataColPath + METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        //bar / 90_contents / {OData_collection} / {dirPath} / check
        String dirPath = null;
        Pattern pattern = Pattern.compile("^([^/]+)/.*");
        Matcher m = pattern.matcher(odataPath);
        if (m.matches()) {
            dirPath = m.replaceAll("$1");
        }
        if (dirPath != null && !dirPath.equals(USERDATA_DIR_NAME)) {
            //bar / 90_contents / {OData_collection} / {dir} / is an error
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
            log.info(message + "entryName: " + entryName);
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }

        //bar / 90_contents / {OData_collection} / 90_data / {entity} / {1.json}
        String fileName = null;
        pattern = Pattern.compile(".*/([^/]+)$");
        m = pattern.matcher(odataPath);
        if (m.matches()) {
            fileName = m.replaceAll("$1");
        }
        if (fileName != null) {
            pattern = Pattern.compile("^([0-9]+).json$");
            m = pattern.matcher(fileName);
            if (!m.matches()) {
                //bar / 90_contents / {OData_collection} / {dir} / is an error
                String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2001");
                log.info(message + "entryName: " + entryName);
                writeOutputStream(true, "PL-BI-1004", entryName, message);
                return false;
            }
        }

        return true;
    }

    /**
     * 10_relations.json, 20_roles.json, 30_extroles.json, 70_ $ links.json, 10_odatarelations.json validation check.
     * @param jp Json Perth
     * @param mapper ObjectMapper
     * @param jsonName file name
     * @throws IOException IOException
     */
    protected void registJsonEntityData(JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
        JsonToken token;
        token = jp.nextToken();

        //Check Relations, Roles, ExtRoles, $ links
        checkMatchFieldName(jp, jsonName);

        token = jp.nextToken();
        //If it is not an array, an error
        if (token != JsonToken.START_ARRAY) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        token = jp.nextToken();

        while (jp.hasCurrentToken()) {
            if (token == JsonToken.END_ARRAY) {
                break;
            } else if (token != JsonToken.START_OBJECT) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }

            //1 item registration processing
            IJSONMappedObject mappedObject = barFileJsonValidate(jp, mapper, jsonName);
            if (jsonName.equals(RELATION_JSON)) {
                createRelation(mappedObject.getJson());
            } else if (jsonName.equals(ROLE_JSON)) {
                createRole(mappedObject.getJson());
            } else if (jsonName.equals(EXTROLE_JSON)) {
                createExtRole(mappedObject.getJson());
            } else if (jsonName.equals(LINKS_JSON)) {
                createLinks(mappedObject, odataProducer);
            } else if (jsonName.equals(RULE_JSON)) {
                createRules(mappedObject.getJson());
            }

            token = jp.nextToken();
        }
    }

    /**
     * Validate check of required items.
     * @param jp Json parser
     * @param mapper ObjectMapper
     * @param jsonName file name
     * @throws IOException IOException
     * @return JSONMappedObject JSONMapped object
     */
    protected IJSONMappedObject barFileJsonValidate(
            JsonParser jp, ObjectMapper mapper, String jsonName) throws IOException {
        if (jsonName.equals(EXTROLE_JSON)) {
            JSONExtRole extRoles = mapper.readValue(jp, JSONExtRole.class);
            if (extRoles.getExtRole() == null) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            if (extRoles.getRelationName() == null) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return extRoles;
        } else if (jsonName.equals(ROLE_JSON)) {
            JSONRole roles = mapper.readValue(jp, JSONRole.class);
            if (roles.getName() == null) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return roles;
        } else if (jsonName.equals(RELATION_JSON)) {
            JSONRelation relations = mapper.readValue(jp, JSONRelation.class);
            if (relations.getName() == null) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return relations;
        } else if (jsonName.equals(LINKS_JSON)) {
            JSONLink links = mapper.readValue(jp, JSONLink.class);
            linksJsonValidate(jsonName, links);
            return links;
        } else if (jsonName.equals(USERDATA_LINKS_JSON)) {
            JSONUserDataLink links = mapper.readValue(jp, JSONUserDataLink.class);
            userDataLinksJsonValidate(jsonName, links);
            return links;
        } else if (jsonName.equals(RULE_JSON)) {
            JSONRule rules = mapper.readValue(jp, JSONRule.class);
            if (rules.getAction() == null) { //TODO What else?
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
            return rules;
        }
        return null;
    }

    /**
     * Validate of 70 _ $ links.json.
     * @param jsonName JSON filename
     * @param links Read JSON object
     */
    private void linksJsonValidate(String jsonName, JSONLink links) {
        if (links.getFromType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            if (!links.getFromType().equals(Relation.EDM_TYPE_NAME)
                    && !links.getFromType().equals(Role.EDM_TYPE_NAME)
                    && !links.getFromType().equals(ExtRole.EDM_TYPE_NAME)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        }
        if (links.getFromName() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> fromNameMap = links.getFromName();
            for (Map.Entry<String, String> entry : fromNameMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
        if (links.getToType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            if (!links.getToType().equals(Relation.EDM_TYPE_NAME)
                    && !links.getToType().equals(Role.EDM_TYPE_NAME)
                    && !links.getToType().equals(ExtRole.EDM_TYPE_NAME)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        }
        if (links.getToName() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> toNameMap = links.getToName();
            for (Map.Entry<String, String> entry : toNameMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
    }

    /**
     * Validation of 10_odatarelations.json.
     * @param jsonName JSON filename
     * @param links Read JSON object
     */
    private void userDataLinksJsonValidate(String jsonName, JSONUserDataLink links) {
        if (links.getFromType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        if (links.getFromId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> fromIdMap = links.getFromId();
            for (Map.Entry<String, String> entry : fromIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
        if (links.getToType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        if (links.getToId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        } else {
            Map<String, String> toIdMap = links.getToId();
            for (Map.Entry<String, String> entry : toIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
                }
            }
        }
    }

    /**
     * Validate of manifest.json.
     * @param jp Json parser
     * @param mapper ObjectMapper
     * @return JSONManifest object
     * @throws IOException if data reading failed
     */
    protected JSONManifest manifestJsonValidate(JsonParser jp, ObjectMapper mapper) throws IOException {
        //Version check of TODO BAR file
        JSONManifest manifest = null;
        try {
            manifest = mapper.readValue(jp, JSONManifest.class);
        } catch (UnrecognizedPropertyException ex) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(
                    "manifest.json unrecognized property");
        }
        if (manifest.getBarVersion() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#barVersion");
        }
        if (manifest.getBoxVersion() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#boxVersion");
        }
        if (manifest.getOldDefaultPath() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params("manifest.json#DefaultPath");
        }
        return manifest;
    }

    /**
     * Confirm that the field name matches the format of the file.
     * @param jp
     * @param jsonName
     * @throws IOException
     * @throws JsonParseException
     */
    private void checkMatchFieldName(JsonParser jp, String jsonName) throws IOException {
        String fieldName = jp.getCurrentName();
        if (!(fieldName.equals("Relations") && jsonName.equals(RELATION_JSON))
                && !(fieldName.equals("Roles") && jsonName.equals(ROLE_JSON))
                && !(fieldName.equals("ExtRoles") && jsonName.equals(EXTROLE_JSON))
                && !(fieldName.equals("Rules") && jsonName.equals(RULE_JSON))
                && !(fieldName.equals("Links") && jsonName.equals(LINKS_JSON))
                && !(fieldName.equals("Links") && jsonName.equals(USERDATA_LINKS_JSON))) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
    }

    /**
     * Output of Http response message.
     * @param isError Specify true on error, false otherwise.
     * @param code
     * Message code (message code defined in personium-messages.properties)
     * @param path
     * Process target resource path (ex. /Bar/meta/roles.json)
     */
    private void writeOutputStream(boolean isError, String code, String path) {
        writeOutputStream(isError, code, path, "");
    }

    /**
     * bar File output of installation log details.
     * @param isError Specify true on error, false otherwise.
     * @param code
     * Message code (message code defined in personium-messages.properties)
     * @param path
     * Process target resource path (ex. /Bar/meta/roles.json)
     * @param detail
     * Detailed information on processing failure (PL-BI-2xxx)
     */
    private void writeOutputStream(boolean isError, String code, String path, String detail) {
        String message = PersoniumCoreMessageUtils.getMessage(code);
        if (detail == null) {
            message = message.replace("{0}", "");
        } else {
            message = message.replace("{0}", detail);
        }
        outputEventBus(isError, code, path, message);

        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

    /**
     * Output the installation processing status to EventBus as an internal event.
     * @param isError Specify true on error, false otherwise.
     * @param code processing code (ex. PL - BI - 0000)
     * @param path bar Entry path in the file (in the case of Edmx, the path of OData)
     * @param message Output message
     */
    @SuppressWarnings("unchecked")
    private void outputEventBus(boolean isError, String code, String path, String message) {
        if (eventBuilder != null) {
            PersoniumEvent event = eventBuilder
                    .type(code)
                    .object(path)
                    .info(message)
                    .build();
            eventBus.post(event);
        }
        if (this.progressInfo != null && isError) {
            JSONObject messageJson = new JSONObject();
            JSONObject messageDetail = new JSONObject();
            messageJson.put("code", code);
            messageJson.put("message", messageDetail);
            messageDetail.put("lang", "en");
            messageDetail.put("value", message);
            this.progressInfo.setMessage(messageJson);
            writeToProgressCache(true);
        } else {
            writeToProgressCache(false);
        }
    }

    /**
     * Output bar installation status to cache.
     * @param forceOutput Specify true to forcibly output, false otherwise
     */
    private void writeToProgressCache(boolean forceOutput) {
        if (this.progressInfo != null && this.progressInfo.isOutputEventBus() || forceOutput) {
            String key = "box-" + this.box.getId();
            Progress progress = new Progress(key, progressInfo.toString());
            ProgressManager.putProgress(key, progress);
            log.info("Progress(" + key + "): " + progressInfo.toString());
        }
    }

    private void setupBarFileOrder() {
        barFileOrder = new LinkedHashMap<String, Boolean>();
        barFileOrder.put("bar/00_meta/00_manifest.json", true);
        barFileOrder.put("bar/00_meta/10_relations.json", false);
        barFileOrder.put("bar/00_meta/20_roles.json", false);
        barFileOrder.put("bar/00_meta/30_extroles.json", false);
        barFileOrder.put("bar/00_meta/50_rules.json", false);
        barFileOrder.put("bar/00_meta/70_$links.json", false);
        barFileOrder.put("bar/00_meta/90_rootprops.xml", true);
        barFileOrder.put("bar/90_contents/", false); // dummy
    }

    private String createdBoxEtag = "";
    private String createdBoxName = "";

    /**
     * Returns the box name of the created Box.
     * @return Box name
     */
    public String getCreatedBoxName() {
        return createdBoxName;
    }

    /**
     * Returns the ETag of the created Box.
     * @return ETag
     */
    public String getCreatedBoxETag() {
        return createdBoxEtag;
    }

    // Post event to EventBus
    private void postCellCtlCreateEvent(EntityResponse res) {
        String name = res.getEntity().getEntitySetName();
        String keyString = AbstractODataResource.replaceDummyKeyToNull(res.getEntity().getEntityKey().toKeyString());
        String object = new StringBuilder(SCHEME_LOCALCELL)
                .append(":/__ctl/").append(name).append(keyString).toString();
        String info = "box install";
        String type = PersoniumEventType.cellctl(name, PersoniumEventType.Operation.CREATE);
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .requestKey(this.requestKey)
                .build();
        EventBus bus = this.cell.getEventBus();
        bus.post(ev);
    }

    /**
     * Register Box information in ES.
     * @param json JSON object read from JSON file
     */
    @SuppressWarnings("unchecked")
    void createBox(JSONObject json) {
        if (boxName == null || boxName.isEmpty()) {
            this.createdBoxName = (String) json.get("Name");
        } else {
            json.put("Name", boxName);
            this.createdBoxName = boxName;
        }
        StringReader stringReader = new StringReader(json.toJSONString());

        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        //Register Box
        EntityResponse res = odataProducer.
                createEntity(entitySetName, oew);
        this.createdBoxEtag = oew.getEtag();

        //Register Dav
        Box newBox = new Box(odataEntityResource.getAccessContext().getCell(), oew);
        this.boxCmp = ModelFactory.boxCmp(newBox);

        this.box = newBox;

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * Register the Relation information defined in 10 _ $ relations.json to the ES.
     * @param json JSON object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createRelation(JSONObject json) {
        json.put("_Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(Relation.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        //Registration of Relation
        EntityResponse res = odataProducer.
                createEntity(Relation.EDM_TYPE_NAME, oew);

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * Register the Role information defined in 20 _ $ roles.json to the ES.
     * @param json JSON object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createRole(JSONObject json) {
        json.put("_Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(Role.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        //Register Role
        EntityResponse res = odataProducer.
                createEntity(Role.EDM_TYPE_NAME, oew);

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * Register ExtRole information defined in 30 _ $ extroles.json in the ES.
     * @param json JSON object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createExtRole(JSONObject json) {
        String url = (String) json.get(ExtRole.EDM_TYPE_NAME);
        json.put(ExtRole.EDM_TYPE_NAME, url);
        json.put("_Relation._Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());

        odataEntityResource.setEntitySetName(ExtRole.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        //Register ExtRole
        EntityResponse res = odataProducer.
                createEntity(ExtRole.EDM_TYPE_NAME, oew);

        // post event
        postCellCtlCreateEvent(res);
    }

    @SuppressWarnings("unchecked")
    private void createRules(JSONObject json) {
        log.debug("createRules: " + json.toString());
        json.put("_Box.Name", createdBoxName);
        StringReader stringReader = new StringReader(json.toJSONString());
        //Register Rule to Entity and also register to RuleManager with afterCreate
        odataEntityResource.setEntitySetName(Rule.EDM_TYPE_NAME);
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());
        //Register Rule
        EntityResponse res = odataProducer.
                createEntity(Rule.EDM_TYPE_NAME, oew);

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * Register the link information defined in 70 _ $ links.json to the ES.
     * @param mappedObject Object read from JSON file
     */
    private void createLinks(IJSONMappedObject mappedObject, PersoniumODataProducer producer) {
        Map<String, String> fromNameMap = ((JSONLink) mappedObject).getFromName();
        String fromkey =
                BarFileUtils.getComplexKeyName(((JSONLink) mappedObject).getFromType(), fromNameMap, this.boxName);
        OEntityKey fromOEKey = OEntityKey.parse(fromkey);
        OEntityId sourceEntity = OEntityIds.create(((JSONLink) mappedObject).getFromType(), fromOEKey);
        String targetNavProp = ((JSONLink) mappedObject).getNavPropToType();

        //Link creation preprocessing
        odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

        Map<String, String> toNameMap = ((JSONLink) mappedObject).getToName();
        String tokey =
                BarFileUtils.getComplexKeyName(((JSONLink) mappedObject).getToType(), toNameMap, this.boxName);
        OEntityKey toOEKey = OEntityKey.parse(tokey);
        OEntityId newTargetEntity = OEntityIds.create(((JSONLink) mappedObject).getToType(), toOEKey);
        //Register $ links
        producer.createLink(sourceEntity, targetNavProp, newTargetEntity);

        // post event
        String keyString = AbstractODataResource.replaceDummyKeyToNull(fromOEKey.toString());
        String targetKeyString = AbstractODataResource.replaceDummyKeyToNull(toOEKey.toString());
        String object = new StringBuilder(SCHEME_LOCALCELL)
                .append(":/__ctl/")
                .append(sourceEntity.getEntitySetName())
                .append(keyString)
                .append("/$links/")
                .append(targetNavProp)
                .append(targetKeyString)
                .toString();
        String info = "box install";
        String type = PersoniumEventType.cellctlLink(
                sourceEntity.getEntitySetName(),
                newTargetEntity.getEntitySetName(),
                PersoniumEventType.Operation.CREATE);
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .requestKey(this.requestKey)
                .build();
        EventBus bus = this.cell.getEventBus();
        bus.post(ev);
    }

    private boolean createUserdataLinks(PersoniumODataProducer producer, List<IJSONMappedObject> userDataLinks) {
        int linkSize = userDataLinks.size();
        int linkCount = 0;
        String message = PersoniumCoreMessageUtils.getMessage("PL-BI-1002");
        for (IJSONMappedObject json : userDataLinks) {
            linkCount++;
            if (!createUserdataLink(json, producer)) {
                return false;
            }
            if (linkCount % linksOutputStreamSize == 0) {
                writeOutputStream(false, "PL-BI-1002",
                        String.format("userDataLinks %d / %d", linkCount, linkSize), message);
            }
        }
        writeOutputStream(false, "PL-BI-1002",
                String.format("userDataLinks %d / %d", linkCount, linkSize), message);
        return true;
    }

    /**
     * Register the link information defined in 10_odatarelations.json to the ES.
     * @param mappedObject Object read from JSON file
     */
    private boolean createUserdataLink(IJSONMappedObject mappedObject, PersoniumODataProducer producer) {
        OEntityId sourceEntity = null;
        OEntityId newTargetEntity = null;
        try {
            Map<String, String> fromId = ((JSONUserDataLink) mappedObject).getFromId();
            String fromKey = "";
            Iterator<Entry<String, String>> fromIterator = fromId.entrySet().iterator();
            fromKey = String.format("('%s')", fromIterator.next().getValue());

            OEntityKey fromOEKey = OEntityKey.parse(fromKey);
            sourceEntity = OEntityIds.create(((JSONUserDataLink) mappedObject).getFromType(), fromOEKey);
            String targetNavProp = ((JSONUserDataLink) mappedObject).getNavPropToType();

            //Link creation preprocessing
            odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toId = ((JSONUserDataLink) mappedObject).getToId();
            String toKey = "";
            Iterator<Entry<String, String>> toIterator = toId.entrySet().iterator();
            toKey = String.format("('%s')", toIterator.next().getValue());

            OEntityKey toOEKey = OEntityKey.parse(toKey);
            newTargetEntity = OEntityIds.create(((JSONUserDataLink) mappedObject).getToType(), toOEKey);

            //Register $ links
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        } catch (Exception e) {
            String path = "";
            String targetPath = "";
            if (sourceEntity != null) {
                path = sourceEntity.getEntitySetName() + sourceEntity.getEntityKey();
            }
            if (newTargetEntity != null) {
                targetPath = "Target Link to " + newTargetEntity.getEntitySetName() + newTargetEntity.getEntityKey();
            }
            log.info(e.getMessage() + " [" + path + "]", e.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1004", path, targetPath);
            return false;
        }
        return true;
    }

    private void createCollection(String collectionUrl,
            String entryName,
            Cell parentCell,
            Box parentBox,
            int collectionType,
            Element aclElement,
            List<Element> propElements) {
        int index;
        if (parentCell == null || parentBox == null) {
            return;
        }

        String type = "";
        switch (collectionType) {
        case TYPE_WEBDAV_COLLECTION:
            type = DavCmp.TYPE_COL_WEBDAV;
            break;
        case TYPE_ODATA_COLLECTION:
            type = DavCmp.TYPE_COL_ODATA;
            break;
        case TYPE_SERVICE_COLLECTION:
            type = DavCmp.TYPE_COL_SVC;
            break;
        default:
            break;
        }

        String parenEntryName = "";
        DavCmp parentCmp = null;
        String tmp = entryName.replace(CONTENTS_DIR, "/");
        String[] slash = tmp.split("/");
        if (slash.length == 2) {
            parentCmp = this.boxCmp;
        } else {
            index = entryName.lastIndexOf("/", entryName.length() - 2);
            parenEntryName = entryName.substring(0, index + 1);
            parentCmp = this.davCmpMap.get(parenEntryName);
            if (parentCmp == null) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_ODATA)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
            } else if (parentCmp.getType().equals(DavCmp.TYPE_COL_SVC)) {
                String crrName = entryName.substring(index + 1, entryName.length() - 1);
                if (!"__src".equals(crrName)) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(entryName);
                }
            }
        }
  //      String parentId = parentCmp.getId();

//// check number of hierarchies in collection
//        DavCmp current = parentCmp;
//
//// Since current already points to a parent, the initial value of depth is 1
//        int depth = 1;
//        int maxDepth = PersoniumCoreConfig.getMaxCollectionDepth();
//        while (null != current.getParent()) {
//            current = (DavCmp) current.getParent();
//            depth++;
//        }
//        if (depth > maxDepth) {
/// / Since it exceeds the limit of the number of collections, it is set to 400 error
//            throw PersoniumCoreException.Dav.COLLECTION_DEPTH_ERROR;
//        }
//
/// / Check number of collection files in parent collection
//        int maxChildResource = PersoniumCoreConfig.getMaxChildResourceCount();
//        if (parentCmp.getChildrenCount() >= maxChildResource) {
/// / Since it exceeded the limit of the number of collection files that can be created in the collection, it is set to 400 error
//            throw PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
//        }


//// Add pointer to parent node
        String collectionName = "";
        index = collectionUrl.lastIndexOf("/");
        collectionName = collectionUrl.substring(index + 1);

        DavCmp collectionCmp = parentCmp.getChild(collectionName);
        collectionCmp.mkcol(type);

        this.davCmpMap.put(entryName, collectionCmp);

        //ACL registration
        if (aclElement != null) {
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(CommonUtils.nodeToString(aclElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            collectionCmp.acl(aclXml);
        }

        //PROPPATCH registration
        registProppatch(collectionCmp, propElements, collectionUrl);
    }

    /**
     * Register ACL and PROPPATCH information in Box.
     * @param targetBox box
     * @param aclElement ACL
     * @param propElements What to set with PROPATCH
     * URL of @param boxUrl box
     */
    private void registBoxAclAndProppatch(Box targetBox, Element aclElement,
            List<Element> propElements, String boxUrl) {
        if (targetBox == null || boxCmp == null) {
            return;
        }

        //ACL registration
        if (aclElement != null) {
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(CommonUtils.nodeToString(aclElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            boxCmp.acl(aclXml);
        }

        //PROPPATCH registration
        registProppatch(boxCmp, propElements, boxUrl);
    }

    private void registProppatch(DavCmp davCmp, List<Element> propElements, String boxUrl) {
        if (!propElements.isEmpty()) {
            Reader propXml = getProppatchXml(propElements);
            try {
                Propertyupdate propUpdate = Propertyupdate.unmarshal(propXml);
                davCmp.proppatch(propUpdate, boxUrl);
            } catch (IOException ex) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(ex);
            }
        }
    }

    /**
     * Get Map <key, DavCmpEsImpl> of the collection defined in the bar file.
     * @return collection MapDavCmpEsImpl object
     */
    private Map<String, DavCmp> getCollections(String colType) {
        Map<String, DavCmp> map = new HashMap<String, DavCmp>();
        Set<String> keySet = davCmpMap.keySet();
        for (String key : keySet) {
            DavCmp davCmp = davCmpMap.get(key);
            if (davCmp != null && colType.equals(davCmp.getType())) {
                map.put(key, davCmp);
            }
        }
        return map;
    }

    /**
     * Get Map <key, DavCmpEsImpl> of the collection defined in the bar file.
     * @param entryName entry name
     * @param collections Collection's Map object
     * @return collection MapDavCmpEsImpl object
     */
    private DavCmp getCollection(String entryName, Map<String, DavCmp> collections) {
        int pos = entryName.lastIndexOf("/");
        if (pos == entryName.length() - 1) {
            return collections.get(entryName);
        }
        String colName = entryName.substring(0, pos + 1);
        return collections.get(colName);
    }

    /**
     * Analyzes 00_ $ metadata_xml to register the user schema.
     * @param entryName entry name
     * @param inputStream Input stream
     * @param davCmp Collection Operation object
     * @return true if successful
     */
    protected boolean registUserSchema(String entryName, InputStream inputStream, DavCmp davCmp) {
        EdmDataServices metadata = null;
        //If you pass InputStream to the XML parser (StAX, SAX, DOM) as is, the file list acquisition processing
        //Because it will be interrupted, store it as a provisional countermeasure and then parse it
        try {
            InputStreamReader isr = new InputStreamReader(new CloseShieldInputStream(inputStream));
            //Load 00_ $ metadata.xml and register user schema
            XMLFactoryProvider2 provider = StaxXMLFactoryProvider2.getInstance();
            XMLInputFactory2 factory = provider.newXMLInputFactory2();
            XMLEventReader2 reader = factory.createXMLEventReader(isr);
            PersoniumEdmxFormatParser parser = new PersoniumEdmxFormatParser();
            metadata = parser.parseMetadata(reader);
        } catch (Exception ex) {
            log.info("XMLParseException: " + ex.getMessage(), ex.fillInStackTrace());
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        } catch (StackOverflowError tw) {
            //StackOverFlowError occurs when circular reference of ComplexType is made
            log.info("XMLParseException: " + tw.getMessage(), tw.fillInStackTrace());
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2002");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        //Entity / Property registration
        //As Property / ComplexProperty sometimes uses ComplexType as the data type,
        //Register ComplexType at the very beginning, then register EntityType
        // PersoniumODataProducer producer = davCmp.getODataProducer();
        try {
            createComplexTypes(metadata, davCmp);
            createEntityTypes(metadata, davCmp);
            createAssociations(metadata, davCmp);
        } catch (PersoniumCoreException e) {
            writeOutputStream(true, "PL-BI-1004", entryName, e.getMessage());
            log.info("PersoniumCoreException: " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.info("Regist Entity Error: " + e.getMessage(), e.fillInStackTrace());
            String message = PersoniumCoreMessageUtils.getMessage("PL-BI-2003");
            writeOutputStream(true, "PL-BI-1004", entryName, message);
            return false;
        }
        return true;
    }

    /**
     * Register EntityType / Property defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    @SuppressWarnings("unchecked")
    protected void createEntityTypes(EdmDataServices metadata, DavCmp davCmp) {
        //Since DeclaredProperty is associated with EntityType, Property is registered for each EntityType
        Iterable<EdmEntityType> entityTypes = metadata.getEntityTypes();
        UserSchemaODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmEntityType entity : entityTypes) {
            log.debug("EntityType: " + entity.getName());
            if (producer == null) {
                producer = (UserSchemaODataProducer) davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            Map<String, String> entityTypeIds = producer.getEntityTypeIds();
            odataEntityResource.setEntitySetName(EntityType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", entity.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            //Register EntityType
            String path = String.format("/%s/%s/%s/EntityType('%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(), entity.getName());
            writeOutputStream(false, "PL-BI-1002", path);
            EntityResponse response = producer.createEntity(EntityType.EDM_TYPE_NAME, oew);
            OEntityWrapper entityResponse = (OEntityWrapper) response.getEntity();
            entityTypeIds.put(entity.getName(), entityResponse.getUuid());
        }
        //Registration of Property associated with EntityType
        for (EdmEntityType entity : entityTypes) {
            createProperties(entity, davCmp, producer);
        }
    }

    /**
     * Register Property / ComplexTypeProperty defined in Edmx.
     * @param entity EntityType / ComplexType object in which the Property to be registered is defined
     * @param davCmp Collection Operation object
     * @param producer OData producer
     */
    @SuppressWarnings("unchecked")
    protected void createProperties(EdmStructuralType entity, DavCmp davCmp, PersoniumODataProducer producer) {
        Iterable<EdmProperty> properties = entity.getDeclaredProperties();
        EdmDataServices userMetadata = null;
        String edmTypeName = Property.EDM_TYPE_NAME;
        if (entity instanceof EdmComplexType) {
            edmTypeName = ComplexTypeProperty.EDM_TYPE_NAME;
        }
        for (EdmProperty property : properties) {
            String name = property.getName();
            log.debug(edmTypeName + ": " + name);
            if (name.startsWith("_")) {
                continue;
            }
            if (userMetadata == null) {
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                odataEntityResource.setEntitySetName(edmTypeName);
            }
            CollectionKind kind = property.getCollectionKind();
            if (kind != null && !kind.equals(CollectionKind.NONE) && !kind.equals(CollectionKind.List)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(METADATA_XML);
            }
            JSONObject json = new JSONObject();
            json.put("Name", property.getName());
            if (entity instanceof EdmComplexType) {
                json.put("_ComplexType.Name", entity.getName());
            } else {
                json.put("_EntityType.Name", entity.getName());
                json.put("IsKey", false); //It is necessary to set it when Iskey is supported
                json.put("UniqueKey", null); //It is necessary to set it when UniqueKey is supported
            }
            String typeName = property.getType().getFullyQualifiedTypeName();
            if (!property.getType().isSimple() && typeName.startsWith("UserData.")) {
                typeName = typeName.replace("UserData.", "");
            }
            json.put("Type", typeName);
            json.put("Nullable", property.isNullable());
            json.put("DefaultValue", property.getDefaultValue());
            json.put("CollectionKind", toStringFromCollectionKind(kind));
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            //Register ComplexTypeProperty
            producer.createEntity(edmTypeName, oew);
        }
    }

    /**
     * Register AssociationEnd and link information defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    protected void createAssociations(EdmDataServices metadata, DavCmp davCmp) {
        Iterable<EdmAssociation> associations = metadata.getAssociations();
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmAssociation association : associations) {
            //Based on the Association information, link between AssociationEnd and AssociationEnd is registered
            String name = association.getName();
            log.debug("Association: " + name);
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                odataEntityResource.setEntitySetName(AssociationEnd.EDM_TYPE_NAME);
            }
            String path = String.format("/%s/%s/%s/Association('%s','%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(),
                    association.getEnd1().getRole(), association.getEnd2().getRole());
            writeOutputStream(false, "PL-BI-1002", path);
            //AssociationEnd registration
            EdmAssociationEnd ae1 = association.getEnd1();
            String realRoleName1 = getRealRoleName(ae1.getRole());
            createAssociationEnd(producer, userMetadata, ae1, realRoleName1);
            EdmAssociationEnd ae2 = association.getEnd2();
            String realRoleName2 = getRealRoleName(ae2.getRole());
            createAssociationEnd(producer, userMetadata, ae2, realRoleName2);

            //$ Links registration between AssociationEnd
            String fromkey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName1, ae1.getType().getName());
            OEntityKey fromOEKey = OEntityKey.parse(fromkey);
            OEntityId sourceEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, fromOEKey);
            String targetNavProp = "_" + AssociationEnd.EDM_TYPE_NAME;
            odataEntityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);
            String tokey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName2, ae2.getType().getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, toOEKey);
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        }
    }

    /*
     * Split the input string ("entity type name: role name") with a colon and return the character string after the colon
     * Throw an exception if the colon is not included in the string
     * @param sourceRoleName The name of the source role ("entity type name: role name")
     * @return Actual role name
     */
    private String getRealRoleName(String sourceRoleName) {
        String[] tokens = sourceRoleName.split(":");
        if (tokens.length != 2) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        if (tokens[0].length() <= 0 || tokens[1].length() <= 0) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(sourceRoleName);
        }
        return tokens[1];
    }

    /**
     * Register the AssociationEnd passed as an argument.
     * @param producer Entity PersoniumODataProcucer object for registration
     * @param userMetadata User defined schema object
     * @param associationEnd AssociationEnd object for registration
     * @param associationEndName AssociationEnd name
     */
    @SuppressWarnings("unchecked")
    protected void createAssociationEnd(PersoniumODataProducer producer,
            EdmDataServices userMetadata, EdmAssociationEnd associationEnd, String associationEndName) {
        //AssociationEnd's name uses the role name of AssociationEnd
        JSONObject json = new JSONObject();
        String entityTypeName = associationEnd.getType().getName();
        json.put(AssociationEnd.P_ASSOCIATION_NAME.getName(), associationEndName);
        json.put(AssociationEnd.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        json.put(AssociationEnd.P_MULTIPLICITY.getName(), associationEnd.getMultiplicity().getSymbolString());
        StringReader stringReader = new StringReader(json.toJSONString());
        OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                odataEntityResource.getOdataResource(), userMetadata);
        producer.createEntity(AssociationEnd.EDM_TYPE_NAME, oew);
    }

    /**
     * Register ComplexType / ComplexTypeProperty defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    @SuppressWarnings("unchecked")
    protected void createComplexTypes(EdmDataServices metadata, DavCmp davCmp) {
        //Since DeclaredProperty is associated with ComplexType, ComplexTypeProperty is registered for each ComplexType
        Iterable<EdmComplexType> complexTypes = metadata.getComplexTypes();
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmComplexType complexType : complexTypes) {
            log.debug("ComplexType: " + complexType.getName());
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(this.cell);
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            odataEntityResource.setEntitySetName(ComplexType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", complexType.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = odataEntityResource.getOEntityWrapper(stringReader,
                    odataEntityResource.getOdataResource(), userMetadata);
            //Register ComplexType
            String path = String.format("/%s/%s/%s/ComplexType('%s')",
                    this.cell.getName(), this.boxName, davCmp.getName(), complexType.getName());
            writeOutputStream(false, "PL-BI-1002", path);
            producer.createEntity(ComplexType.EDM_TYPE_NAME, oew);
        }

        //Register ComplexTypeProperty associated with ComplexType
        for (EdmComplexType complexType : complexTypes) {
            createProperties(complexType, davCmp, producer);
        }
    }

    private Reader getProppatchXml(List<Element> propElements) {
        StringBuffer sbPropXml = new StringBuffer();
        sbPropXml.append("<D:propertyupdate xmlns:D=\"DAV:\"");
        sbPropXml.append(" xmlns:p=\"urn:x-personium:xmlns\"");
        sbPropXml.append(" xmlns:Z=\"http://www.w3.com/standards/z39.50/\">");
        sbPropXml.append("<D:set>");
        sbPropXml.append("<D:prop>");
        for (Element element : propElements) {
            sbPropXml.append(CommonUtils.nodeToString(element));
        }
        sbPropXml.append("</D:prop>");
        sbPropXml.append("</D:set>");
        sbPropXml.append("</D:propertyupdate>");
        Reader propXml = new StringReader(sbPropXml.toString());
        return propXml;
    }

    /**
     * @param entryCount the entryCount to set
     */
    void setEntryCount(long entryCount) {
        this.progressInfo = new BarInstallProgressInfo(this.cell.getId(), this.box.getId(), entryCount);
    }

    /**
     * Record the information that started processing in the cache.
     */
    public void writeInitProgressCache() {
        setEventBus();
        writeOutputStream(false, "PL-BI-1000", SCHEME_LOCALCELL + ":/" + boxName, "");
        writeToProgressCache(true);
    }

    /**
     * Record error information in the cache.
     */
    public void writeErrorProgressCache() {
        if (this.progressInfo != null) {
            this.progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            this.progressInfo.setEndTime();
            writeToProgressCache(true);
        }
    }

    /**
     * Enum collection kind to string.
     * @param collectionKind enum collection kind
     * @return None or List
     */
    private String toStringFromCollectionKind(CollectionKind collectionKind) {
        if (CollectionKind.List.equals(collectionKind)) {
            return Property.COLLECTION_KIND_LIST;
        } else {
            return Property.COLLECTION_KIND_NONE;
        }
    }
}

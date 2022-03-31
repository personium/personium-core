/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.wink.webdav.model.Propertyupdate;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.bar.jackson.JSONUserDataLink;
import io.personium.core.bar.jackson.JSONUserDataLinks;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.Box;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumEdmxFormatParser;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataEntitiesResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.rs.odata.ODataResource;

/**
 * FileVisitor for install contents recursively from zip file to file system.
 */
public class BarFileContentsInstallVisitor implements FileVisitor<Path> {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(BarFileContentsInstallVisitor.class);

    private static final int TYPE_WEBDAV_COLLECTION = 0;
    private static final int TYPE_ODATA_COLLECTION = 1;
    private static final int TYPE_SERVICE_COLLECTION = 2;
    private static final int TYPE_DAV_FILE = 3;
    private static final int TYPE_SVC_FILE = 4;

    /** Install target box. */
    private Box box;
    /** Progress info. */
    private BarInstallProgressInfo progressInfo;
    /** OData entity resource. */
    private ODataEntityResource entityResource;
    /** Personium event bus for sending event. */
    private EventBus eventBus;
    /** Personium event builder. */
    private PersoniumEvent.Builder eventBuilder;
    /** Map of dav file content-type to register. */
    private Map<String, String> davFileContentTypeMap;
    /** Map of dav file acl to register. */
    private Map<String, Element> davFileAclMap;
    /** Map of dav file property to register. */
    private Map<String, List<Element>> davFilePropsMap;
    /** Map of OData collection to register. */
    private Map<String, DavCmp> odataCollectionMap;
    /** Map of WebDAV collection to register. */
    private Map<String, DavCmp> webdavCollectionMap;
    /** Map of service collection to register. */
    private Map<String, DavCmp> serviceCollectionMap;

    private String currentPath = null;
    private int userDataCount = 0;
    private JSONUserDataLinks userDataLinks = new JSONUserDataLinks();
    private LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();
    private Map<String, String> fileNameMap = new HashMap<String, String>();
    private DavCmp currentDavCmp = null;
    private List<String> doneKeys = new ArrayList<String>();

    /**
     * Constructor.
     * @param box Install target box
     * @param progressInfo Progress info
     * @param entityResource OData entity resource
     * @param eventBus Personium event bus for sending event
     * @param eventBuilder Personium event builder
     * @param davCollectionMap Map of dav collection to register
     * @param davFileContentTypeMap Map of dav file content-type to register
     * @param davFileAclMap Map of dav file acl to register
     * @param davFilePropsMap Map of dav file property to register
     */
    public BarFileContentsInstallVisitor(Box box,
            BarInstallProgressInfo progressInfo,
            ODataEntityResource entityResource,
            EventBus eventBus,
            PersoniumEvent.Builder eventBuilder,
            Map<String, DavCmp> davCollectionMap,
            Map<String, String> davFileContentTypeMap,
            Map<String, Element> davFileAclMap,
            Map<String, List<Element>> davFilePropsMap) {
        this.box = box;
        this.progressInfo = progressInfo;
        this.entityResource = entityResource;
        this.eventBus = eventBus;
        this.eventBuilder = eventBuilder;
        this.davFileContentTypeMap = davFileContentTypeMap;
        this.davFileAclMap = davFileAclMap;
        this.davFilePropsMap = davFilePropsMap;
        // Create a map for each collection type.
        odataCollectionMap = getCollections(davCollectionMap, DavCmp.TYPE_COL_ODATA);
        webdavCollectionMap = getCollections(davCollectionMap, DavCmp.TYPE_COL_WEBDAV);
        // Since it may be referred to as parent, Box must be registered.
        webdavCollectionMap.putAll(getCollections(davCollectionMap, DavCmp.TYPE_COL_BOX));
        serviceCollectionMap = getCollections(davCollectionMap, DavCmp.TYPE_COL_SVC);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return createContents(dir, true);
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        progressInfo.addDelta(1L);
        return createContents(file, false);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.info("IOException: " + exc.getMessage(), exc.fillInStackTrace());
        throw PersoniumBarException.INSTALLATION_FAILED.path(file.toString()).detail(exc.getMessage());
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    /**
     * Get Map <key, DavCmpEsImpl> of the collection defined in the bar file.
     * @return collection MapDavCmpEsImpl object
     */
    private Map<String, DavCmp> getCollections(Map<String, DavCmp> davCollectionMap, String colType) {
        Map<String, DavCmp> map = new HashMap<String, DavCmp>();
        Set<String> keySet = davCollectionMap.keySet();
        for (String key : keySet) {
            DavCmp davCmp = davCollectionMap.get(key);
            if (davCmp != null && colType.equals(davCmp.getType())) {
                map.put(key, davCmp);
            }
        }
        return map;
    }

    /**
     * Read one content data (bar/90_contents) in the bar file and register it.
     * @param pathInZip File path obj in zip
     * @return boolean Processing result true:success false:failure
     */
    private FileVisitResult createContents(Path pathInZip, boolean isDirectory) {
        try {
            String entryName = pathInZip.toString();
            if (entryName.startsWith("/")) {
                entryName = entryName.replaceFirst("/", "");
            }
            if (isDirectory && !entryName.endsWith("/")) {
                // workaround for avoid incompatibility which trailing slash is not presented in directory
                entryName = entryName + "/";
            }
            log.debug("Entry Name: " + entryName);
            log.debug("Entry Size: " + Files.size(pathInZip));
            log.debug("Entry isDirectory: " + isDirectory);
            if (BarFile.CONTENTS_DIR.equals(entryName)) {
                // Do not need to process content root directory.
                return FileVisitResult.CONTINUE;
            }

            writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, entryName);

            //When processing changes from ODataCollection to Dav / ServiceCollection / another ODataCollection resource
            //If it is necessary to register the user data or link, the process is executed
            if (currentPath != null && !entryName.startsWith(currentPath)) {
                execBulkRequest(currentDavCmp.getCell().getId(), currentDavCmp.getODataProducer());
                createUserdataLinks(currentDavCmp.getODataProducer());
                userDataLinks = new JSONUserDataLinks();
                currentPath = null;
            }
            int entryType = getEntryType(entryName);
            switch (entryType) {
            case TYPE_ODATA_COLLECTION:
                createODataCollectionContents(entryName, pathInZip);
                writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
                doneKeys.add(entryName);
                return FileVisitResult.SKIP_SUBTREE;

            case TYPE_DAV_FILE:
                //Create a WebDAV collection
                //Register entries under bar / 90_contents / {davcol_name} one by one
                registWebDavFile(entryName, pathInZip);
                break;

            case TYPE_SVC_FILE:
                //Creating a Service collection
                installSvcCollection(pathInZip, entryName);
                break;

            default:
                break;
            }
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
            doneKeys.add(entryName);
        } catch (IOException ex) {
            log.info("IOException: " + ex.getMessage(), ex.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2000");
            throw PersoniumBarException.INSTALLATION_FAILED.path("").detail(detail);
        }
        return FileVisitResult.CONTINUE;
    }

    private void execBulkRequest(String cellId, PersoniumODataProducer producer) {
        //Execute bulk registration in bulk
        producer.bulkCreateEntity(producer.getMetadata(), bulkRequests, cellId);

        //Check response
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            //If an error has occurred, return the error response
            if (request.getValue().getError() != null) {
                if (request.getValue().getError() instanceof PersoniumCoreException) {
                    PersoniumCoreException e = (PersoniumCoreException) request.getValue().getError();
                    log.info("PersoniumCoreException: " + e.getMessage());
                    throw PersoniumBarException.INSTALLATION_FAILED.path(
                            fileNameMap.get(request.getKey())).detail(e.getMessage());
                } else {

                    Exception e = request.getValue().getError();
                    log.info("Regist Entity Error: " + e.toString());
                    log.info("Regist Entity Error: " + e.getClass().getName());
                    log.info("Regist Entity Error: " + e);
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
                    throw PersoniumBarException.INSTALLATION_FAILED.path(
                            fileNameMap.get(request.getKey())).detail(detail);
                }
            }
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, fileNameMap.get(request.getKey()));
        }

        bulkRequests.clear();
        fileNameMap.clear();
    }

    private void createUserdataLinks(PersoniumODataProducer producer) {
        int linkSize = userDataLinks.getLinks().size();
        int linkCount = 0;
        String message = PersoniumCoreMessageUtils.getMessage(BarFileUtils.CODE_INSTALL_PROCESSING);
        for (JSONUserDataLink json : userDataLinks.getLinks()) {
            linkCount++;
            createUserdataLink(json, producer);
            long linksOutputStreamSize = Long.parseLong(
                    PersoniumUnitConfig.get(PersoniumUnitConfig.BAR.BAR_USERDATA_LINKS_OUTPUT_STREAM_SIZE));
            if (linkCount % linksOutputStreamSize == 0) {
                writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING,
                        String.format("userDataLinks %d / %d", linkCount, linkSize), message);
            }
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING,
                String.format("userDataLinks %d / %d", linkCount, linkSize), message);
    }

    /**
     * Register the link information defined in 10_odatarelations.json to the ES.
     * @param mappedObject Object read from JSON file
     */
    private void createUserdataLink(JSONUserDataLink mappedObject, PersoniumODataProducer producer) {
        OEntityId sourceEntity = null;
        OEntityId newTargetEntity = null;
        try {
            Map<String, String> fromId = mappedObject.getFromId();
            String fromKey = "";
            Iterator<Entry<String, String>> fromIterator = fromId.entrySet().iterator();
            fromKey = String.format("('%s')", fromIterator.next().getValue());

            OEntityKey fromOEKey = OEntityKey.parse(fromKey);
            sourceEntity = OEntityIds.create(mappedObject.getFromType(), fromOEKey);
            String targetNavProp = mappedObject.getNavPropToType();

            //Link creation preprocessing
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toId = mappedObject.getToId();
            String toKey = "";
            Iterator<Entry<String, String>> toIterator = toId.entrySet().iterator();
            toKey = String.format("('%s')", toIterator.next().getValue());

            OEntityKey toOEKey = OEntityKey.parse(toKey);
            newTargetEntity = OEntityIds.create(mappedObject.getToType(), toOEKey);

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
            throw PersoniumBarException.INSTALLATION_FAILED.path(path).detail(targetPath);
        }
    }

    /**
     * Get the type of entry under 90_contents of the bar file.
     * @param entryName bar File entry name
     * @return entry type
     */
    private int getEntryType(String entryName) {

        if (odataCollectionMap.containsKey(entryName)) {
            return TYPE_ODATA_COLLECTION;
        } else if (webdavCollectionMap.containsKey(entryName)) {
            return TYPE_WEBDAV_COLLECTION;
        } else if (serviceCollectionMap.containsKey(entryName)) {
            return TYPE_SERVICE_COLLECTION;
        } else if (davFileContentTypeMap.containsKey(entryName)) {
            return TYPE_DAV_FILE;
        }

        for (Entry<String, DavCmp> entry : odataCollectionMap.entrySet()) {
            String odataColPath = entry.getKey();
            if (entryName.startsWith(odataColPath)) {
                return TYPE_ODATA_COLLECTION;
            }
        }

        for (Entry<String, DavCmp> entry : serviceCollectionMap.entrySet()) {
            String serviceColPath = entry.getKey();
            if (entryName.startsWith(serviceColPath)) {
                return TYPE_SVC_FILE;
            }
        }

        //Entries not under the OData collection and not defined in rootprops
        PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2006");
        log.info(detail.getMessage() + " [" + entryName + "]");
        throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
    }

    /**
     * Create ODataCollection contents.
     * @param entryName target name
     * @param pathInZip target path
     */
    private void createODataCollectionContents(String entryName, Path pathInZip) {
        //Register OData Collection
        if (odataCollectionMap.isEmpty()) {
            return;
        }
        isValidODataCollectionContents(entryName);

        Pattern formatPattern = Pattern.compile(BarFile.CONTENTS_DIR + "/.+/90_data/");
        Matcher formatMatcher = formatPattern.matcher(entryName);
        if (formatMatcher.matches()) {
            currentPath = entryName;
        }
        Pattern userodataDirPattern = Pattern.compile(BarFile.CONTENTS_DIR + "/.+/90_data/.+");
        Matcher userodataDirMatcher = userodataDirPattern.matcher(entryName);

        if (getFileExtension(entryName).equals(".xml")) {
            //Analysis of 00_ $ metadata.xml · User schema registration
            progressInfo.addDelta(1L);
            currentDavCmp = getCollection(entryName);
            registUserSchema(entryName, pathInZip, currentDavCmp);
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
            doneKeys.add(entryName);
            return;
        } else if (entryName.endsWith(BarFile.ODATA_RELATIONS_JSON)) {
            progressInfo.addDelta(1L);
            userDataLinks = registJsonLinksUserdata(entryName, pathInZip);
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, entryName);
            doneKeys.add(entryName);
            return;
        } else if (userodataDirMatcher.matches() && getFileExtension(entryName).equals(".json")) {
            progressInfo.addDelta(1L);
            userDataCount++;
            setBulkRequests(pathInZip, entryName, currentDavCmp.getODataProducer());
            doneKeys.add(entryName);
            long bulkSize = Long.parseLong(PersoniumUnitConfig
                    .get(PersoniumUnitConfig.BAR.BAR_USERDATA_BULK_SIZE));
            if (userDataCount % bulkSize == 0) {
                execBulkRequest(currentDavCmp.getCell().getId(), currentDavCmp.getODataProducer());
            }
            return;
        } else if (!entryName.endsWith("/")) {
            //If there are files other than xml and json files, return error
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
            log.info(detail.getMessage() + " [" + entryName + "]");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        } else {
            // In the case of a directory, subordinates are processed recursively.
            // Reason for being separate from recursive processing using FileVisit.
            // - Because the order of processing is fixed, sort path and process it
            try {
                Stream<Path> pathst = Files.list(pathInZip).sorted();
                pathst.forEach(path -> {
                    String nextEntryName = path.toString().replaceFirst("/", "");
                    if (Files.isDirectory(path)) {
                        nextEntryName = nextEntryName + "/";
                    }
                    createODataCollectionContents(nextEntryName, path);
                });
            } catch (IOException e) {
                log.info("IOException: " + e.getMessage(), e.fillInStackTrace());
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2000");
                throw PersoniumBarException.INSTALLATION_FAILED.path("").detail(detail);
            }
        }
    }

    /**
     * bar / 90_contents / {OdataCol_name} is checked to see if it is a correct definition.
     * @param entryName entry name (collection name)
     * @param odataCollectionMap Collection's Map object
     */
    private void isValidODataCollectionContents(String entryName) {

        String odataColPath = "";
        for (Map.Entry<String, DavCmp> entry : odataCollectionMap.entrySet()) {
            if (entryName.startsWith(entry.getKey())) {
                odataColPath = entry.getKey();
                break;
            }
        }

        //Entry name directly under the OData collection
        String odataPath = entryName.replaceAll(odataColPath, "");

        //bar / 90_contents / {OData_collection} Order check immediately below
        if (BarFile.ODATA_RELATIONS_JSON.equals(odataPath)) {

            //Check whether 00_ $ metadata.xml has been processed
            String meatadataPath = odataColPath + BarFile.METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
            //Check whether 90_data / has been processed
            String userDataPath = odataColPath + BarFile.ODATA_DIR + "/";
            if (doneKeys.contains(userDataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }
        if (odataPath.startsWith(BarFile.ODATA_DIR + "/")) {
            //Check whether 00_ $ metadata.xml has been processed
            String meatadataPath = odataColPath + BarFile.METADATA_XML;
            if (!doneKeys.contains(meatadataPath)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }

        //bar / 90_contents / {OData_collection} / {dirPath} / check
        String dirPath = null;
        Pattern pattern = Pattern.compile("^([^/]+)/.*");
        Matcher m = pattern.matcher(odataPath);
        if (m.matches()) {
            dirPath = m.replaceAll("$1");
        }
        if (dirPath != null && !dirPath.equals(BarFile.ODATA_DIR)) {
            //bar / 90_contents / {OData_collection} / {dir} / is an error
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
            log.info(detail.getMessage() + "entryName: " + entryName);
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
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
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                log.info(detail.getMessage() + "entryName: " + entryName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
            }
        }
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
     * Get Map <key, DavCmpEsImpl> of the collection defined in the bar file.
     * @param entryName entry name
     * @return collection MapDavCmpEsImpl object
     */
    private DavCmp getCollection(String entryName) {
        int pos = entryName.lastIndexOf("/");
        if (pos == entryName.length() - 1) {
            return odataCollectionMap.get(entryName);
        }
        String colName = entryName.substring(0, pos + 1);
        return odataCollectionMap.get(colName);
    }

    /**
     * Analyzes 00_ $ metadata_xml to register the user schema.
     * @param entryName entry name
     * @param pathInZip input Path
     * @param davCmp Collection Operation object
     */
    private void registUserSchema(String entryName, Path pathInZip, DavCmp davCmp) {
        EdmDataServices metadata = null;
        //If you pass InputStream to the XML parser (StAX, SAX, DOM) as is, the file list acquisition processing
        //Because it will be interrupted, store it as a provisional countermeasure and then parse it
        try (BufferedReader bufferedReader = Files.newBufferedReader(pathInZip, StandardCharsets.UTF_8)) {
            //Load 00_ $ metadata.xml and register user schema
            XMLFactoryProvider2 provider = StaxXMLFactoryProvider2.getInstance();
            XMLInputFactory2 factory = provider.newXMLInputFactory2();
            XMLEventReader2 reader = factory.createXMLEventReader(bufferedReader);
            PersoniumEdmxFormatParser parser = new PersoniumEdmxFormatParser();
            metadata = parser.parseMetadata(reader);
        } catch (Exception | StackOverflowError ex) {
            //StackOverFlowError occurs when circular reference of ComplexType is made
            log.info("XMLParseException: " + ex.getMessage(), ex.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2002");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
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
            log.info("PersoniumCoreException: " + e.getMessage());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        } catch (Exception e) {
            log.info("Regist Entity Error: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }
    }

    /**
     * Register ComplexType / ComplexTypeProperty defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    @SuppressWarnings("unchecked")
    private void createComplexTypes(EdmDataServices metadata, DavCmp davCmp) {
        //Since DeclaredProperty is associated with ComplexType, ComplexTypeProperty is registered for each ComplexType
        Iterable<EdmComplexType> complexTypes = metadata.getComplexTypes();
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmComplexType complexType : complexTypes) {
            log.debug("ComplexType: " + complexType.getName());
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            entityResource.setEntitySetName(ComplexType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", complexType.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
            //Register ComplexType
            String path = String.format("/%s/%s/%s/ComplexType('%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(), complexType.getName());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
            producer.createEntity(ComplexType.EDM_TYPE_NAME, oew);
        }

        //Register ComplexTypeProperty associated with ComplexType
        for (EdmComplexType complexType : complexTypes) {
            createProperties(complexType, producer);
        }
    }

    /**
     * Register EntityType / Property defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    @SuppressWarnings("unchecked")
    private void createEntityTypes(EdmDataServices metadata, DavCmp davCmp) {
        //Since DeclaredProperty is associated with EntityType, Property is registered for each EntityType
        Iterable<EdmEntityType> entityTypes = metadata.getEntityTypes();
        UserSchemaODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmEntityType entity : entityTypes) {
            log.debug("EntityType: " + entity.getName());
            if (producer == null) {
                producer = (UserSchemaODataProducer) davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
            }
            Map<String, String> entityTypeIds = producer.getEntityTypeIds();
            entityResource.setEntitySetName(EntityType.EDM_TYPE_NAME);
            JSONObject json = new JSONObject();
            json.put("Name", entity.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
            //Register EntityType
            String path = String.format("/%s/%s/%s/EntityType('%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(), entity.getName());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
            EntityResponse response = producer.createEntity(EntityType.EDM_TYPE_NAME, oew);
            OEntityWrapper entityResponse = (OEntityWrapper) response.getEntity();
            entityTypeIds.put(entity.getName(), entityResponse.getUuid());
        }
        //Registration of Property associated with EntityType
        for (EdmEntityType entity : entityTypes) {
            createProperties(entity, producer);
        }
    }

    /**
     * Register Property / ComplexTypeProperty defined in Edmx.
     * @param entity EntityType / ComplexType object in which the Property to be registered is defined
     * @param producer OData producer
     */
    @SuppressWarnings("unchecked")
    private void createProperties(EdmStructuralType entity, PersoniumODataProducer producer) {
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
                entityResource.setEntitySetName(edmTypeName);
            }
            CollectionKind kind = property.getCollectionKind();
            if (kind != null && !kind.equals(CollectionKind.NONE) && !kind.equals(CollectionKind.List)) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.METADATA_XML);
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
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), userMetadata);
            //Register ComplexTypeProperty
            producer.createEntity(edmTypeName, oew);
        }
    }

    /**
     * Register AssociationEnd and link information defined in Edmx.
     * @param metadata Edmx metadata
     * @param davCmp Collection Operation object
     */
    private void createAssociations(EdmDataServices metadata, DavCmp davCmp) {
        Iterable<EdmAssociation> associations = metadata.getAssociations();
        PersoniumODataProducer producer = null;
        EdmDataServices userMetadata = null;
        for (EdmAssociation association : associations) {
            //Based on the Association information, link between AssociationEnd and AssociationEnd is registered
            String name = association.getName();
            log.debug("Association: " + name);
            if (producer == null) {
                producer = davCmp.getSchemaODataProducer(box.getCell());
                userMetadata = CtlSchema.getEdmDataServicesForODataSvcSchema().build();
                entityResource.setEntitySetName(AssociationEnd.EDM_TYPE_NAME);
            }
            String path = String.format("/%s/%s/%s/Association('%s','%s')",
                    box.getCell().getName(), box.getName(), davCmp.getName(),
                    association.getEnd1().getRole(), association.getEnd2().getRole());
            writeOutputStream(false, BarFileUtils.CODE_INSTALL_PROCESSING, path);
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
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);
            String tokey = String.format("(Name='%s',_EntityType.Name='%s')", realRoleName2, ae2.getType().getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(AssociationEnd.EDM_TYPE_NAME, toOEKey);
            producer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        }
    }

    /**
     * Register the AssociationEnd passed as an argument.
     * @param producer Entity PersoniumODataProcucer object for registration
     * @param userMetadata User defined schema object
     * @param associationEnd AssociationEnd object for registration
     * @param associationEndName AssociationEnd name
     */
    @SuppressWarnings("unchecked")
    private void createAssociationEnd(PersoniumODataProducer producer,
            EdmDataServices userMetadata, EdmAssociationEnd associationEnd, String associationEndName) {
        //AssociationEnd's name uses the role name of AssociationEnd
        JSONObject json = new JSONObject();
        String entityTypeName = associationEnd.getType().getName();
        json.put(AssociationEnd.P_ASSOCIATION_NAME.getName(), associationEndName);
        json.put(AssociationEnd.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        json.put(AssociationEnd.P_MULTIPLICITY.getName(), associationEnd.getMultiplicity().getSymbolString());
        StringReader stringReader = new StringReader(json.toJSONString());
        OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                entityResource.getOdataResource(), userMetadata);
        producer.createEntity(AssociationEnd.EDM_TYPE_NAME, oew);
    }

    /**
     * Divides the input string ("entity type name: role name") with a colon and returns the character string after the colon.
     * Throws an exception if the colon is not included in the string.
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
     * Reads data of 10_odatarelations.json and generates Link information of user data.
     * @param entryName Target file name
     * @param pathInZip input Path
     * @return Link information of user data
     */
    private JSONUserDataLinks registJsonLinksUserdata(String entryName, Path pathInZip) {
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(pathInZip, StandardCharsets.UTF_8)) {
            JSONUserDataLinks links = mapper.readValue(reader, JSONUserDataLinks.class);
            for (JSONUserDataLink userDataLink : links.getLinks()) {
                userDataLinksJsonValidate(userDataLink);
            }
            return links;
        } catch (JsonParseException e) {
            //Error parsing JSON file
            log.info("JsonParseException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2002");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        } catch (JsonMappingException e) {
            //Data definition error of JSON file
            log.info("JsonMappingException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2003");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        } catch (PersoniumCoreException e) {
            //JSON file validation error
            log.info("PersoniumCoreException" + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        } catch (IOException e) {
            log.info("IOException: " + e.getMessage(), e.fillInStackTrace());
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2000");
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }
    }

    /**
     * Validation of 10_odatarelations.json.
     * @param jsonName JSON filename
     * @param userDataLink read JSON object
     */
    private void userDataLinksJsonValidate(JSONUserDataLink userDataLink) {
        if (userDataLink.getFromType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        }
        if (userDataLink.getFromId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        } else {
            Map<String, String> fromIdMap = userDataLink.getFromId();
            for (Map.Entry<String, String> entry : fromIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
                }
            }
        }
        if (userDataLink.getToType() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        }
        if (userDataLink.getToId() == null) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
        } else {
            Map<String, String> toIdMap = userDataLink.getToId();
            for (Map.Entry<String, String> entry : toIdMap.entrySet()) {
                if (entry.getValue() == null) {
                    throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFile.ODATA_RELATIONS_JSON);
                }
            }
        }
    }

    private void setBulkRequests(Path pathInZip, String entryName, PersoniumODataProducer producer) {
        BulkRequest bulkRequest = new BulkRequest();
        String key = PersoniumUUID.randomUUID();
        try {
            //Get entityType name
            String entityTypeName = getEntityTypeName(entryName);
            if (producer.getMetadata().findEdmEntitySet(entityTypeName) == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            //Get JSON of user data in StringReader format from ZipArchiveImputStream
            StringReader stringReader = getStringReaderFromPath(pathInZip);

            //Generate request body
            ODataResource odataResource = entityResource.getOdataResource();
            ODataEntitiesResource resource = new ODataEntitiesResource(odataResource, entityTypeName);
            OEntity oEntity = resource.getOEntityWrapper(stringReader, odataResource, producer.getMetadata());

            UserDataODataProducer userDataProducer = (UserDataODataProducer) producer;
            EntitySetDocHandler docHandler = producer.getEntitySetDocHandler(entityTypeName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(userDataProducer.getEntityTypeId(oEntity.getEntitySetName()));

            entityResource.setOdataProducer(userDataProducer);

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
            log.info(entryName + " : " + e.getMessage());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(e.getMessage());
        }
        bulkRequests.put(key, bulkRequest);
        fileNameMap.put(key, entryName);
    }

    private StringReader getStringReaderFromPath(Path pathInZip) throws IOException {
        BufferedReader bufferedReader = Files.newBufferedReader(pathInZip, StandardCharsets.UTF_8);

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

    /**
     * Register the WebDAV file.
     * @param entryName bar File entry name
     * @param pathInZip Path
     */
    private void registWebDavFile(String entryName, Path pathInZip) {

        //Retrieve the file path / collection name of the registration destination
        String filePath = entryName.replaceAll(BarFile.CONTENTS_DIR, "");
        String colPath = entryName.substring(0, entryName.lastIndexOf("/") + 1);

        //Create DavCmp
        DavCmp parentCmp = webdavCollectionMap.get(colPath);

        //Check number of collection files in parent collection
        int maxChildResource = PersoniumUnitConfig.getMaxChildResourceCount();
        if (parentCmp.getChildrenCount() >= maxChildResource) {
            //The number of collection files that can be created in the collection exceeds the limit, so it is an error
            String message = PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR.getMessage();
            log.info(message);
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(message);
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
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2005");
            log.info(detail.getMessage() + ": " + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
        }

        //File Registration
        try (InputStream inputStream = Files.newInputStream(pathInZip)) {
            fileCmp.putForCreate(contentType, inputStream);
        } catch (Exception e) {
            PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2004");
            log.info(detail.getMessage() + ": " + e.getMessage(), e.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(entryName).detail(detail);
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

    private void installSvcCollection(Path pathInZip, String entryName) {
        //Register entries under the bar / 90_contents / {svccol_name} one by one as WebDAV / service
        //Convert {serviceCollection} / {scriptName} to {serviceCollection} / __ src / {scriptName}
        int lastSlashIndex = entryName.lastIndexOf("/");
        StringBuilder serviceSrcName = new StringBuilder();
        serviceSrcName.append(entryName.substring(0, lastSlashIndex));
        serviceSrcName.append("/__src");
        serviceSrcName.append(entryName.substring(lastSlashIndex));

        registWebDavFile(serviceSrcName.toString(), pathInZip);
    }

    /**
     * Output of Http response message.
     * @param isError Specify true on error, false otherwise.
     * @param code Message code (message code defined in personium-messages.properties)
     * @param path Processing target resource path (ex. /bar/meta/roles.json)
     */
    private void writeOutputStream(boolean isError, String code, String path) {
        writeOutputStream(isError, code, path, "");
    }

    /**
     * bar File output of installation log details.
     * @param isError Specify true on error, false otherwise.
     * @param code Message code (message code defined in personium-messages.properties)
     * @param path Processing target resource path (ex. /bar/meta/roles.json)
     * @param detail Detailed information on processing failure (PL-BI-2xxx)
     */
    private void writeOutputStream(boolean isError, String code, String path, String detail) {
        String message = PersoniumCoreMessageUtils.getMessage(code);
        if (detail == null) {
            message = message.replace("{0}", "");
        } else {
            message = message.replace("{0}", detail);
        }
        BarFileUtils.outputEventBus(eventBuilder, eventBus, code, path, message);
        BarFileUtils.writeToProgress(isError, progressInfo, code, message);

        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

    /**
     * Create user data.
     */
    public void createUserdata() {
        //When processing on resources of ODataCollection is finished, if registration of user data and link registration is necessary, it is executed
        if (currentPath != null) {
            execBulkRequest(currentDavCmp.getCell().getId(), currentDavCmp.getODataProducer());
            createUserdataLinks(currentDavCmp.getODataProducer());
            userDataLinks = null;
        }
    }

    /**
     * Check whether a required file exists.
     */
    public void checkNecessaryFile() {
        //Confirm mandatory data (bar / 90_contents / {odatacol_name} / 00 _ $ metadata.xml)
        Set<String> colList = odataCollectionMap.keySet();
        for (String colName : colList) {
            String filename = colName + BarFile.METADATA_XML;
            if (!doneKeys.contains(filename)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2001");
                throw PersoniumBarException.INSTALLATION_FAILED.path(filename).detail(detail);
            }
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

/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propstat;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.Response;
import org.json.simple.JSONObject;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.bar.jackson.IJSONMappedObjects;
import io.personium.core.bar.jackson.JSONExtRole;
import io.personium.core.bar.jackson.JSONExtRoles;
import io.personium.core.bar.jackson.JSONLink;
import io.personium.core.bar.jackson.JSONLinks;
import io.personium.core.bar.jackson.JSONRelation;
import io.personium.core.bar.jackson.JSONRelations;
import io.personium.core.bar.jackson.JSONRole;
import io.personium.core.bar.jackson.JSONRoles;
import io.personium.core.bar.jackson.JSONRule;
import io.personium.core.bar.jackson.JSONRules;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavCommon;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.rs.odata.ODataEntityResource;
import io.personium.core.utils.UriUtils;

/**
 * Runner that performs bar install processing.
 */
public class BarFileInstallRunner implements Runnable {

    /** Logger. */
    static Logger log = LoggerFactory.getLogger(BarFileInstallRunner.class);

    /** Contents directory in bar file. */
    private static final String CONTENTS_DIR = BarFile.CONTENTS_DIR + "/";
    /** personium-localbox:/ non slush. */
    private static final String LOCALBOX_NO_SLUSH = UriUtils.SCHEME_LOCALBOX + ":";

    /** Install target box. */
    private Box box;
    /** Install target boxCmp. */
    private BoxCmp boxCmp;
    /** Bar file path. */
    private Path barFilePath;
    /** Unit base url. */
    private String baseUrl;
    /** Progress info. */
    private BarInstallProgressInfo progressInfo;
    /** OData entity resource. */
    private ODataEntityResource entityResource;
    /** Personium event bus for sending event. */
    private EventBus eventBus;
    /** Personium event builder. */
    private PersoniumEvent.Builder eventBuilder;
    /** Personium event request key. */
    private String requestKey;

    /** Map of dav collection to register. */
    private Map<String, DavCmp> davCollectionMap = new HashMap<String, DavCmp>();
    /** Map of dav file content-type to register. */
    private Map<String, String> davFileContentTypeMap = new HashMap<String, String>();
    /** Map of dav file acl to register. */
    private Map<String, Element> davFileAclMap = new HashMap<String, Element>();
    /** Map of dav file property to register. */
    private Map<String, List<Element>> davFilePropsMap = new HashMap<String, List<Element>>();

    /**
     * Constructor.
     * @param barFilePath Bar file path
     * @param entryCount Entry file count in bar file
     * @param boxName Target box name
     * @param schema Target box schema
     * @param uriInfo URI info
     * @param entityResource OData entity resource
     * @param requestKey Personium event request key
     */
    public BarFileInstallRunner(Path barFilePath,
            long entryCount,
            String boxName,
            String schema,
            UriInfo uriInfo,
            ODataEntityResource entityResource,
            String requestKey) {
        this.barFilePath = barFilePath;
        this.baseUrl = uriInfo.getBaseUri().toASCIIString();
        this.entityResource = entityResource;
        this.requestKey = requestKey;

        createBox(boxName, schema);
        // Since manifest.json has already been processed, subtract 1 from entryCount.
        progressInfo = new BarInstallProgressInfo(box.getCell().getId(), box.getId(), entryCount - 1);
        setEventBus();
    }

    /**
     * Register Box information in ES.
     * @ param json JSON object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createBox(String boxName, String schema) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("Name", boxName);
        jsonObj.put("Schema", schema);
        StringReader stringReader = new StringReader(jsonObj.toJSONString());

        OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                entityResource.getOdataResource(),
                CtlSchema.getEdmDataServicesForCellCtl().build());

        //Register Box
        EntityResponse res = entityResource.getOdataProducer().createEntity(Box.EDM_TYPE_NAME, oew);

        //Register Dav
        box = new Box(entityResource.getAccessContext().getCell(), oew);
        boxCmp = ModelFactory.boxCmp(box);

        // post event
        postCellCtlCreateEvent(res);
    }

    /**
     * bar Make settings for internal event output of installation processing status.
     */
    private void setEventBus() {
        //The schema of the TODO Box and the subject's log are implemented at the time of formal correspondence of internal events
        String type = WebDAVMethod.MKCOL.toString();
        String object = UriUtils.SCHEME_LOCALCELL + ":/" + box.getName();
        String result = "";
        eventBuilder = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(result)
                .requestKey(this.requestKey);
        eventBus = box.getCell().getEventBus();
    }

    /**
     * Install bar file.
     */
    @Override
    public void run() {
        try (BarFile barFile = BarFile.newInstance(barFilePath)) {
            // Start install.
            writeStartProgressCache();

            // Install cell control objects.
            ObjectMapper mapper = new ObjectMapper();
            createCellCtlObjects(barFile, mapper, BarFile.RELATIONS_JSON, JSONRelations.class);
            createCellCtlObjects(barFile, mapper, BarFile.ROLES_JSON, JSONRoles.class);
            createCellCtlObjects(barFile, mapper, BarFile.EXTROLES_JSON, JSONExtRoles.class);
            createCellCtlObjects(barFile, mapper, BarFile.RULES_JSON, JSONRules.class);
            createCellCtlObjects(barFile, mapper, BarFile.LINKS_JSON, JSONLinks.class);

            // Install collections.
            registXmlEntry(barFile.getPath(BarFile.ROOTPROPS_XML).toString(), barFile.getReader(BarFile.ROOTPROPS_XML));

            // Install webdav files and user data.
            BarFileContentsInstallVisitor visitor = new BarFileContentsInstallVisitor(box, progressInfo, entityResource,
                    eventBus, eventBuilder, davCollectionMap, davFileContentTypeMap, davFileAclMap, davFilePropsMap);
            Files.walkFileTree(barFile.getPath(BarFile.CONTENTS_DIR).toAbsolutePath(), visitor);
            visitor.createUserdata();
            visitor.checkNecessaryFile();
        } catch (PersoniumBarException e) {
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            progressInfo.setEndTime();
            log.info("PersoniumException" + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(e);
            writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_FAILED,
                    UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), e.getMessage());
            return;
        } catch (PersoniumCoreException e) {
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            progressInfo.setEndTime();
            log.info("PersoniumException" + e.getMessage(), e.fillInStackTrace());
            writeOutputStream(PersoniumBarException.INSTALLATION_FAILED.detail(e.getMessage()));
            writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_FAILED,
                    UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), e.getMessage());
            return;
        } catch (Throwable t) {
            progressInfo.setStatus(ProgressInfo.STATUS.FAILED);
            progressInfo.setEndTime();
            String message = getErrorMessage(t);
            log.info("Exception: " + message, t.fillInStackTrace());
            writeOutputStream(true, "PL-BI-1005", "", message);
            writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_FAILED,
                    UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), message);
            return;
        } finally {
            try {
                if (Files.exists(barFilePath) && !Files.deleteIfExists(barFilePath)) {
                    log.info("Failed to remove bar file. [" + barFilePath.toAbsolutePath().toString() + "].");
                }
            } catch (IOException e) {
                log.info("Failed to remove bar file. [" + barFilePath.toAbsolutePath().toString() + "].");
            }
        }
        // End install.
        writeCompleteProgressCache();
    }

    /**
     * Record the information that started processing in the cache.
     */
    private void writeStartProgressCache() {
        writeOutputStream(false, "PL-BI-1000", UriUtils.SCHEME_LOCALCELL + ":/" + box.getName(), "");
        BarFileUtils.writeToProgressCache(true, progressInfo);
    }

    /**
     * Record the processed information in the cache.
     */
    private void writeCompleteProgressCache() {
        writeOutputStream(false, BarFileUtils.CODE_BAR_INSTALL_COMPLETED,
                UriUtils.SCHEME_LOCALCELL + ":/" + box.getName());
        progressInfo.setStatus(ProgressInfo.STATUS.COMPLETED);
        progressInfo.setEndTime();
        BarFileUtils.writeToProgressCache(true, progressInfo);
    }

    /**
     * Create cell controle objects.
     * @param barFile Bar file
     * @param mapper Object mapper(Jackson)
     * @param jsonFileName Source json file name
     * @param jsonClazz Class corresponding to Jackson
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void createCellCtlObjects(BarFile barFile, ObjectMapper mapper, String jsonFileName, Class jsonClazz) {
        if (!barFile.exists(jsonFileName)) {
            return;
        }
        // Start cell controle object install.
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, jsonFileName);
        progressInfo.addDelta(1L);
        try {
            IJSONMappedObjects objects =
                    (IJSONMappedObjects) mapper.readValue(barFile.getReader(jsonFileName), jsonClazz);
            if (objects.size() <= 0) {
                return;
            }
            if (BarFile.RELATIONS_JSON.equals(jsonFileName)) {
                createRelations((JSONRelations) objects);
            } else if (BarFile.ROLES_JSON.equals(jsonFileName)) {
                createRoles((JSONRoles) objects);
            } else if (BarFile.EXTROLES_JSON.equals(jsonFileName)) {
                createExtRoles((JSONExtRoles) objects);
            } else if (BarFile.RULES_JSON.equals(jsonFileName)) {
                createRules((JSONRules) objects);
            } else if (BarFile.LINKS_JSON.equals(jsonFileName)) {
                createLinks((JSONLinks) objects);
            }
        } catch (JsonParseException | JsonMappingException e) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonFileName);
        } catch (IOException e) {
            throw PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_READ.params(e.getMessage());
        }
        // End cell controle object install.
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, jsonFileName);
    }

    /**
     * Register the Relation information defined in 10 _ $ relations.json to the ES.
     * @ param jsonMapObjects JSONMap object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createRelations(JSONRelations jsonMapObjects) {
        for (JSONRelation jsonMapObject : jsonMapObjects.getRelations()) {
            JSONObject json = jsonMapObject.getJson();
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(Relation.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            //Registration of Relation
            EntityResponse res = entityResource.getOdataProducer().createEntity(Relation.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * Register the Role information defined in 20 _ $ roles.json to the ES.
     * @ param jsonMapObjects JSONMap object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createRoles(JSONRoles jsonMapObjects) {
        for (JSONRole jsonMapObject : jsonMapObjects.getRoles()) {
            JSONObject json = jsonMapObject.getJson();
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(Role.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            //Register Role
            EntityResponse res = entityResource.getOdataProducer().createEntity(Role.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * Register ExtRole information defined in 30 _ $ extroles.json in the ES.
     * @ param jsonMapObjects JSONMap object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createExtRoles(JSONExtRoles jsonMapObjects) {
        for (JSONExtRole jsonMapObject : jsonMapObjects.getExtRoles()) {
            JSONObject json = jsonMapObject.getJson();
            String url = (String) json.get(ExtRole.EDM_TYPE_NAME);
            json.put(ExtRole.EDM_TYPE_NAME, url);
            json.put("_Relation._Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            entityResource.setEntitySetName(ExtRole.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            //Register ExtRole
            EntityResponse res = entityResource.getOdataProducer().createEntity(ExtRole.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * Register the Rule information defined in 50_rules.json to the ES.
     * @ param jsonMapObjects JSONMap object read from JSON file
     */
    @SuppressWarnings("unchecked")
    private void createRules(JSONRules jsonMapObjects) {
        for (JSONRule jsonMapObject : jsonMapObjects.getRules()) {
            JSONObject json = jsonMapObject.getJson();
            log.debug("createRules: " + json.toString());
            json.put("_Box.Name", box.getName());
            StringReader stringReader = new StringReader(json.toJSONString());
            //Register Rule to Entity and also register to RuleManager with afterCreate
            entityResource.setEntitySetName(Rule.EDM_TYPE_NAME);
            OEntityWrapper oew = entityResource.getOEntityWrapper(stringReader,
                    entityResource.getOdataResource(), CtlSchema.getEdmDataServicesForCellCtl().build());
            //Register Rule
            EntityResponse res = entityResource.getOdataProducer().createEntity(Rule.EDM_TYPE_NAME, oew);
            // post event
            postCellCtlCreateEvent(res);
        }
    }

    /**
     * Register the link information defined in 70 _ $ links.json to the ES.
     * @ param jsonMapObjects JSONMap object read from JSON file
     */
    private void createLinks(JSONLinks jsonMapObjects) {
        for (JSONLink jsonMapObject : jsonMapObjects.getLinks()) {
            Map<String, String> fromNameMap = jsonMapObject.getFromName();
            String fromkey =
                    BarFileUtils.getComplexKeyName(jsonMapObject.getFromType(), fromNameMap, box.getName());
            OEntityKey fromOEKey = OEntityKey.parse(fromkey);
            OEntityId sourceEntity = OEntityIds.create(jsonMapObject.getFromType(), fromOEKey);
            String targetNavProp = jsonMapObject.getNavPropToType();

            //Link creation preprocessing
            entityResource.getOdataResource().beforeLinkCreate(sourceEntity, targetNavProp);

            Map<String, String> toNameMap = jsonMapObject.getToName();
            String tokey =
                    BarFileUtils.getComplexKeyName(jsonMapObject.getToType(), toNameMap, box.getName());
            OEntityKey toOEKey = OEntityKey.parse(tokey);
            OEntityId newTargetEntity = OEntityIds.create(jsonMapObject.getToType(), toOEKey);
            //Register $ links
            entityResource.getOdataProducer().createLink(sourceEntity, targetNavProp, newTargetEntity);

            // post event
            String keyString = AbstractODataResource.replaceDummyKeyToNull(fromOEKey.toString());
            String targetKeyString = AbstractODataResource.replaceDummyKeyToNull(toOEKey.toString());
            String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
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
            EventBus bus = box.getCell().getEventBus();
            bus.post(ev);
        }
    }

    /**
     * Post event to EventBus.
     * @param res
     */
    private void postCellCtlCreateEvent(EntityResponse res) {
        String name = res.getEntity().getEntitySetName();
        String keyString = AbstractODataResource.replaceDummyKeyToNull(res.getEntity().getEntityKey().toKeyString());
        String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                .append(":/__ctl/").append(name).append(keyString).toString();
        String info = "box install";
        String type = PersoniumEventType.cellctl(name, PersoniumEventType.Operation.CREATE);
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .requestKey(this.requestKey)
                .build();
        EventBus bus = box.getCell().getEventBus();
        bus.post(ev);
    }

    /**
     * Analyze 90_rootprops_xml and perform registration processing such as Collectoin / ACL / WebDAV.
     * @ param rootPropsName Path name in bar file of 90_rootprops_xml
     * @ param bufferedReader input stream Reader
     */
    protected void registXmlEntry(String rootPropsName, BufferedReader bufferedReader) {
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_STARTED, rootPropsName);
        progressInfo.addDelta(1L);
        try {
            //If you pass InputStream to the XML parser (StAX, SAX, DOM) as is, the file list acquisition processing
            //Because it will be interrupted, store it as a provisional countermeasure and then parse it
            StringBuffer buf = new StringBuffer();
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                buf.append(str);
            }

            Multistatus multiStatus = Multistatus.unmarshal(new ByteArrayInputStream(buf.toString().getBytes()));

            //Validate the definition contents of 90_rootprops.xml.
            //By checking in advance, make sure that garbage data is not created.
            validateCollectionDefinitions(multiStatus, rootPropsName);
            for (Response response : multiStatus.getResponse()) {
                String collectionType = DavCmp.TYPE_COL_WEBDAV;
                boolean hasCollection = false;
                boolean isBox = false;

                List<String> hrefs = response.getHref();
                String href = hrefs.get(0);
                if (href.equals(LOCALBOX_NO_SLUSH)) {
                    href = UriUtils.SCHEME_BOX_URI;
                }
                if (href.equals(UriUtils.SCHEME_BOX_URI)) {
                    isBox = true;
                }
                String collectionUrl = null;
                collectionUrl = href.replaceFirst(UriUtils.SCHEME_BOX_URI, box.getUrl());

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
                                collectionType = DavCmp.TYPE_COL_ODATA;
                            } else if (nodeName.equals("p:service")) {
                                collectionType = DavCmp.TYPE_COL_SVC;
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
                            aclElement = BarFileUtils.convertToRoleInstanceUrl(element, baseUrl,
                                    box.getCell().getName(), box.getName());
                            continue;
                        }
                        propElements.add(element);
                    }
                }

                String entryName = CONTENTS_DIR + href.replaceFirst(UriUtils.SCHEME_BOX_URI, "");
                if (isBox) {
                    // For Box, collection and ACL registration.
                    davCollectionMap.put(entryName, boxCmp);
                    registBoxAclAndProppatch(this.box, aclElement, propElements, collectionUrl);
                } else if (hasCollection) {
                    if (!entryName.endsWith("/")) {
                        entryName += "/";
                    }
                    //For collections, collection, ACL, PROPPATH registration
                    log.info(entryName);
                    createCollection(collectionUrl, entryName, this.box.getCell(), this.box, collectionType, aclElement,
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
            throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(e.getMessage());
        } catch (Exception ex) {
            String message = getErrorMessage(ex);
            log.info("XMLParseException: " + message, ex.fillInStackTrace());
            throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(message);
        }
        writeOutputStream(false, BarFileUtils.CODE_INSTALL_COMPLETED, rootPropsName);
    }

    /**
     * Verify that there is no inconsistency in the hierarchical structure of path defined in 90_rootprops.xml.
     * @ param multiStatus 90 JOXB object read from 90 _rootprops.xml
     * @ param rootPropsName Name of the entry currently being processed (for log output)
     */
    private void validateCollectionDefinitions(Multistatus multiStatus, String rootPropsName) {

        //Read the XML definition and get the path definition and type of href element (OData collection / WebDAV collection / service collection, WebDAV file, service source).
        Map<String, String> pathMap = new LinkedHashMap<String, String>();
        for (Response response : multiStatus.getResponse()) {
            List<String> hrefs = response.getHref();
            if (hrefs.size() != 1) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2008");
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            String href = hrefs.get(0);
            //If there is no href attribute value, it is regarded as a definition error.
            if (href == null || href.length() == 0) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2009");
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            //If it does not start with localbox as href attribute value, it is regarded as definition error.
            if (!href.startsWith(LOCALBOX_NO_SLUSH)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail(
                        "PL-BI-2010", LOCALBOX_NO_SLUSH, href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            //Select the type of the defined path. Abnormal termination (log output is unnecessary) when an incorrect path type is specified.
            String collectionType = getCollectionType(rootPropsName, response);
            switch (collectionType) {
                case DavCmp.TYPE_COL_WEBDAV:
                case DavCmp.TYPE_COL_ODATA:
                case DavCmp.TYPE_COL_SVC:
                    if (href.endsWith("/")) {
                        href = href.substring(0, href.length() - 1);
                    }
                    break;
                default:
                    break;
            }
            //If the path definitions are duplicated, the same data is registered, so it is defined as a definition error.
            //In order to ignore the condition of "/" designation at the end of the path, check at this timing.
            if (pathMap.containsKey(href)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2011", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            pathMap.put(href, collectionType);
        }
        //Verify the validity of the Collection path based on the read path definition.
        //· Common: Definition of Box route is mandatory
        //· Common: There is no inconsistency in the path hierarchy structure
        //· For OData collection: Path definition under collection does not exist
        //· For Service collection: Path definition "__src" exists under collection
        Set<String> keySet = pathMap.keySet();
        for (Entry<String, String> entry : pathMap.entrySet()) {
            String href = entry.getKey();
            String currentCollectionType = entry.getValue();
            int upperPathposition = href.lastIndexOf("/");
            if (upperPathposition < 0) { //Skip the path of "dcbox:" because it is not checked
                continue;
            }
            //If an upper layer to be checked is not defined as path information, it is defined as a definition error.
            //Even if the Box root path is not defined, definition error also occurs.
            String upper = href.substring(0, upperPathposition);
            if (!keySet.contains(upper)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2012", upper);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            String upperCollectionType = pathMap.get(upper);
            String resourceName = href.substring(upperPathposition + 1, href.length());
            if (DavCmp.TYPE_COL_ODATA.equals(upperCollectionType)) {
                //OData collection: If a collection / file is defined under the collection, it is a definition error.
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2013", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            } else if (DavCmp.TYPE_COL_SVC.equals(upperCollectionType)) {
                //Service collection: If a collection / file is defined under the collection, it is a definition error.
                //However, only "__ src" is excluded as an exception.
                if (!("__src".equals(resourceName) && DavCmp.TYPE_COL_WEBDAV.equals(currentCollectionType))) {
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2014", href);
                    throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                }
            } else if (DavCmp.TYPE_DAV_FILE.equals(upperCollectionType)) {
                //If a collection / file is defined under the WebDAV file / Service source, it is a definition error.
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2015", href);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
            //If the current collection is a Service collection, if "__src" is not defined in the immediately following path, it is a definition error.
            if (DavCmp.TYPE_COL_SVC.equals(currentCollectionType)) {
                String srcPath = href + "/__src";
                if (!keySet.contains(srcPath) || !DavCmp.TYPE_COL_WEBDAV.equals(pathMap.get(srcPath))) {
                    PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2016", href);
                    throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                }
            }

            //Confirm that it is correct as a resource name (collection / file name format is common).
            if (!DavCommon.isValidResourceName(resourceName)) {
                PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2017", resourceName);
                throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
            }
        }
    }

    /**
     * Get the collection type of the path defined in each response tag in 90_rootprops.xml.
     * @ param rootPropsName Name of the entry currently being processed (for log output)
     * @ param response JAXB object for response tag to be processed
     * @ return Returns the value of the collection type according to the definition content.
     * WebDAV file, Service source is returned as WebDAV file.
     * If the type of unauthorized collection is defined, return it as undefined.
     */
    private String getCollectionType(String rootPropsName, Response response) {
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
                        return DavCmp.TYPE_COL_ODATA;
                    } else if (nodeName.equals("p:service")) {
                        return DavCmp.TYPE_COL_SVC;
                    } else {
                        PersoniumBarException.Detail detail = new PersoniumBarException.Detail("PL-BI-2018", nodeName);
                        throw PersoniumBarException.INSTALLATION_FAILED.path(rootPropsName).detail(detail);
                    }
                }
            } else {
                return DavCmp.TYPE_DAV_FILE;
            }
        }
        return DavCmp.TYPE_COL_WEBDAV;
    }

    /**
     * Register ACL and PROPPATCH information in Box.
     * @param targetBox box
     * @param aclElement ACL
     * @ param propElements What to set with PROPATCH
     * URL of @ param boxUrl box
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
            sbAclXml.append(PersoniumCoreUtils.nodeToString(aclElement));
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

    private Reader getProppatchXml(List<Element> propElements) {
        StringBuffer sbPropXml = new StringBuffer();
        sbPropXml.append("<D:propertyupdate xmlns:D=\"DAV:\"");
        sbPropXml.append(" xmlns:p=\"urn:x-personium:xmlns\"");
        sbPropXml.append(" xmlns:Z=\"http://www.w3.com/standards/z39.50/\">");
        sbPropXml.append("<D:set>");
        sbPropXml.append("<D:prop>");
        for (Element element : propElements) {
            sbPropXml.append(PersoniumCoreUtils.nodeToString(element));
        }
        sbPropXml.append("</D:prop>");
        sbPropXml.append("</D:set>");
        sbPropXml.append("</D:propertyupdate>");
        Reader propXml = new StringReader(sbPropXml.toString());
        return propXml;
    }

    private void createCollection(String collectionUrl,
            String entryName,
            Cell parentCell,
            Box parentBox,
            String collectionType,
            Element aclElement,
            List<Element> propElements) {
        int index;
        if (parentCell == null || parentBox == null) {
            return;
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
            parentCmp = this.davCollectionMap.get(parenEntryName);
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

        String collectionName = "";
        index = collectionUrl.lastIndexOf("/");
        collectionName = collectionUrl.substring(index + 1);

        DavCmp collectionCmp = parentCmp.getChild(collectionName);
        collectionCmp.mkcol(collectionType);

        this.davCollectionMap.put(entryName, collectionCmp);

        //ACL registration
        if (aclElement != null) {
            StringBuffer sbAclXml = new StringBuffer();
            sbAclXml.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
            sbAclXml.append(PersoniumCoreUtils.nodeToString(aclElement));
            Reader aclXml = new StringReader(sbAclXml.toString());
            collectionCmp.acl(aclXml);
        }

        //PROPPATCH registration
        registProppatch(collectionCmp, propElements, collectionUrl);
    }

    /**
     * Get messages from exception objects.
     * @ param ex exception object
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
     * Output of Http response message.
     * @ param isError Specify true on error, false otherwise.
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
     * @ param isError Specify true on error, false otherwise.
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
        BarFileUtils.outputEventBus(eventBuilder, eventBus, code, path, message);
        BarFileUtils.writeToProgress(isError, progressInfo, code, message);

        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }

    /**
     * bar File output of installation log details.
     * @param exception Personium bar exception
     */
    private void writeOutputStream(PersoniumBarException exception) {
        String code = exception.getCode();
        String path = exception.getPath();
        String message = exception.getMessage();
        BarFileUtils.outputEventBus(eventBuilder, eventBus, code, path, message);
        BarFileUtils.writeToProgress(true, progressInfo, code, message);
        String output = String.format("\"%s\",\"%s\",\"%s\"", code, path, message);
        log.info(output);
    }
}

/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.model.impl.fs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.odata4j.producer.CountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import io.personium.common.auth.token.Role;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.common.file.CipherInputStream;
import io.personium.common.file.DataCryptor;
import io.personium.common.file.FileDataAccessor;
import io.personium.common.file.FileDataNotFoundException;
import io.personium.common.utils.CommonUtils;
import io.personium.core.ElapsedTimeLog;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.http.header.ByteRangeSpec;
import io.personium.core.http.header.RangeHeaderHandler;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavDestination;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.file.StreamingOutputForDavFile;
import io.personium.core.model.file.StreamingOutputForDavFileWithRange;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.cache.UserDataSchemaCache;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockKeyComposer;
import io.personium.core.model.lock.LockManager;
import io.personium.core.odata.PersoniumODataProducer;

/**
 * DavCmp implementation using FileSystem.
 */
public class DavCmpFsImpl implements DavCmp {
    /** property key separator. ex:name@namespace. */
    private static final String PROP_KEY_SEPARATOR = "@";

    String fsPath;
    File fsDir;

    Box box;
    Cell cell;
    ObjectFactory of;

    String name;
    Acl acl;
    DavMetadataFile metaFile;
    DavCmpFsImpl parent;
    List<String> ownerRepresentativeAccounts = new ArrayList<String>();
    boolean isPhantom = false;

    /**
     * Fixed File Name for storing file.
     */
    public static final String CONTENT_FILE_NAME = "content";
    private static final String TEMP_FILE_NAME = "tmp";
    private static final int KILO_BYTES = 1000;

    /*
     * logger.
     */
    private static Logger log = LoggerFactory.getLogger(DavCmpFsImpl.class);

    DavCmpFsImpl() {
    }

    /**
     * constructor.
     * @param name name of the path component
     * @param parent parent DavCmp object
     */
    protected DavCmpFsImpl(final String name, final DavCmpFsImpl parent) {
        this.name = name;
        this.of = new ObjectFactory();

        this.parent = parent;

        if (parent == null) {
            this.metaFile = DavMetadataFile.newInstance(this.fsPath);
            return;
        }
        this.cell = parent.getCell();
        this.box = parent.getBox();
        this.fsPath = this.parent.fsPath + File.separator + this.name;
        this.fsDir = new File(this.fsPath);

        this.metaFile = DavMetadataFile.newInstance(this.fsPath);
    }

    /**
     * create a DavCmp whose path most probably does not yet exist.
     * There still are possibilities that other thread creates the corresponding resource and
     * the path actually exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static DavCmpFsImpl createPhantom(final String name, final DavCmpFsImpl parent) {
        DavCmpFsImpl ret = new DavCmpFsImpl(name, parent);
        ret.isPhantom = true;
        return ret;
    }

    /**
     * create a DavCmp whose path most probably does exist.
     * There still are possibilities that other thread deletes the corresponding resource and
     * the path actually does not exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static DavCmpFsImpl create(final String name, final DavCmpFsImpl parent) {
        DavCmpFsImpl ret = new DavCmpFsImpl(name, parent);
        if (ret.exists()) {
            ret.load();
        } else {
            ret.isPhantom = true;
        }
        return ret;
    }


    void createNewMetadataFile() {
        this.metaFile = DavMetadataFile.prepareNewFile(this, this.getType());
        this.metaFile.save();
    }

    @Override
    public boolean isEmpty() {
        if (!this.exists()) {
            return true;
        }
        String type = this.getType();
        if (DavCmp.TYPE_COL_WEBDAV.equals(type)) {
            return !(this.getChildrenCount() > 0);
        } else if (DavCmp.TYPE_COL_BOX.equals(type)) {
            return !(this.getChildrenCount() > 0);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            //Get a list of EntityType associated with Collection
            //Resources linked to EntityType (AsssociationEnd, etc.) are relationships where EntityType is always parent
            //If only EntityType is searched, it is unnecessary to check up to resources associated with EntityType
            UserSchemaODataProducer producer = new UserSchemaODataProducer(this.cell, this);
            CountResponse cr = producer.getEntitiesCount(EntityType.EDM_TYPE_NAME, null);
            if (cr.getCount() > 0) {
                return false;
            }
            //Acquire a list of ComplexType associated with Collection
            //Since the ComplexType resource (ComplexTypeProperty) is a relationship in which ComplexType always becomes a parent
            //If only ComplexType is searched, it is not necessary to check up to the resources associated with ComplexType
            cr = producer.getEntitiesCount(ComplexType.EDM_TYPE_NAME, null);
            return cr.getCount() < 1;
        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            DavCmp svcSourceCol = this.getChild(SERVICE_SRC_COLLECTION);
            if (!svcSourceCol.exists()) {
                //When the Service collection is deleted at critical timing
                //Because the ServiceSource collection does not exist, it is regarded as empty
                return true;
            }
            return !(svcSourceCol.getChildrenCount() > 0);
        }
        PersoniumCoreLog.Misc.UNREACHABLE_CODE_ERROR.writeLog();
        throw PersoniumCoreException.Server.UNKNOWN_ERROR;
    }

    /**
     * @return Acl
     */
    public Acl getAcl() {
        return this.acl;
    }

    /**
     * Returns the level of schema authentication.
     * @return schema authentication level
     */
    public String getConfidentialLevel() {
        if (acl == null) {
            return null;
        }
        return this.acl.getRequireSchemaAuthz();
    }

    /**
     * Return unit permission permission user setting.
     * @return unit promotion permission user setting
     */
    public List<String> getOwnerRepresentativeAccounts() {
        return this.ownerRepresentativeAccounts;
    }

    /**
     * Lock Box.
     * @return Lock of own node
     */
    public Lock lock() {
        log.debug("lock:" + LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_DAV, null, this.box.getId(), null));
        return LockManager.getLock(Lock.CATEGORY_DAV, null, this.box.getId(), null);
    }

    /**
     * Lock OData space.
     * @param cellId CellID
     * @param boxId BoxID
     * @param nodeId NodeID
     * @return Lock object
     */
    protected Lock lockOData(String cellId, String boxId, String nodeId) {
        return LockManager.getLock(Lock.CATEGORY_ODATA, cellId, boxId, nodeId);
    }

    /**
     * @return ETag String with double quote signs.
     */
    @Override
    public String getEtag() {
        StringBuilder sb = new StringBuilder("\"");
        sb.append(this.metaFile.getVersion());
        sb.append("-");
        sb.append(this.metaFile.getUpdated());
        sb.append("\"");
        return sb.toString();
    }

    /**
     * checks if this cmp is Cell level.
     * @return true if Cell level
     */
    public boolean isCellLevel() {
        return false;
    }

    void createDir() {
        try {
            Files.createDirectories(this.fsDir.toPath());
        } catch (IOException e) {
            // Failed to create directory.
            throw new RuntimeException(e);
        }
    }

    /**
     * returns if this resource exists.<br />
     * before using this method, do not forget to load() and update the info.
     * @return true if this resource should exist
     */
    @Override
    public final boolean exists() {
        return this.fsDir != null && this.fsDir.exists() && this.metaFile.exists();
    }

    /**
     * load the info from FS for this Dav resouce.
     */
    public final void load() {
        this.metaFile.load();

        /*
         * Analyze JSON Object, and set metadata such as ACL.
         */
        this.name = fsDir.getName();
        this.acl = this.translateAcl(this.metaFile.getAcl());

        // TODO Interim correspondence.(For security reasons)
//        @SuppressWarnings("unchecked")
//        Map<String, String> props = (Map<String, String>) this.metaFile.getProperties();
//        if (props != null) {
//            for (Map.Entry<String, String> entry : props.entrySet()) {
//                String key = entry.getKey();
//                String val = entry.getValue();
//                int idx = key.indexOf(PROP_KEY_SEPARATOR);
//                String elementName = key.substring(0, idx);
//                String namespace = key.substring(idx + 1);
//                QName keyQName = new QName(namespace, elementName);

//                Element element = parseProp(val);
//                String elementNameSpace = element.getNamespaceURI();

//// Retrieve ownerRepresentativeAccounts
//                if (Key.PROP_KEY_OWNER_REPRESENTIVE_ACCOUNTS.equals(keyQName)) {
//                    NodeList accountNodeList = element.getElementsByTagNameNS(elementNameSpace,
//                            Key.PROP_KEY_OWNER_REPRESENTIVE_ACCOUNT.getLocalPart());
//                    for (int i = 0; i < accountNodeList.getLength(); i++) {
//                        this.ownerRepresentativeAccounts.add(accountNodeList.item(i).getTextContent().trim());
//                    }
//                }
//            }
//        }
    }


    /**
     * Update Dav's management data information <br />
     * If there is no management data, it is an error.
     */
    public final void loadAndCheckDavInconsistency() {
        load();
        if (this.metaFile == null) {
            //When tracing from Box and searching by id, if there is inconsistency in Dav data
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND;
        }
    }

    // TODO Interim correspondence.(For security reasons)
//    private Element parseProp(String value) {
//// Element val into DOM
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(true);
//        DocumentBuilder builder = null;
//        Document doc = null;
//        try {
//            builder = factory.newDocumentBuilder();
//            ByteArrayInputStream is = new ByteArrayInputStream(value.getBytes(CharEncoding.UTF_8));
//            doc = builder.parse(is);
//        } catch (Exception e1) {
//            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(e1);
//        }
//        Element e = doc.getDocumentElement();
//        return e;
//    }

    /*
     * Support for the proppatch method. Save method key = namespaceUri + "@" + localName Value =
     * inner XML String
     */
    @Override
    @SuppressWarnings("unchecked")
    public Multistatus proppatch(final Propertyupdate propUpdate, final String url) {
        long now = new Date().getTime();
        String reqUri = url;
        Multistatus ms = this.of.createMultistatus();
        Response res = this.of.createResponse();
        res.getHref().add(reqUri);

        // Lock
        Lock lock = this.lock();
        //Update processing
        try {
            this.load(); //Acquire latest information after lock

            if (!this.exists()) {
                //If it is deleted at critical timing (initial load to lock acquisition), it is set as 404 error
                throw getNotFoundException().params(this.getUrl());
            }

            Map<String, Object> propsJson = (Map<String, Object>) this.metaFile.getProperties();
            List<Prop> propsToSet = propUpdate.getPropsToSet();

            for (Prop prop : propsToSet) {
                if (null == prop) {
                    throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
                }
                List<Element> lpe = prop.getAny();
                for (Element elem : lpe) {
                    res.setProperty(elem, HttpStatus.SC_OK);
                    String key = elem.getLocalName() + PROP_KEY_SEPARATOR + elem.getNamespaceURI();
                    String value = CommonUtils.nodeToString(elem);
                    log.debug("key: " + key);
                    log.debug("val: " + value);
                    propsJson.put(key, value);
                }
            }

            List<Prop> propsToRemove = propUpdate.getPropsToRemove();
            for (Prop prop : propsToRemove) {
                if (null == prop) {
                    throw PersoniumCoreException.Dav.XML_CONTENT_ERROR;
                }
                List<Element> lpe = prop.getAny();
                for (Element elem : lpe) {

                    String key = elem.getLocalName() + PROP_KEY_SEPARATOR + elem.getNamespaceURI();
                    String v = (String) propsJson.get(key);
                    log.debug("Removing key: " + key);
                    if (v == null) {
                        res.setProperty(elem, HttpStatus.SC_NOT_FOUND);
                    } else {
                        propsJson.remove(key);
                        res.setProperty(elem, HttpStatus.SC_OK);
                    }
                }
            }
            // set the last updated date
            this.metaFile.setProperties((JSONObject) propsJson);
            this.metaFile.setUpdated(now);
            this.metaFile.save();
        } finally {
            lock.release();
        }
        ms.getResponse().add(res);
        return ms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResponseBuilder acl(final Reader reader) {
        // If the request is not empty, parse and extend.
        Acl aclToSet = null;
        try {
            aclToSet = ObjectIo.unmarshal(reader, Acl.class);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_CONTENT_ERROR.reason(e1);
        }
        aclToSet.validateAcl(isCellLevel());
        // Lock
        Lock lock = this.lock();
        try {
            // Reload
            this.load();
            if (!this.exists()) {
                throw getNotFoundException().params(this.getUrl());
            }

            // Get the value of xml:base of ACL.
            String aclBase = aclToSet.getBase();

            // Convert role name (Name) of href of principal into role ID (__id).
            List<Ace> aceList = aclToSet.getAceList();
            if (aceList != null) {
                for (Ace ace : aceList) {
                    String pHref = ace.getPrincipalHref();
                    if (pHref != null) {
                        String id = this.cell.roleResourceUrlToId(pHref, aclBase);
                        ace.setPrincipalHref(id);
                    }
                }
            }

            JSONParser parser = new JSONParser();
            JSONObject aclJson = null;
            try {
                aclJson = (JSONObject) parser.parse(aclToSet.toJSON());
            } catch (ParseException e) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e);
            }
            // Not register the value of xml: base in Elasticsearch.
            aclJson.remove(KEY_ACL_BASE);
            this.metaFile.setAcl(aclJson);
            this.metaFile.save();
            // Response
            return javax.ws.rs.core.Response.status(HttpStatus.SC_OK).header(HttpHeaders.ETAG, this.getEtag());
        } finally {
            lock.release();
        }
    }

    @Override
    public final ResponseBuilder putForCreate(final String contentType, final InputStream inputStream) {
        // Locking
        Lock lock = this.lock();
        try {
            //When newly created, there is no DavNode to be created, so reload the parent DavNode to check its existence.
            //When the parent DavNode does not exist: Because it was deleted by another request, 404 is returned
            //If there is a parent DavNode, but there is a DavNode to be created: When created by another request, update processing is executed
            this.parent.load();
            if (!this.parent.exists()) {
                throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.parent.getUrl());
            }

            //When there is a DavNode to be created, update processing
            if (this.exists()) {
                return this.doPutForUpdate(contentType, inputStream, null);
            }
            //When there is no DavNode to be created, a new creation process
            return this.doPutForCreate(contentType, inputStream);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock1");
        }
    }

    @Override
    public ResponseBuilder putForUpdate(final String contentType, final InputStream inputStream, String etag) {
        //Lock
        Lock lock = this.lock();
        try {
            //For updating, since there is a DavNode to be updated, reload the DavNode to be updated and confirm its existence.
            //When there is no DavNode to be updated:
            //· When there is no parent DavNode to be updated: Because the parent is missing, 404 is returned
            //- When there is a parent DavNode to be updated: Because it was deleted by another request, creation processing is executed
            //If there is a DavNode to be updated: Update processing is executed
            this.load();
            if (this.metaFile == null) {
                this.parent.load();
                if (this.parent.metaFile == null) {
                    throw getNotFoundException().params(this.parent.getUrl());
                }
                return this.doPutForCreate(contentType, inputStream);
            }
            return this.doPutForUpdate(contentType, inputStream, etag);
        } finally {
            //Release the lock
            lock.release();
            log.debug("unlock2");
        }
    }

    /**
     * Newly create the resource.
     * @param contentType ContentType of the generated file
     * @param inputStream Stream of generated file
     * @return ResponseBuilder
     */
    protected ResponseBuilder doPutForCreate(final String contentType, final InputStream inputStream) {
        // check the resource count
        checkChildResourceCount();

        InputStream input = inputStream;
        // Perform encryption.
        DataCryptor cryptor = new DataCryptor(getCellId());
        input = cryptor.encode(inputStream, PersoniumUnitConfig.isDavEncryptEnabled());

        // write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params(getContentFilePath()).writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();

        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        try {
            // create new directory.
            Files.createDirectories(Paths.get(this.fsPath));
            // store the file content.
            File newFile = new File(getContentFilePath());
            Files.copy(bufferedInput, newFile.toPath());
            long writtenBytes = newFile.length();
            String encryptionType = DataCryptor.ENCRYPTION_TYPE_NONE;
            if (PersoniumUnitConfig.isDavEncryptEnabled()) {
                writtenBytes = ((CipherInputStream) input).getReadLengthBeforEncryption();
                encryptionType = DataCryptor.ENCRYPTION_TYPE_AES;
            }
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(newFile);
            }
            // write end log
            endLog.setParams(writtenBytes / KILO_BYTES);
            endLog.writeLog();

            // create new metadata file.
            this.metaFile = DavMetadataFile.prepareNewFile(this, DavCmp.TYPE_DAV_FILE);
            this.metaFile.setContentType(contentType);
            this.metaFile.setContentLength(writtenBytes);
            this.metaFile.setEncryptionType(encryptionType);
            this.metaFile.save();
        } catch (IOException ex) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        }
        this.isPhantom = false;
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_CREATED).header(HttpHeaders.ETAG, getEtag());
    }

    /**
     * Overwrite resources..
     * @param contentType ContentType of the update file
     * @param inputStream Stream of update file
     * @param etag Etag
     * @return ResponseBuilder
     */
    protected ResponseBuilder doPutForUpdate(final String contentType, final InputStream inputStream, String etag) {
        //Get current time
        long now = new Date().getTime();
        //Load latest node information
        //Since it loads twice as TODO as a whole, consider the mechanism of lazy load
        this.load();

        //Correspondence when management data of WebDav is deleted at critical timing (between lock and load)
        //If the management data of WebDav does not exist at this point, it is set to 404 error
        if (!this.exists()) {
            throw getNotFoundException().params(getUrl());
        }

        //If there is a specified etag and it is different from what is derived from internal data rather than *, an error
        if (etag != null && !"*".equals(etag) && !matchesETag(etag)) {
            throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
        }

        // Write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params(getContentFilePath()).writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();
        try {
            // Update Content
            InputStream input = inputStream;
            // Perform encryption.
            DataCryptor cryptor = new DataCryptor(getCellId());
            input = cryptor.encode(inputStream, PersoniumUnitConfig.isDavEncryptEnabled());
            BufferedInputStream bufferedInput = new BufferedInputStream(input);
            File tmpFile = new File(getTempContentFilePath());
            File contentFile = new File(getContentFilePath());
            Files.copy(bufferedInput, tmpFile.toPath());
            Files.delete(contentFile.toPath());
            Files.move(tmpFile.toPath(), contentFile.toPath());
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(contentFile);
            }

            long writtenBytes = contentFile.length();
            String encryptionType = DataCryptor.ENCRYPTION_TYPE_NONE;
            if (PersoniumUnitConfig.isDavEncryptEnabled()) {
                writtenBytes = ((CipherInputStream) input).getReadLengthBeforEncryption();
                encryptionType = DataCryptor.ENCRYPTION_TYPE_AES;
            }
            // Write end log
            endLog.setParams(writtenBytes / KILO_BYTES);
            endLog.writeLog();

            // Update Metadata
            this.metaFile.setUpdated(now);
            this.metaFile.setContentType(contentType);
            this.metaFile.setContentLength(writtenBytes);
            this.metaFile.setEncryptionType(encryptionType);
            this.metaFile.save();
        } catch (IOException ex) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        }

        // response
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_NO_CONTENT).header(HttpHeaders.ETAG, getEtag());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ResponseBuilder get(final String rangeHeaderField) {

        String contentType = getContentType();

        ResponseBuilder res = null;
        String fileFullPath = this.fsPath + File.separator + CONTENT_FILE_NAME;
        long fileSize = getContentLength();
        String encryptionType = getEncryptionType();

        //Range header analysis processing
        final RangeHeaderHandler range = RangeHeaderHandler.parse(rangeHeaderField, fileSize);

        // write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params(fileFullPath).writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();
        try {
            //Differentiate between processing with Range header specification
            if (!range.isValid()) {
                //Return whole file
                StreamingOutput sout = new StreamingOutputForDavFile(fileFullPath, getCellId(), encryptionType);
                res = davFileResponse(sout, fileSize, contentType);
            } else {
                //Partial response corresponding to Range

                //Range header range check
                if (!range.isSatisfiable()) {
                    PersoniumCoreLog.Dav.REQUESTED_RANGE_NOT_SATISFIABLE.params(range.getRangeHeaderField()).writeLog();
                    throw PersoniumCoreException.Dav.REQUESTED_RANGE_NOT_SATISFIABLE;
                }

                if (range.getByteRangeSpecCount() > 1) {
                    //Not compatible with MultiPart response
                    throw PersoniumCoreException.Misc.NOT_IMPLEMENTED.params("Range-MultiPart");
                } else {
                    StreamingOutput sout = new StreamingOutputForDavFileWithRange(
                            fileFullPath, fileSize, range, getCellId(), encryptionType);
                    res = davFileResponseForRange(sout, contentType, range);
                }
            }
            // write end log
            endLog.setParams(fileSize / KILO_BYTES);
            endLog.writeLog();

            return res.header(HttpHeaders.ETAG, getEtag()).header(CommonUtils.HttpHeaders.ACCEPT_RANGES,
                    RangeHeaderHandler.BYTES_UNIT);

        } catch (FileDataNotFoundException nex) {
            this.load();
            if (!exists()) {
                throw getNotFoundException().params(getUrl());
            }
            throw PersoniumCoreException.Dav.DAV_UNAVAILABLE.reason(nex);
        }
    }

    /**
     * File response processing.
     * @param sout
     * StreamingOut object
     * @param fileSize
     * file size
     * @param contentType
     * Content type
     * @return response
     */
    public ResponseBuilder davFileResponse(final StreamingOutput sout, long fileSize, String contentType) {
        return javax.ws.rs.core.Response.ok(sout).header(HttpHeaders.CONTENT_LENGTH, fileSize)
                .header(HttpHeaders.CONTENT_TYPE, contentType);
    }

    /**
     * File response processing.
     * @param sout
     * StreamingOut object
     * @param contentType
     * Content type
     * @param range
     *            RangeHeaderHandler
     * @return response
     */
    private ResponseBuilder davFileResponseForRange(final StreamingOutput sout, String contentType,
            final RangeHeaderHandler range) {
        //Because it does not correspond to MultiPart, it processes only the first byte-renge-set.
        int rangeIndex = 0;
        List<ByteRangeSpec> brss = range.getByteRangeSpecList();
        final ByteRangeSpec brs = brss.get(rangeIndex);

        //I have returned Content - Length to the clear because I can not process Chunked 's Range response in iPad' s safari.
        return javax.ws.rs.core.Response.status(HttpStatus.SC_PARTIAL_CONTENT).entity(sout)
                .header(CommonUtils.HttpHeaders.CONTENT_RANGE, brs.makeContentRangeHeaderField())
                .header(HttpHeaders.CONTENT_LENGTH, brs.getContentLength())
                .header(HttpHeaders.CONTENT_TYPE, contentType);
    }

    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DavCmp getChild(final String childName) {
        // if self is phantom then all children should be phantom.
        if (this.isPhantom) {
            return DavCmpFsImpl.createPhantom(childName, this);
        }
        // otherwise, child might / might not be phantom.
        return DavCmpFsImpl.create(childName, this);
    }

    @Override
    public String getType() {
        if (this.isPhantom) {
            return DavCmp.TYPE_NULL;
        }
        if (this.metaFile == null) {
            return DavCmp.TYPE_NULL;
        }
        return (String) this.metaFile.getNodeType();
    }


    @Override
    public final ResponseBuilder mkcol(final String type) {
        if (!this.isPhantom) {
            throw new RuntimeException("Bug do not call this .");
        }

        //Lock
        Lock lock = this.lock();
        try {
            //It is necessary to confirm the presence again here.
            //TODO reload with some means
            this.parent.load();
            if (!this.parent.exists()) {
                //Parents are deleted first at critical timing,
                //Since a parent does not exist, it is regarded as a 409 error
                throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(this.parent.getUrl());
            }
            if (this.exists()) {
                //The collection was made earlier with critical timing,
                //Since it already exists, EXCEPTION
                throw PersoniumCoreException.Dav.METHOD_NOT_ALLOWED;
            }

            //Check number of hierarchies in collection
            DavCmpFsImpl current = this;
            int depth = 0;
            int maxDepth = PersoniumUnitConfig.getMaxCollectionDepth();
            while (null != current.parent) {
                current = current.parent;
                depth++;
            }
            if (depth > maxDepth) {
                //Since it exceeds the limit of the number of collections, it is set to 400 error
                throw PersoniumCoreException.Dav.COLLECTION_DEPTH_ERROR;
            }

            //Check number of collection files in parent collection
            checkChildResourceCount();

            // Create New Directory
            Files.createDirectory(this.fsDir.toPath());
            // Create New Meta File
            this.metaFile = DavMetadataFile.prepareNewFile(this, type);
            this.metaFile.save();

            //Can I just create a TODO directory and metadata?

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock");
        }
        this.isPhantom = false;

        // Response
        return javax.ws.rs.core.Response.status(HttpStatus.SC_CREATED).header(HttpHeaders.ETAG, this.getEtag());
    }

    /**
     * process MOVE operation.
     * @param etag
     *            ETag Value
     * @param overwrite
     *            whether or not overwrite the target resource
     * @param davDestination
     *            Destination information.
     * @return ResponseBuilder Response Object
     */
    @Override
    public ResponseBuilder move(String etag, String overwrite, DavDestination davDestination) {
        ResponseBuilder res = null;

        //Lock
        Lock lock = this.lock();
        try {
            //Existence check of source resource
            this.load();
            if (!this.exists()) {
                //When the move source is deleted at critical timing (initial load ~ lock acquisition).
                //Since there is no moving source, it is determined as a 404 error
                throw getNotFoundException().params(this.getUrl());
            }
            //If there is a specified etag and it is different from what is derived from internal data rather than *, an error
            if (etag != null && !"*".equals(etag) && !matchesETag(etag)) {
                throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
            }

            //Since reloading the source DavNode may cause the parent DavNode to switch to another resource, reload.
            //At this time, there is a possibility that the parent DavNode has been deleted, so existence check is executed.
            // this.parent.nodeId = this.metaFile.getParentId();
            // this.parent.load();
            // if (this.parent.metaFile == null) {
            // throw getNotFoundException().params(this.parent.getUrl());
            // }

            //Destination load
            davDestination.loadDestinationHierarchy();
            //Validate of destination
            davDestination.validateDestinationResource(overwrite, this);

            //In the MOVE method, the source and the destination Box are the same, so even if you acquire the destination access context,
            //Even if you acquire the access context of the source, you can get the same Object
            //Therefore, we use the access context of the move destination
            //AccessContext ac = davDestination.getDestinationRsCmp().getAccessContext();

            //Access control to the destination
            //For the following reasons, access is controlled to the destination after locking.
            //1. Since access to the ES does not occur in the access control, influence on the length of the lock period is small even if executed during locking.
            //2. When performing access control of the move destination before locking, it is necessary to acquire the information of the move destination, and a request to the ES occurs.
            File destDir = ((DavCmpFsImpl) davDestination.getDestinationCmp()).fsDir;
            if (!davDestination.getDestinationCmp().exists()) {
                davDestination.getDestinationRsCmp().getParent().checkAccessContext(BoxPrivilege.BIND);
                Files.move(this.fsDir.toPath(), destDir.toPath());
                res = javax.ws.rs.core.Response.status(HttpStatus.SC_CREATED);
            } else {
                davDestination.getDestinationRsCmp().getParent().checkAccessContext(BoxPrivilege.BIND);
                davDestination.getDestinationRsCmp().getParent().checkAccessContext(BoxPrivilege.UNBIND);
                FileUtils.deleteDirectory(destDir);
                Files.move(this.fsDir.toPath(), destDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                res = javax.ws.rs.core.Response.status(HttpStatus.SC_NO_CONTENT);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // UNLOCK
            lock.release();
            log.debug("unlock");
        }

        res.header(HttpHeaders.LOCATION, davDestination.getDestinationUri());
        res.header(HttpHeaders.ETAG, this.getEtag());
        return res;
    }

    /**
     * Check number of collection/file in parent collection.
     */
    protected void checkChildResourceCount() {
        //Check number of collection files in parent collection
        int maxChildResource = PersoniumUnitConfig.getMaxChildResourceCount();
        if (this.parent.getChildrenCount() >= maxChildResource) {
            //The number of collection files that can be created in the collection exceeds the limit, so it is set to 400 error
            throw PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
        }
    }

    @Override
    public final ResponseBuilder linkChild(final String childName, final String childNodeId, final Long asof) {
        return null;
    }

    @Override
    public final ResponseBuilder unlinkChild(final String childName, final Long asof) {
        return null;
    }

    /**
     * delete this resource.
     * @param ifMatch ifMatch header
     * @param recursive bool
     * @return JaxRS response builder
     */
    @Override
    public ResponseBuilder delete(final String ifMatch, boolean recursive) {
        // If etag is specified, etag is compared
        if (ifMatch != null && !"*".equals(ifMatch) && !matchesETag(ifMatch)) {
            throw PersoniumCoreException.Dav.ETAG_NOT_MATCH;
        }
        // Lock
        Lock lock = this.lock();
        try {
            // Reload
            this.load();
            if (this.metaFile == null) {
                throw getNotFoundException().params(this.getUrl());
            }
            if (!recursive) {
                // If there is subordinate resource in WebDAV collection, it is regarded as error.
                // In the case of TYPE_Box, it is confirmed that there is no lower resource in processing before this.
                if (TYPE_COL_WEBDAV.equals(this.getType()) && this.getChildrenCount() > 0) {
                    throw PersoniumCoreException.Dav.HAS_CHILDREN;
                }
                doDelete();
            } else {
                makeEmpty();
            }
        } finally {
            // Release lock
            log.debug("unlock");
            lock.release();
        }
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Exec delete.
     */
    protected void doDelete() {
        // write start log
        PersoniumCoreLog.Dav.FILE_OPERATION_START.params(getContentFilePath()).writeLog();
        ElapsedTimeLog endLog = ElapsedTimeLog.Dav.FILE_OPERATION_END.params();
        endLog.setStartTime();

        try {
            FileUtils.deleteDirectory(this.fsDir);
        } catch (IOException e) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(e);
        }

        // write end log
        endLog.setParams("-");
        endLog.writeLog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void makeEmpty() {
        if (TYPE_COL_ODATA.equals(getType())) {
            // Deal with OData for recursive deletion.
            // In fact, OData's processing should not be done with DavCmp.
            // Should be implemented in **ODataProducer class.
            Lock lock = lockOData(getCellId(), getBox().getId(), getId());
            try {
                CellDataAccessor accessor = EsModel.cellData(cell.getDataBundleNameWithOutPrefix(), getCellId());
                accessor.bulkDeleteODataCollection(getBox().getId(), getId());
                UserDataSchemaCache.clear(getId());
            } finally {
                lock.release();
            }
        } else if (TYPE_COL_WEBDAV.equals(getType())) {
            for (DavCmp child : getChildren().values()) {
                child.makeEmpty();
            }
        }
        doDelete();
    }

    /**
     * Creates and returns an instance of an accessor of binary data.
     * @return instance of accessor
     */
    protected FileDataAccessor getBinaryDataAccessor() {
        String owner = cell.getOwnerNormalized();
        String unitUserName = null;
        if (owner == null) {
            unitUserName = AccessContext.TYPE_ANONYMOUS;
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }

        return new FileDataAccessor(PersoniumUnitConfig.getBlobStoreRoot(), unitUserName,
                PersoniumUnitConfig.getPhysicalDeleteMode(), PersoniumUnitConfig.getFsyncEnabled());
    }

    @Override
    public final DavCmp getParent() {
        return this.parent;
    }

    @Override
    public final PersoniumODataProducer getODataProducer() {
        return ModelFactory.ODataCtl.userData(this.cell, this);
    }

    @Override
    public final PersoniumODataProducer getSchemaODataProducer(Cell cellObject) {
        return ModelFactory.ODataCtl.userSchema(cellObject, this);
    }

    @Override
    public final int getChildrenCount() {
        return this.getChildDir().length;
    }
    @Override
    public Map<String, DavCmp> getChildren() {
        Map<String, DavCmp> ret = new HashMap<>();
        File[] files = this.getChildDir();
        for (File f : files) {
            String childName = f.getName();
            ret.put(childName, this.getChild(childName));
        }
        return ret;
    }

    /*
     * retrieve child resource dir.
     */
    private File[] getChildDir() {
        File[] children = this.fsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File child) {
                if (child.isDirectory()) {
                    return true;
                }
                return false;
            }

        });
        return children;
    }

    private Acl translateAcl(JSONObject aclObj) {
        //Convert the value of href of principal from role ID (__ id) to role resource URL.
        //base: setting xml value
        String baseUrlStr = createBaseUrlStr();

        //TODO This is a heavy process that ES search runs many times, so you should do it when you need it
        //From here, once should be removed.
        return this.roleIdToName(aclObj, baseUrlStr);
    }

    /**
     * Get role resource URL from role ID.
     * Replace jsonObj's role ID with role resource URL
     * @param jsonObj
     * JSON after ID replacement
     * @param baseUrlStr
     * xml: base value
     */
    private Acl roleIdToName(Object jsonObj, String baseUrlStr) {
        Acl ret = Acl.fromJson(((JSONObject) jsonObj).toJSONString());
        List<Ace> aceList = ret.getAceList();
        if (aceList == null) {
            return ret;
        }
        //xml: base correspondence
        List<Ace> eraseList = new ArrayList<>();
        for (Ace ace : aceList) {
            String pHref = ace.getPrincipalHref();
            if (pHref != null) {
                //When there is no role name corresponding to the role ID, it is determined that the role has been deleted and it is ignored.
                String roloResourceUrl = this.cell.roleIdToRoleResourceUrl(pHref);
                log.debug("###" + pHref + ":" + roloResourceUrl);
                if (roloResourceUrl == null) {
                    eraseList.add(ace);
                    continue;
                }
                //Edit role resource URL from base: xml value
                roloResourceUrl = baseUrlToRoleResourceUrl(baseUrlStr, roloResourceUrl);
                ace.setPrincipalHref(roloResourceUrl);
            }
        }
        aceList.removeAll(eraseList);
        ret.setBase(baseUrlStr);
        return ret;
    }

    /**
     * Generate an xml: base value in the ACL of PROPFIND.
     * @return
     */
    private String createBaseUrlStr() {
        String result = null;
        if (this.box != null) {
            //For ACLs below the Box level, the URL of the Box resource
            //Since cell URLs are attached with slashes in concatenation, erase the URL if it ends with a slash.
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.cell.getUrl().replaceFirst("/$", ""),
                    this.box.getName(), "");
        } else {
            //In case of Cell level ACL, the resource URL of default box
            //Since cell URLs are attached with slashes in concatenation, erase the URL if it ends with a slash.
            result = String.format(Role.ROLE_RESOURCE_FORMAT, this.cell.getUrl().replaceFirst("/$", ""),
                    Box.MAIN_BOX_NAME, "");
        }
        return result;
    }

    /**
     * Formatting RoleResorceUrl according to xml: base.
     * @param baseUrlStr
     * Value of xml: base
     * @param roleResourceUrlStr
     * Role resource URL
     * @return
     */
    private String baseUrlToRoleResourceUrl(String baseUrlStr, String roleResourceUrlStr) {
        String result = null;
        Role baseUrl = null;
        Role roleResourceUrl = null;
        try {
            //Since base: xml is not a role resource URL, add "__" as a dummy
            baseUrl = new Role(new URL(baseUrlStr + "__"));
            roleResourceUrl = new Role(new URL(roleResourceUrlStr));
        } catch (MalformedURLException e) {
            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND.reason(e);
        }
        if (baseUrl.getBoxName().equals(roleResourceUrl.getBoxName())) {
            //When the BOX of base: xml and the BOX of the role resource URL are the same
            result = roleResourceUrl.getName();
        } else {
            //When the BOX of base: xml differs from the BOX of the role resource URL
            result = String.format(ACL_RELATIVE_PATH_FORMAT, roleResourceUrl.getBoxName(), roleResourceUrl.getName());
        }
        return result;
    }

    /**
     * sync file.
     * (Methods for when it is difficult to control with processing parameters such as Files.copy and Files.move)
     *
     * @param file target file
     * @throws IOException failed sync file
     */
    protected void sync(File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.getFD().sync();
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * It judges whether the given string matches the stored Etag.<br>
     * Do not distinguish between Etag and WEtag (Issue #5).
     * @param etag string
     * @return true if given string matches  the stored Etag
     */
    protected boolean matchesETag(String etag) {
        if (etag == null) {
            return false;
        }
        String storedEtag = this.getEtag();
        String weakEtag = "W/" +  storedEtag;
        return etag.equals(storedEtag) || etag.equals(weakEtag);
    }

    static final String KEY_SCHEMA = "Schema";
    static final String KEY_ACL_BASE = "@xml.base";
    static final String ACL_RELATIVE_PATH_FORMAT = "../%s/%s";

    /**
     * @return cell id
     */
    public String getCellId() {
        return this.cell.getId();
    }

    /**
     * @return DavMetadataFile
     */
    public DavMetadataFile getDavMetadataFile() {
        return this.metaFile;
    }

    /**
     * @return FsPath
     */
    public String getFsPath() {
        return this.fsPath;
    }

    /**
     * Get content file path.
     * @return content file path
     */
    protected String getContentFilePath() {
        return this.fsPath + File.separator + CONTENT_FILE_NAME;
    }

    /**
     * Get temp content file path.
     * @return temp content file path
     */
    protected String getTempContentFilePath() {
        return this.fsPath + File.separator + TEMP_FILE_NAME;
    }

    /**
     * @return URL string of this Dav node.
     */
    public String getUrl() {
        // go to the top ancestor DavCmp (BoxCmp) recursively, and BoxCmp
        // overrides here and give root url.
        return this.parent.getUrl() + "/" + this.name;
    }

    /**
     * Search Es with BoxId.
     * @param cellObj Cell
     * @param boxId Box Id
     * @return Search results
     */
    public static Map<String, Object> searchBox(final Cell cellObj, final String boxId) {

        EntitySetAccessor boxType = EsModel.box(cellObj);
        PersoniumGetResponse getRes = boxType.get(boxId);
        if (getRes == null || !getRes.isExists()) {
            PersoniumCoreLog.Dav.ROLE_NOT_FOUND.params("Box Id Not Hit").writeLog();

            throw PersoniumCoreException.Dav.ROLE_NOT_FOUND;
        }
        return getRes.getSource();
    }

    /**
     * retruns NotFoundException for this resource. <br />
     * messages should vary among resource type Cell, box, file, etc..
     * Each *Cmp class should override this method and define the proper exception <br />
     * Additional info (reason etc.) for the message should be set after calling this method.
     * @return NotFoundException
     */
    public PersoniumCoreException getNotFoundException() {
        return PersoniumCoreException.Dav.RESOURCE_NOT_FOUND;
    }

    @Override
    public Cell getCell() {
        return this.cell;
    }

    @Override
    public Box getBox() {
        return this.box;
    }

    @Override
    public Long getUpdated() {
        return this.metaFile.getUpdated();
    }

    @Override
    public Long getPublished() {
        return this.metaFile.getPublished();
    }

    @Override
    public Long getContentLength() {
        return this.metaFile.getContentLength();
    }

    @Override
    public String getContentType() {
        return this.metaFile.getContentType();
    }

    @Override
    public String getEncryptionType() {
        return this.metaFile.getEncryptionType();
    }

    @Override
    public String getCellStatus() {
        return metaFile.getCellStatus() == null ? Cell.STATUS_NORMAL : metaFile.getCellStatus(); // CHECKSTYLE IGNORE
    }

    @Override
    public String getId() {
        return this.metaFile.getNodeId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(String propertyName, String propertyNamespace) throws IOException, SAXException {
        String key = propertyName + PROP_KEY_SEPARATOR + propertyNamespace;
        String propertyXml = metaFile.getProperty(key);
        if (StringUtils.isEmpty(propertyXml)) {
            return propertyXml;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Element element;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream is = new ByteArrayInputStream(propertyXml.getBytes(CharEncoding.UTF_8));
            element = builder.parse(is).getDocumentElement();
        } catch (ParserConfigurationException | UnsupportedEncodingException e) {
            // Usually, this exception does not occur.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return element.getFirstChild().getNodeValue();
    }

    @Override
    public String getPropertyAsRawString(String propertyName, String propertyNamespace) {
        String key = propertyName + PROP_KEY_SEPARATOR + propertyNamespace;
        return this.metaFile.getProperty(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getProperties() {
        return this.metaFile.getProperties();
    }


}

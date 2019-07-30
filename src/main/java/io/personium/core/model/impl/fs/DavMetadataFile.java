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
package io.personium.core.model.impl.fs;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Date;

import org.apache.commons.io.Charsets;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;

/**
 * a class for handling internal fs file storing Dav metadata.
 */
public class DavMetadataFile {
    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(DavMetadataFile.class);

    /** Metadata file name. */
    //TODO filename can be used on Unix, Windows, but it is nice to use on DAV.
    public static final String DAV_META_FILE_NAME = ".pmeta";

    /** Milliseconds to wait of metafile reading retries. */
    private static final long META_LOAD_RETRY_WAIT = 100L;
    /** Maximum number of metafile reading retries. */
    private static final int META_LOAD_RETRY_MAX = 5;
    private static final int KILO_BYTES = 1000;

    File file;
    JSONObject json = new JSONObject();
    private final PersoniumCoreLog fileOperationLog = PersoniumCoreLog.Dav.FILE_OPERATION.create();

    /** JSON Key for ID. */
    private static final String KEY_ID = "i";

    /** JSON Key for Node Type. */
    private static final String KEY_NODE_TYPE = "t";

    /** JSON Key for ACL. */
    private static final String KEY_ACL = "a";

    /** JSON Key for PROPS JSON key to save.*/
    private static final String KEY_PROPS = "d";

    /** JSON Key for published. */
    private static final String KEY_PUBLISHED = "p";

    /** JSON Key for updated. */
    private static final String KEY_UPDATED = "u";

    /** JSON Key for ContentType. */
    private static final String KEY_CONTENT_TYPE = "ct";

    /** JSON Key for Content Length. */
    private static final String KEY_CONTENT_LENGTH = "cl";

    /** JSON Key for Encryption Type. */
    private static final String KEY_ENCRYPTION_TYPE = "et";

    /** JSON Key for Version. */
    private static final String KEY_VERSION = "v";

    /** JSON Key for Cell Status. */
    private static final String KEY_CELL_STATUS = "cs";

    /**
     * constructor.
     */
    private DavMetadataFile(File file) {
        this.file = file;
    }

    private void setDefault() {
        long date = new Date().getTime();
        this.setNodeId(PersoniumUUID.randomUUID());
        this.setUpdated(date);
        this.setPublished(date);
        this.setVersion(0L);
        this.setAcl(new JSONObject());
        this.setProperties(new JSONObject());
    }

    /**
     * Factory method.
     * @param file File
     * @return DavMetadataFile
     */
    public static DavMetadataFile newInstance(File file) {
        DavMetadataFile meta = new DavMetadataFile(file);
        return meta;
    }

    /**
     * factory method.
     * @param fsPath dav file path
     * @return DavMetadataFile
     */
    public static DavMetadataFile newInstance(String fsPath) {
        String path = fsPath + File.separator + DAV_META_FILE_NAME;
        return newInstance(new File(path));
    }

    /**
     * Factory method for non-existing metadata file.
     * to create new file, create a object using this method and save().
     * @param file File
     * @param type type string defined in DavCmp.TYPE* constants.
     * @return DavMetadataFile
     */
    public static DavMetadataFile prepareNewFile(File file, String type) {
        DavMetadataFile ret = new DavMetadataFile(file);
        ret.setDefault();
        ret.setNodeType(type);
        return ret;
    }

    /**
     * Factory method for non-existing metadata file.
     * to create new file, create a object using this method and save().
     * @param dc DavCmpFsImpl
     * @param type type string defined in DavCmp.TYPE* constants.
     * @return DavMetadataFile
     */
    public static DavMetadataFile prepareNewFile(DavCmpFsImpl dc, String type) {
        String path = dc.fsPath + File.separator + DAV_META_FILE_NAME;
        DavMetadataFile ret = new DavMetadataFile(new File(path));
        ret.setDefault();
        ret.setNodeType(type);
        return ret;
    }

    /**
     * @return true if the file exists.
     */
    public boolean exists() {
        return this.file.exists();
    }

    /**
     * load from the file.
     */
    public void load() {
        // Coping with core issue #28.
        // When an Exception occurs Retry several times.
        int retryCount = 0;
        while (true) {
            try {
                doLoad();
                break;
            } catch (PersoniumCoreException pe) {
                if (retryCount < META_LOAD_RETRY_MAX) {
                    try {
                        Thread.sleep(META_LOAD_RETRY_WAIT);
                    } catch (InterruptedException ie) {
                        // If sleep fails, Error
                        throw new RuntimeException(ie);
                    }
                    retryCount++;
                    log.info("Meta file load retry. RetryCount:" + retryCount);
                } else {
                    // IO failure or JSON is broken
                    throw pe;
                }
            }
        }
    }

    /**
     * load from the file.
     */
    private void doLoad() throws PersoniumCoreException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), Charsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            this.json = (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            // IO failure or JSON is broken
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(e);
        }
    }

    /**
     * save to the file.
     */
    public void save() {
        this.incrementVersion();
        String jsonStr = JSONObject.toJSONString(this.getJSON());

        this.fileOperationLog.setParams(this.file.toPath(), jsonStr.getBytes(Charsets.UTF_8).length / KILO_BYTES);
        this.fileOperationLog.writeStartLog();

        try {
            Files.write(this.file.toPath(), jsonStr.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.fileOperationLog.writeEndLog();
    }

    private void incrementVersion() {
        this.setVersion(this.getVersion() + 1);
    }

    /**
     * returns JSONObject representation of the file content.
     * @return JSONObject
     */
    public JSONObject getJSON() {
        return this.json;
    }

    /**
     * Returns the content of metadata as JSONString.
     * @return JSONString
     */
    public String toJSONString() {
        return json.toJSONString();
    }

    /**
     * @return the nodeId
     */
    public String getNodeId() {
        return (String) this.json.get(KEY_ID);
    }

    /**
     * @param id
     *            the nodeId to set
     */
    @SuppressWarnings("unchecked")
    public void setNodeId(String id) {
        this.json.put(KEY_ID, id);
    }

    /**
     * @return the nodeType
     */
    public String getNodeType() {
        return (String) this.json.get(KEY_NODE_TYPE);
    }

    /**
     * @param nodeType
     *            the nodeType to set
     */
    @SuppressWarnings("unchecked")
    public void setNodeType(String nodeType) {
        this.json.put(KEY_NODE_TYPE, nodeType);
    }

    /**
     * @return the acl
     */
    public JSONObject getAcl() {
        return (JSONObject) this.json.get(KEY_ACL);
    }

    /**
     * @param acl
     *            the acl to set
     */
    @SuppressWarnings("unchecked")
    public void setAcl(JSONObject acl) {
        this.json.put(KEY_ACL, acl);
    }

    /**
     * Get property.
     * @param key Property key
     * @return property value
     */
    public String getProperty(String key) {
        JSONObject props = (JSONObject) this.json.get(KEY_PROPS);
        return (String) props.get(key);
    }

    /**
     * @return the properties
     */
    public JSONObject getProperties() {
        return (JSONObject) this.json.get(KEY_PROPS);
    }

    /**
     * @param properties
     *            the properties to set
     */
    @SuppressWarnings("unchecked")
    public void setProperties(JSONObject properties) {
        this.json.put(KEY_PROPS, properties);
    }

    /**
     * @return the published
     */
    public Long getPublished() {
        return (Long) this.json.get(KEY_PUBLISHED);
    }

    /**
     * @param published
     *            the published to set
     */
    @SuppressWarnings("unchecked")
    public void setPublished(long published) {
        this.json.put(KEY_PUBLISHED, published);
    }

    /**
     * @return the updated
     */
    public Long getUpdated() {
        return (Long) this.json.get(KEY_UPDATED);
    }

    /**
     * @param updated
     *            the updated to set
     */
    @SuppressWarnings("unchecked")
    public void setUpdated(long updated) {
        this.json.put(KEY_UPDATED, updated);
    }

    /**
     * @return content type string
     */
    public String getContentType() {
        return (String) this.json.get(KEY_CONTENT_TYPE);
    }

    /**
     * @param contentType content type string
     */
    @SuppressWarnings("unchecked")
    public void setContentType(String contentType) {
        this.json.put(KEY_CONTENT_TYPE, contentType);
    }

    /**
     * @return long value of content length.
     */
    public long getContentLength() {
        return (Long) this.json.get(KEY_CONTENT_LENGTH);
    }

    /**
     * @param contentLength long value of content length.
     */
    @SuppressWarnings("unchecked")
    public void setContentLength(long contentLength) {
        this.json.put(KEY_CONTENT_LENGTH, contentLength);
    }

    /**
     * @return encryption type string.
     */
    public String getEncryptionType() {
        return (String) this.json.get(KEY_ENCRYPTION_TYPE);
    }

    /**
     * @param encryptionType encryption type string
     */
    @SuppressWarnings("unchecked")
    public void setEncryptionType(String encryptionType) {
        this.json.put(KEY_ENCRYPTION_TYPE, encryptionType);
    }

    /**
     * @return cell status string.
     */
    public String getCellStatus() {
        return (String) this.json.get(KEY_CELL_STATUS);
    }

    /**
     * @param cellStatus cell status string
     */
    @SuppressWarnings("unchecked")
    public void setCellStatus(String cellStatus) {
        this.json.put(KEY_CELL_STATUS, cellStatus);
    }

    /**
     * @return long value of the resource version
     */
    public Long getVersion() {
        return (Long) this.json.get(KEY_VERSION);
    }

    @SuppressWarnings("unchecked")
    private void setVersion(Long version) {
        this.json.put(KEY_VERSION, version);
    }

}

/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.http.HttpStatus;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.CellSnapshotDavCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.file.DataCryptor;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockManager;

/**
 * CellSnapshotDavCmp implementation using FileSystem.
 */
public class CellSnapshotDavCmpFsImpl extends DavCmpFsImpl implements CellSnapshotDavCmp {

//    /** Logger. */
//    private static Logger log = LoggerFactory.getLogger(CellSnapshotDavCmpFsImpl.class);

    /**
     * constructor.
     * @param name name of the path component
     * @param parent parent DavCmp object
     */
    protected CellSnapshotDavCmpFsImpl(String name, DavCmpFsImpl parent) {
        super(name, parent);
    }

    /**
     * create a CellSnapshotDavCmp whose path most probably does not yet exist.
     * There still are possibilities that other thread creates the corresponding resource and
     * the path actually exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static CellSnapshotDavCmpFsImpl createPhantom(String name, DavCmpFsImpl parent) {
        CellSnapshotDavCmpFsImpl ret = new CellSnapshotDavCmpFsImpl(name, parent);
        ret.isPhantom = true;
        return ret;
    }

    /**
     * create a CellSnapshotDavCmp whose path most probably does exist.
     * There still are possibilities that other thread deletes the corresponding resource and
     * the path actually does not exists.
     * @param name path name
     * @param parent parent DavCmp
     * @return created DavCmp
     */
    public static CellSnapshotDavCmpFsImpl create(String name, DavCmpFsImpl parent) {
        CellSnapshotDavCmpFsImpl ret = new CellSnapshotDavCmpFsImpl(name, parent);
        if (ret.exists()) {
            ret.load();
        } else {
            ret.isPhantom = true;
        }
        return ret;
    }

    /**
     * Lock cell.
     * @return Lock object of own node
     */
    @Override
    public Lock lock() {
        return LockManager.getLock(Lock.CATEGORY_CELL, cell.getId(), null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseBuilder putForUpdate(String contentType, InputStream inputStream, String etag) {
        Lock lock = this.lock();
        try {
            if (DavCmp.TYPE_NULL.equals(getType())) {
                return this.doPutForCreate(contentType, inputStream);
            } else {
                return this.doPutForUpdate(contentType, inputStream, etag);
            }
        } finally {
            lock.release();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Snapshot file is not encrypted.
     */
    @Override
    protected ResponseBuilder doPutForCreate(String contentType, InputStream inputStream) {
        // check the resource count
        checkChildResourceCount();

        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        try {
            // create new directory.
            Files.createDirectories(Paths.get(this.fsPath));
            // store the file content.
            File newFile = new File(getContentFilePath());
            Files.copy(bufferedInput, newFile.toPath());
            long writtenBytes = newFile.length();
            String encryptionType = DataCryptor.ENCRYPTION_TYPE_NONE;
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(newFile);
            }

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
     * {@inheritDoc}
     * <p>
     * Snapshot file is not encrypted.
     */
    @Override
    protected ResponseBuilder doPutForUpdate(String contentType, InputStream inputStream, String etag) {
        // Load file info
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

        try {
            // Update Content
            BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
            File tmpFile = new File(getTempContentFilePath());
            File contentFile = new File(getContentFilePath());
            Files.copy(bufferedInput, tmpFile.toPath());
            Files.delete(contentFile.toPath());
            Files.move(tmpFile.toPath(), contentFile.toPath());
            long writtenBytes = contentFile.length();
            String encryptionType = DataCryptor.ENCRYPTION_TYPE_NONE;
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                sync(contentFile);
            }

            // Update Metadata
            this.metaFile.setUpdated(new Date().getTime());
            this.metaFile.setContentType(contentType);
            this.metaFile.setContentLength(writtenBytes);
            this.metaFile.setEncryptionType(encryptionType);
            this.metaFile.save();
        } catch (IOException ex) {
            throw PersoniumCoreException.Dav.FS_INCONSISTENCY_FOUND.reason(ex);
        }
        return javax.ws.rs.core.Response.ok().status(HttpStatus.SC_NO_CONTENT).header(HttpHeaders.ETAG, getEtag());
    }

}

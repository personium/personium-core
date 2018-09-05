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
import java.nio.file.Files;

import org.apache.wink.webdav.model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.CellCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockManager;

/**
 * A component class for DAV nature of a Cell using FileSystem.
 */
public class CellCmpFsImpl extends DavCmpFsImpl implements CellCmp {

    static Logger log = LoggerFactory.getLogger(CellCmpFsImpl.class);

    /**
     * Default constructor.
     */
    CellCmpFsImpl() {
    }

    /**
     * constructor.
     * @param cell Cell Object
     */
    public CellCmpFsImpl(final Cell cell) {
        this.of = new ObjectFactory();
        this.cell = cell;
        StringBuilder path = new StringBuilder(PersoniumUnitConfig.getBlobStoreRoot());
        path.append(File.separatorChar);
        path.append(this.getCell().getDataBundleName());
        path.append(File.separator);
        path.append(this.cell.getId());

        this.fsPath = path.toString();
        this.fsDir = new File(fsPath);
        if (!this.fsDir.exists()) {
            this.createDir();
            this.createNewMetadataFile();
        } else {
            this.metaFile = DavMetadataFile.newInstance(this.fsPath);
        }
        this.load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void createNewMetadataFile() {
        metaFile = DavMetadataFile.prepareNewFile(this, this.getType());
        metaFile.setCellStatus(Cell.STATUS_NORMAL);
        metaFile.save();
    }

    /**
     * Lock Cell.
     * @return Lock of own node
     */
    @Override
    public Lock lock() {
        return LockManager.getLock(Lock.CATEGORY_CELL, this.cell.getId(), null, null);
    }

    @Override
    void createDir() {
        try {
            Files.createDirectories(this.fsDir.toPath());
        } catch (IOException e) {
            // Failed to create directory.
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCellStatusAndSave(String status) {
        metaFile.setCellStatus(status);
        metaFile.save();
        log.info(String.format("Changed cell status. CellName:%s, CellStatus:%s", getName(), status));
    }

    /**
     * checks if this cmp is Cell level.
     * @return true if Cell level
     */
    @Override
    public boolean isCellLevel() {
        return true;
    }

    @Override
    public String getType() {
        return DavCmp.TYPE_CELL;
    }

    @Override
    public String getId() {
        return this.cell.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void makeEmpty() {
        doDelete();
    }

    @Override
    public PersoniumCoreException getNotFoundException() {
        return PersoniumCoreException.Dav.CELL_NOT_FOUND;
    }
}

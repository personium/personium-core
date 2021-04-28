/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.model.impl.fs;

import java.io.File;

import org.apache.wink.webdav.model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.DavCmp;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.CellDataAccessor;
import io.personium.core.model.impl.es.accessor.ODataEntityAccessor;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.lock.Lock;

/**
 * A component class for DAV nature of a Box using FileSystem.
 */
public class BoxCmpFsImpl extends DavCmpFsImpl implements BoxCmp {

    /*
     * logger.
     */
    static Logger log = LoggerFactory.getLogger(BoxCmpFsImpl.class);

    /**
     * constructor.
     * @param box Box object
     */
    public BoxCmpFsImpl(final Box box) {
        this.of = new ObjectFactory();
        this.box = box;
        this.cell = box.getCell();
        StringBuilder path = new StringBuilder(PersoniumUnitConfig.getBlobStoreRoot());
        path.append(File.separatorChar);
        path.append(this.getCell().getDataBundleName());
        path.append(File.separatorChar);
        path.append(this.getCell().getId());
        path.append(File.separatorChar);
        path.append(this.box.getId());
        this.fsPath = path.toString();
        this.fsDir = new File(fsPath);

        if (!this.fsDir.exists()) {
            this.createDir();
            this.createNewMetadataFile();
        } else {
            this.metaFile = DavMetadataFile.newInstance(this.fsPath);
        }

        // load info from fs
        this.load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void makeEmpty() {
        // Deal with OData for recursive deletion.
        // In fact, OData's processing should not be done with DavCmp.
        // Should be implemented in **ODataProducer class.
        Lock lock = lockOData(getCellId(), getId(), null);
        try {
            CellDataAccessor cellDataAccessor = EsModel.cellData(cell.getDataBundleNameWithOutPrefix(), getCellId());
            // Delete all data in box.
            cellDataAccessor.bulkDeleteBox(getId());
            // Delete data linked to Box.
            cellDataAccessor.deleteBoxLinkData(getId());
            ODataEntityAccessor boxAccessor = (ODataEntityAccessor) EsModel.box(cell);
            // Delete box.
            boxAccessor.delete(getId(), -1);
        } finally {
            lock.release();
        }
        doDelete();
        BoxCache.clear(getBox().getName(), getCell());
    }

    @Override
    public String getUrl() {
        return this.cell.getUrl() + this.box.getName();
    }

    @Override
    public String getId() {
        return this.box.getId();
    }

    @Override
    public PersoniumCoreException getNotFoundException() {
        return PersoniumCoreException.Dav.BOX_NOT_FOUND;
    }

    @Override
    public String getType() {
        return DavCmp.TYPE_COL_BOX;
    }
}

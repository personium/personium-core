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

import org.apache.wink.webdav.model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.DavCmp;

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

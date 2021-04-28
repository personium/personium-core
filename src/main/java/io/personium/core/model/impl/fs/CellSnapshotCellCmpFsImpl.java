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

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.CellSnapshotCellCmp;
import io.personium.core.model.DavCmp;

/**
 * CellSnapshotCellCmp implementation using FileSystem.
 */
public class CellSnapshotCellCmpFsImpl extends CellCmpFsImpl implements CellSnapshotCellCmp {

    /**
     * constructor.
     * @param cell Cell Object
     */
    public CellSnapshotCellCmpFsImpl(final Cell cell) {
        of = new ObjectFactory();
        this.cell = cell;
        StringBuilder pathBuilder = new StringBuilder(PersoniumUnitConfig.getCellSnapshotRoot());
        pathBuilder.append(File.separator);
        pathBuilder.append(this.cell.getId());

        fsPath = pathBuilder.toString();
        fsDir = new File(fsPath);
        if (!fsDir.exists()) {
            createDir();
        }

        // Do not create Cell's MetadataFile in the CellExport area.
        // (Because it becomes double management.)
        // The Cell's MetadataFile refers to the normal DAV file area.
        StringBuilder davPathBuilder = new StringBuilder(PersoniumUnitConfig.getBlobStoreRoot());
        davPathBuilder.append(File.separatorChar);
        davPathBuilder.append(this.getCell().getDataBundleName());
        davPathBuilder.append(File.separator);
        davPathBuilder.append(this.cell.getId());
        metaFile = DavMetadataFile.newInstance(davPathBuilder.toString());

        load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final DavCmp getChild(String childName) {
        // if self is phantom then all children should be phantom.
        if (this.isPhantom) {
            return CellSnapshotDavCmpFsImpl.createPhantom(childName, this);
        }
        // otherwise, child might / might not be phantom.
        return CellSnapshotDavCmpFsImpl.create(childName, this);
    }

}

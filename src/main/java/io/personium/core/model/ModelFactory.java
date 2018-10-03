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
package io.personium.core.model;

import io.personium.core.auth.AccessContext;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.es.odata.MessageODataProducer;
import io.personium.core.model.impl.es.odata.UnitCtlODataProducer;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.impl.fs.BoxCmpFsImpl;
import io.personium.core.model.impl.fs.CellCmpFsImpl;
import io.personium.core.model.impl.fs.CellSnapshotCellCmpFsImpl;
import io.personium.core.odata.PersoniumODataProducer;

/**
 * Factory class of model object.
 */
public final class ModelFactory {
    /**
     * Dummy constructor.
     */
    private ModelFactory() {

    }

//    /**
//     * Generate and return a Cell object.
//     * If the corresponding Cell does not exist, it is null
//     * @param uriInfo UriInfo
//     * @return Cell object
//     */
//    public static Cell cell(final UriInfo uriInfo) {
//        return CellEsImpl.load(uriInfo);
//    }

    /**
     * Generate and return a Cell object.
     * If the corresponding Cell does not exist, it is null
     * @param id id
     * @return Cell object
     */
    public static Cell cellFromId(String id) {
        return CellEsImpl.loadFromId(id);
    }

//    public static Cell cell(String cellName, String baseUrl) {
//        return CellEsImpl.load(cellName, baseUrl);
//    }

    /**
     * Get cell from the specified cell name.
     * However, the parameter "url" of Cell is not set.
     * @param cellName target cell name
     * @return cell
     */
    public static Cell cellFromName(String cellName) {
        return CellEsImpl.loadFromName(cellName);
    }

    /**
     * Creates and returns an internal implementation model object of Box.
     * @param box Box class
     * @return Box's internal implementation model object
     */
    public static BoxCmp boxCmp(final Box box) {
        return new BoxCmpFsImpl(box);
    }

    /**
     * Generate and return the Cell's internal implementation model object.
     * @param cell Cell
     * @return Cell's internal implementation model object
     */
    public static CellCmp cellCmp(final Cell cell) {
        return new CellCmpFsImpl(cell);
    }

    /**
     * Create CellExport's internal implementation model object.
     * @param cell Cell
     * @return CellExport's internal implementation model object
     */
    public static CellSnapshotCellCmp cellSnapshotCellCmp(final Cell cell) {
        return new CellSnapshotCellCmpFsImpl(cell);
    }

    /**
     * It is a factory of ODataProducer.
     */
    public static class ODataCtl {
        /**
         * Returns the ODataProducer handling the Unit management entity.
         * @param ac access context
         * @return Unit ODataProducer handling management entities
         */
        public static PersoniumODataProducer unitCtl(AccessContext ac) {
            return new UnitCtlODataProducer(ac);
        }

        /**
         * Returns the ODataProducer handling the Cell management entity.
         * @param cell Cell's Cell
         * @return ODataProducer handling Cell management entities
         */
        public static PersoniumODataProducer cellCtl(final Cell cell) {
            return new CellCtlODataProducer(cell);
        }

        /**
         * Return ODataProducer for producing OData about message.
         * @param cell target cell object
         * @param davRsCmp DavRsCmp
         * @return PersoniumODataProducer MessageODataProducer
         */
        public static PersoniumODataProducer message(final Cell cell, final DavRsCmp davRsCmp) {
            return new MessageODataProducer(cell, davRsCmp);
        }

        /**
         * Return ODataProducer of user data schema.
         * @param cell Cell
         * @param davCmp DavCmp
         * @return ODataProducer
         */
        public static PersoniumODataProducer userSchema(final Cell cell, final DavCmp davCmp) {
            return new UserSchemaODataProducer(cell, davCmp);
        }

        /**
         * Return ODataProducer of user data.
         * @param cell Cell
         * @param davCmp DavCmp
         * @return ODataProducer
         */
        public static PersoniumODataProducer userData(final Cell cell, final DavCmp davCmp) {
            return new UserDataODataProducer(cell, davCmp);
        }

    }

}

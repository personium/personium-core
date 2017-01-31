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

import javax.ws.rs.core.UriInfo;

import io.personium.core.auth.AccessContext;
import io.personium.core.model.impl.es.CellEsImpl;
import io.personium.core.model.impl.es.odata.CellCtlODataProducer;
import io.personium.core.model.impl.es.odata.UnitCtlODataProducer;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.impl.es.odata.UserSchemaODataProducer;
import io.personium.core.model.impl.fs.BoxCmpFsImpl;
import io.personium.core.model.impl.fs.CellCmpFsImpl;
import io.personium.core.odata.DcODataProducer;

/**
 * モデルオブジェクトのファクトリークラス.
 */
public final class ModelFactory {
    /**
     * ダミーのコンストラクタ.
     */
    private ModelFactory() {

    }

    /**
     * Cell オブジェクトを生成して返します.
     * 該当するCellが存在しないときはnull
     * @param uriInfo UriInfo
     * @return Cellオブジェクト
     */
    public static Cell cell(final UriInfo uriInfo) {
        return CellEsImpl.load(uriInfo);
    }

    /**
     * Cell オブジェクトを生成して返します.
     * 該当するCellが存在しないときはnull
     * @param id id
     * @param uriInfo UriInfo
     * @return Cellオブジェクト
     */
    public static Cell cell(final String id, final UriInfo uriInfo) {
        return CellEsImpl.load(id, uriInfo);
    }

    /**
     * Boxの内部実装モデルオブジェクトを生成して返します.
     * @param box Boxクラス
     * @return Boxの内部実装モデルオブジェクト
     */
    public static BoxCmp boxCmp(final Box box) {
        return new BoxCmpFsImpl(box);
    }

    /**
     * Cellの内部実装モデルオブジェクトを生成して返します.
     * @param cell Cell
     * @return Cellの内部実装モデルオブジェクト
     */
    public static CellCmp cellCmp(final Cell cell) {
        return new CellCmpFsImpl(cell);
    }

    /**
     * ODataProducer のファクトリです.
     */
    public static class ODataCtl {
        /**
         * Unit管理エンティティを扱うODataProducerを返します.
         * @param ac アクセスコンテキスト
         * @return Unit管理エンティティを扱うODataProducer
         */
        public static DcODataProducer unitCtl(AccessContext ac) {
            return new UnitCtlODataProducer(ac);
        }

        /**
         * Cell管理エンティティを扱うODataProducerを返します.
         * @param cell CellのCell
         * @return Cell管理エンティティを扱うODataProducer
         */
        public static DcODataProducer cellCtl(final Cell cell) {
            return new CellCtlODataProducer(cell);
        }

        /**
         * ユーザデータschema のODataProducerを返します.
         * @param cell Cell
         * @param davCmp DavCmp
         * @return ODataProducer
         */
        public static DcODataProducer userSchema(final Cell cell, final DavCmp davCmp) {
            return new UserSchemaODataProducer(cell, davCmp);
        }

        /**
         * ユーザデータ のODataProducerを返します.
         * @param cell Cell
         * @param davCmp DavCmp
         * @return ODataProducer
         */
        public static DcODataProducer userData(final Cell cell, final DavCmp davCmp) {
            return new UserDataODataProducer(cell, davCmp);
        }

    }

}

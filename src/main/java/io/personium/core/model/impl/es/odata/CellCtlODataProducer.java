/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.model.impl.es.odata;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.odata.OEntityWrapper;

/**
 * Cell管理オブジェクトの ODataProducer.
 */
public class CellCtlODataProducer extends EsODataProducer {
    Cell cell;
    Logger log = LoggerFactory.getLogger(CellCtlODataProducer.class);

    /**
     * Constructor.
     * @param cell Cell
     */
    public CellCtlODataProducer(final Cell cell) {
        this.cell = cell;
    }

    /**
     * Obtains the service metadata for this producer.
     * @return a fully-constructed metadata object
     */
    @Override
    public EdmDataServices getMetadata() {
        return edmDataServices.build();
    }

    // スキーマ情報
    private static EdmDataServices.Builder edmDataServices = CtlSchema.getEdmDataServicesForCellCtl();

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        return null; // 必要時に実装すること
    }

    @Override
    public EntitySetAccessor getAccessorForEntitySet(final String entitySetName) {
        return EsModel.cellCtl(this.cell, entitySetName);
    }

    @Override
    public ODataLinkAccessor getAccessorForLink() {
        return EsModel.cellCtlLink(this.cell);
    }

    @Override
    public DataSourceAccessor getAccessorForLog() {
        return null;
    }

    @Override
    public DataSourceAccessor getAccessorForBatch() {
        return EsModel.batch(this.cell);
    }

    /**
     * CellのIdを返すよう実装.
     * @see io.personium.core.model.impl.es.odata.EsODataProducer#getCellId()
     * @return cell id
     */
    @Override
    public String getCellId() {
        return this.cell.getId();
    }

    @Override
    public void beforeDelete(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {

        if (!Box.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        // Boxの削除時のみ、Dav管理データを削除
        // entitySetがBoxの場合のみの処理
        EntityResponse er = this.getEntity(entitySetName, oEntityKey, new EntityQueryInfo.Builder().build());

        OEntityWrapper oew = (OEntityWrapper) er.getEntity();

        Box box = new Box(this.cell, oew);

        // このBoxが存在するときのみBoxCmpが必要
        BoxCmp davCmp = ModelFactory.boxCmp(box);
        if (!davCmp.isEmpty()) {
            throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
        }
        davCmp.delete(null, false);
        // BoxのCacheクリア
        BoxCache.clear(oEntityKey.asSingleValue().toString(), this.cell);
    }

    @Override
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        if (!Box.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        // BoxのCacheクリア
        BoxCache.clear(oEntityKey.asSingleValue().toString(), this.cell);
    }

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceEntity ソース側Entity
     * @param targetEntity ターゲット側Entity
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity) {
    }

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceDocHandler ソース側Entity
     * @param entity ターゲット側Entity
     * @param targetEntitySetName ターゲットのEntitySet名
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceDocHandler, OEntity entity, String targetEntitySetName) {
    }

    @Override
    public void onChange(String entitySetName) {
    }
}

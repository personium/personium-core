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
package io.personium.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.CellCache;
import io.personium.core.model.impl.es.doc.CellDocHandler;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.UriUtils;

/**
 * ODataProvider for unit control OData service.
 */
public class UnitCtlODataProducer extends EsODataProducer {

    static Logger log = LoggerFactory.getLogger(UnitCtlODataProducer.class);
    AccessContext accesscontext;

    /**
     * Constructor.
     * @param ac access context
     */
    public UnitCtlODataProducer(AccessContext ac) {
        this.accesscontext = ac;
    }

    //Schema information
    private static EdmDataServices.Builder edmDataServices = CtlSchema.getEdmDataServicesForUnitCtl();

    /**
     * Create and get implicit filter based on cell owner information.
     * @param entitySetName Entity set name
     * @return Implicit filter based on cell owner information
     */
    @Override
    protected List<Map<String, Object>> getImplicitFilters(String entitySetName) {
        List<Map<String, Object>> implicitFilters = new ArrayList<Map<String, Object>>();
        // If UnitUserToken or UnitLocalUnitUserToken, add the value of the subject of token to the search condition.
        if (AccessContext.TYPE_UNIT_USER.equals(this.accesscontext.getType())
                || AccessContext.TYPE_UNIT_LOCAL.equals(this.accesscontext.getType())) {
            // Search for matching owner in http format or localunit format.
            String localOwner = UriUtils.convertSchemeFromHttpToLocalUnit(accesscontext.getSubject());
            List<Map<String, Object>> orQueries = new ArrayList<Map<String, Object>>();
            orQueries.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_OWNER, accesscontext.getSubject()));
            orQueries.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_OWNER, localOwner));

            implicitFilters.add(QueryMapFactory.shouldQuery(orQueries));
        }
        return implicitFilters;
    }

    @Override
    public void afterCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        if (!Cell.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        Cell cell = ModelFactory.cellFromId(((OEntityWrapper) oEntity).getUuid());
        // Init Cell Cmp (create metadata if not exist.)
        ModelFactory.cellCmp(cell);
    }
    /**
     * Implementation subclass If you want to perform Producer update processing, implement override this to check existence of child data and return result.
     * @param entitySetName Entity set name
     * @param oEntityKey Entity key to be updated
     * @param docHandler Entity dock handler to be updated
     */
    @Override
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        CellCache.clear(oEntityKey.asSingleValue().toString());
    }

    /**
     * Implementation subclass If Producer wishes to perform deletion processing, it overrides here, checks the existence of child data, and implements it so as to return the result.
     * @param entitySetName Entity set name
     * @param oEntityKey Entity key to delete
     * @param docHandler Document to be deleted
     */
    @Override
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        CellCache.clear(oEntityKey.asSingleValue().toString());
    }

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        return null; //Implementation when necessary
    }

    @Override
    public EntitySetAccessor getAccessorForEntitySet(final String entitySetName) {
        return EsModel.unitCtl(entitySetName, EsIndex.CELL_ROUTING_KEY_NAME);
    }

    @Override
    public ODataLinkAccessor getAccessorForLink() {
        return EsModel.unitCtlLink(EsIndex.CELL_ROUTING_KEY_NAME);
    }

    @Override
    public DataSourceAccessor getAccessorForLog() {
        return null;
    }

    @Override
    public DataSourceAccessor getAccessorForBatch() {
        return EsModel.batch();
    }

    @Override
    public EdmDataServices getMetadata() {
        return edmDataServices.build();
    }

    /**
     * Get DocHandler.
     * @param searchHit Search result
     * @param entitySetName Entity set name
     * @return OEntityDocHandler
     */
    @Override
    protected EntitySetDocHandler getDocHandler(PersoniumSearchHit searchHit, String entitySetName) {
        return new CellDocHandler(searchHit);
    }

    /**
     * Get DocHandler.
     * Type of @param type elasticsearch
     * @param oEntity OEntityWrapper
     * @return OEntityDocHandler
     */
    @Override
    protected EntitySetDocHandler getDocHanlder(String type, OEntityWrapper oEntity) {
        return new CellDocHandler(type, oEntity, getMetadata());
    }

    /**
     * Get DocHandler.
     * @param response GetResponse
     * @param entitySetName Entity set name
     * @return OEntityDocHandler
     */
    @Override
    protected EntitySetDocHandler getDocHandler(PersoniumGetResponse response, String entitySetName) {
        return new CellDocHandler(response);
    }

    /**
     * Check unauthorized Link information.
     * @param sourceEntity source side Entity
     * @param targetEntity Target side Entity
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity) {
    }

    /**
     * Check unauthorized Link information.
     * @param sourceDocHandler Source side Entity
     * @param entity Target side Entity
     * @param targetEntitySetName EntitySet name of the target
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceDocHandler, OEntity entity, String targetEntitySetName) {
    }
    @Override
    public void onChange(String entitySetName) {
    }
}

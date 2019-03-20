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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.core4j.Enumerable;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.rest.RestStatus;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.core.OFunctionParameter;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmAssociation;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmFunctionImport;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.Responses;
import org.odata4j.producer.edm.MetadataProducer;
import org.odata4j.producer.exceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsBulkRequest;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.response.EsClientException.PersoniumSearchPhaseExecutionException;
import io.personium.common.es.response.PersoniumBulkItemResponse;
import io.personium.common.es.response.PersoniumBulkResponse;
import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandlerForBulkRequest;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.EsNavigationTargetKeyProperty.NTKPNotFoundException;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockManager;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyLinkType;
import io.personium.core.utils.ODataUtils;

/**
 * Producer that handles OData with ElasticSearch. Overall, as a premise that the schema check is called on the caller side, it is assumed that it will be called up. In this class, no schema checking is done because it does not perform wasteful double checking.
 * This class specializes in processing unique to dealing with OData with ElasticSearch.
 */
public abstract class EsODataProducer implements PersoniumODataProducer {

    static Logger log = LoggerFactory.getLogger(EsODataProducer.class);

    private Map<String, String> entityTypeMap = new HashMap<String, String>();
    private Map<String, PropertyAlias> propertyAliasMap = new HashMap<String, PropertyAlias>();

    /**
     * Implement in subclass to return accessor object corresponding to ES index to which entitySet name belongs.
     * @param entitySetName entitySet name
     * @return accessor object
     */
    public abstract DataSourceAccessor getAccessorForIndex(String entitySetName);

    /**
     * Implement in subclass to return accessor object corresponding to entitySet name.
     * @param entitySetName entitySet name
     * @return accessor object
     */
    public abstract EntitySetAccessor getAccessorForEntitySet(String entitySetName);

    /**
     * Implement in subclass to return accessor object that stores the link information.
     * @return accessor object
     */
    public abstract ODataLinkAccessor getAccessorForLink();

    /**
     * Implement in subclass to return accessor object that stores Log.
     * @return accessor object
     */
    public abstract DataSourceAccessor getAccessorForLog();

    /**
     * Implement in subclass to return Batch accessor object.
     * @return accessor object
     */
    public abstract DataSourceAccessor getAccessorForBatch();

    /**
     * Constructor.
     */
    public EsODataProducer() {
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific Cell, implement it so that it override here and return cellId.
     * @return CellId
     */
    public String getCellId() {
        return null;
    }

    /**
     * Implementation subclass If you want Producer to be associated with a particular Box, implement it to override here and return boxId.
     * @return getBoxId
     */
    public String getBoxId() {
        return null;
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific Node, implement it to override here and return NodeId.
     * Return @return NodeId
     */
    public String getNodeId() {
        return null;
    }

    /**
     * Get key information of Links.
     * @param entityTypeName EntityType name
     * Return key information of @return links
     */
    public String getLinkskey(String entityTypeName) {
        return entityTypeName;
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific EntityType, implement it to override here and return LinkDocHandler.
     * @param src srcEntitySetDocHandler
     * @param tgt tgtEntitySetDocHandler
     * @return LinkDocHandler
     */
    public LinkDocHandler getLinkDocHandler(EntitySetDocHandler src, EntitySetDocHandler tgt) {
        return new LinkDocHandler(src, tgt);
    }

    /**
     * Generate LinkDocHandler from the search result.
     * @param searchHit Search result
     * @return LinkDocHandler
     */
    public LinkDocHandler getLinkDocHandler(PersoniumSearchHit searchHit) {
        return new LinkDocHandler(searchHit);
    }

    /**
     * .
     * @param queryInfo .
     * @param edmEntityType .
     * @param implicitFilters .
     * @return .
     */
    protected ODataQueryHandler getODataQueryHandler(final QueryInfo queryInfo,
            EdmEntityType edmEntityType,
            List<Map<String, Object>> implicitFilters) {
        ODataQueryHandler queryHandler = new EsQueryHandler(edmEntityType);
        queryHandler.initialize(queryInfo, implicitFilters);
        return queryHandler;
    }

    /**
     * Returns the correspondence Map of property name and alias.
     * @return Correspondence between property names and aliases Map
     */
    public Map<String, String> getEntityTypeMap() {
        return entityTypeMap;
    }

    /**
     * Set correspondence map of property name and alias.
     * @param map correspondence between property name and alias Map
     */
    public void setEntityTypeMap(Map<String, String> map) {
        this.entityTypeMap = map;
    }

    /**
     * Returns the correspondence Map of property name and alias.
     * @return Correspondence between property names and aliases Map
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return propertyAliasMap;
    }

    /**
     * Set correspondence map of property name and alias.
     * @param map correspondence between property name and alias Map
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> map) {
        this.propertyAliasMap = map;
    }

    /**
     * Implementation subclass If you want to perform Producer registration processing, implement override this place and return the result.
     * @param entitySetName Entity set name
     * @param oEntity entity to be registered
     * @param docHandler Entity dock handler to register
     */
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
    }

    /**
     * Implementation subclass If you want to perform Producer update processing, implement override this to check existence of child data and return result.
     * @param entitySetName Entity set name
     * @param oEntityKey Entity key to be updated
     * @param docHandler Entity dock handler to be updated
     */
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
    }

    /**
     * Implementation subclass If Producer wishes to perform deletion processing, it overrides here, checks the existence of child data, and implements it so as to return the result.
     * @param entitySetName Entity set name
     * @param oEntityKey Entity key to delete
     * @param docHandler Document to be deleted
     */
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
    }

    /**
     * Implementation subclass If Producer wishes to perform bulk bulk registration processing, it overrides this, checks the existence of child data, and returns the result.
     * @param bulkRequestDocHandler Bulk bulk registration DocHandler
     */
    public void beforeBulkCreate(final LinkedHashMap<String, BulkRequest> bulkRequestDocHandler) {
    }

    /**
     * Implementation subclass If you want to perform Producer registration processing, implement override this place and return the result.
     * @param entitySetName Entity set name
     * @param oEntity entity to be registered
     * @param docHandler Entity dock handler to register
     */
    public void afterCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
    }

    /**
     * Implementation subclass Producer If you want to perform update processing, implement override this place and return the result.
     */
    public void afterUpdate() {
    }

    /**
     * Implementation subclass Producer If you wish to perform deletion processing, implement override this place and return the result.
     */
    public void afterDelete() {
    }

    /**
     * 1 - 0: Search processing is performed on the N side during N deletion processing.
     * @param np EdmNavigationProperty
     * @param entityKey entityKey
     * @return true if it exists
     */
    public boolean findMultiPoint(final EdmNavigationProperty np, final OEntityKey entityKey) {
        EdmAssociationEnd from = np.getFromRole();
        EdmAssociationEnd to = np.getToRole();
        if ((EdmMultiplicity.ONE.equals(from.getMultiplicity())
                || EdmMultiplicity.ZERO_TO_ONE.equals(from.getMultiplicity()))
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            //Search and check that it is 0;
            CountResponse cr = getNavPropertyCount(from.getType().getName(), entityKey, to.getType().getName(),
                    new EntityQueryInfo.Builder().build());
            return cr.getCount() > 0;
        }
        return false;
    }

    /**
     * Perform locking of OData space to ensure uniqueness designated by PK, UK.
     * @param lock
     */
    Lock lock() {
        return LockManager.getLock(Lock.CATEGORY_ODATA, this.getCellId(), null, this.getNodeId());
    }

    @Override
    public final BaseResponse callFunction(final EdmFunctionImport arg0,
            final Map<String, OFunctionParameter> arg1,
            final QueryInfo arg2) {
        // TODO V1.1 Auto-generated method stub
        return null;
    }

    /**
     * Releases any resources managed by this producer.
     */
    @Override
    public void close() {
        // TODO V1.1 Auto-generated method stub
    }

    /**
     * Obtains a single entity based on its type and key. Also honors $select and $expand in queryInfo
     * @param entitySetName the entity-set name for entities to return
     * @param entityKey the unique entity-key of the entity to start with
     * @param queryInfo the additional constraints to apply to the entities
     * @return the resulting entity
     */
    @Override
    public EntityResponse getEntity(final String entitySetName,
            final OEntityKey entityKey,
            final EntityQueryInfo queryInfo) {
        final int expandMaxNum = PersoniumUnitConfig.getMaxExpandSizeForRetrive();

        //Note) Since the existence guarantee of EntitySet is done on the calling side beforehand, it is not checked here.
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EntitySetDocHandler oedh = this.retrieveWithKey(eSet, entityKey, queryInfo);

        if (oedh == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
        }

        ExpandEntitiesMapCreator creator = new ExpandEntitiesMapCreator(queryInfo, eSet.getType(), expandMaxNum);
        Map<String, List<OEntity>> expandEntitiesMap = creator.create(oedh, this);

        //Set NavigationTargetKeyProperty
        setNavigationTargetKeyProperty(eSet, oedh);
        if (oedh instanceof OEntityDocHandler) {
            ((OEntityDocHandler) oedh).setExpandMaxNum(expandMaxNum);
        }
        //Create OEntity from ES response
        List<EntitySimpleProperty> selectQuery = null;
        if (queryInfo != null) {
            selectQuery = queryInfo.select;
        }
        OEntityWrapper entity = oedh.createOEntity(eSet, this.getMetadata(), expandEntitiesMap, selectQuery);
        //Create an Entity Response for OData.
        EntityResponse res = Responses.entity(entity);
        return res;
    }

    /**
     * get an Entity using internal id.
     * @param entitySetName entity set name
     * @param internalId internal id
     * @return OEntity object
     */
    public OEntity getEntityByInternalId(final String entitySetName,
            final String internalId) {
        final int expandMaxNum = PersoniumUnitConfig.getMaxExpandSizeForRetrive();

        //Note) Since the existence guarantee of EntitySet is done on the calling side beforehand, it is not checked here.
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EntitySetDocHandler oedh = this.retrieveWithInternalId(eSet, internalId);

        if (oedh == null) {
            return null;
        }


        //Set NavigationTargetKeyProperty
        setNavigationTargetKeyProperty(eSet, oedh);
        if (oedh instanceof OEntityDocHandler) {
            ((OEntityDocHandler) oedh).setExpandMaxNum(expandMaxNum);
        }
        //Create OEntity from ES response
        OEntityWrapper entity = oedh.createOEntity(eSet, this.getMetadata(), null, null);
        //Create an Entity Response for OData.
        return entity;
    }


    /**
     * Set NavigationTargetKeyProperty for EntitySetDocHandler.
     * @param eSet EntitySet
     * @param oedh EntitySetDocHandler
     */
    @SuppressWarnings("unchecked")
    public void setNavigationTargetKeyProperty(EdmEntitySet eSet, EntitySetDocHandler oedh) {
        //Set NavigationTargetKeyProperty
        Map<String, Object> staticFields = oedh.getStaticFields();
        Map<String, Object> links = oedh.getManyToOnelinkId();
        Enumerable<EdmProperty> eProps = eSet.getType().getProperties();
        for (EdmProperty eProp : eProps) {
            //Assemble the search information of the link target
            String propertyName = eProp.getName();
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(propertyName);
            if (links != null && ntkp != null) {
                String entityType = ntkp.get("entityType");
                String propName = ntkp.get("propName");
                String linkId = (String) links.get(getLinkskey(entityType));
                //Check if the link information is more than two levels
                while (propName.startsWith("_")) {
                    //Acquire link information
                    String id = (String) links.get(getLinkskey(entityType));
                    EntitySetAccessor esType = this.getAccessorForEntitySet(entityType);
                    PersoniumGetResponse res = esType.get(id);
                    if (res != null) {
                        //Acquire the link ID associated with link
                        HashMap<String, String> tmpntkp = AbstractODataResource.convertNTKP(propName);
                        entityType = tmpntkp.get("entityType");
                        propName = tmpntkp.get("propName");
                        linkId = (String) ((Map<String, Object>) res.getSource().get(OEntityDocHandler.KEY_LINK))
                                .get(entityType);
                    } else {
                        linkId = null;
                    }
                }
                String propValue = null;
                if (linkId != null) {
                    EntitySetAccessor esType = this.getAccessorForEntitySet(entityType);
                    PersoniumGetResponse res = esType.get(linkId);
                    if (res != null) {
                        propValue = (String) ((Map<String, Object>) res.getSource().get(
                                OEntityDocHandler.KEY_STATIC_FIELDS)).get(propName);
                    }
                }
                staticFields.put(propertyName, propValue);
            }
        }
        oedh.setStaticFields(staticFields);
    }

    /**
     * Get an entity.
     * @param entitySet entity set
     * @param oEntityKey entity key
     * @return Acquisition result
     */
    protected EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey) {
        return retrieveWithKey(entitySet, oEntityKey, null);
    }

    /**
     * Acquire one case according to the key.
     * @param entitySet EdmEntitySet
     * @param oEntityKey OEntityKey
     * @param queryInfo EntityQueryInfo
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey, EntityQueryInfo queryInfo) {
        if (entitySet == null) {
            //Schema checking is done at the top, so when EntitySet is specified as null, return it null without raising an exception.
            return null;
        }
        Set<OProperty<?>> keys = new HashSet<OProperty<?>>();
        if (KeyType.SINGLE.equals(oEntityKey.getKeyType())) {
            String name = entitySet.getType().getKeys().get(0);
            String type = entitySet.getType().findProperty(name).getType().getFullyQualifiedTypeName();
            OProperty<?> p = OProperties.parseSimple(name, type, String.valueOf(oEntityKey.asSingleValue()));
            keys.add(p);
        } else {
            Set<OProperty<?>> ks = oEntityKey.asComplexProperties();
            for (OProperty<?> k : ks) {
                keys.add(k);
            }
        }

        //If the compound key is omitted, set the dummy key
        List<String> schemaKeys = entitySet.getType().getKeys();
        for (String schemaKey : schemaKeys) {
            boolean existSchemaKey = false;
            for (OProperty<?> k : keys) {
                if (k.getName().equals(schemaKey)) {
                    existSchemaKey = true;
                    break;
                }
            }
            if (!existSchemaKey) {
                //TODO It is an error if it is not an optional field
                String type = entitySet.getType().findProperty(schemaKey).getType().getFullyQualifiedTypeName();
                OProperty<?> p = OProperties.parseSimple(schemaKey, type, AbstractODataResource.DUMMY_KEY);
                keys.add(p);
            }
        }

        try {
            return this.retrieveWithKey(entitySet, keys, queryInfo);
        } catch (NTKPNotFoundException e) {
            return null;
        }
    }

    /**
     * Acquire one case according to Key.
     * @param entitySet EdmEntitySet
     * @param keys Map<String, OProperty>
     * @param queryInfo queryInfo
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler retrieveWithKey(
            EdmEntitySet entitySet, Set<OProperty<?>> keys, EntityQueryInfo queryInfo) {
        //The logic of the method creates a query, searches for it, and fills the result and returns it.

        //1. Querying ES
        String entitySetName = entitySet.getName();

        //The ES queries commonly have the following structure {"filter": SOME_FILTER}
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();

        if (keys.size() == 1) {
            Map<String, Object> query = new HashMap<String, Object>();

            if (this.getCellId() == null) {
                //When it is not associated with a specific CELL
                //When Key is SINGLE it becomes the following form
                query = QueryMapFactory.filteredQuery(null, QueryMapFactory.termQuery(
                        OEntityDocHandler.KEY_STATIC_FIELDS + "." + keys.iterator().next().getName() + ".untouched",
                        keys.iterator().next().getValue()));
            } else {
                query = QueryMapFactory.filteredQuery(null,
                        QueryMapFactory.mustQuery(getImplicitFilters(entitySetName)));

                //When linked to specific CELL When single key
                Map<String, Object> and = new HashMap<String, Object>();
                List<Object> filters = new ArrayList<Object>();
                filter.put("and", and);
                and.put("filters", filters);

                Map<String, Object> termKey = new HashMap<String, Object>();
                Map<String, Object> key = new HashMap<String, Object>();
                key.put("term", termKey);
                filters.add(key);

                termKey.put(OEntityDocHandler.KEY_STATIC_FIELDS + "." + keys.iterator().next().getName()
                        + ".untouched", keys.iterator().next().getValue());
            }

            source.put("size", 1);
            source.put("version", true);
            source.put("filter", filter);
            source.put("query", query);
        } else {
            //Compound key
            EsNavigationTargetKeyProperty esNtkp = new EsNavigationTargetKeyProperty(this.getCellId(),
                    this.getBoxId(), this.getNodeId(), entitySetName, this);
            esNtkp.setProperties(keys);
            source = esNtkp.getNtkpSearchQuery();
        }

        if (queryInfo != null) {
            ODataQueryHandler queryHandler = getODataQueryHandler(queryInfo, entitySet.getType(),
                    getImplicitFilters(entitySetName));
            queryHandler.getSelectQuery(source, queryInfo.select);
        }

        //2. Request to ES
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        PersoniumSearchResponse res = esType.search(source);

        //3. Evaluation of response from ES
        //In the case of Index fresh, res becomes null. Since there is no data, return null.
        if (res == null) {
            return null;
        }
        PersoniumSearchHits hits = res.getHits();
        //If the number of hits 0 is not done, it returns Null
        if (hits.getCount() == 0) {
            return null;
        }
        //When two or more pieces of data return, abnormal situation
        if (hits.getAllPages() > 1) {
            PersoniumCoreLog.OData.FOUND_MULTIPLE_RECORDS.params(hits.getAllPages()).writeLog();
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(new RuntimeException(
                    "multiple records (" + hits.getAllPages() + ") found for the key ."));
        }
        //Here it is sunny and the number of hits is guaranteed to be 1, so return 1 case.
        return getDocHandler(hits.getHits()[0], entitySetName);
    }
    private EntitySetDocHandler retrieveWithInternalId(EdmEntitySet eSet, String internalId) {
        EntitySetAccessor esType = this.getAccessorForEntitySet(eSet.getName());
        PersoniumGetResponse getRes = esType.get(internalId);
        if (getRes == null) {
            return null;
        }
        return this.getDocHandler(getRes, eSet.getName());
    }


    /**
     * Get DocHandler.
     * @param searchHit Search result
     * @param entitySetName Entity set name
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHandler(PersoniumSearchHit searchHit, String entitySetName) {
        return new OEntityDocHandler(searchHit);
    }

    /**
     * Get DocHandler.
     * Type of @param type elasticsearch
     * @param oEntity OEntityWrapper
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHanlder(String type, OEntityWrapper oEntity) {
        return new OEntityDocHandler(type, oEntity, this.getMetadata());
    }

    /**
     * Get DocHandler.
     * @param response GetResponse
     * @param entitySetName Entity set name
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHandler(PersoniumGetResponse response, String entitySetName) {
        return new OEntityDocHandler(response);
    }

    /**
     * Get a DocHandler for updating.
     * Type of @param type elasticsearch
     * @param oEntityWrapper OEntityWrapper
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getUpdateDocHanlder(String type, OEntityWrapper oEntityWrapper) {
        return getDocHanlder(type, oEntityWrapper);
    }

    /**
     * Perform one acquisition.
     * @param oEntityId OEntityId
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler retrieveWithKey(OEntityId oEntityId) {
        EdmEntitySet entitySet = this.getMetadata().findEdmEntitySet(oEntityId.getEntitySetName());
        return this.retrieveWithKey(entitySet, oEntityId.getEntityKey());
    }

    /**
     * Gets all the entities for a given set matching the query information.
     * @param entitySetName the entity-set name for entities to return
     * @param queryInfo the additional constraints to apply to the entities
     * @return a packaged collection of entities to pass back to the client
     */
    @Override
    public EntitiesResponse getEntities(final String entitySetName, final QueryInfo queryInfo) {
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        return getEntities(entitySetName, queryInfo, eSet);
    }

    /**
     * Gets all the entities for a given set matching the query information.
     * @param entitySetName the entity-set name for entities to return
     * @param queryInfo the additional constraints to apply to the entities
     * @param eSet entity set
     * @return a packaged collection of entities to pass back to the client
     */
    public EntitiesResponse getEntities(final String entitySetName, final QueryInfo queryInfo, EdmEntitySet eSet) {
        //Note) Since the existence guarantee of EntitySet is done on the calling side beforehand, it is not checked here.
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        //Create Implicit Filter based on Cell / Box / Node / EntityType
        List<Map<String, Object>> implicitFilters = getImplicitFilters(entitySetName);
        //Pass implicitFIlters and perform a search
        return execEntitiesRequest(queryInfo, eSet, esType, implicitFilters);
    }

    /**
     * Create an implicit filter based on Cell / Box / Node / EntityType.
     * @param entitySetName Entity set name
     * @return Implicit filter based on Cell / Box / Node / EntityType
     */
    protected List<Map<String, Object>> getImplicitFilters(String entitySetName) {
        String cellId = this.getCellId();
        String boxId = this.getBoxId();
        String nodeId = this.getNodeId();
        String entityTypeId = getEntityTypeId(entitySetName);
        return QueryMapFactory.getImplicitFilters(cellId, boxId, nodeId, entityTypeId, entitySetName);
    }

    /**
     * Perform list retrieval.
     * @param queryInfo query information
     * @param eSet entity set
     * @param esType accessor object
     * @param implicitFilters Implicit search condition
     * @return EntitiesResponse entity list
     */
    public EntitiesResponse execEntitiesRequest(final QueryInfo queryInfo,
            EdmEntitySet eSet,
            EntitySetAccessor esType,
            List<Map<String, Object>> implicitFilters) {
        final int expandMaxNum = PersoniumUnitConfig.getMaxExpandSizeForList();

        //Conditional search etc.
        ODataQueryHandler visitor = getODataQueryHandler(queryInfo, eSet.getType(), implicitFilters);
        Map<String, Object> source = visitor.getSource();

        PersoniumSearchResponse res = null;
        try {
            res = esType.search(source);
        } catch (EsClientException ex) {
            if (ex.getCause() instanceof PersoniumSearchPhaseExecutionException) {
                SearchPhaseExecutionException speex = (SearchPhaseExecutionException) ex.getCause().getCause();
                if (speex.status().equals(RestStatus.BAD_REQUEST)) {
                    throw PersoniumCoreException.OData.SEARCH_QUERY_INVALID_ERROR.reason(ex);
                } else {
                    throw PersoniumCoreException.Server.DATA_STORE_SEARCH_ERROR.reason(ex);
                }
            }
        }
        //Returns the number of hits only when the inlinecount specification is allpages
        Integer count = null;
        if (queryInfo != null && queryInfo.inlineCount != null && queryInfo.inlineCount.equals(InlineCount.ALLPAGES)) {
            if (res == null) {
                count = 0;
            } else {
                count = (int) res.getHits().getAllPages();
            }
        }
        List<OEntity> entList = new ArrayList<OEntity>();
        if (res != null) {
            PersoniumSearchHit[] hits = res.getHits().getHits();
            Map<String, List<OEntity>> expandEntitiesMap = null;

            Map<String, String> ntkpProperties = new HashMap<String, String>();
            Map<String, String> ntkpValueMap = new HashMap<String, String>();
            getNtkpValueMap(eSet, ntkpProperties, ntkpValueMap);

            List<EntitySimpleProperty> selectQuery = null;
            if (queryInfo != null) {
                selectQuery = queryInfo.select;
            }

            //Create Property / ComplexTypeProperty and Alias mapping data
            //Also, create mapping data of UUID and name of EntityType / ComplexType (
            if (this.propertyAliasMap != null) {
                setEntityPropertyMap(eSet, hits, ntkpValueMap);
            }
            List<EntitySetDocHandler> entityList = new ArrayList<EntitySetDocHandler>();
            for (PersoniumSearchHit hit : hits) {
                EntitySetDocHandler oedh = getDocHandler(hit, eSet.getName());
                entityList.add(oedh);
            }
            ExpandEntitiesMapCreator creator =
                    new ExpandEntitiesMapCreator(queryInfo, eSet.getType(), expandMaxNum);
            creator.setCache(entityList, this);

            for (EntitySetDocHandler oedh : entityList) {
                Map<String, Object> staticFields = oedh.getStaticFields();
                if (staticFields == null) {
                    continue;
                }

                expandEntitiesMap = creator.create(oedh, this);

                //Set values from NTKPHashMap
                Map<String, Object> links = oedh.getManyToOnelinkId();
                for (Map.Entry<String, String> ntkpProperty : ntkpProperties.entrySet()) {
                    String linksKey = getLinkskey(ntkpProperty.getValue());
                    if (links != null && links.containsKey(linksKey)) {
                        String linkId = links.get(linksKey).toString();
                        staticFields.put(ntkpProperty.getKey(), ntkpValueMap.get(ntkpProperty.getKey() + linkId));
                    }
                }
                oedh.setStaticFields(staticFields);

                ((OEntityDocHandler) oedh).setExpandMaxNum(expandMaxNum);
                OEntityWrapper oEntity = oedh.createOEntity(eSet, this.getMetadata(), expandEntitiesMap, selectQuery);
                entList.add(oEntity);
                setEntityTypeIds(oEntity, staticFields);
            }
        }
        return Responses.entities(entList, eSet, count, null);
    }

    /**
     * Based on the search result, map properties and Alias.
     * @param eSet EdmEntitySet
     * @param hits Search results
     * @param ntkpValueMap NTKP map
     */
    @SuppressWarnings("unchecked")
    private void setEntityPropertyMap(EdmEntitySet eSet, PersoniumSearchHit[] hits, Map<String, String> ntkpValueMap) {
        String entityTypeKey;
        String linkTypeName;
        if (Property.EDM_TYPE_NAME.equals(eSet.getName())) {
            linkTypeName = EntityType.EDM_TYPE_NAME;
            entityTypeKey = Property.P_ENTITYTYPE_NAME.getName();
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(eSet.getName())) {
            linkTypeName = ComplexType.EDM_TYPE_NAME;
            entityTypeKey = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
        } else {
            //Do nothing other than Property / ComplexTypeProperty.
            return;
        }
        this.entityTypeMap.putAll(ntkpValueMap);

        List<String> processedPropertyAlias = new ArrayList<String>();
        for (PersoniumSearchHit property : hits) {
            Map<String, Object> fields = property.getSource();
            Map<String, Object> staticFileds = (Map<String, Object>) fields.get(OEntityDocHandler.KEY_STATIC_FIELDS);
            Map<String, Object> hideenFileds = (Map<String, Object>) fields.get(OEntityDocHandler.KEY_HIDDEN_FIELDS);
            Map<String, Object> linkFileds = (Map<String, Object>) fields.get(OEntityDocHandler.KEY_LINK);

            String propertyName = (String) staticFileds.get("Name");
            String propertyType = (String) staticFileds.get("Type");
            String propertyAlias = (String) hideenFileds.get("Alias");
            String entityTypeId = (String) linkFileds.get(linkTypeName);

            String entityTypeName = ntkpValueMap.get(entityTypeKey + entityTypeId);
            String key = "Name='" + propertyName + "'," + entityTypeKey + "='" + entityTypeName + "'";
            if (processedPropertyAlias.contains(key)) {
                //Detect duplicate property names
                PersoniumCoreLog.OData.DUPLICATED_PROPERTY_NAME.params(entityTypeId, key).writeLog();
                throw PersoniumCoreException.OData.DUPLICATED_PROPERTY_NAME.params(propertyName);
            }
            processedPropertyAlias.add(key);
            this.propertyAliasMap.put(key, new PropertyAlias(linkTypeName, propertyName, propertyType, propertyAlias));
        }
    }

    /**
     * Set mapping data of EntityType name and UUID.
     * @param oEntity oEntity
     * @param staticFields staticFields
     */
    public void setEntityTypeIds(OEntityWrapper oEntity, Map<String, Object> staticFields) {
    }

    /**
     * Get mapping data of EntityType name and UUID.
     * @return the entityTypeIds
     */
    public Map<String, String> getEntityTypeIds() {
        return null;
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific EntityType, implement it to override here and return EntityTypeId.
     * @param entityTypeName EntityType name
     * Return @return EntityTypeId
     */
    public String getEntityTypeId(final String entityTypeName) {
        return null;
    }

    /**
     * Get NtkpValueMap from EntitySet and set it as an object of argument.
     * @param eSet EntitySet
     * @param ntkpProperties NtkpProperties
     * @param ntkpValueMap NtkpValueMap
     */
    @SuppressWarnings("unchecked")
    protected void getNtkpValueMap(EdmEntitySet eSet,
            Map<String, String> ntkpProperties,
            Map<String, String> ntkpValueMap) {
        Enumerable<EdmProperty> eProps = eSet.getType().getProperties();
        for (EdmProperty eProp : eProps) {
            //Assemble the search information of the link target
            String propertyName = eProp.getName();
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(propertyName);
            if (ntkp != null) {
                String entityType = ntkp.get("entityType");
                String propName = ntkp.get("propName");
                ntkpProperties.put(propertyName, entityType);

                //Acquire the entity list of NTKP at the first level
                EntitySetAccessor ntkpAccessor = this.getAccessorForEntitySet(entityType);
                //Add search condition of Cell, Box, NodeID
                List<Map<String, Object>> implicitFilters = getImplicitFilters(entityType);
                Map<String, Object> searchQuery = new HashMap<String, Object>();
                if (implicitFilters.size() != 0) {
                    Map<String, Object> query = QueryMapFactory.filteredQuery(null,
                            QueryMapFactory.mustQuery(implicitFilters));

                    searchQuery.put("query", query);
                }

                PersoniumSearchHit[] ntkpSearchResults = ntkpAccessor.search(searchQuery).getHits().getHits();

                //When NTKP of the second hierarchy exists, the entity list of the second hierarchy NTKP is acquired
                PersoniumSearchHit[] nestNtkpSearchResults = null;
                Map<String, String> nestNtkpValueMap = new HashMap<String, String>();
                String linkedEntityType = null;
                String linkedPropName = null;
                if (propName.startsWith("_")) {
                    HashMap<String, String> tmpntkp = AbstractODataResource.convertNTKP(propName);
                    linkedEntityType = tmpntkp.get("entityType");
                    linkedPropName = tmpntkp.get("propName");
                    ntkpAccessor = this.getAccessorForEntitySet(linkedEntityType);
                    nestNtkpSearchResults = ntkpAccessor.search(searchQuery).getHits().getHits();
                    for (PersoniumSearchHit nestNtkpSearchResult : nestNtkpSearchResults) {
                        String linkId = nestNtkpSearchResult.getId();
                        String linkNtkpValue = ((Map<String, Object>) nestNtkpSearchResult.getSource().get(
                                OEntityDocHandler.KEY_STATIC_FIELDS)).get(linkedPropName).toString();
                        nestNtkpValueMap.put(linkId, linkNtkpValue);
                    }
                }

                //Create Map with LinkID as Key and NTKP Value as Value
                for (PersoniumSearchHit ntkpSearchResult : ntkpSearchResults) {
                    String linkId = ntkpSearchResult.getId();
                    Map<String, Object> linkFields = (Map<String, Object>) ntkpSearchResult.getSource().get(
                            OEntityDocHandler.KEY_LINK);
                    String linkNtkpValue = null;
                    if (linkedEntityType != null) {
                        linkNtkpValue = nestNtkpValueMap.get(linkFields.get(linkedEntityType));
                    } else {
                        Map<String, Object> staticFields = (Map<String, Object>) ntkpSearchResult.getSource().get(
                                OEntityDocHandler.KEY_STATIC_FIELDS);
                        linkNtkpValue = staticFields.get(propName).toString();
                    }
                    ntkpValueMap.put(getLinkskey(propertyName) + linkId, linkNtkpValue);
                }
            }
        }
    }

    /**
     * Deletes an existing entity.
     * @param entitySetName the entity-set name of the entity
     * @param entityKey the entity-key of the entity
     */
    @Override
    public void deleteEntity(final String entitySetName, final OEntityKey entityKey) {
        this.deleteEntity(entitySetName, entityKey, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEntity(String entitySetName, OEntityKey entityKey, String etag) {
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        // Since the existence guarantee of EntitySet is done on the caller side in advance, it is not checked here.
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);

        // Lock OData space.
        Lock lock = this.lock();
        try {
            deleteEntity(entitySetName, entityKey, etag, eSet, esType);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Delete entity without lock.
     * @param entitySetName the entity-set name of the entity
     * @param entityKey the entity-key of the entity
     */
    protected void deleteEntityWithoutLock(final String entitySetName, final OEntityKey entityKey) {
        this.deleteEntityWithoutLock(entitySetName, entityKey, null);
    }

    /**
     * Delete entity without lock.
     * @param entitySetName the entity-set name of the entity
     * @param entityKey the entity-key of the entity
     * @param etag etag
     */
    protected void deleteEntityWithoutLock(String entitySetName, OEntityKey entityKey, String etag) {
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        // Since the existence guarantee of EntitySet is done on the caller side in advance, it is not checked here.
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);

        deleteEntity(entitySetName, entityKey, etag, eSet, esType);
    }

    /** Delete Entity. */
    private void deleteEntity(String entitySetName, OEntityKey entityKey, String etag,
            EdmEntitySet eSet, EntitySetAccessor esType) {
        EdmEntityType srcType = eSet.getType();

        //Acquire ES id for checking the existence of records & deleting
        EntitySetDocHandler hit = this.retrieveWithKey(eSet, entityKey);

        if (hit == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
        }
        // Check if the value of If-Match header and Etag are equal.
        ODataUtils.checkEtag(etag, hit);

        // Search for N side link of 1-0:N
        for (EdmNavigationProperty np : srcType.getDeclaredNavigationProperties().toList()) {
            if (this.findMultiPoint(np, entityKey)) {
                throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
            }
        }

        // Delete link
        // N:N
        for (EdmNavigationProperty np : srcType.getDeclaredNavigationProperties().toList()) {
            deleteLinks(np, hit);
        }
        // N:1
        Map<String, Object> target = hit.getManyToOnelinkId();
        for (Entry<String, Object> entry : target.entrySet()) {
            String key = entry.getKey();
            EntitySetAccessor targetEsType = this.getAccessorForEntitySet(key);

            // Get linked entity
            PersoniumGetResponse linksRes = targetEsType.get(entry.getValue().toString());
            EntitySetDocHandler linksDocHandler = getDocHandler(linksRes, entitySetName);
            Map<String, Object> links = linksDocHandler.getManyToOnelinkId();

            // When the acquired data has the link information, delete the link information and update the data.
            String linksKey = getLinkskey(entitySetName);
            if (links.containsKey(linksKey)) {
                links.remove(linksKey);
                linksDocHandler.setManyToOnelinkId(links);
                targetEsType.update(entry.getValue().toString(), linksDocHandler);
            }
        }

        // Befor delete
        this.beforeDelete(entitySetName, entityKey, hit);
        PersoniumDeleteResponse res = null;
        // Delete
        res = esType.delete(hit);

        if (res == null) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(new RuntimeException("not found"));
        }
        // If retry processing is done within TransportClient, NotFound is returned as a response.
        // Therefore, even if NotFound is returned, it is regarded as normal termination.
        if (res.isNotFound()) {
            log.info("Request data is already deleted. Then, return success response.");
        }

        // After delete
        this.afterDelete();
    }

    /**
     * EntitySetDocHandler is generated from EntitySet name and OEntity and acquired.
     * @param entitySetName EntitySet name
     * @param entity OEntity
     * @return EntitySetDocHandler
     */
    @Override
    public EntitySetDocHandler getEntitySetDocHandler(final String entitySetName, final OEntity entity) {
        //Generate DocHandler
        OEntityKey entityKey = entity.getEntityKey();
        EntitySetDocHandler docHandler = getDocHanlder(entitySetName, (OEntityWrapper) entity);

        //Link Cell, Box, Node
        docHandler.setCellId(this.getCellId());
        docHandler.setBoxId(this.getBoxId());
        docHandler.setNodeId(this.getNodeId());

        //If there is an NTKP item (ex. _EntityType.Name) with a compound key, link information is set
        if (KeyType.COMPLEX.equals(entityKey.getKeyType())) {
            setLinksFromOEntity(entity, docHandler);
        }
        return docHandler;
    }

    /**
     * Entity creation processing, also performs uniqueness check by primary key and UK.In case of uniqueness problem raises an exception.
     * @param entitySetName the entity-set name
     * @param entity the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties
     */
    @Override
    public EntityResponse createEntity(final String entitySetName, final OEntity entity) {
        OEntityKey entityKey = entity.getEntityKey();
        //Convert from OEntity of request to JSONObject of the form registered in elasticsearch
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        OEntityWrapper oew = (OEntityWrapper) entity;

        //Lock first for uniqueness check
        //Lock the whole OData space (lock it with entitySetName if necessary for future)
        Lock lock = this.lock();
        try {
            return createEntity(entitySetName, entity, entityKey, esType, oew);
        } finally {
            //Unlock
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Create Entity without lock.
     * @param entitySetName the entity-set name
     * @param entity the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties
     */
    protected EntityResponse createEntityWithoutLock(final String entitySetName, final OEntity entity) {
        OEntityKey entityKey = entity.getEntityKey();
        // convert OEntity to JSONObject in order to register to elasticsearch.
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        OEntityWrapper oew = (OEntityWrapper) entity;

        return createEntity(entitySetName, entity, entityKey, esType, oew);
    }

    /** Create Entity. */
    private EntityResponse createEntity(final String entitySetName,
            final OEntity entity,
            OEntityKey entityKey,
            EntitySetAccessor esType,
            OEntityWrapper oew) {
        checkUniqueness(entitySetName, oew);

        EntitySetDocHandler oedh = getDocHanlder(esType.getType(), oew);
        //Pegged with Cell, Box, Node
        oedh.setCellId(this.getCellId());
        oedh.setBoxId(this.getBoxId());
        oedh.setNodeId(this.getNodeId());
        oedh.setEntityTypeId(this.getEntityTypeId(entitySetName));

        //If there is an NTKP item (ex. _EntityType.Name) with a compound key, link information is set
        if (KeyType.COMPLEX.equals(entityKey.getKeyType())) {
            try {
                setLinksFromOEntity(entity, oedh);
            } catch (NTKPNotFoundException e) {
                throw PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
            }
        }

        //Pre-registration process
        this.beforeCreate(entitySetName, entity, oedh);

        //If data does not exist, save esJson in ES
        PersoniumIndexResponse idxRs = null;
        idxRs = esType.create(oedh.getId(), oedh);

        //Post registration process
        this.afterCreate(entitySetName, entity, oedh);

        Long version = idxRs.getVersion();
        oedh.setVersion(version);
        String etag = oedh.createEtag();
        oew.setEtag(etag);
        oew.setUuid(idxRs.getId());
        return Responses.entity(oew);
    }

    /**
     * Perform uniqueness check of data.
     * @param entitySetName entity name
     * @param oew Entity to register / update newly
     */
    protected void checkUniqueness(String entitySetName, OEntityWrapper oew) {
        ODataProducerUtils.checkUniqueness(this, oew, null, null);
    }

    /**
     * If there is an item of NTKP in OEntity, link information is set.
     * @param entity Request information OEntity
     * @param oedh document handler for registered data
     * @throws NTKPNotFoundException The resource specified by NTKP does not exist
     */
    private void setLinksFromOEntity(final OEntity entity, EntitySetDocHandler oedh) throws NTKPNotFoundException {
        //Based on the Property of EntityKey, obtain link information
        Set<OProperty<?>> properties = entity.getEntityKey().asComplexProperties();
        EsNavigationTargetKeyProperty esNtkp = new EsNavigationTargetKeyProperty(this.getCellId(), this.getBoxId(),
                this.getNodeId(), entity.getEntityType().getName(), this);
        setLinksForOedh(properties, esNtkp, oedh);
    }

    /**
     * If there is an item of NTKP in Properties, link information is set.
     * @param properties OEntity key properties
     * @param esNtkp NavigationTargetKeyProperty
     * @param oedh Document handler for registration data
     * @throws NTKPNotFoundException The resource specified by NTKP does not exist
     */
    protected void setLinksForOedh(Set<OProperty<?>> properties, EsNavigationTargetKeyProperty esNtkp,
            EntitySetDocHandler oedh)  throws NTKPNotFoundException {
        esNtkp.setProperties(properties);
        Map.Entry<String, String> link = esNtkp.getLinkEntry();

        //Delete NTKP information from OEDH
        for (OProperty<?> property : properties) {
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(property.getName());
            if (ntkp != null) {
                oedh.getStaticFields().remove(property.getName());
            }
        }

        //If link information is not set, empty the link information
        if (link == null) {
            oedh.setManyToOnelinkId(new HashMap<String, Object>());
            return;
        }
        //If it exists, link information is added to the registration Entity
        //Example) When registering AssociationEnd "l": {"EntityType": "UUID of EntityType"}
        Map<String, Object> links = oedh.getManyToOnelinkId();
        links.put(link.getKey(), link.getValue());
        oedh.setManyToOnelinkId(links);
    }

    /**
     * Creates a new OData entity as a reference of an existing entity, implicitly linked to the existing entity by a
     * navigation property.
     * @param entitySetName the entity-set name of the existing entity
     * @param entityKey the entity-key of the existing entity
     * @param navProp the navigation property off of the existing entity
     * @param entity the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties, and linked to the existing
     *         entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingnewEntries">[odata.org] Creating new
     *      Entries</a>
     */
    @Override
    public final EntityResponse createEntity(final String entitySetName,
            final OEntityKey entityKey,
            final String navProp,
            final OEntity entity) {
        //Implement data creation via NavigationProperty.
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        //Lock the whole OData space (lock it with entitySetName if necessary for future)
        Lock lock = this.lock();
        try {
            //Confirm that src side paste existence. Search with primary key.
            EntitySetDocHandler srcDh = this.retrieveWithKey(eSet, entityKey);
            if (srcDh == null) {
                //If src side paste source does not exist, it is set as 404 error
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
            }
            //Determine the type of association (NN / 1N / N1 / 11)

            //Create tgt side record
            //String of src and tgt
            //TODO implementation incomplete
            return null;
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Creates a link between two entities.
     * @param sourceEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param targetEntity the link target entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingLinksbetweenEntries">[odata.org]
     *      Creating Links between Entries</a>
     */
    @Override
    public final void createLink(OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        String srcSetName = sourceEntity.getEntitySetName();

        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);
        if (srcNavProp == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        //Isolate n: 1 or n: n
        EdmAssociation assoc = srcNavProp.getRelationship();
        //Lock first for uniqueness check
        //Lock the whole OData space (lock it with entitySetName if necessary for future)
        Lock lock = this.lock();
        try {
            createLink(sourceEntity, targetEntity, srcNavProp, assoc);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Create a link between two entities without lock.
     * @param sourceEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param targetEntity the link target entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingLinksbetweenEntries">[odata.org]
     *      Creating Links between Entries</a>
     */
    protected void createLinkWithoutLock(OEntityId sourceEntity, String targetNavProp, OEntityId targetEntity) {
        String srcSetName = sourceEntity.getEntitySetName();

        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);
        if (srcNavProp == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        //Isolate n: 1 or n: n
        EdmAssociation assoc = srcNavProp.getRelationship();

        createLink(sourceEntity, targetEntity, srcNavProp, assoc);
    }

    // internnal method of createLink
    private void createLink(OEntityId sourceEntity, OEntityId targetEntity,
            EdmNavigationProperty srcNavProp, EdmAssociation assoc) {
        EntitySetDocHandler src = this.retrieveWithKey(sourceEntity);
        //If there is no data 404
        if (src == null) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }
        EntitySetDocHandler tgt = this.retrieveWithKey(targetEntity);
        //If the target does not exist 400
        if (tgt == null) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }
        createLinks(sourceEntity, srcNavProp, assoc, src, tgt);
    }

    /**
     * Check the prerequisites for data registration, such as checking the link source / destination data or already creating the link.
     * @param navigationPropertyContext Context for registration via NP
     */
    private void validateLinkForNavigationPropertyContext(NavigationPropertyBulkContext navigationPropertyContext) {
        OEntityId sourceEntity = navigationPropertyContext.getSrcEntityId();
        OEntity targetEntity = navigationPropertyContext.getOEntityWrapper();

        String targetNavProp = navigationPropertyContext.getTgtNavProp();
        String srcSetName = sourceEntity.getEntitySetName();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);

        if (srcNavProp == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        EntitySetDocHandler src = navigationPropertyContext.getSourceDocHandler();
        //If there is no data 404
        if (src == null) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }

        //Since 1: 1 association can not exist, if you try to register 1: 1 with $ links of AssociationEnd - AssociationEnd it is an error
        String targetEntitySetName = targetEntity.getEntitySetName();
        checkAssociationEndMultiplicity(targetNavProp, targetEntity, targetEntitySetName, src);

        //Registered check of $ links
        checkExistsLink(sourceEntity, srcNavProp, src, targetEntity);
    }

    /**
     * Set the link information via NP to the context.
     * @param navigationPropertyContext Context for registration via NP
     */
    private void setNavigationPropertyContext(NavigationPropertyBulkContext navigationPropertyContext) {
        OEntityId sourceEntity = navigationPropertyContext.getSrcEntityId();

        String srcSetName = sourceEntity.getEntitySetName();
        String targetNavProp = navigationPropertyContext.getTgtNavProp();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);

        EntitySetDocHandler sourceDocHandler = navigationPropertyContext.getSourceDocHandler();
        EntitySetDocHandler targetDocHandler = navigationPropertyContext.getTargetDocHandler();

        //Set link information via NP to context
        if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.manyToMany) {
            //In the case of n: n
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceDocHandler, targetDocHandler);

            //Set docHandler for link in context.
            navigationPropertyContext.setLinkDocHandler(docHandler);
        } else if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.oneToOne) {
            //In case of 1: 1 / 0. 1: 1/1: 0..1 / 0..1: 0..1
            String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
            String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
            Map<String, Object> sourceLinks = sourceDocHandler.getManyToOnelinkId();
            Map<String, Object> targetLinks = targetDocHandler.getManyToOnelinkId();

            //Update link information
            sourceLinks.put(toLinksKey, targetDocHandler.getId());
            sourceDocHandler.setManyToOnelinkId(sourceLinks);

            //Convert Alias ​​to property name
            sourceDocHandler.convertAliasToName(getMetadata());

            //When Propety is created via NP, the version is updated, so get the version
            targetLinks.put(fromLinksKey, sourceDocHandler.getId());
            targetDocHandler.setManyToOnelinkId(targetLinks);

            //Convert Alias ​​to property name
            targetDocHandler.convertAliasToName(getMetadata());

            //Set docHandler for link in context.
            navigationPropertyContext.setSourceDocHandler(sourceDocHandler);
            navigationPropertyContext.setTargetDocHandler(targetDocHandler);
        } else {
            String fromEntitySetName = srcNavProp.getFromRole().getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.oneToMany) {
                //1: n / 0. 1: n
                String fromLinksKey = getLinkskey(fromEntitySetName);
                Map<String, Object> links = new HashMap<String, Object>();

                //Create a link object for registration and set it as an object to be registered
                links = targetDocHandler.getManyToOnelinkId();
                links.put(fromLinksKey, sourceDocHandler.getId());
                targetDocHandler.setManyToOnelinkId(links);

                //Convert Alias ​​to property name
                targetDocHandler.convertAliasToName(getMetadata());

                //Set docHandler for link in context.
                navigationPropertyContext.setTargetDocHandler(targetDocHandler);
            } else {
                //In the case of n: 1 / n: 0..1
                String toLinksKey = getLinkskey(toEntitySetName);
                Map<String, Object> links = new HashMap<String, Object>();

                //Create a link object for registration and set it as an object to be registered
                links = sourceDocHandler.getManyToOnelinkId();
                links.put(toLinksKey, targetDocHandler.getId());
                sourceDocHandler.setManyToOnelinkId(links);

                //Convert Alias ​​to property name
                sourceDocHandler.convertAliasToName(getMetadata());

                //Set docHandler for link in context.
                navigationPropertyContext.setSourceDocHandler(sourceDocHandler);
            }
        }
    }

    /**
     * Get the Edm (schema) of NavigationProeprty.
     * @param entitySetName EntitySet name to be acquired
     * @param navigationPropertyName Name of the NavigationProperty to be acquired
     * @return NavigationProeprty's Edm (schema)
     */
    private EdmNavigationProperty getEdmNavigationProperty(String entitySetName, String navigationPropertyName) {
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EdmEntityType srcType = srcSet.getType();
        EdmNavigationProperty srcNavProp = srcType.findNavigationProperty(navigationPropertyName);
        return srcNavProp;
    }

    /**
     * Creates a link between two entities.
     * @param sourceOEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param entity entity
     * @param targetEntitySetName targetEntitySetName
     * @return etag etag
     * @see <a href="http://www.odata.org/developers/protocols/operations#CreatingLinksbetweenEntries">[odata.org]
     *      Creating Links between Entries</a>
     */
    public final EntityResponse createNp(OEntityId sourceOEntity,
            String targetNavProp, OEntity entity, String targetEntitySetName) {
        //Schema check
        //From srcType to tgtType Check if N: N Assoc is defined
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(sourceOEntity.getEntitySetName());
        EdmEntityType srcType = srcSet.getType();
        EdmNavigationProperty srcNavProp = srcType.findNavigationProperty(targetNavProp);
        if (srcNavProp == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        EntityResponse res;
        //Lock first for uniqueness check
        //Lock the whole OData space (lock it with entitySetName if necessary for future)
        Lock lock = this.lock();

        try {

            //If there is no source data 404
            EntitySetDocHandler sourceDocHandler = this.retrieveWithKey(sourceOEntity);
            if (sourceDocHandler == null) {
                throw PersoniumCoreException.OData.NOT_FOUND;
            }
            EntitySetDocHandler targetEntity = this.retrieveWithKey(entity);
            if (targetEntity != null) {
                throw PersoniumCoreException.OData.CONFLICT_LINKS;
            }

            //Since 1: 1 association can not exist, if you try to register 1: 1 with $ links of AssociationEnd - AssociationEnd it is an error
            checkAssociationEndMultiplicity(targetNavProp, entity, targetEntitySetName, sourceDocHandler);

            checkInvalidLinks(sourceDocHandler, entity, targetEntitySetName);

            //Registered check of $ links
            checkExistsLink(sourceOEntity, srcNavProp, sourceDocHandler, entity);

            //$ links high limit check
            checkUpperLimitRecord(srcNavProp, sourceDocHandler, targetEntitySetName);

            //Create Entity of target
            res = createNavigationPropertyEntity(entity, targetEntitySetName);
            if (res == null || res.getEntity() == null) {
                return null;
            }
            EntitySetDocHandler retrievedEntity = this.retrieveWithKey(entity);
            if (retrievedEntity == null) {
                throw PersoniumCoreException.Server.UNKNOWN_ERROR;
            }

            //Register $ links
            entity = createNavigationPropertyLink(sourceOEntity, entity, srcNavProp, sourceDocHandler, retrievedEntity);
            res = Responses.entity(entity);

        } finally {
            log.debug("unlock");
            lock.release();
        }
        return res;
    }

    /**
     * It checks whether you are going to register beyond the limit that can be registered with $ links.
     * @param srcNavProp EdmNavigationProperty on the source side
     * @param sourceDocHandler Source side DocHandler
     * @param targetEntitySetName EntitySet name on the target side
     */
    private void checkUpperLimitRecord(
            EdmNavigationProperty srcNavProp,
            EntitySetDocHandler sourceDocHandler,
            String targetEntitySetName) {
        //When the relation is other than N: N, there is no upper limit value limit
        if (!isAssociationOfNToN(srcNavProp.getRelationship())) {
            return;
        }

        //In case of acquiring $ links of user data, acquire _id of EntityType of target
        String targetEntityTypeId = null;
        if (sourceDocHandler.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetEntitySetName);
        }

        //Registered $ links
        long count = LinkDocHandler.getNtoNCount(this.getAccessorForLink(), sourceDocHandler, targetEntitySetName,
                targetEntityTypeId);
        log.info("Registered links count: [" + count + "]");

        if (count >= (long) PersoniumUnitConfig.getLinksNtoNMaxSize()) {
            throw PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED;
        }
    }

    /**
     * It is judged whether the relation specified by the argument is N: N.
     * @param assoc EdmAssociation
     * @return true: N: N, false: N: other than N
     */
    private boolean isAssociationOfNToN(EdmAssociation assoc) {
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();
        return multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY;
    }

    private void checkExistsLink(OEntityId sourceOEntity,
            EdmNavigationProperty srcNavProp,
            EntitySetDocHandler sourceDocHandler,
            OEntity targetrEntity) {
        //Isolate n: 1 or n: n
        EdmAssociation assoc = srcNavProp.getRelationship();
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();

        if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            //In case of 1: 1 / 0. 1: 1/1: 0..1 / 0..1: 0..1
            checkExistsLinkForOnetoOne(sourceDocHandler, null, srcNavProp);

        } else if (!(multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY)) {
            String multiplicityOneEntitySetName = getOneAssociationEnd(assoc).getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            EntitySetDocHandler targetDocHandler = new OEntityDocHandler();
            //Add Type to the search condition in consideration of the case where there is an element with the same name in another Type
            targetDocHandler.setType(toEntitySetName);
            if (sourceOEntity.getEntitySetName().equals(multiplicityOneEntitySetName)) {
                //In case of 1: N

                //In the case of the user OData, since the uniqueness check as to whether the Name property does not exist and the same data exists after link registration is not performed, the Name property is not set
                if (!(this instanceof UserDataODataProducer)) {
                    Map<String, Object> staticFields = new HashMap<String, Object>();
                    if (ExtRole.EDM_TYPE_NAME.equals(toEntitySetName)) {
                        //Since ExtRole does not have a Name property, it is distinguished by "ExtRole"
                        staticFields.put(ExtRole.EDM_TYPE_NAME,
                                targetrEntity.getProperty(ExtRole.EDM_TYPE_NAME).getValue());
                    } else if (ReceivedMessage.EDM_TYPE_NAME.equals(toEntitySetName)
                            || SentMessage.EDM_TYPE_NAME.equals(toEntitySetName)
                            || Rule.EDM_TYPE_NAME.equals(toEntitySetName)) {
                        staticFields.put("__id", targetrEntity.getProperty("__id").getValue());
                    } else {
                        staticFields.put("Name", targetrEntity.getProperty("Name").getValue());
                    }
                    targetDocHandler.setStaticFields(staticFields);
                }
                checkExistsLinksForOneToN(sourceOEntity, sourceDocHandler, targetDocHandler,
                        multiplicityOneEntitySetName, toEntitySetName);
            } else {
                //When N: 1
                checkExistsLinksForNtoOne(sourceDocHandler, toEntitySetName);
            }
        }
    }

    private void checkAssociationEndMultiplicity(String targetNavProp,
            OEntity entity,
            String targetEntitySetName,
            EntitySetDocHandler sourceDocHandler) {
        if (AssociationEnd.EDM_TYPE_NAME.equals(targetEntitySetName)
                && AssociationEnd.EDM_TYPE_NAME.equals(targetNavProp.substring(1))) {
            String srcMulti = String.valueOf(sourceDocHandler.getStaticFields()
                    .get(AssociationEnd.P_MULTIPLICITY.getName()));
            String tgtMulti = entity.getProperty(AssociationEnd.P_MULTIPLICITY.getName()).getValue().toString();
            if (EdmMultiplicity.ONE.getSymbolString().equals(srcMulti)
                    && EdmMultiplicity.ONE.getSymbolString().equals(tgtMulti)) {
                throw PersoniumCoreException.OData.INVALID_MULTIPLICITY;
            }
        }
    }

    /**
     * Set the link type in the context for batch registration.
     * @param navigationPropertyContext Context of bulk registration
     */
    private void setNavigationPropertyLinkType(
            NavigationPropertyBulkContext navigationPropertyContext) {
        OEntityId sourceOEntity = navigationPropertyContext.getSrcEntityId();
        String srcSetName = sourceOEntity.getEntitySetName();
        String targetNavProp = navigationPropertyContext.getTgtNavProp();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);

        EdmAssociation assoc = srcNavProp.getRelationship();
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();

        if (multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY) {
            navigationPropertyContext.setLinkType(NavigationPropertyLinkType.manyToMany);
        } else if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            navigationPropertyContext.setLinkType(NavigationPropertyLinkType.oneToOne);
        } else {
            String multiplicityOneEntitySetName = getOneAssociationEnd(assoc).getType().getName();
            if (sourceOEntity.getEntitySetName().equals(multiplicityOneEntitySetName)) {
                navigationPropertyContext.setLinkType(NavigationPropertyLinkType.oneToMany);
            } else {
                navigationPropertyContext.setLinkType(NavigationPropertyLinkType.manyToOne);
            }
        }
    }

    private OEntity createNavigationPropertyLink(OEntityId sourceOEntity,
            OEntity entity,
            EdmNavigationProperty srcNavProp,
            EntitySetDocHandler sourceDocHandler,
            EntitySetDocHandler retrievedEntity) {
        EdmAssociation assoc = srcNavProp.getRelationship();
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();

        if (multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY) {
            //In the case of n: n
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceDocHandler, retrievedEntity);

            ODataLinkAccessor linkAccessor = this.getAccessorForLink();
            createLinkForNtoN(linkAccessor, docHandler);
        } else if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            //In case of 1: 1 / 0. 1: 1/1: 0..1 / 0..1: 0..1
            long version = createLinkForOnetoOne(sourceDocHandler, retrievedEntity, srcNavProp);
            setETagVersion((OEntityWrapper) entity, version);
        } else {

            String multiplicityOneEntitySetName = getOneAssociationEnd(assoc).getType().getName();
            String fromEntitySetName = srcNavProp.getFromRole().getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            if (sourceOEntity.getEntitySetName().equals(multiplicityOneEntitySetName)) {
                //1: n / 0. 1: n
                String fromLinksKey = getLinkskey(fromEntitySetName);
                long version = createLinkForNtoOne(sourceDocHandler, retrievedEntity, fromLinksKey,
                        toEntitySetName);
                if (ODataProducerUtils.isParentEntity(sourceOEntity, multiplicityOneEntitySetName)) {
                    setETagVersion((OEntityWrapper) entity, version);
                }

            } else {
                //In the case of n: 1 / n: 0..1
                String toLinksKey = getLinkskey(toEntitySetName);
                long version = createLinkForNtoOne(retrievedEntity, sourceDocHandler, toLinksKey,
                        fromEntitySetName);
                if (ODataProducerUtils.isParentEntity(sourceOEntity, multiplicityOneEntitySetName)) {
                    setETagVersion((OEntityWrapper) entity, version);
                }
            }
        }

        return entity;
    }

    private void setETagVersion(OEntityWrapper entity, long version) {
        EntitySetDocHandler oedh;
        EntitySetAccessor esType;
        String etag = null;
        //When version is updated, update etag information
        esType = this.getAccessorForEntitySet(entity.getEntitySetName());
        oedh = getDocHanlder(esType.getType(), entity);
        oedh.setVersion(version);
        etag = oedh.createEtag();
        entity.setEtag(etag);
    }

    private EntityResponse createNavigationPropertyEntity(OEntity entity, String entitySetName) {
        OEntityKey entityKey = entity.getEntityKey();
        //Convert from OEntity of request to JSONObject of the form registered in elasticsearch
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EntityResponse res = createEntity(entitySetName, entity, entityKey, esType, (OEntityWrapper) entity);
        return res;
    }

    private long createLinks(OEntityId sourceOEntity,
            EdmNavigationProperty srcNavProp,
            EdmAssociation assoc,
            EntitySetDocHandler sourceEntity,
            EntitySetDocHandler targetEntity) {
        long version = -1;
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();
        String srcMultiplicity = (String) sourceEntity.getStaticFields().get(AssociationEnd.P_MULTIPLICITY.getName());
        String tgtMultiplicity = (String) targetEntity.getStaticFields().get(AssociationEnd.P_MULTIPLICITY.getName());

        if (EdmMultiplicity.ONE.getSymbolString().equals(srcMultiplicity)
                && EdmMultiplicity.ONE.getSymbolString().equals(tgtMultiplicity)) {
            throw PersoniumCoreException.OData.INVALID_MULTIPLICITY;
        }

        checkInvalidLinks(sourceEntity, targetEntity);

        if (multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY) {
            //In the case of n: n
            //Take accessors to handle LINK
            ODataLinkAccessor esType = this.getAccessorForLink();
            //Creating a unique key when saving NNLink information ES
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceEntity, targetEntity);

            //Uniqueness check of $ links
            checkExistsLinkForNtoN(esType, docHandler);

            //$ links high limit check
            //Link source
            String targetEntitySetName = srcNavProp.getToRole().getType().getName();
            checkUpperLimitRecord(srcNavProp, sourceEntity, targetEntitySetName);
            //Link destination
            String sourceEntitySetName = srcNavProp.getFromRole().getType().getName();
            checkUpperLimitRecord(srcNavProp, targetEntity, sourceEntitySetName);

            createLinkForNtoN(esType, docHandler);
        } else if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            //In case of 1: 1
            checkExistsLinkForOnetoOne(sourceEntity, targetEntity, srcNavProp);
            version = createLinkForOnetoOne(sourceEntity, targetEntity, srcNavProp);
        } else {
            //Get an EdmAssociationEnd of 1: n: 1
            EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
            //In the case of n: 1, the ID of 1 is added as the link information to the data of n
            //Acquire Entity of each Assoc of 1: N from ES
            String entityTypeName = oneAssoc.getType().getName();
            boolean isParent = ODataProducerUtils.isParentEntity(sourceOEntity, entityTypeName);
            String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
            String targetEntitySetName = srcNavProp.getToRole().getType().getName();
            EntitySetDocHandler src = sourceEntity;
            EntitySetDocHandler tgt = targetEntity;

            if (!isParent) {
                targetEntitySetName = srcNavProp.getFromRole().getType().getName();
                fromLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
                src = targetEntity;
                tgt = sourceEntity;
            }
            checkExistsLinksForOneToN(sourceOEntity, src, tgt, entityTypeName, targetEntitySetName);
            version = createLinkForNtoOne(src, tgt, fromLinksKey, targetEntitySetName);
            if (!isParent) {
                version = -1;
            }
        }
        return version;
    }

    /**
     * Check unauthorized Link information.
     * @param sourceEntity source side Entity
     * @param targetEntity Target side Entity
     */
    protected abstract void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity);

    /**
     * Check unauthorized Link information.
     * @param sourceDocHandler Source side Entity
     * @param entity Target side Entity
     * @param targetEntitySetName EntitySet name of the target
     */
    protected abstract void checkInvalidLinks(EntitySetDocHandler sourceDocHandler,
            OEntity entity,
            String targetEntitySetName);

    private void checkExistsLinkForOnetoOne(final EntitySetDocHandler source,
            final EntitySetDocHandler target,
            EdmNavigationProperty srcNavProp) {
        //When links are already registered in the same NavigationPropeties, it is set to 409
        String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
        String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
        Map<String, Object> sourceLinks = source.getManyToOnelinkId();
        if (sourceLinks.get(toLinksKey) != null) {
            throw PersoniumCoreException.OData.CONFLICT_LINKS;
        } else if (target != null && target.getManyToOnelinkId().get(fromLinksKey) != null) {
            throw PersoniumCoreException.OData.CONFLICT_LINKS;
        }
    }

    /**
     * Generate 1: 1 links.
     * @param sourceEntity Entity specified in the request URL
     * @param targetEntity Entity specified by request BODY
     * @param srcNavProp navigation property
     * @return version information
     */
    private long createLinkForOnetoOne(final EntitySetDocHandler source,
            final EntitySetDocHandler target,
            EdmNavigationProperty srcNavProp) {
        String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
        String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
        Map<String, Object> sourceLinks = source.getManyToOnelinkId();
        Map<String, Object> targetLinks = target.getManyToOnelinkId();

        //Update link information
        sourceLinks.put(toLinksKey, target.getId());
        source.setManyToOnelinkId(sourceLinks);

        //Convert Alias ​​to property name
        source.convertAliasToName(getMetadata());
        updateLink(source, fromLinksKey);

        //When Propety is created via NP, the version is updated, so get the version
        targetLinks.put(fromLinksKey, source.getId());
        target.setManyToOnelinkId(targetLinks);

        //Convert Alias ​​to property name
        target.convertAliasToName(getMetadata());

        long version = updateLink(target, toLinksKey);
        return version;
    }

    /**
     * Update link information.
     * @param docHandler EntitySetDocHandler to be updated
     * @param entSetName EntitySet name to be updated
     * @return version
     */
    private long updateLink(EntitySetDocHandler docHandler, String entSetName) {
        //Take accessors
        EntitySetAccessor esType = this.getAccessorForEntitySet(entSetName);
        return esType.update(docHandler.getId(), docHandler).getVersion();
    }

    private void checkExistsLinkForNtoN(ODataLinkAccessor esType, LinkDocHandler docHandler) {
        String docid = docHandler.createLinkId();
        //Confirm existence of Link
        PersoniumGetResponse gRes = esType.get(docid);
        if (gRes != null && gRes.exists()) {
            //The corresponding LINK already exists
            throw PersoniumCoreException.OData.CONFLICT_LINKS;
        }
    }

    private void createLinkForNtoN(ODataLinkAccessor esType, LinkDocHandler docHandler) {
        String docid = docHandler.createLinkId();
        //JSON document creation when couch of NNLink information is saved
        esType.create(docid, docHandler);
    }

    private void checkExistsLinksForNtoOne(
            EntitySetDocHandler sourceEntity,
            String targetEntityTypeName) {
        String linksKey = getLinkskey(targetEntityTypeName);

        //If links are already registered in the target EntityTYpe (multiplicity 1) on the source side (multiplicity *), it is set to 409
        if (sourceEntity != null) {
            Map<String, Object> links = sourceEntity.getManyToOnelinkId();
            if (links != null && links.get(linksKey) != null) {
                throw PersoniumCoreException.OData.CONFLICT_LINKS;
            }
        }
    }

    private void checkExistsLinksForOneToN(OEntityId sourceOEntity,
            EntitySetDocHandler sourceEntity,
            EntitySetDocHandler targetEntity,
            String sourceEntityTypeName,
            String targetEntitySetName) {
        String linksKey = getLinkskey(sourceEntityTypeName);

        //If multiplicity of the target is * and links are already registered in the same NavigationPropeties, it is set to 409
        if (targetEntity != null) {
            Map<String, Object> links = targetEntity.getManyToOnelinkId();
            if (links != null && links.get(linksKey) != null) {
                throw PersoniumCoreException.OData.CONFLICT_LINKS;
            }
        }

        //When $ link is registered for a single key Entity, if there is a key of the same name 409
        EntitySetAccessor esType = this.getAccessorForEntitySet(targetEntitySetName);
        boolean uniqueness = checkUniquenessEntityKeyForAddLink(sourceOEntity.getEntitySetName(),
                sourceEntity, targetEntity, linksKey, esType);
        if (!uniqueness) {
            String param;
            if (targetEntity == null) {
                param = sourceOEntity.getEntitySetName() + "('No Target Entity', '"
                        + sourceEntity.getStaticFields().get("Name") + "')";
            } else {
                param = sourceOEntity.getEntitySetName() + "('" + targetEntity.getStaticFields().get("Name") + "', '"
                        + sourceEntity.getStaticFields().get("Name") + "')";
            }
            throw PersoniumCoreException.OData.CONFLICT_DUPLICATED_ENTITY.params(param);
        }
    }

    /**
     * Generate Links of N: 1.
     * @param targetEntity Entity specified by request BODY
     * @param oneAssoc N: 1's Association information
     * @param srcNavProp NavigationProperty
     * @return version
     */
    private long createLinkForNtoOne(EntitySetDocHandler sourceEntity,
            EntitySetDocHandler targetEntity,
            String fromLinksKey,
            String targetEntitySetName) {
        //Create a link object for registration and set it as an object to be registered
        Map<String, Object> links = targetEntity.getManyToOnelinkId();
        links.put(fromLinksKey, sourceEntity.getId());
        targetEntity.setManyToOnelinkId(links);

        //Convert Alias ​​to property name
        targetEntity.convertAliasToName(getMetadata());
        long version = updateLink(targetEntity, targetEntitySetName);
        return version;
    }

    /**
     * 1: Get 1 EdmAssociationEnd of N.
     * @param assoc Association information
     * @return 1: N's EdmAssociationEnd
     */
    private EdmAssociationEnd getOneAssociationEnd(EdmAssociation assoc) {
        EdmAssociationEnd[] assocs = {assoc.getEnd1(), assoc.getEnd2() };
        EdmAssociationEnd oneAssoc = null;

        for (EdmAssociationEnd assocEnd : assocs) {
            if (assocEnd.getMultiplicity() != EdmMultiplicity.MANY) {
                oneAssoc = assocEnd;
                break;
            }
        }
        return oneAssoc;
    }

    /**
     * Deletes an existing link between two entities.
     * @param sourceEntityId an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param targetEntityKey if the navigation property represents a set, the key identifying the target entity within
     *        the set, else n/a
     */
    @Override
    public final void deleteLink(final OEntityId sourceEntityId,
            final String targetNavProp,
            final OEntityKey targetEntityKey) {
        String srcSetName = sourceEntityId.getEntitySetName();

        //Schema check
        //From srcType to tgtType Check if N: N Assoc is defined
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(srcSetName);
        EdmEntityType srcType = srcSet.getType();

        EdmNavigationProperty navProp = srcType.findNavigationProperty(targetNavProp);
        if (navProp == null) {
            //TODO Originally, since the requested resource does not exist, 404 error should be returned
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        EdmEntitySet tgtSet = this.getMetadata().findEdmEntitySet(navProp.getToRole().getType().getName());

        //Isolate n: 1 or n: n
        EdmAssociation assoc = navProp.getRelationship();

        //Lock entire OData space
        Lock lock = this.lock();
        try {
            deleteLink(sourceEntityId, targetEntityKey, srcSet, tgtSet, assoc);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Delete an existing link between two entities without lock.
     * @param sourceEntityId an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param targetEntityKey if the navigation property represents a set, the key identifying the target entity within
     *        the set, else n/a
     */
    protected void deleteLinkWithoutLock(final OEntityId sourceEntityId,
            final String targetNavProp,
            final OEntityKey targetEntityKey) {
        String srcSetName = sourceEntityId.getEntitySetName();

        //Schema check
        //From srcType to tgtType Check if N: N Assoc is defined
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(srcSetName);
        EdmEntityType srcType = srcSet.getType();

        EdmNavigationProperty navProp = srcType.findNavigationProperty(targetNavProp);
        if (navProp == null) {
            //TODO Originally, since the requested resource does not exist, 404 error should be returned
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        EdmEntitySet tgtSet = this.getMetadata().findEdmEntitySet(navProp.getToRole().getType().getName());

        //Isolate n: 1 or n: n
        EdmAssociation assoc = navProp.getRelationship();

        deleteLink(sourceEntityId, targetEntityKey, srcSet, tgtSet, assoc);
    }

    private void deleteLink(final OEntityId sourceEntityId,
            final OEntityKey targetEntityKey, EdmEntitySet srcSet, EdmEntitySet tgtSet, EdmAssociation assoc) {

        if (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.MANY
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.MANY) {
            //In the case of n: n
            deleteLinks(sourceEntityId, targetEntityKey, tgtSet);
        } else if (assoc.getEnd1().getMultiplicity() != EdmMultiplicity.MANY
                && assoc.getEnd2().getMultiplicity() != EdmMultiplicity.MANY) {
            //In the case of [0..1: 0..1] or [0..1: 1] or [1: 0..1] or [1: 1], since it is a mutual link, link information is extracted from both data delete
            //Acquire the data of the link destination / link source
            EntitySetDocHandler source = this.retrieveWithKey(sourceEntityId);
            EntitySetDocHandler target = this.retrieveWithKey(tgtSet, targetEntityKey);
            //If the corresponding data does not exist, 404
            if (source == null || target == null) {
                throw PersoniumCoreException.OData.NOT_FOUND;
            }

            //If the linkage between the acquired data is checked and an attempt to delete unlinked links is made, 400 is returned
            isExistsLinks(source, target, tgtSet);
            isExistsLinks(target, source, srcSet);

            //Convert Alias ​​to property name
            source.convertAliasToName(getMetadata());
            target.convertAliasToName(getMetadata());

            //Delete both links
            String sourceEntitySetName = srcSet.getName();
            String targetEntitySetName = tgtSet.getName();
            linkUpdate(source, sourceEntitySetName, targetEntitySetName);
            linkUpdate(target, targetEntitySetName, sourceEntitySetName);
        } else {
            //Get an EdmAssociationEnd of 1: n: 1
            EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
            //In the case of n: 1, the relevant item is deleted from the link information of the data of 1
            deleteLinks(sourceEntityId, targetEntityKey, tgtSet, oneAssoc);
        }
    }

    /**
     * If the linkage between the acquired data is checked, and if it is attempted to delete the unlinked link, 400 is returned.
     * @param source Data of the EntitySet to be checked acquired from ES
     * @param source Data of the linked EntitySet obtained from ES
     * @param entitySet EntitySet
     */
    private void isExistsLinks(EntitySetDocHandler source, EntitySetDocHandler target, EdmEntitySet entitySet) {
        Map<String, Object> links = source.getManyToOnelinkId();
        String linksKey = getLinkskey(entitySet.getName());
        if (!links.containsKey(linksKey) || !target.getId().equals(links.get(linksKey))) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }
    }

    /**
     * Delete N:N links.
     * @param navigationProperty EdmNavigationProperty
     * @param fromDocHandler dochandler
     */
    private void deleteLinks(EdmNavigationProperty navigationProperty, EntitySetDocHandler fromDocHandler) {
        EdmAssociationEnd from = navigationProperty.getFromRole();
        EdmAssociationEnd to = navigationProperty.getToRole();
        if (EdmMultiplicity.MANY.equals(from.getMultiplicity())
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            String toTypeName = to.getType().getName();
            // In the case of links of user data, get _id of EntityType.
            String toEntityTypeId = null;
            if (UserDataODataProducer.USER_ODATA_NAMESPACE.equals(fromDocHandler.getType())) {
                toEntityTypeId = getEntityTypeId(toTypeName);
            }
            // Get links up to the registered number limit.
            EntitySetAccessor toEsType = getAccessorForEntitySet(toTypeName);
            QueryInfo queryInfo = QueryInfo.newBuilder().setTop(PersoniumUnitConfig.getLinksNtoNMaxSize())
                    .setInlineCount(InlineCount.NONE).build();
            List<String> idvals = LinkDocHandler.query(this.getAccessorForLink(),
                    fromDocHandler, toEsType.getType(), toEntityTypeId, queryInfo);

            PersoniumSearchHits searchHits = ODataProducerUtils.searchLinksNN(idvals, toEsType, null);
            if (searchHits == null || searchHits.getCount() == 0) {
                return;
            }
            // Delete links.
            for (PersoniumSearchHit hit : searchHits.getHits()) {
                EntitySetDocHandler toDocHandler = getDocHandler(hit, toTypeName);
                deleteLinkEntity(fromDocHandler, toDocHandler);
            }
        }
    }

    /**
     * Delete the link information of N: N.
     * @param sourceEntityId Entity specified in the request URL
     * @param targetEntityKey Entity specified by request BODY
     * @param tgtSet EntityKey specified in request BODY
     */
    private void deleteLinks(OEntityId sourceEntityId, OEntityKey targetEntityKey, EdmEntitySet tgtSet) {
        //Acquire the id of both
        EntitySetDocHandler src = this.retrieveWithKey(sourceEntityId);
        EntitySetDocHandler tgt = this.retrieveWithKey(tgtSet, targetEntityKey);

        //If the corresponding data does not exist, 404
        if (src == null || tgt == null) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }

        //Delete link entity
        if (!deleteLinkEntity(src, tgt)) {
            //If the applicable LINK does not exist, it is set as an error
            throw PersoniumCoreException.OData.NOT_FOUND;
        }
    }

    /**
     * Delete N: N link entity.
     * @param source source entity
     * @param target Linked Entity
     * @return Delete Returns false if true data does not exist
     */
    private boolean deleteLinkEntity(EntitySetDocHandler source, EntitySetDocHandler target) {
        //Identify the Es document to delete
        ODataLinkAccessor esType = this.getAccessorForLink();
        LinkDocHandler elh = this.getLinkDocHandler(source, target);
        String docid = elh.createLinkId();

        //Confirm existence of Link
        PersoniumGetResponse gRes = esType.get(docid);
        if (gRes != null && gRes.exists()) {
            esType.delete(elh);
            return true;
        }
        return false;
    }

    private void linkUpdate(EntitySetDocHandler tgt, String sourceEntitySetName, String unlinkEntitySetName) {
        Map<String, Object> links = tgt.getManyToOnelinkId();

        //Create a link object for registration and set it as an object to be registered
        links.remove(getLinkskey(unlinkEntitySetName));
        tgt.setManyToOnelinkId(links);

        //Take accessors
        EntitySetAccessor esType = this.getAccessorForEntitySet(sourceEntitySetName);
        esType.update(tgt.getId(), tgt);
    }

    /**
     * Delete the link information of N: 1.
     * @param sourceEntityId Entity specified in the request URL
     * @param targetEntityKey Entity specified by request BODY
     * @param tgtSet EntityKey specified in request BODY
     * @param oneAssoc 1: N 1's Association information
     */
    private void deleteLinks(OEntityId sourceEntityId,
            OEntityKey targetEntityKey,
            EdmEntitySet tgtSet,
            EdmAssociationEnd oneAssoc) {
        //Acquire Entity of each Assoc of 1: N from ES
        EntitySetDocHandler src;
        EntitySetDocHandler tgt;
        String linksKey = getLinkskey(oneAssoc.getType().getName());
        String sourceEntitySetName = null;
        //Acquire the id of both
        if (ODataProducerUtils.isParentEntity(sourceEntityId, oneAssoc.getType().getName())) {
            src = this.retrieveWithKey(sourceEntityId);
            tgt = this.retrieveWithKey(tgtSet, targetEntityKey);
            sourceEntitySetName = tgtSet.getName();
        } else {
            src = this.retrieveWithKey(tgtSet, targetEntityKey);
            tgt = this.retrieveWithKey(sourceEntityId);
            sourceEntitySetName = sourceEntityId.getEntitySetName();
        }

        //If the corresponding data does not exist, 404
        if (src == null || tgt == null) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }

        //When no link to be deleted exists, it is set to 404
        Map<String, Object> links = tgt.getManyToOnelinkId();
        if (!src.getId().equals(links.get(linksKey))) {
            throw PersoniumCoreException.OData.NOT_FOUND;
        }

        //Create a link object for registration and set it as an object to be registered
        links.remove(linksKey);
        tgt.setManyToOnelinkId(links);

        //Convert Alias ​​to property name
        src.convertAliasToName(getMetadata());
        tgt.convertAliasToName(getMetadata());

        //Take accessors
        EntitySetAccessor esType = this.getAccessorForEntitySet(sourceEntitySetName);

        //When deleting $ links for Entity of a composite key, if there is a single key of the same name 409
        boolean uniqueness = checkUniquenessEntityKey(sourceEntitySetName, tgt, linksKey, esType);
        if (!uniqueness) {
            String param = sourceEntitySetName + "('" + tgt.getStaticFields().get("Name") + "')";
            throw PersoniumCoreException.OData.CONFLICT_UNLINKED_ENTITY.params(param);
        }

        esType.update(tgt.getId(), tgt);
    }

    /**
     * Uniqueness check routine.
     * @param entitySetName Entity set name
     * @param tgt Entity to be updated
     * @param esType ESAccessor to search for
     * @param termQuery link search query
     * Search results for @return Es
     */
    protected long checkUniquenessEntityCount(final String entitySetName,
            final EntitySetDocHandler tgt,
            final EntitySetAccessor esType, Map<String, Object> termQuery) {
        //Check if there is an entity with the same name as a single key
        //Assemble search query of Static field
        List<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        if (null != tgt && null != tgt.getStaticFields() && !tgt.getStaticFields().isEmpty()) {
            if (tgt.getStaticFields().get("__id") != null) {
                terms.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id"
                        + ".untouched", (String) tgt.getStaticFields().get("__id"), false));
            } else if (tgt.getStaticFields().get("Name") != null) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + ".Name"
                        + ".untouched", (String) tgt.getStaticFields().get("Name")));
                //Add Type to the search condition in consideration of the case where there is an element with the same name in another Type
                terms.add(QueryMapFactory.termQuery("_type", (String) tgt.getType()));
            } else if (tgt.getStaticFields().get(ExtRole.EDM_TYPE_NAME) != null) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + "." + ExtRole.EDM_TYPE_NAME
                        + ".untouched", (String) tgt.getStaticFields().get(ExtRole.EDM_TYPE_NAME)));
                //Add Type to the search condition in consideration of the case where there is an element with the same name in another Type
                terms.add(QueryMapFactory.termQuery("_type", (String) tgt.getType()));
            }
        }

        //Assemble a null search query for links
        terms.add(termQuery);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null,
                QueryMapFactory.mustQuery(getImplicitFilters(entitySetName)));

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("version", true);
        filter.put("filter", QueryMapFactory.andFilter(terms));

        filter.put("query", query);

        //Perform search
        return esType.count(filter);
    }

    /**
     * Uniqueness check at link deletion.
     * @param entitySetName Entity set name
     * @param tgt Entity to be updated
     * @param linksKey EntittyType name of the link
     * @param esType ESAccessor to search for
     * @return true Uniqueness is preserved | false Uniqueness is not preserved
     */
    protected boolean checkUniquenessEntityKey(final String entitySetName, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {

        //Assemble the search query of Link field
        String linkKey = OEntityDocHandler.KEY_LINK + "." + linksKey;
        //Assemble a null search query for links
        long count = checkUniquenessEntityCount(entitySetName, tgt, esType, QueryMapFactory.missingFilter(linkKey));
        if (count != 0) {
            return false;
        }
        return true;
    }

    /**
     * Uniqueness check at link registration.
     * @param entitySetName Entity set name
     * @param src Entity of link source
     * @param tgt Entity to be updated
     * @param linksKey EntittyType name of the link
     * @param esType ESAccessor to search for
     * @return true Uniqueness is preserved | false Uniqueness is not preserved
     */
    protected boolean checkUniquenessEntityKeyForAddLink(final String entitySetName,
            final EntitySetDocHandler src, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {
        //It is checked whether or not there is an entity of the same name as a compound key

        //Assemble the search query of Link field
        String linkKey = OEntityDocHandler.KEY_LINK + "." + linksKey;
        long count = checkUniquenessEntityCount(entitySetName,
                tgt, esType, QueryMapFactory.termQuery(linkKey, src.getId()));
        if (count != 0) {
            return false;
        }
        return true;
    }

    /** N: Number of maximum link acquisition number of 1.*/
    private static final int DEFAULT_TOP_VALUE = PersoniumUnitConfig.getTopQueryDefaultSize();

    /**
     * Returns the value of an entity's navigation property as a collection of entity links (or a single link if the
     * association cardinality is 1).
     * @param sourceEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @return a collection of entity links (or a single link if the association cardinality is 1)
     */
    @Override
    public final EntityIdResponse getLinks(final OEntityId sourceEntity, final String targetNavProp) {
        return getLinks(sourceEntity, targetNavProp, null);
    }

    /**
     * Returns the value of an entity's navigation property as a collection of entity links (or a single link if the
     * association cardinality is 1).
     * @param sourceEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param queryInfo queryInfo
     * @return a collection of entity links (or a single link if the association cardinality is 1)
     */
    public final EntityIdResponse getLinks(OEntityId sourceEntity, String targetNavProp, QueryInfo queryInfo) {
        log.debug(sourceEntity.getEntityKey().toKeyStringWithoutParentheses());
        log.debug(targetNavProp);

        EntitySetDocHandler src = this.retrieveWithKey(sourceEntity);
        if (src == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
        }

        //From srcType to tgtType Check if N: N Assoc is defined
        String srcSetName = sourceEntity.getEntitySetName();
        EdmEntityType srcType = this.getMetadata().findEdmEntitySet(srcSetName).getType();

        EdmNavigationProperty navProp = srcType.findNavigationProperty(targetNavProp);
        if (navProp == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        //Isolate n: 1 or n: n
        EdmAssociation assoc = navProp.getRelationship();

        String targetSetName = navProp.getToRole().getType().getName();
        List<OEntityId> oeids = new ArrayList<OEntityId>();
        EdmEntitySet tgtSet = this.getMetadata().findEdmEntitySet(targetSetName);

        //In case of acquiring $ links of user data, acquire _id of EntityType of target
        String targetEntityTypeId = null;
        if (src.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetSetName);
        }

        if (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.MANY
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.MANY) {
            //Acquire up to 10,000 cases according to the upper limit value of registration number of N: N link registration number of 10,000
            EntitySetAccessor tgtEsType = this.getAccessorForEntitySet(targetSetName);
            QueryInfo qi = QueryInfo.newBuilder().setTop(PersoniumUnitConfig.getTopQueryMaxSize())
                    .setInlineCount(InlineCount.NONE).build();
            List<String> idvals = LinkDocHandler.query(this.getAccessorForLink(),
                    src, tgtEsType.getType(), targetEntityTypeId, qi);

            PersoniumSearchHits sHits = ODataProducerUtils.searchLinksNN(idvals, tgtEsType, queryInfo);
            oeids = getOEntityIds(sHits, targetSetName, tgtSet);

        } else if ((assoc.getEnd1().getMultiplicity() == EdmMultiplicity.ZERO_TO_ONE //NOPMD -To maintain readability
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.ZERO_TO_ONE)
                || (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.ONE //NOPMD -To maintain readability
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.ONE)) {
            //Acquire one EdmAssociationEnd
            oeids = getOEntityIds(src, targetSetName, tgtSet);

        } else {
            //Get an EdmAssociationEnd of 1: n: 1
            EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
            String linksKey = this.getLinkskey(oneAssoc.getType().getName());

            if (ODataProducerUtils.isParentEntity(sourceEntity, oneAssoc.getType().getName())) {
                //When SOURCE is 1, it searches for {"l": {"entitySet": UUID} of data of type specified by NavProp
                Integer size = DEFAULT_TOP_VALUE;
                Integer from = 0;
                if (queryInfo != null) {
                    if (queryInfo.top != null) {
                        size = queryInfo.top;
                    }
                    if (queryInfo.skip != null) {
                        from = queryInfo.skip;
                    }
                }
                //Specify an implicit filter in order of EntityType / Node / Box / Cell
                //Filter by specifying the link destination information at the head of the filter.
                List<Map<String, Object>> implicitFilters = getImplicitFilters(targetSetName);
                implicitFilters.add(0, getLinkFieldsQuery(linksKey, src.getId()));
                Map<String, Object> query = QueryMapFactory.mustQuery(implicitFilters);
                Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(null, query);
                //Specify sort condition (Cell control object: Name, UserOData: use __ id)
                //Some TODO cell control objects do not have a Name property, in which case the sort order is undefined.
                List<Map<String, Object>> sort = new ArrayList<Map<String, Object>>();
                sort.add(QueryMapFactory.sortQuery("s.Name.untouched", "asc"));
                sort.add(QueryMapFactory.sortQuery("s.__id.untouched", "asc"));
                //Aggregate query information
                Map<String, Object> filter = new HashMap<String, Object>();
                filter.put("size", size);
                filter.put("from", from);
                filter.put("query", filteredQuery);
                filter.put("sort", sort);

                //Perform search
                EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);
                PersoniumSearchHits sHits = esType.search(filter).hits();
                oeids = getOEntityIds(sHits, targetSetName, tgtSet);
            } else {
                //When SOURCE is N
                oeids = getOEntityIds(src, targetSetName, tgtSet);
            }
        }
        EntityIdResponse resp = Responses.multipleIds(oeids);
        return resp;
    }

    /**
     * Generate and obtain a List of OEntityId from the search result.
     * Search results for @param sHits Links
     * @param targetSetName EntitySet name of NavProp specified in the request URL
     * @param tgtSet NavProp's EdmEntitySet specified in the request URL
     * @return OEntityId list
     */
    private List<OEntityId> getOEntityIds(PersoniumSearchHits sHits, String targetSetName, EdmEntitySet tgtSet) {
        //Generate a List of OEntityId from the search result
        List<OEntityId> oeids = new ArrayList<OEntityId>();
        if (sHits == null) {
            return oeids;
        }
        for (PersoniumSearchHit hit : sHits.getHits()) {
            EntitySetDocHandler oedh = getDocHandler(hit, targetSetName);
            OEntityId id = getOEntityId(targetSetName, tgtSet, oedh);
            oeids.add(id);
        }
        return oeids;
    }

    /**
     * N: Generates and obtains OEntityId from N's data.
     * @param src Entity specified in the request URL
     * @param targetSetName EntitySet name of NavProp specified in the request URL
     * @param tgtSet targetEdmEntitySet
     * @return OEntityId list
     */
    private List<OEntityId> getOEntityIds(EntitySetDocHandler src, String targetSetName, EdmEntitySet tgtSet) {
        List<OEntityId> oeids = new ArrayList<OEntityId>();

        EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);
        String linksId = (String) src.getManyToOnelinkId().get(getLinkskey(targetSetName));

        if (linksId != null) {
            PersoniumGetResponse response = esType.get(linksId);
            EntitySetDocHandler docHandler = getDocHandler(response, targetSetName);
            OEntityId id = getOEntityId(targetSetName, tgtSet, docHandler);
            oeids.add(id);
        }
        return oeids;
    }

    private OEntityId getOEntityId(String targetSetName, EdmEntitySet tgtSet, EntitySetDocHandler oedh) {
        setNavigationTargetKeyProperty(tgtSet, oedh);
        OEntity oe = oedh.createOEntity(tgtSet, this.getMetadata(), null);
        OEntityId id = OEntityIds.create(targetSetName, oe.getEntityKey());
        return id;
    }

    @Override
    public MetadataProducer getMetadataProducer() {
        return null;
    }

    /**
     * Execute list acquisition via NavigationProperty.
     * If there is no specification in the query information, the default number of cases (return up to 25 items)
     * @param entitySetName Entity set name
     * @param entityKey entity key
     * @param navPropStr navigation property
     * @param queryInfo query information
     * @return Search results
     */
    @Override
    public BaseResponse getNavProperty(final String entitySetName,
            final OEntityKey entityKey,
            final String navPropStr,
            final QueryInfo queryInfo) {
        //Note) Since the existence guarantee of the origin EntitySet is done in advance on the caller side, it is not checked here.
        //Note) Premise that illegal NavigationProperty specification is confirmed / eliminated beforehand on caller side.

        //Acquisition of Src side Entity
        EdmEntitySet sourceSet = this.getMetadata().findEdmEntitySet(entitySetName);

        //Acquire Target's EdmEntitySet
        EdmNavigationProperty navProp = sourceSet.getType().findNavigationProperty(navPropStr);
        String targetSetName = navProp.getToRole().getType().getName();
        EdmEntitySet targetSet = this.getMetadata().findEdmEntitySet(targetSetName);

        //Get Target's accessor
        EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);

        //Get EntitySet
        EntitySetDocHandler source = this.retrieveWithKey(sourceSet, entityKey);
        if (source == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
        }

        //In case of acquiring $ links of user data, acquire _id of EntityType of target
        String targetEntityTypeId = null;
        if (source.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetSetName);
        }

        //Next, check the multiplicity.
        int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(navProp);
        Map<String, Object> linkQuery;

        //Depending on the Cardinality pattern, create an Es search query.
        if (ODataUtils.Cardinality.MANY_MANY == cardinality) {
            //In the case of N: N, Type [Link] is searched to acquire the ID list of NavigationProperty
            Map<String, Object> idsQuery = new HashMap<String, Object>();
            //Acquire up to 10,000 cases according to the upper limit value of registration number of N: N link registration number of 10,000
            QueryInfo qi = QueryInfo.newBuilder().setTop(PersoniumUnitConfig.getTopQueryMaxSize())
                    .setInlineCount(InlineCount.NONE).build();

            List<String> value = LinkDocHandler.query(this.getAccessorForLink(),
                    source, esType.getType(), targetEntityTypeId, qi);

            //If id is empty, return empty search result
            if (value.isEmpty()) {
                return emptyResult(queryInfo, targetSet);
            }

            //And sets the acquired ID list as a search condition
            linkQuery = new HashMap<String, Object>();

            idsQuery.put("values", value);
            linkQuery.put("ids", idsQuery);
        } else if (ODataUtils.Cardinality.ONE_MANY == cardinality) {
            //In the case of 1: N, the search condition of the link information is set
            linkQuery = getLinkFieldsQuery(getLinkskey(entitySetName), source.getId());

        } else {
            //If N: 1, 1: 1, the ID of NavigationProperty is set as the search condition
            //{"ids": {"values": ["Internal ID of linking source EntityType"]}}
            String linkId = (String) source.getManyToOnelinkId().get(getLinkskey(targetSetName));
            //If the link is not set, return empty search results
            if (linkId == null) {
                return emptyResult(queryInfo, targetSet);
            }
            Map<String, Object> idsQuery = new HashMap<String, Object>();
            List<String> value = new ArrayList<String>();
            value.add(linkId);
            linkQuery = new HashMap<String, Object>();

            idsQuery.put("values", value);
            linkQuery.put("ids", idsQuery);
        }

        //Create Implicit Filter based on Cell / Box / Node / EntityType
        List<Map<String, Object>> implicitFilters = getImplicitFilters(targetSetName);

        //Add search condition of Link information to implicit filter
        implicitFilters.add(linkQuery);

        //Pass implicitFIlters and perform a search
        return execEntitiesRequest(queryInfo, targetSet, esType, implicitFilters);
    }

    /**
     * Get search query of link field.
     * @param entitySet entity set name
     * @param id ID of the link entity
     * @return search query
     */
    public Map<String, Object> getLinkFieldsQuery(String entitySet, String id) {
        //{"term": {"l. linking source EntityType name. untouched": "internal ID of linking source EntityType"}}
        String linkKey = OEntityDocHandler.KEY_LINK + "." + entitySet;
        return QueryMapFactory.termQuery(linkKey, id);
    }

    /**
     * Return empty search results.
     * @param queryInfo search condition
     * @param targetSet target EntitySet
     * @return BaseResponse response
     */
    public BaseResponse emptyResult(final QueryInfo queryInfo, EdmEntitySet targetSet) {
        Integer count = null;
        if (queryInfo != null && queryInfo.inlineCount.equals(InlineCount.ALLPAGES)) {
            count = 0;
        }
        List<OEntity> entList = new ArrayList<OEntity>();
        return Responses.entities(entList, targetSet, count, null);
    }

    /**
     * Modifies an existing entity using merge semantics.
     * @param entitySetName the entity-set name
     * @param entity the entity modifications sent from the client
     * @see <a href="http://www.odata.org/developers/protocols/operations#UpdatingEntries">[odata.org] Updating
     *      Entries</a>
     */
    @Override
    public void mergeEntity(final String entitySetName, final OEntity entity) {
        //Since this method of ODataProducer of OData 4 j has a defect that it can not withstand primary key change structurally
        //It is not used in this application.
        throw new RuntimeException("Bug! Do not call this method. ");
    }

    @Override
    public void mergeEntity(final String entitySetName,
            final OEntityKey originalKey,
            final OEntityWrapper oEntityWrapper) {

        //Get lock
        Lock lock = this.lock();
        try {
            updateAndMergeEntity(entitySetName, originalKey, oEntityWrapper, true);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    @Override
    public void updateEntity(final String entitySetName,
            final OEntityKey originalKey,
            final OEntityWrapper oEntityWrapper) {
        //Note) Since the existence guarantee of EntitySet is done on the calling side beforehand, it is not checked here.

        //Get lock
        Lock lock = this.lock();
        try {
            hasRelatedEntities(entitySetName, originalKey);
            updateAndMergeEntity(entitySetName, originalKey, oEntityWrapper, false);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    @Override
    public void updateEntity(final String entitySetName, final OEntity entity) {
        //This method of OData 4j's ODataProducer is not used in this application because it has a defect that it can not withstand primary key change structurally.
        throw new RuntimeException("Bug! Do not call this method. ");
    }

    private void updateAndMergeEntity(final String entitySetName,
            final OEntityKey originalKey,
            final OEntityWrapper oEntityWrapper,
            boolean isMergeMode) {
        //First of all check the existence. If it does not exist, Null is returned.
        EntitySetDocHandler oedhExisting = this.retrieveWithKey(oEntityWrapper.getEntitySet(), originalKey);
        if (oedhExisting == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
        }

        //Check if the value of If-Match header and Etag are equal
        ODataUtils.checkEtag(oEntityWrapper.getEtag(), oedhExisting);

        //To destroy the UUID in the oEntityWrapper of the argument so that the caller can get the UUID.
        oEntityWrapper.setUuid(oedhExisting.getId());
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EntitySetDocHandler oedhNew = getUpdateDocHanlder(esType.getType(), oEntityWrapper);

        //Perform uniqueness check of changed data.
        ODataProducerUtils.checkUniqueness(this, oEntityWrapper,
                oedhExisting.createOEntity(oEntityWrapper.getEntitySet(), this.getMetadata(), null), originalKey);

        //Pegged with Cell, Box, Node, EntityType
        oedhNew.setCellId(this.getCellId());
        oedhNew.setBoxId(this.getBoxId());
        oedhNew.setNodeId(this.getNodeId());
        oedhNew.setEntityTypeId(this.getEntityTypeId(entitySetName));
        oedhNew.setManyToOnelinkId(oedhExisting.getManyToOnelinkId());

        //Save link information before updating
        //The Map object of link information of oedhExisting and the Map object of link information of oedhNew use the same one.
        //By doing this, if you update link information of oedhNew with setLinksFromOEntity (), the link information of oedhExisting will also be updated.
        //For this reason, it is necessary to hold link information before updating.
        Map<String, Object> originalManeToNoelinkId = new HashMap<String, Object>();
        originalManeToNoelinkId.putAll(oedhExisting.getManyToOnelinkId());

        //If there is an NTKP item (ex. _EntityType.Name) with a compound key, link information is set
        if (KeyType.COMPLEX.equals(oEntityWrapper.getEntityKey().getKeyType())) {
            try {
                setLinksFromOEntity(oEntityWrapper, oedhNew);
            } catch (NTKPNotFoundException e) {
                throw PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
            }
        }

        //Since __published is not updated, use the value obtained from ES
        oedhNew.setPublished(oedhExisting.getPublished());

        //Check if changes are supported
        checkAcceptableModification(entitySetName, oedhExisting, originalManeToNoelinkId, oedhNew);

        //Pre-update processing
        this.beforeUpdate(entitySetName, originalKey, oedhNew);

        if (isMergeMode) {
            //In the merge mode, merge the document of the request into the existing document
            oedhExisting.convertAliasToName(getMetadata());
            ODataProducerUtils.mergeFields(oedhExisting, oedhNew);

            //Check whether the number of properties in the schema and the number of dynamic properties in the data do not exceed the limit
            int propNum = ODataUtils.getStaticPropertyCount(this.getMetadata(), entitySetName);
            checkPropertySize(propNum + oedhNew.getDynamicFields().size());
        }

        //__Id is not updated for user data update
        Map<String, Object> staticFields = oedhNew.getStaticFields();
        if (staticFields.containsKey("__id") && KeyType.SINGLE.equals(originalKey.getKeyType())) {
            //Since user data is currently single key, it does not correspond to compound key
            //TODO In the future, when using user data as a compound key, it is necessary to handle compound key
            staticFields.put("__id", originalKey.asSingleValue());
            oedhNew.setStaticFields(staticFields);
        }

        //Update hidden fields information and UnitUser name.
        //However, in case of updating Account, it is necessary not to replace HashedCredential.
        String hashedCredentialValue = (String) oedhNew.getHiddenFields().get(Account.HASHED_CREDENTIAL);
        oedhNew.getHiddenFields().putAll(oedhExisting.getHiddenFields());
        if (hashedCredentialValue != null) {
            oedhNew.getHiddenFields().put(Account.HASHED_CREDENTIAL, hashedCredentialValue);
        }
        oedhNew.resolveUnitUserName(oedhExisting.getHiddenFields());

        //Copy contents of DynamicField
        if (oedhExisting.getDynamicFields() != null) {
            oedhNew.getDynamicFields().putAll(oedhExisting.getDynamicFields());
        }

        //Copy ACL information
        if (oedhExisting.getAclFields() != null) {
            oedhNew.getAclFields().putAll(oedhExisting.getAclFields());
        }

        //Save esJson in ES
        PersoniumIndexResponse idxRes = null;
        //Retrieve verification version from Etag specification of request (null if there is no Etag specification)
        Long version = oedhNew.getVersion();
        if (version == null || version < 0) {
            idxRes = esType.update(oedhNew.getId(), oedhNew);
        } else {
            idxRes = esType.update(oedhNew.getId(), oedhNew, version);
        }

        //Processing after updating
        this.afterUpdate();

        //Set Version information obtained from response destructively to argument OEntityWrapper so that ETag can be returned by Resource layer
        oedhNew.setVersion(idxRes.version());
        oEntityWrapper.setEtag(oedhNew.createEtag());
    }

    /**
     * Perform password change of Account.
     * @param entitySet entitySetName
     * @param originalKey Key to be updated
     * @param dcCredHeader dcCredHeader
     */
    public void updatePassword(final EdmEntitySet entitySet,
            final OEntityKey originalKey, final String dcCredHeader) {
        Lock lock = lock();
        try {
            //Acquire Account information to be changed from ES
            EntitySetDocHandler oedhNew = this.retrieveWithKey(entitySet, originalKey);
            if (oedhNew == null) {
                throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            }
            //Overwrite password and update date of acquired Account
            ODataProducerUtils.createRequestPassword(oedhNew, dcCredHeader);

            //If the status is passwordChangeRequired, update the status to Active.
            Map<String, Object> staticFields = oedhNew.getStaticFields();
            if (Account.STATUS_PASSWORD_CHANGE_REQUIRED.equals(
                    staticFields.get(Account.P_STATUS.getName()).toString())) {
                staticFields.put(Account.P_STATUS.getName(), Account.STATUS_ACTIVE);
                oedhNew.setStaticFields(staticFields);
            }

            //Save esJson in ES
            //Retrieve version information of Account
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySet.getName());
            Long version = oedhNew.getVersion();
            esType.update(oedhNew.getId(), oedhNew, version);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Replaces an existing link between two entities.
     * @param sourceEntity an entity with at least one navigation property
     * @param targetNavProp the navigation property
     * @param oldTargetEntityKey if the navigation property represents a set, the key identifying the old target entity
     *        within the set, else n/a
     * @param newTargetEntity the new link target entity
     * @see <a href="http://www.odata.org/developers/protocols/operations#ReplacingLinksbetweenEntries">[odata.org]
     *      Replacing Links between Entries</a>
     */
    @Override
    public void updateLink(final OEntityId sourceEntity,
            final String targetNavProp,
            final OEntityKey oldTargetEntityKey,
            final OEntityId newTargetEntity) {
        // TODO V1.1 Auto-generated method stub
    }

    @Override
    public CountResponse getEntitiesCount(final String entitySetName, final QueryInfo queryInfo) {
        Long tmpCount = 0L;

        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        if (eSet != null) {
            //Note) Since the existence guarantee of EntitySet is done on the calling side beforehand, it is not checked here.
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
            //Create Implicit Filter based on Cell / Box / Node / EntityType
            List<Map<String, Object>> implicitFilters = getImplicitFilters(entitySetName);
            ODataQueryHandler visitor = getODataQueryHandler(queryInfo, eSet.getType(), implicitFilters);
            Map<String, Object> source = visitor.getSource();
            try {
                tmpCount = esType.count(source);
            } catch (EsClientException ex) {
                if (ex.getCause() instanceof PersoniumSearchPhaseExecutionException) {
                    SearchPhaseExecutionException speex = (SearchPhaseExecutionException) ex.getCause().getCause();
                    if (speex.status().equals(RestStatus.BAD_REQUEST)) {
                        throw PersoniumCoreException.OData.SEARCH_QUERY_INVALID_ERROR.reason(ex);
                    } else {
                        throw PersoniumCoreException.Server.DATA_STORE_SEARCH_ERROR.reason(ex);
                    }
                }
            }
        }
        final Long count = tmpCount;
        return new CountResponse() {
            @Override
            public long getCount() {
                return count.longValue();
            }
        };
    }

    @Override
    public CountResponse getNavPropertyCount(final String toEntitySetName,
            final OEntityKey entityKey,
            final String fromEntitySetName,
            final QueryInfo query) {
        OEntityId oeId = OEntityIds.create(toEntitySetName, entityKey);
        long tmpCount;
        try {
            EntitySetDocHandler src = this.retrieveWithKey(oeId);
            //Create a query to retrieve data with one to N links
            Map<String, Object> key = getLinkFieldsQuery(getLinkskey(toEntitySetName), src.getId());
            List<Map<String, Object>> filters = getImplicitFilters(fromEntitySetName);
            filters.add(key);

            //Assemble condition search
            EdmEntityType type = getMetadata().findEdmEntitySet(fromEntitySetName).getType();
            ODataQueryHandler visitor = getODataQueryHandler(query, type, filters);
            Map<String, Object> source = visitor.getSource();

            EntitySetAccessor esType = this.getAccessorForEntitySet(fromEntitySetName);
            tmpCount = esType.count(source);
        } catch (PersoniumCoreException e) {
            tmpCount = 0;
        }
        final long count = tmpCount;
        return new CountResponse() {
            @Override
            public long getCount() {
                return count;
            }
        };
    }

    /**
     * Register entities collectively via NavigationProperty.
     * @param npBulkContexts Context of bulk registration
     * @param npBulkRequests Request information for entity batch registration (for bulkCreateEntity)
     */
    public void bulkCreateEntityViaNavigationProperty(
            List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {

        //Lock first for uniqueness check
        Lock lock = this.lock();
        log.debug("bulkCreateEntityViaNavigationProperty get lock");
        try {
            //Collectively search the link source data
            if (!setLinkSourcesToBulkContexts(npBulkContexts)) {
                //When data to be processed does not exist
                return;
            }

            //Check the prerequisites for data registration, such as checking the link source / destination data or already creating the link
            int contextIndex = 0;
            for (BulkRequest npBulkRequest : npBulkRequests.values()) {
                NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
                if (!npBulkContext.isError()) {
                    try {
                        //Set the link type
                        setNavigationPropertyLinkType(npBulkContext);

                        //Perform data check
                        validateLinkForNavigationPropertyContext(npBulkContext);
                        if (isConflictLinks(npBulkContexts, npBulkContext)) {
                            npBulkRequest.setError(PersoniumCoreException.OData.CONFLICT_LINKS);
                            npBulkContext.setException(PersoniumCoreException.OData.CONFLICT_LINKS);
                        }
                    } catch (Exception e) {
                        npBulkRequest.setError(e);
                        npBulkContext.setException(e);
                    }
                }
            }

            //Check upper limit of $ links
            checkLinksUpperLimitRecord(npBulkContexts, npBulkRequests);

            //Register an entity
            List<EntityResponse> resultList = bulkCreateEntityWithoutLock(getMetadata(), npBulkRequests, getCellId());

            //Based on the result of entity registration, update the context of collective registration
            int index = 0;
            contextIndex = 0;
            for (BulkRequest request : npBulkRequests.values()) {
                NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
                Exception exception = request.getError();
                if (exception != null) {
                    //Set Exception that occurred when registering an entity
                    npBulkContext.setException(exception);
                } else {
                    //Since information such as Etag is given, the context is updated at the registered entity
                    EntityResponse entityResponse = resultList.get(index);
                    OEntityWrapper entity = (OEntityWrapper) entityResponse.getEntity();
                    npBulkContext.setOEntityWrapper(entity);
                    npBulkContext.setEntityResponse(entityResponse);

                    //Set information of registered entity to context
                    EntitySetDocHandler targetDocHandler = request.getDocHandler();
                    targetDocHandler.setId(entity.getUuid());
                    npBulkContext.setTargetDocHandler(targetDocHandler);

                    //Set link information as context
                    setNavigationPropertyContext(npBulkContext);
                    index++;
                }
            }

            //Register link information
            bulkCreateLinks(npBulkContexts, getCellId());
        } finally {
            lock.release();
            log.debug("bulkCreateEntityViaNavigationProperty release lock");
        }
    }

    /**
     * Check the upper limit of the number of links when registering entities collectively via NavigationProperty.
     * @param npBulkContexts Context of bulk registration
     * @param npBulkRequests Request information for entity batch registration (for bulkCreateEntity)
     */
    public void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {
    }

    /**
     * Search entities on the source side in batch and set them as NavigationPropertyBulkContext.
     * @param npBulkContexts context
     * @return true: Processing is completed normally / false: Data to be processed does not exist
     */
    @SuppressWarnings("unchecked")
    private boolean setLinkSourcesToBulkContexts(List<NavigationPropertyBulkContext> npBulkContexts) {
        //Create key and context Map temporarily for bulk search
        Map<String, List<NavigationPropertyBulkContext>> npBulkContextMap = new HashMap<String,
                List<NavigationPropertyBulkContext>>();
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            if (!npBulkContext.isError()) {
                String id = npBulkContext.getSrcEntityId().getEntityKey().asSingleValue().toString();
                String key = getLinkskey(npBulkContext.getSrcEntityId().getEntitySetName()) + ":" + id;
                if (!npBulkContextMap.containsKey(key)) {
                    npBulkContextMap.put(key, new ArrayList<NavigationPropertyBulkContext>());
                }
                npBulkContextMap.get(key).add(npBulkContext);
            }
        }

        //Collectively search the link source data
        Map<String, Object> searchQuery = getBulkSearchQuery(npBulkContexts);
        if (searchQuery == null) {
            //When data to be processed does not exist
            return false;
        }
        DataSourceAccessor accessor = getAccessorForBatch();
        PersoniumSearchResponse searchResponse = accessor.searchForIndex(getCellId(), searchQuery);
        if (searchResponse.getHits().getCount() != 0) {
            for (PersoniumSearchHit hit : searchResponse.getHits().getHits()) {
                //Compatible with TODO complex primary key
                HashMap<String, Object> staticFields = (HashMap<String, Object>) hit.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) hit.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                String key = entityTypeId + ":" + (String) staticFields.get("__id");
                List<NavigationPropertyBulkContext> targetContexts = npBulkContextMap.get(key);
                for (NavigationPropertyBulkContext ctx : targetContexts) {
                    //Set the link source data to the context
                    Map<String, String> entityTypeIds = getEntityTypeIds();
                    for (Map.Entry<String, String> entry : entityTypeIds.entrySet()) {
                        String tmpEntityTypeName = entry.getKey();
                        String tmpEntityTypeId = entry.getValue();
                        if (tmpEntityTypeId != null && tmpEntityTypeId.equals(entityTypeId)) {
                            ctx.setSourceDocHandler(getDocHandler(hit, tmpEntityTypeName));
                            break;
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Perform bulk registration.
     * @param metadata schema information
     * @param bulkRequests List of BatchCreateRequests to register
     * @param cellId cellId
     * @return EntitiesResponse
     */
    public List<EntityResponse> bulkCreateEntity(
            EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            String cellId) {
        //Get lock
        Lock lock = this.lock();
        log.debug("lock");
        try {
            return bulkCreateEntityWithoutLock(metadata, bulkRequests, cellId);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Perform bulk registration.
     * Since this method does not acquire locks, be sure to acquire / release locks at the calling side.
     * @param metadata schema information
     * @param bulkRequests List of BatchCreateRequests to register
     * @param cellId cellId
     * @return EntitiesResponse
     */
    @SuppressWarnings("unchecked")
    private List<EntityResponse> bulkCreateEntityWithoutLock(EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            String cellId) {
        List<EntityResponse> response = new ArrayList<EntityResponse>();

        DataSourceAccessor accessor = getAccessorForBatch();

        //If there is data whose primary key conflicts with elasticsearch, error information is set
        Map<String, Object> searchQuery = getBulkConflictCheckQuery(bulkRequests);
        if (searchQuery == null) {
            return response;
        }
        PersoniumSearchResponse searchResponse = accessor.searchForIndex(cellId, searchQuery);
        if (searchResponse.getHits().getCount() != 0) {
            for (PersoniumSearchHit hit : searchResponse.getHits().getHits()) {
                //Compatible with TODO complex primary key
                HashMap<String, Object> staticFields = (HashMap<String, Object>) hit.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) hit.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                String key = entityTypeId + ":" + (String) staticFields.get("__id");
                bulkRequests.get(key).setError(PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS);
            }
        }

        beforeBulkCreate(bulkRequests);

        //Generate request data only for registration target
        Map<String, String> keyMap = new HashMap<String, String>();
        List<EsBulkRequest> esBulkRequest = new ArrayList<EsBulkRequest>();
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            if (request.getValue().getError() == null) {
                keyMap.put(request.getValue().getId(), request.getKey());

                esBulkRequest.add(request.getValue());
            }
        }
        if (esBulkRequest.size() == 0) {
            return response;
        }
        //Execute collective registration
        try {
            PersoniumBulkResponse bulkResponse = accessor.bulkCreate(esBulkRequest, cellId);
            //EntitiesResponse assembly
            for (PersoniumBulkItemResponse itemResponse : bulkResponse.items()) {
                String key = keyMap.get(itemResponse.getId());
                if (itemResponse.isFailed()) {
                    //If an error has occurred in the bulk, set an error
                    bulkRequests.get(key).setError(new ServerErrorException("failed to store to es"));
                } else {
                    bulkRequests.get(key).getDocHandler().setVersion(itemResponse.version());
                    String etag = bulkRequests.get(key).getDocHandler().createEtag();
                    OEntityWrapper oEntity = bulkRequests.get(key).getDocHandler().createOEntity(
                            metadata.getEdmEntitySet(bulkRequests.get(key).getEntitySetName()), metadata, null);
                    oEntity.setEtag(etag);
                    oEntity.setUuid(itemResponse.getId());
                    response.add(Responses.entity(oEntity));
                }
            }
        } catch (PersoniumCoreException e) {
            //If the bulk request fails, set an error in all data used for registration in bulk
            PersoniumCoreLog.OData.BULK_INSERT_FAIL.reason(e).writeLog();
            for (EsBulkRequest request : esBulkRequest) {
                HashMap<String, Object> staticFields = (HashMap<String, Object>) request.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) request.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                bulkRequests.get(entityTypeId + ":" + (String) staticFields.get("__id")).setError(e);
            }
        } catch (EsClientException e) {
            //If the bulk request fails, set an error in all data used for registration in bulk
            PersoniumCoreLog.OData.BULK_INSERT_FAIL.reason(e).writeLog();
            for (EsBulkRequest request : esBulkRequest) {
                HashMap<String, Object> staticFields = (HashMap<String, Object>) request.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) request.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                bulkRequests.get(entityTypeId + ":" + (String) staticFields.get("__id"))
                        .setError(new ServerErrorException("failed to store to es"));
            }
        }
        return response;
    }

    /**
     * Create a query for bulk search of entities contained in bulkRequests.
     * @param bulkRequests List of requests to bulk retrieve
     * @return search query
     */
    private Map<String, Object> getBulkConflictCheckQuery(LinkedHashMap<String, BulkRequest> bulkRequests) {
        //Hash initialization for search conditions
        List<Object> orList = new ArrayList<Object>();

        //Generate a query for data conflict check
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            //Ignore error data
            if (request.getValue().getError() != null) {
                continue;
            }
            //Generated from primary key of TODO schema information, check of unique key, NTKP compliant
            List<Object> andList = new ArrayList<Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            andList.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id.untouched",
                    (String) request.getValue().getDocHandler().getStaticFields().get("__id"), false));
            //Type specification
            andList.add(QueryMapFactory.termQuery("_type",
                    request.getValue().getDocHandler().getType()));
            andList.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID,
                    (String) request.getValue().getDocHandler().getEntityTypeId()));
            and.put("and", andList);
            orList.add(and);
        }
        //When target data does not exist
        if (orList.size() == 0) {
            return null;
        }

        return composeQueryWithOrFilter(orList);
    }

    /**
     * Create a query for collective search of source side entities included in the context.
     * @param bulkContexts context
     * @return search query
     */
    private Map<String, Object> getBulkSearchQuery(List<NavigationPropertyBulkContext> bulkContexts) {
        //Hash initialization for search conditions
        List<Object> orList = new ArrayList<Object>();

        //Eliminate duplicate queries
        Set<List<Object>> registeredQuery = new HashSet<List<Object>>();

        //Generate a query for data conflict check
        for (NavigationPropertyBulkContext bulkContext : bulkContexts) {
            if (bulkContext.isError()) {
                //Ignore error data
                continue;
            }

            //Generated from primary key of TODO schema information, check of unique key, NTKP compliant
            List<Object> andList = new ArrayList<Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            andList.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id.untouched",
                    bulkContext.getSrcEntityId().getEntityKey().asSingleValue().toString(), false));
            //Type specification
            andList.add(QueryMapFactory.termQuery("_type",
                    UserDataODataProducer.USER_ODATA_NAMESPACE));
            andList.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID,
                    getLinkskey(bulkContext.getSrcEntityId().getEntitySetName())));

            //Eliminate duplicate queries
            if (registeredQuery.contains(andList)) {
                continue;
            }
            registeredQuery.add(andList);
            and.put("and", andList);
            orList.add(and);
        }
        //When target data does not exist
        if (orList.size() == 0) {
            return null;
        }
        return composeQueryWithOrFilter(orList);
    }

    /**
     * Attach the search condition of Cell, Box, NodeID etc to the or filter query passed as argument.
     * @param orList or filter query
     * @return search query
     */
    private Map<String, Object> composeQueryWithOrFilter(List<Object> orList) {
        //Hash initialization for search conditions
        Map<String, Object> searchQuery = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();

        filter.put("or", orList);

        //Add search condition of Cell, Box, NodeID
        Map<String, Object> query = QueryMapFactory.filteredQuery(null,
                QueryMapFactory.mustQuery(getImplicitFilters(null)));

        searchQuery.put("query", query);
        searchQuery.put("filter", filter);
        searchQuery.put("size", orList.size());
        return searchQuery;
    }

    private boolean isConflictLinks(
            List<NavigationPropertyBulkContext> npBulkContexts,
            NavigationPropertyBulkContext nvProContext) {
        boolean isConflict = false;
        int matched = 0;
        for (NavigationPropertyBulkContext targetNvProContext : npBulkContexts) {
            NavigationPropertyLinkType type1 = nvProContext.getLinkType();
            NavigationPropertyLinkType type2 = targetNvProContext.getLinkType();

            if (type1 != NavigationPropertyLinkType.oneToOne && type1 != NavigationPropertyLinkType.manyToOne) {
                continue;
            }
            if (type2 == null) {
                continue;
            }
            if (type1 != type2) {
                continue;
            }

            StringBuffer target = new StringBuffer();
            target.append(targetNvProContext.getSrcEntityId().getEntitySetName());
            target.append(":");
            target.append(targetNvProContext.getSrcEntityId().getEntityKey());
            target.append(":");
            target.append(targetNvProContext.getTgtNavProp());

            StringBuffer source = new StringBuffer();
            source.append(nvProContext.getSrcEntityId().getEntitySetName());
            source.append(":");
            source.append(nvProContext.getSrcEntityId().getEntityKey());
            source.append(":");
            source.append(nvProContext.getTgtNavProp());

            if (target.toString().equals(source.toString())) {
                matched++;
            }
            if (matched > 1) {
                isConflict = true;
                break;
            }
        }
        return isConflict;
    }

    /**
     * Perform bulk registration.
     * @param bulkContexts List of NavigationPropertyBulkContext to register
     * @param cellId cellId
     */
    private void bulkCreateLinks(
            List<NavigationPropertyBulkContext> bulkContexts,
            String cellId) {
        //Generate request data only for registration target
        List<EsBulkRequest> esBulkRequest = new ArrayList<EsBulkRequest>();
        List<EntitySetDocHandler> adsBulkEntityRequest = new ArrayList<EntitySetDocHandler>();
        List<LinkDocHandler> adsBulkLinkRequest = new ArrayList<LinkDocHandler>();
        for (NavigationPropertyBulkContext context : bulkContexts) {
            if (context.isError()) {
                continue;
            }

            NavigationPropertyLinkType linkType = context.getLinkType();
            switch (linkType) {
            case oneToOne:
                BulkRequest requestLinkFrom = new BulkRequest();
                requestLinkFrom.setDocHandler(context.getSourceDocHandler());
                esBulkRequest.add(requestLinkFrom);
                adsBulkEntityRequest.add(context.getSourceDocHandler());

                BulkRequest requestLinkTo = new BulkRequest();
                requestLinkTo.setDocHandler(context.getTargetDocHandler());
                esBulkRequest.add(requestLinkTo);
                adsBulkEntityRequest.add(context.getTargetDocHandler());
                break;
            case oneToMany:
                BulkRequest requestLinkOneToMany = new BulkRequest();
                requestLinkOneToMany.setDocHandler(context.getTargetDocHandler());
                esBulkRequest.add(requestLinkOneToMany);
                adsBulkEntityRequest.add(context.getTargetDocHandler());
                break;
            case manyToOne:
                BulkRequest requestLinkManyToOne = new BulkRequest();
                requestLinkManyToOne.setDocHandler(context.getSourceDocHandler());
                esBulkRequest.add(requestLinkManyToOne);
                adsBulkEntityRequest.add(context.getSourceDocHandler());
                break;
            case manyToMany:
                BulkRequest requestLinkMany = new BulkRequest();
                requestLinkMany.setDocHandler(new LinkDocHandlerForBulkRequest(context.getLinkDocHandler()));
                esBulkRequest.add(requestLinkMany);
                adsBulkLinkRequest.add(context.getLinkDocHandler());
                break;
            default:
                break;
            }
        }

        if (esBulkRequest.size() == 0) {
            return;
        }

        //Execute collective registration
        DataSourceAccessor accessor = getAccessorForBatch();
        try {
            int responseIndex = 0;
            PersoniumBulkResponse bulkResponse = accessor.bulkUpdate(esBulkRequest, cellId);
            PersoniumBulkItemResponse[] responseItems = bulkResponse.items();
            for (NavigationPropertyBulkContext context : bulkContexts) {
                if (context.isError()) {
                    continue;
                }

                NavigationPropertyLinkType linkType = context.getLinkType();
                switch (linkType) {
                case oneToOne:
                    PersoniumBulkItemResponse itemResponseFrom = responseItems[responseIndex++];
                    PersoniumBulkItemResponse itemResponseTo = responseItems[responseIndex++];
                    if (itemResponseFrom.isFailed() || itemResponseTo.isFailed()) {
                        context.setException(new ServerErrorException("failed to store to es"));
                    } else {
                        setETagVersion((OEntityWrapper) context.getOEntityWrapper(), itemResponseTo.version());
                    }
                    break;
                case manyToOne:
                    PersoniumBulkItemResponse manyToOneResponse = responseItems[responseIndex++];
                    if (manyToOneResponse.isFailed()) {
                        context.setException(new ServerErrorException("failed to store to es"));
                    }
                    break;
                case oneToMany:
                case manyToMany:
                    PersoniumBulkItemResponse itemResponse = responseItems[responseIndex++];
                    if (itemResponse.isFailed()) {
                        context.setException(new ServerErrorException("failed to store to es"));
                    } else {
                        setETagVersion((OEntityWrapper) context.getOEntityWrapper(), itemResponse.version());
                    }
                    break;
                default:
                    break;
                }
            }
        } catch (EsClientException e) {
            //If the bulk request fails, set an error in all data used for registration in bulk
            for (NavigationPropertyBulkContext context : bulkContexts) {
                if (context.isError()) {
                    continue;
                }
                context.setException(new ServerErrorException("failed to store to es"));
            }
        }
    }

    /**
     * Check the number of elements of the request body.
     * @param propNum property number
     */
    public void checkPropertySize(int propNum) {
    }

    /**
     * Check if it supports change.
     * @param entitySetName Entity set name
     * @param oedhExisting data existing in the data store
     * @param originalManeToNoelinkId Link information existing in the data store
     * @param oedhNew request data
     */
    protected void checkAcceptableModification(String entitySetName,
            EntitySetDocHandler oedhExisting,
            Map<String, Object> originalManeToNoelinkId,
            EntitySetDocHandler oedhNew) {
    }


    /**
     * It checks whether there is a document that refers to the key name of the EntitySet passed as an argument.
     * <p>
     * It is conceivable that the document to be updated is referred to by name (key name of EntitySet). In such a case, it is necessary to confirm whether or not the reference source document exists before updating the document.
     * </p>
     * @param entitySetName EntitySet name to be processed specified in the request URL
     * @param entityKey The key name of the processing target EntitySet specified in the request URL
     */
    protected void hasRelatedEntities(String entitySetName, OEntityKey entityKey) {
    }

}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.producer.QueryInfo;

import io.personium.common.es.response.PersoniumItem;
import io.personium.common.es.response.PersoniumMultiSearchResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler.NtoNQueryParameter;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.utils.ODataUtils;

/**
 * ODataProducerUtils.
 */
public final class ExpandEntitiesMapCreator {

    //List of navigation property names specified by Expand
    private List<String> navigationPropertyList = new ArrayList<String>();

    //Expand The original entity type type
    private EdmEntityType edmBaseEntityType;

    //Expand Maximum number of acquisition entities
    private int expandMaxNum;

    //Caching for each data ID
    private Map<String, Map<String, List<OEntity>>> relatedEntitiesListCache;

    //Map for acquiring link source ID from link destination data ID
    private Map<String, List<String>> baseDataIdMap;

    /**
     * constructor.
     * @param queryInfo query information
     * @param baseEntityType Expand The original entity type type
     * @param expandMaxNum Expand Maximum number of acquired entities
     */
    public ExpandEntitiesMapCreator(final QueryInfo queryInfo,
            final EdmEntityType baseEntityType,
            final int expandMaxNum) {
        if (isExpandQueryExists(queryInfo)) {
            setNavigationPropertyList(queryInfo, baseEntityType);
        }
        this.edmBaseEntityType = baseEntityType;
        this.expandMaxNum = expandMaxNum;
    }

    private boolean isExpandQueryExists(final QueryInfo queryInfo) {
        return queryInfo != null && queryInfo.expand != null && !queryInfo.expand.isEmpty();
    }

    private void setNavigationPropertyList(final QueryInfo queryInfo, EdmEntityType edmType) {
        for (EntitySimpleProperty qinfo : queryInfo.expand) {
            String navigationPropertyName = qinfo.getPropertyName();
            EdmNavigationProperty edmNavProp = edmType.findNavigationProperty(navigationPropertyName);
            //If NavigationProperty that does not exist in the schema is specified, it is an error
            if (edmNavProp == null) {
                throw PersoniumCoreException.OData.EXPAND_NTKP_NOT_FOUND_ERROR.params(navigationPropertyName);
            }
            this.navigationPropertyList.add(navigationPropertyName);
        }
    }

    /**
     * Create a cache of the specified entity.
     * @param baseEntity Expand The original entity
     * @param producer producer
     */
    public void setCache(EntitySetDocHandler baseEntity, EsODataProducer producer) {
        List<EntitySetDocHandler> baseEntityList = new ArrayList<EntitySetDocHandler>();
        baseEntityList.add(baseEntity);
        setCache(baseEntityList, producer);
    }

    /**
     * Create a cache of the specified entity.
     * @param baseEntityList Expand The original entity list
     * @param producer producer
     */
    public void setCache(List<EntitySetDocHandler> baseEntityList, EsODataProducer producer) {
        //Clear cache
        this.relatedEntitiesListCache = new HashMap<String, Map<String, List<OEntity>>>();
        this.baseDataIdMap = new HashMap<String, List<String>>();

        //When there is no cache target, it is completed in an empty state
        if (baseEntityList.isEmpty()) {
            return;
        }

        //If Expand is not specified, empty cache is set for all data
        if (this.navigationPropertyList.isEmpty()) {
            for (EntitySetDocHandler baseEntity : baseEntityList) {
                String id = baseEntity.getId();
                Map<String, List<OEntity>> relatedEntitiesList = new HashMap<String, List<OEntity>>();
                this.relatedEntitiesListCache.put(id, relatedEntitiesList);
            }
            return;
        }

        //In the case of N: N relationship, the link destination entity ID is searched from the Link table and added to the acquisition target entity ID list
        List<String> relatedEntityIdList = new ArrayList<String>();
        List<String> entityTypeListForLinkTable = getEntityTypeListForLinkTable();
        if (!entityTypeListForLinkTable.isEmpty()) {
            List<String> idList = getRelatedEntityIdListFromLinkTable(baseEntityList, producer,
                    entityTypeListForLinkTable);
            relatedEntityIdList.addAll(idList);
        }

        //In the case of N: 1/1: 1 relationship, since the link destination entity ID information exists in the own data, it is added to the acquisition target entity ID list
        //In the case of 1: N, since the ID of its own data is specified as the search condition at the time of actual data acquisition, it is not added to the acquisition target entity ID list
        List<String> idList = getEntityIdListFromBaseData(producer, baseEntityList);
        relatedEntityIdList.addAll(idList);

        //Retrieve expanded data using ID list
        String baseEntityTypeName = this.edmBaseEntityType.getName();
        String baseEntityLinksKey = producer.getLinkskey(baseEntityTypeName);
        PersoniumMultiSearchResponse multiSearchResponse = getRelatedEntities(producer,
                relatedEntityIdList,
                baseEntityLinksKey,
                baseEntityList);

        //Add an empty list to a valid expanded query
        for (EntitySetDocHandler baseEntity : baseEntityList) {
            String id = baseEntity.getId();
            Map<String, List<OEntity>> relatedEntitiesList = new HashMap<String, List<OEntity>>();
            for (String navPropName : this.navigationPropertyList) {
                List<OEntity> expandOEntityList = new ArrayList<OEntity>();
                relatedEntitiesList.put(navPropName, expandOEntityList);
            }
            this.relatedEntitiesListCache.put(id, relatedEntitiesList);
        }
        //If the expanded data does not exist, only the empty list is set in the cache and the process is terminated
        if (multiSearchResponse == null) {
            return;
        }

        //Add search results to relatedEntitieslist
        for (PersoniumItem item : multiSearchResponse) {
            PersoniumSearchHit[] searchHits = item.getSearchHits();
            for (PersoniumSearchHit hit : searchHits) {
                //Generate DocHandler of acquired data
                Map<String, String> entityTypeNameMap = producer.getEntityTypeMap();
                Map<String, Object> source = hit.getSource();
                String entityTypeName;
                String entityTypeId = (String) source.get(OEntityDocHandler.KEY_ENTITY_ID);
                if (entityTypeId != null) {
                    entityTypeId = source.get(OEntityDocHandler.KEY_ENTITY_ID).toString();
                    entityTypeName = entityTypeNameMap.get(Property.P_ENTITYTYPE_NAME.getName() + entityTypeId);
                } else {
                    entityTypeName = hit.getType();
                }
                EntitySetDocHandler docHandler = producer.getDocHandler(hit, entityTypeName);

                //Only data associated with the base data is added to the returned data
                List<String> baseEntityIds;
                if (docHandler.getManyToOnelinkId().containsKey(baseEntityLinksKey)) {
                    baseEntityIds = new ArrayList<String>();
                    baseEntityIds.add(docHandler.getManyToOnelinkId().get(baseEntityLinksKey).toString());
                } else {
                    baseEntityIds = this.baseDataIdMap.get(docHandler.getId());
                    if (baseEntityIds == null) {
                        baseEntityIds = new ArrayList<String>();
                    }
                }

                //Generate DocHander
                EdmEntitySet edmEntitySet = producer.getMetadata().getEdmEntitySet(entityTypeName);
                producer.setNavigationTargetKeyProperty(edmEntitySet, docHandler);
                OEntity relatedEntity = docHandler.createOEntity(edmEntitySet);

                //Get NavigationProperty name from entity type name
                Map<String, String> entityTypeNameNavPropNameMap = getEntityTypeNameNavPropNameMap();
                String navPropName = entityTypeNameNavPropNameMap.get(entityTypeName);

                //Entities not included in the link source list are excluded from the cache
                for (String baseEntityId : baseEntityIds) {
                    if (this.relatedEntitiesListCache.containsKey(baseEntityId)) {
                        Map<String, List<OEntity>> relatedEntitiesList = this.relatedEntitiesListCache
                                .get(baseEntityId);
                        List<OEntity> expandOEntityList = relatedEntitiesList.get(navPropName);

                        //Add entity list for Expand to cache
                        if (expandOEntityList.size() <= this.expandMaxNum) {
                            expandOEntityList.add(relatedEntity);
                            relatedEntitiesList.put(navPropName, expandOEntityList);
                        }
                    }
                }
            }
        }
    }

    /**
     * $ expand Gets the specified Entity.
     * @param baseEntity Expand The original entity
     * @param producer producer
     * @return OEntity list
     */
    public Map<String, List<OEntity>> create(EntitySetDocHandler baseEntity,
            EsODataProducer producer) {
        String baseEntityId = baseEntity.getId();
        if (this.relatedEntitiesListCache != null && this.relatedEntitiesListCache.containsKey(baseEntityId)) {
            return this.relatedEntitiesListCache.get(baseEntityId);
        }
        setCache(baseEntity, producer);
        return this.relatedEntitiesListCache.get(baseEntityId);
    }

    /**
     * Link table Return the list of entity types to be searched.
     * @return Link table List of entity types to search for
     */
    private List<String> getEntityTypeListForLinkTable() {
        List<String> entityTypeListForLinkTable = new ArrayList<String>();
        for (String expandEntity : this.navigationPropertyList) {
            //Get Multiplicity
            EdmNavigationProperty edmNavProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNavProp);

            //In the case of N: N, since the Link table is to be searched, the entity type name is added to the list
            if (ODataUtils.Cardinality.MANY_MANY == cardinality) {
                String entityTypeName = edmNavProp.getToRole().getType().getName();
                entityTypeListForLinkTable.add(entityTypeName);
            }
        }
        return entityTypeListForLinkTable;
    }

    /**
     * Search the link destination entity ID from the Link table and return the ID list of the link destination data.
     * @param baseEntity source entity
     * @param producer EsODataProducer
     * @param entityTypeListForLinkTable Entity type list to be searched
     * @return ID list of linked data
     */
    private List<String> getRelatedEntityIdListFromLinkTable(List<EntitySetDocHandler> baseEntityList,
            EsODataProducer producer,
            List<String> entityTypeListForLinkTable) {
        List<String> relatedEntityIdList = new ArrayList<String>();

        //In the case of user data, the entity type ID is used, but in the case of the managed resource, the entity type name is used
        EntitySetDocHandler baseEntity = baseEntityList.get(0);
        String linksKey = baseEntity.getEntityTypeId();
        if (linksKey == null) {
            linksKey = baseEntity.getType();
        }

        //Create a query list for Link table search
        List<Map<String, Object>> queries =
                createLinkTableSearchQueries(producer, entityTypeListForLinkTable, baseEntityList);
        if (queries.isEmpty()) {
            return relatedEntityIdList;
        }

        //Link table search (multisearch)
        ODataLinkAccessor accessor = producer.getAccessorForLink();
        PersoniumMultiSearchResponse multiSearchResponse = accessor.multiSearch(queries);

        //Add to ID list of linked data
        for (PersoniumItem item : multiSearchResponse.getResponses()) {
            PersoniumSearchHit[] searchHits = item.getSearchHits();
            for (PersoniumSearchHit hit : searchHits) {
                LinkDocHandler linkDoc = producer.getLinkDocHandler(hit);
                String relatedEntityId = linkDoc.getLinkedEntitytIdFromBaseEntityType(linksKey);
                relatedEntityIdList.add(relatedEntityId);

                if (!this.baseDataIdMap.containsKey(relatedEntityId)) {
                    this.baseDataIdMap.put(relatedEntityId, new ArrayList<String>());
                }
                String baseEntityId = linkDoc.getEntitytIdFromEntityType(linksKey);
                this.baseDataIdMap.get(relatedEntityId).add(baseEntityId);
            }
        }
        return relatedEntityIdList;
    }

    /**
     * Create a query list for N: N Link table search.
     * @param producer EsODataProducer
     * @param entityTypeListForLinkTable Entity type list to be searched
     * @param baseEntityList Source entity list
     * @return Link Query list for table search
     */
    private List<Map<String, Object>> createLinkTableSearchQueries(EsODataProducer producer,
            List<String> entityTypeListForLinkTable,
            List<EntitySetDocHandler> baseEntityList) {
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();

        //cell / box / node designation and the above query and condition
        Map<String, Object> implicitFilters = getImplicitFilters(producer);

        for (EntitySetDocHandler baseDocHandler : baseEntityList) {
            for (String expandType : entityTypeListForLinkTable) {
                String targetEntityTypeId = producer.getLinkskey(expandType);
                NtoNQueryParameter queryParameter = new NtoNQueryParameter(baseDocHandler, expandType,
                        targetEntityTypeId);
                Map<String, Object> source = queryParameter.getSourceForExpand(expandMaxNum);
                source.put("filter", implicitFilters);
                queries.add(source);
            }
        }
        return queries;
    }

    private Map<String, Object> getImplicitFilters(EsODataProducer producer) {
        List<Map<String, Object>> andQueryList = producer.getImplicitFilters(null);
        Map<String, Object> filtersQuery = new HashMap<String, Object>();
        filtersQuery.put("filters", andQueryList);
        Map<String, Object> implicitFilters = new HashMap<String, Object>();
        implicitFilters.put("and", filtersQuery);
        return implicitFilters;
    }

    /**
     * N: Obtain ID list of link destination data from link source data of 1/1: 1.
     * @param producer EsODataProducer
     * @return Link table List of entity types to search for
     */
    private List<String> getEntityIdListFromBaseData(EsODataProducer producer,
            List<EntitySetDocHandler> baseEntityList) {
        List<String> idList = new ArrayList<String>();
        for (String expandEntity : this.navigationPropertyList) {
            //Get Multiplicity
            EdmNavigationProperty edmNaviProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNaviProp);

            //In the case of N: 1/1: 1, since the ID information of the link destination data exists in the own data, it is added to the entity ID list
            if (ODataUtils.Cardinality.MANY_ONE == cardinality
                    || ODataUtils.Cardinality.ONE_ONE == cardinality) {
                String expandEntityTypeName = edmNaviProp.getToRole().getType().getName();
                String linksKey = producer.getLinkskey(expandEntityTypeName);

                for (EntitySetDocHandler baseEntity : baseEntityList) {
                    String baseEntityId = baseEntity.getId();
                    Map<String, Object> links = baseEntity.getManyToOnelinkId();

                    if (links.containsKey(linksKey)) {
                        String relatedEntityId = links.get(linksKey).toString();
                        idList.add(relatedEntityId);

                        if (!this.baseDataIdMap.containsKey(relatedEntityId)) {
                            this.baseDataIdMap.put(relatedEntityId, new ArrayList<String>());
                        }
                        this.baseDataIdMap.get(relatedEntityId).add(baseEntityId);
                    }
                }
            }
        }
        return idList;
    }

    private PersoniumMultiSearchResponse getRelatedEntities(EsODataProducer producer,
            List<String> expandEntityIdList,
            String baseEntityLinksKey,
            List<EntitySetDocHandler> baseEntityList) {
        List<Map<String, Object>> queries = createRelatedEntitiesSearchQuery(producer,
                expandEntityIdList, baseEntityLinksKey, baseEntityList);
        if (queries.isEmpty()) {
            return null;
        }

        DataSourceAccessor accessor = producer.getAccessorForBatch();
        PersoniumMultiSearchResponse searchResponse = accessor.multiSearchForIndex(producer.getCellId(), queries);
        return searchResponse;
    }

    private List<Map<String, Object>> createRelatedEntitiesSearchQuery(EsODataProducer producer,
            List<String> expandEntityIdList,
            String baseEntityLinksKey,
            List<EntitySetDocHandler> baseEntityList) {

        List<String> oneManyEntityTypes = new ArrayList<String>();
        List<String> idsEntityTypes = new ArrayList<String>();
        for (String expandEntity : this.navigationPropertyList) {
            //Get Multiplicity
            EdmNavigationProperty edmNaviProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNaviProp);

            //In the case of N: 1/1: 1, since the ID information of the link destination data exists in the own data, it is added to the entity ID list
            if (ODataUtils.Cardinality.MANY_ONE == cardinality
                    || ODataUtils.Cardinality.ONE_ONE == cardinality
                    || ODataUtils.Cardinality.MANY_MANY == cardinality) {
                String expandEntityTypeName = edmNaviProp.getToRole().getType().getName();
                String linksKey = producer.getLinkskey(expandEntityTypeName);
                idsEntityTypes.add(linksKey);
            } else if (ODataUtils.Cardinality.ONE_MANY == cardinality) {
                String expandEntityTypeName = edmNaviProp.getToRole().getType().getName();
                String linksKey = producer.getLinkskey(expandEntityTypeName);
                oneManyEntityTypes.add(linksKey);
            }
        }
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        //cell / box / node designation and the above query and condition
        Map<String, Object> implicitFilters = getImplicitFilters(producer);

        //1: N query added for search
        for (EntitySetDocHandler baseDataDocHandler : baseEntityList) {
            for (String entityType : oneManyEntityTypes) {
                String baseDataId = baseDataDocHandler.getId();
                Map<String, Object> source = new HashMap<String, Object>();

                List<Map<String, Object>> mustQueries = new ArrayList<Map<String, Object>>();
                if (producer instanceof UserDataODataProducer) {
                    mustQueries.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID, entityType));
                    mustQueries.add(QueryMapFactory.termQuery("ll",
                            baseEntityLinksKey + ":" + baseDataId));
                    source.put("query", QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(mustQueries)));
                } else {
                    mustQueries.add(QueryMapFactory.termQuery("_type", entityType));
                    mustQueries.add(QueryMapFactory.termQuery(
                            OEntityDocHandler.KEY_LINK + "." + baseEntityLinksKey, baseDataId));
                    source.put("query", QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(mustQueries)));
                }

                source.put("filter", implicitFilters);
                source.put("sort", QueryMapFactory.sortQuery(
                        OEntityDocHandler.KEY_PUBLISHED, EsQueryHandler.SORT_DESC));
                source.put("size", expandMaxNum);
                source.put("version", true);
                queries.add(source);
            }
        }

        //Since ID is known for N: N / N: 1/1: 1, add search query for IDS query
        if (!expandEntityIdList.isEmpty()) {
            Map<String, Object> source = new HashMap<String, Object>();

            List<Map<String, Object>> mustQueries = new ArrayList<Map<String, Object>>();
            Map<String, Object> idsQuery = new HashMap<String, Object>();
            Map<String, Object> valuesQuery = new HashMap<String, Object>();
            idsQuery.put("ids", valuesQuery);
            valuesQuery.put("values", expandEntityIdList);
            mustQueries.add(idsQuery);

            //In case of acquiring Box information, it is possible to retrieve two pieces of data of dav by designating only ID, so type information is specified
            Map<String, Object> typesQuery = new HashMap<String, Object>();
            Map<String, Object> typesBoolQuery = new HashMap<String, Object>();
            List<Map<String, Object>> shouldQueries = new ArrayList<Map<String, Object>>();
            for (String entityType : idsEntityTypes) {
                if (producer instanceof UserDataODataProducer) {
                    shouldQueries.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID, entityType));
                } else {
                    shouldQueries.add(QueryMapFactory.termQuery("_type", entityType));
                }
            }
            typesBoolQuery.put("should", shouldQueries);
            typesQuery.put("bool", typesBoolQuery);
            mustQueries.add(typesQuery);
            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(mustQueries));

            source.put("query", query);
            source.put("filter", implicitFilters);
            source.put("sort", QueryMapFactory.sortQuery(OEntityDocHandler.KEY_PUBLISHED, EsQueryHandler.SORT_DESC));
            source.put("size", expandEntityIdList.size());
            source.put("version", true);
            queries.add(source);
        }
        return queries;
    }

    /**
     * Set a Map to associate the entity type name specified in the expand query with the navigation property name.
     */
    private Map<String, String> getEntityTypeNameNavPropNameMap() {
        Map<String, String> map = new HashMap<String, String>();
        for (String navigationPropertyName : this.navigationPropertyList) {
            EdmNavigationProperty edmNavProp = this.edmBaseEntityType.findNavigationProperty(navigationPropertyName);
            String entityTypeName = edmNavProp.getToRole().getType().getName();
            map.put(entityTypeName, navigationPropertyName);
        }
        return map;
    }

}

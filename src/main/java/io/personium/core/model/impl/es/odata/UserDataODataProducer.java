/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model.impl.es.odata;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;
import org.odata4j.stax2.XMLEventReader2;
import org.odata4j.stax2.XMLFactoryProvider2;
import org.odata4j.stax2.XMLInputFactory2;
import org.odata4j.stax2.staximpl.StaxXMLFactoryProvider2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.UserDataSchemaCache;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.doc.PropertyDocHandler;
import io.personium.core.model.impl.es.doc.UserDataDocHandler;
import io.personium.core.model.impl.es.doc.UserDataLinkDocHandler;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumEdmxFormatParser;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyLinkType;
import io.personium.core.utils.EscapeControlCode;
import net.spy.memcached.internal.CheckedOperationTimeoutException;

/**
 * ODataProvider for user data OData service.
 */
public class UserDataODataProducer extends EsODataProducer {

    static Logger log = LoggerFactory.getLogger(UserDataODataProducer.class);

    /**
     * The namespace name of user OData.
     */
    public static final String USER_ODATA_NAMESPACE = "UserData";

    /**
     * Schema definition.
     */
    private EdmDataServices metadata = null;

    Cell cell;
    DavCmp davCmp;

    /**
     * Constructor.
     */
    public UserDataODataProducer() {
    }

    /**
     * Constructor.
     * @param cell Cell
     * @param davCmp DavCmp
     */
    public UserDataODataProducer(final Cell cell, final DavCmp davCmp) {
        this.cell = cell;
        this.davCmp = davCmp;
    }

    //Schema information
    private static EdmDataServices.Builder schemaEdmDataServices = CtlSchema.getEdmDataServicesForODataSvcSchema();

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        return null; //Implementation when necessary
    }

    @Override
    public EntitySetAccessor getAccessorForEntitySet(final String entitySetName) {
        return EsModel.cellCtl(cell, USER_ODATA_NAMESPACE);
    }

    @Override
    public ODataLinkAccessor getAccessorForLink() {
        return EsModel.cellCtlLink(cell);
    }

    @Override
    public DataSourceAccessor getAccessorForLog() {
        return null;
    }

    @Override
    public DataSourceAccessor getAccessorForBatch() {
        return EsModel.batch(cell);
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific EntityType, implement it to override here and return EntityTypeId.
     * @param entityTypeName EntityType name
     * Return @return EntityTypeId
     */
    @Override
    public String getEntityTypeId(final String entityTypeName) {
        return entityTypeIds.get(entityTypeName);
    }

    /**
     * Get DocHandler.
     * Type of @param type elasticsearch
     * @param oEntity OEntityWrapper
     * @return EntitySetDocHandler
     */
    @Override
    protected EntitySetDocHandler getDocHanlder(String type, OEntityWrapper oEntity) {
        UserDataDocHandler handler = new UserDataDocHandler(type, oEntity, this.getMetadata());
        handler.setPropertyAliasMap(getPropertyAliasMap());
        handler.setEntitySetName(oEntity.getEntitySet().getName());
        return handler;
    }

    @Override
    protected EntitySetDocHandler getDocHandler(PersoniumSearchHit searchHit, String entitySetName) {
        this.getMetadata();
        UserDataDocHandler handler = new UserDataDocHandler(searchHit);
        handler.setPropertyAliasMap(getPropertyAliasMap());
        handler.setEntitySetName(entitySetName);
        return handler;
    }

    @Override
    protected EntitySetDocHandler getDocHandler(PersoniumGetResponse response, String entitySetName) {
        UserDataDocHandler handler = new UserDataDocHandler(response);
        handler.setPropertyAliasMap(getPropertyAliasMap());
        handler.setEntitySetName(entitySetName);
        return handler;
    }

    @Override
    protected ODataQueryHandler getODataQueryHandler(final QueryInfo queryInfo,
            EdmEntityType edmEntityType,
            List<Map<String, Object>> implicitFilters) {
        ODataQueryHandler queryHandler = new UserDataQueryHandler(edmEntityType, getPropertyAliasMap());
        queryHandler.initialize(queryInfo, implicitFilters);
        return queryHandler;
    }

    /**
     * Get user schema ..
     * @return EdmDataServices edmDataServices
     */
    @Override
    public EdmDataServices getMetadata() {
        if (this.metadata == null) {
            reloadMetadata();
        }

        return this.metadata;
    }

    @SuppressWarnings("unchecked")
    private void reloadMetadata() {
        Map<String, Object> cache = UserDataSchemaCache.get(this.getNodeId());
        if (cache == null) {
            this.metadata = getMetadataFromDataSource();
            Map<String, Object> cacheSchema = createUserDataSchemaCache();

            if (cacheSchema != null) {
                //Because there is a possibility that the cache is created by another request while acquiring metadata
                //Only when the cache information is again acquired and does not exist, it is registered in the cache
                Map<String, Object> latestCache = UserDataSchemaCache.get(this.getNodeId());
                if (latestCache == null) {
                    try {
                        UserDataSchemaCache.cache(this.getNodeId(), cacheSchema);
                    } catch (RuntimeException e) {
                        if (e.getCause() instanceof CheckedOperationTimeoutException) {
                            log.info("Failed to cache UserDataSchema info.");
                        }
                    }
                }

            }

        } else if (UserDataSchemaCache.isDisabled(cache)) {
            this.metadata = getMetadataFromDataSource();
            Map<String, Object> cacheSchema = createUserDataSchemaCache();

            //Since there is a possibility that the cache has been changed by another request during metadata acquisition,
            //Only when the cache information has not been changed is registered in the cache
            if (cacheSchema != null && !UserDataSchemaCache.isChanged(this.getNodeId(), cache)) {
                try {
                    UserDataSchemaCache.cache(this.getNodeId(), cacheSchema);
                } catch (RuntimeException e) {
                    if (e.getCause() instanceof CheckedOperationTimeoutException) {
                        log.info("Failed to cache UserDataSchema info.(CacheOFF)");
                    }
                }
            }

        } else {
            this.entityTypeIds = (Map<String, String>) cache.get("entityTypeIds");
            setPropertyAliasMap((Map<String, PropertyAlias>) cache.get("propertyAliasMap"));
            setEntityTypeMap((Map<String, String>) cache.get("entityTypeMap"));
            //Set acquired information
            //If you pass InputStream to the XML parser (StAX, SAX, DOM) as is, the file list acquisition processing
            //Because it will be interrupted, store it as a provisional countermeasure and then parse it
            EdmDataServices metacache = null;
            try {
                StringReader sr = new StringReader((String) cache.get("edmx"));
                XMLFactoryProvider2 provider = StaxXMLFactoryProvider2.getInstance();
                XMLInputFactory2 factory = provider.newXMLInputFactory2();
                XMLEventReader2 reader = factory.createXMLEventReader(sr);
                PersoniumEdmxFormatParser parser = new PersoniumEdmxFormatParser();
                metacache = parser.parseMetadata(reader);
            } catch (RuntimeException ex) {
                log.info("XMLParseException: " + ex.getMessage(), ex.fillInStackTrace());
                throw ex;
            } catch (StackOverflowError tw) {
                //StackOverFlowError occurs when circular reference of ComplexType is made
                log.info("XMLParseException: " + tw.getMessage(), tw.fillInStackTrace());
                throw tw;
            }
            this.metadata = metacache;
        }
    }

    private Map<String, Object> createUserDataSchemaCache() {
        //I try to cache
        Map<String, Object> cache;
        cache = new HashMap<String, Object>();
        cache.put("entityTypeIds", this.entityTypeIds);
        cache.put("propertyAliasMap", getPropertyAliasMap());
        cache.put("entityTypeMap", getEntityTypeMap());
        StringWriter w = new StringWriter();
        try {
            EdmxFormatWriter.write(this.metadata, w);
        } catch (Exception e) {
            // If Exception(WstxIOException), control code(\u0000)
            // If a control code is included, it is not cached (because escape/unescape is required)
            return null;
        }

        //If a control code is included, it is not cached (because escape / unescape is required)
        if (EscapeControlCode.isContainsControlChar(w.toString())) {
            return null;
        }
        cache.put("edmx", w.toString());
        return cache;
    }

    private EdmDataServices getMetadataFromDataSource() {

        //The number of data acquisition is the maximum number of entity types and the maximum number of properties within one entity type
        int schemaPropertyGetCount =
                PersoniumUnitConfig.getUserdataMaxEntityCount() * PersoniumUnitConfig.getMaxPropertyCountInEntityType();

        //Specify Name as the sorting condition
        List<OrderByExpression> orderBy = OptionsQueryParser.parseOrderBy("Name");
        QueryInfo queryInfo = new QueryInfo(InlineCount.NONE, schemaPropertyGetCount,
                null, null, orderBy, null, null, null, null);

        UserSchemaODataProducer userSchemaODataProducer = new UserSchemaODataProducer(cell, davCmp);

        // EntityType
        EdmDataServices schemaDataServices = schemaEdmDataServices.build();
        EdmEntitySet esetEtype = schemaDataServices.findEdmEntitySet(EntityType.EDM_TYPE_NAME);
        EntitiesResponse typeResponse = userSchemaODataProducer.getEntities(EntityType.EDM_TYPE_NAME,
                queryInfo, esetEtype);
        this.entityTypeIds = userSchemaODataProducer.getEntityTypeIds();

        // AssociationEnd
        EdmEntitySet esetAssocEnd = schemaDataServices.findEdmEntitySet(AssociationEnd.EDM_TYPE_NAME);
        EntitiesResponse assoEndResponse = userSchemaODataProducer.getEntities(AssociationEnd.EDM_TYPE_NAME,
                queryInfo,
                esetAssocEnd);

        // Property
        EdmEntitySet esetProperty = schemaDataServices.findEdmEntitySet(Property.EDM_TYPE_NAME);
        EntitiesResponse propertyResponse = userSchemaODataProducer.getEntities(Property.EDM_TYPE_NAME, queryInfo,
                esetProperty);

        // ComplexType
        EdmEntitySet esetComplexType = schemaDataServices.findEdmEntitySet(ComplexType.EDM_TYPE_NAME);
        EntitiesResponse complexTypeResponse = userSchemaODataProducer.getEntities(ComplexType.EDM_TYPE_NAME,
                queryInfo, esetComplexType);

        // ComplexTypeProperty
        EdmEntitySet esetComplexTypeProperty = schemaDataServices
                .findEdmEntitySet(ComplexTypeProperty.EDM_TYPE_NAME);
        EntitiesResponse complexTypePropertyResponse = userSchemaODataProducer.getEntities(
                ComplexTypeProperty.EDM_TYPE_NAME, queryInfo, esetComplexTypeProperty);

        setPropertyAliasMap(userSchemaODataProducer.getPropertyAliasMap());
        setEntityTypeMap(userSchemaODataProducer.getEntityTypeMap());

        //Set acquired information
        EdmDataServices edmDataService = CtlSchema.getEdmDataServicesForUserData(USER_ODATA_NAMESPACE,
                typeResponse.getEntities(),
                assoEndResponse.getEntities(), propertyResponse.getEntities(), complexTypeResponse.getEntities(),
                complexTypePropertyResponse.getEntities()).build();
        return edmDataService;
    }

    @Override
    public String getCellId() {
        return this.cell.getId();
    }

    @Override
    public String getBoxId() {
        return davCmp.getBox().getId();
    }

    @Override
    public String getNodeId() {
        return davCmp.getId();
    }

    /**
     * Get key information of Links.
     * @param entityTypeName EntityType name
     * @return Return key information of links
     */
    public String getLinkskey(String entityTypeName) {
        return this.getEntityTypeId(entityTypeName);
    }

    /**
     * Implementation subclass If you want Producer to be associated with a specific EntityType, implement it to override here and return LinkDocHandler.
     * @param src srcEntitySetDocHandler
     * @param tgt tgtEntitySetDocHandler
     * @return LinkDocHandler
     */
    @Override
    public LinkDocHandler getLinkDocHandler(EntitySetDocHandler src, EntitySetDocHandler tgt) {
        return new UserDataLinkDocHandler(src, tgt);
    }

    @Override
    public LinkDocHandler getLinkDocHandler(PersoniumSearchHit searchHit) {
        return new UserDataLinkDocHandler(searchHit);
    }

    @Override
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        createDynamicPropertyEntity(docHandler);
    }

    @Override
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        createDynamicPropertyEntity(docHandler);
    }

    @Override
    public void beforeBulkCreate(final LinkedHashMap<String, BulkRequest> bulkRequests) {

        //Create Property of dynamic property not registered,
        //Move from the dynamic property field of the request information to the static property field
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {

            if (request.getValue().getError() != null) {
                continue;
            }

            if (!(request.getValue().getDocHandler() instanceof UserDataDocHandler)) {
                request.getValue().setError(PersoniumCoreException.Server.UNKNOWN_ERROR);
                continue;
            }

            UserDataDocHandler docHandler = (UserDataDocHandler) request.getValue().getDocHandler();
            Map<String, Object> dynamicProperties = docHandler.getDynamicFields();

            String entityTypeName = docHandler.getEntitySetName();
            if (dynamicProperties != null) {
                List<String> createdProperties = new ArrayList<String>();

                //Create a list of registered dynamic properties.
                for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
                    String propertyName = entry.getKey();
                    String key = String.format("Name='%s',_EntityType.Name='%s'", propertyName, entityTypeName);
                    if (this.getPropertyAliasMap().containsKey(key)) {
                        createdProperties.add(propertyName);
                    }
                    docHandler.getStaticFields().put(propertyName, entry.getValue());
                }

                //Since it can not be deleted within the EntrySet loop, it is deleted together.
                if (!createdProperties.isEmpty()) {
                    for (String propertyName : createdProperties) {
                        dynamicProperties.remove(propertyName);
                    }
                }
            }

            //If an error occurs during dynamic property creation, only the corresponding request, an error is set
            try {
                createDynamicPropertyEntity(docHandler);
            } catch (PersoniumCoreException e) {
                request.getValue().setError(e);
            }

            //Reflected added Property information in docHandler
            docHandler.setPropertyAliasMap(this.getPropertyAliasMap());
        }
    }

    /**
     * Entity of dynamic property is created.
     * @param docHandler Entity dock handler to register
     */
    public void createDynamicPropertyEntity(final EntitySetDocHandler docHandler) {
        EntitySetAccessor accessor = EsModel.cellCtl(cell, Property.EDM_TYPE_NAME);
        Map<String, Object> dynamicProperties = docHandler.getDynamicFields();
        List<PropertyDocHandler> propertyDocHandlerList = new ArrayList<PropertyDocHandler>();

        for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
            String type = EdmSimpleType.STRING.getFullyQualifiedTypeName();
            Object value = entry.getValue();
            if (value instanceof Double) {
                type = EdmSimpleType.DOUBLE.getFullyQualifiedTypeName();
            } else if (value instanceof Boolean) {
                type = EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName();
                ((UserDataDocHandler) docHandler).convertDynamicPropertyValue(entry.getKey(), value);
            }
            Map<String, Object> staticProperties = new HashMap<String, Object>();
            staticProperties.put(Property.P_NAME.getName(), entry.getKey());
            staticProperties.put(Property.P_TYPE.getName(), type);
            staticProperties.put(Property.P_COLLECTION_KIND.getName(), Property.COLLECTION_KIND_NONE);
            staticProperties.put(Property.P_DEFAULT_VALUE.getName(), null);
            staticProperties.put(Property.P_IS_KEY.getName(), false);
            staticProperties.put(Property.P_NULLABLE.getName(), true);
            staticProperties.put(Property.P_UNIQUE_KEY.getName(), null);
            staticProperties.put(Property.P_IS_DECLARED.getName(), false);

            PropertyDocHandler propertyDocHandler = new PropertyDocHandler(
                    getCellId(), getBoxId(), getNodeId(), docHandler.getEntityTypeId(), staticProperties);
            propertyDocHandler.setPropertyAliasMap(getPropertyAliasMap());
            propertyDocHandler.setEntityTypeMap(getEntityTypeMap());
            propertyDocHandlerList.add(propertyDocHandler);
        }

        if (dynamicProperties.size() != 0) {
            //It is checked whether Property of the same name has been registered in the same EntityType
            //If Property of the same name has already been registered, it is set as 503 error
            Map<String, Object> queryMap = createDynamicPropertyCountQuery(docHandler, propertyDocHandlerList);
            long count = accessor.count(queryMap);
            if (0 != count) {
                UserDataSchemaCache.clear(this.davCmp.getId());
                throw PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS;
            }

            for (PropertyDocHandler propertyDocHandler : propertyDocHandlerList) {
                accessor.create(propertyDocHandler);
            }

            UserDataSchemaCache.clear(this.davCmp.getId());
            reloadMetadata();
        }
    }

    /**
     * Check the upper limit of the number of links when registering entities collectively via NavigationProperty.
     * @param npBulkContexts Context of bulk registration
     * @param npBulkRequests Request information for entity batch registration (for bulkCreateEntity)
     */
    @Override
    public void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {

        //1. Analyze request information
        //Group request information for each source side type, source side key, target side type
        LinkedHashMap<String, BatchLinkContext> batchLinkContexts =
                new LinkedHashMap<String, BatchLinkContext>();
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            if (npBulkContext.isError()) {
                //If an error has already occurred, do not check the upper limit value
                continue;
            }
            if (NavigationPropertyLinkType.manyToMany != npBulkContext.getLinkType()) {
                //In the case of relation other than N: N, upper limit check is not carried out
                continue;
            }

            String bulkLinkContextsKey = getBatchLinkContextsKey(npBulkContext);
            if (!batchLinkContexts.containsKey(bulkLinkContextsKey)) {
                EntitySetDocHandler srcDocHandler = npBulkContext.getSourceDocHandler();
                String targetEntityTypeName = npBulkContext.getOEntityWrapper().getEntitySetName();
                String targetEntityTypeId = getEntityTypeId(targetEntityTypeName);
                BatchLinkContext batchLinkContext = new BatchLinkContext(srcDocHandler, targetEntityTypeName,
                        targetEntityTypeId);
                batchLinkContexts.put(bulkLinkContextsKey, batchLinkContext);
            }
        }

        //2. Acquire number of entries registered in ES
        //Acquire number registered for ES on source side type, source side key, target side type for each type
        ODataLinkAccessor accessor = getAccessorForLink();
        for (Entry<String, BatchLinkContext> entry : batchLinkContexts.entrySet()) {
            BatchLinkContext batchLinkContext = entry.getValue();
            LinkDocHandler.NtoNQueryParameter parameter = new LinkDocHandler.NtoNQueryParameter(
                    batchLinkContext.getSourceDocHandler(),
                    batchLinkContext.getTargetEntityTypeName(),
                    batchLinkContext.getTargetEntityTypeId());
            long count = accessor.count(parameter.getSource(0, 0));
            batchLinkContext.setExistsCount(count);
            log.info("Registered links count: key [" + entry.getKey() + "] count [" + count + "]");
        }

        //3. Request analysis + upper limit check
        int contextIndex = 0;
        for (BulkRequest npBulkRequest : npBulkRequests.values()) {
            NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
            if (npBulkContext.isError()) {
                //If an error has already occurred, do not check the upper limit value
                continue;
            }
            if (NavigationPropertyLinkType.manyToMany != npBulkContext.getLinkType()) {
                //In the case of relation other than N: N, upper limit check is not carried out
                continue;
            }

            String bulkLinkContextsKey = getBatchLinkContextsKey(npBulkContext);
            BatchLinkContext batchLinkContext = batchLinkContexts.get(bulkLinkContextsKey);
            if (batchLinkContext.getRegistCount() >= PersoniumUnitConfig.getLinksNtoNMaxSize()) {
                //When it exceeds the upper limit value of link
                npBulkRequest.setError(PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED);
                npBulkContext.setException(PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED);
            } else {
                //When it is normal, the number of analyzed items is incremented
                batchLinkContext.incrementRegistCount();
            }
        }
    }

    /**
     * Generate key of map of BatchLinkContext.
     * @param npBulkContext Context of bulk registration
     * @return BatchLinkContext map key
     */
    private String getBatchLinkContextsKey(NavigationPropertyBulkContext npBulkContext) {
        EntitySetDocHandler srcDocHandler = npBulkContext.getSourceDocHandler();
        String targetEntityTypeName = npBulkContext.getOEntityWrapper().getEntitySetName();
        String targetEntityTypeId = getEntityTypeId(targetEntityTypeName);

        //Since the ID of the EntityType is included in the key, there is a possibility that it may become null except for the user OData
        //Therefore, other than user OData is not supported
        return srcDocHandler.getEntityTypeId() + ":" + srcDocHandler.getId() + ":"
                + targetEntityTypeId;

    }

    private Map<String, Object> createDynamicPropertyCountQuery(final EntitySetDocHandler docHandler,
            List<PropertyDocHandler> propertyDocHandlerList) {
        Map<String, Object> queryMap = new HashMap<String, Object>();

        //Create a query for Cell, Box, NodeID
        Map<String, Object> query = QueryMapFactory.filteredQuery(null,
            QueryMapFactory.mustQuery(getImplicitFilters(null)));

        queryMap.put("query", query);

        //Create a query for EntityType name
        List<Map<String, Object>> andList = new ArrayList<Map<String, Object>>();
        Map<String, Object> entityMap = QueryMapFactory.termQuery(OEntityDocHandler.KEY_LINK + "."
                + EntityType.EDM_TYPE_NAME, docHandler.getEntityTypeId());
        andList.add(entityMap);

        //Create a query for Property Name
        List<Map<String, Object>> orList = new ArrayList<Map<String, Object>>();
        for (PropertyDocHandler propertyDocHandler : propertyDocHandlerList) {
            Map<String, Object> propertyNameMap = QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS
                    + "." + Property.P_NAME.getName() + ".untouched", propertyDocHandler.getName());
            orList.add(propertyNameMap);
        }
        Map<String, Object> orMap = new HashMap<String, Object>();
        orMap.put("or", orList);
        andList.add(orMap);

        Map<String, Object> andMap = new HashMap<String, Object>();
        andMap.put("and", andList);
        queryMap.put("filter", andMap);
        return queryMap;
    }

    /**
     * Mapping data of EntityType name and UUID.
     */
    private Map<String, String> entityTypeIds = new HashMap<String, String>();

    /**
     * @return the entityTypeIds
     */
    public Map<String, String> getEntityTypeIds() {
        return entityTypeIds;
    }

    @Override
    protected boolean checkUniquenessEntityKey(final String entitySetName, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {
        //In the case of UserData, since uniqueness is secured by __ id, uniqueness check as to whether the same data exist after link deletion is unnecessary
        return true;
    }

    @Override
    protected boolean checkUniquenessEntityKeyForAddLink(final String entitySetName,
            final EntitySetDocHandler src, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {
        //In the case of UserData, since uniqueness is secured by __ id, uniqueness check as to whether identical data exist after link registration is unnecessary
        return true;
    }

    @Override
    public void setNavigationTargetKeyProperty(EdmEntitySet eSet, EntitySetDocHandler oedh) {
        //NTKP in UserData (method of specifying related data like _Box.Name by property) is not implemented and processing is unnecessary
    }

    @Override
    public Map<String, Object> getLinkFieldsQuery(String entityTypeId, String id) {
        //{"term": {"ll": "Internal ID of EntityType: internal ID of UserData"}}
        return QueryMapFactory.termQuery("ll",
                String.format("%s:%s", entityTypeId, id));
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

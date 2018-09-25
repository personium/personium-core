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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.odata4j.core.NamedValue;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.CellAccessor;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataEntityAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.UserDataSchemaCache;
import io.personium.core.model.impl.es.doc.ComplexTypePropertyDocHandler;
import io.personium.core.model.impl.es.doc.ComplexTypePropertyUpdateDocHandler;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.doc.PropertyDocHandler;
import io.personium.core.model.impl.es.doc.PropertyUpdateDocHandler;
import io.personium.core.model.impl.es.odata.PropertyLimitChecker.CheckError;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.odata.PersoniumOptionsQueryParser;

/**
 * ODataProvider for OData service of user data schema.
 */
public class UserSchemaODataProducer extends EsODataProducer {

    static Logger log = LoggerFactory.getLogger(UserSchemaODataProducer.class);

    Cell cell;
    DavCmp davCmp;
    PersoniumODataProducer userDataProducer = null;

    /**
     * Constructor.
     * @param cell Cell
     * @param davCmp DavCmp
     */
    public UserSchemaODataProducer(final Cell cell, final DavCmp davCmp) {
        this.cell = cell;
        this.davCmp = davCmp;
    }

    //Schema information
    private static EdmDataServices.Builder edmDataServices = CtlSchema.getEdmDataServicesForODataSvcSchema();

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EsIndex index;
        if (esType instanceof CellAccessor) {
            index = ((CellAccessor) esType).getIndex();
        } else { // ODataEntityAccessor
            index = ((ODataEntityAccessor) esType).getIndex();
        }
        return EsModel.getDataSourceAccessorFromIndexName(index.getName());
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

    @Override
    public EdmDataServices getMetadata() {
        return edmDataServices.build();
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
     * Perform uniqueness check of data.
     * @param entitySetName entity name
     * @param entity Entity to register / update newly
     */
    @Override
    protected void checkUniqueness(String entitySetName, OEntityWrapper entity) {
        if (Property.EDM_TYPE_NAME.equals(entitySetName) || ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            //Since we are loading TODO Schema again, reviewing
            EntitiesResponse response = getEntities(entitySetName, null);
            if (response != null) {
                Map<String, PropertyAlias> propertyAliasMap = getPropertyAliasMap();
                String propertyKey = entity.getEntityKey().toKeyStringWithoutParentheses();
                // Name='p_name_1377142311063',_EntityType.Name='SalesDetail'
                if (propertyAliasMap.containsKey(propertyKey)) {
                    //If data exists, it will be a CONFLICT error
                    throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
                }
            }
        } else {
            super.checkUniqueness(entitySetName, entity);
        }
    }

    @Override
    public void afterCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        UserDataSchemaCache.disable(this.davCmp.getId());
    }

    @Override
    public void afterUpdate() {
        UserDataSchemaCache.disable(this.davCmp.getId());
    }

    @Override
    public void afterDelete() {
        UserDataSchemaCache.disable(this.davCmp.getId());
    }

    /**
     * Check processing necessary for registration.
     * @param entitySetName entitySetName
     * @param oEntity oEntity
     * @param docHandler docHandler
     */
    @Override
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        if (Property.EDM_TYPE_NAME.equals(entitySetName)) {
            //In the case of Protey, it is checked whether there is data of the target EntityType before registration
            String entityTypeName = oEntity.getProperty(Property.P_ENTITYTYPE_NAME.getName()).getValue().toString();
            if (!isEmpty(entityTypeName)) {
                //Allow only if data exists, only if Nullable is true
                String nullableKey = Property.P_NULLABLE.getName();
                String nullable = oEntity.getProperty(nullableKey).getValue().toString();
                if (nullable.equals("false")) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(nullableKey);
                }
            }
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            //In the case of ComplexTypeProerty, it is confirmed whether the target ComplexType data exist before registration
            String entityTypeName =
                    oEntity.getProperty(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName()).getValue().toString();
            if (!isEmpty(entityTypeName)) {
                //Allow only if data exists, only if Nullable is true
                String nullableKey = ComplexTypeProperty.P_NULLABLE.getName();
                String nullable = oEntity.getProperty(nullableKey).getValue().toString();
                if (nullable.equals("false")) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(nullableKey);
                }
            }
        } else if (EntityType.EDM_TYPE_NAME.equals(entitySetName)) {
            //Check the upper limit of the number of EntityTypes
            userDataProducer = ModelFactory.ODataCtl.userData(this.cell, this.davCmp);
            Iterator<EdmEntityType> entityTypeIter = userDataProducer.getMetadata().getEntityTypes().iterator();
            int entityTypeCount = 0;
            while (entityTypeIter.hasNext()) {
                entityTypeIter.next();
                entityTypeCount++;
            }
            if (entityTypeCount >= PersoniumUnitConfig.getUserdataMaxEntityCount()) {
                log.info("Number of EntityTypes exceeds the limit.");
                throw PersoniumCoreException.OData.ENTITYTYPE_COUNT_LIMITATION_EXCEEDED;
            }
        }

        //Perform limit value check at property addition.
        if (docHandler instanceof PropertyDocHandler) {
            PropertyLimitChecker checker = new PropertyLimitChecker(userDataProducer.getMetadata(),
                    (PropertyDocHandler) docHandler);
            List<CheckError> checkErrors = checker.checkPropertyLimits();
            if (0 < checkErrors.size()) {
                //Some kind of error occurred in some EntityType
                throw PersoniumCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED;
            }
        }
    }

    @Override
    public void beforeDelete(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        if (EntityType.EDM_TYPE_NAME.equals(entitySetName)) {
            //If entitySet is EntityType, if there is only one user data under entitySet, 409
            if (!isEmpty((String) oEntityKey.asSingleValue())) {
                throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
            }

            //Search and delete dynamically defined property information
            EntitySetAccessor accessor = getAccessorForEntitySet(Property.EDM_TYPE_NAME);

            List<Map<String, Object>> andfilter = new ArrayList<Map<String, Object>>();
            andfilter.add(QueryMapFactory.termQuery(
                    String.format("%s.%s", OEntityDocHandler.KEY_LINK,
                            EntityType.EDM_TYPE_NAME), docHandler.getId()));
            andfilter.add(QueryMapFactory.termQuery(
                    String.format("%s.%s.untouched", OEntityDocHandler.KEY_STATIC_FIELDS,
                            Property.P_IS_DECLARED.getName()), false));

            List<Map<String, Object>> queries = getImplicitFilters(Property.EDM_TYPE_NAME);

            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

            Map<String, Object> filter = new HashMap<String, Object>();
            filter.put("size", PersoniumUnitConfig.getMaxPropertyCountInEntityType());
            filter.put("version", true);
            filter.put("filter", QueryMapFactory.andFilter(andfilter));
            filter.put("query", query);
            PersoniumSearchResponse response = accessor.search(filter);

            for (PersoniumSearchHit hit : response.getHits().getHits()) {
                accessor.delete(this.getDocHandler(hit, hit.getType()));
            }
        } else if (ComplexType.EDM_TYPE_NAME.equals(entitySetName)) {
            String complexTypeName = oEntityKey.asSingleValue().toString();
            beforeDeleteForComplexType(complexTypeName);

        } else if (Property.EDM_TYPE_NAME.equals(entitySetName)) {
            //If entitySet is Property, if there is only one user data under entitySet, 409
            Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
            for (NamedValue<?> nv : nvSet) {
                if (!nv.getName().equals(Property.P_ENTITYTYPE_NAME.getName())) {
                    continue;
                }
                if (!isEmpty((String) nv.getValue())) {
                    throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
                }
            }
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            //When entitySet is ComplexTypeProperty and ComplexTypeProperty is associated with EntityType
            //If there is only one user data under the entitySet, 409 is returned
            //Get ComplexType name
            Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
            String complextypeName = null;
            for (NamedValue<?> nv : nvSet) {
                if (nv.getName().equals(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName())) {
                    complextypeName = nv.getValue().toString();
                }
            }

            List<String> complexTypeList = new ArrayList<String>();
            userDataProducer = ModelFactory.ODataCtl.userData(this.cell, this.davCmp);

            //Type ComplexType attached to ComplexTypeProperty to be deleted
            //Recursively search the set ComplexType and add it to the target ComplexType list
            complexTypeList.add(complextypeName);
            addComplexTypeNameLinkedComplexType(complextypeName, complexTypeList);

            //Search for an EntityType that sets the obtained ComplexType to be checked as a type
            List<String> entityTypeList = new ArrayList<String>();
            entityTypeList = getEntityTypeNameLinkedComplexType(complexTypeList);

            //It is checked whether user data is present in the obtained Entitytype
            for (String entityType : entityTypeList) {
                if (!isEmpty(entityType)) {
                    throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
                }
            }
        }
    }

    /**
     * Preprocessing of ComplexType deletion.
     * @param oEntityKey
     */
    private void beforeDeleteForComplexType(String complexTypeName) {
        //If there is a Property using ComplexType, return 409 error
        if (isUsedComplexType(complexTypeName, Property.EDM_TYPE_NAME)) {
            throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
        }
        //If ComplexTypeProperty using ComplexType exists, it returns 409 error
        if (isUsedComplexType(complexTypeName, ComplexTypeProperty.EDM_TYPE_NAME)) {
            throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
        }
    }

    /**
     * Check whether there is data using ComplexType as a type.
     * @param complexTypeName Complex type name to check
     * @param checkEntityType Entity type to check
     * @return true if it exists true if it does not exist false
     */
    private boolean isUsedComplexType(String complexTypeName, String checkEntityType) {
        EntitySetAccessor accessor = getAccessorForEntitySet(checkEntityType);

        List<Map<String, Object>> andfilter = new ArrayList<Map<String, Object>>();
        andfilter.add(QueryMapFactory.termQuery(
                String.format("%s.%s.untouched", OEntityDocHandler.KEY_STATIC_FIELDS,
                        Property.P_TYPE.getName()), complexTypeName));

        List<Map<String, Object>> queries = getImplicitFilters(checkEntityType);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("filter", QueryMapFactory.andFilter(andfilter));
        filter.put("query", query);

        long useComplexTypeDataCount = accessor.count(filter);
        if (useComplexTypeDataCount != 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the EntityType setting ComplexType as a type.
     * @param complexTypeList List of complexTypes associated with the ComplexTypeProperty to delete
     * @return List of EntityType setting ComplexType as type
     */
    protected List<String> getEntityTypeNameLinkedComplexType(List<String> complexTypeList) {
        ArrayList<String> entityTypeList = new ArrayList<String>();
        Iterator<EdmEntityType> entityTypeIter = null;
        for (String complexTypeName : complexTypeList) {
            entityTypeIter = userDataProducer.getMetadata().getEntityTypes().iterator();
            while (entityTypeIter.hasNext()) {
                EdmEntityType entityType = entityTypeIter.next();
                for (EdmProperty edmProp : entityType.getProperties()) {
                    if (edmProp.getType().getFullyQualifiedTypeName()
                            .equals(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + complexTypeName)
                            && !entityTypeList.contains(entityType.getName())) {
                        entityTypeList.add(entityType.getName());
                    }
                }
            }
        }
        return entityTypeList;
    }

    /**
     * Sets the ComplexType setting the specified ComplexType to type.
     * @param complexTypeName ComplexType name
     * @param complexTypeList List of complexTypes associated with the ComplexTypeProperty to delete
     */
    protected void addComplexTypeNameLinkedComplexType(String complexTypeName, List<String> complexTypeList) {
        Iterator<EdmComplexType> complexTypeIter = userDataProducer.getMetadata().getComplexTypes().iterator();

        while (complexTypeIter.hasNext()) {
            EdmComplexType complexType = complexTypeIter.next();
            for (EdmProperty edmProp : complexType.getProperties()) {
                if (edmProp.getType().getFullyQualifiedTypeName()
                        .equals(UserDataODataProducer.USER_ODATA_NAMESPACE + "." + complexTypeName)
                        && !complexTypeList.contains(complexType.getName())) {
                    complexTypeList.add(complexType.getName());
                    addComplexTypeNameLinkedComplexType(complexType.getName(), complexTypeList);
                }
            }
        }
    }

    /**
     * 1 - 0: Search processing is performed on the N side during N deletion processing.
     * @param np EdmNavigationProperty
     * @param entityKey entityKey
     * @return true if it exists
     */
    @Override
    public boolean findMultiPoint(final EdmNavigationProperty np, final OEntityKey entityKey) {
        EdmAssociationEnd from = np.getFromRole();
        EdmAssociationEnd to = np.getToRole();
        if ((EdmMultiplicity.ONE.equals(from.getMultiplicity())
                || EdmMultiplicity.ZERO_TO_ONE.equals(from.getMultiplicity()))
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            QueryInfo query = new EntityQueryInfo.Builder().build();

            //If the deletion target is EntityType and the retrieval target of the related data is Property, only the static property is set as the retrieval target
            if (EntityType.EDM_TYPE_NAME.equals(from.getType().getName())
                    && Property.EDM_TYPE_NAME.equals(to.getType().getName())) {
                BoolCommonExpression filter = PersoniumOptionsQueryParser.parseFilter("IsDeclared eq true");
                query = new QueryInfo(InlineCount.NONE, null, null, filter, null, null, null, null, null);
            }

            //Search and check that it is 0;
            CountResponse cr = getNavPropertyCount(from.getType().getName(), entityKey, to.getType().getName(),
                    query);
            return cr.getCount() > 0;
        }
        return false;
    }

    /**
     * Checks whether entityTypeName user data exists.
     * @param entityTypeName EntityType name
     * @return boolean
     */
    private boolean isEmpty(final String entityTypeName) {
        userDataProducer = ModelFactory.ODataCtl.userData(this.cell, this.davCmp);
        CountResponse countRes = userDataProducer.getEntitiesCount(entityTypeName, null);
        if (countRes.getCount() > 0) {
            return false;
        }
        return true;
    }

    /**
     * Get DocHandler.
     * Type of @param type elasticsearch
     * @param oEntity OEntityWrapper
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHanlder(String type, OEntityWrapper oEntity) {
        if (Property.EDM_TYPE_NAME.equals(type)) {
            PropertyDocHandler handler = new PropertyDocHandler(type, oEntity, this.getMetadata());
            handler.setPropertyAliasMap(getPropertyAliasMap());
            handler.setEntityTypeMap(getEntityTypeMap());
            return handler;
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(type)) {
            ComplexTypePropertyDocHandler handler =
                    new ComplexTypePropertyDocHandler(type, oEntity, this.getMetadata());
            handler.setPropertyAliasMap(getPropertyAliasMap());
            handler.setEntityTypeMap(getEntityTypeMap());
            return handler;
        }
        return new OEntityDocHandler(type, oEntity, this.getMetadata());
    }

    /**
     * Get a DocHandler for updating.
     * Type of @param type elasticsearch
     * @param oEntityWrapper OEntityWrapper
     * @return EntitySetDocHandler
     */
    @Override
    protected EntitySetDocHandler getUpdateDocHanlder(String type, OEntityWrapper oEntityWrapper) {
        if (Property.EDM_TYPE_NAME.equals(type)) {
            PropertyDocHandler handler = new PropertyUpdateDocHandler(type, oEntityWrapper, this.getMetadata());
            return handler;
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(type)) {
            ComplexTypePropertyDocHandler handler = new ComplexTypePropertyUpdateDocHandler(
                    type, oEntityWrapper, this.getMetadata());
            return handler;
        }
        return this.getDocHanlder(type, oEntityWrapper);

    }

    /**
     * EntityType name and UUID.
     */
    private Map<String, String> entityTypeIds = new HashMap<String, String>();

    /**
     * Get EntityType name and UUID.
     * @return the entityTypeIds
     */
    public Map<String, String> getEntityTypeIds() {
        return entityTypeIds;
    }

    /**
     * Set EntityType name and UUID.
     * @param oEntity oEntity
     * @param staticFields staticFields
     */
    @Override
    public void setEntityTypeIds(OEntityWrapper oEntity, Map<String, Object> staticFields) {
        if (EntityType.EDM_TYPE_NAME.equals(oEntity.getEntitySetName())) {
            String entityTypeName = (String) staticFields.get(EntityType.P_ENTITYTYPE_NAME.getName());
            entityTypeIds.put(entityTypeName, oEntity.getUuid());
        }
    }

    /**
     * Check unauthorized Link information.
     * @param sourceEntity source side Entity
     * @param targetEntity Target side Entity
     * @throws PersoniumCoreException AssociationEnd - AssociationEnd $ link is registered When an association has already been set between the same EntityType
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity)
            throws PersoniumCoreException {

        String targetType = targetEntity.getType();
        //When AssociationEnd - AssociationEnd $ links are registered, if an association is already established between the same EntityType, it is an error
        if (AssociationEnd.EDM_TYPE_NAME.equals(sourceEntity.getType())
                && AssociationEnd.EDM_TYPE_NAME.equals(targetType)) {
            String relatedTargetEntityTypeId = (String) targetEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
            checkAssociationEndToAssociationEndLink(sourceEntity, relatedTargetEntityTypeId);
        }
    }

    /**
     * Check unauthorized Link information.
     * @param sourceDocHandler Source side Entity
     * @param entity Target side Entity
     * @param targetEntitySetName EntitySet name of the target
     * @throws PersoniumCoreException AssociationEnd - AssociationEnd $ link is registered When an association has already been set between the same EntityType
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceDocHandler, OEntity entity, String targetEntitySetName)
            throws PersoniumCoreException {

        //When AssociationEnd - AssociationEnd $ links are registered, if an association is already established between the same EntityType, it is an error
        if (AssociationEnd.EDM_TYPE_NAME.equals(sourceDocHandler.getType())
                && AssociationEnd.EDM_TYPE_NAME.equals(targetEntitySetName)) {

            //Acquires the ID of the EntityType associated with the AssociationEnd on the target side
            EdmEntitySet entitySet = getMetadata().getEdmEntitySet(EntityType.EDM_TYPE_NAME);
            OEntityKey key = entity.getEntityKey();
            String entityTypeName = null;
            if (key == null || OEntityKey.KeyType.COMPLEX != key.getKeyType()) {
                //AssociationEnd KeyType is COMPLEX type so it can not be done
                log.info("Failed to get EntityType ID related with AssociationEnd.");
                throw PersoniumCoreException.OData.DETECTED_INTERNAL_DATA_CONFLICT;
            }
            Set<NamedValue<?>> nvSet = key.asComplexValue();
            for (NamedValue<?> nv : nvSet) {
                if ("_EntityType.Name".equals(nv.getName())) {
                    entityTypeName = (String) nv.getValue();
                }
            }
            OEntityKey oEntityKey = OEntityKey.parse("Name='" + entityTypeName + "'");
            EntitySetDocHandler relatedTargetEntityType = this.retrieveWithKey(entitySet, oEntityKey);
            String relatedTargetEntityTypeId = (String) relatedTargetEntityType.getId();

            checkAssociationEndToAssociationEndLink(sourceDocHandler, relatedTargetEntityTypeId);
        }
    }

    /**
     * Check unauthorized Link information (AssociationEnd - AssociationEnd).
     * @param sourceEntity source side Entity
     * @param relatedTargetEntityTypeId ID of the target side EntityType
     * @throws PersoniumCoreException AssociationEnd - AssociationEnd $ link is registered When an association has already been set between the same EntityType
     */
    private void checkAssociationEndToAssociationEndLink(
            EntitySetDocHandler sourceEntity,
            String relatedTargetEntityTypeId) throws PersoniumCoreException {
        //AssociationEnd list acquisition
        //The number of data acquisition is the maximum number of entity types and the maximum number of properties in one entity type
        int retrieveSize =
                PersoniumUnitConfig.getUserdataMaxEntityCount() * PersoniumUnitConfig.getMaxPropertyCountInEntityType();
        QueryInfo queryInfo = new QueryInfo(InlineCount.NONE, retrieveSize,
                null, null, null, null, null, null, null);
        EdmEntitySet edmAssociationEnd = getMetadata().findEdmEntitySet(AssociationEnd.EDM_TYPE_NAME);
        EntitiesResponse associationEndResponse = getEntities(
                AssociationEnd.EDM_TYPE_NAME,
                queryInfo,
                edmAssociationEnd);

        //Create list of EntityType associated with source / target AssociationEnd specified by $ links
        String relatedSourceEntityTypeId = (String) sourceEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
        // String relatedTargetEntityTypeId = (String) targetEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
        List<OEntityWrapper> relatedSourceEntityTypes = new ArrayList<OEntityWrapper>();
        Set<String> relatedTargetEntityTypes = new HashSet<String>();
        List<OEntity> associationEnds = associationEndResponse.getEntities();
        for (OEntity associationEndOEntity : associationEnds) {
            if (!(associationEndOEntity instanceof OEntityWrapper)) {
                //Unlikely route
                //Since an error occurs in subsequent processing, only log output is set here
                log.info("Failed to get AssociationEnd List.");
            }
            OEntityWrapper associationEnd = (OEntityWrapper) associationEndOEntity;
            if (relatedSourceEntityTypeId.equals(associationEnd.getLinkUuid(EntityType.EDM_TYPE_NAME))) {
                relatedSourceEntityTypes.add(associationEnd);
            }
            if (relatedTargetEntityTypeId.equals(associationEnd.getLinkUuid(EntityType.EDM_TYPE_NAME))) {
                relatedTargetEntityTypes.add(associationEnd.getUuid());
            }
        }

        //Check if relation is already set between same EntityType
        for (OEntityWrapper associationEnd : relatedSourceEntityTypes) {
            if (relatedTargetEntityTypes.contains(associationEnd.getLinkUuid(AssociationEnd.EDM_TYPE_NAME))) {
                throw PersoniumCoreException.OData.CONFLICT_DUPLICATED_ENTITY_RELATION;
            }
        }
    }

    @Override
    public void onChange(String entitySetName) {
        UserDataSchemaCache.clear(this.davCmp.getId());
    }

    /**
     * Check if it supports change.
     * @param entitySetName Entity set name
     * @param oedhExisting data existing in the data store
     * @param originalManeToNoelinkId Link information existing in the data store
     * @param oedhNew request data
     */
    @Override
    protected void checkAcceptableModification(String entitySetName,
            EntitySetDocHandler oedhExisting,
            Map<String, Object> originalManeToNoelinkId,
            EntitySetDocHandler oedhNew) {

        if (Property.EDM_TYPE_NAME.equals(entitySetName) || ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            //Not updated for Property before updating Type before update 32 and Type after update after Double
            String existingType = (String) oedhExisting.getStaticFields().get(Property.P_TYPE.getName());
            String requestType = (String) oedhNew.getStaticFields().get(Property.P_TYPE.getName());
            String message = String.format("%s 'Type' change from [%s] to [%s]", entitySetName, existingType,
                    requestType);

            if (!isAcceptableTypeModify(existingType, requestType)) {
                throw PersoniumCoreException.OData.OPERATION_NOT_SUPPORTED.params(message);
            }
        }

        //l Check whether the value of the field is updated
        for (Entry<String, Object> entry : originalManeToNoelinkId.entrySet()) {
            String requestKey = entry.getKey();
            Object requestValue = entry.getValue();
            Object existingValue = oedhExisting.getManyToOnelinkId().get(requestKey);
            if (!requestValue.equals(existingValue)) {
                throw PersoniumCoreException.OData.OPERATION_NOT_SUPPORTED.params(
                        String.format("%s '_%s.Name' change", entitySetName, requestKey));
            }
        }

        //Check whether the value of s field has been updated
        for (Entry<String, Object> entry : oedhNew.getStaticFields().entrySet()) {
            String requestKey = entry.getKey();
            Object requestValue = entry.getValue();
            Object existingValue = oedhExisting.getStaticFields().get(requestKey);
            if (isStaticFieldValueChanged(requestKey, requestValue, existingValue)) {
                String message = String.format("%s '%s' change from [%s] to [%s]",
                        entitySetName, requestKey, existingValue, requestValue);
                throw PersoniumCoreException.OData.OPERATION_NOT_SUPPORTED.params(message);
            }
        }
    }

    /**
     * Check whether the change of the value of Type is acceptable content.
     * @param existingType Type value before change
     * @param requestType Type value after change
     * @return true: Acceptable false: Unacceptable
     */
    private boolean isAcceptableTypeModify(String existingType, String requestType) {
        //The Type property is allowed only in the following cases
        //- Change from INT32 to Double
        //- No value change
        return (EdmSimpleType.INT32.getFullyQualifiedTypeName().equals(existingType) //NOPMD -To maintain readability
                && EdmSimpleType.DOUBLE.getFullyQualifiedTypeName().equals(requestType))
                || null != requestType && requestType.equals(existingType);
    }

    /**
     * It checks whether each field of the s field is about to update.
     * - Do not allow updates other than Type and Name.
     * @param requestKey Key of the field specified in the request
     * @param requestValue The value of the field specified in the request
     * @param existingValue The value of the current field
     * @return true or false
     */
    private boolean isStaticFieldValueChanged(String requestKey, Object requestValue, Object existingValue) {
        return (null == requestValue && null != existingValue) //NOPMD -To maintain readability
                || (null != requestValue && !requestKey.equals(Property.P_TYPE.getName()) //NOPMD
                        && !requestKey.equals(Property.P_NAME.getName())
                && !requestValue.equals(existingValue));
    }

    /**
     * It checks whether there is a document that refers to the key name of the EntitySet passed as an argument.
     * <p>
     * It is conceivable that the document to be updated is referred to by name (key name of EntitySet). <br />
     * In such a case, it is necessary to confirm whether or not the reference source document exists before updating the document.
     * </p>
     * @param entitySetName EntitySet name to be processed specified in the request URL
     * @param entityKey The key name of the processing target EntitySet specified in the request URL
     */
    protected void hasRelatedEntities(String entitySetName, OEntityKey entityKey) {

        if (!ComplexType.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        //Generate search query
        String[] searchTypes = {Property.EDM_TYPE_NAME, ComplexTypeProperty.EDM_TYPE_NAME };
        Map<String, Object> shouldQuery = QueryMapFactory.shouldQuery(QueryMapFactory.multiTypeTerms(searchTypes));
        List<Map<String, Object>> queries = getImplicitFilters(Property.EDM_TYPE_NAME);
        queries.add(0, shouldQuery);
        Map<String, Object> query = QueryMapFactory.termQuery("s.Type.untouched", entityKey.asSingleValue());
        Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(query, QueryMapFactory.mustQuery(queries));
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("size", 0); //Number of cases
        filter.put("query", filteredQuery);

        DataSourceAccessor accessor = getAccessorForIndex(entitySetName);
        PersoniumSearchResponse response = accessor.indexSearch(filter);
        if (0 < response.getHits().allPages()) {
            throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
        }
    }
}

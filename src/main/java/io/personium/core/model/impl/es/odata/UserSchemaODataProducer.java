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
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
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
import io.personium.core.odata.DcODataProducer;
import io.personium.core.odata.DcOptionsQueryParser;
import io.personium.core.odata.OEntityWrapper;

/**
 * ユーザデータスキーマのODataサービスむけODataProvider.
 */
public class UserSchemaODataProducer extends EsODataProducer {

    static Logger log = LoggerFactory.getLogger(UserSchemaODataProducer.class);

    Cell cell;
    DavCmp davCmp;
    DcODataProducer userDataProducer = null;

    /**
     * Constructor.
     * @param cell Cell
     * @param davCmp DavCmp
     */
    public UserSchemaODataProducer(final Cell cell, final DavCmp davCmp) {
        this.cell = cell;
        this.davCmp = davCmp;
    }

    // スキーマ情報
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
     * データの一意性チェックを行う.
     * @param entitySetName エンティティ名
     * @param entity 新しく登録・更新するEntity
     */
    @Override
    protected void checkUniqueness(String entitySetName, OEntityWrapper entity) {
        if (Property.EDM_TYPE_NAME.equals(entitySetName) || ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            // TODO Schemaを再度読み込んでいるため、見直すこと
            EntitiesResponse response = getEntities(entitySetName, null);
            if (response != null) {
                Map<String, PropertyAlias> propertyAliasMap = getPropertyAliasMap();
                String propertyKey = entity.getEntityKey().toKeyStringWithoutParentheses();
                // Name='p_name_1377142311063',_EntityType.Name='SalesDetail'
                if (propertyAliasMap.containsKey(propertyKey)) {
                    // データが存在したら CONFLICT エラーとする
                    throw DcCoreException.OData.ENTITY_ALREADY_EXISTS;
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
     * 登録時に必要なチェック処理.
     * @param entitySetName entitySetName
     * @param oEntity oEntity
     * @param docHandler docHandler
     */
    @Override
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        if (Property.EDM_TYPE_NAME.equals(entitySetName)) {
            // Proertyの場合は登録前に対象となるEntityTypeのデータが存在するか確認する
            String entityTypeName = oEntity.getProperty(Property.P_ENTITYTYPE_NAME.getName()).getValue().toString();
            if (!isEmpty(entityTypeName)) {
                // データが存在する場合、Nullableがtrueの場合のみ許可
                String nullableKey = Property.P_NULLABLE.getName();
                String nullable = oEntity.getProperty(nullableKey).getValue().toString();
                if (nullable.equals("false")) {
                    throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(nullableKey);
                }
            }
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            // ComplexTypeProertyの場合は登録前に対象となるComplexTypeのデータが存在するか確認する
            String entityTypeName =
                    oEntity.getProperty(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName()).getValue().toString();
            if (!isEmpty(entityTypeName)) {
                // データが存在する場合、Nullableがtrueの場合のみ許可
                String nullableKey = ComplexTypeProperty.P_NULLABLE.getName();
                String nullable = oEntity.getProperty(nullableKey).getValue().toString();
                if (nullable.equals("false")) {
                    throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(nullableKey);
                }
            }
        } else if (EntityType.EDM_TYPE_NAME.equals(entitySetName)) {
            // EntityType数の上限値をチェックする
            userDataProducer = ModelFactory.ODataCtl.userData(this.cell, this.davCmp);
            Iterator<EdmEntityType> entityTypeIter = userDataProducer.getMetadata().getEntityTypes().iterator();
            int entityTypeCount = 0;
            while (entityTypeIter.hasNext()) {
                entityTypeIter.next();
                entityTypeCount++;
            }
            if (entityTypeCount >= DcCoreConfig.getUserdataMaxEntityCount()) {
                log.info("Number of EntityTypes exceeds the limit.");
                throw DcCoreException.OData.ENTITYTYPE_COUNT_LIMITATION_EXCEEDED;
            }
        }

        // プロパティ追加時の制限値チェックを実行する。
        if (docHandler instanceof PropertyDocHandler) {
            PropertyLimitChecker checker = new PropertyLimitChecker(userDataProducer.getMetadata(),
                    (PropertyDocHandler) docHandler);
            List<CheckError> checkErrors = checker.checkPropertyLimits();
            if (0 < checkErrors.size()) {
                // 幾つかの EntityTypeで何らかのエラーが発生
                throw DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED;
            }
        }
    }

    @Override
    public void beforeDelete(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        if (EntityType.EDM_TYPE_NAME.equals(entitySetName)) {
            // entitySetがEntityTypeの場合、 entitySet配下のユーザデータが1件でもあれば、409
            if (!isEmpty((String) oEntityKey.asSingleValue())) {
                throw DcCoreException.OData.CONFLICT_HAS_RELATED;
            }

            // 動的に定義されたプロパティ情報を検索して、削除する
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
            filter.put("size", DcCoreConfig.getMaxPropertyCountInEntityType());
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
            // entitySetがPropertyの場合、 entitySet配下のユーザデータが1件でもあれば、409
            Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
            for (NamedValue<?> nv : nvSet) {
                if (!nv.getName().equals(Property.P_ENTITYTYPE_NAME.getName())) {
                    continue;
                }
                if (!isEmpty((String) nv.getValue())) {
                    throw DcCoreException.OData.CONFLICT_HAS_RELATED;
                }
            }
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            // entitySetがComplexTypePropertyで、かつ、ComplexTypePropertyがEntityTypeと紐ついている場合
            // entitySet配下のユーザデータが1件でもあれば、409を返却する
            // ComplexType名を取得
            Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
            String complextypeName = null;
            for (NamedValue<?> nv : nvSet) {
                if (nv.getName().equals(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName())) {
                    complextypeName = nv.getValue().toString();
                }
            }

            List<String> complexTypeList = new ArrayList<String>();
            userDataProducer = ModelFactory.ODataCtl.userData(this.cell, this.davCmp);

            // 削除対象のComplexTypePropertyに紐ついているComplexTypeを型に
            // 設定しているComplexTypeを再帰的に検索してチェック対象のComplexType一覧に追加する
            complexTypeList.add(complextypeName);
            addComplexTypeNameLinkedComplexType(complextypeName, complexTypeList);

            // 取得したチェック対象のComplexTypeを型に設定しているEntityTypeを検索する
            List<String> entityTypeList = new ArrayList<String>();
            entityTypeList = getEntityTypeNameLinkedComplexType(complexTypeList);

            // 取得したEntitytypeにユーザデータが存在するかチェックする
            for (String entityType : entityTypeList) {
                if (!isEmpty(entityType)) {
                    throw DcCoreException.OData.CONFLICT_HAS_RELATED;
                }
            }
        }
    }

    /**
     * ComplexTypeの削除前処理.
     * @param oEntityKey
     */
    private void beforeDeleteForComplexType(String complexTypeName) {
        // ComplexTypeを使用しているPropertyが存在する場合は409エラーを返却する
        if (isUsedComplexType(complexTypeName, Property.EDM_TYPE_NAME)) {
            throw DcCoreException.OData.CONFLICT_HAS_RELATED;
        }
        // ComplexTypeを使用しているComplexTypePropertyが存在する場合は409エラーを返却する
        if (isUsedComplexType(complexTypeName, ComplexTypeProperty.EDM_TYPE_NAME)) {
            throw DcCoreException.OData.CONFLICT_HAS_RELATED;
        }
    }

    /**
     * ComplexTypeを型として使用しているデータが存在するかチェックする.
     * @param complexTypeName チェック対象のコンプレックスタイプ名
     * @param checkEntityType チェック対象のエンティティタイプ
     * @return 存在する場合はtrue 存在しない場合はfalse
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
     * ComplexTypeを型に設定しているEntityTypeを取得する.
     * @param complexTypeList 削除対象のComplexTypePropertyと関連するcomplexTypeのList
     * @return ComplexTypeを型に設定しているEntityTypeの一覧
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
     * 指定されたComplexTypeを型に設定しているComplexTypeを設定する.
     * @param complexTypeName ComplexType名
     * @param complexTypeList 削除対象のComplexTypePropertyと関連するcomplexTypeの一覧
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
     * 1-0:Nの削除処理時にN側を検索処理を行う.
     * @param np EdmNavigationProperty
     * @param entityKey entityKey
     * @return 存在する場合true
     */
    @Override
    public boolean findMultiPoint(final EdmNavigationProperty np, final OEntityKey entityKey) {
        EdmAssociationEnd from = np.getFromRole();
        EdmAssociationEnd to = np.getToRole();
        if ((EdmMultiplicity.ONE.equals(from.getMultiplicity())
                || EdmMultiplicity.ZERO_TO_ONE.equals(from.getMultiplicity()))
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            QueryInfo query = new EntityQueryInfo.Builder().build();

            // 削除対象がEntityTypeで、関連データの検索対象がPropertyの場合は、静的プロパティのみ検索対象とする
            if (EntityType.EDM_TYPE_NAME.equals(from.getType().getName())
                    && Property.EDM_TYPE_NAME.equals(to.getType().getName())) {
                BoolCommonExpression filter = DcOptionsQueryParser.parseFilter("IsDeclared eq true");
                query = new QueryInfo(InlineCount.NONE, null, null, filter, null, null, null, null, null);
            }

            // 検索して０件であることを確認する;
            CountResponse cr = getNavPropertyCount(from.getType().getName(), entityKey, to.getType().getName(),
                    query);
            return (cr.getCount() > 0);
        }
        return false;
    }

    /**
     * entityTypeName配下のユーザデータが存在するかチェック.
     * @param entityTypeName EntityType名
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
     * DocHandlerを取得する.
     * @param type elasticsearchのType
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
     * 更新用のDocHandlerを取得する.
     * @param type elasticsearchのType
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
     * EntityType名とUUID.
     */
    private Map<String, String> entityTypeIds = new HashMap<String, String>();

    /**
     * EntityType名とUUIDを取得する.
     * @return the entityTypeIds
     */
    public Map<String, String> getEntityTypeIds() {
        return entityTypeIds;
    }

    /**
     * EntityType名とUUIDを設定する.
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
     * 不正なLink情報のチェックを行う.
     * @param sourceEntity ソース側Entity
     * @param targetEntity ターゲット側Entity
     * @throws DcCoreException AssociationEnd - AssociationEndの$links登録時、既に同一EntityType間に関連が設定されている場合
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity)
            throws DcCoreException {

        String targetType = targetEntity.getType();
        // AssociationEnd - AssociationEnd の$links登録時、既に同一EntityType間に関連が設定されている場合はエラーとする
        if (AssociationEnd.EDM_TYPE_NAME.equals(sourceEntity.getType())
                && AssociationEnd.EDM_TYPE_NAME.equals(targetType)) {
            String relatedTargetEntityTypeId = (String) targetEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
            checkAssociationEndToAssociationEndLink(sourceEntity, relatedTargetEntityTypeId);
        }
    }

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceDocHandler ソース側Entity
     * @param entity ターゲット側Entity
     * @param targetEntitySetName ターゲットのEntitySet名
     * @throws DcCoreException AssociationEnd - AssociationEndの$links登録時、既に同一EntityType間に関連が設定されている場合
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceDocHandler, OEntity entity, String targetEntitySetName)
            throws DcCoreException {

        // AssociationEnd - AssociationEnd の$links登録時、既に同一EntityType間に関連が設定されている場合はエラーとする
        if (AssociationEnd.EDM_TYPE_NAME.equals(sourceDocHandler.getType())
                && AssociationEnd.EDM_TYPE_NAME.equals(targetEntitySetName)) {

            // ターゲット側のAssociationEndに紐付くEntityTypeのIDを取得する
            EdmEntitySet entitySet = getMetadata().getEdmEntitySet(EntityType.EDM_TYPE_NAME);
            OEntityKey key = entity.getEntityKey();
            String entityTypeName = null;
            if (key == null || OEntityKey.KeyType.COMPLEX != key.getKeyType()) {
                // AssociationEndの場合KeyTypeはCOMPLEX型なのでありえないルート
                log.info("Failed to get EntityType ID related with AssociationEnd.");
                throw DcCoreException.OData.DETECTED_INTERNAL_DATA_CONFLICT;
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
     * 不正なLink情報(AssociationEnd - AssociationEnd)のチェックを行う.
     * @param sourceEntity ソース側Entity
     * @param relatedTargetEntityTypeId ターゲット側EntityTypeのID
     * @throws DcCoreException AssociationEnd - AssociationEndの$links登録時、既に同一EntityType間に関連が設定されている場合
     */
    private void checkAssociationEndToAssociationEndLink(
            EntitySetDocHandler sourceEntity,
            String relatedTargetEntityTypeId) throws DcCoreException {
        // AssociationEnd一覧取得
        // データ取得件数はエンティティタイプの最大数と1エンティティタイプ内の最大プロパティ数とする
        int retrieveSize =
                DcCoreConfig.getUserdataMaxEntityCount() * DcCoreConfig.getMaxPropertyCountInEntityType();
        QueryInfo queryInfo = new QueryInfo(InlineCount.NONE, retrieveSize,
                null, null, null, null, null, null, null);
        EdmEntitySet edmAssociationEnd = getMetadata().findEdmEntitySet(AssociationEnd.EDM_TYPE_NAME);
        EntitiesResponse associationEndResponse = getEntities(
                AssociationEnd.EDM_TYPE_NAME,
                queryInfo,
                edmAssociationEnd);

        // $linksで指定したソース／ターゲットAssociationEndに紐付くEntityTypeの一覧作成
        String relatedSourceEntityTypeId = (String) sourceEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
        // String relatedTargetEntityTypeId = (String) targetEntity.getManyToOnelinkId().get(EntityType.EDM_TYPE_NAME);
        List<OEntityWrapper> relatedSourceEntityTypes = new ArrayList<OEntityWrapper>();
        Set<String> relatedTargetEntityTypes = new HashSet<String>();
        List<OEntity> associationEnds = associationEndResponse.getEntities();
        for (OEntity associationEndOEntity : associationEnds) {
            if (!(associationEndOEntity instanceof OEntityWrapper)) {
                // ありえないルート
                // 以降の処理でエラーとなるため、ここではログ出力のみとする
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

        // 既に同一EntityType間に関連が設定されているかのチェック
        for (OEntityWrapper associationEnd : relatedSourceEntityTypes) {
            if (relatedTargetEntityTypes.contains(associationEnd.getLinkUuid(AssociationEnd.EDM_TYPE_NAME))) {
                throw DcCoreException.OData.CONFLICT_DUPLICATED_ENTITY_RELATION;
            }
        }
    }

    @Override
    public void onChange(String entitySetName) {
        UserDataSchemaCache.clear(this.davCmp.getId());
    }

    /**
     * サポートする変更かどうかをチェックする.
     * @param entitySetName エンティティセット名
     * @param oedhExisting データストアに存在するデータ
     * @param originalManeToNoelinkId データストアに存在するリンク情報
     * @param oedhNew リクエストデータ
     */
    @Override
    protected void checkAcceptableModification(String entitySetName,
            EntitySetDocHandler oedhExisting,
            Map<String, Object> originalManeToNoelinkId,
            EntitySetDocHandler oedhNew) {

        if (Property.EDM_TYPE_NAME.equals(entitySetName) || ComplexTypeProperty.EDM_TYPE_NAME.equals(entitySetName)) {
            // Propertyの更新前TypeがInt32、更新後TypeがDouble以外は未サポート
            String existingType = (String) oedhExisting.getStaticFields().get(Property.P_TYPE.getName());
            String requestType = (String) oedhNew.getStaticFields().get(Property.P_TYPE.getName());
            String message = String.format("%s 'Type' change from [%s] to [%s]", entitySetName, existingType,
                    requestType);

            if (!isAcceptableTypeModify(existingType, requestType)) {
                throw DcCoreException.OData.OPERATION_NOT_SUPPORTED.params(message);
            }
        }

        // lフィールドの値が更新されていないかのチェック
        for (Entry<String, Object> entry : originalManeToNoelinkId.entrySet()) {
            String requestKey = entry.getKey();
            Object requestValue = entry.getValue();
            Object existingValue = oedhExisting.getManyToOnelinkId().get(requestKey);
            if (!requestValue.equals(existingValue)) {
                throw DcCoreException.OData.OPERATION_NOT_SUPPORTED.params(
                        String.format("%s '_%s.Name' change", entitySetName, requestKey));
            }
        }

        // sフィールドの値が更新されていないかのチェック
        for (Entry<String, Object> entry : oedhNew.getStaticFields().entrySet()) {
            String requestKey = entry.getKey();
            Object requestValue = entry.getValue();
            Object existingValue = oedhExisting.getStaticFields().get(requestKey);
            if (isStaticFieldValueChanged(requestKey, requestValue, existingValue)) {
                String message = String.format("%s '%s' change from [%s] to [%s]",
                        entitySetName, requestKey, existingValue, requestValue);
                throw DcCoreException.OData.OPERATION_NOT_SUPPORTED.params(message);
            }
        }
    }

    /**
     * Typeの値の変更が許容してよい内容かどうかをチェックする.
     * @param existingType 変更前のType値
     * @param requestType 変更後のType値
     * @return true:許容できる false:許容できない
     */
    private boolean isAcceptableTypeModify(String existingType, String requestType) {
        // Typeプロパティは以下の場合のみ許容する
        // - INT32からDoubleへの変更
        // - 値の変更なし
        return (EdmSimpleType.INT32.getFullyQualifiedTypeName().equals(existingType)
                && EdmSimpleType.DOUBLE.getFullyQualifiedTypeName().equals(requestType))
                || (null != requestType && requestType.equals(existingType));
    }

    /**
     * sフィールドの各フィールドが更新しようとしているかをチェック. < br/>
     * - Type,Name以外の更新は許可しない。
     * @param requestKey リクエストで指定されたフィールドのキー
     * @param requestValue リクエストで指定されたフィールドの値
     * @param existingValue 現在のフィールドの値
     * @return true or false
     */
    private boolean isStaticFieldValueChanged(String requestKey, Object requestValue, Object existingValue) {
        return (null == requestValue && null != existingValue)
                || (null != requestValue && !requestKey.equals(Property.P_TYPE.getName())
                        && !requestKey.equals(Property.P_NAME.getName())
                && !requestValue.equals(existingValue));
    }

    /**
     * 引数で渡されたEntitySetのキー名を参照しているドキュメントがあるかどうかを確認する.
     * <p>
     * 更新対象のドキュメントを名前（EntitySetのキー名）で参照している場合が考えられる。<br />
     * このような場合、ドキュメント更新前に参照元のドキュメントが存在しているかどうかを確認する必要がある。
     * </p>
     * @param entitySetName リクエストURLに指定された処理対象のEntitySet名
     * @param entityKey リクエストURLに指定された処理対象EntitySetのキー名
     */
    protected void hasRelatedEntities(String entitySetName, OEntityKey entityKey) {

        if (!ComplexType.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        // 検索クエリの生成
        String[] searchTypes = {Property.EDM_TYPE_NAME, ComplexTypeProperty.EDM_TYPE_NAME };
        Map<String, Object> shouldQuery = QueryMapFactory.shouldQuery(QueryMapFactory.multiTypeTerms(searchTypes));
        List<Map<String, Object>> queries = getImplicitFilters(Property.EDM_TYPE_NAME);
        queries.add(0, shouldQuery);
        Map<String, Object> query = QueryMapFactory.termQuery("s.Type.untouched", entityKey.asSingleValue());
        Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(query, QueryMapFactory.mustQuery(queries));
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("size", 0); // 件数取得
        filter.put("query", filteredQuery);

        DataSourceAccessor accessor = getAccessorForIndex(entitySetName);
        PersoniumSearchResponse response = accessor.indexSearch(filter);
        if (0 < response.getHits().allPages()) {
            throw DcCoreException.OData.CONFLICT_HAS_RELATED;
        }
    }
}

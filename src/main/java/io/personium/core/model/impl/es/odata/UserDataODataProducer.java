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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.spy.memcached.internal.CheckedOperationTimeoutException;

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
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
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
import io.personium.core.odata.PersoniumEdmxFormatParser;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyLinkType;
import io.personium.core.utils.EscapeControlCode;

/**
 * ユーザデータのODataサービスむけODataProvider.
 */
public class UserDataODataProducer extends EsODataProducer {

    static Logger log = LoggerFactory.getLogger(UserDataODataProducer.class);

    /**
     * ユーザODataの名前空間名.
     */
    public static final String USER_ODATA_NAMESPACE = "UserData";

    /**
     * スキーマ定義.
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

    // スキーマ情報
    private static EdmDataServices.Builder schemaEdmDataServices = CtlSchema.getEdmDataServicesForODataSvcSchema();

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        return null; // 必要時に実装すること
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
     * 実装サブクラスProducerが特定のEntityTypeに紐付くようにしたいときは、ここをoverrideしてEntityTypeIdを返すように実装する。
     * @param entityTypeName EntityType名
     * @return EntityTypeIdを返す
     */
    @Override
    public String getEntityTypeId(final String entityTypeName) {
        return entityTypeIds.get(entityTypeName);
    }

    /**
     * DocHandlerを取得する.
     * @param type elasticsearchのType
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
     * ユーザスキーマ取得..
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
                // メタデータ取得中に別リクエストでキャッシュが作成している可能性があるため
                // キャッシュ情報を再度取得して存在していない場合のみ、キャッシュに登録する
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

            // メタデータ取得中に別リクエストでキャッシュを変更している可能性があるため、
            // キャッシュ情報が変更されていない場合のみ、キャッシュに登録する
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
            // 取得した情報を設定する
            // XMLパーサ(StAX,SAX,DOM)にInputStreamをそのまま渡すとファイル一覧の取得処理が
            // 中断してしまうため暫定対処としてバッファに格納してからパースする
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
                // ComplexTypeの循環参照時にStackOverFlowErrorが発生する
                log.info("XMLParseException: " + tw.getMessage(), tw.fillInStackTrace());
                throw tw;
            }
            this.metadata = metacache;
        }
    }

    private Map<String, Object> createUserDataSchemaCache() {
        // キャッシュしてみる
        Map<String, Object> cache;
        cache = new HashMap<String, Object>();
        cache.put("entityTypeIds", this.entityTypeIds);
        cache.put("propertyAliasMap", getPropertyAliasMap());
        cache.put("entityTypeMap", getEntityTypeMap());
        StringWriter w = new StringWriter();
        EdmxFormatWriter.write(this.metadata, w);

        // 制御コードが含まれていた場合は、キャッシュしない(エスケープ・アンエスケープの必要があるため)
        if (EscapeControlCode.isContainsControlChar(w.toString())) {
            return null;
        }
        cache.put("edmx", w.toString());
        return cache;
    }

    private EdmDataServices getMetadataFromDataSource() {

        // データ取得件数はエンティティタイプの最大数と1エンティティタイプ内の最大プロパティ数
        int schemaPropertyGetCount =
                PersoniumUnitConfig.getUserdataMaxEntityCount() * PersoniumUnitConfig.getMaxPropertyCountInEntityType();

        // ソート条件としてNameを指定する
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

        // 取得した情報を設定する
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
     * Linksのkey情報を取得する.
     * @param entityTypeName EntityType名
     * @return linksのkey情報を返す
     */
    public String getLinkskey(String entityTypeName) {
        return this.getEntityTypeId(entityTypeName);
    }

    /**
     * 実装サブクラスProducerが特定のEntityTypeに紐付くようにしたいときは、ここをoverrideしてLinkDocHandlerを返すように実装する。
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

        // 登録済みでないdynamic propertyのPropertyを作成し、
        // リクエスト情報のdynamic propertyフィールドからstatic propertyフィールドに移動する
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

                // 登録済みのdynamic propertiesのリストを作成する。
                for (Map.Entry<String, Object> entry : dynamicProperties.entrySet()) {
                    String propertyName = entry.getKey();
                    String key = String.format("Name='%s',_EntityType.Name='%s'", propertyName, entityTypeName);
                    if (this.getPropertyAliasMap().containsKey(key)) {
                        createdProperties.add(propertyName);
                    }
                    docHandler.getStaticFields().put(propertyName, entry.getValue());
                }

                // EntrySetのループ内では削除できないため、まとめて削除する。
                if (!createdProperties.isEmpty()) {
                    for (String propertyName : createdProperties) {
                        dynamicProperties.remove(propertyName);
                    }
                }
            }

            // 動的プロパティ作成時にエラーが発生した場合は、該当リクエストのみ、エラーを設定する
            try {
                createDynamicPropertyEntity(docHandler);
            } catch (PersoniumCoreException e) {
                request.getValue().setError(e);
            }

            // 追加したProperty情報をdocHandlerに反映
            docHandler.setPropertyAliasMap(this.getPropertyAliasMap());
        }
    }

    /**
     * 動的プロパティのEntity作成を行う.
     * @param docHandler 登録対象のエンティティドックハンドラ
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
            // 同一EntityTypeに、同名のPropertyが登録済みかをチェックする
            // 同名のPropertyが登録済みの場合は、503エラーとする
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
     * NavigationProperty経由でエンティティを一括登録する際のリンク数の上限値チェックを行う.
     * @param npBulkContexts 一括登録のコンテキスト
     * @param npBulkRequests エンティティ一括登録用のリクエスト情報（bulkCreateEntity用）
     */
    @Override
    public void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {

        // 1. リクエスト情報を解析
        // ソース側 type、ソース側 キー、ターゲット側 typeごとにリクエスト情報をグループ化
        LinkedHashMap<String, BatchLinkContext> batchLinkContexts =
                new LinkedHashMap<String, BatchLinkContext>();
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            if (npBulkContext.isError()) {
                // 既にエラーが発生している場合は上限値チェックは実施しない
                continue;
            }
            if (NavigationPropertyLinkType.manyToMany != npBulkContext.getLinkType()) {
                // N:N以外の関連の場合は上限値チェックは実施しない
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

        // 2. ESに登録済みの件数を取得
        // ソース側 type、ソース側 キー、ターゲット側 typeごとにESに登録済みの件数を取得
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

        // 3. リクエスト解析＋上限値チェック
        int contextIndex = 0;
        for (BulkRequest npBulkRequest : npBulkRequests.values()) {
            NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
            if (npBulkContext.isError()) {
                // 既にエラーが発生している場合は上限値チェックは実施しない
                continue;
            }
            if (NavigationPropertyLinkType.manyToMany != npBulkContext.getLinkType()) {
                // N:N以外の関連の場合は上限値チェックは実施しない
                continue;
            }

            String bulkLinkContextsKey = getBatchLinkContextsKey(npBulkContext);
            BatchLinkContext batchLinkContext = batchLinkContexts.get(bulkLinkContextsKey);
            if (batchLinkContext.getRegistCount() >= PersoniumUnitConfig.getLinksNtoNMaxSize()) {
                // リンクの上限値を超えている場合
                npBulkRequest.setError(PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED);
                npBulkContext.setException(PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED);
            } else {
                // 正常な場合は解析済み件数をインクリメントする
                batchLinkContext.incrementRegistCount();
            }
        }
    }

    /**
     * BatchLinkContextのマップのキーを生成する.
     * @param npBulkContext 一括登録のコンテキスト
     * @return BatchLinkContextのマップのキー
     */
    private String getBatchLinkContextsKey(NavigationPropertyBulkContext npBulkContext) {
        EntitySetDocHandler srcDocHandler = npBulkContext.getSourceDocHandler();
        String targetEntityTypeName = npBulkContext.getOEntityWrapper().getEntitySetName();
        String targetEntityTypeId = getEntityTypeId(targetEntityTypeName);

        // EntityTypeのIDをキーに含めているので、ユーザOData以外はnullになる可能性がある
        // このため、ユーザOData以外は未対応
        return srcDocHandler.getEntityTypeId() + ":" + srcDocHandler.getId() + ":"
                + targetEntityTypeId;

    }

    private Map<String, Object> createDynamicPropertyCountQuery(final EntitySetDocHandler docHandler,
            List<PropertyDocHandler> propertyDocHandlerList) {
        Map<String, Object> queryMap = new HashMap<String, Object>();

        // Cell,Box,NodeIDのクエリを作成する
        Map<String, Object> query = QueryMapFactory.filteredQuery(null,
            QueryMapFactory.mustQuery(getImplicitFilters(null)));

        queryMap.put("query", query);

        // EntityType名のクエリを作成する
        List<Map<String, Object>> andList = new ArrayList<Map<String, Object>>();
        Map<String, Object> entityMap = QueryMapFactory.termQuery(OEntityDocHandler.KEY_LINK + "."
                + EntityType.EDM_TYPE_NAME, docHandler.getEntityTypeId());
        andList.add(entityMap);

        // Property名のクエリを作成する
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
     * EntityType名とUUIDのマッピングデータ.
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
        // UserDataの場合は、__idで一意性が確保されているためリンク削除後に同一データが存在するかの一意性チェックは不要
        return true;
    }

    @Override
    protected boolean checkUniquenessEntityKeyForAddLink(final String entitySetName,
            final EntitySetDocHandler src, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {
        // UserDataの場合は、__idで一意性が確保されているためリンク登録後に同一データが存在するかの一意性チェックは不要
        return true;
    }

    @Override
    public void setNavigationTargetKeyProperty(EdmEntitySet eSet, EntitySetDocHandler oedh) {
        // UserDataにおけるNTKP（_Box.Nameのような関連データをプロパティで指定する方法）は未実装であるため処理不要
    }

    @Override
    public Map<String, Object> getLinkFieldsQuery(String entityTypeId, String id) {
        // { "term" : { "ll" : "EntityTypeの内部ID:UserDataの内部ID" }}
        return QueryMapFactory.termQuery("ll",
                String.format("%s:%s", entityTypeId, id));
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

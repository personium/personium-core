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
import io.personium.core.DcCoreException;
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

    // Expandで指定されたナビゲーションプロパティ名の一覧
    private List<String> navigationPropertyList = new ArrayList<String>();

    // Expand元のエンティティタイプ型
    private EdmEntityType edmBaseEntityType;

    // Expandエンティティの最大取得件数
    private int expandMaxNum;

    // データID毎のキャッシュ
    private Map<String, Map<String, List<OEntity>>> relatedEntitiesListCache;

    // リンク先データIDからリンク元IDを取得するためのマップ
    private Map<String, List<String>> baseDataIdMap;

    /**
     * コンストラクタ.
     * @param queryInfo クエリ情報
     * @param baseEntityType Expand元のエンティティタイプ型
     * @param expandMaxNum Expandエンティティの最大取得件数
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
            // スキーマに存在しないNavigationPropertyを指定した場合はエラーとする
            if (edmNavProp == null) {
                throw DcCoreException.OData.EXPAND_NTKP_NOT_FOUND_ERROR.params(navigationPropertyName);
            }
            this.navigationPropertyList.add(navigationPropertyName);
        }
    }

    /**
     * 指定されたエンティティのキャッシュを作成する.
     * @param baseEntity Expand元のエンティティ
     * @param producer producer
     */
    public void setCache(EntitySetDocHandler baseEntity, EsODataProducer producer) {
        List<EntitySetDocHandler> baseEntityList = new ArrayList<EntitySetDocHandler>();
        baseEntityList.add(baseEntity);
        setCache(baseEntityList, producer);
    }

    /**
     * 指定されたエンティティのキャッシュを作成する.
     * @param baseEntityList Expand元のエンティティ一覧
     * @param producer producer
     */
    public void setCache(List<EntitySetDocHandler> baseEntityList, EsODataProducer producer) {
        // キャッシュのクリア
        this.relatedEntitiesListCache = new HashMap<String, Map<String, List<OEntity>>>();
        this.baseDataIdMap = new HashMap<String, List<String>>();

        // キャッシュ対象がないときは空の状態で完了
        if (baseEntityList.isEmpty()) {
            return;
        }

        // Expand指定がない場合は、全データに空キャッシュを設定
        if (this.navigationPropertyList.isEmpty()) {
            for (EntitySetDocHandler baseEntity : baseEntityList) {
                String id = baseEntity.getId();
                Map<String, List<OEntity>> relatedEntitiesList = new HashMap<String, List<OEntity>>();
                this.relatedEntitiesListCache.put(id, relatedEntitiesList);
            }
            return;
        }

        // N : N の関係の場合はLinkテーブルからリンク先エンティティIDを検索して、取得対象エンティティID一覧に追加する
        List<String> relatedEntityIdList = new ArrayList<String>();
        List<String> entityTypeListForLinkTable = getEntityTypeListForLinkTable();
        if (!entityTypeListForLinkTable.isEmpty()) {
            List<String> idList = getRelatedEntityIdListFromLinkTable(baseEntityList, producer,
                    entityTypeListForLinkTable);
            relatedEntityIdList.addAll(idList);
        }

        // N : 1 / 1 : 1の関係の場合は自データにリンク先エンティティID情報が存在するので、取得対象エンティティID一覧に追加する
        // 1 : N の場合は実データ取得時に検索条件として自データのIDを指定するため、取得対象エンティティID一覧には追加しない
        List<String> idList = getEntityIdListFromBaseData(producer, baseEntityList);
        relatedEntityIdList.addAll(idList);

        // ID一覧を使ってexpandデータを取得する
        String baseEntityTypeName = this.edmBaseEntityType.getName();
        String baseEntityLinksKey = producer.getLinkskey(baseEntityTypeName);
        PersoniumMultiSearchResponse multiSearchResponse = getRelatedEntities(producer,
                relatedEntityIdList,
                baseEntityLinksKey,
                baseEntityList);

        // 有効なexpandクエリに対して、空リストを追加する
        for (EntitySetDocHandler baseEntity : baseEntityList) {
            String id = baseEntity.getId();
            Map<String, List<OEntity>> relatedEntitiesList = new HashMap<String, List<OEntity>>();
            for (String navPropName : this.navigationPropertyList) {
                List<OEntity> expandOEntityList = new ArrayList<OEntity>();
                relatedEntitiesList.put(navPropName, expandOEntityList);
            }
            this.relatedEntitiesListCache.put(id, relatedEntitiesList);
        }
        // expand先データが存在しなかった場合は空リストのみキャッシュにセットして終了
        if (multiSearchResponse == null) {
            return;
        }

        // 検索結果をrelatedEntitieslistに追加する
        for (PersoniumItem item : multiSearchResponse) {
            PersoniumSearchHit[] searchHits = item.getSearchHits();
            for (PersoniumSearchHit hit : searchHits) {
                // 取得データのDocHandlerを生成する
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

                // ベースデータに紐付くデータのみ返却データに追加する
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

                // DocHander生成
                EdmEntitySet edmEntitySet = producer.getMetadata().getEdmEntitySet(entityTypeName);
                producer.setNavigationTargetKeyProperty(edmEntitySet, docHandler);
                OEntity relatedEntity = docHandler.createOEntity(edmEntitySet);

                // エンティティタイプ名からNavigationProperty名を取得
                Map<String, String> entityTypeNameNavPropNameMap = getEntityTypeNameNavPropNameMap();
                String navPropName = entityTypeNameNavPropNameMap.get(entityTypeName);

                // リンク元一覧に含まれないエンティティはキャッシュ対象外とする
                for (String baseEntityId : baseEntityIds) {
                    if (this.relatedEntitiesListCache.containsKey(baseEntityId)) {
                        Map<String, List<OEntity>> relatedEntitiesList = this.relatedEntitiesListCache
                                .get(baseEntityId);
                        List<OEntity> expandOEntityList = relatedEntitiesList.get(navPropName);

                        // cacheにExpand対象のエンティティ一覧を追加
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
     * $expand指定されたEntityを取得する.
     * @param baseEntity Expand元のエンティティ
     * @param producer producer
     * @return OEntityリスト
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
     * Linkテーブル検索対象のエンティティタイプ一覧を返却する.
     * @return Linkテーブル検索対象のエンティティタイプ一覧
     */
    private List<String> getEntityTypeListForLinkTable() {
        List<String> entityTypeListForLinkTable = new ArrayList<String>();
        for (String expandEntity : this.navigationPropertyList) {
            // Multiplicityを取得
            EdmNavigationProperty edmNavProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNavProp);

            // N:Nの場合はLinkテーブル検索対象となるため、エンティティタイプ名をリストに追加する
            if (ODataUtils.Cardinality.MANY_MANY == cardinality) {
                String entityTypeName = edmNavProp.getToRole().getType().getName();
                entityTypeListForLinkTable.add(entityTypeName);
            }
        }
        return entityTypeListForLinkTable;
    }

    /**
     * Linkテーブルからリンク先エンティティIDを検索して、リンク先データのID一覧を返却する.
     * @param baseEntity リンク元エンティティ
     * @param producer EsODataProducer
     * @param entityTypeListForLinkTable 検索対象のエンティティタイプ一覧
     * @return リンク先データのID一覧
     */
    private List<String> getRelatedEntityIdListFromLinkTable(List<EntitySetDocHandler> baseEntityList,
            EsODataProducer producer,
            List<String> entityTypeListForLinkTable) {
        List<String> relatedEntityIdList = new ArrayList<String>();

        // ユーザデータの場合はエンティティタイプIDを利用するが、管理リソースの場合はエンティティタイプ名を利用する
        EntitySetDocHandler baseEntity = baseEntityList.get(0);
        String linksKey = baseEntity.getEntityTypeId();
        if (linksKey == null) {
            linksKey = baseEntity.getType();
        }

        // Linkテーブル検索用のクエリ一覧を作成
        List<Map<String, Object>> queries =
                createLinkTableSearchQueries(producer, entityTypeListForLinkTable, baseEntityList);
        if (queries.isEmpty()) {
            return relatedEntityIdList;
        }

        // Linkテーブルの検索(multisearch)
        ODataLinkAccessor accessor = producer.getAccessorForLink();
        PersoniumMultiSearchResponse multiSearchResponse = accessor.multiSearch(queries);

        // リンク先データのID一覧に追加
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
     * N:NのLinkテーブル検索用のクエリ一覧を作成.
     * @param producer EsODataProducer
     * @param entityTypeListForLinkTable 検索対象のエンティティタイプ一覧
     * @param baseEntityList リンク元エンティティ一覧
     * @return Linkテーブル検索用クエリ一覧
     */
    private List<Map<String, Object>> createLinkTableSearchQueries(EsODataProducer producer,
            List<String> entityTypeListForLinkTable,
            List<EntitySetDocHandler> baseEntityList) {
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();

        // cell / box / node指定と上記クエリをand条件
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
     * N:1 / 1:1 のリンク元データからリンク先データのID一覧を取得する.
     * @param producer EsODataProducer
     * @return Linkテーブル検索対象のエンティティタイプ一覧
     */
    private List<String> getEntityIdListFromBaseData(EsODataProducer producer,
            List<EntitySetDocHandler> baseEntityList) {
        List<String> idList = new ArrayList<String>();
        for (String expandEntity : this.navigationPropertyList) {
            // Multiplicityを取得
            EdmNavigationProperty edmNaviProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNaviProp);

            // N:1 / 1:1 の場合は自データにリンク先データのID情報が存在するため、エンティティID一覧に追加する
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
            // Multiplicityを取得
            EdmNavigationProperty edmNaviProp = this.edmBaseEntityType.findNavigationProperty(expandEntity);
            int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(edmNaviProp);

            // N:1 / 1:1 の場合は自データにリンク先データのID情報が存在するため、エンティティID一覧に追加する
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
        // cell / box / node指定と上記クエリをand条件
        Map<String, Object> implicitFilters = getImplicitFilters(producer);

        // 1:N検索用のクエリを追加
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

        // N:N / N:1 / 1:1 はIDが判明しているため、IDSクエリの検索用クエリを追加
        if (!expandEntityIdList.isEmpty()) {
            Map<String, Object> source = new HashMap<String, Object>();

            List<Map<String, Object>> mustQueries = new ArrayList<Map<String, Object>>();
            Map<String, Object> idsQuery = new HashMap<String, Object>();
            Map<String, Object> valuesQuery = new HashMap<String, Object>();
            idsQuery.put("ids", valuesQuery);
            valuesQuery.put("values", expandEntityIdList);
            mustQueries.add(idsQuery);

            // Boxの情報を取得する際にIDのみの指定ではdavのデータと2件取れてしまうため、Type情報を指定しておく
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
     * expandクエリで指定されたエンティティタイプ名をナビゲーションプロパティ名と対応付けるためのMapを設定する.
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

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
package com.fujitsu.dc.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.core4j.Enumerable;
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

import com.fujitsu.dc.common.es.EsBulkRequest;
import com.fujitsu.dc.common.es.response.DcBulkItemResponse;
import com.fujitsu.dc.common.es.response.DcBulkResponse;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcGetResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchHit;
import com.fujitsu.dc.common.es.response.DcSearchHits;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.common.es.response.EsClientException;
import com.fujitsu.dc.common.es.response.EsClientException.DcSearchPhaseExecutionException;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcCoreLog;
import com.fujitsu.dc.core.model.ctl.AssociationEnd;
import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.ComplexTypeProperty;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.core.model.ctl.ExtRole;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.core.model.ctl.ReceivedMessage;
import com.fujitsu.dc.core.model.ctl.SentMessage;
import com.fujitsu.dc.core.model.impl.es.QueryMapFactory;
import com.fujitsu.dc.core.model.impl.es.accessor.DataSourceAccessor;
import com.fujitsu.dc.core.model.impl.es.accessor.EntitySetAccessor;
import com.fujitsu.dc.core.model.impl.es.accessor.ODataLinkAccessor;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandlerForBulkRequest;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.EsNavigationTargetKeyProperty.NTKPNotFoundException;
import com.fujitsu.dc.core.model.lock.Lock;
import com.fujitsu.dc.core.model.lock.LockManager;
import com.fujitsu.dc.core.odata.DcODataProducer;
import com.fujitsu.dc.core.odata.OEntityWrapper;
import com.fujitsu.dc.core.rs.odata.AbstractODataResource;
import com.fujitsu.dc.core.rs.odata.BulkRequest;
import com.fujitsu.dc.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;
import com.fujitsu.dc.core.rs.odata.ODataBatchResource.NavigationPropertyLinkType;
import com.fujitsu.dc.core.utils.ODataUtils;

/**
 * ElasticSearchでODataを扱うProducer. 全体として、スキーマチェックは呼び出し側で行われた上で呼び出される前提とし、 本クラスでは無駄な２重チェックを行わないために、スキーマチェックは行わない。
 * 本クラスはElasticSearchでODataを扱う上での特有の処理に特化する。
 */
public abstract class EsODataProducer implements DcODataProducer {

    static Logger log = LoggerFactory.getLogger(EsODataProducer.class);

    private Map<String, String> entityTypeMap = new HashMap<String, String>();
    private Map<String, PropertyAlias> propertyAliasMap = new HashMap<String, PropertyAlias>();

    /**
     * entitySet名が属するESインデックスに対応したアクセサオブジェクトを返すようサブクラスで実装します.
     * @param entitySetName entitySet名
     * @return アクセサオブジェクト
     */
    public abstract DataSourceAccessor getAccessorForIndex(String entitySetName);

    /**
     * entitySet名に対応したアクセサオブジェクトを返すようサブクラスで実装します.
     * @param entitySetName entitySet名
     * @return アクセサオブジェクト
     */
    public abstract EntitySetAccessor getAccessorForEntitySet(String entitySetName);

    /**
     * リンク情報を格納するアクセサオブジェクトを返すようサブクラスで実装します.
     * @return アクセサオブジェクト
     */
    public abstract ODataLinkAccessor getAccessorForLink();

    /**
     * Logを格納するアクセサオブジェクトを返すようサブクラスで実装します.
     * @return アクセサオブジェクト
     */
    public abstract DataSourceAccessor getAccessorForLog();

    /**
     * Batchアクセサオブジェクトを返すようサブクラスで実装します.
     * @return アクセサオブジェクト
     */
    public abstract DataSourceAccessor getAccessorForBatch();

    /**
     * Constructor.
     */
    public EsODataProducer() {
    }

    /**
     * 実装サブクラスProducerが特定のCellに紐付くようにしたいときは、ここをoverrideしてcellIdを返すように実装する。
     * @return CellId
     */
    public String getCellId() {
        return null;
    }

    /**
     * 実装サブクラスProducerが特定のBoxに紐付くようにしたいときは、ここをoverrideしてboxIdを返すように実装する。
     * @return getBoxId
     */
    public String getBoxId() {
        return null;
    }

    /**
     * 実装サブクラスProducerが特定のNodeに紐付くようにしたいときは、ここをoverrideしてNodeIdを返すように実装する。
     * @return NodeIdを返す
     */
    public String getNodeId() {
        return null;
    }

    /**
     * Linksのkey情報を取得する.
     * @param entityTypeName EntityType名
     * @return linksのkey情報を返す
     */
    public String getLinkskey(String entityTypeName) {
        return entityTypeName;
    }

    /**
     * 実装サブクラスProducerが特定のEntityTypeに紐付くようにしたいときは、ここをoverrideしてLinkDocHandlerを返すように実装する。
     * @param src srcEntitySetDocHandler
     * @param tgt tgtEntitySetDocHandler
     * @return LinkDocHandler
     */
    public LinkDocHandler getLinkDocHandler(EntitySetDocHandler src, EntitySetDocHandler tgt) {
        return new LinkDocHandler(src, tgt);
    }

    /**
     * 検索結果からLinkDocHandlerを生成する.
     * @param searchHit 検索結果
     * @return LinkDocHandler
     */
    public LinkDocHandler getLinkDocHandler(DcSearchHit searchHit) {
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
     * プロパティ名とエイリアスの対応Mapを返す.
     * @return プロパティ名とエイリアスの対応Map
     */
    public Map<String, String> getEntityTypeMap() {
        return entityTypeMap;
    }

    /**
     * プロパティ名とエイリアスの対応Mapを設定する.
     * @param map プロパティ名とエイリアスの対応Map
     */
    public void setEntityTypeMap(Map<String, String> map) {
        this.entityTypeMap = map;
    }

    /**
     * プロパティ名とエイリアスの対応Mapを返す.
     * @return プロパティ名とエイリアスの対応Map
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return propertyAliasMap;
    }

    /**
     * プロパティ名とエイリアスの対応Mapを設定する.
     * @param map プロパティ名とエイリアスの対応Map
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> map) {
        this.propertyAliasMap = map;
    }

    /**
     * 実装サブクラスProducer登録処理を行いたいときは、ここをoverrideして、結果を返すよう実装する。
     * @param entitySetName エンティティセット名
     * @param oEntity 登録対象のエンティティ
     * @param docHandler 登録対象のエンティティドックハンドラ
     */
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
    }

    /**
     * 実装サブクラスProducer更新処理を行いたいときは、ここをoverrideして子データの存在チェックし、結果を返すよう実装する。
     * @param entitySetName エンティティセット名
     * @param oEntityKey 更新対象のエンティティキー
     * @param docHandler 更新対象のエンティティドックハンドラ
     */
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
    }

    /**
     * 実装サブクラスProducerが削除処理を行いたいときは、ここをoverrideして子データの存在チェックし、結果を返すよう実装する。
     * @param entitySetName エンティティセット名
     * @param oEntityKey 削除対象のエンティティキー
     * @param docHandler 削除対象ドキュメント
     */
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
    }

    /**
     * 実装サブクラスProducerがバルク一括登録処理を行いたいときは、ここをoverrideして子データの存在チェックし、結果を返すよう実装する。
     * @param bulkRequestDocHandler バルク一括登録のDocHandler
     */
    public void beforeBulkCreate(final LinkedHashMap<String, BulkRequest> bulkRequestDocHandler) {
    }

    /**
     * 実装サブクラスProducer登録処理を行いたいときは、ここをoverrideして、結果を返すよう実装する。
     * @param entitySetName エンティティセット名
     * @param oEntity 登録対象のエンティティ
     * @param docHandler 登録対象のエンティティドックハンドラ
     */
    public void afterCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
    }

    /**
     * 実装サブクラスProducer更新処理を行いたいときは、ここをoverrideして、結果を返すよう実装する。
     */
    public void afterUpdate() {
    }

    /**
     * 実装サブクラスProducer削除処理を行いたいときは、ここをoverrideして、結果を返すよう実装する。
     */
    public void afterDelete() {
    }

    /**
     * 1-0:Nの削除処理時にN側を検索処理を行う.
     * @param np EdmNavigationProperty
     * @param entityKey entityKey
     * @return 存在する場合true
     */
    public boolean findMultiPoint(final EdmNavigationProperty np, final OEntityKey entityKey) {
        EdmAssociationEnd from = np.getFromRole();
        EdmAssociationEnd to = np.getToRole();
        if ((EdmMultiplicity.ONE.equals(from.getMultiplicity())
                || EdmMultiplicity.ZERO_TO_ONE.equals(from.getMultiplicity()))
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            // 検索して０件であることを確認する;
            CountResponse cr = getNavPropertyCount(from.getType().getName(), entityKey, to.getType().getName(),
                    new EntityQueryInfo.Builder().build());
            return (cr.getCount() > 0);
        }
        return false;
    }

    /**
     * N:N、1-0:1-0の削除処理時にリンクの検索処理を行う.
     * @param np EdmNavigationProperty
     * @param entityKey entityKey
     * @return 存在する場合true
     */
    public boolean findLinks(final EdmNavigationProperty np, final OEntityKey entityKey) {
        EdmAssociationEnd from = np.getFromRole();
        EdmAssociationEnd to = np.getToRole();
        if (EdmMultiplicity.MANY.equals(from.getMultiplicity())
                && EdmMultiplicity.MANY.equals(to.getMultiplicity())) {
            OEntityId oeId = OEntityIds.create(from.getType().getName(), entityKey);
            EntityIdResponse response = null;
            String npName = np.getName();
            response = this.getLinks(oeId, npName);
            int count;
            if (response.getMultiplicity() == EdmMultiplicity.MANY) {
                count = response.getEntities().size();
            } else {
                OEntityId entityId = Enumerable.create(response.getEntities()).firstOrNull();
                if (entityId == null) {
                    count = 0;
                } else {
                    count = 1;
                }
            }
            return (count > 0);
        }
        return false;
    }

    /**
     * PK, UKで指定されたユニーク性確保のためOData空間のLockを行う.
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
        final int expandMaxNum = DcCoreConfig.getMaxExpandSizeForRetrive();

        // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EntitySetDocHandler oedh = this.retrieveWithKey(eSet, entityKey, queryInfo);

        if (oedh == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY;
        }

        ExpandEntitiesMapCreator creator = new ExpandEntitiesMapCreator(queryInfo, eSet.getType(), expandMaxNum);
        Map<String, List<OEntity>> expandEntitiesMap = creator.create(oedh, this);

        // NavigationTargetKeyPropertyを設定する
        setNavigationTargetKeyProperty(eSet, oedh);
        if (oedh instanceof OEntityDocHandler) {
            ((OEntityDocHandler) oedh).setExpandMaxNum(expandMaxNum);
        }
        // ESのレスポンスから OEntityをつくる
        List<EntitySimpleProperty> selectQuery = null;
        if (queryInfo != null) {
            selectQuery = queryInfo.select;
        }
        OEntityWrapper entity = oedh.createOEntity(eSet, this.getMetadata(), expandEntitiesMap, selectQuery);
        // ODataの Entity Responseを作る。
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
        final int expandMaxNum = DcCoreConfig.getMaxExpandSizeForRetrive();

        // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EntitySetDocHandler oedh = this.retrieveWithInternalId(eSet, internalId);

        if (oedh == null) {
            return null;
        }


        // NavigationTargetKeyPropertyを設定する
        setNavigationTargetKeyProperty(eSet, oedh);
        if (oedh instanceof OEntityDocHandler) {
            ((OEntityDocHandler) oedh).setExpandMaxNum(expandMaxNum);
        }
        // ESのレスポンスから OEntityをつくる
        OEntityWrapper entity = oedh.createOEntity(eSet, this.getMetadata(), null, null);
        // ODataの Entity Responseを作る。
        return entity;
    }


    /**
     * EntitySetDocHandlerにNavigationTargetKeyPropertyを設定する.
     * @param eSet EntitySet
     * @param oedh EntitySetDocHandler
     */
    @SuppressWarnings("unchecked")
    public void setNavigationTargetKeyProperty(EdmEntitySet eSet, EntitySetDocHandler oedh) {
        // NavigationTargetKeyPropertyを設定する
        Map<String, Object> staticFields = oedh.getStaticFields();
        Map<String, Object> links = oedh.getManyToOnelinkId();
        Enumerable<EdmProperty> eProps = eSet.getType().getProperties();
        for (EdmProperty eProp : eProps) {
            // リンク対象の検索情報を組み立てる
            String propertyName = eProp.getName();
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(propertyName);
            if (ntkp != null) {
                String entityType = ntkp.get("entityType");
                String propName = ntkp.get("propName");
                String linkId = (String) links.get(getLinkskey(entityType));
                // link情報が2階層以上かどうかをチェックする
                while (propName.startsWith("_")) {
                    // linkの情報を取得する
                    String id = (String) links.get(getLinkskey(entityType));
                    EntitySetAccessor esType = this.getAccessorForEntitySet(entityType);
                    DcGetResponse res = esType.get(id);
                    if (res != null) {
                        // linkに紐付いているlinkのIDを取得する
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
                    DcGetResponse res = esType.get(linkId);
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
     * エンティティを取得する.
     * @param entitySet エンティティセット
     * @param oEntityKey エンティティキー
     * @return 取得結果
     */
    protected EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey) {
        return retrieveWithKey(entitySet, oEntityKey, null);
    }

    /**
     * キーに従い一件取得を行う.
     * @param entitySet EdmEntitySet
     * @param oEntityKey OEntityKey
     * @param queryInfo EntityQueryInfo
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler retrieveWithKey(EdmEntitySet entitySet, OEntityKey oEntityKey, EntityQueryInfo queryInfo) {
        if (entitySet == null) {
            // スキーマチェックは上位でやっているのでEntitySetがnullを指定された時も例外を上げずにnullで返却する。
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

        // 複合キーが省略されていた場合は、ダミーキーを設定
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
                // TODO 省略可能なフィールドでない場合はエラーとする
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
     * Keyに従い一件取得を行う.
     * @param entitySet EdmEntitySet
     * @param keys Map<String, OProperty>
     * @param queryInfo queryInfo
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler retrieveWithKey(
            EdmEntitySet entitySet, Set<OProperty<?>> keys, EntityQueryInfo queryInfo) {
        // メソッドのロジックはクエリを作って検索して結果を詰めて返す流れ。

        // 1. ESのクエリ作成
        String entitySetName = entitySet.getName();

        // ESクエリは共通して次の構造となる {"filter": SOME_FILTER }
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();

        if (keys.size() == 1) {
            Map<String, Object> query = new HashMap<String, Object>();

            if (this.getCellId() == null) {
                // 特定CELLに紐付いていない場合
                // KeyがSINGLEのとき以下の形となる
                query = QueryMapFactory.filteredQuery(null, QueryMapFactory.termQuery(
                        OEntityDocHandler.KEY_STATIC_FIELDS + "." + keys.iterator().next().getName() + ".untouched",
                        keys.iterator().next().getValue()));
            } else {
                query = QueryMapFactory.filteredQuery(null,
                        QueryMapFactory.mustQuery(getImplicitFilters(entitySetName)));

                // 特定CELLに紐付いている場合 単一キーのとき
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
            // 複合キー
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

        // 2. ESへのリクエスト
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        DcSearchResponse res = esType.search(source);

        // 3. ESからの応答の評価
        // Indexつくりたてのときはresがnullになる。データがないのでnullを返せば良い.
        if (res == null) {
            return null;
        }
        DcSearchHits hits = res.getHits();
        // ヒット件数0はしなかったらNullを返す
        if (hits.getCount() == 0) {
            return null;
        }
        // データが２件以上返ったら異常事態
        if (hits.getAllPages() > 1) {
            DcCoreLog.OData.FOUND_MULTIPLE_RECORDS.params(hits.getAllPages()).writeLog();
            throw DcCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(new RuntimeException("multiple records ("
                    + hits.getAllPages() + ") found for the key ."));
        }
        // ここで晴れてhit数は１であることが保証されるのでその１件を返す。
        return getDocHandler(hits.getHits()[0], entitySetName);
    }
    private EntitySetDocHandler retrieveWithInternalId(EdmEntitySet eSet, String internalId) {
        EntitySetAccessor esType = this.getAccessorForEntitySet(eSet.getName());
        DcGetResponse getRes = esType.get(internalId);
        if (getRes == null) {
            return null;
        }
        return this.getDocHandler(getRes, eSet.getName());
    }


    /**
     * DocHandlerを取得する.
     * @param searchHit 検索結果
     * @param entitySetName エンティティセット名
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHandler(DcSearchHit searchHit, String entitySetName) {
        return new OEntityDocHandler(searchHit);
    }

    /**
     * DocHandlerを取得する.
     * @param type elasticsearchのType
     * @param oEntity OEntityWrapper
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHanlder(String type, OEntityWrapper oEntity) {
        return new OEntityDocHandler(type, oEntity, this.getMetadata());
    }

    /**
     * DocHandlerを取得する.
     * @param response GetResponse
     * @param entitySetName エンティティセット名
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getDocHandler(DcGetResponse response, String entitySetName) {
        return new OEntityDocHandler(response);
    }

    /**
     * 更新用のDocHandlerを取得する.
     * @param type elasticsearchのType
     * @param oEntityWrapper OEntityWrapper
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getUpdateDocHanlder(String type, OEntityWrapper oEntityWrapper) {
        return getDocHanlder(type, oEntityWrapper);
    }

    /**
     * 一件取得を行う.
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
        // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        // Cell / Box / Node / EntityTypeに基づいた暗黙フィルタの作成
        List<Map<String, Object>> implicitFilters = getImplicitFilters(entitySetName);
        // implicitFIltersを渡して、検索を実行する
        return execEntitiesRequest(queryInfo, eSet, esType, implicitFilters);
    }

    /**
     * Cell / Box / Node / EntityTypeに基づいた暗黙フィルタの作成.
     * @param entitySetName エンティティセット名
     * @return Cell / Box / Node / EntityTypeに基づいた暗黙フィルタ
     */
    protected List<Map<String, Object>> getImplicitFilters(String entitySetName) {
        String cellId = this.getCellId();
        String boxId = this.getBoxId();
        String nodeId = this.getNodeId();
        String entityTypeId = getEntityTypeId(entitySetName);
        return QueryMapFactory.getImplicitFilters(cellId, boxId, nodeId, entityTypeId, entitySetName);
    }

    /**
     * 一覧取得検索を実行する.
     * @param queryInfo クエリ情報
     * @param eSet エンティティセット
     * @param esType アクセサオブジェクト
     * @param implicitFilters 暗黙的な検索条件
     * @return EntitiesResponse エンティティ一覧
     */
    public EntitiesResponse execEntitiesRequest(final QueryInfo queryInfo,
            EdmEntitySet eSet,
            EntitySetAccessor esType,
            List<Map<String, Object>> implicitFilters) {
        final int expandMaxNum = DcCoreConfig.getMaxExpandSizeForList();

        // 条件検索等。
        ODataQueryHandler visitor = getODataQueryHandler(queryInfo, eSet.getType(), implicitFilters);
        Map<String, Object> source = visitor.getSource();

        DcSearchResponse res = null;
        try {
            res = esType.search(source);
        } catch (EsClientException ex) {
            if (ex.getCause() instanceof DcSearchPhaseExecutionException) {
                throw DcCoreException.Server.DATA_STORE_SEARCH_ERROR.reason(ex);
            }
        }
        // inlinecountの指定がallpagesの場合のみヒット件数を返却する
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
            DcSearchHit[] hits = res.getHits().getHits();
            Map<String, List<OEntity>> expandEntitiesMap = null;

            Map<String, String> ntkpProperties = new HashMap<String, String>();
            Map<String, String> ntkpValueMap = new HashMap<String, String>();
            getNtkpValueMap(eSet, ntkpProperties, ntkpValueMap);

            List<EntitySimpleProperty> selectQuery = null;
            if (queryInfo != null) {
                selectQuery = queryInfo.select;
            }

            // Propert/ComplexTypePropertyと、Aliasのマッピングデータを作成する
            // また、EntityType/ComplexTypeのUUIDと名前とのマッピングデータについても作成する(
            if (this.propertyAliasMap != null) {
                setEntityPropertyMap(eSet, hits, ntkpValueMap);
            }
            List<EntitySetDocHandler> entityList = new ArrayList<EntitySetDocHandler>();
            for (DcSearchHit hit : hits) {
                EntitySetDocHandler oedh = getDocHandler(hit, eSet.getName());
                entityList.add(oedh);
            }
            ExpandEntitiesMapCreator creator =
                    new ExpandEntitiesMapCreator(queryInfo, eSet.getType(), expandMaxNum);
            creator.setCache(entityList, this);

            for (EntitySetDocHandler oedh : entityList) {
                // entityKeyの生成
                List<String> keys = eSet.getType().getKeys();

                List<String> kv = new ArrayList<String>();
                for (String key : keys) {
                    kv.add(key);
                    // TODO キーがStringであることを仮定してしまってる。キーの値が文字列以外であるときは、その対応が必要。
                    String v = (String) oedh.getStaticFields().get(key);
                    if (v == null) {
                        v = AbstractODataResource.DUMMY_KEY;
                    }
                    kv.add(v);
                }
                expandEntitiesMap = creator.create(oedh, this);

                // NTKPHashMapから値を設定する
                Map<String, Object> staticFields = oedh.getStaticFields();
                Map<String, Object> links = oedh.getManyToOnelinkId();
                for (Map.Entry<String, String> ntkpProperty : ntkpProperties.entrySet()) {
                    String linksKey = getLinkskey(ntkpProperty.getValue());
                    if (links.containsKey(linksKey)) {
                        String linkId = links.get(linksKey).toString();
                        staticFields.put(ntkpProperty.getKey(), ntkpValueMap.get(ntkpProperty.getKey() + linkId));
                    } else {
                        staticFields.put(ntkpProperty.getKey(), null);
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
     * 検索結果をもとに、プロパティとAliasをマッピングする.
     * @param eSet EdmEntitySet
     * @param hits 検索結果
     * @param ntkpValueMap NTKPマップ
     */
    @SuppressWarnings("unchecked")
    private void setEntityPropertyMap(EdmEntitySet eSet, DcSearchHit[] hits, Map<String, String> ntkpValueMap) {
        String entityTypeKey;
        String linkTypeName;
        if (Property.EDM_TYPE_NAME.equals(eSet.getName())) {
            linkTypeName = EntityType.EDM_TYPE_NAME;
            entityTypeKey = Property.P_ENTITYTYPE_NAME.getName();
        } else if (ComplexTypeProperty.EDM_TYPE_NAME.equals(eSet.getName())) {
            linkTypeName = ComplexType.EDM_TYPE_NAME;
            entityTypeKey = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
        } else {
            // Property/ComplexTypeProperty以外は何もしない。
            return;
        }
        this.entityTypeMap.putAll(ntkpValueMap);

        List<String> processedPropertyAlias = new ArrayList<String>();
        for (DcSearchHit property : hits) {
            Map<String, Object> fields = property.getSource();
            Map<String, Object> staticFileds = ((Map<String, Object>) fields.get(OEntityDocHandler.KEY_STATIC_FIELDS));
            Map<String, Object> hideenFileds = ((Map<String, Object>) fields.get(OEntityDocHandler.KEY_HIDDEN_FIELDS));
            Map<String, Object> linkFileds = ((Map<String, Object>) fields.get(OEntityDocHandler.KEY_LINK));

            String propertyName = (String) staticFileds.get("Name");
            String propertyType = (String) staticFileds.get("Type");
            String propertyAlias = (String) hideenFileds.get("Alias");
            String entityTypeId = (String) linkFileds.get(linkTypeName);

            String entityTypeName = ntkpValueMap.get(entityTypeKey + entityTypeId);
            String key = "Name='" + propertyName + "'," + entityTypeKey + "='" + entityTypeName + "'";
            if (processedPropertyAlias.contains(key)) {
                // プロパティ名の重複を検出
                DcCoreLog.OData.DUPLICATED_PROPERTY_NAME.params(entityTypeId, key).writeLog();
                throw DcCoreException.OData.DUPLICATED_PROPERTY_NAME.params(propertyName);
            }
            processedPropertyAlias.add(key);
            this.propertyAliasMap.put(key, new PropertyAlias(linkTypeName, propertyName, propertyType, propertyAlias));
        }
    }

    /**
     * EntityType名とUUIDのマッピングデータを設定する.
     * @param oEntity oEntity
     * @param staticFields staticFields
     */
    public void setEntityTypeIds(OEntityWrapper oEntity, Map<String, Object> staticFields) {
    }

    /**
     * EntityType名とUUIDのマッピングデータを取得する.
     * @return the entityTypeIds
     */
    public Map<String, String> getEntityTypeIds() {
        return null;
    }

    /**
     * 実装サブクラスProducerが特定のEntityTypeに紐付くようにしたいときは、ここをoverrideしてEntityTypeIdを返すように実装する。
     * @param entityTypeName EntityType名
     * @return EntityTypeIdを返す
     */
    public String getEntityTypeId(final String entityTypeName) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void getNtkpValueMap(EdmEntitySet eSet,
            Map<String, String> ntkpProperties,
            Map<String, String> ntkpValueMap) {
        Enumerable<EdmProperty> eProps = eSet.getType().getProperties();
        for (EdmProperty eProp : eProps) {
            // リンク対象の検索情報を組み立てる
            String propertyName = eProp.getName();
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(propertyName);
            if (ntkp != null) {
                String entityType = ntkp.get("entityType");
                String propName = ntkp.get("propName");
                ntkpProperties.put(propertyName, entityType);

                // 1階層目のNTKPのエンティティ一覧を取得する
                EntitySetAccessor ntkpAccessor = this.getAccessorForEntitySet(entityType);
                // Cell、Box,NodeIDの検索条件を追加
                List<Map<String, Object>> implicitFilters = getImplicitFilters(entityType);
                Map<String, Object> searchQuery = new HashMap<String, Object>();
                if (implicitFilters.size() != 0) {
                    Map<String, Object> query = QueryMapFactory.filteredQuery(null,
                            QueryMapFactory.mustQuery(implicitFilters));

                    searchQuery.put("query", query);
                }

                DcSearchHit[] ntkpSearchResults = ntkpAccessor.search(searchQuery).getHits().getHits();

                // 2階層目のNTKPが存在する場合、2階層目のNTKPのエンティティ一覧を取得する
                DcSearchHit[] nestNtkpSearchResults = null;
                Map<String, String> nestNtkpValueMap = new HashMap<String, String>();
                if (propName.startsWith("_")) {
                    HashMap<String, String> tmpntkp = AbstractODataResource.convertNTKP(propName);
                    entityType = tmpntkp.get("entityType");
                    propName = tmpntkp.get("propName");
                    ntkpAccessor = this.getAccessorForEntitySet(entityType);
                    nestNtkpSearchResults = ntkpAccessor.search(searchQuery).getHits().getHits();
                    for (DcSearchHit nestNtkpSearchResult : nestNtkpSearchResults) {
                        String linkId = nestNtkpSearchResult.getId();
                        String linkNtkpValue = ((Map<String, Object>) nestNtkpSearchResult.getSource().get(
                                OEntityDocHandler.KEY_STATIC_FIELDS)).get(propName).toString();
                        nestNtkpValueMap.put(linkId, linkNtkpValue);
                    }
                }

                // LinkIDがKey,NTKPの値がValueのMapを作成する
                for (DcSearchHit ntkpSearchResult : ntkpSearchResults) {
                    String linkId = ntkpSearchResult.getId();
                    Map<String, Object> linkFields = (Map<String, Object>) ntkpSearchResult.getSource().get(
                            OEntityDocHandler.KEY_LINK);
                    String linkNtkpValue = null;
                    if (linkFields.containsKey(entityType)) {
                        linkNtkpValue = nestNtkpValueMap.get(linkFields.get(entityType));
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

    @Override
    public void deleteEntity(String entitySetName, OEntityKey entityKey, String etag) {
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(entitySetName);
        EdmEntityType srcType = srcSet.getType();

        // OData 空間全体をlockする
        Lock lock = this.lock();
        try {
            // レコードの存在確認＆削除のためのES id取得
            EntitySetDocHandler hit = this.retrieveWithKey(eSet, entityKey);

            // データが存在しないときは404を返す
            if (hit == null) {
                throw DcCoreException.OData.NO_SUCH_ENTITY;
            }
            // If-MatchヘッダとEtagの値が等しいかチェック
            ODataUtils.checkEtag(etag, hit);

            // Linkデータの有無を確認する
            for (EdmNavigationProperty np : srcType.getDeclaredNavigationProperties().toList()) {
                if (this.findMultiPoint(np, entityKey) || this.findLinks(np, entityKey)) {
                    throw DcCoreException.OData.CONFLICT_HAS_RELATED;
                }
            }

            // Link情報の削除
            // 紐ついているリンク情報を取得する
            Map<String, Object> target = hit.getManyToOnelinkId();
            for (Entry<String, Object> entry : target.entrySet()) {
                String key = entry.getKey();
                EntitySetAccessor targetEsType = this.getAccessorForEntitySet(key);

                // 削除するデータと紐ついているデータを取得する
                DcGetResponse linksRes = targetEsType.get(entry.getValue().toString());
                EntitySetDocHandler linksDocHandler = getDocHandler(linksRes, entitySetName);
                Map<String, Object> links = linksDocHandler.getManyToOnelinkId();

                // 取得したデータがlinks情報を持っている場合、links情報を削除してデータ更新する
                String linksKey = getLinkskey(entitySetName);
                if (links.containsKey(linksKey)) {
                    links.remove(linksKey);
                    linksDocHandler.setManyToOnelinkId(links);
                    targetEsType.update(entry.getValue().toString(), linksDocHandler);
                }
            }

            // 削除前処理
            this.beforeDelete(entitySetName, entityKey, hit);
            DcDeleteResponse res = null;
            // 削除処理
            res = esType.delete(hit);

            if (res == null) {
                throw DcCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(new RuntimeException("not found"));
            }
            // TransportClient内でリトライ処理が行われた場合、レスポンスとしてNotFoundが返却される。
            // このため、ここではそのチェックは行わず、NotFoundが返却されても正常終了とする。
            if (res.isNotFound()) {
                log.info("Request data is already deleted. Then, return success response.");
            }

            // 削除後の処理
            this.afterDelete();

        } finally {
            log.debug("unlock");
            // unlockする
            lock.release();
        }
    }

    /**
     * EntitySet名とOEntityからEntitySetDocHandlerを生成して取得する.
     * @param entitySetName EntitySet名
     * @param entity OEntity
     * @return EntitySetDocHandler
     */
    @Override
    public EntitySetDocHandler getEntitySetDocHandler(final String entitySetName, final OEntity entity) {
        // DocHandlerを生成する
        OEntityKey entityKey = entity.getEntityKey();
        EntitySetDocHandler docHandler = getDocHanlder(entitySetName, (OEntityWrapper) entity);

        // Cell, Box, Nodeの紐付を行う
        docHandler.setCellId(this.getCellId());
        docHandler.setBoxId(this.getBoxId());
        docHandler.setNodeId(this.getNodeId());

        // 複合キーでNTKPの項目(ex. _EntityType.Name)があれば、リンク情報を設定する
        if (KeyType.COMPLEX.equals(entityKey.getKeyType())) {
            setLinksFromOEntity(entity, docHandler);
        }
        return docHandler;
    }

    /**
     * Entityの作成処理.このなかで主キーやUKによる一意チェックも行う.一意性の問題があれば例外を発生する.
     * @param entitySetName the entity-set name
     * @param entity the request entity sent from the client
     * @return the newly-created entity, fully populated with the key and default properties
     */
    @Override
    public EntityResponse createEntity(final String entitySetName, final OEntity entity) {
        OEntityKey entityKey = entity.getEntityKey();
        // リクエストのOEntityからelasticsearchに登録する形式のJSONObjectに変換する
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        OEntityWrapper oew = (OEntityWrapper) entity;

        // ユニーク性チェックのためまずロックを行う
        // OData 空間全体をlockする(将来的に必要があればentitySetNameでロック)
        Lock lock = this.lock();
        try {
            return createEntity(entitySetName, entity, entityKey, esType, oew);
        } finally {
            // ロックの解除
            log.debug("unlock");
            lock.release();
        }
    }

    private EntityResponse createEntity(final String entitySetName,
            final OEntity entity,
            OEntityKey entityKey,
            EntitySetAccessor esType,
            OEntityWrapper oew) {
        checkUniqueness(entitySetName, oew);

        EntitySetDocHandler oedh = getDocHanlder(esType.getType(), oew);
        // Cell, Box, Nodeの紐付
        oedh.setCellId(this.getCellId());
        oedh.setBoxId(this.getBoxId());
        oedh.setNodeId(this.getNodeId());
        oedh.setEntityTypeId(this.getEntityTypeId(entitySetName));

        // 複合キーでNTKPの項目(ex. _EntityType.Name)があれば、リンク情報を設定する
        if (KeyType.COMPLEX.equals(entityKey.getKeyType())) {
            try {
                setLinksFromOEntity(entity, oedh);
            } catch (NTKPNotFoundException e) {
                throw DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
            }
        }

        // 登録前処理
        this.beforeCreate(entitySetName, entity, oedh);

        // データが存在しなければ、esJsonをESに保存する
        DcIndexResponse idxRs = null;
        idxRs = esType.create(oedh.getId(), oedh);

        // 登録後処理
        this.afterCreate(entitySetName, entity, oedh);

        Long version = idxRs.getVersion();
        oedh.setVersion(version);
        String etag = oedh.createEtag();
        oew.setEtag(etag);
        oew.setUuid(idxRs.getId());
        return Responses.entity(oew);
    }

    /**
     * データの一意性チェックを行う.
     * @param entitySetName エンティティ名
     * @param oew 新しく登録・更新するEntity
     */
    protected void checkUniqueness(String entitySetName, OEntityWrapper oew) {
        ODataProducerUtils.checkUniqueness(this, oew, null, null);
    }

    /**
     * OEntityにNTKPの項目があれば、リンク情報を設定する.
     * @param entity リクエスト情報OEntity
     * @param oedh 登録データのドキュメントハンドラー
     * @throws NTKPNotFoundException NTKPで指定されたリソースが存在しない
     */
    private void setLinksFromOEntity(final OEntity entity, EntitySetDocHandler oedh) throws NTKPNotFoundException {
        // EntityKeyのPropertyをもとに、リンク情報を取得する
        Set<OProperty<?>> properties = entity.getEntityKey().asComplexProperties();
        EsNavigationTargetKeyProperty esNtkp = new EsNavigationTargetKeyProperty(this.getCellId(), this.getBoxId(),
                this.getNodeId(), entity
                        .getEntityType().getName(), this);
        esNtkp.setProperties(properties);
        Map.Entry<String, String> link = esNtkp.getLinkEntry();

        // NTKPの情報をOEDHから削除する
        for (OProperty<?> property : properties) {
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(property.getName());
            if (ntkp != null) {
                oedh.getStaticFields().remove(property.getName());
            }
        }

        // リンク情報が設定されていない場合はリンク情報を空にする
        if (link == null) {
            oedh.setManyToOnelinkId(new HashMap<String, Object>());
            return;
        }
        // 存在していれば、登録Entityにリンク情報を追加する
        // 例）AssociationEndの登録の場合 "l":{"EntityType":"EntityTypeのUUID"}
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
        // NavigationProperty経由のデータ作成を実装する。
        EdmEntitySet eSet = this.getMetadata().findEdmEntitySet(entitySetName);
        // OData 空間全体をlockする(将来的に必要があればentitySetNameでロック)
        Lock lock = this.lock();
        try {
            // src側のりソース存在確認.主キーでの検索を行う。
            EntitySetDocHandler srcDh = this.retrieveWithKey(eSet, entityKey);
            if (srcDh == null) {
                // src側のりソースが存在しなかったら404 エラーとする
                throw DcCoreException.OData.NO_SUCH_ENTITY;
            }
            // 関連のタイプ (NN/1N/N1/11) を判定

            // tgt側レコードの作成
            // srcとtgtの紐付
            // TODO 実装未完了
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
            throw DcCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        // n:1かn:nの切り分けを行う
        EdmAssociation assoc = srcNavProp.getRelationship();
        // ユニーク性チェックのためまずロックを行う
        // OData 空間全体をlockする(将来的に必要があればentitySetNameでロック)
        Lock lock = this.lock();
        try {
            EntitySetDocHandler src = this.retrieveWithKey(sourceEntity);
            // データが存在しない場合は404
            if (src == null) {
                throw DcCoreException.OData.NOT_FOUND;
            }
            EntitySetDocHandler tgt = this.retrieveWithKey(targetEntity);
            // ターゲットが存在しない場合は400
            if (tgt == null) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
            }
            createLinks(sourceEntity, srcNavProp, assoc, src, tgt);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * リンク元・先のデータチェックや、既にリンクが作成済みかなどの、データ登録の前提条件をチェックする.
     * @param navigationPropertyContext NP経由登録用のコンテキスト
     */
    private void validateLinkForNavigationPropertyContext(NavigationPropertyBulkContext navigationPropertyContext) {
        OEntityId sourceEntity = navigationPropertyContext.getSrcEntityId();
        OEntity targetEntity = navigationPropertyContext.getOEntityWrapper();

        String targetNavProp = navigationPropertyContext.getTgtNavProp();
        String srcSetName = sourceEntity.getEntitySetName();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);

        if (srcNavProp == null) {
            throw DcCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        EntitySetDocHandler src = navigationPropertyContext.getSourceDocHandler();
        // データが存在しない場合は404
        if (src == null) {
            throw DcCoreException.OData.NOT_FOUND;
        }

        // 1:1の関連は存在し得ないので、AssociationEnd - AssociationEndの$linksで1:1の登録をしようとした場合はエラーとする
        String targetEntitySetName = targetEntity.getEntitySetName();
        checkAssociationEndMultiplicity(targetNavProp, targetEntity, targetEntitySetName, src);

        // $linksの登録済みチェック
        checkExistsLink(sourceEntity, srcNavProp, src, targetEntity);
    }

    /**
     * NP経由のリンク情報をコンテキストに設定する.
     * @param navigationPropertyContext NP経由登録用のコンテキスト
     */
    private void setNavigationPropertyContext(NavigationPropertyBulkContext navigationPropertyContext) {
        OEntityId sourceEntity = navigationPropertyContext.getSrcEntityId();

        String srcSetName = sourceEntity.getEntitySetName();
        String targetNavProp = navigationPropertyContext.getTgtNavProp();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(srcSetName, targetNavProp);

        EntitySetDocHandler sourceDocHandler = navigationPropertyContext.getSourceDocHandler();
        EntitySetDocHandler targetDocHandler = navigationPropertyContext.getTargetDocHandler();

        // NP経由のリンク情報をコンテキストに設定
        if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.manyToMany) {
            // n:nの場合
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceDocHandler, targetDocHandler);

            // コンテキストにリンク用のdocHandlerを設定.
            navigationPropertyContext.setLinkDocHandler(docHandler);
        } else if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.oneToOne) {
            // 1:1/0..1:1/1:0..1/0..1:0..1 の場合
            String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
            String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
            Map<String, Object> sourceLinks = sourceDocHandler.getManyToOnelinkId();
            Map<String, Object> targetLinks = targetDocHandler.getManyToOnelinkId();

            // link情報を更新する
            sourceLinks.put(toLinksKey, targetDocHandler.getId());
            sourceDocHandler.setManyToOnelinkId(sourceLinks);

            // Aliasをプロパティ名に変換する
            sourceDocHandler.convertAliasToName(getMetadata());

            // NP経由でPropetyを作成した場合、バージョンが更新されるので、バージョンを取得する
            targetLinks.put(fromLinksKey, sourceDocHandler.getId());
            targetDocHandler.setManyToOnelinkId(targetLinks);

            // Aliasをプロパティ名に変換する
            targetDocHandler.convertAliasToName(getMetadata());

            // コンテキストにリンク用のdocHandlerを設定.
            navigationPropertyContext.setSourceDocHandler(sourceDocHandler);
            navigationPropertyContext.setTargetDocHandler(targetDocHandler);
        } else {
            String fromEntitySetName = srcNavProp.getFromRole().getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            if (navigationPropertyContext.getLinkType() == NavigationPropertyLinkType.oneToMany) {
                // 1:n/0..1:nの場合
                String fromLinksKey = getLinkskey(fromEntitySetName);
                Map<String, Object> links = new HashMap<String, Object>();

                // 登録用のlinksオブジェクトを生成して、登録対象のオブジェクトに設定する
                links = targetDocHandler.getManyToOnelinkId();
                links.put(fromLinksKey, sourceDocHandler.getId());
                targetDocHandler.setManyToOnelinkId(links);

                // Aliasをプロパティ名に変換する
                targetDocHandler.convertAliasToName(getMetadata());

                // コンテキストにリンク用のdocHandlerを設定.
                navigationPropertyContext.setTargetDocHandler(targetDocHandler);
            } else {
                // n:1/n:0..1の場合
                String toLinksKey = getLinkskey(toEntitySetName);
                Map<String, Object> links = new HashMap<String, Object>();

                // 登録用のlinksオブジェクトを生成して、登録対象のオブジェクトに設定する
                links = sourceDocHandler.getManyToOnelinkId();
                links.put(toLinksKey, targetDocHandler.getId());
                sourceDocHandler.setManyToOnelinkId(links);

                // Aliasをプロパティ名に変換する
                sourceDocHandler.convertAliasToName(getMetadata());

                // コンテキストにリンク用のdocHandlerを設定.
                navigationPropertyContext.setSourceDocHandler(sourceDocHandler);
            }
        }
    }

    /**
     * NavigationProeprtyのEdm（スキーマ）を取得する.
     * @param entitySetName 取得対象のEntitySet名
     * @param navigationPropertyName 取得対象のNavigationProperty名
     * @return NavigationProeprtyのEdm（スキーマ）
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
        // スキーマチェック
        // srcTypeからtgtTypeへN:N Assocが定義されているか調べる
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(sourceOEntity.getEntitySetName());
        EdmEntityType srcType = srcSet.getType();
        EdmNavigationProperty srcNavProp = srcType.findNavigationProperty(targetNavProp);
        if (srcNavProp == null) {
            throw DcCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        EntityResponse res;
        // ユニーク性チェックのためまずロックを行う
        // OData 空間全体をlockする(将来的に必要があればentitySetNameでロック)
        Lock lock = this.lock();

        try {

            // sourceデータが存在しない場合は404
            EntitySetDocHandler sourceDocHandler = this.retrieveWithKey(sourceOEntity);
            if (sourceDocHandler == null) {
                throw DcCoreException.OData.NOT_FOUND;
            }
            EntitySetDocHandler targetEntity = this.retrieveWithKey(entity);
            if (targetEntity != null) {
                throw DcCoreException.OData.CONFLICT_LINKS;
            }

            // 1:1の関連は存在し得ないので、AssociationEnd - AssociationEndの$linksで1:1の登録をしようとした場合はエラーとする
            checkAssociationEndMultiplicity(targetNavProp, entity, targetEntitySetName, sourceDocHandler);

            checkInvalidLinks(sourceDocHandler, entity, targetEntitySetName);

            // $linksの登録済みチェック
            checkExistsLink(sourceOEntity, srcNavProp, sourceDocHandler, entity);

            // $links上限値チェック
            checkUpperLimitRecord(srcNavProp, sourceDocHandler, targetEntitySetName);

            // targetのEntity作成
            res = createNavigationPropertyEntity(entity, targetEntitySetName);
            if (res == null || res.getEntity() == null) {
                return null;
            }
            EntitySetDocHandler retrievedEntity = this.retrieveWithKey(entity);
            if (retrievedEntity == null) {
                throw DcCoreException.Server.UNKNOWN_ERROR;
            }

            // $linksの登録
            entity = createNavigationPropertyLink(sourceOEntity, entity, srcNavProp, sourceDocHandler, retrievedEntity);
            res = Responses.entity(entity);

        } finally {
            log.debug("unlock");
            lock.release();
        }
        return res;
    }

    /**
     * $linksの登録可能な上限値を超えて登録をしようとしているかをチェックする.
     * @param srcNavProp ソース側のEdmNavigationProperty
     * @param sourceDocHandler ソース側のDocHandler
     * @param targetEntitySetName ターゲット側のEntitySet名
     */
    private void checkUpperLimitRecord(
            EdmNavigationProperty srcNavProp,
            EntitySetDocHandler sourceDocHandler,
            String targetEntitySetName) {
        // 関連がN:N以外の場合は、上限値制限なし
        if (!isAssociationOfNToN(srcNavProp.getRelationship())) {
            return;
        }

        // ユーザデータの$links取得の場合、targetのEntityTypeの_idを取得する
        String targetEntityTypeId = null;
        if (sourceDocHandler.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetEntitySetName);
        }

        // 登録済み$links
        long count = LinkDocHandler.getNtoNCount(this.getAccessorForLink(), sourceDocHandler, targetEntitySetName,
                targetEntityTypeId);
        log.info("Registered links count: [" + count + "]");

        if (count >= (long) DcCoreConfig.getLinksNtoNMaxSize()) {
            throw DcCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED;
        }
    }

    /**
     * 引数で指定された関連がN:Nかどうかを判定する.
     * @param assoc EdmAssociation
     * @return true: N:N, false: N:N以外
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
        // n:1かn:nの切り分けを行う
        EdmAssociation assoc = srcNavProp.getRelationship();
        EdmMultiplicity multiplicity1 = assoc.getEnd1().getMultiplicity();
        EdmMultiplicity multiplicity2 = assoc.getEnd2().getMultiplicity();

        if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            // 1:1/0..1:1/1:0..1/0..1:0..1 の場合
            checkExistsLinkForOnetoOne(sourceDocHandler, null, srcNavProp);

        } else if (!(multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY)) {
            String multiplicityOneEntitySetName = getOneAssociationEnd(assoc).getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            EntitySetDocHandler targetDocHandler = new OEntityDocHandler();
            // 別Typeに同じ名前の要素がある場合を考慮して、Typeを検索条件に追加
            targetDocHandler.setType(toEntitySetName);
            if (sourceOEntity.getEntitySetName().equals(multiplicityOneEntitySetName)) {
                // 1:Nの場合

                // ユーザODataの場合は、Nameプロパティが存在しない、かつ、リンク登録後に同一データが存在するかの一意性チェックは行わないため、Nameプロパティはセットしない
                if (!(this instanceof UserDataODataProducer)) {
                    Map<String, Object> staticFields = new HashMap<String, Object>();
                    if (ExtRole.EDM_TYPE_NAME.equals(toEntitySetName)) {
                        // ExtRoleはNameプロパティを持たないため、"ExtRole"で区別する
                        staticFields.put(ExtRole.EDM_TYPE_NAME,
                                targetrEntity.getProperty(ExtRole.EDM_TYPE_NAME).getValue());
                    } else if (ReceivedMessage.EDM_TYPE_NAME.equals(toEntitySetName)
                            || SentMessage.EDM_TYPE_NAME.equals(toEntitySetName)) {
                        staticFields.put("__id", targetrEntity.getProperty("__id").getValue());
                    } else {
                        staticFields.put("Name", targetrEntity.getProperty("Name").getValue());
                    }
                    targetDocHandler.setStaticFields(staticFields);
                }
                checkExistsLinksForOneToN(sourceOEntity, sourceDocHandler, targetDocHandler,
                        multiplicityOneEntitySetName, toEntitySetName);
            } else {
                // N:1の場合
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
                throw DcCoreException.OData.INVALID_MULTIPLICITY;
            }
        }
    }

    /**
     * 一括登録用のコンテキストにリンクタイプをセットする.
     * @param navigationPropertyContext 一括登録のコンテキスト
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
            // n:nの場合
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceDocHandler, retrievedEntity);

            ODataLinkAccessor linkAccessor = this.getAccessorForLink();
            createLinkForNtoN(linkAccessor, docHandler);
        } else if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            // 1:1/0..1:1/1:0..1/0..1:0..1 の場合
            long version = createLinkForOnetoOne(sourceDocHandler, retrievedEntity, srcNavProp);
            setETagVersion((OEntityWrapper) entity, version);
        } else {

            String multiplicityOneEntitySetName = getOneAssociationEnd(assoc).getType().getName();
            String fromEntitySetName = srcNavProp.getFromRole().getType().getName();
            String toEntitySetName = srcNavProp.getToRole().getType().getName();
            if (sourceOEntity.getEntitySetName().equals(multiplicityOneEntitySetName)) {
                // 1:n/0..1:nの場合
                String fromLinksKey = getLinkskey(fromEntitySetName);
                long version = createLinkForNtoOne(sourceDocHandler, retrievedEntity, fromLinksKey,
                        toEntitySetName);
                if (ODataProducerUtils.isParentEntity(sourceOEntity, multiplicityOneEntitySetName)) {
                    setETagVersion((OEntityWrapper) entity, version);
                }

            } else {
                // n:1/n:0..1の場合
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
        // バージョンが更新された場合、etag情報を更新する
        esType = this.getAccessorForEntitySet(entity.getEntitySetName());
        oedh = getDocHanlder(esType.getType(), entity);
        oedh.setVersion(version);
        etag = oedh.createEtag();
        entity.setEtag(etag);
    }

    private EntityResponse createNavigationPropertyEntity(OEntity entity, String entitySetName) {
        OEntityKey entityKey = entity.getEntityKey();
        // リクエストのOEntityからelasticsearchに登録する形式のJSONObjectに変換する
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EntityResponse res = createEntity(entitySetName, entity, entityKey, esType, (OEntityWrapper) entity);
        return res;
    }

    /**
     * N:NのLinksを生成.
     * @param sourceEntity リクエストURLにて指定されたEntity
     * @param targetEntity リクエストBODYにて指定されたEntity
     */
    protected void createLinks(OEntityId sourceEntity, OEntityId targetEntity) {
        EntitySetDocHandler src = this.retrieveWithKey(sourceEntity);
        // データが存在しない場合は404
        if (src == null) {
            throw DcCoreException.OData.NOT_FOUND;
        }
        EntitySetDocHandler tgt = this.retrieveWithKey(targetEntity);
        // ターゲットが存在しない場合は400
        if (tgt == null) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }
        // NNLink情報の ES保存時の一意キー作成
        LinkDocHandler docHandler = this.getLinkDocHandler(src, tgt);
        // LINKを扱うアクセサを取る
        ODataLinkAccessor linkAccessor = this.getAccessorForLink();

        // $linksの登録
        checkExistsLinkForNtoN(linkAccessor, docHandler);

        String sourceEntitySetName = sourceEntity.getEntitySetName();
        String targetEntitySetName = targetEntity.getEntitySetName();
        EdmNavigationProperty srcNavProp = getEdmNavigationProperty(sourceEntitySetName, "_" + targetEntitySetName);
        // $links上限値チェック
        // リンク元
        checkUpperLimitRecord(srcNavProp, src, targetEntitySetName);
        // リンク先
        checkUpperLimitRecord(srcNavProp, tgt, sourceEntitySetName);

        createLinkForNtoN(linkAccessor, docHandler);
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
            throw DcCoreException.OData.INVALID_MULTIPLICITY;
        }

        checkInvalidLinks(sourceEntity, targetEntity);

        if (multiplicity1 == EdmMultiplicity.MANY && multiplicity2 == EdmMultiplicity.MANY) {
            // n:nの場合
            // LINKを扱うアクセサを取る
            ODataLinkAccessor esType = this.getAccessorForLink();
            // NNLink情報の ES保存時の一意キー作成
            LinkDocHandler docHandler = this.getLinkDocHandler(sourceEntity, targetEntity);

            // $linksの一意性チェック
            checkExistsLinkForNtoN(esType, docHandler);

            // $links上限値チェック
            // リンク元
            String targetEntitySetName = srcNavProp.getToRole().getType().getName();
            checkUpperLimitRecord(srcNavProp, sourceEntity, targetEntitySetName);
            // リンク先
            String sourceEntitySetName = srcNavProp.getFromRole().getType().getName();
            checkUpperLimitRecord(srcNavProp, targetEntity, sourceEntitySetName);

            createLinkForNtoN(esType, docHandler);
        } else if ((multiplicity1 == EdmMultiplicity.ONE || multiplicity1 == EdmMultiplicity.ZERO_TO_ONE)
                && (multiplicity2 == EdmMultiplicity.ONE || multiplicity2 == EdmMultiplicity.ZERO_TO_ONE)) {
            // 1:1の場合
            checkExistsLinkForOnetoOne(sourceEntity, targetEntity, srcNavProp);
            version = createLinkForOnetoOne(sourceEntity, targetEntity, srcNavProp);
        } else {
            // n:1の1のEdmAssociationEndを取得する
            EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
            // n:1の場合、nのデータに1のIDをリンク情報をとして追加する
            // ESから1:NそれぞれのAssocのEntityを取得する
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
     * 不正なLink情報のチェックを行う.
     * @param sourceEntity ソース側Entity
     * @param targetEntity ターゲット側Entity
     */
    protected abstract void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity);

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceDocHandler ソース側Entity
     * @param entity ターゲット側Entity
     * @param targetEntitySetName ターゲットのEntitySet名
     */
    protected abstract void checkInvalidLinks(EntitySetDocHandler sourceDocHandler,
            OEntity entity,
            String targetEntitySetName);

    private void checkExistsLinkForOnetoOne(final EntitySetDocHandler source,
            final EntitySetDocHandler target,
            EdmNavigationProperty srcNavProp) {
        // 既に同一NavigationPropetiesにlinksが登録されている場合は409とする
        String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
        String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
        Map<String, Object> sourceLinks = source.getManyToOnelinkId();
        if (sourceLinks.get(toLinksKey) != null) {
            throw DcCoreException.OData.CONFLICT_LINKS;
        } else if (target != null && target.getManyToOnelinkId().get(fromLinksKey) != null) {
            throw DcCoreException.OData.CONFLICT_LINKS;
        }
    }

    /**
     * 1:1のlinksを生成.
     * @param sourceEntity リクエストURLにて指定されたEntity
     * @param targetEntity リクエストBODYにて指定されたEntity
     * @param srcNavProp ナビゲーションプロパティ
     * @return バージョン情報
     */
    private long createLinkForOnetoOne(final EntitySetDocHandler source,
            final EntitySetDocHandler target,
            EdmNavigationProperty srcNavProp) {
        String toLinksKey = getLinkskey(srcNavProp.getToRole().getType().getName());
        String fromLinksKey = getLinkskey(srcNavProp.getFromRole().getType().getName());
        Map<String, Object> sourceLinks = source.getManyToOnelinkId();
        Map<String, Object> targetLinks = target.getManyToOnelinkId();

        // link情報を更新する
        sourceLinks.put(toLinksKey, target.getId());
        source.setManyToOnelinkId(sourceLinks);

        // Aliasをプロパティ名に変換する
        source.convertAliasToName(getMetadata());
        updateLink(source, fromLinksKey);

        // NP経由でPropetyを作成した場合、バージョンが更新されるので、バージョンを取得する
        targetLinks.put(fromLinksKey, source.getId());
        target.setManyToOnelinkId(targetLinks);

        // Aliasをプロパティ名に変換する
        target.convertAliasToName(getMetadata());

        long version = updateLink(target, toLinksKey);
        return version;
    }

    /**
     * link情報を更新する.
     * @param docHandler 更新対象のEntitySetDocHandler
     * @param entSetName 更新対象のEntitySet名
     * @return バージョン
     */
    private long updateLink(EntitySetDocHandler docHandler, String entSetName) {
        // アクセサを取る
        EntitySetAccessor esType = this.getAccessorForEntitySet(entSetName);
        return esType.update(docHandler.getId(), docHandler).getVersion();
    }

    private void checkExistsLinkForNtoN(ODataLinkAccessor esType, LinkDocHandler docHandler) {
        String docid = docHandler.createLinkId();
        // Link の存在確認
        DcGetResponse gRes = esType.get(docid);
        if (gRes != null && gRes.exists()) {
            // 既に該当LINKが存在する
            throw DcCoreException.OData.CONFLICT_LINKS;
        }
    }

    private void createLinkForNtoN(ODataLinkAccessor esType, LinkDocHandler docHandler) {
        String docid = docHandler.createLinkId();
        // NNLink情報の couch保存時のJSONドキュメント作成
        esType.create(docid, docHandler);
    }

    private void checkExistsLinksForNtoOne(
            EntitySetDocHandler sourceEntity,
            String targetEntityTypeName) {
        String linksKey = getLinkskey(targetEntityTypeName);

        // ソース側（multiplicity *）が既にターゲットEntityTYpe（multiplicity 1）にlinksが登録されている場合は409とする
        if (sourceEntity != null) {
            Map<String, Object> links = sourceEntity.getManyToOnelinkId();
            if (links != null && links.get(linksKey) != null) {
                throw DcCoreException.OData.CONFLICT_LINKS;
            }
        }
    }

    private void checkExistsLinksForOneToN(OEntityId sourceOEntity,
            EntitySetDocHandler sourceEntity,
            EntitySetDocHandler targetEntity,
            String sourceEntityTypeName,
            String targetEntitySetName) {
        String linksKey = getLinkskey(sourceEntityTypeName);

        // ターゲット側のmultiplicityが*で既に同一NavigationPropetiesにlinksが登録されている場合は409とする
        if (targetEntity != null) {
            Map<String, Object> links = targetEntity.getManyToOnelinkId();
            if (links != null && links.get(linksKey) != null) {
                throw DcCoreException.OData.CONFLICT_LINKS;
            }
        }

        // 単一キーのEntityに対して$linksを登録した際に、同名のキーが存在する場合は409
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
            throw DcCoreException.OData.CONFLICT_DUPLICATED_ENTITY.params(param);
        }
    }

    /**
     * N:1のLinksを生成.
     * @param targetEntity リクエストBODYにて指定されたEntity
     * @param oneAssoc N:1の1のAssociation情報
     * @param srcNavProp NavigationProperty
     * @return バージョン
     */
    private long createLinkForNtoOne(EntitySetDocHandler sourceEntity,
            EntitySetDocHandler targetEntity,
            String fromLinksKey,
            String targetEntitySetName) {
        // 登録用のlinksオブジェクトを生成して、登録対象のオブジェクトに設定する
        Map<String, Object> links = targetEntity.getManyToOnelinkId();
        links.put(fromLinksKey, sourceEntity.getId());
        targetEntity.setManyToOnelinkId(links);

        // Aliasをプロパティ名に変換する
        targetEntity.convertAliasToName(getMetadata());
        long version = updateLink(targetEntity, targetEntitySetName);
        return version;
    }

    /**
     * 1:Nの1のEdmAssociationEndを取得する.
     * @param assoc Association情報
     * @return 1:Nの1のEdmAssociationEnd
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

        // スキーマチェック
        // srcTypeからtgtTypeへN:N Assocが定義されているか調べる
        EdmEntitySet srcSet = this.getMetadata().findEdmEntitySet(srcSetName);
        EdmEntityType srcType = srcSet.getType();

        EdmNavigationProperty navProp = srcType.findNavigationProperty(targetNavProp);
        if (navProp == null) {
            // TODO 本来はリクエストされたリソースが存在しないことになるため404エラーを返却すべき
            throw DcCoreException.OData.NO_SUCH_ASSOCIATION;
        }
        EdmEntitySet tgtSet = this.getMetadata().findEdmEntitySet(navProp.getToRole().getType().getName());

        // n:1かn:nの切り分けを行う
        EdmAssociation assoc = navProp.getRelationship();

        // OData 空間全体をlockする
        Lock lock = this.lock();
        try {

            if (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.MANY
                    && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.MANY) {
                // n:nの場合
                deleteLinks(sourceEntityId, targetEntityKey, tgtSet);
            } else if (assoc.getEnd1().getMultiplicity() != EdmMultiplicity.MANY
                    && assoc.getEnd2().getMultiplicity() != EdmMultiplicity.MANY) {
                // [0..1:0..1] or [0..1:1] or [1:0..1] or [1:1]の場合は相互リンクのため、両方のデータからリンク情報を削除する
                // リンク先・リンク元のデータを取得する
                EntitySetDocHandler source = this.retrieveWithKey(sourceEntityId);
                EntitySetDocHandler target = this.retrieveWithKey(tgtSet, targetEntityKey);
                // 該当データが存在しない場合は404
                if (source == null || target == null) {
                    throw DcCoreException.OData.NOT_FOUND;
                }

                // 取得したデータ同士の関連付けをチェックし、関連付いていないリンクを削除しようとした場合は400を返却する
                isExistsLinks(source, target, tgtSet);
                isExistsLinks(target, source, srcSet);

                // Aliasをプロパティ名に変換する
                source.convertAliasToName(getMetadata());
                target.convertAliasToName(getMetadata());

                // 両方のリンクを削除
                String sourceEntitySetName = srcSet.getName();
                String targetEntitySetName = tgtSet.getName();
                linkUpdate(source, sourceEntitySetName, targetEntitySetName);
                linkUpdate(target, targetEntitySetName, sourceEntitySetName);
            } else {
                // n:1の1のEdmAssociationEndを取得する
                EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
                // n:1の場合、1のデータのリンク情報から該当する項目を削除する
                deleteLinks(sourceEntityId, targetEntityKey, tgtSet, oneAssoc);
            }
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * 取得したデータ同士の関連付けをチェックし、関連付いていないリンクを削除しようとした場合は400を返却する.
     * @param source ESから取得したチェック対象のEntitySetのデータ
     * @param source ESから取得したリンク先のEntitySetのデータ
     * @param entitySet EntitySet
     */
    private void isExistsLinks(EntitySetDocHandler source, EntitySetDocHandler target, EdmEntitySet entitySet) {
        Map<String, Object> links = source.getManyToOnelinkId();
        String linksKey = getLinkskey(entitySet.getName());
        if (!links.containsKey(linksKey) || !target.getId().equals(links.get(linksKey))) {
            throw DcCoreException.OData.NOT_FOUND;
        }
    }

    /**
     * N:Nのリンク情報を削除する.
     * @param sourceEntityId リクエストURLにて指定されたEntity
     * @param targetEntityKey リクエストBODYにて指定されたEntity
     * @param tgtSet リクエストBODYにて指定されたEntityKey
     */
    private void deleteLinks(OEntityId sourceEntityId, OEntityKey targetEntityKey, EdmEntitySet tgtSet) {
        // 両者のidを取得する
        EntitySetDocHandler src = this.retrieveWithKey(sourceEntityId);
        EntitySetDocHandler tgt = this.retrieveWithKey(tgtSet, targetEntityKey);

        // 該当データが存在しない場合は404
        if (src == null || tgt == null) {
            throw DcCoreException.OData.NOT_FOUND;
        }

        // リンクエンティティを削除する
        if (!deleteLinkEntity(src, tgt)) {
            // 該当LINKが存在しない場合はエラーにする
            throw DcCoreException.OData.NOT_FOUND;
        }
    }

    /**
     * N:Nのリンクエンティティを削除する.
     * @param source リンク元エンティティ
     * @param target リンク先エンティティ
     * @return 削除正常時はtrue データが存在しない場合はfalseを返却
     */
    protected boolean deleteLinkEntity(EntitySetDocHandler source, EntitySetDocHandler target) {
        // 削除すべきEsドキュメントを特定
        ODataLinkAccessor esType = this.getAccessorForLink();
        LinkDocHandler elh = this.getLinkDocHandler(source, target);
        String docid = elh.createLinkId();

        // Link の存在確認
        DcGetResponse gRes = esType.get(docid);
        if (gRes != null && gRes.exists()) {
            esType.delete(elh);
            return true;
        }
        return false;
    }

    private void linkUpdate(EntitySetDocHandler tgt, String sourceEntitySetName, String unlinkEntitySetName) {
        Map<String, Object> links = tgt.getManyToOnelinkId();

        // 登録用のlinksオブジェクトを生成して、登録対象のオブジェクトに設定する
        links.remove(getLinkskey(unlinkEntitySetName));
        tgt.setManyToOnelinkId(links);

        // アクセサを取る
        EntitySetAccessor esType = this.getAccessorForEntitySet(sourceEntitySetName);
        esType.update(tgt.getId(), tgt);
    }

    /**
     * N:1のリンク情報を削除する.
     * @param sourceEntityId リクエストURLにて指定されたEntity
     * @param targetEntityKey リクエストBODYにて指定されたEntity
     * @param tgtSet リクエストBODYにて指定されたEntityKey
     * @param oneAssoc 1:Nの1のAssociation情報
     */
    private void deleteLinks(OEntityId sourceEntityId,
            OEntityKey targetEntityKey,
            EdmEntitySet tgtSet,
            EdmAssociationEnd oneAssoc) {
        // ESから1:NそれぞれのAssocのEntityを取得する
        EntitySetDocHandler src;
        EntitySetDocHandler tgt;
        String linksKey = getLinkskey(oneAssoc.getType().getName());
        String sourceEntitySetName = null;
        // 両者のidを取得する
        if (ODataProducerUtils.isParentEntity(sourceEntityId, oneAssoc.getType().getName())) {
            src = this.retrieveWithKey(sourceEntityId);
            tgt = this.retrieveWithKey(tgtSet, targetEntityKey);
            sourceEntitySetName = tgtSet.getName();
        } else {
            src = this.retrieveWithKey(tgtSet, targetEntityKey);
            tgt = this.retrieveWithKey(sourceEntityId);
            sourceEntitySetName = sourceEntityId.getEntitySetName();
        }

        // 該当データが存在しない場合は404
        if (src == null || tgt == null) {
            throw DcCoreException.OData.NOT_FOUND;
        }

        // 削除対象のlinksが存在しない場合は404とする
        Map<String, Object> links = tgt.getManyToOnelinkId();
        if (!src.getId().equals(links.get(linksKey))) {
            throw DcCoreException.OData.NOT_FOUND;
        }

        // 登録用のlinksオブジェクトを生成して、登録対象のオブジェクトに設定する
        links.remove(linksKey);
        tgt.setManyToOnelinkId(links);

        // Aliasをプロパティ名に変換する
        src.convertAliasToName(getMetadata());
        tgt.convertAliasToName(getMetadata());

        // アクセサを取る
        EntitySetAccessor esType = this.getAccessorForEntitySet(sourceEntitySetName);

        // 複合キーのEntityに対して$linksを削除した際に、同名の単一キーが存在する場合は409
        boolean uniqueness = checkUniquenessEntityKey(sourceEntitySetName, tgt, linksKey, esType);
        if (!uniqueness) {
            String param = sourceEntitySetName + "('" + tgt.getStaticFields().get("Name") + "')";
            throw DcCoreException.OData.CONFLICT_UNLINKED_ENTITY.params(param);
        }

        esType.update(tgt.getId(), tgt);
    }

    /**
     * 一意性チェックルーチン.
     * @param entitySetName エンティティセット名
     * @param tgt 更新対象のEntity
     * @param esType 検索対象のESAccessor
     * @param termQuery リンク検索クエリ
     * @return Esの検索結果
     */
    protected long checkUniquenessEntityCount(final String entitySetName,
            final EntitySetDocHandler tgt,
            final EntitySetAccessor esType, Map<String, Object> termQuery) {
        // 単一キーとなった同名のエンティティが存在するかどうかをチェック
        // Staticフィールドの検索クエリを組み立てる
        List<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        if (null != tgt && null != tgt.getStaticFields() && !tgt.getStaticFields().isEmpty()) {
            if (tgt.getStaticFields().get("__id") != null) {
                terms.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id"
                        + ".untouched", (String) tgt.getStaticFields().get("__id"), false));
            } else if (tgt.getStaticFields().get("Name") != null) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + ".Name"
                        + ".untouched", (String) tgt.getStaticFields().get("Name")));
                // 別Typeに同じ名前の要素がある場合を考慮して、Typeを検索条件に追加
                terms.add(QueryMapFactory.termQuery("_type", (String) tgt.getType()));
            } else if (tgt.getStaticFields().get(ExtRole.EDM_TYPE_NAME) != null) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + "." + ExtRole.EDM_TYPE_NAME
                        + ".untouched", (String) tgt.getStaticFields().get(ExtRole.EDM_TYPE_NAME)));
                // 別Typeに同じ名前の要素がある場合を考慮して、Typeを検索条件に追加
                terms.add(QueryMapFactory.termQuery("_type", (String) tgt.getType()));
            }
        }

        // リンクのNull検索クエリを組み立てる
        terms.add(termQuery);

        Map<String, Object> query = QueryMapFactory.filteredQuery(null,
                QueryMapFactory.mustQuery(getImplicitFilters(entitySetName)));

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("version", true);
        filter.put("filter", QueryMapFactory.andFilter(terms));

        filter.put("query", query);

        // 検索の実行
        return esType.count(filter);
    }

    /**
     * リンク削除時の一意性チェック.
     * @param entitySetName エンティティセット名
     * @param tgt 更新対象のEntity
     * @param linksKey リンク先のEntittyType名
     * @param esType 検索対象のESAccessor
     * @return true 一意性が保たれる | false 一意性が保たれない
     */
    protected boolean checkUniquenessEntityKey(final String entitySetName, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {

        // Linkフィールドの検索クエリを組み立てる
        String linkKey = OEntityDocHandler.KEY_LINK + "." + linksKey;
        // リンクのNull検索クエリを組み立てる
        long count = checkUniquenessEntityCount(entitySetName, tgt, esType, QueryMapFactory.missingFilter(linkKey));
        if (count != 0) {
            return false;
        }
        return true;
    }

    /**
     * リンク登録時の一意性チェック.
     * @param entitySetName エンティティセット名
     * @param src リンク元のEntity
     * @param tgt 更新対象のEntity
     * @param linksKey リンク先のEntittyType名
     * @param esType 検索対象のESAccessor
     * @return true 一意性が保たれる | false 一意性が保たれない
     */
    protected boolean checkUniquenessEntityKeyForAddLink(final String entitySetName,
            final EntitySetDocHandler src, final EntitySetDocHandler tgt,
            final String linksKey, final EntitySetAccessor esType) {
        // 複合キーとなった同名のエンティティが存在するかどうかをチェック

        // Linkフィールドの検索クエリを組み立てる
        String linkKey = OEntityDocHandler.KEY_LINK + "." + linksKey;
        long count = checkUniquenessEntityCount(entitySetName,
                tgt, esType, QueryMapFactory.termQuery(linkKey, src.getId()));
        if (count != 0) {
            return false;
        }
        return true;
    }

    /** N:1の最大リンク取得件数. */
    private static final int DEFAULT_TOP_VALUE = DcCoreConfig.getTopQueryDefaultSize();

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
            throw DcCoreException.OData.NO_SUCH_ENTITY;
        }

        // srcTypeからtgtTypeへN:N Assocが定義されているか調べる
        String srcSetName = sourceEntity.getEntitySetName();
        EdmEntityType srcType = this.getMetadata().findEdmEntitySet(srcSetName).getType();

        EdmNavigationProperty navProp = srcType.findNavigationProperty(targetNavProp);
        if (navProp == null) {
            throw DcCoreException.OData.NO_SUCH_ASSOCIATION;
        }

        // n:1かn:nの切り分けを行う
        EdmAssociation assoc = navProp.getRelationship();

        String targetSetName = navProp.getToRole().getType().getName();
        List<OEntityId> oeids = new ArrayList<OEntityId>();
        EdmEntitySet tgtSet = this.getMetadata().findEdmEntitySet(targetSetName);

        // ユーザデータの$links取得の場合、targetのEntityTypeの_idを取得する
        String targetEntityTypeId = null;
        if (src.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetSetName);
        }

        if (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.MANY
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.MANY) {
            // N:Nのlinkの登録数の上限値1万に合わせて、最大1万件を取得する
            EntitySetAccessor tgtEsType = this.getAccessorForEntitySet(targetSetName);
            QueryInfo qi = QueryInfo.newBuilder().setTop(DcCoreConfig.getTopQueryMaxSize())
                    .setInlineCount(InlineCount.NONE).build();
            List<String> idvals = LinkDocHandler.query(this.getAccessorForLink(),
                    src, tgtEsType.getType(), targetEntityTypeId, qi);

            DcSearchHits sHits = ODataProducerUtils.searchLinksNN(src, targetSetName, idvals, tgtEsType, queryInfo);
            oeids = getOEntityIds(sHits, targetSetName, tgtSet);

        } else if ((assoc.getEnd1().getMultiplicity() == EdmMultiplicity.ZERO_TO_ONE
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.ZERO_TO_ONE)
                || (assoc.getEnd1().getMultiplicity() == EdmMultiplicity.ONE
                && assoc.getEnd2().getMultiplicity() == EdmMultiplicity.ONE)) {
            // 片方のEdmAssociationEndを取得する
            oeids = getOEntityIds(src, targetSetName, tgtSet);

        } else {
            // n:1の1のEdmAssociationEndを取得する
            EdmAssociationEnd oneAssoc = getOneAssociationEnd(assoc);
            String linksKey = this.getLinkskey(oneAssoc.getType().getName());

            if (ODataProducerUtils.isParentEntity(sourceEntity, oneAssoc.getType().getName())) {
                // SOURCEが1の場合、NavPropで指定されたタイプのデータの{"l":{"entitySet":UUID}を検索する
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
                // EntityType / Node / Box / Cell の順で暗黙フィルタを指定
                // リンク先情報をフィルターの先頭に指定することで絞り込みする。
                List<Map<String, Object>> implicitFilters = getImplicitFilters(targetSetName);
                implicitFilters.add(0, getLinkFieldsQuery(linksKey, src.getId()));
                Map<String, Object> query = QueryMapFactory.mustQuery(implicitFilters);
                Map<String, Object> filteredQuery = QueryMapFactory.filteredQuery(null, query);
                // ソート条件を指定（セル制御オブジェクト：Name、UserOData：__idを使用）
                // TODO セル制御オブジェクトにはNameプロパティをもたないものがあり、この場合ソート順が不定になる。
                List<Map<String, Object>> sort = new ArrayList<Map<String, Object>>();
                sort.add(QueryMapFactory.sortQuery("s.Name.untouched", "asc"));
                sort.add(QueryMapFactory.sortQuery("s.__id.untouched", "asc"));
                // クエリ情報の集約
                Map<String, Object> filter = new HashMap<String, Object>();
                filter.put("size", size);
                filter.put("from", from);
                filter.put("query", filteredQuery);
                filter.put("sort", sort);

                // 検索の実行
                EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);
                DcSearchHits sHits = esType.search(filter).hits();
                oeids = getOEntityIds(sHits, targetSetName, tgtSet);
            } else {
                // SOURCEがNの場合
                oeids = getOEntityIds(src, targetSetName, tgtSet);
            }
        }
        EntityIdResponse resp = Responses.multipleIds(oeids);
        return resp;
    }

    /**
     * 検索結果からOEntityIdのListを生成して、取得する.
     * @param sHits Linksの検索結果
     * @param targetSetName リクエストURLにて指定されたNavPropのEntitySet名
     * @param tgtSet リクエストURLにて指定されたNavPropのEdmEntitySet
     * @return OEntityIdの一覧
     */
    private List<OEntityId> getOEntityIds(DcSearchHits sHits, String targetSetName, EdmEntitySet tgtSet) {
        // 検索結果からOEntityIdのListを生成する
        List<OEntityId> oeids = new ArrayList<OEntityId>();
        if (sHits == null) {
            return oeids;
        }
        for (DcSearchHit hit : sHits.getHits()) {
            EntitySetDocHandler oedh = getDocHandler(hit, targetSetName);
            OEntityId id = getOEntityId(targetSetName, tgtSet, oedh);
            oeids.add(id);
        }
        return oeids;
    }

    /**
     * N:1のNのデータからOEntityIdを生成して、取得する.
     * @param src リクエストURLにて指定されたEntity
     * @param targetSetName リクエストURLにて指定されたNavPropのEntitySet名
     * @param tgtSet targetEdmEntitySet
     * @return OEntityIdの一覧
     */
    private List<OEntityId> getOEntityIds(EntitySetDocHandler src, String targetSetName, EdmEntitySet tgtSet) {
        List<OEntityId> oeids = new ArrayList<OEntityId>();

        EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);
        String linksId = (String) src.getManyToOnelinkId().get(getLinkskey(targetSetName));

        if (linksId != null) {
            DcGetResponse response = esType.get(linksId);
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
     * NavigationProperty経由の一覧取得を実行する.
     * クエリ情報で指定がない場合はデフォルト件数(最大25件を返却する)
     * @param entitySetName エンティティセット名
     * @param entityKey エンティティキー
     * @param navPropStr ナビゲーションプロパティ
     * @param queryInfo クエリ情報
     * @return 検索結果
     */
    @Override
    public BaseResponse getNavProperty(final String entitySetName,
            final OEntityKey entityKey,
            final String navPropStr,
            final QueryInfo queryInfo) {
        // 注）起点EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
        // 注）不正なNavigationPropertiy指定も予め呼び出し側で確認・排除されている前提。

        // Src側Entityの取得
        EdmEntitySet sourceSet = this.getMetadata().findEdmEntitySet(entitySetName);

        // TargetのEdmEntitySetを取得
        EdmNavigationProperty navProp = sourceSet.getType().findNavigationProperty(navPropStr);
        String targetSetName = navProp.getToRole().getType().getName();
        EdmEntitySet targetSet = this.getMetadata().findEdmEntitySet(targetSetName);

        // Targetのアクセサを取得
        EntitySetAccessor esType = this.getAccessorForEntitySet(targetSetName);

        // EntitySetの取得を行う
        EntitySetDocHandler source = this.retrieveWithKey(sourceSet, entityKey);
        if (source == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY;
        }

        // ユーザデータの$links取得の場合、targetのEntityTypeの_idを取得する
        String targetEntityTypeId = null;
        if (source.getType().equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
            targetEntityTypeId = getEntityTypeId(targetSetName);
        }

        // 次にMultiplicityを確認する。
        int cardinality = ODataUtils.Cardinality.forEdmNavigationProperty(navProp);
        Map<String, Object> linkQuery;

        // Cardinalityパターンに応じて、Es検索クエリを作る。
        if (ODataUtils.Cardinality.MANY_MANY == cardinality) {
            // N:Nの場合はType[Link]を検索して、NavigationPropertyのID一覧を取得する
            Map<String, Object> idsQuery = new HashMap<String, Object>();
            // N:Nのlinkの登録数の上限値1万に合わせて、最大1万件を取得する
            QueryInfo qi = QueryInfo.newBuilder().setTop(DcCoreConfig.getTopQueryMaxSize())
                    .setInlineCount(InlineCount.NONE).build();

            List<String> value = LinkDocHandler.query(this.getAccessorForLink(),
                    source, esType.getType(), targetEntityTypeId, qi);

            // idが空の場合は空の検索結果を返却する
            if (value.isEmpty()) {
                return emptyResult(queryInfo, targetSet);
            }

            // 取得したID一覧を検索条件として設定する
            linkQuery = new HashMap<String, Object>();

            idsQuery.put("values", value);
            linkQuery.put("ids", idsQuery);
        } else if (ODataUtils.Cardinality.ONE_MANY == cardinality) {
            // 1:Nの場合はリンク情報の検索条件を設定する
            linkQuery = getLinkFieldsQuery(getLinkskey(entitySetName), source.getId());

        } else {
            // N:1,1:1なら検索条件にNavigationPropertyのIDを設定する
            // { "ids" : { "values" : ["リンク元EntityTypeの内部ID"] } }
            String linkId = (String) source.getManyToOnelinkId().get(getLinkskey(targetSetName));
            // リンクが設定されていない場合は、空の検索結果を返却する
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

        // Cell / Box / Node / EntityTypeに基づいた暗黙フィルタの作成
        List<Map<String, Object>> implicitFilters = getImplicitFilters(targetSetName);

        // Link情報の検索条件を暗黙フィルタに追加
        implicitFilters.add(linkQuery);

        // implicitFIltersを渡して、検索を実行する
        return execEntitiesRequest(queryInfo, targetSet, esType, implicitFilters);
    }

    /**
     * リンクフィールドの検索クエリを取得する.
     * @param entitySet エンティティセット名
     * @param id リンクエンティティのID
     * @return 検索クエリ
     */
    public Map<String, Object> getLinkFieldsQuery(String entitySet, String id) {
        // { "term" : { "l.リンク元EntityType名.untouched" : "リンク元EntityTypeの内部ID" }}
        String linkKey = OEntityDocHandler.KEY_LINK + "." + entitySet;
        return QueryMapFactory.termQuery(linkKey, id);
    }

    /**
     * 空の検索結果を返却する.
     * @param queryInfo 検索条件
     * @param targetSet 対象のEntitySet
     * @return BaseResponse レスポンス
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
        // OData4jのODataProducerのこのメソッドは、構造的に主キー変更に耐えられないという欠陥があるため
        // 本アプリでは使わない。
        throw new RuntimeException("Bug! Do not call this method. ");
    }

    @Override
    public void mergeEntity(final String entitySetName,
            final OEntityKey originalKey,
            final OEntityWrapper oEntityWrapper) {

        // ロック取得
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
        // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。

        // ロック取得
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
        // OData4jのODataProducerのこのメソッドは、構造的に主キー変更に耐えられないという欠陥があるため本アプリでは使わない。
        throw new RuntimeException("Bug! Do not call this method. ");
    }

    private void updateAndMergeEntity(final String entitySetName,
            final OEntityKey originalKey,
            final OEntityWrapper oEntityWrapper,
            boolean isMergeMode) {
        // まずは存在確認をする。存在しないときはNullが返ってくる。
        EntitySetDocHandler oedhExisting = this.retrieveWithKey(oEntityWrapper.getEntitySet(), originalKey);
        if (oedhExisting == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY;
        }

        // If-MatchヘッダとEtagの値が等しいかチェック
        ODataUtils.checkEtag(oEntityWrapper.getEtag(), oedhExisting);

        // 呼び出し元がUUIDを取得できるよう引数のoEntityWrapperに破壊的にUUIDを詰めてあげる。
        oEntityWrapper.setUuid(oedhExisting.getId());
        EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
        EntitySetDocHandler oedhNew = getUpdateDocHanlder(esType.getType(), oEntityWrapper);

        // 変更後データの一意性チェックを行う。
        ODataProducerUtils.checkUniqueness(this, oEntityWrapper,
                oedhExisting.createOEntity(oEntityWrapper.getEntitySet(), this.getMetadata(), null), originalKey);

        // Cell, Box, Node, EntityTypeの紐付
        oedhNew.setCellId(this.getCellId());
        oedhNew.setBoxId(this.getBoxId());
        oedhNew.setNodeId(this.getNodeId());
        oedhNew.setEntityTypeId(this.getEntityTypeId(entitySetName));
        oedhNew.setManyToOnelinkId(oedhExisting.getManyToOnelinkId());

        // 更新前のリンク情報を保管しておく
        // oedhExistingのリンク情報のMapオブジェクトとoedhNewのリンク情報のMapオブジェクトは同じものを使用している。
        // これにより、setLinksFromOEntity()でoedhNewのリンク情報を更新するとoedhExistingのリンク情報も更新されてしまう。
        // このため、更新前のリンク情報を保持しておく必要があった。
        Map<String, Object> originalManeToNoelinkId = new HashMap<String, Object>();
        originalManeToNoelinkId.putAll(oedhExisting.getManyToOnelinkId());

        // 複合キーでNTKPの項目(ex. _EntityType.Name)があれば、リンク情報を設定する
        if (KeyType.COMPLEX.equals(oEntityWrapper.getEntityKey().getKeyType())) {
            try {
                setLinksFromOEntity(oEntityWrapper, oedhNew);
            } catch (NTKPNotFoundException e) {
                throw DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
            }
        }

        // __publishedは更新しないため、ESから取得した値を使用する
        oedhNew.setPublished(oedhExisting.getPublished());

        // サポートする変更かどうかをチェックする
        checkAcceptableModification(entitySetName, oedhExisting, originalManeToNoelinkId, oedhNew);

        // 更新前処理
        this.beforeUpdate(entitySetName, originalKey, oedhNew);

        if (isMergeMode) {
            // マージモードの場合は、既存のドキュメントにリクエストのドキュメントをマージする
            oedhExisting.convertAliasToName(getMetadata());
            ODataProducerUtils.mergeFields(oedhExisting, oedhNew);

            // スキーマ内のプロパティ数とデータ内のdynamicプロパティ数が制限値を超えないかチェック
            int propNum = ODataUtils.getStaticPropertyCount(this.getMetadata(), entitySetName);
            checkPropertySize(propNum + oedhNew.getDynamicFields().size());
        }

        // ユーザデータ更新の場合は__idは更新しない
        Map<String, Object> staticFields = oedhNew.getStaticFields();
        if (staticFields.containsKey("__id")) {
            // ユーザデータは現在単一キーなので複合キーの対応はしない
            // TODO 今後、ユーザデータを複合キーにする場合、複合キーの対応が必要
            staticFields.put("__id", originalKey.asSingleValue());
            oedhNew.setStaticFields(staticFields);
        }

        // hidden fieldsの情報とUnitUser名を更新する。
        // ただし、Account更新の場合は、HashedCredentialを置換しないように対処
        String hashedCredentialValue = (String) oedhNew.getHiddenFields().get("HashedCredential");
        oedhNew.getHiddenFields().putAll(oedhExisting.getHiddenFields());
        if (hashedCredentialValue != null) {
            oedhNew.getHiddenFields().put("HashedCredential", hashedCredentialValue);
        }
        oedhNew.resolveUnitUserName(oedhExisting.getHiddenFields());

        // DynamicFieldの内容をコピーする
        if (oedhExisting.getDynamicFields() != null) {
            oedhNew.getDynamicFields().putAll(oedhExisting.getDynamicFields());
        }

        // ACL情報をコピーする
        if (oedhExisting.getAclFields() != null) {
            oedhNew.getAclFields().putAll(oedhExisting.getAclFields());
        }

        // esJsonをESに保存する
        DcIndexResponse idxRes = null;
        // リクエストのEtag指定から検査用versionを取り出す（Etag指定が無い場合はNull）
        Long version = oedhNew.getVersion();
        if (version == null || version < 0) {
            idxRes = esType.update(oedhNew.getId(), oedhNew);
        } else {
            idxRes = esType.update(oedhNew.getId(), oedhNew, version);
        }

        // 更新後の処理
        this.afterUpdate();

        // Resource層でETag返還ができるよう、レスポンスから得たVersion情報を、引数のOEntityWrapperに破壊的に設定
        oedhNew.setVersion(idxRes.version());
        oEntityWrapper.setEtag(oedhNew.createEtag());
    }

    /**
     * Accountのパスワード変更を実行する.
     * @param entitySet entitySetName
     * @param originalKey 更新対象キー
     * @param dcCredHeader dcCredHeader
     */
    public void updatePassword(final EdmEntitySet entitySet,
            final OEntityKey originalKey, final String dcCredHeader) {
        Lock lock = lock();
        try {
            // ESから変更するAccount情報を取得する
            EntitySetDocHandler oedhNew = this.retrieveWithKey(entitySet, originalKey);
            if (oedhNew == null) {
                throw DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            }
            // 取得したAccountのパスワードと更新日を上書きする
            ODataProducerUtils.createRequestPassword(oedhNew, dcCredHeader);

            // esJsonをESに保存する
            // Accountのバージョン情報を取り出す
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySet.getName());
            Long version = oedhNew.getVersion();
            esType.update(oedhNew.getId(), oedhNew, version);
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Accountの最終ログイン時刻を更新する.
     * @param entitySet entitySetName
     * @param originalKey 更新対象キー
     * @param accountId アカウントのID
     */
    public void updateLastAuthenticated(final EdmEntitySet entitySet, final OEntityKey originalKey, String accountId) {
        Lock lock = lock();
        try {
            // 現在時刻を取得
            long nowTimeMillis = System.currentTimeMillis();

            // ESから変更するAccount情報を取得する
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySet.getName());
            DcGetResponse dcGetResponseNew = esType.get(accountId);
            if (dcGetResponseNew == null) {
                // 認証から最終ログイン時刻更新までにAccountが削除された場合は、更新対象が存在しないため、正常終了する。
                DcCoreLog.Auth.ACCOUNT_ALREADY_DELETED.params(originalKey.toKeyString()).writeLog();
                return;
            }
            EntitySetDocHandler oedhNew = new OEntityDocHandler(dcGetResponseNew);
            // 取得したAccountの最終ログイン日時を上書きする
            Map<String, Object> staticFields = oedhNew.getStaticFields();
            staticFields.put("LastAuthenticated", nowTimeMillis);
            oedhNew.setStaticFields(staticFields);
            // 本メソッドが呼ばれるのは認証成功時なので、Accountの更新ではないとみなしている。
            // このため、Accountの__updatedは上書きしていない。
            // また、ElasticsearchのデータをUpdateする際、ETagが置き換わるが、こちらは置き換わってよいものとする。

            // esJsonをESに保存する
            // Accountのバージョン情報を取り出す
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
            // 注）EntitySetの存在保証は予め呼び出し側で行われているため、ここではチェックしない。
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySetName);
            // Cell / Box / Node / EntityTypeに基づいた暗黙フィルタの作成
            List<Map<String, Object>> implicitFilters = getImplicitFilters(entitySetName);
            ODataQueryHandler visitor = getODataQueryHandler(queryInfo, eSet.getType(), implicitFilters);
            Map<String, Object> source = visitor.getSource();
            try {
                tmpCount = esType.count(source);
            } catch (EsClientException ex) {
                if (ex.getCause() instanceof DcSearchPhaseExecutionException) {
                    throw DcCoreException.Server.DATA_STORE_SEARCH_ERROR.reason(ex);
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
            // 1対Nのリンクを持つデータを検索するクエリを作成
            Map<String, Object> key = getLinkFieldsQuery(getLinkskey(toEntitySetName), src.getId());
            List<Map<String, Object>> filters = getImplicitFilters(fromEntitySetName);
            filters.add(key);

            // 条件検索を組み立てる
            EdmEntityType type = getMetadata().findEdmEntitySet(fromEntitySetName).getType();
            ODataQueryHandler visitor = getODataQueryHandler(query, type, filters);
            Map<String, Object> source = visitor.getSource();

            EntitySetAccessor esType = this.getAccessorForEntitySet(fromEntitySetName);
            tmpCount = esType.count(source);
        } catch (DcCoreException e) {
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
     * NavigationProperty経由でエンティティを一括登録する.
     * @param npBulkContexts 一括登録のコンテキスト
     * @param npBulkRequests エンティティ一括登録用のリクエスト情報（bulkCreateEntity用）
     */
    public void bulkCreateEntityViaNavigationProperty(
            List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {

        // ユニーク性チェックのためまずロックを行う
        Lock lock = this.lock();
        log.debug("bulkCreateEntityViaNavigationProperty get lock");
        try {
            // リンク元のデータを一括検索する
            if (!setLinkSourcesToBulkContexts(npBulkContexts)) {
                // 処理対象のデータが存在しないとき
                return;
            }

            // リンク元・先のデータチェックや、既にリンクが作成済みかなどの、データ登録の前提条件をチェックする
            int contextIndex = 0;
            for (BulkRequest npBulkRequest : npBulkRequests.values()) {
                NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
                if (!npBulkContext.isError()) {
                    try {
                        // リンクタイプを設定する
                        setNavigationPropertyLinkType(npBulkContext);

                        // データチェックをする
                        validateLinkForNavigationPropertyContext(npBulkContext);
                        if (isConflictLinks(npBulkContexts, npBulkContext)) {
                            npBulkRequest.setError(DcCoreException.OData.CONFLICT_LINKS);
                            npBulkContext.setException(DcCoreException.OData.CONFLICT_LINKS);
                        }
                    } catch (Exception e) {
                        npBulkRequest.setError(e);
                        npBulkContext.setException(e);
                    }
                }
            }

            // $linksの上限値チェック
            checkLinksUpperLimitRecord(npBulkContexts, npBulkRequests);

            // エンティティを登録する
            List<EntityResponse> resultList = bulkCreateEntityWithoutLock(getMetadata(), npBulkRequests, getCellId());

            // エンティティ登録の結果をもとに、一括登録のコンテキストを更新する
            int index = 0;
            contextIndex = 0;
            for (BulkRequest request : npBulkRequests.values()) {
                NavigationPropertyBulkContext npBulkContext = npBulkContexts.get(contextIndex++);
                Exception exception = request.getError();
                if (exception != null) {
                    // エンティティの登録時に発生したExceptionを設定する
                    npBulkContext.setException(exception);
                } else {
                    // Etagなどの情報が付与されるため、登録したエンティティでコンテキストを更新する
                    EntityResponse entityResponse = resultList.get(index);
                    OEntityWrapper entity = (OEntityWrapper) entityResponse.getEntity();
                    npBulkContext.setOEntityWrapper(entity);
                    npBulkContext.setEntityResponse(entityResponse);

                    // 登録したエンティティの情報をコンテキストに設定
                    EntitySetDocHandler targetDocHandler = request.getDocHandler();
                    targetDocHandler.setId(entity.getUuid());
                    npBulkContext.setTargetDocHandler(targetDocHandler);

                    // リンク情報をコンテキストに設定する
                    setNavigationPropertyContext(npBulkContext);
                    index++;
                }
            }

            // リンク情報を登録する
            bulkCreateLinks(npBulkContexts, getCellId());
        } finally {
            lock.release();
            log.debug("bulkCreateEntityViaNavigationProperty release lock");
        }
    }

    /**
     * NavigationProperty経由でエンティティを一括登録する際のリンク数の上限値チェックを行う.
     * @param npBulkContexts 一括登録のコンテキスト
     * @param npBulkRequests エンティティ一括登録用のリクエスト情報（bulkCreateEntity用）
     */
    public void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests) {
    }

    /**
     * ソース側のエンティティを一括検索してNavigationPropertyBulkContextに設定する.
     * @param npBulkContexts コンテキスト
     * @return true: 正常に処理が終了 / false: 処理対象のデータが存在しない
     */
    @SuppressWarnings("unchecked")
    private boolean setLinkSourcesToBulkContexts(List<NavigationPropertyBulkContext> npBulkContexts) {
        // 一括検索用に一時的にキーとコンテキストのMapを作成
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

        // リンク元のデータを一括検索する
        Map<String, Object> searchQuery = getBulkSearchQuery(npBulkContexts);
        if (searchQuery == null) {
            // 処理対象のデータが存在しないとき
            return false;
        }
        DataSourceAccessor accessor = getAccessorForBatch();
        DcSearchResponse searchResponse = accessor.searchForIndex(getCellId(), searchQuery);
        if (searchResponse.getHits().getCount() != 0) {
            for (DcSearchHit hit : searchResponse.getHits().getHits()) {
                // TODO 複合主キー対応
                HashMap<String, Object> staticFields = (HashMap<String, Object>) hit.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) hit.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                String key = entityTypeId + ":" + (String) staticFields.get("__id");
                List<NavigationPropertyBulkContext> targetContexts = npBulkContextMap.get(key);
                for (NavigationPropertyBulkContext ctx : targetContexts) {
                    // リンク元のデータをコンテキストに設定する
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
     * バルク登録を実行する.
     * @param metadata スキーマ情報
     * @param bulkRequests 登録するBatchCreateRequestのリスト
     * @param cellId cellId
     * @return EntitiesResponse
     */
    public List<EntityResponse> bulkCreateEntity(
            EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            String cellId) {
        // ロック取得
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
     * バルク登録を実行する.
     * このメソッドはロックを取得しないため、必ず呼び出しもとでロックを取得・解放すること.
     * @param metadata スキーマ情報
     * @param bulkRequests 登録するBatchCreateRequestのリスト
     * @param cellId cellId
     * @return EntitiesResponse
     */
    @SuppressWarnings("unchecked")
    private List<EntityResponse> bulkCreateEntityWithoutLock(EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests,
            String cellId) {
        List<EntityResponse> response = new ArrayList<EntityResponse>();

        DataSourceAccessor accessor = getAccessorForBatch();

        // elasticsearchに主キーが衝突するデータがあればエラー情報を設定する
        Map<String, Object> searchQuery = getBulkConflictCheckQuery(bulkRequests);
        if (searchQuery == null) {
            return response;
        }
        DcSearchResponse searchResponse = accessor.searchForIndex(cellId, searchQuery);
        if (searchResponse.getHits().getCount() != 0) {
            for (DcSearchHit hit : searchResponse.getHits().getHits()) {
                // TODO 複合主キー対応
                HashMap<String, Object> staticFields = (HashMap<String, Object>) hit.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) hit.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                String key = entityTypeId + ":" + (String) staticFields.get("__id");
                bulkRequests.get(key).setError(DcCoreException.OData.ENTITY_ALREADY_EXISTS);
            }
        }

        beforeBulkCreate(bulkRequests);

        // 登録対象のみのリクエストデータを生成する
        Map<String, String> keyMap = new HashMap<String, String>();
        List<EsBulkRequest> esBulkRequest = new ArrayList<EsBulkRequest>();
        List<EntitySetDocHandler> adsBulkRequest = new ArrayList<EntitySetDocHandler>();
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            if (request.getValue().getError() == null) {
                keyMap.put(request.getValue().getId(), request.getKey());

                esBulkRequest.add(request.getValue());
                adsBulkRequest.add(request.getValue().getDocHandler());
            }
        }
        if (esBulkRequest.size() == 0) {
            return response;
        }
        // 一括登録を実行する
        try {
            DcBulkResponse bulkResponse = accessor.bulkCreate(esBulkRequest, adsBulkRequest, cellId);
            // EntitiesResponse組み立て
            for (DcBulkItemResponse itemResponse : bulkResponse.items()) {
                String key = keyMap.get(itemResponse.getId());
                if (itemResponse.isFailed()) {
                    // バルク内でエラーが発生していた場合はエラーをセットする
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
        } catch (DcCoreException e) {
            // バルクリクエストが失敗した場合は、バルクで登録に使用したデータすべてにエラーを設定する
            DcCoreLog.OData.BULK_INSERT_FAIL.reason(e).writeLog();
            for (EsBulkRequest request : esBulkRequest) {
                HashMap<String, Object> staticFields = (HashMap<String, Object>) request.getSource()
                        .get(OEntityDocHandler.KEY_STATIC_FIELDS);
                String entityTypeId = (String) request.getSource().get(OEntityDocHandler.KEY_ENTITY_ID);
                bulkRequests.get(entityTypeId + ":" + (String) staticFields.get("__id")).setError(e);
            }
        } catch (EsClientException e) {
            // バルクリクエストが失敗した場合は、バルクで登録に使用したデータすべてにエラーを設定する
            DcCoreLog.OData.BULK_INSERT_FAIL.reason(e).writeLog();
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
     * bulkRequestsに含まれるエンティティの一括検索用のクエリを作成する.
     * @param bulkRequests 一括検索するリクエストのリスト
     * @return 検索クエリ
     */
    private Map<String, Object> getBulkConflictCheckQuery(LinkedHashMap<String, BulkRequest> bulkRequests) {
        // 検索条件のためのHash初期化
        List<Object> orList = new ArrayList<Object>();

        // データ競合確認のためのクエリを生成する
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            // エラーデータは無視する
            if (request.getValue().getError() != null) {
                continue;
            }
            // TODO スキーマ情報の主キーから生成、ユニークキーのチェック、NTKP対応
            List<Object> andList = new ArrayList<Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            andList.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id.untouched",
                    (String) request.getValue().getDocHandler().getStaticFields().get("__id"), false));
            // タイプの指定
            andList.add(QueryMapFactory.termQuery("_type",
                    request.getValue().getDocHandler().getType()));
            andList.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID,
                    (String) request.getValue().getDocHandler().getEntityTypeId()));
            and.put("and", andList);
            orList.add(and);
        }
        // 対象データが存在しないとき
        if (orList.size() == 0) {
            return null;
        }

        return composeQueryWithOrFilter(orList);
    }

    /**
     * コンテキストに含まれるソース側エンティティの一括検索用のクエリを作成する.
     * @param bulkContexts コンテキスト
     * @return 検索クエリ
     */
    private Map<String, Object> getBulkSearchQuery(List<NavigationPropertyBulkContext> bulkContexts) {
        // 検索条件のためのHash初期化
        List<Object> orList = new ArrayList<Object>();

        // 重複クエリを排除する
        Set<List<Object>> registeredQuery = new HashSet<List<Object>>();

        // データ競合確認のためのクエリを生成する
        for (NavigationPropertyBulkContext bulkContext : bulkContexts) {
            if (bulkContext.isError()) {
                // エラーデータは無視する
                continue;
            }

            // TODO スキーマ情報の主キーから生成、ユニークキーのチェック、NTKP対応
            List<Object> andList = new ArrayList<Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            andList.add(QueryMapFactory.termFilter(OEntityDocHandler.KEY_STATIC_FIELDS + ".__id.untouched",
                    bulkContext.getSrcEntityId().getEntityKey().asSingleValue().toString(), false));
            // タイプの指定
            andList.add(QueryMapFactory.termQuery("_type",
                    UserDataODataProducer.USER_ODATA_NAMESPACE));
            andList.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_ENTITY_ID,
                    getLinkskey(bulkContext.getSrcEntityId().getEntitySetName())));

            // 重複クエリを排除する
            if (registeredQuery.contains(andList)) {
                continue;
            }
            registeredQuery.add(andList);
            and.put("and", andList);
            orList.add(and);
        }
        // 対象データが存在しないとき
        if (orList.size() == 0) {
            return null;
        }
        return composeQueryWithOrFilter(orList);
    }

    /**
     * 引数で渡されたorフィルタクエリにCell、Box,NodeIDの検索条件などを付加する.
     * @param orList orフィルタクエリ
     * @return 検索クエリ
     */
    private Map<String, Object> composeQueryWithOrFilter(List<Object> orList) {
        // 検索条件のためのHash初期化
        Map<String, Object> searchQuery = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();

        filter.put("or", orList);

        // Cell、Box,NodeIDの検索条件を追加
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
     * バルク登録を実行する.
     * @param bulkContexts 登録するNavigationPropertyBulkContextのリスト
     * @param cellId cellId
     */
    private void bulkCreateLinks(
            List<NavigationPropertyBulkContext> bulkContexts,
            String cellId) {
        // 登録対象のみのリクエストデータを生成する
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

        // 一括登録を実行する
        DataSourceAccessor accessor = getAccessorForBatch();
        try {
            int responseIndex = 0;
            DcBulkResponse bulkResponse = accessor.bulkUpdateLink(esBulkRequest, adsBulkEntityRequest,
                    adsBulkLinkRequest, cellId);
            DcBulkItemResponse[] responseItems = bulkResponse.items();
            for (NavigationPropertyBulkContext context : bulkContexts) {
                if (context.isError()) {
                    continue;
                }

                NavigationPropertyLinkType linkType = context.getLinkType();
                switch (linkType) {
                case oneToOne:
                    DcBulkItemResponse itemResponseFrom = responseItems[responseIndex++];
                    DcBulkItemResponse itemResponseTo = responseItems[responseIndex++];
                    if (itemResponseFrom.isFailed() || itemResponseTo.isFailed()) {
                        context.setException(new ServerErrorException("failed to store to es"));
                    } else {
                        setETagVersion((OEntityWrapper) context.getOEntityWrapper(), itemResponseTo.version());
                    }
                    break;
                case manyToOne:
                    DcBulkItemResponse manyToOneResponse = responseItems[responseIndex++];
                    if (manyToOneResponse.isFailed()) {
                        context.setException(new ServerErrorException("failed to store to es"));
                    }
                    break;
                case oneToMany:
                case manyToMany:
                    DcBulkItemResponse itemResponse = responseItems[responseIndex++];
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
            // バルクリクエストが失敗した場合は、バルクで登録に使用したデータすべてにエラーを設定する
            for (NavigationPropertyBulkContext context : bulkContexts) {
                if (context.isError()) {
                    continue;
                }
                context.setException(new ServerErrorException("failed to store to es"));
            }
        }
    }

    /**
     * リクエストボディの要素数をチェックする.
     * @param propNum プロパティ数
     */
    public void checkPropertySize(int propNum) {
    }

    /**
     * サポートする変更かどうかをチェックする.
     * @param entitySetName エンティティセット名
     * @param oedhExisting データストアに存在するデータ
     * @param originalManeToNoelinkId データストアに存在するリンク情報
     * @param oedhNew リクエストデータ
     */
    protected void checkAcceptableModification(String entitySetName,
            EntitySetDocHandler oedhExisting,
            Map<String, Object> originalManeToNoelinkId,
            EntitySetDocHandler oedhNew) {
    }


    /**
     * 引数で渡されたEntitySetのキー名を参照しているドキュメントがあるかどうかを確認する.
     * <p>
     * 更新対象のドキュメントを名前（EntitySetのキー名）で参照している場合が考えられる。 このような場合、ドキュメント更新前に参照元のドキュメントが存在しているかどうかを確認する必要がある。
     * </p>
     * @param entitySetName リクエストURLに指定された処理対象のEntitySet名
     * @param entityKey リクエストURLに指定された処理対象EntitySetのキー名
     */
    protected void hasRelatedEntities(String entitySetName, OEntityKey entityKey) {
    }

}

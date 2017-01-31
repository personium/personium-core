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
package io.personium.core.model.impl.es.doc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDateTime;
import org.json.simple.JSONObject;
import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OLinks;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.core.OSimpleObject;
import org.odata4j.core.OSimpleObjects;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.expression.EntitySimpleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.DcGetResponse;
import io.personium.common.es.response.DcSearchHit;
import io.personium.common.es.response.DcSearchHitField;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Property;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;

/**
 * OEntityのDocHandler.
 */
public class OEntityDocHandler implements EntitySetDocHandler {
    // ESのtype
    String type;
    // ESのid
    String id;
    // ESのversion
    Long version;
    // UnitUser名
    String unitUserName;

    Map<String, Object> staticFields = new HashMap<String, Object>();
    Map<String, Object> dynamicFields = new HashMap<String, Object>();
    Map<String, Object> hiddenFields = new HashMap<String, Object>();
    String cellId;
    String boxId;
    String nodeId;
    String entityTypeId;
    Long published;
    Long updated;
    Map<String, Object> manyToOnelinkId = new HashMap<String, Object>();
    int expandMaxNum;

    /**
     * コンストラクタ.
     */
    public OEntityDocHandler() {
    }

    /**
     * @return Dynamic Field の Map
     */
    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    /**
     * @param dynamicFields Dynamic Field の Map
     */
    public void setDynamicFields(Map<String, Object> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    /**
     * @return Hidden Field の Map
     */
    public Map<String, Object> getHiddenFields() {
        return hiddenFields;
    }

    /**
     * @param hiddenFields Hidden Field の Map
     */
    public void setHiddenFields(Map<String, Object> hiddenFields) {
        this.hiddenFields = hiddenFields;
    }

    /**
     * @param id ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param published published
     */
    public void setPublished(Long published) {
        this.published = published;
    }

    /**
     * @param updated updated
     */
    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    /**
     * Constructor.
     * @param getResponse GetResponse
     */
    public OEntityDocHandler(DcGetResponse getResponse) {
        this.type = getResponse.getType();
        this.id = getResponse.getId();
        this.version = getResponse.getVersion();
        Map<String, Object> source = getResponse.getSource();
        this.parseSource(source);
        this.resolveUnitUserName();
    }

    /**
     * Map形式のSourceをパースして自身にマッピングする.
     * @param source Map形式のマッピング用情報
     */
    @SuppressWarnings("unchecked")
    protected void parseSource(Map<String, Object> source) {
        this.hiddenFields = (Map<String, Object>) source.get(KEY_HIDDEN_FIELDS);
        this.staticFields = (Map<String, Object>) source.get(KEY_STATIC_FIELDS);
        this.dynamicFields = (Map<String, Object>) source.get(KEY_DYNAMIC_FIELDS);
        this.cellId = (String) source.get(KEY_CELL_ID);
        this.boxId = (String) source.get(KEY_BOX_ID);
        this.nodeId = (String) source.get(KEY_NODE_ID);
        this.entityTypeId = (String) source.get(KEY_ENTITY_ID);

        this.published = (Long) source.get(KEY_PUBLISHED);
        this.updated = (Long) source.get(KEY_UPDATED);

        this.manyToOnelinkId = parseLinks(source);
    }

    /**
     * Linkフィールドをパースする.
     * @param source Map形式のパース元情報
     * @return Link情報
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseLinks(Map<String, Object> source) {
        return (Map<String, Object>) source.get(KEY_LINK);
    }

    void parseFields(Map<String, DcSearchHitField> fields) {
        for (Map.Entry<String, DcSearchHitField> ent : fields.entrySet()) {
            String key = ent.getKey();
            DcSearchHitField value = ent.getValue();
            if (key.startsWith(KEY_STATIC_FIELDS + ".")) {
                this.staticFields.put(key.substring((KEY_STATIC_FIELDS + ".").length()),
                        value.getValues().get(0));
            } else if (key.startsWith(KEY_DYNAMIC_FIELDS + ".")) {
                this.dynamicFields.put(key.substring((KEY_DYNAMIC_FIELDS + ".").length()),
                        value.getValues().get(0));
            }
        }
        this.cellId = (String) fields.get(KEY_CELL_ID).getValues().get(0);
        this.boxId = (String) fields.get(KEY_BOX_ID).getValues().get(0);
        this.nodeId = (String) fields.get(KEY_NODE_ID).getValues().get(0);
        this.entityTypeId = (String) fields.get(KEY_ENTITY_ID).getValues().get(0);

        this.published = (Long) fields.get(KEY_PUBLISHED).getValues().get(0);
        this.updated = (Long) fields.get(KEY_UPDATED).getValues().get(0);
    }

    /**
     * Constructor.
     * @param searchHit SearchHit
     */
    public OEntityDocHandler(DcSearchHit searchHit) {
        this.type = searchHit.getType();
        this.id = searchHit.getId();
        this.version = searchHit.getVersion();
        Map<String, Object> source = searchHit.getSource();
        if (source != null) {
            this.parseSource(source);
        }
//        Map<String, DcSearchHitField> fields = searchHit.getFields();
//        if (fields.size() > 0) {
//            this.parseFields(fields);
//        }
        this.resolveUnitUserName();
    }

    /**
     * OEntityWrapperから IDのないDocHandlerをつくるConstructor.
     * @param type ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    public OEntityDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        initInstance(type, oEntityWrapper, metadata);
    }

    /**
     * OEntityWrapperから IDのないDocHandlerをつくる.
     * @param typeName ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    @SuppressWarnings("unchecked")
    protected void initInstance(String typeName, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        this.type = typeName;
        // 指定されたuuidをES IDとして設定する
        this.id = oEntityWrapper.getUuid();
        // OEntity Wrapperのときは Version, hiddenFieldをつめる
        this.hiddenFields = oEntityWrapper.getMetadata();
        // Cellへのアクセス時のみUnitUser名を設定する
        resolveUnitUserName();
        String etag = oEntityWrapper.getEtag();
        if (etag != null && etag.length() > 1) {
            this.version = Long.valueOf(etag.substring(0, etag.indexOf("-")));
        }

        // スキーマ情報を取得
        EdmEntitySet entitySet = oEntityWrapper.getEntitySet();
        EdmEntityType eType = entitySet.getType();

        // スキーマに定義されたNavPropを取得
        List<EdmNavigationProperty> navProps = eType.getDeclaredNavigationProperties().toList();
        for (EdmNavigationProperty np : navProps) {
            // NavPropそれぞれについて
            EdmMultiplicity mf = np.getFromRole().getMultiplicity();
            EdmMultiplicity mt = np.getToRole().getMultiplicity();

            // AssociationのMultiplicityとして、こちら側がMANYで相手がONEのときは、
            // NavigationPropertyの値（URL）から、lの項目を求めて詰める
            if (EdmMultiplicity.ONE.equals(mt) && EdmMultiplicity.MANY.equals(mf)) {
                // TODO 未実装
                log.debug("many to one");

            }
        }

        // やってきたすべてのプロパティに対して、
        for (OProperty<?> prop : oEntityWrapper.getProperties()) {
            // スキーマに定義されているかを調べ、Dynamic PropertyかDeclared Propertyの処理切替をする
            String propName = prop.getName();
            EdmProperty edmProperty = eType.findProperty(propName);

            // 定義済みPropertyかDynamicPropertyかで処理を分岐
            if (edmProperty != null) {
                // スキーマ定義されたプロパティ
                // TODO ここでやるかどうか不明だが、 スキーマ上定義された型にあったデータが来ているかのチェック
                // もっと前段階でやっておくべき。

                // タイプごとに値を詰め替える.
                Object value = prop.getValue();
                if ("__published".equals(propName)) {
                    OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) prop;
                    LocalDateTime ldt = propD.getValue();
                    this.published = ldt.toDateTime().getMillis();
                } else if ("__updated".equals(propName)) {
                    OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) prop;
                    LocalDateTime ldt = propD.getValue();
                    this.updated = ldt.toDateTime().getMillis();
                } else {
                    CollectionKind ck = edmProperty.getCollectionKind();
                    if (edmProperty.getType().isSimple()) {
                        if (ck.equals(CollectionKind.List)) {
                            // 不正な型の場合はエラー
                            if (value == null || value instanceof OCollection<?>) {
                                this.staticFields.put(prop.getName(),
                                        getSimpleList(edmProperty.getType(), (OCollection<OObject>) value));
                            } else {
                                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(prop.getName());
                            }
                        } else {
                            value = getSimpleValue(prop, edmProperty.getType());

                            // PropertyがSimple型の場合はそのまま定義済み項目として追加する
                            this.staticFields.put(prop.getName(), value);
                        }
                    } else {
                        String complexTypeName = edmProperty.getType().getFullyQualifiedTypeName();
                        if (ck.equals(CollectionKind.List)) {
                            // CollectionKindがListの場合は、配列で追加する
                            this.staticFields.put(
                                    prop.getName(),
                                    getComplexList((OCollection<OComplexObject>) prop.getValue(), metadata,
                                            complexTypeName));
                        } else {
                            // PropertyがComplex型の場合は再帰的にComplexTypeのプロパティを読み込み追加する
                            this.staticFields.put(prop.getName(), getComplexType(prop, metadata, complexTypeName));
                        }
                    }
                }
            } else {
                // Dynamicプロパティは、String,Integer,Float,Booleanを受ける
                Object propValue = prop.getValue();
                if ("Edm.DateTime".equals(prop.getType().getFullyQualifiedTypeName())) {
                    OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) prop;
                    LocalDateTime ldt = propD.getValue();
                    if (ldt != null) {
                        propValue = "\\/Date(" + ldt.toDateTime().toDate().getTime() + ")\\/";
                    }
                }

                this.dynamicFields.put(prop.getName(), propValue);
            }
        }
    }

    /**
     * スキーマのプロパティ定義に応じて適切な型に変換したプロパティ値オブジェクトを返す.
     * @param prop プロパティオブジェクト
     * @param edmType スキーマのプロパティ定義
     * @return 適切な型に変換したプロパティ値オブジェクト
     */
    @SuppressWarnings("unchecked")
    protected Object getSimpleValue(OProperty<?> prop, EdmType edmType) {
        if (edmType.equals(EdmSimpleType.DATETIME)) {
            OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) prop;
            LocalDateTime ldt = propD.getValue();
            if (ldt != null) {
                return ldt.toDateTime().getMillis();
            }
        }
        return prop.getValue();
    }

    /**
     * Cellへのアクセス時にUnitUser名を設定する.
     * @param hiddenFieldsMap hiddenFieldsのマップオブジェクト
     */
    public void resolveUnitUserName(final Map<String, Object> hiddenFieldsMap) {
        if (hiddenFieldsMap == null) {
            return;
        }
        // Adsアクセス用にUnitUser名を設定する
        String owner = (String) hiddenFieldsMap.get("Owner");
        this.unitUserName = DcCoreConfig.getEsUnitPrefix() + "_";
        if (owner == null) {
            this.unitUserName += AccessContext.TYPE_ANONYMOUS;
        } else {
            this.unitUserName += IndexNameEncoder.encodeEsIndexName(owner);
        }
    }

    /**
     * Cellへのアクセス時にUnitUser名を設定する.
     */
    private void resolveUnitUserName() {
        this.resolveUnitUserName(this.hiddenFields);
    }

    /**
     * SimpleTypeの配列を取得する.
     * @param edmType EdmSimpleType
     * @param value OCollection
     * @return SimpleTypeの配列
     */
    protected List<Object> getSimpleList(final EdmType edmType, final OCollection<OObject> value) {
        if (value == null) {
            return null;
        }

        // CollectionKindがListの場合は、配列で追加する
        Iterator<OObject> iterator = value.iterator();
        List<Object> list = new ArrayList<Object>();
        while (iterator.hasNext()) {
            Object propValue = ((OSimpleObject<?>) iterator.next()).getValue();
            propValue = convertSimpleListValue(edmType, propValue);

            list.add(propValue);
        }
        return list;
    }

    /**
     * 配列の一つのデータを変換する.
     * @param edmType EdmSimpleType
     * @param propValue 変換対象の値
     * @return 変換後の値
     */
    @SuppressWarnings("unchecked")
    protected Object convertSimpleListValue(final EdmType edmType, Object propValue) {
        if (edmType.equals(EdmSimpleType.DATETIME)) {
            OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) propValue;
            if (propD != null) {
                LocalDateTime ldt = propD.getValue();
                if (ldt != null) {
                    return ldt.toDateTime().getMillis();
                }
            }
        }
        return propValue;
    }

    /**
     * ComplexTypeの配列を取得する.
     * @param value OCollection
     * @param metadata スキーマ情報
     * @param complexTypeName コンプレックスタイプ名
     * @return SimpleTypeの配列
     */
    protected List<Object> getComplexList(OCollection<OComplexObject> value,
            EdmDataServices metadata,
            String complexTypeName) {
        if (value == null) {
            return null;
        }
        Iterator<OComplexObject> iterator = value.iterator();
        List<Object> list = new ArrayList<Object>();
        while (iterator.hasNext()) {
            list.add(getComplexType(iterator.next().getProperties(), metadata, complexTypeName));
        }
        return list;
    }

    /**
     * ComplexTypeのプロパティを読み込み取得する.
     * @param property
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object getComplexType(OProperty<?> property, EdmDataServices metadata, String complexTypeName) {
        return getComplexType((List<OProperty<?>>) property.getValue(), metadata, complexTypeName);
    }

    /**
     * ComplexTypeのプロパティを読み込み取得する.
     * @param property プロパティList
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object getComplexType(List<OProperty<?>> props, EdmDataServices metadata, String complexTypeName) {
        // 指定されたPropertyの値を取得する
        Map<String, Object> complex = new HashMap<String, Object>();

        if (props == null) {
            return null;
        }

        // ComplexTypeのPropertyをHashに追加する
        for (OProperty<?> prop : props) {
            EdmProperty edmProp = metadata.findEdmComplexType(complexTypeName).findProperty(prop.getName());
            CollectionKind ck = edmProp.getCollectionKind();
            if (edmProp.getType().isSimple()) {
                if (ck.equals(CollectionKind.List)) {
                    // 不正な型の場合はエラー
                    if (prop.getValue() == null || prop.getValue() instanceof OCollection<?>) {
                        complex.put(prop.getName(),
                                getSimpleList(edmProp.getType(), (OCollection<OObject>) prop.getValue()));
                    } else {
                        throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(prop.getName());
                    }
                } else {
                    Object value = getSimpleValue(prop, edmProp.getType());
                    // PropertyがSimple型の場合はそのまま定義済み項目として追加する
                    complex.put(prop.getName(), value);
                }
            } else {
                // PropertyがComplex型の場合は再帰的にComplexTypeのプロパティを読み込み追加する
                String propComplexTypeName = edmProp.getType().getFullyQualifiedTypeName();
                if (ck.equals(CollectionKind.List)) {
                    // CollectionKindがListの場合は、配列で追加する
                    complex.put(
                            prop.getName(),
                            getComplexList((OCollection<OComplexObject>) prop.getValue(),
                                    metadata, propComplexTypeName));
                } else {
                    // PropertyがComplex型の場合は再帰的にComplexTypeのプロパティを読み込み追加する
                    complex.put(prop.getName(),
                            getComplexType(prop, metadata, propComplexTypeName));
                }

            }
        }
        return complex;
    }

    /**
     * EsのJSONオブジェクトからOEntityを作成して返す.
     * @param entitySet entitySet
     * @return 変換されたOEntityオブジェクト
     */
    public OEntityWrapper createOEntity(final EdmEntitySet entitySet) {
        return createOEntity(entitySet, null, null);
    }

    /**
     * EsのJSONオブジェクトからOEntityを作成して返す.
     * @param entitySet entitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @return 変換されたOEntityオブジェクト
     */
    public OEntityWrapper createOEntity(final EdmEntitySet entitySet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList) {
        return createOEntity(entitySet, null, null, null);
    }

    /**
     * ドキュメントハンドラの種別によりプロパティ名またはプロパティ名に対応するエイリアスを返却する.
     * @param entitySetName エンティティセット名
     * @param propertyName プロパティ名
     * @return プロパティ名またはプロパティ名に対応するエイリアス
     */
    protected String getPropertyNameOrAlias(String entitySetName, String propertyName) {
        return propertyName;
    }

    @Override
    public void convertAliasToName(EdmDataServices metadata) {
    }

    /**
     * EsのJSONオブジェクトからOEntityを作成して返す.
     * @param entitySet entitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @param selectQuery $selectクエリ
     * @return 変換されたOEntityオブジェクト
     */
    public OEntityWrapper createOEntity(final EdmEntitySet entitySet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList,
            List<EntitySimpleProperty> selectQuery) {
        EdmEntityType eType = entitySet.getType();

        List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

        HashMap<String, EntitySimpleProperty> selectMap = new HashMap<String, EntitySimpleProperty>();
        if (selectQuery != null) {
            for (EntitySimpleProperty prop : selectQuery) {
                selectMap.put(prop.getPropertyName(), prop);
            }
        }

        // スキーマに従って Declared Propertyを生成
        for (EdmProperty prop : eType.getProperties()) {
            // 予約項目の処理
            if ("__published".equals(prop.getName()) && this.published != null) {
                properties.add(OProperties.datetime(prop.getName(), new LocalDateTime(this.published)));
                continue;
            } else if ("__updated".equals(prop.getName()) && this.updated != null) {
                properties.add(OProperties.datetime(prop.getName(), new LocalDateTime(this.updated)));
                continue;
            }

            // $selectクエリが指定された場合は、指定された項目もしくは主キー情報のみ追加する
            if (selectMap.size() != 0 && !selectMap.containsKey(prop.getName())
                    && !eType.getKeys().contains(prop.getName())) {
                continue;
            }

            boolean isDynamic = false;
            NamespacedAnnotation<?> annotation = prop.findAnnotation(Common.DC_NAMESPACE.getUri(),
                    Property.P_IS_DECLARED.getName());
            if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
                isDynamic = true;
            }

            // 値を取得
            if (!this.staticFields.containsKey(getPropertyNameOrAlias(entitySet.getName(), prop.getName()))
                    && isDynamic) {
                continue;
            }
            Object valO = this.staticFields.get(getPropertyNameOrAlias(entitySet.getName(), prop.getName()));

            EdmType edmType = prop.getType();

            CollectionKind ck = prop.getCollectionKind();
            if (edmType.isSimple()) {
                // 非予約項目の処理
                if (ck.equals(CollectionKind.List)) {
                    // 配列要素の場合
                    addSimpleListProperty(properties, prop, valO, edmType);
                } else {
                    // シンプル要素の場合
                    addSimpleTypeProperty(properties, prop, valO, edmType);
                }
            } else {
                // ComplexType型のプロパティの場合
                if (metadata != null) {
                    if (ck.equals(CollectionKind.List)) {
                        // 配列要素の場合
                        addComplexListProperty(metadata, properties, prop, valO, edmType);
                    } else {
                        properties.add(createComplexTypeProperty(metadata, prop, valO));
                    }
                }
            }
        }

        // Navigation Propertyの生成処理
        int count = 0;
        List<OLink> links = new ArrayList<OLink>();
        // スキーマに定義されたナビゲーションプロパティそれぞれについて
        for (EdmNavigationProperty enp : eType.getNavigationProperties()) {
            EdmAssociationEnd ae = enp.getToRole();
            String toTypeName = ae.getType().getName();
            String npName = enp.getName();
            OLink lnk = null;
            List<OEntity> relatedEntities = null;
            if (relatedEntitiesList != null) {
                relatedEntities = relatedEntitiesList.get(npName);
            }
            if (EdmMultiplicity.MANY.equals(ae.getMultiplicity())) {
                // 相手先のMultiplicityがMANYであるときは
                if (++count <= expandMaxNum && relatedEntities != null) {
                    lnk = OLinks.relatedEntitiesInline(toTypeName, npName, npName,
                            relatedEntities);
                } else {
                    lnk = OLinks.relatedEntities(toTypeName, npName, npName);
                }
            } else {
                // 相手先のMultiplicityがMANYでないとき。（ZEROまたはONE）
                if (++count <= expandMaxNum && relatedEntities != null) {
                    lnk = OLinks.relatedEntitiesInline(toTypeName, npName, npName,
                            relatedEntities);
                } else {
                    lnk = OLinks.relatedEntity(toTypeName, npName, npName);
                }
            }
            links.add(lnk);
        }

        // entityKeyの生成
        List<String> keys = eType.getKeys();
        List<String> kv = new ArrayList<String>();
        for (String key : keys) {
            kv.add(key);
            // TODO キーがStringであることを仮定してしまってる。キーの値が文字列以外であるときは、その対応が必要。
            String v = (String) this.staticFields.get(key);
            if (v == null) {
                v = AbstractODataResource.DUMMY_KEY;
            }
            kv.add(v);
        }
        OEntityKey entityKey = OEntityKey.create(kv);

        // OEntityの生成
        // TODO AtomPub系項目がダミー
        OEntity entity = OEntities.create(entitySet, entityKey, properties, links, "title", "categoryTerm");

        // ETag や隠し項目が返せるようにするため、OEntityではなくOEntityWrapperにWrapする。
        String etag = this.createEtag();
        OEntityWrapper oew = new OEntityWrapper(this.id, entity, etag);
        // 隠し項目の設定
        if (this.hiddenFields != null) {
            for (Map.Entry<String, Object> entry : this.hiddenFields.entrySet()) {
                oew.put(entry.getKey(), entry.getValue());
            }
        }
        oew.setManyToOneLinks(this.manyToOnelinkId);
        return oew;
    }

    /**
     * シンプルタイプの配列要素をプロパティに追加する.
     * @param properties プロパティ一覧
     * @param edmProp 追加プロパティのスキーマ
     * @param propValue 追加プロカティの値
     * @param edmType タイプの型情報
     */
    @SuppressWarnings("unchecked")
    protected void addSimpleListProperty(List<OProperty<?>> properties,
            EdmProperty edmProp,
            Object propValue,
            EdmType edmType) {
        EdmCollectionType collectionType = new EdmCollectionType(edmProp.getCollectionKind(), edmType);
        OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType.getItemType());
        if (propValue == null) {
            properties.add(OProperties.collection(edmProp.getName(), collectionType, null));
        } else {
            for (Object val : (ArrayList<Object>) propValue) {
                if (null == val) {
                    builder.add(OSimpleObjects.parse((EdmSimpleType<?>) collectionType.getItemType(), null));
                } else {
                    builder.add(OSimpleObjects.parse((EdmSimpleType<?>) collectionType.getItemType(), val.toString()));
                }
            }
            properties.add(OProperties.collection(edmProp.getName(), collectionType, builder.build()));
        }
    }

    /**
     * Complexタイプの配列要素をプロパティに追加する.
     * @param metadata スキーマ情報
     * @param properties プロパティ一覧
     * @param edmProp 追加プロパティのスキーマ
     * @param propValue 追加プロカティの値
     * @param edmType タイプの型情報
     */
    @SuppressWarnings("unchecked")
    protected void addComplexListProperty(EdmDataServices metadata,
            List<OProperty<?>> properties,
            EdmProperty edmProp,
            Object propValue,
            EdmType edmType) {
        EdmCollectionType collectionType = new EdmCollectionType(edmProp.getCollectionKind(), edmType);
        OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType.getItemType());

        // ComplexTypeの型情報を取得する
        EdmComplexType ct = metadata.findEdmComplexType(
                collectionType.getItemType().getFullyQualifiedTypeName());

        if (propValue == null) {
            properties.add(OProperties.collection(edmProp.getName(), collectionType, null));
        } else {
            // ComplexTypeプロパティを配列に追加する
            for (Object val : (ArrayList<Object>) propValue) {
                builder.add(OComplexObjects.create(ct, getComplexTypePropList(metadata, edmProp, val)));
            }

            // ComplexTypeの配列要素をプロパティに追加する
            properties.add(OProperties.collection(edmProp.getName(), collectionType, builder.build()));
        }
    }

    /**
     * SimpleType型のプロパティをプロパティ配列に追加する.
     * @param properties プロパティ配列
     * @param prop 追加するプロパティ
     * @param valO プロパティの値
     * @param edmType タイプ
     */
    private void addSimpleTypeProperty(List<OProperty<?>> properties, EdmProperty prop, Object valO, EdmType edmType) {
        if (edmType.equals(EdmSimpleType.STRING)) {
            if (valO == null) {
                properties.add(OProperties.string(prop.getName(), null));
            } else {
                properties.add(OProperties.string(prop.getName(), String.valueOf(valO)));
            }
        } else if (edmType.equals(EdmSimpleType.DATETIME)) {
            if (valO == null) {
                properties.add(OProperties.null_(prop.getName(), EdmSimpleType.DATETIME));
            } else {
                // ユーザデータにInt型で表現できる範囲の値が入れられたとき、valOにはInteger型のオブジェクトが渡ってくる
                // LocalDateTimeのコンストラクタにInteger型を渡した場合はエラーとなるため、Long型オブジェクトへ変換する
                Long longvalO = Long.valueOf(String.valueOf(valO));
                properties.add(OProperties.datetime(prop.getName(), new LocalDateTime(longvalO)));
            }
        } else if (edmType.equals(EdmSimpleType.SINGLE)) {
            if (valO == null) {
                properties.add(OProperties.single(prop.getName(), null));
            } else {
                properties.add(OProperties.single(prop.getName(), Float.valueOf(String.valueOf(valO))));
            }
        } else if (edmType.equals(EdmSimpleType.DOUBLE)) {
            if (valO == null) {
                properties.add(OProperties.double_(prop.getName(), null));
            } else {
                properties.add(OProperties.double_(prop.getName(), Double.valueOf(String.valueOf(valO))));
            }
        } else if (edmType.equals(EdmSimpleType.INT32)) {
            if (valO == null) {
                properties.add(OProperties.int32(prop.getName(), null));
            } else {
                properties.add(OProperties.int32(prop.getName(), Integer.valueOf(String.valueOf(valO))));
            }
        } else if (edmType.equals(EdmSimpleType.BOOLEAN)) {
            // ESにはValueを文字列で登録しているのでBooleanに変換する
            if (valO == null) {
                properties.add(OProperties.boolean_(prop.getName(), null));
            } else {
                properties.add(OProperties.boolean_(prop.getName(), Boolean.valueOf(String.valueOf(valO))));
            }
        }
    }

    /**
     * ComplexType型のプロパティをOPropertyの配列に変換する.
     * @param metadata スキーマ定義
     * @param prop ComplexType型のプロパティ
     * @param value ComplexType型の値
     * @return OPropertyの配列
     */
    private OProperty<List<OProperty<?>>> createComplexTypeProperty(
            EdmDataServices metadata,
            EdmProperty prop,
            Object value) {
        // スキーマ定義からComplexType定義を取得する
        EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());

        // ComplexTypeのプロパティを取得する
        List<OProperty<?>> props = getComplexTypePropList(metadata, prop, value);

        return OProperties.complex(prop.getName(), edmComplexType, props);
    }

    /**
     * ComplexType型のプロパティをOPropertyの配列に変換する.
     * @param metadata スキーマ定義
     * @param prop ComplexType型のプロパティ
     * @param value ComplexType型の値
     * @return OPropertyの配列
     */
    @SuppressWarnings("unchecked")
    protected List<OProperty<?>> getComplexTypePropList(EdmDataServices metadata,
            EdmProperty prop,
            Object value) {
        if (value == null) {
            return null;
        }

        List<OProperty<?>> props = new ArrayList<OProperty<?>>();

        // スキーマ定義からComplexType定義を取得する
        EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());

        HashMap<String, Object> valMap = (HashMap<String, Object>) value;
        for (EdmProperty propChild : edmComplexType.getDeclaredProperties()) {
            String typeName = propChild.getDeclaringType().getName();
            Object valChild = valMap.get(resolveComplexTypeAlias(propChild.getName(), typeName));
            EdmType edmType = propChild.getType();
            CollectionKind ck = propChild.getCollectionKind();
            if (edmType.isSimple()) {
                // 非予約項目の処理
                if (ck.equals(CollectionKind.List)) {
                    // 配列要素の場合
                    addSimpleListProperty(props, propChild, valChild, edmType);
                } else {
                    // シンプル要素の場合
                    addSimpleTypeProperty(props, propChild, valChild, edmType);
                }
            } else {
                if (ck.equals(CollectionKind.List)) {
                    // 配列要素の場合
                    addComplexListProperty(metadata, props, propChild, valChild, edmType);
                } else {
                    props.add(createComplexTypeProperty(metadata, propChild, valChild));
                }
            }
        }
        return props;
    }

    /**
     * プロパティ名からAlias名を取得する.
     * ユーザデータを操作する場合は、このメソッドをオーバーライドしてAliasへ変換する.
     * @param propertyName プロパティ名
     * @param typeName このプロパティが属するComplexType名
     * @return Alias名
     */
    protected String resolveComplexTypeAlias(String propertyName, String typeName) {
        return propertyName;
    }

    /**
     * @return etag
     */
    public String createEtag() {
        return this.version + "-" + this.updated;
    }

    static Logger log = LoggerFactory.getLogger(OEntityDocHandler.class);

    /**
     * 登録用データを取得する.
     * @return 登録用データ
     */
    @Override
    public Map<String, Object> getSource() {
        return getCommonSource();
    }

    /**
     * 登録用データを取得する.
     * @return 登録用データ
     */
    protected Map<String, Object> getCommonSource() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put(KEY_STATIC_FIELDS, this.staticFields);
        ret.put(KEY_HIDDEN_FIELDS, this.hiddenFields);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        ret.put(KEY_CELL_ID, this.cellId);
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_ENTITY_ID, this.entityTypeId);
        ret.put(KEY_LINK, this.manyToOnelinkId);
        return ret;
    }

    /**
     * データを特定のCellに紐付けたい時は設定する.
     * @param cellId CellId
     */
    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    /**
     * データを特定のBoxに紐付けたい時は設定する.
     * @param boxId Box Id
     */
    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    /**
     * データを特定のNodeに紐付けたい時は設定する.
     * @param nodeId Node Id
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * entityTypeIdのセッター.
     * @param entityTypeId entityTypeのID
     */
    public void setEntityTypeId(String entityTypeId) {
        this.entityTypeId = entityTypeId;
    }

    /**
     * @param version the version to set
     */
    public final void setVersion(Long version) {
        this.version = version;
    }

    /**
     * manyToOnelinkIdのセッター.
     * @param link manyToOnelinkId
     */
    public final void setManyToOnelinkId(Map<String, Object> link) {
        this.manyToOnelinkId = link;
    }

    /**
     * staticFieldsのセッター.
     * @param staticFields staticFields
     */
    public final void setStaticFields(Map<String, Object> staticFields) {
        this.staticFields = staticFields;
    }

    /**
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public final void setType(String type) {
        this.type = type;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Long getVersion() {
        return this.version;
    }

    /**
     * 更新日をLong形式で返します.
     * @return 更新日のLong形式
     */
    public Long getUpdated() {
        return this.updated;
    }

    /**
     * 作成日をLong形式で返します.
     * @return 作成日のLong形式
     */
    public Long getPublished() {
        return this.published;
    }

    /**
     * $expand指定時の上限値を取得します.
     * @return $expand指定時の上限値
     */
    public int getExpandMaxNum() {
        return expandMaxNum;
    }

    /**
     * $expand指定時の上限値を設定します.
     * @param expandMaxNum $expand指定時の上限値
     */
    public void setExpandMaxNum(int expandMaxNum) {
        this.expandMaxNum = expandMaxNum;
    }

    /**
     * @return Cell Id
     */
    public final String getCellId() {
        return cellId;
    }

    /**
     * @return Box Id
     */
    public final String getBoxId() {
        return boxId;
    }

    /**
     * @return Node Id
     */
    public final String getNodeId() {
        return nodeId;
    }

    /**
     * entityTypeIdのゲッター.
     * @return EntityType Id
     */
    public final String getEntityTypeId() {
        return entityTypeId;
    }

    /**
     * manyToOnelinkIdのゲッター.
     * @return manyToOnelinkId
     */
    public final Map<String, Object> getManyToOnelinkId() {
        return this.manyToOnelinkId;
    }

    /**
     * staticFieldsのゲッター.
     * @return staticFields
     */
    public final Map<String, Object> getStaticFields() {
        return this.staticFields;
    }

    /**
     * UnitUserNameのゲッター.
     * @return UnitUser名
     */
    public final String getUnitUserName() {
        return this.unitUserName;
    }

    /**
     * ES上のODataエンティティ格納においてDeclared Propertyを保存するJSONキー.
     */
    public static final String KEY_STATIC_FIELDS = "s";
    /**
     * ES上のODataエンティティ格納においてDynamic Propertyを保存するJSONキー.
     */
    public static final String KEY_DYNAMIC_FIELDS = "d";
    /**
     * ES上のODataエンティティ格納において隠し項目を保存するJSONキー.
     */
    public static final String KEY_HIDDEN_FIELDS = "h";
    /**
     * ES上のODataエンティティ格納において更新日時を保存するJSONキー.
     */
    public static final String KEY_UPDATED = "u";
    /**
     * ES上のODataエンティティ格納において作成日時を保存するJSONキー.
     */
    public static final String KEY_PUBLISHED = "p";
    /**
     * ES上のODataエンティティ格納においてCellの内部IDを保存するJSONキー.
     */
    public static final String KEY_CELL_ID = "c";
    /**
     * ES上のODataエンティティ格納においてBoxの内部IDを保存するJSONキー.
     */
    public static final String KEY_BOX_ID = "b";
    /**
     * ES上のODataエンティティ格納においてコレクションのnodeidを保存するJSONキー.
     */
    public static final String KEY_NODE_ID = "n";
    /**
     * ES上のODataエンティティ格納においてEntityTypeの内部IDを保存するJSONキー.
     */
    public static final String KEY_ENTITY_ID = "t";
    /**
     * ES上のODataエンティティ格納においてn:1/1:1のLinkの先ドキュメント内部IDを保存するJSONキー.
     */
    public static final String KEY_LINK = "l";
    /**
     * ES上のODataエンティティ格納においてOwnerを保持するJSONをキー.
     */
    public static final String KEY_OWNER = "h.Owner.untouched";

    /**
     * @return ACL設定情報
     */
    public Map<String, JSONObject> getAclFields() {
        return new HashMap<String, JSONObject>();
    }

    /**
     * StaticFieldsの文字列表現を返す.
     * @return StaticFields
     */
    public String getStaticFieldsString() {
        return JSONObject.toJSONString(this.staticFields);
    }

    /**
     * DynamicFieldsの文字列表現を返す.
     * @return DynamicFields
     */
    public String getDynamicFieldsString() {
        return JSONObject.toJSONString(this.dynamicFields);
    }

    /**
     * HiddenFieldsの文字列表現を返す.
     * @return HiddenFields
     */
    public String getHiddenFieldsString() {
        return JSONObject.toJSONString(this.hiddenFields);
    }

    /**
     * ManyToOnelinkIdの文字列表現を返す.
     * @return ManyToOnelinkId
     */
    public String getManyToOnelinkIdString() {
        return JSONObject.toJSONString(this.manyToOnelinkId);
    }
}

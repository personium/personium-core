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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.LocalDateTime;
import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;

import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyAlias;
import io.personium.core.odata.OEntityWrapper;

/**
 * OEntityのDocHandler.
 */
public class UserDataDocHandler extends OEntityDocHandler implements EntitySetDocHandler {

    private Map<String, PropertyAlias> propertyAliasMap;
    private String entitySetName;

    /**
     * コンストラクタ.
     */
    public UserDataDocHandler() {
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * ESの1件取得結果からUserDataDocHandlerのインスタンスを生成するコンストラクタ.
     * @param getResponse .
     */
    public UserDataDocHandler(PersoniumGetResponse getResponse) {
        super(getResponse);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * ESの検索結果からUserDataDocHandlerのインスタンスを生成するコンストラクタ.
     * @param searchHit .
     */
    public UserDataDocHandler(PersoniumSearchHit searchHit) {
        super(searchHit);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * OEntityWrapperから IDのないDocHandlerをつくるConstructor.
     * @param type ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    public UserDataDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        initInstance(type, oEntityWrapper, metadata);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * プロパティ名とエイリアスの対応Mapを返す.
     * @return プロパティ名とエイリアスの対応Map
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return this.propertyAliasMap;
    }

    /**
     * プロパティ名とエイリアスの対応Mapを設定する.
     * @param value プロパティ名とエイリアスの対応Map
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> value) {
        this.propertyAliasMap = value;
    }

    /**
     * エンティティセット名を返す.
     * @return エンティティセット名
     */
    public String getEntitySetName() {
        return this.entitySetName;
    }

    /**
     * エンティティセット名を設定する.
     * @param name エンティティセット名
     */
    public void setEntitySetName(String name) {
        this.entitySetName = name;
    }

    @Override
    protected String getPropertyNameOrAlias(String name, String propertyName) {
        String key = getPropertyMapKey(Property.P_ENTITYTYPE_NAME.getName(), name, propertyName);
        String ret = getAlias(key, propertyName);
        if (propertyName.startsWith("_")) {
            ret = propertyName;
        }
        return ret;
    }

    /**
     * プロパティ名からAlias名を取得する.
     * ユーザデータを操作する場合は、このメソッドをオーバーライドしてAliasへ変換する.
     * @param propertyName プロパティ名
     * @param typeName このプロパティが属するComplexType名
     * @return Alias名
     */
    @Override
    protected String resolveComplexTypeAlias(String propertyName, String typeName) {
        String key = this.getPropertyMapKey(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName(), typeName, propertyName);
        return getAlias(key, propertyName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertAliasToName(EdmDataServices metadata) {
        Map<String, Object> staticFieldMap = new HashMap<String, Object>();
        EdmEntityType edmEntityType = metadata.findEdmEntitySet(entitySetName).getType();
        for (EdmProperty prop : edmEntityType.getProperties()) {
            String key = prop.getName();
            // __id以外のプロパティ(__publishedなど)については変換をしないで捨てる
            if (!CtlSchema.P_ID.getName().equals(key) && key.startsWith("_")) {
                continue;
            }
            boolean isDynamic = false;
            NamespacedAnnotation<?> annotation = prop.findAnnotation(Common.DC_NAMESPACE.getUri(),
                    Property.P_IS_DECLARED.getName());
            if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
                isDynamic = true;
            }
            if (!staticFields.containsKey(getPropertyNameOrAlias(edmEntityType.getName(), key)) && isDynamic) {
                continue;
            }

            Object value = staticFields.get(getPropertyNameOrAlias(edmEntityType.getName(), key));
            if (prop.getType().isSimple() || value == null) {
                // Simple型の場合はそのまま変換をおこなう
                staticFieldMap.put(key, value);
            } else {
                // Complex型の場合は中の要素をさらに変換する
                EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());
                if (CollectionKind.List == prop.getCollectionKind()) {
                    List<Map<String, Object>> complexList = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> val : (List<Map<String, Object>>) value) {
                        Map<String, Object> complexMap = new HashMap<String, Object>();
                        convertAliasToNameForComplex(metadata,
                                edmComplexType,
                                (Map<String, Object>) val,
                                complexMap);
                        complexList.add(complexMap);
                    }
                    staticFieldMap.put(key, complexList);
                } else {
                    Map<String, Object> complexMap = new HashMap<String, Object>();
                    convertAliasToNameForComplex(metadata,
                            edmComplexType,
                            (Map<String, Object>) value,
                            complexMap);
                    staticFieldMap.put(key, complexMap);
                }
            }
        }
        this.staticFields = staticFieldMap;
    }

    @SuppressWarnings("unchecked")
    private void convertAliasToNameForComplex(EdmDataServices metadata,
            EdmComplexType edmComplexType,
            Map<String, Object> baseMap,
            Map<String, Object> complexMap) {
        for (EdmProperty prop : edmComplexType.getProperties()) {
            String key = prop.getName();
            Object value = baseMap.get(resolveComplexTypeAlias(key, edmComplexType.getName()));
            if (prop.getType().isSimple() || value == null) {
                complexMap.put(key, value);
            } else {
                // Complex型の場合は中の要素をさらに変換する
                if (CollectionKind.List == prop.getCollectionKind()) {
                    List<Map<String, Object>> complexList = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> val : (List<Map<String, Object>>) value) {
                        Map<String, Object> newComplexMap = new HashMap<String, Object>();
                        convertAliasToNameForComplex(metadata,
                                metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName()),
                                (Map<String, Object>) val,
                                newComplexMap);
                        complexList.add(newComplexMap);
                    }
                    complexMap.put(key, complexList);

                } else {
                    Map<String, Object> newComplexMap = new HashMap<String, Object>();
                    convertAliasToNameForComplex(metadata,
                            metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName()),
                            (Map<String, Object>) value, newComplexMap);
                    complexMap.put(key, newComplexMap);
                }
            }
        }
    }

    /**
     * スキーマのプロパティ定義に応じて適切な型に変換したプロパティ値オブジェクトを返す.<br/>
     * ユーザデータの場合はBoolean型のプロパティ値を文字列に変換する.
     * @param prop プロパティオブジェクト
     * @param edmType スキーマのプロパティ定義
     * @return 適切な型に変換したプロパティ値オブジェクト
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Object getSimpleValue(OProperty<?> prop, EdmType edmType) {
        if (edmType.equals(EdmSimpleType.DATETIME)) {
            OProperty<LocalDateTime> propD = (OProperty<LocalDateTime>) prop;
            LocalDateTime ldt = propD.getValue();
            if (ldt != null) {
                return ldt.toDateTime().getMillis();
            }
        }

        // Boolean型/Double型のプロパティ値を文字列に変換する
        if (prop.getValue() != null
                && (edmType.equals(EdmSimpleType.BOOLEAN) || edmType.equals(EdmSimpleType.DOUBLE))) {
            return String.valueOf(prop.getValue());
        }
        return prop.getValue();
    }

    /**
     * DynamicPropertyの値を変換する.
     * @param key 変換対象のキー
     * @param value 変換する値
     */
    public void convertDynamicPropertyValue(String key, Object value) {
        if (value != null && (value instanceof Boolean || value instanceof Double)) {
            this.getDynamicFields().put(key, String.valueOf(value));
        }
    }

    @Override
    protected Object convertSimpleListValue(final EdmType edmType, Object propValue) {
        Object convertValue = super.convertSimpleListValue(edmType, propValue);
        // Boolean型/Double型のプロパティ値を文字列に変換する
        if (propValue != null && (edmType.equals(EdmSimpleType.BOOLEAN) || edmType.equals(EdmSimpleType.DOUBLE))) {
            return String.valueOf(propValue);
        }
        return convertValue;
    }

    @Override
    public Map<String, Object> getSource() {
        Map<String, Object> ret = new HashMap<String, Object>();
        if (this.dynamicFields != null) {
            this.staticFields.putAll(this.dynamicFields);
            this.dynamicFields = new HashMap<String, Object>();
        }
        Set<Entry<String, Object>> entrySet = this.staticFields.entrySet();
        Map<String, Object> staticFieldMap =
                getStaticFieldMap(Property.P_ENTITYTYPE_NAME.getName(), this.entitySetName, entrySet);
        this.staticFields = staticFieldMap;
        ret.put(KEY_STATIC_FIELDS, this.staticFields);
        ret.put(KEY_HIDDEN_FIELDS, this.hiddenFields);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        ret.put(KEY_CELL_ID, this.cellId);
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_ENTITY_ID, this.entityTypeId);

        // UserDataはリンクフィールドに文字列配列形式("EntityTypeID:UserDataID")でデータを保持するため、文字列配列に変換する
        List<String> linkList = toArrayManyToOnelink();
        ret.put("ll", linkList);
        return ret;
    }

    private List<String> toArrayManyToOnelink() {
        List<String> linkList = new ArrayList<String>();
        for (Entry<String, Object> entry : this.manyToOnelinkId.entrySet()) {
            linkList.add(entry.getKey() + ":" + entry.getValue());
        }
        return linkList;
    }

    /**
     * Linkフィールドをパースする.
     * @param source Map形式のパース元情報
     * @return Link情報
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseLinks(Map<String, Object> source) {
        // UserDataはリンクフィールドに文字列配列形式("EntityTypeID:UserDataID")でデータを保持しているため、Map形式に変換する
        Map<String, Object> linkMap = new HashMap<String, Object>();
        List<String>  linkList = (List<String>) source.get("ll");
        if (linkList != null) {
            for (String link : linkList) {
                String[] tmp = link.split(":");
                linkMap.put(tmp[0], tmp[1]);
            }
        }
        return linkMap;
    }

    @Override
    public String getManyToOnelinkIdString() {
        List<String> newList = new ArrayList<String>();
        List<String> linkList = toArrayManyToOnelink();
        for (String element : linkList) {
            newList.add("\"" + element + "\"");
        }
        return newList.toString();
    }

    /**
     * static fieldに格納されているプロパティ名をAliasへ変換する.
     * @param entityType プロパティが紐付いているエンティティタイプ（EntityType or ComplexType)
     * @param entityName プロパティが紐付いているエンティティ名
     * @param entrySet static fieldに格納されているマップオブジェクト
     * @return static fieldマップオブジェクト
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getStaticFieldMap(
            String entityType,
            String entityName,
            Set<Entry<String, Object>> entrySet) {

        Map<String, Object> ret = new HashMap<String, Object>();

        for (Map.Entry<String, Object> entry : entrySet) {
            String propertyName = entry.getKey();
            String key = getPropertyMapKey(entityType, entityName, propertyName);
            String alias = getAlias(key, propertyName);
            if (alias != null) {
                Object value = entry.getValue();
                if (alias.startsWith("C") && value instanceof Map<?, ?>) {
                    Map<String, Object> childrenEntryMap = (Map<String, Object>) value;
                    String complexTypeName = getComplexTypeName(key);
                    Map<String, Object> fields = getStaticFieldMap(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName(),
                            complexTypeName, childrenEntryMap.entrySet());
                    ret.put(alias, fields);
                } else if (alias.startsWith("C") && value instanceof List<?>) {
                    List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
                    for (Map<String, Object> item : ((List<Map<String, Object>>) value)) {
                        String complexTypeName = getComplexTypeName(key);
                        Map<String, Object> fields = getStaticFieldMap(
                                ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName(),
                                complexTypeName, item.entrySet());
                        items.add(fields);
                    }
                    ret.put(alias, items);
                } else {
                    if (entry.getValue() != null && (entry.getValue() instanceof Boolean
                            || entry.getValue() instanceof Double)) {
                        ret.put(alias, String.valueOf(entry.getValue()));
                    } else {
                        ret.put(alias, entry.getValue());
                    }
                }
            }
        }
        return ret;
    }

    /**
     * プロパティのAlias名をマッピングデータから取得する.
     * @param key マッピングデータの検索キー
     * @param propertyName プロパティ名
     * @return Alias名
     */
    private String getAlias(String key, String propertyName) {
        if (propertyName.startsWith("_")) {
            return propertyName;
        }
        PropertyAlias alias = this.propertyAliasMap.get(key);
        if (alias != null) {
            return alias.getAlias();
        }
        return null;
    }

    /**
     * プロパティのtype属性値に指定されたComplexType名をマッピングデータから取得する.
     * @param key マッピングデータの検索キー
     * @param propertyName プロパティ名
     * @return ComplexType名
     */
    private String getComplexTypeName(String key) {
        PropertyAlias alias = this.propertyAliasMap.get(key);
        if (alias != null && alias.getAlias().startsWith("C")) {
            return alias.getPropertyType();
        }
        return null;
    }

    private String getPropertyMapKey(String entityType, String name, String propertyName) {
        // このメソッドが呼ばれる時点は、EntityType/ComplexTypeの判別が不可能なため、
        // プロパティAliasのキーは、一律"_EntityType.Name"を使用する。
        String key = "Name='" + propertyName + "'," + entityType + "='" + name + "'";
        return key;
    }
}

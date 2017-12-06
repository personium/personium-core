/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * --------------------------------------------------
 * This code is based on JsonFormatParser.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import java.util.ArrayList;
import java.util.List;

import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.format.Settings;
import org.odata4j.format.json.JsonTypeConverter;
import org.odata4j.producer.edm.Edm;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.PersoniumJsonFeedFormatParser.JsonEntry;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader;
import io.personium.core.utils.ODataUtils;

/**
 * JsonFormatParser.
 */
public class PersoniumJsonFormatParser {

    /** __metadata. */
    protected static final String METADATA_PROPERTY = "__metadata";
    /** __deferred. */
    protected static final String DEFERRED_PROPERTY = "__deferred";
    /** __next. */
    protected static final String NEXT_PROPERTY = "__next";
    /** __count. */
    protected static final String COUNT_PROPERTY = "__count";

    /** uri. */
    protected static final String URI_PROPERTY = "uri";
    /** type. */
    protected static final String TYPE_PROPERTY = "type";
    /** etag. */
    protected static final String ETAG_PROPERTY = "etag";
    /** results. */
    protected static final String RESULTS_PROPERTY = "results";
    /** d. */
    protected static final String DATA_PROPERTY = "d";

    /** version. */
    private ODataVersion version;
    /** metadata. */
    private EdmDataServices metadata;
    /** entitySetName. */
    private String entitySetName;
    /** entityKey. */
    private OEntityKey entityKey;
    /** リクエスト時の時刻. */
    private long currentTimeMillis = System.currentTimeMillis();

    /**
     * ODataVersionのゲッター.
     * @return ODataVersion
     */
    public ODataVersion getVersion() {
        return version;
    }

    /**
     * Metadataのゲッター.
     * @return EdmDataServices
     */
    public EdmDataServices getMetadata() {
        return metadata;
    }

    /**
     * Metadataのセッター.
     * @param metadata スキーマ情報
     */
    public void setMetadata(EdmDataServices metadata) {
        this.metadata = metadata;
    }

    /**
     * entitySetNameのゲッター.
     * @return String
     */
    public String getEntitySetName() {
        return entitySetName;
    }

    /**
     * entityKeyのゲッター.
     * @return OEntityKey
     */
    public OEntityKey getEntityKey() {
        return entityKey;
    }

    /**
     * コンストラクタ.
     * @param settings セッティング情報
     */
    protected PersoniumJsonFormatParser(Settings settings) {
        if (settings != null) {
            this.version = settings.version;
            this.metadata = settings.metadata;
            this.entitySetName = settings.entitySetName;
            this.entityKey = settings.entityKey;

        }
    }

    /**
     * ネストデータオブジェクト.
     */
    static class JsonObjectPropertyValue {
        OComplexObject complexObject;
        OCollection<? extends OObject> collection;
        EdmCollectionType collectionType;
    }

    /**
     * JsonEntryをパースする.
     * @param ees EdmEntitySet
     * @param jsr JsonStreamReader
     * @return JsonEntry
     */
    protected JsonEntry parseEntry(EdmEntitySet ees, JsonStreamReader jsr) {
        JsonEntry entry = new JsonEntry(ees);
        entry.properties = new ArrayList<OProperty<?>>();
        entry.links = new ArrayList<OLink>();

        String name = "";
        while (jsr.hasNext()) {
            JsonEvent event = jsr.nextEvent();

            if (event.isStartProperty()) {
                try {
                    name = event.asStartProperty().getName();
                    ees = addProperty(entry, ees, name, jsr);
                } catch (IllegalArgumentException e) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name).reason(e);
                }
            } else if (event.isEndObject()) {
                break;
            }
        }

        entry.oentity = toOEntity(ees, entry.getEntityType(), entry.getEntityKey(), entry.properties, entry.links);
        return entry;
    }

    /**
     * OEntityへ変換する.
     * @param entitySet エンティティセット
     * @param entityType エンティティタイプ
     * @param key キー
     * @param properties プロパティ
     * @param links リンク情報
     * @return OEntity
     */
    private OEntity toOEntity(EdmEntitySet entitySet,
            EdmEntityType entityType,
            OEntityKey key,
            List<OProperty<?>> properties,
            List<OLink> links) {

        // key is what we pulled out of the _metadata, use it first.
        if (key != null) {
            return OEntities.create(entitySet, entityType, key, properties, links);
        }

        if (entityKey != null) {
            return OEntities.create(entitySet, entityType, entityKey, properties, links);
        }

        return OEntities.createRequest(entitySet, properties, links);
    }

    /**
     * adds the property. This property can be a navigation property too. In this
     * case a link will be added. If it's the meta data the information will be
     * added to the entry too.
     * @param entry JsonEntry
     * @param ees EdmEntitySet
     * @param name PropertyName
     * @param jsr JsonStreamReader
     * @return EdmEntitySet
     */
    protected EdmEntitySet addProperty(JsonEntry entry, EdmEntitySet ees, String name, JsonStreamReader jsr) {

        JsonEvent event = jsr.nextEvent();

        if (event.isEndProperty()) {
            // scalar property
            EdmProperty ep = entry.getEntityType().findProperty(name);
            if (ep == null) {
                // OpenEntityTypeの場合は、プロパティを追加する
                NamespacedAnnotation<?> openType = findAnnotation(ees.getType(), null, Edm.EntityType.OpenType);
                if (openType != null && openType.getValue() == "true") {
                    Object propValue = null;
                    try {
                        propValue = event.asEndProperty().getObject();
                    } catch (NumberFormatException e) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name).reason(e);
                    }

                    // 型によって登録するEntityPropertyを変更する
                    if (propValue instanceof Boolean) {
                        entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) EdmSimpleType.BOOLEAN,
                                propValue.toString()));
                    } else if (propValue instanceof Double) {
                        entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) EdmSimpleType.DOUBLE,
                                propValue.toString()));
                    } else {
                        if (propValue == null) {
                            entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) EdmSimpleType.STRING,
                                    null));
                        } else {
                            entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) EdmSimpleType.STRING,
                                    propValue.toString()));
                        }
                    }
                } else {
                    throw PersoniumCoreException.OData.FIELED_INVALID_ERROR.params("unknown property " + name + " for "
                            + entry.getEntityType().getName());
                }
            } else {
                // StaticPropertyの値チェック
                String propValue = event.asEndProperty().getValue();
                if (propValue != null) {
                    EdmType type = ep.getType();
                    if (type.equals(EdmSimpleType.BOOLEAN)
                            && !ODataUtils.validateBoolean(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (type.equals(EdmSimpleType.STRING)
                            && !ODataUtils.validateString(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (type.equals(EdmSimpleType.DATETIME)) {
                        if (!ODataUtils.validateDateTime(propValue)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                        }
                        if (Common.SYSUTCDATETIME.equals(propValue)) {
                            String crrTime = String.valueOf(getCurrentTimeMillis());
                            propValue = String.format("/Date(%s)/", crrTime);
                        }
                    } else if (type.equals(EdmSimpleType.SINGLE) && !ODataUtils.validateSingle(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (type.equals(EdmSimpleType.INT32) && !ODataUtils.validateInt32(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    } else if (type.equals(EdmSimpleType.DOUBLE) && !ODataUtils.validateDouble(propValue)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    }
                }
                if (ep.getType().isSimple()) {
                    // シンプル型（文字列や数値など）であればプロパティに追加する
                    entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) ep.getType(), propValue));
                } else {
                    if (propValue == null) {
                        // ComplexType型で、値がnullの場合はエラーにしない
                        entry.properties.add(JsonTypeConverter.parse(name,
                                (EdmSimpleType<?>) EdmSimpleType.STRING, null));
                    } else {
                        // ComplexType型で、ComplexType型以外の値が指定された場合("aaa")はエラーとする
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    }
                }
            }
        } else if (event.isStartObject()) {
            // JSONオブジェクトの場合は値を取得する
            JsonObjectPropertyValue val = getValue(event, ees, name, jsr, entry);

            if (val.complexObject != null) {
                // ComplexTypeデータであればプロパティに追加する
                entry.properties.add(OProperties.complex(name, (EdmComplexType) val.complexObject.getType(),
                        val.complexObject.getProperties()));
            } else {
                // ComplexTypeデータ以外はエラーとする
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        } else if (event.isStartArray()) {
            // 配列オブジェクトの場合
            JsonObjectPropertyValue val = new JsonObjectPropertyValue();

            // スキーマ定義が存在してCollectionKindがNoneでなければ、配列としてパースする
            EdmProperty eprop = entry.getEntityType().findProperty(name);
            if (null != eprop && eprop.getCollectionKind() != CollectionKind.NONE) {
                val.collectionType = new EdmCollectionType(eprop.getCollectionKind(), eprop.getType());
                PersoniumJsonCollectionFormatParser cfp = new PersoniumJsonCollectionFormatParser(val.collectionType,
                        this.metadata, name);
                val.collection = cfp.parseCollection(jsr);
            }

            // パースに成功した場合は、プロパティに追加する
            if (val.collectionType != null && val.collection != null) {
                entry.properties.add(OProperties.collection(name, val.collectionType, val.collection));
            } else {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        } else {
            throw PersoniumCoreException.OData.INVALID_TYPE_ERROR.params(name);
        }
        return ees;
    }

    /**
     * JSONオブジェクトの値を取得する.
     * @param event JsonEvent
     * @param ees エンティティセット型
     * @param name プロパティ名
     * @param jsr JsonStreamReader
     * @param entry JsonEntry
     * @return JsonObjectPropertyValue
     */
    protected JsonObjectPropertyValue getValue(JsonEvent event,
            EdmEntitySet ees,
            String name,
            JsonStreamReader jsr,
            JsonEntry entry) {
        JsonObjectPropertyValue rt = new JsonObjectPropertyValue();

        ensureStartObject(event);
        event = jsr.nextEvent();
        ensureStartProperty(event);

        // ComplexObjectであればエンティティタイプ定義からプロパティ定義を取得する
        EdmProperty eprop = entry.getEntityType().findProperty(name);

        if (eprop == null) {
            // プロパティがスキーマ定義上に存在しなければエラーとする
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
        } else {
            // スキーマ定義からComplexType定義を取得する
            EdmComplexType ct = metadata.findEdmComplexType(eprop.getType().getFullyQualifiedTypeName());

            if (null != ct) {
                // ComplexTypeが存在する場合は、パースを実施してComplexTypeObjectを取得する
                Settings s = new Settings(version, metadata, entitySetName, entityKey, null, false, ct);
                PersoniumJsonComplexObjectFormatParser cofp = new PersoniumJsonComplexObjectFormatParser(s);
                rt.complexObject = cofp.parseSingleObject(jsr, event);
            } else {
                // ComplexTypeがスキーマ定義上に存在しなければエラーとする
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        }

        ensureEndProperty(jsr.nextEvent());
        return rt;
    }

    /**
     * 指定したアノテーションを取得する.
     * @param type EdmType
     * @param namespaceUri 名前空間
     * @param localName 取得対象名
     * @return namespaceUri
     */
    private NamespacedAnnotation<?> findAnnotation(EdmType type, String namespaceUri, String localName) {
        if (type != null) {
            for (NamespacedAnnotation<?> annotation : type.getAnnotations()) {
                if (localName.equals(annotation.getName())) {
                    String uri = annotation.getNamespace().getUri();
                    if ((namespaceUri == null && uri == null) //NOPMD -To maintain readability
                            || (namespaceUri != null && namespaceUri.equals(uri))) { //NOPMD
                        return annotation;
                    }
                }
            }
        }
        return null;
    }

    /**
     * ensureNext.
     * @param jsr JsonStreamReader
     */
    protected void ensureNext(JsonStreamReader jsr) {
        if (!jsr.hasNext()) {
            throw new IllegalArgumentException("no valid JSON format exepected at least one more event");
        }
    }

    /**
     * ensureStartObject.
     * @param event JsonEvent
     */
    protected void ensureStartObject(JsonEvent event) {
        if (!event.isStartObject()) {
            throw new IllegalArgumentException("no valid OData JSON format expected StartObject got " + event + ")");
        }
    }

    /**
     * ensureStartProperty.
     * @param event JsonEvent
     */
    protected void ensureStartProperty(JsonEvent event) {
        if (!event.isStartProperty()) {
            throw new IllegalArgumentException("no valid OData JSON format (expected StartProperty got " + event + ")");
        }
    }

    /**
     * ensureEndProperty.
     * @param event JsonEvent
     */
    protected void ensureEndProperty(JsonEvent event) {
        if (!event.isEndProperty()) {
            throw new IllegalArgumentException("no valid OData JSON format (expected EndProperty got " + event + ")");
        }
    }

    /**
     * ensureEndArray.
     * @param event JsonEvent
     */
    protected void ensureEndArray(JsonEvent event) {
        if (!event.isEndArray()) {
            throw new IllegalArgumentException("no valid OData JSON format expected EndArray got " + event + ")");
        }
    }

    /**
     * 現在時刻を取得する.
     * @return the currentTimeMillis
     */
    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }
}

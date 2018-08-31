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
    /** Time of request.*/
    private long currentTimeMillis = System.currentTimeMillis();

    /**
     *Getter of ODataVersion.
     * @return ODataVersion
     */
    public ODataVersion getVersion() {
        return version;
    }

    /**
     *Getader of Metadata.
     * @return EdmDataServices
     */
    public EdmDataServices getMetadata() {
        return metadata;
    }

    /**
     *Metadata's setter.
     *@ param metadata schema information
     */
    public void setMetadata(EdmDataServices metadata) {
        this.metadata = metadata;
    }

    /**
     *Getter of entitySetName.
     * @return String
     */
    public String getEntitySetName() {
        return entitySetName;
    }

    /**
     *Getter of entityKey.
     * @return OEntityKey
     */
    public OEntityKey getEntityKey() {
        return entityKey;
    }

    /**
     *constructor.
     *@ param settings setting information
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
     *Nested data object.
     */
    static class JsonObjectPropertyValue {
        OComplexObject complexObject;
        OCollection<? extends OObject> collection;
        EdmCollectionType collectionType;
    }

    /**
     *Parsing JsonEntry.
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
     *Convert to OEntity.
     *@ param entitySet entity set
     *@ param entityType entity type
     *@ param key
     *@ param properties
     *@ param links link information
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
                //For OpenEntityType, add properties
                NamespacedAnnotation<?> openType = findAnnotation(ees.getType(), null, Edm.EntityType.OpenType);
                if (openType != null && openType.getValue() == "true") {
                    Object propValue = null;
                    try {
                        propValue = event.asEndProperty().getObject();
                    } catch (NumberFormatException e) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name).reason(e);
                    }

                    //Change EntityProperty to register by type
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
                //StaticProperty value check
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
                    //If it is a simple type (character string, number, etc.), it is added to the property
                    entry.properties.add(JsonTypeConverter.parse(name, (EdmSimpleType<?>) ep.getType(), propValue));
                } else {
                    if (propValue == null) {
                        //If ComplexType type and value is null, do not set it to error
                        entry.properties.add(JsonTypeConverter.parse(name,
                                (EdmSimpleType<?>) EdmSimpleType.STRING, null));
                    } else {
                        //If ComplexType type and a value other than ComplexType type is specified ("aaa"), it is an error
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
                    }
                }
            }
        } else if (event.isStartObject()) {
            //In case of JSON object, get value
            JsonObjectPropertyValue val = getValue(event, ees, name, jsr, entry);

            if (val.complexObject != null) {
                //If it is ComplexType data, it is added to the property
                entry.properties.add(OProperties.complex(name, (EdmComplexType) val.complexObject.getType(),
                        val.complexObject.getProperties()));
            } else {
                //Make errors other than ComplexType data
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        } else if (event.isStartArray()) {
            //For array objects
            JsonObjectPropertyValue val = new JsonObjectPropertyValue();

            //If the schema definition exists and CollectionKind is not None, parse it as an array
            EdmProperty eprop = entry.getEntityType().findProperty(name);
            if (null != eprop && eprop.getCollectionKind() != CollectionKind.NONE) {
                val.collectionType = new EdmCollectionType(eprop.getCollectionKind(), eprop.getType());
                PersoniumJsonCollectionFormatParser cfp = new PersoniumJsonCollectionFormatParser(val.collectionType,
                        this.metadata, name);
                val.collection = cfp.parseCollection(jsr);
            }

            //If parsing succeeds, add it to the property
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
     *Get the value of the JSON object.
     * @param event JsonEvent
     *@ param ees entity set type
     *@ param name property name
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

        //If it is a ComplexObject, it acquires the property definition from the entity type definition
        EdmProperty eprop = entry.getEntityType().findProperty(name);

        if (eprop == null) {
            //If the property does not exist on the schema definition, it is set as an error
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
        } else {
            //Get ComplexType definition from schema definition
            EdmComplexType ct = metadata.findEdmComplexType(eprop.getType().getFullyQualifiedTypeName());

            if (null != ct) {
                //If there is a ComplexType, execute a parse and acquire a ComplexTypeObject
                Settings s = new Settings(version, metadata, entitySetName, entityKey, null, false, ct);
                PersoniumJsonComplexObjectFormatParser cofp = new PersoniumJsonComplexObjectFormatParser(s);
                rt.complexObject = cofp.parseSingleObject(jsr, event);
            } else {
                //If ComplexType does not exist on the schema definition, it is regarded as an error
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(name);
            }
        }

        ensureEndProperty(jsr.nextEvent());
        return rt;
    }

    /**
     *Gets the specified annotation.
     * @param type EdmType
     *@ param namespaceUri namespace
     *@ param localName Name of acquisition target
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
     *Get the current time.
     * @return the currentTimeMillis
     */
    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }
}

/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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

import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchHitField;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Property;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;

/**
 * DocHandler of OEntity.
 */
public class OEntityDocHandler implements EntitySetDocHandler {
    //ES type
    String type;
    //ES's id
    String id;
    //ES version
    Long version;
    //UnitUser name
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
     * constructor.
     */
    public OEntityDocHandler() {
    }

    /**
     * @return Map of Dynamic Field
     */
    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    /**
     * @param dynamicFields Map of Dynamic Field
     */
    public void setDynamicFields(Map<String, Object> dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    /**
     * @return Hidden Field Map
     */
    public Map<String, Object> getHiddenFields() {
        return hiddenFields;
    }

    /**
     * @param hiddenFields Hidden Field Map
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
    public OEntityDocHandler(PersoniumGetResponse getResponse) {
        this.type = getResponse.getType();
        this.id = getResponse.getId();
        this.version = getResponse.getVersion();
        Map<String, Object> source = getResponse.getSource();
        this.parseSource(source);
        this.resolveUnitUserName();
    }

    /**
     * Parse Source in Map format and map to itself.
     * @param source mapping format information for mapping
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
     * Parsing Link field.
     * @param source parse source information in the form of Map
     * @return Link information
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseLinks(Map<String, Object> source) {
        return (Map<String, Object>) source.get(KEY_LINK);
    }

    void parseFields(Map<String, PersoniumSearchHitField> fields) {
        for (Map.Entry<String, PersoniumSearchHitField> ent : fields.entrySet()) {
            String key = ent.getKey();
            PersoniumSearchHitField value = ent.getValue();
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
    public OEntityDocHandler(PersoniumSearchHit searchHit) {
        this.type = searchHit.getType();
        this.id = searchHit.getId();
        this.version = searchHit.getVersion();
        Map<String, Object> source = searchHit.getSource();
        if (source != null) {
            this.parseSource(source);
        }
//        Map<String, PersoniumSearchHitField> fields = searchHit.getFields();
//        if (fields.size() > 0) {
//            this.parseFields(fields);
//        }
        this.resolveUnitUserName();
    }

    /**
     * Constructor that creates DocHandler without ID from OEntityWrapper.
     * @param type ES type name
     * @param oEntityWrapper OEntityWrapper
     * @param metadata schema information
     */
    public OEntityDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        initInstance(type, oEntityWrapper, metadata);
    }

    /**
     * Create a DocHandler without ID from OEntityWrapper.
     * @param typeName ES type name
     * @param oEntityWrapper OEntityWrapper
     * @param metadata schema information
     */
    @SuppressWarnings("unchecked")
    protected void initInstance(String typeName, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        this.type = typeName;
        //Set the specified uuid as ES ID
        this.id = oEntityWrapper.getUuid();
        //When OEntity Wrapper is used, add Version, hidden field
        this.hiddenFields = oEntityWrapper.getMetadata();
        //Set UnitUser name only when accessing Cell
        resolveUnitUserName();
        String etag = oEntityWrapper.getEtag();
        if (etag != null && etag.length() > 1) {
            this.version = Long.valueOf(etag.substring(0, etag.indexOf("-")));
        }

        //Retrieve schema information
        EdmEntitySet entitySet = oEntityWrapper.getEntitySet();
        EdmEntityType eType = entitySet.getType();

        //Get the NavProp defined in the schema
        List<EdmNavigationProperty> navProps = eType.getDeclaredNavigationProperties().toList();
        for (EdmNavigationProperty np : navProps) {
            //About each NavProp
            EdmMultiplicity mf = np.getFromRole().getMultiplicity();
            EdmMultiplicity mt = np.getToRole().getMultiplicity();

            //As Association's Multiplicity, when this side is MANY and the opponent is ONE,
            //From the value (URL) of NavigationProperty, the item of l is determined and packed
            if (EdmMultiplicity.ONE.equals(mt) && EdmMultiplicity.MANY.equals(mf)) {
                //TODO not implemented yet
                log.debug("many to one");

            }
        }

        //For all properties that came up,
        for (OProperty<?> prop : oEntityWrapper.getProperties()) {
            //Checks whether it is defined in the schema, and switches processing between Dynamic Property and Declared Property
            String propName = prop.getName();
            EdmProperty edmProperty = eType.findProperty(propName);

            //Branch processing by predefined Property or DynamicProperty
            if (edmProperty != null) {
                //Schema-defined properties
                //TODO It is unknown whether to do here, but check whether data corresponding to the type defined in the schema is coming
                //You should do it earlier.

                //Refill the values ​​for each type.
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
                            //Error if invalid type
                            if (value == null || value instanceof OCollection<?>) {
                                this.staticFields.put(prop.getName(),
                                        getSimpleList(edmProperty.getType(), (OCollection<OObject>) value));
                            } else {
                                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(prop.getName());
                            }
                        } else {
                            value = getSimpleValue(prop, edmProperty.getType());

                            //If Property is Simple type, add it as defined item as it is
                            this.staticFields.put(prop.getName(), value);
                        }
                    } else {
                        String complexTypeName = edmProperty.getType().getFullyQualifiedTypeName();
                        if (ck.equals(CollectionKind.List)) {
                            //If CollectionKind is List, add by array
                            this.staticFields.put(
                                    prop.getName(),
                                    getComplexList((OCollection<OComplexObject>) prop.getValue(), metadata,
                                            complexTypeName));
                        } else {
                            //If Property is Complex type, read and add ComplexType property recursively
                            this.staticFields.put(prop.getName(), getComplexType(prop, metadata, complexTypeName));
                        }
                    }
                }
            } else {
                //Dynamic property receives String, Integer, Float, Boolean
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
     * Returns a property value object converted to an appropriate type according to the property definition of the schema.
     * @param prop property object
     * @param edmType Property definition for schema
     * @return Property value object converted to appropriate type
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
     * Set UnitUser name when accessing Cell.
     * @param hiddenFieldsMap map object of hiddenFields
     */
    public void resolveUnitUserName(final Map<String, Object> hiddenFieldsMap) {
        if (hiddenFieldsMap == null) {
            return;
        }
        //Set UnitUser name for Ads access
        String owner = (String) hiddenFieldsMap.get("Owner");
        this.unitUserName = PersoniumUnitConfig.getEsUnitPrefix() + "_";
        if (owner == null) {
            this.unitUserName += AccessContext.TYPE_ANONYMOUS;
        } else {
            this.unitUserName += IndexNameEncoder.encodeEsIndexName(owner);
        }
    }

    /**
     * Set UnitUser name when accessing Cell.
     */
    private void resolveUnitUserName() {
        this.resolveUnitUserName(this.hiddenFields);
    }

    /**
     * Get an array of SimpleType.
     * @param edmType EdmSimpleType
     * @param value OCollection
     * @return An array of SimpleType
     */
    protected List<Object> getSimpleList(final EdmType edmType, final OCollection<OObject> value) {
        if (value == null) {
            return null;
        }

        //If CollectionKind is List, add by array
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
     * Convert one of the data in the array.
     * @param edmType EdmSimpleType
     * @param propValue Value to be converted
     * @return Value after conversion
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
     * Get an array of ComplexType.
     * @param value OCollection
     * @param metadata schema information
     * @param complexTypeName Complex type name
     * @return An array of SimpleType
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
     * Read and acquire properties of ComplexType.
     * @param property
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object getComplexType(OProperty<?> property, EdmDataServices metadata, String complexTypeName) {
        return getComplexType((List<OProperty<?>>) property.getValue(), metadata, complexTypeName);
    }

    /**
     * Read and acquire properties of ComplexType.
     * @param property Property List
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object getComplexType(List<OProperty<?>> props, EdmDataServices metadata, String complexTypeName) {
        //Retrieve the value of the specified Property
        Map<String, Object> complex = new HashMap<String, Object>();

        if (props == null) {
            return null;
        }

        //Add Property of ComplexType to Hash
        for (OProperty<?> prop : props) {
            EdmProperty edmProp = metadata.findEdmComplexType(complexTypeName).findProperty(prop.getName());
            CollectionKind ck = edmProp.getCollectionKind();
            if (edmProp.getType().isSimple()) {
                if (ck.equals(CollectionKind.List)) {
                    //Error if invalid type
                    if (prop.getValue() == null || prop.getValue() instanceof OCollection<?>) {
                        complex.put(prop.getName(),
                                getSimpleList(edmProp.getType(), (OCollection<OObject>) prop.getValue()));
                    } else {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(prop.getName());
                    }
                } else {
                    Object value = getSimpleValue(prop, edmProp.getType());
                    //If Property is Simple type, add it as defined item as it is
                    complex.put(prop.getName(), value);
                }
            } else {
                //If Property is Complex type, read and add ComplexType property recursively
                String propComplexTypeName = edmProp.getType().getFullyQualifiedTypeName();
                if (ck.equals(CollectionKind.List)) {
                    //If CollectionKind is List, add by array
                    complex.put(
                            prop.getName(),
                            getComplexList((OCollection<OComplexObject>) prop.getValue(),
                                    metadata, propComplexTypeName));
                } else {
                    //If Property is Complex type, read and add ComplexType property recursively
                    complex.put(prop.getName(),
                            getComplexType(prop, metadata, propComplexTypeName));
                }

            }
        }
        return complex;
    }

    /**
     * Creates and returns an OEntity from the Es JSON object.
     * @param entitySet entitySet
     * @return Converted OEntity object
     */
    public OEntityWrapper createOEntity(final EdmEntitySet entitySet) {
        return createOEntity(entitySet, null, null);
    }

    /**
     * Creates and returns an OEntity from the Es JSON object.
     * @param entitySet entitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @return Converted OEntity object
     */
    public OEntityWrapper createOEntity(final EdmEntitySet entitySet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList) {
        return createOEntity(entitySet, null, null, null);
    }

    /**
     * Return alias corresponding to property name or property name depending on the type of document handler.
     * @param entitySetName Entity set name
     * @param propertyName property name
     * @return Alias ​​corresponding to property name or property name
     */
    protected String getPropertyNameOrAlias(String entitySetName, String propertyName) {
        return propertyName;
    }

    @Override
    public void convertAliasToName(EdmDataServices metadata) {
    }

    /**
     * Creates and returns an OEntity from the Es JSON object.
     * @param entitySet entitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @param selectQuery $ select query
     * @return Converted OEntity object
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

        //Generate Declared Property according to schema
        for (EdmProperty prop : eType.getProperties()) {
            //Processing of reserved items
            if ("__published".equals(prop.getName()) && this.published != null) {
                properties.add(OProperties.datetime(prop.getName(), new LocalDateTime(this.published)));
                continue;
            } else if ("__updated".equals(prop.getName()) && this.updated != null) {
                properties.add(OProperties.datetime(prop.getName(), new LocalDateTime(this.updated)));
                continue;
            }

            //When a $ select query is specified, only the specified item or primary key information is added
            if (selectMap.size() != 0 && !selectMap.containsKey(prop.getName())
                    && !eType.getKeys().contains(prop.getName())) {
                continue;
            }

            boolean isDynamic = false;
            NamespacedAnnotation<?> annotation = prop.findAnnotation(Common.P_NAMESPACE.getUri(),
                    Property.P_IS_DECLARED.getName());
            if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
                isDynamic = true;
            }

            //Get value
            if (!this.staticFields.containsKey(getPropertyNameOrAlias(entitySet.getName(), prop.getName()))
                    && isDynamic) {
                continue;
            }
            Object valO = this.staticFields.get(getPropertyNameOrAlias(entitySet.getName(), prop.getName()));

            EdmType edmType = prop.getType();

            CollectionKind ck = prop.getCollectionKind();
            if (edmType.isSimple()) {
                //Processing of non reserved items
                if (ck.equals(CollectionKind.List)) {
                    //For array elements
                    addSimpleListProperty(properties, prop, valO, edmType);
                } else {
                    //In case of simple element
                    addSimpleTypeProperty(properties, prop, valO, edmType);
                }
            } else {
                //For properties of type ComplexType
                if (metadata != null) {
                    if (ck.equals(CollectionKind.List)) {
                        //For array elements
                        addComplexListProperty(metadata, properties, prop, valO, edmType);
                    } else {
                        properties.add(createComplexTypeProperty(metadata, prop, valO));
                    }
                }
            }
        }

        //Generation processing of Navigation Property
        int count = 0;
        List<OLink> links = new ArrayList<OLink>();
        //About each navigation property defined in the schema
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
                //When the opponent's Multiplicity is MANY
                if (++count <= expandMaxNum && relatedEntities != null) {
                    lnk = OLinks.relatedEntitiesInline(toTypeName, npName, npName,
                            relatedEntities);
                } else {
                    lnk = OLinks.relatedEntities(toTypeName, npName, npName);
                }
            } else {
                //When the opponent 's Multiplicity is not MANY. (ZERO or ONE)
                if (++count <= expandMaxNum && relatedEntities != null) {
                    lnk = OLinks.relatedEntitiesInline(toTypeName, npName, npName,
                            relatedEntities);
                } else {
                    lnk = OLinks.relatedEntity(toTypeName, npName, npName);
                }
            }
            links.add(lnk);
        }

        //Generation of entityKey
        List<String> keys = eType.getKeys();
        List<String> kv = new ArrayList<String>();
        for (String key : keys) {
            kv.add(key);
            //I assume that the TODO key is a String. If the value of the key is other than a character string, it is necessary to deal with it.
            String v = (String) this.staticFields.get(key);
            if (v == null) {
                v = AbstractODataResource.DUMMY_KEY;
            }
            kv.add(v);
        }
        OEntityKey entityKey = OEntityKey.create(kv);

        //Generation of OEntity
        //TODO AtomPub system item is dummy
        OEntity entity = OEntities.create(entitySet, entityKey, properties, links, "title", "categoryTerm");

        //Wrap it to OEntityWrapper instead of OEntity so that ETag and hidden items can be returned.
        String etag = this.createEtag();
        OEntityWrapper oew = new OEntityWrapper(this.id, entity, etag);
        //Setting hidden items
        if (this.hiddenFields != null) {
            for (Map.Entry<String, Object> entry : this.hiddenFields.entrySet()) {
                oew.put(entry.getKey(), entry.getValue());
            }
        }
        oew.setManyToOneLinks(this.manyToOnelinkId);
        return oew;
    }

    /**
     * Add a simple type array element to the property.
     * @param properties Property summary
     * @param edmProp schema for additional properties
     * @param propValue Value of additional procedure
     * @param edmType Type information of type
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
     * Add Complex type array elements to the property.
     * @param metadata schema information
     * @param properties Property summary
     * @param edmProp schema for additional properties
     * @param propValue Value of additional procedure
     * @param edmType Type information of type
     */
    @SuppressWarnings("unchecked")
    protected void addComplexListProperty(EdmDataServices metadata,
            List<OProperty<?>> properties,
            EdmProperty edmProp,
            Object propValue,
            EdmType edmType) {
        EdmCollectionType collectionType = new EdmCollectionType(edmProp.getCollectionKind(), edmType);
        OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType.getItemType());

        //Obtain type information of ComplexType
        EdmComplexType ct = metadata.findEdmComplexType(
                collectionType.getItemType().getFullyQualifiedTypeName());

        if (propValue == null) {
            properties.add(OProperties.collection(edmProp.getName(), collectionType, null));
        } else {
            //Add ComplexType property to array
            for (Object val : (ArrayList<Object>) propValue) {
                builder.add(OComplexObjects.create(ct, getComplexTypePropList(metadata, edmProp, val)));
            }

            //Add an array element of ComplexType to the property
            properties.add(OProperties.collection(edmProp.getName(), collectionType, builder.build()));
        }
    }

    /**
     * Add properties of type SimpleType to property array.
     * @param properties Property array
     * @param prop Property to add
     * @param valO property's value
     * @param edmType type
     */
    protected void addSimpleTypeProperty(List<OProperty<?>> properties, EdmProperty prop, Object valO,
            EdmType edmType) {
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
                //When a value in a range that can be represented by Int type is entered in the user data, an Integer object is passed to valO
                //When Integer type is passed to the constructor of LocalDateTime, it becomes an error, so it converts it to a long type object
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
            //Since Value is registered as ES in ES, it is converted to Boolean
            if (valO == null) {
                properties.add(OProperties.boolean_(prop.getName(), null));
            } else {
                properties.add(OProperties.boolean_(prop.getName(), Boolean.valueOf(String.valueOf(valO))));
            }
        }
    }

    /**
     * Convert ComplexType type property to OProperty array.
     * @param metadata schema definition
     * @param prop property of type ComplexType
     * @param value Value of type ComplexType
     * @return Array of OProperty
     */
    private OProperty<List<OProperty<?>>> createComplexTypeProperty(
            EdmDataServices metadata,
            EdmProperty prop,
            Object value) {
        //Get ComplexType definition from schema definition
        EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());

        //Get properties of ComplexType
        List<OProperty<?>> props = getComplexTypePropList(metadata, prop, value);

        return OProperties.complex(prop.getName(), edmComplexType, props);
    }

    /**
     * Convert ComplexType type property to OProperty array.
     * @param metadata schema definition
     * @param prop property of type ComplexType
     * @param value Value of type ComplexType
     * @return Array of OProperty
     */
    @SuppressWarnings("unchecked")
    protected List<OProperty<?>> getComplexTypePropList(EdmDataServices metadata,
            EdmProperty prop,
            Object value) {
        if (value == null) {
            return null;
        }

        List<OProperty<?>> props = new ArrayList<OProperty<?>>();

        //Get ComplexType definition from schema definition
        EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());

        HashMap<String, Object> valMap = (HashMap<String, Object>) value;
        for (EdmProperty propChild : edmComplexType.getDeclaredProperties()) {
            String typeName = propChild.getDeclaringType().getName();
            Object valChild = valMap.get(resolveComplexTypeAlias(propChild.getName(), typeName));
            EdmType edmType = propChild.getType();
            CollectionKind ck = propChild.getCollectionKind();
            if (edmType.isSimple()) {
                //Processing of non reserved items
                if (ck.equals(CollectionKind.List)) {
                    //For array elements
                    addSimpleListProperty(props, propChild, valChild, edmType);
                } else {
                    //In case of simple element
                    addSimpleTypeProperty(props, propChild, valChild, edmType);
                }
            } else {
                if (ck.equals(CollectionKind.List)) {
                    //For array elements
                    addComplexListProperty(metadata, props, propChild, valChild, edmType);
                } else {
                    props.add(createComplexTypeProperty(metadata, propChild, valChild));
                }
            }
        }
        return props;
    }

    /**
     * Get Alias ​​name from property name.
     * To manipulate user data, override this method and convert it to Alias.
     * @param propertyName property name
     * @param typeName ComplexType name to which this property belongs
     * @return Alias ​​name
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
     * Acquire registration data.
     * @return Registration data
     */
    @Override
    public Map<String, Object> getSource() {
        return getCommonSource();
    }

    /**
     * Acquire registration data.
     * @return Registration data
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
     * Set it when you want to associate data with a specific Cell.
     * @param cellId CellId
     */
    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    /**
     * Set it when you want to associate data with a specific Box.
     * @param boxId Box Id
     */
    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    /**
     * Set it when you want to associate data with a specific node.
     * @param nodeId Node Id
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Setter of entityTypeId.
     * @param entityTypeId ID of entityType
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
     * Setter of manyToOnelinkId.
     * @param link manyToOnelinkId
     */
    public final void setManyToOnelinkId(Map<String, Object> link) {
        this.manyToOnelinkId = link;
    }

    /**
     * A setter of staticFields.
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
     * Returns the update date in Long format.
     * @return Long format of update date
     */
    public Long getUpdated() {
        return this.updated;
    }

    /**
     * Returns the created date in Long format.
     * @return Created date in Long format
     */
    public Long getPublished() {
        return this.published;
    }

    /**
     * Get upper limit value when $ expand is specified.
     * @return Upper limit when $ expand is specified
     */
    public int getExpandMaxNum() {
        return expandMaxNum;
    }

    /**
     * Set the upper limit value when $ expand is specified.
     * @param expandMaxNum upper limit when specifying $ expand
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
     * Getter of entityTypeId.
     * @return EntityType Id
     */
    public final String getEntityTypeId() {
        return entityTypeId;
    }

    /**
     * Getter of manyToOnelinkId.
     * @return manyToOnelinkId
     */
    public final Map<String, Object> getManyToOnelinkId() {
        return this.manyToOnelinkId;
    }

    /**
     * Getter of staticFields.
     * @return staticFields
     */
    public final Map<String, Object> getStaticFields() {
        return this.staticFields;
    }

    /**
     * Getter of UnitUserName.
     * @return UnitUser name
     */
    public final String getUnitUserName() {
        return this.unitUserName;
    }

    /**
     * JSON key that stores Declared Property in OData entity storage on ES.
     */
    public static final String KEY_STATIC_FIELDS = "s";
    /**
     * JSON key to store Dynamic Property in OData entity storage on ES.
     */
    public static final String KEY_DYNAMIC_FIELDS = "d";
    /**
     * JSON key to store hidden items in OData entity storage on ES.
     */
    public static final String KEY_HIDDEN_FIELDS = "h";
    /**
     * JSON key that stores update date and time in OData entity storage on ES.
     */
    public static final String KEY_UPDATED = "u";
    /**
     * JSON key that stores creation date and time in OData entity storage on ES.
     */
    public static final String KEY_PUBLISHED = "p";
    /**
     * JSON key that stores the Cell's internal ID in OData entity storage on ES.
     */
    public static final String KEY_CELL_ID = "c";
    /**
     * JSON key to store Box's internal ID in OData entity storage on ES.
     */
    public static final String KEY_BOX_ID = "b";
    /**
     * A JSON key that stores the nodeid of the collection in OData entity storage on the ES.
     */
    public static final String KEY_NODE_ID = "n";
    /**
     * JSON key that stores internal ID of EntityType in OData entity storage on ES.
     */
    public static final String KEY_ENTITY_ID = "t";
    /**
     * In OData entity storage on ES, JSON key that stores the destination document internal ID of n: 1: 1: 1 Link.
     */
    public static final String KEY_LINK = "l";
    /**
     * Keys JSON holding Owner in OData entity storage on ES.
     */
    public static final String KEY_OWNER = "h.Owner.untouched";

    /**
     * @return ACL setting information
     */
    public Map<String, JSONObject> getAclFields() {
        return new HashMap<String, JSONObject>();
    }

    /**
     * Returns a string representation of StaticFields.
     * @return StaticFields
     */
    public String getStaticFieldsString() {
        return JSONObject.toJSONString(this.staticFields);
    }

    /**
     * Returns a string representation of DynamicFields.
     * @return DynamicFields
     */
    public String getDynamicFieldsString() {
        return JSONObject.toJSONString(this.dynamicFields);
    }

    /**
     * Returns a string representation of HiddenFields.
     * @return HiddenFields
     */
    public String getHiddenFieldsString() {
        return JSONObject.toJSONString(this.hiddenFields);
    }

    /**
     * Returns a string representation of ManyToOnelinkId.
     * @return ManyToOnelinkId
     */
    public String getManyToOnelinkIdString() {
        return JSONObject.toJSONString(this.manyToOnelinkId);
    }
}

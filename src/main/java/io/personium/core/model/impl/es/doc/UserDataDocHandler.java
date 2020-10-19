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
 * DocHandler of OEntity.
 */
public class UserDataDocHandler extends OEntityDocHandler implements EntitySetDocHandler {

    private Map<String, PropertyAlias> propertyAliasMap;
    private String entitySetName;

    /**
     * constructor.
     */
    public UserDataDocHandler() {
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * Constructor that creates an instance of UserDataDocHandler from the result of acquiring one ES.
     * @param getResponse .
     */
    public UserDataDocHandler(PersoniumGetResponse getResponse) {
        super(getResponse);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * Constructor that creates an instance of UserDataDocHandler from the search result of ES.
     * @param searchHit .
     */
    public UserDataDocHandler(PersoniumSearchHit searchHit) {
        super(searchHit);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * Constructor that creates DocHandler without ID from OEntityWrapper.
     * @param type ES type name
     * @param oEntityWrapper OEntityWrapper
     * @param metadata schema information
     */
    public UserDataDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        initInstance(type, oEntityWrapper, metadata);
        this.propertyAliasMap = null;
        this.entitySetName = null;
    }

    /**
     * Returns the correspondence Map of property name and alias.
     * @return Correspondence between property names and aliases Map
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return this.propertyAliasMap;
    }

    /**
     * Set correspondence map of property name and alias.
     * @param value Correspondence between property name and alias Map
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> value) {
        this.propertyAliasMap = value;
    }

    /**
     * Returns the entity set name.
     * @return entity set name
     */
    public String getEntitySetName() {
        return this.entitySetName;
    }

    /**
     * Set the entity set name.
     * @param name Entity set name
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
     * Get Alias ​​name from property name.
     * To manipulate user data, override this method and convert it to Alias.
     * @param propertyName property name
     * @param typeName ComplexType name to which this property belongs
     * @return Alias ​​name
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
            //Discard properties other than __id (such as __published) without converting
            if (!CtlSchema.P_ID.getName().equals(key) && key.startsWith("_")) {
                continue;
            }
            boolean isDynamic = false;
            NamespacedAnnotation<?> annotation = prop.findAnnotation(Common.P_NAMESPACE.getUri(),
                    Property.P_IS_DECLARED.getName());
            if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
                isDynamic = true;
            }
            if (!staticFields.containsKey(getPropertyNameOrAlias(edmEntityType.getName(), key)) && isDynamic) {
                continue;
            }

            Object value = staticFields.get(getPropertyNameOrAlias(edmEntityType.getName(), key));
            if (prop.getType().isSimple() || value == null) {
                //In the case of the Simple type, conversion is performed as it is
                staticFieldMap.put(key, value);
            } else {
                //In the case of Complex type, further convert the element in
                EdmComplexType edmComplexType = metadata.findEdmComplexType(prop.getType().getFullyQualifiedTypeName());
                if (CollectionKind.List.equals(prop.getCollectionKind())) {
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
                //In the case of Complex type, further convert the element in
                if (CollectionKind.List.equals(prop.getCollectionKind())) {
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
     * Return a property value object converted to an appropriate type according to the property definition of the schema.
     * In the case of user data, convert the Boolean type property value to a character string.
     * @param prop property object
     * Property definition for @param edmType schema
     * @return Property value object converted to appropriate type
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

        //Convert Boolean / Double property values ​​to string
        if (prop.getValue() != null
                && (edmType.equals(EdmSimpleType.BOOLEAN) || edmType.equals(EdmSimpleType.DOUBLE))) {
            return String.valueOf(prop.getValue());
        }
        return prop.getValue();
    }

    /**
     * Convert the value of DynamicProperty.
     * @param key Key to be converted
     * @param value Value to convert
     */
    public void convertDynamicPropertyValue(String key, Object value) {
        if (value != null && (value instanceof Boolean || value instanceof Double)) {
            this.getDynamicFields().put(key, String.valueOf(value));
        }
    }

    @Override
    protected Object convertSimpleListValue(final EdmType edmType, Object propValue) {
        Object convertValue = super.convertSimpleListValue(edmType, propValue);
        //Convert Boolean / Double property values ​​to string
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

        //UserData holds data in the link field in character array format ("EntityTypeID: UserDataID"), so it converts it to a string array
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
     * Parsing Link field.
     * @param source parse source information in the form of Map
     * @return Link information
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> parseLinks(Map<String, Object> source) {
        //Since UserData holds data in the string field format ("EntityTypeID: UserDataID") in the link field, it converts it to Map format
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
     * Convert the property name stored in static field to Alias.
     * @param entity type to which the entityType property is attached (EntityType or ComplexType)
     * @param entity name to which the entityName property is attached
     * @param entrySet static map map object stored in field
     * @return static field map object
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
                    for (Map<String, Object> item : (List<Map<String, Object>>) value) {
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
     * Get the property's Alias ​​name from the mapping data.
     * @param key Search key for mapping data
     * @param propertyName property name
     * @return Alias ​​name
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
     * Get the ComplexType name specified in the type attribute value of the property from the mapping data.
     * @param key Search key for mapping data
     * @param propertyName property name
     * @return ComplexType name
     */
    private String getComplexTypeName(String key) {
        PropertyAlias alias = this.propertyAliasMap.get(key);
        if (alias != null && alias.getAlias().startsWith("C")) {
            return alias.getPropertyType();
        }
        return null;
    }

    private String getPropertyMapKey(String entityType, String name, String propertyName) {
        //Since it is impossible to determine EntityType / ComplexType when this method is called,
        //The key of property Alias ​​uses uniform "_EntityType.Name".
        String key = "Name='" + propertyName + "'," + entityType + "='" + name + "'";
        return key;
    }
}

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

import org.odata4j.edm.EdmDataServices;

import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyAlias;
import io.personium.core.odata.OEntityWrapper;

/**
 * Property DocHandler.
 */
public class PropertyDocHandler extends OEntityDocHandler implements EntitySetDocHandler {

    Map<String, PropertyAlias> propertyAliasMap;
    Map<String, String> entityTypeMap;
    String linkTypeName = Property.P_ENTITYTYPE_NAME.getName();

    /**
     * constructor.
     */
    public PropertyDocHandler() {
        this.propertyAliasMap = null;
    }

    /**
     * Constructor that creates DocHandler without ID from OEntityWrapper.
     * @param type ES type name
     * @param oEntityWrapper OEntityWrapper
     * @param metadata schema information
     */
    public PropertyDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        this.propertyAliasMap = null;
        initInstance(type, oEntityWrapper, metadata);
        this.staticFields.put(Property.P_IS_DECLARED.getName(), true);
    }

    /**
     * constructor.
     * @param cellId Cell ID
     * @param boxId Box ID
     * @param nodeId node ID
     * @param entityTypeId ID of the associated entity type
     * @param source static property field
     */
    public PropertyDocHandler(String cellId,
            String boxId,
            String nodeId,
            String entityTypeId,
            Map<String, Object> source) {
        this.propertyAliasMap = null;
        this.type = Property.EDM_TYPE_NAME;
        this.version = 0L;

        //Pegged with Cell, Box, Node
        this.setCellId(cellId);
        this.setBoxId(boxId);
        this.setNodeId(nodeId);

        // published, updated
        long crrTime = System.currentTimeMillis();
        this.setPublished(crrTime);
        this.setUpdated(crrTime);

        this.staticFields = source;
        this.dynamicFields = new HashMap<String, Object>();
        this.hiddenFields = new HashMap<String, Object>();
        Map<String, Object> linksMap = new HashMap<String, Object>();
        linksMap.put(EntityType.EDM_TYPE_NAME, entityTypeId);
        this.manyToOnelinkId = linksMap;
    }

    /**
     * Return EntityType name.
     * @return EntityType name
     */
    public String getEntityTypeName() {
        String entityTypeId = (String) this.manyToOnelinkId.get(EntityType.EDM_TYPE_NAME);
        return this.entityTypeMap.get(linkTypeName + entityTypeId);
    }

    /**
     * Return the registered alias list.
     * @return propertyMap Registered alias list
     */
    public Map<String, String> getEntityTypeMap() {
        return this.entityTypeMap;
    }

    /**
     * Set up a registered alias list.
     * @param map propertyMap to set
     */
    public void setEntityTypeMap(Map<String, String> map) {
        this.entityTypeMap = map;
    }

    /**
     * Return the registered alias list.
     * @return propertyMap Registered alias list
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return this.propertyAliasMap;
    }

    /**
     * Set up a registered alias list.
     * @param map set propertyAliasMap
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> map) {
        this.propertyAliasMap = map;
    }

    /**
     * Acquire ES / MySQL registration data.
     * @return Registration data
     */
    @Override
    public Map<String, Object> getSource() {
        String dataType = (String) this.staticFields.get("Type");
        String entityTypeId = (String) this.manyToOnelinkId.get(EntityType.EDM_TYPE_NAME);
        String entityTypeName = this.entityTypeMap.get(linkTypeName + entityTypeId);
        String alias = getNextAlias(entityTypeName, dataType);
        String propertyName = (String) this.staticFields.get("Name");
        String key = "Name='" + propertyName + "'," + linkTypeName + "='" + entityTypeName + "'";
        PropertyAlias propertyAlias = new PropertyAlias(linkTypeName, propertyName, entityTypeName, alias);
        Map<String, Object> ret = setSource(key, propertyAlias);
        return ret;
    }

    /**
     * Set data for ES / MySQL registration to Map object.
     * @param propertyAlias ​​property Alias ​​information
     * @param key property key
     * @return Created Map object
     */
    protected Map<String, Object> setSource(String key, PropertyAlias propertyAlias) {
        this.propertyAliasMap.put(key, propertyAlias);
        this.hiddenFields.put("Alias", propertyAlias.getAlias());
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put(KEY_STATIC_FIELDS, this.staticFields);
        ret.put(KEY_HIDDEN_FIELDS, this.hiddenFields);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        ret.put(KEY_CELL_ID, this.cellId);
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_LINK, this.manyToOnelinkId);
        return ret;
    }

    /**
     * Get Name of Property.
     * @return Name of Property
     */
    public String getName() {
        return (String) this.staticFields.get("Name");
    }

    /**
     * Get the property Alias ​​with the maximum value of registered property + 1.
     * @param entityTypeName EntityType name
     * @param dataType property data type name
     * @return Alias ​​of the assigned property name
     */
    protected String getNextAlias(String entityTypeName, String dataType) {
        //Determine the prefix of the alias from the data type
        String aliasPrefix = "P";
        if (!dataType.startsWith("Edm.")) {
            aliasPrefix = "C";
        }

        //Construct a List of aliases by narrowing the correspondence map of properties and aliases with the EntityType name
        List<Integer> aliasList = new ArrayList<Integer>();
        for (Map.Entry<String, PropertyAlias> entry : this.propertyAliasMap.entrySet()) {
            if (entry.getKey().endsWith(this.linkTypeName + "='" + entityTypeName + "'")) {
                String value = entry.getValue().getAlias();
                if (value == null) {
                    //Temporary null check
                    continue;
                }
                if (!value.startsWith(aliasPrefix)) {
                    //Exclude aliases with different prefixes (P / C)
                    continue;
                }
                //Cut out only numerical part
                int num = getAliasNumber(value);
                aliasList.add(Integer.valueOf(num));
            }
        }

        int nextNum = aliasList.size() + 1;

        //Numbering of property numbers
        //Common numbers of simple type and complex type are numbered separately.
        if (aliasList.contains(Integer.valueOf(nextNum))) {
            //Search for vacancies as they are already in use
            for (int i = 0; i < aliasList.size(); i++) {
                if (!aliasList.contains(Integer.valueOf(i + 1))) {
                    nextNum = i + 1;
                    break;
                }
            }
        }

        String newAlias = String.format("%s%03d", aliasPrefix, nextNum);
        return newAlias;
    }

    /**
     * Alias ​​Gets index from string.
     * @param alias Alias ​​string
     * @return index
     */
    protected int getAliasNumber(String alias) {
        if (alias.startsWith("C")) {
            String[] splitedAlias = alias.split(":");
            return Integer.parseInt(splitedAlias[0].substring(1));
        }
        return Integer.parseInt(alias.substring(1));
    }
}

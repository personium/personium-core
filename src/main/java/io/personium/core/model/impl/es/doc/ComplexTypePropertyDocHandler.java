/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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

import java.util.HashMap;
import java.util.Map;

import org.odata4j.edm.EdmDataServices;

import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyAlias;
import io.personium.core.odata.OEntityWrapper;

/**
 * DocHandler of ComplexType property.
 */
public class ComplexTypePropertyDocHandler extends PropertyDocHandler implements EntitySetDocHandler {

    /**
     * constructor.
     */
    public ComplexTypePropertyDocHandler() {
        propertyAliasMap = null;
        this.linkTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
    }

    /**
     * Constructor that creates DocHandler without ID from OEntityWrapper.
     * @param type ES type name
     * @param oEntityWrapper OEntityWrapper
     * @param metadata schema information
     */
    public ComplexTypePropertyDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        propertyAliasMap = null;
        this.linkTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
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
    public ComplexTypePropertyDocHandler(String cellId,
            String boxId,
            String nodeId,
            String entityTypeId,
            Map<String, Object> source) {
        super(cellId, boxId, nodeId, entityTypeId, source);
        this.type = ComplexTypeProperty.EDM_TYPE_NAME;
        Map<String, Object> linksMap = new HashMap<String, Object>();
        linksMap.put(ComplexType.EDM_TYPE_NAME, entityTypeId);
        this.manyToOnelinkId = linksMap;
        this.linkTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
    }

    /**
     * Acquire ES / MySQL registration data.
     * @return Registration data
     */
    @Override
    public Map<String, Object> getSource() {
        String dataType = (String) this.staticFields.get("Type");
        String entityTypeId = (String) this.manyToOnelinkId.get(ComplexType.EDM_TYPE_NAME);
        String entityTypeName = getEntityTypeMap().get(linkTypeName + entityTypeId);
        String alias = getNextAlias(entityTypeName, dataType);
        String propertyName = (String) this.staticFields.get("Name");
        String key = "Name='" + propertyName + "'," + linkTypeName + "='" + entityTypeName + "'";
        PropertyAlias propertyAlias = new PropertyAlias(linkTypeName, propertyName, entityTypeName, alias);
        Map<String, Object> ret = setSource(key, propertyAlias);
        return ret;
    }

    /**
     * Return ComplexType name.
     * @return ComplexType name
     */
    @Override
    public String getEntityTypeName() {
        String entityTypeId = (String) this.manyToOnelinkId.get(ComplexType.EDM_TYPE_NAME);
        return this.entityTypeMap.get(linkTypeName + entityTypeId);
    }

}

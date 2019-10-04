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

import java.util.Map;

import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.OrderByExpression;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * UserData elasticsearch query handler.
 */
public class UserDataQueryHandler extends EsQueryHandler implements ODataQueryHandler {
    /**
     * Property / ComplexTypeProperty and Alias ​​mapping data.
     */
    private Map<String, PropertyAlias> propertyAliasMap;

    /**
     * constructor.
     * @param entityType entity type
     * @param map property name and Map of Alias
     */
    public UserDataQueryHandler(EdmEntityType entityType, Map<String, PropertyAlias> map) {
        super(entityType);
        this.propertyAliasMap = map;
    }

    @Override
    public void visit(OrderByExpression expr) {
        log.debug("visit(OrderByExpression expr)");
        if (!(expr.getExpression() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }

        //Acquire the property schema to be sorted
        String name = ((EntitySimpleProperty) expr.getExpression()).getPropertyName();
        EdmProperty edmProperty = this.entityType.findProperty(name);

        //If the property does not exist, do not add it to the sort condition
        if (edmProperty == null) {
            return;
        }

        //An error occurs when sorting on an array is specified
        if (CollectionKind.List.equals(edmProperty.getCollectionKind())) {
            throw PersoniumCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY;
        }

        if (!isUntouched(name, edmProperty)) {
            super.visit(expr);
        } else {
            //In the case of a character string
            String key = getSearchKey(expr.getExpression(), true);
            String orderOption = getOrderOption(expr.getDirection());
            Map<String, Object> orderByValue = null;
            orderByValue = UserDataQueryHandlerHelper.getOrderByValue(orderOption, key);
            this.orderBy.put(UserDataQueryHandlerHelper.getOrderByKey(key), orderByValue);
        }
    }

    @Override
    protected String getSearchKey(CommonExpression expr, Boolean isUntouched) {
        //Set as search key
        String name = ((EntitySimpleProperty) expr).getPropertyName();
        EdmProperty edmProperty = this.entityType.findProperty(name);

        // published, updated
        if (Common.P_PUBLISHED.getName().equals(name)) {
            return OEntityDocHandler.KEY_PUBLISHED;
        } else if (Common.P_UPDATED.getName().equals(name)) {
            return OEntityDocHandler.KEY_UPDATED;
        }

        //s. Search field fields
        String fieldPrefix = OEntityDocHandler.KEY_STATIC_FIELDS;

        //Convert key name to Alias
        String key = "Name='" + name + "',_EntityType.Name='" + this.entityType.getName() + "'";
        String keyName = getAlias(key, this.entityType.getName());
        if (keyName == null) {
            keyName = name;
        }

        //Change suffix (search target field) depending on type
        String suffix = getSuffix(edmProperty);

        if (isUntouched) {
            return fieldPrefix + "." + keyName + "." + suffix;
        } else {
            return fieldPrefix + "." + keyName;
        }
    }

    /**
     * It is determined whether the untouched field should be used.
     * @param name property name
     * @param edmProperty property schema information
     * true if the untretched field should be used, false otherwise
     */
    private boolean isUntouched(String name, EdmProperty edmProperty) {
        if (Common.P_ID.getName().equals(name)
                || Common.P_PUBLISHED.getName().equals(name)
                || Common.P_UPDATED.getName().equals(name)
                || EdmSimpleType.SINGLE.equals(edmProperty.getType())
                || EdmSimpleType.DOUBLE.equals(edmProperty.getType())
                || EdmSimpleType.INT32.equals(edmProperty.getType())) {
            return false;
        }
        return true;
    }

    private String getSuffix(EdmProperty edmProperty) {
        String suffix = "untouched";
        if (edmProperty != null) {
            if (EdmSimpleType.SINGLE.equals(edmProperty.getType())
                    || EdmSimpleType.DOUBLE.equals(edmProperty.getType())) {
                suffix = "double";
            } else if (EdmSimpleType.INT32.equals(edmProperty.getType())) {
                suffix = "long";
            } else if (EdmSimpleType.DATETIME.equals(edmProperty.getType())) {
                suffix = "long";
            }
        }
        return suffix;
    }

    @Override
    protected String getFieldName(String prop) {
        String key = "Name='" + prop + "',_EntityType.Name='" + this.entityType.getName() + "'";
        String keyName = getAlias(key, this.entityType.getName());
        return OEntityDocHandler.KEY_STATIC_FIELDS + "." + keyName;
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

}

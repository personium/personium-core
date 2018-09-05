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

import java.io.Serializable;

/**
 * Alias ​​management class of Property and ComplexTypeProperty.
 */
public class PropertyAlias implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * EntityTYpe name.
     * EntityType or ComplexType
     */
    private String entityTypeName;
    /**
     * Property name on Edmx / ComplexTypeProperty name.
     */
    private String propertyName;
    /**
     * EntityType name associated with Property / ComplexTypeProperty.
     */
    private String propertyType;
    /**
     * Alias ​​name to be set when data of Property / ComplexTypeProperty is stored.
     */
    private String alias;

    /**
     * constructor.
     * @ param entityTypeName EntityTYpe name
     * @ param propertyName Property name on Edmx / ComplexTypeProperty name
     * @ param propertyType Property type to which Property / ComplexTypeProperty is associated (eg Edm.String, complexXXX)
     * @ param alias Alias ​​name to be set when data of Property / ComplexTypeProperty is stored
     */
    public PropertyAlias(
            String entityTypeName,
            String propertyName,
            String propertyType,
            String alias) {
        this.entityTypeName = entityTypeName;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.alias = alias;
    }

    /**
     * @return the type
     */
    public String getEntityTypeName() {
        return entityTypeName;
    }

    /**
     * @return the propertyName
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the entityName
     */
    public String getPropertyType() {
        return propertyType;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }
}

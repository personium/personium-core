/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.test.jersey.box;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Schemaを扱うオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class Schema {
    /**
     * コンストラクタ.
     */
    public Schema() {
    }

    Schema(String namespace,
            List<EntityType> entityTypes,
            List<Association> associations,
            EntityContainer entityContainer) {
        this.namespace = namespace;
        this.entityTypes = entityTypes;
        this.associations = associations;
        this.entityContainer = entityContainer;
    }

    Schema(String namespace,
            Map<String, EntityType> entityTypes,
            List<Association> associations,
            EntityContainer entityContainer) {
        this.namespace = namespace;
        this.checkEntityType = entityTypes;
        this.associations = associations;
        this.entityContainer = entityContainer;
    }

    @XmlElement(name = "EntityType", type = EntityType.class)
    List<EntityType> entityTypes;

    Map<String, EntityType> checkEntityType;

    @XmlElement(name = "Association", type = Association.class)
    List<Association> associations;

    @XmlElement(name = "EntityContainer", type = EntityContainer.class)
    EntityContainer entityContainer;

    @XmlAttribute(name = "Namespace")
    String namespace;

    @Override
    public boolean equals(Object obj) {
        Schema schema = (Schema) obj;
        for (EntityType entityType : schema.entityTypes) {
            if (!this.checkEntityType.get(entityType.name).equals(entityType)) {
                Edmx.printResult(this,
                       "EntityType",
                       entityType.toString(),
                       this.checkEntityType.get(entityType.name).toString());
                return false;
            }
        }

        if (!namespace.equals(schema.namespace)) {
            Edmx.printResult(this, "Namespace", schema.namespace, namespace);
            return false;
        }

        if (associations.size() != schema.associations.size()) {
            Edmx.printResult(this, "Association", schema.associations.toString(), associations.toString());
        }
        if (!associations.equals(schema.associations)) {
            Edmx.printResult(this, "Association", schema.associations.toString(), associations.toString());
            return false;
        }

        if (!entityContainer.equals(schema.entityContainer)) {
            Edmx.printResult(this, "EntityContainer", schema.entityContainer.toString(), entityContainer.toString());
            return false;
        }
        return true;

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Schema => Namespace:").append(namespace).append("\n");
        if (entityTypes != null) {
            for (EntityType entityType : entityTypes) {
                builder.append(entityType.toString());
            }
        }
        if (checkEntityType != null) {
            for (Map.Entry<String, EntityType> entry : checkEntityType.entrySet()) {
                builder.append(entry.getValue().toString());
            }
        }
        for (Association association : associations) {
            builder.append(association.toString());
        }
        builder.append(entityContainer.toString());
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

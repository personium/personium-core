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
package io.personium.test.jersey.box;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * EntityContainer.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://schemas.microsoft.com/ado/2006/04/edm")
public class EntityContainer {
    /**
     * コンストラクタ.
     */
    public EntityContainer() {
    }

    EntityContainer(String name,
            String isDefaultEntityContainer,
            Set<EntitySet> entitySets,
            List<AssociationSet> associationSets) {
        this.name = name;
        this.isDefaultEntityContainer = isDefaultEntityContainer;
        this.entitySets = entitySets;
        this.associationSets = associationSets;
    }

    @XmlElement(name = "EntitySet", type = EntitySet.class)
    Set<EntitySet> entitySets;

    @XmlElement(name = "AssociationSet", type = AssociationSet.class)
    List<AssociationSet> associationSets;

    @XmlAttribute(name = "Name")
    String name;

    @XmlAttribute(namespace = "http://schemas.microsoft.com/ado/2007/08/dataservices/metadata",
            name = "IsDefaultEntityContainer")
    String isDefaultEntityContainer;

    @Override
    public boolean equals(Object obj) {
        EntityContainer entityContainer = (EntityContainer) obj;
        if (!name.equals(entityContainer.name)) {
            Edmx.printResult(this, "Name", entityContainer.name, name);
            return false;
        }

        if (!isDefaultEntityContainer.equals(entityContainer.isDefaultEntityContainer)) {
            Edmx.printResult(this,
                    "IsDefaultEntityContainer",
                    entityContainer.isDefaultEntityContainer,
                    isDefaultEntityContainer);
            return false;
        }

        if (entitySets.size() != entityContainer.entitySets.size()) {
            Edmx.printResult(this, "EntitySet", entityContainer.entitySets.toString(), entitySets.toString());
        }
        assertEquals(entitySets, entityContainer.entitySets);

        if (associationSets.size() != entityContainer.associationSets.size()) {
            Edmx.printResult(this,
                "AssociationSet",
                entityContainer.associationSets.toString(),
                associationSets.toString());
        }
        if (!associationSets.equals(entityContainer.associationSets)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Association => Name:").append(name)
                .append(",IsDefaultEntityContainer:").append(isDefaultEntityContainer).append("\n");
        for (AssociationSet associationSet : associationSets) {
            builder.append(associationSet.toString());
        }
        for (EntitySet entitySet : entitySets) {
            builder.append(entitySet.toString());
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

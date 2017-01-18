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
package com.fujitsu.dc.test.unit.core.model.impl.es.doc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.model.impl.es.odata.PropertyAlias;
import com.fujitsu.dc.test.categories.Unit;

/**
 * OEntityDocHandlerのユニットテスト.
 */
@Category({ Unit.class })
public class PropertyAliasTest {

    /**
     * オブジェクトに設定したEntityTypeの値が正しく取得できること.
     */
    @Test
    public void オブジェクトに設定したEntityTypeの値が正しく取得できること() {
        String entityType = "EntityType";
        String propertyName = "property";
        String entityTypeName = "entity";
        String alias = "P001";
        PropertyAlias propertyAlias = new PropertyAlias(entityType, propertyName, entityTypeName, alias);
        assertEquals(entityType, propertyAlias.getEntityTypeName());
        assertEquals(propertyName, propertyAlias.getPropertyName());
        assertEquals(entityTypeName, propertyAlias.getPropertyType());
        assertEquals(alias, propertyAlias.getAlias());
    }


    /**
     * オブジェクトに設定したComplexTypeの値が正しく取得できること.
     */
    @Test
    public void オブジェクトに設定したComplexTypeの値が正しく取得できること() {
        String entityType = "ComplexType";
        String propertyName = "property";
        String entityTypeName = "entity";
        String alias = "C001";
        PropertyAlias propertyAlias = new PropertyAlias(entityType, propertyName, entityTypeName, alias);
        assertEquals(entityType, propertyAlias.getEntityTypeName());
        assertEquals(propertyName, propertyAlias.getPropertyName());
        assertEquals(entityTypeName, propertyAlias.getPropertyType());
        assertEquals(alias, propertyAlias.getAlias());
    }
}

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

import java.util.HashMap;
import java.util.Map;

import org.odata4j.edm.EdmDataServices;

import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyAlias;
import io.personium.core.odata.OEntityWrapper;

/**
 * ComplexTypeプロパティのDocHandler.
 */
public class ComplexTypePropertyDocHandler extends PropertyDocHandler implements EntitySetDocHandler {

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyDocHandler() {
        propertyAliasMap = null;
        this.linkTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
    }

    /**
     * OEntityWrapperから IDのないDocHandlerをつくるConstructor.
     * @param type ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    public ComplexTypePropertyDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        propertyAliasMap = null;
        this.linkTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
        initInstance(type, oEntityWrapper, metadata);
        this.staticFields.put(Property.P_IS_DECLARED.getName(), true);
    }

    /**
     * コンストラクタ.
     * @param cellId セルID
     * @param boxId ボックスID
     * @param nodeId ノードID
     * @param entityTypeId 紐付くエンティティタイプのID
     * @param source 静的プロパティフィールド
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
     * ES/MySQL登録用データを取得する.
     * @return 登録用データ
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
     * ComplexType名を返却する.
     * @return ComplexType名
     */
    @Override
    public String getEntityTypeName() {
        String entityTypeId = (String) this.manyToOnelinkId.get(ComplexType.EDM_TYPE_NAME);
        return this.entityTypeMap.get(linkTypeName + entityTypeId);
    }

}

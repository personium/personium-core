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

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.expression.EntitySimpleProperty;

import io.personium.core.odata.OEntityWrapper;

/**
 * ElasticSearchのドキュメントを生成するHandler.
 */
public interface EntitySetDocHandler extends EsDocHandler {

    /**
     * typeのゲッター.
     * @return type
     */
    String getType();

    /**
     * CellIDのゲッター.
     * @return cellId
     */
    String getCellId();

    /**
     * BoxIDのゲッター.
     * @return boxId
     */
    String getBoxId();

    /**
     * NodeIDのゲッター.
     * @return nodeId
     */
    String getNodeId();

    /**
     * EntityTypeIDのゲッター.
     * @return entityTypeId
     */
    String getEntityTypeId();

    /**
     * StaticFieldsのゲッター.
     * @return StaticFields
     */
    Map<String, Object> getStaticFields();

    /**
     * DynamicFieldsのゲッター.
     * @return DynamicFields
     */
    Map<String, Object> getDynamicFields();

    /**
     * AclFieldsのゲッター.
     * @return ManyToOnelinkId
     */
    Map<String, JSONObject> getAclFields();

    /**
     * HiddenFieldsのゲッター.
     * @return HiddenFields
     */
    Map<String, Object> getHiddenFields();

    /**
     * ManyToOnelinkIdのゲッター.
     * @return ManyToOnelinkId
     */
    Map<String, Object> getManyToOnelinkId();

    /**
     * StaticFieldsの文字列表現を返す.
     * @return StaticFields
     */
    String getStaticFieldsString();

    /**
     * DynamicFieldsの文字列表現を返す.
     * @return DynamicFields
     */
    String getDynamicFieldsString();

    /**
     * HiddenFieldsの文字列表現を返す.
     * @return HiddenFields
     */
    String getHiddenFieldsString();

    /**
     * ManyToOnelinkIdの文字列表現を返す.
     * @return ManyToOnelinkId
     */
    String getManyToOnelinkIdString();

    /**
     * Publishedのゲッター.
     * @return Published
     */
    Long getPublished();

    /**
     * Updatedのゲッター.
     * @return Updated
     */
    Long getUpdated();

    /**
     * UnitUserNameのゲッター.
     * @return UnitUser名
     */
    String getUnitUserName();

    /**
     * Cellへのアクセス時にUnitUser名を設定する.
     * @param hiddenFieldsMap hiddenFieldsのマップオブジェクト
     */
    void resolveUnitUserName(Map<String, Object> hiddenFieldsMap);

    /**
     * Idのセッター.
     * @param id elasticsearchのID
     */
    void setId(String id);

    /**
     * staticFieldsのセッター.
     * @param staticFields staticFields
     */
    void setStaticFields(Map<String, Object> staticFields);

    /**
     * cellIdのセッター.
     * @param cellId CellのID
     */
    void setCellId(String cellId);

    /**
     * boxIdのセッター.
     * @param boxId BoxのID
     */
    void setBoxId(String boxId);

    /**
     * nodeIdのセッター.
     * @param nodeId nodeのID
     */
    void setNodeId(String nodeId);

    /**
     * entityTypeIdのセッター.
     * @param entityTypeId entityTypeのID
     */
    void setEntityTypeId(String entityTypeId);

    /**
     * typeのセッター.
     * @param type type
     */
    void setType(String type);

    /**
     * versionのセッター.
     * @param version version
     */
    void setVersion(Long version);

    /**
     * HiddenFieldsのセッター.
     * @param hiddenFields hiddenFields
     */
    void setHiddenFields(Map<String, Object> hiddenFields);

    /**
     * linksのセッター.
     * @param links links情報
     */
    void setManyToOnelinkId(Map<String, Object> links);

    /**
     * Publishedのセッター.
     * @param published published情報
     */
    void setPublished(Long published);

    /**
     * Updatedのセッター.
     * @param updated updated情報
     */
    void setUpdated(Long updated);

    /**
     * dynamicFieldsのセッター.
     * @param dynamicFields Dynamic Field の Map
     */
    void setDynamicFields(Map<String, Object> dynamicFields);

    /**
     * OEntityを作成する.
     * @param eSet EdmEntitySet
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet);

    /**
     * OEntityを作成する.
     * @param eSet EdmEntitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList);

    /**
     * OEntityを作成する.
     * @param eSet EdmEntitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @param selectQuery $selectクエリ
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList,
            List<EntitySimpleProperty> selectQuery);

    /**
     * ETagを生成する.
     * @return ETag
     */
    String createEtag();

    /**
     * 静的プロパティをAlias名からプロパティ名に変更する.
     * @param metadata スキーマ情報
     */
    void convertAliasToName(EdmDataServices metadata);
}

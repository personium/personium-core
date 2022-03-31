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

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.expression.EntitySimpleProperty;

import io.personium.core.odata.OEntityWrapper;

/**
 * Handler to generate ElasticSearch document.
 */
public interface EntitySetDocHandler extends EsDocHandler {

    /**
     * Getter of type.
     * @return type
     */
    String getType();

    /**
     * CellID getter.
     * @return cellId
     */
    String getCellId();

    /**
     * Getter of Box ID.
     * @return boxId
     */
    String getBoxId();

    /**
     * Getter of NodeID.
     * @return nodeId
     */
    String getNodeId();

    /**
     * Getter of EntityTypeID.
     * @return entityTypeId
     */
    String getEntityTypeId();

    /**
     * Getter of StaticFields.
     * @return StaticFields
     */
    Map<String, Object> getStaticFields();

    /**
     * Getter of Dynamic Fields.
     * @return DynamicFields
     */
    Map<String, Object> getDynamicFields();

    /**
     * Getlter of AclFields.
     * @return ManyToOnelinkId
     */
    Map<String, JSONObject> getAclFields();

    /**
     * Getter of Hidden Fields.
     * @return HiddenFields
     */
    Map<String, Object> getHiddenFields();

    /**
     * Getter of ManyToOnelinkId.
     * @return ManyToOnelinkId
     */
    Map<String, Object> getManyToOnelinkId();

    /**
     * Returns a string representation of StaticFields.
     * @return StaticFields
     */
    String getStaticFieldsString();

    /**
     * Returns a string representation of DynamicFields.
     * @return DynamicFields
     */
    String getDynamicFieldsString();

    /**
     * Returns a string representation of HiddenFields.
     * @return HiddenFields
     */
    String getHiddenFieldsString();

    /**
     * Returns a string representation of ManyToOnelinkId.
     * @return ManyToOnelinkId
     */
    String getManyToOnelinkIdString();

    /**
     * Published getter.
     * @return Published
     */
    Long getPublished();

    /**
     * Updated getter.
     * @return Updated
     */
    Long getUpdated();

    /**
     * Getter of UnitUserName.
     * @return UnitUser name
     */
    String getUnitUserName();

    /**
     * Set UnitUser name when accessing Cell.
     * @param hiddenFieldsMap map object of hiddenFields
     */
    void resolveUnitUserName(Map<String, Object> hiddenFieldsMap);

    /**
     * Id's setter.
     * @param id ID of elasticsearch
     */
    void setId(String id);

    /**
     * A setter of staticFields.
     * @param staticFields staticFields
     */
    void setStaticFields(Map<String, Object> staticFields);

    /**
     * CellId's setter.
     * @param cellId Cell ID
     */
    void setCellId(String cellId);

    /**
     * Setter of boxId.
     * @param boxId Box ID
     */
    void setBoxId(String boxId);

    /**
     * Setter of nodeId.
     * @param nodeId ID of node
     */
    void setNodeId(String nodeId);

    /**
     * Setter of entityTypeId.
     * @param entityTypeId ID of entityType
     */
    void setEntityTypeId(String entityTypeId);

    /**
     * Type setter.
     * @param type type
     */
    void setType(String type);

    /**
     * Version setter.
     * @param version version
     */
    void setVersion(Long version);

    /**
     * HiddenFields setter.
     * @param hiddenFields hiddenFields
     */
    void setHiddenFields(Map<String, Object> hiddenFields);

    /**
     * The setters of links.
     * @param links links information
     */
    void setManyToOnelinkId(Map<String, Object> links);

    /**
     * Published setter.
     * @param published published information
     */
    void setPublished(Long published);

    /**
     * Updated setter.
     * @param updated updated information
     */
    void setUpdated(Long updated);

    /**
     * The setter of dynamicFields.
     * @param dynamicFields Map of Dynamic Field
     */
    void setDynamicFields(Map<String, Object> dynamicFields);

    /**
     * Create OEntity.
     * @param eSet EdmEntitySet
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet);

    /**
     * Create OEntity.
     * @param eSet EdmEntitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList);

    /**
     * Create OEntity.
     * @param eSet EdmEntitySet
     * @param metadata metadata
     * @param relatedEntitiesList relatedEntitiesList
     * @param selectQuery $ select query
     * @return OEntityWrapper
     */
    OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList,
            List<EntitySimpleProperty> selectQuery);

    /**
     * Generate ETag.
     * @return ETag
     */
    String createEtag();

    /**
     * Change the static property from Alias ​​name to property name.
     * @param metadata schema information
     */
    void convertAliasToName(EdmDataServices metadata);
}

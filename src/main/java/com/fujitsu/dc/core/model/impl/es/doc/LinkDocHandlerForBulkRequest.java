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
package com.fujitsu.dc.core.model.impl.es.doc;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.odata4j.core.OEntity;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.expression.EntitySimpleProperty;

import com.fujitsu.dc.core.odata.OEntityWrapper;

/**
 * LinkDocHandlerをバルクで扱うためのDocHandler.
 * EntitySetDocHandlerとしてアクセスするために必要な情報のみ元のLinkDocHandlerからコピーして保持する。
 * したがってこの用途で不要なメソッドは実装しない
 */
public class LinkDocHandlerForBulkRequest implements EntitySetDocHandler {

    private String id;
    private String type;
    private Map<String, Object> source;

    /**
     * コンストラクタ.
     * @param srcHandler LinkDocHandler
     */
    public LinkDocHandlerForBulkRequest(LinkDocHandler srcHandler) {
        this.id = srcHandler.getId();
        this.type = "link";
        this.source = srcHandler.getSource();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Map<String, Object> getSource() {
        return this.source;
    }

    // 以下は使用されないため実装しない

    @Override
    public Long getVersion() {
        return null;
    }

    @Override
    public String getCellId() {
        return null;
    }

    @Override
    public String getBoxId() {
        return null;
    }

    @Override
    public String getNodeId() {
        return null;
    }

    @Override
    public String getEntityTypeId() {
        return null;
    }

    @Override
    public Map<String, Object> getStaticFields() {
        return null;
    }

    @Override
    public Map<String, Object> getDynamicFields() {
        return null;
    }

    @Override
    public Map<String, JSONObject> getAclFields() {
        return null;
    }

    @Override
    public Map<String, Object> getHiddenFields() {
        return null;
    }

    @Override
    public Map<String, Object> getManyToOnelinkId() {
        return null;
    }

    @Override
    public String getStaticFieldsString() {
        return null;
    }

    @Override
    public String getDynamicFieldsString() {
        return null;
    }

    @Override
    public String getHiddenFieldsString() {
        return null;
    }

    @Override
    public String getManyToOnelinkIdString() {
        return null;
    }

    @Override
    public Long getPublished() {
        return null;
    }

    @Override
    public Long getUpdated() {
        return null;
    }

    @Override
    public String getUnitUserName() {
        return null;
    }

    @Override
    public void resolveUnitUserName(Map<String, Object> hiddenFieldsMap) {
    }

    @Override
    public void setId(String id) {
    }

    @Override
    public void setStaticFields(Map<String, Object> staticFields) {
    }

    @Override
    public void setCellId(String cellId) {
    }

    @Override
    public void setBoxId(String boxId) {
    }

    @Override
    public void setNodeId(String nodeId) {
    }

    @Override
    public void setEntityTypeId(String entityTypeId) {
    }

    @Override
    public void setType(String type) {
    }

    @Override
    public void setVersion(Long version) {
    }

    @Override
    public void setHiddenFields(Map<String, Object> hiddenFields) {
    }

    @Override
    public void setManyToOnelinkId(Map<String, Object> links) {
    }

    @Override
    public void setPublished(Long published) {
    }

    @Override
    public void setUpdated(Long updated) {
    }

    @Override
    public void setDynamicFields(Map<String, Object> dynamicFields) {
    }

    @Override
    public OEntityWrapper createOEntity(EdmEntitySet eSet) {
        return null;
    }

    @Override
    public OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList) {
        return null;
    }

    @Override
    public OEntityWrapper createOEntity(EdmEntitySet eSet,
            EdmDataServices metadata,
            Map<String, List<OEntity>> relatedEntitiesList,
            List<EntitySimpleProperty> selectQuery) {
        return null;
    }

    @Override
    public String createEtag() {
        return null;
    }

    @Override
    public void convertAliasToName(EdmDataServices metadata) {
    }
}

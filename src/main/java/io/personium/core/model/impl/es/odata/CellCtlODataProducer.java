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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.impl.es.odata.EsNavigationTargetKeyProperty.NTKPNotFoundException;
import io.personium.core.model.lock.Lock;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.UriUtils;

/**
 * Cell管理オブジェクトの ODataProducer.
 */
public class CellCtlODataProducer extends EsODataProducer {
    Cell cell;
    Logger log = LoggerFactory.getLogger(CellCtlODataProducer.class);

    /**
     * Constructor.
     * @param cell Cell
     */
    public CellCtlODataProducer(final Cell cell) {
        this.cell = cell;
    }

    /**
     * Obtains the service metadata for this producer.
     * @return a fully-constructed metadata object
     */
    @Override
    public EdmDataServices getMetadata() {
        return edmDataServices.build();
    }

    // スキーマ情報
    private static EdmDataServices.Builder edmDataServices = CtlSchema.getEdmDataServicesForCellCtl();

    @Override
    public DataSourceAccessor getAccessorForIndex(final String entitySetName) {
        return null; // 必要時に実装すること
    }

    @Override
    public EntitySetAccessor getAccessorForEntitySet(final String entitySetName) {
        return EsModel.cellCtl(this.cell, entitySetName);
    }

    @Override
    public ODataLinkAccessor getAccessorForLink() {
        return EsModel.cellCtlLink(this.cell);
    }

    @Override
    public DataSourceAccessor getAccessorForLog() {
        return null;
    }

    @Override
    public DataSourceAccessor getAccessorForBatch() {
        return EsModel.batch(this.cell);
    }

    /**
     * CellのIdを返すよう実装.
     * @see io.personium.core.model.impl.es.odata.EsODataProducer#getCellId()
     * @return cell id
     */
    @Override
    public String getCellId() {
        return this.cell.getId();
    }

    /**
     * 登録前処理.
     * @param entitySetName エンティティセット名
     * @param oEntity 登録対象のエンティティ
     * @param docHandler 登録対象のエンティティドックハンドラ
     */
    @Override
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        if (entitySetName.equals(ReceivedMessage.EDM_TYPE_NAME)
                || entitySetName.equals(SentMessage.EDM_TYPE_NAME)) {
            // Removed _Box.Name and add links
            Map<String, Object> staticFields = docHandler.getStaticFields();
            if (staticFields.get(Common.P_BOX_NAME.getName()) != null) {
                Box box = this.cell.getBoxForName((String) staticFields.get(Common.P_BOX_NAME.getName()));
                docHandler.getStaticFields().remove(Common.P_BOX_NAME.getName());

                Map<String, Object> links = docHandler.getManyToOnelinkId();
                links.put("Box", box.getId());
                docHandler.setManyToOnelinkId(links);
            }
        }
    }

    @Override
    public void beforeDelete(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {

        if (!Box.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        // Boxの削除時のみ、Dav管理データを削除
        // entitySetがBoxの場合のみの処理
        EntityResponse er = this.getEntity(entitySetName, oEntityKey, new EntityQueryInfo.Builder().build());

        OEntityWrapper oew = (OEntityWrapper) er.getEntity();

        Box box = new Box(this.cell, oew);

        // このBoxが存在するときのみBoxCmpが必要
        BoxCmp davCmp = ModelFactory.boxCmp(box);
        if (!davCmp.isEmpty()) {
            throw PersoniumCoreException.OData.CONFLICT_HAS_RELATED;
        }
        davCmp.delete(null, false);
        // BoxのCacheクリア
        BoxCache.clear(oEntityKey.asSingleValue().toString(), this.cell);
    }

    @Override
    public void beforeUpdate(final String entitySetName,
            final OEntityKey oEntityKey,
            final EntitySetDocHandler docHandler) {
        if (!Box.EDM_TYPE_NAME.equals(entitySetName)) {
            return;
        }
        // BoxのCacheクリア
        BoxCache.clear(oEntityKey.asSingleValue().toString(), this.cell);
    }

    /**
     * 関係登録/削除、及びメッセージ受信のステータスを変更する.
     * @param entitySet entitySetName
     * @param originalKey 更新対象キー
     * @param status メッセージステータス
     * @return ETag
     */
    public String changeStatusAndUpdateRelation(final EdmEntitySet entitySet,
            final OEntityKey originalKey, final String status) {
        Lock lock = lock();
        try {
            // ESから変更する受信メッセージ情報を取得する
            EntitySetDocHandler entitySetDocHandler = this.retrieveWithKey(entitySet, originalKey);
            if (entitySetDocHandler == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
            }

            // Get Ntkp from entitySet and store it as staticFields.
            // Message does not include _Box.Name in Key, so it requires processing.
            Map<String, Object> staticFields = convertNtkpValueToFields(
                    entitySet, entitySetDocHandler.getStaticFields(), entitySetDocHandler.getManyToOnelinkId());
            entitySetDocHandler.setStaticFields(staticFields);

            // TypeとStatusのチェック
            String type = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_TYPE.getName());
            String currentStatus = (String) entitySetDocHandler.getStaticFields()
                    .get(ReceivedMessage.P_STATUS.getName());

            if (!isValidMessageStatus(type, status)
                    || !isValidRelationStatus(type, status)
                    || !isValidCurrentStatus(type, currentStatus)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.MESSAGE_COMMAND);
            }

            // 関係登録/削除
            updateRelation(entitySetDocHandler, status);

            // 取得した受信メッセージのステータスと更新日を上書きする
            updateStatusOfEntitySetDocHandler(entitySetDocHandler, status);

            // Remove _Box.Name
            entitySetDocHandler.getStaticFields().remove(ReceivedMessage.P_BOX_NAME.getName());

            // ESに保存する
            EntitySetAccessor esType = this.getAccessorForEntitySet(entitySet.getName());
            Long version = entitySetDocHandler.getVersion();
            PersoniumIndexResponse idxRes;
            idxRes = esType.update(entitySetDocHandler.getId(), entitySetDocHandler, version);
            entitySetDocHandler.setVersion(idxRes.version());
            return entitySetDocHandler.createEtag();
        } finally {
            log.debug("unlock");
            lock.release();
        }
    }

    /**
     * Convert NavigationTargetKeyProperty value to staticFields.
     * @param entitySet entitySetName
     * @param staticFields static fields
     * @param links links
     * @return If the converted value is already set staticFields
     */
    protected Map<String, Object> convertNtkpValueToFields(
            EdmEntitySet entitySet, Map<String, Object> staticFields, Map<String, Object> links) {
        Map<String, String> ntkpProperties = new HashMap<String, String>();
        Map<String, String> ntkpValueMap = new HashMap<String, String>();
        getNtkpValueMap(entitySet, ntkpProperties, ntkpValueMap);
        for (Map.Entry<String, String> ntkpProperty : ntkpProperties.entrySet()) {
            String linksKey = getLinkskey(ntkpProperty.getValue());
            if (links.containsKey(linksKey)) {
                String linkId = links.get(linksKey).toString();
                staticFields.put(ntkpProperty.getKey(), ntkpValueMap.get(ntkpProperty.getKey() + linkId));
            } else {
                staticFields.put(ntkpProperty.getKey(), null);
            }
        }
        return staticFields;
    }

    /**
     * Perform relationship registration / deletion.
     * @param entitySetDocHandler Received message
     * @param status Change Status
     */
    private void updateRelation(EntitySetDocHandler entitySetDocHandler, String status) {

        if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
            // Do register / delete Entity / ExtCell
            String type = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_TYPE.getName());

            // Get name to be registered
            String requestRelation = (String) entitySetDocHandler.getStaticFields().get(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
            String name = getNameFromRequestRelation(requestRelation, type);
            // Get box name
            String boxName = getBoxNameFromRequestRelation(requestRelation, type);
            if (boxName == null) {
                // If box can not be found from RequestRelation (RequestRelation is Name only),
                // get BoxName from _ Box.Name
                boxName = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_BOX_NAME.getName());
            }
            // Get cell URL to link
            String extCellUrl = (String) entitySetDocHandler.getStaticFields().get(
                    ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
            if (!extCellUrl.endsWith("/")) {
                extCellUrl += "/";
            }

            Map<String, Object> entityKeyMap = getEntityKeyMapFromType(name, boxName, type);

            Map<String, Object> extCellKeyMap = new HashMap<>();
            extCellKeyMap.put(ExtCell.P_URL.getName(), extCellUrl);

            // registration / deletion
            if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)) {
                registerRelation(Relation.EDM_TYPE_NAME, entityKeyMap, extCellKeyMap);
            } else if (ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
                deleteRelation(Relation.EDM_TYPE_NAME, entityKeyMap, extCellKeyMap);
            } else if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)) {
                registerRelation(Role.EDM_TYPE_NAME, entityKeyMap, extCellKeyMap);
            } else if (ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
                deleteRelation(Role.EDM_TYPE_NAME, entityKeyMap, extCellKeyMap);
            }

        }
    }

    /**
     * Get Name from RequestRelation.
     * @param requestRelation RequestRelation
     * @param type message type
     * @return RelationName
     */
    protected String getNameFromRequestRelation(String requestRelation, String type) {
        String name = null;
        log.debug(String.format("RequestRelation = [%s]", requestRelation));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), requestRelation);
        Pattern pattern = Pattern.compile(getRegexFromType(type));
        Matcher m = pattern.matcher(convertedRequestRelation);
        if (m.matches()) {
            name = m.replaceAll("$3");
        } else {
            name = convertedRequestRelation;
        }
        return name;
    }

    /**
     * Get BoxName from RequestRelation.
     * If RequestRelation is only Name, return null.
     * @param requestRelation RequestRelation
     * @param type message type
     * @return BoxName
     * @throws PersoniumCoreException Box corresponding to the ClassURL can not be found
     */
    protected String getBoxNameFromRequestRelation(String requestRelation, String type)
            throws PersoniumCoreException {
        String boxName = null;
        log.debug(String.format("RequestRelation = [%s]", requestRelation));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), requestRelation);
        Pattern pattern = Pattern.compile(getRegexFromType(type));
        Matcher matcher = pattern.matcher(convertedRequestRelation);
        if (matcher.matches()) {
            String schema = matcher.replaceAll("$1" + "/" + "$2" + "/");
            Box box = this.cell.getBoxForSchema(schema);
            if (box != null) {
                boxName = box.getName();
            } else {
                throw PersoniumCoreException.ReceivedMessage
                        .BOX_THAT_MATCHES_RELATION_CLASS_URL_NOT_EXISTS.params(convertedRequestRelation);
            }
        }
        return boxName;
    }

    /**
     * Get regex from message type.
     * @param type message type
     * @return regex
     */
    private String getRegexFromType(String type) {
        if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)
                || ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
            return Common.PATTERN_RELATION_CLASS_URL;
        } else if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)
                || ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
            return Common.PATTERN_ROLE_CLASS_URL;
        }
        // There is usually no root for else.
        // Return empty for the time being.
        return "";
    }

    /**
     * Get entity key map from message type.
     * @param name name
     * @param boxName box name
     * @param type message type
     * @return key map
     */
    private Map<String, Object> getEntityKeyMapFromType(String name, String boxName, String type) {
        Map<String, Object> entityKeyMap = new HashMap<>();
        if (boxName != null) {
            entityKeyMap.put(Common.P_BOX_NAME.getName(), boxName);
        }
        if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)
                || ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
            entityKeyMap.put(Relation.P_NAME.getName(), name);
        } else if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)
                || ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
            entityKeyMap.put(Role.P_NAME.getName(), name);
        }
        return entityKeyMap;
    }

    /**
     * Register relationship.
     * @param edmType Entity EDM Type
     * @param entityKeyMap Entity key map to be registered
     * @param extCellKeyMap ExtCell key map to link
     */
    private void registerRelation(String edmType, Map<String, Object> entityKeyMap, Map<String, Object> extCellKeyMap) {
        if (getEntitySetDocHandler(edmType, entityKeyMap) == null) {
            // If data does not exist, register newly
            createOEntity(edmType, entityKeyMap);
        }

        if (getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap) == null) {
            // If data does not exist, register newly
            createOEntity(ExtCell.EDM_TYPE_NAME, extCellKeyMap);
        }

        // Create relationship between Entity and ExtCell
        createExtCellLinks(edmType, entityKeyMap, extCellKeyMap);
    }

    /**
     * Delete relationship.
     * @param edmType Entity EDM Type
     * @param entityKeyMap Entity key map to be delete
     * @param extCellKeyMap ExtCell key map to unlink
     */
    private void deleteRelation(String edmType, Map<String, Object> entityKeyMap, Map<String, Object> extCellKeyMap) {
        // Confirm that target Entity exists
        EntitySetDocHandler entity = getEntitySetDocHandler(edmType, entityKeyMap);
        if (entity == null) {
            log.debug(String.format("RequestRelation does not exists. Type [%s]. Keys [%s]",
                    edmType, entityKeyMap.toString()));
            throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                    edmType, entityKeyMap.toString());
        }

        // Confirm that target ExtCell exists
        EntitySetDocHandler extCell = getEntitySetDocHandler(ExtCell.EDM_TYPE_NAME, extCellKeyMap);
        if (extCell == null) {
            log.debug(String.format("RequestRelationTarget does not exists. Type [%s]. Keys [%s].",
                    ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString()));
            throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS.params(
                    ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
        }

        // Delete relationship between Entity and ExtCell
        if (!deleteLinkEntity(entity, extCell)) {
            log.debug(String.format("RequestRelation and RequestRelationTarget does not related. "
                    + "[Type [{%s}]. Keys[{%s}]] - [Type {%s}. Keys[{%s}]]",
                    edmType, entityKeyMap.toString(), ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString()));
            throw PersoniumCoreException.ReceivedMessage.LINK_DOES_NOT_EXISTS.params(
                    edmType, entityKeyMap.toString(), ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
        }
    }

    /**
     * Get EntitySetDocHandler.
     * @param edmType edm type
     * @param entityKeyMap entity key map
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getEntitySetDocHandler(String edmType, Map<String, Object> entityKeyMap) {
        EdmEntitySet edmEntitySet = getMetadata().getEdmEntitySet(edmType);
        OEntityKey oEntityKey = OEntityKey.create(entityKeyMap);

        return retrieveWithKey(edmEntitySet, oEntityKey);
    }

    /**
     * ESに保存.
     * @param typeName 登録するデータのType名
     * @param staticFields 登録するstaticFieldsの値
     */
    private void createOEntity(String typeName, Map<String, Object> staticFields) {
        EntitySetAccessor esType = this.getAccessorForEntitySet(typeName);

        // EntitySetDocHandlerの作成
        EntitySetDocHandler oedh = new OEntityDocHandler();
        oedh.setType(typeName);
        oedh.setId(PersoniumUUID.randomUUID());

        // staticFields
        oedh.setStaticFields(new HashMap<String, Object>(staticFields));

        // Cell, Box, Nodeの紐付
        oedh.setCellId(this.getCellId());
        oedh.setBoxId(null);
        oedh.setNodeId(null);

        // published, updated
        long crrTime = System.currentTimeMillis();
        oedh.setPublished(crrTime);
        oedh.setUpdated(crrTime);

        // 複合キーでNTKPの項目(ex. _EntityType.Name)があれば、リンク情報を設定する
        OEntityKey entityKey = OEntityKey.create(staticFields);
        if (KeyType.COMPLEX.equals(entityKey.getKeyType())) {
            try {
                setLinksFromOEntityKey(entityKey, typeName, oedh);
            } catch (NTKPNotFoundException e) {
                throw PersoniumCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
            }
        }

        // 登録前処理
        this.beforeCreate(typeName, null, oedh);

        // ESに保存する
        esType.create(oedh.getId(), oedh);

        // 登録後処理
        this.afterCreate(typeName, null, oedh);
    }

    /**
     * If there is an item of NTKP in OEntityKey, link information is set.
     * @param key OEntityKey
     * @param typeName EntityTypeName
     * @param oedh Document handler for registration data
     * @throws NTKPNotFoundException The resource specified by NTKP does not exist
     */
    private void setLinksFromOEntityKey(OEntityKey key, String typeName, EntitySetDocHandler oedh)
            throws NTKPNotFoundException {
        // Based on the Property of EntityKey, set link information
        Set<OProperty<?>> properties = key.asComplexProperties();
        EsNavigationTargetKeyProperty esNtkp = new EsNavigationTargetKeyProperty(this.getCellId(), this.getBoxId(),
                this.getNodeId(), typeName, this);
        setLinksForOedh(properties, esNtkp, oedh);
    }

    /**
     * Create $link with ExtCell.
     * @param edmType Entity EDM Type
     * @param entityKeyMap Entity key map
     * @param extCellKeyMap ExtCell key map
     */
    private void createExtCellLinks(String edmType,
            Map<String, Object> entityKeyMap, Map<String, Object> extCellKeyMap) {
        try {
            OEntityKey oEntityKey = OEntityKey.create(entityKeyMap);
            OEntityId entityId = OEntityIds.create(edmType, oEntityKey);
            OEntityKey extCellOEntityKey = OEntityKey.create(extCellKeyMap);
            OEntityId extCellEntityId = OEntityIds.create(ExtCell.EDM_TYPE_NAME, extCellOEntityKey);

            // n:nの場合
            createLinks(entityId, extCellEntityId);
        } catch (PersoniumCoreException e) {
            if (PersoniumCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                // $linksが既に存在する場合
                throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
            }
            throw e;
        }
    }

    /**
     * Messageのステータスバリデート.
     * @param type メッセージタイプ
     * @param status メッセージステータス
     * @return boolean
     */
    protected boolean isValidMessageStatus(String type, String status) {
        // messageの場合のみバリデートをして、read / unread であればtrueを返却する
        if (type.equals(ReceivedMessage.TYPE_MESSAGE)) {
            return ReceivedMessage.STATUS_UNREAD.equals(status)
                    || ReceivedMessage.STATUS_READ.equals(status);
        }
        return true;
    }

    /**
     * Relationのステータスバリデート.
     * @param type メッセージタイプ
     * @param status メッセージステータス
     * @return boolean
     */
    protected boolean isValidRelationStatus(String type, String status) {
        // Validate only for relation registration / deletion, and return true if approved / rejected
        if (type.equals(ReceivedMessage.TYPE_REQ_RELATION_BUILD)
                || type.equals(ReceivedMessage.TYPE_REQ_RELATION_BREAK)
                || type.equals(ReceivedMessage.TYPE_REQ_ROLE_GRANT)
                || type.equals(ReceivedMessage.TYPE_REQ_ROLE_REVOKE)) {
            return ReceivedMessage.STATUS_APPROVED.equals(status)
                    || ReceivedMessage.STATUS_REJECTED.equals(status);
        }
        return true;
    }

    /**
     * 受信メッセージのステータスバリデート.
     * @param type メッセージタイプ
     * @param status メッセージステータス
     * @return boolean
     */
    protected boolean isValidCurrentStatus(String type, String status) {
        // Validate only for relation registration / deletion, and return true if none
        if (type.equals(ReceivedMessage.TYPE_REQ_RELATION_BUILD)
                || type.equals(ReceivedMessage.TYPE_REQ_RELATION_BREAK)
                || type.equals(ReceivedMessage.TYPE_REQ_ROLE_GRANT)
                || type.equals(ReceivedMessage.TYPE_REQ_ROLE_REVOKE)) {
            return ReceivedMessage.STATUS_NONE.equals(status);
        }
        return true;
    }

    /**
     * 受信メッセージのステータスと更新日を上書きする.
     * @param entitySetDocHandler DocHandler
     * @param status メッセージステータス
     */
    private void updateStatusOfEntitySetDocHandler(EntitySetDocHandler entitySetDocHandler, String status) {
        Map<String, Object> staticFields = entitySetDocHandler.getStaticFields();
        // 変更するメッセージステータスをHashedCredentialへ上書きする
        staticFields.put(ReceivedMessage.P_STATUS.getName(), status);
        entitySetDocHandler.setStaticFields(staticFields);

        // 現在時刻を取得して__updatedを上書きする
        long nowTimeMillis = System.currentTimeMillis();
        entitySetDocHandler.setUpdated(nowTimeMillis);
    }

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceEntity ソース側Entity
     * @param targetEntity ターゲット側Entity
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceEntity, EntitySetDocHandler targetEntity) {
    }

    /**
     * 不正なLink情報のチェックを行う.
     * @param sourceDocHandler ソース側Entity
     * @param entity ターゲット側Entity
     * @param targetEntitySetName ターゲットのEntitySet名
     */
    @Override
    protected void checkInvalidLinks(EntitySetDocHandler sourceDocHandler, OEntity entity, String targetEntitySetName) {
    }

    @Override
    public void onChange(String entitySetName) {
    }
}

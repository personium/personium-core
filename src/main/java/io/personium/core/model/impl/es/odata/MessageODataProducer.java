/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.core.OProperties;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.Cell;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.lock.Lock;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.utils.UriUtils;

/**
 * MessageODataProducer.
 */
public class MessageODataProducer extends CellCtlODataProducer {
    DavRsCmp davRsCmp;

    private long currentTimeMillis = System.currentTimeMillis();

    Logger log = LoggerFactory.getLogger(MessageODataProducer.class);

    /**
     * Constructor.
     * @param cell Cell
     * @param davRsCmp DavRsCmp
     */
    public MessageODataProducer(final Cell cell, DavRsCmp davRsCmp) {
        super(cell);
        this.davRsCmp = davRsCmp;
    }

    /**
     * Obtains the service metadata for this producer.
     * @return a fully-constructed metadata object
     */
    @Override
    public EdmDataServices getMetadata() {
        return CtlSchema.getEdmDataServicesForMessage().build();
    }

    /**
     * 登録前処理.
     * @param entitySetName エンティティセット名
     * @param oEntity 登録対象のエンティティ
     * @param docHandler 登録対象のエンティティドックハンドラ
     */
    @Override
    public void beforeCreate(final String entitySetName, final OEntity oEntity, final EntitySetDocHandler docHandler) {
        if (ReceivedMessage.EDM_TYPE_NAME.equals(entitySetName)
                || SentMessage.EDM_TYPE_NAME.equals(entitySetName)) {
            // Removed _Box.Name and add links
            Map<String, Object> staticFields = docHandler.getStaticFields();
            if (staticFields.get(Common.P_BOX_NAME.getName()) != null) {
                Box box = this.cell.getBoxForName((String) staticFields.get(Common.P_BOX_NAME.getName()));
                docHandler.getStaticFields().remove(Common.P_BOX_NAME.getName());

                Map<String, Object> links = docHandler.getManyToOnelinkId();
                links.put("Box", box.getId());
                docHandler.setManyToOnelinkId(links);
            }
        } else {
            super.beforeCreate(entitySetName, oEntity, docHandler);
        }
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

            if (ReceivedMessage.TYPE_MESSAGE.equals(type)) {
                // message
                if (!isValidMessageStatus(status)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.MESSAGE_COMMAND);
                }
            } else if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)
                    || ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
                // relation
                if (isValidCurrentStatus(currentStatus)) {
                    if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                        // check social privilege
                        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.SOCIAL);
                        // create or delete Relation
                        updateRelation(entitySetDocHandler);
                    } else if (!ReceivedMessage.STATUS_REJECTED.equals(status)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                ReceivedMessage.MESSAGE_COMMAND);
                    }
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.MESSAGE_COMMAND);
                }
            } else if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)
                    || ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
                // role
                if (isValidCurrentStatus(currentStatus)) {
                    if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                        // check social privilege
                        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.SOCIAL);
                        // create or delete Role
                        updateRole(entitySetDocHandler);
                    } else if (!ReceivedMessage.STATUS_REJECTED.equals(status)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                ReceivedMessage.MESSAGE_COMMAND);
                    }
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.MESSAGE_COMMAND);
                }
            } else if (ReceivedMessage.TYPE_REQ_RULE_REGISTER.equals(type)
                    || ReceivedMessage.TYPE_REQ_RULE_UNREGISTER.equals(type)) {
                // rule
                if (isValidCurrentStatus(currentStatus)) {
                    if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                        // check rule privilege
                        this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.RULE);
                        // register or unregister rule
                        updateRule(entitySetDocHandler);
                    } else if (!ReceivedMessage.STATUS_REJECTED.equals(status)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                ReceivedMessage.MESSAGE_COMMAND);
                    }
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.MESSAGE_COMMAND);
                }
            } else {
                // never reach here
                String detail = ReceivedMessage.P_TYPE.getName() + ":" + type;
                throw PersoniumCoreException.OData.DETECTED_INTERNAL_DATA_CONFLICT.params(detail);
            }

            // 取得した受信メッセージのステータスと更新日を上書きする
            updateStatusOfEntitySetDocHandler(entitySetDocHandler, status);

            // Remove _Box.Name
            entitySetDocHandler.getStaticFields().remove(Common.P_BOX_NAME.getName());

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
     */
    private void updateRelation(EntitySetDocHandler entitySetDocHandler) {
        Map<String, Object> staticFields = entitySetDocHandler.getStaticFields();
        String type = (String) staticFields.get(ReceivedMessage.P_TYPE.getName());
        String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());

        // Get name to be registered
        String requestRelation = (String) staticFields.get(ReceivedMessage.P_REQUEST_RELATION.getName());
        String name = getNameFromRequestRelation(requestRelation, Common.PATTERN_RELATION_CLASS_URL);
        // Get box name
        String boxName = getBoxNameFromRequestRelation(requestRelation, Common.PATTERN_RELATION_CLASS_URL);
        if (boxName == null) {
            // If box can not be found from RequestRelation (RequestRelation is Name only),
            // get BoxName from _ Box.Name
            boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
        }
        // Get cell URL to link
        String extCellUrl = (String) staticFields.get(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Relation.P_NAME.getName(), name);
        if (boxName != null) {
            entityKeyMap.put(Common.P_BOX_NAME.getName(), boxName);
        }
        OEntityKey entityKey = OEntityKey.create(entityKeyMap);
        EdmEntitySet edmEntitySet = getMetadata().findEdmEntitySet(Relation.EDM_TYPE_NAME);
        EntitySetDocHandler docHandler = retrieveWithKey(edmEntitySet, entityKey);
        // If Relation does not exist, then throw exception.
        if (docHandler == null) {
            throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                    Relation.EDM_TYPE_NAME, entityKeyMap.toString());
        }

        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put(ExtCell.P_URL.getName(), extCellUrl);
        OEntityKey extCellKey = OEntityKey.create(extCellKeyMap);
        EdmEntitySet extCellEdmEntitySet = getMetadata().findEdmEntitySet(ExtCell.EDM_TYPE_NAME);
        EntitySetDocHandler extCellDocHandler = retrieveWithKey(extCellEdmEntitySet, extCellKey);

        OEntityId entityId = OEntityIds.create(Relation.EDM_TYPE_NAME, entityKey);
        OEntityId extCellEntityId = OEntityIds.create(ExtCell.EDM_TYPE_NAME, extCellKey);

        String targetNavProp = "_" + ExtCell.EDM_TYPE_NAME;

        // prepare event posting
        String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toString());
        String extCellKeyString = AbstractODataResource.replaceDummyKeyToNull(extCellKey.toString());
        String info = "approved for message " + messageId;
        EventBus eventBus = this.cell.getEventBus();

        // registration / deletion
        if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)) {
            if (extCellDocHandler == null) {
                extCellKeyMap.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
                extCellKeyMap.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));
                OEntityWrapper oew = createOEntityWrapper(extCellEdmEntitySet, extCellKeyMap, extCellKey);
                createEntityWithoutLock(ExtCell.EDM_TYPE_NAME, oew);

                // post event
                String object = String.format("%s:/__ctl/%s%s",
                        UriUtils.SCHEME_LOCALCELL, ExtCell.EDM_TYPE_NAME, extCellKeyString);
                String extCellType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                        + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
                PersoniumEvent ev = new PersoniumEvent(null, null, extCellType, object, info, null);
                eventBus.post(ev);
            }
            try {
                createLinkWithoutLock(entityId, targetNavProp, extCellEntityId);

                // post event
                String object = String.format("%s:/__ctl/%s%s/$links/%s%s",
                        UriUtils.SCHEME_LOCALCELL, Relation.EDM_TYPE_NAME, keyString, targetNavProp, extCellKeyString);
                String relationType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                        + Relation.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR
                        + PersoniumEventType.Operation.LINK + PersoniumEventType.SEPALATOR
                        + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
                PersoniumEvent ev = new PersoniumEvent(null, null, relationType, object, info, null);
                eventBus.post(ev);
            } catch (PersoniumCoreException e) {
                if (PersoniumCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                    throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
                }
                throw e;
            }
        } else if (ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
            if (extCellDocHandler == null) {
                throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                        ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            }
            deleteLinkWithoutLock(entityId, targetNavProp, extCellKey);

            // post event
            String object = String.format("%s:/__ctl/%s%s/$links/%s%s",
                    UriUtils.SCHEME_LOCALCELL, Relation.EDM_TYPE_NAME, keyString, targetNavProp, extCellKeyString);
            String relationType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                    + Relation.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR
                    + PersoniumEventType.Operation.LINK + PersoniumEventType.SEPALATOR
                    + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.DELETE;
            PersoniumEvent ev = new PersoniumEvent(null, null, relationType, object, info, null);
            eventBus.post(ev);
        }
    }

    private void updateRole(EntitySetDocHandler entitySetDocHandler) {
        Map<String, Object> staticFields = entitySetDocHandler.getStaticFields();
        String type = (String) staticFields.get(ReceivedMessage.P_TYPE.getName());
        String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());

        // Get name to be registered
        String requestRelation = (String) staticFields.get(ReceivedMessage.P_REQUEST_RELATION.getName());
        String name = getNameFromRequestRelation(requestRelation, Common.PATTERN_ROLE_CLASS_URL);
        // Get box name
        String boxName = getBoxNameFromRequestRelation(requestRelation, Common.PATTERN_ROLE_CLASS_URL);
        if (boxName == null) {
            // If box can not be found from RequestRelation (RequestRelation is Name only),
            // get BoxName from _ Box.Name
            boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
        }
        // Get cell URL to link
        String extCellUrl = (String) staticFields.get(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Common.P_NAME.getName(), name);
        if (boxName != null) {
            entityKeyMap.put(Common.P_BOX_NAME.getName(), boxName);
        }
        OEntityKey entityKey = OEntityKey.create(entityKeyMap);
        EdmEntitySet edmEntitySet = getMetadata().findEdmEntitySet(Role.EDM_TYPE_NAME);
        EntitySetDocHandler docHandler = retrieveWithKey(edmEntitySet, entityKey);
        // If Role does not exist, then throw exception.
        if (docHandler == null) {
            throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                    Role.EDM_TYPE_NAME, entityKeyMap.toString());
        }

        Map<String, Object> extCellKeyMap = new HashMap<>();
        extCellKeyMap.put(ExtCell.P_URL.getName(), extCellUrl);
        OEntityKey extCellKey = OEntityKey.create(extCellKeyMap);
        EdmEntitySet extCellEdmEntitySet = getMetadata().findEdmEntitySet(ExtCell.EDM_TYPE_NAME);
        EntitySetDocHandler extCellDocHandler = retrieveWithKey(extCellEdmEntitySet, extCellKey);

        OEntityId entityId = OEntityIds.create(Role.EDM_TYPE_NAME, entityKey);
        OEntityId extCellEntityId = OEntityIds.create(ExtCell.EDM_TYPE_NAME, extCellKey);

        String targetNavProp = "_" + ExtCell.EDM_TYPE_NAME;

        // prepare event posting
        String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toString());
        String extCellKeyString = AbstractODataResource.replaceDummyKeyToNull(extCellKey.toString());
        String info = "approved for message " + messageId;
        EventBus eventBus = this.cell.getEventBus();

        // registration / deletion
        if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)) {
            if (extCellDocHandler == null) {
                extCellKeyMap.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
                extCellKeyMap.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));
                OEntityWrapper oew = createOEntityWrapper(extCellEdmEntitySet, extCellKeyMap, extCellKey);
                createEntityWithoutLock(ExtCell.EDM_TYPE_NAME, oew);

                // post event
                String object = String.format("%s:/__ctl/%s%s",
                        UriUtils.SCHEME_LOCALCELL, ExtCell.EDM_TYPE_NAME, extCellKeyString);
                String extCellType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                        + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
                PersoniumEvent ev = new PersoniumEvent(null, null, extCellType, object, info, null);
                eventBus.post(ev);
            }
            try {
                createLinkWithoutLock(entityId, targetNavProp, extCellEntityId);

                // post event
                String object = String.format("%s:/__ctl/%s%s/$links/%s%s",
                        UriUtils.SCHEME_LOCALCELL, Role.EDM_TYPE_NAME, keyString, targetNavProp, extCellKeyString);
                String roleType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                        + Role.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR
                        + PersoniumEventType.Operation.LINK + PersoniumEventType.SEPALATOR
                        + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
                PersoniumEvent ev = new PersoniumEvent(null, null, roleType, object, info, null);
                eventBus.post(ev);
            } catch (PersoniumCoreException e) {
                if (PersoniumCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                    throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
                }
                throw e;
            }
        } else if (ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
            if (extCellDocHandler == null) {
                throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                        ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            }
            deleteLinkWithoutLock(entityId, targetNavProp, extCellKey);

            // post event
            String object = String.format("%s:/__ctl/%s%s/$links/%s%s",
                    UriUtils.SCHEME_LOCALCELL, Role.EDM_TYPE_NAME, keyString, targetNavProp, extCellKeyString);
            String roleType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                    + Role.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR
                    + PersoniumEventType.Operation.LINK + PersoniumEventType.SEPALATOR
                    + ExtCell.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.DELETE;
            PersoniumEvent ev = new PersoniumEvent(null, null, roleType, object, info, null);
            eventBus.post(ev);
        }
    }

    private void updateRule(EntitySetDocHandler entitySetDocHandler) {
        Map<String, Object> staticFields = entitySetDocHandler.getStaticFields();
        String type = (String) staticFields.get(ReceivedMessage.P_TYPE.getName());
        Map<String, Object> requestRule =
                (Map<String, Object>) staticFields.get(ReceivedMessage.P_REQUEST_RULE.getName());
        String boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
        String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());

        log.info("requestRule: " + requestRule);

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Rule.P_NAME.getName(), requestRule.get(ReceivedMessage.P_REQUESTRULE_NAME.getName()));
        if (boxName != null) {
            entityKeyMap.put(Common.P_BOX_NAME.getName(), boxName);
        }
        OEntityKey entityKey = OEntityKey.create(entityKeyMap);
        EdmEntitySet edmEntitySet = getMetadata().findEdmEntitySet(Rule.EDM_TYPE_NAME);
        entityKey = AbstractODataResource.normalizeOEntityKey(entityKey, edmEntitySet);

        if (ReceivedMessage.TYPE_REQ_RULE_REGISTER.equals(type)) {
            // copy rule
            Map<String, Object> rule = new HashMap<>();
            rule.put(Rule.P_NAME.getName(), requestRule.get(ReceivedMessage.P_REQUESTRULE_NAME.getName()));
            rule.put(Rule.P_SUBJECT.getName(), requestRule.get(Rule.P_SUBJECT.getName()));
            rule.put(Rule.P_TYPE.getName(), requestRule.get(Rule.P_TYPE.getName()));
            rule.put(Rule.P_OBJECT.getName(), requestRule.get(Rule.P_OBJECT.getName()));
            rule.put(Rule.P_INFO.getName(), requestRule.get(Rule.P_INFO.getName()));
            rule.put(Rule.P_ACTION.getName(), requestRule.get(Rule.P_ACTION.getName()));
            rule.put(Rule.P_SERVICE.getName(), requestRule.get(Rule.P_SERVICE.getName()));

            // Rule settings
            //   External: false
            //   _Box.Name: null or ReceivedMessage._Box.Name
            //   __published, __updated: current time
            rule.put(Rule.P_EXTERNAL.getName(), PersoniumEvent.INTERNAL_EVENT);
            rule.put(Common.P_BOX_NAME.getName(), boxName);
            rule.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
            rule.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));

            OEntityWrapper oew = createOEntityWrapper(edmEntitySet, rule, entityKey);
            createEntityWithoutLock(Rule.EDM_TYPE_NAME, oew);

            // post rule event to eventBus
            String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toKeyString());
            String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, Rule.EDM_TYPE_NAME, keyString);
            String info = "approved for message " + messageId;
            String ruleType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                    + Rule.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
            PersoniumEvent ev = new PersoniumEvent(null, null, ruleType, object, info, null);
            EventBus eventBus = this.cell.getEventBus();
            eventBus.post(ev);
        } else if (ReceivedMessage.TYPE_REQ_RULE_UNREGISTER.equals(type)) {
            // deletion
            deleteEntityWithoutLock(Rule.EDM_TYPE_NAME, entityKey);

            // post rule event to eventBus
            String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toKeyString());
            String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, Rule.EDM_TYPE_NAME, keyString);
            String info = "approved for message " + messageId;
            String ruleType = PersoniumEventType.Category.CELLCTL + PersoniumEventType.SEPALATOR
                    + Rule.EDM_TYPE_NAME + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.DELETE;
            PersoniumEvent ev = new PersoniumEvent(null, null, ruleType, object, info, null);
            EventBus eventBus = this.cell.getEventBus();
            eventBus.post(ev);
        }
    }

    // create OEntityWrapper
    private OEntityWrapper createOEntityWrapper(EdmEntitySet entitySet,
            Map<String, Object> request, OEntityKey entityKey) {
        List<OProperty<?>> properties = new ArrayList<OProperty<?>>();

        for (Map.Entry<String, Object> entry : request.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            EdmProperty ep = entitySet.getType().findProperty(key);
            if (ep != null) {
                EdmType type = ep.getType();
                if (type.isSimple()) {
                    properties.add(OProperties.simple(key, (EdmSimpleType<?>) type, value));
                }
            }
        }

        OEntity entity = OEntities.create(entitySet, entitySet.getType(), entityKey, properties, null);
        String uuid = PersoniumUUID.randomUUID();
        return new OEntityWrapper(uuid, entity, null);
    }

    /**
     * Get Name from RequestRelation.
     * @param requestRelation RequestRelation
     * @param regex pattern of class url
     * @return RelationName
     */
    protected String getNameFromRequestRelation(String requestRelation, String regex) {
        String name = null;
        log.debug(String.format("RequestRelation = [%s]", requestRelation));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), requestRelation);
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(convertedRequestRelation);
        if (m.matches()) {
            name = m.replaceAll("$2");
        } else {
            name = convertedRequestRelation;
        }
        return name;
    }

    /**
     * Get BoxName from RequestRelation.
     * If RequestRelation is only Name, return null.
     * @param requestRelation RequestRelation
     * @param regex pattern of class url
     * @return BoxName
     * @throws PersoniumCoreException Box corresponding to the ClassURL can not be found
     */
    protected String getBoxNameFromRequestRelation(String requestRelation, String regex)
            throws PersoniumCoreException {
        String boxName = null;
        log.debug(String.format("RequestRelation = [%s]", requestRelation));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), requestRelation);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(convertedRequestRelation);
        if (matcher.matches()) {
            String schema = matcher.replaceAll("$1");
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
     * Messageのステータスバリデート.
     * @param status メッセージステータス
     * @return boolean
     */
    protected boolean isValidMessageStatus(String status) {
        // messageの場合のみバリデートをして、read / unread であればtrueを返却する
        return ReceivedMessage.STATUS_UNREAD.equals(status)
                || ReceivedMessage.STATUS_READ.equals(status);
    }

    /**
     * 受信メッセージのステータスバリデート.
     * @param status メッセージステータス
     * @return boolean
     */
    protected boolean isValidCurrentStatus(String status) {
        // return true if none
        return ReceivedMessage.STATUS_NONE.equals(status);
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
     * Get current time.
     * @return the currentTimeMillis
     */
    private long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

}

/**
 * personium.io
 * Copyright 2017-2018 FUJITSU LIMITED
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
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
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
import io.personium.core.model.ctl.RequestObject;
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
     * Pre-registration processing.
     * @param entitySetName Entity set name
     * @param oEntity entity to be registered
     * @param docHandler Entity dock handler to register
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
     * Change the status of relation registration / deletion and message reception.
     * @param entitySet entitySetName
     * @param originalKey Key to be updated
     * @param status Message status
     * @return ETag
     */
    @SuppressWarnings("unchecked")
    public String changeStatusAndUpdateRelation(final EdmEntitySet entitySet,
            final OEntityKey originalKey, final String status) {
        Lock lock = lock();
        try {
            //Acquire received message information to be changed from ES
            EntitySetDocHandler entitySetDocHandler = this.retrieveWithKey(entitySet, originalKey);
            if (entitySetDocHandler == null) {
                throw PersoniumCoreException.OData.NO_SUCH_ENTITY;
            }

            // Get Ntkp from entitySet and store it as staticFields.
            // Message does not include _Box.Name in Key, so it requires processing.
            Map<String, Object> staticFields = convertNtkpValueToFields(
                    entitySet, entitySetDocHandler.getStaticFields(), entitySetDocHandler.getManyToOnelinkId());
            entitySetDocHandler.setStaticFields(staticFields);

            //Check Type and Status
            String type = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_TYPE.getName());
            String currentStatus = (String) entitySetDocHandler.getStaticFields()
                    .get(ReceivedMessage.P_STATUS.getName());

            if (ReceivedMessage.TYPE_MESSAGE.equals(type)) {
                // message
                if (!isValidMessageStatus(status)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            ReceivedMessage.MESSAGE_COMMAND);
                }
            } else if (ReceivedMessage.TYPE_REQUEST.equals(type)) {
                List<Map<String, String>> requestObjects = (List<Map<String, String>>) entitySetDocHandler
                        .getStaticFields().get(ReceivedMessage.P_REQUEST_OBJECTS.getName());
                for (Map<String, String> requestObject : requestObjects) {
                    String requestType = requestObject.get(RequestObject.P_REQUEST_TYPE.getName());
                    if (RequestObject.REQUEST_TYPE_RELATION_ADD.equals(requestType)
                            || RequestObject.REQUEST_TYPE_RELATION_REMOVE.equals(requestType)) {
                        // relation
                        if (isValidCurrentStatus(currentStatus)) {
                            if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                                // check social privilege
                                davRsCmp.checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.SOCIAL);
                                // create or delete Relation
                                String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());
                                String boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
                                updateRelation(messageId, boxName, requestObject);
                            } else if (!ReceivedMessage.STATUS_REJECTED.equals(status)) {
                                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                        ReceivedMessage.MESSAGE_COMMAND);
                            }
                        } else {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    ReceivedMessage.MESSAGE_COMMAND);
                        }
                    } else if (RequestObject.REQUEST_TYPE_ROLE_ADD.equals(requestType)
                            || RequestObject.REQUEST_TYPE_ROLE_REMOVE.equals(requestType)) {
                        // role
                        if (isValidCurrentStatus(currentStatus)) {
                            if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                                // check social privilege
                                davRsCmp.checkAccessContext(davRsCmp.getAccessContext(), CellPrivilege.SOCIAL);
                                // create or delete Role
                                String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());
                                String boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
                                updateRole(messageId, boxName, requestObject);
                            } else if (!ReceivedMessage.STATUS_REJECTED.equals(status)) {
                                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                        ReceivedMessage.MESSAGE_COMMAND);
                            }
                        } else {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    ReceivedMessage.MESSAGE_COMMAND);
                        }
                    } else if (RequestObject.REQUEST_TYPE_RULE_ADD.equals(requestType)
                            || RequestObject.REQUEST_TYPE_RULE_REMOVE.equals(requestType)) {
                        // rule
                        if (isValidCurrentStatus(currentStatus)) {
                            if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
                                // check rule privilege
                                this.davRsCmp.checkAccessContext(this.davRsCmp.getAccessContext(), CellPrivilege.RULE);
                                // register or unregister rule
                                String messageId = (String) staticFields.get(ReceivedMessage.P_ID.getName());
                                String boxName = (String) staticFields.get(Common.P_BOX_NAME.getName());
                                updateRule(messageId, boxName, requestObject);
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
                }
            }

            //Overwrite the status and update date of the acquired received message
            updateStatusOfEntitySetDocHandler(entitySetDocHandler, status);

            // Remove _Box.Name
            entitySetDocHandler.getStaticFields().remove(Common.P_BOX_NAME.getName());

            //Save to ES
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
     * @param messageId MessageID
     * @param linkedBoxName _Box.Name
     * @param requestObject RequestObject
     */
    private void updateRelation(String messageId, String linkedBoxName, Map<String, String> requestObject) {
        String requestType = requestObject.get(RequestObject.P_REQUEST_TYPE.getName());
        String name = requestObject.get(RequestObject.P_NAME.getName());
        String classUrl = requestObject.get(RequestObject.P_CLASS_URL.getName());
        String targetUrl = requestObject.get(RequestObject.P_TARGET_URL.getName());

        String relationName = "";
        String boxName = "";
        // Validation has confirmed that it contains values only in name or classUrl.
        if (name != null) {
            relationName = name;
            boxName = linkedBoxName;
        } else if (classUrl != null) {
            relationName = getNameFromClassUrl(classUrl, Common.PATTERN_RELATION_CLASS_URL);
            boxName = getBoxNameFromClassUrl(classUrl, Common.PATTERN_RELATION_CLASS_URL);
        }

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Relation.P_NAME.getName(), relationName);
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
        extCellKeyMap.put(Common.P_URL.getName(), targetUrl);
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
        if (RequestObject.REQUEST_TYPE_RELATION_ADD.equals(requestType)) {
            if (extCellDocHandler == null) {
                extCellKeyMap.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
                extCellKeyMap.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));
                OEntityWrapper oew = createOEntityWrapper(extCellEdmEntitySet, extCellKeyMap, extCellKey);
                createEntityWithoutLock(ExtCell.EDM_TYPE_NAME, oew);

                // post event
                String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                        .append(":/__ctl/")
                        .append(ExtCell.EDM_TYPE_NAME)
                        .append(extCellKeyString)
                        .toString();
                String extCellType = PersoniumEventType.cellctl(
                        ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
                PersoniumEvent ev = new PersoniumEvent.Builder()
                        .type(extCellType)
                        .object(object)
                        .info(info)
                        .davRsCmp(this.davRsCmp)
                        .build();
                eventBus.post(ev);
            }
            try {
                createLinkWithoutLock(entityId, targetNavProp, extCellEntityId);

                // post event
                String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                        .append(":/__ctl/")
                        .append(Relation.EDM_TYPE_NAME)
                        .append(keyString)
                        .append("/$links/")
                        .append(targetNavProp)
                        .append(extCellKeyString)
                        .toString();
                String relationType = PersoniumEventType.cellctlLink(
                        Relation.EDM_TYPE_NAME, ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
                PersoniumEvent ev = new PersoniumEvent.Builder()
                        .type(relationType)
                        .object(object)
                        .info(info)
                        .davRsCmp(this.davRsCmp)
                        .build();
                eventBus.post(ev);
            } catch (PersoniumCoreException e) {
                if (PersoniumCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                    throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
                }
                throw e;
            }
        } else if (RequestObject.REQUEST_TYPE_RELATION_REMOVE.equals(requestType)) {
            if (extCellDocHandler == null) {
                throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                        ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            }
            deleteLinkWithoutLock(entityId, targetNavProp, extCellKey);

            // post event
            String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                    .append(":/__ctl/")
                    .append(Relation.EDM_TYPE_NAME)
                    .append(keyString)
                    .append("/$links/")
                    .append(targetNavProp)
                    .append(extCellKeyString)
                    .toString();
            String relationType = PersoniumEventType.cellctlLink(
                    Relation.EDM_TYPE_NAME, ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
            PersoniumEvent ev = new PersoniumEvent.Builder()
                    .type(relationType)
                    .object(object)
                    .info(info)
                    .davRsCmp(this.davRsCmp)
                    .build();
            eventBus.post(ev);
        }
    }

    /**
     * Perform role grant / revoke.
     * @param messageId MessageID
     * @param linkedBoxName _Box.Name
     * @param requestObject RequestObject
     */
    private void updateRole(String messageId, String linkedBoxName, Map<String, String> requestObject) {
        String requestType = requestObject.get(RequestObject.P_REQUEST_TYPE.getName());
        String name = requestObject.get(RequestObject.P_NAME.getName());
        String classUrl = requestObject.get(RequestObject.P_CLASS_URL.getName());
        String targetUrl = requestObject.get(RequestObject.P_TARGET_URL.getName());

        String roleName = "";
        String boxName = "";
        // Validation has confirmed that it contains values only in name or classUrl.
        if (name != null) {
            roleName = name;
            boxName = linkedBoxName;
        } else if (classUrl != null) {
            roleName = getNameFromClassUrl(classUrl, Common.PATTERN_ROLE_CLASS_URL);
            boxName = getBoxNameFromClassUrl(classUrl, Common.PATTERN_ROLE_CLASS_URL);
        }

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Common.P_NAME.getName(), roleName);
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
        extCellKeyMap.put(Common.P_URL.getName(), targetUrl);
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
        if (RequestObject.REQUEST_TYPE_ROLE_ADD.equals(requestType)) {
            if (extCellDocHandler == null) {
                extCellKeyMap.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
                extCellKeyMap.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));
                OEntityWrapper oew = createOEntityWrapper(extCellEdmEntitySet, extCellKeyMap, extCellKey);
                createEntityWithoutLock(ExtCell.EDM_TYPE_NAME, oew);

                // post event
                String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                        .append(":/__ctl/")
                        .append(ExtCell.EDM_TYPE_NAME)
                        .append(extCellKeyString)
                        .toString();
                String extCellType = PersoniumEventType.cellctl(
                        ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
                PersoniumEvent ev = new PersoniumEvent.Builder()
                        .type(extCellType)
                        .object(object)
                        .info(info)
                        .davRsCmp(this.davRsCmp)
                        .build();
                eventBus.post(ev);
            }
            try {
                createLinkWithoutLock(entityId, targetNavProp, extCellEntityId);

                // post event
                String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                        .append(":/__ctl/")
                        .append(Role.EDM_TYPE_NAME)
                        .append(keyString)
                        .append("/$links/")
                        .append(targetNavProp)
                        .append(extCellKeyString)
                        .toString();
                String roleType = PersoniumEventType.cellctlLink(
                        Role.EDM_TYPE_NAME, ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
                PersoniumEvent ev = new PersoniumEvent.Builder()
                        .type(roleType)
                        .object(object)
                        .info(info)
                        .davRsCmp(this.davRsCmp)
                        .build();
                eventBus.post(ev);
            } catch (PersoniumCoreException e) {
                if (PersoniumCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                    throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_EXISTS_ERROR;
                }
                throw e;
            }
        } else if (RequestObject.REQUEST_TYPE_ROLE_REMOVE.equals(requestType)) {
            if (extCellDocHandler == null) {
                throw PersoniumCoreException.ReceivedMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(
                        ExtCell.EDM_TYPE_NAME, extCellKeyMap.toString());
            }
            deleteLinkWithoutLock(entityId, targetNavProp, extCellKey);

            // post event
            String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                    .append(":/__ctl/")
                    .append(Role.EDM_TYPE_NAME)
                    .append(keyString)
                    .append("/$links/")
                    .append(targetNavProp)
                    .append(extCellKeyString)
                    .toString();
            String roleType = PersoniumEventType.cellctlLink(
                    Role.EDM_TYPE_NAME, ExtCell.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
            PersoniumEvent ev = new PersoniumEvent.Builder()
                    .type(roleType)
                    .object(object)
                    .info(info)
                    .davRsCmp(this.davRsCmp)
                    .build();
            eventBus.post(ev);
        }
    }

    /**
     * Perform rule regist / unregist.
     * @param messageId MessageID
     * @param linkedBoxName _Box.Name
     * @param requestObject RequestObject
     */
    private void updateRule(String messageId, String linkedBoxName, Map<String, String> requestObject) {
        String requestType = requestObject.get(RequestObject.P_REQUEST_TYPE.getName());

        log.info("requestRule: " + requestObject);

        Map<String, Object> entityKeyMap = new HashMap<>();
        entityKeyMap.put(Rule.P_NAME.getName(), requestObject.get(RequestObject.P_NAME.getName()));
        if (linkedBoxName != null) {
            entityKeyMap.put(Common.P_BOX_NAME.getName(), linkedBoxName);
        }
        OEntityKey entityKey = OEntityKey.create(entityKeyMap);
        EdmEntitySet edmEntitySet = getMetadata().findEdmEntitySet(Rule.EDM_TYPE_NAME);
        entityKey = AbstractODataResource.normalizeOEntityKey(entityKey, edmEntitySet);

        if (RequestObject.REQUEST_TYPE_RULE_ADD.equals(requestType)) {
            // copy rule
            Map<String, Object> rule = new HashMap<>();
            rule.put(Rule.P_NAME.getName(), requestObject.get(RequestObject.P_NAME.getName()));
            rule.put(Rule.P_SUBJECT.getName(), requestObject.get(Rule.P_SUBJECT.getName()));
            rule.put(Rule.P_TYPE.getName(), requestObject.get(Rule.P_TYPE.getName()));
            rule.put(Rule.P_OBJECT.getName(), requestObject.get(Rule.P_OBJECT.getName()));
            rule.put(Rule.P_INFO.getName(), requestObject.get(Rule.P_INFO.getName()));
            rule.put(Rule.P_ACTION.getName(), requestObject.get(Rule.P_ACTION.getName()));
            rule.put(Rule.P_SERVICE.getName(), requestObject.get(RequestObject.P_TARGET_URL.getName()));

            // Rule settings
            //   External: false
            //   _Box.Name: null or ReceivedMessage._Box.Name
            //   __published, __updated: current time
            rule.put(Rule.P_EXTERNAL.getName(), PersoniumEvent.INTERNAL_EVENT);
            rule.put(Common.P_BOX_NAME.getName(), linkedBoxName);
            rule.put(Common.P_PUBLISHED.getName(), new Date(getCurrentTimeMillis()));
            rule.put(Common.P_UPDATED.getName(), new Date(getCurrentTimeMillis()));

            OEntityWrapper oew = createOEntityWrapper(edmEntitySet, rule, entityKey);
            createEntityWithoutLock(Rule.EDM_TYPE_NAME, oew);

            // post rule event to eventBus
            String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toKeyString());
            String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                    .append(":/__ctl/")
                    .append(Rule.EDM_TYPE_NAME)
                    .append(keyString)
                    .toString();
            String info = "approved for message " + messageId;
            String ruleType = PersoniumEventType.cellctl(
                    Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.CREATE);
            PersoniumEvent ev = new PersoniumEvent.Builder()
                    .type(ruleType)
                    .object(object)
                    .info(info)
                    .davRsCmp(this.davRsCmp)
                    .build();
            EventBus eventBus = this.cell.getEventBus();
            eventBus.post(ev);
        } else if (RequestObject.REQUEST_TYPE_RULE_REMOVE.equals(requestType)) {
            // deletion
            deleteEntityWithoutLock(Rule.EDM_TYPE_NAME, entityKey);

            // post rule event to eventBus
            String keyString = AbstractODataResource.replaceDummyKeyToNull(entityKey.toKeyString());
            String object = new StringBuilder(UriUtils.SCHEME_LOCALCELL)
                    .append(":/__ctl/")
                    .append(Rule.EDM_TYPE_NAME)
                    .append(keyString)
                    .toString();
            String info = "approved for message " + messageId;
            String ruleType = PersoniumEventType.cellctl(
                    Rule.EDM_TYPE_NAME, PersoniumEventType.Operation.DELETE);
            PersoniumEvent ev = new PersoniumEvent.Builder()
                    .type(ruleType)
                    .object(object)
                    .info(info)
                    .davRsCmp(this.davRsCmp)
                    .build();
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
     * Get Name from ClassUrl.
     * @param classUrl ClassUrl
     * @param regex pattern of class url
     * @return RelationName
     */
    protected String getNameFromClassUrl(String classUrl, String regex) {
        String name = null;
        log.debug(String.format("ClassUrl = [%s]", classUrl));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), classUrl);
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
     * Get BoxName from ClassUrl.
     * @param classUrl ClassUrl
     * @param regex pattern of class url
     * @return BoxName
     * @throws PersoniumCoreException Box corresponding to the ClassURL can not be found
     */
    protected String getBoxNameFromClassUrl(String classUrl, String regex)
            throws PersoniumCoreException {
        String boxName = null;
        log.debug(String.format("RequestRelation = [%s]", classUrl));

        // convert localunitUrl to unitUrl
        String convertedRequestRelation = UriUtils.convertSchemeFromLocalUnitToHttp(cell.getUnitUrl(), classUrl);
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
     * Message status validation.
     * @param status Message status
     * @return boolean
     */
    protected boolean isValidMessageStatus(String status) {
        //Validate only for message and return true if read / unread
        return ReceivedMessage.STATUS_UNREAD.equals(status)
                || ReceivedMessage.STATUS_READ.equals(status);
    }

    /**
     * Status validation of incoming messages.
     * @param status Message status
     * @return boolean
     */
    protected boolean isValidCurrentStatus(String status) {
        // return true if none
        return ReceivedMessage.STATUS_NONE.equals(status);
    }

    /**
     * Overwrite the status of the received message and the update date.
     * @param entitySetDocHandler DocHandler
     * @param status Message status
     */
    private void updateStatusOfEntitySetDocHandler(EntitySetDocHandler entitySetDocHandler, String status) {
        Map<String, Object> staticFields = entitySetDocHandler.getStaticFields();
        //Overwrite message status to be changed to HashedCredential
        staticFields.put(ReceivedMessage.P_STATUS.getName(), status);
        entitySetDocHandler.setStaticFields(staticFields);

        //Get current time and overwrite __updated
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

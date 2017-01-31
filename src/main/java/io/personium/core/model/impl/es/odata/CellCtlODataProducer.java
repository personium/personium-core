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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.DcCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.ModelFactory;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.DataSourceAccessor;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.cache.BoxCache;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;
import io.personium.core.model.lock.Lock;
import io.personium.core.odata.OEntityWrapper;

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
        if (entitySetName.equals(ReceivedMessagePort.EDM_TYPE_NAME)) {
            // 受信メッセージの場合は登録データから「_Box.Name」を削除
            Map<String, Object> dynamic = docHandler.getDynamicFields();
            dynamic.remove(ReceivedMessagePort.P_BOX_NAME.getName());
            docHandler.setDynamicFields(dynamic);
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
            throw DcCoreException.OData.CONFLICT_HAS_RELATED;
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
                throw DcCoreException.OData.NO_SUCH_ENTITY;
            }

            // TypeとStatusのチェック
            String type = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_TYPE.getName());
            String currentStatus = (String) entitySetDocHandler.getStaticFields()
                    .get(ReceivedMessage.P_STATUS.getName());

            if (!isValidMessageStatus(type, status)
                    || !isValidRelationStatus(type, status)
                    || !isValidCurrentStatus(type, currentStatus)) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.MESSAGE_COMMAND);
            }

            // 関係登録/削除
            updateRelation(entitySetDocHandler, status);

            // 取得した受信メッセージのステータスと更新日を上書きする
            updateStatusOfEntitySetDocHandler(entitySetDocHandler, status);

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
     * 関係登録/削除を行う.
     * @param entitySetDocHandler 受信メッセージ
     * @param status 変更するStatus
     */
    private void updateRelation(EntitySetDocHandler entitySetDocHandler, String status) {
        String type = (String) entitySetDocHandler.getStaticFields().get(ReceivedMessage.P_TYPE.getName());

        if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
            // approvedの場合、Relation/ExtCellの登録/削除を行う

            if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)) {
                buildRelation(entitySetDocHandler);
            } else if (ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
                breakRelation(entitySetDocHandler);
            }

        }
    }

    /**
     * 関係登録を行う. <br />
     * 現状はRequestRelationで指定されたURLのRelation名のみ取得して、対象の受信メッセージのCellに <br />
     * Boxと紐付かないRelationを登録する.
     * @param entitySetDocHandler 受信メッセージ
     */
    private void buildRelation(EntitySetDocHandler entitySetDocHandler) {

        // 登録対象のRelation名取得
        String requestRelation = (String) entitySetDocHandler.getStaticFields().get(
                ReceivedMessage.P_REQUEST_RELATION.getName());
        String[] partRequestRelation = requestRelation.split("/");
        String relationKey = partRequestRelation[partRequestRelation.length - 1];

        EntitySetDocHandler relation = getRelation(relationKey);
        if (relation == null) {
            // データが存在しない場合はRelationを新規に登録
            createRelationEntity(relationKey);
        }

        // 関係を結ぶセルURL取得
        String requestExtCell = (String) entitySetDocHandler.getStaticFields().get(
                ReceivedMessage.P_REQUEST_RELATION_TARGET.getName());
        if (!requestExtCell.endsWith("/")) {
            requestExtCell += "/";
        }

        if (getExtCell(requestExtCell) == null) {
            // データが存在しない場合はExtCellを新規に登録
            createExtCellEntity(requestExtCell);
        }

        // RelationとExtCellの$linksを作成
        createRelationExtCellLinks(relationKey, requestExtCell);
    }

    /**
     * RelationとExtCellの$links作成.
     * @param relationKey
     * @param requestExtCell
     */
    private void createRelationExtCellLinks(String relationKey, String requestExtCell) {
        try {
            OEntityKey relationOEntityKey = createRelationOEntityKey(relationKey);
            OEntityId relationEntityId = OEntityIds.create(Relation.EDM_TYPE_NAME, relationOEntityKey);
            OEntityKey extCellOEntityKey = createExtCellOEntityKey(requestExtCell);
            OEntityId extCellEntityId = OEntityIds.create(ExtCell.EDM_TYPE_NAME, extCellOEntityKey);

            // n:nの場合
            createLinks(relationEntityId, extCellEntityId);
        } catch (DcCoreException e) {
            if (DcCoreException.OData.CONFLICT_LINKS.getCode().equals(e.getCode())) {
                // $linksが既に存在する場合
                throw DcCoreException.ReceiveMessage.REQUEST_RELATION_EXISTS_ERROR;
            }
            throw e;
        }
    }

    private OEntityKey createExtCellOEntityKey(String requestExtCell) {
        OEntityKey extCellOEntityKey;
        try {
            extCellOEntityKey = OEntityKey.parse("('" + requestExtCell + "')");
        } catch (IllegalArgumentException e) {
            throw DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_PARSE_ERROR.reason(e);
        }
        return extCellOEntityKey;
    }

    private OEntityKey createRelationOEntityKey(String relationKey) {
        OEntityKey relationOEntityKey;
        try {
            relationOEntityKey = OEntityKey.parse("('" + relationKey + "')");
        } catch (IllegalArgumentException e) {
            throw DcCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR.reason(e);
        }
        return relationOEntityKey;
    }

    /**
     * Relationを取得する.
     * @param key キー
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getRelation(String key) {
        EdmEntitySet edmEntitySet = getMetadata().getEdmEntitySet(Relation.EDM_TYPE_NAME);
        OEntityKey oEntityKey = createRelationOEntityKey(key);

        return retrieveWithKey(edmEntitySet, oEntityKey);
    }

    /**
     * ExtCellを取得する.
     * @param key キー
     * @return EntitySetDocHandler
     */
    protected EntitySetDocHandler getExtCell(String key) {
        EdmEntitySet edmEntitySet = getMetadata().getEdmEntitySet(ExtCell.EDM_TYPE_NAME);
        OEntityKey oEntityKey = createExtCellOEntityKey(key);

        return retrieveWithKey(edmEntitySet, oEntityKey);
    }

    /**
     * RelationをESに保存.
     * @param key RelationのNameの値
     */
    private void createRelationEntity(String key) {

        // staticFields
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(Relation.P_ROLE_NAME.getName(), key);

        createEntity(Relation.EDM_TYPE_NAME, staticFields);
    }

    /**
     * ExtCellをESに保存.
     * @param key ExtCellのUrlの値
     */
    private void createExtCellEntity(String key) {

        // staticFields
        Map<String, Object> staticFields = new HashMap<String, Object>();
        staticFields.put(ExtCell.P_URL.getName(), key);

        createEntity(ExtCell.EDM_TYPE_NAME, staticFields);
    }

    /**
     * ESに保存.
     * @param typeName 登録するデータのType名
     * @param staticFields 登録するstaticFieldsの値
     */
    private void createEntity(String typeName, Map<String, Object> staticFields) {
        EntitySetAccessor esType = this.getAccessorForEntitySet(typeName);

        // EntitySetDocHandlerの作成
        EntitySetDocHandler oedh = new OEntityDocHandler();
        oedh.setType(typeName);
        oedh.setId(PersoniumUUID.randomUUID());

        // staticFields
        oedh.setStaticFields(staticFields);

        // Cell, Box, Nodeの紐付
        oedh.setCellId(this.getCellId());
        oedh.setBoxId(null);
        oedh.setNodeId(null);

        // published, updated
        long crrTime = System.currentTimeMillis();
        oedh.setPublished(crrTime);
        oedh.setUpdated(crrTime);

        // TODO 複合キーでNTKPの項目(ex. _EntityType.Name)があれば、リンク情報を設定する
        // if (KeyType.COMPLEX.equals(oEntityKey.getKeyType())) {
        // try {
        // setLinksFromOEntity(getMetadata().findEdmEntitySet(Relation.EDM_TYPE_NAME).getType(),
        // oEntityKey, entitySetDocHandler);
        // } catch (NTKPNotFoundException e) {
        // throw DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(e.getMessage());
        // }
        // }

        // 登録前処理
        this.beforeCreate(typeName, null, oedh);

        // ESに保存する
        esType.create(oedh.getId(), oedh);

        // 登録後処理
        this.afterCreate(typeName, null, oedh);
    }

    /**
     * 関係削除を行う.
     * @param entitySetDocHandler 受信メッセージ
     */
    protected void breakRelation(EntitySetDocHandler entitySetDocHandler) {
        log.debug("breakRelation start.");
        // RequestRelationからRelation名を取得する
        String reqRelation = entitySetDocHandler.getStaticFields()
                .get(ReceivedMessagePort.P_REQUEST_RELATION.getName()).toString();
        String relationName = getRelationFromRelationClassUrl(reqRelation);
        if (relationName == null) {
            throw DcCoreException.ReceiveMessage.REQUEST_RELATION_PARSE_ERROR;
        }

        // 対象のRelationが存在することを確認
        EntitySetDocHandler relation = getRelation(relationName);
        if (relation == null) {
            log.debug(String.format("RequestRelation does not exists. [%s]", relationName));
            throw DcCoreException.ReceiveMessage.REQUEST_RELATION_DOES_NOT_EXISTS.params(relationName);
        }

        // 対象のExtCell(RequestRelationTarget)が存在することを確認
        String extCellUrl = entitySetDocHandler.getStaticFields()
                .get(ReceivedMessagePort.P_REQUEST_RELATION_TARGET.getName()).toString();
        EntitySetDocHandler extCell = getExtCell(extCellUrl);
        if (extCell == null) {
            log.debug(String.format("RequestRelationTarget does not exists. [%s]", extCellUrl));
            throw DcCoreException.ReceiveMessage.REQUEST_RELATION_TARGET_DOES_NOT_EXISTS.params(extCellUrl);
        }

        // RelationとExtCellの関連を削除する
        if (!deleteLinkEntity(relation, extCell)) {
            log.debug(String.format("RequestRelation and RequestRelationTarget does not related. [%s] - [%s]",
                    relationName, extCellUrl));
            throw DcCoreException.ReceiveMessage.LINK_DOES_NOT_EXISTS.params(relationName, extCellUrl);
        }
        log.debug("breakRelation success.");
    }

    /**
     * リレーションクラスURLからリレーション名を取得する.
     * @param relationClassUrl リレーションクラスURL
     * @return リレーション名
     */
    protected String getRelationFromRelationClassUrl(String relationClassUrl) {
        String relationName = null;
        log.debug(String.format("RequestRelation URI = [%s]", relationClassUrl));

        Pattern pattern = Pattern.compile(".+/([^/]+)/__relation/([^/]+)/([^/]+)/?");
        Matcher m = pattern.matcher(relationClassUrl);
        if (!m.matches()) {
            log.debug(String.format("RequestRelation URI if not relationClassUrl format. [%s]", relationClassUrl));
            return relationName;
        }
        relationName = m.replaceAll("$3");
        log.debug(String.format("RequestRelation URI Path = [%s]", relationName));
        return relationName;
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
        // build or breakの場合のみバリデートをして、approved / rejectedであればtrueを返却する
        if (type.equals(ReceivedMessage.TYPE_REQ_RELATION_BUILD)
                || type.equals(ReceivedMessage.TYPE_REQ_RELATION_BREAK)) {
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
        // build or breakの場合のみバリデートをして、none であればtrueを返却する
        if (type.equals(ReceivedMessage.TYPE_REQ_RELATION_BUILD)
                || type.equals(ReceivedMessage.TYPE_REQ_RELATION_BREAK)) {
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

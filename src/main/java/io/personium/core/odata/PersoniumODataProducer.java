/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * --------------------------------------------------
 * This code is based on ODataProducer.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import java.util.LinkedHashMap;
import java.util.List;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.ODataProducer;

import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.rs.odata.BulkRequest;
import io.personium.core.rs.odata.ODataBatchResource.NavigationPropertyBulkContext;

/**
 * ETag・主キー変更に対応させたODataProducer.
 */
public interface PersoniumODataProducer extends ODataProducer {
    /**
     * ETag・主キー変更対応のEntity更新.
     * @param entitySetName entitySetName
     * @param originalKey 更新対象キー
     * @param oEntityWrapper データ（更新後キーも含む）
     */
    void updateEntity(final String entitySetName, final OEntityKey originalKey, final OEntityWrapper oEntityWrapper);

    /**
     * Accountのパスワード変更.
     * @param entitySetName entitySetName
     * @param originalKey 更新対象キー
     * @param dcCredHeader dcCredHeader
     */
    void updatePassword(final EdmEntitySet entitySetName, final OEntityKey originalKey, final String dcCredHeader);

    /**
     * Accountの最終ログイン時刻変更.
     * @param entitySetName entitySetName
     * @param originalKey 更新対象キー
     * @param accountId アカウントのID
     */
    void updateLastAuthenticated(final EdmEntitySet entitySetName, final OEntityKey originalKey, String accountId);

    /**
     * ETag・主キー変更対応のEntity MERGE.
     * @param entitySetName entitySetName
     * @param originalKey 更新対象キー
     * @param oEntityWrapper データ（更新後キーも含む）
     */
    void mergeEntity(final String entitySetName, final OEntityKey originalKey, final OEntityWrapper oEntityWrapper);

    /**
     * ETag対応のEntity削除.
     * @param entitySetName entitySetName
     * @param entityKey entityKey
     * @param etag etag
     */
    void deleteEntity(String entitySetName, OEntityKey entityKey, String etag);

    /**
     * EntitySet名とOEntityからEntitySetDocHandlerを生成して取得する.
     * @param entitySetName EntitySet名
     * @param entity OEntity
     * @return EntitySetDocHandler
     */
    EntitySetDocHandler getEntitySetDocHandler(String entitySetName, OEntity entity);

    /**
     * 更新系の処理ハンドラ.
     * @param entitySetName エンティティセット名
     */
    void onChange(String entitySetName);

    /**
     * バルク登録を実行する.
     * @param metadata スキーマ情報
     * @param bulkRequests 登録するEntitySetDocHandlerのリスト
     * @param cellId セルID
     * @return EntitiesResponse
     */
    List<EntityResponse> bulkCreateEntity(EdmDataServices metadata,
            LinkedHashMap<String, BulkRequest> bulkRequests, String cellId);

    /**
     * NP経由でエンティティを登録後リンクを登録する.
     * @param sourceEntity sourceEntity
     * @param targetNavProp targetNavProp
     * @param oew oew
     * @param entity targetEntity
     * @return etag
     */
    EntityResponse createNp(OEntityId sourceEntity, String targetNavProp,
            OEntity oew, String entity);

    /**
     * NavigationProperty経由でエンティティを一括登録する.
     * @param npBulkContexts 一括登録のコンテキスト
     * @param npBulkRequests エンティティ一括登録用のリクエスト情報（bulkCreateEntity用）
     */
    void bulkCreateEntityViaNavigationProperty(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests);

    /**
     * NavigationProperty経由でエンティティを一括登録する際のリンク数の上限値チェックを行う.
     * @param npBulkContexts 一括登録のコンテキスト
     * @param npBulkRequests エンティティ一括登録用のリクエスト情報（bulkCreateEntity用）
     */
    void checkLinksUpperLimitRecord(List<NavigationPropertyBulkContext> npBulkContexts,
            LinkedHashMap<String, BulkRequest> npBulkRequests);



}

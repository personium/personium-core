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
package io.personium.core.rs.odata;

import java.util.List;

import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.odata.OEntityWrapper;

/**
 * ODataResourceに対する事前処理や事後処理.
 */
public class ODataCtlResource {
    /**
     * 作成前処理.
     * @param oEntityWrapper OEntityWrapperオブジェクト
     */
    public void beforeCreate(final OEntityWrapper oEntityWrapper) {
    }

    /**
     * 更新前処理.
     * @param oEntityWrapper OEntityWrapperオブジェクト
     * @param oEntityKey 削除対象のentityKey
     */
    public void beforeUpdate(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
    }

    /**
     * 部分更新前処理.
     * @param oEntityWrapper OEntityWrapperオブジェクト
     * @param oEntityKey 削除対象のentityKey
     */
    public void beforeMerge(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        beforeUpdate(oEntityWrapper, oEntityKey);
    }

    /**
     * 削除前処理.
     * @param entitySetName entitySet名
     * @param oEntityKey 削除対象のentityKey
     */
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey) {
    }

    /**
     * 削除後処理.
     * @param entitySetName entitySet名
     * @param oEntityKey 削除対象のentityKey
     */
    public void afterDelete(final String entitySetName, final OEntityKey oEntityKey) {
    }

    /**
     * リンク登録前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    public void beforeLinkCreate(OEntityId sourceEntity, String targetNavProp) {
        // ExtRoleと_Relationの$links指定は不可（Relation:ExtRoleは1:Nの関係だから）
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * リンク取得前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    public void beforeLinkGet(OEntityId sourceEntity, String targetNavProp) {
    }

    /**
     * リンク削除前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    public void beforeLinkDelete(OEntityId sourceEntity, String targetNavProp) {
        // ExtRoleと_Relationの$links指定は不可（Relation:ExtRoleは1:Nの関係だから）
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * p:Format以外のチェック処理.
     * @param props プロパティ一覧
     */
    public void validate(List<OProperty<?>> props) {
    }

    private void checkNonSupportLinks(String sourceEntity, String targetNavProp) {
        if (targetNavProp.startsWith("_")) {
            targetNavProp = targetNavProp.substring(1);
        }
        if ((sourceEntity.equals(ExtRole.EDM_TYPE_NAME) //NOPMD -To maintain readability
                        && targetNavProp.equals(Relation.EDM_TYPE_NAME)) //NOPMD
                || (sourceEntity.equals(Relation.EDM_TYPE_NAME) //NOPMD
                        && targetNavProp.equals(ExtRole.EDM_TYPE_NAME))) { //NOPMD
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
    }
}

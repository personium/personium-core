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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AuthUtils;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.utils.ODataUtils;

/**
 * ODataProducerUtils.
 */
public final class ODataProducerUtils {

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(ODataProducerUtils.class);

    private ODataProducerUtils() {
    }

    /**
     * Entity登録・更新時のデータの一意性チェックを行う.
     * @param producer
     * @param newEntity 新しく登録・更新するEntity
     * @param originalEntity もとのEntity
     * @param originalKey 更新リクエストで指定されたキー名
     */
    static void checkUniqueness(EsODataProducer producer, OEntityWrapper newEntity,
            OEntityWrapper originalEntity, OEntityKey originalKey) {
        boolean needsPkCheck = false;
        if (originalEntity == null) {
            needsPkCheck = true;
        } else {
            // originalKeyからoewのキーが変更になっているときのみ、キー変更による影響確認をする。
            OEntityKey normNewKey = AbstractODataResource.normalizeOEntityKey(newEntity.getEntityKey(),
                    newEntity.getEntitySet());
            if (null == originalKey) {
                originalKey = originalEntity.getEntityKey();
            }
            OEntityKey normOrgKey = AbstractODataResource.normalizeOEntityKey(
                    originalKey, newEntity.getEntitySet());
            String newKeyStr = normNewKey.toKeyStringWithoutParentheses();
            String orgKeyStr = normOrgKey.toKeyStringWithoutParentheses();
            // KEYを正規化した上で比較しなくてはならない。
            log.debug("NWKEY:" + newKeyStr);
            log.debug("ORKEY:" + orgKeyStr);
            if (!newKeyStr.equals(orgKeyStr)) {
                needsPkCheck = true;
            }
        }
        if (needsPkCheck) {
            // 主キーでの検索を行う。
            EntitySetDocHandler hit = producer.retrieveWithKey(newEntity.getEntitySet(), newEntity.getEntityKey());
            if (hit != null) {
                // データが存在したら CONFLICT エラーとする
                throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
            }
        }

        // UK 制約による一意性チェック
        // UK 制約の抽出処理
        // TODO スキーマ情報と共にキャッシュ(別メソッド化)
        Map<String, List<String>> uks = new HashMap<String, List<String>>();
        List<EdmProperty> listEdmProperties = newEntity.getEntityType().getProperties().toList();
        for (EdmProperty edmProp : listEdmProperties) {
            Iterable<? extends NamespacedAnnotation<?>> anots = edmProp.getAnnotations();
            for (NamespacedAnnotation<?> anot : anots) {
                if ("Unique".equals(anot.getName())
                        && PersoniumCoreUtils.XmlConst.NS_PERSONIUM.equals(anot.getNamespace().getUri())) {
                    String ukName = (String) anot.getValue();
                    List<String> ukProps = uks.get(ukName);
                    if (ukProps == null) {
                        ukProps = new ArrayList<String>();
                    }
                    ukProps.add(edmProp.getName());
                    uks.put(ukName, ukProps);
                }
            }
        }

        // ここですべてのユニークキーでの検索を行い、データが存在しないことを確認する
        for (Map.Entry<String, List<String>> uk : uks.entrySet()) {
            log.debug("checking uk : [" + uk.getKey() + "] = ");
            List<String> ukProps = uk.getValue();
            Set<OProperty<?>> ukSet = new HashSet<OProperty<?>>();
            // UKは非null項目の一意性確保なので、全項目がnullであるものは、いくつあってもよい。
            boolean allNull = true;
            // UKを構成する全項目に変更がなかったときは、チェックを入れる必要がない。
            boolean changed = false;
            for (String k : ukProps) {
                log.debug("              - [" + k + "]");
                OProperty<?> oProp = newEntity.getProperty(k);
                if (oProp.getValue() != null) {
                    allNull = false;
                    if (originalEntity != null) {
                        OProperty<?> origProp = originalEntity.getProperty(k);
                        if (!oProp.getValue().equals(origProp.getValue())) {
                            changed = true;
                        }
                    }
                }
                ukSet.add(oProp);
            }
            // UKを構成する全項目がNullであるときはチェック必要なし
            boolean needsUkCheck = !allNull;
            if (originalEntity != null && !changed) {
                // 変更で、UKを構成する全項目の変更がないときはチェックの必要なし。
                needsUkCheck = false;
            }

            // 変更後キーがAllNullでなく、変更があったときのみ、チェック。
            if (needsUkCheck) {
                EntitySetDocHandler edh = producer.retrieveWithKey(newEntity.getEntitySet(), ukSet, null);
                if (edh != null) {
                    // データが存在したら CONFLICT エラーとする
                    throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
                }
            }
        }
    }

    /**
     * Search N:N links.
     * @param idvals idvals
     * @param tgtEsType tgtEsType
     * @param queryInfo Request query
     * @return Search result
     */
    public static PersoniumSearchHits searchLinksNN(
            List<String> idvals, EntitySetAccessor tgtEsType, QueryInfo queryInfo) {

        if (idvals.size() == 0) {
            return null;
        }

        // If no query is specified, the default value is set.
        Integer size = PersoniumUnitConfig.getTopQueryDefaultSize();
        Integer from = 0;
        if (queryInfo != null) {
            if (queryInfo.top != null) {
                size = queryInfo.top;
            }
            if (queryInfo.skip != null) {
                from = queryInfo.skip;
            }
        }

        // Obtain target primary key column from target UUID column.
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();
        Map<String, Object> ids = new HashMap<String, Object>();
        source.put("filter", filter);
        source.put("size", size);
        source.put("from", from);
        filter.put("ids", ids);
        ids.put("values", idvals);

        List<Map<String, Object>> sort = new ArrayList<Map<String, Object>>();
        Map<String, Object> orderByName = new HashMap<String, Object>();
        Map<String, Object> orderById = new HashMap<String, Object>();
        Map<String, Object> order = new HashMap<String, Object>();
        source.put("sort", sort);
        sort.add(orderByName);
        sort.add(orderById);
        order.put("order", "asc");
        order.put("ignore_unmapped", true);
        orderByName.put("s.Name.untouched", order);
        orderById.put("s.__id.untouched", order);

        PersoniumSearchHits sHits = tgtEsType.search(source).hits();
        return sHits;
    }

    /**
     * linksKeyと親キーが等しいか比較する.
     * @param entity entity
     * @param linksKey linksKey
     * @return boolean
     */
    public static boolean isParentEntity(OEntityId entity, String linksKey) {
        return entity.getEntitySetName().equals(linksKey);
    }

    /**
     * ESへのパスワード変更リクエストを生成する.
     * @param oedhNew oedhNew
     * @param dcCredHeader dcCredHeader
     */
    public static void createRequestPassword(EntitySetDocHandler oedhNew, String dcCredHeader) {
        // 更新前処理(Hash文字列化したパスワードを取得)
        String hPassStr = AuthUtils.checkValidatePassword(dcCredHeader, oedhNew.getType());
        // 変更するパスワードをHashedCredentialへ上書きする
        Map<String, Object> hiddenFields = oedhNew.getHiddenFields();
        // X-Personium-Credentialの値をHashedCredentialのキーへputする
        // 指定がない場合400エラーを返却する
        if (hPassStr != null) {
            hiddenFields.put("HashedCredential", hPassStr);
        } else {
            throw PersoniumCoreException.Auth.P_CREDENTIAL_REQUIRED;
        }
        oedhNew.setHiddenFields(hiddenFields);

        // 現在時刻を取得して__updatedを上書きする
        long nowTimeMillis = System.currentTimeMillis();
        oedhNew.setUpdated(nowTimeMillis);
    }

    /**
     * 引数で指定されたOEDHのdynamic fieldsとstatic fieldsの値をマージしてoedhNewを更新する.
     * @param oedhExisting ベースにするOEDH
     * @param oedhNew 追加するOEDH
     */
    public static void mergeFields(EntitySetDocHandler oedhExisting, EntitySetDocHandler oedhNew) {
        // static fieldsに登録済みのプロパティを追加
        oedhNew.setStaticFields(ODataUtils.getMergeFields(oedhExisting.getStaticFields(),
                oedhNew.getStaticFields()));
    }
}

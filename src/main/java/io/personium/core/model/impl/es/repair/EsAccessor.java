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
package io.personium.core.model.impl.es.repair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.DcSearchResponse;
import io.personium.common.es.response.EsClientException;
import io.personium.core.DcCoreConfig;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.QueryMapFactory;

/**
 * データストア層(Elasticsearch)への操作処理を実装したクラス.
 */
public class EsAccessor {

    /** ログ用オブジェクト. */
    static Logger log = LoggerFactory.getLogger(EsAccessor.class);

    /**
     * デフォルトコンストラクタ（使用不可）.
     */
    private EsAccessor() {
    }

    /**
     * ドキュメントを検索.
     * @param indexName 検索対象のインデックス名
     * @param routingId 検索時のルーティングID
     * @param idList 検索対象のuuidリスト
     * @param type タイプ名
     * @return 検索結果
     * @throws EsClientException ESへの検索に失敗した場合
     */
    public static DcSearchResponse search(final String indexName,
            final String routingId,
            final List<String> idList,
            final String type)
            throws EsClientException {

        // TODO クエリを作成する箇所とESへリクエストする箇所を別のメソッドにする。
        // IDとTypeで検索する。
        Map<String, Object> query = new HashMap<String, Object>();
        List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
        Map<String, Object> ids = new HashMap<String, Object>();
        Map<String, Object> idValues = new HashMap<String, Object>();
        Map<String, Object> typeFilter = new HashMap<String, Object>();
        Map<String, Object> typeValue = new HashMap<String, Object>();

        query.put("filter", QueryMapFactory.mustQuery(queries));
        // ids
        queries.add(ids);
        ids.put("ids", idValues);
        idValues.put("values", idList);
        // typeFilter
        queries.add(typeFilter);
        typeFilter.put("type", typeValue);
        typeValue.put("value", type);
        // version
        query.put("version", true);
        // size
        query.put("size", idList.size());
        // リペアツールでは、アクセス先が特定のインデックスに限らないため、その都度EsIndexを生成する。
        // TODO コスト的に問題がないのかを確認する必要あり。
        EsIndex index = getEsIndex(indexName);
        return index.search(routingId, query);
    }

    /**
     * インデックス名に応じたEsIndex インスタンスを取得する.
     * @param indexName インデックス名(unit prefix付き)
     * @return 生成した EsIndex インスタンス
     */
    private static EsIndex getEsIndex(final String indexName) {
        final String cellIndexName = EsIndex.CATEGORY_AD;
        String unitUserName = indexName.replace(DcCoreConfig.getEsUnitPrefix() + "_", "");
        if (cellIndexName.equals(unitUserName)) {
            return EsModel.idxAdmin();
        } else {
            return EsModel.idxUser(unitUserName);
        }
    }
}

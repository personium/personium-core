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
package com.fujitsu.dc.core.model.impl.es.odata;

import java.util.List;
import java.util.Map;

import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.producer.QueryInfo;

/**
 * ODataのクエリハンドラー.
 */
public interface ODataQueryHandler {

    /**
     * 初期化.
     * @param queryInfo OData4jのQueryInfo.
     * @param implicitConds 暗黙検索条件.
     */
    void initialize(QueryInfo queryInfo, List<Map<String, Object>> implicitConds);

    /**
     * 検索クエリを取得する.
     * @return 検索クエリ
     */
    Map<String, Object> getSource();

    /**
     * $selectの値からES検索用のクエリを組立てる.
     * @param baseSource 入力値を格納したMap
     * @param selects $select
     */
    void getSelectQuery(Map<String, Object> baseSource,
            List<EntitySimpleProperty> selects);
}

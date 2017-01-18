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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;

import com.fujitsu.dc.common.es.response.DcSearchHits;
import com.fujitsu.dc.core.model.impl.es.QueryMapFactory;
import com.fujitsu.dc.core.model.impl.es.accessor.EntitySetAccessor;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.odata.NavigationTargetKeyProperty;
import com.fujitsu.dc.core.rs.odata.AbstractODataResource;

/**
 * NavigationTargetKeyPropertyクラスのES実装.
 */
public class EsNavigationTargetKeyProperty implements NavigationTargetKeyProperty {

    /** EntityType. */
    private String type;

    /** cellId. */
    private String cellId;

    /** boxId. */
    private String boxId;

    /** nodeId. */
    private String nodeId;

    /** staticフィールドの検索項目. */
    private Map<String, String> statics = new HashMap<String, String>();

    /** linkフィールドの検索項目. */
    private Map<String, String> links = new HashMap<String, String>();

    /** 推移先NTKP. */
    private EsNavigationTargetKeyProperty shiftNtkp;

    /** Property情報. */
    private Set<OProperty<?>> properties = new HashSet<OProperty<?>>();

    /**
     * propertiesのセッター.
     * @param properties properties
     */
    public void setProperties(Set<OProperty<?>> properties) {
        this.properties = properties;
    }

    /** ODataProducer情報. */
    private EsODataProducer odataProducer;

    /**
     * コンストラクタ.
     * @param cellId CellId
     * @param boxId boxId
     * @param nodeId nodeId
     * @param type タイプ
     * @param odataProducer odataProducer
     */
    public EsNavigationTargetKeyProperty(String cellId,
            String boxId,
            String nodeId,
            String type,
            EsODataProducer odataProducer) {
        this.cellId = cellId;
        this.boxId = boxId;
        this.nodeId = nodeId;
        this.type = type;
        this.odataProducer = odataProducer;
    }

    /**
     * NavigationTargetKeyPropertyを検索する.
     * @return リンク情報のTypeとIDのHashMap
     */
    private Map<String, String> search() {
        Map<String, Object> filter = getSearchQuery();

        // 検索の実行
        EntitySetAccessor esType = odataProducer.getAccessorForEntitySet(type);
        DcSearchHits sHits = esType.search(filter).hits();

        Map<String, String> link = new HashMap<String, String>();
        if (sHits.getCount() == 0) {
            // 存在していなければ、例外をあげる
            String value = null;
            for (Map.Entry<String, String> ent : statics.entrySet()) {
                value = ent.getValue();
            }
            if (AbstractODataResource.isDummy(value)) {
                link.put(type, AbstractODataResource.DUMMY_KEY);
            } else {
                throw new NTKPNotFoundException(value);
            }
        } else {
            // {"type" : "id情報"}の形式で検索結果を返却する
            link.put(type, sHits.getAt(0).getId());
        }
        return link;
    }

    /**
     * 検索クエリー取得する.
     * @return 検索クエリー
     */
    private Map<String, Object> getSearchQuery() {
        // Staticフィールドの検索クエリを組み立てる
        List<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        if (!statics.isEmpty()) {
            for (Map.Entry<String, String> ent : statics.entrySet()) {
                terms.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_STATIC_FIELDS + "." + ent.getKey()
                        + ".untouched", ent.getValue()));
            }
        }

        // Linkフィールドの検索クエリを組み立てる
        if (!links.isEmpty()) {
            for (Map.Entry<String, String> ent : links.entrySet()) {
                String linkKey = OEntityDocHandler.KEY_LINK + "." + ent.getKey();
                if (AbstractODataResource.isDummy(ent.getValue())) {
                    // ダミーキーの場合はリンクのNull検索クエリを組み立てる
                    terms.add(QueryMapFactory.missingFilter(linkKey));
                } else {
                    terms.add(QueryMapFactory.termQuery(linkKey, ent.getValue()));
                }
            }
        }

        List<Map<String, Object>> implicitConditions = new ArrayList<Map<String, Object>>();
        // NodeIDを検索条件に指定
        if (nodeId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_NODE_ID, nodeId));
        }
        // BoxIdを検索条件に指定
        if (boxId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_BOX_ID, boxId));
        }
        // CellIDを検索条件に指定
        if (cellId != null) {
            implicitConditions.add(QueryMapFactory.termQuery(OEntityDocHandler.KEY_CELL_ID, cellId));
        }

        Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(implicitConditions));

        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("version", true);
        if (!terms.isEmpty()) {
            filter.put("filter", QueryMapFactory.andFilter(terms));
        }
        filter.put("query", query);
        return filter;
    }

    /**
     * プロパティー情報を解析する.
     */
    private void analyzeProperties() {
        for (OProperty<?> property : properties) {
            HashMap<String, String> ntkp = AbstractODataResource.convertNTKP(property.getName());
            if (ntkp == null) {
                // staticフィールドの検索条件
                statics.put(property.getName(), (String) property.getValue());
            } else {
                // linksフィールドの検索条件
                if (shiftNtkp == null) {
                    shiftNtkp = new EsNavigationTargetKeyProperty(cellId, boxId, nodeId, ntkp.get("entityType"),
                            odataProducer);
                }
                // OPropertyを組み立てて、追加する
                OProperty<?> newProperty = OProperties.string(ntkp.get("propName"), (String) property.getValue());
                shiftNtkp.properties.add(newProperty);
            }
        }
        if (shiftNtkp != null) {
            links = shiftNtkp.recursiveSearch();
        }
    }

    /**
     * NTKPの推移を再帰的に検索して、リンク情報のTypeとIDのHashMapを返却する.
     * @return リンク情報のTypeとIDのHashMap
     */
    private Map<String, String> recursiveSearch() {
        analyzeProperties();
        return search();
    }

    /**
     * NavigationTargetKeyPropertyの情報から、リンク情報のTypeとIDのEntryを取得する.
     * @return リンク情報のTypeとIDのEntry
     */
    @Override
    public Map.Entry<String, String> getLinkEntry() {
        analyzeProperties();
        if (links.isEmpty()) {
            return null;
        }
        // ダミーキーが設定されている場合はリンク情報無し
        Map.Entry<String, String> entry = links.entrySet().iterator().next();
        if (AbstractODataResource.isDummy(entry.getValue())) {
            return null;
        }
        return entry;
    }

    /**
     * NavigationTargetKeyPropertyを解析して、NTKPを検索するクエリーを取得する.
     * @return NTKP検索クエリー
     */
    @Override
    public Map<String, Object> getNtkpSearchQuery() {
        analyzeProperties();
        return getSearchQuery();
    }

    /**
     * NTKPNotFoundException.
     */
    @SuppressWarnings("serial")
    public static class NTKPNotFoundException extends RuntimeException {
        /**
         * コンストラクタ.
         * @param msg msg.
         */
        public NTKPNotFoundException(String msg) {
            super(msg);
        }
    }
}

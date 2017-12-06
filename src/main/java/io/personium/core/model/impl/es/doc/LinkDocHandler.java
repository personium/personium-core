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
package io.personium.core.model.impl.es.doc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.odata4j.producer.QueryInfo;

import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.core.model.impl.es.odata.EsQueryHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;

/**
 * ESでN:Nリンクを扱う. リンクは２つのタイプの間に張られている。 EsLinkHandler elh = new EsLinkHandler(type1, type2); 双方のキーを指定して、リンクドキュメントを作成する。
 * 双方のキーを指定して、リンクドキュメントのキーを作成する。 片側のTypeのキーを指定してもう片方のTypeの一覧を取得する。
 */
public class LinkDocHandler implements EsDocHandler {
    private String id;
    private String cellId;
    private String boxId;
    private String nodeId;
    private String ent1Type;
    private String ent1Key;
    private String ent2Type;
    private String ent2Key;
    private Long published;
    private Long updated;

    private static final int DEFAULT_TOP_VALUE = PersoniumUnitConfig.getTopQueryDefaultSize();

    /**
     * コンストラクタ.
     */
    public LinkDocHandler() {
        this.id = null;
        this.cellId = null;
        this.boxId = null;
        this.nodeId = null;
        this.ent1Type = null;
        this.ent1Key = null;
        this.ent2Type = null;
        this.ent2Key = null;
        this.published = null;
        this.updated = null;
    }

    /**
     * コンストラクタ.
     * @param srcHandler OEntityDocHandler
     * @param tgtHandler OEntityDocHandler
     */
    public LinkDocHandler(final EntitySetDocHandler srcHandler, final EntitySetDocHandler tgtHandler) {
        this.cellId = srcHandler.getCellId();
        this.boxId = srcHandler.getBoxId();
        this.nodeId = srcHandler.getNodeId();
        String srcType = srcHandler.getType();
        String srcId = srcHandler.getId();
        String tgtType = tgtHandler.getType();
        String tgtId = tgtHandler.getId();

        // ES 保存時の一意キー作成
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put(srcType, srcId);
        tm.put(tgtType, tgtId);

        this.ent1Type = tm.firstKey();
        this.ent2Type = tm.lastKey();
        this.ent1Key = tm.get(ent1Type);
        this.ent2Key = tm.get(ent2Type);
        this.id = this.createLinkId();
        long dateTime = new Date().getTime();
        this.published = dateTime;
        this.updated = dateTime;
    }

    /**
     * 検索結果からLinkDocHandlerを生成するコンストラクタ.
     * @param searchHit 検索結果データ
     */
    public LinkDocHandler(final PersoniumSearchHit searchHit) {
        this.id = searchHit.getId();

        Map<String, Object> source = searchHit.getSource();
        this.cellId = source.get(KEY_CELL_ID).toString();
        if (source.containsKey(KEY_BOX_ID) && source.get(KEY_BOX_ID) != null) {
            this.boxId = source.get(KEY_BOX_ID).toString();
        }
        if (source.containsKey(KEY_NODE_ID) && source.get(KEY_NODE_ID) != null) {
            this.nodeId = source.get(KEY_NODE_ID).toString();
        }

        // ES 保存時の一意キー作成
        String srcType = source.get(KEY_ENT1_TYPE).toString();
        String srcId = source.get(KEY_ENT1_ID).toString();
        String tgtType = source.get(KEY_ENT2_TYPE).toString();
        String tgtId = source.get(KEY_ENT2_ID).toString();
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put(srcType, srcId);
        tm.put(tgtType, tgtId);

        this.ent1Type = tm.firstKey();
        this.ent2Type = tm.lastKey();
        this.ent1Key = tm.get(ent1Type);
        this.ent2Key = tm.get(ent2Type);
        this.published = Long.parseLong(source.get(KEY_PUBLISHED).toString());
        this.updated = Long.parseLong(source.get(KEY_UPDATED).toString());
    }

    /**
     * @return the ent1Type
     */
    public String getEnt1Type() {
        return ent1Type;
    }

    /**
     * @param ent1Type the ent1Type to set
     */
    public void setEnt1Type(String ent1Type) {
        this.ent1Type = ent1Type;
    }

    /**
     * @return the ent1Key
     */
    public String getEnt1Key() {
        return ent1Key;
    }

    /**
     * @param ent1Key the ent1Key to set
     */
    public void setEnt1Key(String ent1Key) {
        this.ent1Key = ent1Key;
    }

    /**
     * @return the ent2Type
     */
    public String getEnt2Type() {
        return ent2Type;
    }

    /**
     * @param ent2Type the ent2Type to set
     */
    public void setEnt2Type(String ent2Type) {
        this.ent2Type = ent2Type;
    }

    /**
     * @return the ent2Key
     */
    public String getEnt2Key() {
        return ent2Key;
    }

    /**
     * @param ent2Key the ent2Key to set
     */
    public void setEnt2Key(String ent2Key) {
        this.ent2Key = ent2Key;
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * @return cell id
     */
    public String getCellId() {
        return cellId;
    }

    /**
     * @param cellid cell Id
     */
    public void setCellId(String cellid) {
        this.cellId = cellid;
    }

    /**
     * @return box id
     */
    public String getBoxId() {
        return boxId;
    }

    /**
     * @param boxid box id
     */
    public void setBoxId(String boxid) {
        this.boxId = boxid;
    }

    /**
     * @return node id
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeid node id
     */
    public void setNodeId(String nodeid) {
        this.nodeId = nodeid;
    }

    @Override
    public Long getVersion() {
        return null;
    }

    @Override
    public Map<String, Object> getSource() {
        return this.createLinkDoc();
    }

    /**
     * @return Json
     */
    public Map<String, Object> createLinkDoc() {
        Map<String, Object> ret = new HashMap<String, Object>();
        if (this.cellId != null) {
            ret.put(KEY_CELL_ID, this.cellId);
        }
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_ENT1_TYPE, this.ent1Type);
        ret.put(KEY_ENT1_ID, this.ent1Key);
        ret.put(KEY_ENT2_TYPE, this.ent2Type);
        ret.put(KEY_ENT2_ID, this.ent2Key);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        return ret;
    }

    /**
     * @return ID
     */
    public String createLinkId() {
        return this.ent1Key + "-" + this.ent2Key;
    }

    /**
     * N:Nのリンクのドキュメント件数を取得する.
     * @param accessor link用のaccessor
     * @param srcHandler OEntityDocHandler
     * @param targetSetName targetSetName
     * @param targetEntityTypeId targetEntityTypeId
     * @return ドキュメント件数
     */
    public static long getNtoNCount(
            final ODataLinkAccessor accessor,
            final EntitySetDocHandler srcHandler,
            final String targetSetName,
            final String targetEntityTypeId) {

        NtoNQueryParameter parameter = new NtoNQueryParameter(srcHandler, targetSetName,
                targetEntityTypeId);

        return accessor.count(parameter.getSource(0, 0));
    }

    /**
     * N:Nのリンクの一覧を取得する.
     * @param accessor link用のaccessor
     * @param srcHandler OEntityDocHandler
     * @param targetSetName targetSetName
     * @param targetEntityTypeId targetEntityTypeId
     * @param queryInfo queryInfo
     * @return ESQuery
     */
    public static List<String> query(final ODataLinkAccessor accessor,
            final EntitySetDocHandler srcHandler,
            final String targetSetName,
            final String targetEntityTypeId,
            final QueryInfo queryInfo) {

        NtoNQueryParameter parameter = new NtoNQueryParameter(srcHandler, targetSetName,
                targetEntityTypeId);

        // IDの一覧を検索
        Integer size = DEFAULT_TOP_VALUE;
        Integer from = 0;
        if (queryInfo != null) {
            if (queryInfo.top != null) {
                size = queryInfo.top;
            }
            if (queryInfo.skip != null) {
                from = queryInfo.skip;
            }
        }

        List<String> ret = new ArrayList<String>();
        PersoniumSearchResponse sr = accessor.search(parameter.getSource(size, from));
        if (sr == null) {
            return ret;
        }

        for (PersoniumSearchHit hit : sr.getHits().getHits()) {
            Map<String, Object> hs = hit.getSource();
            ret.add((String) hs.get(parameter.getTargetKey()));
        }
        return ret;
    }

    /**
     * @return the published
     */
    public Long getPublished() {
        return published;
    }

    /**
     * @param published the published to set
     */
    public void setPublished(Long published) {
        this.published = published;
    }

    /**
     * @return the updated
     */
    public Long getUpdated() {
        return updated;
    }

    /**
     * @param updated the updated to set
     */
    public void setUpdated(Long updated) {
        this.updated = updated;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * ES上のOData Link格納において更新日時を保存するJSONキー.
     */
    public static final String KEY_UPDATED = "u";
    /**
     * ES上のOData Link格納において作成日時を保存するJSONキー.
     */
    public static final String KEY_PUBLISHED = "p";
    /**
     * ES上のOData Link格納においてCellの内部IDを保存するJSONキー.
     */
    public static final String KEY_CELL_ID = "c";
    /**
     * ES上のOData Link格納においてBoxの内部IDを保存するJSONキー.
     */
    public static final String KEY_BOX_ID = "b";
    /**
     * ES上のOData Link格納においてコレクションのnodeidを保存するJSONキー.
     */
    public static final String KEY_NODE_ID = "n";

    /**
     * ES上のOData Link格納において文字列比較で小さい側のタイプ名を保存するJSONキー.
     */
    public static final String KEY_ENT1_TYPE = "t1";
    /**
     * ES上のOData Link格納において文字列比較で小さい側のタイプのエンティティIDを保存するJSONキー.
     */
    public static final String KEY_ENT1_ID = "k1";
    /**
     * ES上のOData Link格納において文字列比較で大きい側のタイプ名を保存するJSONキー.
     */
    public static final String KEY_ENT2_TYPE = "t2";
    /**
     * ES上のOData Link格納において文字列比較で大きい側のタイプのエンティティIDを保存するJSONキー.
     */
    public static final String KEY_ENT2_ID = "k2";

    /**
     * 指定されたエンティティタイプと関連付いているデータのIDを返却する.
     * @param baseEntityType エンティティタイプ
     * @return ID 存在しない場合はnullを返却
     */
    public String getLinkedEntitytIdFromBaseEntityType(String baseEntityType) {
        if (this.ent1Type.equals(baseEntityType)) {
            return this.ent2Key;
        } else if (this.ent2Type.equals(baseEntityType)) {
            return this.ent1Key;
        } else {
            return null;
        }
    }

    /**
     * 指定されたエンティティタイプのIDを返却する.
     * @param entityType エンティティタイプ
     * @return ID 存在しない場合はnullを返却
     */
    public String getEntitytIdFromEntityType(String entityType) {
        if (this.ent1Type.equals(entityType)) {
            return this.ent1Key;
        } else if (this.ent2Type.equals(entityType)) {
            return this.ent2Key;
        } else {
            return null;
        }
    }

    /**
     * N:Nのリンクの一覧を取得するためのクエリ情報を生成するクラス.
     */
    public static class NtoNQueryParameter {
        private String t1;
        private String t2;
        private String k1;
        private String k2;

        /**
         * コンストラクタ.
         * @param srcHandler OEntityDocHandler
         * @param targetSetName ターゲット側のEntitySet名
         * @param targetEntityTypeId ターゲット側のEntityTypeID
         */
        public NtoNQueryParameter(
                final EntitySetDocHandler srcHandler,
                final String targetSetName,
                final String targetEntityTypeId) {
            String srcSetName = srcHandler.getType();
            String srcId = srcHandler.getId();

            TreeMap<String, String> tm = new TreeMap<String, String>();
            if (srcSetName.equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
                tm.put(srcHandler.getEntityTypeId(), srcId);
                tm.put(targetEntityTypeId, "");
            } else {
                tm.put(srcSetName, srcId);
                tm.put(targetSetName, "");
            }
            this.t1 = tm.firstKey();
            this.t2 = tm.lastKey();
            this.k1 = tm.get(t1);
            this.k2 = tm.get(t2);
        }

        /**
         * N:Nのリンクの一覧を取得するためのクエリ情報を生成する（ソートなし）.
         * @param size 取得する件数
         * @param from フェッチ数
         * @return ESQuery
         */
        public Map<String, Object> getSource(Integer size, Integer from) {
            Map<String, Object> source = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            Map<String, Object> and = new HashMap<String, Object>();
            List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();

            if (this.k1.length() == 0) {
                filters.add(QueryMapFactory.termQuery(KEY_ENT2_ID, this.k2));
            } else {
                filters.add(QueryMapFactory.termQuery(KEY_ENT1_ID, this.k1));
            }

            List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
            queries.add(QueryMapFactory.termQuery(KEY_ENT1_TYPE, this.t1));
            queries.add(QueryMapFactory.termQuery(KEY_ENT2_TYPE, this.t2));

            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

            and.put("filters", filters);
            filter.put("and", and);

            source.put("query", query);
            source.put("filter", filter);
            source.put("size", size);
            source.put("from", from);
            return source;
        }

        /**
         * Expand対象データ取得時のN:Nのリンクの一覧を取得するためのクエリ情報を生成する.
         * @param size 取得する件数
         * @return ESQuery
         */
        public Map<String, Object> getSourceForExpand(Integer size) {
            Map<String, Object> source = new HashMap<String, Object>();

            List<Map<String, Object>> queries = new ArrayList<Map<String, Object>>();
            if (this.k1.length() == 0) {
                queries.add(QueryMapFactory.termQuery(KEY_ENT2_ID, this.k2));
            } else {
                queries.add(QueryMapFactory.termQuery(KEY_ENT1_ID, this.k1));
            }
            queries.add(QueryMapFactory.termQuery(KEY_ENT1_TYPE, this.t1));
            queries.add(QueryMapFactory.termQuery(KEY_ENT2_TYPE, this.t2));

            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(queries));

            source.put("query", query);
            source.put("sort", QueryMapFactory.sortQuery(getTargetKey(), EsQueryHandler.SORT_ASC));
            source.put("size", size);
            return source;
        }

        /**
         * ターゲット側のキー名(k1, k2)を取得する.
         */
        String getTargetKey() {
            if (this.k1.length() == 0) {
                return KEY_ENT1_ID;
            } else {
                return KEY_ENT2_ID;
            }
        }
    }

}

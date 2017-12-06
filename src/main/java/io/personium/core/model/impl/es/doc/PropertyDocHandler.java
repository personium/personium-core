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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odata4j.edm.EdmDataServices;

import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyAlias;
import io.personium.core.odata.OEntityWrapper;

/**
 * プロパティのDocHandler.
 */
public class PropertyDocHandler extends OEntityDocHandler implements EntitySetDocHandler {

    Map<String, PropertyAlias> propertyAliasMap;
    Map<String, String> entityTypeMap;
    String linkTypeName = Property.P_ENTITYTYPE_NAME.getName();

    /**
     * コンストラクタ.
     */
    public PropertyDocHandler() {
        this.propertyAliasMap = null;
    }

    /**
     * OEntityWrapperから IDのないDocHandlerをつくるConstructor.
     * @param type ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    public PropertyDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        this.propertyAliasMap = null;
        initInstance(type, oEntityWrapper, metadata);
        this.staticFields.put(Property.P_IS_DECLARED.getName(), true);
    }

    /**
     * コンストラクタ.
     * @param cellId セルID
     * @param boxId ボックスID
     * @param nodeId ノードID
     * @param entityTypeId 紐付くエンティティタイプのID
     * @param source 静的プロパティフィールド
     */
    public PropertyDocHandler(String cellId,
            String boxId,
            String nodeId,
            String entityTypeId,
            Map<String, Object> source) {
        this.propertyAliasMap = null;
        this.type = Property.EDM_TYPE_NAME;
        this.version = 0L;

        // Cell, Box, Nodeの紐付
        this.setCellId(cellId);
        this.setBoxId(boxId);
        this.setNodeId(nodeId);

        // published, updated
        long crrTime = System.currentTimeMillis();
        this.setPublished(crrTime);
        this.setUpdated(crrTime);

        this.staticFields = source;
        this.dynamicFields = new HashMap<String, Object>();
        this.hiddenFields = new HashMap<String, Object>();
        Map<String, Object> linksMap = new HashMap<String, Object>();
        linksMap.put(EntityType.EDM_TYPE_NAME, entityTypeId);
        this.manyToOnelinkId = linksMap;
    }

    /**
     * EntityType名を返却する.
     * @return EntityType名
     */
    public String getEntityTypeName() {
        String entityTypeId = (String) this.manyToOnelinkId.get(EntityType.EDM_TYPE_NAME);
        return this.entityTypeMap.get(linkTypeName + entityTypeId);
    }

    /**
     * 登録済のエイリアス一覧を返却する.
     * @return propertyMap 登録済のエイリアス一覧
     */
    public Map<String, String> getEntityTypeMap() {
        return this.entityTypeMap;
    }

    /**
     * 登録済のエイリアス一覧を設定する.
     * @param map セットする propertyMap
     */
    public void setEntityTypeMap(Map<String, String> map) {
        this.entityTypeMap = map;
    }

    /**
     * 登録済のエイリアス一覧を返却する.
     * @return propertyMap 登録済のエイリアス一覧
     */
    public Map<String, PropertyAlias> getPropertyAliasMap() {
        return this.propertyAliasMap;
    }

    /**
     * 登録済のエイリアス一覧を設定する.
     * @param map セットする propertyAliasMap
     */
    public void setPropertyAliasMap(Map<String, PropertyAlias> map) {
        this.propertyAliasMap = map;
    }

    /**
     * ES/MySQL登録用データを取得する.
     * @return 登録用データ
     */
    @Override
    public Map<String, Object> getSource() {
        String dataType = (String) this.staticFields.get("Type");
        String entityTypeId = (String) this.manyToOnelinkId.get(EntityType.EDM_TYPE_NAME);
        String entityTypeName = this.entityTypeMap.get(linkTypeName + entityTypeId);
        String alias = getNextAlias(entityTypeName, dataType);
        String propertyName = (String) this.staticFields.get("Name");
        String key = "Name='" + propertyName + "'," + linkTypeName + "='" + entityTypeName + "'";
        PropertyAlias propertyAlias = new PropertyAlias(linkTypeName, propertyName, entityTypeName, alias);
        Map<String, Object> ret = setSource(key, propertyAlias);
        return ret;
    }

    /**
     * ES/MySQL登録用データをMapオブジェクトに設定する.
     * @param propertyAlias プロパティのAlias情報
     * @param key キー
     * @return 作成したMapオブジェクト
     */
    protected Map<String, Object> setSource(String key, PropertyAlias propertyAlias) {
        this.propertyAliasMap.put(key, propertyAlias);
        this.hiddenFields.put("Alias", propertyAlias.getAlias());
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put(KEY_STATIC_FIELDS, this.staticFields);
        ret.put(KEY_HIDDEN_FIELDS, this.hiddenFields);
        ret.put(KEY_PUBLISHED, this.published);
        ret.put(KEY_UPDATED, this.updated);
        ret.put(KEY_CELL_ID, this.cellId);
        ret.put(KEY_BOX_ID, this.boxId);
        ret.put(KEY_NODE_ID, this.nodeId);
        ret.put(KEY_LINK, this.manyToOnelinkId);
        return ret;
    }

    /**
     * PropertyのNameを取得する.
     * @return PropertyのName
     */
    public String getName() {
        return (String) this.staticFields.get("Name");
    }

    /**
     * 登録済みプロパティの最大値＋１のプロパティAliasを取得する.
     * @param entityTypeName EntityType名
     * @param dataType プロパティのデータ型名
     * @return 採番したプロパティ名のAlias
     */
    protected String getNextAlias(String entityTypeName, String dataType) {
        // データ種別からエイリアスのプレフィックスを決定する
        String aliasPrefix = "P";
        if (!dataType.startsWith("Edm.")) {
            aliasPrefix = "C";
        }

        // プロパティとエイリアスの対応MapをEntityType名で絞り込んでエイリアスのListを作る
        List<Integer> aliasList = new ArrayList<Integer>();
        for (Map.Entry<String, PropertyAlias> entry : this.propertyAliasMap.entrySet()) {
            if (entry.getKey().endsWith(this.linkTypeName + "='" + entityTypeName + "'")) {
                String value = entry.getValue().getAlias();
                if (value == null) {
                    // 暫定的なnullチェック
                    continue;
                }
                if (!value.startsWith(aliasPrefix)) {
                    // プレフィックス(P/C)が異なるエイリアスは除外
                    continue;
                }
                // 数値部分のみ切り出し
                int num = getAliasNumber(value);
                aliasList.add(Integer.valueOf(num));
            }
        }

        int nextNum = aliasList.size() + 1;

        // プロパティ番号の採番
        // 単純型と複合型の通番は個別に採番する。
        if (aliasList.contains(Integer.valueOf(nextNum))) {
            // 既に使われているので空きを探す
            for (int i = 0; i < aliasList.size(); i++) {
                if (!aliasList.contains(Integer.valueOf(i + 1))) {
                    nextNum = i + 1;
                    break;
                }
            }
        }

        String newAlias = String.format("%s%03d", aliasPrefix, nextNum);
        return newAlias;
    }

    /**
     * Alias文字列からインデックスを取得する.
     * @param alias Alias文字列
     * @return インデックス
     */
    protected int getAliasNumber(String alias) {
        if (alias.startsWith("C")) {
            String[] splitedAlias = alias.split(":");
            return Integer.parseInt(splitedAlias[0].substring(1));
        }
        return Integer.parseInt(alias.substring(1));
    }
}

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

import java.util.Map;

import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.OrderByExpression;

import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * UserDataのelasticsearchクエリハンドラー.
 */
public class UserDataQueryHandler extends EsQueryHandler implements ODataQueryHandler {
    /**
     * Property/ComplexTypePropertyとAliasのマッピングデータ.
     */
    private Map<String, PropertyAlias> propertyAliasMap;

    /**
     * コンストラクタ.
     * @param entityType エンティティタイプ
     * @param map プロパティ名とAliasのMap
     */
    public UserDataQueryHandler(EdmEntityType entityType, Map<String, PropertyAlias> map) {
        super(entityType);
        this.propertyAliasMap = map;
    }

    @Override
    public void visit(OrderByExpression expr) {
        log.debug("visit(OrderByExpression expr)");
        if (!(expr.getExpression() instanceof EntitySimpleProperty)) {
            throw DcCoreException.OData.FILTER_PARSE_ERROR;
        }

        // ソート対象のプロパティスキーマを取得する
        String name = ((EntitySimpleProperty) expr.getExpression()).getPropertyName();
        EdmProperty edmProperty = this.entityType.findProperty(name);

        // プロパティが存在しない場合はソート条件に追加しない
        if (edmProperty == null) {
            return;
        }

        // 配列に対するソート指定時はエラーとする
        if (CollectionKind.List.equals(edmProperty.getCollectionKind())) {
            throw DcCoreException.OData.CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY;
        }

        if (!isUntouched(name, edmProperty)) {
            super.visit(expr);
        } else {
            // 文字列の場合
            String key = getSearchKey(expr.getExpression(), true);
            String orderOption = getOrderOption(expr.getDirection());
            Map<String, Object> orderByValue = null;
            if (DcCoreConfig.getOrderbySortOrder()) {
                orderByValue = UserDataQueryHandlerHelper.getOrderByValueForMissingFirst(orderOption, key);
            } else {
                orderByValue = UserDataQueryHandlerHelper.getOrderByValue(orderOption, key);
            }
            this.orderBy.put(UserDataQueryHandlerHelper.getOrderByKey(key), orderByValue);
        }
    }

    @Override
    protected String getSearchKey(CommonExpression expr, Boolean isUntouched) {
        // 検索キーとして設定を行う
        String name = ((EntitySimpleProperty) expr).getPropertyName();
        EdmProperty edmProperty = this.entityType.findProperty(name);

        // published, updated
        if (Common.P_PUBLISHED.getName().equals(name)) {
            return OEntityDocHandler.KEY_PUBLISHED;
        } else if (Common.P_UPDATED.getName().equals(name)) {
            return OEntityDocHandler.KEY_UPDATED;
        }

        // s.フィールドフィールドを検索する
        String fieldPrefix = OEntityDocHandler.KEY_STATIC_FIELDS;

        // キー名をAliasに変換する
        String key = "Name='" + name + "',_EntityType.Name='" + this.entityType.getName() + "'";
        String keyName = getAlias(key, this.entityType.getName());
        if (keyName == null) {
            keyName = name;
        }

        // 型によってサフィックス（検索対象フィールド）変更する
        String suffix = getSuffix(edmProperty);

        if (isUntouched) {
            return fieldPrefix + "." + keyName + "." + suffix;
        } else {
            return fieldPrefix + "." + keyName;
        }
    }

    /**
     * untouchedフィールドを使用すべき項目かどうかを判定する.
     * @param name プロパティ名
     * @param edmProperty プロパティのスキーマ情報
     * @return untouchedフィールドを使用すべき場合はtrue、そうでない場合はfalse
     */
    private boolean isUntouched(String name, EdmProperty edmProperty) {
        if (Common.P_ID.getName().equals(name)
                || Common.P_PUBLISHED.getName().equals(name)
                || Common.P_UPDATED.getName().equals(name)
                || EdmSimpleType.SINGLE.equals(edmProperty.getType())
                || EdmSimpleType.DOUBLE.equals(edmProperty.getType())
                || EdmSimpleType.INT32.equals(edmProperty.getType())) {
            return false;
        }
        return true;
    }

    private String getSuffix(EdmProperty edmProperty) {
        String suffix = "untouched";
        if (edmProperty != null) {
            if (EdmSimpleType.SINGLE.equals(edmProperty.getType())
                    || EdmSimpleType.DOUBLE.equals(edmProperty.getType())) {
                suffix = "double";
            } else if (EdmSimpleType.INT32.equals(edmProperty.getType())) {
                suffix = "long";
            }
        }
        return suffix;
    }

    @Override
    protected String getFieldName(String prop) {
        String key = "Name='" + prop + "',_EntityType.Name='" + this.entityType.getName() + "'";
        String keyName = getAlias(key, this.entityType.getName());
        return OEntityDocHandler.KEY_STATIC_FIELDS + "." + keyName;
    }

    /**
     * プロパティのAlias名をマッピングデータから取得する.
     * @param key マッピングデータの検索キー
     * @param propertyName プロパティ名
     * @return Alias名
     */
    private String getAlias(String key, String propertyName) {
        if (propertyName.startsWith("_")) {
            return propertyName;
        }
        PropertyAlias alias = this.propertyAliasMap.get(key);
        if (alias != null) {
            return alias.getAlias();
        }
        return null;
    }

}

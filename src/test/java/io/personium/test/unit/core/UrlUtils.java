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
package io.personium.test.unit.core;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang.StringUtils;

import io.personium.core.model.Box;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.utils.UriUtils;

/**
 * URLの作成の組立を行う関数群.
 */
public final class UrlUtils {

    /** リクエスト送信先URLを取得するプロパティのキー. */
    public static final String PROP_TARGET_URL = "io.personium.test.target";
    /** デフォルトのリクエスト送信先URL. */
    public static final String DEFAULT_TARGET_URL = "http://localhost:9998";

    /**
     * システムプロパティから接続先のURLを取得する。 指定がない場合はデフォルトのURLを使用する。
     */
    private static String baseUrl = System.getProperty(PROP_TARGET_URL, DEFAULT_TARGET_URL);

    /**
     * @param baseUrl baseUrl
     */
    public static void setBaseUrl(String baseUrl) {
        UrlUtils.baseUrl = baseUrl;
    }

    /**
     * Hostのgetter.
     * @return baseUrl
     */
    public static String getHost() {
        URL url;
        try {
            url = new URL(UrlUtils.baseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        StringBuilder host = new StringBuilder();
        host.append(url.getAuthority());
        return host.toString();
    }

    /**
     * baseUrlのgetter.
     * @return baseUrl
     */
    public static String getBaseUrl() {
        return UrlUtils.baseUrl;
    }

    private UrlUtils() {
    }

    /**
     * UnitのルートURL取得.
     * @return UnitのルートURL
     */
    public static String unitRoot() {
        return String.format("%s/", baseUrl);
    }

    /**
     * セルのルートURL取得.
     * @param cellName セル名
     * @return セルのルートURL
     */
    public static String cellRoot(final String cellName) {
        return String.format("%s/%s/", baseUrl, cellName);
    }

    /**
     * メッセージ受信URL取得.
     * @param cellName セル名
     * @return メッセージ受信URL
     */
    public static String receivedMessage(final String cellName) {
        return String.format("%s/%s/__message/port", baseUrl, cellName);
    }

    /**
     * メッセージ受信URL取得.
     * @param cellName セル名
     * @return メッセージ受信URL
     */
    public static String receivedMessageCtl(final String cellName) {
        return String.format("%s/%s/__ctl/%s", baseUrl, cellName, ReceivedMessage.EDM_TYPE_NAME);
    }

    /**
     * メッセージ受信URL取得.
     * @param cellName セル名
     * @param messageId メッセージID
     * @return メッセージ受信URL
     */
    public static String receivedMessageCtl(final String cellName, final String messageId) {
        return String.format("%s/%s/__ctl/%s('%s')", baseUrl, cellName, ReceivedMessage.EDM_TYPE_NAME, messageId);
    }

    /**
     * メッセージ承認URL取得.
     * @param cellName セル名
     * @param messageId メッセージID
     * @return メッセージ受信URL
     */
    public static String approvedMessage(final String cellName, final String messageId) {
        return String.format("%s/%s/__message/received/%s", baseUrl, cellName, messageId);
    }

    /**
     * メッセージ送信URL取得.
     * @param cellName セル名
     * @return メッセージ送信URL
     */
    public static String sentMessageCtl(final String cellName) {
        return String.format("%s/%s/__ctl/%s", baseUrl, cellName, SentMessage.EDM_TYPE_NAME);
    }

    /**
     * メッセージ送信URL取得.
     * @param cellName セル名
     * @param messageId メッセージID
     * @return メッセージ送信URL
     */
    public static String sentMessageCtl(final String cellName, final String messageId) {
        return String.format("%s/%s/__ctl/%s('%s')", baseUrl, cellName, SentMessage.EDM_TYPE_NAME, messageId);
    }

    /**
     * ユニット制御APIのURL取得.
     * @param type タイプ名
     * @return URL
     */
    public static String unitCtl(final String type) {
        return String.format("%s/__ctl/%s", baseUrl, type);
    }

    /**
     * ユニット制御APIのURL取得.
     * @param type タイプ名
     * @param idString ID文字列
     * @return URL
     */
    public static String unitCtl(final String type, final String idString) {
        return String.format("%s/__ctl/%s('%s')", baseUrl, type, idString);
    }

    /**
     * ユニット制御APIのURL取得.
     * @param type タイプ名
     * @param idString ID文字列
     * @return URL
     */
    public static String unitCtlCompKey(final String type, final String idString) {
        return String.format("%s/__ctl/%s(%s)", baseUrl, type, idString);
    }

    /**
     * ユニット制御APIのURL取得.
     * @param type タイプ名
     * @param idString ID文字列
     * @param navprop Navigationプロパティ
     * @return URL
     */
    public static String unitCtl(final String type, final String idString, final String navprop) {
        return String.format("%s/__ctl/%s('%s')/%s", baseUrl, type, idString, navprop);
    }

    /**
     * セル制御APIのURL取得.
     * @param cellName セル名
     * @param type タイプ名（Account、Role等）
     * @return URL
     */
    public static String cellCtl(final String cellName, final String type) {
        return String.format("%s/%s/__ctl/%s", baseUrl, cellName, type);
    }

    /**
     * セル制御APIのURL取得.
     * @param cellName セル名
     * @param type タイプ名（Account、Role等）
     * @param idString ID文字列
     * @return URL
     */
    public static String cellCtl(final String cellName, final String type, final String idString) {
        return String.format("%s/%s/__ctl/%s('%s')", baseUrl, cellName, type, idString);
    }

    /**
     * セル制御APIのURL取得(シングルクォート無し).
     * @param cellName セル名
     * @param type タイプ名（Account、Role等）
     * @param idString ID文字列
     * @return URL
     */
    public static String cellCtlWithoutSingleQuote(final String cellName, final String type, final String idString) {
        return String.format("%s/%s/__ctl/%s(%s)", baseUrl, cellName, type, idString);
    }

    /**
     * セル制御APIのNavigationProperty経由URL取得.
     * @param cellName セル名
     * @param entityTypeName エンティティタイプ名
     * @param entityTypeKey エンティティタイプキー
     * @param navigationPropertyName ナビゲーションプロパティ名
     * @return セル制御APIのNavigationProperty経由URL
     */
    public static String cellCtlNagvigationProperty(final String cellName,
            final String entityTypeName,
            final String entityTypeKey,
            final String navigationPropertyName) {
        return String.format("%s/%s/__ctl/%s('%s')/%s?$format=json",
                baseUrl, cellName, entityTypeName, entityTypeKey, navigationPropertyName);
    }

    /**
     * セル制御APIの$linksのMultiURL取得.
     * @param cellName セル名
     * @param entityTypeName エンティティタイプ名
     * @param entityTypeKey エンティティタイプキー
     * @param navigationPropertyName ナビゲーションプロパティ名
     * @return セル制御APIの$linksのMultiURL取得
     */
    public static String cellCtlLinksMulti(final String cellName,
            final String entityTypeName,
            final String entityTypeKey,
            final String navigationPropertyName) {
        return String.format("%s/%s/__ctl/%s('%s')/$links/%s?$format=json",
                baseUrl, cellName, entityTypeName, entityTypeKey, navigationPropertyName);
    }

    /**
     * セル制御APIの$linksURL取得.
     * @param cellName セル名
     * @param entityTypeName エンティティタイプ名
     * @param entityTypeKey エンティティタイプキー
     * @param navigationPropertyName ナビゲーションプロパティ名
     * @param navigationPropertyKey ナビゲーションプロパティキー
     * @return セル制御APIの$links
     */
    public static String cellCtlLinks(final String cellName,
            final String entityTypeName,
            final String entityTypeKey,
            final String navigationPropertyName,
            final String navigationPropertyKey) {
        return String.format("%s/%s/__ctl/%s('%s')/$links/%s('%s')?$format=json",
                baseUrl, cellName, entityTypeName, entityTypeKey, navigationPropertyName, navigationPropertyKey);
    }

    /**
     * アカウント・ロール結びつけAPIのURL取得.
     * @param cellName セル名
     * @param account アカウント名
     * @return アカウント・ロール結びつけAPIのURL
     */
    public static String accountLinks(final String cellName, final String account) {
        // TODO Acceptヘッダに未対応のため?$format=jsonを付与
        return String.format("%s/%s/__ctl/Account('%s')/$links/_Role?$format=json", baseUrl, cellName, account);
    }

    /**
     * アカウント・ロール結びつけAPIのURL取得.
     * @param cellName セル名
     * @param account アカウント名
     * @param role ロール名
     * @return アカウント・ロール結びつけAPIのURL
     */
    public static String accountLink(final String cellName, final String account, final String role) {
        return String.format("%s/%s/__ctl/Account('%s')/$links/_Role('%s')",
                baseUrl, cellName, account, role);
    }

    /**
     * 認証エンドポイント.
     * @param cellName セル名
     * @return 認証エンドポイントURL
     */
    public static String auth(final String cellName) {
        return String.format("%s/%s/__token", baseUrl, cellName);
    }

    /**
     * ログ取り出しエンドポイント.
     * @param cellName セル名
     * @return 認証エンドポイントURL
     */
    public static String log(final String cellName) {
        return String.format("%s/%s/__log", baseUrl, cellName);
    }

    /**
     * BOXアクセス.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param pathInBox ボックス内パス
     * @return 認証エンドポイントURL
     */
    public static String box(final String cellName, final String boxName, final String... pathInBox) {
        String path = "";
        if (pathInBox != null) {
            path = StringUtils.join(pathInBox, "/");
        }
        return String.format("%s/%s/%s/%s", baseUrl, cellName, boxName, path);
    }

    /**
     * BOXrootアクセス.
     * @param cellName セル名
     * @param boxName ボックス名
     * @return 認証エンドポイントURL
     */
    public static String boxRoot(final String cellName, final String boxName) {
        return String.format("%s/%s/%s", baseUrl, cellName, boxName);
    }

    /**
     * BoxURl取得APIアクセス.
     * @param cellName セル名
     * @return 認証エンドポイントURL
     */
    public static String boxUrl(final String cellName) {
        return boxUrl(cellName, null);
    }

    /**
     * BoxURl取得APIアクセス.
     * @param cellName セル名
     * @param query クエリ
     * @return 認証エンドポイントURL
     */
    public static String boxUrl(final String cellName, final String query) {
        if (query == null) {
            return String.format("%s/%s/__box", baseUrl, cellName);
        } else {
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(query, "utf-8");
            } catch (UnsupportedEncodingException e) {
                // Normally it does not occur.
                return String.format("%s/%s/__box", baseUrl, cellName);
            }
            return String.format("%s/%s/__box?schema=%s", baseUrl, cellName, encodedQuery);
        }
    }

    /**
     * AssociationEndアクセス.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param assocName アソシエーションエンドの値
     * @param etName EntityTypeNameの値
     * @return 認証エンドポイントURL
     */
    public static String associationEnd(final String cellName,
            final String boxName,
            final String colName,
            final String assocName,
            final String etName) {
        if (etName != null) {
            return String.format("%s/%s/%s/%s/$metadata/AssociationEnd(Name='%s',_EntityType.Name='%s')", baseUrl,
                    cellName, boxName, colName, assocName, etName);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/AssociationEnd(Name='%s',_EntityType.Name=null)", baseUrl,
                    cellName, boxName, colName, assocName);
        }
    }

    /**
     * AssociationEndの$linksアクセス.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param srcAssocName ソース側アソシエーションエンドの値
     * @param srcEtName ソース側EntityTypeNameの値
     * @param tgtAssocName ターゲット側アソシエーションエンドの値
     * @param tgtEtName ターゲット側EntityTypeNameの値
     * @return 認証エンドポイントURL
     */
    public static String associationEndLink(final String cellName,
            final String boxName,
            final String colName,
            final String srcAssocName,
            final String srcEtName,
            final String tgtAssocName,
            final String tgtEtName) {
        String format = "%s/%s/%s/%s/$metadata/AssociationEnd(Name='%s',_EntityType.Name='%s')"
                + "/$links/_AssociationEnd(Name='%s',_EntityType.Name='%s')";
        return String.format(format, baseUrl, cellName, boxName, colName,
                srcAssocName, srcEtName, tgtAssocName, tgtEtName);
    }

    /**
     * userDataアクセス.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityType エンティティタイプ名
     * @return userDataURL
     */
    public static String userData(final String cellName,
            final String boxName,
            final String colName,
            final String entityType) {
        return String.format("%s/%s/%s/%s/%s",
                baseUrl, cellName, boxName, colName, entityType);
    }

    /**
     * ロールリソースのURL取得.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param roleName ロール名
     * @return ロールリソースURL
     */
    public static String roleResource(final String cellName, final String boxName, final String roleName) {
        String box = null;
        if (boxName == null) {
            box = Box.DEFAULT_BOX_NAME;
        } else {
            box = boxName;
        }
        return String.format("%s/%s/__role/%s/%s", baseUrl, cellName, box, roleName);
    }

    /**
     * ロールのURL取得.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param roleName ロール名
     * @return ロールリソースURL
     */
    public static String roleUrl(final String cellName, final String boxName, final String roleName) {
        String box = null;
        if (boxName == null) {
            box = "null";
        } else {
            box = "'" + boxName + "'";
        }
        return String.format("%s/%s/__ctl/Role(_Box.Name=%s,Name='%s')", baseUrl, cellName, box, roleName);
    }

    /**
     * RelationのURL取得.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param relationName Relation名
     * @return RelationリソースURL
     */
    public static String relationUrl(final String cellName, final String boxName, final String relationName) {
        String box = null;
        if (boxName == null) {
            box = "null";
        } else {
            box = "'" + boxName + "'";
        }
        return String.format("%s/%s/__ctl/Relation(_Box.Name=%s,Name='%s')", baseUrl, cellName, box, relationName);
    }

    /**
     * Get RelationClassURL.
     * @param cellName Cell name
     * @param relationName Relation name
     * @return RelationClassURL
     */
    public static String relationClassUrl(final String cellName, final String relationName) {
        return String.format("%s/%s/__relation/__/%s", baseUrl, cellName, relationName);
    }

    /**
     * Get unit local RelationClassURL.
     * @param cellName Cell name
     * @param relationName Relation name
     * @return unit local RelationClassURL
     */
    public static String unitLocalRelationClassUrl(final String cellName, final String relationName) {
        return String.format("%s%s/__relation/__/%s", UriUtils.SCHEME_UNIT_URI, cellName, relationName);
    }

    /**
     * Get RoleClassURL.
     * @param cellName Cell name
     * @param roleName Role name
     * @return RoleClassURL
     */
    public static String roleClassUrl(final String cellName, final String roleName) {
        return String.format("%s/%s/__role/__/%s", baseUrl, cellName, roleName);
    }

    /**
     * Get unit local RoleClassURL.
     * @param cellName Cell name
     * @param roleName Role name
     * @return unit local RoleClassURL
     */
    public static String unitLocalRoleClassUrl(final String cellName, final String roleName) {
        return String.format("%s%s/__role/__/%s", UriUtils.SCHEME_UNIT_URI, cellName, roleName);
    }

    /**
     * ExtRoleのURL取得.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param relationName リレーション名
     * @param extRoleName ExtRole名
     * @return ExtRoleリソースURL
     */
    public static String extRoleUrl(final String cellName,
            final String boxName,
            final String relationName,
            final String extRoleName) {
        String box = null;
        String relation = null;
        String extRole = "'" + extRoleName + "'";
        if (boxName == null) {
            box = "null";
        } else {
            box = "'" + boxName + "'";
        }
        if (relationName == null) {
            relation = "null";
        } else {
            relation = "'" + relationName + "'";
        }

        return String.format("%s/%s/__ctl/ExtRole(ExtRole=%s,_Relation.Name=%s,_Relation._Box.Name=%s)", baseUrl,
                cellName, extRole, relation, box);
    }

    /**
     * ExtCellリソースのURL取得.
     * @param cellName cellName
     * @param extCellUri extCellUri
     * @return ExtCellリソースURL
     */
    public static String extCellResource(final String cellName, final String extCellUri) {
        return String.format("%s/%s/__ctl/ExtCell('%s')", baseUrl, cellName, extCellUri);
    }

    /**
     * ACLロールリソース相対パスのURL取得.
     * @param boxName ボックス名
     * @param roleName ロール名
     * @return ロールリソース相対パスのURL
     */
    public static String aclRelativePath(final String boxName, final String roleName) {
        return String.format("../%s/%s", boxName, roleName);
    }

    /**
     * Subjectに入れるアカウントのURL取得.
     * @param cellName セル名
     * @param accountName アカウント名
     * @return Subjectに入れるアカウントのURL
     */
    public static String subjectUrl(final String cellName, final String accountName) {
        return String.format("%s/%s/#%s", baseUrl, cellName, accountName);
    }

    /**
     * ステータス確認用URL取得.
     * @return ステータス確認用URL
     */
    public static String status() {
        return String.format("%s/__status", baseUrl);
    }

    /**
     * EntityTypeリソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityTypeName エンティティタイプ名
     * @return EntityTypeリソースURL
     */
    public static String entityType(final String cellName,
            final String boxName,
            final String colName,
            final String entityTypeName) {
        if (entityTypeName == null) {
            return String.format("%s/%s/%s/%s/$metadata/EntityType", baseUrl,
                    cellName, boxName, colName);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/EntityType('%s')", baseUrl,
                    cellName, boxName, colName, entityTypeName);
        }
    }

    /**
     * ComplexTypeリソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param complexTypeName コンプレックスタイプ名
     * @return ComplexTypeリソースURL
     */
    public static String complexType(final String cellName,
            final String boxName,
            final String colName,
            final String complexTypeName) {
        if (complexTypeName == null) {
            return String.format("%s/%s/%s/%s/$metadata/ComplexType", baseUrl,
                    cellName, boxName, colName);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/ComplexType('%s')", baseUrl,
                    cellName, boxName, colName, complexTypeName);
        }
    }

    /**
     * PropertyリソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param propertyName プロパティ名
     * @param entityTypeName エンティティタイプ名
     * @return PropertyリソースURL
     */
    public static String property(final String cellName,
            final String boxName,
            final String colName,
            final String propertyName,
            final String entityTypeName) {
        if (propertyName == null || entityTypeName == null) {
            return String.format("%s/%s/%s/%s/$metadata/Property", baseUrl,
                    cellName, boxName, colName);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/Property(Name='%s',_EntityType.Name='%s')", baseUrl,
                    cellName, boxName, colName, propertyName, entityTypeName);
        }
    }

    /**
     * ComplexTypePropertyリソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param complexTypePropertyName コンプレックスタイププロパティ名
     * @param complexTypeName コンプレックスタイプ名
     * @return ComplextTypePropertyリソースURL
     */
    public static String complexTypeProperty(final String cellName,
            final String boxName,
            final String colName,
            final String complexTypePropertyName,
            final String complexTypeName) {
        if (complexTypePropertyName == null || complexTypeName == null) {
            return String.format("%s/%s/%s/%s/$metadata/ComplexTypeProperty", baseUrl,
                    cellName, boxName, colName);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/ComplexTypeProperty(Name='%s',_ComplexType.Name='%s')",
                    baseUrl,
                    cellName, boxName, colName, complexTypePropertyName, complexTypeName);
        }
    }

    /**
     * スキーマリソースの$linksURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityType エンティティタイプ名
     * @param entityTypeId エンティティタイプID
     * @param navigationProperty ナビゲーションプロパティ名
     * @param navigationPropertyId ナビゲーションプロパティID
     * @return スキーマリソース$linksURL
     */
    public static String schemaLinksWithSingleQuote(final String cellName,
            final String boxName,
            final String colName,
            final String entityType,
            final String entityTypeId,
            final String navigationProperty,
            final String navigationPropertyId) {
        if (navigationPropertyId == null) {
            return String.format("%s/%s/%s/%s/$metadata/%s('%s')/$links/_%s", baseUrl,
                    cellName, boxName, colName, entityType, entityTypeId, navigationProperty);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/%s('%s')/$links/_%s('%s')", baseUrl,
                    cellName, boxName, colName, entityType, entityTypeId, navigationProperty, navigationPropertyId);
        }
    }

    /**
     * スキーマリソースの$linksURLを取得する(複合キー).
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityType エンティティタイプ名
     * @param entityTypeId エンティティタイプID
     * @param navigationProperty ナビゲーションプロパティ名
     * @param navigationPropertyId ナビゲーションプロパティID
     * @return スキーマリソース$linksURL
     */
    public static String schemaLinks(final String cellName,
            final String boxName,
            final String colName,
            final String entityType,
            final String entityTypeId,
            final String navigationProperty,
            final String navigationPropertyId) {
        if (navigationPropertyId == null) {
            return String.format("%s/%s/%s/%s/$metadata/%s(%s)/$links/_%s", baseUrl,
                    cellName, boxName, colName, entityType, entityTypeId, navigationProperty);
        } else {
            return String.format("%s/%s/%s/%s/$metadata/%s(%s)/$links/_%s(%s)", baseUrl,
                    cellName, boxName, colName, entityType, entityTypeId, navigationProperty, navigationPropertyId);
        }
    }

    /**
     * ユーザデータリソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityTypeName エンティティタイプ名
     * @param key 主キー
     * @return ユーザデータリソースURL
     */
    public static String userdata(final String cellName,
            final String boxName,
            final String colName,
            final String entityTypeName,
            final String key) {
        if (key == null) {
            return String.format("%s/%s/%s/%s/%s", baseUrl,
                    cellName, boxName, colName, entityTypeName);
        } else {
            return String.format("%s/%s/%s/%s/%s('%s')", baseUrl,
                    cellName, boxName, colName, entityTypeName, key);
        }
    }

    /**
     * ユーザデータNP経由リソースURLを取得する.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityTypeName エンティティタイプ名
     * @param key 主キー
     * @param navPropName NP名
     * @return ユーザデータリソースURL
     */
    public static String userdataNP(final String cellName,
            final String boxName,
            final String colName,
            final String entityTypeName,
            final String key,
            final String navPropName) {
        return String.format("%s/%s/%s/%s/%s('%s')/_%s", baseUrl,
                cellName, boxName, colName, entityTypeName, key, navPropName);
    }
}

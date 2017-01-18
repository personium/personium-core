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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.property;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.json.simple.JSONObject;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.EntityType;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * PropertyUtils.
 */
public class PropertyUtils {
    /**
     * コンストラクタ.
     */
    private PropertyUtils() {
    }

    /** Property NameKey名. */
    public static final String PROPERTY_NAME_KEY = Property.P_NAME.getName().toString();

    /** Property _EntityTypeKey名. */
    public static final String PROPERTY_ENTITYTYPE_NAME_KEY = Property.P_ENTITYTYPE_NAME.getName().toString();

    /** Property TypeKey名. */
    public static final String PROPERTY_TYPE_KEY = Property.P_TYPE.getName().toString();

    /** Property NullableKey名. */
    public static final String PROPERTY_NULLABLE_KEY = Property.P_NULLABLE.getName().toString();

    /** Property DefaultValueKey名. */
    public static final String PROPERTY_DEFAULT_VALUE_KEY = Property.P_DEFAULT_VALUE.getName().toString();

    /** Property CollectionKindKey名. */
    public static final String PROPERTY_COLLECTION_KIND_KEY = Property.P_COLLECTION_KIND.getName().toString();

    /** Property IsKeyKey名. */
    public static final String PROPERTY_IS_KEY_KEY = Property.P_IS_KEY.getName().toString();

    /** Property UniqueKeyKey名. */
    public static final String PROPERTY_UNIQUE_KEY_KEY = Property.P_UNIQUE_KEY.getName().toString();

    /** Property isDeclared名. */
    public static final String PROPERTY_IS_DECLARED_KEY = Property.P_IS_DECLARED.getName().toString();

    /** PropertyリソースURL. */
    public static final String REQUEST_URL =
            UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);

    /** 名前空間. */
    public static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + Property.EDM_TYPE_NAME;

    /**
     * PropertyをEntityTypeからのNP経由で登録する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityTypeName EntityType名
     * @param propertyName Property名
     * @param type PropertyのType項目
     * @param nullable PropertyのNullable項目
     * @param defaultValue PropertyのDefaultValue項目
     * @param collectionKind PropertyのcollectionKind項目
     * @param isKey PropertyのisKey項目
     * @param uniqueKey PropertyのUniqueKey項目
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaPropertyNP(
            String token, String cell, String box, String collection,
            String entityTypeName, String propertyName,
            String type, Boolean nullable, String defaultValue, String collectionKind,
            Boolean isKey, String uniqueKey, int code) {

        // リクエストボディの組み立て
        JSONObject body = new JSONObject();
        body.put("Name", propertyName);
        body.put("_EntityType.Name", entityTypeName);
        body.put("Type", type);
        body.put("Nullable", nullable);
        body.put("DefaultValue", defaultValue);
        body.put("CollectionKind", collectionKind);
        body.put("IsKey", isKey);
        body.put("UniqueKey", uniqueKey);

        TResponse res = Http.request("box/odatacol/schema/createViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("entityType", "EntityType")
                .with("id", entityTypeName)
                .with("navPropName", "_Property")
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * PropertyをEntityTypeからのNP経由で一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityTypeName EntityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaPropertyNP(
            String cell, String box, String collection,
            String entityTypeName, int code) {

        TResponse res = Http.request("box/odatacol/schema/listViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("entityType", "EntityType")
                .with("id", entityTypeName)
                .with("navPropName", "_Property")
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * Propertyを登録する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityTypeName EntityType名
     * @param propertyName Property名
     * @param type PropertyのType項目
     * @param nullable PropertyのNullable項目
     * @param defaultValue PropertyのDefaultValue項目
     * @param collectionKind PropertyのcollectionKind項目
     * @param isKey PropertyのisKey項目
     * @param uniqueKey PropertyのUniqueKey項目
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static DcResponse create(
            String token, String cell, String box, String collection,
            String entityTypeName, String propertyName,
            String type, boolean nullable, Object defaultValue, String collectionKind,
            boolean isKey, String uniqueKey, int code) {

        String url = UrlUtils.property(cell, box, collection, null, null);
        DcRequest req = DcRequest.post(url);
        req.header(HttpHeaders.AUTHORIZATION, token);
        req.addJsonBody(PROPERTY_NAME_KEY, propertyName);
        req.addJsonBody(PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
        req.addJsonBody(PROPERTY_TYPE_KEY, type);
        req.addJsonBody(PROPERTY_NULLABLE_KEY, nullable);
        req.addJsonBody(PROPERTY_DEFAULT_VALUE_KEY, defaultValue);
        req.addJsonBody(PROPERTY_COLLECTION_KIND_KEY, collectionKind);
        req.addJsonBody(PROPERTY_IS_KEY_KEY, isKey);
        req.addJsonBody(PROPERTY_UNIQUE_KEY_KEY, uniqueKey);

        // リクエスト実行
        DcResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * PropertyのLocation URLを作成する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param propertyName Property名
     * @param entityTypeName EntityType名
     * @return Location URL
     */
    public static String composeLocationUrl(String cell, String box, String collection,
            String propertyName, String entityTypeName) {
        return UrlUtils.property(cell, box, collection, propertyName, entityTypeName);
    }

    /**
     * Propertyを取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param propertyName Property名
     * @param entityTypeName EntityType名
     * @return レスポンス
     */
    public static DcResponse get(String token, String cell, String box,
            String collection, String propertyName, String entityTypeName) {
        String locationUrl = UrlUtils.property(cell, box, collection, propertyName, entityTypeName);

        // Property取得
        DcRequest req = DcRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        return AbstractCase.request(req);
    }

    /**
     * Propertyを取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @return レスポンス
     */
    public static DcResponse list(
            String token,
            String cell,
            String box,
            String collection) {
        return list(token, cell, box, collection, null);
    }

    /**
     * Propertyを取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param query クエリ
     * @return レスポンス
     */
    public static DcResponse list(
            String token,
            String cell,
            String box,
            String collection,
            String query) {
        String locationUrl = UrlUtils.property(cell, box, collection, null, null);
        if (null != query) {
            locationUrl += query;
        }

        // Property取得
        DcRequest req = DcRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        return AbstractCase.request(req);
    }


    /**
     * Propertyを更新する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcPropertyName 更新前Property名
     * @param srcEntityTypeName 更新前EntityType名
     * @param propertyName リクエストに指定するProperty名
     * @param entityTypeName リクエストに指定するEntityType名
     * @param type PropertyのType項目
     * @param nullable PropertyのNullable項目
     * @param defaultValue PropertyのDefaultValue項目
     * @param collectionKind PropertyのcollectionKind項目
     * @param isKey PropertyのisKey項目
     * @param uniqueKey PropertyのUniqueKey項目
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static DcResponse update(
            String token, String cell, String box,
            String collection, String srcPropertyName,
            String srcEntityTypeName, String propertyName,
            String entityTypeName, String type, Boolean nullable, Object defaultValue,
            String collectionKind, Boolean isKey, String uniqueKey) {

        // リクエストボディの組み立て
        JSONObject body = new JSONObject();
        body.put("Name", propertyName);
        body.put("_EntityType.Name", entityTypeName);
        body.put("Type", type);
        body.put("Nullable", nullable);
        body.put("DefaultValue", defaultValue);
        body.put("CollectionKind", collectionKind);
        body.put("IsKey", isKey);
        body.put("UniqueKey", uniqueKey);

        return update(token, cell, box, collection, srcPropertyName, srcEntityTypeName, body);

    }

    /**
     * Propertyを更新する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcPropertyName 更新前Property名
     * @param srcEntityTypeName 更新前EntityType名
     * @param body リクエストボディ
     * @return レスポンス
     */
    public static DcResponse update(
            String token, String cell, String box,
            String collection, String srcPropertyName,
            String srcEntityTypeName, JSONObject body) {

        // リクエストパラメータ設定
        DcRequest req = DcRequest.put(UrlUtils.property(cell, box, collection,
                srcPropertyName, srcEntityTypeName));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        req.header(HttpHeaders.IF_MATCH, "*");
        req.addStringBody(body.toJSONString());

        // リクエスト実行
        return AbstractCase.request(req);

    }

    /**
     * Propertyを削除する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityTypeName EntityType名
     * @param propertyName Property名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static DcResponse delete(
            String token, String cell, String box, String collection,
            String entityTypeName, String propertyName, int code) {

        String url = UrlUtils.property(cell, box, collection, propertyName, entityTypeName);
        DcRequest req = DcRequest.delete(url);
        req.header(HttpHeaders.AUTHORIZATION, token);

        // リクエスト実行
        DcResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * EntityTypeからのNP経由でPropertyを一覧取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param entityTypeName EntityType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listLinks(
            String token, String cell, String box, String collection,
            String entityTypeName, int code) {

        String path = String.format("\\$metadata/%s('%s')", EntityType.EDM_TYPE_NAME, entityTypeName);
        return Http.request("box/odatacol/list-link.txt")
                .with("cellPath", cell)
                .with("boxPath", box)
                .with("colPath", collection)
                .with("srcPath", path)
                .with("trgPath", Property.EDM_TYPE_NAME)
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .returns()
                .statusCode(code)
                .debug();
    }
}

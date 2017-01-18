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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.complextypeproperty;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.json.simple.JSONObject;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.ComplexTypeProperty;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ComplexTypePropertyUtils.
 */
public class ComplexTypePropertyUtils {
    /**
     * コンストラクタ.
     */
    private ComplexTypePropertyUtils() {
    }

    /** ComplexTypeProperty NameKey名. */
    public static final String CT_PROPERTY_NAME_KEY = ComplexTypeProperty.P_NAME.getName().toString();

    /** ComplexTypeProperty __ComplexTypeKey名. */
    public static final String CT_PROPERTY_COMPLEXTYPE_NAME_KEY = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName()
            .toString();

    /** ComplexTypeProperty TypeKey名. */
    public static final String CT_PROPERTY_TYPE_KEY = ComplexTypeProperty.P_TYPE.getName().toString();

    /** ComplexTypeProperty NullableKey名. */
    public static final String CT_PROPERTY_NULLABLE_KEY = ComplexTypeProperty.P_NULLABLE.getName().toString();

    /** ComplexTypeProperty DefaultValueKey名. */
    public static final String CT_PROPERTY_DEFAULT_VALUE_KEY = ComplexTypeProperty.P_DEFAULT_VALUE.getName()
            .toString();

    /** ComplexTypeProperty CollectionKindKey名. */
    public static final String CT_PROPERTY_COLLECTION_KIND_KEY = ComplexTypeProperty.P_COLLECTION_KIND.getName()
            .toString();

    /** ComplexTypePropertyリソースURL. */
    public static final String CTP_REQUEST_URL =
            UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);

    /** 名前空間. */
    public static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + ComplexTypeProperty.EDM_TYPE_NAME;

    /** ComplexTypeリクエストURL. */
    public static final String CT_REQUEST_URL =
            UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);

    /**
     * ComplexTypePropertyのLocation URLを作成する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypePropertyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @return Location URL
     */
    public static String composeLocationUrl(String cell, String box, String collection,
            String complexTypePropertyName, String complexTypeName) {
        return UrlUtils.complexTypeProperty(cell, box, collection, complexTypePropertyName, complexTypeName);
    }

    /**
     * ComplexTypePropertyを登録する.
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypePropertyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @param type ComplexTypePropertyの型
     * @param code 期待するコード
     * @return レスポンス
     */
    public static DcResponse create(
            String cell, String box, String col,
            String complexTypePropertyName, String complexTypeName, String type, int code) {
        return createWithToken(AbstractCase.MASTER_TOKEN_NAME, cell, box, col, complexTypePropertyName,
                complexTypeName, type, code);
    }

    /**
     * ComplexTypePropertyを登録する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypePropertyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @param type ComplexTypePropertyの型
     * @param code 期待するコード
     * @return レスポンス
     */
    public static DcResponse createWithToken(
            String token, String cell, String box, String col,
            String complexTypePropertyName, String complexTypeName, String type, int code) {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(UrlUtils.complexTypeProperty(cell, box, col, null, null));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, complexTypePropertyName);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, type);

        // リクエスト実行
        DcResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * ComplexTypePropertyを登録する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcComplexTypePropertyName 更新前ComplexTypeProperty名
     * @param srcComplexTypeName 更新前ComplexType名
     * @param complexTypePropertyName リクエストに指定するComplexTypeProperty名
     * @param complexTypeName リクエストに指定するComplexType名
     * @param type PropertyのType項目
     * @param nullable PropertyのNullable項目
     * @param defaultValue PropertyのDefaultValue項目
     * @param collectionKind PropertyのcollectionKind項目
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static DcResponse create(
            String cell, String box, String collection,
            String srcComplexTypePropertyName, String srcComplexTypeName,
            String complexTypePropertyName, String complexTypeName,
            String type, Boolean nullable, Object defaultValue, String collectionKind) {

        // リクエストボディの組み立て
        JSONObject body = new JSONObject();
        body.put("Name", complexTypePropertyName);
        body.put("_ComplexType.Name", complexTypeName);
        body.put("Type", type);
        body.put("Nullable", nullable);
        body.put("DefaultValue", defaultValue);
        body.put("CollectionKind", collectionKind);

        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(UrlUtils.complexTypeProperty(cell, box, collection, null, null));
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.addStringBody(body.toJSONString());

        // リクエスト実行
        return AbstractCase.request(req);
    }

    /**
     * ComplexTypePropertyを削除する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypePropretyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static DcResponse delete(
            String cell,
            String box,
            String collection,
            String complexTypePropretyName,
            String complexTypeName,
            int code
            ) {
        return deleteWithToken(AbstractCase.MASTER_TOKEN_NAME, cell, box, collection, complexTypePropretyName,
                complexTypeName, code);
    }

    /**
     * ComplexTypePropertyを削除する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypePropretyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static DcResponse deleteWithToken(
            String token,
            String cell,
            String box,
            String collection,
            String complexTypePropretyName,
            String complexTypeName,
            int code
            ) {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.delete(UrlUtils.complexTypeProperty(
                cell, box, collection, complexTypePropretyName, complexTypeName));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        req.header(HttpHeaders.IF_MATCH, "*");

        // リクエスト実行
        DcResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * ComplexTypePropertyを取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypePropertyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @return レスポンス
     */
    public static DcResponse get(String token, String cell, String box,
            String collection, String complexTypePropertyName, String complexTypeName) {
        String locationUrl = UrlUtils.complexTypeProperty(cell, box, collection, complexTypePropertyName,
                complexTypeName);

        // Property取得
        DcRequest req = DcRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        return AbstractCase.request(req);
    }

    /**
     * ComplexTypePropertyを取得する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypePropertyName ComplexTypeProperty名
     * @param complexTypeName ComplexType名
     * @return レスポンス
     */
    public static DcResponse getWithToken(String token, String cell, String box, String collection,
            String complexTypePropertyName, String complexTypeName) {
        String locationUrl = UrlUtils.complexTypeProperty(cell, box, collection, complexTypePropertyName,
                complexTypeName);

        // Property取得
        DcRequest req = DcRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        return AbstractCase.request(req);
    }

    /**
     * ComplexTypePropertyを一覧取得する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            String token,
            String cell,
            String box,
            String col,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexTypeProperty")
                .with("query", "")
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypePropertyを一覧取得する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param query クエリ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            String token,
            String cell,
            String box,
            String col,
            String query,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexTypeProperty")
                .with("query", query)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypePropertyを更新する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcComplexTypePropertyName 更新前ComplexTypeProperty名
     * @param srcComplexTypeName 更新前ComplexType名
     * @param complexTypePropertyName リクエストに指定するComplexTypeProperty名
     * @param complexTypeName リクエストに指定するComplexType名
     * @param type PropertyのType項目
     * @param nullable PropertyのNullable項目
     * @param defaultValue PropertyのDefaultValue項目
     * @param collectionKind PropertyのcollectionKind項目
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static DcResponse update(
            String cell, String box, String collection,
            String srcComplexTypePropertyName, String srcComplexTypeName,
            String complexTypePropertyName, String complexTypeName,
            String type, Boolean nullable, Object defaultValue, String collectionKind) {

        // リクエストボディの組み立て
        JSONObject body = new JSONObject();
        body.put("Name", complexTypePropertyName);
        body.put("_ComplexType.Name", complexTypeName);
        body.put("Type", type);
        body.put("Nullable", nullable);
        body.put("DefaultValue", defaultValue);
        body.put("CollectionKind", collectionKind);

        return update(cell, box, collection, srcComplexTypePropertyName, srcComplexTypeName, body);

    }

    /**
     * ComplexTypePropertyを更新する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcComplexTypePropertyName 更新前ComplexTypeProperty名
     * @param srcComplexTypeName 更新前ComplexType名
     * @param body リクエストボディ
     * @return レスポンス
     */
    public static DcResponse update(
            String cell, String box, String collection,
            String srcComplexTypePropertyName, String srcComplexTypeName,
            JSONObject body) {
        return updateWithToken(AbstractCase.MASTER_TOKEN_NAME, cell, box, collection, srcComplexTypePropertyName,
                srcComplexTypeName, body);
    }

    /**
     * ComplexTypePropertyを更新する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param srcComplexTypePropertyName 更新前ComplexTypeProperty名
     * @param srcComplexTypeName 更新前ComplexType名
     * @param body リクエストボディ
     * @return レスポンス
     */
    public static DcResponse updateWithToken(
            String token, String cell, String box, String collection,
            String srcComplexTypePropertyName, String srcComplexTypeName,
            JSONObject body) {

        // リクエストパラメータ設定
        DcRequest req = DcRequest.put(UrlUtils.complexTypeProperty(cell, box, collection,
                srcComplexTypePropertyName, srcComplexTypeName));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        req.header(HttpHeaders.IF_MATCH, "*");
        req.addStringBody(body.toJSONString());

        // リクエスト実行
        return AbstractCase.request(req);

    }

    /**
     * ComplexTypePropertyをComplexTypeからのNP経由で登録する.
     * @param token トークン
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypeName complexType名
     * @param complexTypePropertyName complexTypePropertyName名
     * @param type ComplexTypePropertyのType
     * @param nullable ComplexTypePropertyのNullable
     * @param defaultValue ComplexTypePropertyのDefaultValue
     * @param collectionKind ComplexTypePropertyのcollectionKind
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createViaComplexTypePropertyNP(
            String token, String cell, String box, String collection,
            String complexTypeName, String complexTypePropertyName,
            String type, Boolean nullable, String defaultValue, String collectionKind, int code) {

        // リクエストボディの組み立て
        JSONObject body = new JSONObject();
        body.put("Name", complexTypePropertyName);
        body.put("_ComplexType.Name", complexTypeName);
        body.put("Type", type);
        body.put("Nullable", nullable);
        body.put("DefaultValue", defaultValue);
        body.put("CollectionKind", collectionKind);

        TResponse res = Http.request("box/odatacol/schema/createViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("entityType", "ComplexType")
                .with("id", complexTypeName)
                .with("navPropName", "_ComplexTypeProperty")
                .with("body", body.toJSONString())
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }

    /**
     * ComplexTypePropertyをComplexTypeからのNP経由で一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param collection コレクション名
     * @param complexTypeName complexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse listViaComplexTypePropertyNP(
            String cell, String box, String collection,
            String complexTypeName, int code) {

        TResponse res = Http.request("box/odatacol/schema/listViaNP.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", collection)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("entityType", "ComplexType")
                .with("id", complexTypeName)
                .with("navPropName", "_ComplexTypeProperty")
                .returns()
                .statusCode(code)
                .debug();
        return res;
    }
}

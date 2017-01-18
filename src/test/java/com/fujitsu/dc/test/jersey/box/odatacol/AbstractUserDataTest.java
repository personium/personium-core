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
package com.fujitsu.dc.test.jersey.box.odatacol;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRestAdapter;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataテスト用の抽象クラス.
 */
public abstract class AbstractUserDataTest extends AbstractCase {

    String cellName = "testcell1";
    String boxName = "box1";
    String colName = "setodata";
    String entityTypeName = "Category";
    String navPropName = null;

    /**
     * コンストラクタ.
     */
    public AbstractUserDataTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * コンストラクタ.
     * @param build WebAppDescriptor
     */
    public AbstractUserDataTest(WebAppDescriptor build) {
        super(build);
    }

    /**
     * ユーザーデータを作成する.
     * @param body リクエストボディ
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse createUserData(JSONObject body, int sc) {
        TResponse response = Http.request("box/odatacol/create.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

    /**
     * ユーザデータの一覧を作成.
     * @param userdataId1 1つめのID
     * @param userdataId2 2つめのID
     */
    @SuppressWarnings("unchecked")
    public void createUserDataList(String userdataId1, String userdataId2) {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userdataId1);
        body.put("dynamicProperty1", "dynamicPropertyValue1");
        body.put("dynamicProperty2", "dynamicPropertyValue2");
        body.put("dynamicProperty3", "dynamicPropertyValue3");

        JSONObject body2 = new JSONObject();
        body2.put("__id", userdataId2);
        body2.put("dynamicProperty1", "dynamicPropertyValueA");
        body2.put("dynamicProperty2", "dynamicPropertyValueB");
        body2.put("dynamicProperty3", "dynamicPropertyValueC");

        // ユーザデータ作成
        createUserData(body, HttpStatus.SC_CREATED);
        createUserData(body2, HttpStatus.SC_CREATED);
    }

    /**
     * ユーザデータの一覧を作成(Etag返却).
     * @param userdataId1 1つめのID
     * @param userdataId2 2つめのID
     * @param etag etag
     */
    @SuppressWarnings("unchecked")
    public void createUserDataList(String userdataId1, String userdataId2, Map<String, String> etag) {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userdataId1);
        body.put("dynamicProperty1", "dynamicPropertyValue1");
        body.put("dynamicProperty2", "dynamicPropertyValue2");
        body.put("dynamicProperty3", "dynamicPropertyValue3");

        JSONObject body2 = new JSONObject();
        body2.put("__id", userdataId2);
        body2.put("dynamicProperty1", "dynamicPropertyValueA");
        body2.put("dynamicProperty2", "dynamicPropertyValueB");
        body2.put("dynamicProperty3", "dynamicPropertyValueC");

        // ユーザデータ作成
        TResponse response = createUserData(body, HttpStatus.SC_CREATED);
        // Etag取得
        etag.put(userdataId1, response.getHeader(HttpHeaders.ETAG));
        // ユーザデータ作成
        response = createUserData(body2, HttpStatus.SC_CREATED);
        // Etag取得
        etag.put(userdataId2, response.getHeader(HttpHeaders.ETAG));
    }

    /**
     * ユーザーデータを作成する.
     * @param body リクエストボディ
     * @param sc 期待するステータスコード
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @return レスポンス
     */
    protected TResponse createUserData(JSONObject body,
            int sc,
            String cell,
            String box,
            String col,
            String entityType) {
        TResponse response = Http.request("box/odatacol/create.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

    /**
     * ユーザーデータを一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @return レスポンス
     */
    protected TResponse getUserDataList(String cell,
            String box,
            String col,
            String entityType) {
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("query", "")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
        return response;
    }

    /**
     * ユーザーデータを一覧取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param query クエリ
     * @return レスポンス
     */
    protected TResponse getUserDataList(String cell,
            String box,
            String col,
            String entityType,
            String query) {
        TResponse response = Http.request("box/odatacol/list.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
        return response;
    }

    /**
     * ユーザーデータを一覧を取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param targetEntityTypeName エンティティ名
     * @param query リクエストクエリ
     * @return ユーザーデータ取得時のレスポンスオブジェクト
     */
    protected static DcResponse getUserDataWithDcClient(String cell,
            String box,
            String col,
            String targetEntityTypeName,
            String query) {
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, "Bearer " + DcCoreConfig.getMasterToken());
        requestheaders.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        // リクエストを実行する
        try {
            res = rest.getAcceptEncodingGzip(
                    UrlUtils.userData(cell, box, col, targetEntityTypeName) + query, requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return res;
    }

    /**
     * ユーザーデータを取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId ユーザデータID
     * @param token 認証トークン
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse getUserData(String cell, String box, String col, String entityType,
            String userDataId, String token, int sc) {
        return getUserData(cell, box, col, entityType, userDataId, token, "", sc);
    }

    /**
     * ユーザーデータを取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId ユーザデータID
     * @param token 認証トークン
     * @param query クエリ
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse getUserData(String cell, String box, String col, String entityType,
            String userDataId, String token, String query, int sc) {
        TResponse response = Http.request("box/odatacol/get.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("id", userDataId)
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("query", query)
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

    /**
     * ユーザーデータを更新する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId ユーザデータID
     * @param body リクエストボディ
     * @return レスポンス
     */
    protected TResponse updateUserData(String cell, String box, String col, String entityType,
            String userDataId, JSONObject body) {
        return Http.request("box/odatacol/update.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("id", userDataId)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", "*")
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT)
                .debug();

    }

    /**
     * ユーザーデータを１件作成する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティ名
     * @param body リクエストボディ
     * @return ユーザーデータ作成時のレスポンスオブジェクト
     */
    protected DcResponse createUserDataWithDcClient(String cell,
            String box,
            String col,
            String entityType,
            JSONObject body) {
        DcRestAdapter rest = new DcRestAdapter();
        DcResponse res = null;

        // リクエストヘッダをセット
        HashMap<String, String> requestheaders = new HashMap<String, String>();
        requestheaders.put(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエストを実行する
        try {
            res = rest.post(UrlUtils.userData(cell, box, col, entityType),
                    body.toJSONString(), requestheaders);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        return res;
    }

    /**
     * ユーザーデータをNP経由で作成する.
     * @param id エンティティID
     * @param body リクエストボディ
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse createUserDataWithNP(String id, JSONObject body, int sc) {
        TResponse response = Http.request("box/odatacol/createNP.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("id", id)
                .with("navPropName", "_" + navPropName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body.toJSONString())
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

    /**
     * ComplexTypeユーザデータをNP経由で作成する.
     * @param id エンティティID
     * @param reqBody リクエストボディ
     * @return レスポンス
     */
    protected DcResponse createComplexTypeUserDataWithNP(String id, HashMap<String, Object> reqBody) {
        // UserData作成
        String requestUrl = UrlUtils.userdataNP(Setup.TEST_CELL1, Setup.TEST_BOX1,
                UserDataListWithNPTest.ODATA_COLLECTION,
                entityTypeName, id, navPropName);
        DcRequest req = DcRequest.post(requestUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        for (String key : reqBody.keySet()) {
            req.addJsonBody(key, reqBody.get(key));
        }
        // 登録
        return request(req);
    }

    /**
     * ユーザーデータを削除する.
     * @param userDataId 削除対象ID
     */
    protected void deleteUserData(String userDataId) {
        // リクエスト実行
        Http.request("box/odatacol/delete.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("id", userDataId)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns()
                .statusCode(-1);
    }

    /**
     * ユーザーデータを削除する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId 削除対象ID
     * @param token 認証トークン
     * @param sc レスポンス
     */
    protected void deleteUserData(String cell, String box, String col, String entityType,
            String userDataId, String token, int sc) {
        // リクエスト実行
        deleteUserData(cell, box, col, entityType, userDataId, token, "*", sc);
    }

    /**
     * ユーザーデータを削除する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataId 削除対象ID
     * @param token 認証トークン
     * @param ifMatch ifMatch
     * @param sc レスポンス
     */
    protected void deleteUserData(String cell, String box, String col, String entityType,
            String userDataId, String token, String ifMatch, int sc) {
        // リクエスト実行
        Http.request("box/odatacol/delete.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("id", userDataId)
                .with("token", token)
                .with("ifMatch", ifMatch)
                .returns()
                .statusCode(sc);
    }

    /**
     * ユーザデータの一覧を削除.
     * @param id1 1つめのID
     * @param id2 2つめのID
     */
    public void deleteUserDataList(String id1, String id2) {
        deleteUserData(id1);
        deleteUserData(id2);
    }

    /**
     * ユーザーデータのリンク情報を削除する.
     * @param userDataId 削除対象ID
     * @param navPropId 削除対象のNavigationPropertyのID
     */
    protected void deleteUserDataLinks(String userDataId, String navPropId) {
        // リクエスト実行
        Http.request("box/odatacol/delete-link.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("entityType", entityTypeName)
                .with("id", userDataId)
                .with("navProp", "_" + navPropName)
                .with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * UserDataの名前空間を取得する.
     * @param entityType エンティティタイプ名
     * @return UserDataの名前空間
     */
    protected String getNameSpace(String entityType) {
        // NameSpace取得のためにメタデータを取得する
        return getNameSpace(entityType, colName);
    }

    /**
     * UserDataの名前空間を取得する.
     * @param entityType エンティティタイプ名
     * @param col コレクション名
     * @return UserDataの名前空間
     */
    protected String getNameSpace(String entityType, String col) {
        // NameSpace取得のためにメタデータを取得する
        TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                .with("path", "\\$metadata")
                .with("col", col)
                .with("accept", "application/xml")
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
        Pattern pattern = Pattern.compile("Namespace=\"([^\"]+)\">");
        Matcher matcher = pattern.matcher(res.getBody());
        matcher.find();
        return matcher.group(1) + "." + entityType;
    }

    /**
     * リンク確認用のデータを取得する.
     * @param linkColName コレクション名
     * @param linkEntityTypeName エンティティタイプ名
     * @param links リンク対象のエンティティタイプ名配列
     * @return リンク確認用のデータ
     */
    protected Map<String, Object> getLinkCheckData(String linkColName,
            String linkEntityTypeName,
            ArrayList<String> links) {
        String baseUrl = UrlUtils.userData(cellName, boxName, linkColName, linkEntityTypeName + "('parent')/_");
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("__id", "parent");
        for (String link : links) {
            Map<String, Object> uri = new HashMap<String, Object>();
            Map<String, Object> deferred = new HashMap<String, Object>();
            uri.put("uri", baseUrl + link);
            deferred.put("__deferred", uri);
            additional.put("_" + link, deferred);
        }
        return additional;
    }

    /**
     * 4階層のComplexTypeスキーマを作成する.
     */
    protected void create4ComplexTypeSchema() {
        UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);
        addComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
        addComplexType("complexType2nd", "ct2ndComplexProp", "complexType3rd", "ct3rdStrProp");
    }

    /**
     * コンプレックスタイプを追加する.
     * @param parentComplex 親ComplexType
     * @param parentComplexProperty 親ComplexTypeに追加するComplexTypeProperty
     * @param addComplex 追加ComplexType
     * @param addComplexProerty 追加ComplexTypeのプロパティ
     */
    protected void addComplexType(String parentComplex,
            String parentComplexProperty,
            String addComplex,
            String addComplexProerty) {
        // ComplexType作成
        UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, addComplex);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, parentComplexProperty, parentComplex,
                addComplex, false, null, null);
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, addComplexProerty, addComplex,
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);
    }

    /**
     * 5階層のComplexTypeスキーマを削除する.
     */
    protected void delete5ComplexTypeSchema() {
        deleteComplexType("complexType3rd", "ct3rdComplexProp", "complexType4th", "ct4thStrProp");
        deleteComplexType("complexType2nd", "ct2ndComplexProp", "complexType3rd", "ct3rdStrProp");
        deleteComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "ct1stComplexProp", "complexType2nd",
                "ct2ndStrProp");
        UserDataComplexTypeUtils.deleteComplexTypeSchema();
    }

    /**
     * コンプレックスタイプを削除する.
     * @param parentComplex 親ComplexType
     * @param parentComplexProperty 親ComplexTypeから削除するComplexTypeProperty
     * @param delComplex 削除ComplexType
     * @param delComplexProerty 削除ComplexTypeのプロパティ
     */
    protected void deleteComplexType(String parentComplex,
            String parentComplexProperty,
            String delComplex,
            String delComplexProerty) {
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                delComplexProerty, delComplex);
        String pctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                parentComplexProperty, parentComplex);
        String ctlocationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                delComplex);

        // 作成したComplexTypePropertyを削除
        ODataCommon.deleteOdataResource(ctplocationUrl);
        ODataCommon.deleteOdataResource(pctplocationUrl);

        // 作成したComplexTypeを削除
        ODataCommon.deleteOdataResource(ctlocationUrl);

    }

    /**
     * 一件取得のレスポンスボディ情報を取得する.
     * @param json レスポンスボディ
     * @return ユーザOData情報
     */
    protected JSONObject getResult(JSONObject json) {
        return (JSONObject) ((JSONObject) json.get("d")).get("results");
    }

    /**
     * 一覧取得のレスポンスボディ情報を取得する.
     * @param userDataId ユーザODataID
     * @param json レスポンスボディ
     * @return 指定されたIDのユーザOData情報
     */
    protected JSONObject getResultsFromId(String userDataId, JSONObject json) {
        JSONArray results = ((JSONArray) ((JSONObject) json.get("d")).get("results"));
        for (Object result : results) {
            JSONObject entity = (JSONObject) result;
            if (userDataId.equals(entity.get("__id").toString())) {
                return entity;
            }
        }
        return null;
    }

}

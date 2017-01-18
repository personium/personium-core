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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserDataのNavigationProperty経由一覧のテスト.
 */
@RunWith(DcRunner.class)
@Ignore
public class AbstractUserDataWithNP extends AbstractUserDataTest {

    /** OdataCollection. **/
    public static final String ODATA_COLLECTION = "nptest";
    /** npEntityA. **/
    public static final String ENTITY_TYPE_A = "npEntityA";
    /** npEntityB. **/
    public static final String ENTITY_TYPE_B = "npEntityB";
    /** npEntityC. **/
    public static final String ENTITY_TYPE_C = "npEntityC";
    /** npEntityD. **/
    public static final String ENTITY_TYPE_D = "npEntityD";
    /** ComplexType用ctNpEntityA. **/
    public static final String CT_ENTITY_TYPE_A = "ctNpEntityA";
    /** ComplexType用ctNpEntityB. **/
    public static final String CT_ENTITY_TYPE_B = "ctNpEntityB";
    /** ComplexType用ctNpEntityC. **/
    public static final String CT_ENTITY_TYPE_C = "ctNpEntityC";
    /** ComplexType用ctNpEntityD. **/
    public static final String CT_ENTITY_TYPE_D = "ctNpEntityD";

    /** npAssocAB_A. **/
    public static final String ASSOC_AB_A = "npAssocAB_A";
    /** npAssocAC_A. **/
    public static final String ASSOC_AC_A = "npAssocAC_A";
    /** npAssocAD_A. **/
    public static final String ASSOC_AD_A = "npAssocAD_A";
    /** npAssocAB_B. **/
    public static final String ASSOC_AB_B = "npAssocAB_B";
    /** npAssocAC_C. **/
    public static final String ASSOC_AC_C = "npAssocAC_C";
    /** npAssocAD_D. **/
    public static final String ASSOC_AD_D = "npAssocAD_D";
    /** npAssocBC_B. **/
    public static final String ASSOC_BC_B = "npAssocBC_B";
    /** npAssocBC_C. **/
    public static final String ASSOC_BC_C = "npAssocBC_C";
    /** npAssocBD_B. **/
    public static final String ASSOC_BD_B = "npAssocBD_B";
    /** npAssocBD_D. **/
    public static final String ASSOC_BD_D = "npAssocBD_D";
    /** npAssocCD_C. **/
    public static final String ASSOC_CD_C = "npAssocCD_C";
    /** npAssocCD_D. **/
    public static final String ASSOC_CD_D = "npAssocCD_D";

    /** 0or1. **/
    public static final String MULTI_ZERO_ONE = "0..1";
    /** 1. **/
    public static final String MULTI_ONE = "1";
    /** *. **/
    public static final String MULTI_AST = "*";

    /**
     * コンストラクタ.
     */
    public AbstractUserDataWithNP() {
        super();
        colName = ODATA_COLLECTION;
    }

    /**
     * ユーザーデータ削除(1:N系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    void deleteUserDataForAST(String parentEntityType, String childEnitityType) {
        ResourceUtils.deleteUserDataLinks("parent", "userdataNP", childEnitityType, Setup.TEST_CELL1, Setup.TEST_BOX1,
                ODATA_COLLECTION, parentEntityType, -1);
        ResourceUtils.deleteUserDataLinks("parent", "userdataNP2", childEnitityType, Setup.TEST_CELL1, Setup.TEST_BOX1,
                ODATA_COLLECTION, parentEntityType, -1);
        deleteUserData(cellName, boxName, colName, childEnitityType, "userdataNP2", DcCoreConfig.getMasterToken(), -1);
        deleteUserDataForONE(parentEntityType, childEnitityType);
    }

    /**
     * ユーザーデータ削除(1:1系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    void deleteUserDataForONE(String parentEntityType, String childEnitityType) {
        deleteUserData(cellName, boxName, colName, childEnitityType, "userdata", DcCoreConfig.getMasterToken(), -1);
        deleteUserData(cellName, boxName, colName, childEnitityType, "userdataNP", DcCoreConfig.getMasterToken(), -1);
        deleteUserData(cellName, boxName, colName, parentEntityType, "parent", DcCoreConfig.getMasterToken(), -1);
    }

    /**
     * ユーザーデータ確認(1:N系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     * @param etag etag
     */
    void checkResponseForAST(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType,
            int count,
            Map<String, String> etag) {
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP')"));
        uri.put("userdataNP2", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP2')"));

        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        Map<String, Object> additionalprop2 = new HashMap<String, Object>();
        additional.put("userdataNP", additionalprop);
        additional.put("userdataNP2", additionalprop2);
        additionalprop.put("dynamicProperty", "dynamicPropertyValueNp");
        additionalprop2.put("dynamicProperty", "dynamicPropertyValueNp2");

        Map<String, Map<String, String>> navigationProp = new HashMap<String, Map<String, String>>();
        Map<String, String> userdataNP = new HashMap<String, String>();
        String baseUrl = UrlUtils.userData(cellName, boxName, colName, childEntityType + "('userdataNP')/_");
        userdataNP.put("_" + ENTITY_TYPE_A, baseUrl + ENTITY_TYPE_A);
        userdataNP.put("_" + ENTITY_TYPE_B, baseUrl + ENTITY_TYPE_B);
        userdataNP.put("_" + ENTITY_TYPE_C, baseUrl + ENTITY_TYPE_C);
        userdataNP.put("_" + ENTITY_TYPE_D, baseUrl + ENTITY_TYPE_D);
        userdataNP.remove("_" + childEntityType);

        Map<String, String> userdataNP2 = new HashMap<String, String>();
        baseUrl = UrlUtils.userData(cellName, boxName, colName, childEntityType + "('userdataNP2')/_");
        userdataNP2.put("_" + ENTITY_TYPE_A, baseUrl + ENTITY_TYPE_A);
        userdataNP2.put("_" + ENTITY_TYPE_B, baseUrl + ENTITY_TYPE_B);
        userdataNP2.put("_" + ENTITY_TYPE_C, baseUrl + ENTITY_TYPE_C);
        userdataNP2.put("_" + ENTITY_TYPE_D, baseUrl + ENTITY_TYPE_D);
        userdataNP2.remove("_" + childEntityType);

        navigationProp.put("userdataNP", userdataNP);
        navigationProp.put("userdataNP2", userdataNP2);

        String nameSpace = getNameSpace(childEntityType);
        ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", count,
                navigationProp, etag);
    }

    /**
     * ユーザーデータ確認(1:N系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     */
    @SuppressWarnings("unchecked")
    void checkComplexTypeResponseForAST(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType,
            int count) {
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP')"));
        uri.put("userdataNP2", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP2')"));

        JSONObject ct1stProp = new JSONObject();
        ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop1 = new HashMap<String, Object>();
        Map<String, Object> additionalprop2 = new HashMap<String, Object>();
        additional.put("userdataNP", additionalprop1);
        additional.put("userdataNP2", additionalprop2);
        additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
        additionalprop2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        additionalprop2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

        Map<String, Map<String, String>> navigationProp = new HashMap<String, Map<String, String>>();
        Map<String, String> userdataNP = new HashMap<String, String>();
        String baseUrl = UrlUtils.userData(cellName, boxName, colName, childEntityType + "('userdataNP')/_");
        userdataNP.put("_" + CT_ENTITY_TYPE_A, baseUrl + CT_ENTITY_TYPE_A);
        userdataNP.put("_" + CT_ENTITY_TYPE_B, baseUrl + CT_ENTITY_TYPE_B);
        userdataNP.put("_" + CT_ENTITY_TYPE_C, baseUrl + CT_ENTITY_TYPE_C);
        userdataNP.put("_" + CT_ENTITY_TYPE_D, baseUrl + CT_ENTITY_TYPE_D);
        userdataNP.remove("_" + childEntityType);

        Map<String, String> userdataNP2 = new HashMap<String, String>();
        baseUrl = UrlUtils.userData(cellName, boxName, colName, childEntityType + "('userdataNP2')/_");
        userdataNP2.put("_" + CT_ENTITY_TYPE_A, baseUrl + CT_ENTITY_TYPE_A);
        userdataNP2.put("_" + CT_ENTITY_TYPE_B, baseUrl + CT_ENTITY_TYPE_B);
        userdataNP2.put("_" + CT_ENTITY_TYPE_C, baseUrl + CT_ENTITY_TYPE_C);
        userdataNP2.put("_" + CT_ENTITY_TYPE_D, baseUrl + CT_ENTITY_TYPE_D);
        userdataNP2.remove("_" + childEntityType);

        navigationProp.put("userdataNP", userdataNP);
        navigationProp.put("userdataNP2", userdataNP2);

        String nameSpace = getNameSpace(childEntityType);
        ODataCommon.checkResponseBodyList(
                response.bodyAsJson(), uri, nameSpace, additional, "__id", count, navigationProp, null);
    }

    /**
     * ユーザーデータ確認(1:N系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     */
    void checkResponseForAST(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType) {
        checkResponseForAST(response, parentEntityType, parentEntityKey, childEntityType, ODataCommon.COUNT_NONE, null);
    }

    /**
     * ユーザーデータ確認(1:1系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     */
    void checkResponseForONE(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType) {
        Map<String, String> navigationProp = new HashMap<String, String>();
        String baseUrl = UrlUtils.userData(cellName, boxName, colName, childEntityType + "('userdataNP')/_");
        navigationProp.put("_" + ENTITY_TYPE_A, baseUrl + ENTITY_TYPE_A);
        navigationProp.put("_" + ENTITY_TYPE_B, baseUrl + ENTITY_TYPE_B);
        navigationProp.put("_" + ENTITY_TYPE_C, baseUrl + ENTITY_TYPE_C);
        navigationProp.put("_" + ENTITY_TYPE_D, baseUrl + ENTITY_TYPE_D);
        navigationProp.remove("_" + childEntityType);

        checkResponseForONE(response, parentEntityType, parentEntityKey, childEntityType, null, navigationProp);
    }

    /**
     * ユーザーデータ確認(1:1系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     * @param etag etag
     */
    void checkResponseForONE(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType,
            Map<String, String> etag,
            Map<String, String> navigationProp) {
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP')"));

        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdataNP", additionalprop);
        additionalprop.put("dynamicProperty", "dynamicPropertyValueNp");

        String nameSpace = getNameSpace(childEntityType);
        ODataCommon.checkResponseBodyList(
                response.bodyAsJson(), uri, nameSpace, additional, "__id", navigationProp, etag);
    }

    /**
     * CompleTypeユーザデータ確認(1:1系用）.
     * @param response レスポンス
     * @param parentEntityType 親EntityType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     */
    @SuppressWarnings("unchecked")
    void checkComplexTypeResponseForONE(TResponse response,
            String parentEntityType,
            String parentEntityKey,
            String childEntityType,
            Map<String, String> navigationProp) {
        Map<String, String> uri = new HashMap<String, String>();
        uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                colName, childEntityType + "('userdataNP')"));

        JSONObject ct1stProp = new JSONObject();
        ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
        Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
        Map<String, Object> additionalprop = new HashMap<String, Object>();
        additional.put("userdataNP", additionalprop);
        additionalprop.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        additionalprop.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

        String nameSpace = getNameSpace(childEntityType);
        ODataCommon.checkResponseBodyList(
                response.bodyAsJson(), uri, nameSpace, additional, "__id", navigationProp, null);
    }

    /**
     * ユーザーデータ作成(1:N系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    void createUserDataForAST(String parentEntityType, String childEnitityType) {
        createUserDataForAST(parentEntityType, childEnitityType, new HashMap<String, String>());
    }

    /**
     * ユーザーデータ作成(1:N系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     * @param etag etag
     */
    @SuppressWarnings("unchecked")
    void createUserDataForAST(String parentEntityType, String childEnitityType, Map<String, String> etag) {
        createUserDataForONE(parentEntityType, childEnitityType, etag);
        entityTypeName = parentEntityType;
        navPropName = childEnitityType;
        JSONObject body = new JSONObject();
        body = new JSONObject();
        body.put("__id", "userdataNP2");
        body.put("dynamicProperty", "dynamicPropertyValueNp2");
        createUserDataWithNP("parent", body, HttpStatus.SC_CREATED);
    }

    /**
     * ComplexTypeユーザデータ作成(1:N系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    @SuppressWarnings("unchecked")
    void createComplexTypeUserDataForAST(String parentEntityType, String childEnitityType) {
        createComplexTypeUserDataForONE(parentEntityType, childEnitityType);
        entityTypeName = parentEntityType;
        navPropName = childEnitityType;

        HashMap<String, Object> reqBody = new HashMap<String, Object>();
        JSONObject ct1stProp = new JSONObject();
        ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
        reqBody.put("__id", "userdataNP2");
        reqBody.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        reqBody.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
        DcResponse resPost = createComplexTypeUserDataWithNP("parent", reqBody);
        assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());
    }

    /**
     * ユーザーデータ作成(1:1系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    void createUserDataForONE(String parentEntityType, String childEnitityType) {
        createUserDataForONE(parentEntityType, childEnitityType, new HashMap<String, String>());
    }

    /**
     * ユーザーデータ作成(1:1系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     * @param etags etags
     */
    @SuppressWarnings("unchecked")
    void createUserDataForONE(String parentEntityType, String childEnitityType, Map<String, String> etags) {
        JSONObject body;
        TResponse respons;

        createUserDataParent(parentEntityType, etags);

        entityTypeName = childEnitityType;
        body = new JSONObject();
        body.put("__id", "userdata");
        body.put("dynamicProperty", "dynamicPropertyValue");
        respons = createUserData(body, HttpStatus.SC_CREATED);
        etags.put("userdata", respons.getHeader(HttpHeaders.ETAG));

        entityTypeName = parentEntityType;
        navPropName = childEnitityType;
        body = new JSONObject();
        body.put("__id", "userdataNP");
        body.put("dynamicProperty", "dynamicPropertyValueNp");
        respons = createUserDataWithNP("parent", body, HttpStatus.SC_CREATED);
        etags.put("userdataNP", respons.getHeader(HttpHeaders.ETAG));
    }

    /**
     * ComplexTypeユーザデータ作成(1:1系用）.
     * @param parentEntityType 親EntityType
     * @param childEnitityType 子EntityType
     */
    @SuppressWarnings("unchecked")
    void createComplexTypeUserDataForONE(String parentEntityType, String childEnitityType) {
        DcResponse resPost = null;
        JSONObject ct1stProp = new JSONObject();
        ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");

        createComplexTypeUserDataParent(parentEntityType);

        entityTypeName = childEnitityType;
        HashMap<String, Object> reqBody1 = new HashMap<String, Object>();
        reqBody1.put("__id", "userdata");
        reqBody1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        reqBody1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
        resPost = createUserDataComplexType(reqBody1);
        assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());

        entityTypeName = parentEntityType;
        navPropName = childEnitityType;
        HashMap<String, Object> reqBody2 = new HashMap<String, Object>();
        reqBody2.put("__id", "userdataNP");
        reqBody2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        reqBody2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
        resPost = createComplexTypeUserDataWithNP("parent", reqBody2);
        assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());
    }

    /**
     * 親データの登録を行う.
     * @param parentEntityType 親EntityType
     */
    void createUserDataParent(String parentEntityType) {
        createUserDataParent(parentEntityType, new HashMap<String, String>());
    }

    /**
     * 親データの登録を行う.
     * @param parentEntityType 親EntityType
     * @param etags etags
     */
    @SuppressWarnings("unchecked")
    void createUserDataParent(String parentEntityType, Map<String, String> etags) {
        entityTypeName = parentEntityType;
        JSONObject body = new JSONObject();
        body.put("__id", "parent");
        body.put("dynamicProperty", "dynamicPropertyPatent");
        TResponse respons = createUserData(body, HttpStatus.SC_CREATED);
        etags.put("parent", respons.getHeader(HttpHeaders.ETAG));
    }

    /**
     * 親データの登録を行う.
     * @param parentEntityType 親EntityType
     */
    @SuppressWarnings("unchecked")
    void createComplexTypeUserDataParent(String parentEntityType) {
        entityTypeName = parentEntityType;
        JSONObject ct1stProp = new JSONObject();
        ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
        HashMap<String, Object> reqBody = new HashMap<String, Object>();
        reqBody.put("__id", "parent");
        reqBody.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
        reqBody.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
        DcResponse resPost = createUserDataComplexType(reqBody);
        assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());
    }

    /**
     * ComplexTypeユーザデータの登録を行う.
     * @param reqBody リクエストボディ
     */
    private DcResponse createUserDataComplexType(HashMap<String, Object> reqBody) {
        // UserData作成
        String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                UserDataListWithNPTest.ODATA_COLLECTION,
                entityTypeName, null);
        DcRequest req = DcRequest.post(requestUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        for (String key : reqBody.keySet()) {
            req.addJsonBody(key, reqBody.get(key));
        }
        // 登録
        return request(req);
    }

    /**
     * NP経由の一覧取得実行.
     * @param parentEntityType 親EnittyType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     * @param query クエリー
     * @return レスポンス
     */
    TResponse execNpListWithQuery(String parentEntityType,
            String parentEntityKey,
            String childEntityType,
            String query) {
        return Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + parentEntityType + "('" + parentEntityKey + "')")
                .with("entityType", "_" + childEntityType)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_OK)
                .debug();
    }

    /**
     * NP経由の一覧取得実行.
     * @param parentEntityType 親EnittyType
     * @param parentEntityKey 親EntityKey
     * @param childEntityType 子EntityType
     * @return レスポンス
     */
    TResponse execNpList(String parentEntityType, String parentEntityKey, String childEntityType) {
        return execNpListWithQuery(parentEntityType, parentEntityKey, childEntityType, "");
    }
}

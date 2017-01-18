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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserDataComplexType一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataComplexTypeListTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataComplexTypeListTest() {
        super();
    }

    /** EntityType名. */
    public static final String ENTITY_TYPE_NAME = "entityType";

    /** ComplexType名. */
    public static final String COMPLEX_TYPE_NAME = "complexType1st";

    /** entityTypeの文字列プロパティ名. */
    public static final String ET_STRING_PROP = "etStrProp";

    /** entityTypeのComplexTypeプロパティ名. */
    public static final String ET_CT1ST_PROP = "etComplexProp";

    /** ユーザデータID1. */
    public static final String USER_DATA_ID1 = "test001";
    /** ユーザデータID2. */
    public static final String USER_DATA_ID2 = "test002";

    /** ユーザデータURI1. */
    public static final String USER_DATA_LOCATION_URL1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
            Setup.TEST_ODATA, ENTITY_TYPE_NAME, USER_DATA_ID1);
    /** ユーザデータURI2. */
    public static final String USER_DATA_LOCATION_URL2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
            Setup.TEST_ODATA, ENTITY_TYPE_NAME, USER_DATA_ID2);

    /**
     * ComplexType2階層のデータ一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType2階層のデータ一覧が取得できること() {

        try {
            HashMap<String, Object> reqBody = new HashMap<String, Object>();

            // ComplexTypeのプロパティ定義登録
            UserDataComplexTypeUtils.createComplexTypeSchema(ENTITY_TYPE_NAME, COMPLEX_TYPE_NAME, ET_STRING_PROP,
                    ET_CT1ST_PROP, "ct1stStrProp");

            // ユーザデータ1登録
            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put("ct1stStrProp", "ct1stStrPropValue1");
            reqBody.put("__id", USER_DATA_ID1);
            reqBody.put(ET_STRING_PROP, "etStrPropValue1");
            reqBody.put(ET_CT1ST_PROP, ct1stProp1);
            DcResponse resPost = createUserDataComplexType(reqBody);
            assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put("ct1stStrProp", "ct1stStrPropValue2");
            reqBody.put("__id", USER_DATA_ID2);
            reqBody.put(ET_STRING_PROP, "etStrPropValue2");
            reqBody.put(ET_CT1ST_PROP, ct1stProp2);
            resPost = createUserDataComplexType(reqBody);
            assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(ET_CT1ST_PROP, ct1stProp1);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put(ET_CT1ST_PROP, ct1stProp2);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexType4階層のデータ一覧が取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType4階層のデータ一覧が取得できること() {

        try {
            // ComplexTypeのプロパティ定義登録
            UserDataComplexTypeUtils.createComplexTypeSchema(ENTITY_TYPE_NAME, COMPLEX_TYPE_NAME, ET_STRING_PROP,
                    ET_CT1ST_PROP, "ct1stStrProp");

            create4ComplexTypeSchema();

            // ユーザデータ1登録
            JSONObject ct3rdProp1 = new JSONObject();
            ct3rdProp1.put("ct3rdStrProp", "CT3RD_STRING_PROP_VALUE1");

            JSONObject ct2ndProp1 = new JSONObject();
            ct2ndProp1.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE1");
            ct2ndProp1.put("ct2ndComplexProp", ct3rdProp1);

            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put("ct1stStrProp", "CT1ST_STRING_PROP_VALUE1");
            ct1stProp1.put("ct1stComplexProp", ct2ndProp1);

            String requestUrl1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    ENTITY_TYPE_NAME, null);
            DcRequest req1 = DcRequest.post(requestUrl1);
            req1.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req1.addJsonBody("__id", USER_DATA_ID1);
            req1.addJsonBody(ET_STRING_PROP, "etStrPropValue1");
            req1.addJsonBody(ET_CT1ST_PROP, ct1stProp1);

            DcResponse resPost1 = request(req1);
            assertEquals(HttpStatus.SC_CREATED, resPost1.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct3rdProp2 = new JSONObject();
            ct3rdProp2.put("ct3rdStrProp", "CT3RD_STRING_PROP_VALUE2");

            JSONObject ct2ndProp2 = new JSONObject();
            ct2ndProp2.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE2");
            ct2ndProp2.put("ct2ndComplexProp", ct3rdProp2);

            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put("ct1stStrProp", "CT1ST_STRING_PROP_VALUE2");
            ct1stProp2.put("ct1stComplexProp", ct2ndProp2);

            String requestUrl2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    ENTITY_TYPE_NAME, null);
            DcRequest req2 = DcRequest.post(requestUrl2);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody("__id", USER_DATA_ID2);
            req2.addJsonBody(ET_STRING_PROP, "etStrPropValue2");
            req2.addJsonBody(ET_CT1ST_PROP, ct1stProp2);

            DcResponse resPost2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, resPost2.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(ET_CT1ST_PROP, ct1stProp1);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put(ET_CT1ST_PROP, ct1stProp2);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            delete5ComplexTypeSchema();
        }
    }

    /**
     * ユーザデータのシンプル型配列データ一覧が正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのシンプル型配列データ一覧が正常に取得できること() {

        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add("xxx");
            etListPropStr.add("yyy");
            etListPropStr.add("zzz");

            JSONArray etListPropInt = new JSONArray();
            etListPropInt.add(1);
            etListPropInt.add(2);
            etListPropInt.add(3);

            JSONArray etListPropSingle = new JSONArray();
            etListPropSingle.add(1.1);
            etListPropSingle.add(2.2);
            etListPropSingle.add(3.3);

            JSONArray etListPropBoolean = new JSONArray();
            etListPropBoolean.add(true);
            etListPropBoolean.add(false);

            // ユーザデータ1登録
            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE1");

            String requestUrl1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req1 = DcRequest.post(requestUrl1);
            req1.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req1.addJsonBody("__id", USER_DATA_ID1);
            req1.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            req1.addJsonBody("etListPropStr", etListPropStr);
            req1.addJsonBody("etListPropInt", etListPropInt);
            req1.addJsonBody("etListPropSingle", etListPropSingle);
            req1.addJsonBody("etListPropBoolean", etListPropBoolean);
            req1.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            // シンプル型配列データの作成
            DcResponse response1 = request(req1);
            assertEquals(HttpStatus.SC_CREATED, response1.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE2");

            String requestUrl2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req2 = DcRequest.post(requestUrl2);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody("__id", USER_DATA_ID2);
            req2.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            req2.addJsonBody("etListPropStr", etListPropStr);
            req2.addJsonBody("etListPropInt", etListPropInt);
            req2.addJsonBody("etListPropSingle", etListPropSingle);
            req2.addJsonBody("etListPropBoolean", etListPropBoolean);
            req2.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            // シンプル型配列データの作成
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put("etListPropStr", etListPropStr);
            additionalprop1.put("etListPropInt", etListPropInt);
            additionalprop1.put("etListPropSingle", etListPropSingle);
            additionalprop1.put("etListPropBoolean", etListPropBoolean);
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put("etListPropStr", etListPropStr);
            additionalprop2.put("etListPropInt", etListPropInt);
            additionalprop2.put("etListPropSingle", etListPropSingle);
            additionalprop2.put("etListPropBoolean", etListPropBoolean);
            additionalprop2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ユーザデータのComplex型配列データ一覧が正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplex型配列データ一覧が正常に取得できること() {

        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx");
            listComplexType2.put("lctStr", "yyy");

            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(listComplexType1);
            etListPropStr.add(listComplexType2);

            // ユーザデータ1登録
            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE1");

            String requestUrl1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req1 = DcRequest.post(requestUrl1);
            req1.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req1.addJsonBody("__id", USER_DATA_ID1);
            req1.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            req1.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);
            req1.addJsonBody("listComplexType", etListPropStr);

            // Complex型配列データの作成
            DcResponse response1 = request(req1);
            assertEquals(HttpStatus.SC_CREATED, response1.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE2");

            String requestUrl2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req2 = DcRequest.post(requestUrl2);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody("__id", USER_DATA_ID2);
            req2.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            req2.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);
            req2.addJsonBody("listComplexType", etListPropStr);

            // Complex型配列データの作成
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);
            additionalprop1.put("listComplexType", etListPropStr);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);
            additionalprop2.put("listComplexType", etListPropStr);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ユーザデータのComplexType内のシンプル型配列データ一覧が正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplexType内のシンプル型配列データ一覧が正常に取得できること() {

        try {
            UserDataComplexTypeUtils.createSimpleArraySchemaInComplex();

            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add("xxx");
            ctListPropStr.add("yyy");
            ctListPropStr.add("zzz");

            JSONArray ctListPropInt = new JSONArray();
            ctListPropInt.add((long) 1);
            ctListPropInt.add((long) 2);
            ctListPropInt.add((long) 3);

            JSONArray ctListPropSingle = new JSONArray();
            ctListPropSingle.add(1.1);
            ctListPropSingle.add(2.2);
            ctListPropSingle.add(3.3);

            JSONArray ctListPropBoolean = new JSONArray();
            ctListPropBoolean.add(true);
            ctListPropBoolean.add(false);

            // ユーザデータ1登録
            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE1");

            ct1stProp1.put("ctListPropStr", ctListPropStr);
            ct1stProp1.put("ctListPropInt", ctListPropInt);
            ct1stProp1.put("ctListPropSingle", ctListPropSingle);
            ct1stProp1.put("ctListPropBoolean", ctListPropBoolean);

            String requestUrl1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req1 = DcRequest.post(requestUrl1);
            req1.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req1.addJsonBody("__id", USER_DATA_ID1);
            req1.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            req1.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            // Complex型配列データの作成
            DcResponse response1 = request(req1);
            assertEquals(HttpStatus.SC_CREATED, response1.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE2");

            ct1stProp2.put("ctListPropStr", ctListPropStr);
            ct1stProp2.put("ctListPropInt", ctListPropInt);
            ct1stProp2.put("ctListPropSingle", ctListPropSingle);
            ct1stProp2.put("ctListPropBoolean", ctListPropBoolean);

            String requestUrl2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req2 = DcRequest.post(requestUrl2);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody("__id", USER_DATA_ID2);
            req2.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            req2.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            // Complex型配列データの作成
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            UserDataComplexTypeUtils.deleteSimpleArraySchemaInComplex();
        }
    }

    /**
     * ユーザデータのComplexType内のComplexType型配列データ一覧が正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplexType内のComplexType型配列データ一覧が正常に取得できること() {

        try {
            UserDataComplexTypeUtils.createComplexArraySchemaInComplex();

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx");
            listComplexType2.put("lctStr", "yyy");
            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add(listComplexType1);
            ctListPropStr.add(listComplexType2);

            // ユーザデータ1登録
            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE1");
            ct1stProp1.put("listComplexType", ctListPropStr);

            String requestUrl1 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req1 = DcRequest.post(requestUrl1);
            req1.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req1.addJsonBody("__id", USER_DATA_ID1);
            req1.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            req1.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            // Complex型配列データの作成
            DcResponse response1 = request(req1);
            assertEquals(HttpStatus.SC_CREATED, response1.getStatusCode());

            // ユーザデータ2登録
            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE2");
            ct1stProp2.put("listComplexType", ctListPropStr);

            String requestUrl2 = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req2 = DcRequest.post(requestUrl2);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody("__id", USER_DATA_ID2);
            req2.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            req2.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            // Complex型配列データの作成
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // ユーザデータの一覧取得
            TResponse res = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", ENTITY_TYPE_NAME)
                    .with("query", "?\\$orderby=__id%20desc")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(USER_DATA_ID1, USER_DATA_LOCATION_URL1);
            uri.put(USER_DATA_ID2, USER_DATA_LOCATION_URL2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(USER_DATA_ID1, additionalprop1);
            additional.put(USER_DATA_ID2, additionalprop2);

            additionalprop1.put("__id", USER_DATA_ID1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp1);

            additionalprop2.put("__id", USER_DATA_ID2);
            additionalprop2.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue2");
            additionalprop2.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp2);

            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL2);
            UserDataComplexTypeUtils.deleteComplexArraySchemaInComplex();
        }
    }

    /**
     * ユーザデータにNull値許可設定の配列を定義してデータが正常に登録できること.
     */
    @Test
    public final void ユーザデータにNull値許可設定の配列を定義してデータが正常に登録できること() {
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, HttpStatus.SC_CREATED);

            // Property作成
            UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "nullableList", UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, "List", false, null);

            // ComplexType作成
            UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);

            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "ctNullableListProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, "List");

            // Property作成
            UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "ctNullableList", UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, true, null, "List", false, null);

            // ユーザデータ登録
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", USER_DATA_ID1);
            req.addJsonBody("nullableList", null);
            req.addJsonBody("ctNullableList", null);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            JSONObject result = ((JSONObject) (((JSONObject) response.bodyAsJson().get("d")).get("results")));
            assertEquals(null, result.get("nullableList"));
            assertEquals(null, result.get("ctNullableList"));

            // ユーザデータ取得
            req = DcRequest.get(USER_DATA_LOCATION_URL1);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            result = ((JSONObject) (((JSONObject) response.bodyAsJson().get("d")).get("results")));
            assertEquals(null, result.get("nullableList"));
            assertEquals(null, result.get("ctNullableList"));

        } finally {
            // 作成したUserDataを削除
            ODataCommon.deleteOdataResource(USER_DATA_LOCATION_URL1);

            // 作成したComplexTypePropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    "ctNullableListProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));

            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ctNullableList", UserDataComplexTypeUtils.ENTITY_TYPE_NAME));

            // 作成したComplexTypeを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));

            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "nullableList", UserDataComplexTypeUtils.ENTITY_TYPE_NAME));

            // 作成したEntityTypeを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, AbstractCase.MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, Setup.TEST_CELL1, -1);
        }
    }

    private DcResponse createUserDataComplexType(HashMap<String, Object> reqBody) {
        // UserData作成
        String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                ENTITY_TYPE_NAME, null);
        DcRequest req = DcRequest.post(requestUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        for (String key : reqBody.keySet()) {
            req.addJsonBody(key, reqBody.get(key));
        }
        // 登録
        return request(req);
    }

    /**
     * 4階層のComplexTypeスキーマを作成する.
     */
    protected void create4ComplexTypeSchema() {
        addComplexType(COMPLEX_TYPE_NAME, "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
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
     * ComplexTypeSchemaを削除する.
     */
    protected void deleteComplexTypeSchema() {
        String ctlocationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                COMPLEX_TYPE_NAME);
        String propStrlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                ET_STRING_PROP, ENTITY_TYPE_NAME);
        String propCtlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                ET_CT1ST_PROP, ENTITY_TYPE_NAME);
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                "ct1stStrProp", COMPLEX_TYPE_NAME);

        // 作成したPropertyを削除
        ODataCommon.deleteOdataResource(propStrlocationUrl);
        ODataCommon.deleteOdataResource(propCtlocationUrl);
        // 作成したComplexTypePropertyを削除
        ODataCommon.deleteOdataResource(ctplocationUrl);
        // 作成したComplexTypeを削除
        ODataCommon.deleteOdataResource(ctlocationUrl);
        // 作成したEntityTypeを削除
        EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, ENTITY_TYPE_NAME, Setup.TEST_CELL1, -1);
    }

    /**
     * 5階層のComplexTypeスキーマを削除する.
     */
    protected void delete5ComplexTypeSchema() {
        // deleteComplexType("complexType4th", "ct4thComplexProp", "complexType5th", "ct5thStrProp");
        deleteComplexType("complexType3rd", "ct3rdComplexProp", "complexType4th", "ct4thStrProp");
        deleteComplexType("complexType2nd", "ct2ndComplexProp", "complexType3rd", "ct3rdStrProp");
        deleteComplexType(COMPLEX_TYPE_NAME, "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
        deleteComplexTypeSchema();
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
}

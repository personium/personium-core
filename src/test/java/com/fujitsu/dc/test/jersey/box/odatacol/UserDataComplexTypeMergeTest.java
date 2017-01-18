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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * UserDataComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataComplexTypeMergeTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataComplexTypeMergeTest() {
        super();
    }

    /**
     * 複数階層あるデータに対して1階層目のデータをMERGEして_正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 複数階層あるデータに対して1階層目のデータをMERGEして_正常に更新できること() {
        String userDataId = "test000";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId);
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject body = new JSONObject();
            body.put(UserDataComplexTypeUtils.ET_STRING_PROP, "mergeTest");
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", userDataId);
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "mergeTest");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(getResponse.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 複数階層あるデータに対して2階層目のデータをMERGEして_正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 複数階層あるデータに対して2階層目のデータをMERGEして_正常に更新できること() {
        String userDataId = "test000";
        String complexTypePropertylocationUrl = null;
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId);
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // ComplexTypeProperty登録(1階層目)
            DcResponse complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct2ndStrProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.String", true, null, "None");
            complexTypePropertylocationUrl = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct2ndStrProp",
                    "CT2ND_STRING_PROP_VALUE");

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject body = new JSONObject();
            JSONObject bodyComplex = new JSONObject();
            bodyComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            body.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, bodyComplex);
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            Map<String, Object> expectedComplex = new HashMap<String, Object>();
            expected.put("__id", userDataId);
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expectedComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            expectedComplex.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, expectedComplex);
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(getResponse.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 複数階層あるデータに対して2階層目のデータ値にnullを指定してMERGEした場合_正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 複数階層あるデータに対して2階層目のデータ値にnullを指定してMERGEした場合_正常に更新できること() {
        String userDataId = "test000";
        String complexTypePropertylocationUrlList = null;
        String complexTypePropertylocationUrl = null;
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId);
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // ComplexTypeProperty登録(2階層目)
            DcResponse complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct2ndStrProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.String", true, null, "None");
            complexTypePropertylocationUrl = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // ComplexTypeProperty登録(2階層目_List)
            complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct3rdIntListProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.Int32", true, null, "List");
            complexTypePropertylocationUrlList = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            JSONArray ct3rdIntListProp = new JSONArray();
            ct3rdIntListProp.add(1);
            ct3rdIntListProp.add(2);
            ct3rdIntListProp.add(3);
            ct3rdIntListProp.add(4);
            ct3rdIntListProp.add(5);
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct2ndStrProp",
                    "CT2ND_STRING_PROP_VALUE");
            ct1stProp.put("ct3rdIntListProp",
                    ct3rdIntListProp);

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject body = new JSONObject();
            JSONObject bodyComplex = new JSONObject();
            bodyComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            bodyComplex.put("ct2ndStrProp", null);
            body.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, bodyComplex);
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            JSONObject resbody = getResponse.bodyAsJson();
            JSONObject complexResults = (JSONObject) ((JSONObject) ((JSONObject) resbody.get("d"))
                    .get("results")).get(UserDataComplexTypeUtils.ET_CT1ST_PROP);
            assertEquals("mergeTest", complexResults.get(UserDataComplexTypeUtils.CT1ST_STRING_PROP));
            assertEquals(null, complexResults.get("ct2ndStrProp"));
            assertEquals(ct3rdIntListProp.toJSONString(),
                    ((JSONArray) complexResults.get("ct3rdIntListProp")).toJSONString());

            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(resbody, userdatalocationUrl, nameSpace, null);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrlList);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 複数階層あるデータに対して2階層目のListデータをMERGEして_正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 複数階層あるデータに対して2階層目のListデータをMERGEして_正常に更新できること() {
        String userDataId = "test000";
        String complexTypePropertylocationUrl = null;
        String complexTypePropertylocationUrlList = null;
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId);
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // ComplexTypeProperty登録(2階層目)
            DcResponse complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct2ndStrProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.String", true, null, "None");
            complexTypePropertylocationUrl = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // ComplexTypeProperty登録(2階層目_List)
            complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct3rdIntListProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.Int32", true, null, "List");
            complexTypePropertylocationUrlList = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            JSONArray ct3rdIntListProp = new JSONArray();
            ct3rdIntListProp.add(1);
            ct3rdIntListProp.add(2);
            ct3rdIntListProp.add(3);
            ct3rdIntListProp.add(4);
            ct3rdIntListProp.add(5);
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct2ndStrProp",
                    "CT2ND_STRING_PROP_VALUE");
            ct1stProp.put("ct3rdIntListProp",
                    ct3rdIntListProp);

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject body = new JSONObject();
            JSONObject bodyComplex = new JSONObject();
            JSONArray ct3rdIntListPropMerge = new JSONArray();
            ct3rdIntListPropMerge.add(6);
            ct3rdIntListPropMerge.add(7);
            ct3rdIntListPropMerge.add(8);
            bodyComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            bodyComplex.put("ct3rdIntListProp", ct3rdIntListPropMerge);
            body.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, bodyComplex);
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            JSONObject resbody = getResponse.bodyAsJson();
            JSONObject complexResults = (JSONObject) ((JSONObject) ((JSONObject) resbody.get("d"))
                    .get("results")).get(UserDataComplexTypeUtils.ET_CT1ST_PROP);
            assertEquals("mergeTest", complexResults.get(UserDataComplexTypeUtils.CT1ST_STRING_PROP));
            assertEquals("CT2ND_STRING_PROP_VALUE", complexResults.get("ct2ndStrProp"));
            assertEquals(ct3rdIntListPropMerge.toJSONString(),
                    ((JSONArray) complexResults.get("ct3rdIntListProp")).toJSONString());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(resbody, userdatalocationUrl, nameSpace, null);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrlList);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 配列型ComplexTypeをもつデータに対してMERGEを実行してデータが差分更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 配列型ComplexTypeをもつデータに対してMERGEを実行してデータが差分更新できること() {
        String userDataId = "test000";
        List<String> locationUrlList = new ArrayList<String>();
        locationUrlList.add(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId));
        try {
            // EntityType作成
            TResponse etRes = EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, HttpStatus.SC_CREATED);

            // ComplexType作成
            DcResponse ctRes = UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);

            // Property作成
            DcResponse p1Res = UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null, false, null);

            DcResponse p2Res = UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, false, null, "List", false, null);

            // complexTypeProperty作成
            DcResponse p3Res = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // 削除用リストにLocationHeaderを追加
            locationUrlList.add(p3Res.getFirstHeader(HttpHeaders.LOCATION));
            locationUrlList.add(p2Res.getFirstHeader(HttpHeaders.LOCATION));
            locationUrlList.add(p1Res.getFirstHeader(HttpHeaders.LOCATION));
            locationUrlList.add(ctRes.getFirstHeader(HttpHeaders.LOCATION));
            locationUrlList.add(etRes.getLocationHeader());

            // リクエストパラメータ設定
            JSONArray ct1stProp = new JSONArray();

            JSONObject ct1stProp1 = new JSONObject();
            ct1stProp1.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP first");
            ct1stProp.add(ct1stProp1);

            JSONObject ct1stProp2 = new JSONObject();
            ct1stProp2.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP second");
            ct1stProp.add(ct1stProp2);

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject ct1stProp3 = new JSONObject();
            ct1stProp3.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP third");
            ct1stProp.add(ct1stProp3);

            JSONObject body = new JSONObject();
            body.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE updated");
            body.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            JSONObject resbody = getResponse.bodyAsJson();
            JSONObject resultsBody = ((JSONObject) ((JSONObject) resbody.get("d")).get("results"));
            assertEquals("UserDataComplexTypeUtils.ET_STRING_PROP_VALUE updated",
                    resultsBody.get(UserDataComplexTypeUtils.ET_STRING_PROP));
            JSONObject complexPropList1 = (JSONObject)
                    ((JSONArray) resultsBody.get(UserDataComplexTypeUtils.ET_CT1ST_PROP)).get(0);
            JSONObject complexPropList2 = (JSONObject)
                    ((JSONArray) resultsBody.get(UserDataComplexTypeUtils.ET_CT1ST_PROP)).get(1);
            JSONObject complexPropList3 = (JSONObject)
                    ((JSONArray) resultsBody.get(UserDataComplexTypeUtils.ET_CT1ST_PROP)).get(2);

            assertEquals("CT1ST_STRING_PROP first", complexPropList1.get(UserDataComplexTypeUtils.CT1ST_STRING_PROP));
            assertEquals("CT1ST_STRING_PROP second", complexPropList2.get(UserDataComplexTypeUtils.CT1ST_STRING_PROP));
            assertEquals("CT1ST_STRING_PROP third", complexPropList3.get(UserDataComplexTypeUtils.CT1ST_STRING_PROP));
        } finally {
            for (String locationUrl : locationUrlList) {
                ODataCommon.deleteOdataResource(locationUrl);
            }
        }
    }

    /**
     * ComplexTypeの値がnullのユーザODataに対してComplexTypeの値を部分更新_正常に更新できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの値がnullのユーザODataに対してComplexTypeの値を部分更新_正常に更新できること() {
        String userDataId = "test000";
        String complexTypePropertylocationUrl = null;
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId);
        try {
            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, HttpStatus.SC_CREATED);
            // ComplexType作成
            UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
            // Property作成
            UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null, false, null);
            UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.ET_CT1ST_PROP,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, true, null, null, false, null);
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);
            // ComplexTypeProperty2のproperty登録(1階層目)
            DcResponse complexTypePropertyResponse = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "ct2ndStrProp", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "Edm.String", true, null, "None");
            complexTypePropertylocationUrl = complexTypePropertyResponse.getFirstHeader(HttpHeaders.LOCATION);

            // ユーザデータ登録リクエスト実行
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", userDataId);
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP,
                    "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータマージリクエスト実行
            JSONObject body = new JSONObject();
            JSONObject bodyComplex = new JSONObject();
            bodyComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            bodyComplex.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");
            body.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, bodyComplex);
            TResponse mergeResponse = mergeRequest(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, userDataId, "*", body);
            assertEquals(HttpStatus.SC_NO_CONTENT, mergeResponse.getStatusCode());

            // ユーザデータ1件取得リクエスト実行
            TResponse getResponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    userDataId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            Map<String, Object> expectedComplex = new HashMap<String, Object>();
            expected.put("__id", userDataId);
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expectedComplex.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "mergeTest");
            expectedComplex.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, expectedComplex);
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(getResponse.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(complexTypePropertylocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 5階層のComplexTypeスキーマを作成する.
     */
    protected void create5ComplexTypeSchema() {
        UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);
        addComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
        addComplexType("complexType2nd", "ct2ndComplexProp", "complexType3rd", "ct3rdStrProp");
        addComplexType("complexType3rd", "ct3rdComplexProp", "complexType4th", "ct4thStrProp");
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
     * 5階層のComplexTypeスキーマを作成する.
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

    private TResponse mergeRequest(String cell, String box, String col, String entity,
            String userDataId, String ifMatch, JSONObject updateReqBody) {
        return Http.request("box/odatacol/merge.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entity)
                .with("id", userDataId)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("ifMatch", ifMatch)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", updateReqBody.toJSONString())
                .returns()
                .debug();
    }
}

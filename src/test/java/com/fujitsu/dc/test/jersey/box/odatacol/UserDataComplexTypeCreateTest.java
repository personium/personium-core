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
import com.fujitsu.dc.core.DcCoreException;
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
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataComplexTypeCreateTest extends AbstractUserDataTest {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public UserDataComplexTypeCreateTest() {
        super(new WebAppDescriptor.Builder(UserDataComplexTypeCreateTest.INIT_PARAMS).build());
    }

    /**
     * ComplexTypeのデータを新規作成して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeのデータを新規作成して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexType4階層のデータを新規作成して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType4階層のデータを新規作成して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            create4ComplexTypeSchema();

            JSONObject ct3rdProp = new JSONObject();
            ct3rdProp.put("ct3rdStrProp", "CT3RD_STRING_PROP_VALUE");

            JSONObject ct2ndProp = new JSONObject();
            ct2ndProp.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");
            ct2ndProp.put("ct2ndComplexProp", ct3rdProp);

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct1stComplexProp", ct2ndProp);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            delete5ComplexTypeSchema();
        }
    }

    /**
     * ユーザデータ登録時にシンプル型の配列指定して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にシンプル型の配列指定して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
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

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropSingle", etListPropSingle);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("etListPropStr", etListPropStr);
            expected.put("etListPropInt", etListPropInt);
            expected.put("etListPropSingle", etListPropSingle);
            expected.put("etListPropBoolean", etListPropBoolean);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ユーザデータ登録時にシンプル型の配列の要素にnullを指定して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にシンプル型の配列の要素にnullを指定して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(null);
            etListPropStr.add("yyy");
            etListPropStr.add("zzz");

            JSONArray etListPropInt = new JSONArray();
            etListPropInt.add(1);
            etListPropInt.add(null);
            etListPropInt.add(3);

            JSONArray etListPropSingle = new JSONArray();
            etListPropSingle.add(1.1);
            etListPropSingle.add(2.2);
            etListPropSingle.add(null);

            JSONArray etListPropBoolean = new JSONArray();
            etListPropBoolean.add(null);
            etListPropBoolean.add(false);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropSingle", etListPropSingle);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("etListPropStr", etListPropStr);
            expected.put("etListPropInt", etListPropInt);
            expected.put("etListPropSingle", etListPropSingle);
            expected.put("etListPropBoolean", etListPropBoolean);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // ユーザデータの取得
            TResponse getresponse = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000",
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);
            // レスポンスヘッダからETAGを取得する
            String etag = getresponse.getHeader(HttpHeaders.ETAG);
            expected.remove("etListPropBoolean");
            JSONArray getListPropBoolean = new JSONArray();
            getListPropBoolean.add(null);
            getListPropBoolean.add(false);
            expected.put("etListPropBoolean", getListPropBoolean);
            ODataCommon.checkResponseBody(getresponse.bodyAsJson(), null, nameSpace, expected, null, etag);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ユーザデータ登録時にComplexTypeの配列指定して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にComplexTypeの配列指定して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx");
            listComplexType2.put("lctStr", "yyy");
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(listComplexType1);
            etListPropStr.add(listComplexType2);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            req.addJsonBody("listComplexType", etListPropStr);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("listComplexType", etListPropStr);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ユーザデータ登録時にComplexType内にシンプル型の配列指定して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にComplexType内にシンプル型の配列指定して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createSimpleArraySchemaInComplex();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
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

            ct1stProp.put("ctListPropStr", ctListPropStr);
            ct1stProp.put("ctListPropInt", ctListPropInt);
            ct1stProp.put("ctListPropSingle", ctListPropSingle);
            ct1stProp.put("ctListPropBoolean", ctListPropBoolean);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchemaInComplex();
        }
    }

    /**
     * ユーザデータ登録時にComplexType内にComplexType型の配列指定して_正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にComplexType内にComplexType型の配列指定して_正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchemaInComplex();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx");
            listComplexType2.put("lctStr", "yyy");
            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add(listComplexType1);
            ctListPropStr.add(listComplexType2);
            ct1stProp.put("listComplexType", ctListPropStr);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexArraySchemaInComplex();
        }
    }

    /**
     * 必須であるComplexType項目を指定しない場合にBadRequestが返却されること.
     */
    @Test
    public final void 必須であるComplexType項目を指定しない場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(
                            UserDataComplexTypeUtils.ET_CT1ST_PROP).getMessage());
        } finally {
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 必須であるComplexType項目にNullを指定した場合にBadRequestが返却されること.
     */
    @Test
    public final void 必須であるComplexType項目にNullを指定した場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(UserDataComplexTypeUtils.ET_CT1ST_PROP)
                            .getMessage());
        } finally {
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの必須項目を指定しない場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの必須項目を指定しない場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredEntry", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("requiredEntry").getMessage());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredEntry", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * 配列の必須項目を指定しない場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 配列の必須項目を指定しない場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add("xxx");
            etListPropStr.add("yyy");
            etListPropStr.add("zzz");

            JSONArray etListPropInt = new JSONArray();
            etListPropInt.add(1);
            etListPropInt.add(2);
            etListPropInt.add(3);

            JSONArray etListPropBoolean = new JSONArray();
            etListPropBoolean.add(true);
            etListPropBoolean.add(false);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("etListPropSingle").getMessage());
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ComplexTypeの配列に必須項目を指定しない場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの配列に必須項目を指定しない場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx_lctStr");
            listComplexType1.put("requiredStringEntry", "xxx_requiredStringEntry");
            listComplexType2.put("lctStr", "yyy_lctStr");
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(listComplexType1);
            etListPropStr.add(listComplexType2);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            req.addJsonBody("listComplexType", etListPropStr);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("requiredStringEntry").getMessage());
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType"));
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ComplexTypeの配列に必須項目にNullを指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの配列に必須項目にNullを指定した場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx_lctStr");
            listComplexType1.put("requiredStringEntry", "xxx_requiredStringEntry");
            listComplexType2.put("lctStr", "yyy_lctStr");
            listComplexType2.put("requiredStringEntry", null);
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(listComplexType1);
            etListPropStr.add(listComplexType2);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            req.addJsonBody("listComplexType", etListPropStr);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("requiredStringEntry").getMessage());
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType"));
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ComplexTypeの配列の必須ではない項目にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの配列の必須ではない項目にデフォルト値が設定されて登録されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "test", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx_lctStr");
            listComplexType1.put("requiredStringEntry", "xxx_requiredStringEntry");
            listComplexType2.put("lctStr", "yyy_lctStr");
            listComplexType2.put("requiredStringEntry", null);
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add(listComplexType1);
            etListPropStr.add(listComplexType2);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            req.addJsonBody("listComplexType", etListPropStr);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            listComplexType2.put("requiredStringEntry", "test");
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("listComplexType", etListPropStr);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType"));
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ComplexTypeの文字列項目を省略した場合にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの文字列項目を省略した場合にデフォルト値が設定されて登録されること() {
        String checkPropName = "stringEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "test", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            ct1stProp.put(checkPropName, "test");
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したComplexTypePropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの真偽値項目を省略した場合にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの真偽値項目を省略した場合にデフォルト値が設定されて登録されること() {
        String checkPropName = "booleanEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), false, "true", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            ct1stProp.put(checkPropName, true);
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの整数項目を省略した場合にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの整数項目を省略した場合にデフォルト値が設定されて登録されること() {
        String checkPropName = "intEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), false, "10", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            ct1stProp.put(checkPropName, (long) 10);
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの小数項目を省略した場合にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの小数項目を省略した場合にデフォルト値が設定されて登録されること() {
        String checkPropName = "singleEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), false, "11.11", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            ct1stProp.put(checkPropName, 11.11);
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの真偽値項目に不正文字列を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの真偽値項目に不正文字列を指定した場合にBadRequestが返却されること() {
        String checkPropName = "booleanEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), false, "true", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put(checkPropName, "test");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(checkPropName).getMessage());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの整数項目に不正文字列を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの整数項目に不正文字列を指定した場合にBadRequestが返却されること() {
        String checkPropName = "intEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName(), false, "10", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put(checkPropName, "2147483648");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(checkPropName).getMessage());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeの小数項目に不正文字列を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeの小数項目に不正文字列を指定した場合にBadRequestが返却されること() {
        String checkPropName = "singleEntry";
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName(), false, "11.11", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put(checkPropName, 11111.111111);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(checkPropName).getMessage());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, checkPropName, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeを含めて最大要素数を超えた場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeを含めて最大要素数を超えた場合にBadRequestが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // 最大要素数+1までデータを追加する。
            for (int i = 0; i <= DcCoreConfig.getMaxPropertyCountInEntityType() - 3; i++) {
                req.addJsonBody(String.valueOf(i), i);
            }

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getCode(),
                    DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED.getMessage());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypeを含めて最大要素数の場合に正常に作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexTypeを含めて最大要素数の場合に正常に作成できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            Map<String, Object> expected = new HashMap<String, Object>();

            // 最大要素数までデータを追加する。
            for (int i = 0; i <= DcCoreConfig.getMaxPropertyCountInEntityType() - 4; i++) {
                req.addJsonBody(String.valueOf(i), String.valueOf(i));
                expected.put(String.valueOf(i), String.valueOf(i));
            }

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", UserDataComplexTypeUtils.COMPLEX_TYPE_NAME));
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * シンプル型の真偽値配列に不正な値を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void シンプル型の真偽値配列に不正な値を指定した場合にBadRequestが返却されること() {
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
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
            etListPropBoolean.add(false);
            etListPropBoolean.add("test");

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropSingle", etListPropSingle);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("etListPropBoolean").getMessage());
        } finally {
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * シンプル型の小数配列に不正な値を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void シンプル型の小数配列に不正な値を指定した場合にBadRequestが返却されること() {
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
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
            etListPropSingle.add(11111.111111);
            etListPropSingle.add(3.3);

            JSONArray etListPropBoolean = new JSONArray();
            etListPropBoolean.add(true);
            etListPropBoolean.add(false);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropSingle", etListPropSingle);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("etListPropSingle").getMessage());
        } finally {
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * シンプル型の整数配列に不正な値を指定した場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void シンプル型の整数配列に不正な値を指定した場合にBadRequestが返却されること() {
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            JSONArray etListPropStr = new JSONArray();
            etListPropStr.add("xxx");
            etListPropStr.add("yyy");
            etListPropStr.add("zzz");

            JSONArray etListPropInt = new JSONArray();
            etListPropInt.add(1);
            etListPropInt.add(2);
            etListPropInt.add("2147483648");

            JSONArray etListPropSingle = new JSONArray();
            etListPropSingle.add(1.1);
            etListPropSingle.add(2.2);
            etListPropSingle.add(3.3);

            JSONArray etListPropBoolean = new JSONArray();
            etListPropBoolean.add(true);
            etListPropBoolean.add(false);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody("etListPropStr", etListPropStr);
            req.addJsonBody("etListPropInt", etListPropInt);
            req.addJsonBody("etListPropSingle", etListPropSingle);
            req.addJsonBody("etListPropBoolean", etListPropBoolean);
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("etListPropInt").getMessage());
        } finally {
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ComplexType内のComplexTypeの項目にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType内のComplexTypeの項目にデフォルト値が設定されて登録されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);
            addComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "complexType2nd",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "test", null);

            JSONObject ct2ndProp = new JSONObject();
            ct2ndProp.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct1stComplexProp", ct2ndProp);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            ct2ndProp.put("requiredStringEntry", "test");
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "complexType2nd"));
            deleteComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "ct1stComplexProp", "complexType2nd",
                    "ct2ndStrProp");
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexType内のComplexTypeの必須項目を指定しない場合にBadrequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType内のComplexTypeの必須項目を指定しない場合にBadrequestが返却されること() {
        try {
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);
            addComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    "ct1stComplexProp", "complexType2nd", "ct2ndStrProp");
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "complexType2nd",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            JSONObject ct2ndProp = new JSONObject();
            ct2ndProp.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct1stComplexProp", ct2ndProp);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("requiredStringEntry").getMessage());
        } finally {
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "complexType2nd"));
            deleteComplexType(UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, "ct1stComplexProp", "complexType2nd",
                    "ct2ndStrProp");
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexType内のシンプル型の配列に必須項目を指定しない場合に_Badrequestになること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType内のシンプル型の配列に必須項目を指定しない場合に_Badrequestになること() {
        try {
            UserDataComplexTypeUtils.createSimpleArraySchemaInComplex();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");
            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add("xxx");
            ctListPropStr.add("yyy");
            ctListPropStr.add("zzz");

            JSONArray ctListPropInt = new JSONArray();
            ctListPropInt.add((long) 1);
            ctListPropInt.add((long) 2);
            ctListPropInt.add((long) 3);

            JSONArray ctListPropBoolean = new JSONArray();
            ctListPropBoolean.add(true);
            ctListPropBoolean.add(false);

            ct1stProp.put("ctListPropStr", ctListPropStr);
            ct1stProp.put("ctListPropInt", ctListPropInt);
            ct1stProp.put("ctListPropBoolean", ctListPropBoolean);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("ctListPropSingle").getMessage());
        } finally {
            UserDataComplexTypeUtils.deleteSimpleArraySchemaInComplex();
        }
    }

    /**
     * ComplexType内のComplexType型配列にデフォルト値が設定されて登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType内のComplexType型配列にデフォルト値が設定されて登録されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchemaInComplex();
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, "test", null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx_lctStr");
            listComplexType1.put("requiredStringEntry", "xxx_requiredStringEntry");
            listComplexType2.put("lctStr", "yyy_lctStr");
            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add(listComplexType1);
            ctListPropStr.add(listComplexType2);
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("listComplexType", ctListPropStr);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            listComplexType2.put("requiredStringEntry", "test");
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType"));
            UserDataComplexTypeUtils.deleteComplexArraySchemaInComplex();
        }
    }

    /**
     * ComplexType内のComplexType型配列に必須項目を指定しない場合にBadRequestが返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType内のComplexType型配列に必須項目を指定しない場合にBadRequestが返却されること() {
        try {
            UserDataComplexTypeUtils.createComplexArraySchemaInComplex();
            // complexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType",
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "CT1ST_STRING_PROP_VALUE");

            JSONObject listComplexType1 = new JSONObject();
            JSONObject listComplexType2 = new JSONObject();
            listComplexType1.put("lctStr", "xxx_lctStr");
            listComplexType1.put("requiredStringEntry", "xxx_requiredStringEntry");
            listComplexType2.put("lctStr", "yyy_lctStr");
            JSONArray ctListPropStr = new JSONArray();
            ctListPropStr.add(listComplexType1);
            ctListPropStr.add(listComplexType2);
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("listComplexType", ctListPropStr);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_STRING_PROP, "ET_STRING_PROP_VALUE");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("requiredStringEntry").getMessage());
        } finally {
            ODataCommon.deleteOdataResource(UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "requiredStringEntry", "ListComplexType"));
            UserDataComplexTypeUtils.deleteComplexArraySchemaInComplex();
        }
    }

    /**
     * ユーザデータ登録時にEntityのpropertyを省略して_正常に作成取得できること.
     */
    @Test
    public final void ユーザデータ登録時にEntityのpropertyを省略して_正常に作成取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            // スキーマデータ作成
            createSchemaData();

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);

            // 作成したスキーマデータを削除
            deleteSchemaData();
        }
    }

    /**
     * ユーザデータ登録時にEntityのpropertyにnullを指定して_正常に作成取得できること.
     */
    @Test
    public final void ユーザデータ登録時にEntityのpropertyにnullを指定して_正常に作成取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            // スキーマデータ作成
            createSchemaData();

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);

            // 作成したスキーマデータを削除
            deleteSchemaData();
        }
    }

    /**
     * ユーザデータ登録時にEntityのpropertyに文字列を指定して_BadRequestが返却されること.
     */
    @Test
    public final void ユーザデータ登録時にEntityのpropertyに文字列を指定して_BadRequestが返却されること() {
        try {
            // スキーマデータ作成
            createSchemaData();

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, "propertyValue");

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());

        } finally {
            // 作成したスキーマデータを削除
            deleteSchemaData();
        }
    }

    /**
     * ユーザデータ登録時にComplexTypePropertyの値にnullを指定して_正常に作成取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータ登録時にComplexTypePropertyの値にnullを指定して_正常に作成取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            // スキーマデータ作成
            createSchemaData();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP, null);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

            // 取得リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);

        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);

            // 作成したスキーマデータを削除
            deleteSchemaData();
        }
    }

    /**
     * 文字列配列型で定義したプロパティに文字列型のデータを登録しようとすると400エラーが返却されること.
     */
    @Test
    public final void 文字列配列型で定義したプロパティに文字列型のデータを登録しようとすると400エラーが返却されること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            // スキーマデータ作成
            EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "listprop", UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, "List", false, null);

            // リクエストパラメータ設定
            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody("listprop", "hoge");

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        } finally {
            // 作成したユーザデータを削除
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            // 作成したPropertyを削除
            String propCtlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "listprop", UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.deleteOdataResource(propCtlocationUrl);
            // 作成したEntityTypeを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, AbstractCase.MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * Nameが大文字小文字のみ異なるComplexTypeが存在する場合に指定データがそれぞれ適切に登録されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Nameが大文字小文字のみ異なるComplexTypeが存在する場合に指定データがそれぞれ適切に登録されること() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userODataComplexTypeTestCell";
        String boxName = "TestBox";
        String colName = "TestCol";
        String entityTypeName = "TestEntityType";
        String nameSpace = "UserData.TestEntityType";

        try {
            // Cell作成
            CellUtils.create(cellName, token, HttpStatus.SC_CREATED);
            // Box作成
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);
            // Collection作成
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);
            // EntityType作成
            EntityTypeUtils.create(cellName,
                    token, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            /*
             * Property:propertySmall → ComplexType:complex → ComplexTypeProperty:compro Property:propertyBig →
             * ComplexType:COMPLEX → ComplexTypeProperty:COMPRO
             */
            // ComplexType作成(小文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "complex");
            // ComplexType作成(大文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "COMPLEX");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "compro", "complex", "Edm.String", true, null, "None");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "COMPRO", "COMPLEX", "Edm.String", true, null, "None");
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertySmall",
                    entityTypeName, "complex", true, null, "None", false, null);
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertyBig",
                    entityTypeName, "COMPLEX", true, null, "None", false, null);

            // 登録データ生成
            JSONObject bodySmall = new JSONObject();
            bodySmall.put("__id", "small");
            JSONObject bodySmallComplex = new JSONObject();
            bodySmallComplex.put("compro", "smallcomplex");
            bodySmall.put("propertySmall", bodySmallComplex);
            // ユーザOData作成
            TResponse res = UserDataUtils.create(token, HttpStatus.SC_CREATED, bodySmall,
                    cellName, boxName, colName, entityTypeName);
            res.bodyAsJson();

            // レスポンスチェック
            ODataCommon.checkResponseBody(res.bodyAsJson(), res.getLocationHeader(), nameSpace, bodySmall);

            // 登録データ生成
            JSONObject bodyBig = new JSONObject();
            bodyBig.put("__id", "big");
            JSONObject bodyBigComplex = new JSONObject();
            bodyBigComplex.put("COMPRO", "bigcomplex");
            bodyBig.put("propertyBig", bodyBigComplex);
            // ユーザOData作成
            res = UserDataUtils.create(token, HttpStatus.SC_CREATED, bodyBig,
                    cellName, boxName, colName, entityTypeName);
            // レスポンスチェック
            ODataCommon.checkResponseBody(res.bodyAsJson(), res.getLocationHeader(), nameSpace, bodyBig);
        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * ユーザOData登録でNameが大文字小文字のみ異なるComplexTypeが存在する場合にスキーマ定義にないComplexTypePropertyが登録できないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザOData登録でNameが大文字小文字のみ異なるComplexTypeが存在する場合にスキーマ定義にないComplexTypePropertyが登録できないこと() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String cellName = "userODataComplexTypeTestCell";
        String boxName = "TestBox";
        String colName = "TestCol";
        String entityTypeName = "TestEntityType";

        try {
            // Cell作成
            CellUtils.create(cellName, token, HttpStatus.SC_CREATED);
            // Box作成
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);
            // Collection作成
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);
            // EntityType作成
            EntityTypeUtils.create(cellName,
                    token, boxName, colName, entityTypeName, HttpStatus.SC_CREATED);

            /*
             * Property:propertySmall → ComplexType:complex → ComplexTypeProperty:compro Property:propertyBig →
             * ComplexType:COMPLEX → ComplexTypeProperty:COMPRO
             */
            // ComplexType作成(小文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "complex");
            // ComplexType作成(大文字)
            UserDataUtils.createComplexType(cellName, boxName, colName, "COMPLEX");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "compro", "complex", "Edm.String", true, null, "None");
            // ComplexTypeProperty作成
            UserDataUtils.createComplexTypeProperty(cellName, boxName, colName,
                    "COMPRO", "COMPLEX", "Edm.String", true, null, "None");
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertySmall",
                    entityTypeName, "complex", true, null, "None", false, null);
            // Property作成
            UserDataUtils.createProperty(cellName, boxName, colName, "propertyBig",
                    entityTypeName, "COMPLEX", true, null, "None", false, null);

            // 登録データ生成
            JSONObject bodySmall = new JSONObject();
            bodySmall.put("__id", "big");
            JSONObject bodySmallComplex = new JSONObject();
            bodySmallComplex.put("compro", "smallcomplex");
            bodySmall.put("propertyBig", bodySmallComplex);
            // ユーザOData作成
            UserDataUtils.create(token, HttpStatus.SC_BAD_REQUEST, bodySmall,
                    cellName, boxName, colName, entityTypeName);
        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * ユーザデータ登録用のスキーマデータを作成する.
     */
    private void createSchemaData() {
        // EntityType作成
        EntityTypeUtils.create(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, HttpStatus.SC_CREATED);

        // ComplexType作成
        UserDataUtils.createComplexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);

        // Property作成（Nullable=trueで作成）
        UserDataUtils.createProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, true, null, null, false, null);

        // complexTypeProperty作成
        UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.CT1ST_STRING_PROP, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                EdmSimpleType.STRING.getFullyQualifiedTypeName(), true, null, null);
    }

    /**
     * ユーザデータ登録用のスキーマデータを削除する.
     */
    private void deleteSchemaData() {
        // 作成したPropertyを削除
        String propCtlocationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
        ODataCommon.deleteOdataResource(propCtlocationUrl);

        // 作成したComplexTypePropertyを削除
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.CT1ST_STRING_PROP, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
        ODataCommon.deleteOdataResource(ctplocationUrl);

        // 作成したComplexTypeを削除
        String ctlocationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
        ODataCommon.deleteOdataResource(ctlocationUrl);

        // 作成したEntityTypeを削除
        EntityTypeUtils.delete(Setup.TEST_ODATA, AbstractCase.MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, UserDataComplexTypeUtils.ENTITY_TYPE_NAME, Setup.TEST_CELL1, -1);
    }

}

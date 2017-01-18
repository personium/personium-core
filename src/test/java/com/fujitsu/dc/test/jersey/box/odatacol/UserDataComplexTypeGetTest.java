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

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserDataComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataComplexTypeGetTest extends AbstractUserDataTest {

    /**
     * コンストラクタ.
     */
    public UserDataComplexTypeGetTest() {
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

    /**
     * ComplexType2階層のデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType2階層のデータを取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                ENTITY_TYPE_NAME, "test000");
        try {
            // ユーザデータ登録
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put("ct1stStrProp", "ct1stStrPropValue");
            HashMap<String, Object> reqBody = new HashMap<String, Object>();
            reqBody.put("__id", "test000");
            reqBody.put(ET_STRING_PROP, "etStrPropValue");
            reqBody.put(ET_CT1ST_PROP, ct1stProp);
            DcResponse resPost = createUserDataComplexType(reqBody);
            assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());

            // ユーザデータ取得
            DcRequest req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(ET_STRING_PROP, "etStrPropValue");
            expected.put(ET_CT1ST_PROP, ct1stProp);
            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(resGet.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexType4階層のデータを取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ComplexType4階層のデータを取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                ENTITY_TYPE_NAME, "test000");
        try {
            // ユーザデータ登録
            create4ComplexTypeSchema();
            JSONObject ct3rdProp = new JSONObject();
            ct3rdProp.put("ct3rdStrProp", "CT3RD_STRING_PROP_VALUE");

            JSONObject ct2ndProp = new JSONObject();
            ct2ndProp.put("ct2ndStrProp", "CT2ND_STRING_PROP_VALUE");
            ct2ndProp.put("ct2ndComplexProp", ct3rdProp);

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put("ct1stStrProp", "CT1ST_STRING_PROP_VALUE");
            ct1stProp.put("ct1stComplexProp", ct2ndProp);

            String requestUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    ENTITY_TYPE_NAME, null);
            DcRequest req = DcRequest.post(requestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("__id", "test000");
            req.addJsonBody(ET_STRING_PROP, "etStrPropValue");
            req.addJsonBody(ET_CT1ST_PROP, ct1stProp);

            DcResponse resPost = request(req);
            assertEquals(HttpStatus.SC_CREATED, resPost.getStatusCode());

            // ユーザデータ取得
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(ET_STRING_PROP, "etStrPropValue");
            expected.put(ET_CT1ST_PROP, ct1stProp);
            String nameSpace = getNameSpace(ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(resGet.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            delete5ComplexTypeSchema();
        }
    }

    /**
     * ユーザデータのシンプル型配列データが正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのシンプル型配列データが正常に取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createSimpleArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "UserDataComplexTypeUtils.CT1ST_STRING_PROP_VALUE");
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

            // シンプル型配列データの作成
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("etListPropStr", etListPropStr);
            expected.put("etListPropInt", etListPropInt);
            expected.put("etListPropSingle", etListPropSingle);
            expected.put("etListPropBoolean", etListPropBoolean);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchema();
        }
    }

    /**
     * ユーザデータのComplex型配列データが正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplex型配列データが正常に取得できること() {
        String userdatalocationUrl = UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                UserDataComplexTypeUtils.ENTITY_TYPE_NAME, "test000");
        try {
            UserDataComplexTypeUtils.createComplexArraySchema();

            // リクエストパラメータ設定
            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put(UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    "UserDataComplexTypeUtils.CT1ST_STRING_PROP_VALUE");

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

            // Complex型配列データの作成
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put("listComplexType", etListPropStr);
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexArraySchema();
        }
    }

    /**
     * ユーザデータのComplexType内のシンプル型配列データが正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplexType内のシンプル型配列データが正常に取得できること() {
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

            // Complex型配列データの作成
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteSimpleArraySchemaInComplex();
        }
    }

    /**
     * ユーザデータのComplexType内のComplexType型配列データが正常に取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータのComplexType内のComplexType型配列データが正常に取得できること() {
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

            // Complex型配列データの作成
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエスト実行
            req = DcRequest.get(userdatalocationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("__id", "test000");
            expected.put(UserDataComplexTypeUtils.ET_STRING_PROP, "UserDataComplexTypeUtils.ET_STRING_PROP_VALUE");
            expected.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            String nameSpace = getNameSpace(UserDataComplexTypeUtils.ENTITY_TYPE_NAME);
            ODataCommon.checkResponseBody(response.bodyAsJson(), userdatalocationUrl, nameSpace, expected);
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            UserDataComplexTypeUtils.deleteComplexArraySchemaInComplex();
        }
    }

    private DcResponse createUserDataComplexType(HashMap<String, Object> reqBody) {

        // ComplexTypeのプロパティ定義登録
        UserDataComplexTypeUtils.createComplexTypeSchema(ENTITY_TYPE_NAME, COMPLEX_TYPE_NAME, ET_STRING_PROP,
                ET_CT1ST_PROP, "ct1stStrProp");

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
        UserDataComplexTypeUtils.createComplexTypeSchema(ENTITY_TYPE_NAME, COMPLEX_TYPE_NAME, ET_STRING_PROP,
                ET_CT1ST_PROP, "ct1stStrProp");

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
     * 5階層のComplexTypeスキーマを作成する.
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

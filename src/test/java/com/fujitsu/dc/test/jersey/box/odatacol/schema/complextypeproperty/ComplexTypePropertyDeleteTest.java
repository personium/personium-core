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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.AbstractUserDataTest;
import com.fujitsu.dc.test.jersey.box.odatacol.UserDataComplexTypeUtils;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * ComplexTypeProperty更新のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyDeleteTest extends AbstractUserDataTest {

    /** ComplexTypeProperty名. */
    private static final String CT_PROPERTY_NAME = "ctp_name";

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "address";

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyDeleteTest() {
        super();
    }

    /**
     * ユーザデータが存在する状態でPropertyと紐付く最大階層のComplexTypePropertyを削除した場合409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータが存在する状態でPropertyと紐付く最大階層のComplexTypePropertyを削除した場合409が返却されること() {
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
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexTypeProperty削除
            // リクエストパラメータ設定
            req = DcRequest.delete(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "ct3rdStrProp", "complexType3rd"));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            response = request(req);

            // レスポンスの確認
            assertEquals(HttpStatus.SC_CONFLICT, response.getStatusCode());
        } finally {
            ODataCommon.deleteOdataResource(userdatalocationUrl);
            delete5ComplexTypeSchema();
        }
    }

    /**
     * ユーザデータが存在する状態でPropertyと紐付くComplexTypePropertyを削除した場合409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータが存在する状態でPropertyと紐付くComplexTypePropertyを削除した場合409が返却されること() {
        String propName = "testprop";
        String propertyRequestUrl = null;
        String userDataId = "compro_test";
        String entityTypeName = "Price";
        DcResponse response = null;

        try {
            // ComplexType登録
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(UrlUtils.complexType(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexTypeProperty登録
            req = DcRequest.post(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // Property登録(Type:adress)
            // リクエストパラメータ設定
            req = DcRequest.post(PropertyUtils.REQUEST_URL);

            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, COMPLEX_TYPE_NAME);

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ユーザデータ登録
            JSONObject body = new JSONObject();
            body.put("__id", "compro_test");

            // リクエスト実行
            createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, body);

            // ComplexTypeProperty削除
            // リクエストパラメータ設定
            req = DcRequest.delete(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, CT_PROPERTY_NAME, COMPLEX_TYPE_NAME));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            response = request(req);

            // レスポンスの確認
            assertEquals(HttpStatus.SC_CONFLICT, response.getStatusCode());

        } finally {
            // ユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, MASTER_TOKEN_NAME, -1);

            // ComplexTypeProperty削除
            propertyRequestUrl =
                    UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, CT_PROPERTY_NAME,
                            COMPLEX_TYPE_NAME);
            response = ODataCommon.deleteOdataResource(propertyRequestUrl);

            // Property削除
            propertyRequestUrl =
                    UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                            entityTypeName);
            response = ODataCommon.deleteOdataResource(propertyRequestUrl);

            // ComplexType削除
            propertyRequestUrl =
                    UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);
            response = ODataCommon.deleteOdataResource(propertyRequestUrl);
        }
    }

    /**
     * Propertyと紐付かないComplexTypePropertyを削除できること.
     */
    @Test
    public final void Propertyと紐付かないComplexTypePropertyを削除できること() {
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);

        try {
            // ComplexType登録
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(UrlUtils.complexType(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexTypeProperty登録
            req = DcRequest.post(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexTypeProperty削除
            // リクエストパラメータ設定
            req = DcRequest.delete(UrlUtils.complexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, CT_PROPERTY_NAME, COMPLEX_TYPE_NAME));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.header(HttpHeaders.IF_MATCH, "*");

            // リクエスト実行
            response = request(req);

            // レスポンスの確認
            assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, ODataCommon.deleteOdataResource(locationUrl).getStatusCode());
        }
    }
}

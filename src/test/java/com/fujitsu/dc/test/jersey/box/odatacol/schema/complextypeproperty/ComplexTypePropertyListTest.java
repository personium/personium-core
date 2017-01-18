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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DaoException;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * ComplexTypeProperty一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyListTest extends ODataCommon {

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /** ComplexTypeProperty名1. */
    private static final String CT_PROPERTY_NAME = "ctp_name";

    /** ComplexTypeProperty名2. */
    private static final String CT_PROPERTY_NAME2 = "ctp_name2";

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "address";

    /** ComplexTypeリソースURL. */
    private static final String CT_LOCATION_URL = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1,
            Setup.TEST_ODATA, COMPLEX_TYPE_NAME);

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        // ComplexType作成
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        // リクエスト実行
        request(req);
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        // 作成したComplexTypeを削除
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(CT_LOCATION_URL).getStatusCode());
    }

    /**
     * ComplexTypePropertyを作成して_一覧が正常に取得できること.
     */
    @Test
    public final void ComplexTypePropertyを作成して_一覧が正常に取得できること() {
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                CT_PROPERTY_NAME, COMPLEX_TYPE_NAME);
        String ctplocationUrl2 = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                CT_PROPERTY_NAME2, COMPLEX_TYPE_NAME);
        String ctplocationUrlGet = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                null, null);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 作成時のetag
            String etag = getEtag(response);

            // リクエストパラメータ設定
            DcRequest req2 = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME2);
            req2.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req2.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // 作成時のetag
            String etag2 = getEtag(response2);

            // ComplexTypeProperty取得
            req = DcRequest.get(ctplocationUrlGet);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());

            Map<String, String> urlMap = new HashMap<String, String>();
            urlMap.put(CT_PROPERTY_NAME, ctplocationUrl);
            urlMap.put(CT_PROPERTY_NAME2, ctplocationUrl2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(CT_PROPERTY_NAME, additionalprop);
            additional.put(CT_PROPERTY_NAME2, additionalprop2);

            additionalprop.put("Name", CT_PROPERTY_NAME);
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            additionalprop.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);

            additionalprop2.put("Name", CT_PROPERTY_NAME2);
            additionalprop2.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME2);
            additionalprop2.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            additionalprop2.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            additionalprop2.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            additionalprop2.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            additionalprop2
                    .put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);

            Map<String, String> etagMap = new HashMap<String, String>();
            etagMap.put(CT_PROPERTY_NAME, etag);
            etagMap.put(CT_PROPERTY_NAME2, etag2);

            checkResponseBodyList(resGet.bodyAsJson(), urlMap, ComplexTypePropertyUtils.NAMESPACE, additional, "Name",
                    COUNT_NONE, etagMap);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl2).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyが存在しないとき_一覧取得で0件になること.
     */
    @Test
    public final void ComplexTypePropertyが存在しないとき_一覧取得で0件になること() {
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null,
                null);

        // ComplexTypeProperty取得
        DcRequest req = DcRequest.get(ctplocationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse resGet = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
        checkResponseBodyList(resGet.bodyAsJson(), null, ComplexTypePropertyUtils.NAMESPACE, null);
    }

    /**
     * DefaultValueに制御コードを含むComplexTypePropertyを作成した場合_レスポンスボディがエスケープされて一覧取得できること.
     * @throws DaoException レスポンスボディのパースに失敗
     */
    @Test
    public final void DefaultValueに制御コードを含むComplexTypePropertyを作成した場合_レスポンスボディがエスケープされて一覧取得できること() throws DaoException {
        String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                CT_PROPERTY_NAME, COMPLEX_TYPE_NAME);
        String ctplocationUrlGet = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                null, null);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, false);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "\u0000");
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexTypeProperty取得
            req = DcRequest.get(ctplocationUrlGet);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
            String resBody = response.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
        }
    }
}

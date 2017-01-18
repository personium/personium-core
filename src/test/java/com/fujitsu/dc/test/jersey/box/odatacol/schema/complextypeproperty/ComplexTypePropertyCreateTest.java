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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreException;
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
import com.fujitsu.dc.test.jersey.box.odatacol.UserDataComplexTypeUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * ComplexTypeProperty登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyCreateTest extends ODataCommon {

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /** ComplexTypeProperty名. */
    private static final String CT_PROPERTY_NAME = "ctp_name";

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "address";

    /** ComplexTypeリソースURL. */
    private static final String CT_LOCATION_URL =
            UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyCreateTest() {
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
     * ComplexTypePropertyを新規作成して_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyを新規作成して_正常に作成できること() {
        String ctplocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);

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

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), ctplocationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
        }
    }

    /**
     * 既にデータが存在するComplexTypeに対してComplexTypePropertyのNullableをFalseで作成した場合_BadRequestが返却されること.
     */
    @Test
    public final void 既にデータが存在するComplexTypeに対してComplexTypePropertyのNullableをFalseで作成した場合_BadRequestが返却されること() {
        // TODO ComplexTypeユーザデータ作成処理の実装後に本テストを追加すること
    }

    /**
     * ComplexTypePropertyのName属性がない場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのName属性がない場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING
                        .params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのNameが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNameが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, "");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのNameが1文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのNameが1文字の場合_正常に作成できること() {
        String ctPropertyName = "a";
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, ctPropertyName,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNameが128文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのNameが128文字の場合_正常に作成できること() {
        String ctPropertyName = STRING_LENGTH_128;
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, ctPropertyName,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNameが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNameが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, STRING_LENGTH_129);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのNameが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのNameが利用可能な文字種の場合_正常に作成できること() {
        String ctPropertyName = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, ctPropertyName,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, ctPropertyName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNameが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNameが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, "ctp.*name");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのNameが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNameが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, "-ctp_name");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, "_ctp_name");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY)
                        .getMessage());
    }

    /**
     * 既に同一名のComplexTypePropertyが作成済みの場合_Conflictが返却されること.
     */
    @Test
    public final void 既に同一名のComplexTypePropertyが作成済みの場合_Conflictが返却されること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
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

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getCode(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getMessage());

        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyの_ComplexTypeName属性がない場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeName属性がない場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, "");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが1文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが1文字の場合_正常に作成できること() {
        String complexTypeName = "a";
        String ctypelocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        String locationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, CT_PROPERTY_NAME, complexTypeName);
        try {
            // ComplexType作成
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctypelocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが128文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが128文字の場合_正常に作成できること() {
        String complexTypeName = STRING_LENGTH_128;
        String ctypelocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        String locationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, CT_PROPERTY_NAME, complexTypeName);
        try {
            // ComplexType作成
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctypelocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, STRING_LENGTH_129);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが利用可能な文字種の場合_正常に作成できること() {
        String complexTypeName = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String ctypelocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        String locationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, CT_PROPERTY_NAME, complexTypeName);

        try {
            // ComplexType作成
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctypelocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, "Ad.*s");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, "-address");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, "_address");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyの_ComplexTypeNameが存在しないComplexTypeの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyの_ComplexTypeNameが存在しないComplexTypeの場合_BadRequestが返却されること() {
        String complexTypeName = "xxx";

        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(complexTypeName).getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmBooleanの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmBooleanの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmSingleの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmSingleの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmInt32の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmInt32の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmDateTimeの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmDateTimeの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);

        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, "Edm.Datetime");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのTypeが登録済みのComplexTypeの場合_正常に作成されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeが登録済みのComplexTypeの場合_正常に作成されること() {
        String complexTypeName = "ctPropTest";
        String complexLocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        String ctpLocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("Name", complexTypeName);
            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, complexTypeName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), ctpLocationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctpLocationUrl).getStatusCode());
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(complexLocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNullableがtrueの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのNullableがtrueの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNullableがfalseの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのNullableがfalseの場合_正常に作成できること() {
        String complexTypeName = "xxx";
        String ctypelocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        String locationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, CT_PROPERTY_NAME, complexTypeName);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, false);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, complexTypeName);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, false);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctypelocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのNullableが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのNullableが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, "test");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY)
                        .getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmStringでDefaultValueが空文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmStringでDefaultValueが空文字の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "");
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最小値を下回る場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最小値を下回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.INT32.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "-2147483649");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最小値の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最小値の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, -2147483648);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "-2147483648");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最大値の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最大値の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, 2147483647);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "2147483647");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最大値を上回る場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmInt32でDefaultValueが最大値を上回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.INT32.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "2147483648");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmSingleでDefaultValueが整数5桁_小数5桁の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmSingleでDefaultValueが整数5桁_小数5桁の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, 11111.11111);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "11111.11111");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmSingleでDefaultValueが整数が6桁の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmSingleでDefaultValueが整数が6桁の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, 111111.11111);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmSingleでDefaultValueが小数が6桁の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmSingleでDefaultValueが小数が6桁の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, 11111.111111);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyのTypeがEdmBooleanでDefaultValueがtrueの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmBooleanでDefaultValueがtrueの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, true);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "true");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmBooleanでDefaultValueがfalseの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmBooleanでDefaultValueがfalseの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, false);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "false");
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのTypeがEdmBooleanでDefaultValueが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのTypeがEdmBooleanでDefaultValueが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, "test");
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyのCollectionKindがNoneの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのCollectionKindがNoneの場合_正常に作成できること() {
        String locationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, CT_PROPERTY_NAME, COMPLEX_TYPE_NAME);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            deleteOdataResource(locationUrl).getStatusCode();
        }
    }

    /**
     * ComplexTypePropertyのCollectionKindがListの場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのCollectionKindがListの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, "List");

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, "List");
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのCollectionKindが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyのCollectionKindが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, "test");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(
                response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY).getMessage());
    }

    /**
     * ComplexTypePropertyを文字列型で作成後に真偽値型で再作成した場合にBadRequestが返却されること.
     */
    @Test
    public final void ComplexTypePropertyを文字列型で作成後に真偽値型で再作成した場合にBadRequestが返却されること() {
        try {
            // 1階層のComplexTypeSchemaを作成する
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // 文字列型で作成したComplexTypePropertyを削除する
            String ctplocationUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    UserDataComplexTypeUtils.CT1ST_STRING_PROP, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
            ODataCommon.deleteOdataResource(ctplocationUrl);

            // complexTypePropertyをBoolean型で再作成する
            DcResponse response = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName(), false, null, null);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        } finally {
            // 作成したComplexTypeSchemaを削除する
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * Cellをまたがって同じ名前のComplexTypePropertyを追加した場合に正常に作成できること.
     */
    @Test
    public final void Cellをまたがって同じ名前のComplexTypePropertyを追加した場合に正常に作成できること() {
        // 作成するComplexType/ComplexTypePropertyのリソースURL
        String ctlocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL2, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
        String ctplocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL2, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                        UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);
        try {
            // 1階層のComplexTypeSchemaを作成する
            UserDataComplexTypeUtils.createComplexTypeSchema(UserDataComplexTypeUtils.ENTITY_TYPE_NAME,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME, UserDataComplexTypeUtils.ET_STRING_PROP,
                    UserDataComplexTypeUtils.ET_CT1ST_PROP, UserDataComplexTypeUtils.CT1ST_STRING_PROP);

            // Cell2にComplexTypeを作成する
            UserDataUtils.createComplexType(Setup.TEST_CELL2, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.COMPLEX_TYPE_NAME);

            // Cellのみ異なる同じ名前のComplexTypePropertyを作成する
            DcResponse response = UserDataUtils.createComplexTypeProperty(Setup.TEST_CELL2, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, UserDataComplexTypeUtils.CT1ST_STRING_PROP,
                    UserDataComplexTypeUtils.COMPLEX_TYPE_NAME,
                    EdmSimpleType.STRING.getFullyQualifiedTypeName(), false, null, null);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        } finally {
            // 作成したデータを削除
            deleteOdataResource(ctplocationUrl).getStatusCode();
            deleteOdataResource(ctlocationUrl).getStatusCode();
            UserDataComplexTypeUtils.deleteComplexTypeSchema();
        }
    }

    /**
     * ComplexTypePropertyを階層最大値まで新規作成して_正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyを階層最大値まで新規作成して_正常に作成できること() {
        final int repeatCount = 20;

        // 結びつけるComplexType作成
        // リクエストパラメータ設定
        DcRequest req0 = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
        req0.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req0.addJsonBody(COMPLEX_TYPE_NAME_KEY, "testComplexType");
        // リクエスト実行
        request(req0);

        try {
            DcResponse response = null;
            // リクエストパラメータ設定
            for (int i = 0; i < repeatCount; i++) {
                DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
                req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, "testComplexType");

                // リクエスト実行
                response = request(req);
                // レスポンスチェック
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
                try {
                    assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
                } catch (Error e) {
                    fail(">>> loop count : " + i);
                }
            }
        } finally {
            // 作成したComplexTypePropertyを削除
            for (int i = 0; i < repeatCount; i++) {
                try {
                    String ctplocationUrl =
                            UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                                    CT_PROPERTY_NAME + i,
                                    COMPLEX_TYPE_NAME);
                    assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
                } catch (Exception e) {
                    System.out.println(">>> " + i);
                }

            }

            // 作成したComplexTypeを削除
            String complexTypeUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "testComplexType");
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(complexTypeUrl).getStatusCode());

        }
    }

    /**
     * ComplexTypePropertyを階層最大値を超えるまで新規作成して_異常を検知できること.
     */
    @Test
    public final void ComplexTypePropertyを階層最大値を超えるまで新規作成して_異常を検知できること() {
        final int repeatCount = 21;

        // 結びつけるComplexType作成
        // リクエストパラメータ設定
        DcRequest req0 = DcRequest.post(ComplexTypePropertyUtils.CT_REQUEST_URL);
        req0.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req0.addJsonBody(COMPLEX_TYPE_NAME_KEY, "testComplexType");
        // リクエスト実行
        request(req0);

        try {
            DcResponse response = null;
            // リクエストパラメータ設定
            for (int i = 0; i < repeatCount; i++) {
                DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
                req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY, "testComplexType");

                // リクエスト実行
                response = request(req);
                // レスポンスチェック
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
                if (i < repeatCount - 1) {
                    assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
                } else {
                    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
                }
            }
        } finally {
            // 作成したComplexTypePropertyを削除
            for (int i = 0; i < repeatCount - 1; i++) {
                try {
                    String ctplocationUrl =
                            UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                                    CT_PROPERTY_NAME + i,
                                    COMPLEX_TYPE_NAME);
                    assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
                } catch (Exception e) {
                    System.out.println(">>> " + i);
                }

            }

            // 作成したComplexTypeを削除
            String complexTypeUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "testComplexType");
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(complexTypeUrl).getStatusCode());

        }
    }

    /**
     * Simple型のComplexTypePropertyを階層最大値まで新規作成して_正常に作成できること.
     */
    @Test
    public final void Simple型のComplexTypePropertyを階層最大値まで新規作成して_正常に作成できること() {
        final int repeatCount = 50;

        try {
            DcResponse response = null;
            // リクエストパラメータ設定
            for (int i = 0; i < repeatCount; i++) {
                DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
                req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());

                // リクエスト実行
                response = request(req);
                // レスポンスチェック
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
                try {
                    assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
                } catch (Error e) {
                    fail(">>> loop count : " + i);
                }
            }
        } finally {
            // 作成したComplexTypePropertyを削除
            for (int i = 0; i < repeatCount; i++) {
                try {
                    String ctplocationUrl =
                            UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                                    CT_PROPERTY_NAME + i,
                                    COMPLEX_TYPE_NAME);
                    assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
                } catch (Exception e) {
                    System.out.println(">>> " + i);
                }
            }
        }
    }

    /**
     * SimpleTypeのComplexTypePropertyを階層最大値を超えるまで新規作成して_異常を検知できること.
     */
    @Test
    public final void SimpleTypeのComplexTypePropertyを階層最大値を超えるまで新規作成して_異常を検知できること() {
        final int repeatCount = 51;

        try {
            DcResponse response = null;
            // リクエストパラメータ設定
            for (int i = 0; i < repeatCount; i++) {
                DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
                req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());

                // リクエスト実行
                response = request(req);
                // レスポンスチェック
                Map<String, Object> expected = new HashMap<String, Object>();
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME + i);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                        EdmSimpleType.STRING.getFullyQualifiedTypeName());
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
                expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
                if (i < repeatCount - 1) {
                    assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
                } else {
                    assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
                }
            }
        } finally {
            // 作成したComplexTypePropertyを削除
            for (int i = 0; i < repeatCount - 1; i++) {
                try {
                    String ctplocationUrl =
                            UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                                    CT_PROPERTY_NAME + i,
                                    COMPLEX_TYPE_NAME);
                    assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
                } catch (Exception e) {
                    System.out.println(">>> " + i);
                }
            }
        }
    }

    /**
     * ComplexTypeProperty登録でDefaulltValueに制御コードを指定した場合_レスポンスボディがエスケープされること.
     * @throws DaoException レスポンスボディのパースに失敗
     */
    @Test
    public final void ComplexTypeProperty登録でDefaulltValueに制御コードを指定した場合_レスポンスボディがエスケープされること() throws DaoException {
        String ctplocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);

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

            // レスポンスチェック
            String resBody = response.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypePropertyのEdm.Doubleの場合に正常に作成できること.
     */
    @Test
    public final void ComplexTypePropertyのEdm_Doubleの場合に正常に作成できること() {
        String ctplocationUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        CT_PROPERTY_NAME,
                        COMPLEX_TYPE_NAME);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(ComplexTypePropertyUtils.CTP_REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            req.addJsonBody(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NAME_KEY, CT_PROPERTY_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COMPLEXTYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_TYPE_KEY,
                    EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_NULLABLE_KEY, true);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(ComplexTypePropertyUtils.CT_PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), ctplocationUrl, ComplexTypePropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したComplexTypePropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(ctplocationUrl).getStatusCode());
        }
    }
}

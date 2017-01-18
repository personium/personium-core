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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Property;
import com.fujitsu.dc.core.utils.ODataUtils;
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
 * Property登録のバリデートテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyCreateValidateDefaultValueTest extends ODataCommon {

    /** Property名. */
    private static String propName = null;

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyCreateValidateDefaultValueTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        propName = "p_name_" + String.valueOf(System.currentTimeMillis());
    }

    /**
     * PropertyのTypeがEdmStringでDefaultValueが空文字の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmStringでDefaultValueが空文字の場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmInt32でDefaultValueが最小値を下回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmInt32でDefaultValueが最小値を下回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "-2147483649");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmInt32でDefaultValueが最小値の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmInt32でDefaultValueが最小値の場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, -2147483648);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "-2147483648");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmInt32でDefaultValueが最大値の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmInt32でDefaultValueが最大値の場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, 2147483647);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "2147483647");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmInt32でDefaultValueが最大値を上回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmInt32でDefaultValueが最大値を上回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "2147483648");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDoubleでDefaultValueが正のDouble最大値を上回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDoubleでDefaultValueが正のDouble最大値を上回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "1.791e308");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDoubleでDefaultValueが正のDouble最小値を下回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDoubleでDefaultValueが正のDouble最小値を下回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "2.229e-308");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDoubleでDefaultValueが負のDouble最大値を上回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDoubleでDefaultValueが負のDouble最大値を上回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "-2.229e-308");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDoubleでDefaultValueが負のDouble最小値を下回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDoubleでDefaultValueが負のDouble最小値を下回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "-1.791e308");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmSingleでDefaultValueが整数5桁_小数5桁の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmSingleでDefaultValueが整数5桁_小数5桁の場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, 11111.11111);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "11111.11111");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmSingleでDefaultValueが整数が6桁の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmSingleでDefaultValueが整数が6桁の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, 111111.11111);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmSingleでDefaultValueが小数が6桁の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmSingleでDefaultValueが小数が6桁の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, 11111.111111);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmBooleanでDefaultValueがtrueの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmBooleanでDefaultValueがtrueの場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, true);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "true");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmBooleanでDefaultValueがfalseの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmBooleanでDefaultValueがfalseの場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, false);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "false");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmBooleanでDefaultValueが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmBooleanでDefaultValueが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "test");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueが最小値を下回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueが最小値を下回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(-9223372036854775809)/");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());

    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueが最小値の場合正常に作成されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueが最小値の場合正常に作成されること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(" + ODataUtils.DATETIME_MIN + ")/");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(" + ODataUtils.DATETIME_MIN + ")/");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueが最大値の場合正常に作成されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueが最大値の場合正常に作成されること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(" + ODataUtils.DATETIME_MAX + ")/");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(" + ODataUtils.DATETIME_MAX + ")/");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueが最大値を上回る場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueが最大値を上回る場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "/Date(9223372036854775808)/");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueがSYSUTCDATETIMEの場合正常に作成されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueがSYSUTCDATETIMEの場合正常に作成されること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "SYSUTCDATETIME()");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "SYSUTCDATETIME()");
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueがLong型を指定した場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueがLong型を指定した場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, new Long("9223372036854775807"));
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueが不正な文字列を指定した場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueが不正な文字列を指定した場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "Date(10000)/");
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(), DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY)
                        .getMessage());
    }

    /**
     * PropertyのTypeがEdmDateTimeでDefaultValueがnullの場合正常に作成されること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeでDefaultValueがnullの場合正常に作成されること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DATETIME.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのTypeがEdmStringでDefaultValueに制御コードが存在する場合_レスポンスボディがエスケープされて返却されること.
     * @throws DaoException レスポンスボディのパースに失敗
     */
    @Test
    public final void PropertyのTypeがEdmStringでDefaultValueに制御コードが存在する場合_レスポンスボディがエスケープされて返却されること() throws DaoException {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "\u0000");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            String resBody = response.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));

        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

}

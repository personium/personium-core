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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Property;
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

/**
 * Property登録のバリデートテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyCreateValidateTest extends ODataCommon {

    /** Property名. */
    private static String propName = null;

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyCreateValidateTest() {
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
     * PropertyのName属性がない場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのName属性がない場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING
                        .params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * PropertyのNameが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNameが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, "");
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * PropertyのNameが1文字の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのNameが1文字の場合_正常に作成できること() {
        String propertyName = "a";
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propertyName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
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
     * PropertyのNameが128文字の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのNameが128文字の場合_正常に作成できること() {
        String propertyName = STRING_LENGTH_128;
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propertyName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
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
     * PropertyのNameが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNameが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, STRING_LENGTH_129);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * PropertyのNameが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのNameが利用可能な文字種の場合_正常に作成できること() {
        String propertyName = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propertyName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propertyName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
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
     * PropertyのNameが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNameが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, "Ad.*s");
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * PropertyのNameが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNameが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, "-p_name");
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * PropertyのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, "_p_name");
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NAME_KEY).getMessage());
    }

    /**
     * Propertyの_EntityTypeName属性がない場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeName属性がない場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "");
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが1文字の場合_正常に作成できること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが1文字の場合_正常に作成できること() {
        String entityTypeName = "a";
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, entityTypeName);
        try {
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, entityTypeName, Setup.TEST_CELL1, -1);
        }

    }

    /**
     * Propertyの_EntityTypeNameが128文字の場合_正常に作成できること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが128文字の場合_正常に作成できること() {
        String entityTypeName = STRING_LENGTH_128;
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, entityTypeName);
        try {
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * Propertyの_EntityTypeNameが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, STRING_LENGTH_129);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが利用可能な文字種の場合_正常に作成できること() {
        String entityTypeName = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, entityTypeName);
        EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME,
                Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);

        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        Map<String, Object> expected = new HashMap<String, Object>();
        assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);

        assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                MediaType.APPLICATION_JSON, entityTypeName, Setup.TEST_CELL1, -1);
    }

    /**
     * Propertyの_EntityTypeNameが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "Ad.*s");
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "-SalesDetail");
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "_SalesDetail");
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY)
                        .getMessage());
    }

    /**
     * Propertyの_EntityTypeNameが存在しないEntityTypeの場合_BadRequestが返却されること.
     */
    @Test
    public final void Propertyの_EntityTypeNameが存在しないEntityTypeの場合_BadRequestが返却されること() {
        String entityTypeName = "xxx";

        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(entityTypeName).getMessage());
    }

    /**
     * PropertyのTypeがEdmBooleanの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmBooleanの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
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
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());
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
     * PropertyのTypeがEdmSingleの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmSingleの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
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
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.SINGLE.getFullyQualifiedTypeName());
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
     * PropertyのTypeがEdmInt32の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmInt32の場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
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
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
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
     * PropertyのTypeがEdmDateTimeの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのTypeがEdmDateTimeの場合_正常に作成できること() {
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
     * PropertyのTypeが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのTypeが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, "Edm.Datetime");
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_TYPE_KEY).getMessage());
    }

    /**
     * PropertyのTypeが登録済みのComplexTypeの場合_正常に作成されること.
     */
    @Test
    public final void PropertyのTypeが登録済みのComplexTypeの場合_正常に作成されること() {
        String complexLocationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "propTest");
        String propertyLocationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("Name", "propTest");

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, "propTest");
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, "propTest");
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), propertyLocationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(propertyLocationUrl);
            // 作成したComplexTypeを削除
            deleteOdataResource(complexLocationUrl);
        }
    }

    /**
     * PropertyのNullableがtrueの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのNullableがtrueの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
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
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl).getStatusCode();
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, PROPERTY_ENTITYTYPE_NAME, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * PropertyのNullableがfalseの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのNullableがfalseの場合_正常に作成できること() {
        String entityTypeName = "xxx";
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, entityTypeName);

        try {
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME,
                    Setup.TEST_ODATA, entityTypeName, HttpStatus.SC_CREATED);
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, false);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, false);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl).getStatusCode();
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    MediaType.APPLICATION_JSON, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * PropertyのNullableが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのNullableが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, "Edm.Datetime");
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, "test");
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_NULLABLE_KEY)
                        .getMessage());
    }

    /**
     * PropertyのCollectionKindがNoneの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのCollectionKindがNoneの場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, PROPERTY_ENTITYTYPE_NAME);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
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
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl).getStatusCode();
        }
    }

    /**
     * PropertyのCollectionKindがListの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのCollectionKindがListの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, "List");
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
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, "List");
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
     * PropertyのCollectionKindが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのCollectionKindが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, "test");
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY)
                        .getMessage());
    }

    /**
     * PropertyのIsKeyがtrueの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのIsKeyがtrueの場合_正常に作成できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, propName, PROPERTY_ENTITYTYPE_NAME);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, true);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, true);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl).getStatusCode();
        }
    }

    /**
     * PropertyのIsKeyがfalseの場合_正常に作成できること.
     */
    @Test
    public final void PropertyのIsKeyがfalseの場合_正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
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
     * PropertyのIsKeyが不正な値の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのIsKeyが不正な値の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, "Edm.Datetime");
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, "test");
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR
                        .params(PropertyUtils.PROPERTY_IS_KEY_KEY).getMessage());
    }

    /**
     * PropertyのUniqueKeyが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのUniqueKeyが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, "");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY)
                        .getMessage());
    }

    /**
     * PropertyのUniqueKeyが1文字の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのUniqueKeyが1文字の場合_正常に作成できること() {
        String uniqueKey = "a";
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのuniqueKeyが128文字の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのuniqueKeyが128文字の場合_正常に作成できること() {
        String uniqueKey = STRING_LENGTH_128;
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのuniqueKeyが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのuniqueKeyが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, STRING_LENGTH_129);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY)
                        .getMessage());
    }

    /**
     * PropertyのuniqueKeyが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void PropertyのuniqueKeyが利用可能な文字種の場合_正常に作成できること() {
        String uniqueKey = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, uniqueKey);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * PropertyのuniqueKeyが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのuniqueKeyが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, "Ad.*s");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY)
                        .getMessage());
    }

    /**
     * PropertyのuniqueKeyが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのuniqueKeyが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, "-p_name");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY)
                        .getMessage());
    }

    /**
     * PropertyのuniqueKeyが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void PropertyのuniqueKeyが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
        req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, "_p_name");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY)
                        .getMessage());
    }

}

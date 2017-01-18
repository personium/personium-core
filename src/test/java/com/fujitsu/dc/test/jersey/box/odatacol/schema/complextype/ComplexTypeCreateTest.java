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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.complextype;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * ComplexType登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypeCreateTest extends ODataCommon {

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "Address";

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /** ComplexTypeリソースURL. */
    public static final String REQUEST_URL =
            UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);

    /** 名前空間. */
    private static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + ComplexType.EDM_TYPE_NAME;

    /**
     * コンストラクタ.
     */
    public ComplexTypeCreateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ComplexTypeを新規作成して_正常に作成できること.
     */
    @Test
    public final void ComplexTypeを新規作成して_正常に作成できること() {
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, NAMESPACE, expected);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypeのName属性がない場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのName属性がない場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addStringBody("{}");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypeのNameが空文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのNameが空文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, "");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypeのNameが1文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypeのNameが1文字の場合_正常に作成できること() {
        String complexTypeName = "a";
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, NAMESPACE, expected);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypeのNameが128文字の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypeのNameが128文字の場合_正常に作成できること() {
        String complexTypeName = STRING_LENGTH_128;
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, NAMESPACE, expected);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypeのNameが129文字の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのNameが129文字の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, STRING_LENGTH_129);

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypeのNameが利用可能な文字種の場合_正常に作成できること.
     */
    @Test
    public final void ComplexTypeのNameが利用可能の場合_正常に作成できること() {
        String complexTypeName = "abcdefghijklmnopqrstuvwxyz1234567890-_";
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, complexTypeName);

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(COMPLEX_TYPE_NAME_KEY, complexTypeName);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
            checkResponseBody(response.bodyAsJson(), locationUrl, NAMESPACE, expected);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * ComplexTypeのNameが半角英数字以外の場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのNameが半角英数字以外の場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, "Ad.*s");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypeのNameが先頭文字がハイフンの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのNameが先頭文字がハイフンの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, "-Address");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * ComplexTypeのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること.
     */
    @Test
    public final void ComplexTypeのNameが先頭文字がアンダーバーの場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(COMPLEX_TYPE_NAME_KEY, "_Address");

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(COMPLEX_TYPE_NAME_KEY).getMessage());
    }

    /**
     * 既に同一名のComplexTypeが作成済みの場合_Conflictが返却されること.
     */
    @Test
    public final void 既に同一名のComplexTypeが作成済みの場合_Conflictが返却されること() {
        String locationUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, COMPLEX_TYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // リクエスト実行
            response = request(req);
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getCode(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getMessage());
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }
}

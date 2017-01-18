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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
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
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;

/**
 * Property登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyCreateTest extends ODataCommon {

    /** Property名. */
    private static String propName = null;

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyCreateTest() {
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
     * Propertyを新規作成して_正常に作成できること.
     */
    @Test
    public final void Propertyを新規作成して_正常に作成できること() {
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
     * 既にデータが存在するEntityTypeに対してPropertyのNullableをFalseで作成した場合_BadRequestが返却されること.
     */
    @Test
    public final void 既にデータが存在するEntityTypeに対してPropertyのNullableをFalseで作成した場合_BadRequestが返却されること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "SalesDetail");
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
        req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, false);

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
     * 既に同一名のPropertyが作成済みの場合_Conflictが返却されること.
     */
    @Test
    public final void 既に同一名のPropertyが作成済みの場合_Conflictが返却されること() {
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
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * Propertyを文字列型で作成後に文字列型で再作成した場合に正常に作成できること.
     */
    @Test
    public final void Propertyを文字列型で作成後に文字列型で再作成した場合に正常に作成できること() {
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

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl);
        }
    }

    /**
     * Propertyを文字列型で作成後に真偽値型で再作成した場合にBadRequestが返却されること.
     */
    @Test
    public final void Propertyを文字列型で作成後に真偽値型で再作成した場合にBadRequestが返却されること() {
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

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());

            // リクエストパラメータ設定
            req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());

            // リクエスト実行
            response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        } finally {
            // 作成したPropertyを削除
            deleteOdataResource(locationUrl);
        }
    }

    /**
     * Collectionの異なるEntityTypeNameを指定してPropertyを新規作成した場合にBadRequestが返却されること.
     */
    @Test
    public final void Collectionの異なるEntityTypeNameを指定してPropertyを新規作成した場合にBadRequestが返却されること() {
        try {
            // Collection/EntityType作成
            DavResourceUtils.createODataCollection(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1,
                    Setup.TEST_BOX1, "testcol");
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(), "testcol",
                    "anotherColEntityType", HttpStatus.SC_CREATED);

            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "anotherColEntityType");
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params("anotherColEntityType").getMessage());
        } finally {
            // 作成したEntityType/Collectionを削除
            EntityTypeUtils.delete("testcol", DcCoreConfig.getMasterToken(),
                    MediaType.APPLICATION_JSON, "anotherColEntityType", Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, "testcol",
                    DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * Boxの異なるEntityTypeNameを指定してPropertyを新規作成した場合にBadRequestが返却されること.
     */
    @Test
    public final void Boxの異なるEntityTypeNameを指定してPropertyを新規作成した場合にBadRequestが返却されること() {
        try {
            // Box/Collection/EntityType作成
            BoxUtils.create(Setup.TEST_CELL1, "anotherbox", DcCoreConfig.getMasterToken());
            DavResourceUtils.createODataCollection(DcCoreConfig.getMasterToken(), HttpStatus.SC_CREATED,
                    Setup.TEST_CELL1,
                    "anotherbox", Setup.TEST_ODATA);
            EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(), "anotherbox",
                    Setup.TEST_ODATA,
                    "anotherBoxEntityType", HttpStatus.SC_CREATED);

            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "anotherBoxEntityType");
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params("anotherBoxEntityType").getMessage());
        } finally {
            // 作成したEntityType/Collectionを削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                    MediaType.APPLICATION_JSON, "anotherBoxEntityType", "anotherbox", Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, "anotherbox", Setup.TEST_ODATA,
                    DcCoreConfig.getMasterToken(),
                    -1);
            BoxUtils.delete(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(), "anotherbox");
        }
    }

    /**
     * Property制限値より１つ少ないPropertyを持つEntityTypeに_Propertyを１つ追加して_正常終了すること.
     * @throws Exception テスト時のエラー
     */
    @Test
    @Ignore
    public final void Property制限値より１つ少ないPropertyを持つEntityTypeに_Propertyを１つ追加して_正常終了すること() throws Exception {
        // 実際には 400プロパティが登録されているEntityTypeしか存在しないので、DcCoreConfigをだまして、
        // SimplePropertyの最大値が 401であるように扱う。
        Field singletonField = DcCoreConfig.class.getDeclaredField("singleton");
        singletonField.setAccessible(true);
        DcCoreConfig singleton = (DcCoreConfig) singletonField.get(null);
        Field propField = DcCoreConfig.class.getDeclaredField("props");
        propField.setAccessible(true);
        Properties props = (Properties) propField.get(singleton);
        // ここで数値を詐称する。
        props.put(DcCoreConfig.UserDataProperties.MAX_PROPERTY_COUNT_IN_ENTITY, "401");

        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        Setup.TEST_ENTITYTYPE_MDP);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, Setup.TEST_ENTITYTYPE_MDP);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            props.put(DcCoreConfig.UserDataProperties.MAX_PROPERTY_COUNT_IN_ENTITY, "400");
        }
    }

    /**
     * Property制限値一杯のEntityTypeに_Propertyを１つ追加して_異常終了すること.
     */
    @Test
    public final void Property制限値一杯のEntityTypeに_Propertyを１つ追加して_異常終了すること() {
        // リクエストパラメータ設定
        DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
        req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, Setup.TEST_ENTITYTYPE_MDP);
        req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

        // リクエスト実行
        DcResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Property登録でTypeにEdm_Doubleを指定して正常に作成できること.
     */
    @Test
    public final void Property登録でTypeにEdm_Doubleを指定して正常に作成できること() {
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, propName);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.DOUBLE.getFullyQualifiedTypeName());
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
}

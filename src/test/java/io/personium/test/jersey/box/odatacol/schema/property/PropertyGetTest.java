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
package io.personium.test.jersey.box.odatacol.schema.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.model.ctl.Property;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.DaoException;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;

/**
 * Property1件取得のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyGetTest extends ODataCommon {

    /** Property名. */
    private static final String PROPERTY_NAME = "p_name";

    /** Property名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * Propertyを作成して_正常に取得できること.
     */
    @Test
    public final void Propertyを作成して_正常に取得できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            PersoniumRequest req = PersoniumRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // etag取得
            String etag = getEtag(response);

            // Property取得
            req = PersoniumRequest.get(locationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            expected.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            expected.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            expected.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            expected.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            expected.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            expected.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            expected.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
            checkResponseBody(resGet.bodyAsJson(), locationUrl, PropertyUtils.NAMESPACE, expected, null, etag);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * 存在しないPropertyを指定して1件取得した場合_404エラーになること.
     */
    @Test
    public final void 存在しないPropertyを指定して1件取得した場合_404エラーになること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        // Property取得
        PersoniumRequest req = PersoniumRequest.get(locationUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        PersoniumResponse resGet = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_NOT_FOUND, resGet.getStatusCode());
    }

    /**
     * 存在しないEntityTypeを指定して1件取得した場合_404エラーになること.
     */
    @Test
    public final void 存在しないEntityTypeを指定して1件取得した場合_404エラーになること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                "test");
        try {
            // リクエストパラメータ設定
            PersoniumRequest req = PersoniumRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // Property取得
            req = PersoniumRequest.get(locationUrlGet);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_NOT_FOUND, resGet.getStatusCode());
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * DefaultValueに制御コードを含むPropertyを1件取得して_レスポンスボディがエスケープされて返却されること.
     * @throws DaoException レスポンスボディのパースに失敗
     */
    @Test
    public final void DefaultValueに制御コードを含むPropertyを1件取得して_レスポンスボディがエスケープされて返却されること() throws DaoException {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        try {
            // リクエストパラメータ設定
            PersoniumRequest req = PersoniumRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            req.addJsonBody(PropertyUtils.PROPERTY_NULLABLE_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, "\u0000");
            req.addJsonBody(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_IS_KEY_KEY, null);
            req.addJsonBody(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);

            // リクエスト実行
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // Property取得
            req = PersoniumRequest.get(locationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            String resBody = resGet.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));

            // Property取得(2回目：キャッシュされた場合の動作を確認する)
            req = PersoniumRequest.get(locationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            resGet = request(req);

            // レスポンスチェック
            resBody = resGet.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

}

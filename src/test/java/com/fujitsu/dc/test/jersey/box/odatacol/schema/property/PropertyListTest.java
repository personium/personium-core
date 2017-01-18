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

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

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
 * Property一覧取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyListTest extends ODataCommon {

    /** Property名. */
    private static final String PROPERTY_NAME = "p_name";

    /** Property名. */
    private static final String PROPERTY_NAME2 = "p_name2";

    /** EntityType名. */
    private static final String PROPERTY_ENTITYTYPE_NAME = "Price";

    /**
     * コンストラクタ.
     */
    public PropertyListTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * Propertyを作成して_一覧が正常に取得できること.
     */
    @Test
    public final void Propertyを作成して_一覧が正常に取得できること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        String locationUrl2 = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME2,
                PROPERTY_ENTITYTYPE_NAME);
        String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // etag取得
            String etag = getEtag(response);

            // リクエストパラメータ設定
            DcRequest req2 = DcRequest.post(PropertyUtils.REQUEST_URL);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME2);
            req2.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            req2.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());

            // リクエスト実行
            DcResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // etag取得
            String etag2 = getEtag(response2);

            // Property取得
            req = DcRequest.get(locationUrlGet + "?$orderby=__published+desc&$top=2");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());

            Map<String, String> urlMap = new HashMap<String, String>();
            urlMap.put(PROPERTY_NAME, locationUrl);
            urlMap.put(PROPERTY_NAME2, locationUrl2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(PROPERTY_NAME, additionalprop);
            additional.put(PROPERTY_NAME2, additionalprop2);

            additionalprop.put(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME);
            additionalprop.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            additionalprop.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            additionalprop.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            additionalprop.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            additionalprop.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            additionalprop.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            additionalprop.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            additionalprop.put(PropertyUtils.PROPERTY_IS_DECLARED_KEY, true);

            additionalprop2.put(PropertyUtils.PROPERTY_NAME_KEY, PROPERTY_NAME2);
            additionalprop2.put(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, PROPERTY_ENTITYTYPE_NAME);
            additionalprop2.put(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.INT32.getFullyQualifiedTypeName());
            additionalprop2.put(PropertyUtils.PROPERTY_NULLABLE_KEY, true);
            additionalprop2.put(PropertyUtils.PROPERTY_DEFAULT_VALUE_KEY, null);
            additionalprop2.put(PropertyUtils.PROPERTY_COLLECTION_KIND_KEY, Property.COLLECTION_KIND_NONE);
            additionalprop2.put(PropertyUtils.PROPERTY_IS_KEY_KEY, false);
            additionalprop2.put(PropertyUtils.PROPERTY_UNIQUE_KEY_KEY, null);
            additionalprop2.put(PropertyUtils.PROPERTY_IS_DECLARED_KEY, true);

            Map<String, String> etagMap = new HashMap<String, String>();
            etagMap.put(PROPERTY_NAME, etag);
            etagMap.put(PROPERTY_NAME2, etag2);

            checkResponseBodyList(resGet.bodyAsJson(), urlMap, PropertyUtils.NAMESPACE, additional, "Name", COUNT_NONE,
                    etagMap);
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl2).getStatusCode());
        }
    }

    /**
     * Propertyが存在しないとき_一覧取得で0件になること.
     */
    @Test
    public final void Propertyが存在しないとき_一覧取得で0件になること() {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);

        // Property取得
        DcRequest req = DcRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse resGet = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
        checkResponseBodyList(resGet.bodyAsJson(), null, PropertyUtils.NAMESPACE, null);
    }

    /**
     * DefaultValueに制御コードを含むPropertyを一覧取得して_レスポンスボディがエスケープされて返却されること.
     * @throws DaoException レスポンスボディのパースに失敗
     */
    @Test
    public final void DefaultValueに制御コードを含むPropertyを一覧取得して_レスポンスボディがエスケープされて返却されること() throws DaoException {
        String locationUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, PROPERTY_NAME,
                PROPERTY_ENTITYTYPE_NAME);
        String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);

        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
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
            DcResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // Property取得
            req = DcRequest.get(locationUrlGet + "?$orderby=__published+desc");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            DcResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());

            // レスポンスチェック
            String resBody = resGet.bodyAsString();
            assertTrue(resBody.contains("\\u0000"));
            assertFalse(resBody.contains("\u0000"));
        } finally {
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }
}

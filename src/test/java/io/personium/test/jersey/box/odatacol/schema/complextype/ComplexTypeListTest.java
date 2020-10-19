/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.jersey.box.odatacol.schema.complextype;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.setup.Setup;
import io.personium.test.utils.UrlUtils;

/**
 * ComplexType一覧取得のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypeListTest extends ODataCommon {

    /** ComplexType名1. */
    private static final String COMPLEX_TYPE_NAME = "Address";

    /** ComplexType名2. */
    private static final String COMPLEX_TYPE_NAME2 = "NestedColumn";

    /** ComplexType NameKey名. */
    private static final String COMPLEX_TYPE_NAME_KEY = ComplexType.P_COMPLEXTYPE_NAME.getName().toString();

    /** ComplexTypeリソースURL. */
    public static final String REQUEST_URL = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
            null);

    /** 名前空間. */
    private static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + ComplexType.EDM_TYPE_NAME;

    /**
     * コンストラクタ.
     */
    public ComplexTypeListTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ComplexTypeを作成して一覧が正常に取得できること.
     */
    @Test
    public final void ComplexTypeを作成して一覧が正常に取得できること() {
        String locationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                COMPLEX_TYPE_NAME);
        String locationUrl2 = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                COMPLEX_TYPE_NAME2);
        String locationUrlGet = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);

        try {
            // ComplexType1作成
            PersoniumRequest req = PersoniumRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexType1作成時のetag
            String etag = getEtag(response);

            // ComplexType2作成
            PersoniumRequest req2 = PersoniumRequest.post(REQUEST_URL);
            req2.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req2.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME2);
            PersoniumResponse response2 = request(req2);
            assertEquals(HttpStatus.SC_CREATED, response2.getStatusCode());

            // ComplexType2作成時のetag
            String etag2 = getEtag(response2);

            // ComplexType一覧取得
            req = PersoniumRequest.get(locationUrlGet);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());

            Map<String, String> urlMap = new HashMap<String, String>();
            urlMap.put(COMPLEX_TYPE_NAME, locationUrl);
            urlMap.put(COMPLEX_TYPE_NAME2, locationUrl2);

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(COMPLEX_TYPE_NAME, additionalprop);
            additional.put(COMPLEX_TYPE_NAME2, additionalprop2);
            additionalprop.put("Name", COMPLEX_TYPE_NAME);
            additionalprop2.put("Name", COMPLEX_TYPE_NAME2);

            Map<String, String> etagMap = new HashMap<String, String>();
            etagMap.put(COMPLEX_TYPE_NAME, etag);
            etagMap.put(COMPLEX_TYPE_NAME2, etag2);

            checkResponseBodyList(resGet.bodyAsJson(), urlMap, NAMESPACE, additional, "Name", COUNT_NONE, etagMap);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl2).getStatusCode());
        }
    }

    /**
     * ComplexTypeが存在しないとき_一覧取得で0件になること.
     */
    @Test
    public final void ComplexTypeが存在しないとき_一覧取得で0件になること() {
        String locationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);

        // ComplexType一覧取得
        PersoniumRequest req = PersoniumRequest.get(locationUrl);
        req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        PersoniumResponse resGet = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());
        checkResponseBodyList(resGet.bodyAsJson(), null, NAMESPACE, null);

    }

}

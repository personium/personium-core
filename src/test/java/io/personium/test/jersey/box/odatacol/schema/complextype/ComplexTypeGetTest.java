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
import io.personium.test.unit.core.UrlUtils;

/**
 * ComplexType1件取得のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypeGetTest extends ODataCommon {

    /** ComplexType名. */
    private static final String COMPLEX_TYPE_NAME = "Address";

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
    public ComplexTypeGetTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ComplexTypeを作成して正常に取得できること.
     */
    @Test
    public final void ComplexTypeを作成して正常に取得できること() {
        String locationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                COMPLEX_TYPE_NAME);
        try {
            // ComplexType作成
            PersoniumRequest req = PersoniumRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // 作成時のetag
            String etag = getEtag(response);

            // ComplexType取得
            req = PersoniumRequest.get(locationUrl);
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, resGet.getStatusCode());

            Map<String, Object> expected = new HashMap<String, Object>();
            expected.put("Name", COMPLEX_TYPE_NAME);
            checkResponseBody(resGet.bodyAsJson(), locationUrl, NAMESPACE, expected, null, etag);
        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * 存在しないComplexTypeを取得すると404エラーとなること.
     */
    @Test
    public final void 存在しないComplexTypeを取得すると404エラーとなること() {
        String locationUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                COMPLEX_TYPE_NAME);
        String locationUrlGet = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "adress2");
        try {
            // ComplexType作成
            PersoniumRequest req = PersoniumRequest.post(REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(COMPLEX_TYPE_NAME_KEY, COMPLEX_TYPE_NAME);
            PersoniumResponse response = request(req);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());

            // ComplexType取得
            req = PersoniumRequest.get(locationUrlGet);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse resGet = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_NOT_FOUND, resGet.getStatusCode());

        } finally {
            // 作成したComplexTypeを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, deleteOdataResource(locationUrl).getStatusCode());
        }
    }

}

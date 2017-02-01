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
package io.personium.test.jersey.box.odatacol.schema.complextypeproperty;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * ComplexTypeのLinksテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypePropertyLinkTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public ComplexTypePropertyLinkTest() {
        super("io.personium.core.rs");
    }

    /**
     * ComplexTypePropertyとComplexTypeのLink作成は400が返却される事.
     */
    @Test
    public final void ComplexTypePropertyとComplexTypeのLink作成は400が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.post(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        ComplexTypeProperty.EDM_TYPE_NAME, "id", ComplexType.EDM_TYPE_NAME, null));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエスト実行
        PersoniumResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getCode(),
                PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getMessage());
    }

    /**
     * ComplexTypePropertyとComplexTypeのLink更新は501が返却される事.
     */
    @Test
    public final void ComplexTypePropertyとComplexTypeのLink更新は501が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.put(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        ComplexTypeProperty.EDM_TYPE_NAME, "id", ComplexType.EDM_TYPE_NAME, "id"));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエスト実行
        PersoniumResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getCode(),
                PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED.getMessage());
    }

    /**
     * ComplexTypePropertyとComplexTypeのLink削除は400が返却される事.
     */
    @Test
    public final void ComplexTypePropertyとComplexTypeのLink削除は400が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.delete(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        ComplexTypeProperty.EDM_TYPE_NAME, "id", ComplexType.EDM_TYPE_NAME, "id"));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);

        // リクエスト実行
        PersoniumResponse response = request(req);

        // レスポンスチェック
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
        checkErrorResponse(response.bodyAsJson(),
                PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getCode(),
                PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getMessage());
    }

    /**
     * ComplexTypePropertyとComplexTypeのLinkの一覧取得ができる事.
     */
    @Test
    public final void ComplexTypePropertyとComplexTypeのLinkの一覧取得ができる事() {
        String complexTypeName = "testComplexType";
        String complexTypePropertyName = "testComplexTypeProperty";
        String complexTypeUrl = null;
        String complexTypePropertyUrl = null;

        try {
            // ComplexType作成
            complexTypeUrl = UserDataUtils.createComplexType(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypeName)
                    .getFirstHeader(HttpHeaders.LOCATION);

            // ComplexTypeProperty作成
            complexTypePropertyUrl = UserDataUtils.createComplexTypeProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    complexTypePropertyName, complexTypeName, "Edm.String", true, null, "None")
                    .getFirstHeader(HttpHeaders.LOCATION);

            // ComplexTypeProperty - ComplexType $links一覧取得
            String key = String.format("Name='%s',_ComplexType.Name='%s'", complexTypePropertyName, complexTypeName);
            PersoniumRequest req = PersoniumRequest.get(
                    UrlUtils.schemaLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            ComplexTypeProperty.EDM_TYPE_NAME, key, ComplexType.EDM_TYPE_NAME, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ArrayList<String> expectedUri = new ArrayList<String>();
            expectedUri.add(complexTypeUrl);
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), expectedUri);
        } finally {

            // ComplexTypeProperty削除
            ODataCommon.deleteOdataResource(complexTypePropertyUrl);
            // ComplexType削除
            ODataCommon.deleteOdataResource(complexTypeUrl);
        }
    }

}

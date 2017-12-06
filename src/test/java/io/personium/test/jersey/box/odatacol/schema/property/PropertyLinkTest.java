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

import java.util.ArrayList;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * ComplexTypeのLinksテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyLinkTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public PropertyLinkTest() {
        super("io.personium.core.rs");
    }

    /**
     * PropertyとEntityTypeのLink作成は400が返却される事.
     */
    @Test
    public final void PropertyとEntityTypeのLink作成は400が返却される事() {
        String entityTypeName = "testEntity";
        String propertyName = "testProperty";
        String propertyLocationUrl = null;
        String entityTypeLocationUrl = null;

        try {
            // EntityType作成
            entityTypeLocationUrl = EntityTypeUtils.create(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName,
                    HttpStatus.SC_CREATED)
                    .getLocationHeader();

            // Property作成
            propertyLocationUrl = UserDataUtils.createProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, propertyName,
                    entityTypeName, "Edm.String", true, null, "None", false, null)
                    .getFirstHeader(HttpHeaders.LOCATION);

            // EntityType - Property $links一覧取得
            String key = String.format("Name='%s',_EntityType.Name='%s'", propertyName, entityTypeName);
            PersoniumRequest req = PersoniumRequest.post(
                    UrlUtils.schemaLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            Property.EDM_TYPE_NAME, key, EntityType.EDM_TYPE_NAME, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("uri",
                    UrlUtils.entityType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName));

            // リクエスト実行
            PersoniumResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getCode(),
                    PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getMessage());
        } finally {
            // Property削除
            ODataCommon.deleteOdataResource(propertyLocationUrl);

            // EntityType削除
            ODataCommon.deleteOdataResource(entityTypeLocationUrl);
        }
    }

    /**
     * PropertyとEntityTypeのLink更新は501が返却される事.
     */
    @Test
    public final void PropertyとEntityTypeのLink更新は501が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.put(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        Property.EDM_TYPE_NAME, "id", EntityType.EDM_TYPE_NAME, "id"));
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
     * PropertyとEntityTypeのLink削除は400が返却される事.
     */
    @Test
    public final void PropertyとEntityTypeのLink削除は400が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.delete(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        Property.EDM_TYPE_NAME, "id", EntityType.EDM_TYPE_NAME, "id"));
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
     * PropertyとEntityTypeのLinkの一覧取得ができる事.
     */
    @Test
    public final void PropertyとEntityTypeのLinkの一覧取得ができる事() {
        String entityTypeName = "testEntity";
        String propertyName = "testProperty";
        String propertyLocationUrl = null;
        String entityTypeLocationUrl = null;

        try {
            // EntityType作成
            entityTypeLocationUrl = EntityTypeUtils.create(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName,
                    HttpStatus.SC_CREATED)
                    .getLocationHeader();

            // Property作成
            propertyLocationUrl = UserDataUtils.createProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, propertyName,
                    entityTypeName, "Edm.String", true, null, "None", false, null)
                    .getFirstHeader(HttpHeaders.LOCATION);

            // Property - EntityType $links一覧取得
            String propertyKey = String.format("Name='%s',_EntityType.Name='%s'", propertyName, entityTypeName);
            PersoniumRequest req = PersoniumRequest.get(
                    UrlUtils.schemaLinks(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            Property.EDM_TYPE_NAME, propertyKey, EntityType.EDM_TYPE_NAME, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ArrayList<String> expectedUri = new ArrayList<String>();
            expectedUri.add(entityTypeLocationUrl);
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), expectedUri);
        } finally {
            // Property削除
            ODataCommon.deleteOdataResource(propertyLocationUrl);

            // EntityType削除
            ODataCommon.deleteOdataResource(entityTypeLocationUrl);
        }
    }

    /**
     * EntityTypeとPropertyのLink作成は400が返却される事.
     */
    @Test
    public final void EntityTypeとPropertyのLink作成は400が返却される事() {
        String entityTypeName = "testEntity";
        String entityTypeLocationUrl = null;

        try {
            // EntityType作成
            entityTypeLocationUrl = EntityTypeUtils.create(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName,
                    HttpStatus.SC_CREATED)
                    .getLocationHeader();

            // EntityType - Property $links登録
            PersoniumRequest req = PersoniumRequest.post(
                    UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            EntityType.EDM_TYPE_NAME, entityTypeName, Property.EDM_TYPE_NAME, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("uri", UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    "dummyProperty", entityTypeName));

            // リクエスト実行
            PersoniumResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
            checkErrorResponse(response.bodyAsJson(),
                    PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getCode(),
                    PersoniumCoreException.OData.NO_SUCH_ASSOCIATION.getMessage());
        } finally {
            // EntityType削除
            ODataCommon.deleteOdataResource(entityTypeLocationUrl);
        }
    }

    /**
     * EntityTypeとPropertyのLink更新は501が返却される事.
     */
    @Test
    public final void EntityTypeとPropertyのLink更新は501が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.put(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        EntityType.EDM_TYPE_NAME, "id", Property.EDM_TYPE_NAME, "id"));
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
     * EntityTypeとPropertyのLink削除は400が返却される事.
     */
    @Test
    public final void EntityTypeとPropertyのLink削除は400が返却される事() {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.delete(
                UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                        EntityType.EDM_TYPE_NAME, "id", Property.EDM_TYPE_NAME, "id"));
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
     * EntityTypeとPropertyのLinkの一覧取得ができる事.
     */
    @Test
    public final void EntityTypeとPropertyのLinkの一覧取得ができる事() {
        String entityTypeName = "testEntity";
        String propertyName = "testProperty";
        String propertyLocationUrl = null;
        String entityTypeLocationUrl = null;

        try {
            // EntityType作成
            entityTypeLocationUrl = EntityTypeUtils.create(
                    Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, entityTypeName,
                    HttpStatus.SC_CREATED)
                    .getLocationHeader();

            // Property作成
            propertyLocationUrl = UserDataUtils.createProperty(
                    Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, propertyName,
                    entityTypeName, "Edm.String", true, null, "None", false, null)
                    .getFirstHeader(HttpHeaders.LOCATION);

            // EntityType - Property $links一覧取得
            PersoniumRequest req = PersoniumRequest.get(
                    UrlUtils.schemaLinksWithSingleQuote(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            EntityType.EDM_TYPE_NAME, entityTypeName, Property.EDM_TYPE_NAME, null));
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse response = request(req);

            // レスポンスチェック
            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
            ArrayList<String> expectedUri = new ArrayList<String>();
            expectedUri.add(propertyLocationUrl);
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), expectedUri);
        } finally {
            // Property削除
            ODataCommon.deleteOdataResource(propertyLocationUrl);

            // EntityType削除
            ODataCommon.deleteOdataResource(entityTypeLocationUrl);
        }
    }
}

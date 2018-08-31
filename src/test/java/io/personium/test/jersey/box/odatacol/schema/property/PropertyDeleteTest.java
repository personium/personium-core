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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.box.odatacol.AbstractUserDataTest;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;

/**
 * Property削除のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropertyDeleteTest extends AbstractUserDataTest {

    /** Property名. */
    private static String propName = null;

    /**
     * コンストラクタ.
     */
    public PropertyDeleteTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @Before
    public void before() {
        propName = "p_name_" + String.valueOf(System.currentTimeMillis());
    }

    /**
     * ユーザデータが存在する場合にPropertyを削除した場合409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ユーザデータが存在する場合にPropertyを削除した場合409が返却されること() {
        String entityTypeName = "testEntity";
        String userDataId = "userdata001";
        String entityRequestUrl = null;
        String propertyRequestUrl = null;
        PersoniumResponse res = null;

        try {
            // EntityType登録
            entityRequestUrl = UrlUtils.entityType(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, null);
            PersoniumRequest req = PersoniumRequest.post(entityRequestUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody("Name", entityTypeName);
            // リクエスト実行
            res = request(req);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

            // プロパティ登録
            propertyRequestUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);
            req = PersoniumRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, entityTypeName);
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.STRING.getFullyQualifiedTypeName());
            // リクエスト実行
            res = request(req);
            assertEquals(HttpStatus.SC_CREATED, res.getStatusCode());

            // ユーザデータ登録
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put(propName, "staticPropertyValue");
            body.put("dynamicProperty", "dynamicPropertyValue");
            // リクエスト実行
            createUserDataWithDcClient(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName, body);

            // DynamicPropertyの削除 409が返却される事
            propertyRequestUrl =
                    UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "dynamicProperty",
                            entityTypeName);
            res = ODataCommon.deleteOdataResource(propertyRequestUrl);

            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());

            // StaticPropertyの削除 409が返却される事
            propertyRequestUrl =
                    UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                            entityTypeName);
            res = ODataCommon.deleteOdataResource(propertyRequestUrl);

            assertEquals(HttpStatus.SC_CONFLICT, res.getStatusCode());

        } finally {
            // ユーザデータ削除
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, MASTER_TOKEN_NAME, -1);

            // DynamicProperty削除
            propertyRequestUrl =
                    UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "dynamicProperty",
                            entityTypeName);
            res = ODataCommon.deleteOdataResource(propertyRequestUrl);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // StaticProperty削除
            propertyRequestUrl =
                    UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                            entityTypeName);
            res = ODataCommon.deleteOdataResource(propertyRequestUrl);
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());

            // エンティテタイプ削除
            entityRequestUrl = UrlUtils.entityType(Setup.TEST_CELL1,
                    Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);
            ODataCommon.deleteOdataResource(entityRequestUrl);
        }
    }
}

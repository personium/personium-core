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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.model.ctl.ComplexType;
import com.fujitsu.dc.core.model.ctl.ComplexTypeProperty;
import com.fujitsu.dc.core.model.ctl.EntityType;
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

/**
 * ComplexType削除のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ComplexTypeDeleteTest extends ODataCommon {

    /**
     * コンストラクタ.
     */
    public ComplexTypeDeleteTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ComplexTypeを使用しているPropertyが存在している場合に４０９が返却される.
     */
    @Test
    public final void ComplexTypeを使用しているPropertyが存在している場合に４０９が返却される() {
        // コンプレックスタイプ作成
        String complexTypeName = "deleteTest";
        String complexTypeEntitiesUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);
        DcRequest req = DcRequest.post(complexTypeEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexType.P_COMPLEXTYPE_NAME.getName(), complexTypeName);
        request(req);

        // エンティティタイプ作成
        String entityTypeName = "deleteTestEntity";
        String entityTypeEntitiesUrl =
                UrlUtils.entityType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);
        req = DcRequest.post(entityTypeEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(EntityType.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        request(req);

        // プロパティ作成
        String propertyName = "deleteTestProperty";
        String propertyEntitiesUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);
        req = DcRequest.post(propertyEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(Property.P_NAME.getName(), propertyName);
        req.addJsonBody(Property.P_TYPE.getName(), complexTypeName);
        req.addJsonBody(Property.P_ENTITYTYPE_NAME.getName(), entityTypeName);
        request(req);

        // ComplexTypeを使用しているPropertyが存在している場合に４０９が返却されること
        String complexTypeEntityUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, complexTypeName);
        req = DcRequest.delete(complexTypeEntityUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse response = request(req);
        try {
            assertEquals(HttpStatus.SC_CONFLICT, response.getStatusCode());
        } finally {
            // Property削除
            String propertyEntityUrl = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    propertyName, entityTypeName);
            req = DcRequest.delete(propertyEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // ComplexType削除
            req = DcRequest.delete(complexTypeEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            response.getStatusCode();

            // EntityType削除
            String entityTypeEntityUrl =
                    UrlUtils.entityType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);
            req = DcRequest.delete(entityTypeEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            request(req);
        }

    }

    /**
     * ComplexTypeを使用しているComplexTypePropertyが存在している場合に４０９が返却される.
     */
    @Test
    public final void ComplexTypeを使用しているComplexTypePropertyが存在している場合に４０９が返却される() {
        // コンプレックスタイプ作成（コンプレックスタイププロパティの型）
        String complexTypeName = "deleteTest";
        String complexTypeEntitiesUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);
        DcRequest req = DcRequest.post(complexTypeEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexType.P_COMPLEXTYPE_NAME.getName(), complexTypeName);
        request(req);

        // コンプレックスタイプ作成(コンプレックスタイププロパティのリンク)
        String linkedComplexTypeName = "linkedComplexTypeName";
        String linkedComplexTypeEntitiesUrl =
                UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null);
        req = DcRequest.post(linkedComplexTypeEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexType.P_COMPLEXTYPE_NAME.getName(), linkedComplexTypeName);
        request(req);

        // コンプレックスタイププロパティ作成
        String complexTypePropertyName = "deleteTestProperty";
        String complexTypePropertyEntitiesUrl =
                UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, null, null);
        req = DcRequest.post(complexTypePropertyEntitiesUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexTypeProperty.P_NAME.getName(), complexTypePropertyName);
        req.addJsonBody(ComplexTypeProperty.P_TYPE.getName(), complexTypeName);
        req.addJsonBody(ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName(), linkedComplexTypeName);
        request(req);

        // ComplexTypeを使用しているComplexTypePropertyが存在している場合に４０９が返却されること
        String complexTypeEntityUrl = UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                complexTypeName);
        req = DcRequest.delete(complexTypeEntityUrl);
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
        DcResponse response = request(req);
        try {
            assertEquals(HttpStatus.SC_CONFLICT, response.getStatusCode());
        } finally {
            // ComplexTypeProperty削除
            String complexTypePropertyEntityUrl = UrlUtils.complexTypeProperty(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, complexTypePropertyName, linkedComplexTypeName);
            req = DcRequest.delete(complexTypePropertyEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);

            // ComplexType削除（コンプレックスタイププロパティの型）
            req = DcRequest.delete(complexTypeEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            response = request(req);
            response.getStatusCode();

            // ComplexType削除(コンプレックスタイププロパティのリンク)
            String linkedComplexTypeEntityUrl =
                    UrlUtils.complexType(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, linkedComplexTypeName);
            req = DcRequest.delete(linkedComplexTypeEntityUrl);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            request(req);
        }

    }
}
